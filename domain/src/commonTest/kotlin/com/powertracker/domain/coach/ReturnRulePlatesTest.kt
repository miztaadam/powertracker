package com.powertracker.domain.coach

import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.on
import com.powertracker.domain.stack.inCurrentUnits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * 1.17: back on the powerlifting side after time on the bodybuilding side, on a plate-counted exercise. Plate 9 × 12 @ 8
 * 35 days ago on this side, so progression alone gives plate 10 × 8; the break ended 20 days ago on the other side.
 */
class ReturnRulePlatesTest {
    private val p = PlateFixtures
    private val here = p.pl(35, p.plate(9.0, 12))

    private fun history(vararg sessions: LoggedSession) = p.history(PlateCount.WHOLE, *sessions)

    private fun target(h: TrainingHistory) = assertNotNull(p.target(TrainingMode.POWERLIFTING, h))

    private fun progression(h: TrainingHistory): AdvicePayload.Target {
        val current = h.inCurrentUnits()
        val a = ProgressionRule.target(p.plItem, h.exercises.getValue(p.pulldown), HistoryStats(current.on(TrainingMode.POWERLIFTING)), CoachSettings(), p.today)
        return assertNotNull(a).payload as AdvicePayload.Target
    }

    @Test
    fun trainedOnTheOtherSide_capsAtItsHeaviestGoodPlate() {
        val h = history(here, p.bb(20, p.plate(8.0, 10)), p.bb(10, p.plate(11.0, 6, failed = true)))
        assertEquals(10.0 to 8, progression(h).let { it.weightKg to it.reps })
        val a = target(h)
        val t = a.payload as AdvicePayload.Target
        assertEquals(8.0 to 8, t.weightKg to t.reps)
        assertEquals("Last time: plate 9 × 12 @ 8. Coach suggests plate 8 × 8 @ 8." + Coach.RE_ENTRY_NOTE, a.reason)
    }

    @Test
    fun aCapAtOrAboveTheTarget_changesNothing() {
        for (other in listOf(12.0, 10.0)) {
            val a = target(history(here, p.bb(20, p.plate(other, 10)), p.bb(10, p.plate(other, 10))))
            val t = a.payload as AdvicePayload.Target
            assertEquals(10.0 to 8, t.weightKg to t.reps)
            assertEquals("Last time: plate 9 × 12 @ 8. Coach suggests plate 10 × 8 @ 8.", a.reason)
        }
    }

    @Test
    fun notTrainedOnEitherSide_isEasedByTheStepRule() {
        // Plate 9 × 8 @ 8 40 days ago; since then only another exercise, on the other side, 10 days ago.
        val h = history(p.pl(40, p.plate(9.0, 8)), p.bb(10, p.kg(30.0, 12, id = p.legExtension)))
        assertEquals(9.0 to 9, progression(h).let { it.weightKg to it.reps })
        val a = target(h)
        val t = a.payload as AdvicePayload.Target
        assertEquals(7.0 to 8, t.weightKg to t.reps)
        assertEquals("Last time: plate 9 × 8 @ 8. Coach suggests plate 7 × 8 @ 8." + Coach.RE_ENTRY_NOTE, a.reason)
    }
}
