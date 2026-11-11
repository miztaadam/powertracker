package com.powertracker.platform

import kotlin.math.max

/**
 * The rest countdown: when it ends (null when no rest is running) and how long the rest is in all,
 * for the progress line. Pure, so the clock is always passed in.
 */
data class RestTimerState(val endsAtMillis: Long?, val totalSec: Int = 0) {

    /** Whole seconds left, rounded up so the clock reads 0:00 only when the rest is really over. */
    fun remainingSec(now: Long): Int {
        val left = (endsAtMillis ?: return 0) - now
        return if (left <= 0) 0 else ((left + 999) / 1000).toInt()
    }

    fun isRunning(now: Long): Boolean = endsAtMillis != null && endsAtMillis > now

    /** Moves the end by [sec] seconds (negative shortens the rest), never to before [now]. */
    fun plus(sec: Int, now: Long): RestTimerState {
        val moved = RestTimerState(max((endsAtMillis ?: return this) + sec * 1000L, now), totalSec)
        return moved.copy(totalSec = max(totalSec, moved.remainingSec(now)))
    }
}
