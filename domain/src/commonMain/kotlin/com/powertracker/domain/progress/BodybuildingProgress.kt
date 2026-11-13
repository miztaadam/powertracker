package com.powertracker.domain.progress

import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.LocalDate

/**
 * One exercise of the bodybuilding plan on the Progress screen: its best set from each of its last four bodybuilding
 * sessions (oldest first, with the session's date), and whether it is going up; [goingUp] is null with fewer than
 * two sessions.
 */
data class ExerciseTrend(
    val exerciseId: Long,
    val name: String,
    val bestSets: List<Pair<LocalDate, LoggedSet>>,
    val goingUp: Boolean?,
)

/** The bodybuilding side's Progress rows (spec 7.6). */
object BodybuildingProgress {
    private const val SESSIONS = 4

    /**
     * One row per distinct exercise in the bodybuilding plan, in plan order (weekday, then the day's order), for
     * sessions on or before [today]. Missed sets never count. A session's best set is the one with the most reps at
     * its heaviest weight. Going up: the newest session's heaviest weight is above the one before's, or the same with
     * more reps in total.
     */
    fun rows(history: TrainingHistory, plan: List<PlanItem>, today: LocalDate): List<ExerciseTrend> {
        val sessions = history.sessions
            .filter { it.mode == TrainingMode.BODYBUILDING && it.date <= today }
            .sortedBy { it.date }
        return plan
            .filter { it.mode == TrainingMode.BODYBUILDING }
            .sortedWith(compareBy({ it.weekday }, { it.order }))
            .map { it.exerciseId }
            .distinct()
            .mapNotNull { id ->
                val exercise = history.exercises[id] ?: return@mapNotNull null
                val recent = sessions
                    .mapNotNull { s -> s.sets.filter { it.exerciseId == id && !it.failed }.takeIf { it.isNotEmpty() }?.let { s.date to it } }
                    .takeLast(SESSIONS)
                ExerciseTrend(id, exercise.name, recent.map { (date, sets) -> date to best(sets) }, goingUp(recent.map { it.second }))
            }
    }

    private fun best(sets: List<LoggedSet>): LoggedSet {
        val heaviest = sets.maxOf { it.weightKg }
        return sets.filter { it.weightKg == heaviest }.maxBy { it.reps }
    }

    private fun goingUp(sessions: List<List<LoggedSet>>): Boolean? {
        if (sessions.size < 2) return null
        val (before, latest) = sessions.takeLast(2)
        val a = before.maxOf { it.weightKg }
        val b = latest.maxOf { it.weightKg }
        return b > a || (b == a && latest.sumOf { it.reps } > before.sumOf { it.reps })
    }
}
