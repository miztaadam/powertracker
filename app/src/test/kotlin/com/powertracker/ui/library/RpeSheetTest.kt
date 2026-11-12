package com.powertracker.ui.library

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.RpeGuide
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
class RpeSheetTest {
    // The activity is started by the test, not the rule, so the font scale can be set before it exists.
    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<ComponentActivity>? = null
    private var dismissed = 0

    @After
    fun tearDown() {
        scenario?.close()
    }

    private fun show(fontScale: Float = 1f) {
        RuntimeEnvironment.setFontScale(fontScale)
        scenario = ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
            assertEquals(fontScale, activity.resources.configuration.fontScale, 0.001f)
            activity.setContent { PowertrackerTheme { RpeSheet(onDismiss = { dismissed++ }) } }
        }
        compose.waitUntil(5_000) { compose.rpeSheetIsOpen() }
    }

    private val inSheet = hasAnyAncestor(hasTestTag("rpe-sheet"))

    @Test
    fun showsTheWholeExplainerInOrder_andNoSafetyLine() {
        show()

        val order = listOf(RpeGuide.title) + RpeGuide.lines
        val tops = order.map { compose.onNode(hasText(it) and inSheet).getUnclippedBoundsInRoot().top }
        assertEquals("the sheet's lines, top to bottom", tops.sortedBy { it.value }, tops)
        assertEquals("every line is on its own row", tops.size, tops.distinct().size)
        assertTrue(compose.onNode(hasText(RpeGuide.title) and inSheet).fetchSemanticsNode().config.contains(SemanticsProperties.Heading))

        // It is not an exercise: nothing but the explainer, and no safety line under it.
        val texts = compose.onAllNodes(inSheet, useUnmergedTree = true).fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.Text)?.joinToString() }
        assertEquals(order, texts)
        compose.onNode(hasText(Library.SAFETY_LINE)).assertDoesNotExist()
        compose.onNode(hasTestTag("guide-safety")).assertDoesNotExist()

        val close = compose.onNodeWithContentDescription("Close")
        close.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        close.performClick()
        compose.waitUntil(5_000) { dismissed == 1 }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun rpeSheet_largeFont_scrollsToTheLastLine() {
        show(fontScale = 1.3f)

        val screen = compose.onNodeWithTag("rpe-sheet").getUnclippedBoundsInRoot()
        // No line is wider than the sheet or cut off, at any scroll position, down to the last one.
        for (line in listOf(RpeGuide.title) + RpeGuide.lines) {
            val node = compose.onNode(hasText(line) and inSheet).performScrollTo().assertIsDisplayed()
            val b = node.getUnclippedBoundsInRoot()
            assertTrue("\"$line\" fits across the sheet", b.left >= screen.left && b.right <= screen.right)
            val n = node.fetchSemanticsNode()
            assertEquals("\"$line\" is not clipped", n.size.height.toFloat(), n.boundsInRoot.height, 0.5f)
        }
        assertTrue("the last line ends on screen", compose.onNode(hasText(RpeGuide.lines.last()) and inSheet).getUnclippedBoundsInRoot().bottom <= 640.dp)
    }
}
