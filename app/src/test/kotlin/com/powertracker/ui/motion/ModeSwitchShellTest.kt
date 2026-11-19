package com.powertracker.ui.motion

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.BackupService
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.TrainingMode
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.AppShell
import com.powertracker.ui.ModeTheme
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The mode switch animations in the real shell (1.15): opened from Plan's Settings, over the navigation host. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w412dp-h915dp-xhdpi")
class ModeSwitchShellTest {
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var settings: SettingsRepository

    private class RecordingFeedback : ModeSwitchFeedback {
        val starts = mutableListOf<ModeSwitchKind>()
        var stops = 0
        override fun start(kind: ModeSwitchKind) {
            starts += kind
        }
        override fun stop() {
            stops++
        }
        override fun skip() = stop()
        override fun prefetch(next: ModeSwitchKind) = Unit
        override fun release() = Unit
    }

    private val feedback = RecordingFeedback()
    private val controller = ModeSwitchController(feedback)
    private var accent = Color.Unspecified

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        settings = SettingsRepository(context)
        runBlocking { settings.update { AppSettings(trainingMode = TrainingMode.BODYBUILDING) } }
    }

    private fun stored() = runBlocking { settings.settings.first().trainingMode }

    private fun has(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun openSettings() {
        val db = database.db
        compose.setContent {
            ModeTheme(settings.settings, darkTheme = true, motion = true, modeSwitch = controller) {
                val a = PowertrackerTheme.colors.accent
                SideEffect { accent = a }
                AppShell(TrainingRepository(db), AdviceRepository(db), settings, BackupService(db, settings))
            }
        }
        compose.waitUntil(10_000) { has("tab-plan") }
        compose.onNodeWithTag("tab-plan").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasContentDescription("Settings")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.waitUntil(10_000) { has("bodybuilding-mode") }
    }

    @Test
    fun shell_systemBack_midDrop_endsItCleanly_onTheNewSide() {
        openSettings()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("bodybuilding-mode").performClick()
        assertEquals(ModeSwitchKind.DROP, controller.kind)
        repeat(4) { compose.mainClock.advanceTimeByFrame() }
        assertTrue("drawn over the shell", has("mode-switch-overlay"))
        // The system back gesture: not a tap, so it is not a skip; it leaves Settings.
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.mainClock.advanceTimeByFrame()
        assertNull("ended on leaving Settings", controller.kind)
        assertEquals(1, feedback.stops)
        compose.mainClock.autoAdvance = true
        compose.waitUntil(5_000) { stored() == TrainingMode.POWERLIFTING }
        compose.waitUntil(5_000) { accent == Color(0xFF93979D) }
        compose.waitForIdle()
        assertTrue("nothing left drawn", !has("mode-switch-overlay"))
        assertEquals(listOf(ModeSwitchKind.DROP), feedback.starts)
    }

    @Test
    fun shell_theOverlayCoversTheWholeWindow() {
        openSettings()
        val g = controller.geometry
        val view = compose.activity.window.decorView
        assertTrue("measured", g != null)
        assertEquals(view.width.toFloat(), g!!.size.width, 1f)
        assertEquals(view.height.toFloat(), g.size.height, 1f)
    }
}
