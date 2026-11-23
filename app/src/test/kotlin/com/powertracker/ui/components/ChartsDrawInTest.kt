package com.powertracker.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.powertracker.ui.theme.LocalMotion
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Tone
import com.powertracker.ui.theme.color
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.powertracker.ui.design.SAMPLE_HISTORY_WITH_PRESS
import com.powertracker.ui.design.SHOWCASE_DATE
import com.powertracker.ui.progress.ChartRange
import com.powertracker.ui.progress.ProgressContent
import com.powertracker.ui.progress.ProgressViewModel
import com.powertracker.ui.progress.LineChart as BodyweightChart

/**
 * Charts fill in (stage 4, Task 1): with motion on a line draws from the left and bars grow from the baseline, all
 * done by 700 ms; a range change draws the line again; one point or none still renders; with motion off everything
 * is complete in the first frame. Checked on the drawn pixels: each chart is drawn in pure red on black.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h800dp-mdpi")
class ChartsDrawInTest {
    @get:Rule
    val compose = createComposeRule()

    private val red = Color(0xFFFF0000)

    /** The clock is held: nothing moves until the test moves it. */
    private fun show(motion: Boolean = true, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PowertrackerTheme {
                CompositionLocalProvider(LocalMotion provides motion) {
                    Box(Modifier.size(400.dp, 800.dp).background(Color.Black)) { content() }
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun after(ms: Long) {
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeBy(ms)
        compose.waitForIdle()
    }

    private fun snapshot(): Bitmap {
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        return Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
    }

    private fun isRed(pixel: Int) = android.graphics.Color.red(pixel) > 180 && android.graphics.Color.green(pixel) < 80 && android.graphics.Color.blue(pixel) < 80

    private fun isColor(pixel: Int, color: Color): Boolean {
        val c = color.toArgb()
        return listOf(16, 8, 0).all { shift -> kotlin.math.abs((pixel shr shift and 0xFF) - (c shr shift and 0xFF)) < 12 }
    }

    /** Red pixels in columns [x0, x1) and rows [y0, y1), at 1 px per dp. */
    private fun Bitmap.red(x0: Int, x1: Int, y0: Int = 0, y1: Int = height): Int {
        var n = 0
        for (x in x0 until x1) for (y in y0 until y1) if (isRed(getPixel(x, y))) n++
        return n
    }

    /** The rightmost column with a red pixel, or -1. */
    private fun Bitmap.redRight(y0: Int = 0, y1: Int = height): Int = (width - 1 downTo 0).firstOrNull { x -> (y0 until y1).any { isRed(getPixel(x, it)) } } ?: -1

    // A rising line across a 200 × 60 box: it reaches the right edge only once it is drawn in full.
    @Composable
    private fun RisingSparkline(key: Any? = Unit) {
        val drawn = rememberDrawIn(key, LINE_DRAW_MS)
        Sparkline(listOf(0f, 1f, 2f, 3f, 4f), red, Modifier.size(200.dp, 60.dp), fill = false, drawn = drawn::value)
    }

    @Composable
    private fun FourBars() {
        val grown = rememberBarsGrowIn(Unit)
        BarStrip(listOf(4f, 4f, 4f, 4f), red, Modifier.size(200.dp, 60.dp), grown = grown::value)
    }

    @Test
    fun line_isPartlyDrawn_midway_andComplete_by700ms() {
        show { RisingSparkline() }
        assertEquals("nothing drawn at the first frame", 0, snapshot().red(0, 200, 0, 60))
        after(250)
        val mid = snapshot()
        val head = mid.redRight(0, 60)
        assertTrue("part drawn at 250 ms (head at $head)", head in 20..170)
        after(450)
        assertTrue("complete by 700 ms, end dot at the right", snapshot().redRight(0, 60) >= 194)
    }

    @Test
    fun lineChart_withAxes_drawsIn_andKeepsItsDescription_fromFrame0() {
        show {
            val drawn = rememberDrawIn(Unit, LINE_DRAW_MS)
            Box(Modifier.width(300.dp)) {
                LineChart(listOf(ChartSeries("Squat", red, listOf(100f, 110f, 120f, 130f, 140f))), List(5) { "" }, drawn = drawn::value)
            }
        }
        compose.onNodeWithContentDescription("Line chart. Latest: Squat 140 kg").assertExists()
        after(250)
        val head = snapshot().redRight(0, 400)
        assertTrue("part drawn at 250 ms (head at $head)", head in 40..250)
        after(450)
        assertTrue("complete by 700 ms", snapshot().redRight(0, 400) >= 288)
    }

    @Test
    fun bars_growFromTheBaseline_oneAfterAnother_andAreFull_by700ms() {
        show { FourBars() }
        assertEquals("no bar at the first frame", 0, snapshot().red(0, 200, 0, 60))
        after(150)
        val early = snapshot()
        // Bars are 47 px wide with 4 px gaps: sample the middle of the first and the last.
        val first = (0 until 60).count { isRed(early.getPixel(23, it)) }
        val last = (0 until 60).count { isRed(early.getPixel(176, it)) }
        assertTrue("the first bar part grown at 150 ms ($first of 60)", first in 3..57)
        assertTrue("the last bar behind the first ($last vs $first)", last < first)
        after(550)
        val done = snapshot()
        for (x in listOf(23, 74, 125, 176)) assertEquals("bar at $x full by 700 ms", 60, (0 until 60).count { isRed(done.getPixel(x, it)) })
    }

    @Test
    fun manyBars_stillFinishWithin700ms() {
        show {
            val grown = rememberBarsGrowIn(Unit)
            BarStrip(List(12) { 1f }, red, Modifier.size(240.dp, 60.dp), grown = grown::value)
        }
        after(700)
        val done = snapshot()
        assertEquals("the last of twelve bars full", 60, (0 until 60).count { isRed(done.getPixel(235, it)) })
    }

    @Test
    fun aRangeChange_drawsTheLineAgain_andAQuickDoubleChange_endsComplete() {
        var range by mutableStateOf(0)
        show { RisingSparkline(range) }
        after(700)
        assertTrue(snapshot().redRight(0, 60) >= 194)
        compose.runOnIdle { range = 1 }
        after(16)
        val again = snapshot().redRight(0, 60)
        assertTrue("starts again from the left ($again)", again < 60)
        after(100)
        compose.runOnIdle { range = 2 }
        after(16)
        assertTrue("and again", snapshot().redRight(0, 60) < 60)
        after(700)
        assertTrue("complete for the last range chosen", snapshot().redRight(0, 60) >= 194)
    }

    @Test
    fun aDataChange_withTheSameRange_doesNotRedraw() {
        var values by mutableStateOf(listOf(0f, 1f, 2f, 3f, 4f))
        show {
            val drawn = rememberDrawIn(Unit, LINE_DRAW_MS)
            Sparkline(values, red, Modifier.size(200.dp, 60.dp), fill = false, drawn = drawn::value)
        }
        after(700)
        compose.runOnIdle { values = listOf(0f, 2f, 1f, 3f, 4f) }
        after(16)
        assertTrue("still complete", snapshot().redRight(0, 60) >= 194)
    }

    @Test
    fun ratioMarker_slidesFromTheStart_toItsValue() {
        var warning = Color.Unspecified
        show {
            warning = Tone.WARNING.color()
            val slid = rememberDrawIn(Unit, MARKER_SLIDE_MS)
            Box(Modifier.width(300.dp).testTag("ratio")) { RatioBar("Bench ÷ Squat", 75, 60, 70, slid = slid::value) }
        }
        // The bar is the bottom 34 dp; the pill above it is amber too.
        val bottom = compose.onNodeWithTag("ratio").fetchSemanticsNode().boundsInRoot.bottom.toInt()
        fun markerX(): Int {
            val bmp = snapshot()
            val xs = (0 until 300).filter { x -> (bottom - 34 until bottom).any { isColor(bmp.getPixel(x, it), warning) } }
            return if (xs.isEmpty()) -1 else xs.average().toInt()
        }
        val start = markerX()
        after(200)
        val mid = markerX()
        after(500)
        val end = markerX()
        assertTrue("at the start first ($start)", start in 0..12)
        assertTrue("on its way at 200 ms ($start < $mid < $end)", mid in (start + 10) until (end - 10))
        assertTrue("at its value by 700 ms ($end)", end > 200)
    }

    @Test
    fun onePoint_orNone_stillRender() {
        show {
            Column {
                val drawn = rememberDrawIn(Unit, LINE_DRAW_MS)
                Box(Modifier.width(300.dp)) { LineChart(listOf(ChartSeries("Squat", red, listOf(140f))), listOf("Now"), drawn = drawn::value) }
                Box(Modifier.width(300.dp)) { LineChart(listOf(ChartSeries("Bench", red, emptyList())), emptyList(), drawn = drawn::value) }
                Sparkline(listOf(3f), red, Modifier.size(200.dp, 40.dp), drawn = drawn::value)
                Sparkline(emptyList(), red, Modifier.size(200.dp, 40.dp), drawn = drawn::value)
                BarStrip(emptyList(), red, Modifier.size(200.dp, 40.dp), grown = rememberBarsGrowIn(Unit)::value)
                BarStrip(listOf(5f), red, Modifier.size(200.dp, 40.dp), grown = rememberBarsGrowIn(Unit)::value)
                BodyweightChart(listOf(LocalDate(2026, 11, 1) to 82.5), color = red, drawn = drawn::value)
                BodyweightChart(emptyList(), color = red, drawn = drawn::value)
            }
        }
        compose.onNodeWithContentDescription("Line chart. Latest: Squat 140 kg").assertExists()
        after(700)
        assertTrue("the single points and the single bar are drawn", snapshot().red(0, 400, 0, 800) > 100)
    }

    @Test
    fun motionOff_everythingIsComplete_atTheFirstFrame() {
        var warning = Color.Unspecified
        show(motion = false) {
            warning = Tone.WARNING.color()
            Column {
                RisingSparkline()
                FourBars()
                Box(Modifier.width(300.dp).testTag("ratio")) { RatioBar("Bench ÷ Squat", 75, 60, 70, slid = rememberDrawIn(Unit, MARKER_SLIDE_MS)::value) }
            }
        }
        val bmp = snapshot()
        assertTrue("line complete", bmp.redRight(0, 60) >= 194)
        for (x in listOf(23, 74, 125, 176)) assertEquals("bar at $x full", 60, (60 until 120).count { isRed(bmp.getPixel(x, it)) })
        val bottom = compose.onNodeWithTag("ratio").fetchSemanticsNode().boundsInRoot.bottom.toInt()
        val marker = (0 until 300).filter { x -> (bottom - 34 until bottom).any { isColor(bmp.getPixel(x, it), warning) } }
        assertTrue("marker at its value ($marker)", marker.isNotEmpty() && marker.average() > 200)
    }

    // ---- The real Progress screen ----

    @Composable
    private fun Progress(motion: Boolean, range: ChartRange, onRange: (ChartRange) -> Unit = {}) = PowertrackerTheme {
        CompositionLocalProvider(LocalMotion provides motion) {
            ProgressContent(ProgressViewModel.build(SAMPLE_HISTORY_WITH_PRESS, range, SHOWCASE_DATE, showStrictPress = true), onRange = onRange, onOpenLift = {})
        }
    }

    @Test
    @Config(qualifiers = "w412dp-h2040dp-mdpi")
    fun progress_drawsIn_withItsWordsFromFrame0_andEndsExactlyAsWithMotionOff() {
        var motion by mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent { Progress(motion, ChartRange.MONTHS_6) }
        compose.mainClock.advanceTimeByFrame()
        assertEquals("both line charts described at the first frame", 2, compose.onAllNodesWithContentDescription("Line chart. Latest:", substring = true).fetchSemanticsNodes().size)
        compose.onNodeWithText("Squat volume this week", substring = true).assertExists()
        compose.onNodeWithText("Bench ÷ Squat").assertExists()
        val first = snapshot()
        after(700)
        val drawn = snapshot()
        compose.runOnIdle { motion = false }
        after(16)
        val still = snapshot()
        assertTrue("the first frame is not the finished screen", !first.sameAs(still))
        assertTrue("by 700 ms the screen is exactly as with motion off", drawn.sameAs(still))
    }

    @Test
    @Config(qualifiers = "w412dp-h2040dp-mdpi")
    fun progress_aRangeChange_drawsAgain_andEndsComplete() {
        var range by mutableStateOf(ChartRange.MONTHS_6)
        compose.mainClock.autoAdvance = false
        compose.setContent { Progress(true, range, onRange = { range = it }) }
        after(700)
        val before = snapshot()
        compose.onNodeWithText("1 yr").performClick()
        after(16)
        compose.onNodeWithText("All").performClick()
        after(16)
        val restarted = snapshot()
        after(700)
        val done = snapshot()
        assertTrue("redrawing after the change", !restarted.sameAs(done))
        assertTrue("the new range differs from the old", !before.sameAs(done))
        after(500)
        assertTrue("and nothing moves after 700 ms", done.sameAs(snapshot()))
    }
}
