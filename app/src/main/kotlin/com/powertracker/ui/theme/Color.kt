package com.powertracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The one signature accent ("volt"). Fills use it in both themes; ink sits on top. */
val Volt = Color(0xFFC9F73A)
val VoltInk = Color(0xFF121700)

/** Brand colours Material's scheme has no slot for. Read through [PowertrackerTheme.colors]. */
@Immutable
data class PtColors(
    /** Accent as a fill (buttons, progress, selected states). */
    val accent: Color,
    val onAccent: Color,
    /** Accent as text or icon on a neutral surface. */
    val accentText: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    /** Tertiary text: captions, units, axis labels. */
    val muted: Color,
    /** PR, progress, completed. */
    val success: Color,
    val successContainer: Color,
    /** Stall, caution. */
    val warning: Color,
    val warningContainer: Color,
    /** Deload, failed rep, destructive. */
    val danger: Color,
    val dangerContainer: Color,
    /** Neutral coach information (weak point, re-entry). */
    val info: Color,
    val infoContainer: Color,
    /** Fixed chart identity per lift. Never reuse for state. */
    val squat: Color,
    val bench: Color,
    val deadlift: Color,
    /** Hairlines between rows and around cards. */
    val hairline: Color,
)

val DarkPtColors = PtColors(
    accent = Volt,
    onAccent = VoltInk,
    accentText = Volt,
    accentContainer = Color(0xFF25300A),
    onAccentContainer = Color(0xFFDDFB86),
    muted = Color(0xFF959BA4),
    success = Color(0xFF3DD68C),
    successContainer = Color(0xFF0F2A1D),
    warning = Color(0xFFFFB627),
    warningContainer = Color(0xFF33250A),
    danger = Color(0xFFFF6B6B),
    dangerContainer = Color(0xFF3A1416),
    info = Color(0xFF6AA8FA),
    infoContainer = Color(0xFF10233F),
    squat = Color(0xFF3987E5),
    bench = Color(0xFFD95926),
    deadlift = Color(0xFF199E70),
    hairline = Color(0xFF262A30),
)

val LightPtColors = PtColors(
    accent = Volt,
    onAccent = VoltInk,
    accentText = Color(0xFF3F6100),
    accentContainer = Color(0xFFE6F9A8),
    onAccentContainer = Color(0xFF2A3F00),
    muted = Color(0xFF595E66),
    success = Color(0xFF0F6B3D),
    successContainer = Color(0xFFDDF5E7),
    warning = Color(0xFF7A5200),
    warningContainer = Color(0xFFFFEFC7),
    danger = Color(0xFFB3232B),
    dangerContainer = Color(0xFFFDE1E0),
    info = Color(0xFF1A55AD),
    infoContainer = Color(0xFFDFEAFB),
    squat = Color(0xFF2A78D6),
    bench = Color(0xFFEB6834),
    deadlift = Color(0xFF1BAF7A),
    hairline = Color(0xFFE0E0D9),
)

val LocalPtColors = staticCompositionLocalOf { DarkPtColors }

/** Dark first: graphite surfaces stepping up in tone, chalk text. */
val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Volt,
    onPrimary = VoltInk,
    primaryContainer = DarkPtColors.accentContainer,
    onPrimaryContainer = DarkPtColors.onAccentContainer,
    secondary = Color(0xFFA9AEB6),
    onSecondary = Color(0xFF131518),
    secondaryContainer = Color(0xFF2B3036),
    onSecondaryContainer = Color(0xFFF3F4EF),
    tertiary = DarkPtColors.info,
    onTertiary = Color(0xFF0B0C0E),
    tertiaryContainer = DarkPtColors.infoContainer,
    onTertiaryContainer = Color(0xFFD6E6FF),
    error = DarkPtColors.danger,
    onError = Color(0xFF2A0608),
    errorContainer = DarkPtColors.dangerContainer,
    onErrorContainer = Color(0xFFFFDAD8),
    background = Color(0xFF0B0C0E),
    onBackground = Color(0xFFF3F4EF),
    surface = Color(0xFF131518),
    onSurface = Color(0xFFF3F4EF),
    surfaceVariant = Color(0xFF22262B),
    onSurfaceVariant = Color(0xFFA9AEB6),
    surfaceDim = Color(0xFF0B0C0E),
    surfaceBright = Color(0xFF2B3036),
    surfaceContainerLowest = Color(0xFF0B0C0E),
    surfaceContainerLow = Color(0xFF131518),
    surfaceContainer = Color(0xFF1A1D21),
    surfaceContainerHigh = Color(0xFF22262B),
    surfaceContainerHighest = Color(0xFF2B3036),
    surfaceTint = Color.Transparent,
    outline = Color(0xFF5A616B),
    outlineVariant = Color(0xFF2E333A),
    inverseSurface = Color(0xFFF3F4EF),
    inverseOnSurface = Color(0xFF131518),
    inversePrimary = LightPtColors.accentText,
    scrim = Color(0xFF000000),
)

/** Light: chalk-paper background, white cards, ink text. Same volt fills. */
val LightColorScheme: ColorScheme = lightColorScheme(
    primary = LightPtColors.accentText,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = LightPtColors.accentContainer,
    onPrimaryContainer = LightPtColors.onAccentContainer,
    secondary = Color(0xFF4D525A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6E6E0),
    onSecondaryContainer = Color(0xFF111315),
    tertiary = LightPtColors.info,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = LightPtColors.infoContainer,
    onTertiaryContainer = Color(0xFF0B2A5C),
    error = LightPtColors.danger,
    onError = Color(0xFFFFFFFF),
    errorContainer = LightPtColors.dangerContainer,
    onErrorContainer = Color(0xFF5C0D12),
    background = Color(0xFFF4F4F0),
    onBackground = Color(0xFF111315),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111315),
    surfaceVariant = Color(0xFFE6E6E0),
    onSurfaceVariant = Color(0xFF4D525A),
    surfaceDim = Color(0xFFE6E6E0),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEEEEE9),
    surfaceContainerHigh = Color(0xFFE6E6E0),
    surfaceContainerHighest = Color(0xFFDCDCD5),
    surfaceTint = Color.Transparent,
    outline = Color(0xFF8C9098),
    outlineVariant = Color(0xFFD6D6CE),
    inverseSurface = Color(0xFF131518),
    inverseOnSurface = Color(0xFFF3F4EF),
    inversePrimary = Volt,
    scrim = Color(0xFF000000),
)

/** IPF-style plate colours by weight in kg. Fixed in both themes: they picture real objects. */
object PlateColors {
    val Red = Color(0xFFD8262C)
    val Blue = Color(0xFF1F5FBF)
    val Yellow = Color(0xFFF2C417)
    val Green = Color(0xFF1E9E4A)
    val White = Color(0xFFF2F2EE)
    val Black = Color(0xFF08080A)
    val Chrome = Color(0xFFB4BAC2)
    val Steel = Color(0xFF8D949D)

    fun forKg(kg: Double): Color = when {
        kg >= 25.0 -> Red
        kg >= 20.0 -> Blue
        kg >= 15.0 -> Yellow
        kg >= 10.0 -> Green
        kg >= 5.0 -> White
        kg >= 2.5 -> Black
        else -> Chrome
    }
}
