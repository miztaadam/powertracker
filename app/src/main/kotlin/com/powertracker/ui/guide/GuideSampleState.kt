package com.powertracker.ui.guide

import com.powertracker.data.StoredSet
import com.powertracker.domain.guide.Guide
import com.powertracker.domain.guide.GuideSample
import com.powertracker.domain.guide.GuideScreenId
import com.powertracker.domain.guide.GuideStep
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plates.PlateLoader
import com.powertracker.ui.today.SetEditor
import com.powertracker.ui.today.SkippedToday
import com.powertracker.ui.today.TodayItem
import com.powertracker.ui.today.TodayState

/**
 * The domain's sample lifter as the real screens' state: what each picture's content component is fed at each step.
 * Nothing here is stored; the ids are the sample's own ([GuideSample.exercises], [GuideSample.plan]).
 */
internal object GuideSampleState {
    /** The sample session's id: any id, as nothing is stored. */
    private const val SESSION_ID = 1L

    /**
     * Today on [GuideSample.TODAY], in session, at [step] of screen 4: the sample lift's card with nothing in it, then
     * the weight, the reps and the RPE put in one by one, then the set logged (ticked), then the day's last planned
     * exercise skipped. Each item is built as [com.powertracker.ui.today.TodayViewModel] builds it for a lifter with no
     * history: no weight yet, the plan's reps and target RPE, the plan's sets, the plates for the weight.
     */
    fun today(mode: TrainingMode, step: GuideStep): TodayState {
        val steps = Guide.steps(GuideScreenId.TODAY, mode)
        val at = steps.indexOf(step)
        require(at >= 0) { "$step is not a step of the Today picture" }
        fun reached(s: GuideStep) = at >= steps.indexOf(s)

        val sample = GuideSample.today(mode)
        val exercises = GuideSample.exercises(mode)
        val planned = GuideSample.plan(mode).filter { it.weekday == sample.date.dayOfWeek }.sortedBy { it.order }
        val liftId = exercises.values.first { it.name == sample.exerciseId }.id

        fun item(plan: PlanItem): TodayItem {
            val lift = plan.exerciseId == liftId
            val editor = if (lift) {
                SetEditor(
                    weightKg = if (reached(GuideStep.TODAY_WEIGHT)) sample.doneWeightKg else null,
                    reps = if (reached(GuideStep.TODAY_REPS)) sample.doneReps else plan.reps,
                    rpe = if (reached(GuideStep.TODAY_RPE)) sample.doneRpe else plan.targetRpe,
                )
            } else {
                SetEditor(null, plan.reps, plan.targetRpe)
            }
            val sets = if (lift && reached(GuideStep.TODAY_TICKED)) {
                listOf(StoredSet(1, SESSION_ID, LoggedSet(liftId, sample.doneWeightKg, sample.doneReps, sample.doneRpe, failed = false, missPoint = null, isTopSet = false)))
            } else {
                emptyList()
            }
            return TodayItem(
                exercise = exercises.getValue(plan.exerciseId),
                planned = plan,
                suggestion = null,
                sets = sets,
                editor = editor,
                targetSets = plan.sets,
                perSide = editor.weightKg?.let { PlateLoader.perSide(it, changePlates = false) },
                lastWeightKg = sets.lastOrNull()?.set?.weightKg,
            )
        }

        // Skip today: the day's last planned exercise, which has nothing logged, leaves the list for the "Skipped today" line.
        val skipped = if (reached(GuideStep.TODAY_SKIP)) planned.lastOrNull { it.exerciseId != liftId } else null
        return TodayState(
            date = sample.date,
            loading = false,
            sessionId = SESSION_ID,
            items = planned.filter { it != skipped }.map(::item),
            exercises = exercises.values.toList(),
            mode = mode,
            skippedToday = listOfNotNull(skipped?.let { SkippedToday(it.exerciseId, exercises.getValue(it.exerciseId).name, skipId = 1) }),
        )
    }
}
