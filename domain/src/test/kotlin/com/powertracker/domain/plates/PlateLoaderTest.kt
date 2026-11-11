package com.powertracker.domain.plates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlateLoaderTest {
    @Test fun stepIsTwiceSmallestPlate() {
        assertEquals(2.5, PlateLoader.step(false))
        assertEquals(0.5, PlateLoader.step(true))
    }

    @Test fun platesIncludeChangePlatesWhenEnabled() {
        assertEquals(PlateLoader.DEFAULT_PLATES, PlateLoader.plates(false))
        assertEquals(PlateLoader.DEFAULT_PLATES + PlateLoader.CHANGE_PLATES, PlateLoader.plates(true))
    }

    @Test fun roundTieGoesLighter() = assertEquals(145.0, PlateLoader.round(146.25, false))
    @Test fun roundToNearestStep() = assertEquals(147.5, PlateLoader.round(147.6, false))
    @Test fun roundNeverBelowBar() = assertEquals(20.0, PlateLoader.round(10.0, false))
    @Test fun roundWithChangePlates() = assertEquals(101.5, PlateLoader.round(101.3, true))

    @Test fun perSideGreedy() =
        assertEquals(listOf(25.0, 25.0, 25.0, 5.0, 1.25), PlateLoader.perSide(182.5, false))
    @Test fun perSideBarOnlyIsEmpty() = assertEquals(emptyList(), PlateLoader.perSide(20.0, false))
    @Test fun perSideUnloadableIsNull() = assertNull(PlateLoader.perSide(21.0, false))
    @Test fun perSideBelowBarIsNull() = assertNull(PlateLoader.perSide(15.0, false))
    @Test fun perSideWithChangePlates() =
        assertEquals(listOf(25.0, 15.0, 0.5), PlateLoader.perSide(101.0, true))
    @Test fun roundFromKeepsAnchorGrid() = assertEquals(12.0, PlateLoader.roundFrom(12.0, 12.6, false))
    @Test fun roundFromStepsFromAnchor() = assertEquals(14.5, PlateLoader.roundFrom(12.0, 14.0, false))
    @Test fun roundFromNeverBelowZero() = assertEquals(0.0, PlateLoader.roundFrom(0.0, -1.0, false))
    @Test fun roundFromNoBarFloor() = assertEquals(10.0, PlateLoader.roundFrom(10.0, 10.0, false))
    @Test fun roundFromTieGoesLighter() = assertEquals(10.0, PlateLoader.roundFrom(10.0, 11.25, false))
}
