package com.powertracker.domain.library.figures.areas

import com.powertracker.domain.library.figures.*
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Figures for the library's Core area (library/areas/Core.kt), keyed by catalogue name.
//
// Camera (Task 12). The side view for the crunches, the front plank, the dead bug and bird dog, the rollouts, the
// Pallof press and three of the carries: what each guide is about (the trunk curling with the hips still or the hips
// rolling up, the straight line from heels to head, the opposite arm and leg reaching, the long body at the reach, the
// arms pressing out and back, the upright trunk and the load's place) shows from the side. Case by case:
// - Side plank: seen from the front of the body (as the side-lying figures, HamstringsGlutes.kt), so the line from the
//   feet to the head and the hips lifted off the floor show; from the side the body would hide behind itself.
// - Pallof press: side on. Seen from the front, the arms would press straight out at the camera and the press would
//   not show (foreshortened to nothing), and no view from above is available. Side on, the arms press out from the
//   chest and back, and the column stands beside the lifter, beyond the body (drawn behind it), its pulley level with
//   the chest; the cable runs from it level to the handle (it comes out of the picture, so it shows as a level line).
// - Cable woodchop: the front view. The sweep is a diagonal across the body, from above the near shoulder down to the
//   far hip, which a side view would fold onto one line; from the front the cable's line from the pulley beside the
//   lifter and the arms' path both show. The trunk's turn is out of the picture and is not drawn.
// - Suitcase carry: the front view, so the load on one side only and the level shoulders and upright trunk show
//   (lessons 9 and 10: the farmer's walk, both hands loaded, is the side view).
// The carries' START and END would be the same pose, so they show what each guide stresses: START standing tall with
// the load where the guide puts it, END mid-stride with it still there (each figure says what its panels are).
// Every position a guide does not fix is sourced in its comment (allowed domains only), or kept neutral and marked so.
//
// The figure's torso is one rigid piece (Body.kt), so a crunch's curl is drawn as the torso tilting about the hips,
// the hips and thighs staying where they were (the guides' "hips stay still"), with the chin tucked.

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

/** A straight arm pointing at the screen angle [world] whatever the torso's lean (straight down is 270). */
private fun armAt(world: Double, torso: Double, elbow: Double = 0.0) = Arm(shoulder = fold180(world - torso - 180.0), elbow = elbow)

private const val BRACE_TRUNK = "Brace your trunk before each rep."
private const val WALK_SLOW = "Walk slowly and under control."

// ---- lying on the back ----

/** Lying face up along the floor, the head to the left: the torso level with its back on the floor. */
private const val SUPINE = 180.0
private val SUPINE_PELVIS = Point(0.0, -Body.TORSO_RADIUS)

/**
 * The crunch's top: the torso 30° up from the floor, the shoulder blades off it. ExRx's curl-up test protocol sets its
 * tape so the curl reaches "30 degree spinal flexion" (https://exrx.net/Testing/CurlUpTests), and ACE's group fitness
 * assessment protocols put the shoulder blades off the mat at about 30° of trunk flexion
 * (https://www.acefitness.org/groupfitnessresources/pdfs/GFI_Assessments.pdf), both seen in the search result.
 */
private const val CRUNCH = 30.0

/** The knees-bent, feet-flat legs of a lying figure: the ankle 58 cm past the hips (knees about 95°, neutral). */
private fun bentLegs(pelvis: Point, torso: Double): Leg = Reach.leg(pelvis, Point(SUPINE_PELVIS.x + 58.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)

/**
 * The hands behind the head, the elbows pulled back and out to the sides: the near hand over the side of the head,
 * behind the ear, the elbow pointing out of the picture towards the camera and back towards the head (3D arm).
 */
private fun handBehindHead(pose: Pose, elbowForward: Double = 0.0): Arm {
    val s = pose.skeleton()
    val back = unit(pose.torso - pose.neck + 90.0)
    val headDir = unit(pose.torso - pose.neck)
    val grip = s.head + back * 4.0 - headDir * 2.0
    val sh = v3(s.shoulder, Body.SHOULDER_HALF_WIDTH)
    val g = v3(grip, Body.HEAD_RADIUS * 0.6)
    val toKnee = unit(pose.torso + 180.0)
    val hint = V3(toKnee.x * elbowForward + headDir.x * 0.3, toKnee.y * elbowForward + headDir.y * 0.3, 1.0)
    return arm3(sh, g, hint, pose.torso)
}

/**
 * Floor crunch. Lying on the back, the knees bent, the feet flat, the hands behind the head with the elbows back.
 * START the back on the floor; END the ribs curled towards the hips until the upper back is off the floor ([CRUNCH]),
 * the chin tucked slightly (the neck 15°, neutral), the feet, tailbone and lower back still down: the pelvis and the
 * thighs stay put. ACE crunch (https://www.acefitness.org/resources/everyone/exercise-library/52/crunch/, Sources.kt).
 */
private val floorCrunch: ExerciseFigure by lazy {
    fun pose(torso: Double, neck: Double): Pose {
        val bare = Pose(SUPINE_PELVIS.x, SUPINE_PELVIS.y, torso, neck = neck, nearArm = HANG, nearLeg = bentLegs(SUPINE_PELVIS, torso))
        return bare.copy(nearArm = handBehindHead(bare)).let { it.copy(farArm = it.nearArm) }
    }
    figure(
        View.SIDE, pose(SUPINE, 8.0), pose(SUPINE - CRUNCH, 15.0), listOf(Floor),
        startCaption = "Knees bent, feet flat",
        endCaption = "Upper back off the floor",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie on your back with your knees bent and your feet flat on the floor.",
            "Curl your ribs toward your hips until your upper back is off the floor. Let your chin tuck slightly.",
            "Keep your feet, tailbone and lower back on the floor.",
        ),
        contacts = FEET_DOWN + Contact(BodyPoint.HIPS, Surface.FLOOR, BOTH),
        checks = emptyList(),
    )
}

/**
 * Bicycle crunch. The upper body curled up ([CRUNCH]) and held there, the hands behind the head. START the far knee
 * pulled in (the thigh 10° past upright towards the chest, the knee 90°) with the near elbow reaching towards it,
 * across the body, while the near leg is straight, about 30° off the floor (neutral); END switched: the near knee in,
 * the far leg straight, the far elbow towards the near knee. The turn of the trunk is out of the picture; the elbow
 * swinging forward towards the bent knee stands for it. ACE supine bicycle crunches
 * (https://www.acefitness.org/resources/everyone/exercise-library/241/supine-bicycle-crunches/, Sources.kt).
 */
private val bicycleCrunch: ExerciseFigure by lazy {
    val torso = SUPINE - CRUNCH
    val inLeg = Leg(hip = fold180(100.0 - torso - 180.0), knee = 90.0, ankle = -15.0)
    val outLeg = Leg(hip = fold180(30.0 - torso - 180.0), knee = 0.0, ankle = -25.0)
    fun pose(nearIn: Boolean): Pose {
        val bare = Pose(SUPINE_PELVIS.x, SUPINE_PELVIS.y, torso, neck = 15.0, nearArm = HANG, nearLeg = if (nearIn) inLeg else outLeg, farLeg = if (nearIn) outLeg else inLeg)
        // The elbow on the bent knee's opposite side swings forward towards it; the other stays back.
        return bare.copy(nearArm = handBehindHead(bare, if (nearIn) 0.0 else 1.2), farArm = handBehindHead(bare, if (nearIn) 1.2 else 0.0))
    }
    figure(
        View.SIDE, pose(nearIn = false), pose(nearIn = true), listOf(Floor),
        startCaption = "Elbow to opposite knee",
        endCaption = "Switch sides",
        thumbnail = Which.START,
        guideLines = listOf(
            "Lift both feet until your thighs are upright, with your knees bent to about 90 degrees.",
            "Curl your upper body up and turn it so one elbow moves toward the opposite knee, while you straighten your other leg.",
            "Switch sides, bringing the other elbow toward the other knee.",
        ),
        contacts = listOf(Contact(BodyPoint.HIPS, Surface.FLOOR, BOTH)),
        checks = emptyList(),
    )
}

/** The arms along the floor at the sides, reaching towards the feet (2° down, so the hands rest on it). */
private fun floorArm(torso: Double) = armAt(-2.0, torso)

/** How far the hips roll up off the floor at the reverse crunch's top, cm (neutral: the hips plainly up, the upper back down). */
private const val ROLL_UP = 9.0

/**
 * Reverse crunch. Lying on the back, the arms along the floor. START the feet off the floor, the knees over the hips
 * (the thighs upright) and bent 90°; END the hips rolled up off the floor towards the ribs ([ROLL_UP], neutral): the
 * torso pivots on the shoulders, which stay down with the head, and the thighs tip towards the chest (35° past upright,
 * neutral), the knee bend the same 90°. ACE reverse crunch
 * (https://www.acefitness.org/resources/everyone/exercise-library/76/reverse-crunch/, Sources.kt).
 */
private val reverseCrunch: ExerciseFigure by lazy {
    val shoulder = shoulderOf(SUPINE_PELVIS, SUPINE)
    fun pose(rise: Double, thighWorld: Double): Pose {
        val torso = SUPINE + kotlin.math.asin(rise / Segment.TORSO.length) * DEG
        val pelvis = shoulder - unit(torso) * Segment.TORSO.length
        val leg = Leg(hip = fold180(thighWorld - torso - 180.0), knee = 90.0, ankle = -20.0)
        return Pose(pelvis.x, pelvis.y, torso, neck = fold180(torso - SUPINE), nearArm = floorArm(torso), nearLeg = leg)
    }
    figure(
        View.SIDE, pose(0.0, 90.0), pose(ROLL_UP, 125.0), listOf(Floor),
        startCaption = "Knees over your hips",
        endCaption = "Roll your hips up",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie on your back with your feet off the floor and your knees over your hips, bent to about 90 degrees.",
            "Breathe out and roll your hips up off the floor toward your ribs. Keep the same bend in your knees.",
        ),
        contacts = listOf(Contact(BodyPoint.HEAD, Surface.FLOOR, BOTH), Contact(BodyPoint.HIPS, Surface.FLOOR, setOf(Which.START))),
        checks = emptyList(),
    )
}

/**
 * Dead bug. Lying on the back, the lower back and hips on the floor in both panels. START the arms straight up (the
 * hands over the shoulders), the knees over the hips bent 90°; END the near arm lowered overhead and the far leg
 * straightened out, each until it nearly touches the floor (a light touch, 1–3 cm off it), the other arm and leg still
 * up: one arm and the opposite leg. ACE supine dead bug
 * (https://www.acefitness.org/resources/everyone/exercise-library/147/supine-dead-bug/, Sources.kt).
 */
private val deadBug: ExerciseFigure by lazy {
    val torso = SUPINE
    val up = armAt(90.0, torso)
    val table = Leg(hip = fold180(90.0 - torso - 180.0), knee = 90.0, ankle = -15.0)
    fun base(near: Arm = up, far: Leg = table) =
        Pose(SUPINE_PELVIS.x, SUPINE_PELVIS.y, torso, nearArm = near, nearLeg = table, farArm = up, farLeg = far)
    // The arm lowered overhead, level with the floor (the shoulder's full 180°, Body.kt): the shoulder joint is 10 cm up,
    // so the wrist bends back until the fingertips are 2 cm off the floor (a light touch).
    val wristDown = solveAngle(0.0, 60.0) { w ->
        base(near = Arm(180.0, 0.0, wrist = w)).skeleton().near.let { -maxOf(it.wrist.y, it.hand.y) - Body.HAND_WIDTH / 2 - 2.0 }
    }
    // The leg straightened out until the heel's lowest point is 2 cm off the floor.
    val legDown = solveAngle(-15.0, 40.0) { w ->
        base(far = Leg(hip = fold180(w - torso - 180.0), knee = 0.0, ankle = 0.0)).skeleton().far.let { -maxOf(it.heel.y, it.toe.y, it.ankle.y) - Body.FOOT_WIDTH / 2 - 2.0 }
    }
    figure(
        View.SIDE, base(), base(near = Arm(180.0, 0.0, wrist = wristDown), far = Leg(hip = fold180(legDown - torso - 180.0), knee = 0.0, ankle = 0.0)), listOf(Floor),
        startCaption = "Knees over your hips",
        endCaption = "Heel and opposite hand",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lift your arms and legs off the floor: knees over your hips and bent to 90 degrees, elbows over your shoulders.",
            "Lower one heel and the opposite hand toward the floor. Touch it lightly, without resting.",
        ),
        contacts = listOf(Contact(BodyPoint.HIPS, Surface.FLOOR, BOTH), Contact(BodyPoint.HEAD, Surface.FLOOR, BOTH)),
        checks = emptyList(),
    )
}

// ---- planks ----

/**
 * Front plank. On the forearms and toes, the head to the right: the elbows right under the shoulders (the upper arms
 * vertical), the forearms flat along the floor, palms down; the legs straight, the toes pulled towards the shins (the
 * feet square to the shins, the toe tips on the floor); one straight line from the heels to the head (the hips straight,
 * the head in line). START lying face down, about to lift (see START below); END the body lifted onto the forearms
 * and toes. ACE front plank (https://www.acefitness.org/resources/everyone/exercise-library/32/front-plank/, Sources.kt).
 */
private val frontPlank: ExerciseFigure by lazy {
    val elbowY = -Body.UPPER_ARM_WIDTH / 2
    val shoulderY = elbowY - Segment.UPPER_ARM.length
    val arm: (Double) -> Arm = { torso -> Arm(shoulder = fold180(270.0 - torso - 180.0), elbow = 90.0) }
    fun plank(theta: Double, shoulder: Point): Pose {
        val pelvis = shoulder - unit(theta) * Segment.TORSO.length
        return Pose(pelvis.x, pelvis.y, theta, nearArm = arm(theta), nearLeg = Leg(0.0, 0.0, ankle = 0.0))
    }
    val sh = Point(0.0, shoulderY)
    val theta = solveAngle(0.5, 30.0) { t -> plank(t, sh).skeleton().near.toe.y + Body.FOOT_WIDTH / 2 }
    val end = plank(theta, sh)
    // START lying face down, about to lift: the chest and hips on the floor, the torso level, the toes tucked (the
    // feet square to the shins, the toe tips on the floor), so the straight legs rise a little to the ankles; the
    // elbows on the floor under the shoulders, the upper arms pointing out to the sides towards the camera (seen from
    // the side the elbow is straight below the shoulder, the upper arm foreshortened), the forearms forward along the
    // floor.
    val lowPelvis = Point(sh.x - Segment.TORSO.length, -Body.TORSO_RADIUS)
    val flatOut = acos(((elbowY + Body.TORSO_RADIUS) / Segment.UPPER_ARM.length).coerceIn(-1.0, 1.0)) * DEG
    val lowArm = Arm(shoulder = fold180(270.0 - 0.0 - 180.0), elbow = 90.0, out = flatOut)
    fun lying(legUp: Double) = Pose(lowPelvis.x, lowPelvis.y, 0.0, nearArm = lowArm, nearLeg = Leg(hip = -legUp, knee = 0.0, ankle = 0.0))
    val legUp = solveAngle(0.0, 19.9) { u -> lying(u).skeleton().near.toe.y + Body.FOOT_WIDTH / 2 }
    val low = lying(legUp)
    figure(
        View.SIDE, low, end, listOf(Floor),
        startCaption = "Lie face down",
        endCaption = "Body in a straight line",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie face down with your elbows close to your sides and right under your shoulders, palms down.",
            "Lift your body off the floor onto your forearms and toes, keeping your trunk and legs stiff.",
            "Hold your body in a straight line.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH), Contact(BodyPoint.ELBOWS, Surface.FLOOR, BOTH)),
        checks = emptyList(),
    )
}

/**
 * Side plank. Seen from the front of the body (the camera's note above), lying on the side with the head to the left,
 * propped on the bottom (far) forearm: its elbow on the floor right under the bottom shoulder (the upper arm vertical),
 * the forearm on the floor pointing out of the picture towards the camera, so it shows as a short stub; the top arm
 * along the side, the hand on the hip; the legs straight, the feet stacked (the top leg resting on the bottom one),
 * the side of the bottom foot on the floor, the head in line with the spine. START the hips down on the floor; END the
 * hips and knees lifted until the body is one straight line from the head to the heels. ACE side plank
 * (https://www.acefitness.org/resources/everyone/exercise-library/303/side-plank/, Sources.kt).
 */
private val sidePlank: ExerciseFigure by lazy {
    val view = View.FRONT
    val topArm = Arm(shoulder = 0.0, elbow = 0.0)
    val elbowY = -Body.UPPER_ARM_WIDTH / 2
    /**
     * The bottom arm: the upper arm drawn straight down from the bottom shoulder to the elbow on the floor, pointing
     * [out]° out of the picture towards the camera when the shoulder is lower than the arm's length; the forearm on the
     * floor pointing towards the camera (80° out: a short stub), the elbow bent 90°.
     */
    fun bottomArm(torso: Double, out: Double) = Arm(shoulder = fold180(torso - 180.0 - 270.0), elbow = 90.0, out = out, forearmOut = 80.0)
    fun pose(pelvis: Point, torso: Double, bottomHip: Double, topHip: Double, out: Double) =
        Pose(pelvis.x, pelvis.y, torso, nearArm = topArm, nearLeg = Leg(topHip, 0.0), farArm = bottomArm(torso, out), farLeg = Leg(bottomHip, 0.0))
    fun lowest(l: LimbPoints) = maxOf(l.ankle.y + Body.SHIN_WIDTH / 2, l.heel.y + Body.FOOT_WIDTH / 2, l.toe.y + Body.FOOT_WIDTH / 2)
    /** The bottom shoulder of a torso at [torso]° from [pelvis]. */
    fun bottomShoulder(pelvis: Point, torso: Double) = shoulderOf(pelvis, torso) - unit(torso - 90.0) * Body.SHOULDER_HALF_WIDTH
    /** The upper arm's angle out of the picture that puts the elbow on the floor straight below the bottom shoulder. */
    fun outFor(pelvis: Point, torso: Double) = acos(((elbowY - bottomShoulder(pelvis, torso).y) / Segment.UPPER_ARM.length).coerceIn(-1.0, 1.0)) * DEG
    // The top leg stacked on the bottom one: its ankle a shin's width above the bottom one.
    fun stacked(pelvis: Point, torso: Double, bottomHip: Double): Pose {
        val out = outFor(pelvis, torso)
        val top = solveAngle(-20.0, 40.0) { h -> pose(pelvis, torso, bottomHip, h, out).skeleton(view).let { (it.far.ankle.y - it.near.ankle.y) - Body.SHIN_WIDTH } }
        val p = pose(pelvis, torso, bottomHip, top, out)
        // The bottom elbow at x = 0 in both panels.
        val dx = -bottomShoulder(pelvis, torso).x
        return p.copy(pelvisX = p.pelvisX + dx)
    }
    // END: the elbow straight under the bottom shoulder (the upper arm in the picture), the legs straight on from the
    // body (the bottom hip 0), the torso tilted until the bottom foot is on the floor.
    fun lineAt(t: Double): Point {
        val sh = Point(0.0, elbowY - Segment.UPPER_ARM.length) + unit(t - 90.0) * Body.SHOULDER_HALF_WIDTH
        return sh - unit(t) * Segment.TORSO.length
    }
    val lineTorso = solveAngle(150.0, 179.0) { t -> lowest(pose(lineAt(t), t, 0.0, 0.0, 0.0).skeleton(view).far) }
    val end = stacked(lineAt(lineTorso), lineTorso, 0.0)
    // START: the hips down on the floor (the torso outline's side at the hips on it), the bottom leg turned up towards the
    // top side by 18° (inside the 20° adduction limit, Body.kt) to lie along the floor, the torso tilted until it does;
    // the bottom shoulder is then lower than the upper arm's length, so the upper arm points out of the picture.
    val hipsDown = 18.0
    fun hipsOnFloor(t: Double): Point {
        val bottomOfHips = unit(t) * -5.0 - unit(t - 90.0) * 16.0
        return Point(0.0, -bottomOfHips.y)
    }
    val bottomSign = listOf(-1.0, 1.0).first { sg ->
        val f = { t: Double -> lowest(pose(hipsOnFloor(t), t, sg * hipsDown, 0.0, 0.0).skeleton(view).far) }
        (f(150.0) > 0) != (f(179.0) > 0)
    }
    val downTorso = solveAngle(150.0, 179.0) { t -> lowest(pose(hipsOnFloor(t), t, bottomSign * hipsDown, 0.0, 0.0).skeleton(view).far) }
    val start = stacked(hipsOnFloor(downTorso), downTorso, bottomSign * hipsDown)
    figure(
        view, start, end, listOf(Floor),
        startCaption = "Prop up on your forearm",
        endCaption = "Straight, head to heels",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie on your side with your legs straight and your feet stacked.",
            "Prop yourself up on your forearm, with your elbow right under your shoulder.",
            "Lift your hips and knees off the floor, keeping the side of your bottom foot on the floor.",
            "Hold your body in a straight line from head to heels.",
        ),
        contacts = listOf(Contact(BodyPoint.FAR_FOOT, Surface.FLOOR, BOTH)),
        checks = emptyList(),
    )
}

// ---- kneeling: the quadruped, the rollouts and the cable crunch ----

/** A knee resting on the floor (the thigh's round touching it), at [x]. */
private fun kneeOnFloor(x: Double) = Point(x, -(Body.THIGH_WIDTH / 2 - 0.25))

/** The kneeling leg for a thigh pointing from the hip to the knee at [thighWorld]°: the lower leg back along the floor, 8° up (the quadruped's). */
private fun kneel(thighWorld: Double, torso: Double) =
    Leg(hip = fold180(thighWorld - torso - 180.0), knee = fold180(thighWorld - 172.0), ankle = -50.0)

/**
 * Bird dog. On the hands and knees, the head to the right: the Quadruped hip extension's base (HamstringsGlutes.kt):
 * the knees under the hips, the hands under the shoulders, the elbows softened so the back slopes at most 15°. START
 * all fours; END the near arm and the far leg raised to level with the floor (the guide's "toward level"; the back
 * itself slopes up to the head, so the hip is extended by that slope, about 15°, inside its normal range), the knee
 * and elbow straight.
 * ACE bird dog (https://www.acefitness.org/resources/everyone/exercise-library/14/bird-dog/, Sources.kt).
 */
private val birdDog: ExerciseFigure by lazy {
    val kneeP = kneeOnFloor(0.0)
    val hipP = kneeP + Point(0.0, -Segment.THIGH.length)
    fun upperFor(elbow: Double): Double = solveAngle(240.0, 270.0) { u ->
        Segment.UPPER_ARM.length * cos(u / DEG) + Segment.FOREARM.length * cos((u + elbow) / DEG)
    }
    fun handsDown(torso: Double, elbow: Double, upper: Double): Pose {
        val arm = Arm(shoulder = fold180(upper - torso - 180.0), elbow = elbow, wrist = PUSH_UP_WRIST)
        return Pose(hipP.x, hipP.y, torso, nearArm = arm, nearLeg = kneel(270.0, torso))
    }
    fun tilt(elbow: Double): Double {
        val upper = upperFor(elbow)
        return solveAngle(0.0, 40.0) { t ->
            val n = handsDown(t, elbow, upper).skeleton().near
            -(maxOf(n.wrist.y, n.hand.y) + Body.HAND_WIDTH / 2)
        }
    }
    val elbow = (0..45).map { it.toDouble() }.first { tilt(it) <= 15.0 }
    val start = handsDown(tilt(elbow), elbow, upperFor(elbow))
    val t = start.torso
    // Level with the floor: the arm straight forward (world 0°), the leg straight back (world 180°, the hip extended by
    // the back's slope, inside its 20° limit, Body.kt).
    val end = start.copy(nearArm = armAt(0.0, t), farArm = start.nearArm, farLeg = Leg(hip = fold180(180.0 - t - 180.0), knee = 0.0, ankle = -30.0))
    figure(
        View.SIDE, start, end, listOf(Floor),
        startCaption = "Hands and knees",
        endCaption = "Opposite arm and leg",
        thumbnail = Which.END,
        guideLines = listOf(
            "Get on your hands and knees, knees and feet hip-width apart.",
            "Raise one leg and the opposite arm toward level with the floor. Raise them only as high as you can keep your lower back still.",
        ),
        contacts = listOf(
            Contact(BodyPoint.NEAR_KNEE, Surface.FLOOR, BOTH), Contact(BodyPoint.FAR_KNEE, Surface.FLOOR, setOf(Which.START)),
            Contact(BodyPoint.HANDS, Surface.FLOOR, setOf(Which.START)), Contact(BodyPoint.FAR_HAND, Surface.FLOOR, setOf(Which.END)),
        ),
        checks = emptyList(),
    )
}

/**
 * The ab wheel's reach: the body line from the knees 20° above the floor (neutral: no allowed source gives an angle;
 * the guide stops before the hips sag or the back arches, so the figure stops short of the floor with the hips straight).
 */
private const val WHEEL_REACH = 20.0

/**
 * The wrists on the wheel's handles, flexed so the hands lie forward over the hub instead of reaching past it into the
 * floor (the drawn hand cannot curl round the handle): 60° at the start (the wrist's limit, Body.kt), 40° at the reach
 * (neutral).
 */
private const val WHEEL_WRIST = 60.0
private const val WHEEL_WRIST_OUT = 40.0

/**
 * Ab wheel rollout. Kneeling, the head to the right, the hands on the wheel's handles. START the wheel on the floor 30
 * cm in front of the knees (neutral "near the front of your knees"), the arms straight (the elbows about 8°: the drawn
 * hand's offset of the grip needs that much), the shoulders over the wheel and the hips sat back behind the knees; END rolled out: the body one straight
 * line from the knees to the head ([WHEEL_REACH]° up from the floor, the hips straight, no sag), the arms nearly
 * straight (elbows 5°, neutral) reaching forward to the wheel on the floor. The return is to the start, bending at the hips. ExRx kneeling wheel rollout (https://exrx.net/WeightExercises/HipFlexors/BWKneelingWheelRollout,
 * Sources.kt).
 */
private val abWheelRollout: ExerciseFigure by lazy {
    val kneeP = kneeOnFloor(0.0)
    val hipP = kneeP + Point(0.0, -Segment.THIGH.length)
    val wheel0 = Point(kneeP.x + 30.0, -AbWheel.RADIUS)
    // The hands wrap the handles, so the hand turns forward over the hub (the wrist bent [WHEEL_WRIST]°) and does not
    // reach past the wheel into the floor.
    val g = Segment.HAND.length * Body.GRIP
    fun armReach(w: Double) = Segment.UPPER_ARM.length + kotlin.math.hypot(Segment.FOREARM.length + g * cos(w / DEG), g * sin(w / DEG))
    // The arms straight: the elbows 5° (the wrist's offset of the grip needs a little bend to reach it the natural way).
    fun bentReach(w: Double, bend: Double): Double {
        val link = armReach(w) - Segment.UPPER_ARM.length
        return sqrt(Segment.UPPER_ARM.length * Segment.UPPER_ARM.length + link * link + 2 * Segment.UPPER_ARM.length * link * cos(bend / DEG))
    }
    // START: the shoulders over the wheel (the straight arm 5° back from vertical, neutral), the hips sat back behind
    // the knees as far as that needs: the thigh and torso solved as two links from the knee to the shoulder.
    val sh0 = wheel0 + unit(95.0) * bentReach(WHEEL_WRIST, 22.0)
    val d = sh0 - kneeP
    val alpha = kotlin.math.acos(
        ((Segment.THIGH.length * Segment.THIGH.length + d.length() * d.length() - Segment.TORSO.length * Segment.TORSO.length) /
            (2 * Segment.THIGH.length * d.length())).coerceIn(-1.0, 1.0),
    ) * DEG
    val hip0 = kneeP + unit(angleTo(d) + alpha) * Segment.THIGH.length
    val t0 = angleTo(sh0 - hip0)
    val start = Pose(hip0.x, hip0.y, t0, nearArm = Reach.arm(sh0, wheel0, t0, wrist = WHEEL_WRIST), nearLeg = kneel(angleTo(kneeP - hip0), t0))
    val hip1 = kneeP + unit(WHEEL_REACH) * Segment.THIGH.length
    val sh1 = shoulderOf(hip1, WHEEL_REACH)
    val bend = 5.0
    val reach = bentReach(WHEEL_WRIST_OUT, bend + 8.0)
    val wheel1 = Point(sh1.x + sqrt(reach * reach - (wheel0.y - sh1.y) * (wheel0.y - sh1.y)), wheel0.y)
    val end = Pose(hip1.x, hip1.y, WHEEL_REACH, nearArm = Reach.arm(sh1, wheel1, WHEEL_REACH, wrist = WHEEL_WRIST_OUT), nearLeg = kneel(WHEEL_REACH + 180.0, WHEEL_REACH))
    figure(
        View.SIDE, start, end, listOf(Floor, AbWheel),
        startCaption = "Wheel near your knees",
        endCaption = "Stop before hips sag",
        thumbnail = Which.END,
        guideLines = listOf(
            "Kneel on the floor and hold the wheel's handles.",
            "Put the wheel on the floor near the front of your knees, arms straight.",
            "Roll the wheel forward, lowering your body toward the floor.",
            "Stop before your hips start to sag or your lower back starts to arch.",
        ),
        contacts = listOf(Contact(BodyPoint.KNEES, Surface.FLOOR, BOTH)),
        checks = emptyList(),
    )
}

/**
 * Stability ball rollout. Kneeling in a kneeling plank facing a 65 cm ball (KitHamstrings.kt's), the head to the right:
 * the body one straight line from the knees to the head in both panels ("hips in line with your body"). START the
 * forearms on the ball, the shoulders right above the elbows (the upper arms vertical), the body 50° up from the floor
 * (neutral); END the ball pushed forward by straightening the arms, the body lowered to 30° (neutral: stopped before
 * the hips sag), the forearms still on the ball. ACE core-strengthening stability ball workout, kneeling plank roll-out
 * (https://www.acefitness.org/resources/pros/expert-articles/5123/core-strengthening-stability-ball-workout/, Sources.kt).
 */
private val stabilityBallRollout: ExerciseFigure by lazy {
    val kneeP = kneeOnFloor(0.0)
    val r = StabilityBall.DIAMETER / 2
    val onBall = r + Body.FOREARM_WIDTH / 2
    fun body(beta: Double) = (kneeP + unit(beta) * Segment.THIGH.length).let { hip -> hip to shoulderOf(hip, beta) }
    // The ball's centre under a forearm from [elbow] at [fore]° with the contact [along] cm down it.
    fun ballUnder(elbow: Point, fore: Double, along: Double) = elbow + unit(fore) * along + unit(fore - 90.0) * onBall
    // START: the upper arm vertical; the forearm's angle solved so the ball sits on the floor, touching the forearm
    // 15 cm from the elbow.
    val (hip0, sh0) = body(50.0)
    val elbow0 = sh0 + Point(0.0, Segment.UPPER_ARM.length)
    val fore0 = solveAngle(0.0, 80.0) { f -> ballUnder(elbow0, f, 15.0).y + r }
    val ball0 = ballUnder(elbow0, fore0, 15.0)
    val start = Pose(
        hip0.x, hip0.y, 50.0, nearArm = Arm(shoulder = fold180(270.0 - 50.0 - 180.0), elbow = fold180(fore0 - 270.0)),
        nearLeg = kneel(50.0 + 180.0, 50.0),
    )
    // END: the arm straight from the shoulder; its angle solved so the ball under the forearm (8 cm from the wrist) is on
    // the floor.
    val (hip1, sh1) = body(30.0)
    fun ballFor(a: Double): Point {
        val elbow = sh1 + unit(a) * Segment.UPPER_ARM.length
        return ballUnder(elbow, a, Segment.FOREARM.length - 8.0)
    }
    val a1 = solveAngle(-60.0, 20.0) { a -> ballFor(a).y + r }
    val ball1 = ballFor(a1)
    val end = Pose(hip1.x, hip1.y, 30.0, nearArm = armAt(a1, 30.0), nearLeg = kneel(30.0 + 180.0, 30.0))
    figure(
        View.SIDE, start, end, listOf(Floor, StabilityBall(ball0.x)),
        startCaption = "Shoulders above elbows",
        endCaption = "Stop before hips sag",
        thumbnail = Which.END,
        guideLines = listOf(
            "Kneel facing a stability ball.",
            "Rest your forearms on the ball, with your shoulders right above your elbows.",
            "Slowly push the ball forward by straightening your arms.",
            "Stop before your hips start to sag.",
        ),
        contacts = listOf(Contact(BodyPoint.KNEES, Surface.FLOOR, BOTH)),
        checks = emptyList(),
        endProps = listOf(Floor, StabilityBall(ball1.x)),
    )
}

/** The hands holding the rope beside the head: by the side of the head, at the temple, a little in front. */
private fun ropeGrip(s: Skeleton, torso: Double, neck: Double): Point = s.head + unit(torso - neck - 90.0) * 6.0 - unit(torso - neck) * 4.0

/**
 * Cable crunch. Kneeling below a high pulley, facing the column (the head to the right), the rope in both hands
 * beside the head. The knees stay under the hips (the thighs upright) and the hips still in both panels. START the
 * trunk upright, leaning 15° forward (neutral); END curled down, the elbows coming towards the thighs: the torso
 * [CABLE_CRUNCH_END]° above level (neutral), the chin tucked (the neck 20°, neutral). ExRx cable kneeling crunch
 * (https://exrx.net/WeightExercises/RectusAbdominis/CBKneelingCrunch, Sources.kt). The cable runs from the pulley to the
 * rope in both panels.
 */
private const val CABLE_CRUNCH_END = 25.0

private val cableCrunch: ExerciseFigure by lazy {
    val kneeP = kneeOnFloor(0.0)
    val hipP = kneeP + Point(0.0, -Segment.THIGH.length)
    fun pose(torso: Double, neck: Double): Pose {
        val bare = Pose(hipP.x, hipP.y, torso, neck = neck, nearArm = HANG, nearLeg = kneel(270.0, torso))
        val s = bare.skeleton()
        return bare.copy(nearArm = armTo(s.shoulder, ropeGrip(s, torso, neck), torso)).let { it.copy(farArm = it.nearArm) }
    }
    val start = pose(75.0, 5.0)
    val end = pose(CABLE_CRUNCH_END, 20.0)
    val stack = CableStack(x = kneeP.x + 75.0, pulleyHeight = 190.0, reach = -45.0)
    figure(
        View.SIDE, start, end, listOf(Floor, stack, CableAttachment(Attachment.ROPE)),
        startCaption = "Rope by your head",
        endCaption = "Elbows toward thighs",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set a rope on a high pulley.",
            "Kneel below the pulley and hold the rope with both hands.",
            "Keep your hips still and curl your trunk down, bringing your elbows toward your thighs.",
        ),
        contacts = listOf(Contact(BodyPoint.KNEES, Surface.FLOOR, BOTH)),
        checks = emptyList(),
    )
}

/**
 * Machine crunch. Seated on the crunch machine, the back of the hips against the hip pad, the feet flat on the floor in
 * front (the knees about 90°), the chest pad on the upper chest, the hands on the outside of the pad. START sitting
 * upright; END the trunk curled forward and down, the torso 45° forward (to 45° above level, neutral), the chin tucked,
 * the hips and thighs still; the pad follows the chest on its lever. ExRx lever seated crunch (chest pad)
 * (https://exrx.net/WeightExercises/RectusAbdominis/LVSeatedCrunchChestPad, Sources.kt).
 */
private val machineCrunch: ExerciseFigure by lazy {
    val seatH = 45.0
    val pelvis = Point(0.0, -(seatH + Body.TORSO_RADIUS))
    val leg = { torso: Double -> Reach.leg(pelvis, Point(pelvis.x + Segment.THIGH.length, -Body.ANKLE_HEIGHT), torso, foot = 0.0) }
    fun pad(s: Skeleton) = s.upperChest() + unit(s.torso - 90.0) * CrunchMachine.PAD_RADIUS
    fun pose(torso: Double, neck: Double): Pose {
        val bare = Pose(pelvis.x, pelvis.y, torso, neck = neck, nearArm = HANG, nearLeg = leg(torso))
        val s = bare.skeleton()
        val grip = pad(s) + unit(s.torso - 90.0) * (CrunchMachine.PAD_RADIUS - 2.0) + unit(s.torso) * 3.0
        return bare.copy(nearArm = armTo(s.shoulder, grip, torso)).let { it.copy(farArm = it.nearArm) }
    }
    val start = pose(90.0, 0.0)
    val end = pose(45.0, 15.0)
    val hipPadX = pelvis.x - Body.TORSO_RADIUS
    val pivot = leverPivot(pad(start.skeleton()), pad(end.skeleton()), pelvis.x - 30.0)
    fun machine(p: Pose) = CrunchMachine(
        seatFront = pelvis.x + 25.0, seatHeight = seatH, hipPadX = hipPadX,
        feet = Point(pelvis.x + Segment.THIGH.length - 12.0, -CrunchMachine.ROLLER_RADIUS - 1.0), pivot = pivot, pad = pad(p.skeleton()),
    )
    figure(
        View.SIDE, start, end, listOf(Floor, machine(start)),
        startCaption = "Chest level with pad",
        endCaption = "Curl into a C shape",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set the seat so your chest is level with the pad.",
            "Put your hands on the outside of the pad.",
            "Keep your hips still and curl your trunk forward and down into a C shape.",
        ),
        contacts = listOf(Contact(BodyPoint.HIPS, Surface.SEAT, BOTH), Contact(BodyPoint.HIPS, Surface.HIP_PAD, BOTH), Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = emptyList(),
        endProps = listOf(Floor, machine(end)),
    )
}

/** The point on the perpendicular bisector of [a] and [b] at x = [x]: a pivot whose lever is the same length to both. */
private fun leverPivot(a: Point, b: Point, x: Double): Point {
    val m = (a + b) * 0.5
    val d = b - a
    return if (abs(d.y) < 1e-9) Point(x, m.y - 40.0) else Point(x, m.y - (x - m.x) * d.x / d.y)
}

// ---- standing: the cable drills and the carries ----

/** Standing tall on both feet, hip-width (side view: the feet side by side), knees [knee]° bent. */
private fun stand(knee: Double = 0.0): StandingLeg = standingLeg(0.0, shinLean = knee * 0.5, knee = knee, torso = 90.0)

/**
 * Pallof press. Side on to the camera, standing tall, the feet hip-width, the knees slightly bent (10°, neutral), the
 * handle in both hands, the column beside the lifter (beyond the body: drawn behind it) with its pulley level with the
 * chest. START the handle at the chest, the elbows tucked in by the sides; END pressed straight out in front of the
 * chest, the arms straight, level with the chest; the chest and hips square (the torso upright and unturned in both).
 * The camera: see the file's header. ACE standing anti-rotation press
 * (https://www.acefitness.org/resources/everyone/exercise-library/332/standing-anti-rotation-press/, Sources.kt).
 */
private val pallofPress: ExerciseFigure by lazy {
    val leg = stand(10.0)
    val bare = standOn(leg, 90.0, HANG).skeleton()
    val chest = bare.chest()
    val atChest = chest + Point(Body.HAND_WIDTH, 0.0)
    val sh = bare.shoulder
    val out = Point(sh.x + sqrt(ARM_REACH * ARM_REACH - (chest.y - sh.y) * (chest.y - sh.y)) - 1e-6, chest.y)
    val start = standOn(leg, 90.0, armTo(sh, atChest, 90.0))
    val end = standOn(leg, 90.0, straightArm(sh, out, 90.0))
    val stack = CableStack(x = atChest.x, pulleyHeight = -chest.y, reach = 0.0, beyond = true)
    figure(
        View.SIDE, start, end, listOf(Floor, stack, CableHandle(Side.NEAR)),
        startCaption = "Handle at your chest",
        endCaption = "Press straight out",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set a single handle on a cable at about chest height.",
            "Stand side-on to the machine, feet hip-width apart and knees slightly bent.",
            "Hold the handle with both hands at your chest, fingers laced and elbows tucked in by your sides.",
            "Press the handle straight out in front of your chest. The cable should run level with your chest.",
        ),
        contacts = FEET_DOWN,
        checks = emptyList(),
    )
}

/**
 * Cable woodchop. The front view (the header), the column on the lifter's near side (the right of the picture), the
 * pulley at shoulder height; the feet wider than the shoulders (each leg 12° out, neutral), the knees straight, the
 * torso upright. The hands laced on one handle, out in front of the body. The figure's shoulders cannot turn towards
 * the machine (the turn is out of the picture), and two straight arms from square shoulders can only meet on the
 * body's middle line; so the handle is drawn 6 cm to the near side of the middle at the top and 6 cm to the far side at
 * the bottom, the trade between the diagonal showing and the arms staying nearly straight: the arm on the handle's
 * side bends up to about 35°, the other is straight (neutral). START the handle high on the near side, 42 cm above
 * the shoulders, 6 cm towards the near side (neutral); END swept down across the body to the far hip's height, 6 cm to
 * the far side. The cable
 * runs from the pulley to the handle in both panels. ACE standing rotational chop
 * (https://www.acefitness.org/resources/everyone/exercise-library/349/standing-rotational-chop/, Sources.kt).
 */
private val cableWoodchop: ExerciseFigure by lazy {
    val view = View.FRONT
    val apart = 12.0
    val legLen = Segment.THIGH.length + Segment.SHIN.length
    val pelvis = Point(0.0, -(Body.ANKLE_HEIGHT + legLen * cos(apart / DEG)))
    val legs = Leg(hip = apart, knee = 0.0)
    val bare = Pose(pelvis.x, pelvis.y, 90.0, nearArm = HANG, nearLeg = legs).skeleton(view)
    val nearSh = bare.near.shoulder
    val farSh = bare.far.shoulder
    // The hands at [p] in the picture, as far in front of the body as the longer arm needs to be nearly straight.
    fun armsTo(p: Point): Pair<Arm, Arm> {
        val far = maxOf((p - farSh).length(), (p - nearSh).length())
        val z = sqrt(maxOf(ARM_REACH * ARM_REACH * 0.997 - far * far, 0.0))
        val g = v3(p, z)
        val hint = V3(0.0, 0.0, -1.0)
        val near = arm3(v3(nearSh, 0.0), g, hint, 90.0)
        val axis = pelvis.x
        // The far arm is drawn as the near one's mirror image: its shoulder and the hands' point mirrored.
        val farArm = arm3(v3(farSh, 0.0).mirror(axis), g.mirror(axis), hint, 90.0)
        return near to farArm
    }
    val high = Point(pelvis.x + 6.0, nearSh.y - 42.0)
    val low = Point(pelvis.x - 6.0, bare.far.hip.y)
    fun pose(p: Point) = armsTo(p).let { (n, f) -> Pose(pelvis.x, pelvis.y, 90.0, nearArm = n, nearLeg = legs, farArm = f) }
    val start = pose(high)
    val end = pose(low)
    val stack = CableStack(x = nearSh.x + 75.0, pulleyHeight = -nearSh.y, reach = -12.0, cableInFront = true)
    figure(
        view, start, end, listOf(Floor, stack, CableHandle(Side.NEAR)),
        startCaption = "High on the near side",
        endCaption = "Down toward far hip",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set a single handle on a cable at shoulder height.",
            "Stand side-on to the machine, feet wider than shoulder-width.",
            "Hold the handle with both hands, fingers laced and arms straight.",
            "At the same time, sweep the handle from above your near shoulder down toward your far hip.",
        ),
        contacts = FEET_DOWN,
        checks = emptyList(),
    )
}

// ---- carries ----

/** The mid-stride legs: the front (near) foot landing on its heel, the toes up, the back (far) foot pushing off its ball. */
private class Stride(val pelvis: Point, val front: Leg, val back: Leg)

/**
 * A slow walking stride (neutral: the front knee 5° and the back knee 15° bent, the front foot 15° toes-up on its heel,
 * the back foot 30° down on its ball, the pelvis over the middle), the torso upright.
 */
private val stride: Stride by lazy {
    val r = Body.FOOT_WIDTH / 2
    fun heelLow(ankle: Point, foot: Double): Double {
        val fd = unit(foot)
        val sole = unit(foot - 90.0) * (Body.ANKLE_HEIGHT - r)
        val heel = ankle + sole - fd * (Segment.FOOT.length * 0.2 - r)
        val toe = ankle + sole + fd * (Segment.FOOT.length * 0.8 - r)
        return maxOf(heel.y, toe.y) + r
    }
    val frontFoot = 15.0
    val backFoot = -30.0
    fun ankleAt(x: Double, foot: Double): Point = Point(x, solveAngle(-60.0, 0.0) { y -> heelLow(Point(x, y), foot) })
    fun reachFor(knee: Double) = sqrt(
        Segment.THIGH.length * Segment.THIGH.length + Segment.SHIN.length * Segment.SHIN.length +
            2 * Segment.THIGH.length * Segment.SHIN.length * cos(knee / DEG),
    ) - 1e-6
    val front = ankleAt(22.0, frontFoot)
    val px = 3.0
    // The pelvis at the height where the front knee bends 5°; the back ankle as far behind as a 15° back knee reaches.
    val pelvis = Point(px, front.y - sqrt(reachFor(5.0) * reachFor(5.0) - (front.x - px) * (front.x - px)))
    val backX = solveAngle(-80.0, -1.0) { x -> (ankleAt(x, backFoot) - pelvis).length() - reachFor(15.0) }
    val back = ankleAt(backX, backFoot)
    Stride(pelvis, Reach.leg(pelvis, front, 90.0, frontFoot), Reach.leg(pelvis, back, 90.0, backFoot))
}

private fun striding(near: Arm, far: Arm = near) =
    Pose(stride.pelvis.x, stride.pelvis.y, 90.0, nearArm = near, nearLeg = stride.front, farArm = far, farLeg = stride.back)

private val tall by lazy { stand() }

private val STRIDE_CONTACTS = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH))

/**
 * Farmer's walk. A heavy dumbbell in each hand, the palms in (handles front to back: seen in profile from the side,
 * lesson 2), the arms straight at the sides. START standing tall after the pick-up; END mid-stride ([stride]), the
 * back upright and the head up, the weights still at the sides. ACE farmer's carry
 * (https://www.acefitness.org/resources/everyone/exercise-library/359/farmer-s-carry/, Sources.kt).
 */
private val farmersWalk: ExerciseFigure by lazy {
    val props = listOf(Floor, Dumbbell(Side.FAR, neutral = true, squareToForearm = true), Dumbbell(Side.NEAR, neutral = true, squareToForearm = true))
    figure(
        View.SIDE, standOn(tall, 90.0, HANG), striding(HANG), props,
        startCaption = "Arms straight at sides",
        endCaption = "Back upright, head up",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand tall with your arms straight at your sides.",
            "Walk a set distance, keeping your back upright and your head up.",
            WALK_SLOW,
        ),
        contacts = STRIDE_CONTACTS,
        checks = emptyList(),
    )
}

/**
 * Suitcase carry. The front view (the header): one dumbbell in the near hand only, held at the side with the palm in,
 * so the front view sees it end on (a disc); the arm hanging straight, out from the side just enough that the weight
 * clears the thigh ("without the weight touching your body"); the torso upright and the shoulders level, no lean either
 * way. START standing tall; END mid-stride: the far foot swinging through, off the floor (the thigh 25° forward towards
 * the camera, the knee bent 40°: neutral), the body still upright over the standing leg. ACE suitcase carry
 * (https://www.acefitness.org/resources/everyone/exercise-library/358/suitcase-carry/, Sources.kt).
 */
private val suitcaseCarry: ExerciseFigure by lazy {
    val view = View.FRONT
    val pelvis = Point(0.0, -(Body.ANKLE_HEIGHT + Segment.THIGH.length + Segment.SHIN.length))
    val stance = Leg(hip = 0.0, knee = 0.0)
    // The near arm out from the side until the disc's edge is 1.5 cm clear of the thigh's outside.
    fun posed(far: Leg, arm: Arm) = Pose(pelvis.x, pelvis.y, 90.0, nearArm = arm, nearLeg = stance, farArm = Arm(4.0, 0.0), farLeg = far)
    val abduct = solveAngle(0.0, 30.0) { a ->
        val s = posed(stance, Arm(a, 0.0)).skeleton(view)
        (s.near.grip.x - Dumbbell.HEAD_RADIUS) - (s.near.hip.x + Body.THIGH_WIDTH / 2) - 1.5
    }
    val arm = Arm(abduct, 0.0)
    val swing = Leg(hip = 0.0, knee = 40.0, out = 25.0, shinOut = -15.0)
    val props = listOf(Floor, Dumbbell(Side.NEAR, endOnFront = true))
    figure(
        view, posed(stance, arm), posed(swing, arm), props,
        startCaption = "Arm hangs straight",
        endCaption = "Stay upright",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lift it in one hand with an overhand grip, keeping your back straight.",
            "Let the arm hang straight, without the weight touching your body.",
            "Walk a set distance, keeping your torso upright.",
            WALK_SLOW,
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, setOf(Which.START)), Contact(BodyPoint.NEAR_FOOT, Surface.FLOOR, BOTH)),
        checks = emptyList(),
    )
}

/**
 * Waiter's carry. The kettlebell in the near hand, the far arm hanging. START held at the front of the shoulder (the
 * Kettlebell press's rack, Shoulders.kt: the upper arm 15° forward, the elbow bent 145°), standing tall; END pressed
 * overhead, the arm straight and vertical over the shoulder, the palm forward (the bell behind the forearm), the trunk,
 * arm and kettlebell in one vertical line, mid-stride ([stride]). NSCA core training concepts
 * (https://www.nsca.com/contentassets/1066cf7e2c9d4e61b3c1a6e3fea988e0/coach-7.1.2-implementing-core-training-concepts-into-strength-training-for-sport.pdf,
 * Sources.kt).
 */
private val waitersCarry: ExerciseFigure by lazy {
    val rack = Arm(shoulder = 15.0, elbow = 145.0)
    figure(
        View.SIDE, standOn(tall, 90.0, rack, HANG), striding(Arm(180.0, 0.0), HANG), listOf(Floor, Kettlebell(Side.NEAR, KettlebellHold.RACK)),
        startCaption = "Kettlebell at shoulder",
        endCaption = "Arm and bell in one line",
        thumbnail = Which.END,
        guideLines = listOf(
            "Hold a kettlebell in one hand at the front of your shoulder.",
            "Press it overhead until your arm is straight, palm facing forward.",
            "Walk a set distance, keeping your trunk, arm and the kettlebell in one vertical line.",
        ),
        contacts = STRIDE_CONTACTS,
        checks = listOf(LockoutOverShoulders(Which.END)),
        endProps = listOf(Floor, Kettlebell(Side.NEAR, KettlebellHold.OVERHEAD)),
    )
}

/**
 * Plate pinch carry. A weight plate pinched by its rim in each hand (its face along the body's side: seen face on from
 * the side), the arms straight at the sides. START standing tall; END mid-stride ([stride]), standing tall. ACE grip
 * strength (https://www.acefitness.org/resources/pros/expert-articles/5765/how-to-improve-grip-strength/, Sources.kt).
 */
private val platePinchCarry: ExerciseFigure by lazy {
    val props = listOf(Floor, PinchPlate(Side.FAR), PinchPlate(Side.NEAR))
    figure(
        View.SIDE, standOn(tall, 90.0, HANG), striding(HANG), props,
        startCaption = "Pinch plates by the edge",
        endCaption = "Walk, standing tall",
        thumbnail = Which.END,
        guideLines = listOf(
            "Pinch a weight plate by its edge in each hand, thumb on one side and fingers on the other.",
            "Walk a set distance, standing tall.",
            WALK_SLOW,
        ),
        contacts = STRIDE_CONTACTS,
        checks = emptyList(),
    )
}

internal val coreFigures: Map<String, Lazy<ExerciseFigure>> = mapOf(
    "Front plank" to lazy { frontPlank },
    "Side plank" to lazy { sidePlank },
    "Farmer's walk" to lazy { farmersWalk },
    "Suitcase carry" to lazy { suitcaseCarry },
    "Waiter's carry" to lazy { waitersCarry },
    "Plate pinch carry" to lazy { platePinchCarry },
    "Pallof press" to lazy { pallofPress },
    "Cable woodchop" to lazy { cableWoodchop },
    "Ab wheel rollout" to lazy { abWheelRollout },
    "Stability ball rollout" to lazy { stabilityBallRollout },
    "Floor crunch" to lazy { floorCrunch },
    "Reverse crunch" to lazy { reverseCrunch },
    "Cable crunch" to lazy { cableCrunch },
    "Machine crunch" to lazy { machineCrunch },
    "Bicycle crunch" to lazy { bicycleCrunch },
    "Dead bug" to lazy { deadBug },
    "Bird dog" to lazy { birdDog },
)
