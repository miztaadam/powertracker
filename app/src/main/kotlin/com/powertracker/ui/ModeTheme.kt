package com.powertracker.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.powertracker.data.AppSettings
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.components.LocalVibrateOnTaps
import com.powertracker.ui.motion.AndroidModeSwitchFeedback
import com.powertracker.ui.motion.LocalModeSwitch
import com.powertracker.ui.motion.ModeSwitchController
import com.powertracker.ui.theme.LocalMotion
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.rememberSystemMotion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The theme root: [PowertrackerTheme] on the side the stored settings are on. Until the settings are read it keeps the
 * side last seen in this process, so a rotation does not flash.
 * It is also where motion is turned on ([LocalMotion]): from the system's animation setting unless [motion] says;
 * and where the "Vibrate on taps" setting reaches the screens ([LocalVibrateOnTaps]).
 *
 * It holds the mode switch animations' controller ([LocalModeSwitch], 1.15). While one runs, the theme shows the
 * controller's side, which lags the stored side until the drop's impact or the pump's third beat; with motion off the
 * Settings switch changes the look at once. Tests pass their own [modeSwitch], or null for none.
 */
@Composable
fun ModeTheme(
    settings: Flow<AppSettings>,
    darkTheme: Boolean = isSystemInDarkTheme(),
    motion: Boolean = rememberSystemMotion(),
    modeSwitch: ModeSwitchController? = rememberModeSwitchController(),
    content: @Composable () -> Unit,
) {
    val modes = remember(settings) { settings.map { it.trainingMode }.distinctUntilChanged() }
    val mode by modes.collectAsState(initial = LastMode.value)
    LaunchedEffect(mode) { LastMode.value = mode }
    SideEffect {
        modeSwitch?.motionOn = motion
        modeSwitch?.noteStored(mode)
    }
    val shown = modeSwitch?.shownMode ?: mode
    // Off until the settings are read: a buzz the lifter turned off must not slip out at start-up.
    val vibrates = remember(settings) { settings.map { it.vibrateOnTaps }.distinctUntilChanged() }
    val vibrateOnTaps by vibrates.collectAsState(initial = false)
    CompositionLocalProvider(LocalMotion provides motion, LocalVibrateOnTaps provides vibrateOnTaps, LocalModeSwitch provides modeSwitch) {
        PowertrackerTheme(mode = shown, darkTheme = darkTheme, content = content)
    }
}

/** The app's controller, with the real vibration and sound; neither is touched until the first switch. */
@Composable
fun rememberModeSwitchController(): ModeSwitchController {
    val context = LocalContext.current.applicationContext
    val controller = remember(context) { ModeSwitchController(AndroidModeSwitchFeedback(context)) }
    DisposableEffect(controller) { onDispose { controller.dispose() } }
    return controller
}

private object LastMode {
    var value: TrainingMode = TrainingMode.POWERLIFTING
}
