package com.powertracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {
    private val settings = SettingsRepository(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun restAlert_defaultsToVibrate() = runTest {
        settings.update { AppSettings() }
        assertEquals(RestAlert.VIBRATE, AppSettings().restAlert)
        assertEquals(RestAlert.VIBRATE, settings.settings.first().restAlert)
    }

    @Test
    fun restAlert_roundTrips() = runTest {
        for (alert in RestAlert.entries) {
            settings.update { it.copy(restAlert = alert) }
            assertEquals(alert, settings.settings.first().restAlert)
        }
    }

    @Test
    fun everySetting_roundTrips() = runTest {
        val all = AppSettings(
            changePlates = true, defaultRpe = 7.5, trainingDays = setOf(kotlinx.datetime.DayOfWeek.SUNDAY), appLock = true,
            restMainSec = 240, restAccessorySec = 75, restAlert = RestAlert.OFF, showStrictPress = true,
        )
        settings.update { all }
        assertEquals(all, settings.settings.first())
    }

    /** Strict press spec §4.2: off by default; the switch is stored and read back through its own flow. */
    @Test
    fun showStrictPress_defaultsOff_andPersists() = runTest {
        settings.update { AppSettings() }
        assertFalse(AppSettings().showStrictPress)
        assertFalse(settings.showStrictPress.first())

        settings.setShowStrictPress(true)
        assertTrue(settings.showStrictPress.first())
        assertTrue(settings.settings.first().showStrictPress)

        settings.setShowStrictPress(false)
        assertFalse(settings.showStrictPress.first())
    }
}
