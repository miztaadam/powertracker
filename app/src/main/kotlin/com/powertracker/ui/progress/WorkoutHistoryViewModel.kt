package com.powertracker.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.components.formatKg
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

/** One exercise of a workout with every set of it, in the order logged. */
data class WorkoutExercise(val exerciseId: Long, val name: String, val sets: List<LoggedSet>)

/** One past workout: a session with at least one set. [setCount] is over all its exercises. */
data class Workout(
    val sessionId: Long,
    val date: LocalDate,
    val side: TrainingMode,
    val bodyweightKg: Double?,
    val exercises: List<WorkoutExercise>,
    val setCount: Int,
) {
    /** "Bodybuilding · 3 exercises · 9 sets", and " · 82.5 kg bodyweight" when one was logged that day. The web app's words. */
    val summary: String
        get() = listOfNotNull(
            if (side == TrainingMode.BODYBUILDING) "Bodybuilding" else "Powerlifting",
            count(exercises.size, "exercise"),
            count(setCount, "set"),
            bodyweightKg?.let { "${formatKg(it)} kg bodyweight" },
        ).joinToString(" · ")
}

internal fun count(n: Int, one: String) = "$n ${if (n == 1) one else "${one}s"}"

data class WorkoutHistoryState(
    val loading: Boolean = true,
    /** Newest first. */
    val workouts: List<Workout> = emptyList(),
)

/** Every past workout, from both sides (1.20, as the web app's History since 1.19): what Progress's History button opens. */
class WorkoutHistoryViewModel(training: TrainingRepository) : ViewModel() {
    val state: StateFlow<WorkoutHistoryState> = training.history()
        .map { build(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutHistoryState())

    companion object {
        /** A set whose exercise has no row. Not expected: a set's exercise cannot be deleted (the database refuses it). */
        private const val UNKNOWN_EXERCISE = "Exercise"

        /**
         * Every workout of the log, newest first, from both sides: each session that has a set, with its exercises in
         * the order each was first logged and every set of an exercise together, in logged order (an exercise come back
         * to later in the session is still one group). A session with no sets is no workout: it is left out. An archived
         * exercise keeps its name. The same list as the web app's `buildWorkoutHistory`.
         */
        internal fun build(history: TrainingHistory): WorkoutHistoryState {
            val workouts = history.sessions
                .filter { it.sets.isNotEmpty() }
                .sortedWith(compareByDescending<LoggedSession> { it.date }.thenByDescending { it.id })
                .map { session ->
                    val groups = LinkedHashMap<Long, MutableList<LoggedSet>>()
                    for (set in session.sets) groups.getOrPut(set.exerciseId) { mutableListOf() } += set
                    Workout(
                        sessionId = session.id,
                        date = session.date,
                        side = session.mode,
                        bodyweightKg = session.bodyweightKg,
                        exercises = groups.map { (id, sets) -> WorkoutExercise(id, history.exercises[id]?.name ?: UNKNOWN_EXERCISE, sets) },
                        setCount = session.sets.size,
                    )
                }
            return WorkoutHistoryState(loading = false, workouts = workouts)
        }

        fun factory(training: TrainingRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { WorkoutHistoryViewModel(training) } }
    }
}
