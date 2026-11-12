package com.powertracker.domain.library.figures.areas

import com.powertracker.domain.library.areas.RACK_SAFETY
import com.powertracker.domain.library.figures.AdjustableBench
import com.powertracker.domain.library.figures.Arm
import com.powertracker.domain.library.figures.Attachment
import com.powertracker.domain.library.figures.BarOnRails
import com.powertracker.domain.library.figures.BarTouches
import com.powertracker.domain.library.figures.Barbell
import com.powertracker.domain.library.figures.Body
import com.powertracker.domain.library.figures.BodyPoint
import com.powertracker.domain.library.figures.CableAttachment
import com.powertracker.domain.library.figures.CableHandle
import com.powertracker.domain.library.figures.CableHandles
import com.powertracker.domain.library.figures.CableStack
import com.powertracker.domain.library.figures.Contact
import com.powertracker.domain.library.figures.Dumbbell
import com.powertracker.domain.library.figures.ElbowBend
import com.powertracker.domain.library.figures.ExerciseFigure
import com.powertracker.domain.library.figures.FlatBench
import com.powertracker.domain.library.figures.Floor
import com.powertracker.domain.library.figures.HandsLevelWith
import com.powertracker.domain.library.figures.Kettlebell
import com.powertracker.domain.library.figures.KettlebellHold
import com.powertracker.domain.library.figures.KeyCheck
import com.powertracker.domain.library.figures.LateralRaiseMachine
import com.powertracker.domain.library.figures.Leg
import com.powertracker.domain.library.figures.LockoutOverShoulders
import com.powertracker.domain.library.figures.PecDeck
import com.powertracker.domain.library.figures.Point
import com.powertracker.domain.library.figures.Pose
import com.powertracker.domain.library.figures.PressMachine
import com.powertracker.domain.library.figures.Prop
import com.powertracker.domain.library.figures.Reach
import com.powertracker.domain.library.figures.Scene
import com.powertracker.domain.library.figures.Seat
import com.powertracker.domain.library.figures.Segment
import com.powertracker.domain.library.figures.Side
import com.powertracker.domain.library.figures.SmithMachine
import com.powertracker.domain.library.figures.Surface
import com.powertracker.domain.library.figures.TBarLever
import com.powertracker.domain.library.figures.adjustableHinge
import com.powertracker.domain.library.figures.View
import com.powertracker.domain.library.figures.Which
import com.powertracker.domain.library.figures.point
import com.powertracker.domain.library.figures.skeleton
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

// Figures for the library's Shoulders area (library/areas/Shoulders.kt), keyed by catalogue name.

/**
 * DB shoulder press. Seated on an upright bench, head, back and buttocks against it, feet flat. START the dumbbells at
 * shoulder height; END pressed overhead with straight elbows, over the shoulders.
 * - View: side, not front. The press is a frontal-plane movement, but the figure cannot draw a seated body from the
 *   front (its thighs would point at the camera, and segment lengths are fixed), so the side view shows the seat, the
 *   back support and the lockout over the shoulders, which the guide's lines are about.
 * - START: "beside your shoulders, with your elbows under your wrists": the forearm vertical, the dumbbell straight
 *   above the elbow at about ear height, the elbow just below the shoulder. Seen from the side an upper arm out to the
 *   side is foreshortened, so it is drawn 50° out of the picture plane (Arm.out; its true elbow bend is about 120°).
 *   The dumbbell is drawn 13 cm in front of the shoulder joint, not over it, so the forearm stands clear in front of
 *   the face instead of across it (a neutral placement; the guide's "beside" cannot be seen from the side).
 * - Back upright (90°): ExRx's dumbbell shoulder press (https://exrx.net/WeightExercises/DeltoidAnterior/DBShoulderPress)
 *   and ACE's seated overhead press (https://www.acefitness.org/resources/everyone/exercise-library/45/seated-overhead-press/)
 *   sit upright against the back support; feet and knees placed neutrally (knees about 90°).
 */
private val dbShoulderPress: ExerciseFigure by lazy {
    val bench = AdjustableBench(x = 30.0)
    val hinge = Point(bench.x - AdjustableBench.SEAT_LENGTH, -FlatBench.HEIGHT)
    val pelvis = Point(hinge.x + Body.TORSO_RADIUS, hinge.y - Body.TORSO_RADIUS)
    val legs = Reach.leg(pelvis, Point(pelvis.x + 44.0, -Body.ANKLE_HEIGHT), torso = 90.0, foot = 0.0)
    val shoulder = shoulderOf(pelvis, 90.0)
    // The upper arm out to the side, 50° out of the picture plane, so it looks short from the side; the elbow in front
    // of and below the shoulder, the forearm vertical, the dumbbell straight above the elbow.
    val out = 50.0
    val ahead = 13.0
    val drawn = Segment.UPPER_ARM.length * kotlin.math.cos(out * kotlin.math.PI / 180)
    val elbowAt = shoulder + Point(ahead, kotlin.math.sqrt(drawn * drawn - ahead * ahead))
    val upper = angleTo(elbowAt - shoulder)
    val atShoulder = Arm(shoulder = fold180(upper - 90.0 - 180.0), elbow = fold180(90.0 - upper), out = out)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, 90.0, nearArm = arm, nearLeg = legs)
    val props = listOf(Floor, bench, Dumbbell(Side.FAR), Dumbbell(Side.NEAR))
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(pose(atShoulder), props),
        end = Scene(pose(Arm(180.0, 0.0)), props),
        startCaption = "At shoulder height",
        endCaption = "Elbows straight overhead",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit on a bench with back support. Keep your head, back and buttocks against it, and your feet flat on the floor.",
            "Hold them beside your shoulders, with your elbows under your wrists.",
            "Breathe out and press the dumbbells overhead until your elbows are straight.",
            "Breathe in and lower them under control to shoulder height.",
        ),
        contacts = listOf(
            Contact(BodyPoint.HIPS, Surface.SEAT, BOTH),
            Contact(BodyPoint.UPPER_BACK, Surface.BACK_PAD, BOTH),
            Contact(BodyPoint.HEAD, Surface.BACK_PAD, BOTH),
            Contact(BodyPoint.FEET, Surface.FLOOR, BOTH),
        ),
        checks = listOf(LockoutOverShoulders(Which.END)),
    )
}

// ---- Task 8: the rest of the Shoulders area ----
//
// Camera: the side view for the presses and the front raises, whose key positions (where the weight starts, the
// lockout over the shoulders, the bench and pad contacts, the arm at shoulder height) are in the sagittal plane, and
// for the cable rear delt row, whose high elbows, bar at the upper chest and elbows just behind the back show from the
// side (as the seated rows do). The front view for the lateral raises, the reverse flys and the rear delt raises,
// whose point is the arms moving out to the sides: from the side an arm out to the side points at the camera and
// vanishes. The bent-over and lying rear delt raises are drawn head-on, the torso foreshortened towards the camera;
// the chest-supported one's legs lie straight away behind the shoulders and are hidden (the archer push-up's rule).
// The cable reverse fly is seen from behind (the lifter faces the pulleys, beyond them): its arms' angle out of the
// plane is still measured towards the lifter's chest (the drawing is the same, cos), so the "behind the body" limit
// keeps its meaning. The rotations are drawn from the front: the upper arm stays at the side, and the forearm's swing
// across the belly and out (or in) shows as the hand moving across the picture, the forearm drawn foreshortened where
// it points forward. The side-lying one is seen from in front of the lifter's body, lying along the floor, so its
// forearm swings in the picture's plane, from hanging towards the floor up to the ceiling.
// Every position a guide does not fix is sourced in its comment, or kept neutral and marked so.

private fun figure(
    view: View,
    start: Pose,
    end: Pose,
    props: List<Prop>,
    startCaption: String,
    endCaption: String,
    thumbnail: Which,
    guideLines: List<String>,
    contacts: List<Contact>,
    checks: List<KeyCheck>,
    endProps: List<Prop> = props,
) = ExerciseFigure(view, Scene(start, props), Scene(end, endProps), startCaption, endCaption, thumbnail, guideLines, contacts, checks)

private val HANG = Arm(0.0, 0.0)
private val FEET_DOWN = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH))

/** Shoulder to grip with the elbow bent [bend]° (wrist straight). */
private fun reachAt(bend: Double) = sqrt(
    Segment.UPPER_ARM.length * Segment.UPPER_ARM.length + FORE_LINK * FORE_LINK +
        2 * Segment.UPPER_ARM.length * FORE_LINK * cos(bend / DEG),
)

/** The near shoulder's 3D point in a side view: on the torso's axis, half the shoulders' width towards the camera. */
private fun sideShoulder(p: Point) = v3(p, Body.SHOULDER_HALF_WIDTH)

/** A unit 3D vector. */
private fun u3(x: Double, y: Double, z: Double) = V3(x, y, z).unit()

/** The forearm bent [bend]° off the upper arm [u], turned towards [toward] (made square to [u] first). */
private fun bent(u: V3, toward: V3, bend: Double): V3 {
    val t = (toward - u * toward.dot(u)).unit()
    return (u * cos(bend / DEG) + t * sin(bend / DEG)).unit()
}

/** An [Arm] for the 3D upper arm [u] and forearm [f], the drawn torso at [torso]° (90 in the front view). */
private fun arm(u: V3, f: V3, torso: Double = 90.0) = armFrom(u, f, torso)

/**
 * The slight, fixed elbow bend of the raises and flys: 18°. ExRx's lying rear lateral raise
 * (https://exrx.net/WeightExercises/DeltoidPosterior/DBLyingRearLateralRaise) holds the elbow between 10° and 30°, as
 * does its one-arm lateral raise (https://exrx.net/WeightExercises/DeltoidLateral/DBOneArmLateralRaise); 18° is inside.
 */
private const val RAISE_BEND = 18.0

// ---- seated on an upright bench (as the approved DB shoulder press) ----

private val uprightBench = AdjustableBench(x = 30.0)
private val uprightPelvis = Point(uprightBench.x - AdjustableBench.SEAT_LENGTH + Body.TORSO_RADIUS, -FlatBench.HEIGHT - Body.TORSO_RADIUS)
private val uprightLegs by lazy { Reach.leg(uprightPelvis, Point(uprightPelvis.x + 44.0, -Body.ANKLE_HEIGHT), torso = 90.0, foot = 0.0) }
private val uprightShoulder = shoulderOf(uprightPelvis, 90.0)
private fun uprightPose(arm: Arm, neck: Double = 0.0) = Pose(uprightPelvis.x, uprightPelvis.y, 90.0, neck, arm, uprightLegs)

private val UPRIGHT_CONTACTS = listOf(
    Contact(BodyPoint.HIPS, Surface.SEAT, BOTH),
    Contact(BodyPoint.UPPER_BACK, Surface.BACK_PAD, BOTH),
    Contact(BodyPoint.FEET, Surface.FLOOR, BOTH),
)

/** A barbell's grip, [half] cm out from the body's middle: the hands a little (or well) wider than the shoulders. */
private fun barArm(shoulder: Point, grip: Point, half: Double, hint: V3) = arm3(sideShoulder(shoulder), v3(grip, half), hint, 90.0)

/** Straight arms overhead to a bar gripped [half] cm out, its centre [ahead] cm in front of the shoulder joints. */
private fun barOverhead(shoulder: Point, half: Double, ahead: Double): Arm {
    val dz = half - Body.SHOULDER_HALF_WIDTH
    val up = sqrt(ARM_REACH * ARM_REACH - dz * dz - ahead * ahead)
    return barArm(shoulder, shoulder + Point(ahead, -up), half, V3(0.0, 0.0, 1.0))
}

/** The seated barbell press's grip: a little wider than the shoulders, the hands 30 cm out (neutral). */
private const val SEATED_GRIP = 30.0

/**
 * Seated barbell press. Seated on the upright bench in the rack, back on the pad, feet on the floor; START the bar
 * lowered to the front of the shoulders at about chin height (its centre 8 cm above the shoulder joints, 12.5 cm in
 * front: clear of the face), the elbows forward and down under it; END pressed overhead to straight arms over the
 * shoulders. The guide's safety line asks for pins: the rack's J-hooks just under the bar's start and its safety arms
 * 10 cm under it. ExRx barbell seated military press (https://exrx.net/WeightExercises/DeltoidAnterior/BBSeatedMilitaryPress);
 * the grip width and the chin-height bar's offsets are neutral.
 */
private val seatedBarbellPress: ExerciseFigure by lazy {
    val sh = uprightShoulder
    val start = barArm(sh, sh + Point(12.5, -8.0), SEATED_GRIP, V3(0.4, 1.0, 0.5))
    val end = barOverhead(sh, SEATED_GRIP, 0.0)
    val barStart = uprightPose(start).skeleton().near.grip
    val rack = rackInFront(barStart.x, jHookHeight = -barStart.y - 6.0, safetyHeight = -barStart.y - 10.0, gap = 60.0)
    figure(
        View.SIDE, uprightPose(start), uprightPose(end), listOf(Floor, uprightBench, rack, Barbell()),
        startCaption = "About chin height",
        endCaption = "Arms straight overhead",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit on a shoulder-press bench in the rack, with your back straight against the pad.",
            RACK_SAFETY,
            "Lower it to the front of your shoulders, to about chin height.",
            "Press it overhead until your arms are straight, keeping your elbows forward.",
        ),
        contacts = UPRIGHT_CONTACTS + Contact(BodyPoint.HEAD, Surface.BACK_PAD, BOTH),
        checks = listOf(LockoutOverShoulders(Which.END)),
    )
}

/**
 * Smith shoulder press. Seated at the Smith machine on a bench whose back pad is 10° back from upright (neutral), the
 * back and head against it: the head is back, so the bar on its vertical rails, 8.5 cm in front of the shoulder
 * joints, passes just in front of the face (the guide's "pull your head back slightly"); START the bar at the front of the shoulders, the stops just under it ("can't come down below your
 * shoulders"); END pressed up the rails to straight arms. The grip wider than the shoulders: the hands 36 cm out
 * (neutral). ExRx Smith shoulder press (https://exrx.net/WeightExercises/DeltoidAnterior/SMShoulderPress).
 */
private val smithShoulderPress: ExerciseFigure by lazy {
    val backAngle = 80.0
    val torso = 180.0 - backAngle
    val bench = AdjustableBench(x = 30.0, backAngle = backAngle)
    val pelvis = seatedPelvis(adjustableHinge(bench), backAngle)
    val legs = Reach.leg(pelvis, Point(pelvis.x + 44.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
    val sh = shoulderOf(pelvis, torso)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, torso, nearArm = arm, nearLeg = legs)
    val half = 36.0
    val railX = sh.x + 8.5
    val start = arm3(sideShoulder(sh), V3(railX, sh.y - 2.0, half), V3(0.2, 1.0, 0.8), torso)
    val dz = half - Body.SHOULDER_HALF_WIDTH
    val end = arm3(sideShoulder(sh), V3(railX, sh.y - sqrt(ARM_REACH * ARM_REACH - dz * dz - 8.5 * 8.5), half), V3(0.0, 0.0, 1.0), torso)
    val barStart = pose(start).skeleton().near.grip
    val smith = SmithMachine(x = railX, stopHeight = -barStart.y - Barbell.SHAFT_RADIUS - 5.0)
    figure(
        View.SIDE, pose(start), pose(end), listOf(Floor, bench, smith, Barbell()),
        startCaption = "Front of your shoulders",
        endCaption = "Arms straight overhead",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set the safety stops so the bar can't come down below your shoulders.",
            "Sit on the bench with the bar in front of your shoulders.",
            "Press overhead until your arms are straight.",
            "Lower it to the front of your shoulders. Pull your head back slightly so the bar doesn't touch it.",
        ),
        contacts = UPRIGHT_CONTACTS + Contact(BodyPoint.HEAD, Surface.BACK_PAD, BOTH),
        checks = listOf(BarOnRails(Which.START), BarOnRails(Which.END), ElbowBend(0.0, 1.0, Which.END)),
    )
}

/**
 * Machine shoulder press. Seated with the back on the pad (10° back from upright, neutral, as the chest press
 * machines), feet flat; START the handles a little above the shoulders, gripped palms forward (the handles run across,
 * seen end-on), the elbows down and out; END pressed up until the elbows are nearly straight (10° left, neutral, as
 * the chest presses' "without locking"). The lever pivots behind the back pad, the same length in both panels. ExRx
 * lever shoulder press (https://exrx.net/WeightExercises/DeltoidAnterior/LVShoulderPress).
 */
private val machineShoulderPress: ExerciseFigure by lazy {
    val back = 80.0
    val torso = 180.0 - back
    val seat = 41.0
    val hinge = Point(40.0 - PressMachine.SEAT_LENGTH, -seat)
    val pelvis = seatedPelvis(hinge, back)
    val legs = Reach.leg(pelvis, Point(pelvis.x + 40.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
    val sh = shoulderOf(pelvis, torso)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, torso, nearArm = arm, nearLeg = legs)
    val half = 30.0
    val startGrip = sh + Point(12.0, -8.0)
    val start = arm3(sideShoulder(sh), v3(startGrip, half), V3(0.2, 1.0, 0.8), torso)
    val dz = half - Body.SHOULDER_HALF_WIDTH
    val reach = reachAt(10.0)
    val endGrip = sh + Point(6.0, -sqrt(reach * reach - dz * dz - 36.0))
    val end = arm3(sideShoulder(sh), v3(endGrip, half), V3(0.3, 0.6, 1.0), torso)
    val pivot = pivotAtX(startGrip, endGrip, hinge.x - 45.0)
    val machine = PressMachine(x = hinge.x + PressMachine.SEAT_LENGTH, seatHeight = seat, backAngle = back, pivot = pivot, handleEndOn = true)
    figure(
        View.SIDE, pose(start), pose(end), listOf(Floor, machine),
        startCaption = "Level with shoulders",
        endCaption = "Elbows nearly straight",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set the seat so the handles are level with your shoulders or a little above.",
            "Sit with your back against the pad.",
            "Press the handles up until your elbows are nearly straight.",
        ),
        contacts = listOf(
            Contact(BodyPoint.HIPS, Surface.SEAT, BOTH), Contact(BodyPoint.UPPER_BACK, Surface.BACK_PAD, BOTH),
            Contact(BodyPoint.FEET, Surface.FLOOR, BOTH),
        ),
        checks = listOf(ElbowBend(5.0, 15.0, Which.END)),
    )
}

/** Sitting back on a bench whose back pad is [back]° from level: the pelvis touching the seat and the pad. */
private fun seatedPelvis(hinge: Point, back: Double): Point {
    val r = back / DEG
    val t = (Body.TORSO_RADIUS - Body.TORSO_RADIUS * cos(r)) / sin(r)
    return hinge + unit(180.0 - back) * t + unit(90.0 - back) * Body.TORSO_RADIUS
}

/** The pivot at x = [x] the same distance from [a] and [b] (a lever's two positions). */
private fun pivotAtX(a: Point, b: Point, x: Double): Point {
    val m = (a + b) * 0.5
    val d = b - a
    return Point(x, m.y - (x - m.x) * d.x / d.y)
}

// ---- standing presses ----

private val tall by lazy { standingLeg(0.0, shinLean = 0.0, knee = 0.0, torso = 90.0) }
private val tallShoulder by lazy { shoulderOf(tall.hip, 90.0) }

/**
 * The dumbbell at the shoulder, as the approved DB shoulder press's START: the upper arm 50° out to the side (drawn
 * short), the elbow just below and 13 cm in front of the shoulder, the forearm upright under the dumbbell.
 */
private fun atShoulder(): Arm {
    val out = 50.0
    val ahead = 13.0
    val drawn = Segment.UPPER_ARM.length * cos(out / DEG)
    val elbowAt = Point(ahead, sqrt(drawn * drawn - ahead * ahead))
    val upper = angleTo(elbowAt)
    return Arm(shoulder = fold180(upper - 90.0 - 180.0), elbow = fold180(90.0 - upper), out = out)
}

/**
 * One-arm DB press. Standing tall, one dumbbell at the shoulder with the elbow under the wrist (the DB shoulder
 * press's START), the free arm hanging; END pressed overhead to a straight arm over the shoulder. The guide gives no
 * grip, so the dumbbell keeps the press's usual palm-forward grip, seen end-on. ExRx dumbbell one-arm shoulder press
 * (https://exrx.net/WeightExercises/DeltoidAnterior/DBOneArmShoulderPress).
 */
private val oneArmDbPress: ExerciseFigure by lazy {
    figure(
        View.SIDE, standOn(tall, 90.0, atShoulder(), HANG), standOn(tall, 90.0, Arm(180.0, 0.0), HANG), listOf(Floor, Dumbbell(Side.NEAR)),
        startCaption = "Dumbbell at one shoulder",
        endCaption = "Arm straight overhead",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand tall. Start with a dumbbell at one shoulder, with your elbow under your wrist.",
            "Press the dumbbell overhead until your arm is straight.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(LockoutOverShoulders(Which.END)),
    )
}

/**
 * Arnold press. Front view, so the guide's point shows: the elbows start together in front of the chest and open out
 * as the dumbbells go up. Standing tall; START the dumbbells in front of the shoulders, palms facing you (the handles
 * across, so seen in profile), the elbows in front of the chest under the wrists: the upper arms point forward
 * towards the camera (drawn short) and in, the forearms upright, the hands 24 cm apart just above the shoulders, below the chin; END
 * pressed overhead until the arms are straight, the arms 10° out from upright (the drawn shoulder's 170° limit,
 * Body.kt), palms forward (the handles still across). ExRx dumbbell Arnold press
 * (https://exrx.net/WeightExercises/DeltoidAnterior/DBArnoldPress); the hands' spacing and height are
 * neutral (the dumbbells side by side, just clear of each other).
 */
private val arnoldPress: ExerciseFigure by lazy {
    val shY = shoulderY(frontStanding(HANG))
    val sh = frontShoulder(shY)
    // The grip 12 cm right of the middle, 7 cm above the shoulder joints (in front of the shoulders, below the chin, so
    // the face shows above the dumbbells), straight above the elbow.
    val gripX = 12.0
    val elbowDrop = FORE_LINK - 7.0
    val dx = gripX - sh.x
    val elbow = V3(gripX, shY + elbowDrop, sqrt(Segment.UPPER_ARM.length * Segment.UPPER_ARM.length - dx * dx - elbowDrop * elbowDrop))
    val front = arm((elbow - sh).unit(), V3(0.0, -1.0, 0.0))
    val up = Arm(shoulder = 170.0, elbow = 0.0)
    figure(
        View.FRONT, frontStanding(front), frontStanding(up), listOf(Floor, Dumbbell(Side.FAR), Dumbbell(Side.NEAR)),
        startCaption = "Elbows under your wrists",
        endCaption = "Arms straight overhead",
        thumbnail = Which.START,
        guideLines = listOf(
            "Stand. Start with the dumbbells in front of your shoulders, palms facing you, with your elbows under your wrists.",
            "Then press the dumbbells overhead until your arms are straight.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(ElbowBend(0.0, 1.0, Which.END)),
    )
}

/**
 * Kettlebell press. Standing, the feet hip-width, the kettlebell's side's foot 12 cm in front of the other (neutral
 * "slightly"); START the kettlebell racked in front of the chest near the shoulder, the bell resting on the forearm,
 * the elbow down by the ribs (the upper arm 15° forward, the elbow bent 145°, neutral); END pressed straight
 * overhead to a straight arm, the bell behind the forearm. The free arm hangs. ACE single-arm overhead press
 * (https://www.acefitness.org/resources/everyone/exercise-library/395/single-arm-overhead-press/).
 */
private val kettlebellPress: ExerciseFigure by lazy {
    val pelvis = Point(0.0, -(Body.ANKLE_HEIGHT + 85.0))
    val near = Reach.leg(pelvis, Point(6.0, -Body.ANKLE_HEIGHT), 90.0, foot = 0.0)
    val far = Reach.leg(pelvis, Point(-6.0, -Body.ANKLE_HEIGHT), 90.0, foot = 0.0)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, 90.0, nearArm = arm, nearLeg = near, farArm = HANG, farLeg = far)
    val rack = Arm(shoulder = 15.0, elbow = 145.0)
    figure(
        View.SIDE, pose(rack), pose(Arm(180.0, 0.0)), listOf(Floor, Kettlebell(Side.NEAR, KettlebellHold.RACK)),
        startCaption = "In front of your chest",
        endCaption = "Press straight overhead",
        thumbnail = Which.START,
        guideLines = listOf(
            "Stand with your feet hip-width apart, one foot slightly in front of the other.",
            "Start with the kettlebell on the same side as your front foot, held in front of your chest near your shoulder and resting on your forearm.",
            "Push your front foot into the floor to shift your weight forward, and press the kettlebell straight overhead.",
            "Pull your elbow back down to your ribs to lower it.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(LockoutOverShoulders(Which.END)),
        endProps = listOf(Floor, Kettlebell(Side.NEAR, KettlebellHold.OVERHEAD)),
    )
}

/**
 * Landmine press. A split stance, feet far apart: the pressing side's (near) leg back and nearly straight, the other
 * forward with the knee bent; the torso leaning 5° forward with the back straight (neutral). The bar's far end sits in
 * the landmine on the floor in front; the near end, loaded, is held by its tip. START the end at the shoulder; END
 * pressed up and forward along its arc (the bar's length kept) until the arm is straight. The plates sit 20 cm from
 * the hand along the sleeve. ExRx lever one-arm shoulder press
 * (https://exrx.net/WeightExercises/DeltoidAnterior/LVOneArmShoulderPress); the stance and lengths are neutral (a bar
 * about 2.2 m long).
 */
private val landminePress: ExerciseFigure by lazy {
    val torso = 85.0
    val pelvis = Point(0.0, -(Body.ANKLE_HEIGHT + 76.0))
    // The back foot on the ball (the heel up 25°, neutral), the front foot flat.
    val back = Reach.leg(pelvis, ankleOnToes(-46.0, foot = -25.0), torso, foot = -25.0)
    val front = Reach.leg(pelvis, Point(36.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
    val sh = shoulderOf(pelvis, torso)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, torso, nearArm = arm, nearLeg = back, farArm = HANG, farLeg = front)
    val gripStart = sh + Point(20.0, 2.0)
    val start = Reach.arm(sh, gripStart, torso)
    // The bar's far end on the floor in front, 215 cm from the hand.
    val r = 215.0
    val pivot = Point(gripStart.x + sqrt(r * r - (gripStart.y + 4.0) * (gripStart.y + 4.0)), -4.0)
    // Up the arc until the hand is at arm's length from the shoulder.
    var a = angleTo(gripStart - pivot)
    var g = pivot + unit(a) * r
    while ((g - sh).length() < ARM_REACH - 0.01) {
        a -= 0.01
        g = pivot + unit(a) * r
    }
    val end = straightArm(sh, g, torso)
    figure(
        View.SIDE, pose(start), pose(end), listOf(Floor, TBarLever(pivot, plateAlong = r - 20.0)),
        startCaption = "End near your shoulder",
        endCaption = "Up and forward",
        thumbnail = Which.END,
        guideLines = listOf(
            "Hold the end of the bar near your shoulder.",
            "Set your feet far apart, with the leg on the pressing side back and the other leg forward and bent.",
            "Press the end of the bar up and forward until your arm is straight.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(ElbowBend(0.0, 1.0, Which.END, Side.NEAR)),
    )
}

// ---- front raises (side view) ----

/**
 * The toe of a foot standing on the floor: the ankle's height that puts the foot capsule's toe end on the floor for a
 * foot at screen angle [foot] (pointing down and forward, on the ball of the foot), as Back.kt's.
 */
private fun ankleOnToes(ankleX: Double, foot: Double): Point {
    val r = Body.FOOT_WIDTH / 2
    val sole = unit(foot - 90.0) * (Body.ANKLE_HEIGHT - r)
    val toe = sole + unit(foot) * (Segment.FOOT.length * 0.8 - r)
    return Point(ankleX, -r - toe.y)
}

/** Arms hanging slightly forward so the grips are [front] cm in front of the hip joint (the weight against the thighs). */
private fun hangForward(front: Double): Arm = Arm(shoulder = asin(front / ARM_REACH) * DEG, elbow = 0.0)

/**
 * Front raise. Standing tall; START the dumbbells resting against the fronts of the thighs, palms facing them (the
 * handles across: end-on); END raised forward and up to shoulder height, the elbows straight. ExRx dumbbell front
 * raise (https://exrx.net/WeightExercises/DeltoidAnterior/DBFrontRaise), ACE front raise
 * (https://www.acefitness.org/resources/everyone/exercise-library/54/front-raise/).
 */
private val frontRaise: ExerciseFigure by lazy {
    val start = hangForward(Body.THIGH_WIDTH / 2 + Dumbbell.HEAD_RADIUS)
    figure(
        View.SIDE, standOn(tall, 90.0, start), standOn(tall, 90.0, Arm(90.0, 0.0)), listOf(Floor, Dumbbell(Side.FAR), Dumbbell(Side.NEAR)),
        startCaption = "Against your thighs",
        endCaption = "Arms to shoulder height",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand holding a dumbbell in each hand, resting against your thighs, palms facing your thighs.",
            "Breathe out and raise the dumbbells forward and up until your arms are at shoulder height.",
            "Keep your elbows straight or slightly bent.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(HandsLevelWith(BodyPoint.SHOULDERS, 2.0, Which.END), ElbowBend(0.0, 1.0, Which.END)),
    )
}

/**
 * Barbell front raise. Standing tall; START the bar hanging in front of the thighs, palms facing them; END raised
 * forward and up to shoulder height, the elbows nearly straight (5°, neutral). ExRx barbell front raise
 * (https://exrx.net/WeightExercises/DeltoidAnterior/BBFrontRaise).
 */
private val barbellFrontRaise: ExerciseFigure by lazy {
    val start = hangForward(Body.THIGH_WIDTH / 2 + Barbell.SHAFT_RADIUS + 0.5)
    // Nearly straight: bent 5°, the arm raised so the grip is level with the shoulder.
    val r = reachAt(5.0)
    val inner = asin(FORE_LINK * sin(5.0 / DEG) / r) * DEG
    val end = Arm(shoulder = 90.0 - inner, elbow = 5.0)
    figure(
        View.SIDE, standOn(tall, 90.0, start), standOn(tall, 90.0, end), listOf(Floor, Barbell()),
        startCaption = "In front of your thighs",
        endCaption = "Bar to shoulder height",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand holding a barbell in front of your thighs, palms facing your thighs.",
            "Keep your elbows nearly straight.",
            "Raise the bar forward and up to about shoulder height.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(HandsLevelWith(BodyPoint.SHOULDERS, 2.0, Which.END), ElbowBend(3.0, 10.0, Which.END)),
    )
}

/**
 * Cable front raise. Standing a little in front of a low pulley behind (the pulley 15 cm up, neutral), the handle in
 * the near hand, the free arm hanging; START the arm slightly back at the side (12° back, neutral); END raised forward
 * and up to shoulder height, the elbow straight. The cable is drawn from the pulley to the handle in both panels. ExRx
 * cable one-arm front raise (https://exrx.net/WeightExercises/DeltoidAnterior/CBFrontRaise).
 */
private val cableFrontRaise: ExerciseFigure by lazy {
    val stack = CableStack(x = -70.0, pulleyHeight = 15.0, reach = 12.0, cableInFront = true)
    figure(
        View.SIDE, standOn(tall, 90.0, Arm(-12.0, 0.0), HANG), standOn(tall, 90.0, Arm(90.0, 0.0), HANG),
        listOf(Floor, stack, CableHandle(Side.NEAR)),
        startCaption = "Arm slightly back",
        endCaption = "Arm to shoulder height",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set a handle on a low pulley and grip it with one hand.",
            "Stand a little away from the pulley, with your arm slightly back at your side.",
            "Raise the handle forward and up to about shoulder height.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(HandsLevelWith(BodyPoint.SHOULDERS, 2.0, Which.END), ElbowBend(0.0, 1.0, Which.END, Side.NEAR)),
    )
}

// ---- cable rear delt row (side view) ----

/**
 * Cable rear delt row. The seated cable row's station and body (Back.kt), a straight bar gripped palms down; START
 * the arms straight towards the pulley; END the bar pulled to the upper chest, just below the neck (touching it), the
 * upper arms level and out to the sides (the elbows at shoulder height), the elbows 12 cm behind the shoulder
 * joints: just behind the back. With the fixed lengths, that end puts the hands about 30 cm out from the middle (the
 * elbows 48 cm out): the guide's "about elbow-width" is drawn on the narrow side. ExRx cable rear delt row (https://exrx.net/WeightExercises/DeltoidPosterior/CBRearDeltRow).
 */
private val cableRearDeltRow: ExerciseFigure by lazy {
    val bare = rowPose(HANG).skeleton()
    val sh = sideShoulder(rowSeatShoulder)
    val chest = bare.point(BodyPoint.UPPER_CHEST)
    // The elbow level with the shoulder, 12 cm behind it (2 cm behind the back), at the upper arm's length out to the
    // side; the hands as far out as puts the bar on the upper chest at the forearm's reach from it.
    val behind = 12.0
    val elbow = sh + V3(-behind, 0.0, sqrt(Segment.UPPER_ARM.length * Segment.UPPER_ARM.length - behind * behind))
    val g = chest + Point(1.3, 0.0)
    val gap = Point(g.x - elbow.x, g.y - elbow.y).length()
    val grip = V3(g.x, g.y, elbow.z - sqrt(FORE_LINK * FORE_LINK - gap * gap))
    val end = arm((elbow - sh).unit(), (grip - elbow).unit())
    figure(
        View.SIDE, rowPose(rowReachAt(grip.z)), rowPose(end), listOf(Floor, rowStation, CableAttachment(Attachment.STRAIGHT_BAR)),
        startCaption = "Arms straight",
        endCaption = "Elbows up and out",
        thumbnail = Which.END,
        guideLines = listOf(
            "Attach a bar to a seated cable row.",
            "Sit up straight and slide your hips back until your knees are only slightly bent.",
            "Pull the bar toward your upper chest, just below your neck, with your elbows up and out to the sides.",
            "Keep your upper arms level.",
            "Stop when your elbows are slightly behind your back.",
        ),
        contacts = SEATED_ROW_CONTACTS,
        checks = listOf(BarTouches(BodyPoint.UPPER_CHEST, Which.END), ElbowBend(0.0, 1.0, Which.START)),
    )
}

/** The seated row's stretch (Back.kt's), the hands [z] cm out from the middle: straight arms 15° down towards the pulley. */
private fun rowReachAt(z: Double): Arm {
    val sh = sideShoulder(rowSeatShoulder)
    val dz = z - Body.SHOULDER_HALF_WIDTH
    val flat = sqrt(ARM_REACH * ARM_REACH - dz * dz)
    return arm3(sh, V3(sh.x + flat * cos(15.0 / DEG), sh.y + flat * sin(15.0 / DEG), z), V3(0.0, 1.0, 0.0), 90.0)
}

// ---- front view: standing and seated ----

/** The front view's near shoulder joint (3D) for a lifter centred at x = 0, its shoulders at height [y], [z] forward. */
private fun frontShoulder(y: Double, z: Double = 0.0) = V3(Body.SHOULDER_HALF_WIDTH, y, z)

/** Standing, front view: the feet a little wider than the hips (5°), the thighs [kneesOut]° forward, the shins upright. */
private fun frontStanding(arm: Arm, kneesOut: Double = 4.0, lean: Double = 0.0, neckOut: Double = 0.0, far: Arm = arm): Pose {
    val leg = Leg(hip = 5.0, knee = 5.0, out = kneesOut)
    val drop = Segment.THIGH.length * cos(kneesOut / DEG) * cos(5.0 / DEG) + Segment.SHIN.length
    return Pose(0.0, -(Body.ANKLE_HEIGHT + drop), 90.0, nearArm = arm, nearLeg = leg, farArm = far, lean = lean, neckOut = neckOut)
}

/** Seated, front view: the thighs pointing at the camera (80° out of the plane), the shins upright, feet flat. */
private fun frontSeatedLeg(hip: Double) = Leg(hip = hip, knee = hip, out = 80.0)
private val FRONT_SEATED_Y by lazy { -(Body.ANKLE_HEIGHT + Segment.THIGH.length * cos(80.0 / DEG) + Segment.SHIN.length) }
private fun frontSeated(arm: Arm, hip: Double = 10.0, lean: Double = 0.0, neckOut: Double = 0.0, far: Arm = arm) =
    Pose(0.0, FRONT_SEATED_Y, 90.0, nearArm = arm, nearLeg = frontSeatedLeg(hip), farArm = far, lean = lean, neckOut = neckOut)
private val frontSeatTop by lazy { -(FRONT_SEATED_Y + Body.TORSO_RADIUS) }

/** The near shoulder joint's height of a front-view pose. */
private fun shoulderY(p: Pose) = p.skeleton(View.FRONT).near.shoulder.y

/** The upper arm hanging [abduct]° out from the side, and out to the side level ([LEVEL]): front-view directions. */
private fun sideDown(abduct: Double) = V3(sin(abduct / DEG), cos(abduct / DEG), 0.0)
private val LEVEL = V3(1.0, 0.0, 0.0)

/** Forward, towards the camera (front view). */
private val FORWARD = V3(0.0, 0.0, 1.0)

/**
 * The lateral raise's arms: START hanging [abduct]° out from the sides, END the upper arms level out to the sides; the
 * elbows bent [RAISE_BEND]° and pointing back, so the forearms angle forward (the hands a little in front of the
 * elbows, at the top level with them: never the elbows below the wrists).
 */
private fun lateralArms(abduct: Double): Pair<Arm, Arm> =
    arm(sideDown(abduct), bent(sideDown(abduct), FORWARD, RAISE_BEND)) to arm(LEVEL, bent(LEVEL, FORWARD, RAISE_BEND))

private const val TOP_CAPTION = "Out to shoulder height"

/**
 * Lateral raise. Front view: standing, the feet a little wider than the hips, a dumbbell in each hand at the sides,
 * palms facing the body (so the handles point at the camera: end-on); START the arms hanging, just clear of the
 * thighs; END the upper arms raised out to the sides until the elbows are at shoulder height, the slight bend fixed.
 * ExRx dumbbell lateral raise (https://exrx.net/WeightExercises/DeltoidLateral/DBLateralRaise), ACE lateral raise
 * (https://www.acefitness.org/resources/everyone/exercise-library/26/lateral-raise/).
 */
private val lateralRaise: ExerciseFigure by lazy {
    val (down, up) = lateralArms(10.0)
    figure(
        View.FRONT, frontStanding(down), frontStanding(up), listOf(Floor, Dumbbell(Side.FAR, endOnFront = true), Dumbbell(Side.NEAR, endOnFront = true)),
        startCaption = "Palms facing your body",
        endCaption = TOP_CAPTION,
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand holding a dumbbell in each hand, palms facing your body.",
            "Bend your elbows slightly and point them back.",
            "Raise your upper arms out to the sides until your elbows are at shoulder height.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(ElbowBend(10.0, 30.0, Which.START), ElbowBend(10.0, 30.0, Which.END)),
    )
}

/**
 * Seated lateral raise. Front view: seated on the end of a bench (seen end-on), the legs together; START the arms
 * hanging at the sides, slightly bent, palms in (end-on dumbbells), clear of the bench; END the elbows raised to
 * shoulder height, the bend the same. ExRx dumbbell seated lateral raise
 * (https://exrx.net/WeightExercises/DeltoidLateral/DBSeatedLateralRaise).
 */
private val seatedLateralRaise: ExerciseFigure by lazy {
    val (down, up) = lateralArms(12.0)
    val bench = Seat(x = 0.0, height = frontSeatTop, width = FlatBench.WIDTH)
    figure(
        View.FRONT, frontSeated(down, hip = 2.0), frontSeated(up, hip = 2.0),
        listOf(Floor, bench, Dumbbell(Side.FAR, endOnFront = true), Dumbbell(Side.NEAR, endOnFront = true)),
        startCaption = "Arms hang, palms in",
        endCaption = TOP_CAPTION,
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit on the end of a bench with your legs together, holding a dumbbell in each hand.",
            "Let your arms hang down, slightly bent, with your palms facing in.",
            "Raise your upper arms out to the sides until your elbows are at shoulder height.",
            "Keep the bend in your elbows the same.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH), Contact(BodyPoint.HIPS, Surface.SEAT, BOTH)),
        checks = listOf(ElbowBend(10.0, 30.0, Which.START), ElbowBend(10.0, 30.0, Which.END)),
    )
}

/**
 * Cable lateral raise. Front view: standing side-on to a low pulley (15 cm up, neutral) on the free arm's side, the
 * handle in the other (near) hand, the free arm hanging; START the arm hanging at the side, the elbow slightly bent;
 * END raised out to the side, away from the pulley, until the elbow is at shoulder height. The cable runs from the
 * pulley across in front of the body to the handle (drawn in front). ExRx cable one-arm lateral raise
 * (https://exrx.net/WeightExercises/DeltoidLateral/CBOneArmLateralRaise).
 */
private val cableLateralRaise: ExerciseFigure by lazy {
    val (down, up) = lateralArms(6.0)
    val stack = CableStack(x = -62.0, pulleyHeight = 15.0, reach = 12.0, cableInFront = true)
    figure(
        View.FRONT, frontStanding(down, far = HANG), frontStanding(up, far = HANG), listOf(Floor, stack, CableHandle(Side.NEAR)),
        startCaption = "Side-on to the pulley",
        endCaption = "Elbow to shoulder height",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set a handle on a low pulley.",
            "Stand side-on to the pulley, with your free arm nearest to it. Grip the handle with your other hand.",
            "Raise your arm out to the side, away from the pulley, until your elbow is at shoulder height.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(ElbowBend(10.0, 30.0, Which.START, Side.NEAR), ElbowBend(10.0, 30.0, Which.END, Side.NEAR)),
    )
}

/**
 * Machine lateral raise. Front view: seated in the machine, the back on its pad, the arms bent at a right angle
 * (neutral) with the forearms forward, the pads on the outside of the upper arms by the elbows; each pad's lever
 * pivots level with the shoulder joint. START the upper arms at the sides; END raised out to the sides until level
 * with the floor. ExRx lever lateral raise (https://exrx.net/WeightExercises/DeltoidLateral/LVLateralRaise).
 */
private val machineLateralRaise: ExerciseFigure by lazy {
    val ahead = u3(-0.15, 0.25, 1.0)
    val down = sideDown(8.0)
    val start = arm(down, bent(down, ahead, 90.0))
    val end = arm(LEVEL, bent(LEVEL, ahead, 90.0))
    val shY = shoulderY(frontSeated(start))
    val machine = LateralRaiseMachine(x = 0.0, seatHeight = frontSeatTop, padTop = -shY - 6.0, pivotHeight = -shY)
    figure(
        View.FRONT, frontSeated(start), frontSeated(end), listOf(Floor, machine),
        startCaption = "Pads outside your arms",
        endCaption = "Upper arms to level",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit in the machine with your bent arms between the pads and your sides, so the pads rest on the outside of your arms.",
            "Raise your arms out to the sides until your upper arms are level with the floor.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH), Contact(BodyPoint.HIPS, Surface.SEAT, BOTH)),
        checks = listOf(ElbowBend(85.0, 95.0, Which.START), ElbowBend(85.0, 95.0, Which.END)),
    )
}

// ---- rear delts ----

/**
 * The rear delt raises' arms (front view, the torso head-on): START hanging straight down under the shoulders,
 * [abduct]° out; END the upper arms straight out from the shoulders, level; the elbows' slight bend ([RAISE_BEND])
 * fixed, the forearms angled towards the head (forward, at the camera).
 */
private fun rearArms(abduct: Double): Pair<Arm, Arm> {
    val inward = u3(-0.5, 0.0, 0.85)
    return arm(sideDown(abduct), bent(sideDown(abduct), inward, RAISE_BEND)) to arm(LEVEL, bent(LEVEL, u3(0.0, 0.4, 0.9), RAISE_BEND))
}

/** The rear delt raises' dumbbells: palms facing each other, so the handles point at the camera (end-on). */
private val REAR_DUMBBELLS = listOf(Dumbbell(Side.FAR, endOnFront = true), Dumbbell(Side.NEAR, endOnFront = true))

/** "Close to level": the torso 20° above level, as the bent-over row (Back.kt ROW_TORSO); seen head-on, a 70° lean. */
private const val BENT_LEAN = 70.0

/**
 * Rear delt raise. Front view, the torso head-on: standing with the knees bent (the thighs 25° forward, neutral) and
 * bent over at the hips until the torso is close to level (20° above it, as the bent-over row), the head in line;
 * START the arms hanging under the shoulders, the dumbbells palms facing each other (end-on); END the upper arms
 * raised straight out from the shoulders until the elbows are at shoulder height, the slight bend fixed. The legs
 * stand under the hips, behind the torso, so they show below it. ExRx dumbbell rear lateral raise
 * (https://exrx.net/WeightExercises/DeltoidPosterior/DBRearLateralRaise).
 */
private val rearDeltRaise: ExerciseFigure by lazy {
    val (down, up) = rearArms(0.0)
    fun pose(arm: Arm) = frontStanding(arm, kneesOut = 25.0, lean = BENT_LEAN, neckOut = 75.0)
    figure(
        View.FRONT, pose(down), pose(up), listOf(Floor) + REAR_DUMBBELLS,
        startCaption = "Torso close to level",
        endCaption = "Out to the sides",
        thumbnail = Which.END,
        guideLines = listOf(
            "Hold a dumbbell in each hand, palms facing each other.",
            "Bend your knees and bend over at your hips, until your torso is close to level with the floor.",
            "Raise your upper arms out to the sides until your elbows are at shoulder height.",
            "Keep your upper arms straight out from your shoulders, and keep the bend in your elbows the same.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(ElbowBend(10.0, 30.0, Which.START), ElbowBend(10.0, 30.0, Which.END)),
    )
}

/**
 * Seated rear delt raise. Front view, the torso head-on: seated on the edge of a bench (seen end-on), bent well over
 * towards the thighs, the head in line with the back (sunk to the shoulders' line, as the standing Rear delt raise),
 * so the shoulders sit low over the knees and the torso shows as a short block. The hip's 140° flexion limit (Body.kt,
 * the conservative end) sets how far: the thighs slope down 18° to the knees (sitting on the bench's edge, the feet
 * tucked a little back, shins 15° back; neutral) and the torso is 22.5° above level (a 67.5° lean), the chest about
 * 5 cm short of the thighs, which the front view cannot show. A bench end about 52 cm up suits that (neutral). START
 * the arms hanging down outside the legs, clear of them; END the elbows raised out to shoulder height, the bend the
 * same. ExRx dumbbell seated rear lateral raise
 * (https://exrx.net/WeightExercises/DeltoidPosterior/DBSeatedRearLateralRaise).
 */
private val seatedRearDeltRaise: ExerciseFigure by lazy {
    val (down, up) = rearArms(13.0)
    val thighOut = 72.0
    val shinBack = -15.0
    val leg = Leg(hip = 6.0, knee = 6.0, out = thighOut, shinOut = shinBack)
    val pelvisY = -(Body.ANKLE_HEIGHT + Segment.THIGH.length * cos(thighOut / DEG) * cos(6.0 / DEG) + Segment.SHIN.length * cos(shinBack / DEG))
    fun pose(arm: Arm) = Pose(0.0, pelvisY, 90.0, nearArm = arm, nearLeg = leg, lean = 67.5, neckOut = 75.0)
    val bench = Seat(x = 0.0, height = -(pelvisY + Body.TORSO_RADIUS), width = FlatBench.WIDTH)
    figure(
        View.FRONT, pose(down), pose(up), listOf(Floor, bench) + REAR_DUMBBELLS,
        startCaption = "Bend over",
        endCaption = TOP_CAPTION,
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit on the edge of a bench, holding a dumbbell in each hand.",
            "Bend over and rest your chest on your thighs.",
            "Raise your upper arms out to the sides until your elbows are at shoulder height.",
            "Keep the bend in your elbows the same.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH), Contact(BodyPoint.HIPS, Surface.SEAT, BOTH)),
        checks = listOf(ElbowBend(10.0, 30.0, Which.START), ElbowBend(10.0, 30.0, Which.END)),
    )
}

/**
 * Chest-supported rear delt raise. Front view, head-on from the bench's head end: lying face down on a bench close to
 * flat (10° up towards the head, so an 80° lean; ExRx: close to horizontal, high enough for the dumbbells), the bench
 * seen end-on under the chest (drawn wide, see below), its top about 68 cm up so the hanging dumbbells clear the floor; the legs lie straight back along
 * the bench, behind the shoulders, and are hidden. START the arms hanging straight down beside the bench; END the
 * elbows raised out to shoulder height, the bend fixed. ExRx dumbbell lying rear lateral raise
 * (https://exrx.net/WeightExercises/DeltoidPosterior/DBLyingRearLateralRaise).
 */
private val chestSupportedRearDeltRaise: ExerciseFigure by lazy {
    val (down, up) = rearArms(14.0)
    val lean = 80.0
    val legs = Leg(hip = 0.0, knee = 0.0, out = -90.0, shinOut = -90.0)
    val shoulderHigh = 80.0
    val pelvisY = -shoulderHigh + Segment.TORSO.length * cos(lean / DEG)
    fun pose(arm: Arm) = Pose(0.0, pelvisY, 90.0, nearArm = arm, nearLeg = legs, lean = lean, neckOut = 60.0)
    // The pad's end under the chest: its top a chest's depth below the shoulder joints. Drawn 46 cm wide, wider than a
    // real 30 cm pad and than the torso, so the end-on bench reads as a bench under the body, not a stool behind it
    // (the one size here not at real scale); the arms hang outside it.
    val bench = Seat(x = 0.0, height = shoulderHigh - Body.CHEST_DEPTH, width = 46.0)
    figure(
        View.FRONT, pose(down), pose(up), listOf(Floor, bench) + REAR_DUMBBELLS,
        startCaption = "Lie face down",
        endCaption = TOP_CAPTION,
        thumbnail = Which.END,
        guideLines = listOf(
            "Set a bench flat or close to flat, high enough that the dumbbells clear the floor.",
            "Lie face down on it, holding a dumbbell in each hand.",
            "Raise your upper arms out to the sides until your elbows are at shoulder height.",
            "Keep the bend in your elbows the same.",
        ),
        contacts = emptyList(),
        checks = listOf(ElbowBend(10.0, 30.0, Which.START), ElbowBend(10.0, 30.0, Which.END)),
    )
}

/**
 * Machine reverse fly. Front view: seated facing the machine, the chest against its pad (drawn over the torso: it is
 * between the camera and the chest), the seat set so the handles are at shoulder height; START the arms reaching
 * forward to the handles in front, the elbows raised to shoulder height, a slight bend (10°, neutral); END the
 * handles pulled apart and back until the elbows are just behind the torso (the upper arms 12° behind the body's
 * plane, neutral), still at shoulder height. The machine's arms hang from pivots above the shoulders, as the pec
 * deck's. ExRx lever seated reverse fly (https://exrx.net/WeightExercises/DeltoidPosterior/LVReverseFlyOverhand).
 */
private val machineReverseFly: ExerciseFigure by lazy {
    val bend = 10.0
    val ahead = u3(-0.3, 0.0, 1.0)
    val start = arm(ahead, bent(ahead, LEVEL, bend))
    val back = u3(1.0, 0.0, -tan(12.0 / DEG))
    val end = arm(back, bent(back, FORWARD, bend))
    val bare = frontSeated(HANG).skeleton(View.FRONT)
    val shY = bare.near.shoulder.y
    val machine = PecDeck(x = 0.0, seatHeight = frontSeatTop, padTop = -shY - 4.0, pivotHeight = -bare.head.y + 22.0, facingPad = true)
    figure(
        View.FRONT, frontSeated(start), frontSeated(end), listOf(Floor, machine),
        startCaption = "Chest against the pad",
        endCaption = "Just behind your torso",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set the seat so the handles are at shoulder height.",
            "Sit facing the machine with your chest against the pad.",
            "Grip the handles palms down, with your elbows raised to shoulder height.",
            "Pull the handles apart and back until your elbows are just behind your torso.",
            "Keep your elbows at shoulder height.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH), Contact(BodyPoint.HIPS, Surface.SEAT, BOTH)),
        checks = listOf(
            HandsLevelWith(BodyPoint.SHOULDERS, 4.0, Which.START), HandsLevelWith(BodyPoint.SHOULDERS, 4.0, Which.END),
            ElbowBend(5.0, 15.0, Which.START), ElbowBend(5.0, 15.0, Which.END),
        ),
    )
}

/**
 * Cable reverse fly. Front view, seen from behind: standing facing two pulleys set close together at shoulder height
 * (the columns 30 cm either side of the middle, beyond the lifter, so drawn behind), a handle in each hand on the
 * crossed cables (each hand on the other side's cable); the columns are beyond the lifter, so drawn behind every limb
 * (CableStack.beyond). START the arms reaching forward at shoulder height, the hands just outside the shoulders,
 * slightly bent (10°, neutral); END pulled out to the sides, the upper arms level at shoulder height,
 * the bend the same. ExRx cable reverse fly (https://exrx.net/WeightExercises/DeltoidPosterior/CBStandingReverseFly).
 */
private val cableReverseFly: ExerciseFigure by lazy {
    val bend = 10.0
    val bare = frontStanding(HANG)
    val shY = shoulderY(bare)
    // Reaching forward towards the pulleys, the hands just outside the shoulders: seen from behind the arms are then
    // mostly hidden by the back, and nothing crosses over it.
    val reachIn = u3(0.05, 0.0, 1.0)
    val start = arm(reachIn, bent(reachIn, LEVEL, bend))
    val out = u3(1.0, 0.0, 0.08)
    val end = arm(out, bent(out, FORWARD, bend))
    val stack = CableStack(x = 0.0, pulleyHeight = -shY, reach = 6.0, halfSpan = 30.0, crossed = true, beyond = true)
    figure(
        View.FRONT, frontStanding(start), frontStanding(end), listOf(Floor, stack, CableHandles),
        startCaption = "Face the pulleys",
        endCaption = "Arms level, out to sides",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set two pulleys close together, at about shoulder height.",
            "Face the pulleys and grip a handle in each hand. Step back until the cables are tight.",
            "Pull the handles out to the sides.",
            "Keep your upper arms moving level, at shoulder height, and keep the bend in your elbows the same.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(
            HandsLevelWith(BodyPoint.SHOULDERS, 4.0, Which.START), HandsLevelWith(BodyPoint.SHOULDERS, 4.0, Which.END),
            ElbowBend(5.0, 15.0, Which.START), ElbowBend(5.0, 15.0, Which.END),
        ),
    )
}

// ---- rotations ----

/** The elbow at the side: the upper arm hanging straight down beside the torso (its inner edge on the torso's side). */
private val AT_SIDE = V3(0.0, 1.0, 0.0)

/**
 * The forearm's two ends of a rotation, the elbow bent 90°: across the belly (turned in, 35° forward of pointing
 * straight across, so the hand passes in front of the belly) and turned out 45° past pointing straight forward. The
 * guides fix neither end beyond "across your belly" and "away from your body": 45° out is a neutral, conservative
 * value, well inside the arm-at-side external rotation the shoulder allows. ExRx cable standing external rotation
 * (https://exrx.net/WeightExercises/Infraspinatus/CBStandingExternalRotation): the elbow against the side at 90°, the
 * forearm across the belly at the start.
 */
private val ACROSS = V3(-cos(35.0 / DEG), 0.0, sin(35.0 / DEG))
private val TURNED_OUT = V3(sin(45.0 / DEG), 0.0, cos(45.0 / DEG))

/** A standing rotation's near arm: the elbow at the side, bent 90°, the forearm along [forearm]. */
private fun rotation(forearm: V3) = arm(AT_SIDE, forearm)

/**
 * Cable external rotation. Front view: standing side-on to a pulley at about waist height on the far side (the column
 * beside the lifter), the handle in the hand farther from it (near), the elbow against the side and bent 90°, the
 * free arm hanging; START the forearm across the belly; END turned out, the hand moved away from the body. The cable
 * runs from the pulley across in front of the body to the handle.
 */
private val cableExternalRotation: ExerciseFigure by lazy {
    val start = rotation(ACROSS)
    val grip = frontStanding(start, far = HANG).skeleton(View.FRONT).near.grip
    val stack = CableStack(x = -62.0, pulleyHeight = -grip.y, reach = 12.0, cableInFront = true)
    figure(
        View.FRONT, frontStanding(start, far = HANG), frontStanding(rotation(TURNED_OUT), far = HANG), listOf(Floor, stack, CableHandle(Side.NEAR)),
        startCaption = "Forearm across belly",
        endCaption = "Hand away from your body",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set a cable handle at about waist height. With a band, fix it at the same height.",
            "Stand side-on to it and hold the handle in the hand farther from it.",
            "Bend your elbow and keep it against your side, with your forearm across your belly.",
            "Turn your arm outward, moving your hand away from your body.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(ElbowBend(89.0, 91.0, Which.START, Side.NEAR), ElbowBend(89.0, 91.0, Which.END, Side.NEAR)),
    )
}

/**
 * Cable internal rotation. Front view: standing side-on to a pulley at elbow height on the near side, the handle in
 * the hand nearer to it, the elbow against the side and bent 90°, the free arm hanging; START the forearm turned out
 * towards the pulley (45° past forward, neutral, as the external rotation's end); END turned in, the forearm across
 * the belly. ExRx cable standing internal rotation (https://exrx.net/WeightExercises/Subscapularis/CBStandingInternalRotation).
 */
private val cableInternalRotation: ExerciseFigure by lazy {
    val start = rotation(TURNED_OUT)
    val elbowY = frontStanding(start, far = HANG).skeleton(View.FRONT).near.elbow.y
    val stack = CableStack(x = 70.0, pulleyHeight = -elbowY, reach = -12.0, cableInFront = true)
    figure(
        View.FRONT, frontStanding(start, far = HANG), frontStanding(rotation(ACROSS), far = HANG), listOf(Floor, stack, CableHandle(Side.NEAR)),
        startCaption = "Elbow against your side",
        endCaption = "Forearm across belly",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set a handle on a cable at about elbow height.",
            "Stand side-on to the pulley and hold the handle in the hand nearer to it.",
            "Bend your elbow and keep it against your side.",
            "Turn your arm inward, bringing your forearm across your belly.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(ElbowBend(89.0, 91.0, Which.START, Side.NEAR), ElbowBend(89.0, 91.0, Which.END, Side.NEAR)),
    )
}

/**
 * Side-lying external rotation. Front view of a body lying on its side along the floor, the head to the left, seen
 * from in front of the chest: the torso drawn across the picture (6° up towards the head, so both the shoulder's and
 * the hip's undersides rest on the floor), the bottom arm reaching along the floor under the head, the top leg a
 * little forward of the bottom one (the legs apart for support: drawn slightly short). The top arm's elbow lies
 * against the side, bent 90°; START the forearm hanging down across the belly towards the floor (in front of it);
 * END turned out, the dumbbell raised until the forearm points up. The dumbbell's handle stays along the body (in
 * profile). ExRx dumbbell lying external rotation (https://exrx.net/WeightExercises/Infraspinatus/DBLyingExternalRotation).
 */
private val sideLyingExternalRotation: ExerciseFigure by lazy {
    val torso = 174.0
    val pelvis = Point(0.0, -21.0)
    val top = Leg(hip = 0.0, knee = 10.0, out = 25.0)
    val bottom = Leg(hip = -4.0, knee = 0.0)
    // The top (near) arm along the torso towards the hips; the forearm hanging (in front of the belly) or up.
    val hanging = Arm(shoulder = 0.0, elbow = -90.0, forearmOut = 50.0)
    val up = Arm(shoulder = 0.0, elbow = 90.0)
    val under = Arm(shoulder = 170.0, elbow = 0.0)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, torso, neck = -6.0, nearArm = arm, nearLeg = top, farArm = under, farLeg = bottom)
    figure(
        View.FRONT, pose(hanging), pose(up), listOf(Floor, Dumbbell(Side.NEAR)),
        startCaption = "Elbow against your side",
        endCaption = "Turn to raise it",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie on your side, with your legs apart for support.",
            "Hold a dumbbell in your top hand.",
            "Bend your elbow and keep it against your side, with your forearm across your belly.",
            "Turn your upper arm outward to raise the dumbbell, keeping your elbow against your side.",
        ),
        contacts = listOf(Contact(BodyPoint.FAR_FOOT, Surface.FLOOR, BOTH)),
        checks = listOf(ElbowBend(89.0, 91.0, Which.START, Side.NEAR), ElbowBend(89.0, 91.0, Which.END, Side.NEAR)),
    )
}

/** The batch's figures by catalogue name, each built on first use (Figures.kt's laziness rule). */
private val shoulderBatchFigures: Map<String, Lazy<ExerciseFigure>> by lazy {
    mapOf(
        "Seated barbell press" to lazy { seatedBarbellPress },
        "One-arm DB press" to lazy { oneArmDbPress },
        "Arnold press" to lazy { arnoldPress },
        "Machine shoulder press" to lazy { machineShoulderPress },
        "Smith shoulder press" to lazy { smithShoulderPress },
        "Landmine press" to lazy { landminePress },
        "Kettlebell press" to lazy { kettlebellPress },
        "Lateral raise" to lazy { lateralRaise },
        "Seated lateral raise" to lazy { seatedLateralRaise },
        "Cable lateral raise" to lazy { cableLateralRaise },
        "Machine lateral raise" to lazy { machineLateralRaise },
        "Front raise" to lazy { frontRaise },
        "Barbell front raise" to lazy { barbellFrontRaise },
        "Cable front raise" to lazy { cableFrontRaise },
        "Rear delt raise" to lazy { rearDeltRaise },
        "Seated rear delt raise" to lazy { seatedRearDeltRaise },
        "Chest-supported rear delt raise" to lazy { chestSupportedRearDeltRaise },
        "Machine reverse fly" to lazy { machineReverseFly },
        "Cable reverse fly" to lazy { cableReverseFly },
        "Cable rear delt row" to lazy { cableRearDeltRow },
        "Cable external rotation" to lazy { cableExternalRotation },
        "Side-lying external rotation" to lazy { sideLyingExternalRotation },
        "Cable internal rotation" to lazy { cableInternalRotation },
    )
}

internal val shoulderFigures: Map<String, Lazy<ExerciseFigure>> by lazy {
    mapOf("DB shoulder press" to lazy { dbShoulderPress }) + shoulderBatchFigures
}
