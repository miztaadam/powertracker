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

/**
 * [assertTextsFit] for a screen with more than one window (a dialog, a menu or a sheet over it): in every window, each
 * text is laid out whole, inside its window and not cut by a parent; each window is no wider than the screen,
 * [screenWidthPx] wide. Returns the texts checked.
 */
fun ComposeTestRule.assertTextsFitInEveryWindow(screenWidthPx: Float): List<String> {
    val checked = mutableListOf<String>()
    val roots = onAllNodes(androidx.compose.ui.test.isRoot(), useUnmergedTree = true).fetchSemanticsNodes()
    for (root in roots) {
        val window = root.boundsInRoot
        assertTrue("a window is wider than the screen (${window.width} of $screenWidthPx)", window.width <= screenWidthPx + 0.5f)
        val texts = mutableListOf<androidx.compose.ui.semantics.SemanticsNode>()
        fun walk(n: androidx.compose.ui.semantics.SemanticsNode) {
            if (n.config.getOrNull(SemanticsProperties.Text) != null) texts += n
            n.children.forEach(::walk)
        }
        walk(root)
        for (node in texts) {
            val text = node.config[SemanticsProperties.Text].joinToString()
            checked += text
            val layouts = mutableListOf<TextLayoutResult>()
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
            for (layout in layouts) {
                val widest = (0 until layout.lineCount).maxOfOrNull { layout.getLineRight(it) - layout.getLineLeft(it) } ?: 0f
                assertTrue("\"$text\" is wider than its box ($widest in ${layout.size.width})", widest <= layout.size.width + 1f)
                assertFalse("\"$text\" is ellipsised", (0 until layout.lineCount).any { layout.isLineEllipsized(it) })
                assertFalse("\"$text\" has more lines than it may show (shows \"${layout.layoutInput.text.text.substring(0, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))}\" in ${layout.size.width} px)", layout.multiParagraph.didExceedMaxLines)
            }
            val left = node.positionInRoot.x
            val right = left + node.size.width
            assertTrue("\"$text\" runs out of its window ($left to $right of ${window.width})", left >= -0.5f && right <= window.width + 0.5f)
            // Bounds in the window are cut by every parent: smaller than the text means a parent clips it.
            val shown = node.boundsInRoot
            assertTrue("\"$text\" is cut by a parent (${shown.width} × ${shown.height} of ${node.size})", shown.width >= node.size.width - 1f && shown.height >= node.size.height - 1f)
        }
    }
    return checked
}
