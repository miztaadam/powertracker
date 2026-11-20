package com.powertracker.domain.progress

import com.powertracker.domain.e1rm.E1rm
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.on
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

    /**
     * Whether [set], about to be logged on [date] in a session on the [mode] side, is a new record under exactly the
     * rule of [prs]: a completed set of a main lift itself whose e1RM beats every earlier one of that lift. [history] is
     * everything stored before it; only the powerlifting side's sessions up to [date] count, as in Lift history.
     * As in [prs], the first completed set of a lift is its first record. Variations, accessories, failed sets and the
     * bodybuilding side never are.
     */
    fun isNewPr(history: TrainingHistory, set: LoggedSet, date: LocalDate, mode: TrainingMode): Boolean {
        if (mode != TrainingMode.POWERLIFTING) return false
        val lift = history.exercises[set.exerciseId]?.takeIf { it.isMainLift }?.mainLift ?: return false
        if (!history.isCompletedMainSet(set, lift)) return false
        val sides = history.on(TrainingMode.POWERLIFTING)
        val before = sides.copy(sessions = sides.sessions.filter { it.date <= date })
        // The set walks last (a later session on the same date sorts after the stored ones), so it adds a record only
        // when it is one.
        val after = before.copy(sessions = before.sessions + LoggedSession(-1, date, null, null, listOf(set), TrainingMode.POWERLIFTING))
        return prs(after, lift).size > prs(before, lift).size
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

    /**
     * A lift's known best, as Plan's Known bests shows it (1.16.1): the higher of the latest typed baseline of the lift's
     * own exercises and its best logged e1RM ever, the last of [prs], which Progress's Records shows. Only the
     * powerlifting side's sessions count, as on Progress. Null when there is neither.
     */
    fun knownBest(history: TrainingHistory, lift: MainLift): Double? {
        val typed = history.baselines
            .filter { b -> history.exercises[b.exerciseId]?.let { it.isMainLift && it.mainLift == lift } == true }
            // The latest; of two typed on one day (two rows of the lift), the higher.
            .maxWithOrNull(compareBy({ it.date }, { it.e1rmKg }))
            ?.e1rmKg
        val lifted = prs(history.on(TrainingMode.POWERLIFTING), lift).lastOrNull()?.e1rmKg
        return if (typed == null || lifted == null) typed ?: lifted else maxOf(typed, lifted)
    }

    private val SBD = listOf(MainLift.SQUAT, MainLift.BENCH, MainLift.DEADLIFT)

    /**
     * Squat + bench + deadlift current e1RM as of [until], plus strict press when [includeStrictPress]; null until
     * every included lift has one.
     */
    fun total(stats: HistoryStats, until: LocalDate, includeStrictPress: Boolean): Double? {
        val included = if (includeStrictPress) SBD + MainLift.STRICT_PRESS else SBD
        val lifts = included.map { stats.currentE1rm(it, until) ?: return null }
        return lifts.sum()
    }
}
