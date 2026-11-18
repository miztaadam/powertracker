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
import com.powertracker.domain.coach.Coaching
import com.powertracker.domain.coach.PhysiqueCoach
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.AccessoryPrescription
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.CheckIn
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.NewExercise
import com.powertracker.domain.model.SetAddition
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingMode
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The web app restores the committed Android backup in its own tests, so the file must track what Android really
 * exports: since version 5 (1.13) that is android-v5.json. Regenerate it with
 * `./gradlew :app:testDebugUnitTest -Pfixtures.update=true`. The version 1 to 4 fixtures are frozen: they are what the
 * previous apps wrote, and must keep restoring (versions 1 and 2 with everything on the powerlifting side, version 3
 * with no check-ins, version 4 with no skips).
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
    private val androidFixture = File(fixtureDir, "android-v5.json")
    private val androidV4Fixture = File(fixtureDir, "android-v4.json")
    private val androidV3Fixture = File(fixtureDir, "android-v3.json")
    private val androidV2Fixture = File(fixtureDir, "android-v2.json")
    private val androidV1Fixture = File(fixtureDir, "android-v1.json")
    private val webFixture = File(fixtureDir, "web-v1.json")
    private val webV2Fixture = File(fixtureDir, "web-v2.json")
    private val webV3Fixture = File(fixtureDir, "web-v3.json")
    private val webV4Fixture = File(fixtureDir, "web-v4.json")

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
                showStrictPress = true,
                // Version 3: on the bodybuilding side, with its own days, both sides left once.
                trainingMode = TrainingMode.BODYBUILDING,
                bodybuildingTrainingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
                powerliftingLeftOn = LocalDate(2026, 10, 31),
                bodybuildingLeftOn = LocalDate(2026, 10, 24),
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
        // Version 2: a strict press row from the catalogue, logged with an "off the shoulders" miss.
        val press = CatalogueResolver(db).resolve("Strict press")
        db.sets().insert(SetEntity(sessionId = s2, exerciseId = press, weightKg = 60.0, reps = 5, rpe = 8.5, failed = false, missPoint = null, isTopSet = true))
        db.sets().insert(SetEntity(sessionId = s2, exerciseId = press, weightKg = 65.0, reps = 2, rpe = 10.0, failed = true, missPoint = MissPoint.FLOOR, isTopSet = false))

        db.plan().upsert(PlanItemEntity(weekday = DayOfWeek.TUESDAY, exerciseId = squat, sets = 4, reps = 5, targetRpe = 8.0, sortOrder = 0))
        db.plan().upsert(PlanItemEntity(weekday = DayOfWeek.TUESDAY, exerciseId = pause, sets = 3, reps = 4, targetRpe = 7.5, sortOrder = 1))
        db.plan().upsert(PlanItemEntity(weekday = DayOfWeek.SATURDAY, exerciseId = bench, sets = 5, reps = 3, targetRpe = 8.5, sortOrder = 0))

        // Version 3: the bodybuilding side's plan, a session logged on it, and its advice.
        val curl = exercises.getValue("Curl").id
        db.plan().upsert(PlanItemEntity(weekday = DayOfWeek.MONDAY, exerciseId = curl, sets = 3, reps = 8, targetRpe = 8.0, sortOrder = 0,
            mode = TrainingMode.BODYBUILDING, repsHigh = 12))
        db.plan().upsert(PlanItemEntity(weekday = DayOfWeek.MONDAY, exerciseId = squat, sets = 4, reps = 10, targetRpe = 8.0, sortOrder = 1,
            mode = TrainingMode.BODYBUILDING, repsHigh = 15))
        val s3 = db.sessions().insert(SessionEntity(date = LocalDate(2026, 11, 2), bodyweightKg = 84.0, readiness = 3, mode = TrainingMode.BODYBUILDING))
        db.sets().insert(SetEntity(sessionId = s3, exerciseId = curl, weightKg = 16.0, reps = 11, rpe = 8.0, failed = false, missPoint = null, isTopSet = false))
        db.sets().insert(SetEntity(sessionId = s3, exerciseId = curl, weightKg = 16.0, reps = 9, rpe = 9.0, failed = false, missPoint = null, isTopSet = false))

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
                advice(AdviceType.TARGET, null, "Last time: 16 × 11, 9. Beat it. Coach suggests 16 × 8–12 @ 8.", 0,
                    AdvicePayload.Target(curl, 16.0, 8, 8.0, repsHigh = 12), AdviceStatus.OPEN).copy(mode = TrainingMode.BODYBUILDING),
            ),
        )
        db.baselines().upsert(BaselineEntity(squat, 172.5, LocalDate(2026, 9, 1)))

        // Version 4: two check-ins, and the lagging-muscle coach's two kinds of advice, true to the plan: a lateral raise
        // at 4 sets after its applied set addition, and no chest work yet for the open Pec deck suggestion.
        val training = TrainingRepository(db)
        val raise = CatalogueResolver(db).resolve("Lateral raise")
        db.plan().upsert(PlanItemEntity(weekday = DayOfWeek.MONDAY, exerciseId = raise, sets = 4, reps = 8, targetRpe = 8.0, sortOrder = 2,
            mode = TrainingMode.BODYBUILDING, repsHigh = 12))
        val bbPlan = training.plan(TrainingMode.BODYBUILDING).first()
        val known = training.exercises().first().associateBy { it.id }
        // Before the applied addition the raise had 3 sets.
        val sideBefore = PhysiqueCoach.weeklySets(bbPlan, known, MuscleGroup.SIDE_DELTS) - 1
        check(sideBefore == 3 && PhysiqueCoach.weeklySets(bbPlan, known, MuscleGroup.CHEST) == 0)
        training.saveCheckIn(CheckIn(LocalDate(2026, 10, 5), emptySet(), setOf(MuscleGroup.BICEPS)))
        training.saveCheckIn(CheckIn(LocalDate(2026, 11, 2), setOf(MuscleGroup.CHEST, MuscleGroup.SIDE_DELTS), setOf(MuscleGroup.QUADS)))
        db.advice().insert(
            listOf(
                advice(AdviceType.MORE_WORK, null, "You marked chest as lagging. Add Pec deck on Monday, 3 × 8–12 @ 8. That takes it from 0 to 3 sets a week.", 0,
                    AdvicePayload.MoreWork(MuscleGroup.CHEST, emptyList(), NewExercise(DayOfWeek.MONDAY, "Pec deck", 3, 8, 12, 8.0), 0, 3),
                    AdviceStatus.OPEN).copy(mode = TrainingMode.BODYBUILDING, createdAt = LocalDate(2026, 11, 2)),
                // Applied: the lateral raise went from 3 to 4 sets, the only side-delt work in the plan.
                advice(AdviceType.MORE_WORK, null, "You marked side delts as lagging. Add a set to Lateral raise on Monday. That takes it from 3 to 4 sets a week.", 0,
                    AdvicePayload.MoreWork(MuscleGroup.SIDE_DELTS, listOf(SetAddition(DayOfWeek.MONDAY, raise, "Lateral raise", 3, 4)), null, sideBefore, sideBefore + 1),
                    AdviceStatus.APPLIED).copy(mode = TrainingMode.BODYBUILDING, createdAt = LocalDate(2026, 11, 2)),
                advice(AdviceType.SWAP, null, "Curl hasn't moved in 4 weeks. Same weight and no more reps since 5 Oct.", 0,
                    AdvicePayload.Swap(curl, "Curl", MuscleGroup.BICEPS, LocalDate(2026, 10, 5), listOf("Cable curl", "EZ-bar curl", "Incline DB curl")),
                    AdviceStatus.OPEN).copy(mode = TrainingMode.BODYBUILDING, createdAt = LocalDate(2026, 11, 2)),
            ),
        )

        // Version 5: skips on both sides. Today's pain skip of the lateral raise carries the card the coach really gives
        // for it; a skip with no reason, and last week's busy skip of bench, whose plan item has since gone.
        val day = LocalDate(2026, 11, 4)
        val items = db.plan().all()
        val raiseItem = items.single { it.exerciseId == raise }.id
        val squatItem = items.single { it.exerciseId == squat && it.mode == TrainingMode.POWERLIFTING }.id
        val history = training.history().first()
        val plans = training.plan(TrainingMode.BODYBUILDING).first() + training.plan(TrainingMode.POWERLIFTING).first()
        val prior = AdviceRepository(db).all()
        training.skip(Skip(0, day, TrainingMode.BODYBUILDING, raise, raiseItem, SkipReason.PAIN)) { stored, all ->
            Coaching.afterSkip(stored, all, history, plans, day, prior).also { check(it.size == 1) }
        }
        training.skip(Skip(0, day, TrainingMode.POWERLIFTING, squat, squatItem, null)) { _, _ -> emptyList() }
        training.skip(Skip(0, LocalDate(2026, 10, 28), TrainingMode.POWERLIFTING, bench, null, SkipReason.BUSY)) { _, _ -> emptyList() }
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

    /** The committed fixture holds both sides, so the web app's restore of it covers every version 3 field. */
    @Test
    fun androidFixture_holdsBothSides() {
        val root = tree(androidFixture.readText()) as JsonObject
        assertEquals(JsonPrimitive(5), root.getValue("version"))
        for (list in listOf("planItems", "sessions", "advice")) {
            val modes = (root.getValue(list) as JsonArray).map { ((it as JsonObject).getValue("mode") as JsonPrimitive).content }.toSet()
            assertEquals(list, setOf("POWERLIFTING", "BODYBUILDING"), modes)
        }
    }

    /** Stage 3: the committed fixture holds check-ins and both new kinds of advice, so the web app's restore covers them. */
    @Test
    fun androidFixture_holdsCheckInsAndTheNewAdvice() {
        val root = tree(androidFixture.readText()) as JsonObject
        assertEquals(2, (root.getValue("checkIns") as JsonArray).size)
        val types = (root.getValue("advice") as JsonArray).map { ((it as JsonObject).getValue("type") as JsonPrimitive).content }
        assertTrue(types.containsAll(listOf("MORE_WORK", "SWAP")))
    }

    /** 1.13: the committed fixture holds skips on both sides, every reason form and a skip card, so the web app's restore covers them. */
    @Test
    fun androidFixture_holdsSkipsAndASkipCard() {
        val root = tree(androidFixture.readText()) as JsonObject
        val skips = (root.getValue("skips") as JsonArray).map { it as JsonObject }
        assertEquals(3, skips.size)
        assertEquals(setOf("POWERLIFTING", "BODYBUILDING"), skips.map { (it.getValue("mode") as JsonPrimitive).content }.toSet())
        assertEquals(setOf(JsonPrimitive("PAIN"), JsonNull, JsonPrimitive("BUSY")), skips.map { it.getValue("reason") }.toSet())
        assertTrue(skips.any { it.getValue("planItemId") == JsonNull })
        val card = (root.getValue("advice") as JsonArray).map { it as JsonObject }.single { (it.getValue("type") as JsonPrimitive).content == "SKIP" }
        val payload = Json.decodeFromString(AdvicePayload.serializer(), (card.getValue("payload") as JsonPrimitive).content) as AdvicePayload.Skipped
        assertEquals(skips.first { it.getValue("reason") == JsonPrimitive("PAIN") }.getValue("id"), JsonPrimitive(payload.skipId))
    }

    /** 1.13: the 1.12 Android backup (version 4) restores exactly, with no skips. */
    @Test
    fun androidV4Fixture_restoresWithNoSkips() = runTest {
        TrainingRepository(db).skip(Skip(0, LocalDate(2026, 11, 4), TrainingMode.POWERLIFTING, 1, null, null)) { _, _ -> emptyList() }

        val actual = restoreAndExport(androidV4Fixture)

        assertEquals(asUpgradedV4(tree(androidV4Fixture.readText())), actual)
        assertTrue(db.skips().all().isEmpty())
        assertEquals(2, db.checkIns().all().size)
    }

    /** The version 3 Android backup restores exactly, with no check-ins. */
    @Test
    fun androidV3Fixture_restoresWithNoCheckIns() = runTest {
        val actual = restoreAndExport(androidV3Fixture)
        assertEquals(asUpgradedV3(tree(androidV3Fixture.readText())), actual)
        assertTrue(db.checkIns().all().isEmpty())
        assertEquals(TrainingMode.BODYBUILDING, settings.settings.first().trainingMode)
    }

    /** Stage 3: the web app's version 4 backup restores exactly, every check-in and every advice with it. */
    @Test
    fun webV4Fixture_restoresOnAndroid() = runTest {
        assertTrue("Missing ${webV4Fixture.path}; the web app writes it", webV4Fixture.exists())
        val fixture = tree(webV4Fixture.readText()) as JsonObject
        assertEquals(JsonPrimitive(4), fixture.getValue("version"))
        val actual = restoreAndExport(webV4Fixture)
        assertEquals(asUpgradedV4(fixture), actual)
        val checkIns = TrainingRepository(db).checkIns().first()
        assertEquals((fixture.getValue("checkIns") as JsonArray).size, checkIns.size)
        assertTrue(checkIns.any { it.lagging.isNotEmpty() || it.growingWell.isNotEmpty() })
        val stored = db.advice().all()
        assertEquals((fixture.getValue("advice") as JsonArray).size, stored.size)
        // Each new advice's payload is one this app reads.
        val payloads = stored.filter { it.type == AdviceType.MORE_WORK || it.type == AdviceType.SWAP }
            .map { Json.decodeFromString(AdvicePayload.serializer(), it.payload) }
        assertTrue(payloads.any { it is AdvicePayload.MoreWork })
        assertTrue(payloads.any { it is AdvicePayload.Swap })
    }

    /** An Android version 1 backup restores, and exports again as the current version with the strict press switch off. */
    @Test
    fun androidV1Fixture_restoresOnAndroid() = runTest {
        val actual = restoreAndExport(androidV1Fixture)
        assertEquals(asUpgradedV1(tree(androidV1Fixture.readText())), actual)
    }

    /** Bodybuilding mode spec §6: the strict press release's backup restores with everything on the powerlifting side. */
    @Test
    fun androidV2Fixture_restoresAsAllPowerlifting() = runTest {
        val actual = restoreAndExport(androidV2Fixture)
        assertEquals(asUpgradedV2(tree(androidV2Fixture.readText())), actual)
        assertTrue(db.plan().all().all { it.mode == TrainingMode.POWERLIFTING && it.repsHigh == null })
        assertTrue(db.sessions().all().all { it.mode == TrainingMode.POWERLIFTING })
        assertTrue(db.advice().all().all { it.mode == TrainingMode.POWERLIFTING })
        assertEquals(TrainingMode.POWERLIFTING, settings.settings.first().trainingMode)
    }

    @Test
    fun webFixture_restoresOnAndroid() = runTest {
        assertTrue("Missing ${webFixture.path}; generate it with UPDATE_FIXTURES=1 npm test in web/", webFixture.exists())
        // Restored data matches the fixture semantically (exportedAt ignored), webRestAlert included; only the
        // version and the setting version 1 lacked differ. Neither v1 fixture has a strict press row to convert.
        val actual = restoreAndExport(webFixture)
        assertEquals(asUpgradedV1(tree(webFixture.readText())), actual)
        val settingsJson = (actual as JsonObject).getValue("settings") as JsonObject
        assertEquals("SILENT", (settingsJson.getValue("webRestAlert") as JsonPrimitive).content)
        assertEquals("VIBRATE", (settingsJson.getValue("restAlert") as JsonPrimitive).content)
    }

    /** The web app's version-2 backup restores exactly: its strict press row, sets and switch included. */
    @Test
    fun webV2Fixture_restoresOnAndroid() = runTest {
        assertTrue("Missing ${webV2Fixture.path}", webV2Fixture.exists())
        val actual = restoreAndExport(webV2Fixture)
        assertEquals(asUpgradedV2(tree(webV2Fixture.readText())), actual)
        val settingsJson = (actual as JsonObject).getValue("settings") as JsonObject
        assertEquals(JsonPrimitive(true), settingsJson.getValue("showStrictPress"))
        assertTrue(settings.settings.first().showStrictPress)
        val press = db.exercises().all().single { it.category == Category.STRICT_PRESS }
        assertEquals("Strict press", press.name)
        assertTrue(db.sets().all().any { it.exerciseId == press.id && it.missPoint == MissPoint.FLOOR })
    }

    /** The web app's version 3 backup restores exactly: both sides' plans, sessions, advice and settings. */
    @Test
    fun webV3Fixture_restoresOnAndroid() = runTest {
        assertTrue("Missing ${webV3Fixture.path}", webV3Fixture.exists())
        val actual = restoreAndExport(webV3Fixture)
        assertEquals(asUpgradedV3(tree(webV3Fixture.readText())), actual)
        assertEquals(TrainingMode.BODYBUILDING, settings.settings.first().trainingMode)
        assertTrue(db.plan().all().any { it.mode == TrainingMode.BODYBUILDING && it.repsHigh != null })
        assertTrue(db.sessions().all().any { it.mode == TrainingMode.BODYBUILDING })
    }

    /**
     * Stage 2 (owner, option A): a settings key this app does not know is ignored, so a later app can add a setting
     * without older apps turning its backups away. "Vibrate on taps" is not in the backup: a restore leaves it on.
     */
    @Test
    fun androidV4Fixture_withSettingsThisAppDoesNotKnow_restores() = runTest {
        val fixture = tree(androidFixture.readText()) as JsonObject
        val settingsJson = fixture.getValue("settings") as JsonObject
        val later = mapOf("vibrateOnTaps" to JsonPrimitive(false), "somethingLater" to JsonObject(mapOf("a" to JsonPrimitive(1))))
        val withNew = JsonObject(fixture + ("settings" to JsonObject(settingsJson + later)))
        settings.update { it.copy(vibrateOnTaps = false) }

        val result = Backup.decode(withNew.toString())
        assertTrue(result.toString(), result is BackupResult.Ok)
        service.restore((result as BackupResult.Ok).file)

        assertEquals(withoutExportedAt(fixture), withoutExportedAt(tree(pretty.encodeToString(BackupFile.serializer(), service.export(EXPORTED_AT)))))
        assertTrue(settings.settings.first().vibrateOnTaps)
    }

    /** Only `settings` is lenient: an unknown key anywhere else, or a known setting with a bad value, is still corrupt. */
    @Test
    fun unknownKeys_outsideSettings_areStillCorrupt() {
        val fixture = tree(androidFixture.readText()) as JsonObject
        assertTrue(Backup.decode(JsonObject(fixture + ("somethingLater" to JsonPrimitive(1))).toString()) is BackupResult.Corrupt)
        val sets = fixture.getValue("sets") as JsonArray
        val firstSet = JsonObject(sets[0] as JsonObject + ("extra" to JsonPrimitive(1)))
        assertTrue(Backup.decode(JsonObject(fixture + ("sets" to JsonArray(listOf(firstSet) + sets.drop(1)))).toString()) is BackupResult.Corrupt)
        val settingsJson = fixture.getValue("settings") as JsonObject
        assertTrue(Backup.decode(JsonObject(fixture + ("settings" to JsonObject(settingsJson + ("restAlert" to JsonPrimitive("LOUD"))))).toString()) is BackupResult.Corrupt)
        assertTrue(Backup.decode(JsonObject(fixture + ("settings" to JsonPrimitive(1))).toString()) is BackupResult.Corrupt)
    }

    /** "Vibrate on taps" stays on this phone: the export never carries it, so 1.6 apps keep reading 1.7 backups. */
    @Test
    fun export_leavesVibrateOnTapsOut() = runTest {
        settings.update { it.copy(vibrateOnTaps = false) }
        assertFalse(Backup.encode(service.export(EXPORTED_AT)).contains("vibrateOnTaps"))
    }

    private suspend fun restoreAndExport(fixture: File): JsonElement {
        val result = Backup.decode(fixture.readText())
        assertTrue(result.toString(), result is BackupResult.Ok)
        service.restore((result as BackupResult.Ok).file)
        return withoutExportedAt(tree(pretty.encodeToString(BackupFile.serializer(), service.export(EXPORTED_AT))))
    }

    /** What a version-1 file with no strict press row exports as once restored: the switch off, then as [asUpgradedV2]. */
    private fun asUpgradedV1(v1: JsonElement): JsonElement {
        val root = v1 as JsonObject
        assertEquals(JsonPrimitive(1), root.getValue("version"))
        val settings = JsonObject(root.getValue("settings") as JsonObject + ("showStrictPress" to JsonPrimitive(false)))
        return withSidesAdded(JsonObject(root + ("settings" to settings)))
    }

    /** What a version-2 file exports as once restored: the current version, everything on the powerlifting side. */
    private fun asUpgradedV2(v2: JsonElement): JsonElement {
        assertEquals(JsonPrimitive(2), (v2 as JsonObject).getValue("version"))
        return withSidesAdded(v2)
    }

    /** What a version 3 file exports as once restored: the current version, with no check-ins. */
    private fun asUpgradedV3(v3: JsonElement): JsonElement {
        val root = v3 as JsonObject
        assertEquals(JsonPrimitive(3), root.getValue("version"))
        return withoutExportedAt(
            JsonObject(root + mapOf("version" to JsonPrimitive(Backup.VERSION), "checkIns" to JsonArray(emptyList()), "skips" to JsonArray(emptyList()))),
        )
    }

    /** What a version 4 file exports as once restored: the current version, with no skips. */
    private fun asUpgradedV4(v4: JsonElement): JsonElement {
        val root = v4 as JsonObject
        assertEquals(JsonPrimitive(4), root.getValue("version"))
        return withoutExportedAt(JsonObject(root + mapOf("version" to JsonPrimitive(Backup.VERSION), "skips" to JsonArray(emptyList()))))
    }

    private fun withSidesAdded(root: JsonObject): JsonElement {
        val powerlifting = JsonPrimitive("POWERLIFTING")
        fun stamp(list: String, vararg extra: Pair<String, JsonElement>) =
            JsonArray((root.getValue(list) as JsonArray).map { JsonObject(it as JsonObject + ("mode" to powerlifting) + extra) })
        val settings = JsonObject(
            root.getValue("settings") as JsonObject + mapOf(
                "trainingMode" to powerlifting,
                "bodybuildingTrainingDays" to JsonArray(listOf(1, 3, 5).map(::JsonPrimitive)),
                "powerliftingLeftOn" to JsonNull,
                "bodybuildingLeftOn" to JsonNull,
            ),
        )
        return withoutExportedAt(
            JsonObject(
                root + mapOf(
                    "version" to JsonPrimitive(Backup.VERSION), "settings" to settings, "sessions" to stamp("sessions"),
                    "planItems" to stamp("planItems", "repsHigh" to JsonNull), "advice" to stamp("advice"),
                    "checkIns" to JsonArray(emptyList()), "skips" to JsonArray(emptyList()),
                ),
            ),
        )
    }

    private companion object {
        const val EXPORTED_AT = 1_793_000_000_000L
    }
}
