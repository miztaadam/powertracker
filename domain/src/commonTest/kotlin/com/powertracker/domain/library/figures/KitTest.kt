package com.powertracker.domain.library.figures

import com.powertracker.domain.library.figures.FigureFixtures.ALL
import com.powertracker.domain.library.figures.FigureFixtures.figureOf
import com.powertracker.domain.library.figures.FigureFixtures.standing
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Smoke tests for every kit piece and the front view: each renders inside the view box, and its surfaces hold contacts. */
class KitTest {
    private fun problems(figure: ExerciseFigure) = FigureChecks.check("Bench", figure)

    private fun contactProblems(figure: ExerciseFigure) = problems(figure).filter { it.message.startsWith("contact") }

    /** Both panels render inside the view box with at least [roles], and the given kit shape kinds appear. */
    private fun assertRenders(figure: ExerciseFigure, roles: Set<Role>) {
        for (which in Which.entries) {
            val shapes = FigureRenderer.shapes(figure, which)
            assertEquals(emptyList(), FigureChecks.outsideViewBox(shapes))
            val present = shapes.map { it.role }.toSet()
            assertTrue(present.containsAll(roles), "missing ${roles - present}")
        }
    }

    @Test
    fun dumbbells_sideView_inBothHands_feetOnFloor() {
        val props = listOf(Floor, Dumbbell(Side.NEAR), Dumbbell(Side.FAR))
        val feet = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, ALL))
        val figure = figureOf(View.SIDE, Scene(standing(), props), contacts = feet)
        assertRenders(figure, setOf(Role.BODY, Role.BODY_FAR, Role.GAP, Role.EQUIPMENT, Role.ACCENT, Role.ACCENT_FILL))
        assertEquals(emptyList(), problems(figure))
        // The dumbbells sit in the hands.
        val s = standing().skeleton()
        assertEquals(s.near.grip, Dumbbell(Side.NEAR).center(s))
        assertEquals(s.far.grip, Dumbbell(Side.FAR).center(s))

        val lifted = contactProblems(figureOf(View.SIDE, Scene(standing(lift = 3.0), props), contacts = feet))
        assertEquals(setOf("near foot", "far foot"), lifted.map { it.message.substringAfter(": ").substringBefore(" is") }.toSet())
    }

    @Test
    fun frontView_dumbbellPress_mirrorsTheFarSide_andStandsOnTheFloor() {
        val press = Arm(shoulder = 90.0, elbow = 90.0) // upper arms out to the sides, forearms up
        val props = listOf(Floor, FlatBench(x = 150.0), Dumbbell(Side.NEAR), Dumbbell(Side.FAR))
        val feet = listOf(Contact(BodyPoint.FEET, Surface.FLOOR, ALL))
        val figure = figureOf(View.FRONT, Scene(standing(arm = press), props), contacts = feet)
        assertRenders(figure, setOf(Role.BODY, Role.GAP, Role.EQUIPMENT, Role.ACCENT))
        assertTrue(FigureRenderer.shapes(figure, Which.START).none { it.role == Role.BODY_FAR }, "front-view limbs are all BODY")
        assertEquals(emptyList(), problems(figure))

        val s = standing(arm = press).skeleton(View.FRONT)
        // Near side to the right, far side its mirror image about the torso's axis.
        assertTrue(s.near.shoulder.x > s.shoulder.x && s.far.shoulder.x < s.shoulder.x)
        for ((near, far) in listOf(s.near.grip to s.far.grip, s.near.toe to s.far.toe, s.near.heel to s.far.heel)) {
            assertEquals(near.x, -far.x, 1e-6)
            assertEquals(near.y, far.y, 1e-6)
        }
        assertTrue(s.near.grip.y < s.near.elbow.y, "forearm up")

        val lifted = contactProblems(figureOf(View.FRONT, Scene(standing(lift = 3.0, arm = press), props), contacts = feet))
        assertEquals(4, lifted.size, lifted.toString()) // both feet, both panels
    }

    @Test
    fun frontView_barbellLockoutInARack_levelGrips() {
        val lockout = Arm(shoulder = 170.0, elbow = 0.0)
        val rack = PowerRack(x = 0.0, jHookHeight = 140.0, safetyHeight = 80.0)
        val props = listOf(Floor, rack, Barbell())
        val figure = figureOf(View.FRONT, Scene(standing(arm = lockout), props), checks = listOf(LockoutOverShoulders(Which.START)))
        assertRenders(figure, setOf(Role.BODY, Role.EQUIPMENT, Role.ACCENT, Role.ACCENT_FILL))
        assertEquals(emptyList(), problems(figure))

        val uneven = problems(figureOf(View.FRONT, Scene(standing(arm = lockout, farArm = Arm(170.0, 30.0)), props), checks = figure.checks))
        assertTrue(uneven.any { it.which == Which.START && "hands are" in it.message }, uneven.toString())
        assertTrue(uneven.any { it.which == Which.START && "lockout: elbows bent" in it.message }, uneven.toString())
    }

    @Test
    fun latPulldown_seatPadCableAndBar_holdContacts() {
        fun scene(padLift: Double): Scene {
            val pelvis = Point(0.0, -(48.0 + Body.TORSO_RADIUS))
            val legs = Reach.leg(pelvis, Point(45.0, -Body.ANKLE_HEIGHT), torso = 90.0, foot = 0.0)
            val pose = Pose(pelvis.x, pelvis.y, 90.0, nearArm = Arm(170.0, 10.0), nearLeg = legs)
            val s = pose.skeleton()
            // The roller sits on the thigh, 80% of the way to the knee.
            val thighAngle = angleOf(s.near.knee - s.near.hip)
            val on = s.near.hip + (s.near.knee - s.near.hip) * 0.8 +
                dir(thighAngle + 90.0) * (Body.THIGH_WIDTH / 2 + LatPulldownSeat.PAD_RADIUS + padLift)
            val seat = LatPulldownSeat(x = -8.0, padX = on.x, seatHeight = 48.0, padHeight = -on.y)
            val stack = CableStack(x = -60.0, reach = s.near.grip.x + 60.0)
            return Scene(pose, listOf(Floor, stack, seat, LatBar))
        }
        val contacts = listOf(
            Contact(BodyPoint.HIPS, Surface.SEAT, ALL),
            Contact(BodyPoint.FEET, Surface.FLOOR, ALL),
            Contact(BodyPoint.THIGHS, Surface.THIGH_PAD, ALL),
        )
        val figure = figureOf(View.SIDE, scene(0.0), contacts = contacts)
        assertRenders(figure, setOf(Role.BODY, Role.BODY_FAR, Role.EQUIPMENT, Role.ACCENT))
        assertEquals(emptyList(), problems(figure))
        // The cable runs from the pulley to the bar.
        val s = figure.start.pose.skeleton()
        val bar = FigureRenderer.toViewBox(figure, LatBar.center(s))
        assertTrue(
            FigureRenderer.shapes(figure, Which.START).filterIsInstance<Capsule>()
                .any { it.role == Role.EQUIPMENT && abs(it.x2 - bar.x) < 0.02 && abs(it.y2 - bar.y) < 0.02 },
        )

        val raised = contactProblems(figureOf(View.SIDE, scene(4.0), contacts = contacts))
        assertEquals(4, raised.size, raised.toString()) // both thighs, both panels
        assertEquals(setOf("contact THIGHS on THIGH_PAD: near thigh", "contact THIGHS on THIGH_PAD: far thigh"), raised.map { it.message.substringBefore(" is") }.toSet())
    }

    @Test
    fun frontView_latBarOnACable_rendersAndKeepsGripsLevel() {
        val overhead = Arm(150.0, 0.0)
        val figure = figureOf(View.FRONT, Scene(standing(arm = overhead), listOf(Floor, CableStack(x = 0.0, reach = 0.0), LatBar)))
        assertRenders(figure, setOf(Role.BODY, Role.EQUIPMENT, Role.ACCENT))
        assertEquals(emptyList(), problems(figure))
        val s = figure.start.pose.skeleton(View.FRONT)
        assertEquals(0.0, LatBar.center(s).x, 1e-6)
    }

    @Test
    fun pullUpBar_frontAndSide_handsOnTheBar() {
        val hang = Arm(170.0, 0.0)
        fun figure(view: View, barLift: Double): ExerciseFigure {
            val pose = Pose(0.0, -100.0, nearArm = hang, nearLeg = Leg(0.0, 0.0))
            val grip = pose.skeleton(view).near.grip
            val bar = PullUpBar(x = if (view == View.SIDE) grip.x else 0.0, height = -grip.y + barLift)
            return figureOf(view, Scene(pose, listOf(bar)), contacts = listOf(Contact(BodyPoint.HANDS, Surface.PULL_UP_BAR, ALL)))
        }
        for (view in View.entries) {
            assertRenders(figure(view, 0.0), setOf(Role.BODY, Role.EQUIPMENT))
            assertEquals(emptyList(), problems(figure(view, 0.0)), view.name)
            val off = contactProblems(figure(view, 4.0))
            assertEquals(4, off.size, off.toString()) // both hands, both panels
            assertTrue(off.all { "PULL_UP_BAR" in it.message })
        }
    }

    @Test
    fun oneHandOnTheBench_nearAndFarPointsAreSeparate() {
        // Leaning over a bench, the near hand on the pad with a straight arm, the far arm hanging.
        val pose = standing(x = -20.0, torso = 30.0, arm = Arm(60.0, 0.0), farArm = Arm(0.0, 0.0))
        val s = pose.skeleton()
        val benchTop = maxOf(s.near.wrist.y, s.near.hand.y) + Body.HAND_WIDTH / 2
        val props = listOf(Floor, FlatBench(x = 0.0, height = -benchTop))
        val figure = figureOf(
            View.SIDE, Scene(pose, props),
            contacts = listOf(Contact(BodyPoint.NEAR_HAND, Surface.BENCH, ALL), Contact(BodyPoint.NEAR_FOOT, Surface.FLOOR, ALL)),
        )
        assertRenders(figure, setOf(Role.BODY, Role.EQUIPMENT))
        assertEquals(emptyList(), problems(figure))

        val farHand = contactProblems(figure.copy(contacts = listOf(Contact(BodyPoint.FAR_HAND, Surface.BENCH, ALL))))
        assertEquals(2, farHand.size)
        assertTrue(farHand.all { "far hand" in it.message })

        val farLegUp = pose.copy(farLeg = Leg(100.0, 0.0))
        val feet = listOf(Contact(BodyPoint.NEAR_FOOT, Surface.FLOOR, ALL), Contact(BodyPoint.FAR_FOOT, Surface.FLOOR, ALL))
        val footProblems = contactProblems(figureOf(View.SIDE, Scene(farLegUp, props), contacts = feet))
        assertTrue(footProblems.isNotEmpty() && footProblems.all { "FAR_FOOT" in it.message && "far foot" in it.message }, footProblems.toString())
    }

    @Test
    fun kneesOnTheFloor_pluralPoint() {
        // Kneeling upright: thighs down, shins back along the floor.
        val kneelY = -(Body.SHIN_WIDTH / 2 + Segment.THIGH.length)
        fun figure(lift: Double) = figureOf(
            View.SIDE,
            Scene(Pose(0.0, kneelY - lift, nearArm = Arm(0.0, 0.0), nearLeg = Leg(0.0, 90.0, ankle = -50.0)), listOf(Floor)),
            contacts = listOf(Contact(BodyPoint.KNEES, Surface.FLOOR, ALL)),
        )
        assertEquals(emptyList(), contactProblems(figure(0.0)))
        val off = contactProblems(figure(3.0)).map { it.message.substringBefore(" is") }.toSet()
        assertEquals(setOf("contact KNEES on FLOOR: near knee", "contact KNEES on FLOOR: far knee"), off)
    }

    @Test
    fun legPress45_angledPads_holdTheBackAndHips() {
        val press = LegPress45(x = 0.0, sled = 20.0)
        // The torso lies along the reclined back pad (its front face 6 cm out), 10 cm off it, the pelvis on the seat.
        val corner = Point(-30.0, -LegPress45.SEAT_HEIGHT)
        val axisStart = corner + dir(LegPress45.BACK_ANGLE - 90.0) * (6.0 + Body.TORSO_RADIUS)
        val pelvisY = -(LegPress45.SEAT_HEIGHT + Body.TORSO_RADIUS)
        val t = (pelvisY - axisStart.y) / dir(LegPress45.BACK_ANGLE).y
        fun figure(lift: Double): ExerciseFigure {
            val pelvis = axisStart + dir(LegPress45.BACK_ANGLE) * t + dir(LegPress45.BACK_ANGLE - 90.0) * lift
            val legs = Reach.leg(pelvis, Point(35.0, -80.0), torso = LegPress45.BACK_ANGLE, foot = 0.0)
            val pose = Pose(pelvis.x, pelvis.y, LegPress45.BACK_ANGLE, nearArm = Arm(0.0, 0.0), nearLeg = legs)
            return figureOf(
                View.SIDE, Scene(pose, listOf(Floor, press)),
                contacts = listOf(Contact(BodyPoint.HIPS, Surface.SEAT, ALL), Contact(BodyPoint.UPPER_BACK, Surface.BACK_PAD, ALL)),
            )
        }
        val seated = figure(0.0)
        assertRenders(seated, setOf(Role.BODY, Role.EQUIPMENT))
        // The reclined back pad, the foot plate and the sled behind it are drawn as polygons.
        assertEquals(3, FigureRenderer.shapes(seated, Which.START).count { it is Polygon && it.role == Role.EQUIPMENT })
        assertEquals(emptyList(), contactProblems(seated))
        val lifted = contactProblems(figure(4.0)).map { it.message.substringBefore(":") }.toSet()
        assertEquals(setOf("contact HIPS on SEAT", "contact UPPER_BACK on BACK_PAD"), lifted)

        // The foot plate moves with the sled; the seat and back pad do not.
        val low = press.surfaces(View.SIDE)
        val high = press.copy(sled = 40.0).surfaces(View.SIDE)
        assertEquals(low[Surface.SEAT], high[Surface.SEAT])
        assertEquals(low[Surface.BACK_PAD], high[Surface.BACK_PAD])
        val moved = high.getValue(Surface.FOOT_PLATE).single().let { Point(it.cx, it.cy) } -
            low.getValue(Surface.FOOT_PLATE).single().let { Point(it.cx, it.cy) }
        assertEquals(20.0, moved.length(), 1e-6)
        assertEquals(LegPress45.RAIL_ANGLE, angleOf(moved), 1e-6)
    }

    @Test
    fun adjustableBench_upright_seatAndBackPadHoldTheHipsBackAndHead() {
        val bench = AdjustableBench(x = 0.0)
        val hinge = adjustableHinge(bench)
        fun figure(lean: Double): ExerciseFigure {
            // Sitting upright against the back pad: the pelvis on the seat, the torso axis one torso radius in front of it.
            val pelvis = Point(hinge.x + Body.TORSO_RADIUS + lean, hinge.y - Body.TORSO_RADIUS)
            val legs = Reach.leg(pelvis, Point(pelvis.x + 40.0, -Body.ANKLE_HEIGHT), torso = 90.0, foot = 0.0)
            val pose = Pose(pelvis.x, pelvis.y, 90.0, nearArm = Arm(0.0, 0.0), nearLeg = legs)
            return figureOf(
                View.SIDE, Scene(pose, listOf(Floor, bench)),
                contacts = listOf(
                    Contact(BodyPoint.HIPS, Surface.SEAT, ALL), Contact(BodyPoint.UPPER_BACK, Surface.BACK_PAD, ALL),
                    Contact(BodyPoint.HEAD, Surface.BACK_PAD, ALL), Contact(BodyPoint.FEET, Surface.FLOOR, ALL),
                ),
            )
        }
        assertRenders(figure(0.0), setOf(Role.BODY, Role.EQUIPMENT))
        assertEquals(emptyList(), problems(figure(0.0)))
        // The upright back pad is an angled slab (a polygon); it reaches above the seated head.
        assertTrue(FigureRenderer.shapes(figure(0.0), Which.START).any { it is Polygon && it.role == Role.EQUIPMENT })
        val back = bench.surfaces(View.SIDE).getValue(Surface.BACK_PAD).single()
        assertTrue(back.cy - AdjustableBench.BACK_LENGTH / 2 < figure(0.0).start.pose.skeleton().head.y)
        val away = contactProblems(figure(4.0)).map { it.message.substringBefore(":") }.toSet()
        assertEquals(setOf("contact UPPER_BACK on BACK_PAD", "contact HEAD on BACK_PAD"), away)
    }

    @Test
    fun powerRack_lifterLeft_mirrorsAboutItsX() {
        fun xs(rack: PowerRack) = rack.draw(View.SIDE, null).map { (it.shape as Capsule).let { c -> listOf(c.x1, c.x2) } }
        val right = PowerRack(x = 100.0, jHookHeight = 140.0, safetyHeight = 80.0)
        val left = right.copy(lifterLeft = true)
        for ((r, l) in xs(right).zip(xs(left))) for ((a, b) in r.zip(l)) assertEquals(100.0 - a, b - 100.0, 1e-9)
        // The safety arm reaches towards the lifter: to the left of the posts.
        assertTrue(xs(left).last().min() < 100.0 - 50.0)
    }

    @Test
    fun flatBench_nearLimbsBeside_checksOnlyTheTorsoHeadAndFarLimbs() {
        // Standing beside the bench: the near leg crosses the pad's outline in the side view.
        val pose = standing(x = 20.0)
        val beside = figureOf(View.SIDE, Scene(pose, listOf(Floor, FlatBench(x = 0.0, nearLimbsBeside = true))))
        val through = figureOf(View.SIDE, Scene(pose.copy(farLeg = Leg(0.0, 0.0)), listOf(Floor, FlatBench(x = 0.0))))
        assertTrue(problems(through).any { "near thigh goes through the bench" in it.message || "near shin goes through the bench" in it.message })
        // Beside: the near leg passes, the far leg (the same line) is still stopped.
        val found = problems(beside).filter { "through" in it.message }
        assertTrue(found.isNotEmpty() && found.all { it.message.startsWith("far ") }, found.toString())
    }

    @Test
    fun nearAndFarKnee_upperChest_points() {
        val pose = standing(farLeg = Leg(90.0, 90.0))
        val s = pose.skeleton()
        assertEquals(s.near.knee, s.point(BodyPoint.NEAR_KNEE))
        assertEquals(s.far.knee, s.point(BodyPoint.FAR_KNEE))
        // The upper chest is above the bench-press touch point, on the front of the torso.
        assertTrue(s.point(BodyPoint.UPPER_CHEST).y < s.point(BodyPoint.CHEST).y)
        assertTrue(s.point(BodyPoint.UPPER_CHEST).x > s.shoulder.x)
    }

    @Test
    fun slab_signedDistance_andRing() {
        val slab = Slab(0.0, 0.0, 10.0, 2.0, 45.0)
        val normal = dir(135.0)
        assertEquals(-1.0, slab.signedDistance(Point(0.0, 0.0)), 1e-9)
        assertEquals(0.0, slab.signedDistance(normal * 1.0), 1e-9)
        assertEquals(2.0, slab.signedDistance(normal * 3.0), 1e-9)
        assertEquals(1.0, slab.signedDistance(dir(45.0) * 6.0), 1e-9)
        // A ring is outer then inner, closed: 2 × 37 points.
        val r = ring(Role.ACCENT, 0.0, 0.0, 5.0, 4.0)
        assertEquals(2 * 37 * 2, r.points.size)
        assertEquals(Bounds(-5.0, -5.0, 5.0, 5.0), r.bounds().let { b -> Bounds(round1(b.minX), round1(b.minY), round1(b.maxX), round1(b.maxY)) })
    }
    @Test
    fun squatBox_holdsTheHips_andStopsTheBody() {
        val pelvis = Point(-30.0, -50.0)
        val legs = Reach.leg(pelvis, Point(0.0, -Body.ANKLE_HEIGHT), torso = 45.0, foot = 0.0)
        val pose = Pose(pelvis.x, pelvis.y, 45.0, nearArm = Arm(0.0, 0.0), nearLeg = legs)
        fun figure(boxHeight: Double) = figureOf(
            View.SIDE, Scene(pose, listOf(Floor, SquatBox(x = pelvis.x - 5.0, height = boxHeight))),
            contacts = listOf(Contact(BodyPoint.HIPS, Surface.BOX, ALL), Contact(BodyPoint.FEET, Surface.FLOOR, ALL)),
        )
        val sitting = figure(-pelvis.y - Body.TORSO_RADIUS)
        assertRenders(sitting, setOf(Role.BODY, Role.EQUIPMENT))
        assertEquals(emptyList(), problems(sitting))
        assertEquals(setOf("contact HIPS on BOX"), contactProblems(figure(30.0)).map { it.message.substringBefore(":") }.toSet())
        assertTrue(problems(figure(48.0)).any { "through the box" in it.message })
    }

    @Test
    fun deficitPlatform_holdsTheFeet_platesOnTheFloorBeside() {
        val pose = standing(lift = 5.0)
        val feet = listOf(Contact(BodyPoint.FEET, Surface.PLATFORM, ALL))
        val on = figureOf(View.SIDE, Scene(pose, listOf(Floor, DeficitPlatform(x = 5.0))), contacts = feet)
        assertRenders(on, setOf(Role.BODY, Role.EQUIPMENT))
        assertEquals(emptyList(), problems(on))
        assertEquals(4, contactProblems(figureOf(View.SIDE, Scene(standing(lift = 9.0), listOf(Floor, DeficitPlatform(x = 5.0))), contacts = feet)).size)
        // Standing on the floor through it is caught.
        assertTrue(problems(figureOf(View.SIDE, Scene(standing(), listOf(Floor, DeficitPlatform(x = 5.0))))).any { "through the platform" in it.message })
    }

    @Test
    fun pullingBlocksAndBenchBoards_drawAtTheirSizes() {
        val blocks = PullingBlocks(x = 10.0, height = 10.0)
        val box = blocks.draw(View.SIDE, null).single().shape as RoundRect
        assertEquals(42.0, box.w, 1e-9) // a little narrower than the 45 cm plates
        assertEquals(-10.0, box.y, 1e-9)
        assertEquals(-10.0, blocks.surfaces(View.SIDE).getValue(Surface.BLOCK).single().let { it.cy - it.thickness / 2 }, 1e-9)

        val boards = BenchBoards(x = 60.0, bottom = -65.0)
        assertEquals(7.6, boards.thickness, 1e-9)
        val drawn = boards.draw(View.SIDE, null).map { it.shape as RoundRect }
        assertEquals(2, drawn.count { it.role == Role.EQUIPMENT }) // two boards, each set off by a gap outline
        assertEquals(2, drawn.count { it.role == Role.GAP })
        val top = boards.surfaces(View.SIDE).getValue(Surface.BOARD).single()
        assertEquals(-65.0 - 7.6, top.cy - top.thickness / 2, 1e-9)
        assertEquals(BenchBoards.LENGTH, top.length, 1e-9)
    }

    @Test
    fun safetyBarTrapBarAndFrontRack_renderAndSitWhereTheyShould() {
        val s = standing().skeleton()
        // The safety bar: yoke behind the neck, plates hanging lower, handles in front of the chest, all in the accent.
        val ssb = SafetySquatBar.draw(s).map { it.shape }
        assertTrue(SafetySquatBar.center(s).x < s.shoulder.x && SafetySquatBar.center(s).y > s.shoulder.y - 5.0)
        assertTrue(safetyBarLoad(s).y > SafetySquatBar.center(s).y + 10.0)
        assertTrue(safetyBarHandle(s).x > s.shoulder.x + 15.0)
        assertTrue(ssb.count { it.role == Role.ACCENT } >= 4 && ssb.any { it.role == Role.EQUIPMENT })
        // The trap bar's frame runs level through the hands, front to back.
        val frame = TrapBar.draw(s).map { it.shape }.filterIsInstance<Capsule>().single()
        assertEquals(TrapBar.FRAME, frame.x2 - frame.x1, 1e-9)
        assertEquals(s.near.grip.y, frame.y1, 1e-9)
        // The front rack: in front of the shoulder joint, above it, below the chin.
        val rack = Barbell(BarHold.FRONT_RACK).center(s)
        assertTrue(rack.x - s.shoulder.x in 5.0..12.0 && rack.y < s.shoulder.y && rack.y > s.chin().y)
        for (prop in listOf(SafetySquatBar, TrapBar, Barbell(BarHold.FRONT_RACK))) {
            assertRenders(figureOf(View.SIDE, Scene(standing(), listOf(Floor, prop))), setOf(Role.BODY, Role.ACCENT, Role.ACCENT_FILL))
        }
    }

    @Test
    fun powerRack_safetyArmSurface_isTheArmsTop() {
        val rack = PowerRack(x = 100.0, jHookHeight = 140.0, safetyHeight = 80.0, armLength = 50.0, lifterLeft = true)
        val arm = rack.surfaces(View.SIDE).getValue(Surface.SAFETY_ARM).single()
        assertEquals(-80.0, arm.cy - arm.thickness / 2, 1e-9)
        assertEquals(50.0, arm.cx - arm.length / 2, 1e-9) // reaching to x - armLength
        assertEquals(114.0, arm.cx + arm.length / 2, 1e-9) // and 14 cm behind the posts
        assertEquals(2, rack.surfaces(View.FRONT).getValue(Surface.SAFETY_ARM).size)
    }
}
