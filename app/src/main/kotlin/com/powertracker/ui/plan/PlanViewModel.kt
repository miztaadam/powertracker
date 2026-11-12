package com.powertracker.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.library.Kind
import com.powertracker.domain.library.Library
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.validation.InputValidator
import com.powertracker.platform.systemToday
import com.powertracker.ui.library.PickerEntry
import com.powertracker.ui.library.recentExerciseIds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

data class PlanRow(val item: PlanItem, val exerciseName: String)

data class DaySection(val day: DayOfWeek, val rows: List<PlanRow>)

data class PlanUiState(
    val sections: List<DaySection> = emptyList(),
    /** Non-archived exercises, for the picker. */
    val exercises: List<Exercise> = emptyList(),
    /** Exercise ids, most recently logged first, for the picker's "Your exercises". */
    val recentIds: List<Long> = emptyList(),
    /** The main lifts' own exercises (squat, bench, deadlift and, once it has a row, strict press), for "Set a known best". */
    val mainExercises: List<Exercise> = emptyList(),
    val defaultRpe: Double = 8.0,
    /** The days the lifter trains, from Settings; a day can have plan items without being one. */
    val trainingDays: Set<DayOfWeek> = emptySet(),
)

/** How adding an exercise from the Exercises tab went. */
enum class PlanAdd {
    ADDED,

    /** The exercise is already on that day; nothing was added. */
    ALREADY_THERE,

    /** The write failed; nothing was changed. */
    FAILED,
}

class PlanViewModel(
    private val training: TrainingRepository,
    private val settings: SettingsRepository,
    private val today: () -> LocalDate = ::systemToday,
) : ViewModel() {

    /** True while [addOnce] is saving; further calls are ignored until it has answered. */
    private val adding = MutableStateFlow(false)

    val state: StateFlow<PlanUiState> = combine(training.plan(), training.history(), settings.settings) { plan, history, s ->
        build(plan, history, s)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    private fun build(plan: List<PlanItem>, history: TrainingHistory, s: AppSettings): PlanUiState {
        val exercises = history.exercises.values.sortedBy { it.id }
        val names = exercises.associate { it.id to it.name }
        val byDay = plan.groupBy { it.weekday }
        val days = (s.trainingDays + byDay.keys).sorted()
        return PlanUiState(
            sections = days.map { day ->
                DaySection(day, byDay[day].orEmpty().sortedBy { it.order }.map { PlanRow(it, names[it.exerciseId] ?: "Unknown exercise") })
            },
            exercises = exercises.filter { !it.archived },
            recentIds = recentExerciseIds(history),
            mainExercises = exercises.filter { it.isMainLift },
            defaultRpe = s.defaultRpe,
            trainingDays = s.trainingDays,
        )
    }

    /**
     * Adds what was picked at the end of [day]: 3 × 5 for a compound lift, 3 × 10 for an accessory,
     * at the default RPE. A catalogue exercise gets its row, if it has none yet, in the same transaction.
     */
    fun add(day: DayOfWeek, entry: PickerEntry) {
        val item = newItem(day, entry, state.value.defaultRpe, nextOrder(day))
        viewModelScope.launch { training.savePlanItem(item, entry.catalogueName) }
    }

    /**
     * [add] for the Exercises tab, where the day is chosen after the exercise: the same defaults and the
     * same single transaction, but an exercise already on [day] is left alone, and [onResult] hears how
     * it went, once. A second call before that (a double tap) does nothing. Read from what is stored at
     * the moment of the call, so it does not depend on this view model's screen being shown.
     */
    fun addOnce(day: DayOfWeek, entry: PickerEntry, onResult: (PlanAdd) -> Unit) {
        if (!adding.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            val result = try {
                // After whatever else this tap set off: a second tap in the same frame meets the guard, however fast the store answers.
                yield()
                val onDay = training.plan().first().filter { it.weekday == day }
                // The row a catalogue name would be saved to (the oldest live one, as CatalogueResolver picks it), if it has one yet.
                val name = entry.catalogueName
                val rowId = if (name == null) entry.exerciseId else training.exercises().first().filter { !it.archived && it.name.equals(name, ignoreCase = true) }.minOfOrNull { it.id }
                if (rowId != null && onDay.any { it.exerciseId == rowId }) {
                    PlanAdd.ALREADY_THERE
                } else {
                    val order = (onDay.maxOfOrNull { it.order } ?: -1) + 1
                    training.savePlanItem(newItem(day, entry.copy(exerciseId = rowId), settings.settings.first().defaultRpe, order), name)
                    PlanAdd.ADDED
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed write must not crash the app: nothing was saved, and the lifter is told.
                PlanAdd.FAILED
            } finally {
                adding.value = false
            }
            onResult(result)
        }
    }

    private fun newItem(day: DayOfWeek, entry: PickerEntry, defaultRpe: Double, order: Int) =
        PlanItem(0, day, entry.exerciseId ?: 0, entry.defaultSets, entry.defaultReps, defaultRpe.coerceIn(MIN_RPE, MAX_RPE), order)

    /** Updates [existing] in place. [exercise] is set when the exercise itself was changed. */
    fun save(existing: PlanItem, exercise: PickerEntry?, sets: Int, reps: Int, rpe: Double) {
        val item = existing.copy(exerciseId = exercise?.exerciseId ?: existing.exerciseId, sets = sets, reps = reps, targetRpe = rpe)
        viewModelScope.launch { training.savePlanItem(item, exercise?.catalogueName) }
    }

    private fun nextOrder(day: DayOfWeek): Int =
        (state.value.sections.firstOrNull { it.day == day }?.rows?.maxOfOrNull { it.item.order } ?: -1) + 1

    fun delete(item: PlanItem) {
        viewModelScope.launch { training.deletePlanItem(item.id) }
    }

    /** Swaps [item] with its neighbour in the given direction (-1 up, +1 down). */
    fun move(item: PlanItem, direction: Int) {
        val rows = state.value.sections.firstOrNull { it.day == item.weekday }?.rows ?: return
        val i = rows.indexOfFirst { it.item.id == item.id }
        val other = rows.getOrNull(i + direction)?.item ?: return
        if (i < 0) return
        viewModelScope.launch {
            training.savePlanItem(item.copy(order = other.order))
            training.savePlanItem(other.copy(order = item.order))
        }
    }

    /**
     * Creates a custom exercise and reports it as a picker entry, ready to [add] or [save]. A name the
     * catalogue knows (a name or an alias, ignoring case) is not made again: the catalogue's entry is
     * reported instead, tied to its oldest live row if it has one, and gets its row like any other
     * catalogue exercise when it is saved.
     */
    fun addCustomExercise(name: String, category: Category, parentLift: MainLift?, muscles: List<String>, onCreated: (PickerEntry) -> Unit) {
        val trimmed = name.trim()
        val known = Library.find(trimmed)
        if (known != null) {
            val row = state.value.exercises.filter { !it.archived && it.name.equals(known.name, ignoreCase = true) }.minByOrNull { it.id }
            onCreated(PickerEntry(known.name, known.muscles, known.kind, known, row?.id))
            return
        }
        viewModelScope.launch {
            val id = training.addExercise(Exercise(0, trimmed, category, parentLift, muscles, isBuiltIn = false))
            onCreated(PickerEntry(trimmed, muscles, if (category == Category.ACCESSORY) Kind.ACCESSORY else Kind.COMPOUND, library = null, exerciseId = id))
        }
    }

    /** Ignores values the dialog would not accept (non-finite, 0 or below, above 500 kg). */
    fun setBaseline(mainExerciseId: Long, e1rmKg: Double) {
        if (!InputValidator.isValidBaselineKg(e1rmKg)) return
        viewModelScope.launch { training.setBaseline(Baseline(mainExerciseId, e1rmKg, today())) }
    }

    companion object {
        const val MIN_RPE = 6.0
        const val MAX_RPE = 10.0

        fun factory(training: TrainingRepository, settings: SettingsRepository, today: () -> LocalDate = ::systemToday): ViewModelProvider.Factory =
            viewModelFactory { initializer { PlanViewModel(training, settings, today) } }
    }
}
