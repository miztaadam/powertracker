package com.powertracker.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powertracker.data.AdviceRepository
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.coach.WarmUpSet
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.coach.WelcomeBackCard
import com.powertracker.platform.systemToday
import com.powertracker.ui.components.EmptyState
import com.powertracker.ui.components.RestTimerBar
import com.powertracker.ui.components.ScreenHeader
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.library.ExercisePickerScreen
import com.powertracker.ui.library.HeldFailures
import com.powertracker.ui.library.SAVE_FAILED
import com.powertracker.ui.theme.LocalMotion
import com.powertracker.ui.theme.Space
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaDayOfWeek
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import com.powertracker.ui.components.rememberHaptics

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
    /** Ticks or unticks a warm-up set (1.23). Nothing is logged. */
    val onTickWarmUp: (exerciseId: Long, WarmUpSet, done: Boolean) -> Unit = { _, _, _ -> },
    /** Log the editor's numbers for this exercise, as done or as a missed rep. */
    val onLogSet: (TodayItem, failed: Boolean) -> Unit = { _, _ -> },
    val onConfirmPending: () -> Unit = {},
    val onEditPending: () -> Unit = {},
    val onMissPoint: (setId: Long, MissPoint?) -> Unit = { _, _ -> },
    val onUpdateSet: ((setId: Long, weightKg: Double, reps: Int, rpe: Double?) -> Unit)? = null,
    val onDeleteSet: (setId: Long) -> Unit = {},
    val onApply: (adviceId: Long) -> Unit = {},
    val onDismiss: (adviceId: Long) -> Unit = {},
    val onAddExercise: (exerciseId: Long) -> Unit = {},
    /** Adds a catalogue exercise by name; returns the id its card is shown under. */
    val onAddFromCatalogue: (name: String) -> Long? = { null },
    val onCreateExercise: (name: String, Category, parentLift: MainLift?, muscles: List<String>, onFailed: () -> Unit, onCreated: (Long) -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    val onRemoveExercise: (exerciseId: Long) -> Unit = {},
    /** Skips a planned exercise for today, with the reason picked in the sheet (null for "Just skip"). */
    val onSkip: (exerciseId: Long, planItemId: Long?, reason: SkipReason?) -> Unit = { _, _, _ -> },
    /** Bring back on the "Skipped today" line. */
    val onBringBack: (exerciseId: Long) -> Unit = {},
    val onFinish: () -> Unit = {},
    val onKeepLogging: () -> Unit = {},
    val onOpenCoach: () -> Unit = {},
    /** The welcome-back card's two answers; the second also opens the Plan. */
    val onResumePlan: () -> Unit = {},
    val onGoalsChanged: () -> Unit = {},
    /**
     * Stores an exercise's plate setting from the plate sheet (1.17). Null means "no plate item" in any row menu, which
     * is what the guide's Today, drawn with `TodayActions()`, gets.
     */
    val onCountPlates: ((exerciseId: Long, PlateCount) -> Unit)? = null,
    /** Toggles single-sided ("one side at a time") tracking for an exercise (1.25). Null hides the menu item. */
    val onToggleSingleSided: ((exerciseId: Long, Boolean) -> Unit)? = null,
    /** Logs one half (Right or Left) of a single-sided set (1.25). */
    val onLogSide: (TodayItem, failed: Boolean) -> Unit = { _, _ -> },
    /** Clears a half-logged right side before logging left (1.25). */
    val onClearPendingRight: (exerciseId: Long) -> Unit = {},
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
    onOpenPlan: () -> Unit = {},
) {
    val viewModel = rememberTodayViewModel(training, advice, settings, today)
    val state by viewModel.state.collectAsStateWithLifecycle()

    val setLogged by rememberUpdatedState(onSetLogged)
    // Log set and Missed buzz once the set is saved: one buzz per set, none for an entry that was refused or held.
    // A new best gets the stronger buzz instead of the tick; the view model says which just after the save.
    val haptics = rememberHaptics()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TodayEvent.SetLogged -> setLogged(event.isMainOrVariation)
                is TodayEvent.SetBuzz -> if (event.isNewBest) haptics.best() else haptics.tick()
                // Coach Apply ticks once the target is in the editor.
                TodayEvent.TargetApplied -> haptics.tick()
                // A failed save is said, as the Exercises tab says one: the newest message replaces the one on screen.
                TodayEvent.SaveFailed -> {
                    snackbar.currentSnackbarData?.dismiss()
                    launch { snackbar.showSnackbar(SAVE_FAILED) }
                }
                // A skip ticks once it is stored, and Undo stays on offer for the snackbar's usual time.
                is TodayEvent.Skipped -> {
                    haptics.tick()
                    snackbar.currentSnackbarData?.dismiss()
                    launch {
                        val result = snackbar.showSnackbar(skippedMessage(event.name), actionLabel = UNDO, duration = SnackbarDuration.Short)
                        if (result == SnackbarResult.ActionPerformed) viewModel.undoSkip(event.skipId)
                    }
                }
            }
        }
    }

    val openCoach by rememberUpdatedState(onOpenCoach)
    val openPlan by rememberUpdatedState(onOpenPlan)
    val actions = remember(viewModel) {
        TodayActions(
            onStartSession = viewModel::startSession,
            onEdit = viewModel::edit,
            onTickWarmUp = viewModel::tickWarmUp,
            onLogSet = { item, failed ->
                val weight = item.editor.weightKg
                if (weight != null) {
                    val set = LoggedSet(item.exercise.id, weight, item.editor.reps, item.editor.rpe, failed = failed, missPoint = null, isTopSet = false, unit = item.unit)
                    viewModel.logSet(item.planned?.id, item.exercise.id, set)
                }
            },
            onConfirmPending = { viewModel.state.value.pending?.let { viewModel.logSet(it.planItemId, it.exerciseId, it.set, confirmed = true) } },
            onEditPending = viewModel::dismissWarning,
            onMissPoint = viewModel::setMissPoint,
            onUpdateSet = viewModel::updateSet,
            onDeleteSet = viewModel::deleteSet,
            onApply = viewModel::applyTarget,
            onDismiss = viewModel::dismissTarget,
            onAddExercise = viewModel::addExercise,
            onAddFromCatalogue = viewModel::addFromCatalogue,
            onCreateExercise = viewModel::createExercise,
            onRemoveExercise = viewModel::removeExercise,
            onSkip = viewModel::skip,
            onBringBack = viewModel::bringBack,
            onFinish = viewModel::finishSession,
            onKeepLogging = viewModel::dismissSummary,
            onOpenCoach = { openCoach() },
            onResumePlan = viewModel::answerWelcomeBack,
            onGoalsChanged = {
                viewModel.answerWelcomeBack()
                openPlan()
            },
            onCountPlates = viewModel::setPlateCount,
            onToggleSingleSided = viewModel::setSingleSided,
            onLogSide = { item, failed ->
                val weight = item.editor.weightKg
                if (weight != null) {
                    val side = com.powertracker.domain.model.SideLog(weight, item.editor.reps, item.editor.rpe, failed)
                    viewModel.logSide(item.planned?.id, item.exercise.id, side, item.unit)
                }
            },
            onClearPendingRight = viewModel::clearPendingRight,
        )
    }
    TodayContent(state, actions, restTimer, snackbar)
}

/**
 * Today's view model, kept by [owner]. The Today tab owns it; the Exercises tab passes the Today
 * tab's owner, so both see the one session: what was added to it and whether it was finished.
 */
@Composable
fun rememberTodayViewModel(
    training: TrainingRepository,
    advice: AdviceRepository,
    settings: SettingsRepository,
    today: () -> LocalDate = ::systemToday,
    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) { "No ViewModelStoreOwner for the Today view model" },
): TodayViewModel {
    // One view model per calendar day: coming back to the app the next morning starts a fresh day.
    var date by remember { mutableStateOf(today()) }
    LifecycleResumeEffect(Unit) {
        date = today()
        onPauseOrDispose {}
    }
    // The view model is keyed by the day it was opened for, but gets the real clock: a view model
    // left over from an earlier day must see that the day has changed and stop writing.
    return viewModel(viewModelStoreOwner = owner, key = "today-$date", factory = TodayViewModel.factory(training, advice, settings, today))
}

private val HEADER_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM")

/** The Today screen without its view model: start card, the session's exercises, or the finish summary. */
@Composable
fun TodayContent(
    state: TodayState,
    actions: TodayActions,
    restTimer: RestTimerUi? = null,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    val items = state.items
    val inSession = state.inSession
    var activeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    // The exercise whose reason sheet is open; kept across recreation, so the question is still there.
    var skippingId by rememberSaveable { mutableStateOf<Long?>(null) }
    // The exercise whose plate sheet is open (1.17), the unit it was in when the sheet opened and whether it had sets
    // in that unit: a choice applies when tapped, so the live values change under the open sheet, and the note is about
    // the unit the sheet opened in.
    var platingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var platingFrom by rememberSaveable { mutableStateOf(LoadUnit.KG) }
    var platingHadSets by rememberSaveable { mutableStateOf(false) }
    fun openPlates(item: TodayItem) {
        platingFrom = item.unit
        platingHadSets = item.sets.any { it.set.unit == item.unit } || item.lastWeightKg != null
        platingId = item.exercise.id
    }
    // Offered only where the exercise may count plates and the screen can store the choice: never in the guide.
    fun countPlates(item: TodayItem): (() -> Unit)? = if (item.plateOffered && actions.onCountPlates != null) ({ openPlates(item) }) else null
    // Adding an exercise from the picker ticks, once it is added.
    val haptics = rememberHaptics()
    // Read when called, from the session as last drawn (before the add).
    val shownItems by rememberUpdatedState(items)
    val resolvedIds by rememberUpdatedState(state.resolved)
    fun inSessionAlready(id: Long): Boolean = shownItems.any { it.exercise.id == (resolvedIds[id] ?: id) }

    // A catalogue exercise added without a row changes id when its first set saves the row: follow it.
    val chosenId = activeId?.let { state.resolved[it] ?: it }
    LaunchedEffect(chosenId) { if (chosenId != activeId) activeId = chosenId }

    // The exercise being logged: the one the lifter chose, else the first with sets left to do.
    // In a superset (1.24) the card to open is the exercise whose turn it is, from the sets logged today.
    fun turnOf(item: TodayItem?): TodayItem? {
        val pair = item?.superset ?: return item
        if (pair.due == null || pair.due == pair.half) return item
        return items.firstOrNull { it.exercise.id == pair.partnerId } ?: item
    }
    val active = items.firstOrNull { it.exercise.id == chosenId } ?: turnOf(items.firstOrNull { it.hasWorkLeft() }) ?: items.lastOrNull()
    // Pin it once chosen, so finishing its last set does not whisk the card (and its "where did it stall?") away.
    LaunchedEffect(active?.exercise?.id) { if (activeId == null) activeId = active?.exercise?.id }

    // A superset's set was logged and it is the other exercise's turn (1.24): its card opens. A request made before this
    // screen was drawn (another tab meanwhile, or a turn of the phone) is not followed again. Not saved: the view
    // model's count starts afresh with the app, and a saved one could match a new request and swallow it.
    val openRequest = state.openRequest
    var followed by remember { mutableIntStateOf(openRequest?.n ?: 0) }
    LaunchedEffect(openRequest?.n) {
        if (openRequest != null && openRequest.n != followed) {
            followed = openRequest.n
            activeId = openRequest.exerciseId
        }
    }

    // With motion on, a catalogue exercise keeps its stand-in's key once its row is saved, so its card stays put
    // instead of fading out and in again. With motion off the key is the id, as before.
    val motion = LocalMotion.current
    val standInOf = if (motion) state.resolved.entries.associate { (standIn, id) -> id to standIn } else emptyMap()
    fun itemKey(id: Long): Long = standInOf[id] ?: id

    // The "Skipped today" line rises in when it comes with a skip made now (1.16): not when the screen opens with it
    // there (or is restored), nor on a side switch or another day. A second skip while it shows leaves it where it is.
    val line = remember { LineMemory() }
    val lineKey = state.mode to state.date
    val hasLine = state.skippedToday.isNotEmpty()
    // A skip made now on this side: an exercise on this side's list, as last drawn, has moved to the line. A side's
    // state that comes in pieces (its side first, its own list and line after) has a line no exercise moved to.
    val skippedNow = line.key == lineKey && state.skippedToday.any { it.exerciseId in line.listed }
    // True only in the draw the skip arrives in: the line rises if it is drawn then, never when first scrolled to later
    // or scrolled back.
    val arriving = motion && !state.loading && skippedNow && !line.had
    if (arriving) {
        line.arriving = true
        line.arrivals++
    }
    // The line, if it is on screen, is drawn in this frame's layout; by the next frame the arrival is over.
    LaunchedEffect(line.arrivals) {
        withFrameNanos {}
        line.arriving = false
    }
    SideEffect {
        if (!state.loading) {
            line.key = lineKey
            line.had = hasLine
            line.listed = items.mapTo(HashSet()) { it.exercise.id }
        }
    }
    val skippedLine = @Composable {
        // Whether it came with the skip, as this line was first drawn; kept while it is drawn, gone when it is not.
        var rising by remember { mutableStateOf(line.arriving) }
        SkippedTodayLine(state.skippedToday, actions.onBringBack, if (rising) risingIn { rising = false } else Modifier)
    }

    val list = rememberLazyListState()
    // The note and the welcome-back card, when shown, come before the exercises: the first exercise keeps them in view.
    val leading = (if (state.sideNote != null) 1 else 0) + (if (state.welcomeBack) 1 else 0)
    val activeIndex = items.indexOf(active)
    LaunchedEffect(activeIndex, inSession) {
        if (inSession && activeIndex >= 0) list.animateScrollToItem(if (activeIndex == 0) 0 else activeIndex + leading)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenHeader("Today", eyebrow = state.date.toJavaLocalDate().format(HEADER_DATE.withLocale(Locale.getDefault())), modeLabel = true) {
                if (inSession) SecondaryButton("Finish", actions.onFinish, compact = true, modifier = Modifier.padding(end = Space.s))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
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
            if (!state.loading) {
                state.sideNote?.let { note ->
                    item(key = "side-note") {
                        Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Space.xs))
                    }
                }
                if (state.welcomeBack) {
                    item(key = "welcome-back") { WelcomeBackCard(onResume = actions.onResumePlan, onGoalsChanged = actions.onGoalsChanged) }
                }
            }
            when {
                state.loading -> Unit
                state.summary != null -> item(key = "summary") {
                    SummaryCard(state.summary, onOpenCoach = actions.onOpenCoach, onKeepLogging = actions.onKeepLogging)
                }
                state.sessionId == null -> {
                    item(key = "start") {
                        StartSessionCard(
                            items, state.date.dayOfWeek.toJavaDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()), actions.onStartSession,
                            onSkipToday = { skippingId = it.exercise.id },
                            skipped = state.skippedToday.size,
                            onCountPlates = if (actions.onCountPlates != null) ::openPlates else null,
                        )
                    }
                    if (state.skippedToday.isNotEmpty()) {
                        item(key = "skipped-today") { skippedLine() }
                    }
                }
                else -> {
                    val plannedTotal = items.sumOf { it.targetSets ?: 0 }
                    val doneSets = items.sumOf { it.sets.size }
                    if (plannedTotal > 0) {
                        item(key = "session-progress") {
                            val pt = com.powertracker.ui.theme.PowertrackerTheme.colors
                            val complete = doneSets >= plannedTotal
                            val fraction = (doneSets.toFloat() / plannedTotal).coerceIn(0f, 1f)
                            androidx.compose.foundation.layout.Column(
                                Modifier.fillMaxWidth().padding(horizontal = Space.xs),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        if (complete) "WORKOUT COMPLETE" else "SESSION PROGRESS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = pt.muted,
                                    )
                                    Text(
                                        "$doneSets / $plannedTotal sets",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainerHighest,
                                            androidx.compose.foundation.shape.CircleShape,
                                        ),
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(fraction)
                                            .height(5.dp)
                                            .background(
                                                if (complete) pt.success else pt.accentText,
                                                androidx.compose.foundation.shape.CircleShape,
                                            ),
                                    )
                                }
                            }
                        }
                    }
                    // The list's items in order, for sliding them only when one is added, removed or moved.
                    val keys: List<Any> = if (motion) items.map { itemKey(it.exercise.id) } + (if (items.isEmpty()) listOf("empty") else emptyList()) + "add" else emptyList()
                    itemsIndexed(items, key = { _, item -> itemKey(item.exercise.id) }) { i, item ->
                        // With motion on, opening another exercise slides the closing card down to its row and the
                        // opening one up from its row, in place; the rows below follow.
                        val exercise = @Composable {
                            SlidingSwap(
                                opened = item === active,
                                row = { rowModifier ->
                                    val toggleSingleSided: (() -> Unit)? = if (item.singleSidedOffered && actions.onToggleSingleSided != null) {
                                        { actions.onToggleSingleSided.invoke(item.exercise.id, !item.singleSided) }
                                    } else {
                                        null
                                    }
                                    ExerciseRow(
                                        item, onClick = { activeId = item.exercise.id }, modifier = rowModifier,
                                        onSkipToday = if (item.canSkip) ({ skippingId = item.exercise.id }) else null,
                                        onCountPlates = countPlates(item),
                                        onToggleSingleSided = toggleSingleSided,
                                    )
                                },
                                card = { content ->
                                    val upNext = turnOf((items.drop(i + 1) + items.take(i)).firstOrNull { it.hasWorkLeft() })
                                    val next = if (upNext != null) {
                                        NextStep("Next: ${upNext.exercise.name}") { activeId = upNext.exercise.id }
                                    } else {
                                        NextStep("Finish session", actions.onFinish)
                                    }
                                    val toggleSingleSided: (() -> Unit)? = if (item.singleSidedOffered && actions.onToggleSingleSided != null) {
                                        { actions.onToggleSingleSided.invoke(item.exercise.id, !item.singleSided) }
                                    } else {
                                        null
                                    }
                                    ExerciseCard(
                                        item, i + 1, items.size, state.weightStepKg, state.changePlates, next, actions,
                                        bodybuilding = state.mode == TrainingMode.BODYBUILDING,
                                        today = state.date,
                                        slidingContent = content,
                                        onSkipToday = if (item.canSkip) ({ skippingId = item.exercise.id }) else null,
                                        onCountPlates = countPlates(item),
                                        onToggleSingleSided = toggleSingleSided,
                                        prSetIds = state.prSetIds,
                                    )
                                },
                            )
                        }
                        if (motion) Box(slidingListItem(keys).testTag("exercise-${item.exercise.id}")) { exercise() } else exercise()
                    }
                    if (items.isEmpty()) {
                        item(key = "empty") {
                            EmptyState(Icons.Filled.FitnessCenter, "No exercises yet", "Add what you are training today and log it set by set.", slidingListItem(keys))
                        }
                    }
                    item(key = "add") {
                        SecondaryButton("Add exercise", { picking = true }, slidingListItem(keys).fillMaxWidth(), icon = Icons.Filled.Add)
                    }
                    if (state.skippedToday.isNotEmpty()) {
                        item(key = "skipped-today") { skippedLine() }
                    }
                }
            }
        }
    }

    state.pending?.let { TypoDialog(it, onConfirm = actions.onConfirmPending, onEdit = actions.onEditPending) }

    // The reason sheet: an answer skips the exercise; back or a tap outside cancels and stores nothing. It closes by
    // itself if the row can no longer be skipped (a set was logged meanwhile, or it left the plan).
    val skipping = skippingId?.let { id -> items.firstOrNull { it.exercise.id == id } }
    if (skippingId != null && !state.loading && (skipping == null || !skipping.canSkip)) {
        LaunchedEffect(skippingId) { skippingId = null }
    }
    if (skipping != null && skipping.canSkip) {
        SkipReasonSheet(
            skipping.exercise.name,
            onReason = { reason ->
                skippingId = null
                actions.onSkip(skipping.exercise.id, skipping.planned?.id, reason)
            },
            onDismiss = { skippingId = null },
        )
    }

    // The plate sheet follows a catalogue exercise across to its row, which choosing plates may have just made.
    val platingShown = platingId?.let { state.resolved[it] ?: it }
    LaunchedEffect(platingShown) { if (platingShown != platingId) platingId = platingShown }
    val plating = platingShown?.let { id -> items.firstOrNull { it.exercise.id == id } }
    val onCountPlates = actions.onCountPlates
    if (platingId != null && !state.loading && (plating == null || !plating.plateOffered || onCountPlates == null)) {
        // Gone from the list (removed, or the day moved on): nothing to set.
        LaunchedEffect(platingId) { platingId = null }
    }
    if (plating != null && plating.plateOffered && onCountPlates != null) {
        PlateCountSheet(
            plating.exercise.name,
            plating.plateCount,
            plateNote(platingFrom, plating.unit, platingHadSets),
            onChoose = { onCountPlates(plating.exercise.id, it) },
            onDismiss = { platingId = null },
            saving = state.plateSaving,
        )
    }

    if (picking) {
        // Failed creates from the picker's "Create your own": its form stays open with what was typed, and says so. One
        // per opening: a failure that comes once the picker has gone is said by Today's own snackbar alone.
        val createFailures = remember { HeldFailures() }
        ExercisePickerScreen(
            title = "Add exercise",
            exercises = state.exercises,
            recentIds = state.recentIds,
            // Picking one already in the session just opens its card, with no buzz; a new one ticks once it is added.
            onPick = { entry ->
                val name = entry.catalogueName
                val id = if (name != null) actions.onAddFromCatalogue(name) else entry.exerciseId?.also(actions.onAddExercise)
                if (id != null && !inSessionAlready(id)) haptics.tick()
                activeId = id
                picking = false
            },
            // The picker stays until the exercise is made; if making it fails, its create form is still there.
            onCreateCustom = { name, category, parent, muscles ->
                actions.onCreateExercise(name, category, parent, muscles, createFailures::add) { id ->
                    if (!inSessionAlready(id)) haptics.tick()
                    activeId = id
                    picking = false
                }
            },
            onDismiss = { picking = false },
            createFailures = createFailures,
        )
    }
}

/** What Today knows of the "Skipped today" line between draws, to tell a skip made now from one already there. */
private class LineMemory {
    /** The side and day last drawn loaded: null until then, so what the screen opens with is simply there. */
    var key: Any? = null
    var had = false
    /** The exercises on the list as last drawn loaded. */
    var listed: Set<Long> = emptySet()
    var arriving = false
    var arrivals = 0
}

/** The skipped line rising [riseIn] as it fades in, over the last of its time; [onOver] when done. */
@Composable
private fun risingIn(onOver: () -> Unit): Modifier {
    val progress = eventProgress(EVENT_MOVE_MS, onOver) ?: return Modifier
    val rise = with(LocalDensity.current) { RISE.toPx() }
    return Modifier
        .graphicsLayer {
            val f = riseFade(progress())
            alpha = f
            translationY = rise * (1f - f)
        }
        .semantics { riseIn = progress }
}

/** How far the "Skipped today" line rises as it fades in. */
private val RISE = 14.dp

/** Planned sets still to do, or an added exercise with nothing logged yet. */
private fun TodayItem.hasWorkLeft(): Boolean = if (targetSets != null) !isComplete else sets.isEmpty()
