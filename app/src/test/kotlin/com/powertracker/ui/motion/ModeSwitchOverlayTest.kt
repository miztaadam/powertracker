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

    /** Notes what the controller asked of the vibration and the sound. */
    private class RecordingFeedback : ModeSwitchFeedback {
        val starts = mutableListOf<ModeSwitchKind>()
        var stops = 0
        var releases = 0
        override fun start(kind: ModeSwitchKind) {
            starts += kind
        }
        override fun stop() {
            stops++
        }
        override fun release() {
            releases++
        }
    }

    private val feedback = RecordingFeedback()
    private val controller = ModeSwitchController(feedback)

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

    // ---- in the app ----

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
    fun appStart_worksNothingOut() {
        var pools = 0
        val real = ModeSwitchController(AndroidModeSwitchFeedback(context, vibrator = { null }, soundPool = { pools++; defaultSoundPool() }))
        compose.setContent {
            ModeTheme(settings.settings, darkTheme = true, motion = true, modeSwitch = real) {
                ModeSwitchHost { SettingsScreen(settings, BackupService(database.db, settings), onBack = {}) }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("bodybuilding-mode")).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        assertEquals("no sound loaded", 0, pools)
        assertEquals("no frame worked out", 0, ModeSwitchProbe.frames)
        assertEquals("no shapes built", 0, ModeSwitchProbe.artBuilds)
        assertFalse(overlayShown())
    }

    /**
     * Every frame of both animations keeps time on the main thread, under Robolectric's frame clock, after two warm-up
     * runs (a phone's code is compiled ahead of time):
     * - the overlay's draw, every frame, well under 16 ms;
     * - the whole frame (the clock step, recomposition, layout, the shake or swell and the reveal) under 16 ms, except
     *   the one frame at the moment, when the theme and Settings' rows change side. That frame does the work a switch
     *   has always done at the tap, and it is printed, not held to 16 ms.
     * Robolectric runs no draw pass of its own, so each frame is drawn here, recorded as a phone's main thread records it.
     */
    @Test
    fun framesKeepTime_dropAndPump() {
        show()
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        val draws = mutableListOf<Long>()
        ModeSwitchProbe.onDraw = { draws += it }
        class Run(val draws: List<Long>, val frames: List<Long>, val moment: Long)
        fun runOnce(): Run {
            draws.clear()
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
                val picture = android.graphics.Picture()
                view.draw(picture.beginRecording(view.width, view.height))
                picture.endRecording()
                if (!controller.artReady) compose.waitUntil(2_000) { controller.kind == null || controller.artReady }
            }
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            return Run(draws.toList(), frames, moment)
        }
        runOnce(); runOnce()
        val drop = runOnce()
        val pump = runOnce()
        assertTrue("drop frames drawn: ${drop.draws.size}", drop.draws.size > 50)
        assertTrue("pump frames drawn: ${pump.draws.size}", pump.draws.size > 60)
        val slowestDraw = (drop.draws + pump.draws).max() / 1_000_000.0
        val slowestFrame = (drop.frames + pump.frames).max() / 1_000_000.0
        println("slowest overlay draw $slowestDraw ms; slowest frame $slowestFrame ms; the moment: drop ${drop.moment / 1e6} ms, pump ${pump.moment / 1e6} ms")
        assertTrue("slowest overlay draw $slowestDraw ms", slowestDraw < 16.0)
        assertTrue("slowest frame $slowestFrame ms", slowestFrame < 16.0)
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

    private fun shot(name: String, kind: ModeSwitchKind, at: Double, dark: Boolean) {
        if (kind == ModeSwitchKind.PUMP) runBlocking { settings.update { it.copy(trainingMode = TrainingMode.POWERLIFTING) } }
        show(dark = dark)
        compose.mainClock.autoAdvance = false
        flip()
        val moment = if (kind == ModeSwitchKind.DROP) ModeSwitchMotion.DROP.impact else ModeSwitchMotion.PUMP.beats[2]
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        controller.hold(moment - 30)
        frame()
        compose.waitUntil(5_000) { controller.artReady }
        frame()
        // Robolectric runs no draw pass: this one is where a phone's frame would record the old side.
        view.draw(android.graphics.Picture().beginRecording(view.width, view.height))
        controller.hold(at)
        frame()
        frame()
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        assertEquals("412 dp wide at 2x", 824, bitmap.width)
        val corner = bitmap.getPixel(0, 0)
        val pixels = IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
        assertTrue("$name rendered blank", pixels.count { it != corner } > pixels.size / 20)
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name-${if (dark) "dark" else "light"}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun shot_dropAtImpact_dark() = shot("mode-switch-drop-impact", ModeSwitchKind.DROP, ModeSwitchMotion.DROP.impact + 20, dark = true)

    @Test
    fun shot_dropAtImpact_light() = shot("mode-switch-drop-impact", ModeSwitchKind.DROP, ModeSwitchMotion.DROP.impact + 20, dark = false)

    @Test
    fun shot_pumpAtThirdBeat_dark() = shot("mode-switch-pump-third-beat", ModeSwitchKind.PUMP, ModeSwitchMotion.PUMP.beats[2] + 120, dark = true)

    @Test
    fun shot_pumpAtThirdBeat_light() = shot("mode-switch-pump-third-beat", ModeSwitchKind.PUMP, ModeSwitchMotion.PUMP.beats[2] + 120, dark = false)
}
