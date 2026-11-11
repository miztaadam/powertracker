package com.powertracker.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.ui.common.Stepper
import com.powertracker.ui.common.formatRpe
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** The text shown for one plan item, e.g. "Squat  3 × 5 @ 8". */
fun planItemText(name: String, item: PlanItem): String = "$name  ${item.sets} × ${item.reps} @ ${formatRpe(item.targetRpe)}"

fun dayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, Locale.getDefault())

// Dialogs holding text fields use full-width properties: with the platform-default width the field
// never settles under Robolectric (the Compose test never goes idle).
private val TEXT_FIELD_DIALOG = DialogProperties(usePlatformDefaultWidth = false)

private sealed interface PlanDialog {
    data class Edit(val day: DayOfWeek, val item: PlanItem?) : PlanDialog
    data class Baseline(val lift: Exercise) : PlanDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    training: TrainingRepository,
    settings: SettingsRepository,
    onOpenSettings: () -> Unit,
    today: () -> LocalDate = LocalDate::now,
    viewModel: PlanViewModel = viewModel(factory = PlanViewModel.factory(training, settings, today)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<PlanDialog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan") },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (section in state.sections) {
                item(key = section.day) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(dayName(section.day), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { dialog = PlanDialog.Edit(section.day, null) }, modifier = Modifier.testTag("add-${section.day}")) {
                            Text("Add exercise")
                        }
                    }
                }
                if (section.rows.isEmpty()) {
                    item(key = "empty-${section.day}") { Text("Rest day", style = MaterialTheme.typography.bodyMedium) }
                }
                items(section.rows, key = { it.item.id }) { row ->
                    PlanItemRow(
                        text = planItemText(row.exerciseName, row.item),
                        canMoveUp = row != section.rows.first(),
                        canMoveDown = row != section.rows.last(),
                        onMove = { viewModel.move(row.item, it) },
                        onEdit = { dialog = PlanDialog.Edit(section.day, row.item) },
                        onDelete = { viewModel.delete(row.item) },
                    )
                }
                item(key = "divider-${section.day}") { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            }
            item(key = "bests-header") { Text("Known bests", style = MaterialTheme.typography.titleMedium) }
            items(state.mainExercises, key = { "best-${it.id}" }) { lift ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(lift.name)
                    TextButton(onClick = { dialog = PlanDialog.Baseline(lift) }, modifier = Modifier.testTag("baseline-${lift.name}")) {
                        Text("Set a known best")
                    }
                }
            }
        }
    }

    when (val d = dialog) {
        is PlanDialog.Edit -> PlanItemDialog(
            title = "${if (d.item == null) "Add to" else "Edit"} ${dayName(d.day)}",
            exercises = state.exercises,
            initial = d.item,
            defaultRpe = state.defaultRpe,
            onCreateCustom = viewModel::addCustomExercise,
            onSave = { exerciseId, sets, reps, rpe ->
                viewModel.save(d.day, d.item, exerciseId, sets, reps, rpe)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is PlanDialog.Baseline -> BaselineDialog(
            lift = d.lift,
            onSave = { kg ->
                viewModel.setBaseline(d.lift.id, kg)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun PlanItemRow(
    text: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f))
        IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) { Icon(Icons.Filled.KeyboardArrowUp, "Move up") }
        IconButton(onClick = { onMove(1) }, enabled = canMoveDown) { Icon(Icons.Filled.KeyboardArrowDown, "Move down") }
        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit") }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete") }
    }
}

@Composable
private fun PlanItemDialog(
    title: String,
    exercises: List<Exercise>,
    initial: PlanItem?,
    defaultRpe: Double,
    onCreateCustom: (String, Category, MainLift?, List<String>, (Long) -> Unit) -> Unit,
    onSave: (exerciseId: Long, sets: Int, reps: Int, rpe: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf(initial?.exerciseId) }
    var sets by rememberSaveable { mutableStateOf(initial?.sets ?: 3) }
    var reps by rememberSaveable { mutableStateOf(initial?.reps ?: 5) }
    var rpe by rememberSaveable { mutableStateOf(initial?.targetRpe ?: defaultRpe.coerceIn(6.0, 10.0)) }
    var showCustom by rememberSaveable { mutableStateOf(false) }

    val q = query.trim().lowercase()
    val matches = exercises.filter { q.isEmpty() || it.name.lowercase().contains(q) }
        .sortedWith(compareByDescending<Exercise> { it.name.lowercase().startsWith(q) }.thenBy { it.name.lowercase() })
    val selected = exercises.firstOrNull { it.id == selectedId }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = TEXT_FIELD_DIALOG,
        modifier = Modifier.padding(horizontal = 16.dp),
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, label = { Text("Search exercises") },
                    singleLine = true, modifier = Modifier.fillMaxWidth().testTag("exercise-search"),
                )
                Column(Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                    for (ex in matches) {
                        Text(
                            ex.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (ex.id == selectedId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth().clickable { selectedId = ex.id }.padding(vertical = 10.dp).testTag("exercise-${ex.name}"),
                        )
                    }
                }
                TextButton(onClick = { showCustom = true }) { Text("Add custom exercise") }
                Text("Selected: ${selected?.name ?: "none"}", style = MaterialTheme.typography.bodyMedium)
                LabeledStepper("Sets", sets.toString(), sets > 1, sets < 10, { sets-- }, { sets++ })
                LabeledStepper("Reps", reps.toString(), reps > 1, reps < 20, { reps-- }, { reps++ })
                LabeledStepper("RPE", formatRpe(rpe), rpe > 6.0, rpe < 10.0, { rpe -= 0.5 }, { rpe += 0.5 })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selectedId!!, sets, reps, rpe) }, enabled = selected != null) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (showCustom) {
        CustomExerciseDialog(
            onCreate = { name, category, parent, muscles ->
                onCreateCustom(name, category, parent, muscles) { id -> selectedId = id }
                showCustom = false
            },
            onDismiss = { showCustom = false },
        )
    }
}

@Composable
private fun LabeledStepper(label: String, value: String, canDec: Boolean, canInc: Boolean, onDec: () -> Unit, onInc: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Stepper(label, value, canDec, canInc, onDec, onInc)
    }
}

@Composable
private fun CustomExerciseDialog(
    onCreate: (String, Category, MainLift?, List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(Category.ACCESSORY) }
    var parent by rememberSaveable { mutableStateOf<MainLift?>(null) }
    var muscles by rememberSaveable { mutableStateOf("") }
    val valid = name.isNotBlank() && (category != Category.VARIATION || parent != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = TEXT_FIELD_DIALOG,
        modifier = Modifier.padding(horizontal = 16.dp),
        title = { Text("Add custom exercise") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(category == Category.VARIATION, { category = Category.VARIATION }, label = { Text("Variation") })
                    FilterChip(category == Category.ACCESSORY, { category = Category.ACCESSORY }, label = { Text("Accessory") })
                }
                if (category == Category.VARIATION) {
                    Text("Parent lift", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (lift in MainLift.entries) {
                            FilterChip(parent == lift, { parent = lift }, label = { Text(lift.name.lowercase().replaceFirstChar { it.uppercase() }) })
                        }
                    }
                }
                OutlinedTextField(
                    muscles, { muscles = it }, label = { Text("Muscles (comma-separated)") }, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val list = muscles.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onCreate(name, category, if (category == Category.VARIATION) parent else null, list)
                },
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun BaselineDialog(lift: Exercise, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val kg = text.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = TEXT_FIELD_DIALOG,
        modifier = Modifier.padding(horizontal = 16.dp),
        title = { Text("Known best: ${lift.name}") },
        text = {
            OutlinedTextField(
                text, { text = it }, label = { Text("Estimated 1RM (kg)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(kg!!) }, enabled = kg != null) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
