package com.powertracker.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A numeric value with minus/plus buttons; [label] names the buttons for accessibility. */
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
        IconButton(onClick = onDecrement, enabled = canDecrement) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrease $label")
        }
        Text(valueText, modifier = Modifier.widthIn(min = 48.dp).padding(horizontal = 4.dp))
        IconButton(onClick = onIncrement, enabled = canIncrement) {
            Icon(Icons.Filled.Add, contentDescription = "Increase $label")
        }
    }
}

/** Formats 8.0 as "8" and 7.5 as "7.5". */
fun formatRpe(rpe: Double): String = if (rpe % 1.0 == 0.0) rpe.toInt().toString() else rpe.toString()
