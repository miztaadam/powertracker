package com.powertracker.ui.design

import androidx.compose.runtime.Composable
import com.powertracker.domain.coach.GroupNote
import com.powertracker.domain.coach.NoteReason
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.SetAddition
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.checkin.CheckInActions
import com.powertracker.ui.checkin.CheckInContent
import com.powertracker.ui.checkin.CheckInState
import com.powertracker.ui.checkin.CheckInStep
import com.powertracker.ui.checkin.MORE_WORK_SUGGESTED
import com.powertracker.ui.checkin.SavedLine
import com.powertracker.ui.checkin.noteText
import com.powertracker.ui.coach.CardDetail
import com.powertracker.ui.coach.CoachCard
import com.powertracker.ui.coach.CoachContent
import com.powertracker.ui.coach.CoachState
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

// Stage 3's mockups: the same lifter as the other bodybuilding captures checks in on Wednesday 11 November, four weeks
// on, marking chest, side delts, rear delts and calves lagging and quads growing well.

private val LAGGING = setOf(MuscleGroup.CHEST, MuscleGroup.SIDE_DELTS, MuscleGroup.REAR_DELTS, MuscleGroup.CALVES)
private val GROWING = setOf(MuscleGroup.QUADS)

@Composable
fun CheckInLaggingShowcase() =
    CheckInContent(CheckInState(loading = false, available = true, lagging = LAGGING), CheckInActions())

@Composable
fun CheckInGrowingShowcase() =
    CheckInContent(CheckInState(loading = false, available = true, step = CheckInStep.GROWING, lagging = LAGGING, growingWell = GROWING), CheckInActions())

@Composable
fun CheckInSavedShowcase() {
    val lines = listOf(
        SavedLine(MuscleGroup.CHEST, MORE_WORK_SUGGESTED),
        SavedLine(MuscleGroup.SIDE_DELTS, MORE_WORK_SUGGESTED),
        SavedLine(MuscleGroup.REAR_DELTS, MORE_WORK_SUGGESTED),
        SavedLine(MuscleGroup.CALVES, noteText(GroupNote(MuscleGroup.CALVES, NoteReason.LATER, 6))),
    )
    CheckInContent(
        CheckInState(loading = false, available = true, step = CheckInStep.SAVED, lagging = LAGGING, growingWell = GROWING, lines = lines, hasAdvice = true),
        CheckInActions(),
    )
}

/** Coach with a check-in due, a lagging chest's extra sets, and a stuck lat pulldown. */
@Composable
fun CoachCheckInShowcase() {
    val moreWork = AdvicePayload.MoreWork(
        MuscleGroup.CHEST,
        listOf(SetAddition(DayOfWeek.MONDAY, 1, "Incline DB press", 3, 4), SetAddition(DayOfWeek.WEDNESDAY, 1, "Incline DB press", 3, 4)),
        null, 12, 14,
    )
    val swap = AdvicePayload.Swap(2, "Lat pulldown", MuscleGroup.LATS, LocalDate(2026, 10, 14), listOf("Machine pulldown", "Close-grip pulldown", "Underhand pulldown"))
    val cards = listOf(
        CoachCard(
            Advice(AdviceType.MORE_WORK, null, SHOWCASE_DATE, "You marked chest as lagging.", 0, moreWork, id = 1, mode = TrainingMode.BODYBUILDING),
            CardDetail.MoreWork(moreWork),
        ),
        CoachCard(
            Advice(AdviceType.SWAP, null, SHOWCASE_DATE, "Lat pulldown hasn't moved in 4 weeks.", 0, swap, id = 2, mode = TrainingMode.BODYBUILDING),
            CardDetail.Swap(swap),
        ),
    )
    CoachContent(CoachState(loading = false, cards = cards, checkInDue = true), onApply = {}, onDismiss = {})
}
