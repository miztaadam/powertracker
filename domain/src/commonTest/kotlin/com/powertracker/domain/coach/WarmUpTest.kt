package com.powertracker.domain.coach

import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlateCount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WarmUpTest {
    private val squat = Exercise(1, "Squat", Category.SQUAT, null, emptyList(), true)
    private val pauseBench = Exercise(10, "Pause Bench", Category.VARIATION, MainLift.BENCH, emptyList(), true)
    private val dumbbellCurl = Exercise(50, "Dumbbell curl", Category.ACCESSORY, null, emptyList(), true)
    private val legPress = Exercise(60, "Leg press", Category.ACCESSORY, null, emptyList(), true)
    private val custom = Exercise(70, "Garage row", Category.ACCESSORY, null, emptyList(), false)

    private fun kg(weight: Double, reps: Int, vararg perSide: Double) = WarmUpSet(weight, reps, LoadUnit.KG, perSide.toList())
    private fun free(weight: Double, reps: Int) = WarmUpSet(weight, reps, LoadUnit.KG, null)
    private fun plate(weight: Double, reps: Int) = WarmUpSet(weight, reps, LoadUnit.PLATES, null)

    private fun sets(exercise: Exercise, working: Double?, reps: Int = 5, changePlates: Boolean = false) =
        WarmUp.sets(exercise, working, reps, changePlates)

    @Test
    fun mainLift_halfForTwelve_thenThreeQuartersForFive_withThePlatesPerSide() {
        assertEquals(listOf(kg(50.0, 12, 15.0), kg(75.0, 5, 25.0, 2.5)), sets(squat, 100.0))
        // A variation of a main lift is a heavy lift too.
        assertEquals(listOf(kg(40.0, 12, 10.0), kg(60.0, 5, 20.0)), sets(pauseBench, 80.0))
    }

    @Test
    fun barbell_roundsToWhatThePlatesMake_aTieGoingLighter() {
        // Half of 142.5 is 71.25, between 70 and 72.5: the lighter. Three quarters is 106.875: 107.5 is nearest.
        assertEquals(listOf(kg(70.0, 12, 25.0), kg(107.5, 5, 25.0, 15.0, 2.5, 1.25)), sets(squat, 142.5))
        // With change plates the grid is 1 kg: 71 (71.25 down) and 107 (106.875 up).
        assertEquals(listOf(71.0, 107.0), sets(squat, 142.5, changePlates = true).map { it.weight })
        assertEquals(listOf(25.0, 0.5), sets(squat, 142.5, changePlates = true).first().perSide)
    }

    @Test
    fun barbell_justAboveTheBar_isTheEmptyBarOnce_andNothingAtTheBar() {
        // Half and three quarters both round up to the bar: one row, the bar alone.
        for (working in listOf(22.5, 25.0)) assertEquals(listOf(kg(20.0, 12)), sets(squat, working), "$working")
        assertEquals(listOf(kg(20.0, 12), kg(22.5, 5, 1.25)), sets(squat, 30.0))
        // 40: half is the bar, three quarters is 30.
        assertEquals(listOf(kg(20.0, 12), kg(30.0, 5, 5.0)), sets(squat, 40.0))
        // The bar itself has nothing lighter to load.
        assertEquals(emptyList(), sets(squat, 20.0))
    }

    @Test
    fun mainLift_underTheBar_isAFreeLoad_onItsOwnGrid_withNoPlates() {
        // A 15 kg technique bar: 7.5, then 11.25 is between 10 and 12.5 and goes lighter.
        assertEquals(listOf(free(7.5, 12), free(10.0, 5)), sets(squat, 15.0))
    }

    @Test
    fun accessory_isOneSetOnTheWorkingWeightsGrid_neverTheBar() {
        assertEquals(listOf(free(12.5, 12)), sets(dumbbellCurl, 25.0, reps = 10))
        // 22 kg dumbbells sit on 22, 19.5, 17, 14.5, 12, 9.5: half (11) is nearest 12.
        assertEquals(listOf(free(12.0, 12)), sets(dumbbellCurl, 22.0, reps = 10))
        // 37.5: 18.75 is between 17.5 and 20 and goes lighter.
        assertEquals(listOf(free(17.5, 12)), sets(custom, 37.5, reps = 8))
        // A heavy accessory is still not put on a bar: 100 gives 50 with no plates listed.
        assertEquals(listOf(free(50.0, 12)), sets(custom, 100.0, reps = 8))
    }

    @Test
    fun accessory_tooLightToHalve_hasNone() {
        // 2 kg: the grid is 2 and nothing below it above zero.
        assertEquals(emptyList(), sets(dumbbellCurl, 2.0, reps = 10))
        // 2.5 kg: half ties between 2.5 and 0 and goes to 0, which is no set.
        assertEquals(emptyList(), sets(dumbbellCurl, 2.5, reps = 10))
        // 5 kg: 2.5.
        assertEquals(listOf(free(2.5, 12)), sets(dumbbellCurl, 5.0, reps = 10))
    }

    @Test
    fun plates_roundDownToTheMachinesGrid_neverUnderPlateOne() {
        val whole = legPress.copy(plateCount = PlateCount.WHOLE)
        val half = legPress.copy(plateCount = PlateCount.HALF)
        assertEquals(listOf(plate(4.0, 12)), sets(whole, 8.0, reps = 10))
        assertEquals(listOf(plate(3.0, 12)), sets(whole, 7.0, reps = 10))
        assertEquals(listOf(plate(3.5, 12)), sets(half, 7.0, reps = 10))
        assertEquals(listOf(plate(3.5, 12)), sets(half, 7.5, reps = 10))
        // Plate 2: plate 1. Plate 1 (and 1½, whose half is under plate 1 and is held at 1): plate 1 or nothing.
        assertEquals(listOf(plate(1.0, 12)), sets(whole, 2.0, reps = 10))
        assertEquals(emptyList(), sets(whole, 1.0, reps = 10))
        assertEquals(listOf(plate(1.0, 12)), sets(half, 1.5, reps = 10))
    }

    @Test
    fun plates_setOnAnExerciseThatMayNotCountThem_isKg() {
        // A stored plate setting on a main lift counts as off (PlateStack.unitOf).
        val odd = squat.copy(plateCount = PlateCount.WHOLE)
        assertEquals(sets(squat, 100.0), sets(odd, 100.0))
    }

    @Test
    fun highReps_haveNone_fromFifteen() {
        assertEquals(1, sets(dumbbellCurl, 20.0, reps = 14).size)
        assertEquals(emptyList(), sets(dumbbellCurl, 20.0, reps = 15))
        assertEquals(emptyList(), sets(squat, 100.0, reps = 20))
    }

    @Test
    fun noWeight_orABodyweightSet_hasNone() {
        for (working in listOf(null, 0.0, -5.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(emptyList(), sets(squat, working), "$working")
            assertEquals(emptyList(), sets(custom, working), "$working")
        }
    }

    @Test
    fun bodyweightAndAssistedExercises_haveNone_whateverWeightIsTyped() {
        // The catalogue's bodyweight exercises: the weight typed is added weight.
        for (name in listOf("Pull-up", "Weighted pull-up", "Triceps dip", "Push-up")) {
            val exercise = Exercise(80, name, Category.ACCESSORY, null, emptyList(), true)
            assertEquals(emptyList(), sets(exercise, 20.0, reps = 8), name)
        }
        // More weight is easier on an assisted machine, built-in or the lifter's own.
        for (name in listOf("Assisted pull-up", "Assisted dip")) {
            assertEquals(emptyList(), sets(Exercise(81, name, Category.ACCESSORY, null, emptyList(), true), 40.0, reps = 8), name)
        }
        assertEquals(emptyList(), sets(custom.copy(name = "My assisted chin"), 40.0, reps = 8))
        // A custom exercise that shares a catalogue bodyweight name is the lifter's own: the catalogue is not asked.
        assertEquals(1, sets(custom.copy(name = "Pull-up"), 20.0, reps = 8).size)
    }

    @Test
    fun everySet_isLighterThanTheWorkingWeight_heavierThanTheOneBefore_andLoadable() {
        for (exercise in listOf(squat, pauseBench, dumbbellCurl, custom)) for (changePlates in listOf(false, true)) {
            var working = 0.5
            while (working <= 300.0) {
                val out = sets(exercise, working, changePlates = changePlates)
                assertTrue(out.size <= 2, "$exercise $working")
                var before = 0.0
                for (set in out) {
                    assertTrue(set.weight > before && set.weight < working, "${exercise.name} $working: $out")
                    before = set.weight
                    // A barbell set says how to load it, or it would be drawn as "can't load".
                    if (exercise.mainLift != null && working >= 20.0) assertTrue(set.perSide != null, "${exercise.name} $working: $out")
                }
                working += 0.5
            }
        }
    }
}
