package com.powertracker.domain.motion

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * The seeded shape builders of the mode switch prototype (docs/design/motion/mode-switch-sound.html), ported
 * call for call. rng(7) is one stream used in order by the cracks, the chalk dust, the sparks and the starburst;
 * rng(23) is the pump's stream, the main veins and their branches interleaved. The order of every draw matters,
 * so nothing here may be reordered.
 */

/** The prototype's `rng` (mulberry32): the same seed gives the same numbers as the page, bit for bit. */
internal class MotionRng(seed: Int) {
    private var s = seed

    fun next(): Double {
        s += 0x6D2B79F5
        var t = (s xor (s ushr 15)) * (1 or s)
        t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
        return (t xor (t ushr 14)).toUInt().toDouble() / 4294967296.0
    }
}

/** JavaScript's `Number.prototype.toFixed(digits)` for the sizes used here, half away from zero. */
internal fun fixed(v: Double, digits: Int): String {
    var scale = 1.0
    repeat(digits) { scale *= 10 }
    val n = floor(abs(v) * scale + 0.5).toLong()
    val sign = if (v < 0 && n != 0L) "-" else ""
    if (digits == 0) return sign + n
    val unit = scale.toLong()
    return sign + (n / unit) + "." + (n % unit).toString().padStart(digits, '0')
}

/** A number as JavaScript prints it in a template string, for the whole numbers and halves the hit point can be. */
private fun jsNumber(v: Double): String = if (v == floor(v) && abs(v) < 1e15) v.toLong().toString() else fixed(v, 3).trimEnd('0').trimEnd('.')

internal class DropShapes(
    val cracks: List<MotionPath>,
    val dust: List<Particle>,
    val sparks: List<Particle>,
    val burst: List<Point>,
)

/** The whole rng(7) run for a hit point: cracks, then dust, then sparks, then the burst, as on the page. */
internal fun buildDropShapes(hit: Point): DropShapes {
    val r = MotionRng(7)
    val cracks = ArrayList<MotionPath>()
    val n = 15
    for (i in 0 until n) {
        var a = (i.toDouble() / n) * PI * 2 + (r.next() - .5) * .45
        val len = 150 + r.next() * 260
        var x = hit.x
        var y = hit.y
        val d = StringBuilder("M${jsNumber(x)},${jsNumber(y)}")
        val segs = 5 + (r.next() * 3).toInt()
        var bx = 0.0
        var by = 0.0
        var ba = 0.0
        for (s in 0 until segs) {
            a += (r.next() - .5) * .7
            x += cos(a) * len / segs
            y += sin(a) * len / segs
            d.append(" L").append(fixed(x, 1)).append(',').append(fixed(y, 1))
            if (s == 2) {
                bx = x; by = y
                ba = a + (if (r.next() > .5) .8 else -.8)
            }
        }
        val w = 1.8 + r.next() * 2.2
        val path = d.toString()
        cracks += MotionPath(path, null, ModeSwitchMotion.Colors.CRACK_SHADOW, w + 2.5, .55, "round", "round")
        cracks += MotionPath(path, null, ModeSwitchMotion.Colors.CRACK, w, 1.0, "round", "round")
        if (i % 2 == 0) {
            val bl = 40 + r.next() * 70
            val bd = "M${fixed(bx, 1)},${fixed(by, 1)} L${fixed(bx + cos(ba) * bl * .5, 1)},${fixed(by + sin(ba) * bl * .5, 1)}" +
                " L${fixed(bx + cos(ba + .4) * bl, 1)},${fixed(by + sin(ba + .4) * bl, 1)}"
            cracks += MotionPath(bd, null, ModeSwitchMotion.Colors.CRACK_BRANCH, 1.0, 1.0, "round", "round")
        }
    }

    val s = ModeSwitchMotion.DROP.hammerScale
    val dust = ArrayList<Particle>()
    for (i in 0 until 30) {
        val side = if (i % 2 != 0) 1 else -1
        val u = r.next()
        val ox = hit.x + side * u * 158 * s
        val oy = hit.y - u * 50 * s
        val ang = -PI / 2 + side * (.5 + r.next() * 1.0)
        val sp = 60 + r.next() * 170
        val r0 = 9 + r.next() * 14
        val life = 380 + r.next() * 340
        val alpha = .45 + r.next() * .4
        dust += Particle(ox, oy, cos(ang) * sp, sin(ang) * sp * .75, r0, life, alpha)
    }
    val sparks = ArrayList<Particle>()
    for (i in 0 until 34) {
        val ang = -PI / 2 + (r.next() - .5) * 3.4
        val sp = 160 + r.next() * 300
        val life = 240 + r.next() * 260
        sparks += Particle(hit.x, hit.y - 6, cos(ang) * sp, sin(ang) * sp, 0.0, life, 1.0)
    }
    val burst = ArrayList<Point>()
    val bn = 18
    for (i in 0 until bn * 2) {
        val a = i.toDouble() / (bn * 2) * PI * 2
        val rad = if (i % 2 != 0) 16 + r.next() * 10 else 70 + r.next() * 70
        burst += Point(fixed(cos(a) * rad * 1.5, 1).toDouble(), fixed(sin(a) * rad * .55, 1).toDouble())
    }
    return DropShapes(cracks, dust, sparks, burst)
}

internal class PumpShapes(val veins: List<VeinPath>, val nodes: List<VeinNode>)

/** Pump layers in drawing order; nodes are drawn between [LAYER_HOT] and [LAYER_PULSE]. */
internal val VEIN_LAYERS = listOf(VeinPath.LAYER_GLOW, VeinPath.LAYER_CORE, VeinPath.LAYER_HOT, VeinPath.LAYER_PULSE)

/**
 * The whole rng(23) run: seven main veins in a 120-degree fan centred on [fanCentreDeg] (the prototype's fan is
 * centred on 228), each running to the screen's edge from [src], capped at 840 x [reach], with their branches.
 */
internal fun buildPumpShapes(w: Double, h: Double, src: Point, fanCentreDeg: Double, reach: Double): PumpShapes {
    val pr = MotionRng(23)
    val veins = ArrayList<VeinPath>()
    val nodes = ArrayList<VeinNode>()
    fun f1(p: DoubleArray) = fixed(p[0] + src.x, 1) + "," + fixed(p[1] + src.y, 1)
    fun midp(a: DoubleArray, b: DoubleArray) = doubleArrayOf((a[0] + b[0]) / 2, (a[1] + b[1]) / 2)
    fun smoothD(pts: List<DoubleArray>, toMid: Boolean): String {
        val d = StringBuilder("M").append(f1(pts[0])).append(" L").append(f1(midp(pts[0], pts[1])))
        for (i in 1 until pts.size - 1) d.append(" Q").append(f1(pts[i])).append(' ').append(f1(midp(pts[i], pts[i + 1])))
        if (!toMid) d.append(" L").append(f1(pts[pts.size - 1]))
        return d.toString()
    }
    fun vein(layer: String, pts: List<DoubleArray>, toMid: Boolean, d0: Double, len: Double, width: Double) {
        veins += VeinPath(smoothD(pts, toMid), layer, VeinPath.colorOf(layer), width, d0, len, layer != VeinPath.LAYER_GLOW)
    }
    fun branch(p0: DoubleArray, ang: Double, length: Double, d0: Double, level: Int) {
        val step = 24
        val n = max(2, (length / step).roundToInt())
        val pts = arrayListOf(p0)
        val heads = ArrayList<Double>()
        var x = p0[0]
        var y = p0[1]
        var wander = 0.0
        for (s in 0 until n) {
            wander = wander * .7 + (pr.next() - .5) * .8
            val hd = ang + wander
            x += cos(hd) * step; y += sin(hd) * step
            pts += doubleArrayOf(x, y); heads += hd
        }
        vein(VeinPath.LAYER_GLOW, pts, false, d0, (n * step).toDouble(), if (level == 1) 6.0 else 4.0)
        vein(VeinPath.LAYER_CORE, pts, false, d0, (n * step).toDouble(), if (level == 1) 1.3 else .9)
        nodes += VeinNode(p0[0] + src.x, p0[1] + src.y, if (level == 1) 2.6 else 1.7, d0)
        if (level == 1 && n > 2 && pr.next() < .65) {
            val k = 1 + (pr.next() * (n - 1)).toInt()
            val sign = if (pr.next() > .5) 1 else -1
            val turn = heads[k - 1] + sign * (.5 + pr.next() * .45)
            val bl = 30 + pr.next() * 55
            branch(pts[k], turn, bl, d0 + k * step, 2)
        }
    }

    val count = 7
    val step = 34
    val pulses = ArrayList<VeinPath>()
    for (i in 0 until count) {
        val base = (fanCentreDeg - 60 + i * (120 / (count - 1)) + (pr.next() - .5) * 8) * PI / 180
        val dx = cos(base)
        val dy = sin(base)
        val toEdge = min(if (dx < 0) src.x / -dx else (w - src.x) / dx, if (dy < 0) src.y / -dy else (h - src.y) / dy)
        val n = max(4, (min(840 * reach, toEdge + 30) / step).roundToInt())
        val pts = arrayListOf(doubleArrayOf(0.0, 0.0))
        val heads = ArrayList<Double>()
        var x = 0.0
        var y = 0.0
        var wander = 0.0
        for (s in 0 until n) {
            wander = wander * .72 + (pr.next() - .5) * .62
            val hd = base + wander
            x += cos(hd) * step; y += sin(hd) * step
            pts += doubleArrayOf(x, y); heads += hd
        }
        val len = (n * step).toDouble()
        val k1 = max(2, (n * .6).roundToInt())
        val k2 = max(2, (n * .28).roundToInt())
        vein(VeinPath.LAYER_GLOW, pts, false, 0.0, len, 9.0)
        vein(VeinPath.LAYER_CORE, pts, false, 0.0, len, 1.5)
        vein(VeinPath.LAYER_CORE, pts.subList(0, k1 + 1), true, 0.0, (k1 - .5) * step, 2.4)
        vein(VeinPath.LAYER_CORE, pts.subList(0, k2 + 1), true, 0.0, (k2 - .5) * step, 3.4)
        vein(VeinPath.LAYER_HOT, pts.subList(0, k1 + 1), true, 0.0, (k1 - .5) * step, .9)
        pulses += VeinPath(smoothD(pts, false), VeinPath.LAYER_PULSE, VeinPath.colorOf(VeinPath.LAYER_PULSE), 2.6, 0.0, len, false)
        var side = if (pr.next() > .5) 1 else -1
        for (s in 2 until n - 1) {
            if (pr.next() < .5) {
                side = -side
                val turn = heads[s - 1] + side * (.45 + pr.next() * .5)
                val bl = 60 + pr.next() * 130 * (1 - .4 * s / n)
                branch(pts[s], turn, bl, (s * step).toDouble(), 1)
            }
        }
    }
    val ordered = VEIN_LAYERS.flatMap { layer -> (veins + pulses).filter { it.layer == layer } }
    return PumpShapes(ordered, nodes)
}

/** The distance from [p] to the screen corner furthest from it. */
internal fun farCorner(w: Double, h: Double, p: Point): Double =
    max(max(hypot(p.x, p.y), hypot(w - p.x, p.y)), max(hypot(p.x, h - p.y), hypot(w - p.x, h - p.y)))

private fun circleD(cx: Double, cy: Double, r: Double): String {
    val a = jsNumber(cx - r)
    val b = jsNumber(cx + r)
    val c = jsNumber(cy)
    val rr = jsNumber(r)
    return "M$a,$c A$rr,$rr 0 1,0 $b,$c A$rr,$rr 0 1,0 $a,$c Z"
}

private const val RUNE_D = "M-9,-116 L-9,-36 M-9,-100 L15,-78 L-9,-56"

/** The hammer, in the page's `#hammerShape` order. Circles are written as two arcs. */
internal val HAMMER: List<MotionPath> by lazy {
    val ink = "#0B0C0E"
    listOf(
        // hanging ring and pommel
        MotionPath(circleD(0.0, -306.0, 11.0), null, "url(#gBand)", 6.0, 1.0),
        MotionPath("M-22,-278 Q-24,-296 0,-296 Q24,-296 22,-278 Z", "url(#gBand)", null, 0.0, 1.0),
        // haft: bare dark oak, tapered, three plain iron bands
        MotionPath("M-15,-280 L15,-280 L21,-150 L-21,-150 Z", "url(#gWood)", null, 0.0, 1.0),
        MotionPath("M-6,-276 L-8,-154 M3,-276 L4,-154 M10,-276 L13,-154", null, "#0C0A08", 1.2, .7),
        MotionPath("M-19,-284 L19,-284 L19.6,-272 L-19.6,-272 Z", "url(#gBand)", null, 0.0, 1.0),
        MotionPath("M-20,-226 L20,-226 L20.6,-214 L-20.6,-214 Z", "url(#gBand)", null, 0.0, 1.0),
        // socket where haft meets head
        MotionPath("M-25,-176 L25,-176 L34,-142 L-34,-142 Z", "url(#gBand)", null, 0.0, 1.0),
        // head: broad pendant-style wedge, flared ends, keel underside
        MotionPath(
            "M-30,-150 C-70,-150 -108,-140 -148,-114 L-160,-52 Q-86,-34 -30,-7 Q-12,0 0,0 Q12,0 30,-7 Q86,-34 160,-52 L148,-114 C108,-140 70,-150 30,-150 Z",
            "url(#gIron)", ink, 2.0, 1.0, lineJoin = "round",
        ),
        // top bevel catching light
        MotionPath(
            "M-30,-148 C-70,-148 -108,-138 -146,-113 L-144,-104 C-106,-128 -70,-138 -30,-139 L30,-139 C70,-138 106,-128 144,-104 L146,-113 C108,-138 70,-148 30,-148 Z",
            "#C9CED6", null, 0.0, .55,
        ),
        // end faces
        MotionPath("M-148,-114 L-160,-52 L-146,-56 L-137,-108 Z", "#0F1113", null, 0.0, .7),
        MotionPath("M148,-114 L160,-52 L146,-56 L137,-108 Z", "#9AA1AA", null, 0.0, .35),
        // incised border
        MotionPath(
            "M-30,-128 C-66,-127 -98,-120 -130,-100 L-138,-62 Q-78,-46 -28,-22 Q-10,-14 0,-14 Q10,-14 28,-22 Q78,-46 138,-62 L130,-100 C98,-120 66,-127 30,-128 Z",
            null, ink, 2.4, .75,
        ),
        MotionPath(
            "M-30,-126 C-66,-125 -98,-118 -129,-98.500 L-136.500,-61 Q-78,-44 -28,-20 Q-10,-12 0,-12 Q10,-12 28,-20 Q78,-44 136.500,-61 L129,-98.500 C98,-118 66,-125 30,-126 Z",
            null, "#B8BEC6", 1.0, .35,
        ),
        // ring-and-dot punch marks
        MotionPath(circleD(-98.0, -84.0, 13.0), null, ink, 2.6, 1.0),
        MotionPath(circleD(98.0, -84.0, 13.0), null, ink, 2.6, 1.0),
        MotionPath(circleD(-58.0, -66.0, 6.0), null, ink, 2.6, 1.0),
        MotionPath(circleD(58.0, -66.0, 6.0), null, ink, 2.6, 1.0),
        MotionPath(circleD(-98.0, -84.0, 4.0), ink, null, 0.0, 1.0),
        MotionPath(circleD(98.0, -84.0, 4.0), ink, null, 0.0, 1.0),
        MotionPath(circleD(-58.0, -66.0, 1.8), ink, null, 0.0, 1.0),
        MotionPath(circleD(58.0, -66.0, 1.8), ink, null, 0.0, 1.0),
        // thurisaz, cut into the iron
        MotionPath(RUNE_D, null, "#08090A", 8.0, 1.0, lineCap = "square", lineJoin = "miter"),
    )
}

/** The rune's glow, drawn over the hammer at [DropFrame.runeOpacity]. */
internal val HAMMER_RUNE_GLOW = MotionPath(RUNE_D, null, "#FFC98A", 5.0, 1.0, lineCap = "square", lineJoin = "miter")

/** The page's `<defs>` gradients. Linear ones run in the shape's bounding box (SVG's default units). */
internal val MOTION_GRADIENTS: List<MotionGradient> by lazy {
    listOf(
        MotionGradient("gIron", false, 0.0, 0.0, 0.0, 1.0, listOf(GradientStop(0.0, "#7B828B", 1.0), GradientStop(.38, "#4A5058", 1.0), GradientStop(1.0, "#1D2024", 1.0))),
        MotionGradient("gBand", false, 0.0, 0.0, 1.0, 0.0, listOf(GradientStop(0.0, "#2A2E33", 1.0), GradientStop(.35, "#9AA1AA", 1.0), GradientStop(1.0, "#23262A", 1.0))),
        MotionGradient("gWood", false, 0.0, 0.0, 1.0, 0.0, listOf(GradientStop(0.0, "#15110E", 1.0), GradientStop(.4, "#4A3A2C", 1.0), GradientStop(1.0, "#120F0C", 1.0))),
        MotionGradient("gHalo", true, 0.0, 0.0, 0.0, 0.0, listOf(GradientStop(0.0, "#BFD7FF", .75), GradientStop(.4, "#6FA4FF", .32), GradientStop(1.0, "#4F8DFF", 0.0))),
        MotionGradient("gChalk", true, 0.0, 0.0, 0.0, 0.0, listOf(GradientStop(0.0, "#E4E6E2", .9), GradientStop(.55, "#C9CED6", .45), GradientStop(1.0, "#C9CED6", 0.0))),
    )
}
