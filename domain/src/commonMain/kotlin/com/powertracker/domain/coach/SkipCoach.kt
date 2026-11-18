package com.powertracker.domain.coach

import com.powertracker.domain.library.Library
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.on
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * The coach's answer to a skipped planned exercise (skip-a-planned-exercise spec, "What the coach does"). Pure
 * functions; a skip is never a set, a missed set, a stall or a reason to deload, and nothing here reads it as one.
 *
 * [afterSkip] runs when a skip is stored and gives at most one SKIP card, by the skip's reason, counted per exercise
 * and per side over the [WINDOW_DAYS] days ending on the skip's date (that skip included):
 * - pain: every pain skip. The card offers gentler alternatives and says the weight is held ([held]); for a main lift,
 *   or with nothing left to swap to, it only says the weight is held;
 * - busy or no equipment: the [BUSY_SKIPS]th busy skip, or a later one. Alternatives whose first equipment differs;
 * - repeat: the [REPEAT_SKIPS]th skip of any reason but time, or a later one. Alternatives as they come;
 * - out of time: never a card, and not counted.
 * Earlier in that list wins; a rule with no alternative to offer gives way to the next. A main lift (category SQUAT,
 * BENCH, DEADLIFT or STRICT_PRESS) is never swapped out: it gets only the pain card. No card while an OPEN SKIP or
 * SWAP card for the exercise stands on that side, within [CARD_COOLDOWN_DAYS] days of an earlier SKIP card for it,
 * or within [DISMISSED_MEMORY_DAYS] days of a dismissed one at least as severe (pain over busy over repeat).
 *
 * Every number here is a product rule, the designer's and not sourced. The pain card's last line is sourced:
 * [CoachSources.SKIP_PAIN_PHYSICIAN] and [CoachSources.SKIP_PAIN_TRAIN_AROUND].
 */
object SkipCoach {
    /** Skips are counted over this many days ending on the skip's date. */
    const val WINDOW_DAYS = 28

    /** The busy skip from which a card is given (the rails below keep it to one at a time). */
    const val BUSY_SKIPS = 2

    /** The skip, of any reason but time, from which the repeat card is given. */
    const val REPEAT_SKIPS = 3

    /** No second SKIP card for an exercise within this many days of the last, whatever became of it. */
    const val CARD_COOLDOWN_DAYS = 7

    /** A dismissed SKIP card holds back one as severe or less for this many days, as the coach's other cards do. */
    const val DISMISSED_MEMORY_DAYS = 28

    /** Alternatives offered on a card. */
    const val ALTERNATIVES = PhysiqueCoach.ALTERNATIVES

    /** Card severities: a dismissed card holds back a new one of the same severity or lower. */
    const val REPEAT_SEVERITY = 1
    const val BUSY_SEVERITY = 2
    const val PAIN_SEVERITY = 3

    /** The pain card's last line (ExRx.net, CoachSources.SKIP_PAIN_PHYSICIAN). */
    internal const val PAIN_LINE = "If the pain persists or gets worse, see a physician."

    /**
     * Zero or one SKIP card for [skip], stamped with its side. [skips] is every stored skip ([skip] among them); only
     * [skip]'s side and exercise count. [plan] is the whole plan: the alternatives come from [skip]'s side of it.
     */
    fun afterSkip(
        skip: Skip,
        skips: List<Skip>,
        history: TrainingHistory,
        plan: List<PlanItem>,
        today: LocalDate,
        priorAdvice: List<Advice>,
    ): List<Advice> {
        val exercise = history.exercises[skip.exerciseId] ?: return emptyList()
        val mode = skip.mode
        val cards = priorAdvice.filter { it.mode == mode && exerciseOf(it) == skip.exerciseId }
        if (blocked(cards, today)) return emptyList()

        val from = skip.date.minus(WINDOW_DAYS - 1, DateTimeUnit.DAY)
        // One skip per exercise, date and side: [skip] stands for its own date.
        val counted = skips.filter {
            it.mode == mode && it.exerciseId == skip.exerciseId && it.date >= from && it.date < skip.date
        } + skip
        val busy = counted.count { it.reason == SkipReason.BUSY }
        val notTime = counted.count { it.reason != SkipReason.TIME }

        val main = exercise.isMainLift
        val alternatives by lazy {
            if (main) emptyList() else PhysiqueCoach.swapAlternatives(exercise, plan.on(mode), history.exercises, mode)
        }
        val card = pain(skip, exercise, main, today) { alternatives.take(ALTERNATIVES).map { it.name } }
            ?: busy(skip, busy, exercise, main, today) {
                val own = Library.find(exercise.name)?.equipment?.firstOrNull()
                alternatives.filter { it.equipment.firstOrNull() != own }.take(ALTERNATIVES).map { it.name }
            }
            ?: repeat(skip, notTime, exercise, main, today) { alternatives.take(ALTERNATIVES).map { it.name } }
            ?: return emptyList()
        if (!dismissedMemoryAllows(card.severity, cards, today)) return emptyList()
        return listOf(card)
    }

    /** [payload]'s exercise swapped for [choice] in every [mode] plan item that holds it ([PhysiqueCoach.swap]). */
    fun applySkipSwap(
        payload: AdvicePayload.Skipped,
        choice: String,
        mode: TrainingMode,
        plan: List<PlanItem>,
        exercises: Map<Long, Exercise>,
    ): PlanChange = PhysiqueCoach.swap(payload.exerciseId, payload.name, payload.alternatives, choice, mode, plan, exercises)

    /**
     * Whether a pain skip holds [exerciseId]'s weight on [today]: a pain skip of it, on either side and dated on or
     * before [today], with no session holding a set of it, on either side, dated after the latest such skip and before
     * [today]. Today's own session releases it only from tomorrow, so today's held target and reps aim stay as they
     * were while it is being done (the coach reads only sessions before today, as for every target).
     */
    fun held(exerciseId: Long, skips: List<Skip>, history: TrainingHistory, today: LocalDate): Boolean {
        val latest = skips
            .filter { it.exerciseId == exerciseId && it.reason == SkipReason.PAIN && it.date <= today }
            .maxOfOrNull { it.date } ?: return false
        return history.sessions.none { s -> s.date > latest && s.date < today && s.sets.any { it.exerciseId == exerciseId } }
    }

    // ---- the three cards ----

    private fun pain(skip: Skip, exercise: Exercise, main: Boolean, today: LocalDate, alternatives: () -> List<String>): Advice? {
        if (skip.reason != SkipReason.PAIN) return null
        val offered = if (main) emptyList() else alternatives()
        val name = exercise.name
        val reason = if (offered.isEmpty()) {
            "You skipped $name because of pain. Its weight won't go up until you do it again. $PAIN_LINE"
        } else {
            "You skipped $name because of pain. Try one of these instead. Its weight won't go up until you do $name again. $PAIN_LINE"
        }
        return card(skip, exercise, main, today, reason, PAIN_SEVERITY, offered)
    }

    private fun busy(skip: Skip, busy: Int, exercise: Exercise, main: Boolean, today: LocalDate, alternatives: () -> List<String>): Advice? {
        if (main || skip.reason != SkipReason.BUSY || busy < BUSY_SKIPS) return null
        val offered = alternatives().ifEmpty { return null }
        val reason = "You've skipped ${exercise.name} twice in 4 weeks because you were busy or had no equipment. Swap it for one of these?"
        return card(skip, exercise, main, today, reason, BUSY_SEVERITY, offered)
    }

    private fun repeat(skip: Skip, notTime: Int, exercise: Exercise, main: Boolean, today: LocalDate, alternatives: () -> List<String>): Advice? {
        if (main || skip.reason == SkipReason.TIME || notTime < REPEAT_SKIPS) return null
        val offered = alternatives().ifEmpty { return null }
        val reason = "You've skipped ${exercise.name} 3 times in 4 weeks. Keep it in your plan, or swap it for one of these?"
        return card(skip, exercise, main, today, reason, REPEAT_SEVERITY, offered)
    }

    /** A SKIP card; its lift is the exercise's own for a main lift, and null for anything else (a variation included). */
    private fun card(skip: Skip, exercise: Exercise, main: Boolean, today: LocalDate, reason: String, severity: Int, alternatives: List<String>) = Advice(
        type = AdviceType.SKIP,
        lift = if (main) exercise.mainLift else null,
        createdAt = today,
        reason = reason,
        severity = severity,
        payload = AdvicePayload.Skipped(skip.id, skip.exerciseId, exercise.name, skip.reason, alternatives),
        mode = skip.mode,
    )

    // ---- the rails ----

    /** The exercise a SKIP or SWAP card is about; null for any other advice. */
    private fun exerciseOf(advice: Advice): Long? = when (val p = advice.payload) {
        is AdvicePayload.Skipped -> if (advice.type == AdviceType.SKIP) p.exerciseId else null
        is AdvicePayload.Swap -> if (advice.type == AdviceType.SWAP) p.exerciseId else null
        else -> null
    }

    /** An OPEN SKIP or SWAP card stands, or a SKIP card was made in the [CARD_COOLDOWN_DAYS] days ending [today]. */
    private fun blocked(cards: List<Advice>, today: LocalDate): Boolean =
        cards.any { it.status == AdviceStatus.OPEN } ||
            cards.any { it.type == AdviceType.SKIP && within(it.createdAt, today, CARD_COOLDOWN_DAYS) }

    /**
     * False when the latest SKIP card dismissed within the [DISMISSED_MEMORY_DAYS] days ending [today] (counted from
     * when it was made, as the coach's other cards are) was at least as severe as [severity].
     */
    private fun dismissedMemoryAllows(severity: Int, cards: List<Advice>, today: LocalDate): Boolean {
        val last = cards
            .filter { it.type == AdviceType.SKIP && it.status == AdviceStatus.DISMISSED && within(it.createdAt, today, DISMISSED_MEMORY_DAYS) }
            .maxWithOrNull(compareBy({ it.createdAt }, { it.id }))
        return last == null || severity > last.severity
    }

    /** [date] is one of the [days] days ending [today]. */
    private fun within(date: LocalDate, today: LocalDate, days: Int): Boolean =
        date <= today && date >= today.minus(days - 1, DateTimeUnit.DAY)
}
