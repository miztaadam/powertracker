package com.powertracker.domain.stack

import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.Library
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlateCount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlateStackTest {
    /** The row the app holds for catalogue entry [name]: its own category, built in. */
    private fun catalogue(name: String, plateCount: PlateCount = PlateCount.OFF): Exercise {
        val entry = assertNotNull(Library.find(name), name)
        return Exercise(1, entry.name, entry.category, entry.parentLift, entry.muscles, isBuiltIn = true, plateCount = plateCount)
    }

    private fun custom(name: String, category: Category = Category.ACCESSORY, parentLift: MainLift? = null) =
        Exercise(2, name, category, parentLift, emptyList(), isBuiltIn = false)

    @Test
    fun offered_machineAndCableAccessories_andCustomAccessories() {
        val cable = Library.all().first { it.category == Category.ACCESSORY && Equipment.CABLE in it.equipment }
        for (name in listOf("Lat pulldown", cable.name, "T-bar row", "Leg press")) {
            assertTrue(PlateStack.offered(catalogue(name)), name)
        }
        assertEquals(listOf(Equipment.MACHINE, Equipment.BARBELL), Library.find("T-bar row")!!.equipment)
        assertTrue(PlateStack.offered(custom("My row machine")))
        assertTrue(PlateStack.offered(custom("Unassisted row")))
    }

    @Test
    fun neverOffered_mainLifts_variations_smith_assisted() {
        for (category in listOf(Category.SQUAT, Category.BENCH, Category.DEADLIFT, Category.STRICT_PRESS)) {
            assertFalse(PlateStack.offered(Exercise(3, category.name, category, null, emptyList(), true)), category.name)
            assertFalse(PlateStack.offered(Exercise(3, category.name, category, null, emptyList(), false)), "custom ${category.name}")
        }
        val builtInVariation = Library.all().first { it.category == Category.VARIATION }
        assertFalse(PlateStack.offered(catalogue(builtInVariation.name)), builtInVariation.name)
        assertFalse(PlateStack.offered(custom("My machine squat", Category.VARIATION, MainLift.SQUAT)))
        val smith = Library.all().first { it.category == Category.ACCESSORY && it.equipment == listOf(Equipment.SMITH) }
        assertFalse(PlateStack.offered(catalogue(smith.name)), smith.name)
        for (name in listOf("Assisted pull-up", "Assisted chin-up", "Assisted dip")) {
            val entry = Library.find(name)!!
            assertEquals(Category.ACCESSORY, entry.category, name)
            assertTrue(Equipment.MACHINE in entry.equipment, name)
            assertFalse(PlateStack.offered(catalogue(name)), name)
        }
        for (name in listOf("Assisted dip", "assisted row", "Machine-assisted dip", "My assisted row")) {
            assertFalse(PlateStack.offered(custom(name)), "custom $name")
        }
        val dumbbell = Library.all().first {
            it.category == Category.ACCESSORY && Equipment.DUMBBELL in it.equipment &&
                Equipment.MACHINE !in it.equipment && Equipment.CABLE !in it.equipment
        }
        assertFalse(PlateStack.offered(catalogue(dumbbell.name)), dumbbell.name)
    }

    @Test
    fun everyMachineOrCableCatalogueEntry_isAnAccessory() {
        val machines = Library.all().filter { Equipment.MACHINE in it.equipment || Equipment.CABLE in it.equipment }
        assertTrue(machines.isNotEmpty())
        for (entry in machines) assertEquals(Category.ACCESSORY, entry.category, entry.name)
    }

    @Test
    fun unitOf_isKgWhenNotOffered_whateverTheSettingSays() {
        assertEquals(LoadUnit.KG, PlateStack.unitOf(Exercise(1, "Squat", Category.SQUAT, null, emptyList(), true, plateCount = PlateCount.WHOLE)))
        assertEquals(LoadUnit.KG, PlateStack.unitOf(catalogue("Lat pulldown", PlateCount.OFF)))
        assertEquals(LoadUnit.PLATES, PlateStack.unitOf(catalogue("Lat pulldown", PlateCount.WHOLE)))
        assertEquals(LoadUnit.PLATES, PlateStack.unitOf(catalogue("Lat pulldown", PlateCount.HALF)))
    }

    @Test
    fun step_isHalfOnlyForHalfSteps() {
        assertEquals(0.5, PlateStack.step(PlateCount.HALF))
        assertEquals(1.0, PlateStack.step(PlateCount.WHOLE))
        assertEquals(1.0, PlateStack.step(PlateCount.OFF))
    }

    @Test
    fun upAndDown_onEachGrid() {
        assertEquals(1.0, PlateStack.up(null, 1.0))
        assertEquals(1.0, PlateStack.down(null, 1.0))
        assertEquals(8.0, PlateStack.up(7.0, 1.0))
        assertEquals(7.5, PlateStack.up(7.0, 0.5))
        assertEquals(8.0, PlateStack.up(7.5, 1.0))
        assertEquals(7.0, PlateStack.down(7.5, 1.0))
        assertEquals(1.0, PlateStack.down(1.0, 1.0))
        assertEquals(1.0, PlateStack.down(1.0, 0.5))
        assertEquals(50.0, PlateStack.up(50.0, 1.0))
    }

    @Test
    fun eased_theDesignsExamples() {
        assertEquals(6.0, PlateStack.eased(7.0, 0.95, 1.0))
        assertEquals(6.0, PlateStack.eased(7.0, 0.9, 1.0))
        assertEquals(5.0, PlateStack.eased(7.0, 0.85, 1.0))
        assertEquals(5.5, PlateStack.eased(7.0, 0.85, 0.5))
        assertEquals(1.0, PlateStack.eased(2.0, 0.9, 1.0))
        assertEquals(1.5, PlateStack.eased(2.0, 0.9, 0.5))
        assertEquals(1.0, PlateStack.eased(1.0, 0.9, 1.0))
        assertEquals(6.0, PlateStack.eased(7.0, 0.975, 1.0))
        assertEquals(6.5, PlateStack.eased(7.0, 0.975, 0.5))
        assertEquals(6.5, PlateStack.eased(7.0, 0.95, 0.5))
    }

    @Test
    fun eased_isAtLeastOneStepDown_andLandsOnTheGrid() {
        assertEquals(9.0, PlateStack.eased(10.0, 0.9, 1.0))
        assertEquals(19.0, PlateStack.eased(20.0, 0.975, 1.0))
        assertEquals(7.0, PlateStack.eased(7.5, 0.95, 1.0))
        assertEquals(39.0, PlateStack.eased(40.0, 0.975, 1.0))
    }

    @Test
    fun floor_landsOnTheGrid_neverUnderOne() {
        assertEquals(7.0, PlateStack.floor(7.5, 1.0))
        assertEquals(7.5, PlateStack.floor(7.5, 0.5))
        assertEquals(7.0, PlateStack.floor(7.0, 1.0))
        assertEquals(1.0, PlateStack.floor(0.5, 1.0))
    }

    @Test
    fun check_wholeAndHalf() {
        assertEquals(PlateCheck.OK, PlateStack.check(7.0, PlateCount.WHOLE))
        assertEquals(PlateCheck.STEP, PlateStack.check(7.5, PlateCount.WHOLE))
        assertEquals(PlateCheck.OK, PlateStack.check(7.5, PlateCount.HALF))
        assertEquals(PlateCheck.STEP, PlateStack.check(7.25, PlateCount.HALF))
        assertEquals(PlateCheck.RANGE, PlateStack.check(0.0, PlateCount.WHOLE))
        assertEquals(PlateCheck.RANGE, PlateStack.check(0.5, PlateCount.HALF))
        assertEquals(PlateCheck.RANGE, PlateStack.check(51.0, PlateCount.WHOLE))
        assertEquals(PlateCheck.OK, PlateStack.check(50.0, PlateCount.HALF))
        assertEquals(PlateCheck.STEP, PlateStack.check(7.5, PlateCount.OFF))
        assertEquals(PlateCheck.STEP, PlateStack.check(Double.NaN, PlateCount.WHOLE))
    }

    @Test
    fun text_andSpoken() {
        assertEquals("plate 7", PlateStack.text(7.0))
        assertEquals("plate 7½", PlateStack.text(7.5))
        assertEquals("plate 12½", PlateStack.text(12.5))
        assertEquals("plate 7 and a half", PlateStack.spoken(7.5))
        assertEquals("plate 7", PlateStack.spoken(7.0))
    }
}
