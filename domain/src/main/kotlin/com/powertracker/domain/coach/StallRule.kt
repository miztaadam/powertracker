package com.powertracker.domain.coach

import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.AccessoryPrescription
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Detects a plateau in a main lift's weekly e1RM and prescribes accessories for the dominant miss point. */
object StallRule {
    private const val PEAK_MARGIN = 1.01
    private const val MAX_ITEMS = 3

    fun evaluate(
        lift: MainLift,
        stats: HistoryStats,
        settings: CoachSettings,
        today: LocalDate,
        currentAccessoryNames: Set<String>,
    ): Advice? {
        val n = if (lift == MainLift.DEADLIFT) settings.deadliftStallWeeks else settings.stallWeeks
        val trend = stats.weeklyTrend(lift, today)
        if (trend.isEmpty()) return null

        var peak = trend.first()
        var best = peak.e1rmKg
        for (point in trend.drop(1)) {
            if (point.e1rmKg > best * PEAK_MARGIN) peak = point
            best = maxOf(best, point.e1rmKg)
        }

        val thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weeksSincePeak = ChronoUnit.WEEKS.between(peak.weekStart, thisWeek).toInt()
        if (weeksSincePeak < n) return null

        val windowStart = thisWeek.minusWeeks(n - 1L)
        if (trend.count { !it.weekStart.isBefore(windowStart) } < n - 1) return null

        val miss = stats.missPoints(lift, peak.weekStart.plusWeeks(1), today)
            .withIndex()
            .groupBy { it.value }
            .maxWithOrNull(compareBy({ it.value.size }, { it.value.last().index }))
            ?.key

        val owned = currentAccessoryNames.map { it.lowercase() }.toSet()
        val items = AccessoryMap.options(lift, miss)
            .filter { it.exerciseName.lowercase() !in owned }
            .take(MAX_ITEMS)
            .map { if (it.heavy) AccessoryPrescription(it.exerciseName, 3, 3, 5) else AccessoryPrescription(it.exerciseName, 3, 8, 10) }

        val name = lift.displayName()
        val head = "$name e1RM hasn't improved for $weeksSincePeak weeks."
        val reason = if (miss == null) {
            "$head Add one set, change the rep range (5s → 3s or 8s), or swap to a variation."
        } else {
            val muscles = AccessoryMap.muscles(lift, miss).joinToString(" and ") { it.lowercase() }
            "$head You've mostly missed ${missPhrase(lift, miss)} — this works the $muscles."
        }

        return Advice(
            type = AdviceType.STALL,
            lift = lift,
            createdAt = today,
            reason = reason,
            severity = weeksSincePeak,
            payload = AdvicePayload.Accessories(items),
        )
    }

    private fun MainLift.displayName() = when (this) {
        MainLift.SQUAT -> "Squat"
        MainLift.BENCH -> "Bench"
        MainLift.DEADLIFT -> "Deadlift"
    }

    private fun missPhrase(lift: MainLift, miss: MissPoint) = when (miss) {
        MissPoint.LOCKOUT -> "at lockout"
        MissPoint.MID -> "mid-range"
        MissPoint.FLOOR -> when (lift) {
            MainLift.SQUAT -> "out of the hole"
            MainLift.BENCH -> "off the chest"
            MainLift.DEADLIFT -> "off the floor"
        }
    }
}
