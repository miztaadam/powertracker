package com.powertracker.domain.library.figures.areas

import com.powertracker.domain.library.figures.Arm
import com.powertracker.domain.library.figures.Body
import com.powertracker.domain.library.figures.BodyPoint
import com.powertracker.domain.library.figures.Contact
import com.powertracker.domain.library.figures.ExerciseFigure
import com.powertracker.domain.library.figures.Floor
import com.powertracker.domain.library.figures.LegPress45
import com.powertracker.domain.library.figures.Point
import com.powertracker.domain.library.figures.Pose
import com.powertracker.domain.library.figures.Reach
import com.powertracker.domain.library.figures.Scene
import com.powertracker.domain.library.figures.Segment
import com.powertracker.domain.library.figures.Surface
import com.powertracker.domain.library.figures.View
import com.powertracker.domain.library.figures.Which
import com.powertracker.domain.library.figures.surfaces
import kotlin.math.abs

// Figures for the library's Quads area (library/areas/Quads.kt), keyed by catalogue name.

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
    val top = sledFor(15.0)
    val bottom = sledFor(85.0)
    val arms = Arm(shoulder = 15.0, elbow = 25.0)
    fun scene(press: LegPress45) = Scene(Pose(pelvis.x, pelvis.y, back, nearArm = arms, nearLeg = legs(press)), listOf(Floor, press))
    ExerciseFigure(
        view = View.SIDE,
        start = scene(top),
        end = scene(bottom),
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

internal val quadFigures: Map<String, Lazy<ExerciseFigure>> by lazy { mapOf("Leg press" to lazy { legPress }) }
