package com.powertracker.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.powertracker.MainActivity
import com.powertracker.R
import java.util.Date

/**
 * Keeps the rest countdown alive while the phone is locked or the app is in the background: an
 * ongoing notification that counts down, one buzz at zero, then it stops itself. It holds no time
 * of its own; every command makes it look at [RestTimer.state] again.
 */
class RestTimerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val check = Runnable { sync() }
    private var foreground = false
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        alive = true
        val channel = NotificationChannel(CHANNEL_ID, "Rest timer", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Counts down your rest between sets"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        notificationManager().createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ADD_30 -> RestTimer.shift(30)
            ACTION_SKIP -> RestTimer.clear()
        }
        sync()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(check)
        releaseWakeLock()
        alive = false
        super.onDestroy()
    }

    /** Brings the notification and the wake-up at zero in line with [RestTimer.state]. */
    private fun sync() {
        handler.removeCallbacks(check)
        val state = RestTimer.state.value
        val now = RestTimer.now()
        val endsAt = state.endsAtMillis
        when {
            endsAt == null -> quit()
            !state.isRunning(now) -> finish()
            enterForeground(countdown(endsAt)) -> {
                val left = endsAt - now
                holdWakeLock(left)
                handler.postDelayed(check, left)
            }
            else -> stopSelf() // Not allowed to run in the foreground: the countdown in the app carries on alone.
        }
    }

    /** The rest was skipped: leave without a trace. */
    private fun quit() {
        // A service started as a foreground service must show itself once before it may stop.
        if (!foreground) enterForeground(restOver())
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        foreground = false
        stopSelf()
    }

    /** Zero: buzz once, swap the countdown for a quiet "Rest over" and stop. */
    private fun finish() {
        buzz()
        RestTimer.clear()
        if (!foreground) enterForeground(restOver())
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        foreground = false
        runCatching { notificationManager().notify(NOTIFICATION_ID, restOver()) }
        stopSelf()
    }

    private fun enterForeground(notification: Notification): Boolean = try {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        foreground = true
        true
    } catch (e: RuntimeException) {
        // E.g. ForegroundServiceStartNotAllowedException; a missing notification must never take the app down.
        false
    }

    private fun base(): NotificationCompat.Builder = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_rest)
        .setColor(ContextCompat.getColor(this, R.color.notification_accent))
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Nothing private here, and the lock screen is where it is read.
        .setOnlyAlertOnce(true)
        .setSilent(true)

    private fun countdown(endsAt: Long): Notification = base()
        .setContentTitle("Rest")
        .setContentText("Next set at ${DateFormat.getTimeFormat(this).format(Date(endsAt))}")
        .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        .setOngoing(true)
        .setShowWhen(true)
        .setWhen(endsAt)
        .setUsesChronometer(true)
        .setChronometerCountDown(true)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .addAction(0, "+30 s", command(ACTION_ADD_30, 1))
        .addAction(0, "Skip", command(ACTION_SKIP, 2))
        .build()

    private fun restOver(): Notification = base()
        .setContentTitle("Rest over")
        .setContentText("Time for your next set")
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setAutoCancel(true)
        .setTimeoutAfter(REST_OVER_SHOWN_MS)
        .build()

    private fun command(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this, requestCode, Intent(this, RestTimerService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun buzz() {
        val effect = VibrationEffect.createWaveform(BUZZ, -1)
        runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                vibrator().vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
            } else {
                // Alarm usage, so a phone on silent in a gym bag still buzzes.
                vibrator().vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            }
        }
    }

    internal fun vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    /**
     * A foreground service does not keep the processor awake on its own, and a sleeping phone would
     * buzz late. Held only for the length of the rest; the timeout releases it if anything goes wrong.
     */
    private fun holdWakeLock(forMillis: Long) {
        val lock = wakeLock ?: (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "powertracker:rest-timer")
            .apply { setReferenceCounted(false) }
            .also { wakeLock = it }
        runCatching { lock.acquire(forMillis + WAKE_MARGIN_MS) }
    }

    private fun releaseWakeLock() {
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
    }

    private fun notificationManager() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "rest_timer"
        const val NOTIFICATION_ID = 14
        const val ACTION_ADD_30 = "com.powertracker.rest.ADD_30"
        const val ACTION_SKIP = "com.powertracker.rest.SKIP"

        /** True between onCreate and onDestroy, so [RestTimer] never starts the service just to tell it to stop. */
        @Volatile
        internal var alive = false
            private set

        private val BUZZ = longArrayOf(0, 300, 150, 300)
        private const val WAKE_MARGIN_MS = 5_000L
        private const val REST_OVER_SHOWN_MS = 60_000L
    }
}
