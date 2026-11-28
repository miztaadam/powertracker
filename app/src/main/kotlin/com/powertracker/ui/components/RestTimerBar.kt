package com.powertracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

/** 108 → "1:48". */
fun formatClock(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

/**
 * The rest countdown docked above the bottom bar: time left in big figures, an accent line along the
 * top edge that empties as the rest runs out, and −30 s / +30 s / Skip.
 */
@Composable
fun RestTimerBar(
    remainingSec: Int,
    totalSec: Int,
    onSubtractThirty: () -> Unit,
    onAddThirty: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    val accent = pt.accentText
    val track = scheme.surfaceContainerHighest
    val fraction = if (totalSec > 0) (remainingSec.toFloat() / totalSec).coerceIn(0f, 1f) else 0f
    val finalTen = remainingSec in 1..10
    val ready = remainingSec <= 0 && totalSec > 0
    val barColor = if (ready) pt.success else accent
    val clockColor = when {
        ready -> pt.success
        finalTen -> accent
        else -> scheme.onSurface
    }
    val shape = MaterialTheme.shapes.large
    // Its own layer over the card it docks on: the card shadow and highlight, no border.
    Surface(modifier.fillMaxWidth().cardShadow(shape), color = scheme.surfaceContainerHigh, shape = shape) {
        Row(
            Modifier
                .cardHighlight(shape)
                .drawBehind {
                    val h = 4.dp.toPx()
                    drawRect(track, Offset.Zero, Size(size.width, h))
                    drawRect(barColor, Offset.Zero, Size(size.width * (if (ready) 1f else fraction), h))
                }
                .padding(start = Space.l, end = Space.s, top = Space.m, bottom = Space.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            Column(Modifier.weight(1f).clearAndSetSemantics { contentDescription = "Rest, ${formatClock(remainingSec)} left" }) {
                Eyebrow(if (ready) "Ready" else if (finalTen) "Almost up" else "Rest")
                Text(formatClock(remainingSec), style = MaterialTheme.typography.displaySmall, color = clockColor, maxLines = 1)
            }
            SecondaryButton("−30 s", onSubtractThirty, compact = true)
            SecondaryButton("+30 s", onAddThirty, compact = true)
            QuietButton("Skip", onSkip)
        }
    }
}

@Preview
@Composable
private fun RestTimerBarPreview() = PowertrackerTheme(darkTheme = true) {
    RestTimerBar(108, 180, {}, {}, {})
}
