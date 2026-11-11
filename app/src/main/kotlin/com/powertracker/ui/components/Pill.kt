package com.powertracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Tone
import com.powertracker.ui.theme.color
import com.powertracker.ui.theme.container

/** A small, non-interactive status label. The words carry the meaning; the tone supports them. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, tone: Tone = Tone.NEUTRAL, icon: ImageVector? = null) {
    Row(
        modifier.background(tone.container(), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = tone.color(), modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = tone.color(), maxLines = 1)
    }
}

@Preview
@Composable
private fun PillPreview() = PowertrackerTheme(darkTheme = true) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill("PR", tone = Tone.SUCCESS, icon = Icons.Filled.EmojiEvents)
        Pill("Stall", tone = Tone.WARNING)
        Pill("Failed", tone = Tone.DANGER)
        Pill("Set 4 of 5")
    }
}
