package com.powertracker.domain.motion

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A point on the screen, in the same units as the screen size passed in (dp on Android, CSS px on the web). */
data class Point(val x: Double, val y: Double)

/** The hammer drop's timeline in ms from the tap, and the hammer's size. */
data class DropTimeline(
    val fall: Double,
    val impact: Double,
    val hold: Double,
    val settle: Double,
    val lift: Double,
    val end: Double,
    val hammerScale: Double,
)

/** The pump's timeline in ms from the tap: one entry per beat in each list. */
data class PumpTimeline(
    val beats: List<Double>,
    val amps: List<Double>,
    val rise: List<Double>,
    val fall: List<Double>,
    val reach: List<Double>,
    val surge: List<Double>,
    val flood: Double,
    val end: Double,
)

/**
 * One shape: SVG path data, a fill and a stroke (null for none; a `#RRGGBB` colour or `url(#id)` of one of
 * [ModeSwitchMotion.GRADIENTS]), the stroke width, the opacity, and SVG's line cap and join names.
 */
data class MotionPath(
    val d: String,
    val fill: String?,
    val stroke: String?,
    val strokeWidth: Double,
    val opacity: Double,
    val lineCap: String = "butt",
    val lineJoin: String = "miter",
)

/** A gradient stop: [offset] 0..1, a `#RRGGBB` [color] and its [opacity]. */
data class GradientStop(val offset: Double, val color: String, val opacity: Double)

/** A gradient in the shape's bounding box (0..1 both ways). Radial ones are centred, radius half the box. */
data class MotionGradient(
    val id: String,
    val radial: Boolean,
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
    val stops: List<GradientStop>,
)

/**
 * A vein of light: path data in screen units, its [layer] (drawing order glow, core, hot, then the nodes, then
 * pulse), its colour and width. It is drawn from its start to [ModeSwitchMotion.veinProgress] of its length;
 * [d0] is how far the vein front has run when it starts, [len] its length in the front's units. [swell] veins
 * widen with the glow ([PumpFrame.veinWidthScale]); a pulse is a bead (6% of the path) riding the front.
 * Every vein and pulse is stroked with round caps and round joins, and no fill.
 */
data class VeinPath(
    val d: String,
    val layer: String,
    val color: String,
    val width: Double,
    val d0: Double,
    val len: Double,
    val swell: Boolean,
) {
    companion object {
        const val LAYER_GLOW = "glow"
        const val LAYER_CORE = "core"
        const val LAYER_HOT = "hot"
        const val LAYER_PULSE = "pulse"

        fun colorOf(layer: String): String = when (layer) {
            LAYER_GLOW -> ModeSwitchMotion.Colors.VEIN_GLOW
            LAYER_CORE -> ModeSwitchMotion.Colors.VEIN_CORE
            LAYER_HOT -> ModeSwitchMotion.Colors.VEIN_HOT
            else -> ModeSwitchMotion.Colors.VEIN_PULSE
        }
    }
}

/** A bright node where a branch leaves its vein: shown once the front passes [d0] + 6, radius [r] x the node scale. */
data class VeinNode(val x: Double, val y: Double, val r: Double, val d0: Double)

/**
 * A chalk mote or a spark: it starts at ([ox], [oy]) and travels ([dx], [dy]) as the frame's ease runs to 1;
 * [r0] the dust's starting radius, [life] ms after impact, [alpha] its starting opacity.
 */
data class Particle(val ox: Double, val oy: Double, val dx: Double, val dy: Double, val r0: Double, val life: Double, val alpha: Double)

/** The hammer's transform: translate([x], [y]), rotate([rotation] degrees), scale([scaleX], [scaleY]); the hammer scale is in. */
data class Pose(val x: Double, val y: Double, val rotation: Double, val scaleX: Double, val scaleY: Double)

/** One of the three motion-smear copies behind the falling hammer. */
data class Ghost(val opacity: Double, val pose: Pose)

/** A circle centred on the hit or the switch; [strokeWidth] 0 for the filled halos. */
data class MotionCircle(val opacity: Double, val radius: Double, val strokeWidth: Double)

data class DustState(val cx: Double, val cy: Double, val r: Double, val opacity: Double)

data class SparkState(val x1: Double, val y1: Double, val x2: Double, val y2: Double, val opacity: Double)

/**
 * Everything of the hammer drop at one moment. Positions are in screen units; the hit is [hitX], [hitY].
 *
 * Drawing order, back to front: shadow, cracks, flash, [ring1], [ring2], dust, burst, ghosts, hammer (with the
 * rune glow over it), sparks.
 *
 * - [revealRadius]: the new side shows inside this circle round the hit; 0 before impact. With [revealFull] the
 *   new side shows everywhere.
 * - Shake: the content is translated ([shakeX], [shakeY]), rotated [shakeRotation] degrees and scaled [shakeScale]
 *   round its centre; [deviceShakeY] is the prototype's extra drop of the whole phone, for whichever layer stands
 *   in for it.
 * - [crackDrawn]: how much of each crack is drawn, 0..1 from its start; the cracks layer fades with [cracksOpacity].
 * - [stampScale] and [stampOpacity]: the POWERLIFTING stamp.
 * - [dust] and [sparks] follow [ModeSwitchMotion.dust] and [ModeSwitchMotion.sparks], index for index.
 */
data class DropFrame(
    val hitX: Double,
    val hitY: Double,
    val shadowOpacity: Double,
    val shadowRx: Double,
    val shadowRy: Double,
    val shadowCx: Double,
    val shadowCy: Double,
    val hammer: Pose?,
    val ghosts: List<Ghost>,
    val runeOpacity: Double,
    val revealRadius: Double,
    val revealFull: Boolean,
    val shakeX: Double,
    val shakeY: Double,
    val shakeRotation: Double,
    val shakeScale: Double,
    val deviceShakeY: Double,
    val stampScale: Double,
    val stampOpacity: Double,
    val flashOpacity: Double,
    val burstOpacity: Double,
    val burstScale: Double,
    val ring1: MotionCircle,
    val ring2: MotionCircle,
    val cracksOpacity: Double,
    val crackDrawn: Double,
    val dust: List<DustState>,
    val sparks: List<SparkState>,
)

/**
 * Everything of the pump at one moment, centred on the switch at ([srcX], [srcY]).
 *
 * Drawing order, back to front: tint (the whole screen), halos, rings, the flood's soft edge, its edge, then the
 * veins layer ([veinsOpacity]): glow veins (at [glowLayerOpacity]), core, hot, nodes, pulses.
 *
 * - [contentScale] scales the content round its centre and [deviceScale] the prototype's phone; each element also
 *   swells on its own with [ModeSwitchMotion.pumpElementScale].
 * - [veinFront]: how far the veins have run (see [ModeSwitchMotion.veinProgress]).
 * - Nodes past the front show at [nodeOpacity], radius x [nodeScale]. Pulses show at [pulseOpacity]
 *   ([ModeSwitchMotion.pulseOpacity] per vein).
 * - [floodRadius]: the new side shows inside this circle round the switch; 0 before the third beat. With
 *   [floodFull] the new side shows everywhere. The edge rings sit on the same radius.
 * - [pillScale]: the BODYBUILDING label swelling in.
 */
data class PumpFrame(
    val srcX: Double,
    val srcY: Double,
    val swell: Double,
    val contentScale: Double,
    val deviceScale: Double,
    val glow: Double,
    val tintOpacity: Double,
    val halos: List<MotionCircle>,
    val rings: List<MotionCircle>,
    val veinsOpacity: Double,
    val glowLayerOpacity: Double,
    val veinFront: Double,
    val veinWidthScale: Double,
    val nodeOpacity: Double,
    val nodeScale: Double,
    val pulseOpacity: Double,
    val floodRadius: Double,
    val floodFull: Boolean,
    val edgeOpacity: Double,
    val edgeWidth: Double,
    val edgeSoftOpacity: Double,
    val edgeSoftWidth: Double,
    val pillScale: Double,
)

/**
 * The mode switch animations: the hammer drop (into powerlifting) and the pump (into bodybuilding), as pure
 * functions of time. A port of the approved prototype `docs/design/motion/mode-switch-sound.html`: its timeline,
 * its seeded shapes and its `render`, `renderPump`, `fallPose`, `hammerPose`, `beatEnv` and `veinFront`.
 *
 * **Geometry.** Sizes are the prototype's, in screen units (its screen is 390 x 760). The hammer lands on the
 * bottom edge, centred: the hit is (w / 2, h - 4). The radii that must cover the screen (the reveal, the shock
 * rings, the flood) and the veins' reach scale by the distance to the screen's far corner over the prototype's
 * same distance, which is exactly 1 on the prototype's screen. The pump's vein fan is aimed from the switch to
 * the screen's centre (the prototype aimed it at 228 degrees). The hammer's lift out of frame grows with a screen
 * taller than the prototype's, so it always leaves the top.
 *
 * Nothing is worked out until it is first used, and the seeded shapes are built once per hit point or switch.
 */
object ModeSwitchMotion {
    val DROP = DropTimeline(fall = 115.0, impact = 250.0, hold = 330.0, settle = 520.0, lift = 640.0, end = 1120.0, hammerScale = 0.42)

    val PUMP = PumpTimeline(
        beats = listOf(90.0, 400.0, 750.0),
        amps = listOf(.36, .62, 1.0),
        rise = listOf(110.0, 120.0, 180.0),
        fall = listOf(190.0, 210.0, 370.0),
        reach = listOf(175.0, 390.0, 1000.0),
        surge = listOf(200.0, 230.0, 420.0),
        flood = 380.0,
        end = 1330.0,
    )

    /** The drop's vibration, started at impact: on, off, on, ... in ms (the web's `navigator.vibrate` pattern). */
    val DROP_VIBE: LongArray get() = longArrayOf(420, 35, 180, 45, 110, 60, 60, 80, 30)

    /** Amplitude (0..255) per [DROP_VIBE] slot: the slam at full strength, the aftershocks stepping down, 0 when off. */
    val DROP_AMPLITUDES: IntArray get() = intArrayOf(255, 0, 200, 0, 150, 0, 100, 0, 60)

    /** The pump's three heartbeats in ms: three separate pulses, `PUMP_VIBE[i]` started at `PUMP.beats[i]`. Not an on/off pattern. */
    val PUMP_VIBE: LongArray get() = longArrayOf(28, 55, 120)

    /** Amplitude (0..255) per heartbeat, building to full strength. */
    val PUMP_AMPLITUDES: IntArray get() = intArrayOf(150, 205, 255)

    /** The hammer's shapes, back to front, with the striking boss at (0, 0) and the haft up. Place it with a [Pose]. */
    val HAMMER_PATHS: List<MotionPath> get() = HAMMER

    /** The rune's warm glow, drawn over the hammer at [DropFrame.runeOpacity]. */
    val HAMMER_RUNE: MotionPath get() = HAMMER_RUNE_GLOW

    /** The gradients the shapes name with `url(#id)`: gIron, gBand, gWood (hammer), gHalo (pump halos), gChalk (dust). */
    val GRADIENTS: List<MotionGradient> get() = MOTION_GRADIENTS

    /** The effects' colours, the prototype's. The screens themselves use the app's own tokens. */
    object Colors {
        const val SHADOW = "#000000"
        const val CRACK_SHADOW = "#000000"
        const val CRACK = "#F3F4EF"
        const val CRACK_BRANCH = "#C9CED6"
        const val FLASH = "#F3F4EF"
        /** The light scheme's flash: a dark punch, drawn at half [DropFrame.flashOpacity]. */
        const val FLASH_LIGHT = "#2A2E33"
        const val RING1 = "#F3F4EF"
        const val RING2 = "#93979D"
        const val BURST = "#FFF6E8"
        const val SPARK = "#FFB36B"
        const val TINT = "#4F8DFF"
        const val PUMP_RING = "#8DB8FF"
        const val EDGE = "#DCE9FF"
        const val EDGE_SOFT = "#8DB8FF"
        const val VEIN_GLOW = "#4F8DFF"
        const val VEIN_CORE = "#8DB8FF"
        const val VEIN_HOT = "#EAF2FF"
        const val VEIN_PULSE = "#FFFFFF"
        const val NODE = "#DCE9FF"
    }

    const val SHADOW_OPACITY_MAX = .5
    const val SPARK_WIDTH = 2.4
    const val PUMP_RING_FILL = "none"

    // ---- the prototype's geometry, for the scale rule ----
    private const val REF_W = 390.0
    private const val REF_H = 760.0
    private val REF_HIT = Point(195.0, 756.0)
    private val REF_SRC = Point(341.0, 715.0)
    internal const val REF_FAN_CENTRE = 228.0
    private val REF_HIT_CORNER by lazy { farCorner(REF_W, REF_H, REF_HIT) }
    private val REF_SRC_CORNER by lazy { farCorner(REF_W, REF_H, REF_SRC) }

    /** Where the hammer lands on a [w] x [h] screen: the bottom edge, centred. */
    fun hitPoint(w: Double, h: Double): Point = Point(w / 2, h - 4)

    /** How much the covering radii grow on this screen: 1 on the prototype's. */
    internal fun dropReach(w: Double, h: Double): Double = farCorner(w, h, hitPoint(w, h)) / REF_HIT_CORNER

    internal fun pumpReach(w: Double, h: Double, src: Point): Double = farCorner(w, h, src) / REF_SRC_CORNER

    // ---- easing, as on the page ----
    private fun clamp01(v: Double) = if (v < 0) 0.0 else if (v > 1) 1.0 else v
    private fun easeOut(p: Double) = 1 - (1 - p).pow(3)
    private fun smooth01(p: Double) = p * p * (3 - 2 * p)

    // ---- the drop ----

    /** `fallPose`: [t] in [fall, impact], swung down from the upper right, accelerating hard. */
    fun fallPose(t: Double, hit: Point): Pose {
        val d = DROP
        val p = clamp01((t - d.fall) / (d.impact - d.fall))
        val e = p.pow(3.4)
        return pose(hit.x + 64 * (1 - e), -70 + (hit.y + 70) * e, -32 * (1 - e), 1 - .04 * e, 1 + .1 * e)
    }

    /** `hammerPose`: where the hammer is at [t], or null while it is out of sight. */
    fun hammerPose(t: Double, hit: Point): Pose? {
        val d = DROP
        if (t < d.fall || t >= d.end) return null
        if (t < d.impact) return fallPose(t, hit)
        if (t < d.hold) return pose(hit.x, hit.y + 4, 0.0, 1.14, .8) // hit-stop, squashed into the glass
        if (t < d.settle) {
            val q = (t - d.hold) / (d.settle - d.hold)
            val b = sin(PI * q) * (1 - q * .4)
            return pose(hit.x + 5 * b, hit.y - 24 * b, 6 * b, 1.0, 1.0) // recoil
        }
        if (t < d.lift) return pose(hit.x, hit.y, 0.0, 1.0, 1.0)
        val q = (t - d.lift) / (d.end - 30 - d.lift)
        val e = q * q * q // wrenched back out of frame
        return pose(hit.x + 30 * e, hit.y - 960 * liftReach(hit) * e, -14 * e, 1.0, 1.0)
    }

    /** The lift out of frame grows with a screen taller than the prototype's, so the hammer always leaves the top. */
    private fun liftReach(hit: Point): Double = max(1.0, (hit.y + 4) / REF_H)

    private fun pose(x: Double, y: Double, r: Double, sx: Double, sy: Double) =
        Pose(x, y, r, sx * DROP.hammerScale, sy * DROP.hammerScale)

    /** `render`: the hammer drop at [tMs] on a [w] x [h] screen. At or after the end everything is at rest. */
    fun dropFrame(tMs: Double, w: Double, h: Double): DropFrame {
        val d = DROP
        val t = tMs
        val hit = hitPoint(w, h)
        val k = dropReach(w, h)
        val shapes = dropShapes(hit)
        val tau = t - d.impact
        val g = clamp01(t / d.impact)

        val ghosts = listOf(.32, .2, .1).mapIndexed { i, op ->
            val gt = t - (i + 1) * 13
            if (t >= d.fall && t < d.impact + 20 && gt > d.fall) Ghost(op, fallPose(min(gt, d.impact), hit))
            else Ghost(0.0, fallPose(d.impact, hit))
        }

        val revealRadius: Double
        val revealFull: Boolean
        when {
            tau < 0 -> { revealRadius = 0.0; revealFull = false }
            tau < 170 -> { revealRadius = 30 + 790 * k * easeOut(tau / 170); revealFull = false }
            else -> { revealRadius = 30 + 790 * k; revealFull = true }
        }

        var shakeX = 0.0
        var shakeY = 0.0
        var shakeR = 0.0
        var shakeS = 1.0
        var deviceY = 0.0
        if (tau >= 0 && tau < 620) {
            val dk = exp(-tau / 165)
            shakeY = 36 * dk * cos(tau * .098)
            shakeX = 18 * dk * sin(tau * .151 + .6)
            shakeR = 2.4 * dk * sin(tau * .12 + 1)
            shakeS = 1 + .07 * dk
            deviceY = 12 * dk * cos(tau * .098)
        }

        val bp = clamp01(tau / 170)
        fun ring(dl: Double, dur: Double, sw: Double): MotionCircle {
            val p = clamp01((tau - dl) / dur)
            val on = tau >= dl && p < 1
            return MotionCircle(if (on) .9 * (1 - p) else 0.0, 20 + 780 * k * easeOut(p), sw * (1 - p) + .5)
        }
        val cp = clamp01(tau / 85)

        val dust = shapes.dust.map { p ->
            if (tau < 0 || tau > p.life) DustState(p.ox, p.oy, 0.0, 0.0)
            else {
                val s = 1 - exp(-tau / 190)
                DustState(p.ox + p.dx * s, p.oy + p.dy * s + .00016 * tau * tau, p.r0 * (1 + 2.4 * s), p.alpha * (1 - tau / p.life))
            }
        }
        val sparks = shapes.sparks.map { p ->
            if (tau < 0 || tau > p.life) SparkState(p.ox, p.oy, p.ox, p.oy, 0.0)
            else {
                val s = 1 - exp(-tau / 110)
                val s0 = max(0.0, s - .16)
                val gy = .0009 * tau * tau
                SparkState(p.ox + p.dx * s0, p.oy + p.dy * s0 + gy, p.ox + p.dx * s, p.oy + p.dy * s + gy, 1 - tau / p.life)
            }
        }
        val atRest = t >= d.end

        return DropFrame(
            hitX = hit.x,
            hitY = hit.y,
            shadowOpacity = if (tau < 0) SHADOW_OPACITY_MAX * g * g else 0.0,
            shadowRx = 36 + 66 * g,
            shadowRy = 9 + 22 * g,
            shadowCx = hit.x,
            shadowCy = hit.y - 4,
            hammer = hammerPose(t, hit),
            ghosts = ghosts,
            runeOpacity = if (tau < 0) 0.0 else clamp01(1 - tau / 620),
            revealRadius = revealRadius,
            revealFull = revealFull || atRest,
            shakeX = shakeX,
            shakeY = shakeY,
            shakeRotation = shakeR,
            shakeScale = shakeS,
            deviceShakeY = deviceY,
            stampScale = if (tau >= 0 && tau < 420) 1 + .7 * exp(-tau / 80) else 1.0,
            stampOpacity = stampOpacity(t),
            flashOpacity = if (tau < 0) 0.0 else clamp01(1 - tau / 150),
            burstOpacity = if (tau < 0) 0.0 else 1 - bp,
            burstScale = .5 + 1.5 * easeOut(bp),
            ring1 = ring(0.0, 340.0, 26.0),
            ring2 = ring(60.0, 460.0, 14.0),
            cracksOpacity = if (tau < 0) 0.0 else clamp01(1 - (tau - 380) / 420),
            crackDrawn = easeOut(cp),
            dust = dust,
            sparks = sparks,
        )
    }

    /** The POWERLIFTING stamp: it appears at impact, holds until the lift, and is gone by the end. */
    private fun stampOpacity(t: Double): Double {
        val d = DROP
        return when {
            t < d.impact || t >= d.end -> 0.0
            t < d.lift -> 1.0
            else -> 1 - (t - d.lift) / (d.end - d.lift)
        }
    }

    /**
     * The hop of a row, card or bar whose centre is at [elementCenterY], as the shock passes under it: a
     * translateY in screen units at [tMs] (0 outside the hop). [hitY] is [DropFrame.hitY].
     */
    fun hop(tMs: Double, elementCenterY: Double, hitY: Double): Double {
        val dist = abs(elementCenterY - hitY)
        val delay = dist * .22
        val amp = 24 - min(10.0, dist * .015)
        val u = tMs - DROP.impact - delay
        return if (u > 0 && u < 560) -amp * exp(-u / 120) * sin(u * .052) else 0.0
    }

    // ---- the pump ----

    /** `beatEnv`: one beat, [u] ms from its onset: swell up, relax a touch past rest, settle. */
    fun beatEnv(u: Double, rise: Double, fall: Double): Double {
        if (u <= 0 || u >= rise + fall) return 0.0
        if (u < rise) return .5 - .5 * cos(PI * u / rise)
        val v = (u - rise) / fall
        val dip = .12
        if (v < .68) return -dip + (1 + dip) * (.5 + .5 * cos(PI * v / .68))
        return -dip * (.5 + .5 * cos(PI * (v - .68) / .32))
    }

    /** `pumpSwell`: the three beats summed, each at its amplitude. */
    fun pumpSwell(t: Double): Double {
        val p = PUMP
        var s = 0.0
        for (i in 0 until 3) s += p.amps[i] * beatEnv(t - p.beats[i], p.rise[i], p.fall[i])
        return s
    }

    /** `veinFront` on the prototype's screen: surge on each beat, ebb a little between them. Scale it by the reach. */
    fun veinFront(t: Double): Double {
        val p = PUMP
        var f = 0.0
        for (i in 0 until 3) {
            val u = t - p.beats[i]
            if (u <= 0) break
            val from = if (i > 0) p.reach[i - 1] * .93 else 0.0
            val q = clamp01(u / p.surge[i])
            f = from + (p.reach[i] - from) * (if (i < 2) easeOut(q) else smooth01(q))
            if (q >= 1 && i < 2) {
                f = p.reach[i] * (1 - .07 * smooth01(clamp01((u - p.surge[i]) / (p.beats[i + 1] - p.beats[i] - p.surge[i]))))
            }
        }
        return f
    }

    /** `renderPump`: the pump at [tMs] on a [w] x [h] screen, from the switch at [src]. At or after the end it is at rest. */
    fun pumpFrame(tMs: Double, w: Double, h: Double, src: Point): PumpFrame {
        val p = PUMP
        val t = tMs
        val k = pumpReach(w, h, src)
        if (t >= p.end) {
            val none = MotionCircle(0.0, 0.0, 0.0)
            return PumpFrame(
                src.x, src.y, 0.0, 1.0, 1.0, 0.0, 0.0, List(3) { none }, List(2) { none }, 0.0, 0.0, veinFront(t) * k, 1.0,
                0.0, 1.0, 0.0, 12 + 860 * k, true, 0.0, 2.0, 0.0, 50.0, 1.0,
            )
        }
        val sw = pumpSwell(t)
        var glow = 0.0
        for (i in 0 until 3) {
            val u = t - p.beats[i]
            if (u > 0) glow = max(glow, p.amps[i] * exp(-u / 190))
        }
        val hr = listOf(70.0, 120.0, 210.0)
        val halos = (0 until 3).map { i ->
            val u = t - p.beats[i]
            val q = clamp01(u / 440)
            val on = u >= 0 && q < 1
            MotionCircle(if (on) .9 * sqrt(p.amps[i]) * (1 - q) else 0.0, 26 + hr[i] * easeOut(q), 0.0)
        }
        val rings = (0 until 2).map { i ->
            val u = t - p.beats[i]
            val q = clamp01(u / 460)
            val on = u >= 0 && q < 1
            MotionCircle(if (on) .32 * (1 - q) else 0.0, 20 + p.reach[i] * k * 1.05 * easeOut(q), 12 * (1 - q) + 1)
        }
        val front = veinFront(t) * k
        var bi = -1
        for (i in 0 until 3) if (t > p.beats[i]) bi = i
        val pp = if (bi < 0) 1.0 else clamp01((t - p.beats[bi]) / p.surge[bi])

        val u3 = t - p.beats[2]
        val fp = clamp01(u3 / p.flood)
        val rad = 12 + 860 * k * smooth01(fp)
        val edgeOn = u3 >= 0 && fp < 1
        return PumpFrame(
            srcX = src.x,
            srcY = src.y,
            swell = sw,
            contentScale = 1 + .03 * sw,
            deviceScale = 1 + .032 * sw,
            glow = glow,
            tintOpacity = .15 * glow,
            halos = halos,
            rings = rings,
            veinsOpacity = if (t < p.beats[0]) 0.0 else 1 - clamp01((t - (p.beats[2] + 280)) / 300),
            glowLayerOpacity = .16 + .42 * glow,
            veinFront = front,
            veinWidthScale = 1 + .45 * glow,
            nodeOpacity = .55 + .45 * glow,
            nodeScale = 1 + .7 * glow,
            pulseOpacity = if (pp >= 1) 0.0 else .95 * sin(PI * pp),
            floodRadius = if (u3 < 0) 0.0 else rad,
            floodFull = u3 >= 0 && fp >= 1,
            edgeOpacity = if (edgeOn) .5 * (1 - fp * fp) else 0.0,
            edgeWidth = 2 + 6 * (1 - fp),
            edgeSoftOpacity = if (edgeOn) .2 * (1 - fp) else 0.0,
            edgeSoftWidth = 50.0,
            pillScale = 1 + .28 * max(0.0, beatEnv(u3 - 230, 120.0, 260.0)),
        )
    }

    /**
     * The swell of a card or row whose centre is at [elementCenterY], nearest the switch first: a scale round its
     * own centre at [tMs] (1 at rest).
     */
    fun pumpElementScale(tMs: Double, w: Double, elementCenterY: Double, src: Point): Double {
        if (tMs >= PUMP.end) return 1.0
        val delay = hypot(w / 2 - src.x, elementCenterY - src.y) * .16
        return 1 + .034 * pumpSwell(tMs - delay)
    }

    /** How much of a vein starting at [d0], [len] long, is drawn at [front]: 0..1 from its start, hidden at 0. */
    fun veinProgress(front: Double, d0: Double, len: Double): Double = clamp01((front - d0) / len)

    fun veinProgress(front: Double, vein: VeinPath): Double = veinProgress(front, vein.d0, vein.len)

    /** Whether [node] shows at [front]. */
    fun nodeShown(front: Double, node: VeinNode): Boolean = front > node.d0 + 6

    /** A pulse's opacity on a [len] long vein: [PumpFrame.pulseOpacity], or 0 once [front] has run past its end. */
    fun pulseOpacity(framePulseOpacity: Double, front: Double, len: Double): Double =
        if (front / len > 1.02) 0.0 else framePulseOpacity

    fun pulseOpacity(frame: PumpFrame, pulse: VeinPath): Double = pulseOpacity(frame.pulseOpacity, frame.veinFront, pulse.len)

    /** Where a pulse's bead starts along a [len] long vein at [front], 0..1 of its length: the bead is the next 6%. */
    fun pulseStart(front: Double, len: Double): Double = max(0.0, front / len - .06)

    fun pulseStart(frame: PumpFrame, pulse: VeinPath): Double = pulseStart(frame.veinFront, pulse.len)

    // ---- the seeded shapes ----

    private var dropCache: Pair<Point, DropShapes>? = null
    private var pumpCache: Pair<List<Double>, PumpShapes>? = null

    private fun dropShapes(hit: Point): DropShapes {
        dropCache?.let { (p, s) -> if (p == hit) return s }
        return buildDropShapes(hit).also { dropCache = hit to it }
    }

    internal fun pumpShapes(w: Double, h: Double, src: Point, fanCentreDeg: Double, reach: Double): PumpShapes {
        val key = listOf(w, h, src.x, src.y, fanCentreDeg, reach)
        pumpCache?.let { (k, s) -> if (k == key) return s }
        return buildPumpShapes(w, h, src, fanCentreDeg, reach).also { pumpCache = key to it }
    }

    /** The fan's centre in degrees (0 right, 90 down): from the switch to the screen's centre. */
    fun veinAim(w: Double, h: Double, src: Point): Double = atan2(h / 2 - src.y, w / 2 - src.x) * 180 / PI

    /** The cracks round [hit], seeded as on the page: for each crack a dark edge, the crack, and every other one a branch. */
    fun cracks(w: Double, h: Double, hit: Point): List<MotionPath> = dropShapes(hit).cracks

    /** The chalk dust, seeded as on the page, filled with gChalk. */
    fun dust(hit: Point): List<Particle> = dropShapes(hit).dust

    /** The sparks, seeded as on the page: lines in [Colors.SPARK], [SPARK_WIDTH] wide, round caps. */
    fun sparks(hit: Point): List<Particle> = dropShapes(hit).sparks

    /** The starburst's points round the hit, before [DropFrame.burstScale]. */
    fun burst(hit: Point): List<Point> = dropShapes(hit).burst

    /** The veins from the switch at [src], aimed at the screen's centre and reaching its edges, in drawing order. */
    fun veins(w: Double, h: Double, src: Point): List<VeinPath> =
        pumpShapes(w, h, src, veinAim(w, h, src), pumpReach(w, h, src)).veins

    /** The veins' branch nodes, drawn between the hot veins and the pulses. */
    fun veinNodes(w: Double, h: Double, src: Point): List<VeinNode> =
        pumpShapes(w, h, src, veinAim(w, h, src), pumpReach(w, h, src)).nodes
}
