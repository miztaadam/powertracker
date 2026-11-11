package com.powertracker.domain.progress

import com.powertracker.domain.e1rm.E1rm
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.time.mondayOfWeek
import kotlinx.datetime.LocalDate

/** A set whose e1RM beat every earlier set of the same main lift. */
data class Pr(val date: LocalDate, val e1rmKg: Double, val weightKg: Double, val reps: Int, val rpe: Double?)

/** The Monday of the ISO week [date] falls in. */
fun weekStart(date: LocalDate): LocalDate = date.mondayOfWeek()

/** The figures the Progress screens derive from history. Pure, shared with the web app. */
object ProgressMath {
    /** True for a completed set of the competition lift itself: variations and failed sets never count. */
    fun TrainingHistory.isCompletedMainSet(set: LoggedSet, lift: MainLift): Boolean =
        !set.failed && exercises[set.exerciseId]?.let { it.isMainLift && it.mainLift == lift } == true

    /**
     * Every personal record of [lift], oldest first: each completed main-lift set whose e1RM beats
     * all earlier ones (earlier sessions, or earlier in the same session).
     */
    fun prs(history: TrainingHistory, lift: MainLift): List<Pr> {
        val prs = mutableListOf<Pr>()
        var best = 0.0
        for (session in history.sessions.sortedBy { it.date }) {
            for (set in session.sets) {
                if (!history.isCompletedMainSet(set, lift)) continue
                val e1rm = E1rm.estimate(set.weightKg, set.reps, set.rpe)
                if (e1rm > best) {
                    best = e1rm
                    prs += Pr(session.date, e1rm, set.weightKg, set.reps, set.rpe)
                }
            }
        }
        return prs
    }

    /** Σ weight × reps of completed main-lift sets per ISO week (keyed by its Monday), oldest first. Weeks without such sets are left out. */
    fun weeklyVolume(history: TrainingHistory, lift: MainLift): List<Pair<LocalDate, Double>> {
        val volume = mutableMapOf<LocalDate, Double>()
        for (session in history.sessions) {
            for (set in session.sets) {
                if (history.isCompletedMainSet(set, lift)) {
                    val week = weekStart(session.date)
                    volume[week] = (volume[week] ?: 0.0) + set.weightKg * set.reps
                }
            }
        }
        return volume.toList().sortedBy { it.first }
    }

    /** Squat + bench + deadlift current e1RM as of [until]; null until all three have one. */
    fun total(stats: HistoryStats, until: LocalDate): Double? {
        val lifts = MainLift.entries.map { stats.currentE1rm(it, until) ?: return null }
        return lifts.sum()
    }
}
