package com.powertracker.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.platform.systemToday
import com.powertracker.domain.progress.Pr
import com.powertracker.domain.progress.ProgressMath
import com.powertracker.domain.progress.ProgressMath.isCompletedMainSet
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

/** [index] counts from 1 within the exercise in that session. */
data class HistorySet(val index: Int, val set: LoggedSet, val isPr: Boolean)

/** The sets of one exercise in a session: the lift itself or one of its variations. */
data class HistoryGroup(val exerciseName: String, val isVariation: Boolean, val sets: List<HistorySet>)

data class HistorySession(val sessionId: Long, val date: LocalDate, val groups: List<HistoryGroup>)

data class LiftHistoryState(
    val loading: Boolean = true,
    val currentE1rmKg: Double? = null,
    val best: Pr? = null,
    /** Newest first. */
    val sessions: List<HistorySession> = emptyList(),
)

class LiftHistoryViewModel(
    training: TrainingRepository,
    lift: MainLift,
    private val today: () -> LocalDate = ::systemToday,
) : ViewModel() {

    val state: StateFlow<LiftHistoryState> = training.history()
        .map { build(it, lift, today()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiftHistoryState())

    companion object {
        /** Every session with a set of [lift] or one of its variations, newest first, with the lift's records marked. */
        internal fun build(history: TrainingHistory, lift: MainLift, today: LocalDate): LiftHistoryState {
            // Walks the sets in the order ProgressMath.prs does, so each record lands on the set that made it.
            val prs = ProgressMath.prs(history, lift)
            var nextPr = 0
            val sessions = history.sessions.sortedBy { it.date }.mapNotNull { session ->
                val groups = LinkedHashMap<Long, MutableList<HistorySet>>()
                for (set in session.sets) {
                    if (history.exercises[set.exerciseId]?.mainLift != lift) continue
                    val pr = prs.getOrNull(nextPr)
                    val isPr = pr != null && history.isCompletedMainSet(set, lift) &&
                        pr.date == session.date && pr.weightKg == set.weightKg && pr.reps == set.reps && pr.rpe == set.rpe
                    if (isPr) nextPr++
                    val sets = groups.getOrPut(set.exerciseId) { mutableListOf() }
                    sets += HistorySet(sets.size + 1, set, isPr)
                }
                if (groups.isEmpty()) return@mapNotNull null
                HistorySession(
                    session.id,
                    session.date,
                    groups.map { (exerciseId, sets) ->
                        val exercise = history.exercises.getValue(exerciseId)
                        HistoryGroup(exercise.name, !exercise.isMainLift, sets)
                    }.sortedBy { it.isVariation }, // the lift itself first; sortedBy is stable
                )
            }
            return LiftHistoryState(
                loading = false,
                currentE1rmKg = HistoryStats(history).currentE1rm(lift, today),
                best = prs.lastOrNull(),
                sessions = sessions.reversed(),
            )
        }

        fun factory(training: TrainingRepository, lift: MainLift, today: () -> LocalDate = ::systemToday): ViewModelProvider.Factory =
            viewModelFactory { initializer { LiftHistoryViewModel(training, lift, today) } }
    }
}

data class ExerciseHistoryState(
    val loading: Boolean = true,
    /** Null until loaded, or for an exercise that is not there. */
    val name: String? = null,
    /** Newest first. */
    val sessions: List<HistorySession> = emptyList(),
)

/** One exercise's history, from both sides: what a bodybuilding Progress row opens when the exercise is not a main lift's. */
class ExerciseHistoryViewModel(training: TrainingRepository, exerciseId: Long) : ViewModel() {
    val state: StateFlow<ExerciseHistoryState> = training.history()
        .map { build(it, exerciseId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseHistoryState())

    companion object {
        /** Every session with a set of [exerciseId], whichever side it was logged on, newest first. */
        internal fun build(history: TrainingHistory, exerciseId: Long): ExerciseHistoryState {
            val name = history.exercises[exerciseId]?.name
            val sessions = history.sessions.sortedWith(compareByDescending<com.powertracker.domain.model.LoggedSession> { it.date }.thenByDescending { it.id })
                .mapNotNull { session ->
                    val sets = session.sets.filter { it.exerciseId == exerciseId }
                    if (sets.isEmpty()) return@mapNotNull null
                    HistorySession(session.id, session.date, listOf(HistoryGroup(name.orEmpty(), false, sets.mapIndexed { i, set -> HistorySet(i + 1, set, isPr = false) })))
                }
            return ExerciseHistoryState(loading = false, name = name, sessions = sessions)
        }

        fun factory(training: TrainingRepository, exerciseId: Long): ViewModelProvider.Factory =
            viewModelFactory { initializer { ExerciseHistoryViewModel(training, exerciseId) } }
    }
}
