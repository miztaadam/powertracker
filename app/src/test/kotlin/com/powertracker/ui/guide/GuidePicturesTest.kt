package com.powertracker.ui.guide

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.powertracker.domain.guide.Guide
import com.powertracker.domain.guide.GuideFrame
import com.powertracker.domain.guide.GuideSample
import com.powertracker.domain.guide.GuideScreenId
import com.powertracker.domain.guide.GuideSideFrame
import com.powertracker.domain.guide.GuideStep
import com.powertracker.domain.guide.GuideTap
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.components.formatKg
import com.powertracker.ui.guide.pictures.sideLoopNow
import com.powertracker.ui.guide.pictures.sidePictureCompositions
import com.powertracker.ui.motion.LocalModeSwitch
import com.powertracker.ui.motion.ModeSwitchController
import com.powertracker.ui.motion.ModeSwitchFeedback
import com.powertracker.ui.motion.ModeSwitchKind
import com.powertracker.ui.theme.LocalMotion
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.targetHeadline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The guide's other seven pictures (1.16, Task 4). Every value drawn comes from the domain; none is typed here. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w412dp-h915dp-xhdpi")
class GuidePicturesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val pl = TrainingMode.POWERLIFTING
    private val bb = TrainingMode.BODYBUILDING

    /** The pictures with more than one step. */
    private val moving = listOf(GuideScreenId.WELCOME, GuideScreenId.PLAN, GuideScreenId.TARGETS, GuideScreenId.HOLDS_BACK, GuideScreenId.MONTHS_LATER)

    private val seven = Guide.SCREENS - GuideScreenId.TODAY

    /** The rule takes one setContent per test: later calls swap in fresh content, built anew. */
    private var content by mutableStateOf<(@Composable () -> Unit)?>(null)
    private var contentSet = false

    private fun setOnce(c: @Composable () -> Unit) {
        val token = Any()
        content = { key(token) { c() } }
        if (!contentSet) {
            contentSet = true
            compose.setContent { content?.invoke() }
        }
        compose.waitForIdle()
    }

    private fun show(
        startAt: GuideScreenId,
        mode: TrainingMode = pl,
        motionOn: Boolean = true,
        clock: GuideClock = HeldGuideClock(),
        fontScale: Float = 1f,
    ) = setOnce {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalMotion provides motionOn, LocalDensity provides Density(density.density, fontScale)) {
            PowertrackerTheme(mode = mode, darkTheme = true) {
                GuideScreen(mode, firstLaunch = false, hasPlan = false, motionOn = motionOn, onPickSide = {}, onClose = {}, clock = clock, startAt = startAt)
            }
        }
    }

    private fun pictureFrame(): GuideFrame = compose.onNodeWithTag("guide-picture").fetchSemanticsNode().config[GuidePictureFrame]

    private fun assertOn(id: GuideScreenId, mode: TrainingMode = pl) =
        compose.onNodeWithTag("guide-title").assertTextEquals(Guide.screen(id, mode).title)

    private fun drawn(): IntArray {
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
    }

    // ---- what each picture draws first and last (drawn on its own, so its text can be read) ----

    /** [id]'s picture alone at [frame], on a phone-sized box, its text readable (the guide clears it). */
    private fun drawAlone(id: GuideScreenId, mode: TrainingMode, frame: GuideFrame) = setOnce {
        PowertrackerTheme(mode = mode, darkTheme = true) {
            Box(Modifier.requiredSize(PHONE_WIDTH, PHONE_HEIGHT)) { PictureContent(id, mode, frame) }
        }
    }

    private fun firstFrame(id: GuideScreenId, mode: TrainingMode) = Guide.frame(Guide.steps(id, mode), 0)

    private fun lastFrame(id: GuideScreenId, mode: TrainingMode) = Guide.frame(Guide.steps(id, mode), Guide.doneMs(Guide.steps(id, mode).size))

    private fun shows(text: String) = compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()

    /** First and last step in the guide on a held clock: the frame is its first, then its last step, settled. */
    private fun firstAndLastInTheGuide(id: GuideScreenId, mode: TrainingMode) {
        val steps = Guide.steps(id, mode)
        val clock = HeldGuideClock(0)
        show(id, mode, clock = clock)
        assertOn(id, mode)
        assertEquals(GuideFrame(0, steps.first(), 1.0, steps.size == 1), pictureFrame())
        val first = drawn()
        clock.holdStep(steps.lastIndex)
        compose.waitForIdle()
        assertEquals(GuideFrame(steps.lastIndex, steps.last(), 1.0, true), pictureFrame())
        if (steps.size > 1) assertFalse("$id on $mode: the last step draws something else", first.contentEquals(drawn()))
        // Held, it stays.
        compose.mainClock.advanceTimeBy(Guide.STEP_MS * 5L)
        assertEquals(steps.last(), pictureFrame().step)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun guide_welcome_firstAndLastStep() {
        for (mode in TrainingMode.entries) {
            firstAndLastInTheGuide(GuideScreenId.WELCOME, mode)
        }
    }

    @Test
    fun guide_welcome_drawsTheMark_thenBothSides() {
        drawAlone(GuideScreenId.WELCOME, pl, lastFrame(GuideScreenId.WELCOME, pl))
        assertTrue(shows("Powerlifting") && shows("Bodybuilding"))
        // No numbers: the welcome is not an example.
        assertFalse(Guide.screen(GuideScreenId.WELCOME, pl).example)
    }

    @Test
    fun guide_side_firstAndLastStep() {
        for (mode in TrainingMode.entries) {
            // Its one step is first and last, done at once; the loop is its own (below).
            show(GuideScreenId.SIDE, mode, motionOn = false)
            assertEquals(GuideFrame(0, GuideStep.SIDE_LOOP, 1.0, true), pictureFrame())
            assertEquals(Guide.SIDE_STILL, sideLoop())
        }
    }

    private fun sideLoop(): GuideSideFrame = compose.runOnIdle { sideLoopNow?.invoke() } ?: error("no side loop")

    /** Screen 2's loop, with motion on, follows the frame clock in the domain's loop, and is read only when drawn. */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun guide_side_loopsTheHammerAndThePump_onTheFrameClock() {
        compose.mainClock.autoAdvance = false
        show(GuideScreenId.SIDE)
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        val compositions = sidePictureCompositions
        val start = sideLoop()
        assertTrue("$start", start.dropMs < 100)
        val before = drawn()
        compose.mainClock.advanceTimeBy(300)
        val mid = sideLoop()
        assertTrue("it moves: $start to $mid", mid.dropMs > start.dropMs && mid.pumpMs > start.pumpMs)
        assertFalse(before.contentEquals(drawn()))
        compose.mainClock.advanceTimeBy(Guide.SIDE_LOOP_MS.toLong())
        assertTrue("round again: ${sideLoop()} after $mid", kotlin.math.abs(sideLoop().dropMs - mid.dropMs) < 40)
        assertEquals("the loop runs in the draw phase only", compositions, sidePictureCompositions)
    }

    @Test
    fun guide_screen2_callsNoControllerSoundOrVibration() {
        val feedback = object : ModeSwitchFeedback {
            var calls = 0
            override fun start(kind: ModeSwitchKind) { calls++ }
            override fun stop() { calls++ }
            override fun skip() { calls++ }
            override fun prefetch(next: ModeSwitchKind) { calls++ }
            override fun release() { calls++ }
        }
        val controller = ModeSwitchController(feedback)
        compose.mainClock.autoAdvance = false
        setOnce {
            CompositionLocalProvider(LocalMotion provides true, LocalModeSwitch provides controller) {
                PowertrackerTheme(mode = pl, darkTheme = true) {
                    GuideScreen(pl, firstLaunch = false, hasPlan = false, motionOn = true, onPickSide = {}, onClose = {}, clock = HeldGuideClock(), startAt = GuideScreenId.SIDE)
                }
            }
        }
        compose.mainClock.advanceTimeBy(Guide.SIDE_LOOP_MS * 2L)
        assertTrue(sideLoop().dropMs >= 0)
        assertEquals("no start, sound or vibration", 0, feedback.calls)
        assertNull(controller.kind)
        assertEquals(0, controller.run)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun guide_plan_firstAndLastStep() {
        for (mode in TrainingMode.entries) {
            firstAndLastInTheGuide(GuideScreenId.PLAN, mode)
        }
    }

    @Test
    fun guide_plan_drawsTheEmptyPlan_thenTheSavedWeekWithItsSwap() {
        for (mode in TrainingMode.entries) {
            val swap = GuideSample.swap(mode)
            drawAlone(GuideScreenId.PLAN, mode, firstFrame(GuideScreenId.PLAN, mode))
            assertTrue(shows("Choose a template"))
            assertFalse(shows(swap.from))
            drawAlone(GuideScreenId.PLAN, mode, lastFrame(GuideScreenId.PLAN, mode))
            assertTrue("$mode: ${swap.to} saved", shows(swap.to))
            assertFalse("$mode: ${swap.from} swapped out", shows(swap.from))
            // The template's draft, before and after the swap.
            val steps = Guide.steps(GuideScreenId.PLAN, mode)
            drawAlone(GuideScreenId.PLAN, mode, Guide.frame(steps, Guide.settledMs(steps.indexOf(GuideStep.PLAN_DRAFT))))
            assertTrue(shows(swap.from) && shows(GuideSample.template(mode).name))
            drawAlone(GuideScreenId.PLAN, mode, Guide.frame(steps, Guide.settledMs(steps.indexOf(GuideStep.PLAN_SWAP))))
            assertTrue(shows(swap.to) && !shows(swap.from))
        }
    }

    /** Each of screen 3's taps rings its own control: the guide finds each one's box on the drawn screen. */
    @Test
    fun guide_plan_ringsEachTap_onItsControl() {
        for (mode in TrainingMode.entries) {
            val steps = Guide.steps(GuideScreenId.PLAN, mode)
            val clock = HeldGuideClock(0)
            show(GuideScreenId.PLAN, mode, clock = clock)
            for (i in 1 until steps.size) {
                val tap = Guide.tap(steps[i]) ?: continue
                clock.heldMs = Guide.motionStartMs(steps, i) + Guide.TAP_MS / 2
                compose.waitForIdle()
                assertEquals(tap, pictureFrame().tap)
                val box = compose.onNodeWithTag("guide-picture").fetchSemanticsNode().config.getOrNull(GuideTapBox)
                assertNotNull("$mode: no box for $tap", box)
                assertTrue("$mode $tap: $box", box!!.width > 0 && box.height > 0 && box.bottom < with(compose.density) { PHONE_HEIGHT.toPx() })
            }
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun guide_targets_firstAndLastStep() {
        for (mode in TrainingMode.entries) {
            firstAndLastInTheGuide(GuideScreenId.TARGETS, mode)
        }
    }

    @Test
    fun guide_targets_drawTheCoachsTarget_firstAndLast() {
        for (mode in TrainingMode.entries) {
            val advice = GuideSample.targetAdvice(mode)
            for ((frame, a) in listOf(firstFrame(GuideScreenId.TARGETS, mode) to advice.first(), lastFrame(GuideScreenId.TARGETS, mode) to advice.last())) {
                drawAlone(GuideScreenId.TARGETS, mode, frame)
                assertTrue("$mode: ${targetHeadline(a.payload as AdvicePayload.Target)}", shows(targetHeadline(a.payload as AdvicePayload.Target)))
                val e = GuideSample.targetEditor(mode, frame.stepIndex, 1.0)
                assertTrue(shows(formatKg(e.weightKg!!)))
            }
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun guide_holdsBack_firstAndLastStep() {
        for (mode in TrainingMode.entries) {
            firstAndLastInTheGuide(GuideScreenId.HOLDS_BACK, mode)
        }
    }

    @Test
    fun guide_holdsBack_drawsOneCardAStep_inTheRuleTablesOrder() {
        for (mode in TrainingMode.entries) {
            val steps = Guide.steps(GuideScreenId.HOLDS_BACK, mode)
            for ((i, a) in GuideSample.advice(mode).withIndex()) {
                drawAlone(GuideScreenId.HOLDS_BACK, mode, Guide.frame(steps, Guide.settledMs(i)))
                assertTrue("$mode card ${i + 1}: ${a.type}", shows(a.reason.take(24)))
            }
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun guide_monthsLater_firstAndLastStep() {
        for (mode in TrainingMode.entries) {
            firstAndLastInTheGuide(GuideScreenId.MONTHS_LATER, mode)
        }
    }

    @Test
    fun guide_monthsLater_endsOnTheBestsAndTotal_orOnTheCheckIn() {
        val total = GuideSample.progress(pl).totalKg!!
        drawAlone(GuideScreenId.MONTHS_LATER, pl, firstFrame(GuideScreenId.MONTHS_LATER, pl))
        assertFalse(shows("Lagging"))
        drawAlone(GuideScreenId.MONTHS_LATER, pl, lastFrame(GuideScreenId.MONTHS_LATER, pl))
        // As the screen shows it: to the half kilo.
        val shown = Math.round(total * 2) / 2.0
        assertTrue("the total at the end: ${formatKg(shown)}", shows(formatKg(shown)))
        assertFalse(shows("Lagging"))
        drawAlone(GuideScreenId.MONTHS_LATER, bb, firstFrame(GuideScreenId.MONTHS_LATER, bb))
        assertFalse(shows("Lagging"))
        drawAlone(GuideScreenId.MONTHS_LATER, bb, lastFrame(GuideScreenId.MONTHS_LATER, bb))
        assertTrue("a check-in marks the lagging muscle", shows("Lagging") && shows("Calves"))
    }

    @Test
    fun guide_yourTurn_firstAndLastStep() {
        for (mode in TrainingMode.entries) {
            show(GuideScreenId.YOUR_TURN, mode)
            assertEquals(GuideFrame(0, GuideStep.TURN_BUTTONS, 1.0, true), pictureFrame())
            drawAlone(GuideScreenId.YOUR_TURN, mode, firstFrame(GuideScreenId.YOUR_TURN, mode))
            assertTrue(shows("Choose a template"))
        }
    }

    // ---- motion off, replay ----

    @Test
    fun guide_eachPicture_withMotionOff_showsItsLastStepStill() {
        for (mode in TrainingMode.entries) for (id in seven) {
            show(id, mode, motionOn = false, clock = TickingGuideClock)
            val steps = Guide.steps(id, mode)
            assertEquals("$id $mode", GuideFrame(steps.lastIndex, steps.last(), 1.0, true), pictureFrame())
            compose.mainClock.advanceTimeBy(Guide.STEP_MS * 5L)
            assertEquals(GuideFrame(steps.lastIndex, steps.last(), 1.0, true), pictureFrame())
            assertNull(pictureFrame().tap)
            if (id == GuideScreenId.SIDE) assertEquals(Guide.SIDE_STILL, sideLoop())
        }
    }

    @Test
    fun guide_eachMovingPicture_onReplay_startsAgainAtItsFirstStep() {
        for (id in moving) {
            show(id, clock = TickingGuideClock)
            val steps = Guide.steps(id, pl)
            compose.mainClock.advanceTimeBy(Guide.doneMs(steps.size) + 500L)
            assertEquals("$id plays to its end", steps.lastIndex, pictureFrame().stepIndex)
            compose.onNodeWithTag("guide-next").performClick()
            compose.waitForIdle()
            compose.mainClock.autoAdvance = false
            compose.onNodeWithTag("guide-back").performClick()
            var frames = 0
            while (compose.onAllNodes(hasTestTag("guide-title") and hasText(Guide.screen(id, pl).title)).fetchSemanticsNodes().isEmpty() && frames++ < 120) {
                compose.mainClock.advanceTimeByFrame()
            }
            repeat(20) { compose.mainClock.advanceTimeByFrame() }
            assertOn(id)
            assertEquals("$id starts again", 0, pictureFrame().stepIndex)
            compose.mainClock.autoAdvance = true
        }
    }

    // ---- review focus ----

    /** Review Focus 5: screen 6, the longest copy, at twice the font size: the text wraps, none is cut, and the controls stay. */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun guide_screen6_atTwiceTheFontSize_cutsNothing_andKeepsTheControls() {
        for (mode in TrainingMode.entries) {
            show(GuideScreenId.HOLDS_BACK, mode, fontScale = 2f)
            assertOn(GuideScreenId.HOLDS_BACK, mode)
            for (tag in listOf("guide-skip", "guide-back", "guide-next", "guide-dots")) compose.onNodeWithTag(tag).assertIsDisplayed()
            for (line in Guide.screen(GuideScreenId.HOLDS_BACK, mode).lines) {
                val node = compose.onNodeWithText(line.text).fetchSemanticsNode()
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                val l = layouts.single()
                // Every word laid out, none ellipsized, all its lines inside its own height.
                assertFalse("cut: ${line.text}", l.didOverflowHeight)
                assertEquals("cut: ${line.text}", line.text.length, l.getLineEnd(l.lineCount - 1))
                assertTrue((0 until l.lineCount).none { l.isLineEllipsized(it) })
                // And on screen: its box is inside the page, between the top bar and the controls.
                val box = node.boundsInRoot
                val next = compose.onNodeWithTag("guide-next").fetchSemanticsNode().boundsInRoot
                assertTrue("${line.text}: $box", box.height >= l.size.height - 1 && box.top >= 0f && box.bottom <= next.top)
            }
            // The text is larger than at 1×: at least one line wraps.
            val wrapped = Guide.screen(GuideScreenId.HOLDS_BACK, mode).lines.any { line ->
                val layouts = mutableListOf<TextLayoutResult>()
                compose.onNodeWithText(line.text).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                layouts.single().lineCount > 1
            }
            assertTrue(wrapped)
        }
    }

    /** Review Focus 3: a rotation (the activity's state saved and restored) keeps the screen shown. */
    @Test
    fun guide_rotationOrResize_keepsTheScreen() {
        val restore = StateRestorationTester(compose)
        restore.setContent {
            CompositionLocalProvider(LocalMotion provides false) {
                PowertrackerTheme(mode = pl, darkTheme = true) {
                    GuideScreen(pl, firstLaunch = false, hasPlan = false, motionOn = false, onPickSide = {}, onClose = {}, clock = HeldGuideClock())
                }
            }
        }
        repeat(Guide.SCREENS.indexOf(GuideScreenId.HOLDS_BACK)) {
            compose.onNodeWithTag("guide-next").performClick()
            compose.waitForIdle()
        }
        assertOn(GuideScreenId.HOLDS_BACK)
        restore.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        assertOn(GuideScreenId.HOLDS_BACK)
        assertEquals(GuideStep.HOLDS_CARD, pictureFrame().step)
    }

    @Test
    fun guide_everyPicture_isOneDescription() {
        for (id in seven) {
            show(id)
            val node = compose.onNodeWithTag("guide-picture").fetchSemanticsNode()
            assertEquals(listOf(pictureDescription(id)), node.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription))
            assertTrue(node.children.isEmpty())
        }
    }

    /**
     * For the owner (GUIDE_FRAMES=<dir>): each of the seven pictures, powerlifting dark, every GUIDE_FRAME_MS (80 by
     * default) from its start to just past its end, from one live composition, the held guide clock and the frame clock
     * moving together. Screen 2 runs one turn of its loop on the frame clock.
     */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun guide_pictures_frameStrips() {
        val dir = System.getenv("GUIDE_FRAMES") ?: return
        val every = System.getenv("GUIDE_FRAME_MS")?.toInt() ?: 80
        java.io.File(dir).mkdirs()
        fun save(name: String) {
            val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
            val full = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(full))
            val b = compose.onNodeWithTag("guide-picture").fetchSemanticsNode().boundsInRoot
            val crop = Bitmap.createBitmap(full, b.left.toInt(), b.top.toInt(), b.width.toInt(), b.height.toInt())
            java.io.File(dir, "$name.png").outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        for (id in seven) {
            val name = id.name.lowercase().replace('_', '-')
            val steps = Guide.steps(id, pl)
            val clock = HeldGuideClock(0)
            // Screen 2's loop is an infinite animation, which waiting for idle cancels: its strip never waits.
            compose.mainClock.autoAdvance = id != GuideScreenId.SIDE
            show(id, clock = clock)
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeByFrame()
            val end = if (id == GuideScreenId.SIDE) Guide.SIDE_LOOP_MS else Guide.doneMs(steps.size) + 300
            var t = 0
            var n = 0
            while (t <= end) {
                compose.runOnUiThread { clock.heldMs = t }
                androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
                compose.mainClock.advanceTimeBy(every.toLong())
                save("android-$name-powerlifting-dark-%03d-%05dms".format(n++, t))
                t += every
            }
        }
    }
}
