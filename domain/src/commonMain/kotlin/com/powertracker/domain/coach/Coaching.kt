package com.powertracker.domain.coach

import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.PlanItem
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
    ): List<Advice> {
        val items = plan.on(mode)
        val prior = priorAdvice.forSide(mode)
        val advice = when (mode) {
            TrainingMode.POWERLIFTING -> Coach.targetsFor(day, history, items, settings, today, prior)
            TrainingMode.BODYBUILDING -> BodybuildingCoach.targetsFor(day, history, items, settings, today, prior)
        }
        return advice.map { it.copy(mode = mode) }
    }

    /**
     * New advice to store for [mode]'s side. On the bodybuilding side in this stage only the advice that belongs to
     * the lifter: easing back in after a break and the fatigue deload (the same rule and the same one-at-a-time
     * rails as on the powerlifting side). The deload's other signals read only powerlifting sessions, so on the
     * bodybuilding side low readiness (over every session, as the rule reads it) is enough by itself.
     */
    fun evaluate(
        mode: TrainingMode,
        history: TrainingHistory,
        plan: List<PlanItem>,
        settings: CoachSettings,
        today: LocalDate,
        priorAdvice: List<Advice>,
    ): List<Advice> {
        val advice = Coach.evaluate(
            history, plan.on(mode), settings, today, priorAdvice.forSide(mode),
            readinessAloneDeloads = mode == TrainingMode.BODYBUILDING,
        )
        val kept = when (mode) {
            TrainingMode.POWERLIFTING -> advice
            TrainingMode.BODYBUILDING -> advice.filter { it.type == AdviceType.RE_ENTRY || it.type == AdviceType.DELOAD }
        }
        return kept.map { it.copy(mode = mode) }
    }

    /** The welcome-back card: this side has a plan and was left more than 30 days before [today]. */
    fun welcomeBack(leftOn: LocalDate?, today: LocalDate, hasPlan: Boolean): Boolean =
        hasPlan && leftOn != null && leftOn.daysUntil(today) > WELCOME_BACK_DAYS
}
