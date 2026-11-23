package com.powertracker.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.BackupService
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.PlanItem
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.library.checkRpeInfoOpensSheet
import com.powertracker.ui.plan.PlanScreen
import com.powertracker.ui.plan.SettingsScreen
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.TodayScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The "What RPE means" button and its sheet, from each place RPE is set (strict press spec §5.4). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class RpeInfoTest {
    /** Outermost: the database closes after the compose rule has torn the screen down, and once no query is running. */
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        training = TrainingRepository(db)
        settings = SettingsRepository(context)
        runBlocking {
            settings.update { AppSettings() }
            val squat = training.exercises().first().first { it.name == "Squat" }
            training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, squat.id, sets = 5, reps = 5, targetRpe = 8.0, order = 0))
            training.startSession(today, null, null)
        }
    }

    private fun waitForText(text: String) = compose.waitUntil(5_000) {
        compose.waitForIdle()
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun rpeInfo_opensFromTodayPlanAndSettings() {
        var screen by mutableStateOf("today")
        compose.setContent {
            PowertrackerTheme {
                when (screen) {
                    "today" -> TodayScreen(training, AdviceRepository(db), settings, onOpenCoach = {}, today = { today })
                    "plan" -> PlanScreen(training, settings, onOpenSettings = {})
                    else -> SettingsScreen(settings, BackupService(db, settings), onBack = {})
                }
            }
        }

        // Today: beside the RPE field's label on the exercise card. Reading it logs and changes nothing.
        waitForText("Log set 1")
        compose.checkRpeInfoOpensSheet()
        compose.onNodeWithText("Log set 1").assertExists()
        assertTrue(runBlocking { training.history().first() }.sessions.all { it.sets.isEmpty() })

        // Plan: beside the target RPE in the item dialog, which stays open behind the sheet.
        screen = "plan"
        waitForText("Squat  5 × 5 @ 8")
        compose.onNodeWithContentDescription("More").performClick()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithText("Edit Wednesday").assertExists()
        compose.onNode(hasContentDescription("What RPE means") and hasAnyAncestor(isDialog())).assertExists()
        compose.checkRpeInfoOpensSheet()
        compose.onNodeWithText("Edit Wednesday").assertExists()
        compose.onNodeWithText("Cancel").performClick()

        // Settings: beside the default RPE.
        screen = "settings"
        waitForText("Default RPE")
        compose.onNodeWithContentDescription("What RPE means").performScrollTo()
        compose.checkRpeInfoOpensSheet()
        assertEquals(8.0, runBlocking { settings.settings.first() }.defaultRpe, 0.0)
    }
}
