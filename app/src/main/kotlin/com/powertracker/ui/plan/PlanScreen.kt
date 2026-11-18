package com.powertracker.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.repsText
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
import com.powertracker.ui.components.SoftDivider
import com.powertracker.ui.library.ExercisePickerScreen
import com.powertracker.ui.library.FormMessage
import com.powertracker.ui.library.InfoButton
import com.powertracker.ui.library.PickerEntry
import com.powertracker.ui.library.RpeInfoButton
import com.powertracker.ui.library.SAVE_FAILED
import com.powertracker.ui.plan.builder.BUILD_MY_PLAN
import com.powertracker.ui.plan.builder.PLAN_OPTIONS
import com.powertracker.ui.plan.builder.REBUILD_MY_PLAN
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.today.SlidingColumnItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaDayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import com.powertracker.ui.components.rememberHaptics
import com.powertracker.ui.components.PtIconButton
import com.powertracker.ui.components.PtDropdownMenuItem

/** The text shown for one plan item, e.g. "Squat  3 × 5 @ 8", or "Curl  3 × 8–12 @ 8" for a rep range. */
fun planItemText(name: String, item: PlanItem): String = "$name  ${item.sets} × ${repsText(item.reps, item.repsHigh)} @ ${formatRpe(item.targetRpe)}"

/** The line an empty bodybuilding plan shows, over its "Build my plan" button (bodybuilding mode spec §7.3, stage 2). */
const val EMPTY_BODYBUILDING_PLAN = "Your bodybuilding plan is empty. Add exercises to each training day."

/** The most reps the top of a range can be set to. */
private const val MAX_RANGE_REPS = 30

/** The most fixed reps a plan item can be set to. */
private const val MAX_PLAN_REPS = 20

/**
 * How long, in ms, the add form ignores taps on Add, Cancel and outside it once it opens. It opens under the finger
 * that picked the exercise, so the second tap of a double tap on the row would otherwise land on it.
 */
internal const val ADD_FORM_TAP_WINDOW_MS = 300L

fun dayName(day: DayOfWeek): String = day.toJavaDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault())

// Dialogs holding text fields use full-width properties: with the platform-default width the field
// never settles under Robolectric (the Compose test never goes idle).
private val TEXT_FIELD_DIALOG = DialogProperties(usePlatformDefaultWidth = false)

/** [TEXT_FIELD_DIALOG] while it ignores taps outside it ([ADD_FORM_TAP_WINDOW_MS]). */
private val TEXT_FIELD_DIALOG_HOLDING_TAPS = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)

/**
 * Swallows touches until [live] is true, before the button under them sees them, so a tap does nothing; keyboard and
 * accessibility clicks do not come through touches and still work.
 */
private fun Modifier.dropTapsUntil(live: State<Boolean>): Modifier = pointerInput(live) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (!live.value) event.changes.forEach { it.consume() }
        }
    }
}

private sealed interface PlanDialog {
    /** The full-screen picker, adding to [day]. */
    data class Add(val day: DayOfWeek) : PlanDialog

    /**
     * The sets/reps form for [entry], just picked for [day]: [draft] holds the ready-made numbers, stored nowhere yet.
     * [saving] is true from Add's tap until the write answers.
     */
    data class AddDetails(val day: DayOfWeek, val entry: PickerEntry, val draft: PlanItem, val saving: Boolean = false) : PlanDialog
    data class Edit(val item: PlanItem) : PlanDialog
    data class Baseline(val lift: Exercise) : PlanDialog

    companion object {
        /**
         * Keeps the add form across a rotation or the process being killed: the day, the picked entry and the draft.
         * The other dialogs are not kept, as before, and neither is an add form whose Add is saving: its item may be
         * stored by the time it is restored, and a second Add would store it again.
         */
        val Saver: Saver<PlanDialog?, Any> = listSaver(
            save = { d ->
                if (d !is PlanDialog.AddDetails || d.saving) {
                    emptyList()
                } else {
                    val entry = with(PickerEntry.Saver) { save(d.entry) } as List<*>
                    val item = d.draft
                    listOf(d.day.name) + entry + listOf(item.exerciseId, item.sets, item.reps, item.targetRpe, item.order, item.mode.name, item.repsHigh ?: -1)
                }
            },
            restore = { v ->
                if (v.isEmpty()) return@listSaver null
                val day = DayOfWeek.valueOf(v[0] as String)
                val entry = PickerEntry.Saver.restore(v.subList(1, 6))!!
                val draft = PlanItem(
                    0, day, v[6] as Long, v[7] as Int, v[8] as Int, v[9] as Double, v[10] as Int,
                    TrainingMode.valueOf(v[11] as String), (v[12] as Int).takeIf { it >= 0 },
                )
                AddDetails(day, entry, draft)
            },
        )
    }
}

@Composable
fun PlanScreen(
    training: TrainingRepository,
    settings: SettingsRepository,
    onOpenSettings: () -> Unit,
    today: () -> LocalDate = ::systemToday,
    viewModel: PlanViewModel = viewModel(factory = PlanViewModel.factory(training, settings, today)),
    /** Opens the plan builder: offered on the bodybuilding side only. */
    onOpenBuilder: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by rememberSaveable(stateSaver = PlanDialog.Saver) { mutableStateOf(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            // A failed save is said, as the Exercises tab says one: the newest message replaces the one on screen.
            snackbar.currentSnackbarData?.dismiss()
            launch { snackbar.showSnackbar(message) }
        }
    }

    PlanContent(
        state = state,
        onOpenSettings = onOpenSettings,
        onAdd = { day -> dialog = PlanDialog.Add(day) },
        onEdit = { item -> dialog = PlanDialog.Edit(item) },
        onMove = viewModel::move,
        onDelete = viewModel::delete,
        onSetBaseline = { lift -> dialog = PlanDialog.Baseline(lift) },
        onBuild = onOpenBuilder,
        snackbar = snackbar,
    )

    // Adding an exercise to a day ticks once it is stored; changing one in the edit dialog does not.
    val haptics = rememberHaptics()
    when (val d = dialog) {
        // Picking opens the sets/reps form with the ready-made numbers; nothing is stored until its Add. An exercise
        // the lifter creates here is saved at once, as before, and then opens the same form. The form opens under the
        // finger, so for a moment it ignores taps on Add, Cancel and outside it (ADD_FORM_TAP_WINDOW_MS).
        is PlanDialog.Add -> {
            var createFailures by remember(d) { mutableIntStateOf(0) }
            ExercisePickerScreen(
                title = "Add to ${dayName(d.day)}",
                exercises = state.exercises,
                recentIds = state.recentIds,
                onPick = { entry -> dialog = PlanDialog.AddDetails(d.day, entry, viewModel.draft(d.day, entry)) },
                // The picker and its create form stay until the exercise is made, and the add form then takes their
                // place. If making it fails, the create form is still there with what was typed, and says so.
                onCreateCustom = { name, category, parent, muscles ->
                    viewModel.addCustomExercise(name, category, parent, muscles, onFailed = { createFailures++ }) { entry ->
                        dialog = PlanDialog.AddDetails(d.day, entry, viewModel.draft(d.day, entry))
                    }
                },
                onDismiss = { dialog = null },
                createFailures = createFailures,
            )
        }
        is PlanDialog.AddDetails -> {
            // Failed Adds: the form says each one over itself, since Plan's snackbar is under its scrim.
            var addFailures by remember { mutableIntStateOf(0) }
            PlanItemDialog(
                title = "Add to ${dayName(d.day)}",
                state = state,
                item = d.draft,
                exerciseName = d.entry.name,
                confirmText = "Add",
                ignoreTapsForMillis = ADD_FORM_TAP_WINDOW_MS,
                onCreateCustom = viewModel::addCustomExercise,
                onSave = { exercise, sets, reps, rpe, repsHigh ->
                    // The form stays open until the item is stored, then closes and ticks. If the write fails, the
                    // form stays as it is, with its numbers, the message is said, and Add works again. Read at the
                    // tap: a second tap meanwhile (in the same frame too) stores nothing more.
                    val open = dialog
                    if (open is PlanDialog.AddDetails && !open.saving) {
                        val saving = open.copy(saving = true)
                        dialog = saving
                        viewModel.add(
                            open.day, exercise ?: open.entry, sets, reps, rpe, repsHigh,
                            onAdded = {
                                if (dialog == saving) dialog = null
                                haptics.tick()
                            },
                            onFailed = {
                                if (dialog == saving) dialog = open
                                addFailures++
                            },
                        )
                    }
                },
                onDismiss = { dialog = null },
                saveFailures = addFailures,
            )
        }
        is PlanDialog.Edit -> PlanItemDialog(
            title = "Edit ${dayName(d.item.weekday)}",
            state = state,
            item = d.item,
            confirmText = "Save",
            onCreateCustom = viewModel::addCustomExercise,
            onSave = { exercise, sets, reps, rpe, repsHigh ->
                viewModel.save(d.item, exercise, sets, reps, rpe, repsHigh)
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
    /** The bodybuilding side's plan builder: the empty plan's button and the menu's "Rebuild my plan". */
    onBuild: () -> Unit = {},
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scheme = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = scheme.background,
        topBar = {
            ScreenHeader("Plan", eyebrow = "Weekly template", modeLabel = true) {
                PtIconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = scheme.onSurfaceVariant)
                }
                // The bodybuilding side's menu; an empty plan has its own button instead.
                if (state.mode == TrainingMode.BODYBUILDING && !state.emptySide) {
                    Box {
                        PtIconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = PLAN_OPTIONS, tint = scheme.onSurfaceVariant)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            PtDropdownMenuItem(
                                text = { Text(REBUILD_MY_PLAN) },
                                leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                                onClick = {
                                    menuOpen = false
                                    onBuild()
                                },
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxWidth(),
            contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, top = Space.s, bottom = Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            if (state.emptySide) {
                item(key = "empty-side") {
                    Column(Modifier.padding(horizontal = Space.xs), verticalArrangement = Arrangement.spacedBy(Space.m)) {
                        Text(EMPTY_BODYBUILDING_PLAN, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                        PrimaryButton(BUILD_MY_PLAN, onBuild, Modifier.testTag("build-my-plan"))
                    }
                }
            }
            items(state.sections, key = { it.day }) { section ->
                DayCard(section, onAdd = { onAdd(section.day) }, onEdit = onEdit, onMove = onMove, onDelete = onDelete)
            }
            // Known bests are the powerlifting side's baselines for squat, bench and deadlift.
            if (state.mode == TrainingMode.POWERLIFTING) item(key = "bests") {
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
                            SoftDivider()
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
        // With motion on, a new row slides in, a deleted one slides out, and Move up and Move down slide two rows past
        // each other.
        SlidingColumnItems(rows, key = { it.item.id }, reorder = true) { i, row ->
            SoftDivider(Modifier.padding(end = Space.s))
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
        append("${item.sets} × ${repsText(item.reps, item.repsHigh)}")
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
        PtIconButton(onClick = { onMove(-1) }, enabled = canMoveUp) { Icon(Icons.Filled.KeyboardArrowUp, "Move up") }
        PtIconButton(onClick = { onMove(1) }, enabled = canMoveDown) { Icon(Icons.Filled.KeyboardArrowDown, "Move down") }
        Box {
            PtIconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "More") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                PtDropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Filled.Edit, null) },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    },
                )
                PtDropdownMenuItem(
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

/**
 * Sets, reps and RPE of a plan item, and its exercise, which "Change" swaps through the picker. Edit opens it on a
 * stored row ("Save"); Add opens it on a draft that is stored only by [confirmText] ("Add").
 */
@Composable
private fun PlanItemDialog(
    title: String,
    state: PlanUiState,
    item: PlanItem,
    confirmText: String,
    onCreateCustom: (String, Category, MainLift?, List<String>, onFailed: () -> Unit, (PickerEntry) -> Unit) -> Unit,
    onSave: (exercise: PickerEntry?, sets: Int, reps: Int, rpe: Double, repsHigh: Int?) -> Unit,
    onDismiss: () -> Unit,
    /** The exercise's name when it may have no stored row yet: the entry picked for the add form. */
    exerciseName: String? = null,
    /**
     * For this long after it opens, taps on the confirm button, on Cancel and outside the dialog do nothing: the add
     * form opens under the finger that picked, where a double tap's second tap lands. Keyboard, back and accessibility
     * actions are not held; neither are the steppers and Change. 0 (Edit) holds nothing.
     */
    ignoreTapsForMillis: Long = 0L,
    /** How many of [onSave]'s writes have failed while the form stays open (Add): each one is said over the form. */
    saveFailures: Int = 0,
) {
    // Null until the exercise is changed; saved with the item, not before.
    var changed by rememberSaveable(stateSaver = PickerEntry.Saver) { mutableStateOf(null) }
    var sets by rememberSaveable { mutableStateOf(item.sets) }
    var reps by rememberSaveable { mutableStateOf(item.reps) }
    // A range on the bodybuilding side; null for fixed reps.
    var repsHigh by rememberSaveable { mutableStateOf(item.repsHigh ?: if (item.mode == TrainingMode.BODYBUILDING) item.reps else null) }
    var rpe by rememberSaveable { mutableStateOf(item.targetRpe) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var createFailures by remember { mutableIntStateOf(0) }
    // Creates that failed once their picker had been closed: back here, under this form's scrim, Plan's snackbar is
    // hidden, so the form says them.
    var formCreateFailures by remember { mutableIntStateOf(0) }
    val saveFailuresBefore = remember { saveFailures }
    val name = changed?.name ?: exerciseName ?: state.exercises.firstOrNull { it.id == item.exerciseId }?.name ?: "Unknown exercise"
    // Not kept across recreation: a restored form holds taps for a moment again, which does no harm.
    val tapsLive = remember { mutableStateOf(ignoreTapsForMillis <= 0L) }
    if (!tapsLive.value) {
        LaunchedEffect(Unit) {
            delay(ignoreTapsForMillis)
            tapsLive.value = true
        }
    }
    val heldTaps = if (ignoreTapsForMillis > 0L) Modifier.dropTapsUntil(tapsLive) else Modifier

    AlertDialog(
        onDismissRequest = onDismiss,
        // A tap outside is the only way here that can't be told apart from back by its callback, so it is held by the
        // window itself: back still closes the form at once.
        properties = if (tapsLive.value) TEXT_FIELD_DIALOG else TEXT_FIELD_DIALOG_HOLDING_TAPS,
        modifier = Modifier.padding(horizontal = 16.dp),
        title = { Text(title) },
        text = {
            FormMessage(SAVE_FAILED, showId = (saveFailures - saveFailuresBefore).coerceAtLeast(0) + formCreateFailures)
            PlanItemFields(
                name, sets, reps, repsHigh, rpe,
                onSets = { sets = it }, onReps = { reps = it }, onRepsHigh = { repsHigh = it }, onRpe = { rpe = it },
                onChange = { picking = true },
            )
        },
        confirmButton = { PrimaryButton(confirmText, { onSave(changed, sets, reps, rpe, repsHigh) }, heldTaps, compact = true) },
        dismissButton = { QuietButton("Cancel", onDismiss, heldTaps) },
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
            // The picker stays until the exercise is made; if making it fails, its create form is still there.
            onCreateCustom = { customName, category, parent, muscles ->
                onCreateCustom(customName, category, parent, muscles, {
                    createFailures++
                    if (!picking) formCreateFailures++
                }) { entry ->
                    changed = entry
                    picking = false
                }
            },
            onDismiss = { picking = false },
            createFailures = createFailures,
        )
    }
}

/**
 * The body of [PlanItemDialog]: the exercise with its "Change" button, then Sets, Reps (or Reps from and Reps to for a
 * range, when [repsHigh] is set) and RPE, within Edit's limits. Internal so the mockups can draw it outside its window.
 */
@Composable
internal fun PlanItemFields(
    name: String,
    sets: Int,
    reps: Int,
    repsHigh: Int?,
    rpe: Double,
    onSets: (Int) -> Unit,
    onReps: (Int) -> Unit,
    onRepsHigh: (Int) -> Unit,
    onRpe: (Double) -> Unit,
    onChange: () -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(bottom = Space.s), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Exercise")
                Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            SecondaryButton("Change", onChange, compact = true, modifier = Modifier.semantics { contentDescription = "Change exercise" })
        }
        LabeledStepper("Sets", sets.toString(), sets > 1, sets < 10, { onSets(sets - 1) }, { onSets(sets + 1) })
        if (repsHigh == null) {
            LabeledStepper("Reps", reps.toString(), reps > 1, reps < MAX_PLAN_REPS, { onReps(reps - 1) }, { onReps(reps + 1) })
        } else {
            // Two ends of the range; the top never goes below the bottom.
            LabeledStepper("Reps from", reps.toString(), reps > 1, reps < repsHigh, { onReps(reps - 1) }, { onReps(reps + 1) })
            LabeledStepper("Reps to", repsHigh.toString(), repsHigh > reps, repsHigh < MAX_RANGE_REPS, { onRepsHigh(repsHigh - 1) }, { onRepsHigh(repsHigh + 1) })
        }
        LabeledStepper("RPE", formatRpe(rpe), rpe > PlanViewModel.MIN_RPE, rpe < PlanViewModel.MAX_RPE, { onRpe(rpe - 0.5) }, { onRpe(rpe + 0.5) }) { RpeInfoButton() }
    }
}

@Composable
private fun LabeledStepper(
    label: String,
    value: String,
    canDec: Boolean,
    canInc: Boolean,
    onDec: () -> Unit,
    onInc: () -> Unit,
    labelAction: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = Space.xs).testTag("plan-field-$label"), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        labelAction?.invoke()
        Spacer(Modifier.weight(1f))
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
