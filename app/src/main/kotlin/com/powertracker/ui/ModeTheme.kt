package com.powertracker.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.powertracker.data.AppSettings
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The theme root: [PowertrackerTheme] on the side the stored settings are on, so the Settings switch changes the look
 * at once. Until the settings are read it keeps the side last seen in this process, so a rotation does not flash.
 */
@Composable
fun ModeTheme(settings: Flow<AppSettings>, darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val modes = remember(settings) { settings.map { it.trainingMode }.distinctUntilChanged() }
    val mode by modes.collectAsState(initial = LastMode.value)
    LaunchedEffect(mode) { LastMode.value = mode }
    PowertrackerTheme(mode = mode, darkTheme = darkTheme, content = content)
}

private object LastMode {
    var value: TrainingMode = TrainingMode.POWERLIFTING
}
