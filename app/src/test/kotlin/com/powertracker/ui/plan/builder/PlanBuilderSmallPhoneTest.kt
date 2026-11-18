package com.powertracker.ui.plan.builder

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import com.powertracker.domain.library.Library
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.plan.BuilderAnswers
import com.powertracker.domain.plan.PlanBuilder
import com.powertracker.ui.design.assertTextsFitInEveryWindow
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.datetime.DayOfWeek
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The builder's bar at 320 dp with 130% text (1.14): "Use this plan" and each step's Next label are whole. The font scale
 * is set before the activity exists, as the other large-text checks do.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h6000dp-xhdpi")
class PlanBuilderSmallPhoneTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<ComponentActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
    }

    private fun show(state: PlanBuilderState) {
        scenario?.close()
        RuntimeEnvironment.setFontScale(1.3f)
        scenario = ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
            activity.setContent {
                PowertrackerTheme(mode = TrainingMode.BODYBUILDING) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        PlanBuilderContent(state, PlanBuilderActions(isTicked = { pickKey(it) in state.ticked }))
                    }
                }
            }
        }
        compose.waitForIdle()
        assertEquals(1.3f, RuntimeEnvironment.getApplication().resources.configuration.fontScale, 0.001f)
    }

    private val days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

    private fun state(step: BuilderStep, ticked: Boolean = true): PlanBuilderState {
        val rows = listOf("DB press", "Lat pulldown", "Leg press").mapIndexed { i, name ->
            val lib = Library.find(name)!!
            Exercise(100L + i, lib.name, lib.category, lib.parentLift, lib.muscles, isBuiltIn = true)
        }
        val picks = if (ticked) rows.map(PlanBuilderViewModel::entryFor).associate { pickKey(it) to PlanBuilderViewModel.chosen(it) } else emptyMap()
        return PlanBuilderState(
            loading = false, mode = TrainingMode.BODYBUILDING, step = step, dayCount = 4, days = days,
            exercises = rows, recentIds = rows.map { it.id }, ticked = picks,
            draft = if (step == BuilderStep.DRAFT) PlanBuilder.build(BuilderAnswers(days.sorted(), picks.values.map { it.pick }, emptyList())) else null,
        )
    }

    private fun assertBarWhole(what: String, label: String) {
        assertTrue("$what: $label shown", compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty())
        val checked = compose.assertTextsFitInEveryWindow(320f * 2)
        assertTrue("$what: $label checked", label in checked)
    }

    /**
     * Only the bar's [label]: laid out whole, on its lines and not cut. The exercises and body parts steps have filter
     * rows that scroll sideways, whose chips past the edge [assertTextsFitInEveryWindow] would count as cut.
     */
    private fun assertLabelWhole(what: String, label: String) {
        val nodes = compose.onAllNodes(hasText(label) and hasClickAction().not(), useUnmergedTree = true).fetchSemanticsNodes()
            .filter { it.config.getOrNull(SemanticsProperties.Text)?.joinToString() == label }
        assertTrue("$what: $label shown", nodes.isNotEmpty())
        for (node in nodes) {
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
            val layout = layouts.single()
            assertFalse("$what: \"$label\" has more lines than it may show", layout.multiParagraph.didExceedMaxLines)
            assertFalse("$what: \"$label\" is ellipsised", (0 until layout.lineCount).any { layout.isLineEllipsized(it) })
            val shown = node.boundsInRoot
            assertTrue("$what: \"$label\" is cut by a parent", shown.width >= node.size.width - 1f && shown.height >= node.size.height - 1f)
            assertTrue("$what: \"$label\" runs off the screen", node.positionInRoot.x >= -0.5f && node.positionInRoot.x + node.size.width <= 640.5f)
        }
    }

    @Test
    fun theDraftsBar_keepsUseThisPlanWhole() {
        show(state(BuilderStep.DRAFT))
        assertBarWhole("the draft", USE_THIS_PLAN)
    }

    @Test
    fun theOtherStepsBars_keepTheirLabelsWhole() {
        show(state(BuilderStep.DAYS))
        assertLabelWhole("days", "Next")
        show(state(BuilderStep.EXERCISES, ticked = false))
        assertLabelWhole("exercises, nothing ticked", CHOOSE_FOR_ME)
        assertLabelWhole("exercises, nothing ticked", "Back")
        show(state(BuilderStep.EXERCISES))
        assertLabelWhole("exercises", "Next")
        show(state(BuilderStep.BODY_PARTS))
        assertLabelWhole("body parts", "Skip")
    }
}
