package com.powertracker.domain.coach

import com.powertracker.domain.CoachSnapshot
import com.powertracker.domain.ExpectedAdvice
import com.powertracker.domain.ExpectedTarget
import com.powertracker.domain.GoldenScenario
import com.powertracker.domain.Ids
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The powerlifting coach reads only the powerlifting side's sessions; a deload belongs to the lifter. */
class CoachSidesTest {
    /** Three sets of ten on bench, logged on the bodybuilding side. */
    private fun benchTens(id: Long, date: LocalDate) = LoggedSession(
        id, date, null, null,
        List(3) { LoggedSet(Ids.BENCH, 70.0, 10, 8.0, failed = false, missPoint = null, isTopSet = false) },
        TrainingMode.BODYBUILDING,
    )

    /** [history] with a bodybuilding bench session [after] days after each of [days] (all powerlifting days). */
    private fun withBodybuilding(history: TrainingHistory, start: LocalDate, days: List<Int>): TrainingHistory {
        val taken = history.sessions.map { it.date }.toSet()
        val extra = days.mapIndexed { i, d -> benchTens(1000L + i, start.plus(d, DateTimeUnit.DAY)) }
        extra.forEach { check(it.date !in taken) { "one session per date: ${it.date}" } }
        return history.copy(sessions = (history.sessions + extra).sortedBy { it.date })
    }

    private fun assertSameCoachOutput(
        name: String,
        plain: TrainingHistory,
        mixed: TrainingHistory,
        plan: List<PlanItem>,
        settings: CoachSettings,
        start: LocalDate,
        days: IntRange,
        stored: (LocalDate) -> List<Advice>,
    ) {
        for (d in days) {
            val today = start.plus(d, DateTimeUnit.DAY)
            val a = plain.copy(sessions = plain.sessions.filter { it.date <= today })
            val b = mixed.copy(sessions = mixed.sessions.filter { it.date <= today })
            for (prior in listOf(emptyList(), stored(today))) {
                assertEquals(
                    Coach.evaluate(a, plan, settings, today, prior),
                    Coach.evaluate(b, plan, settings, today, prior),
                    "$name evaluate on $today",
                )
                for (day in DayOfWeek.entries) {
                    assertEquals(
                        Coach.targetsFor(day, a, plan, settings, today, prior),
                        Coach.targetsFor(day, b, plan, settings, today, prior),
                        "$name targets for $day on $today",
                    )
                }
            }
        }
    }

    @Test
    fun bodybuildingBenchSessionsLeaveTheGoldenScenarioUnchanged() {
        // Two days after each Thursday deadlift, weeks 0 to 8: never inside the 21-day holiday, so the gap stays.
        val days = (0..8).map { it * 7 + 5 }
        val mixed = withBodybuilding(GoldenScenario.history, GoldenScenario.day(0), days)
        assertSameCoachOutput(
            "golden", GoldenScenario.history, mixed, GoldenScenario.plan, GoldenScenario.settings,
            GoldenScenario.day(0), 0..100,
        ) { today -> GoldenScenario.checkpoints.firstOrNull { it.today == today }?.storedAdvice.orEmpty() }
    }

    @Test
    fun bodybuildingBenchSessionsLeaveTheMixedHistoryUnchanged() {
        val days = (0..7).map { it * 7 + 5 }
        val mixed = withBodybuilding(CoachSnapshot.mixedHistory, CoachSnapshot.goldenStart, days)
        assertSameCoachOutput(
            "mixed", CoachSnapshot.mixedHistory, mixed, CoachSnapshot.mixedPlan, CoachSettings(changePlates = true),
            CoachSnapshot.goldenStart, 0..63,
        ) { today -> listOf(Advice(AdviceType.DELOAD, null, today, "stored", 2, AdvicePayload.Deload())) }
    }

    @Test
    fun anOpenDeloadMadeOnTheBodybuildingSideClampsAndBlocksOnThePowerliftingSide() {
        val cp = GoldenScenario.checkpoints.single { it.label == "fatigue" }
        val history = GoldenScenario.historyUntil(cp.today)
        val deload = cp.storedAdvice.single().copy(mode = TrainingMode.BODYBUILDING)

        val targets = Coach.targetsFor(cp.weekday, history, GoldenScenario.plan, GoldenScenario.settings, cp.today, listOf(deload))
        val actual = targets.map { (it.payload as AdvicePayload.Target).let { t -> ExpectedTarget(t.exerciseId, t.weightKg, t.reps, t.sets) } }
        assertEquals(cp.expectedTargets, actual)

        val advice = Coach.evaluate(history, GoldenScenario.plan, GoldenScenario.settings, cp.today, listOf(deload))
        assertTrue(advice.none { it.type == AdviceType.DELOAD }, "a second deload while one is open: $advice")
        assertEquals(listOf(ExpectedAdvice(AdviceType.STALL, MainLift.BENCH, 8)), advice.map { ExpectedAdvice(it.type, it.lift, it.severity) })
    }

    @Test
    fun aDeloadOnATargetTheReturnRuleEasedKeepsTheEasingNote() {
        // Bench on the powerlifting side on days 0, 7 and 14 (100 × 5 @ 8), then only on the bodybuilding side
        // (60 × 10 @ 8) until day 49. Day 56: the return rule caps the bench target and says why; a deload is open
        // and no re-entry is. The deload's clamp keeps the easing-back-in note after its own.
        val start = LocalDate(2026, 1, 5)
        val sessions = listOf(0, 7, 14).mapIndexed { i, d ->
            LoggedSession(
                i + 1L, start.plus(d, DateTimeUnit.DAY), null, null,
                listOf(LoggedSet(Ids.BENCH, 100.0, 5, 8.0, failed = false, missPoint = null, isTopSet = true)),
            )
        } + listOf(21, 28, 35, 42, 49).mapIndexed { i, d ->
            LoggedSession(
                10L + i, start.plus(d, DateTimeUnit.DAY), null, null,
                List(3) { LoggedSet(Ids.BENCH, 60.0, 10, 8.0, failed = false, missPoint = null, isTopSet = false) },
                TrainingMode.BODYBUILDING,
            )
        }
        val h = com.powertracker.domain.history(start) { }.copy(sessions = sessions)
        val today = start.plus(56, DateTimeUnit.DAY)
        val plan = listOf(PlanItem(1, DayOfWeek.MONDAY, Ids.BENCH, 5, 5, 8.0, 0))
        val eased = Coach.targetsFor(DayOfWeek.MONDAY, h, plan, CoachSettings(), today, emptyList()).single()
        assertTrue(eased.reason.endsWith(" Easing back in: lighter load."), eased.reason)
        val deload = Advice(AdviceType.DELOAD, null, today, "Signs of fatigue", 2, AdvicePayload.Deload())
        val advice = Coach.targetsFor(DayOfWeek.MONDAY, h, plan, CoachSettings(), today, listOf(deload)).single()
        assertTrue(
            advice.reason.endsWith("@ 8. Deload week: lighter load and fewer sets. Easing back in: lighter load."),
            advice.reason,
        )
        assertEquals(3, (advice.payload as AdvicePayload.Target).sets)
    }
}
