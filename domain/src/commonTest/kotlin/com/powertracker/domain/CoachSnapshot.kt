package com.powertracker.domain

import com.powertracker.domain.coach.Coach
import com.powertracker.domain.coach.CoachSettings
import com.powertracker.domain.coach.FatigueRule
import com.powertracker.domain.coach.StallRule
import com.powertracker.domain.coach.WeakPointRule
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.progress.ProgressMath
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.math.roundToLong

/**
 * Renders everything the coach and the Progress maths say about a history, day by day, as plain text that is the
 * same on the JVM and in JS (numbers are printed in thousandths, never through Double.toString). Used to pin the
 * output of histories with no strict press, so adding the fourth main lift cannot change it.
 */
object CoachSnapshot {
    /** The three lifts every pinned history trains, named explicitly so the snapshot never loops over new values. */
    private val SBD = listOf(MainLift.SQUAT, MainLift.BENCH, MainLift.DEADLIFT)

    private fun n(v: Double): String = (v * 1000).roundToLong().toString()
    private fun n(v: Double?): String = v?.let { n(it) } ?: "-"

    private fun payload(p: AdvicePayload): String = when (p) {
        is AdvicePayload.Target -> "target(${p.exerciseId},${n(p.weightKg)},${p.reps},${n(p.rpe)},${p.sets})"
        is AdvicePayload.Accessories -> "acc(" + p.items.joinToString(";") { "${it.exerciseName},${it.sets},${it.repsLow},${it.repsHigh}" } + ")"
        is AdvicePayload.Deload -> "deload(${n(p.volumeFactor)},${n(p.loadFactor)})"
        is AdvicePayload.WeakPoint -> "weak(${p.lift},${p.missPoint})"
        is AdvicePayload.ReEntry -> "reentry(${n(p.loadFactor)})"
    }

    private fun advice(a: Advice) = "${a.type}|${a.lift}|${a.createdAt}|${a.severity}|${a.status}|${a.reason}|${payload(a.payload)}"

    fun render(
        history: TrainingHistory,
        plan: List<PlanItem>,
        settings: CoachSettings,
        start: LocalDate,
        days: IntRange,
        stored: (LocalDate) -> List<Advice> = { emptyList() },
    ): String = buildString {
        for (d in days) {
            val today = start.plus(d, DateTimeUnit.DAY)
            val h = history.copy(sessions = history.sessions.filter { it.date <= today })
            val stats = HistoryStats(h)
            appendLine("== $today")
            for (a in Coach.evaluate(h, plan, settings, today, emptyList())) appendLine("E ${advice(a)}")
            for (a in Coach.evaluate(h, plan, settings, today, stored(today))) appendLine("ES ${advice(a)}")
            for (day in DayOfWeek.entries) {
                for (a in Coach.targetsFor(day, h, plan, settings, today, emptyList())) appendLine("T $day ${advice(a)}")
                for (a in Coach.targetsFor(day, h, plan, settings, today, stored(today))) appendLine("TS $day ${advice(a)}")
            }
            for (s in FatigueRule.signals(stats, h, today)) appendLine("F ${s.text}")
            FatigueRule.evaluate(stats, h, today)?.let { appendLine("FE ${advice(it)}") }
            for (a in WeakPointRule.evaluate(stats, today)) appendLine("W ${advice(a)}")
            val accessories = plan.mapNotNull { h.exercises[it.exerciseId]?.name }.toSet()
            for (lift in SBD) {
                StallRule.evaluate(lift, stats, settings, today, accessories)?.let { appendLine("S ${advice(it)}") }
                StallRule.evaluate(lift, stats, settings, today, emptySet())?.let { appendLine("S0 ${advice(it)}") }
                appendLine("C $lift ${n(stats.currentE1rm(lift, today))} ${stats.sessionCount(lift)}")
                for (m in MissPoint.entries) appendLine("M $lift $m ${stats.missPoints(lift, start, today).count { it == m }}")
            }
            appendLine("TOTAL ${n(ProgressMath.total(stats, today, includeStrictPress = false))}")
        }
        val stats = HistoryStats(history)
        for (lift in SBD) {
            appendLine("TREND $lift " + stats.weeklyTrend(lift, start.plus(days.last, DateTimeUnit.DAY)).joinToString(" ") { "${it.weekStart}:${n(it.e1rmKg)}" })
            appendLine("PR $lift " + ProgressMath.prs(history, lift).joinToString(" ") { "${it.date}:${n(it.e1rmKg)}:${n(it.weightKg)}x${it.reps}@${n(it.rpe)}" })
            appendLine("VOL $lift " + ProgressMath.weeklyVolume(history, lift).joinToString(" ") { "${it.first}:${n(it.second)}" })
        }
    }

    /** 64-bit FNV-1a over the UTF-16 code units, printed in hex: the same on every platform. */
    fun hash(text: String): String {
        var h = -0x340d631b7bdddcdbL // 0xcbf29ce484222325
        for (c in text) {
            h = h xor c.code.toLong()
            h *= 0x100000001b3L
        }
        return h.toULong().toString(16)
    }

    /** Every seed row as id|name|category|parentLift|muscles|isBuiltIn|archived, one per line. */
    fun seed(): String = com.powertracker.domain.seed.SeedLibrary.exercises().joinToString("\n") {
        "${it.id}|${it.name}|${it.category}|${it.parentLift}|${it.muscles.joinToString(",")}|${it.isBuiltIn}|${it.archived}"
    }

    // ---- the pinned histories ----

    val goldenStart: LocalDate = GoldenScenario.day(0)

    fun golden(): String = render(
        GoldenScenario.history, GoldenScenario.plan, GoldenScenario.settings, goldenStart, 0..100,
    ) { today -> GoldenScenario.checkpoints.firstOrNull { it.today == today }?.storedAdvice.orEmpty() }

    /**
     * A second multi-lift history that reaches the rules the golden scenario does not: a low squat ÷ deadlift ratio,
     * repeated deadlift floor misses, RPE rising at the same load on two lifts, e1RM down on two lifts, a baseline,
     * accessories in the plan and change plates on.
     */
    val mixedHistory: TrainingHistory = history {
        baseline(Ids.SQUAT, 150.0)
        for (w in 0..7) {
            val late = w >= 6
            session(w * 7, readiness = if (late) 2 else 4) {
                squat(140.0, 5, rpe = if (late) 9.0 + (w - 6) else 7.5)
                bench(100.0 + (if (w < 5) 2.5 * w else 10.0 - 2.5 * (w - 5)), 5, rpe = if (late) 9.5 else 8.0)
                set(Ids.DB_PRESS, 30.0, 10, rpe = 8.0, isTopSet = true)
            }
            session(w * 7 + 2) {
                deadlift(200.0 + 5.0 * minOf(w, 4), 3, rpe = 8.5)
                if (w >= 4) set(Ids.DEADLIFT, 220.0, 1, rpe = 10.0, failed = true, miss = MissPoint.FLOOR)
                set(Ids.PULL_UP, 0.0, 8, rpe = 8.0, isTopSet = true)
            }
            session(w * 7 + 4) {
                squat(if (late) 130.0 else 135.0, 5, rpe = 8.0)
                set(Ids.PAUSE_BENCH, 85.0, 4, rpe = 8.0, isTopSet = true)
            }
        }
    }

    val mixedPlan: List<PlanItem> = listOf(
        PlanItem(1, DayOfWeek.MONDAY, Ids.SQUAT, 5, 5, 8.0, 0),
        PlanItem(2, DayOfWeek.MONDAY, Ids.BENCH, 4, 5, 8.0, 1),
        PlanItem(3, DayOfWeek.MONDAY, Ids.DB_PRESS, 3, 10, 8.0, 2),
        PlanItem(4, DayOfWeek.WEDNESDAY, Ids.DEADLIFT, 3, 3, 8.5, 0),
        PlanItem(5, DayOfWeek.WEDNESDAY, Ids.PULL_UP, 3, 8, 8.0, 1),
        PlanItem(6, DayOfWeek.FRIDAY, Ids.SQUAT, 3, 5, 8.0, 0),
        PlanItem(7, DayOfWeek.FRIDAY, Ids.PAUSE_BENCH, 3, 4, 8.0, 1),
    )

    fun mixed(): String = render(
        mixedHistory, mixedPlan, CoachSettings(changePlates = true), goldenStart, 0..63,
    ) { today -> listOf(Advice(AdviceType.DELOAD, null, today, "stored", 2, AdvicePayload.Deload())) }
}
