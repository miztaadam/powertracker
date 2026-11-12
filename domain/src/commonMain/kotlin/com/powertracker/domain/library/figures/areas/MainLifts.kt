package com.powertracker.domain.library.figures.areas

import com.powertracker.domain.library.areas.RACK_SAFETY
import com.powertracker.domain.library.figures.Arm
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

internal val mainLiftFigures: Map<String, ExerciseFigure> by lazy {
    mapOf("Squat" to squat, "Bench" to bench, "Deadlift" to deadlift, "Strict press" to strictPress)
}
