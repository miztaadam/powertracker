package com.powertracker.domain.e1rm

import kotlin.test.Test
import kotlin.test.assertEquals

class E1rmTest {
    @Test fun singleAtRpe10IsWeight() = assertEquals(100.0, E1rm.estimate(100.0, 1, 10.0), 1e-9)

    @Test fun fiveAtRpe10() = assertEquals(115.87, E1rm.estimate(100.0, 5, 10.0), 0.01)

    @Test fun rpe8MeansTwoRepsInReserve() =
        assertEquals(E1rm.estimate(100.0, 5, 10.0), E1rm.estimate(100.0, 3, 8.0), 1e-9)

    @Test fun halfStepInterpolates() =
        // effective reps 5.5: midway between .863 (5) and .837 (6) = .850
        assertEquals(100.0 / 0.850, E1rm.estimate(100.0, 4, 8.5), 1e-9)

    @Test fun nullRpeUsesEpley() = assertEquals(116.67, E1rm.estimate(100.0, 5, null), 0.01)

    @Test fun beyondTwelveEffectiveRepsFallsBackToEpley() =
        assertEquals(150.0, E1rm.estimate(100.0, 12, 7.0), 1e-9)

    @Test fun zeroWeightIsZero() = assertEquals(0.0, E1rm.estimate(0.0, 5, 8.0), 0.0)

    @Test fun nonPositiveRepsIsZero() = assertEquals(0.0, E1rm.estimate(100.0, 0, 8.0), 0.0)

    @Test fun rpeBelowSixIsClampedToSix() =
        assertEquals(E1rm.estimate(100.0, 3, 6.0), E1rm.estimate(100.0, 3, 4.0), 1e-9)

    @Test fun rpeAboveTenIsClampedToTen() =
        assertEquals(E1rm.estimate(100.0, 3, 10.0), E1rm.estimate(100.0, 3, 11.0), 1e-9)

    @Test fun loadForUndoesEstimate() {
        for (w in 40..200) for (r in 1..12) for (rpe in listOf(null, 7.0, 8.0, 9.0)) {
            val e = E1rm.estimate(w.toDouble(), r, rpe)
            assertEquals(w.toDouble(), E1rm.loadFor(e, r, rpe), 0.01, "$w kg x $r @ $rpe")
        }
    }

    @Test fun loadForNothingIsZero() {
        assertEquals(0.0, E1rm.loadFor(0.0, 5, 8.0), 0.0)
        assertEquals(0.0, E1rm.loadFor(150.0, 0, 8.0), 0.0)
    }
}
