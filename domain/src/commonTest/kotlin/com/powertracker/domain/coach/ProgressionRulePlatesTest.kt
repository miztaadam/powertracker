package com.powertracker.domain.coach

import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** 1.17: the powerlifting side's rule on a plate-counted exercise (Lat pulldown, plan 3 × 8 @ 8). */
class ProgressionRulePlatesTest {
    private val p = PlateFixtures

    /** The target for Lat pulldown after [sessions] (powerlifting), with plates set to [count]. */
    private fun after(vararg sessions: LoggedSession, count: PlateCount = PlateCount.WHOLE, item: PlanItem = p.plItem, skips: List<Skip> = emptyList()) =
        assertNotNull(p.target(TrainingMode.POWERLIFTING, p.history(count, *sessions), item, skips = skips))

    private fun pl(daysAgo: Int, vararg sets: LoggedSet) = p.pl(daysAgo, *sets)
    private fun plate(plate: Double, reps: Int, rpe: Double? = 8.0, failed: Boolean = false) = p.plate(plate, reps, rpe, failed)

    private fun assertTarget(plate: Double, reps: Int, advice: Advice) {
        val t = advice.payload as AdvicePayload.Target
        assertEquals(plate to reps, t.weightKg to t.reps, advice.reason)
    }

    private val plan12 = p.plItem.copy(reps = 12)
    private val plan15 = p.plItem.copy(reps = 15)

    @Test
    fun onTarget_addsARep() {
        val a = after(pl(3, plate(7.0, 8)))
        assertTarget(7.0, 9, a)
        assertEquals(LoadUnit.PLATES, (a.payload as AdvicePayload.Target).unit)
        assertEquals("Last time: plate 7 × 8 @ 8. Coach suggests plate 7 × 9 @ 8.", a.reason)
        assertTarget(7.0, 12, after(pl(3, plate(7.0, 11))))
        assertTarget(7.0, 9, after(pl(3, plate(7.0, 8, rpe = 8.5))))
        assertTarget(7.0, 9, after(pl(3, plate(7.0, 8, rpe = 6.5))))
    }

    @Test
    fun atTwelveReps_goesUpOneStep_backToThePlannedReps() {
        assertTarget(8.0, 8, after(pl(3, plate(7.0, 12))))
        assertTarget(8.0, 8, after(pl(3, plate(7.0, 12, rpe = 7.0))))
        val half = after(pl(3, plate(7.0, 12)), count = PlateCount.HALF)
        assertTarget(7.5, 8, half)
        assertTrue(half.reason.endsWith("Coach suggests plate 7½ × 8 @ 8."), half.reason)
    }

    @Test
    fun aPlanOfTwelveOrMoreReps_earnsTheStepEachTime() {
        assertTarget(8.0, 12, after(pl(3, plate(7.0, 12)), item = plan12))
        assertTarget(8.0, 15, after(pl(3, plate(7.0, 15)), item = plan15))
    }

    @Test
    fun oneStepIsNotCappedAtFivePercent() {
        assertTarget(3.0, 8, after(pl(3, plate(2.0, 12))))
    }

    @Test
    fun aLittleTooHard_repeats() {
        assertTarget(7.0, 8, after(pl(3, plate(7.0, 8, rpe = 9.0))))
        assertTarget(7.0, 10, after(pl(3, plate(7.0, 10, rpe = 9.0))))
    }

    @Test
    fun aRecentStepUpWithRpeRising_repeats() {
        assertTarget(7.0, 8, after(pl(13, plate(6.0, 12)), pl(3, plate(7.0, 8, rpe = 8.5))))
        // An easier top set than planned (delta −1) checks no recent jump, as in kg.
        assertTarget(7.0, 9, after(pl(13, plate(6.0, 12, rpe = 6.5)), pl(3, plate(7.0, 8, rpe = 7.0))))
    }

    @Test
    fun farTooHard_stepsDown() {
        assertTarget(6.0, 8, after(pl(3, plate(7.0, 8, rpe = 9.5))))
        assertTarget(6.5, 8, after(pl(3, plate(7.0, 8, rpe = 9.5)), count = PlateCount.HALF))
    }

    @Test
    fun aMissedTopSet_stepsDown() {
        val a = after(pl(3, plate(7.0, 8, failed = true)))
        assertTarget(6.0, 8, a)
        assertTrue(a.reason.startsWith("Last time: plate 7 × 8 (missed)."), a.reason)
        assertTarget(6.5, 8, after(pl(3, plate(7.0, 8, failed = true)), count = PlateCount.HALF))
        assertTarget(1.0, 8, after(pl(3, plate(1.0, 8, failed = true))))
    }

    @Test
    fun halfStepsTurnedOffAfterAHalf() {
        assertTarget(7.0, 10, after(pl(3, plate(7.5, 9))))
        assertTarget(8.0, 8, after(pl(3, plate(7.5, 12))))
        assertTarget(7.0, 8, after(pl(3, plate(7.5, 8, rpe = 9.0))))
        assertTarget(7.0, 8, after(pl(3, plate(7.5, 8, failed = true))))
    }

    @Test
    fun belowThePlannedReps_aimsAtThePlan() {
        assertTarget(7.0, 8, after(pl(3, plate(7.0, 6))))
    }

    @Test
    fun noReason_putsPlateAfterTheAt() {
        // PlateFixtures.target asserts this on every target made in this file, CoachPlatesTest and
        // BodybuildingCoachPlatesTest; here every case of this file is run once more in one place.
        val cases = listOf(
            after(pl(3, plate(7.0, 8))),
            after(pl(3, plate(7.0, 12)), count = PlateCount.HALF),
            after(pl(3, plate(7.5, 9))),
            after(pl(3, plate(7.0, 8, rpe = 9.0))),
            after(pl(3, plate(7.0, 8, rpe = 9.5)), count = PlateCount.HALF),
            after(pl(3, plate(7.0, 8, failed = true))),
            after(pl(3, plate(7.0, 8, rpe = null))),
            after(pl(3, plate(7.0, 12)), skips = listOf(p.painSkip(2))),
        )
        for (a in cases) {
            assertFalse("@ plate" in a.reason, a.reason)
            assertTrue(a.reason.startsWith("Last time: plate "), a.reason)
        }
    }

    @Test
    fun underAPainHold_noStepAndNoExtraRep() {
        val pain = listOf(p.painSkip(2))
        assertTarget(7.0, 12, after(pl(3, plate(7.0, 12)), skips = pain))
        val held = after(pl(3, plate(7.0, 9)), skips = pain)
        assertTarget(7.0, 9, held)
        assertEquals("Last time: plate 7 × 9 @ 8. Coach suggests plate 7 × 9 @ 8.", held.reason)
        assertTarget(6.0, 8, after(pl(3, plate(7.0, 8, failed = true)), skips = pain))
        // The exercise's own step is used: half steps keep the logged 7½ (one whole step would drop it to 7).
        assertTarget(7.5, 9, after(pl(3, plate(7.5, 9)), count = PlateCount.HALF, skips = pain))
    }
}

/** Lat pulldown counting plates, on either side, through [Coaching.targetsFor]; shared by the 1.17 coach tests. */
internal object PlateFixtures {
    val today = LocalDate(2026, 11, 20)
    val pulldown = PhysiqueFixtures.idOf("Lat pulldown")

    /** A catalogue accessory that stays in kg. */
    val legExtension = PhysiqueFixtures.idOf("Leg extension")

    val plItem = PlanItem(1, today.dayOfWeek, pulldown, 3, 8, 8.0, 0)
    val bbItem = PlanItem(2, today.dayOfWeek, pulldown, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12)

    fun exercises(count: PlateCount) =
        PhysiqueFixtures.exercises + (pulldown to PhysiqueFixtures.exercises.getValue(pulldown).copy(plateCount = count))

    fun history(count: PlateCount, vararg sessions: LoggedSession, baselines: List<Baseline> = emptyList()) =
        TrainingHistory(sessions.toList(), exercises(count), baselines)

    fun plate(plate: Double, reps: Int, rpe: Double? = 8.0, failed: Boolean = false, id: Long = pulldown) =
        LoggedSet(id, plate, reps, rpe, failed, null, false, LoadUnit.PLATES)

    fun kg(weight: Double, reps: Int, rpe: Double? = 8.0, failed: Boolean = false, id: Long = pulldown) =
        LoggedSet(id, weight, reps, rpe, failed, null, false)

    fun day(daysAgo: Int): LocalDate = today.minus(daysAgo, DateTimeUnit.DAY)

    fun pl(daysAgo: Int, vararg sets: LoggedSet) =
        LoggedSession(daysAgo.toLong() + 1, day(daysAgo), null, null, sets.toList(), TrainingMode.POWERLIFTING)

    fun bb(daysAgo: Int, vararg sets: LoggedSet) =
        LoggedSession(daysAgo.toLong() + 1001, day(daysAgo), null, null, sets.toList(), TrainingMode.BODYBUILDING)

    fun painSkip(daysAgo: Int, mode: TrainingMode = TrainingMode.POWERLIFTING) =
        Skip(1, day(daysAgo), mode, pulldown, if (mode == TrainingMode.POWERLIFTING) plItem.id else bbItem.id, SkipReason.PAIN)

    fun deload(daysAgo: Int = 0) = Advice(AdviceType.DELOAD, null, day(daysAgo), "Deload", 1, AdvicePayload.Deload())

    fun reEntry(daysAgo: Int = 0) = Advice(AdviceType.RE_ENTRY, null, day(daysAgo), "Welcome back", 1, AdvicePayload.ReEntry())

    /** Lat pulldown's target on [mode]'s side, or null; any target made is checked to be in plates and worded as plates. */
    fun target(
        mode: TrainingMode,
        h: TrainingHistory,
        item: PlanItem = if (mode == TrainingMode.POWERLIFTING) plItem else bbItem,
        prior: List<Advice> = emptyList(),
        skips: List<Skip> = emptyList(),
        settings: CoachSettings = CoachSettings(),
    ): Advice? {
        fun run(s: CoachSettings) = Coaching.targetsFor(mode, today.dayOfWeek, h, listOf(item), s, today, prior, skips)
            .singleOrNull { (it.payload as AdvicePayload.Target).exerciseId == pulldown }
        val a = run(settings) ?: return null
        assertInPlates(a)
        // Change plates (0.5 and 0.25 kg) is a kg setting: no plate target moves with it.
        assertEquals(a, run(settings.copy(changePlates = !settings.changePlates)), "changePlates moved: ${a.reason}")
        return a
    }

    fun assertInPlates(a: Advice) {
        assertEquals(LoadUnit.PLATES, (a.payload as AdvicePayload.Target).unit, a.reason)
        assertTrue("Coach suggests plate " in a.reason, a.reason)
        assertFalse("@ plate" in a.reason, a.reason)
    }
}
