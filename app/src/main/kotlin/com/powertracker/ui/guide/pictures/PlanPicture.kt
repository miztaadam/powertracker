package com.powertracker.ui.guide.pictures

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.powertracker.domain.guide.GuideFrame
import com.powertracker.domain.guide.GuideSample
import com.powertracker.domain.guide.GuideStep
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.guide.PHONE_STATUS_BAR
import com.powertracker.ui.plan.DaySection
import com.powertracker.ui.plan.PlanContent
import com.powertracker.ui.plan.PlanRow
import com.powertracker.ui.plan.PlanUiState
import com.powertracker.ui.plan.templates.TemplateStep
import com.powertracker.ui.plan.templates.TemplatesActions
import com.powertracker.ui.plan.templates.TemplatesContent
import com.powertracker.ui.plan.templates.TemplatesState

/** Which real screen a step of screen 3 is drawn with. */
private enum class PlanScreenShown { EMPTY_PLAN, TEMPLATES, FILLED_PLAN }

private fun shownAt(step: GuideStep): PlanScreenShown = when (step) {
    GuideStep.PLAN_EMPTY -> PlanScreenShown.EMPTY_PLAN
    GuideStep.PLAN_TEMPLATES, GuideStep.PLAN_DRAFT, GuideStep.PLAN_SWAP -> PlanScreenShown.TEMPLATES
    GuideStep.PLAN_FILLED -> PlanScreenShown.FILLED_PLAN
    else -> throw IllegalArgumentException("$step is not a step of the plan picture")
}

/**
 * Screen 3 (1.16): the empty Plan screen ([PlanContent]), then the real templates ([TemplatesContent]) through the list,
 * the 3-day template's draft on its weekdays, and that draft with one exercise swapped ([GuideSample.swap]), then the
 * Plan screen with the saved week ([GuideSample.filledPlan]). Each tap is ringed on its control just before its result
 * (the guide's ring). The templates' three steps are one instance, so the draft's rows change in place; a step onto
 * another screen fades it in with the move, as the app's own navigation fades.
 *
 * The real Swap opens a sheet of alternatives, its own window; the picture shows the draft before and after it.
 */
@Composable
internal fun PlanPicture(mode: TrainingMode, frame: GuideFrame) {
    val shown = shownAt(frame.step)
    // A step that brings on another screen: it fades in over the move.
    val arriving = frame.stepIndex > 0 && frame.progress < 1.0 &&
        shownAt(com.powertracker.domain.guide.Guide.steps(com.powertracker.domain.guide.GuideScreenId.PLAN, mode)[frame.stepIndex - 1]) != shown
    val alpha = if (arriving) frame.progress.toFloat() else 1f
    Box(Modifier.fillMaxSize().padding(top = PHONE_STATUS_BAR).graphicsLayer { this.alpha = alpha }) {
        key(shown) {
            when (shown) {
                PlanScreenShown.EMPTY_PLAN -> EmptyPlan(mode)
                PlanScreenShown.TEMPLATES -> {
                    val state = remember(mode, frame.step) { templatesState(mode, frame.step) }
                    val actions = remember { TemplatesActions() }
                    TemplatesContent(state, actions)
                }
                PlanScreenShown.FILLED_PLAN -> {
                    val state = remember(mode) { planState(mode, GuideSample.filledPlan(mode), GuideSample.filledExercises(mode)) }
                    PlanContent(state, onOpenSettings = {}, onAdd = {}, onEdit = {}, onMove = { _, _ -> }, onDelete = {}, onSetBaseline = {})
                }
            }
        }
    }
}

/** The Plan screen of a side with no plan yet: its template button (and on bodybuilding the builder's). */
@Composable
internal fun EmptyPlan(mode: TrainingMode) {
    val state = remember(mode) { planState(mode, emptyList(), GuideSample.exercises(mode)) }
    PlanContent(state, onOpenSettings = {}, onAdd = {}, onEdit = {}, onMove = { _, _ -> }, onDelete = {}, onSetBaseline = {})
}

/**
 * The Plan screen's state as [com.powertracker.ui.plan.PlanViewModel] builds it for [plan] on [mode]'s side: one card
 * per day with items (the side's training days are the template's), the main lifts for "Known bests".
 */
private fun planState(mode: TrainingMode, plan: List<PlanItem>, exercises: Map<Long, Exercise>): PlanUiState {
    val byDay = plan.groupBy { it.weekday }
    val days = byDay.keys.sorted()
    return PlanUiState(
        sections = days.map { day ->
            DaySection(day, byDay.getValue(day).sortedBy { it.order }.map { PlanRow(it, exercises.getValue(it.exerciseId).name) })
        },
        exercises = exercises.values.sortedBy { it.id },
        mainExercises = exercises.values.sortedBy { it.id }.filter { it.isMainLift },
        trainingDays = days.toSet(),
        mode = mode,
        loaded = true,
    )
}

/** The templates at [step]: the side's list; then the 3-day template's draft on the suggested weekdays; then swapped. */
private fun templatesState(mode: TrainingMode, step: GuideStep): TemplatesState {
    val list = TemplatesState(mode, loading = false)
    if (step == GuideStep.PLAN_TEMPLATES) return list
    val draft = GuideSample.draft(mode, swapped = step == GuideStep.PLAN_SWAP)
    return list.copy(
        step = TemplateStep.DRAFT,
        chosen = GuideSample.template(mode),
        days = draft.sessions.map { it.day }.toSet(),
        draft = draft,
    )
}
