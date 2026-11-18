package com.powertracker.ui.plan.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plan.BuilderAnswers
import com.powertracker.domain.plan.BuilderPick
import com.powertracker.domain.plan.PlanBuilder
import com.powertracker.domain.plan.PlanDraft
import com.powertracker.platform.systemToday
import com.powertracker.ui.library.PickerEntry
import com.powertracker.ui.library.recentExerciseIds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/** The builder's four steps, in order. */
enum class BuilderStep { DAYS, EXERCISES, BODY_PARTS, DRAFT }

/**
 * One ticked exercise: what the builder is told about it, for the lifter's own exercise the row it is saved with, and
 * the browser's [entry] it was ticked from (listed under "Chosen").
 */
data class ChosenPick(val pick: BuilderPick, val exerciseId: Long?, val entry: PickerEntry)

data class PlanBuilderState(
    val loading: Boolean = true,
    /** The side the app is on; the builder is only for the bodybuilding side. */
    val mode: TrainingMode? = null,
    val step: BuilderStep = BuilderStep.DAYS,
    /** 3 or 4. */
    val dayCount: Int = 3,
    val days: Set<DayOfWeek> = emptySet(),
    /** The lifter's stored exercises and the ids most recently logged, for the exercise browser. */
    val exercises: List<Exercise> = emptyList(),
    val recentIds: List<Long> = emptyList(),
    /** The ticked exercises by [pickKey], in the order they were ticked. */
    val ticked: Map<String, ChosenPick> = emptyMap(),
    /** The body parts to improve most, in the order chosen; at most [PlanBuilder.MAX_PRIORITIES]. */
    val priorities: List<MuscleGroup> = emptyList(),
    /** A tick past the limit was just refused: the step says why until a body part is unticked. */
    val priorityRefused: Boolean = false,
    val draft: PlanDraft? = null,
    /** "Another draft" counts up from 0. */
    val variant: Int = 0,
    /** The bodybuilding side already has a plan: using the draft asks first. */
    val hasPlan: Boolean = false,
    /** The "this replaces your plan" question is showing. */
    val confirming: Boolean = false,
    val saving: Boolean = false,
    /** The draft is stored as the plan and the days are set: the screen returns to Plan. */
    val saved: Boolean = false,
    /** The save failed; nothing was changed. */
    val failed: Boolean = false,
) {
    val available: Boolean get() = mode == TrainingMode.BODYBUILDING
    val targetGroups: List<MuscleGroup> get() = PlanBuilder.TARGET_GROUPS
    val chosenCount: Int get() = ticked.size
    val nothingTicked: Boolean get() = ticked.isEmpty()
    /** Every ticked exercise, in the order ticked (pre-ticks first): the exercises step's "Chosen" section. */
    val chosen: List<PickerEntry> get() = ticked.values.map { it.entry }
    val canNext: Boolean get() = available && (step != BuilderStep.DAYS || days.size == dayCount)
}

/**
 * Stores [days] as [mode]'s training days: `bodybuildingTrainingDays` for the bodybuilding side, `trainingDays` for the
 * powerlifting side, whichever side the app is on. The builder's and the templates' save (1.14) both set the days this way.
 */
suspend fun setSideTrainingDays(settings: SettingsRepository, mode: TrainingMode, days: Set<DayOfWeek>) = settings.update {
    if (mode == TrainingMode.BODYBUILDING) it.copy(bodybuildingTrainingDays = days) else it.copy(trainingDays = days)
}

/** How the builder tells ticked exercises apart: a catalogue exercise by its name, the lifter's own by its row. */
fun pickKey(entry: PickerEntry): String = entry.catalogueName ?: "own-${entry.exerciseId}"

/**
 * The bodybuilding plan builder (stage 2 plan, Task 2): days, exercises, body parts, then the draft the shared
 * [PlanBuilder] makes of those answers. Nothing is stored until the lifter uses the draft; then the side's whole plan is
 * replaced ([replacePlan]) and [setDays] sets its training days to the chosen weekdays, both or neither (see [store], as
 * the templates save). A test can make either write fail.
 */
class PlanBuilderViewModel(
    private val training: TrainingRepository,
    private val settings: SettingsRepository,
    private val setDays: suspend (Set<DayOfWeek>) -> Unit = { days -> setSideTrainingDays(settings, TrainingMode.BODYBUILDING, days) },
    private val replacePlan: suspend (List<Pair<PlanItem, String?>>) -> Unit = { items -> training.replacePlan(TrainingMode.BODYBUILDING, items) },
    private val today: () -> LocalDate = ::systemToday,
) : ViewModel() {
    private val _state = MutableStateFlow(PlanBuilderState())
    val state: StateFlow<PlanBuilderState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // The answers start from what is stored when the builder opens; later changes only refresh the lists.
            val s = settings.settings.first()
            val history = training.history().first()
            val plans = training.plans().first()
            val stored = history.exercises
            val suggested = PlanBuilder.suggestedPicks(history, plans.powerlifting + plans.bodybuilding, today())
            val preTicked = suggested
                .mapNotNull { id -> stored[id]?.let(::entryFor) }
                .associateBy(::pickKey)
                .mapValues { (_, entry) -> chosen(entry) }
            val days = s.bodybuildingTrainingDays
            _state.update {
                it.copy(
                    loading = false,
                    mode = s.trainingMode,
                    dayCount = if (days.size == 4) 4 else 3,
                    days = if (days.size in 3..4) days else emptySet(),
                    ticked = preTicked,
                    hasPlan = plans.bodybuilding.isNotEmpty(),
                )
            }
            combine(training.history(), training.plan(TrainingMode.BODYBUILDING), settings.settings) { h, plan, st -> Triple(h, plan, st) }
                .collect { (h, plan, st) ->
                    _state.update {
                        it.copy(
                            exercises = h.exercises.values.filter { e -> !e.archived }.sortedBy { e -> e.id },
                            // The usual list: what is ticked is listed above it, under "Chosen".
                            recentIds = recentExerciseIds(h),
                            hasPlan = plan.isNotEmpty(),
                            mode = st.trainingMode,
                        )
                    }
                }
        }
    }

    // Step 1

    fun chooseDayCount(count: Int) {
        if (count == 3 || count == 4) _state.update { it.copy(dayCount = count) }
    }

    fun toggleDay(day: DayOfWeek) = _state.update { it.copy(days = if (day in it.days) it.days - day else it.days + day) }

    // Step 2

    fun isTicked(entry: PickerEntry): Boolean = pickKey(entry) in _state.value.ticked

    fun toggle(entry: PickerEntry) {
        val key = pickKey(entry)
        _state.update { s -> s.copy(ticked = if (key in s.ticked) s.ticked - key else s.ticked + (key to chosen(entry))) }
    }

    /** Unticks the exercise with [key], if it is ticked. */
    fun untick(key: String) = _state.update { s -> s.copy(ticked = s.ticked - key) }

    // Step 3

    fun togglePriority(group: MuscleGroup) {
        if (group !in PlanBuilder.TARGET_GROUPS) return
        _state.update { s ->
            when {
                group in s.priorities -> s.copy(priorities = s.priorities - group, priorityRefused = false)
                s.priorities.size >= PlanBuilder.MAX_PRIORITIES -> s.copy(priorityRefused = true)
                else -> s.copy(priorities = s.priorities + group, priorityRefused = false)
            }
        }
    }

    // Moving between steps

    fun next() {
        val s = _state.value
        if (!s.canNext) return
        when (s.step) {
            BuilderStep.DAYS -> _state.update { it.copy(step = BuilderStep.EXERCISES) }
            BuilderStep.EXERCISES -> _state.update { it.copy(step = BuilderStep.BODY_PARTS) }
            BuilderStep.BODY_PARTS -> _state.update { it.copy(step = BuilderStep.DRAFT, variant = 0, draft = build(it, 0), priorityRefused = false) }
            BuilderStep.DRAFT -> Unit
        }
    }

    /** One step back; false on the first step, where back leaves the builder. Nothing is stored either way. */
    fun back(): Boolean {
        val s = _state.value
        if (s.step == BuilderStep.DAYS) return false
        _state.update { it.copy(step = BuilderStep.entries[s.step.ordinal - 1], confirming = false, priorityRefused = false) }
        return true
    }

    // Step 4

    fun anotherDraft() {
        val s = _state.value
        if (s.step != BuilderStep.DRAFT || !s.available) return
        val variant = s.variant + 1
        _state.update { it.copy(variant = variant, draft = build(it, variant)) }
    }

    /** What [name] on [day] may be swapped for: the shared builder's alternatives, none already on that day. */
    fun alternatives(day: DayOfWeek, name: String): List<LibraryExercise> {
        val s = _state.value
        val draft = s.draft ?: return emptyList()
        return PlanBuilder.alternatives(name, draft, picks(s), day)
    }

    fun swap(day: DayOfWeek, from: String, to: String) {
        val draft = _state.value.draft ?: return
        val swapped = runCatching { PlanBuilder.swap(draft, day, from, to) }.getOrNull() ?: return
        _state.update { it.copy(draft = swapped) }
    }

    /** "Use this plan": asks first when it would replace a plan, otherwise saves. */
    fun use() {
        val s = _state.value
        if (s.draft == null || !s.available || s.saving) return
        if (s.hasPlan) _state.update { it.copy(confirming = true) } else save()
    }

    fun confirmReplace() {
        if (!_state.value.confirming) return
        _state.update { it.copy(confirming = false) }
        save()
    }

    fun cancelReplace() = _state.update { it.copy(confirming = false) }

    private fun save() {
        val s = _state.value
        val draft = s.draft ?: return
        if (!s.available || s.saving) return
        _state.update { it.copy(saving = true, failed = false) }
        // The lifter's own exercises are saved with their rows; catalogue exercises by name, resolved in the transaction.
        val ownIds = s.ticked.values.filter { !it.pick.inCatalogue }.associate { Library.normalise(it.pick.name) to it.exerciseId }
        val items = draft.sessions.flatMap { session ->
            session.exercises.mapIndexed { order, e ->
                val item = PlanItem(
                    0, session.day, if (e.inCatalogue) 0 else ownIds[Library.normalise(e.name)] ?: 0, e.sets, e.repsLow, e.rpe, order,
                    TrainingMode.BODYBUILDING, e.repsHigh,
                )
                item to (if (e.inCatalogue) e.name else null)
            }
        }
        val days = s.days
        viewModelScope.launch {
            try {
                // Not cut short by leaving the screen mid-save: the writes finish, or are undone, together.
                withContext(NonCancellable) { store(items, days) }
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Nothing was changed (see store): the builder stays open with the error, and trying again saves both.
                _state.update { it.copy(saving = false, failed = true) }
            }
        }
    }

    /**
     * Replaces the bodybuilding plan and sets its days, all or nothing. The days are set last, inside the plan's
     * transaction: a failed days write rolls the plan back, and a failed plan write never reaches the days. If the
     * transaction still fails once the days are stored (a store that wrote and then threw, or a failed commit), the
     * side's days are put back as they were.
     */
    private suspend fun store(items: List<Pair<PlanItem, String?>>, days: Set<DayOfWeek>) {
        val before = settings.settings.first().bodybuildingTrainingDays
        try {
            training.inTransaction {
                replacePlan(items)
                setDays(days)
            }
        } catch (e: Exception) {
            if (settings.settings.first().bodybuildingTrainingDays != before) runCatching { setDays(before) }
            throw e
        }
    }

    private fun picks(s: PlanBuilderState): List<BuilderPick> = s.ticked.values.map { it.pick }

    private fun build(s: PlanBuilderState, variant: Int): PlanDraft =
        PlanBuilder.build(BuilderAnswers(s.days.sorted(), picks(s), s.priorities, variant))

    companion object {
        /** A stored exercise as the browser lists it: the catalogue's entry when its name is the catalogue's. */
        fun entryFor(e: Exercise): PickerEntry {
            val lib = Library.all().firstOrNull { it.name.equals(e.name, ignoreCase = true) }
            return if (lib != null) PickerEntry(lib.name, lib.muscles, lib.kind, lib, e.id)
            else PickerEntry(e.name, e.muscles, if (e.category == Category.ACCESSORY) Kind.ACCESSORY else Kind.COMPOUND, null, e.id)
        }

        fun chosen(entry: PickerEntry): ChosenPick {
            val lib = entry.library
            val pick = BuilderPick(entry.name, entry.muscles, entry.kind == Kind.COMPOUND, lib?.equipment.orEmpty(), inCatalogue = lib != null)
            return ChosenPick(pick, if (lib == null) entry.exerciseId else null, entry)
        }

        fun factory(training: TrainingRepository, settings: SettingsRepository, today: () -> LocalDate = ::systemToday): ViewModelProvider.Factory =
            viewModelFactory { initializer { PlanBuilderViewModel(training, settings, today = today) } }
    }
}
