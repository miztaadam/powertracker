package com.powertracker.ui.today

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/*
 * One-off motion for an event on Today (1.16, the owner's extras): a set's tick draws itself in when the set is logged,
 * and the "Skipped today" line rises in when an exercise is skipped. Each plays on the event only, never when the screen
 * opens with it there, and never with motion off: the callers decide that (they ask only for an event, under
 * LocalMotion). Their time is the frame clock, or the guide's own ([LocalEventClock]) when the guide draws the screen.
 */

/** Each effect's time, in ms: the guide's step move (Guide.MOVE_MS), which the owner's sketch maps onto. */
internal const val EVENT_MOVE_MS = 280

/** The skipped line fades in (and rises) over the last of [EVENT_MOVE_MS], once the rows around it have made its room. */
internal const val EVENT_FADE_MS = FADE_MS

/**
 * The guide's time for the screen it draws: [nowMs] into the picture, the step shown having started at [stepStartMs].
 * An effect started in a step counts from that step's start, so a held clock holds it where it is and a replay replays it.
 */
@Immutable
data class EventClock(val nowMs: Int, val stepStartMs: Int)

/** Null in the app: effects run on the frame clock. The guide provides its own time. */
val LocalEventClock = compositionLocalOf<EventClock?> { null }

/** The skipped line's rise in progress, for tests: read it for how far (0 to 1) through its time it is. */
val RiseIn = SemanticsPropertyKey<() -> Float>("RiseIn")
internal var SemanticsPropertyReceiver.riseIn by RiseIn

/**
 * An effect of [durationMs] for an event that has just happened, started when this is first composed: how far through
 * it is (0 to 1), read when drawn; null once it is over. [onOver] is called when it ends.
 */
@Composable
internal fun eventProgress(durationMs: Int, onOver: () -> Unit = {}): (() -> Float)? {
    val over by rememberUpdatedState(onOver)
    val clock = LocalEventClock.current
    if (clock != null) {
        val start = remember { clock.stepStartMs }
        val p = ((clock.nowMs - start).toFloat() / durationMs).coerceIn(0f, 1f)
        if (p >= 1f) {
            SideEffect { over() }
            return null
        }
        return { p }
    }
    val progress = remember { Animatable(0f) }
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMs, easing = LinearEasing))
        done = true
        over()
    }
    return if (done) null else ({ progress.value })
}

/** How far into its own fade the skipped line is when its effect is [p] (0 to 1) through [EVENT_MOVE_MS]. */
internal fun riseFade(p: Float): Float =
    ((p * EVENT_MOVE_MS - (EVENT_MOVE_MS - EVENT_FADE_MS)) / EVENT_FADE_MS).coerceIn(0f, 1f)

/*
 * Where a control is, for the guide's tap ring (1.16): the guide's picture is out of the accessibility tree and nothing
 * in production can look a node up by its tag, so the control says where it is, to a guide that asks. In the app no
 * one asks, and the marker is no modifier at all: it draws nothing and changes nothing.
 */

/** The controls the guide can ring. */
enum class TapAnchor { LOG_SET }

/** Where each [TapAnchor] is, in the space of the layout that holds it ([root]): the guide's. */
class TapAnchors {
    private var root: LayoutCoordinates? = null
    private val placed = HashMap<TapAnchor, LayoutCoordinates>()

    /** Each anchor's box in [root]'s space, kept up to date as either moves. */
    val boxes = mutableStateMapOf<TapAnchor, Rect>()

    fun placeRoot(coordinates: LayoutCoordinates) {
        root = coordinates
        placed.keys.toList().forEach(::measure)
    }

    internal fun place(anchor: TapAnchor, coordinates: LayoutCoordinates) {
        placed[anchor] = coordinates
        measure(anchor)
    }

    private fun measure(anchor: TapAnchor) {
        val r = root ?: return
        val c = placed[anchor] ?: return
        if (!r.isAttached || !c.isAttached) return
        val box = r.localBoundingBoxOf(c, clipBounds = false)
        if (boxes[anchor] != box) boxes[anchor] = box
    }
}

/** Null in the app; the guide provides one to find its controls. */
val LocalTapAnchors = staticCompositionLocalOf<TapAnchors?> { null }

/** Says where this control is to a guide that asks ([LocalTapAnchors]); in the app it is nothing. */
@Composable
internal fun Modifier.tapAnchor(anchor: TapAnchor): Modifier {
    val anchors = LocalTapAnchors.current ?: return this
    return this.onGloballyPositioned { anchors.place(anchor, it) }
}
