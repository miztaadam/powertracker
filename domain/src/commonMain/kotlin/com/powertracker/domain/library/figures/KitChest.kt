package com.powertracker.domain.library.figures

import kotlin.math.max
import kotlin.math.min

// Kit pieces the chest batch needs (Task 6), at real-world scale in centimetres. Sizes are typical, manufacturer-
// neutral values, each noted on its piece. Kit.kt's surfaces, solids and drawing hand these pieces to the functions
// below.

/**
 * A decline bench: a pad whose top runs down from its high (hip) end at [x], [topHeight] above the floor, for
 * [length] at [angle]° below level towards the head (to the left), with a leg brace in front of the high end: a
 * roller centred at [brace] that the lower shins hook under. A typical decline bench: the pad about 100 cm long and
 * 25° down (a typical decline bench is 15–30°), its high end about 68 cm up, a 12 cm roller; neutral sizes.
 */
data class DeclineBench(
    val x: Double,
    val brace: Point,
    val angle: Double = 25.0,
    val topHeight: Double = 68.0,
    val length: Double = 100.0,
) : FixedProp {
    /** The pad's top line: from its high end towards the head. */
    internal val down: Point get() = dir(180.0 + angle)
    internal val top: Point get() = Point(x, -topHeight)

    companion object {
        const val ROLLER_RADIUS = 6.0
    }
}

/**
 * A Smith machine seen from the side: the bar runs on vertical rails at [x] (the two rails, one at each end of the
 * bar, are one line from the side), [height] tall, with the safety stops' tops [stopHeight] above the floor. A typical
 * Smith machine: rails about 215 cm tall, stops that clamp to the frame; neutral sizes. The bar itself is the held
 * [Barbell]; [BarOnRails] checks it sits on the rails. [stopBehind]: how far the stop reaches behind the rails, away
 * from the lifter (Task 12: 6 cm, as before, unless a figure needs the stop to show past a back under the bar).
 */
data class SmithMachine(val x: Double, val stopHeight: Double, val height: Double = 215.0, val stopBehind: Double = 6.0) : FixedProp

/**
 * Cable handles (D-handles), one in each hand, each on its own cable from a [CableStack]'s pulley. In the side view
 * the cable runs to the near hand; in the front view to each hand from its own column.
 */
data object CableHandles : HeldProp

/**
 * A seated press machine (chest press, incline or decline press) whose seat's front edge is at [x], its top
 * [seatHeight] up, with a back pad [BACK_LENGTH] long hinged at the seat's rear, [backAngle]° from level. The handle
 * is on a lever that swings about [pivot] (on the machine's frame above the lifter) and follows the hands. A typical
 * selectorised chest press: a 40 cm seat, a 70 cm back pad, a lever about 70–90 cm long; neutral sizes. The figure sets
 * the pivot so the lever is the same length in both panels. With [handleEndOn] (Task 8: a shoulder press gripped palms
 * forward) the handle runs across the body, so the side view sees its end, a disc; the default handle is upright.
 */
data class PressMachine(val x: Double, val seatHeight: Double, val backAngle: Double, val pivot: Point, val handleEndOn: Boolean = false) : FixedProp {
    companion object {
        const val SEAT_LENGTH = 40.0
        const val BACK_LENGTH = 70.0
    }
}

/**
 * A pec deck seen from the front, centred at [x]: a seat [seatHeight] up, a back pad behind the lifter up to
 * [padTop], and a frame beam at [pivotHeight] with each arm's pivot [pivotHalf] cm either side of the middle, above the
 * shoulders. Each arm hangs from its pivot to a vertical handle in the hand. A typical machine, sizes neutral: the seat
 * 40 cm wide, the pad 34 cm wide. With [facingPad] (Task 8) the lifter sits the other way round, chest to the pad (a
 * reverse fly): the pad, 28 cm wide and 36 cm tall from [padTop] down, is in front of the torso and drawn over it
 * (KitShoulders.kt), and the handles, gripped palms down, run across (drawn level). The default drawing is unchanged.
 */
data class PecDeck(
    val x: Double,
    val seatHeight: Double,
    val padTop: Double,
    val pivotHeight: Double,
    val pivotHalf: Double = 18.0,
    val facingPad: Boolean = false,
) : FixedProp

/**
 * A seat seen from the front, centred at [x], its top [height] up and [width] across, on one post (a cable station's
 * bench seat, the pec deck's). A typical 40 cm seat; neutral. The seated lifter's legs are in front of it, so it is no
 * obstacle in the front view; the hips rest on its top.
 */
data class Seat(val x: Double, val height: Double, val width: Double = 40.0) : FixedProp

/**
 * Parallel dip bars seen from the side, centred at [x], [height] (their centre) above the floor, [length] long front to
 * back. The near and far bars are one line from the side. A typical dip station: bars about 120 cm up and 50 cm long,
 * 4 cm thick; neutral sizes.
 */
data class DipBars(val x: Double, val height: Double = 120.0, val length: Double = 50.0) : FixedProp {
    companion object {
        const val THICKNESS = 4.0
    }
}

/**
 * An assisted dip machine seen from the side: its column at [x] behind the lifter, dip handles reaching forward from
 * it at [handleHeight] to [handleEnd], and a step lever under the feet, its top [stepHeight] up and centred at
 * [stepX], on a lever from [pivot] on the column. The assist weight pushes the step up. A typical machine: a 220 cm
 * column, handles about 120 cm up, a 30 cm step; neutral sizes. The figure sets the pivot so the lever keeps its length.
 */
data class AssistedDipMachine(
    val x: Double,
    val handleHeight: Double,
    val handleEnd: Double,
    val stepX: Double,
    val stepHeight: Double,
    val pivot: Point,
) : FixedProp {
    companion object {
        const val STEP = 30.0
    }
}

/**
 * Weight plates lying flat on the upper back (a weighted push-up): [count] 45 cm plates, each [THICKNESS] thick,
 * stacked on the back between the shoulder blades. Typical 20 kg plates are about 4–5 cm thick at the hub and thinner
 * at the rim; 3 cm is drawn, neutral.
 */
data class BackPlates(val count: Int = 2) : HeldProp {
    companion object {
        const val THICKNESS = 3.0

        /** Where the stack sits along the torso from the pelvis, as a fraction of its length. */
        const val ALONG = 0.62
    }
}

// ---- geometry (internal) ----

private val E = Role.EQUIPMENT

private fun cap(role: Role, a: Point, b: Point, width: Double) = Capsule(role, a.x, a.y, b.x, b.y, width)

private fun slabPolygon(role: Role, s: Slab): Polygon = Polygon(role, s.corners().flatMap { listOf(it.x, it.y) })

internal fun DeclineBench.padSlab(): Slab {
    val c = top + down * (length / 2) + dir(270.0 + angle) * (FlatBench.PAD / 2)
    return Slab(c.x, c.y, length, FlatBench.PAD, 180.0 + angle)
}

/** The decline bench's two legs: under the high end and near the low end. */
private fun DeclineBench.legs(): List<Pair<Point, Point>> {
    val high = top + Point(-10.0, FlatBench.PAD)
    val lowTop = top + down * (length - 12.0) + dir(270.0 + angle) * FlatBench.PAD
    return listOf(high to Point(high.x, 0.0), lowTop to Point(lowTop.x, 0.0))
}

internal fun DeclineBench.surfacesOf(): Map<Surface, List<Slab>> = mapOf(
    Surface.BENCH to listOf(padSlab()),
    Surface.LEG_BRACE to listOf(Slab(brace.x, brace.y, 0.0, 0.0, radius = DeclineBench.ROLLER_RADIUS)),
)

internal fun DeclineBench.solidsOf(): List<Solid> =
    (listOf(padSlab()) + legs().map { (a, b) -> Slab.box(a.x - 2.5, a.y, a.x + 2.5, b.y) })
        .map { Solid("bench", it, stopsArms = false) } +
        Solid("leg brace", Slab(brace.x, brace.y, 0.0, 0.0, radius = DeclineBench.ROLLER_RADIUS), stopsArms = false)

internal fun DeclineBench.drawOf(): List<Placed> = buildList {
    for ((a, b) in legs()) {
        add(Placed(cap(E, a, Point(b.x, -1.5), 5.0)))
        add(Placed(Capsule(E, b.x - 12.0, -1.5, b.x + 12.0, -1.5, 3.0)))
    }
    // The brace's stem from the frame under the high end to the roller.
    add(Placed(cap(E, top + Point(-4.0, FlatBench.PAD + 4.0), brace, 4.0)))
    add(Placed(slabPolygon(E, padSlab())))
    add(Placed(Circle(E, brace.x, brace.y, DeclineBench.ROLLER_RADIUS)))
}

internal fun SmithMachine.stopSlab(): Slab = Slab.box(x - stopBehind, -stopHeight, x + 6.0, -stopHeight + 5.0)

internal fun SmithMachine.drawOf(): List<Placed> = listOf(
    Placed(Capsule(E, x, -1.5, x, -height, 4.0), fit = false),
    Placed(Capsule(E, x - 25.0, -1.5, x + 25.0, -1.5, 3.0)),
    Placed(RoundRect(E, x - stopBehind, -stopHeight, stopBehind + 6.0, 5.0, 1.5)),
)

/** The press machine's seat and back pad. */
internal fun PressMachine.parts(): Pair<Slab, Slab> {
    val pad = FlatBench.PAD
    val seat = Slab.box(x - PressMachine.SEAT_LENGTH, -seatHeight, x, -seatHeight + pad)
    val hinge = pressHinge()
    val along = 180.0 - backAngle
    val c = hinge + dir(along) * (PressMachine.BACK_LENGTH / 2) + dir(along + 90.0) * (pad / 2)
    return seat to Slab(c.x, c.y, PressMachine.BACK_LENGTH, pad, along)
}

/** Where the press machine's back pad meets the seat's top. */
internal fun PressMachine.pressHinge() = Point(x - PressMachine.SEAT_LENGTH, -seatHeight)

internal fun PressMachine.drawOf(grip: Point?): List<Placed> = buildList {
    val (seat, back) = parts()
    val mid = x - PressMachine.SEAT_LENGTH / 2
    val postX = pressHinge().x - 22.0
    add(Placed(Capsule(E, mid, -seatHeight + FlatBench.PAD, mid, -1.5, 5.0)))
    add(Placed(Capsule(E, postX - 15.0, -1.5, x + 10.0, -1.5, 3.0)))
    // The frame: a post behind the back pad up to a beam that carries the lever's pivot.
    add(Placed(Capsule(E, postX, -1.5, postX, pivot.y, 6.0)))
    add(Placed(Capsule(E, postX, pivot.y, pivot.x, pivot.y, 5.0)))
    add(Placed(slabShape(E, seat)))
    add(Placed(slabPolygon(E, back)))
    if (grip != null) {
        add(Placed(cap(E, pivot, grip, 4.0)))
        if (handleEndOn) add(Placed(Circle(E, grip.x, grip.y, 2.6)))
        else add(Placed(Capsule(E, grip.x, grip.y - 6.0, grip.x, grip.y + 6.0, 3.5)))
    }
    add(Placed(Circle(E, pivot.x, pivot.y, 4.0)))
}

private fun slabShape(role: Role, s: Slab): Shape = RoundRect(role, s.cx - s.length / 2, s.cy - s.thickness / 2, s.length, s.thickness, 2.0)

internal fun PecDeck.seatSlab(): Slab = Slab.box(x - 20.0, -seatHeight, x + 20.0, -seatHeight + FlatBench.PAD)

internal fun PecDeck.drawOf(near: Point?, far: Point?): List<Placed> = buildList {
    add(Placed(Capsule(E, x, -seatHeight + FlatBench.PAD, x, -1.5, 6.0)))
    add(Placed(Capsule(E, x - 25.0, -1.5, x + 25.0, -1.5, 3.0)))
    add(Placed(slabShape(E, seatSlab())))
    // The back pad behind the torso, and the post that carries the beam above it.
    if (!facingPad) add(Placed(RoundRect(E, x - 17.0, -padTop, 34.0, padTop - seatHeight - 2.0, 3.0)))
    add(Placed(Capsule(E, x, -padTop, x, -pivotHeight, 5.0)))
    add(Placed(Capsule(E, x - pivotHalf - 6.0, -pivotHeight, x + pivotHalf + 6.0, -pivotHeight, 5.0)))
    for ((sgn, grip) in listOf(1.0 to near, -1.0 to far)) {
        val pivot = Point(x + sgn * pivotHalf, -pivotHeight)
        if (grip != null) {
            add(Placed(cap(E, pivot, grip, 4.0)))
            if (facingPad) add(Placed(Capsule(E, grip.x - 6.0, grip.y, grip.x + 6.0, grip.y, 3.5)))
            else add(Placed(Capsule(E, grip.x, grip.y - 7.0, grip.x, grip.y + 7.0, 3.5)))
        }
        add(Placed(Circle(E, pivot.x, pivot.y, 3.5)))
    }
}

internal fun Seat.slab(): Slab = Slab.box(x - width / 2, -height, x + width / 2, -height + FlatBench.PAD)

internal fun Seat.drawOf(): List<Placed> = listOf(
    Placed(Capsule(E, x, -height + FlatBench.PAD, x, -1.5, 6.0)),
    Placed(Capsule(E, x - 25.0, -1.5, x + 25.0, -1.5, 3.0)),
    Placed(slabShape(E, slab())),
)

internal fun DipBars.barSlab(): Slab = Slab(x, -height, length, DipBars.THICKNESS)

internal fun DipBars.drawOf(): List<Placed> = buildList {
    for (end in listOf(x - length / 2 + 3.0, x + length / 2 - 3.0)) {
        add(Placed(Capsule(E, end, -height, end, -1.5, 5.0)))
        add(Placed(Capsule(E, end - 10.0, -1.5, end + 10.0, -1.5, 3.0)))
    }
    add(Placed(Capsule(E, x - length / 2, -height, x + length / 2, -height, DipBars.THICKNESS)))
}

internal fun AssistedDipMachine.handleSlab(): Slab = Slab.box(min(x, handleEnd), -handleHeight - 2.0, max(x, handleEnd), -handleHeight + 2.0)

internal fun AssistedDipMachine.stepSlab(): Slab =
    Slab.box(stepX - AssistedDipMachine.STEP / 2, -stepHeight, stepX + AssistedDipMachine.STEP / 2, -stepHeight + 4.0)

internal fun AssistedDipMachine.drawOf(): List<Placed> = buildList {
    add(Placed(RoundRect(E, x - 9.0, -220.0, 18.0, 218.5, 3.0), fit = false))
    add(Placed(Capsule(E, x - 25.0, -1.5, x + 25.0, -1.5, 3.0)))
    add(Placed(RoundRect(E, x - 7.0, -62.0, 14.0, 50.0, 2.0)))
    add(Placed(Capsule(E, x, -handleHeight, handleEnd, -handleHeight, 4.0)))
    val step = stepSlab()
    add(Placed(cap(E, pivot, Point(step.cx, step.cy + 2.0), 5.0)))
    add(Placed(Circle(E, pivot.x, pivot.y, 4.0)))
    add(Placed(slabShape(E, step)))
}

/** The cable columns: side view one column (Kit.kt), front view two at [CableStack.x] ± [CableStack.halfSpan]. */
internal fun CableStack.drawFrontOf(near: Point?, far: Point?): List<Placed> = buildList {
    // Crossed: each column's cable runs to the hand on the other side.
    val ends = if (crossed) listOf(1.0 to far, -1.0 to near) else listOf(1.0 to near, -1.0 to far)
    for ((sgn, end) in ends) {
        val cx = x + sgn * halfSpan
        val pulley = Point(cx - sgn * reach, -pulleyHeight)
        add(Placed(RoundRect(E, cx - 9.0, -height, 18.0, height - 1.5, 3.0), fit = false))
        add(Placed(Capsule(E, cx, -pulleyHeight, pulley.x, pulley.y, 5.0)))
        add(Placed(Circle(E, pulley.x, pulley.y, 4.0)))
        add(Placed(RoundRect(E, cx - 7.0, -62.0, 14.0, 50.0, 2.0)))
        add(Placed(cap(E, pulley, end ?: (pulley + Point(0.0, 40.0)), 0.8)))
    }
}

/** Where the back plates' stack is centred: on the back of the torso (see [BackPlates]). */
internal fun BackPlates.centerOn(s: Skeleton): Point =
    s.onTorso(Segment.TORSO.length * BackPlates.ALONG, -(Body.TORSO_RADIUS + count * BackPlates.THICKNESS / 2))

internal fun BackPlates.drawOn(s: Skeleton): List<Shape> = buildList {
    for (i in 0 until count) {
        val c = s.onTorso(Segment.TORSO.length * BackPlates.ALONG, -(Body.TORSO_RADIUS + (i + 0.5) * BackPlates.THICKNESS))
        val plate = Slab(c.x, c.y, Barbell.PLATE_DIAMETER, BackPlates.THICKNESS - 0.4, s.torso)
        add(slabPolygon(Role.GAP, plate.copy(length = plate.length + 1.0, thickness = plate.thickness + 0.8)))
        add(slabPolygon(Role.ACCENT, plate))
    }
}

/** The cable handles: a D-handle in each hand (side view: the near hand's). */
internal fun CableHandles.drawOn(s: Skeleton): List<Shape> =
    if (s.view == View.SIDE) {
        listOf(ring(Role.ACCENT, s.near.grip.x, s.near.grip.y, 3.4, 2.2))
    } else {
        Side.entries.map { side -> s.side(side).grip.let { Capsule(Role.ACCENT, it.x, it.y - 5.0, it.x, it.y + 5.0, 2.6) } }
    }
