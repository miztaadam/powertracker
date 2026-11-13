package com.powertracker.domain.coach

import com.powertracker.domain.CoachSnapshot
import com.powertracker.domain.GoldenScenario
import com.powertracker.domain.Ids
import com.powertracker.domain.history
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The one entry point both apps call: the powerlifting side through it is exactly the coach it always was. */
class CoachingTest {
    private fun assertSameAsCoach(
        name: String,
        history: TrainingHistory,
        plan: List<PlanItem>,
        settings: CoachSettings,
        start: LocalDate,
        days: IntRange,
        stored: (LocalDate) -> List<Advice>,
    ) {
        for (d in days) {
            val today = start.plus(d, DateTimeUnit.DAY)
            val h = history.copy(sessions = history.sessions.filter { it.date <= today })
            for (prior in listOf(emptyList(), stored(today))) {
                assertEquals(
                    Coach.evaluate(h, plan, settings, today, prior),
                    Coaching.evaluate(TrainingMode.POWERLIFTING, h, plan, settings, today, prior),
                    "$name evaluate on $today",
                )
                for (day in DayOfWeek.entries) {
                    assertEquals(
                        Coach.targetsFor(day, h, plan, settings, today, prior),
                        Coaching.targetsFor(TrainingMode.POWERLIFTING, day, h, plan, settings, today, prior),
                        "$name targets for $day on $today",
                    )
                }
            }
        }
    }

    @Test
    fun theGoldenScenarioThroughCoachingIsTheCoachsOwnOutput() {
        assertSameAsCoach(
            "golden", GoldenScenario.history, GoldenScenario.plan, GoldenScenario.settings, GoldenScenario.day(0), 0..100,
        ) { today -> GoldenScenario.checkpoints.firstOrNull { it.today == today }?.storedAdvice.orEmpty() }
    }

    @Test
    fun theMixedHistoryThroughCoachingIsTheCoachsOwnOutput() {
        assertSameAsCoach(
            "mixed", CoachSnapshot.mixedHistory, CoachSnapshot.mixedPlan, CoachSettings(changePlates = true),
            CoachSnapshot.goldenStart, 0..63,
        ) { today -> listOf(Advice(AdviceType.DELOAD, null, today, "stored", 2, AdvicePayload.Deload())) }
    }

    @Test
    fun theOtherSidesPlanAndAdviceAreLeftOut() {
        val cp = GoldenScenario.checkpoints.single { it.label == "stall" }
        val h = GoldenScenario.historyUntil(cp.today)
        val bbItem = PlanItem(99, DayOfWeek.MONDAY, Ids.DB_PRESS, 3, 8, 8.0, 9, TrainingMode.BODYBUILDING, 12)
        val bbStall = Advice(AdviceType.STALL, null, cp.today, "other side", 3, AdvicePayload.Deload(), mode = TrainingMode.BODYBUILDING)
        assertEquals(
            Coach.targetsFor(cp.weekday, h, GoldenScenario.plan, GoldenScenario.settings, cp.today, emptyList()),
            Coaching.targetsFor(TrainingMode.POWERLIFTING, cp.weekday, h, GoldenScenario.plan + bbItem, GoldenScenario.settings, cp.today, listOf(bbStall)),
        )
        assertEquals(
            Coach.evaluate(h, GoldenScenario.plan, GoldenScenario.settings, cp.today, emptyList()),
            Coaching.evaluate(TrainingMode.POWERLIFTING, h, GoldenScenario.plan + bbItem, GoldenScenario.settings, cp.today, listOf(bbStall)),
        )
    }

    @Test
    fun bodybuildingTargetsCarryTheirSide() {
        val start = LocalDate(2026, 1, 5)
        val h = history(start) {
            session(0, mode = TrainingMode.BODYBUILDING) { repeat(3) { set(Ids.DB_PRESS, 26.0, 10) } }
            session(1) { bench(100.0, 5) }
        }
        val plan = listOf(
            PlanItem(1, DayOfWeek.MONDAY, Ids.DB_PRESS, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12),
            PlanItem(2, DayOfWeek.MONDAY, Ids.BENCH, 3, 5, 8.0, 1),
        )
        val today = start.plus(7, DateTimeUnit.DAY)
        val targets = Coaching.targetsFor(TrainingMode.BODYBUILDING, DayOfWeek.MONDAY, h, plan, CoachSettings(), today, emptyList())
        assertEquals(listOf(Ids.DB_PRESS), targets.map { (it.payload as AdvicePayload.Target).exerciseId })
        assertTrue(targets.all { it.mode == TrainingMode.BODYBUILDING })
    }

    @Test
    fun bodybuildingEvaluateGivesOnlyReEntryAndDeloadStampedWithItsSide() {
        // The golden scenario's fatigue checkpoint gives a deload and a bench stall; on the return checkpoint a
        // re-entry. The bodybuilding side gets the deload and the re-entry, never the stall.
        for (cp in GoldenScenario.checkpoints) {
            val h = GoldenScenario.historyUntil(cp.today)
            val power = Coach.evaluate(h, GoldenScenario.plan, GoldenScenario.settings, cp.today, emptyList())
            val body = Coaching.evaluate(TrainingMode.BODYBUILDING, h, GoldenScenario.plan, GoldenScenario.settings, cp.today, emptyList())
            assertEquals(
                power.filter { it.type == AdviceType.DELOAD || it.type == AdviceType.RE_ENTRY }.map { it.copy(mode = TrainingMode.BODYBUILDING) },
                body,
                cp.label,
            )
            assertTrue(body.all { it.mode == TrainingMode.BODYBUILDING })
        }
        val fatigue = GoldenScenario.checkpoints.single { it.label == "fatigue" }
        assertEquals(
            listOf(AdviceType.DELOAD),
            Coaching.evaluate(
                TrainingMode.BODYBUILDING, GoldenScenario.historyUntil(fatigue.today), emptyList(), GoldenScenario.settings,
                fatigue.today, emptyList(),
            ).map { it.type },
        )
    }

    @Test
    fun anOpenDeloadFromEitherSideBlocksASecondOne() {
        val fatigue = GoldenScenario.checkpoints.single { it.label == "fatigue" }
        val h = GoldenScenario.historyUntil(fatigue.today)
        for (madeOn in TrainingMode.entries) {
            val open = fatigue.storedAdvice.single().copy(mode = madeOn)
            for (side in TrainingMode.entries) {
                val advice = Coaching.evaluate(side, h, GoldenScenario.plan, GoldenScenario.settings, fatigue.today, listOf(open))
                assertTrue(advice.none { it.type == AdviceType.DELOAD }, "made on $madeOn, asked on $side: $advice")
            }
        }
    }

    @Test
    fun welcomeBackAfterMoreThanThirtyDaysWithAPlan() {
        val today = LocalDate(2026, 11, 13)
        assertFalse(Coaching.welcomeBack(null, today, hasPlan = true))
        assertFalse(Coaching.welcomeBack(today.minus(30, DateTimeUnit.DAY), today, hasPlan = true))
        assertTrue(Coaching.welcomeBack(today.minus(31, DateTimeUnit.DAY), today, hasPlan = true))
        assertFalse(Coaching.welcomeBack(today.minus(31, DateTimeUnit.DAY), today, hasPlan = false))
        assertFalse(Coaching.welcomeBack(today, today, hasPlan = true))
    }
}
