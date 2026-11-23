package com.powertracker.ui.library

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.RpeGuide

fun ComposeContentTestRule.guideIsOpen(): Boolean = onAllNodes(hasTestTag("form-guide")).fetchSemanticsNodes().isNotEmpty()

/**
 * Taps the info button for [name] (the [index]-th, when the exercise is on screen more than once),
 * checks its size and that the sheet opens on that exercise's guide, then closes the sheet.
 */
fun ComposeContentTestRule.checkInfoButtonOpensGuide(name: String, index: Int = 0) {
    val button = onAllNodesWithContentDescription("Form guide for $name")[index]
    button.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
    button.performClick()
    waitUntil(5_000) { guideIsOpen() }

    val inSheet = hasAnyAncestor(hasTestTag("form-guide"))
    onNode(hasText(name) and inSheet).assertExists()
    val entry = Library.find(name)
    if (entry != null) {
        onNode(hasText(entry.guide.purpose) and inSheet).assertExists()
        onNode(hasText(entry.guide.steps.first()) and inSheet).assertExists()
    } else {
        onNodeWithText(NO_GUIDE).assertExists()
    }
    onNodeWithText(Library.SAFETY_LINE).assertExists()

    onNodeWithContentDescription("Close guide").performClick()
    waitUntil(5_000) { !guideIsOpen() }
}

fun ComposeTestRule.rpeSheetIsOpen(): Boolean = onAllNodes(hasTestTag("rpe-sheet")).fetchSemanticsNodes().isNotEmpty()

/**
 * Taps the "What RPE means" button, checks its size and that the sheet opens with the whole
 * explainer and no exercise safety line, then closes the sheet.
 */
fun ComposeContentTestRule.checkRpeInfoOpensSheet() {
    val button = onNodeWithContentDescription("What RPE means")
    button.assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
    button.performClick()
    waitUntil(5_000) { rpeSheetIsOpen() }

    val inSheet = hasAnyAncestor(hasTestTag("rpe-sheet"))
    onNode(hasText(RpeGuide.title) and inSheet).assertExists()
    for (line in RpeGuide.lines) onNode(hasText(line) and inSheet).assertExists()
    // It explains a scale, not an exercise: no safety line.
    onNodeWithText(Library.SAFETY_LINE).assertDoesNotExist()
    onAllNodes(hasTestTag("guide-safety")).assertCountEquals(0)

    onNodeWithContentDescription("Close").performClick()
    waitUntil(5_000) { !rpeSheetIsOpen() }
}
