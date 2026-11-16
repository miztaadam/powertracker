package com.powertracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingFlat
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
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone
import com.powertracker.ui.theme.color
import com.powertracker.ui.theme.container

/**
 * The shell of every coach card: a toned icon and tag say what kind of advice it is, then what to
 * do ([title]), why ([body]), optional detail ([content]: accessories, a mini chart), and actions.
 * Tones: STALL = WARNING, DELOAD = DANGER, WEAK_POINT / RE_ENTRY = INFO, TARGET = ACCENT.
 */
@Composable
fun AdviceCard(
    tone: Tone,
    icon: ImageVector,
    tag: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    primaryAction: String? = null,
    onPrimary: () -> Unit = {},
    secondaryAction: String? = null,
    onSecondary: () -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    PtCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            Box(Modifier.size(36.dp).background(tone.container(), MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tone.color(), modifier = Modifier.size(20.dp))
            }
            Eyebrow(tag, color = tone.color())
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content?.invoke(this)
        if (primaryAction != null || secondaryAction != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                // The primary action is the card's Apply ("Add to plan", "Apply deload" and so on): it ticks.
                val haptics = rememberHaptics()
                if (primaryAction != null) {
                    PrimaryButton(
                        primaryAction,
                        {
                            haptics.tick()
                            onPrimary()
                        },
                        compact = true,
                    )
                }
                if (secondaryAction != null) QuietButton(secondaryAction, onSecondary)
            }
        }
    }
}

@Preview
@Composable
private fun AdviceCardPreview() = PowertrackerTheme(darkTheme = true) {
    AdviceCard(
        Tone.WARNING, Icons.Filled.TrendingFlat, "Stall · Bench", "Bench has stalled", "Best weekly e1RM flat for 3 weeks.",
        primaryAction = "Add to plan", secondaryAction = "Dismiss",
    )
}
