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

    // ---- chest batch (Task 6) ----

    @Test
    fun declineBench_padDownTowardsTheHead_legBraceRoller() {
        val bench = DeclineBench(x = 100.0, brace = Point(140.0, -60.0))
        val pad = bench.surfaces(View.SIDE).getValue(Surface.BENCH).single()
        // The pad's top runs from the high end (68 cm) down 25° towards the head, 100 cm long.
        assertEquals(100.0, pad.length, 1e-9)
        assertEquals(205.0, pad.angle, 1e-9)
        val lowEnd = bench.top + bench.down * bench.length
        assertEquals(68.0 - 100.0 * kotlin.math.sin(25.0 * kotlin.math.PI / 180), -lowEnd.y, 1e-9)
        assertEquals(0.0, pad.signedDistance(bench.top + bench.down * 50.0), 1e-6) // the top line is the pad's surface
        val roller = bench.surfaces(View.SIDE).getValue(Surface.LEG_BRACE).single()
        assertEquals(DeclineBench.ROLLER_RADIUS, roller.radius)
        assertEquals(Point(140.0, -60.0), Point(roller.cx, roller.cy))
        assertTrue(bench.solids(View.SIDE).any { it.label == "leg brace" })
        assertRenders(figureOf(View.SIDE, Scene(standing(x = 200.0), listOf(Floor, bench))), setOf(Role.EQUIPMENT, Role.BODY))
    }

    @Test
    fun smithMachine_railAtTheBar_stopsAtTheirHeight() {
        val smith = SmithMachine(x = 60.0, stopHeight = 63.5)
        val rail = smith.draw(View.SIDE, null).map { it.shape }.filterIsInstance<Capsule>().first()
        assertEquals(60.0, rail.x1, 1e-9)
        assertEquals(-215.0, rail.y2, 1e-9) // a typical 215 cm frame
        val stop = smith.surfaces(View.SIDE).getValue(Surface.SAFETY_ARM).single()
        assertEquals(-63.5, stop.cy - stop.thickness / 2, 1e-9)
        assertEquals(emptyList(), smith.solids(View.SIDE))
        // The bar on the rails passes the check; off them it fails.
        val pose = standing(arm = Arm(90.0, 0.0))
        val grip = pose.skeleton().near.grip
        fun fig(x: Double) = figureOf(View.SIDE, Scene(pose, listOf(Floor, SmithMachine(x, 50.0), Barbell())), checks = listOf(BarOnRails(Which.START)))
        assertEquals(emptyList(), problems(fig(grip.x)).filter { "rails" in it.message })
        assertTrue(problems(fig(grip.x + 5.0)).any { "off the rails" in it.message })
    }

    @Test
    fun cableStack_frontView_twoColumns_cabledToEachHand_handlesInTheAccent() {
        val stack = CableStack(x = 0.0, pulleyHeight = 200.0, reach = 10.0, halfSpan = 105.0)
        val pose = standing(arm = Arm(80.0, 0.0))
        val s = pose.skeleton(View.FRONT)
        val shapes = stack.draw(View.FRONT, null, s).map { it.shape }
        val columns = shapes.filterIsInstance<RoundRect>().filter { it.h > 200.0 }
        assertEquals(listOf(-105.0, 105.0), columns.map { it.x + it.w / 2 }.sorted())
        // Each cable runs from its side's pulley to that side's hand.
        val cables = shapes.filterIsInstance<Capsule>().filter { it.width < 1.0 }
        assertEquals(setOf(s.near.grip, s.far.grip), cables.map { Point(it.x2, it.y2) }.toSet())
        assertEquals(setOf(95.0, -95.0), cables.map { it.x1 }.toSet())
        // The side view is the single column it was (the lat pulldown's), whatever halfSpan says.
        assertEquals(CableStack(x = 0.0).draw(View.SIDE, null), stack.copy(pulleyHeight = 215.0, reach = 45.0).draw(View.SIDE, null))
        val handles = CableHandles.draw(s).map { it.shape }
        assertEquals(2, handles.size)
        assertTrue(handles.all { it.role == Role.ACCENT })
        assertEquals((s.near.grip + s.far.grip) * 0.5, CableHandles.center(s))
        assertEquals(pose.skeleton().near.grip, CableHandles.center(pose.skeleton()))
        assertRenders(figureOf(View.FRONT, Scene(pose, listOf(Floor, stack, CableHandles))), setOf(Role.BODY, Role.EQUIPMENT, Role.ACCENT))
    }

    @Test
    fun pressMachine_seatAndBackPad_leverFromThePivotToTheHand() {
        val m = PressMachine(x = 40.0, seatHeight = 41.0, backAngle = 80.0, pivot = Point(60.0, -170.0))
        val surfaces = m.surfaces(View.SIDE)
        val seat = surfaces.getValue(Surface.SEAT).single()
        assertEquals(-41.0, seat.cy - seat.thickness / 2, 1e-9)
        assertEquals(PressMachine.SEAT_LENGTH, seat.length, 1e-9)
        val back = surfaces.getValue(Surface.BACK_PAD).single()
        assertEquals(PressMachine.BACK_LENGTH, back.length, 1e-9)
        assertEquals(100.0, back.angle, 1e-9)
        val s = standing(x = 20.0, arm = Arm(90.0, 0.0)).skeleton()
        val lever = m.draw(View.SIDE, null, s).map { it.shape }.filterIsInstance<Capsule>().single { it.x1 == 60.0 && it.y1 == -170.0 }
        assertEquals(s.near.grip, Point(lever.x2, lever.y2))
        assertEquals(2, m.solids(View.SIDE).size)
    }

    @Test
    fun pecDeckAndSeat_frontView_hipsOnTheSeat_legsInFrontOfIt() {
        val deck = PecDeck(x = 0.0, seatHeight = 47.0, padTop = 140.0, pivotHeight = 195.0)
        assertEquals(-47.0, deck.surfaces(View.FRONT).getValue(Surface.SEAT).single().let { it.cy - it.thickness / 2 }, 1e-9)
        assertEquals(emptyList(), deck.solids(View.FRONT))
        val s = standing(arm = Arm(90.0, 0.0)).skeleton(View.FRONT)
        val levers = deck.draw(View.FRONT, null, s).map { it.shape }.filterIsInstance<Capsule>().filter { it.y1 == -195.0 && it.width == 4.0 }
        assertEquals(setOf(s.near.grip, s.far.grip), levers.map { Point(it.x2, it.y2) }.toSet())
        assertEquals(setOf(18.0, -18.0), levers.map { it.x1 }.toSet()) // pivots above the shoulders
        val seat = Seat(x = 0.0, height = 47.0)
        assertEquals(40.0, seat.surfaces(View.FRONT).getValue(Surface.SEAT).single().length, 1e-9)
        assertEquals(emptyList(), seat.solids(View.FRONT))
    }

    @Test
    fun dipBarsAndAssistedDip_heldBars_assistStep() {
        val bars = DipBars(x = 0.0)
        val bar = bars.surfaces(View.SIDE).getValue(Surface.DIP_BAR).single()
        assertEquals(-120.0, bar.cy, 1e-9)
        assertEquals(50.0, bar.length, 1e-9)
        assertTrue(Surface.DIP_BAR.grip)
        val machine = AssistedDipMachine(x = -55.0, handleHeight = 120.0, handleEnd = 15.0, stepX = 0.0, stepHeight = 80.0, pivot = Point(-46.0, -60.0))
        val step = machine.surfaces(View.SIDE).getValue(Surface.ASSIST_STEP).single()
        assertEquals(-80.0, step.cy - step.thickness / 2, 1e-9)
        assertEquals(AssistedDipMachine.STEP, step.length, 1e-9)
        assertEquals(70.0, machine.surfaces(View.SIDE).getValue(Surface.DIP_BAR).single().length, 1e-9)
        assertEquals(listOf("step"), machine.solids(View.SIDE).map { it.label })
        for (piece in listOf(bars, machine)) assertRenders(figureOf(View.SIDE, Scene(standing(x = 80.0), listOf(Floor, piece))), setOf(Role.EQUIPMENT))
    }

    @Test
    fun backPlates_lieFlatOnTheUpperBack() {
        val s = Pose(0.0, -40.0, torso = 10.0, nearArm = Arm(90.0, 0.0), nearLeg = Leg(0.0, 0.0)).skeleton()
        val plates = BackPlates(count = 2)
        val drawn = plates.draw(s).map { it.shape }
        assertEquals(2, drawn.count { it.role == Role.ACCENT })
        // Each plate is 45 cm across, lying along the torso, stacked on its back.
        val c = plates.center(s)
        assertEquals(Body.TORSO_RADIUS + BackPlates.THICKNESS, (c - s.onTorso(Segment.TORSO.length * BackPlates.ALONG, 0.0)).length(), 1e-9)
        assertTrue(c.y < s.onTorso(Segment.TORSO.length * BackPlates.ALONG, 0.0).y, "on the back, above the prone torso")
    }

    @Test
    fun dumbbell_neutralGrip_sideView_inProfileAlongTheTorso() {
        // The usual grip is end-on, a disc (unchanged); a neutral grip shows the handle and two heads, along the torso.
        val s = Pose(0.0, -53.0, torso = 160.0, nearArm = Arm(90.0, 0.0), nearLeg = Leg(0.0, 0.0)).skeleton()
        val disc = Dumbbell(Side.NEAR).draw(s).map { it.shape }
        assertTrue(disc.any { it is Circle } && disc.none { it is Polygon && it.role == Role.ACCENT && it.points.size == 8 })
        val profile = Dumbbell(Side.NEAR, neutral = true).draw(s).map { it.shape }
        assertEquals(1, profile.count { it is Capsule })
        val heads = profile.filterIsInstance<Polygon>()
        assertEquals(2, heads.size)
        assertTrue(profile.all { it.role == Role.ACCENT })
        // The handle lies along the torso's line, centred in the hand.
        val handle = profile.filterIsInstance<Capsule>().single()
        val along = Point(handle.x2 - handle.x1, handle.y2 - handle.y1)
        assertEquals(160.0, fold(angleOf(along)), 1e-6)
        assertEquals(s.near.grip, Point((handle.x1 + handle.x2) / 2, (handle.y1 + handle.y2) / 2))
        // The front view draws the profile either way.
        val f = standing(arm = Arm(90.0, 0.0)).skeleton(View.FRONT)
        assertEquals(Dumbbell(Side.NEAR).draw(f), Dumbbell(Side.NEAR, neutral = true).draw(f))
    }

    @Test
    fun cableStack_cableInFront_runsFromThePulleyToTheHand() {
        val pose = standing(arm = Arm(90.0, 0.0))
        val s = pose.skeleton()
        val grip = CableHandles.center(s)
        val stack = CableStack(x = 0.0, pulleyHeight = 12.0, reach = 0.0, height = 75.0, cableInFront = true)
        // Not behind the body, but in front of it, set off by a gap outline, ending at the hand.
        assertTrue(stack.draw(View.SIDE, grip, s).map { it.shape }.none { it is Capsule && it.width < 1.0 })
        val front = stack.drawFront(View.SIDE, grip).map { it.shape as Capsule }
        assertEquals(listOf(Role.GAP, Role.EQUIPMENT), front.map { it.role })
        for (c in front) {
            assertEquals(Point(0.0, -12.0), Point(c.x1, c.y1))
            assertEquals(grip, Point(c.x2, c.y2))
        }
        // The usual stack (the lat pulldown's) is unchanged: its cable behind, nothing in front.
        assertEquals(emptyList(), stack.copy(cableInFront = false).drawFront(View.SIDE, grip))
        assertTrue(stack.copy(cableInFront = false).draw(View.SIDE, grip, s).map { it.shape }.any { it is Capsule && it.width < 1.0 })
    }

    // ---- Task 7: the back batch's kit ----

    @Test
    fun rowStation_seatFootPlateAndLowPulley_cabledToTheAttachment() {
        val station = RowStation(seatX = 14.0, seatHeight = 42.0, plate = Point(85.0, -37.0), pulley = Point(107.0, -41.0))
        val surfaces = station.surfaces(View.SIDE)
        val seat = surfaces.getValue(Surface.SEAT).single()
        assertEquals(-42.0, seat.cy - seat.thickness / 2, 1e-9)
        val plate = surfaces.getValue(Surface.FOOT_PLATE).single()
        assertEquals(75.0, plate.angle, 1e-9)
        // The plate's face (towards the lifter) passes through the given point.
        assertEquals(0.0, plate.signedDistance(Point(85.0, -37.0)), 1e-6)
        val grip = Point(60.0, -85.0)
        val cable = station.draw(View.SIDE, grip).map { it.shape }.filterIsInstance<Capsule>().single { it.width < 1.0 }
        assertEquals(listOf(Point(107.0, -41.0), grip), listOf(Point(cable.x1, cable.y1), Point(cable.x2, cable.y2)))
        assertEquals(2, station.solids(View.SIDE).size)
    }

    @Test
    fun rowMachine_chestPadAndLever_followTheHand() {
        val m = RowMachine(seatX = 14.0, seatHeight = 45.0, padFace = 12.5, padHeight = 125.0, pivot = Point(40.0, -40.0))
        val pad = m.surfaces(View.SIDE).getValue(Surface.CHEST_PAD).single()
        assertEquals(12.5, pad.cx - pad.length / 2, 1e-9)
        assertEquals(-125.0, pad.cy, 1e-9)
        val s = standing(arm = Arm(90.0, 0.0)).skeleton()
        val lever = m.draw(View.SIDE, null, s).map { it.shape }.filterIsInstance<Capsule>().single { it.x1 == 40.0 && it.y1 == -40.0 }
        assertEquals(s.near.grip, Point(lever.x2, lever.y2))
    }

    @Test
    fun proneBench_padRisesTowardsTheHead_optionalPlatform() {
        val b = ProneBench(low = Point(0.0, -62.0), angle = 30.0, length = 65.0)
        val pad = b.surfaces(View.SIDE).getValue(Surface.CHEST_PAD).single()
        assertEquals(30.0, pad.angle, 1e-9)
        // Its top line starts at the low point.
        assertEquals(0.0, pad.signedDistance(Point(0.0, -62.0) + dir(30.0) * 10.0), 1e-6)
        assertEquals(null, b.surfaces(View.SIDE)[Surface.FOOT_PLATE])
        val withPlate = b.copy(footPlate = Point(-60.0, -15.0))
        assertEquals(-15.0, withPlate.surfaces(View.SIDE).getValue(Surface.FOOT_PLATE).single().let { it.cy - it.thickness / 2 }, 1e-9)
        assertRenders(figureOf(View.SIDE, Scene(standing(x = -80.0), listOf(Floor, withPlate))), setOf(Role.EQUIPMENT))
    }

    @Test
    fun tBarLever_fromThePivotThroughTheHandle_platesAlongIt_inTheAccent() {
        val pose = standing(arm = Arm(30.0, 0.0))
        val s = pose.skeleton()
        val lever = TBarLever(pivot = Point(-150.0, -4.0), plateAlong = 140.0)
        val shapes = lever.draw(View.SIDE, null, s).map { it.shape }
        val tube = shapes.filterIsInstance<Capsule>().single { it.width == TBarLever.TUBE }
        assertEquals(Role.ACCENT, tube.role)
        assertEquals(Point(-150.0, -4.0), Point(tube.x1, tube.y1))
        // The plates sit on the lever's line, 140 cm from the pivot.
        val plate = shapes.filterIsInstance<Circle>().single { it.role == Role.ACCENT_FILL }
        val d = (s.near.grip - lever.pivot).let { it * (1.0 / it.length()) }
        assertEquals(lever.pivot + d * 140.0, Point(plate.cx, plate.cy))
        assertEquals(Barbell.PLATE_DIAMETER / 2, plate.r, 1e-9)
        assertEquals(emptyMap(), lever.surfaces(View.SIDE))
    }

    @Test
    fun rackBarAssistMachineAndParallelHandles_holdTheHands_drawnInFront() {
        val rack = RackBar(x = 10.0, height = 95.0)
        assertEquals(-95.0, rack.surfaces(View.SIDE).getValue(Surface.PULL_UP_BAR).single().cy, 1e-9)
        assertEquals(listOf(Role.GAP, Role.EQUIPMENT), rack.drawFront(View.SIDE).map { it.shape.role })
        val m = AssistPullUpMachine(x = 50.0, barX = 0.0, barHeight = 215.0, padX = -10.0, padHeight = 90.0, pivot = Point(41.0, -60.0))
        val pad = m.surfaces(View.SIDE).getValue(Surface.ASSIST_STEP).single()
        assertEquals(-90.0, pad.cy - pad.thickness / 2, 1e-9)
        assertEquals(AssistPullUpMachine.PAD, pad.length, 1e-9)
        assertEquals(listOf("knee pad"), m.solids(View.SIDE).map { it.label })
        assertEquals(2, m.drawFront(View.SIDE).size)
        val handles = ParallelHandles(x = 0.0, height = 200.0)
        val h = handles.surfaces(View.SIDE).getValue(Surface.PULL_UP_BAR).single()
        assertEquals(18.0, h.length, 1e-9)
        // Seen in profile: the handle runs front to back, in front of the hands.
        val front = handles.drawFront(View.SIDE).map { it.shape as Capsule }
        assertEquals(listOf(-9.0, 9.0), front.last().let { listOf(it.x1, it.x2) })
        for (piece in listOf(rack, m, handles)) assertRenders(figureOf(View.SIDE, Scene(standing(x = 0.0), listOf(Floor, piece))), setOf(Role.EQUIPMENT))
    }

    @Test
    fun bandDipBeltAndChestPlate_followTheBody() {
        val pose = standing(arm = Arm(180.0, 0.0))
        val s = pose.skeleton()
        // The band's two strands run from the bar to under the near foot, in front of the body.
        val band = PullUpBand(Point(0.0, -230.0))
        val strands = band.drawFront(View.SIDE, null, s).map { it.shape as Capsule }.filter { it.role == Role.EQUIPMENT }
        assertEquals(2, strands.size)
        for (c in strands) assertTrue(c.y2 > maxOf(s.near.heel.y, s.near.toe.y), "below the sole")
        assertEquals(emptyList(), band.draw(View.SIDE, null, s))
        // The dip belt's plate hangs below the hips: a disc from the side, edge-on from the front.
        val belt = DipBelt(drop = 60.0)
        assertTrue(belt.draw(View.SIDE, null, s).any { it.shape is Circle && (it.shape as Circle).r == 15.0 })
        val f = pose.skeleton(View.FRONT)
        assertTrue(belt.draw(View.FRONT, null, f).any { it.shape is RoundRect && (it.shape as RoundRect).w == 3.0 })
        assertEquals(belt.hook(s) + Point(0.0, 60.0), belt.plateCentre(s))
        assertTrue(belt.hook(s).x > s.pelvis.x, "the chain hangs from the front of the hips")
        // The chest plate lies flat on the front of the chest, edge-on.
        val plate = ChestPlate().draw(View.SIDE, null, s).map { it.shape }.last() as Polygon
        val xs = plate.points.filterIndexed { i, _ -> i % 2 == 0 }
        assertTrue(xs.min() >= s.pelvis.x + Body.CHEST_DEPTH - 0.1, "in front of the chest")
        assertEquals(ChestPlate.THICKNESS, xs.max() - xs.min(), 1e-6)
    }

    @Test
    fun pulldownLeverPulloverMachineHyperBenchAndSpotter_drawAndHold() {
        val s = standing(arm = Arm(150.0, 0.0)).skeleton()
        val lever = PulldownLever(pivot = Point(-40.0, -200.0))
        val shapes = lever.draw(View.SIDE, null, s).map { it.shape }
        // The lever runs from the plate horn behind the pivot to the handle in the hand.
        val arm = shapes.filterIsInstance<Capsule>().single { it.width == 4.0 }
        assertEquals(s.near.grip, Point(arm.x2, arm.y2))
        assertTrue(arm.x1 < -40.0, "horn behind the pivot")
        val m = PulloverMachine(seatX = 20.0, seatHeight = 41.0, backAngle = 85.0, pivot = s.shoulder)
        assertEquals(setOf(Surface.SEAT, Surface.BACK_PAD), m.surfaces(View.SIDE).keys)
        // The lever and elbow pad are outside the near arm: drawn in front of it, from the pivot.
        val front = m.drawFront(View.SIDE, null, s).map { it.shape }
        assertTrue(front.any { it is Capsule && it.role == Role.EQUIPMENT && Point(it.x1, it.y1) == s.shoulder })
        val bench = HyperBench(pad = Point(-10.0, -95.0), footPlate = Point(-70.0, -40.0), roller = Point(-60.0, -55.0))
        assertEquals(setOf(Surface.THIGH_PAD, Surface.FOOT_PLATE, Surface.LEG_BRACE), bench.surfaces(View.SIDE).keys)
        assertEquals(45.0, bench.surfaces(View.SIDE).getValue(Surface.THIGH_PAD).single().angle, 1e-9)
        assertEquals(0.0, bench.surfaces(View.SIDE).getValue(Surface.THIGH_PAD).single().signedDistance(Point(-10.0, -95.0)), 1e-6)
        assertEquals(3, bench.solids(View.SIDE).size)
        val spotter = Spotter(standing(x = -80.0))
        val body = spotter.draw(View.SIDE, null).map { it.shape }
        assertTrue(body.isNotEmpty() && body.all { it.role == Role.BODY_FAR })
        assertEquals(emptyList(), spotter.solids(View.SIDE))
    }

    @Test
    fun cableAttachmentsHandleCamberedBarAndPulloverDumbbell_inTheHands() {
        val s = standing(arm = Arm(90.0, 30.0)).skeleton()
        for (kind in Attachment.entries) {
            val a = CableAttachment(kind)
            assertEquals(s.near.grip, a.center(s))
            assertTrue(a.draw(s).all { it.shape.role == Role.ACCENT })
        }
        // The V-handle's grip is square to the forearm, centred in the hand.
        val v = CableAttachment(Attachment.V_HANDLE).draw(s).first().shape as Capsule
        val f = s.near.wrist - s.near.elbow
        assertEquals(0.0, (v.x2 - v.x1) * f.x + (v.y2 - v.y1) * f.y, 1e-6)
        assertEquals(s.near.grip, Point((v.x1 + v.x2) / 2, (v.y1 + v.y2) / 2))
        val front = standing(arm = Arm(150.0, 0.0)).skeleton(View.FRONT)
        assertEquals((front.near.grip + front.far.grip) * 0.5, CableAttachment(Attachment.STRAIGHT_BAR).center(front))
        assertEquals(s.far.grip, CableHandle(Side.FAR).center(s))
        assertEquals(1, CableHandle(Side.NEAR).draw(front).size)
        val bar = CamberedBar(drop = 12.0)
        val plate = bar.draw(s).map { it.shape }.filterIsInstance<Circle>().single { it.role == Role.ACCENT_FILL }
        assertEquals(s.near.grip + Point(0.0, 12.0), Point(plate.cx, plate.cy))
        val pull = PulloverDumbbell.center(s)
        assertEquals(Dumbbell.HANDLE / 2 + 2.5, (pull - s.near.grip).length(), 1e-6)
        assertEquals(3, PulloverDumbbell.draw(s).size)
        // The front view's end-on dumbbell is a disc; the default front drawing is unchanged.
        val down = standing(arm = Arm(8.0, 0.0)).skeleton(View.FRONT)
        assertTrue(Dumbbell(Side.NEAR, endOnFront = true).draw(down).any { it.shape is Circle })
        assertTrue(Dumbbell(Side.NEAR).draw(down).none { it.shape is Circle })
    }

    @Test
    fun latPulldownSeat_frontView_rollerAcrossTheThighs_drawnInFront() {
        val seat = LatPulldownSeat(x = 0.0, padX = 0.0, seatHeight = 47.0, padHeight = 70.0)
        val pad = seat.surfaces(View.FRONT).getValue(Surface.THIGH_PAD).single()
        assertEquals(2 * LatPulldownSeat.FRONT_HALF, pad.length, 1e-9)
        assertEquals(emptyList(), seat.solids(View.FRONT))
        assertEquals(listOf(Role.GAP, Role.EQUIPMENT), seat.drawFront(View.FRONT).map { it.shape.role })
        // The side view is unchanged: nothing in front.
        assertEquals(emptyList(), seat.drawFront(View.SIDE))
    }

    // ---- Task 8: the shoulder batch's pieces ----

    @Test
    fun kettlebell_handleInTheHand_bellOnTheForearm_rackInFront_overheadBehind() {
        val rack = standing(arm = Arm(15.0, 145.0)).skeleton()
        val rackBell = Kettlebell(Side.NEAR, KettlebellHold.RACK)
        assertEquals(rack.near.grip, rackBell.center(rack))
        val c = rackBell.bellCentre(rack)
        // Racked: by the wrist, just in front of the upright forearm.
        assertTrue(c.x > rack.near.wrist.x && (c - rack.near.wrist).length() < Kettlebell.BELL_RADIUS + 8.0, "rack bell by the wrist, in front")
        val overhead = standing(arm = Arm(180.0, 0.0)).skeleton()
        val up = Kettlebell(Side.NEAR, KettlebellHold.OVERHEAD)
        val o = up.bellCentre(overhead)
        assertTrue(o.x < overhead.near.wrist.x - Body.FOREARM_WIDTH / 2, "overhead bell behind the forearm")
        val shapes = up.draw(overhead).map { it.shape }
        assertTrue(shapes.all { it.role == Role.ACCENT || it.role == Role.ACCENT_FILL })
        val body = shapes.filterIsInstance<Circle>().single { it.role == Role.ACCENT_FILL }
        assertEquals(Kettlebell.BELL_RADIUS, body.r, 1e-9)
        // The handle's two horns meet at the grip.
        val horns = shapes.filterIsInstance<Capsule>()
        assertEquals(2, horns.size)
        assertTrue(horns.all { Point(it.x2, it.y2) == overhead.near.grip })
        assertRenders(figureOf(View.SIDE, Scene(standing(arm = Arm(180.0, 0.0)), listOf(Floor, up))), setOf(Role.BODY, Role.ACCENT, Role.ACCENT_FILL))
    }

    @Test
    fun lateralRaiseMachine_pivotsAtTheShoulders_padsOutsideTheArms_drawnInFront() {
        val pose = standing(arm = Arm(8.0, 0.0))
        val s = pose.skeleton(View.FRONT)
        val m = LateralRaiseMachine(x = 0.0, seatHeight = 47.0, padTop = 130.0, pivotHeight = -s.near.shoulder.y)
        assertEquals(s.near.shoulder.x, m.pivot(Side.NEAR).x, 1e-9)
        assertEquals(s.far.shoulder.x, m.pivot(Side.FAR).x, 1e-9)
        assertEquals(-47.0, m.surfaces(View.FRONT).getValue(Surface.SEAT).single().let { it.cy - it.thickness / 2 }, 1e-9)
        assertEquals(emptyList(), m.solids(View.FRONT))
        // The hanging arm's pad is outside it, by the elbow; on the far side too.
        val (a, b) = m.armPad(Side.NEAR, s)
        assertTrue((a + b).x / 2 > s.near.elbow.x + Body.UPPER_ARM_WIDTH / 2)
        val (fa, fb) = m.armPad(Side.FAR, s)
        assertTrue((fa + fb).x / 2 < s.far.elbow.x - Body.UPPER_ARM_WIDTH / 2)
        val front = m.drawFront(View.FRONT, null, s).map { it.shape }
        assertTrue(front.any { it.role == Role.GAP } && front.any { it.role == Role.EQUIPMENT })
        assertEquals(emptyList(), m.drawFront(View.SIDE, null, pose.skeleton()))
        assertRenders(figureOf(View.FRONT, Scene(pose, listOf(Floor, m))), setOf(Role.BODY, Role.EQUIPMENT))
    }

    @Test
    fun pecDeckFacingThePad_chestPadDrawnInFront_noBackPad_defaultUnchanged() {
        val deck = PecDeck(x = 0.0, seatHeight = 47.0, padTop = 104.0, pivotHeight = 195.0)
        val facing = deck.copy(facingPad = true)
        val s = standing(arm = Arm(90.0, 0.0)).skeleton(View.FRONT)
        assertEquals(emptyList(), deck.drawFront(View.FRONT, null, s))
        val pad = facing.drawFront(View.FRONT, null, s).map { it.shape }.filterIsInstance<RoundRect>().single { it.role == Role.EQUIPMENT }
        assertEquals(-104.0, pad.y, 1e-9)
        assertEquals(36.0, pad.h, 1e-9)
        // No pad behind; the rest of the machine is as before.
        assertEquals(deck.draw(View.FRONT, null, s).size - 1, facing.draw(View.FRONT, null, s).size)
    }

    @Test
    fun cableStack_crossedCables_andAFrontViewSingleColumnCableInFront() {
        val pose = standing(arm = Arm(80.0, 0.0))
        val s = pose.skeleton(View.FRONT)
        val crossed = CableStack(x = 0.0, pulleyHeight = 140.0, reach = 6.0, halfSpan = 30.0, crossed = true)
        val cables = crossed.draw(View.FRONT, null, s).map { it.shape }.filterIsInstance<Capsule>().filter { it.width < 1.0 }
        // The right-hand pulley's cable runs to the far (left) hand, and the other way round.
        assertEquals(s.far.grip, cables.single { it.x1 > 0 }.let { Point(it.x2, it.y2) })
        assertEquals(s.near.grip, cables.single { it.x1 < 0 }.let { Point(it.x2, it.y2) })
        val single = CableStack(x = -60.0, pulleyHeight = 15.0, reach = 12.0, cableInFront = true)
        val grip = s.near.grip
        assertTrue(single.draw(View.FRONT, grip, s).none { (it.shape as? Capsule)?.width == 0.8 }, "no cable behind")
        val front = single.drawFront(View.FRONT, grip, s).map { it.shape as Capsule }
        assertEquals(listOf(Role.GAP, Role.EQUIPMENT), front.map { it.role })
        assertEquals(Point(-48.0, -15.0), Point(front[1].x1, front[1].y1))
        assertEquals(grip, Point(front[1].x2, front[1].y2))
        // The default front single column still draws its cable behind.
        assertEquals(emptyList(), single.copy(cableInFront = false).drawFront(View.FRONT, grip, s))
    }

    @Test
    fun pressMachine_handleEndOn_drawsADisc() {
        val m = PressMachine(x = 40.0, seatHeight = 41.0, backAngle = 80.0, pivot = Point(-60.0, -170.0), handleEndOn = true)
        val s = standing(x = 20.0, arm = Arm(150.0, 20.0)).skeleton()
        val shapes = m.draw(View.SIDE, null, s).map { it.shape }
        assertTrue(shapes.any { it is Circle && Point(it.cx, it.cy) == s.near.grip })
        assertTrue(m.copy(handleEndOn = false).draw(View.SIDE, null, s).map { it.shape }.none { it is Circle && Point(it.cx, it.cy) == s.near.grip })
    }

    // ---- Task 9: the arm batch's pieces ----

    @Test
    fun ezBar_endOnWithSmallerPlates_inBothHands_frontViewZigzag() {
        val s = standing(arm = Arm(8.0, 145.0)).skeleton()
        assertEquals(s.near.grip, EZBar.center(s))
        val side = EZBar.draw(s).map { it.shape }
        val plate = side.filterIsInstance<Circle>().single { it.role == Role.ACCENT_FILL }
        assertEquals(EZBar.PLATE_DIAMETER / 2, plate.r, 1e-9)
        assertTrue(EZBar.PLATE_DIAMETER < Barbell.PLATE_DIAMETER)
        val front = standing(arm = Arm(10.0, 0.0)).skeleton(View.FRONT)
        val shaft = EZBar.draw(front).map { it.shape }.filterIsInstance<Capsule>()
        // The cambered middle: some shaft pieces are not level.
        assertTrue(shaft.any { abs(it.y1 - it.y2) > 1.0 })
        assertRenders(figureOf(View.SIDE, Scene(standing(arm = Arm(8.0, 145.0)), listOf(Floor, EZBar))), setOf(Role.BODY, Role.ACCENT, Role.ACCENT_FILL))
        // Two-handed: the hands apart on the bar is caught.
        val apart = figureOf(View.SIDE, Scene(standing(arm = Arm(8.0, 145.0), farArm = Arm(0.0, 0.0)), listOf(Floor, EZBar)))
        assertTrue(problems(apart).any { it.message.startsWith("held: the hands are") })
    }

    @Test
    fun overheadDumbbell_hangsStraightDownFromTheHands_inProfile() {
        val s = standing(arm = Arm(175.0, 95.0)).skeleton()
        val c = OverheadDumbbell.center(s)
        assertEquals(s.near.grip.x, c.x, 1e-9)
        assertEquals(s.near.grip.y + Dumbbell.HANDLE / 2 + 2.5, c.y, 1e-9)
        val shapes = OverheadDumbbell.draw(s).map { it.shape }
        val handle = shapes.filterIsInstance<Capsule>().single()
        assertEquals(handle.x1, handle.x2, 1e-9)
        assertEquals(2, shapes.filterIsInstance<Polygon>().size)
        assertRenders(figureOf(View.SIDE, Scene(standing(arm = Arm(175.0, 95.0)), listOf(Floor, OverheadDumbbell))), setOf(Role.BODY, Role.ACCENT))
    }

    @Test
    fun dumbbell_squareToForearm_profileCrossesTheHand_defaultUnchanged() {
        val s = standing(arm = Arm(0.0, 0.0)).skeleton()
        val square = Dumbbell(Side.NEAR, neutral = true, squareToForearm = true).draw(s).map { it.shape }.filterIsInstance<Capsule>().single()
        // The hand hangs straight down, so the handle is level.
        assertEquals(square.y1, square.y2, 1e-9)
        // The default neutral profile still runs along the torso (upright: a vertical handle).
        val along = Dumbbell(Side.NEAR, neutral = true).draw(s).map { it.shape }.filterIsInstance<Capsule>().single()
        assertEquals(along.x1, along.x2, 1e-9)
    }

    @Test
    fun preacherBench_seatAndSlopedArmPad_surfacesAndSolids() {
        val b = PreacherBench(seatX = -10.0, seatHeight = 55.0, padTop = Point(0.7, -107.6), padAngle = 45.0)
        val pad = b.surfaces(View.SIDE).getValue(Surface.ARM_PAD).single()
        assertEquals(-45.0, pad.angle, 1e-9)
        assertEquals(PreacherBench.PAD_THICKNESS, pad.thickness, 1e-9)
        // The pad's top face starts at padTop: its upper corner on the face is there.
        assertTrue(pad.corners().any { (it - b.padTop).length() < 1e-6 })
        assertEquals(-55.0, b.surfaces(View.SIDE).getValue(Surface.SEAT).single().let { it.cy - it.thickness / 2 }, 1e-9)
        // Only the seat is an obstacle (the pad is beside the torso).
        assertEquals(listOf("bench"), b.solids(View.SIDE).map { it.label })
        assertRenders(figureOf(View.SIDE, Scene(standing(), listOf(Floor, b))), setOf(Role.BODY, Role.EQUIPMENT))
    }

    @Test
    fun armPadMachine_leverFromThePivotToTheHand_drawnInFront_curlAndExtensionUses() {
        val pose = standing(arm = Arm(50.0, 130.0))
        val s = pose.skeleton()
        for (angle in listOf(40.0, 35.0)) {
            val m = ArmPadMachine(seatX = -12.0, seatHeight = 50.0, padTop = Point(1.0, -100.0), padAngle = angle, pivot = s.near.elbow, stackX = 90.0)
            assertEquals(setOf(Surface.SEAT, Surface.ARM_PAD), m.surfaces(View.SIDE).keys)
            val front = m.drawFront(View.SIDE, null, s).map { it.shape }
            val lever = front.filterIsInstance<Capsule>().single { it.role == Role.EQUIPMENT }
            assertEquals(s.near.elbow, Point(lever.x1, lever.y1))
            assertEquals(s.near.grip, Point(lever.x2, lever.y2))
            assertTrue(front.any { it.role == Role.GAP })
            assertEquals(emptyList(), m.drawFront(View.FRONT, null, pose.skeleton(View.FRONT)))
            assertRenders(figureOf(View.SIDE, Scene(pose, listOf(Floor, m))), setOf(Role.BODY, Role.EQUIPMENT))
        }
    }

    // ---- Task 10: quad kit ----

    @Test
    fun hackSquatMachine_padFollowsTheHips_plateFixed_shoulderPadInFront() {
        val origin = Point(0.0, -25.0)
        val a = HackSquatMachine(origin = origin, pelvis = Point(-70.0, -70.0))
        val b = HackSquatMachine(origin = origin, pelvis = Point(-45.0, -45.0))
        for (m in listOf(a, b)) {
            assertEquals(setOf(Surface.BACK_PAD, Surface.FOOT_PLATE), m.surfaces(View.SIDE).keys)
            // The pad along the rail, its front face a torso's radius behind the hips; the plate's face through the origin.
            val pad = m.surfaces(View.SIDE).getValue(Surface.BACK_PAD).single()
            assertEquals(135.0, pad.angle, 1e-9)
            assertEquals(Body.TORSO_RADIUS, pad.signedDistance(m.pelvis), 1e-6)
            val plate = m.surfaces(View.SIDE).getValue(Surface.FOOT_PLATE).single()
            assertEquals(0.0, plate.signedDistance(origin), 1e-6)
            assertEquals(listOf("machine", "foot plate"), m.solids(View.SIDE).map { it.label })
            val front = m.drawFront(View.SIDE).map { it.shape }
            assertTrue(front.any { it is Circle && it.role == Role.EQUIPMENT && abs(it.cx - m.shoulderPad().x) < 1e-9 })
            assertTrue(front.any { it.role == Role.GAP })
        }
        // Only the pad moves.
        assertEquals(a.surfaces(View.SIDE)[Surface.FOOT_PLATE], b.surfaces(View.SIDE)[Surface.FOOT_PLATE])
        assertRenders(figureOf(View.SIDE, Scene(standing(), listOf(Floor, a)), Scene(standing(), listOf(Floor, b))), setOf(Role.BODY, Role.EQUIPMENT))
    }

    @Test
    fun leverSquatMachine_leverFromThePivot_yokeDrawnInFront_noSurfaces() {
        val m = LeverSquatMachine(pivot = Point(-120.0, -100.0), pad = Point(0.0, -150.0))
        assertEquals(emptyMap(), m.surfaces(View.SIDE))
        assertEquals(emptyList(), m.solids(View.SIDE))
        val lever = m.draw(View.SIDE, null).map { it.shape }.filterIsInstance<Capsule>().single { it.x1 == -120.0 && it.y1 == -100.0 && it.x2 == 0.0 }
        assertEquals(-150.0, lever.y2)
        val yoke = m.drawFront(View.SIDE).map { it.shape }.filterIsInstance<Circle>()
        assertEquals(setOf(Role.GAP, Role.EQUIPMENT), yoke.map { it.role }.toSet())
        assertTrue(yoke.all { it.cx == 0.0 && it.cy == -150.0 })
        assertEquals(emptyList(), m.drawFront(View.FRONT))
        assertRenders(figureOf(View.SIDE, Scene(standing(), listOf(Floor, m))), setOf(Role.BODY, Role.EQUIPMENT))
    }

    @Test
    fun legExtensionMachine_seatBackPadAndRollers_leverDrawnInFront() {
        val pose = Pose(5.0, -62.0, 105.0, nearArm = Arm(0.0, 0.0), nearLeg = Leg(72.0, 90.0))
        val s = pose.skeleton()
        val roller = s.near.ankle + Point(10.0, -5.0)
        val one = LegExtensionMachine(seatFront = 40.0, seatHeight = 52.0, backAngle = 75.0, pivot = s.near.knee, roller = roller, handle = Point(17.0, -56.0))
        val two = one.copy(farRoller = roller + Point(0.0, 20.0))
        assertEquals(setOf(Surface.SEAT, Surface.BACK_PAD, Surface.SHIN_PAD), one.surfaces(View.SIDE).keys)
        assertEquals(1, one.surfaces(View.SIDE).getValue(Surface.SHIN_PAD).size)
        assertEquals(2, two.surfaces(View.SIDE).getValue(Surface.SHIN_PAD).size)
        assertEquals(-52.0, one.surfaces(View.SIDE).getValue(Surface.SEAT).single().let { it.cy - it.thickness / 2 }, 1e-9)
        val lever = one.drawFront(View.SIDE).map { it.shape }.filterIsInstance<Capsule>().single { it.role == Role.EQUIPMENT }
        assertEquals(s.near.knee, Point(lever.x1, lever.y1))
        assertEquals(roller, Point(lever.x2, lever.y2))
        // The far roller is drawn behind the body, with the machine.
        assertTrue(two.draw(View.SIDE, null).map { it.shape }.any { it is Circle && it.cy == roller.y + 20.0 })
        assertRenders(figureOf(View.SIDE, Scene(pose, listOf(Floor, two))), setOf(Role.BODY, Role.EQUIPMENT))
    }

    @Test
    fun pushSled_handlePostHeld_platesInTheAccent() {
        val sled = PushSled(x = 40.0)
        val post = sled.surfaces(View.SIDE).getValue(Surface.SLED_HANDLE).single()
        assertTrue(Surface.SLED_HANDLE.grip)
        assertEquals(-sled.handleTop, post.cy - post.thickness / 2, 1e-9)
        assertEquals(emptyList(), sled.solids(View.SIDE))
        val roles = sled.draw(View.SIDE, null).map { it.shape.role }.toSet()
        assertTrue(roles.containsAll(setOf(Role.EQUIPMENT, Role.ACCENT, Role.ACCENT_FILL)))
        // A grip on the post holds; one 10 cm off it does not.
        val reach = Point(40.0, -110.0)
        val hands = listOf(Contact(BodyPoint.HANDS, Surface.SLED_HANDLE, ALL))
        fun at(x: Double): Pose {
            val base = standing()
            val sh = base.skeleton().shoulder
            val arm = Reach.arm(sh, Point(x, reach.y), 90.0)
            return base.copy(nearArm = arm, farArm = arm)
        }
        assertEquals(emptyList(), contactProblems(figureOf(View.SIDE, Scene(at(40.0), listOf(Floor, sled)), contacts = hands)))
        assertTrue(contactProblems(figureOf(View.SIDE, Scene(at(30.0), listOf(Floor, sled)), contacts = hands)).isNotEmpty())
    }

    @Test
    fun barbellInTheCrook_centredInsideTheElbowBend_otherHoldsUnchanged() {
        val pose = standing(arm = Arm(shoulder = 25.0, elbow = 140.0))
        val s = pose.skeleton()
        val c = Barbell(BarHold.CROOK).center(s)
        // Between the upper arm and the forearm, a forearm's half width and the shaft (plus 1 cm) from the elbow.
        assertEquals(Body.FOREARM_WIDTH / 2 + Barbell.SHAFT_RADIUS + 1.0, (c - s.near.elbow).length(), 1e-9)
        assertTrue(c.y < s.near.elbow.y, "above the elbow, inside the bend")
        assertEquals(s.near.grip, Barbell(BarHold.HANDS).center(s))
        // Not a two-handed grip: the hands may be crossed in front, apart.
        assertEquals(emptyList(), problems(figureOf(View.SIDE, Scene(pose, listOf(Floor, Barbell(BarHold.CROOK))))).filter { it.message.startsWith("held") })
    }

    @Test
    fun feetOffFloor_passesInTheAir_failsOnTheFloor() {
        val ground = figureOf(View.SIDE, Scene(standing(), listOf(Floor)), checks = listOf(FeetOffFloor(10.0, Which.START)))
        assertTrue(problems(ground).any { it.message.startsWith("feet ") && it.message.contains("cm off the floor, needs 10") }, problems(ground).toString())
        val air = figureOf(View.SIDE, Scene(standing(lift = 15.0), listOf(Floor)), checks = listOf(FeetOffFloor(10.0, Which.START)))
        assertEquals(emptyList(), problems(air).filter { it.message.startsWith("feet") })
        val low = figureOf(View.SIDE, Scene(standing(lift = 5.0), listOf(Floor)), checks = listOf(FeetOffFloor(10.0, Which.START)))
        assertTrue(problems(low).any { it.message.startsWith("feet 5") })
    }

    // ---- Task 11: the hamstring and glute batch's pieces ----

    @Test
    fun miniBand_stripBetweenTheLegsAboveTheKneesOrAnkles_drawnInFront() {
        val pose = Pose(0.0, FigureFixtures.STANDING_PELVIS_Y, 90.0, nearArm = Arm(10.0, 0.0), nearLeg = Leg(10.0, 0.0))
        val s = pose.skeleton(View.FRONT)
        for (at in BandAt.entries) {
            val band = MiniBand(at)
            assertEquals(emptyMap(), band.surfaces(View.FRONT))
            assertEquals(emptyList(), band.solids(View.FRONT))
            assertEquals(emptyList(), band.draw(View.FRONT, null, s))
            val strip = band.drawFront(View.FRONT, null, s).map { it.shape }.filterIsInstance<Capsule>()
            assertEquals(listOf(Role.GAP, Role.EQUIPMENT), strip.map { it.role })
            val (a, b) = band.ends(s)
            assertEquals(Point(strip[1].x1, strip[1].y1), a)
            assertEquals(Point(strip[1].x2, strip[1].y2), b)
            val above = if (at == BandAt.KNEES) (s.near.knee - a).length() else (s.near.ankle - a).length()
            assertEquals(if (at == BandAt.KNEES) MiniBand.ABOVE_KNEE else MiniBand.ABOVE_ANKLE, above, 1e-9)
        }
        assertRenders(figureOf(View.FRONT, Scene(pose, listOf(Floor, MiniBand()))), setOf(Role.BODY, Role.EQUIPMENT, Role.GAP))
    }

    @Test
    fun stabilityBall_65cmOnTheFloor_itsTopASurface_andSolid() {
        val ball = StabilityBall(x = 40.0)
        assertEquals(65.0, ball.diameter)
        val top = ball.surfaces(View.SIDE).getValue(Surface.BALL).single()
        assertEquals(0.0, top.signedDistance(Point(40.0, -65.0)), 1e-9)
        assertEquals(0.0, top.signedDistance(Point(40.0, 0.0)), 1e-9)
        assertEquals(listOf("ball"), ball.solids(View.SIDE).map { it.label })
        assertTrue(ball.draw(View.SIDE, null).any { it.shape is Circle && it.shape.role == Role.EQUIPMENT })
        assertRenders(figureOf(View.SIDE, Scene(standing(), listOf(Floor, ball))), setOf(Role.BODY, Role.EQUIPMENT))
    }

    @Test
    fun ankleStrap_onTheLowerShin_theCableRunsToIt() {
        val pose = standing(farLeg = Leg(0.0, 90.0))
        val s = pose.skeleton()
        val strap = AnkleStrap(Side.NEAR)
        val c = strap.center(s)
        assertEquals(AnkleStrap.ABOVE_ANKLE, (c - s.near.ankle).length(), 1e-9)
        assertTrue(strap.draw(s).all { it.shape.role == Role.ACCENT })
        // A cable stack with its cable in front runs it from the pulley to the cuff.
        val stack = CableStack(x = 60.0, pulleyHeight = 12.0, reach = -14.0, cableInFront = true)
        val world = FigureRenderer.world(Scene(pose, listOf(Floor, stack, strap)), View.SIDE).map { it.shape }
        assertTrue(world.any { it is Capsule && it.role == Role.EQUIPMENT && it.x1 == 46.0 && it.y1 == -12.0 && Point(it.x2, it.y2) == c })
        assertRenders(figureOf(View.SIDE, Scene(pose, listOf(Floor, stack, strap))), setOf(Role.BODY, Role.EQUIPMENT, Role.ACCENT))
    }

    @Test
    fun nordicStation_kneePadAndAnkleRoller() {
        val st = NordicStation(padFrom = -37.0, padTo = 14.0, padHeight = 6.0, roller = Point(-35.0, -24.0))
        val pad = st.surfaces(View.SIDE).getValue(Surface.KNEE_PAD).single()
        assertEquals(-6.0, pad.cy - pad.thickness / 2, 1e-9)
        assertEquals(51.0, pad.length, 1e-9)
        assertEquals(0.0, st.surfaces(View.SIDE).getValue(Surface.LEG_BRACE).single().signedDistance(Point(-35.0, -24.0 + NordicStation.ROLLER_RADIUS)), 1e-9)
        assertEquals(listOf("pad"), st.solids(View.SIDE).map { it.label })
        assertRenders(figureOf(View.SIDE, Scene(standing(), listOf(Floor, st))), setOf(Role.BODY, Role.EQUIPMENT))
    }

    @Test
    fun reverseHyperBench_highPad_handlesUnderTheHeadEnd_postsClearOfTheEdge() {
        val b = ReverseHyperBench(edge = 8.0, height = 95.0, handle = Point(68.0, -48.0))
        val pad = b.surfaces(View.SIDE).getValue(Surface.BENCH).single()
        assertEquals(-95.0, pad.cy - pad.thickness / 2, 1e-9)
        assertEquals(8.0, pad.cx - pad.length / 2, 1e-9)
        assertEquals(0.0, b.surfaces(View.SIDE).getValue(Surface.HANDLE).single().signedDistance(Point(68.0, -48.0)).coerceAtLeast(0.0))
        assertTrue(Surface.HANDLE.grip)
        // The pad and two posts; the first post well inside the edge, clear of the hanging feet.
        val solids = b.solids(View.SIDE)
        assertEquals(3, solids.size)
        assertTrue(solids.drop(1).all { it.slab.cx - 8.0 >= 20.0 })
        assertRenders(figureOf(View.SIDE, Scene(standing(), listOf(Floor, b))), setOf(Role.BODY, Role.EQUIPMENT))
    }

    @Test
    fun legCurlMachines_padsSurfaces_leverFromThePivotToTheRoller_drawnInFront() {
        val pivot = Point(-40.0, -67.0)
        val roller = Point(-75.0, -78.0)
        val lying = LyingLegCurlMachine(kneeEnd = -36.0, height = 60.0, pivot = pivot, roller = roller, handle = Point(80.0, -42.0))
        assertEquals(setOf(Surface.BENCH, Surface.SHIN_PAD, Surface.HANDLE), lying.surfaces(View.SIDE).keys)
        assertEquals(-60.0, lying.surfaces(View.SIDE).getValue(Surface.BENCH).single().let { it.cy - it.thickness / 2 }, 1e-9)
        val seated = SeatedLegCurlMachine(seatFront = 30.0, seatHeight = 50.0, backAngle = 80.0, pivot = pivot, roller = roller, lap = Point(-30.0, -80.0), handle = Point(14.0, -60.0))
        assertEquals(setOf(Surface.SEAT, Surface.BACK_PAD, Surface.SHIN_PAD, Surface.THIGH_PAD, Surface.HANDLE), seated.surfaces(View.SIDE).keys)
        val standingM = StandingLegCurlMachine(postX = 40.0, thighPad = Point(20.0, -60.0), pivot = pivot, roller = roller, handle = Point(30.0, -110.0))
        assertEquals(setOf(Surface.THIGH_PAD, Surface.SHIN_PAD, Surface.HANDLE), standingM.surfaces(View.SIDE).keys)
        assertEquals(emptyList(), standingM.solids(View.SIDE))
        for (m in listOf<FixedProp>(lying, seated, standingM)) {
            val front = m.drawFront(View.SIDE).map { it.shape }
            val lever = front.filterIsInstance<Capsule>().first { it.role == Role.EQUIPMENT }
            assertEquals(pivot, Point(lever.x1, lever.y1))
            assertEquals(roller, Point(lever.x2, lever.y2))
            assertTrue(front.any { it is Circle && it.role == Role.EQUIPMENT && it.cx == roller.x && it.cy == roller.y })
            assertTrue(front.any { it.role == Role.GAP })
            assertEquals(emptyList(), m.drawFront(View.FRONT))
            assertRenders(figureOf(View.SIDE, Scene(standing(), listOf(Floor, m))), setOf(Role.BODY, Role.EQUIPMENT))
        }
        // The seated machine's lap pad is drawn in front too.
        assertTrue(seated.drawFront(View.SIDE).map { it.shape }.any { it is Circle && it.cx == -30.0 && it.cy == -80.0 })
    }

    @Test
    fun hipThrustMachine_backPadPlatformAndHipPad_padDrawnInFront() {
        val m = HipThrustMachine(benchEdge = 0.0, benchHeight = 43.0, platform = Point(95.0, -5.0), pivot = Point(105.0, -40.0), pad = Point(45.0, -70.0))
        assertEquals(setOf(Surface.BENCH, Surface.FOOT_PLATE, Surface.HIP_PAD), m.surfaces(View.SIDE).keys)
        assertEquals(-43.0, m.surfaces(View.SIDE).getValue(Surface.BENCH).single().let { it.cy - it.thickness / 2 }, 1e-9)
        assertEquals(-5.0, m.surfaces(View.SIDE).getValue(Surface.FOOT_PLATE).single().let { it.cy - it.thickness / 2 }, 1e-9)
        assertEquals(listOf("bench", "platform"), m.solids(View.SIDE).map { it.label })
        // The lever from the pivot to the pad behind the body; the pad itself in front of the hips.
        assertTrue(m.draw(View.SIDE, null).map { it.shape }.any { it is Capsule && it.x1 == 105.0 && it.y1 == -40.0 && it.x2 == 45.0 && it.y2 == -70.0 })
        val front = m.drawFront(View.SIDE).map { it.shape }.filterIsInstance<Circle>()
        assertEquals(setOf(Role.GAP, Role.EQUIPMENT), front.map { it.role }.toSet())
        assertRenders(figureOf(View.SIDE, Scene(standing(), listOf(Floor, m))), setOf(Role.BODY, Role.EQUIPMENT))
    }

    @Test
    fun hipMachine_frontView_kneePadsOutsideOrInside_drawnInFront() {
        val leg = Leg(hip = 40.0, knee = 40.0, out = 60.0)
        val pose = Pose(0.0, -60.0, 90.0, nearArm = Arm(12.0, 0.0), nearLeg = leg)
        val s = pose.skeleton(View.FRONT)
        for (outside in listOf(true, false)) {
            val m = HipMachine(x = 0.0, seatHeight = 50.0, padTop = 128.0, padsOutside = outside)
            assertEquals(setOf(Surface.SEAT), m.surfaces(View.FRONT).keys)
            assertEquals(emptyList(), m.solids(View.FRONT))
            for (side in Side.entries) {
                val k = s.side(side).knee
                val (a, b) = m.kneePad(side, s)
                assertEquals(a.x, b.x)
                // Outside: further from the middle than the knee; inside: nearer.
                val further = abs(a.x) > abs(k.x)
                assertEquals(outside, further, "pads outside $outside, $side")
                assertEquals(Body.SHIN_WIDTH / 2 + HipMachine.KNEE_PAD_RADIUS, abs(a.x - k.x), 1e-9)
            }
            val front = m.drawFront(View.FRONT, null, s).map { it.shape }
            assertEquals(4, front.size)
            assertEquals(emptyList(), m.drawFront(View.SIDE, null, s))
            assertRenders(figureOf(View.FRONT, Scene(pose, listOf(Floor, m))), setOf(Role.BODY, Role.EQUIPMENT, Role.GAP))
        }
    }

    @Test
    fun kettlebellSwing_bellPastTheHandInLineWithTheForearm_otherHoldsUnchanged() {
        val pose = standing(arm = Arm(shoulder = 70.0, elbow = 0.0))
        val s = pose.skeleton()
        val c = Kettlebell(Side.NEAR, KettlebellHold.SWING).bellCentre(s)
        val f = (s.near.wrist - s.near.elbow).let { it * (1.0 / it.length()) }
        assertEquals(s.near.grip + f * SWING_DROP, c)
        assertEquals(s.near.grip, Kettlebell(Side.NEAR, KettlebellHold.SWING).center(s))
        // The rack hold's bell is not on the forearm's line (the old holds are as they were).
        val rack = Kettlebell(Side.NEAR, KettlebellHold.RACK).bellCentre(s)
        assertTrue((rack - (s.near.grip + f * SWING_DROP)).length() > 5.0)
        assertRenders(figureOf(View.SIDE, Scene(pose, listOf(Floor, Kettlebell(Side.NEAR, KettlebellHold.SWING)))), setOf(Role.BODY, Role.ACCENT, Role.ACCENT_FILL))
    }

    // ---- Task 12: the calf and core kit ----

    @Test
    fun standingCalfMachine_padAndHandle_drawnInFront_handleIsAGrip() {
        val m = StandingCalfMachine(columnX = -45.0, pivot = Point(-45.0, -150.0), pad = Point(-2.0, -155.0), handle = Point(18.0, -135.0))
        assertEquals(setOf(Surface.HANDLE), m.surfaces(View.SIDE).keys)
        assertEquals(0.0, m.surfaces(View.SIDE).getValue(Surface.HANDLE).single().signedDistance(Point(18.0, -135.0)).coerceAtLeast(0.0))
        assertEquals(emptyList(), m.solids(View.SIDE))
        // The lever from the pivot to the pad behind the body; the pad and the handle's grip in front, with gap rings.
        assertTrue(m.draw(View.SIDE, null).map { it.shape }.any { it is Capsule && Point(it.x1, it.y1) == m.pivot && Point(it.x2, it.y2) == m.pad })
        val front = m.drawFront(View.SIDE).map { it.shape }.filterIsInstance<Circle>()
        assertTrue(front.any { it.role == Role.EQUIPMENT && it.cx == -2.0 && it.r == StandingCalfMachine.PAD_RADIUS })
        assertTrue(front.any { it.role == Role.GAP })
        assertEquals(emptyList(), m.drawFront(View.FRONT))
        assertRenders(figureOf(View.SIDE, Scene(standing(), listOf(Floor, m))), setOf(Role.BODY, Role.EQUIPMENT, Role.GAP))
    }

    @Test
    fun seatedCalfMachine_seatAndKneePad_leverDrawnInFront() {
        val m = SeatedCalfMachine(seatX = -40.0, seatHeight = 50.0, pivot = Point(50.0, -62.0), pad = Point(0.0, -70.0))
        assertEquals(setOf(Surface.SEAT, Surface.THIGH_PAD), m.surfaces(View.SIDE).keys)
        assertEquals(-50.0, m.surfaces(View.SIDE).getValue(Surface.SEAT).single().let { it.cy - it.thickness / 2 }, 1e-9)
        assertEquals(0.0, m.surfaces(View.SIDE).getValue(Surface.THIGH_PAD).single().signedDistance(Point(0.0, -70.0 - SeatedCalfMachine.PAD_RADIUS)), 1e-9)
        assertEquals(listOf("seat"), m.solids(View.SIDE).map { it.label })
        val lever = m.drawFront(View.SIDE).map { it.shape }.filterIsInstance<Capsule>().first { it.role == Role.EQUIPMENT }
        assertEquals(m.pivot, Point(lever.x1, lever.y1))
        assertEquals(m.pad, Point(lever.x2, lever.y2))
        assertRenders(figureOf(View.SIDE, Scene(standing(), listOf(Floor, m))), setOf(Role.BODY, Role.EQUIPMENT, Role.GAP))
    }

    @Test
    fun donkeyCalfMachine_forearmSupportAndHipPad_drawnInFront() {
        val m = DonkeyCalfMachine(columnX = 80.0, supportHeight = 100.0, supportLength = 40.0, pivot = Point(80.0, -120.0), pad = Point(0.0, -110.0))
        assertEquals(setOf(Surface.ARM_PAD, Surface.HIP_PAD), m.surfaces(View.SIDE).keys)
        val support = m.surfaces(View.SIDE).getValue(Surface.ARM_PAD).single()
        assertEquals(-100.0, support.cy - support.thickness / 2, 1e-9)
        assertEquals(40.0, support.length, 1e-9)
        assertEquals(80.0, support.cx + support.length / 2, 1e-9)
        assertEquals(emptyList(), m.solids(View.SIDE))
        assertTrue(m.drawFront(View.SIDE).any { it.shape is RoundRect && it.shape.role == Role.EQUIPMENT })
        assertTrue(m.drawFront(View.SIDE).any { it.shape is Circle && it.shape.role == Role.EQUIPMENT })
        assertRenders(figureOf(View.SIDE, Scene(standing(), listOf(Floor, m))), setOf(Role.BODY, Role.EQUIPMENT, Role.GAP))
    }

    @Test
    fun crunchMachine_seatHipPadAndChestPad_padDrawnInFront() {
        val m = CrunchMachine(seatFront = 25.0, seatHeight = 45.0, hipPadX = -10.0, feet = Point(30.0, -6.0), pivot = Point(-30.0, -100.0), pad = Point(20.0, -120.0))
        assertEquals(setOf(Surface.SEAT, Surface.HIP_PAD, Surface.CHEST_PAD), m.surfaces(View.SIDE).keys)
        val hip = m.surfaces(View.SIDE).getValue(Surface.HIP_PAD).single()
        assertEquals(-10.0, hip.cx + hip.length / 2, 1e-9)
        assertEquals(-45.0, hip.cy + hip.thickness / 2, 1e-9)
        assertEquals(listOf("seat", "hip pad"), m.solids(View.SIDE).map { it.label })
        assertTrue(m.draw(View.SIDE, null).map { it.shape }.any { it is Capsule && Point(it.x1, it.y1) == m.pivot && Point(it.x2, it.y2) == m.pad })
        assertEquals(setOf(Role.GAP, Role.EQUIPMENT), m.drawFront(View.SIDE).map { it.shape.role }.toSet())
        assertRenders(figureOf(View.SIDE, Scene(standing(), listOf(Floor, m))), setOf(Role.BODY, Role.EQUIPMENT))
    }

    @Test
    fun supportPost_aHeldGripAtItsHeight() {
        val post = SupportPost(x = 40.0, gripHeight = 110.0)
        val grip = post.surfaces(View.SIDE).getValue(Surface.HANDLE).single()
        assertEquals(0.0, grip.signedDistance(Point(34.0, -110.0)).coerceAtLeast(0.0))
        assertEquals(emptyList(), post.solids(View.SIDE))
        assertTrue(post.drawFront(View.SIDE).any { it.shape.role == Role.GAP })
        assertRenders(figureOf(View.SIDE, Scene(standing(), listOf(Floor, post))), setOf(Role.BODY, Role.EQUIPMENT))
    }

    @Test
    fun abWheel_inBothHands_atTheGrip_twoHanded() {
        val pose = standing(arm = Arm(shoulder = 60.0, elbow = 0.0))
        val s = pose.skeleton()
        assertEquals(s.near.grip, AbWheel.center(s))
        assertTrue(AbWheel.draw(s).any { it.shape is Circle && it.shape.r == AbWheel.RADIUS })
        assertRenders(figureOf(View.SIDE, Scene(pose, listOf(Floor, AbWheel))), setOf(Role.BODY, Role.ACCENT, Role.ACCENT_FILL))
        // Two-handed: hands apart on it is a problem.
        val apart = figureOf(View.SIDE, Scene(pose.copy(farArm = Arm(0.0, 0.0)), listOf(Floor, AbWheel)))
        assertTrue(problems(apart).any { it.message.startsWith("held: the hands are") })
    }

    @Test
    fun pinchPlate_rimInTheHand_faceFromTheSide_edgeFromTheFront() {
        val pose = standing()
        val s = pose.skeleton()
        val plate = PinchPlate(Side.NEAR)
        assertEquals(s.near.grip + Point(0.0, PinchPlate.RADIUS - PinchPlate.PINCH), plate.center(s))
        assertTrue(plate.draw(s).any { it.shape is Circle && it.shape.r == PinchPlate.RADIUS })
        val front = pose.skeleton(View.FRONT)
        assertTrue(plate.draw(front).single().shape is RoundRect)
        assertRenders(figureOf(View.SIDE, Scene(pose, listOf(Floor, plate, PinchPlate(Side.FAR)))), setOf(Role.BODY, Role.ACCENT, Role.ACCENT_FILL))
    }
}
