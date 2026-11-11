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
import com.powertracker.ui.lock.LockScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the mockups (dark and light) from the real components.
 *
 * A normal test run is a smoke check: every screen must render, and the PNGs go to
 * `app/build/screenshots/`. The committed copies in `docs/design/screens/` are only rewritten with
 * `./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*' -Pscreenshots.update=true`.
 *
 * The Today and Coach mockups render the real `TodayContent` / `CoachContent` with sample state
 * (Today: mid-session, before the session, an exercise finished, after Finish; Coach: the feed, the
 * empty feed, and the rarer re-entry and miss-point cards). Progress and lift history render the
 * real `ProgressContent` / `LiftHistoryContent` from a sample training history. The exercise picker
 * is the real `ExercisePickerContent`; the form guide is the real `FormGuideContent` in a stand-in
 * for the bottom sheet (the sheet itself is its own window, which this capture cannot see). Today, Plan,
 * the exercise picker,
 * lift history, the empty Coach feed and the two lock screens are phone size (412 × 915 dp); the
 * other Coach captures, Progress, Settings and the form guide are full-page captures of a scrolling screen, so
 * their height differs. The launcher icon is rendered by [LauncherIconTest].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h915dp-xhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    // Unit tests run with the module directory (app/) as the working directory.
    private val outDir = File(if (System.getProperty("screenshots.update") == "true") "../docs/design/screens" else "build/screenshots").apply { mkdirs() }

    private fun render(name: String, tab: Tab?, content: @Composable () -> Unit) {
        var dark by mutableStateOf(true)
        compose.setContent { ShowcaseFrame(dark, tab, content) }
        for (mode in listOf(true, false)) {
            dark = mode
            compose.waitForIdle()
            // Not captureToImage(): under Robolectric it waits for a frame-drawn callback that never
            // comes and times out. Drawing the Compose root view into a bitmap gives the same pixels
            // with native graphics.
            val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            assertEquals("$name should be 412 dp wide at 2x", 824, bitmap.width)
            // Something was actually drawn: the frame is not one flat colour.
            val corner = bitmap.getPixel(0, 0)
            val pixels = IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
            assertTrue("$name rendered blank", pixels.count { it != corner } > pixels.size / 20)
            val file = File(outDir, "$name-${if (mode) "dark" else "light"}.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test
    fun today() = render("today", Tab.TODAY) { TodayShowcase() }

    @Test
    fun todayStart() = render("today-start", Tab.TODAY) { TodayStartShowcase() }

    @Test
    fun todayNext() = render("today-next", Tab.TODAY) { TodayNextShowcase() }

    @Test
    fun todaySummary() = render("today-summary", Tab.TODAY) { TodaySummaryShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1440dp-xhdpi")
    fun coach() = render("coach", Tab.COACH) { CoachShowcase() }

    @Test
    fun coachEmpty() = render("coach-empty", Tab.COACH) { CoachEmptyShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1000dp-xhdpi")
    fun coachMore() = render("coach-more", Tab.COACH) { CoachMoreShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1830dp-xhdpi")
    fun progress() = render("progress", Tab.PROGRESS) { ProgressShowcase() }

    @Test
    fun liftHistory() = render("lift-history", null) { LiftHistoryShowcase() }

    @Test
    fun plan() = render("plan", Tab.PLAN) { PlanShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1030dp-xhdpi")
    fun settings() = render("settings", null) { SettingsShowcase() }

    @Test
    fun exercisePicker() = render("exercise-picker", null) { ExercisePickerShowcase() }

    @Test
    @Config(qualifiers = "w412dp-h1370dp-xhdpi")
    fun formGuide() = render("form-guide", null) { FormGuideShowcase() }

    @Test
    fun lock() = render("lock", null) { LockScreen(noDeviceLock = false, onUnlock = {}, onContinue = {}) }

    @Test
    fun lockNoScreenLock() = render("lock-no-screen-lock", null) { LockScreen(noDeviceLock = true, onUnlock = {}, onContinue = {}) }
}
