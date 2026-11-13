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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
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
 * The Exercises tab's thumbnails are worked out by a background worker: never on the main thread, an empty tile until
 * ready, no flash once ready, a head start for the whole catalogue, and nothing for the picker or own exercises. Each
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

    private fun show(loader: ThumbnailLoader, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalThumbnails provides loader) {
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

    @Test
    fun picker_startsNoThumbnailWork() {
        val held = StandardTestDispatcher()
        val recording = Recording()
        val loader = loader(recording, held)
        show(loader) {
            ExercisePickerContent("Add to Monday", seeded + own, listOf(seeded.first { it.name == "Bench" }.id), onPick = {}, onCreateCustom = { _, _, _, _ -> }, onClose = {})
        }
        held.scheduler.advanceUntilIdle()
        compose.waitForIdle()

        assertFalse(loader.warmUpRequested)
        assertTrue(recording.looked.isEmpty())
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
