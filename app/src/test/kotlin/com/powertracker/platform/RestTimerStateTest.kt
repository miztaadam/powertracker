package com.powertracker.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestTimerStateTest {
    private val now = 1_000_000L

    @Test
    fun `remainingSec counts down and never goes below zero`() {
        val state = RestTimerState(endsAtMillis = now + 90_000)
        assertEquals(90, state.remainingSec(now))
        assertEquals(30, state.remainingSec(now + 60_000))
        assertEquals(0, state.remainingSec(now + 90_000))
        assertEquals(0, state.remainingSec(now + 500_000))
    }

    @Test
    fun `remainingSec rounds a part second up so the clock shows zero only at the end`() {
        val state = RestTimerState(endsAtMillis = now + 90_000)
        assertEquals(90, state.remainingSec(now + 1))
        assertEquals(1, state.remainingSec(now + 89_999))
    }

    @Test
    fun `an idle timer has nothing remaining and is not running`() {
        val idle = RestTimerState(endsAtMillis = null)
        assertEquals(0, idle.remainingSec(now))
        assertFalse(idle.isRunning(now))
        assertNull(idle.plus(30, now).endsAtMillis)
    }

    @Test
    fun `plus 30 extends the end time`() {
        val state = RestTimerState(endsAtMillis = now + 90_000).plus(30, now)
        assertEquals(now + 120_000, state.endsAtMillis)
        assertEquals(120, state.remainingSec(now))
    }

    @Test
    fun `plus minus 30 shortens the rest`() {
        val state = RestTimerState(endsAtMillis = now + 90_000).plus(-30, now)
        assertEquals(60, state.remainingSec(now))
    }

    @Test
    fun `plus minus 30 never sets the end time before now`() {
        val state = RestTimerState(endsAtMillis = now + 10_000).plus(-30, now)
        assertEquals(now, state.endsAtMillis)
        assertEquals(0, state.remainingSec(now))
        assertFalse(state.isRunning(now))
    }

    @Test
    fun `running until the end time`() {
        val state = RestTimerState(endsAtMillis = now + 1_000)
        assertTrue(state.isRunning(now))
        assertFalse(state.isRunning(now + 1_000))
    }

    @Test
    fun `total grows with added time so progress never passes full`() {
        val started = RestTimerState(endsAtMillis = now + 180_000, totalSec = 180)
        assertEquals(210, started.plus(30, now).totalSec)
        assertEquals(180, started.plus(-30, now).totalSec)
        // Half way through, +30 s leaves 120 s of a 180 s rest: the total stays.
        assertEquals(180, started.plus(30, now + 90_000).totalSec)
    }
}
