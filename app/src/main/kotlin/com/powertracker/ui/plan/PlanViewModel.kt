package com.powertracker.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.validation.InputValidator
import com.powertracker.platform.systemToday
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
    /** The built-in SQUAT / BENCH / DEADLIFT exercises, for "Set a known best". */
    val mainExercises: List<Exercise> = emptyList(),
    val defaultRpe: Double = 8.0,
)

class PlanViewModel(
    private val training: TrainingRepository,
    settings: SettingsRepository,
    private val today: () -> LocalDate = ::systemToday,
) : ViewModel() {

    val state: StateFlow<PlanUiState> = combine(training.plan(), training.exercises(), settings.settings) { plan, exercises, s ->
        build(plan, exercises, s)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    private fun build(plan: List<PlanItem>, exercises: List<Exercise>, s: AppSettings): PlanUiState {
        val names = exercises.associate { it.id to it.name }
        val byDay = plan.groupBy { it.weekday }
        val days = (s.trainingDays + byDay.keys).sorted()
        return PlanUiState(
            sections = days.map { day ->
                DaySection(day, byDay[day].orEmpty().sortedBy { it.order }.map { PlanRow(it, names[it.exerciseId] ?: "Unknown exercise") })
            },
            exercises = exercises.filter { !it.archived },
            mainExercises = exercises.filter { it.category in MAIN_CATEGORIES },
            defaultRpe = s.defaultRpe,
        )
    }

    /** Adds at the end of the day when [existing] is null, otherwise updates it in place. */
    fun save(day: DayOfWeek, existing: PlanItem?, exerciseId: Long, sets: Int, reps: Int, rpe: Double) {
        viewModelScope.launch {
            if (existing != null) {
                training.savePlanItem(existing.copy(exerciseId = exerciseId, sets = sets, reps = reps, targetRpe = rpe))
            } else {
                val next = (state.value.sections.firstOrNull { it.day == day }?.rows?.maxOfOrNull { it.item.order } ?: -1) + 1
                training.savePlanItem(PlanItem(0, day, exerciseId, sets, reps, rpe, next))
            }
        }
    }

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

    /** Creates a custom exercise and reports its id. */
    fun addCustomExercise(name: String, category: Category, parentLift: MainLift?, muscles: List<String>, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = training.addExercise(Exercise(0, name.trim(), category, parentLift, muscles, isBuiltIn = false))
            onCreated(id)
        }
    }

    /** Ignores values the dialog would not accept (non-finite, 0 or below, above 500 kg). */
    fun setBaseline(mainExerciseId: Long, e1rmKg: Double) {
        if (!InputValidator.isValidBaselineKg(e1rmKg)) return
        viewModelScope.launch { training.setBaseline(Baseline(mainExerciseId, e1rmKg, today())) }
    }

    companion object {
        private val MAIN_CATEGORIES = setOf(Category.SQUAT, Category.BENCH, Category.DEADLIFT)

        fun factory(training: TrainingRepository, settings: SettingsRepository, today: () -> LocalDate = ::systemToday): ViewModelProvider.Factory =
            viewModelFactory { initializer { PlanViewModel(training, settings, today) } }
    }
}
