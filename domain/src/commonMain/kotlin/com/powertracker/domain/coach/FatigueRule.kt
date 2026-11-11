package com.powertracker.domain.coach

import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.liftLabel
import com.powertracker.domain.time.mondayOfWeek
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.roundToLong

/** Recommends a deload week when at least two of four fatigue signals fire over the last 14 days. */
object FatigueRule {
    private const val WINDOW_DAYS = 14L
    private const val MIN_SIGNALS = 2
    private const val MIN_LIFTS = 2
    private const val RPE_RISE = 1.0
    private const val MIN_FAILED_SETS = 2
    private const val LOW_READINESS = 2.0
    private const val READINESS_DROP = 1.5

    internal data class Signal(val text: String)

    fun evaluate(stats: HistoryStats, history: TrainingHistory, today: LocalDate): Advice? {
        val fired = signals(stats, history, today)
        if (fired.size < MIN_SIGNALS) return null
        val deload = AdvicePayload.Deload()
        val reason = "Signs of fatigue: ${fired.joinToString("; ") { it.text }}. " +
            "Take a deload week: about ${percent(deload.volumeFactor)}% of the volume " +
            "at about ${percent(deload.loadFactor)}% of the load."
        return Advice(AdviceType.DELOAD, null, today, reason, fired.size, deload)
    }

    internal fun signals(stats: HistoryStats, history: TrainingHistory, today: LocalDate): List<Signal> {
        val windowStart = today.minus(WINDOW_DAYS - 1, DateTimeUnit.DAY)
        val sessions = history.sessions
            .filter { it.date >= windowStart && it.date <= today }
            .sortedBy { it.date }
        return listOfNotNull(
            rpeRising(sessions, history),
            failedReps(sessions, history),
            lowReadiness(sessions, windowStart),
            e1rmDown(stats, today),
        )
    }

    private fun percent(factor: Double) = (factor * 100).roundToLong()

    private fun liftNames(lifts: List<MainLift>) = lifts.joinToString(" and ") { liftLabel(it).lowercase() }

    private fun mainSets(session: LoggedSession, history: TrainingHistory): List<LoggedSet> =
        session.sets.filter { history.exercises[it.exerciseId]?.isMainLift == true }

    private fun topSet(sets: List<LoggedSet>): LoggedSet? =
        sets.lastOrNull { it.isTopSet } ?: sets.filter { !it.failed }.maxByOrNull { it.weightKg }

    private fun rpeRising(sessions: List<LoggedSession>, history: TrainingHistory): Signal? {
        val lifts = MainLift.entries.filter { lift ->
            val tops = sessions.mapNotNull { s ->
                topSet(mainSets(s, history).filter { history.exercises[it.exerciseId]?.mainLift == lift })
            }
            val latest = tops.lastOrNull()
            val latestRpe = latest?.rpe
            latest != null && latestRpe != null && tops.dropLast(1).any { earlier ->
                val earlierRpe = earlier.rpe
                earlierRpe != null && latest.weightKg <= earlier.weightKg && latestRpe - earlierRpe >= RPE_RISE
            }
        }
        return if (lifts.size >= MIN_LIFTS) Signal("RPE rising at the same load on ${liftNames(lifts)}") else null
    }

    private fun failedReps(sessions: List<LoggedSession>, history: TrainingHistory): Signal? {
        val failed = sessions.sumOf { s -> mainSets(s, history).count { it.failed } }
        return if (failed >= MIN_FAILED_SETS) Signal("$failed failed reps") else null
    }

    private fun lowReadiness(sessions: List<LoggedSession>, windowStart: LocalDate): Signal? {
        val rated = sessions.mapNotNull { s -> s.readiness?.let { s.date to it.toDouble() } }
        if (rated.isEmpty()) return null
        val low = rated.map { it.second }.average() <= LOW_READINESS
        val midpoint = windowStart.plus(WINDOW_DAYS / 2, DateTimeUnit.DAY)
        val earlier = rated.filter { it.first < midpoint }.map { it.second }
        val later = rated.filter { it.first >= midpoint }.map { it.second }
        val dropped = earlier.isNotEmpty() && later.isNotEmpty() &&
            earlier.average() - later.average() >= READINESS_DROP
        return if (low || dropped) Signal("low readiness") else null
    }

    private fun e1rmDown(stats: HistoryStats, today: LocalDate): Signal? {
        val recentWeek = today.mondayOfWeek().minus(1, DateTimeUnit.WEEK)
        val lifts = MainLift.entries.filter { lift ->
            val trend = stats.weeklyTrend(lift, today)
            val latest = trend.lastOrNull()
            val previous = trend.getOrNull(trend.size - 2)
            latest != null && previous != null &&
                latest.e1rmKg < previous.e1rmKg && latest.weekStart >= recentWeek
        }
        return if (lifts.size >= MIN_LIFTS) Signal("e1RM down on ${liftNames(lifts)}") else null
    }
}
