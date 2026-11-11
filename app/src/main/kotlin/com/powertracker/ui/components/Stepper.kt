package com.powertracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = scheme.surfaceContainerHighest,
            contentColor = scheme.onSurface,
            disabledContainerColor = scheme.surfaceContainer,
            disabledContentColor = scheme.onSurfaceVariant.copy(alpha = 0.4f),
        ),
    ) { Icon(icon, contentDescription = description) }
}

/** A numeric value with minus/plus buttons on one line; [label] names the buttons for accessibility. */
@Composable
fun Stepper(
    label: String,
    valueText: String,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
        StepButton(Icons.Filled.Remove, "Decrease $label", canDecrement, onDecrement, Modifier.size(Space.touch))
        Text(
            valueText,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 72.dp).padding(horizontal = Space.s),
        )
        StepButton(Icons.Filled.Add, "Increase $label", canIncrement, onIncrement, Modifier.size(Space.touch))
    }
}

/** The in-session stepper: label, a big number, and two wide buttons underneath. Place three in a row. */
@Composable
fun StepperField(
    label: String,
    valueText: String,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        NumberDisplay(valueText, unit = unit, label = label, size = NumberSize.LARGE, centered = true)
        Row(Modifier.fillMaxWidth().padding(top = Space.s), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            StepButton(Icons.Filled.Remove, "Decrease $label", canDecrement, onDecrement, Modifier.weight(1f).height(Space.touch))
            StepButton(Icons.Filled.Add, "Increase $label", canIncrement, onIncrement, Modifier.weight(1f).height(Space.touch))
        }
    }
}

@Preview
@Composable
private fun StepperPreview() = PowertrackerTheme(darkTheme = true) {
    PtCard {
        Stepper("Sets", "3", true, true, {}, {})
        Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            StepperField("Weight", "155", true, true, {}, {}, Modifier.weight(1.3f), unit = "kg")
            StepperField("Reps", "5", true, true, {}, {}, Modifier.weight(1f))
            StepperField("RPE", "8", true, true, {}, {}, Modifier.weight(1f))
        }
    }
}
