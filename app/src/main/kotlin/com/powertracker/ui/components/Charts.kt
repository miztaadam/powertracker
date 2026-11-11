package com.powertracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * One line on a chart. Use the lift colours from [PowertrackerTheme.colors] so a lift keeps its colour everywhere.
 * A null value is a point in time with no reading: the line runs straight across it.
 */
@Immutable
data class ChartSeries(val name: String, val color: Color, val values: List<Float?>)

/** Chart figures to one decimal at most: 187.5, 92.4, 250. */
private fun formatValue(value: Float): String = formatKg((value * 10).roundToInt() / 10.0)

private fun niceStep(range: Float): Float =
    listOf(2.5f, 5f, 10f, 20f, 25f, 50f, 100f, 200f).firstOrNull { range / it <= 4f } ?: 500f

private fun DrawScope.drawSeriesLine(points: List<Offset>, color: Color, ring: Color) {
    if (points.isEmpty()) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (p in points.drop(1)) lineTo(p.x, p.y)
    }
    drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    // End marker with a surface ring so it stays readable where lines cross.
    drawCircle(ring, 6.dp.toPx(), points.last())
    drawCircle(color, 4.dp.toPx(), points.last())
}

/**
 * Change over time for up to four series on one shared axis. A legend with each series' latest
 * value is always shown; values are written in text colour, the dot beside them carries identity.
 * [xLabels] has one entry per point; leave entries blank to skip them.
 */
@Composable
fun LineChart(series: List<ChartSeries>, xLabels: List<String>, modifier: Modifier = Modifier, unit: String = "kg") {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    val measurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.bodySmall.copy(color = pt.muted)
    val all = series.flatMap { it.values.filterNotNull() }
    val summary = series.joinToString(", ") { "${it.name} ${formatValue(it.values.lastOrNull { v -> v != null } ?: 0f)} $unit" }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.l), verticalAlignment = Alignment.CenterVertically) {
            for (s in series) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).background(s.color, CircleShape))
                    Text(s.name, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    Text(formatValue(s.values.lastOrNull { it != null } ?: 0f), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                }
            }
        }
        Canvas(Modifier.fillMaxWidth().height(168.dp).clearAndSetSemantics { contentDescription = "Line chart. Latest: $summary" }) {
            if (all.isEmpty()) return@Canvas
            val step = niceStep(all.max() - all.min())
            val lo = floor(all.min() / step) * step
            val hi = (ceil(all.max() / step) * step).let { if (it == lo) lo + step else it }
            val labelH = measurer.measure("0", axisStyle).size.height.toFloat()
            val left = measurer.measure(formatKg(hi.toDouble()), axisStyle).size.width + 8.dp.toPx()
            val right = size.width - 8.dp.toPx()
            val top = labelH / 2
            val bottom = size.height - labelH - 8.dp.toPx()
            fun y(v: Float) = bottom - (v - lo) / (hi - lo) * (bottom - top)
            val count = series.maxOf { it.values.size }
            fun x(i: Int) = if (count <= 1) left else left + (right - left) * i / (count - 1)

            var tick = lo
            while (tick <= hi + 0.01f) {
                drawLine(pt.hairline, Offset(left, y(tick)), Offset(size.width, y(tick)), 1.dp.toPx())
                val label = measurer.measure(formatKg(tick.toDouble()), axisStyle)
                drawText(label, topLeft = Offset(left - 8.dp.toPx() - label.size.width, y(tick) - label.size.height / 2))
                tick += step
            }
            xLabels.forEachIndexed { i, text ->
                if (text.isBlank()) return@forEachIndexed
                val label = measurer.measure(text, axisStyle)
                val lx = (x(i) - label.size.width / 2).coerceIn(left, size.width - label.size.width)
                drawText(label, topLeft = Offset(lx, size.height - label.size.height))
            }
            for (s in series) drawSeriesLine(s.values.mapIndexedNotNull { i, v -> v?.let { Offset(x(i), y(it)) } }, s.color, scheme.surface)
        }
    }
}

/**
 * A tiny trend line with no axes, for stat tiles and coach cards. Say the trend in words next to it.
 * The line fills the height unless the values vary by less than [minSpan]: then it is drawn to that
 * scale, so a flat trend looks flat instead of having its noise stretched.
 */
@Composable
fun Sparkline(values: List<Float>, color: Color, modifier: Modifier = Modifier, fill: Boolean = true, minSpan: Float = 0f) {
    val ring = MaterialTheme.colorScheme.surface
    Canvas(modifier.clearAndSetSemantics { }) {
        if (values.size < 2) return@Canvas
        val pad = 6.dp.toPx()
        val range = values.max() - values.min()
        val span = maxOf(range, minSpan).takeIf { it > 0f } ?: 1f
        // Centre the values when the scale is wider than they are.
        val lo = values.min() - (span - range) / 2
        val points = values.mapIndexed { i, v ->
            Offset(pad + (size.width - 2 * pad) * i / (values.size - 1), size.height - pad - (v - lo) / span * (size.height - 2 * pad))
        }
        if (fill) {
            val area = Path().apply {
                moveTo(points.first().x, size.height)
                for (p in points) lineTo(p.x, p.y)
                lineTo(points.last().x, size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent)))
        }
        drawSeriesLine(points, color, ring)
    }
}

/**
 * A row of bars for one quantity over equal periods (weekly volume), oldest first, no axes: say the
 * figure that matters in text next to it. Bars share the strip's own scale; an empty period keeps a
 * stub on the baseline so the rhythm of the weeks stays visible.
 */
@Composable
fun BarStrip(values: List<Float>, color: Color, modifier: Modifier = Modifier) {
    val stub = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier.clearAndSetSemantics { }) {
        if (values.isEmpty()) return@Canvas
        val gap = 4.dp.toPx()
        val barW = (size.width - gap * (values.size - 1)) / values.size
        val max = values.max().takeIf { it > 0f } ?: 1f
        val stubH = 3.dp.toPx()
        val r = CornerRadius(minOf(3.dp.toPx(), barW / 2))
        values.forEachIndexed { i, v ->
            val h = if (v > 0f) maxOf(v / max * size.height, stubH * 2) else stubH
            drawRoundRect(if (v > 0f) color else stub, Offset(i * (barW + gap), size.height - h), Size(barW, h), r)
        }
    }
}

@Preview
@Composable
private fun ChartsPreview() = PowertrackerTheme(darkTheme = true) {
    val c = PowertrackerTheme.colors
    PtCard {
        LineChart(
            listOf(
                ChartSeries("Squat", c.squat, listOf(170f, 172.5f, 175f, 177.5f, 182.5f)),
                ChartSeries("Bench", c.bench, listOf(110f, 112.5f, 112.5f, 112f, 112.5f)),
            ),
            listOf("Aug", "", "", "", "Nov"),
        )
        Sparkline(listOf(1f, 2f, 2f, 3f, 5f), c.accentText, Modifier.fillMaxWidth().height(48.dp))
        BarStrip(listOf(3f, 4f, 0f, 5f, 4.5f), c.squat, Modifier.fillMaxWidth().height(48.dp))
    }
}
