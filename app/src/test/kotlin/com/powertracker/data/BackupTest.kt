package com.powertracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.db.AppDatabase
import com.powertracker.data.db.ExerciseEntity
import com.powertracker.data.db.SessionEntity
import com.powertracker.data.db.SetEntity
import com.powertracker.domain.library.Library
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
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
        assertEquals(2, Json.parseToJsonElement(text).jsonObject.getValue("version").jsonPrimitive.int)
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

        assertEquals(BackupResult.TooNew(2), Backup.decode(text, supportedVersion = 1))
        assertTrue(Backup.decode(text) is BackupResult.Ok)
    }

    /** [text] as the previous app wrote it: version 1, and no setting it did not have. */
    private fun asVersion1(text: String): String {
        val root = Json.parseToJsonElement(text).jsonObject
        val settings = JsonObject(root.getValue("settings").jsonObject - "showStrictPress")
        return JsonObject(root + mapOf("version" to JsonPrimitive(1), "settings" to settings)).toString()
    }
}
