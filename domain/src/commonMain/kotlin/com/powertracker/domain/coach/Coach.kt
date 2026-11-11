package com.powertracker.domain.coach

import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Single entry point for the app: runs every coach rule and applies the safety rails of spec 5.5
 * (minimum history, one card per lift per week, OPEN/DISMISSED memory, re-entry and deload clamps).
 */
object Coach {
    private const val MIN_SESSIONS = 3
    private const val RECENT_DAYS = 7L
    private const val RE_ENTRY_SHOW_DAYS = 7L
    private const val STALL_PAUSE_DAYS = 14L
    private const val GAP_DAYS = 14L

    /** An OPEN deload or re-entry clamps (and blocks a new one) only this long; then the feed hides it. */
    const val OPEN_ACTIVE_DAYS = 14L
    private const val APPLIED_ACTIVE_DAYS = 7L
    private const val DELOAD_COOLDOWN_DAYS = 21L
    private const val DISMISSED_MEMORY_DAYS = 28L
    private const val STALL_APPLIED_COOLDOWN_DAYS = 28L

    /** Within one evaluate call, the advice kept for a lift: earlier in this list wins. */
    private val LIFT_PRIORITY = listOf(AdviceType.STALL, AdviceType.WEAK_POINT)

    private const val DELOAD_NOTE = " Deload week: lighter load and fewer sets."
    private const val RE_ENTRY_NOTE = " Easing back in: lighter load."

    /** New advice to store. Never returns duplicates of OPEN prior advice. */
    fun evaluate(
        history: TrainingHistory,
        plan: List<PlanItem>,
        settings: CoachSettings,
        today: LocalDate,
        priorAdvice: List<Advice>,
    ): List<Advice> {
        val stats = HistoryStats(history)
        val candidates = mutableListOf<Advice>()

        val returnDate = stats.returnDateAfterGap(today)
        if (returnDate != null) reEntry(stats, returnDate, today, priorAdvice)?.let(candidates::add)

        if (!deloadBlocked(priorAdvice, today)) FatigueRule.evaluate(stats, history, today)?.let(candidates::add)

        val stallsPaused = returnDate != null && today < returnDate.plus(STALL_PAUSE_DAYS, DateTimeUnit.DAY)
        if (!stallsPaused) {
            val accessoryNames = plan
                .mapNotNull { history.exercises[it.exerciseId] }
                .filter { it.category == Category.ACCESSORY || it.category == Category.VARIATION }
                .map { it.name }
                .toSet()
            MainLift.entries.mapNotNullTo(candidates) { StallRule.evaluate(it, stats, settings, today, accessoryNames) }
        }

        candidates += WeakPointRule.evaluate(stats, today)

        val kept = candidates.filter { advice ->
            val lift = advice.lift
            notRepeated(advice, priorAdvice, today) &&
                !stallCoolingDown(advice, priorAdvice, today) &&
                (lift == null || (hasMinimumHistory(lift, stats, history) && !shownThisWeek(lift, priorAdvice, today)))
        }
        return onePerLift(kept)
    }

    /**
     * TARGET advice for today's plan items only (used by the Today screen), in plan order.
     * Skips items whose exercise is missing or archived, and main-lift/variation items whose lift lacks
     * minimum history. While a DELOAD or RE_ENTRY is OPEN (for 14 days) or APPLIED within the last 7 days,
     * loads are capped against the top set from before that advice (and, for a deload, sets reduced). On the
     * first day back after a gap of 14 days or more, re-entry applies even before any RE_ENTRY advice exists.
     * Does not dedupe against prior TARGET advice: the app does that.
     */
    fun targetsFor(
        day: DayOfWeek,
        history: TrainingHistory,
        plan: List<PlanItem>,
        settings: CoachSettings,
        today: LocalDate,
        priorAdvice: List<Advice>,
    ): List<Advice> {
        val stats = HistoryStats(history)
        val deload = activeAdvice(AdviceType.DELOAD, priorAdvice, today)
        val lastSession = stats.lastSessionDate(today)
        val firstDayBack = lastSession != null && lastSession.daysUntil(today) >= GAP_DAYS
        val reEntry = activeAdvice(AdviceType.RE_ENTRY, priorAdvice, today)
            ?: if (firstDayBack) Advice(AdviceType.RE_ENTRY, null, today, "", 0, AdvicePayload.ReEntry()) else null
        return plan.filter { it.weekday == day }.sortedBy { it.order }.mapNotNull { item ->
            val exercise = history.exercises[item.exerciseId]?.takeUnless { it.archived } ?: return@mapNotNull null
            val lift = exercise.mainLift
            if (lift != null && !hasMinimumHistory(lift, stats, history)) return@mapNotNull null
            val target = ProgressionRule.target(item, exercise, stats, settings, today) ?: return@mapNotNull null
            clamp(target, item, exercise, stats, settings, today, deload, reEntry)
        }
    }

    private fun reEntry(stats: HistoryStats, returnDate: LocalDate, today: LocalDate, prior: List<Advice>): Advice? {
        if (today > returnDate.plus(RE_ENTRY_SHOW_DAYS, DateTimeUnit.DAY)) return null
        if (prior.any { it.type == AdviceType.RE_ENTRY && it.createdAt >= returnDate }) return null
        val previous = stats.lastSessionDate(returnDate) ?: return null
        val gap = previous.daysUntil(returnDate)
        val payload = AdvicePayload.ReEntry()
        val reason = "Welcome back — it's been $gap days since your previous session. " +
            "Take a lighter week: about ${(payload.loadFactor * 100).roundToLong()}% of your usual loads."
        return Advice(AdviceType.RE_ENTRY, null, today, reason, gap, payload)
    }

    /** At least 3 sessions of the lift itself, or a Baseline for the lift's main exercise. */
    private fun hasMinimumHistory(lift: MainLift, stats: HistoryStats, history: TrainingHistory): Boolean =
        stats.sessionCount(lift) >= MIN_SESSIONS ||
            history.baselines.any { b -> history.exercises[b.exerciseId]?.let { it.isMainLift && it.mainLift == lift } == true }

    /** Any non-TARGET advice for [lift] created today − 6 … today, whatever its status. */
    private fun shownThisWeek(lift: MainLift, prior: List<Advice>, today: LocalDate): Boolean =
        prior.any { it.type != AdviceType.TARGET && it.lift == lift && withinLastWeek(it.createdAt, today) }

    /**
     * False if the same type + lift is still OPEN (a deload or re-entry only for its first 14 days), or its
     * latest dismissal within the last 28 days was at least as severe.
     */
    private fun notRepeated(advice: Advice, prior: List<Advice>, today: LocalDate): Boolean {
        val same = prior.filter { it.type == advice.type && it.lift == advice.lift }
        if (same.any { it.status == AdviceStatus.OPEN && (!expiresWhenOpen(it.type) || within(it.createdAt, today, OPEN_ACTIVE_DAYS)) }) {
            return false
        }
        val lastDismissed = same
            .filter { it.status == AdviceStatus.DISMISSED && within(it.createdAt, today, DISMISSED_MEMORY_DAYS) }
            .maxWithOrNull(compareBy({ it.createdAt }, { it.id }))
        return lastDismissed == null || advice.severity > lastDismissed.severity
    }

    private fun expiresWhenOpen(type: AdviceType) = type == AdviceType.DELOAD || type == AdviceType.RE_ENTRY

    /** No new deload while one is active, nor within 21 days of an applied one. */
    private fun deloadBlocked(prior: List<Advice>, today: LocalDate): Boolean =
        activeAdvice(AdviceType.DELOAD, prior, today) != null ||
            prior.any { it.type == AdviceType.DELOAD && it.status == AdviceStatus.APPLIED && within(it.createdAt, today, DELOAD_COOLDOWN_DAYS) }

    /** A stall the lifter applied gets 28 days for its accessories to work before the lift can stall again. */
    private fun stallCoolingDown(advice: Advice, prior: List<Advice>, today: LocalDate): Boolean =
        advice.type == AdviceType.STALL && prior.any {
            it.type == AdviceType.STALL && it.lift == advice.lift && it.status == AdviceStatus.APPLIED &&
                within(it.createdAt, today, STALL_APPLIED_COOLDOWN_DAYS)
        }

    private fun onePerLift(advice: List<Advice>): List<Advice> {
        val chosen = advice.filter { it.lift != null }
            .groupBy { it.lift }
            .values
            .map { forLift -> forLift.minBy { LIFT_PRIORITY.indexOf(it.type).let { i -> if (i < 0) Int.MAX_VALUE else i } } }
        return advice.filter { a -> a.lift == null || chosen.any { it === a } }
    }

    private fun withinLastWeek(date: LocalDate, today: LocalDate): Boolean = within(date, today, RECENT_DAYS)

    /** [date] is one of the [days] days ending today (today − (days − 1) … today). */
    private fun within(date: LocalDate, today: LocalDate, days: Long): Boolean =
        date >= today.minus(days - 1, DateTimeUnit.DAY) && date <= today

    /** The latest advice of [type] that is OPEN (created within 14 days), or APPLIED within the last 7 days. */
    private fun activeAdvice(type: AdviceType, prior: List<Advice>, today: LocalDate): Advice? =
        prior.filter {
            it.type == type &&
                (
                    (it.status == AdviceStatus.OPEN && within(it.createdAt, today, OPEN_ACTIVE_DAYS)) ||
                        (it.status == AdviceStatus.APPLIED && within(it.createdAt, today, APPLIED_ACTIVE_DAYS))
                    )
        }.maxByOrNull { it.createdAt }

    private fun clamp(
        target: Advice,
        item: PlanItem,
        exercise: Exercise,
        stats: HistoryStats,
        settings: CoachSettings,
        today: LocalDate,
        deload: Advice?,
        reEntry: Advice?,
    ): Advice {
        if (deload == null && reEntry == null) return target
        val (_, last) = stats.lastTopSet(item.exerciseId, today) ?: return target
        val payload = target.payload as AdvicePayload.Target

        /**
         * [factor] of a fixed reference: the top set from before [advice] was created (so a week of lighter
         * sessions does not compound), else the latest one for an exercise first done since.
         */
        fun capFor(advice: Advice, factor: Double): Double {
            // A re-entry is measured from the last session before the break, even when the advice was
            // only created a session or two after the return (e.g. Finish was skipped on the return day).
            val referenceDate = if (advice.type == AdviceType.RE_ENTRY) {
                stats.returnDateAfterGap(advice.createdAt)?.takeIf { it < advice.createdAt } ?: advice.createdAt
            } else {
                advice.createdAt
            }
            val reference = (stats.lastTopSet(item.exerciseId, referenceDate)?.second ?: last).weightKg
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
            notes += DELOAD_NOTE
        }
        if (reEntry != null) {
            val factors = reEntry.payload as? AdvicePayload.ReEntry ?: AdvicePayload.ReEntry()
            weight = minOf(weight, capFor(reEntry, factors.loadFactor))
            notes += RE_ENTRY_NOTE
        }
        // A lighter week does the planned reps, not progression's extra rep.
        val reps = item.reps
        return target.copy(
            reason = ProgressionRule.describe(last, weight, reps, payload.rpe) + notes,
            payload = payload.copy(weightKg = weight, reps = reps, sets = sets),
        )
    }
}
