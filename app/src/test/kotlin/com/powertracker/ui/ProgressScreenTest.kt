package com.powertracker.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.ui.library.checkInfoButtonOpensGuide
import com.powertracker.ui.progress.LiftHistoryScreen
import com.powertracker.ui.progress.ProgressScreen
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class ProgressScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository
    private lateinit var settings: SettingsRepository
    private lateinit var exercises: List<Exercise>

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
        training = TrainingRepository(db)
        settings = SettingsRepository(context)
        runBlocking {
            settings.update { AppSettings() }
            // A new install has no strict press row until the lift is first used.
            training.addExercise(Exercise(0, "Strict press", Category.STRICT_PRESS, null, listOf("Front delts", "Triceps"), isBuiltIn = true))
        }
        exercises = runBlocking { training.exercises().first() }
    }

    @After
    fun tearDown() = db.close()

    private fun id(name: String) = exercises.first { it.name == name }.id

    private fun log(date: LocalDate, bodyweightKg: Double?, vararg sets: LoggedSet) = runBlocking {
        val session = training.startSession(date, bodyweightKg, null)
        for (set in sets) training.logSet(session, set)
    }

    private fun set(name: String, kg: Double, reps: Int, rpe: Double, failed: Boolean = false) =
        LoggedSet(id(name), kg, reps, rpe, failed, null, isTopSet = false)

    /** Two weeks of all three lifts; the second squat session beats the first. */
    private fun seed() {
        log(today.minus(9, DateTimeUnit.DAY), 92.0, set("Squat", 140.0, 5, 8.0), set("Bench", 100.0, 5, 8.0), set("Deadlift", 180.0, 5, 8.0))
        log(
            today.minus(2, DateTimeUnit.DAY), 92.5,
            set("Squat", 145.0, 5, 8.0), set("Squat", 150.0, 3, 10.0, failed = true), set("Pause squat", 120.0, 3, 8.0),
            set("Bench", 100.0, 5, 8.0), set("Deadlift", 185.0, 5, 8.0),
        )
    }

    /** [seed], with a strict press top set in both sessions. */
    private fun seedWithPress() {
        log(today.minus(9, DateTimeUnit.DAY), 92.0, set("Squat", 140.0, 5, 8.0), set("Bench", 100.0, 5, 8.0), set("Deadlift", 180.0, 5, 8.0), set("Strict press", 60.0, 5, 8.0))
        log(today.minus(2, DateTimeUnit.DAY), 92.5, set("Squat", 145.0, 5, 8.0), set("Bench", 100.0, 5, 8.0), set("Deadlift", 185.0, 5, 8.0), set("Strict press", 62.5, 5, 8.0))
    }

    private fun showStrictPress(on: Boolean) = runBlocking { settings.setShowStrictPress(on) }

    private fun count(text: String) = compose.onAllNodesWithText(text, substring = true, ignoreCase = true).fetchSemanticsNodes().size +
        compose.onAllNodesWithContentDescription(text, substring = true, ignoreCase = true).fetchSemanticsNodes().size

    private fun waitFor(condition: () -> Boolean) = compose.waitUntil(5_000) {
        compose.waitForIdle()
        condition()
    }

    private fun shows(text: String) = compose.onAllNodesWithText(text, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()

    /** Progress and lift history wired the way the app shell wires them; returns once history has loaded. */
    private fun show(expect: String = "Weekly best e1RM") {
        compose.setContent {
            PowertrackerTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = Tab.PROGRESS.route) {
                    composable(Tab.PROGRESS.route) {
                        ProgressScreen(training, settings, onOpenLift = { nav.navigate(Routes.liftHistory(it.name)) }, today = { today })
                    }
                    composable(Routes.LIFT_HISTORY) { entry ->
                        LiftHistoryScreen(training, entry.arguments?.getString("lift").orEmpty(), onBack = { nav.popBackStack() }, today = { today })
                    }
                }
            }
        }
        waitFor { shows(expect) }
    }

    @Test
    fun infoButton_opensGuide_fromEveryPlace() {
        seed()
        show()
        compose.onNodeWithContentDescription("Squat history", substring = true).performScrollTo().performClick()
        waitFor { shows("Mon 9 Nov 2026") }

        // The lift-history header carries the lift's guide.
        compose.checkInfoButtonOpensGuide("Squat")
        compose.onNodeWithText("Squat history").assertIsDisplayed()
    }

    @Test
    fun tapLift_opensHistory() {
        seed()
        show()

        compose.onNodeWithContentDescription("Squat history", substring = true).performScrollTo().performClick()
        waitFor { shows("Mon 9 Nov 2026") }

        // The history lists both sessions, newest first, with main and variation sets.
        compose.onNodeWithText("Squat history").assertIsDisplayed()
        compose.onNodeWithText("Mon 9 Nov 2026").assertIsDisplayed()
        compose.onNodeWithText("Mon 2 Nov 2026").assertIsDisplayed()
        compose.onNodeWithText("Set 1: 145 kg × 5 @ 8, personal record, done").assertIsDisplayed()
        compose.onNodeWithText("Set 2: 150 kg × 3 @ 10, failed").assertIsDisplayed()
        compose.onNodeWithText("Pause squat").assertIsDisplayed()
        compose.onNodeWithText("Set 1: 120 kg × 3 @ 8, done").assertIsDisplayed()
        compose.onNodeWithText("Set 1: 140 kg × 5 @ 8, personal record, done").assertIsDisplayed()
        // No other lift's sets leak in.
        assertEquals(0, compose.onAllNodesWithText("185 kg", substring = true).fetchSemanticsNodes().size)

        compose.onNodeWithContentDescription("Back").performClick()
        waitFor { shows("Weekly best e1RM") }
        compose.onNodeWithText("Progress").assertIsDisplayed()
    }

    @Test
    fun progress_showsTotalsPrsAndRatios() {
        seed()
        show()

        compose.onNodeWithText("Weekly best e1RM", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText("Records", ignoreCase = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Weekly volume", ignoreCase = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Bodyweight", ignoreCase = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Squat ÷ Deadlift").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Bench ÷ Squat").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun ratio_hiddenWhenALiftHasNoE1rm() {
        log(today.minus(2, DateTimeUnit.DAY), null, set("Squat", 145.0, 5, 8.0), set("Bench", 100.0, 5, 8.0))
        show()

        compose.onNodeWithText("Bench ÷ Squat").performScrollTo().assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("Squat ÷ Deadlift").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("Bodyweight", ignoreCase = true).fetchSemanticsNodes().size)
    }

    @Test
    fun rangeWithNoSessions_keepsTheSelectorAndSaysSo() {
        log(today.minus(10, DateTimeUnit.WEEK), null, set("Squat", 140.0, 5, 8.0))
        show()

        compose.onNodeWithText("8 wk").performClick()
        waitFor { shows("No sessions in this range") }

        // The selector is still there, so the lifter can get back to a range with data.
        compose.onNodeWithText("All").performClick()
        waitFor { !shows("No sessions in this range") }
    }

    @Test
    fun emptyState_showsHint() {
        show(expect = "No lifts logged yet")

        compose.onNodeWithText("No lifts logged yet").assertIsDisplayed()
        compose.onNodeWithText("Log a squat, bench or deadlift session on Today and your charts will appear here.").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("Weekly best e1RM", ignoreCase = true).fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("Lift ratios", ignoreCase = true).fetchSemanticsNodes().size)
    }

    // ---- Strict press behind its setting (strict press spec §5.2) ----

    @Test
    fun switchOff_progressUnchanged() {
        seedWithPress()
        show()

        // Logged strict press sets show nowhere: not as a tile, a line, a record or a volume row.
        assertEquals(0, count("Strict press"))
        compose.onNodeWithText("Total (S+B+D)", ignoreCase = true).assertIsDisplayed()
        assertEquals(0, count("S+B+D+P"))
        for (lift in listOf("Squat", "Bench", "Deadlift")) compose.onNodeWithContentDescription("$lift history", substring = true).assertExists()
    }

    @Test
    fun switchOn_showsStrictPressEverywhere() {
        seedWithPress()
        showStrictPress(true)
        show()

        // The tile, the chart legend, the record and the volume row.
        compose.onNodeWithContentDescription("Strict press history. Estimated 1RM", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Strict press").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Strict press record:", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Strict press volume this week: 313 kg").performScrollTo().assertIsDisplayed()
        // It is in no ratio.
        assertEquals(0, compose.onAllNodesWithText("Strict press ÷", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("÷ Strict press", substring = true).fetchSemanticsNodes().size)

        // Its tile opens its own history page.
        compose.onNodeWithContentDescription("Strict press history. Estimated 1RM", substring = true).performScrollTo().performClick()
        waitFor { shows("Mon 9 Nov 2026") }
        compose.onNodeWithText("Strict press history").assertIsDisplayed()
        compose.onNodeWithText("Set 1: 62.5 kg × 5 @ 8, personal record, done").assertIsDisplayed()
        compose.onNodeWithText("Set 1: 60 kg × 5 @ 8, personal record, done").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("185 kg", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun totalLabel_bothWays() {
        seedWithPress()
        show()
        compose.onNodeWithText("Total (S+B+D)", ignoreCase = true).assertIsDisplayed()

        // The label follows the switch while the screen is open.
        showStrictPress(true)
        waitFor { shows("Total (S+B+D+P)") }
        compose.onNodeWithText("Total (S+B+D+P)", ignoreCase = true).assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("Total (S+B+D)", ignoreCase = true).fetchSemanticsNodes().size)

        showStrictPress(false)
        waitFor { shows("Total (S+B+D)") && !shows("S+B+D+P") }
    }

    @Test
    fun switchOn_noPressData_totalExplains() {
        seed()
        showStrictPress(true)
        show()

        // No total without every included lift: the note says which lift it is waiting for.
        compose.onNodeWithText("Total (S+B+D+P)", ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithText("Log all four lifts, strict press included, to see your total.").assertIsDisplayed()
        // The empty lift has its tile and volume row, and nothing crashes on its empty series.
        compose.onNodeWithContentDescription("Strict press history. No estimate yet").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Strict press volume this week: 0 kg").performScrollTo().assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("Strict press record:", substring = true).fetchSemanticsNodes().size)
        compose.onNodeWithText("Squat ÷ Deadlift").performScrollTo().assertIsDisplayed()

        // Its history page opens empty.
        compose.onNodeWithContentDescription("Strict press history. No estimate yet").performScrollTo().performClick()
        waitFor { shows("No strict press sessions yet") }

        // Switched off again, the three-lift total is back.
        compose.onNodeWithContentDescription("Back").performClick()
        showStrictPress(false)
        waitFor { !shows("Log all four lifts") }
        compose.onNodeWithText("Total (S+B+D)", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun emptyState_namesStrictPress_whenTheSwitchIsOn() {
        showStrictPress(true)
        show(expect = "No lifts logged yet")

        compose.onNodeWithText("Log a squat, bench, deadlift or strict press session on Today and your charts will appear here.").assertIsDisplayed()
    }
}
