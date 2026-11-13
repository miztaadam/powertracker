package com.powertracker.domain.coach

import com.powertracker.domain.e1rm.E1rm
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.on
import com.powertracker.domain.model.repsText
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * Coming back to a side after time on the other. Applies only while the other side has a session in the last
 * [AWAY_DAYS] days, so a log kept on one side alone is coached exactly as before. Then, for an exercise whose
 * family has no set on this side in those days:
 * - trained on the other side in them: the target is capped by what those sets support at the target's reps and
 *   RPE (the e1RM estimate, rounded down to a loadable weight); when that cap lowers the weight, the reason gains
 *   the easing-back-in note so the lifter sees why it is lighter;
 * - not trained on either side: the existing re-entry reduction of the last top set on this side, at the planned
 *   reps, never the old top set plus an increase.
 * The family is a main lift and its variations on the powerlifting side, the exercise itself on the bodybuilding
 * side. The result is never heavier than [ease]'s incoming target.
 */
internal object ReturnRule {
    const val AWAY_DAYS = 28L

    private const val SUGGESTION = "Coach suggests "

    /** [target] eased for a side returned to; unchanged when the exercise was trained on [mode] within AWAY_DAYS. */
    fun ease(
        target: Advice,
        item: PlanItem,
        exercise: Exercise,
        history: TrainingHistory,
        mode: TrainingMode,
        settings: CoachSettings,
        today: LocalDate,
    ): Advice {
        val payload = target.payload as? AdvicePayload.Target ?: return target
        val from = today.minus(AWAY_DAYS, DateTimeUnit.DAY)
        val recent = history.sessions.filter { it.date >= from && it.date < today }
        val (here, other) = recent.partition { it.mode == mode }
        if (other.isEmpty()) return target

        val family = family(exercise, mode, history)
        fun familySets(sessions: List<LoggedSession>) = sessions.flatMap { it.sets }.filter { it.exerciseId in family }
        if (familySets(here).isNotEmpty()) return target

        val plates = settings.changePlates
        val elsewhere = familySets(other).filter { !it.failed }
        if (elsewhere.isNotEmpty()) {
            val best = elsewhere.maxOf { E1rm.estimate(it.weightKg, it.reps, it.rpe) }
            val supported = E1rm.loadFor(best, payload.reps, payload.rpe)
            val free = ProgressionRule.isFreeLoad(exercise, payload.weightKg)
            val cap = ProgressionRule.roundDown(supported, payload.weightKg, free, plates)
            if (cap >= payload.weightKg) return target
            return target.copy(
                reason = withSuggestion(target.reason, cap, payload.reps, payload.repsHigh, payload.rpe) + Coach.RE_ENTRY_NOTE,
                payload = payload.copy(weightKg = cap),
            )
        }

        val (_, last) = HistoryStats(history.on(mode)).lastTopSet(item.exerciseId, today) ?: return target
        val factor = AdvicePayload.ReEntry().loadFactor
        val free = ProgressionRule.isFreeLoad(exercise, last.weightKg)
        val weight = minOf(payload.weightKg, ProgressionRule.roundDown(last.weightKg * factor, last.weightKg, free, plates))
        val reps = item.reps
        return target.copy(
            reason = withSuggestion(target.reason, weight, reps, payload.repsHigh, payload.rpe) + Coach.RE_ENTRY_NOTE,
            payload = payload.copy(weightKg = weight, reps = reps),
        )
    }

    /** The exercise ids that count as training [exercise] on [mode]. */
    private fun family(exercise: Exercise, mode: TrainingMode, history: TrainingHistory): Set<Long> {
        val lift = exercise.mainLift
        if (mode != TrainingMode.POWERLIFTING || lift == null) return setOf(exercise.id)
        return history.exercises.values.filter { it.mainLift == lift }.map { it.id }.toSet() + exercise.id
    }

    /** [reason] with its closing "Coach suggests ..." sentence rewritten for the new weight and reps. */
    internal fun withSuggestion(reason: String, weightKg: Double, reps: Int, repsHigh: Int?, rpe: Double): String {
        val at = reason.lastIndexOf(SUGGESTION)
        val prefix = if (at < 0) (if (reason.isEmpty()) "" else "$reason ") else reason.substring(0, at)
        return "$prefix$SUGGESTION${ProgressionRule.fmt(weightKg)} × ${repsText(reps, repsHigh)} @ ${ProgressionRule.fmt(rpe)}."
    }
}
