package com.powertracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/** Brand colours Material's scheme has no slot for. Read through [PowertrackerTheme.colors]. */
@Immutable
data class PtColors(
    /** The one accent, grey on the powerlifting side and blue on the bodybuilding side, as a fill: the primary button, the current tab, a switch that is on. */
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
    val press: Color,
    /** Hairlines between rows. Cards have no border: they take [CardShadow]. */
    val hairline: Color,
)

val DarkPtColors = PtColors(
    accent = Color(0xFF93979D),
    onAccent = Color(0xFF0B0C0E),
    accentText = Color(0xFF93979D),
    accentContainer = Color(0xFF2A2C2F),
    onAccentContainer = Color(0xFFDEDFE1),
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
    press = Color(0xFFA98BF0),
    hairline = Color(0xFF262A30),
)

val LightPtColors = PtColors(
    accent = Color(0xFF4A4D52),
    onAccent = Color(0xFFF6F7F8),
    accentText = Color(0xFF4A4D52),
    accentContainer = Color(0xFFDCDEE0),
    onAccentContainer = Color(0xFF2F3135),
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
    press = Color(0xFF7C52CC),
    hairline = Color(0xFFE0E0D9),
)

val LocalPtColors = staticCompositionLocalOf { DarkPtColors }

/** Dark first: graphite surfaces stepping up in tone, chalk-white text, a grey accent. */
val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = DarkPtColors.accent,
    onPrimary = DarkPtColors.onAccent,
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
    // Selected chips and segments are told apart by this outline: 3:1 on container high and highest.
    outline = Color(0xFF717A86),
    outlineVariant = Color(0xFF2E333A),
    inverseSurface = Color(0xFFF3F4EF),
    inverseOnSurface = Color(0xFF131518),
    inversePrimary = LightPtColors.accentText,
    scrim = Color(0xFF000000),
)

/** Light: chalk-paper background, white cards, ink text. The grey accent turns darker. */
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
    outline = Color(0xFF787C83),
    outlineVariant = Color(0xFFD6D6CE),
    inverseSurface = Color(0xFF131518),
    inverseOnSurface = Color(0xFFF3F4EF),
    inversePrimary = DarkPtColors.accent,
    scrim = Color(0xFF000000),
)

/*
 * The bodybuilding side (bodybuilding mode spec §7.2): blue surfaces and a blue accent. Text, state, chart and plate
 * colours are the powerlifting side's, so only the accent, the surfaces and the hairline change. Container highest is
 * one step lighter than the spec's #22314F (dark) and #C9D7EC (light), the least that keeps the selected-chip
 * outline at 3:1 and muted text at 4.5:1 on it; the web app uses the same two values.
 */
val BodybuildingDarkPtColors = DarkPtColors.copy(
    accent = Color(0xFF8DB8FF),
    onAccent = Color(0xFF07162E),
    accentText = Color(0xFF8DB8FF),
    accentContainer = Color(0xFF14274A),
    onAccentContainer = Color(0xFFD6E6FF),
    hairline = Color(0xFF1E2A44),
)

val BodybuildingLightPtColors = LightPtColors.copy(
    accent = Color(0xFF1F4FA8),
    onAccent = Color(0xFFF5F8FF),
    accentText = Color(0xFF1F4FA8),
    accentContainer = Color(0xFFDCE8FB),
    onAccentContainer = Color(0xFF173B7D),
    hairline = Color(0xFFD0DCEE),
)

/** Dark, tinted blue: each surface slot takes the blue step that matches its graphite one. */
val BodybuildingDarkColorScheme: ColorScheme = DarkColorScheme.copy(
    primary = BodybuildingDarkPtColors.accent,
    onPrimary = BodybuildingDarkPtColors.onAccent,
    primaryContainer = BodybuildingDarkPtColors.accentContainer,
    onPrimaryContainer = BodybuildingDarkPtColors.onAccentContainer,
    secondaryContainer = Color(0xFF21304E),
    background = Color(0xFF070B14),
    surface = Color(0xFF0D1422),
    surfaceVariant = Color(0xFF1A2640),
    surfaceDim = Color(0xFF070B14),
    surfaceBright = Color(0xFF21304E),
    surfaceContainerLowest = Color(0xFF070B14),
    surfaceContainerLow = Color(0xFF0D1422),
    surfaceContainer = Color(0xFF131C2E),
    surfaceContainerHigh = Color(0xFF1A2640),
    surfaceContainerHighest = Color(0xFF21304E),
    inverseOnSurface = Color(0xFF0D1422),
    inversePrimary = BodybuildingLightPtColors.accentText,
)

/** Light, tinted blue: a pale blue page, white cards, blue-grey containers, a deep blue accent. */
val BodybuildingLightColorScheme: ColorScheme = LightColorScheme.copy(
    primary = BodybuildingLightPtColors.accentText,
    onPrimary = BodybuildingLightPtColors.onAccent,
    primaryContainer = BodybuildingLightPtColors.accentContainer,
    onPrimaryContainer = BodybuildingLightPtColors.onAccentContainer,
    secondaryContainer = Color(0xFFD6E1F1),
    background = Color(0xFFEDF2FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFD6E1F1),
    surfaceDim = Color(0xFFD6E1F1),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFE2EAF6),
    surfaceContainerHigh = Color(0xFFD6E1F1),
    surfaceContainerHighest = Color(0xFFCEDCF1),
    inversePrimary = BodybuildingDarkPtColors.accent,
)

/**
 * The soft card's shadow (DESIGN.md "Space and shape"), the web's `--elevation-card`, drawn in `PtCard.kt` as two
 * `dropShadow` layers (the [spot] colour at y [elevation], blur three times it; the [ambient] colour at y 1 dp, blur
 * 2 dp) and in dark a [highlight] drawn as a 1 dp `innerShadow` along the card's top edge.
 * The bodybuilding side uses the same shadows. Read through [PowertrackerTheme.cardShadow].
 */
@Immutable
data class CardShadow(val elevation: Dp, val ambient: Color, val spot: Color, val highlight: Color)

val DarkCardShadow = CardShadow(
    elevation = Elevation.card,
    ambient = Color.Black.copy(alpha = 0.35f),
    spot = Color.Black.copy(alpha = 0.35f),
    highlight = Color.White.copy(alpha = 0.05f),
)

val LightCardShadow = CardShadow(
    elevation = Elevation.card,
    ambient = Color(0xFF111315).copy(alpha = 0.06f),
    spot = Color(0xFF111315).copy(alpha = 0.08f),
    highlight = Color.Transparent,
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
