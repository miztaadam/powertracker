package com.powertracker.domain.library.figures

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// World units are centimetres with y pointing down; the floor's top is y = 0. Angles are degrees, counter-clockwise
// as seen on screen (the maths convention), so a direction at angle a is (cos a, -sin a) in world units.

/** A point in world centimetres (y down), or in view-box units once rendered. */
data class Point(val x: Double, val y: Double) {
    operator fun plus(o: Point) = Point(x + o.x, y + o.y)
    operator fun minus(o: Point) = Point(x - o.x, y - o.y)
    operator fun times(k: Double) = Point(x * k, y * k)
    fun length(): Double = hypot(x, y)
}

/** The unit vector at [degrees] (counter-clockwise on screen). */
internal fun dir(degrees: Double): Point = (degrees * PI / 180).let { Point(cos(it), -sin(it)) }

/** The screen angle of [v] in degrees, the inverse of [dir]. */
internal fun angleOf(v: Point): Double = atan2(-v.y, v.x) * 180 / PI

/** [degrees] folded into (-180, 180]. */
internal fun fold(degrees: Double): Double {
    var a = degrees % 360.0
    if (a <= -180.0) a += 360.0
    if (a > 180.0) a -= 360.0
    return a
}

/**
 * Body segments as fractions of stature.
 *
 * Source for the limbs: Winter's segment-length table, as quoted on PMC (allowed source):
 * - https://pmc.ncbi.nlm.nih.gov/articles/PMC12637938/ (Joint angle trajectories are robust to segment length
 *   estimation methods in human reaching): humeral 0.186, radioulnar 0.146 and hand 0.108 of height (Winter).
 * - https://pmc.ncbi.nlm.nih.gov/articles/PMC3582351/ (Dynamic sagittal-plane trunk control during ACL injury):
 *   segment lengths "normalized by height as provided by Winter", foot 0.152H and thigh 0.245H; the shank 0.246H is
 *   from the same Winter table (the search excerpt shows the value but not its row label, see the task report).
 *   Cross-check: https://pmc.ncbi.nlm.nih.gov/articles/PMC11233572/ measures adult thigh 0.231 and leg 0.221 of
 *   height (shank close to thigh, as in Winter).
 * Head and torso are derived, not tabled: they split the height left above the hip (1 - ankle 0.039 - shank - thigh
 * = 0.470H) into torso 0.288 (hip joint to shoulder joint), neck 0.052 and head 0.130 (chin to crown), so the
 * standing figure is exactly the stature.
 */
enum class Segment(val fraction: Double) {
    UPPER_ARM(0.186), FOREARM(0.146), HAND(0.108),
    THIGH(0.245), SHIN(0.246), FOOT(0.152),
    TORSO(0.288), NECK(0.052), HEAD(0.130);

    val length: Double get() = fraction * Body.HEIGHT
}

/** The one body every figure uses: proportions never change between exercises. */
object Body {
    const val HEIGHT = 175.0

    /** Ankle joint above the sole (derived drawing value, the 0.039H the derived head and torso leave below the shank). */
    const val ANKLE_HEIGHT = 0.039 * HEIGHT

    /** Where the grip sits along the hand, as a fraction from the wrist (the bar lies across the palm). */
    const val GRIP = 0.45

    // Drawing widths (cm): a solid figure in the draft's proportions. Checks use half of each as the capsule radius.
    const val TORSO_RADIUS = 10.0
    /**
     * The drawn head is a circle 20 cm across (the head's depth in profile), centred halfway up [Segment.HEAD]; the
     * segment itself is the 22.75 cm chin-to-crown height, which sets where the head sits, not how big it is drawn.
     */
    const val HEAD_RADIUS = 10.0
    const val NECK_WIDTH = 9.0
    const val UPPER_ARM_WIDTH = 10.0
    const val FOREARM_WIDTH = 8.5
    const val HAND_WIDTH = 6.0
    const val THIGH_WIDTH = 14.0
    const val SHIN_WIDTH = 11.0
    const val FOOT_WIDTH = 7.0

    /** How much wider the surface-coloured outline under a near limb is. */
    const val GAP_EXTRA = 3.0

    /** Front view: half the distance between the shoulder joints and between the hip joints (drawing values). */
    const val SHOULDER_HALF_WIDTH = 18.0
    const val HIP_HALF_WIDTH = 9.0

    /** Front view: a foot is seen end-on, so it is drawn this fraction of its length. */
    const val FOOT_FRONT_FRACTION = 0.4

    /** Where the chest touch point is: this far below the shoulder joint along the torso, on its front surface. */
    const val CHEST_BELOW_SHOULDER = 17.0
    const val CHEST_DEPTH = 12.5
}

enum class Joint { NECK, SHOULDER, ELBOW, WRIST, HIP, KNEE, ANKLE }

/** A side of the body. In the side view NEAR faces the viewer; in the front view NEAR is the one on the right. */
enum class Side { NEAR, FAR }

/**
 * Joint angles of one arm, in degrees.
 * - [shoulder]: flexion (+) / extension (-) from hanging along the torso; in the front view, abduction from the side.
 * - [elbow]: flexion from straight. [wrist]: flexion (+) / extension (-) from in line with the forearm.
 */
data class Arm(val shoulder: Double, val elbow: Double, val wrist: Double = 0.0)

/**
 * Joint angles of one leg, in degrees.
 * - [hip]: flexion (+) / extension (-) from in line with the torso; in the front view, abduction (+) / adduction (-).
 * - [knee]: flexion from straight. [ankle]: dorsiflexion (+) / plantar flexion (-) from the foot at right angles to the shin.
 */
data class Leg(val hip: Double, val knee: Double, val ankle: Double = 0.0)

/**
 * A body position: the pelvis (hip joint centre) in world cm, the torso's angle (pelvis to shoulders: 90 = upright,
 * 180 = lying face up with the head to the left), the neck's flexion, and each limb's joint angles. The far limbs
 * copy the near ones unless given.
 */
data class Pose(
    val pelvisX: Double,
    val pelvisY: Double,
    val torso: Double = 90.0,
    val neck: Double = 0.0,
    val nearArm: Arm,
    val nearLeg: Leg,
    val farArm: Arm = nearArm,
    val farLeg: Leg = nearLeg,
)

/** One side's joint positions. [hand] is the end of the hand; [grip] is where a held handle sits. */
data class LimbPoints(
    val shoulder: Point,
    val elbow: Point,
    val wrist: Point,
    val hand: Point,
    val grip: Point,
    val hip: Point,
    val knee: Point,
    val ankle: Point,
    val heel: Point,
    val toe: Point,
)

/** Joint positions computed from a [Pose]. [shoulder] and [neckBase] are on the torso's axis. */
data class Skeleton(
    val view: View,
    val torso: Double,
    val pelvis: Point,
    val shoulder: Point,
    val neckBase: Point,
    val head: Point,
    val near: LimbPoints,
    val far: LimbPoints,
) {
    fun side(side: Side): LimbPoints = if (side == Side.NEAR) near else far

    /** A point in the torso's frame: [along] from the pelvis towards the shoulders, [front] towards the chest. */
    fun onTorso(along: Double, front: Double): Point = pelvis + dir(torso) * along + dir(torso - 90.0) * front
}

/** Forward kinematics: every joint from the pelvis outwards, with the fixed segment lengths. */
fun Pose.skeleton(view: View = View.SIDE): Skeleton {
    val pelvis = Point(pelvisX, pelvisY)
    val shoulder = pelvis + dir(torso) * Segment.TORSO.length
    val headDir = torso - neck
    val head = shoulder + dir(headDir) * (Segment.NECK.length + Segment.HEAD.length / 2)
    fun limb(side: Side): LimbPoints {
        val arm = if (side == Side.NEAR) nearArm else farArm
        val leg = if (side == Side.NEAR) nearLeg else farLeg
        // The front view puts the near shoulder and hip to the right of the axis and mirrors the far side.
        val sign = if (side == Side.NEAR) 1.0 else -1.0
        val front = view == View.FRONT
        val across = dir(torso - 90.0)
        val s = if (front) shoulder + across * (sign * Body.SHOULDER_HALF_WIDTH) else shoulder
        val h = if (front) pelvis + across * (sign * Body.HIP_HALF_WIDTH) else pelvis
        fun mirrored(a: Double) = if (front && side == Side.FAR) 2 * torso - a else a
        val upperArm = torso + 180.0 + arm.shoulder
        val forearm = upperArm + arm.elbow
        val handAngle = forearm + arm.wrist
        val elbow = s + dir(mirrored(upperArm)) * Segment.UPPER_ARM.length
        val wrist = elbow + dir(mirrored(forearm)) * Segment.FOREARM.length
        val hand = wrist + dir(mirrored(handAngle)) * Segment.HAND.length
        val grip = wrist + dir(mirrored(handAngle)) * (Segment.HAND.length * Body.GRIP)
        val thigh = torso + 180.0 + leg.hip
        val shin = thigh - leg.knee
        val footAngle = shin + 90.0 + leg.ankle
        val knee = h + dir(mirrored(thigh)) * Segment.THIGH.length
        val ankle = knee + dir(mirrored(shin)) * Segment.SHIN.length
        val footDir = dir(mirrored(footAngle))
        val sole = dir(mirrored(footAngle - 90.0)) * (Body.ANKLE_HEIGHT - Body.FOOT_WIDTH / 2)
        val footLength = Segment.FOOT.length * (if (front) Body.FOOT_FRONT_FRACTION else 1.0)
        // Heel and toe are the ends of the foot capsule's centre line, so the drawn foot (round ends included) is
        // footLength long, a fifth of it behind the ankle.
        val r = Body.FOOT_WIDTH / 2
        val heel = ankle + sole - footDir * maxOf(footLength * 0.2 - r, 0.0)
        val toe = ankle + sole + footDir * (footLength * 0.8 - r)
        return LimbPoints(s, elbow, wrist, hand, grip, h, knee, ankle, heel, toe)
    }
    return Skeleton(view, torso, pelvis, shoulder, shoulder, head, limb(Side.NEAR), limb(Side.FAR))
}

/** A joint's allowed range in degrees, inclusive. */
data class JointRange(val min: Double, val max: Double) {
    operator fun contains(angle: Double) = angle >= min - EPSILON && angle <= max + EPSILON

    private companion object {
        const val EPSILON = 1e-6
    }
}

/**
 * Normal adult ranges of motion. Where sources differ, the most conservative (smallest) range is used.
 * - Shoulder flexion 180°, ankle dorsiflexion 20° and plantar flexion 50°, wrist flexion 80° and extension 70°:
 *   the standards table in https://pmc.ncbi.nlm.nih.gov/articles/PMC3512326/ (EDS hypermobility review).
 * - Shoulder extension 60°, hip flexion 120°, hip extension 20°, knee flexion 135°, hip abduction 40° and
 *   adduction 20°: the expected values in https://pmc.ncbi.nlm.nih.gov/articles/PMC13392859/ (standardized manual
 *   goniometry protocol).
 * - Elbow 0–150° and knee 0–135°: https://pmc.ncbi.nlm.nih.gov/articles/PMC4613894/ (wearable ROM sensor validation).
 * - Wrist flexion 60–80° at https://exrx.net/Testing/JointROM: the conservative 60° is used.
 * - Cervical flexion about 80° (https://pmc.ncbi.nlm.nih.gov/articles/PMC1250253/) and extension 50°
 *   (https://pmc.ncbi.nlm.nih.gov/articles/PMC8075604/, the lower of the two reviews' values).
 * - Shoulder abduction 170–180° (https://pmc.ncbi.nlm.nih.gov/articles/PMC9730981/): the conservative 170° is used.
 * The ankle is not checked in the front view, where the foot is seen end-on.
 */
object JointLimits {
    fun range(joint: Joint, view: View): JointRange? = when (joint) {
        Joint.NECK -> JointRange(-50.0, 80.0)
        Joint.SHOULDER -> if (view == View.SIDE) JointRange(-60.0, 180.0) else JointRange(0.0, 170.0)
        Joint.ELBOW -> JointRange(0.0, 150.0)
        Joint.WRIST -> JointRange(-70.0, 60.0)
        Joint.HIP -> if (view == View.SIDE) JointRange(-20.0, 120.0) else JointRange(-20.0, 40.0)
        Joint.KNEE -> JointRange(0.0, 135.0)
        Joint.ANKLE -> if (view == View.SIDE) JointRange(-50.0, 20.0) else null
    }

    /** One line per joint outside its range, e.g. "near elbow -20.0° outside 0.0..150.0". */
    fun problems(pose: Pose, view: View): List<String> = buildList {
        fun check(label: String, joint: Joint, angle: Double) {
            val range = range(joint, view) ?: return
            if (angle !in range) add("$label ${round1(angle)}° outside ${range.min}..${range.max}")
        }
        check("neck", Joint.NECK, pose.neck)
        for ((side, arm, leg) in listOf(Triple("near", pose.nearArm, pose.nearLeg), Triple("far", pose.farArm, pose.farLeg))) {
            check("$side shoulder", Joint.SHOULDER, arm.shoulder)
            check("$side elbow", Joint.ELBOW, arm.elbow)
            check("$side wrist", Joint.WRIST, arm.wrist)
            check("$side hip", Joint.HIP, leg.hip)
            check("$side knee", Joint.KNEE, leg.knee)
            check("$side ankle", Joint.ANKLE, leg.ankle)
        }
    }
}

internal fun round1(v: Double): Double = kotlin.math.round(v * 10) / 10

/**
 * Two-joint reach: the joint angles that put a grip or an ankle on a target, so a figure entry can say where the
 * hands or feet go and still be pure angles. Side view (or the near side of a front view).
 */
object Reach {
    /** The arm whose grip lands on [target], with the elbow bent the natural way (flexion ≥ 0) and [wrist] fixed. */
    fun arm(shoulder: Point, target: Point, torso: Double, wrist: Double = 0.0): Arm {
        val g = Segment.HAND.length * Body.GRIP
        val w = wrist * PI / 180
        // The elbow-to-grip link, and its angle off the forearm.
        val link = hypot(Segment.FOREARM.length + g * cos(w), g * sin(w))
        val offset = atan2(g * sin(w), Segment.FOREARM.length + g * cos(w)) * 180 / PI
        val (upper, lower) = twoLink(shoulder, target, Segment.UPPER_ARM.length, link, bendPositive = true)
        val forearm = lower - offset
        return Arm(shoulder = fold(upper - torso - 180.0), elbow = fold(forearm - upper), wrist = wrist)
    }

    /** The leg whose ankle lands on [ankle], knee forward (flexion ≥ 0), with the foot at the screen angle [foot]. */
    fun leg(hip: Point, ankle: Point, torso: Double, foot: Double): Leg {
        val (thigh, shin) = twoLink(hip, ankle, Segment.THIGH.length, Segment.SHIN.length, bendPositive = false)
        return Leg(hip = fold(thigh - torso - 180.0), knee = fold(thigh - shin), ankle = fold(foot - shin - 90.0))
    }

    /** Absolute angles of two links from [base] to [target]; the second bends counter-clockwise when [bendPositive]. */
    private fun twoLink(base: Point, target: Point, a: Double, b: Double, bendPositive: Boolean): Pair<Double, Double> {
        val d = target - base
        val dist = d.length()
        require(dist <= a + b + 1e-9 && dist >= abs(a - b) - 1e-9) { "target out of reach: ${round1(dist)} cm" }
        val toTarget = angleOf(d)
        val atBase = acos(((a * a + dist * dist - b * b) / (2 * a * dist)).coerceIn(-1.0, 1.0)) * 180 / PI
        val first = if (bendPositive) toTarget - atBase else toTarget + atBase
        val joint = base + dir(first) * a
        return first to angleOf(target - joint)
    }
}
