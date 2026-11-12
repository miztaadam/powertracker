package com.powertracker.domain.library.figures.areas

import com.powertracker.domain.library.areas.RACK_SAFETY
import com.powertracker.domain.library.figures.BarNearLegs
import com.powertracker.domain.library.figures.BarOverMidfoot
import com.powertracker.domain.library.figures.Barbell
import com.powertracker.domain.library.figures.Body
import com.powertracker.domain.library.figures.BodyPoint
import com.powertracker.domain.library.figures.Contact
import com.powertracker.domain.library.figures.ExerciseFigure
import com.powertracker.domain.library.figures.Floor
import com.powertracker.domain.library.figures.Scene
import com.powertracker.domain.library.figures.Surface
import com.powertracker.domain.library.figures.View
import com.powertracker.domain.library.figures.Which
import com.powertracker.domain.library.figures.skeleton
import kotlin.math.abs

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

internal val hamstringGluteFigures: Map<String, Lazy<ExerciseFigure>> by lazy { mapOf("RDL" to lazy { rdl }) }
