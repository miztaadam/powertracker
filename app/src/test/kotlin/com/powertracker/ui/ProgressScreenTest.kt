package com.powertracker.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.TrainingRepository
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.ui.progress.LiftHistoryScreen
import com.powertracker.ui.progress.ProgressScreen
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class ProgressScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val today: LocalDate = LocalDate.of(2026, 11, 11) // a Wednesday

    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository
    private lateinit var exercises: List<Exercise>

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
        training = TrainingRepository(db)
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
        log(today.minusDays(9), 92.0, set("Squat", 140.0, 5, 8.0), set("Bench", 100.0, 5, 8.0), set("Deadlift", 180.0, 5, 8.0))
        log(
            today.minusDays(2), 92.5,
            set("Squat", 145.0, 5, 8.0), set("Squat", 150.0, 3, 10.0, failed = true), set("Pause squat", 120.0, 3, 8.0),
            set("Bench", 100.0, 5, 8.0), set("Deadlift", 185.0, 5, 8.0),
        )
    }

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
                        ProgressScreen(training, onOpenLift = { nav.navigate(Routes.liftHistory(it.name)) }, today = { today })
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
        compose.onNodeWithText("Recent PRs", ignoreCase = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Weekly volume", ignoreCase = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Bodyweight", ignoreCase = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Squat ÷ Deadlift").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Bench ÷ Squat").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun ratio_hiddenWhenALiftHasNoE1rm() {
        log(today.minusDays(2), null, set("Squat", 145.0, 5, 8.0), set("Bench", 100.0, 5, 8.0))
        show()

        compose.onNodeWithText("Bench ÷ Squat").performScrollTo().assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("Squat ÷ Deadlift").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("Bodyweight", ignoreCase = true).fetchSemanticsNodes().size)
    }

    @Test
    fun emptyState_showsHint() {
        show(expect = "No lifts logged yet")

        compose.onNodeWithText("No lifts logged yet").assertIsDisplayed()
        compose.onNodeWithText("Log a squat, bench or deadlift session on Today and your charts will appear here.").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("Weekly best e1RM", ignoreCase = true).fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("Lift ratios", ignoreCase = true).fetchSemanticsNodes().size)
    }
}
