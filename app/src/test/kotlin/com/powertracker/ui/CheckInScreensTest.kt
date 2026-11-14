package com.powertracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.powertracker.domain.coach.GroupNote
import com.powertracker.domain.coach.NoteReason
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.MuscleRating
import com.powertracker.domain.model.NewExercise
import com.powertracker.domain.model.SetAddition
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.progress.ExerciseTrend
import com.powertracker.domain.progress.MuscleRow
import com.powertracker.ui.checkin.CheckInActions
import com.powertracker.ui.checkin.CheckInContent
import com.powertracker.ui.checkin.CheckInState
import com.powertracker.ui.checkin.CheckInStep
import com.powertracker.ui.checkin.SavedLine
import com.powertracker.ui.checkin.noteText
import com.powertracker.ui.coach.CardDetail
import com.powertracker.ui.coach.CoachCard
import com.powertracker.ui.coach.CoachContent
import com.powertracker.ui.coach.CoachState
import com.powertracker.ui.progress.ProgressContent
import com.powertracker.ui.progress.ProgressState
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Stage 3's words on Android: the check-in's three steps, the Coach's due card and two new cards, and Progress. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class CheckInScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate(2026, 11, 11)

    private fun show(mode: TrainingMode = TrainingMode.BODYBUILDING, content: @Composable () -> Unit) =
        compose.setContent { PowertrackerTheme(mode = mode, darkTheme = true, content = content) }

    // ---- the check-in ----

    @Test
    fun stepOne_asksWhichAreLagging_overTheEighteenGroups_nothingTicked() {
        show { CheckInContent(CheckInState(loading = false, available = true), CheckInActions()) }
        compose.onNodeWithText("Which body parts are lagging?").assertExists()
        compose.onNodeWithText("Leave a body part unticked if it is on track.").assertExists()
        for (g in MuscleGroup.entries) compose.onNodeWithTag("muscle-option-${g.name}").assertIsOff()
        compose.onNodeWithText("Next").assertExists()
        compose.onNodeWithText("Step 1 of 2", ignoreCase = true).assertExists()
    }

    @Test
    fun stepOne_tickingAGroup_tellsTheViewModel() {
        val ticked = mutableListOf<MuscleGroup>()
        show { CheckInContent(CheckInState(loading = false, available = true), CheckInActions(onLagging = { ticked += it })) }
        compose.onNodeWithTag("muscle-option-CHEST").performClick()
        assertEquals(listOf(MuscleGroup.CHEST), ticked)
    }

    @Test
    fun stepTwo_asksWhichAreGrowingWell_andTheLaggingOnesCannotBeTicked() {
        val ticked = mutableListOf<MuscleGroup>()
        show {
            CheckInContent(
                CheckInState(loading = false, available = true, step = CheckInStep.GROWING, lagging = setOf(MuscleGroup.CHEST), growingWell = setOf(MuscleGroup.QUADS)),
                CheckInActions(onGrowing = { ticked += it }),
            )
        }
        compose.onNodeWithText("Which are growing well?").assertExists()
        compose.onNodeWithText("Body parts marked lagging can't also be growing well.").assertExists()
        compose.onNodeWithTag("muscle-option-CHEST").assertIsNotEnabled()
        compose.onNodeWithTag("muscle-option-QUADS").assertIsOn()
        compose.onNodeWithTag("muscle-option-CHEST").performClick()
        assertEquals(emptyList<MuscleGroup>(), ticked)
        compose.onNodeWithText("Save").assertExists()
        compose.onNodeWithText("Back").assertExists()
        compose.onNodeWithText("Step 2 of 2", ignoreCase = true).assertExists()
    }

    @Test
    fun saved_saysWhatEachLaggingGroupGot() {
        val lines = listOf(
            SavedLine(MuscleGroup.CHEST, "More work suggested on Coach"),
            SavedLine(MuscleGroup.LATS, noteText(GroupNote(MuscleGroup.LATS, NoteReason.LATER, 16))),
        )
        var coach = 0
        var done = 0
        show {
            CheckInContent(
                CheckInState(loading = false, available = true, step = CheckInStep.SAVED, lines = lines, hasAdvice = true),
                CheckInActions(onSeeCoach = { coach++ }, onDone = { done++ }),
            )
        }
        compose.onNodeWithText("Check-in saved").assertExists()
        compose.onNodeWithText("Chest").assertExists()
        compose.onNodeWithText("More work suggested on Coach").assertExists()
        compose.onNodeWithText("The coach adds to three body parts at a time. It will look at lats again at your next check-in.").assertExists()
        compose.onNodeWithText("See Coach").performClick()
        compose.onNodeWithText("Done").performClick()
        assertEquals(1 to 1, coach to done)
    }

    @Test
    fun saved_withoutAdvice_hasNoSeeCoach() {
        show { CheckInContent(CheckInState(loading = false, available = true, step = CheckInStep.SAVED), CheckInActions()) }
        compose.onNodeWithText("Check-in saved").assertExists()
        compose.onNodeWithText("See Coach").assertDoesNotExist()
        compose.onNodeWithText("Done").assertExists()
    }

    @Test
    fun everyNote_hasItsLine() {
        assertEquals("Upper back already gets 20 sets a week, the most the coach adds.", noteText(GroupNote(MuscleGroup.UPPER_BACK, NoteReason.AT_CEILING, 20)))
        assertEquals("No other adductors exercise uses the equipment in your plan.", noteText(GroupNote(MuscleGroup.ADDUCTORS, NoteReason.NO_EXERCISE, 0)))
        assertEquals("Every session that could take one already has 10 exercises.", noteText(GroupNote(MuscleGroup.CHEST, NoteReason.FULL, 10)))
        assertEquals("The coach adds to three body parts at a time. It will look at side delts again at your next check-in.", noteText(GroupNote(MuscleGroup.SIDE_DELTS, NoteReason.LATER, 9)))
        assertEquals("More upper back work would take a body part past 20 sets a week.", noteText(GroupNote(MuscleGroup.UPPER_BACK, NoteReason.WOULD_PASS_CEILING, 19)))
    }

    // ---- Coach ----

    private val moreWorkSets = Advice(
        AdviceType.MORE_WORK, null, today, "You marked chest as lagging.", 0,
        AdvicePayload.MoreWork(
            MuscleGroup.CHEST,
            listOf(SetAddition(DayOfWeek.MONDAY, 1, "Incline DB press", 3, 4), SetAddition(DayOfWeek.THURSDAY, 2, "DB press", 3, 4)), null, 12, 14,
        ),
        id = 1, mode = TrainingMode.BODYBUILDING,
    )
    private val moreWorkNew = Advice(
        AdviceType.MORE_WORK, null, today, "You marked chest as lagging.", 0,
        AdvicePayload.MoreWork(MuscleGroup.CHEST, emptyList(), NewExercise(DayOfWeek.THURSDAY, "Pec deck", 3, 8, 12, 8.0), 12, 15),
        id = 2, mode = TrainingMode.BODYBUILDING,
    )
    private val swap = Advice(
        AdviceType.SWAP, null, today, "Leg extension hasn't moved in 3 weeks. Same weight and no more reps since 21 Oct.", 0,
        AdvicePayload.Swap(5, "Leg extension", MuscleGroup.QUADS, LocalDate(2026, 10, 21), listOf("Bulgarian split squat", "Split squat", "Lunge")),
        id = 3, mode = TrainingMode.BODYBUILDING,
    )

    private fun cards(vararg advice: Advice) = advice.map { a ->
        CoachCard(a, when (val p = a.payload) {
            is AdvicePayload.MoreWork -> CardDetail.MoreWork(p)
            is AdvicePayload.Swap -> CardDetail.Swap(p)
            else -> error("not here")
        })
    }

    @Test
    fun theDueCard_hasItsWordsAndOneButton_atTheTop() {
        var checkIns = 0
        show { CoachContent(CoachState(loading = false, checkInDue = true, cards = cards(swap)), onApply = {}, onDismiss = {}, onCheckIn = { checkIns++ }) }
        compose.onNodeWithText("Time for your check-in").assertExists()
        compose.onNodeWithText("Four weeks on this plan. Mark the body parts that stand out; everything else stays on track.").assertExists()
        compose.onNodeWithText("Check in").performClick()
        assertEquals(1, checkIns)
        // No dismiss of its own: the only Dismiss is the swap card's.
        assertEquals(1, compose.onAllNodesWithText("Dismiss").fetchSemanticsNodes().size)
        // Above the advice.
        val due = compose.onNodeWithText("Time for your check-in").fetchSemanticsNode().boundsInRoot.top
        val stuck = compose.onNodeWithText("Leg extension hasn't moved in 3 weeks").fetchSemanticsNode().boundsInRoot.top
        assertTrue(due < stuck)
    }

    @Test
    fun aMoreWorkCard_withSets_saysWhichSetsAndTheWeeklyCount() {
        var applied = 0L
        show { CoachContent(CoachState(loading = false, cards = cards(moreWorkSets)), onApply = { applied = it }, onDismiss = {}) }
        compose.onNodeWithText("MORE WORK", substring = true, ignoreCase = true).assertExists()
        compose.onNodeWithText("Chest is lagging").assertExists()
        compose.onNodeWithText("One more set of Incline DB press on Monday (3 → 4)", substring = true).assertExists()
        compose.onNodeWithText("One more set of DB press on Thursday (3 → 4)", substring = true).assertExists()
        compose.onNodeWithText("Chest: 12 → 14 sets a week").assertExists()
        compose.onNodeWithText("Dismiss").assertExists()
        compose.onNodeWithText("Add to plan").performClick()
        assertEquals(1L, applied)
    }

    @Test
    fun aMoreWorkCard_withANewExercise_saysWhatAndWhen() {
        show { CoachContent(CoachState(loading = false, cards = cards(moreWorkNew)), onApply = {}, onDismiss = {}) }
        compose.onNodeWithText("Add Pec deck on Thursday, 3 × 8–12 @ 8").assertExists()
        compose.onNodeWithText("Chest: 12 → 15 sets a week").assertExists()
    }

    @Test
    fun aSwapCard_offersEachAlternative() {
        var swapped: Pair<Long, String>? = null
        var dismissed = 0L
        show { CoachContent(CoachState(loading = false, cards = cards(swap)), onApply = {}, onDismiss = { dismissed = it }, onSwap = { id, name -> swapped = id to name }) }
        compose.onNodeWithText("STUCK", substring = true, ignoreCase = true).assertExists()
        compose.onNodeWithText("Leg extension hasn't moved in 3 weeks").assertExists()
        compose.onNodeWithText("Same weight and no more reps since 21 Oct. Another exercise for quads can get it moving.").assertExists()
        compose.onNodeWithText("Swap for Bulgarian split squat").assertExists()
        compose.onNodeWithText("Swap for Lunge").assertExists()
        compose.onNodeWithText("Swap for Split squat").performClick()
        assertEquals(3L to "Split squat", swapped)
        compose.onNodeWithText("Dismiss").performClick()
        assertEquals(3L, dismissed)
    }

    @Test
    fun aSwapCard_countsTheWeeksSinceItsBaseline() {
        val older = swap.copy(payload = (swap.payload as AdvicePayload.Swap).copy(since = LocalDate(2026, 10, 14)))
        show { CoachContent(CoachState(loading = false, cards = cards(older)), onApply = {}, onDismiss = {}) }
        compose.onNodeWithText("Leg extension hasn't moved in 4 weeks").assertExists()
    }

    // ---- Progress ----

    private fun row(group: MuscleGroup, ratings: List<MuscleRating>, done: Int, planned: Int, up: Int, tracked: Int) =
        MuscleRow(group, ratings.mapIndexed { i, r -> LocalDate(2026, 8, 1 + i * 28 % 28) to r }, done, planned, up, tracked)

    @Test
    fun progress_bodyParts_showRatingSetsAndExercises() {
        var checkIns = 0
        val rows = listOf(
            row(MuscleGroup.CHEST, listOf(MuscleRating.LAGGING, MuscleRating.ON_TRACK, MuscleRating.GROWING_WELL, MuscleRating.LAGGING), 11, 14, 2, 3),
            row(MuscleGroup.QUADS, listOf(MuscleRating.ON_TRACK), 6, 9, 0, 0),
        )
        show {
            ProgressContent(
                ProgressState(loading = false, hasData = true, mode = TrainingMode.BODYBUILDING, muscles = rows, checkedIn = true,
                    trends = listOf(ExerciseTrend(1, "DB press", emptyList(), goingUp = null))),
                onRange = {}, onOpenLift = {}, onCheckIn = { checkIns++ },
            )
        }
        compose.onNodeWithText("Body parts", ignoreCase = true).assertExists()
        compose.onNodeWithText("Chest").assertExists()
        compose.onNodeWithText("11 of 14 planned sets in the last 7 days").assertExists()
        compose.onNodeWithText("2 of 3 exercises going up").assertExists()
        compose.onNodeWithText("Lagging · On track · Growing well").assertExists()
        compose.onAllNodesWithText("Lagging").fetchSemanticsNodes().let { assertEquals(1, it.size) }
        compose.onNodeWithText("On track").assertExists()
        compose.onNodeWithText("6 of 9 planned sets in the last 7 days").assertExists()
        // Quads have no exercise with enough sessions to tell: no going-up line for them.
        assertEquals(1, compose.onAllNodesWithText("going up", substring = true).fetchSemanticsNodes().size)
        compose.onNodeWithText("Your first check-in is four weeks after your first bodybuilding session. You can check in any time.").assertDoesNotExist()
        compose.onNodeWithText("Check in").performClick()
        assertEquals(1, checkIns)
        // The stage 1 rows are still there, under the body parts.
        compose.onNodeWithText("DB press").assertExists()
    }

    @Test
    fun progress_beforeTheFirstCheckIn_saysWhenItComes() {
        show {
            ProgressContent(
                ProgressState(loading = false, hasData = true, mode = TrainingMode.BODYBUILDING, muscles = listOf(row(MuscleGroup.CHEST, emptyList(), 3, 6, 0, 1)),
                    trends = listOf(ExerciseTrend(1, "DB press", emptyList(), goingUp = null))),
                onRange = {}, onOpenLift = {},
            )
        }
        compose.onNodeWithText("Check in").assertExists()
        compose.onNodeWithText("Your first check-in is four weeks after your first bodybuilding session. You can check in any time.").assertExists()
        // Never rated: no rating label.
        compose.onNodeWithText("On track").assertDoesNotExist()
        compose.onNodeWithText("0 of 1 exercise going up").assertExists()
    }

    @Test
    fun saveFailing_showsItsLine() {
        show { CheckInContent(CheckInState(loading = false, available = true, step = CheckInStep.GROWING, failed = true), CheckInActions()) }
        compose.onNodeWithText("The check-in could not be saved. Nothing was changed.").assertExists()
    }

    /** With no plan there is nothing a check-in could add to: no button, no first-check-in line. */
    @Test
    fun progress_withAnEmptyPlan_offersNoCheckIn() {
        show {
            ProgressContent(
                ProgressState(loading = false, hasData = false, mode = TrainingMode.BODYBUILDING, muscles = listOf(row(MuscleGroup.CHEST, listOf(MuscleRating.LAGGING), 0, 0, 0, 0))),
                onRange = {}, onOpenLift = {},
            )
        }
        compose.onNodeWithText("Check in").assertDoesNotExist()
        compose.onNodeWithText("Your first check-in is four weeks after your first bodybuilding session. You can check in any time.").assertDoesNotExist()
        compose.onNodeWithText("No exercises in your plan yet").assertExists()
    }

    @Test
    fun progress_onThePowerliftingSide_hasNoCheckIn() {
        show(TrainingMode.POWERLIFTING) { ProgressContent(ProgressState(loading = false, hasData = false), onRange = {}, onOpenLift = {}) }
        compose.onNodeWithText("Check in").assertDoesNotExist()
        compose.onNodeWithText("Body parts", ignoreCase = true).assertDoesNotExist()
    }
}
