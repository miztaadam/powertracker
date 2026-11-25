package com.powertracker.domain.web

import com.powertracker.domain.ExpectedAdvice
import com.powertracker.domain.GoldenScenario
import com.powertracker.domain.coach.Coach
import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Equipment
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.library.RpeGuide
import com.powertracker.domain.library.figure
import com.powertracker.domain.library.figures.Capsule
import com.powertracker.domain.library.figures.Circle
import com.powertracker.domain.library.figures.FigureRenderer
import com.powertracker.domain.library.figures.Polygon
import com.powertracker.domain.library.figures.RoundRect
import com.powertracker.domain.library.figures.Which
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
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
        val sbd = listOf(MainLift.SQUAT, MainLift.BENCH, MainLift.DEADLIFT)
        val total = sbd.sumOf { GritDomain.currentE1rm(history, it.name, "2026-03-16", 28)!! }
        assertEquals(total, GritDomain.total(history, "2026-03-16", false))
        assertNull(GritDomain.total(history, "2025-01-01", false))
        // The golden scenario has no strict press, so a total that includes it is not known yet.
        assertNull(GritDomain.total(history, "2026-03-16", true))
        // Plan's "Best e1RM": with no typed best, the lift's last record.
        assertEquals(prs.last().e1rmKg, GritDomain.knownBest(history, "BENCH"))
        assertNull(GritDomain.knownBest(history, "STRICT_PRESS"))
    }

    @Test
    fun isNewPr_followsTheDomainRule() {
        val history = jsHistory(GoldenScenario.day(70))
        val bench = GritDomain.prs(history, "BENCH").last()
        val benchId = history.exercises.first { it.name == "Bench" }.id
        val heavier = JsSet(benchId, bench.weightKg + 20.0, bench.reps, bench.rpe, false, null, false)
        val same = JsSet(benchId, bench.weightKg, bench.reps, bench.rpe, false, null, false)
        assertTrue(GritDomain.isNewPr(history, heavier, "2026-03-20", "POWERLIFTING"))
        assertFalse(GritDomain.isNewPr(history, same, "2026-03-20", "POWERLIFTING"))
        assertFalse(GritDomain.isNewPr(history, heavier.copy(failed = true), "2026-03-20", "POWERLIFTING"))
        assertFalse(GritDomain.isNewPr(history, heavier, "2026-03-20", "BODYBUILDING"))
    }

    @Test
    fun strictPress_liftsLabelsAndRatios() {
        assertContentEquals(arrayOf("SQUAT", "BENCH", "DEADLIFT", "STRICT_PRESS"), GritDomain.mainLifts())
        assertEquals("Strict press", GritDomain.liftLabel("STRICT_PRESS"))
        assertEquals("Off the shoulders", GritDomain.missPointLabel("STRICT_PRESS", "FLOOR"))
        assertEquals("Mid", GritDomain.missPointLabel("STRICT_PRESS", "MID"))
        assertEquals("Lockout", GritDomain.missPointLabel("STRICT_PRESS", "LOCKOUT"))
        assertEquals("Floor", GritDomain.missPointLabel("BENCH", "FLOOR"))
        assertFailsWith<IllegalArgumentException> { GritDomain.missPointLabel("PRESS", "FLOOR") }
        assertTrue(GritDomain.ratioRanges().none { it.numerator == "STRICT_PRESS" || it.denominator == "STRICT_PRESS" })
        assertEquals(
            listOf("Seated barbell press", "Incline bench press", "Landmine press"),
            GritDomain.accessoryOptions("STRICT_PRESS", "FLOOR").map { it.name },
        )
        assertEquals("STRICT_PRESS", GritDomain.mainLiftOf(JsExercise(4, "Strict press", "STRICT_PRESS", null, emptyArray(), true, false)))
        assertTrue(GritDomain.isMainLift(JsExercise(4, "Strict press", "STRICT_PRESS", null, emptyArray(), true, false)))
        val entry = GritDomain.findInLibrary("Overhead press")!!
        assertEquals("Strict press", entry.name)
        assertEquals("STRICT_PRESS", entry.category)
        assertEquals("MAIN_LIFTS", entry.area)
        assertTrue(GritDomain.seedExercises().none { it.category == "STRICT_PRESS" })
        assertTrue(GritDomain.isStrictPressUpgradeName("overhead  press"))
        assertTrue(!GritDomain.isStrictPressUpgradeName("Landmine press"))
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
    fun lastTimes_oneEntryForEachExerciseDoneOnTheSide() {
        fun set(id: Int, kg: Double, reps: Int, unit: String = "KG") = JsSet(id, kg, reps, 8.0, false, null, false, unit)
        val history = JsHistory(
            arrayOf(
                JsSession(1, "2026-01-05", null, null, arrayOf(set(1, 100.0, 5), set(2, 20.0, 12)), "BODYBUILDING"),
                JsSession(2, "2026-01-12", null, null, arrayOf(set(2, 22.0, 11), set(1, 105.0, 5), set(2, 7.5, 10, "PLATES")), "BODYBUILDING"),
                // The other side, later: not this side's last time.
                JsSession(3, "2026-01-14", null, null, arrayOf(set(2, 30.0, 5)), "POWERLIFTING"),
                JsSession(4, "2026-01-19", null, null, arrayOf(set(1, 110.0, 5)), "BODYBUILDING"),
            ),
            arrayOf(JsExercise(1, "Squat", "SQUAT", null, emptyArray(), true, false), JsExercise(2, "Curl", "ACCESSORY", null, emptyArray(), true, false)),
            emptyArray(),
        )
        val last = GritDomain.lastTimes(history, "BODYBUILDING", "2026-01-19").associateBy { it.exerciseId }
        assertEquals(setOf(1, 2), last.keys)
        assertEquals("2026-01-12", last.getValue(1).date)
        assertEquals(listOf(105.0), last.getValue(1).sets.map { it.weightKg })
        // Every set, in logged order, each in its own unit.
        assertEquals(listOf(Triple(22.0, 11, "KG"), Triple(7.5, 10, "PLATES")), last.getValue(2).sets.map { Triple(it.weightKg, it.reps, it.unit) })
        val powerlifting = GritDomain.lastTimes(history, "POWERLIFTING", "2026-01-19")
        assertEquals(listOf(2 to "2026-01-14"), powerlifting.map { it.exerciseId to it.date })
        assertEquals(0, GritDomain.lastTimes(history, "POWERLIFTING", "2026-01-14").size)
        assertFailsWith<IllegalArgumentException> { GritDomain.lastTimes(history, "NOPE", "2026-01-19") }
        assertFailsWith<IllegalArgumentException> { GritDomain.lastTimes(history, "BODYBUILDING", "soon") }
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
        assertEquals(
            Library.byMuscles(setOf(MuscleGroup.CHEST, MuscleGroup.BICEPS), setOf(Equipment.DUMBBELL)).map { it.name },
            GritDomain.exercisesByMuscles(arrayOf("CHEST", "BICEPS"), arrayOf("DUMBBELL")).map { it.name },
        )
        assertEquals(0, GritDomain.exercisesByMuscles(emptyArray(), emptyArray()).size)
        assertFailsWith<IllegalArgumentException> { GritDomain.exercisesByMuscles(arrayOf("Chest"), emptyArray()) }
        assertFailsWith<IllegalArgumentException> { GritDomain.exercisesByMuscles(arrayOf("CHEST"), arrayOf("ROPE")) }
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

    @Test
    fun rpeGuide_matchesTheDomain() {
        val guide = GritDomain.rpeGuide()
        assertEquals(RpeGuide.title, guide.title)
        assertContentEquals(RpeGuide.lines.toTypedArray(), guide.lines)
    }

    @Test
    fun figureInfo_andShapes_plainData_nullForUnknown() {
        val info = assertNotNull(GritDomain.figureInfo("bench press"))
        val bench = Library.figure("Bench")!!
        assertEquals(bench.startCaption, info.startCaption)
        assertEquals(bench.endCaption, info.endCaption)
        assertEquals("end", info.thumbnail)
        assertContentEquals(arrayOf(400.0, 300.0), info.viewBox)

        for ((which, domainWhich) in listOf("start" to Which.START, "end" to Which.END)) {
            val shapes = assertNotNull(GritDomain.figureShapes("Bench", which))
            val expected = FigureRenderer.shapes(bench, domainWhich)
            assertEquals(expected.size, shapes.size)
            for ((js, shape) in shapes.zip(expected)) {
                assertEquals(shape.role.name, js.role)
                when (shape) {
                    is Circle -> { js as JsFigureCircle; assertEquals("circle", js.kind); assertEquals(listOf(shape.cx, shape.cy, shape.r), listOf(js.cx, js.cy, js.r)) }
                    is Capsule -> {
                        js as JsFigureCapsule
                        assertEquals("capsule", js.kind)
                        assertEquals(listOf(shape.x1, shape.y1, shape.x2, shape.y2, shape.width), listOf(js.x1, js.y1, js.x2, js.y2, js.width))
                    }
                    is RoundRect -> {
                        js as JsFigureRect
                        assertEquals("rect", js.kind)
                        assertEquals(listOf(shape.x, shape.y, shape.w, shape.h, shape.radius), listOf(js.x, js.y, js.w, js.h, js.radius))
                    }
                    is Polygon -> { js as JsFigurePolygon; assertEquals("polygon", js.kind); assertContentEquals(shape.points.toTypedArray(), js.points) }
                }
            }
        }
        assertTrue(GritDomain.figureShapes("Bench", "start")!!.map { it.kind }.toSet().containsAll(listOf("circle", "capsule", "rect", "polygon")))

        assertTrue(GritDomain.hasFigure("bench press"))
        assertTrue(GritDomain.hasFigure("Barbell bench press"))
        assertFalse(GritDomain.hasFigure("My own exercise"))
        assertNull(GritDomain.figureInfo("My own exercise"))
        assertNull(GritDomain.figureShapes("My own exercise", "start"))
        assertFailsWith<IllegalArgumentException> { GritDomain.figureShapes("Bench", "START") }
    }

    @Test
    fun sidesCrossTheBoundary() {
        val oldItem = JsPlanItem(1, 1, 2, 3, 5, 8.0, 0)
        assertEquals("POWERLIFTING", oldItem.mode)
        assertNull(oldItem.repsHigh)
        val item = JsPlanItem(1, 1, 2, 3, 8, 8.0, 0, "BODYBUILDING", 12).toDomain()
        assertEquals(TrainingMode.BODYBUILDING, item.mode)
        assertEquals(12, item.repsHigh)
        assertEquals(TrainingMode.POWERLIFTING, oldItem.toDomain().mode)
        assertFailsWith<IllegalArgumentException> { JsPlanItem(1, 1, 2, 3, 8, 8.0, 0, "bodybuilding").toDomain() }

        assertEquals("POWERLIFTING", JsSession(1, "2026-11-02", null, null, emptyArray()).mode)
        assertEquals(TrainingMode.BODYBUILDING, JsSession(1, "2026-11-02", null, null, emptyArray(), "BODYBUILDING").toDomain().mode)

        val target = AdvicePayload.Target(2, 60.0, 8, 8.0, repsHigh = 12)
        val advice = Advice(AdviceType.TARGET, null, LocalDate(2026, 11, 2), "why", 1, target, mode = TrainingMode.BODYBUILDING)
        val js = advice.toJs()
        assertEquals("BODYBUILDING", js.mode)
        assertEquals(advice, js.toDomain())
        assertEquals("POWERLIFTING", jsAdvice(Advice(AdviceType.DELOAD, null, LocalDate(2026, 11, 2), "why", 1, AdvicePayload.Deload())).mode)
        assertEquals(12, GritDomain.advicePayload(js.payloadJson)!!.repsHigh)
        assertNull(GritDomain.advicePayload(Json.encodeToString(AdvicePayload.serializer(), AdvicePayload.Target(2, 100.0, 5, 8.0)))!!.repsHigh)

        assertEquals("5", GritDomain.repsText(5, null))
        assertEquals("8\u201312", GritDomain.repsText(8, 12))
    }

    @Test
    fun coachingCrossesTheBoundary() {
        for (cp in GoldenScenario.checkpoints) {
            val history = jsHistory(cp.today)
            val today = cp.today.toString()
            val weekday = cp.weekday.ordinal + 1
            val prior = priorOf(cp.storedAdvice)
            assertContentEquals(
                GritDomain.evaluate(history, jsPlan, jsSettings, today, prior),
                GritDomain.coachingEvaluate("POWERLIFTING", history, jsPlan, jsSettings, today, prior),
                cp.label,
            )
            assertContentEquals(
                GritDomain.targetsFor(weekday, history, jsPlan, jsSettings, today, prior),
                GritDomain.coachingTargetsFor("POWERLIFTING", weekday, history, jsPlan, jsSettings, today, prior),
                cp.label,
            )
            assertTrue(GritDomain.coachingEvaluate("BODYBUILDING", history, jsPlan, jsSettings, today, prior).all { it.mode == "BODYBUILDING" })
        }
        assertFailsWith<IllegalArgumentException> {
            GritDomain.coachingEvaluate("bodybuilding", jsHistory(GoldenScenario.day(35)), jsPlan, jsSettings, "2026-02-09", emptyArray())
        }

        // A bodybuilding press done 11, 10, 9 at 26 kg the Monday before.
        val press = JsExercise(52, "DB press", "ACCESSORY", null, emptyArray(), true, false)
        val sets = arrayOf(11, 10, 9).map { JsSet(52, 26.0, it, 8.0, false, null, false) }.toTypedArray()
        val history = JsHistory(arrayOf(JsSession(1, "2026-11-02", null, null, sets, "BODYBUILDING")), arrayOf(press), emptyArray())
        val item = JsPlanItem(1, 1, 52, 3, 8, 8.0, 0, "BODYBUILDING", 12)
        val target = GritDomain.coachingTargetsFor("BODYBUILDING", 1, history, arrayOf(item), jsSettings, "2026-11-09", emptyArray()).single()
        assertEquals("BODYBUILDING", target.mode)
        assertEquals("Last time: 26 \u00d7 11, 10, 9. Beat it. Coach suggests 26 \u00d7 8\u201312 @ 8.", target.reason)
        assertEquals(12, GritDomain.advicePayload(target.payloadJson)!!.repsHigh)
        assertEquals(listOf(12, 11, 10), (1..3).map { GritDomain.repsToBeat(item, history, it, "2026-11-09") })

        val rows = GritDomain.bodybuildingProgress(history, arrayOf(item), "2026-11-09")
        assertEquals(1, rows.size)
        assertEquals(52, rows[0].exerciseId)
        assertEquals("DB press", rows[0].name)
        assertEquals("2026-11-02", rows[0].bestSets.single().date)
        assertEquals(JsSet(52, 26.0, 11, 8.0, false, null, false), rows[0].bestSets.single().set)
        assertNull(rows[0].goingUp)

        val trained = "2026-09-01"
        assertFalse(GritDomain.welcomeBack(null, "2026-11-13", true, trained))
        assertFalse(GritDomain.welcomeBack("2026-10-14", "2026-11-13", true, trained))
        assertTrue(GritDomain.welcomeBack("2026-10-13", "2026-11-13", true, trained))
        assertFalse(GritDomain.welcomeBack("2026-10-13", "2026-11-13", false, trained))
        // A side never trained before it was left: no card.
        assertFalse(GritDomain.welcomeBack("2026-10-13", "2026-11-13", true, null))
        assertFalse(GritDomain.welcomeBack("2026-10-13", "2026-11-13", true, "2026-11-12"))
        assertTrue(GritDomain.welcomeBack("2026-10-13", "2026-11-13", true, "2026-10-13"))
        assertFailsWith<IllegalArgumentException> { GritDomain.welcomeBack("13/10/2026", "2026-11-13", true, trained) }
        assertFailsWith<IllegalArgumentException> { GritDomain.welcomeBack("2026-10-13", "2026-11-13", true, "1/9/2026") }
    }
}
