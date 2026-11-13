package com.powertracker.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.powertracker.data.AppSettings
import com.powertracker.data.StoredSet
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.progress.ExerciseTrend
import com.powertracker.ui.coach.CoachContent
import com.powertracker.ui.coach.CoachState
import com.powertracker.ui.plan.DaySection
import com.powertracker.ui.plan.EMPTY_BODYBUILDING_PLAN
import com.powertracker.ui.plan.PlanContent
import com.powertracker.ui.plan.PlanUiState
import com.powertracker.ui.plan.SettingsContent
import com.powertracker.ui.progress.ProgressContent
import com.powertracker.ui.progress.ProgressState
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.SetEditor
import com.powertracker.ui.today.TodayActions
import com.powertracker.ui.today.TodayContent
import com.powertracker.ui.today.TodayItem
import com.powertracker.ui.today.TodayState
import com.powertracker.ui.today.targetReason
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What the bodybuilding side's screens draw, from their content composables. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class BodybuildingContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val date = LocalDate(2026, 11, 11)
    private val press = Exercise(7, "Incline dumbbell press", Category.ACCESSORY, null, emptyList(), isBuiltIn = true)
    private val bench = Exercise(2, "Bench", Category.BENCH, null, emptyList(), isBuiltIn = true)

    private fun stored(id: Long, kg: Double, reps: Int, rpe: Double?, failed: Boolean = false) =
        StoredSet(id, 1, LoggedSet(press.id, kg, reps, rpe, failed, null, isTopSet = false))

    private fun item(exercise: Exercise, sets: List<StoredSet>, suggestion: Advice? = null) = TodayItem(
        exercise = exercise,
        planned = PlanItem(1, DayOfWeek.WEDNESDAY, exercise.id, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12),
        suggestion = suggestion,
        sets = sets,
        editor = SetEditor(26.0, 10, 8.0),
        targetSets = 3,
        perSide = null,
        lastWeightKg = 26.0,
    )

    private val target = Advice(
        AdviceType.TARGET, null, date, "Last time: 26 × 11, 10, 9. Beat it. Coach suggests 26 × 8–12 @ 8.", 0,
        AdvicePayload.Target(press.id, 26.0, 8, 8.0, repsHigh = 12), id = 5, mode = TrainingMode.BODYBUILDING,
    )

    @Test
    fun suggestionSentence_stripsTheRangeForm_andThePowerliftingFormAsBefore() {
        assertEquals("Last time: 26 × 11, 10, 9. Beat it.", targetReason(target))
        assertEquals(
            "Last time: 26 × 12, 12, 12. Coach suggests 22 × 8–12 @ 8. Easing back in: lighter load.".let { reason ->
                targetReason(target.copy(reason = reason))
            },
            "Last time: 26 × 12, 12, 12. Easing back in: lighter load.",
        )
        assertEquals("Last time: 150 × 5 @ 7.", targetReason(target.copy(reason = "Last time: 150 × 5 @ 7. Coach suggests 155 × 5 @ 8.")))
    }

    @Test
    fun today_bodybuildingCard_hasTheRangeHeadline_noTopSetChip_noStallQuestion_butMissedStillWorks() {
        val sets = listOf(stored(1, 24.0, 12, 8.0), stored(2, 26.0, 6, 10.0, failed = true))
        compose.setContent {
            PowertrackerTheme(mode = TrainingMode.BODYBUILDING) {
                TodayContent(
                    TodayState(date, loading = false, sessionId = 1, items = listOf(item(press, sets, target)), mode = TrainingMode.BODYBUILDING),
                    TodayActions(),
                )
            }
        }
        compose.onNodeWithText("26 kg × 8–12 @ 8").assertExists()
        compose.onNodeWithText("Last time: 26 × 11, 10, 9. Beat it.").assertExists()
        compose.onNodeWithText("Top set", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("miss-row-2").assertDoesNotExist()
        compose.onNodeWithText("Where did it stall?").assertDoesNotExist()
        compose.onNodeWithText("Missed").assertExists()
        compose.onNodeWithText("3 × 8–12 @ 8").assertDoesNotExist() // the active card shows the coach, not the plan line
    }

    @Test
    fun today_powerliftingCard_stillAsksWhereItStalled() {
        val sets = listOf(stored(1, 24.0, 12, 8.0), stored(2, 26.0, 6, 10.0, failed = true))
        compose.setContent {
            PowertrackerTheme {
                TodayContent(TodayState(date, loading = false, sessionId = 1, items = listOf(item(bench, sets))), TodayActions())
            }
        }
        compose.onNodeWithText("Top set", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("miss-row-2").assertExists()
    }

    @Test
    fun today_sideNote_andWelcomeBack_withBothButtons() {
        var resumed = 0
        var goals = 0
        compose.setContent {
            PowertrackerTheme(mode = TrainingMode.BODYBUILDING) {
                TodayContent(
                    TodayState(
                        date, loading = false, sessionId = 1, items = listOf(item(bench, emptyList())), mode = TrainingMode.POWERLIFTING,
                        sideNote = "Today's session is on the powerlifting side. Bodybuilding starts with your next session.", welcomeBack = true,
                    ),
                    TodayActions(onResumePlan = { resumed++ }, onGoalsChanged = { goals++ }),
                )
            }
        }
        compose.onNodeWithText("Today's session is on the powerlifting side. Bodybuilding starts with your next session.").assertExists()
        compose.onNodeWithText("Welcome back to the better side.").assertExists()
        compose.onNodeWithText("Resume my plan").performClick()
        compose.onNodeWithText("My goals have changed").performClick()
        assertEquals(1 to 1, resumed to goals)
    }

    @Test
    fun coach_welcomeBackCard_atTheTop() {
        var goals = 0
        compose.setContent {
            PowertrackerTheme(mode = TrainingMode.BODYBUILDING) {
                CoachContent(CoachState(loading = false, welcomeBack = true), onApply = {}, onDismiss = {}, onGoalsChanged = { goals++ })
            }
        }
        compose.onNodeWithText("Welcome back to the better side.").assertExists()
        compose.onNodeWithText("Resume my plan").assertExists()
        compose.onNodeWithText("My goals have changed").performClick()
        assertEquals(1, goals)
    }

    @Test
    fun plan_emptyBodybuildingPlan_saysSo() {
        compose.setContent {
            PowertrackerTheme(mode = TrainingMode.BODYBUILDING) {
                PlanContent(
                    PlanUiState(sections = listOf(DaySection(DayOfWeek.MONDAY, emptyList())), mode = TrainingMode.BODYBUILDING),
                    onOpenSettings = {}, onAdd = {}, onEdit = {}, onMove = { _, _ -> }, onDelete = {}, onSetBaseline = {},
                )
            }
        }
        compose.onNodeWithText(EMPTY_BODYBUILDING_PLAN).assertExists()
    }

    @Test
    fun settings_trainingDays_editTheActiveSidesDays() {
        val start = AppSettings(trainingMode = TrainingMode.BODYBUILDING, trainingDays = setOf(DayOfWeek.MONDAY), bodybuildingTrainingDays = setOf(DayOfWeek.TUESDAY))
        var settings = start
        compose.setContent {
            PowertrackerTheme(mode = TrainingMode.BODYBUILDING) {
                SettingsContent(settings, onUpdate = { f -> settings = f(settings) }, onExport = {}, onRestore = {}, onBack = {})
            }
        }
        compose.onNodeWithText("Thu").performClick()
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), settings.bodybuildingTrainingDays)
        assertEquals(setOf(DayOfWeek.MONDAY), settings.trainingDays)
        // And on the powerlifting side, its own days.
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), start.copy(trainingMode = TrainingMode.POWERLIFTING).withActiveTrainingDays(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)).trainingDays)
    }

    @Test
    fun progress_bodybuildingList_rowsWithBestSetsAndArrows_noCharts() {
        val opened = mutableListOf<MainLift>()
        val openedExercises = mutableListOf<Long>()
        fun best(kg: Double, reps: Int, day: Int) = LocalDate(2026, 11, day) to LoggedSet(1, kg, reps, 8.0, false, null, false)
        compose.setContent {
            PowertrackerTheme(mode = TrainingMode.BODYBUILDING) {
                ProgressContent(
                    ProgressState(
                        loading = false, mode = TrainingMode.BODYBUILDING,
                        trends = listOf(
                            ExerciseTrend(2, "Bench", listOf(best(60.0, 10, 1), best(60.0, 12, 8)), goingUp = true),
                            ExerciseTrend(7, "Incline dumbbell press", listOf(best(26.0, 11, 4), best(26.0, 10, 9)), goingUp = false),
                            ExerciseTrend(8, "Cable fly", emptyList(), goingUp = null),
                        ),
                    ),
                    onRange = {}, onOpenLift = { opened += it }, onOpenExercise = { openedExercises += it },
                )
            }
        }
        compose.onNodeWithText("60 × 10 · 60 × 12").assertExists()
        compose.onNodeWithText("26 × 11 · 26 × 10").assertExists()
        compose.onNodeWithContentDescription("Going up", substring = true).assertExists()
        compose.onNodeWithContentDescription("Not going up", substring = true).assertExists()
        assertEquals(1, compose.onAllNodesWithText("Not logged yet").fetchSemanticsNodes().size)
        compose.onNodeWithText("Total (S+B+D)").assertDoesNotExist()
        compose.onNodeWithText("Weekly best e1RM").assertDoesNotExist()
        // Every row opens the exercise's own history, a main lift's too (fix-round ruling B).
        compose.onNodeWithText("Bench").performClick()
        compose.onNodeWithText("Cable fly").performClick()
        assertEquals(listOf(2L, 8L), openedExercises)
        assertEquals(emptyList<MainLift>(), opened)
        compose.onNode(hasTestTag("mode-label")).assertExists()
    }
}
