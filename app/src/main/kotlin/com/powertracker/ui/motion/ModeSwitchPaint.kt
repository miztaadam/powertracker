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

/*
 * The mode switch's painting (1.15), moved here unchanged from ModeSwitchOverlay.kt (1.16) so the guide's screen 2 can
 * draw the hammer and the pump in a small box: the shapes, built once per run, and the drop's and the pump's frames.
 */

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

internal class Shape(val path: Path, val fill: Brush?, val stroke: Brush?, val style: Stroke?, val alpha: Float)

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
internal class Partial(val measure: PathMeasure, val length: Float, val color: Color, val width: Float, val alpha: Float) {
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

internal class HammerArt(val shapes: List<Shape>, val runeGlows: List<Shape>)

private val HAMMER_ART: HammerArt by lazy { HammerArt(ModeSwitchMotion.HAMMER_PATHS.map(::shapeOf), ModeSwitchMotion.HAMMER_RUNE_PATHS.map(::shapeOf)) }

internal class DropArt(
    val hammer: HammerArt,
    val cracks: List<Partial>,
    val burst: Path,
    val chalk: Array<Pair<Float, Color>>,
    val chalkLight: Array<Pair<Float, Color>>,
)

internal fun buildDropArt(w: Double, h: Double): DropArt {
    val hit = ModeSwitchMotion.hitPoint(w, h)
    val cracks = ModeSwitchMotion.cracks(w, h, hit).map { partialOf(it.d, it.stroke ?: "#000000", it.strokeWidth, it.opacity) }
    val burst = Path().apply {
        ModeSwitchMotion.burst(hit).forEachIndexed { i, p -> if (i == 0) moveTo(p.x.toFloat(), p.y.toFloat()) else lineTo(p.x.toFloat(), p.y.toFloat()) }
        close()
    }
    val chalk = GRADIENT_BY_ID.getValue("gChalk").stops.map { it.offset.toFloat() to hex(it.color).copy(alpha = it.opacity.toFloat()) }.toTypedArray()
    val chalkLight = ModeSwitchMotion.LightColors.CHALK.map { it.offset.toFloat() to hex(it.color).copy(alpha = it.opacity.toFloat()) }.toTypedArray()
    return DropArt(HAMMER_ART, cracks, burst, chalk, chalkLight)
}

internal class Vein(val partial: Partial, val layer: String, val swell: Boolean, val d0: Double, val len: Double)

internal class PumpArt(val veins: List<Vein>, val nodes: List<VeinNode>, val halo: Array<Pair<Float, Color>>)

internal fun buildPumpArt(w: Double, h: Double, src: Point): PumpArt {
    val veins = ModeSwitchMotion.veins(w, h, src).map { v: VeinPath -> Vein(partialOf(v.d, v.color, v.width, 1.0), v.layer, v.swell, v.d0, v.len) }
    val halo = GRADIENT_BY_ID.getValue("gHalo").stops.map { it.offset.toFloat() to hex(it.color).copy(alpha = it.opacity.toFloat()) }.toTypedArray()
    return PumpArt(veins, ModeSwitchMotion.veinNodes(w, h, src), halo)
}

// ---- drawing, in dp ----

private val SHADOW = hex(ModeSwitchMotion.Colors.SHADOW)
private val FLASH = hex(ModeSwitchMotion.Colors.FLASH)
private val RING1 = hex(ModeSwitchMotion.Colors.RING1)
private val RING2 = hex(ModeSwitchMotion.Colors.RING2)
private val BURST = hex(ModeSwitchMotion.Colors.BURST)
private val SPARK = hex(ModeSwitchMotion.Colors.SPARK)
private val TINT = hex(ModeSwitchMotion.Colors.TINT)
private val PUMP_RING = hex(ModeSwitchMotion.Colors.PUMP_RING)
private val EDGE = hex(ModeSwitchMotion.Colors.EDGE)
private val EDGE_SOFT = hex(ModeSwitchMotion.Colors.EDGE_SOFT)
private val NODE = hex(ModeSwitchMotion.Colors.NODE)

/** The light scheme's effects (spec "Settled" 1): the domain's one set, the web's too. */
private object Light {
    val FLASH = hex(ModeSwitchMotion.LightColors.FLASH)
    val RING1 = hex(ModeSwitchMotion.LightColors.RING1)
    val SPARK = hex(ModeSwitchMotion.LightColors.SPARK)
    val BURST_EDGE = hex(ModeSwitchMotion.LightColors.BURST_EDGE)
    val VEIN_EDGE = hex(ModeSwitchMotion.LightColors.VEIN_EDGE)
}

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
                if (runeAlpha > 0f) {
                    for (r in art.runeGlows) {
                        val a = runeAlpha * r.alpha
                        r.fill?.let { drawPath(r.path, it, alpha = a) }
                        if (r.stroke != null && r.style != null) drawPath(r.path, r.stroke, alpha = a, style = r.style)
                    }
                }
            }
        }
    }
}

internal fun DrawScope.drawDrop(f: DropFrame, art: DropArt?, light: Boolean) {
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
                val alpha = if (light && c.color == SHADOW) ModeSwitchMotion.LightColors.CRACK_SHADOW_OPACITY.toFloat() else c.alpha
                drawPath(c.cut(0f, drawn), c.color, alpha = alpha, style = Stroke(c.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
    if (f.flashOpacity > 0) {
        if (light) drawRect(Light.FLASH, alpha = (f.flashOpacity * ModeSwitchMotion.LightColors.FLASH_OPACITY_SCALE).toFloat()) else drawRect(FLASH, alpha = f.flashOpacity.toFloat())
    }
    for ((ring, color) in listOf(f.ring1 to (if (light) Light.RING1 else RING1), f.ring2 to RING2)) {
        if (ring.opacity > 0) drawCircle(color, ring.radius.toFloat(), hit, alpha = ring.opacity.toFloat(), style = Stroke(ring.strokeWidth.toFloat()))
    }
    if (art != null) {
        for (d in f.dust) {
            if (d.opacity <= 0 || d.r <= 0) continue
            val c = Offset(d.cx.toFloat(), d.cy.toFloat())
            val r = d.r.toFloat()
            drawCircle(Brush.radialGradient(*(if (light) art.chalkLight else art.chalk), center = c, radius = r), r, c, alpha = d.opacity.toFloat())
        }
    }
    if (art != null && f.burstOpacity > 0) {
        translate(hit.x, hit.y) {
            scale(f.burstScale.toFloat(), pivot = Offset.Zero) {
                drawPath(art.burst, BURST, alpha = f.burstOpacity.toFloat())
                if (light) {
                    drawPath(art.burst, Light.BURST_EDGE, alpha = f.burstOpacity.toFloat(), style = Stroke(ModeSwitchMotion.LightColors.BURST_EDGE_WIDTH.toFloat(), join = StrokeJoin.Round))
                }
            }
        }
    }
    if (art != null) {
        for (g in f.ghosts) if (g.opacity > 0) group(g.opacity.toFloat()) { drawHammer(art.hammer, g.pose, 0f) }
        f.hammer?.let { drawHammer(art.hammer, it, f.runeOpacity.toFloat()) }
    }
    for (s in f.sparks) {
        if (s.opacity <= 0) continue
        drawLine(if (light) Light.SPARK else SPARK, Offset(s.x1.toFloat(), s.y1.toFloat()), Offset(s.x2.toFloat(), s.y2.toFloat()), ModeSwitchMotion.SPARK_WIDTH.toFloat(), StrokeCap.Round, alpha = s.opacity.toFloat())
    }
}

internal fun DrawScope.drawPump(f: PumpFrame, art: PumpArt?, light: Boolean) {
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
            for (v in art.veins) if (v.layer == VeinPath.LAYER_GLOW) drawVein(v, front, f.veinWidthScale)
        }
        // Light: a navy edge under the core veins, one layer below them (as the web draws it).
        if (light) {
            group(ModeSwitchMotion.LightColors.VEIN_EDGE_OPACITY.toFloat()) {
                for (v in art.veins) if (v.layer == VeinPath.LAYER_CORE) drawVeinEdge(v, front, f.veinWidthScale)
            }
        }
        for (v in art.veins) {
            when (v.layer) {
                VeinPath.LAYER_CORE, VeinPath.LAYER_HOT -> drawVein(v, front, f.veinWidthScale)
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

private fun DrawScope.drawVein(v: Vein, front: Double, widthScale: Double) {
    val progress = ModeSwitchMotion.veinProgress(front, v.d0, v.len).toFloat()
    if (progress <= 0f) return
    val p = v.partial
    val width = if (v.swell) (p.width * widthScale).toFloat() else p.width
    drawPath(p.cut(0f, progress), p.color, alpha = p.alpha, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** The light scheme's edge under a core vein: the core again in navy, as wide as [ModeSwitchMotion.veinEdgeWidth]. */
private fun DrawScope.drawVeinEdge(v: Vein, front: Double, widthScale: Double) {
    val progress = ModeSwitchMotion.veinProgress(front, v.d0, v.len).toFloat()
    if (progress <= 0f) return
    val width = ModeSwitchMotion.veinEdgeWidth(v.partial.width.toDouble(), if (v.swell) widthScale else 1.0).toFloat()
    drawPath(v.partial.cut(0f, progress), Light.VEIN_EDGE, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
