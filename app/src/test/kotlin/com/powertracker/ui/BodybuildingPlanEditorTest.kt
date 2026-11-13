package com.powertracker.ui

import android.content.Context
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
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.ui.plan.PlanScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.After
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
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate(2026, 11, 11)
    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
        training = TrainingRepository(db)
        settings = SettingsRepository(context)
        runBlocking { settings.update { AppSettings(trainingMode = BODYBUILDING) } }
    }

    @After
    fun tearDown() = db.close()

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
}
