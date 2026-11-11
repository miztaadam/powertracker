package com.powertracker.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The one rest timer of the app. [state] is the truth both the Today screen and [RestTimerService]
 * read; the service only adds what needs the system: the notification and the buzz at zero. If the
 * service cannot run, the countdown in the app still works.
 */
object RestTimer {
    private val _state = MutableStateFlow(RestTimerState(null))
    val state: StateFlow<RestTimerState> = _state.asStateFlow()

    /** The clock; tests replace it. */
    @Volatile
    internal var now: () -> Long = System::currentTimeMillis

    /** Starts a rest of [seconds], replacing any that is running. */
    fun start(ctx: Context, seconds: Int) {
        if (seconds <= 0) return stop(ctx)
        _state.value = RestTimerState(now() + seconds * 1000L, seconds)
        // Rests start from a tap in the app, where starting a foreground service is allowed.
        runCatching { ContextCompat.startForegroundService(ctx, Intent(ctx, RestTimerService::class.java)) }
    }

    /** Adds or takes off time. Taking off all that is left ends the rest, without the buzz. */
    fun adjust(ctx: Context, deltaSec: Int) {
        if (!shift(deltaSec)) return
        if (_state.value.endsAtMillis == null) stop(ctx) else sync(ctx)
    }

    /** Ends the rest now (Skip). */
    fun stop(ctx: Context) {
        clear()
        sync(ctx)
    }

    /** Applies [deltaSec] to a running rest; false when none is running. Clears the state when nothing is left. */
    internal fun shift(deltaSec: Int): Boolean {
        val t = now()
        val current = _state.value
        if (!current.isRunning(t)) return false
        val next = current.plus(deltaSec, t)
        _state.value = if (next.isRunning(t)) next else RestTimerState(null)
        return true
    }

    internal fun clear() {
        _state.value = RestTimerState(null)
    }

    /** Tells a live service to look at [state] again. A service that is not up yet reads it when it starts. */
    private fun sync(ctx: Context) {
        if (!RestTimerService.alive) return
        runCatching { ctx.startService(Intent(ctx, RestTimerService::class.java)) }
    }
}
