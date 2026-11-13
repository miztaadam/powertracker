package com.powertracker.ui.plan.builder

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
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
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import com.powertracker.platform.systemToday
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
import org.junit.After
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
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate(2026, 11, 11)
    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
        settings = SettingsRepository(context)
        runBlocking { settings.update { AppSettings(trainingMode = BODYBUILDING) } }
    }

    @After
    fun tearDown() = db.close()

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

        // 1. Days: 3 or 4, then that many weekdays.
        compose.onNodeWithText("3 days").assertIsSelected()
        compose.onNodeWithText("4 days").assertExists()
        compose.onNodeWithTag("builder-next").assertIsEnabled()
        compose.onNodeWithText("4 days").performClick()
        compose.onNodeWithTag("builder-next").assertIsNotEnabled()
        compose.onNodeWithTag("builder-day-THURSDAY").performClick()
        compose.onNodeWithTag("builder-next").assertIsEnabled()
        next()

        // 2. Exercises: nothing planned or logged yet, so nothing is ticked.
        waitFor { says(WOULD_LIKE_HEADING) }
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

        // 4. The draft: four sessions, weekly sets, the guideline, the two buttons.
        waitFor { says(DRAFT_HEADING) }
        assertTrue(says("Monday · Upper"))
        assertTrue(compose.onAllNodesWithText("3 × 8–12 @ 8").fetchSemanticsNodes().isNotEmpty())
        assertTrue(says(ANOTHER_DRAFT))
        assertTrue(says(USE_THIS_PLAN))
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
        compose.onAllNodesWithText(SWAP)[1].performClick()
        waitFor { has("alternatives") }
        compose.onAllNodesWithTag("alternative")[0].performClick()
        waitFor { !has("alternatives") }

        compose.onNodeWithText(USE_THIS_PLAN).performClick()
        waitFor { saved }
        assertFalse(closed)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), runBlocking { settings.settings.first().bodybuildingTrainingDays })
        assertTrue(runBlocking { training.plan(BODYBUILDING).first() }.isNotEmpty())
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
        compose.onNodeWithText("Cancel").performClick()
        waitFor { !says(REPLACE_WARNING) }
        assertEquals(1, runBlocking { training.plan(BODYBUILDING).first() }.size)
        compose.onNodeWithText(USE_THIS_PLAN).performClick()
        waitFor { says(REPLACE_WARNING) }
        compose.onNodeWithText(REPLACE).performClick()
        waitFor { saved }
    }

    @Test
    fun backFromTheFirstStep_closes_andTheHeaderBackGoesAStepBack() {
        var closed = false
        compose.setContent { PowertrackerTheme { PlanBuilderScreen(training, settings, onClose = { closed = true }, onSaved = {}, today = { today }) } }
        waitFor { says(DAYS_HEADING) }
        next()
        waitFor { says(WOULD_LIKE_HEADING) }
        compose.onNodeWithContentDescription("Back").performClick()
        waitFor { says(DAYS_HEADING) }
        assertFalse(closed)
        compose.onNodeWithTag("builder-back").performClick()
        waitFor { closed }
        assertTrue(runBlocking { training.plan(BODYBUILDING).first() }.isEmpty())
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

private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertTextEquals(text: String) =
    this.assert(hasText(text) or androidx.compose.ui.test.hasAnyDescendant(hasText(text)))

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithContentDescription(text: String) =
    onAllNodes(androidx.compose.ui.test.hasContentDescription(text))
