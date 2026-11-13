package com.powertracker.domain.coach

import com.powertracker.domain.e1rm.E1rm
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.on
import com.powertracker.domain.model.repsText
import com.powertracker.domain.plates.PlateLoader
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * The bodybuilding side's coach: reps first, then weight (spec 5.3). For each of today's bodybuilding plan items it
 * looks at the exercise's sets in its latest bodybuilding session before today:
 * - every planned set done, none missed, each at the top of the range at or under the target RPE (a set with no RPE
 *   counts as on target): add weight, [INCREASE] of the last weight rounded to a loadable weight and at least one
 *   step more, and aim for the range again from its low end;
 * - otherwise: the same weight, and beat last time's reps;
 * - no bodybuilding session yet: from powerlifting sets of the exercise in the last 28 days, a starting weight for
 *   the middle of the range at the target RPE from the e1RM estimate, rounded down; with none, no advice.
 * Then the return rule, and the deload and easing-back-in clamps the powerlifting targets get (the range is kept).
 * No lift is special here: there is no minimum history and no main-lift rule.
 */
object BodybuildingCoach {
    /** The size of a weight increase, as a fraction of the last weight. Source: [CoachSources.BODYBUILDING_INCREASE]. */
    const val INCREASE: Double = 0.02

    /** The step a weight off the barbell goes up by. */
    private const val FREE_STEP_KG = 0.5

    private const val FIRST_TIME_DAYS = 28L

    private const val INCREASE_TEXT = "Time to add weight."
    private const val REPEAT_TEXT = "Beat it."
    private const val FIRST_TIME_TEXT = "First time on this side."

    /** TARGET advice for [day]'s bodybuilding plan items, in plan order; items with nothing to go on are left out. */
    fun targetsFor(
        day: DayOfWeek,
        history: TrainingHistory,
        plan: List<PlanItem>,
        settings: CoachSettings,
        today: LocalDate,
        priorAdvice: List<Advice>,
    ): List<Advice> {
        val all = HistoryStats(history)
        val side = HistoryStats(history.on(TrainingMode.BODYBUILDING))
        val deload = Coach.activeAdvice(AdviceType.DELOAD, priorAdvice, today)
        val lastSession = all.lastSessionDate(today)
        val firstDayBack = lastSession != null && lastSession.daysUntil(today) >= Coach.GAP_DAYS
        val reEntry = Coach.activeAdvice(AdviceType.RE_ENTRY, priorAdvice, today)
            ?: if (firstDayBack) Advice(AdviceType.RE_ENTRY, null, today, "", 0, AdvicePayload.ReEntry()) else null
        return plan
            .filter { it.weekday == day && it.mode == TrainingMode.BODYBUILDING }
            .sortedBy { it.order }
            .mapNotNull { item ->
                val exercise = history.exercises[item.exerciseId]?.takeUnless { it.archived } ?: return@mapNotNull null
                val target = target(item, exercise, history, settings, today) ?: return@mapNotNull null
                val eased = ReturnRule.ease(target, item, exercise, history, TrainingMode.BODYBUILDING, settings, today)
                clamp(eased, item, exercise, side, all, settings, deload, reEntry)
            }
    }

    /**
     * Reps to put in the reps field for set [setNumber] (1-based): last time's reps for that set + 1, capped at the
     * top of the range; the low end the first time, after an increase, and for a set not done last time.
     */
    fun repsToBeat(item: PlanItem, history: TrainingHistory, setNumber: Int, today: LocalDate): Int {
        val last = lastSets(item.exerciseId, history, today) ?: return item.reps
        if (earnedIncrease(last, item)) return item.reps
        val set = last.getOrNull(setNumber - 1) ?: return item.reps
        return minOf(set.reps + 1, high(item))
    }

    private fun high(item: PlanItem) = item.repsHigh ?: item.reps

    /** The exercise's sets, in logged order, in its latest bodybuilding session before [today]; null if none. */
    private fun lastSets(exerciseId: Long, history: TrainingHistory, today: LocalDate): List<LoggedSet>? =
        history.sessions
            .filter { it.mode == TrainingMode.BODYBUILDING && it.date < today }
            .sortedBy { it.date }
            .lastOrNull { s -> s.sets.any { it.exerciseId == exerciseId } }
            ?.sets?.filter { it.exerciseId == exerciseId }

    private fun earnedIncrease(sets: List<LoggedSet>, item: PlanItem): Boolean =
        sets.size >= item.sets && sets.none { it.failed } &&
            sets.all { it.reps >= high(item) && (it.rpe == null || it.rpe <= item.targetRpe) }

    private fun target(
        item: PlanItem,
        exercise: Exercise,
        history: TrainingHistory,
        settings: CoachSettings,
        today: LocalDate,
    ): Advice? {
        val last = lastSets(item.exerciseId, history, today)
        val (weight, opening) = if (last == null) {
            firstTimeWeight(item, exercise, history, settings, today)?.let { it to FIRST_TIME_TEXT } ?: return null
        } else {
            val heaviest = (last.filter { !it.failed }.ifEmpty { last }).maxOf { it.weightKg }
            if (earnedIncrease(last, item)) {
                increased(heaviest, exercise, settings.changePlates) to "${lastTime(last)} $INCREASE_TEXT"
            } else {
                heaviest to "${lastTime(last)} $REPEAT_TEXT"
            }
        }
        return Advice(
            type = AdviceType.TARGET,
            lift = exercise.mainLift,
            createdAt = today,
            reason = "$opening ${suggestion(weight, item)}",
            severity = 0,
            payload = AdvicePayload.Target(item.exerciseId, weight, item.reps, item.targetRpe, repsHigh = item.repsHigh),
            mode = TrainingMode.BODYBUILDING,
        )
    }

    /** From the best non-missed powerlifting set of the exercise in the last 28 days; null when there is none. */
    private fun firstTimeWeight(
        item: PlanItem,
        exercise: Exercise,
        history: TrainingHistory,
        settings: CoachSettings,
        today: LocalDate,
    ): Double? {
        val from = today.minus(FIRST_TIME_DAYS, DateTimeUnit.DAY)
        val sets = history.sessions
            .filter { it.mode == TrainingMode.POWERLIFTING && it.date >= from && it.date < today }
            .flatMap { it.sets }
            .filter { it.exerciseId == item.exerciseId && !it.failed && it.weightKg > 0.0 }
        val best = sets.maxByOrNull { E1rm.estimate(it.weightKg, it.reps, it.rpe) } ?: return null
        val middle = (item.reps + high(item)) / 2
        val kg = E1rm.loadFor(E1rm.estimate(best.weightKg, best.reps, best.rpe), middle, item.targetRpe)
        val free = ProgressionRule.isFreeLoad(exercise, best.weightKg)
        return ProgressionRule.roundDown(kg, best.weightKg, free, settings.changePlates)
    }

    /**
     * [lastKg] × (1 + [INCREASE]) at the nearest loadable weight (an exact tie goes lighter), never less than one
     * step above [lastKg]: the plate grid's step for a barbell load, [FREE_STEP_KG] on [lastKg]'s own grid otherwise.
     */
    private fun increased(lastKg: Double, exercise: Exercise, changePlates: Boolean): Double {
        val wanted = lastKg * (1 + INCREASE)
        if (ProgressionRule.isFreeLoad(exercise, lastKg)) {
            val step = (FREE_STEP_KG * 100).roundToLong()
            val over = (wanted * 100).roundToLong() - (lastKg * 100).roundToLong()
            val whole = over.floorDiv(step)
            val units = if ((over - whole * step) * 2 > step) whole + 1 else whole
            return ((lastKg * 100).roundToLong() + max(units, 1L) * step) / 100.0
        }
        val rounded = PlateLoader.round(wanted, changePlates)
        return if (rounded > lastKg) rounded else PlateLoader.round(lastKg + PlateLoader.step(changePlates), changePlates)
    }

    /** "Last time: 26 × 11, 10, 9." (one weight), or each set's own weight when they differ; a missed set is marked. */
    private fun lastTime(sets: List<LoggedSet>): String {
        fun reps(s: LoggedSet) = if (s.failed) "${s.reps} (missed)" else "${s.reps}"
        val weights = sets.map { it.weightKg }.distinct()
        val body = if (weights.size == 1) {
            "${ProgressionRule.fmt(weights.single())} × ${sets.joinToString(", ") { reps(it) }}"
        } else {
            sets.joinToString(", ") { "${ProgressionRule.fmt(it.weightKg)} × ${reps(it)}" }
        }
        return "Last time: $body."
    }

    private fun suggestion(weightKg: Double, item: PlanItem) =
        "Coach suggests ${ProgressionRule.fmt(weightKg)} × ${repsText(item.reps, item.repsHigh)} @ ${ProgressionRule.fmt(item.targetRpe)}."

    /**
     * The deload and easing-back-in clamps, as the powerlifting coach applies them: the weight is capped at the
     * advice's load factor of the top set from before it (on this side), a deload also cuts the sets, the range is
     * kept, and the reason gains the same notes.
     */
    private fun clamp(
        target: Advice,
        item: PlanItem,
        exercise: Exercise,
        side: HistoryStats,
        all: HistoryStats,
        settings: CoachSettings,
        deload: Advice?,
        reEntry: Advice?,
    ): Advice {
        if (deload == null && reEntry == null) return target
        val payload = target.payload as AdvicePayload.Target

        fun capFor(advice: Advice, factor: Double): Double {
            val referenceDate = if (advice.type == AdviceType.RE_ENTRY) {
                all.returnDateAfterGap(advice.createdAt)?.takeIf { it < advice.createdAt } ?: advice.createdAt
            } else {
                advice.createdAt
            }
            // The top set on this side from before the advice, else the latest one, else (the first time on this
            // side) the suggested weight itself.
            val reference = (side.lastTopSet(item.exerciseId, referenceDate) ?: side.lastTopSet(item.exerciseId, target.createdAt))
                ?.second?.weightKg ?: payload.weightKg
            val free = ProgressionRule.isFreeLoad(exercise, reference)
            return ProgressionRule.roundDown(reference * factor, reference, free, settings.changePlates)
        }

        var weight = payload.weightKg
        var sets = payload.sets
        var notes = ""
        if (deload != null) {
            val factors = deload.payload as? AdvicePayload.Deload ?: AdvicePayload.Deload()
            weight = minOf(weight, capFor(deload, factors.loadFactor))
            sets = maxOf(1, (item.sets * factors.volumeFactor).roundToInt())
            notes += Coach.DELOAD_NOTE
        }
        if (reEntry != null) {
            val factors = reEntry.payload as? AdvicePayload.ReEntry ?: AdvicePayload.ReEntry()
            weight = minOf(weight, capFor(reEntry, factors.loadFactor))
        }
        val easedByReturn = target.reason.endsWith(Coach.RE_ENTRY_NOTE)
        if (reEntry != null || easedByReturn) notes += Coach.RE_ENTRY_NOTE
        val base = if (easedByReturn) target.reason.removeSuffix(Coach.RE_ENTRY_NOTE) else target.reason
        return target.copy(
            reason = ReturnRule.withSuggestion(base, weight, payload.reps, payload.repsHigh, payload.rpe) + notes,
            payload = payload.copy(weightKg = weight, sets = sets),
        )
    }
}
