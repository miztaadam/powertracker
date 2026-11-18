package com.powertracker.ui

import android.content.Context
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingMode
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.components.FakeHapticFeedback
import com.powertracker.ui.components.WithHaptics
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.SKIP_SHEET_TAP_WINDOW_MS
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

/** Skipping a planned exercise on Today (skip-a-planned-exercise plan, Task 3): the row menu, the sheet, Undo, Bring back. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class SkipTodayScreenTest {
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository
    private lateinit var squat: Exercise
    private lateinit var bench: Exercise
    private var squatItem = 0L
    private var benchItem = 0L
    private val haptics = FakeHapticFeedback()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
        settings = SettingsRepository(context)
        runBlocking {
            settings.update { AppSettings() }
            val all = training.exercises().first()
            squat = all.first { it.name == "Squat" }
            bench = all.first { it.name == "Bench" }
            squatItem = training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, squat.id, sets = 5, reps = 5, targetRpe = 8.0, order = 0))
            benchItem = training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, bench.id, sets = 3, reps = 5, targetRpe = 8.0, order = 1))
        }
    }

    private fun show() = compose.setContent {
        PowertrackerTheme { WithHaptics(haptics) { TodayScreen(training, advice, settings, onOpenCoach = {}, today = { today }) } }
    }

    private fun waitFor(condition: () -> Boolean) = compose.waitUntil(5_000) {
        compose.waitForIdle()
        condition()
    }

    private fun exists(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    private fun menuExists(name: String) = compose.onAllNodes(hasContentDescription("Options for $name")).fetchSemanticsNodes().isNotEmpty()

    private fun skips(): List<Skip> = runBlocking { training.skips().first() }

    /** Long enough for a skip to have been written, had one been chosen. */
    private fun settled() {
        val deadline = System.currentTimeMillis() + 300
        while (System.currentTimeMillis() < deadline) { compose.waitForIdle(); Thread.sleep(10) }
    }

    /** The row menu's Skip today for [name]: the reason sheet opens. */
    private fun openSheet(name: String) {
        waitFor { menuExists(name) }
        compose.onNodeWithContentDescription("Options for $name").performScrollTo().performClick()
        waitFor { exists("Skip today") }
        compose.onNodeWithText("Skip today").performClick()
        waitFor { exists("Why skip $name?") }
    }

    private fun pastTheTapWindow() {
        compose.mainClock.advanceTimeBy(SKIP_SHEET_TAP_WINDOW_MS)
        compose.waitForIdle()
    }

    private fun choose(reason: String) {
        pastTheTapWindow()
        compose.onNodeWithText(reason).performClick()
    }

    /** Back, sent to the sheet's window. */
    private fun pressBack() {
        val view = compose.onNode(hasText("Why skip Bench?")).fetchSemanticsNode().root as android.view.View
        compose.runOnUiThread {
            val root = view.rootView
            root.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_BACK))
            root.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_BACK))
        }
        compose.waitForIdle()
    }

    private fun startSession() = runBlocking { training.startSession(today, null, null) }

    private fun set(exercise: Exercise, kg: Double) = LoggedSet(exercise.id, kg, 5, 8.0, failed = false, missPoint = null, isTopSet = false)

    @Test
    fun skip_menuOnlyOnPlannedRowWithNoSets() {
        val session = startSession()
        runBlocking { training.logSet(session, set(squat, 100.0)) }
        show()
        waitFor { menuExists("Bench") }
        assertTrue("squat has a set", !menuExists("Squat"))

        // An exercise added today keeps its Remove, and has no Skip today.
        compose.onNodeWithText("Add exercise").performScrollTo().performClick()
        waitFor { compose.onAllNodes(hasTestTag("exercise-search")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("exercise-search").performTextReplacement("Curl")
        waitFor { compose.onAllNodes(hasTestTag("exercise-Curl")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("exercise-Curl").performClick()
        waitFor { exists("Remove") }
        assertTrue("an added exercise", !menuExists("Curl"))
        assertTrue(menuExists("Bench"))
    }

    @Test
    fun skip_menuOnTheStartCard_beforeASession() {
        show()
        waitFor { exists("Ready to train?") }
        assertTrue(menuExists("Squat"))
        assertTrue(menuExists("Bench"))
    }

    @Test
    fun skip_sheetCancel_storesNothing() {
        show()
        openSheet("Bench")
        for (reason in listOf("Busy or no equipment", "Pain", "Out of time", "Just skip")) compose.onNodeWithText(reason).assertExists()

        pressBack()
        waitFor { !exists("Why skip Bench?") }
        settled()
        assertEquals(emptyList<Skip>(), skips())
        assertTrue(menuExists("Bench"))
        assertEquals(0, haptics.calls.size)
    }

    @Test
    fun skip_withoutSession_rowLeaves_snackbarAndSkippedLine() {
        show()
        openSheet("Bench")
        choose("Busy or no equipment")

        waitFor { skips().isNotEmpty() }
        assertEquals(Skip(skips().single().id, today, TrainingMode.POWERLIFTING, bench.id, benchItem, SkipReason.BUSY), skips().single())
        waitFor { exists("Bench skipped.") }
        compose.onNodeWithText("Undo").assertExists()
        waitFor { exists("Skipped today") }
        compose.onNodeWithText("Bring back").assertExists()
        assertTrue("the row left", !menuExists("Bench"))
        compose.onNodeWithText("Ready to train?").assertExists()
    }

    @Test
    fun skip_undo_onTheSnackbar_bringsTheRowBack() {
        startSession()
        show()
        openSheet("Bench")
        choose("Pain")
        waitFor { exists("Bench skipped.") }

        compose.onNodeWithText("Undo").performClick()

        waitFor { skips().isEmpty() }
        waitFor { menuExists("Bench") && !exists("Skipped today") }
        assertTrue(runBlocking { advice.all() }.none { it.type == com.powertracker.domain.model.AdviceType.SKIP })
    }

    @Test
    fun skip_bringBack_onTheSkippedLine() {
        startSession()
        show()
        openSheet("Bench")
        choose("Just skip")
        waitFor { exists("Skipped today") }
        assertEquals(listOf<SkipReason?>(null), skips().map { it.reason })

        compose.onNodeWithText("Bring back").performScrollTo().performClick()

        waitFor { skips().isEmpty() }
        waitFor { menuExists("Bench") && !exists("Skipped today") }
    }

    @Test
    fun skip_ticksOnce() {
        startSession()
        show()
        openSheet("Bench")
        assertEquals("opening the menu and the sheet", 0, haptics.calls.size)
        choose("Out of time")
        waitFor { skips().isNotEmpty() }
        waitFor { haptics.calls.isNotEmpty() }
        settled()
        assertEquals(listOf(HapticFeedbackType.ContextClick), haptics.calls.toList())

        // Undo does not buzz.
        compose.onNodeWithText("Undo").performClick()
        waitFor { skips().isEmpty() }
        settled()
        assertEquals(1, haptics.calls.size)
    }

    @Test
    fun doubleTapSkipToday_doesNotPickAReason() {
        show()
        openSheet("Bench")
        val opened = compose.mainClock.currentTime

        // A tap on an answer just after the sheet opened (the second tap of a double tap): nothing happens.
        compose.onNodeWithText("Pain").performClick()
        compose.waitForIdle()
        assertTrue("inside the window", compose.mainClock.currentTime - opened < SKIP_SHEET_TAP_WINDOW_MS)
        settled()
        assertEquals(emptyList<Skip>(), skips())
        assertTrue("the sheet stays", exists("Why skip Bench?"))

        // Once the moment has passed, the answer counts.
        choose("Pain")
        waitFor { skips().isNotEmpty() }
        assertEquals(SkipReason.PAIN, skips().single().reason)
    }

    @Test
    fun skip_survivesRecreation() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { PowertrackerTheme { WithHaptics(haptics) { TodayScreen(training, advice, settings, onOpenCoach = {}, today = { today }) } } }
        openSheet("Bench")

        restoration.emulateSavedInstanceStateRestore()

        waitFor { exists("Why skip Bench?") }
        assertEquals(emptyList<Skip>(), skips())
        choose("Busy or no equipment")
        waitFor { skips().isNotEmpty() }
        assertEquals(SkipReason.BUSY, skips().single().reason)
    }

    @Test
    fun skipEveryPlannedExercise_noRestDayView_skippedLineShown() {
        show()
        openSheet("Squat")
        choose("Just skip")
        waitFor { skips().size == 1 && !menuExists("Squat") }
        openSheet("Bench")
        choose("Just skip")
        waitFor { skips().size == 2 && exists("Skipped today") }

        // Before a session: not the rest day's "Nothing planned".
        assertTrue(!exists("Nothing planned"))
        assertTrue(!exists("There is nothing in your plan"))
        assertEquals(2, compose.onAllNodesWithText("Bring back").fetchSemanticsNodes().size)
        compose.onNodeWithText("WEDNESDAY · 0 EXERCISES").assertExists()

        // In the session too, the line stays.
        compose.onNodeWithText("Start session").performScrollTo().performClick()
        waitFor { exists("Add exercise") }
        assertTrue(!exists("There is nothing in your plan"))
        waitFor { exists("Skipped today") }
        assertEquals(2, compose.onAllNodesWithText("Bring back").fetchSemanticsNodes().size)
    }

    @Test
    fun finish_countsSkipped_inTheSummary() {
        val session = startSession()
        runBlocking { training.logSet(session, set(squat, 100.0)) }
        show()
        openSheet("Bench")
        choose("Out of time")
        waitFor { skips().isNotEmpty() }

        compose.onNodeWithText("Finish").performClick()

        waitFor { exists("Session saved") }
        compose.onNodeWithText("No new coach notes, 1 skipped").assertExists()
    }

    @Test
    fun skippedLine_notOnAnotherDay() {
        runBlocking {
            training.skip(Skip(0, LocalDate(2026, 11, 10), TrainingMode.POWERLIFTING, bench.id, benchItem, SkipReason.BUSY)) { _, _ -> emptyList() }
        }
        show()
        waitFor { menuExists("Bench") }
        assertTrue(!exists("Skipped today"))
    }
}
