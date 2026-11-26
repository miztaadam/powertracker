package com.powertracker.ui

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.powertracker.ui.progress.calendarSummary
import com.powertracker.ui.progress.firstOfMonth
import com.powertracker.ui.progress.monthGrid
import com.powertracker.ui.progress.monthIndex
import com.powertracker.ui.progress.monthTitle
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
                    composable(Routes.WORKOUT_HISTORY) { WorkoutHistoryScreen(training, onBack = { nav.popBackStack() }, today = { today }) }
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
        compose.setContent { PowertrackerTheme { WorkoutHistoryContent(WorkoutHistoryState(loading = false, workouts = workouts(45)), onBack = {}, today = today) } }
        compose.onNodeWithText("45 WORKOUTS", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("workout-45", useUnmergedTree = true).assertExists()
        // Lazy: the oldest workout is not drawn until it is scrolled to.
        compose.onNodeWithTag("workout-1", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("workout-list").performScrollToNode(hasTestTag("workout-1"))
        compose.onNodeWithTag("workout-1", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Mon 28 Sep 2026", useUnmergedTree = true).assertExists()
    }

    // ---- 1.22: the calendar over the list ----

    /** A workout on each of [days] (ISO), one squat set each, newest first as History lists them. */
    private fun workoutsOn(vararg days: String) = days.sortedDescending().mapIndexed { i, day ->
        Workout((days.size - i).toLong(), LocalDate.parse(day), TrainingMode.POWERLIFTING, null, listOf(WorkoutExercise(1, "Squat", listOf(set(1, 100.0, 5, 8.0)))), 1)
    }

    private fun showHistory(workouts: List<Workout>) = compose.setContent { PowertrackerTheme { WorkoutHistoryContent(WorkoutHistoryState(loading = false, workouts = workouts), onBack = {}, today = today) } }

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun calendar_opensOnThisMonth_withAButtonOnEachWorkoutDay_aRingOnToday_andTheCounts() {
        // Wednesday 11 November: this week (Mon 9), last week (Tue 3, Fri 6) and the week before (Wed 28 Oct), then a gap.
        showHistory(workoutsOn("2026-11-09", "2026-11-06", "2026-11-03", "2026-10-28", "2026-10-07"))
        compose.onNodeWithText("November 2026").assertExists()
        for (day in listOf("2026-11-03", "2026-11-06", "2026-11-09")) compose.onNodeWithTag("workout-day-$day", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Tue 3 Nov 2026: Powerlifting · 1 exercise · 1 set. Show it").assertExists()
        // The other days are numbers only, today among them, in its ring.
        assertTrue(exists("day-2026-11-11"))
        assertTrue(!exists("workout-day-2026-11-11"))
        compose.onAllNodesWithTag("workout-day-today", useUnmergedTree = true).assertCountEquals(1)
        assertTrue(exists("day-2026-11-01") && exists("day-2026-11-30") && !exists("day-2026-10-31") && !exists("day-2026-12-01"))
        compose.onNodeWithText("3 workouts in November · 3 weeks in a row", useUnmergedTree = true).assertExists()
        // This month is as far forward as it goes.
        compose.onNodeWithContentDescription("Next month").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Previous month").assertIsEnabled()
    }

    @Test
    fun calendar_arrows_goBackToTheFirstWorkoutsMonth_andForwardToThisMonth_only() {
        showHistory(workoutsOn("2026-11-09", "2026-10-28", "2026-09-30"))
        compose.onNodeWithContentDescription("Previous month").performClick()
        compose.onNodeWithText("October 2026").assertExists()
        // The count is the month on screen's; the weeks in a row are counted from today whatever month shows.
        compose.onNodeWithText("1 workout in October · 1 week in a row", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("workout-day-2026-10-28", useUnmergedTree = true).assertExists()
        // No day is ringed in a month that is not this one.
        compose.onAllNodesWithTag("workout-day-today", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithContentDescription("Previous month").performClick()
        compose.onNodeWithText("September 2026").assertExists()
        compose.onNodeWithContentDescription("Previous month").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Next month").performClick()
        compose.onNodeWithContentDescription("Next month").performClick()
        compose.onNodeWithText("November 2026").assertExists()
        compose.onNodeWithContentDescription("Next month").assertIsNotEnabled()
    }

    @Test
    fun calendar_tapADay_bringsItsWorkoutToTheTop_alsoOneFarDownTheLog() {
        // Sixty days in a row back from yesterday: most are far below what the lazy list has drawn.
        showHistory(workoutsOn(*(1..60).map { today.minus(it, DateTimeUnit.DAY).toString() }.toTypedArray()))
        // Session ids count up with the date: 12 October is the 30th workout down, id 31.
        compose.onNodeWithTag("workout-31", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Previous month").performClick()
        compose.onNodeWithText("October 2026").assertExists()
        compose.onNodeWithContentDescription("Mon 12 Oct 2026: Powerlifting · 1 exercise · 1 set. Show it").performClick()
        compose.waitForIdle()
        // Its card is drawn, at the top of the list (thirty more below it fill this tall test screen).
        val card = compose.onNodeWithTag("workout-31", useUnmergedTree = true)
        card.assertExists()
        val list = compose.onNodeWithTag("workout-list").getUnclippedBoundsInRoot()
        assertEquals(list.top.value, card.getUnclippedBoundsInRoot().top.value, 1f)
        compose.onNodeWithText("Mon 12 Oct 2026", useUnmergedTree = true).assertExists()
        // The calendar scrolled away with the list's top: back at the top it is still on October.
        compose.onNodeWithTag("workout-list").performScrollToNode(hasTestTag("workout-calendar"))
        compose.onNodeWithText("October 2026").assertExists()
        // Near the end of the log there is not a screenful below a workout: it is brought as far up as the list goes.
        compose.onNodeWithContentDescription("Previous month").performClick()
        compose.onNodeWithContentDescription("Tue 22 Sep 2026: Powerlifting · 1 exercise · 1 set. Show it").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("workout-11", useUnmergedTree = true).assertExists()
    }

    @Test
    fun calendar_monthMaths_andWords() {
        // November 2026 starts on a Sunday and has 30 days: six blanks, then the 1st; six rows.
        val november = monthGrid(LocalDate(2026, 11, 1))
        assertEquals(6, november.size)
        assertEquals(listOf(null, null, null, null, null, null, LocalDate(2026, 11, 1)), november.first())
        assertEquals(listOf(LocalDate(2026, 11, 30), null, null, null, null, null, null), november.last())
        // February 2027 starts on a Monday and has 28 days: exactly four rows, no blanks. A leap February has 29.
        assertEquals((1..28).map { LocalDate(2027, 2, it) }, monthGrid(LocalDate(2027, 2, 1)).flatten())
        assertEquals(29, monthGrid(LocalDate(2028, 2, 1)).flatten().count { it != null })
        // December follows November; January follows December.
        assertEquals(LocalDate(2027, 1, 1), firstOfMonth(monthIndex(LocalDate(2026, 12, 15)) + 1))
        assertEquals(LocalDate(2026, 12, 1), firstOfMonth(monthIndex(LocalDate(2027, 1, 7)) - 1))
        assertEquals("January 2027", monthTitle(LocalDate(2027, 1, 1)))
        assertEquals("1 workout in November", calendarSummary(1, LocalDate(2026, 11, 1), 0))
        assertEquals("12 workouts in January · 1 week in a row", calendarSummary(12, LocalDate(2027, 1, 1), 1))
        assertEquals("0 workouts in November · 2 weeks in a row", calendarSummary(0, LocalDate(2026, 11, 1), 2))
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w360dp-h2400dp-xhdpi")
    fun calendar_on360dp_everyDayAndArrowIsAFullTarget_insideTheScreen() {
        showHistory(workoutsOn("2026-11-02", "2026-11-10", "2026-11-29", "2026-11-30"))
        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        val days = listOf("workout-day-2026-11-02", "workout-day-2026-11-10", "workout-day-2026-11-29", "workout-day-2026-11-30").map { compose.onNodeWithTag(it, useUnmergedTree = true).getUnclippedBoundsInRoot() }
        for (b in days) {
            assertTrue("a day is at least 48 dp wide (${b.right - b.left})", b.right - b.left >= androidx.compose.ui.unit.Dp(47.5f))
            assertTrue("a day is at least 48 dp tall (${b.bottom - b.top})", b.bottom - b.top >= androidx.compose.ui.unit.Dp(47.5f))
            assertTrue("a day is inside the screen", b.left >= screen.left && b.right <= screen.right)
        }
        // Sunday's column ends inside the screen and Monday's starts inside it.
        assertTrue(days[0].left >= screen.left && days[2].right <= screen.right)
        compose.assertTextsFit()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w320dp-h2400dp-xhdpi")
    fun calendar_smallPhoneAndLargeFont_isWhole() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                PowertrackerTheme {
                    WorkoutHistoryContent(WorkoutHistoryState(loading = false, workouts = workoutsOn("2026-11-02", "2026-11-10", "2026-11-29", "2026-11-30", "2026-09-01")), onBack = {}, today = today)
                }
            }
        }
        compose.waitForIdle()
        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        for (tag in listOf("workout-day-2026-11-02", "workout-day-2026-11-30", "day-2026-11-28", "day-2026-11-11")) {
            val b = compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
            assertTrue("$tag is inside the screen", b.left >= screen.left && b.right <= screen.right)
        }
        // The month's name and the line under the grid are whole: every letter on a line that is drawn.
        for (text in listOf("November 2026", "4 workouts in November · 2 weeks in a row")) {
            val node = compose.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            val layout = mutableListOf<androidx.compose.ui.text.TextLayoutResult>().also { node.config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult].action?.invoke(it) }.single()
            assertEquals("\"$text\" is whole", text.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
            assertTrue("\"$text\" is not cut at the bottom", !layout.didOverflowHeight)
        }
        compose.assertTextsFit()
    }

    @Test
    fun oneWorkout_isCountedInTheSingular_andLoadingShowsNeitherACountNorTheEmptyState() {
        compose.setContent { PowertrackerTheme { WorkoutHistoryContent(WorkoutHistoryState(loading = false, workouts = workouts(1)), onBack = {}, today = today) } }
        compose.onNodeWithText("1 WORKOUT", useUnmergedTree = true).assertExists()
    }

    @Test
    fun whileLoading_thereIsNeitherACountNorTheEmptyState() {
        compose.setContent { PowertrackerTheme { WorkoutHistoryContent(WorkoutHistoryState(), onBack = {}, today = today) } }
        compose.onNodeWithText("History").assertIsDisplayed()
        compose.onAllNodesWithText("WORKOUT", substring = true, useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithText("No workouts yet").assertCountEquals(0)
        // Nothing to mark yet: no calendar, as with an empty log.
        compose.onAllNodesWithTag("workout-calendar", useUnmergedTree = true).assertCountEquals(0)
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
                    if (onHistory) WorkoutHistoryContent(WorkoutHistoryState(loading = false, workouts = workouts(3)), onBack = {}, today = today)
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
