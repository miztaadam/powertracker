package com.powertracker.ui.plan

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.Backup
import com.powertracker.data.BackupFile
import com.powertracker.data.BackupService
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.data.sidesLosingPlanOf
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek.MONDAY
import kotlinx.datetime.DayOfWeek.TUESDAY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The warning before a backup restore that would remove a side's plan (1.14.1). */
@RunWith(RobolectricTestRunner::class)
class BackupRestoreWarningTest {
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private lateinit var training: TrainingRepository
    private lateinit var backup: BackupService

    @Before
    fun setUp() {
        training = TrainingRepository(database.db)
        backup = BackupService(database.db, SettingsRepository(ApplicationProvider.getApplicationContext<Context>()))
    }

    private fun id(name: String) = runBlocking { training.exercises().first() }.first { it.name == name }.id

    /** A backup of what is stored now, round-tripped through the file format. */
    private fun export(): BackupFile = runBlocking {
        (Backup.decode(Backup.encode(backup.export(0))) as com.powertracker.data.BackupResult.Ok).file
    }

    private fun storeBothSides() = runBlocking {
        training.savePlanItem(PlanItem(0, MONDAY, id("Squat"), 3, 5, 8.0, 0))
        training.savePlanItem(PlanItem(0, TUESDAY, id("Curl"), 3, 8, 8.0, 0, BODYBUILDING, 12))
    }

    private fun withoutSide(file: BackupFile, side: String) = file.copy(planItems = file.planItems.filter { it.mode != side })

    @Test
    fun sides_currentHasItems_backupHasNone_perSide() {
        val empty = export()
        storeBothSides()
        val full = export()

        runBlocking {
            assertEquals("the backup has both plans: no warning", emptyList<Any>(), backup.sidesLosingPlan(full))
            assertEquals(listOf(BODYBUILDING), backup.sidesLosingPlan(withoutSide(full, "BODYBUILDING")))
            assertEquals(listOf(POWERLIFTING), backup.sidesLosingPlan(withoutSide(full, "POWERLIFTING")))
            assertEquals("both, powerlifting first", listOf(POWERLIFTING, BODYBUILDING), backup.sidesLosingPlan(empty))
        }
        // No current plan on a side: nothing to lose there.
        assertEquals(emptyList<Any>(), sidesLosingPlanOf(emptyList(), empty))
        assertEquals(listOf(BODYBUILDING), sidesLosingPlanOf(listOf(BODYBUILDING, BODYBUILDING), withoutSide(full, "BODYBUILDING")))
        assertEquals(emptyList<Any>(), sidesLosingPlanOf(listOf(POWERLIFTING), withoutSide(full, "BODYBUILDING")))
    }

    @Test
    fun strings() {
        assertEquals("This backup has no powerlifting plan", restoreWarningTitle(listOf(POWERLIFTING)))
        assertEquals("This backup has no bodybuilding plan", restoreWarningTitle(listOf(BODYBUILDING)))
        assertEquals("This backup has no powerlifting or bodybuilding plan", restoreWarningTitle(listOf(POWERLIFTING, BODYBUILDING)))
        assertEquals(
            "Restoring it removes your current bodybuilding plan. Your other data is replaced by the backup's too.",
            restoreWarningBody(listOf(BODYBUILDING)),
        )
        assertEquals(
            "Restoring it removes your current powerlifting and bodybuilding plans. Your other data is replaced by the backup's too.",
            restoreWarningBody(listOf(POWERLIFTING, BODYBUILDING)),
        )
    }

    private var restored = 0
    private var cancelled = 0

    private fun dialog(losing: List<com.powertracker.domain.model.TrainingMode>) = compose.setContent {
        PowertrackerTheme { RestoreConfirmDialog(losing, onConfirm = { restored++ }, onCancel = { cancelled++ }) }
    }

    @Test
    fun warning_cancelRestoresNothing() {
        dialog(listOf(POWERLIFTING, BODYBUILDING))
        compose.onNodeWithText("This backup has no powerlifting or bodybuilding plan").assertIsDisplayed()
        compose.onNodeWithText(restoreWarningBody(listOf(POWERLIFTING, BODYBUILDING))).assertIsDisplayed()
        compose.onNodeWithText("Restore anyway").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        assertEquals(0, restored)
        assertEquals(1, cancelled)
    }

    @Test
    fun warning_restoreAnywayRestores() {
        dialog(listOf(BODYBUILDING))
        compose.onNodeWithText("This backup has no bodybuilding plan").assertIsDisplayed()
        compose.onNodeWithText("Restore anyway").performClick()
        compose.waitForIdle()
        assertEquals(1, restored)
    }

    @Test
    fun noWarning_whenTheBackupHasThePlan_theFlowIsUnchanged() {
        dialog(emptyList())
        compose.onNodeWithText("Replace all data?").assertIsDisplayed()
        compose.onNodeWithText("Restoring replaces everything in Grit with the contents of this backup.").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText("Restore anyway").fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText("Replace").performClick()
        compose.waitForIdle()
        assertEquals(1, restored)
    }
}
