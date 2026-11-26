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
import com.powertracker.domain.history.LastTime
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.stack.PlateStack
import com.powertracker.ui.components.formatKg
import com.powertracker.ui.components.pastSetSpoken
import com.powertracker.ui.today.lastTimeDate
import com.powertracker.ui.today.useSetText
import androidx.compose.ui.test.assertCountEquals
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

    // ---- Last time (1.20): every set of the exercise's last session, on the Today card on both sides, where the
    // ---- stretch and squeeze lines were on the bodybuilding side (they are in the exercise's guide now) ----

    private val squat = Exercise(1, "Squat", Category.SQUAT, null, emptyList(), isBuiltIn = true)
    private val twoSets = listOf(stored(1, 26.0, 11, 8.0), stored(2, 26.0, 10, 8.0))

    private fun past(kg: Double, reps: Int, rpe: Double? = 8.0, failed: Boolean = false, unit: LoadUnit = LoadUnit.KG) =
        LoggedSet(squat.id, kg, reps, rpe, failed, null, isTopSet = false, unit = unit)

    /** Wednesday 4 November: a set, a set with no RPE, and a missed one at another weight. */
    private val lastWeek = LastTime(LocalDate(2026, 11, 4), listOf(past(100.0, 12), past(100.0, 11, rpe = null), past(102.5, 8, 9.5, failed = true)))
    /** As TalkBack reads them with two sets logged today: the third is the one to match (1.21). */
    private val lastWeekRows = listOf("Set 1: 100 kg × 12 at RPE 8", "Set 2: 100 kg × 11", "Set 3: 102.5 kg × 8 at RPE 9.5, missed, your next set")

    private var shownCard: MutableState<Triple<TrainingMode, LastTime?, List<StoredSet>>>? = null

    /** The squat's card on [mode]'s side, in that side's theme, with [last] as its last time. Called again, it swaps the card in place. */
    private fun showCard(mode: TrainingMode, last: LastTime?, sets: List<StoredSet> = twoSets) {
        shownCard?.let {
            it.value = Triple(mode, last, sets)
            compose.waitForIdle()
            return
        }
        val card = mutableStateOf(Triple(mode, last, sets)).also { shownCard = it }
        compose.setContent {
            val (side, lastTime, logged) = card.value
            PowertrackerTheme(mode = side) {
                TodayContent(TodayState(date, loading = false, sessionId = 1, items = listOf(item(squat, logged).copy(lastTime = lastTime)), mode = side), TodayActions())
            }
        }
    }

    private fun top(text: String) = compose.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot().top
    private fun bottom(text: String) = compose.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot().bottom

    /** From the exercise's name to the Missed button: the height a block between them would add to. */
    private fun cardSpan() = top("Missed") - top("Squat")

    @Test
    fun card_showsEverySetOfLastTime_betweenTheLoggedSetsAndTheWeightEntry_onBothSides() {
        for (mode in listOf(TrainingMode.BODYBUILDING, TrainingMode.POWERLIFTING)) {
            showCard(mode, lastWeek)

            compose.onNodeWithTag("last-time", useUnmergedTree = true).assertExists()
            compose.onNodeWithText("LAST TIME · WED 4 NOV", useUnmergedTree = true).assertExists()
            // One row for each set, in the order logged: a set with no RPE says none, and only the missed set says so.
            val rows = lastWeekRows.map { top(it) }
            assertEquals("$mode: the rows are in logged order", rows.sorted(), rows)
            assertEquals("$mode: each row is on its own line", rows.distinct().size, rows.size)
            // Today's two rows, last time's three and the "Use set 3" button under them (1.21), and no more
            // ("Set 3 of 3" is the card's count, not a row).
            compose.onAllNodesWithText(" kg × ", substring = true, useUnmergedTree = true).assertCountEquals(twoSets.size + lastWeekRows.size + 1)
            compose.onNodeWithText("Use set 3: 102.5 kg × 8").assertExists()
            // After today's logged sets, before the weight entry.
            val lastSet = compose.onNodeWithText("Set 2: 26 kg × 10", substring = true).getUnclippedBoundsInRoot().bottom
            val weight = compose.onNodeWithContentDescription("Weight, kilograms").getUnclippedBoundsInRoot().top
            assertTrue("$mode: the heading comes after the logged sets", top("LAST TIME · WED 4 NOV") >= lastSet)
            assertTrue("$mode: the heading is over the rows", top("LAST TIME · WED 4 NOV") < rows.first())
            assertTrue("$mode: the button is under the rows", top("Use set 3: 102.5 kg × 8") >= bottom(lastWeekRows.last()))
            assertTrue("$mode: the whole block ends above the weight entry", bottom("Use set 3: 102.5 kg × 8") <= weight)
            // The stretch and squeeze lines are not on the card, on either side.
            compose.onNodeWithText("STRETCH", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithText("SQUEEZE", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithTag("muscle-cues", useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test
    fun card_neverDoneOnThisSide_showsNothing_andLeavesNoGap() {
        showCard(TrainingMode.BODYBUILDING, null)
        compose.onNodeWithTag("last-time", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("LAST TIME", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("STRETCH", useUnmergedTree = true).assertDoesNotExist()
        val none = cardSpan()

        // A last time with no sets (never built, but never an empty box either) is nothing too.
        showCard(TrainingMode.BODYBUILDING, LastTime(LocalDate(2026, 11, 4), emptyList()))
        compose.onNodeWithTag("last-time", useUnmergedTree = true).assertDoesNotExist()
        assertEquals(none.value, cardSpan().value, 0.5f)

        // No gap: the same height on the powerlifting side, which never had anything there.
        showCard(TrainingMode.POWERLIFTING, null)
        assertEquals(none.value, cardSpan().value, 0.5f)

        // And the guard: with a last time the card is taller by its block.
        showCard(TrainingMode.POWERLIFTING, lastWeek)
        assertTrue(cardSpan() > none)
    }

    @Test
    fun card_finished_keepsLastTime_aboveTheNextButton() {
        showCard(TrainingMode.BODYBUILDING, lastWeek, twoSets + stored(3, 26.0, 9, 8.0))
        compose.onNodeWithText("Done", useUnmergedTree = true).assertExists()
        // The exercise is done: no weight entry, the button that moves on instead, and last time still there above it.
        compose.onNodeWithContentDescription("Weight, kilograms").assertDoesNotExist()
        val lastSet = compose.onNodeWithText("Set 3: 26 kg × 9", substring = true).getUnclippedBoundsInRoot().bottom
        assertTrue("last time comes after the logged sets", top("LAST TIME · WED 4 NOV") >= lastSet)
        // Nothing is being logged, so no row is marked and there is no "Use set" button.
        val unmarked = "Set 3: 102.5 kg × 8 at RPE 9.5, missed"
        assertTrue("the whole block ends above the button", bottom(unmarked) <= top("Finish session"))
        compose.onAllNodesWithText("your next set", substring = true, useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithText("Use set", substring = true, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun lastTime_fromAnotherYear_carriesTheYear_andAPlateSetReadsAsAPlateNumber() {
        showCard(TrainingMode.POWERLIFTING, LastTime(LocalDate(2025, 12, 31), listOf(past(60.0, 12), past(7.5, 10, unit = LoadUnit.PLATES))))
        compose.onNodeWithText("LAST TIME · WED 31 DEC 2025", useUnmergedTree = true).assertExists()
        // Each set in the unit it was logged in.
        compose.onNodeWithText("Set 1: 60 kg × 12 at RPE 8", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Set 2: ${PlateStack.spoken(7.5)} × 10 at RPE 8", useUnmergedTree = true).assertExists()
        assertEquals("Set 2: ${PlateStack.spoken(7.5)} × 10 at RPE 8", pastSetSpoken(2, past(7.5, 10, unit = LoadUnit.PLATES)))
        assertEquals("Wed 4 Nov", lastTimeDate(LocalDate(2026, 11, 4), date))
        assertEquals("Wed 31 Dec 2025", lastTimeDate(LocalDate(2025, 12, 31), date))
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w320dp-h2400dp-xhdpi")
    fun card_smallPhoneAndLargeFont_lastTimeWrapsWithoutClipping() {
        // Eight sets, the long kind: a half-kilo weight, two-figure reps, a half RPE, and a missed one.
        val long = LastTime(LocalDate(2026, 11, 4), (0 until 8).map { past(102.5 + it, 15 - it, 9.5, failed = it == 7) })
        var fontScale = 0f
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                fontScale = LocalDensity.current.fontScale
                PowertrackerTheme(mode = TrainingMode.BODYBUILDING) {
                    TodayContent(
                        TodayState(date, loading = false, sessionId = 1, items = listOf(item(squat, twoSets).copy(lastTime = long)), mode = TrainingMode.BODYBUILDING),
                        TodayActions(),
                    )
                }
            }
        }
        compose.waitForIdle()
        assertEquals(1.3f, fontScale, 0.001f)
        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        // The heading is whole: not clipped, not cut short, not ellipsised.
        val heading = compose.onNodeWithText("LAST TIME · WED 4 NOV", useUnmergedTree = true)
        val h = heading.fetchSemanticsNode()
        assertEquals("the heading is not clipped", h.size.height.toFloat(), h.boundsInRoot.height, 0.5f)
        assertEquals("the heading is not cut at the side", h.size.width.toFloat(), h.boundsInRoot.width, 0.5f)
        val layout = mutableListOf<TextLayoutResult>().also { h.config[SemanticsActions.GetTextLayoutResult].action?.invoke(it) }.single()
        // Whole: every character is on a line that is drawn (a heading as wide as its words reports a width overflow
        // against the room it was offered, so the width is checked by its bounds above).
        assertFalse("the heading is cut at the bottom", layout.didOverflowHeight)
        assertFalse("the heading is ellipsised", (0 until layout.lineCount).any { layout.isLineEllipsized(it) })
        assertEquals("the heading's last line ends with its last letter", "LAST TIME · WED 4 NOV".length, layout.getLineEnd(layout.lineCount - 1))
        // Every row is inside the screen and drawn whole, one under the other.
        // Two sets are logged today, so the third row is the marked one.
        val rows = (1..8).map { n ->
            compose.onNodeWithText("Set $n: ${formatKg(101.5 + n)} kg × ${16 - n} at RPE 9.5" + (if (n == 8) ", missed" else "") + (if (n == 3) ", your next set" else ""), useUnmergedTree = true)
        }
        var above = h.boundsInRoot.bottom
        for ((i, row) in rows.withIndex()) {
            val b = row.getUnclippedBoundsInRoot()
            assertTrue("row ${i + 1} fits across the screen", b.left >= screen.left && b.right <= screen.right)
            val n = row.fetchSemanticsNode()
            assertEquals("row ${i + 1} is not clipped", n.size.height.toFloat(), n.boundsInRoot.height, 0.5f)
            assertEquals("row ${i + 1} is not cut at the side", n.size.width.toFloat(), n.boundsInRoot.width, 0.5f)
            assertTrue("row ${i + 1} is below the one before", n.boundsInRoot.top >= above - 0.5f)
            above = n.boundsInRoot.bottom
        }
        // The "Use set" button under the rows is whole and inside the screen.
        val use = compose.onNodeWithText("Use set 3: 104.5 kg × 13").getUnclippedBoundsInRoot()
        assertTrue("the Use set button is inside the screen", use.left >= screen.left && use.right <= screen.right)
        assertTrue("the Use set button is below the rows", use.top >= rows.last().getUnclippedBoundsInRoot().bottom)
        compose.assertTextsFit()
    }

    // ---- 1.21: the row for the set about to be logged is marked, and "Use set N" fills its weight and reps ----

    @Test
    fun useSet_marksTheRowForTheNextSet_andPutsItsWeightAndRepsInTheFields_loggingNothing() {
        val edits = mutableListOf<Pair<Long, SetEditor>>()
        var logged = 0
        compose.setContent {
            PowertrackerTheme {
                TodayContent(
                    TodayState(date, loading = false, sessionId = 1, items = listOf(item(squat, twoSets).copy(lastTime = lastWeek)), mode = TrainingMode.POWERLIFTING),
                    TodayActions(onEdit = { id, editor -> edits += id to editor }, onLogSet = { _, _ -> logged++ }),
                )
            }
        }
        // Two sets are logged, so last time's third is the one to match: the only row marked.
        compose.onAllNodesWithText("your next set", substring = true, useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithText("Set 3: 102.5 kg × 8 at RPE 9.5, missed, your next set", useUnmergedTree = true).assertExists()
        assertEquals("Use set 3: 102.5 kg × 8", useSetText(3, lastWeek.sets[2]))
        compose.onNodeWithText("Use set 3: 102.5 kg × 8").performClick()
        // The weight and the reps of that row, the effort as it was in the field; nothing logged.
        assertEquals(listOf(squat.id to SetEditor(102.5, 8, 8.0)), edits)
        assertEquals(0, logged)
    }

    @Test
    fun useSet_pastLastTimesSets_orInAnotherUnit_hasNoButton() {
        // Three sets last time and three logged today, with a fourth asked for: none to match.
        val oneSet = LastTime(LocalDate(2026, 11, 4), listOf(past(100.0, 12)))
        showCard(TrainingMode.POWERLIFTING, oneSet)
        compose.onNodeWithText("Set 1: 100 kg × 12 at RPE 8", useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText("your next set", substring = true, useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithText("Use set", substring = true, useUnmergedTree = true).assertCountEquals(0)

        // The set to match was logged as a plate number; the squat counts in kg: it is marked, but cannot be used.
        showCard(TrainingMode.POWERLIFTING, LastTime(LocalDate(2026, 11, 4), listOf(past(100.0, 12), past(100.0, 11), past(7.5, 10, unit = LoadUnit.PLATES))))
        compose.onNodeWithText("Set 3: ${PlateStack.spoken(7.5)} × 10 at RPE 8, your next set", useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText("Use set", substring = true, useUnmergedTree = true).assertCountEquals(0)

        // The same set in kg can.
        showCard(TrainingMode.POWERLIFTING, LastTime(LocalDate(2026, 11, 4), listOf(past(100.0, 12), past(100.0, 11), past(97.5, 10))))
        compose.onNodeWithText("Use set 3: 97.5 kg × 10").assertExists()
    }

    /** A plate-counted machine's card with [last] as its last time and [logged] plate sets done today. */
    private fun platesCard(count: PlateCount, last: LastTime, logged: Int) = item(squat.copy(plateCount = count), (1..logged).map { StoredSet(it.toLong(), 1, past(7.0, 12, unit = LoadUnit.PLATES)) })
        .copy(editor = SetEditor(7.0, 12, 8.0, LoadUnit.PLATES), unit = LoadUnit.PLATES, plateCount = count, lastTime = last)

    @Test
    fun useSet_aPlateNumberOffTheMachinesGrid_isMarked_butHasNoButton() {
        // Plate 7½ was logged when the machine counted half steps; it counts whole plates now.
        val last = LastTime(LocalDate(2026, 11, 4), listOf(past(7.5, 12, unit = LoadUnit.PLATES), past(8.0, 10, unit = LoadUnit.PLATES)))
        val shown = mutableStateOf(platesCard(PlateCount.WHOLE, last, logged = 0))
        compose.setContent {
            PowertrackerTheme { TodayContent(TodayState(date, loading = false, sessionId = 1, items = listOf(shown.value)), TodayActions()) }
        }
        compose.onNodeWithText("Set 1: ${PlateStack.spoken(7.5)} × 12 at RPE 8, your next set", useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText("Use set", substring = true, useUnmergedTree = true).assertCountEquals(0)
        // With half steps on, the same row can be used.
        shown.value = platesCard(PlateCount.HALF, last, logged = 0)
        compose.waitForIdle()
        compose.onNodeWithText("Use set 1: ${PlateStack.text(7.5)} × 12").assertExists()
        // And on whole plates the second row, a whole plate, can.
        shown.value = platesCard(PlateCount.WHOLE, last, logged = 1)
        compose.waitForIdle()
        compose.onNodeWithText("Use set 2: ${PlateStack.text(8.0)} × 10").assertExists()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w320dp-h2400dp-xhdpi")
    fun useSet_theLongestLabel_isWholeOnASmallPhoneAtALargeFont() {
        // Ten sets logged of a machine on half steps: "Use set 11: plate 12½ × 15", the longest the button gets.
        val last = LastTime(LocalDate(2026, 11, 4), (1..11).map { past(12.5, 15, 9.5, unit = LoadUnit.PLATES) })
        val label = "Use set 11: ${PlateStack.text(12.5)} × 15"
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                PowertrackerTheme(mode = TrainingMode.BODYBUILDING) {
                    TodayContent(
                        TodayState(date, loading = false, sessionId = 1, items = listOf(platesCard(PlateCount.HALF, last, logged = 10).copy(targetSets = 12)), mode = TrainingMode.BODYBUILDING),
                        TodayActions(),
                    )
                }
            }
        }
        compose.waitForIdle()
        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        val button = compose.onNodeWithText(label).getUnclippedBoundsInRoot()
        assertTrue("the button is inside the screen", button.left >= screen.left && button.right <= screen.right)
        // Every letter of the label is on a line that is drawn: a one-line text that runs out of room drops its tail.
        val text = compose.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode()
        val layout = mutableListOf<TextLayoutResult>().also { text.config[SemanticsActions.GetTextLayoutResult].action?.invoke(it) }.single()
        assertEquals("the label is whole", label.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
        assertFalse("the label is cut at the bottom", layout.didOverflowHeight)
        // The marked row, the eleventh, says "NEXT" and is whole too.
        compose.onNodeWithText("Set 11: ${PlateStack.spoken(12.5)} × 15 at RPE 9.5, your next set", useUnmergedTree = true).assertExists()
        compose.assertTextsFit()
    }
}
