package com.powertracker.domain.unilateral

import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.SideLog
import com.powertracker.domain.model.SplitSides
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SingleSidedTest {
    private fun accessory(name: String, builtIn: Boolean = true, singleSided: Boolean = false) =
        Exercise(1L, name, Category.ACCESSORY, null, emptyList(), isBuiltIn = builtIn, singleSided = singleSided)

    @Test
    fun offeredOnUnilateralCapableAccessoriesAndCustom_neverOnMainLiftsOrBarbells() {
        assertTrue(SingleSided.offered(accessory("Pec deck")))
        assertTrue(SingleSided.offered(accessory("Lat pulldown")))
        assertTrue(SingleSided.offered(accessory("Incline DB press")))
        assertTrue(SingleSided.offered(accessory("Leg press")))
        assertTrue(SingleSided.offered(accessory("My custom machine", builtIn = false)))

        // Barbell accessories, main lifts, and variations never offer single-sided
        assertFalse(SingleSided.offered(accessory("Barbell row")))
        assertFalse(SingleSided.offered(Exercise(2L, "Squat", Category.SQUAT, null, emptyList(), isBuiltIn = true)))
        assertFalse(SingleSided.offered(Exercise(3L, "Pause squat", Category.VARIATION, MainLift.SQUAT, emptyList(), isBuiltIn = true)))
        assertFalse(SingleSided.offered(Exercise(4L, "Custom squat var", Category.VARIATION, MainLift.SQUAT, emptyList(), isBuiltIn = false)))
    }

    @Test
    fun activeRequiresBothOfferedAndEnabled() {
        assertTrue(SingleSided.active(accessory("Pec deck", singleSided = true)))
        assertFalse(SingleSided.active(accessory("Pec deck", singleSided = false)))
        assertFalse(SingleSided.active(accessory("Barbell row", singleSided = true)))
    }

    @Test
    fun weakerPicksFailedThenLowerWeightThenFewerRepsThenHigherRpe() {
        val r = SideLog(40.0, 12, 8.0, failed = false)
        val lFewer = SideLog(40.0, 10, 8.5, failed = false)
        assertEquals(lFewer, SingleSided.weaker(r, lFewer))

        val lLighter = SideLog(35.0, 12, 8.0, failed = false)
        assertEquals(lLighter, SingleSided.weaker(r, lLighter))

        val lHarder = SideLog(40.0, 12, 9.0, failed = false)
        assertEquals(lHarder, SingleSided.weaker(r, lHarder))

        val lFailed = SideLog(45.0, 12, 10.0, failed = true)
        assertEquals(lFailed, SingleSided.weaker(r, lFailed))
    }

    @Test
    fun combineStoresWeakerSideAsPrimaryFieldsAndPreservesBothSides() {
        val right = SideLog(40.0, 12, 8.0, failed = false)
        val left = SideLog(40.0, 10, 8.5, failed = false)
        val set = SingleSided.combine(exerciseId = 10L, right = right, left = left, unit = LoadUnit.KG)
        assertEquals(40.0, set.weightKg)
        assertEquals(10, set.reps)
        assertEquals(8.5, set.rpe)
        assertFalse(set.failed)
        assertEquals(SplitSides(right, left), set.splitSides)
    }

    @Test
    fun summaryTextFormatsSameAndDifferentWeightsAndSingleMiss() {
        assertEquals(
            "R 12 · L 10",
            SingleSided.summaryText(SplitSides(SideLog(40.0, 12, 8.0, false), SideLog(40.0, 10, 8.5, false))),
        )
        assertEquals(
            "R 40×12 · L 37.5×10",
            SingleSided.summaryText(SplitSides(SideLog(40.0, 12, 8.0, false), SideLog(37.5, 10, 8.5, false))),
        )
        assertEquals(
            "R 12 · L 9 miss",
            SingleSided.summaryText(SplitSides(SideLog(40.0, 12, 8.0, false), SideLog(40.0, 9, 10.0, true))),
        )
        assertEquals(
            "R plate 7×12 · L plate 6½×10",
            SingleSided.summaryText(SplitSides(SideLog(7.0, 12, 8.0, false), SideLog(6.5, 10, 8.5, false)), LoadUnit.PLATES),
        )
    }
}
