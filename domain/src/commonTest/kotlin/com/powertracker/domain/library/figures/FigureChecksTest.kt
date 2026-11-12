package com.powertracker.domain.library.figures

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FigureChecksTest {
    private val draft = FigureFixtures.benchDraft()

    private fun problems(figure: ExerciseFigure) = FigureChecks.check("Bench", figure)

    @Test
    fun barTouchesChest_passesForDraft_failsWhenBarRaised5cm() {
        assertEquals(emptyList(), problems(draft))
        val raised = problems(FigureFixtures.benchDraft(barLift = 5.0))
        assertEquals(1, raised.size, raised.toString())
        assertEquals(Which.START, raised.single().which)
        assertTrue("bar" in raised.single().message && "CHEST" in raised.single().message, raised.single().message)
    }

    @Test
    fun lockoutOverShoulders_failsWithBentElbows() {
        val bent = draft.copy(end = draft.end.copy(pose = draft.end.pose.copy(nearArm = Arm(90.0, 30.0), farArm = Arm(90.0, 30.0))))
        val found = problems(bent)
        assertTrue(found.any { it.which == Which.END && "lockout" in it.message }, found.toString())
    }

    @Test
    fun footOffFloor_reportsContact() {
        val found = problems(FigureFixtures.benchDraft(footLift = 4.0))
        assertTrue(found.isNotEmpty())
        assertTrue(found.all { "FEET" in it.message && "FLOOR" in it.message }, found.toString())
        assertEquals(setOf(Which.START, Which.END), found.mapNotNull { it.which }.toSet())
    }

    @Test
    fun limbThroughBench_reportsIntersection() {
        // Swing the near thigh down through the pad (hip extended 20°, inside the joint range).
        val leg = draft.start.pose.nearLeg
        val through = draft.copy(start = draft.start.copy(pose = draft.start.pose.copy(nearLeg = leg.copy(hip = -20.0, knee = 60.0))))
        val found = problems(through).filter { "through" in it.message }
        assertTrue(found.any { it.which == Which.START && "near thigh" in it.message && "bench" in it.message }, found.toString())
    }

    @Test
    fun captionOver24_reported() {
        val long = problems(draft.copy(startCaption = "Lower the bar to your chest"))
        assertEquals(1, long.size, long.toString())
        assertTrue("caption" in long.single().message)
        assertTrue(problems(draft.copy(endCaption = "  ")).single().message.contains("caption"))
    }

    @Test
    fun guideLineNotInGuide_reported() {
        val found = problems(draft.copy(guideLines = draft.guideLines + "Bounce the bar off your chest."))
        assertEquals(1, found.size, found.toString())
        assertTrue("guide" in found.single().message)
        // A name outside the catalogue is reported too.
        assertTrue(FigureChecks.check("Bench pressing", draft).any { "catalogue" in it.message })
    }

    @Test
    fun barOverMidfoot_passesOverTheFoot_failsInFrontOfIt() {
        val props = listOf(Floor, Barbell())
        // Leaning 10° forward with the arms hanging straight down: the bar is over the middle of the foot.
        val over = FigureFixtures.figureOf(
            View.SIDE, Scene(FigureFixtures.standing(torso = 80.0, arm = Arm(10.0, 0.0)), props),
            checks = listOf(BarOverMidfoot(Which.START)),
        )
        assertEquals(emptyList(), problems(over))
        // Upright with the arms along the torso, the bar hangs over the ankle, 8 cm behind the mid-foot.
        val behind = problems(over.copy(start = Scene(FigureFixtures.standing(), props)))
        assertEquals(1, behind.size, behind.toString())
        assertEquals(Which.START, behind.single().which)
        assertTrue("mid-foot" in behind.single().message, behind.single().message)
    }

    @Test
    fun hipDepth_passesBelowTheKnee_failsStanding() {
        val pelvis = Point(-20.0, -38.0)
        val squat = Pose(
            pelvis.x, pelvis.y, torso = 70.0, nearArm = Arm(0.0, 0.0),
            nearLeg = Reach.leg(pelvis, Point(0.0, -Body.ANKLE_HEIGHT), torso = 70.0, foot = 0.0),
        )
        assertTrue(squat.skeleton().near.hip.y > squat.skeleton().near.knee.y, "the fixture squat is below parallel")
        val stand = Scene(FigureFixtures.standing(), listOf(Floor))
        val deep = FigureFixtures.figureOf(View.SIDE, stand, end = Scene(squat, listOf(Floor)), checks = listOf(HipDepth(0.0, Which.END)))
        assertEquals(emptyList(), problems(deep).filter { "hip depth" in it.message })
        val shallow = problems(deep.copy(end = stand)).filter { "hip depth" in it.message }
        assertEquals(1, shallow.size, shallow.toString())
        assertEquals(Which.END, shallow.single().which)
    }

    @Test
    fun grips_bothHandsOnOneBar_sideView() {
        val props = listOf(Floor, Barbell())
        val together = FigureFixtures.figureOf(View.SIDE, Scene(FigureFixtures.standing(), props))
        assertEquals(emptyList(), problems(together))
        val apart = problems(together.copy(end = Scene(FigureFixtures.standing(farArm = Arm(0.0, 40.0)), props)))
        assertEquals(1, apart.size, apart.toString())
        assertEquals(Which.END, apart.single().which)
        assertTrue("held: the hands are" in apart.single().message, apart.single().message)
        // A bar across the upper back needs no grip on it.
        val onBack = listOf(Floor, Barbell(BarHold.UPPER_BACK))
        assertEquals(emptyList(), problems(FigureFixtures.figureOf(View.SIDE, Scene(FigureFixtures.standing(farArm = Arm(0.0, 40.0)), onBack))))
    }

    @Test
    fun shapeOutsideViewBox_reported() {
        assertEquals(emptyList(), FigureChecks.outsideViewBox(listOf(Circle(Role.BODY, 200.0, 150.0, 20.0))))
        val outside = FigureChecks.outsideViewBox(
            listOf(Circle(Role.BODY, 398.0, 150.0, 5.0), Capsule(Role.EQUIPMENT, 10.0, -2.0, 50.0, 10.0, 2.0), RoundRect(Role.ACCENT, 10.0, 10.0, 5.0, 5.0, 1.0)),
        )
        assertEquals(2, outside.size, outside.toString())
        assertTrue(outside.all { it.startsWith("shape outside the view box") })
    }
}
