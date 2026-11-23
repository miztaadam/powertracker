package com.powertracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    /** 1.17.5: the session Finish was tapped for is kept beside the settings, and a change of settings leaves it. */
    @Test
    fun finishedSession_roundTrips_andIsNotASetting() = runTest {
        settings.setFinishedSession(null)
        assertNull(settings.finishedSession.first())

        val note = FinishedSession(sessionId = 12, date = LocalDate(2026, 11, 11), setCount = 14, exerciseCount = 4, newNotes = 1, skipped = 2)
        settings.setFinishedSession(note)
        settings.update { AppSettings() }
        assertEquals(note, settings.finishedSession.first())

        settings.setFinishedSession(null)
        assertNull(settings.finishedSession.first())
    }

    /** A damaged note finishes nothing: it reads as none. */
    @Test
    fun finishedSession_damaged_isNone() {
        val whole = FinishedSession(12, LocalDate(2026, 11, 11), 14, 4, 1, 2)
        assertEquals(whole, FinishedSession.decode(whole.encode()))
        for (bad in listOf("", "12", "12|2026-11-11|14|4|1", "x|2026-11-11|14|4|1|2", "12|yesterday|14|4|1|2", "12|2026-11-11|14|four|1|2", "12|2026-11-11|-1|4|1|2")) {
            assertNull(bad, FinishedSession.decode(bad))
        }
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

    /** Bodybuilding mode spec §6: off by default, neither side ever left, the powerlifting days active. */
    /** Stage 2: "Vibrate on taps" is on by default and stored on its own, apart from the rest alert. */
    @Test
    fun vibrateOnTaps_defaultsOn_andPersists() = runTest {
        settings.update { AppSettings() }
        assertTrue(AppSettings().vibrateOnTaps)
        assertTrue(settings.settings.first().vibrateOnTaps)

        settings.update { it.copy(vibrateOnTaps = false) }
        assertFalse(settings.settings.first().vibrateOnTaps)
        assertEquals(RestAlert.VIBRATE, settings.settings.first().restAlert)

        settings.update { it.copy(vibrateOnTaps = true) }
        assertTrue(settings.settings.first().vibrateOnTaps)
    }

    @Test
    fun trainingMode_defaults() = runTest {
        settings.update { AppSettings() }
        val s = settings.settings.first()
        assertEquals(TrainingMode.POWERLIFTING, s.trainingMode)
        assertNull(s.powerliftingLeftOn)
        assertNull(s.bodybuildingLeftOn)
        assertEquals(s.trainingDays, s.activeTrainingDays)
    }

    @Test
    fun trainingSettings_roundTrip() = runTest {
        val all = AppSettings(
            trainingDays = setOf(DayOfWeek.TUESDAY), trainingMode = TrainingMode.BODYBUILDING,
            bodybuildingTrainingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            powerliftingLeftOn = LocalDate(2026, 11, 1), bodybuildingLeftOn = LocalDate(2026, 9, 30),
        )
        settings.update { all }
        assertEquals(all, settings.settings.first())
        settings.update { it.copy(powerliftingLeftOn = null, bodybuildingLeftOn = null) }
        assertNull(settings.settings.first().powerliftingLeftOn)
        assertNull(settings.settings.first().bodybuildingLeftOn)
    }

    /** Switching stamps the side being left; the first entry to the bodybuilding side copies the training days, once. */
    @Test
    fun setTrainingMode_stampsLeftSide_copiesDaysOnce() = runTest {
        val days = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)
        settings.update { AppSettings(trainingDays = days) }

        settings.setTrainingMode(TrainingMode.BODYBUILDING, LocalDate(2026, 11, 2))
        var s = settings.settings.first()
        assertEquals(TrainingMode.BODYBUILDING, s.trainingMode)
        assertEquals(LocalDate(2026, 11, 2), s.powerliftingLeftOn)
        assertNull(s.bodybuildingLeftOn)
        assertEquals(days, s.bodybuildingTrainingDays)
        assertEquals(days, s.activeTrainingDays)

        // The bodybuilding side's own days, then back to powerlifting: its days are its own again.
        settings.update { it.copy(bodybuildingTrainingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)) }
        settings.setTrainingMode(TrainingMode.POWERLIFTING, LocalDate(2026, 11, 9))
        s = settings.settings.first()
        assertEquals(TrainingMode.POWERLIFTING, s.trainingMode)
        assertEquals(LocalDate(2026, 11, 9), s.bodybuildingLeftOn)
        assertEquals(LocalDate(2026, 11, 2), s.powerliftingLeftOn)
        assertEquals(days, s.activeTrainingDays)

        // Back again: the bodybuilding days are kept, not copied a second time.
        settings.setTrainingMode(TrainingMode.BODYBUILDING, LocalDate(2026, 11, 16))
        s = settings.settings.first()
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), s.bodybuildingTrainingDays)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), s.activeTrainingDays)
        assertEquals(LocalDate(2026, 11, 16), s.powerliftingLeftOn)
        assertEquals(days, s.trainingDays)
    }

    @Test
    fun setTrainingMode_toTheSideAlreadyOn_changesNothing() = runTest {
        settings.update { AppSettings() }
        settings.setTrainingMode(TrainingMode.POWERLIFTING, LocalDate(2026, 11, 2))
        assertEquals(AppSettings(), settings.settings.first())
    }

    @Test
    fun clearLeftOn_forgetsThatSideOnly() = runTest {
        settings.update { AppSettings(powerliftingLeftOn = LocalDate(2026, 10, 1), bodybuildingLeftOn = LocalDate(2026, 8, 1)) }
        settings.clearLeftOn(TrainingMode.BODYBUILDING)
        assertNull(settings.settings.first().bodybuildingLeftOn)
        assertEquals(LocalDate(2026, 10, 1), settings.settings.first().powerliftingLeftOn)
        settings.clearLeftOn(TrainingMode.POWERLIFTING)
        assertNull(settings.settings.first().powerliftingLeftOn)
    }
}
