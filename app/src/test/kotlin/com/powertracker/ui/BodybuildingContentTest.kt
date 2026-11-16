package com.powertracker.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleFocus
import com.powertracker.ui.design.assertTextsFit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.robolectric.annotation.GraphicsMode
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

    // ---- Stretch and squeeze (stage 4): on the bodybuilding side's Today card only ----

    private val squat = Exercise(1, "Squat", Category.SQUAT, null, emptyList(), isBuiltIn = true)
    private val plank = Exercise(9, "Front plank", Category.ACCESSORY, null, emptyList(), isBuiltIn = true)
    private val tyreFlip = Exercise(900, "Tyre flip", Category.ACCESSORY, null, emptyList(), isBuiltIn = false)
    private val twoSets = listOf(stored(1, 26.0, 11, 8.0), stored(2, 26.0, 10, 8.0))

    private fun lines(name: String) = Library.focus(name) as MuscleFocus.Lines

    private var shownCard: MutableState<Pair<Exercise, TrainingMode>>? = null

    /** One exercise card on [mode]'s side, in that side's theme. Called again, it swaps the card in place. */
    private fun showCard(exercise: Exercise, mode: TrainingMode) {
        shownCard?.let {
            it.value = exercise to mode
            compose.waitForIdle()
            return
        }
        val card = mutableStateOf(exercise to mode).also { shownCard = it }
        compose.setContent {
            val (shown, side) = card.value
            PowertrackerTheme(mode = side) {
                TodayContent(TodayState(date, loading = false, sessionId = 1, items = listOf(item(shown, twoSets)), mode = side), TodayActions())
            }
        }
    }

    private fun top(text: String) = compose.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot().top

    /** From the exercise's name to the Missed button: the height a cue block would add to. */
    private fun cardSpan(name: String) = top("Missed") - top(name)

    @Test
    fun bodybuildingCard_showsStretchAndSqueeze() {
        val squatLines = lines("Squat")
        showCard(squat, TrainingMode.BODYBUILDING)

        compose.onNodeWithText("STRETCH", useUnmergedTree = true).assertExists()
        compose.onNodeWithText(squatLines.stretch, useUnmergedTree = true).assertExists()
        compose.onNodeWithText("SQUEEZE", useUnmergedTree = true).assertExists()
        compose.onNodeWithText(squatLines.squeeze, useUnmergedTree = true).assertExists()
        // As the mockup: after the logged sets, before the weight entry; the stretch first, each label beside its line.
        val lastSet = compose.onNodeWithText("Set 2: 26 kg × 10", substring = true).getUnclippedBoundsInRoot().bottom
        val weight = compose.onNodeWithContentDescription("Weight, kilograms").getUnclippedBoundsInRoot().top
        val order = listOf(top("STRETCH"), top(squatLines.stretch), top("SQUEEZE"), top(squatLines.squeeze))
        assertTrue("the cues come after the logged sets", order.all { it >= lastSet })
        assertTrue("the cues come before the weight entry", order.all { it < weight })
        assertTrue("the stretch comes before the squeeze", order[1] < order[3])
        assertEquals("STRETCH sits beside its line", order[0].value, order[1].value, 4f)
        assertEquals("SQUEEZE sits beside its line", order[2].value, order[3].value, 4f)
    }

    @Test
    fun bodybuildingCard_restoredRowUnderCatalogueName_showsLines() {
        // A row restored from a backup under a catalogue name, in another case: matched as its guide is.
        showCard(squat.copy(name = "squat"), TrainingMode.BODYBUILDING)
        compose.onNodeWithText(lines("Squat").stretch, useUnmergedTree = true).assertExists()
        compose.onNodeWithText(lines("Squat").squeeze, useUnmergedTree = true).assertExists()
    }

    @Test
    fun bodybuildingCard_noFocus_showsNothing() {
        // The plank has NoFocus: the card shows nothing (the guide sheet carries the fixed line).
        assertTrue(Library.focus("Front plank") is MuscleFocus.NoFocus)
        showCard(plank, TrainingMode.BODYBUILDING)
        compose.onNodeWithText("STRETCH", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("SQUEEZE", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText(Library.NO_FOCUS_LINE, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("muscle-cues", useUnmergedTree = true).assertDoesNotExist()
        val plankSpan = cardSpan("Front plank")

        // The lifter's own exercise: nothing either.
        showCard(tyreFlip, TrainingMode.BODYBUILDING)
        compose.onNodeWithText("STRETCH", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("SQUEEZE", useUnmergedTree = true).assertDoesNotExist()
        val ownSpan = cardSpan("Tyre flip")

        // No gap: both cards are exactly as tall as the same card on the powerlifting side, which has no cues at all.
        showCard(plank, TrainingMode.POWERLIFTING)
        val powerliftingSpan = cardSpan("Front plank")
        assertEquals(powerliftingSpan.value, plankSpan.value, 0.5f)
        assertEquals(powerliftingSpan.value, ownSpan.value, 0.5f)

        // And the guard: a card that does have lines is taller by its cue block.
        showCard(squat.copy(category = Category.ACCESSORY), TrainingMode.BODYBUILDING)
        assertTrue(cardSpan("Squat") > powerliftingSpan)
    }

    @Test
    fun powerliftingCard_showsNoCues() {
        showCard(squat, TrainingMode.POWERLIFTING)
        compose.onNodeWithText("Squat", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("STRETCH", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("SQUEEZE", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText(lines("Squat").stretch, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("muscle-cues", useUnmergedTree = true).assertDoesNotExist()

        // Switching the side shows the cues at once, and switching back hides them.
        showCard(squat, TrainingMode.BODYBUILDING)
        compose.onNodeWithText("STRETCH", useUnmergedTree = true).assertExists()
        showCard(squat, TrainingMode.POWERLIFTING)
        compose.onNodeWithText("STRETCH", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("muscle-cues", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w360dp-h2400dp-xhdpi")
    fun bodybuildingCard_largeFont_wrapsWithoutClipping() {
        val squatLines = lines("Squat")
        var fontScale = 0f
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                fontScale = LocalDensity.current.fontScale
                PowertrackerTheme(mode = TrainingMode.BODYBUILDING) {
                    TodayContent(
                        TodayState(date, loading = false, sessionId = 1, items = listOf(item(squat, twoSets)), mode = TrainingMode.BODYBUILDING),
                        TodayActions(),
                    )
                }
            }
        }
        compose.waitForIdle()
        assertEquals(1.3f, fontScale, 0.001f)
        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        for (text in listOf("STRETCH", "SQUEEZE", squatLines.stretch, squatLines.squeeze)) {
            val node = compose.onNodeWithText(text, useUnmergedTree = true)
            val b = node.getUnclippedBoundsInRoot()
            assertTrue("\"$text\" fits across the screen", b.left >= screen.left && b.right <= screen.right)
            val n = node.fetchSemanticsNode()
            assertEquals("\"$text\" is not clipped", n.size.height.toFloat(), n.boundsInRoot.height, 0.5f)
            assertEquals("\"$text\" is not cut at the side", n.size.width.toFloat(), n.boundsInRoot.width, 0.5f)
            val layouts = mutableListOf<TextLayoutResult>()
            n.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            val layout = layouts.single()
            assertFalse("\"$text\" is cut short", layout.hasVisualOverflow)
            assertFalse("\"$text\" is ellipsised", (0 until layout.lineCount).any { layout.isLineEllipsized(it) })
            if (text.length > 20) assertTrue("\"$text\" is shown whole", layout.layoutInput.text.text == text)
        }
        // At this size the long line wraps rather than running on.
        val stretch = mutableListOf<TextLayoutResult>().also {
            compose.onNodeWithText(squatLines.stretch, useUnmergedTree = true).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(it)
        }.single()
        assertTrue("the stretch line wraps at font scale 1.3 on a 360 dp phone", stretch.lineCount > 1)
        compose.assertTextsFit()
    }
}
