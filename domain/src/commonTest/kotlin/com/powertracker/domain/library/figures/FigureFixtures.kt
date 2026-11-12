package com.powertracker.domain.library.figures

import com.powertracker.domain.library.areas.RACK_SAFETY

/**
 * The proving fixture: the bench press draft (docs/library/pictures/bench-press-figure-draft.svg) drawn with the kit.
 * Side view, lying on a flat bench inside a rack with the safety arms below the chest; START the bar on the chest,
 * END lockout. Used only by tests, with knobs to break it in the ways the checks must catch.
 */
internal object FigureFixtures {
    const val PELVIS_X = 95.0
    const val ANKLE_X = 127.0

    fun benchDraft(barLift: Double = 0.0, footLift: Double = 0.0): ExerciseFigure {
        val pelvisY = -(FlatBench.HEIGHT + Body.TORSO_RADIUS)
        val bare = Pose(PELVIS_X, pelvisY, torso = 180.0, nearArm = Arm(0.0, 0.0), nearLeg = Leg(0.0, 0.0)).skeleton()
        val chest = bare.point(BodyPoint.CHEST)
        val leg = Reach.leg(bare.pelvis, Point(ANKLE_X, -Body.ANKLE_HEIGHT - footLift), torso = 180.0, foot = 0.0)
        val onChest = Reach.arm(bare.shoulder, Point(chest.x, chest.y - Barbell.SHAFT_RADIUS - barLift), torso = 180.0)
        val props = listOf(Floor, FlatBench(), PowerRack(x = 2.0, jHookHeight = 110.0, safetyHeight = 60.0), Barbell())
        return ExerciseFigure(
            view = View.SIDE,
            start = Scene(Pose(PELVIS_X, pelvisY, torso = 180.0, nearArm = onChest, nearLeg = leg), props),
            end = Scene(Pose(PELVIS_X, pelvisY, torso = 180.0, nearArm = Arm(90.0, 0.0), nearLeg = leg), props),
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
                Contact(BodyPoint.FEET, Surface.FLOOR, Which.entries.toSet()),
                Contact(BodyPoint.HEAD, Surface.BENCH, Which.entries.toSet()),
                Contact(BodyPoint.UPPER_BACK, Surface.BENCH, Which.entries.toSet()),
                Contact(BodyPoint.HIPS, Surface.BENCH, Which.entries.toSet()),
            ),
            checks = listOf(BarTouches(BodyPoint.CHEST, Which.START), LockoutOverShoulders(Which.END)),
        )
    }

    /** The hip joint's height when standing with straight legs and the feet flat. */
    val STANDING_PELVIS_Y = -(Body.ANKLE_HEIGHT + Segment.SHIN.length + Segment.THIGH.length)

    /** A small hand-built figure for kit and check tests: both panels the same unless [end] is given. */
    fun figureOf(
        view: View,
        start: Scene,
        end: Scene = start,
        contacts: List<Contact> = emptyList(),
        checks: List<KeyCheck> = emptyList(),
    ) = ExerciseFigure(view, start, end, "Start", "End", Which.START, emptyList(), contacts, checks)

    fun standing(
        x: Double = 0.0,
        lift: Double = 0.0,
        torso: Double = 90.0,
        arm: Arm = Arm(0.0, 0.0),
        farArm: Arm = arm,
        farLeg: Leg? = null,
    ): Pose {
        // Legs straight down whatever the torso's lean.
        val leg = Leg(90.0 - torso, 0.0)
        return Pose(x, STANDING_PELVIS_Y - lift, torso, nearArm = arm, nearLeg = leg, farArm = farArm, farLeg = farLeg ?: leg)
    }

    val ALL = Which.entries.toSet()
}
