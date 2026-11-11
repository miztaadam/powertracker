package com.powertracker.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AdvicePayloadTest {
    @Test fun targetRoundTripsPolymorphically() {
        val payload: AdvicePayload = AdvicePayload.Target(exerciseId = 1, weightKg = 142.5, reps = 5, rpe = 8.0)
        val json = Json.encodeToString<AdvicePayload>(payload)
        assertEquals(payload, Json.decodeFromString<AdvicePayload>(json))
    }

    @Test fun mainLiftDerivation() {
        val squat = Exercise(1, "Squat", Category.SQUAT, null, emptyList(), true)
        val pause = Exercise(2, "Pause squat", Category.VARIATION, MainLift.SQUAT, emptyList(), true)
        val curl = Exercise(3, "Curl", Category.ACCESSORY, null, emptyList(), true)
        assertEquals(MainLift.SQUAT, squat.mainLift)
        assertEquals(true, squat.isMainLift)
        assertEquals(MainLift.SQUAT, pause.mainLift)
        assertEquals(false, pause.isMainLift)
        assertEquals(null, curl.mainLift)
    }
}
