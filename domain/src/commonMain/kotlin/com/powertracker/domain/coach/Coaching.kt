package com.powertracker.domain.coach

import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.CheckIn
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.Skip
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.forSide
import com.powertracker.domain.model.on
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * The one entry point both apps call, never [Coach] or [BodybuildingCoach] directly. It takes the whole log, the
 * whole plan and every stored advice, narrows the plan and the advice to [TrainingMode]'s side, asks that side's
 * coach, and stamps the side on what comes back. On the powerlifting side the output is exactly [Coach]'s.
 */
object Coaching {
    /** A side left this many days ago or fewer shows no welcome-back card. */
    private const val WELCOME_BACK_DAYS = 30

    fun targetsFor(
        mode: TrainingMode,
        day: DayOfWeek,
        history: TrainingHistory,
        plan: List<PlanItem>,
        settings: CoachSettings,
        today: LocalDate,
        priorAdvice: List<Advice>,
        skips: List<Skip> = emptyList(),
    ): List<Advice> {
        val items = plan.on(mode)
        val prior = priorAdvice.forSide(mode)
        val advice = when (mode) {
            TrainingMode.POWERLIFTING -> Coach.targetsFor(day, history, items, settings, today, prior, skips)
            TrainingMode.BODYBUILDING -> BodybuildingCoach.targetsFor(day, history, items, settings, today, prior, skips)
        }
        return advice.map { it.copy(mode = mode) }
    }

    /**
     * New advice to store for [mode]'s side. On the bodybuilding side in this stage only the advice that belongs to
     * the lifter: easing back in after a break and the fatigue deload (the same rule and the same one-at-a-time
     * rails as on the powerlifting side). The deload's other signals read only powerlifting sessions, so on the
     * bodybuilding side low readiness (over every session, as the rule reads it) is enough by itself. Easing back in
     * is made only for a side trained before the session that ended the break, on either side (1.16).
     *
     * From stage 3 the bodybuilding side also gets [PhysiqueCoach.stuck]'s SWAP advice, reading [checkIns] for the
     * growing-well groups; a deload or easing-back-in made in this same call counts as one created today. The
     * powerlifting side ignores [checkIns].
     */
    fun evaluate(
        mode: TrainingMode,
        history: TrainingHistory,
        plan: List<PlanItem>,
        settings: CoachSettings,
        today: LocalDate,
        priorAdvice: List<Advice>,
        checkIns: List<CheckIn> = emptyList(),
    ): List<Advice> {
        val advice = Coach.evaluate(
            history, plan.on(mode), settings, today, priorAdvice.forSide(mode),
            readinessAloneDeloads = mode == TrainingMode.BODYBUILDING,
            side = mode,
        )
        val kept = when (mode) {
            TrainingMode.POWERLIFTING -> advice
            TrainingMode.BODYBUILDING -> advice.filter { it.type == AdviceType.RE_ENTRY || it.type == AdviceType.DELOAD }
        }.map { it.copy(mode = mode) }
        return when (mode) {
            TrainingMode.POWERLIFTING -> kept
            TrainingMode.BODYBUILDING -> kept + PhysiqueCoach.stuck(history, plan.on(mode), checkIns, priorAdvice.forSide(mode) + kept, today)
        }
    }

    /** Whether a check-in is due on the bodybuilding side ([PhysiqueCoach.checkInDue]). */
    fun checkInDue(history: TrainingHistory, plan: List<PlanItem>, checkIns: List<CheckIn>, today: LocalDate): Boolean =
        PhysiqueCoach.checkInDue(history, plan.on(TrainingMode.BODYBUILDING), checkIns, today)

    /**
     * What saving [checkIn] gives ([PhysiqueCoach.afterCheckIn]), from the bodybuilding side's plan only, with its
     * advice stamped BODYBUILDING. The OPEN MORE_WORK advice of that side in [priorAdvice] comes back dismissed.
     */
    fun afterCheckIn(
        checkIn: CheckIn,
        history: TrainingHistory,
        plan: List<PlanItem>,
        today: LocalDate,
        priorAdvice: List<Advice> = emptyList(),
    ): CheckInOutcome {
        val side = TrainingMode.BODYBUILDING
        val outcome = PhysiqueCoach.afterCheckIn(checkIn, history, plan.on(side), today, priorAdvice.filter { it.mode == side })
        return outcome.copy(advice = outcome.advice.map { it.copy(mode = side) })
    }

    fun afterSkip(skip: Skip, skips: List<Skip>, history: TrainingHistory, plan: List<PlanItem>, today: LocalDate, priorAdvice: List<Advice>): List<Advice> =
        SkipCoach.afterSkip(skip, skips, history, plan, today, priorAdvice)

    fun applySkipSwap(payload: AdvicePayload.Skipped, choice: String, mode: TrainingMode, plan: List<PlanItem>, exercises: Map<Long, Exercise>): PlanChange =
        SkipCoach.applySkipSwap(payload, choice, mode, plan, exercises)

    /**
     * The welcome-back card: this side has a plan, was left more than 30 days before [today], and was trained before
     * it was left: its earliest session ([firstSessionOn]) is on or before [leftOn]. A side left in Settings before
     * any session on it has nothing to come back to, so it gets no card, even once it is trained.
     */
    fun welcomeBack(leftOn: LocalDate?, today: LocalDate, hasPlan: Boolean, firstSessionOn: LocalDate?): Boolean =
        hasPlan && leftOn != null && firstSessionOn != null && firstSessionOn <= leftOn &&
            leftOn.daysUntil(today) > WELCOME_BACK_DAYS

    /** The date of the earliest session logged on [mode]'s side of [history]; null when it has none. */
    fun firstSessionOn(mode: TrainingMode, history: TrainingHistory): LocalDate? =
        history.sessions.filter { it.mode == mode }.minOfOrNull { it.date }
}
