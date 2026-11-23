package com.powertracker.ui.today

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ActivityScenario
import com.powertracker.domain.library.Library
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import kotlinx.datetime.DayOfWeek
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Every catalogue name on a Today row stays whole, with the row menu (⋮) beside it: not ellipsised, and wrapped only
 * between words (or after a hyphen), never inside one. Checked at 320 dp with 100% text, 360 dp with 130% and 320 dp
 * with 130%, on the folded rows, the start card's rows, the open exercise card's header and the "Skipped today" line,
 * on both sides.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h2400dp-xhdpi")
class TodayNamesWholeTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<ComponentActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
        RuntimeEnvironment.setFontScale(1f)
    }

    private val names = Library.all().map { it.name }

    private fun item(i: Int, name: String, mode: TrainingMode): TodayItem {
        val bodybuilding = mode == TrainingMode.BODYBUILDING
        val lib = Library.find(name)!!
        // The widest plan text each side writes: a range on the bodybuilding side, a half RPE on both.
        val plan = PlanItem(i + 1L, DayOfWeek.WEDNESDAY, i + 1L, if (bodybuilding) 3 else 10, if (bodybuilding) 8 else 10, 7.5, i, mode, if (bodybuilding) 12 else null)
        return TodayItem(Exercise(i + 1L, lib.name, lib.category, lib.parentLift, lib.muscles, isBuiltIn = true), plan, null, emptyList(), SetEditor(20.0, 8, 7.5), plan.sets, null, 20.0)
    }

    private fun show(width: Int, fontScale: Float, content: @Composable () -> Unit) {
        scenario?.close()
        RuntimeEnvironment.setQualifiers("w${width}dp-h2400dp-xhdpi")
        RuntimeEnvironment.setFontScale(fontScale)
        scenario = ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
            activity.setContent {
                PowertrackerTheme {
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter)) {
                        content()
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    /** The catalogue names found inside [tag]-prefixed rows that are ellipsised or broken inside a word, and how many were seen. */
    private fun broken(tagOf: (String) -> String): Pair<List<String>, Int> {
        val bad = mutableListOf<String>()
        var seen = 0
        for (name in names) {
            val nodes = compose.onAllNodes(
                SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(AnnotatedString(name))) and hasAnyAncestor(hasTestTag(tagOf(name))),
                useUnmergedTree = true,
            ).fetchSemanticsNodes()
            if (nodes.isEmpty()) continue
            seen++
            val layouts = mutableListOf<TextLayoutResult>()
            nodes.first().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
            val layout = layouts.single()
            val lines = (0 until layout.lineCount).map { name.substring(layout.getLineStart(it), layout.getLineEnd(it)) }
            val ellipsised = (0 until layout.lineCount).any { layout.isLineEllipsized(it) }
            val inside = (0 until layout.lineCount - 1).any { line ->
                val end = layout.getLineEnd(line, visibleEnd = true)
                !(name[end] == ' ' || name[end - 1] == ' ' || name[end - 1] == '-')
            }
            if (ellipsised || inside || layout.getLineEnd(layout.lineCount - 1) != name.length) bad += "${tagOf(name)}: $lines"
        }
        return bad to seen
    }

    private fun check(width: Int, fontScale: Float) {
        for (mode in TrainingMode.entries) {
            val items = names.mapIndexed { i, n -> item(i, n, mode) }
            // The folded rows, each with its ⋮.
            show(width, fontScale) {
                for (it in items) ExerciseRow(it, onClick = {}, modifier = Modifier.testTag("row-${it.exercise.name}").padding(vertical = Space.xs), onSkipToday = {})
            }
            val (rows, seenRows) = broken { "row-$it" }
            assertEquals(names.size, seenRows)
            assertTrue("$mode at $width dp × $fontScale, folded rows: $rows", rows.isEmpty())

            // The folded rows of exercises done, each with its Done pill (no ⋮).
            val done = items.map { it.copy(sets = List(it.targetSets!!) { n -> com.powertracker.data.StoredSet(n + 1L, 1, com.powertracker.domain.model.LoggedSet(it.exercise.id, 20.0, 8, 7.5, false, null, false)) }) }
            show(width, fontScale) {
                for (it in done) ExerciseRow(it, onClick = {}, modifier = Modifier.testTag("done-${it.exercise.name}").padding(vertical = Space.xs))
            }
            val (doneRows, _) = broken { "done-$it" }
            assertTrue("$mode at $width dp × $fontScale, rows done: $doneRows", doneRows.isEmpty())

            // The start card's rows.
            show(width, fontScale) { StartSessionCard(items, "Wednesday", { _, _ -> }, Modifier.testTag("start"), onSkipToday = {}) }
            val (start, seenStart) = broken { "start" }
            assertEquals(names.size, seenStart)
            assertTrue("$mode at $width dp × $fontScale, start card: $start", start.isEmpty())

            // The "Skipped today" line, each name beside its Bring back.
            show(width, fontScale) { SkippedTodayLine(items.map { SkippedToday(it.exercise.id, it.exercise.name, it.exercise.id) }, onBringBack = {}) }
            val (skipped, seenSkipped) = broken { "skipped-today" }
            assertEquals(names.size, seenSkipped)
            assertTrue("$mode at $width dp × $fontScale, skipped line: $skipped", skipped.isEmpty())

            // The open card's header, for the longest names.
            val longest = items.sortedByDescending { it.exercise.name.length }.take(12)
            show(width, fontScale) {
                for (it in longest) {
                    ExerciseCard(
                        it, 1, 4, 2.5, false, NextStep("Finish session") {}, TodayActions(),
                        modifier = Modifier.testTag("card-${it.exercise.name}"),
                        bodybuilding = mode == TrainingMode.BODYBUILDING, onSkipToday = {},
                    )
                }
            }
            val longNames = longest.map { it.exercise.name }.toSet()
            val (cards, _) = broken { if (it in longNames) "card-$it" else "none" }
            assertTrue("$mode at $width dp × $fontScale, card headers: $cards", cards.isEmpty())
        }
    }

    @Test
    fun todayNamesWhole_at320dp() = check(320, 1f)

    @Test
    fun todayNamesWhole_at360dpWith130PercentText() = check(360, 1.3f)

    /** The width where the web stacks its guide button and ⋮ (final review, 1.13). */
    @Test
    fun todayNamesWhole_at320dpWith130PercentText() = check(320, 1.3f)

    /** Whether the ⋮ sits under the guide button, on a folded row and on the open card, at [width] dp × [fontScale]. */
    private fun stacked(width: Int, fontScale: Float): Pair<Boolean, Boolean> {
        val it = item(0, "Curl", TrainingMode.POWERLIFTING)
        show(width, fontScale) {
            ExerciseRow(it, onClick = {}, onSkipToday = {})
            ExerciseCard(it, 1, 1, 2.5, false, NextStep("Finish session") {}, TodayActions(), onSkipToday = {})
        }
        fun under(i: Int): Boolean {
            val guide = compose.onAllNodes(hasContentDescription("Form guide for Curl"), useUnmergedTree = true).fetchSemanticsNodes()[i].boundsInRoot
            val menu = compose.onAllNodes(hasContentDescription("Options for Curl"), useUnmergedTree = true).fetchSemanticsNodes()[i].boundsInRoot
            return menu.top >= guide.bottom
        }
        return under(0) to under(1)
    }

    /** The guide button and ⋮ stack only where the card is narrower than ROW_STACK_WIDTH over the font scale. */
    @Test
    fun guideAndMenu_stackOnlyOnANarrowCard() {
        assertEquals(true to true, stacked(320, 1.3f))
        assertEquals(false to false, stacked(320, 1f))
        assertEquals(false to false, stacked(411, 1.3f))
    }
}
