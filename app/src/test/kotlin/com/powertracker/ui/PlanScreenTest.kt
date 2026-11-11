package com.powertracker.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.BackupService
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.data.db.AppDatabase
import com.powertracker.ui.plan.PlanScreen
import com.powertracker.ui.plan.SettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
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
        compose.onNodeWithTag("exercise-Squat").performClick()
        // Defaults are 3 sets x 5 reps @ RPE 8.
        compose.onNodeWithText("Save").performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithText("Squat  3 × 5 @ 8").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Squat  3 × 5 @ 8").assertExists()
    }

    @Test
    fun settings_toggleChangePlates_persists() {
        compose.setContent { SettingsScreen(settings, BackupService(db, settings), onBack = {}) }

        compose.onNodeWithText("Change plates").performClick()

        compose.waitUntil(5_000) { runBlocking { settings.settings.first().changePlates } }
        assertTrue(runBlocking { settings.settings.first().changePlates })
    }
}
