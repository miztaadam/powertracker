package com.powertracker

import android.app.Application
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.platform.AppLock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** MainActivity turns the guide's first-launch gate on (1.16): a new install opens on the guide. */
@RunWith(RobolectricTestRunner::class)
class MainActivityGuideTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        AppLock.reset()
        runBlocking {
            val settings = SettingsRepository(app)
            settings.update { AppSettings() }
            settings.forgetGuideSeen()
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        AppLock.reset()
    }

    @Test
    fun aNewInstall_opensOnTheGuide() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag("guide")).fetchSemanticsNodes().isNotEmpty() }
    }
}
