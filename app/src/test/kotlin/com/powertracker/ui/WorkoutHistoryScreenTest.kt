package com.powertracker.ui

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.TrainingMode
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.design.assertTextsFit
import com.powertracker.ui.progress.ProgressContent
import com.powertracker.ui.progress.ProgressScreen
import com.powertracker.ui.progress.ProgressState
import com.powertracker.ui.progress.Workout
import com.powertracker.ui.progress.WorkoutExercise
import com.powertracker.ui.progress.WorkoutHistoryContent
import com.powertracker.ui.progress.WorkoutHistoryScreen
import com.powertracker.ui.progress.WorkoutHistoryState
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** History (1.20): every past workout, opened from the History button at the top right of Progress. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class WorkoutHistoryScreenTest {
    /** Outermost: the database closes after the compose rule has torn the screen down, and once no query is running. */
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

    private lateinit var training: TrainingRepository
    private lateinit var settings: SettingsRepository
    private lateinit var exercises: List<Exercise>

    @Before
    fun setUp() {
        training = TrainingRepository(database.db)
        settings = SettingsRepository(ApplicationProvider.getApplicationContext<Context>())
        runBlocking { settings.update { AppSettings() } }
        exercises = runBlocking { training.exercises().first() }
    }

    private fun id(name: String) = exercises.first { it.name == name }.id
    private val accessory get() = exercises.first { it.category == Category.ACCESSORY }

    private fun set(exerciseId: Long, kg: Double, reps: Int, rpe: Double?, failed: Boolean = false) = LoggedSet(exerciseId, kg, reps, rpe, failed, null, isTopSet = false)

    private fun log(daysAgo: Int, mode: TrainingMode, bodyweightKg: Double?, vararg sets: LoggedSet) = runBlocking {
        val session = training.startSession(today.minus(daysAgo, DateTimeUnit.DAY), bodyweightKg, null, mode)
        for (set in sets) training.logSet(session, set)
        session
    }

    private fun waitFor(condition: () -> Boolean) = compose.waitUntil(5_000) {
        compose.waitForIdle()
        condition()
    }

    private fun shows(text: String) = compose.onAllNodesWithText(text, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()

    /** Progress and History wired the way the app shell wires them; returns once Progress shows [expect]. */
    private fun show(expect: String) {
        compose.setContent {
            PowertrackerTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = Tab.PROGRESS.route) {
                    composable(Tab.PROGRESS.route) {
                        ProgressScreen(training, settings, onOpenLift = {}, today = { today }, onOpenHistory = { nav.navigate(Routes.WORKOUT_HISTORY) { launchSingleTop = true } })
                    }
                    composable(Routes.WORKOUT_HISTORY) { WorkoutHistoryScreen(training, onBack = { nav.popBackStack() }) }
                }
            }
        }
        waitFor { shows(expect) }
    }

    private fun top(text: String) = compose.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot().top

    @Test
    fun historyButton_opensEveryWorkout_newestFirst_fromBothSides_andBackReturns() {
        val squat = id("Squat")
        val pause = id("Pause squat")
        val curl = accessory
        log(9, TrainingMode.POWERLIFTING, null, set(squat, 140.0, 5, 8.0), set(id("Bench"), 100.0, 5, 8.0))
        log(5, TrainingMode.BODYBUILDING, null, set(curl.id, 20.0, 12, 8.0), set(curl.id, 20.0, 11, null))
        // Squat, then a variation, then squat again: one Squat group, in the order first logged.
        val latest = log(2, TrainingMode.POWERLIFTING, 92.5, set(squat, 145.0, 5, 8.0), set(pause, 120.0, 3, 8.0), set(squat, 150.0, 3, 10.0, failed = true))
        // Started and nothing logged: no workout.
        log(0, TrainingMode.POWERLIFTING, null)
        show("Weekly best e1RM")

        compose.onNodeWithText("History").assertIsDisplayed().performClick()
        waitFor { shows("3 workouts") }

        // Newest first, both sides in one list, each with its side, its counts and the bodyweight when there was one.
        val dates = listOf("Mon 9 Nov 2026", "Fri 6 Nov 2026", "Mon 2 Nov 2026").map { top(it) }
        assertEquals(dates.sorted(), dates)
        compose.onNodeWithText("POWERLIFTING · 2 EXERCISES · 3 SETS · 92.5 KG BODYWEIGHT", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("BODYBUILDING · 1 EXERCISE · 2 SETS", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("POWERLIFTING · 2 EXERCISES · 2 SETS", useUnmergedTree = true).assertExists()
        // The latest workout: Squat's two sets together, then the variation's; the missed set says so.
        val card = hasTestTag("workout-$latest")
        compose.onNodeWithTag("workout-$latest", useUnmergedTree = true).assertExists()
        val rows = listOf("Set 1: 145 kg × 5 at RPE 8", "Set 2: 150 kg × 3 at RPE 10, missed", "Set 1: 120 kg × 3 at RPE 8").map { top(it) }
        assertEquals("Squat's sets, then the pause squat's", rows.sorted(), rows)
        // "Squat" is in the oldest workout too: the first on screen is the latest workout's.
        val squatName = compose.onAllNodesWithText("Squat", useUnmergedTree = true)[0].getUnclippedBoundsInRoot().top
        assertTrue(squatName < rows[0] && rows[1] < top("Pause squat") && top("Pause squat") < rows[2])
        compose.onNodeWithText("Set 2: 20 kg × 11", useUnmergedTree = true).assertExists()
        compose.onAllNodes(card).assertCountEquals(1)

        compose.onNodeWithContentDescription("Back").performClick()
        waitFor { shows("Weekly best e1RM") }
        compose.onNodeWithText("Progress").assertIsDisplayed()
    }

    @Test
    fun historyButton_isOnBothSides_alsoWithNothingLogged() {
        show("No lifts logged yet")
        compose.onNodeWithText("History").assertIsDisplayed()
        runBlocking { settings.setTrainingMode(TrainingMode.BODYBUILDING, today) }
        waitFor { shows("No exercises in your plan yet") }
        compose.onNodeWithText("History").assertIsDisplayed().performClick()

        waitFor { shows("No workouts yet") }
        compose.onNodeWithText("0 WORKOUTS", useUnmergedTree = true).assertExists()
        compose.onAllNodesWithTag("past-sets", useUnmergedTree = true).assertCountEquals(0)
    }

    private fun workouts(n: Int) = (0 until n).map { i ->
        Workout(
            sessionId = (n - i).toLong(),
            date = today.minus(i, DateTimeUnit.DAY),
            side = TrainingMode.POWERLIFTING,
            bodyweightKg = if (i % 2 == 0) 92.5 else null,
            exercises = listOf(WorkoutExercise(1, "Squat", listOf(set(1, 100.0 + i, 5, 8.0))), WorkoutExercise(2, "Incline dumbbell press", listOf(set(2, 27.5, 12, 9.5), set(2, 27.5, 10, 9.5, failed = true)))),
            setCount = 3,
        )
    }

    @Test
    fun aLongLog_isOneLazyList_theCountIsOfTheWholeLog_andTheOldestCanBeReached() {
        compose.setContent { PowertrackerTheme { WorkoutHistoryContent(WorkoutHistoryState(loading = false, workouts = workouts(45)), onBack = {}) } }
        compose.onNodeWithText("45 WORKOUTS", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("workout-45", useUnmergedTree = true).assertExists()
        // Lazy: the oldest workout is not drawn until it is scrolled to.
        compose.onNodeWithTag("workout-1", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("workout-list").performScrollToNode(hasTestTag("workout-1"))
        compose.onNodeWithTag("workout-1", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Mon 28 Sep 2026", useUnmergedTree = true).assertExists()
    }

    @Test
    fun oneWorkout_isCountedInTheSingular_andLoadingShowsNeitherACountNorTheEmptyState() {
        compose.setContent { PowertrackerTheme { WorkoutHistoryContent(WorkoutHistoryState(loading = false, workouts = workouts(1)), onBack = {}) } }
        compose.onNodeWithText("1 WORKOUT", useUnmergedTree = true).assertExists()
    }

    @Test
    fun whileLoading_thereIsNeitherACountNorTheEmptyState() {
        compose.setContent { PowertrackerTheme { WorkoutHistoryContent(WorkoutHistoryState(), onBack = {}) } }
        compose.onNodeWithText("History").assertIsDisplayed()
        compose.onAllNodesWithText("WORKOUT", substring = true, useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithText("No workouts yet").assertCountEquals(0)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w320dp-h2400dp-xhdpi")
    fun smallPhoneAndLargeFont_historyFits_andSoDoesProgressWithItsButton_onBothSides() {
        var side = TrainingMode.POWERLIFTING
        var history = false
        val shown = androidx.compose.runtime.mutableStateOf(side to history)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                val (mode, onHistory) = shown.value
                PowertrackerTheme(mode = mode) {
                    if (onHistory) WorkoutHistoryContent(WorkoutHistoryState(loading = false, workouts = workouts(3)), onBack = {})
                    else ProgressContent(ProgressState(loading = false, mode = mode), onRange = {}, onOpenLift = {}, onOpenHistory = {})
                }
            }
        }
        for (mode in listOf(TrainingMode.POWERLIFTING, TrainingMode.BODYBUILDING)) {
            side = mode
            shown.value = side to false
            compose.waitForIdle()
            // The button is whole and inside the screen, beside the title; the caption and the title are not cut.
            val screen = compose.onRoot().getUnclippedBoundsInRoot()
            val button = compose.onNodeWithText("History").getUnclippedBoundsInRoot()
            assertTrue("$mode: the History button is inside the screen", button.left >= screen.left && button.right <= screen.right)
            // A compact button, as Today's Finish is: 40 dp at least.
            assertTrue("$mode: the History button keeps its height", button.bottom - button.top >= androidx.compose.ui.unit.Dp(39.5f))
            compose.assertTextsFit()
            // Beside the button the caption and the title have less room, and each is one line: a one-line text that
            // runs out of room drops its tail without an ellipsis, which the check above cannot see. So: every letter
            // of each is on its line.
            for (text in listOf(if (mode == TrainingMode.BODYBUILDING) "BEST SET PER SESSION" else "ESTIMATED 1RM", "Progress")) {
                val node = compose.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
                val layout = mutableListOf<androidx.compose.ui.text.TextLayoutResult>().also { node.config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult].action?.invoke(it) }.single()
                assertEquals("$mode: \"$text\" is whole", text.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
                assertTrue("$mode: \"$text\" is not cut at the bottom", !layout.didOverflowHeight)
            }
        }
        history = true
        shown.value = side to history
        compose.waitForIdle()
        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        // Rows speak as one sentence, so they are checked by their bounds: inside the screen and drawn whole.
        for (text in listOf("Set 1: 100 kg × 5 at RPE 8", "Set 1: 27.5 kg × 12 at RPE 9.5", "Set 2: 27.5 kg × 10 at RPE 9.5, missed")) {
            val row = compose.onAllNodesWithText(text, useUnmergedTree = true)[0]
            val b = row.getUnclippedBoundsInRoot()
            assertTrue("\"$text\" fits across the screen", b.left >= screen.left && b.right <= screen.right)
            val n = row.fetchSemanticsNode()
            assertEquals("\"$text\" is not clipped", n.size.height.toFloat(), n.boundsInRoot.height, 0.5f)
            assertEquals("\"$text\" is not cut at the side", n.size.width.toFloat(), n.boundsInRoot.width, 0.5f)
        }
        compose.assertTextsFit()
    }
}
