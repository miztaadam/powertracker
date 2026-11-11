package com.powertracker.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.validation.InputWarning
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.QuietButton
import com.powertracker.ui.components.formatKg
import com.powertracker.ui.theme.Space

// Dialogs holding text fields use full-width properties: with the platform-default width the field
// never settles under Robolectric (the Compose test never goes idle).
private val TEXT_FIELD_DIALOG = DialogProperties(usePlatformDefaultWidth = false)

/** Type a number instead of stepping to it. Opens with the current value selected, so typing replaces it. */
@Composable
fun NumberEntryDialog(
    title: String,
    unit: String?,
    initial: Double?,
    max: Double,
    whole: Boolean,
    onDismiss: () -> Unit,
    min: Double = 0.0,
    onSet: (Double) -> Unit,
) {
    val start = initial?.let(::formatKg).orEmpty()
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(start, TextRange(0, start.length))) }
    val value = field.text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in min..max && (!whole || it % 1.0 == 0.0) }
    val focus = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = TEXT_FIELD_DIALOG,
        modifier = Modifier.padding(horizontal = 16.dp),
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = { field = it },
                singleLine = true,
                isError = field.text.isNotBlank() && value == null,
                textStyle = MaterialTheme.typography.headlineMedium,
                suffix = unit?.let { { Text(it, style = MaterialTheme.typography.titleSmall) } },
                keyboardOptions = KeyboardOptions(keyboardType = if (whole) KeyboardType.Number else KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { value?.let(onSet) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("number-entry"),
            )
        },
        confirmButton = { PrimaryButton("Set", { value?.let(onSet) }, enabled = value != null, compact = true) },
        dismissButton = { QuietButton("Cancel", onDismiss) },
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** "Did you mean…?" — shown before a set that looks like a typo is saved. */
@Composable
fun TypoDialog(pending: PendingLog, onConfirm: () -> Unit, onEdit: () -> Unit) {
    val set = pending.set
    AlertDialog(
        onDismissRequest = onEdit,
        title = { Text("Did you mean ${formatKg(set.weightKg)} kg × ${set.reps}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                for (warning in pending.warnings) {
                    Text(
                        when (warning) {
                            InputWarning.HighReps -> "${set.reps} reps is a lot for one set."
                            is InputWarning.BigJump -> "That is a big jump from ${formatKg(warning.fromKg)} kg."
                        },
                    )
                }
                Text("Nothing is saved yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { PrimaryButton("Confirm", onConfirm, compact = true) },
        dismissButton = { QuietButton("Edit", onEdit) },
    )
}

/** Search the library and tap an exercise to add it to this session. */
@Composable
fun ExercisePickerDialog(exercises: List<Exercise>, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim().lowercase()
    val matches = exercises.filter { q.isEmpty() || it.name.lowercase().contains(q) }
        .sortedWith(compareByDescending<Exercise> { it.name.lowercase().startsWith(q) }.thenBy { it.name.lowercase() })

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = TEXT_FIELD_DIALOG,
        modifier = Modifier.padding(horizontal = 16.dp),
        title = { Text("Add exercise") },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, label = { Text("Search exercises") },
                    singleLine = true, modifier = Modifier.fillMaxWidth().testTag("exercise-search"),
                )
                Column(Modifier.padding(top = Space.s).heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    for (exercise in matches) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = Space.touch)
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onPick(exercise.id) }
                                .padding(horizontal = Space.m)
                                .testTag("exercise-${exercise.name}"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(exercise.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if (matches.isEmpty()) {
                        Text(
                            "No exercise matches. Custom exercises are created on the Plan tab.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(Space.m),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { QuietButton("Cancel", onDismiss) },
    )
}
