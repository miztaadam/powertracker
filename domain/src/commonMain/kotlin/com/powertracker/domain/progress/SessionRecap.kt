package com.powertracker.domain.progress

import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.LocalDate
import kotlin.math.round

/** Post-workout recap numbers computed from a session's logged sets. */
data class SessionRecap(
    /** Total kg moved across completed kg sets (`sum(weightKg * reps)`). Plate-counted sets are excluded. */
    val tonnageKg: Double,
    /** Number of sets in this session that set a new personal record against prior history. */
    val prCount: Int,
)

/** Computes session recap stats (tonnage moved and PRs hit) for the Finish summary card. */
object SessionRecapMath {
    fun of(
        historyBeforeToday: TrainingHistory,
        todaysSets: List<LoggedSet>,
        today: LocalDate,
        mode: TrainingMode,
    ): SessionRecap {
        val tonnage = todaysSets
            .filter { !it.failed && it.unit == LoadUnit.KG && it.weightKg > 0.0 && it.reps > 0 }
            .sumOf { it.weightKg * it.reps }
        // Count distinct exercises (or sets) that beat prior history; each set that beats the running best counts once per exercise.
        var prs = 0
        val byExercise = todaysSets.groupBy { it.exerciseId }
        for ((_, sets) in byExercise) {
            if (sets.any { ProgressMath.isNewPr(historyBeforeToday, it, today, mode) }) {
                prs++
            }
        }
        return SessionRecap(
            tonnageKg = round(tonnage * 10.0) / 10.0,
            prCount = prs,
        )
    }
}
