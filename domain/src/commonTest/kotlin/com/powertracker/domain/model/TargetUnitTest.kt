package com.powertracker.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 1.17: a stored TARGET carries its unit, and every payload stored before reads as kg. */
class TargetUnitTest {
    @Test
    fun aKgTarget_encodesWithNoUnitKey() {
        val json = Json.encodeToString<AdvicePayload>(AdvicePayload.Target(1, 100.0, 5, 8.0))
        assertFalse(json.contains("\"unit\""), json)
    }

    @Test
    fun aPayloadStoredBefore1_17_decodesAsKg() {
        val stored = """{"type":"com.powertracker.domain.model.AdvicePayload.Target","exerciseId":2,"weightKg":100.0,"reps":5,"rpe":8.0}"""
        val payload = Json.decodeFromString<AdvicePayload>(stored) as AdvicePayload.Target
        assertEquals(LoadUnit.KG, payload.unit)
        assertEquals(AdvicePayload.Target(2, 100.0, 5, 8.0), payload)
    }

    @Test
    fun aPlateTarget_roundTrips() {
        val target: AdvicePayload = AdvicePayload.Target(5, 7.5, 8, 8.0, repsHigh = 12, unit = LoadUnit.PLATES)
        val json = Json.encodeToString(target)
        assertTrue(json.contains("\"unit\":\"PLATES\""), json)
        assertEquals(target, Json.decodeFromString<AdvicePayload>(json))
    }
}
