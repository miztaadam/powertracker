package com.powertracker.ui

import android.content.Context
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasTestTag
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
import com.powertracker.domain.model.PlanItem
import com.powertracker.ui.plan.PlanScreen
import com.powertracker.ui.plan.RestoreCheck
import com.powertracker.ui.plan.SettingsScreen
import com.powertracker.ui.plan.backupFileName
import com.powertracker.ui.plan.checkBackupText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek
import java.time.LocalDate

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
        compose.onNodeWithTag("exercise-Squat").performClick()
        // Defaults are 3 sets x 5 reps @ RPE 8.
        compose.onNodeWithText("Save").performClick()

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
        compose.onNodeWithText("Selected: Bench").assertExists()
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
        assertEquals("grit-backup-2026-11-11.json", backupFileName(LocalDate.of(2026, 11, 11)))
    }
}
