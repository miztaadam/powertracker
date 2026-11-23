package com.powertracker.domain.library.figures

// Kit pieces the quad batch needs (Task 10), at real-world scale in centimetres. Sizes are typical, manufacturer-
// neutral values, each noted on its piece. Kit.kt's surfaces, solids and drawing hand these pieces to the functions
// below. The quad batch also gives Barbell a new hold, [BarHold.CROOK] (a Zercher squat's bar in the crook of the
// elbows), and reuses SquatBox as the step-up box; the old pieces' drawing is unchanged.

/**
 * A sled hack squat seen from the side. The back pad and its shoulder pads ride a rail sloping up and back at
 * [angle]° above level; the lifter lies back on the pad, the shoulders under the shoulder pads, the feet on a fixed foot
 * plate at the bottom whose top face passes through [origin] square to the rail (so the plate is tilted [angle]° up
 * towards the toes). [pelvis] is where the sled holds the hips in this panel (the pad slides along the rail, so it
 * moves between the panels; the rail, frame and plate stay put). A typical hack squat: a rail at 45°, a back pad about
 * 75 cm long and 6 cm thick, shoulder pads about 10 cm across, a foot plate about 50 cm long; neutral sizes.
 */
data class HackSquatMachine(val origin: Point, val pelvis: Point, val angle: Double = 45.0) : FixedProp {
    companion object {
        /** The foot plate's length along its face, cm, from [PLATE_BACK] behind the origin. */
        const val PLATE_LENGTH = 50.0
        const val PLATE_BACK = 14.0
        const val PLATE_THICKNESS = 5.0

        /** The back pad: [PAD_THICKNESS] thick, from [PAD_LOW] below the hips to [PAD_HIGH] above them along the torso. */
        const val PAD_THICKNESS = 6.0
        const val PAD_LOW = 12.0
        const val PAD_HIGH = 52.0

        /** A shoulder pad's radius, cm (a 10 cm roll). */
        const val SHOULDER_PAD_RADIUS = 5.0
    }
}

/**
 * A plate-loaded lever squat seen from the side: a long lever that turns about [pivot] (on a post behind the lifter),
 * reaching forward to a padded yoke that rests across the tops of the shoulders at [pad] (the lever's end in this
 * panel; the figure sets the pivot so the lever is the same length in both panels). The lifter stands on the floor in
 * front of the pivot. A typical machine: a lever about 1.5 m long, a yoke roll about 12 cm across; neutral sizes. The
 * yoke is drawn in front of the shoulders, the lever behind the body.
 */
data class LeverSquatMachine(val pivot: Point, val pad: Point) : FixedProp {
    companion object {
        /** The yoke roll's radius, cm (a 12 cm roll). */
        const val PAD_RADIUS = 6.0
    }
}

/**
 * A leg extension machine seen from the side: a seat whose front edge is at [seatFront], its top [seatHeight] up, a
 * back pad hinged at the seat's rear [backAngle]° from level, and a lever that turns about [pivot] (lined up with the
 * knee) down to a roller pad on the front of the lower shin at [roller] (in this panel). [farRoller]: the far leg's own
 * pad, for a machine whose two levers move apart (an alternating leg extension); null for one pad across both shins.
 * The lever and the near roller are beside the shin and in front of it, so they are drawn in front of the leg. A
 * typical machine: a 40 cm seat about 50 cm up, a 60 cm back pad, a roller 12 cm across, a lever about the shin's
 * length; neutral sizes. [handle]: where the side handle is, under the near hand.
 */
data class LegExtensionMachine(
    val seatFront: Double,
    val seatHeight: Double,
    val backAngle: Double,
    val pivot: Point,
    val roller: Point,
    val handle: Point,
    val farRoller: Point? = null,
) : FixedProp {
    companion object {
        const val SEAT_LENGTH = 40.0
        const val BACK_LENGTH = 60.0

        /** The roller pad's radius, cm (a 12 cm roller). */
        const val ROLLER_RADIUS = 6.0
    }
}

/**
 * A push sled (a prowler) seen from the side: a skid along the floor from [x] forward, an upright handle post at its
 * rear end, at [x], up to [handleTop] above the floor, and weight plates stacked on a post in the middle. The lifter
 * stands behind it (towards -x) and holds the post. A typical sled: a skid about 100 cm long, handle posts about
 * 110–130 cm tall, 45 cm plates; neutral sizes.
 */
data class PushSled(val x: Double, val handleTop: Double = 125.0) : FixedProp {
    companion object {
        const val SKID = 100.0

        /** The handle post's thickness, cm. */
        const val POST = 4.0
    }
}

// ---- geometry (internal) ----

private val E = Role.EQUIPMENT

private fun cap(role: Role, a: Point, b: Point, width: Double) = Capsule(role, a.x, a.y, b.x, b.y, width)

private fun slabPolygon(role: Role, s: Slab): Polygon = Polygon(role, s.corners().flatMap { listOf(it.x, it.y) })

/** The Zercher bar's centre: in the crook of the near elbow, on the inside of the bend, against the arm. */
internal fun crookOf(s: Skeleton): Point {
    val l = s.near
    val u = (l.shoulder - l.elbow).let { it * (1.0 / it.length()) }
    val f = (l.wrist - l.elbow).let { it * (1.0 / it.length()) }
    val b = (u + f).let { it * (1.0 / it.length()) }
    return l.elbow + b * (Body.FOREARM_WIDTH / 2 + Barbell.SHAFT_RADIUS + 1.0)
}

// HackSquatMachine

private fun HackSquatMachine.up() = dir(180.0 - angle)
private fun HackSquatMachine.forward() = dir(angle)

/** The back pad: along the rail behind the torso, its front face [Body.TORSO_RADIUS] behind the torso's axis. */
internal fun HackSquatMachine.padSlab(): Slab {
    val c = pelvis + up() * ((HackSquatMachine.PAD_HIGH - HackSquatMachine.PAD_LOW) / 2) -
        forward() * (Body.TORSO_RADIUS + HackSquatMachine.PAD_THICKNESS / 2)
    return Slab(c.x, c.y, HackSquatMachine.PAD_LOW + HackSquatMachine.PAD_HIGH, HackSquatMachine.PAD_THICKNESS, 180.0 - angle)
}

internal fun HackSquatMachine.plateSlab(): Slab {
    val along = (HackSquatMachine.PLATE_LENGTH / 2 - HackSquatMachine.PLATE_BACK)
    val c = origin + forward() * along - up() * (HackSquatMachine.PLATE_THICKNESS / 2)
    return Slab(c.x, c.y, HackSquatMachine.PLATE_LENGTH, HackSquatMachine.PLATE_THICKNESS, angle)
}

/** The shoulder pad's centre: over the top of the shoulder, on the torso's axis just past the shoulder joint. */
internal fun HackSquatMachine.shoulderPad(): Point =
    pelvis + up() * (Segment.TORSO.length + 5.0 + HackSquatMachine.SHOULDER_PAD_RADIUS)

/** The rail: parallel to the pad, 26 cm behind the torso's axis, from near the floor to its top. */
private fun HackSquatMachine.rail(): Pair<Point, Point> {
    val base = pelvis + forward() * -(Body.TORSO_RADIUS + 16.0)
    // From 12 cm above the floor up to 190 cm, along the rail's line.
    fun at(y: Double) = base + up() * ((y - base.y) / up().y)
    return at(-12.0) to at(-190.0)
}

internal fun HackSquatMachine.drawOf(): List<Placed> = buildList {
    val (low, high) = rail()
    val plate = plateSlab()
    val plateLow = Point(plate.cx, plate.cy) - forward() * (plate.length / 2 - 6.0) - up() * (plate.thickness / 2)
    // The base along the floor, the rail on its post, the plate on a strut from the base.
    add(Placed(Capsule(E, minOf(low.x, plateLow.x) - 15.0, -1.5, maxOf(low.x, plateLow.x) + 30.0, -1.5, 3.0)))
    add(Placed(cap(E, high, Point(high.x, -1.5), 6.0), fit = false))
    add(Placed(cap(E, low, high, 7.0), fit = false))
    add(Placed(cap(E, plateLow, Point(plateLow.x, -1.5), 5.0)))
    // The carriage: two brackets from the back of the pad to the rail.
    val pad = padSlab()
    val padBack = Point(pad.cx, pad.cy) - forward() * (pad.thickness / 2)
    for (k in listOf(-0.3, 0.3)) {
        val a = padBack + up() * (pad.length * k)
        add(Placed(cap(E, a, a - forward() * (16.0 - HackSquatMachine.PAD_THICKNESS / 2), 4.0)))
    }
    add(Placed(slabPolygon(E, pad)))
    add(Placed(slabPolygon(Role.GAP, plate.copy(length = plate.length + 3.0, thickness = plate.thickness + 3.0))))
    add(Placed(slabPolygon(E, plate)))
}

/** The shoulder pad over the shoulder, and its stem from the back pad: in front of the shoulder, set off by a gap. */
internal fun HackSquatMachine.drawFrontOf(): List<Placed> {
    val p = shoulderPad()
    val stem = p - forward() * (Body.TORSO_RADIUS + 2.0)
    val r = HackSquatMachine.SHOULDER_PAD_RADIUS
    return listOf(
        Placed(cap(Role.GAP, stem, p, 4.0 + Body.GAP_EXTRA)),
        Placed(cap(E, stem, p, 4.0)),
        Placed(Circle(Role.GAP, p.x, p.y, r + Body.GAP_EXTRA / 2)),
        Placed(Circle(E, p.x, p.y, r)),
    )
}

// LeverSquatMachine

internal fun LeverSquatMachine.drawOf(): List<Placed> = buildList {
    // The base under the lifter and the pivot's post; the lever from the pivot to the yoke, behind the body.
    add(Placed(Capsule(E, pivot.x - 15.0, -1.5, pad.x + 45.0, -1.5, 3.0)))
    add(Placed(cap(E, pivot, Point(pivot.x, -1.5), 7.0)))
    add(Placed(cap(E, pivot, pad, 6.0)))
    // A plate horn on the lever near the pivot.
    val along = (pad - pivot).let { it * (1.0 / it.length()) }
    val horn = pivot + along * 25.0
    add(Placed(cap(E, horn, horn + dir(angleOf(along) + 90.0) * 12.0, 4.0)))
    add(Placed(Circle(E, pivot.x, pivot.y, 5.0)))
}

/** The yoke across the shoulders and its handle forward: in front of the shoulders, set off by a gap. */
internal fun LeverSquatMachine.drawFrontOf(): List<Placed> {
    val r = LeverSquatMachine.PAD_RADIUS
    return listOf(
        Placed(Circle(Role.GAP, pad.x, pad.y, r + Body.GAP_EXTRA / 2)),
        Placed(Circle(E, pad.x, pad.y, r)),
    )
}

// LegExtensionMachine

internal fun LegExtensionMachine.seatSlab(): Slab =
    Slab.box(seatFront - LegExtensionMachine.SEAT_LENGTH, -seatHeight, seatFront, -seatHeight + FlatBench.PAD)

/** The back pad, hinged at the seat's rear top corner, [backAngle]° from level, its front face from the hinge. */
internal fun LegExtensionMachine.backSlab(): Slab {
    val hinge = Point(seatFront - LegExtensionMachine.SEAT_LENGTH, -seatHeight)
    val along = 180.0 - backAngle
    val c = hinge + dir(along) * (LegExtensionMachine.BACK_LENGTH / 2) + dir(along + 90.0) * (FlatBench.PAD / 2)
    return Slab(c.x, c.y, LegExtensionMachine.BACK_LENGTH, FlatBench.PAD, along)
}

internal fun LegExtensionMachine.rollerSlabs(): List<Slab> =
    listOfNotNull(roller, farRoller).map { Slab(it.x, it.y, 0.0, 0.0, radius = LegExtensionMachine.ROLLER_RADIUS) }

internal fun LegExtensionMachine.drawOf(): List<Placed> = buildList {
    val seat = seatSlab()
    val mid = seatFront - LegExtensionMachine.SEAT_LENGTH / 2
    add(Placed(Capsule(E, mid - 30.0, -1.5, pivot.x + 20.0, -1.5, 3.0)))
    add(Placed(Capsule(E, mid, -seatHeight + FlatBench.PAD, mid, -1.5, 6.0)))
    // The frame from under the seat's front up to the pivot's housing, beside the knee.
    add(Placed(cap(E, Point(seatFront - 4.0, -seatHeight + FlatBench.PAD + 2.0), pivot, 5.0)))
    // The side handle, beside the seat under the near hand.
    add(Placed(cap(E, Point(handle.x - 6.0, -seatHeight + FlatBench.PAD), handle, 3.0)))
    add(Placed(RoundRect(E, seat.cx - seat.length / 2, seat.cy - seat.thickness / 2, seat.length, seat.thickness, 2.0)))
    add(Placed(slabPolygon(E, backSlab())))
    farRoller?.let {
        add(Placed(cap(E, pivot, it, 4.0)))
        add(Placed(Circle(E, it.x, it.y, LegExtensionMachine.ROLLER_RADIUS)))
    }
}

/** The near lever from the pivot down to the roller, and the roller: beside and in front of the shin. */
internal fun LegExtensionMachine.drawFrontOf(): List<Placed> {
    val r = LegExtensionMachine.ROLLER_RADIUS
    return listOf(
        Placed(cap(Role.GAP, pivot, roller, 4.0 + Body.GAP_EXTRA)),
        Placed(cap(E, pivot, roller, 4.0)),
        Placed(Circle(Role.GAP, roller.x, roller.y, r + Body.GAP_EXTRA / 2)),
        Placed(Circle(E, roller.x, roller.y, r)),
        Placed(Circle(Role.GAP, pivot.x, pivot.y, 3.5 + Body.GAP_EXTRA / 2)),
        Placed(Circle(E, pivot.x, pivot.y, 3.5)),
    )
}

// PushSled

/** The handle post, held: from the skid up to [PushSled.handleTop]. */
internal fun PushSled.postSlab(): Slab = Slab.box(x - PushSled.POST / 2, -handleTop, x + PushSled.POST / 2, -12.0)

internal fun PushSled.drawOf(): List<Placed> = buildList {
    // The skid on the floor, the handle post at its rear, and a plate post in the middle with plates stacked on it.
    add(Placed(RoundRect(E, x - 4.0, -12.0, PushSled.SKID + 4.0, 10.5, 2.0)))
    val post = postSlab()
    add(Placed(RoundRect(E, post.cx - post.length / 2, post.cy - post.thickness / 2, post.length, post.thickness, 2.0)))
    val px = x + PushSled.SKID / 2
    add(Placed(Capsule(E, px, -12.0, px, -42.0, 4.0)))
    for (i in 0 until 3) {
        val top = -12.0 - (i + 1) * 5.0
        add(Placed(RoundRect(Role.ACCENT_FILL, px - Barbell.PLATE_DIAMETER / 2, top, Barbell.PLATE_DIAMETER, 4.2, 1.5)))
        add(Placed(RoundRect(Role.ACCENT, px - Barbell.PLATE_DIAMETER / 2, top, Barbell.PLATE_DIAMETER, 1.2, 0.6)))
    }
}
