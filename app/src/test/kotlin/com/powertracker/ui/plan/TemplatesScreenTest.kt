package com.powertracker.ui.plan

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.AdviceRepository
import com.powertracker.data.AppSettings
import com.powertracker.data.BackupService
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import com.powertracker.domain.plan.PlanTemplates
import com.powertracker.testing.TestDatabase
import com.powertracker.ui.AppShell
import com.powertracker.ui.design.assertTextsFit
import com.powertracker.ui.plan.builder.BUILD_MY_PLAN
import com.powertracker.ui.plan.builder.PLAN_OPTIONS
import com.powertracker.ui.plan.builder.REBUILD_MY_PLAN
import com.powertracker.ui.plan.builder.USE_THIS_PLAN
import com.powertracker.ui.plan.builder.chooseDaysText
import com.powertracker.ui.plan.templates.CANCEL
import com.powertracker.ui.plan.templates.CHOOSE_A_TEMPLATE
import com.powertracker.ui.plan.templates.REPLACE_KEPT
import com.powertracker.ui.plan.templates.TemplateStep
import com.powertracker.ui.plan.templates.TemplatesActions
import com.powertracker.ui.plan.templates.TemplatesContent
import com.powertracker.ui.plan.templates.TemplatesScreen
import com.powertracker.ui.plan.templates.TemplatesState
import com.powertracker.ui.plan.templates.planSetText
import com.powertracker.ui.plan.templates.replaceTitle
import com.powertracker.ui.plan.templates.templateDayTitles
import com.powertracker.ui.plan.templates.templateDaysLine
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.DayOfWeek.FRIDAY
import kotlinx.datetime.DayOfWeek.MONDAY
import kotlinx.datetime.DayOfWeek.SATURDAY
import kotlinx.datetime.DayOfWeek.WEDNESDAY
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Choosing a template as the lifter sees it (1.14, Task 3): the ways in, the list, the draft, the confirm and the save. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class TemplatesScreenTest {
    @get:Rule(order = 0)
    val database = TestDatabase()

    @get:Rule(order = 1)
    val compose = createComposeRule()

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
        runBlocking { settings.update { AppSettings(trainingDays = setOf(MONDAY, WEDNESDAY, FRIDAY)) } }
    }

    private fun says(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun has(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(condition: () -> Boolean) = compose.waitUntil(5_000) {
        compose.waitForIdle()
        condition()
    }

    private fun row(name: String) = runBlocking { training.exercises().first() }.first { it.name == name }
    private fun plan(mode: TrainingMode) = runBlocking { training.plan(mode).first() }

    // The ways in

    @Test
    fun anEmptyPlan_offersATemplateOnBothSides_andTheBuilderOnBodybuilding_andTheMenuHasItOnceThereIsAPlan() {
        var templates = 0
        var built = 0
        var state by mutableStateOf(PlanUiState(sections = listOf(DaySection(MONDAY, emptyList())), mode = POWERLIFTING))
        compose.setContent {
            PowertrackerTheme {
                PlanContent(state, onOpenSettings = {}, onAdd = {}, onEdit = {}, onMove = { _, _ -> }, onDelete = {}, onSetBaseline = {}, onBuild = { built++ }, onTemplates = { templates++ })
            }
        }
        // Powerlifting: the new line over Choose a template; no builder, no menu while it is empty.
        compose.onNodeWithText(EMPTY_POWERLIFTING_PLAN).assertExists()
        compose.onNodeWithText(CHOOSE_A_TEMPLATE).performClick()
        assertEquals(1, templates)
        assertFalse(says(BUILD_MY_PLAN))
        assertTrue(compose.onAllNodes(androidx.compose.ui.test.hasContentDescription(PLAN_OPTIONS)).fetchSemanticsNodes().isEmpty())

        // Bodybuilding: Choose a template beside Build my plan.
        state = PlanUiState(sections = listOf(DaySection(MONDAY, emptyList())), mode = BODYBUILDING)
        compose.waitForIdle()
        compose.onNodeWithText(EMPTY_BODYBUILDING_PLAN).assertExists()
        compose.onNodeWithText(CHOOSE_A_TEMPLATE).performClick()
        compose.onNodeWithText(BUILD_MY_PLAN).performClick()
        assertEquals(2, templates)
        assertEquals(1, built)

        // With a plan, each side's menu has Choose a template; bodybuilding keeps Rebuild my plan above it.
        val bbRow = PlanRow(PlanItem(1, MONDAY, 1, 3, 8, 8.0, 0, BODYBUILDING, 12), "Curl")
        state = PlanUiState(sections = listOf(DaySection(MONDAY, listOf(bbRow))), mode = BODYBUILDING)
        compose.waitForIdle()
        assertFalse(says(CHOOSE_A_TEMPLATE))
        compose.onNodeWithContentDescription(PLAN_OPTIONS).performClick()
        compose.onNodeWithText(REBUILD_MY_PLAN).assertExists()
        compose.onNodeWithText(CHOOSE_A_TEMPLATE).performClick()
        assertEquals(3, templates)

        val plRow = PlanRow(PlanItem(2, MONDAY, 1, 3, 5, 8.0, 0), "Squat")
        state = PlanUiState(sections = listOf(DaySection(MONDAY, listOf(plRow))), mode = POWERLIFTING)
        compose.waitForIdle()
        compose.onNodeWithContentDescription(PLAN_OPTIONS).performClick()
        assertFalse("no builder on powerlifting", says(REBUILD_MY_PLAN))
        compose.onNodeWithText(CHOOSE_A_TEMPLATE).performClick()
        assertEquals(4, templates)
    }

    /** Before the stored plan is read, Plan offers nothing: a full plan must not flash "Your plan is empty". */
    @Test
    fun beforeThePlanIsRead_nothingIsCalledEmpty() {
        val vm = PlanViewModel(training, settings)
        assertFalse(vm.state.value.emptyPlan)
        assertFalse(vm.state.value.emptySide)
    }

    // The list and the draft

    @Test
    fun theList_showsTheSidesTemplates_eachWithItsDaysWhoItSuitsAndItsDayTitles() {
        var shown by mutableStateOf(POWERLIFTING)
        compose.setContent { PowertrackerTheme(mode = shown) { TemplatesContent(TemplatesState(shown, loading = false), TemplatesActions()) } }
        for (mode in TrainingMode.entries) {
            shown = mode
            compose.waitForIdle()
            for (t in PlanTemplates.forMode(mode)) {
                compose.onNodeWithTag("template-list").performScrollToNode(hasTestTag("template-${t.id}"))
                compose.onNodeWithText(t.name).assertExists()
                compose.onNodeWithText(templateDaysLine(t)).assertExists()
                compose.onNodeWithText(templateDayTitles(t)).assertExists()
            }
            assertEquals(3, compose.onAllNodes(hasTestTag("template-pl-3") or hasTestTag("template-pl-4") or hasTestTag("template-pl-5") or hasTestTag("template-bb-3") or hasTestTag("template-bb-4") or hasTestTag("template-bb-5")).fetchSemanticsNodes().size)
        }
        assertEquals("3 days a week · New to powerlifting: 2–3 days a week", templateDaysLine(PlanTemplates.byId("pl-3")!!))
        assertEquals("Upper A · Lower A · Upper B · Lower B", templateDayTitles(PlanTemplates.byId("bb-4")!!))
    }

    private fun templates(mode: TrainingMode, onSaved: (String) -> Unit = {}, onClose: () -> Unit = {}) {
        runBlocking { settings.update { it.copy(trainingMode = mode) } }
        compose.setContent { PowertrackerTheme(mode = mode) { TemplatesScreen(mode, training, settings, onClose = onClose, onSaved = onSaved) } }
        waitFor { has("template-list") }
    }

    private fun open(id: String) {
        compose.onNodeWithTag("template-list").performScrollToNode(hasTestTag("template-$id"))
        compose.onNodeWithTag("template-$id").performClick()
        waitFor { has("draft-list") }
    }

    private fun scrollTo(tag: String) = compose.onNodeWithTag("draft-list").performScrollToNode(hasTestTag(tag))

    @Test
    fun theDraft_startsOnTheSuggestedDays_andUseThisPlanWaitsForTheRightCount() {
        templates(POWERLIFTING)
        open("pl-3")
        compose.onNodeWithTag("template-day-MONDAY").assertIsOn()
        compose.onNodeWithTag("template-day-WEDNESDAY").assertIsOn()
        compose.onNodeWithTag("template-day-FRIDAY").assertIsOn()
        compose.onNodeWithTag("template-day-TUESDAY").assertIsOff()
        compose.onNodeWithTag("templates-use").assertIsEnabled()
        assertTrue(says("Mon, Wed, Fri"))
        assertTrue(says("Monday · Squat & bench"))

        compose.onNodeWithTag("template-day-FRIDAY").performClick()
        compose.onNodeWithTag("templates-use").assertIsNotEnabled()
        assertTrue(says(chooseDaysText(3)))
        // While the count is off, the days are numbered, not named.
        assertTrue(says("Day 1 · Squat & bench"))
        compose.onNodeWithTag("template-day-SATURDAY").performClick()
        compose.onNodeWithTag("templates-use").assertIsEnabled()
        scrollTo("session-2")
        assertTrue(says("Saturday · Squat & deadlift"))

        // Back returns to the list, with nothing stored.
        compose.onNodeWithTag("templates-back").performClick()
        waitFor { has("template-list") }
        assertTrue(plan(POWERLIFTING).isEmpty())
    }

    @Test
    fun thePowerliftingDraft_showsFixedReps_noSwapOnMainLifts_andNoWeeklySets() {
        templates(POWERLIFTING)
        open("pl-3")
        val monday = hasAnyAncestor(hasTestTag("session-0"))
        compose.onNode(hasText("Squat") and monday).assertExists()
        compose.onAllNodes(hasText("3 × 5", substring = true) and monday).fetchSemanticsNodes().let { assertEquals("squat and bench", 2, it.size) }
        assertTrue("no range on powerlifting", compose.onAllNodes(hasText("–", substring = true) and monday).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithContentDescription("Swap Squat").assertDoesNotExist()
        compose.onNodeWithContentDescription("Swap Bench").assertDoesNotExist()
        compose.onNodeWithContentDescription("Swap Seated cable row").assertExists()
        compose.onAllNodes(hasText("3 × 10", substring = true) and monday).fetchSemanticsNodes().let { assertEquals("two accessories", 2, it.size) }
        assertFalse("no weekly sets on powerlifting", has("volume"))

        // Swap on an accessory opens the template's list.
        compose.onNodeWithContentDescription("Swap Seated cable row").performClick()
        waitFor { has("alternatives") }
        assertTrue(compose.onAllNodes(hasTestTag("alternative")).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun theBodybuildingDraft_showsRanges_swapEverywhere_andTheWeeklySets() {
        templates(BODYBUILDING)
        open("bb-3")
        compose.onNodeWithContentDescription("Swap Leg press").assertExists()
        assertTrue(compose.onAllNodes(hasText("8–12", substring = true)).fetchSemanticsNodes().isNotEmpty())
        scrollTo("volume")
        compose.onNodeWithTag("volume").assertExists()
    }

    @Test
    fun aSaveOnAnEmptySide_storesItAndSaysTheName() {
        var saved: String? = null
        templates(POWERLIFTING, onSaved = { saved = it })
        open("pl-3")
        compose.onNodeWithText(USE_THIS_PLAN).performClick()
        waitFor { saved != null }
        assertEquals("3-day powerlifting", saved)
        assertEquals(12, plan(POWERLIFTING).size)
    }

    @Test
    fun replacing_asksFirst_cancelKeepsThePlan_andReplaceReplacesIt() {
        val curl = row("Curl")
        runBlocking { training.savePlanItem(PlanItem(0, MONDAY, curl.id, 3, 10, 8.0, 0)) }
        var saved: String? = null
        templates(POWERLIFTING, onSaved = { saved = it })
        open("pl-4")
        compose.onNodeWithText(USE_THIS_PLAN).performClick()
        waitFor { says(replaceTitle(POWERLIFTING)) }
        compose.onNode(isDialog()).assertExists()
        assertTrue(says(REPLACE_KEPT))
        compose.onNodeWithText(CANCEL).performClick()
        waitFor { !says(replaceTitle(POWERLIFTING)) }
        assertEquals(listOf(curl.id), plan(POWERLIFTING).map { it.exerciseId })

        compose.onNodeWithText(USE_THIS_PLAN).performClick()
        waitFor { has("templates-replace") }
        compose.onNodeWithTag("templates-replace").performClick()
        waitFor { saved != null }
        assertTrue(plan(POWERLIFTING).none { it.exerciseId == curl.id })
        assertEquals("Replace your bodybuilding plan?", replaceTitle(BODYBUILDING))
    }

    // Through the app

    @Test
    fun shell_planChooseATemplate_useIt_backOnPlanWithTheSnackbar() {
        compose.setContent { PowertrackerTheme { AppShell(training, advice, settings, BackupService(db, settings)) } }
        waitFor { has("tab-plan") }
        compose.onNodeWithTag("tab-plan").performClick()
        waitFor { says(EMPTY_POWERLIFTING_PLAN) }
        compose.onNodeWithText(CHOOSE_A_TEMPLATE).performClick()
        waitFor { has("template-list") }
        assertFalse("the templates hide the tabs", has("tab-plan"))
        open("pl-3")
        compose.onNodeWithText(USE_THIS_PLAN).performClick()
        waitFor { has("tab-plan") && says(planSetText("3-day powerlifting")) }
        assertEquals(setOf(MONDAY, WEDNESDAY, FRIDAY), runBlocking { settings.settings.first() }.trainingDays)
        waitFor { says("Squat  3 × 5 @ 8") }

        // Now there is a plan, the menu reaches the templates again; Close leaves with nothing changed.
        val before = plan(POWERLIFTING)
        compose.onNodeWithContentDescription(PLAN_OPTIONS).performClick()
        compose.onNodeWithText(CHOOSE_A_TEMPLATE).performClick()
        waitFor { has("template-list") }
        compose.onNodeWithContentDescription("Close").performClick()
        waitFor { has("tab-plan") }
        assertEquals(before, plan(POWERLIFTING))
    }
}

/**
 * The list and the drafts at 320 dp with 130% text: every name and line is whole and on screen (1.14). The font scale is
 * set before the activity exists, as the other large-text checks do.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h6000dp-xhdpi")
class TemplatesSmallPhoneTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<ComponentActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
    }

    private fun show(mode: TrainingMode, content: @Composable () -> Unit) {
        scenario?.close()
        RuntimeEnvironment.setFontScale(1.3f)
        scenario = ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
            activity.setContent {
                PowertrackerTheme(mode = mode) { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() } }
            }
        }
        compose.waitForIdle()
        assertEquals(1.3f, RuntimeEnvironment.getApplication().resources.configuration.fontScale, 0.001f)
    }

    private fun draftState(id: String, strictPress: Boolean = true): TemplatesState {
        val t = PlanTemplates.byId(id)!!
        val days = PlanTemplates.suggestedDays(t.dayCount, emptySet())
        return TemplatesState(
            t.mode, loading = false, step = TemplateStep.DRAFT, chosen = t, days = days.toSet(),
            draft = PlanTemplates.draft(t, days, strictPress), strictPress = strictPress,
        )
    }

    @Test
    fun theListsAndEveryDraft_fitAt320WithLargeText() {
        for (mode in TrainingMode.entries) {
            show(mode) { TemplatesContent(TemplatesState(mode, loading = false), TemplatesActions()) }
            compose.assertTextsFit()
            for (t in PlanTemplates.forMode(mode)) {
                show(mode) { TemplatesContent(draftState(t.id), TemplatesActions()) }
                assertEquals(320f * 2, compose.onRoot().fetchSemanticsNode().boundsInRoot.width, 0.5f)
                // Every exercise of the draft is on screen, so each name is checked.
                for (e in draftState(t.id).draft!!.sessions.flatMap { it.exercises }) {
                    assertTrue("${t.id}: ${e.name} shown", compose.onAllNodesWithText(e.name).fetchSemanticsNodes().isNotEmpty())
                }
                compose.assertTextsFit()
                val broken = namesBrokenInsideAWord(draftState(t.id).draft!!.sessions.flatMap { it.exercises }.map { it.name }.distinct())
                assertTrue("${t.id} at 320 dp and 130%, broken inside a word: $broken", broken.isEmpty())
            }
        }
    }

    /** Of [names], those whose Text breaks onto a new line inside a word (after a space or a hyphen is fine). */
    private fun namesBrokenInsideAWord(names: List<String>): List<String> = names.flatMap { name ->
        compose.onAllNodes(
            androidx.compose.ui.test.SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.Text, listOf(androidx.compose.ui.text.AnnotatedString(name)),
            ),
            useUnmergedTree = true,
        ).fetchSemanticsNodes().mapNotNull { node ->
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            node.config[androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
            val layout = layouts.single()
            val inside = (0 until layout.lineCount - 1).any { line ->
                val end = layout.getLineEnd(line, visibleEnd = true)
                !(name[end] == ' ' || name[end - 1] == ' ' || name[end - 1] == '-')
            }
            if (inside) name else null
        }
    }

    @Test
    fun theEmptyPlans_fitAt320WithLargeText() {
        for (mode in TrainingMode.entries) {
            show(mode) {
                PlanContent(
                    PlanUiState(sections = listOf(DaySection(DayOfWeek.MONDAY, emptyList())), mode = mode),
                    onOpenSettings = {}, onAdd = {}, onEdit = {}, onMove = { _, _ -> }, onDelete = {}, onSetBaseline = {},
                )
            }
            compose.assertTextsFit()
        }
    }

    @Test
    fun theDraft_withSevenDaysToChooseFrom_keepsEveryChipWhole() {
        show(POWERLIFTING) { TemplatesContent(draftState("pl-5").copy(days = setOf(MONDAY, SATURDAY)), TemplatesActions()) }
        for (day in DayOfWeek.entries) compose.onNodeWithTag("template-day-${day.name}").assertExists()
        compose.assertTextsFit()
    }
}
