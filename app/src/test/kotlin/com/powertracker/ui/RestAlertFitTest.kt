package com.powertracker.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ActivityScenario
import com.powertracker.data.AppSettings
import com.powertracker.ui.plan.SettingsContent
import com.powertracker.ui.theme.PowertrackerTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

/**
 * Settings' rest alerts on a narrow phone and at large text: each label, "Sound + vibrate" the longest, is laid out
 * whole on one line inside its segment, at no smaller than 12 sp. Checked in 1.12.2: it already was, so nothing changed.
 */
// Native graphics, so text is measured at its real size.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RestAlertFitTest {
    // The activity is started by the test, so the screen and the font scale can be set before it exists.
    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<ComponentActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
    }

    private val labels = listOf("Vibrate only", "Sound + vibrate", "Off")

    @Test
    fun restAlert_eachLabelWholeOnOneLine_at320dp_and360dpAt130Percent() {
        // 320 dp at 100% text and 360 dp at 130%: 320 and 277 dp of text room. (At 320 dp and 130%, 246 dp of room, the
        // labels are cut short: that is below the plan's 320 dp of room and is left to the owner.)
        val problems = mutableListOf<String>()
        for ((width, scale) in listOf(320 to 1f, 360 to 1.3f)) {
            scenario?.close()
            RuntimeEnvironment.setQualifiers("w${width}dp-h2400dp-xhdpi")
            RuntimeEnvironment.setFontScale(scale)
            scenario = ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
                activity.setContent { PowertrackerTheme { SettingsContent(AppSettings(), onUpdate = {}, onExport = {}, onRestore = {}, onBack = {}) } }
            }
            compose.waitForIdle()
            val density = compose.onRoot().fetchSemanticsNode().layoutInfo.density
            assertEquals(width * density.density, compose.onRoot().fetchSemanticsNode().boundsInRoot.width, 0.5f)

            for (label in labels) {
                val node = compose.onAllNodes(
                    SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(AnnotatedString(label))) and hasAnyAncestor(isSelectable()),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes().single()
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
                val layout = layouts.single()
                val where = "\"$label\" at $width dp, text x$scale"
                // One line (the segment allows no more), holding the whole label, inside its box, as FitChecks measures it.
                val shown = label.substring(0, layout.getLineEnd(0, visibleEnd = true))
                val line = layout.getLineRight(0) - layout.getLineLeft(0)
                val sp = with(density) { layout.layoutInput.style.fontSize.toPx() } / density.density / scale
                if (layout.lineCount != 1 || shown != label || layout.isLineEllipsized(0) || line > layout.size.width + 1f || sp < 12f) {
                    problems += "$where: shows \"$shown\" ($line px in ${layout.size.width} px, $sp sp)"
                }
            }
        }
        assertTrue("labels not whole on one line: $problems", problems.isEmpty())
    }
}
