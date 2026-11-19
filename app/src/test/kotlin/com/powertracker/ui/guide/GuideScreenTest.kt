package com.powertracker.ui.guide

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.platform.ViewRootForTest
import android.graphics.Canvas
import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.input.key.Key
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.onNodeWithText
import com.powertracker.domain.guide.Guide
import com.powertracker.domain.guide.GuideSample
import com.powertracker.domain.guide.GuideScreenId
import com.powertracker.domain.guide.GuideStep
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.components.formatKg
import com.powertracker.ui.components.formatRpe
import com.powertracker.ui.today.SKIPPED_TODAY
import com.powertracker.ui.theme.LocalMotion
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.TodayActions
import com.powertracker.ui.today.TodayContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The guide's shell and its Today picture (1.16, Task 2). Copy is read from the domain, never typed here. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w412dp-h915dp-xhdpi")
class GuideScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val exits = mutableListOf<GuideExit>()

    private fun show(
        mode: TrainingMode = TrainingMode.POWERLIFTING,
        motionOn: Boolean = true,
        clock: GuideClock = HeldGuideClock(),
        startAt: GuideScreenId = GuideScreenId.WELCOME,
    ) {
        // The rule takes one setContent per test: a later show() swaps in a fresh guide with the new arguments.
        val first = shown == null
        shown = Shown(mode, motionOn, clock, startAt)
        if (first) {
            compose.setContent {
                val s = shown ?: return@setContent
                key(s) {
                    CompositionLocalProvider(LocalMotion provides s.motionOn) {
                        PowertrackerTheme(mode = s.mode, darkTheme = true) {
                            GuideScreen(
                                s.mode, firstLaunch = false, hasPlan = false, motionOn = s.motionOn,
                                onPickSide = {}, onClose = { exits += it }, clock = s.clock, startAt = s.startAt,
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private class Shown(val mode: TrainingMode, val motionOn: Boolean, val clock: GuideClock, val startAt: GuideScreenId)

    private var shown by mutableStateOf<Shown?>(null)

    private fun title(id: GuideScreenId, mode: TrainingMode = TrainingMode.POWERLIFTING) = Guide.screen(id, mode).title

    private fun assertOn(id: GuideScreenId, mode: TrainingMode = TrainingMode.POWERLIFTING) {
        compose.onNodeWithTag("guide-title").assertTextEquals(title(id, mode))
        val index = Guide.SCREENS.indexOf(id)
        compose.onNodeWithTag("guide-dots").assertContentDescription(dotsLabel(index, Guide.SCREENS.size))
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertContentDescription(expected: String) {
        assertEquals(listOf(expected), fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription))
    }

    /** A screen taller than any phone the pictures use, so nothing on it is cut. */
    private val TALL = 1900.dp

    /** A wrapped line of the Today list's text, left free at the phone's foot. */
    private val SPARE = 24.dp

    private fun pictureStep(): GuideStep? = compose.onNodeWithTag("guide-picture").fetchSemanticsNode().config.getOrNull(GuidePictureStep)

    @Test
    fun guide_showsEightDots_andMovesWithNextBackAndSwipe() {
        show()
        assertEquals(8, Guide.SCREENS.size)
        // Eight dots drawn, one description for them all.
        compose.onAllNodes(hasTestTag("guide-dot") and hasAnyAncestor(hasTestTag("guide-dots")), useUnmergedTree = true).assertCountEquals(Guide.SCREENS.size)
        compose.onNodeWithTag("guide-dots").onChildren().assertCountEquals(0)
        assertOn(GuideScreenId.WELCOME)
        compose.onNodeWithTag("guide-next").performClick()
        compose.waitForIdle()
        assertOn(GuideScreenId.SIDE)
        compose.onNodeWithTag("guide-next").performClick()
        compose.waitForIdle()
        assertOn(GuideScreenId.PLAN)
        compose.onNodeWithTag("guide-back").performClick()
        compose.waitForIdle()
        assertOn(GuideScreenId.SIDE)
        // A swipe that starts on the picture still turns the page: the picture blocks its own taps, not the pager's.
        compose.onNodeWithTag("guide-picture").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertOn(GuideScreenId.PLAN)
        compose.onNodeWithTag("guide").performTouchInput { swipeRight() }
        compose.waitForIdle()
        assertOn(GuideScreenId.SIDE)
        assertTrue("moving through the guide closes nothing", exits.isEmpty())
    }

    @Test
    fun guide_skipIsOnEveryScreen_andClosesWithSkipped() {
        show()
        for ((i, id) in Guide.SCREENS.withIndex()) {
            assertOn(id)
            compose.onNodeWithTag("guide-skip").assertIsDisplayed()
            if (i < Guide.SCREENS.lastIndex) {
                compose.onNodeWithTag("guide-next").performClick()
                compose.waitForIdle()
            }
        }
        assertTrue(exits.isEmpty())
        compose.onNodeWithTag("guide-skip").performClick()
        compose.waitForIdle()
        assertEquals(listOf(GuideExit.SKIPPED), exits)
    }

    @Test
    fun guide_systemBack_closesWithSkipped() {
        show(startAt = GuideScreenId.TODAY)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertEquals(listOf(GuideExit.SKIPPED), exits)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun guide_screen4_stepsThroughLoggingASet_onAHeldClock() {
        for (mode in TrainingMode.entries) {
            exits.clear()
            val clock = HeldGuideClock()
            show(mode, clock = clock, startAt = GuideScreenId.TODAY)
            assertOn(GuideScreenId.TODAY, mode)
            val steps = Guide.steps(GuideScreenId.TODAY, mode)
            assertEquals(GuideStep.TODAY_EMPTY, pictureStep())
            var before: IntArray? = null
            for ((i, step) in steps.withIndex()) {
                clock.held = i
                compose.waitForIdle()
                assertEquals("$mode step $i", step, pictureStep())
                // Each step draws something new in the guide's own picture (what it draws is pinned below, step by step).
                val now = drawnPixels()
                if (before != null) assertTrue("$mode $step draws the same as the step before", !now.contentEquals(before))
                before = now
            }
            assertEquals(GuideStep.TODAY_SKIP, pictureStep())
        }
    }

    /** The screen as drawn (native graphics): the Compose root view into a bitmap, as the screenshots are made. */
    private fun drawnPixels(): IntArray {
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
    }

    /** What the lift's editor shows, as drawn: its weight, reps and RPE fields (the first card's: the lift is first). */
    private fun drawnEditor(): Triple<String, String, String> {
        fun field(description: String) =
            compose.onAllNodesWithContentDescription(description)[0].fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        return Triple(field("Weight, kilograms"), field("Reps"), field("RPE"))
    }

    /** What is drawn at each step: the lift's editor fills field by field, then the set is ticked, then Skip today. */
    @Test
    @Config(qualifiers = "w412dp-h2000dp-xhdpi")
    fun guide_screen4_eachStepDrawsItsState() {
        var step by mutableStateOf(GuideStep.TODAY_EMPTY)
        var side by mutableStateOf(TrainingMode.POWERLIFTING)
        compose.setContent {
            key(side) {
                PowertrackerTheme(mode = side) {
                    Box(Modifier.requiredSize(PHONE_WIDTH, TALL)) { TodayPicture(side, step) }
                }
            }
        }
        for (m in TrainingMode.entries) {
            side = m
            val steps = Guide.steps(GuideScreenId.TODAY, m)
            val sample = GuideSample.today(m)
            val item = liftItem(m)
            val done = "Set 1: ${formatKg(sample.doneWeightKg)} kg × ${sample.doneReps} @ ${formatRpe(sample.doneRpe)}, done"
            for ((i, s) in steps.withIndex()) {
                step = s
                compose.waitForIdle()
                fun at(x: GuideStep) = i >= steps.indexOf(x)
                assertEquals(
                    "$m $s",
                    Triple(
                        if (at(GuideStep.TODAY_WEIGHT)) formatKg(sample.doneWeightKg) else "",
                        (if (at(GuideStep.TODAY_REPS)) sample.doneReps else item.reps).toString(),
                        formatRpe(if (at(GuideStep.TODAY_RPE)) sample.doneRpe else item.targetRpe),
                    ),
                    drawnEditor(),
                )
                assertEquals("$m $s: the ticked set", at(GuideStep.TODAY_TICKED), compose.onAllNodes(hasText(done)).fetchSemanticsNodes().isNotEmpty())
                assertEquals("$m $s: Skipped today", at(GuideStep.TODAY_SKIP), compose.onAllNodesWithText(SKIPPED_TODAY).fetchSemanticsNodes().isNotEmpty())
            }
        }
    }

    /** The weight, reps and RPE steps each change what the editor shows: the sample's set is not the editor's start. */
    @Test
    @Config(qualifiers = "w412dp-h2000dp-xhdpi")
    fun guide_screen4_theWeightRepsAndRpeSteps_eachChangeWhatIsDrawn() {
        var step by mutableStateOf(GuideStep.TODAY_EMPTY)
        var side by mutableStateOf(TrainingMode.POWERLIFTING)
        compose.setContent {
            key(side) {
                PowertrackerTheme(mode = side) {
                    Box(Modifier.requiredSize(PHONE_WIDTH, TALL)) { TodayPicture(side, step) }
                }
            }
        }
        for (m in TrainingMode.entries) {
            side = m
            fun drawnAt(s: GuideStep): Triple<String, String, String> {
                step = s
                compose.waitForIdle()
                return drawnEditor()
            }
            val empty = drawnAt(GuideStep.TODAY_EMPTY)
            val weight = drawnAt(GuideStep.TODAY_WEIGHT)
            val reps = drawnAt(GuideStep.TODAY_REPS)
            val rpe = drawnAt(GuideStep.TODAY_RPE)
            assertTrue("$m: the weight step", weight.first != empty.first)
            assertTrue("$m: the reps step", reps.second != weight.second)
            assertTrue("$m: the RPE step", rpe.third != reps.third)
        }
    }

    /** The plan item of the lift screen 4 logs, on the day it draws. */
    private fun liftItem(mode: TrainingMode) = GuideSample.todayItems(mode).single {
        GuideSample.exercises(mode).getValue(it.exerciseId).name == GuideSample.today(mode).exerciseId
    }

    /** What the picture draws at each step: the real TodayContent with the guide's sample state. */
    @Test
    fun guide_screen4_states_fillTheSetRow_thenSkip() {
        for (mode in TrainingMode.entries) {
            val sample = GuideSample.today(mode)
            val steps = Guide.steps(GuideScreenId.TODAY, mode)
            val first = GuideSampleState.today(mode, steps.first())
            val lift = first.items.first()
            assertEquals(sample.exerciseId, lift.exercise.name)
            assertTrue("step 0: no set logged", lift.sets.isEmpty())
            assertNull("step 0: no weight", lift.editor.weightKg)
            assertTrue(first.skippedToday.isEmpty())

            val ticked = GuideSampleState.today(mode, GuideStep.TODAY_TICKED).items.first()
            assertEquals(sample.doneWeightKg, ticked.editor.weightKg)
            assertEquals(sample.doneReps, ticked.editor.reps)
            assertEquals(sample.doneRpe, ticked.editor.rpe, 0.0)
            val set = ticked.sets.single().set
            assertEquals(sample.doneWeightKg, set.weightKg, 0.0)
            assertEquals(sample.doneReps, set.reps)
            assertEquals(sample.doneRpe, set.rpe)

            val last = GuideSampleState.today(mode, steps.last())
            assertEquals(1, last.skippedToday.size)
            assertTrue(last.items.none { it.exercise.id == last.skippedToday.single().exerciseId })
            assertEquals(1, last.items.first().sets.size)
        }
    }

    /**
     * On the picture's own phone, each side's last step shows both the ticked set and the whole "Skipped today" line,
     * with a wrapped line to spare. Measured with the real fonts (native graphics), on a screen tall enough not to cut
     * the list, so the line's natural place is what is compared with the phone's foot.
     */
    @Test
    @Config(qualifiers = "w412dp-h2000dp-xhdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun guide_screen4_lastState_drawsTheTickAndTheSkippedLine() {
        var mode by mutableStateOf(TrainingMode.POWERLIFTING)
        compose.setContent {
            key(mode) {
                PowertrackerTheme(mode = mode) {
                    Box(Modifier.requiredSize(PHONE_WIDTH, TALL)) { TodayPicture(mode, Guide.steps(GuideScreenId.TODAY, mode).last()) }
                }
            }
        }
        for (side in TrainingMode.entries) {
            mode = side
            compose.waitForIdle()
            val state = GuideSampleState.today(side, Guide.steps(GuideScreenId.TODAY, side).last())
            val sample = GuideSample.today(side)
            // The set row as the real SetRow says it: the sample's numbers, ticked done.
            compose.onNode(hasText("Set 1: ${formatKg(sample.doneWeightKg)} kg × ${sample.doneReps} @ ${formatRpe(sample.doneRpe)}, done")).assertIsDisplayed()
            compose.onNodeWithTag("skipped-today").assertIsDisplayed()
            compose.onNodeWithText(SKIPPED_TODAY).assertIsDisplayed()
            compose.onNodeWithText(state.skippedToday.single().name).assertIsDisplayed()
            // Wholly on the phone, with room for one more wrapped line: not cut at its foot.
            val bottom = compose.onNodeWithTag("skipped-today").fetchSemanticsNode().boundsInRoot.bottom
            val phone = with(compose.density) { (phoneHeight(side) - SPARE).toPx() }
            assertTrue("$side: the skipped line ends at $bottom px, the phone (less a line) at $phone px", bottom <= phone)
        }
    }

    @Test
    fun guide_reduceMotion_showsTheLastStepStill() {
        for (mode in TrainingMode.entries) {
            // The real clock, with motion off: the last step at once, and it stays.
            show(mode, motionOn = false, clock = TickingGuideClock, startAt = GuideScreenId.TODAY)
            assertEquals(Guide.steps(GuideScreenId.TODAY, mode).last(), pictureStep())
            compose.mainClock.advanceTimeBy(Guide.STEP_MS * 10L)
            compose.waitForIdle()
            assertEquals(Guide.steps(GuideScreenId.TODAY, mode).last(), pictureStep())
        }
    }

    @Test
    fun guide_realClock_advancesOneStepEveryStepMs() {
        compose.mainClock.autoAdvance = false
        show(clock = TickingGuideClock, startAt = GuideScreenId.TODAY)
        compose.mainClock.advanceTimeByFrame()
        val steps = Guide.steps(GuideScreenId.TODAY, TrainingMode.POWERLIFTING)
        assertEquals(steps[0], pictureStep())
        compose.mainClock.advanceTimeBy(Guide.STEP_MS.toLong())
        assertEquals(steps[1], pictureStep())
        compose.mainClock.advanceTimeBy(Guide.STEP_MS * 10L)
        assertEquals("it holds on the last step", steps.last(), pictureStep())
    }

    @Test
    fun guide_picturesShowingNumbers_areLabelledExample() {
        for (mode in TrainingMode.entries) {
            show(mode)
            for ((i, id) in Guide.SCREENS.withIndex()) {
                assertOn(id, mode)
                val labelled = compose.onAllNodes(hasTestTag("guide-example")).fetchSemanticsNodes().isNotEmpty()
                assertEquals("$mode $id", Guide.screen(id, mode).example, labelled)
                if (labelled) compose.onNode(hasText(EXAMPLE) and hasAnyAncestor(hasTestTag("guide-example"))).assertExists()
                if (i < Guide.SCREENS.lastIndex) {
                    compose.onNodeWithTag("guide-next").performClick()
                    compose.waitForIdle()
                }
            }
        }
    }

    @Test
    fun guide_pictureIsNotInteractive() {
        show(startAt = GuideScreenId.TODAY, clock = HeldGuideClock(0))
        val picture = compose.onNodeWithTag("guide-picture")
        // One description, nothing beneath it: no buttons, fields or text from the Today screen reach the accessibility tree.
        picture.assertContentDescription(pictureDescription(GuideScreenId.TODAY))
        picture.onChildren().assertCountEquals(0)
        assertNull(picture.fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick))
        compose.onAllNodes(hasText("Log set 1") and hasAnyAncestor(hasTestTag("guide"))).assertCountEquals(0)
        // Taps all over the picture (its steppers, Log set, the ⓘ and ⋮ buttons where they are drawn) change nothing and
        // open nothing: a guide sheet or menu would be a window of its own, a second root.
        for (x in 1..9) for (y in 1..9) {
            picture.performTouchInput { click(androidx.compose.ui.geometry.Offset(width * x / 10f, height * y / 10f)) }
        }
        compose.waitForIdle()
        compose.onAllNodes(isRoot()).assertCountEquals(1)
        assertEquals(GuideStep.TODAY_EMPTY, pictureStep())
        assertOn(GuideScreenId.TODAY)
        assertTrue(exits.isEmpty())
    }
    /** The focused control's tag; fails if the focus is on nothing the guide shows (inside the picture, say). */
    private fun focusedTag(move: String): String {
        val focused = compose.onAllNodes(isFocused()).fetchSemanticsNodes()
        assertEquals("$move: one focused control of the guide's", 1, focused.size)
        return focused.single().config.getOrNull(SemanticsProperties.TestTag) ?: "untagged"
    }

    /**
     * The keyboard moves through the guide's own controls: Tab from Skip reaches Back and Next, Shift+Tab comes back,
     * and the focus never goes into the picture's fields. Screen 1, with no picture drawn, is the control.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun guide_tabFromSkip_reachesBackAndNext_andNeverEntersThePicture() {
        val controls = setOf("guide-skip", "guide-back", "guide-next")
        for (start in listOf(GuideScreenId.WELCOME, GuideScreenId.TODAY)) {
            show(startAt = start, clock = HeldGuideClock(0))
            compose.onNodeWithTag("guide-skip").requestFocus()
            compose.waitForIdle()
            val forward = (1..4).map { i ->
                compose.onRoot().performKeyInput { pressKey(Key.Tab) }
                compose.waitForIdle()
                focusedTag("$start Tab $i")
            }
            assertTrue("$start: Tab went to $forward", forward.all { it in controls })
            assertTrue("$start: Tab reaches Next ($forward)", "guide-next" in forward)
            if (start != GuideScreenId.WELCOME) assertTrue("$start: Tab reaches Back ($forward)", "guide-back" in forward)
            compose.onNodeWithTag("guide-next").requestFocus()
            compose.waitForIdle()
            val backward = (1..4).map { i ->
                compose.onRoot().performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
                compose.waitForIdle()
                focusedTag("$start Shift+Tab $i")
            }
            assertTrue("$start: Shift+Tab went to $backward", backward.all { it in controls })
            assertTrue("$start: Shift+Tab reaches Skip ($backward)", "guide-skip" in backward)
            assertOn(start)
        }
    }

    /** A page change is announced: the guide is a pane titled with the screen shown, outside the pager. */
    @Test
    fun guide_pageChange_isAnnounced_byThePanesTitle() {
        show()
        fun pane() = compose.onNodeWithTag("guide").fetchSemanticsNode().config.getOrNull(SemanticsProperties.PaneTitle)
        assertEquals(title(GuideScreenId.WELCOME), pane())
        compose.onNodeWithTag("guide-next").performClick()
        compose.waitForIdle()
        assertEquals(title(GuideScreenId.SIDE), pane())
    }

    /** Opening the guide works out screen 1 only: none of the sample is read until a picture that shows it is reached. */
    @Test
    fun guide_opening_readsNoSample() {
        val before = GuideSample.reads
        show()
        assertOn(GuideScreenId.WELCOME)
        assertEquals("opening the guide read the sample", before, GuideSample.reads)
        // The counter does count: reaching screen 4 reads it.
        repeat(Guide.SCREENS.indexOf(GuideScreenId.TODAY)) {
            compose.onNodeWithTag("guide-next").performClick()
            compose.waitForIdle()
        }
        assertOn(GuideScreenId.TODAY)
        assertTrue(GuideSample.reads > before)
    }
}
