package com.powertracker.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.powertracker.data.AppSettings
import com.powertracker.ui.ModeTheme
import com.powertracker.ui.theme.LocalMotion
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Tone
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.LocalDate
import com.powertracker.ui.coach.SwapCard
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.powertracker.ui.library.ExercisePickerContent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import com.powertracker.ui.library.ExercisesContent

/**
 * Press feedback and the buzz (stage 2, Task 2): a press sinks and springs back only with motion on, never on a
 * disabled control, and a press that becomes a scroll springs back and fires nothing. Each main action buzzes once,
 * and nothing buzzes with "Vibrate on taps" off or on a disabled button.
 */
// Native graphics, so text has its real size and a 3% sink is many pixels.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h900dp-xxhdpi")
class PressAndHapticsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(motion: Boolean = true, content: @Composable () -> Unit) = compose.setContent {
        PowertrackerTheme { CompositionLocalProvider(LocalMotion provides motion, content = content) }
    }

    /** The drawn width of [text]: inside the button's layer, so it shrinks with the press. */
    private fun width(text: String) = compose.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width

    private fun frames(ms: Long) {
        compose.mainClock.advanceTimeBy(ms)
        compose.waitForIdle()
    }

    // ---- The press ----

    @Test
    fun press_sinks_thenSpringsBack_withAnOvershoot() {
        show { PrimaryButton("Log set", {}) }
        compose.waitForIdle()
        val rest = width("Log set")
        compose.mainClock.autoAdvance = false

        compose.onNodeWithText("Log set").performTouchInput { down(center) }
        frames(150)
        assertEquals("pressed: 97%", rest * PRESS_SCALE, width("Log set"), rest * 0.002f)

        compose.onNodeWithText("Log set").performTouchInput { up() }
        var widest = 0f
        repeat(25) {
            frames(16)
            widest = maxOf(widest, width("Log set"))
        }
        assertTrue("springs past its size on the way back ($widest vs $rest)", widest > rest * 1.001f)
        frames(500)
        assertEquals("back at rest", rest, width("Log set"), 0.01f)
    }

    @Test
    fun press_takesAbout90ms_down_andSettlesInAbout260ms() {
        show { PrimaryButton("Log set", {}) }
        compose.waitForIdle()
        val rest = width("Log set")
        compose.mainClock.autoAdvance = false

        compose.onNodeWithText("Log set").performTouchInput { down(center) }
        frames(48)
        val midway = width("Log set")
        assertTrue("still sinking after 48 ms", midway < rest && midway > rest * PRESS_SCALE)
        frames(64)
        assertEquals("down by 112 ms", rest * PRESS_SCALE, width("Log set"), rest * 0.002f)

        compose.onNodeWithText("Log set").performTouchInput { up() }
        frames(320)
        assertEquals("settled by about 300 ms", rest, width("Log set"), rest * 0.001f)
    }

    @Test
    fun withMotionOff_aPressDoesNotScale() {
        show(motion = false) { PrimaryButton("Log set", {}) }
        compose.waitForIdle()
        val rest = width("Log set")
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Log set").performTouchInput { down(center) }
        frames(200)
        assertEquals(rest, width("Log set"), 0f)
        compose.onNodeWithText("Log set").performTouchInput { up() }
        frames(400)
        assertEquals(rest, width("Log set"), 0f)
    }

    @Test
    fun disabled_doesNotScale() {
        show { PrimaryButton("Log set", {}, enabled = false) }
        compose.waitForIdle()
        val rest = width("Log set")
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Log set").performTouchInput { down(center) }
        frames(200)
        assertEquals(rest, width("Log set"), 0f)
    }

    @Test
    fun pressScale_disabled_ignoresPressesItsSourceStillSees() {
        // A control that still reports presses while disabled (a plain clickable): the modifier itself must not scale.
        show {
            val source = remember { MutableInteractionSource() }
            Box(Modifier.pressScale(source, enabled = false).clickable(source, LocalIndication.current) {}.testTag("box")) {
                Text("Inside")
            }
        }
        compose.waitForIdle()
        val rest = width("Inside")
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("box").performTouchInput { down(center) }
        frames(200)
        assertEquals(rest, width("Inside"), 0f)
    }

    @Test
    fun pressScale_takesItsOwnDepth_asTheTabIconsDo() {
        show {
            val source = remember { MutableInteractionSource() }
            Box(Modifier.pressScale(source, pressedScale = TAB_PRESS_SCALE).clickable(source, LocalIndication.current) {}.testTag("box")) {
                Text("Tab")
            }
        }
        compose.waitForIdle()
        val rest = width("Tab")
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("box").performTouchInput { down(center) }
        frames(150)
        assertEquals(rest * TAB_PRESS_SCALE, width("Tab"), rest * 0.002f)
    }

    @Test
    fun aPressThatBecomesAScroll_springsBack_andFiresNothing() {
        var clicks = 0
        show {
            LazyColumn(Modifier.fillMaxSize().testTag("list")) {
                items((1..30).toList()) { i ->
                    PtCard(Modifier.height(72.dp).testTag("row-$i"), onClick = { clicks++ }) { Text("Row $i") }
                }
            }
        }
        compose.waitForIdle()
        val rest = width("Row 3")
        compose.mainClock.autoAdvance = false

        compose.onNodeWithTag("row-3").performTouchInput { down(center) }
        // Inside a scrolling list the press is only reported after the tap timeout.
        frames(250)
        assertTrue("sinks once the press is reported", width("Row 3") < rest * 0.98f)

        compose.onNodeWithTag("row-3").performTouchInput {
            repeat(10) { moveBy(androidx.compose.ui.geometry.Offset(0f, -30f)) }
            up()
        }
        frames(600)
        assertEquals("nothing fired", 0, clicks)
        assertEquals("sprang back", rest, width("Row 3"), 0.01f)
    }

    // ---- The buzz ----

    @Test
    fun stepper_ticksOncePerStep_andNotWhenDisabled() {
        val fake = FakeHapticFeedback()
        var value = 0
        show {
            WithHaptics(fake) {
                Stepper("Sets", "3", canDecrement = false, canIncrement = true, onDecrement = { value-- }, onIncrement = { value++ }, tick = true)
            }
        }
        repeat(3) { compose.onNodeWithContentDescription("Increase Sets").performClick() }
        compose.onNodeWithContentDescription("Decrease Sets").performClick()
        compose.waitForIdle()
        assertEquals(3, value)
        assertEquals(listOf(HapticFeedbackType.ContextClick, HapticFeedbackType.ContextClick, HapticFeedbackType.ContextClick), fake.calls)
    }

    @Test
    fun stepperField_fastTaps_eachTickOnce_withNoQueueAfter() {
        val fake = FakeHapticFeedback()
        show {
            WithHaptics(fake) {
                StepperField("Reps", "5", canDecrement = true, canIncrement = true, onDecrement = {}, onIncrement = {}, tick = true)
            }
        }
        compose.mainClock.autoAdvance = false
        repeat(5) {
            compose.onNodeWithContentDescription("Increase Reps").performTouchInput { click(center) }
            frames(16)
        }
        assertEquals(5, fake.ticks)
        frames(2_000)
        assertEquals("no buzzes after the taps", 5, fake.calls.size)
    }

    @Test
    fun withTheSettingOff_nothingBuzzes() {
        val fake = FakeHapticFeedback()
        show {
            WithHaptics(fake, vibrateOnTaps = false) {
                Stepper("Sets", "3", canDecrement = true, canIncrement = true, onDecrement = {}, onIncrement = {}, tick = true)
            }
        }
        compose.onNodeWithContentDescription("Increase Sets").performClick()
        compose.waitForIdle()
        assertEquals(emptyList<HapticFeedbackType>(), fake.calls)
    }

    @Test
    fun theStoredSetting_reachesTheScreens_throughTheThemeRoot() {
        val fake = FakeHapticFeedback()
        val settings = MutableStateFlow(AppSettings(vibrateOnTaps = false))
        compose.setContent {
            ModeTheme(settings, motion = false) {
                CompositionLocalProvider(LocalHapticFeedback provides fake) {
                    Stepper("Sets", "3", canDecrement = true, canIncrement = true, onDecrement = {}, onIncrement = {}, tick = true)
                }
            }
        }
        compose.onNodeWithContentDescription("Increase Sets").performClick()
        compose.waitForIdle()
        assertEquals("off: no buzz", 0, fake.calls.size)

        settings.value = AppSettings(vibrateOnTaps = true)
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Increase Sets").performClick()
        compose.waitForIdle()
        assertEquals("on: one tick", 1, fake.ticks)
    }

    @Test
    fun haptics_tick_and_best_followTheSettingAsItChanges() {
        val fake = FakeHapticFeedback()
        val on = androidx.compose.runtime.mutableStateOf(true)
        lateinit var haptics: Haptics
        show { WithHaptics(fake, vibrateOnTaps = on.value) { haptics = rememberHaptics() } }
        compose.waitForIdle()
        val kept = haptics
        compose.runOnIdle { kept.tick(); kept.best() }
        assertEquals(listOf(HapticFeedbackType.ContextClick, HapticFeedbackType.Confirm), fake.calls)

        on.value = false
        compose.waitForIdle()
        // The same object, captured once (as a remembered lambda would), now stays quiet.
        compose.runOnIdle { kept.tick(); kept.best() }
        assertEquals(2, fake.calls.size)
        assertTrue(haptics === kept)
    }

    @Test
    fun coachCards_doNotBuzzThemselves_theScreenTicksOnceTheApplyIsStored() {
        val fake = FakeHapticFeedback()
        var applied = 0
        show {
            WithHaptics(fake) {
                Column {
                    CoachSuggestion("155 × 5 @ 8", "Last time 150 × 5 @ 7.", onApply = { applied++ }, onDismiss = {})
                    AdviceCard(Tone.WARNING, Icons.Filled.TrendingFlat, "Stall", "Bench has stalled", "Flat for 3 weeks.",
                        primaryAction = "Add to plan", onPrimary = { applied++ }, secondaryAction = "Dismiss", onSecondary = {})
                }
            }
        }
        compose.onNodeWithText("Apply").performClick()
        compose.onNodeWithText("Add to plan").performClick()
        compose.onNodeWithText("Dismiss").performClick()
        compose.onNodeWithContentDescription("Dismiss").performClick()
        compose.waitForIdle()
        assertEquals(2, applied)
        assertEquals(0, fake.calls.size)
    }

    @Test
    fun aDisabledButton_neitherFiresNorBuzzes() {
        val fake = FakeHapticFeedback()
        show {
            WithHaptics(fake) {
                Stepper("Sets", "1", canDecrement = false, canIncrement = false, onDecrement = {}, onIncrement = {}, tick = true)
            }
        }
        compose.onNodeWithContentDescription("Decrease Sets").performClick()
        compose.onNodeWithContentDescription("Increase Sets").performClick()
        compose.waitForIdle()
        assertEquals(0, fake.calls.size)
    }

    @Test
    fun swapCard_doesNotBuzzItself_theScreenTicksOnceTheSwapIsStored() {
        val fake = FakeHapticFeedback()
        val swapped = mutableListOf<String>()
        val swap = AdvicePayload.Swap(2, "Lat pulldown", MuscleGroup.LATS, LocalDate(2026, 10, 14), listOf("Machine pulldown", "Close-grip pulldown"))
        val advice = Advice(AdviceType.SWAP, null, LocalDate(2026, 11, 11), "Stuck.", 0, swap, id = 2, mode = TrainingMode.BODYBUILDING)
        show { WithHaptics(fake) { SwapCard(advice, swap, onSwap = { swapped += it }, onDismiss = {}) } }
        compose.onNodeWithText("Swap for Close-grip pulldown").performClick()
        compose.onNodeWithText("Dismiss").performClick()
        compose.waitForIdle()
        assertEquals(listOf("Close-grip pulldown"), swapped)
        assertEquals(emptyList<HapticFeedbackType>(), fake.calls)
    }

    @Test
    fun aQuickTap_inAScrollingScreen_stillSinks_thenSpringsBack() {
        show {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                PrimaryButton("Log set", {})
                Box(Modifier.height(2000.dp))
            }
        }
        compose.waitForIdle()
        val rest = width("Log set")
        compose.mainClock.autoAdvance = false
        // Inside a scrolling screen the press is held back, then reported with its release when the finger lifts first.
        compose.onNodeWithText("Log set").performTouchInput { click(center) }
        var smallest = rest
        repeat(30) {
            frames(16)
            smallest = minOf(smallest, width("Log set"))
        }
        assertTrue("a quick tap sinks visibly ($smallest vs $rest)", smallest < rest * 0.985f)
        frames(500)
        assertEquals(rest, width("Log set"), 0.01f)
    }

    @Test
    fun aPickerRow_sinksWhilePressed() {
        show {
            ExercisePickerContent("Add exercise", emptyList(), emptyList(), onPick = {}, onCreateCustom = { _, _, _, _ -> }, onClose = {})
        }
        compose.onNodeWithTag("exercise-search").performTextReplacement("Hammer")
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("exercise-Hammer curl")).fetchSemanticsNodes().isNotEmpty() }
        val rest = width("Hammer curl")
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("exercise-Hammer curl").performTouchInput { down(center) }
        frames(250)
        assertEquals(rest * PRESS_SCALE, width("Hammer curl"), rest * 0.003f)
        compose.onNodeWithTag("exercise-Hammer curl").performTouchInput { cancel() }
        frames(600)
        assertEquals(rest, width("Hammer curl"), 0.01f)
    }

    @Test
    fun steppers_doNotTick_unlessAsked_onlyTheSetEntryDoes() {
        val fake = FakeHapticFeedback()
        var steps = 0
        show {
            WithHaptics(fake) {
                Column {
                    Stepper("Rest", "3:00", canDecrement = true, canIncrement = true, onDecrement = { steps++ }, onIncrement = { steps++ })
                    StepperField("Sets", "3", canDecrement = true, canIncrement = true, onDecrement = { steps++ }, onIncrement = { steps++ })
                }
            }
        }
        compose.onNodeWithContentDescription("Increase Rest").performClick()
        compose.onNodeWithContentDescription("Decrease Sets").performClick()
        compose.waitForIdle()
        assertEquals(2, steps)
        assertEquals(0, fake.calls.size)
    }

    // ---- Every tappable thing sinks (fix round) ----

    /** Presses [tag] and reports how far the text [inside] sank, as a fraction of its width at rest. */
    private fun sinkOf(tag: String, inside: String): Float = sinkOf(compose.onNodeWithTag(tag), tag) { width(inside) }

    private fun sinkOf(node: SemanticsNodeInteraction, name: String, measure: () -> Float): Float {
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        val rest = measure()
        compose.mainClock.autoAdvance = false
        node.performTouchInput { down(center) }
        frames(250)
        val pressed = measure()
        node.performTouchInput { cancel() }
        frames(600)
        assertEquals("$name springs back", rest, measure(), 0.01f)
        return pressed / rest
    }

    /** The set row clears its children's semantics, so it is measured by its own (clickable) node, inside the sink. */
    private val setRow get() = compose.onNode(hasClickAction() and hasAnyAncestor(hasTestTag("set-box")), useUnmergedTree = true)

    private fun setRowSink() = sinkOf(setRow, "set row") { setRow.fetchSemanticsNode().boundsInRoot.width }

    @Composable
    private fun EveryKind() {
        Column {
            PtIconButton(onClick = {}, modifier = Modifier.testTag("icon")) { Text("i") }
            PtIconButton(onClick = {}, modifier = Modifier.testTag("icon-off"), enabled = false) { Text("o") }
            StatTile("Squat", "180", Modifier.testTag("tile"), unit = "kg", onClick = {}, onClickLabel = "Open history")
            StatTile("Bench", "120", Modifier.testTag("tile-still"), unit = "kg")
            Box(Modifier.testTag("set-box")) { SetRow(1, 150.0, 5, 8.0, SetState.DONE, onClick = {}) }
            Box(Modifier.pressClickable { }.testTag("clickable")) { Text("Click") }
            Box(Modifier.pressClickable(enabled = false) { }.testTag("clickable-off")) { Text("Off") }
            var on by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            Box(Modifier.pressToggleable(on) { on = it }.testTag("toggle")) { Text("Toggle") }
            PtMenuItemHost()
        }
    }

    @Composable
    private fun PtMenuItemHost() {
        Box(Modifier.testTag("menu")) { PtDropdownMenuItem(text = { Text("Edit") }, onClick = {}) }
    }

    @Test
    fun everyTappableKind_sinks_withMotionOn_butNotWhenDisabledOrNotTappable() {
        show { EveryKind() }
        assertEquals(PRESS_SCALE, sinkOf("icon", "i"), 0.003f)
        assertEquals(PRESS_SCALE, sinkOf("tile", "180"), 0.003f)
        assertEquals(PRESS_SCALE, setRowSink(), 0.003f)
        assertEquals(PRESS_SCALE, sinkOf("clickable", "Click"), 0.003f)
        assertEquals(PRESS_SCALE, sinkOf("toggle", "Toggle"), 0.003f)
        assertEquals(PRESS_SCALE, sinkOf("menu", "Edit"), 0.003f)
        assertEquals(1f, sinkOf("icon-off", "o"), 0f)
        assertEquals(1f, sinkOf("clickable-off", "Off"), 0f)
        assertEquals(1f, sinkOf("tile-still", "120"), 0f)
    }

    @Test
    fun everyTappableKind_staysStill_withMotionOff() {
        show(motion = false) { EveryKind() }
        for ((tag, text) in listOf("icon" to "i", "tile" to "180", "clickable" to "Click", "toggle" to "Toggle", "menu" to "Edit")) {
            assertEquals(tag, 1f, sinkOf(tag, text), 0f)
        }
        assertEquals(1f, setRowSink(), 0f)
    }

    @Test
    fun thePickersMuscleTiles_andDropDownChip_sink() {
        show {
            ExercisePickerContent("Add exercise", emptyList(), emptyList(), onPick = {}, onCreateCustom = { _, _, _, _ -> }, onClose = {})
        }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("muscle-CHEST")).fetchSemanticsNodes().isNotEmpty() }
        val chest = { compose.onNode(hasText("Chest") and hasAnyAncestor(hasTestTag("muscle-CHEST")), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width }
        assertEquals(PRESS_SCALE, sinkOf(compose.onNodeWithTag("muscle-CHEST"), "muscle tile", chest), 0.003f)
    }

    @Test
    fun theExercisesTabsMuscleDropDownChip_sinks() {
        show { ExercisesContent(emptyList(), emptyList(), onOpen = {}) }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("muscles-dropdown")).fetchSemanticsNodes().isNotEmpty() }
        val chip = { compose.onNode(hasText("Muscles") and hasAnyAncestor(hasTestTag("muscles-dropdown")), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width }
        assertEquals(PRESS_SCALE, sinkOf(compose.onNodeWithTag("muscles-dropdown"), "drop-down chip", chip), 0.003f)
    }
}
