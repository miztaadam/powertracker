package com.powertracker.domain.coach

import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.validation.InputValidator
import com.powertracker.domain.validation.InputWarning
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 1.17: the bodybuilding side's double progression on a plate-counted exercise (plan 3 × 8–12 @ 8). */
class BodybuildingCoachPlatesTest {
    private val p = PlateFixtures

    private fun run(h: TrainingHistory, prior: List<Advice> = emptyList(), skips: List<Skip> = emptyList(), settings: CoachSettings = CoachSettings()) =
        p.target(TrainingMode.BODYBUILDING, h, prior = prior, skips = skips, settings = settings)

    private fun after(vararg sessions: LoggedSession, count: PlateCount = PlateCount.WHOLE, prior: List<Advice> = emptyList(), skips: List<Skip> = emptyList()) =
        assertNotNull(run(p.history(count, *sessions), prior, skips))

    private fun t(a: Advice) = a.payload as AdvicePayload.Target

    /** Three plate sets at [plate] with [reps], @ 8 unless said, three days ago. */
    private fun last(plate: Double, vararg reps: Int, rpe: Double? = 8.0) =
        p.bb(3, *reps.map { p.plate(plate, it, rpe) }.toTypedArray())

    private fun bbSets(daysAgo: Int, vararg sets: LoggedSet) = p.bb(daysAgo, *sets)

    @Test
    fun everySetAtTheTop_goesUpOneStep() {
        val a = after(last(7.0, 12, 12, 12))
        assertEquals(8.0, t(a).weightKg)
        assertEquals(8, t(a).reps)
        assertEquals(12, t(a).repsHigh)
        assertEquals(LoadUnit.PLATES, t(a).unit)
        assertEquals("Last time: plate 7 × 12, 12, 12. Time to add weight. Coach suggests plate 8 × 8–12 @ 8.", a.reason)

        val half = after(last(7.0, 12, 12, 12), count = PlateCount.HALF)
        assertEquals(7.5, t(half).weightKg)
        assertEquals("Last time: plate 7 × 12, 12, 12. Time to add weight. Coach suggests plate 7½ × 8–12 @ 8.", half.reason)
    }

    @Test
    fun otherwise_samePlate_beatIt() {
        val a = after(last(7.0, 12, 12, 11))
        assertEquals(7.0, t(a).weightKg)
        assertEquals("Last time: plate 7 × 12, 12, 11. Beat it. Coach suggests plate 7 × 8–12 @ 8.", a.reason)
    }

    @Test
    fun aMissedSet_orASetOverTheTargetRpe_earnsNothing() {
        val missed = after(bbSets(3, p.plate(7.0, 12), p.plate(7.0, 12), p.plate(7.0, 12, failed = true)))
        assertEquals(7.0, t(missed).weightKg)
        assertTrue(missed.reason.startsWith("Last time: plate 7 × 12, 12, 12 (missed). Beat it."), missed.reason)
        val hard = after(bbSets(3, p.plate(7.0, 12), p.plate(7.0, 12), p.plate(7.0, 12, rpe = 9.0)))
        assertEquals(7.0, t(hard).weightKg)
        assertTrue("Beat it." in hard.reason, hard.reason)
    }

    @Test
    fun aSetWithNoRpe_countsAsOnTarget() {
        assertEquals(8.0, t(after(last(7.0, 12, 12, 12, rpe = null))).weightKg)
    }

    @Test
    fun underAPainHold_noStep_andRepsToBeatCarriesOn() {
        val pain = listOf(p.painSkip(2, TrainingMode.BODYBUILDING))
        val session = last(7.0, 12, 12, 12)
        val a = after(session, skips = pain)
        assertEquals(7.0, t(a).weightKg)
        assertEquals("Last time: plate 7 × 12, 12, 12. Beat it. Coach suggests plate 7 × 8–12 @ 8.", a.reason)
        val h = p.history(PlateCount.WHOLE, session)
        assertEquals(12, BodybuildingCoach.repsToBeat(p.bbItem, h, 1, p.today, pain))
        assertEquals(8, BodybuildingCoach.repsToBeat(p.bbItem, h, 1, p.today))
    }

    @Test
    fun differentPlatesInOneSession_areEachNamed() {
        val a = after(bbSets(3, p.plate(7.0, 12), p.plate(7.5, 10), p.plate(7.5, 9)), count = PlateCount.HALF)
        assertEquals(7.5, t(a).weightKg)
        assertEquals("Last time: plate 7 × 12, plate 7½ × 10, plate 7½ × 9. Beat it. Coach suggests plate 7½ × 8–12 @ 8.", a.reason)
    }

    @Test
    fun halfStepsTurnedOff_repeatRoundsDown_increaseGoesToTheNextWholePlate() {
        val repeat = after(last(7.5, 12, 12, 11))
        assertEquals(7.0, t(repeat).weightKg)
        assertEquals("Last time: plate 7½ × 12, 12, 11. Beat it. Coach suggests plate 7 × 8–12 @ 8.", repeat.reason)
        assertEquals(8.0, t(after(last(7.5, 12, 12, 12))).weightKg)
    }

    @Test
    fun deloadAndEasingBackIn_useTheStepRule() {
        val beatIt = last(7.0, 12, 12, 11)
        val deload = after(beatIt, prior = listOf(p.deload()))
        assertEquals(6.0, t(deload).weightKg)
        assertEquals(2, t(deload).sets)
        assertEquals(8 to 12, t(deload).reps to t(deload).repsHigh)
        assertEquals("Last time: plate 7 × 12, 12, 11. Beat it. Coach suggests plate 6 × 8–12 @ 8." + Coach.DELOAD_NOTE, deload.reason)

        // The same session 20 days ago: the first day back after a break, on a side trained before it.
        val beforeABreak = p.bb(20, p.plate(7.0, 12), p.plate(7.0, 12), p.plate(7.0, 11))
        val easing = after(beforeABreak)
        assertEquals(5.0, t(easing).weightKg)
        assertEquals("Last time: plate 7 × 12, 12, 11. Beat it. Coach suggests plate 5 × 8–12 @ 8." + Coach.RE_ENTRY_NOTE, easing.reason)
        assertEquals(5.0, t(after(beforeABreak, prior = listOf(p.deload()))).weightKg)

        // An earned step cut back by the deload: "Time to add weight." is dropped.
        val earned = after(last(7.0, 12, 12, 12), prior = listOf(p.deload()))
        assertEquals(6.0, t(earned).weightKg)
        assertEquals("Last time: plate 7 × 12, 12, 12. Coach suggests plate 6 × 8–12 @ 8." + Coach.DELOAD_NOTE, earned.reason)
    }

    /**
     * With half steps the two readings differ: the lower of the two caps from plate 7 is the easing cap,
     * eased(7, 0.85, ½) = 5½ (the deload's is eased(7, 0.9, ½) = 6), while the caps compounding would give
     * eased(6, 0.85, ½) = 5.
     */
    @Test
    fun deloadAndEasingBackIn_withHalfSteps_takeTheLowerCapFromPlateSeven_notCompounded() {
        val beforeABreak = p.bb(20, p.plate(7.0, 12), p.plate(7.0, 12), p.plate(7.0, 11))
        val a = after(beforeABreak, count = PlateCount.HALF, prior = listOf(p.deload()))
        assertEquals(5.5, t(a).weightKg)
        assertEquals(2, t(a).sets)
        assertEquals(8 to 12, t(a).reps to t(a).repsHigh)
    }

    @Test
    fun firstTimeOnThisSide_takesTheHeaviestGoodPlateFromTheOtherSide() {
        val a = after(
            p.pl(10, p.plate(8.0, 8)),
            p.pl(5, p.plate(9.0, 8)),
            p.pl(3, p.plate(10.0, 8, failed = true)),
        )
        assertEquals(9.0, t(a).weightKg)
        assertEquals("First time on this side. Coach suggests plate 9 × 8–12 @ 8.", a.reason)
        // No e1RM conversion: the reps there do not change the plate.
        val fewReps = after(p.pl(10, p.plate(8.0, 12)), p.pl(5, p.plate(9.0, 3)), p.pl(3, p.plate(10.0, 8, failed = true)))
        assertEquals(9.0, t(fewReps).weightKg)
    }

    @Test
    fun firstTime_fromOldPlateSets_isEasedOnce() {
        val old = p.pl(40, p.plate(9.0, 8))
        assertEquals(7.0, t(after(old)).weightKg)
        assertEquals(7.5, t(after(old, count = PlateCount.HALF)).weightKg)
        // This side was trained before the break (another exercise), and an easing back in is stored as well.
        val trainedBefore = p.bb(45, p.kg(30.0, 12, id = p.legExtension))
        val both = after(trainedBefore, old, prior = listOf(p.reEntry()))
        assertEquals(7.0, t(both).weightKg)
        assertTrue(both.reason.endsWith(Coach.RE_ENTRY_NOTE), both.reason)
    }

    @Test
    fun firstTime_withOnlyKgSets_orOnlyABaseline_givesNoTarget() {
        assertNull(run(p.history(PlateCount.WHOLE, p.pl(10, p.kg(60.0, 8)), p.pl(5, p.kg(62.5, 8)))))
        assertNull(run(p.history(PlateCount.WHOLE, baselines = listOf(Baseline(p.pulldown, 80.0, p.day(10))))))
    }

    @Test
    fun aKgHistoryThenPlates_givesNoBodybuildingTarget() {
        val kg = arrayOf(
            p.bb(10, p.kg(60.0, 12), p.kg(60.0, 12), p.kg(60.0, 12)),
            p.bb(3, p.kg(62.0, 10), p.kg(62.0, 10), p.kg(62.0, 9)),
        )
        assertNull(run(p.history(PlateCount.WHOLE, *kg)))
    }

    @Test
    fun aPlateSet_neverTakesABodyweightBranch() {
        // earnedIncrease refuses a 0 kg load, never plate 1.
        assertEquals(2.0, t(after(last(1.0, 12, 12, 12))).weightKg)
        // firstTimeWeight skips 0 kg sets, never plate 1.
        assertEquals(1.0, t(after(p.pl(3, p.plate(1.0, 8)))).weightKg)
        // The jump check is skipped from 0 kg, never from plate 1.
        assertTrue(InputWarning.BigJump(1.0, LoadUnit.PLATES) in InputValidator.check(3.0, 10, 1.0, LoadUnit.PLATES, 1.0))
    }

    @Test
    fun theFirstEverPlateSet_hasNoTarget_andTheRepsAimIsTheBottomOfTheRange() {
        val none = p.history(PlateCount.WHOLE)
        assertNull(run(none))
        assertEquals(8, BodybuildingCoach.repsToBeat(p.bbItem, none, 1, p.today))
        // After kg sessions too: the kg reps are not carried into plates.
        val kgBefore = p.history(PlateCount.WHOLE, p.bb(3, p.kg(60.0, 10), p.kg(60.0, 10), p.kg(60.0, 10)))
        assertNull(run(kgBefore))
        assertEquals(8, BodybuildingCoach.repsToBeat(p.bbItem, kgBefore, 1, p.today))
    }

    @Test
    fun changePlates_changesNoPlateTarget() {
        val bbCases: List<Pair<TrainingHistory, List<Advice>>> = listOf(
            p.history(PlateCount.WHOLE, last(7.0, 12, 12, 12)) to emptyList(),
            p.history(PlateCount.HALF, last(7.0, 12, 12, 12)) to emptyList(),
            p.history(PlateCount.WHOLE, last(7.0, 12, 12, 11)) to emptyList(),
            p.history(PlateCount.WHOLE, last(7.5, 12, 12, 11)) to emptyList(),
            p.history(PlateCount.WHOLE, last(7.0, 12, 12, 11)) to listOf(p.deload()),
            p.history(PlateCount.WHOLE, p.bb(20, p.plate(7.0, 12), p.plate(7.0, 12), p.plate(7.0, 11))) to emptyList(),
            p.history(PlateCount.WHOLE, p.pl(5, p.plate(9.0, 8))) to emptyList(),
            p.history(PlateCount.HALF, p.pl(40, p.plate(9.0, 8))) to emptyList(),
        )
        for ((h, prior) in bbCases) {
            val off = assertNotNull(run(h, prior))
            assertEquals(off, run(h, prior, settings = CoachSettings(changePlates = true)))
        }
        val plCases: List<Pair<TrainingHistory, List<Advice>>> = listOf(
            p.history(PlateCount.WHOLE, p.pl(3, p.plate(7.0, 8))) to emptyList(),
            p.history(PlateCount.HALF, p.pl(3, p.plate(7.0, 12))) to emptyList(),
            p.history(PlateCount.WHOLE, p.pl(3, p.plate(7.0, 8, failed = true))) to emptyList(),
            p.history(PlateCount.HALF, p.pl(3, p.plate(7.0, 8, rpe = 9.5))) to emptyList(),
            p.history(PlateCount.WHOLE, p.pl(3, p.plate(7.0, 8))) to listOf(p.deload()),
            p.history(PlateCount.HALF, p.pl(20, p.plate(7.0, 8))) to emptyList(),
        )
        for ((h, prior) in plCases) {
            val off = assertNotNull(p.target(TrainingMode.POWERLIFTING, h, prior = prior))
            assertEquals(off, p.target(TrainingMode.POWERLIFTING, h, prior = prior, settings = CoachSettings(changePlates = true)))
        }
    }
}
