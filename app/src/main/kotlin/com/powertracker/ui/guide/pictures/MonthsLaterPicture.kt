package com.powertracker.ui.guide.pictures

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.powertracker.domain.guide.GuideFrame
import com.powertracker.domain.guide.GuideSample
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.guide.PHONE_STATUS_BAR
import com.powertracker.ui.progress.ChartRange
import com.powertracker.ui.progress.ProgressContent
import com.powertracker.ui.progress.ProgressViewModel

/**
 * Screen 7 (1.16): the real Progress screen ([ProgressContent]) built by its own view model's builders from the
 * sample's six months, drawn as of [GuideSample.monthsDate]: the first weeks (the chart draws itself in as the picture
 * opens), then week by week through each move, so the chart gains its points one at a time and the bests and the total
 * climb through what the screen would have shown. On bodybuilding the last step ends on a check-in rating the lagging
 * muscle. One instance; its state changes once a week of the run, not every frame.
 */
@Composable
internal fun MonthsLaterPicture(mode: TrainingMode, frame: GuideFrame) {
    val date = GuideSample.monthsDate(mode, frame.step, frame.progress)
    val checkIn = GuideSample.monthsCheckIn(mode, frame.step, frame.progress)
    val state = remember(mode, date, checkIn) {
        val history = GuideSample.monthsHistory(mode, date)
        if (mode == TrainingMode.BODYBUILDING) {
            ProgressViewModel.buildBodybuilding(history, GuideSample.monthsPlan(mode), date, listOfNotNull(checkIn))
        } else {
            ProgressViewModel.build(history, ChartRange.MONTHS_6, date, showStrictPress = false)
        }
    }
    Box(Modifier.fillMaxSize().padding(top = PHONE_STATUS_BAR)) {
        ProgressContent(state, onRange = {}, onOpenLift = {})
    }
}
