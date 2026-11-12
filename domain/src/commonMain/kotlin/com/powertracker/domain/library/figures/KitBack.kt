package com.powertracker.domain.library.figures

import kotlin.math.max
import kotlin.math.min

// Kit pieces the back batch needs (Task 7), at real-world scale in centimetres. Sizes are typical, manufacturer-
// neutral values, each noted on its piece. Kit.kt's surfaces, solids and drawing hand these pieces to the functions
// below. The lifter faces +x, as everywhere.

/**
 * A seated cable row station: a long, low bench whose front edge is at [seatX] and top [seatHeight] up, a foot plate
 * whose face (towards the lifter) is centred at [plate] and tilted [plateTilt]° back from upright, its top towards the
 * machine, and a low pulley at [pulley] on the column behind the plate, cabled to the attachment in the hands. A
 * typical station: the bench about 45 cm high and 120 cm long, 35 cm foot plates tilted about 15°, the pulley a little
 * above the foot plates; neutral sizes.
 */
data class RowStation(val seatX: Double, val seatHeight: Double, val plate: Point, val pulley: Point, val plateTilt: Double = 15.0) : FixedProp {
    /** The foot plate's long axis, from its bottom to its top (towards the machine at the top). */
    val plateAngle: Double get() = 90.0 - plateTilt

    companion object {
        const val SEAT_LENGTH = 70.0
        const val PLATE = 35.0
        const val PLATE_THICKNESS = 4.0
    }
}

/**
 * A seated row machine with a chest pad: a seat whose front edge is at [seatX], top [seatHeight] up; an upright chest
 * pad whose face (towards the lifter) is at x = [padFace], centred at height [padHeight]; and the handles on a lever
 * that swings about [pivot] on the frame above, following the hands. A typical selectorised seated row: a 40 cm seat
 * about 45 cm up, a 30 cm tall, 8 cm thick chest pad, vertical (neutral-grip) handles; neutral sizes.
 */
data class RowMachine(val seatX: Double, val seatHeight: Double, val padFace: Double, val padHeight: Double, val pivot: Point) : FixedProp {
    companion object {
        const val SEAT_LENGTH = 40.0
        const val PAD_LENGTH = 30.0
        const val PAD_THICKNESS = 8.0
    }
}

/**
 * A chest-support pad for rows lying face down: the pad's top runs up from its low end at [low] (world point) for
 * [length] at [angle]° above level, towards +x (the lifter's head end), on a frame to the floor. With [footPlate] it
 * is a machine (the chest-supported T-bar row's lever machine): a foot platform whose top is at that point. A typical
 * incline bench pad is about 85 cm long, 6 cm thick; a machine's pad similar; neutral sizes.
 */
data class ProneBench(val low: Point, val angle: Double, val length: Double = 85.0, val footPlate: Point? = null) : FixedProp {
    companion object {
        const val PAD = 6.0
        const val PLATFORM = 40.0
    }
}

/**
 * A T-bar row lever: a bar that pivots at [pivot] on the floor (a landmine-style T-bar behind the lifter, or a chest-
 * supported machine's lever in front), held by a crossbar handle in the hands, with plates loaded [plateAlong] cm
 * from the pivot along the lever (past the handle on a T-bar, between pivot and handle on a machine). Drawn in the
 * chalk accent like a bar. A typical lever: a 5 cm tube about 1.5–2 m long, 45 cm plates; neutral sizes.
 */
data class TBarLever(val pivot: Point, val plateAlong: Double) : FixedProp {
    companion object {
        const val TUBE = 4.0
    }
}

/**
 * A bar resting in a rack's J-hooks at [x], its centre [height] up (an inverted row's fixed bar). Seen from the side
 * the rack's uprights, at the bar's ends, are one line just behind the bar. The bar is drawn in front of the hands.
 * A typical power rack, 230 cm uprights; the bar's height is set per figure.
 */
data class RackBar(val x: Double, val height: Double) : FixedProp

/**
 * An assisted pull-up machine seen from the side: its column at [x] in front of the lifter, a beam from its top out
 * to the pull-up bar (seen end-on) at [barX], [barHeight] up, and a knee pad (its top [padHeight] up, centred at
 * [padX]) on a lever from [pivot] on the column; the counterweight pushes the pad up. A typical machine: a 230 cm
 * column, the bar about 210–225 cm up, a 30 cm knee pad; neutral sizes. The figure sets the pivot so the lever keeps
 * its length.
 */
data class AssistPullUpMachine(
    val x: Double,
    val barX: Double,
    val barHeight: Double,
    val padX: Double,
    val padHeight: Double,
    val pivot: Point,
) : FixedProp {
    companion object {
        const val PAD = 30.0
    }
}

/**
 * Parallel pull-up handles (a neutral grip), seen from the side in profile: a handle [length] long front to back,
 * centred at [x], [height] up, hung on a frame from a wall post behind. A typical pair: 15–20 cm handles, 3.2 cm thick;
 * neutral sizes. The handle is drawn in front of the hands.
 */
data class ParallelHandles(val x: Double, val height: Double, val length: Double = 18.0) : FixedProp

/**
 * A resistance band looped over the pull-up bar at [bar] and hanging down under the near foot (a band-assisted
 * pull-up): two strands from the bar to the sole. A typical loop band, about 1 m long relaxed; drawn 2 cm wide. It
 * is drawn in front of the body, set off by a gap outline.
 */
data class PullUpBand(val bar: Point) : FixedProp {
    companion object {
        const val WIDTH = 1.6
    }
}

/**
 * A dip belt round the hips with a plate [plate] cm across hanging on its chain between the legs, [drop] cm below the
 * front of the hips. A typical 10 kg iron plate is about 30 cm across; neutral sizes. Side view: the plate hangs with
 * its face to the side (it fits between the legs), so it shows as a disc; front view, edge-on.
 */
data class DipBelt(val plate: Double = 30.0, val drop: Double = 38.0) : FixedProp

/**
 * A plate-loaded pulldown lever: it swings about [pivot] on a frame behind the seat, its handle in the hand, with plate
 * horns [horn] cm behind the pivot on the lever's other end. A typical machine: a lever about 1 m from pivot to
 * handle, 45 cm plates; neutral sizes.
 */
data class PulldownLever(val pivot: Point, val horn: Double = 35.0) : FixedProp

/**
 * A pullover machine: a seat whose front edge is at [seatX], top [seatHeight] up, an upright back pad
 * [backAngle]° from level, and a lever pivoting at [pivot] (beside the shoulder) out to an elbow pad on the back of
 * each upper arm and a crossbar in the hands. A typical machine: a 40 cm seat, a 70 cm back pad, a 25 cm elbow pad;
 * neutral sizes.
 */
data class PulloverMachine(val seatX: Double, val seatHeight: Double, val backAngle: Double, val pivot: Point) : FixedProp {
    companion object {
        const val SEAT_LENGTH = 40.0
        const val BACK_LENGTH = 70.0
        const val ELBOW_PAD = 24.0
    }
}

/**
 * A 45° back-extension bench: its thigh pad's face (under the fronts of the thighs) centred at [pad], along the body's
 * line at [angle]° above level, rising towards +x; a foot plate square to that line whose face (under the soles) is
 * centred at [footPlate]; and an ankle roller at [roller] behind the lower calves. A typical bench: a 30 cm, 10 cm
 * thick thigh pad, a 30 cm foot plate, a 12 cm roller; neutral sizes.
 */
data class HyperBench(val pad: Point, val footPlate: Point, val roller: Point, val angle: Double = 45.0) : FixedProp {
    companion object {
        const val PAD_LENGTH = 30.0
        const val PAD_THICKNESS = 10.0
        const val PLATE = 30.0
        const val ROLLER_RADIUS = 6.0
    }
}

/**
 * A weight plate held flat against the chest (a weighted back extension): [diameter] across, [THICKNESS] thick, on
 * the front of the chest. A typical 15 kg iron plate is about 35 cm across; neutral sizes. Seen from the side,
 * edge-on.
 */
data class ChestPlate(val diameter: Double = 35.0) : FixedProp {
    companion object {
        const val THICKNESS = 4.0
    }
}

/**
 * A spotter at the head end of the bench, in [pose] (side view), drawn in the far-limb shade behind the lifter: the
 * guide's "Use a spotter." for a weight moved over the face. A simplified body: capsules and a head, at the lifter's
 * own proportions (Body.kt, 175 cm).
 */
data class Spotter(val pose: Pose) : FixedProp

/** A cable attachment in both hands: a V-handle (parallel grip), a straight bar, or a rope. */
enum class Attachment { V_HANDLE, STRAIGHT_BAR, ROPE }

/**
 * A cable attachment held in both hands (Task 7), cabled to the pulley. Side view: a V-handle's parallel handle seen
 * in profile, square to the forearm; a straight bar end-on; a rope's near strand from the cable to the hand. Front
 * view: the V-handle's two handles, a straight bar [STRAIGHT_HALF] cm each side, the rope's two strands.
 */
data class CableAttachment(val kind: Attachment) : HeldProp {
    /** A typical straight cable bar is about 90 cm long ([STRAIGHT_HALF] cm each side); a V-handle's grips about 12 cm. */
    companion object {
        const val STRAIGHT_HALF = 45.0
        const val HANDLE = 12.0
    }
}

/** A single D-handle on a cable, in one hand ([hand]): a one-arm row or pulldown. A typical handle, about 7 cm across. */
data class CableHandle(val hand: Side) : HeldProp

/**
 * A cambered bar (a seal row's): its middle bent up towards the bench, so the sleeves and plates hang [drop] cm below
 * the hands. A typical cambered row bar, about 10–15 cm of camber; 45 cm plates; neutral.
 */
data class CamberedBar(val drop: Double = 12.0) : HeldProp

/**
 * One dumbbell held in both hands under its inner plate (a pullover): the handle points down from the palms along the
 * forearms, so the side view sees it in profile, its centre [Dumbbell.HANDLE] / 2 + half a head from the palms back
 * towards the shoulders. A typical dumbbell: [Dumbbell.HANDLE] 14 cm handle, 20 cm heads 5 cm thick (as [Dumbbell]).
 */
data object PulloverDumbbell : HeldProp

// ---- geometry (internal) ----

private val E = Role.EQUIPMENT
private val A = Role.ACCENT

private fun cap(role: Role, a: Point, b: Point, width: Double) = Capsule(role, a.x, a.y, b.x, b.y, width)

private fun slabPolygon(role: Role, s: Slab): Polygon = Polygon(role, s.corners().flatMap { listOf(it.x, it.y) })

private fun box(role: Role, s: Slab, radius: Double = 2.0): Shape =
    RoundRect(role, s.cx - s.length / 2, s.cy - s.thickness / 2, s.length, s.thickness, radius)

private fun foot(x: Double) = listOf(Placed(Capsule(E, x - 12.0, -1.5, x + 12.0, -1.5, 3.0)))

private fun plateDisc(c: Point, r: Double): List<Shape> = listOf(
    Circle(Role.ACCENT_FILL, c.x, c.y, r),
    ring(A, c.x, c.y, r, r - 1.3),
    Circle(A, c.x, c.y, Barbell.SLEEVE_RADIUS),
)

// RowStation
internal fun RowStation.seatSlab(): Slab = Slab.box(seatX - RowStation.SEAT_LENGTH, -seatHeight, seatX, -seatHeight + FlatBench.PAD)

internal fun RowStation.plateSlab(): Slab {
    val c = plate + dir(plateAngle - 90.0) * (RowStation.PLATE_THICKNESS / 2)
    return Slab(c.x, c.y, RowStation.PLATE, RowStation.PLATE_THICKNESS, plateAngle)
}

internal fun RowStation.drawOf(held: Point?): List<Placed> = buildList {
    val seat = seatSlab()
    val column = pulley.x + 14.0
    add(Placed(Capsule(E, seatX - RowStation.SEAT_LENGTH + 10.0, -1.5, column + 12.0, -1.5, 3.0)))
    for (lx in listOf(seatX - RowStation.SEAT_LENGTH + 10.0, seatX - 10.0)) add(Placed(Capsule(E, lx, -seatHeight + FlatBench.PAD, lx, -1.5, 5.0)))
    add(Placed(box(E, seat)))
    // The foot plate on a post from the frame.
    val p = plateSlab()
    add(Placed(Capsule(E, p.cx + 4.0, p.cy + 6.0, p.cx + 4.0, -1.5, 4.0)))
    add(Placed(slabPolygon(E, p)))
    // The column with its weight stack, the pulley on an arm towards the lifter, the cable to the attachment.
    add(Placed(RoundRect(E, column - 9.0, -200.0, 18.0, 198.5, 3.0), fit = false))
    add(Placed(Capsule(E, column, pulley.y, pulley.x, pulley.y, 5.0)))
    add(Placed(Circle(E, pulley.x, pulley.y, 4.0)))
    add(Placed(cap(E, pulley, held ?: (pulley + Point(-30.0, 0.0)), 0.8)))
}

// RowMachine
internal fun RowMachine.seatSlab(): Slab = Slab.box(seatX - RowMachine.SEAT_LENGTH, -seatHeight, seatX, -seatHeight + FlatBench.PAD)

internal fun RowMachine.padSlab(): Slab =
    Slab.box(padFace, -padHeight - RowMachine.PAD_LENGTH / 2, padFace + RowMachine.PAD_THICKNESS, -padHeight + RowMachine.PAD_LENGTH / 2)

internal fun RowMachine.drawOf(grip: Point?): List<Placed> = buildList {
    val seat = seatSlab()
    val pad = padSlab()
    val postX = padFace + RowMachine.PAD_THICKNESS + 12.0
    val mid = seatX - RowMachine.SEAT_LENGTH / 2
    add(Placed(Capsule(E, mid - 20.0, -1.5, postX + 25.0, -1.5, 3.0)))
    add(Placed(Capsule(E, mid, -seatHeight + FlatBench.PAD, mid, -1.5, 5.0)))
    // The frame: a post in front of the pad, up to a beam carrying the lever's pivot; the pad on an arm from the post.
    add(Placed(Capsule(E, postX, -1.5, postX, pivot.y, 6.0)))
    add(Placed(Capsule(E, postX, pivot.y, pivot.x, pivot.y, 5.0)))
    add(Placed(Capsule(E, pad.cx, pad.cy, postX, pad.cy, 4.0)))
    add(Placed(box(E, seat)))
    add(Placed(box(E, pad, 3.0)))
    if (grip != null) {
        add(Placed(cap(E, pivot, grip, 4.0)))
        add(Placed(Capsule(E, grip.x, grip.y - 7.0, grip.x, grip.y + 7.0, 3.5)))
    }
    add(Placed(Circle(E, pivot.x, pivot.y, 4.0)))
}

// ProneBench
internal fun ProneBench.padSlab(): Slab {
    val c = low + dir(angle) * (length / 2) + dir(angle - 90.0) * (ProneBench.PAD / 2)
    return Slab(c.x, c.y, length, ProneBench.PAD, angle)
}

internal fun ProneBench.platformSlab(): Slab? =
    footPlate?.let { Slab.box(it.x - ProneBench.PLATFORM / 2, it.y, it.x + ProneBench.PLATFORM / 2, it.y + 5.0) }

internal fun ProneBench.drawOf(): List<Placed> = buildList {
    val under = dir(angle - 90.0) * ProneBench.PAD
    val lowLeg = low + dir(angle) * 12.0 + under
    val highLeg = low + dir(angle) * (length - 12.0) + under
    add(Placed(Capsule(E, min(lowLeg.x, footPlate?.x ?: lowLeg.x) - 15.0, -1.5, highLeg.x + 15.0, -1.5, 3.0)))
    add(Placed(cap(E, lowLeg, Point(lowLeg.x, -1.5), 5.0)))
    add(Placed(cap(E, highLeg, Point(highLeg.x, -1.5), 5.0)))
    platformSlab()?.let { p ->
        add(Placed(Capsule(E, p.cx, p.cy, p.cx, -1.5, 5.0)))
        add(Placed(box(E, p, 1.5)))
    }
    add(Placed(slabPolygon(E, padSlab())))
}

// TBarLever
internal fun TBarLever.drawOf(grip: Point?): List<Placed> {
    val g = grip ?: (pivot + Point(100.0, -20.0))
    val d = (g - pivot).let { it * (1.0 / it.length()) }
    val plates = pivot + d * plateAlong
    val end = pivot + d * max(plateAlong + 6.0, (g - pivot).length() + 4.0)
    return buildList {
        add(Placed(Circle(E, pivot.x, pivot.y, 4.0)))
        add(Placed(Capsule(E, pivot.x - 10.0, -1.5, pivot.x + 10.0, -1.5, 3.0)))
        add(Placed(cap(A, pivot, end, TBarLever.TUBE)))
        for (shape in plateDisc(plates, Barbell.PLATE_DIAMETER / 2)) add(Placed(shape))
        add(Placed(Circle(A, g.x, g.y, 2.4)))
    }
}

// RackBar
internal fun RackBar.barSlab(): Slab = Slab(x, -height, 2 * PullUpBar.RADIUS, 2 * PullUpBar.RADIUS)

internal fun RackBar.drawOf(): List<Placed> = listOf(
    Placed(Capsule(E, x + 6.0, -1.5, x + 6.0, -230.0, 6.0), fit = false),
    Placed(Capsule(E, x - 6.0, -1.5, x + 18.0, -1.5, 3.0)),
    Placed(Capsule(E, x + 6.0, -height + PullUpBar.RADIUS + 1.75, x - 2.0, -height + PullUpBar.RADIUS + 1.75, 3.5)),
)

/** A bar seen end-on drawn over the hands, set off by a gap ring (as the side view's pull-up bar). */
internal fun barEndOn(x: Double, height: Double): List<Placed> = listOf(
    Placed(Circle(Role.GAP, x, -height, PullUpBar.DRAWN_RADIUS + Body.GAP_EXTRA / 2)),
    Placed(Circle(E, x, -height, PullUpBar.DRAWN_RADIUS)),
)

// AssistPullUpMachine
internal fun AssistPullUpMachine.barSlab(): Slab = Slab(barX, -barHeight, 2 * PullUpBar.RADIUS, 2 * PullUpBar.RADIUS)

internal fun AssistPullUpMachine.padSlab(): Slab =
    Slab.box(padX - AssistPullUpMachine.PAD / 2, -padHeight, padX + AssistPullUpMachine.PAD / 2, -padHeight + 6.0)

internal fun AssistPullUpMachine.drawOf(): List<Placed> = buildList {
    add(Placed(RoundRect(E, x - 9.0, -232.0, 18.0, 230.5, 3.0), fit = false))
    add(Placed(Capsule(E, x - 30.0, -1.5, x + 30.0, -1.5, 3.0)))
    add(Placed(RoundRect(E, x - 7.0, -62.0, 14.0, 50.0, 2.0)))
    add(Placed(Capsule(E, x, -barHeight - 6.0, barX, -barHeight - 6.0, 5.0)))
    add(Placed(Capsule(E, barX, -barHeight - 6.0, barX, -barHeight, 3.0)))
    val pad = padSlab()
    add(Placed(cap(E, pivot, Point(pad.cx, pad.cy + 3.0), 5.0)))
    add(Placed(Circle(E, pivot.x, pivot.y, 4.0)))
    add(Placed(box(E, pad)))
}

// ParallelHandles
internal fun ParallelHandles.handleSlab(): Slab = Slab(x, -height, length, 2 * PullUpBar.RADIUS)

internal fun ParallelHandles.drawOf(): List<Placed> = listOf(
    Placed(Capsule(E, x - length / 2, -height, x - length / 2, -height - 14.0, 3.0)),
    Placed(Capsule(E, x - length / 2, -height - 14.0, x - 55.0, -height - 14.0, 3.0)),
    Placed(Capsule(E, x - 55.0, -height - 30.0, x - 55.0, -height + 2.0, 5.0)),
)

internal fun ParallelHandles.drawFrontOf(): List<Placed> = listOf(
    Placed(Capsule(Role.GAP, x - length / 2, -height, x + length / 2, -height, 2 * PullUpBar.DRAWN_RADIUS + Body.GAP_EXTRA / 2)),
    Placed(Capsule(E, x - length / 2, -height, x + length / 2, -height, 2 * PullUpBar.DRAWN_RADIUS)),
)

// PullUpBand
/** Where the band runs under the near foot: the middle of its sole. */
internal fun bandFoot(s: Skeleton): Point {
    val n = s.near
    val mid = (n.heel + n.toe) * 0.5
    val down = dir(angleOf(n.toe - n.heel) - 90.0)
    return mid + down * (Body.FOOT_WIDTH / 2 + PullUpBand.WIDTH / 2)
}

internal fun PullUpBand.drawFrontOf(s: Skeleton?): List<Placed> {
    if (s == null) return emptyList()
    val f = bandFoot(s)
    return buildList {
        for (dx in listOf(-1.6, 1.6)) {
            val a = bar + Point(dx, PullUpBar.RADIUS)
            val b = f + Point(dx, 0.0)
            add(Placed(cap(Role.GAP, a, b, PullUpBand.WIDTH + Body.GAP_EXTRA / 2)))
        }
        for (dx in listOf(-1.6, 1.6)) add(Placed(cap(E, bar + Point(dx, PullUpBar.RADIUS), f + Point(dx, 0.0), PullUpBand.WIDTH)))
    }
}

// DipBelt
/** The dip belt's chain hangs from the front of the hips; the plate's centre below it. */
internal fun DipBelt.hook(s: Skeleton): Point =
    if (s.view == View.SIDE) s.onTorso(-2.0, Body.TORSO_RADIUS - 1.0) else s.pelvis + Point(0.0, 4.0)

internal fun DipBelt.plateCentre(s: Skeleton): Point = hook(s) + Point(0.0, drop)

internal fun DipBelt.drawOf(s: Skeleton?): List<Placed> {
    if (s == null) return emptyList()
    val h = hook(s)
    val c = plateCentre(s)
    val r = plate / 2
    return buildList {
        // Side view: the chain is drawn in front of the near leg (drawFrontOf), so it shows from the hip to the plate.
        if (s.view != View.SIDE) add(Placed(cap(E, h, c + Point(0.0, -r + 3.0), 1.2)))
        if (s.view == View.SIDE) {
            for (shape in plateDisc(c, r)) add(Placed(shape))
        } else {
            add(Placed(RoundRect(Role.ACCENT, c.x - 1.5, c.y - r, 3.0, plate, 1.0)))
        }
    }
}

/** Side view: the chain from the belt to the plate, over the near leg, set off by a gap outline. */
internal fun DipBelt.drawFrontOf(s: Skeleton?): List<Placed> {
    if (s == null || s.view != View.SIDE) return emptyList()
    val a = hook(s)
    val b = plateCentre(s) + Point(0.0, -plate / 2 + 3.0)
    return listOf(Placed(cap(Role.GAP, a, b, 1.2 + Body.GAP_EXTRA / 2)), Placed(cap(E, a, b, 1.2)))
}

// PulldownLever
internal fun PulldownLever.drawOf(grip: Point?): List<Placed> = buildList {
    val g = grip ?: (pivot + Point(80.0, 0.0))
    val d = (pivot - g).let { it * (1.0 / it.length()) }
    val horn = pivot + d * horn
    add(Placed(Capsule(E, pivot.x - 6.0, -1.5, pivot.x - 6.0, pivot.y, 6.0), fit = false))
    add(Placed(Capsule(E, pivot.x - 40.0, -1.5, pivot.x + 25.0, -1.5, 3.0)))
    for (shape in plateDisc(horn, Barbell.PLATE_DIAMETER / 2)) add(Placed(shape))
    add(Placed(cap(E, horn, g, 4.0)))
    add(Placed(Circle(E, pivot.x, pivot.y, 4.0)))
    add(Placed(Capsule(E, g.x - 6.0, g.y, g.x + 6.0, g.y, 3.5)))
}

// PulloverMachine
internal fun PulloverMachine.parts(): Pair<Slab, Slab> {
    val pad = FlatBench.PAD
    val seat = Slab.box(seatX - PulloverMachine.SEAT_LENGTH, -seatHeight, seatX, -seatHeight + pad)
    val hinge = Point(seatX - PulloverMachine.SEAT_LENGTH, -seatHeight)
    val along = 180.0 - backAngle
    val c = hinge + dir(along) * (PulloverMachine.BACK_LENGTH / 2) + dir(along + 90.0) * (pad / 2)
    return seat to Slab(c.x, c.y, PulloverMachine.BACK_LENGTH, pad, along)
}

internal fun PulloverMachine.drawOf(s: Skeleton?): List<Placed> = buildList {
    val (seat, back) = parts()
    val mid = seatX - PulloverMachine.SEAT_LENGTH / 2
    val postX = seatX - PulloverMachine.SEAT_LENGTH - 16.0
    add(Placed(Capsule(E, postX - 15.0, -1.5, seatX + 15.0, -1.5, 3.0)))
    add(Placed(Capsule(E, mid, -seatHeight + FlatBench.PAD, mid, -1.5, 5.0)))
    add(Placed(Capsule(E, postX, -1.5, postX, pivot.y - 10.0, 6.0)))
    add(Placed(Capsule(E, postX, pivot.y - 10.0, pivot.x, pivot.y, 5.0)))
    add(Placed(box(E, seat)))
    add(Placed(slabPolygon(E, back)))
    add(Placed(Circle(E, pivot.x, pivot.y, 4.0)))
}

/** The lever, its elbow pad and the bar: beside the body, outside the near arm, so drawn in front of it. */
internal fun PulloverMachine.drawFrontOf(s: Skeleton?): List<Placed> = buildList {
    if (s == null) return@buildList
    val n = s.near
    // The lever from the pivot out past the elbow; the elbow pad on the back of the upper arm, by the elbow.
    val u = (n.elbow - n.shoulder).let { it * (1.0 / it.length()) }
    val f = n.wrist - n.elbow
    val behind = dir(angleOf(u) + if (u.x * f.y - u.y * f.x > 0) 90.0 else -90.0)
    val half = PulloverMachine.ELBOW_PAD / 2
    val padMid = n.elbow - u * (half - 2.0) + behind * (Body.UPPER_ARM_WIDTH / 2 + 2.5)
    add(Placed(cap(Role.GAP, pivot, padMid + u * half, 4.0 + Body.GAP_EXTRA)))
    add(Placed(cap(E, pivot, padMid + u * half, 4.0)))
    add(Placed(cap(Role.GAP, padMid - u * half, padMid + u * half, 5.0 + Body.GAP_EXTRA)))
    add(Placed(cap(E, padMid - u * half, padMid + u * half, 5.0)))
    add(Placed(Circle(E, pivot.x, pivot.y, 4.0)))
    add(Placed(Circle(E, n.grip.x, n.grip.y, 2.2)))
}

// HyperBench
internal fun HyperBench.padSlab(): Slab {
    val c = pad + dir(angle - 90.0) * (HyperBench.PAD_THICKNESS / 2)
    return Slab(c.x, c.y, HyperBench.PAD_LENGTH, HyperBench.PAD_THICKNESS, angle)
}

internal fun HyperBench.plateSlab(): Slab {
    val c = footPlate + dir(angle + 180.0) * 2.0
    return Slab(c.x, c.y, HyperBench.PLATE, 4.0, angle - 90.0)
}

internal fun HyperBench.rollerSlab(): Slab = Slab(roller.x, roller.y, 0.0, 0.0, radius = HyperBench.ROLLER_RADIUS)

internal fun HyperBench.drawOf(): List<Placed> = buildList {
    val p = padSlab()
    val f = plateSlab()
    val padBack = Point(p.cx, p.cy) + dir(angle - 90.0) * (HyperBench.PAD_THICKNESS / 2 + 4.0)
    val plateBack = Point(f.cx, f.cy) + dir(angle + 180.0) * 3.0 + dir(angle - 90.0) * 6.0
    add(Placed(Capsule(E, plateBack.x - 15.0, -1.5, padBack.x + 25.0, -1.5, 3.0)))
    // The frame's rail under the body from the foot plate up to the pad, on two posts.
    add(Placed(cap(E, plateBack, padBack, 5.0)))
    add(Placed(Capsule(E, padBack.x, padBack.y, padBack.x + 12.0, -1.5, 5.0)))
    add(Placed(Capsule(E, plateBack.x, plateBack.y, plateBack.x, -1.5, 5.0)))
    // The roller's stem from the rail.
    add(Placed(cap(E, plateBack + dir(angle) * 10.0, roller, 3.5)))
    add(Placed(slabPolygon(E, f)))
    add(Placed(slabPolygon(E, p)))
    add(Placed(Circle(E, roller.x, roller.y, HyperBench.ROLLER_RADIUS)))
}

// ChestPlate
internal fun ChestPlate.drawOf(s: Skeleton?): List<Placed> {
    if (s == null) return emptyList()
    val c = s.onTorso(Segment.TORSO.length - Body.CHEST_BELOW_SHOULDER + 2.0, Body.CHEST_DEPTH + ChestPlate.THICKNESS / 2 + 0.3)
    val plate = Slab(c.x, c.y, diameter, ChestPlate.THICKNESS, s.torso)
    return listOf(
        Placed(slabPolygon(Role.GAP, plate.copy(length = plate.length + 1.0, thickness = plate.thickness + 1.0))),
        Placed(slabPolygon(Role.ACCENT, plate)),
    )
}

// Spotter
internal fun Spotter.drawOf(): List<Placed> {
    val s = pose.skeleton()
    val f = Role.BODY_FAR
    return buildList {
        for (side in listOf(s.far, s.near)) {
            for (seg in FigureRenderer.limbSegments(side)) add(Placed(seg.capsule(f)))
        }
        add(Placed(cap(f, s.pelvis + dir(s.torso) * -2.0, s.shoulder + dir(s.torso) * 2.0, 2 * Body.TORSO_RADIUS)))
        add(Placed(cap(f, s.neckBase, s.head, Body.NECK_WIDTH)))
        add(Placed(Circle(f, s.head.x, s.head.y, Body.HEAD_RADIUS)))
    }
}

// ---- held pieces ----

internal fun CableAttachment.drawOn(s: Skeleton): List<Shape> {
    val side = s.view == View.SIDE
    return when (kind) {
        Attachment.STRAIGHT_BAR -> if (side) {
            listOf(Circle(A, s.near.grip.x, s.near.grip.y, 2.0))
        } else {
            val c = (s.near.grip + s.far.grip) * 0.5
            listOf(Capsule(A, c.x - CableAttachment.STRAIGHT_HALF, c.y, c.x + CableAttachment.STRAIGHT_HALF, c.y, 2.6))
        }
        Attachment.V_HANDLE -> if (side) {
            val g = s.near.grip
            val f = (s.near.wrist - s.near.elbow).let { it * (1.0 / it.length()) }
            val across = Point(-f.y, f.x)
            val h = CableAttachment.HANDLE / 2
            val tip = g + f * 9.0
            listOf(
                cap(A, g - across * h, g + across * h, 3.0),
                cap(A, g - across * h, tip, 2.2),
                cap(A, g + across * h, tip, 2.2),
            )
        } else {
            Side.entries.map { side2 -> s.side(side2).grip.let { Capsule(A, it.x, it.y - 6.0, it.x, it.y + 6.0, 2.6) } }
        }
        Attachment.ROPE -> if (side) {
            val g = s.near.grip
            val f = (s.near.wrist - s.near.elbow).let { it * (1.0 / it.length()) }
            listOf(cap(A, g - f * 3.0, g + f * 10.0, 2.4), Circle(A, g.x - f.x * 4.0, g.y - f.y * 4.0, 2.4))
        } else {
            val c = (s.near.grip + s.far.grip) * 0.5 + Point(0.0, -6.0)
            Side.entries.map { side2 -> s.side(side2).grip.let { cap(A, c, it, 2.4) } }
        }
    }
}

internal fun CableHandle.drawOn(s: Skeleton): List<Shape> = s.side(hand).grip.let {
    if (s.view == View.SIDE) listOf(ring(A, it.x, it.y, 3.4, 2.2)) else listOf(Capsule(A, it.x, it.y - 5.0, it.x, it.y + 5.0, 2.6))
}

internal fun CamberedBar.load(s: Skeleton): Point = s.near.grip + Point(0.0, drop)

internal fun CamberedBar.drawOn(s: Skeleton): List<Shape> {
    val g = s.near.grip
    val p = load(s)
    return listOf(cap(A, g, p, 2 * Barbell.SHAFT_RADIUS)) + plateDisc(p, Barbell.PLATE_DIAMETER / 2) + listOf(Circle(A, g.x, g.y, 1.8))
}

internal fun PulloverDumbbell.centerOn(s: Skeleton): Point {
    val f = (s.near.wrist - s.near.elbow).let { it * (1.0 / it.length()) }
    return s.near.grip - f * (Dumbbell.HANDLE / 2 + 2.5)
}

internal fun PulloverDumbbell.drawOn(s: Skeleton): List<Shape> {
    val c = centerOn(s)
    val along = (s.near.wrist - s.near.elbow).let { it * (1.0 / it.length()) }
    val angle = angleOf(along)
    val h = Dumbbell.HANDLE / 2
    return listOf(Capsule(A, c.x - along.x * h, c.y - along.y * h, c.x + along.x * h, c.y + along.y * h, 3.2)) +
        listOf(-1.0, 1.0).map { sgn ->
            val m = c + along * (sgn * (h + 2.5))
            Polygon(A, Slab(m.x, m.y, 5.0, 2 * Dumbbell.HEAD_RADIUS, angle).corners().flatMap { listOf(it.x, it.y) })
        }
}

/** A dumbbell seen end-on (its handle pointing at the camera): the disc of its head. */
internal fun dumbbellDisc(c: Point): List<Shape> = listOf(
    Circle(Role.ACCENT_FILL, c.x, c.y, Dumbbell.HEAD_RADIUS),
    ring(A, c.x, c.y, Dumbbell.HEAD_RADIUS, Dumbbell.HEAD_RADIUS - 1.2),
    Circle(A, c.x, c.y, 1.6),
)
