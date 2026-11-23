package com.powertracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone
import com.powertracker.ui.theme.color

/**
 * A lift ratio against its typical range: the band marks the range (with its bounds in figures),
 * the marker is the lifter. In range reads green, outside reads amber, and a pill says which.
 * [slid] is how far the marker has come from the start of the bar to its value ([rememberDrawIn]); the figures and
 * the pill are always complete.
 */
@Composable
fun RatioBar(label: String, valuePct: Int, lowPct: Int, highPct: Int, modifier: Modifier = Modifier, slid: () -> Float = DrawnComplete) {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    val tone = if (valuePct in lowPct..highPct) Tone.SUCCESS else Tone.WARNING
    val status = when {
        valuePct < lowPct -> "Below range"
        valuePct > highPct -> "Above range"
        else -> "In range"
    }
    val markerColor = tone.color()
    val measurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.bodySmall.copy(color = pt.muted)
    // Show the range in the middle half of the bar, whatever its bounds.
    val span = (highPct - lowPct).coerceAtLeast(1)
    val domainLo = lowPct - span
    val domainHi = highPct + span

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, modifier = Modifier.weight(1f))
            Pill(status, tone = tone)
            Text("$valuePct%", style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface)
        }
        Canvas(Modifier.fillMaxWidth().height(34.dp).clearAndSetSemantics { }) {
            fun x(pct: Int) = size.width * (pct - domainLo).coerceIn(0, domainHi - domainLo) / (domainHi - domainLo)
            val trackH = 8.dp.toPx()
            val trackY = 4.dp.toPx()
            val r = CornerRadius(trackH / 2)
            drawRoundRect(scheme.surfaceContainerHighest, Offset(0f, trackY), Size(size.width, trackH), r)
            drawRoundRect(scheme.onSurface.copy(alpha = 0.32f), Offset(x(lowPct), trackY), Size(x(highPct) - x(lowPct), trackH), r)
            val markerW = 6.dp.toPx()
            val markerH = 16.dp.toPx()
            val start = markerW / 2
            val end = x(valuePct).coerceIn(start, size.width - markerW / 2)
            val progress = slid()
            val mx = if (progress >= 1f) end else start + (end - start) * progress
            drawRoundRect(scheme.surface, Offset(mx - markerW / 2 - 2.dp.toPx(), 0f), Size(markerW + 4.dp.toPx(), markerH), CornerRadius(markerW))
            drawRoundRect(markerColor, Offset(mx - markerW / 2, 0f), Size(markerW, markerH), CornerRadius(markerW / 2))
            for (bound in listOf(lowPct, highPct)) {
                val text = measurer.measure("$bound%", axisStyle)
                drawText(text, topLeft = Offset(x(bound) - text.size.width / 2, size.height - text.size.height))
            }
        }
    }
}

@Preview
@Composable
private fun RatioBarPreview() = PowertrackerTheme(darkTheme = true) {
    PtCard {
        RatioBar("Squat ÷ Deadlift", 84, 80, 90)
        RatioBar("Bench ÷ Squat", 56, 60, 70)
    }
}
