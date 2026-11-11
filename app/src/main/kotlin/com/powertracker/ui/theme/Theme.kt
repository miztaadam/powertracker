package com.powertracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The brand theme. Android 12 dynamic colour is deliberately not used: the palette is part of the
 * product and state colours must mean the same thing on every phone.
 */
@Composable
fun PowertrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPtColors provides if (darkTheme) DarkPtColors else LightPtColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = PowertrackerTypography,
            shapes = PowertrackerShapes,
            content = content,
        )
    }
}

object PowertrackerTheme {
    /** Brand colours beyond the Material scheme (accent fill, state and lift colours). */
    val colors: PtColors
        @Composable @ReadOnlyComposable get() = LocalPtColors.current
}
