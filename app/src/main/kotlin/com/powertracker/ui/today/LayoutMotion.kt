package com.powertracker.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.powertracker.ui.theme.LocalMotion
import kotlinx.coroutines.launch

/*
 * Layout motion (stage 3): things slide into place when they are added, removed, opened or moved. Everything here is
 * gated on LocalMotion: with motion off each helper is the plain layout it wraps, with no node added, so tests,
 * screenshots and "Remove animations" see exactly what 1.7 drew. Nothing here blocks input: a tap during a slide goes
 * to whatever is under the finger, except a row that is on its way out, which takes no taps.
 */

/**
 * The stiffness of the move spring: with [Spring.DampingRatioLowBouncy] a card-sized move (300 px, 100 dp at xxhdpi)
 * settles to the pixel in about 270 ms, and a move of 600 dp, more than a full card opens or closes by, in under
 * 350 ms (LayoutMotionTest pins both).
 */
internal const val MOVE_STIFFNESS = 950f

/** Fades: 150 ms. */
internal const val FADE_MS = 150

/** The move spring, low bounce so a heavy card does not wobble. */
internal fun <T> moveSpring(visibilityThreshold: T? = null) = spring(Spring.DampingRatioLowBouncy, MOVE_STIFFNESS, visibilityThreshold)

internal fun <T> fade(): FiniteAnimationSpec<T> = tween(FADE_MS)

/** How a row or a block comes in: it fades in while it opens up, pushing what is below down. */
private val SlideIn = fadeIn(fade()) + expandVertically(moveSpring(IntSize.VisibilityThreshold))

/** How it goes: it fades out while it closes up. */
private val SlideOut = fadeOut(fade()) + shrinkVertically(moveSpring(IntSize.VisibilityThreshold))

/**
 * A lazy item that fades in when added, fades out when removed, and slides when the items around it change (Coach's
 * cards, whose sizes never change on their own). With motion off it is nothing.
 */
@Composable
fun LazyItemScope.slidingItem(): Modifier =
    if (LocalMotion.current) Modifier.animateItem(fadeInSpec = fade(), placementSpec = moveSpring(IntOffset.VisibilityThreshold), fadeOutSpec = fade()) else Modifier

/**
 * A lazy item in a list whose items also open and close by sliding (Today's exercises). It fades in when added and
 * out when removed, and slides to its new place only when the list's items change ([keys]: one added, removed or
 * moved). When an item above it opens or closes, it follows that slide at once: the list's own placement animation
 * would chase it a few frames late and draw the two over each other. With motion off it is nothing.
 */
@Composable
fun LazyItemScope.slidingListItem(keys: List<Any>): Modifier =
    if (LocalMotion.current) Modifier.animateItem(fadeInSpec = fade(), placementSpec = null, fadeOutSpec = fade()).slidingReorder(keys, anyChange = true) else Modifier

/**
 * A block that comes and goes ([visible]) by sliding open and closed. While it is leaving it takes no taps and is not
 * read out. With motion off it is shown or not, as before, with nothing around it.
 */
@Composable
fun ColumnScope.SlidingBlock(visible: Boolean, spacing: Dp = 0.dp, content: @Composable ColumnScope.() -> Unit) {
    if (!LocalMotion.current) {
        if (visible) content()
        return
    }
    AnimatedVisibility(visible, enter = SlideIn, exit = SlideOut) {
        Column(Modifier.fillMaxWidth().leaving(this), verticalArrangement = Arrangement.spacedBy(spacing)) { content() }
    }
}

/**
 * [items] in a column, each sliding in when it is added and out when it is removed; the rest close up. A removed item
 * is drawn as it last was until it has gone, takes no taps and is not read out. Items there at the first draw are
 * simply there. With motion off this is the plain loop, as before.
 *
 * With [reorder], an item that changes place slides there ([slidingReorder]).
 *
 * [content] gets the item's index in [items]; one on its way out gets the index it last had.
 */
@Composable
fun <T> SlidingColumnItems(items: List<T>, key: (T) -> Any, reorder: Boolean = false, content: @Composable (index: Int, item: T) -> Unit) {
    if (!LocalMotion.current) {
        items.forEachIndexed { i, item -> content(i, item) }
        return
    }
    val memory = remember { SlidingMemory<T>() }
    val firstDraw = memory.shown.isEmpty() && !memory.drawn
    val liveKeys = items.map(key)
    val live = liveKeys.toHashSet()

    // What is shown: the live items in their order, with each one on its way out kept after the item it followed.
    val shown = items.toMutableList()
    memory.shown.forEachIndexed { i, old ->
        val k = key(old)
        if (k in live) return@forEachIndexed
        val state = memory.states[k] ?: return@forEachIndexed
        if (state.isIdle && !state.currentState) return@forEachIndexed
        val before = memory.shown.subList(0, i).lastOrNull { prev -> shown.any { key(it) == key(prev) } }
        val at = if (before == null) 0 else shown.indexOfFirst { key(it) == key(before) } + 1
        shown.add(at, old)
    }
    val shownKeys = shown.map(key)
    for (k in shownKeys) {
        if (k !in memory.states) memory.states[k] = MutableTransitionState(firstDraw)
    }
    SideEffect {
        memory.drawn = true
        memory.shown = shown
        memory.states.keys.retainAll(shownKeys.toSet())
        for (k in shownKeys) memory.states.getValue(k).targetState = k in live
        memory.lastIndex.keys.retainAll(shownKeys.toSet())
        liveKeys.forEachIndexed { i, k -> memory.lastIndex[k] = i }
    }

    shown.forEach { item ->
        val k = key(item)
        val index = liveKeys.indexOf(k).takeIf { it >= 0 } ?: memory.lastIndex[k] ?: 0
        key(k) {
            val moves = if (reorder) Modifier.slidingReorder(liveKeys) else Modifier
            AnimatedVisibility(memory.states.getValue(k), moves, enter = SlideIn, exit = SlideOut) {
                Column(Modifier.fillMaxWidth().leaving(this)) { content(index, item) }
            }
        }
    }
}

private class SlidingMemory<T> {
    var drawn = false
    var shown: List<T> = emptyList()
    val states = HashMap<Any, MutableTransitionState<Boolean>>()
    val lastIndex = HashMap<Any, Int>()
}

/** A row on its way out takes no taps and is not read out. */
private fun Modifier.leaving(scope: AnimatedVisibilityScope): Modifier =
    if (scope.transition.targetState != EnterExitState.PostExit) this else inert()

/** Takes no taps and is not read out. */
private fun Modifier.inert(): Modifier {
    return this
        .clearAndSetSemantics {}
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
}

/**
 * Today's exercise as a one-line [row] or the full [card], sliding from one to the other in place. With motion off it
 * is one or the other, as before, and [card] gets null.
 *
 * With motion on, [card] gets the modifier for its inner content (inside the card's surface, under its padding):
 * - **Opening:** the card replaces the row at once at the row's height, and its content is uncovered from the top as
 *   it grows to its own height. The surface and its shadow follow the growing edge.
 * - **Closing:** the card keeps all its content and shrinks to the row's height, its content cut at the shrinking
 *   edge (never the surface or its shadow). The row cross-fades in over it during the last 150 ms, then takes over.
 * - Either way it is one move spring ([moveSpring]); the rows below follow it at once, so no gap opens. Re-opening
 *   a closing card turns it round from where it is.
 *
 * [cardPadding] is the card's padding above plus below its content (PtCard's default, 16 dp each), so the card is the
 * row's height exactly when its content is that much shorter.
 */
@Composable
fun SlidingSwap(
    opened: Boolean,
    row: @Composable (Modifier) -> Unit,
    card: @Composable (content: Modifier?) -> Unit,
    cardPadding: Dp = 32.dp,
) {
    if (!LocalMotion.current) {
        if (opened) card(null) else row(Modifier)
        return
    }
    val swap = remember { SwapState(opened) }
    // swap.closing is read first, so this composition follows it: the card goes when the close finishes, even when
    // nothing else here changes.
    val closing = !opened && (swap.closing || swap.lastOpened)
    // The card's content starts at the row's height when it opens in place of the row.
    if (opened && !swap.lastOpened) swap.openFromRow = true
    SideEffect {
        if (!opened && swap.lastOpened) {
            swap.closing = true
            swap.rowAlpha = 0f
        }
        if (opened) swap.closing = false
        swap.lastOpened = opened
    }
    Box {
        if (!opened) {
            // Measured first, so the closing card knows the height it is going to; drawn on top, fading in.
            val alpha = if (closing) Modifier.graphicsLayer { this.alpha = swap.rowAlpha } else Modifier
            row(Modifier.zIndex(1f).then(alpha).then(RowHeightElement(swap)))
        }
        // A closing card takes no taps and is not read out: the row under it is the exercise now.
        if (opened || closing) card(SwapContentElement(swap, !opened, cardPadding).let { if (closing) Modifier.then(it).inert() else Modifier.then(it) })
    }
}

private class SwapState(opened: Boolean) {
    var lastOpened = opened
    var closing by mutableStateOf(false)
    var openFromRow = false
    var rowHeight = -1
    var rowAlpha by mutableFloatStateOf(0f)
}

/** Records the row's height for the card closing over it. */
private class RowHeightElement(val swap: SwapState) : ModifierNodeElement<RowHeightNode>() {
    override fun create() = RowHeightNode(swap)
    override fun update(node: RowHeightNode) {
        node.swap = swap
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "rowHeight"
    }
    override fun equals(other: Any?) = other is RowHeightElement && other.swap === swap
    override fun hashCode() = System.identityHashCode(swap)
}

private class RowHeightNode(var swap: SwapState) : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        swap.rowHeight = placeable.height
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}

private class SwapContentElement(val swap: SwapState, val closing: Boolean, val cardPadding: Dp) : ModifierNodeElement<SwapContentNode>() {
    override fun create() = SwapContentNode(swap, closing, cardPadding)
    override fun update(node: SwapContentNode) {
        node.swap = swap
        node.closing = closing
        node.cardPadding = cardPadding
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "swapContent"
        properties["closing"] = closing
    }
    override fun equals(other: Any?) = other is SwapContentElement && other.swap === swap && other.closing == closing && other.cardPadding == cardPadding
    override fun hashCode() = (System.identityHashCode(swap) * 31 + closing.hashCode()) * 31 + cardPadding.hashCode()
}

/**
 * The card's content, measured at its own height but taking the sliding height, and cut there. Only an open or a
 * close slides; any other change of the content's height (a set row sliding in) is followed at once.
 */
private class SwapContentNode(var swap: SwapState, var closing: Boolean, var cardPadding: Dp) : Modifier.Node(), LayoutModifierNode, DrawModifierNode {
    private var height: Animatable<Int, *>? = null
    private var wasClosing = closing
    private var natural = 0
    private var shown = 0

    override fun onDetach() {
        height = null
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        natural = placeable.height
        val rowContent = if (swap.rowHeight >= 0) (swap.rowHeight - cardPadding.roundToPx()).coerceAtLeast(0) else 0
        val target = if (closing) rowContent else natural
        val toggled = closing != wasClosing
        wasClosing = closing
        var anim = height
        if (anim == null) {
            val start = if (swap.openFromRow && swap.rowHeight >= 0) rowContent else target
            swap.openFromRow = false
            anim = Animatable(start, Int.VectorConverter, 1).also { height = it }
        }
        var slides = false
        if (target != anim.targetValue || (anim.value != target && !anim.isRunning)) {
            if (toggled || anim.isRunning || anim.value != target) {
                slides = true
                val from = anim.value
                val closingNow = closing
                coroutineScope.launch {
                    if (closingNow) launch { fadeInRow(from, target) }
                    anim.animateTo(target, moveSpring(1))
                    if (closingNow && closing) swap.closing = false
                }
            } else {
                coroutineScope.launch { anim.snapTo(target) }
            }
        }
        // A close with nothing to slide (the content is already the row's height, or the slide toward it is still on its
        // way): finish the swap now, or the card would stay over the row, inert, with the row unseen.
        if (closing && toggled && !slides && !anim.isRunning) {
            coroutineScope.launch {
                swap.rowAlpha = 1f
                if (closing) swap.closing = false
            }
        }
        val sliding = toggled || anim.isRunning || anim.value != target
        val h = constraints.constrainHeight(if (sliding) anim.value else target)
        if (shown != h) {
            shown = h
            invalidateDraw()
        }
        return layout(placeable.width, h) { placeable.place(0, 0) }
    }

    /** The row fades in over the closing card in the last [FADE_MS] of its slide. */
    private suspend fun fadeInRow(from: Int, to: Int) {
        val ms = moveSpring(1f).vectorize(Float.VectorConverter)
            .getDurationNanos(AnimationVector1D(from.toFloat()), AnimationVector1D(to.toFloat()), AnimationVector1D(0f)) / 1_000_000
        swap.rowAlpha = 0f
        animate(0f, 1f, animationSpec = tween(FADE_MS, delayMillis = (ms - FADE_MS).toInt().coerceAtLeast(0))) { value, _ -> swap.rowAlpha = value }
    }

    override fun ContentDrawScope.draw() {
        if (shown >= natural) {
            drawContent()
            return
        }
        clipRect(0f, 0f, size.width, shown.toFloat()) { this@draw.drawContent() }
    }
}

/**
 * A row that slides to its new place when the rows are put in a new order (Plan's Move up and Move down), so two rows
 * pass each other. [order] is the order of every row's key; only a change of order slides (or with [anyChange], any
 * change of the keys: one added or removed), so a row that shifts because another opened or closed (which already
 * slides) follows it at once. With motion off it adds nothing.
 */
@Composable
fun Modifier.slidingReorder(order: List<Any>, anyChange: Boolean = false): Modifier {
    if (!LocalMotion.current) return this
    // A new generation each time the same rows come in a different order.
    val memory = remember { ReorderMemory() }
    if (order != memory.order) {
        if (memory.order.isNotEmpty() && (anyChange || order.toSet() == memory.order.toSet())) memory.generation++
        memory.order = order
    }
    return this then SlidingReorderElement(memory.generation)
}

private class ReorderMemory {
    var order: List<Any> = emptyList()
    var generation = 0
}

private class SlidingReorderElement(val generation: Int) : ModifierNodeElement<SlidingReorderNode>() {
    override fun create() = SlidingReorderNode(generation)

    override fun update(node: SlidingReorderNode) {
        node.generation = generation
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "slidingReorder"
        properties["generation"] = generation
    }

    override fun equals(other: Any?) = other is SlidingReorderElement && other.generation == generation
    override fun hashCode() = generation
}

private class SlidingReorderNode(var generation: Int) : Modifier.Node(), LayoutModifierNode {
    private var offset = restingOffset()
    private var lastPosition: IntOffset? = null
    private var seenGeneration = generation

    private fun restingOffset() = Animatable(IntOffset.Zero, IntOffset.VectorConverter, IntOffset.VisibilityThreshold)

    override fun onDetach() {
        lastPosition = null
        // A slide cut short here is over. A lazy list lets go of a row whose new place is off the screen while it is
        // still sliding there, and uses this node again when the row comes back: it must then be in its place, not
        // short of it by what was left of the slide (Today's "Add exercise", drawn over the card just added).
        offset = restingOffset()
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val position = coordinates?.positionInParent()?.round()
            val last = lastPosition
            lastPosition = position
            val offset = offset
            var from = offset.value
            if (generation != seenGeneration) {
                seenGeneration = generation
                if (position != null && last != null && position != last) {
                    // Start from where it was drawn, and slide to its new place.
                    from = offset.value + (last - position)
                    val start = from
                    coroutineScope.launch {
                        offset.snapTo(start)
                        offset.animateTo(IntOffset.Zero, moveSpring(IntOffset.VisibilityThreshold))
                    }
                }
            }
            placeable.place(from)
        }
    }
}
