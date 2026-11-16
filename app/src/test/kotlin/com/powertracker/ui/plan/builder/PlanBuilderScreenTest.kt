package com.powertracker.ui.plan.builder

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.BackupService
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import com.powertracker.platform.systemToday
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.AppShell
import com.powertracker.ui.plan.EMPTY_BODYBUILDING_PLAN
import com.powertracker.ui.plan.PlanContent
import com.powertracker.ui.plan.PlanUiState
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The builder's four steps as the lifter sees them, and the three ways in (stage 2 plan, Task 2). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class PlanBuilderScreenTest {
    /** Outermost: the database closes after the compose rule has torn the screen down, and once no query is running. */
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val today = LocalDate(2026, 11, 11)
    private val db get() = database.db
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
        settings = SettingsRepository(context)
        runBlocking { settings.update { AppSettings(trainingMode = BODYBUILDING) } }
    }

    private fun says(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun has(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    // Idles first: a condition on a plain variable would otherwise never let the main looper run what the store posts.
    private fun waitFor(condition: () -> Boolean) = compose.waitUntil(5_000) {
        compose.waitForIdle()
        condition()
    }

    private fun scrollTo(tag: String) = compose.onNodeWithTag("draft-list").performScrollToNode(hasTestTag(tag))

    private fun next() = compose.onNodeWithTag("builder-next").performClick()

    @Test
    fun theFourSteps_theirWordingAndRules() {
        var closed = false
        var saved = false
        compose.setContent { PowertrackerTheme { PlanBuilderScreen(training, settings, onClose = { closed = true }, onSaved = { saved = true }, today = { today }) } }
        waitFor { says(DAYS_HEADING) }

        // 1. Days: 3 or 4, then that many weekdays. No Back on the first step (ruling P2); "Which days?" (P5).
        compose.onNodeWithText("3 days").assertIsSelected()
        compose.onNodeWithText("4 days").assertExists()
        assertTrue(says(WHICH_DAYS_HEADING.uppercase()))
        assertFalse("no Back on step 1", has("builder-back"))
        compose.onNodeWithContentDescription(CLOSE).assertExists()
        // Ruling P4: once the count is met, the chosen days in week order.
        assertTrue(says("Mon, Wed, Fri"))
        compose.onNodeWithTag("builder-next").assertIsEnabled()
        compose.onNodeWithText("4 days").performClick()
        compose.onNodeWithTag("builder-next").assertIsNotEnabled()
        assertTrue(says(chooseDaysText(4)))
        compose.onNodeWithTag("builder-day-THURSDAY").performClick()
        compose.onNodeWithTag("builder-next").assertIsEnabled()
        assertTrue(says("Mon, Wed, Thu, Fri"))
        next()

        // 2. Exercises: nothing planned or logged yet, so nothing is ticked.
        waitFor { says(WOULD_LIKE_HEADING) }
        compose.onNodeWithTag("builder-back").assertExists()
        compose.onNodeWithText("0 chosen").assertExists()
        compose.onNodeWithTag("builder-next").assertTextEquals(CHOOSE_FOR_ME)
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-Leg press"))
        compose.onNodeWithTag("exercise-Leg press").performClick()
        waitFor { says(DO_NOW_HEADING) }
        compose.onNodeWithText("1 chosen").assertExists()
        compose.onNodeWithTag("builder-next").assertTextEquals("Next")
        next()

        // 3. Body parts: the 12 groups, at most 3; Skip when none.
        waitFor { says(BODY_PARTS_HEADING) }
        compose.onNodeWithTag("builder-next").assertTextEquals("Skip")
        for (group in listOf(MuscleGroup.CHEST, MuscleGroup.BICEPS, MuscleGroup.CALVES, MuscleGroup.ABS)) {
            compose.onNodeWithTag("muscle-option-${group.name}").performClick()
        }
        waitFor { says(TOO_MANY_PRIORITIES) }
        assertFalse(has("muscle-option-FOREARMS"))
        compose.onNodeWithTag("builder-next").assertTextEquals("Next")
        next()

        // 4. The draft: four sessions, weekly sets, the guideline. The bar holds Back and Use this plan; Another
        // draft sits under the intro line (ruling P3). Each session title is a heading (P13).
        waitFor { says(DRAFT_HEADING) }
        assertTrue(says("Monday · Upper"))
        compose.onNode(hasText("Monday · Upper") and isHeading()).assertExists()
        assertTrue(compose.onAllNodesWithText("3 × 8–12 @ 8").fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithTag("draft-list").onChildren()[1].assert(hasTestTag("builder-another") or hasAnyDescendant(hasTestTag("builder-another")))
        assertTrue(says(USE_THIS_PLAN))
        compose.onNodeWithTag("builder-back").assertExists()
        compose.onNodeWithTag("builder-use").assertExists()
        // Ruling P10: every Swap is named for its exercise; the visible word stays "Swap".
        val swaps = compose.onAllNodesWithTag("swap").fetchSemanticsNodes()
        assertTrue(swaps.isNotEmpty())
        val named = compose.onAllNodes(hasContentDescriptionStarting("$SWAP ")).fetchSemanticsNodes()
        assertEquals(swaps.size, named.size)
        val legPress = com.powertracker.domain.library.Library.find("Leg press")!!
        val expected = com.powertracker.domain.plan.PlanBuilder.build(
            com.powertracker.domain.plan.BuilderAnswers(
                listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                listOf(PlanBuilderViewModel.chosen(com.powertracker.ui.library.PickerEntry(legPress.name, legPress.muscles, legPress.kind, legPress, null)).pick),
                listOf(MuscleGroup.CHEST, MuscleGroup.BICEPS, MuscleGroup.CALVES),
            ),
        )
        compose.onNodeWithContentDescription("$SWAP ${expected.sessions[0].exercises[0].name}").assertExists()
        // The visible word is not read on top of the name: no button is named just "Swap".
        assertTrue(compose.onAllNodesWithText(SWAP).fetchSemanticsNodes().isEmpty())
        scrollTo("session-THURSDAY")
        assertTrue(says("Thursday · Upper"))
        scrollTo("volume")
        assertTrue(says(WEEKLY_SETS.uppercase()))
        assertTrue(says(GUIDELINE))
        assertTrue(has("short-CALVES") || has("short-ABS") || has("short-REAR_DELTS"))
        compose.onNodeWithTag("draft-list").performScrollToNode(hasTestTag("heading"))

        // Swap the second exercise of Monday for its first alternative.
        val swapTags = compose.onAllNodesWithTag("swap", useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue(swapTags.isNotEmpty())
        compose.onAllNodesWithTag("swap")[1].performClick()
        waitFor { has("alternatives") }
        compose.onAllNodesWithTag("alternative")[0].performClick()
        waitFor { !has("alternatives") }

        compose.onNodeWithText(USE_THIS_PLAN).performClick()
        waitFor { saved }
        assertFalse(closed)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), runBlocking { settings.settings.first().bodybuildingTrainingDays })
        assertTrue(runBlocking { training.plan(BODYBUILDING).first() }.isNotEmpty())
    }

    /** From the days step to the draft, with the days as they start (Mon, Wed, Fri) and nothing changed on the way. */
    private fun toDraft() {
        waitFor { says(DAYS_HEADING) }
        next()
        waitFor { says(CHOOSE_FOR_ME) }
        next()
        waitFor { says(BODY_PARTS_HEADING) }
        next()
        waitFor { says(DRAFT_HEADING) }
    }

    private val defaultDraft get() = com.powertracker.domain.plan.PlanBuilder.build(
        com.powertracker.domain.plan.BuilderAnswers(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), emptyList(), emptyList()),
    )

    // Parity with the web draft (final review, minor 1): each row starts with its place in the session, 1, 2, 3, ...,
    // as the Plan screen's rows do.
    @Test
    fun draft_eachRowShowsItsPositionBeforeItsName() {
        compose.setContent { PowertrackerTheme { PlanBuilderScreen(training, settings, onClose = {}, onSaved = {}, today = { today }) } }
        toDraft()
        val monday = defaultDraft.sessions.first { it.day == DayOfWeek.MONDAY }
        val card = androidx.compose.ui.test.hasAnyAncestor(hasTestTag("session-MONDAY"))
        monday.exercises.forEachIndexed { i, exercise ->
            val position = compose.onAllNodes(hasText("${i + 1}") and card).fetchSemanticsNodes()
            assertEquals("one position ${i + 1} on Monday", 1, position.size)
            val name = compose.onAllNodes(hasText(exercise.name) and card).fetchSemanticsNodes().single()
            val p = position.single().boundsInRoot
            val n = name.boundsInRoot
            assertTrue("${i + 1} sits left of ${exercise.name}", p.right <= n.left)
            assertTrue("${i + 1} is on ${exercise.name}'s row", p.top < n.bottom + 40f && p.bottom > n.top - 40f)
        }
        assertTrue(compose.onAllNodes(hasText("${monday.exercises.size + 1}") and card).fetchSemanticsNodes().isEmpty())
    }

    // Final review, minor 3: a built plan is what Today shows on that plan's first training day.
    @Test
    fun aBuiltPlan_isWhatTodayShowsOnItsFirstTrainingDay() {
        val monday = LocalDate(2026, 11, 9)
        var built by androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            PowertrackerTheme(mode = BODYBUILDING) {
                if (!built) PlanBuilderScreen(training, settings, onClose = {}, onSaved = { built = true }, today = { monday })
                else com.powertracker.ui.today.TodayScreen(training, advice, settings, onOpenCoach = {}, today = { monday })
            }
        }
        toDraft()
        compose.onNodeWithText(USE_THIS_PLAN).performClick()
        waitFor { built }
        val first = defaultDraft.sessions.first().also { assertEquals(DayOfWeek.MONDAY, it.day) }
        val lead = first.exercises.first()
        val dose = "${lead.sets} × ${lead.repsLow}–${lead.repsHigh} @ ${lead.rpe.toInt()}"
        assertEquals("4 × 8–12 @ 8", dose)
        waitFor { says(lead.name) && says(dose) }
        assertTrue(says("Ready to train?"))
        val name = compose.onNodeWithText(lead.name).fetchSemanticsNode().boundsInRoot
        val line = compose.onNodeWithText(dose).fetchSemanticsNode().boundsInRoot
        assertTrue("${lead.name}'s row shows $dose", line.top < name.bottom && line.bottom > name.top)
        // The rest of Monday follows, each with its 8–12 range.
        for (e in first.exercises.drop(1)) assertTrue(e.name, says(e.name))
        assertEquals(first.exercises.size, compose.onAllNodes(androidx.compose.ui.test.hasText("8–12 @ 8", substring = true)).fetchSemanticsNodes().size)
    }

    @Test
    fun overAnExistingPlan_useThisPlanAsksFirst() {
        val curl = runBlocking { training.exercises().first() }.first { it.name == "Curl" }
        runBlocking { training.savePlanItem(PlanItem(0, DayOfWeek.TUESDAY, curl.id, 3, 8, 8.0, 0, BODYBUILDING, 12)) }
        var saved = false
        compose.setContent { PowertrackerTheme { PlanBuilderScreen(training, settings, onClose = {}, onSaved = { saved = true }, today = { today }) } }
        waitFor { says(DAYS_HEADING) }
        next()
        waitFor { says("1 chosen") }
        next()
        waitFor { says(BODY_PARTS_HEADING) }
        next()
        waitFor { says(USE_THIS_PLAN) }
        compose.onNodeWithText(USE_THIS_PLAN).performClick()
        waitFor { says(REPLACE_WARNING) }
        // Ruling P1: the title, the body, Replace and Cancel.
        assertTrue(says(REPLACE_TITLE))
        compose.onNodeWithText(REPLACE).assertExists()
        compose.onNodeWithText("Cancel").performClick()
        waitFor { !says(REPLACE_WARNING) }
        assertEquals(1, runBlocking { training.plan(BODYBUILDING).first() }.size)
        compose.onNodeWithText(USE_THIS_PLAN).performClick()
        waitFor { says(REPLACE_WARNING) }
        compose.onNodeWithText(REPLACE).performClick()
        waitFor { saved }
    }

    /** Ruling P2: the bar's Back goes one step back; the header's Close leaves from any step with nothing saved. */
    @Test
    fun barBackGoesAStepBack_andCloseLeavesFromAnyStep() {
        var closed = 0
        compose.setContent { PowertrackerTheme { PlanBuilderScreen(training, settings, onClose = { closed++ }, onSaved = {}, today = { today }) } }
        waitFor { says(DAYS_HEADING) }
        next()
        waitFor { says(WOULD_LIKE_HEADING) }
        compose.onNodeWithTag("builder-back").performClick()
        waitFor { says(DAYS_HEADING) }
        assertEquals(0, closed)
        compose.onNodeWithContentDescription(CLOSE).performClick()
        waitFor { closed == 1 }
        next(); next(); next()
        waitFor { says(DRAFT_HEADING) }
        compose.onNodeWithContentDescription(CLOSE).performClick()
        waitFor { closed == 2 }
        assertTrue(runBlocking { training.plan(BODYBUILDING).first() }.isEmpty())
    }

    /** Ruling P9: stored days that are not 3 or 4 start on "3 days" with no weekday ticked and the line showing. */
    @Test
    fun days_withTwoStored_startOnThreeDaysWithNoneTicked() {
        runBlocking { settings.update { it.copy(bodybuildingTrainingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)) } }
        compose.setContent { PowertrackerTheme { PlanBuilderScreen(training, settings, onClose = {}, onSaved = {}, today = { today }) } }
        waitFor { says(DAYS_HEADING) }
        compose.onNodeWithText("3 days").assertIsSelected()
        for (day in DayOfWeek.entries) compose.onNodeWithTag("builder-day-${day.name}").assertIsOff()
        assertTrue(says(chooseDaysText(3)))
        compose.onNodeWithTag("builder-next").assertIsNotEnabled()
    }

    /** Ruling P11: a failed save is plain text at the top of the draft, there until the next try. */
    @Test
    fun aFailedSave_showsAtTheTopOfTheDraft() {
        val draft = com.powertracker.domain.plan.PlanBuilder.build(
            com.powertracker.domain.plan.BuilderAnswers(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), emptyList(), emptyList()),
        )
        val state = PlanBuilderState(loading = false, mode = BODYBUILDING, step = BuilderStep.DRAFT, days = setOf(DayOfWeek.MONDAY), draft = draft, failed = true)
        compose.setContent { PowertrackerTheme { PlanBuilderContent(state, PlanBuilderActions()) } }
        compose.onNodeWithTag("draft-list").onChildren()[0].assert(hasText(SAVE_FAILED) or hasAnyDescendant(hasText(SAVE_FAILED)))
    }

    // Ruling P6 and P14: the "Chosen" section, and ticks that survive a search, a filter and a rotation.

    private fun exercisesStepWithSquatPlanned(restoration: StateRestorationTester? = null) {
        val squat = runBlocking { training.exercises().first() }.first { it.name == "Squat" }
        runBlocking { training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, squat.id, 3, 5, 8.0, 0)) }
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            PowertrackerTheme { PlanBuilderScreen(training, settings, onClose = {}, onSaved = {}, today = { today }) }
        }
        if (restoration != null) restoration.setContent(content) else compose.setContent(content)
        waitFor { says(DAYS_HEADING) }
        next()
        waitFor { says("1 chosen") }
    }

    private fun tick(name: String) {
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-$name"))
        compose.onNodeWithTag("exercise-$name").performClick()
    }

    /** The names under "Chosen", in order; the section is at the top of the list, so it is scrolled there first. */
    private fun chosenNames(): List<String> {
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTagStarting("chosen-"))
        return compose.onAllNodes(hasTestTagStarting("chosen-")).fetchSemanticsNodes().map { it.config[androidx.compose.ui.semantics.SemanticsProperties.TestTag].removePrefix("chosen-") }
    }

    @Test
    fun chosen_listsEveryTickedExerciseAtTheTop_andUntickingThereUnticksTheList() {
        exercisesStepWithSquatPlanned()
        assertTrue(says("CHOSEN"))
        assertEquals(listOf("Squat"), chosenNames())
        val names = listOf("Leg press", "Cable curl", "T-bar row", "Pec deck", "Lateral raise", "Face pull", "Seated leg curl")
        for (n in names) tick(n)
        waitFor { says("8 chosen") }
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("chosen-Squat"))
        // No cap: every one of the eight is under "Chosen".
        val all = listOf("Squat") + names
        for (n in all) compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("chosen-$n"))
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("chosen-Cable curl"))
        compose.onNodeWithTag("chosen-Cable curl").performClick()
        waitFor { says("7 chosen") }
        assertFalse(has("chosen-Cable curl"))
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-Cable curl"))
        compose.onNodeWithTag("exercise-Cable curl").assertIsOff()
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-Leg press"))
        compose.onNodeWithTag("exercise-Leg press").assertIsOn()
    }

    @Test
    fun ticks_surviveASearchAndAFilterChange() {
        exercisesStepWithSquatPlanned()
        tick("Leg press")
        waitFor { says("2 chosen") }
        compose.onNodeWithTag("exercise-search").performTextInput("curl")
        waitFor { has("exercise-Cable curl") }
        compose.onNodeWithTag("exercise-Cable curl").performClick()
        waitFor { says("3 chosen") }
        compose.onNodeWithTag("exercise-search").performTextClearance()
        compose.onNodeWithTag("equipment-MACHINE").performClick()
        compose.waitForIdle()
        assertTrue(says("3 chosen"))
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-Leg press"))
        compose.onNodeWithTag("exercise-Leg press").assertIsOn()
        compose.onNodeWithTag("equipment-MACHINE").performClick()
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-Cable curl"))
        compose.onNodeWithTag("exercise-Cable curl").assertIsOn()
        assertEquals(listOf("Squat", "Leg press", "Cable curl"), chosenNames())
    }

    @Test
    fun ticks_surviveARotation() {
        val restoration = StateRestorationTester(compose)
        exercisesStepWithSquatPlanned(restoration)
        tick("Leg press")
        waitFor { says("2 chosen") }
        restoration.emulateSavedInstanceStateRestore()
        waitFor { says("2 chosen") }
        assertTrue(says(DO_NOW_HEADING))
        assertEquals(listOf("Squat", "Leg press"), chosenNames())
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-Leg press"))
        compose.onNodeWithTag("exercise-Leg press").assertIsOn()
    }

    @Test
    fun onThePowerliftingSide_theBuilderClosesItself() {
        runBlocking { settings.update { it.copy(trainingMode = POWERLIFTING) } }
        var closed = false
        compose.setContent { PowertrackerTheme { PlanBuilderScreen(training, settings, onClose = { closed = true }, onSaved = {}, today = { today }) } }
        waitFor { closed }
        assertFalse(says(DAYS_HEADING))
    }

    // The ways in

    @Test
    fun plan_theEmptyBodybuildingPlan_hasBuildMyPlan_andTheMenuRebuildMyPlan() {
        var built = 0
        var state by androidx.compose.runtime.mutableStateOf(PlanUiState(mode = BODYBUILDING))
        compose.setContent {
            PowertrackerTheme {
                PlanContent(state, onOpenSettings = {}, onAdd = {}, onEdit = {}, onMove = { _, _ -> }, onDelete = {}, onSetBaseline = {}, onBuild = { built++ })
            }
        }
        compose.onNodeWithText(EMPTY_BODYBUILDING_PLAN).assertExists()
        compose.onNodeWithText(BUILD_MY_PLAN).performClick()
        assertEquals(1, built)

        val row = com.powertracker.ui.plan.PlanRow(PlanItem(1, DayOfWeek.MONDAY, 1, 3, 8, 8.0, 0, BODYBUILDING, 12), "Curl")
        state = PlanUiState(sections = listOf(com.powertracker.ui.plan.DaySection(DayOfWeek.MONDAY, listOf(row))), mode = BODYBUILDING)
        compose.waitForIdle()
        assertFalse(says(BUILD_MY_PLAN))
        compose.onNodeWithContentDescription(PLAN_OPTIONS).performClick()
        compose.onNodeWithText(REBUILD_MY_PLAN).performClick()
        assertEquals(2, built)

        // The powerlifting side has neither.
        state = PlanUiState(sections = listOf(com.powertracker.ui.plan.DaySection(DayOfWeek.MONDAY, emptyList())), mode = POWERLIFTING)
        compose.waitForIdle()
        assertFalse(says(BUILD_MY_PLAN))
        assertTrue(compose.onAllNodesWithContentDescription(PLAN_OPTIONS).fetchSemanticsNodes().isEmpty())
    }

    private fun shell() = compose.setContent { PowertrackerTheme { AppShell(training, advice, settings, BackupService(db, settings)) } }

    private fun awayForSixWeeks(mode: TrainingMode) = runBlocking {
        val curl = training.exercises().first().first { it.name == "Curl" }
        training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, curl.id, 3, 8, 8.0, 0, mode, if (mode == BODYBUILDING) 12 else null))
        val left = systemToday().minus(42, DateTimeUnit.DAY)
        settings.update {
            if (mode == BODYBUILDING) it.copy(trainingMode = BODYBUILDING, bodybuildingLeftOn = left) else it.copy(trainingMode = POWERLIFTING, powerliftingLeftOn = left)
        }
    }

    @Test
    fun shell_planButtonOpensTheBuilder_andUsingItReturnsToPlan() {
        shell()
        waitFor { has("tab-plan") }
        compose.onNodeWithTag("tab-plan").performClick()
        waitFor { says(BUILD_MY_PLAN) }
        compose.onNodeWithText(BUILD_MY_PLAN).performClick()
        waitFor { says(DAYS_HEADING) }
        assertFalse("the builder hides the tabs", has("tab-plan"))
        next()
        waitFor { says(CHOOSE_FOR_ME) }
        next()
        waitFor { says(BODY_PARTS_HEADING) }
        next()
        waitFor { says(USE_THIS_PLAN) }
        compose.onNodeWithText(USE_THIS_PLAN).performClick()
        waitFor { has("tab-plan") }
        compose.onNodeWithTag("tab-plan").assertIsSelected()
        waitFor { says("Leg press  4 × 8–12 @ 8") }
    }

    @Test
    fun shell_goalsChangedOnTheBodybuildingSide_opensTheBuilder() {
        awayForSixWeeks(BODYBUILDING)
        shell()
        waitFor { has("tab-coach") }
        compose.onNodeWithTag("tab-coach").performClick()
        waitFor { says("My goals have changed") }
        compose.onNodeWithText("My goals have changed").performClick()
        waitFor { says(DAYS_HEADING) }
    }

    @Test
    fun shell_goalsChangedOnTheBodybuildingSideFromToday_opensTheBuilder() {
        awayForSixWeeks(BODYBUILDING)
        shell()
        waitFor { says("My goals have changed") }
        compose.onNodeWithText("My goals have changed").performClick()
        waitFor { says(DAYS_HEADING) }
    }

    @Test
    fun shell_goalsChangedOnThePowerliftingSide_stillOpensPlan() {
        awayForSixWeeks(POWERLIFTING)
        shell()
        waitFor { has("tab-coach") }
        compose.onNodeWithTag("tab-coach").performClick()
        waitFor { says("My goals have changed") }
        compose.onNodeWithText("My goals have changed").performClick()
        waitFor { compose.onAllNodes(hasTestTag("tab-plan")).fetchSemanticsNodes().isNotEmpty() && says("KNOWN BESTS") }
        compose.onNodeWithTag("tab-plan").assertIsSelected()
        assertFalse(says(DAYS_HEADING))
        assertFalse(says(REBUILD_MY_PLAN))
    }
}

private fun hasContentDescriptionStarting(prefix: String) = SemanticsMatcher("content description starts with $prefix") { node ->
    node.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription) { null }?.any { it.startsWith(prefix) } == true
}

private fun hasTestTagStarting(prefix: String) = SemanticsMatcher("test tag starts with $prefix") { node ->
    node.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { null }?.startsWith(prefix) == true
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertTextEquals(text: String) =
    this.assert(hasText(text) or androidx.compose.ui.test.hasAnyDescendant(hasText(text)))

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithContentDescription(text: String) =
    onAllNodes(androidx.compose.ui.test.hasContentDescription(text))
