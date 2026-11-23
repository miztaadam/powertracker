package com.powertracker.ui

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.StoredSet
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.plates.PlateLoader
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.design.assertTextsFit
import com.powertracker.ui.library.rpeSheetIsOpen
import com.powertracker.ui.plan.PlanScreen
import com.powertracker.ui.plan.SettingsContent
import com.powertracker.ui.progress.ChartRange
import com.powertracker.ui.progress.ProgressContent
import com.powertracker.ui.progress.ProgressViewModel
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.SetEditor
import com.powertracker.ui.today.TodayActions
import com.powertracker.ui.today.TodayContent
import com.powertracker.ui.today.TodayItem
import com.powertracker.ui.today.TodayState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Strict press, its switch and the RPE info button on the smallest phone the app supports (360 dp):
 * nothing is cut off or runs off the screen, and every target is at least 48 dp.
 */
// Native graphics, so text is measured at its real size: the layout checks here depend on it.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h2400dp-xhdpi")
class StrictPressSmallPhoneTest {
    /** Outermost: the database closes after the compose rule has torn the screen down, and once no query is running. */
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday

    private val squat = Exercise(1, "Squat", Category.SQUAT, null, emptyList(), isBuiltIn = true)
    private val bench = Exercise(2, "Bench", Category.BENCH, null, emptyList(), isBuiltIn = true)
    private val deadlift = Exercise(3, "Deadlift", Category.DEADLIFT, null, emptyList(), isBuiltIn = true)
    private val press = Exercise(4, "Strict press", Category.STRICT_PRESS, null, emptyList(), isBuiltIn = true)

    private fun screenWidth() = compose.onRoot().getUnclippedBoundsInRoot().right

    @Test
    fun progress_withStrictPress_fitsASmallPhone() {
        // Wide figures: every lift at a half kilo, six weeks apart so each has a four-week change.
        fun day(daysAgo: Int, kg: List<Double>) = LoggedSession(
            daysAgo.toLong(), today.minus(daysAgo, DateTimeUnit.DAY), 102.5, null,
            listOf(squat, bench, deadlift, press).zip(kg).map { (e, w) -> LoggedSet(e.id, w, 1, 10.0, false, null, isTopSet = false) },
        )
        val history = TrainingHistory(
            listOf(day(42, listOf(222.5, 152.5, 262.5, 102.5)), day(2, listOf(237.5, 157.5, 272.5, 107.5))),
            listOf(squat, bench, deadlift, press).associateBy { it.id },
            emptyList(),
        )
        val state = ProgressViewModel.build(history, ChartRange.MONTHS_6, today, showStrictPress = true)
        compose.setContent { PowertrackerTheme { ProgressContent(state, onRange = {}, onOpenLift = {}) } }

        compose.assertTextsFit()
        compose.onNodeWithText("Total (S+B+D+P)", ignoreCase = true).assertExists()
        // The captions under the volume bars stay on one line each, side by side.
        val from = compose.onNodeWithText("Week of", substring = true).getUnclippedBoundsInRoot()
        val to = compose.onNodeWithText("This week").getUnclippedBoundsInRoot()
        assertTrue("the volume captions do not wrap or meet", from.right < to.left && to.bottom - to.top < 20.dp && from.bottom - from.top < 20.dp)
        val tiles = listOf("Squat", "Bench", "Deadlift", "Strict press").map { lift ->
            compose.onNodeWithContentDescription("$lift history", substring = true).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).getUnclippedBoundsInRoot()
        }
        tiles.forEach { assertTrue("a lift tile stays on the screen", it.left >= 0.dp && it.right <= screenWidth()) }
        // No tile lies over another.
        for (a in tiles) for (b in tiles) if (a !== b) assertTrue(a.right <= b.left || b.right <= a.left || a.bottom <= b.top || b.bottom <= a.top)
        // The four legend entries are all there with their figures.
        for (name in listOf("Squat", "Bench", "Deadlift", "Strict press")) compose.onNodeWithText(name).assertExists()
        assertTrue(compose.onAllNodesWithText("107.5").fetchSemanticsNodes().isNotEmpty())
    }

    private fun pressItem(): TodayItem {
        val missed = StoredSet(1, 1, LoggedSet(press.id, 62.5, 3, 10.0, failed = true, missPoint = null, isTopSet = false))
        return TodayItem(
            exercise = press,
            planned = PlanItem(1, DayOfWeek.WEDNESDAY, press.id, 3, 5, 8.0, 0),
            suggestion = null,
            sets = listOf(missed),
            editor = SetEditor(62.5, 5, 8.0),
            targetSets = 3,
            perSide = PlateLoader.perSide(62.5, changePlates = false),
            lastWeightKg = 62.5,
        )
    }

    @Test
    fun today_strictPressMissRowAndRpeInfo_fitASmallPhone() {
        compose.setContent {
            PowertrackerTheme { TodayContent(TodayState(today, loading = false, sessionId = 1, items = listOf(pressItem())), TodayActions()) }
        }

        compose.assertTextsFit()
        val inRow = hasAnyAncestor(hasTestTag("miss-row-1"))
        val segments = listOf("Off the shoulders", "Mid", "Lockout").map { compose.onNode(hasText(it) and inRow).getUnclippedBoundsInRoot() }
        segments.zipWithNext { a, b -> assertTrue("the miss points sit side by side", a.right <= b.left) }
        assertTrue(segments.first().left >= 0.dp && segments.last().right <= screenWidth())

        // The info button is a full-size target beside the RPE label, clear of the fields and their buttons.
        val info = compose.onNodeWithContentDescription("What RPE means")
        info.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        val bounds = info.getUnclippedBoundsInRoot()
        assertTrue("the info button stays on the screen", bounds.left >= 0.dp && bounds.right <= screenWidth())
        val fields = listOf("Weight, kilograms", "Reps", "RPE").map { compose.onNodeWithContentDescription(it).getUnclippedBoundsInRoot() }
        fields.zipWithNext { a, b -> assertTrue("fields do not run into each other", a.right <= b.left) }
        // The three figures stay on one line: the button adds no height to its column.
        assertEquals(fields[0].top, fields[2].top)
        assertEquals(fields[0].bottom, fields[2].bottom)
        assertTrue(bounds.left >= fields[1].right)
        // It is centred on the caption line. Its target reaches at most the strip above the digits (10 dp of the figure's 53).
        assertTrue("the info target barely reaches the figure", bounds.bottom - fields[2].top <= 10.dp)
        val steppers = listOf("Decrease RPE", "Increase RPE", "Decrease Reps", "Increase Reps").map { compose.onNodeWithContentDescription(it).getUnclippedBoundsInRoot() }
        steppers.forEach { assertTrue("the info target is clear of the step buttons", bounds.bottom <= it.top) }
        // Nor does it reach the set rows above the editor.
        assertTrue(bounds.top >= segments.maxOf { it.bottom })

        // All of it takes the tap, out to each edge of the round target: above the caption, over the figure's top, left and right.
        val edges: List<(Float, Float) -> Offset> = listOf(
            { w, _ -> Offset(w / 2, 2f) },
            { w, h -> Offset(w / 2, h - 2f) },
            { _, h -> Offset(2f, h / 2) },
            { w, h -> Offset(w - 2f, h / 2) },
        )
        for (edge in edges) {
            info.performTouchInput { click(edge(width.toFloat(), height.toFloat())) }
            compose.waitUntil(5_000) { compose.rpeSheetIsOpen() }
            compose.onNodeWithContentDescription("Close").performClick()
            compose.waitUntil(5_000) { !compose.rpeSheetIsOpen() }
        }
    }

    @Test
    fun settings_strictPressSwitchAndRpeInfo_fitASmallPhone() {
        var settings = AppSettings()
        compose.setContent {
            PowertrackerTheme { SettingsContent(settings, onUpdate = { settings = it(settings) }, onExport = {}, onRestore = {}, onBack = {}) }
        }

        compose.assertTextsFit()
        compose.onNodeWithText("Show it on Progress and add it to your total.").assertExists()
        val row = compose.onNode(hasText("Strict press") and hasText("Show it on Progress and add it to your total."))
        row.assertHeightIsAtLeast(48.dp)
        row.performScrollTo().performClick()
        assertTrue(settings.showStrictPress)

        val info = compose.onNodeWithContentDescription("What RPE means")
        info.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        val bounds = info.getUnclippedBoundsInRoot()
        val minus = compose.onNodeWithContentDescription("Decrease Default RPE").getUnclippedBoundsInRoot()
        assertTrue("the info button is clear of the stepper", bounds.right <= minus.left)
        assertTrue(bounds.left >= 0.dp)
        // "Default RPE" stays on one line.
        assertTrue(compose.onNodeWithText("Default RPE").getUnclippedBoundsInRoot().let { it.bottom - it.top } < 30.dp)
    }

    @Test
    fun planDialog_rpeInfo_fitsASmallPhone() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = database.db
        val training = TrainingRepository(db)
        val settings = SettingsRepository(context)
        runBlocking {
            settings.update { AppSettings() }
            val id = training.exercises().first().first { it.name == "Squat" }.id
            training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, id, sets = 5, reps = 5, targetRpe = 8.5, order = 0))
        }
        compose.setContent { PowertrackerTheme { PlanScreen(training, settings, onOpenSettings = {}) } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Squat  5 × 5 @ 8.5").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithContentDescription("More")[0].performClick()
        compose.onNodeWithText("Edit").performClick()
        compose.onNodeWithText("Edit Wednesday").assertExists()

        val inDialog = hasAnyAncestor(isDialog())
        val info = compose.onNode(hasContentDescription("What RPE means") and inDialog)
        info.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        val bounds = info.getUnclippedBoundsInRoot()
        val minus = compose.onNode(hasContentDescription("Decrease RPE") and inDialog).getUnclippedBoundsInRoot()
        val plus = compose.onNode(hasContentDescription("Increase RPE") and inDialog).getUnclippedBoundsInRoot()
        val dialog = compose.onNode(isDialog()).getUnclippedBoundsInRoot()
        assertTrue("the info button is clear of the stepper", bounds.right <= minus.left)
        assertTrue("the row stays inside the dialog", bounds.left >= 0.dp && plus.right <= dialog.right - dialog.left)
    }
}
