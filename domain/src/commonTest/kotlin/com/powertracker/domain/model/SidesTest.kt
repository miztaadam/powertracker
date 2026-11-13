package com.powertracker.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SidesTest {
    private val day = LocalDate(2026, 11, 2)
    private val bench = Exercise(2, "Bench", Category.BENCH, null, emptyList(), true)
    private val baseline = Baseline(2, 120.0, day)

    private fun session(id: Long, mode: TrainingMode) =
        LoggedSession(id, LocalDate(2026, 11, id.toInt()), null, null, emptyList(), mode)

    private fun advice(type: AdviceType, mode: TrainingMode, payload: AdvicePayload) =
        Advice(type, MainLift.BENCH, day, "why", 1, payload, mode = mode)

    @Test fun historyOnKeepsOnlyThatSidesSessionsAndEverythingElse() {
        val history = TrainingHistory(
            sessions = listOf(
                session(1, TrainingMode.POWERLIFTING),
                session(2, TrainingMode.BODYBUILDING),
                session(3, TrainingMode.POWERLIFTING),
            ),
            exercises = mapOf(2L to bench),
            baselines = listOf(baseline),
        )
        val power = history.on(TrainingMode.POWERLIFTING)
        assertEquals(listOf(1L, 3L), power.sessions.map { it.id })
        assertEquals(history.exercises, power.exercises)
        assertEquals(history.baselines, power.baselines)
        val body = history.on(TrainingMode.BODYBUILDING)
        assertEquals(listOf(2L), body.sessions.map { it.id })
        assertEquals(history.exercises, body.exercises)
        assertEquals(history.baselines, body.baselines)
    }

    @Test fun planOnKeepsOnlyThatSidesItems() {
        val plan = listOf(
            PlanItem(1, DayOfWeek.MONDAY, 2, 3, 5, 8.0, 0),
            PlanItem(2, DayOfWeek.MONDAY, 2, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12),
        )
        assertEquals(listOf(1L), plan.on(TrainingMode.POWERLIFTING).map { it.id })
        assertEquals(listOf(2L), plan.on(TrainingMode.BODYBUILDING).map { it.id })
    }

    @Test fun forSideKeepsTheLiftersDeloadAndReEntryAndOnlyThatSidesRest() {
        val deload = advice(AdviceType.DELOAD, TrainingMode.POWERLIFTING, AdvicePayload.Deload())
        val reEntry = advice(AdviceType.RE_ENTRY, TrainingMode.POWERLIFTING, AdvicePayload.ReEntry())
        val stall = advice(AdviceType.STALL, TrainingMode.POWERLIFTING, AdvicePayload.Accessories(emptyList()))
        val bodyTarget = advice(
            AdviceType.TARGET, TrainingMode.BODYBUILDING,
            AdvicePayload.Target(2, 60.0, 8, 8.0, repsHigh = 12),
        )
        val all = listOf(deload, reEntry, stall, bodyTarget)
        assertEquals(listOf(deload, reEntry, bodyTarget), all.forSide(TrainingMode.BODYBUILDING))
        assertEquals(listOf(deload, reEntry, stall), all.forSide(TrainingMode.POWERLIFTING))
    }

    @Test fun repsTextPrintsFixedRepsOrARangeWithAnEnDash() {
        assertEquals("5", repsText(5, null))
        assertEquals("8–12", repsText(8, 12))
    }

    @Test fun oldConstructorCallsAreOnThePowerliftingSide() {
        val item = PlanItem(1, DayOfWeek.MONDAY, 2, 3, 5, 8.0, 0)
        assertEquals(TrainingMode.POWERLIFTING, item.mode)
        assertNull(item.repsHigh)
        assertEquals(TrainingMode.POWERLIFTING, LoggedSession(1, day, null, null, emptyList()).mode)
        assertEquals(TrainingMode.POWERLIFTING, Advice(AdviceType.DELOAD, null, day, "why", 1, AdvicePayload.Deload()).mode)
        assertNull(AdvicePayload.Target(2, 100.0, 5, 8.0).repsHigh)
    }

    @Test fun targetPayloadWithoutRepsHighStillDecodesAndANullOneIsNotWritten() {
        val stored = """{"type":"com.powertracker.domain.model.AdvicePayload.Target","exerciseId":2,"weightKg":100.0,"reps":5,"rpe":8.0}"""
        val decoded = Json.decodeFromString(AdvicePayload.serializer(), stored)
        assertEquals(AdvicePayload.Target(2, 100.0, 5, 8.0), decoded)

        val fixed = Json.encodeToString(AdvicePayload.serializer(), AdvicePayload.Target(2, 100.0, 5, 8.0))
        assertFalse("repsHigh" in Json.parseToJsonElement(fixed).jsonObject)
        assertEquals(
            setOf("type", "exerciseId", "weightKg", "reps", "rpe"),
            Json.parseToJsonElement(fixed).jsonObject.keys,
        )

        val range = AdvicePayload.Target(2, 60.0, 8, 8.0, repsHigh = 12)
        val rangeJson = Json.encodeToString(AdvicePayload.serializer(), range)
        assertEquals(range, Json.decodeFromString(AdvicePayload.serializer(), rangeJson))
    }
}
