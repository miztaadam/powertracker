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
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Next-session load/rep target for one plan item, from the last top set. */
object ProgressionRule {
    private const val MAX_INCREASE = 1.05
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
        val step = settings.changePlates
        val cap = last.weightKg * MAX_INCREASE

        var reps = item.reps
        val weight = when (val change = change(last, item.targetRpe, exercise, previous, lastDate)) {
            is Change.Scale -> PlateLoader.round(last.weightKg * change.factor, step)
            Change.Repeat -> PlateLoader.round(last.weightKg, step)
            is Change.Add -> {
                var w = PlateLoader.round(minOf(last.weightKg + change.kg, cap), step)
                if (w > cap) w = PlateLoader.round(w - PlateLoader.step(step), step)
                if (w <= last.weightKg) {
                    reps = minOf(last.reps + 1, MAX_REPS)
                    last.weightKg
                } else {
                    w
                }
            }
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
        if (last.failed) return Change.Scale(0.95)
        val delta = (last.rpe ?: targetRpe) - targetRpe
        return when {
            delta >= 1.5 -> Change.Scale(0.975)
            delta > 0.5 -> Change.Repeat
            delta >= -0.5 -> if (recentJumpWithRisingRpe(last, previous, lastDate)) Change.Repeat else Change.Add(2.5)
            delta <= -1.0 &&
                (exercise.category == Category.SQUAT || exercise.category == Category.DEADLIFT) -> Change.Add(5.0)
            else -> Change.Add(2.5)
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
            ChronoUnit.DAYS.between(prevDate, lastDate) in 0..RECENT_JUMP_DAYS &&
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

    private fun fmt(v: Double): String = if (v == Math.rint(v)) v.toLong().toString() else v.toString()
}
