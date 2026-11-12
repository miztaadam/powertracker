package com.powertracker.domain.library.figures.areas

import com.powertracker.domain.library.areas.RACK_SAFETY
import com.powertracker.domain.library.figures.Arm
import com.powertracker.domain.library.figures.BarAbove
import com.powertracker.domain.library.figures.BarRestsOn
import com.powertracker.domain.library.figures.BenchBoards
import com.powertracker.domain.library.figures.DeficitPlatform
import com.powertracker.domain.library.figures.HandlesBesideLegs
import com.powertracker.domain.library.figures.HandsInsideKnees
import com.powertracker.domain.library.figures.KeyCheck
import com.powertracker.domain.library.figures.Prop
import com.powertracker.domain.library.figures.PullingBlocks
import com.powertracker.domain.library.figures.SafetySquatBar
import com.powertracker.domain.library.figures.SquatBox
import com.powertracker.domain.library.figures.TrapBar
import com.powertracker.domain.library.figures.center
import com.powertracker.domain.library.figures.safetyBarHandle
import com.powertracker.domain.library.figures.BarHold
import com.powertracker.domain.library.figures.BarNearLegs
import com.powertracker.domain.library.figures.BarOverMidfoot
import com.powertracker.domain.library.figures.BarTouches
import com.powertracker.domain.library.figures.Barbell
import com.powertracker.domain.library.figures.Body
import com.powertracker.domain.library.figures.BodyPoint
import com.powertracker.domain.library.figures.Contact
import com.powertracker.domain.library.figures.ExerciseFigure
import com.powertracker.domain.library.figures.FlatBench
import com.powertracker.domain.library.figures.Floor
import com.powertracker.domain.library.figures.HipDepth
import com.powertracker.domain.library.figures.Leg
import com.powertracker.domain.library.figures.LockoutOverShoulders
import com.powertracker.domain.library.figures.Point
import com.powertracker.domain.library.figures.Pose
import com.powertracker.domain.library.figures.PowerRack
import com.powertracker.domain.library.figures.Reach
import com.powertracker.domain.library.figures.Scene
import com.powertracker.domain.library.figures.Segment
import com.powertracker.domain.library.figures.Surface
import com.powertracker.domain.library.figures.View
import com.powertracker.domain.library.figures.Which
import com.powertracker.domain.library.figures.point
import com.powertracker.domain.library.figures.skeleton

// Figures for the library's main lifts (library/areas/MainLifts.kt), keyed by catalogue name. Side view for all four:
// the bar moves in the sagittal plane. Every position a guide does not fix is sourced in its comment, or kept neutral.

/** The bar's shaft centre when it rests across the upper back (Kit.kt), for a torso at [torso] rising from [pelvis]. */
private fun barOnBack(pelvis: Point, torso: Double): Point =
    pelvis + unit(torso) * (Segment.TORSO.length - 4.0) + unit(torso + 90.0) * (Body.TORSO_RADIUS + Barbell.SHAFT_RADIUS)

/**
 * Squat. START standing tall with the knees locked, the bar across the upper back; END at competition depth.
 * - Depth (guide): the hip crease below the top of the knee. The thigh capsule is 14 cm thick from hip to knee, so the
 *   crease is below the knee's top when the hip joint is below the knee joint; HipDepth asks 2 cm, a visible margin.
 * - Bottom: trunk about 46° from vertical, the high-bar back squat value in
 *   https://pmc.ncbi.nlm.nih.gov/articles/PMC10143703/ (anterior knee displacement review); the shins lean 33° (inside
 *   the 35° weight-bearing ankle limit, Body.kt), so the trunk and shins are close to parallel, the side-view check in
 *   https://pmc.ncbi.nlm.nih.gov/articles/PMC4262933/ (the guide's back squat assessment source). Knees 126°, inside
 *   the 135° limit.
 * - Balance: the bar over the middle of the foot in both panels (ExRx deadlift form's bar-over-midfoot line,
 *   https://exrx.net/Kinesiology/DeadliftForm, applied to the loaded squat as BarOverMidfoot's own definition).
 *   Standing, that needs the body tipped 4° forward over the ankles and the trunk 12° forward with the bar on the
 *   back; kept as small as the check allows.
 * - Hands: wider than the shoulders on the bar, the upper arms turned out (Arm.turnedOut), so the elbows point down and
 *   the forearms fold back up to the bar. The real arm is foreshortened in a side view; the figure's hands stop where
 *   its elbow is fully bent (Poses.kt MAX_ELBOW), a few cm behind the bar, inside the plate.
 * - Rack (guide's safety line): J-hooks just below shoulder height; safety arms 6 cm under the bar's lowest point.
 */
private val squat: ExerciseFigure by lazy {
    val ankleX = 0.0
    val standLeg = standingLeg(ankleX, shinLean = 4.0, knee = 0.0, torso = 78.0)
    val standTorso = 78.0
    // The hip 2.2 cm below the knee (HipDepth asks 2): the thigh slopes down from knee to hip by asin(2.2 / thigh).
    val slope = kotlin.math.asin(2.2 / Segment.THIGH.length) * 180 / kotlin.math.PI
    val bottomLean = 33.0
    val bottomLeg = standingLeg(ankleX, shinLean = bottomLean, knee = 90.0 + bottomLean + slope, torso = 44.0)
    val bottomTorso = 44.0
    fun arms(pelvis: Point, torso: Double): Arm = armTo(shoulderOf(pelvis, torso), barOnBack(pelvis, torso), torso, turnedOut = true)
    val start = standOn(standLeg, standTorso, arms(standLeg.hip, standTorso))
    val end = standOn(bottomLeg, bottomTorso, arms(bottomLeg.hip, bottomTorso))
    val barTop = barOnBack(standLeg.hip, standTorso)
    val barLow = barOnBack(bottomLeg.hip, bottomTorso)
    val rack = rackInFront(barTop.x, jHookHeight = -barTop.y - 8.0, safetyHeight = -barLow.y - Barbell.SHAFT_RADIUS - 6.0)
    val props = listOf(Floor, rack, Barbell(BarHold.UPPER_BACK))
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(start, props),
        end = Scene(end, props),
        startCaption = "Stand tall, knees locked",
        endCaption = "Hip crease below knee",
        thumbnail = Which.END,
        guideLines = listOf(
            RACK_SAFETY,
            "Step under the bar and rest it across your upper back.",
            "Go down until the crease of your hip is below the top of your knee. This is competition depth.",
            "Stand tall with your knees locked. Breathe, then brace again for the next rep.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(
            HipDepth(belowKnee = 2.0, at = Which.END), BarOverMidfoot(Which.START), BarOverMidfoot(Which.END),
        ),
    )
}

/**
 * Bench: from the approved draft (docs/library/pictures/bench-press-figure-draft.svg), reviewed against the sources.
 * Side view, lying on a flat bench (IPF: pad top 42–45 cm) inside a rack, with the safety arms below the chest as the
 * guide's safety line asks. START: the bar paused on the chest; END: lockout over the shoulders. Feet flat on the floor;
 * head, upper back and buttocks on the bench (the guide's setup line).
 * - Touch point: lower to mid chest (https://exrx.net/Kinesiology/BenchPress), above the bottom of the sternum
 *   (https://exrx.net/WeightExercises/PectoralSternal/BBBenchPressPowerLift); Body.CHEST_BELOW_SHOULDER.
 * - Bottom arm: forearms vertical under the bar (the same ExRx analyses page, citing Rippetoe). With full-length
 *   segments in a side view the forearm cannot be exactly vertical while the grip is on the chest: the real upper arm
 *   is 45–70° out from the torso (same page), so a side view sees it foreshortened. The figure's forearm comes out
 *   about 16° off vertical, the elbow a little towards the feet, and the 2D elbow angle (about 142°) is not the real
 *   3D one (about 90° with a wider-than-shoulder grip, https://pmc.ncbi.nlm.nih.gov/articles/PMC5968970/). Bending the
 *   wrist does not bring it closer than 4 cm, so the wrist stays straight (the bar over the forearm).
 * - Feet: flat, 32 cm past the hips (neutral, the draft's placement), knees bent about 80°.
 * - Rack: J-hooks at the IPF's highest bar rest (110 cm); safety arms 60 cm, under the chest (the bar touches about
 *   65 cm up).
 */
private val bench: ExerciseFigure by lazy {
    val torso = 180.0 // face up, head to the left
    val pelvis = Point(95.0, -(FlatBench.HEIGHT + Body.TORSO_RADIUS))
    val bare = Pose(pelvis.x, pelvis.y, torso, nearArm = Arm(0.0, 0.0), nearLeg = Leg(0.0, 0.0)).skeleton()
    val chest = bare.point(BodyPoint.CHEST)
    val legs = Reach.leg(pelvis, Point(127.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
    val bar = Point(chest.x, chest.y - Barbell.SHAFT_RADIUS)
    val onChest = Reach.arm(bare.shoulder, bar, torso)
    val lockout = Arm(shoulder = 90.0, elbow = 0.0)
    val props = listOf(Floor, FlatBench(), PowerRack(x = 2.0, jHookHeight = 110.0, safetyHeight = 60.0), Barbell())
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(Pose(pelvis.x, pelvis.y, torso, nearArm = onChest, nearLeg = legs), props),
        end = Scene(Pose(pelvis.x, pelvis.y, torso, nearArm = lockout, nearLeg = legs), props),
        startCaption = "Pause on your chest",
        endCaption = "Press until elbows lock",
        thumbnail = Which.END,
        guideLines = listOf(
            RACK_SAFETY,
            "Lie on the bench with your head, shoulders and buttocks on the bench. Plant your feet flat on the floor.",
            "Pause with the bar still on your chest.",
            "Press the bar up until your elbows lock.",
        ),
        contacts = listOf(
            Contact(BodyPoint.FEET, Surface.FLOOR, BOTH),
            Contact(BodyPoint.HEAD, Surface.BENCH, BOTH),
            Contact(BodyPoint.UPPER_BACK, Surface.BENCH, BOTH),
            Contact(BodyPoint.HIPS, Surface.BENCH, BOTH),
        ),
        checks = listOf(BarTouches(BodyPoint.CHEST, Which.START), LockoutOverShoulders(Which.END)),
    )
}

/**
 * Deadlift. START set up over the bar on the floor; END standing tall with the knees locked and the shoulders back.
 * - Bar on the floor: its centre one plate radius up (45 cm plates, IPF).
 * - Setup: the bar over the mid-foot and against the shins (ACE has the shins touching it; ExRx deadlift form,
 *   https://exrx.net/Kinesiology/DeadliftForm, and the NSCA's progression article, about an inch away:
 *   https://www.nsca.com/education/articles/nsca-coach/exercise-progressions-for-resuming-strength-training-following-posterior-chain-muscle-injury/;
 *   the bar is 0.5 cm off the shin here, the guide's "close to it"). The shins lean 12° forward, so the knees are a
 *   little in front of the bar, and the shoulders are just ahead of it (1 cm, ExRx's "slightly in front").
 * - With straight arms that sets the hips: about 63 cm up, the torso about 31° above level, the hips flexed about 130°
 *   and the knees about 82°. ExRx's deadlift analysis (https://exrx.net/Kinesiology/Deadlift) starts with the hips
 *   deeply flexed and the knees moderately bent (it puts them near 90°); the figure's knees are on the less-bent,
 *   conservative side of that, still clearly a bent-knee conventional pull and not a stiff-legged one.
 * - Lockout: legs vertical, torso upright, the arms angled just enough to hold the bar against the front of the thighs.
 */
private val deadlift: ExerciseFigure by lazy {
    val ankleX = 0.0
    val shinLean = 12.0
    val barY = -Barbell.PLATE_DIAMETER / 2
    // The shin's centre line at the bar's height, then the bar 0.5 cm in front of the shin's surface.
    val shinAtBar = ankleX + (-barY - Body.ANKLE_HEIGHT) * kotlin.math.tan(shinLean * kotlin.math.PI / 180)
    val barX = shinAtBar + (Body.SHIN_WIDTH / 2 + 0.5 + Barbell.SHAFT_RADIUS) / kotlin.math.cos(shinLean * kotlin.math.PI / 180)
    val bar = Point(barX, barY)
    val lead = 1.0
    val shoulder = Point(barX + lead, barY - kotlin.math.sqrt(ARM_REACH * ARM_REACH - lead * lead))
    val knee = Point(ankleX, -Body.ANKLE_HEIGHT) + unit(90.0 - shinLean) * Segment.SHIN.length
    // The hip: thigh length from the knee and torso length from the shoulder, on the side behind them.
    val hip = circleMeet(knee, Segment.THIGH.length, shoulder, Segment.TORSO.length)
    val torso = angleTo(shoulder - hip)
    val kneeBend = fold180(angleTo(hip - knee) - (90.0 - shinLean))
    val setupLeg = standingLeg(ankleX, shinLean, kneeBend, torso)
    val start = standOn(setupLeg, torso, straightArm(shoulder, bar, torso))

    val standLeg = standingLeg(ankleX, 0.0, 0.0, 90.0)
    val standShoulder = shoulderOf(standLeg.hip, 90.0)
    val frontOfThigh = Body.THIGH_WIDTH / 2 + Barbell.SHAFT_RADIUS + 0.5
    val end = standOn(standLeg, 90.0, hangingArmAt(standShoulder, ankleX + frontOfThigh, 90.0))
    val props = listOf(Floor, Barbell())
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(start, props),
        end = Scene(end, props),
        startCaption = "Shins close to the bar",
        endCaption = "Stand tall, knees locked",
        thumbnail = Which.START,
        guideLines = listOf(
            "Stand with your feet flat under the bar, about shoulder-width apart, with your shins close to it.",
            "Bend down and grip the bar shoulder-width or a little wider. Use both palms down, or one up and one down.",
            "Keep the bar close to your legs as it rises. Raise it at the same rate as your hips.",
            "Stand tall with your knees locked and your shoulders back.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(
            BarOverMidfoot(Which.START), BarOverMidfoot(Which.END),
            BarNearLegs(within = 4.0, at = Which.START), BarNearLegs(within = 4.0, at = Which.END),
        ),
    )
}

/**
 * Strict press. START with the bar at the front of the neck; END pressed overhead to straight arms. Feet flat, back
 * straight and tall (the torso upright in both panels).
 * - START: the guide lowers the bar "to the front of your neck": the bar rests at the front of the shoulders, below
 *   the chin, about 9 cm in front of the shoulder joint. The elbows are forward and folded to the figure's limit, and
 *   the hand is bent back 55° so the bar sits in the palm (the front rack). The figure's wrist sign is set by the
 *   palm side of an unrotated forearm; with the press's overhand grip the palm faces forward, so this drawn bend
 *   towards the face is the wrist bent back (extension), inside both the 60° flexion and 70° extension limits.
 * - END: the bar straight over the shoulder joints with locked elbows (ExRx military press,
 *   https://exrx.net/WeightExercises/DeltoidAnterior/BBMilitaryPress: press until the arms are extended overhead), and
 *   over the mid-foot: the legs lean 3° forward from the ankles so the shoulders and bar are over the foot.
 * - Rack (guide's safety line): J-hooks about shoulder height, safety arms 10 cm under the bar's start.
 */
private val strictPress: ExerciseFigure by lazy {
    val ankleX = 0.0
    val leg = standingLeg(ankleX, 3.0, 0.0, 90.0)
    val shoulder = shoulderOf(leg.hip, 90.0)
    // The front rack: the hand bent back so the bar sits in the palm close to the front of the shoulders.
    val rackAt = shoulder + Point(8.0, -3.0)
    fun gripOf(arm: Arm) = Pose(leg.hip.x, leg.hip.y, 90.0, nearArm = arm, nearLeg = leg.leg).skeleton().near.grip
    // The elbow folded to its limit and the hand bent back 55°; the shoulder angle that brings the bar nearest the rack point.
    val front = (0..900).map { Arm(shoulder = it / 10.0, elbow = MAX_ELBOW, wrist = 55.0) }.minBy { (gripOf(it) - rackAt).length() }
    val start = standOn(leg, 90.0, front)
    val end = standOn(leg, 90.0, Arm(180.0, 0.0))
    val barStart = start.skeleton().near.grip
    val rack = rackInFront(barStart.x, jHookHeight = -barStart.y - 6.0, safetyHeight = -barStart.y - 10.0, gap = 35.0)
    val props = listOf(Floor, rack, Barbell())
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(start, props),
        end = Scene(end, props),
        startCaption = "Front of your neck",
        endCaption = "Press to straight arms",
        thumbnail = Which.END,
        guideLines = listOf(
            RACK_SAFETY,
            "Take the bar across the front of your shoulders and step back.",
            "Press the bar overhead until your arms are straight.",
            "Keep your back straight and tall.",
            "Lower the bar under control to the front of your neck.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(LockoutOverShoulders(Which.END), BarOverMidfoot(Which.END)),
    )
}

/** Where two circles meet, on the side to the left of the line from [a] to [b] (behind a lifter who faces +x). */
private fun circleMeet(a: Point, ra: Double, b: Point, rb: Double): Point {
    val d = (b - a).length()
    val along = (ra * ra - rb * rb + d * d) / (2 * d)
    val h = kotlin.math.sqrt((ra * ra - along * along).coerceAtLeast(0.0))
    val u = (b - a) * (1.0 / d)
    val base = a + u * along
    val left = Point(u.y, -u.x) // rotated a quarter turn towards -x for an upward u
    return base + left * h
}

// ---- Variations (Task 5) ----
// Each variation starts from its parent lift's approved pose (Squat, Bench or Deadlift above) and changes only what its
// own guide changes. Shared solvers below keep the body balanced the same way the parent does.

/** The middle of the near foot, for a foot flat with its ankle joint at [ankleX] (the point BarOverMidfoot uses). */
private fun midfootX(ankleX: Double): Double {
    val s = standOn(standingLeg(ankleX, 0.0, 0.0, 90.0), 90.0, Arm(0.0, 0.0)).skeleton()
    return (s.near.heel.x + s.near.toe.x) / 2
}

/**
 * The torso angle (in [range], 0.1° steps) that puts [load] over the middle of the foot for a lifter standing on [leg]:
 * the squat's own balance rule (BarOverMidfoot), solved instead of hand-tuned.
 */
private fun balancedTorso(leg: StandingLeg, range: ClosedFloatingPointRange<Double>, load: (Point, Double) -> Point): Double {
    val mid = midfootX(leg.ankle.x)
    var best = range.start
    var t = range.start
    while (t <= range.endInclusive + 1e-9) {
        if (kotlin.math.abs(load(leg.hip, t).x - mid) < kotlin.math.abs(load(leg.hip, best).x - mid)) best = t
        t += 0.1
    }
    return best
}

/** The knee bend that puts the hip [below] cm under the knee for a shin leaning [shinLean]° (squat depth). */
private fun kneeForDepth(shinLean: Double, below: Double): Double =
    90.0 + shinLean + kotlin.math.asin(below / Segment.THIGH.length) * 180 / kotlin.math.PI

private const val BAR_ON_BACK_LINE = "Step under the bar and rest it across your upper back."

/** The back-squat pose with the bar across the upper back: the squat's turned-out arms. */
private fun backSquatPose(leg: StandingLeg, torso: Double): Pose =
    standOn(leg, torso, armTo(shoulderOf(leg.hip, torso), barOnBack(leg.hip, torso), torso, turnedOut = true))

/** The squat's rack: J-hooks 8 cm under the bar's top position, safety arms 6 cm under its lowest. */
private fun squatRack(barTop: Point, barLow: Point) =
    rackInFront(barTop.x, jHookHeight = -barTop.y - 8.0, safetyHeight = -barLow.y - Barbell.SHAFT_RADIUS - 6.0)

/**
 * Pause squat: the squat's own two poses (its guide's depth is "your normal depth"), END the still pause at the
 * bottom. Nothing in the guide changes the positions, only the hold.
 */
private val pauseSquat: ExerciseFigure by lazy {
    squat.copy(
        startCaption = "Stand tall and brace",
        endCaption = "Hold still at the bottom",
        guideLines = listOf(
            RACK_SAFETY,
            BAR_ON_BACK_LINE,
            "Squat down under control to your normal depth.",
            "Hold still at the bottom for a short pause. Stay braced.",
            "Stand tall, breathe, and brace again for the next rep.",
        ),
    )
}

/**
 * High-bar squat: the squat's poses. The approved Squat figure is already drawn high-bar (the bar on the top of the
 * upper back, Kit.kt's UPPER_BACK, and the trunk about 46° from vertical, the high-bar value in
 * https://pmc.ncbi.nlm.nih.gov/articles/PMC10143703/). Its depth, the hip 2.2 cm under the knee, is "thighs just past
 * level with the floor" (the thigh slopes 3° below level), so this guide changes nothing in the positions.
 */
private val highBarSquat: ExerciseFigure by lazy {
    squat.copy(
        startCaption = "Bar high on upper back",
        endCaption = "Thighs just past level",
        guideLines = listOf(
            RACK_SAFETY,
            "Step under the bar and rest it high on your upper back, on the top of the muscle just below your neck.",
            "Push your hips back and let your knees bend forward. Keep your back straight and your torso upright.",
            "Go down until your thighs are just past level with the floor. Stop sooner if your heels lift or your back starts to round.",
        ),
    )
}

/**
 * Tempo squat. END the squat's bottom ("your normal squat depth"); START halfway down, the slow way down the guide is
 * about (two stills cannot show speed, so the panel shows the lifter part-way through it). The halfway pose is a
 * neutral in-between: shins 18° forward, knees bent 60°, the torso solved so the bar stays over the mid-foot, as in
 * the squat.
 */
private val tempoSquat: ExerciseFigure by lazy {
    val leg = standingLeg(0.0, shinLean = 18.0, knee = 60.0, torso = 70.0)
    val torso = balancedTorso(leg, 40.0..90.0, ::barOnBack)
    val leg2 = standingLeg(0.0, 18.0, 60.0, torso)
    val start = backSquatPose(leg2, torso)
    // The Squat's rack, its posts 55 cm out so they clear the head as the lifter leans part-way down.
    val barTop = Barbell(BarHold.UPPER_BACK).center(squat.start.pose.skeleton())
    val barLow = Barbell(BarHold.UPPER_BACK).center(squat.end.pose.skeleton())
    val rack = rackInFront(barTop.x, jHookHeight = -barTop.y - 8.0, safetyHeight = -barLow.y - Barbell.SHAFT_RADIUS - 6.0, gap = 55.0)
    val props = listOf(Floor, rack, Barbell(BarHold.UPPER_BACK))
    squat.copy(
        start = Scene(start, props),
        end = Scene(squat.end.pose, props),
        startCaption = "Lower yourself slowly",
        endCaption = "Normal squat depth",
        guideLines = listOf(
            RACK_SAFETY,
            BAR_ON_BACK_LINE,
            "Lower yourself slowly and under control. Take longer to go down than to come up.",
            "Go down to your normal squat depth.",
        ),
    )
}

/**
 * Front squat. The bar in the front rack (Kit.kt FRONT_RACK: on the front of the shoulders, on the upper arms),
 * the elbows pointing straight ahead (the upper arms level), the torso upright.
 * - Depth (guide): thighs level with the floor; the hip joint is level with the knee joint (HipDepth 0), the
 *   conservative end of the guide's "stop sooner" rule.
 * - Upright torso: the front squat keeps a more upright trunk than the back squat
 *   (https://pmc.ncbi.nlm.nih.gov/articles/PMC12820588/). The figure gets it from balance alone: the shins lean 34°
 *   (inside the 35° ankle limit, Body.kt; the guide's mistake line is "keeping your shins too upright"), and the
 *   torso is solved so the bar is over the mid-foot, which brings it to about 70° above level (20° from vertical).
 * - Hands: the hands under the bar with the wrists bent back. The forearms run back from the elbows to the bar and in
 *   towards the neck, so from the side they look shorter: drawn 45° out of the picture plane (Arm.forearmOut, a
 *   neutral angle); the true elbow bend stays inside its limit.
 * - Rack (guide's safety line): J-hooks under the bar at upper-chest height; safety arms 6 cm under its lowest point.
 */
private const val FRONT_RACK_FOREARM_OUT = 45.0

private val frontSquat: ExerciseFigure by lazy {
    val front = Barbell(BarHold.FRONT_RACK)
    fun rackBar(pelvis: Point, torso: Double) = pelvis + unit(torso) * (Segment.TORSO.length + 6.5) + unit(torso - 90.0) * 9.0
    val standLeg = standingLeg(0.0, 0.0, 0.0, 90.0)
    val standTorso = balancedTorso(standLeg, 80.0..90.0, ::rackBar)
    val lean = 34.0
    val bottomLeg0 = standingLeg(0.0, lean, kneeForDepth(lean, 0.5), 70.0)
    val bottomTorso = balancedTorso(bottomLeg0, 50.0..90.0, ::rackBar)
    fun pose(leg0: StandingLeg, torso: Double): Pose {
        val leg = standingLeg(leg0.ankle.x, leg0.leg.ankle, leg0.leg.knee, torso)
        val shoulder = shoulderOf(leg.hip, torso)
        val bar = rackBar(leg.hip, torso)
        // Elbows straight ahead: the upper arm level. The forearm folds back to the bar and in towards the neck, so
        // from the side it is foreshortened (Arm.forearmOut 45°); the elbow bend and the wrist (bent back) are the ones
        // that bring the grip nearest the bar.
        // A coarse search, then a fine one around the best (kept small: figures are built when the app first asks).
        fun arm(e: Double, w: Double) = Arm(shoulder = fold180(0.0 - torso - 180.0), elbow = e, wrist = w, forearmOut = FRONT_RACK_FOREARM_OUT)
        fun miss(a: Arm) = (Pose(leg.hip.x, leg.hip.y, torso, nearArm = a, nearLeg = leg.leg).skeleton().near.grip - bar).length()
        val coarse = (120..178 step 4).flatMap { e -> (-70..60 step 10).map { w -> arm(e.toDouble(), w.toDouble()) } }.minBy(::miss)
        val arm = (-4..4).flatMap { de -> (-5..5).map { dw -> arm((coarse.elbow + de).coerceAtMost(178.0), (coarse.wrist + 2 * dw).coerceIn(-70.0, 60.0)) } }.minBy(::miss)
        return standOn(leg, torso, arm)
    }
    val start = pose(standLeg, standTorso)
    val end = pose(bottomLeg0, bottomTorso)
    val barTop = front.center(start.skeleton())
    val barLow = front.center(end.skeleton())
    val props = listOf(Floor, squatRack(barTop, barLow), front)
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(start, props),
        end = Scene(end, props),
        startCaption = "Elbows straight ahead",
        endCaption = "Thighs level with floor",
        thumbnail = Which.END,
        guideLines = listOf(
            RACK_SAFETY,
            "Rest the bar across the front of your shoulders, palms facing up and elbows pointing straight ahead.",
            "Squat down, keeping your torso upright and your back straight.",
            "Go down until your thighs are level with the floor. Stop sooner if your heels lift or your back starts to round.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(HipDepth(belowKnee = 0.0, at = Which.END), BarOverMidfoot(Which.START), BarOverMidfoot(Which.END)),
    )
}

/**
 * Pin squat. START the bar resting on the safety pins, END standing tall (the squat's START).
 * - Pin height (guide): "often just below where your squat slows down". In free-weight back squats the sticking
 *   region comes at about 98–102° at the knee (inside angle), with the bottom at about 89°
 *   (https://pmc.ncbi.nlm.nih.gov/articles/PMC4234771/; also https://pmc.ncbi.nlm.nih.gov/articles/PMC8217455/).
 *   The pins are set just below it: the knees bent 85° (an inside angle of 95°), the shins 25° forward, the torso
 *   solved so the bar is over the mid-foot.
 * - The pins are the rack's safety arms, reaching under the bar (the pins also catch it, the guide says); J-hooks as
 *   in the Squat.
 */
private val pinSquat: ExerciseFigure by lazy {
    val leg0 = standingLeg(0.0, shinLean = 25.0, knee = 85.0, torso = 60.0)
    val torso = balancedTorso(leg0, 30.0..90.0, ::barOnBack)
    val leg = standingLeg(0.0, 25.0, 85.0, torso)
    val start = backSquatPose(leg, torso)
    val bar = barOnBack(leg.hip, torso)
    val barTop = Barbell(BarHold.UPPER_BACK).center(squat.start.pose.skeleton())
    val gap = 55.0 // the posts clear of the head as the lifter leans over the pins
    val pins = PowerRack(
        x = barTop.x + gap, jHookHeight = -barTop.y - 8.0, safetyHeight = -bar.y - Barbell.SHAFT_RADIUS,
        armLength = gap + (barTop.x - bar.x) + 12.0, lifterLeft = true,
    )
    val props = listOf(Floor, pins, Barbell(BarHold.UPPER_BACK))
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(start, props),
        end = Scene(squat.start.pose, props),
        startCaption = "Bar rests on the pins",
        endCaption = "Stand tall, knees locked",
        thumbnail = Which.START,
        guideLines = listOf(
            "Set the safety pins in the rack at the height you want to start from, often just below where your squat slows down.",
            "Rest the bar on the pins. The pins also catch the bar if you can't stand up.",
            "Get under the bar and rest it across your upper back.",
            "Drive the bar up off the pins by straightening your hips and knees together.",
            "Stand tall with your knees locked.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(BarRestsOn(Surface.SAFETY_ARM, Which.START), BarOverMidfoot(Which.START), BarOverMidfoot(Which.END)),
    )
}

/**
 * Safety-bar squat. The safety squat bar (Kit.kt) across the upper back, hands on its handles in front, the squat's
 * shins and its depth.
 * - Depth (guide): thighs just below level: the hip 2.2 cm under the knee, as in the Squat.
 * - Upright: the safety bar keeps the torso more upright than a high-bar squat (the guide; also
 *   https://pmc.ncbi.nlm.nih.gov/articles/PMC8392107/ and its review of the bar). In the figure that comes from the
 *   cambered plates hanging forward of the yoke: the torso is solved so the plates are over the mid-foot.
 * - Rack (guide's safety line): as in the Squat.
 */
private val safetyBarSquat: ExerciseFigure by lazy {
    // The plates' position (Kit.kt safetyBarLoad), worked out from the torso alone so the balance search stays cheap.
    fun load(pelvis: Point, torso: Double): Point =
        pelvis + unit(torso) * (Segment.TORSO.length - 2.0) + unit(torso + 90.0) * (Body.TORSO_RADIUS + SafetySquatBar.PAD_RADIUS) +
            Point(SafetySquatBar.CAMBER_FORWARD, SafetySquatBar.CAMBER)
    fun pose(leg0: StandingLeg, range: ClosedFloatingPointRange<Double>): Pose {
        val torso = balancedTorso(leg0, range, ::load)
        val leg = standingLeg(leg0.ankle.x, leg0.leg.ankle, leg0.leg.knee, torso)
        val bare = standOn(leg, torso, Arm(0.0, 0.0)).skeleton()
        val arm = Reach.arm(bare.shoulder, safetyBarHandle(bare), torso)
        return standOn(leg, torso, arm)
    }
    val start = pose(standingLeg(0.0, 3.0, 0.0, 85.0), 80.0..90.0)
    val lean = 33.0
    val end = pose(standingLeg(0.0, lean, kneeForDepth(lean, 2.2), 50.0), 40.0..80.0)
    val top = SafetySquatBar.center(start.skeleton())
    val low = SafetySquatBar.center(end.skeleton())
    // Safety arms 6 cm under the cambered sleeves at the bottom, where they would catch the bar.
    // The posts 55 cm out, clear of the handles in front of the chest.
    val rack = rackInFront(top.x, jHookHeight = -top.y - 8.0, safetyHeight = -low.y - SafetySquatBar.CAMBER - Barbell.SHAFT_RADIUS - 6.0, gap = 55.0)
    val props = listOf(Floor, rack, SafetySquatBar)
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(start, props),
        end = Scene(end, props),
        startCaption = "Handles in front of you",
        endCaption = "Thighs just below level",
        thumbnail = Which.END,
        guideLines = listOf(
            RACK_SAFETY,
            "Step under the bar and rest the pads on your upper back and shoulders.",
            "Hold the handles in front of you.",
            "Squat down until your thighs are just below level with the floor.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH)),
        checks = listOf(HipDepth(belowKnee = 2.0, at = Which.END), BarOverMidfoot(Which.START), BarOverMidfoot(Which.END)),
    )
}

/**
 * Box squat. START standing with the box just behind; END sitting back on it.
 * - Shins (guide): "close to upright". ExRx's box squat (https://exrx.net/WeightExercises/GluteusMaximus/BBBoxSquat)
 *   keeps them nearly perpendicular to the floor: 5° here.
 * - Box height (guide): "your target depth". The guide gives no depth, so the box is at parallel (the hip level with
 *   the knee), the conservative choice, and the hips sit on it (HIPS on BOX).
 * - Torso: sitting back with upright shins puts the hips well behind the feet, so the torso leans forward, to 40°
 *   above level (a neutral value; the lifter's weight is partly on the box, so the bar is behind the mid-foot here and
 *   BarOverMidfoot is asked only standing).
 * - Rack (guide's safety line): as in the Squat.
 */
private val boxSquat: ExerciseFigure by lazy {
    val lean = 5.0
    val torso = 40.0
    val leg = standingLeg(0.0, lean, kneeForDepth(lean, 0.0), torso)
    val end = backSquatPose(leg, torso)
    val boxTop = -leg.hip.y - Body.TORSO_RADIUS
    val box = SquatBox(x = leg.hip.x - 6.0, height = boxTop)
    val barTop = Barbell(BarHold.UPPER_BACK).center(squat.start.pose.skeleton())
    val barLow = barOnBack(leg.hip, torso)
    val props = listOf(Floor, box, squatRack(barTop, barLow), Barbell(BarHold.UPPER_BACK))
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(squat.start.pose, props),
        end = Scene(end, props),
        startCaption = "Box just behind you",
        endCaption = "Pause briefly on the box",
        thumbnail = Which.END,
        guideLines = listOf(
            RACK_SAFETY,
            "Place a sturdy box behind you at the height of your target depth.",
            "Lift the bar out and step back until the box is just behind you.",
            "Sit back by bending your hips and knees. Keep your shins close to upright.",
            "Pause briefly on the box while staying braced.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, BOTH), Contact(BodyPoint.HIPS, Surface.BOX, setOf(Which.END))),
        checks = listOf(BarOverMidfoot(Which.START)),
    )
}

// ---- Bench variations: the Bench's body, bench and rack, with the bar and arms changed per guide ----

private val benchTorso = 180.0
private val benchPelvis = Point(95.0, -(FlatBench.HEIGHT + Body.TORSO_RADIUS))
private val benchBare by lazy { Pose(benchPelvis.x, benchPelvis.y, benchTorso, nearArm = Arm(0.0, 0.0), nearLeg = Leg(0.0, 0.0)).skeleton() }
private val benchChest by lazy { benchBare.point(BodyPoint.CHEST) }

/** The Bench's pose with [arm]; the legs stay as in the Bench unless [legs] is given. */
private fun benchPose(arm: Arm, legs: Leg = bench.start.pose.nearLeg) =
    Pose(benchPelvis.x, benchPelvis.y, benchTorso, nearArm = arm, nearLeg = legs)

/** The Bench's arm with the bar [above] cm over the chest (0 = on it), the bar straight above the touch point. */
private fun benchArmAbove(above: Double): Arm =
    Reach.arm(benchBare.shoulder, Point(benchChest.x, benchChest.y - Barbell.SHAFT_RADIUS - above), benchTorso)

private val benchLockout = Arm(shoulder = 90.0, elbow = 0.0)
private const val PRESS_LOCK = "Press until elbows lock"

/** A bench variation from the Bench: the same lying contacts and rack unless given. */
private fun benchVariation(
    start: Pose,
    startCaption: String,
    guideLines: List<String>,
    checks: List<KeyCheck>,
    end: Pose = benchPose(benchLockout),
    props: List<Prop> = bench.start.props,
    contacts: List<Contact> = bench.contacts,
    endCaption: String = PRESS_LOCK,
) = ExerciseFigure(
    view = View.SIDE,
    start = Scene(start, props),
    end = Scene(end, props),
    startCaption = startCaption,
    endCaption = endCaption,
    thumbnail = Which.END,
    guideLines = guideLines,
    contacts = contacts,
    checks = checks,
)

private const val BENCH_LIE_LINE = "Lie on the bench with your head, shoulders and buttocks on the bench. Plant your feet flat on the floor."
private const val LOCKOUT_LINE = "Press the bar up until your elbows lock."

/**
 * Spoto press. START the bar held still just above the chest, not touching: 4.5 cm above it (the usual 1–2 in
 * "just above", chosen at the top so the gap shows plainly at phone size, the guide's point being "without
 * touching"); END the Bench's lockout.
 */
private val spotoPress: ExerciseFigure by lazy {
    benchVariation(
        start = benchPose(benchArmAbove(4.5)),
        startCaption = "Stop just above chest",
        guideLines = listOf(RACK_SAFETY, BENCH_LIE_LINE, "Stop it just above your chest, without touching. Hold it still for a moment.", LOCKOUT_LINE),
        checks = listOf(BarAbove(BodyPoint.CHEST, min = 3.5, max = 5.5, at = Which.START), LockoutOverShoulders(Which.END)),
    )
}

/** Long-pause bench: the Bench's two poses; START is the longer pause on the chest. */
private val longPauseBench: ExerciseFigure by lazy {
    benchVariation(
        start = bench.start.pose,
        startCaption = "Hold still on your chest",
        guideLines = listOf(
            RACK_SAFETY, BENCH_LIE_LINE,
            "Hold it still on your chest for a few seconds, longer than a competition pause.", LOCKOUT_LINE,
        ),
        checks = listOf(BarTouches(BodyPoint.CHEST, Which.START), LockoutOverShoulders(Which.END)),
    )
}

/**
 * Pin press. START the bar resting on the safety pins, END lockout. The guide sets the pins "where your bench slows
 * down": the sticking point averages about 10 cm off the chest (10.0 ± 3.3 cm, range 5–15, in
 * https://pmc.ncbi.nlm.nih.gov/articles/PMC8162553/; about 10 cm in other studies, and 3–5 cm for the start of the
 * sticking region, reviewed in https://pmc.ncbi.nlm.nih.gov/articles/PMC12372072/). The pins are 10 cm over the chest.
 */
private val pinPress: ExerciseFigure by lazy {
    val arm = benchArmAbove(10.0)
    val bar = benchPose(arm).skeleton().near.grip
    val rack = PowerRack(x = 2.0, jHookHeight = 110.0, safetyHeight = -bar.y - Barbell.SHAFT_RADIUS)
    benchVariation(
        start = benchPose(arm),
        startCaption = "Bar rests on the pins",
        props = listOf(Floor, FlatBench(), rack, Barbell()),
        guideLines = listOf(
            "Set a bench inside a power rack. The bar starts on the hooks.",
            "Set the safety pins at the height you want to press from, often where your bench slows down. The pins are the bottom stop, and they catch the bar if you miss.",
            BENCH_LIE_LINE,
            "Lower the bar under control until it rests on the pins.",
            LOCKOUT_LINE,
        ),
        checks = listOf(BarRestsOn(Surface.SAFETY_ARM, Which.START), LockoutOverShoulders(Which.END)),
    )
}

/**
 * Larsen press. The Bench with the feet off the floor: the legs held still, straight out past the end of the bench and
 * raised 5° (a neutral, low hold; the guide fixes only that the feet are off the floor and the legs still).
 * Bench pressing with the legs raised is studied in https://pmc.ncbi.nlm.nih.gov/articles/PMC8038688/.
 */
private val larsenPress: ExerciseFigure by lazy {
    val legs = Leg(hip = 5.0, knee = 0.0, ankle = -20.0)
    benchVariation(
        start = benchPose(bench.start.pose.nearArm, legs),
        end = benchPose(benchLockout, legs),
        startCaption = "Feet off the floor",
        contacts = bench.contacts.filter { it.point != BodyPoint.FEET },
        guideLines = listOf(
            RACK_SAFETY,
            "Lie on the bench with your head, shoulders and buttocks on the bench.",
            "Lift your feet off the floor and hold your legs still.",
            "Lower the bar under control to your chest.",
            LOCKOUT_LINE,
        ),
        checks = listOf(BarTouches(BodyPoint.CHEST, Which.START), LockoutOverShoulders(Which.END)),
    )
}

/**
 * Close-grip bench: the Bench's poses. The narrower grip cannot be seen from the side; the guide's other change, the
 * elbows kept close to the ribs (ExRx close-grip bench, https://exrx.net/WeightExercises/Triceps/BBCloseGripBenchPress),
 * is how the Bench is already drawn: its upper arm lies in the picture plane along the body, at full length.
 */
private val closeGripBench: ExerciseFigure by lazy {
    benchVariation(
        start = bench.start.pose,
        startCaption = "Elbows close to ribs",
        guideLines = listOf(
            RACK_SAFETY, BENCH_LIE_LINE,
            "Lower the bar to your chest, keeping your elbows close to your ribs.",
            "Push your feet into the floor and press the bar back up until your elbows lock.",
        ),
        checks = listOf(BarTouches(BodyPoint.CHEST, Which.START), LockoutOverShoulders(Which.END)),
    )
}

/**
 * Board press. A 2-board stack (Kit.kt BenchBoards) on the chest at the Bench's touch point, START the bar touching the
 * boards, END lockout. ExRx's board press (https://exrx.net/WeightExercises/PectoralSternal/BBBoardPress) uses 1, 2 or
 * 3 boards; 2 is the middle.
 */
private val boardPress: ExerciseFigure by lazy {
    val boards = BenchBoards(x = benchChest.x, bottom = benchChest.y)
    val arm = benchArmAbove(boards.thickness)
    benchVariation(
        start = benchPose(arm),
        startCaption = "Bar touches the board",
        props = listOf(Floor, FlatBench(), bench.start.props.filterIsInstance<PowerRack>().single(), boards, Barbell()),
        guideLines = listOf(
            RACK_SAFETY, BENCH_LIE_LINE,
            "A partner holds the board on your lower chest, or you strap it to your body. A thicker board shortens the range more.",
            "Lower the bar under control until it touches the board.", LOCKOUT_LINE,
        ),
        checks = listOf(BarRestsOn(Surface.BOARD, Which.START), LockoutOverShoulders(Which.END)),
    )
}

/**
 * Wide-grip bench. START the bar on the chest with the forearms vertical, END lockout; the wider grip is what changes.
 * - A wide grip is about 200% of the shoulder (biacromial) width
 *   (https://pmc.ncbi.nlm.nih.gov/articles/PMC8296276/, the guide's source). Seen from the side, the upper arms
 *   then point further out to the side, so they are foreshortened (Arm.out): with the forearm vertical under the bar
 *   on the Bench's touch point the upper arm is drawn 36° out of the picture plane.
 * - Lockout: straight arms angled out to the hands, about 20° from vertical for a 200% grip on this body's shoulders
 *   (Arm.out and forearmOut), so the bar locks out a few cm lower than in the Bench: the shorter range the guide says.
 */
private val wideGripBench: ExerciseFigure by lazy {
    val bar = Point(benchChest.x, benchChest.y - Barbell.SHAFT_RADIUS)
    val grip = Segment.FOREARM.length + Segment.HAND.length * Body.GRIP
    val elbow = Point(bar.x, bar.y + grip)
    val upper = elbow - benchBare.shoulder
    val out = kotlin.math.acos(upper.length() / Segment.UPPER_ARM.length) * 180 / kotlin.math.PI
    val upperAngle = angleTo(upper)
    val bottom = Arm(shoulder = fold180(upperAngle - benchTorso - 180.0), elbow = fold180(90.0 - upperAngle), out = out)
    val lockout = Arm(shoulder = 90.0, elbow = 0.0, out = 20.0, forearmOut = 20.0)
    benchVariation(
        start = benchPose(bottom),
        end = benchPose(lockout),
        startCaption = "Wider than normal grip",
        guideLines = listOf(
            RACK_SAFETY, BENCH_LIE_LINE,
            "Grip the bar wider than your normal bench grip, but no wider than you can control.",
            "Lower the bar to your chest.", LOCKOUT_LINE,
        ),
        checks = listOf(BarTouches(BodyPoint.CHEST, Which.START), LockoutOverShoulders(Which.END)),
    )
}

/**
 * Barbell floor press. Lying on the floor inside a rack, knees bent and feet flat; START the upper arms on the floor,
 * END straight arms over the shoulders.
 * - Bottom (guide; ExRx floor press, https://exrx.net/WeightExercises/PectoralSternal/BBFloorPress: lower until the
 *   triceps rest on the floor): the elbows on the floor, the forearms vertical. The upper arms are out from the sides
 *   along the floor, so seen from the side they are foreshortened: 50° out of the plane (Arm.out), a neutral angle
 *   that puts the bar over the chest; the true elbow bend is then about 99°.
 * - Feet: flat, 55 cm past the hips (neutral), the knees bent.
 * - Rack (guide's safety line): safety arms at 30 cm, under the bar's lowest point and above the chest; J-hooks 5 cm
 *   under the lockout.
 */
private val floorPress: ExerciseFigure by lazy {
    val torso = 180.0
    val pelvis = Point(95.0, -Body.TORSO_RADIUS)
    val bare = Pose(pelvis.x, pelvis.y, torso, nearArm = Arm(0.0, 0.0), nearLeg = Leg(0.0, 0.0)).skeleton()
    val legs = Reach.leg(pelvis, Point(pelvis.x + 55.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
    val out = 50.0
    val drawn = Segment.UPPER_ARM.length * kotlin.math.cos(out * kotlin.math.PI / 180)
    val drop = -Body.UPPER_ARM_WIDTH / 2 - bare.shoulder.y
    val upper = Point(kotlin.math.sqrt(drawn * drawn - drop * drop), drop)
    val upperAngle = angleTo(upper)
    val bottom = Arm(shoulder = fold180(upperAngle - torso - 180.0), elbow = fold180(90.0 - upperAngle), out = out)
    val lock = Arm(90.0, 0.0)
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, torso, nearArm = arm, nearLeg = legs)
    val lockBar = pose(lock).skeleton().near.grip
    val rack = PowerRack(x = 2.0, jHookHeight = -lockBar.y - 5.0, safetyHeight = 30.0)
    val props = listOf(Floor, rack, Barbell())
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(pose(bottom), props),
        end = Scene(pose(lock), props),
        startCaption = "Upper arms touch floor",
        endCaption = "Press to straight arms",
        thumbnail = Which.START,
        guideLines = listOf(
            "Lie on the floor inside a power rack, under the bar, with your knees bent.",
            RACK_SAFETY,
            "Lower the bar until your upper arms touch the floor.",
            "Press the bar up until your arms are straight.",
        ),
        contacts = listOf(
            Contact(BodyPoint.HEAD, Surface.FLOOR, BOTH),
            Contact(BodyPoint.UPPER_BACK, Surface.FLOOR, BOTH),
            Contact(BodyPoint.HIPS, Surface.FLOOR, BOTH),
            Contact(BodyPoint.FEET, Surface.FLOOR, BOTH),
            Contact(BodyPoint.ELBOWS, Surface.FLOOR, setOf(Which.START)),
        ),
        checks = listOf(LockoutOverShoulders(Which.END)),
    )
}

// ---- Deadlift variations ----

/**
 * A conventional pull set-up, the Deadlift's solver: feet flat (soles at [floorY]), the shins leaning [shinLean]°, the
 * bar's centre at [barY] and [barGap] cm in front of the shin's surface, the shoulders [lead] cm ahead of the bar with
 * straight arms, and the hips where the thigh and torso meet.
 */
private fun pullPose(barY: Double, shinLean: Double, lead: Double, floorY: Double = 0.0, barGap: Double = 0.5): Pose {
    val ankle = Point(0.0, floorY - Body.ANKLE_HEIGHT)
    val rad = kotlin.math.PI / 180
    val shinAtBar = (ankle.y - barY) * kotlin.math.tan(shinLean * rad)
    val barX = shinAtBar + (Body.SHIN_WIDTH / 2 + barGap + Barbell.SHAFT_RADIUS) / kotlin.math.cos(shinLean * rad)
    val bar = Point(barX, barY)
    val shoulder = Point(barX + lead, barY - kotlin.math.sqrt(ARM_REACH * ARM_REACH - lead * lead))
    return pullPoseThrough(ankle, shinLean, shoulder, bar)
}

/** The pull pose whose shoulders are at [shoulder] with straight arms down to [bar], the shin from [ankle] at [shinLean]°. */
private fun pullPoseThrough(ankle: Point, shinLean: Double, shoulder: Point, bar: Point): Pose {
    val thigh = Segment.THIGH.length
    val knee = ankle + unit(90.0 - shinLean) * Segment.SHIN.length
    val hip = circleMeet(knee, thigh, shoulder, Segment.TORSO.length)
    val torso = angleTo(shoulder - hip)
    val thighAngle = angleTo(knee - hip)
    val leg = Leg(hip = fold180(thighAngle - torso - 180.0), knee = fold180(angleTo(hip - knee) - (90.0 - shinLean)), ankle = shinLean)
    return Pose(hip.x, hip.y, torso, nearArm = straightArm(shoulder, bar, torso), nearLeg = leg)
}

/** The Deadlift's lockout, moved up by [floorY] (a lifter standing on a platform). */
private fun pullLockout(floorY: Double = 0.0): Pose = deadlift.end.pose.let { it.copy(pelvisY = it.pelvisY + floorY) }

/** The pull from a higher bar, the bar still over the same x as the floor set-up: the shin lean that keeps it [gap] off the shin. */
private fun pullFromHeight(barY: Double, lead: Double, gap: Double = 0.5): Pose {
    val x0 = Barbell().center(deadlift.start.pose.skeleton()).x
    var lo = 0.0
    var hi = 20.0
    repeat(60) {
        val mid = (lo + hi) / 2
        val rad = mid * kotlin.math.PI / 180
        val x = (-Body.ANKLE_HEIGHT - barY) * kotlin.math.tan(rad) + (Body.SHIN_WIDTH / 2 + gap + Barbell.SHAFT_RADIUS) / kotlin.math.cos(rad)
        if (x > x0) hi = mid else lo = mid
    }
    return pullPose(barY, shinLean = lo, lead = lead, barGap = gap)
}

private const val PULL_STANCE_LINE = "Stand with your feet flat under the bar, about shoulder-width apart, with your shins close to it."
private val pullChecks = listOf(
    BarOverMidfoot(Which.START), BarOverMidfoot(Which.END),
    BarNearLegs(within = 4.0, at = Which.START), BarNearLegs(within = 4.0, at = Which.END),
)

/**
 * Paused deadlift. START the Deadlift's set-up; END the pause just after the bar leaves the floor: the plates 5 cm up
 * (a neutral "just after", visible at phone size). The hips rise with the bar (the Deadlift's "at the same rate as your
 * hips"): the shoulders stay just ahead of the bar and the shins come back so the bar stays over the same point.
 */
private val pausedDeadlift: ExerciseFigure by lazy {
    val barY = -Barbell.PLATE_DIAMETER / 2 - 5.0
    val props = deadlift.start.props
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(deadlift.start.pose, props),
        end = Scene(pullFromHeight(barY, lead = 1.0), props),
        startCaption = "Shins close to the bar",
        endCaption = "Hold just off the floor",
        thumbnail = Which.END,
        guideLines = listOf(
            PULL_STANCE_LINE,
            "Stop just after it leaves the floor and hold still for a moment.",
            "Keep your back flat and the bar close during the pause.",
        ),
        contacts = deadlift.contacts,
        checks = pullChecks,
    )
}

/**
 * Paused deadlift at the knee. START the Deadlift's set-up; END the pause with the bar just below the knees: the bar's
 * top 4 cm under the knee joint (a neutral "just below"), the shins back near vertical so the bar stays over the
 * mid-foot, and the shoulders 3 cm ahead of the bar (ExRx's "slightly in front", https://exrx.net/Kinesiology/DeadliftForm).
 */
private val pausedDeadliftAtKnee: ExerciseFigure by lazy {
    // The knee joint's height with a near-vertical shin, then the bar just under it.
    val kneeHeight = Body.ANKLE_HEIGHT + Segment.SHIN.length * kotlin.math.cos(4.0 * kotlin.math.PI / 180)
    val barY = -(kneeHeight - 4.0 - Barbell.SHAFT_RADIUS)
    val props = deadlift.start.props
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(deadlift.start.pose, props),
        end = Scene(pullFromHeight(barY, lead = 3.0, gap = 1.0), props),
        startCaption = "Shins close to the bar",
        endCaption = "Bar just below knees",
        thumbnail = Which.END,
        guideLines = listOf(
            PULL_STANCE_LINE,
            "Stop with the bar just below your knees and hold still for a moment.",
            "Keep your back flat and the bar close during the pause.",
        ),
        contacts = deadlift.contacts,
        checks = pullChecks,
    )
}

/**
 * Deficit deadlift. Standing on a 5 cm platform (Kit.kt DeficitPlatform; ExRx's deficit deadlift,
 * https://exrx.net/WeightExercises/ErectorSpinae/BBDeficitDeadlift, uses a plate or a shallow platform and fixes no
 * height, so the common 5 cm is used), the plates on the floor. START the Deadlift's set-up rule from 5 cm lower:
 * the shins lean 14° (2° more than the Deadlift), so the hips are lower ("hips low, shoulders high"); END the lockout
 * on the platform.
 */
private val deficitDeadlift: ExerciseFigure by lazy {
    val platform = DeficitPlatform(x = 6.0)
    val floorY = -platform.height
    val start = pullPose(barY = -Barbell.PLATE_DIAMETER / 2, shinLean = 14.0, lead = 1.0, floorY = floorY)
    val props = listOf(Floor, platform, Barbell())
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(start, props),
        end = Scene(pullLockout(floorY), props),
        startCaption = "Stand on a low platform",
        endCaption = "Stand tall, knees locked",
        thumbnail = Which.START,
        guideLines = listOf(
            "Stand on a weight plate or a low, sturdy platform, with the bar over your feet.",
            "Keep your hips low, your shoulders high, and your arms and back straight.",
            "Stand tall with your knees locked.",
        ),
        contacts = listOf(Contact(BodyPoint.FEET, Surface.PLATFORM, BOTH)),
        checks = pullChecks,
    )
}

/**
 * Block pull. The plates on 10 cm blocks (the guide's middle height; ExRx's block pull,
 * https://exrx.net/WeightExercises/GluteusMaximus/BBBlockPull, gives 5, 10 or 15 cm). START the Deadlift's set-up
 * rule from the higher bar: the shins 8° forward, the bar against them, the shoulders just ahead; END the lockout.
 */
private val blockPull: ExerciseFigure by lazy {
    val blockHeight = 10.0
    val start = pullPose(barY = -Barbell.PLATE_DIAMETER / 2 - blockHeight, shinLean = 8.0, lead = 1.0)
    val bar = Barbell().center(start.skeleton())
    // Blocks a little narrower than the plates (42 cm), so they read as under the plates and not as a step under the feet.
    val props = listOf(Floor, PullingBlocks(x = bar.x, height = blockHeight, length = 42.0), Barbell())
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(start, props),
        end = Scene(deadlift.end.pose, props),
        startCaption = "Plates on sturdy blocks",
        endCaption = "Stand tall, knees locked",
        thumbnail = Which.START,
        guideLines = listOf(
            "Set the plates on sturdy blocks, about 5, 10 or 15 cm high.",
            "Keep your hips low, your shoulders high, and your arms and back straight.",
            "Stand tall with your knees locked.",
        ),
        contacts = deadlift.contacts,
        checks = pullChecks + BarRestsOn(Surface.BLOCK, Which.START),
    )
}

/**
 * Sumo deadlift, front view: the guide's points are the very wide stance and the hands inside the legs, which only a
 * front view shows. START the set-up, END standing tall.
 * - Feet: very wide, the ankles 36 cm out from the middle, inside the plates (the bar's inner collars are 131 cm
 *   apart). The shins are vertical over the feet.
 * - Knees out over the feet: the thighs run from the hips out to the knees and forward, towards the camera, so they
 *   look short and near level from the front (Leg.out). Their true abduction is kept just inside the 40° hip
 *   abduction limit (Body.kt), the conservative side of a sumo stance.
 * - Hands: about shoulder-width (the guide), the straight arms hanging down between the knees to the bar on the
 *   floor, angled 6° out (neutral) so the far arm shows beside the torso rather than hidden behind it.
 * - Torso: leaning 40° forward, towards the camera (Pose.lean), so 50° above level from the side: more upright than
 *   the conventional pull (https://pmc.ncbi.nlm.nih.gov/articles/PMC12148905/: a significantly smaller trunk
 *   inclination at the start with the wider stance; also https://pmc.ncbi.nlm.nih.gov/articles/PMC9022700/). With
 *   straight arms to the bar and the thighs at 40° abduction that puts the hips about 51 cm up, just above the knees,
 *   and the 3D hip flexion about 118°. This is the guide's "chest high, hips low".
 * - END: hips and knees straight on the same wide feet (the legs 18° out), torso upright, shoulders back.
 */
private val sumoDeadlift: ExerciseFigure by lazy {
    val rad = kotlin.math.PI / 180
    val lean = 40.0
    val ankleOut = 36.0
    val hipHalf = Body.HIP_HALF_WIDTH
    // START: shins vertical, so the knee is straight over the ankle; the shoulders over the hands, the bar on the floor.
    val knee = Point(ankleOut, -(Body.ANKLE_HEIGHT + Segment.SHIN.length))
    val armOut = 6.0
    val shoulderY = -Barbell.PLATE_DIAMETER / 2 - ARM_REACH * kotlin.math.cos(armOut * rad)
    val pelvisY = shoulderY + Segment.TORSO.length * kotlin.math.cos(lean * rad)
    val thighDrawn = knee - Point(hipHalf, pelvisY)
    val thighAngle = angleTo(thighDrawn)
    val out = kotlin.math.acos(thighDrawn.length() / Segment.THIGH.length) / rad
    val setupLeg = Leg(hip = fold180(thighAngle - 270.0), knee = fold180(thighAngle + 90.0), ankle = 0.0, out = out)
    val arms = Arm(shoulder = armOut, elbow = 0.0)
    val start = Pose(0.0, pelvisY, 90.0, nearArm = arms, nearLeg = setupLeg, lean = lean)
    // END: straight legs from the hips to the same ankles.
    val spread = ankleOut - hipHalf
    val legLength = Segment.THIGH.length + Segment.SHIN.length
    val abduction = kotlin.math.asin(spread / legLength) / rad
    val standLeg = Leg(hip = abduction, knee = 0.0, ankle = -abduction)
    val end = Pose(0.0, -(Body.ANKLE_HEIGHT + legLength * kotlin.math.cos(abduction * rad)), 90.0, nearArm = arms, nearLeg = standLeg)
    val props = listOf(Floor, Barbell())
    ExerciseFigure(
        view = View.FRONT,
        start = Scene(start, props),
        end = Scene(end, props),
        startCaption = "Grip between your legs",
        endCaption = "Stand tall at the top",
        thumbnail = Which.START,
        guideLines = listOf(
            "Stand with your feet very wide and under the bar.",
            "Squat down and grip the bar between your legs, shoulder-width apart, with a mixed grip: one palm up, one down.",
            "Keep your arms straight, your chest high, your hips low and your back straight.",
            "At the top, stand tall with your shoulders back.",
        ),
        contacts = deadlift.contacts,
        checks = listOf(HandsInsideKnees(Which.START)),
    )
}

/**
 * Trap-bar deadlift. Standing inside the hex bar (Kit.kt TrapBar), the hands on its side handles, so the handles are
 * beside the legs over the mid-foot and the arms hang straight down at the sides.
 * - START: hips low, shoulders high (guide). A typical hex-bar start has the knees at about 95–100° (inside angle),
 *   with the trunk more upright than a straight-bar pull (https://pmc.ncbi.nlm.nih.gov/articles/PMC9022700/;
 *   ExRx's deadlift variations, https://exrx.net/Kinesiology/DeadliftVariations: more of the load on the knees). The
 *   shins lean 14° and the shoulders are straight over the handles; the knees come out at about 92° of bend (an
 *   inside angle of 88°, a little more bent than that study's start, which with this body's long arms is what
 *   brings the hips low and the torso clearly more upright than the Deadlift's, as the guide asks).
 * - END: standing tall with the hips and knees straight; the body tipped 5° forward from the ankles so the handles
 *   stay over the mid-foot.
 */
private const val TRAP_SHIN = 14.0

/** The handles inside the leg's drawn outline, front to back: within half the shin's 11 cm width of its centre line. */
private const val HANDLES_WITHIN = 5.5

private val trapBarDeadlift: ExerciseFigure by lazy {
    val ankle = Point(0.0, -Body.ANKLE_HEIGHT)
    val handle = Point(midfootX(0.0), -Barbell.PLATE_DIAMETER / 2)
    val start = pullPoseThrough(ankle, TRAP_SHIN, Point(handle.x, handle.y - ARM_REACH), handle)
    val leg = standingLeg(0.0, 5.0, 0.0, 90.0)
    // The arms hang straight down beside the body, the handles under the shoulders.
    val end = standOn(leg, 90.0, Arm(0.0, 0.0))
    val props = listOf(Floor, TrapBar)
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(start, props),
        end = Scene(end, props),
        startCaption = "Hips low, shoulders high",
        endCaption = "Hips and knees straight",
        thumbnail = Which.START,
        guideLines = listOf(
            "Stand inside the bar with your feet shoulder-width apart or narrower.",
            "Squat down with your feet flat and grip the handles.",
            "Keep your hips low, your shoulders high, and your arms and back straight.",
            "Stand up by straightening your hips and knees fully.",
        ),
        contacts = deadlift.contacts,
        checks = listOf(
            BarOverMidfoot(Which.START), BarOverMidfoot(Which.END),
            HandlesBesideLegs(within = HANDLES_WITHIN, at = Which.START), HandlesBesideLegs(within = HANDLES_WITHIN, at = Which.END),
        ),
    )
}

internal val mainLiftFigures: Map<String, ExerciseFigure> by lazy {
    mapOf(
        "Squat" to squat, "Bench" to bench, "Deadlift" to deadlift, "Strict press" to strictPress,
        "Pause squat" to pauseSquat, "Front squat" to frontSquat, "Pin squat" to pinSquat,
        "Safety-bar squat" to safetyBarSquat, "Box squat" to boxSquat, "High-bar squat" to highBarSquat,
        "Tempo squat" to tempoSquat,
        "Spoto press" to spotoPress, "Long-pause bench" to longPauseBench, "Pin press" to pinPress,
        "Larsen press" to larsenPress, "Close-grip bench" to closeGripBench, "Board press" to boardPress,
        "Barbell floor press" to floorPress, "Wide-grip bench" to wideGripBench,
        "Deficit deadlift" to deficitDeadlift, "Paused deadlift" to pausedDeadlift,
        "Paused deadlift at the knee" to pausedDeadliftAtKnee, "Block pull" to blockPull,
        "Sumo deadlift" to sumoDeadlift, "Trap-bar deadlift" to trapBarDeadlift,
    )
}
