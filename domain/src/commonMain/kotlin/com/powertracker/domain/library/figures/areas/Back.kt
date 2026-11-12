package com.powertracker.domain.library.figures.areas

import com.powertracker.domain.library.figures.Arm
import com.powertracker.domain.library.figures.BarTouches
import com.powertracker.domain.library.figures.Body
import com.powertracker.domain.library.figures.BodyPoint
import com.powertracker.domain.library.figures.CableStack
import com.powertracker.domain.library.figures.ChinOverBar
import com.powertracker.domain.library.figures.Contact
import com.powertracker.domain.library.figures.Dumbbell
import com.powertracker.domain.library.figures.ExerciseFigure
import com.powertracker.domain.library.figures.FlatBench
import com.powertracker.domain.library.figures.Floor
import com.powertracker.domain.library.figures.LatBar
import com.powertracker.domain.library.figures.LatPulldownSeat
import com.powertracker.domain.library.figures.Leg
import com.powertracker.domain.library.figures.Point
import com.powertracker.domain.library.figures.Pose
import com.powertracker.domain.library.figures.PullUpBar
import com.powertracker.domain.library.figures.Reach
import com.powertracker.domain.library.figures.Scene
import com.powertracker.domain.library.figures.Segment
import com.powertracker.domain.library.figures.Side
import com.powertracker.domain.library.figures.Surface
import com.powertracker.domain.library.figures.View
import com.powertracker.domain.library.figures.Which
import com.powertracker.domain.library.figures.point
import com.powertracker.domain.library.figures.skeleton
import kotlin.math.abs

// Figures for the library's Back area (library/areas/Back.kt), keyed by catalogue name.

/**
 * Pull-up. START hanging with straight arms; END pulled up until the chin clears the bar. Body upright and still, one
 * leg crossed over the other (knees bent) in both panels. No floor: the lifter hangs clear of it.
 * - View: side, so the chin over the bar shows; the wide grip is the guide's setup, not something this view can show.
 * - END: the bar about 19 cm in front of the shoulder joints, in front of the throat, with the elbows bent down and
 *   forward; the chin about 5.5 cm over the bar's top, plainly clear (ExRx pull-up,
 *   https://exrx.net/WeightExercises/LatissimusDorsi/BWPullup: pull until the chin is over the bar; ACE pull-ups,
 *   https://www.acefitness.org/resources/everyone/exercise-library/191/pull-ups/). The torso stays upright (90°) for
 *   the guide's "upright, without swinging".
 * - Legs crossed with the knees bent well back (neutral), which keeps the hanging figure short enough to read.
 * - The bar on a wall bracket (Kit.kt PullUpBar), so its height reads across the picture.
 */
private val pullUp: ExerciseFigure by lazy {
    val bar = Point(0.0, -200.0)
    val hangArm = Arm(180.0, 0.0)
    val near = Leg(hip = 25.0, knee = 110.0, ankle = -20.0)
    val far = Leg(hip = 20.0, knee = 120.0, ankle = -25.0)
    val hangShoulder = bar + Point(0.0, ARM_REACH)
    val hangPelvis = hangShoulder + Point(0.0, Segment.TORSO.length)
    val start = Pose(hangPelvis.x, hangPelvis.y, 90.0, nearArm = hangArm, nearLeg = near, farLeg = far)
    // The chin plainly over the bar: the shoulder joint 2 cm below the bar's centre puts the chin about 5.5 cm over its top.
    val topShoulder = bar + Point(-19.0, 2.0)
    val pull = Reach.arm(topShoulder, bar, 90.0)
    val topPelvis = topShoulder + Point(0.0, Segment.TORSO.length)
    val end = Pose(topPelvis.x, topPelvis.y, 90.0, nearArm = pull, nearLeg = near, farLeg = far)
    val props = listOf(PullUpBar(x = bar.x, height = -bar.y))
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(start, props),
        end = Scene(end, props),
        startCaption = "Hang with straight arms",
        endCaption = "Chin clears the bar",
        thumbnail = Which.END,
        guideLines = listOf(
            "Hang with straight arms.",
            "Cross one leg over the other, and brace your abs.",
            "Pull yourself up until your chin clears the bar.",
            "Keep your body upright, without swinging.",
        ),
        contacts = listOf(Contact(BodyPoint.HANDS, Surface.PULL_UP_BAR, BOTH)),
        checks = listOf(ChinOverBar(at = Which.END)),
    )
}

/**
 * Lat pulldown. Seated with the thighs under the roller pad, feet flat. START the arms fully stretched up to the bar;
 * END the bar pulled down to the upper chest. The trunk leans back 10° in both panels: the guide's "only slightly"
 * (ExRx cable pulldown, https://exrx.net/WeightExercises/LatissimusDorsi/CBFrontPulldown; ACE seated lat pulldown,
 * https://www.acefitness.org/resources/everyone/exercise-library/158/seated-lat-pulldown/); 10° is a neutral "slightly".
 * - View: side, like the guide's sources; the wide grip is the guide's setup, not something this view can show.
 * - Seat 48 cm, roller on the thighs near the knees, the pulley over the bar's start: typical machine sizes, neutral.
 */
private val latPulldown: ExerciseFigure by lazy {
    val torso = 100.0
    val seatHeight = 48.0
    val pelvis = Point(0.0, -(seatHeight + Body.TORSO_RADIUS))
    val legs = Reach.leg(pelvis, Point(42.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
    val shoulder = shoulderOf(pelvis, torso)
    val up = straightArm(shoulder, shoulder + Point(0.0, -10.0), torso)
    val bare = Pose(pelvis.x, pelvis.y, torso, nearArm = up, nearLeg = legs).skeleton()
    val chest = bare.point(BodyPoint.UPPER_CHEST) + unit(torso - 90.0) * 1.3
    val down = armTo(shoulder, chest, torso)
    // The roller sits on the thighs, 80% of the way to the knee.
    val thigh = bare.near.knee - bare.near.hip
    val pad = bare.near.hip + thigh * 0.8 + unit(angleTo(thigh) + 90.0) * (Body.THIGH_WIDTH / 2 + LatPulldownSeat.PAD_RADIUS)
    val seat = LatPulldownSeat(x = -6.0, padX = pad.x, seatHeight = seatHeight, padHeight = -pad.y)
    val grip = bare.near.grip
    val stack = CableStack(x = -60.0, reach = grip.x + 60.0)
    val props = listOf(Floor, stack, seat, LatBar)
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(Pose(pelvis.x, pelvis.y, torso, nearArm = up, nearLeg = legs), props),
        end = Scene(Pose(pelvis.x, pelvis.y, torso, nearArm = down, nearLeg = legs), props),
        startCaption = "Arms fully stretched",
        endCaption = "Bar to your upper chest",
        thumbnail = Which.END,
        guideLines = listOf(
            "Sit with your thighs under the pads.",
            "Lean back only slightly, using your legs to support your body.",
            "Pull the bar down to your upper chest.",
            "Return under control until your arms and shoulders are fully stretched.",
        ),
        contacts = listOf(
            Contact(BodyPoint.HIPS, Surface.SEAT, BOTH),
            Contact(BodyPoint.THIGHS, Surface.THIGH_PAD, BOTH),
            Contact(BodyPoint.FEET, Surface.FLOOR, BOTH),
        ),
        checks = listOf(BarTouches(BodyPoint.UPPER_CHEST, Which.END)),
    )
}

/**
 * One-arm DB row. The far knee and far hand on a flat bench, the near foot on the floor beside it and slightly back;
 * the near hand rows the dumbbell. START the arm hanging straight; END the dumbbell pulled up to the lower ribs: the
 * upper arm 25° behind the torso line, the elbow up past the back, the forearm hanging under it (the guide's "until it
 * touches your ribs").
 * - Torso 12° above level: the guide's "close to level". The bench hand is flat under the shoulder with the elbow
 *   slightly bent; with the thigh upright on the bench the figure's fixed lengths need that much rise.
 * - ExRx dumbbell bent-over row (https://exrx.net/WeightExercises/BackGeneral/DBBentOverRow) and ACE single-arm row
 *   (https://www.acefitness.org/resources/everyone/exercise-library/126/single-arm-row/): the same kneeling set-up.
 */
private val oneArmRow: ExerciseFigure by lazy {
    val torso = 12.0
    val bench = FlatBench(x = 0.0, nearLimbsBeside = true)
    // The knee 6.6 cm over the pad: the thigh's thicker round end (7 cm) rests on it, the knee within the contact tolerance.
    val knee = Point(36.0, -(FlatBench.HEIGHT + 6.6))
    val pelvis = knee + Point(0.0, -Segment.THIGH.length)
    val shoulder = shoulderOf(pelvis, torso)
    // Far knee on the bench: the thigh straight down, the shin back along the pad, the foot off the end.
    val kneel = Leg(hip = fold180(270.0 - torso - 180.0), knee = 90.0, ankle = -40.0)
    // The near foot slightly back of the hip: with the thigh upright on the bench the standing leg is close to straight.
    val standing = Reach.leg(pelvis, Point(pelvis.x - 4.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
    // Far hand flat on the bench under the shoulder: the wrist bent back, the hand's underside on the pad.
    val wrist = -65.0
    val handArm = (0..200).map { i -> -FlatBench.HEIGHT - 2.0 - i * 0.05 }.map { y ->
        Reach.arm(shoulder, Point(shoulder.x + 8.0, y), torso, wrist)
    }.minBy { arm ->
        val s = Pose(pelvis.x, pelvis.y, torso, nearArm = arm, nearLeg = standing, farArm = arm, farLeg = kneel).skeleton()
        val lowest = maxOf(s.far.wrist.y, s.far.hand.y) + Body.HAND_WIDTH / 2
        abs(lowest + FlatBench.HEIGHT)
    }
    val hang = Arm(shoulder = fold180(270.0 - torso - 180.0), elbow = 0.0)
    // The elbow drawn up and back past the torso, the forearm hanging straight down: the dumbbell at the lower ribs.
    val rowShoulder = -25.0
    val rowed = Arm(shoulder = rowShoulder, elbow = fold180(270.0 - (torso + 180.0 + rowShoulder)))
    fun pose(arm: Arm) = Pose(pelvis.x, pelvis.y, torso, nearArm = arm, nearLeg = standing, farArm = handArm, farLeg = kneel)
    val props = listOf(Floor, bench, Dumbbell(Side.NEAR))
    ExerciseFigure(
        view = View.SIDE,
        start = Scene(pose(hang), props),
        end = Scene(pose(rowed), props),
        startCaption = "Arm straight",
        endCaption = "Pull to your ribs",
        thumbnail = Which.END,
        guideLines = listOf(
            "Kneel with one knee on a bench, and put the hand on the same side on the bench, under your shoulder.",
            "Set your other foot on the floor, slightly back and out to the side.",
            "Keep your torso close to level with the floor. Move the knee or hand on the bench forward or back to level it.",
            "Pull the dumbbell up to your side, until it touches your ribs or your upper arm is just past level with your torso.",
            "Lower it under control until your arm is straight and your shoulder is stretched down.",
        ),
        contacts = listOf(
            Contact(BodyPoint.FAR_HAND, Surface.BENCH, BOTH),
            Contact(BodyPoint.FAR_KNEE, Surface.BENCH, BOTH),
            Contact(BodyPoint.NEAR_FOOT, Surface.FLOOR, BOTH),
        ),
        checks = emptyList(),
    )
}

internal val backFigures: Map<String, Lazy<ExerciseFigure>> by lazy {
    mapOf("Pull-up" to lazy { pullUp }, "Lat pulldown" to lazy { latPulldown }, "One-arm DB row" to lazy { oneArmRow })
}
