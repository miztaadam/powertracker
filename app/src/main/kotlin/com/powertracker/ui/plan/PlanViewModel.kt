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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    /** The built-in SQUAT / BENCH / DEADLIFT exercises, for "Set a known best". */
    val mainExercises: List<Exercise> = emptyList(),
    val defaultRpe: Double = 8.0,
)

class PlanViewModel(
    private val training: TrainingRepository,
    settings: SettingsRepository,
    private val today: () -> LocalDate = ::systemToday,
) : ViewModel() {

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
            mainExercises = exercises.filter { it.category in MAIN_CATEGORIES },
            defaultRpe = s.defaultRpe,
        )
    }

    /**
     * Adds what was picked at the end of [day]: 3 × 5 for a compound lift, 3 × 10 for an accessory,
     * at the default RPE. A catalogue exercise gets its row, if it has none yet, in the same transaction.
     */
    fun add(day: DayOfWeek, entry: PickerEntry) {
        val item = PlanItem(0, day, entry.exerciseId ?: 0, entry.defaultSets, entry.defaultReps, state.value.defaultRpe.coerceIn(MIN_RPE, MAX_RPE), nextOrder(day))
        viewModelScope.launch { training.savePlanItem(item, entry.catalogueName) }
    }

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
        private val MAIN_CATEGORIES = setOf(Category.SQUAT, Category.BENCH, Category.DEADLIFT)

        fun factory(training: TrainingRepository, settings: SettingsRepository, today: () -> LocalDate = ::systemToday): ViewModelProvider.Factory =
            viewModelFactory { initializer { PlanViewModel(training, settings, today) } }
    }
}
