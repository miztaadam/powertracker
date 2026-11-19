@file:OptIn(ExperimentalJsExport::class)

package com.powertracker.domain.web

import com.powertracker.domain.motion.DropFrame
import com.powertracker.domain.motion.DropTimeline
import com.powertracker.domain.motion.DustState
import com.powertracker.domain.motion.Ghost
import com.powertracker.domain.motion.GradientStop
import com.powertracker.domain.motion.ModeSwitchMotion
import com.powertracker.domain.motion.MotionCircle
import com.powertracker.domain.motion.MotionGradient
import com.powertracker.domain.motion.MotionPath
import com.powertracker.domain.motion.Particle
import com.powertracker.domain.motion.Point
import com.powertracker.domain.motion.Pose
import com.powertracker.domain.motion.PumpFrame
import com.powertracker.domain.motion.PumpTimeline
import com.powertracker.domain.motion.SparkState
import com.powertracker.domain.motion.VeinNode
import com.powertracker.domain.motion.VeinPath

/*
 * The mode switch motion model for the web (ModeSwitchMotion): plain data, the same fields as the domain's types.
 * Times are ms from the tap; positions are CSS px on the screen.
 */

@JsExport
data class JsMotionPoint(val x: Double, val y: Double)

/** The hammer drop's timeline (ms) and the hammer's size. */
@JsExport
data class JsDropTimeline(
    val fall: Double,
    val impact: Double,
    val hold: Double,
    val settle: Double,
    val lift: Double,
    val end: Double,
    val hammerScale: Double,
)

/** The pump's timeline (ms): one entry per beat in each array. */
@JsExport
data class JsPumpTimeline(
    val beats: Array<Double>,
    val amps: Array<Double>,
    val rise: Array<Double>,
    val fall: Array<Double>,
    val reach: Array<Double>,
    val surge: Array<Double>,
    val flood: Double,
    val end: Double,
)

/**
 * The vibrations, for `navigator.vibrate`: [dropPattern] (on, off, on, ... ms) starts at impact; [pumpPulses] are
 * three separate pulses, `pumpPulses[i]` started at `PUMP.beats[i]`. Amplitudes (0..255) match them slot for slot.
 */
@JsExport
data class JsModeSwitchVibration(
    val dropPattern: Array<Int>,
    val dropAmplitudes: Array<Int>,
    val pumpPulses: Array<Int>,
    val pumpAmplitudes: Array<Int>,
)

/** A shape: SVG path data; fill and stroke null for none, else `#RRGGBB` or `url(#id)`; SVG line cap and join names. */
@JsExport
data class JsMotionPath(
    val d: String,
    val fill: String?,
    val stroke: String?,
    val strokeWidth: Double,
    val opacity: Double,
    val lineCap: String,
    val lineJoin: String,
)

@JsExport
data class JsGradientStop(val offset: Double, val color: String, val opacity: Double)

/** A gradient in the shape's bounding box; radial ones are centred, radius half the box. */
@JsExport
data class JsMotionGradient(
    val id: String,
    val radial: Boolean,
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
    val stops: Array<JsGradientStop>,
)

/** A vein of light; [layer] is glow, core, hot or pulse (drawing order, nodes between hot and pulse). */
@JsExport
data class JsVeinPath(
    val d: String,
    val layer: String,
    val color: String,
    val width: Double,
    val d0: Double,
    val len: Double,
    val swell: Boolean,
)

@JsExport
data class JsVeinNode(val x: Double, val y: Double, val r: Double, val d0: Double)

@JsExport
data class JsMotionParticle(val ox: Double, val oy: Double, val dx: Double, val dy: Double, val r0: Double, val life: Double, val alpha: Double)

/** translate(x y) rotate(rotation) scale(scaleX scaleY), the hammer's size included. */
@JsExport
data class JsMotionPose(val x: Double, val y: Double, val rotation: Double, val scaleX: Double, val scaleY: Double)

@JsExport
data class JsMotionGhost(val opacity: Double, val pose: JsMotionPose)

@JsExport
data class JsMotionCircle(val opacity: Double, val radius: Double, val strokeWidth: Double)

@JsExport
data class JsDustState(val cx: Double, val cy: Double, val r: Double, val opacity: Double)

@JsExport
data class JsSparkState(val x1: Double, val y1: Double, val x2: Double, val y2: Double, val opacity: Double)

/** The hammer drop at one moment: see the domain's `DropFrame` for each field and the drawing order. */
@JsExport
data class JsDropFrame(
    val hitX: Double,
    val hitY: Double,
    val shadowOpacity: Double,
    val shadowRx: Double,
    val shadowRy: Double,
    val shadowCx: Double,
    val shadowCy: Double,
    val hammer: JsMotionPose?,
    val ghosts: Array<JsMotionGhost>,
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
    val ring1: JsMotionCircle,
    val ring2: JsMotionCircle,
    val cracksOpacity: Double,
    val crackDrawn: Double,
    val dust: Array<JsDustState>,
    val sparks: Array<JsSparkState>,
)

/** The pump at one moment: see the domain's `PumpFrame` for each field and the drawing order. */
@JsExport
data class JsPumpFrame(
    val srcX: Double,
    val srcY: Double,
    val swell: Double,
    val contentScale: Double,
    val deviceScale: Double,
    val glow: Double,
    val tintOpacity: Double,
    val halos: Array<JsMotionCircle>,
    val rings: Array<JsMotionCircle>,
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

/** The effects' colours (`#RRGGBB`); [flashLight] is the light scheme's flash, drawn at half the flash opacity. */
@JsExport
data class JsModeSwitchColors(
    val shadow: String,
    val crackShadow: String,
    val crack: String,
    val crackBranch: String,
    val flash: String,
    val flashLight: String,
    val ring1: String,
    val ring2: String,
    val burst: String,
    val spark: String,
    val tint: String,
    val pumpRing: String,
    val edge: String,
    val edgeSoft: String,
    val veinGlow: String,
    val veinCore: String,
    val veinHot: String,
    val veinPulse: String,
    val node: String,
)

internal fun Point.toJs() = JsMotionPoint(x, y)
internal fun DropTimeline.toJs() = JsDropTimeline(fall, impact, hold, settle, lift, end, hammerScale)
internal fun PumpTimeline.toJs() = JsPumpTimeline(
    beats.toTypedArray(), amps.toTypedArray(), rise.toTypedArray(), fall.toTypedArray(), reach.toTypedArray(), surge.toTypedArray(), flood, end,
)
internal fun MotionPath.toJs() = JsMotionPath(d, fill, stroke, strokeWidth, opacity, lineCap, lineJoin)
internal fun GradientStop.toJs() = JsGradientStop(offset, color, opacity)
internal fun MotionGradient.toJs() = JsMotionGradient(id, radial, x1, y1, x2, y2, stops.map { it.toJs() }.toTypedArray())
internal fun VeinPath.toJs() = JsVeinPath(d, layer, color, width, d0, len, swell)
internal fun VeinNode.toJs() = JsVeinNode(x, y, r, d0)
internal fun Particle.toJs() = JsMotionParticle(ox, oy, dx, dy, r0, life, alpha)
internal fun Pose.toJs() = JsMotionPose(x, y, rotation, scaleX, scaleY)
internal fun Ghost.toJs() = JsMotionGhost(opacity, pose.toJs())
internal fun MotionCircle.toJs() = JsMotionCircle(opacity, radius, strokeWidth)
internal fun DustState.toJs() = JsDustState(cx, cy, r, opacity)
internal fun SparkState.toJs() = JsSparkState(x1, y1, x2, y2, opacity)

internal fun DropFrame.toJs() = JsDropFrame(
    hitX, hitY, shadowOpacity, shadowRx, shadowRy, shadowCx, shadowCy, hammer?.toJs(), ghosts.map { it.toJs() }.toTypedArray(),
    runeOpacity, revealRadius, revealFull, shakeX, shakeY, shakeRotation, shakeScale, deviceShakeY, stampScale, stampOpacity,
    flashOpacity, burstOpacity, burstScale, ring1.toJs(), ring2.toJs(), cracksOpacity, crackDrawn,
    dust.map { it.toJs() }.toTypedArray(), sparks.map { it.toJs() }.toTypedArray(),
)

internal fun PumpFrame.toJs() = JsPumpFrame(
    srcX, srcY, swell, contentScale, deviceScale, glow, tintOpacity, halos.map { it.toJs() }.toTypedArray(),
    rings.map { it.toJs() }.toTypedArray(), veinsOpacity, glowLayerOpacity, veinFront, veinWidthScale, nodeOpacity, nodeScale,
    pulseOpacity, floodRadius, floodFull, edgeOpacity, edgeWidth, edgeSoftOpacity, edgeSoftWidth, pillScale,
)

internal fun modeSwitchColorsJs() = with(ModeSwitchMotion.Colors) {
    JsModeSwitchColors(
        SHADOW, CRACK_SHADOW, CRACK, CRACK_BRANCH, FLASH, FLASH_LIGHT, RING1, RING2, BURST, SPARK, TINT, PUMP_RING, EDGE, EDGE_SOFT,
        VEIN_GLOW, VEIN_CORE, VEIN_HOT, VEIN_PULSE, NODE,
    )
}
