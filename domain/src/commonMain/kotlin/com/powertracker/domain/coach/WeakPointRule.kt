package com.powertracker.domain.coach

import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.liftLabel
import com.powertracker.domain.time.mondayOfWeek
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlin.math.roundToInt

/** Flags a lagging lift from lift-to-lift strength ratios and from consistently repeated miss points. */
object WeakPointRule {
    val SQUAT_TO_DEADLIFT = 0.80..0.90
    val BENCH_TO_SQUAT = 0.60..0.70

    private const val RATIO_WEEKS = 4
    private const val RATIO_SEVERITY = 4
    private const val MISS_WINDOW_DAYS = 27L
    private const val MIN_MISSES = 3
    private const val MISS_SHARE = 0.6

    private class LiftPair(
        val numerator: MainLift,
        val denominator: MainLift,
        val range: ClosedFloatingPointRange<Double>,
    )

    // The only ratios: strict press is never part of one, so it is never flagged as lagging another lift.
    private val pairs = listOf(
        LiftPair(MainLift.SQUAT, MainLift.DEADLIFT, SQUAT_TO_DEADLIFT),
        LiftPair(MainLift.BENCH, MainLift.SQUAT, BENCH_TO_SQUAT),
    )

    private class Flag(var severity: Int = 0, var missPoint: MissPoint? = null, val reasons: MutableList<String> = mutableListOf())

    fun evaluate(stats: HistoryStats, today: LocalDate): List<Advice> {
        val flags = mutableMapOf<MainLift, Flag>()
        fun flag(lift: MainLift) = flags.getOrPut(lift) { Flag() }

        for (pair in pairs) ratioFlag(pair, stats, today)?.let { (lift, reason) ->
            flag(lift).apply { severity = maxOf(severity, RATIO_SEVERITY); reasons += reason }
        }
        for (lift in MainLift.entries) missFlag(lift, stats, today)?.let { (point, count) ->
            flag(lift).apply {
                severity = maxOf(severity, count)
                missPoint = point
                reasons += "You've missed ${lift.lower()} ${missPhrase(lift, point)} $count times in the last 4 weeks."
            }
        }

        return flags.entries.sortedBy { it.key }.map { (lift, f) ->
            Advice(
                type = AdviceType.WEAK_POINT,
                lift = lift,
                createdAt = today,
                reason = f.reasons.joinToString(" "),
                severity = f.severity,
                payload = AdvicePayload.WeakPoint(lift, f.missPoint),
            )
        }
    }

    private fun ratioFlag(pair: LiftPair, stats: HistoryStats, today: LocalDate): kotlin.Pair<MainLift, String>? {
        val num = stats.weeklyTrend(pair.numerator, today).associate { it.weekStart to it.e1rmKg }
        val den = stats.weeklyTrend(pair.denominator, today).associate { it.weekStart to it.e1rmKg }
        val ratios = num.keys.filter { it in den }.sorted().takeLast(RATIO_WEEKS)
        if (ratios.size < RATIO_WEEKS) return null
        val thisWeek = today.mondayOfWeek()
        if (ratios.last().daysUntil(thisWeek) / 7 > 1) return null

        val values = ratios.map { num.getValue(it) / den.getValue(it) }
        val lo = (pair.range.start * 100).roundToInt()
        val hi = (pair.range.endInclusive * 100).roundToInt()
        val pct = (values.last() * 100).roundToInt()
        val n = pair.numerator.lower()
        val d = pair.denominator.lower()
        return when {
            values.all { it < pair.range.start } ->
                pair.numerator to "Your $n is low relative to your $d ($pct% vs a typical $lo–$hi%). Prioritise $n."
            values.all { it > pair.range.endInclusive } ->
                pair.denominator to
                    "Your $d is low relative to your $n ($n is $pct% of $d, typical $lo–$hi%). Prioritise $d."
            else -> null
        }
    }

    private fun missFlag(lift: MainLift, stats: HistoryStats, today: LocalDate): kotlin.Pair<MissPoint, Int>? {
        val misses = stats.missPoints(lift, today.minus(MISS_WINDOW_DAYS, DateTimeUnit.DAY), today)
        val top = misses.groupingBy { it }.eachCount().maxByOrNull { it.value } ?: return null
        return if (top.value >= MIN_MISSES && top.value >= MISS_SHARE * misses.size) top.key to top.value else null
    }

    private fun MainLift.lower() = liftLabel(this).lowercase()
}
