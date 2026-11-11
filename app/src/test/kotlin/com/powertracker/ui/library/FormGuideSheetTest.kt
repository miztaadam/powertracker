package com.powertracker.ui.library

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.powertracker.domain.library.Library
import com.powertracker.ui.theme.PowertrackerTheme
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

// Native graphics, so text is measured at its real size: the layout checks here depend on it.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class FormGuideSheetTest {
    // The activity is started by the test, not the rule, so the font scale can be set before it exists.
    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<ComponentActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
    }

    private fun show(name: String, fontScale: Float = 1f) {
        RuntimeEnvironment.setFontScale(fontScale)
        scenario = ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
            assertEquals(fontScale, activity.resources.configuration.fontScale, 0.001f)
            activity.setContent { PowertrackerTheme { FormGuideSheet(name, onDismiss = {}) } }
        }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("form-guide")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun toDp(px: Float) = (px / RuntimeEnvironment.getApplication().resources.displayMetrics.density).dp

    private val inSheet = hasAnyAncestor(hasTestTag("form-guide"))

    private fun top(text: String) = compose.onNode(hasText(text) and inSheet).getUnclippedBoundsInRoot().top

    @Test
    fun showsAllSections_andSafetyLineLast() {
        val entry = Library.find("Squat")!!
        val guide = entry.guide
        show("Squat")

        // Name; area and equipment; purpose; Setup; How to do it; Cues; Common mistakes; the safety line.
        val order = listOf("Squat", "MAIN LIFTS · BARBELL", guide.purpose, "Setup") + guide.setup +
            listOf("How to do it") + guide.steps + listOf("Cues") + guide.cues + listOf("Common mistakes") + guide.mistakes +
            listOf(Library.SAFETY_LINE)
        val tops = order.map { top(it) }
        assertEquals("the sheet's lines, top to bottom", tops.sortedBy { it.value }, tops)
        assertEquals("every line is on its own row", tops.size, tops.distinct().size)

        // Nothing comes after the safety line.
        val safetyBottom = compose.onNodeWithText(Library.SAFETY_LINE).getUnclippedBoundsInRoot().bottom
        val texts = compose.onAllNodes(inSheet).fetchSemanticsNodes().filter { it.config.getOrNull(SemanticsProperties.Text) != null }
        assertTrue(texts.size > order.size - 1)
        for (node in texts) {
            val bottom = toDp(node.boundsInRoot.bottom)
            assertTrue("${node.config[SemanticsProperties.Text]} is below the safety line", bottom <= safetyBottom)
        }
        // The steps are numbered.
        compose.onNode(hasText("1") and hasText(guide.steps.first())).assertExists()
    }

    @Test
    fun everyCatalogueName_andAlias_showsItsGuide() {
        show("Back squat")
        compose.onNode(hasText("Back squat") and inSheet).assertExists()
        compose.onNodeWithText(Library.find("Squat")!!.guide.purpose).assertExists()
    }

    @Test
    fun customExercise_showsNoGuideText() {
        show("Sled push")

        compose.onNode(hasText("Sled push") and inSheet).assertExists()
        compose.onNodeWithText("No form guide for your own exercises.").assertExists()
        compose.onNodeWithText("Setup").assertDoesNotExist()
        compose.onNodeWithText("Common mistakes").assertDoesNotExist()
        // The sheet still ends with the safety line.
        assertTrue(top("No form guide for your own exercises.") < top(Library.SAFETY_LINE))
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun guideSheet_largeFont_scrollsToSafetyLine() {
        // The longest guide in the catalogue: the one most likely not to fit.
        val entry = Library.all().maxBy { e -> with(e.guide) { (setup + steps + cues + mistakes).sumOf { it.length } + purpose.length } }
        show(entry.name, fontScale = 1.3f)

        val screen = compose.onNodeWithTag("form-guide").getUnclippedBoundsInRoot()
        val safety = compose.onNodeWithText(Library.SAFETY_LINE)
        assertTrue("the guide is longer than the screen, so it has to scroll", safety.getUnclippedBoundsInRoot().bottom > 640.dp)

        safety.performScrollTo().assertIsDisplayed()
        val shown = safety.fetchSemanticsNode()
        // Whole and inside the sheet: the full height of the line is on screen, and it is not cut at either side.
        assertEquals("the safety line is not clipped", shown.size.height.toFloat(), shown.boundsInRoot.height, 0.5f)
        assertEquals(shown.size.width.toFloat(), shown.boundsInRoot.width, 0.5f)
        val bounds = safety.getUnclippedBoundsInRoot()
        assertTrue(bounds.left >= screen.left && bounds.right <= screen.right)
        assertTrue("the safety line ends on screen", bounds.bottom <= 640.dp)

        // No line of the guide is wider than the sheet, at any scroll position.
        with(entry.guide) { listOf(purpose) + setup + steps + cues + mistakes }.forEach { line ->
            val node = compose.onNode(hasText(line) and inSheet).performScrollTo().assertIsDisplayed()
            val b = node.getUnclippedBoundsInRoot()
            assertTrue("\"$line\" fits across the sheet", b.left >= screen.left && b.right <= screen.right)
            val n = node.fetchSemanticsNode()
            assertEquals("\"$line\" is not clipped", n.size.height.toFloat(), n.boundsInRoot.height, 0.5f)
        }
    }
}
