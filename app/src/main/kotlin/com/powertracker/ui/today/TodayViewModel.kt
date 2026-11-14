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
import com.powertracker.domain.coach.BodybuildingCoach
import com.powertracker.domain.coach.Coaching
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.forSide
import com.powertracker.domain.plates.PlateLoader
import com.powertracker.domain.validation.InputValidator
import com.powertracker.domain.validation.InputWarning
import com.powertracker.platform.systemToday
import com.powertracker.ui.library.recentExerciseIds
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.yield
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate

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
    /** The lifter's non-archived exercises, for the picker. */
    val exercises: List<Exercise> = emptyList(),
    /** Exercise ids, most recently logged first, for the picker's "Your exercises". */
    val recentIds: List<Long> = emptyList(),
    /**
     * A catalogue exercise added before it had a row is shown under a stand-in id; this maps each
     * stand-in to the row's id once the row exists, so the screen can follow the card across.
     */
    val resolved: Map<Long, Long> = emptyMap(),
    val weightStepKg: Double = 2.5,
    val changePlates: Boolean = false,
    /** The side of today's session when there is one, else the side the app is on (bodybuilding mode spec §5.1). */
    val mode: TrainingMode = TrainingMode.POWERLIFTING,
    /** Says that today's session is on the other side from the app's; null when they agree. */
    val sideNote: String? = null,
    /** The welcome-back card for the side the app is on (bodybuilding mode spec §7.7). */
    val welcomeBack: Boolean = false,
) {
    val pendingWarning: InputWarning? get() = pending?.warnings?.firstOrNull()

    /** A session was started today and has not been finished: sets can be logged and exercises added. */
    val inSession: Boolean get() = sessionId != null && summary == null
}

/** How adding an exercise to today's session from outside the Today screen went. */
enum class TodayAdd {
    ADDED,

    /** The exercise is already in today's session; nothing changed. */
    ALREADY_THERE,

    /** There is no open, unfinished session to add to (or no such exercise), or it failed; nothing changed. */
    REFUSED,
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
    private val today: () -> LocalDate = ::systemToday,
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
        val mode: TrainingMode,
        val activeHasPlan: Boolean,
    )

    private val local = MutableStateFlow(Local())
    private val starting = MutableStateFlow(false)

    /** True while a set is being saved; further log taps are ignored until the screen shows it. */
    private val saving = MutableStateFlow(false)

    /** True while [addToOpenSession] is working; further calls are ignored until it has answered. */
    private val addingToSession = MutableStateFlow(false)

    private val _coachPasses = MutableStateFlow(0)

    /** How many times today's targets have been worked out and stored (once on open, again when the plan changes). */
    val coachPasses: StateFlow<Int> = _coachPasses

    private val _events = MutableSharedFlow<TodayEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<TodayEvent> = _events

    private val session: Flow<Pair<Long?, List<StoredSet>>> =
        training.sessionOn(date).map { it?.id }.distinctUntilChanged().flatMapLatest { id ->
            if (id == null) flowOf(null to emptyList()) else training.setsForSession(id).map { id to it }
        }

    /**
     * The side Today shows: today's session's when there is one, else the side the app is on. Switching on a day
     * already trained leaves today's session, its plan and its targets as they were (bodybuilding mode spec §5.1).
     */
    private val mode: Flow<TrainingMode> =
        combine(training.sessionOn(date), settings.settings) { session, s -> session?.mode ?: s.trainingMode }.distinctUntilChanged()

    /** The plan of the side shown. */
    private val plan: Flow<List<PlanItem>> = combine(training.plans(), mode) { plans, mode -> plans.of(mode) }

    /** Whether the side the app is on has a plan, for the welcome-back card. */
    private val activeHasPlan: Flow<Boolean> =
        combine(training.plans(), settings.settings) { plans, s -> plans.of(s.trainingMode).isNotEmpty() }.distinctUntilChanged()

    /** Today's TARGET advice for the side shown, in every status; re-read whenever the advice table changes. */
    private val targets: Flow<List<Advice>> = combine(advice.open(), mode) { _, mode -> todaysTargets(advice.all().forSide(mode)) }

    private val stored: Flow<Stored> =
        combine(
            combine(session, training.history(), plan, ::Triple),
            settings.settings, targets, mode, activeHasPlan,
        ) { (session, history, plan), s, targets, mode, hasPlan ->
            Stored(session.first, session.second, history, plan, s, targets, mode, hasPlan)
        }

    val state: StateFlow<TodayState> = combine(stored, local, ::build)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayState(date))

    init {
        // Ask the coach for today's targets when the screen opens and again if the plan changes.
        viewModelScope.launch {
            combine(mode, plan, settings.settings) { mode, plan, s -> Triple(mode, plan, s.coach()) }
                .distinctUntilChanged()
                .collect { (mode, plan, coach) ->
                    // A view model kept from an earlier day (the screen keys a new one per day) must not
                    // store targets dated that day any more.
                    if (today() != date) return@collect
                    val prior = advice.all()
                    // Each side has its own targets: one for the other side does not stand in for this one.
                    val known = todaysTargets(prior.forSide(mode)).map { it.exerciseId() }.toSet()
                    val fresh = Coaching.targetsFor(mode, date.dayOfWeek, training.history().first(), plan, coach, date, prior)
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
        // A catalogue exercise added without a row gets one with its first set (or from the Plan tab meanwhile).
        val live = d.history.exercises.values.filter { !it.archived }.sortedBy { it.id }
        val resolved = l.added.filter { it < 0 }.mapNotNull { standIn ->
            val name = catalogueEntry(standIn)?.name ?: return@mapNotNull null
            live.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { standIn to it.id }
        }.toMap()
        val standInOf = resolved.entries.associate { (standIn, id) -> id to standIn }
        val added = l.added.map { resolved[it] ?: it }
        val addedIds = (loggedIds.filter { it !in added } + added).filter { it !in plannedIds }.distinct()

        fun item(exercise: Exercise, plan: PlanItem?): TodayItem {
            val sets = d.sets.filter { it.set.exerciseId == exercise.id }
            val mine = d.targets.filter { it.exerciseId() == exercise.id }
            val applied = mine.lastOrNull { it.status == AdviceStatus.APPLIED }?.payload as? AdvicePayload.Target
            val top = stats.lastTopSet(exercise.id, date)?.second
            val lastToday = sets.lastOrNull()?.set
            val rpe = plan?.targetRpe ?: d.settings.defaultRpe
            val editor = l.editors[exercise.id] ?: l.editors[standInOf[exercise.id]] ?: when {
                lastToday != null -> SetEditor(lastToday.weightKg, lastToday.reps, lastToday.rpe ?: rpe)
                applied != null -> SetEditor(applied.weightKg, appliedReps(applied, plan, d.history, sets.size + 1), applied.rpe)
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
            addedIds.mapNotNull { id -> (d.history.exercises[id] ?: catalogueEntry(id)?.standIn(id))?.let { item(it, null) } }
        return TodayState(
            date = date,
            loading = false,
            sessionId = d.sessionId,
            items = items,
            pending = l.pending,
            summary = l.summary,
            exercises = live,
            recentIds = recentExerciseIds(d.history),
            resolved = resolved,
            weightStepKg = PlateLoader.step(d.settings.changePlates),
            changePlates = d.settings.changePlates,
            mode = d.mode,
            sideNote = if (d.sessionId != null && d.mode != d.settings.trainingMode) sideNote(d.mode) else null,
            welcomeBack = Coaching.welcomeBack(d.settings.leftOn(d.settings.trainingMode), date, d.activeHasPlan),
        )
    }

    /** The reps a target puts in the field: for a range, the reps to beat for [setNumber]; else the target's. */
    private fun appliedReps(target: AdvicePayload.Target, plan: PlanItem?, history: TrainingHistory, setNumber: Int): Int =
        if (target.repsHigh != null && plan != null) BodybuildingCoach.repsToBeat(plan, history, setNumber, date) else target.reps

    private suspend fun loaded(): TodayState = state.first { !it.loading }

    fun startSession(bodyweightKg: Double?, readiness: Int?) {
        if (!starting.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                val now = loaded()
                // With no session yet, Today is on the side the app is on: the session is stamped with it.
                if (now.sessionId == null) training.startSession(date, bodyweightKg, readiness, now.mode)
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
                // A stand-in id means a catalogue exercise with no row yet: the row is saved with this set, in one transaction.
                val setId = training.logSet(sessionId, set.copy(exerciseId = exerciseId, isTopSet = false), catalogueName = catalogueEntry(exerciseId)?.name)
                local.update { it.copy(pending = null) }
                // Every exercise on the bodybuilding side takes the accessory rest (bodybuilding mode spec §5.3).
                val mainRest = now.mode == TrainingMode.POWERLIFTING && item?.exercise?.category != Category.ACCESSORY
                _events.emit(TodayEvent.SetLogged(isMainOrVariation = mainRest))
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
            val now = loaded()
            val item = now.items.firstOrNull { it.suggestion?.id == adviceId } ?: return@launch
            val target = item.suggestion ?: return@launch
            val payload = target.payload as? AdvicePayload.Target ?: return@launch
            // A range target fills the reps to beat for the set about to be done (bodybuilding mode spec §5.3).
            val reps = appliedReps(payload, item.planned, training.history().first(), item.sets.size + 1)
            // Status first: if storing it fails, the editor keeps its numbers and the chip stays.
            advice.setStatus(adviceId, AdviceStatus.APPLIED)
            edit(payload.exerciseId, SetEditor(payload.weightKg, reps, payload.rpe))
        }
    }

    fun dismissTarget(adviceId: Long) {
        viewModelScope.launch { advice.setStatus(adviceId, AdviceStatus.DISMISSED) }
    }

    /** Adds an exercise for this session only; it is kept once a set is logged for it. */
    fun addExercise(exerciseId: Long) {
        local.update { if (exerciseId in it.added) it else it.copy(added = it.added + exerciseId) }
    }

    /**
     * Adds a catalogue exercise for this session and returns the id its card is shown under: its
     * row's id when it has one, otherwise a stand-in; null for a name the catalogue doesn't know.
     * Nothing is stored until a set is logged for it.
     */
    fun addFromCatalogue(name: String): Long? = catalogueId(name, state.value)?.also(::addExercise)

    /** The id a catalogue exercise's card is shown under: its row's when it has one, otherwise a stand-in. */
    private fun catalogueId(name: String, now: TodayState): Long? {
        val entry = Library.find(name) ?: return null
        return now.exercises.firstOrNull { it.name.equals(entry.name, ignoreCase = true) }?.id ?: standInId(entry)
    }

    /**
     * Adds an exercise from another screen (the Exercises tab) the way "Add exercise" here does: a
     * catalogue exercise by [catalogueName], with no row until its first set; the lifter's own by
     * [exerciseId]. The session is checked when this is called, not when the button was drawn: with no
     * session, or one finished meanwhile, nothing is added. A session still on screen after midnight
     * takes it, as Today's own "Add exercise" does. [onResult] hears how it went, once; a
     * second call before that (a double tap) does nothing.
     */
    fun addToOpenSession(catalogueName: String?, exerciseId: Long?, onResult: (TodayAdd) -> Unit) {
        if (!addingToSession.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            val result = try {
                // After whatever else this tap set off: a second tap in the same frame meets the guard.
                yield()
                val now = loaded()
                val id = if (catalogueName != null) catalogueId(catalogueName, now) else exerciseId
                when {
                    // The summary is read where Finish writes it, so a Finish a moment ago counts.
                    now.sessionId == null || local.value.summary != null || id == null -> TodayAdd.REFUSED
                    now.items.any { it.exercise.id == id } -> TodayAdd.ALREADY_THERE
                    else -> {
                        addExercise(id)
                        // Hold the guard until its card is there, so a second tap cannot be told "already there".
                        withTimeoutOrNull(SHOWN_TIMEOUT_MS) { state.first { s -> s.items.any { it.exercise.id == id } } }
                        TodayAdd.ADDED
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed read must not crash the app: nothing was added, and the lifter is told.
                TodayAdd.REFUSED
            } finally {
                addingToSession.value = false
            }
            onResult(result)
        }
    }

    /**
     * Creates a custom exercise, adds it to this session and reports its id. A name the catalogue knows
     * (a name or an alias, ignoring case) is added from the catalogue instead, as if it had been picked.
     */
    fun createExercise(name: String, category: Category, parentLift: MainLift?, muscles: List<String>, onCreated: (Long) -> Unit) {
        val known = Library.find(name.trim())
        if (known != null) {
            addFromCatalogue(known.name)?.let(onCreated)
            return
        }
        viewModelScope.launch {
            val id = training.addExercise(Exercise(0, name.trim(), category, parentLift, muscles, isBuiltIn = false))
            addExercise(id)
            onCreated(id)
        }
    }

    /** Takes back an added exercise that has no sets yet. */
    fun removeExercise(exerciseId: Long) {
        val standIn = state.value.resolved.entries.firstOrNull { it.value == exerciseId }?.key
        local.update { it.copy(added = it.added - exerciseId - listOfNotNull(standIn).toSet(), editors = it.editors - exerciseId - listOfNotNull(standIn).toSet()) }
    }

    /** Runs the coach over the history including today and stores what is new. */
    fun finishSession() {
        viewModelScope.launch {
            val now = loaded()
            if (now.sessionId == null) return@launch
            // The session's own side: its plan and its advice (deloads and easing back in count on both).
            val fresh = Coaching.evaluate(
                now.mode,
                training.history().first(),
                training.plan(now.mode).first(),
                settings.settings.first().coach(),
                date,
                advice.all(),
                // The latest check-in's growing-well body parts keep their exercises out of the swaps (stage 3).
                training.checkIns().first(),
            )
            if (fresh.isNotEmpty()) advice.insert(fresh)
            val done = now.items.filter { it.sets.isNotEmpty() }
            local.update { it.copy(summary = FinishSummary(done.sumOf { i -> i.sets.size }, done.size, fresh.size)) }
        }
    }

    /**
     * The welcome-back card was answered, either way: the side the app is on is marked as not left, so the card does
     * not come back. "My goals have changed" also opens the Plan; the screen does that.
     */
    fun answerWelcomeBack() {
        viewModelScope.launch { settings.clearLeftOn(settings.settings.first().trainingMode) }
    }

    /** Back to logging after the summary. */
    fun dismissSummary() {
        local.update { it.copy(summary = null) }
    }

    companion object {
        private const val DEFAULT_REPS = 5

        /** One line under the header when today's session is on [sessionMode] and the app is on the other side. */
        fun sideNote(sessionMode: TrainingMode): String = when (sessionMode) {
            TrainingMode.POWERLIFTING -> "Today's session is on the powerlifting side. Bodybuilding starts with your next session."
            TrainingMode.BODYBUILDING -> "Today's session is on the bodybuilding side. Powerlifting starts with your next session."
        }
        private const val SHOWN_TIMEOUT_MS = 2_000L

        private fun Advice.exerciseId(): Long? = (payload as? AdvicePayload.Target)?.exerciseId

        /** The id a catalogue exercise is shown under while it has no row: negative, so it can never be a row's. */
        private fun standInId(entry: LibraryExercise): Long = -(Library.all().indexOf(entry) + 1L)

        /** The catalogue entry behind a stand-in id; null for a row's id. */
        private fun catalogueEntry(id: Long): LibraryExercise? = if (id < 0) Library.all().getOrNull((-id - 1).toInt()) else null

        private fun LibraryExercise.standIn(id: Long) = Exercise(id, name, category, parentLift, muscles, isBuiltIn = true)

        fun factory(
            training: TrainingRepository,
            advice: AdviceRepository,
            settings: SettingsRepository,
            today: () -> LocalDate = ::systemToday,
        ): ViewModelProvider.Factory = viewModelFactory { initializer { TodayViewModel(training, advice, settings, today) } }
    }
}
