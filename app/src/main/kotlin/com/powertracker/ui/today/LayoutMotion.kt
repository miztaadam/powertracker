package com.powertracker.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
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
 * settles to the pixel in about 280 ms, and a move of 400 dp, more than a card ever opens by, in under 350 ms
 * (LayoutMotionTest pins both).
 */
internal const val MOVE_STIFFNESS = 850f

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
private fun Modifier.leaving(scope: AnimatedVisibilityScope): Modifier {
    if (scope.transition.targetState != EnterExitState.PostExit) return this
    return this
        .clearAndSetSemantics {}
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
}

/**
 * An item whose height changes when it opens or closes (Today's exercise: a one-line row, or the full card). When
 * [opened] changes, the height slides from the old one to the new one; any other change of height (a set row sliding
 * in inside the card) is followed at once, since that change already slides. While it slides the content is cut at
 * the sliding edge, but never at the sides or the top, so the card's shadow stays; at rest nothing is cut.
 * With motion off it adds nothing.
 */
@Composable
fun Modifier.slidingOpen(opened: Boolean, shadowRoom: Dp = 32.dp, gapBelow: Dp = 12.dp): Modifier =
    if (!LocalMotion.current) this else this then SlidingOpenElement(opened, shadowRoom, gapBelow)

private class SlidingOpenElement(val opened: Boolean, val shadowRoom: Dp, val gapBelow: Dp) : ModifierNodeElement<SlidingOpenNode>() {
    override fun create() = SlidingOpenNode(opened, shadowRoom, gapBelow)

    override fun update(node: SlidingOpenNode) {
        node.opened = opened
        node.shadowRoom = shadowRoom
        node.gapBelow = gapBelow
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "slidingOpen"
        properties["opened"] = opened
    }

    override fun equals(other: Any?) = other is SlidingOpenElement && other.opened == opened && other.shadowRoom == shadowRoom && other.gapBelow == gapBelow
    override fun hashCode() = (opened.hashCode() * 31 + shadowRoom.hashCode()) * 31 + gapBelow.hashCode()
}

private class SlidingOpenNode(var opened: Boolean, var shadowRoom: Dp, var gapBelow: Dp) : Modifier.Node(), LayoutModifierNode, DrawModifierNode {
    private var height: Animatable<Int, *>? = null
    private var openedAtLastMeasure = opened

    // The content's own height, and the height the box takes this frame; the content is cut only while it is taller.
    private var contentHeight = 0
    private var boxHeight = 0

    override fun onDetach() {
        height = null
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        val target = placeable.height
        val anim = height ?: Animatable(target, Int.VectorConverter, 1).also { height = it }
        val toggled = opened != openedAtLastMeasure
        openedAtLastMeasure = opened
        if (target != anim.targetValue) {
            if (toggled || anim.isRunning) {
                coroutineScope.launch { anim.animateTo(target, moveSpring(1)) }
            } else {
                coroutineScope.launch { anim.snapTo(target) }
            }
        }
        // A change that is not an open or a close shows at once, not a frame late.
        val shown = if (toggled || anim.isRunning) anim.value else target
        val h = constraints.constrainHeight(shown)
        if (contentHeight != target || boxHeight != h) {
            contentHeight = target
            boxHeight = h
            invalidateDraw()
        }
        return layout(placeable.width, h) { placeable.place(0, 0) }
    }

    override fun ContentDrawScope.draw() {
        if (boxHeight >= contentHeight) {
            drawContent()
            return
        }
        val side = shadowRoom.toPx()
        // The draw size here is the content's, not the box's: cut at the box.
        clipRect(-side, -side, size.width + side, boxHeight + gapBelow.toPx()) { this@draw.drawContent() }
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
    private val offset = Animatable(IntOffset.Zero, IntOffset.VectorConverter, IntOffset.VisibilityThreshold)
    private var lastPosition: IntOffset? = null
    private var seenGeneration = generation

    override fun onDetach() {
        lastPosition = null
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val position = coordinates?.positionInParent()?.round()
            val last = lastPosition
            lastPosition = position
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
