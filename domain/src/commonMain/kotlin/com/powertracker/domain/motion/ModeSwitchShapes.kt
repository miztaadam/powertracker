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

/**
 * JavaScript's `Number.prototype.toFixed(digits)` for the sizes used here, half away from zero. Int arithmetic where the
 * number fits (every shape's does): Kotlin/JS emulates Long, and this runs for every point of every vein.
 */
internal fun fixed(v: Double, digits: Int): String {
    var scale = 1.0
    repeat(digits) { scale *= 10 }
    val m = floor(abs(v) * scale + 0.5)
    if (m >= Int.MAX_VALUE) return fixedLong(v, m, digits, scale)
    val n = m.toInt()
    val sign = if (v < 0 && n != 0) "-" else ""
    if (digits == 0) return sign + n
    val unit = scale.toInt()
    val frac = (n % unit).toString()
    return sign + (n / unit) + "." + (if (frac.length < digits) frac.padStart(digits, '0') else frac)
}

private fun fixedLong(v: Double, m: Double, digits: Int, scale: Double): String {
    val n = m.toLong()
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

/** The whole rng(19) run for a hit point: cracks (with molten inner seams), chalk dust, sparks, and starburst. */
internal fun buildDropShapes(hit: Point): DropShapes {
    val r = MotionRng(19)
    val cracks = ArrayList<MotionPath>()
    val n = 15
    for (i in 0 until n) {
        var a = (i.toDouble() / n) * PI * 2 + (r.next() - .5) * .4
        val len = 155 + r.next() * 265
        var x = hit.x
        var y = hit.y
        val d = StringBuilder("M${jsNumber(x)},${jsNumber(y)}")
        val segs = 5 + (r.next() * 3).toInt()
        var bx = 0.0
        var by = 0.0
        var ba = 0.0
        for (s in 0 until segs) {
            a += (r.next() - .5) * .72
            x += cos(a) * len / segs
            y += sin(a) * len / segs
            d.append(" L").append(fixed(x, 1)).append(',').append(fixed(y, 1))
            if (s == 2) {
                bx = x; by = y
                ba = a + (if (r.next() > .5) .75 else -.75)
            }
        }
        val w = 1.6 + r.next() * 2.2
        val path = d.toString()
        cracks += MotionPath(path, null, ModeSwitchMotion.Colors.CRACK_SHADOW, w + 2.6, .6, "round", "round")
        cracks += MotionPath(path, null, ModeSwitchMotion.Colors.CRACK, w, 1.0, "round", "round")
        if (i % 3 == 0) {
            cracks += MotionPath(path, null, ModeSwitchMotion.Colors.CRACK_HOT, max(1.2, w * .8), .85, "round", "round")
        }
        if (i % 2 == 0) {
            val bl = 40 + r.next() * 70
            val bd = "M${fixed(bx, 1)},${fixed(by, 1)} L${fixed(bx + cos(ba) * bl * .5, 1)},${fixed(by + sin(ba) * bl * .5, 1)}" +
                " L${fixed(bx + cos(ba + .4) * bl, 1)},${fixed(by + sin(ba + .4) * bl, 1)}"
            cracks += MotionPath(bd, null, ModeSwitchMotion.Colors.CRACK_BRANCH, 1.1, .95, "round", "round")
        }
    }

    val s = ModeSwitchMotion.DROP.hammerScale
    val dust = ArrayList<Particle>()
    for (i in 0 until 30) {
        val side = if (i % 2 != 0) 1 else -1
        val u = r.next()
        val ox = hit.x + side * u * 164 * s
        val oy = hit.y - u * 44 * s
        val ang = -PI / 2 + side * (.85 + r.next() * .65)
        val sp = 60 + r.next() * 190
        val r0 = 10 + r.next() * 14
        val life = 400 + r.next() * 340
        val alpha = .45 + r.next() * .42
        dust += Particle(ox, oy, cos(ang) * sp, sin(ang) * sp * .55, r0, life, alpha)
    }
    val sparks = ArrayList<Particle>()
    for (i in 0 until 36) {
        val ang = -PI / 2 + (r.next() - .5) * 3.1
        val sp = 165 + r.next() * 330
        val life = 250 + r.next() * 290
        sparks += Particle(hit.x + (r.next() - .5) * 30, hit.y - 6, cos(ang) * sp, sin(ang) * sp, 0.0, life, 1.0)
    }
    val burst = ArrayList<Point>()
    val bn = 18
    for (i in 0 until bn * 2) {
        val a = i.toDouble() / (bn * 2) * PI * 2
        val rad = if (i % 2 != 0) 15 + r.next() * 10 else 72 + r.next() * 72
        burst += Point(fixed(cos(a) * rad * 1.48, 1).toDouble(), fixed(sin(a) * rad * .52, 1).toDouble())
    }
    return DropShapes(cracks, dust, sparks, burst)
}

internal class PumpShapes(val veins: List<VeinPath>, val nodes: List<VeinNode>)

/** Pump layers in drawing order; nodes are drawn between [LAYER_HOT] and [LAYER_PULSE]. */
internal val VEIN_LAYERS = listOf(VeinPath.LAYER_GLOW, VeinPath.LAYER_CORE, VeinPath.LAYER_HOT, VeinPath.LAYER_PULSE)

/**
 * The whole rng(23) run: seven main veins in a 120-degree fan centred on [fanCentreDeg], each running to the screen's
 * edge from [src], capped at 840 x [reach], with their branches.
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
        vein(VeinPath.LAYER_GLOW, pts, false, d0, (n * step).toDouble(), if (level == 1) 7.0 else 4.5)
        vein(VeinPath.LAYER_CORE, pts, false, d0, (n * step).toDouble(), if (level == 1) 1.5 else .95)
        nodes += VeinNode(p0[0] + src.x, p0[1] + src.y, if (level == 1) 2.8 else 1.8, d0)
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
            wander = wander * .72 + (pr.next() - .5) * .58
            val hd = base + wander
            x += cos(hd) * step; y += sin(hd) * step
            pts += doubleArrayOf(x, y); heads += hd
        }
        val len = (n * step).toDouble()
        val k1 = max(2, (n * .6).roundToInt())
        val k2 = max(2, (n * .28).roundToInt())
        vein(VeinPath.LAYER_GLOW, pts, false, 0.0, len, 10.0)
        vein(VeinPath.LAYER_CORE, pts, false, 0.0, len, 1.7)
        vein(VeinPath.LAYER_CORE, pts.subList(0, k1 + 1), true, 0.0, (k1 - .5) * step, 2.8)
        vein(VeinPath.LAYER_CORE, pts.subList(0, k2 + 1), true, 0.0, (k2 - .5) * step, 3.6)
        vein(VeinPath.LAYER_HOT, pts.subList(0, k1 + 1), true, 0.0, (k1 - .5) * step, 1.1)
        pulses += VeinPath(smoothD(pts, false), VeinPath.LAYER_PULSE, VeinPath.colorOf(VeinPath.LAYER_PULSE), 3.2, 0.0, len, false)
        var side = if (pr.next() > .5) 1 else -1
        for (s in 2 until n - 1) {
            if (pr.next() < .48) {
                side = -side
                val turn = heads[s - 1] + side * (.45 + pr.next() * .5)
                val bl = 58 + pr.next() * 125 * (1 - .4 * s / n)
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

private const val RUNE_D = "M-10,-104 L-10,-46 M-10,-92 L18,-75 L-10,-58"

private val KNURL_D: String by lazy {
    val sb = StringBuilder()
    var y = -268
    while (y <= -186) {
        val half = 15.0 + ((y + 282.0) / 128.0) * 6.0
        val l = fixed(-half + 1.0, 1)
        val r = fixed(half - 1.0, 1)
        if (sb.isNotEmpty()) sb.append(' ')
        sb.append("M").append(l).append(',').append(y).append(" L").append(r).append(',').append(y + 9)
        sb.append(" M").append(r).append(',').append(y).append(" L").append(l).append(',').append(y + 9)
        y += 18
    }
    sb.toString()
}

/** The forged iron hammer, back to front, with the striking face at (0, 0) and the haft up. */
internal val HAMMER: List<MotionPath> by lazy {
    val ink = "#090B0E"
    listOf(
        // hanging steel ring & machined pommel
        MotionPath(circleD(0.0, -312.0, 12.0), null, "url(#gCollar)", 6.5, 1.0),
        MotionPath("M-24,-282 L-18,-300 L18,-300 L24,-282 Z", "url(#gCollar)", ink, 1.8, 1.0, lineJoin = "round"),
        MotionPath("M-16,-297 L16,-297 L19,-290 L-19,-290 Z", "#E5EAF0", null, 0.0, .35),
        // tapered haft with knurled leather/steel grip
        MotionPath("M-15,-282 L15,-282 L22,-154 L-22,-154 Z", "url(#gGrip)", ink, 1.8, 1.0),
        MotionPath(KNURL_D, null, "#090807", 1.5, .65),
        MotionPath(KNURL_D, null, "#8E7B68", .7, .22),
        // machined iron grip collars & bolster socket
        MotionPath("M-20,-284 L20,-284 L21,-273 L-21,-273 Z", "url(#gCollar)", ink, 1.6, 1.0),
        MotionPath("M-21,-228 L21,-228 L22,-217 L-22,-217 Z", "url(#gCollar)", ink, 1.6, 1.0),
        MotionPath("M-26,-178 L26,-178 L42,-146 L-42,-146 Z", "url(#gCollar)", ink, 2.0, 1.0, lineJoin = "round"),
        MotionPath("M-24,-175 L24,-175 L27,-168 L-27,-168 Z", "#E5EAF0", null, 0.0, .35),
        // main forged octagonal-beveled head
        MotionPath(
            "M-44,-150 L-126,-142 L-162,-112 L-166,-44 L-128,-16 L-42,-4 L0,0 L42,-4 L128,-16 L166,-44 L162,-112 L126,-142 L44,-150 Z",
            "url(#gSteelFace)", ink, 2.6, 1.0, lineJoin = "round",
        ),
        // top crown bright chamfer
        MotionPath(
            "M-44,-150 L-126,-142 L-116,-124 L-40,-131 L40,-131 L116,-124 L126,-142 L44,-150 Z",
            "#BFC6D0", null, 0.0, .82,
        ),
        // left dark forged cheek & right lit forged cheek
        MotionPath(
            "M-126,-142 L-162,-112 L-166,-44 L-128,-16 L-114,-34 L-138,-52 L-136,-104 L-116,-124 Z",
            "#181C21", null, 0.0, .9,
        ),
        MotionPath(
            "M126,-142 L162,-112 L166,-44 L128,-16 L114,-34 L138,-52 L136,-104 L116,-124 Z",
            "#8E96A1", null, 0.0, .58,
        ),
        // bottom striking face bevel
        MotionPath(
            "M-128,-16 L-42,-4 L0,0 L42,-4 L128,-16 L114,-34 L38,-22 L0,-18 L-38,-22 L-114,-34 Z",
            "#363C44", null, 0.0, .92,
        ),
        // inner recessed forged panel outline & top specular ridge
        MotionPath(
            "M-116,-124 L-40,-131 L40,-131 L116,-124 L136,-104 L138,-52 L114,-34 L38,-22 L0,-18 L-38,-22 L-114,-34 L-138,-52 L-136,-104 Z",
            null, ink, 2.4, 1.0, lineJoin = "round",
        ),
        MotionPath("M-114,-122 L-40,-129 L40,-129 L114,-122", null, "#E8EDF5", 1.6, .65, lineCap = "round"),
        // twin plate-style weight grooves on left & right wings
        MotionPath("M-96,-110 L-96,-48 M-78,-112 L-78,-44", null, ink, 4.0, .78, lineCap = "round"),
        MotionPath("M-94,-110 L-94,-48 M-76,-112 L-76,-44", null, "#A8B1BC", 1.2, .45, lineCap = "round"),
        MotionPath("M96,-110 L96,-48 M78,-112 L78,-44", null, ink, 4.0, .78, lineCap = "round"),
        MotionPath("M98,-110 L98,-48 M80,-112 L80,-44", null, "#C6CED8", 1.2, .45, lineCap = "round"),
        // center recessed rune plate
        MotionPath("M-36,-116 L36,-116 L46,-76 L34,-34 L-34,-34 L-46,-76 Z", "#171A1F", ink, 2.2, 1.0, lineJoin = "round"),
        MotionPath("M-34,-114 L34,-114", null, "#9BA4B0", 1.2, .5),
        // deeply chiseled thurisaz rune
        MotionPath(RUNE_D, null, "#050608", 9.0, 1.0, lineCap = "square", lineJoin = "miter"),
    )
}

/** The rune's golden core stroke, drawn over the hammer at [DropFrame.runeOpacity]. */
internal val HAMMER_RUNE_GLOW = MotionPath(RUNE_D, null, "#FFB84D", 5.5, 1.0, lineCap = "square", lineJoin = "miter")

/** The full molten rune glow layers (ember bloom, orange halo, gold core, white-hot center, wing embers, top glint). */
internal val HAMMER_RUNE_GLOWS: List<MotionPath> by lazy {
    listOf(
        MotionPath(circleD(0.0, -75.0, 58.0), "url(#gEmber)", null, 0.0, 1.0),
        MotionPath(RUNE_D, null, "#FF6B1A", 9.0, .7, lineCap = "square", lineJoin = "miter"),
        HAMMER_RUNE_GLOW,
        MotionPath(RUNE_D, null, "#FFF8E7", 2.2, 1.0, lineCap = "square", lineJoin = "miter"),
        MotionPath("M-78,-108 L-78,-48 M78,-108 L78,-48", null, "#FF9A3C", 2.0, .7, lineCap = "round"),
        MotionPath("M-116,-124 L-40,-131 L40,-131 L116,-124", null, "#FFFFFF", 3.0, .75, lineCap = "round"),
    )
}

/** The `<defs>` gradients. Linear ones run in the shape's bounding box (SVG's default units). */
internal val MOTION_GRADIENTS: List<MotionGradient> by lazy {
    listOf(
        MotionGradient("gSteelFace", false, .15, 0.0, .85, 1.0, listOf(GradientStop(0.0, "#6F7782", 1.0), GradientStop(.32, "#3E444D", 1.0), GradientStop(.72, "#22262C", 1.0), GradientStop(1.0, "#121519", 1.0))),
        MotionGradient("gGrip", false, 0.0, 0.0, 1.0, 0.0, listOf(GradientStop(0.0, "#12100E", 1.0), GradientStop(.35, "#3E332B", 1.0), GradientStop(.68, "#241E19", 1.0), GradientStop(1.0, "#0E0C0A", 1.0))),
        MotionGradient("gCollar", false, 0.0, 0.0, 1.0, 0.0, listOf(GradientStop(0.0, "#20242A", 1.0), GradientStop(.3, "#C5CCD6", 1.0), GradientStop(.55, "#646C77", 1.0), GradientStop(1.0, "#1A1E23", 1.0))),
        MotionGradient("gEmber", true, 0.0, 0.0, 0.0, 0.0, listOf(GradientStop(0.0, "#FFF6DC", 1.0), GradientStop(.35, "#FF9F3F", .8), GradientStop(.75, "#EA580C", .25), GradientStop(1.0, "#EA580C", 0.0))),
        MotionGradient("gHalo", true, 0.0, 0.0, 0.0, 0.0, listOf(GradientStop(0.0, "#DCEBFF", .85), GradientStop(.38, "#60A5FA", .35), GradientStop(1.0, "#2563EB", 0.0))),
        MotionGradient("gChalk", true, 0.0, 0.0, 0.0, 0.0, listOf(GradientStop(0.0, "#F3F4EF", .92), GradientStop(.52, "#C9CED6", .45), GradientStop(1.0, "#C9CED6", 0.0))),
    )
}
