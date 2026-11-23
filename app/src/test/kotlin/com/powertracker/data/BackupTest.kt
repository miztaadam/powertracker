package com.powertracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.db.AppDatabase
import com.powertracker.data.db.ExerciseEntity
import com.powertracker.data.db.SessionEntity
import com.powertracker.data.db.SetEntity
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.data.db.AdviceEntity
import com.powertracker.data.db.PlanItemEntity
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupTest {
    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var service: BackupService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
        settings = SettingsRepository(context)
        service = BackupService(db, settings)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedData() {
        settings.update { AppSettings(changePlates = true, defaultRpe = 7.5, trainingDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY)) }
        val ex = db.exercises().all().first().id
        val s = db.sessions().insert(SessionEntity(date = LocalDate(2026, 5, 4), bodyweightKg = 90.5, readiness = 4))
        db.sets().insert(SetEntity(sessionId = s, exerciseId = ex, weightKg = 100.0, reps = 5, rpe = 8.0, failed = false, missPoint = null, isTopSet = true))
    }

    /** Spec §5: a backup made after adding catalogue exercises restores on this version, and holds only ordinary rows. */
    @Test
    fun backupAfterCatalogueAdd_roundTrips() = runTest {
        val training = TrainingRepository(db)
        val name = "Goblet squat"
        for (row in db.exercises().all().filter { it.name.equals(name, ignoreCase = true) }) db.exercises().delete(row.id)
        training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, 0, sets = 3, reps = 10, targetRpe = 8.0, order = 0), name)
        val session = training.startSession(LocalDate(2026, 11, 10), null, null)
        training.logSet(session, LoggedSet(0, 24.0, 10, 8.0, failed = false, missPoint = null, isTopSet = false), name)

        val exported = service.export(1L)
        val text = Backup.encode(exported)
        val row = exported.exercises.single { it.name == name }
        assertTrue(row.isBuiltIn)
        assertEquals(Library.find(name)!!.muscles, row.muscles)
        assertEquals(row.id, exported.planItems.single().exerciseId)
        assertEquals(row.id, exported.sets.single().exerciseId)
        // Every exercise is an ordinary row: the same fields as before the catalogue, and no catalogue data.
        val ordinary = setOf("id", "name", "category", "parentLift", "muscles", "isBuiltIn", "archived")
        for (e in Json.parseToJsonElement(text).jsonObject.getValue("exercises").jsonArray) assertEquals(ordinary, e.jsonObject.keys)

        // Restored into an empty app on this version, it comes back the same.
        val decoded = Backup.decode(text)
        assertTrue(decoded is BackupResult.Ok)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fresh = AppDatabase.inMemory(context)
        try {
            val restoredService = BackupService(fresh, settings)
            restoredService.restore((decoded as BackupResult.Ok).file)
            assertEquals(exported, restoredService.export(1L))
            val restored = fresh.exercises().all().single { it.name == name }
            assertEquals(row.id, restored.id)
            assertTrue(restored.isBuiltIn)
            assertEquals(row.id, TrainingRepository(fresh).plan().first().single().exerciseId)
        } finally {
            fresh.close()
        }
    }

    @Test
    fun encodeDecode_roundTrips() = runTest {
        seedData()
        val file = service.export(1234L)
        assertTrue(file.sets.isNotEmpty())
        assertEquals(BackupResult.Ok(file), Backup.decode(Backup.encode(file)))
    }

    @Test
    fun newerVersion_isTooNew() = runTest {
        val file = service.export(1L).copy(version = Backup.VERSION + 1)
        assertEquals(BackupResult.TooNew(Backup.VERSION + 1), Backup.decode(Backup.encode(file)))
    }

    @Test
    fun garbage_isCorrupt() {
        assertTrue(Backup.decode("not json") is BackupResult.Corrupt)
        assertTrue(Backup.decode("{}") is BackupResult.Corrupt)
        assertTrue(Backup.decode("[1]") is BackupResult.Corrupt)
        assertTrue(Backup.decode("""{"version":"x"}""") is BackupResult.Corrupt)
        assertTrue(Backup.decode("""{"version":1}""") is BackupResult.Corrupt)
    }

    @Test
    fun restore_replacesAllData() = runTest {
        seedData()
        val exported = service.export(1L)
        db.sets().clear()
        db.sessions().clear()
        db.sessions().insert(SessionEntity(date = LocalDate(2026, 6, 1), bodyweightKg = null, readiness = null))
        settings.update { AppSettings() }

        service.restore(exported)

        assertEquals(exported, service.export(1L))
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY), settings.settings.first().trainingDays)
    }

    @Test
    fun restore_failureLeavesDataUntouched() = runTest {
        seedData()
        val before = service.export(1L)
        val bad = before.copy(sets = before.sets.map { it.copy(exerciseId = 999_999) })

        assertThrows(Exception::class.java) { kotlinx.coroutines.runBlocking { service.restore(bad) } }

        assertEquals(before, service.export(1L))
    }

    @Test
    fun restore_invalidSettings_leavesDataUntouched() = runTest {
        seedData()
        val before = service.export(1L)
        val bad = before.copy(settings = before.settings.copy(trainingDays = listOf(9)))

        assertThrows(Exception::class.java) { kotlinx.coroutines.runBlocking { service.restore(bad) } }

        assertEquals(before, service.export(1L))
        assertTrue(Backup.decode(Backup.encode(bad)) is BackupResult.Corrupt)
    }

    @Test
    fun restAlert_roundTripsThroughBackup() = runTest {
        seedData()
        settings.update { it.copy(restAlert = RestAlert.SOUND_AND_VIBRATE) }
        val text = Backup.encode(service.export(1L))
        settings.update { AppSettings() }

        service.restore((Backup.decode(text) as BackupResult.Ok).file)

        assertEquals(RestAlert.SOUND_AND_VIBRATE, settings.settings.first().restAlert)
    }

    /** A file written before the rest alert existed (and before the app was called Grit) still restores. */
    @Test
    fun backupWithoutRestAlert_decodesWithVibrate() {
        val old = """{"version":1,"exportedAt":1,"settings":{"changePlates":true,"defaultRpe":7.5,"trainingDays":[2,6],""" +
            """"appLock":false,"restMainSec":180,"restAccessorySec":90},"exercises":[],"sessions":[],"sets":[],""" +
            """"planItems":[],"advice":[],"baselines":[]}"""

        val result = Backup.decode(old)

        assertTrue(result.toString(), result is BackupResult.Ok)
        val restored = (result as BackupResult.Ok).file.settings.toAppSettings()
        assertEquals(RestAlert.VIBRATE, restored.restAlert)
        assertTrue(restored.changePlates)
        assertFalse(restored.showStrictPress)
    }

    @Test
    fun webRestAlert_roundTripsOpaquely() = runTest {
        seedData()
        settings.update { it.copy(webRestAlert = "{\"sound\":true}") }
        val text = Backup.encode(service.export(1L))
        settings.update { AppSettings() }

        service.restore((Backup.decode(text) as BackupResult.Ok).file)

        assertEquals("{\"sound\":true}", settings.settings.first().webRestAlert)
        settings.update { it.copy(webRestAlert = null) }
        assertNull(settings.settings.first().webRestAlert)
    }

    @Test
    fun unknownRestAlert_isCorrupt() = runTest {
        val file = service.export(1L)
        val bad = file.copy(settings = file.settings.copy(restAlert = "LOUD"))
        assertTrue(Backup.decode(Backup.encode(bad)) is BackupResult.Corrupt)
    }

    @Test
    fun outOfRangeAdviceDate_isCorrupt() = runTest {
        val file = service.export(1L)
        val advice = AdviceDto(1, "DELOAD", null, Long.MAX_VALUE, "Take a lighter week", 2, """{"type":"deload"}""", "OPEN")
        assertTrue(Backup.decode(Backup.encode(file.copy(advice = listOf(advice)))) is BackupResult.Corrupt)
    }

    /** Strict press spec §4.3: a version-1 file's "Overhead press" row is converted on restore by the §4.1 rule. */
    @Test
    fun v1Restore_convertsOverheadPress() = runTest {
        seedData()
        val ohp = db.exercises().insert(ExerciseEntity(name = "Overhead press", category = Category.ACCESSORY, parentLift = null, muscles = listOf("Delts"), isBuiltIn = false))
        val mil = db.exercises().insert(ExerciseEntity(name = "Military press", category = Category.ACCESSORY, parentLift = null, muscles = listOf("Delts"), isBuiltIn = false))
        val session = db.sessions().all().single().id
        db.sets().insert(SetEntity(sessionId = session, exerciseId = ohp, weightKg = 60.0, reps = 5, rpe = 8.0, failed = false, missPoint = null, isTopSet = true))
        db.sets().insert(SetEntity(sessionId = session, exerciseId = mil, weightKg = 55.0, reps = 5, rpe = 8.0, failed = false, missPoint = null, isTopSet = false))
        val before = service.export(1L)
        val v1 = asVersion1(Backup.encode(before))

        val decoded = Backup.decode(v1)
        assertTrue(decoded.toString(), decoded is BackupResult.Ok)
        assertEquals(1, (decoded as BackupResult.Ok).file.version)
        settings.update { it.copy(showStrictPress = true) }
        service.restore(decoded.file)

        val after = service.export(1L)
        assertEquals(Backup.VERSION, after.version)
        assertEquals(ExerciseDto(ohp, "Strict press", "STRICT_PRESS", null, listOf("Delts"), isBuiltIn = true, archived = false), after.exercises.single { it.id == ohp })
        assertEquals(ExerciseDto(mil, "Military press", "STRICT_PRESS", null, listOf("Delts"), isBuiltIn = false, archived = false), after.exercises.single { it.id == mil })
        // Nothing else changes: the other rows, every set and the settings (a v1 file means the switch is off).
        assertEquals(before.exercises.filter { it.id != ohp && it.id != mil }, after.exercises.filter { it.id != ohp && it.id != mil })
        assertEquals(before.sets, after.sets)
        assertEquals(before.settings.copy(showStrictPress = false), after.settings)
        assertFalse(settings.settings.first().showStrictPress)
    }

    @Test
    fun v2RoundTrip_keepsShowStrictPress() = runTest {
        seedData()
        settings.update { it.copy(showStrictPress = true) }
        // A v2 file is already upgraded: a row named like the strict press is left as it is.
        db.exercises().insert(ExerciseEntity(name = "OHP", category = Category.ACCESSORY, parentLift = null, muscles = listOf("Delts"), isBuiltIn = false))
        val exported = service.export(1L)
        val text = Backup.encode(exported)
        val settingsJson = Json.parseToJsonElement(text).jsonObject.getValue("settings").jsonObject
        assertEquals(Backup.VERSION, Json.parseToJsonElement(text).jsonObject.getValue("version").jsonPrimitive.int)
        assertEquals(true, settingsJson.getValue("showStrictPress").jsonPrimitive.boolean)
        settings.update { AppSettings() }

        service.restore((Backup.decode(text) as BackupResult.Ok).file)

        assertTrue(settings.settings.first().showStrictPress)
        assertEquals(exported, service.export(1L))
    }

    /** An app that only reads version 1 turns a version-2 file away with the existing "too new" result. */
    @Test
    fun v2File_onVersion1Decoder_isTooNew() = runTest {
        seedData()
        val text = Backup.encode(service.export(1L))

        assertEquals(BackupResult.TooNew(Backup.VERSION), Backup.decode(text, supportedVersion = 1))
        assertTrue(Backup.decode(text) is BackupResult.Ok)
    }

    /** Both sides' plans, a session and advice on each side, and both left-on dates. */
    private suspend fun seedBothSides() {
        settings.update {
            AppSettings(
                trainingDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY), trainingMode = TrainingMode.BODYBUILDING,
                bodybuildingTrainingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                powerliftingLeftOn = LocalDate(2026, 11, 2), bodybuildingLeftOn = LocalDate(2026, 9, 28),
            )
        }
        val training = TrainingRepository(db)
        val squat = db.exercises().all().first { it.name == "Squat" }.id
        val curl = db.exercises().all().first { it.name == "Curl" }.id
        training.savePlanItem(PlanItem(0, DayOfWeek.TUESDAY, squat, sets = 5, reps = 5, targetRpe = 8.0, order = 0))
        training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, curl, sets = 3, reps = 8, targetRpe = 8.0, order = 0, mode = TrainingMode.BODYBUILDING, repsHigh = 12))
        val pl = training.startSession(LocalDate(2026, 10, 27), 84.5, 4)
        training.logSet(pl, LoggedSet(squat, 140.0, 5, 8.0, failed = false, missPoint = null, isTopSet = true))
        val bb = training.startSession(LocalDate(2026, 11, 3), null, null, TrainingMode.BODYBUILDING)
        training.logSet(bb, LoggedSet(curl, 16.0, 11, 8.0, failed = false, missPoint = null, isTopSet = false))
        db.advice().insert(
            listOf(
                AdviceEntity(type = AdviceType.TARGET, lift = null, createdAt = LocalDate(2026, 11, 3), reason = "Coach suggests 16 × 8–12 @ 8.",
                    severity = 0, payload = """{"type":"target","exerciseId":$curl,"weightKg":16.0,"reps":8,"rpe":8.0,"repsHigh":12}""",
                    status = AdviceStatus.OPEN, mode = TrainingMode.BODYBUILDING),
                AdviceEntity(type = AdviceType.DELOAD, lift = null, createdAt = LocalDate(2026, 10, 27), reason = "Take a lighter week",
                    severity = 2, payload = """{"type":"deload"}""", status = AdviceStatus.DISMISSED),
            ),
        )
    }

    /** Bodybuilding mode spec §6: version 3 carries the sides, and restores exactly. */
    @Test
    fun v3RoundTrip_keepsBothSides() = runTest {
        seedBothSides()
        val exported = service.export(1L)
        val text = Backup.encode(exported)
        val root = Json.parseToJsonElement(text).jsonObject
        assertEquals(Backup.VERSION, root.getValue("version").jsonPrimitive.int)
        val s = root.getValue("settings").jsonObject
        assertEquals("BODYBUILDING", s.getValue("trainingMode").jsonPrimitive.content)
        assertEquals(listOf(1, 3, 4, 5), s.getValue("bodybuildingTrainingDays").jsonArray.map { it.jsonPrimitive.int })
        assertEquals(LocalDate(2026, 11, 2).toEpochDays().toInt(), s.getValue("powerliftingLeftOn").jsonPrimitive.int)
        assertEquals(LocalDate(2026, 9, 28).toEpochDays().toInt(), s.getValue("bodybuildingLeftOn").jsonPrimitive.int)
        assertEquals(listOf("POWERLIFTING", "BODYBUILDING"), modes(root, "planItems"))
        assertEquals(listOf("POWERLIFTING", "BODYBUILDING"), modes(root, "sessions"))
        assertEquals(listOf("BODYBUILDING", "POWERLIFTING"), modes(root, "advice"))
        assertEquals(listOf("null", "12"), root.getValue("planItems").jsonArray.map { it.jsonObject.getValue("repsHigh").toString() })

        db.plan().clear()
        settings.update { AppSettings() }
        service.restore((Backup.decode(text) as BackupResult.Ok).file)

        assertEquals(exported, service.export(1L))
        val restored = settings.settings.first()
        assertEquals(TrainingMode.BODYBUILDING, restored.trainingMode)
        assertEquals(LocalDate(2026, 11, 2), restored.powerliftingLeftOn)
        assertEquals(LocalDate(2026, 9, 28), restored.bodybuildingLeftOn)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), restored.activeTrainingDays)
        assertEquals(1, TrainingRepository(db).plan(TrainingMode.BODYBUILDING).first().size)
    }

    /** A version 2 file (no side anywhere) restores with everything on the powerlifting side. */
    @Test
    fun v2File_restoresAsAllPowerlifting() = runTest {
        seedBothSides()
        val v2 = asVersion2(Backup.encode(service.export(1L)))
        settings.update { AppSettings() }

        val decoded = Backup.decode(v2)
        assertTrue(decoded.toString(), decoded is BackupResult.Ok)
        service.restore((decoded as BackupResult.Ok).file)

        assertTrue(db.plan().all().all { it.mode == TrainingMode.POWERLIFTING && it.repsHigh == null })
        assertTrue(db.sessions().all().all { it.mode == TrainingMode.POWERLIFTING })
        assertTrue(db.advice().all().all { it.mode == TrainingMode.POWERLIFTING })
        assertEquals(2, db.sessions().all().size)
        assertEquals(2, db.sets().all().size)
        val s = settings.settings.first()
        assertEquals(TrainingMode.POWERLIFTING, s.trainingMode)
        assertEquals(AppSettings().bodybuildingTrainingDays, s.bodybuildingTrainingDays)
        assertNull(s.powerliftingLeftOn)
        assertNull(s.bodybuildingLeftOn)
    }

    /** 1.13: an app that reads only version 4 (1.12) turns a version 5 file away as too new. */
    @Test
    fun v5File_onVersion4Decoder_isTooNew() = runTest {
        val text = Backup.encode(service.export(1L).copy(version = 5))
        assertEquals(BackupResult.TooNew(5), Backup.decode(text, supportedVersion = 4))
    }

    /** 1.13: every backup is version 5 and has a `skips` list, even with nothing skipped. */
    @Test
    fun everyBackup_isVersion5_withOrWithoutSkips() = runTest {
        val empty = Json.parseToJsonElement(Backup.encode(service.export(1L))).jsonObject
        assertEquals(Backup.VERSION, empty.getValue("version").jsonPrimitive.int)
        assertEquals(JsonArray(emptyList()), empty.getValue("skips"))

        seedSkips()
        val full = Json.parseToJsonElement(Backup.encode(service.export(1L))).jsonObject
        assertEquals(Backup.VERSION, full.getValue("version").jsonPrimitive.int)
        assertEquals(3, full.getValue("skips").jsonArray.size)
    }

    /** 1.13: version 5 carries the skips and their cards, and restores exactly. */
    @Test
    fun v5RoundTrip_keepsSkips() = runTest {
        seedBothSides()
        val ids = seedSkips()
        val exported = service.export(1L)
        val text = Backup.encode(exported)
        val skips = Json.parseToJsonElement(text).jsonObject.getValue("skips").jsonArray.map { it.jsonObject }
        assertEquals(listOf("id", "date", "mode", "exerciseId", "planItemId", "reason"), skips[0].keys.toList())
        assertEquals(listOf("PAIN", null, "TIME"), skips.map { it.getValue("reason").jsonPrimitive.contentOrNullValue() })
        assertEquals(LocalDate(2026, 11, 18).toEpochDays().toInt(), skips[0].getValue("date").jsonPrimitive.int)

        db.skips().clear()
        db.advice().clear()
        service.restore((Backup.decode(text) as BackupResult.Ok).file)

        assertEquals(exported, service.export(1L))
        val read = TrainingRepository(db).skips().first().sortedBy { it.id }
        assertEquals(ids, read.map { it.id })
        assertEquals(listOf(com.powertracker.domain.model.SkipReason.PAIN, null, com.powertracker.domain.model.SkipReason.TIME), read.map { it.reason })
        val card = db.advice().all().single { it.type == AdviceType.SKIP }
        assertEquals(ids[0], (Json.decodeFromString(com.powertracker.domain.model.AdvicePayload.serializer(), card.payload) as com.powertracker.domain.model.AdvicePayload.Skipped).skipId)
    }

    /** 1.13: a version 4 (1.12) file has no skips: it restores with none, and replaces any stored. */
    @Test
    fun v4File_restoresWithNoSkips() = runTest {
        seedBothSides()
        seedStage3()
        seedSkips()
        val root = Json.parseToJsonElement(Backup.encode(service.export(1L))).jsonObject
        val v4 = JsonObject(root - "skips" + ("version" to JsonPrimitive(4)) +
            ("advice" to JsonArray(root.getValue("advice").jsonArray.filter { it.jsonObject.getValue("type").jsonPrimitive.content != "SKIP" })))

        val decoded = Backup.decode(v4.toString(), supportedVersion = 4)
        assertTrue(decoded.toString(), decoded is BackupResult.Ok)
        assertEquals(emptyList<Any>(), (decoded as BackupResult.Ok).file.skips)
        service.restore(decoded.file)

        assertEquals(emptyList<Any>(), db.skips().all())
        assertEquals(2, db.checkIns().all().size)
        assertEquals(2, db.sessions().all().size)
    }

    @Test
    fun badSkips_areCorrupt() = runTest {
        seedSkips()
        val file = service.export(1L)
        val first = file.skips.first()
        for (bad in listOf(
            file.copy(skips = file.skips + first.copy(id = 99)),
            file.copy(skips = listOf(first.copy(reason = "TIRED"))),
            file.copy(skips = listOf(first.copy(mode = "CROSSFIT"))),
        )) {
            assertTrue(bad.toString(), Backup.decode(Backup.encode(bad)) is BackupResult.Corrupt)
        }
    }

    /** 1.13: a version 5 file restored over other version 5 skips replaces them, one on the same date, side and exercise included. */
    @Test
    fun v5File_restoredOverOtherSkips_replacesThem() = runTest {
        seedBothSides()
        seedSkips()
        val exported = service.export(1L)
        val text = Backup.encode(exported)

        // Different skips now, under new ids: one on the same date, side and exercise with another reason, and three more.
        db.skips().clear()
        db.advice().clear()
        val training = TrainingRepository(db)
        val exercises = db.exercises().all().associateBy { it.name }
        val day = LocalDate(2026, 11, 18)
        training.skip(com.powertracker.domain.model.Skip(0, day, TrainingMode.BODYBUILDING, exercises.getValue("Curl").id, null, com.powertracker.domain.model.SkipReason.BUSY)) { _, _ -> emptyList() }
        training.skip(com.powertracker.domain.model.Skip(0, day, TrainingMode.POWERLIFTING, exercises.getValue("Bench").id, null, com.powertracker.domain.model.SkipReason.PAIN)) { _, _ -> emptyList() }
        training.skip(com.powertracker.domain.model.Skip(0, LocalDate(2026, 11, 1), TrainingMode.POWERLIFTING, exercises.getValue("Deadlift").id, null, null)) { _, _ -> emptyList() }
        training.skip(com.powertracker.domain.model.Skip(0, LocalDate(2026, 11, 2), TrainingMode.POWERLIFTING, exercises.getValue("Row").id, null, null)) { _, _ -> emptyList() }

        service.restore((Backup.decode(text) as BackupResult.Ok).file)

        assertEquals(exported, service.export(1L))
        assertEquals(exported.skips.size, db.skips().all().size)
    }

    /** 1.13: a version 6 file, from a later app, is turned away as too new. */
    @Test
    fun v6File_isTooNew() = runTest {
        seedSkips()
        val root = Json.parseToJsonElement(Backup.encode(service.export(1L))).jsonObject
        val v6 = JsonObject(root + ("version" to JsonPrimitive(7)))
        assertEquals(BackupResult.TooNew(7), Backup.decode(v6.toString()))
    }

    /** Three skips: today's pain skip of curl on the bodybuilding side (with its card), and two on the powerlifting side. */
    private suspend fun seedSkips(): List<Long> {
        val training = TrainingRepository(db)
        val exercises = db.exercises().all().associateBy { it.name }
        val curl = exercises.getValue("Curl").id
        val squat = exercises.getValue("Squat").id
        val item = training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, curl, 3, 8, 8.0, 5, TrainingMode.BODYBUILDING, 12))
        val day = LocalDate(2026, 11, 18)
        val pain = training.skip(com.powertracker.domain.model.Skip(0, day, TrainingMode.BODYBUILDING, curl, item, com.powertracker.domain.model.SkipReason.PAIN)) { stored, _ ->
            listOf(
                com.powertracker.domain.model.Advice(
                    AdviceType.SKIP, null, day, "You skipped Curl because of pain.", 3,
                    com.powertracker.domain.model.AdvicePayload.Skipped(stored.id, curl, "Curl", stored.reason, listOf("Cable curl")),
                    mode = TrainingMode.BODYBUILDING,
                ),
            )
        }
        val none = training.skip(com.powertracker.domain.model.Skip(0, day, TrainingMode.POWERLIFTING, squat, null, null)) { _, _ -> emptyList() }
        val time = training.skip(com.powertracker.domain.model.Skip(0, LocalDate(2026, 11, 11), TrainingMode.POWERLIFTING, squat, null, com.powertracker.domain.model.SkipReason.TIME)) { _, _ -> emptyList() }
        return listOf(pain, none, time)
    }

    private fun JsonPrimitive.contentOrNullValue(): String? = if (this is kotlinx.serialization.json.JsonNull) null else content

    /** Stage 3: version 4 carries the check-ins and the two new advice types, and restores exactly. */
    @Test
    fun v4RoundTrip_keepsCheckInsAndTheNewAdvice() = runTest {
        seedBothSides()
        seedStage3()
        val exported = service.export(1L)
        val text = Backup.encode(exported)
        val root = Json.parseToJsonElement(text).jsonObject
        assertEquals(Backup.VERSION, root.getValue("version").jsonPrimitive.int)
        val checkIns = root.getValue("checkIns").jsonArray.map { it.jsonObject }
        assertEquals(listOf(LocalDate(2026, 10, 13).toEpochDays().toInt(), LocalDate(2026, 11, 10).toEpochDays().toInt()),
            checkIns.map { it.getValue("date").jsonPrimitive.int })
        assertEquals(listOf("CHEST", "SIDE_DELTS"), checkIns[1].getValue("lagging").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("QUADS"), checkIns[1].getValue("growingWell").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("id", "date", "lagging", "growingWell"), checkIns[0].keys.toList())
        val types = root.getValue("advice").jsonArray.map { it.jsonObject.getValue("type").jsonPrimitive.content }
        assertTrue(types.containsAll(listOf("MORE_WORK", "SWAP")))

        db.checkIns().clear()
        db.advice().clear()
        service.restore((Backup.decode(text) as BackupResult.Ok).file)

        assertEquals(exported, service.export(1L))
        val read = TrainingRepository(db).checkIns().first()
        assertEquals(setOf(com.powertracker.domain.library.MuscleGroup.QUADS), read[1].growingWell)
        val payloads = db.advice().all().filter { it.type == AdviceType.MORE_WORK || it.type == AdviceType.SWAP }
            .map { Json.decodeFromString(com.powertracker.domain.model.AdvicePayload.serializer(), it.payload) }
        assertTrue(payloads.any { it is com.powertracker.domain.model.AdvicePayload.MoreWork })
        assertTrue(payloads.any { it is com.powertracker.domain.model.AdvicePayload.Swap })
    }

    /** A version 3 file has no check-ins: it restores with none, and replaces any stored. */
    @Test
    fun v3File_restoresWithNoCheckIns() = runTest {
        seedBothSides()
        seedStage3()
        val root = Json.parseToJsonElement(Backup.encode(service.export(1L))).jsonObject
        val v3 = JsonObject(root - "checkIns" + ("version" to JsonPrimitive(3)) +
            ("advice" to JsonArray(root.getValue("advice").jsonArray.filter { it.jsonObject.getValue("type").jsonPrimitive.content !in setOf("MORE_WORK", "SWAP") })))

        val decoded = Backup.decode(v3.toString())
        assertTrue(decoded.toString(), decoded is BackupResult.Ok)
        service.restore((decoded as BackupResult.Ok).file)

        assertEquals(emptyList<Any>(), db.checkIns().all())
        assertEquals(2, db.sessions().all().size)
    }

    @Test
    fun badCheckIns_areCorrupt() = runTest {
        seedBothSides()
        seedStage3()
        val file = service.export(1L)
        val first = file.checkIns.first()
        for (bad in listOf(
            file.copy(checkIns = file.checkIns + first.copy(id = 99)),
            file.copy(checkIns = listOf(first.copy(lagging = listOf("WINGS")))),
            file.copy(checkIns = listOf(first.copy(lagging = listOf("CHEST"), growingWell = listOf("CHEST")))),
        )) {
            assertTrue(bad.toString(), Backup.decode(Backup.encode(bad)) is BackupResult.Corrupt)
        }
    }

    private suspend fun seedStage3() {
        val training = TrainingRepository(db)
        training.saveCheckIn(com.powertracker.domain.model.CheckIn(LocalDate(2026, 10, 13), emptySet(), setOf(com.powertracker.domain.library.MuscleGroup.BICEPS)))
        training.saveCheckIn(
            com.powertracker.domain.model.CheckIn(
                LocalDate(2026, 11, 10),
                setOf(com.powertracker.domain.library.MuscleGroup.CHEST, com.powertracker.domain.library.MuscleGroup.SIDE_DELTS),
                setOf(com.powertracker.domain.library.MuscleGroup.QUADS),
            ),
        )
        val curl = db.exercises().all().first { it.name == "Curl" }.id
        AdviceRepository(db).insert(
            listOf(
                com.powertracker.domain.model.Advice(
                    AdviceType.MORE_WORK, null, LocalDate(2026, 11, 10), "You marked chest as lagging.", 0,
                    com.powertracker.domain.model.AdvicePayload.MoreWork(
                        com.powertracker.domain.library.MuscleGroup.CHEST, emptyList(),
                        com.powertracker.domain.model.NewExercise(DayOfWeek.MONDAY, "Pec deck", 3, 8, 12, 8.0), 0, 3,
                    ),
                    mode = TrainingMode.BODYBUILDING,
                ),
                com.powertracker.domain.model.Advice(
                    AdviceType.SWAP, null, LocalDate(2026, 11, 3), "Curl hasn't moved in 3 weeks.", 0,
                    com.powertracker.domain.model.AdvicePayload.Swap(
                        curl, "Curl", com.powertracker.domain.library.MuscleGroup.BICEPS, LocalDate(2026, 10, 13), listOf("Cable curl", "EZ-bar curl"),
                    ),
                    status = AdviceStatus.DISMISSED, mode = TrainingMode.BODYBUILDING,
                ),
            ),
        )
    }

    /** An app that reads only version 2 turns a version 3 file away. */
    @Test
    fun v3File_onVersion2Decoder_isTooNew() = runTest {
        seedBothSides()
        assertEquals(BackupResult.TooNew(3), Backup.decode(Backup.encode(service.export(1L).copy(version = 3)), supportedVersion = 2))
    }

    @Test
    fun unknownMode_isCorrupt() = runTest {
        seedBothSides()
        val file = service.export(1L)
        for (bad in listOf(
            file.copy(settings = file.settings.copy(trainingMode = "CROSSFIT")),
            file.copy(planItems = file.planItems.map { it.copy(mode = "CROSSFIT") }),
            file.copy(sessions = file.sessions.map { it.copy(mode = "CROSSFIT") }),
            file.copy(advice = file.advice.map { it.copy(mode = "CROSSFIT") }),
            file.copy(settings = file.settings.copy(bodybuildingTrainingDays = listOf(8))),
        )) {
            assertTrue(bad.toString(), Backup.decode(Backup.encode(bad)) is BackupResult.Corrupt)
        }
    }

    /** 1.17: a plate exercise (Half steps too), a kg exercise, and a session holding a kg set, a plate set and a plate 7½ set. */
    private suspend fun seedPlates(): Pair<Long, Long> {
        val training = TrainingRepository(db)
        val pulldown = training.setPlateCount(null, "Lat pulldown", PlateCount.HALF)
        val squat = db.exercises().all().first { it.name == "Squat" }.id
        val s = training.startSession(LocalDate(2026, 11, 18), null, null)
        training.logSet(s, LoggedSet(squat, 140.0, 5, 8.0, failed = false, missPoint = null, isTopSet = true))
        training.logSet(s, LoggedSet(pulldown, 50.0, 10, 8.0, failed = false, missPoint = null, isTopSet = false))
        training.logSet(s, LoggedSet(pulldown, 7.0, 10, 8.0, failed = false, missPoint = null, isTopSet = false, unit = LoadUnit.PLATES))
        training.logSet(s, LoggedSet(pulldown, 7.5, 9, 8.5, failed = false, missPoint = null, isTopSet = true, unit = LoadUnit.PLATES))
        return pulldown to squat
    }

    /** 1.17: version 6 carries each set's unit and each exercise's plate setting, and restores exactly. */
    @Test
    fun version6_roundTripsUnitAndPlateCount() = runTest {
        val (pulldown, _) = seedPlates()
        val exported = service.export(1L)
        val text = Backup.encode(exported)
        assertEquals(6, Json.parseToJsonElement(text).jsonObject.getValue("version").jsonPrimitive.int)

        db.sets().clear()
        db.sessions().clear()
        service.restore((Backup.decode(text) as BackupResult.Ok).file)

        assertEquals(exported, service.export(1L))
        assertEquals(PlateCount.HALF, db.exercises().all().single { it.id == pulldown }.plateCount)
        assertEquals(listOf(LoadUnit.KG, LoadUnit.KG, LoadUnit.PLATES, LoadUnit.PLATES), db.sets().all().map { it.unit })
        assertEquals(7.5, db.sets().all().last().weightKg, 0.0)
    }

    /** 1.17: a default is never written. A kg set and an off exercise carry neither key; a plate row ends with it. */
    @Test
    fun aKgSetAndAnOffExercise_exportWithNoNewKey() = runTest {
        val (pulldown, _) = seedPlates()
        val root = Json.parseToJsonElement(Backup.encode(service.export(1L))).jsonObject
        val setKeys = listOf("id", "sessionId", "exerciseId", "weightKg", "reps", "rpe", "failed", "missPoint", "isTopSet")
        val exerciseKeys = listOf("id", "name", "category", "parentLift", "muscles", "isBuiltIn", "archived")
        for (e in root.getValue("exercises").jsonArray.map { it.jsonObject }) {
            if (e.getValue("id").jsonPrimitive.int.toLong() == pulldown) {
                assertEquals(exerciseKeys + "plateCount", e.keys.toList())
                assertEquals("HALF", e.getValue("plateCount").jsonPrimitive.content)
            } else {
                assertEquals(exerciseKeys, e.keys.toList())
            }
        }
        val sets = root.getValue("sets").jsonArray.map { it.jsonObject }
        assertEquals(4, sets.size)
        for (s in sets.take(2)) assertEquals(setKeys, s.keys.toList())
        for (s in sets.drop(2)) {
            assertEquals(setKeys + "unit", s.keys.toList())
            assertEquals("PLATES", s.getValue("unit").jsonPrimitive.content)
        }
        assertTrue(Backup.encode(service.export(1L)).contains(""""isTopSet":true,"unit":"PLATES"}"""))
        assertTrue(Backup.encode(service.export(1L)).contains(""""archived":false,"plateCount":"HALF"}"""))
    }

    /** 1.17: every frozen fixture, both apps', restores with every set in kg and every exercise off, and exports with no new key. */
    @Test
    fun versions1to5_restoreWithEverySetKgAndEveryExerciseOff() = runTest {
        val dir = java.io.File("../web/src/backup/__fixtures__")
        for (v in 1..5) for (app in listOf("android", "web")) {
            val file = java.io.File(dir, "$app-v$v.json")
            val decoded = Backup.decode(file.readText())
            assertTrue("${file.name}: $decoded", decoded is BackupResult.Ok)
            service.restore((decoded as BackupResult.Ok).file)
            assertTrue(file.name, db.sets().all().isNotEmpty())
            assertTrue(file.name, db.sets().all().all { it.unit == LoadUnit.KG })
            assertTrue(file.name, db.exercises().all().all { it.plateCount == PlateCount.OFF })
            val text = Backup.encode(service.export(1L))
            assertFalse(file.name, text.contains("\"unit\""))
            assertFalse(file.name, text.contains("\"plateCount\""))
        }
    }

    /** 1.17: an app that reads only version 5 (1.16) turns a version 6 file away as too new. */
    @Test
    fun aVersion6File_isTooNewForAVersion5Reader() = runTest {
        seedPlates()
        assertEquals(BackupResult.TooNew(6), Backup.decode(Backup.encode(service.export(1L)), supportedVersion = 5))
    }

    @Test
    fun anUnknownUnitOrSettingName_isCorrupt() = runTest {
        seedPlates()
        val file = service.export(1L)
        for (bad in listOf(
            file.copy(sets = file.sets.map { it.copy(unit = "LB") }),
            file.copy(sets = file.sets.map { it.copy(unit = "plates") }),
            file.copy(exercises = file.exercises.map { it.copy(plateCount = "QUARTER") }),
        )) {
            assertTrue(bad.toString(), Backup.decode(Backup.encode(bad)) is BackupResult.Corrupt)
        }
    }

    /** Review Focus 4: a plate set numbered under 1 is a damaged file, and restoring nothing changes nothing. */
    @Test
    fun aPlateSetBelowPlateOne_isCorrupt() = runTest {
        seedPlates()
        val file = service.export(1L)
        val plate = file.sets.last()
        for (weight in listOf(0.0, 0.5, -1.0)) {
            val bad = file.copy(sets = file.sets.dropLast(1) + plate.copy(weightKg = weight))
            assertEquals(BackupResult.Corrupt("A plate set below plate 1"), Backup.decode(Backup.encode(bad)))
        }
        // Plate 1 itself, and a kg set at 0 kg (bodyweight), are fine.
        assertTrue(Backup.decode(Backup.encode(file.copy(sets = file.sets.dropLast(1) + plate.copy(weightKg = 1.0)))) is BackupResult.Ok)
        val kg = file.sets.first()
        assertTrue(Backup.decode(Backup.encode(file.copy(sets = listOf(kg.copy(weightKg = 0.0)) + file.sets.drop(1)))) is BackupResult.Ok)
    }

    /** 1.17.1: a plate set over plate 50 is a damaged file. */
    @Test
    fun aPlateSetAbovePlate50_isCorrupt() = runTest {
        seedPlates()
        val file = service.export(1L)
        val plate = file.sets.last()
        for (weight in listOf(50.5, 51.0, 80.0)) {
            val bad = file.copy(sets = file.sets.dropLast(1) + plate.copy(weightKg = weight))
            assertEquals(BackupResult.Corrupt("A plate set above plate 50"), Backup.decode(Backup.encode(bad)))
        }
        // Plate 50 itself, and a kg set at 80 kg, are fine.
        assertTrue(Backup.decode(Backup.encode(file.copy(sets = file.sets.dropLast(1) + plate.copy(weightKg = 50.0)))) is BackupResult.Ok)
        val kg = file.sets.first()
        assertTrue(Backup.decode(Backup.encode(file.copy(sets = listOf(kg.copy(weightKg = 80.0)) + file.sets.drop(1)))) is BackupResult.Ok)
    }

    /** 1.17.1: a plate set off the half-plate grid (7.3 would print as "plate 7½") is a damaged file, and so is 0.3. */
    @Test
    fun aPlateSetOffTheHalfPlateGrid_isCorrupt() = runTest {
        seedPlates()
        val file = service.export(1L)
        val plate = file.sets.last()
        for (weight in listOf(7.3, 7.25, 0.3)) {
            val bad = file.copy(sets = file.sets.dropLast(1) + plate.copy(weightKg = weight))
            assertEquals(BackupResult.Corrupt("A plate set that is not a whole or half plate"), Backup.decode(Backup.encode(bad)))
        }
        // A kg set at 7.3 kg is fine.
        val kg = file.sets.first()
        assertTrue(Backup.decode(Backup.encode(file.copy(sets = listOf(kg.copy(weightKg = 7.3)) + file.sets.drop(1)))) is BackupResult.Ok)
    }

    /** Every table and the settings, as they stand. */
    private suspend fun everything(): List<Any> = listOf(
        db.exercises().all(), db.sessions().all(), db.sets().all(), db.plan().all(), db.advice().all(),
        db.skips().all(), db.baselines().all(), db.checkIns().all(), settings.settings.first(),
    )

    /** What Settings does with a chosen file: restore it only when the check passes it. */
    private suspend fun attemptRestore(file: BackupFile) {
        when (val check = com.powertracker.ui.plan.checkBackupText(Backup.encode(file))) {
            is com.powertracker.ui.plan.RestoreCheck.Ready -> service.restore(check.file)
            is com.powertracker.ui.plan.RestoreCheck.Rejected -> Unit
        }
    }

    /** 1.17.1: a file refused for its plate numbers changes nothing: every table and the settings are as they were. */
    @Test
    fun aRefusedBackup_changesNothing() = runTest {
        seedBothSides()
        val (pulldown, _) = seedPlates()
        val training = TrainingRepository(db)
        training.saveCheckIn(com.powertracker.domain.model.CheckIn(LocalDate(2026, 11, 3), setOf(MuscleGroup.CHEST), setOf(MuscleGroup.LATS)))
        training.setBaseline(com.powertracker.domain.model.Baseline(pulldown, 80.0, LocalDate(2026, 11, 1)))
        db.skips().insert(com.powertracker.data.db.SkipEntity(date = LocalDate(2026, 11, 4), mode = TrainingMode.BODYBUILDING, exerciseId = pulldown, planItemId = null, reason = null))
        val before = everything()
        assertTrue("every table holds rows", before.take(8).all { (it as List<*>).isNotEmpty() })

        // A file that, were it restored, would change every table and the settings.
        val file = service.export(1L)
        val plate = file.sets.last()
        fun withPlate(weight: Double) = file.copy(
            settings = file.settings.copy(defaultRpe = 9.5),
            sets = file.sets.dropLast(2) + plate.copy(weightKg = weight),
            planItems = file.planItems.drop(1), advice = file.advice.drop(1), skips = emptyList(), baselines = emptyList(), checkIns = emptyList(),
        )
        for (weight in listOf(80.0, 7.3, 0.5)) {
            attemptRestore(withPlate(weight))
            assertEquals("plate $weight: refused, nothing changed", before, everything())
        }
        // The same file with a storable plate is restored, so the comparison would have seen a change.
        attemptRestore(withPlate(7.5))
        assertNotEquals(before, everything())
    }

    /** 1.17.1: with several bad plate sets, the first in file order names the fault. */
    @Test
    fun theFirstBadPlateSetInFileOrder_namesTheFault() = runTest {
        seedPlates()
        val file = service.export(1L)
        val plates = file.sets.withIndex().filter { it.value.unit == LoadUnit.PLATES.name }.map { it.index }
        assertTrue("at least two plate sets", plates.size >= 2)
        val (first, later) = plates.first() to plates.last()
        fun with(a: Double, b: Double) = file.copy(sets = file.sets.mapIndexed { i, s ->
            when (i) { first -> s.copy(weightKg = a); later -> s.copy(weightKg = b); else -> s }
        })
        assertEquals(BackupResult.Corrupt("A plate set that is not a whole or half plate"), Backup.decode(Backup.encode(with(7.3, 80.0))))
        assertEquals(BackupResult.Corrupt("A plate set above plate 50"), Backup.decode(Backup.encode(with(80.0, 7.3))))
        assertEquals(BackupResult.Corrupt("A plate set below plate 1"), Backup.decode(Backup.encode(with(0.5, 7.3))))
    }

    /** 1.17.1: the check is the half-plate grid and plate 1 to 50, never the exercise's setting now. */
    @Test
    fun aHalfPlateOnAWholePlateOrKgExercise_stillRestores() = runTest {
        val (pulldown, _) = seedPlates()
        val file = service.export(1L)
        assertEquals(7.5, file.sets.last().weightKg, 0.0)
        for (setting in listOf(PlateCount.WHOLE, PlateCount.OFF)) {
            val edited = file.copy(exercises = file.exercises.map { if (it.id == pulldown) it.copy(plateCount = setting.name) else it })
            val decoded = Backup.decode(Backup.encode(edited))
            assertTrue("$setting: $decoded", decoded is BackupResult.Ok)
            db.sets().clear()
            db.sessions().clear()
            service.restore((decoded as BackupResult.Ok).file)
            assertEquals(setting, db.exercises().all().single { it.id == pulldown }.plateCount)
            assertEquals(7.5 to LoadUnit.PLATES, db.sets().all().last().let { it.weightKg to it.unit })
        }
    }

    /** 1.17: a plate target's payload is an opaque string: it comes back byte for byte. */
    @Test
    fun aPlateTargetsPayload_comesBackByteForByte() = runTest {
        val (pulldown, _) = seedPlates()
        val payload = Json.encodeToString(
            com.powertracker.domain.model.AdvicePayload.serializer(),
            com.powertracker.domain.model.AdvicePayload.Target(pulldown, 7.5, 8, 8.0, repsHigh = 12, unit = LoadUnit.PLATES),
        )
        assertTrue(payload, payload.endsWith(""""repsHigh":12,"unit":"PLATES"}"""))
        db.advice().insert(
            listOf(
                AdviceEntity(type = AdviceType.TARGET, lift = null, createdAt = LocalDate(2026, 11, 18), reason = "Coach suggests plate 7½ × 8–12 @ 8.",
                    severity = 0, payload = payload, status = AdviceStatus.OPEN, mode = TrainingMode.BODYBUILDING),
            ),
        )
        val text = Backup.encode(service.export(1L))
        db.advice().clear()
        service.restore((Backup.decode(text) as BackupResult.Ok).file)

        assertEquals(payload, db.advice().all().single().payload)
        val target = Json.decodeFromString(com.powertracker.domain.model.AdvicePayload.serializer(), payload) as com.powertracker.domain.model.AdvicePayload.Target
        assertEquals(LoadUnit.PLATES, target.unit)
    }

    private fun modes(root: JsonObject, list: String): List<String> =
        root.getValue(list).jsonArray.map { it.jsonObject.getValue("mode").jsonPrimitive.content }

    /** [text] as the strict press release wrote it: version 2, and none of the version 3 fields. */
    private fun asVersion2(text: String): String {
        val root = Json.parseToJsonElement(text).jsonObject
        fun strip(list: String, vararg keys: String) =
            JsonArray(root.getValue(list).jsonArray.map { e -> JsonObject(e.jsonObject - keys.toSet()) })
        val settings = JsonObject(root.getValue("settings").jsonObject - setOf("trainingMode", "bodybuildingTrainingDays", "powerliftingLeftOn", "bodybuildingLeftOn"))
        return JsonObject(
            root + mapOf(
                "version" to JsonPrimitive(2), "settings" to settings, "sessions" to strip("sessions", "mode"),
                "planItems" to strip("planItems", "mode", "repsHigh"), "advice" to strip("advice", "mode"),
            ),
        ).toString()
    }

    /** [text] as the previous app wrote it: version 1, and no setting it did not have. */
    private fun asVersion1(text: String): String {
        val root = Json.parseToJsonElement(text).jsonObject
        val settings = JsonObject(root.getValue("settings").jsonObject - "showStrictPress")
        return JsonObject(root + mapOf("version" to JsonPrimitive(1), "settings" to settings)).toString()
    }
}
