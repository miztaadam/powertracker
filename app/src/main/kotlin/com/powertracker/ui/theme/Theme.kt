package com.powertracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.powertracker.domain.model.TrainingMode

/** The side the app is on, for the few things that differ by side (the mode label). Set by [PowertrackerTheme]. */
val LocalTrainingMode = staticCompositionLocalOf { TrainingMode.POWERLIFTING }

val LocalCardShadow = staticCompositionLocalOf { DarkCardShadow }

/** The soft card's shadow for a side and scheme. Both sides use the same shadows; only the scheme picks. */
@Suppress("UNUSED_PARAMETER")
fun cardShadowFor(mode: TrainingMode, darkTheme: Boolean): CardShadow = if (darkTheme) DarkCardShadow else LightCardShadow

/**
 * The brand theme. Android 12 dynamic colour is deliberately not used: the palette is part of the
 * product and state colours must mean the same thing on every phone. [mode] picks between the powerlifting
 * side's grey and graphite and the bodybuilding side's blue (bodybuilding mode spec §7.2).
 */
@Composable
fun PowertrackerTheme(
    mode: TrainingMode = TrainingMode.POWERLIFTING,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val blue = mode == TrainingMode.BODYBUILDING
    val pt = when {
        blue && darkTheme -> BodybuildingDarkPtColors
        blue -> BodybuildingLightPtColors
        darkTheme -> DarkPtColors
        else -> LightPtColors
    }
    val scheme = when {
        blue && darkTheme -> BodybuildingDarkColorScheme
        blue -> BodybuildingLightColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    CompositionLocalProvider(
        LocalPtColors provides pt,
        LocalTrainingMode provides mode,
        LocalCardShadow provides cardShadowFor(mode, darkTheme),
    ) {
        MaterialTheme(
            colorScheme = scheme,
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

    /** The soft card's shadow for the current scheme. */
    val cardShadow: CardShadow
        @Composable @ReadOnlyComposable get() = LocalCardShadow.current
}
