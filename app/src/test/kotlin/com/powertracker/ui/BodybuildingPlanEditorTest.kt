package com.powertracker.ui

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.plan.PlanScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Plan entry editor on the bodybuilding side: a rep range with two steppers (bodybuilding mode spec §7.3). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class BodybuildingPlanEditorTest {
    /** Outermost: the database closes after the compose rule has torn the screen down, and once no query is running. */
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val today = LocalDate(2026, 11, 11)
    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        training = TrainingRepository(db)
        settings = SettingsRepository(context)
        runBlocking { settings.update { AppSettings(trainingMode = BODYBUILDING) } }
    }

    @Test
    fun plan_editor_hasTwoRepsSteppers_theTopNeverBelowTheBottom() {
        val curl = runBlocking { training.exercises().first() }.first { it.name == "Curl" }
        runBlocking { training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, curl.id, sets = 3, reps = 10, targetRpe = 8.0, order = 0, mode = BODYBUILDING, repsHigh = 11)) }
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}, today = { today }) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Curl  3 × 10–11 @ 8").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithText("Reps from").assertExists()
        compose.onNodeWithText("Reps to").assertExists()
        compose.onNodeWithContentDescription("Increase Reps from").performClick()
        // 11–11: neither end can pass the other.
        compose.onNodeWithContentDescription("Increase Reps from").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Decrease Reps to").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Increase Reps to").performClick()
        compose.onNodeWithContentDescription("Increase Reps to").performClick()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Curl  3 × 11–13 @ 8").fetchSemanticsNodes().isNotEmpty() }
        val item = runBlocking { training.plan(BODYBUILDING).first() }.single()
        assertEquals(11 to 13, item.reps to item.repsHigh)
    }

    private fun openEditor(item: PlanItem, row: String) {
        runBlocking { training.savePlanItem(item) }
        compose.setContent { PlanScreen(training, settings, onOpenSettings = {}, today = { today }) }
        compose.waitUntil(5_000) { compose.onAllNodesWithText(row).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Edit").performClick()
    }

    private fun curl() = runBlocking { training.exercises().first() }.first { it.name == "Curl" }

    @Test
    fun plan_editor_repsToGoesUpTo30() {
        openEditor(PlanItem(0, DayOfWeek.MONDAY, curl().id, sets = 3, reps = 8, targetRpe = 8.0, order = 0, mode = BODYBUILDING, repsHigh = 29), "Curl  3 × 8–29 @ 8")
        compose.onNodeWithContentDescription("Increase Reps to").assertIsEnabled().performClick()
        compose.onNodeWithContentDescription("Increase Reps to").assertIsNotEnabled()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Curl  3 × 8–30 @ 8").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(30, runBlocking { training.plan(BODYBUILDING).first() }.single().repsHigh)
    }

    @Test
    fun plan_editor_aBodybuildingItemWithNoTopEnd_opensAsARangeFromItsLowEnd() {
        openEditor(PlanItem(0, DayOfWeek.MONDAY, curl().id, sets = 3, reps = 10, targetRpe = 8.0, order = 0, mode = BODYBUILDING, repsHigh = null), "Curl  3 × 10 @ 8")
        compose.onNodeWithText("Reps from").assertExists()
        compose.onNodeWithText("Reps to").assertExists()
        // 10–10: neither end can pass the other.
        compose.onNodeWithContentDescription("Increase Reps from").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Decrease Reps to").assertIsNotEnabled()
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Curl  3 × 10–10 @ 8").fetchSemanticsNodes().isNotEmpty() }
        val item = runBlocking { training.plan(BODYBUILDING).first() }.single()
        assertEquals(10 to 10, item.reps to item.repsHigh)
    }

    @Test
    fun plan_editor_powerliftingSide_keepsOneRepsStepper_upTo20() {
        runBlocking { settings.update { AppSettings() } }
        openEditor(PlanItem(0, DayOfWeek.MONDAY, curl().id, sets = 3, reps = 20, targetRpe = 8.0, order = 0, mode = POWERLIFTING), "Curl  3 × 20 @ 8")
        compose.onNodeWithText("Reps").assertExists()
        compose.onNodeWithText("Reps from").assertDoesNotExist()
        compose.onNodeWithContentDescription("Increase Reps").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Decrease Reps").assertIsEnabled()
    }
}
