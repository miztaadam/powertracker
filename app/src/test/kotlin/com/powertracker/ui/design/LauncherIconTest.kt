package com.powertracker.ui.design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.powertracker.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Draws the launcher icon the way a launcher does: both layers at 108 dp, the middle 72 dp cut out
 * by a mask. One sheet with a circle mask, a rounded-square mask and the themed (monochrome) icon,
 * each 192 px. Like [ScreenshotTest], a normal run writes to `app/build/screenshots/`; the
 * committed `docs/design/screens/launcher-icon.png` is only rewritten with `-Pscreenshots.update=true`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xhdpi")
class LauncherIconTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val outDir = File(if (System.getProperty("screenshots.update") == "true") "../docs/design/screens" else "build/screenshots").apply { mkdirs() }

    private fun icon(id: Int) = ContextCompat.getDrawable(context, id) as AdaptiveIconDrawable

    /** The visible 72 dp of the 108 dp layers, [SIZE] px wide, cut out by [mask]. */
    private fun masked(mask: Path, vararg layers: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.clipPath(mask)
        val bleed = SIZE / 4 // 18 dp of the 72 dp shown
        for (layer in layers) {
            layer.setBounds(-bleed, -bleed, SIZE + bleed, SIZE + bleed)
            layer.draw(canvas)
        }
        return bitmap
    }

    private val circle = Path().apply { addCircle(SIZE / 2f, SIZE / 2f, SIZE / 2f, Path.Direction.CW) }
    private val roundedSquare = Path().apply { addRoundRect(RectF(0f, 0f, SIZE.toFloat(), SIZE.toFloat()), SIZE * 0.22f, SIZE * 0.22f, Path.Direction.CW) }

    private fun Bitmap.count(colour: Int): Int {
        val pixels = IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }
        return pixels.count { it == colour }
    }

    @Test
    fun bothLauncherIconsAreAdaptiveWithAThemedLayer() {
        for (id in listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round)) {
            val icon = icon(id)
            assertNotNull(icon.background)
            assertNotNull(icon.foreground)
            assertNotNull("themed icon layer", icon.monochrome)
        }
    }

    @Test
    fun rendersGritInChalkAndSteelOnGraphite() {
        val icon = icon(R.mipmap.ic_launcher)
        val round = masked(circle, icon.background, icon.foreground)
        val square = masked(roundedSquare, icon.background, icon.foreground)
        val themed = masked(
            circle,
            android.graphics.drawable.ColorDrawable(THEMED_BACKGROUND),
            checkNotNull(icon.monochrome).mutate().apply { setColorFilter(THEMED_INK, PorterDuff.Mode.SRC_IN) },
        )

        for (bitmap in listOf(round, square)) {
            assertEquals("graphite at the edge of the mask", GRAPHITE, bitmap.getPixel(SIZE / 2, 6))
            assertTrue("mostly graphite", bitmap.count(GRAPHITE) > SIZE * SIZE / 2)
            assertTrue("chalk letters", bitmap.count(CHALK) > 1_500)
            assertTrue("steel plates", bitmap.count(PLATE) > 300)
            assertTrue("steel bar", bitmap.count(BAR) > 60)
            // No green anywhere: nothing in the icon is more green than red or blue by a real margin.
            val pixels = IntArray(SIZE * SIZE).also { bitmap.getPixels(it, 0, SIZE, 0, 0, SIZE, SIZE) }
            assertTrue("no green", pixels.none { Color.green(it) > Color.red(it) + 24 && Color.green(it) > Color.blue(it) + 24 })
            // The word sits inside the safe zone: the outer ring of the visible area is background only.
            for (i in 0 until SIZE) {
                for (edge in listOf(10, SIZE - 11)) {
                    // Anything not fully opaque is the soft edge of the mask.
                    assertTrue(bitmap.getPixel(i, edge).let { it == GRAPHITE || Color.alpha(it) < 255 })
                    assertTrue(bitmap.getPixel(edge, i).let { it == GRAPHITE || Color.alpha(it) < 255 })
                }
            }
        }
        assertTrue("corners are masked off", round.getPixel(2, 2) == Color.TRANSPARENT && square.getPixel(1, 1) == Color.TRANSPARENT)
        assertTrue("themed icon draws the word in one tint", themed.count(THEMED_INK) > 2_000)

        val gap = 32
        val sheet = Bitmap.createBitmap(3 * SIZE + 4 * gap, SIZE + 2 * gap, Bitmap.Config.ARGB_8888)
        Canvas(sheet).apply {
            drawColor(WALLPAPER)
            listOf(round, square, themed).forEachIndexed { i, bitmap -> drawBitmap(bitmap, (gap + i * (SIZE + gap)).toFloat(), gap.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG)) }
        }
        File(outDir, "launcher-icon.png").outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val SIZE = 192
        val GRAPHITE = Color.parseColor("#131518")
        val CHALK = Color.parseColor("#F3F4EF")
        val PLATE = Color.parseColor("#B4BAC2")
        val BAR = Color.parseColor("#7E858F")
        val WALLPAPER = Color.parseColor("#2A2F3A")
        val THEMED_BACKGROUND = Color.parseColor("#1F2A33")
        val THEMED_INK = Color.parseColor("#C6E8FF")
    }
}
