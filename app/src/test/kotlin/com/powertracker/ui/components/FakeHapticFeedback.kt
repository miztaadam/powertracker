package com.powertracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Stands in for the phone's vibrator: records each buzz asked for, in order. */
class FakeHapticFeedback : HapticFeedback {
    val calls = mutableListOf<HapticFeedbackType>()

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        calls += hapticFeedbackType
    }

    val ticks get() = calls.count { it == HapticFeedbackType.ContextClick }
}

/** [content] with [fake] as the vibrator and "Vibrate on taps" set to [vibrateOnTaps]. */
@Composable
fun WithHaptics(fake: FakeHapticFeedback, vibrateOnTaps: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalHapticFeedback provides fake, LocalVibrateOnTaps provides vibrateOnTaps, content = content)
}
