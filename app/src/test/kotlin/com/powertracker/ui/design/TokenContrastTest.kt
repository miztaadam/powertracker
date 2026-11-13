package com.powertracker.ui.design

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.powertracker.ui.theme.BodybuildingDarkColorScheme
import com.powertracker.ui.theme.BodybuildingDarkPtColors
import com.powertracker.ui.theme.BodybuildingLightColorScheme
import com.powertracker.ui.theme.BodybuildingLightPtColors
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

    // The bodybuilding side's palettes (bodybuilding mode spec §7.2): the same checks, and the exact values.

    @Test
    fun bodybuildingDarkScheme_meetsAa() = check("bodybuilding dark", BodybuildingDarkColorScheme, BodybuildingDarkPtColors)

    @Test
    fun bodybuildingLightScheme_meetsAa() = check("bodybuilding light", BodybuildingLightColorScheme, BodybuildingLightPtColors)

    @Test
    fun bodybuildingDarkAccent_meetsTheMinimums() = checkAccent("bodybuilding dark", BodybuildingDarkColorScheme, BodybuildingDarkPtColors)

    @Test
    fun bodybuildingLightAccent_meetsTheMinimums() = checkAccent("bodybuilding light", BodybuildingLightColorScheme, BodybuildingLightPtColors)

    @Test
    fun bodybuildingDarkOutline_showsOnSelectedFills() = checkOutline("bodybuilding dark", BodybuildingDarkColorScheme)

    @Test
    fun bodybuildingLightOutline_showsOnSelectedFills() = checkOutline("bodybuilding light", BodybuildingLightColorScheme)

    private fun checkBlue(scheme: ColorScheme, pt: PtColors, base: PtColors, v: List<Long>) {
        val c = v.map { Color(it) }
        assertEquals("background", c[0], scheme.background)
        assertEquals("surface", c[1], scheme.surface)
        assertEquals("container", c[2], scheme.surfaceContainer)
        assertEquals("container high", c[3], scheme.surfaceContainerHigh)
        assertEquals("container highest", c[4], scheme.surfaceContainerHighest)
        assertEquals("hairline", c[5], pt.hairline)
        assertEquals("accent", c[6], pt.accent)
        assertEquals("on accent", c[7], pt.onAccent)
        assertEquals("accent text", c[8], pt.accentText)
        assertEquals("accent container", c[9], pt.accentContainer)
        assertEquals("on accent container", c[10], pt.onAccentContainer)
        assertEquals(pt.accentText, scheme.primary)
        assertEquals(pt.onAccent, scheme.onPrimary)
        // Text, state, chart and plate colours are the powerlifting side's.
        assertEquals(
            base.copy(accent = pt.accent, onAccent = pt.onAccent, accentText = pt.accentText, accentContainer = pt.accentContainer, onAccentContainer = pt.onAccentContainer, hairline = pt.hairline),
            pt,
        )
    }

    // Container highest is one step lighter than the spec's #22314F / #C9D7EC: at the spec's value the outline
    // (dark and light) and muted text (light) miss their minimums on it.
    @Test
    fun bodybuildingDark_isPinned() {
        checkBlue(
            BodybuildingDarkColorScheme, BodybuildingDarkPtColors, DarkPtColors,
            listOf(0xFF070B14, 0xFF0D1422, 0xFF131C2E, 0xFF1A2640, 0xFF21304E, 0xFF1E2A44, 0xFF8DB8FF, 0xFF07162E, 0xFF8DB8FF, 0xFF14274A, 0xFFD6E6FF),
        )
        assertEquals(DarkColorScheme.onSurface, BodybuildingDarkColorScheme.onSurface)
        assertEquals(DarkColorScheme.onSurfaceVariant, BodybuildingDarkColorScheme.onSurfaceVariant)
    }

    @Test
    fun bodybuildingLight_isPinned() {
        checkBlue(
            BodybuildingLightColorScheme, BodybuildingLightPtColors, LightPtColors,
            listOf(0xFFEDF2FA, 0xFFFFFFFF, 0xFFE2EAF6, 0xFFD6E1F1, 0xFFCEDCF1, 0xFFD0DCEE, 0xFF1F4FA8, 0xFFF5F8FF, 0xFF1F4FA8, 0xFFDCE8FB, 0xFF173B7D),
        )
        assertEquals(LightColorScheme.onSurface, BodybuildingLightColorScheme.onSurface)
        assertEquals(LightColorScheme.onSurfaceVariant, BodybuildingLightColorScheme.onSurfaceVariant)
    }
}
