package com.powertracker.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import com.powertracker.ui.theme.LocalMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** How far a pressed control sinks (owner decision 1). */
const val PRESS_SCALE = 0.97f

/** The tab bar's icons are small, so they sink further. */
const val TAB_PRESS_SCALE = 0.92f

/** The sink, about 90 ms. */
internal const val PRESS_DOWN_MS = 90

/**
 * The spring back on release, with a small overshoot: settles in about 260 ms. The travel is only 0.03, so the
 * spring's default stopping point (0.01) would end it early and flat; this one lets the overshoot play out.
 */
internal val PressRelease = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium, visibilityThreshold = 0.0002f)

/**
 * Press feedback: the control sinks to [pressedScale] while pressed and springs back on release. Pass the same
 * [interactionSource] the control's click uses, so a press that turns into a scroll (the click's Cancel) springs back
 * as well, and fires nothing. It is a layer scale only: nothing around it moves.
 *
 * With motion off ([LocalMotion]) or [enabled] false it adds nothing at all.
 */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, enabled: Boolean = true, pressedScale: Float = PRESS_SCALE): Modifier =
    if (!LocalMotion.current || !enabled) this else this then PressScaleElement(interactionSource, pressedScale)

private class PressScaleElement(val source: InteractionSource, val pressedScale: Float) : ModifierNodeElement<PressScaleNode>() {
    override fun create() = PressScaleNode(source, pressedScale)

    override fun update(node: PressScaleNode) {
        node.pressedScale = pressedScale
        if (node.source != source) {
            node.source = source
            node.listen()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "pressScale"
        properties["pressedScale"] = pressedScale
    }

    override fun equals(other: Any?) = other is PressScaleElement && other.source == source && other.pressedScale == pressedScale
    override fun hashCode() = 31 * source.hashCode() + pressedScale.hashCode()
}

private class PressScaleNode(var source: InteractionSource, var pressedScale: Float) : Modifier.Node(), LayoutModifierNode {
    // One float per control; the animation itself only runs while a press is on.
    private val scale = mutableFloatStateOf(1f)
    private var velocity = 0f
    private var listening: Job? = null
    private var sinking: Job? = null
    private var springing: Job? = null
    private val layer: GraphicsLayerScope.() -> Unit = {
        val s = scale.floatValue
        scaleX = s
        scaleY = s
    }

    override fun onAttach() = listen()

    override fun onDetach() {
        listening = null
        sinking = null
        springing = null
        scale.floatValue = 1f
        velocity = 0f
    }

    fun listen() {
        if (!isAttached) return
        listening?.cancel()
        listening = coroutineScope.launch {
            val presses = mutableListOf<PressInteraction.Press>()
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        presses += interaction
                        sink()
                    }
                    // A tap: the sink plays out first, so even a quick one shows (in a scrolling screen the press is
                    // only reported with its release).
                    is PressInteraction.Release -> {
                        presses -= interaction.press
                        if (presses.isEmpty()) springBack(afterSink = true)
                    }
                    // Not a tap after all (it became a scroll): straight back.
                    is PressInteraction.Cancel -> {
                        presses -= interaction.press
                        if (presses.isEmpty()) springBack(afterSink = false)
                    }
                }
            }
        }
    }

    private fun sink() {
        springing?.cancel()
        sinking?.cancel()
        sinking = coroutineScope.launch { glide(pressedScale, tween(PRESS_DOWN_MS, easing = FastOutSlowInEasing)) }
    }

    private fun springBack(afterSink: Boolean) {
        val sink = sinking
        if (!afterSink) sink?.cancel()
        springing?.cancel()
        if (sink?.isActive != true && scale.floatValue == 1f) return
        springing = coroutineScope.launch {
            sink?.join()
            glide(1f, PressRelease)
        }
    }

    /** From where it is and how fast it is going, to [target]. */
    private suspend fun glide(target: Float, spec: AnimationSpec<Float>) {
        animate(scale.floatValue, target, velocity, spec) { value, speed ->
            scale.floatValue = value
            velocity = speed
        }
        velocity = 0f
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.placeWithLayer(0, 0, layerBlock = layer) }
    }
}
