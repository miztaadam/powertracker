package com.powertracker.domain.history

import com.powertracker.domain.e1rm.E1rm
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.TrainingHistory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class WeeklyPoint(val weekStart: LocalDate, val e1rmKg: Double)

/** Read-only query layer over a [TrainingHistory]; every coach rule reads history through this. */
class HistoryStats(private val history: TrainingHistory) {
    private val sessions = history.sessions.sortedBy { it.date }

    private fun categoryOf(set: LoggedSet): Category? = history.exercises[set.exerciseId]?.category
    private fun liftOf(set: LoggedSet): MainLift? = history.exercises[set.exerciseId]?.mainLift

    /**
     * Best e1RM per ISO week (Monday start) for the lift, from sessions on or before [until].
     * Main-lift sets count in full; VARIATION sets of that parentLift count at x0.9 (trend only).
     * Failed sets are excluded. Weeks with no qualifying sets are omitted. Sorted ascending.
     */
    fun weeklyTrend(lift: MainLift, until: LocalDate): List<WeeklyPoint> {
        val best = sortedMapOf<LocalDate, Double>()
        for (session in sessions) {
            if (session.date.isAfter(until)) continue
            val week = session.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            for (set in session.sets) {
                if (set.failed || liftOf(set) != lift) continue
                val factor = when (categoryOf(set)) {
                    Category.VARIATION -> 0.9
                    Category.SQUAT, Category.BENCH, Category.DEADLIFT -> 1.0
                    else -> continue
                }
                val e = factor * E1rm.estimate(set.weightKg, set.reps, set.rpe)
                best.merge(week, e, ::maxOf)
            }
        }
        return best.map { (week, e) -> WeeklyPoint(week, e) }
    }

    /**
     * Best e1RM from non-failed main-lift sets only (never variations) within (until - days, until].
     * With no such sets, falls back to the latest Baseline (date <= until) of the lift's main exercise;
     * null if there is none either.
     */
    fun currentE1rm(lift: MainLift, until: LocalDate, days: Long = 28): Double? {
        val from = until.minusDays(days)
        val best = sessions
            .filter { it.date.isAfter(from) && !it.date.isAfter(until) }
            .flatMap { it.sets }
            .filter { !it.failed && history.exercises[it.exerciseId]?.let { e -> e.isMainLift && e.mainLift == lift } == true }
            .maxOfOrNull { E1rm.estimate(it.weightKg, it.reps, it.rpe) }
        if (best != null) return best
        val mainIds = history.exercises.values.filter { it.isMainLift && it.mainLift == lift }.map { it.id }.toSet()
        return history.baselines
            .filter { it.exerciseId in mainIds && !it.date.isAfter(until) }
            .maxByOrNull { it.date }
            ?.e1rmKg
    }

    /** Number of sessions containing at least one main-lift (not variation) set of [lift]. */
    fun sessionCount(lift: MainLift): Int = sessions.count { s ->
        s.sets.any { set -> history.exercises[set.exerciseId]?.let { it.isMainLift && it.mainLift == lift } == true }
    }

    /**
     * The top set of the latest session strictly before [before] that contains [exerciseId].
     * Top set = the set flagged isTopSet (last one if several), else the heaviest non-failed set
     * (ties go to the later set). If every set failed, the heaviest failed set is returned so that
     * progression rules can see the failure.
     */
    fun lastTopSet(exerciseId: Long, before: LocalDate): Pair<LocalDate, LoggedSet>? =
        topSets(exerciseId, before).lastOrNull()

    /** The top set of the session preceding the one [lastTopSet] returns; null if there is none. */
    fun previousTopSet(exerciseId: Long, before: LocalDate): Pair<LocalDate, LoggedSet>? =
        topSets(exerciseId, before).let { if (it.size >= 2) it[it.size - 2] else null }

    private fun topSets(exerciseId: Long, before: LocalDate): List<Pair<LocalDate, LoggedSet>> =
        sessions.filter { it.date.isBefore(before) }.mapNotNull { s ->
            val sets = s.sets.filter { it.exerciseId == exerciseId }
            val top = sets.lastOrNull { it.isTopSet }
                ?: sets.filter { !it.failed }.withIndex().maxWithOrNull(compareBy({ it.value.weightKg }, { it.index }))?.value
                ?: sets.withIndex().maxWithOrNull(compareBy({ it.value.weightKg }, { it.index }))?.value
            top?.let { s.date to it }
        }

    /** Miss points of sets on the lift or its variations in sessions within [from]..[until], both inclusive. */
    fun missPoints(lift: MainLift, from: LocalDate, until: LocalDate): List<MissPoint> =
        sessions
            .filter { !it.date.isBefore(from) && !it.date.isAfter(until) }
            .flatMap { it.sets }
            .filter { liftOf(it) == lift }
            .mapNotNull { it.missPoint }

    /** Date of the latest session strictly before [before]; null if none. */
    fun lastSessionDate(before: LocalDate): LocalDate? =
        sessions.lastOrNull { it.date.isBefore(before) }?.date

    /**
     * Considering sessions on or before [until]: the later date of the most recent pair of consecutive
     * session dates at least 14 days apart; null if there is no such gap.
     */
    fun returnDateAfterGap(until: LocalDate): LocalDate? {
        val dates = sessions.map { it.date }.filter { !it.isAfter(until) }.distinct()
        return dates.zipWithNext().lastOrNull { (a, b) -> ChronoUnit.DAYS.between(a, b) >= 14 }?.second
    }
}
