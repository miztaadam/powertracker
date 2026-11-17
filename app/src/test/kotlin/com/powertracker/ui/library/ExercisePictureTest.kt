package com.powertracker.ui.library

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.figure
import com.powertracker.domain.library.figures.Role
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.seed.SeedLibrary
import com.powertracker.ui.design.assertTextsFit
import com.powertracker.ui.theme.DarkColorScheme
import com.powertracker.ui.theme.DarkPtColors
import com.powertracker.ui.theme.LightColorScheme
import com.powertracker.ui.theme.LightPtColors
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.PtColors
import com.powertracker.ui.theme.Space
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * The exercise pictures: the two-pose picture in the guide sheet and the thumbnails on the Exercises tab, in the picker
 * and in the plan builder. "Bench" is the exercise with a figure; a lookup that finds nothing stands in for one without.
 */
// Native graphics, so text is measured at its real size and the canvas really draws: the checks here depend on both.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h2400dp-xhdpi")
class ExercisePictureTest {
    // The activity is started by the test, not the rule, so the font scale can be set before it exists.
    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<ComponentActivity>? = null

    private var dark by mutableStateOf(true)
    private var mode = TrainingMode.POWERLIFTING

    private val bench = Library.figure("Bench")!!
    private val label = "Picture: ${bench.startCaption}, then ${bench.endCaption}."

    @After
    fun tearDown() {
        scenario?.close()
    }

    /** [content] on the sheet's surface, in the theme [dark] chooses. */
    private fun show(fontScale: Float = 1f, content: @Composable () -> Unit) {
        RuntimeEnvironment.setFontScale(fontScale)
        scenario = ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
            activity.setContent {
                PowertrackerTheme(mode = mode, darkTheme = dark) { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) { content() } }
            }
        }
        compose.waitForIdle()
    }

    private fun showGuide(name: String, fontScale: Float = 1f) = show(fontScale) { FormGuideContent(name, onClose = {}) }

    private val own = Exercise(900, "Tyre flip", Category.ACCESSORY, null, listOf("Quads", "Glutes"), isBuiltIn = false)

    private fun showExercisesTab() {
        val seeded = SeedLibrary.exercises()
        show { ExercisesContent(seeded + own, recentIds = listOf(seeded.first { it.name == "Bench" }.id), onOpen = {}) }
    }

    private fun node(tag: String): SemanticsNode = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()

    private fun nodes(tag: String): List<SemanticsNode> = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes()

    private fun toPx(dp: Int): Float = dp * RuntimeEnvironment.getApplication().resources.displayMetrics.density

    /** The screen as drawn. */
    private fun screen(): Bitmap {
        compose.waitForIdle()
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        return Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
    }

    /** The colours drawn inside [bounds], with how many pixels each covers. */
    private fun Bitmap.colours(bounds: Rect): Map<Int, Int> {
        val w = bounds.width.toInt()
        val h = bounds.height.toInt()
        val pixels = IntArray(w * h).also { getPixels(it, 0, w, bounds.left.toInt(), bounds.top.toInt(), w, h) }
        return pixels.toList().groupingBy { it }.eachCount()
    }

    private fun Map<Int, Int>.of(colour: Color): Int = this[colour.toArgb()] ?: 0

    private fun captionLayout(text: String): TextLayoutResult {
        val caption = compose.onNode(hasTestTag("picture-caption") and SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(androidx.compose.ui.text.AnnotatedString(text))), useUnmergedTree = true).fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        caption.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
        return layouts.single()
    }

    @Test
    fun guideSheet_showsTwoPosePicture_withCaptions() {
        showGuide("Bench")

        val picture = node("exercise-picture").boundsInRoot
        val panels = nodes("picture-panel").map { it.boundsInRoot }
        assertEquals("a start and an end panel", 2, panels.size)
        val (start, end) = panels
        // Side by side, the same size, 4:3, 8 dp apart, across the picture's width.
        assertEquals(start.top, end.top, 0.5f)
        assertEquals(start.width, end.width, 1f)
        assertEquals(4f / 3f, start.width / start.height, 0.02f)
        assertEquals(toPx(8), end.left - start.right, 1f)
        assertEquals(picture.left, start.left, 0.5f)
        assertEquals(picture.right, end.right, 0.5f)

        // Each caption is under its own panel.
        val captions = nodes("picture-caption")
        assertEquals(listOf(bench.startCaption, bench.endCaption), captions.map { it.config[SemanticsProperties.Text].single().text })
        for ((panel, caption) in panels.zip(captions.map { it.boundsInRoot })) {
            assertTrue("the caption is under its panel", caption.top >= panel.bottom)
            assertTrue("the caption is as wide as its panel at most", caption.left >= panel.left - 0.5f && caption.right <= panel.right + 0.5f)
        }

        // Both panels are drawn, and differently: the bar is on the chest in one and locked out in the other.
        val shot = screen()
        val scheme = DarkColorScheme
        val drawn = panels.map { shot.colours(it) }
        for (colours in drawn) {
            assertTrue("the body is drawn", colours.of(scheme.onSurface) > 200)
            assertTrue("the equipment is drawn", colours.of(DarkPtColors.muted) > 100)
            assertTrue("the bar is drawn", colours.of(DarkPtColors.accent) > 20)
        }
        assertTrue("the two poses differ", drawn[0] != drawn[1])

        // Under the name and the area and equipment line, above the purpose text.
        val eyebrow = compose.onNodeWithText("MAIN LIFTS · BARBELL").fetchSemanticsNode().boundsInRoot
        val purpose = compose.onNodeWithText(Library.find("Bench")!!.guide.purpose).fetchSemanticsNode().boundsInRoot
        assertTrue(picture.top >= eyebrow.bottom)
        assertTrue(picture.bottom <= purpose.top)
    }

    @Test
    fun guideSheet_pictureLabel_readsBothCaptions() {
        showGuide("Bench")

        assertEquals("Picture: Pause on your chest, then Press until elbows lock.", label)
        // One element for a screen reader: the label, and nothing under it speaks.
        val picture = compose.onNodeWithContentDescription(label).fetchSemanticsNode()
        assertEquals("exercise-picture", picture.config[SemanticsProperties.TestTag])
        assertEquals(androidx.compose.ui.semantics.Role.Image, picture.config[SemanticsProperties.Role])
        assertTrue("nothing inside the picture is read on its own", picture.children.isEmpty())
        compose.onNodeWithText(bench.startCaption).assertDoesNotExist()
        compose.onNodeWithText(bench.endCaption).assertDoesNotExist()
    }

    @Test
    fun guideSheet_noFigure_showsNoPictureArea() {
        // A catalogue exercise whose figure is not found: the guide is as it was before pictures.
        show { FormGuideContent("Bench", onClose = {}, figureFor = { null }) }
        compose.onNodeWithTag("exercise-picture", useUnmergedTree = true).assertDoesNotExist()
        compose.onAllNodesWithTag("picture-panel", useUnmergedTree = true).assertCountEquals(0)
        val eyebrow = compose.onNodeWithText("MAIN LIFTS · BARBELL").fetchSemanticsNode().boundsInRoot
        val purpose = compose.onNodeWithText(Library.find("Bench")!!.guide.purpose).fetchSemanticsNode().boundsInRoot
        assertTrue("the purpose follows the header: no room is kept for a picture", purpose.top - eyebrow.bottom < toPx(40))
        scenario?.close()

        // A custom exercise keeps its one line, with no picture.
        assertNull(Library.figure("Tyre flip"))
        showGuide("Tyre flip")
        compose.onNodeWithText(NO_GUIDE).assertExists()
        compose.onNodeWithTag("exercise-picture", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun exercisesTab_rowsShowThumbnail() {
        showExercisesTab()
        // The thumbnail is worked out in the background: wait for its drawing.
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasTestTag("thumbnail-drawing") and hasAnyAncestor(hasTestTag("exercise-Bench")), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

        val row = node("exercise-Bench")
        val thumbnail = compose.onNode(hasTestTag("exercise-thumbnail") and hasAnyAncestor(hasTestTag("exercise-Bench")), useUnmergedTree = true).fetchSemanticsNode()
        val bounds = thumbnail.boundsInRoot
        // About 56 × 40, at the left of the row, before the name, centred in the row's height.
        assertEquals(toPx(56), bounds.width, 1f)
        assertEquals(toPx(40), bounds.height, 1f)
        assertEquals(toPx(16), bounds.left - row.boundsInRoot.left, 1f)
        val name = compose.onNode(androidx.compose.ui.test.hasText("Bench") and hasAnyAncestor(hasTestTag("exercise-Bench")), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("the thumbnail is left of the name", bounds.right < name.left)
        assertEquals(row.boundsInRoot.center.y, bounds.center.y, 1f)

        // The figure is drawn on the tile, in the role colours.
        val colours = screen().colours(bounds)
        assertTrue("the body is drawn", colours.of(DarkColorScheme.onSurface) > 30)
        assertTrue("the equipment is drawn", colours.of(DarkPtColors.muted) > 10)
        assertTrue("the tile shows around it", colours.of(DarkColorScheme.surfaceContainerHigh) > 500)

        // Decorative: the row already names the exercise, so the thumbnail says nothing and is no element of its own.
        assertNull(thumbnail.config.getOrNull(SemanticsProperties.ContentDescription))
        assertNull(thumbnail.config.getOrNull(SemanticsProperties.Role))
        assertTrue(compose.onAllNodes(hasTestTag("exercise-thumbnail")).fetchSemanticsNodes().isEmpty())
        // The row's target is no smaller for it.
        compose.onNodeWithTag("exercise-Bench").assertHeightIsAtLeast(Space.touch)

        // Every row has one, so the names line up.
        val rows = compose.onAllNodes(SemanticsMatcher("an exercise row") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("exercise-") == true && it.config.getOrNull(SemanticsProperties.TestTag) !in setOf("exercise-thumbnail", "exercise-search", "exercise-list", "exercise-picture") }).fetchSemanticsNodes()
        assertTrue(rows.size > 5)
        assertEquals(rows.size, nodes("exercise-thumbnail").size)
    }

    @Test
    fun exercisesTab_customExercise_showsEmptyTile() {
        showExercisesTab()

        val tile = compose.onNode(hasTestTag("exercise-thumbnail") and hasAnyAncestor(hasTestTag("exercise-Tyre flip")), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val benchTile = compose.onNode(hasTestTag("exercise-thumbnail") and hasAnyAncestor(hasTestTag("exercise-Bench")), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals("the same size as a drawn thumbnail", benchTile.size, tile.size)
        assertEquals("names stay aligned", benchTile.left, tile.left, 0.5f)

        // Nothing but the muted tile and its rounded corners over the screen behind it.
        val colours = screen().colours(tile)
        val area = (tile.width.toInt() * tile.height.toInt())
        assertTrue("the tile is one flat muted colour", colours.of(DarkColorScheme.surfaceContainerHigh) > area * 0.97)
        assertEquals("nothing is drawn on it", 0, colours.of(DarkColorScheme.onSurface) + colours.of(DarkPtColors.muted) + colours.of(DarkPtColors.accent))
    }

    private fun tileOf(row: String): SemanticsNode =
        compose.onNode(hasTestTag("exercise-thumbnail") and hasAnyAncestor(hasTestTag(row)), useUnmergedTree = true).fetchSemanticsNode()

    private fun drawnIn(row: String): Boolean =
        compose.onAllNodes(hasTestTag("thumbnail-drawing") and hasAnyAncestor(hasTestTag(row)), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun textIn(row: String, text: String): SemanticsNode =
        compose.onNode(androidx.compose.ui.test.hasText(text) and hasAnyAncestor(hasTestTag(row)), useUnmergedTree = true).fetchSemanticsNode()

    @Test
    fun picker_rowsShowThumbnail() {
        val seeded = SeedLibrary.exercises()
        for (side in TrainingMode.entries) {
            scenario?.close()
            mode = side
            show { ExercisePickerContent("Add to Monday", seeded + own, listOf(seeded.first { it.name == "Bench" }.id), onPick = {}, onCreateCustom = { _, _, _, _ -> }, onClose = {}) }

            val list = compose.onNodeWithTag("exercise-list")
            list.performScrollToNode(hasTestTag("exercise-Bench"))
            // The thumbnail is worked out in the background: wait for its drawing.
            compose.waitUntil(10_000) { drawnIn("exercise-Bench") }
            val row = node("exercise-Bench").boundsInRoot
            val tile = tileOf("exercise-Bench")
            // The tab's tile: about 56 × 40, at the left of the row, before the name, centred in the row's height.
            assertEquals("$side", toPx(56), tile.boundsInRoot.width, 1f)
            assertEquals("$side", toPx(40), tile.boundsInRoot.height, 1f)
            assertEquals("$side", toPx(16), tile.boundsInRoot.left - row.left, 1f)
            val benchName = textIn("exercise-Bench", "Bench").boundsInRoot
            assertTrue("$side: the thumbnail is left of the name", tile.boundsInRoot.right < benchName.left)
            assertEquals("$side", row.center.y, tile.boundsInRoot.center.y, 1f)
            // Decorative, as on the tab: it says nothing, and the row's spoken name is unchanged.
            assertNull(tile.config.getOrNull(SemanticsProperties.ContentDescription))
            assertTrue(compose.onAllNodes(hasTestTag("exercise-thumbnail")).fetchSemanticsNodes().isEmpty())
            compose.onNodeWithTag("exercise-Bench").assertHeightIsAtLeast(Space.touch)

            // The lifter's own exercise: an empty tile of the same size, so its name lines up with Bench's.
            list.performScrollToNode(hasTestTag("exercise-Tyre flip"))
            val ownTile = tileOf("exercise-Tyre flip").boundsInRoot
            assertEquals("$side: the same size as a drawn thumbnail", tile.boundsInRoot.size, ownTile.size)
            assertEquals("$side", tile.boundsInRoot.left, ownTile.left, 0.5f)
            assertFalse("$side: nothing is drawn for it", drawnIn("exercise-Tyre flip"))
            assertEquals("$side: names stay aligned", benchName.left, textIn("exercise-Tyre flip", "Tyre flip").boundsInRoot.left, 0.5f)
        }
    }

    /** The names that broke inside a word in a ticked row at 320 dp with a thumbnail beside them. */
    private val longNames = listOf(
        "Safety-bar squat", "Chest-supported row", "Chest-supported T-bar row", "Neutral-grip pull-up",
        "Chest-supported rear delt raise", "Smith shoulder press", "Concentration curl", "Bodyweight squat",
    )

    /** The plan builder's exercises step with [longNames] stored and ticked: each is under "Chosen" and in the list. */
    private fun showBuilderWithLongNamesTicked(fontScale: Float = 1f) {
        val stored = longNames.mapIndexed { i, n ->
            val lib = Library.find(n)!!
            Exercise(100L + i, lib.name, lib.category, lib.parentLift, lib.muscles, isBuiltIn = true)
        }
        val entries = stored.map(com.powertracker.ui.plan.builder.PlanBuilderViewModel::entryFor)
        val ticked = entries.associate { com.powertracker.ui.plan.builder.pickKey(it) to com.powertracker.ui.plan.builder.PlanBuilderViewModel.chosen(it) }
        val state = com.powertracker.ui.plan.builder.PlanBuilderState(
            loading = false,
            mode = TrainingMode.BODYBUILDING,
            step = com.powertracker.ui.plan.builder.BuilderStep.EXERCISES,
            exercises = stored,
            recentIds = stored.map { it.id },
            ticked = ticked,
        )
        mode = TrainingMode.BODYBUILDING
        show(fontScale) {
            com.powertracker.ui.plan.builder.PlanBuilderContent(
                state,
                com.powertracker.ui.plan.builder.PlanBuilderActions(isTicked = { com.powertracker.ui.plan.builder.pickKey(it) in state.ticked }),
            )
        }
    }

    /** [name] in [row] wraps only between words (or after a hyphen), never inside one. */
    private fun assertNameWhole(row: String, name: String) {
        val node = textIn(row, name)
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
        val layout = layouts.single()
        val lines = (0 until layout.lineCount).map { name.substring(layout.getLineStart(it), layout.getLineEnd(it)) }
        for (line in 0 until layout.lineCount - 1) {
            val end = layout.getLineEnd(line, visibleEnd = true)
            assertTrue("$row: \"$name\" is broken inside a word: $lines", name[end] == ' ' || name[end - 1] == ' ' || name[end - 1] == '-')
        }
    }

    /** The row's tick, name, equipment and 48 dp info button, left to right inside the row. */
    private fun assertRowFits(row: String, name: String) {
        val bounds = node(row).boundsInRoot
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Checkbox) and hasTestTag(row), useUnmergedTree = true).assertExists()
        val text = textIn(row, name).boundsInRoot
        val info = compose.onNode(
            androidx.compose.ui.test.hasClickAction() and androidx.compose.ui.test.hasAnyDescendant(androidx.compose.ui.test.hasContentDescription("Form guide for $name")) and hasAnyAncestor(hasTestTag(row)),
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        assertTrue("$row: the name is inside the row", text.left >= bounds.left && text.right <= info.left)
        assertTrue("$row: the info button is inside the row", info.right <= bounds.right + 0.5f)
        assertTrue("$row: the info button keeps its 48 dp target", info.width >= toPx(48) - 0.5f && info.height >= toPx(48) - 0.5f)
    }

    /**
     * The plan builder at 320 dp: a ticked row has its tick box, so under 360 dp it shows no thumbnail and no empty tile,
     * and every long name stays whole. The picker at the same width keeps its thumbnails.
     */
    @Test
    @Config(qualifiers = "w320dp-h2400dp-xhdpi")
    fun planBuilder_tickedRow_namesWholeAt320() {
        for (theme in listOf(true, false)) {
            scenario?.close()
            dark = theme
            showBuilderWithLongNamesTicked()
            assertEquals(toPx(320), compose.onRoot().fetchSemanticsNode().boundsInRoot.width, 0.5f)
            for (name in longNames) {
                for (row in listOf("chosen-$name", "exercise-$name")) {
                    compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag(row))
                    assertTrue("$row has no tile", compose.onAllNodes(hasTestTag("exercise-thumbnail") and hasAnyAncestor(hasTestTag(row)), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
                    assertNameWhole(row, name)
                    assertRowFits(row, name)
                }
            }
            compose.assertTextsFit()
        }

        // The picker is unchanged at this width: its rows keep the thumbnail.
        scenario?.close()
        mode = TrainingMode.POWERLIFTING
        show { ExercisePickerContent("Add to Monday", SeedLibrary.exercises() + own, emptyList(), onPick = {}, onCreateCustom = { _, _, _, _ -> }, onClose = {}) }
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-Concentration curl"))
        assertEquals(toPx(56), tileOf("exercise-Concentration curl").boundsInRoot.width, 1f)
        assertNameWhole("exercise-Concentration curl", "Concentration curl")
    }

    /** The plan builder at 360 dp: ticked rows show the tab's thumbnail, and the long names still stay whole. */
    @Test
    @Config(qualifiers = "w360dp-h2400dp-xhdpi")
    fun planBuilder_showsThumbnailsAt360() {
        showBuilderWithLongNamesTicked()
        for (name in longNames) {
            for (row in listOf("chosen-$name", "exercise-$name")) {
                compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag(row))
                compose.waitUntil(10_000) { drawnIn(row) }
                val tile = tileOf(row).boundsInRoot
                assertEquals(toPx(56), tile.width, 1f)
                assertEquals(toPx(40), tile.height, 1f)
                assertTrue("$row: the name follows the tile", textIn(row, name).boundsInRoot.left > tile.right)
                assertNameWhole(row, name)
                assertRowFits(row, name)
            }
        }
        compose.assertTextsFit()
    }

    /**
     * Scrolls the list from top to bottom and returns every catalogue name, with the rows it was seen in, that wraps
     * inside a word, and how many catalogue names it saw.
     */
    private fun namesBrokenInsideAWord(): Pair<List<String>, Int> {
        val catalogue = Library.all().map { it.name }
        val broken = LinkedHashSet<String>()
        val seen = HashSet<String>()
        val list = compose.onNodeWithTag("exercise-list")
        var index = 0
        while (true) {
            try {
                list.performScrollToIndex(index)
            } catch (e: Throwable) {
                break
            }
            compose.waitForIdle()
            for (name in catalogue) {
                for (row in listOf("exercise-$name", "chosen-$name")) {
                    val nodes = compose.onAllNodes(
                        SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(androidx.compose.ui.text.AnnotatedString(name))) and hasAnyAncestor(hasTestTag(row)),
                        useUnmergedTree = true,
                    ).fetchSemanticsNodes()
                    if (nodes.isEmpty()) continue
                    seen += name
                    val layouts = mutableListOf<TextLayoutResult>()
                    nodes.first().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
                    val layout = layouts.single()
                    val inside = (0 until layout.lineCount - 1).any { line ->
                        val end = layout.getLineEnd(line, visibleEnd = true)
                        !(name[end] == ' ' || name[end - 1] == ' ' || name[end - 1] == '-')
                    }
                    if (inside) broken += "$row: " + (0 until layout.lineCount).map { name.substring(layout.getLineStart(it), layout.getLineEnd(it)) }
                }
            }
            index += 4
        }
        return broken.toList() to seen.size
    }

    private fun showPicker(fontScale: Float) = show(fontScale) {
        ExercisePickerContent("Add to Monday", SeedLibrary.exercises() + own, emptyList(), onPick = {}, onCreateCustom = { _, _, _, _ -> }, onClose = {})
    }

    private fun thumbnailCount(): Int = nodes("exercise-thumbnail").size

    /**
     * At 130% text the picker leaves the thumbnails out at 320 and 360 dp, so no catalogue name breaks inside a word. At
     * its edge ([PICKER_THUMBNAIL_MIN_ROOM_DP] of text room on a 320 dp screen, the narrowest) and at 360 dp at 115% it
     * keeps them, and the names still stay whole.
     */
    @Test
    @Config(qualifiers = "w320dp-h2400dp-xhdpi")
    fun picker_namesWholeAtFontScale1_3() {
        val edge = 320f / PICKER_THUMBNAIL_MIN_ROOM_DP
        for ((width, scale, tiles) in listOf(Triple(320, 1.3f, false), Triple(360, 1.3f, false), Triple(320, edge, true), Triple(360, 1.15f, true))) {
            scenario?.close()
            RuntimeEnvironment.setQualifiers("w${width}dp-h2400dp-xhdpi")
            showPicker(scale)
            assertEquals(toPx(width), compose.onRoot().fetchSemanticsNode().boundsInRoot.width, 0.5f)
            assertEquals("tiles at $width dp, text x$scale", tiles, thumbnailCount() > 0)
            val (broken, seen) = namesBrokenInsideAWord()
            assertEquals("every catalogue name checked at $width dp, text x$scale", Library.all().size, seen)
            assertTrue("at $width dp, text x$scale, broken inside a word: $broken", broken.isEmpty())
        }
    }

    /** At the default text size the picker keeps its thumbnails at 320 dp. */
    @Test
    @Config(qualifiers = "w320dp-h2400dp-xhdpi")
    fun picker_showsThumbnailsAt320_defaultFont() {
        showPicker(1f)
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-Concentration curl"))
        compose.waitUntil(10_000) { drawnIn("exercise-Concentration curl") }
        assertEquals(toPx(56), tileOf("exercise-Concentration curl").boundsInRoot.width, 1f)
        assertNameWhole("exercise-Concentration curl", "Concentration curl")
    }

    /** At 130% text the plan builder at 360 dp leaves the thumbnails out, so no name breaks inside a word. */
    @Test
    @Config(qualifiers = "w360dp-h2400dp-xhdpi")
    fun planBuilder_namesWholeAtFontScale1_3() {
        showBuilderWithLongNamesTicked(fontScale = 1.3f)
        val (broken, seen) = namesBrokenInsideAWord()
        assertEquals("every catalogue name checked", Library.all().size, seen)
        assertTrue("130% text, broken inside a word: $broken", broken.isEmpty())
        assertEquals("no tiles at 130% text", 0, thumbnailCount())
    }

    /**
     * The Exercises tab at large text: below [TAB_THUMBNAIL_MIN_ROOM_DP] of text room (320 dp at 115% and 130%, 360 dp at
     * 130%) its rows leave the thumbnail out, so no catalogue name breaks inside a word; at 360 dp at 115% and 412 dp at
     * 115% and 130% they keep it, and the names still stay whole.
     */
    @Test
    @Config(qualifiers = "w320dp-h2400dp-xhdpi")
    fun exercisesTab_namesWholeAtLargeText() {
        val seeded = SeedLibrary.exercises()
        for ((width, scale, tiles) in listOf(Triple(320, 1.15f, false), Triple(320, 1.3f, false), Triple(360, 1.15f, true), Triple(360, 1.3f, false), Triple(412, 1.15f, true), Triple(412, 1.3f, true))) {
            scenario?.close()
            RuntimeEnvironment.setQualifiers("w${width}dp-h2400dp-xhdpi")
            show(scale) { ExercisesContent(seeded + own, recentIds = listOf(seeded.first { it.name == "Bench" }.id), onOpen = {}) }
            assertEquals(toPx(width), compose.onRoot().fetchSemanticsNode().boundsInRoot.width, 0.5f)
            assertEquals("tiles at $width dp, text x$scale", tiles, thumbnailCount() > 0)
            val (broken, seen) = namesBrokenInsideAWord()
            assertEquals("every catalogue name checked at $width dp, text x$scale", Library.all().size, seen)
            assertTrue("at $width dp, text x$scale, broken inside a word: $broken", broken.isEmpty())
        }

        // The edge, on the narrowest phone: 310 dp of text room has no tile, 311 has one.
        for ((room, tiles) in listOf(310f to false, 311f to true)) {
            scenario?.close()
            RuntimeEnvironment.setQualifiers("w320dp-h2400dp-xhdpi")
            show(320f / room) { ExercisesContent(seeded + own, recentIds = emptyList(), onOpen = {}) }
            assertEquals("tiles at $room dp of text room", tiles, thumbnailCount() > 0)
        }
    }

    /** At the edge of the picker's room: 308 dp at the default text size has no tile, 309 dp has one. */
    @Test
    @Config(qualifiers = "w308dp-h2400dp-xhdpi")
    fun picker_thumbnailEdge_308hides_309shows() {
        for ((width, tiles) in listOf(308 to false, 309 to true)) {
            scenario?.close()
            RuntimeEnvironment.setQualifiers("w${width}dp-h2400dp-xhdpi")
            showPicker(1f)
            compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-Bench"))
            assertEquals("tiles at $width dp", tiles, thumbnailCount() > 0)
        }
    }

    /** At the edge of a ticked row's room: 359 dp at the default text size has no tile, 360 dp has one. */
    @Test
    @Config(qualifiers = "w359dp-h2400dp-xhdpi")
    fun planBuilder_thumbnailEdge_359hides_360shows() {
        for ((width, tiles) in listOf(359 to false, 360 to true)) {
            scenario?.close()
            RuntimeEnvironment.setQualifiers("w${width}dp-h2400dp-xhdpi")
            showBuilderWithLongNamesTicked()
            compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("chosen-Concentration curl"))
            assertEquals("tiles at $width dp", tiles, thumbnailCount() > 0)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h2400dp-xhdpi")
    fun picture_noOverflow_atPhoneWidth() {
        showGuide("Bench")

        val sheet = node("form-guide").boundsInRoot
        assertEquals(toPx(360), sheet.width, 0.5f)
        val picture = node("exercise-picture")
        for (inside in listOf(picture) + nodes("picture-panel") + nodes("picture-caption")) {
            val b = inside.boundsInRoot
            assertTrue("inside the sheet ($b in $sheet)", b.left >= sheet.left && b.right <= sheet.right)
            // Laid out whole: nothing is cut by its parent.
            assertEquals(inside.size.width.toFloat(), b.width, 0.5f)
            assertEquals(inside.size.height.toFloat(), b.height, 0.5f)
        }
        // The same margins as the guide's text.
        val purpose = compose.onNodeWithText(Library.find("Bench")!!.guide.purpose).fetchSemanticsNode().boundsInRoot
        assertEquals(purpose.left, picture.boundsInRoot.left, 0.5f)
        assertEquals(toPx(24), sheet.right - picture.boundsInRoot.right, 0.5f)
        for (caption in listOf(bench.startCaption, bench.endCaption)) {
            val layout = captionLayout(caption)
            assertFalse("\"$caption\" is cut off", (0 until layout.lineCount).any { layout.isLineEllipsized(it) } || layout.hasVisualOverflow)
        }
        compose.assertTextsFit()

        // The Exercises tab rows fit too: the name, the equipment and the info button keep their room beside the thumbnail.
        scenario?.close()
        showExercisesTab()
        val row = node("exercise-Bench").boundsInRoot
        assertEquals(toPx(360), row.width, 0.5f)
        // (The chip rows above scroll sideways, so only the rows are checked here.)
        val texts = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) and hasAnyAncestor(hasTestTag("exercise-Bench")), useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("the name, the muscles and the equipment", texts.size >= 3)
        for (text in texts) {
            val b = text.boundsInRoot
            assertTrue("${text.config[SemanticsProperties.Text]} is inside the row", b.left >= row.left && b.right <= row.right && b.width > 0f)
            assertEquals("${text.config[SemanticsProperties.Text]} is not clipped", text.size.width.toFloat(), b.width, 0.5f)
            val layouts = mutableListOf<TextLayoutResult>()
            text.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
            val l = layouts.single()
            val widest = (0 until l.lineCount).maxOf { l.getLineRight(it) - l.getLineLeft(it) }
            assertTrue("${l.layoutInput.text} is cut off", widest <= l.size.width + 1f && (0 until l.lineCount).none { l.isLineEllipsized(it) })
        }
        compose.onNodeWithContentDescription("Form guide for Bench").assertHeightIsAtLeast(Space.touch)
    }

    @Test
    fun picture_contrast_darkAndLight() {
        fun ratio(a: Color, b: Color): Double {
            val la = a.luminance() + 0.05
            val lb = b.luminance() + 0.05
            return (maxOf(la, lb) / minOf(la, lb)).toDouble()
        }

        fun check(name: String, scheme: ColorScheme, pt: PtColors) {
            // The guide's picture sits on the sheet; a thumbnail sits on its tile.
            for ((on, surface) in listOf("the sheet" to scheme.surface, "the thumbnail tile" to thumbnailSurface(scheme))) {
                val colours = figureColors(scheme, pt, surface)
                for (role in listOf(Role.BODY, Role.EQUIPMENT, Role.ACCENT)) {
                    val r = ratio(colours.of(role), surface)
                    assertTrue("$name: $role on $on is $r", r >= 3.0)
                }
                // The roles are the tokens the design names.
                assertEquals(scheme.onSurface, colours.of(Role.BODY))
                assertEquals(pt.muted, colours.of(Role.EQUIPMENT))
                assertEquals(pt.accent, colours.of(Role.ACCENT))
                assertEquals(pt.accent.copy(alpha = 0.10f), colours.of(Role.ACCENT_FILL))
                assertEquals(surface, colours.of(Role.GAP))
                assertEquals(androidx.compose.ui.graphics.lerp(scheme.onSurface, surface, 0.45f), colours.of(Role.BODY_FAR))
                assertTrue("$name: far limbs are lighter than the body but still show", ratio(colours.of(Role.BODY_FAR), surface) in 2.0..ratio(colours.of(Role.BODY), surface))
            }
        }
        check("dark", DarkColorScheme, DarkPtColors)
        check("light", LightColorScheme, LightPtColors)
    }

    @Test
    @Config(qualifiers = "w360dp-h2400dp-xhdpi")
    fun picture_largeText_captionsWrap() {
        showGuide("Bench", fontScale = 2f)
        assertEquals(2f, RuntimeEnvironment.getApplication().resources.configuration.fontScale, 0.001f)

        val sheet = node("form-guide").boundsInRoot
        val panels = nodes("picture-panel").map { it.boundsInRoot }
        val captions = nodes("picture-caption")
        for ((caption, panel) in captions.zip(panels)) {
            val text = caption.config[SemanticsProperties.Text].single().text
            val layout = captionLayout(text)
            assertTrue("\"$text\" wraps at this size", layout.lineCount >= 2)
            assertFalse("\"$text\" is cut off", (0 until layout.lineCount).any { layout.isLineEllipsized(it) })
            assertFalse("\"$text\" overflows its box", layout.hasVisualOverflow)
            // Every word is laid out: the last line ends at the caption's last character.
            assertEquals(text.length, layout.getLineEnd(layout.lineCount - 1))
            val b = caption.boundsInRoot
            assertTrue("the caption stays under its panel", b.left >= panel.left - 0.5f && b.right <= panel.right + 0.5f && b.top >= panel.bottom)
            assertEquals("the caption is not clipped", caption.size.height.toFloat(), b.height, 0.5f)
        }
        // The panels keep their shape and stay inside the sheet.
        for (panel in panels) {
            assertEquals(4f / 3f, panel.width / panel.height, 0.02f)
            assertTrue(panel.left >= sheet.left && panel.right <= sheet.right)
        }
        assertTrue(panels[1].right <= sheet.right - toPx(24) + 0.5f)
        // The purpose text starts below the taller caption.
        val purpose = compose.onNodeWithText(Library.find("Bench")!!.guide.purpose).fetchSemanticsNode().boundsInRoot
        assertTrue(captions.all { it.boundsInRoot.bottom <= purpose.top })
        compose.assertTextsFit()
    }

    @Test
    fun picture_followsThemeChange() {
        showGuide("Bench")
        val panel = nodes("picture-panel").first()

        val darkColours = screen().colours(panel.boundsInRoot)
        assertTrue(darkColours.of(DarkColorScheme.onSurface) > 200)
        assertEquals(0, darkColours.of(LightColorScheme.onSurface))

        // The theme changes under the open sheet: the same picture, recoloured, without being opened again.
        dark = false
        val lightColours = screen().colours(nodes("picture-panel").first().boundsInRoot)
        // The same shapes in the new colours. Not to the pixel: edges and the translucent plate blend differently.
        fun same(was: Int, now: Int) = assertTrue("$was pixels, now $now", now > 0 && abs(was - now) <= was / 10)
        same(darkColours.of(DarkColorScheme.onSurface), lightColours.of(LightColorScheme.onSurface))
        same(darkColours.of(DarkPtColors.muted), lightColours.of(LightPtColors.muted))
        same(darkColours.of(DarkPtColors.accent), lightColours.of(LightPtColors.accent))
        assertEquals(0, lightColours.of(DarkColorScheme.onSurface))
        assertTrue("the sheet is light behind it", lightColours.of(LightColorScheme.surface) > abs(lightColours.of(LightColorScheme.onSurface)))
        assertNotNull(compose.onNodeWithContentDescription(label).fetchSemanticsNode())
    }
}
