package com.powertracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.db.AdviceEntity
import com.powertracker.data.db.AppDatabase
import com.powertracker.data.db.BaselineEntity
import com.powertracker.data.db.ExerciseEntity
import com.powertracker.data.db.PlanItemEntity
import com.powertracker.data.db.SessionEntity
import com.powertracker.data.db.SetEntity
import com.powertracker.domain.model.AccessoryPrescription
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The web app restores this committed Android backup in its own tests, so the file must track what Android
 * really exports. Regenerate it with `./gradlew :app:testDebugUnitTest -Pfixtures.update=true`.
 *
 * Comparison is semantic (parsed JSON, `exportedAt` ignored): Kotlin/JS prints whole doubles without ".0",
 * so byte equality would be wrong.
 */
@RunWith(RobolectricTestRunner::class)
class BackupFixtureTest {
    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var service: BackupService

    // Gradle runs app tests with the module directory as the working directory.
    private val fixtureDir = File("../web/src/backup/__fixtures__")
    private val androidFixture = File(fixtureDir, "android-v1.json")
    private val webFixture = File(fixtureDir, "web-v1.json")

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

    private suspend fun populate() {
        settings.update {
            AppSettings(
                changePlates = true,
                defaultRpe = 7.5,
                trainingDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
                appLock = true,
                restMainSec = 210,
                restAccessorySec = 75,
                restAlert = RestAlert.SOUND_AND_VIBRATE,
            )
        }
        val exercises = db.exercises().all().associateBy { it.name }
        val squat = exercises.getValue("Squat").id
        val bench = exercises.getValue("Bench").id
        val pause = exercises.getValue("Pause squat").id
        val custom = db.exercises().insert(
            ExerciseEntity(name = "Zercher squat", category = Category.VARIATION, parentLift = MainLift.SQUAT, muscles = listOf("Quads", "Core"), isBuiltIn = false),
        )

        val s1 = db.sessions().insert(SessionEntity(date = LocalDate(2026, 10, 27), bodyweightKg = 84.5, readiness = 4))
        db.sets().insert(SetEntity(sessionId = s1, exerciseId = squat, weightKg = 140.0, reps = 5, rpe = 8.0, failed = false, missPoint = null, isTopSet = true))
        db.sets().insert(SetEntity(sessionId = s1, exerciseId = squat, weightKg = 147.5, reps = 3, rpe = null, failed = true, missPoint = MissPoint.MID, isTopSet = false))
        db.sets().insert(SetEntity(sessionId = s1, exerciseId = custom, weightKg = 100.0, reps = 6, rpe = 7.5, failed = false, missPoint = null, isTopSet = false))
        val s2 = db.sessions().insert(SessionEntity(date = LocalDate(2026, 10, 29), bodyweightKg = null, readiness = null))
        db.sets().insert(SetEntity(sessionId = s2, exerciseId = bench, weightKg = 102.5, reps = 4, rpe = 9.0, failed = false, missPoint = null, isTopSet = true))
        db.sets().insert(SetEntity(sessionId = s2, exerciseId = bench, weightKg = 105.0, reps = 1, rpe = 10.0, failed = true, missPoint = MissPoint.LOCKOUT, isTopSet = false))

        db.plan().upsert(PlanItemEntity(weekday = DayOfWeek.TUESDAY, exerciseId = squat, sets = 4, reps = 5, targetRpe = 8.0, sortOrder = 0))
        db.plan().upsert(PlanItemEntity(weekday = DayOfWeek.TUESDAY, exerciseId = pause, sets = 3, reps = 4, targetRpe = 7.5, sortOrder = 1))
        db.plan().upsert(PlanItemEntity(weekday = DayOfWeek.SATURDAY, exerciseId = bench, sets = 5, reps = 3, targetRpe = 8.5, sortOrder = 0))

        val created = LocalDate(2026, 10, 30)
        fun advice(type: AdviceType, lift: MainLift?, reason: String, severity: Int, payload: AdvicePayload, status: AdviceStatus) =
            AdviceEntity(type = type, lift = lift, createdAt = created, reason = reason, severity = severity,
                payload = Json.encodeToString(AdvicePayload.serializer(), payload), status = status)
        db.advice().insert(
            listOf(
                advice(AdviceType.TARGET, MainLift.SQUAT, "Last top set was clean at RPE 8", 1, AdvicePayload.Target(squat, 142.5, 5, 8.0, sets = 4), AdviceStatus.OPEN),
                advice(AdviceType.STALL, MainLift.BENCH, "No bench PR in four weeks", 2,
                    AdvicePayload.Accessories(listOf(AccessoryPrescription("Pause bench", 3, 5, 8))), AdviceStatus.APPLIED),
                advice(AdviceType.DELOAD, null, "Fatigue is building", 3, AdvicePayload.Deload(), AdviceStatus.DISMISSED),
                advice(AdviceType.WEAK_POINT, MainLift.SQUAT, "Misses cluster mid-lift", 2, AdvicePayload.WeakPoint(MainLift.SQUAT, MissPoint.MID), AdviceStatus.OPEN),
                advice(AdviceType.RE_ENTRY, null, "Back after a break", 1, AdvicePayload.ReEntry(), AdviceStatus.OPEN),
            ),
        )
        db.baselines().upsert(BaselineEntity(squat, 172.5, LocalDate(2026, 9, 1)))
    }

    private fun tree(text: String): JsonElement = Json.parseToJsonElement(text)
    private fun withoutExportedAt(e: JsonElement): JsonElement =
        normalize(JsonObject((e as JsonObject).filterKeys { it != "exportedAt" }))

    /** Numbers compare by value ("100" == "100.0"): JSON literals otherwise compare by their text. */
    private fun normalize(e: JsonElement): JsonElement = when (e) {
        is JsonObject -> JsonObject(e.mapValues { normalize(it.value) })
        is JsonArray -> JsonArray(e.map(::normalize))
        is JsonPrimitive -> if (e.isString) e else e.doubleOrNull?.let(::JsonPrimitive) ?: e
    }

    private val pretty = Json { prettyPrint = true; encodeDefaults = true }

    private fun exportText(): String = runBlocking {
        populate()
        pretty.encodeToString(BackupFile.serializer(), service.export(EXPORTED_AT)) + "\n"
    }

    @Test
    fun androidFixture_isUpToDate() {
        val actual = exportText()
        if (System.getProperty("fixtures.update") == "true") {
            fixtureDir.mkdirs()
            androidFixture.writeText(actual)
        }
        assertTrue("Missing ${androidFixture.path}; run with -Pfixtures.update=true", androidFixture.exists())
        assertEquals(withoutExportedAt(tree(androidFixture.readText())), withoutExportedAt(tree(actual)))
        // The fixture is a real backup: Android's own decoder accepts it.
        assertTrue(Backup.decode(androidFixture.readText()) is BackupResult.Ok)
    }

    @Test
    fun webFixture_restoresOnAndroid() = runTest {
        assertTrue("Missing ${webFixture.path}; generate it with UPDATE_FIXTURES=1 npm test in web/", webFixture.exists())
        val result = Backup.decode(webFixture.readText())
        assertTrue(result.toString(), result is BackupResult.Ok)
        service.restore((result as BackupResult.Ok).file)

        // Restored data matches the fixture semantically (exportedAt ignored), webRestAlert included.
        val expected = withoutExportedAt(tree(webFixture.readText()))
        val actual = withoutExportedAt(tree(pretty.encodeToString(BackupFile.serializer(), service.export(EXPORTED_AT))))
        assertEquals(expected, actual)
        val settingsJson = (actual as JsonObject).getValue("settings") as JsonObject
        assertEquals("SILENT", (settingsJson.getValue("webRestAlert") as JsonPrimitive).content)
        assertEquals("VIBRATE", (settingsJson.getValue("restAlert") as JsonPrimitive).content)
    }

    private companion object {
        const val EXPORTED_AT = 1_793_000_000_000L
    }
}
