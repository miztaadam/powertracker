package com.powertracker.domain.library.figures.areas

import com.powertracker.domain.library.figures.AdjustableBench
import com.powertracker.domain.library.figures.Arm
import com.powertracker.domain.library.figures.Body
import com.powertracker.domain.library.figures.BodyPoint
import com.powertracker.domain.library.figures.Contact
import com.powertracker.domain.library.figures.Dumbbell
import com.powertracker.domain.library.figures.ExerciseFigure
import com.powertracker.domain.library.figures.FlatBench
import com.powertracker.domain.library.figures.Floor
import com.powertracker.domain.library.figures.LockoutOverShoulders
import com.powertracker.domain.library.figures.Point
import com.powertracker.domain.library.figures.Pose
import com.powertracker.domain.library.figures.Reach
import com.powertracker.domain.library.figures.Scene
import com.powertracker.domain.library.figures.Segment
import com.powertracker.domain.library.figures.Side
import com.powertracker.domain.library.figures.Surface
import com.powertracker.domain.library.figures.View
import com.powertracker.domain.library.figures.Which

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

internal val shoulderFigures: Map<String, ExerciseFigure> by lazy { mapOf("DB shoulder press" to dbShoulderPress) }
