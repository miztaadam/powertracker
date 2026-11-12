package com.powertracker.domain.library.figures

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// The equipment kit, at real-world scale in centimetres. Pieces are added only when a batch needs them (spec §2.2).
// Fixed pieces stand in the scene at a given x (the floor's top is y = 0); held pieces follow from the pose: they
// sit in the hands, or across the upper back, and are never placed on their own.
//
// IPF Technical Rules 2026 (https://www.powerlifting.sport/fileadmin/ipf/data/rules/technical-rules/english/2026_IPF_Technical_Rulebook__effective_01_March_2026__v3.pdf):
// bench at least 122 cm long, 29–32 cm wide, 42–45 cm from the floor to the top of the pad; bar rests 75–110 cm;
// the largest discs not more than 45 cm across. Bar shaft 28–29 mm (IPF Technical Rules 2021, same site).
// Other sizes (rack, cable stack, machines) are typical gym dimensions, kept neutral.

/** A kit piece in a [Scene]. */
sealed interface Prop

/** A piece that stands in the scene: the same in both panels unless its own settings change. */
sealed interface FixedProp : Prop

/** A piece held in the hands or across the upper back; its position follows from the pose. */
sealed interface HeldProp : Prop

/** The floor; its top is y = 0. Drawn across the panel on the bottom margin. */
data object Floor : FixedProp

/**
 * A flat bench running from [x] (head end) for [length], its pad's top [height] above the floor. With [nearLimbsBeside]
 * the near limbs pass beside it, not through it (a one-arm row: the far knee and hand on the bench, the near foot on the
 * floor next to it), so only the torso, head and far limbs are checked against it.
 */
data class FlatBench(
    val x: Double = 0.0,
    val length: Double = LENGTH,
    val height: Double = HEIGHT,
    val nearLimbsBeside: Boolean = false,
) : FixedProp {
    companion object {
        const val HEIGHT = 43.0
        const val LENGTH = 122.0
        const val PAD = 6.0
        const val WIDTH = 30.0
        const val LEG_INSET = 15.0
    }
}

/**
 * A power rack's uprights at [x] (the lifter's side of the posts), with J-hooks whose bar rest is [jHookHeight] above
 * the floor and safety arms whose top is [safetyHeight] above it, reaching [armLength] towards the lifter. The lifter
 * is to the right of the posts, or to the left when [lifterLeft] (a lifter who faced the hooks and stepped back).
 * Upright height, post spacing and arm length are typical gym rack sizes, kept neutral.
 */
data class PowerRack(
    val x: Double,
    val jHookHeight: Double,
    val safetyHeight: Double,
    val armLength: Double = 80.0,
    val height: Double = 230.0,
    val lifterLeft: Boolean = false,
) : FixedProp

/** How a barbell is held. */
enum class BarHold { HANDS, UPPER_BACK }

/** An Olympic barbell loaded with 45 cm plates. */
data class Barbell(val on: BarHold = BarHold.HANDS) : HeldProp {
    companion object {
        const val PLATE_DIAMETER = 45.0
        const val SHAFT_RADIUS = 1.45
        const val SLEEVE_RADIUS = 2.5
        const val LENGTH = 220.0
        const val INNER_COLLARS = 131.0
    }
}

/** A dumbbell in one hand. */
data class Dumbbell(val hand: Side) : HeldProp {
    companion object {
        const val HEAD_RADIUS = 10.0
        const val HANDLE = 14.0
    }
}

/** A cable column at [x] with its top pulley [reach] towards the lifter, [pulleyHeight] above the floor. */
data class CableStack(val x: Double, val pulleyHeight: Double = 215.0, val reach: Double = 45.0, val height: Double = 230.0) : FixedProp

/** A lat bar on the cable, in both hands. */
data object LatBar : HeldProp {
    const val HALF_LENGTH = 60.0
}

/** A lat pulldown seat centred at [x], [seatHeight] to its top, with a roller thigh pad at ([padX], [padHeight]). */
data class LatPulldownSeat(val x: Double, val padX: Double, val seatHeight: Double = 48.0, val padHeight: Double = 66.0) : FixedProp {
    companion object {
        const val PAD_RADIUS = 6.0
    }
}

/**
 * A 45° leg press. [x] is the front edge of the seat; the back pad reclines behind it. The foot plate rides the 45°
 * rail, [sled] cm up from its lowest stop. Sizes are typical, kept neutral. The back pad reclines to 25° above level
 * ([BACK_ANGLE] 155°): with the knees about 90° at the bottom and the feet flat, a more upright pad would fold the hips
 * further than the guide allows (see Quads.kt).
 */
data class LegPress45(val x: Double, val sled: Double) : FixedProp {
    companion object {
        const val SEAT_HEIGHT = 45.0
        const val BACK_ANGLE = 155.0
        const val RAIL_ANGLE = 45.0

        /**
         * How far the foot plate's centre stands off the rail on its carriage (a neutral estimate). The plate
         * travels along a line a little above the seated lifter's hips, so the legs push close to along the rail and,
         * with the knees bent about 90° at the bottom, the flat feet need no more ankle bend than the figure allows.
         */
        const val PLATE_RISE = 52.0

        /**
         * The foot plate's tilt back from square to the rail, towards upright (a neutral estimate: plates on these
         * machines are square to the rail or tilted a little). Its face's long axis points along [PLATE_ANGLE].
         */
        const val PLATE_TILT = 10.0
        const val PLATE_ANGLE = RAIL_ANGLE + 90.0 - PLATE_TILT
    }
}

/**
 * A pull-up bar at [x], [height] (its centre) above the floor, 3.2 cm thick. A typical gym bar size, kept neutral; the
 * height is set per figure so the lifter hangs clear of the floor.
 */
data class PullUpBar(val x: Double, val height: Double = 230.0) : FixedProp {
    companion object {
        const val RADIUS = 1.6

        /** The side view draws the bar's end larger than it is (3 cm, not 1.6), so it shows in front of the hand at phone size. */
        const val DRAWN_RADIUS = 3.0
    }
}

/**
 * A bench with a back support: a seat whose front edge is at [x], its top [FlatBench.HEIGHT] up (the IPF bench height,
 * as a typical bench), and a back pad hinged at the seat's rear, [backAngle] from level (90 = upright). Seat
 * [SEAT_LENGTH] and back pad [BACK_LENGTH] are typical adjustable-bench sizes, kept neutral; the back pad is long
 * enough to reach a seated lifter's head. The lifter faces +x.
 */
data class AdjustableBench(val x: Double, val backAngle: Double = 90.0) : FixedProp {
    companion object {
        const val SEAT_LENGTH = 40.0
        const val BACK_LENGTH = 85.0
    }
}

// ---- geometry the renderer and the checks share (internal) ----

/** An oriented box: centre, [length] along [angle], [thickness] across it, its edges rounded by [radius] (a roller is all radius). */
internal data class Slab(
    val cx: Double,
    val cy: Double,
    val length: Double,
    val thickness: Double,
    val angle: Double = 0.0,
    val radius: Double = 0.0,
) {
    /** Signed distance from [p] to the box: negative inside. */
    fun signedDistance(p: Point): Double {
        val d = p - Point(cx, cy)
        val along = dir(angle)
        val across = dir(angle + 90.0)
        val qx = abs(d.x * along.x + d.y * along.y) - length / 2
        val qy = abs(d.x * across.x + d.y * across.y) - thickness / 2
        return hypot(max(qx, 0.0), max(qy, 0.0)) + min(max(qx, qy), 0.0) - radius
    }

    fun corners(): List<Point> {
        val c = Point(cx, cy)
        val a = dir(angle) * (length / 2)
        val b = dir(angle + 90.0) * (thickness / 2)
        return listOf(c - a - b, c + a - b, c + a + b, c - a + b)
    }

    companion object {
        /** The axis-aligned box from (x1, y1) to (x2, y2). */
        fun box(x1: Double, y1: Double, x2: Double, y2: Double) =
            Slab((x1 + x2) / 2, (y1 + y2) / 2, abs(x2 - x1), abs(y2 - y1))
    }
}

/** The smallest signed distance from the segment [a]–[b] to [slab] (sampled finely enough for the checks). */
internal fun segmentDistance(a: Point, b: Point, slab: Slab): Double {
    if (a == b) return slab.signedDistance(a)
    var best = Double.MAX_VALUE
    for (i in 0..SAMPLES) best = min(best, slab.signedDistance(a + (b - a) * (i.toDouble() / SAMPLES)))
    return best
}

private const val SAMPLES = 64

/** A solid piece of kit the body must not pass through. Arms can pass beside narrow pads, so only the floor stops them. */
internal data class Solid(val label: String, val slab: Slab, val stopsArms: Boolean, val stopsNearLimbs: Boolean = true)

/** A shape in world units, before fitting. [fit] = false pieces (tall uprights) are clipped to the panel instead. */
internal data class Placed(val shape: Shape, val fit: Boolean = true, val floor: Boolean = false)

private val FLOOR_SLAB = Slab(0.0, 500.0, 1.0e6, 1000.0)

private fun eq(shape: Shape, fit: Boolean = true) = Placed(shape, fit)

private fun capsule(role: Role, a: Point, b: Point, width: Double) = Capsule(role, a.x, a.y, b.x, b.y, width)

private fun slabShape(role: Role, s: Slab, radius: Double): Shape =
    if (s.angle % 180.0 == 0.0) {
        RoundRect(role, s.cx - s.length / 2, s.cy - s.thickness / 2, s.length, s.thickness, radius)
    } else {
        Polygon(role, s.corners().flatMap { listOf(it.x, it.y) })
    }

/** A ring as one polygon: the outer circle one way and the inner the other, so the non-zero fill leaves a hole. */
internal fun ring(role: Role, cx: Double, cy: Double, outer: Double, inner: Double, steps: Int = 36): Polygon {
    val pts = ArrayList<Double>((steps + 1) * 4)
    for (i in 0..steps) (2 * PI * i / steps).let { pts += cx + outer * cos(it); pts += cy - outer * sin(it) }
    for (i in steps downTo 0) (2 * PI * i / steps).let { pts += cx + inner * cos(it); pts += cy - inner * sin(it) }
    return Polygon(role, pts)
}

private fun legPressParts(p: LegPress45): Triple<Slab, Slab, Slab> {
    val seat = Slab(p.x - 15.0, -LegPress45.SEAT_HEIGHT + 3.0, 30.0, 6.0)
    val corner = Point(p.x - 30.0, -LegPress45.SEAT_HEIGHT)
    val backCentre = corner + dir(LegPress45.BACK_ANGLE) * 35.0 + dir(LegPress45.BACK_ANGLE - 90.0) * 3.0
    val back = Slab(backCentre.x, backCentre.y, 70.0, 6.0, LegPress45.BACK_ANGLE)
    val plateCentre = legPressCarriage(p) + dir(LegPress45.RAIL_ANGLE + 90.0) * LegPress45.PLATE_RISE
    val plate = Slab(plateCentre.x, plateCentre.y, 55.0, 5.0, LegPress45.PLATE_ANGLE)
    return Triple(seat, back, plate)
}

/** The adjustable bench's seat and back pad. The back pad's front face starts at the hinge, the seat's rear top corner. */
private fun adjustableParts(b: AdjustableBench): Pair<Slab, Slab> {
    val pad = FlatBench.PAD
    val seat = Slab.box(b.x - AdjustableBench.SEAT_LENGTH, -FlatBench.HEIGHT, b.x, -FlatBench.HEIGHT + pad)
    val hinge = adjustableHinge(b)
    val along = 180.0 - b.backAngle
    val c = hinge + dir(along) * (AdjustableBench.BACK_LENGTH / 2) + dir(along + 90.0) * (pad / 2)
    return seat to Slab(c.x, c.y, AdjustableBench.BACK_LENGTH, pad, along)
}

/** Where the back pad's front face meets the seat's top. */
internal fun adjustableHinge(b: AdjustableBench) = Point(b.x - AdjustableBench.SEAT_LENGTH, -FlatBench.HEIGHT)

private fun legPressRailStart(p: LegPress45) = Point(p.x + 25.0, -30.0)

/** Where the sled's carriage sits on the rail. */
private fun legPressCarriage(p: LegPress45) = legPressRailStart(p) + dir(LegPress45.RAIL_ANGLE) * (40.0 + p.sled)

/** Where a fixed piece's named surfaces are. */
internal fun FixedProp.surfaces(view: View): Map<Surface, List<Slab>> = when (this) {
    Floor -> mapOf(Surface.FLOOR to listOf(FLOOR_SLAB))
    is FlatBench -> mapOf(Surface.BENCH to listOf(benchPad(view)))
    is PowerRack, is CableStack -> emptyMap()
    is LatPulldownSeat -> mapOf(
        Surface.SEAT to listOf(Slab(x, -seatHeight + 3.5, 44.0, 7.0)),
        Surface.THIGH_PAD to listOf(Slab(padX, -padHeight, 0.0, 0.0, radius = LatPulldownSeat.PAD_RADIUS)),
    )
    is LegPress45 -> legPressParts(this).let { (seat, back, plate) ->
        mapOf(Surface.SEAT to listOf(seat), Surface.BACK_PAD to listOf(back), Surface.FOOT_PLATE to listOf(plate))
    }
    is AdjustableBench -> adjustableParts(this).let { (seat, back) -> mapOf(Surface.SEAT to listOf(seat), Surface.BACK_PAD to listOf(back)) }
    is PullUpBar -> mapOf(
        Surface.PULL_UP_BAR to listOf(if (view == View.SIDE) Slab(x, -height, 2 * PullUpBar.RADIUS, 2 * PullUpBar.RADIUS) else Slab(x, -height, 120.0, 2 * PullUpBar.RADIUS)),
    )
}

private fun FlatBench.benchPad(view: View): Slab =
    if (view == View.SIDE) Slab.box(x, -height, x + length, -height + FlatBench.PAD)
    else Slab.box(x - FlatBench.WIDTH / 2, -height, x + FlatBench.WIDTH / 2, -height + FlatBench.PAD)

private fun FlatBench.legXs(view: View): List<Double> =
    if (view == View.SIDE) listOf(x + FlatBench.LEG_INSET, x + length - FlatBench.LEG_INSET) else listOf(x)

/** The solid parts of a fixed piece, for the no-intersection check. */
internal fun FixedProp.solids(view: View): List<Solid> = when (this) {
    Floor -> listOf(Solid("floor", FLOOR_SLAB, stopsArms = true))
    is FlatBench -> (
        listOf(benchPad(view)) + legXs(view).flatMap { lx ->
            listOf(Slab.box(lx - 2.5, -height + FlatBench.PAD, lx + 2.5, 0.0), Slab.box(lx - 12.0, -3.0, lx + 12.0, 0.0))
        }
        ).map { Solid("bench", it, stopsArms = false, stopsNearLimbs = !nearLimbsBeside) }
    is PowerRack, is CableStack, is PullUpBar -> emptyList()
    is LatPulldownSeat -> surfaces(view).values.flatten().map { Solid("lat pulldown", it, false) }
    is LegPress45 -> surfaces(view).values.flatten().map { Solid("leg press", it, false) }
    is AdjustableBench -> surfaces(view).values.flatten().map { Solid("bench", it, false) } +
        Solid("bench", Slab.box(x - AdjustableBench.SEAT_LENGTH / 2 - 2.5, -FlatBench.HEIGHT + FlatBench.PAD, x - AdjustableBench.SEAT_LENGTH / 2 + 2.5, 0.0), false)
}

/** The fixed piece's shapes in world units. [held] is the centre of a cable attachment in the scene, if any. */
internal fun FixedProp.draw(view: View, held: Point?): List<Placed> {
    val e = Role.EQUIPMENT
    return when (this) {
        Floor -> listOf(Placed(Capsule(e, 0.0, FLOOR_LINE / 2, 0.0, FLOOR_LINE / 2, FLOOR_LINE), floor = true))
        is FlatBench -> buildList {
            add(eq(slabShape(e, benchPad(view), 2.0)))
            for (lx in legXs(view)) {
                add(eq(Capsule(e, lx, -height + FlatBench.PAD, lx, -1.5, 5.0)))
                add(eq(Capsule(e, lx - 12.0, -1.5, lx + 12.0, -1.5, 3.0)))
            }
        }
        is PowerRack -> if (view == View.SIDE) {
            // Drawn for a lifter to the right of the posts, then mirrored about x when the lifter is to the left.
            val m = if (lifterLeft) -1.0 else 1.0
            fun at(dx: Double) = x + m * dx
            buildList {
                for (dx in listOf(-3.0, -11.0)) add(eq(Capsule(e, at(dx), -1.5, at(dx), -height, 6.0), fit = false))
                add(eq(Capsule(e, at(-28.0), -1.5, at(10.0), -1.5, 3.0)))
                add(eq(Capsule(e, at(0.0), -jHookHeight + 1.75, at(7.0), -jHookHeight + 1.75, 3.5)))
                add(eq(Capsule(e, at(7.0), -jHookHeight + 1.75, at(7.0), -jHookHeight - 3.0, 3.0)))
                add(eq(Capsule(e, at(-14.0), -safetyHeight + 2.0, at(armLength), -safetyHeight + 2.0, 4.0)))
            }
        } else {
            buildList {
                for (s in listOf(-1.0, 1.0)) {
                    val px = x + s * 60.0
                    add(eq(Capsule(e, px, -1.5, px, -height, 6.0), fit = false))
                    add(eq(Capsule(e, px, -jHookHeight + 1.75, px - s * 7.0, -jHookHeight + 1.75, 3.5)))
                    add(eq(RoundRect(e, min(px, px - s * 10.0), -safetyHeight, 10.0, 4.0, 1.0)))
                }
                add(eq(Capsule(e, x - 75.0, -1.5, x + 75.0, -1.5, 3.0)))
            }
        }
        is CableStack -> buildList {
            val pulley = Point(x + reach, -pulleyHeight)
            add(eq(RoundRect(e, x - 9.0, -height, 18.0, height - 1.5, 3.0), fit = false))
            add(eq(Capsule(e, x, -pulleyHeight, pulley.x, pulley.y, 5.0)))
            add(eq(Circle(e, pulley.x, pulley.y, 4.0)))
            add(eq(RoundRect(e, x - 7.0, -62.0, 14.0, 50.0, 2.0)))
            val end = held ?: (pulley + Point(0.0, 40.0))
            add(eq(capsule(e, pulley, end, 0.8)))
        }
        is LatPulldownSeat -> buildList {
            add(eq(RoundRect(e, x - 22.0, -seatHeight, 44.0, 7.0, 2.5)))
            add(eq(Capsule(e, x, -seatHeight + 7.0, x, -1.5, 6.0)))
            add(eq(Capsule(e, x - 30.0, -1.5, x + 30.0, -1.5, 3.0)))
            add(eq(Capsule(e, padX, -padHeight, padX + 14.0, -seatHeight + 4.0, 3.0)))
            add(eq(Circle(e, padX, -padHeight, LatPulldownSeat.PAD_RADIUS)))
        }
        is LegPress45 -> buildList {
            val (seat, back, plate) = legPressParts(this@draw)
            val railStart = legPressRailStart(this@draw)
            val railEnd = railStart + dir(LegPress45.RAIL_ANGLE) * 160.0
            add(eq(Capsule(e, x - 70.0, -1.5, railEnd.x, -1.5, 3.0)))
            add(eq(Capsule(e, seat.cx, seat.cy, seat.cx, -1.5, 5.0)))
            // A second leg under the back pad's low end, so the seat reads as part of the frame, not a stool.
            add(eq(Capsule(e, x - 30.0, -LegPress45.SEAT_HEIGHT, x - 40.0, -1.5, 5.0)))
            add(eq(Capsule(e, railEnd.x, railEnd.y, railEnd.x, -1.5, 5.0)))
            add(eq(capsule(e, railStart, railEnd, 5.0)))
            add(eq(slabShape(e, seat, 2.0)))
            add(eq(slabShape(e, back, 2.0)))
            // The sled: a solid wedge from its carriage on the rail out to the back of the foot plate.
            val carriage = legPressCarriage(this@draw)
            val rail = dir(LegPress45.RAIL_ANGLE)
            val along = dir(LegPress45.PLATE_ANGLE) * (plate.length / 2)
            val plateBack = Point(plate.cx, plate.cy) + dir(LegPress45.PLATE_ANGLE - 90.0) * (plate.thickness / 2)
            val sled = listOf(plateBack + along, plateBack - along, carriage - rail * 12.0, carriage + rail * 12.0)
            add(eq(Polygon(e, sled.flatMap { listOf(it.x, it.y) })))
            // The plate itself thicker than the sled behind it, set off by a gap line so the feet show on its face.
            add(eq(slabShape(Role.GAP, plate.copy(length = plate.length + 3.0, thickness = plate.thickness + 3.0), 2.5)))
            add(eq(slabShape(e, plate, 1.5)))
        }
        is AdjustableBench -> buildList {
            val (seat, back) = adjustableParts(this@draw)
            val mid = x - AdjustableBench.SEAT_LENGTH / 2
            add(eq(Capsule(e, mid, -FlatBench.HEIGHT + FlatBench.PAD, mid, -1.5, 5.0)))
            add(eq(Capsule(e, mid - 22.0, -1.5, mid + 22.0, -1.5, 3.0)))
            add(eq(slabShape(e, seat, 2.0)))
            add(eq(slabShape(e, back, 2.0)))
        }
        // Side view: the bar is held out on a level bracket from a wall post behind the lifter (a typical wall-mounted
        // bar, 45 cm out; neutral), so the bracket marks the bar's height right across the picture. The bar itself is
        // drawn in front of the hands (drawFront).
        is PullUpBar -> if (view == View.SIDE) {
            listOf(
                eq(Capsule(e, x - 45.0, -height, x, -height, 2.5)),
                eq(Capsule(e, x - 45.0, -height - 15.0, x - 45.0, -height + 15.0, 5.0)),
            )
        } else {
            listOf(
                eq(Capsule(e, x - 60.0, -height, x - 60.0, -height - 40.0, 4.0), fit = false),
                eq(Capsule(e, x + 60.0, -height, x + 60.0, -height - 40.0, 4.0), fit = false),
                eq(Capsule(e, x - 60.0, -height, x + 60.0, -height, 2 * PullUpBar.RADIUS)),
            )
        }
    }
}

/**
 * The parts of a fixed piece drawn in front of the body: the side view's pull-up bar, which the hands wrap round, would
 * otherwise be hidden under them. Drawn at [PullUpBar.DRAWN_RADIUS] with a gap ring so it shows at phone size.
 */
internal fun FixedProp.drawFront(view: View): List<Placed> = when {
    this is PullUpBar && view == View.SIDE -> listOf(
        eq(Circle(Role.GAP, x, -height, PullUpBar.DRAWN_RADIUS + Body.GAP_EXTRA / 2)),
        eq(Circle(Role.EQUIPMENT, x, -height, PullUpBar.DRAWN_RADIUS)),
    )
    else -> emptyList()
}

private const val FLOOR_LINE = 1.5

/** Where a held piece's centre is: in the hands (between them in the front view) or across the upper back. */
internal fun HeldProp.center(s: Skeleton): Point = when (this) {
    is Barbell -> when (on) {
        BarHold.HANDS -> grips(s)
        BarHold.UPPER_BACK ->
            if (s.view == View.SIDE) s.onTorso(Segment.TORSO.length - 4.0, -(Body.TORSO_RADIUS + Barbell.SHAFT_RADIUS))
            else s.onTorso(Segment.TORSO.length - 4.0, 0.0)
    }
    is Dumbbell -> s.side(hand).grip
    LatBar -> grips(s)
}

private fun grips(s: Skeleton): Point = if (s.view == View.SIDE) s.near.grip else (s.near.grip + s.far.grip) * 0.5

/** The held piece's shapes in world units: the chalk accent. */
internal fun HeldProp.draw(s: Skeleton): List<Placed> {
    val c = center(s)
    val a = Role.ACCENT
    val f = Role.ACCENT_FILL
    val side = s.view == View.SIDE
    return when (this) {
        is Barbell -> if (side) {
            val r = Barbell.PLATE_DIAMETER / 2
            listOf(
                Circle(f, c.x, c.y, r),
                ring(a, c.x, c.y, r, r - 1.3),
                ring(a, c.x, c.y, 5.5, 4.6),
                Circle(a, c.x, c.y, Barbell.SLEEVE_RADIUS),
            )
        } else {
            val half = Barbell.LENGTH / 2
            val inner = Barbell.INNER_COLLARS / 2
            buildList {
                add(Capsule(a, c.x - half, c.y, c.x + half, c.y, 2 * Barbell.SHAFT_RADIUS))
                for (sgn in listOf(-1.0, 1.0)) {
                    val px = c.x + sgn * (inner + 4.0)
                    add(RoundRect(f, px - 3.0, c.y - Barbell.PLATE_DIAMETER / 2, 6.0, Barbell.PLATE_DIAMETER, 1.5))
                    add(RoundRect(a, px - 1.0, c.y - Barbell.PLATE_DIAMETER / 2, 2.0, Barbell.PLATE_DIAMETER, 1.0))
                }
            }
        }
        is Dumbbell -> if (side) {
            listOf(
                Circle(f, c.x, c.y, Dumbbell.HEAD_RADIUS),
                ring(a, c.x, c.y, Dumbbell.HEAD_RADIUS, Dumbbell.HEAD_RADIUS - 1.2),
                Circle(a, c.x, c.y, 1.6),
            )
        } else {
            val h = Dumbbell.HANDLE / 2
            listOf(
                Capsule(a, c.x - h, c.y, c.x + h, c.y, 3.2),
                RoundRect(a, c.x - h - 5.0, c.y - Dumbbell.HEAD_RADIUS, 5.0, 2 * Dumbbell.HEAD_RADIUS, 1.5),
                RoundRect(a, c.x + h, c.y - Dumbbell.HEAD_RADIUS, 5.0, 2 * Dumbbell.HEAD_RADIUS, 1.5),
            )
        }
        LatBar -> if (side) {
            listOf(Circle(a, c.x, c.y, 2.0))
        } else {
            val straight = LatBar.HALF_LENGTH - 18.0
            val l = Point(c.x - straight, c.y)
            val r = Point(c.x + straight, c.y)
            listOf(
                capsule(a, l, r, 2.6),
                capsule(a, l, l + dir(200.0) * 18.0, 2.6),
                capsule(a, r, r + dir(-20.0) * 18.0, 2.6),
            )
        }
    }.map { Placed(it) }
}
