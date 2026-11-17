package com.powertracker.ui.library

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.figure
import com.powertracker.domain.library.figures.ExerciseFigure
import com.powertracker.domain.library.figures.FigureRenderer
import com.powertracker.domain.library.figures.Shape
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/** A row's worked-out thumbnail: the shapes of its figure's thumbnail pose, or null when there is no figure. */
@Immutable
class Thumbnail(val shapes: List<Shape>?)

/**
 * Works out the thumbnails of the Exercises tab, the picker and the plan builder away from the main thread, and keeps
 * the ones that are ready.
 *
 * All the work (the [figureFor] lookup, which solves the figure's poses the first time, and [shapesFor]) is done by
 * one worker coroutine on [dispatcher], one thumbnail at a time, so it never competes with itself for the cores.
 * Its queue has two parts: the rows on screen that asked ([load]) come first, in the order they asked; then the
 * warm-up's names ([warmUp]), skipping any already ready. So a row on screen waits at most for the one thumbnail
 * in hand. A row that leaves the screen before its thumbnail is ready withdraws its request.
 *
 * [peek] only reads the cache, so a row can draw a ready thumbnail in its first frame.
 */
class ThumbnailLoader(
    private val figureFor: (String) -> ExerciseFigure?,
    val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val shapesFor: (ExerciseFigure) -> List<Shape> = { FigureRenderer.shapes(it, it.thumbnail) },
) {
    private val ready = ConcurrentHashMap<String, Thumbnail>()

    private class Request {
        val done = CompletableDeferred<Thumbnail>()
        var waiting = 0
    }

    private val lock = Any()

    // Guarded by lock.
    private val asked = LinkedHashMap<String, Request>()
    private var warmUpNames: List<String> = emptyList()
    private var warmUpNext = 0
    private var warmUpStarted = false
    private var working = false

    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    /** The thumbnail of [name] if it is ready. Computes nothing, so it is safe on the main thread. */
    fun peek(name: String): Thumbnail? = ready[name]

    /** Whether [warmUp] has been asked for. */
    val warmUpRequested: Boolean get() = synchronized(lock) { warmUpStarted }

    /** The thumbnail of [name], from the cache or from the worker, ahead of the warm-up. */
    suspend fun load(name: String): Thumbnail {
        ready[name]?.let { return it }
        val request = synchronized(lock) {
            ready[name]?.let { return it }
            asked.getOrPut(name, ::Request).also {
                it.waiting++
                startWorker()
            }
        }
        wake.trySend(Unit)
        try {
            return request.done.await()
        } finally {
            synchronized(lock) {
                request.waiting--
                // Nobody waits for it any more (the row left the screen): the warm-up will come to it in its turn.
                if (request.waiting == 0 && asked[name] === request) asked.remove(name)
            }
        }
    }

    /**
     * Works out every thumbnail in [names], in that order, after any row that asks. Only the first call counts: the
     * warm-up runs once for this loader (once per process for the app's own).
     */
    fun warmUp(names: List<String>) {
        synchronized(lock) {
            if (warmUpStarted) return
            warmUpStarted = true
            warmUpNames = names
            startWorker()
        }
        wake.trySend(Unit)
    }

    /** Stops the worker. Only for a loader of its own, such as one for a test's lookup. */
    fun close() = scope.cancel()

    private fun startWorker() {
        if (working) return
        working = true
        scope.launch { work() }
    }

    private suspend fun work() {
        while (true) {
            val name = synchronized(lock) { nextName() }
            if (name == null) {
                wake.receive()
                continue
            }
            val thumbnail = try {
                workOut(name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // A figure that cannot be built fails the row that asked, as it did when rows built their own.
                synchronized(lock) { asked.remove(name) }?.done?.completeExceptionally(e)
                continue
            }
            synchronized(lock) { asked.remove(name) }?.done?.complete(thumbnail)
        }
    }

    // Under lock.
    private fun nextName(): String? {
        asked.keys.firstOrNull()?.let { return it }
        while (warmUpNext < warmUpNames.size) {
            val name = warmUpNames[warmUpNext++]
            if (!ready.containsKey(name)) return name
        }
        return null
    }

    private fun workOut(name: String): Thumbnail {
        ready[name]?.let { return it }
        val thumbnail = Thumbnail(figureFor(name)?.let(shapesFor))
        return ready.putIfAbsent(name, thumbnail) ?: thumbnail
    }
}

/** The app's figure lookup. A thumbnail asked for with any other lookup does not use [CatalogueThumbnails]. */
internal val catalogueFigure: (String) -> ExerciseFigure? = { Library.figure(it) }

/** The catalogue's thumbnails, for the life of the process. */
val CatalogueThumbnails = ThumbnailLoader(catalogueFigure)

/**
 * Where the rows of the Exercises tab, the picker and the plan builder get their thumbnails; tests provide a loader of
 * their own.
 */
val LocalThumbnails = staticCompositionLocalOf { CatalogueThumbnails }
