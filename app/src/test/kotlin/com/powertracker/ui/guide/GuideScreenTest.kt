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
import com.powertracker.domain.guide.GuideFrame
import com.powertracker.domain.guide.GuideSample
import com.powertracker.domain.guide.GuideScreenId
import com.powertracker.domain.guide.GuideStep
import com.powertracker.domain.guide.GuideTap
import androidx.compose.ui.platform.testTag
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

    /** The step screen 4's picture shows, when more than one page is composed (during a drag). */
    private fun todayStep(): GuideStep? = compose.onAllNodes(hasTestTag("guide-picture")).fetchSemanticsNodes()
        .mapNotNull { it.config.getOrNull(GuidePictureStep) }.singleOrNull { it in Guide.steps(GuideScreenId.TODAY, TrainingMode.POWERLIFTING) }

    private fun pictureFrame(): GuideFrame = compose.onNodeWithTag("guide-picture").fetchSemanticsNode().config[GuidePictureFrame]

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
                clock.holdStep(i)
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
            val phone = with(compose.density) { (PHONE_HEIGHT - SPARE).toPx() }
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
    fun guide_realClock_movesThenHoldsEachStep_andHoldsTheLast() {
        compose.mainClock.autoAdvance = false
        show(clock = TickingGuideClock, startAt = GuideScreenId.TODAY)
        compose.mainClock.advanceTimeByFrame()
        val steps = Guide.steps(GuideScreenId.TODAY, TrainingMode.POWERLIFTING)
        assertEquals(steps[0], pictureStep())
        // Into step 2's move: past its start, before it settles.
        compose.mainClock.advanceTimeBy(Guide.stepStartMs(1) + Guide.MOVE_MS / 2L)
        val moving = pictureFrame()
        assertEquals(1, moving.stepIndex)
        assertTrue("mid-move: ${moving.progress}", moving.progress > 0.0 && moving.progress < 1.0)
        compose.mainClock.advanceTimeBy(Guide.MOVE_MS.toLong())
        assertEquals(GuideFrame(1, steps[1], 1.0, false), pictureFrame())
        compose.mainClock.advanceTimeBy(Guide.doneMs(steps.size) * 2L)
        assertEquals("it holds on the last step", GuideFrame(steps.lastIndex, steps.last(), 1.0, true), pictureFrame())
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

    // ---- the picture moves (Task 2b) ----

    private fun stepIndex(step: GuideStep, mode: TrainingMode = TrainingMode.POWERLIFTING) = Guide.steps(GuideScreenId.TODAY, mode).indexOf(step)

    /** Mid-move the clock's time reaches the picture as a progress between 0 and 1, and the editor draws the domain's in-between weight. */
    @Test
    @Config(qualifiers = "w412dp-h2000dp-xhdpi")
    fun guide_today_numbersRollThroughInBetweenValues() {
        for (mode in TrainingMode.entries) {
            val weight = stepIndex(GuideStep.TODAY_WEIGHT, mode)
            val clock = HeldGuideClock(Guide.stepStartMs(weight) + Guide.MOVE_MS / 2)
            show(mode, clock = clock, startAt = GuideScreenId.TODAY)
            val frame = pictureFrame()
            assertEquals(GuideStep.TODAY_WEIGHT, frame.step)
            assertTrue("$mode: ${frame.progress}", frame.progress > 0.0 && frame.progress < 1.0)
            assertEquals(Guide.frame(GuideScreenId.TODAY, mode, clock.heldMs), frame)
        }
    }

    /** What a move's progress draws: the real editor field shows the in-between weight, strictly between empty and the set's. */
    @Test
    @Config(qualifiers = "w412dp-h2000dp-xhdpi")
    fun guide_today_numbersRollThroughInBetweenValues_asDrawn() {
        var progress by mutableStateOf(0.5)
        var side by mutableStateOf(TrainingMode.POWERLIFTING)
        compose.setContent {
            key(side) {
                PowertrackerTheme(mode = side) {
                    Box(Modifier.requiredSize(PHONE_WIDTH, TALL)) { TodayPicture(side, GuideStep.TODAY_WEIGHT, progress) }
                }
            }
        }
        for (m in TrainingMode.entries) {
            side = m
            for (p in listOf(0.25, 0.5, 0.75)) {
                progress = p
                compose.waitForIdle()
                val kg = GuideSample.todayEditor(m, GuideStep.TODAY_WEIGHT, p).weightKg!!
                assertTrue("$m at $p: $kg kg", kg > 0.0 && kg < GuideSample.today(m).doneWeightKg)
                assertEquals("$m at $p", formatKg(kg), drawnEditor().first)
            }
        }
    }

    /** Between the end of a move and the next step's start the picture is not composed at all: the clock is idle. */
    @Test
    fun guide_whileAStepHolds_nothingRenders() {
        compose.mainClock.autoAdvance = false
        show(clock = TickingGuideClock, startAt = GuideScreenId.TODAY)
        compose.mainClock.advanceTimeByFrame()
        // A frame's slack either side of the clock's own start.
        val slack = 50L
        compose.mainClock.advanceTimeBy(Guide.settledMs(1) + slack)
        assertEquals(GuideFrame(1, Guide.steps(GuideScreenId.TODAY, TrainingMode.POWERLIFTING)[1], 1.0, false), pictureFrame())
        val held = guidePictureCompositions
        compose.mainClock.advanceTimeBy(Guide.stepStartMs(2) - Guide.settledMs(1) - 2 * slack)
        assertEquals("composed while the step held", held, guidePictureCompositions)
        // And it does compose while a step moves: one per frame.
        compose.mainClock.advanceTimeBy(2 * slack + Guide.MOVE_MS / 2L)
        assertTrue("a move draws frames", guidePictureCompositions > held + 3)
    }

    /**
     * A drag that starts on screen 4 pauses its picture where it is (it used to snap back to the first step, and so play
     * backwards); coming back to the page later starts it afresh at its first step, with nothing moving backwards.
     */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun guide_replayOrPageReturn_restartsWithoutPlayingBackwards() {
        show(clock = TickingGuideClock, startAt = GuideScreenId.TODAY)
        compose.mainClock.advanceTimeBy(Guide.doneMs(Guide.steps(GuideScreenId.TODAY, TrainingMode.POWERLIFTING).size) + 500L)
        assertEquals(GuideStep.TODAY_SKIP, todayStep())
        compose.mainClock.autoAdvance = false
        // A drag held part-way: the pager is scrolling, so the clock stands still, on the last step.
        compose.onNodeWithTag("guide-pages").performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(-width / 6f, 0f))
        }
        repeat(10) { compose.mainClock.advanceTimeByFrame() }
        assertEquals("a drag leaves the picture where it was", GuideStep.TODAY_SKIP, todayStep())
        compose.onNodeWithTag("guide-pages").performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertOn(GuideScreenId.TODAY)
        assertEquals(GuideStep.TODAY_SKIP, todayStep())
        // To screen 5 and back: screen 4 shows its first step at once, and stays still on it.
        compose.onNodeWithTag("guide-next").performClick()
        compose.waitForIdle()
        assertOn(GuideScreenId.TARGETS)
        compose.onNodeWithTag("guide-back").performClick()
        compose.mainClock.autoAdvance = false
        var frames = 0
        while (compose.onAllNodes(hasTestTag("guide-title") and hasText(title(GuideScreenId.TODAY))).fetchSemanticsNodes().isEmpty() && frames++ < 120) {
            compose.mainClock.advanceTimeByFrame()
        }
        repeat(30) { compose.mainClock.advanceTimeByFrame() }
        assertOn(GuideScreenId.TODAY)
        assertEquals(GuideStep.TODAY_EMPTY, todayStep())
        val first = drawnPixels()
        repeat(5) { compose.mainClock.advanceTimeByFrame() }
        assertTrue("nothing moves on the first step", first.contentEquals(drawnPixels()))
        assertEquals(GuideStep.TODAY_EMPTY, todayStep())
    }

    /** A lifecycle the test moves by hand. */
    private class TestLifecycle : androidx.lifecycle.LifecycleOwner {
        val registry = androidx.lifecycle.LifecycleRegistry.createUnsafe(this).apply { currentState = androidx.lifecycle.Lifecycle.State.RESUMED }
        override val lifecycle: androidx.lifecycle.Lifecycle get() = registry
    }

    /** While the guide is not on screen (stopped) its clock stands still; started again, it goes on from where it was. */
    @Test
    fun guide_clockStops_whenHiddenOrUnmounted() {
        val owner = TestLifecycle()
        var on by mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalMotion provides true, androidx.lifecycle.compose.LocalLifecycleOwner provides owner) {
                PowertrackerTheme(mode = TrainingMode.POWERLIFTING, darkTheme = true) {
                    if (on) {
                        GuideScreen(
                            TrainingMode.POWERLIFTING, firstLaunch = false, hasPlan = false, motionOn = true,
                            onPickSide = {}, onClose = {}, clock = TickingGuideClock, startAt = GuideScreenId.TODAY,
                        )
                    }
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(Guide.settledMs(1) + 50L)
        assertEquals(1, pictureFrame().stepIndex)
        compose.runOnIdle { owner.registry.currentState = androidx.lifecycle.Lifecycle.State.CREATED }
        compose.mainClock.advanceTimeBy(Guide.STEP_MS * 10L)
        assertEquals("stopped: it stands still", GuideFrame(1, GuideStep.TODAY_WEIGHT, 1.0, false), pictureFrame())
        val stopped = guidePictureCompositions
        compose.runOnIdle { owner.registry.currentState = androidx.lifecycle.Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeBy(Guide.stepStartMs(2) - Guide.settledMs(1) + Guide.MOVE_MS + 100L)
        assertEquals("started again: on from where it was", 2, pictureFrame().stepIndex)
        assertTrue(guidePictureCompositions > stopped)
        // Gone: nothing of it is composed again, however long the clock runs.
        on = false
        compose.mainClock.advanceTimeByFrame()
        val gone = guidePictureCompositions
        compose.mainClock.advanceTimeBy(Guide.STEP_MS * 20L)
        assertEquals(gone, guidePictureCompositions)
    }

    /**
     * The picture is one Today instance through its steps, so the real screen's own motion plays: the frame after the
     * log step starts, with no fed value changing, the set's block is mid-slide (it differs from both the step before
     * and the settled step).
     */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun guide_today_inheritsTheScreensOwnMotion() {
        val clock = HeldGuideClock()
        clock.holdStep(stepIndex(GuideStep.TODAY_RPE))
        show(clock = clock, startAt = GuideScreenId.TODAY)
        compose.mainClock.advanceTimeBy(1000L)
        val before = drawnPixels()
        compose.mainClock.autoAdvance = false
        // The log step, at its settled time: the editor's values are the step's own from the first frame.
        compose.runOnUiThread { clock.holdStep(stepIndex(GuideStep.TODAY_TICKED)) }
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        repeat(3) { compose.mainClock.advanceTimeByFrame() }
        assertEquals(GuideStep.TODAY_TICKED, pictureStep())
        val mid = drawnPixels()
        compose.mainClock.advanceTimeBy(1000L)
        val settled = drawnPixels()
        assertTrue("the log step changed nothing", !settled.contentEquals(before))
        assertTrue("mid-slide on the next frames: the drawn screen is already settled", !mid.contentEquals(settled))
        assertTrue("mid-slide: the drawn screen has not changed yet", !mid.contentEquals(before))
    }

    /** The screen as drawn now, in root pixels. */
    private fun drawnBitmap(): Bitmap {
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun guide_ring_onLogSet_justBeforeTheSetIsLogged_onTheGuidesClock() {
        val steps = Guide.steps(GuideScreenId.TODAY, TrainingMode.POWERLIFTING)
        val ticked = stepIndex(GuideStep.TODAY_TICKED)
        val ringFrom = Guide.motionStartMs(steps, ticked)
        val clock = HeldGuideClock()
        clock.holdStep(ticked - 1)
        // First the screen drawn on its own, its semantics there to be read: where Log set is on the phone. Then the guide.
        var alone by mutableStateOf(true)
        compose.setContent {
            CompositionLocalProvider(LocalMotion provides true) {
                PowertrackerTheme(mode = TrainingMode.POWERLIFTING, darkTheme = true) {
                    if (alone) {
                        Box(Modifier.requiredSize(PHONE_WIDTH, PHONE_HEIGHT).testTag("phone")) { TodayPicture(TrainingMode.POWERLIFTING, steps[ticked - 1]) }
                    } else {
                        GuideScreen(TrainingMode.POWERLIFTING, firstLaunch = false, hasPlan = false, motionOn = true, onPickSide = {}, onClose = {}, clock = clock, startAt = GuideScreenId.TODAY)
                    }
                }
            }
        }
        compose.waitForIdle()
        val phone = compose.onNodeWithTag("phone").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithText("Log set 1").fetchSemanticsNode().boundsInRoot.translate(-phone.left, -phone.top)
        alone = false
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        fun at(ms: Int) {
            compose.runOnUiThread { clock.heldMs = ms }
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            compose.mainClock.advanceTimeByFrame()
            compose.mainClock.advanceTimeByFrame()
        }
        fun tapBox() = compose.onNodeWithTag("guide-picture").fetchSemanticsNode().config.getOrNull(GuideTapBox)
        at(ringFrom - 20)
        assertNull(pictureFrame().tap)
        assertNull(tapBox())
        val before = drawnBitmap()
        // Half-way through the ring: still the step before; the ring on Log set.
        at(ringFrom + Guide.TAP_MS / 2)
        assertEquals(steps[ticked - 1], pictureFrame().step)
        assertEquals(GuideTap.LOG_SET, pictureFrame().tap)
        val box = tapBox()!!
        assertTrue("$box against $button", kotlin.math.abs(box.center.x - button.center.x) < 1.5f && kotlin.math.abs(box.center.y - button.center.y) < 1.5f)
        val ringing = drawnBitmap()
        // Drawn: the picture differs from just before, around the button only.
        val picture = compose.onNodeWithTag("guide-picture").fetchSemanticsNode().boundsInRoot
        val scale = picture.width / phone.width
        val cx = picture.left + box.center.x * scale
        val cy = picture.top + box.center.y * scale
        // The ring at its largest reaches 7 dp × 4.2 from its centre, shrunk with the phone.
        val reach = 30f * compose.density.density * scale
        var changed = 0
        var far = 0
        for (y in picture.top.toInt() until picture.bottom.toInt()) for (x in picture.left.toInt() until picture.right.toInt()) {
            if (before.getPixel(x, y) == ringing.getPixel(x, y)) continue
            changed++
            if (kotlin.math.hypot(x - cx, y - cy) > reach) far++
        }
        assertTrue("the ring is drawn: $changed", changed > 20)
        assertEquals("nothing else changes", 0, far)
        // Held, it holds: the frame clock goes on, the ring does not.
        compose.mainClock.advanceTimeBy(500)
        assertTrue(ringing.sameAs(drawnBitmap()))
        // It ends as the result appears.
        at(Guide.stepStartMs(ticked))
        assertEquals(GuideStep.TODAY_TICKED, pictureFrame().step)
        assertNull(pictureFrame().tap)
        assertNull(tapBox())
    }

    @Test
    fun guide_ring_withMotionOff_isNotThere() {
        show(motionOn = false, clock = HeldGuideClock(), startAt = GuideScreenId.TODAY)
        assertNull(pictureFrame().tap)
        assertNull(compose.onNodeWithTag("guide-picture").fetchSemanticsNode().config.getOrNull(GuideTapBox))
    }

    /**
     * For the owner (GUIDE_FRAMES=<dir>): powerlifting dark, every 40 ms (GUIDE_FRAME_MS) from just before the tap ring
     * across the log step and the skip step, from one live composition, the held guide clock and the frame clock moving
     * together.
     */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun guide_today_frameStrip() {
        val dir = System.getenv("GUIDE_FRAMES") ?: return
        // Every this many ms (GUIDE_FRAME_MS; 40 by default: close enough to show the ring and the tick mid-draw).
        val every = System.getenv("GUIDE_FRAME_MS")?.toInt() ?: 40
        java.io.File(dir).mkdirs()
        val steps = Guide.steps(GuideScreenId.TODAY, TrainingMode.POWERLIFTING)
        val ticked = stepIndex(GuideStep.TODAY_TICKED)
        val skip = stepIndex(GuideStep.TODAY_SKIP)
        val clock = HeldGuideClock()
        // From just before the ring on Log set.
        clock.holdStep(ticked - 1)
        show(clock = clock, startAt = GuideScreenId.TODAY)
        compose.mainClock.advanceTimeBy(1000L)
        compose.mainClock.autoAdvance = false
        var t = Guide.motionStartMs(steps, ticked) - every
        compose.runOnUiThread { clock.heldMs = t }
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeBy(every.toLong())
        var n = 0
        val end = Guide.settledMs(skip) + 400
        while (t <= end) {
            val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            java.io.File(dir, "android-today-powerlifting-dark-%02d-%05dms-%s.png".format(n++, t, pictureStep()!!.name.lowercase())).outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            // The steps between the two are skipped over: from the log step's settling to the skip step's start.
            t = if (t in Guide.settledMs(ticked) + 400 until Guide.stepStartMs(skip) - every) Guide.stepStartMs(skip) - every else t + every
            compose.runOnUiThread { clock.heldMs = t }
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            compose.mainClock.advanceTimeBy(every.toLong())
        }
    }
}
