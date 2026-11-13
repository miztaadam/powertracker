package com.powertracker.ui.design

import androidx.compose.runtime.Composable
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plan.BuilderAnswers
import com.powertracker.domain.plan.PlanBuilder
import com.powertracker.ui.plan.builder.BuilderStep
import com.powertracker.ui.plan.builder.PlanBuilderActions
import com.powertracker.ui.plan.builder.PlanBuilderContent
import com.powertracker.ui.plan.builder.PlanBuilderState
import com.powertracker.ui.plan.builder.PlanBuilderViewModel
import com.powertracker.ui.plan.builder.pickKey
import kotlinx.datetime.DayOfWeek

// The plan builder's four steps (stage 2 plan, Task 2), for a lifter planning four days who does seven exercises now
// and most wants bigger side delts and biceps: the worked example of the builder's report.

private val SHOWCASE_PICKS = listOf("DB press", "Lat pulldown", "Leg press", "RDL", "Lateral raise", "Cable curl", "Hip thrust")
private val SHOWCASE_DAYS = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
private val SHOWCASE_PRIORITIES = listOf(MuscleGroup.SIDE_DELTS, MuscleGroup.BICEPS)

private fun builderState(step: BuilderStep): PlanBuilderState {
    // The picks are stored rows (planned or logged), so the browser lists them first, under "Your exercises".
    val rows = SHOWCASE_PICKS.mapIndexed { i, name ->
        val lib = Library.find(name)!!
        Exercise(100L + i, lib.name, lib.category, lib.parentLift, lib.muscles, isBuiltIn = true)
    }
    val ticked = rows.map(PlanBuilderViewModel::entryFor).associate { pickKey(it) to PlanBuilderViewModel.chosen(it) }
    val draft = PlanBuilder.build(BuilderAnswers(SHOWCASE_DAYS.sorted(), ticked.values.map { it.pick }, SHOWCASE_PRIORITIES))
    return PlanBuilderState(
        loading = false,
        mode = TrainingMode.BODYBUILDING,
        step = step,
        dayCount = 4,
        days = SHOWCASE_DAYS,
        exercises = rows,
        recentIds = rows.map { it.id },
        ticked = ticked,
        priorities = SHOWCASE_PRIORITIES,
        draft = if (step == BuilderStep.DRAFT) draft else null,
        hasPlan = true,
    )
}

@Composable
fun PlanBuilderShowcase(step: BuilderStep) {
    val state = builderState(step)
    PlanBuilderContent(state, PlanBuilderActions(isTicked = { pickKey(it) in state.ticked }))
}
