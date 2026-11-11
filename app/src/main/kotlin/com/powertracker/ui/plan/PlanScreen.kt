package com.powertracker.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
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
import com.powertracker.domain.validation.InputValidator
import com.powertracker.platform.systemToday
import com.powertracker.ui.components.Eyebrow
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.PtCard
import com.powertracker.ui.components.QuietButton
import com.powertracker.ui.components.ScreenHeader
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.components.SectionHeader
import com.powertracker.ui.components.Stepper
import com.powertracker.ui.components.formatRpe
import com.powertracker.ui.library.ExercisePickerScreen
import com.powertracker.ui.library.InfoButton
import com.powertracker.ui.library.PickerEntry
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaDayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** The text shown for one plan item, e.g. "Squat  3 × 5 @ 8". */
fun planItemText(name: String, item: PlanItem): String = "$name  ${item.sets} × ${item.reps} @ ${formatRpe(item.targetRpe)}"

fun dayName(day: DayOfWeek): String = day.toJavaDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault())

// Dialogs holding text fields use full-width properties: with the platform-default width the field
// never settles under Robolectric (the Compose test never goes idle).
private val TEXT_FIELD_DIALOG = DialogProperties(usePlatformDefaultWidth = false)

private sealed interface PlanDialog {
    /** The full-screen picker, adding to [day]. */
    data class Add(val day: DayOfWeek) : PlanDialog
    data class Edit(val item: PlanItem) : PlanDialog
    data class Baseline(val lift: Exercise) : PlanDialog
}

@Composable
fun PlanScreen(
    training: TrainingRepository,
    settings: SettingsRepository,
    onOpenSettings: () -> Unit,
    today: () -> LocalDate = ::systemToday,
    viewModel: PlanViewModel = viewModel(factory = PlanViewModel.factory(training, settings, today)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<PlanDialog?>(null) }

    PlanContent(
        state = state,
        onOpenSettings = onOpenSettings,
        onAdd = { day -> dialog = PlanDialog.Add(day) },
        onEdit = { item -> dialog = PlanDialog.Edit(item) },
        onMove = viewModel::move,
        onDelete = viewModel::delete,
        onSetBaseline = { lift -> dialog = PlanDialog.Baseline(lift) },
    )

    when (val d = dialog) {
        // Picking adds the exercise with its default sets and reps; they are edited from the row, as before.
        is PlanDialog.Add -> ExercisePickerScreen(
            title = "Add to ${dayName(d.day)}",
            exercises = state.exercises,
            recentIds = state.recentIds,
            onPick = { entry ->
                viewModel.add(d.day, entry)
                dialog = null
            },
            onCreateCustom = { name, category, parent, muscles ->
                viewModel.addCustomExercise(name, category, parent, muscles) { entry -> viewModel.add(d.day, entry) }
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is PlanDialog.Edit -> PlanItemDialog(
            title = "Edit ${dayName(d.item.weekday)}",
            state = state,
            item = d.item,
            onCreateCustom = viewModel::addCustomExercise,
            onSave = { exercise, sets, reps, rpe ->
                viewModel.save(d.item, exercise, sets, reps, rpe)
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

/** The Plan screen without its view model or dialogs: one card per training day, then known bests. */
@Composable
fun PlanContent(
    state: PlanUiState,
    onOpenSettings: () -> Unit,
    onAdd: (DayOfWeek) -> Unit,
    onEdit: (PlanItem) -> Unit,
    onMove: (PlanItem, Int) -> Unit,
    onDelete: (PlanItem) -> Unit,
    onSetBaseline: (Exercise) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = scheme.background,
        topBar = {
            ScreenHeader("Plan", eyebrow = "Weekly template") {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = scheme.onSurfaceVariant)
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxWidth(),
            contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, top = Space.s, bottom = Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            items(state.sections, key = { it.day }) { section ->
                DayCard(section, onAdd = { onAdd(section.day) }, onEdit = onEdit, onMove = onMove, onDelete = onDelete)
            }
            item(key = "bests") {
                Column(Modifier.padding(top = Space.s)) {
                    SectionHeader("Known bests")
                    PtCard(contentPadding = PaddingValues(horizontal = Space.l, vertical = Space.s), spacing = 0.dp) {
                        Text(
                            "Tell the coach your current maxes so it can advise from day one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Space.s),
                        )
                        for (lift in state.mainExercises) {
                            HorizontalDivider(color = PowertrackerTheme.colors.hairline)
                            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(lift.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                SecondaryButton("Set best", { onSetBaseline(lift) }, compact = true, modifier = Modifier.testTag("baseline-${lift.name}"))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCard(
    section: DaySection,
    onAdd: () -> Unit,
    onEdit: (PlanItem) -> Unit,
    onMove: (PlanItem, Int) -> Unit,
    onDelete: (PlanItem) -> Unit,
) {
    val rows = section.rows
    val summary = if (rows.isEmpty()) {
        "Rest day"
    } else {
        val sets = rows.sumOf { it.item.sets }
        "${rows.size} ${if (rows.size == 1) "exercise" else "exercises"} · $sets sets"
    }
    PtCard(contentPadding = PaddingValues(start = Space.l, end = Space.s, top = Space.m, bottom = Space.s), spacing = 0.dp) {
        Row(Modifier.fillMaxWidth().padding(end = Space.s, bottom = Space.xs), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(dayName(section.day), style = MaterialTheme.typography.headlineMedium)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = PowertrackerTheme.colors.muted)
            }
            SecondaryButton(
                "Add", onAdd, compact = true, icon = Icons.Filled.Add,
                modifier = Modifier.testTag("add-${section.day}").semantics { contentDescription = "Add exercise to ${dayName(section.day)}" },
            )
        }
        rows.forEachIndexed { i, row ->
            HorizontalDivider(Modifier.padding(end = Space.s), color = PowertrackerTheme.colors.hairline)
            PlanItemRow(
                position = i + 1,
                name = row.exerciseName,
                item = row.item,
                canMoveUp = i > 0,
                canMoveDown = i < rows.lastIndex,
                onMove = { onMove(row.item, it) },
                onEdit = { onEdit(row.item) },
                onDelete = { onDelete(row.item) },
            )
        }
    }
}

@Composable
private fun PlanItemRow(
    position: Int,
    name: String,
    item: PlanItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val muted = PowertrackerTheme.colors.muted
    var menuOpen by remember { mutableStateOf(false) }
    val prescription = buildAnnotatedString {
        append("${item.sets} × ${item.reps}")
        withStyle(SpanStyle(color = muted)) { append("  @ ") }
        append(formatRpe(item.targetRpe))
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(position.toString(), style = MaterialTheme.typography.titleSmall, color = muted, modifier = Modifier.width(24.dp))
        // Read and found as one line, e.g. "Squat  3 × 5 @ 8".
        Column(Modifier.weight(1f).padding(vertical = Space.s).clearAndSetSemantics { text = AnnotatedString(planItemText(name, item)) }) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            Text(prescription, style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface)
        }
        InfoButton(name)
        IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) { Icon(Icons.Filled.KeyboardArrowUp, "Move up") }
        IconButton(onClick = { onMove(1) }, enabled = canMoveDown) { Icon(Icons.Filled.KeyboardArrowDown, "Move down") }
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "More") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Filled.Edit, null) },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = scheme.error) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = scheme.error) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}

/** Sets, reps and RPE of a plan item, and its exercise, which "Change" swaps through the picker. */
@Composable
private fun PlanItemDialog(
    title: String,
    state: PlanUiState,
    item: PlanItem,
    onCreateCustom: (String, Category, MainLift?, List<String>, (PickerEntry) -> Unit) -> Unit,
    onSave: (exercise: PickerEntry?, sets: Int, reps: Int, rpe: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    // Null until the exercise is changed; saved with the item, not before.
    var changed by rememberSaveable(stateSaver = PickerEntry.Saver) { mutableStateOf(null) }
    var sets by rememberSaveable { mutableStateOf(item.sets) }
    var reps by rememberSaveable { mutableStateOf(item.reps) }
    var rpe by rememberSaveable { mutableStateOf(item.targetRpe) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val name = changed?.name ?: state.exercises.firstOrNull { it.id == item.exerciseId }?.name ?: "Unknown exercise"

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = TEXT_FIELD_DIALOG,
        modifier = Modifier.padding(horizontal = 16.dp),
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth().padding(bottom = Space.s), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow("Exercise")
                        Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    SecondaryButton("Change", { picking = true }, compact = true, modifier = Modifier.semantics { contentDescription = "Change exercise" })
                }
                LabeledStepper("Sets", sets.toString(), sets > 1, sets < 10, { sets-- }, { sets++ })
                LabeledStepper("Reps", reps.toString(), reps > 1, reps < 20, { reps-- }, { reps++ })
                LabeledStepper("RPE", formatRpe(rpe), rpe > PlanViewModel.MIN_RPE, rpe < PlanViewModel.MAX_RPE, { rpe -= 0.5 }, { rpe += 0.5 })
            }
        },
        confirmButton = { PrimaryButton("Save", { onSave(changed, sets, reps, rpe) }, compact = true) },
        dismissButton = { QuietButton("Cancel", onDismiss) },
    )

    if (picking) {
        ExercisePickerScreen(
            title = "Change exercise",
            exercises = state.exercises,
            recentIds = state.recentIds,
            onPick = { entry ->
                changed = entry
                picking = false
            },
            onCreateCustom = { customName, category, parent, muscles ->
                onCreateCustom(customName, category, parent, muscles) { entry -> changed = entry }
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun LabeledStepper(label: String, value: String, canDec: Boolean, canInc: Boolean, onDec: () -> Unit, onInc: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = Space.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Stepper(label, value, canDec, canInc, onDec, onInc)
    }
}

@Composable
private fun BaselineDialog(lift: Exercise, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    // Only finite values up to 500 kg: anything else would be nonsense advice, and non-finite numbers break backups.
    val kg = InputValidator.baselineKg(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = TEXT_FIELD_DIALOG,
        modifier = Modifier.padding(horizontal = 16.dp),
        title = { Text("Known best: ${lift.name}") },
        text = {
            OutlinedTextField(
                text, { text = it }, label = { Text("Estimated 1RM (kg)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = { PrimaryButton("Save", { onSave(kg!!) }, enabled = kg != null, compact = true) },
        dismissButton = { QuietButton("Cancel", onDismiss) },
    )
}
