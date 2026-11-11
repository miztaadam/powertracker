package com.powertracker.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powertracker.data.AdviceRepository
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.platform.systemToday
import com.powertracker.ui.components.EmptyState
import com.powertracker.ui.components.RestTimerBar
import com.powertracker.ui.components.ScreenHeader
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.library.ExercisePickerScreen
import com.powertracker.ui.theme.Space
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaDayOfWeek
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** The rest countdown to dock above the bottom bar; supplied by whoever runs the timer. */
data class RestTimerUi(
    val remainingSec: Int,
    val totalSec: Int,
    val onSubtractThirty: () -> Unit,
    val onAddThirty: () -> Unit,
    val onSkip: () -> Unit,
)

/** Everything the Today screen can ask for. Defaults do nothing, for previews and mockups. */
class TodayActions(
    val onStartSession: (bodyweightKg: Double?, readiness: Int?) -> Unit = { _, _ -> },
    val onEdit: (exerciseId: Long, SetEditor) -> Unit = { _, _ -> },
    /** Log the editor's numbers for this exercise, as done or as a missed rep. */
    val onLogSet: (TodayItem, failed: Boolean) -> Unit = { _, _ -> },
    val onConfirmPending: () -> Unit = {},
    val onEditPending: () -> Unit = {},
    val onMissPoint: (setId: Long, MissPoint?) -> Unit = { _, _ -> },
    val onDeleteSet: (setId: Long) -> Unit = {},
    val onApply: (adviceId: Long) -> Unit = {},
    val onDismiss: (adviceId: Long) -> Unit = {},
    val onAddExercise: (exerciseId: Long) -> Unit = {},
    /** Adds a catalogue exercise by name; returns the id its card is shown under. */
    val onAddFromCatalogue: (name: String) -> Long = { 0 },
    val onCreateExercise: (name: String, Category, parentLift: MainLift?, muscles: List<String>, onCreated: (Long) -> Unit) -> Unit = { _, _, _, _, _ -> },
    val onRemoveExercise: (exerciseId: Long) -> Unit = {},
    val onFinish: () -> Unit = {},
    val onKeepLogging: () -> Unit = {},
    val onOpenCoach: () -> Unit = {},
)

/**
 * The logging screen. [onSetLogged] fires after each saved set (true for a main lift or variation) so a
 * rest timer can start; [restTimer] is drawn above the bottom bar while one is running.
 */
@Composable
fun TodayScreen(
    training: TrainingRepository,
    advice: AdviceRepository,
    settings: SettingsRepository,
    onOpenCoach: () -> Unit,
    today: () -> LocalDate = ::systemToday,
    onSetLogged: (isMainOrVariation: Boolean) -> Unit = {},
    restTimer: RestTimerUi? = null,
) {
    // One view model per calendar day: coming back to the app the next morning starts a fresh day.
    var date by remember { mutableStateOf(today()) }
    LifecycleResumeEffect(Unit) {
        date = today()
        onPauseOrDispose {}
    }
    // The view model is keyed by the day it was opened for, but gets the real clock: a view model
    // left over from an earlier day must see that the day has changed and stop writing.
    val viewModel: TodayViewModel = viewModel(key = "today-$date", factory = TodayViewModel.factory(training, advice, settings, today))
    val state by viewModel.state.collectAsStateWithLifecycle()

    val setLogged by rememberUpdatedState(onSetLogged)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TodayEvent.SetLogged -> setLogged(event.isMainOrVariation)
            }
        }
    }

    val openCoach by rememberUpdatedState(onOpenCoach)
    val actions = remember(viewModel) {
        TodayActions(
            onStartSession = viewModel::startSession,
            onEdit = viewModel::edit,
            onLogSet = { item, failed ->
                val weight = item.editor.weightKg
                if (weight != null) {
                    val set = LoggedSet(item.exercise.id, weight, item.editor.reps, item.editor.rpe, failed = failed, missPoint = null, isTopSet = false)
                    viewModel.logSet(item.planned?.id, item.exercise.id, set)
                }
            },
            onConfirmPending = { viewModel.state.value.pending?.let { viewModel.logSet(it.planItemId, it.exerciseId, it.set, confirmed = true) } },
            onEditPending = viewModel::dismissWarning,
            onMissPoint = viewModel::setMissPoint,
            onDeleteSet = viewModel::deleteSet,
            onApply = viewModel::applyTarget,
            onDismiss = viewModel::dismissTarget,
            onAddExercise = viewModel::addExercise,
            onAddFromCatalogue = viewModel::addFromCatalogue,
            onCreateExercise = viewModel::createExercise,
            onRemoveExercise = viewModel::removeExercise,
            onFinish = viewModel::finishSession,
            onKeepLogging = viewModel::dismissSummary,
            onOpenCoach = { openCoach() },
        )
    }
    TodayContent(state, actions, restTimer)
}

private val HEADER_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM")

/** The Today screen without its view model: start card, the session's exercises, or the finish summary. */
@Composable
fun TodayContent(state: TodayState, actions: TodayActions, restTimer: RestTimerUi? = null) {
    val items = state.items
    val inSession = state.sessionId != null && state.summary == null
    var activeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }

    // A catalogue exercise added without a row changes id when its first set saves the row: follow it.
    val chosenId = activeId?.let { state.resolved[it] ?: it }
    LaunchedEffect(chosenId) { if (chosenId != activeId) activeId = chosenId }

    // The exercise being logged: the one the lifter chose, else the first with sets left to do.
    val active = items.firstOrNull { it.exercise.id == chosenId } ?: items.firstOrNull { it.hasWorkLeft() } ?: items.lastOrNull()
    // Pin it once chosen, so finishing its last set does not whisk the card (and its "where did it stall?") away.
    LaunchedEffect(active?.exercise?.id) { if (activeId == null) activeId = active?.exercise?.id }

    val list = rememberLazyListState()
    val activeIndex = items.indexOf(active)
    LaunchedEffect(activeIndex, inSession) { if (inSession && activeIndex >= 0) list.animateScrollToItem(activeIndex) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenHeader("Today", eyebrow = state.date.toJavaLocalDate().format(HEADER_DATE.withLocale(Locale.getDefault()))) {
                if (inSession) SecondaryButton("Finish", actions.onFinish, compact = true, modifier = Modifier.padding(end = Space.s))
            }
        },
        bottomBar = {
            if (restTimer != null) {
                RestTimerBar(
                    restTimer.remainingSec, restTimer.totalSec, restTimer.onSubtractThirty, restTimer.onAddThirty, restTimer.onSkip,
                    Modifier.padding(horizontal = Space.gutter).padding(top = Space.xs, bottom = Space.s),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            when {
                state.loading -> Unit
                state.summary != null -> item(key = "summary") {
                    SummaryCard(state.summary, onOpenCoach = actions.onOpenCoach, onKeepLogging = actions.onKeepLogging)
                }
                state.sessionId == null -> item(key = "start") {
                    StartSessionCard(items, state.date.dayOfWeek.toJavaDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()), actions.onStartSession)
                }
                else -> {
                    itemsIndexed(items, key = { _, item -> item.exercise.id }) { i, item ->
                        if (item === active) {
                            val upNext = (items.drop(i + 1) + items.take(i)).firstOrNull { it.hasWorkLeft() }
                            val next = if (upNext != null) {
                                NextStep("Next: ${upNext.exercise.name}") { activeId = upNext.exercise.id }
                            } else {
                                NextStep("Finish session", actions.onFinish)
                            }
                            ExerciseCard(item, i + 1, items.size, state.weightStepKg, state.changePlates, next, actions)
                        } else {
                            ExerciseRow(item, onClick = { activeId = item.exercise.id })
                        }
                    }
                    if (items.isEmpty()) {
                        item(key = "empty") {
                            EmptyState(Icons.Filled.FitnessCenter, "No exercises yet", "Add what you are training today and log it set by set.")
                        }
                    }
                    item(key = "add") {
                        SecondaryButton("Add exercise", { picking = true }, Modifier.fillMaxWidth(), icon = Icons.Filled.Add)
                    }
                }
            }
        }
    }

    state.pending?.let { TypoDialog(it, onConfirm = actions.onConfirmPending, onEdit = actions.onEditPending) }

    if (picking) {
        ExercisePickerScreen(
            title = "Add exercise",
            exercises = state.exercises,
            recentIds = state.recentIds,
            // Picking one already in the session just opens its card.
            onPick = { entry ->
                val name = entry.catalogueName
                activeId = if (name != null) actions.onAddFromCatalogue(name) else entry.exerciseId?.also(actions.onAddExercise)
                picking = false
            },
            onCreateCustom = { name, category, parent, muscles ->
                actions.onCreateExercise(name, category, parent, muscles) { id -> activeId = id }
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

/** Planned sets still to do, or an added exercise with nothing logged yet. */
private fun TodayItem.hasWorkLeft(): Boolean = if (targetSets != null) !isComplete else sets.isEmpty()
