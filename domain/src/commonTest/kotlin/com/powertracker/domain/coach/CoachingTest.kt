package com.powertracker.domain.coach

import com.powertracker.domain.CoachSnapshot
import com.powertracker.domain.GoldenScenario
import com.powertracker.domain.Ids
import com.powertracker.domain.history
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
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
        // re-entry. The bodybuilding side gets the deload, never the stall. The golden log has no bodybuilding
        // session, so that side was never trained before the break and gets no re-entry (1.16); the same log kept on
        // the bodybuilding side gets it.
        for (cp in GoldenScenario.checkpoints) {
            val h = GoldenScenario.historyUntil(cp.today)
            val power = Coach.evaluate(h, GoldenScenario.plan, GoldenScenario.settings, cp.today, emptyList())
            val body = Coaching.evaluate(TrainingMode.BODYBUILDING, h, GoldenScenario.plan, GoldenScenario.settings, cp.today, emptyList())
            assertEquals(
                power.filter { it.type == AdviceType.DELOAD }.map { it.copy(mode = TrainingMode.BODYBUILDING) },
                body,
                cp.label,
            )
            assertTrue(body.all { it.mode == TrainingMode.BODYBUILDING })
        }
        val back = GoldenScenario.checkpoints.single { it.label == "return" }
        val onBody = GoldenScenario.historyUntil(back.today).let { h -> h.copy(sessions = h.sessions.map { it.copy(mode = TrainingMode.BODYBUILDING) }) }
        assertEquals(
            Coach.evaluate(GoldenScenario.historyUntil(back.today), GoldenScenario.plan, GoldenScenario.settings, back.today, emptyList())
                .filter { it.type == AdviceType.RE_ENTRY }.map { it.copy(mode = TrainingMode.BODYBUILDING) },
            Coaching.evaluate(TrainingMode.BODYBUILDING, onBody, GoldenScenario.plan, GoldenScenario.settings, back.today, emptyList())
                .filter { it.type == AdviceType.RE_ENTRY },
        )
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

    /** Four bodybuilding sessions in the last fortnight, each rated [readiness]; today is the day after the last. */
    private fun bodybuildingOnly(readiness: Int): Pair<TrainingHistory, LocalDate> {
        val start = LocalDate(2026, 1, 5)
        val h = history(start) {
            for (d in listOf(0, 3, 7, 10)) {
                session(d, readiness = readiness, mode = TrainingMode.BODYBUILDING) { repeat(3) { set(Ids.DB_PRESS, 26.0, 10) } }
            }
        }
        return h to start.plus(11, DateTimeUnit.DAY)
    }

    private val bbPlan = listOf(PlanItem(1, DayOfWeek.MONDAY, Ids.DB_PRESS, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12))

    @Test
    fun lowReadinessAloneGivesADeloadOnTheBodybuildingSide() {
        val (h, today) = bodybuildingOnly(readiness = 1)
        val advice = Coaching.evaluate(TrainingMode.BODYBUILDING, h, bbPlan, CoachSettings(), today, emptyList())
        val deload = advice.single()
        assertEquals(AdviceType.DELOAD, deload.type)
        assertEquals(TrainingMode.BODYBUILDING, deload.mode)
        assertEquals(AdvicePayload.Deload(), deload.payload)
        assertEquals(
            "Signs of fatigue: low readiness. Take a deload week: about 60% of the volume at about 90% of the load.",
            deload.reason,
        )
    }

    @Test
    fun lowReadinessAloneStillGivesNoDeloadOnThePowerliftingSide() {
        val (h, today) = bodybuildingOnly(readiness = 1)
        val advice = Coaching.evaluate(TrainingMode.POWERLIFTING, h, bbPlan, CoachSettings(), today, emptyList())
        assertTrue(advice.none { it.type == AdviceType.DELOAD }, "$advice")
        assertTrue(Coach.evaluate(h, bbPlan, CoachSettings(), today, emptyList()).none { it.type == AdviceType.DELOAD })
    }

    @Test
    fun normalReadinessGivesNoDeloadOnTheBodybuildingSide() {
        val (h, today) = bodybuildingOnly(readiness = 4)
        assertEquals(emptyList(), Coaching.evaluate(TrainingMode.BODYBUILDING, h, bbPlan, CoachSettings(), today, emptyList()))
    }

    @Test
    fun anActiveDeloadBlocksASecondReadinessDeload() {
        val (h, today) = bodybuildingOnly(readiness = 1)
        for (madeOn in TrainingMode.entries) {
            val open = Advice(AdviceType.DELOAD, null, today.minus(3, DateTimeUnit.DAY), "stored", 1, AdvicePayload.Deload(), mode = madeOn)
            val advice = Coaching.evaluate(TrainingMode.BODYBUILDING, h, bbPlan, CoachSettings(), today, listOf(open))
            assertTrue(advice.none { it.type == AdviceType.DELOAD }, "made on $madeOn: $advice")
        }
        // An applied one keeps its cooldown too.
        val applied = Advice(
            AdviceType.DELOAD, null, today.minus(10, DateTimeUnit.DAY), "stored", 1, AdvicePayload.Deload(),
            AdviceStatus.APPLIED,
        )
        assertTrue(
            Coaching.evaluate(TrainingMode.BODYBUILDING, h, bbPlan, CoachSettings(), today, listOf(applied)).none { it.type == AdviceType.DELOAD },
        )
    }

    @Test
    fun welcomeBackAfterMoreThanThirtyDaysWithAPlan() {
        val today = LocalDate(2026, 11, 13)
        val trained = today.minus(60, DateTimeUnit.DAY)
        assertFalse(Coaching.welcomeBack(null, today, hasPlan = true, firstSessionOn = trained))
        assertFalse(Coaching.welcomeBack(today.minus(30, DateTimeUnit.DAY), today, hasPlan = true, firstSessionOn = trained))
        assertTrue(Coaching.welcomeBack(today.minus(31, DateTimeUnit.DAY), today, hasPlan = true, firstSessionOn = trained))
        assertFalse(Coaching.welcomeBack(today.minus(31, DateTimeUnit.DAY), today, hasPlan = false, firstSessionOn = trained))
        assertFalse(Coaching.welcomeBack(today, today, hasPlan = true, firstSessionOn = trained))
    }

    @Test
    fun noWelcomeBackToASideNeverTrainedBeforeItWasLeft() {
        val today = LocalDate(2026, 11, 13)
        val left = today.minus(40, DateTimeUnit.DAY)
        // Left in Settings before any session on it: no card, however long ago.
        assertFalse(Coaching.welcomeBack(left, today, hasPlan = true, firstSessionOn = null))
        // Its first session came after it was left (on the return, the card not yet answered): still no card, today
        // or on any later day.
        assertFalse(Coaching.welcomeBack(left, today, hasPlan = true, firstSessionOn = today))
        assertFalse(Coaching.welcomeBack(left, today, hasPlan = true, firstSessionOn = today.minus(1, DateTimeUnit.DAY)))
        assertFalse(Coaching.welcomeBack(left, today, hasPlan = true, firstSessionOn = left.plus(1, DateTimeUnit.DAY)))
        // A session on the day it was left, or before, is a side trained and left: the card as before.
        assertTrue(Coaching.welcomeBack(left, today, hasPlan = true, firstSessionOn = left))
        assertTrue(Coaching.welcomeBack(left, today, hasPlan = true, firstSessionOn = left.minus(5, DateTimeUnit.DAY)))
    }

    @Test
    fun theFirstSessionOnASideIsItsEarliestSessionOnThatSide() {
        val h = history {
            session(3, mode = TrainingMode.BODYBUILDING) { set(Ids.CURL, 12.0, 10) }
            session(5) { squat(100.0, 5) }
            session(1, mode = TrainingMode.BODYBUILDING) { set(Ids.CURL, 12.0, 10) }
        }
        val start = LocalDate(2026, 1, 5)
        assertEquals(start.plus(1, DateTimeUnit.DAY), Coaching.firstSessionOn(TrainingMode.BODYBUILDING, h))
        assertEquals(start.plus(5, DateTimeUnit.DAY), Coaching.firstSessionOn(TrainingMode.POWERLIFTING, h))
        assertEquals(null, Coaching.firstSessionOn(TrainingMode.POWERLIFTING, history { session(0, mode = TrainingMode.BODYBUILDING) { set(Ids.CURL, 12.0, 10) } }))
    }
}

/** Stage 3 through the one entry point: check-ins, due, and stuck exercises on the bodybuilding side only. */
class CoachingCheckInTest {
    private val today = LocalDate(2026, 11, 14)
    private val bbPlan = PhysiqueFixtures.default3
    private fun ago(days: Int) = today.minus(days, DateTimeUnit.DAY)

    private fun bbSession(daysAgo: Int, kg: Double = 40.0, mode: TrainingMode = TrainingMode.BODYBUILDING) = com.powertracker.domain.model.LoggedSession(
        100L - daysAgo, ago(daysAgo), null, null,
        List(3) { com.powertracker.domain.model.LoggedSet(PhysiqueFixtures.idOf("Leg extension"), kg, 10, 8.0, false, null, false) }, mode,
    )

    @Test
    fun aCheckInIsDueTwentyEightDaysAfterTheLaterOfTheLastCheckInAndTheFirstSession() {
        val h = PhysiqueFixtures.history(listOf(bbSession(28), bbSession(3)))
        assertFalse(Coaching.checkInDue(h, emptyList(), emptyList(), today), "no plan")
        assertFalse(Coaching.checkInDue(h, bbPlan.map { it.copy(mode = TrainingMode.POWERLIFTING) }, emptyList(), today), "no bodybuilding plan")
        assertFalse(Coaching.checkInDue(PhysiqueFixtures.history(listOf(bbSession(40, mode = TrainingMode.POWERLIFTING))), bbPlan, emptyList(), today), "no session")
        assertTrue(Coaching.checkInDue(h, bbPlan, emptyList(), today), "28 days after the first session")
        assertFalse(Coaching.checkInDue(PhysiqueFixtures.history(listOf(bbSession(27))), bbPlan, emptyList(), today), "27 days")
        val checkIn = com.powertracker.domain.model.CheckIn(ago(27), emptySet(), emptySet())
        assertFalse(Coaching.checkInDue(h, bbPlan, listOf(checkIn), today), "27 days after the check-in")
        assertTrue(Coaching.checkInDue(h, bbPlan, listOf(checkIn.copy(date = ago(28))), today), "28 days after the check-in")
        // The later of the two: a check-in older than the first session does not make it due sooner.
        assertFalse(Coaching.checkInDue(PhysiqueFixtures.history(listOf(bbSession(20))), bbPlan, listOf(checkIn.copy(date = ago(60))), today))
    }

    @Test
    fun afterCheckInReadsOnlyTheBodybuildingPlanAndStampsTheSide() {
        val checkIn = com.powertracker.domain.model.CheckIn(today, setOf(com.powertracker.domain.library.MuscleGroup.CHEST), emptySet())
        val power = bbPlan.map { it.copy(id = it.id + 1000, mode = TrainingMode.POWERLIFTING, sets = 1) }
        val out = Coaching.afterCheckIn(checkIn, PhysiqueFixtures.history(), bbPlan + power, today)
        assertEquals(PhysiqueCoach.afterCheckIn(checkIn, PhysiqueFixtures.history(), bbPlan, today), out)
        assertTrue(out.advice.isNotEmpty() && out.advice.all { it.mode == TrainingMode.BODYBUILDING })
        // A powerlifting side's MORE_WORK cannot exist, but its other advice is never dismissed.
        val stall = Advice(AdviceType.STALL, null, today, "s", 1, AdvicePayload.Accessories(emptyList()))
        assertEquals(emptyList(), Coaching.afterCheckIn(checkIn, PhysiqueFixtures.history(), bbPlan, today, listOf(stall)).dismissed)
    }

    @Test
    fun bodybuildingEvaluateAddsStuckExercises() {
        val h = PhysiqueFixtures.history(listOf(bbSession(28), bbSession(14), bbSession(7)))
        val advice = Coaching.evaluate(TrainingMode.BODYBUILDING, h, bbPlan, CoachSettings(), today, emptyList())
        assertEquals(listOf(AdviceType.SWAP), advice.map { it.type })
        assertEquals(PhysiqueCoach.stuck(h, bbPlan, emptyList(), emptyList(), today), advice)
        val growing = com.powertracker.domain.model.CheckIn(ago(2), emptySet(), setOf(com.powertracker.domain.library.MuscleGroup.QUADS))
        assertEquals(emptyList(), Coaching.evaluate(TrainingMode.BODYBUILDING, h, bbPlan, CoachSettings(), today, emptyList(), listOf(growing)))
        // The powerlifting side never gets one.
        assertTrue(Coaching.evaluate(TrainingMode.POWERLIFTING, h, bbPlan, CoachSettings(), today, emptyList()).none { it.type == AdviceType.SWAP })
    }

    @Test
    fun aDeloadMadeInTheSameEvaluationHoldsBackTheSwaps() {
        // Four low-readiness sessions give a readiness deload on the bodybuilding side; the flat leg extension waits.
        val sessions = listOf(28, 14, 7, 3, 1).map { d -> bbSession(d).copy(readiness = 1) }
        val h = PhysiqueFixtures.history(sessions)
        val advice = Coaching.evaluate(TrainingMode.BODYBUILDING, h, bbPlan, CoachSettings(), today, emptyList())
        assertEquals(listOf(AdviceType.DELOAD), advice.map { it.type })
    }

    @Test
    fun thePowerliftingSideIgnoresCheckIns() {
        val checkIns = listOf(
            com.powertracker.domain.model.CheckIn(GoldenScenario.day(10), com.powertracker.domain.library.MuscleGroup.entries.take(6).toSet(), com.powertracker.domain.library.MuscleGroup.entries.drop(10).toSet()),
        )
        for (cp in GoldenScenario.checkpoints) {
            val h = GoldenScenario.historyUntil(cp.today)
            assertEquals(
                Coaching.evaluate(TrainingMode.POWERLIFTING, h, GoldenScenario.plan, GoldenScenario.settings, cp.today, cp.storedAdvice),
                Coaching.evaluate(TrainingMode.POWERLIFTING, h, GoldenScenario.plan, GoldenScenario.settings, cp.today, cp.storedAdvice, checkIns),
                cp.label,
            )
        }
    }
}
