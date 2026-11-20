package com.powertracker.domain.coach

import com.powertracker.domain.e1rm.E1rm
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.Skip
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
import kotlinx.datetime.plus
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * The bodybuilding side's coach: reps first, then weight (spec 5.3). For each of today's bodybuilding plan items it
 * looks at the exercise's sets in its latest bodybuilding session before today:
 * - every planned set done, none missed, each at the top of the range at or under the target RPE (a set with no RPE
 *   counts as on target), at a load above 0 kg: add weight, [INCREASE] of the last weight rounded to a loadable weight and at least one
 *   step more, and aim for the range again from its low end;
 * - otherwise: the same weight, and beat last time's reps;
 * - no bodybuilding session yet: from the exercise's powerlifting sets (the 28 days ending at its latest
 *   powerlifting session) or else its baseline, a starting weight for the middle of the range at the target RPE
 *   from the e1RM estimate, eased by the re-entry factor when that history is more than 28 days old, rounded
 *   down; with neither, no advice.
 * Then the return rule, and the deload and easing-back-in clamps the powerlifting targets get (the range is kept).
 * A target is eased by the re-entry factor at most once, whatever the route, and its reason drops "Time to add
 * weight." when the final weight is not above last time's.
 * No lift is special here: there is no minimum history and no main-lift rule.
 * While a pain skip holds an exercise ([SkipCoach.held]) no increase is earned: the weight repeats and the reps aim
 * carries on from last time ([repsToBeat]) rather than starting again at the low end.
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
        skips: List<Skip> = emptyList(),
    ): List<Advice> {
        val all = HistoryStats(history)
        val side = HistoryStats(history.on(TrainingMode.BODYBUILDING))
        val deload = Coach.activeAdvice(AdviceType.DELOAD, priorAdvice, today)
        val lastSession = all.lastSessionDate(today)
        val firstDayBack = lastSession != null && lastSession.daysUntil(today) >= Coach.GAP_DAYS
        // A side with no session before today was never trained: a break eases nothing back on it (1.16). Nor does a
        // stored easing back in from a break this side had not been trained before.
        val trained = Coach.trainedBefore(TrainingMode.BODYBUILDING, history, today)
        val reEntry = Coach.activeAdvice(AdviceType.RE_ENTRY, priorAdvice, today)
            ?.takeIf { Coach.easesBackIn(it, TrainingMode.BODYBUILDING, history, all) }
            ?: if (trained && firstDayBack) Advice(AdviceType.RE_ENTRY, null, today, "", 0, AdvicePayload.ReEntry()) else null
        return plan
            .filter { it.weekday == day && it.mode == TrainingMode.BODYBUILDING }
            .sortedBy { it.order }
            .mapNotNull { item ->
                val exercise = history.exercises[item.exerciseId]?.takeUnless { it.archived } ?: return@mapNotNull null
                // The break's re-entry clamp eases a first-time weight itself: never a second 0.85.
                val held = SkipCoach.held(item.exerciseId, skips, history, today)
                val target = target(item, exercise, history, settings, today, easeOldHistory = reEntry == null, held = held)
                    ?: return@mapNotNull null
                val eased = ReturnRule.ease(target, item, exercise, history, TrainingMode.BODYBUILDING, settings, today)
                withoutAddWeight(clamp(eased, item, exercise, side, all, settings, deload, reEntry), item, history, today)
            }
    }

    /**
     * Reps to put in the reps field for set [setNumber] (1-based): last time's reps for that set + 1, capped at the
     * top of the range; the low end the first time, after an increase, and for a set not done last time. While a pain
     * skip in [skips] holds the exercise there is no increase, so no return to the low end either.
     */
    fun repsToBeat(item: PlanItem, history: TrainingHistory, setNumber: Int, today: LocalDate, skips: List<Skip> = emptyList()): Int {
        val last = lastSets(item.exerciseId, history, today) ?: return item.reps
        if (earnedIncrease(last, item) && !SkipCoach.held(item.exerciseId, skips, history, today)) return item.reps
        val set = last.getOrNull(setNumber - 1) ?: return item.reps
        return minOf(set.reps + 1, high(item))
    }

    private fun high(item: PlanItem) = item.repsHigh ?: item.reps

    /** "Time to add weight." comes out when easing or a deload left the weight at or below last time's. */
    private fun withoutAddWeight(advice: Advice, item: PlanItem, history: TrainingHistory, today: LocalDate): Advice {
        val last = lastSets(item.exerciseId, history, today) ?: return advice
        val weight = (advice.payload as AdvicePayload.Target).weightKg
        if (!earnedIncrease(last, item) || weight > heaviest(last)) return advice
        return advice.copy(reason = advice.reason.replace("${lastTime(last)} $INCREASE_TEXT ", "${lastTime(last)} "))
    }

    /** The exercise's sets, in logged order, in its latest bodybuilding session before [today]; null if none. */
    private fun lastSets(exerciseId: Long, history: TrainingHistory, today: LocalDate): List<LoggedSet>? =
        history.sessions
            .filter { it.mode == TrainingMode.BODYBUILDING && it.date < today }
            .sortedBy { it.date }
            .lastOrNull { s -> s.sets.any { it.exerciseId == exerciseId } }
            ?.sets?.filter { it.exerciseId == exerciseId }

    /** Every planned set at the top of the range at or under the target RPE; never for a 0 kg (bodyweight) load. */
    private fun earnedIncrease(sets: List<LoggedSet>, item: PlanItem): Boolean =
        sets.size >= item.sets && sets.none { it.failed } && heaviest(sets) > 0.0 &&
            sets.all { it.reps >= high(item) && (it.rpe == null || it.rpe <= item.targetRpe) }

    /** The weight of the heaviest non-missed set (of any set when all were missed). */
    private fun heaviest(sets: List<LoggedSet>): Double = (sets.filter { !it.failed }.ifEmpty { sets }).maxOf { it.weightKg }

    private fun target(
        item: PlanItem,
        exercise: Exercise,
        history: TrainingHistory,
        settings: CoachSettings,
        today: LocalDate,
        easeOldHistory: Boolean,
        held: Boolean,
    ): Advice? {
        val last = lastSets(item.exerciseId, history, today)
        val (weight, opening) = if (last == null) {
            firstTimeWeight(item, exercise, history, settings, today, easeOldHistory)?.let { it to FIRST_TIME_TEXT } ?: return null
        } else {
            val heaviest = heaviest(last)
            if (!held && earnedIncrease(last, item)) {
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

    /**
     * From the best non-missed powerlifting set of the exercise in the 28 days ending at its latest powerlifting
     * session (the last 28 days when that session is recent), or with no such set at all from its latest baseline.
     * When that session (or the baseline) is more than 28 days old, the weight is eased by the re-entry factor
     * before rounding down, unless [easeOld] is false (the break's re-entry clamp eases it instead). Null when there
     * is neither.
     */
    private fun firstTimeWeight(
        item: PlanItem,
        exercise: Exercise,
        history: TrainingHistory,
        settings: CoachSettings,
        today: LocalDate,
        easeOld: Boolean,
    ): Double? {
        val recentFrom = today.minus(FIRST_TIME_DAYS, DateTimeUnit.DAY)
        val dated = history.sessions
            .filter { it.mode == TrainingMode.POWERLIFTING && it.date < today }
            .flatMap { s -> s.sets.map { s.date to it } }
            .filter { (_, set) -> set.exerciseId == item.exerciseId && !set.failed && set.weightKg > 0.0 }
        val middle = (item.reps + high(item)) / 2
        val (kg, anchor, asOf) = if (dated.isNotEmpty()) {
            val latest = dated.maxOf { it.first }
            // 28 days ending yesterday when the latest session is recent (as before), else ending on that session.
            val end = if (latest >= recentFrom) today else latest.plus(1, DateTimeUnit.DAY)
            val from = end.minus(FIRST_TIME_DAYS, DateTimeUnit.DAY)
            val best = dated.filter { it.first >= from && it.first < end }.map { it.second }
                .maxBy { E1rm.estimate(it.weightKg, it.reps, it.rpe) }
            Triple(E1rm.loadFor(E1rm.estimate(best.weightKg, best.reps, best.rpe), middle, item.targetRpe), best.weightKg, latest)
        } else {
            val baseline = history.baselines.filter { it.exerciseId == item.exerciseId && it.date <= today }
                .maxByOrNull { it.date } ?: return null
            val kg = E1rm.loadFor(baseline.e1rmKg, middle, item.targetRpe)
            Triple(kg, 0.0, baseline.date)
        }
        val wanted = if (easeOld && asOf < recentFrom) kg * AdvicePayload.ReEntry().loadFactor else kg
        val free = ProgressionRule.isFreeLoad(exercise, if (anchor > 0.0) anchor else wanted)
        return roundDown(wanted, anchor, free, settings.changePlates)
    }

    /**
     * The nearest loadable weight not above [kg] on this side: a free load on the [FREE_STEP_KG] grid anchored on
     * [anchorKg] (never below 0), a barbell load on the plate grid as the powerlifting side rounds it.
     */
    internal fun roundDown(kg: Double, anchorKg: Double, free: Boolean, changePlates: Boolean): Double {
        if (!free) return ProgressionRule.roundDown(kg, anchorKg, free = false, changePlates = changePlates)
        val step = (FREE_STEP_KG * 100).roundToLong()
        val base = (anchorKg * 100).roundToLong()
        val over = (kg * 100).roundToLong() - base
        return max(base + over.floorDiv(step) * step, 0L) / 100.0
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
            return roundDown(reference * factor, reference, free, settings.changePlates)
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
