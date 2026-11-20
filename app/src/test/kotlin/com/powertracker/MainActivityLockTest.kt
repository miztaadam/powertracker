package com.powertracker

import android.app.Application
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.platform.AppLock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The app lock as the user meets it: what MainActivity shows, and when it asks. */
@RunWith(RobolectricTestRunner::class)
class MainActivityLockTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private var clock = 1_000_000L
    private var prompts = 0
    private var unlock: (() -> Unit)? = null
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        AppLock.reset()
        AppLock.now = { clock }
        AppLock.deviceLockAvailable = { true }
        AppLock.prompt = { _, onUnlocked ->
            prompts++
            unlock = onUnlocked
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        AppLock.reset()
    }

    private fun launch(appLock: Boolean): ActivityScenario<MainActivity> {
        runBlocking {
            val settings = SettingsRepository(app)
            settings.update { AppSettings(appLock = appLock) }
            // The empty database is a new user: this device has seen the guide, so the first-launch gate stays shut.
            settings.markGuideSeen()
        }
        return ActivityScenario.launch(MainActivity::class.java).also { scenario = it }
    }

    private fun shown(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun awaitShown(text: String) = compose.waitUntil(5_000) { shown(text) }

    @Test
    fun `the activity can host the biometric prompt`() {
        launch(appLock = false).onActivity { assertTrue((it as Any) is FragmentActivity) }
    }

    @Test
    fun `with app lock off the app opens straight away`() {
        launch(appLock = false)

        awaitShown("Coach")
        assertEquals(0, prompts)
        assertTrue(!shown("Unlock"))
    }

    @Test
    fun `with app lock on a cold start hides the app and asks to unlock`() {
        launch(appLock = true)

        awaitShown("Unlock")
        assertTrue("the log must stay hidden", !shown("Coach"))
        assertEquals(1, prompts)

        compose.runOnIdle { checkNotNull(unlock)() }
        awaitShown("Coach")
        assertTrue(!shown("Unlock"))
    }

    @Test
    fun `the unlock button asks again after a cancelled prompt`() {
        launch(appLock = true)
        awaitShown("Unlock")

        compose.onNodeWithText("Unlock").performClick()

        assertEquals(2, prompts)
    }

    @Test
    fun `coming back after a minute locks again, sooner does not`() {
        val activity = launch(appLock = true)
        awaitShown("Unlock")
        compose.runOnIdle { checkNotNull(unlock)() }
        awaitShown("Coach")

        activity.moveToState(Lifecycle.State.CREATED)
        clock += 30_000
        activity.moveToState(Lifecycle.State.RESUMED)
        awaitShown("Coach")
        assertEquals(1, prompts)

        activity.moveToState(Lifecycle.State.CREATED)
        clock += 60_000
        activity.moveToState(Lifecycle.State.RESUMED)
        awaitShown("Unlock")
        assertTrue(!shown("Coach"))
        assertEquals(2, prompts)
    }

    @Test
    fun `a phone without a screen lock gets a message and is let in`() {
        AppLock.deviceLockAvailable = { false }
        launch(appLock = true)

        awaitShown("This phone has no screen lock")
        assertEquals(0, prompts)
        assertTrue(!shown("Coach"))

        compose.onNodeWithText("Continue").performClick()
        awaitShown("Coach")
    }
}
