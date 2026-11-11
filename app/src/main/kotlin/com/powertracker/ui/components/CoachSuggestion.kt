package com.powertracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

/**
 * The coach's next-session target, shown inline on an exercise: "Coach suggests" + [headline]
 * (e.g. "155 × 5 @ 8") + the [reason], with Apply and Dismiss. Nothing changes until Apply.
 */
@Composable
fun CoachSuggestion(headline: String, reason: String, onApply: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val pt = PowertrackerTheme.colors
    Row(
        modifier.fillMaxWidth().background(pt.accentContainer, MaterialTheme.shapes.medium).padding(start = Space.m, top = Space.s, bottom = Space.s, end = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = pt.onAccentContainer, modifier = Modifier.size(14.dp))
                Eyebrow("Coach suggests", color = pt.onAccentContainer)
            }
            Text(headline, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PrimaryButton("Apply", onApply, compact = true, modifier = Modifier.padding(start = Space.s))
        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Preview
@Composable
private fun CoachSuggestionPreview() = PowertrackerTheme(darkTheme = true) {
    CoachSuggestion("155 × 5 @ 8", "Last time you hit 150 × 5 @ 7.", {}, {})
}
