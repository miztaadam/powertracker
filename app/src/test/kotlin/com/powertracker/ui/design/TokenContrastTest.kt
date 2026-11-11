package com.powertracker.ui.design

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.powertracker.ui.theme.DarkColorScheme
import com.powertracker.ui.theme.DarkPtColors
import com.powertracker.ui.theme.LightColorScheme
import com.powertracker.ui.theme.LightPtColors
import com.powertracker.ui.theme.PtColors
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG AA (4.5:1) for every text/background pairing the design system allows. */
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
}
