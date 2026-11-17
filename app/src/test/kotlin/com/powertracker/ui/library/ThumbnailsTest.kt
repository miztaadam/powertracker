package com.powertracker.ui.library

import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.figure
import com.powertracker.domain.library.figures.ExerciseFigure
import com.powertracker.domain.library.figures.FigureRenderer
import com.powertracker.domain.library.figures.Shape
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.seed.SeedLibrary
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * The thumbnails on the Exercises tab and in the picker are worked out by a background worker: never on the main thread,
 * an empty tile until ready, no flash once ready, one head start for the whole catalogue, and nothing for own exercises. Each
 * test has a loader of its own (through [LocalThumbnails]), so none depends on what another left in the app's cache.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h2400dp-xhdpi")
class ThumbnailsTest {
    @get:Rule
    val compose = createComposeRule()

    private val seeded = SeedLibrary.exercises()
    private val own = Exercise(900, "Tyre flip", Category.ACCESSORY, null, listOf("Quads", "Glutes"), isBuiltIn = false)
    private val catalogue = Library.all().map { it.name }
    private val loaders = mutableListOf<ThumbnailLoader>()

    @After
    fun tearDown() = loaders.forEach { it.close() }

    /** A figure lookup and a shapes step that note what they were asked and on which thread, and how many ran at once. */
    private class Recording {
        val calls = ConcurrentLinkedQueue<Pair<String, Thread>>()
        val looked = ConcurrentLinkedQueue<String>()
        private val running = AtomicInteger()
        val mostAtOnce = AtomicInteger()

        private fun <T> noted(what: String, work: () -> T): T {
            mostAtOnce.accumulateAndGet(running.incrementAndGet(), ::maxOf)
            try {
                calls.add(what to Thread.currentThread())
                return work()
            } finally {
                running.decrementAndGet()
            }
        }

        val figureFor: (String) -> ExerciseFigure? = { name -> looked.add(name); noted("figure $name") { Library.figure(name) } }
        val shapesFor: (ExerciseFigure) -> List<Shape> = { f -> noted("shapes") { FigureRenderer.shapes(f, f.thumbnail) } }
    }

    private fun loader(recording: Recording, dispatcher: CoroutineDispatcher) =
        ThumbnailLoader(recording.figureFor, dispatcher, recording.shapesFor).also { loaders += it }

    private var showing by mutableStateOf(true)

    /** Text size: overrides the font scale the rows read their room from. */
    private var fontScale by mutableStateOf(1f)

    private fun show(loader: ThumbnailLoader, content: @Composable () -> Unit) {
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(LocalThumbnails provides loader, androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, fontScale)) {
                PowertrackerTheme(darkTheme = true) { Box { if (showing) content() } }
            }
        }
        compose.waitForIdle()
    }

    private fun showTab(loader: ThumbnailLoader) = show(loader) {
        ExercisesContent(seeded + own, recentIds = listOf(seeded.first { it.name == "Bench" }.id), onOpen = {})
    }

    private fun tile(row: String) = compose.onNode(hasTestTag("exercise-thumbnail") and hasAnyAncestor(hasTestTag("exercise-$row")), useUnmergedTree = true)

    private fun drawn(row: String): Boolean =
        compose.onAllNodes(hasTestTag("thumbnail-drawing") and hasAnyAncestor(hasTestTag("exercise-$row")), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    /** The catalogue rows composed right now, top to bottom. */
    private fun composedRows(): List<String> = compose.onAllNodes(
        SemanticsMatcher("a catalogue row") { node -> node.config.getOrNull(SemanticsProperties.TestTag)?.removePrefix("exercise-") in catalogue },
        useUnmergedTree = true,
    ).fetchSemanticsNodes().map { it.config[SemanticsProperties.TestTag].removePrefix("exercise-") }

    @Test
    fun exercisesTab_worksOutNoThumbnailOnMainThread() {
        val main = Looper.getMainLooper().thread
        val recording = Recording()
        showTab(loader(recording, Dispatchers.Default))

        // Scroll down through the list and back, as a lifter would.
        val list = compose.onNodeWithTag("exercise-list")
        repeat(6) { list.performTouchInput { swipeUp() } }
        list.performScrollToIndex(0)
        compose.waitUntil(10_000) { drawn("Bench") }

        assertTrue("thumbnails were worked out", recording.calls.any { it.first == "figure Bench" } && recording.calls.any { it.first == "shapes" })
        val onMain = recording.calls.filter { it.second == main }.map { it.first }
        assertTrue("worked out on the main thread: ${onMain.take(5)}", onMain.isEmpty())
    }

    @Test
    fun exercisesTab_emptyTileFirst_thenTheDrawing() {
        val held = StandardTestDispatcher()
        showTab(loader(Recording(), held))

        // The worker has not run: the tile is there, at its size, with nothing drawn on it.
        val before = tile("Bench").fetchSemanticsNode().boundsInRoot
        assertFalse(drawn("Bench"))
        assertEquals("the same tile as an own exercise's", tile("Tyre flip").fetchSemanticsNode().boundsInRoot.size, before.size)

        held.scheduler.advanceUntilIdle()
        compose.waitForIdle()
        assertTrue(drawn("Bench"))
        assertEquals("nothing moves when the drawing arrives", before, tile("Bench").fetchSemanticsNode().boundsInRoot)
    }

    @Test
    fun exercisesTab_readyThumbnails_drawnInFirstFrame() {
        val held = StandardTestDispatcher()
        val loader = loader(Recording(), held)
        // Bench alone is made ready before the tab exists.
        CoroutineScope(held).launch { loader.load("Bench") }
        held.scheduler.advanceUntilIdle()

        showTab(loader)
        assertTrue("ready before the tab: drawn at once", drawn("Bench"))
        val notReady = composedRows().first { it != "Bench" }
        assertFalse(drawn(notReady))

        // Everything is worked out; the tab closes and opens again with the worker held: nothing flashes empty.
        held.scheduler.advanceUntilIdle()
        compose.waitForIdle()
        showing = false
        compose.waitForIdle()
        showing = true
        compose.waitForIdle()
        val rows = composedRows()
        assertTrue(rows.size > 5)
        for (row in rows) assertTrue("$row is drawn in the first frame", drawn(row))
    }

    @Test
    fun exercisesTab_ownExercise_emptyTile_noWorkQueued() {
        val held = StandardTestDispatcher()
        val recording = Recording()
        showTab(loader(recording, held))
        held.scheduler.advanceUntilIdle()
        compose.waitForIdle()

        tile("Tyre flip").assertExists()
        assertFalse("the lifter's own exercise stays an empty tile", drawn("Tyre flip"))
        assertFalse("nothing is asked for it", "Tyre flip" in recording.looked)
    }

    @Test
    fun exercisesTab_headStart_everyCatalogueThumbnailReady_once_oneAtATime() {
        val recording = Recording()
        val loader = loader(recording, Dispatchers.Default)
        showTab(loader)

        // No scrolling: the warm-up gets to every catalogue exercise on its own.
        compose.waitUntil(60_000) { catalogue.all { loader.peek(it) != null } }
        assertEquals("one at a time", 1, recording.mostAtOnce.get())
        assertEquals("each worked out once", catalogue.size, recording.looked.size)
        assertEquals(catalogue.toSet(), recording.looked.toSet())

        // The tab shown again starts no second warm-up.
        showing = false
        compose.waitForIdle()
        showing = true
        compose.waitForIdle()
        Thread.sleep(200)
        assertEquals(catalogue.size, recording.looked.size)
    }

    @Test
    fun exercisesTab_headStart_followsTheListOrder_afterRowsOnScreen() {
        val held = StandardTestDispatcher()
        val recording = Recording()
        showTab(loader(recording, held))
        val onScreen = composedRows()
        held.scheduler.advanceUntilIdle()

        val order = recording.looked.toList()
        assertEquals(catalogue.size, order.size)
        // The rows on screen asked first and were served first.
        assertEquals(onScreen.distinct().toSet(), order.take(onScreen.distinct().size).toSet())
        // Then the warm-up, in the list's order: "Your exercises" (Bench), then the catalogue area by area.
        val warmUp = order.drop(onScreen.distinct().size)
        val areas = warmUp.map { Library.find(it)!!.area.ordinal }
        assertEquals("area by area, as the list shows them", areas.sorted(), areas)
    }

    @Test
    fun thumbnailOrder_isYourExercisesThenTheListThenTheRestOfTheCatalogue() {
        fun entry(name: String) = Library.find(name)!!.let { PickerEntry(it.name, it.muscles, it.kind, it, null) }
        val ownEntry = PickerEntry(own.name, own.muscles, Library.find("Bench")!!.kind, null, own.id)
        // "Your exercises" holds the catalogue's last exercise, which the catalogue's own order would leave to the end.
        val last = catalogue.last()
        val list = PickerList(yours = listOf(ownEntry, entry(last)), all = catalogue.dropLast(1).map(::entry))

        // As one flat list: your exercises first, then the list as given. The lifter's own exercise has no thumbnail.
        assertEquals(listOf(last) + catalogue.dropLast(1), thumbnailOrder(list, byArea = false))

        // Under body areas: your exercises still first, then area by area, every catalogue exercise once.
        val grouped = thumbnailOrder(list, byArea = true)
        assertEquals(last, grouped.first())
        val areas = grouped.drop(1).map { Library.find(it)!!.area.ordinal }
        assertEquals("area by area", areas.sorted(), areas)
        assertEquals(catalogue.sorted(), grouped.sorted())

        // A filtered list: what it shows first, then the rest of the catalogue, so the head start still covers it all.
        val shown = listOf(catalogue[40], catalogue[7], catalogue[120])
        val filtered = thumbnailOrder(PickerList(yours = emptyList(), all = shown.map(::entry)), byArea = false)
        assertEquals(shown, filtered.take(3))
        assertEquals(catalogue.filter { it !in shown }, filtered.drop(3))
    }

    private fun showPicker(loader: ThumbnailLoader) = show(loader) {
        ExercisePickerContent("Add to Monday", seeded + own, listOf(seeded.first { it.name == "Bench" }.id), onPick = {}, onCreateCustom = { _, _, _, _ -> }, onClose = {})
    }

    @Test
    fun picker_neverWorksAThumbnailOutOnTheMainThread() {
        val main = Looper.getMainLooper().thread
        val recording = Recording()
        val loader = loader(recording, Dispatchers.Default)
        showPicker(loader)

        // Into a muscle, down its list, back out and down the whole list, as a lifter would.
        compose.onNodeWithTag("muscle-QUADS").performClick()
        val list = compose.onNodeWithTag("exercise-list")
        repeat(3) { list.performTouchInput { swipeUp() } }
        compose.onNodeWithTag("exercise-search").performTextInput("press")
        repeat(3) { list.performTouchInput { swipeUp() } }
        compose.waitUntil(10_000) { composedRows().any(::drawn) }

        assertTrue("the picker asked for the warm-up", loader.warmUpRequested)
        assertTrue("thumbnails were worked out", recording.calls.any { it.first.startsWith("figure ") } && recording.calls.any { it.first == "shapes" })
        val onMain = recording.calls.filter { it.second == main }.map { it.first }
        assertTrue("worked out on the main thread: ${onMain.take(5)}", onMain.isEmpty())
    }

    /** Review Focus 2: the picker opened before the Exercises tab ever was. Names first, empty tiles, then pictures; nothing moves. */
    @Test
    fun picker_beforeTheTab_namesAndEmptyTilesFirst_thenTheDrawings() {
        val held = StandardTestDispatcher()
        showPicker(loader(Recording(), held))
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-Bench"))

        compose.onNode(hasText("Bench") and hasAnyAncestor(hasTestTag("exercise-Bench")), useUnmergedTree = true).assertExists()
        val before = tile("Bench").fetchSemanticsNode().boundsInRoot
        assertFalse(drawn("Bench"))
        assertEquals("the same tile as an own exercise's", tile("Tyre flip").fetchSemanticsNode().boundsInRoot.size, before.size)

        held.scheduler.advanceUntilIdle()
        compose.waitForIdle()
        assertTrue(drawn("Bench"))
        assertEquals("nothing moves when the drawing arrives", before, tile("Bench").fetchSemanticsNode().boundsInRoot)
        assertFalse("the lifter's own exercise stays an empty tile", drawn("Tyre flip"))
    }

    /** One warm-up per process, whichever list asks first: the picker, then the tab, works each thumbnail out once. */
    @Test
    fun picker_thenTab_oneWarmUp_eachThumbnailOnce() {
        val recording = Recording()
        val loader = loader(recording, Dispatchers.Default)
        var tab by mutableStateOf(false)
        show(loader) {
            if (tab) ExercisesContent(seeded + own, recentIds = listOf(seeded.first { it.name == "Bench" }.id), onOpen = {})
            else ExercisePickerContent("Add to Monday", seeded + own, listOf(seeded.first { it.name == "Bench" }.id), onPick = {}, onCreateCustom = { _, _, _, _ -> }, onClose = {})
        }
        assertTrue(loader.warmUpRequested)
        compose.waitUntil(60_000) { catalogue.all { loader.peek(it) != null } }

        // The tab opened after the picker starts no second warm-up.
        tab = true
        compose.waitForIdle()
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-Bench"))
        assertTrue(drawn("Bench"))
        Thread.sleep(200)
        assertEquals("one at a time", 1, recording.mostAtOnce.get())
        assertEquals("each worked out once", catalogue.size, recording.looked.size)
        assertEquals(catalogue.toSet(), recording.looked.toSet())
    }

    /**
     * Review Focus 1: typing quickly. After each of three letters, a row shows a picture only if its own is ready, and
     * every picture drawn is its own exercise's: the same pixels as that exercise's tile showed before any search.
     */
    @Test
    fun pickerSearch_eachRowShowsItsOwnPicture() {
        val held = StandardTestDispatcher()
        val loader = loader(Recording(), held)
        // Only Bench is ready when the picker opens; the worker is held.
        CoroutineScope(held).launch { loader.load("Bench") }
        held.scheduler.advanceUntilIdle()
        showPicker(loader)
        val search = compose.onNodeWithTag("exercise-search")

        for (letter in listOf("b", "e", "n")) {
            search.performTextInput(letter)
            compose.waitForIdle()
            val rows = composedRows()
            assertTrue("rows for \"$letter\"", rows.isNotEmpty())
            for (row in rows.distinct()) assertEquals("$row after \"$letter\": drawn only if its own is ready", loader.peek(row) != null, drawn(row))
        }

        // With every thumbnail ready, each search's rows show their own pictures.
        search.performTextClearance()
        held.scheduler.advanceUntilIdle()
        compose.waitForIdle()
        val seen = HashMap<String, List<Int>>()
        fun check(step: String) {
            compose.waitForIdle()
            val shot = screen()
            val rows = composedRows().distinct().filter { onScreen(it, shot) }
            assertTrue("rows on screen for $step", rows.size > 2)
            for (row in rows) {
                assertTrue("$row is drawn ($step)", drawn(row))
                val pixels = tilePixels(row, shot)
                val own = seen.getOrPut(row) { pixels }
                // The same picture drawn lower or higher in the list differs only in a few edge pixels' smoothing (about
                // 30 of its 8960); a different picture differs in well over a thousand. (Some exercises share a picture.)
                val ownDiff = differing(own, pixels)
                val nearestOther = seen.filterKeys { loader.peek(it)!!.shapes != loader.peek(row)!!.shapes }.values.minOfOrNull { differing(it, pixels) } ?: Int.MAX_VALUE
                assertTrue("$row shows its own picture ($step): $ownDiff pixels off its own, $nearestOther off the nearest other", ownDiff <= 150 && ownDiff * 4 < nearestOther)
            }
        }
        // Each picture the last search shows, as its own row showed it in the list before any search.
        val last = catalogue.filter { "ben" in Library.normalise(it) }
        assertTrue(last.size > 1)
        for (name in last) {
            compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-$name"))
            compose.waitForIdle()
            seen[name] = tilePixels(name, screen())
        }
        compose.onNodeWithTag("exercise-list").performScrollToIndex(0)
        for (letter in listOf("b", "e", "n")) {
            search.performTextInput(letter)
            check("after \"$letter\"")
        }
        assertTrue("pictures were compared with others", seen.keys.map { loader.peek(it)!!.shapes }.distinct().size > 5)
    }

    /** The screen as drawn. */
    private fun screen(): android.graphics.Bitmap {
        compose.waitForIdle()
        val view = (compose.onRoot().fetchSemanticsNode().root as androidx.compose.ui.platform.ViewRootForTest).view
        return android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888).also { view.draw(android.graphics.Canvas(it)) }
    }

    private fun onScreen(row: String, shot: android.graphics.Bitmap): Boolean {
        val b = tile(row).fetchSemanticsNode().boundsInRoot
        // Whole: a tile cut by the list's edge is left out.
        val whole = tile(row).fetchSemanticsNode().size
        return b.left >= 0 && b.top >= 0 && b.right <= shot.width && b.bottom <= shot.height && b.height.toInt() == whole.height && b.width.toInt() == whole.width
    }

    private fun differing(a: List<Int>, b: List<Int>): Int = if (a.size != b.size) Int.MAX_VALUE else a.indices.count { a[it] != b[it] }

    private fun tilePixels(row: String, shot: android.graphics.Bitmap): List<Int> {
        val b = tile(row).fetchSemanticsNode().boundsInRoot
        val w = b.width.toInt()
        val h = b.height.toInt()
        return IntArray(w * h).also { shot.getPixels(it, 0, w, b.left.toInt(), b.top.toInt(), w, h) }.toList()
    }

    private fun showBuilder(loader: ThumbnailLoader) = show(loader) {
        val state = com.powertracker.ui.plan.builder.PlanBuilderState(
            loading = false,
            mode = com.powertracker.domain.model.TrainingMode.BODYBUILDING,
            step = com.powertracker.ui.plan.builder.BuilderStep.EXERCISES,
            exercises = seeded,
        )
        com.powertracker.ui.plan.builder.PlanBuilderContent(state, com.powertracker.ui.plan.builder.PlanBuilderActions())
    }

    /** A picker with no room for thumbnails (130% text at 320 dp) asks for no warm-up and works nothing out. */
    @Test
    @Config(qualifiers = "w320dp-h2400dp-xhdpi")
    fun picker_largeText_startsNoWarmUp() {
        fontScale = 1.3f
        val recording = Recording()
        val loader = loader(recording, Dispatchers.Default)
        showPicker(loader)
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-Bench"))
        Thread.sleep(200)
        compose.waitForIdle()

        assertFalse("no warm-up asked", loader.warmUpRequested)
        assertTrue("nothing worked out: ${recording.looked.take(5)}", recording.looked.isEmpty())
    }

    /** The same for the plan builder: 130% text at 320 dp leaves its ticked rows no room, so no warm-up. */
    @Test
    @Config(qualifiers = "w320dp-h2400dp-xhdpi")
    fun planBuilder_largeText_startsNoWarmUp() {
        fontScale = 1.3f
        val recording = Recording()
        val loader = loader(recording, Dispatchers.Default)
        showBuilder(loader)
        Thread.sleep(200)
        compose.waitForIdle()

        assertFalse("no warm-up asked", loader.warmUpRequested)
        assertTrue("nothing worked out: ${recording.looked.take(5)}", recording.looked.isEmpty())
    }

    /** The Exercises tab with no room for thumbnails (130% text at 320 dp) asks for no warm-up and works nothing out. */
    @Test
    @Config(qualifiers = "w320dp-h2400dp-xhdpi")
    fun tab_largeText_startsNoWarmUp() {
        fontScale = 1.3f
        val recording = Recording()
        val loader = loader(recording, Dispatchers.Default)
        showTab(loader)
        compose.onNodeWithTag("exercise-list").performScrollToNode(hasTestTag("exercise-Bench"))
        Thread.sleep(200)
        compose.waitForIdle()

        assertFalse("no warm-up asked", loader.warmUpRequested)
        assertTrue("nothing worked out: ${recording.looked.take(5)}", recording.looked.isEmpty())
    }

    /** After a picker that showed no tiles (115% text at 360 dp), the Exercises tab, which keeps its thumbnails there, still warms up. */
    @Test
    @Config(qualifiers = "w360dp-h2400dp-xhdpi")
    fun tab_stillWarmsUp_afterATilelessPicker() {
        fontScale = 1.15f
        val recording = Recording()
        val loader = loader(recording, Dispatchers.Default)
        var tab by mutableStateOf(false)
        show(loader) {
            if (tab) ExercisesContent(seeded + own, recentIds = listOf(seeded.first { it.name == "Bench" }.id), onOpen = {})
            else ExercisePickerContent("Add to Monday", seeded + own, listOf(seeded.first { it.name == "Bench" }.id), onPick = {}, onCreateCustom = { _, _, _, _ -> }, onClose = {})
        }
        assertFalse(loader.warmUpRequested)

        tab = true
        compose.waitForIdle()
        assertTrue("the tab asks for the warm-up", loader.warmUpRequested)
        compose.waitUntil(60_000) { catalogue.all { loader.peek(it) != null } }
        assertEquals("each worked out once", catalogue.size, recording.looked.size)
    }

    @Test
    fun fakeFigureFor_isHonoured_andLeavesTheRealCacheAlone() {
        val held = StandardTestDispatcher()
        val shared = loader(Recording(), held)
        CoroutineScope(held).launch { shared.load("Bench") }
        held.scheduler.advanceUntilIdle()
        val name = "Not in the catalogue"
        show(shared) {
            Column {
                // A lookup that finds a figure for a name the catalogue has not got.
                Box(Modifier.testTag("exercise-fake")) {
                    ExerciseThumbnail(name, figureFor = { Library.figure("Squat") })
                }
                // A lookup that finds nothing, for a name whose real thumbnail is ready.
                Box(Modifier.testTag("exercise-none")) {
                    ExerciseThumbnail("Bench", figureFor = { null })
                }
            }
        }
        assertFalse("not served from the real cache", drawn("none"))
        held.scheduler.advanceUntilIdle()
        compose.waitForIdle()

        assertTrue("the fake lookup's figure is drawn", drawn("fake"))
        assertFalse("the fake lookup's null is honoured", drawn("none"))
        assertNull("the shared loader is not filled", shared.peek(name))
        assertNull("nor is the app's", CatalogueThumbnails.peek(name))
    }
}
