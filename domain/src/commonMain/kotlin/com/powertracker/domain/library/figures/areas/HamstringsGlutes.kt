package com.powertracker.domain.library.figures.areas

import com.powertracker.domain.library.areas.RACK_IT
import com.powertracker.domain.library.areas.RACK_SAFETY
import com.powertracker.domain.library.figures.*
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Figures for the library's HamstringsGlutes area (library/areas/HamstringsGlutes.kt), keyed by catalogue name.

/**
 * RDL. START standing tall with the knees slightly bent, the bar hanging against the thighs; END hips pushed back with
 * the same knee bend, the bar slid down close to the legs to about the knees.
 * - Knee bend 18° in both panels: the guide's "same slight bend" (ACE romanian deadlift, a slight bend held through the
 *   movement: https://www.acefitness.org/resources/everyone/exercise-library/317/romanian-deadlift/); the value is a
 *   neutral "slight".
 * - END: the bar at the knees, the more conservative of the sources' depths (NSCA lowers to just below the knees, the
 *   tibial tuberosity: https://www.nsca.com/education/articles/kinetic-select/romanian-deadlift-rdl/). The shins come
 *   back to vertical as the hips go back; the torso ends up about 30° above level, the stage the NSCA's progression
 *   article shows at the knees (https://www.nsca.com/education/articles/nsca-coach/exercise-progressions-for-resuming-strength-training-following-posterior-chain-muscle-injury/).
 * - The bar over the mid-foot and within 4 cm of the legs in both panels (NSCA: as close to the body as possible).
 * - Rack (guide's safety line): the bar was taken from J-hooks at mid-thigh height; safety arms below its lowest point.
 */
private val rdl: ExerciseFigure by lazy {
    val knee = 18.0
    val ankleX = 0.0
    val clear = Body.THIGH_WIDTH / 2 + Barbell.SHAFT_RADIUS + 1.0
    val upLeg = standingLeg(ankleX, shinLean = 8.0, knee = knee, torso = 88.0)
    val upShoulder = shoulderOf(upLeg.hip, 88.0)
    // Standing: the bar against the front of the thighs.
    val thighFrontX = upLeg.hip.x + (upLeg.knee.x - upLeg.hip.x) * 0.4 + clear
    val start = standOn(upLeg, 88.0, hangingArmAt(upShoulder, thighFrontX, 88.0))

    val downLeg = standingLeg(ankleX, shinLean = 0.0, knee = knee, torso = 30.0)
    val barX = downLeg.knee.x + clear
    // The torso angle that brings the bar to the knees with the arms hanging to just in front of them.
    val torso = (15..60).map { it.toDouble() }.minBy { t ->
        val sh = shoulderOf(downLeg.hip, t)
        val pose = standOn(standingLeg(ankleX, 0.0, knee, t), t, hangingArmAt(sh, barX, t))
        abs(pose.skeleton().near.grip.y - (downLeg.knee.y - 2.0))
    }
    val endLeg = standingLeg(ankleX, 0.0, knee, torso)
    val end = standOn(endLeg, torso, hangingArmAt(shoulderOf(endLeg.hip, torso), barX, torso))
    val barStart = start.skeleton().near.grip
    val barLow = end.skeleton().near.grip
    // The posts 60 cm in front of the bar, clear of the head as the hips go back.
    val rack = rackInFront(barStart.x, jHookHeight = -barStart.y - 4.0, safetyHeight = -barLow.y - Barbell.SHAFT_RADIUS - 8.0, gap = 60.0)
    val props = listOf(Floor, rack, Barbell())
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(start, props),
        end = Scene(end, props),
        startCaption = "Knees slightly bent",
        endCaption = "Weight about at knees",
        thumbnail = Which.END,
        guideLines = listOf(
            RACK_SAFETY,
            "Stand tall with your knees slightly bent.",
            "Push your hips back. Keep the same slight bend in your knees.",
            "Slide the bar down close to your legs.",
            "Lower until the weight is about at your knees. Stop sooner if your back starts to round.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(
            BarOverMidfoot(Which.START), BarOverMidfoot(Which.END),
            BarNearLegs(within = 4.0, at = Which.START), BarNearLegs(within = 4.0, at = Which.END),
        ),
    )
}


// ---- the batch (Task 11) ----
//
// Camera. The side view for the hinges, the swings, the leg curls (the knee bends in the sagittal plane), the Nordic
// curl and the glute-ham raise, the bridges and thrusts, the reverse hyper and the quadruped hip extension: what each
// guide is about (the hip hinge and the back, the bell's path, the pad on the lower leg and the knee's bend, the body
// straight from the knees, the hips in line at the top) shows from the side. The abduction and adduction group moves
// the legs across the camera, so a side view cannot show it (lesson 9); each gets the front view:
// - Machine hip abduction and adduction: seen from in front of the seat, the knees move apart or together.
// - Lateral band walk: seen from in front, the feet step apart against the band.
// - Cable hip abduction: seen from in front, the cuffed leg swings out to the side, away from the pulley.
// - Side-lying hip abduction and the clamshell: the lifter lies on their side along the floor, seen from in front of
//   the body (as the approved Side-lying external rotation), so the top leg, or the top knee, rises in the picture.
// Every position a guide does not fix is sourced in its comment (allowed domains only), or kept neutral and marked so.

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

private fun norm(v: Point): Point = v * (1.0 / v.length())

// Guide lines shared by several hamstring and glute guides (library/areas/HamstringsGlutes.kt), verbatim.
private const val BRACE_TRUNK = "Brace your trunk before each rep."
private const val HINGE_DEPTH = "Lower until the weight is about at your knees. Stop sooner if your back starts to round."
private const val SAME_BEND = "Push your hips back. Keep the same slight bend in your knees."
private const val KNEE_ON_PIVOT = "Line up your knees with the machine's pivot."
private const val SWITCH_LEGS = "Do all your reps on one leg, then switch."

// ---- hinges ----

/**
 * A two-leg hinge with the load hanging from straight arms, [clear] cm in front of the legs' centre line: START standing
 * tall (the torso 88°) with the knees bent [knee]° and the shins [startShin]° forward, the load against the front of
 * the thighs; END the hips pushed back with the same knee bend, the shins vertical, the torso lowered until the load
 * is about at the knees (the RDL's own rule above, solved the same way).
 */
private class Hinge(val start: Pose, val end: Pose)

private fun hinge(knee: Double, startShin: Double, clear: Double): Hinge {
    val upLeg = standingLeg(0.0, startShin, knee, 88.0)
    val thighFrontX = upLeg.hip.x + (upLeg.knee.x - upLeg.hip.x) * 0.4 + clear
    val start = standOn(upLeg, 88.0, hangingArmAt(shoulderOf(upLeg.hip, 88.0), thighFrontX, 88.0))
    val downLeg = standingLeg(0.0, 0.0, knee, 30.0)
    val loadX = downLeg.knee.x + clear
    fun at(t: Double): Pose {
        val leg = standingLeg(0.0, 0.0, knee, t)
        return standOn(leg, t, hangingArmAt(shoulderOf(leg.hip, t), loadX, t))
    }
    val torso = (10..120).map { it * 0.5 }.minBy { t -> abs(at(t).skeleton().near.grip.y - (downLeg.knee.y - 2.0)) }
    return Hinge(start, at(torso))
}

/**
 * Stiff-leg deadlift. The RDL's hinge (above) with the knees only slightly bent: 8° (the RDL holds 18°), so the hips
 * go back less and the torso comes lower for the same depth. Guide: "Bend your knees slightly", the bar close to the
 * legs, "about at your knees" (the same depth as the RDL, NSCA's and ACE's stop, Sources.kt). 8° is neutral: the
 * smallest bend that still shows, as no allowed source seen gives an angle. The bar is taken from the floor (the guide's set-up), so no rack is drawn; the bar over the mid-foot and close
 * to the legs in both panels.
 */
private val stiffLegDeadlift: ExerciseFigure by lazy {
    val h = hinge(knee = 8.0, startShin = 4.0, clear = Body.THIGH_WIDTH / 2 + Barbell.SHAFT_RADIUS + 1.0)
    figure(
        View.SIDE, h.start, h.end, listOf(Floor, Barbell()),
        startCaption = "Bend knees slightly",
        endCaption = "Bar close to your legs",
        thumbnail = Which.END,
        guideLines = listOf(
            "Bend your knees slightly and keep your back straight.",
            "Bend at your hips to lower the bar close to your legs.",
            HINGE_DEPTH,
        ),
        contacts = FEET_DOWN,
        checks = listOf(
            BarOverMidfoot(Which.START), BarOverMidfoot(Which.END),
            BarNearLegs(within = 4.0, at = Which.START), BarNearLegs(within = 4.0, at = Which.END),
        ),
    )
}

/**
 * DB RDL. The RDL's two poses (the same 18° knee bend) with a dumbbell in each hand instead of the bar, the palms
 * towards the thighs, so the handles cross the body and the side view sees each dumbbell end on, a disc (lesson 2);
 * the disc's edge just clear of the thighs at the top and of the knees at the bottom ("close to your legs"). END the
 * dumbbells about at the knees. ACE technique series RDL
 * (https://www.acefitness.org/continuing-education/prosource/january-2016/5767/ace-technique-series-romanian-deadlift/,
 * Sources.kt). No rack: the guide starts standing with the dumbbells.
 */
private val dbRdl: ExerciseFigure by lazy {
    val h = hinge(knee = 18.0, startShin = 8.0, clear = Body.THIGH_WIDTH / 2 + Dumbbell.HEAD_RADIUS + 0.5)
    figure(
        View.SIDE, h.start, h.end, listOf(Floor, Dumbbell(Side.FAR), Dumbbell(Side.NEAR)),
        startCaption = "Knees slightly bent",
        endCaption = "Weights about at knees",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand tall holding a dumbbell in each hand.",
            "Keep your knees slightly bent and your back straight.",
            SAME_BEND,
            "Lower the dumbbells close to your legs.",
            HINGE_DEPTH,
        ),
        contacts = FEET_DOWN,
        checks = emptyList(),
    )
}

/**
 * Single-leg RDL. START standing on both feet, hip-width, the knees slightly bent (10°), the dumbbell in the near hand,
 * hanging in front of the thigh (the palm towards the body: a disc). END on the far leg, the near leg reaching straight
 * back in line with the body (hip and knee straight: "back leg in line with your body"), the back flat, both arms
 * hanging straight down, lowered until the dumbbell reaches the floor (the guide's step), touching it.
 * - How it reaches: ExRx's dumbbell single-leg stiff-leg deadlift lowers the weights towards the floor and says a lifter
 *   may bend the supporting knee more to get them closer (https://exrx.net/WeightExercises/GluteusMaximus/DBSingleLegStiffLegDeadlift,
 *   Sources.kt). So the standing knee is bent 35° (the shin 12° forward: still a soft knee, not a squat), and with that
 *   the torso is solved to the angle that sets the dumbbell on the floor: about 16° below level, the head a little lower
 *   than the hips, the back straight. (With the figure's arm length a level torso leaves the weight about 14 cm up; the
 *   weight hand's wrist is flexed so the drawn hand, which cannot curl round the handle, does not poke past the disc,
 *   which costs about 3°.) A judgement call, in the report: more knee would turn it into a squat.
 * - The free (far) arm hangs down beside the near one, its elbow bent (lesson 11), not along the torso.
 */
private val singleLegRdl: ExerciseFigure by lazy {
    val clear = Body.THIGH_WIDTH / 2 + Dumbbell.HEAD_RADIUS + 0.5
    val stand = standingLeg(0.0, 5.0, 10.0, 88.0)
    val frontX = stand.hip.x + (stand.knee.x - stand.hip.x) * 0.4 + clear
    val start = standOn(stand, 88.0, hangingArmAt(shoulderOf(stand.hip, 88.0), frontX, 88.0), farArm = armAt(270.0, 88.0))
    fun lowered(torso: Double): Pose {
        val one = standingLeg(0.0, 12.0, 35.0, torso)
        // The weight hand's wrist flexed 48° (the fingers round the handle, forward), so the drawn hand stays inside the
        // dumbbell's disc above the floor; the free hand hangs with the elbow bent 40°, clear of the floor.
        val down = armAt(270.0, torso).copy(wrist = 48.0)
        val free = armAt(270.0, torso, elbow = 40.0)
        return Pose(one.hip.x, one.hip.y, torso, nearArm = down, nearLeg = Leg(hip = 0.0, knee = 0.0, ankle = -10.0), farArm = free, farLeg = one.leg)
    }
    // The torso angle (0.1° steps, level down to 20° below) whose hanging dumbbell just touches the floor.
    val end = lowered((0..200).map { -it * 0.1 }.minBy { t -> abs(-lowered(t).skeleton().near.grip.y - Dumbbell.HEAD_RADIUS - 0.5) })
    figure(
        View.SIDE, start, end, listOf(Floor, Dumbbell(Side.NEAR)),
        startCaption = "Knees slightly bent",
        endCaption = "Leg straight behind you",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand with your feet about hip-width apart, back tall and knees slightly bent.",
            "Hold a dumbbell in the hand opposite your standing leg, or use no weight.",
            "Push your hips back and lift one foot off the floor.",
            "Keep your back straight as that leg reaches straight back behind you.",
            "Keep a slight bend in your standing knee.",
        ),
        contacts = listOf(Contact(BodyPoint.FAR_FOOT, Surface.FLOOR, BOTH), Contact(BodyPoint.NEAR_FOOT, Surface.FLOOR, setOf(Which.START))),
        checks = emptyList(),
    )
}

/**
 * Good morning. The bar across the upper back, the hands out on it (the Squat's turned-out arms, MainLifts.kt).
 * START standing tall, the knees 10° bent, the torso solved so the bar is over the mid-foot; END the torso lowered to
 * level with the floor (3° above it: "no further"), the knees bent 22° ("let your knees bend slightly"; neutral, a little
 * more than the RDL's 18°; the torso-to-level depth is ExRx's bent-knee good morning's, as the catalogue notes:
 * https://exrx.net/WeightExercises/GluteusMaximus/BBBentKneeGoodMorning, Sources.kt), the hips back so the bar stays
 * over the foot (the shins tipped back up to 10°, the most hip shift the ankle gives; the bar ends over the front of the
 * foot, not the mid-foot: with the torso level the hips cannot go further back). Rack (the guide's safety line): J-hooks
 * under the bar's top position, safety arms under its lowest, the posts in front, clear of the head at the bottom.
 */
private val goodMorning: ExerciseFigure by lazy {
    // Standing, the body tipped forward over the ankles (the shins 12°, the thighs upright) as the Squat's standing pose
    // is, so the torso needs only a small lean to put the bar over the mid-foot.
    val upLeg0 = standingLeg(0.0, 12.0, 10.0, 85.0)
    val upTorso = balancedTorso(upLeg0, 70.0..90.0) { hip, t -> barOnBack(hip, t) }
    val start = backSquatPose(standingLeg(0.0, 12.0, 10.0, upTorso), upTorso)
    val torso = 3.0
    val mid = midfootX(0.0)
    val shin = (-20..8).map { it * 0.5 }.minBy { sl ->
        abs(barOnBack(standingLeg(0.0, sl, 22.0, torso).hip, torso).x - mid)
    }
    val end = backSquatPose(standingLeg(0.0, shin, 22.0, torso), torso)
    val barTop = barOnBack(Point(start.pelvisX, start.pelvisY), upTorso)
    val barLow = barOnBack(Point(end.pelvisX, end.pelvisY), torso)
    val headFront = end.skeleton().head.x + Body.HEAD_RADIUS
    val startFace = start.skeleton().head.x + Body.HEAD_RADIUS
    // The rack stands back far enough that the head clears the posts in both panels, and its safety arms (at about the
    // head's height when the torso is level) end 6 cm in front of the face at the bottom, so the head does not rest on
    // them. They still run under the bar's height, in front of its path, as the other racks' arms stop short of it.
    val posts = maxOf(headFront, startFace) + 30.0
    val rack = PowerRack(
        x = posts, jHookHeight = -barTop.y - 8.0, safetyHeight = -barLow.y - Barbell.SHAFT_RADIUS - 6.0,
        armLength = posts - headFront - 6.0, lifterLeft = true,
    )
    figure(
        View.SIDE, start, end, listOf(Floor, rack, Barbell(BarHold.UPPER_BACK)),
        startCaption = "Bar across your back",
        endCaption = "Torso level, no further",
        thumbnail = Which.END,
        guideLines = listOf(
            RACK_SAFETY,
            "Rest the bar across the back of your shoulders and grip it out to the sides.",
            "Bend at your hips to lower your torso forward. Let your knees bend slightly.",
            "Lower until your torso is level with the floor, no further. Stop sooner if your back starts to round.",
            RACK_IT,
        ),
        contacts = FEET_DOWN,
        checks = listOf(BarOverMidfoot(Which.START)),
    )
}

// ---- swings ----

/** The swing's bottom torso and its arm: the straight arm down between the thighs, the wrist on the near thigh's line. */
private class SwingBottom(val leg: StandingLeg, val torso: Double, val arm: Arm, val along: Double)

/**
 * The bottom of a swing: knees bent 22° (neutral: a soft bend, not a squat) with the shins nearly upright (4°: the
 * guide's "shins upright"), the hips folded until the straight arm's wrist lands on the near thigh's centre line 55% of the way
 * from the hip to the knee: the forearms against the inner thighs, the bell swung back under the hips (ExRx kettlebell two arm swing: "forearms make contact with inner thighs permitting kettlebell to swing back under
 * hips", https://exrx.net/WeightExercises/Kettlebell/KBTwoArmSwing, Sources.kt).
 */
private val swingBottom: SwingBottom by lazy {
    val reach = Segment.UPPER_ARM.length + Segment.FOREARM.length
    fun at(t: Double): SwingBottom? {
        val leg = standingLeg(0.0, 4.0, 22.0, t)
        val sh = shoulderOf(leg.hip, t)
        // The point on the thigh's line (hip to knee, and on past the knee) at the wrist's reach from the shoulder.
        val d = leg.knee - leg.hip
        var best: Double? = null
        var u = 0.0
        while (u <= 1.6) {
            val p = leg.hip + d * u
            if (best == null && (p - sh).length() >= reach) best = u
            u += 0.002
        }
        val along = best ?: return null
        val p = leg.hip + d * along
        return SwingBottom(leg, t, straightArm(sh, p, t), along)
    }
    (20..90).map { it * 0.5 }.mapNotNull { at(it) }.minBy { abs(it.along - 0.55) }
}

/** The swing's top: standing tall, the straight arms forward with the hands at chest height (the chest touch point's height). */
private val swingTopArm: Arm by lazy { armAt(-asin(Body.CHEST_BELOW_SHOULDER / ARM_REACH) * DEG, 90.0) }

private val tallLeg by lazy { standingLeg(0.0, 0.0, 0.0, 90.0) }

/**
 * Kettlebell swing. START the bottom of the swing (above): the hinge, the bell back between the legs with the forearms
 * against the inner thighs, the back straight, the shins upright. END standing tall, the hips and knees straight, the
 * arms straight forward with the bell at chest height (the guide's "chest or shoulder height", the lower of ACE's and
 * NSCA's heights: Sources.kt). Both hands on the handle (the far arm hidden behind the near). The bell hangs on past the
 * hands in line with the arms all through the swing (Kit KettlebellHold.SWING).
 */
private val kettlebellSwing: ExerciseFigure by lazy {
    val b = swingBottom
    figure(
        View.SIDE, standOn(b.leg, b.torso, b.arm), standOn(tallLeg, 90.0, swingTopArm), listOf(Floor, Kettlebell(Side.NEAR, KettlebellHold.SWING)),
        startCaption = "Bell back between legs",
        endCaption = "Chest or shoulder height",
        thumbnail = Which.END,
        guideLines = listOf(
            "Hinge at your hips to swing the bell back between your legs. Keep your back straight and your shins upright.",
            "Drive your hips forward and straighten your knees to swing the bell forward and up.",
            "Let it rise to chest or shoulder height, with your arms straight.",
        ),
        contacts = FEET_DOWN,
        checks = emptyList(),
    )
}

/**
 * Single-arm kettlebell swing. The two-arm swing's legs and bell arm, the bell in the near hand only; the free (far)
 * arm swings back as the hips hinge (START: the upper arm 30° behind the torso's line, elbow 15°: neutral) and forward
 * as they drive (END: straight forward and down 35°, below the bell arm, elbow 10°: neutral), so it is drawn apart from
 * the bell arm in both panels (lesson 11). The thumbnail is START, where the free arm reaching back sets it apart from
 * the two-arm swing's thumbnail at a glance (lesson 10). END the bell at about chest height (the guide).
 */
private val singleArmKettlebellSwing: ExerciseFigure by lazy {
    val b = swingBottom
    figure(
        View.SIDE,
        standOn(b.leg, b.torso, b.arm, farArm = Arm(shoulder = -30.0, elbow = 15.0)),
        standOn(tallLeg, 90.0, swingTopArm, farArm = armAt(-35.0, 90.0, elbow = 10.0)),
        listOf(Floor, Kettlebell(Side.NEAR, KettlebellHold.SWING)),
        startCaption = "Free arm swings back",
        endCaption = "About chest height",
        thumbnail = Which.START,
        guideLines = listOf(
            "Sink into your hips and draw the bell back between your legs.",
            "Let it rise to about chest height, then guide it back down between your legs.",
            "Let your free arm swing back as you hinge, and forward as your hips drive.",
        ),
        contacts = FEET_DOWN,
        checks = emptyList(),
    )
}

// ---- leg curls ----

/**
 * Where a curl machine's roller sits on [l]'s lower leg: [along] of the shin from the knee, on its back (the calf's
 * side), touching it. The calf faces a quarter turn clockwise from the shin's direction, whichever way the leg points.
 */
private fun calfRoller(l: LimbPoints, radius: Double, along: Double = 0.8): Point {
    val d = norm(l.ankle - l.knee)
    return l.knee + d * (Segment.SHIN.length * along) + unit(angleTo(d) - 90.0) * (Body.SHIN_WIDTH / 2 + radius)
}

/** The knee bend at the top of the leg curls: 115°, neutral (the guides: towards the back of the thigh, short of the 135° limit). */
private const val CURL_KNEE = 115.0

/**
 * Lying leg curl. Face down on a level pad 60 cm up (the torso level, head to the right), the hips on it and the
 * thighs tipped 4° down from the body's line so they rest on it by the knees; the knees just past the pad's edge
 * (the joint 4 cm beyond it), lined up with the lever's pivot; the roller on the backs of the lower legs, above the
 * heels (80% down the shin); the hands on the handles under the head end. START knees straight; END bent to 115°,
 * the pad raised towards the backs of the thighs, the hips still on the pad. ExRx lever lying leg curl ("raise lever
 * pad to back of thighs by flexing knees", https://exrx.net/WeightExercises/Hamstrings/LVLyingLegCurl, Sources.kt);
 * the angle is neutral.
 */
private val lyingLegCurl: ExerciseFigure by lazy {
    val h = 60.0
    val pelvis = Point(0.0, -(h + Body.TORSO_RADIUS))
    val hip = asin(3.0 / Segment.THIGH.length) * DEG
    val bare = Pose(pelvis.x, pelvis.y, 0.0, nearArm = HANG, nearLeg = Leg(hip, 0.0)).skeleton()
    // The handles just past the pad's head end, below it: the arms reach forward and down.
    val handle = Point(bare.shoulder.x + 30.0, -h + 18.0)
    val arm = armTo(bare.shoulder, handle, 0.0)
    fun pose(knee: Double) = Pose(pelvis.x, pelvis.y, 0.0, nearArm = arm, nearLeg = Leg(hip, knee, ankle = 0.0))
    val knee = bare.near.knee
    fun machine(p: Pose) = LyingLegCurlMachine(
        kneeEnd = knee.x + 4.0, height = h, pivot = knee,
        roller = calfRoller(p.skeleton().near, LyingLegCurlMachine.ROLLER_RADIUS), handle = handle,
    )
    val start = pose(0.0)
    val end = pose(CURL_KNEE)
    figure(
        View.SIDE, start, end, listOf(Floor, machine(start)),
        startCaption = "Knees just past the edge",
        endCaption = "Pad toward your thighs",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie face down on the bench with your knees just past its edge.",
            KNEE_ON_PIVOT,
            "Set the pad on the back of your lower legs, above your heels.",
            "Grip the handles.",
            "Bend your knees to raise the pad toward the backs of your thighs.",
            "Lower the pad until your knees are straight.",
        ),
        contacts = listOf(
            Contact(BodyPoint.HIPS, Surface.BENCH, BOTH),
            Contact(BodyPoint.THIGHS, Surface.BENCH, BOTH),
            Contact(BodyPoint.SHINS, Surface.SHIN_PAD, BOTH),
            Contact(BodyPoint.HANDS, Surface.HANDLE, BOTH),
        ),
        checks = emptyList(),
        endProps = listOf(Floor, machine(end)),
    )
}

/**
 * Seated leg curl. On a seat 50 cm up, the back against a pad reclined 10° (the torso 100°), the thighs along the seat
 * (4° down, resting on it towards the knees), the knees 11 cm past the seat's front, lined up with the pivot; the lap
 * pad on the thighs just above the knees; the roller under the backs of the lower legs, above the ankles (85% down the
 * shin); the near hand on the side handle. START the legs straight out in front (knees 2°: "until your legs are
 * straight"); END the knees bent to 100°, the pad pulled back under the seat's front. ExRx lever seated leg curl
 * ("pull the far padded lever toward the back of the thighs by flexing the knees",
 * https://exrx.net/WeightExercises/Hamstrings/LVSeatedLegCurl, Sources.kt); the angles are neutral.
 */
private val seatedLegCurl: ExerciseFigure by lazy {
    val seat = 50.0
    val torso = 100.0
    val pelvis = Point(0.0, -(seat + Body.TORSO_RADIUS))
    // The thigh along the seat, 4° down so it rests on it by the knee.
    val hip = fold180(-asin(3.0 / Segment.THIGH.length) * DEG - torso - 180.0)
    val bare = Pose(pelvis.x, pelvis.y, torso, nearArm = HANG, nearLeg = Leg(hip, 0.0)).skeleton()
    // The back pad's face on the torso's back: hinged where that line meets the seat's top.
    val back = pelvis + unit(torso + 90.0) * Body.TORSO_RADIUS
    val hingeX = back.x + (-seat - back.y) / (-sin(torso / DEG)) * cos(torso / DEG)
    val seatFront = hingeX + SeatedLegCurlMachine.SEAT_LENGTH
    val knee = bare.near.knee
    val lap = bare.near.hip + norm(knee - bare.near.hip) * (Segment.THIGH.length * 0.8) +
        unit(angleTo(knee - bare.near.hip) + 90.0) * (Body.THIGH_WIDTH / 2 + SeatedLegCurlMachine.ROLLER_RADIUS)
    val handle = Point(pelvis.x + 14.0, -seat - 10.0)
    val arm = armTo(bare.shoulder, handle, torso)
    fun pose(k: Double) = Pose(pelvis.x, pelvis.y, torso, nearArm = arm, nearLeg = Leg(hip, k, ankle = 0.0))
    fun machine(p: Pose) = SeatedLegCurlMachine(
        seatFront = seatFront, seatHeight = seat, backAngle = 180.0 - torso, pivot = knee,
        roller = calfRoller(p.skeleton().near, SeatedLegCurlMachine.ROLLER_RADIUS, along = 0.85), lap = lap, handle = handle,
    )
    val start = pose(2.0)
    val end = pose(100.0)
    figure(
        View.SIDE, start, end, listOf(Floor, machine(start)),
        startCaption = "Legs straight",
        endCaption = "Pull the pad back under",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit with your back against the back pad.",
            KNEE_ON_PIVOT,
            "Rest the backs of your lower legs on the pad, just above your ankles.",
            "Lower the lap pad onto your thighs, just above your knees.",
            "Bend your knees to pull the pad back under you.",
            "Let it come back until your legs are straight.",
        ),
        contacts = listOf(
            Contact(BodyPoint.HIPS, Surface.SEAT, BOTH),
            Contact(BodyPoint.UPPER_BACK, Surface.BACK_PAD, BOTH),
            Contact(BodyPoint.THIGHS, Surface.THIGH_PAD, BOTH),
            Contact(BodyPoint.SHINS, Surface.SHIN_PAD, BOTH),
            Contact(BodyPoint.NEAR_HAND, Surface.HANDLE, BOTH),
        ),
        checks = emptyList(),
        endProps = listOf(Floor, machine(end)),
    )
}

/**
 * Standing leg curl. Standing on the far leg (knee 3°), bent forward at the hips (the torso 60°: neutral, "bend
 * forward at your hips"), the hands on the machine's handles; the working (near) thigh hangs 15° forward of vertical
 * against the thigh pad just above the knee, the knee on the pivot, the roller behind the lower leg above the heel.
 * START the knee straight, the foot just off the floor; END bent to 115°, the heel towards the back of the thigh. ExRx
 * lever standing leg curl (https://exrx.net/WeightExercises/Hamstrings/LVStandingLegCurl, Sources.kt); angles neutral.
 */
private val standingLegCurl: ExerciseFigure by lazy {
    val torso = 60.0
    val stand = standingLeg(0.0, 2.0, 3.0, torso)
    val hip = 285.0 - torso - 180.0
    val bare = Pose(stand.hip.x, stand.hip.y, torso, nearArm = HANG, nearLeg = Leg(hip, 0.0), farLeg = stand.leg).skeleton()
    val n = bare.near
    val thighDir = norm(n.knee - n.hip)
    val pad = n.hip + thighDir * (Segment.THIGH.length * 0.8) + unit(angleTo(thighDir) + 90.0) * (Body.THIGH_WIDTH / 2 + StandingLegCurlMachine.ROLLER_RADIUS)
    val handle = bare.shoulder + Point(22.0, 30.0)
    val arm = armTo(bare.shoulder, handle, torso)
    fun pose(k: Double) = Pose(stand.hip.x, stand.hip.y, torso, nearArm = arm, nearLeg = Leg(hip, k, ankle = 0.0), farLeg = stand.leg)
    fun machine(p: Pose) = StandingLegCurlMachine(
        postX = maxOf(pad.x, handle.x) + 10.0, thighPad = pad, pivot = n.knee,
        roller = calfRoller(p.skeleton().near, StandingLegCurlMachine.ROLLER_RADIUS), handle = handle,
    )
    val start = pose(0.0)
    val end = pose(CURL_KNEE)
    figure(
        View.SIDE, start, end, listOf(Floor, machine(start)),
        startCaption = "Bend forward at hips",
        endCaption = "Pad toward your thigh",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand in the machine with the pad behind one lower leg.",
            "Line up that knee with the machine's pivot.",
            "Bend forward at your hips and grip the handles.",
            "Bend your knee to raise the pad toward the back of your thigh.",
            "Lower until your knee is straight.",
        ),
        contacts = listOf(
            Contact(BodyPoint.FAR_FOOT, Surface.FLOOR, BOTH),
            Contact(BodyPoint.NEAR_THIGH, Surface.THIGH_PAD, BOTH),
            Contact(BodyPoint.NEAR_SHIN, Surface.SHIN_PAD, BOTH),
            Contact(BodyPoint.HANDS, Surface.HANDLE, BOTH),
        ),
        checks = emptyList(),
        endProps = listOf(Floor, machine(end)),
    )
}

/**
 * Cable leg curl. Facing a low pulley, the cuff on the near ankle; the far foot stepped back, its leg straight and in
 * line with the torso, leaning in 10° (the torso 80°), the arms straight to the stack's frame at about shoulder height
 * ("arms straight to hold you up"); the working thigh 20° forward of vertical. START the knee straight, the foot just
 * off the floor; END the knee fully bent (120°, short of the 135° limit), the cable pulled back. ExRx cable standing
 * leg curl (https://exrx.net/WeightExercises/Hamstrings/CBStandingLegCurl, Sources.kt); angles neutral. The cable runs
 * from the pulley to the cuff in both panels (lesson 3).
 */
private val cableLegCurl: ExerciseFigure by lazy {
    val torso = 80.0
    val stand = standingLeg(0.0, 10.0, 0.0, torso)
    val hip = 290.0 - torso - 180.0
    val bare = Pose(stand.hip.x, stand.hip.y, torso, nearArm = HANG, nearLeg = Leg(hip, 0.0), farLeg = stand.leg).skeleton()
    val arm = armAt(-12.0, torso)
    val grip = Pose(stand.hip.x, stand.hip.y, torso, nearArm = arm, nearLeg = Leg(hip, 0.0)).skeleton().near.grip
    val column = grip.x + 9.0 + Body.HAND_WIDTH / 2
    val stack = CableStack(x = column, pulleyHeight = 12.0, reach = -14.0, cableInFront = true)
    fun pose(k: Double) = Pose(stand.hip.x, stand.hip.y, torso, nearArm = arm, nearLeg = Leg(hip, k, ankle = 0.0), farLeg = stand.leg)
    figure(
        View.SIDE, pose(0.0), pose(120.0), listOf(Floor, stack, AnkleStrap(Side.NEAR)),
        startCaption = "Foot just off the floor",
        endCaption = "Knee fully bent",
        thumbnail = Which.END,
        guideLines = listOf(
            "Attach a foot harness to a low pulley and put it on one ankle.",
            "Grip the support bar with both hands and step back with your other foot.",
            "Keep your arms straight to hold you up, with your working foot just off the floor.",
            "Bend your knee to pull the cable back until your knee is fully bent.",
        ),
        contacts = listOf(Contact(BodyPoint.FAR_FOOT, Surface.FLOOR, BOTH)),
        checks = emptyList(),
    )
}

/**
 * Stability ball leg curl. Lying on the back, the head to the left, the shoulders and head on the floor (the neck bent
 * so the head lies flat), the arms out on the floor (drawn reaching down towards the feet, foreshortened: they point
 * out to the sides, towards the camera); a 65 cm ball (KitHamstrings.kt). START the hips lifted until the body is in a
 * straight line from the shoulders to the heels, the lower legs on the top of the ball, the toes pointing up; END the
 * heels pulled in towards the hips, the knees bent 100°, the soles on the ball, rolled in, the torso still in line
 * with the thighs (the guide's cue). ACE stability ball hamstring curl
 * (https://www.acefitness.org/resources/everyone/exercise-library/59/stability-ball-hamstring-curl/, Sources.kt); the
 * knee angle is neutral.
 */
private val stabilityBallLegCurl: ExerciseFigure by lazy {
    val r = StabilityBall.DIAMETER / 2
    val sh = Point(0.0, -Body.TORSO_RADIUS)
    val legLength = Segment.THIGH.length + Segment.SHIN.length
    fun arms(torso: Double) = Arm(shoulder = fold180(-8.0 - torso - 180.0), elbow = 0.0, out = 60.0, forearmOut = 60.0)
    // START: the body line rising [theta]° from the shoulders; the ball under the shins, tangent to their underside.
    fun straight(theta: Double): Pair<Pose, Double> {
        val torso = theta + 180.0
        val pelvis = sh + unit(theta) * Segment.TORSO.length
        val pose = Pose(pelvis.x, pelvis.y, torso, neck = theta, nearArm = arms(torso), nearLeg = Leg(0.0, 0.0, ankle = -theta))
        // The centre of a ball under the leg's line: the line offset down by the shin's and the ball's radii.
        val below = unit(theta - 90.0) * (Body.SHIN_WIDTH / 2 + r)
        // Where on the leg's line the centre's height is the ball's radius: solve along the line.
        val u = (-r - (sh.y + below.y)) / unit(theta).y
        return pose to (sh.x + unit(theta).x * u + below.x)
    }
    val theta = (100..300).map { it * 0.1 }.minBy { t ->
        val (p, bx) = straight(t)
        val s = p.skeleton()
        // The tangent point 80% down the shin: where the leg's line passes closest to the ball's centre.
        val c = Point(bx, -r)
        val d = norm(s.near.ankle - s.near.knee)
        val along = ((c - s.near.knee).x * d.x + (c - s.near.knee).y * d.y) / Segment.SHIN.length
        abs(along - 0.8)
    }
    val (start, startBall) = straight(theta)
    // END: the knees bent 100°, the body line steeper, the ball under the soles.
    val k = 100.0
    fun curled(phi: Double): Pair<Pose, Double> {
        val torso = phi + 180.0
        val pelvis = sh + unit(phi) * Segment.TORSO.length
        val pose = Pose(pelvis.x, pelvis.y, torso, neck = phi, nearArm = arms(torso), nearLeg = Leg(0.0, k, ankle = 0.0))
        val n = pose.skeleton().near
        val foot = angleTo(n.toe - n.heel)
        val mid = (n.heel + n.toe) * 0.5
        val c = mid + unit(foot - 90.0) * (Body.FOOT_WIDTH / 2 + r)
        return pose to c.x
    }
    val phi = (200..600).map { it * 0.1 }.minBy { p ->
        val (pose, _) = curled(p)
        val n = pose.skeleton().near
        val foot = angleTo(n.toe - n.heel)
        val c = (n.heel + n.toe) * 0.5 + unit(foot - 90.0) * (Body.FOOT_WIDTH / 2 + r)
        abs(c.y + r)
    }
    val (end, endBall) = curled(phi)
    figure(
        View.SIDE, start, end, listOf(Floor, StabilityBall(startBall)),
        startCaption = "Body in a straight line",
        endCaption = "Heels toward your hips",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie on your back with your lower legs and heels on the ball, feet about hip-width apart.",
            "Point your toes up toward the ceiling.",
            "Squeeze your glutes to lift your hips until your body is in a straight line.",
            "Pull your heels toward your hips, letting the ball roll in.",
        ),
        contacts = listOf(
            Contact(BodyPoint.HEAD, Surface.FLOOR, BOTH),
            Contact(BodyPoint.SHINS, Surface.BALL, setOf(Which.START)),
            Contact(BodyPoint.FEET, Surface.BALL, setOf(Which.END)),
        ),
        checks = emptyList(),
        endProps = listOf(Floor, StabilityBall(endBall)),
    )
}

// ---- Nordic curl and glute-ham raise ----

/** Arms crossed on the chest: the hands just in front of the chest touch point (the back extension's, Back.kt). */
private fun crossedArms(pose: Pose): Arm {
    val bare = pose.skeleton()
    val hands = bare.point(BodyPoint.CHEST) + unit(pose.torso - 90.0) * (Body.HAND_WIDTH / 2 + 1.0)
    return armTo(bare.shoulder, hands, pose.torso)
}

/**
 * Nordic curl. Kneeling on a 6 cm pad (the knee 0.4 cm into its top under the thigh's round, so the knee rests on it),
 * the shins along it, the ankles under a roller, the feet past the pad's end with the toes on the floor; the body in
 * one line from the knees to the head. START upright (knees 90°), the arms out to the sides, the elbows slightly bent
 * (drawn foreshortened, pointing out towards the camera: 55° out of the picture, elbows 20°: neutral). END lowered
 * forward by straightening the knees, the hips still straight, to 30° above level (knees 30°: neutral, where the body
 * "starts to fall"), the fingertips just touching down on the floor in front of the shoulders. ExRx self-assisted
 * inverse leg curl (floor) (https://exrx.net/WeightExercises/Hamstrings/ASHamstringRaiseSelfFloor, Sources.kt).
 */
private val nordicCurl: ExerciseFigure by lazy {
    val padH = 6.0
    val knee = Point(0.0, -(padH + Body.THIGH_WIDTH / 2 - 0.4))
    val shinLeg = 180.0
    fun pose(lean: Double, arm: Arm?): Pose {
        // The body straight from the knee at [lean]° above level: the thigh points from the knee along it.
        val hip = knee + unit(lean) * Segment.THIGH.length
        val thigh = lean + 180.0 // hip to knee
        val torso = lean
        val leg = Leg(hip = fold180(thigh - torso - 180.0), knee = fold180(thigh - shinLeg), ankle = -50.0)
        return Pose(hip.x, hip.y, torso, nearArm = arm ?: HANG, nearLeg = leg)
    }
    val up = pose(90.0, Arm(shoulder = 25.0, elbow = 20.0, out = 55.0, forearmOut = 55.0)).let { it.copy(farArm = it.nearArm) }
    val lowBare = pose(30.0, null)
    // Fingertips on the floor: the straight arm swung down and forward until the hand's lowest point (its round) is
    // at the floor, the elbow kept slightly bent (10°: "elbows slightly bent").
    fun reaching(a: Double) = lowBare.copy(nearArm = armAt(a, lowBare.torso, elbow = 10.0)).let { it.copy(farArm = it.nearArm) }
    val down = reaching((-900..-200).map { it * 0.1 }.minBy { a ->
        val n = reaching(a).skeleton().near
        abs(maxOf(n.wrist.y, n.hand.y) + Body.HAND_WIDTH / 2)
    })
    val s0 = up.skeleton()
    val ankle = s0.near.ankle
    val roller = ankle + Point(8.0, -(Body.SHIN_WIDTH / 2 + NordicStation.ROLLER_RADIUS))
    val station = NordicStation(padFrom = ankle.x + 6.0, padTo = knee.x + 14.0, padHeight = padH, roller = roller)
    figure(
        View.SIDE, up, down, listOf(Floor, station),
        startCaption = "One line, knees to head",
        endCaption = "Fingertips touch down",
        thumbnail = Which.END,
        guideLines = listOf(
            "Kneel on a thick pad with your ankles under a fixed bar or pad.",
            "Keep your hips straight, so your body is in one line from your knees to your head.",
            "Lower your body forward by straightening your knees. Keep your hips straight.",
            "Only when your body starts to fall, touch down with your fingertips out to each side. Let your arms take the rest of the way down.",
        ),
        contacts = listOf(Contact(BodyPoint.KNEES, Surface.KNEE_PAD, BOTH), Contact(BodyPoint.SHINS, Surface.LEG_BRACE, BOTH)),
        checks = emptyList(),
    )
}

/**
 * Glute-ham raise. The glute-ham machine drawn as the back-extension bench laid level (HyperBench at 0°, Back.kt's
 * piece): the knees on the near (rear) end of the padded pad, its top at the knees' underside, the shins level behind,
 * the ankles under the roller and the soles against the foot plate. START kneeling upright (knees 90°, the body
 * straight up from the knees); END the knees straightened until the body is level with the floor, no further (the
 * guide), the hips straight, the thighs along the pad. The arms crossed on the chest. ExRx inverse leg curl
 * (https://exrx.net/WeightExercises/Hamstrings/BWHamstringRaise, Sources.kt). The pad's height (knees 95 cm up) is a
 * typical machine's, neutral.
 */
private val gluteHamRaise: ExerciseFigure by lazy {
    val knee = Point(0.0, -95.0)
    val ankle = knee + Point(-Segment.SHIN.length, 0.0)
    fun pose(lean: Double): Pose {
        val hip = knee + unit(lean) * Segment.THIGH.length
        val thigh = lean + 180.0
        val bare = Pose(hip.x, hip.y, lean, nearArm = HANG, nearLeg = Leg(hip = fold180(thigh - lean - 180.0), knee = fold180(thigh - 180.0), ankle = 0.0))
        return crossedArms(bare).let { bare.copy(nearArm = it, farArm = it) }
    }
    val pad = Point(knee.x + 10.0, knee.y + Body.THIGH_WIDTH / 2 - 0.4)
    val plate = Point(ankle.x - Body.ANKLE_HEIGHT, ankle.y + 10.0)
    val roller = ankle + Point(9.0, -(Body.SHIN_WIDTH / 2 + HyperBench.ROLLER_RADIUS))
    val bench = HyperBench(pad = pad, footPlate = plate, roller = roller, angle = 0.0)
    figure(
        View.SIDE, pose(90.0), pose(0.0), listOf(Floor, bench),
        startCaption = "Kneel upright",
        endCaption = "Level, no further",
        thumbnail = Which.END,
        guideLines = listOf(
            "Adjust the pads, then kneel upright in the machine.",
            "Put your knees on the near side of the padded hump, your ankles between the ankle pads and your feet on the platform.",
            "Cross your arms over your chest, or put your hands behind your back.",
            "Straighten your knees to lower your body until it is level with the floor.",
        ),
        contacts = listOf(
            Contact(BodyPoint.KNEES, Surface.THIGH_PAD, BOTH),
            Contact(BodyPoint.FEET, Surface.FOOT_PLATE, BOTH),
            Contact(BodyPoint.SHINS, Surface.LEG_BRACE, BOTH),
        ),
        checks = emptyList(),
    )
}

// ---- bridges ----

/** How far the shins lean back from vertical at a bridge's top (the knees a little behind the ankles): 8°, neutral. */
private const val BRIDGE_SHIN_BACK = 8.0

/**
 * A floor bridge's fixed points: the ankle at x = 0, the shoulder joint on the floor (its torso's back on it) where the
 * body is straight from the shoulders through the hips to the knees at the top with the shins 8° back from vertical
 * (the guides' straight line; "shins near vertical"). The head lies on the floor (the neck bent to the torso's tilt).
 */
private class Bridge(val ankle: Point, val shoulder: Point, val topPelvis: Point, val topTorso: Double)

private val bridgeBase: Bridge by lazy {
    val ankle = Point(0.0, -Body.ANKLE_HEIGHT)
    val knee = ankle + unit(90.0 + BRIDGE_SHIN_BACK) * Segment.SHIN.length
    val line = Segment.TORSO.length + Segment.THIGH.length
    val dy = knee.y + Body.TORSO_RADIUS
    val sh = Point(knee.x - sqrt(line * line - dy * dy), -Body.TORSO_RADIUS)
    val phi = angleTo(knee - sh)
    Bridge(ankle, sh, sh + unit(phi) * Segment.TORSO.length, phi + 180.0)
}

/** The arms along the floor at the sides, reaching towards the feet (5° down, so the hands rest on it). */
private fun floorArm(torso: Double) = armAt(-5.0, torso)

/** A bridge pose: the pelvis at [pelvis], the shoulders on the floor, the feet flat at the base's ankle. */
private fun bridgePose(pelvis: Point, near: Leg? = null): Pose {
    val b = bridgeBase
    val torso = angleTo(b.shoulder - pelvis)
    val leg = Reach.leg(pelvis, b.ankle, torso, foot = 0.0)
    val a = floorArm(torso)
    return Pose(pelvis.x, pelvis.y, torso, neck = fold180(torso - 180.0), nearArm = a, nearLeg = near ?: leg, farLeg = leg)
}

private val bridgeBottomPelvis by lazy { bridgeBase.shoulder + Point(Segment.TORSO.length, 0.0) }

private val BRIDGE_CONTACTS = listOf(
    Contact(BodyPoint.HEAD, Surface.FLOOR, BOTH),
    Contact(BodyPoint.FEET, Surface.FLOOR, BOTH),
    Contact(BodyPoint.HIPS, Surface.FLOOR, setOf(Which.START)),
)

/**
 * Glute bridge. START lying on the back, the knees bent, the feet flat, the arms along the floor; END the hips lifted
 * until the shoulders, hips and knees are in one line (no arch past it), the shins near vertical, the shoulders and head
 * still on the floor (Bridge above). ACE glute bridge
 * (https://www.acefitness.org/resources/everyone/exercise-library/49/glute-bridge/, Sources.kt).
 */
private val gluteBridge: ExerciseFigure by lazy {
    figure(
        View.SIDE, bridgePose(bridgeBottomPelvis), bridgePose(bridgeBase.topPelvis), listOf(Floor),
        startCaption = "Knees bent, feet flat",
        endCaption = "Squeeze your glutes",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie on your back with your knees bent and your feet flat, hip-width apart.",
            "Breathe out and lift your hips by squeezing your glutes. Press your heels into the floor.",
        ),
        contacts = BRIDGE_CONTACTS,
        checks = emptyList(),
    )
}

/**
 * Single-leg glute bridge. The glute bridge's planted (far) leg; the near leg straight, START lying along the floor
 * in line with the body, END kept in line with the hips as they rise (hip and knee straight: the guide), the foot
 * relaxed. The arms along the sides on the floor. ACE single-leg glute bridge
 * (https://www.acefitness.org/resources/everyone/exercise-library/145/glute-bridge-single-leg-progression/, Sources.kt).
 * The straight leg in the air sets it apart from the two-leg bridge at a glance (lesson 10).
 */
private val singleLegGluteBridge: ExerciseFigure by lazy {
    val straight = Leg(hip = 0.0, knee = 0.0, ankle = -20.0)
    figure(
        View.SIDE, bridgePose(bridgeBottomPelvis, near = straight), bridgePose(bridgeBase.topPelvis, near = straight), listOf(Floor),
        startCaption = "Other leg straight",
        endCaption = "Leg in line with hips",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie on your back with your arms along your sides.",
            "Bend one knee with that foot flat on the floor. Keep the other leg straight.",
            "Breathe out, press your planted foot into the floor and lift your hips by squeezing that glute.",
            "Keep your straight leg in line with your hips.",
        ),
        contacts = listOf(
            Contact(BodyPoint.HEAD, Surface.FLOOR, BOTH),
            Contact(BodyPoint.FAR_FOOT, Surface.FLOOR, BOTH),
            Contact(BodyPoint.HIPS, Surface.FLOOR, setOf(Which.START)),
        ),
        checks = emptyList(),
    )
}

/** Where a bar across the hips sits: over the hip crease, on the front of the hips (the torso's drawn front, 9 cm). */
private fun hipBar(s: Skeleton): Point = s.onTorso(2.0, 9.0 + Barbell.SHAFT_RADIUS + 1.0)

/**
 * Lying on the back with the hands on a load on the hips (a bar, dumbbells, a machine's pad): the arm reaches from the
 * shoulder to [grip] with the elbow out to the side (towards the camera, and a little up), as the elbows rest out on
 * the floor or the bench beside the body; a bend in the picture's plane would put the elbow through the floor. The grip
 * is 25 cm out from the middle (about shoulder-width hands: neutral). Both arms the same (the far one behind).
 */
private fun handsOn(pose: Pose, grip: Point): Pose {
    val s = pose.skeleton()
    val arm = arm3(v3(s.shoulder, Body.SHOULDER_HALF_WIDTH), v3(grip, 25.0), V3(0.0, -0.3, 1.0), pose.torso)
    return pose.copy(nearArm = arm, farArm = arm)
}

/**
 * Barbell glute bridge. The glute bridge with a barbell across the hips (over the hip crease), the hands on it with
 * the palms down. START the hips lowered as far as the bar allows: the plates (45 cm) resting on the floor, the hips
 * just off it; END the glute bridge's top. ACE hip bridge
 * (https://www.acefitness.org/resources/everyone/exercise-library/318/hip-bridge/, Sources.kt).
 */
private val barbellGluteBridge: ExerciseFigure by lazy {
    fun at(pelvis: Point): Pose {
        val bare = bridgePose(pelvis)
        return handsOn(bare, hipBar(bare.skeleton()))
    }
    // The pelvis height at which the bar's plates rest on the floor.
    val low = (0..400).map { it * 0.01 }.map { dy -> bridgeBottomPelvis + Point(0.0, -dy) }
        .minBy { p -> abs(hipBar(bridgePose(p).skeleton()).y + Barbell.PLATE_DIAMETER / 2) }
    figure(
        View.SIDE, at(low), at(bridgeBase.topPelvis), listOf(Floor, Barbell()),
        startCaption = "Bar across your hips",
        endCaption = "Hips toward the ceiling",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie on your back under the bar, so it sits across the top of your hips. Pad it if needed.",
            "Grip the bar with your palms down.",
            "Bend your knees and set your feet flat on the floor.",
            "Press your feet into the floor and lift your hips toward the ceiling.",
        ),
        contacts = BRIDGE_CONTACTS,
        checks = listOf(HeldBeside(BodyPoint.HIPS, 12.0, Which.START), HeldBeside(BodyPoint.HIPS, 12.0, Which.END)),
    )
}

// ---- thrusts ----

/**
 * A hip thrust on a bench seen end on (a 43 cm flat bench, the guide's 40–45 cm; its foot-side edge at x = 0): the
 * upper back on the bench's edge (the torso's back touching its top corner), the feet flat. END the body straight from
 * the shoulders to the knees, the torso level, the shins vertical; START the torso turned about the bench's edge,
 * [startTilt]° up from level, the hips lowered, the feet where they were. The ankle sits [footRise] cm higher on a
 * machine's platform.
 */
private class Thrust(val start: Pose, val end: Pose, val ankle: Point)

private val THRUST_EDGE = Point(0.0, -FlatBench.HEIGHT)

private fun thrustPelvis(tilt: Double): Pair<Point, Double> {
    val torso = 180.0 - tilt
    val ub = THRUST_EDGE + unit(torso - 90.0) * Body.TORSO_RADIUS
    return (ub - unit(torso) * (Segment.TORSO.length - 8.0)) to torso
}

private fun thrust(startTilt: Double, footRise: Double = 0.0, arm: (Skeleton) -> Point?): Thrust {
    val (top, topTorso) = thrustPelvis(0.0)
    val ankleY = -(Body.ANKLE_HEIGHT + footRise)
    val kneeY = ankleY - Segment.SHIN.length
    val ankle = Point(top.x + sqrt(Segment.THIGH.length * Segment.THIGH.length - (kneeY - top.y) * (kneeY - top.y)), ankleY)
    fun pose(pelvis: Point, torso: Double, neck: Double): Pose {
        val leg = Reach.leg(pelvis, ankle, torso, foot = 0.0)
        val bare = Pose(pelvis.x, pelvis.y, torso, neck = neck, nearArm = HANG, nearLeg = leg)
        val s = bare.skeleton()
        val hands = arm(s)
        return if (hands == null) bare else handsOn(bare, hands)
    }
    val (low, lowTorso) = thrustPelvis(startTilt)
    // At the top the chin is tucked, the eyes forward (neck 20°: "keep your chest down", neutral).
    return Thrust(pose(low, lowTorso, 0.0), pose(top, topTorso, 20.0), ankle)
}

/** The barbell hip thrust's start: the torso tilted until the bar's plates rest on the floor. */
private val thrustLowTilt: Double by lazy {
    (0..600).map { it * 0.1 }.minBy { t ->
        val (p, torso) = thrustPelvis(t)
        abs(hipBar(Pose(p.x, p.y, torso, nearArm = HANG, nearLeg = Leg(90.0, 90.0)).skeleton()).y + Barbell.PLATE_DIAMETER / 2)
    }
}

private val thrustBench by lazy { FlatBench(x = THRUST_EDGE.x - FlatBench.WIDTH, length = FlatBench.WIDTH) }

private val THRUST_CONTACTS = listOf(Contact(BodyPoint.UPPER_BACK, Surface.BENCH, BOTH), Contact(BodyPoint.FEET, Surface.FLOOR, BOTH))

/**
 * Hip thrust. The upper back on a 43 cm bench (seen end on), the bar across the hips over the hip crease, the hands
 * on it. START the hips lowered until the plates rest on the floor; END the hips driven up until the body is straight
 * from the shoulders to the knees (no arch past it), the shins vertical. ExRx barbell hip thrust
 * (https://exrx.net/WeightExercises/GluteusMaximus/BBHipThrust, Sources.kt).
 */
private val hipThrust: ExerciseFigure by lazy {
    val t = thrust(thrustLowTilt) { hipBar(it) }
    figure(
        View.SIDE, t.start, t.end, listOf(Floor, thrustBench, Barbell()),
        startCaption = "Upper back on the bench",
        endCaption = "Body straight",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit on the floor with your upper back against a bench about 40 to 45 cm high.",
            "Roll the bar over your hips so it sits across the top of your hips and lower belly. Pad it if needed.",
            "Plant your feet flat, about shoulder-width apart, with your knees bent.",
            "Drive through your feet and lift your hips until your body is straight.",
        ),
        contacts = THRUST_CONTACTS,
        checks = listOf(HeldBeside(BodyPoint.HIPS, 12.0, Which.START), HeldBeside(BodyPoint.HIPS, 12.0, Which.END)),
    )
}

/** A dumbbell resting on the hips: its disc (end on, the handle across the body) on the front of the hips. */
private fun hipDumbbell(s: Skeleton): Point = s.onTorso(2.0, 9.0 + Dumbbell.HEAD_RADIUS + 0.3)

/**
 * DB hip thrust. The hip thrust's poses with a dumbbell in each hand resting on the hips (seen end on, a disc: the
 * handles across the body). START the tailbone lowered towards the floor (the hip thrust's bottom), END the body
 * straight. ACE elevated glute bridge
 * (https://www.acefitness.org/resources/everyone/exercise-library/367/elevated-glute-bridge/, Sources.kt).
 */
private val dbHipThrust: ExerciseFigure by lazy {
    val t = thrust(thrustLowTilt) { hipDumbbell(it) }
    figure(
        View.SIDE, t.start, t.end, listOf(Floor, thrustBench, Dumbbell(Side.FAR), Dumbbell(Side.NEAR)),
        startCaption = "Tailbone toward floor",
        endCaption = "Body straight",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie across a flat bench with your upper back and shoulders on it.",
            "Hold a dumbbell in each hand and rest them on your hips.",
            "Lower your tailbone toward the floor, keeping the weights on your hips.",
            "Press your feet into the floor and squeeze your glutes to lift your hips until your body is straight.",
        ),
        contacts = THRUST_CONTACTS,
        checks = listOf(HeldBeside(BodyPoint.HIPS, 21.0, Which.START), HeldBeside(BodyPoint.HIPS, 21.0, Which.END)),
    )
}

/**
 * Machine hip thrust. The hip thrust's body on a plate-loaded machine (KitHamstrings.kt): the upper back on its back
 * pad, the feet on its 5 cm platform, the padded roller over the front of the hips on a lever whose pivot is set so
 * the lever is the same length in both panels, the hands on the roller. START lowered (the hip thrust's bottom tilt),
 * END the hips straightened until the body is straight. ExRx lever hip thrust
 * (https://exrx.net/WeightExercises/GluteusMaximus/LVHipThrustPL, Sources.kt).
 */
private val machineHipThrust: ExerciseFigure by lazy {
    fun pad(s: Skeleton) = s.onTorso(1.0, 9.0 + HipThrustMachine.PAD_RADIUS + 0.5)
    val t = thrust(thrustLowTilt, footRise = HipThrustMachine.PLATFORM_THICKNESS) { s -> pad(s) + unit(s.torso - 90.0) * (HipThrustMachine.PAD_RADIUS + Body.HAND_WIDTH / 2) }
    val a = pad(t.start.skeleton())
    val b = pad(t.end.skeleton())
    // The pivot on the two pads' perpendicular bisector, out at the feet's end, so the lever's length is the same.
    val m = (a + b) * 0.5
    val nrm = norm(Point(-(b - a).y, (b - a).x))
    val pivot = m + nrm * ((t.ankle.x + 18.0 - m.x) / nrm.x)
    val platform = Point(t.ankle.x + 8.0, -HipThrustMachine.PLATFORM_THICKNESS)
    fun machine(pad: Point) = HipThrustMachine(benchEdge = THRUST_EDGE.x, benchHeight = FlatBench.HEIGHT, platform = platform, pivot = pivot, pad = pad)
    figure(
        View.SIDE, t.start, t.end, listOf(Floor, machine(a)),
        startCaption = "Pad snug over your hips",
        endCaption = "Body straight",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie on the machine with your knees bent and your feet on the platform, shoulder-width apart or a little narrower.",
            "Set your hips just below the padded bar or belt. Lock or tighten it snugly over your hips.",
            "Lift the weight by straightening your hips until your body is straight.",
        ),
        contacts = listOf(
            Contact(BodyPoint.UPPER_BACK, Surface.BENCH, BOTH),
            Contact(BodyPoint.FEET, Surface.FOOT_PLATE, BOTH),
            Contact(BodyPoint.HIPS, Surface.HIP_PAD, BOTH),
        ),
        checks = emptyList(),
        endProps = listOf(Floor, machine(b)),
    )
}

// ---- hip extensions ----

/**
 * Reverse hyper. Face down on a 95 cm bench, the torso and waist on the pad (the hips 8 cm past its edge, so the hanging
 * thighs clear it), the hands on the handles under the head end. START the straight legs hanging straight down, the
 * feet clear of the floor; END the legs raised by straightening the hips until they are nearly in line with the body
 * (6° short of it, the knees straight). ExRx reverse hyper-extension: the hips extended as high as possible with the
 * legs straight (https://exrx.net/WeightExercises/GluteusMaximus/BWReverseHyperextension, Sources.kt); the 6° is
 * neutral, the guide's "nearly in line".
 */
private val reverseHyper: ExerciseFigure by lazy {
    val height = 95.0
    val pelvis = Point(0.0, -(height + Body.TORSO_RADIUS))
    val bare = Pose(pelvis.x, pelvis.y, 0.0, neck = 10.0, nearArm = HANG, nearLeg = Leg(90.0, 0.0)).skeleton()
    // The handles forward of the head end and low, so the arms reach down to them nearly straight.
    val handle = Point(bare.shoulder.x + 18.0, -height + 47.0)
    val arm = armTo(bare.shoulder, handle, 0.0)
    fun pose(hip: Double) = Pose(pelvis.x, pelvis.y, 0.0, neck = 10.0, nearArm = arm, nearLeg = Leg(hip, 0.0, ankle = 0.0))
    figure(
        View.SIDE, pose(90.0), pose(6.0), listOf(Floor, ReverseHyperBench(edge = pelvis.x + 8.0, height = height, handle = handle)),
        startCaption = "Legs hang straight",
        endCaption = "Nearly in line",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie face down with your torso and waist on the bench, and grip the handles.",
            "Let your legs hang straight, with your feet off the floor.",
            "Raise your legs by straightening your hips until your legs are nearly in line with your body.",
        ),
        contacts = listOf(Contact(BodyPoint.WAIST, Surface.BENCH, BOTH), Contact(BodyPoint.HANDS, Surface.HANDLE, BOTH)),
        checks = emptyList(),
    )
}

/**
 * Quadruped hip extension. On all fours, the head to the right: the knees on the floor under the hips (thighs
 * vertical), the arms straight down under the shoulders, the hands on the floor, the fingers forward (the wrist bent
 * [PUSH_UP_WRIST]°, the heel of the palm a little raised: Chest.kt's push-up hand). With straight arms the figure's arm
 * and thigh lengths and that hand tilt the straight back up towards the head by about 21°, which reads as sagging hips;
 * so the elbows are softened (bent backwards towards the knees, the least bend that does it, about 30°, neutral) until
 * the back slopes 15° at most, the wrists still under the shoulders. START both knees down; END the near leg lifted
 * from the hip, the knee kept bent at 90°, until the thigh is in line with the back (hip straight, not past it: the
 * guide's "move only at the hip", without arching). ACE quadruped bent-knee hip extensions
 * (https://www.acefitness.org/resources/everyone/exercise-library/270/quadruped-bent-knee-hip-extensions/, Sources.kt).
 */
/** The quadruped's most back slope up towards the head, degrees (the fix round's ruling: 10–15°). */
private const val QUADRUPED_SLOPE = 15.0

private val quadrupedHipExtension: ExerciseFigure by lazy {
    val kneeY = -(Body.THIGH_WIDTH / 2 - 0.25)
    val hipP = Point(0.0, kneeY - Segment.THIGH.length)
    // The kneeling leg: the thigh straight down, the lower leg back along the floor 8° up (as the knee push-up's).
    fun kneel(torso: Double) = Leg(hip = fold180(270.0 - torso - 180.0), knee = 270.0 - 172.0, ankle = -50.0)
    // The upper arm's screen angle that puts the wrist straight under the shoulder for an elbow bent [elbow]°
    // (bisection: the wrist's offset falls as the upper arm turns forward).
    fun upperFor(elbow: Double): Double = solveAngle(240.0, 270.0) { u ->
        Segment.UPPER_ARM.length * cos(u / DEG) + Segment.FOREARM.length * cos((u + elbow) / DEG)
    }
    fun handsDown(torso: Double, elbow: Double, upper: Double): Pose {
        val arm = Arm(shoulder = fold180(upper - torso - 180.0), elbow = elbow, wrist = PUSH_UP_WRIST)
        return Pose(hipP.x, hipP.y, torso, nearArm = arm, nearLeg = kneel(torso))
    }
    // The torso tilt that sets the hand's lowest point on the floor (bisection: the hand rises as the torso tilts up).
    fun tilt(elbow: Double): Double {
        val upper = upperFor(elbow)
        return solveAngle(0.0, 40.0) { t ->
            val n = handsDown(t, elbow, upper).skeleton().near
            -(maxOf(n.wrist.y, n.hand.y) + Body.HAND_WIDTH / 2)
        }
    }
    // The least elbow bend (whole degrees) that brings the back's slope down to 15°.
    val elbow = (0..45).map { it.toDouble() }.first { tilt(it) <= QUADRUPED_SLOPE }
    val start = handsDown(tilt(elbow), elbow, upperFor(elbow))
    val lifted = Leg(hip = 0.0, knee = 90.0, ankle = 0.0)
    figure(
        View.SIDE, start, start.copy(nearLeg = lifted), listOf(Floor),
        startCaption = "Knees under your hips",
        endCaption = "Lift, knee bent",
        thumbnail = Which.END,
        guideLines = listOf(
            "Kneel on all fours with your hands under your shoulders and your knees under your hips.",
            "Squeeze the glute on one side to lift that leg. Keep the knee bent and move only at the hip.",
        ),
        contacts = listOf(
            Contact(BodyPoint.HANDS, Surface.FLOOR, BOTH),
            Contact(BodyPoint.FAR_KNEE, Surface.FLOOR, BOTH),
            Contact(BodyPoint.NEAR_KNEE, Surface.FLOOR, setOf(Which.START)),
        ),
        checks = emptyList(),
    )
}

// ---- abduction and adduction (front view) ----

/** The seated machines' thighs: 5° down from level (neutral), so the shins hang to the foot bars. */
private const val SEATED_DROOP = 5.0

/**
 * A seated leg seen from the front: the thigh pointing forward, [apart]° out from straight ahead (the hip's abduction,
 * in the seat's level plane), 5° down; the shin straight down from the knee. The thigh is drawn as its projection
 * (Leg.out), and its drawn sideways angle is the drawn knee bend too, so the shin hangs vertically.
 */
private fun seatedLeg(apart: Double): Leg {
    val sx = sin(apart / DEG) * cos(SEATED_DROOP / DEG)
    val dy = sin(SEATED_DROOP / DEG)
    val hip = atan2(sx, dy) * DEG
    return Leg(hip = hip, knee = hip, out = acos(sqrt(sx * sx + dy * dy)) * DEG)
}

private const val HIP_SEAT = 50.0

/** Seated in the hip machine: the hips on the seat, the arms down to the handles at its sides (12° out: neutral). */
private fun hipMachinePose(apart: Double): Pose {
    val arm = Arm(shoulder = 12.0, elbow = 0.0)
    return Pose(0.0, -(HIP_SEAT + Body.TORSO_RADIUS), 90.0, nearArm = arm, nearLeg = seatedLeg(apart))
}

/** The hip machines' handles: where the hanging hands grip, at the seat's sides (the arms are the same in both panels). */
private val hipMachineHandle: Point by lazy { hipMachinePose(0.0).skeleton(View.FRONT).near.grip }

private val HIP_MACHINE_CONTACTS = listOf(Contact(BodyPoint.HIPS, Surface.SEAT, BOTH), Contact(BodyPoint.HANDS, Surface.HANDLE, BOTH))

/** The machines' wide end: the thighs 30° out from straight ahead (neutral; the 40° abduction limit's conservative side). */
private const val MACHINE_APART = 30.0

/**
 * Machine hip abduction. Front view, seated, the back against the pad, the knees inside the side pads (on their
 * outsides), the heels on the foot bars, the hands on the handles. START the pads together (the knees hip-width, the
 * thighs straight ahead); END the legs pushed apart, the thighs 30° out (neutral, inside the 40° limit). ExRx lever
 * seated hip abduction (https://exrx.net/WeightExercises/HipAbductor/LVSeatedHipAbduction, Sources.kt).
 */
private val machineHipAbduction: ExerciseFigure by lazy {
    val machine = HipMachine(x = 0.0, seatHeight = HIP_SEAT, padTop = 128.0, padsOutside = true, handle = hipMachineHandle)
    figure(
        View.FRONT, hipMachinePose(0.0), hipMachinePose(MACHINE_APART), listOf(Floor, machine),
        startCaption = "Legs inside the pads",
        endCaption = "Push your legs apart",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit with your legs inside the side pads. Put your heels on the foot bars if there are any.",
            "Sit back against the back pad and grip the handles.",
            "Push your legs apart.",
        ),
        contacts = HIP_MACHINE_CONTACTS,
        checks = emptyList(),
    )
}

/**
 * Machine hip adduction. The abduction machine's seat with the pads on the insides of the knees (the legs outside the
 * pads). START at the set width, the thighs 30° out; END squeezed together until the two pads meet (the thighs 6° out:
 * the pads' thickness between the knees). ExRx lever seated hip adduction
 * (https://exrx.net/WeightExercises/HipAdductors/LVSeatedHipAdduction, Sources.kt).
 */
private val machineHipAdduction: ExerciseFigure by lazy {
    val machine = HipMachine(x = 0.0, seatHeight = HIP_SEAT, padTop = 128.0, padsOutside = false, handle = hipMachineHandle)
    // The knees as close as the two pads between them allow.
    val together = (0..300).map { it * 0.1 }.first { a ->
        hipMachinePose(a).skeleton(View.FRONT).near.knee.x >= Body.SHIN_WIDTH / 2 + 2 * HipMachine.KNEE_PAD_RADIUS
    }
    figure(
        View.FRONT, hipMachinePose(MACHINE_APART), hipMachinePose(together), listOf(Floor, machine),
        startCaption = "Legs outside the pads",
        endCaption = "Squeeze legs together",
        // The legs together: apart from the abduction machine's thumbnail (legs apart) at a glance (lesson 10).
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit with your legs outside the pads.",
            "Set the lever to your starting width, then lock it.",
            "Squeeze your legs together.",
        ),
        contacts = HIP_MACHINE_CONTACTS,
        checks = emptyList(),
    )
}

/**
 * Standing with the hips and knees slightly bent, seen from the front: each leg straight in the picture, [apart]° out
 * from vertical, the thigh 15° forward towards the camera and the shin 15° back (a slight knee bend), the torso leaning
 * 15° forward; lowered until the feet are on the floor. Arms hanging 10° out.
 */
private fun bentStance(apart: Double): Pose {
    val leg = Leg(hip = apart, knee = 0.0, out = 15.0, shinOut = -15.0)
    val raw = Pose(0.0, 0.0, 90.0, nearArm = Arm(10.0, 0.0), nearLeg = leg, lean = 15.0)
    val n = raw.skeleton(View.FRONT).near
    val bottom = maxOf(n.heel.y, n.toe.y) + Body.FOOT_WIDTH / 2
    return raw.copy(pelvisY = -bottom)
}

/**
 * Lateral band walk. Front view, a mini band just above the knees (KitHamstrings.kt), the hips and knees slightly bent
 * (neutral: 15°), the torso leaning a little forward. START the feet about hip-width (the legs 3° out); END a step to
 * the side, the feet wide (each leg 12° out: neutral), the band stretched. ACE joint-friendly lower-extremity exercises
 * (https://www.acefitness.org/resources/pros/expert-articles/9158/ankles-knees-and-hips-10-joint-friendly-exercises-for-the-lower-extremity/,
 * Sources.kt).
 */
private val lateralBandWalk: ExerciseFigure by lazy {
    figure(
        View.FRONT, bentStance(3.0), bentStance(12.0), listOf(Floor, MiniBand(BandAt.KNEES)),
        startCaption = "Feet about hip-width",
        endCaption = "Step to the side",
        thumbnail = Which.END,
        guideLines = listOf(
            "Put a small band around your ankles or just above your knees.",
            "Stand with your feet about hip-width apart and your hips and knees slightly bent.",
            "Step to the side, keeping tension on the band.",
        ),
        contacts = FEET_DOWN,
        checks = emptyList(),
    )
}

/**
 * Cable hip abduction. Front view, side-on to a low pulley on the lifter's right as drawn (the picture's left), the
 * cuff on the far-from-the-pulley ankle (the near leg, on the right). Standing on the leg nearest the pulley, the hand
 * on that side out to the stack's frame, the other arm hanging. START the cuffed leg crossed in front of the standing
 * leg (12° in, 10° forward, the foot just off the floor); END lifted straight out to the side, the knee straight and
 * pointing forward, 30° out (neutral: ACE's side-lying abduction stops when the hips tilt, about 45° being the hip's
 * own limit, https://www.acefitness.org/resources/everyone/exercise-library/38/side-lying-hip-abduction/, Sources.kt;
 * the conservative side of it, inside the figure's 40° limit), the pelvis level. ExRx cable hip abduction
 * (https://exrx.net/WeightExercises/HipAbductor/CBHipAbduction, Sources.kt). The cable runs from the pulley to the cuff
 * in both panels (lesson 3).
 */
private val cableHipAbduction: ExerciseFigure by lazy {
    val stand = Leg(0.0, 0.0)
    val pelvisY = -(Body.ANKLE_HEIGHT + Segment.SHIN.length + Segment.THIGH.length)
    val far = Arm(shoulder = 62.0, elbow = 10.0)
    fun pose(near: Leg) = Pose(0.0, pelvisY, 90.0, nearArm = Arm(10.0, 0.0), nearLeg = near, farArm = far, farLeg = stand)
    val start = pose(Leg(hip = -12.0, knee = 0.0, out = 10.0, shinOut = 10.0))
    val end = pose(Leg(hip = 30.0, knee = 0.0))
    val grip = start.skeleton(View.FRONT).far.grip
    val column = grip.x - 9.0 - Body.HAND_WIDTH / 2
    val stack = CableStack(x = column, pulleyHeight = 12.0, reach = 12.0, cableInFront = true)
    figure(
        View.FRONT, start, end, listOf(Floor, stack, AnkleStrap(Side.NEAR)),
        startCaption = "Leg crossed in front",
        endCaption = "Lift it out to the side",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand side-on to a low pulley and attach the cuff to your far ankle.",
            "Balance on the leg nearest the pulley, with your cuffed leg crossed in front of it.",
            "Lift your cuffed leg out to the side, away from the pulley.",
            "Stop when your hips start to tilt.",
        ),
        contacts = listOf(Contact(BodyPoint.FAR_FOOT, Surface.FLOOR, BOTH)),
        checks = emptyList(),
    )
}

// ---- side-lying (front view) ----

/** Lying on the side along the floor, head to the left: Side-lying external rotation's torso (Shoulders.kt). */
private const val SIDE_TORSO = 174.0
private val SIDE_PELVIS = Point(0.0, -21.0)

/** The bottom arm reaching along the floor under the head; the top hand on the floor in front of the chest. */
private val UNDER_HEAD = Arm(shoulder = 170.0, elbow = 0.0)
private val TOP_HAND_DOWN = Arm(shoulder = -90.0, elbow = 0.0, out = 60.0, forearmOut = 60.0)

private fun sideLying(top: Leg, bottom: Leg) =
    Pose(SIDE_PELVIS.x, SIDE_PELVIS.y, SIDE_TORSO, neck = -6.0, nearArm = TOP_HAND_DOWN, nearLeg = top, farArm = UNDER_HEAD, farLeg = bottom)

/** The bottom leg's hip angle that rests its lowest point (the ankle's or the foot's round) on the floor, for a leg [shape]d as given. */
private fun bottomLeg(shape: (Double) -> Leg): Leg {
    val hip = (-200..200).map { it * 0.1 }.minBy { h ->
        val f = sideLying(Leg(0.0, 0.0), shape(h)).skeleton(View.FRONT).far
        abs(maxOf(f.ankle.y + Body.SHIN_WIDTH / 2, f.heel.y + Body.FOOT_WIDTH / 2, f.toe.y + Body.FOOT_WIDTH / 2))
    }
    return shape(hip)
}

/** The top leg's hip angle that rests it on the bottom leg: its ankle a shin's width above the bottom one. */
private fun restingTop(bottom: Leg, shape: (Double) -> Leg): Leg {
    val hip = (-200..100).map { it * 0.1 }.minBy { h ->
        val s = sideLying(shape(h), bottom).skeleton(View.FRONT)
        abs((s.far.ankle.y - s.near.ankle.y) - Body.SHIN_WIDTH)
    }
    return shape(hip)
}

/**
 * Side-lying hip abduction. Front view of a body lying on its side along the floor (the head to the left, resting on
 * the bottom arm, in line with the spine; the top hand on the floor in front of the chest), the legs straight, stacked.
 * START the top leg resting on the bottom one; END lifted with the knee straight and pointing forward, 30° from the
 * bottom leg's line (neutral: ACE's own stop is when the hips start to tilt, the hip abducting to about 45° at most,
 * https://www.acefitness.org/resources/everyone/exercise-library/38/side-lying-hip-abduction/, Sources.kt), the hips
 * stacked (the torso does not turn).
 */
private val sideLyingHipAbduction: ExerciseFigure by lazy {
    val bottom = bottomLeg { Leg(it, 0.0) }
    val rest = restingTop(bottom) { Leg(it, 0.0) }
    figure(
        View.FRONT, sideLying(rest, bottom), sideLying(Leg(rest.hip + 30.0, 0.0), bottom), listOf(Floor),
        startCaption = "Legs straight, stacked",
        endCaption = "Lift your top leg",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie on your side with your legs straight.",
            "Stack your hips and shoulders, with your head in line with your spine.",
            "Breathe out and lift your top leg with the knee straight and pointing forward.",
            "Stop when your hips start to tilt.",
        ),
        contacts = listOf(Contact(BodyPoint.FAR_FOOT, Surface.FLOOR, BOTH)),
        checks = emptyList(),
    )
}

/**
 * Clamshell. The side-lying body above with the hips bent 45° (the clam variants' hip angle in the systematic review
 * https://pmc.ncbi.nlm.nih.gov/articles/PMC4595911/, seen in its search result) and the knees 90° (neutral), the feet
 * together, a mini band just above the knees. Seen from the front of the body, the thighs point towards the camera and the shins back to the
 * feet, so a stacked leg reads as one foreshortened line. START the knees together; END the top knee lifted (the thigh
 * turned 40° about the line from the hip to the feet, neutral: short of the pelvis rolling back), the feet still
 * together, the band stretched.
 */
private val clamshell: ExerciseFigure by lazy {
    fun bent(h: Double) = Leg(hip = h, knee = 0.0, out = 45.0, shinOut = -45.0)
    val bottom = bottomLeg(::bent)
    val rest = restingTop(bottom, ::bent)
    val s0 = sideLying(rest, bottom).skeleton(View.FRONT)
    // Turn the top thigh about the hip-to-foot line: the knee rises in the picture, out of it towards the camera less.
    val h = s0.near.hip
    val f = s0.near.ankle
    val u = norm(f - h)
    val n = unit(angleTo(u) + 90.0)
    val k0 = s0.near.knee - h
    val along = k0.x * u.x + k0.y * u.y
    val p = k0.x * n.x + k0.y * n.y
    val z = Segment.THIGH.length * sin(45.0 / DEG)
    val rho = 40.0 / DEG
    val p1 = p * cos(rho) + z * sin(rho)
    val z1 = z * cos(rho) - p * sin(rho)
    val knee = h + u * along + n * p1
    val thighAngle = angleTo(knee - h)
    val shinAngle = angleTo(f - knee)
    val open = Leg(
        hip = fold180(thighAngle - (SIDE_TORSO + 180.0)),
        knee = fold180(thighAngle - shinAngle),
        out = acos(((knee - h).length() / Segment.THIGH.length).coerceAtMost(1.0)) * DEG,
        shinOut = -asin((z1 / Segment.SHIN.length).coerceIn(-1.0, 1.0)) * DEG,
    )
    figure(
        View.FRONT, sideLying(rest, bottom), sideLying(open, bottom), listOf(Floor, MiniBand(BandAt.KNEES)),
        startCaption = "Feet together",
        endCaption = "Lift your top knee",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie on your side with your hips stacked and both knees bent.",
            "Keep your feet together.",
            "For more resistance, put a mini band just above your knees.",
            "Squeeze your glutes and lift your top knee, keeping your feet together.",
        ),
        contacts = listOf(Contact(BodyPoint.FAR_FOOT, Surface.FLOOR, BOTH)),
        checks = emptyList(),
    )
}

/** The batch's figures by catalogue name, each built on first use (Figures.kt's laziness rule). */
private val hamstringGluteBatchFigures: Map<String, Lazy<ExerciseFigure>> by lazy {
    mapOf(
        "Hip thrust" to lazy { hipThrust },
        "Stiff-leg deadlift" to lazy { stiffLegDeadlift },
        "DB RDL" to lazy { dbRdl },
        "Single-leg RDL" to lazy { singleLegRdl },
        "Good morning" to lazy { goodMorning },
        "Lying leg curl" to lazy { lyingLegCurl },
        "Seated leg curl" to lazy { seatedLegCurl },
        "Standing leg curl" to lazy { standingLegCurl },
        "Cable leg curl" to lazy { cableLegCurl },
        "Stability ball leg curl" to lazy { stabilityBallLegCurl },
        "Nordic curl" to lazy { nordicCurl },
        "Glute-ham raise" to lazy { gluteHamRaise },
        "Glute bridge" to lazy { gluteBridge },
        "Single-leg glute bridge" to lazy { singleLegGluteBridge },
        "Barbell glute bridge" to lazy { barbellGluteBridge },
        "DB hip thrust" to lazy { dbHipThrust },
        "Machine hip thrust" to lazy { machineHipThrust },
        "Kettlebell swing" to lazy { kettlebellSwing },
        "Single-arm kettlebell swing" to lazy { singleArmKettlebellSwing },
        "Reverse hyper" to lazy { reverseHyper },
        "Quadruped hip extension" to lazy { quadrupedHipExtension },
        "Machine hip abduction" to lazy { machineHipAbduction },
        "Cable hip abduction" to lazy { cableHipAbduction },
        "Side-lying hip abduction" to lazy { sideLyingHipAbduction },
        "Clamshell" to lazy { clamshell },
        "Lateral band walk" to lazy { lateralBandWalk },
        "Machine hip adduction" to lazy { machineHipAdduction },
    )
}

internal val hamstringGluteFigures: Map<String, Lazy<ExerciseFigure>> by lazy {
    mapOf("RDL" to lazy { rdl }) + hamstringGluteBatchFigures
}
