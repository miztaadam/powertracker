package com.powertracker.domain.library.figures

// Kit pieces the calf and core batch needs (Task 12), at real-world scale in centimetres. Sizes are typical,
// manufacturer-neutral values, each noted on its piece. Kit.kt's surfaces, solids and drawing hand these pieces to the
// functions below. The batch also reuses, unchanged: the deficit platform as the calf block or step (10 cm tall, a
// typical calf block), the 45° leg press, the Smith machine, the power rack, the flat bench, the stability ball, the
// cable stack with its handle and rope, the dumbbell and the kettlebell. The old pieces' drawing is unchanged.

/**
 * A standing calf raise machine seen from the side: an upright column on a base at [columnX] behind the lifter, a
 * lever that turns about [pivot] (on the column) to the shoulder pad resting on top of the shoulders at [pad] (in this
 * panel; the figure sets the pivot so the lever is the same length in both panels), and handles at [handle] on the
 * pad's frame, in front of the shoulders. The calf block is a separate piece ([DeficitPlatform]). A typical machine:
 * shoulder pads about 12 cm across, a column about 200 cm tall; neutral sizes.
 */
data class StandingCalfMachine(val columnX: Double, val pivot: Point, val pad: Point, val handle: Point) : FixedProp {
    companion object {
        /** The shoulder pad's radius, cm (a 12 cm pad). */
        const val PAD_RADIUS = 6.0
        const val HEIGHT = 200.0
    }
}

/**
 * A seated calf raise machine seen from the side: a seat whose middle is at [seatX], its top [seatHeight] up, and a
 * lever that turns about [pivot] (low, in front of the shins) to the knee pad resting on the lower thighs at [pad] (in
 * this panel; the pivot is set so the lever is the same length in both panels). The foot platform is a separate piece
 * ([DeficitPlatform]). A typical machine: a 40 cm seat, a knee pad about 12 cm across; neutral sizes.
 */
data class SeatedCalfMachine(val seatX: Double, val seatHeight: Double, val pivot: Point, val pad: Point) : FixedProp {
    companion object {
        const val SEAT_LENGTH = 40.0

        /** The knee pad's radius, cm (a 12 cm pad). */
        const val PAD_RADIUS = 6.0
    }
}

/**
 * A donkey calf raise machine seen from the side: a column in front of the lifter on a base at [columnX], carrying a
 * level forearm support whose top is [supportHeight] up, running back from the column for [supportLength]; and a
 * lever that turns about [pivot] (on the column) to the pad resting across the lower back and hips at [pad] (in this
 * panel; the pivot is set so the lever is the same length in both panels). The calf block is a separate piece
 * ([DeficitPlatform]). A typical machine: a forearm support about 40 cm long, a hip pad about 14 cm across; neutral.
 */
data class DonkeyCalfMachine(
    val columnX: Double,
    val supportHeight: Double,
    val supportLength: Double,
    val pivot: Point,
    val pad: Point,
) : FixedProp {
    companion object {
        /** The hip pad's radius, cm (a 14 cm pad). */
        const val PAD_RADIUS = 7.0
        const val SUPPORT_THICKNESS = 6.0
    }
}

/**
 * A seated crunch machine (chest pad) seen from the side: a seat whose front edge is at [seatFront], its top
 * [seatHeight] up; an upright hip pad behind the hips at [hipPadX] (its front face); foot rollers at [feet]; and a
 * lever that turns about [pivot] to the chest pad on the upper chest at [pad] (in this panel; the pivot is set so the
 * lever is the same length in both panels), drawn in front of the chest. A typical machine: a 40 cm seat about 45 cm
 * up, a hip pad 30 cm tall, a chest pad about 14 cm across; neutral sizes. The lever runs beside the lifter from the
 * pivot (at the hips' axis on the figure) to the pad, so it is drawn in front of the body with the pad (fix round 1).
 */
data class CrunchMachine(
    val seatFront: Double,
    val seatHeight: Double,
    val hipPadX: Double,
    val feet: Point,
    val pivot: Point,
    val pad: Point,
) : FixedProp {
    companion object {
        const val SEAT_LENGTH = 40.0
        const val HIP_PAD_HEIGHT = 30.0
        const val HIP_PAD_THICKNESS = 6.0

        /** The chest pad's and foot roller's radius, cm (14 cm and 10 cm). */
        const val PAD_RADIUS = 7.0
        const val ROLLER_RADIUS = 5.0
    }
}

/**
 * A support to hold for balance: an upright post on a base at [x], with a level grip at [gripHeight] above the floor
 * (a rack's upright or a wall rail; a typical rail about 100 cm up, a 4 cm post). Its grip is a [Surface.HANDLE].
 */
data class SupportPost(val x: Double, val gripHeight: Double) : FixedProp {
    companion object {
        const val HEIGHT = 150.0
    }
}

/**
 * An ab wheel in both hands: the handles run through the wheel's hub, so the grip is at the hub, and the wheel
 * [RADIUS] round it rests on the floor. A typical ab wheel is about 18 cm across; neutral.
 */
data object AbWheel : HeldProp {
    const val RADIUS = 9.0
}

/**
 * A weight plate pinched by its rim in one hand ([hand]): its top edge in the hand, so its centre hangs [RADIUS] −
 * [PINCH] below the grip. Its face is parallel to the body's side, so the side view sees the face (a disc) and the
 * front view its edge. A typical 10 kg bumper plate is 45 cm across (the standard full-size disc) and about 3 cm thick
 * at its rim (neutral); fix round 1 drew it at that size, so it reads as a plate at thumbnail size.
 */
data class PinchPlate(val hand: Side) : HeldProp {
    companion object {
        const val RADIUS = 22.5
        const val THICKNESS = 3.0

        /** How far the fingers reach down the plate's face from its rim, cm. */
        const val PINCH = 3.0
    }
}

// ---- geometry (internal) ----

private val E = Role.EQUIPMENT

private fun cap(role: Role, a: Point, b: Point, width: Double) = Capsule(role, a.x, a.y, b.x, b.y, width)

private fun box(role: Role, s: Slab, radius: Double = 2.0) =
    RoundRect(role, s.cx - s.length / 2, s.cy - s.thickness / 2, s.length, s.thickness, radius)

private fun roller(p: Point, r: Double) = Slab(p.x, p.y, 0.0, 0.0, radius = r)

/** A pad (roller) drawn in front: a gap ring, then the pad. */
private fun frontRoller(p: Point, r: Double): List<Placed> =
    listOf(Placed(Circle(Role.GAP, p.x, p.y, r + Body.GAP_EXTRA / 2)), Placed(Circle(E, p.x, p.y, r)))

// StandingCalfMachine

internal fun StandingCalfMachine.padSlab(): Slab = roller(pad, StandingCalfMachine.PAD_RADIUS)

internal fun StandingCalfMachine.handleSlab(): Slab = roller(handle, 1.6)

internal fun StandingCalfMachine.drawOf(): List<Placed> = buildList {
    add(Placed(Capsule(E, columnX - 25.0, -1.5, columnX + 25.0, -1.5, 3.0)))
    add(Placed(Capsule(E, columnX, -1.5, columnX, -StandingCalfMachine.HEIGHT, 7.0)))
    // The lever from the pivot on the column to the pad, behind the body, and the handle's strut from the pad.
    add(Placed(cap(E, pivot, pad, 5.0)))
    add(Placed(Circle(E, pivot.x, pivot.y, 4.0)))
    add(Placed(cap(E, pad, handle, 3.0)))
}

/** The shoulder pad on top of the shoulders and the handle's grip over the hand, set off by gap rings. */
internal fun StandingCalfMachine.drawFrontOf(): List<Placed> = frontRoller(pad, StandingCalfMachine.PAD_RADIUS) + frontRoller(handle, 2.2)

// SeatedCalfMachine

internal fun SeatedCalfMachine.seatSlab(): Slab =
    Slab.box(seatX - SeatedCalfMachine.SEAT_LENGTH / 2, -seatHeight, seatX + SeatedCalfMachine.SEAT_LENGTH / 2, -seatHeight + FlatBench.PAD)

internal fun SeatedCalfMachine.padSlab(): Slab = roller(pad, SeatedCalfMachine.PAD_RADIUS)

internal fun SeatedCalfMachine.drawOf(): List<Placed> = buildList {
    add(Placed(Capsule(E, seatX - 25.0, -1.5, pivot.x + 15.0, -1.5, 3.0)))
    add(Placed(Capsule(E, seatX, -seatHeight + FlatBench.PAD, seatX, -1.5, 6.0)))
    // The pivot's post in front of the platform, standing on the base.
    add(Placed(cap(E, Point(pivot.x, -1.5), pivot, 6.0)))
    add(Placed(box(E, seatSlab())))
}

/** The lever from the pivot to the knee pad and the pad: beside and over the legs, so drawn in front. */
internal fun SeatedCalfMachine.drawFrontOf(): List<Placed> = listOf(
    Placed(cap(Role.GAP, pivot, pad, 4.0 + Body.GAP_EXTRA)),
    Placed(cap(E, pivot, pad, 4.0)),
) + frontRoller(pad, SeatedCalfMachine.PAD_RADIUS) + frontRoller(pivot, 3.5)

// DonkeyCalfMachine

internal fun DonkeyCalfMachine.supportSlab(): Slab = Slab.box(
    columnX - supportLength, -supportHeight, columnX, -supportHeight + DonkeyCalfMachine.SUPPORT_THICKNESS,
)

internal fun DonkeyCalfMachine.padSlab(): Slab = roller(pad, DonkeyCalfMachine.PAD_RADIUS)

internal fun DonkeyCalfMachine.drawOf(): List<Placed> = buildList {
    add(Placed(Capsule(E, columnX - 20.0, -1.5, columnX + 25.0, -1.5, 3.0)))
    add(Placed(Capsule(E, columnX, -1.5, columnX, minOf(pivot.y, -supportHeight) - 4.0, 7.0)))
    add(Placed(cap(E, pivot, pad, 5.0)))
    add(Placed(Circle(E, pivot.x, pivot.y, 4.0)))
}

/** The forearm support (the forearms lie on top of it) and the hip pad on the back, drawn in front with gaps. */
internal fun DonkeyCalfMachine.drawFrontOf(): List<Placed> = frontRoller(pad, DonkeyCalfMachine.PAD_RADIUS) + listOf(
    Placed(box(Role.GAP, supportSlab().let { it.copy(length = it.length + Body.GAP_EXTRA, thickness = it.thickness + Body.GAP_EXTRA) }, 3.0)),
    Placed(box(E, supportSlab())),
)

// CrunchMachine

internal fun CrunchMachine.seatSlab(): Slab =
    Slab.box(seatFront - CrunchMachine.SEAT_LENGTH, -seatHeight, seatFront, -seatHeight + FlatBench.PAD)

internal fun CrunchMachine.hipPadSlab(): Slab = Slab.box(
    hipPadX - CrunchMachine.HIP_PAD_THICKNESS, -seatHeight - CrunchMachine.HIP_PAD_HEIGHT, hipPadX, -seatHeight,
)

internal fun CrunchMachine.padSlab(): Slab = roller(pad, CrunchMachine.PAD_RADIUS)

internal fun CrunchMachine.drawOf(): List<Placed> = buildList {
    val mid = seatFront - CrunchMachine.SEAT_LENGTH / 2
    add(Placed(Capsule(E, hipPadX - 30.0, -1.5, feet.x + 15.0, -1.5, 3.0)))
    add(Placed(Capsule(E, mid, -seatHeight + FlatBench.PAD, mid, -1.5, 6.0)))
    // The frame post behind the hip pad up to the pivot's height, and the beam to the pivot (behind the body).
    val postX = hipPadX - CrunchMachine.HIP_PAD_THICKNESS - 6.0
    add(Placed(Capsule(E, postX, -1.5, postX, pivot.y, 6.0)))
    add(Placed(cap(E, Point(postX, pivot.y), pivot, 5.0)))
    // The foot rollers' stem from under the seat's front.
    add(Placed(cap(E, Point(seatFront - 4.0, -seatHeight + FlatBench.PAD), feet, 3.5)))
    add(Placed(Circle(E, feet.x, feet.y, CrunchMachine.ROLLER_RADIUS)))
    add(Placed(box(E, hipPadSlab())))
    add(Placed(box(E, seatSlab())))
}

/** The lever from the pivot to the chest pad, the pad over the upper chest and the pivot's hub, set off by gaps. */
internal fun CrunchMachine.drawFrontOf(): List<Placed> = listOf(
    Placed(cap(Role.GAP, pivot, pad, 4.0 + Body.GAP_EXTRA)),
    Placed(cap(E, pivot, pad, 4.0)),
) + frontRoller(pad, CrunchMachine.PAD_RADIUS) + frontRoller(pivot, 4.0)

// SupportPost

internal fun SupportPost.gripSlab(): Slab = Slab(x - 6.0, -gripHeight, 12.0, 3.0)

internal fun SupportPost.drawOf(): List<Placed> = listOf(
    Placed(Capsule(E, x - 15.0, -1.5, x + 15.0, -1.5, 3.0)),
    Placed(Capsule(E, x, -1.5, x, -SupportPost.HEIGHT, 4.0)),
)

/** The grip's end, seen end on over the hand (as the side view's pull-up bar), with a gap ring. */
internal fun SupportPost.drawFrontOf(): List<Placed> = listOf(
    Placed(Capsule(Role.GAP, x - 12.0, -gripHeight, x, -gripHeight, 3.5 + Body.GAP_EXTRA)),
    Placed(Capsule(E, x - 12.0, -gripHeight, x, -gripHeight, 3.5)),
)

// AbWheel

internal fun AbWheel.drawOn(s: Skeleton): List<Shape> {
    val c = if (s.view == View.SIDE) s.near.grip else (s.near.grip + s.far.grip) * 0.5
    return listOf(
        Circle(Role.ACCENT_FILL, c.x, c.y, AbWheel.RADIUS),
        ring(Role.ACCENT, c.x, c.y, AbWheel.RADIUS, AbWheel.RADIUS - 2.2),
        Circle(Role.ACCENT, c.x, c.y, 2.0),
    )
}

// PinchPlate

internal fun PinchPlate.centerOn(s: Skeleton): Point = s.side(hand).grip + Point(0.0, PinchPlate.RADIUS - PinchPlate.PINCH)

internal fun PinchPlate.drawOn(s: Skeleton): List<Shape> {
    val c = centerOn(s)
    val r = PinchPlate.RADIUS
    return if (s.view == View.SIDE) {
        listOf(Circle(Role.ACCENT_FILL, c.x, c.y, r), ring(Role.ACCENT, c.x, c.y, r, r - 2.0), ring(Role.ACCENT, c.x, c.y, 5.5, 4.0))
    } else {
        listOf(RoundRect(Role.ACCENT, c.x - PinchPlate.THICKNESS / 2, c.y - r, PinchPlate.THICKNESS, 2 * r, 1.2))
    }
}
