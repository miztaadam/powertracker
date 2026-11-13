package com.powertracker.ui.plan

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.AnnotatedString
import com.powertracker.BuildConfig
import com.powertracker.data.AppSettings
import com.powertracker.ui.theme.PowertrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The quiet "Grit 1.1 · AppsbyAdam" line at the very bottom of Settings. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2400dp-xxhdpi")
class SettingsVersionLineTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show() = compose.setContent {
        PowertrackerTheme { SettingsContent(AppSettings(), onUpdate = {}, onExport = {}, onRestore = {}, onBack = {}) }
    }

    @Test
    fun versionLine_format() {
        assertEquals("Grit 1.1 · AppsbyAdam", appVersionLine("1.1"))
    }

    @Test
    fun versionLine_showsTheBuildsVersion() {
        show()
        val node = compose.onNodeWithTag("app-version", useUnmergedTree = true).fetchSemanticsNode()
        val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
        assertEquals("Grit ${BuildConfig.VERSION_NAME} · AppsbyAdam", text)
        // Read aloud once, without the middle dots.
        assertEquals(listOf("Grit version ${BuildConfig.VERSION_NAME}, by AppsbyAdam"), node.config.getOrNull(SemanticsProperties.ContentDescription))
        // Plain text: not a button or a link.
        assertEquals(null, node.config.getOrNull(SemanticsProperties.Role))
        assertEquals(null, node.config.getOrNull(androidx.compose.ui.semantics.SemanticsActions.OnClick))
    }

    @Test
    fun versionLine_isTheLastThingOnSettings() {
        show()
        val line = compose.onNodeWithTag("app-version", useUnmergedTree = true).fetchSemanticsNode()
        val lineTop = line.positionInRoot.y
        val others = mutableListOf<SemanticsNode>()
        fun walk(n: SemanticsNode) {
            if (n.id != line.id && n.config.getOrNull(SemanticsProperties.Text) != null) others += n
            n.children.forEach(::walk)
        }
        walk(compose.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        assertTrue("found the other texts", others.any { it.config[SemanticsProperties.Text].any { t: AnnotatedString -> t.text == "Restore backup" } })
        for (n in others) {
            val bottom = n.positionInRoot.y + n.size.height
            assertTrue("${n.config[SemanticsProperties.Text]} ends below the version line", bottom <= lineTop + 0.5f)
        }
    }
}
