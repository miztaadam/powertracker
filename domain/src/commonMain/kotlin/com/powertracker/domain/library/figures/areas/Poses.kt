package com.powertracker.domain.library.figures.areas

import com.powertracker.domain.library.figures.Arm
import com.powertracker.domain.library.figures.Body
import com.powertracker.domain.library.figures.PowerRack
import com.powertracker.domain.library.figures.Leg
import com.powertracker.domain.library.figures.Point
import com.powertracker.domain.library.figures.Pose
import com.powertracker.domain.library.figures.Reach
import com.powertracker.domain.library.figures.Segment
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

// Small helpers the area files share to build poses from where the feet, hands and bar go. They only compute joint
// angles; proportions come from the one body (Body.kt).

internal val BOTH = com.powertracker.domain.library.figures.Which.entries.toSet()

/** The screen angle of the vector [v] in degrees (counter-clockwise, y down). */
internal fun angleTo(v: Point): Double = atan2(-v.y, v.x) * 180 / PI

/** The unit vector at [degrees]. */
internal fun unit(degrees: Double): Point = (degrees * PI / 180).let { Point(cos(it), -sin(it)) }

internal fun fold180(degrees: Double): Double {
    var a = degrees % 360.0
    if (a <= -180.0) a += 360.0
    if (a > 180.0) a -= 360.0
    return a
}

/** The joints' positions of a standing leg, worked out from the floor up. */
internal data class StandingLeg(val ankle: Point, val knee: Point, val hip: Point, val leg: Leg)

/**
 * A leg with the foot flat on the floor, its ankle joint at [ankleX], the shin leaning [shinLean]° forward of vertical
 * (that is the ankle's dorsiflexion) and the knee bent [knee]°, for a torso at [torso]°.
 */
internal fun standingLeg(ankleX: Double, shinLean: Double, knee: Double, torso: Double): StandingLeg {
    val ankle = Point(ankleX, -Body.ANKLE_HEIGHT)
    val up = 90.0 - shinLean
    val kneeP = ankle + unit(up) * Segment.SHIN.length
    val hip = kneeP + unit(up + knee) * Segment.THIGH.length
    val thigh = up + knee + 180.0 // hip to knee
    return StandingLeg(ankle, kneeP, hip, Leg(hip = fold180(thigh - torso - 180.0), knee = knee, ankle = shinLean))
}

/** A pose standing on [leg] with the torso at [torso]°. */
internal fun standOn(leg: StandingLeg, torso: Double, nearArm: Arm, farArm: Arm = nearArm, neck: Double = 0.0) =
    Pose(leg.hip.x, leg.hip.y, torso, neck, nearArm, leg.leg, farArm, leg.leg)

/** The shoulder joint of a torso at [torso]° rising from [pelvis]. */
internal fun shoulderOf(pelvis: Point, torso: Double): Point = pelvis + unit(torso) * Segment.TORSO.length

/** The straight arm from [shoulder] pointing at [target] (its grip lands on the target if it is at arm's length). */
internal fun straightArm(shoulder: Point, target: Point, torso: Double): Arm =
    Arm(shoulder = fold180(angleTo(target - shoulder) - torso - 180.0), elbow = 0.0)

/** Shoulder to grip with the arm straight. */
internal val ARM_REACH = Segment.UPPER_ARM.length + Segment.FOREARM.length + Segment.HAND.length * Body.GRIP

/**
 * A straight arm hanging from [shoulder] down to the height where its grip is at [x] (the arm angled to clear the
 * thighs or shins).
 */
internal fun hangingArmAt(shoulder: Point, x: Double, torso: Double): Arm {
    val a = -acos(((x - shoulder.x) / ARM_REACH).coerceIn(-1.0, 1.0)) * 180 / PI
    return Arm(shoulder = fold180(a - torso - 180.0), elbow = 0.0)
}

/**
 * The most the figure bends an elbow to bring the grip in close (the joint limit is 150°). With fixed segment lengths
 * in a side view, a grip close to the shoulder (a bar on the back or at the front of the neck) cannot be reached: the
 * real arm is foreshortened because it points partly towards or away from the camera.
 */
internal const val MAX_ELBOW = 148.0

/**
 * An arm whose grip lands on [target], or, when [target] is closer to the shoulder than the arm can fold
 * ([MAX_ELBOW]), as near to it as it can along the same line.
 */
internal fun armTo(shoulder: Point, target: Point, torso: Double, wrist: Double = 0.0, turnedOut: Boolean = false): Arm {
    val g = Segment.HAND.length * Body.GRIP
    val w = wrist * PI / 180
    val link = hypot(Segment.FOREARM.length + g * cos(w), g * sin(w))
    val a = Segment.UPPER_ARM.length
    val inner = (180.0 - MAX_ELBOW) * PI / 180
    val minReach = kotlin.math.sqrt(a * a + link * link - 2 * a * link * cos(inner)) + 0.05
    val d = target - shoulder
    val dist = d.length()
    val reachable = if (dist < minReach) shoulder + d * (minReach / max(dist, 1e-9)) else target
    return Reach.arm(shoulder, reachable, torso, wrist, turnedOut)
}

/**
 * A rack the lifter has stepped back from: posts [gap] cm in front of the bar, J-hooks and safety arms reaching back
 * to 8 cm short of the bar, under its plates, so the arms show under the bar's path without being drawn through the
 * lifter's legs (in the rack they run on past the body, out of this side view's way).
 */
internal fun rackInFront(barX: Double, jHookHeight: Double, safetyHeight: Double, gap: Double = 40.0) =
    PowerRack(x = barX + gap, jHookHeight = jHookHeight, safetyHeight = safetyHeight, armLength = gap - 8.0, lifterLeft = true)

// ---- 3D arms (shared by the chest and back figures) ----

/** A point or direction in 3D: x and y as in the picture (y down), z out of the picture towards the camera. */
internal data class V3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(o: V3) = V3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: V3) = V3(x - o.x, y - o.y, z - o.z)
    operator fun times(k: Double) = V3(x * k, y * k, z * k)
    fun dot(o: V3) = x * o.x + y * o.y + z * o.z
    fun length() = sqrt(dot(this))
    fun unit() = this * (1.0 / length())
    val flat: Point get() = Point(x, y)
}

internal fun v3(p: Point, z: Double) = V3(p.x, p.y, z)

internal const val DEG = 180 / PI

/** Shoulder to grip along the forearm and hand, with the wrist straight. */
internal val FORE_LINK = Segment.FOREARM.length + Segment.HAND.length * Body.GRIP

/**
 * The arm whose grip lands on [grip] from [shoulder] (3D), its elbow on the side [hint] points to, for a torso at
 * [torso]°: the upper arm's and forearm's angles out of the picture become [Arm.out] and [Arm.forearmOut]. In the
 * front view the far arm is drawn as the near one's mirror image, so its points are passed mirrored ([mirror]).
 */
internal fun arm3(shoulder: V3, grip: V3, hint: V3, torso: Double): Arm {
    val a = Segment.UPPER_ARM.length
    val b = FORE_LINK
    val g = grip - shoulder
    val d = g.length()
    require(d <= a + b + 1e-6 && d >= abs(a - b) - 1e-6) { "grip out of reach: ${(d * 10).toLong() / 10.0} cm" }
    val gu = g.unit()
    val alpha = acos(((a * a + d * d - b * b) / (2 * a * d)).coerceIn(-1.0, 1.0))
    val h = hint - gu * hint.dot(gu)
    val n = if (h.length() < 1e-9) V3(0.0, 1.0, 0.0) else h.unit()
    val elbow = shoulder + (gu * cos(alpha) + n * sin(alpha)) * a
    val u = (elbow - shoulder).unit()
    val f = (grip - elbow).unit()
    return armFrom(u, f, torso)
}

/** The [Arm] for a 3D upper arm [u] and forearm [f] (unit vectors), the wrist straight. */
internal fun armFrom(u: V3, f: V3, torso: Double): Arm {
    val upper = angleTo(u.flat)
    val fore = if (f.flat.length() < 1e-9) upper else angleTo(f.flat)
    return Arm(
        shoulder = fold180(upper - torso - 180.0),
        elbow = fold180(fore - upper),
        out = asin(u.z.coerceIn(-1.0, 1.0)) * DEG,
        forearmOut = asin(f.z.coerceIn(-1.0, 1.0)) * DEG,
    )
}

/** A 3D point mirrored across the vertical line x = [axis] (the front view's far side drawn as the near one). */
internal fun V3.mirror(axis: Double) = V3(2 * axis - x, y, z)
