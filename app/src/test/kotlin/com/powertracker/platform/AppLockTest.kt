package com.powertracker.platform

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLockTest {
    private var clock = 1_000_000L

    @Before
    fun setUp() {
        AppLock.reset()
        AppLock.now = { clock }
    }

    @After
    fun tearDown() = AppLock.reset()

    @Test
    fun `a new process is locked`() {
        assertTrue(AppLock.needsUnlock())
        assertEquals(LockGate.LOCKED, AppLock.gate(enabled = true, deviceLockAvailable = true))
    }

    @Test
    fun `unlocking opens the app`() {
        AppLock.markUnlocked()
        assertFalse(AppLock.needsUnlock())
        assertEquals(LockGate.OPEN, AppLock.gate(enabled = true, deviceLockAvailable = true))
    }

    @Test
    fun `a short trip to the background does not lock again`() {
        AppLock.markUnlocked()
        AppLock.markStopped()
        clock += 59_999
        assertFalse(AppLock.needsUnlock())
    }

    @Test
    fun `sixty seconds in the background locks again`() {
        AppLock.markUnlocked()
        AppLock.markStopped()
        clock += 60_000
        assertTrue(AppLock.needsUnlock())
    }

    @Test
    fun `the lock stays until the next unlock, then the timer starts afresh`() {
        AppLock.markUnlocked()
        AppLock.markStopped()
        clock += 120_000
        assertTrue(AppLock.needsUnlock())
        // Leaving again while locked must not open the app.
        AppLock.markStopped()
        assertTrue(AppLock.needsUnlock())

        AppLock.markUnlocked()
        assertFalse(AppLock.needsUnlock())
        AppLock.markStopped()
        clock += 1_000
        assertFalse(AppLock.needsUnlock())
    }

    @Test
    fun `the gate waits until the setting is known`() {
        assertEquals(LockGate.LOADING, AppLock.gate(enabled = null, deviceLockAvailable = true))
    }

    @Test
    fun `app lock off is always open`() {
        assertEquals(LockGate.OPEN, AppLock.gate(enabled = false, deviceLockAvailable = true))
    }

    @Test
    fun `a phone without a screen lock is not shut out`() {
        assertEquals(LockGate.NO_DEVICE_LOCK, AppLock.gate(enabled = true, deviceLockAvailable = false))
    }

    @Test
    fun `turning app lock on does not lock the session it was turned on in`() {
        // The app was opened with the lock off...
        assertEquals(LockGate.OPEN, AppLock.gate(enabled = false, deviceLockAvailable = true))
        AppLock.markUnlocked()
        // ...and the switch is flipped in Settings.
        assertEquals(LockGate.OPEN, AppLock.gate(enabled = true, deviceLockAvailable = true))
    }
}
