package com.powertracker.ui.library

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.powertracker.ui.theme.Space

/**
 * The info button beside an exercise's name: opens its form guide. It goes wherever an exercise is
 * shown, custom ones included, so rows line up; the sheet says when there is no guide.
 */
@Composable
fun InfoButton(exerciseName: String, modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    InfoIconButton(exerciseName, onClick = { open = true }, modifier)
    if (open) FormGuideSheet(exerciseName, onDismiss = { open = false })
}

/** The button alone, where the screen opens the guide itself (rows of the exercise browser). */
@Composable
fun InfoIconButton(exerciseName: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // A full 48 dp square, not the 40 dp an icon button draws itself at: it sits in rows that are tapped too.
    IconButton(onClick = onClick, modifier = modifier.size(Space.touch)) {
        Icon(Icons.Outlined.Info, contentDescription = "Form guide for $exerciseName", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
