package com.powertracker.ui.guide

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
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

    private fun pictureStep(): GuideStep? = compose.onNodeWithTag("guide-picture").fetchSemanticsNode().config.getOrNull(GuidePictureStep)

    @Test
    fun guide_showsEightDots_andMovesWithNextBackAndSwipe() {
        show()
        assertEquals(8, Guide.SCREENS.size)
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
    fun guide_screen4_stepsThroughLoggingASet_onAHeldClock() {
        for (mode in TrainingMode.entries) {
            exits.clear()
            val clock = HeldGuideClock()
            show(mode, clock = clock, startAt = GuideScreenId.TODAY)
            assertOn(GuideScreenId.TODAY, mode)
            val steps = Guide.steps(GuideScreenId.TODAY, mode)
            assertEquals(GuideStep.TODAY_EMPTY, pictureStep())
            for ((i, step) in steps.withIndex()) {
                clock.held = i
                compose.waitForIdle()
                assertEquals("$mode step $i", step, pictureStep())
            }
            assertEquals(GuideStep.TODAY_SKIP, pictureStep())
        }
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

    /** On the picture's own phone, each side's last step shows both the ticked set and the "Skipped today" line. */
    @Test
    @Config(qualifiers = "w412dp-h2000dp-xhdpi")
    fun guide_screen4_lastState_drawsTheTickAndTheSkippedLine() {
        var mode by mutableStateOf(TrainingMode.POWERLIFTING)
        compose.setContent {
            key(mode) {
                PowertrackerTheme(mode = mode) {
                    Box(Modifier.requiredSize(PHONE_WIDTH, phoneHeight(mode))) { TodayPicture(mode, Guide.steps(GuideScreenId.TODAY, mode).last()) }
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
            // Wholly on the phone, not cut at its foot.
            val bottom = compose.onNodeWithTag("skipped-today").fetchSemanticsNode().boundsInRoot.bottom
            val phone = with(compose.density) { phoneHeight(side).toPx() }
            assertTrue("$side: the skipped line ends at $bottom px, the phone at $phone px", bottom <= phone)
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
}
