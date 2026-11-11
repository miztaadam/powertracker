package com.powertracker.ui

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.data.RestAlert
import com.powertracker.data.SettingsRepository
import com.powertracker.platform.RestTimer
import com.powertracker.ui.components.RestTimerBar
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.RestTimerHooks
import com.powertracker.ui.today.rememberRestTimerHooks
import com.powertracker.ui.today.restSeconds
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class RestTimerHooksTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val clock = 70_000L
    private lateinit var settings: SettingsRepository
    private lateinit var hooks: RestTimerHooks

    @Before
    fun setUp() {
        RestTimer.now = { clock }
        RestTimer.stop(app)
        app.getSharedPreferences("rest_timer", Context.MODE_PRIVATE).edit().clear().commit()
        settings = SettingsRepository(app)
        runBlocking { settings.update { AppSettings(restMainSec = 240, restAccessorySec = 75) } }
    }

    @After
    fun tearDown() {
        RestTimer.stop(app)
        RestTimer.now = System::currentTimeMillis
    }

    private fun show() = compose.setContent {
        PowertrackerTheme {
            hooks = rememberRestTimerHooks(settings)
            hooks.restTimer?.let { RestTimerBar(it.remainingSec, it.totalSec, it.onSubtractThirty, it.onAddThirty, it.onSkip) }
        }
    }

    private fun logSet(isMainOrVariation: Boolean) {
        compose.runOnIdle { hooks.onSetLogged(isMainOrVariation) }
        compose.waitUntil(5_000) {
            compose.waitForIdle()
            RestTimer.state.value.endsAtMillis != null
        }
    }

    @Test
    fun `rest length follows the kind of exercise`() {
        val s = AppSettings(restMainSec = 240, restAccessorySec = 75)
        assertEquals(240, s.restSeconds(isMainOrVariation = true))
        assertEquals(75, s.restSeconds(isMainOrVariation = false))
    }

    @Test
    fun `no bar while no rest is running`() {
        show()
        compose.runOnIdle { assertNull(hooks.restTimer) }
    }

    @Test
    fun `a logged main set starts the main rest and shows the bar`() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        show()
        logSet(isMainOrVariation = true)

        assertEquals(clock + 240_000, RestTimer.state.value.endsAtMillis)
        compose.onNodeWithContentDescription("Rest, 4:00 left").assertIsDisplayed()
    }

    @Test
    fun `a logged set hands the rest alert from Settings to the timer`() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        runBlocking { settings.update { it.copy(restAlert = RestAlert.SOUND_AND_VIBRATE) } }
        show()
        logSet(isMainOrVariation = true)

        assertEquals(RestAlert.SOUND_AND_VIBRATE, RestTimer.alert)
    }

    @Test
    fun `a logged accessory set starts the accessory rest`() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        show()
        logSet(isMainOrVariation = false)

        compose.onNodeWithContentDescription("Rest, 1:15 left").assertIsDisplayed()
    }

    @Test
    fun `the bar's buttons change and end the rest`() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        show()
        logSet(isMainOrVariation = true)

        compose.onNodeWithText("+30 s").performClick()
        compose.onNodeWithContentDescription("Rest, 4:30 left").assertIsDisplayed()
        compose.onNodeWithText("−30 s").performClick()
        compose.onNodeWithText("−30 s").performClick()
        compose.onNodeWithContentDescription("Rest, 3:30 left").assertIsDisplayed()
        compose.onNodeWithText("Skip").performClick()
        compose.onNodeWithText("Skip").assertDoesNotExist()
        assertNull(RestTimer.state.value.endsAtMillis)
    }

    @Test
    fun `notification permission is asked for on the first rest only`() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        show()
        logSet(isMainOrVariation = true)
        compose.waitForIdle()
        val asked = shadowOf(compose.activity).lastRequestedPermission
        assertEquals(listOf(Manifest.permission.POST_NOTIFICATIONS), asked?.requestedPermissions?.toList())

        compose.runOnIdle { RestTimer.stop(app) }
        logSet(isMainOrVariation = true)
        compose.waitForIdle()
        assertSame(asked, shadowOf(compose.activity).lastRequestedPermission)
        // Denied or not, the rest runs in the app.
        compose.onNodeWithContentDescription("Rest, 4:00 left").assertIsDisplayed()
    }
}
