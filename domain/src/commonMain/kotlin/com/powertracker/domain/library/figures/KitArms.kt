package com.powertracker.domain.library.figures

// Kit pieces the arm batch needs (Task 9), at real-world scale in centimetres. Sizes are typical, manufacturer-neutral
// values, each noted on its piece. Kit.kt's surfaces, solids and drawing hand these pieces to the functions below. The
// arm batch also gives Dumbbell a new option, [Dumbbell.squareToForearm], whose default keeps the old drawing.

/**
 * An EZ curl bar in both hands: a short bar whose middle is bent in a zigzag, so the hands sit at an angle between a
 * palms-up and a palms-in grip. Seen from the side it is end-on like a barbell (the bends are in the bar's own plane,
 * across the body), so it is told apart by its load: a typical EZ bar is about 120 cm long with short sleeves, and it
 * is drawn loaded with 10 kg iron plates about [PLATE_DIAMETER] cm across, where the barbell carries 45 cm plates.
 * A front view draws the zigzag middle, the sleeves and the plates. Neutral sizes.
 */
data object EZBar : HeldProp {
    /** A 10 kg iron plate, cm across (the dip belt's plate, KitBack.kt). */
    const val PLATE_DIAMETER = 30.0

    /** Half the bar's length, cm (a 120 cm bar). */
    const val HALF_LENGTH = 60.0

    /** Where the plates sit, cm from the middle (inside the 25 cm sleeves). */
    const val PLATES_AT = 40.0
}

/**
 * One dumbbell held overhead in both hands, the palms under its top plate (an overhead triceps extension). Its
 * handle hangs straight down from the hands, so the side view (and the front view) sees it in profile: the handle with
 * a head at each end, its centre [Dumbbell.HANDLE] / 2 + half a head below the hands. A typical dumbbell: a 14 cm
 * handle and two heads 20 cm across and 5 cm thick ([Dumbbell.HANDLE], [Dumbbell.HEAD_RADIUS]); neutral sizes.
 */
data object OverheadDumbbell : HeldProp

/**
 * A preacher bench seen from the side: a seat whose top is [seatHeight] up, centred at [seatX], and in front of it an
 * arm pad sloping down away from the lifter, its top face starting at [padTop] (the upper end, by the armpit) and
 * running [padLength] at [padAngle]° below level towards +x, on a strut to the floor. The backs of the upper arms lie
 * on the pad. A typical preacher bench: a 30 cm seat about 50–60 cm up, a 30–35 cm arm pad sloped about 45° whose
 * padded block is about 14 cm deep under its top face (it fills the space in front of the chest); neutral sizes. The pad is in front of the chest and under the arms, beside the torso in this view, so only
 * the seat is an obstacle; the pad is a [Surface.ARM_PAD].
 */
data class PreacherBench(
    val seatX: Double,
    val seatHeight: Double,
    val padTop: Point,
    val padAngle: Double = 45.0,
    val padLength: Double = 35.0,
) : FixedProp {
    companion object {
        const val PAD_THICKNESS = 14.0
        const val SEAT_LENGTH = 30.0
    }
}

/**
 * A seated arm machine seen from the side, a curl machine or a triceps extension machine: a seat whose top is
 * [seatHeight] up, centred at [seatX]; an arm pad like a preacher bench's (top face from [padTop], [padLength] at
 * [padAngle]° below level towards +x) on a frame post; a weight-stack column in front of the pad at [stackX]; and a
 * lever that turns about [pivot] (lined up with the elbows) out to a handle in the hands, gripped across the body so
 * the side view sees it end-on. The lever and handle are beside the forearm, outside it, so they are drawn in front of
 * it. A typical selectorised arm machine: a 35 cm seat about 50 cm up, a 30 cm pad, a lever about the forearm's
 * length, a 150 cm stack column; neutral sizes.
 */
data class ArmPadMachine(
    val seatX: Double,
    val seatHeight: Double,
    val padTop: Point,
    val padAngle: Double,
    val pivot: Point,
    val stackX: Double,
    val padLength: Double = 30.0,
) : FixedProp {
    companion object {
        const val SEAT_LENGTH = 35.0
        const val STACK_HEIGHT = 150.0
    }
}

// ---- geometry (internal) ----

private val E = Role.EQUIPMENT
private val A = Role.ACCENT

private fun cap(role: Role, a: Point, b: Point, width: Double) = Capsule(role, a.x, a.y, b.x, b.y, width)

private fun slabPolygon(role: Role, s: Slab): Polygon = Polygon(role, s.corners().flatMap { listOf(it.x, it.y) })

/** An arm pad's slab: its top face from [top] for [length] at [angle]° below level, [thickness] under it. */
private fun armPadSlab(top: Point, angle: Double, length: Double, thickness: Double): Slab {
    val c = top + dir(-angle) * (length / 2) + dir(-angle - 90.0) * (thickness / 2)
    return Slab(c.x, c.y, length, thickness, -angle)
}

private fun seatSlab(x: Double, height: Double, length: Double) = Slab.box(x - length / 2, -height, x + length / 2, -height + FlatBench.PAD)

private fun seatOnPost(x: Double, height: Double, length: Double): List<Placed> = listOf(
    Placed(Capsule(E, x, -height + FlatBench.PAD, x, -1.5, 5.0)),
    Placed(Capsule(E, x - 15.0, -1.5, x + 15.0, -1.5, 3.0)),
    Placed(seatSlab(x, height, length).let { RoundRect(E, it.cx - it.length / 2, it.cy - it.thickness / 2, it.length, it.thickness, 2.0) }),
)

/** The pad and its strut: from under the pad's middle down to a foot on the floor. */
private fun padOnStrut(pad: Slab, angle: Double): List<Placed> {
    val under = Point(pad.cx, pad.cy) + dir(-angle - 90.0) * (pad.thickness / 2)
    return listOf(
        Placed(cap(E, under, Point(under.x, -1.5), 5.0)),
        Placed(Capsule(E, under.x - 14.0, -1.5, under.x + 14.0, -1.5, 3.0)),
        Placed(slabPolygon(E, pad)),
    )
}

// PreacherBench

internal fun PreacherBench.padSlab(): Slab = armPadSlab(padTop, padAngle, padLength, PreacherBench.PAD_THICKNESS)

internal fun PreacherBench.seat(): Slab = seatSlab(seatX, seatHeight, PreacherBench.SEAT_LENGTH)

internal fun PreacherBench.drawOf(): List<Placed> = seatOnPost(seatX, seatHeight, PreacherBench.SEAT_LENGTH) + padOnStrut(padSlab(), padAngle)

// ArmPadMachine

internal fun ArmPadMachine.padSlab(): Slab = armPadSlab(padTop, padAngle, padLength, PreacherBench.PAD_THICKNESS)

internal fun ArmPadMachine.seat(): Slab = seatSlab(seatX, seatHeight, ArmPadMachine.SEAT_LENGTH)

internal fun ArmPadMachine.drawOf(): List<Placed> = buildList {
    addAll(seatOnPost(seatX, seatHeight, ArmPadMachine.SEAT_LENGTH))
    // The weight stack's column, with its plates' block low down, and a beam from it to the pivot.
    add(Placed(RoundRect(E, stackX - 9.0, -ArmPadMachine.STACK_HEIGHT, 18.0, ArmPadMachine.STACK_HEIGHT - 1.5, 3.0)))
    add(Placed(Capsule(E, stackX - 15.0, -1.5, stackX + 15.0, -1.5, 3.0)))
    add(Placed(cap(E, Point(stackX, pivot.y), pivot, 5.0)))
    addAll(padOnStrut(padSlab(), padAngle))
}

/** The lever from the pivot to the handle and the handle's end: beside the forearm, drawn in front of it. */
internal fun ArmPadMachine.drawFrontOf(s: Skeleton?): List<Placed> {
    if (s == null || s.view != View.SIDE) return emptyList()
    val g = s.near.grip
    return listOf(
        Placed(cap(Role.GAP, pivot, g, 3.0 + Body.GAP_EXTRA)),
        Placed(cap(E, pivot, g, 3.0)),
        Placed(Circle(Role.GAP, g.x, g.y, 2.8 + Body.GAP_EXTRA / 2)),
        Placed(Circle(E, g.x, g.y, 2.8)),
        Placed(Circle(Role.GAP, pivot.x, pivot.y, 3.5 + Body.GAP_EXTRA / 2)),
        Placed(Circle(E, pivot.x, pivot.y, 3.5)),
    )
}

// ---- held pieces ----

internal fun EZBar.drawOn(s: Skeleton): List<Shape> {
    val r = EZBar.PLATE_DIAMETER / 2
    return if (s.view == View.SIDE) {
        val c = s.near.grip
        listOf(
            Circle(Role.ACCENT_FILL, c.x, c.y, r),
            ring(A, c.x, c.y, r, r - 1.3),
            ring(A, c.x, c.y, 4.5, 3.6),
            Circle(A, c.x, c.y, Barbell.SLEEVE_RADIUS),
        )
    } else {
        val c = (s.near.grip + s.far.grip) * 0.5
        val w = 2 * Barbell.SHAFT_RADIUS
        // The zigzag middle: in to the hands' angled grips and out again, then the straight sleeves.
        val pts = listOf(-EZBar.HALF_LENGTH to 0.0, -26.0 to 0.0, -18.0 to -3.0, -8.0 to 3.0, 0.0 to 0.0, 8.0 to 3.0, 18.0 to -3.0, 26.0 to 0.0, EZBar.HALF_LENGTH to 0.0)
            .map { (dx, dy) -> Point(c.x + dx, c.y + dy) }
        pts.zipWithNext { a, b -> cap(A, a, b, w) } + listOf(-1.0, 1.0).flatMap { sgn ->
            val px = c.x + sgn * EZBar.PLATES_AT
            listOf(
                RoundRect(Role.ACCENT_FILL, px - 2.5, c.y - r, 5.0, 2 * r, 1.5),
                RoundRect(A, px - 1.0, c.y - r, 2.0, 2 * r, 1.0),
            )
        }
    }
}

internal fun OverheadDumbbell.centerOn(s: Skeleton): Point {
    val g = if (s.view == View.SIDE) s.near.grip else (s.near.grip + s.far.grip) * 0.5
    return g + Point(0.0, Dumbbell.HANDLE / 2 + 2.5)
}

internal fun OverheadDumbbell.drawOn(s: Skeleton): List<Shape> {
    val c = centerOn(s)
    val h = Dumbbell.HANDLE / 2
    return listOf(Capsule(A, c.x, c.y - h, c.x, c.y + h, 3.2)) + listOf(-1.0, 1.0).map { sgn ->
        val m = c + Point(0.0, sgn * (h + 2.5))
        Polygon(A, Slab(m.x, m.y, 5.0, 2 * Dumbbell.HEAD_RADIUS, 90.0, 0.0).corners().flatMap { listOf(it.x, it.y) })
    }
}
