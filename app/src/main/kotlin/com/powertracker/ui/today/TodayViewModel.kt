package com.powertracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.StoredSet
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.coach.Coach
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.plates.PlateLoader
import com.powertracker.domain.validation.InputValidator
import com.powertracker.domain.validation.InputWarning
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

/** The numbers in the steppers for the next set. [weightKg] is null until the lifter enters one. */
data class SetEditor(val weightKg: Double?, val reps: Int, val rpe: Double)

data class TodayItem(
    val exercise: Exercise,
    /** Today's plan item, or null for an exercise added during the session. */
    val planned: PlanItem?,
    /** The coach's OPEN target for today, if any. Changes nothing until applied. */
    val suggestion: Advice?,
    val sets: List<StoredSet>,
    val editor: SetEditor,
    /** Sets to do: the plan's, or an applied coach target's. Null for an added exercise. */
    val targetSets: Int?,
    /** Plates for the editor weight; null when there is no weight or it cannot be loaded exactly. */
    val perSide: List<Double>?,
    /** The weight a typo is judged against: the last set today, else the last top set before today. */
    val lastWeightKg: Double?,
) {
    val logged: List<LoggedSet> get() = sets.map { it.set }
    val isComplete: Boolean get() = targetSets != null && sets.size >= targetSets
}

/** A set held back because it looks like a typo; saved only after the lifter confirms. */
data class PendingLog(val planItemId: Long?, val exerciseId: Long, val set: LoggedSet, val warnings: List<InputWarning>)

data class FinishSummary(val setCount: Int, val exerciseCount: Int, val newNotes: Int)

data class TodayState(
    val date: LocalDate,
    val loading: Boolean = true,
    val sessionId: Long? = null,
    val items: List<TodayItem> = emptyList(),
    val pending: PendingLog? = null,
    val summary: FinishSummary? = null,
    /** Exercises that can still be added to this session. */
    val addable: List<Exercise> = emptyList(),
    val weightStepKg: Double = 2.5,
    val changePlates: Boolean = false,
) {
    val pendingWarning: InputWarning? get() = pending?.warnings?.firstOrNull()
}

sealed interface TodayEvent {
    /** A set was saved; the rest timer starts from this. */
    data class SetLogged(val isMainOrVariation: Boolean) : TodayEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val training: TrainingRepository,
    private val advice: AdviceRepository,
    private val settings: SettingsRepository,
    today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val date: LocalDate = today()

    /** What only this screen knows: edited numbers, exercises added but not yet logged, open prompts. */
    private data class Local(
        val editors: Map<Long, SetEditor> = emptyMap(),
        val added: List<Long> = emptyList(),
        val pending: PendingLog? = null,
        val summary: FinishSummary? = null,
    )

    private data class Stored(
        val sessionId: Long?,
        val sets: List<StoredSet>,
        val history: TrainingHistory,
        val plan: List<PlanItem>,
        val settings: AppSettings,
        val targets: List<Advice>,
    )

    private val local = MutableStateFlow(Local())
    private val starting = MutableStateFlow(false)

    /** True while a set is being saved; further log taps are ignored until the screen shows it. */
    private val saving = MutableStateFlow(false)

    private val _coachPasses = MutableStateFlow(0)

    /** How many times today's targets have been worked out and stored (once on open, again when the plan changes). */
    val coachPasses: StateFlow<Int> = _coachPasses

    private val _events = MutableSharedFlow<TodayEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<TodayEvent> = _events

    private val session: Flow<Pair<Long?, List<StoredSet>>> =
        training.sessionOn(date).map { it?.id }.distinctUntilChanged().flatMapLatest { id ->
            if (id == null) flowOf(null to emptyList()) else training.setsForSession(id).map { id to it }
        }

    /** Today's TARGET advice in every status; re-read whenever the advice table changes. */
    private val targets: Flow<List<Advice>> = advice.open().map { todaysTargets(advice.all()) }

    private val stored: Flow<Stored> =
        combine(session, training.history(), training.plan(), settings.settings, targets) { session, history, plan, s, targets ->
            Stored(session.first, session.second, history, plan, s, targets)
        }

    val state: StateFlow<TodayState> = combine(stored, local, ::build)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayState(date))

    init {
        // Ask the coach for today's targets when the screen opens and again if the plan changes.
        viewModelScope.launch {
            combine(training.plan(), settings.settings) { plan, s -> plan to s.coach() }
                .distinctUntilChanged()
                .collect { (plan, coach) ->
                    val prior = advice.all()
                    val known = todaysTargets(prior).map { it.exerciseId() }.toSet()
                    val fresh = Coach.targetsFor(date.dayOfWeek, training.history().first(), plan, coach, date, prior)
                        .filter { it.exerciseId() !in known }
                        .distinctBy { it.exerciseId() }
                    if (fresh.isNotEmpty()) advice.insert(fresh)
                    _coachPasses.update { it + 1 }
                }
        }
    }

    private fun todaysTargets(all: List<Advice>) =
        all.filter { it.type == AdviceType.TARGET && it.createdAt == date && it.payload is AdvicePayload.Target }

    private fun build(d: Stored, l: Local): TodayState {
        val stats = HistoryStats(d.history)
        // One card per exercise: sets are stored per exercise, so a second plan line for the same one merges into the first.
        val planned = d.plan.filter { it.weekday == date.dayOfWeek }.sortedBy { it.order }.distinctBy { it.exerciseId }
        val plannedIds = planned.map { it.exerciseId }.toSet()
        val loggedIds = d.sets.map { it.set.exerciseId }.distinct()
        val addedIds = (loggedIds.filter { it !in l.added } + l.added).filter { it !in plannedIds }.distinct()

        fun item(exercise: Exercise, plan: PlanItem?): TodayItem {
            val sets = d.sets.filter { it.set.exerciseId == exercise.id }
            val mine = d.targets.filter { it.exerciseId() == exercise.id }
            val applied = mine.lastOrNull { it.status == AdviceStatus.APPLIED }?.payload as? AdvicePayload.Target
            val top = stats.lastTopSet(exercise.id, date)?.second
            val lastToday = sets.lastOrNull()?.set
            val rpe = plan?.targetRpe ?: d.settings.defaultRpe
            val editor = l.editors[exercise.id] ?: when {
                lastToday != null -> SetEditor(lastToday.weightKg, lastToday.reps, lastToday.rpe ?: rpe)
                applied != null -> SetEditor(applied.weightKg, applied.reps, applied.rpe)
                else -> SetEditor(top?.weightKg, plan?.reps ?: top?.reps ?: DEFAULT_REPS, rpe)
            }
            return TodayItem(
                exercise = exercise,
                planned = plan,
                suggestion = mine.firstOrNull { it.status == AdviceStatus.OPEN },
                sets = sets,
                editor = editor,
                targetSets = plan?.let { applied?.sets ?: it.sets },
                perSide = editor.weightKg?.let { PlateLoader.perSide(it, d.settings.changePlates) },
                lastWeightKg = lastToday?.weightKg ?: top?.weightKg,
            )
        }

        val items = planned.mapNotNull { p -> d.history.exercises[p.exerciseId]?.let { item(it, p) } } +
            addedIds.mapNotNull { id -> d.history.exercises[id]?.let { item(it, null) } }
        val shown = items.map { it.exercise.id }.toSet()
        return TodayState(
            date = date,
            loading = false,
            sessionId = d.sessionId,
            items = items,
            pending = l.pending,
            summary = l.summary,
            addable = d.history.exercises.values.filter { !it.archived && it.id !in shown }.sortedBy { it.name.lowercase() },
            weightStepKg = PlateLoader.step(d.settings.changePlates),
            changePlates = d.settings.changePlates,
        )
    }

    private suspend fun loaded(): TodayState = state.first { !it.loading }

    fun startSession(bodyweightKg: Double?, readiness: Int?) {
        if (!starting.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                if (loaded().sessionId == null) training.startSession(date, bodyweightKg, readiness)
            } finally {
                starting.value = false
            }
        }
    }

    /** Changes the numbers for the next set of [exerciseId]. Nothing is saved. */
    fun edit(exerciseId: Long, editor: SetEditor) {
        local.update { it.copy(editors = it.editors + (exerciseId to editor)) }
    }

    /**
     * Saves [set] to today's session. Unless [confirmed], a set that looks like a typo (more than 12
     * reps, or a jump of more than 30% from the last weight) is held in [TodayState.pending] instead.
     */
    fun logSet(planItemId: Long?, exerciseId: Long, set: LoggedSet, confirmed: Boolean = false) {
        // A fast double tap must not save the set twice.
        if (!saving.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                val now = loaded()
                val sessionId = now.sessionId ?: return@launch
                val item = now.items.firstOrNull { it.exercise.id == exerciseId }
                if (!confirmed) {
                    val warnings = InputValidator.check(set.weightKg, set.reps, item?.lastWeightKg)
                    if (warnings.isNotEmpty()) {
                        local.update { it.copy(pending = PendingLog(planItemId, exerciseId, set, warnings)) }
                        return@launch
                    }
                }
                // The top set is worked out from the session afterwards (heaviest good set), never flagged here.
                val setId = training.logSet(sessionId, set.copy(exerciseId = exerciseId, isTopSet = false))
                local.update { it.copy(pending = null) }
                _events.emit(TodayEvent.SetLogged(isMainOrVariation = item?.exercise?.category != Category.ACCESSORY))
                // Hold the guard until the new set is on screen, so the button the lifter sees is never stale.
                withTimeoutOrNull(SHOWN_TIMEOUT_MS) { state.first { s -> s.items.any { i -> i.sets.any { it.id == setId } } } }
            } finally {
                saving.value = false
            }
        }
    }

    /** Back to the editor without saving the held set. */
    fun dismissWarning() {
        local.update { it.copy(pending = null) }
    }

    /** Records where the rep was hardest or missed; null clears it. */
    fun setMissPoint(setId: Long, point: MissPoint?) {
        viewModelScope.launch {
            val stored = loaded().items.flatMap { it.sets }.firstOrNull { it.id == setId } ?: return@launch
            training.updateSet(setId, stored.set.copy(missPoint = point))
        }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch { training.deleteSet(setId) }
    }

    /** Puts the coach's numbers into the editor and marks the advice applied. */
    fun applyTarget(adviceId: Long) {
        viewModelScope.launch {
            val target = loaded().items.mapNotNull { it.suggestion }.firstOrNull { it.id == adviceId } ?: return@launch
            val payload = target.payload as? AdvicePayload.Target ?: return@launch
            // Status first: if storing it fails, the editor keeps its numbers and the chip stays.
            advice.setStatus(adviceId, AdviceStatus.APPLIED)
            edit(payload.exerciseId, SetEditor(payload.weightKg, payload.reps, payload.rpe))
        }
    }

    fun dismissTarget(adviceId: Long) {
        viewModelScope.launch { advice.setStatus(adviceId, AdviceStatus.DISMISSED) }
    }

    /** Adds an exercise for this session only; it is kept once a set is logged for it. */
    fun addExercise(exerciseId: Long) {
        local.update { if (exerciseId in it.added) it else it.copy(added = it.added + exerciseId) }
    }

    /** Takes back an added exercise that has no sets yet. */
    fun removeExercise(exerciseId: Long) {
        local.update { it.copy(added = it.added - exerciseId, editors = it.editors - exerciseId) }
    }

    /** Runs the coach over the history including today and stores what is new. */
    fun finishSession() {
        viewModelScope.launch {
            val now = loaded()
            if (now.sessionId == null) return@launch
            val fresh = Coach.evaluate(
                training.history().first(),
                training.plan().first(),
                settings.settings.first().coach(),
                date,
                advice.all(),
            )
            if (fresh.isNotEmpty()) advice.insert(fresh)
            val done = now.items.filter { it.sets.isNotEmpty() }
            local.update { it.copy(summary = FinishSummary(done.sumOf { i -> i.sets.size }, done.size, fresh.size)) }
        }
    }

    /** Back to logging after the summary. */
    fun dismissSummary() {
        local.update { it.copy(summary = null) }
    }

    companion object {
        private const val DEFAULT_REPS = 5
        private const val SHOWN_TIMEOUT_MS = 2_000L

        private fun Advice.exerciseId(): Long? = (payload as? AdvicePayload.Target)?.exerciseId

        fun factory(
            training: TrainingRepository,
            advice: AdviceRepository,
            settings: SettingsRepository,
            today: () -> LocalDate = LocalDate::now,
        ): ViewModelProvider.Factory = viewModelFactory { initializer { TodayViewModel(training, advice, settings, today) } }
    }
}
