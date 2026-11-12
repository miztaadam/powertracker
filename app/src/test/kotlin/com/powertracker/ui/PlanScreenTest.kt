package com.powertracker.ui

import android.content.Context
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.Backup
import com.powertracker.data.BackupService
import com.powertracker.data.RestAlert
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.PlanItem
import com.powertracker.ui.library.checkInfoButtonOpensGuide
import com.powertracker.ui.plan.PlanScreen
import com.powertracker.ui.plan.RestoreCheck
import com.powertracker.ui.plan.SettingsScreen
import com.powertracker.ui.plan.backupFileName
import com.powertracker.ui.plan.checkBackupText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class PlanScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
        training = TrainingRepository(db)
        settings = SettingsRepository(context)
        runBlocking { settings.update { com.powertracker.data.AppSettings() } }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun addPlanItem_showsUnderWeekday() {
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }

        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("add-MONDAY")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("add-MONDAY").performClick()
        compose.onNodeWithTag("exercise-search").performTextReplacement("Squat")
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("exercise-Squat")).fetchSemanticsNodes().isNotEmpty() }
        // Picking adds it straight away. Defaults for a compound lift are 3 sets x 5 reps @ RPE 8.
        compose.onNodeWithTag("exercise-Squat").performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithText("Squat  3 × 5 @ 8").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Squat  3 × 5 @ 8").assertExists()
    }

    @Test
    fun moreMenu_editOpensDialogWithItemValues_andDeleteRemovesRow() {
        runBlocking {
            val bench = training.exercises().first().first { it.name == "Bench" }
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, bench.id, sets = 4, reps = 6, targetRpe = 7.5, order = 0))
        }
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        val row = "Bench  4 × 6 @ 7.5"
        compose.waitUntil(5_000) { compose.onAllNodesWithText(row).fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithText("Edit Monday").assertExists()
        compose.onNode(hasText("Bench") and hasAnyAncestor(isDialog())).assertExists()
        compose.onNodeWithText("4").assertExists()
        compose.onNodeWithText("6").assertExists()
        compose.onNodeWithText("7.5").assertExists()
        compose.onNodeWithText("Cancel").performClick()

        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(row).fetchSemanticsNodes().isEmpty() }
        assertTrue(runBlocking { training.plan().first().isEmpty() })
    }

    @Test
    fun infoButton_opensGuide_fromEveryPlace() {
        runBlocking {
            val exercises = training.exercises().first()
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, exercises.first { it.name == "Bench" }.id, sets = 4, reps = 6, targetRpe = 7.5, order = 0))
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, exercises.first { it.name == "Curl" }.id, sets = 3, reps = 10, targetRpe = 8.0, order = 1))
            // The lifter's own exercise has the button too, and a sheet that says there is no guide.
            val own = training.addExercise(Exercise(0, "Tyre flip", Category.ACCESSORY, null, emptyList(), isBuiltIn = false))
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, own, sets = 3, reps = 10, targetRpe = 8.0, order = 2))
        }
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Tyre flip  3 × 10 @ 8").fetchSemanticsNodes().isNotEmpty() }

        compose.checkInfoButtonOpensGuide("Bench")
        compose.checkInfoButtonOpensGuide("Curl")
        compose.checkInfoButtonOpensGuide("Tyre flip")

        assertEquals(3, runBlocking { training.plan().first() }.size)
    }

    @Test
    fun edit_changeExercise_swapsItThroughThePicker_onSave() {
        runBlocking {
            val bench = training.exercises().first().first { it.name == "Bench" }
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, bench.id, sets = 4, reps = 6, targetRpe = 7.5, order = 0))
            // The exercise to change to has no row yet.
            db.exercises().all().filter { it.name == "Curl" }.forEach { db.exercises().delete(it.id) }
        }
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Bench  4 × 6 @ 7.5").fetchSemanticsNodes().isNotEmpty() }

        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithContentDescription("Change exercise").performClick()
        compose.onNodeWithTag("exercise-search").performTextReplacement("curl")
        compose.onNodeWithTag("exercise-Curl").performClick()
        // Chosen, not saved: Cancel would still leave bench, and no row for the curl.
        compose.onNodeWithText("Edit Monday").assertExists()
        assertTrue(runBlocking { db.exercises().all() }.none { it.name == "Curl" })
        compose.onNodeWithText("Save").performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithText("Curl  4 × 6 @ 7.5").fetchSemanticsNodes().isNotEmpty() }
        val curl = runBlocking { db.exercises().all() }.single { it.name == "Curl" }
        assertEquals(listOf(curl.id), runBlocking { training.plan().first() }.map { it.exerciseId })
    }

    // Native graphics, so text is measured at its real width.
    @Test
    @org.robolectric.annotation.Config(qualifiers = "w360dp-h2400dp-xxhdpi")
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun planRows_fitASmallPhone_withFullSizeTargets() {
        runBlocking {
            val exercises = training.exercises().first()
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, exercises.first { it.name == "Bulgarian split squat" }.id, sets = 3, reps = 10, targetRpe = 7.5, order = 0))
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, exercises.first { it.name == "Squat" }.id, sets = 3, reps = 5, targetRpe = 8.0, order = 1))
        }
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Squat  3 × 5 @ 8").fetchSemanticsNodes().isNotEmpty() }
        val width = compose.onRoot().getUnclippedBoundsInRoot().width
        assertEquals(360.dp, width)

        for (name in listOf("Bulgarian split squat", "Squat")) {
            compose.onNodeWithContentDescription("Form guide for $name").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        }
        // The info button and the row's own controls are all on screen, side by side.
        val controls = listOf("Form guide for Bulgarian split squat", "Form guide for Squat").map { compose.onNodeWithContentDescription(it) } +
            listOf("Move up", "Move down", "More").flatMap { d -> List(2) { compose.onAllNodesWithContentDescription(d)[it] } }
        for (control in controls) {
            val b = control.getUnclippedBoundsInRoot()
            assertTrue(b.left >= 0.dp && b.right <= width)
        }
        val firstRow = listOf("Form guide for Bulgarian split squat", "Move up", "Move down", "More").map { compose.onAllNodesWithContentDescription(it)[0].getUnclippedBoundsInRoot() }
        assertTrue(firstRow.zipWithNext().all { (a, b) -> a.right <= b.left })
        // The text keeps a readable share of the row: it wraps, it is not squeezed out.
        for (text in listOf("Bulgarian split squat  3 × 10 @ 7.5", "Squat  3 × 5 @ 8")) {
            val b = compose.onNodeWithText(text).getUnclippedBoundsInRoot()
            assertTrue("$text is ${b.width} wide", b.width >= 80.dp && b.left >= 0.dp)
        }
    }

    @Test
    fun settings_toggleChangePlates_persists() {
        compose.setContent { SettingsScreen(settings, BackupService(db, settings), onBack = {}) }

        compose.onNodeWithText("Change plates").performClick()

        compose.waitUntil(5_000) { runBlocking { settings.settings.first().changePlates } }
        assertTrue(runBlocking { settings.settings.first().changePlates })
    }

    @Test
    fun settings_restAlert_showsThreeChoicesAndPersists() {
        compose.setContent { SettingsScreen(settings, BackupService(db, settings), onBack = {}) }

        compose.onNodeWithText("Rest alert").assertExists()
        compose.onNodeWithText("Vibrate only").assertIsSelected()
        compose.onNodeWithText("Off").assertExists()
        compose.onNodeWithText("Sound + vibrate").performClick()

        compose.waitUntil(5_000) { runBlocking { settings.settings.first().restAlert } == RestAlert.SOUND_AND_VIBRATE }
        compose.onNodeWithText("Sound + vibrate").assertIsSelected()
    }

    @Test
    fun settings_appLockRow_saysWhatItDoes() {
        compose.setContent { SettingsScreen(settings, BackupService(db, settings), onBack = {}) }

        compose.onNodeWithText("Ask for your fingerprint, face or screen lock when Grit opens, and after a minute away").assertExists()
    }

    @Test
    fun settings_copy_saysGrit() {
        assertEquals(RestoreCheck.Rejected("This file isn't a valid Grit backup."), checkBackupText("not json"))
        assertEquals(
            RestoreCheck.Rejected("This backup is from a newer version of Grit."),
            checkBackupText("""{"version":${Backup.VERSION + 1}}"""),
        )
        assertEquals("grit-backup-2026-11-11.json", backupFileName(LocalDate(2026, 11, 11)))
    }

    @Test
    fun baselineDialog_rejectsInfinityAndOverFiveHundred_andBackupStillEncodes() {
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("baseline-Squat")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("baseline-Squat").performClick()

        val field = compose.onNodeWithText("Estimated 1RM (kg)")
        for (text in listOf("Infinity", "1e400", "NaN", "501")) {
            field.performTextReplacement(text)
            compose.onNodeWithText("Save").assertIsNotEnabled()
        }
        field.performTextReplacement("182,5")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()

        compose.waitUntil(5_000) { runBlocking { training.history().first().baselines.isNotEmpty() } }
        assertEquals(listOf(182.5), runBlocking { training.history().first().baselines.map { it.e1rmKg } })
        // Every stored double is finite, so the strict JSON writer cannot fail on export.
        val text = runBlocking { Backup.encode(BackupService(db, settings).export(0L)) }
        assertTrue(text.contains("182.5"))
    }

    // ---- Strict press: the Progress switch and the known bests ----

    @Test
    fun settings_strictPressSwitch_saysWhatItDoes_andPersists() {
        compose.setContent { SettingsScreen(settings, BackupService(db, settings), onBack = {}) }

        compose.onNodeWithText("Show it on Progress and add it to your total.").assertExists()
        compose.onNodeWithText("Strict press").performClick()
        compose.waitUntil(5_000) { runBlocking { settings.showStrictPress.first() } }

        compose.onNodeWithText("Strict press").performClick()
        compose.waitUntil(5_000) { !runBlocking { settings.showStrictPress.first() } }
    }

    @Test
    fun knownBests_includeStrictPress_onceItHasARow() {
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}) }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("baseline-Squat")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("baseline-Strict press").assertDoesNotExist()

        runBlocking { training.addExercise(Exercise(0, "Strict press", Category.STRICT_PRESS, null, emptyList(), isBuiltIn = true)) }

        // It is a main lift like the other three: it can be given a known best.
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("baseline-Strict press")).fetchSemanticsNodes().isNotEmpty() }
        for (lift in listOf("Squat", "Bench", "Deadlift")) compose.onNodeWithTag("baseline-$lift").assertExists()
    }
}
