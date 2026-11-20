package com.powertracker.ui.guide.pictures

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.powertracker.domain.guide.GuideFrame
import com.powertracker.domain.guide.GuideSample
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.coach.CardDetail
import com.powertracker.ui.coach.CoachCard
import com.powertracker.ui.coach.CoachContent
import com.powertracker.ui.coach.CoachState
import com.powertracker.ui.guide.PHONE_STATUS_BAR
import kotlin.math.roundToInt

/**
 * Screen 6 (1.16): the real Coach screen ([CoachContent]) with one of the side's holding-back cards a step, in the rule
 * table's order ([GuideSample.advice]). One instance: the feed is keyed by card, so the screen's own motion slides one
 * card out and the next in.
 */
@Composable
internal fun HoldsBackPicture(mode: TrainingMode, frame: GuideFrame) {
    val state = remember(mode, frame.stepIndex) { coachState(mode, frame.stepIndex) }
    Box(Modifier.fillMaxSize().padding(top = PHONE_STATUS_BAR)) {
        CoachContent(state, onApply = {}, onDismiss = {})
    }
}

/** The weeks of a stall card's trend the Coach screen shows: its view model's last six. */
private const val TREND_WEEKS = 6

private fun percent(factor: Double) = (factor * 100).roundToInt()

/** The Coach feed at step [index]: that card alone, its figures as the Coach view model works them out. */
private fun coachState(mode: TrainingMode, index: Int): CoachState {
    val advice = GuideSample.advice(mode)[index]
    return CoachState(loading = false, cards = listOf(CoachCard(advice, detail(mode, advice))))
}

private fun detail(mode: TrainingMode, advice: Advice): CardDetail = when (val p = advice.payload) {
    is AdvicePayload.Deload -> CardDetail.Deload(percent(p.volumeFactor), percent(p.loadFactor))
    is AdvicePayload.ReEntry -> CardDetail.ReEntry(percent(p.loadFactor))
    is AdvicePayload.Skipped -> CardDetail.Skip(p)
    is AdvicePayload.Swap -> CardDetail.Swap(p)
    is AdvicePayload.Accessories -> {
        val trend = GuideSample.stallTrend(mode).takeLast(TREND_WEEKS)
        CardDetail.Stall(trend.map { it.second.toFloat() }, GuideSample.stallE1rm(mode) ?: trend.lastOrNull()?.second, p.items)
    }
    else -> throw IllegalArgumentException("${advice.type} is not a card of screen 6")
}
