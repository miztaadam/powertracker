package com.powertracker.domain.library.figures.areas

import com.powertracker.domain.library.areas.RACK_SAFETY
import com.powertracker.domain.library.figures.*
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Figures for the library's Quads area (library/areas/Quads.kt), keyed by catalogue name.
//
// Camera (Task 10): the side view for the squats, the forward and reverse lunges, the split squats, the step-up, the leg
// extensions, the leg presses and the sled push: the hips and knees bend in the sagittal plane, and what each guide is
// about (the depth, where the load is, the front shin and the back knee, the box, the pad on the shin, the lean into
// the sled) shows from the side. Three exercises move across the camera, so a side view cannot show them (they get the
// front view):
// - Lateral lunge: the step is straight out to the side.
// - Curtsy lunge: the back leg crosses behind the front leg, to the other side.
// - Lateral step-up: the box is beside the lifter, and the step up onto it is sideways.
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

// Guide lines shared by several quad guides (library/areas/Quads.kt), verbatim.
private const val HIPS_BACK = "Push your hips back and let your knees bend forward. Keep your back straight."
private const val SQUAT_DEPTH = "Go down until your thighs are just past level with the floor. Stop sooner if your heels lift or your back starts to round."
private const val STAND_UP = "Stand up by straightening your knees and hips."
private const val DBS_AT_SIDES = "Hold a dumbbell in each hand at your sides."
private const val LUNGE_DEPTH = "Lower until your front thigh is about level with the floor and your back knee is just above it. Don't let your knee touch the floor."
private const val LUNGE_STEP = "Step forward with one leg. Land on your heel, then your forefoot."
private const val BAR_ON_UPPER_BACK = "Step under the bar and rest it across your upper back."
private const val SMITH_SAFETY = "Set the safety stops just below your lowest position."
private const val BOX_HEIGHT = "Use a sturdy box or bench no higher than your knee."

private const val JUST_PAST = "Thighs just past level"
private const val THIGH_LEVEL = "Front thigh about level"
private const val KNEE_NEAR = "Back knee near the floor"

/**
 * A dumbbell held at the side, palms in (as "at your sides" hangs naturally, and as the DB shrug draws it): its handle
 * runs front to back, so the side view sees it in profile, square to the hanging hand (Kit.kt Dumbbell.squareToForearm),
 * and the front view sees it end-on (Dumbbell.endOnFront).
 */
private fun atSide(side: Side, view: View = View.SIDE) =
    if (view == View.SIDE) Dumbbell(side, neutral = true, squareToForearm = true) else Dumbbell(side, endOnFront = true)

// ---- squats ----

private val tall by lazy { standingLeg(0.0, shinLean = 0.0, knee = 0.0, torso = 90.0) }

/** A straight arm pointing at the screen angle [world] whatever the torso's lean (straight down is 270). */
private fun armAt(world: Double, torso: Double) = Arm(shoulder = fold180(world - torso - 180.0), elbow = 0.0)

/**
 * The squat bottom's leg: the shins [lean]° forward (inside the 35° ankle limit, Body.kt), the hip [below] cm under
 * the knee (0: thighs level, the guides' "about level"; 2.2: "just past level", the Squat's depth, MainLifts.kt).
 */
private fun squatLeg(lean: Double, below: Double, torso: Double = 60.0) = standingLeg(0.0, lean, kneeForDepth(lean, below), torso)

/** The same leg, for a torso at [torso]°. */
private fun StandingLeg.at(torso: Double) = standingLeg(ankle.x, leg.ankle, leg.knee, torso)

/**
 * A light load hangs or is held close to the body, so the figure balances its upper body: the shoulders over the middle
 * of the foot (the bar-over-midfoot line of the barbell lifts, MainLifts.kt, applied to the shoulders, which carry the
 * hanging dumbbells and most of the upper body's weight). A neutral balance rule; it puts the torso about 57° above
 * level at the squat's depth, near the high-bar squat's 44–50° and more upright, as a load at the sides or in front
 * allows (ACE goblet squat: the weight in front helps you stay upright,
 * https://www.acefitness.org/resources/everyone/exercise-library/362/goblet-squat/).
 */
private fun shouldersOverFeet(leg: StandingLeg, range: ClosedFloatingPointRange<Double> = 30.0..90.0) =
    balancedTorso(leg, range, ::shoulderOf)

/**
 * Squat depth for the dumbbell, goblet, kettlebell and bodyweight squats: the shins 33° forward as in the approved
 * Squat (MainLifts.kt), so the knees travel forward over the toes ("let your knees bend forward"), and the hips
 * [below] cm under the knees.
 */
private const val SQUAT_SHIN = 33.0

/**
 * Bodyweight squat. START standing tall, the chest up, the arms down; END hips back and down until the thighs are
 * about level with the floor (the hip joint level with the knee's, HipDepth 0), the arms reaching straight forward,
 * level: ACE's arms forward to offset the hips going back (ACE 5 variations of the body-weight squat,
 * https://www.acefitness.org/resources/pros/expert-articles/5875/5-variations-of-the-body-weight-squat/; also ACE's
 * squat myths article, the arms as a counterweight). The torso by the shoulders-over-feet rule.
 */
private val bodyweightSquat: ExerciseFigure by lazy {
    val leg0 = squatLeg(SQUAT_SHIN, 0.5)
    val torso = shouldersOverFeet(leg0)
    val end = standOn(leg0.at(torso), torso, armAt(0.0, torso))
    figure(
        View.SIDE, standOn(tall, 90.0, HANG), end, listOf(Floor),
        startCaption = "Lift your chest",
        endCaption = "Thighs about level",
        thumbnail = Which.END,
        guideLines = listOf(
            "Brace your trunk and lift your chest.",
            "Push your hips back, then down, so your hips and knees bend together. Keep your back as flat as you can.",
            "Go down until your thighs are about level with the floor. Stop sooner if your heels start to lift or your back starts to round.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(HipDepth(belowKnee = 0.0, at = Which.END)),
    )
}

/**
 * Goblet squat. One dumbbell held upright against the chest with both hands (its profile along the torso, Kit.kt
 * Dumbbell.neutral), the hands under its top head, the elbows down by the ribs: the dumbbell's centre 16 cm below the
 * shoulder joint and 23 cm in front of the torso's axis, so its heads (20 cm across) touch the chest (neutral). START
 * standing tall; END thighs just past level (the hip 2.2 cm under the knee, HipDepth 2), the torso by the
 * shoulders-over-feet rule.
 */
private val gobletSquat: ExerciseFigure by lazy {
    fun pose(leg: StandingLeg, torso: Double): Pose {
        val sh = shoulderOf(leg.hip, torso)
        val dumbbell = leg.hip + unit(torso) * (Segment.TORSO.length - 16.0) + unit(torso - 90.0) * 23.0
        return standOn(leg, torso, armTo(sh, dumbbell, torso))
    }
    val leg0 = squatLeg(SQUAT_SHIN, 2.2)
    val torso = shouldersOverFeet(leg0)
    figure(
        View.SIDE, pose(tall, 90.0), pose(leg0.at(torso), torso), listOf(Floor, Dumbbell(Side.NEAR, neutral = true)),
        startCaption = "Dumbbell against chest",
        endCaption = JUST_PAST,
        thumbnail = Which.END,
        guideLines = listOf(
            "Hold one dumbbell upright against your chest with both hands, or a kettlebell by the sides of its handle.",
            "Keep your elbows close to your ribs.",
            HIPS_BACK,
            SQUAT_DEPTH,
        ),
        contacts = FEET_DOWN,
        checks = listOf(HipDepth(belowKnee = 2.0, at = Which.END)),
    )
}

/**
 * DB squat. A dumbbell in each hand at the sides, the arms hanging straight down (palms in: the dumbbells in profile,
 * [atSide]); START standing tall, END thighs just past level, the torso by the shoulders-over-feet rule, which also
 * keeps the dumbbells hanging over the feet (ExRx dumbbell squat, https://exrx.net/WeightExercises/Quadriceps/DBSquat:
 * the dumbbells at the sides, the arms straight).
 */
private val dbSquat: ExerciseFigure by lazy {
    val leg0 = squatLeg(SQUAT_SHIN, 2.2)
    val torso = shouldersOverFeet(leg0)
    val down = armAt(270.0, torso)
    figure(
        View.SIDE, standOn(tall, 90.0, HANG), standOn(leg0.at(torso), torso, down), listOf(Floor, atSide(Side.FAR), atSide(Side.NEAR)),
        startCaption = "Dumbbells at your sides",
        endCaption = JUST_PAST,
        thumbnail = Which.END,
        guideLines = listOf(DBS_AT_SIDES, HIPS_BACK, SQUAT_DEPTH),
        contacts = FEET_DOWN,
        checks = listOf(HipDepth(belowKnee = 2.0, at = Which.END)),
    )
}

/**
 * Kettlebell front squat. A kettlebell racked in front of each shoulder, the bell on the back of the forearm (Kit
 * KettlebellHold.RACK), the arms close to the body: the upper arm 15° forward of the torso, the elbow bent 145°, the
 * wrist straight (the approved Kettlebell press's rack, Shoulders.kt). START standing tall, END thighs just past level,
 * the torso by the shoulders-over-feet rule (the bells are at the shoulders). ExRx kettlebell front squat
 * (https://exrx.net/WeightExercises/Kettlebell/KBFrontSquat).
 */
private val kettlebellFrontSquat: ExerciseFigure by lazy {
    val rack = Arm(shoulder = 15.0, elbow = 145.0)
    val leg0 = squatLeg(SQUAT_SHIN, 2.2)
    val torso = shouldersOverFeet(leg0)
    figure(
        View.SIDE, standOn(tall, 90.0, rack), standOn(leg0.at(torso), torso, rack),
        listOf(Floor, Kettlebell(Side.FAR, KettlebellHold.RACK), Kettlebell(Side.NEAR, KettlebellHold.RACK)),
        startCaption = "Kettlebells at shoulders",
        endCaption = JUST_PAST,
        thumbnail = Which.END,
        guideLines = listOf(
            "Start with a kettlebell in front of each shoulder, arms close to your body and wrists straight.",
            HIPS_BACK,
            SQUAT_DEPTH,
        ),
        contacts = FEET_DOWN,
        checks = listOf(HipDepth(belowKnee = 2.0, at = Which.END)),
    )
}

/**
 * The Zercher arms: the upper arm 25° forward of the torso's line, the elbow bent 140°, so the forearms rise in front
 * of the chest to the crossed wrists and the bar sits in the crook of the elbows (Kit.kt BarHold.CROOK) against the
 * body at the upper waist (neutral angles).
 */
private val ZERCHER_ARM = Arm(shoulder = 25.0, elbow = 140.0)

private fun zercherBar(pelvis: Point, torso: Double): Point =
    Barbell(BarHold.CROOK).center(Pose(pelvis.x, pelvis.y, torso, nearArm = ZERCHER_ARM, nearLeg = Leg(0.0, 0.0)).skeleton())

/**
 * Zercher squat. The bar in the crook of the elbows, held close against the body ([ZERCHER_ARM]); the torso solved so
 * the bar is over the middle of the foot (BarOverMidfoot, as the barbell squats), which keeps it upright (ExRx
 * barbell Zercher squat, https://exrx.net/WeightExercises/GluteusMaximus/BBZercherSquat; NSCA: an upright torso,
 * https://www.nsca.com/education/articles/kinetic-select/zercher-squat-field-alternative-for-front-squat/). START
 * standing tall, END thighs about level (HipDepth 0). Rack (guide's safety line): J-hooks 8 cm under the bar's
 * standing height, about the upper waist, safety arms 6 cm under its lowest point, as the Squat's; the posts 60 cm out.
 */
private val zercherSquat: ExerciseFigure by lazy {
    // Standing, the whole body tips back over the ankles (straight, [back]°) until the bar is over the mid-foot.
    fun standing(back: Double) = standingLeg(0.0, -back, 0.0, 90.0 + back)
    val mid = midfootX(0.0)
    val back = (0..100).map { it / 10.0 }.minBy { b -> abs(zercherBar(standing(b).hip, 90.0 + b).x - mid) }
    val standTorso = 90.0 + back
    val leg0 = squatLeg(SQUAT_SHIN, 0.5)
    val torso = balancedTorso(leg0, 40.0..90.0, ::zercherBar)
    val start = standOn(standing(back), standTorso, ZERCHER_ARM)
    val end = standOn(leg0.at(torso), torso, ZERCHER_ARM)
    val bar = Barbell(BarHold.CROOK)
    val top = bar.center(start.skeleton())
    val low = bar.center(end.skeleton())
    // The posts 60 cm out, clear of the forearms and hands in front of the chest.
    val rack = rackInFront(top.x, jHookHeight = -top.y - 8.0, safetyHeight = -low.y - Barbell.SHAFT_RADIUS - 6.0, gap = 60.0)
    val props = listOf(Floor, rack, bar)
    figure(
        View.SIDE, start, end, props,
        startCaption = "Bar in crook of elbows",
        endCaption = "Thighs about level",
        thumbnail = Which.END,
        guideLines = listOf(
            RACK_SAFETY,
            "Put your forearms under the bar and rest it in the crook of your elbows, about shoulder-width apart.",
            "Cross your wrists and pull the bar in close against your body.",
            "Bend your hips and knees together, keeping your torso upright and your head in line with your spine.",
            "Go down until your thighs are about level with the floor. Stop sooner if your shoulders or back start to round.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(HipDepth(belowKnee = 0.0, at = Which.END), BarOverMidfoot(Which.START), BarOverMidfoot(Which.END)),
    )
}

/**
 * Single-leg box squat. The near leg works; the far leg is held straight out in front, off the floor, in both panels
 * (ExRx single leg box squat, https://exrx.net/WeightExercises/Quadriceps/BWSingleLegBoxSquat: balance on one leg, the
 * other leg extended forward, the arms out in front). The arms straight out in front, level.
 * - START standing tall on the near leg, the free foot just off the floor: that leg 30° forward, its knee soft (10°).
 * - END sitting back until the buttocks touch the box (HIPS on BOX), the thighs level (the box sets the depth; the
 *   guide gives none, so the conservative level), the shin 20° forward, the torso 55° above level (neutral); the free
 *   leg straight, pointing 22° down so its foot is well clear of the floor and plainly apart from the working leg.
 */
private val singleLegBoxSquat: ExerciseFigure by lazy {
    val arms = { torso: Double -> armAt(0.0, torso) }
    val freeStart = Leg(hip = 30.0, knee = 10.0, ankle = 5.0)
    val start = standOn(tall, 90.0, arms(90.0)).copy(farLeg = freeStart)
    val torso = 55.0
    val leg = standingLeg(0.0, 20.0, kneeForDepth(20.0, 0.0), torso)
    val free = Leg(hip = fold180(-22.0 - torso - 180.0), knee = 0.0, ankle = 0.0)
    val end = standOn(leg, torso, arms(torso)).copy(farLeg = free)
    // The box behind the hips, its front edge 7 cm in front of the hip joint (clear of the free thigh).
    val box = SquatBox(x = leg.hip.x - 18.0, height = -leg.hip.y - Body.TORSO_RADIUS)
    figure(
        View.SIDE, start, end, listOf(Floor, box),
        startCaption = "Other foot off the floor",
        endCaption = "Buttocks touch the box",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand in front of a sturdy bench or box, facing away from it. Its height sets your depth.",
            "Hold your arms out in front of you.",
            "Balance on one leg, with the other foot just off the floor.",
            "Lower until your buttocks touch the box. Don't drop onto it.",
        ),
        contacts = listOf(Contact(BodyPoint.NEAR_FOOT, Surface.FLOOR, BOTH), Contact(BodyPoint.HIPS, Surface.BOX, setOf(Which.END))),
        checks = emptyList(),
    )
}

/**
 * Jump squat. START hips back and knees bent to 90° (neutral: a half squat, "stopping before your heels lift"), the
 * shins 25° forward, the heels down, the arms hanging at the sides (the guide's set-up); the torso by the
 * shoulders-over-feet rule. END in the air at the top of the jump: the legs straight, the toes pointed (the ankles 30°
 * plantar flexed), the feet about 35 cm off the floor (neutral: a jump of 30–50 cm; FeetOffFloor, no floor contact in
 * this panel), the arms swung forward and up, straight, 100° from the sides, a little above level (ACE squat jumps: drive the arms forward and
 * upward during the jump, https://www.acefitness.org/resources/everyone/exercise-library/116/squat-jumps/).
 */
private val jumpSquat: ExerciseFigure by lazy {
    val leg0 = standingLeg(0.0, 25.0, 90.0, 60.0)
    val torso = shouldersOverFeet(leg0)
    val start = standOn(leg0.at(torso), torso, armAt(270.0, torso))
    val air = Leg(hip = 0.0, knee = 0.0, ankle = -30.0)
    val end = Pose(tall.hip.x, tall.hip.y - 40.0, 90.0, nearArm = Arm(shoulder = 100.0, elbow = 0.0), nearLeg = air)
    figure(
        View.SIDE, start, end, listOf(Floor),
        startCaption = "Hips back, knees bent",
        endCaption = "Jump up explosively",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand with your feet hip-width apart and your arms at your sides.",
            "Push your hips back and bend your knees, stopping before your heels lift.",
            "Jump up explosively.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, setOf(Which.START))),
        checks = listOf(FeetOffFloor(min = 25.0, at = Which.END)),
    )
}

// ---- Smith machine ----

/** The Smith machine for a bar at [top] and [low]: its rails on the bar's line, the stops 4 cm under its lowest point. */
private fun smithFor(top: Point, low: Point) = SmithMachine(x = (top.x + low.x) / 2, stopHeight = -low.y - Barbell.SHAFT_RADIUS - 4.0)

private const val SMITH_UNHOOK = "Unhook the bar by turning it back off the hooks."
private const val SMITH_FEET = "Place your feet under the bar."

/**
 * Smith squat. The Squat's poses (MainLifts.kt): the bar across the back of the shoulders, the feet under it, so the
 * bar is over the middle of the foot in both panels; the standing torso is solved so the bar is on the bottom's line,
 * so the bar runs straight down the rails (BarOnRails), with the straight legs tipped just far enough over the ankles
 * that the standing torso is at least 80° (tall, not stooped). START standing tall, END thighs just past level (the Squat's depth, HipDepth 2).
 * The safety stops 4 cm under the bar's lowest point (guide). ExRx Smith squat
 * (https://exrx.net/WeightExercises/Quadriceps/SMSquat).
 */
private val smithSquat: ExerciseFigure by lazy {
    // The bottom at the Squat's trunk angle (44°, inside the hip's 140°), the standing torso solved so the bar is on
    // the same vertical line.
    val low0 = squatLeg(33.0, 2.2, 44.0)
    val lowTorso = 44.0
    // Standing: the legs straight, tipped forward over the ankles just enough (0.5° steps) that the torso solved for
    // the rail line is at least 80° (standing tall, not stooped).
    val lowBar = barOnBack(low0.hip, lowTorso)
    fun torsoFor(leg: StandingLeg) = (700..900).map { it / 10.0 }.minBy { abs(barOnBack(leg.hip, it).x - lowBar.x) }
    val stand0 = (0..20).map { standingLeg(0.0, it / 2.0, 0.0, 80.0) }.first { torsoFor(it) >= 80.0 }
    val standTorso = torsoFor(stand0)
    val start = backSquatPose(stand0.at(standTorso), standTorso)
    val end = backSquatPose(low0.at(lowTorso), lowTorso)
    val bar = Barbell(BarHold.UPPER_BACK)
    figure(
        View.SIDE, start, end, listOf(Floor, smithFor(bar.center(start.skeleton()), bar.center(end.skeleton())), bar),
        startCaption = "Bar on back of shoulders",
        endCaption = JUST_PAST,
        thumbnail = Which.END,
        guideLines = listOf(
            SMITH_SAFETY,
            "Rest the bar across the back of your shoulders and grip it out to the sides.",
            SMITH_FEET,
            SQUAT_DEPTH,
        ),
        contacts = FEET_DOWN,
        checks = listOf(
            HipDepth(belowKnee = 2.0, at = Which.END), BarOnRails(Which.START), BarOnRails(Which.END),
            BarOverMidfoot(Which.START), BarOverMidfoot(Which.END),
        ),
    )
}

/**
 * Smith front squat. The approved Front squat's poses (MainLifts.kt): the bar on the front of the shoulders, the elbows
 * pointing straight ahead, the torso solved so the bar is over the middle of the foot, which is also the rails' line
 * (the feet under the bar). START standing tall, END thighs level (the Front squat's depth; the guide's "just past
 * level" is met within its stop-sooner rule, HipDepth 0). Stops 4 cm under the lowest point. ExRx Smith front squat
 * (https://exrx.net/WeightExercises/Quadriceps/SMFrontSquat).
 */
private val smithFrontSquat: ExerciseFigure by lazy {
    val bar = Barbell(BarHold.FRONT_RACK)
    val start = frontSquat.start.pose
    val end = frontSquat.end.pose
    figure(
        View.SIDE, start, end, listOf(Floor, smithFor(bar.center(start.skeleton()), bar.center(end.skeleton())), bar),
        startCaption = "Elbows point forward",
        endCaption = "Thighs level with floor",
        thumbnail = Which.END,
        guideLines = listOf(
            SMITH_SAFETY,
            "Rest the bar on the front of your shoulders with your elbows pointing forward.",
            SMITH_FEET,
            SQUAT_DEPTH,
        ),
        contacts = FEET_DOWN,
        checks = listOf(
            HipDepth(belowKnee = 0.0, at = Which.END), BarOnRails(Which.START), BarOnRails(Which.END),
            BarOverMidfoot(Which.START), BarOverMidfoot(Which.END),
        ),
    )
}

// ---- machines ----

/**
 * Hack squat. Lying back on the sled's pad, the shoulders under the shoulder pads, the feet on the foot plate (Kit
 * HackSquatMachine: the rail at 45°, the plate square to it). The body works as a squat tilted back 45°: in the plate's
 * own frame the hips move straight along the rail towards the feet.
 * - END (guide): lowered until the knees are just short of fully bent: the knees 126° (the 135° limit, Body.kt, is the
 *   figure's full bend; ExRx sled hack squat, https://exrx.net/WeightExercises/Quadriceps/SLHackSquat: "just short of
 *   complete flexion"), the shins 33° forward of square to the plate, the thighs just past square to the rail.
 * - START: the legs straightened (the knees 3°, neutral), the hips further up the same rail line.
 * - The feet sit 18 cm forward of the hips' line on the plate, which is what lets the knees bend that far with the
 *   ankles inside their limit (ExRx: the platform slightly higher than the base of the sled).
 * - The hands on the side handles by the hips (the same place on the body in both panels; handles not drawn).
 */
private val hackSquat: ExerciseFigure by lazy {
    val angle = 45.0
    val origin = Point(0.0, -25.0)
    // From the plate's frame (x along the plate, y down, as a standing squat's) to the world.
    fun world(p: Point) = origin + unit(angle) * p.x + unit(angle + 90.0) * (-p.y)
    val bottom = squatLeg(33.0, 2.2, 90.0)
    val topLean = (-400..300).map { it / 10.0 }.minBy { abs(standingLeg(0.0, it, 3.0, 90.0).hip.x - bottom.hip.x) }
    val top = standingLeg(0.0, topLean, 3.0, 90.0)
    val torso = 90.0 + angle
    fun scene(leg: StandingLeg): Scene {
        val pelvis = world(leg.hip)
        val machine = HackSquatMachine(origin = world(Point(0.0, 0.0)), pelvis = pelvis, angle = angle)
        // The hands on the side handles beside the hips, in the same place on the body in both panels: on the front of
        // the hip, 12 cm up the torso from the hip joint and 12 cm in front of its axis (neutral).
        val sh = shoulderOf(pelvis, torso)
        val arm = Reach.arm(sh, pelvis + unit(torso) * 12.0 + unit(torso - 90.0) * 12.0, torso)
        return Scene(Pose(pelvis.x, pelvis.y, torso, nearArm = arm, nearLeg = leg.leg), listOf(Floor, machine))
    }
    ExerciseFigure(
        View.SIDE, scene(top), scene(bottom),
        startCaption = "Shoulders under the pads",
        endCaption = "Just short of fully bent",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lean back on the sled with your shoulders under the pads.",
            "Place your feet on the platform. Lower puts more work on your quads; higher puts more on your glutes.",
            "Straighten your legs and release the lock.",
            "Lower by bending your hips and knees until your knees are just short of fully bent.",
        ),
        contacts = listOf(
            Contact(BodyPoint.UPPER_BACK, Surface.BACK_PAD, BOTH),
            Contact(BodyPoint.HIPS, Surface.BACK_PAD, BOTH),
            Contact(BodyPoint.FEET, Surface.FOOT_PLATE, BOTH),
        ),
        checks = emptyList(),
    )
}

/** The lever squat's yoke on the shoulders: across their tops, behind the neck (2 cm above the shoulder joint, 12 cm back). */
private fun yokeOn(pelvis: Point, torso: Double): Point =
    pelvis + unit(torso) * (Segment.TORSO.length + 2.0) + unit(torso + 90.0) * 12.0

/**
 * Lever squat. The yoke across the shoulders, the feet shoulder-width and directly beneath the shoulders (ExRx lever
 * squat, https://exrx.net/WeightExercises/Quadriceps/LVSquat), so the figure balances the yoke over the middle of the
 * foot, as the barbell squats balance the bar. START standing tall, END thighs just past level (HipDepth 2), the shins
 * 33° as the Squat. The lever pivots 120 cm behind the lifter, set so it is the same length in both panels;
 * the hands hold the yoke's handles (the Squat's arms).
 */
private val leverSquat: ExerciseFigure by lazy {
    val stand0 = standingLeg(0.0, 4.0, 0.0, 80.0)
    val standTorso = balancedTorso(stand0, 70.0..90.0, ::yokeOn)
    val low0 = squatLeg(33.0, 2.2, 45.0)
    val lowTorso = balancedTorso(low0, 30.0..75.0, ::yokeOn)
    val start = backSquatPose(stand0.at(standTorso), standTorso)
    val end = backSquatPose(low0.at(lowTorso), lowTorso)
    val a = yokeOn(start.skeleton().pelvis, standTorso)
    val b = yokeOn(end.skeleton().pelvis, lowTorso)
    // The pivot 120 cm behind, on the line of points the same distance from both yoke positions (the yoke moves almost
    // straight down, so that line is close to level, at about hip height).
    val m = (a + b) * 0.5
    val d = b - a
    val pivotX = m.x - 120.0
    val pivot = Point(pivotX, m.y - (pivotX - m.x) * d.x / d.y)
    figure(
        View.SIDE, start, end, listOf(Floor, LeverSquatMachine(pivot, a)),
        startCaption = "Shoulders under the pads",
        endCaption = JUST_PAST,
        thumbnail = Which.END,
        guideLines = listOf(
            "Squat down and put your shoulders under the pads.",
            "Place your feet shoulder-width apart, directly under your shoulders.",
            "Straighten your legs and release the support lever.",
            HIPS_BACK,
            "Go down until your thighs are just past level with the floor.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(HipDepth(belowKnee = 2.0, at = Which.END)),
        endProps = listOf(Floor, LeverSquatMachine(pivot, b)),
    )
}

/** Sitting back on a seat whose back pad is [back]° from level: the pelvis touching the seat and the pad (Shoulders.kt's). */
private fun seatedPelvis(hinge: Point, back: Double): Point {
    val r = back / DEG
    val t = (Body.TORSO_RADIUS - Body.TORSO_RADIUS * cos(r)) / sin(r)
    return hinge + unit(180.0 - back) * t + unit(90.0 - back) * Body.TORSO_RADIUS
}

/** The leg extension machine and the lifter on it, for the near and far knees' bends. */
private class LegExtension(val near: Double, val far: Double, val alternating: Boolean) {
    val scene: Scene by lazy { build() }

    private fun build(): Scene {
        // A seat 52 cm up (so the feet hang clear of the floor), the back pad reclined 15° (75° from level): neutral.
        val seatHeight = 52.0
        val back = 75.0
        val torso = 180.0 - back
        val hinge = Point(0.0, -seatHeight)
        val pelvis = seatedPelvis(hinge, back)
        // The thighs along the seat, 3° down towards the knees (neutral), the knees past the seat's front edge.
        val thigh = -3.0
        fun leg(knee: Double) = Leg(hip = fold180(thigh - torso - 180.0), knee = knee, ankle = 0.0)
        val bare = Pose(pelvis.x, pelvis.y, torso, nearArm = HANG, nearLeg = leg(near), farLeg = leg(far)).skeleton()
        // The roller on the front of the lower shin, 80% of the way from the knee (ExRx: the pad on the lower legs).
        fun roller(l: LimbPoints): Point {
            val along = (l.ankle - l.knee).let { it * (1.0 / it.length()) }
            val front = unit(angleTo(along) + 90.0)
            return l.knee + along * (Segment.SHIN.length * 0.8) + front * (Body.SHIN_WIDTH / 2 + LegExtensionMachine.ROLLER_RADIUS)
        }
        // The side handle under the hand, beside the seat a little in front of the hips.
        val handle = Point(pelvis.x + 12.0, -seatHeight - 4.0)
        val arm = Reach.arm(bare.shoulder, handle, torso)
        val machine = LegExtensionMachine(
            seatFront = hinge.x + LegExtensionMachine.SEAT_LENGTH, seatHeight = seatHeight, backAngle = back,
            pivot = bare.near.knee, roller = roller(bare.near), handle = handle,
            farRoller = if (alternating) roller(bare.far) else null,
        )
        return Scene(Pose(pelvis.x, pelvis.y, torso, nearArm = arm, nearLeg = leg(near), farLeg = leg(far)), listOf(machine))
    }
}

/** The leg extension's start: the knees bent 90°, the shins hanging (neutral, the brief's "about 90°"). */
private const val EXT_START = 90.0

/** The leg extension's end: the legs straight (the knees 3°, the guide's "until your legs are straight"; neutral). */
private const val EXT_END = 3.0

private val LEG_EXT_CONTACTS = listOf(
    Contact(BodyPoint.HIPS, Surface.SEAT, BOTH),
    Contact(BodyPoint.UPPER_BACK, Surface.BACK_PAD, BOTH),
    Contact(BodyPoint.SHINS, Surface.SHIN_PAD, BOTH),
)

/**
 * Leg extension. Seated, the back on the pad, the knees lined up with the lever's pivot, the roller on the front of
 * the lower shins, the hands on the side handles. START the knees bent 90°, END the legs straight. ExRx lever leg
 * extension (https://exrx.net/WeightExercises/Quadriceps/LVLegExtension). The seat and pad angles are neutral.
 */
private val legExtension: ExerciseFigure by lazy {
    val start = LegExtension(EXT_START, EXT_START, alternating = false).scene
    val end = LegExtension(EXT_END, EXT_END, alternating = false).scene
    ExerciseFigure(
        View.SIDE, start.copy(props = listOf(Floor) + start.props), end.copy(props = listOf(Floor) + end.props),
        startCaption = "Lower legs under the pad",
        endCaption = "Legs straight",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit with your back against the back pad.",
            "Put the front of your lower legs under the lower pad.",
            "Line up your knees with the machine's pivot.",
            "Straighten your knees to lift the pad until your legs are straight.",
        ),
        contacts = LEG_EXT_CONTACTS,
        checks = emptyList(),
    )
}

/**
 * Single-leg leg extension. As the Leg extension, on a machine whose two levers move apart (ExRx lever alternating leg
 * extension, https://exrx.net/WeightExercises/Quadriceps/LVAlternatingLegExtension): END the near leg straight while
 * the far leg stays bent 90° under its own pad, so the two legs are plainly apart.
 */
private val singleLegLegExtension: ExerciseFigure by lazy {
    val start = LegExtension(EXT_START, EXT_START, alternating = true).scene
    val end = LegExtension(EXT_END, EXT_START, alternating = true).scene
    ExerciseFigure(
        View.SIDE, start.copy(props = listOf(Floor) + start.props), end.copy(props = listOf(Floor) + end.props),
        startCaption = "Lower legs under the pad",
        endCaption = "Straighten one knee",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit with your back against the back pad.",
            "Put the front of your lower legs under the pad or pads.",
            "Line up your knees with the machine's pivot.",
            "Straighten one knee to lift the pad until that leg is straight.",
        ),
        contacts = LEG_EXT_CONTACTS,
        checks = emptyList(),
    )
}

/**
 * The 45° leg press and the lifter on it (the approved Leg press's geometry, shared with the single-leg press).
 * See [legPress] for the choices.
 */
private class LegPressRig {
    val back = LegPress45.BACK_ANGLE
    // The torso along the reclined back pad (its front face 6 cm out from the frame), the pelvis on the seat.
    val corner = Point(-30.0, -LegPress45.SEAT_HEIGHT)
    val axisStart = corner + unit(back - 90.0) * (6.0 + Body.TORSO_RADIUS)
    val pelvisY = -(LegPress45.SEAT_HEIGHT + Body.TORSO_RADIUS)
    val pelvis = axisStart + unit(back) * ((pelvisY - axisStart.y) / unit(back).y)
    val foot = LegPress45.PLATE_ANGLE // the sole flat on the platform, toes along it

    fun ankleOn(press: LegPress45): Point {
        val plate = press.surfaces(View.SIDE).getValue(Surface.FOOT_PLATE).single()
        val face = Point(plate.cx, plate.cy) - unit(LegPress45.PLATE_ANGLE - 90.0) * (plate.thickness / 2)
        // The foot capsule's centre line lies one foot radius off the face; the ankle is the sole's depth above it,
        // with the foot's middle on the plate's middle.
        return face - unit(LegPress45.PLATE_ANGLE - 90.0) * (Body.FOOT_WIDTH / 2 + Body.ANKLE_HEIGHT - Body.FOOT_WIDTH / 2) -
            unit(foot) * (Segment.FOOT.length * 0.3)
    }

    fun legs(press: LegPress45) = Reach.leg(pelvis, ankleOn(press), back, foot)

    /** The sled position that bends the knees [knee]°. */
    fun sledFor(knee: Double): LegPress45 =
        (0..1200).map { LegPress45(x = 0.0, sled = it / 10.0) }
            .filter { (ankleOn(it) - pelvis).length() <= Segment.THIGH.length + Segment.SHIN.length - 1e-6 }
            .minBy { abs(legs(it).knee - knee) }

    val top by lazy { sledFor(15.0) }
    val bottom by lazy { sledFor(85.0) }
    val arms = Arm(shoulder = 15.0, elbow = 25.0)
}

/**
 * Leg press (45°). START the legs nearly straight with the platform pushed up; END lowered until the knees bend to
 * about 90°. Back and tailbone on the back pad, hips on the seat, feet flat on the platform in both panels.
 * - Knees: 85° at the bottom, the guide's "about 90 degrees" on its conservative side (it says to stop sooner if the
 *   hips lift or the thighs press into the ribs); 15° at the top, "nearly straight" but not locked (the guide's
 *   "straight but not locked"; ExRx's 45° leg press, https://exrx.net/WeightExercises/Quadriceps/SL45LegPress, has the
 *   knees just short of lockout). 15° is a neutral "nearly".
 * - Feet: in the middle of the platform (the guide gives no height for the neutral foot position), toes along it.
 * - Hips at the bottom: the guide stops "sooner if your hips start to lift" and before the thighs press into the ribs,
 *   so the figure keeps hip flexion (trunk to thigh) at about 113°, well short of the squat's. With the feet flat and
 *   the ankle inside its 35° limit, that needs the back pad reclined to 25° above level (LegPress45.BACK_ANGLE) and
 *   the plate tilted 10° (LegPress45.PLATE_TILT): hip flexion = 180 - back + (plate - 90) - ankle + knee.
 * - Hands on the side handles: drawn as arms resting along the body (the handles are not in the kit).
 */
private val legPress: ExerciseFigure by lazy {
    val rig = LegPressRig()
    fun scene(press: LegPress45) =
        Scene(Pose(rig.pelvis.x, rig.pelvis.y, rig.back, nearArm = rig.arms, nearLeg = rig.legs(press)), listOf(Floor, press))
    ExerciseFigure(
        view = View.SIDE,
        start = scene(rig.top),
        end = scene(rig.bottom),
        startCaption = "Legs nearly straight",
        endCaption = "Knees about 90 degrees",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit with your back and tailbone flat against the back pad.",
            "Push the platform up until your legs are nearly straight. Release the lock handles, then hold the side handles.",
            "Lower until your knees bend to about 90 degrees. Stop sooner if your hips start to lift off the pad. Don't let your thighs press into your ribs.",
        ),
        contacts = listOf(
            Contact(BodyPoint.UPPER_BACK, Surface.BACK_PAD, BOTH),
            Contact(BodyPoint.HIPS, Surface.SEAT, BOTH),
            Contact(BodyPoint.FEET, Surface.FOOT_PLATE, BOTH),
        ),
        checks = emptyList(),
    )
}

/**
 * Single-leg leg press. The Leg press's machine, sled positions and working leg (the near leg: nearly straight at the
 * top, about 90° at the bottom); the far foot on the floor below the platform, in front of the seat, the knee bent
 * about 90° with the shin near upright (neutral), so the free leg is plainly apart from the working one. ExRx sled
 * single leg 45° leg press (https://exrx.net/WeightExercises/Quadriceps/SLSingleLeg45LegPress).
 */
private val singleLegLegPress: ExerciseFigure by lazy {
    val rig = LegPressRig()
    val floorFoot = Point(rig.pelvis.x + 48.0, -Body.ANKLE_HEIGHT)
    val down = Reach.leg(rig.pelvis, floorFoot, rig.back, foot = 0.0)
    fun scene(press: LegPress45) =
        Scene(Pose(rig.pelvis.x, rig.pelvis.y, rig.back, nearArm = rig.arms, nearLeg = rig.legs(press), farLeg = down), listOf(Floor, press))
    ExerciseFigure(
        View.SIDE, scene(rig.top), scene(rig.bottom),
        startCaption = "One foot on the platform",
        endCaption = "Knee about 90 degrees",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit with your back flat against the back pad.",
            "Place one foot on the platform and the other on the floor below it.",
            "Push the platform up with your working leg. Release the lock handles, then hold the side handles.",
            "Lower the platform until your knee bends to about 90 degrees. Stop sooner if your hips start to lift off the pad. Don't let your thigh press into your ribs.",
        ),
        contacts = listOf(
            Contact(BodyPoint.UPPER_BACK, Surface.BACK_PAD, BOTH),
            Contact(BodyPoint.HIPS, Surface.SEAT, BOTH),
            Contact(BodyPoint.NEAR_FOOT, Surface.FOOT_PLATE, BOTH),
            Contact(BodyPoint.FAR_FOOT, Surface.FLOOR, BOTH),
        ),
        checks = emptyList(),
    )
}

// ---- split stances: lunges, split squats, the sled push ----

/** The middle of the foot capsule's lowest point above the floor for a foot at [footAngle] with its ankle at [ankle]. */
private fun footLow(ankle: Point, footAngle: Double): Double {
    val fd = unit(footAngle)
    val sole = unit(footAngle - 90.0) * (Body.ANKLE_HEIGHT - Body.FOOT_WIDTH / 2)
    val heel = ankle + sole - fd * maxOf(Segment.FOOT.length * 0.2 - Body.FOOT_WIDTH / 2, 0.0)
    val toe = ankle + sole + fd * (Segment.FOOT.length * 0.8 - Body.FOOT_WIDTH / 2)
    return -maxOf(heel.y, toe.y) - Body.FOOT_WIDTH / 2
}

/**
 * The back foot on its ball in a split stance: the heel up, the toes on the floor under it, the foot drawn pointing
 * [BALL_FOOT]° below level (forward and steeply down; neutral). The figure's foot is one straight piece with no toe
 * joint, so the drawn foot runs from the ankle to the toe tips; standing it steeply on the toes is what lifts the heel
 * and the ankle about 21 cm off the floor, as on the ball of the foot (ExRx split squat: the heel of the rear foot
 * elevated off the floor, https://exrx.net/WeightExercises/Quadriceps/DBSplitSquat). The ankle's bend stays inside its
 * normal limit (Body.kt), so no check is relaxed.
 */
private const val BALL_FOOT = -74.0

/**
 * The back leg of a split stance from [hip], its knee at [knee] and the foot on its ball ([BALL_FOOT]): the ankle at the
 * height that puts the toes on the floor, behind the knee at the shin's length, so the shin slopes up from the knee to
 * the raised heel.
 */
private fun backLegOnBall(hip: Point, knee: Point, torso: Double): Leg {
    val ankleY = (0..600).map { -(Body.ANKLE_HEIGHT + it / 20.0) }.minBy { abs(footLow(Point(0.0, it), BALL_FOOT)) }
    val dy = ankleY - knee.y
    val ankle = Point(knee.x - sqrt(Segment.SHIN.length * Segment.SHIN.length - dy * dy), ankleY)
    return Reach.leg(hip, ankle, torso, BALL_FOOT)
}

/**
 * A leg from [hip] to a foot on its ball, the ankle at [ankleX] and plantar flexed [plantar]°: the ankle raised until
 * the toes touch the floor.
 */
private fun legOnBall(hip: Point, ankleX: Double, torso: Double, plantar: Double, surface: Double = 0.0): Leg {
    fun shinOf(l: Leg) = fold180(torso + 180.0 + l.hip - l.knee)
    val reach = Segment.THIGH.length + Segment.SHIN.length - 1e-6
    val y = (0..800).map { -(surface + it / 20.0) }.filter { (Point(ankleX, it) - hip).length() <= reach }.minBy { yy ->
        val l = Reach.leg(hip, Point(ankleX, yy), torso, foot = 0.0)
        abs(footLow(Point(ankleX, yy), shinOf(l) + 90.0 + plantar) - surface)
    }
    val l = Reach.leg(hip, Point(ankleX, y), torso, foot = 0.0)
    return Reach.leg(hip, Point(ankleX, y), torso, shinOf(l) + 90.0 + plantar)
}

/** A split stance's joints: the pelvis, the front leg and the back leg, upright torso. */
private class Split(val pelvis: Point, val front: Leg, val back: Leg, val torso: Double = 90.0) {
    /** The pose with the front leg [frontNear] (else the far one in front), and arms. */
    fun pose(arm: Arm, frontNear: Boolean = true) = Pose(
        pelvis.x, pelvis.y, torso, nearArm = arm,
        nearLeg = if (frontNear) front else back, farLeg = if (frontNear) back else front,
    )

    fun shifted(dx: Double) = Split(Point(pelvis.x + dx, pelvis.y), front, back, torso)
}

/**
 * The bottom of a lunge or split squat: the front foot flat with its ankle at [frontAnkle], the front shin [shin]°
 * forward (near upright: the front knee over the ankle, neutral 8°), the front thigh about level (the hip [rise] cm
 * above the knee: "about level"), the back thigh [backThigh]° behind vertical (inside the hip's 20° extension limit,
 * Body.kt) down to the back knee, which then sits just above the floor (its round about 4–5 cm off it), the back foot on
 * its ball ([backLegOnBall], the shin sloping up to the raised heel), the torso upright (the guides' "Torso upright").
 * The lunges' long step: [LUNGE_BACK_THIGH]; the split squats' shorter stance: [SPLIT_BACK_THIGH].
 */
private fun splitBottom(frontAnkle: Double, shin: Double = 8.0, rise: Double = 0.8, backThigh: Double = LUNGE_BACK_THIGH): Split {
    val ankle = Point(frontAnkle, -Body.ANKLE_HEIGHT)
    val knee = ankle + unit(90.0 - shin) * Segment.SHIN.length
    val hip = knee - Point(sqrt(Segment.THIGH.length * Segment.THIGH.length - rise * rise), rise)
    val front = Reach.leg(hip, ankle, 90.0, foot = 0.0)
    val backKnee = hip + unit(270.0 - backThigh) * Segment.THIGH.length
    return Split(hip, front, backLegOnBall(hip, backKnee, 90.0))
}

/** A lunge's back thigh, degrees behind vertical: a long step forward or back (about 90 cm ankle to ankle), neutral. */
private const val LUNGE_BACK_THIGH = 18.0

/**
 * A split squat's back thigh, degrees behind vertical: the shorter stance (about 85 cm ankle to ankle) that lets the
 * hips rise to nearly standing with the feet still (neutral; a long lunge step would stop the rise at the hip's 20°
 * extension limit). The front shin 15° forward (the front foot a little nearer the hips) and the front thigh 1.5 cm
 * above level keep the back knee off the floor; neutral.
 */
private const val SPLIT_BACK_THIGH = 12.0

private fun splitSquatBottom(frontAnkle: Double) = splitBottom(frontAnkle, shin = 15.0, rise = 1.5, backThigh = SPLIT_BACK_THIGH)

/** The near side's toe of a pose, its world point. */
private fun Pose.toe(side: Side) = skeleton().side(side).toe

/**
 * A split stance's top: the pelvis risen straight up from [bottom] over the same feet, as high as the legs allow. The
 * front leg no straighter than [slack]° short of straight; the back foot stays on its ball with its ankle over the same
 * spot ([backAnkleX]), pivoting on the ball (its plantar flexion between 10° and 50°, Body.kt's limit, the toes forward), and the back
 * thigh inside the hip's 20° extension limit, with a degree to spare. The back knee stays bent.
 */
private fun splitTop(bottom: Split, frontAnkle: Point, backAnkleX: Double, slack: Double = 2.0): Split {
    val reach = Segment.THIGH.length + Segment.SHIN.length
    val legLen = sqrt(reach * reach - 2 * Segment.THIGH.length * Segment.SHIN.length * (1 - cos(slack / DEG)))
    fun at(y: Double): Split? {
        val hip = Point(bottom.pelvis.x, y)
        if ((frontAnkle - hip).length() > legLen) return null
        val back = (10..50 step 5).map { legOnBall(hip, backAnkleX, 90.0, -it.toDouble()) }
            .filter { it.hip >= -19.0 && it.knee >= 5.0 }.maxByOrNull { it.ankle } ?: return null
        return Split(hip, Reach.leg(hip, frontAnkle, 90.0, foot = 0.0), back)
    }
    var best = bottom
    var y = bottom.pelvis.y
    while (y > -110.0) {
        at(y)?.let { best = it }
        y -= 0.5
    }
    return best
}

/** The skeleton's ankle and foot angle of a [Split]'s back leg. */
private fun Split.backAnkle(): Pair<Point, Double> {
    val s = pose(HANG).skeleton()
    return s.far.ankle to angleTo(s.far.toe - s.far.heel)
}

/** Dumbbells at the sides in both hands, side view. */
private val SIDE_DBS = listOf(Floor, atSide(Side.FAR), atSide(Side.NEAR))

private const val SPLIT_FRONT = 40.0

/**
 * Lunge. START standing with the feet together, a dumbbell at each side (palms in, [atSide]); END stepped forward into
 * the lunge's bottom ([splitBottom]): the back foot is where the feet were (its ball pivots), the front foot a long step
 * ahead. ACE forward lunge (https://www.acefitness.org/resources/everyone/exercise-library/94/forward-lunge/) and ExRx
 * dumbbell lunge (https://exrx.net/WeightExercises/Quadriceps/DBLunge): front thigh level, back knee just above the floor.
 */
private val lunge: ExerciseFigure by lazy {
    val bottom = splitBottom(SPLIT_FRONT)
    val end = bottom.pose(HANG)
    // Standing where the back foot is: its toe at the back foot's toe.
    val back = end.toe(Side.FAR)
    val standToe = standOn(tall, 90.0, HANG).toe(Side.NEAR)
    val start = standOn(tall, 90.0, HANG).let { it.copy(pelvisX = it.pelvisX + back.x - standToe.x) }
    figure(
        View.SIDE, start, end, SIDE_DBS,
        startCaption = "Feet together",
        endCaption = THIGH_LEVEL,
        thumbnail = Which.END,
        guideLines = listOf("Stand with your feet together. Hold a dumbbell in each hand at your sides, or use no weight.", LUNGE_STEP, LUNGE_DEPTH),
        contacts = FEET_DOWN,
        checks = emptyList(),
    )
}

/**
 * Walking lunge. Two lunges one after the other, to show the travel: START the bottom of a lunge (the near leg in
 * front); END the bottom of the next lunge, the back leg brought forward into it (the far leg in front now), the whole
 * figure one step further on: the new back foot's toes where the old front foot's toes were. Both bottoms as the Lunge's.
 * ExRx dumbbell walking lunge (https://exrx.net/WeightExercises/Quadriceps/DBWalkingLunge).
 */
private val walkingLunge: ExerciseFigure by lazy {
    val one = splitBottom(SPLIT_FRONT)
    val first = one.pose(HANG)
    val step = first.toe(Side.NEAR).x - first.toe(Side.FAR).x
    val next = one.shifted(step).pose(HANG, frontNear = false)
    figure(
        View.SIDE, first, next, SIDE_DBS,
        startCaption = THIGH_LEVEL,
        endCaption = "Into the next lunge",
        thumbnail = Which.END,
        guideLines = listOf(DBS_AT_SIDES + " Or use no weight.", LUNGE_STEP, LUNGE_DEPTH, "Bring your back leg forward into the next lunge. Keep alternating legs."),
        contacts = FEET_DOWN,
        checks = emptyList(),
    )
}

/**
 * Reverse lunge. START standing, a dumbbell at each side; END stepped back into the lunge's bottom: the front foot is
 * where the feet were, the back foot a long step behind, on its ball. ACE reverse lunge
 * (https://www.acefitness.org/resources/everyone/exercise-library/319/reverse-lunge/), ExRx dumbbell rear lunge
 * (https://exrx.net/WeightExercises/Quadriceps/DBRearLunge).
 */
private val reverseLunge: ExerciseFigure by lazy {
    val end = splitBottom(0.0).pose(HANG)
    figure(
        View.SIDE, standOn(tall, 90.0, HANG), end, SIDE_DBS,
        startCaption = "Feet hip-width apart",
        endCaption = "Step back with one leg",
        thumbnail = Which.END,
        guideLines = listOf(
            DBS_AT_SIDES + " Or use no weight.",
            "Stand with your feet about hip-width apart.",
            "Step back with one leg, landing on the ball of that foot well behind you.",
            LUNGE_DEPTH,
        ),
        contacts = FEET_DOWN,
        checks = emptyList(),
    )
}

/** The back-squat arms for a bar across the upper back (MainLifts.kt's), for [pose]'s torso. */
private fun Pose.withBar(): Pose = copy(nearArm = armTo(shoulderOf(Point(pelvisX, pelvisY), torso), barOnBack(Point(pelvisX, pelvisY), torso), torso, turnedOut = true)).let { it.copy(farArm = it.nearArm) }

/**
 * The rack a barbell lunge was walked out of, in front of the lifter: J-hooks 8 cm under the bar's standing height,
 * safety arms 6 cm under its lowest point (the guide's safety line), the arms reaching back to [clear] cm in front of
 * the furthest-forward chest of the two panels, so they never cross the lifter, and the posts 25 cm past the
 * furthest-forward toe.
 */
private fun lungeRack(start: Pose, end: Pose): PowerRack {
    val bar = Barbell(BarHold.UPPER_BACK)
    val top = bar.center(start.skeleton())
    val low = listOf(start, end).map { bar.center(it.skeleton()) }.maxBy { it.y }
    val front = listOf(start, end).flatMap { p -> p.skeleton().let { listOf(it.near.toe.x, it.far.toe.x) } }.max()
    val chest = listOf(start, end).maxOf { it.skeleton().chest().x }
    val post = front + 25.0
    return PowerRack(x = post, jHookHeight = -top.y - 8.0, safetyHeight = -low.y - Barbell.SHAFT_RADIUS - 6.0, armLength = post - (chest + 8.0), lifterLeft = true)
}

/**
 * Barbell lunge. The Lunge's poses with the bar across the upper back (the Squat's arms, the hands wide on the bar),
 * the torso upright. START standing tall with the feet together, END the lunge's bottom. ExRx barbell lunge
 * (https://exrx.net/WeightExercises/Quadriceps/BBLunge). Rack (guide's safety line): [lungeRack].
 */
private val barbellLunge: ExerciseFigure by lazy {
    val start = lunge.start.pose.withBar()
    val end = lunge.end.pose.withBar()
    figure(
        View.SIDE, start, end, listOf(Floor, lungeRack(start, end), Barbell(BarHold.UPPER_BACK)),
        startCaption = "Bar on your upper back",
        endCaption = THIGH_LEVEL,
        thumbnail = Which.END,
        guideLines = listOf(RACK_SAFETY, BAR_ON_UPPER_BACK, LUNGE_STEP, LUNGE_DEPTH),
        contacts = FEET_DOWN,
        checks = emptyList(),
    )
}

/** Barbell reverse lunge. The Reverse lunge's poses with the bar across the upper back; the rack as the Barbell lunge's. */
private val barbellReverseLunge: ExerciseFigure by lazy {
    val start = reverseLunge.start.pose.withBar()
    val end = reverseLunge.end.pose.withBar()
    figure(
        View.SIDE, start, end, listOf(Floor, lungeRack(start, end), Barbell(BarHold.UPPER_BACK)),
        startCaption = "Bar on your upper back",
        endCaption = "Step back with one leg",
        thumbnail = Which.END,
        guideLines = listOf(RACK_SAFETY, BAR_ON_UPPER_BACK, "Step back with one leg, landing on the ball of that foot.", LUNGE_DEPTH),
        contacts = FEET_DOWN,
        checks = emptyList(),
    )
}

/**
 * Split squat. The feet stay put; the hips go straight down and up. A split squat's own, shorter stance than the
 * lunges' ([splitSquatBottom], about 85 cm ankle to ankle). END the back knee almost touching the floor, the back heel
 * up; START standing up nearly tall over the same feet ([splitTop]: the front leg 2° short of straight, neutral). The
 * back foot stays on its ball in both panels (it pivots on it as the hips rise). A dumbbell at each side. ExRx dumbbell split squat
 * (https://exrx.net/WeightExercises/Quadriceps/DBSplitSquat).
 */
private val splitSquat: ExerciseFigure by lazy {
    val bottom = splitSquatBottom(SPLIT_FRONT)
    val top = splitTop(bottom, Point(SPLIT_FRONT, -Body.ANKLE_HEIGHT), bottom.backAnkle().first.x)
    figure(
        View.SIDE, top.pose(HANG), bottom.pose(HANG), SIDE_DBS,
        startCaption = "Feet far apart",
        endCaption = KNEE_NEAR,
        thumbnail = Which.END,
        guideLines = listOf(
            DBS_AT_SIDES + " Or use no weight.",
            "Stand with your feet far apart, one in front and one behind.",
            "Go down until your back knee is almost touching the floor.",
        ),
        contacts = FEET_DOWN,
        checks = emptyList(),
    )
}

/**
 * Smith split squat. The Split squat's bottom with the bar across the back of the shoulders on the rails; START
 * standing up over the same feet with the back foot still on its ball (the guide's set-up: "well back on the ball of
 * the foot"). The torso upright and the hips going straight down, so the bar runs straight down the rails. The stops
 * 4 cm under the bar's lowest point. The Split squat's shorter stance. ExRx Smith split squat (the URL in Sources.kt,
 * https://exrx.net/WeightExercises/Quadriceps/SMSplitSquat; ExRx also lists it under GluteusMaximus/SMSplitSquat).
 */
private val smithSplitSquat: ExerciseFigure by lazy {
    val bottom = splitSquatBottom(SPLIT_FRONT)
    val top = splitTop(bottom, Point(SPLIT_FRONT, -Body.ANKLE_HEIGHT), bottom.backAnkle().first.x)
    val start = top.pose(HANG).withBar()
    val end = bottom.pose(HANG).withBar()
    val bar = Barbell(BarHold.UPPER_BACK)
    figure(
        View.SIDE, start, end, listOf(Floor, smithFor(bar.center(start.skeleton()), bar.center(end.skeleton())), bar),
        startCaption = "Back foot well back",
        endCaption = KNEE_NEAR,
        thumbnail = Which.END,
        guideLines = listOf(
            SMITH_SAFETY,
            "Rest the bar across the back of your shoulders and grip it out to the sides.",
            "Place one foot slightly forward, and the other well back on the ball of the foot.",
            "Bend both knees and hips to lower until your back knee is almost touching the floor.",
        ),
        contacts = FEET_DOWN,
        checks = listOf(BarOnRails(Which.START), BarOnRails(Which.END)),
    )
}

/**
 * Bulgarian split squat. The far (back) leg's foot rests, its top down, on a bench behind; the near (front) foot flat
 * on the floor; a dumbbell at each side. The bench 40 cm high, the height the velocity studies use
 * (https://pmc.ncbi.nlm.nih.gov/articles/PMC10437032/: the rear foot elevated to 40 cm).
 * - END: the back knee nearly touching the floor (12 cm up, its round 6.5 cm off the floor: the knee's 135° limit,
 *   Body.kt, with the foot up on the bench keeps it there), the front thigh about level, the front shin 10° forward,
 *   the torso upright (the cues); the back foot pointed (the ankle at its 50° plantar flexion limit) so the top of the
 *   foot lies on the pad, the ankle just past the bench's front edge.
 * - START: risen straight up over the same feet, the front leg 10° short of straight (neutral).
 */
private val bulgarianSplitSquat: ExerciseFigure by lazy {
    val torso = 90.0
    val benchTop = 40.0
    val plantar = -50.0
    val knee = Point(0.0, -12.0)
    // The back shin's angle that puts the pointed foot's lowest point on the bench top.
    val shin = (900..1800).map { it / 10.0 }.minBy { a ->
        val ankle = knee + unit(a) * Segment.SHIN.length
        abs(footLow(ankle, a + 90.0 + plantar) - benchTop)
    }
    val ankle = knee + unit(shin) * Segment.SHIN.length
    // The hip over the back knee, the back thigh 12° behind vertical (inside the 20° hip extension limit).
    val hip = knee + unit(90.0 - 12.0) * Segment.THIGH.length
    val back = Reach.leg(hip, ankle, torso, shin + 90.0 + plantar)
    // The front foot: the shin 10° forward, the knee at the hip's height or just below (the thigh about level).
    val frontKneeY = minOf(hip.y + 1.0, -(Body.ANKLE_HEIGHT + Segment.SHIN.length * cos(10.0 / DEG)))
    val frontKnee = Point(hip.x + sqrt(Segment.THIGH.length * Segment.THIGH.length - (frontKneeY - hip.y) * (frontKneeY - hip.y)), frontKneeY)
    val frontAnkle = Point(frontKnee.x - Segment.SHIN.length * sin(10.0 / DEG), -Body.ANKLE_HEIGHT)
    val front = Reach.leg(hip, frontAnkle, torso, foot = 0.0)
    val bottom = Split(hip, front, back)
    // START: risen over the same front foot, the front leg 10° short of straight (neutral); the back foot pivots on
    // the pad, its ankle over the same spot, still pointed to the limit.
    val topHip = Point(hip.x, -(Body.ANKLE_HEIGHT + sqrt(
        (Segment.THIGH.length * Segment.THIGH.length + Segment.SHIN.length * Segment.SHIN.length +
            2 * Segment.THIGH.length * Segment.SHIN.length * cos(10.0 / DEG)) - (frontAnkle.x - hip.x) * (frontAnkle.x - hip.x),
    )))
    val top = Split(topHip, Reach.leg(topHip, frontAnkle, torso, foot = 0.0), legOnBall(topHip, ankle.x, torso, plantar, benchTop))
    // The bench's front edge 7 cm behind the ankle, the pad running back from it.
    val bench = FlatBench(x = ankle.x - 7.0 - FlatBench.LENGTH, height = benchTop)
    figure(
        View.SIDE, top.pose(HANG), bottom.pose(HANG), listOf(Floor, bench, atSide(Side.FAR), atSide(Side.NEAR)),
        startCaption = "Rear foot on the bench",
        endCaption = KNEE_NEAR,
        thumbnail = Which.END,
        guideLines = listOf(
            "Hold a dumbbell in each hand at your sides, or rest a barbell across your upper back.",
            "Reach one leg back and rest the top of that foot on the bench.",
            "Go down until your back knee is nearly touching the floor.",
        ),
        contacts = listOf(Contact(BodyPoint.NEAR_FOOT, Surface.FLOOR, BOTH), Contact(BodyPoint.FAR_FOOT, Surface.BENCH, BOTH)),
        checks = emptyList(),
    )
}

/**
 * Sled push. The guide's set-up: hips low, the back foot back, the torso nearly upright and the arms straight to the
 * handle posts (ExRx sled push, https://exrx.net/WeightExercises/Power/WTPushSprint: hips low, one foot back, the torso
 * nearly vertical, the arms straight; at the start the body drops down with the heels off the ground). START that
 * set-up: the torso 80° above level, the hips 72 cm up, the front (far) foot flat under the knee, the back (near) foot
 * back on its ball. END leaning into the sled, the weight low: the torso 55° above level (neutral), the near (back) leg
 * driving almost straight behind, both heels off the ground, the arms straight to the posts.
 */
private val sledPush: ExerciseFigure by lazy {
    val sledX = 0.0
    fun pose(torso: Double, hipY: Double, frontDx: Double, frontPlantar: Double, backDx: Double): Pose {
        // The hips placed so a straight arm, level, reaches the post.
        val shUp = unit(torso) * Segment.TORSO.length
        val pelvis = Point(sledX - shUp.x - ARM_REACH, hipY)
        val sh = pelvis + shUp
        val grip = Point(sledX, sh.y)
        val arm = straightArm(sh, grip, torso)
        // Front foot: its ankle [frontDx] ahead of the hips; flat, or on its ball.
        val frontAnkleX = pelvis.x + frontDx
        val frontLeg = if (frontPlantar == 0.0) {
            Reach.leg(pelvis, Point(frontAnkleX, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
        } else {
            legOnBall(pelvis, frontAnkleX, torso, frontPlantar)
        }
        // Back leg: its ankle [backDx] behind the hips, the foot on its ball.
        val back = legOnBall(pelvis, pelvis.x - backDx, torso, -25.0)
        return Pose(pelvis.x, pelvis.y, torso, nearArm = arm, nearLeg = back, farLeg = frontLeg)
    }
    val start = pose(torso = 80.0, hipY = -72.0, frontDx = 22.0, frontPlantar = 0.0, backDx = 35.0)
    val end = pose(torso = 55.0, hipY = -72.0, frontDx = 25.0, frontPlantar = -30.0, backDx = 58.0)
    figure(
        View.SIDE, start, end, listOf(Floor, PushSled(sledX)),
        startCaption = "Hips low, arms straight",
        endCaption = "Lean into the sled",
        thumbnail = Which.END,
        guideLines = listOf(
            "Bend down and grip the handles with both hands.",
            "Set your hips low and your back foot back, with your torso nearly upright and your arms straight.",
            "To start, lean into the sled, with your heels off the ground.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH), Contact(BodyPoint.HANDS, Surface.SLED_HANDLE, BOTH)),
        checks = emptyList(),
    )
}

// ---- step-ups ----

/** The step-up box's height: knee height or a little lower (the guide's limit), 45 cm here (neutral). */
private const val STEP_BOX = 45.0

/**
 * Step-up. Facing the box (Kit SquatBox, 45 cm, below the knee joint's 50 cm: the guide's "no higher than your knee"),
 * a dumbbell at each side, palms in (the guide's grip: in profile). START the near foot flat on the box, its whole foot
 * on the top, the far leg straight on the floor behind it, the torso upright; END standing tall on the box, both feet
 * up ("then bring your other foot up"). ExRx dumbbell step-up (https://exrx.net/WeightExercises/Quadriceps/DBStepUp).
 */
private val stepUp: ExerciseFigure by lazy {
    val box = SquatBox(x = 40.0, height = STEP_BOX, depth = 50.0)
    // START: the far foot on the floor 22 cm in front of the box; the near foot 6 cm in from the box's front edge.
    val boxFront = box.x - box.depth / 2
    val nearAnkle = Point(boxFront + 6.0 + Segment.FOOT.length * 0.2, -(STEP_BOX + Body.ANKLE_HEIGHT))
    val farAnkle = Point(boxFront - 22.0, -Body.ANKLE_HEIGHT)
    // The pelvis over the far foot with that leg 3° short of straight.
    val legLen = sqrt(
        Segment.THIGH.length * Segment.THIGH.length + Segment.SHIN.length * Segment.SHIN.length -
            2 * Segment.THIGH.length * Segment.SHIN.length * cos((180.0 - 3.0) / DEG),
    )
    val pelvis = Point(farAnkle.x + 4.0, -(Body.ANKLE_HEIGHT + sqrt(legLen * legLen - 16.0)))
    val start = Pose(
        pelvis.x, pelvis.y, 90.0, nearArm = HANG,
        nearLeg = Reach.leg(pelvis, nearAnkle, 90.0, foot = 0.0), farLeg = Reach.leg(pelvis, farAnkle, 90.0, foot = 0.0),
    )
    val end = Pose(nearAnkle.x, tall.hip.y - STEP_BOX, 90.0, nearArm = HANG, nearLeg = tall.leg)
    figure(
        View.SIDE, start, end, listOf(Floor, box, atSide(Side.FAR), atSide(Side.NEAR)),
        startCaption = "One foot on the box",
        endCaption = "Stand up on the box",
        thumbnail = Which.START,
        guideLines = listOf(
            BOX_HEIGHT,
            "Stand facing it with your feet about hip-width apart. Hold a dumbbell in each hand, palms facing in, or use no weight.",
            "Place one foot on the box.",
            "Stand up on the box by straightening that leg, then bring your other foot up.",
        ),
        contacts = listOf(
            Contact(BodyPoint.NEAR_FOOT, Surface.BOX, BOTH),
            Contact(BodyPoint.FAR_FOOT, Surface.FLOOR, setOf(Which.START)),
            Contact(BodyPoint.FAR_FOOT, Surface.BOX, setOf(Which.END)),
        ),
        checks = emptyList(),
    )
}

// ---- front view ----

/**
 * A front-view leg from its 3D joints (x across, y down, z towards the camera): the drawn hip and knee angles and the
 * thigh's and shin's angles out of the picture plane (Leg.out, Leg.shinOut), the foot drawn at [foot]° (the near side's
 * screen angle; 0 = a flat foot). The far side is drawn mirrored (Body.kt), so its angles are worked out mirrored.
 */
private fun frontLeg(side: Side, hip: V3, knee: V3, ankle: V3, foot: Double = 0.0): Leg {
    val t = knee - hip
    val sh = ankle - knee
    val ta = angleTo(t.flat)
    val sa = angleTo(sh.flat)
    val near = side == Side.NEAR
    val hipA = fold180(if (near) ta - 270.0 else -90.0 - ta)
    val kneeA = fold180(if (near) ta - sa else sa - ta)
    val shinLocal = 270.0 + hipA - kneeA
    return Leg(
        hip = hipA, knee = kneeA, ankle = fold180(foot - shinLocal - 90.0),
        out = asin((t.z / t.length()).coerceIn(-1.0, 1.0)) * DEG, shinOut = asin((sh.z / sh.length()).coerceIn(-1.0, 1.0)) * DEG,
    )
}

/** The knee of a front-view leg from [hip] to [ankle] (3D), bent towards [hint] (Poses.kt arm3's two-link solve). */
private fun knee3(hip: V3, ankle: V3, hint: V3): V3 {
    val a = Segment.THIGH.length
    val b = Segment.SHIN.length
    val g = ankle - hip
    val d = g.length()
    require(d <= a + b + 1e-6) { "ankle out of reach: ${(d * 10).toLong() / 10.0} cm" }
    val gu = g.unit()
    val alpha = kotlin.math.acos(((a * a + d * d - b * b) / (2 * a * d)).coerceIn(-1.0, 1.0))
    val h = hint - gu * hint.dot(gu)
    val n = if (h.length() < 1e-9) V3(0.0, 0.0, 1.0) else h.unit()
    return hip + (gu * cos(alpha) + n * sin(alpha)) * a
}

/** The hip joints of a front-view pose whose pelvis is at ([x], [y]), [z] towards the camera. */
private fun frontHips(x: Double, y: Double, z: Double = 0.0) = V3(x + Body.HIP_HALF_WIDTH, y, z) to V3(x - Body.HIP_HALF_WIDTH, y, z)

/** A straight front-view leg from [hip] to the flat foot's ankle at [ankle] (exactly a leg's length away). */
private fun straightFrontLeg(side: Side, hip: V3, ankle: V3): Leg {
    val k = hip + (ankle - hip) * (Segment.THIGH.length / (ankle - hip).length())
    return frontLeg(side, hip, k, ankle)
}

/** The standing hip height over feet [spread] cm out from the hip joints, with straight legs. */
private fun standHipY(spread: Double) = -(Body.ANKLE_HEIGHT + sqrt((Segment.THIGH.length + Segment.SHIN.length).let { it * it } - spread * spread))

/** Front view, standing with straight legs, the ankles [spread] cm out from the hip joints, the pelvis at [x]. */
private fun frontStand(x: Double, spread: Double, arm: Arm): Pose {
    val y = standHipY(spread)
    val (nh, fh) = frontHips(x, y)
    val near = straightFrontLeg(Side.NEAR, nh, V3(nh.x + spread, -Body.ANKLE_HEIGHT, 0.0))
    val far = straightFrontLeg(Side.FAR, fh, V3(fh.x - spread, -Body.ANKLE_HEIGHT, 0.0))
    return Pose(x, y, 90.0, nearArm = arm, nearLeg = near, farLeg = far)
}

/**
 * Lateral lunge. Front view (the step is straight out to the side). START standing, the feet hip-width apart (the
 * ankles 3 cm out from the hips), a dumbbell in each hand, palms facing each other (end-on from the front). END the
 * near foot stepped straight out to the side, the far foot where it was:
 * - the near shin upright ("Keep that shin close to upright"), the knee over the foot;
 * - the hips pushed back (29 cm behind the feet) and down to 67 cm, the torso leaning 35° towards the camera
 *   (neutral), so the near knee bends about 65°, a conservative depth (the guide gives none);
 * - the far leg straight; both hips stay inside the 40° abduction limit (Body.kt), which sets the step, about 100 cm
 *   between the ankles;
 * - the arms hang straight down, so the weights come down by the near knee ("reach the weights toward that foot").
 * ACE lateral lunge (https://www.acefitness.org/resources/everyone/exercise-library/364/lateral-lunge/), ExRx
 * dumbbell side lunge (https://exrx.net/WeightExercises/Quadriceps/DBSideLunge).
 */
private val lateralLunge: ExerciseFigure by lazy {
    val start = frontStand(0.0, 3.0, HANG)
    val farAnkleX = -Body.HIP_HALF_WIDTH - 3.0
    val hipY = -67.0
    val kneeY = -(Body.ANKLE_HEIGHT + Segment.SHIN.length)
    val out = 26.0 // the near knee this far out from its hip joint
    val back = sqrt(Segment.THIGH.length * Segment.THIGH.length - (kneeY - hipY) * (kneeY - hipY) - out * out)
    // The pelvis's x that puts the straight far leg's ankle on its START spot.
    val reach = Segment.THIGH.length + Segment.SHIN.length
    val farDx = sqrt(reach * reach - (hipY + Body.ANKLE_HEIGHT) * (hipY + Body.ANKLE_HEIGHT) - back * back)
    val px = farAnkleX + farDx + Body.HIP_HALF_WIDTH
    val (nh, fh) = frontHips(px, hipY, -back)
    val nearKnee = V3(nh.x + out, kneeY, 0.0)
    val near = frontLeg(Side.NEAR, nh, nearKnee, V3(nearKnee.x, -Body.ANKLE_HEIGHT, 0.0))
    val far = straightFrontLeg(Side.FAR, fh, V3(farAnkleX, -Body.ANKLE_HEIGHT, 0.0))
    val end = Pose(px, hipY, 90.0, nearArm = HANG, nearLeg = near, farLeg = far, lean = 35.0)
    figure(
        View.FRONT, start, end, listOf(Floor, atSide(Side.FAR, View.FRONT), atSide(Side.NEAR, View.FRONT)),
        startCaption = "Feet hip-width apart",
        endCaption = "Step out to one side",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand with your feet hip-width apart. Hold a dumbbell in each hand, palms facing each other, or use no weight.",
            "Step straight out to one side. Land on your heel, then your forefoot.",
            "Push your hips back and bend the knee of the leg you stepped onto. Keep that shin close to upright and reach the weights toward that foot.",
        ),
        contacts = FEET_DOWN,
        checks = emptyList(),
    )
}

/**
 * Curtsy lunge. Front view (the back leg crosses behind the front leg to the other side). START the feet wide apart
 * (the ankles 22 cm out from the hips, neutral). END the far leg stepped back and behind the near one: its knee low
 * (20 cm up, between the legs) and its foot on its ball behind, past the near foot on the other side; the near foot
 * where it was, 25 cm out from the hips' middle, its shin upright ("keeping your front knee over your heel"), the thigh forward towards the camera; the hips 60 cm up
 * (the guide gives no depth: neutral); the far thigh crosses 12 cm in, inside the 20° adduction limit (Body.kt). The
 * chest up (the torso leans 10°, neutral), the arms hanging. ACE 8 butt-toning moves
 * (https://www.acefitness.org/resources/pros/expert-articles/6355/8-butt-toning-moves/).
 */
private val curtsyLunge: ExerciseFigure by lazy {
    val spread = 22.0
    val start = frontStand(0.0, spread, HANG)
    val nearAnkleX = Body.HIP_HALF_WIDTH + spread
    val hipY = -60.0
    val px = nearAnkleX - 25.0
    val (nh, fh) = frontHips(px, hipY)
    // The near (front) leg: the shin upright under the knee, the thigh forward.
    val kneeY = -(Body.ANKLE_HEIGHT + Segment.SHIN.length)
    val dx = nearAnkleX - nh.x
    val dz = sqrt(Segment.THIGH.length * Segment.THIGH.length - (kneeY - hipY) * (kneeY - hipY) - dx * dx)
    val nearKnee = V3(nearAnkleX, kneeY, dz)
    val near = frontLeg(Side.NEAR, nh, nearKnee, V3(nearAnkleX, -Body.ANKLE_HEIGHT, dz))
    // The far (back) leg: the knee 20 cm up, 12 cm across, a little behind; the foot behind, 25 cm further across, on
    // its ball (the drawn foot pointing down and across, so its toes are on the floor).
    val farKneeY = -20.0
    val kx = fh.x + 12.0
    val shinAcross = 34.0
    val kz = -sqrt(Segment.THIGH.length * Segment.THIGH.length - (farKneeY - hipY) * (farKneeY - hipY) - 144.0)
    val farKnee = V3(kx, farKneeY, kz)
    val footAngle = -120.0 // the far side's foot drawn pointing down and across (mirrored: down towards +x)
    fun farLegTo(ankleY: Double): Leg {
        val ax = kx + shinAcross
        val az = kz - sqrt(Segment.SHIN.length * Segment.SHIN.length - shinAcross * shinAcross - (ankleY - farKneeY) * (ankleY - farKneeY))
        return frontLeg(Side.FAR, fh, farKnee, V3(ax, ankleY, az), footAngle)
    }
    fun pose(far: Leg) = Pose(px, hipY, 90.0, nearArm = HANG, nearLeg = near, farLeg = far, lean = 10.0)
    // The ankle's height that puts the far foot's toes on the floor.
    val lowBy = pose(farLegTo(-10.0)).skeleton(View.FRONT).far.let { -maxOf(it.heel.y, it.toe.y) - Body.FOOT_WIDTH / 2 }
    val end = pose(farLegTo(-10.0 + lowBy))
    figure(
        View.FRONT, start, end, listOf(Floor),
        startCaption = "Feet wide apart",
        endCaption = "Step back and behind",
        thumbnail = Which.END,
        guideLines = listOf(
            "Stand with your feet wide apart.",
            "Step one leg back and behind the other, like a curtsy.",
            "Bend both knees to lower yourself, keeping your front knee over your heel.",
        ),
        contacts = FEET_DOWN,
        checks = emptyList(),
    )
}

/**
 * Lateral step-up. Front view (the box is beside the lifter, the step up is sideways). The box (Kit SquatBox) beside
 * the near side, 40 cm high, under the knee (the guide's limit) and lower than the forward step-up's (neutral), 30 cm
 * across. START standing on the far leg, the near foot flat on the box, 15 cm forward of the standing leg ("a little
 * forward of your standing knee"), the near knee bent forward and out. END standing tall on the box on the straightened
 * near leg, the far foot lifted off the floor beside the box, the leg 8° out to the side and its knee bent 30° behind (neutral). Dumbbells at the
 * sides (palms in: end-on from the front). ExRx dumbbell lateral step-up
 * (https://exrx.net/WeightExercises/GluteusMaximus/DBLateralStepUp).
 */
private val lateralStepUp: ExerciseFigure by lazy {
    val height = 40.0
    val footX = Body.HIP_HALF_WIDTH + 30.0
    val box = SquatBox(x = footX + 4.0, height = height, depth = 30.0)
    val standY = standHipY(2.0)
    val (nh, fh) = frontHips(0.0, standY)
    val nearAnkle = V3(footX, -(height + Body.ANKLE_HEIGHT), 15.0)
    val near = frontLeg(Side.NEAR, nh, knee3(nh, nearAnkle, V3(0.4, 0.0, 1.0)), nearAnkle)
    val far = straightFrontLeg(Side.FAR, fh, V3(fh.x - 2.0, -Body.ANKLE_HEIGHT, 0.0))
    val start = Pose(0.0, standY, 90.0, nearArm = HANG, nearLeg = near, farLeg = far)
    // END: the near leg straight up from the same foot, the pelvis over it.
    val upY = nearAnkle.y - (Segment.THIGH.length + Segment.SHIN.length)
    val upX = footX - Body.HIP_HALF_WIDTH
    val (nh2, fh2) = frontHips(upX, upY, 15.0)
    val upNear = straightFrontLeg(Side.NEAR, nh2, nearAnkle)
    val farKnee = fh2 + V3(-Segment.THIGH.length * sin(8.0 / DEG), Segment.THIGH.length * cos(8.0 / DEG), 0.0)
    val farAnkle = farKnee + V3(-Segment.SHIN.length * sin(4.0 / DEG), Segment.SHIN.length * cos(8.0 / DEG) * cos(30.0 / DEG), -Segment.SHIN.length * sin(30.0 / DEG))
    val hanging = frontLeg(Side.FAR, fh2, farKnee, farAnkle)
    val end = Pose(upX, upY, 90.0, nearArm = HANG, nearLeg = upNear, farLeg = hanging)
    figure(
        View.FRONT, start, end, listOf(Floor, box, atSide(Side.FAR, View.FRONT), atSide(Side.NEAR, View.FRONT)),
        startCaption = "Near foot on the box",
        endCaption = "Stand up on the box",
        thumbnail = Which.END,
        guideLines = listOf(
            BOX_HEIGHT,
            "Stand side-on to it. Hold a dumbbell in each hand at your sides, or use no weight.",
            "Lift the leg nearest the box and place that foot on it, a little forward of your standing knee.",
            "Stand up on the box by straightening that leg.",
        ),
        contacts = listOf(
            Contact(BodyPoint.NEAR_FOOT, Surface.BOX, BOTH),
            Contact(BodyPoint.FAR_FOOT, Surface.FLOOR, setOf(Which.START)),
        ),
        checks = emptyList(),
    )
}

internal val quadFigures: Map<String, Lazy<ExerciseFigure>> by lazy {
    mapOf(
        "Leg press" to lazy { legPress },
        "Bulgarian split squat" to lazy { bulgarianSplitSquat },
        "Goblet squat" to lazy { gobletSquat },
        "DB squat" to lazy { dbSquat },
        "Bodyweight squat" to lazy { bodyweightSquat },
        "Kettlebell front squat" to lazy { kettlebellFrontSquat },
        "Zercher squat" to lazy { zercherSquat },
        "Single-leg box squat" to lazy { singleLegBoxSquat },
        "Jump squat" to lazy { jumpSquat },
        "Smith squat" to lazy { smithSquat },
        "Smith front squat" to lazy { smithFrontSquat },
        "Smith split squat" to lazy { smithSplitSquat },
        "Single-leg leg press" to lazy { singleLegLegPress },
        "Hack squat" to lazy { hackSquat },
        "Lever squat" to lazy { leverSquat },
        "Leg extension" to lazy { legExtension },
        "Single-leg leg extension" to lazy { singleLegLegExtension },
        "Sled push" to lazy { sledPush },
        "Lunge" to lazy { lunge },
        "Walking lunge" to lazy { walkingLunge },
        "Reverse lunge" to lazy { reverseLunge },
        "Barbell reverse lunge" to lazy { barbellReverseLunge },
        "Barbell lunge" to lazy { barbellLunge },
        "Lateral lunge" to lazy { lateralLunge },
        "Curtsy lunge" to lazy { curtsyLunge },
        "Split squat" to lazy { splitSquat },
        "Step-up" to lazy { stepUp },
        "Lateral step-up" to lazy { lateralStepUp },
    )
}
