package com.powertracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.powertracker.ui.theme.PowertrackerTheme

enum class NumberSize { HERO, LARGE, MEDIUM }

/**
 * Upper-case caption above a value or a group. One line by default, cut where it ends; [maxLines] lets a caption that
 * must be read whole in a narrow place (a superset's tag on a small phone's Plan row) go on to a second line.
 */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = PowertrackerTheme.colors.muted, maxLines: Int = 1) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, maxLines = maxLines, modifier = modifier)
}

/** A glanceable figure: optional eyebrow label, a big tabular number, and a small unit on the baseline. */
@Composable
fun NumberDisplay(
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    label: String? = null,
    size: NumberSize = NumberSize.LARGE,
    color: Color = MaterialTheme.colorScheme.onSurface,
    centered: Boolean = false,
) {
    val type = MaterialTheme.typography
    val style = when (size) {
        NumberSize.HERO -> type.displayLarge
        NumberSize.LARGE -> type.displayMedium
        NumberSize.MEDIUM -> type.displaySmall
    }
    Column(modifier, horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        if (label != null) Eyebrow(label)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = style, color = color, maxLines = 1, modifier = Modifier.alignByBaseline())
            if (unit != null) {
                Text(unit, style = type.titleSmall, color = PowertrackerTheme.colors.muted, maxLines = 1, modifier = Modifier.alignByBaseline())
            }
        }
    }
}

@Preview
@Composable
private fun NumberDisplayPreview() = PowertrackerTheme(darkTheme = true) {
    PtCard {
        NumberDisplay("512.5", unit = "kg", label = "Total", size = NumberSize.HERO)
        NumberDisplay("155", unit = "kg", label = "Weight")
    }
}
