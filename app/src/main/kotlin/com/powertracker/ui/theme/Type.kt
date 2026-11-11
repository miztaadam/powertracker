package com.powertracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.powertracker.R

/** Barlow: UI text. Bundled (OFL), the app has no network. */
val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
)

/** Barlow Condensed: numerals and screen titles. */
val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

// Tabular figures everywhere, so weights, reps and timers never shift as they change.
private const val TNUM = "tnum"

private fun display(size: Int, line: Int, weight: FontWeight = FontWeight.SemiBold) = TextStyle(
    fontFamily = BarlowCondensed, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, fontFeatureSettings = TNUM,
)

private fun text(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, spacing: Double = 0.0) = TextStyle(
    fontFamily = Barlow, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp, fontFeatureSettings = TNUM,
)

val PowertrackerTypography = Typography(
    // Numerals: hero (current weight, timer, total), large (stat tiles), medium (set rows, steppers).
    displayLarge = display(64, 64, FontWeight.Bold),
    displayMedium = display(44, 46, FontWeight.Bold),
    displaySmall = display(30, 34),
    // Screen and card titles.
    headlineLarge = display(34, 38, FontWeight.Bold),
    headlineMedium = display(26, 30),
    headlineSmall = display(22, 26),
    titleLarge = text(20, 26, FontWeight.SemiBold),
    titleMedium = text(16, 22, FontWeight.SemiBold),
    titleSmall = text(14, 20, FontWeight.SemiBold),
    bodyLarge = text(16, 24),
    bodyMedium = text(14, 20),
    bodySmall = text(12, 16),
    // Buttons.
    labelLarge = text(15, 20, FontWeight.SemiBold, 0.2),
    // Pills and chips.
    labelMedium = text(12, 16, FontWeight.SemiBold, 0.3),
    // Eyebrows and units: always upper case.
    labelSmall = text(11, 14, FontWeight.SemiBold, 1.1),
)
