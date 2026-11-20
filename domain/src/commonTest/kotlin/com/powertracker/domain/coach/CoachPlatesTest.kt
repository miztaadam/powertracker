package com.powertracker.domain.coach

import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.TrainingMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** 1.17: the powerlifting side's lighter weeks on a plate-counted exercise, through [Coaching.targetsFor]. */
class CoachPlatesTest {
    private val p = PlateFixtures

    private fun after(vararg sessions: LoggedSession, count: PlateCount = PlateCount.WHOLE, prior: List<Advice> = emptyList()) =
        assertNotNull(p.target(TrainingMode.POWERLIFTING, p.history(count, *sessions), prior = prior))

    private fun t(a: Advice) = a.payload as AdvicePayload.Target

    /** Plate 7 × 8 @ 8 three days ago: on target, so progression alone would give plate 7 × 9. */
    private val seven = p.pl(3, p.plate(7.0, 8))

    /** The same, 20 days ago: today is the first day back after a break. */
    private val sevenBeforeABreak = p.pl(20, p.plate(7.0, 8))

    @Test
    fun deload_isNinetyPercentRoundedDown() {
        val a = after(seven, prior = listOf(p.deload()))
        assertEquals(6.0, t(a).weightKg)
        assertEquals(8, t(a).reps)
        assertEquals(2, t(a).sets)
        assertTrue(a.reason.endsWith(Coach.DELOAD_NOTE), a.reason)
        assertEquals("Last time: plate 7 × 8 @ 8. Coach suggests plate 6 × 8 @ 8." + Coach.DELOAD_NOTE, a.reason)
    }

    @Test
    fun easingBackIn_isEightyFivePercentRoundedDown() {
        val a = after(sevenBeforeABreak)
        assertEquals(5.0, t(a).weightKg)
        assertEquals(8, t(a).reps)
        assertEquals("Last time: plate 7 × 8 @ 8. Coach suggests plate 5 × 8 @ 8." + Coach.RE_ENTRY_NOTE, a.reason)
        assertEquals(5.5, t(after(sevenBeforeABreak, count = PlateCount.HALF)).weightKg)
    }

    @Test
    fun plateTwo_inADeload() {
        val two = p.pl(3, p.plate(2.0, 8))
        assertEquals(1.0, t(after(two, prior = listOf(p.deload()))).weightKg)
        assertEquals(1.5, t(after(two, count = PlateCount.HALF, prior = listOf(p.deload()))).weightKg)
    }

    @Test
    fun plateOne_staysPlateOne_andTheDeloadStillCutsTheSets() {
        val a = after(p.pl(3, p.plate(1.0, 8)), prior = listOf(p.deload()))
        assertEquals(1.0, t(a).weightKg)
        assertEquals(8, t(a).reps)
        assertEquals(2, t(a).sets)
        assertTrue(a.reason.endsWith(Coach.DELOAD_NOTE), a.reason)
    }

    @Test
    fun deloadAndEasingBackTogether_takeTheLowerCap_notTwoSteps() {
        val a = after(sevenBeforeABreak, prior = listOf(p.deload()))
        assertEquals(5.0, t(a).weightKg)
        assertEquals(2, t(a).sets)
        assertEquals("Last time: plate 7 × 8 @ 8. Coach suggests plate 5 × 8 @ 8." + Coach.DELOAD_NOTE + Coach.RE_ENTRY_NOTE, a.reason)
    }

    @Test
    fun aLighterWeekOfSeveralSessions_losesOnePlateOnly() {
        // Plate 7 before the deload (made 7 days ago), then two deload sessions at plate 6.
        val a = after(
            p.pl(10, p.plate(7.0, 8)),
            p.pl(5, p.plate(6.0, 8, rpe = 7.0)),
            p.pl(2, p.plate(6.0, 8, rpe = 7.0)),
            prior = listOf(p.deload(7)),
        )
        assertEquals(6.0, t(a).weightKg)
        assertEquals(8, t(a).reps)
        assertEquals(2, t(a).sets)
    }

    @Test
    fun aLighterWeek_doesThePlannedReps() {
        val a = after(p.pl(3, p.plate(7.0, 10)), prior = listOf(p.deload()))
        assertEquals(6.0 to 8, t(a).weightKg to t(a).reps)
    }
}
