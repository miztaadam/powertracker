package com.powertracker.domain.web

import com.powertracker.domain.ExpectedAdvice
import com.powertracker.domain.GoldenScenario
import com.powertracker.domain.coach.Coach
import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
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
        assertEquals(
            listOf(JsRatioRange("SQUAT", "DEADLIFT", 0.80, 0.90), JsRatioRange("BENCH", "SQUAT", 0.60, 0.70)),
            GritDomain.ratioRanges().toList(),
        )
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
        val total = MainLift.entries.sumOf { GritDomain.currentE1rm(history, it.name, "2026-03-16", 28)!! }
        assertEquals(total, GritDomain.total(history, "2026-03-16"))
        assertNull(GritDomain.total(history, "2025-01-01"))
    }

    @Test
    fun mainLiftOfAnExercise() {
        fun exercise(category: String, parentLift: String?) = JsExercise(1, "x", category, parentLift, emptyArray(), true, false)
        assertEquals("BENCH", GritDomain.mainLiftOf(exercise("BENCH", null)))
        assertTrue(GritDomain.isMainLift(exercise("BENCH", null)))
        // A variation trains its parent lift but is not the lift itself.
        assertEquals("SQUAT", GritDomain.mainLiftOf(exercise("VARIATION", "SQUAT")))
        assertTrue(!GritDomain.isMainLift(exercise("VARIATION", "SQUAT")))
        assertNull(GritDomain.mainLiftOf(exercise("ACCESSORY", null)))
        assertTrue(!GritDomain.isMainLift(exercise("ACCESSORY", null)))
    }

    @Test
    fun lastTopSet_matchesHistoryStats() {
        fun set(kg: Double, failed: Boolean = false, top: Boolean = false) = JsSet(1, kg, 5, 8.0, failed, null, top)
        fun session(id: Int, date: String, vararg sets: JsSet) = JsSession(id, date, null, null, arrayOf(*sets))
        val history = JsHistory(
            arrayOf(
                // Flagged wins over heavier; the last flagged one if several.
                session(1, "2026-01-05", set(100.0, top = true), set(120.0), set(90.0, top = true)),
                // No flag: the heaviest good set, the later one on a tie; a heavier failed set does not count.
                session(2, "2026-01-07", set(110.0), JsSet(1, 110.0, 3, 9.0, false, null, false), set(130.0, failed = true)),
                // Every set failed: the heaviest failed set.
                session(3, "2026-01-09", set(140.0, failed = true), set(135.0, failed = true)),
                session(4, "2026-01-11", JsSet(2, 60.0, 8, null, false, null, false)),
            ),
            arrayOf(JsExercise(1, "Squat", "SQUAT", null, emptyArray(), true, false)),
            emptyArray(),
        )
        assertNull(GritDomain.lastTopSet(history, 1, "2026-01-05"))
        assertEquals(90.0, GritDomain.lastTopSet(history, 1, "2026-01-07")!!.weightKg)
        assertEquals(3, GritDomain.lastTopSet(history, 1, "2026-01-09")!!.reps)
        // A session without the exercise is skipped, and `before` is exclusive.
        assertEquals(140.0, GritDomain.lastTopSet(history, 1, "2026-01-12")!!.weightKg)
        assertNull(GritDomain.lastTopSet(history, 3, "2026-01-12"))
        // The same answer as the domain gives on the golden history.
        val golden = GoldenScenario.historyUntil(GoldenScenario.day(70))
        val stats = com.powertracker.domain.history.HistoryStats(golden)
        for (id in golden.exercises.keys) {
            val expected = stats.lastTopSet(id, GoldenScenario.day(70))?.second
            val actual = GritDomain.lastTopSet(jsHistory(GoldenScenario.day(70)), id.toInt(), GoldenScenario.day(70).toString())
            assertEquals(expected?.let { listOf(it.weightKg, it.reps, it.rpe) }, actual?.let { listOf(it.weightKg, it.reps, it.rpe) })
        }
    }

    @Test
    fun openAdviceExpired_after14Days() {
        // Created 2026-03-01: day 14 is 2026-03-14, the last day it counts.
        for (type in listOf("DELOAD", "RE_ENTRY")) {
            assertTrue(!GritDomain.openAdviceExpired(type, "2026-03-01", "2026-03-14"))
            assertTrue(GritDomain.openAdviceExpired(type, "2026-03-01", "2026-03-15"))
        }
        for (type in listOf("STALL", "WEAK_POINT", "TARGET")) {
            assertTrue(!GritDomain.openAdviceExpired(type, "2026-03-01", "2027-03-15"))
        }
        assertEquals(Coach.OPEN_ACTIVE_DAYS, 14L)
        assertFailsWith<IllegalArgumentException> { GritDomain.openAdviceExpired("NOPE", "2026-03-01", "2026-03-15") }
    }

    @Test
    fun advicePayload_decodesWithAndroidDefaults() {
        val prefix = "com.powertracker.domain.model.AdvicePayload"
        val deload = GritDomain.advicePayload("""{"type":"$prefix.Deload"}""")!!
        assertEquals(listOf<Any?>("Deload", 0.6, 0.9), listOf(deload.kind, deload.volumeFactor, deload.loadFactor))
        val reEntry = GritDomain.advicePayload("""{"type":"$prefix.ReEntry"}""")!!
        assertEquals(listOf<Any?>("ReEntry", 0.85, null), listOf(reEntry.kind, reEntry.loadFactor, reEntry.volumeFactor))
        assertEquals(0.8, GritDomain.advicePayload("""{"type":"$prefix.Deload","volumeFactor":0.5,"loadFactor":0.8}""")!!.loadFactor)

        val target = GritDomain.advicePayload("""{"type":"$prefix.Target","exerciseId":3,"weightKg":102.5,"reps":5,"rpe":8}""")!!
        assertEquals(listOf<Any?>("Target", 3, 102.5, 5, 8.0, null), listOf(target.kind, target.exerciseId, target.weightKg, target.reps, target.rpe, target.sets))
        assertEquals(2, GritDomain.advicePayload("""{"type":"$prefix.Target","exerciseId":3,"weightKg":102.5,"reps":5,"rpe":8,"sets":2}""")!!.sets)

        val accessories = GritDomain.advicePayload(
            """{"type":"$prefix.Accessories","items":[{"exerciseName":"Pause squat","sets":3,"repsLow":3,"repsHigh":5}]}""",
        )!!
        assertEquals("Accessories", accessories.kind)
        assertEquals(listOf(JsPrescription("Pause squat", 3, 3, 5)), accessories.items.toList())

        val weak = GritDomain.advicePayload("""{"type":"$prefix.WeakPoint","lift":"BENCH","missPoint":"LOCKOUT"}""")!!
        assertEquals(listOf<Any?>("WeakPoint", "BENCH", "LOCKOUT"), listOf(weak.kind, weak.lift, weak.missPoint))
        assertNull(GritDomain.advicePayload("""{"type":"$prefix.WeakPoint","lift":"SQUAT","missPoint":null}""")!!.missPoint)

        // What the facade itself encodes decodes again.
        val cp = GoldenScenario.checkpoints.first { it.label == "fatigue" }
        for (a in GritDomain.evaluate(jsHistory(cp.today), jsPlan, jsSettings, cp.today.toString(), emptyArray())) {
            assertTrue(GritDomain.advicePayload(a.payloadJson) != null, a.payloadJson)
        }
        // Anything unreadable is null, never a throw.
        for (bad in listOf("", "{}", "not json", """{"type":"$prefix.Nope"}""", """{"type":"$prefix.Target","exerciseId":"x"}""")) {
            assertNull(GritDomain.advicePayload(bad), bad)
        }
    }

    @Test
    fun library_isPlainDataWithEnumsByName() {
        val all = GritDomain.library()
        assertEquals(Library.all().size, all.size)
        val squat = all.first { it.name == "Squat" }
        assertEquals("MAIN_LIFTS", squat.area)
        assertEquals("SQUAT", squat.category)
        assertEquals("COMPOUND", squat.kind)
        assertNull(squat.parentLift)
        assertContentEquals(arrayOf("BARBELL"), squat.equipment)
        assertContentEquals(arrayOf("Quads", "Glutes"), squat.muscles)
        assertEquals("SQUAT", all.first { it.name == "Pause squat" }.parentLift)

        assertEquals("Pull-up", GritDomain.findInLibrary("pull up")?.name)
        assertEquals("RDL", GritDomain.findInLibrary("Romanian deadlift")?.name)
        assertNull(GritDomain.findInLibrary("My own exercise"))
    }

    @Test
    fun searchLibrary_takesEnumNamesAndRejectsUnknownOnes() {
        val chestDumbbell = GritDomain.searchLibrary("", arrayOf("CHEST"), arrayOf("DUMBBELL")).map { it.name }
        assertEquals(Library.search("", setOf(BodyArea.CHEST), setOf(Equipment.DUMBBELL)).map { it.name }, chestDumbbell)
        assertTrue("DB press" in chestDumbbell)
        assertEquals("RDL", GritDomain.searchLibrary("romanian", emptyArray(), emptyArray()).first().name)
        assertEquals(Library.all().size, GritDomain.searchLibrary("", emptyArray(), emptyArray()).size)
        assertFailsWith<IllegalArgumentException> { GritDomain.searchLibrary("", arrayOf("LEGS"), emptyArray()) }
        assertFailsWith<IllegalArgumentException> { GritDomain.searchLibrary("", emptyArray(), arrayOf("ROPE")) }
    }

    @Test
    fun muscleGroups_takeEnumNamesAndRejectUnknownOnes() {
        assertContentEquals(Library.muscleGroups().map { it.name }.toTypedArray(), GritDomain.muscleGroups())
        assertEquals("CHEST", GritDomain.muscleGroups().first())
        assertEquals("Upper back", GritDomain.muscleGroupLabel("UPPER_BACK"))
        assertEquals("CHEST", GritDomain.muscleGroupOf("pecs"))
        assertNull(GritDomain.muscleGroupOf("Whole body"))
        assertEquals(
            Library.byMuscle(MuscleGroup.CHEST, setOf(Equipment.DUMBBELL)).map { it.name },
            GritDomain.exercisesByMuscle("CHEST", arrayOf("DUMBBELL")).map { it.name },
        )
        assertEquals(Library.byMuscle(MuscleGroup.GLUTES).size, GritDomain.exercisesByMuscle("GLUTES", emptyArray()).size)
        assertFailsWith<IllegalArgumentException> { GritDomain.exercisesByMuscle("Chest", emptyArray()) }
        assertFailsWith<IllegalArgumentException> { GritDomain.exercisesByMuscle("CHEST", arrayOf("ROPE")) }
        assertFailsWith<IllegalArgumentException> { GritDomain.muscleGroupLabel("NECK") }
    }

    @Test
    fun formGuide_matchesTheCatalogue() {
        val guide = GritDomain.formGuide("rdl")
        val expected = Library.find("RDL")!!.guide
        assertEquals(expected.purpose, guide?.purpose)
        assertContentEquals(expected.setup.toTypedArray(), guide?.setup)
        assertContentEquals(expected.steps.toTypedArray(), guide?.steps)
        assertContentEquals(expected.cues.toTypedArray(), guide?.cues)
        assertContentEquals(expected.mistakes.toTypedArray(), guide?.mistakes)
        assertNull(GritDomain.formGuide("My own exercise"))
        assertEquals(Library.SAFETY_LINE, GritDomain.safetyLine())
    }
}
