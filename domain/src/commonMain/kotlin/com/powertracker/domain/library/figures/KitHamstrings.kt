package com.powertracker.domain.library.figures

import kotlin.math.max
import kotlin.math.min

// Kit pieces the hamstring and glute batch needs (Task 11), at real-world scale in centimetres. Sizes are typical,
// manufacturer-neutral values, each noted on its piece. Kit.kt's surfaces, solids and drawing hand these pieces to the
// functions below. The batch also gives the kettlebell a new hold ([KettlebellHold.SWING]) and reuses, unchanged, the
// flat bench (a short length seen end-on, the hip thrust's bench), the 45° back-extension bench (HyperBench, laid
// level as the glute-ham machine), the cable stack and the kettlebell. The old pieces' drawing is unchanged.

/** Where a [MiniBand] sits: just above the knees, or around the ankles. */
enum class BandAt { KNEES, ANKLES }

/**
 * A mini band (a small loop band) around both legs, [at] the knees (just above them, [ABOVE_KNEE] up the thigh) or the
 * ankles ([ABOVE_ANKLE] up the shin): drawn as a strip from one leg to the other, in front of the legs, set off by a gap
 * outline. A typical mini band: a loop about 30 cm across laid flat, about 5 cm wide; drawn [WIDTH] wide, seen edge on.
 */
data class MiniBand(val at: BandAt = BandAt.KNEES) : FixedProp {
    companion object {
        const val WIDTH = 2.5
        const val ABOVE_KNEE = 7.0
        const val ABOVE_ANKLE = 6.0
    }
}

/**
 * A stability ball on the floor, its centre at [x], [diameter] across (a typical 65 cm ball, the size ACE's chart gives
 * for a lifter of 5'8" to 6'3": https://www.acefitness.org/certifiednewsarticle/3235/play-ball/). It rolls between the
 * panels, so a figure sets its [x] per panel.
 */
data class StabilityBall(val x: Double, val diameter: Double = DIAMETER) : FixedProp {
    val radius: Double get() = diameter / 2

    companion object {
        const val DIAMETER = 65.0
    }
}

/**
 * A cable's ankle cuff (a foot harness) on the [side] leg, around the lower shin [ABOVE_ANKLE] above the ankle joint: the
 * cable runs to it. A typical cuff is about 8 cm wide; drawn as a band across the shin, in the accent.
 */
data class AnkleStrap(val side: Side) : HeldProp {
    companion object {
        const val ABOVE_ANKLE = 5.0
    }
}

/**
 * A kneeling station for the Nordic curl: a thick floor pad from [padFrom] to [padTo] (x), [padHeight] tall, under the
 * knees and shins, and a padded roller over the backs of the ankles at [roller], held by a post from a base on the
 * floor. A typical set-up: a 6–8 cm thick exercise pad, an ankle roller about 12 cm across; neutral sizes.
 */
data class NordicStation(val padFrom: Double, val padTo: Double, val padHeight: Double, val roller: Point) : FixedProp {
    companion object {
        /** The roller's radius, cm (a 12 cm roller). */
        const val ROLLER_RADIUS = 6.0
    }
}

/**
 * A reverse hyper bench seen from the side: a high pad whose foot-end edge is at [edge] (the hips just past it), running
 * [length] towards +x (the head), its top [height] above the floor, on two posts; handles at [handle] below the head
 * end. A typical bench: a pad about 45 cm long and 8 cm thick, about 95 cm up, so the straight legs hang clear of the
 * floor; neutral sizes. (Plate-loaded machines add a pendulum under the bench; the bodyweight bench the guide also
 * names has none, so none is drawn.)
 */
data class ReverseHyperBench(val edge: Double, val height: Double, val handle: Point, val length: Double = 45.0) : FixedProp {
    companion object {
        const val PAD = 8.0
    }
}

/**
 * A lying leg curl machine seen from the side: a level pad from the knee-end edge at [kneeEnd] running [length]
 * towards +x (the head), its top [height] up; a lever that turns about [pivot] (lined up with the knee) to a roller pad
 * on the backs of the lower legs at [roller] (in this panel), drawn in front of the legs; handles at [handle] below
 * the head end. A typical machine: a pad about 120 cm long, about 60 cm up, a roller 12 cm across; neutral sizes.
 */
data class LyingLegCurlMachine(
    val kneeEnd: Double,
    val height: Double,
    val pivot: Point,
    val roller: Point,
    val handle: Point,
    val length: Double = 110.0,
) : FixedProp {
    companion object {
        const val PAD = 8.0

        /** The roller pad's radius, cm (a 12 cm roller). */
        const val ROLLER_RADIUS = 6.0
    }
}

/**
 * A seated leg curl machine seen from the side: a seat whose front edge is at [seatFront], its top [seatHeight] up, a
 * back pad hinged at the seat's rear [backAngle]° from level, a lever that turns about [pivot] (lined up with the
 * knee) to a roller under the backs of the lower legs at [roller] (in this panel), and a lap pad on the thighs just
 * above the knees at [lap]. The lever, the roller and the lap pad are beside and over the legs, so they are drawn in
 * front of them. [handle]: the side handle under the near hand. A typical machine: a 40 cm seat about 50 cm up, a
 * 60 cm back pad, rollers about 12 cm across; neutral sizes (the leg extension machine's seat, Task 10).
 */
data class SeatedLegCurlMachine(
    val seatFront: Double,
    val seatHeight: Double,
    val backAngle: Double,
    val pivot: Point,
    val roller: Point,
    val lap: Point,
    val handle: Point,
) : FixedProp {
    companion object {
        const val SEAT_LENGTH = 40.0
        const val BACK_LENGTH = 60.0

        /** The roller and lap pads' radius, cm (12 cm rollers). */
        const val ROLLER_RADIUS = 6.0
    }
}

/**
 * A standing leg curl machine seen from the side: an upright post on a base at [postX], a pad in front of the working
 * thigh just above the knee at [thighPad], a lever that turns about [pivot] (lined up with the knee) to a roller behind
 * the lower leg at [roller] (in this panel), drawn in front of the leg, and handles at [handle] for the hands of the
 * forward lean. A typical machine: a post about 120 cm tall, rollers about 12 cm across; neutral sizes.
 */
data class StandingLegCurlMachine(
    val postX: Double,
    val thighPad: Point,
    val pivot: Point,
    val roller: Point,
    val handle: Point,
) : FixedProp {
    companion object {
        /** The roller and thigh pads' radius, cm (12 cm rollers). */
        const val ROLLER_RADIUS = 6.0
    }
}

/**
 * A plate-loaded hip thrust machine seen from the side: a back pad seen end on, its foot-side edge at [benchEdge] and
 * its top [benchHeight] up (as the hip thrust's bench); a foot platform whose top's centre is at [platform]; and a
 * lever that turns about [pivot] to a padded roller over the front of the hips at [pad] (in this panel; the figure sets
 * the pivot so the lever is the same length in both panels), drawn in front of the hips. A typical machine: a back pad
 * about 30 cm across and 40–45 cm up, a platform about 50 cm long and 5 cm high, a hip pad about 14 cm across; neutral
 * sizes.
 */
data class HipThrustMachine(
    val benchEdge: Double,
    val benchHeight: Double,
    val platform: Point,
    val pivot: Point,
    val pad: Point,
) : FixedProp {
    companion object {
        const val BACK_PAD = 30.0
        const val PLATFORM_LENGTH = 50.0
        const val PLATFORM_THICKNESS = 5.0

        /** The hip pad's radius, cm (a 14 cm roller). */
        const val PAD_RADIUS = 7.0
    }
}

/**
 * A seated hip abduction or adduction machine seen from the front, centred at [x]: a seat [seatHeight] up, a back pad
 * behind the lifter up to [padTop], and on each side a knee pad that rides with the knee, on its outside
 * ([padsOutside]: abduction, the legs inside the pads) or its inside (adduction, the legs outside the pads), on a
 * lever down to a pivot under the seat; foot bars under the heels. The pads are drawn in front of the legs, the levers behind.
 * [handle]: the near side's handle grip (the far one mirrored about [x]), on a post at the seat's side, drawn across
 * the hand. A typical machine: a seat about 45 cm up and 44 cm wide, knee pads about 25 cm tall and 8 cm thick, handles
 * about 12 cm long; neutral sizes.
 */
data class HipMachine(val x: Double, val seatHeight: Double, val padTop: Double, val padsOutside: Boolean, val handle: Point? = null) : FixedProp {
    companion object {
        const val SEAT_WIDTH = 44.0
        const val KNEE_PAD_LENGTH = 25.0

        /** The knee pad's half thickness, cm (an 8 cm pad). */
        const val KNEE_PAD_RADIUS = 4.0
    }
}

// ---- geometry (internal) ----

private val E = Role.EQUIPMENT

private fun cap(role: Role, a: Point, b: Point, width: Double) = Capsule(role, a.x, a.y, b.x, b.y, width)

private fun box(role: Role, s: Slab, radius: Double = 2.0) =
    RoundRect(role, s.cx - s.length / 2, s.cy - s.thickness / 2, s.length, s.thickness, radius)

private fun slabPolygon(role: Role, s: Slab): Polygon = Polygon(role, s.corners().flatMap { listOf(it.x, it.y) })

private fun roller(p: Point, r: Double) = Slab(p.x, p.y, 0.0, 0.0, radius = r)

/** A pad (roller) drawn in front: a gap ring, then the pad. */
private fun frontRoller(p: Point, r: Double): List<Placed> =
    listOf(Placed(Circle(Role.GAP, p.x, p.y, r + Body.GAP_EXTRA / 2)), Placed(Circle(E, p.x, p.y, r)))

/** A lever drawn in front: a gap outline, then the lever. */
private fun frontLever(a: Point, b: Point, width: Double = 4.0): List<Placed> =
    listOf(Placed(cap(Role.GAP, a, b, width + Body.GAP_EXTRA)), Placed(cap(E, a, b, width)))

private fun unitOf(v: Point): Point = v * (1.0 / v.length())

// MiniBand

/** The band's two ends: on each leg's centre line, just above the knee or the ankle. */
internal fun MiniBand.ends(s: Skeleton): Pair<Point, Point> {
    fun at(l: LimbPoints): Point = when (at) {
        BandAt.KNEES -> l.knee + unitOf(l.hip - l.knee) * MiniBand.ABOVE_KNEE
        BandAt.ANKLES -> l.ankle + unitOf(l.knee - l.ankle) * MiniBand.ABOVE_ANKLE
    }
    return at(s.near) to at(s.far)
}

internal fun MiniBand.drawFrontOf(s: Skeleton?): List<Placed> {
    if (s == null) return emptyList()
    val (a, b) = ends(s)
    return listOf(
        Placed(cap(Role.GAP, a, b, MiniBand.WIDTH + Body.GAP_EXTRA)),
        Placed(cap(E, a, b, MiniBand.WIDTH)),
    )
}

// StabilityBall

internal fun StabilityBall.slab(): Slab = roller(Point(x, -radius), radius)

internal fun StabilityBall.drawOf(): List<Placed> = listOf(
    Placed(Circle(E, x, -radius, radius)),
    // A seam line, so the ball reads as a ball and not a disc.
    Placed(ring(Role.GAP, x, -radius, radius * 0.62, radius * 0.62 - 0.8)),
)

// AnkleStrap

internal fun AnkleStrap.centerOn(s: Skeleton): Point = s.side(side).let { it.ankle + unitOf(it.knee - it.ankle) * AnkleStrap.ABOVE_ANKLE }

internal fun AnkleStrap.drawOn(s: Skeleton): List<Shape> {
    val l = s.side(side)
    val c = centerOn(s)
    val along = unitOf(l.knee - l.ankle)
    val across = Point(-along.y, along.x) * (Body.SHIN_WIDTH / 2 + 1.0)
    return listOf(cap(Role.ACCENT, c - across, c + across, 3.0))
}

// NordicStation

internal fun NordicStation.padSlab(): Slab = Slab.box(padFrom, -padHeight, padTo, 0.0)

internal fun NordicStation.rollerSlab(): Slab = roller(roller, NordicStation.ROLLER_RADIUS)

internal fun NordicStation.drawOf(): List<Placed> = buildList {
    val base = roller.x - 32.0
    // The base on the floor behind the toes, the post up to the roller, the roller over the ankles.
    add(Placed(Capsule(E, base - 15.0, -1.5, base + 15.0, -1.5, 3.0)))
    add(Placed(cap(E, Point(base, -1.5), roller, 4.0)))
    add(Placed(box(E, padSlab(), 2.0)))
    add(Placed(Circle(E, roller.x, roller.y, NordicStation.ROLLER_RADIUS)))
}

// ReverseHyperBench

internal fun ReverseHyperBench.padSlab(): Slab = Slab.box(edge, -height, edge + length, -height + ReverseHyperBench.PAD)

internal fun ReverseHyperBench.handleSlab(): Slab = Slab(handle.x, handle.y, 6.0, 3.0)

/** The posts: the first clear of the hanging feet, which point under the bench. */
private fun ReverseHyperBench.posts(): List<Slab> = listOf(edge + 24.0, edge + length - 8.0).map {
    Slab.box(it - 2.5, -height + ReverseHyperBench.PAD, it + 2.5, 0.0)
}

/** The handle's grip seen end on, over the hand (as the side view's pull-up bar), set off by a gap ring. */
internal fun ReverseHyperBench.drawFrontOf(): List<Placed> = frontRoller(handle, 2.2)

internal fun ReverseHyperBench.solidsOf(): List<Solid> = (listOf(padSlab()) + posts()).map { Solid("bench", it, stopsArms = false) }

internal fun ReverseHyperBench.drawOf(): List<Placed> = buildList {
    add(Placed(Capsule(E, edge - 5.0, -1.5, edge + length + 15.0, -1.5, 3.0)))
    for (p in posts()) add(Placed(Capsule(E, p.cx, p.cy - p.thickness / 2, p.cx, -1.5, 5.0)))
    // The handle on a strut from under the head end.
    val top = Point(edge + length - 4.0, -height + ReverseHyperBench.PAD)
    add(Placed(cap(E, top, handle, 3.5)))
    add(Placed(Capsule(E, handle.x - 3.0, handle.y, handle.x + 3.0, handle.y, 3.5)))
    add(Placed(box(E, padSlab(), 2.5)))
}

// LyingLegCurlMachine

internal fun LyingLegCurlMachine.padSlab(): Slab = Slab.box(kneeEnd, -height, kneeEnd + length, -height + LyingLegCurlMachine.PAD)

internal fun LyingLegCurlMachine.rollerSlab(): Slab = roller(roller, LyingLegCurlMachine.ROLLER_RADIUS)

internal fun LyingLegCurlMachine.handleSlab(): Slab = Slab(handle.x, handle.y, 6.0, 3.0)

internal fun LyingLegCurlMachine.drawOf(): List<Placed> = buildList {
    val underside = -height + LyingLegCurlMachine.PAD
    val back = kneeEnd + 12.0
    val front = kneeEnd + length - 15.0
    add(Placed(Capsule(E, back - 15.0, -1.5, front + 25.0, -1.5, 3.0)))
    add(Placed(Capsule(E, back, underside, back, -1.5, 5.0)))
    add(Placed(Capsule(E, front, underside, front, -1.5, 5.0)))
    // The pivot's housing from under the knee end of the pad, beside the knee.
    add(Placed(cap(E, Point(kneeEnd + 6.0, underside), pivot, 5.0)))
    // The handles on a strut from under the head end.
    add(Placed(cap(E, Point(kneeEnd + length - 6.0, underside), handle, 3.5)))
    add(Placed(Capsule(E, handle.x - 3.0, handle.y, handle.x + 3.0, handle.y, 3.5)))
    add(Placed(box(E, padSlab(), 2.5)))
}

/** The lever from the pivot to the roller, and the roller: beside and over the lower legs. */
internal fun LyingLegCurlMachine.drawFrontOf(): List<Placed> =
    frontLever(pivot, roller) + frontRoller(roller, LyingLegCurlMachine.ROLLER_RADIUS) + frontRoller(pivot, 3.5)

// SeatedLegCurlMachine

internal fun SeatedLegCurlMachine.seatSlab(): Slab =
    Slab.box(seatFront - SeatedLegCurlMachine.SEAT_LENGTH, -seatHeight, seatFront, -seatHeight + FlatBench.PAD)

/** The back pad, hinged at the seat's rear top corner, [SeatedLegCurlMachine.backAngle]° from level. */
internal fun SeatedLegCurlMachine.backSlab(): Slab {
    val hinge = Point(seatFront - SeatedLegCurlMachine.SEAT_LENGTH, -seatHeight)
    val along = 180.0 - backAngle
    val c = hinge + dir(along) * (SeatedLegCurlMachine.BACK_LENGTH / 2) + dir(along + 90.0) * (FlatBench.PAD / 2)
    return Slab(c.x, c.y, SeatedLegCurlMachine.BACK_LENGTH, FlatBench.PAD, along)
}

internal fun SeatedLegCurlMachine.rollerSlab(): Slab = roller(roller, SeatedLegCurlMachine.ROLLER_RADIUS)

internal fun SeatedLegCurlMachine.lapSlab(): Slab = roller(lap, SeatedLegCurlMachine.ROLLER_RADIUS)

internal fun SeatedLegCurlMachine.handleSlab(): Slab = Slab(handle.x, handle.y, 3.0, 6.0)

internal fun SeatedLegCurlMachine.drawOf(): List<Placed> = buildList {
    val seat = seatSlab()
    val mid = seatFront - SeatedLegCurlMachine.SEAT_LENGTH / 2
    add(Placed(Capsule(E, mid - 30.0, -1.5, pivot.x + 25.0, -1.5, 3.0)))
    add(Placed(Capsule(E, mid, -seatHeight + FlatBench.PAD, mid, -1.5, 6.0)))
    // The frame from under the seat's front to the pivot's post, which stands on the base.
    add(Placed(cap(E, Point(seatFront - 4.0, -seatHeight + FlatBench.PAD + 2.0), Point(pivot.x + 4.0, -seatHeight + FlatBench.PAD + 2.0), 5.0)))
    add(Placed(cap(E, Point(pivot.x + 4.0, -1.5), pivot, 5.0)))
    add(Placed(cap(E, Point(handle.x - 6.0, -seatHeight + FlatBench.PAD), handle, 3.0)))
    add(Placed(box(E, seat)))
    add(Placed(slabPolygon(E, backSlab())))
}

/** The lever to the roller under the lower legs, the lap pad's arm over the thighs, and both pads: in front of the legs. */
internal fun SeatedLegCurlMachine.drawFrontOf(): List<Placed> {
    val r = SeatedLegCurlMachine.ROLLER_RADIUS
    return frontLever(pivot, roller) + frontRoller(roller, r) + frontLever(pivot, lap, 3.5) + frontRoller(lap, r) + frontRoller(pivot, 3.5)
}

// StandingLegCurlMachine

internal fun StandingLegCurlMachine.thighSlab(): Slab = roller(thighPad, StandingLegCurlMachine.ROLLER_RADIUS)

internal fun StandingLegCurlMachine.rollerSlab(): Slab = roller(roller, StandingLegCurlMachine.ROLLER_RADIUS)

internal fun StandingLegCurlMachine.handleSlab(): Slab = Slab(handle.x, handle.y, 3.0, 6.0)

internal fun StandingLegCurlMachine.drawOf(): List<Placed> = buildList {
    val top = min(handle.y, thighPad.y) - 4.0
    add(Placed(Capsule(E, postX - 25.0, -1.5, postX + 25.0, -1.5, 3.0)))
    add(Placed(Capsule(E, postX, -1.5, postX, top, 6.0)))
    // The arms from the post to the handle, the thigh pad and the pivot's housing.
    add(Placed(cap(E, Point(postX, handle.y), handle, 3.5)))
    add(Placed(Capsule(E, handle.x, handle.y - 4.0, handle.x, handle.y + 4.0, 3.5)))
    add(Placed(cap(E, Point(postX, thighPad.y), thighPad, 4.0)))
    add(Placed(cap(E, Point(postX, pivot.y), pivot, 4.0)))
    add(Placed(Circle(E, thighPad.x, thighPad.y, StandingLegCurlMachine.ROLLER_RADIUS)))
}

/** The lever from the pivot to the roller behind the lower leg, and the roller: in front of the leg. */
internal fun StandingLegCurlMachine.drawFrontOf(): List<Placed> =
    frontLever(pivot, roller) + frontRoller(roller, StandingLegCurlMachine.ROLLER_RADIUS) + frontRoller(pivot, 3.5)

// HipThrustMachine

internal fun HipThrustMachine.benchSlab(): Slab = Slab.box(benchEdge - HipThrustMachine.BACK_PAD, -benchHeight, benchEdge, -benchHeight + FlatBench.PAD)

internal fun HipThrustMachine.platformSlab(): Slab = Slab.box(
    platform.x - HipThrustMachine.PLATFORM_LENGTH / 2, platform.y,
    platform.x + HipThrustMachine.PLATFORM_LENGTH / 2, platform.y + HipThrustMachine.PLATFORM_THICKNESS,
)

internal fun HipThrustMachine.padSlab(): Slab = roller(pad, HipThrustMachine.PAD_RADIUS)

internal fun HipThrustMachine.drawOf(): List<Placed> = buildList {
    val bench = benchSlab()
    val plate = platformSlab()
    val postX = bench.cx
    add(Placed(Capsule(E, bench.cx - bench.length / 2 - 5.0, -1.5, max(plate.cx + plate.length / 2, pivot.x) + 5.0, -1.5, 3.0)))
    add(Placed(Capsule(E, postX, -benchHeight + FlatBench.PAD, postX, -1.5, 6.0)))
    // The lever's post, then the lever from the pivot to the hip pad (behind the body), the platform.
    add(Placed(cap(E, Point(pivot.x, -1.5), pivot, 6.0)))
    add(Placed(cap(E, pivot, pad, 5.0)))
    add(Placed(Circle(E, pivot.x, pivot.y, 4.0)))
    add(Placed(box(E, plate, 1.5)))
    add(Placed(box(E, bench)))
}

/** The hip pad over the front of the hips, set off by a gap ring. */
internal fun HipThrustMachine.drawFrontOf(): List<Placed> = frontRoller(pad, HipThrustMachine.PAD_RADIUS)

// HipMachine

internal fun HipMachine.seatSlab(): Slab = Slab.box(x - HipMachine.SEAT_WIDTH / 2, -seatHeight, x + HipMachine.SEAT_WIDTH / 2, -seatHeight + FlatBench.PAD)

/** Both handles' grips: the near one and its mirror image about [HipMachine.x]. */
internal fun HipMachine.handles(): List<Point> = handle?.let { listOf(it, Point(2 * x - it.x, it.y)) } ?: emptyList()

internal fun HipMachine.handleSlabs(): List<Slab> = handles().map { Slab(it.x, it.y, 12.0, 3.0) }

/** The knee pad on [side]: an upright pad beside the knee, outside it or inside it, from just above the knee down. */
internal fun HipMachine.kneePad(side: Side, s: Skeleton): Pair<Point, Point> {
    val k = s.side(side).knee
    val outward = if ((k.x - x) >= 0.0) 1.0 else -1.0
    val toward = if (padsOutside) outward else -outward
    val c = k + Point(toward * (Body.SHIN_WIDTH / 2 + HipMachine.KNEE_PAD_RADIUS), 0.0)
    val h = HipMachine.KNEE_PAD_LENGTH / 2 - HipMachine.KNEE_PAD_RADIUS
    return Point(c.x, c.y - h + 4.0) to Point(c.x, c.y + h + 4.0)
}

internal fun HipMachine.drawOf(s: Skeleton?): List<Placed> = buildList {
    add(Placed(Capsule(E, x - 30.0, -1.5, x + 30.0, -1.5, 3.0)))
    add(Placed(Capsule(E, x, -seatHeight + FlatBench.PAD, x, -1.5, 7.0)))
    // The back pad behind the torso, from above the hips up (the renderer draws the front view's far legs before the
    // machine, so the pad stops above them and the seat stays under them).
    add(Placed(RoundRect(E, x - 19.0, -padTop, 38.0, padTop - seatHeight - BACK_PAD_GAP, 3.0)))
    add(Placed(box(E, seatSlab())))
    // The knee pads' levers, down behind the shins to the pivot under the seat.
    if (s != null && s.view == View.FRONT) for (side in Side.entries) {
        val (_, b) = kneePad(side, s)
        add(Placed(cap(E, b, Point(x, -seatHeight + 12.0), 3.5)))
    }
    add(Placed(Circle(E, x, -seatHeight + 12.0, 3.5)))
    // Each handle's post, from the seat's side up to the grip.
    for (h in handles()) add(Placed(Capsule(E, h.x, h.y, h.x, -seatHeight + FlatBench.PAD, 3.0)))
    // Foot bars under the heels.
    if (s != null) for (side in Side.entries) {
        val l = s.side(side)
        val y = max(l.heel.y, l.toe.y) + Body.FOOT_WIDTH / 2 + 1.5
        add(Placed(Capsule(E, l.ankle.x - 7.0, y, l.ankle.x + 7.0, y, 3.0)))
    }
}

/** How far above the seat the back pad stops, cm: clear of the hips and thighs. */
private const val BACK_PAD_GAP = 26.0

/**
 * The knee pads: beside the knees, drawn in front of the legs with a gap (their levers run behind the shins); and the
 * handles' grips across the hands, so the hands show on them.
 */
internal fun HipMachine.drawFrontOf(s: Skeleton?): List<Placed> = buildList {
    if (s == null || s.view != View.FRONT) return@buildList
    for (h in handles()) addAll(frontLever(Point(h.x - 6.0, h.y), Point(h.x + 6.0, h.y), 3.0))
    for (side in Side.entries) {
        val (a, b) = kneePad(side, s)
        add(Placed(cap(Role.GAP, a, b, 2 * HipMachine.KNEE_PAD_RADIUS + Body.GAP_EXTRA)))
        add(Placed(cap(E, a, b, 2 * HipMachine.KNEE_PAD_RADIUS)))
    }
}
