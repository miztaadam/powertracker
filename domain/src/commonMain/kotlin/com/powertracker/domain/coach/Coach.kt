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
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.on
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
 *
 * The powerlifting side's coach. Callers pass the whole log; progression, minimum history, stalls, weak points and
 * fatigue's top-set signals read only its powerlifting sessions, while the 14-day gap, re-entry and readiness read
 * every session. A deload in the prior advice counts whichever side it was made on; so does a re-entry, but only on a
 * side trained before the session that ended its break ([easesBackIn]).
 */
object Coach {
    private const val MIN_SESSIONS = 3
    private const val RECENT_DAYS = 7L
    private const val RE_ENTRY_SHOW_DAYS = 7L
    private const val STALL_PAUSE_DAYS = 14L
    /** The break the coach eases a lifter back from: the same gap [HistoryStats.returnDateAfterGap] finds. */
    internal const val GAP_DAYS = HistoryStats.GAP_DAYS

    /** An OPEN deload or re-entry clamps (and blocks a new one) only this long; then the feed hides it. */
    const val OPEN_ACTIVE_DAYS = 14L
    private const val APPLIED_ACTIVE_DAYS = 7L
    private const val DELOAD_COOLDOWN_DAYS = 21L
    private const val DISMISSED_MEMORY_DAYS = 28L
    private const val STALL_APPLIED_COOLDOWN_DAYS = 28L

    /** Within one evaluate call, the advice kept for a lift: earlier in this list wins. */
    private val LIFT_PRIORITY = listOf(AdviceType.STALL, AdviceType.WEAK_POINT)

    internal const val DELOAD_NOTE = " Deload week: lighter load and fewer sets."
    internal const val RE_ENTRY_NOTE = " Easing back in: lighter load."

    /** New advice to store. Never returns duplicates of OPEN prior advice. */
    fun evaluate(
        history: TrainingHistory,
        plan: List<PlanItem>,
        settings: CoachSettings,
        today: LocalDate,
        priorAdvice: List<Advice>,
    ): List<Advice> = evaluate(history, plan, settings, today, priorAdvice, readinessAloneDeloads = false)

    /**
     * As [evaluate], for [side]: with [readinessAloneDeloads] (the bodybuilding side) low readiness by itself raises
     * the fatigue deload, under the same rails. The re-entry is made only when [side] has a session before the one
     * that ended the break (1.16): a side first trained on the return has nothing to come back to.
     */
    internal fun evaluate(
        history: TrainingHistory,
        plan: List<PlanItem>,
        settings: CoachSettings,
        today: LocalDate,
        priorAdvice: List<Advice>,
        readinessAloneDeloads: Boolean,
        side: TrainingMode = TrainingMode.POWERLIFTING,
    ): List<Advice> {
        val all = HistoryStats(history)
        val stats = powerliftingStats(history, all)
        val candidates = mutableListOf<Advice>()

        val returnDate = all.returnDateAfterGap(today)
        if (returnDate != null) if (trainedBefore(side, history, returnDate)) reEntry(all, returnDate, today, priorAdvice)?.let(candidates::add)

        if (!deloadBlocked(priorAdvice, today)) FatigueRule.evaluate(all, history, today, readinessAloneDeloads)?.let(candidates::add)

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
     * first day back after a gap of 14 days or more, re-entry applies even before any RE_ENTRY advice exists. A
     * stored RE_ENTRY clamps only when this side was trained before its break ended ([easesBackIn]).
     * After time on the bodybuilding side, [ReturnRule] eases each exercise's first target back first.
     * An exercise held by a pain skip in [skips] ([SkipCoach.held]) gets no increase: where progression would add
     * weight, or a rep at the same weight, the target repeats its latest top set instead ([ProgressionRule.held]); a
     * decrease still applies. The
     * return rule and the clamps still apply after that.
     * Does not dedupe against prior TARGET advice: the app does that.
     */
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
        val stats = powerliftingStats(history, all)
        val deload = activeAdvice(AdviceType.DELOAD, priorAdvice, today)
        val lastSession = all.lastSessionDate(today)
        val firstDayBack = lastSession != null && lastSession.daysUntil(today) >= GAP_DAYS
        val reEntry = activeAdvice(AdviceType.RE_ENTRY, priorAdvice, today)
            ?.takeIf { easesBackIn(it, TrainingMode.POWERLIFTING, history, all) }
            ?: if (firstDayBack) Advice(AdviceType.RE_ENTRY, null, today, "", 0, AdvicePayload.ReEntry()) else null
        return plan.filter { it.weekday == day }.sortedBy { it.order }.mapNotNull { item ->
            val exercise = history.exercises[item.exerciseId]?.takeUnless { it.archived } ?: return@mapNotNull null
            val lift = exercise.mainLift
            if (lift != null && !hasMinimumHistory(lift, stats, history)) return@mapNotNull null
            val free = ProgressionRule.target(item, exercise, stats, settings, today) ?: return@mapNotNull null
            val target = if (SkipCoach.held(item.exerciseId, skips, history, today)) ProgressionRule.held(free, item, stats, today) else free
            val eased = ReturnRule.ease(target, item, exercise, history, TrainingMode.POWERLIFTING, settings, today)
            clamp(eased, item, exercise, stats, all, settings, today, deload, reEntry)
        }
    }

    /** Stats over the powerlifting sessions of [history]; [all] itself when every session is on that side. */
    private fun powerliftingStats(history: TrainingHistory, all: HistoryStats): HistoryStats =
        if (history.sessions.all { it.mode == TrainingMode.POWERLIFTING }) all
        else HistoryStats(history.on(TrainingMode.POWERLIFTING))

    /** Whether [side] has a session in [history] dated before [date]. */
    internal fun trainedBefore(side: TrainingMode, history: TrainingHistory, date: LocalDate): Boolean =
        history.sessions.any { it.mode == side && it.date < date }

    /**
     * Whether a stored [reEntry] eases [side]'s targets: only when [side] was trained before the session that ended
     * the break ([all]'s return date as of the advice), so a card stored by an older version on a side first trained
     * on the return eases nothing (1.16).
     */
    internal fun easesBackIn(reEntry: Advice, side: TrainingMode, history: TrainingHistory, all: HistoryStats): Boolean =
        reEntryCounts(side, reEntry.createdAt, history, all)

    /**
     * Whether a RE_ENTRY created on [createdAt] counts on [side]: [side] has a session before the session that ended
     * its break ([all]'s return date as of [createdAt]). Both the clamp and the feed read it.
     */
    internal fun reEntryCounts(side: TrainingMode, createdAt: LocalDate, history: TrainingHistory, all: HistoryStats): Boolean =
        trainedBefore(side, history, all.returnDateAfterGap(createdAt) ?: createdAt)

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

    /**
     * Any non-TARGET advice for [lift] created today − 6 … today, whatever its status. A SKIP card is not counted: it
     * answers the lifter's skip, and has its own rails ([SkipCoach]).
     */
    private fun shownThisWeek(lift: MainLift, prior: List<Advice>, today: LocalDate): Boolean =
        prior.any { it.type != AdviceType.TARGET && it.type != AdviceType.SKIP && it.lift == lift && withinLastWeek(it.createdAt, today) }

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

    /**
     * True once an OPEN deload or re-entry created on [createdAt] no longer counts on [today]: it is older than
     * [OPEN_ACTIVE_DAYS]. Other advice stays open until it is acted on.
     */
    fun openExpired(type: AdviceType, createdAt: LocalDate, today: LocalDate): Boolean =
        expiresWhenOpen(type) && createdAt < today.minus(OPEN_ACTIVE_DAYS - 1, DateTimeUnit.DAY)

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
    internal fun activeAdvice(type: AdviceType, prior: List<Advice>, today: LocalDate): Advice? =
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
        all: HistoryStats,
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
                all.returnDateAfterGap(advice.createdAt)?.takeIf { it < advice.createdAt } ?: advice.createdAt
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
        }
        // Easing back in, from a break or from the return rule.
        if (reEntry != null || target.reason.endsWith(RE_ENTRY_NOTE)) notes += RE_ENTRY_NOTE
        // A lighter week does the planned reps, not progression's extra rep.
        val reps = item.reps
        return target.copy(
            reason = ProgressionRule.describe(last, weight, reps, payload.rpe) + notes,
            payload = payload.copy(weightKg = weight, reps = reps, sets = sets),
        )
    }
}
