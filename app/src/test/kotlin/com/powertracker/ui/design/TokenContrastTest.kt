package com.powertracker.ui.design

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.powertracker.ui.theme.DarkColorScheme
import com.powertracker.ui.theme.DarkPtColors
import com.powertracker.ui.theme.LightColorScheme
import com.powertracker.ui.theme.LightPtColors
import com.powertracker.ui.theme.PtColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG AA (4.5:1) for every text/background pairing the design system allows, plus the warm chalk
 * accent: its exact values and the 3:1 minimums that keep the primary button and the selected
 * outline visible.
 */
class TokenContrastTest {
    private fun ratio(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return (maxOf(la, lb) / minOf(la, lb)).toDouble()
    }

    private fun check(name: String, scheme: ColorScheme, pt: PtColors) {
        val surfaces = mapOf(
            "background" to scheme.background,
            "surface" to scheme.surface,
            "surfaceContainer" to scheme.surfaceContainer,
            "surfaceContainerHigh" to scheme.surfaceContainerHigh,
            "surfaceContainerHighest" to scheme.surfaceContainerHighest,
        )
        val texts = mapOf(
            "onSurface" to scheme.onSurface,
            "onSurfaceVariant" to scheme.onSurfaceVariant,
            "muted" to pt.muted,
            "primary" to scheme.primary,
            "accentText" to pt.accentText,
            "success" to pt.success,
            "warning" to pt.warning,
            "danger" to pt.danger,
            "info" to pt.info,
        )
        for ((sn, s) in surfaces) for ((tn, t) in texts) {
            assertTrue("$name: $tn on $sn is ${ratio(t, s)}", ratio(t, s) >= 4.5)
        }
        val pairs = listOf(
            Triple("onAccent on accent", pt.onAccent, pt.accent),
            Triple("onPrimary on primary", scheme.onPrimary, scheme.primary),
            Triple("onAccentContainer on accentContainer", pt.onAccentContainer, pt.accentContainer),
            Triple("success on successContainer", pt.success, pt.successContainer),
            Triple("warning on warningContainer", pt.warning, pt.warningContainer),
            Triple("danger on dangerContainer", pt.danger, pt.dangerContainer),
            Triple("info on infoContainer", pt.info, pt.infoContainer),
            Triple("onSurface on warningContainer", scheme.onSurface, pt.warningContainer),
            Triple("onSurface on dangerContainer", scheme.onSurface, pt.dangerContainer),
        )
        for ((label, fg, bg) in pairs) assertTrue("$name: $label is ${ratio(fg, bg)}", ratio(fg, bg) >= 4.5)
    }

    @Test
    fun darkScheme_meetsAa() = check("dark", DarkColorScheme, DarkPtColors)

    @Test
    fun lightScheme_meetsAa() = check("light", LightColorScheme, LightPtColors)

    private fun surfaces(scheme: ColorScheme) = mapOf(
        "background" to scheme.background,
        "surface" to scheme.surface,
        "surfaceContainer" to scheme.surfaceContainer,
        "surfaceContainerHigh" to scheme.surfaceContainerHigh,
        "surfaceContainerHighest" to scheme.surfaceContainerHighest,
    )

    private fun checkAccent(name: String, scheme: ColorScheme, pt: PtColors) {
        assertTrue("$name: onAccent on accent is ${ratio(pt.onAccent, pt.accent)}", ratio(pt.onAccent, pt.accent) >= 4.5)
        assertTrue(
            "$name: onAccentContainer on accentContainer is ${ratio(pt.onAccentContainer, pt.accentContainer)}",
            ratio(pt.onAccentContainer, pt.accentContainer) >= 4.5,
        )
        for ((sn, s) in surfaces(scheme)) {
            assertTrue("$name: accent fill against $sn is ${ratio(pt.accent, s)}", ratio(pt.accent, s) >= 3.0)
            assertTrue("$name: accentText on $sn is ${ratio(pt.accentText, s)}", ratio(pt.accentText, s) >= 4.5)
            // A disabled Material button is onSurface at 12% over whatever it sits on.
            val disabled = scheme.onSurface.copy(alpha = 0.12f).compositeOver(s)
            assertTrue("$name: accent fill against the disabled fill on $sn is ${ratio(pt.accent, disabled)}", ratio(pt.accent, disabled) >= 3.0)
        }
        // SecondaryButton's fill.
        assertTrue(
            "$name: accent fill against the secondary-button fill is ${ratio(pt.accent, scheme.surfaceContainerHighest)}",
            ratio(pt.accent, scheme.surfaceContainerHighest) >= 3.0,
        )
    }

    /** The outline is the main "selected" cue on chips and segments, so it must show on their fills. */
    private fun checkOutline(name: String, scheme: ColorScheme) {
        for ((sn, s) in listOf("surfaceContainerHigh" to scheme.surfaceContainerHigh, "surfaceContainerHighest" to scheme.surfaceContainerHighest)) {
            assertTrue("$name: outline against $sn is ${ratio(scheme.outline, s)}", ratio(scheme.outline, s) >= 3.0)
        }
    }

    @Test
    fun darkAccent_isWarmChalk() {
        assertEquals(Color(0xFFDCD6C8), DarkPtColors.accent)
        assertEquals(Color(0xFF1A1812), DarkPtColors.onAccent)
        assertEquals(Color(0xFFDCD6C8), DarkPtColors.accentText)
        assertEquals(Color(0xFF332F26), DarkPtColors.accentContainer)
        assertEquals(Color(0xFFEAE4D5), DarkPtColors.onAccentContainer)
        assertEquals(DarkPtColors.accent, DarkColorScheme.primary)
        assertEquals(DarkPtColors.onAccent, DarkColorScheme.onPrimary)
    }

    @Test
    fun lightAccent_isWarmChalk() {
        assertEquals(Color(0xFF46423B), LightPtColors.accent)
        assertEquals(Color(0xFFFBF8F1), LightPtColors.onAccent)
        assertEquals(Color(0xFF4B463E), LightPtColors.accentText)
        assertEquals(Color(0xFFE8E2D4), LightPtColors.accentContainer)
        assertEquals(Color(0xFF39342B), LightPtColors.onAccentContainer)
        assertEquals(LightPtColors.accentText, LightColorScheme.primary)
    }

    /** The strict press chart colour, as on the web (`--chart-press`): a line and a dot must stand out from what they are drawn on. */
    private fun checkPress(name: String, scheme: ColorScheme, pt: PtColors, expected: Color) {
        assertEquals(expected, pt.press)
        for ((sn, s) in surfaces(scheme)) assertTrue("$name: press against $sn is ${ratio(pt.press, s)}", ratio(pt.press, s) >= 3.0)
        for (other in listOf(pt.squat, pt.bench, pt.deadlift)) assertTrue("$name: press is its own colour", pt.press != other)
    }

    @Test
    fun darkPress_isPinned() = checkPress("dark", DarkColorScheme, DarkPtColors, Color(0xFFA98BF0))

    @Test
    fun lightPress_isPinned() = checkPress("light", LightColorScheme, LightPtColors, Color(0xFF7C52CC))

    @Test
    fun darkAccent_meetsTheMinimums() = checkAccent("dark", DarkColorScheme, DarkPtColors)

    @Test
    fun lightAccent_meetsTheMinimums() = checkAccent("light", LightColorScheme, LightPtColors)

    @Test
    fun darkOutline_showsOnSelectedFills() = checkOutline("dark", DarkColorScheme)

    @Test
    fun lightOutline_showsOnSelectedFills() = checkOutline("light", LightColorScheme)
}
