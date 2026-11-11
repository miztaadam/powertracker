package com.powertracker.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.powertracker.ui.theme.PowertrackerTheme

private val Regular = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
private val Compact = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

@Composable
private fun Label(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
}

// Compact buttons are 40 dp tall to look at, but Material pads their touch target to 48 dp
// (minimum interactive size), so they still meet Space.touch. ComponentsTest checks this.

/** The one accent-filled action on a screen or card. [compact] is for use inside cards and rows. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    compact: Boolean = false,
) {
    val c = PowertrackerTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = if (compact) 40.dp else 52.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
        contentPadding = if (compact) Compact else Regular,
    ) { Label(text, icon) }
}

/** A neutral tonal action that sits beside or below a primary one. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    compact: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = if (compact) 40.dp else 52.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = if (compact) Compact else Regular,
    ) { Label(text, icon) }
}

/** A text-only action: dismiss, cancel, skip. */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 40.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        contentPadding = Compact,
    ) { Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
}

@Preview
@Composable
private fun ButtonsPreview() = PowertrackerTheme(darkTheme = true) {
    PtCard {
        PrimaryButton("Log set", {})
        SecondaryButton("+30 s", {}, compact = true)
        QuietButton("Dismiss", {})
    }
}
