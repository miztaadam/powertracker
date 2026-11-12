package com.powertracker.domain.library.figures

// Kit pieces the shoulder batch needs (Task 8), at real-world scale in centimetres. Sizes are typical, manufacturer-
// neutral values, each noted on its piece. Kit.kt's surfaces, solids and drawing hand these pieces to the functions
// below. The shoulder batch also reuses, with new options that default to the old drawing: PressMachine
// (handleEndOn), PecDeck (facingPad), CableStack (crossed, and cableInFront in a front view's single column).

/** How a kettlebell is held: racked at the shoulder, or locked out overhead. Either way the bell rests on the forearm. */
enum class KettlebellHold { RACK, OVERHEAD }

/**
 * A kettlebell in one hand ([hand]), its handle in the palm and the bell resting on the back of the forearm, by the
 * wrist. Side view: [KettlebellHold.RACK] the bell sits just in front of the upright forearm (it rests on the
 * forearm's outside, which faces forward and out in the rack; drawn in front so it does not vanish into the body);
 * [KettlebellHold.OVERHEAD] it hangs behind the forearm (the palm faces forward, so the back of the forearm faces
 * back). A typical 16 kg kettlebell: the bell about 21 cm across, about
 * 28 cm tall with its handle, the handle about 3.3 cm thick; neutral sizes. Its [center] for the checks is the handle
 * in the hand.
 */
data class Kettlebell(val hand: Side, val hold: KettlebellHold) : HeldProp {
    companion object {
        /** The bell's radius, cm (a 21 cm bell). */
        const val BELL_RADIUS = 10.5

        /** The handle's drawn thickness, cm. */
        const val HANDLE = 3.0
    }
}

/**
 * A seated lateral raise machine seen from the front, centred at [x]: a seat [seatHeight] up, a back pad behind the
 * lifter up to [padTop], and on each side a lever that pivots at the shoulder joint's height [pivotHeight],
 * [Body.SHOULDER_HALF_WIDTH] either side of the middle (the lever's axis lines up with the shoulder joint, front to back),
 * carrying an arm pad that rests on the outside of the upper arm by the elbow. The pads and levers are drawn in
 * front of the arms. A typical machine: a 40 cm seat about 45 cm up, a 34 cm wide back pad, 20 cm arm pads about
 * 9 cm thick; neutral sizes.
 */
data class LateralRaiseMachine(val x: Double, val seatHeight: Double, val padTop: Double, val pivotHeight: Double) : FixedProp {
    companion object {
        /** The arm pad's length along the upper arm, cm. */
        const val PAD_LENGTH = 20.0

        /** The arm pad's radius (half its 9 cm thickness), cm. */
        const val PAD_RADIUS = 4.5
    }
}

// ---- geometry (internal) ----

private val E = Role.EQUIPMENT
private val A = Role.ACCENT

private fun cap(role: Role, a: Point, b: Point, width: Double) = Capsule(role, a.x, a.y, b.x, b.y, width)

private fun unitOf(v: Point): Point = v * (1.0 / v.length())

/** The bell's centre: by the wrist, on the back of the forearm (see [Kettlebell]). */
internal fun Kettlebell.bellCentre(s: Skeleton): Point {
    val l = s.side(hand)
    val f = unitOf(l.wrist - l.elbow)
    // The forearm's back, in the picture: a quarter turn from the forearm, towards -x for a forearm pointing up.
    val back = dir(angleOf(f) + 90.0)
    return when (hold) {
        KettlebellHold.RACK -> l.wrist - f * 4.0 - back * (Body.FOREARM_WIDTH / 2 + Kettlebell.BELL_RADIUS - 3.0)
        KettlebellHold.OVERHEAD -> l.wrist - f * 4.0 + back * (Body.FOREARM_WIDTH / 2 + Kettlebell.BELL_RADIUS - 1.5)
    }
}

internal fun Kettlebell.drawOn(s: Skeleton): List<Shape> {
    val g = s.side(hand).grip
    val c = bellCentre(s)
    val r = Kettlebell.BELL_RADIUS
    // The handle: two horns from the bell's shoulders up to the grip, either side of the line from the bell to it.
    val up = angleOf(g - c)
    val horns = listOf(-40.0, 40.0).map { c + dir(up + it) * (r - 1.0) }
    return buildList {
        for (h in horns) add(cap(A, h, g, Kettlebell.HANDLE))
        add(Circle(Role.ACCENT_FILL, c.x, c.y, r))
        add(ring(A, c.x, c.y, r, r - 1.3))
    }
}

// LateralRaiseMachine

internal fun LateralRaiseMachine.seatSlab(): Slab = Slab.box(x - 20.0, -seatHeight, x + 20.0, -seatHeight + FlatBench.PAD)

/** The pivot of the lever on [side]: at the shoulder joint's height, beside the middle by the shoulders' half width. */
internal fun LateralRaiseMachine.pivot(side: Side): Point =
    Point(x + (if (side == Side.NEAR) 1.0 else -1.0) * Body.SHOULDER_HALF_WIDTH, -pivotHeight)

/** The arm pad's two ends on [side]: on the outside of the upper arm, by the elbow. */
internal fun LateralRaiseMachine.armPad(side: Side, s: Skeleton): Pair<Point, Point> {
    val l = s.side(side)
    val u = unitOf(l.elbow - l.shoulder)
    // The outside of the arm: away from the body when it hangs, on top when it is raised.
    val out = dir(angleOf(u) + if (side == Side.NEAR) 90.0 else -90.0)
    val mid = l.elbow - u * (LateralRaiseMachine.PAD_LENGTH / 2 - 2.0) + out * (Body.UPPER_ARM_WIDTH / 2 + LateralRaiseMachine.PAD_RADIUS)
    val half = u * (LateralRaiseMachine.PAD_LENGTH / 2 - LateralRaiseMachine.PAD_RADIUS)
    return (mid - half) to (mid + half)
}

internal fun LateralRaiseMachine.drawOf(): List<Placed> = buildList {
    add(Placed(Capsule(E, x, -seatHeight + FlatBench.PAD, x, -1.5, 6.0)))
    add(Placed(Capsule(E, x - 25.0, -1.5, x + 25.0, -1.5, 3.0)))
    add(Placed(RoundRect(E, x - 20.0, -seatHeight, 40.0, FlatBench.PAD, 2.0)))
    // The back pad behind the torso.
    add(Placed(RoundRect(E, x - 17.0, -padTop, 34.0, padTop - seatHeight - 2.0, 3.0)))
}

/** The levers and arm pads: outside the arms, so drawn in front of them, each set off by a gap outline. */
internal fun LateralRaiseMachine.drawFrontOf(s: Skeleton?): List<Placed> = buildList {
    if (s == null || s.view != View.FRONT) return@buildList
    for (side in Side.entries) {
        val p = pivot(side)
        val (a, b) = armPad(side, s)
        add(Placed(cap(Role.GAP, p, b, 4.0 + Body.GAP_EXTRA)))
        add(Placed(cap(E, p, b, 4.0)))
        add(Placed(cap(Role.GAP, a, b, 2 * LateralRaiseMachine.PAD_RADIUS + Body.GAP_EXTRA)))
        add(Placed(cap(E, a, b, 2 * LateralRaiseMachine.PAD_RADIUS)))
        add(Placed(Circle(E, p.x, p.y, 3.0)))
    }
}

// PecDeck facing the pad (a reverse fly)

/** The chest pad of a [PecDeck] with [PecDeck.facingPad]: in front of the torso, from [PecDeck.padTop] down 36 cm. */
internal fun PecDeck.chestPad(): Slab = Slab.box(x - 14.0, -padTop, x + 14.0, -padTop + 36.0)

internal fun PecDeck.drawFrontOf(): List<Placed> = if (!facingPad) emptyList() else chestPad().let { p ->
    listOf(
        Placed(RoundRect(Role.GAP, p.cx - p.length / 2 - 1.5, p.cy - p.thickness / 2 - 1.5, p.length + 3.0, p.thickness + 3.0, 4.0)),
        Placed(RoundRect(E, p.cx - p.length / 2, p.cy - p.thickness / 2, p.length, p.thickness, 3.0)),
    )
}
