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

    @Test
    fun chinOverBar_passesAboveTheBar_failsBelowIt() {
        // Hanging straight under a bar, then the same body 70 cm higher: the chin goes from far below to above it.
        val hang = Pose(0.0, -120.0, nearArm = Arm(180.0, 0.0), nearLeg = Leg(0.0, 0.0))
        val grip = hang.skeleton().near.grip
        val bar = PullUpBar(x = grip.x, height = -grip.y)
        val high = hang.copy(pelvisY = hang.pelvisY - 70.0, nearArm = Arm(180.0, 0.0))
        val chin = high.skeleton().chin()
        assertTrue(chin.y < -bar.height - PullUpBar.RADIUS, "fixture: the raised chin is over the bar")
        val figure = FigureFixtures.figureOf(View.SIDE, Scene(hang, listOf(bar)), end = Scene(high, listOf(bar)), checks = listOf(ChinOverBar(at = Which.END)))
        assertEquals(emptyList(), problems(figure).filter { "chin" in it.message })
        val low = problems(figure.copy(end = Scene(hang, listOf(bar)))).filter { "chin" in it.message }
        assertEquals(1, low.size, low.toString())
        assertEquals(Which.END, low.single().which)
        // Clearing the bar by less than the margin (4 cm) fails: the picture must show the chin plainly over it.
        val barTop = -bar.height - PullUpBar.RADIUS
        val justOver = hang.copy(pelvisY = hang.pelvisY - (hang.skeleton().chin().y - barTop) - 2.0)
        val thin = problems(figure.copy(end = Scene(justOver, listOf(bar)))).filter { "chin" in it.message }
        assertEquals(1, thin.size, thin.toString())
        // No bar in the scene is reported, not passed.
        assertTrue(problems(figure.copy(end = Scene(high, listOf(Floor)))).any { "no pull-up bar" in it.message })
    }

    @Test
    fun barNearLegs_passesBesideTheShins_failsAwayFromThem() {
        val props = listOf(Floor, Barbell())
        // Arms hanging, leaning 10° forward: the bar hangs in front of the thighs.
        val pose = FigureFixtures.standing(torso = 80.0, arm = Arm(10.0, 0.0))
        val s = pose.skeleton()
        val gap = FigureChecks.barToLegs(Barbell().center(s), s)
        val near = FigureFixtures.figureOf(View.SIDE, Scene(pose, props), checks = listOf(BarNearLegs(gap + 1.0, Which.START)))
        assertEquals(emptyList(), problems(near).filter { "legs" in it.message })
        val far = problems(near.copy(checks = listOf(BarNearLegs(gap - 1.0, Which.START)))).filter { "legs" in it.message }
        assertEquals(1, far.size, far.toString())
        // A bar pushed into the legs fails too (arms along the torso: the grip is inside the thigh).
        val into = pose.copy(nearArm = Arm(0.0, 0.0), farArm = Arm(0.0, 0.0))
        assertTrue(problems(near.copy(start = Scene(into, props), checks = listOf(BarNearLegs(10.0, Which.START)))).any { "into the legs" in it.message })
    }
    @Test
    fun barAbove_passesInItsRange_failsOnTheChest() {
        val check = listOf(BarAbove(BodyPoint.CHEST, min = 2.0, max = 3.0, at = Which.START))
        val above = FigureFixtures.benchDraft(barLift = 2.5).copy(checks = check)
        assertEquals(emptyList(), problems(above))
        val touching = problems(FigureFixtures.benchDraft().copy(checks = check))
        assertEquals(1, touching.size, touching.toString())
        assertTrue("above CHEST" in touching.single().message, touching.single().message)
        assertTrue(problems(FigureFixtures.benchDraft(barLift = 5.0).copy(checks = check)).single().message.contains("needs"))
    }

    @Test
    fun barRestsOn_pinsAndBoards_passResting_failOffThem() {
        val draft = FigureFixtures.benchDraft(barLift = 10.0)
        val bar = draft.start.pose.skeleton().near.grip
        fun pins(lift: Double) = draft.copy(
            start = draft.start.copy(props = listOf(Floor, FlatBench(), PowerRack(x = 2.0, jHookHeight = 110.0, safetyHeight = -bar.y - Barbell.SHAFT_RADIUS - lift), Barbell())),
            checks = listOf(BarRestsOn(Surface.SAFETY_ARM, Which.START)),
        )
        assertEquals(emptyList(), problems(pins(0.0)))
        assertTrue(problems(pins(4.0)).single().message.contains("off the SAFETY_ARM"))
        // Pins too short to reach under the bar are no rest either.
        val short = pins(0.0).let { f -> f.copy(start = f.start.copy(props = f.start.props.map { if (it is PowerRack) it.copy(armLength = 20.0) else it })) }
        assertEquals(1, problems(short).size)

        val chest = draft.start.pose.skeleton().chest()
        fun boards(barLift: Double) = FigureFixtures.benchDraft(barLift = barLift).let { f ->
            f.copy(start = f.start.copy(props = f.start.props + BenchBoards(chest.x, chest.y)), checks = listOf(BarRestsOn(Surface.BOARD, Which.START)))
        }
        assertEquals(emptyList(), problems(boards(2 * BenchBoards.BOARD)))
        assertTrue(problems(boards(4.0)).single().message.contains("into the BOARD"))
        // No such surface in the scene is reported, not passed.
        assertTrue(problems(FigureFixtures.benchDraft().copy(checks = listOf(BarRestsOn(Surface.BLOCK)))).single().message.contains("no BLOCK"))
    }

    @Test
    fun barRestsOn_blocks_platesOnTheBlocks() {
        val pose = FigureFixtures.standing(torso = 80.0, arm = Arm(10.0, 0.0))
        val bar = Barbell().center(pose.skeleton())
        val rim = -bar.y - Barbell.PLATE_DIAMETER / 2
        fun blocks(height: Double) = FigureFixtures.figureOf(
            View.SIDE, Scene(pose, listOf(Floor, PullingBlocks(bar.x, height), Barbell())), checks = listOf(BarRestsOn(Surface.BLOCK, Which.START)),
        )
        assertEquals(emptyList(), problems(blocks(rim)))
        assertTrue(problems(blocks(rim - 4.0)).single().message.contains("off the BLOCK"))
    }

    @Test
    fun handlesBesideLegs_passesAtTheSides_failsInFront() {
        val props = listOf(Floor, TrapBar)
        val sides = FigureFixtures.figureOf(View.SIDE, Scene(FigureFixtures.standing(), props), checks = listOf(HandlesBesideLegs(5.5, Which.START)))
        assertEquals(emptyList(), problems(sides))
        val s = FigureFixtures.standing().skeleton()
        assertEquals(0.0, FigureChecks.legLineOffset(TrapBar.center(s), s)!!, 0.01)
        val inFront = problems(sides.copy(start = Scene(FigureFixtures.standing(arm = Arm(25.0, 0.0)), props)))
        assertEquals(1, inFront.size, inFront.toString())
        assertTrue("handles" in inFront.single().message, inFront.single().message)
        // Hands above the hips are beside no leg at all.
        val up = problems(sides.copy(start = Scene(FigureFixtures.standing(arm = Arm(90.0, 0.0)), props)))
        assertTrue(up.single().message.contains("not beside the legs"), up.toString())
    }

    @Test
    fun safetySquatBar_handsOnTheHandles() {
        val base = FigureFixtures.standing()
        val bare = base.skeleton()
        val arm = Reach.arm(bare.shoulder, safetyBarHandle(bare), 90.0)
        val on = FigureFixtures.figureOf(View.SIDE, Scene(base.copy(nearArm = arm, farArm = arm), listOf(Floor, SafetySquatBar)))
        assertEquals(emptyList(), problems(on))
        val off = problems(on.copy(start = Scene(base, listOf(Floor, SafetySquatBar))))
        assertEquals(2, off.size, off.toString()) // both hands
        assertTrue(off.all { "safety bar's handle" in it.message })
        // Its plates are the load over the mid-foot, not the yoke.
        val s = on.start.pose.skeleton()
        assertEquals(SafetySquatBar.center(s) + Point(SafetySquatBar.CAMBER_FORWARD, SafetySquatBar.CAMBER), safetyBarLoad(s))
    }
    @Test
    fun handsInsideKnees_passesBetweenWideKnees_failsOutsideThem() {
        // Front view: knees bent out sideways, the arms hanging straight down from the shoulders.
        val wide = Pose(0.0, -70.0, 90.0, nearArm = Arm(0.0, 0.0), nearLeg = Leg(60.0, 60.0))
        val props = listOf(Floor, Barbell())
        val f = FigureFixtures.figureOf(View.FRONT, Scene(wide, props), checks = listOf(HandsInsideKnees(Which.START)))
        assertEquals(emptyList(), problems(f).filter { "inside knees" in it.message })
        // Knees together (legs straight down): the hands are outside them.
        val narrow = problems(f.copy(start = Scene(wide.copy(nearLeg = Leg(0.0, 0.0)), props))).filter { "inside knees" in it.message }
        assertEquals(1, narrow.size, narrow.toString())
        // It is a front-view check.
        assertTrue(problems(f.copy(view = View.SIDE)).any { "front-view check" in it.message })
    }

    // ---- chest batch checks (Task 6) ----

    private fun one(pose: Pose, props: List<Prop>, check: KeyCheck, view: View = View.SIDE) =
        FigureChecks.check("Bench", FigureFixtures.figureOf(view, Scene(pose, props), checks = listOf(check))).filter {
            !it.message.startsWith("contact") && !it.message.startsWith("joint") && "goes through" !in it.message
        }

    @Test
    fun elbowBend_andHandsLevelWith_passAndFail() {
        val bent = FigureFixtures.standing(arm = Arm(60.0, 20.0))
        assertEquals(emptyList(), one(bent, listOf(Floor), ElbowBend(10.0, 30.0, Which.START)))
        assertTrue(one(bent, listOf(Floor), ElbowBend(0.0, 5.0, Which.START)).any { it.message.startsWith("elbow bend 20") })
        // Only the side asked for.
        val mixed = bent.copy(farArm = Arm(60.0, 0.0))
        assertEquals(emptyList(), one(mixed, listOf(Floor), ElbowBend(10.0, 30.0, Which.START, Side.NEAR)))
        assertTrue(one(mixed, listOf(Floor), ElbowBend(10.0, 30.0, Which.START)).isNotEmpty())
        val level = FigureFixtures.standing(arm = Arm(90.0, 0.0))
        assertEquals(emptyList(), one(level, listOf(Floor), HandsLevelWith(BodyPoint.SHOULDERS, 1.0, Which.START)))
        assertTrue(one(level.copy(nearArm = Arm(70.0, 0.0)), listOf(Floor), HandsLevelWith(BodyPoint.SHOULDERS, 3.0, Which.START)).any { "off level" in it.message })
    }

    @Test
    fun heldOverAndBeside_passAndFail() {
        val up = FigureFixtures.standing(arm = Arm(180.0, 0.0))
        val props = listOf(Floor, Dumbbell(Side.NEAR))
        assertEquals(emptyList(), one(up, props, HeldOver(BodyPoint.SHOULDERS, 1.0, Which.START)))
        assertTrue(one(up.copy(nearArm = Arm(160.0, 0.0)), props, HeldOver(BodyPoint.SHOULDERS, 3.0, Which.START)).any { "off the line" in it.message })
        val s = up.skeleton()
        val d = (s.near.grip - s.head).length()
        assertEquals(emptyList(), one(up, props, HeldBeside(BodyPoint.HEAD, d + 0.1, Which.START)))
        assertTrue(one(up, props, HeldBeside(BodyPoint.HEAD, d - 1.0, Which.START)).any { "from HEAD" in it.message })
        assertTrue(one(up, listOf(Floor), HeldOver(BodyPoint.SHOULDERS, 1.0, Which.START)).any { "nothing held" in it.message })
    }

    @Test
    fun handsTogether_chestOffFloor_handsOffFloor_passAndFail() {
        val apart = FigureFixtures.standing(arm = Arm(90.0, 0.0))
        assertTrue(one(apart, listOf(Floor), HandsTogether(10.0, Which.START), View.FRONT).any { "apart" in it.message })
        val inward = Arm(90.0, 0.0, out = 70.0, forearmOut = 70.0)
        val together = apart.copy(nearArm = inward.copy(shoulder = 30.0))
        val gap = together.skeleton(View.FRONT).let { kotlin.math.abs(it.near.grip.x - it.far.grip.x) }
        assertEquals(emptyList(), one(together, listOf(Floor), HandsTogether(gap + 0.1, Which.START), View.FRONT))
        // A prone body: chest and hands measured off the floor.
        val prone = Pose(0.0, -20.0, torso = 0.0, nearArm = Arm(90.0, 0.0), nearLeg = Leg(0.0, 0.0))
        val chest = -prone.skeleton().chest().y
        assertEquals(emptyList(), one(prone, listOf(Floor), ChestOffFloor(chest - 1.0, chest + 1.0, Which.START)))
        assertTrue(one(prone, listOf(Floor), ChestOffFloor(0.0, chest - 2.0, Which.START)).any { "chest" in it.message })
        val air = prone.copy(pelvisY = -90.0)
        assertEquals(emptyList(), one(air, listOf(Floor), HandsOffFloor(5.0, Which.START)))
        assertTrue(one(prone.copy(pelvisY = -70.0), listOf(Floor), HandsOffFloor(5.0, Which.START)).any { "hands" in it.message })
    }

    @Test
    fun upperArmLevel_andShouldersOverHands_passAndFail() {
        val level = FigureFixtures.standing(arm = Arm(-90.0, 90.0, out = 30.0)) // elbow straight back, level
        assertEquals(emptyList(), one(level, listOf(Floor), UpperArmLevel(2.0, Which.START)).filter { "upper arm" in it.message && "level" in it.message })
        assertTrue(one(level.copy(nearArm = Arm(-50.0, 90.0, out = 30.0)), listOf(Floor), UpperArmLevel(5.0, Which.START)).any { "off level" in it.message })
        val down = FigureFixtures.standing(arm = Arm(0.0, 0.0))
        assertEquals(emptyList(), one(down, listOf(Floor), ShouldersOverHands(1.0, Which.START)))
        assertTrue(one(down.copy(nearArm = Arm(20.0, 0.0)), listOf(Floor), ShouldersOverHands(5.0, Which.START)).any { "shoulders" in it.message })
    }

    @Test
    fun upperArmDown_passAndFail() {
        // Upright: hanging along the torso passes; raised 30° forward fails a 20° bound. Bent over, the torso's line counts.
        val down = FigureFixtures.standing(arm = Arm(5.0, 120.0))
        assertEquals(emptyList(), one(down, listOf(Floor), UpperArmDown(10.0, Which.START)).filter { "torso's line" in it.message })
        assertTrue(one(down.copy(nearArm = Arm(30.0, 120.0)), listOf(Floor), UpperArmDown(20.0, Which.START)).any { "off the torso's line" in it.message })
        val bent = Pose(0.0, -90.0, torso = 30.0, nearArm = Arm(0.0, 0.0), nearLeg = Leg(60.0, 0.0))
        assertTrue(one(bent, emptyList(), UpperArmDown(2.0, Which.START)).none { "torso's line" in it.message })
    }
}
