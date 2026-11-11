package com.powertracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

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

/** Pick exactly one (or none yet) of a few short options: miss point, chart range. */
@Composable
fun SegmentedChoice(options: List<String>, selectedIndex: Int?, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().background(scheme.surfaceContainerHigh, MaterialTheme.shapes.medium).padding(4.dp),
    ) {
        options.forEachIndexed { i, option ->
            val selected = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
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
    }
}

@Preview
@Composable
private fun ChoicePreview() = PowertrackerTheme(darkTheme = true) {
    PtCard {
        SegmentedChoice(listOf("Floor", "Mid", "Lockout"), 1, {})
        ChoiceChip("Mon", true, {})
    }
}
