package com.powertracker.testing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.RobolectricTestRunner

/**
 * A settings write that a test's screen began and did not wait for must not block the test after it
 * ([TestDatabase], `forgetFinishedSession`). Two tests, in name order: the first ends as
 * `ExercisesScreenTest`'s Keep logging does, the second is any test's set-up.
 *
 * Without the rule's wait, the second test's write never gets through: the store hands the first write's change to the
 * main thread, the test ends before the main thread runs it, and every later write queues behind it. In a set-up, which
 * has no time limit, that was a hang (CI, 2026-11-24).
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(RobolectricTestRunner::class)
class SettingsWriteLeftInFlightTest {
    @get:Rule
    val database = TestDatabase()

    private val settings = SettingsRepository(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun a_endsWithAWriteBegunOnTheMainThread() {
        // As a view model does it: on the main dispatcher, and nobody waits.
        CoroutineScope(Dispatchers.Main.immediate).launch { settings.setFinishedSession(null) }
        // Long enough for the store's own thread to pick the write up and hand its change to the main thread.
        Thread.sleep(300)
    }

    @Test
    fun b_theNextTestsWrite_goesThrough() {
        val written = try {
            runBlocking { withTimeout(5_000) { settings.update { it } } }
            true
        } catch (e: TimeoutCancellationException) {
            false
        }
        assertTrue("a settings write left in flight by the test before blocked this one", written)
    }
}
