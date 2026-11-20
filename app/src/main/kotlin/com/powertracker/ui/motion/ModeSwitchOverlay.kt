package com.powertracker.ui.motion

import android.graphics.Picture
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.powertracker.domain.motion.DropFrame
import com.powertracker.domain.motion.ModeSwitchMotion
import com.powertracker.domain.motion.MotionGradient
import com.powertracker.domain.motion.MotionPath
import com.powertracker.domain.motion.Point
import com.powertracker.domain.motion.PumpFrame
import com.powertracker.domain.motion.VeinNode
import com.powertracker.domain.motion.VeinPath
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * The mode switch animations over the whole app (1.15): [content] (the navigation host) with the overlay above it.
 * Without a [LocalModeSwitch] controller it is just [content].
 *
 * While an animation runs:
 * - one frame clock ([withFrameMillis]) drives the controller from the tap, as the sound and the vibration run from it:
 *   the first frame catches up to the time since the tap, and a late frame jumps ahead rather than falling behind them;
 * - the content shakes (the drop) or swells (the pump) as one layer;
 * - from just before the moment, the old side is kept as a recorded picture and drawn outside the reveal circle, so
 *   the new side's real screen opens inside it (spec "Settled" 7);
 * - the overlay draws the effects. It is hidden from accessibility and takes no input: a tap anywhere but the switch's
 *   row skips, and does nothing else (the whole gesture is taken before the screen sees it);
 * - the app going to the background ends it.
 */
@Composable
fun ModeSwitchHost(content: @Composable () -> Unit) {
    val controller = LocalModeSwitch.current
    if (controller == null) {
        content()
        return
    }
    val density = LocalDensity.current.density
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, controller) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) controller.finish(ModeSwitchEnd.HIDDEN) }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            controller.finish(ModeSwitchEnd.LEAVE)
        }
    }
    val run = controller.run
    val kind = controller.kind
    val art = remember { ArtHolder() }
    LaunchedEffect(run) {
        val k = controller.kind ?: return@LaunchedEffect
        val g = controller.geometry ?: return@LaunchedEffect
        val src = controller.pumpSource()
        controller.artBuilds++
        art.value = null
        art.value = withContext(Dispatchers.Default) {
            if (k == ModeSwitchKind.DROP) buildDropArt(g.widthDp, g.heightDp) else buildPumpArt(g.widthDp, g.heightDp, src)
        }
        if (controller.run == run && controller.kind != null) controller.artReady = true
    }
    LaunchedEffect(run) {
        if (controller.kind == null) return@LaunchedEffect
        // The first frame: the picture catches up to the time since the tap, as the sound and the vibration run from it.
        var last = withFrameMillis { now ->
            if (controller.run == run) controller.catchUp()
            now
        }
        while (controller.kind != null && controller.run == run) {
            withFrameMillis { now ->
                if (controller.run == run) controller.advanceBy((now - last).coerceAtLeast(0L).toDouble())
                last = now
            }
        }
    }
    val reveal = remember { RevealHolder() }
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { controller.geometry = ModeSwitchGeometry(it.positionInRoot(), it.size.toSize(), density) }
            .pointerInput(controller) { skipTaps(controller) },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin.Center
                    when (controller.kind) {
                        ModeSwitchKind.DROP -> controller.dropFrame()?.let { f ->
                            translationX = (f.shakeX * density).toFloat()
                            translationY = (f.shakeY * density).toFloat()
                            rotationZ = f.shakeRotation.toFloat()
                            scaleX = f.shakeScale.toFloat()
                            scaleY = f.shakeScale.toFloat()
                        }
                        ModeSwitchKind.PUMP -> controller.pumpFrame()?.let { f ->
                            scaleX = f.contentScale.toFloat()
                            scaleY = f.contentScale.toFloat()
                        }
                        null -> Unit
                    }
                }
                .drawWithContent {
                    val probe = ModeSwitchProbe.onReveal
                    val start = if (probe != null) System.nanoTime() else 0L
                    drawRevealed(controller, reveal, density) { drawContent() }
                    probe?.invoke(System.nanoTime() - start)
                },
        ) {
            Box(Modifier.fillMaxSize().graphicsLayer()) { content() }
        }
        if (kind != null) {
            val light = MaterialTheme.colorScheme.background.luminance() > .5f
            Canvas(
                Modifier
                    .fillMaxSize()
                    .testTag("mode-switch-overlay")
                    .semantics { hideFromAccessibility() },
            ) {
                val probe = ModeSwitchProbe.onDraw
                val start = if (probe != null) System.nanoTime() else 0L
                scale(density, density, pivot = Offset.Zero) {
                    when (controller.kind) {
                        ModeSwitchKind.DROP -> controller.dropFrame()?.let { drawDrop(it, art.value as? DropArt, light) }
                        ModeSwitchKind.PUMP -> controller.pumpFrame()?.let { drawPump(it, art.value as? PumpArt, light) }
                        null -> Unit
                    }
                }
                probe?.invoke(System.nanoTime() - start)
            }
        }
    }
}

/**
 * How long before the moment the old side is recorded: by then the screen under the hammer or the beats is still. A
 * frame that jumps over this window gives a plain swap at the moment.
 */
private const val CAPTURE_LEAD_MS = 60.0

private class ArtHolder {
    @Volatile var value: Any? = null
}

private class RevealHolder {
    var picture: Picture? = null
    var forRun = -1
}

/** A tap while an animation runs: inside the switch's row it is a flip; anywhere else it skips, and the whole gesture is taken. */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.skipTaps(controller: ModeSwitchController) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    if (controller.kind == null) return@awaitEachGesture
    val origin = controller.geometry?.origin ?: Offset.Zero
    if (controller.switchRow?.contains(down.position + origin) == true) return@awaitEachGesture
    controller.skip()
    down.consume()
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        event.changes.forEach { it.consume() }
        if (event.changes.none { it.pressed }) break
    }
}

/** The content, with the old side's picture outside the reveal circle once the moment has passed. */
private inline fun androidx.compose.ui.graphics.drawscope.ContentDrawScope.drawRevealed(
    controller: ModeSwitchController,
    reveal: RevealHolder,
    density: Float,
    drawContent: () -> Unit,
) {
    val k = controller.kind
    if (k == null) {
        reveal.picture = null
        drawContent()
        return
    }
    val run = controller.run
    val t = controller.tMs
    if (t < k.moment) {
        if (reveal.forRun != run && t >= k.moment - CAPTURE_LEAD_MS) {
            val picture = Picture()
            val canvas = androidx.compose.ui.graphics.Canvas(picture.beginRecording(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1)))
            draw(this, layoutDirection, canvas, size) { drawContent() }
            picture.endRecording()
            reveal.picture = picture
            reveal.forRun = run
        }
        drawContent()
        return
    }
    val centre: Offset
    val radius: Double
    val full: Boolean
    if (k == ModeSwitchKind.DROP) {
        val f = controller.dropFrame()
        centre = Offset(((f?.hitX ?: 0.0) * density).toFloat(), ((f?.hitY ?: 0.0) * density).toFloat())
        radius = f?.revealRadius ?: 0.0
        full = f?.revealFull ?: true
    } else {
        val f = controller.pumpFrame()
        centre = Offset(((f?.srcX ?: 0.0) * density).toFloat(), ((f?.srcY ?: 0.0) * density).toFloat())
        radius = f?.floodRadius ?: 0.0
        full = f?.floodFull ?: true
    }
    drawContent()
    val picture = reveal.picture.takeIf { reveal.forRun == run }
    if (full) {
        reveal.picture = null
    } else if (picture != null && !ModeSwitchProbe.revealOff.value) {
        val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(centre, (radius * density).toFloat())) }
        clipPath(circle, ClipOp.Difference) { drawIntoCanvas { it.nativeCanvas.drawPicture(picture) } }
        ModeSwitchProbe.reveals++
    }
}

// ---- the stamp ----

/**
 * The POWERLIFTING stamp, in [com.powertracker.ui.components.ModeLabel]'s style, just below the screen's title (so it
 * never covers it): shown on the powerlifting side from the drop's impact, fading out by its end, scaled from its top
 * edge so it grows away from the title. It says nothing to a screen reader; the switch says the state.
 */
@Composable
fun ModeSwitchStamp(modifier: Modifier = Modifier) {
    val controller = LocalModeSwitch.current ?: return
    val showing by remember(controller) {
        derivedStateOf { controller.kind == ModeSwitchKind.DROP && controller.tMs >= ModeSwitchMotion.DROP.impact }
    }
    if (!showing) return
    val pt = PowertrackerTheme.colors
    Box(
        modifier
            .testTag("mode-stamp")
            .clearAndSetSemantics { }
            .graphicsLayer {
                val f = controller.dropFrame()
                alpha = (f?.stampOpacity ?: 0.0).toFloat()
                val s = (f?.stampScale ?: 1.0).toFloat()
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(0f, 0f)
            },
    ) {
        Text(
            "POWERLIFTING",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
            color = pt.onAccentContainer,
            maxLines = 1,
            modifier = Modifier.background(pt.accentContainer, CircleShape).padding(horizontal = Space.s, vertical = 2.dp),
        )
    }
}

// ---- rows and cards ----

private class CentreHolder {
    var y = Float.NaN
}

/**
 * A row, card or header that moves with the animation: it hops as the drop's shock passes under it, and swells with
 * the pump, nearest the switch first. Nothing without a controller.
 */
fun Modifier.modeSwitchElement(): Modifier = composed {
    val controller = LocalModeSwitch.current ?: return@composed this
    val centre = remember { CentreHolder() }
    this
        .onGloballyPositioned { if (controller.kind == null) centre.y = it.positionInRoot().y + it.size.height / 2f }
        .graphicsLayer {
            val k = controller.kind ?: return@graphicsLayer
            val g = controller.geometry ?: return@graphicsLayer
            if (centre.y.isNaN()) return@graphicsLayer
            val yDp = (centre.y - g.origin.y) / g.density.toDouble()
            when (k) {
                ModeSwitchKind.DROP -> {
                    val hitY = ModeSwitchMotion.hitPoint(g.widthDp, g.heightDp).y
                    translationY = (ModeSwitchMotion.hop(controller.tMs, yDp, hitY) * g.density).toFloat()
                }
                ModeSwitchKind.PUMP -> {
                    val s = ModeSwitchMotion.pumpElementScale(controller.tMs, g.widthDp, yDp, controller.pumpSource()).toFloat()
                    scaleX = s
                    scaleY = s
                }
            }
        }
}
