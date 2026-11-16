package com.powertracker.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.ui.design.ShowcaseFrame
import com.powertracker.ui.theme.LocalMotion
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.systemMotionOn
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The motion-off switch (stage 2 plan, Task 1): on only where the app root says the system allows it. */
@RunWith(RobolectricTestRunner::class)
class MotionTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun setScale(scale: Float) {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
    }

    @After
    fun resetScale() = setScale(1f)

    @Test
    fun systemMotion_isOff_onlyWhenAnimationsAreRemoved() {
        assertTrue("unset is the normal scale", systemMotionOn(context))
        setScale(0f)
        assertFalse(systemMotionOn(context))
        setScale(0.5f)
        assertTrue(systemMotionOn(context))
        setScale(1f)
        assertTrue(systemMotionOn(context))
    }

    @Test
    fun motion_isOff_underTheThemeAlone_asTestsAndScreenshotsUseIt() {
        var motion: Boolean? = null
        compose.setContent { PowertrackerTheme { val m = LocalMotion.current; SideEffect { motion = m } } }
        compose.waitForIdle()
        assertEquals(false, motion)
    }

    @Test
    fun screenshotFrame_hasMotionOff() {
        var motion: Boolean? = null
        compose.setContent { ShowcaseFrame(dark = true, tab = null) { val m = LocalMotion.current; SideEffect { motion = m } } }
        compose.waitForIdle()
        assertEquals(false, motion)
    }

    @Test
    fun appRoot_followsTheSystem() {
        var motion: Boolean? = null
        compose.setContent { ModeTheme(flowOf(AppSettings())) { val m = LocalMotion.current; SideEffect { motion = m } } }
        compose.waitForIdle()
        assertEquals(true, motion)
    }

    @Test
    fun appRoot_withAnimationsRemoved_isOff() {
        setScale(0f)
        var motion: Boolean? = null
        compose.setContent { ModeTheme(flowOf(AppSettings())) { val m = LocalMotion.current; SideEffect { motion = m } } }
        compose.waitForIdle()
        assertEquals(false, motion)
    }

    @Test
    fun appRoot_followsAChangeWhileOpen() {
        var motion: Boolean? = null
        compose.setContent { ModeTheme(flowOf(AppSettings())) { val m = LocalMotion.current; SideEffect { motion = m } } }
        compose.waitForIdle()
        assertEquals(true, motion)
        setScale(0f)
        compose.waitForIdle()
        assertEquals(false, motion)
    }

    @Test
    fun appRoot_motionParameter_turnsItOff() {
        var motion: Boolean? = null
        compose.setContent { ModeTheme(flowOf(AppSettings()), motion = false) { val m = LocalMotion.current; SideEffect { motion = m } } }
        compose.waitForIdle()
        assertEquals(false, motion)
    }
}
