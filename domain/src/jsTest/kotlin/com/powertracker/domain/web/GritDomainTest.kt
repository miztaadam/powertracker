package com.powertracker.domain.web

import com.powertracker.domain.ExpectedAdvice
import com.powertracker.domain.GoldenScenario
import com.powertracker.domain.coach.Coach
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.MainLift
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GritDomainTest {
    private fun jsHistory(until: LocalDate) = GoldenScenario.historyUntil(until).let { h ->
        JsHistory(
            sessions = h.sessions.map { s ->
                JsSession(
                    s.id.toInt(), s.date.toString(), s.bodyweightKg, s.readiness,
                    s.sets.map { JsSet(it.exerciseId.toInt(), it.weightKg, it.reps, it.rpe, it.failed, it.missPoint?.name, it.isTopSet) }.toTypedArray(),
                )
            }.toTypedArray(),
            exercises = h.exercises.values.map {
                JsExercise(it.id.toInt(), it.name, it.category.name, it.parentLift?.name, it.muscles.toTypedArray(), it.isBuiltIn, it.archived)
            }.toTypedArray(),
            baselines = h.baselines.map { JsBaseline(it.exerciseId.toInt(), it.e1rmKg, it.date.toString()) }.toTypedArray(),
        )
    }

    private val jsPlan = GoldenScenario.plan.map {
        JsPlanItem(it.id.toInt(), it.weekday.ordinal + 1, it.exerciseId.toInt(), it.sets, it.reps, it.targetRpe, it.order)
    }.toTypedArray()

    private val jsSettings = JsCoachSettings(GoldenScenario.settings.defaultRpe, GoldenScenario.settings.changePlates)

    private fun jsAdvice(a: Advice) = JsAdvice(
        a.id.toInt(), a.type.name, a.lift?.name, a.createdAt.toString(), a.reason, a.severity, a.status.name,
        Json.encodeToString(AdvicePayload.serializer(), a.payload),
    )

    private fun priorOf(stored: List<Advice>) = stored.map(::jsAdvice).toTypedArray()

    @Test
    fun facade_matchesDomain() {
        for (cp in GoldenScenario.checkpoints) {
            val history = jsHistory(cp.today)
            val today = cp.today.toString()

            val advice = GritDomain.evaluate(history, jsPlan, jsSettings, today, emptyArray())
            assertEquals(
                cp.expectedAdvice,
                advice.map { ExpectedAdvice(AdviceType.valueOf(it.type), it.lift?.let(MainLift::valueOf), it.severity) },
                "advice at ${cp.label}",
            )
            assertTrue(advice.all { it.id == 0 && it.status == "OPEN" && it.createdAt == today })

            val targets = GritDomain.targetsFor(cp.weekday.ordinal + 1, history, jsPlan, jsSettings, today, priorOf(cp.storedAdvice))
            val actual = targets.map {
                val t = Json.decodeFromString(AdvicePayload.serializer(), it.payloadJson) as AdvicePayload.Target
                listOf(t.exerciseId, t.weightKg, t.reps, t.sets)
            }
            val expected = cp.expectedTargets.map { listOf(it.exerciseId, it.weightKg, it.reps, it.sets) }
            assertEquals(expected, actual, "targets at ${cp.label}")
            assertTrue(targets.all { it.type == "TARGET" && it.id == 0 && it.status == "OPEN" })
        }
    }

    @Test
    fun isoDates_roundTrip() {
        for (text in listOf("2026-01-01", "2028-02-29", "2026-12-31")) {
            assertEquals(text, parseDate("date", text).toString())
        }
        assertEquals(DayOfWeek.MONDAY, parseWeekday("weekday", 1))
        assertEquals(DayOfWeek.SUNDAY, parseWeekday("weekday", 7))
        assertEquals("2026-01-05", GritDomain.weeklyTrend(jsHistory(LocalDate(2026, 1, 8)), "SQUAT", "2026-01-08").single().weekStart)
        assertFailsWith<IllegalArgumentException> { parseWeekday("weekday", 0) }
        assertFailsWith<IllegalArgumentException> { parseWeekday("weekday", 8) }
    }

    @Test
    fun payloadJson_matchesAndroid() {
        val cp = GoldenScenario.checkpoints.first { it.label == "fatigue" }
        val domainHistory = GoldenScenario.historyUntil(cp.today)
        val advice = GritDomain.evaluate(jsHistory(cp.today), jsPlan, jsSettings, cp.today.toString(), emptyArray())
        val targets = GritDomain.targetsFor(1, jsHistory(cp.today), jsPlan, jsSettings, cp.today.toString(), priorOf(cp.storedAdvice))
        val android = Json // the configuration AdviceRepository uses to store AdvicePayload

        // Kotlin/JS prints whole doubles without ".0", so compare decoded payloads, never raw strings.
        fun decoded(list: Array<JsAdvice>) = list.map { android.decodeFromString(AdvicePayload.serializer(), it.payloadJson) }
        assertEquals(
            Coach.evaluate(domainHistory, GoldenScenario.plan, GoldenScenario.settings, cp.today, emptyList()).map { it.payload },
            decoded(advice),
        )
        assertEquals(
            Coach.targetsFor(cp.weekday, domainHistory, GoldenScenario.plan, GoldenScenario.settings, cp.today, cp.storedAdvice).map { it.payload },
            decoded(targets),
        )
        // The polymorphic discriminator is the Android one, and the JSON tree equals the Android encoding's tree.
        for (a in advice + targets) {
            val payload = android.decodeFromString(AdvicePayload.serializer(), a.payloadJson)
            assertEquals(
                android.parseToJsonElement(android.encodeToString(AdvicePayload.serializer(), payload)).jsonObject.keys,
                android.parseToJsonElement(a.payloadJson).jsonObject.keys,
            )
            assertTrue(a.payloadJson.contains("\"type\":\"com.powertracker.domain.model.AdvicePayload."), a.payloadJson)
        }
        assertEquals(AdvicePayload.Deload(), decoded(advice).filterIsInstance<AdvicePayload.Deload>().single())
    }

    @Test
    fun invalidDate_throwsClearError() {
        val ex = assertFailsWith<IllegalArgumentException> {
            GritDomain.evaluate(jsHistory(LocalDate(2026, 1, 8)), jsPlan, jsSettings, "2026-13-01", emptyArray())
        }
        assertTrue(ex.message.orEmpty().contains("today"), ex.message)
        assertTrue(ex.message.orEmpty().contains("2026-13-01"), ex.message)
    }

    @Test
    fun idBeyondIntRange_throws() {
        val ex = assertFailsWith<IllegalArgumentException> { 3_000_000_000L.toFacadeId("exercise.id") }
        assertTrue(ex.message.orEmpty().contains("exercise.id"))
    }

    @Test
    fun simpleHelpers() {
        assertContentEquals(arrayOf(25.0, 20.0), GritDomain.platesPerSide(110.0, false)!!)
        assertNull(GritDomain.platesPerSide(19.0, false))
        assertEquals(2.5, GritDomain.loadStep(false))
        assertEquals(0.5, GritDomain.loadStep(true))
        assertEquals(102.5, GritDomain.roundLoad(102.4, false))
        assertContentEquals(arrayOf("HIGH_REPS", "BIG_JUMP"), GritDomain.checkInput(140.0, 15, 100.0))
        assertEquals(182.5, GritDomain.parseBaselineKg("182,5"))
        assertNull(GritDomain.parseBaselineKg("abc"))
        assertContentEquals(doubleArrayOf(0.80, 0.90, 0.60, 0.70).toTypedArray(), GritDomain.ratioRanges())
        assertEquals(listOf("Close-grip bench", "JM press", "Board press"), GritDomain.accessoryOptions("BENCH", "LOCKOUT").map { it.name })
        assertEquals(listOf("Triceps"), GritDomain.accessoryMuscles("BENCH", "LOCKOUT").toList())
        assertEquals(listOf("Quads", "Lats", "Position"), GritDomain.accessoryMuscles("DEADLIFT", "FLOOR").toList())
        assertEquals(emptyList(), GritDomain.accessoryMuscles("SQUAT", null).toList())
        assertTrue(GritDomain.seedExercises().any { it.name == "Squat" })
    }

    @Test
    fun progressFigures() {
        val history = jsHistory(GoldenScenario.day(70))
        val prs = GritDomain.prs(history, "BENCH")
        assertEquals(3, prs.size)
        assertEquals("2026-01-05", prs.first().date)
        val volume = GritDomain.weeklyVolume(history, "BENCH")
        assertEquals(10, volume.size)
        assertEquals(450.0, volume.first().value)
        assertTrue(GritDomain.currentE1rm(history, "SQUAT", "2026-03-16", 28)!! > 0.0)
        assertNull(GritDomain.currentE1rm(history, "SQUAT", "2025-01-01", 28))
    }
}
