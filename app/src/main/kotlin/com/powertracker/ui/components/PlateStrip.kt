package com.powertracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.powertracker.ui.theme.PlateColors
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone
import com.powertracker.ui.theme.color

/** "25 + 20 + 2.5" for the plates on one side, heaviest first; "Empty bar" for none. */
fun platesLabel(perSide: List<Double>): String =
    if (perSide.isEmpty()) "Empty bar" else perSide.joinToString(" + ") { formatKg(it) }

// Width (thickness) in dp and height as a share of a full-size plate.
private fun plateShape(kg: Double): Pair<Float, Float> = when {
    kg >= 25.0 -> 13f to 1f
    kg >= 20.0 -> 11f to 1f
    kg >= 15.0 -> 9f to 1f
    kg >= 10.0 -> 7f to 1f
    kg >= 5.0 -> 6f to 0.62f
    kg >= 2.5 -> 5f to 0.5f
    kg >= 1.25 -> 5f to 0.4f
    kg >= 0.5 -> 4f to 0.32f
    else -> 3f to 0.26f
}

/**
 * One side of the bar, drawn as it is loaded (collar on the left, heaviest plate first) in IPF
 * colours, with the same list in figures beside it. Pass `PlateLoader.perSide(...)`.
 */
@Composable
fun PlateStrip(perSide: List<Double>, modifier: Modifier = Modifier, caption: String = "per side · 20 kg bar") {
    val scheme = MaterialTheme.colorScheme
    val edge = scheme.onSurface.copy(alpha = 0.4f)
    Row(
        modifier.fillMaxWidth().background(scheme.surfaceContainer, MaterialTheme.shapes.medium).padding(horizontal = Space.m, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Canvas(Modifier.width(112.dp).height(48.dp)) {
            val mid = size.height / 2
            val bar = 6.dp.toPx()
            // Sleeve, then the thicker shoulder the plates sit against.
            drawRoundRect(PlateColors.Steel, Offset(0f, mid - bar / 2), Size(size.width, bar), CornerRadius(bar / 2))
            val shoulder = Size(5.dp.toPx(), 16.dp.toPx())
            drawRoundRect(PlateColors.Chrome, Offset(6.dp.toPx(), mid - shoulder.height / 2), shoulder, CornerRadius(1.5.dp.toPx()))
            var x = 6.dp.toPx() + shoulder.width + 2.dp.toPx()
            for (kg in perSide) {
                val (w, share) = plateShape(kg)
                val plate = Size(w.dp.toPx(), size.height * share)
                if (x + plate.width > size.width) break
                val topLeft = Offset(x, mid - plate.height / 2)
                val radius = CornerRadius(2.dp.toPx())
                drawRoundRect(PlateColors.forKg(kg), topLeft, plate, radius)
                // A faint edge keeps black plates visible on dark and white plates on light.
                drawRoundRect(edge, topLeft, plate, radius, style = Stroke(1.dp.toPx()))
                x += plate.width + 2.dp.toPx()
            }
        }
        Column {
            Text(platesLabel(perSide), style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface, maxLines = 1)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = PowertrackerTheme.colors.muted, maxLines = 1)
        }
    }
}

/** Stands in for [PlateStrip] when there is nothing to draw: no weight yet, or one the plates cannot make. */
@Composable
fun PlateStripNote(title: String, caption: String, modifier: Modifier = Modifier, tone: Tone = Tone.NEUTRAL) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().background(scheme.surfaceContainer, MaterialTheme.shapes.medium).heightIn(min = 64.dp).padding(horizontal = Space.m, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Icon(
            if (tone == Tone.NEUTRAL) Icons.Filled.FitnessCenter else Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = tone.color(),
            modifier = Modifier.size(24.dp),
        )
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = PowertrackerTheme.colors.muted)
        }
    }
}

@Preview
@Composable
private fun PlateStripPreview() = PowertrackerTheme(darkTheme = true) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PlateStrip(listOf(25.0, 25.0, 15.0, 2.5))
        PlateStrip(listOf(20.0, 10.0, 5.0, 2.5, 1.25, 0.5, 0.25))
        PlateStripNote("Can't load 181 kg exactly", "Nearest is 180 kg.", tone = Tone.WARNING)
    }
}
