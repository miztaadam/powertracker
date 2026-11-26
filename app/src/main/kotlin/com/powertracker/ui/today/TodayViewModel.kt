package com.powertracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.FinishedSession
import com.powertracker.data.SettingsRepository
import com.powertracker.data.StoredSet
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.coach.BodybuildingCoach
import com.powertracker.domain.coach.Coaching
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.history.LastTime
import com.powertracker.domain.history.lastTimes
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.stack.PlateCheck
import com.powertracker.domain.stack.PlateStack
import com.powertracker.domain.stack.inCurrentUnits
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.SkipReason
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
import com.powertracker.domain.progress.ProgressMath

/**
 * The numbers in the steppers for the next set. [weightKg] is null until the lifter enters one; it is a plate number
 * when [unit] is plates (1.17). An editor kept in another unit than its exercise's is never shown.
 */
data class SetEditor(val weightKg: Double?, val reps: Int, val rpe: Double, val unit: LoadUnit = LoadUnit.KG)

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
    /** The weight a typo is judged against: the last set today, else the last top set before today, both in [unit]. */
    val lastWeightKg: Double?,
    /** The exercise's current unit ([PlateStack.unitOf]): what the editor, the targets and the typo check are in (1.17). */
    val unit: LoadUnit = LoadUnit.KG,
    /** The exercise's stored plate setting, which the plate sheet marks. */
    val plateCount: PlateCount = PlateCount.OFF,
    /** Whether the exercise may count plates at all ([PlateStack.offered]): the row menu then offers it. */
    val plateOffered: Boolean = false,
    /**
     * Every set of this exercise in its last session on the side shown, before the day shown, in logged order and each
     * in the unit it was logged in (1.20, as the web app's since 1.18); null when it was never done on this side. The
     * domain's [lastTimes]: the session the coach's "Last time: …" names, except just after the exercise's plate setting
     * changed, when the coach reads only the sets in the new unit.
     */
    val lastTime: LastTime? = null,
) {
    val logged: List<LoggedSet> get() = sets.map { it.set }
    val isComplete: Boolean get() = targetSets != null && sets.size >= targetSets
}

/** A set held back because it looks like a typo; saved only after the lifter confirms. */
data class PendingLog(val planItemId: Long?, val exerciseId: Long, val set: LoggedSet, val warnings: List<InputWarning>)

/** [skipped] is how many exercises were skipped today on the session's side ("Skip today"). */
data class FinishSummary(val setCount: Int, val exerciseCount: Int, val newNotes: Int, val skipped: Int = 0)

/** A planned exercise skipped today on the side shown, for the "Skipped today" line and its Bring back. */
data class SkippedToday(val exerciseId: Long, val name: String, val skipId: Long)

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
    /** Today's planned exercises skipped on the side shown, in plan order; they are not in [items]. */
    val skippedToday: List<SkippedToday> = emptyList(),
    /** True while a plate setting chosen in the plate sheet is being stored (1.17): the sheet marks the tapped choice. */
    val plateSaving: Boolean = false,
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

    /**
     * The buzz for a saved set, sent just after [SetLogged] once the history has been asked whether it is a new best
     * ([isNewBest], ProgressMath.isNewPr). While "Vibrate on taps" is off the history is not read and it says false.
     */
    data class SetBuzz(val isNewBest: Boolean) : TodayEvent

    /** A coach target was put into the editor (Apply). */
    data object TargetApplied : TodayEvent

    /** A save failed and nothing changed: the screen says so ([com.powertracker.ui.library.SAVE_FAILED]). */
    data object SaveFailed : TodayEvent

    /** A planned exercise was skipped for today and stored as [skipId]: the screen ticks and offers Undo. */
    data class Skipped(val skipId: Long, val name: String) : TodayEvent
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
        /** Plate settings being stored, not yet shown or failed. */
        val plateSaves: Int = 0,
        /** False until it has been read whether this day's session was finished before the app was last closed. */
        val restored: Boolean = false,
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
        /** Every stored skip, both sides': the pain hold crosses sides. */
        val skips: List<Skip>,
    )

    private val local = MutableStateFlow(Local())
    private val starting = MutableStateFlow(false)

    /** True while a set is being saved; further log taps are ignored until the screen shows it. */
    private val saving = MutableStateFlow(false)

    /** True while [addToOpenSession] is working; further calls are ignored until it has answered. */
    private val addingToSession = MutableStateFlow(false)

    /** True while a skip is being stored; a second Skip today meanwhile (a double tap) is ignored. */
    private val skipping = MutableStateFlow(false)

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
            combine(session, training.history(), plan, training.skips()) { session, history, plan, skips -> Quad(session, history, plan, skips) },
            settings.settings, targets, mode, activeHasPlan,
        ) { (session, history, plan, skips), s, targets, mode, hasPlan ->
            Stored(session.first, session.second, history, plan, s, targets, mode, hasPlan, skips)
        }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    /** What the coach's pass is keyed by; [plates] is each plate-counting exercise's (id, setting). */
    private data class CoachKey(
        val mode: TrainingMode,
        val plan: List<PlanItem>,
        val coach: com.powertracker.domain.coach.CoachSettings,
        val skips: List<Skip>,
        val plates: Set<Pair<Long, PlateCount>>,
    )

    val state: StateFlow<TodayState> = combine(stored, local, ::build)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayState(date))

    init {
        // A session finished before the app was closed is still finished when it opens again: the summary Finish stored
        // comes back. The screen has no data until this is read, so the session never shows as open first.
        viewModelScope.launch {
            val finished = try {
                val note = settings.finishedSession.first()
                note?.takeIf { it.date == date && training.sessionOn(date).first()?.id == it.sessionId }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed read must not keep the screen empty: the session shows as open, as it did before 1.17.5.
                null
            }
            local.update { l ->
                l.copy(restored = true, summary = l.summary ?: finished?.let { FinishSummary(it.setCount, it.exerciseCount, it.newNotes, it.skipped) })
            }
        }
        // Ask the coach for today's targets when the screen opens and again if the plan changes, or a skip is stored or
        // taken back: a pain skip, and taking any skip back, delete that day's target, which is then worked out again.
        // A change of an exercise's plate setting asks again too (1.17): the coach then reads it in its new unit. Only
        // the exercises counting plates are in the key, so a kg-only lifter's passes are as they were.
        val plateSettings = training.exercises()
            .map { list -> list.filter { it.plateCount != PlateCount.OFF }.map { it.id to it.plateCount }.toSet() }
            .distinctUntilChanged()
        viewModelScope.launch {
            combine(mode, plan, settings.settings, training.skips(), plateSettings) { mode, plan, s, skips, plates -> CoachKey(mode, plan, s.coach(), skips, plates) }
                .distinctUntilChanged()
                .collect { (mode, plan, coach, skips) ->
                    // A view model kept from an earlier day (the screen keys a new one per day) must not
                    // store targets dated that day any more.
                    if (today() != date) return@collect
                    val prior = advice.all()
                    // Each side has its own targets: one for the other side does not stand in for this one. Each unit
                    // has its own too: a kg target stored today does not stand in for the plate one, nor the other way.
                    // A plate target off the exercise's current grid (plate 7½ once half steps are turned off) does not
                    // count: a fresh one on the grid is stored beside it. Nothing stored is deleted or rewritten.
                    val history = training.history().first()
                    val known = todaysTargets(prior.forSide(mode))
                        .filter { a -> (a.payload as AdvicePayload.Target).onGrid(history.exercises[a.exerciseId()]?.plateCount ?: PlateCount.OFF) }
                        .map { it.targetKey() }.toSet()
                    val fresh = Coaching.targetsFor(mode, date.dayOfWeek, history, plan, coach, date, prior, skips)
                        .filter { it.targetKey() !in known }
                        .distinctBy { it.targetKey() }
                    if (fresh.isNotEmpty()) advice.insert(fresh)
                    _coachPasses.update { it + 1 }
                }
        }
    }

    private fun todaysTargets(all: List<Advice>) =
        all.filter { it.type == AdviceType.TARGET && it.createdAt == date && it.payload is AdvicePayload.Target }

    private fun build(d: Stored, l: Local): TodayState {
        // Each exercise is read in its current unit only: a kg set never fills the PLATE field, nor the other way.
        val stats = HistoryStats(d.history.inCurrentUnits())
        // One card per exercise: sets are stored per exercise, so a second plan line for the same one merges into the first.
        val allPlanned = d.plan.filter { it.weekday == date.dayOfWeek }.sortedBy { it.order }.distinctBy { it.exerciseId }
        val loggedIds = d.sets.map { it.set.exerciseId }.distinct()
        // A planned exercise skipped today on this side leaves the list, unless sets were logged for it after all.
        val skippedIds = todaysSkips(d.skips, d.mode).associateBy { it.exerciseId }
        val planned = allPlanned.filter { it.exerciseId !in skippedIds || it.exerciseId in loggedIds }
        val plannedIds = planned.map { it.exerciseId }.toSet()
        // A catalogue exercise added without a row gets one with its first set (or from the Plan tab meanwhile).
        val live = d.history.exercises.values.filter { !it.archived }.sortedBy { it.id }
        val resolved = l.added.filter { it < 0 }.mapNotNull { standIn ->
            val name = catalogueEntry(standIn)?.name ?: return@mapNotNull null
            live.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { standIn to it.id }
        }.toMap()
        val standInOf = resolved.entries.associate { (standIn, id) -> id to standIn }
        val added = l.added.map { resolved[it] ?: it }
        val addedIds = (loggedIds.filter { it !in added } + added).filter { it !in plannedIds }.distinct()

        // Each exercise's sets in its last session on this side, asked once for all the cards, of the log as it is stored
        // (not the current-unit copy above: "Last time" shows what was done, in the unit it was logged in).
        val past = d.history.lastTimes(d.mode, date)

        fun item(exercise: Exercise, plan: PlanItem?): TodayItem {
            val unit = PlateStack.unitOf(exercise)
            val sets = d.sets.filter { it.set.exerciseId == exercise.id }
            // A stored target in the other unit is neither shown nor applied.
            // ... nor is a plate target off the exercise's current grid.
            val mine = d.targets.filter { a ->
                val target = a.payload as AdvicePayload.Target
                a.exerciseId() == exercise.id && target.unit == unit && target.onGrid(exercise.plateCount)
            }
            // The newest of them (highest id; all are today's) decides, whatever its status (1.17.1): open, it is the
            // chip; applied, it fills the field and sets the sets to do; dismissed, neither. No older one comes back.
            val newest = mine.maxByOrNull { it.id }
            val applied = newest?.takeIf { it.status == AdviceStatus.APPLIED }?.payload as? AdvicePayload.Target
            val top = stats.lastTopSet(exercise.id, date)?.second
            val lastToday = sets.lastOrNull { it.set.unit == unit }?.set
            val rpe = plan?.targetRpe ?: d.settings.defaultRpe
            val kept = (l.editors[exercise.id] ?: l.editors[standInOf[exercise.id]])?.takeIf { it.unit == unit }
            val editor = kept ?: when {
                lastToday != null -> SetEditor(lastToday.weightKg, lastToday.reps, lastToday.rpe ?: rpe, unit)
                applied != null -> SetEditor(applied.weightKg, appliedReps(applied, plan, d.history, sets.size + 1, d.skips), applied.rpe, unit)
                else -> SetEditor(top?.weightKg, plan?.reps ?: top?.reps ?: DEFAULT_REPS, rpe, unit)
            }
            return TodayItem(
                exercise = exercise,
                planned = plan,
                suggestion = newest?.takeIf { it.status == AdviceStatus.OPEN },
                sets = sets,
                editor = editor,
                targetSets = plan?.let { applied?.sets ?: it.sets },
                perSide = if (unit == LoadUnit.KG) editor.weightKg?.let { PlateLoader.perSide(it, d.settings.changePlates) } else null,
                lastWeightKg = lastToday?.weightKg ?: top?.weightKg,
                unit = unit,
                plateCount = exercise.plateCount,
                plateOffered = PlateStack.offered(exercise),
                lastTime = past[exercise.id],
            )
        }

        val items = planned.mapNotNull { p -> d.history.exercises[p.exerciseId]?.let { item(it, p) } } +
            addedIds.mapNotNull { id -> (d.history.exercises[id] ?: catalogueEntry(id)?.standIn(id))?.let { item(it, null) } }
        // Only what is still planned today and not on the list: after a swap there is nothing left to bring back.
        val shownIds = items.map { it.exercise.id }.toSet()
        val skippedToday = allPlanned.mapNotNull { p ->
            val skip = skippedIds[p.exerciseId] ?: return@mapNotNull null
            if (p.exerciseId in shownIds) return@mapNotNull null
            d.history.exercises[p.exerciseId]?.let { SkippedToday(it.id, it.name, skip.id) }
        }
        return TodayState(
            date = date,
            loading = !l.restored,
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
            welcomeBack = Coaching.welcomeBack(
                d.settings.leftOn(d.settings.trainingMode), date, d.activeHasPlan,
                Coaching.firstSessionOn(d.settings.trainingMode, d.history),
            ),
            skippedToday = skippedToday,
            plateSaving = l.plateSaves > 0,
        )
    }

    /** The skips dated today on [mode]'s side. */
    private fun todaysSkips(skips: List<Skip>, mode: TrainingMode) = skips.filter { it.date == date && it.mode == mode }

    /**
     * The reps a target puts in the field: for a range, the reps to beat for [setNumber]; else the target's. [skips] are
     * every stored skip: under a pain hold the aim is not reset to the low end of the range.
     */
    private fun appliedReps(target: AdvicePayload.Target, plan: PlanItem?, history: TrainingHistory, setNumber: Int, skips: List<Skip>): Int =
        if (target.repsHigh != null && plan != null) BodybuildingCoach.repsToBeat(plan, history, setNumber, date, skips) else target.reps

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
                // A new set must be in its exercise's current unit. One made in the other unit (the setting changed
                // between the entry and the tap) is not saved and not relabelled: its number is re-entered in the new
                // field. A held one keeps the unit it was checked in.
                if (!confirmed && item != null && set.unit != item.unit) return@launch
                // A backstop, the rule a backup is restored by: a plate number that is not a whole or half plate from
                // plate 1 to the highest plate is never stored, held or not. One off the exercise's own grid is (a logged
                // half stands with whole plates on).
                if (set.unit == LoadUnit.PLATES && PlateStack.check(set.weightKg, PlateCount.HALF) != PlateCheck.OK) return@launch
                if (!confirmed) {
                    // Judged against the last weight in the same unit only, with the plate step for a plate number.
                    val warnings = InputValidator.check(set.weightKg, set.reps, item?.lastWeightKg, set.unit, PlateStack.step(item?.plateCount ?: PlateCount.OFF))
                    if (warnings.isNotEmpty()) {
                        local.update { it.copy(pending = PendingLog(planItemId, exerciseId, set, warnings)) }
                        return@launch
                    }
                }
                val stored = set.copy(exerciseId = exerciseId, isTopSet = false)
                // The top set is worked out from the session afterwards (heaviest good set), never flagged here.
                // A stand-in id means a catalogue exercise with no row yet: the row is saved with this set, in one transaction.
                val setId = training.logSet(sessionId, stored, catalogueName = catalogueEntry(exerciseId)?.name)
                local.update { it.copy(pending = null) }
                // Every exercise on the bodybuilding side takes the accessory rest (bodybuilding mode spec §5.3).
                val mainRest = now.mode == TrainingMode.POWERLIFTING && item?.exercise?.category != Category.ACCESSORY
                _events.emit(TodayEvent.SetLogged(isMainOrVariation = mainRest))
                // The buzz is worked out after the save, beside it: it never holds the set up, and if it fails it ticks.
                viewModelScope.launch { _events.emit(TodayEvent.SetBuzz(isNewBest(setId, stored, now.mode))) }
                // Hold the guard until the new set is on screen, so the button the lifter sees is never stale.
                withTimeoutOrNull(SHOWN_TIMEOUT_MS) { state.first { s -> s.items.any { i -> i.sets.any { it.id == setId } } } }
            } finally {
                saving.value = false
            }
        }
    }

    /**
     * Whether the set just saved as [setId] is a new best, asked of the history without it, under the rule Progress uses
     * for its records. False (a tick) when the history cannot be read; not asked at all while nothing would buzz.
     */
    private suspend fun isNewBest(setId: Long, set: LoggedSet, mode: TrainingMode): Boolean = try {
        settings.settings.first().vibrateOnTaps && ProgressMath.isNewPr(training.historyWithout(setId), set, date, mode)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
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

    /**
     * Stores [exerciseId]'s plate setting (1.17). A stand-in id (negative) is a catalogue exercise with no row yet: its
     * row is made in the same write, as its first set would make it. Needs no session. The numbers kept for its next
     * set, under its id and its stand-in's, are dropped, so the field starts afresh in the new unit; no stored set or
     * target changes. If the save fails, [events] sends [TodayEvent.SaveFailed] and nothing changes.
     * [TodayState.plateSaving] is true from the call until the new setting is on screen, or the save has failed.
     */
    fun setPlateCount(exerciseId: Long, value: PlateCount) {
        local.update { it.copy(plateSaves = it.plateSaves + 1) }
        val request = ++plateRequests
        viewModelScope.launch {
            try {
                val id = try {
                    training.setPlateCount(exerciseId.takeIf { it > 0 }, catalogueEntry(exerciseId)?.name, value)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _events.emit(TodayEvent.SaveFailed)
                    return@launch
                }
                val standIns = state.value.resolved.filterValues { it == id || it == exerciseId }.keys
                val dropped = setOf(exerciseId, id) + standIns
                local.update { it.copy(editors = it.editors - dropped) }
                // Settled once the stored setting is what the screen shows, or a later choice has been made (it settles then).
                withTimeoutOrNull(SHOWN_TIMEOUT_MS) {
                    state.first { s -> request != plateRequests || s.items.any { it.exercise.id == id && it.plateCount == value } }
                }
            } finally {
                local.update { it.copy(plateSaves = (it.plateSaves - 1).coerceAtLeast(0)) }
            }
        }
    }

    /** Puts the coach's numbers into the editor and marks the advice applied. */
    fun applyTarget(adviceId: Long) {
        // A second tap landing before the first has applied (a double tap) applies nothing more and says nothing more.
        if (!applyingTargets.add(adviceId)) return
        viewModelScope.launch {
            var applied = false
            try {
                val now = loaded()
                val item = now.items.firstOrNull { it.suggestion?.id == adviceId } ?: return@launch
                val target = item.suggestion ?: return@launch
                val payload = target.payload as? AdvicePayload.Target ?: return@launch
                // A range target fills the reps to beat for the set about to be done (bodybuilding mode spec §5.3).
                val reps = appliedReps(payload, item.planned, training.history().first(), item.sets.size + 1, training.skips().first())
                // Status first: if storing it fails, the editor keeps its numbers and the chip stays.
                advice.setStatus(adviceId, AdviceStatus.APPLIED)
                edit(payload.exerciseId, SetEditor(payload.weightKg, reps, payload.rpe, payload.unit))
                applied = true
                _events.emit(TodayEvent.TargetApplied)
            } finally {
                // Kept once applied: the advice is closed for good. Otherwise it can be tried again.
                if (!applied) applyingTargets.remove(adviceId)
            }
        }
    }

    /** How many plate settings have been asked for; touched on the main thread only. */
    private var plateRequests = 0

    /** Coach targets whose Apply is under way or done; touched on the main thread only. */
    private val applyingTargets = mutableSetOf<Long>()

    fun dismissTarget(adviceId: Long) {
        viewModelScope.launch { advice.setStatus(adviceId, AdviceStatus.DISMISSED) }
    }

    /**
     * Adds an exercise for this session only; it is kept once a set is logged for it. One skipped today on this side is
     * brought back instead ([bringBack]): its skip is taken back and it returns as its planned row.
     */
    fun addExercise(exerciseId: Long) {
        if (state.value.skippedToday.any { it.exerciseId == exerciseId }) {
            bringBack(exerciseId)
            return
        }
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
     * (a name or an alias, ignoring case) is added from the catalogue instead, as if it had been picked. If the save
     * fails, [onCreated] is not called, [events] sends [TodayEvent.SaveFailed] and [onFailed] runs.
     */
    fun createExercise(name: String, category: Category, parentLift: MainLift?, muscles: List<String>, onFailed: () -> Unit = {}, onCreated: (Long) -> Unit) {
        val known = Library.find(name.trim())
        if (known != null) {
            addFromCatalogue(known.name)?.let(onCreated)
            return
        }
        viewModelScope.launch {
            val id = try {
                training.addExercise(Exercise(0, name.trim(), category, parentLift, muscles, isBuiltIn = false))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed write must not crash the app: nothing was saved or added, and the lifter is told.
                _events.emit(TodayEvent.SaveFailed)
                onFailed()
                return@launch
            }
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
            val skipped = todaysSkips(training.skips().first(), now.mode).size
            val summary = FinishSummary(done.sumOf { i -> i.sets.size }, done.size, fresh.size, skipped)
            // Stored before it is shown, so the summary on screen means the session stays finished when the app is
            // closed and opened. A failed write only costs that: the session is finished on screen all the same.
            try {
                settings.setFinishedSession(FinishedSession(now.sessionId, date, summary.setCount, summary.exerciseCount, summary.newNotes, summary.skipped))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Nothing to undo.
            }
            local.update { it.copy(summary = summary) }
        }
    }

    /**
     * Skips [exerciseId], planned today with no sets logged, for today on the side shown, with an optional [reason]
     * ("Just skip" is null). The skip and the coach's card for it, if any ([Coaching.afterSkip]), are stored together;
     * [events] then sends [TodayEvent.Skipped]. Skipping needs no session. Skipping one already skipped today replaces
     * its reason and its card. Anything else (sets logged, not planned today, or a skip already being stored) does nothing.
     */
    fun skip(exerciseId: Long, planItemId: Long?, reason: SkipReason?) {
        if (!skipping.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                val now = loaded()
                val item = now.items.firstOrNull { it.exercise.id == exerciseId }
                // A row on the list, planned and with nothing logged; or one skipped already today, whose reason this replaces.
                val again = now.skippedToday.firstOrNull { it.exerciseId == exerciseId }
                val name = when {
                    item != null -> if (item.planned == null || item.sets.isNotEmpty()) return@launch else item.exercise.name
                    again != null -> again.name
                    else -> return@launch
                }
                val history = training.history().first()
                val plans = training.plans().first()
                val skipId = training.skip(Skip(0, date, now.mode, exerciseId, planItemId ?: item?.planned?.id, reason)) { stored, all ->
                    // The advice is read here, inside the transaction: a re-skip's old card is already gone, so it
                    // does not hold back the new reason's card.
                    Coaching.afterSkip(stored, all, history, plans.powerlifting + plans.bodybuilding, date, advice.all())
                }
                _events.emit(TodayEvent.Skipped(skipId, name))
                // Hold the guard until the row has left, so a second tap cannot skip it again.
                withTimeoutOrNull(SHOWN_TIMEOUT_MS) { state.first { s -> s.items.none { it.exercise.id == exerciseId } } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed write must not crash the app: nothing was stored, and the lifter is told.
                _events.emit(TodayEvent.SaveFailed)
            } finally {
                skipping.value = false
            }
        }
    }

    /** Undo on the snackbar: takes back the skip [skipId] and its still-open card. An answered card stays. */
    fun undoSkip(skipId: Long) {
        viewModelScope.launch {
            try {
                training.unskip(skipId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.emit(TodayEvent.SaveFailed)
            }
        }
    }

    /** Bring back on the "Skipped today" line: takes back today's skip of [exerciseId] on the side shown. */
    fun bringBack(exerciseId: Long) {
        val skipId = state.value.skippedToday.firstOrNull { it.exerciseId == exerciseId }?.skipId ?: return
        undoSkip(skipId)
    }

    /**
     * The welcome-back card was answered, either way: the side the app is on is marked as not left, so the card does
     * not come back. "My goals have changed" also opens the Plan; the screen does that.
     */
    fun answerWelcomeBack() {
        viewModelScope.launch { settings.clearLeftOn(settings.settings.first().trainingMode) }
    }

    /** Back to logging after the summary: the session is open again, also when the app is next opened. */
    fun dismissSummary() {
        local.update { it.copy(summary = null) }
        viewModelScope.launch {
            try {
                settings.setFinishedSession(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The session is open on screen; a failed write only brings the summary back on the next open.
            }
        }
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

        /** A target's exercise and unit: today holds at most one target for each. */
        private fun Advice.targetKey(): Pair<Long, LoadUnit>? = (payload as? AdvicePayload.Target)?.let { it.exerciseId to it.unit }

        /** A kg target, or a plate target whose plate is on [count]'s grid ([PlateStack.check] is OK). */
        private fun AdvicePayload.Target.onGrid(count: PlateCount): Boolean =
            unit != LoadUnit.PLATES || PlateStack.check(weightKg, count) == com.powertracker.domain.stack.PlateCheck.OK

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
