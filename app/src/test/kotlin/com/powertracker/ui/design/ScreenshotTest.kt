package com.powertracker.ui.design

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.powertracker.ui.Tab
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the mockups in docs/design/screens (dark and light) from the real components.
 * Today, Plan and Settings are phone size (412 × 915 dp); Coach and Progress are full-page
 * captures of a scrolling screen, so they are taller.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h915dp-xhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    // Unit tests run with the module directory as the working directory.
    private val outDir = File("../docs/design/screens").apply { mkdirs() }

    private fun render(name: String, tab: Tab?, content: @Composable () -> Unit) {
        var dark by mutableStateOf(true)
        compose.setContent { ShowcaseFrame(dark, tab, content) }
        for (mode in listOf(true, false)) {
            dark = mode
            compose.waitForIdle()
            val file = File(outDir, "$name-${if (mode) "dark" else "light"}.png")
            // captureToImage() never gets its frame callback under Robolectric, so draw the view directly.
            val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            assertTrue("$file should be a real image", file.length() > 10_000)
        }
    }

    @Test
    fun today() = render("today", Tab.TODAY) { TodayShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1256dp-xhdpi")
    fun coach() = render("coach", Tab.COACH) { CoachShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1104dp-xhdpi")
    fun progress() = render("progress", Tab.PROGRESS) { ProgressShowcase() }

    @Test
    fun plan() = render("plan", Tab.PLAN) { PlanShowcase() }

    @Test
    fun settings() = render("settings", null) { SettingsShowcase() }
}
