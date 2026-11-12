package com.powertracker.domain.library.figures.areas

import com.powertracker.domain.library.areas.RACK_SAFETY
import com.powertracker.domain.library.figures.Arm
import com.powertracker.domain.library.figures.BarTouches
import com.powertracker.domain.library.figures.Barbell
import com.powertracker.domain.library.figures.Body
import com.powertracker.domain.library.figures.BodyPoint
import com.powertracker.domain.library.figures.Contact
import com.powertracker.domain.library.figures.ExerciseFigure
import com.powertracker.domain.library.figures.FlatBench
import com.powertracker.domain.library.figures.Floor
import com.powertracker.domain.library.figures.Leg
import com.powertracker.domain.library.figures.LockoutOverShoulders
import com.powertracker.domain.library.figures.Point
import com.powertracker.domain.library.figures.Pose
import com.powertracker.domain.library.figures.PowerRack
import com.powertracker.domain.library.figures.Reach
import com.powertracker.domain.library.figures.Scene
import com.powertracker.domain.library.figures.Surface
import com.powertracker.domain.library.figures.View
import com.powertracker.domain.library.figures.Which
import com.powertracker.domain.library.figures.point
import com.powertracker.domain.library.figures.skeleton

// Figures for the library's main lifts (library/areas/MainLifts.kt), keyed by catalogue name.

private val BOTH = Which.entries.toSet()

/**
 * Bench: from the approved draft (docs/library/pictures/bench-press-figure-draft.svg). Side view, lying on a flat
 * bench (IPF: pad top 42–45 cm) inside a rack, with the safety arms below the chest as the guide's safety line asks.
 * START: the bar paused on the chest; END: lockout over the shoulders. Feet flat on the floor; head, upper back and
 * buttocks on the bench (the guide's setup line). Task 4 reviews it against the sources.
 */
private val bench: ExerciseFigure by lazy {
    val torso = 180.0 // face up, head to the left
    val pelvis = Point(95.0, -(FlatBench.HEIGHT + Body.TORSO_RADIUS))
    val bare = Pose(pelvis.x, pelvis.y, torso, nearArm = Arm(0.0, 0.0), nearLeg = Leg(0.0, 0.0)).skeleton()
    val chest = bare.point(BodyPoint.CHEST)
    val legs = Reach.leg(pelvis, Point(127.0, -Body.ANKLE_HEIGHT), torso, foot = 0.0)
    val onChest = Reach.arm(bare.shoulder, Point(chest.x, chest.y - Barbell.SHAFT_RADIUS), torso)
    val lockout = Arm(shoulder = 90.0, elbow = 0.0)
    // J-hooks at the IPF's highest bar rest (110 cm), just under the straight-arm hold; safety arms 60 cm, under the
    // chest (the bar touches it about 65 cm up).
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

internal val mainLiftFigures: Map<String, ExerciseFigure> by lazy { mapOf("Bench" to bench) }
