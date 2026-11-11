package com.powertracker.platform

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class RestTimerTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val notifications get() = shadowOf(app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
    private var clock = 50_000L
    private var service: ServiceController<RestTimerService>? = null

    @Before
    fun setUp() {
        RestTimer.now = { clock }
        RestTimer.stop(app)
        shadowOf(app).clearStartedServices()
    }

    @After
    fun tearDown() {
        service?.destroy()
        RestTimer.stop(app)
        RestTimer.now = System::currentTimeMillis
    }

    /** Hands the last intent sent to the service to a real service instance, as the system would. */
    private fun deliver(intent: Intent = checkNotNull(shadowOf(app).nextStartedService)): RestTimerService {
        val controller = service ?: Robolectric.buildService(RestTimerService::class.java).create().also { service = it }
        controller.get().onStartCommand(intent, 0, 1)
        return controller.get()
    }

    private fun timerNotification(): Notification? = notifications.getNotification(RestTimerService.NOTIFICATION_ID)

    @Test
    fun `start sets the end time and starts the service`() {
        RestTimer.start(app, 180)

        assertEquals(RestTimerState(endsAtMillis = clock + 180_000, totalSec = 180), RestTimer.state.value)
        val started = shadowOf(app).peekNextStartedService()
        assertNotNull(started)
        assertEquals(RestTimerService::class.java.name, started.component?.className)
    }

    @Test
    fun `stop resets the state`() {
        RestTimer.start(app, 180)
        RestTimer.stop(app)

        assertNull(RestTimer.state.value.endsAtMillis)
        assertFalse(RestTimer.state.value.isRunning(clock))
    }

    @Test
    fun `adjust moves the end time`() {
        RestTimer.start(app, 180)
        RestTimer.adjust(app, 30)
        assertEquals(clock + 210_000, RestTimer.state.value.endsAtMillis)
        RestTimer.adjust(app, -30)
        assertEquals(clock + 180_000, RestTimer.state.value.endsAtMillis)
    }

    @Test
    fun `taking off more than is left ends the rest`() {
        RestTimer.start(app, 20)
        RestTimer.adjust(app, -30)
        assertNull(RestTimer.state.value.endsAtMillis)
    }

    @Test
    fun `adjust does nothing when no timer is running`() {
        RestTimer.adjust(app, 30)
        assertNull(RestTimer.state.value.endsAtMillis)
        assertNull(shadowOf(app).peekNextStartedService())
    }

    @Test
    fun `the service shows an ongoing countdown with plus 30 and skip`() {
        RestTimer.start(app, 180)
        val running = deliver()

        val n = checkNotNull(timerNotification())
        assertEquals("Rest", n.extras.getString(Notification.EXTRA_TITLE))
        assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(n.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertEquals(clock + 180_000, n.`when`)
        assertEquals(listOf("+30 s", "Skip"), n.actions.map { it.title.toString() })
        assertEquals(RestTimerService.CHANNEL_ID, n.channelId)
        assertEquals(NotificationManager.IMPORTANCE_LOW, (app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).getNotificationChannel(RestTimerService.CHANNEL_ID).importance)
        assertTrue(shadowOf(running).isLastForegroundNotificationAttached)
    }

    @Test
    fun `the plus 30 action extends the timer and the notification`() {
        RestTimer.start(app, 180)
        deliver()
        deliver(Intent(app, RestTimerService::class.java).setAction(RestTimerService.ACTION_ADD_30))

        assertEquals(clock + 210_000, RestTimer.state.value.endsAtMillis)
        assertEquals(clock + 210_000, timerNotification()?.`when`)
    }

    @Test
    fun `the skip action ends the rest and removes the notification`() {
        RestTimer.start(app, 180)
        val running = deliver()
        deliver(Intent(app, RestTimerService::class.java).setAction(RestTimerService.ACTION_SKIP))

        assertNull(RestTimer.state.value.endsAtMillis)
        assertNull(timerNotification())
        assertTrue(shadowOf(running).isStoppedBySelf)
    }

    @Test
    fun `at zero the service vibrates once, says rest is over and stops`() {
        RestTimer.start(app, 180)
        val running = deliver()
        val vibrator = shadowOf(running.vibrator())
        assertFalse(vibrator.isVibrating)

        clock += 180_000
        shadowOf(Looper.getMainLooper()).idleFor(180, TimeUnit.SECONDS)

        assertTrue(vibrator.isVibrating)
        assertNull(RestTimer.state.value.endsAtMillis)
        val n = checkNotNull(timerNotification())
        assertEquals("Rest over", n.extras.getString(Notification.EXTRA_TITLE))
        assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT == 0)
        assertTrue(shadowOf(running).isStoppedBySelf)
    }

    @Test
    fun `a timer extended in the app does not go off at the old time`() {
        RestTimer.start(app, 60)
        val running = deliver()
        RestTimer.adjust(app, 30)
        deliver()

        clock += 60_000
        shadowOf(Looper.getMainLooper()).idleFor(60, TimeUnit.SECONDS)
        assertFalse(shadowOf(running.vibrator()).isVibrating)
        assertFalse(shadowOf(running).isStoppedBySelf)

        clock += 30_000
        shadowOf(Looper.getMainLooper()).idleFor(30, TimeUnit.SECONDS)
        assertTrue(shadowOf(running.vibrator()).isVibrating)
    }
}
