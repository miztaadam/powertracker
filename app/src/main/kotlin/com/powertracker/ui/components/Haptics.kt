package com.powertracker.ui.components

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * The stored "Vibrate on taps" setting, as [com.powertracker.ui.ModeTheme] provides it. Off by default, and off there
 * until the settings are read, so nothing outside the app root (previews, tests that do not ask) buzzes.
 */
val LocalVibrateOnTaps = staticCompositionLocalOf { false }

/**
 * The main actions' buzz (owner decision 2): [tick] for Log set, Missed, Apply, Add to plan or today and each stepper
 * step; [best] for a new best. Both do nothing while "Vibrate on taps" is off. Each call goes with something the
 * lifter sees, so it is made where the action is accepted, not on every press.
 *
 * Kept by [rememberHaptics] for the life of the screen: it reads the setting and the phone's feedback when called, so
 * a lambda that captured it once still follows the switch.
 */
class Haptics internal constructor(private val feedback: State<HapticFeedback>, private val on: State<Boolean>) {
    /** One short tick. A light click, which every phone this app runs on (API 26 and up) has. */
    fun tick() = perform(HapticFeedbackType.ContextClick)

    /**
     * The stronger buzz for a new best: the phone's "confirm" pattern (a double pulse on most phones). Before Android 11
     * the platform has no confirm and androidx would play an ordinary key click, so those phones get the long-press buzz.
     */
    fun best() = perform(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackType.Confirm else HapticFeedbackType.LongPress)

    private fun perform(type: HapticFeedbackType) {
        if (on.value) feedback.value.performHapticFeedback(type)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val feedback = rememberUpdatedState(LocalHapticFeedback.current)
    val on = rememberUpdatedState(LocalVibrateOnTaps.current)
    return remember { Haptics(feedback, on) }
}
