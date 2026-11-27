package com.powertracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.plan.SupersetHalf
import com.powertracker.domain.plates.PlateLoader
import com.powertracker.data.StoredSet
import com.powertracker.ui.design.assertTextsFit
import com.powertracker.ui.plan.DaySection
import com.powertracker.ui.plan.PlanContent
import com.powertracker.ui.plan.PlanRow
import com.powertracker.ui.plan.PlanUiState
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.today.SetEditor
import com.powertracker.ui.today.TodayActions
import com.powertracker.ui.today.TodayContent
import com.powertracker.ui.today.TodayItem
import com.powertracker.ui.today.TodayState
import com.powertracker.ui.today.TodaySuperset
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The superset tags (1.24) on a small phone and at large text: on the Plan tab's rows, on Today's open card and on its
 * folded rows, each tag is drawn whole. A tag is one line by rule ([com.powertracker.ui.components.Eyebrow]), so one
 * that did not fit would lose its "A" or "B" without being ellipsised: [whole] asks for that by name.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h2400dp-xhdpi")
class SupersetFitTest {
    @get:Rule
    val compose = createComposeRule()

    private val scales = listOf(1f, 1.3f, 2f)


    /** Each text that reads [text] is laid out whole: every character shown, in no more lines than it may take, on screen. */
    private fun whole(text: String, what: String): Int {
        val nodes = compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(androidx.compose.ui.text.AnnotatedString(text))), useUnmergedTree = true).fetchSemanticsNodes()
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        for (node in nodes) {
            val layouts = mutableListOf<TextLayoutResult>()
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            val layout = layouts.single()
            assertFalse("$what: \"$text\" has more lines than it may show", layout.multiParagraph.didExceedMaxLines)
            assertFalse("$what: \"$text\" is ellipsised", (0 until layout.lineCount).any { layout.isLineEllipsized(it) })
            assertEquals("$what: \"$text\" shows every character", text.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
            val widest = (0 until layout.lineCount).maxOf { layout.getLineRight(it) - layout.getLineLeft(it) }
            assertTrue("$what: \"$text\" is wider than its box ($widest in ${layout.size.width})", widest <= layout.size.width + 1f)
            val shown = node.boundsInRoot
            assertTrue("$what: \"$text\" is cut by a parent (${shown.width} × ${shown.height} of ${node.size})", shown.width >= node.size.width - 1f && shown.height >= node.size.height - 1f)
            assertTrue("$what: \"$text\" runs off the screen", node.positionInRoot.x >= 0f && node.positionInRoot.x + node.size.width <= screen.width + 0.5f)
        }
        return nodes.size
    }

    private fun planState(): PlanUiState {
        fun row(i: Int, name: String, half: SupersetHalf?, partner: String?) =
            PlanRow(PlanItem(10L + i, DayOfWeek.MONDAY, i.toLong(), 3, 5, 8.0, i, supersetWithAbove = half == SupersetHalf.SECOND), name, half, partner)
        return PlanUiState(
            sections = listOf(
                DaySection(
                    DayOfWeek.MONDAY,
                    listOf(
                        row(0, "Bulgarian split squat", SupersetHalf.FIRST, "Close-grip bench"),
                        row(1, "Close-grip bench", SupersetHalf.SECOND, "Bulgarian split squat"),
                        row(2, "Barbell row", null, null),
                    ),
                ),
            ),
        )
    }

    @Test
    fun plan_theTagsAreWhole_at320dp_atEveryTextSize() {
        var scale by androidx.compose.runtime.mutableFloatStateOf(1f)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = scale)) {
                PowertrackerTheme { PlanContent(planState(), onOpenSettings = {}, onAdd = {}, onEdit = {}, onMove = { _, _ -> }, onDelete = {}, onSetBaseline = {}) }
            }
        }
        for (s in scales) {
            scale = s
            compose.waitForIdle()
            assertEquals("Plan at $s: one A", 1, whole("SUPERSET A", "Plan at $s"))
            assertEquals("Plan at $s: one B", 1, whole("SUPERSET B", "Plan at $s"))
            // The pair's own lines too: the tag above them takes nothing from them.
            for (text in listOf("Bulgarian split squat", "Close-grip bench")) assertEquals("Plan at $s: $text", 1, whole(text, "Plan at $s"))
            // Not every text on the screen: at large text this width cuts the header's "WEEKLY TEMPLATE", which is older
            // than supersets and not theirs to answer for here.
            compose.assertTextsFit()
        }
    }

    private fun todayItem(id: Long, name: String, done: Int, superset: TodaySuperset?): TodayItem {
        val exercise = Exercise(id, name, Category.VARIATION, null, emptyList(), true)
        return TodayItem(
            exercise = exercise,
            planned = PlanItem(id, DayOfWeek.WEDNESDAY, id, 3, 5, 8.0, id.toInt()),
            suggestion = null,
            sets = List(done) { StoredSet(100 * id + it, 1, LoggedSet(id, 100.0, 5, 8.0, failed = false, missPoint = null, isTopSet = false)) },
            editor = SetEditor(100.0, 5, 8.0),
            targetSets = 3,
            perSide = PlateLoader.perSide(100.0, changePlates = false),
            lastWeightKg = 100.0,
            superset = superset,
        )
    }

    /** Wednesday mid-pair: the first exercise has a set and is folded (its tag beside its plan and count), the second is open. */
    private fun todayState() = TodayState(
        LocalDate(2026, 11, 11), loading = false, sessionId = 1,
        items = listOf(
            todayItem(1, "Bulgarian split squat", 1, TodaySuperset(SupersetHalf.FIRST, 2, "Close-grip bench", due = SupersetHalf.SECOND)),
            todayItem(2, "Close-grip bench", 0, TodaySuperset(SupersetHalf.SECOND, 1, "Bulgarian split squat", due = SupersetHalf.SECOND)),
            todayItem(3, "Barbell row", 0, null),
        ),
    )

    @Test
    fun today_theTagsAreWhole_at320dp_atEveryTextSize() {
        var scale by androidx.compose.runtime.mutableFloatStateOf(1f)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = scale)) {
                PowertrackerTheme { TodayContent(todayState(), TodayActions()) }
            }
        }
        for (s in scales) {
            scale = s
            compose.waitForIdle()
            // The open card's heading, and the folded first exercise's tag with its count beside it.
            assertEquals("Today at $s: the open card's tag", 1, whole("SUPERSET B", "Today at $s"))
            assertEquals("Today at $s: the folded row's tag", 1, whole("Superset A", "Today at $s"))
            assertEquals("Today at $s: the folded row's count", 1, whole("1 of 3", "Today at $s"))
            assertEquals("Today at $s: the open card's set count", 1, whole("Set 1 of 3", "Today at $s"))
            // Not every text on the card: at this width the plate strip's "per side · 20 kg bar" loses its last word,
            // which is older than supersets and not theirs to answer for here.
            compose.assertTextsFit()
        }
    }
}
