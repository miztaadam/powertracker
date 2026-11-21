package com.powertracker.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.PlateCount
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.PtDropdownMenuItem
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

// Counting plates on a stack machine (1.17, plate-count design "The setting"). The web app's Today uses the same words.
const val COUNT_PLATES = "Count plates on this machine"

/** The sheet's accessible name; its heading alone would share a name with the form guide's sheet. */
fun plateSheetLabel(name: String): String = "Count plates on $name"

const val PLATE_QUESTION = "How does this machine show its weight?"

/** The sheet's three choices, in its order: the label and the grid's hint. */
val PLATE_CHOICES: List<Triple<PlateCount, String, String?>> = listOf(
    Triple(PlateCount.OFF, "In kg", null),
    Triple(PlateCount.WHOLE, "Whole plates", "1, 2, 3 …"),
    Triple(PlateCount.HALF, "Half steps too", "1, 1½, 2 …"),
)

/**
 * The line under the choices: only when the choice starts a new unit ([chosen] is not [opened], the unit when the sheet
 * opened) and the exercise has sets in the unit it leaves. Whole plates ↔ half steps is never a change of unit.
 */
fun plateNote(opened: LoadUnit, chosen: LoadUnit, hasSetsInOpenedUnit: Boolean): String? = when {
    chosen == opened || !hasSetsInOpenedUnit -> null
    chosen == LoadUnit.PLATES -> "The coach starts this exercise fresh in plates. Your kg sets stay as they are."
    else -> "The coach goes back to kg. Your plate sets stay as they are."
}

/** The row menu's plate item; internal so the mockups can draw it outside its popup. */
@Composable
internal fun PlateMenuItem(onClick: () -> Unit) {
    Box(Modifier.testTag("exercise-menu-plates")) {
        PtDropdownMenuItem(text = { Text(COUNT_PLATES) }, leadingIcon = { Icon(Icons.Filled.Tune, null) }, onClick = onClick)
    }
}

/**
 * Asks how [name]'s machine shows its weight. A choice applies when tapped ([onChoose]); Done, back or a tap outside
 * only close it ([onDismiss]). [current] is the stored setting; [note] is [plateNote]'s line, or null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlateCountSheet(name: String, current: PlateCount, note: String?, onChoose: (PlateCount) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = scheme.outline) },
    ) {
        PlateCountContent(name, current, note, onChoose, onDone = onDismiss, Modifier.navigationBarsPadding())
    }
}

/** The sheet's content without the sheet. Internal for the mockups. It scrolls, so Done is reachable at any text size. */
@Composable
internal fun PlateCountContent(name: String, current: PlateCount, note: String?, onChoose: (PlateCount) -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val pt = PowertrackerTheme.colors
    // Marked at once; the stored setting catches up a moment later, and wins when it changes.
    var marked by remember(current) { mutableStateOf(current) }
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = Space.xl, end = Space.xl, bottom = Space.xl)
            .testTag("plate-sheet")
            .semantics { paneTitle = plateSheetLabel(name) },
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Text(name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(PLATE_QUESTION, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.selectableGroup().testTag("plate-options")) {
            for ((value, label, hint) in PLATE_CHOICES) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = Space.touch)
                        .selectable(selected = marked == value, role = Role.RadioButton) {
                            marked = value
                            if (value != current) onChoose(value)
                        }
                        .testTag("plate-option-${value.name}")
                        .padding(vertical = Space.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    RadioButton(selected = marked == value, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = pt.accent))
                    // The label and its hint wrap under each other where they do not fit side by side.
                    WrappingPair(Modifier.weight(1f), first = { Text(label, style = MaterialTheme.typography.titleMedium) }) {
                        if (hint != null) Text(hint, style = MaterialTheme.typography.bodyMedium, color = pt.muted)
                    }
                }
            }
        }
        if (note != null) {
            Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("plate-note"))
        }
        PrimaryButton("Done", onDone, Modifier.fillMaxWidth().padding(top = Space.xs).testTag("plate-done"))
    }
}
