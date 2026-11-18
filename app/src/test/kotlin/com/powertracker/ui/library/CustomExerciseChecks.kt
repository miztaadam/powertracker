package com.powertracker.ui.library

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement

/** What [fillCustomExercise] types into "Create your own": a variation of the bench, with two muscles. */
const val OWN_NAME = "Tyre flip"
const val OWN_MUSCLES = "Lats, Grip"

private val inForm: SemanticsMatcher = hasAnyAncestor(hasTestTag("custom-exercise-form"))

private fun ComposeContentTestRule.field(label: String) = onNode(hasSetTextAction() and hasText(label) and inForm)

private fun ComposeContentTestRule.chip(text: String) = onNode(isToggleable() and hasText(text) and inForm)

fun ComposeContentTestRule.createFormOpen(): Boolean = onAllNodes(hasTestTag("custom-exercise-form")).fetchSemanticsNodes().isNotEmpty()

/** In the open "Create your own" form: everything it holds, set away from its defaults. Does not tap Create. */
fun ComposeContentTestRule.fillCustomExercise() {
    field("Name").performTextReplacement(OWN_NAME)
    chip("Variation").performClick()
    chip("Bench").performClick()
    field("Muscles (comma-separated)").performTextReplacement(OWN_MUSCLES)
    waitForIdle()
}

/** The create form is open and still holds what [fillCustomExercise] typed: name, category, parent lift and muscles. */
fun ComposeContentTestRule.assertCustomExerciseKept() {
    check(createFormOpen()) { "the create form is closed" }
    check(typed("Name") == OWN_NAME) { "name was ${typed("Name")}" }
    chip("Variation").assertIsOn()
    chip("Accessory").assertIsOff()
    chip("Bench").assertIsOn()
    check(typed("Muscles (comma-separated)") == OWN_MUSCLES) { "muscles were ${typed("Muscles (comma-separated)")}" }
}

/** The create form is open as new: [name] (what was searched for), an accessory, no parent lift and no muscles. */
fun ComposeContentTestRule.assertCustomExerciseEmpty(name: String) {
    check(createFormOpen()) { "the create form is closed" }
    check(typed("Name") == name) { "name was ${typed("Name")}" }
    chip("Accessory").assertIsOn()
    chip("Variation").assertIsOff()
    onAllNodes(isToggleable() and hasText("Bench") and inForm).fetchSemanticsNodes().let { check(it.isEmpty()) { "a parent lift is offered" } }
    check(typed("Muscles (comma-separated)").isEmpty()) { "muscles were carried over" }
}

private fun ComposeContentTestRule.typed(label: String): String =
    field(label).fetchSemanticsNode().config[SemanticsProperties.EditableText].text

/**
 * The failed save's message is shown over the create form, in a window of its own above the form's scrim (not behind
 * it on the screen underneath), while the form is still open.
 */
fun ComposeContentTestRule.assertSaveFailedOverTheForm() {
    check(createFormOpen()) { "the create form is closed" }
    onNode(hasText(SAVE_FAILED) and hasAnyAncestor(isPopup())).assertExists()
}
