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
 * - one frame clock ([withFrameMillis]) drives the controller;
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
        art.value = null
        art.value = withContext(Dispatchers.Default) {
            if (k == ModeSwitchKind.DROP) buildDropArt(g.widthDp, g.heightDp) else buildPumpArt(g.widthDp, g.heightDp, src)
        }
        if (controller.run == run && controller.kind != null) controller.artReady = true
    }
    LaunchedEffect(run) {
        if (controller.kind == null) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (controller.kind != null && controller.run == run) {
            withFrameMillis { now ->
                if (controller.run == run) controller.advanceBy(min(MAX_STEP_MS, now - last).toDouble())
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
                .drawWithContent { drawRevealed(controller, reveal, density) { drawContent() } },
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

/** The longest step the clock takes in one frame, so a stalled frame never jumps the animation ahead. */
private const val MAX_STEP_MS = 50L

/** How long before the moment the old side is recorded: by then the screen under the hammer or the beats is still. */
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
    } else if (picture != null) {
        val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(centre, (radius * density).toFloat())) }
        clipPath(circle, ClipOp.Difference) { drawIntoCanvas { it.nativeCanvas.drawPicture(picture) } }
    }
}

// ---- the stamp ----

/**
 * The POWERLIFTING stamp, in [com.powertracker.ui.components.ModeLabel]'s style and place: shown on the powerlifting
 * side from the drop's impact, fading out by its end. It says nothing to a screen reader; the switch says the state.
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
                transformOrigin = TransformOrigin(0f, .5f)
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

// ---- the shapes, built off the main thread once per run ----

private fun hex(c: String): Color = Color(android.graphics.Color.parseColor(c))

private val GRADIENT_BY_ID: Map<String, MotionGradient> by lazy { ModeSwitchMotion.GRADIENTS.associateBy { it.id } }

private fun parse(d: String): Path = PathParser().parsePathString(d).toPath()

/** A fill or stroke: a colour, or a gradient over the shape's bounding box, as SVG's objectBoundingBox. */
private fun paintOf(spec: String, path: Path): Brush {
    if (!spec.startsWith("url(#")) return androidx.compose.ui.graphics.SolidColor(hex(spec))
    val g = GRADIENT_BY_ID.getValue(spec.removePrefix("url(#").removeSuffix(")"))
    val b = path.getBounds()
    val stops = g.stops.map { it.offset.toFloat() to hex(it.color).copy(alpha = it.opacity.toFloat()) }.toTypedArray()
    return if (g.radial) {
        Brush.radialGradient(*stops, center = b.center, radius = maxOf(b.width, b.height) / 2f)
    } else {
        Brush.linearGradient(
            *stops,
            start = Offset(b.left + g.x1.toFloat() * b.width, b.top + g.y1.toFloat() * b.height),
            end = Offset(b.left + g.x2.toFloat() * b.width, b.top + g.y2.toFloat() * b.height),
        )
    }
}

private fun cap(name: String) = when (name) {
    "round" -> StrokeCap.Round
    "square" -> StrokeCap.Square
    else -> StrokeCap.Butt
}

private fun join(name: String) = when (name) {
    "round" -> StrokeJoin.Round
    "bevel" -> StrokeJoin.Bevel
    else -> StrokeJoin.Miter
}

private class Shape(val path: Path, val fill: Brush?, val stroke: Brush?, val style: Stroke?, val alpha: Float)

private fun shapeOf(p: MotionPath): Shape {
    val path = parse(p.d)
    return Shape(
        path,
        p.fill?.let { paintOf(it, path) },
        p.stroke?.let { paintOf(it, path) },
        if (p.stroke != null) Stroke(p.strokeWidth.toFloat(), cap = cap(p.lineCap), join = join(p.lineJoin)) else null,
        p.opacity.toFloat(),
    )
}

/** A stroke drawn from its start to a fraction of its length. */
private class Partial(val measure: PathMeasure, val length: Float, val color: Color, val width: Float, val alpha: Float) {
    val segment = Path()

    fun cut(from: Float, to: Float): Path {
        segment.rewind()
        measure.getSegment(from * length, to * length, segment, true)
        return segment
    }
}

private fun partialOf(d: String, color: String, width: Double, alpha: Double): Partial {
    val m = PathMeasure()
    m.setPath(parse(d), false)
    return Partial(m, m.length, hex(color), width.toFloat(), alpha.toFloat())
}

private class HammerArt(val shapes: List<Shape>, val rune: Shape)

private val HAMMER_ART: HammerArt by lazy { HammerArt(ModeSwitchMotion.HAMMER_PATHS.map(::shapeOf), shapeOf(ModeSwitchMotion.HAMMER_RUNE)) }

private class DropArt(val hammer: HammerArt, val cracks: List<Partial>, val burst: Path, val chalk: Array<Pair<Float, Color>>)

private fun buildDropArt(w: Double, h: Double): DropArt {
    ModeSwitchProbe.artBuilds++
    val hit = ModeSwitchMotion.hitPoint(w, h)
    val cracks = ModeSwitchMotion.cracks(w, h, hit).map { partialOf(it.d, it.stroke ?: "#000000", it.strokeWidth, it.opacity) }
    val burst = Path().apply {
        ModeSwitchMotion.burst(hit).forEachIndexed { i, p -> if (i == 0) moveTo(p.x.toFloat(), p.y.toFloat()) else lineTo(p.x.toFloat(), p.y.toFloat()) }
        close()
    }
    val chalk = GRADIENT_BY_ID.getValue("gChalk").stops.map { it.offset.toFloat() to hex(it.color).copy(alpha = it.opacity.toFloat()) }.toTypedArray()
    return DropArt(HAMMER_ART, cracks, burst, chalk)
}

private class Vein(val partial: Partial, val layer: String, val swell: Boolean, val d0: Double, val len: Double)

private class PumpArt(val veins: List<Vein>, val nodes: List<VeinNode>, val halo: Array<Pair<Float, Color>>)

private fun buildPumpArt(w: Double, h: Double, src: Point): PumpArt {
    ModeSwitchProbe.artBuilds++
    val veins = ModeSwitchMotion.veins(w, h, src).map { v: VeinPath -> Vein(partialOf(v.d, v.color, v.width, 1.0), v.layer, v.swell, v.d0, v.len) }
    val halo = GRADIENT_BY_ID.getValue("gHalo").stops.map { it.offset.toFloat() to hex(it.color).copy(alpha = it.opacity.toFloat()) }.toTypedArray()
    return PumpArt(veins, ModeSwitchMotion.veinNodes(w, h, src), halo)
}

// ---- drawing, in dp ----

private val SHADOW = hex(ModeSwitchMotion.Colors.SHADOW)
private val FLASH = hex(ModeSwitchMotion.Colors.FLASH)
private val FLASH_LIGHT = hex(ModeSwitchMotion.Colors.FLASH_LIGHT)
private val RING1 = hex(ModeSwitchMotion.Colors.RING1)
private val RING2 = hex(ModeSwitchMotion.Colors.RING2)
private val BURST = hex(ModeSwitchMotion.Colors.BURST)
private val SPARK = hex(ModeSwitchMotion.Colors.SPARK)
private val TINT = hex(ModeSwitchMotion.Colors.TINT)
private val PUMP_RING = hex(ModeSwitchMotion.Colors.PUMP_RING)
private val EDGE = hex(ModeSwitchMotion.Colors.EDGE)
private val EDGE_SOFT = hex(ModeSwitchMotion.Colors.EDGE_SOFT)
private val NODE = hex(ModeSwitchMotion.Colors.NODE)

/** The darker edge the light scheme gives the cracks, the chalk and the veins (spec "Settled" 1). */
private val LIGHT_EDGE = Color(0xFF2A2E33)
private val LIGHT_VEIN_EDGE = Color(0xFF123063)

private val layerPaint = Paint()

/** Draws [block] as one group at [alpha], as SVG's group opacity does. */
private inline fun DrawScope.group(alpha: Float, block: DrawScope.() -> Unit) {
    if (alpha <= 0f) return
    if (alpha >= 1f) {
        block()
        return
    }
    drawIntoCanvas { canvas ->
        layerPaint.alpha = alpha
        canvas.saveLayer(androidx.compose.ui.geometry.Rect(Offset.Zero, size), layerPaint)
        block()
        canvas.restore()
    }
}

private fun DrawScope.drawHammer(art: HammerArt, pose: com.powertracker.domain.motion.Pose, runeAlpha: Float) {
    translate(pose.x.toFloat(), pose.y.toFloat()) {
        rotate(pose.rotation.toFloat(), pivot = Offset.Zero) {
            scale(pose.scaleX.toFloat(), pose.scaleY.toFloat(), pivot = Offset.Zero) {
                for (s in art.shapes) {
                    s.fill?.let { drawPath(s.path, it, alpha = s.alpha) }
                    if (s.stroke != null && s.style != null) drawPath(s.path, s.stroke, alpha = s.alpha, style = s.style)
                }
                if (runeAlpha > 0f) drawPath(art.rune.path, art.rune.stroke!!, alpha = runeAlpha, style = art.rune.style!!)
            }
        }
    }
}

private fun DrawScope.drawDrop(f: DropFrame, art: DropArt?, light: Boolean) {
    val hit = Offset(f.hitX.toFloat(), f.hitY.toFloat())
    if (f.shadowOpacity > 0) {
        val rx = f.shadowRx.toFloat()
        val ry = f.shadowRy.toFloat()
        drawOval(SHADOW, Offset(f.shadowCx.toFloat() - rx, f.shadowCy.toFloat() - ry), Size(rx * 2, ry * 2), alpha = f.shadowOpacity.toFloat())
    }
    if (art != null && f.cracksOpacity > 0 && f.crackDrawn > 0) {
        group(f.cracksOpacity.toFloat()) {
            val drawn = f.crackDrawn.toFloat()
            for (c in art.cracks) {
                // The dark edge under each crack, stronger in light so the crack reads on a pale screen.
                val alpha = if (light && c.color == SHADOW) .9f else c.alpha
                drawPath(c.cut(0f, drawn), c.color, alpha = alpha, style = Stroke(c.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
    if (f.flashOpacity > 0) {
        if (light) drawRect(FLASH_LIGHT, alpha = (f.flashOpacity * .5).toFloat()) else drawRect(FLASH, alpha = f.flashOpacity.toFloat())
    }
    for ((ring, color) in listOf(f.ring1 to RING1, f.ring2 to RING2)) {
        if (ring.opacity > 0) drawCircle(color, ring.radius.toFloat(), hit, alpha = ring.opacity.toFloat(), style = Stroke(ring.strokeWidth.toFloat()))
    }
    if (art != null) {
        for (d in f.dust) {
            if (d.opacity <= 0 || d.r <= 0) continue
            val c = Offset(d.cx.toFloat(), d.cy.toFloat())
            val r = d.r.toFloat()
            drawCircle(Brush.radialGradient(*art.chalk, center = c, radius = r), r, c, alpha = d.opacity.toFloat())
            if (light) drawCircle(LIGHT_EDGE, r * .7f, c, alpha = (d.opacity * .45).toFloat(), style = Stroke(1f))
        }
    }
    if (art != null && f.burstOpacity > 0) {
        translate(hit.x, hit.y) {
            scale(f.burstScale.toFloat(), pivot = Offset.Zero) { drawPath(art.burst, BURST, alpha = f.burstOpacity.toFloat()) }
        }
    }
    if (art != null) {
        for (g in f.ghosts) if (g.opacity > 0) group(g.opacity.toFloat()) { drawHammer(art.hammer, g.pose, 0f) }
        f.hammer?.let { drawHammer(art.hammer, it, f.runeOpacity.toFloat()) }
    }
    for (s in f.sparks) {
        if (s.opacity <= 0) continue
        drawLine(SPARK, Offset(s.x1.toFloat(), s.y1.toFloat()), Offset(s.x2.toFloat(), s.y2.toFloat()), ModeSwitchMotion.SPARK_WIDTH.toFloat(), StrokeCap.Round, alpha = s.opacity.toFloat())
    }
}

private fun DrawScope.drawPump(f: PumpFrame, art: PumpArt?, light: Boolean) {
    val src = Offset(f.srcX.toFloat(), f.srcY.toFloat())
    if (f.tintOpacity > 0) drawRect(TINT, alpha = f.tintOpacity.toFloat())
    if (art != null) {
        for (h in f.halos) {
            if (h.opacity <= 0) continue
            val r = h.radius.toFloat()
            drawCircle(Brush.radialGradient(*art.halo, center = src, radius = r), r, src, alpha = h.opacity.toFloat())
        }
    }
    for (ring in f.rings) {
        if (ring.opacity > 0) drawCircle(PUMP_RING, ring.radius.toFloat(), src, alpha = ring.opacity.toFloat(), style = Stroke(ring.strokeWidth.toFloat()))
    }
    if (f.edgeSoftOpacity > 0) drawCircle(EDGE_SOFT, f.floodRadius.toFloat(), src, alpha = f.edgeSoftOpacity.toFloat(), style = Stroke(f.edgeSoftWidth.toFloat()))
    if (f.edgeOpacity > 0) drawCircle(EDGE, f.floodRadius.toFloat(), src, alpha = f.edgeOpacity.toFloat(), style = Stroke(f.edgeWidth.toFloat()))
    if (art == null || f.veinsOpacity <= 0) return
    val front = f.veinFront
    group(f.veinsOpacity.toFloat()) {
        group(f.glowLayerOpacity.toFloat()) {
            for (v in art.veins) if (v.layer == VeinPath.LAYER_GLOW) drawVein(v, front, f.veinWidthScale, null)
        }
        for (v in art.veins) {
            when (v.layer) {
                VeinPath.LAYER_CORE -> drawVein(v, front, f.veinWidthScale, if (light) LIGHT_VEIN_EDGE else null)
                VeinPath.LAYER_HOT -> drawVein(v, front, f.veinWidthScale, null)
            }
        }
        for (n in art.nodes) {
            if (!ModeSwitchMotion.nodeShown(front, n)) continue
            drawCircle(NODE, (n.r * f.nodeScale).toFloat(), Offset(n.x.toFloat(), n.y.toFloat()), alpha = f.nodeOpacity.toFloat())
        }
        for (v in art.veins) {
            if (v.layer != VeinPath.LAYER_PULSE) continue
            val alpha = ModeSwitchMotion.pulseOpacity(f.pulseOpacity, front, v.len).toFloat()
            if (alpha <= 0f) continue
            val from = ModeSwitchMotion.pulseStart(front, v.len).toFloat()
            if (from >= 1f) continue
            val p = v.partial
            drawPath(p.cut(from, min(1f, from + .06f)), p.color, alpha = alpha, style = Stroke(p.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

private fun DrawScope.drawVein(v: Vein, front: Double, widthScale: Double, edge: Color?) {
    val progress = ModeSwitchMotion.veinProgress(front, v.d0, v.len).toFloat()
    if (progress <= 0f) return
    val p = v.partial
    val width = if (v.swell) (p.width * widthScale).toFloat() else p.width
    val path = p.cut(0f, progress)
    if (edge != null) drawPath(path, edge, alpha = .55f, style = Stroke(width + 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, p.color, alpha = p.alpha, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
