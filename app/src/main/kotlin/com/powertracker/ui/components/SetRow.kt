package com.powertracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone

enum class SetState { PLANNED, DONE, FAILED }

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
) {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    val strong = if (state == SetState.PLANNED) scheme.onSurfaceVariant else scheme.onSurface
    val soft = pt.muted
    val spoken = buildString {
        append("Set $index: ${formatKg(weightKg)} kg × $reps")
        if (rpe != null) append(" @ ${formatRpe(rpe)}")
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
        withStyle(SpanStyle(color = strong)) { append(formatKg(weightKg)) }
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
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .clearAndSetSemantics { text = AnnotatedString(spoken) },
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
        Text(line, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
        if (isTopSet) Pill("Top set")
        if (isPr) Pill("PR", tone = Tone.SUCCESS, icon = Icons.Filled.EmojiEvents)
        Box(Modifier.weight(1f))
        when (state) {
            SetState.PLANNED -> Box(Modifier.padding(end = 2.dp).size(24.dp).border(1.5.dp, scheme.outline, CircleShape))
            SetState.DONE -> StateDot(pt.success, pt.successContainer, done = true)
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
