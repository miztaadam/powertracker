package com.powertracker.domain.web

import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlateCount
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GritDomainPlatesTest {
    private val pulldown = JsExercise(5, "Lat pulldown", "ACCESSORY", null, arrayOf("Lats"), true, false)
    private val squat = JsExercise(1, "Squat", "SQUAT", null, arrayOf("Quads"), true, false)

    @Test
    fun aSetBuiltWithoutAUnit_isKg() {
        assertEquals("KG", JsSet(5, 60.0, 10, 8.0, false, null, false).unit)
        assertEquals(LoadUnit.KG, JsSet(5, 60.0, 10, 8.0, false, null, false).toDomain().unit)
        assertEquals(LoadUnit.KG, LoggedSet(5, 60.0, 10, 8.0, false, null, false).unit)
    }

    @Test
    fun anExerciseBuiltWithoutASetting_isOff() {
        assertEquals("OFF", pulldown.plateCount)
        assertEquals(PlateCount.OFF, pulldown.toDomain().plateCount)
        assertEquals(PlateCount.OFF, Exercise(5, "Lat pulldown", Category.ACCESSORY, null, emptyList(), true, false).plateCount)
    }

    @Test
    fun toJs_carriesUnitAndPlateCount() {
        assertEquals("PLATES", JsSet(5, 7.0, 10, 8.0, false, null, true, "PLATES").toDomain().toJs().unit)
        assertEquals("HALF", pulldown.copy(plateCount = "HALF").toDomain().toJs().plateCount)

        val history = JsHistory(
            arrayOf(
                JsSession(1, "2026-11-10", null, null, arrayOf(JsSet(5, 7.0, 10, 8.0, false, null, true, "PLATES")), "BODYBUILDING"),
                JsSession(2, "2026-11-13", null, null, arrayOf(JsSet(5, 8.0, 9, 8.0, false, null, true, "PLATES")), "BODYBUILDING"),
            ),
            arrayOf(pulldown.copy(plateCount = "WHOLE")),
            emptyArray(),
        )
        val plan = arrayOf(JsPlanItem(1, 1, 5, 3, 8, 8.0, 0, "BODYBUILDING", 12))
        val rows = GritDomain.bodybuildingProgress(history, plan, "2026-11-20")
        assertEquals("PLATES", rows[0].bestSets[0].set.unit)
        assertEquals("PLATES", rows[0].bestSets[1].set.unit)
    }

    @Test
    fun aBadUnitOrSetting_isRefused() {
        assertTrue(runCatching { JsSet(5, 7.0, 10, 8.0, false, null, true, "STONES").toDomain() }.isFailure)
        assertTrue(runCatching { pulldown.copy(plateCount = "QUARTER").toDomain() }.isFailure)
    }

    @Test
    fun plateExports_answerAsPlateStack() {
        assertTrue(GritDomain.plateCountOffered(pulldown))
        assertFalse(GritDomain.plateCountOffered(squat))
        assertEquals("KG", GritDomain.loadUnitOf(pulldown))
        assertEquals("PLATES", GritDomain.loadUnitOf(pulldown.copy(plateCount = "WHOLE")))
        assertEquals("KG", GritDomain.loadUnitOf(squat.copy(plateCount = "WHOLE")))
        assertEquals(0.5, GritDomain.plateStep("HALF"))
        assertEquals(1.0, GritDomain.plateStep("WHOLE"))
        assertEquals(1.0, GritDomain.plateUp(null, "WHOLE"))
        assertEquals(7.5, GritDomain.plateUp(7.0, "HALF"))
        assertEquals(8.0, GritDomain.plateUp(7.5, "WHOLE"))
        assertEquals(7.0, GritDomain.plateDown(7.5, "WHOLE"))
        assertEquals(1.0, GritDomain.plateDown(1.0, "HALF"))
        assertEquals("OK", GritDomain.plateCheck(7.5, "HALF"))
        assertEquals("STEP", GritDomain.plateCheck(7.5, "WHOLE"))
        assertEquals("RANGE", GritDomain.plateCheck(51.0, "WHOLE"))
        assertEquals("plate 7½", GritDomain.plateText(7.5))
        assertEquals("plate 7", GritDomain.plateText(7.0))
        assertEquals("plate 7 and a half", GritDomain.plateSpoken(7.5))
        assertEquals(50.0, GritDomain.maxPlate())
        assertTrue(runCatching { GritDomain.plateStep("QUARTER") }.isFailure)
    }

    @Test
    fun advicePayload_carriesTheUnit() {
        val old = """{"type":"com.powertracker.domain.model.AdvicePayload.Target","exerciseId":5,"weightKg":60.0,"reps":8,"rpe":8.0}"""
        val plate = """{"type":"com.powertracker.domain.model.AdvicePayload.Target","exerciseId":5,"weightKg":7.5,"reps":8,"rpe":8.0,"unit":"PLATES"}"""
        assertEquals("KG", GritDomain.advicePayload(old)?.unit)
        assertEquals("PLATES", GritDomain.advicePayload(plate)?.unit)
        assertEquals(7.5, GritDomain.advicePayload(plate)?.weightKg)
        assertEquals(null, GritDomain.advicePayload("""{"type":"com.powertracker.domain.model.AdvicePayload.ReEntry"}""")?.unit)
    }

    @Test
    fun lastTopSet_isInTheCurrentUnit() {
        fun history(count: String) = JsHistory(
            arrayOf(
                JsSession(1, "2026-11-10", null, null, arrayOf(JsSet(5, 60.0, 10, 8.0, false, null, true)), "POWERLIFTING"),
                JsSession(2, "2026-11-13", null, null, arrayOf(JsSet(5, 7.0, 10, 8.0, false, null, true, "PLATES")), "POWERLIFTING"),
                JsSession(3, "2026-11-16", null, null, arrayOf(JsSet(5, 65.0, 8, 8.0, false, null, true)), "POWERLIFTING"),
            ),
            arrayOf(pulldown.copy(plateCount = count)),
            emptyArray(),
        )
        val inPlates = GritDomain.lastTopSet(history("WHOLE"), 5, "2026-11-20")
        assertEquals("PLATES", inPlates?.unit)
        assertEquals(7.0, inPlates?.weightKg)
        val inKg = GritDomain.lastTopSet(history("OFF"), 5, "2026-11-20")
        assertEquals("KG", inKg?.unit)
        assertEquals(65.0, inKg?.weightKg)
        // Plates on with only kg sets: nothing to fill the field from.
        val kgOnly = JsHistory(arrayOf(history("WHOLE").sessions[0]), arrayOf(pulldown.copy(plateCount = "HALF")), emptyArray())
        assertEquals(null, GritDomain.lastTopSet(kgOnly, 5, "2026-11-20"))
    }

    @Test
    fun checkLoadInput_inPlates_namesBigJump() {
        assertContentEquals(arrayOf("BIG_JUMP"), GritDomain.checkLoadInput(5.0, 10, 3.0, "PLATES", "WHOLE"))
        assertContentEquals(emptyArray(), GritDomain.checkLoadInput(4.0, 10, 3.0, "PLATES", "WHOLE"))
        assertContentEquals(arrayOf("HIGH_REPS"), GritDomain.checkLoadInput(7.0, 13, 7.0, "PLATES", "HALF"))
        assertContentEquals(emptyArray(), GritDomain.checkLoadInput(7.0, 10, null, "PLATES", "WHOLE"))
        for ((weight, last) in listOf(130.0 to 100.0, 131.0 to 100.0, 20.0 to 0.0)) {
            assertContentEquals(GritDomain.checkInput(weight, 5, last), GritDomain.checkLoadInput(weight, 5, last, "KG", "OFF"))
        }
    }
}
