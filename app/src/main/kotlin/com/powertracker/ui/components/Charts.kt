package com.powertracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.powertracker.ui.theme.LocalMotion
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

/** A line draws from left to right in this long (stage 4, owner decision 3). */
const val LINE_DRAW_MS = 600

/** Each bar grows in this long, */
const val BAR_GROW_MS = 450

/** starting this long after the one before, */
const val BAR_STAGGER_MS = 30

/** and the whole strip is done within this long: with many bars the stagger shrinks to fit. */
const val BAR_STRIP_MS = 700

/** A ratio marker slides from the start of its bar to its value in this long. */
const val MARKER_SLIDE_MS = 450

/** Drawn in full: the default for every chart, and what [rememberDrawIn] gives with motion off. */
val DrawnComplete: () -> Float = { 1f }

private object Complete : State<Float> {
    override val value = 1f
}

/**
 * How much of a chart is drawn, from 0 to 1 over [durationMs], starting when the chart first appears and again
 * each time [key] changes (the chart's range). A change of data with the same key leaves it where it is. With
 * motion off ([LocalMotion]) it is 1 from the start and nothing runs.
 *
 * Read it where the chart draws (pass `state::value`), so each frame only redraws. Only what is drawn moves:
 * a chart's figures, labels and description are complete from the first frame.
 */
@Composable
fun rememberDrawIn(key: Any?, durationMs: Int, easing: Easing = FastOutSlowInEasing): State<Float> {
    if (!LocalMotion.current) return Complete
    // A new key starts a new run at 0, so the first frame after a range change already draws nothing of the old line.
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(progress) { progress.animateTo(1f, tween(durationMs, easing = easing)) }
    return progress.asState()
}

/** The gap between one bar starting and the next, so that [count] bars finish within [BAR_STRIP_MS]. */
private fun barStagger(count: Int): Float =
    if (count <= 1) 0f else minOf(BAR_STAGGER_MS.toFloat(), (BAR_STRIP_MS - BAR_GROW_MS).toFloat() / (count - 1))

/**
 * The clock for [BarStrip]'s `grown`: 0 to 1, evenly, over [BAR_STRIP_MS]. The strip times each bar against it, so
 * any number of bars is done by the end.
 */
@Composable
fun rememberBarsGrowIn(key: Any?): State<Float> = rememberDrawIn(key, BAR_STRIP_MS, LinearEasing)

/** Chart figures to one decimal at most: 187.5, 92.4, 250. */
private fun formatValue(value: Float): String = formatKg((value * 10).roundToInt() / 10.0)

private fun niceStep(range: Float): Float =
    listOf(2.5f, 5f, 10f, 20f, 25f, 50f, 100f, 200f).firstOrNull { range / it <= 4f } ?: 500f

private fun linePath(points: List<Offset>) = Path().apply {
    moveTo(points.first().x, points.first().y)
    for (p in points.drop(1)) lineTo(p.x, p.y)
}

/**
 * The line through [points], drawn up to [drawn] of its length. The end marker shows once the line reaches it.
 * Drawn in full (or a single point, which has no length) it is the whole path, as before charts filled in.
 */
private fun DrawScope.drawSeriesLine(points: List<Offset>, color: Color, ring: Color, drawn: Float = 1f) {
    if (points.isEmpty()) return
    val path = linePath(points)
    val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    if (drawn < 1f) {
        val measure = PathMeasure().apply { setPath(path, false) }
        if (measure.length > 0f) {
            if (drawn > 0f) {
                val part = Path()
                measure.getSegment(0f, measure.length * drawn, part, true)
                drawPath(part, color, style = stroke)
            }
            return
        }
    }
    drawPath(path, color, style = stroke)
    // End marker with a surface ring so it stays readable where lines cross.
    drawCircle(ring, 6.dp.toPx(), points.last())
    drawCircle(color, 4.dp.toPx(), points.last())
}

/** Where a line through [points] has got to when [drawn] of it is drawn. */
private fun lineHead(points: List<Offset>, drawn: Float): Offset {
    val measure = PathMeasure().apply { setPath(linePath(points), false) }
    return measure.getPosition(measure.length * drawn)
}

/**
 * Change over time for up to four series on one shared axis. A legend with each series' latest
 * value is always shown; values are written in text colour, the dot beside them carries identity.
 * [xLabels] has one entry per point; leave entries blank to skip them.
 * [drawn] is how much of each line is drawn ([rememberDrawIn]); the legend, axes and description are always complete.
 */
@Composable
fun LineChart(series: List<ChartSeries>, xLabels: List<String>, modifier: Modifier = Modifier, unit: String = "kg", drawn: () -> Float = DrawnComplete) {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    val measurer = rememberTextMeasurer()
    val axisStyle = MaterialTheme.typography.bodySmall.copy(color = pt.muted)
    val all = series.flatMap { it.values.filterNotNull() }
    val summary = series.joinToString(", ") { "${it.name} ${formatValue(it.values.lastOrNull { v -> v != null } ?: 0f)} $unit" }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
        // Four lifts with their figures are wider than a small phone: the legend wraps.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.l), verticalArrangement = Arrangement.spacedBy(Space.xs), itemVerticalAlignment = Alignment.CenterVertically) {
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
            val progress = drawn()
            for (s in series) drawSeriesLine(s.values.mapIndexedNotNull { i, v -> v?.let { Offset(x(i), y(it)) } }, s.color, scheme.surface, progress)
        }
    }
}

/**
 * A tiny trend line with no axes, for stat tiles and coach cards. Say the trend in words next to it.
 * The line fills the height unless the values vary by less than [minSpan]: then it is drawn to that
 * scale, so a flat trend looks flat instead of having its noise stretched. [drawn] is how much of the line is drawn
 * ([rememberDrawIn]); the fill under it follows the line's head.
 */
@Composable
fun Sparkline(values: List<Float>, color: Color, modifier: Modifier = Modifier, fill: Boolean = true, minSpan: Float = 0f, drawn: () -> Float = DrawnComplete) {
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
        val progress = drawn()
        if (fill && progress > 0f) {
            val area = Path().apply {
                moveTo(points.first().x, size.height)
                for (p in points) lineTo(p.x, p.y)
                lineTo(points.last().x, size.height)
                close()
            }
            val brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent))
            if (progress >= 1f) drawPath(area, brush) else clipRect(right = lineHead(points, progress).x) { drawPath(area, brush) }
        }
        drawSeriesLine(points, color, ring, progress)
    }
}

/**
 * A row of bars for one quantity over equal periods (weekly volume), oldest first, no axes: say the
 * figure that matters in text next to it. Bars share the strip's own scale; an empty period keeps a
 * stub on the baseline so the rhythm of the weeks stays visible.
 *
 * [grown] is the strip's clock ([rememberBarsGrowIn]): each bar grows from the baseline in [BAR_GROW_MS], one after
 * another. The stubs are the baseline, not a value, so they show at once.
 */
@Composable
fun BarStrip(values: List<Float>, color: Color, modifier: Modifier = Modifier, grown: () -> Float = DrawnComplete) {
    val stub = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier.clearAndSetSemantics { }) {
        if (values.isEmpty()) return@Canvas
        val gap = 4.dp.toPx()
        val barW = (size.width - gap * (values.size - 1)) / values.size
        val max = values.max().takeIf { it > 0f } ?: 1f
        val stubH = 3.dp.toPx()
        val r = CornerRadius(minOf(3.dp.toPx(), barW / 2))
        val clock = grown()
        val stagger = barStagger(values.size)
        values.forEachIndexed { i, v ->
            val full = if (v > 0f) maxOf(v / max * size.height, stubH * 2) else stubH
            val h = if (clock >= 1f || v <= 0f) full else {
                val own = ((clock * BAR_STRIP_MS - i * stagger) / BAR_GROW_MS).coerceIn(0f, 1f)
                full * FastOutSlowInEasing.transform(own)
            }
            if (h > 0f) drawRoundRect(if (v > 0f) color else stub, Offset(i * (barW + gap), size.height - h), Size(barW, h), r)
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
