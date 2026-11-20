package com.powertracker.ui.guide.pictures

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.powertracker.domain.guide.GuideEditor
import com.powertracker.domain.guide.GuideFrame
import com.powertracker.domain.guide.GuideSample
import com.powertracker.domain.history.HistoryStats
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plates.PlateLoader
import com.powertracker.ui.guide.PHONE_STATUS_BAR
import com.powertracker.ui.today.SetEditor
import com.powertracker.ui.today.TodayActions
import com.powertracker.ui.today.TodayContent
import com.powertracker.ui.today.TodayItem
import com.powertracker.ui.today.TodayState

/**
 * Screen 5 (1.16): Today on each next session of the sample lift, one step a session ([GuideSample.targetAdvice]): the
 * lift's card with the coach's OPEN target and its reason, and the fields put in to follow it (on bodybuilding the reps
 * to beat, which climb, then the weight). One instance through the steps; the fields roll from one session's to the
 * next through the domain's in-between values ([GuideSample.targetEditor]).
 */
@Composable
internal fun TargetsPicture(mode: TrainingMode, frame: GuideFrame) {
    val editor = GuideSample.targetEditor(mode, frame.stepIndex, frame.progress)
    val state = remember(mode, frame.stepIndex, editor) { targetsState(mode, frame.stepIndex, editor) }
    val actions = remember { TodayActions() }
    Box(Modifier.fillMaxSize().padding(top = PHONE_STATUS_BAR)) { TodayContent(state, actions) }
}

/** The sample session's id: any id, as nothing is stored. */
private const val SESSION_ID = 1L

/**
 * Today on the date target [index] is for, in session, as [com.powertracker.ui.today.TodayViewModel] builds it: the
 * lift's plan item for that weekday, the OPEN target as its suggestion, no sets yet, the last top set before that day
 * as the weight a typo is judged against, and the fields at [shown].
 */
private fun targetsState(mode: TrainingMode, index: Int, shown: GuideEditor): TodayState {
    val advice = GuideSample.targetAdvice(mode)[index]
    val target = advice.payload as AdvicePayload.Target
    val date = advice.createdAt
    val exercises = GuideSample.exercises(mode)
    val plan = GuideSample.plan(mode).first { it.exerciseId == target.exerciseId && it.weekday == date.dayOfWeek }
    val history = GuideSample.targetHistory(mode)
    val before = TrainingHistory(history.sessions.filter { it.date < date }, history.exercises, history.baselines)
    val top = HistoryStats(before).lastTopSet(target.exerciseId, date)?.second
    val item = TodayItem(
        exercise = exercises.getValue(target.exerciseId),
        planned = plan,
        suggestion = advice,
        sets = emptyList(),
        editor = SetEditor(shown.weightKg, shown.reps, shown.rpe),
        targetSets = plan.sets,
        perSide = shown.weightKg?.let { PlateLoader.perSide(it, changePlates = false) },
        lastWeightKg = top?.weightKg,
    )
    return TodayState(
        date = date,
        loading = false,
        sessionId = SESSION_ID,
        items = listOf(item),
        exercises = exercises.values.toList(),
        mode = mode,
    )
}
