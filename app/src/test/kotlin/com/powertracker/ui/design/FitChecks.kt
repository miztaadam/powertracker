package com.powertracker.ui.design

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Every text on screen is laid out whole (not cut or ellipsised) and inside the screen's width.
 * Needs native graphics, so text is measured at its real size. Texts inside a row that speaks as
 * one sentence (`clearAndSetSemantics`) are not in the tree: check those by their row.
 */
fun ComposeTestRule.assertTextsFit() {
    val screen = onRoot().fetchSemanticsNode().boundsInRoot
    val nodes = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true).fetchSemanticsNodes()
    assertTrue("there is text to check", nodes.isNotEmpty())
    for (node in nodes) {
        val text = node.config[SemanticsProperties.Text].joinToString()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        for (layout in layouts) {
            // A line's own width, not where it sits: centred text is laid out in a wider paragraph.
            val widest = (0 until layout.lineCount).maxOfOrNull { layout.getLineRight(it) - layout.getLineLeft(it) } ?: 0f
            assertTrue("\"$text\" is wider than its box ($widest in ${layout.size.width})", widest <= layout.size.width + 1f)
            assertFalse("\"$text\" is ellipsised", (0 until layout.lineCount).any { layout.isLineEllipsized(it) })
        }
        val left = node.positionInRoot.x
        val right = left + node.size.width
        assertTrue("\"$text\" runs off the screen ($left to $right of ${screen.width})", left >= 0f && right <= screen.width + 0.5f)
    }
}
