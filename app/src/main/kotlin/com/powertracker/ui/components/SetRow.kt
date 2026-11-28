package com.powertracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.SplitSides
import com.powertracker.domain.stack.PlateStack
import com.powertracker.domain.unilateral.SingleSided
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone

enum class SetState { PLANNED, DONE, FAILED }

/** A done set's tick drawing itself in, for tests: read it for how far (0 to 1) through its time it is. */
val TickDrawIn = SemanticsPropertyKey<() -> Float>("TickDrawIn")
private var SemanticsPropertyReceiver.tickDrawIn by TickDrawIn

/**
 * One set: number, weight × reps @ RPE, and its state. Planned sets are dimmed, done sets get a
 * green tick, failed sets a red cross and a "Failed" pill; a personal record gets a "PR" pill.
 * Reads to TalkBack as one sentence.
 */
@Composable
fun SetRow(
    index: Int,
    weightKg: Double,
    reps: Int,
    rpe: Double?,
    state: SetState,
    modifier: Modifier = Modifier,
    isTopSet: Boolean = false,
    isPr: Boolean = false,
    onClick: (() -> Unit)? = null,
    /**
     * A done set's tick drawing itself in (1.16): how far through it is, 0 to 1, read as it is drawn. Null draws the
     * tick as it always is; Today passes one only for a set logged now.
     */
    drawIn: (() -> Float)? = null,
    /** What [weightKg] is: kg, or a plate number on a plate-counted machine (1.17), "plate 7½". */
    unit: LoadUnit = LoadUnit.KG,
    /** Version 8 (1.25): both halves of a single-sided set ("R 12 · L 10"); null for a normal set. */
    splitSides: SplitSides? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    val strong = if (state == SetState.PLANNED) scheme.onSurfaceVariant else scheme.onSurface
    val soft = pt.muted
    val plates = unit == LoadUnit.PLATES
    val splitText = splitSides?.let { SingleSided.summaryText(it, unit) }
    val spoken = buildString {
        if (plates) {
            append("Set $index: ${PlateStack.spoken(weightKg)} × $reps")
            if (rpe != null) append(" at RPE ${formatRpe(rpe)}")
        } else {
            append("Set $index: ${formatKg(weightKg)} kg × $reps")
            if (rpe != null) append(" @ ${formatRpe(rpe)}")
        }
        if (splitText != null) append(", $splitText")
        if (isTopSet) append(", top set")
        if (isPr) append(", personal record")
        append(
            when (state) {
                SetState.PLANNED -> ", planned"
                SetState.DONE -> ", done"
                SetState.FAILED -> ", failed"
            },
        )
    }
    val line = buildAnnotatedString {
        withStyle(SpanStyle(color = strong)) { append(if (plates) PlateStack.text(weightKg) else formatKg(weightKg)) }
        withStyle(SpanStyle(color = soft)) { append("  ×  ") }
        withStyle(SpanStyle(color = strong)) { append(reps.toString()) }
        if (rpe != null) {
            withStyle(SpanStyle(color = soft)) { append("   @ ") }
            withStyle(SpanStyle(color = strong)) { append(formatRpe(rpe)) }
        }
    }

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Space.touch)
            .clip(MaterialTheme.shapes.small)
            .then(if (onClick != null) Modifier.pressClickable(onClick = onClick) else Modifier)
            .clearAndSetSemantics {
                text = AnnotatedString(spoken)
                if (drawIn != null && state == SetState.DONE) tickDrawIn = drawIn
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Text(
            index.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = soft,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(20.dp),
        )
        Column {
            Text(line, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
            if (splitText != null) {
                Text(splitText, style = MaterialTheme.typography.bodySmall, color = soft, maxLines = 1)
            }
        }
        if (isTopSet) Pill("Top set")
        if (isPr) Pill("PR", tone = Tone.SUCCESS, icon = Icons.Filled.EmojiEvents)
        Box(Modifier.weight(1f))
        when (state) {
            SetState.PLANNED -> Box(Modifier.padding(end = 2.dp).size(24.dp).border(1.5.dp, scheme.outline, CircleShape))
            SetState.DONE -> if (drawIn != null) DrawingTick(pt.success, pt.successContainer, drawIn) else StateDot(pt.success, pt.successContainer, done = true)
            SetState.FAILED -> {
                Pill("Failed", tone = Tone.DANGER)
                StateDot(pt.danger, pt.dangerContainer, done = false)
            }
        }
    }
}

@Composable
private fun StateDot(fg: Color, bg: Color, done: Boolean) {
    Box(Modifier.padding(end = 2.dp).size(24.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
        Icon(if (done) Icons.Filled.Check else Icons.Filled.Close, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
    }
}

/*
 * The tick drawing itself in (1.16, the sketch the owner approved): the circle grows from TICK_SCALE_FROM past full size
 * (TICK_SCALE_PEAK, at TICK_PEAK_AT of its time) and settles (by TICK_SETTLE_AT) as it fades in, and the check's line is
 * drawn from its start to its end, from TICK_DRAW_FROM. The web's guide draws the same.
 */
internal const val TICK_SCALE_FROM = 0.4f
internal const val TICK_SCALE_PEAK = 1.12f
internal const val TICK_PEAK_AT = 0.4f
internal const val TICK_SETTLE_AT = 0.7f
internal const val TICK_DRAW_FROM = 0.2f

/** The circle's size at [p] (0 to 1) through the tick. */
internal fun tickScale(p: Float): Float = when {
    p < TICK_PEAK_AT -> TICK_SCALE_FROM + (TICK_SCALE_PEAK - TICK_SCALE_FROM) * p / TICK_PEAK_AT
    p < TICK_SETTLE_AT -> TICK_SCALE_PEAK + (1f - TICK_SCALE_PEAK) * (p - TICK_PEAK_AT) / (TICK_SETTLE_AT - TICK_PEAK_AT)
    else -> 1f
}

/** The tick's opacity at [p]: in by its peak. */
internal fun tickAlpha(p: Float): Float = (p / TICK_PEAK_AT).coerceIn(0f, 1f)

/** How much of the check's line is drawn at [p]. */
internal fun tickLine(p: Float): Float = ((p - TICK_DRAW_FROM) / (1f - TICK_DRAW_FROM)).coerceIn(0f, 1f)

/**
 * The Check icon's centre line on its 24-unit grid: stroked 2 units wide, square-ended and mitred, it covers what the
 * filled icon does, so the drawn line ends as the tick that is always drawn.
 */
private val CHECK_LINE = listOf(Offset(4.12f, 12.705f), Offset(9f, 17.585f), Offset(20.295f, 6.295f))
private const val CHECK_LINE_WIDTH = 2f

@Composable
private fun DrawingTick(fg: Color, bg: Color, drawIn: () -> Float) {
    val segment = androidx.compose.runtime.remember { Path() }
    val measure = androidx.compose.runtime.remember { PathMeasure() }
    Box(
        Modifier
            .padding(end = 2.dp)
            .size(24.dp)
            .graphicsLayer {
                val p = drawIn()
                scaleX = tickScale(p)
                scaleY = tickScale(p)
                alpha = tickAlpha(p)
            }
            .background(bg, CircleShape)
            .drawBehind {
                // The icon is 16 of the dot's 24, centred: its 24 units in that square.
                val icon = 16.dp.toPx()
                val unit = icon / 24f
                val origin = Offset((size.width - icon) / 2, (size.height - icon) / 2)
                val line = Path().apply {
                    CHECK_LINE.forEachIndexed { i, o -> if (i == 0) moveTo(origin.x + o.x * unit, origin.y + o.y * unit) else lineTo(origin.x + o.x * unit, origin.y + o.y * unit) }
                }
                measure.setPath(line, false)
                segment.reset()
                measure.getSegment(0f, measure.length * tickLine(drawIn()), segment, true)
                drawPath(segment, fg, style = Stroke(width = CHECK_LINE_WIDTH * unit, cap = StrokeCap.Butt, join = StrokeJoin.Miter))
            },
    )
}

@Preview
@Composable
private fun SetRowPreview() = PowertrackerTheme(darkTheme = true) {
    PtCard(spacing = 0.dp) {
        SetRow(1, 150.0, 5, 7.5, SetState.DONE)
        SetRow(2, 155.0, 5, 8.5, SetState.DONE, isTopSet = true)
        SetRow(3, 155.0, 3, 10.0, SetState.FAILED)
        SetRow(4, 150.0, 5, 8.0, SetState.PLANNED)
    }
}
