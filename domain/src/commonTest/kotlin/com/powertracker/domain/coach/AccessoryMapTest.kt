package com.powertracker.domain.coach

import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccessoryMapTest {
    @Test
    fun benchLockout_optionsMatchSpecTable() {
        assertEquals(
            listOf("Close-grip bench", "JM press", "Board press"),
            AccessoryMap.options(MainLift.BENCH, MissPoint.LOCKOUT).map { it.exerciseName },
        )
    }

    @Test
    fun squatFloor_musclesMatchSpecTable() {
        assertEquals(listOf("Quads", "Glutes"), AccessoryMap.muscles(MainLift.SQUAT, MissPoint.FLOOR))
    }

    @Test
    fun everyLiftAndMissPoint_hasAtLeastTwoOptions() {
        for (lift in MainLift.entries) {
            for (miss in MissPoint.entries + listOf(null)) {
                assertTrue(AccessoryMap.options(lift, miss).size >= 2, "$lift $miss")
            }
        }
    }

    @Test
    fun heavyFlag_marksVariationsOnly() {
        val bench = AccessoryMap.options(MainLift.BENCH, MissPoint.LOCKOUT).associate { it.exerciseName to it.heavy }
        assertEquals(mapOf("Close-grip bench" to true, "JM press" to false, "Board press" to true), bench)
    }

    @Test
    fun fallback_isFirstTwoHeavyVariationsPerLift() {
        assertEquals(listOf("Pause squat", "Front squat"), AccessoryMap.options(MainLift.SQUAT, null).map { it.exerciseName })
        assertEquals(listOf("Spoto press", "Long-pause bench"), AccessoryMap.options(MainLift.BENCH, null).map { it.exerciseName })
        assertEquals(listOf("Deficit deadlift", "Paused deadlift"), AccessoryMap.options(MainLift.DEADLIFT, null).map { it.exerciseName })
        assertTrue(AccessoryMap.options(MainLift.SQUAT, null).all { it.heavy })
    }

    @Test
    fun allExerciseNames_coversEveryOption() {
        for (lift in MainLift.entries) for (miss in MissPoint.entries + listOf(null)) {
            assertTrue(AccessoryMap.allExerciseNames.containsAll(AccessoryMap.options(lift, miss).map { it.exerciseName }))
        }
        assertTrue("RDL" in AccessoryMap.allExerciseNames)
    }
}
