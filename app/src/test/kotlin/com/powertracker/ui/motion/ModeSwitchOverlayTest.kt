package com.powertracker.ui.motion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.data.BackupService
import com.powertracker.data.SettingsRepository
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.motion.ModeSwitchMotion
import com.powertracker.domain.motion.Point
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.ModeTheme
import com.powertracker.ui.plan.SettingsScreen
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import kotlin.math.abs

/**
 * The mode switch animations on Android (1.15): the controller's timeline, the overlay over the app, and the Settings
 * switch that starts it. Held frames of both animations, dark and light, go to `app/build/screenshots/`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h915dp-xhdpi")
class ModeSwitchOverlayTest {
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var settings: SettingsRepository

    /** Notes what the controller asked of the vibration and the sound. A skip is a stop and a tick. */
    private class RecordingFeedback : ModeSwitchFeedback {
        val starts = mutableListOf<ModeSwitchKind>()
        val prefetches = mutableListOf<ModeSwitchKind>()
        var stops = 0
        var ticks = 0
        var releases = 0
        override fun start(kind: ModeSwitchKind) {
            starts += kind
        }
        override fun stop() {
            stops++
        }
        override fun skip() {
            stops++
            ticks++
        }
        override fun prefetch(next: ModeSwitchKind) {
            prefetches += next
        }
        override fun release() {
            releases++
        }
    }

    private val feedback = RecordingFeedback()

    /** The tap's clock (ms): it stands still unless a test moves it, so the frames alone move the picture. */
    private var uptime = 0L
    private val controller = ModeSwitchController(feedback) { uptime }

    private var accent = Color.Unspecified
    private var showSettings by mutableStateOf(true)

    @Before
    fun setUp() {
        settings = SettingsRepository(context)
        runBlocking { settings.update { AppSettings(trainingMode = TrainingMode.BODYBUILDING) } }
    }

    @After
    fun tearDown() {
        ModeSwitchProbe.reset()
    }

    private fun stored() = runBlocking { settings.settings.first() }

    private fun show(motion: Boolean = true, dark: Boolean = true, fontScale: Float = 1f) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ModeTheme(settings.settings, darkTheme = dark, motion = motion, modeSwitch = controller) {
                    val a = PowertrackerTheme.colors.accent
                    SideEffect { accent = a }
                    ModeSwitchHost {
                        Box {
                            if (showSettings) SettingsScreen(settings, BackupService(database.db, settings), onBack = {}, today = { LocalDate(2026, 11, 19) })
                        }
                    }
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("bodybuilding-mode")).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun frame() = compose.mainClock.advanceTimeByFrame()

    private fun flip() = compose.onNodeWithTag("bodybuilding-mode").performClick()

    private fun overlayShown() = compose.onAllNodes(hasTestTag("mode-switch-overlay"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun waitStored(mode: TrainingMode) {
        val until = System.currentTimeMillis() + 5_000
        while (stored().trainingMode != mode) {
            check(System.currentTimeMillis() < until) { "never stored $mode" }
            Thread.sleep(5)
        }
    }

    // ---- the controller alone ----

    @Test
    fun controller_drop_holdsTheOldSide_untilTheImpact() {
        controller.noteStored(TrainingMode.BODYBUILDING)
        controller.start(ModeSwitchKind.DROP)
        assertEquals(ModeSwitchKind.DROP, controller.kind)
        assertEquals(TrainingMode.BODYBUILDING, controller.shownMode)
        controller.noteStored(TrainingMode.POWERLIFTING) // stored at the tap
        controller.advanceTo(249.0)
        assertEquals(TrainingMode.BODYBUILDING, controller.shownMode)
        controller.advanceTo(250.0)
        assertEquals(TrainingMode.POWERLIFTING, controller.shownMode)
        controller.advanceTo(ModeSwitchMotion.DROP.end)
        assertNull("over at the end", controller.kind)
        assertNull("the stored side shows again", controller.shownMode)
        assertEquals(listOf(ModeSwitchKind.DROP), feedback.starts)
        assertEquals("the sound's tail plays out", 0, feedback.stops)
    }

    @Test
    fun controller_pump_changesAtTheThirdBeat() {
        controller.noteStored(TrainingMode.POWERLIFTING)
        controller.start(ModeSwitchKind.PUMP)
        controller.noteStored(TrainingMode.BODYBUILDING)
        controller.advanceTo(400.0)
        assertEquals(TrainingMode.POWERLIFTING, controller.shownMode)
        controller.advanceTo(749.0)
        assertEquals(TrainingMode.POWERLIFTING, controller.shownMode)
        controller.advanceTo(750.0)
        assertEquals(TrainingMode.BODYBUILDING, controller.shownMode)
        controller.advanceTo(ModeSwitchMotion.PUMP.end)
        assertNull(controller.kind)
    }

    @Test
    fun controller_skip_endsAtTheEndState_andStopsTheSoundAndVibration() {
        controller.noteStored(TrainingMode.POWERLIFTING)
        controller.start(ModeSwitchKind.PUMP)
        controller.noteStored(TrainingMode.BODYBUILDING)
        controller.advanceTo(200.0)
        controller.skip()
        assertNull(controller.kind)
        assertNull(controller.shownMode)
        assertEquals(1, feedback.stops)
    }

    @Test
    fun controller_aSkipBeforeTheWriteLands_keepsTheNewSide() {
        controller.noteStored(TrainingMode.BODYBUILDING)
        controller.start(ModeSwitchKind.DROP)
        controller.skip()
        assertEquals(TrainingMode.POWERLIFTING, controller.shownMode)
        controller.noteStored(TrainingMode.POWERLIFTING)
        assertNull(controller.shownMode)
    }

    @Test
    fun controller_threeFlips_oneAnimation_oneSound_theLastSide() {
        controller.noteStored(TrainingMode.BODYBUILDING)
        controller.start(ModeSwitchKind.DROP)
        controller.noteStored(TrainingMode.POWERLIFTING)
        controller.advanceTo(120.0)
        controller.start(ModeSwitchKind.PUMP)
        assertEquals("the drop skipped to its end", TrainingMode.POWERLIFTING, controller.shownMode)
        controller.noteStored(TrainingMode.BODYBUILDING)
        controller.advanceTo(300.0)
        controller.start(ModeSwitchKind.DROP)
        controller.noteStored(TrainingMode.POWERLIFTING)
        assertEquals(ModeSwitchKind.DROP, controller.kind)
        assertEquals(0.0, controller.tMs, 0.0)
        assertEquals(listOf(ModeSwitchKind.DROP, ModeSwitchKind.PUMP, ModeSwitchKind.DROP), feedback.starts)
        assertEquals("each running one stopped before the next", 2, feedback.stops)
        controller.advanceTo(ModeSwitchMotion.DROP.end)
        assertNull(controller.shownMode)
    }

    @Test
    fun controller_reduceMotion_noAnimation_butTheVibrationAndSound() {
        controller.motionOn = false
        controller.noteStored(TrainingMode.BODYBUILDING)
        controller.start(ModeSwitchKind.DROP)
        assertNull("nothing runs", controller.kind)
        assertEquals(listOf(ModeSwitchKind.DROP), feedback.starts)
        assertEquals("the new side at once", TrainingMode.POWERLIFTING, controller.shownMode)
        controller.noteStored(TrainingMode.POWERLIFTING)
        assertNull(controller.shownMode)
    }

    @Test
    fun controller_reduceMotion_leavingOrTheBackground_stopsTheSoundAndVibration_once() {
        controller.motionOn = false
        controller.noteStored(TrainingMode.BODYBUILDING)
        controller.start(ModeSwitchKind.DROP)
        controller.finish(ModeSwitchEnd.LEAVE)
        assertEquals(1, feedback.stops)
        controller.finish(ModeSwitchEnd.LEAVE)
        controller.finish(ModeSwitchEnd.HIDDEN)
        assertEquals("stopped once", 1, feedback.stops)
        controller.start(ModeSwitchKind.PUMP)
        controller.finish(ModeSwitchEnd.HIDDEN)
        assertEquals(2, feedback.stops)
        // The clock's end and a skip are not stops of a free sound: there is nothing running to end.
        controller.start(ModeSwitchKind.DROP)
        controller.finish(ModeSwitchEnd.END)
        controller.skip()
        assertEquals(2, feedback.stops)
    }

    @Test
    fun controller_skip_cancelsTheVibration_thenTicks() {
        controller.start(ModeSwitchKind.DROP)
        controller.advanceTo(400.0)
        controller.skip()
        assertEquals(1, feedback.ticks)
        controller.start(ModeSwitchKind.PUMP)
        controller.finish(ModeSwitchEnd.LEAVE)
        assertEquals("only a skip ticks", 1, feedback.ticks)
    }

    // ---- in the app ----

    @Test
    fun theFirstFrame_isTheTimeSinceTheTap() {
        show()
        compose.mainClock.autoAdvance = false
        uptime = 10_000
        flip()
        assertEquals(0.0, controller.tMs, 0.0)
        // The frame that composes the overlay starts its clock; the clock's first frame comes 40 ms after the tap (the
        // frame clock says less): the picture is where the sound and the vibration are, 40 ms in.
        frame()
        assertEquals("nothing of either animation shows at 0", 0.0, controller.tMs, 0.0)
        uptime += 40
        frame()
        assertEquals(40.0, controller.tMs, 0.0)
        // Later frames add their own steps, with no cap.
        val before = controller.tMs
        compose.mainClock.advanceTimeBy(100)
        assertTrue("a late frame jumps ahead: ${controller.tMs}", controller.tMs >= before + 100)
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun theFirstFrame_doesNotOverrideAHold() {
        show()
        compose.mainClock.autoAdvance = false
        uptime = 10_000
        flip()
        controller.hold(200.0)
        uptime += 500
        frame()
        frame()
        assertEquals(200.0, controller.tMs, 0.0)
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun openingSettings_prefetchesTheNextSwitchsSound() {
        show()
        compose.waitUntil(5_000) { feedback.prefetches.isNotEmpty() }
        assertEquals("stored bodybuilding: the next switch is the drop", listOf(ModeSwitchKind.DROP), feedback.prefetches)
    }

    @Test
    fun reduceMotion_leavingSettings_stopsTheSoundAndVibration() {
        show(motion = false)
        flip()
        assertEquals(0, feedback.stops)
        showSettings = false
        compose.waitForIdle()
        assertEquals(1, feedback.stops)
    }

    @Test
    fun reduceMotion_theBackground_stopsTheSoundAndVibration() {
        show(motion = false)
        flip()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertEquals("stopped once, on pause or stop", 1, feedback.stops)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @Test
    fun theStamp_isBelowTheTitle_fromTheImpactToTheEnd() {
        show()
        compose.mainClock.autoAdvance = false
        flip()
        waitStored(TrainingMode.POWERLIFTING)
        var seen = 0
        for (t in listOf(250.0, 270.0, 330.0, 450.0, 640.0, 880.0, 1100.0)) {
            controller.hold(t)
            frame()
            frame()
            val title = compose.onNode(androidx.compose.ui.test.hasText("Settings") and androidx.compose.ui.test.isHeading()).fetchSemanticsNode()
            val stamp = compose.onAllNodes(hasTestTag("mode-stamp"), useUnmergedTree = true).fetchSemanticsNodes().singleOrNull() ?: continue
            seen++
            // In the title's own coordinates: the shake moves, turns and scales both alike.
            val at = title.layoutInfo.coordinates.localPositionOf(stamp.layoutInfo.coordinates, Offset.Zero)
            val titleHeight = title.layoutInfo.coordinates.size.height
            assertTrue("at $t the stamp's top ${at.y} is above the title's bottom $titleHeight", at.y >= titleHeight)
            assertTrue("at $t the stamp starts at the title's left (${at.x})", abs(at.x) < 2f)
        }
        assertEquals("the stamp was there each time", 7, seen)
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun switchOff_storesAtTheTap_theColoursChangeAtTheImpact() {
        show()
        assertEquals(Color(0xFF8DB8FF), accent)
        compose.mainClock.autoAdvance = false
        flip()
        waitStored(TrainingMode.POWERLIFTING)
        assertEquals(ModeSwitchKind.DROP, controller.kind)
        controller.hold(249.0)
        frame()
        assertEquals("still the old side at 249 ms", Color(0xFF8DB8FF), accent)
        // The switch already says off: the screen reader hears the new state at the tap.
        val row = compose.onNodeWithTag("bodybuilding-mode").fetchSemanticsNode()
        assertEquals(ToggleableState.Off, row.config[SemanticsProperties.ToggleableState])
        controller.hold(250.0)
        frame()
        assertEquals("the new side at the impact", Color(0xFF93979D), accent)
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun switchOn_theColoursChangeAtTheThirdBeat() {
        runBlocking { settings.update { it.copy(trainingMode = TrainingMode.POWERLIFTING) } }
        show()
        assertEquals(Color(0xFF93979D), accent)
        compose.mainClock.autoAdvance = false
        flip()
        waitStored(TrainingMode.BODYBUILDING)
        assertEquals(ModeSwitchKind.PUMP, controller.kind)
        controller.hold(749.0)
        frame()
        assertEquals(Color(0xFF93979D), accent)
        controller.hold(750.0)
        frame()
        assertEquals(Color(0xFF8DB8FF), accent)
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun theAnimation_runsToItsEnd_andLeavesNothingDrawn() {
        show()
        flip()
        compose.waitUntil(5_000) { controller.kind == null }
        compose.waitForIdle()
        assertFalse(overlayShown())
        assertEquals(Color(0xFF93979D), accent)
    }

    @Test
    fun aTap_skips_andDoesNothingElse() {
        show()
        val plates = stored().changePlates
        compose.mainClock.autoAdvance = false
        flip()
        frame()
        assertEquals(ModeSwitchKind.DROP, controller.kind)
        compose.onNodeWithText("Change plates").performClick()
        assertNull("skipped", controller.kind)
        assertEquals(1, feedback.stops)
        assertEquals("the skip's tick", 1, feedback.ticks)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals("the tap did nothing else", plates, stored().changePlates)
        assertFalse(overlayShown())
        assertEquals(Color(0xFF93979D), accent)
    }

    @Test
    fun flippingAgain_skipsTheRunningOne_andPlaysTheNewOne() {
        show()
        compose.mainClock.autoAdvance = false
        flip()
        waitStored(TrainingMode.POWERLIFTING)
        repeat(5) { frame() }
        flip()
        assertEquals(ModeSwitchKind.PUMP, controller.kind)
        assertEquals(listOf(ModeSwitchKind.DROP, ModeSwitchKind.PUMP), feedback.starts)
        assertEquals(1, feedback.stops)
        waitStored(TrainingMode.BODYBUILDING)
        compose.mainClock.autoAdvance = true
        compose.waitUntil(5_000) { controller.kind == null }
        compose.waitForIdle()
        assertEquals(Color(0xFF8DB8FF), accent)
    }

    @Test
    fun leavingSettings_endsIt_cleanly() {
        show()
        compose.mainClock.autoAdvance = false
        flip()
        repeat(3) { frame() }
        showSettings = false
        frame()
        assertNull(controller.kind)
        assertEquals(1, feedback.stops)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertFalse(overlayShown())
        assertEquals(Color(0xFF93979D), accent)
    }

    @Test
    fun goingToTheBackground_endsIt_cleanly() {
        show()
        compose.mainClock.autoAdvance = false
        flip()
        repeat(3) { frame() }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertNull(controller.kind)
        assertTrue(feedback.stops >= 1)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertFalse(overlayShown())
    }

    @Test
    fun reduceMotion_drawsNoOverlay_butStillAsksForTheVibrationAndSound() {
        show(motion = false)
        flip()
        assertFalse(overlayShown())
        assertNull(controller.kind)
        assertEquals(listOf(ModeSwitchKind.DROP), feedback.starts)
        compose.waitUntil(5_000) { accent == Color(0xFF93979D) }
    }

    @Test
    fun vibrateOnTapsOff_stillVibrates() {
        runBlocking { settings.update { it.copy(vibrateOnTaps = false) } }
        show()
        flip()
        assertEquals(listOf(ModeSwitchKind.DROP), feedback.starts)
    }

    @Test
    fun theOverlay_isHiddenFromAccessibility() {
        show()
        compose.mainClock.autoAdvance = false
        flip()
        controller.hold(300.0)
        frame()
        val overlay = compose.onNodeWithTag("mode-switch-overlay", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(SemanticsProperties.HideFromAccessibility in overlay.config)
        val stamp = compose.onNodeWithTag("mode-stamp", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue("the stamp says nothing", SemanticsProperties.Text !in stamp.config && SemanticsProperties.ContentDescription !in stamp.config)
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun aRestore_changesTheLook_withNoAnimation() {
        show()
        runBlocking { settings.update { it.copy(trainingMode = TrainingMode.POWERLIFTING) } }
        compose.waitUntil(5_000) { accent == Color(0xFF93979D) }
        assertNull(controller.kind)
        assertTrue(feedback.starts.isEmpty())
        assertFalse(overlayShown())
    }

    @Test
    fun appStart_worksNothingOut_andSettingsLoadsTheSounds() {
        var pools = 0
        val real = ModeSwitchController(AndroidModeSwitchFeedback(context, vibrator = { null }, soundPool = { pools++; defaultSoundPool() }))
        var settingsOpen by mutableStateOf(false)
        compose.setContent {
            ModeTheme(settings.settings, darkTheme = true, motion = true, modeSwitch = real) {
                ModeSwitchHost {
                    if (settingsOpen) SettingsScreen(settings, BackupService(database.db, settings), onBack = {}) else androidx.compose.material3.Text("Plan")
                }
            }
        }
        compose.waitForIdle()
        // At start (any screen but Settings): nothing loaded, worked out or built.
        assertEquals("no sound loaded", 0, pools)
        assertEquals("no frame worked out", 0, ModeSwitchProbe.frames)
        assertEquals("no shapes built", 0, real.artBuilds)
        assertFalse(overlayShown())
        // Settings opens: the sounds load ahead of the tap, and still nothing is worked out.
        settingsOpen = true
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("bodybuilding-mode")).fetchSemanticsNodes().isNotEmpty() && pools == 1 }
        compose.waitForIdle()
        assertEquals("loaded once", 1, pools)
        assertEquals(0, ModeSwitchProbe.frames)
        assertEquals(0, real.artBuilds)
    }

    /**
     * Every frame of both animations keeps time: the overlay's draw and the reveal's (recording the old side, then drawing
     * its picture outside the circle) take under 16 ms together on every frame of the drop and the pump, the moment
     * included, and the reveal is drawn, under Robolectric's frame clock and after two warm-up runs (a phone's code is compiled
     * ahead of time). Robolectric runs no draw pass of its own, so each frame is drawn here, recorded as a phone's main
     * thread records it. A slow draw must repeat in a second measured pair to fail, so one pause of the test's own JVM
     * does not. The whole frame's wall time (the clock step, recomposition, layout and the draw), and the frame
     * at the moment, when the theme and Settings' rows change side, are printed for the report, not asserted: wall time
     * on a shared build machine is too noisy to hold to 16 ms.
     */
    @Test
    fun framesKeepTime_dropAndPump() {
        show()
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        // Each frame's draw: the overlay's, plus the reveal's (recording the old side once, then drawing it outside the
        // circle), summed over the frame.
        val draws = mutableListOf<Long>()
        var overlayNs = 0L
        var revealNs = 0L
        ModeSwitchProbe.onDraw = { overlayNs += it }
        ModeSwitchProbe.onReveal = { revealNs += it }
        class Run(val draws: List<Long>, val frames: List<Long>, val moment: Long, val reveals: Int)
        fun runOnce(): Run {
            draws.clear()
            ModeSwitchProbe.reveals = 0
            val frames = mutableListOf<Long>()
            var moment = 0L
            compose.mainClock.autoAdvance = false
            flip()
            val kind = controller.kind!!
            var guard = 0
            while (controller.kind != null && guard++ < 200) {
                val before = controller.tMs
                val start = System.nanoTime()
                frame()
                val took = System.nanoTime() - start
                if (before < kind.moment && (controller.kind == null || controller.tMs >= kind.moment)) moment = took else frames += took
                overlayNs = 0L
                revealNs = 0L
                val picture = android.graphics.Picture()
                view.draw(picture.beginRecording(view.width, view.height))
                picture.endRecording()
                if (overlayNs > 0L) draws += overlayNs + revealNs
                if (!controller.artReady) compose.waitUntil(2_000) { controller.kind == null || controller.artReady }
            }
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            return Run(draws.toList(), frames, moment, ModeSwitchProbe.reveals)
        }
        runOnce(); runOnce()
        // A pause of the test's own JVM (a collection after the other tests in this class) is not the draw's: a slow
        // frame must show up again in a second measured pair to fail.
        var slowestDraw = Double.MAX_VALUE
        for (attempt in 1..2) {
            System.gc()
            val drop = runOnce()
            val pump = runOnce()
            assertTrue("drop frames drawn: ${drop.draws.size}", drop.draws.size > 50)
            assertTrue("pump frames drawn: ${pump.draws.size}", pump.draws.size > 60)
            assertTrue("the drop's reveal drew the old side: ${drop.reveals} frames", drop.reveals > 0)
            assertTrue("the pump's reveal drew the old side: ${pump.reveals} frames", pump.reveals > 0)
            slowestDraw = (drop.draws + pump.draws).max() / 1_000_000.0
            val slowestFrame = (drop.frames + pump.frames).max() / 1_000_000.0
            println("attempt $attempt: slowest overlay and reveal draw $slowestDraw ms; slowest frame $slowestFrame ms; the moment: drop ${drop.moment / 1e6} ms, pump ${pump.moment / 1e6} ms")
            if (slowestDraw < 16.0) break
        }
        assertTrue("slowest overlay and reveal draw $slowestDraw ms", slowestDraw < 16.0)
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun theVeinsAndFlood_startAtTheMeasuredSwitch_scrolled_narrow_largeText() {
        runBlocking { settings.update { it.copy(trainingMode = TrainingMode.POWERLIFTING) } }
        show(fontScale = 1.3f)
        val before = compose.onNodeWithTag("bodybuilding-mode-switch", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        compose.onRoot().performTouchInput { swipeUp(startY = centerY, endY = centerY - 60f) }
        compose.waitForIdle()
        val bounds = compose.onNodeWithTag("bodybuilding-mode-switch", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("Settings scrolled", bounds.top < before.top - 10)
        compose.mainClock.autoAdvance = false
        flip()
        assertEquals(ModeSwitchKind.PUMP, controller.kind)
        frame()
        val src = controller.source()
        assertNotNull("the switch was measured", src)
        src!!
        val density = context.resources.displayMetrics.density
        val want = Offset(bounds.center.x / density, bounds.center.y / density)
        assertTrue("the pump starts at the switch: $src, want $want", abs(src.x - want.x) < 0.5 && abs(src.y - want.y) < 0.5)
        compose.mainClock.autoAdvance = true
    }

    // ---- held frames for the owner ----

    /** A held frame: as drawn, and (while the reveal is under way) drawn again with no reveal, the new side everywhere. */
    private class Shot(val bitmap: Bitmap, val noReveal: Bitmap?, val widthDp: Double, val heightDp: Double, val source: Offset)

    private fun capture(view: android.view.View): Bitmap =
        Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }

    private fun shot(name: String, kind: ModeSwitchKind, at: Double, dark: Boolean): Shot {
        if (kind == ModeSwitchKind.PUMP) runBlocking { settings.update { it.copy(trainingMode = TrainingMode.POWERLIFTING) } }
        show(dark = dark)
        compose.mainClock.autoAdvance = false
        flip()
        val moment = if (kind == ModeSwitchKind.DROP) ModeSwitchMotion.DROP.impact else ModeSwitchMotion.PUMP.beats[2]
        // Held before the wait, and before the window where the old side is recorded (a draw pass while waiting would
        // record it then); then the stored write, so the switch in the shot says the new side, as the lifter sees it
        // straight after the tap.
        controller.hold(moment - 100)
        waitStored(kind.target)
        // ...and until the screen has it: the switch says the new side before the old side is recorded.
        val want = if (kind.target == TrainingMode.BODYBUILDING) ToggleableState.On else ToggleableState.Off
        val switchSays = { compose.onNodeWithTag("bodybuilding-mode").fetchSemanticsNode().config[SemanticsProperties.ToggleableState] }
        for (i in 0 until 200) {
            if (switchSays() == want) break
            ShadowLooper.idleMainLooper()
            frame()
        }
        assertEquals("the switch says the stored side", want, switchSays())
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        frame()
        compose.waitUntil(5_000) { controller.artReady }
        // The clock is held: these frames only let the switch's own thumb animation settle on the new side.
        repeat(30) { frame() }
        controller.hold(moment - 30)
        frame()
        // Robolectric runs no draw pass: this one is where a phone's frame would record the old side.
        view.draw(android.graphics.Picture().beginRecording(view.width, view.height))
        controller.hold(at)
        frame()
        frame()
        val bitmap = capture(view)
        assertEquals("412 dp wide at 2x", 824, bitmap.width)
        val corner = bitmap.getPixel(0, 0)
        val pixels = IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
        assertTrue("$name rendered blank", pixels.count { it != corner } > pixels.size / 20)
        val g = controller.geometry!!
        val src = controller.pumpSource()
        val underWay = if (kind == ModeSwitchKind.DROP) ModeSwitchMotion.dropFrame(at, g.widthDp, g.heightDp).let { it.revealRadius > 0 && !it.revealFull }
        else ModeSwitchMotion.pumpFrame(at, g.widthDp, g.heightDp, src).let { it.floodRadius > 0 && !it.floodFull }
        var noReveal: Bitmap? = null
        if (underWay) {
            ModeSwitchProbe.revealOff.value = true
            frame()
            noReveal = capture(view)
            ModeSwitchProbe.revealOff.value = false
            frame()
        }
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name-${if (dark) "dark" else "light"}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.mainClock.autoAdvance = true
        return Shot(bitmap, noReveal, g.widthDp, g.heightDp, Offset(src.x.toFloat(), src.y.toFloat()))
    }

    private val Int.r get() = (this shr 16) and 0xFF
    private val Int.g get() = (this shr 8) and 0xFF
    private val Int.b get() = this and 0xFF

    /**
     * The reveal drew the old side outside its circle: against the same frame with no reveal, pixels well outside the
     * circle differ (the old side's rows are not the new side's), and pixels well inside are the same. [band] dp either
     * side of the edge is left out: the drop's circle moves with the shake.
     */
    private fun assertRevealed(s: Shot, centreDp: Offset, radiusDp: Double, band: Double) {
        val plain = assertNotNull("the reveal is under way", s.noReveal).let { s.noReveal!! }
        var outside = 0
        var outsideDiffer = 0
        var inside = 0
        var insideDiffer = 0
        for (y in 0 until s.bitmap.height step 6) for (x in 0 until s.bitmap.width step 6) {
            val d = kotlin.math.hypot(x / 2.0 - centreDp.x, y / 2.0 - centreDp.y)
            val a = s.bitmap.getPixel(x, y)
            val b = plain.getPixel(x, y)
            val differs = abs(a.r - b.r) + abs(a.g - b.g) + abs(a.b - b.b) > 6
            if (d > radiusDp + band) {
                outside++
                if (differs) outsideDiffer++
            } else if (d < radiusDp - band) {
                inside++
                if (differs) insideDiffer++
            }
        }
        assertTrue("outside the circle, the old side: $outsideDiffer of $outside pixels differ from the new side", outside > 0 && outsideDiffer > outside / 20)
        assertTrue("inside the circle, the new side: $insideDiffer of $inside differ", inside > 0 && insideDiffer == 0)
    }

    @Test
    fun shot_dropAtImpact_dark() {
        val at = ModeSwitchMotion.DROP.impact + 20
        val s = shot("mode-switch-drop-impact", ModeSwitchKind.DROP, at, dark = true)
        val f = ModeSwitchMotion.dropFrame(at, s.widthDp, s.heightDp)
        assertRevealed(s, Offset(f.hitX.toFloat(), f.hitY.toFloat()), f.revealRadius, band = 40.0)
    }

    @Test
    fun shot_dropAtImpact_light() {
        val at = ModeSwitchMotion.DROP.impact + 20
        val s = shot("mode-switch-drop-impact", ModeSwitchKind.DROP, at, dark = false)
        val f = ModeSwitchMotion.dropFrame(at, s.widthDp, s.heightDp)
        assertRevealed(s, Offset(f.hitX.toFloat(), f.hitY.toFloat()), f.revealRadius, band = 40.0)
        // Light: ring 1 is dark, so it reads over the punch and once it has gone (the prototype's near-white would be
        // brighter than anything round it here). Sampled on the ring, across its upper half.
        val samples = (235..305 step 7).map { deg ->
            val a = Math.toRadians(deg.toDouble())
            val p = s.bitmap.getPixel(((f.hitX + kotlin.math.cos(a) * f.ring1.radius) * 2).toInt(), ((f.hitY + kotlin.math.sin(a) * f.ring1.radius) * 2).toInt())
            (p.r + p.g + p.b) / 3
        }
        assertTrue("ring 1 in light is dark: $samples", samples.count { it < 110 } >= samples.size * 3 / 4)
    }

    @Test
    fun shot_dropStamp_dark() {
        shot("mode-switch-drop-stamp", ModeSwitchKind.DROP, ModeSwitchMotion.DROP.impact + 200, dark = true)
    }

    @Test
    fun shot_dropStamp_light() {
        val at = ModeSwitchMotion.DROP.impact + 200
        val s = shot("mode-switch-drop-stamp", ModeSwitchKind.DROP, at, dark = false)
        // Light, once the punch has gone: the sparks still alive are the darker orange, not the pale one.
        val f = ModeSwitchMotion.dropFrame(at, s.widthDp, s.heightDp)
        val strong = f.sparks.filter { it.opacity > .45 && (it.x1 + it.x2) in 0.0..(s.bitmap.width - 1.0) && (it.y1 + it.y2) in 0.0..(s.bitmap.height - 1.0) }
        assertTrue("sparks alive at $at", strong.size >= 3)
        val orange = strong.count { sp ->
            val p = s.bitmap.getPixel(((sp.x1 + sp.x2)).toInt(), ((sp.y1 + sp.y2)).toInt())
            p.r - p.b > 60 && p.b < 150
        }
        assertTrue("sparks drawn dark orange: $orange of ${strong.size}", orange >= strong.size / 2)
    }

    @Test
    fun shot_pumpAtThirdBeat_dark() {
        val at = ModeSwitchMotion.PUMP.beats[2] + 120
        val s = shot("mode-switch-pump-third-beat", ModeSwitchKind.PUMP, at, dark = true)
        val f = ModeSwitchMotion.pumpFrame(at, s.widthDp, s.heightDp, Point(s.source.x.toDouble(), s.source.y.toDouble()))
        // The flood's circle scales with the content (about 2% here), so it sits up to about 13 dp off the frame's.
        assertRevealed(s, s.source, f.floodRadius, band = 24.0)
    }

    @Test
    fun shot_pumpAtThirdBeat_light() {
        val at = ModeSwitchMotion.PUMP.beats[2] + 120
        val s = shot("mode-switch-pump-third-beat", ModeSwitchKind.PUMP, at, dark = false)
        val src = Point(s.source.x.toDouble(), s.source.y.toDouble())
        val f = ModeSwitchMotion.pumpFrame(at, s.widthDp, s.heightDp, src)
        // The flood's circle scales with the content (about 2% here), so it sits up to about 13 dp off the frame's.
        assertRevealed(s, s.source, f.floodRadius, band = 24.0)
        // Light: the core veins carry their navy edge. Near a point on each main vein (where its second and third
        // steps meet), a navy pixel.
        val num = Regex("-?\\d+(\\.\\d+)?")
        val mains = ModeSwitchMotion.veins(s.widthDp, s.heightDp, src).filter { it.layer == com.powertracker.domain.motion.VeinPath.LAYER_PULSE }
        val edged = mains.count { v ->
            val n = num.findAll(v.d).map { it.value.toDouble() }.toList()
            val cx = (n[10] * 2).toInt()
            val cy = (n[11] * 2).toInt()
            (-12..12).any { dy ->
                (-12..12).any { dx ->
                    val x = cx + dx
                    val y = cy + dy
                    x in 0 until s.bitmap.width && y in 0 until s.bitmap.height && s.bitmap.getPixel(x, y).let { p -> p.b - p.r > 50 && p.r < 120 }
                }
            }
        }
        assertTrue("navy edges by $edged of ${mains.size} main veins", edged >= mains.size / 2)
    }
}
