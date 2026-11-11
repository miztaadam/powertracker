package com.powertracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone
import com.powertracker.ui.theme.color

/** A labelled figure with an optional change note underneath ("+5 kg in 4 wk"). */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    note: String? = null,
    noteTone: Tone = Tone.NEUTRAL,
) {
    Column(
        modifier.background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium).padding(Space.m),
        verticalArrangement = Arrangement.spacedBy(Space.xxs),
    ) {
        NumberDisplay(value, unit = unit, label = label, size = NumberSize.MEDIUM)
        if (note != null) Text(note, style = MaterialTheme.typography.labelMedium, color = noteTone.color(), maxLines = 1)
    }
}

@Preview
@Composable
private fun StatTilePreview() = PowertrackerTheme(darkTheme = true) {
    StatTile("Squat", "182.5", unit = "kg", note = "+5 kg in 4 wk", noteTone = Tone.SUCCESS)
}
