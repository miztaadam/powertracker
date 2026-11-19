package com.powertracker.domain.coach

import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.plates.PlateLoader
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.math.floor

/** Next-session load/rep target for one plan item, from the last top set. */
object ProgressionRule {
    /** A target is never more than this times the last top set's weight. */
    internal const val MAX_INCREASE = 1.05

    /** A missed top set: the next target is this fraction of its weight. */
    internal const val FAILED_SCALE = 0.95

    /** A top set 1.5 RPE or more above target: the next target is this fraction of its weight. */
    internal const val HARD_SCALE = 0.975

    /** The usual increase, in kilograms. */
    internal const val STEP_KG = 2.5

    /** The increase for a squat or deadlift top set at least 1 RPE under target. */
    internal const val BIG_STEP_KG = 5.0
    private const val MAX_REPS = 12
    private const val RECENT_JUMP_DAYS = 14L

    /** null when the exercise has no prior top set. */
    fun target(
        item: PlanItem,
        exercise: Exercise,
        stats: HistoryStats,
        settings: CoachSettings,
        today: LocalDate,
    ): Advice? {
        val (lastDate, last) = stats.lastTopSet(item.exerciseId, today) ?: return null
        val previous = stats.previousTopSet(item.exerciseId, today)
        val plates = settings.changePlates
        val free = isFreeLoad(exercise, last.weightKg)
        val cap = last.weightKg * MAX_INCREASE
        fun loadable(kg: Double) = round(kg, last.weightKg, free, plates)

        var reps = item.reps
        val change = change(last, item.targetRpe, exercise, previous, lastDate)
        var weight = when (change) {
            is Change.Scale -> loadable(last.weightKg * change.factor)
            Change.Repeat -> loadable(last.weightKg)
            is Change.Add -> loadable(minOf(last.weightKg + change.kg, cap))
        }
        // Rounding can land above the 5% cap: step down until it does not.
        while (weight > cap && weight > 0.0) weight = round(weight - PlateLoader.step(plates), last.weightKg, free, plates)
        if (change is Change.Add && weight <= last.weightKg) {
            reps = minOf(last.reps + 1, MAX_REPS)
            weight = last.weightKg
        }

        val reason = describe(last, weight, reps, item.targetRpe)
        return Advice(
            type = AdviceType.TARGET,
            lift = exercise.mainLift,
            createdAt = today,
            reason = reason,
            severity = 0,
            payload = AdvicePayload.Target(item.exerciseId, weight, reps, item.targetRpe),
        )
    }

    /**
     * [target] (progression's own) while a pain skip holds the exercise ([SkipCoach.held]): the hold blocks increases
     * only. The held aim is the last top set's weight, and its reps when they were above the plan's (reps added by
     * progression), else the planned reps. A target above that aim (more weight, or more reps at the same weight) is
     * replaced by it; a decrease or a repeat is kept as it is.
     */
    fun held(target: Advice, item: PlanItem, stats: HistoryStats, today: LocalDate): Advice {
        val (_, last) = stats.lastTopSet(item.exerciseId, today) ?: return target
        val payload = target.payload as AdvicePayload.Target
        val reps = maxOf(item.reps, last.reps)
        val increase = payload.weightKg > last.weightKg || (payload.weightKg == last.weightKg && payload.reps > reps)
        if (!increase) return target
        return target.copy(
            reason = describe(last, last.weightKg, reps, item.targetRpe),
            payload = payload.copy(weightKg = last.weightKg, reps = reps),
        )
    }

    /** Accessories, and anything logged below the empty bar, are not loaded on a barbell: no bar floor. */
    internal fun isFreeLoad(exercise: Exercise, lastKg: Double): Boolean =
        exercise.category == Category.ACCESSORY || lastKg < PlateLoader.BAR_KG

    /** Nearest loadable weight: on the bar's plate grid, or for a [free] load on [anchorKg]'s own grid. */
    private fun round(kg: Double, anchorKg: Double, free: Boolean, changePlates: Boolean): Double =
        if (free) PlateLoader.roundFrom(anchorKg, kg, changePlates) else PlateLoader.round(kg, changePlates)

    /** Nearest loadable weight not above [kg] (never below the empty bar unless [free], never below 0). */
    internal fun roundDown(kg: Double, anchorKg: Double, free: Boolean, changePlates: Boolean): Double {
        val rounded = round(kg, anchorKg, free, changePlates)
        if (rounded <= kg) return rounded
        return maxOf(0.0, round(rounded - PlateLoader.step(changePlates), anchorKg, free, changePlates))
    }

    private sealed interface Change {
        data class Scale(val factor: Double) : Change
        data object Repeat : Change
        data class Add(val kg: Double) : Change
    }

    private fun change(
        last: LoggedSet,
        targetRpe: Double,
        exercise: Exercise,
        previous: Pair<LocalDate, LoggedSet>?,
        lastDate: LocalDate,
    ): Change {
        if (last.failed) return Change.Scale(FAILED_SCALE)
        val delta = (last.rpe ?: targetRpe) - targetRpe
        return when {
            delta >= 1.5 -> Change.Scale(HARD_SCALE)
            delta > 0.5 -> Change.Repeat
            delta >= -0.5 -> if (recentJumpWithRisingRpe(last, previous, lastDate)) Change.Repeat else Change.Add(STEP_KG)
            // Bench, strict press and everything else jump 2.5 kg.
            delta <= -1.0 &&
                (exercise.category == Category.SQUAT || exercise.category == Category.DEADLIFT) -> Change.Add(BIG_STEP_KG)
            else -> Change.Add(STEP_KG)
        }
    }

    private fun recentJumpWithRisingRpe(
        last: LoggedSet,
        previous: Pair<LocalDate, LoggedSet>?,
        lastDate: LocalDate,
    ): Boolean {
        val (prevDate, prev) = previous ?: return false
        val lastRpe = last.rpe ?: return false
        val prevRpe = prev.rpe ?: return false
        return prev.weightKg < last.weightKg &&
            prevDate.daysUntil(lastDate) in 0..RECENT_JUMP_DAYS &&
            lastRpe > prevRpe
    }

    /** The TARGET reason text: what was done last time and what the coach suggests now. */
    internal fun describe(last: LoggedSet, weightKg: Double, reps: Int, rpe: Double): String =
        "${lastTime(last)} Coach suggests ${fmt(weightKg)} × $reps @ ${fmt(rpe)}."

    private fun lastTime(last: LoggedSet): String {
        val base = "Last time: ${fmt(last.weightKg)} × ${last.reps}"
        return when {
            last.failed -> "$base (missed)."
            last.rpe == null -> "$base."
            else -> "$base @ ${fmt(last.rpe)}."
        }
    }

    internal fun fmt(v: Double): String = if (v == floor(v)) v.toLong().toString() else v.toString()
}
