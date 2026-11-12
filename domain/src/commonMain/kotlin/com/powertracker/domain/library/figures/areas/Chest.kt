package com.powertracker.domain.library.figures.areas

import com.powertracker.domain.library.areas.RACK_SAFETY
import com.powertracker.domain.library.figures.AdjustableBench
import com.powertracker.domain.library.figures.Arm
import com.powertracker.domain.library.figures.AssistedDipMachine
import com.powertracker.domain.library.figures.BackPlates
import com.powertracker.domain.library.figures.BarOnRails
import com.powertracker.domain.library.figures.BarTouches
import com.powertracker.domain.library.figures.Barbell
import com.powertracker.domain.library.figures.Body
import com.powertracker.domain.library.figures.BodyPoint
import com.powertracker.domain.library.figures.CableHandles
import com.powertracker.domain.library.figures.CableStack
import com.powertracker.domain.library.figures.ChestOffFloor
import com.powertracker.domain.library.figures.Contact
import com.powertracker.domain.library.figures.DeclineBench
import com.powertracker.domain.library.figures.DipBars
import com.powertracker.domain.library.figures.Dumbbell
import com.powertracker.domain.library.figures.ElbowBend
import com.powertracker.domain.library.figures.ExerciseFigure
import com.powertracker.domain.library.figures.FlatBench
import com.powertracker.domain.library.figures.Floor
import com.powertracker.domain.library.figures.HandsLevelWith
import com.powertracker.domain.library.figures.HandsOffFloor
import com.powertracker.domain.library.figures.HandsTogether
import com.powertracker.domain.library.figures.HeldBeside
import com.powertracker.domain.library.figures.HeldOver
import com.powertracker.domain.library.figures.KeyCheck
import com.powertracker.domain.library.figures.Leg
import com.powertracker.domain.library.figures.LockoutOverShoulders
import com.powertracker.domain.library.figures.PecDeck
import com.powertracker.domain.library.figures.Point
import com.powertracker.domain.library.figures.Pose
import com.powertracker.domain.library.figures.PowerRack
import com.powertracker.domain.library.figures.PressMachine
import com.powertracker.domain.library.figures.Prop
import com.powertracker.domain.library.figures.Reach
import com.powertracker.domain.library.figures.Scene
import com.powertracker.domain.library.figures.Seat
import com.powertracker.domain.library.figures.Segment
import com.powertracker.domain.library.figures.ShouldersOverHands
import com.powertracker.domain.library.figures.Side
import com.powertracker.domain.library.figures.Skeleton
import com.powertracker.domain.library.figures.SmithMachine
import com.powertracker.domain.library.figures.Surface
import com.powertracker.domain.library.figures.UpperArmLevel
import com.powertracker.domain.library.figures.View
import com.powertracker.domain.library.figures.Which
import com.powertracker.domain.library.figures.chest
import com.powertracker.domain.library.figures.point
import com.powertracker.domain.library.figures.skeleton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Figures for the library's Chest area (library/areas/Chest.kt), keyed by catalogue name (Task 6).
//
// Camera: the side view for the presses, the lying flys, the dips and the push-ups, whose key positions (the stop
// point at the chest, the lockout, the depth, the bench angle) are in the sagittal plane. The front view for the
// standing and seated flys (cable crossover, low-to-high fly, seated cable fly, pec deck), whose hugging arc is in the
// frontal plane, and for the archer push-up, whose point is the arm out to the side.
// Lying flys stay side-on: the guide's limit there is a height ("level with your shoulders"), which only the side
// view shows; the arm out to the side is then drawn foreshortened (Arm.out), the dumbbell beside the shoulder.
// Every position a guide does not fix is sourced in its comment, or kept neutral and marked so.

// ---- shared bodies ----

/** The near shoulder's 3D point in a side view: on the torso's axis, half the shoulders' width towards the camera. */
private fun sideShoulder(s: Skeleton) = v3(s.shoulder, Body.SHOULDER_HALF_WIDTH)

private val SIDE_ARMS_DOWN = Arm(0.0, 0.0)

/** Lying face up on the flat bench, as the approved Bench: pelvis, torso and the legs with the feet flat. */
private val FLAT_TORSO = 180.0
private val FLAT_PELVIS = Point(95.0, -(FlatBench.HEIGHT + Body.TORSO_RADIUS))
private val flatLegs by lazy { Reach.leg(FLAT_PELVIS, Point(127.0, -Body.ANKLE_HEIGHT), FLAT_TORSO, foot = 0.0) }
private val flatBare by lazy { Pose(FLAT_PELVIS.x, FLAT_PELVIS.y, FLAT_TORSO, nearArm = SIDE_ARMS_DOWN, nearLeg = flatLegs).skeleton() }

private fun flatPose(arm: Arm) = Pose(FLAT_PELVIS.x, FLAT_PELVIS.y, FLAT_TORSO, nearArm = arm, nearLeg = flatLegs)

private val LYING_CONTACTS = listOf(
    Contact(BodyPoint.FEET, Surface.FLOOR, BOTH),
    Contact(BodyPoint.HEAD, Surface.BENCH, BOTH),
    Contact(BodyPoint.UPPER_BACK, Surface.BENCH, BOTH),
    Contact(BodyPoint.HIPS, Surface.BENCH, BOTH),
)

/** Straight arms up from the shoulders (a lockout over the shoulders). */
private val LOCKOUT_UP = Arm(shoulder = 90.0, elbow = 0.0)

/** The straight arm from [shoulder] to [target] at arm's length on the same line (a lockout over a point). */
private fun straightTo(shoulder: Point, target: Point, torso: Double): Pair<Arm, Point> {
    val d = (target - shoulder).let { it * (1.0 / it.length()) }
    return Arm(shoulder = fold180(angleTo(d) - torso - 180.0), elbow = 0.0) to (shoulder + d * ARM_REACH)
}

/** The lockout straight above [x] for arms of full reach from [shoulder] (a Smith bar's vertical path). */
private fun lockoutAbove(shoulder: Point, x: Double, torso: Double): Arm {
    val dx = x - shoulder.x
    val target = Point(x, shoulder.y - sqrt(ARM_REACH * ARM_REACH - dx * dx))
    return straightTo(shoulder, target, torso).first
}

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
) = ExerciseFigure(
    view = view,
    start = Scene(start, props),
    end = Scene(end, endProps),
    startCaption = startCaption,
    endCaption = endCaption,
    thumbnail = thumbnail,
    guideLines = guideLines,
    contacts = contacts,
    checks = checks,
)

/** Two dumbbells, the far one drawn behind. */
private val DUMBBELLS = listOf(Dumbbell(Side.FAR), Dumbbell(Side.NEAR))

/**
 * A fly's dumbbells: the palms face each other, so the handles run along the body and the side view sees each
 * dumbbell in profile (Kit.kt Dumbbell.neutral), where a press shows it end-on.
 */
private val FLY_DUMBBELLS = listOf(Dumbbell(Side.FAR, neutral = true), Dumbbell(Side.NEAR, neutral = true))

/**
 * A dumbbell press bottom: the grip at [at] beside the chest, [z] cm out from the body's middle, with the forearm
 * upright under the dumbbell in the picture and the elbow below it, out to the side: the elbow is where the upper arm
 * from the shoulder meets the forearm in the upright plane through the grip.
 */
private fun pressBottom(s: Skeleton, at: Point, torso: Double, z: Double = 26.0): Arm {
    val sh = sideShoulder(s)
    val a = Segment.UPPER_ARM.length
    val b = FORE_LINK
    val dx = at.x - sh.x
    // In the plane x = at.x: the shoulder's circle (radius rs about (sh.y, sh.z)) meets the forearm's (b about the grip).
    val rs = sqrt(a * a - dx * dx)
    val c1 = Point(sh.y, sh.z)
    val c2 = Point(at.y, z)
    val d = (c2 - c1).length()
    val along = (rs * rs - b * b + d * d) / (2 * d)
    val h = sqrt((rs * rs - along * along).coerceAtLeast(0.0))
    val u = (c2 - c1) * (1.0 / d)
    val base = c1 + u * along
    // Of the two meeting points, the lower one (larger y): the elbow below the dumbbell.
    val e1 = base + Point(-u.y, u.x) * h
    val e2 = base + Point(u.y, -u.x) * h
    val e = if (e1.x > e2.x) e1 else e2
    val elbow = V3(at.x, e.x, e.y)
    return armFrom((elbow - sh).unit(), (v3(at, z) - elbow).unit(), torso)
}

// ---- flat bench ----

/**
 * DB press. Lying on the flat bench as the approved Bench (head, upper back and hips on the bench, feet flat); START
 * the dumbbells at the sides of the chest, END pressed to straight arms over the shoulders.
 * - Bottom: the dumbbells beside the chest, their handles 4 cm above the chest touch point's height and in line with
 *   it ("hold them at the sides of your chest"; ExRx dumbbell bench press,
 *   https://exrx.net/WeightExercises/PectoralSternal/DBBenchPress: lower to the sides of the chest), 24 cm out from the
 *   body's middle (neutral, just outside the torso). The forearm stands under the dumbbell and the elbow is below and
 *   out to the side, so the upper arm is drawn foreshortened (Arm.out).
 * - Top: straight arms over the shoulders (the guide's "ending over your shoulders").
 */
private val dbPress: ExerciseFigure by lazy {
    val chest = flatBare.point(BodyPoint.CHEST)
    val bottom = pressBottom(flatBare, Point(chest.x, chest.y - 4.0), FLAT_TORSO)
    figure(
        View.SIDE, flatPose(bottom), flatPose(LOCKOUT_UP), listOf(Floor, FlatBench()) + DUMBBELLS,
        startCaption = "Sides of your chest",
        endCaption = "Straight, over shoulders",
        thumbnail = Which.END,
        guideLines = listOf(
            "Bring the dumbbells up to your shoulders and lie back. Hold them at the sides of your chest.",
            "Press the dumbbells up until your arms are straight. They move slightly in, ending over your shoulders.",
            "Lower them under control to the sides of your chest.",
        ),
        contacts = LYING_CONTACTS,
        checks = listOf(HeldBeside(BodyPoint.CHEST, 8.0, Which.START), LockoutOverShoulders(Which.END)),
    )
}

/** A fly's fixed slight elbow bend (neutral: "slightly bent"; the checks allow 10–30°). */
private const val FLY_BEND = 18.0

/**
 * A lying fly's top: the dumbbells (or handles) over [over] (side view), nearly together, the arms nearly straight
 * with the elbows slightly bent and pointing out to the sides.
 */
private fun flyTop(s: Skeleton, over: Point, torso: Double): Arm {
    val sh = sideShoulder(s)
    // The grip over the point, 6 cm out from the body's middle (the dumbbells nearly together).
    val dx = over.x - sh.x
    val z = 6.0
    val reach = sqrt(Segment.UPPER_ARM.length * Segment.UPPER_ARM.length + FORE_LINK * FORE_LINK +
        2 * Segment.UPPER_ARM.length * FORE_LINK * cos(FLY_BEND / DEG))
    val dz = z - sh.z
    val up = sqrt(reach * reach - dx * dx - dz * dz)
    return arm3(sh, V3(over.x, sh.y - up, z), V3(0.0, 0.0, 1.0), torso)
}

/**
 * A lying fly's bottom: the arms out to the sides, the grips level with [level]'s height, [ahead] cm along the body
 * from the shoulder, the elbows pointing down (the same fixed bend).
 */
private fun flyBottom(s: Skeleton, level: Double, ahead: Point, torso: Double): Arm {
    val sh = sideShoulder(s)
    val reach = sqrt(Segment.UPPER_ARM.length * Segment.UPPER_ARM.length + FORE_LINK * FORE_LINK +
        2 * Segment.UPPER_ARM.length * FORE_LINK * cos(FLY_BEND / DEG))
    val dx = ahead.x
    val dy = level - sh.y
    val dz = sqrt(reach * reach - dx * dx - dy * dy)
    return arm3(sh, V3(sh.x + dx, level, sh.z + dz), V3(0.0, 1.0, 0.0), torso)
}

private val FLY_LYING_LINES_DB = listOf(
    "Hold the dumbbells over your chest, arms straight but elbows slightly bent.",
    "Breathe in and lower the dumbbells out to the sides in a wide arc, until they are level with your shoulders or chest.",
    "Keep the bend in your elbows the same.",
)

/**
 * DB fly. Lying on the flat bench as the Bench; START the dumbbells over the chest, nearly together, the arms nearly
 * straight (elbows bent 18°, pointing out); END lowered out to the sides in a wide arc until level with the chest.
 * - Bottom: the guide allows "level with your shoulders or chest", and its mistakes line forbids going below the
 *   shoulders. Lying down the chest's surface is higher than the shoulder joint, so the dumbbells stop level with the
 *   chest, the higher, shallower of the two (ACE lying chest fly,
 *   https://www.acefitness.org/resources/everyone/exercise-library/21/lying-chest-fly/: no lower than shoulder level).
 *   They are in line with the shoulders, 6 cm towards the feet (neutral).
 * - The bend: the same 18° in both panels (neutral "slightly bent"; the guide keeps it fixed).
 */
private val dbFly: ExerciseFigure by lazy {
    val chest = flatBare.point(BodyPoint.CHEST)
    val top = flyTop(flatBare, chest, FLAT_TORSO)
    val bottom = flyBottom(flatBare, chest.y, Point(6.0, 0.0), FLAT_TORSO)
    figure(
        View.SIDE, flatPose(top), flatPose(bottom), listOf(Floor, FlatBench()) + FLY_DUMBBELLS,
        startCaption = "Over your chest",
        endCaption = "Level with your chest",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie on a flat bench with a dumbbell in each hand. Put your feet on the floor, or on a raised platform, so your spine stays neutral.",
            "Keep your head, shoulders and buttocks on the bench.",
        ) + FLY_LYING_LINES_DB,
        contacts = LYING_CONTACTS,
        checks = listOf(
            HeldOver(BodyPoint.CHEST, 3.0, Which.START), HandsLevelWith(BodyPoint.CHEST, 3.0, Which.END),
            ElbowBend(10.0, 30.0, Which.START), ElbowBend(10.0, 30.0, Which.END),
        ),
    )
}


/**
 * A low pulley beside a bench, seen from the side behind the lifter: only its lower part is drawn (75 cm), so it reads
 * as the floor-level pulley it is and not as a post through the bench.
 */
private const val LOW_PULLEY = 75.0

/**
 * Lying cable fly. The flat bench square between two low pulleys (Kit.kt CableStack; the far column shows behind the
 * lifter, the near one is left out of the side view), lying as the Bench, a handle in each hand. START the hands out
 * to the sides, level with the shoulders (the guide's limit, ACE's flat-fly rule carried over in its comment); END
 * brought together over the chest in a hugging arc, the elbows' bend fixed.
 */
private val lyingCableFly: ExerciseFigure by lazy {
    val chest = flatBare.point(BodyPoint.CHEST)
    val out = flyBottom(flatBare, flatBare.shoulder.y, Point(4.0, 0.0), FLAT_TORSO)
    val together = flyTop(flatBare, chest, FLAT_TORSO)
    val props = listOf(Floor, CableStack(x = flatBare.shoulder.x, pulleyHeight = 12.0, reach = 0.0, height = LOW_PULLEY, cableInFront = true), FlatBench(), CableHandles)
    figure(
        View.SIDE, flatPose(out), flatPose(together), props,
        startCaption = "Level with shoulders",
        endCaption = "Together over your chest",
        thumbnail = Which.START,
        guideLines = listOf(
            "Set both pulleys low, with a handle on each.",
            "Place a flat bench in the middle, square to the pulleys. Lie on it with a handle in each hand.",
            "Bring the handles together over your chest in a hugging arc. Keep the bend in your elbows fixed.",
            "Let them go back out until your hands are level with your shoulders.",
        ),
        contacts = LYING_CONTACTS,
        checks = listOf(
            HandsLevelWith(BodyPoint.SHOULDERS, 3.0, Which.START), HeldOver(BodyPoint.CHEST, 3.0, Which.END),
            ElbowBend(10.0, 30.0, Which.START), ElbowBend(10.0, 30.0, Which.END),
        ),
    )
}

/**
 * Smith bench press. A flat bench under a Smith machine (Kit.kt SmithMachine), the chest under the bar; START the bar
 * on the chest, END pressed straight up its rails to straight arms. The rails are upright, so the bar locks out
 * straight above the chest, not over the shoulders (ExRx Smith bench press,
 * https://exrx.net/WeightExercises/PectoralSternal/SMBenchPress: lower to the chest, press until the arms are
 * extended). The safety stops (the guide's first line) sit 2 cm under the bar's lowest point, so it can't come down
 * below the chest.
 */
private val smithBench: ExerciseFigure by lazy {
    val chest = flatBare.point(BodyPoint.CHEST)
    val bar = Point(chest.x, chest.y - Barbell.SHAFT_RADIUS)
    val smith = SmithMachine(x = bar.x, stopHeight = -(chest.y + 2.0))
    figure(
        View.SIDE, flatPose(Reach.arm(flatBare.shoulder, bar, FLAT_TORSO)), flatPose(lockoutAbove(flatBare.shoulder, bar.x, FLAT_TORSO)),
        listOf(Floor, smith, FlatBench(), Barbell()),
        startCaption = "Lower the bar to chest",
        endCaption = "Press to straight arms",
        thumbnail = Which.START,
        guideLines = listOf(
            "Set the safety stops so the bar can't come down below your chest.",
            "Lie on the bench with your chest under the bar.",
            "Lower the bar to your chest.",
            "Press the bar up until your arms are straight.",
        ),
        contacts = LYING_CONTACTS,
        checks = listOf(
            BarTouches(BodyPoint.CHEST, Which.START), BarOnRails(Which.START), BarOnRails(Which.END), ElbowBend(0.0, 2.0, Which.END),
        ),
    )
}

// ---- incline bench ----

/**
 * The incline: 45°, the incline DB press guide's own figure (45 to 60°; 45 is its lower, flatter end), used for every
 * incline here. https://pmc.ncbi.nlm.nih.gov/articles/PMC7579505/ compares bench inclinations of 0–60° (the upper
 * pectoralis most active around 30°, the front deltoid taking over above 45°), so 45° is a real incline-press angle,
 * not steeper. The Task 4 adjustable bench.
 */
private const val INCLINE = 45.0
private val inclineBench = AdjustableBench(x = 60.0, backAngle = INCLINE)
private val INCLINE_TORSO = 180.0 - INCLINE

/** Sitting back on a bench whose back pad is [back]° from level: the pelvis touching the seat and the pad. */
private fun seatedPelvis(hinge: Point, back: Double): Point {
    val r = back / DEG
    val t = (Body.TORSO_RADIUS - Body.TORSO_RADIUS * cos(r)) / sin(r)
    return hinge + unit(180.0 - back) * t + unit(90.0 - back) * Body.TORSO_RADIUS
}

private val inclinePelvis by lazy { seatedPelvis(Point(inclineBench.x - AdjustableBench.SEAT_LENGTH, -FlatBench.HEIGHT), INCLINE) }
private val inclineLegs by lazy { Reach.leg(inclinePelvis, Point(inclinePelvis.x + 46.0, -Body.ANKLE_HEIGHT), INCLINE_TORSO, foot = 0.0) }
private fun inclinePose(arm: Arm) = Pose(inclinePelvis.x, inclinePelvis.y, INCLINE_TORSO, nearArm = arm, nearLeg = inclineLegs)
private val inclineBare by lazy { inclinePose(SIDE_ARMS_DOWN).skeleton() }

private val INCLINE_CONTACTS = listOf(
    Contact(BodyPoint.FEET, Surface.FLOOR, BOTH),
    Contact(BodyPoint.HIPS, Surface.SEAT, BOTH),
    Contact(BodyPoint.UPPER_BACK, Surface.BACK_PAD, BOTH),
    Contact(BodyPoint.HEAD, Surface.BACK_PAD, BOTH),
)

/** Straight arms, the grips straight up (world vertical) from the shoulder: a lockout over the shoulders. */
private fun armUp(torso: Double) = Arm(shoulder = fold180(90.0 - torso - 180.0), elbow = 0.0)

/** The bar touching [point] of [s]: its centre one shaft radius off the torso's front, square to it. */
private fun barOn(s: Skeleton, point: BodyPoint): Point = s.point(point) + unit(s.torso - 90.0) * Barbell.SHAFT_RADIUS

/**
 * A rack behind a reclined lifter (the Bench's convention): its posts [behind] the given x, J-hooks 5 cm under the
 * bar's top, safety arms 5 cm under its lowest point and reaching 10 cm past the bar towards the feet.
 */
private fun rackBehind(postX: Double, top: Point, low: Point) = PowerRack(
    x = postX, jHookHeight = -top.y - 5.0, safetyHeight = -low.y - Barbell.SHAFT_RADIUS - 5.0, armLength = low.x - postX + 10.0,
)

/**
 * Incline bench press. Lying back on the 45° bench inside a rack, the safety pins under the chest (the guide's safety
 * line); START the bar lowered to the chest, END held over the upper chest with straight arms (the guide's first step),
 * that is straight above the upper chest touch point. Head, back and hips on the bench, feet flat.
 */
private val inclineBenchPress: ExerciseFigure by lazy {
    val bar = barOn(inclineBare, BodyPoint.CHEST)
    val top = lockoutAbove(inclineBare.shoulder, inclineBare.point(BodyPoint.UPPER_CHEST).x, INCLINE_TORSO)
    val endPose = inclinePose(top)
    val barTop = endPose.skeleton().near.grip
    val padTopX = inclineBench.x - AdjustableBench.SEAT_LENGTH - AdjustableBench.BACK_LENGTH * cos(INCLINE / DEG)
    val rack = rackBehind(padTopX - 6.0, barTop, bar)
    figure(
        View.SIDE, inclinePose(Reach.arm(inclineBare.shoulder, bar, INCLINE_TORSO)), endPose,
        listOf(Floor, rack, inclineBench, Barbell()),
        startCaption = "Lower the bar to chest",
        endCaption = "Over your upper chest",
        thumbnail = Which.END,
        guideLines = listOf(
            RACK_SAFETY, "Lie back on the incline bench.",
            "Lift the bar out and hold it over your upper chest with straight arms.",
            "Lower the bar to your chest.",
        ),
        contacts = INCLINE_CONTACTS,
        checks = listOf(BarTouches(BodyPoint.CHEST, Which.START), HeldOver(BodyPoint.UPPER_CHEST, 2.0, Which.END), ElbowBend(0.0, 2.0, Which.END)),
    )
}

/**
 * Incline DB press. The 45° bench; START the dumbbells at the sides of the upper chest, forearms upright under them;
 * END pressed to straight arms over the shoulders. Head, shoulders and buttocks on the bench, feet on the floor.
 */
private val inclineDbPress: ExerciseFigure by lazy {
    val upper = inclineBare.point(BodyPoint.UPPER_CHEST)
    val bottom = pressBottom(inclineBare, Point(upper.x, upper.y - 4.0), INCLINE_TORSO)
    figure(
        View.SIDE, inclinePose(bottom), inclinePose(armUp(INCLINE_TORSO)), listOf(Floor, inclineBench) + DUMBBELLS,
        startCaption = "Sides of upper chest",
        endCaption = "Over your shoulders",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set the bench to an incline of 45 to 60 degrees.",
            "Keep your head, shoulders and buttocks on the bench and your feet on the floor.",
            "Press the dumbbells up until your arms are straight. They move in a slight arc, ending over your shoulders.",
            "Lower them under control to the sides of your upper chest.",
        ),
        contacts = INCLINE_CONTACTS,
        checks = listOf(HeldBeside(BodyPoint.UPPER_CHEST, 8.0, Which.START), LockoutOverShoulders(Which.END)),
    )
}

/**
 * Incline DB fly. The 45° bench; START the dumbbells over the upper chest, nearly together, the elbows slightly bent
 * and pointing out; END lowered out to the sides of the shoulders until level with them (the guide's stop).
 */
private val inclineDbFly: ExerciseFigure by lazy {
    val upper = inclineBare.point(BodyPoint.UPPER_CHEST)
    figure(
        View.SIDE, inclinePose(flyTop(inclineBare, upper, INCLINE_TORSO)),
        inclinePose(flyBottom(inclineBare, inclineBare.shoulder.y, Point(2.0, 0.0), INCLINE_TORSO)),
        listOf(Floor, inclineBench) + FLY_DUMBBELLS,
        startCaption = "Over your upper chest",
        endCaption = "Level with shoulders",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lie back on an incline bench with a dumbbell in each hand.",
            "Hold the dumbbells over your upper chest, elbows slightly bent and pointing out to the sides.",
            "Lower the dumbbells out to the sides of your shoulders. Keep the bend in your elbows fixed.",
            "When the dumbbells are level with your shoulders, bring them back up in a hugging motion.",
        ),
        contacts = INCLINE_CONTACTS,
        checks = listOf(
            HeldOver(BodyPoint.UPPER_CHEST, 3.0, Which.START), HandsLevelWith(BodyPoint.SHOULDERS, 3.0, Which.END),
            ElbowBend(10.0, 30.0, Which.START), ElbowBend(10.0, 30.0, Which.END),
        ),
    )
}

/**
 * Incline bench cable fly. The 45° bench between two low pulleys (the far column behind); START the handles pressed up
 * over the shoulders, the arms pointing straight up with the elbows slightly bent (the guide's setup); END lowered out
 * to the sides until level with the shoulders.
 */
private val inclineCableFly: ExerciseFigure by lazy {
    val props = listOf(Floor, CableStack(x = inclineBare.shoulder.x, pulleyHeight = 12.0, reach = 0.0, height = LOW_PULLEY, cableInFront = true), inclineBench, CableHandles)
    figure(
        View.SIDE, inclinePose(flyTop(inclineBare, inclineBare.shoulder, INCLINE_TORSO)),
        inclinePose(flyBottom(inclineBare, inclineBare.shoulder.y, Point(2.0, 0.0), INCLINE_TORSO)), props,
        startCaption = "Arms point straight up",
        endCaption = "Level with shoulders",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set an incline bench between two low pulleys.",
            "Press the handles up over your shoulders until your arms point straight up.",
            "Bend your elbows slightly, pointing them out to the sides.",
            "Lower the handles out to the sides of your shoulders. Keep the bend in your elbows fixed.",
        ),
        contacts = INCLINE_CONTACTS,
        checks = listOf(
            HeldOver(BodyPoint.SHOULDERS, 3.0, Which.START), HandsLevelWith(BodyPoint.SHOULDERS, 3.0, Which.END),
            ElbowBend(10.0, 30.0, Which.START), ElbowBend(10.0, 30.0, Which.END),
        ),
    )
}

/**
 * Smith incline press. The 45° bench under the Smith bar; START the bar on the chest, END pressed straight up the
 * rails to straight arms (ExRx Smith incline bench press,
 * https://exrx.net/WeightExercises/PectoralClavicular/SMInclineBenchPress). The stops 2 cm under the bar's lowest point.
 */
private val smithIncline: ExerciseFigure by lazy {
    val bar = barOn(inclineBare, BodyPoint.CHEST)
    val smith = SmithMachine(x = bar.x, stopHeight = -(bar.y + Barbell.SHAFT_RADIUS + 2.0))
    figure(
        View.SIDE, inclinePose(Reach.arm(inclineBare.shoulder, bar, INCLINE_TORSO)), inclinePose(lockoutAbove(inclineBare.shoulder, bar.x, INCLINE_TORSO)),
        listOf(Floor, smith, inclineBench, Barbell()),
        startCaption = "Lower the bar to chest",
        endCaption = "Press to straight arms",
        thumbnail = Which.START,
        guideLines = listOf(
            "Set the safety stops so the bar can't come down below your chest.",
            "Lie back on an incline bench in the Smith machine, with the bar over your chest.",
            "Lower the bar to your chest.",
            "Press the bar up until your arms are straight.",
        ),
        contacts = INCLINE_CONTACTS,
        checks = listOf(BarTouches(BodyPoint.CHEST, Which.START), BarOnRails(Which.START), BarOnRails(Which.END), ElbowBend(0.0, 2.0, Which.END)),
    )
}

// ---- decline bench ----

/**
 * The decline: neutral: a typical decline bench is 15–30°; 25° drawn (no allowed page found that states a decline
 * angle). Lying back with the hips near the high end, the thighs over it and the
 * lower shins hooked under the leg brace's roller (knees bent 105°, neutral).
 */
private const val DECLINE = 25.0
private val DECLINE_TORSO = 180.0 + DECLINE
private val DECLINE_TOP = Point(100.0, -68.0)
private val declinePelvis = DECLINE_TOP + unit(DECLINE_TORSO) * 12.0 + unit(DECLINE_TORSO - 90.0) * Body.TORSO_RADIUS
private val declineLeg = Leg(hip = fold180(35.0 - DECLINE_TORSO - 180.0), knee = 105.0, ankle = -35.0)
private fun declinePose(arm: Arm, neck: Double = 0.0) = Pose(declinePelvis.x, declinePelvis.y, DECLINE_TORSO, neck, arm, declineLeg)
private val declineBare by lazy { declinePose(SIDE_ARMS_DOWN).skeleton() }

/** The decline bench, its roller on the front of the lower shins just above the ankle. */
private val declineBench by lazy {
    val n = declineBare.near
    val shin = angleTo(n.ankle - n.knee)
    val roller = n.ankle + unit(shin + 180.0) * 7.0 + unit(shin + 90.0) * (Body.SHIN_WIDTH / 2 + DeclineBench.ROLLER_RADIUS)
    DeclineBench(x = DECLINE_TOP.x, brace = roller, angle = DECLINE, topHeight = -DECLINE_TOP.y)
}

private val DECLINE_CONTACTS = listOf(
    Contact(BodyPoint.SHINS, Surface.LEG_BRACE, BOTH),
    Contact(BodyPoint.HEAD, Surface.BENCH, BOTH),
    Contact(BodyPoint.UPPER_BACK, Surface.BENCH, BOTH),
    Contact(BodyPoint.HIPS, Surface.BENCH, BOTH),
)

/**
 * Decline bench press. The 25° decline bench inside a rack (the posts beyond the head, the safety pins under the
 * chest: the guide's safety line), the feet hooked under the leg brace; START the bar lowered to the chest, END held
 * over the chest with straight arms (the guide's first step), straight above the chest touch point.
 */
private val declineBenchPress: ExerciseFigure by lazy {
    val bar = barOn(declineBare, BodyPoint.CHEST)
    val endPose = declinePose(lockoutAbove(declineBare.shoulder, declineBare.point(BodyPoint.CHEST).x, DECLINE_TORSO))
    val rack = rackBehind(declineBare.head.x - Body.HEAD_RADIUS - 8.0, endPose.skeleton().near.grip, bar)
    figure(
        View.SIDE, declinePose(Reach.arm(declineBare.shoulder, bar, DECLINE_TORSO)), endPose,
        listOf(Floor, rack, declineBench, Barbell()),
        startCaption = "Lower the bar to chest",
        endCaption = "Over your chest",
        thumbnail = Which.END,
        guideLines = listOf(
            RACK_SAFETY, "Lie back on the decline bench with your feet hooked under the leg brace.",
            "Lift the bar out and hold it over your chest with straight arms.",
            "Lower the bar to your chest.",
        ),
        contacts = DECLINE_CONTACTS,
        checks = listOf(BarTouches(BodyPoint.CHEST, Which.START), HeldOver(BodyPoint.CHEST, 2.0, Which.END), ElbowBend(0.0, 2.0, Which.END)),
    )
}

/**
 * Decline DB press. The decline bench, feet under the brace; START the dumbbells at the sides of the chest with each
 * forearm upright under its dumbbell, the elbows out to the sides; END pressed to straight arms over the shoulders.
 */
private val declineDbPress: ExerciseFigure by lazy {
    val chest = declineBare.point(BodyPoint.CHEST)
    figure(
        View.SIDE, declinePose(pressBottom(declineBare, Point(chest.x, chest.y - 4.0), DECLINE_TORSO)), declinePose(armUp(DECLINE_TORSO)),
        listOf(Floor, declineBench) + DUMBBELLS,
        startCaption = "Sides of your chest",
        endCaption = "Over your shoulders",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit on the decline bench with your feet under the leg brace and the dumbbells resting on your thighs.",
            "Lie back and hold the dumbbells at the sides of your chest, with each forearm under its dumbbell.",
            "Press the dumbbells up, elbows out to the sides, until your arms are straight.",
            "They move in a slight arc, ending over your shoulders.",
        ),
        contacts = DECLINE_CONTACTS,
        checks = listOf(HeldBeside(BodyPoint.CHEST, 8.0, Which.START), LockoutOverShoulders(Which.END)),
    )
}

/**
 * Decline DB fly. The decline bench, feet under the brace; START the dumbbells over the upper abdomen (the guide's
 * point, not over the face), nearly together, elbows slightly bent; END lowered out to the sides until level with the
 * shoulders.
 */
private val declineDbFly: ExerciseFigure by lazy {
    val abdomen = declineBare.point(BodyPoint.UPPER_ABDOMEN)
    figure(
        View.SIDE, declinePose(flyTop(declineBare, abdomen, DECLINE_TORSO)),
        declinePose(flyBottom(declineBare, declineBare.shoulder.y, Point(2.0, 0.0), DECLINE_TORSO)),
        listOf(Floor, declineBench) + FLY_DUMBBELLS,
        startCaption = "Over your upper abdomen",
        endCaption = "Level with shoulders",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit on the decline bench with your feet under the leg brace.",
            "Lie back and hold the dumbbells over your upper abdomen, elbows slightly bent.",
            "Lower the dumbbells out to the sides until they are level with your shoulders. Keep the bend in your elbows fixed.",
        ),
        contacts = DECLINE_CONTACTS,
        checks = listOf(
            HeldOver(BodyPoint.UPPER_ABDOMEN, 3.0, Which.START), HandsLevelWith(BodyPoint.SHOULDERS, 3.0, Which.END),
            ElbowBend(10.0, 30.0, Which.START), ElbowBend(10.0, 30.0, Which.END),
        ),
    )
}

/**
 * Smith decline press. The decline bench under the Smith bar, the lower chest under it; START the bar on the lower
 * chest (the chest touch point, Body.CHEST: lower to mid chest), END pressed straight up the rails (ExRx Smith decline
 * bench press, https://exrx.net/WeightExercises/PectoralSternal/SMDeclineBenchPress). The stops 2 cm under it.
 */
private val smithDecline: ExerciseFigure by lazy {
    val bar = barOn(declineBare, BodyPoint.CHEST)
    val smith = SmithMachine(x = bar.x, stopHeight = -(bar.y + Barbell.SHAFT_RADIUS + 2.0))
    figure(
        View.SIDE, declinePose(Reach.arm(declineBare.shoulder, bar, DECLINE_TORSO)), declinePose(lockoutAbove(declineBare.shoulder, bar.x, DECLINE_TORSO)),
        listOf(Floor, smith, declineBench, Barbell()),
        startCaption = "Bar to your lower chest",
        endCaption = "Press to straight arms",
        thumbnail = Which.START,
        guideLines = listOf(
            "Set the safety stops so the bar can't come down below your chest.",
            "Lie back on a decline bench in the Smith machine, feet under the leg brace and your lower chest under the bar.",
            "Lower the bar to your lower chest.",
            "Press the bar up until your arms are straight.",
        ),
        contacts = DECLINE_CONTACTS,
        checks = listOf(BarTouches(BodyPoint.CHEST, Which.START), BarOnRails(Which.START), BarOnRails(Which.END), ElbowBend(0.0, 2.0, Which.END)),
    )
}

// ---- machines ----

/**
 * The press machines' seat: its top 41 cm up (so this body's thighs lie level on it), the back pad 10° back from upright (neutral: typical selectorised chest
 * presses), the lifter sitting back against it, feet flat. [press] is the direction the handles travel, from level.
 */
private const val MACHINE_SEAT = 41.0
private const val MACHINE_BACK = 80.0
private val MACHINE_TORSO = 180.0 - MACHINE_BACK
private val machineHinge = Point(40.0 - PressMachine.SEAT_LENGTH, -MACHINE_SEAT)
private val machinePelvis by lazy { seatedPelvis(machineHinge, MACHINE_BACK) }
private val machineLegs by lazy { Reach.leg(machinePelvis, Point(machinePelvis.x + 40.0, -Body.ANKLE_HEIGHT), MACHINE_TORSO, foot = 0.0) }
private fun machinePose(arm: Arm) = Pose(machinePelvis.x, machinePelvis.y, MACHINE_TORSO, nearArm = arm, nearLeg = machineLegs)
private val machineBare by lazy { machinePose(SIDE_ARMS_DOWN).skeleton() }

private val MACHINE_CONTACTS = listOf(
    Contact(BodyPoint.FEET, Surface.FLOOR, BOTH),
    Contact(BodyPoint.HIPS, Surface.SEAT, BOTH),
    Contact(BodyPoint.UPPER_BACK, Surface.BACK_PAD, BOTH),
)

/** The press's slight bend at the end: "straight, without locking your elbows" (neutral 10°). */
private const val PRESS_SOFT = 10.0

private fun reachAt(bend: Double) = sqrt(
    Segment.UPPER_ARM.length * Segment.UPPER_ARM.length + FORE_LINK * FORE_LINK +
        2 * Segment.UPPER_ARM.length * FORE_LINK * cos(bend / DEG),
)

/**
 * A seated machine press: the handles start at [start] (side view, [z] out from the body's middle) with the elbows
 * back and out, and travel along [press]° until the arms are straight but soft. The lever's pivot is on the frame
 * above, on the perpendicular bisector of the two handle positions, so the lever is the same length in both.
 */
private fun machinePress(start: Point, press: Double, z: Double, elbowHint: V3): Triple<Arm, Arm, PressMachine> {
    val sh = sideShoulder(machineBare)
    val startArm = arm3(sh, v3(start, z), elbowHint, MACHINE_TORSO)
    // Along the press line until the grip is at the soft-elbow reach.
    val dir = unit(press)
    val reach = reachAt(PRESS_SOFT)
    var t = 0.0
    while ((v3(start + dir * (t + 0.1), z) - sh).length() < reach) t += 0.1
    val end = start + dir * t
    val endArm = arm3(sh, v3(end, z), V3(0.0, 1.0, 0.6), MACHINE_TORSO)
    val mid = (start + end) * 0.5
    val n = unit(press + 90.0)
    val pivot = mid + n * 75.0
    return Triple(startArm, endArm, PressMachine(x = machineHinge.x + PressMachine.SEAT_LENGTH, seatHeight = MACHINE_SEAT, backAngle = MACHINE_BACK, pivot = pivot))
}

/**
 * Machine chest press. Seated, back and shoulder blades on the backrest, feet flat; START the handles level with the
 * middle of the chest, beside it, the elbows back and out to the sides; END pressed straight forward until the arms
 * are straight, without locking the elbows (10° left). ExRx lever chest press
 * (https://exrx.net/WeightExercises/PectoralSternal/LVChestPress): press the handles forward until the arms are
 * extended. The handles start 2 cm in front of the chest's surface, 26 cm out (neutral).
 */
private val machineChestPress: ExerciseFigure by lazy {
    val chest = machineBare.point(BodyPoint.CHEST)
    val (a, b, machine) = machinePress(Point(chest.x + 2.0, chest.y), press = 0.0, z = 26.0, elbowHint = V3(-1.0, 0.3, 0.9))
    figure(
        View.SIDE, machinePose(a), machinePose(b), listOf(Floor, machine),
        startCaption = "Level with mid chest",
        endCaption = "Without locking elbows",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set the seat so the handles are level with the middle of your chest.",
            "Sit with your back and shoulder blades against the backrest.",
            "Breathe out and press the handles forward until your arms are straight, without locking your elbows.",
            "Let the handles come back slowly to the start, level with the middle of your chest.",
        ),
        contacts = MACHINE_CONTACTS,
        checks = listOf(HandsLevelWith(BodyPoint.CHEST, 2.0, Which.START), ElbowBend(5.0, 15.0, Which.END)),
    )
}

/**
 * Incline machine press. Seated against the backrest; START the handles just below the upper chest ("your upper chest
 * is just above the handles"), gripped wide with the elbows out to the sides; END pressed up and forward along the
 * machine's incline, 35° above level (neutral; ExRx lever incline chest press,
 * https://exrx.net/WeightExercises/PectoralClavicular/LVInclineChestPress: press upward and forward), until the arms
 * are straight without locking.
 */
private val inclineMachinePress: ExerciseFigure by lazy {
    val upper = machineBare.point(BodyPoint.UPPER_CHEST)
    val (a, b, machine) = machinePress(Point(upper.x + 2.0, upper.y + 4.0), press = 35.0, z = 30.0, elbowHint = V3(-0.8, 0.3, 1.0))
    figure(
        View.SIDE, machinePose(a), machinePose(b), listOf(Floor, machine),
        startCaption = "Just below upper chest",
        endCaption = "Press until straight",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set the seat so your upper chest is just above the handles.",
            "Sit with your back against the backrest.",
            "Grip the handles wide, elbows out to the sides, then let go of the foot lever.",
            "Press until your arms are straight, without locking your elbows.",
        ),
        contacts = MACHINE_CONTACTS,
        checks = listOf(HandsLevelWith(BodyPoint.UPPER_CHEST, 6.0, Which.START), ElbowBend(5.0, 15.0, Which.END)),
    )
}

/**
 * Decline machine press. Seated against the backrest; START the handles about level with the lower chest, gripped
 * wide with the elbows out to the sides just below the shoulders; END pressed forward and down along the machine's
 * decline, 20° below level (neutral; ExRx lever decline chest press,
 * https://exrx.net/WeightExercises/PectoralSternal/LVDeclineChestPress: press forward and downward), until straight.
 */
private val declineMachinePress: ExerciseFigure by lazy {
    val chest = machineBare.point(BodyPoint.CHEST)
    val (a, b, machine) = machinePress(Point(chest.x + 2.0, chest.y + 3.0), press = -20.0, z = 30.0, elbowHint = V3(-0.5, 0.15, 1.0))
    figure(
        View.SIDE, machinePose(a), machinePose(b), listOf(Floor, machine),
        startCaption = "Level with lower chest",
        endCaption = "Press until straight",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set the seat so the handles are about level with your lower chest.",
            "Grip the handles wide and overhand, elbows out to the sides just below your shoulders.",
            "Press until your arms are straight.",
        ),
        contacts = MACHINE_CONTACTS,
        checks = listOf(HandsLevelWith(BodyPoint.CHEST, 5.0, Which.START), ElbowBend(5.0, 15.0, Which.END)),
    )
}

// ---- standing cable press ----

/**
 * Cable chest press. Standing between the pulleys with the back to the machine (the column behind; its pulleys at
 * shoulder height), in a split stance, the near foot forward; START the handles beside the chest, the elbows out to
 * the sides; END pressed straight forward until the arms are straight and level (at shoulder height). Torso upright
 * (ExRx cable standing chest press, https://exrx.net/WeightExercises/PectoralSternal/CBStandingChestPress). The
 * stance (feet about 30 cm in front of and 35 cm behind the hips) is neutral.
 */
private val cableChestPress: ExerciseFigure by lazy {
    val torso = 88.0
    val pelvis = Point(0.0, -84.0)
    val front = Reach.leg(pelvis, Point(pelvis.x + 30.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
    val back = Reach.leg(pelvis, Point(pelvis.x - 34.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, torso, nearArm = arm, nearLeg = front, farLeg = back)
    val bare = pose(SIDE_ARMS_DOWN).skeleton()
    val chest = bare.point(BodyPoint.CHEST)
    val sh = sideShoulder(bare)
    val start = arm3(sh, V3(chest.x + 3.0, chest.y, 26.0), V3(-1.0, 0.3, 0.9), torso)
    val dz = 4.0
    val ahead = sqrt(ARM_REACH * ARM_REACH - dz * dz)
    val end = arm3(sh, V3(sh.x + ahead, sh.y, sh.z - dz), V3(0.0, 1.0, 0.0), torso)
    val stack = CableStack(x = pelvis.x - 70.0, pulleyHeight = -bare.shoulder.y, reach = 20.0)
    figure(
        View.SIDE, pose(start), pose(end), listOf(Floor, stack, CableHandles),
        startCaption = "Beside your chest",
        endCaption = "Arms straight and level",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set both pulleys at shoulder height, with a handle on each.",
            "Take a split stance, one foot forward and one back.",
            "Hold the handles beside your chest, elbows out to the sides.",
            "Press the handles forward until your arms are straight and level.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(HeldBeside(BodyPoint.CHEST, 8.0, Which.START), HandsLevelWith(BodyPoint.SHOULDERS, 2.0, Which.END), ElbowBend(0.0, 2.0, Which.END)),
    )
}

// ---- front view: standing and seated flys ----

/** The front view's shoulders, for a torso upright at x = [cx], its shoulder joints at height [y] (world y), [z] forward. */
private fun frontShoulder(cx: Double, y: Double, z: Double = 0.0) = V3(cx + Body.SHOULDER_HALF_WIDTH, y, z)

/**
 * A front-view pair of arms, the far one the near one's mirror image (both drawn by the near side's angles): the grip
 * at [grip] (3D, the near side's), the elbow towards [hint].
 */
private fun frontArms(cx: Double, shoulderY: Double, shoulderZ: Double, grip: V3, hint: V3): Arm =
    arm3(frontShoulder(cx, shoulderY, shoulderZ), grip, hint, 90.0)

/** Standing with the hips and knees slightly bent: front view, the knees [kneesOut]° forward, the torso [lean]° forward. */
private fun frontStanding(cx: Double, lean: Double, kneesOut: Double, arm: Arm): Pose {
    // Hips 5° apart; the shin stands upright under the knee.
    val leg = Leg(hip = 5.0, knee = 5.0, out = kneesOut)
    val drop = Segment.THIGH.length * cos(kneesOut / DEG) * cos(5.0 / DEG) + Segment.SHIN.length
    return Pose(cx, -(Body.ANKLE_HEIGHT + drop), 90.0, nearArm = arm, nearLeg = leg, lean = lean)
}

/** Seated, front view: the thighs pointing at the camera (80° out of the plane), the shins upright, feet flat. */
private val SEATED_FRONT_LEG = Leg(hip = 10.0, knee = 10.0, out = 80.0)
private val SEATED_FRONT_PELVIS_Y by lazy {
    -(Body.ANKLE_HEIGHT + Segment.THIGH.length * cos(80.0 / DEG) * cos(10.0 / DEG) + Segment.SHIN.length)
}
private fun frontSeated(arm: Arm) = Pose(0.0, SEATED_FRONT_PELVIS_Y, 90.0, nearArm = arm, nearLeg = SEATED_FRONT_LEG)

/** The near grip, out to the side of the shoulder at [dy] below its height and [dz] in front, at the fly's reach. */
private fun flyOut(sh: V3, dy: Double, dz: Double): V3 {
    val r = reachAt(FLY_BEND)
    return V3(sh.x + sqrt(r * r - dy * dy - dz * dz), sh.y + dy, sh.z + dz)
}

/** The near grip in front, [half] cm right of the middle at [dy] below the shoulder, at the fly's reach. */
private fun flyIn(sh: V3, cx: Double, half: Double, dy: Double): V3 {
    val r = reachAt(FLY_BEND)
    val dx = cx + half - sh.x
    return V3(cx + half, sh.y + dy, sh.z + sqrt(r * r - dx * dx - dy * dy))
}

/** The elbow's way at the start of a fly (arms out): back and a little down, so the bend opens towards the front. */
private val FLY_OUT_ELBOW = V3(0.0, 0.4, -1.0)

/** At the end (hands together in front): out to the side and a little down, the same bend. */
private val FLY_IN_ELBOW = V3(1.0, 0.4, 0.0)

/**
 * Cable crossover. Front view: standing between two high pulleys (drawn closer than a real 3–4 m station, Kit.kt), a
 * handle in each hand, the hips and knees slightly bent; START the arms out to the sides, the hands a little above
 * shoulder height towards the pulleys; END brought together in front in a hugging arc, low in front of the chest
 * (the alias's "high to low"), the elbows' slight bend fixed. "Stay upright": the torso leans 8° (neutral).
 * ExRx cable standing fly (https://exrx.net/WeightExercises/PectoralSternal/CBStandingFly) and ACE standing chest fly.
 */
private val cableCrossover: ExerciseFigure by lazy {
    val lean = 8.0
    val bare = frontStanding(0.0, lean, 12.0, SIDE_ARMS_DOWN)
    val s = bare.skeleton(View.FRONT)
    val shY = s.near.shoulder.y
    val shZ = Segment.TORSO.length * sin(lean / DEG)
    val sh = frontShoulder(0.0, shY, shZ)
    val chestY = shY + Body.CHEST_BELOW_SHOULDER
    val start = frontArms(0.0, shY, shZ, flyOut(sh, -6.0, 6.0), FLY_OUT_ELBOW)
    val end = frontArms(0.0, shY, shZ, flyIn(sh, 0.0, 4.0, chestY + 8.0 - shY), FLY_IN_ELBOW)
    val props = listOf(Floor, CableStack(x = 0.0, pulleyHeight = 200.0, reach = 10.0, halfSpan = 105.0), CableHandles)
    figure(
        View.FRONT, bare.copy(nearArm = start, farArm = start), bare.copy(nearArm = end, farArm = end), props,
        startCaption = "Arms out to the sides",
        endCaption = "Hands together in front",
        thumbnail = Which.START,
        guideLines = listOf(
            "Set both pulleys high, with a handle on each.",
            "Hold one handle in each hand and step forward, so your arms are out to the sides.",
            "Bend your hips and knees slightly, and bend your elbows slightly.",
            "Bring your hands together in front of you in a hugging arc. Keep the bend in your elbows fixed.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(HandsTogether(12.0, Which.END), ElbowBend(10.0, 30.0, Which.START), ElbowBend(10.0, 30.0, Which.END)),
    )
}

/**
 * Low-to-high cable fly. Front view: standing upright between two low pulleys, between knee and hip height (65 cm,
 * neutral); START the arms down and out to the sides; END brought forward and up in an arc until the hands meet at
 * about face height, the elbows' slight bend kept. ExRx cable standing incline fly
 * (https://exrx.net/WeightExercises/PectoralClavicular/CBStandingInclineFly).
 */
private val lowToHighFly: ExerciseFigure by lazy {
    val bare = frontStanding(0.0, 0.0, 6.0, SIDE_ARMS_DOWN)
    val s = bare.skeleton(View.FRONT)
    val shY = s.near.shoulder.y
    val sh = frontShoulder(0.0, shY)
    val start = frontArms(0.0, shY, 0.0, flyOut(sh, 38.0, 10.0), V3(0.0, -0.2, -1.0))
    val end = frontArms(0.0, shY, 0.0, flyIn(sh, 0.0, 4.0, s.head.y - shY), V3(1.0, 0.5, 0.0))
    val props = listOf(Floor, CableStack(x = 0.0, pulleyHeight = 65.0, reach = 10.0, halfSpan = 105.0), CableHandles)
    figure(
        View.FRONT, bare.copy(nearArm = start, farArm = start), bare.copy(nearArm = end, farArm = end), props,
        startCaption = "Arms down and out",
        endCaption = "Meet at face height",
        thumbnail = Which.END,
        guideLines = listOf(
            "Set both pulleys low, between knee and hip height, with a handle on each.",
            "Start with your arms down and out to your sides, elbows slightly bent.",
            "Bring your hands forward and up in an arc, until they meet at about face height.",
            "Keep a slight bend in your elbows.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(
            HandsTogether(12.0, Which.END), HandsLevelWith(BodyPoint.HEAD, 6.0, Which.END),
            ElbowBend(10.0, 30.0, Which.START), ElbowBend(10.0, 30.0, Which.END),
        ),
    )
}

/**
 * Seated cable fly. Front view: seated between two pulleys at shoulder height, a handle on each side; START the arms
 * out to the sides with the elbows high, level with the shoulders; END brought together in front in a hugging arc,
 * still high. ExRx cable seated fly (https://exrx.net/WeightExercises/PectoralSternal/CBSeatedFly).
 */
private val seatedCableFly: ExerciseFigure by lazy {
    val bare = frontSeated(SIDE_ARMS_DOWN)
    val s = bare.skeleton(View.FRONT)
    val shY = s.near.shoulder.y
    val sh = frontShoulder(0.0, shY)
    val start = frontArms(0.0, shY, 0.0, flyOut(sh, 2.0, 4.0), V3(0.0, 0.0, -1.0))
    val end = frontArms(0.0, shY, 0.0, flyIn(sh, 0.0, 4.0, 4.0), V3(1.0, 0.0, 0.0))
    val seatTop = -(SEATED_FRONT_PELVIS_Y + Body.TORSO_RADIUS)
    val props = listOf(Floor, CableStack(x = 0.0, pulleyHeight = -shY, reach = 10.0, halfSpan = 105.0), Seat(x = 0.0, height = seatTop), CableHandles)
    figure(
        View.FRONT, bare.copy(nearArm = start, farArm = start), bare.copy(nearArm = end, farArm = end), props,
        startCaption = "Hold a handle each side",
        endCaption = "Together, elbows high",
        thumbnail = Which.START,
        guideLines = listOf(
            "Sit on the seat and hold a handle on each side.",
            "Bend your elbows slightly.",
            "Keep your elbows high and bring the handles together in a hugging arc. Keep the bend in your elbows fixed.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH), Contact(BodyPoint.HIPS, Surface.SEAT, BOTH)),
        checks = listOf(
            HandsTogether(12.0, Which.END), HandsLevelWith(BodyPoint.SHOULDERS, 6.0, Which.START), HandsLevelWith(BodyPoint.SHOULDERS, 6.0, Which.END),
            ElbowBend(10.0, 30.0, Which.START), ElbowBend(10.0, 30.0, Which.END),
        ),
    )
}

/**
 * Pec deck. Front view: seated with the back on the pad (it stands behind the torso; the front view cannot show the
 * contact, so it is drawn, not checked), gripping the handles at shoulder height, the elbows slightly bent; START the
 * arms out to the sides; END brought forward and together in front of the chest. The machine's arms hang from pivots
 * above the shoulders (ExRx lever pec deck fly, https://exrx.net/WeightExercises/PectoralSternal/LVPecDeckFly).
 */
private val pecDeck: ExerciseFigure by lazy {
    val bare = frontSeated(SIDE_ARMS_DOWN)
    val s = bare.skeleton(View.FRONT)
    val shY = s.near.shoulder.y
    val sh = frontShoulder(0.0, shY)
    val start = frontArms(0.0, shY, 0.0, flyOut(sh, 0.0, 2.0), V3(0.0, 0.3, -1.0))
    val end = frontArms(0.0, shY, 0.0, flyIn(sh, 0.0, 5.0, 0.0), V3(1.0, 0.3, 0.0))
    val seatTop = -(SEATED_FRONT_PELVIS_Y + Body.TORSO_RADIUS)
    val machine = PecDeck(x = 0.0, seatHeight = seatTop, padTop = -shY + 12.0, pivotHeight = -s.head.y + 22.0)
    figure(
        View.FRONT, bare.copy(nearArm = start, farArm = start), bare.copy(nearArm = end, farArm = end), listOf(Floor, machine),
        startCaption = "At shoulder height",
        endCaption = "Forward and together",
        thumbnail = Which.START,
        guideLines = listOf(
            "Sit with your back on the pad.",
            "Grip the handles at shoulder height. On a machine with arm pads, rest your forearms on the pads.",
            "Bend your elbows slightly.",
            "Bring the handles forward and together.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH), Contact(BodyPoint.HIPS, Surface.SEAT, BOTH)),
        checks = listOf(
            HandsTogether(14.0, Which.END), HandsLevelWith(BodyPoint.SHOULDERS, 4.0, Which.START), HandsLevelWith(BodyPoint.SHOULDERS, 4.0, Which.END),
            ElbowBend(10.0, 30.0, Which.START), ElbowBend(10.0, 30.0, Which.END),
        ),
    )
}

// ---- dips ----

/**
 * The dips' forward lean at the bottom. https://pmc.ncbi.nlm.nih.gov/articles/PMC9603242/ (bench, bar and ring dips)
 * finds the bar dip leans the thorax forward clearly more than the bench dip's 12.6° (±6.5°, from vertical); no
 * page seen gives the bar-dip value, so this is neutral: 32° drawn.
 */
private const val DIP_LEAN = 32.0

/** The bottom's upper arms flare out to the sides ("let your elbows flare out"): drawn 30° out of the plane (neutral). */
private const val DIP_FLARE = 30.0

/**
 * A dip's two poses with the grip fixed at [grip]: START arms straight, the shoulders over the hands, the torso
 * [topLean]° forward; END the upper arms level, the forearm upright, the torso [DIP_LEAN]° forward. [leg] is START's;
 * the legs keep their angle to the floor as the torso tips forward, so only the hip angle changes.
 */
private fun dipPoses(grip: Point, topLean: Double, leg: Leg): Pair<Pose, Pose> {
    val topShoulder = grip + Point(0.0, -ARM_REACH)
    val topTorso = 90.0 - topLean
    val topPelvis = topShoulder - unit(topTorso) * Segment.TORSO.length
    val start = Pose(topPelvis.x, topPelvis.y, topTorso, nearArm = Arm(fold180(-90.0 - topTorso - 180.0), 0.0), nearLeg = leg)
    val elbow = grip + Point(0.0, -FORE_LINK)
    val shoulder = elbow + Point(Segment.UPPER_ARM.length * cos(DIP_FLARE / DEG), 0.0)
    val torso = 90.0 - DIP_LEAN
    val pelvis = shoulder - unit(torso) * Segment.TORSO.length
    val bottom = Arm(shoulder = fold180(180.0 - torso - 180.0), elbow = fold180(-90.0 - 180.0), out = DIP_FLARE)
    return start to Pose(pelvis.x, pelvis.y, torso, nearArm = bottom, nearLeg = leg.copy(hip = leg.hip + topTorso - torso))
}

private val DIP_LINES = listOf(
    "Lower yourself by bending your arms. Let your elbows flare out to the sides.",
    "Stop when your upper arms are level with the floor, then push back up until your arms are straight.",
)

private val DIP_CHECKS = listOf(
    ShouldersOverHands(5.0, Which.START), ElbowBend(0.0, 2.0, Which.START), UpperArmLevel(5.0, Which.END),
)

/**
 * Chest dip. Parallel bars (Kit.kt DipBars, 120 cm); START pushed up on straight arms, the shoulders over the hands,
 * knees and hips slightly bent; END lowered until the upper arms are level with the floor (the guide's stop), the
 * elbows flared out, the forearms upright and the torso leaning 32° forward (neutral; bar dips lean clearly more than
 * bench dips' 12.6°, PMC9603242). ExRx chest dip (https://exrx.net/WeightExercises/PectoralSternal/ChestDip).
 */
private val chestDip: ExerciseFigure by lazy {
    val bars = DipBars(x = 0.0)
    val (start, end) = dipPoses(Point(0.0, -bars.height), topLean = 8.0, leg = Leg(hip = 20.0, knee = 45.0, ankle = -25.0))
    figure(
        View.SIDE, start, end, listOf(Floor, bars),
        startCaption = "Push up to straight arms",
        endCaption = "Upper arms level",
        thumbnail = Which.END,
        guideLines = listOf("Grip the bars and push up until your arms are straight.", "Bend your knees and hips slightly.") + DIP_LINES,
        contacts = listOf(Contact(BodyPoint.HANDS, Surface.DIP_BAR, BOTH)),
        checks = DIP_CHECKS,
    )
}

/**
 * Assisted dip. The machine's column behind, its dip handles at 120 cm, the lifter standing on the assist step with
 * the hips and knees bent (the guide's set-up; the knees under the hips, the shins back, the feet flat on the step);
 * START arms straight with the shoulders over the hands, END the upper arms level, as the chest dip. The step rides
 * down with the body on its lever (Kit.kt AssistedDipMachine), whose pivot keeps it the same length.
 */
private val assistedDip: ExerciseFigure by lazy {
    val grip = Point(0.0, -120.0)
    val leg = Leg(hip = 70.0, knee = 85.0, ankle = 25.0)
    val (start, end) = dipPoses(grip, topLean = 8.0, leg = leg)
    fun step(p: Pose) = p.skeleton().near.let { Point((it.heel.x + it.toe.x) / 2, maxOf(it.heel.y, it.toe.y) + Body.FOOT_WIDTH / 2) }
    val s0 = step(start)
    val s1 = step(end)
    val column = grip.x - 55.0
    // The pivot on the column, on the two step positions' perpendicular bisector.
    val mid = (s0 + s1) * 0.5
    val d = s1 - s0
    val pivot = Point(column + 9.0, mid.y + (mid.x - (column + 9.0)) * d.x / d.y)
    fun machine(st: Point) = AssistedDipMachine(x = column, handleHeight = -grip.y, handleEnd = grip.x + 15.0, stepX = st.x, stepHeight = -st.y, pivot = pivot)
    figure(
        View.SIDE, start, end, listOf(Floor, machine(s0)),
        endProps = listOf(Floor, machine(s1)),
        startCaption = "Shoulders over hands",
        endCaption = "Upper arms level",
        thumbnail = Which.END,
        guideLines = listOf(
            "Grip the bars with your arms straight and your shoulders over your hands.",
            "Step down onto the assist lever with your hips and knees bent. On some machines you kneel on a pad instead.",
        ) + DIP_LINES,
        contacts = listOf(Contact(BodyPoint.HANDS, Surface.DIP_BAR, BOTH), Contact(BodyPoint.FEET, Surface.ASSIST_STEP, BOTH)),
        checks = DIP_CHECKS,
    )
}

// ---- push-ups ----

/**
 * The push-up hand: flat would need the wrist bent back about 90°, past the figure's 60° limit (Body.kt, the
 * conservative wrist range), so the hand is drawn bent [PUSH_UP_WRIST]° from the forearm, the heel of the palm a little
 * raised, the fingers on the floor (on a bench's top, with the arm leaning towards the head, it lies flat).
 */
private const val PUSH_UP_WRIST = 60.0

/** Hands "about shoulder-width apart, or slightly wider": 10 cm wider each side than the shoulder joints (neutral). */
private const val HANDS_OUT = Body.SHOULDER_HALF_WIDTH + 10.0

/** A knee push-up's lower legs: back along the floor, 8° up from it (the feet just clear of it; neutral). */
private const val KNEE_SHIN = 172.0

/** How a push-up's lower body rests: on the toes on the floor or on a bench top, or on the knees. */
private enum class Support { TOES, KNEES }

/**
 * A prone pose for a body straight from the head to the [support] ([torso]° above level, facing +x), its feet or
 * knees resting on a surface [supportHeight] up at x = 0, the arm [arm].
 */
private fun pronePose(torso: Double, support: Support, supportHeight: Double, arm: Arm, neck: Double = 0.0): Pose {
    val leg = when (support) {
        Support.TOES -> Leg(hip = 0.0, knee = 0.0, ankle = 0.0)
        // The lower legs lie back along the floor, 8° up from it, the same in both panels: the knee is the pivot.
        Support.KNEES -> Leg(hip = 0.0, knee = torso + 180.0 - KNEE_SHIN, ankle = -50.0)
    }
    val raw = Pose(0.0, 0.0, torso, neck, arm, leg).skeleton()
    val n = raw.near
    val (x, bottom) = when (support) {
        Support.TOES -> n.toe.x to (maxOf(n.toe.y, n.heel.y) + Body.FOOT_WIDTH / 2)
        // The thigh (the thicker) rests on the floor just above the knee; the knee itself is 1 cm over it.
        Support.KNEES -> n.knee.x to (n.knee.y + Body.SHIN_WIDTH / 2 + 1.1)
    }
    return Pose(-x, -supportHeight - bottom, torso, neck, arm, leg)
}

/**
 * The hand's angle that puts its fingers on a surface whose top is at world y [surface], for the wrist at [w] and a
 * forearm drawn at [fore]° (the hand drawn [scale] of its length): of the two ways it can touch, the one the wrist
 * bends to least; a flat hand ([flat]) stays flat.
 */
private fun handOn(w: Point, fore: Double, scale: Double, surface: Double, flat: Boolean): Double {
    if (flat) return 0.0
    val v = ((surface - Body.HAND_WIDTH / 2 - w.y) / (Segment.HAND.length * scale)).coerceIn(-1.0, 1.0)
    val right = -asin(v) * DEG
    val left = 180.0 - right
    return listOf(right, left).minBy { abs(fold180(it - fore)) }
}

/** The arm from the shoulder of [p] whose wrist lands on [wrist] (3D), the elbow towards [hint], the hand on [surface]. */
private fun pushArm(p: Pose, wrist: V3, hint: V3, surface: Double, flat: Boolean): Arm {
    val sh = sideShoulder(p.skeleton())
    val a = Segment.UPPER_ARM.length
    val b = Segment.FOREARM.length
    val g = wrist - sh
    val d = g.length()
    val gu = g.unit()
    val alpha = acos(((a * a + d * d - b * b) / (2 * a * d)).coerceIn(-1.0, 1.0))
    val h = hint - gu * hint.dot(gu)
    val elbow = sh + (gu * cos(alpha) + h.unit() * sin(alpha)) * a
    val arm = armFrom((elbow - sh).unit(), (wrist - elbow).unit(), p.torso)
    val fore = angleTo((wrist - elbow).flat)
    val hand = handOn(wrist.flat, fore, cos(arm.forearmOut / DEG), surface, flat)
    // Past the wrist's limit the hand stays at it: the heel of the palm on the surface, the fingers a little raised.
    return arm.copy(wrist = fold180(hand - fore).coerceIn(-70.0, PUSH_UP_WRIST))
}

/** The bottom of a hand with its wrist at [w], pointing [hand]° and drawn [scale] of its length: its lowest y plus its radius. */
private fun handBottom(w: Point, hand: Double, scale: Double = 1.0): Double =
    maxOf(w.y, (w + unit(hand) * (Segment.HAND.length * scale)).y) + Body.HAND_WIDTH / 2

/** The torso angle in [lo]..[hi] where [f] crosses zero (bisection; [f] rises with the angle). */
private fun solveAngle(lo: Double, hi: Double, f: (Double) -> Double): Double {
    var a = lo
    var b = hi
    val rising = f(hi) > f(lo)
    repeat(60) {
        val m = (a + b) / 2
        if ((f(m) > 0) == rising) b = m else a = m
    }
    return (a + b) / 2
}

/**
 * A push-up's two poses. START at the top: straight arms from the shoulders along [armAngle]° (world; -90 is straight
 * down) to the hands on a surface [handsHeight] up, the body straight, the hand at [hand]°; END the body lowered (the
 * hands and feet staying put) until [bottom] is met, the elbows bent up and out to the sides.
 */
private class PushUp(val start: Pose, val end: Pose, val wrist: Point)

private fun pushUp(
    support: Support,
    supportHeight: Double,
    handsHeight: Double,
    armAngle: (Double) -> Double,
    hand: Double,
    bottom: (Skeleton) -> Double,
    neckEnd: Double = 0.0,
): PushUp {
    val armLen = Segment.UPPER_ARM.length + Segment.FOREARM.length
    val zOut = HANDS_OUT - Body.SHOULDER_HALF_WIDTH
    val armDrawn = sqrt(armLen * armLen - zOut * zOut)
    fun topArm(torso: Double) = Arm(fold180(armAngle(torso) - torso - 180.0), 0.0, wrist = fold180(hand - armAngle(torso)))
        .copy(out = asin(zOut / armLen) * DEG, forearmOut = asin(zOut / armLen) * DEG)
    fun topWrist(torso: Double): Point {
        val s = pronePose(torso, support, supportHeight, topArm(torso)).skeleton()
        return s.shoulder + unit(armAngle(torso)) * armDrawn
    }
    val scale = cos(asin(zOut / armLen))
    val top = solveAngle(-40.0, 60.0) { t -> -(handBottom(topWrist(t), hand, scale) + handsHeight) }
    val start = pronePose(top, support, supportHeight, topArm(top))
    val w = topWrist(top)
    val w3 = V3(w.x, w.y, HANDS_OUT)
    fun low(t: Double) = pronePose(t, support, supportHeight, SIDE_ARMS_DOWN, neckEnd)
    val t = solveAngle(top - 40.0, top) { a -> bottom(low(a).skeleton()) }
    val lowPose = low(t)
    val bent = pushArm(lowPose, w3, V3(-0.6, -1.0, 0.9), -handsHeight, flat = hand == 0.0)
    val end = lowPose.copy(nearArm = bent, farArm = bent)
    return PushUp(start, end, w)
}

private val PUSH_UP_CONTACTS_TOES = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH), Contact(BodyPoint.HANDS, Surface.FLOOR, BOTH))

/**
 * The push-ups' depth: the chest lowered to 6 cm off the floor ("toward the floor"; ACE lowers until the chest reaches
 * it, so 6 cm is the shallower, conservative side, and keeps the elbow inside its range with this body's arms).
 */
private const val CHEST_GAP = 6.0
private val floorPushUp by lazy {
    pushUp(Support.TOES, 0.0, 0.0, { -90.0 }, -90.0 + PUSH_UP_WRIST, bottom = { -it.chest().y - CHEST_GAP })
}

private const val PUSH_UP_BRACE_LINE = "Brace your trunk and squeeze your glutes and thighs. Keep your head in line with your spine."

/**
 * Push-up. Side view; START on the toes with straight arms, the hands under the shoulders, the body in one straight
 * line; END lowered with the body still straight until the chest is 6 cm off the floor, the hands and toes staying put
 * and the elbows bending up and out to the sides. Hands under the shoulders and the depth: ACE's push-up and bent-knee
 * push-up (https://www.acefitness.org/resources/everyone/exercise-library/13/bent-knee-push-up/: lower until the chest
 * reaches the floor) and ExRx push-up (https://exrx.net/WeightExercises/PectoralSternal/BWPushup: body straight).
 */
private val pushUpFigure: ExerciseFigure by lazy {
    figure(
        View.SIDE, floorPushUp.start, floorPushUp.end, listOf(Floor),
        startCaption = "Body in a straight line",
        endCaption = "Chest toward the floor",
        thumbnail = Which.START,
        guideLines = listOf(
            "Push up onto your toes with straight arms, so your body is in a straight line.",
            PUSH_UP_BRACE_LINE,
            "Bend your elbows to lower your chest toward the floor. Keep your body straight.",
            "Push the floor away until your elbows are straight.",
        ),
        contacts = PUSH_UP_CONTACTS_TOES,
        checks = listOf(ChestOffFloor(4.0, 9.0, Which.END), ElbowBend(0.0, 2.0, Which.START)),
    )
}

/**
 * Weighted push-up. The push-up with two plates lying on the upper back (Kit.kt BackPlates); START the top, END the
 * chest lowered towards the floor, the body rigid. The partner who puts the plates on is left out.
 */
private val weightedPushUp: ExerciseFigure by lazy {
    figure(
        View.SIDE, floorPushUp.start, floorPushUp.end, listOf(Floor, BackPlates()),
        startCaption = "Plates on your back",
        endCaption = "Lower your chest",
        thumbnail = Which.START,
        guideLines = listOf(
            "Get into a push-up position, hands slightly wider than your shoulders, body straight.",
            "A partner places weight plates on your back.",
            "Bend your elbows to lower your chest. Keep your body rigid.",
            "Push up until your arms are straight.",
        ),
        contacts = PUSH_UP_CONTACTS_TOES,
        checks = listOf(ChestOffFloor(4.0, 9.0, Which.END), ElbowBend(0.0, 2.0, Which.START)),
    )
}

/**
 * Clap push-up. START the chest lowered towards the floor (the push-up's bottom); END pushed up so fast that the hands
 * leave the floor: the body straight and pivoted up on the toes, the arms straight, the hands 8 cm in the air and
 * together under the chest for the clap (neutral heights; ExRx clap push-up,
 * https://exrx.net/WeightExercises/PectoralSternal/BWClapPushup).
 */
private val clapPushUp: ExerciseFigure by lazy {
    val armLen = Segment.UPPER_ARM.length + Segment.FOREARM.length
    val zIn = Body.SHOULDER_HALF_WIDTH - 4.0
    val out = -asin(zIn / armLen) * DEG
    val air = Arm(0.0, 0.0, wrist = 30.0, out = out, forearmOut = out)
    fun pose(t: Double) = pronePose(t, Support.TOES, 0.0, air.copy(shoulder = fold180(-90.0 - t - 180.0)))
    fun handsUp(t: Double) = pose(t).skeleton().near.let { -maxOf(it.wrist.y, it.hand.y) - Body.HAND_WIDTH / 2 }
    val t = solveAngle(0.0, 60.0) { handsUp(it) - 8.0 }
    figure(
        View.SIDE, floorPushUp.end, pose(t), listOf(Floor),
        startCaption = "Chest toward the floor",
        endCaption = "Hands leave the floor",
        thumbnail = Which.END,
        guideLines = listOf(
            "Lower your chest toward the floor.",
            "Push up as fast as you can, so your hands leave the floor.",
            "Clap quickly, then put your hands back in place and catch yourself.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH), Contact(BodyPoint.HANDS, Surface.FLOOR, setOf(Which.START))),
        checks = listOf(ChestOffFloor(4.0, 9.0, Which.START), HandsOffFloor(6.0, Which.END)),
    )
}

/**
 * Knee push-up. On the knees (on the floor; the mat is not drawn), the lower legs lying back along it, the body
 * straight from the knees to the head and pivoting at the knees; START on straight arms, END the chest lowered towards the floor. ExRx knee
 * push-up (https://exrx.net/WeightExercises/PectoralSternal/BWKneePushup) and ACE bent-knee push-up.
 */
private val kneePushUp: ExerciseFigure by lazy {
    val p = pushUp(Support.KNEES, 0.0, 0.0, { -90.0 }, -90.0 + PUSH_UP_WRIST, bottom = { -it.chest().y - CHEST_GAP })
    figure(
        View.SIDE, p.start, p.end, listOf(Floor),
        startCaption = "Straight knees to head",
        endCaption = "Chest toward the floor",
        thumbnail = Which.START,
        guideLines = listOf(
            "Kneel on a mat. Place your hands on the floor slightly wider than your shoulders.",
            "Push up on straight arms, so your body is straight from your knees to your head.",
            "Bend your arms to lower your chest toward the floor. Keep your body straight.",
        ),
        contacts = listOf(Contact(BodyPoint.KNEES, Surface.FLOOR, BOTH), Contact(BodyPoint.HANDS, Surface.FLOOR, BOTH)),
        checks = listOf(ChestOffFloor(4.0, 9.0, Which.END), ElbowBend(0.0, 2.0, Which.START)),
    )
}

/**
 * Incline push-up. The hands on the edge of a bench (seen end-on, its top at 43 cm), the feet on the floor; START the
 * arms and body straight, the arms square to the body so the hands lie flat on the bench; END the chest lowered to the
 * bench's edge (the guide's "chest to the edge").
 */
private val inclinePushUp: ExerciseFigure by lazy {
    val benchTop = FlatBench.HEIGHT
    // The arms square to the body at the start (ACE's incline push-up), which lets the hands lie flat on the bench's
    // top. The bottom: the chest point level with the bench's top, and the bench's near edge right there.
    val p = pushUp(Support.TOES, 0.0, benchTop, { t -> t - 90.0 }, 0.0, bottom = { -it.chest().y - benchTop })
    val edgeX = p.end.skeleton().chest().x
    val bench = FlatBench(x = edgeX, length = maxOf(30.0, p.wrist.x - edgeX + 14.0))
    figure(
        View.SIDE, p.start, p.end, listOf(Floor, bench),
        startCaption = "Arms and body straight",
        endCaption = "Chest to the edge",
        thumbnail = Which.START,
        guideLines = listOf(
            "Place your hands on the edge of a bench or platform, slightly wider than your shoulders.",
            "Walk your feet back until your arms and body are straight.",
            "Bend your arms to lower your chest to the edge.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH), Contact(BodyPoint.HANDS, Surface.BENCH, BOTH), Contact(BodyPoint.CHEST, Surface.BENCH, setOf(Which.END))),
        checks = listOf(ElbowBend(0.0, 2.0, Which.START)),
    )
}

/**
 * The decline push-up's straight arms at the top, 16° off vertical towards the head: with the feet up the shoulders
 * travel back over the hands as the body lowers, so the hands start a little ahead of them (neutral).
 */
private const val DECLINE_PUSH_ARM = -74.0

/**
 * Decline push-up. The feet up on a bench behind (seen end-on, 43 cm), the hands on the floor; START straight arms,
 * the body straight; END lowered all the way down, the chest 6 cm off the floor as in the push-up, the body now
 * sloping head-down, with the head tipped back (35°, neutral) as the guide says so the face stays clear of the floor
 * and the body straight.
 */
private val declinePushUp: ExerciseFigure by lazy {
    val benchTop = FlatBench.HEIGHT
    // All the way down: the chest 6 cm off the floor (or the face 1 cm off it, if that came first).
    val p = pushUp(Support.TOES, benchTop, 0.0, { DECLINE_PUSH_ARM }, -90.0 + PUSH_UP_WRIST, neckEnd = -35.0, bottom = { s ->
        minOf(-s.chest().y - CHEST_GAP, -(s.head.y + Body.HEAD_RADIUS) - 1.0)
    })
    val bench = FlatBench(x = -22.0, length = 30.0)
    figure(
        View.SIDE, p.start, p.end, listOf(Floor, bench),
        startCaption = "Feet up on the bench",
        endCaption = "Go all the way down",
        thumbnail = Which.START,
        guideLines = listOf(
            "Put your feet up on the bench and straighten your arms, with your body straight.",
            "Bend your arms to lower your upper body. Keep your body straight.",
            "Tip your head back slightly, so you can go all the way down without arching your back.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.BENCH, BOTH), Contact(BodyPoint.HANDS, Surface.FLOOR, BOTH)),
        checks = listOf(ChestOffFloor(4.0, 9.0, Which.END), ElbowBend(0.0, 2.0, Which.START)),
    )
}

/**
 * The archer push-up seen head-on, the camera in front of the head at about its height. The body slopes away from the
 * camera, so the hips and legs lie behind the shoulders and are hidden by them: the legs point straight away
 * (Leg.out = Leg.shinOut = −90°) and are not drawn (FigureRenderer). The torso is what shows of a body sloping away:
 * short, behind and below the head ([ARCHER_LEAN], drawn at cos of it), the head in front of it ([Pose.neckOut]).
 */
private const val ARCHER_LEAN = 75.0
private val ARCHER_LEG = Leg(hip = 0.0, knee = 0.0, ankle = 0.0, out = -90.0, shinOut = -90.0)

/** The head-on pose whose near shoulder is at [near], the shoulders tilted [tilt]° (+: the near shoulder higher). */
private fun archerPose(near: Point, tilt: Double, nearArm: Arm, farArm: Arm): Pose {
    val torso = 90.0 + tilt
    val centre = near - unit(tilt) * Body.SHOULDER_HALF_WIDTH
    val pelvis = centre - unit(torso) * (Segment.TORSO.length * cos(ARCHER_LEAN / DEG))
    return Pose(pelvis.x, pelvis.y, torso, nearArm = nearArm, nearLeg = ARCHER_LEG, farArm = farArm, lean = ARCHER_LEAN, neckOut = 70.0)
}

/** [v] (3D) with its picture part reflected across the line along [axis]° (the front view draws the far arm so). */
private fun V3.reflect(axis: Double): V3 {
    val a = unit(axis)
    val d = x * a.x + y * a.y
    return V3(2 * d * a.x - x, 2 * d * a.y - y, z)
}

/**
 * The front-view arm of [side] from its shoulder [shoulder] (3D) whose wrist lands on [wrist], the elbow towards
 * [hint], for a torso at [torso]°, the hand bent at the push-up's limit with its fingers on the floor.
 */
private fun archerArm(side: Side, shoulder: V3, wrist: V3, hint: V3, torso: Double): Arm {
    val a = Segment.UPPER_ARM.length
    val b = Segment.FOREARM.length
    val g = wrist - shoulder
    val d = g.length()
    require(d <= a + b + 1e-6) { "wrist out of reach: $d" }
    val gu = g.unit()
    val alpha = acos(((a * a + d * d - b * b) / (2 * a * d)).coerceIn(-1.0, 1.0))
    val h = hint - gu * hint.dot(gu)
    val elbow = shoulder + (gu * cos(alpha) + (if (h.length() < 1e-9) V3(0.0, 1.0, 0.0) else h.unit()) * sin(alpha)) * a
    var u = (elbow - shoulder).unit()
    var f = (wrist - elbow).unit()
    val realFore = angleTo(f.flat)
    var hand = handOn(wrist.flat, realFore, sqrt(1 - f.z * f.z), 0.0, flat = false)
    if (side == Side.FAR) {
        u = u.reflect(torso)
        f = f.reflect(torso)
        hand = 2 * torso - hand
    }
    val arm = armFrom(u, f, torso)
    val fore = angleTo(f.flat)
    return arm.copy(wrist = fold180(hand - fore).coerceIn(-70.0, PUSH_UP_WRIST))
}

/**
 * Archer push-up. Head-on (front view), so the arm out to the side shows; the legs are hidden behind the shoulders
 * (see [ARCHER_LEAN]). The hands stay put. Heights are neutral; ExRx archer push-up
 * (https://exrx.net/WeightExercises/PectoralSternal/BWArcherPushup).
 * - START (the set-up): on the toes, the chest low (shoulders 28 cm up, level), the body over the near hand, which is
 *   under the chest beside the ribs with its elbow bent up and back; the far arm reaches out along the floor at arm's
 *   length, nearly straight (about 20°).
 * - END: pressed up with the near arm until it is straight, close to upright under its shoulder (10° off vertical);
 *   the far arm straight and long, sloping out and down to its hand. With the hands fixed that lifts the near
 *   shoulder above the far one: the shoulders tilt 18° down towards the straight arm.
 */
private val archerPushUp: ExerciseFigure by lazy {
    val wristY = -(Body.HAND_WIDTH / 2 + Segment.HAND.length * 0.5)
    val reach = Segment.UPPER_ARM.length + Segment.FOREARM.length - 0.02
    val nearW = V3(0.0, wristY, -12.0)
    // END: the near shoulder 10° off vertical above its hand, the shoulders tilted 18°, the far arm straight.
    val offVertical = 10.0
    val tilt = 18.0
    val endNearX = nearW.x - reach * sin(offVertical / DEG)
    val endNearY = wristY - sqrt(reach * reach - (endNearX - nearW.x).let { it * it } - nearW.z * nearW.z)
    val endNear = Point(endNearX, endNearY)
    val endFar = endNear - unit(tilt) * (2 * Body.SHOULDER_HALF_WIDTH)
    val farX = endFar.x - sqrt(reach * reach - (wristY - endFar.y).let { it * it })
    val farW = V3(farX, wristY, 0.0)
    // START: level shoulders 28 cm up, the far arm at its 20° reach.
    val startY = -28.0
    val farReach = sqrt(Segment.UPPER_ARM.length * Segment.UPPER_ARM.length + Segment.FOREARM.length * Segment.FOREARM.length +
        2 * Segment.UPPER_ARM.length * Segment.FOREARM.length * cos(20.0 / DEG))
    val startFarX = farX + sqrt(farReach * farReach - (startY - wristY) * (startY - wristY))
    val startNear = Point(startFarX + 2 * Body.SHOULDER_HALF_WIDTH, startY)
    fun pose(near: Point, tilt: Double, nearHint: V3, farHint: V3): Pose {
        val far = near - unit(tilt) * (2 * Body.SHOULDER_HALF_WIDTH)
        val torso = 90.0 + tilt
        return archerPose(
            near, tilt,
            archerArm(Side.NEAR, v3(near, 0.0), nearW, nearHint, torso),
            archerArm(Side.FAR, v3(far, 0.0), farW, farHint, torso),
        )
    }
    figure(
        View.FRONT,
        pose(startNear, 0.0, V3(0.5, -0.6, -0.7), V3(0.0, 1.0, -0.3)),
        pose(endNear, tilt, V3(1.0, 0.0, 0.0), V3(0.0, 1.0, 0.0)),
        listOf(Floor),
        startCaption = "One arm out to the side",
        endCaption = "Press up until straight",
        thumbnail = Which.END,
        guideLines = listOf(
            "Get into a push-up position on your toes.",
            "Place one hand near your ribs, with the other arm reaching out to the side at arm's length.",
            "Press up with the arm near your body until it is straight. Keep the other arm and your body fairly straight.",
        ),
        // The toes are on the floor but hidden behind the body, so only the hands are checked.
        contacts = listOf(Contact(BodyPoint.HANDS, Surface.FLOOR, BOTH)),
        checks = listOf(ElbowBend(0.0, 5.0, Which.END, Side.NEAR), ElbowBend(0.0, 5.0, Which.END, Side.FAR), ElbowBend(10.0, 30.0, Which.START, Side.FAR)),
    )
}

internal val chestFigures: Map<String, Lazy<ExerciseFigure>> by lazy {
    mapOf(
        "DB press" to lazy { dbPress },
        "Incline bench press" to lazy { inclineBenchPress },
        "Decline bench press" to lazy { declineBenchPress },
        "Incline DB press" to lazy { inclineDbPress },
        "Decline DB press" to lazy { declineDbPress },
        "DB fly" to lazy { dbFly },
        "Incline DB fly" to lazy { inclineDbFly },
        "Decline DB fly" to lazy { declineDbFly },
        "Machine chest press" to lazy { machineChestPress },
        "Incline machine press" to lazy { inclineMachinePress },
        "Decline machine press" to lazy { declineMachinePress },
        "Pec deck" to lazy { pecDeck },
        "Smith bench press" to lazy { smithBench },
        "Smith incline press" to lazy { smithIncline },
        "Smith decline press" to lazy { smithDecline },
        "Cable crossover" to lazy { cableCrossover },
        "Low-to-high cable fly" to lazy { lowToHighFly },
        "Cable chest press" to lazy { cableChestPress },
        "Lying cable fly" to lazy { lyingCableFly },
        "Incline bench cable fly" to lazy { inclineCableFly },
        "Seated cable fly" to lazy { seatedCableFly },
        "Chest dip" to lazy { chestDip },
        "Assisted dip" to lazy { assistedDip },
        "Push-up" to lazy { pushUpFigure },
        "Incline push-up" to lazy { inclinePushUp },
        "Decline push-up" to lazy { declinePushUp },
        "Knee push-up" to lazy { kneePushUp },
        "Archer push-up" to lazy { archerPushUp },
        "Clap push-up" to lazy { clapPushUp },
        "Weighted push-up" to lazy { weightedPushUp },
    )
}
