package com.powertracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.powertracker.ui.theme.CardShadow
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

/** The standard container: one tonal step above the background and the soft card shadow, no border. */
@Composable
fun PtCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Space.l),
    spacing: Dp = Space.m,
    /** Makes the whole card a button. The ripple is clipped to the card, after its shadow, so the shadow stays. */
    onClickLabel: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .cardShadow(shape)
            .then(if (onClick != null) Modifier.clip(shape).clickable(onClickLabel = onClickLabel, onClick = onClick) else Modifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.cardHighlight(shape).padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
    }
}

/**
 * The soft card's shadow ([PowertrackerTheme.cardShadow]), the web's `--elevation-card` drawn the same way, as blurred
 * copies of the card's shape (Compose's `dropShadow`, which also draws in the screenshots, unlike `Modifier.shadow`):
 * the spot colour is the drop, y [CardShadow.elevation] and blur three times it; the ambient colour is the close
 * shadow, y 1 dp and blur 2 dp. It lies outside the card, so it goes before the card's background; the card's own clip
 * does not cut it. For cards and tiles only, never rows.
 */
@Composable
fun Modifier.cardShadow(shape: Shape): Modifier {
    val s = PowertrackerTheme.cardShadow
    return this
        .dropShadow(shape, Shadow(radius = s.elevation * 3, color = s.spot, offset = DpOffset(0.dp, s.elevation)))
        .dropShadow(shape, Shadow(radius = 2.dp, color = s.ambient, offset = DpOffset(0.dp, 1.dp)))
}

/**
 * The web's `inset 0 1px 0` highlight: an inner shadow 1 dp down with no blur, so a thin line along the top edge that
 * follows the corners. It goes on the card's content, inside its background. Nothing in light.
 */
@Composable
fun Modifier.cardHighlight(shape: Shape): Modifier {
    val highlight = PowertrackerTheme.cardShadow.highlight
    if (highlight.alpha == 0f) return this
    return innerShadow(shape, Shadow(radius = 0.dp, color = highlight, offset = DpOffset(0.dp, 1.dp)))
}
