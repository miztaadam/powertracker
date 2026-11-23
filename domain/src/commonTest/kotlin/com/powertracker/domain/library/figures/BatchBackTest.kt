package com.powertracker.domain.library.figures

import com.powertracker.domain.library.BodyArea
import com.powertracker.domain.library.Library
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The back batch (Task 7): every back exercise, each with the key positions its guide gives. */
class BatchBackTest {
    private fun figure(name: String): ExerciseFigure = assertNotNull(Figures.all[name], "no figure for $name")

    private fun s(name: String, which: Which) = figure(name).let { it.scene(which).pose.skeleton(it.view) }

    private fun pose(name: String, which: Which) = figure(name).scene(which).pose

    private fun held(name: String, which: Which): Point =
        figure(name).scene(which).props.filterIsInstance<HeldProp>().first().center(s(name, which))

    private fun props(name: String, which: Which = Which.START) = figure(name).scene(which).props

    private fun assertContact(name: String, point: BodyPoint, surface: Surface) =
        assertTrue(figure(name).contacts.any { it.point == point && it.surface == surface && it.at == Which.entries.toSet() }, "$name: $point on $surface")

    private fun assertCheck(name: String, check: KeyCheck) = assertTrue(check in figure(name).checks, "$name: $check in ${figure(name).checks}")

    private fun inRange(name: String, label: String, value: Double, range: ClosedFloatingPointRange<Double>) =
        assertTrue(value in range, "$name: $label ${round1(value)} not in $range")

    private fun bend(name: String, which: Which, side: Side = Side.NEAR) =
        pose(name, which).let { JointLimits.elbowBend(if (side == Side.NEAR) it.nearArm else it.farArm) }

    /** The world angle of [v], 0 = level towards +x, 90 = straight up. */
    private fun angle(v: Point) = atan2(-v.y, v.x) * 180 / PI

    /** How far the near upper arm points off the torso's line towards the hips, in the drawn plane. */
    private fun upperArmOffTorso(name: String, which: Which) =
        s(name, which).let { abs(fold(angle(it.near.elbow - it.near.shoulder) - (it.torso + 180.0))) }

    private fun straight(name: String, which: Which = Which.START) = inRange(name, "arm straight", bend(name, which), 0.0..1.0)

    @Test
    fun back_everyExerciseHasAFigure_thatPassesItsChecks() {
        val area = Library.all().filter { it.area == BodyArea.BACK }.map { it.name }
        assertEquals(33, area.size)
        val problems = area.flatMap { name -> Figures.all[name]?.let { FigureChecks.check(name, it) } ?: listOf(FigureProblem(name, null, "no figure")) }
        assertEquals(emptyList(), problems.map { "${it.name} ${it.which}: ${it.message}" })
        assertEquals("Arms straight", figure("Wide-grip cable row").startCaption)
        val front = setOf("Underhand pulldown", "One-arm pulldown", "Chin-up", "Barbell shrug", "DB shrug")
        for (name in area) assertEquals(if (name in front) View.FRONT else View.SIDE, figure(name).view, name)
    }

    // ---- rows: bent over ----

    @Test
    fun row_closeToLevel_barOverTheFeet_thenToTheUpperWaist() {
        val n = "Row"
        val st = s(n, Which.START)
        inRange(n, "torso above level", st.torso, 15.0..25.0)
        inRange(n, "knees slightly bent", pose(n, Which.START).nearLeg.knee, 15.0..35.0)
        straight(n)
        val mid = (st.near.heel.x + st.near.toe.x) / 2
        inRange(n, "bar over the mid-foot", abs(held(n, Which.START).x - mid), 0.0..5.0)
        inRange(n, "bar clear in front of the shins", FigureChecks.barToLegs(held(n, Which.START), st), 0.0..3.0)
        assertCheck(n, BarTouches(BodyPoint.UPPER_ABDOMEN, Which.END))
        // The bar goes straight up; the elbow rises above the back.
        inRange(n, "bar path upright", abs(held(n, Which.END).x - held(n, Which.START).x), 0.0..4.0)
        val e = s(n, Which.END)
        assertTrue(e.near.elbow.y < e.shoulder.y + 5.0, "$n: elbow up beside the back")
        assertEquals(st.torso, e.torso)
    }

    @Test
    fun tBarRow_leverFromTheFloorBehind_handleOnItsArc_toTheTorso() {
        val n = "T-bar row"
        val lever = props(n).filterIsInstance<TBarLever>().single()
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        assertTrue(lever.pivot.x < st.near.heel.x - 50.0 && lever.pivot.y > -6.0, "$n: pivot on the floor behind")
        straight(n)
        inRange(n, "hanging straight down", abs(st.near.grip.x - st.shoulder.x), 0.0..1.0)
        // The handle keeps its distance from the pivot (one lever), and ends touching the torso.
        inRange(n, "lever length kept", abs((st.near.grip - lever.pivot).length() - (en.near.grip - lever.pivot).length()), 0.0..0.5)
        inRange(n, "handle at the torso", distanceToTorso(en, en.near.grip), Body.CHEST_DEPTH - 2.0..Body.CHEST_DEPTH + 2.0)
        inRange(n, "torso down", st.torso, 20.0..35.0)
        assertTrue(lever.plateAlong > (st.near.grip - lever.pivot).length(), "$n: plates past the handle")
    }

    private fun distanceToTorso(s: Skeleton, p: Point): Double {
        val ab = s.shoulder - s.pelvis
        val t = (((p.x - s.pelvis.x) * ab.x + (p.y - s.pelvis.y) * ab.y) / (ab.x * ab.x + ab.y * ab.y)).coerceIn(0.0, 1.0)
        return (p - (s.pelvis + ab * t)).length()
    }

    // ---- rows: seated on a cable ----

    private fun assertSeatedRow(n: String) {
        assertContact(n, BodyPoint.HIPS, Surface.SEAT)
        assertContact(n, BodyPoint.FEET, Surface.FOOT_PLATE)
        val st = s(n, Which.START)
        assertEquals(90.0, st.torso, "$n: torso upright")
        assertEquals(90.0, s(n, Which.END).torso, "$n: torso upright at the end")
        inRange(n, "knees slightly bent", pose(n, Which.START).nearLeg.knee, 10.0..40.0)
        straight(n)
        // At the stretch the hands are in front, a little below the shoulders, towards the pulley.
        inRange(n, "reach forward", st.near.grip.x - st.shoulder.x, 55.0..68.0)
        inRange(n, "a little below the shoulders", st.near.grip.y - st.shoulder.y, 5.0..25.0)
        val station = props(n).filterIsInstance<RowStation>().single()
        assertTrue(station.pulley.x > st.near.toe.x, "$n: pulley beyond the feet")
    }

    @Test
    fun seatedCableRow_vHandleToTheWaist_elbowsCloseToTheRibs() {
        val n = "Seated cable row"
        assertSeatedRow(n)
        assertTrue(CableAttachment(Attachment.V_HANDLE) in props(n))
        assertCheck(n, HeldBeside(BodyPoint.WAIST, 2.0, Which.END))
        val e = s(n, Which.END)
        inRange(n, "elbow behind the torso", e.pelvis.x - e.near.elbow.x, 0.0..25.0)
        inRange(n, "upper arm down and back", upperArmOffTorso(n, Which.END), 20.0..50.0)
        assertEquals(0.0, pose(n, Which.END).nearArm.out, "$n: elbows in, close to the ribs")
    }

    @Test
    fun wideGripCableRow_straightBar_elbowsOut_toTheWaist() {
        val n = "Wide-grip cable row"
        assertSeatedRow(n)
        assertTrue(CableAttachment(Attachment.STRAIGHT_BAR) in props(n))
        assertCheck(n, BarTouches(BodyPoint.WAIST, Which.END))
        // Out of the picture plane: the elbows point out and back, never forward or in (a wrong-way elbow).
        val arm = pose(n, Which.END).nearArm
        inRange(n, "elbow out to the side", arm.out, 25.0..70.0)
        val e = s(n, Which.END)
        inRange(n, "elbow back past the torso", e.shoulder.x - e.near.elbow.x, 5.0..30.0)
        inRange(n, "elbow below the shoulder", e.near.elbow.y - e.shoulder.y, 0.0..25.0)
        inRange(n, "start arm out to the wide grip", pose(n, Which.START).nearArm.out, 10.0..30.0)
    }

    @Test
    fun oneArmCableRow_singleHandle_toTheSide_farArmDown() {
        val n = "One-arm cable row"
        assertSeatedRow(n)
        assertTrue(CableHandle(Side.NEAR) in props(n))
        val e = s(n, Which.END)
        inRange(n, "handle at the waist's height", abs(e.near.grip.y - e.point(BodyPoint.WAIST).y), 0.0..3.0)
        inRange(n, "handle beside the torso", abs(e.near.grip.x - e.pelvis.x), 0.0..10.0)
        val arm = pose(n, Which.END).nearArm
        inRange(n, "elbow out a little", arm.out, 0.0..40.0)
        assertTrue(e.near.elbow.x < e.shoulder.x - 10.0, "$n: elbow back past the torso")
        assertEquals(Arm(0.0, 0.0), pose(n, Which.START).farArm)
    }

    @Test
    fun machineRow_chestOnThePad_handlesAtShoulderHeight_elbowsPastTheTorso() {
        val n = "Machine row"
        assertContact(n, BodyPoint.CHEST, Surface.CHEST_PAD)
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        val m = props(n).filterIsInstance<RowMachine>().single()
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        straight(n)
        inRange(n, "handles about level with the shoulders", abs(st.near.grip.y - st.near.shoulder.y), 0.0..4.0)
        // Past the torso: the elbow behind the back's line (10 cm behind the axis).
        assertTrue(en.near.elbow.x < en.pelvis.x - Body.TORSO_RADIUS, "$n: elbows pass the torso")
        inRange(n, "elbow below the shoulder", en.near.elbow.y - en.shoulder.y, 0.0..20.0)
        inRange(n, "lever length kept", abs((st.near.grip - m.pivot).length() - (en.near.grip - m.pivot).length()), 0.0..0.5)
        assertEquals(90.0, en.torso)
    }

    // ---- rows: lying face down ----

    @Test
    fun chestSupportedRow_slightIncline_chestOnThePad_dumbbellsToTheSides() {
        val n = "Chest-supported row"
        val bench = props(n).filterIsInstance<ProneBench>().single()
        inRange(n, "slight incline", bench.angle, 20.0..35.0)
        inRange(n, "legs trailing nearly straight", pose(n, Which.START).nearLeg.knee, 0.0..30.0)
        assertEquals(bench.angle, s(n, Which.START).torso)
        assertContact(n, BodyPoint.CHEST, Surface.CHEST_PAD)
        straight(n)
        val st = s(n, Which.START)
        inRange(n, "arms hanging down", abs(st.near.grip.x - st.shoulder.x), 0.0..1.0)
        inRange(n, "dumbbells clear of the floor", -st.near.grip.y - Dumbbell.HEAD_RADIUS, 3.0..30.0)
        val e = s(n, Which.END)
        assertTrue(e.near.elbow.y < e.shoulder.y + 8.0 && e.near.elbow.x < e.shoulder.x - 20.0, "$n: elbow up past the back")
        inRange(n, "forearm hanging", abs(e.near.wrist.x - e.near.elbow.x), 0.0..1.0)
        assertTrue(props(n).count { it is Dumbbell } == 2)
    }

    @Test
    fun sealRow_highFlatBench_cambered_platesClearTheFloor_toTheUpperWaist() {
        val n = "Seal row"
        val bench = props(n).filterIsInstance<FlatBench>().single()
        inRange(n, "high bench", bench.height, 85.0..105.0)
        assertEquals(0.0, s(n, Which.START).torso)
        assertContact(n, BodyPoint.CHEST, Surface.BENCH)
        straight(n)
        val bar = props(n).filterIsInstance<CamberedBar>().single()
        val st = s(n, Which.START)
        inRange(n, "plates clear the floor", -(st.near.grip.y + bar.drop) - Barbell.PLATE_DIAMETER / 2, 2.0..15.0)
        assertCheck(n, HeldOver(BodyPoint.UPPER_ABDOMEN, 3.0, Which.END))
        inRange(n, "hands up at the bench top", abs(s(n, Which.END).near.grip.y + bench.height), 0.0..4.0)
    }

    @Test
    fun chestSupportedTBarRow_padFeetOnPlatform_handlesUpHigh() {
        val n = "Chest-supported T-bar row"
        val bench = props(n).filterIsInstance<ProneBench>().single()
        assertNotNull(bench.footPlate)
        assertContact(n, BodyPoint.CHEST, Surface.CHEST_PAD)
        assertContact(n, BodyPoint.FEET, Surface.FOOT_PLATE)
        inRange(n, "legs trailing nearly straight", pose(n, Which.START).nearLeg.knee, 0.0..30.0)
        straight(n)
        val lever = props(n).filterIsInstance<TBarLever>().single()
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        inRange(n, "lever length kept", abs((st.near.grip - lever.pivot).length() - (en.near.grip - lever.pivot).length()), 0.0..0.5)
        inRange(n, "handles up high", st.near.grip.y - en.near.grip.y, 25.0..50.0)
        assertTrue(en.near.elbow.x < en.shoulder.x - 10.0, "$n: elbow past the back")
    }

    @Test
    fun invertedRow_heelsDown_bodyStraight_straightArmsThenChestToTheBar() {
        val n = "Inverted row"
        val bar = props(n).filterIsInstance<RackBar>().single()
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        assertContact(n, BodyPoint.HANDS, Surface.PULL_UP_BAR)
        for (w in Which.entries) {
            val p = pose(n, w)
            assertEquals(Leg(0.0, 0.0, 0.0), p.nearLeg, "$n: body straight")
        }
        straight(n)
        // The heels stay put: the body pivots on them.
        inRange(n, "heels put", (s(n, Which.START).near.heel - s(n, Which.END).near.heel).length(), 0.0..0.5)
        val st = s(n, Which.START)
        inRange(n, "back just off the floor", -st.shoulder.y - Body.TORSO_RADIUS, 0.0..15.0)
        val e = s(n, Which.END)
        val chest = e.point(BodyPoint.UPPER_CHEST)
        inRange(n, "chest to the bar", (chest - Point(bar.x, -bar.height)).length() - PullUpBar.RADIUS, -0.5..1.5)
        // The hang: the arms lean towards the feet as little as a rigid body on fixed heels allows (about 28°).
        inRange(n, "arms off vertical at the hang", abs(angle(st.near.grip - st.shoulder) - 90.0), 0.0..30.0)
        assertTrue(st.near.grip.x > st.shoulder.x, "$n: hands towards the feet, not the head")
    }

    // ---- pulldowns ----

    private fun assertPulldownSeat(n: String) {
        assertContact(n, BodyPoint.HIPS, Surface.SEAT)
        assertContact(n, BodyPoint.THIGHS, Surface.THIGH_PAD)
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
    }

    @Test
    fun closeGripPulldown_vHandle_fullStretch_thenAtTheUpperChest() {
        val n = "Close-grip pulldown"
        assertPulldownSeat(n)
        assertTrue(CableAttachment(Attachment.V_HANDLE) in props(n))
        straight(n)
        // The approved Lat pulldown's stretch and lean.
        assertEquals(figure("Lat pulldown").start.pose, pose(n, Which.START))
        inRange(n, "handle at the upper chest", (held(n, Which.END) - s(n, Which.END).point(BodyPoint.UPPER_CHEST)).length(), 4.0..8.0)
    }

    @Test
    fun machinePulldown_leverFromBehind_sameLength_toTheUpperChest() {
        val n = "Machine pulldown"
        assertPulldownSeat(n)
        val lever = props(n).filterIsInstance<PulldownLever>().single()
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        straight(n)
        assertTrue(lever.pivot.x < st.pelvis.x, "$n: pivot behind")
        inRange(n, "lever length kept", abs((st.near.grip - lever.pivot).length() - (en.near.grip - lever.pivot).length()), 0.0..0.5)
        assertCheck(n, HandsLevelWith(BodyPoint.UPPER_CHEST, 3.0, Which.END))
        assertTrue(props(n).none { it is CableStack }, "$n: a lever, not a cable")
    }

    @Test
    fun straightArmPulldown_seatedFacingAway_fixedBend_overheadToTheThighs() {
        val n = "Straight-arm pulldown"
        val stack = props(n).filterIsInstance<CableStack>().single()
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        assertTrue(stack.x + stack.reach < st.pelvis.x, "$n: the pulley behind (facing away)")
        assertTrue(stack.pulleyHeight > -st.head.y, "$n: a high pulley")
        for (w in Which.entries) inRange(n, "fixed slight bend", bend(n, w), 10.0..20.0)
        assertEquals(pose(n, Which.START).nearArm.elbow, pose(n, Which.END).nearArm.elbow)
        assertTrue(st.near.grip.y < st.head.y, "$n: bar overhead")
        val thighTop = (en.near.hip + en.near.knee).let { it * 0.5 }.y - Body.THIGH_WIDTH / 2
        inRange(n, "down to just above the thighs", thighTop - en.near.grip.y, 2.0..10.0)
        assertTrue(en.near.grip.x > en.shoulder.x, "$n: forward")
    }

    @Test
    fun underhandPulldown_front_shoulderWidth_elbowsToTheSides() {
        val n = "Underhand pulldown"
        assertPulldownSeat(n)
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        straight(n)
        inRange(n, "shoulder-width grip", st.near.grip.x - st.pelvis.x, 22.0..28.0)
        assertTrue(st.near.grip.y < st.head.y - 20.0, "$n: arms up")
        inRange(n, "bar at the upper chest", abs(en.near.grip.y - en.point(BodyPoint.UPPER_CHEST).y), 0.0..2.0)
        // The elbows down at the sides, just outside the shoulders, not out wide nor in front (out of the plane).
        inRange(n, "elbow below the shoulder", en.near.elbow.y - en.near.shoulder.y, 15.0..33.0)
        inRange(n, "elbow at the side", en.near.elbow.x - en.near.shoulder.x, 0.0..12.0)
        inRange(n, "elbow not far forward or back", pose(n, Which.END).nearArm.out, -30.0..30.0)
        assertCheck(n, UpperArmDown(25.0, Which.END))
    }

    @Test
    fun oneArmPulldown_front_oneHandle_straightUp_thenSideOfTheChest() {
        val n = "One-arm pulldown"
        assertPulldownSeat(n)
        assertTrue(CableHandle(Side.NEAR) in props(n))
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        straight(n)
        assertTrue(st.near.grip.y < st.head.y - 20.0, "$n: arm straight up")
        inRange(n, "side of the chest", abs(en.near.grip.y - en.point(BodyPoint.CHEST).y), 0.0..2.0)
        inRange(n, "beside the chest", en.near.grip.x - en.pelvis.x, 18.0..30.0)
        inRange(n, "elbow below the shoulder, at the side", en.near.elbow.y - en.near.shoulder.y, 15.0..33.0)
        inRange(n, "elbow at the side, not in or out wide", en.near.elbow.x - en.near.shoulder.x, 0.0..12.0)
        // The far hand rests low on the pad: below the shoulder, in front.
        assertTrue(st.far.grip.y > st.far.shoulder.y + 20.0, "$n: far hand down on the pad")
        assertEquals(pose(n, Which.START).farArm, pose(n, Which.END).farArm)
    }

    // ---- pull-ups and chin-ups ----

    @Test
    fun chinUp_front_shoulderWidth_hang_thenElbowsAtTheSides() {
        val n = "Chin-up"
        assertContact(n, BodyPoint.HANDS, Surface.PULL_UP_BAR)
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        straight(n)
        inRange(n, "about shoulder width", st.near.grip.x - st.pelvis.x, 22.0..28.0)
        assertTrue(st.near.grip.y < st.head.y - 30.0, "$n: hanging below the bar")
        inRange(n, "elbow below the shoulder", en.near.elbow.y - en.near.shoulder.y, 15.0..33.0)
        inRange(n, "elbow at the side", en.near.elbow.x - en.near.shoulder.x, -2.0..12.0)
        inRange(n, "elbow forward, not back", pose(n, Which.END).nearArm.out, 0.0..60.0)
        inRange(n, "chin about at the bar", abs(en.chin().y - en.near.grip.y), 0.0..4.0)
    }

    @Test
    fun weightedPullUp_pullUpPoses_plateOnABeltBelowTheLegs() {
        val n = "Weighted pull-up"
        assertEquals(figure("Pull-up").start.pose, pose(n, Which.START))
        assertEquals(figure("Pull-up").end.pose, pose(n, Which.END))
        val belt = props(n).filterIsInstance<DipBelt>().single()
        val st = s(n, Which.START)
        val plate = belt.plateCentre(st)
        assertTrue(plate.y > maxOf(st.near.knee.y, st.near.ankle.y), "$n: the plate hangs below the knees and feet")
        assertCheck(n, ChinOverBar(at = Which.END))
        // The chain is drawn over the near leg, from the hip to the plate, so the plate hangs from the belt.
        val chain = belt.drawFront(View.SIDE, null, st).map { it.shape as Capsule }.last()
        assertEquals(belt.hook(st), Point(chain.x1, chain.y1))
        inRange(n, "chain to the plate's top", abs(chain.y2 - (plate.y - belt.plate / 2)), 0.0..4.0)
    }

    @Test
    fun weightedChinUp_belt_elbowsAtTheSides() {
        val n = "Weighted chin-up"
        assertTrue(props(n).any { it is DipBelt })
        assertEquals(figure("Pull-up").start.pose, pose(n, Which.START))
        inRange(n, "upper arm down the torso", upperArmOffTorso(n, Which.END), 0.0..12.0)
        val e = s(n, Which.END)
        inRange(n, "bar at the upper chest", e.near.grip.y - e.shoulder.y, -5.0..8.0)
    }

    @Test
    fun neutralGripPullUp_parallelHandles_neckAtHandHeight() {
        val n = "Neutral-grip pull-up"
        assertTrue(props(n).single() is ParallelHandles)
        straight(n)
        val e = s(n, Which.END)
        inRange(n, "neck at the hands' height", abs(e.near.grip.y - e.point(BodyPoint.NECK).y), 0.0..2.0)
        inRange(n, "handles in front of the shoulder", e.near.grip.x - e.shoulder.x, 12.0..22.0)
        assertEquals(90.0, e.torso)
    }

    @Test
    fun bandAssistedPullUp_bandedLegStraight_freeFootOnTop_chinAbove() {
        val n = "Band-assisted pull-up"
        assertTrue(props(n).any { it is PullUpBand })
        for (w in Which.entries) {
            val p = pose(n, w)
            assertEquals(0.0, p.nearLeg.knee, "$n: banded leg straight")
            val sk = s(n, w)
            // The free foot rests on the banded one: its sole just above it.
            inRange(n, "free foot on the banded foot", (sk.far.ankle - sk.near.ankle).length(), 5.0..12.0)
            assertTrue(sk.far.ankle.y < sk.near.ankle.y, "$n: free foot on top")
        }
        assertCheck(n, ChinOverBar(at = Which.END))
    }

    private fun assertAssisted(n: String) {
        assertContact(n, BodyPoint.SHINS, Surface.ASSIST_STEP)
        assertContact(n, BodyPoint.HANDS, Surface.PULL_UP_BAR)
        straight(n)
        val m0 = props(n, Which.START).filterIsInstance<AssistPullUpMachine>().single()
        val m1 = props(n, Which.END).filterIsInstance<AssistPullUpMachine>().single()
        assertTrue(m1.padHeight > m0.padHeight + 15.0, "$n: the pad rides up with the knees")
        inRange(n, "pad lever kept", abs((Point(m0.padX, -m0.padHeight) - m0.pivot).length() - (Point(m1.padX, -m1.padHeight) - m1.pivot).length()), 0.0..0.5)
        for (w in Which.entries) assertEquals(90.0, pose(n, w).nearLeg.knee, "$n: kneeling")
        assertTrue(props(n).contains(Floor))
    }

    @Test
    fun assistedPullUp_kneeling_neckToHandHeight() {
        val n = "Assisted pull-up"
        assertAssisted(n)
        val e = s(n, Which.END)
        inRange(n, "neck at the hands' height", abs(e.near.grip.y - e.point(BodyPoint.NECK).y), 0.0..2.0)
    }

    @Test
    fun assistedChinUp_kneeling_elbowsToTheSides() {
        val n = "Assisted chin-up"
        assertAssisted(n)
        inRange(n, "upper arm down the torso", upperArmOffTorso(n, Which.END), 0.0..12.0)
    }

    // ---- pullovers ----

    @Test
    fun dbPullover_acrossTheBench_hipsDown_overTheChest_thenUpperArmsLevel_withASpotter() {
        val n = "DB pullover"
        assertContact(n, BodyPoint.UPPER_BACK, Surface.BENCH)
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        val st = s(n, Which.START)
        inRange(n, "hips below the shoulders", st.pelvis.y - st.shoulder.y, 5.0..20.0)
        for (w in Which.entries) inRange(n, "fixed slight bend", bend(n, w), 10.0..20.0)
        assertEquals(pose(n, Which.START).nearArm.elbow, pose(n, Which.END).nearArm.elbow)
        inRange(n, "over the chest", abs(held(n, Which.START).x - st.point(BodyPoint.CHEST).x), 0.0..2.0)
        // Upper arms about level with the torso: within 15° of its line past the head, never beyond it.
        val e = s(n, Which.END)
        val off = fold(angle(e.near.elbow - e.near.shoulder) - e.torso)
        inRange(n, "upper arm about in line, short of it", off, -15.0..0.0)
        assertTrue(held(n, Which.END).x < e.head.x, "$n: behind the head")
        for (w in Which.entries) {
            val spotter = props(n, w).filterIsInstance<Spotter>().single().pose.skeleton()
            inRange(n, "spotter's hands near the wrists", (spotter.near.grip - s(n, w).near.wrist).length(), 0.0..25.0)
            assertTrue(spotter.pelvis.x < e.head.x, "$n: spotter at the head")
        }
    }

    @Test
    fun machinePullover_pivotAtTheShoulder_overheadThenElbowsAtTheSides() {
        val n = "Machine pullover"
        assertContact(n, BodyPoint.UPPER_BACK, Surface.BACK_PAD)
        assertContact(n, BodyPoint.HIPS, Surface.SEAT)
        val m = props(n).filterIsInstance<PulloverMachine>().single()
        assertEquals(s(n, Which.START).shoulder, m.pivot)
        inRange(n, "upper arms in line, as far as they go", pose(n, Which.START).nearArm.shoulder, 160.0..175.0)
        assertTrue(s(n, Which.START).near.grip.x < s(n, Which.START).shoulder.x, "$n: hands behind the head")
        inRange(n, "elbows at the sides", upperArmOffTorso(n, Which.END), 0.0..10.0)
        assertEquals(pose(n, Which.START).nearArm.elbow, pose(n, Which.END).nearArm.elbow)
    }

    // ---- shrugs ----

    private fun assertShrug(n: String) {
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        assertEquals(0.0, pose(n, Which.START).shrug)
        inRange(n, "shoulders up", en.near.shoulder.y - st.near.shoulder.y, -8.0..-6.0)
        // Only the shoulders rise: the hips, torso axis and head stay.
        assertEquals(st.head, en.head)
        assertEquals(st.pelvis, en.pelvis)
        for (w in Which.entries) inRange(n, "arms straight", bend(n, w), 0.0..1.0)
        inRange(n, "the load rises with them", held(n, Which.START).y - held(n, Which.END).y, 6.0..8.0)
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
    }

    @Test
    fun barbellShrug_front_barInFrontOfTheThighs_shouldersUp() {
        val n = "Barbell shrug"
        assertShrug(n)
        val st = s(n, Which.START)
        inRange(n, "a little wider than the shoulders", st.near.grip.x - st.pelvis.x, 20.0..28.0)
        assertTrue(props(n).contains(Barbell()))
    }

    @Test
    fun dbShrug_front_dumbbellsEndOnAtTheSides_clearOfTheThighs() {
        val n = "DB shrug"
        assertShrug(n)
        assertTrue(props(n).filterIsInstance<Dumbbell>().all { it.endOnFront })
        val st = s(n, Which.START)
        inRange(n, "dumbbell clear of the thigh", st.near.grip.x - Dumbbell.HEAD_RADIUS - (st.near.hip.x + Body.THIGH_WIDTH / 2), 0.0..6.0)
    }

    @Test
    fun trapBarShrug_side_handlesBesideTheLegs_shouldersUp() {
        val n = "Trap-bar shrug"
        assertShrug(n)
        assertTrue(TrapBar in props(n))
        assertCheck(n, HandlesBesideLegs(3.0, Which.END))
    }

    // ---- back extensions ----

    private fun assertExtension(n: String) {
        for (c in listOf(BodyPoint.THIGHS to Surface.THIGH_PAD, BodyPoint.FEET to Surface.FOOT_PLATE, BodyPoint.SHINS to Surface.LEG_BRACE)) {
            assertContact(n, c.first, c.second)
        }
        val bench = props(n).filterIsInstance<HyperBench>().single()
        assertEquals(45.0, bench.angle)
        // In line with the legs at the top, not arched past it; a right angle at the bottom.
        val top = pose(n, Which.START)
        inRange(n, "in line at the top", top.nearLeg.hip, -2.0..2.0)
        inRange(n, "body line 45°", top.torso, 43.0..47.0)
        inRange(n, "right angle at the bottom", pose(n, Which.END).nearLeg.hip, 85.0..95.0)
        // The legs stay put, straight along the line.
        for (w in Which.entries) assertEquals(0.0, pose(n, w).nearLeg.knee)
        assertEquals(s(n, Which.START).near.ankle, s(n, Which.END).near.ankle)
        // The hands crossed on the chest: the grip near the chest's front.
        for (w in Which.entries) inRange(n, "hands on the chest", (s(n, w).near.grip - s(n, w).point(BodyPoint.CHEST)).length(), 0.0..12.0)
        // The belly clear of the pad at the bottom: the pad's top edge below the hip joint.
        val e = s(n, Which.END)
        assertTrue((bench.pad - e.pelvis).length() > 20.0, "$n: pad below the hips")
    }

    @Test
    fun backExtension_45degreeBench_inLine_thenRightAngle() = assertExtension("Back extension")

    @Test
    fun weightedBackExtension_plateOnTheChest() {
        val n = "Weighted back extension"
        assertExtension(n)
        assertTrue(props(n).any { it is ChestPlate })
    }

    // ---- face pull ----

    @Test
    fun facePull_highRope_armsStraightInFront_thenElbowsOutHigh_ropeToTheFace() {
        val n = "Face pull"
        assertTrue(CableAttachment(Attachment.ROPE) in props(n))
        val stack = props(n).filterIsInstance<CableStack>().single()
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        assertEquals(90.0, st.torso)
        inRange(n, "knees slightly bent", pose(n, Which.START).nearLeg.knee, 5.0..20.0)
        straight(n)
        inRange(n, "in front of the face", abs(st.near.grip.y - st.head.y), 0.0..2.0)
        assertTrue(stack.pulleyHeight > -st.head.y, "$n: a high pulley")
        // END: the hands beside the face; the upper arm out to the side (drawn short) and level with the shoulder; the
        // elbow below the hand (the forearm turned up), never in front of the face (a wrong-way elbow).
        inRange(n, "hands at the face", (en.near.grip - en.head).length(), 0.0..16.0)
        val arm = pose(n, Which.END).nearArm
        inRange(n, "upper arm out of the plane", arm.out, 55.0..90.0)
        inRange(n, "elbow level with the shoulder", abs(en.near.elbow.y - en.near.shoulder.y), 0.0..10.0)
        assertTrue(en.near.elbow.y > en.near.grip.y, "$n: forearm up")
        assertTrue(en.near.elbow.x < en.near.grip.x, "$n: elbow back from the hands")
    }
}
