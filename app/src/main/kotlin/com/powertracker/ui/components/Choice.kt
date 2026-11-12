package com.powertracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * A toggle chip. Selected is tonal (container highest, text colour, a 1 dp outline), not inverse:
 * the primary button stays the brightest filled element on a screen.
 */
@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .sizeIn(minWidth = Space.touch, minHeight = Space.touch)
            .clip(CircleShape)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(vertical = 4.dp)
            .background(if (selected) scheme.surfaceContainerHighest else scheme.surfaceContainerHigh, CircleShape)
            .border(1.dp, if (selected) scheme.outline else Color.Transparent, CircleShape)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** Room on each side of a segment's label when the segments are sized by their labels. */
private val SEGMENT_LABEL_PADDING = 6.dp

/**
 * Pick exactly one (or none yet) of a few short options: miss point, chart range. The segments
 * share the width equally when every label fits an equal share; when one does not ("Off the
 * shoulders" beside "Mid"), each gets its label's width and an equal share of what is left.
 */
@Composable
fun SegmentedChoice(options: List<String>, selectedIndex: Int?, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Layout(
        modifier = modifier.fillMaxWidth().background(scheme.surfaceContainerHigh, MaterialTheme.shapes.medium).padding(4.dp),
        content = {
            options.forEachIndexed { i, option ->
                val selected = i == selectedIndex
                Box(
                    Modifier
                        .heightIn(min = 40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(if (selected) scheme.surfaceContainerHighest else scheme.surfaceContainerHigh)
                        .border(1.dp, if (selected) scheme.outline else Color.Transparent, MaterialTheme.shapes.small)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(i) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        option,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val count = measurables.size
        val wanted = measurables.map { it.maxIntrinsicWidth(constraints.maxHeight) + 2 * SEGMENT_LABEL_PADDING.roundToPx() }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else wanted.sum()
        val widths = when {
            count == 0 -> emptyList()
            wanted.max() * count <= width -> equalShares(width, count)
            // Each label's width plus an equal share of the rest. Narrower than the labels, they all give up the same and clip.
            else -> equalShares(width - wanted.sum(), count).mapIndexed { i, share -> (wanted[i] + share).coerceAtLeast(0) }
        }
        val placeables = measurables.mapIndexed { i, segment -> segment.measure(constraints.copy(minWidth = widths[i], maxWidth = widths[i])) }
        layout(width, placeables.maxOfOrNull { it.height } ?: 0) {
            var x = 0
            for (placeable in placeables) {
                placeable.placeRelative(x, 0)
                x += placeable.width
            }
        }
    }
}

/** [total] pixels split into [count] whole shares the way a weighted row does it: rounded, the odd pixels to the first ones. */
private fun equalShares(total: Int, count: Int): List<Int> {
    val share = (total.toFloat() / count).roundToInt()
    val odd = total - share * count
    return List(count) { i -> share + if (i < abs(odd)) odd.sign else 0 }
}

@Preview
@Composable
private fun ChoicePreview() = PowertrackerTheme(darkTheme = true) {
    PtCard {
        SegmentedChoice(listOf("Floor", "Mid", "Lockout"), 1, {})
        ChoiceChip("Mon", true, {})
    }
}
