package com.powertracker.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.powertracker.data.AppSettings
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.components.LocalVibrateOnTaps
import com.powertracker.ui.theme.LocalMotion
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.rememberSystemMotion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The theme root: [PowertrackerTheme] on the side the stored settings are on, so the Settings switch changes the look
 * at once. Until the settings are read it keeps the side last seen in this process, so a rotation does not flash.
 * It is also where motion is turned on ([LocalMotion]): from the system's animation setting unless [motion] says;
 * and where the "Vibrate on taps" setting reaches the screens ([LocalVibrateOnTaps]).
 */
@Composable
fun ModeTheme(
    settings: Flow<AppSettings>,
    darkTheme: Boolean = isSystemInDarkTheme(),
    motion: Boolean = rememberSystemMotion(),
    content: @Composable () -> Unit,
) {
    val modes = remember(settings) { settings.map { it.trainingMode }.distinctUntilChanged() }
    val mode by modes.collectAsState(initial = LastMode.value)
    LaunchedEffect(mode) { LastMode.value = mode }
    // Off until the settings are read: a buzz the lifter turned off must not slip out at start-up.
    val vibrates = remember(settings) { settings.map { it.vibrateOnTaps }.distinctUntilChanged() }
    val vibrateOnTaps by vibrates.collectAsState(initial = false)
    CompositionLocalProvider(LocalMotion provides motion, LocalVibrateOnTaps provides vibrateOnTaps) {
        PowertrackerTheme(mode = mode, darkTheme = darkTheme, content = content)
    }
}

private object LastMode {
    var value: TrainingMode = TrainingMode.POWERLIFTING
}
