package com.powertracker.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import com.powertracker.domain.progress.ExerciseTrend
import com.powertracker.ui.coach.CardDetail
import com.powertracker.ui.coach.CoachCard
import com.powertracker.ui.coach.CoachContent
import com.powertracker.ui.coach.CoachState
import com.powertracker.ui.progress.ProgressContent
import com.powertracker.ui.progress.ProgressState
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The bodybuilding side reads on Android as it does on the web: the easing-back-in card and the Progress rows. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class BodybuildingParityTest {
    @get:Rule
    val compose = createComposeRule()

    private fun reEntryFeed(mode: TrainingMode) {
        val reEntry = Advice(AdviceType.RE_ENTRY, null, LocalDate(2026, 11, 11), "back", 0, AdvicePayload.ReEntry(), id = 1)
        compose.setContent {
            PowertrackerTheme(mode = mode) {
                CoachContent(CoachState(loading = false, cards = listOf(CoachCard(reEntry, CardDetail.ReEntry(85)))), onApply = {}, onDismiss = {})
            }
        }
    }

    @Test
    fun coach_easingBackInCard_onTheBodybuildingSide_saysAllExercises_andLastWeights() {
        reEntryFeed(BODYBUILDING)
        compose.onNodeWithText("RE-ENTRY · ALL EXERCISES").assertExists()
        compose.onNodeWithText("of your last weights, for 7 days").assertExists()
        compose.onNodeWithText("RE-ENTRY · ALL LIFTS").assertDoesNotExist()
        compose.onNodeWithText("of your last top sets, for 7 days").assertDoesNotExist()
    }

    @Test
    fun coach_easingBackInCard_onThePowerliftingSide_isAsBefore() {
        reEntryFeed(POWERLIFTING)
        compose.onNodeWithText("RE-ENTRY · ALL LIFTS").assertExists()
        compose.onNodeWithText("of your last top sets, for 7 days").assertExists()
    }

    private fun spokenAs(expected: String) = SemanticsMatcher("spoken as \"$expected\"") { node ->
        node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString(", ") == expected
    }

    private val openHistory = SemanticsMatcher("click label Open history") { it.config.getOrNull(SemanticsActions.OnClick)?.label == "Open history" }

    @Test
    fun progress_rows_areSpokenAsOnTheWeb_nameSetsTrend_thenOpenHistory() {
        fun best(day: Int, kg: Double, reps: Int) = LocalDate(2026, 11, day) to LoggedSet(1, kg, reps, 8.0, false, null, false)
        compose.setContent {
            PowertrackerTheme(mode = BODYBUILDING) {
                ProgressContent(
                    ProgressState(
                        loading = false, hasData = true, mode = BODYBUILDING,
                        trends = listOf(
                            ExerciseTrend(1, "DB press", listOf(best(1, 26.0, 10), best(8, 26.0, 11)), goingUp = true),
                            ExerciseTrend(2, "Row", listOf(best(2, 60.0, 10), best(9, 60.0, 10)), goingUp = false),
                            ExerciseTrend(3, "Curl", emptyList(), goingUp = null),
                        ),
                    ),
                    onRange = {}, onOpenLift = {},
                )
            }
        }
        // The action is the row's click label, which a screen reader says after the description.
        compose.onNode(hasClickAction() and hasText("DB press")).assert(spokenAs("DB press. 26 × 10, 26 × 11. Going up")).assert(openHistory)
        compose.onNode(hasClickAction() and hasText("Row")).assert(spokenAs("Row. 60 × 10, 60 × 10. Not going up")).assert(openHistory)
        compose.onNode(hasClickAction() and hasText("Curl")).assert(spokenAs("Curl. Not logged yet")).assert(openHistory)
    }
}
