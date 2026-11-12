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
 * Limbs (allowed sources, each value labelled with its segment there):
 * - Upper arm 0.186, forearm 0.146 and hand 0.108 of height: Winter's table, as quoted in
 *   https://pmc.ncbi.nlm.nih.gov/articles/PMC12637938/ (joint angle trajectories in human reaching).
 * - Thigh 0.245 and shank 0.246 of height: https://pmc.ncbi.nlm.nih.gov/articles/PMC13417611/ (Noraxon IMU vs Vicon
 *   gait validation) labels them "thigh length 0.245*height" and "shank length, lateral femoral epicondyle to lateral
 *   malleolus, 0.246*height" (Dempster-derived ratios, the same values as Winter's table). Thigh 0.245H is also in
 *   https://pmc.ncbi.nlm.nih.gov/articles/PMC3582351/ (segment lengths "as provided by Winter"), foot 0.152H too.
 *   Shank choice: https://pmc.ncbi.nlm.nih.gov/articles/PMC11233572/ measures a shorter leg (0.221H, a depth-camera
 *   estimate of one sample) and an athletics review (PMC13672677) 0.22; the labelled anthropometric table value 0.246
 *   is kept, because it is the one the thigh and foot values come from, so the leg stays one consistent table.
 * Head and torso: Winter's joint-height landmarks (ankle 0.039H, hip 0.530H, shoulder 0.818H, chin 0.870H) are not in
 * an allowed source we could find, so they are a derived drawing value: the torso (hip joint to shoulder joint 0.288),
 * neck (shoulder to chin 0.052) and head (chin to crown 0.130) close the stature exactly over ankle + shank + thigh.
 * Cross-check: the acromion-height regression quoted in https://pmc.ncbi.nlm.nih.gov/articles/PMC4754207/
 * (0.957 × stature − 0.208 × sitting height + 0.065 × waist − 9.64) gives about 144 cm for this body with a typical
 * sitting height and waist, within 1.5 cm of the 143 cm the split puts the shoulder at.
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

    /** Ankle joint above the sole: the 0.039H left below the shank by the split above (a derived drawing value). */
    const val ANKLE_HEIGHT = 0.039 * HEIGHT

    /** Where the grip sits along the hand, as a fraction from the wrist (the bar lies across the palm). */
    const val GRIP = 0.45

    // Drawing widths (cm): a solid figure in the approved draft's proportions, a neutral drawing estimate (no allowed
    // source gives limb widths for a profile drawing). Checks use half of each as the capsule radius. Cross-check for
    // the torso: a man's external chest depth averages 24.5 cm (https://pmc.ncbi.nlm.nih.gov/articles/PMC4840890/),
    // which the drawn torso (10 cm behind the axis, up to [CHEST_DEPTH] in front) matches.
    const val TORSO_RADIUS = 10.0
    /**
     * The drawn head is a circle 20 cm across: an adult man's head length (glabella to the back of the head) is about
     * 19–20 cm (https://pmc.ncbi.nlm.nih.gov/articles/PMC6820124/, 191–202 mm means). [Segment.HEAD] is the 22.75 cm
     * chin-to-crown height; a circle cannot be both, so the circle keeps the head's depth and its top sits at the
     * crown (the stature stays exact). The neck capsule fills the 2.75 cm under it, where the jaw is.
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

    /** Front view: half the distance between the shoulder joints and between the hip joints (neutral drawing values). */
    const val SHOULDER_HALF_WIDTH = 18.0
    const val HIP_HALF_WIDTH = 9.0

    /** Front view: a foot is seen end-on, so it is drawn this fraction of its length. */
    const val FOOT_FRONT_FRACTION = 0.4

    /**
     * The chest touch point: [CHEST_BELOW_SHOULDER] below the shoulder joint along the torso, [CHEST_DEPTH] in front of
     * its axis. Depth: half the 24.5 cm mean male chest depth (https://pmc.ncbi.nlm.nih.gov/articles/PMC4840890/),
     * rounded up to the pectoral surface. Height: ExRx's bench press analyses
     * (https://exrx.net/Kinesiology/BenchPress) put the touch on the lower to mid chest, and ExRx's bench press
     * power-lift page (https://exrx.net/WeightExercises/PectoralSternal/BBBenchPressPowerLift) requires it above the
     * bottom of the sternum; 17 cm below the
     * shoulder joint is mid-chest on this torso (a neutral estimate inside that range, not a measured value).
     */
    const val CHEST_BELOW_SHOULDER = 17.0
    const val CHEST_DEPTH = 12.5

    /**
     * The upper chest (the lat pulldown's bar target): 12 cm below the shoulder joint, at the top of the drawn chest's
     * flat front, above the bench press touch point. A neutral drawing estimate.
     */
    const val UPPER_CHEST_BELOW_SHOULDER = 12.0
    const val UPPER_CHEST_DEPTH = 12.5
}

enum class Joint { NECK, SHOULDER, ELBOW, WRIST, HIP, KNEE, ANKLE }

/** A side of the body. In the side view NEAR faces the viewer; in the front view NEAR is the one on the right. */
enum class Side { NEAR, FAR }

/**
 * Joint angles of one arm, in degrees.
 * - [shoulder]: flexion (+) / extension (-) from hanging along the torso; in the front view, abduction from the side.
 * - [elbow]: flexion from straight. [wrist]: flexion (+) / extension (-) from in line with the forearm.
 * - [turnedOut]: the upper arm is turned out to the side (abducted and rotated outwards, as with the hands wide on a bar
 *   across the upper back), so seen from the side the elbow folds the other way: the forearm swings back, not forward.
 *   The joint ranges are the same; only the side-view drawing of the bend is mirrored.
 * - [out]: side view, how far the upper arm points out of the picture plane, towards the camera (0 = in the
 *   plane; negative: away from it, back towards the body's middle). Seen from the side, an upper arm out to the side looks shorter: its drawn length is the true length times
 *   cos([out]), a projection, not a change of proportions. [shoulder] and [elbow] are then the drawn (projected)
 *   angles, and the elbow's limit is checked on its true 3D bend (see [JointLimits.trueElbow]). The forearm is taken
 *   to lie in the picture plane, as it does when it is vertical (a dumbbell press) or along the body.
 * - [forearmOut]: side view only, the same for the forearm and hand: drawn at their true length times
 *   cos([forearmOut]). A straight arm angled out to the side (a wide-grip bench lockout) has [out] and [forearmOut]
 *   equal. The elbow's true bend counts both (see [JointLimits.trueElbow]).
 * - Front view: [out] and [forearmOut] are the upper arm's and forearm's angles out of the picture plane, towards the
 *   camera (+) or away from it (-), drawn the same way, at their true length times the cosine (a cable fly's hands
 *   meeting in front of the chest, a push-up's upper arm along the body). With both 0 the front view is unchanged.
 */
data class Arm(
    val shoulder: Double,
    val elbow: Double,
    val wrist: Double = 0.0,
    val turnedOut: Boolean = false,
    val out: Double = 0.0,
    val forearmOut: Double = 0.0,
)

/**
 * Joint angles of one leg, in degrees.
 * - [hip]: flexion (+) / extension (-) from in line with the torso; in the front view, abduction (+) / adduction (-).
 * - [knee]: flexion from straight. [ankle]: dorsiflexion (+) / plantar flexion (-) from the foot at right angles to the shin.
 * - [out]: how far the thigh points out of the picture plane: in the side view out to the side, in the front view
 *   forward, towards the camera (a sumo set-up, the knees pushed out over wide feet). The thigh then looks shorter:
 *   it is drawn at its true length times cos([out]), a projection, not a change of proportions. The shin is taken to
 *   stay in the plane (near vertical in a wide stance). The joint limits are checked on the true 3D thigh (see
 *   [JointLimits.trueAbduction]). In the front view a negative [out] points the thigh away from the camera (a push-up's
 *   legs, seen from the head end).
 * - [shinOut]: front view only, the shin's angle out of the picture plane, drawn at its true length times
 *   cos([shinOut]); 0 keeps it in the plane. A straight leg pointing away from the camera has [shinOut] = [out].
 */
data class Leg(val hip: Double, val knee: Double, val ankle: Double = 0.0, val out: Double = 0.0, val shinOut: Double = 0.0)

/**
 * A body position: the pelvis (hip joint centre) in world cm, the torso's angle (pelvis to shoulders: 90 = upright,
 * 180 = lying face up with the head to the left), the neck's flexion, and each limb's joint angles. The far limbs
 * copy the near ones unless given. [lean] is for the front view only: how far the torso leans forward, towards the
 * camera, from upright; seen from the front it then looks shorter, drawn at its true length times cos([lean]) (a
 * projection, like [Arm.out]). The head stays at its full height above the shoulders (the lifter looks forward),
 * unless [neckOut] is given: front view only, the neck and head pointing out of the plane, towards the camera, so the
 * head is drawn that much closer to the shoulders (times cos([neckOut])), as when the head stays in line with a
 * torso that leans towards the camera (a push-up seen from the front).
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
    val lean: Double = 0.0,
    val neckOut: Double = 0.0,
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
    /** How much of the torso's length shows: cos of the front view's [Pose.lean], 1 otherwise. */
    val alongScale: Double = 1.0,
) {
    fun side(side: Side): LimbPoints = if (side == Side.NEAR) near else far

    /** A point in the torso's frame: [along] from the pelvis towards the shoulders, [front] towards the chest. */
    fun onTorso(along: Double, front: Double): Point = pelvis + dir(torso) * (along * alongScale) + dir(torso - 90.0) * front
}

/** Forward kinematics: every joint from the pelvis outwards, with the fixed segment lengths. */
fun Pose.skeleton(view: View = View.SIDE): Skeleton {
    val pelvis = Point(pelvisX, pelvisY)
    val alongScale = if (view == View.FRONT) kotlin.math.cos(lean * kotlin.math.PI / 180) else 1.0
    val shoulder = pelvis + dir(torso) * (Segment.TORSO.length * alongScale)
    val headDir = torso - neck
    // The head circle's top is at the crown (see Body.HEAD_RADIUS).
    val headScale = if (view == View.FRONT) kotlin.math.cos(neckOut * kotlin.math.PI / 180) else 1.0
    val head = shoulder + dir(headDir) * ((Segment.NECK.length + Segment.HEAD.length - Body.HEAD_RADIUS) * headScale)
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
        val bend = if (arm.turnedOut && !front) -1.0 else 1.0
        val forearm = upperArm + bend * arm.elbow
        val handAngle = forearm + bend * arm.wrist
        val upperDrawn = Segment.UPPER_ARM.length * kotlin.math.cos(arm.out * kotlin.math.PI / 180)
        val lowerScale = kotlin.math.cos(arm.forearmOut * kotlin.math.PI / 180)
        val elbow = s + dir(mirrored(upperArm)) * upperDrawn
        val wrist = elbow + dir(mirrored(forearm)) * (Segment.FOREARM.length * lowerScale)
        val hand = wrist + dir(mirrored(handAngle)) * (Segment.HAND.length * lowerScale)
        val grip = wrist + dir(mirrored(handAngle)) * (Segment.HAND.length * Body.GRIP * lowerScale)
        val thigh = torso + 180.0 + leg.hip
        val shin = thigh - leg.knee
        val footAngle = shin + 90.0 + leg.ankle
        val thighDrawn = Segment.THIGH.length * kotlin.math.cos(leg.out * kotlin.math.PI / 180)
        val knee = h + dir(mirrored(thigh)) * thighDrawn
        val shinDrawn = Segment.SHIN.length * (if (front) kotlin.math.cos(leg.shinOut * kotlin.math.PI / 180) else 1.0)
        val ankle = knee + dir(mirrored(shin)) * shinDrawn
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
    return Skeleton(view, torso, pelvis, shoulder, shoulder, head, limb(Side.NEAR), limb(Side.FAR), alongScale)
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
 * - Shoulder flexion 180°, wrist flexion 80° and extension 70°, ankle plantar flexion 50°: the standards table in
 *   https://pmc.ncbi.nlm.nih.gov/articles/PMC3512326/ (EDS hypermobility review).
 * - Ankle dorsiflexion 35°. The figure's ankle carries body weight with the knee bent in every pose that bends it far
 *   (squat, deadlift, leg press), so it uses weight-bearing values, not the 20° measured lying down: young adults
 *   reach 38–42.5° (95% CI) in the weight-bearing lunge test (https://pmc.ncbi.nlm.nih.gov/articles/PMC6249959/),
 *   and under 35° counts as a mobility limitation (https://pmc.ncbi.nlm.nih.gov/articles/PMC7932177/). The
 *   conservative 35° is used.
 * - Shoulder extension 60°, hip extension 20°, knee flexion 135°, hip abduction 40° and adduction 20°: the expected
 *   values in https://pmc.ncbi.nlm.nih.gov/articles/PMC13392859/ (standardized manual goniometry protocol).
 * - Hip flexion, side view, 140°. The figure's hip angle is the trunk-to-thigh angle (hip flexion measured as
 *   180° minus the trunk–thigh angle, as in https://pmc.ncbi.nlm.nih.gov/articles/PMC9403702/), and its torso is
 *   one rigid piece, so it also carries the pelvis's tilt, which goniometry's pelvis-fixed 120° (PMC13392859) leaves
 *   out. A high-bar back squat holds the trunk about 46° from vertical at depth
 *   (https://pmc.ncbi.nlm.nih.gov/articles/PMC10143703/), which with the thighs just below level is a trunk–thigh
 *   flexion of about 135–139°; 140° allows that and no more.
 * - Elbow 0–150° and knee 0–135°: https://pmc.ncbi.nlm.nih.gov/articles/PMC4613894/ (wearable ROM sensor validation).
 * - Wrist flexion 60–80° at https://exrx.net/Testing/JointROM: the conservative 60° is used.
 * - Cervical flexion about 80° (https://pmc.ncbi.nlm.nih.gov/articles/PMC1250253/) and extension 50°
 *   (https://pmc.ncbi.nlm.nih.gov/articles/PMC8075604/, the lower of the two reviews' values).
 * - Shoulder abduction 170–180° (https://pmc.ncbi.nlm.nih.gov/articles/PMC9730981/): the conservative 170° is used.
 *   An arm's [Arm.out] (the upper arm out of the picture plane) is kept to ±90°, a direction, inside that range.
 *   [Arm.forearmOut] likewise.
 * - A thigh out of the picture plane ([Leg.out]) is checked on its true 3D angles: its abduction (the angle out of
 *   the body's middle plane) inside the 40° hip abduction range above (PMC13392859), and, in the front view, its
 *   flexion (180° minus the 3D trunk–thigh angle, with the torso's [Pose.lean]) inside the 140° hip flexion range.
 * - An arm out of the picture plane ([Arm.out] or [Arm.forearmOut] not 0) is checked on its true 3D angles instead of
 *   the drawn ones: the elbow's bend by its size only, and the shoulder only for pointing more than 60° behind the
 *   body. So for such arms an elbow bent the wrong way, and a shoulder raised past its flexion or abduction limit,
 *   are no longer caught; a figure using them must bound them in its own test (Task 6's do).
 * The ankle is not checked in the front view, where the foot is seen end-on.
 */
object JointLimits {
    fun range(joint: Joint, view: View): JointRange? = when (joint) {
        Joint.NECK -> JointRange(-50.0, 80.0)
        Joint.SHOULDER -> if (view == View.SIDE) JointRange(-60.0, 180.0) else JointRange(0.0, 170.0)
        Joint.ELBOW -> JointRange(0.0, 150.0)
        Joint.WRIST -> JointRange(-70.0, 60.0)
        Joint.HIP -> if (view == View.SIDE) JointRange(-20.0, 140.0) else JointRange(-20.0, 40.0)
        Joint.KNEE -> JointRange(0.0, 135.0)
        Joint.ANKLE -> if (view == View.SIDE) JointRange(-50.0, 35.0) else null
    }

    /**
     * The elbow's true bend for a side-view arm whose upper arm points [Arm.out]° out of the picture plane: the angle
     * between the 3D upper arm (its drawn direction scaled by cos(out), plus the out-of-plane part) and the forearm in
     * the plane, or out of it by [Arm.forearmOut] on the same side. With both 0 it is the drawn [Arm.elbow].
     */
    fun trueElbow(arm: Arm): Double {
        if (arm.out == 0.0 && arm.forearmOut == 0.0) return arm.elbow
        val r = kotlin.math.PI / 180
        val c = (
            kotlin.math.cos(arm.out * r) * kotlin.math.cos(arm.forearmOut * r) * kotlin.math.cos(arm.elbow * r) +
                kotlin.math.sin(arm.out * r) * kotlin.math.sin(arm.forearmOut * r)
            ).coerceIn(-1.0, 1.0)
        return kotlin.math.acos(c) / r * kotlin.math.sign(arm.elbow).let { if (it == 0.0) 1.0 else it }
    }

    /**
     * How far an upper arm out of the picture plane points behind the body, in degrees: the angle between its true 3D
     * direction and the body's frontal plane, on the back side (0 when it points forward or to the side). Side view:
     * "forward" is square to the torso towards the chest, in the picture; front view, it is towards the camera
     * ([Arm.out]), tipped down with a torso leaning [lean]° towards the camera (a push-up seen head-on: the chest faces
     * the floor). Checked against the 60° shoulder extension limit (PMC13392859, above).
     */
    fun trueShoulderBack(arm: Arm, view: View, lean: Double = 0.0): Double {
        val r = kotlin.math.PI / 180
        val forward = if (view == View.SIDE) {
            // The drawn upper arm is (torso + 180 + shoulder); the chest faces (torso - 90). Their dot product, scaled.
            kotlin.math.cos(arm.out * r) * kotlin.math.cos((180.0 + arm.shoulder + 90.0) * r)
        } else {
            // In the torso's frame: the chest faces (down the torso) × sin(lean) + (towards the camera) × cos(lean).
            kotlin.math.cos(arm.out * r) * kotlin.math.cos(arm.shoulder * r) * kotlin.math.sin(lean * r) +
                kotlin.math.sin(arm.out * r) * kotlin.math.cos(lean * r)
        }
        return if (forward >= 0.0) 0.0 else kotlin.math.asin((-forward).coerceAtMost(1.0)) / r
    }

    /**
     * The elbow's bend as checked: the drawn [Arm.elbow] for an arm in the picture plane; for an arm out of it, the
     * size of its true 3D bend ([trueElbow]), because which way the drawn bend turns is then a projection, not flexion
     * or extension (a fly's slight bend, the elbows pointing out to the sides).
     */
    fun elbowBend(arm: Arm): Double = if (arm.out == 0.0 && arm.forearmOut == 0.0) arm.elbow else abs(trueElbow(arm))

    /**
     * The true hip abduction of a thigh [Leg.out]° out of the picture plane: the angle between the 3D thigh and the
     * body's middle (sagittal) plane, in degrees. Side view: [Leg.out] itself. Front view: the drawn sideways part,
     * sin([Leg.hip]) × cos([Leg.out]), as an angle.
     */
    fun trueAbduction(leg: Leg, view: View): Double =
        if (view == View.SIDE) leg.out
        else kotlin.math.asin(kotlin.math.sin(leg.hip * kotlin.math.PI / 180) * kotlin.math.cos(leg.out * kotlin.math.PI / 180)) * 180 / kotlin.math.PI

    /**
     * The true hip flexion of a front-view leg whose thigh points [Leg.out]° towards the camera, with the torso leaning
     * [lean]° towards it: 180° minus the 3D angle between the torso (hip to shoulders) and the thigh (hip to knee).
     */
    fun trueFrontHipFlexion(leg: Leg, lean: Double): Double {
        val r = kotlin.math.PI / 180
        // Unit vectors in (sideways, up, towards the camera): the thigh drawn [Leg.hip] out from straight down.
        val up = kotlin.math.cos(leg.out * r) * -kotlin.math.cos(leg.hip * r) * kotlin.math.cos(lean * r) +
            kotlin.math.sin(leg.out * r) * kotlin.math.sin(lean * r)
        return 180.0 - kotlin.math.acos(up.coerceIn(-1.0, 1.0)) / r
    }

    /** One line per joint outside its range, e.g. "near elbow -20.0° outside 0.0..150.0". */
    fun problems(pose: Pose, view: View): List<String> = buildList {
        fun check(label: String, joint: Joint, angle: Double) {
            val range = range(joint, view) ?: return
            if (angle !in range) add("$label ${round1(angle)}° outside ${range.min}..${range.max}")
        }
        check("neck", Joint.NECK, pose.neck)
        for ((side, arm, leg) in listOf(Triple("near", pose.nearArm, pose.nearLeg), Triple("far", pose.farArm, pose.farLeg))) {
            if (arm.out == 0.0) {
                check("$side shoulder", Joint.SHOULDER, arm.shoulder)
            } else {
                // An upper arm out of the plane: its drawn angle is a projection (a fly's arm out to the side, a press's
                // elbow back and out), so its true 3D direction is checked: how far it points behind the body.
                val back = trueShoulderBack(arm, view, pose.lean)
                if (back > 60.0 + 1e-6) add("$side upper arm ${round1(back)}° behind the body, more than 60.0")
            }
            check("$side elbow", Joint.ELBOW, elbowBend(arm))
            val outRange = -90.0..90.0
            if (arm.out !in outRange) add("$side upper arm ${round1(arm.out)}° out of the plane, outside $outRange")
            if (arm.forearmOut !in outRange) add("$side forearm ${round1(arm.forearmOut)}° out of the plane, outside $outRange")
            check("$side wrist", Joint.WRIST, arm.wrist)
            if (view == View.FRONT && leg.shinOut !in -90.0..90.0) add("$side shin ${round1(leg.shinOut)}° out of the plane, outside -90..90")
            if (leg.out != 0.0) {
                // A thigh out of the plane: its true abduction and flexion, not the drawn angles.
                val thighRange = if (view == View.SIDE) 0.0..90.0 else -90.0..90.0
                if (leg.out !in thighRange) add("$side thigh ${round1(leg.out)}° out of the plane, outside $thighRange")
                val abduction = trueAbduction(leg, view)
                if (abduction !in JointRange(-20.0, 40.0)) add("$side hip abduction ${round1(abduction)}° outside -20.0..40.0")
                if (view == View.FRONT) {
                    val flexion = trueFrontHipFlexion(leg, pose.lean)
                    if (flexion !in JointRange(-20.0, 140.0)) add("$side hip flexion ${round1(flexion)}° outside -20.0..140.0")
                }
            } else {
                check("$side hip", Joint.HIP, leg.hip)
            }
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
    /**
     * The arm whose grip lands on [target], with the elbow bent the natural way (flexion ≥ 0) and [wrist] fixed; with
     * [turnedOut], the side-view bend mirrored (see [Arm.turnedOut]).
     */
    fun arm(shoulder: Point, target: Point, torso: Double, wrist: Double = 0.0, turnedOut: Boolean = false): Arm {
        val g = Segment.HAND.length * Body.GRIP
        val w = wrist * PI / 180
        val bend = if (turnedOut) -1.0 else 1.0
        // The elbow-to-grip link, and its angle off the forearm.
        val link = hypot(Segment.FOREARM.length + g * cos(w), g * sin(w))
        val offset = atan2(g * sin(w), Segment.FOREARM.length + g * cos(w)) * 180 / PI
        val (upper, lower) = twoLink(shoulder, target, Segment.UPPER_ARM.length, link, bendPositive = !turnedOut)
        val forearm = lower - bend * offset
        return Arm(shoulder = fold(upper - torso - 180.0), elbow = bend * fold(forearm - upper), wrist = wrist, turnedOut = turnedOut)
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
