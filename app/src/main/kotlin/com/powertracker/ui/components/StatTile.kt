package com.powertracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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

/**
 * A labelled figure with an optional change note underneath ("+5 kg in 4 wk"). With [onClick] the
 * whole tile is a button that opens more about the figure, and a chevron says so; give it an
 * [onClickLabel] such as "Open squat history".
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    note: String? = null,
    noteTone: Tone = Tone.NEUTRAL,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
    Box(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick) else Modifier),
    ) {
        Column(Modifier.padding(Space.m), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            NumberDisplay(value, unit = unit, label = label, size = NumberSize.MEDIUM)
            if (note != null) Text(note, style = MaterialTheme.typography.labelMedium, color = noteTone.color(), maxLines = 1)
        }
        if (onClick != null) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = PowertrackerTheme.colors.muted,
                modifier = Modifier.align(Alignment.TopEnd).padding(Space.s).size(18.dp),
            )
        }
    }
}

@Preview
@Composable
private fun StatTilePreview() = PowertrackerTheme(darkTheme = true) {
    StatTile("Squat", "182.5", unit = "kg", note = "+5 kg in 4 wk", noteTone = Tone.SUCCESS)
}
