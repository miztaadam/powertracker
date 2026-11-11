package com.powertracker.domain.seed

import com.powertracker.domain.coach.AccessoryMap
import com.powertracker.domain.model.MainLift
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeedLibraryTest {
    @Test
    fun idsAreOneBasedPositions() {
        val all = SeedLibrary.exercises()
        assertEquals((1L..all.size).toList(), all.map { it.id })
    }

    @Test
    fun containsMainLiftsFirstAndEveryAccessoryMapName() {
        val all = SeedLibrary.exercises()
        assertEquals(listOf("Squat", "Bench", "Deadlift"), all.take(3).map { it.name })
        assertEquals(listOf("Curl", "Row", "Pull-up"), all.takeLast(3).map { it.name })
        // Every name the squat, bench and deadlift rows prescribe. Strict press is not seeded: its rows come from the catalogue.
        val seeded = listOf(MainLift.SQUAT, MainLift.BENCH, MainLift.DEADLIFT).flatMap { lift -> AccessoryMap.allOptions(lift).map { it.exerciseName } }
        assertTrue(all.map { it.name }.toSet().containsAll(seeded))
        assertTrue(all.all { it.isBuiltIn && !it.archived })
    }
}
