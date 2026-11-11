package com.powertracker.domain.coach

import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.plates.PlateLoader
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Single entry point for the app: runs every coach rule and applies the safety rails of spec 5.5
 * (minimum history, one card per lift per week, OPEN/DISMISSED memory, re-entry and deload clamps).
 */
object Coach {
    private const val MIN_SESSIONS = 3
    private const val RECENT_DAYS = 7L
    private const val RE_ENTRY_SHOW_DAYS = 7L
    private const val STALL_PAUSE_DAYS = 14L

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

        FatigueRule.evaluate(stats, history, today)?.let(candidates::add)

        val stallsPaused = returnDate != null && today.isBefore(returnDate.plusDays(STALL_PAUSE_DAYS))
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
            notRepeated(advice, priorAdvice) &&
                (lift == null || (hasMinimumHistory(lift, stats, history) && !shownThisWeek(lift, priorAdvice, today)))
        }
        return onePerLift(kept)
    }

    /**
     * TARGET advice for today's plan items only (used by the Today screen), in plan order.
     * Skips items whose exercise is missing or archived, and main-lift/variation items whose lift lacks
     * minimum history. While a DELOAD or RE_ENTRY is OPEN, or APPLIED within the last 7 days, loads are
     * capped (and, for a deload, sets reduced). Does not dedupe against prior TARGET advice: the app does that.
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
        val reEntry = activeAdvice(AdviceType.RE_ENTRY, priorAdvice, today)
        return plan.filter { it.weekday == day }.sortedBy { it.order }.mapNotNull { item ->
            val exercise = history.exercises[item.exerciseId]?.takeUnless { it.archived } ?: return@mapNotNull null
            val lift = exercise.mainLift
            if (lift != null && !hasMinimumHistory(lift, stats, history)) return@mapNotNull null
            val target = ProgressionRule.target(item, exercise, stats, settings, today) ?: return@mapNotNull null
            clamp(target, item, stats, settings, today, deload, reEntry)
        }
    }

    private fun reEntry(stats: HistoryStats, returnDate: LocalDate, today: LocalDate, prior: List<Advice>): Advice? {
        if (today.isAfter(returnDate.plusDays(RE_ENTRY_SHOW_DAYS))) return null
        if (prior.any { it.type == AdviceType.RE_ENTRY && !it.createdAt.isBefore(returnDate) }) return null
        val previous = stats.lastSessionDate(returnDate) ?: return null
        val gap = ChronoUnit.DAYS.between(previous, returnDate).toInt()
        val payload = AdvicePayload.ReEntry()
        val reason = "Welcome back — it's been $gap days since your previous session. " +
            "Take a lighter week: about ${Math.round(payload.loadFactor * 100)}% of your usual loads."
        return Advice(AdviceType.RE_ENTRY, null, today, reason, gap, payload)
    }

    /** At least 3 sessions of the lift itself, or a Baseline for the lift's main exercise. */
    private fun hasMinimumHistory(lift: MainLift, stats: HistoryStats, history: TrainingHistory): Boolean =
        stats.sessionCount(lift) >= MIN_SESSIONS ||
            history.baselines.any { b -> history.exercises[b.exerciseId]?.let { it.isMainLift && it.mainLift == lift } == true }

    /** Any non-TARGET advice for [lift] created today − 6 … today, whatever its status. */
    private fun shownThisWeek(lift: MainLift, prior: List<Advice>, today: LocalDate): Boolean =
        prior.any { it.type != AdviceType.TARGET && it.lift == lift && withinLastWeek(it.createdAt, today) }

    /** False if the same type + lift is still OPEN, or its latest dismissal was at least as severe. */
    private fun notRepeated(advice: Advice, prior: List<Advice>): Boolean {
        val same = prior.filter { it.type == advice.type && it.lift == advice.lift }
        if (same.any { it.status == AdviceStatus.OPEN }) return false
        val lastDismissed = same
            .filter { it.status == AdviceStatus.DISMISSED }
            .maxWithOrNull(compareBy({ it.createdAt }, { it.id }))
        return lastDismissed == null || advice.severity > lastDismissed.severity
    }

    private fun onePerLift(advice: List<Advice>): List<Advice> {
        val chosen = advice.filter { it.lift != null }
            .groupBy { it.lift }
            .values
            .map { forLift -> forLift.minBy { LIFT_PRIORITY.indexOf(it.type).let { i -> if (i < 0) Int.MAX_VALUE else i } } }
        return advice.filter { a -> a.lift == null || chosen.any { it === a } }
    }

    private fun withinLastWeek(date: LocalDate, today: LocalDate): Boolean =
        !date.isBefore(today.minusDays(RECENT_DAYS - 1)) && !date.isAfter(today)

    /** The latest advice of [type] that is OPEN, or APPLIED within the last 7 days. */
    private fun activeAdvice(type: AdviceType, prior: List<Advice>, today: LocalDate): Advice? =
        prior.filter {
            it.type == type &&
                (it.status == AdviceStatus.OPEN || (it.status == AdviceStatus.APPLIED && withinLastWeek(it.createdAt, today)))
        }.maxByOrNull { it.createdAt }

    private fun clamp(
        target: Advice,
        item: PlanItem,
        stats: HistoryStats,
        settings: CoachSettings,
        today: LocalDate,
        deload: Advice?,
        reEntry: Advice?,
    ): Advice {
        if (deload == null && reEntry == null) return target
        val (_, last) = stats.lastTopSet(item.exerciseId, today) ?: return target
        val payload = target.payload as AdvicePayload.Target
        var weight = payload.weightKg
        var sets = payload.sets
        var notes = ""
        if (deload != null) {
            val factors = deload.payload as? AdvicePayload.Deload ?: AdvicePayload.Deload()
            weight = minOf(weight, roundDown(last.weightKg * factors.loadFactor, settings.changePlates))
            sets = maxOf(1, (item.sets * factors.volumeFactor).roundToInt())
            notes += DELOAD_NOTE
        }
        if (reEntry != null) {
            val factors = reEntry.payload as? AdvicePayload.ReEntry ?: AdvicePayload.ReEntry()
            weight = minOf(weight, roundDown(last.weightKg * factors.loadFactor, settings.changePlates))
            notes += RE_ENTRY_NOTE
        }
        return target.copy(
            reason = ProgressionRule.describe(last, weight, payload.reps, payload.rpe) + notes,
            payload = payload.copy(weightKg = weight, sets = sets),
        )
    }

    /** Nearest loadable weight not above [kg] (never below the empty bar). */
    private fun roundDown(kg: Double, changePlates: Boolean): Double {
        val rounded = PlateLoader.round(kg, changePlates)
        return if (rounded > kg) PlateLoader.round(rounded - PlateLoader.step(changePlates), changePlates) else rounded
    }
}
