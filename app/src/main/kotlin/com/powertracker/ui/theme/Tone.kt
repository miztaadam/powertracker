package com.powertracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** The meaning a coloured element carries. Pick by meaning, never by looks. */
enum class Tone { NEUTRAL, ACCENT, SUCCESS, WARNING, DANGER, INFO }

/** Foreground (text, icon, stroke) for this tone on a neutral or matching-container surface. */
@Composable
fun Tone.color(): Color {
    val c = PowertrackerTheme.colors
    return when (this) {
        Tone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        Tone.ACCENT -> c.accentText
        Tone.SUCCESS -> c.success
        Tone.WARNING -> c.warning
        Tone.DANGER -> c.danger
        Tone.INFO -> c.info
    }
}

/** Soft tinted background for this tone. */
@Composable
fun Tone.container(): Color {
    val c = PowertrackerTheme.colors
    return when (this) {
        Tone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHigh
        Tone.ACCENT -> c.accentContainer
        Tone.SUCCESS -> c.successContainer
        Tone.WARNING -> c.warningContainer
        Tone.DANGER -> c.dangerContainer
        Tone.INFO -> c.infoContainer
    }
}
