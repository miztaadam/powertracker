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

/** The arm batch (Task 9): every arm exercise, each with the key positions its guide gives. */
class BatchArmsTest {
    private fun figure(name: String): ExerciseFigure = assertNotNull(Figures.all[name], "no figure for $name")

    private fun s(name: String, which: Which) = figure(name).let { it.scene(which).pose.skeleton(it.view) }

    private fun pose(name: String, which: Which) = figure(name).scene(which).pose

    private fun props(name: String, which: Which = Which.START) = figure(name).scene(which).props

    private fun assertContact(name: String, point: BodyPoint, surface: Surface) =
        assertTrue(figure(name).contacts.any { it.point == point && it.surface == surface && it.at == Which.entries.toSet() }, "$name: $point on $surface")

    private fun inRange(name: String, label: String, value: Double, range: ClosedFloatingPointRange<Double>) =
        assertTrue(value in range, "$name: $label ${round1(value)} not in $range")

    private fun bend(name: String, which: Which, side: Side = Side.NEAR) =
        pose(name, which).let { JointLimits.elbowBend(if (side == Side.NEAR) it.nearArm else it.farArm) }

    private fun angle(v: Point) = atan2(-v.y, v.x) * 180 / PI

    /** How far the near upper arm points from straight down, in degrees (0 = hanging). */
    private fun upperFromDown(sk: Skeleton, side: Side = Side.NEAR) = sk.side(side).let { abs(fold(angle(it.elbow - it.shoulder) + 90.0)) }

    /** How far the near upper arm points from straight up, in degrees. */
    private fun upperFromUp(sk: Skeleton) = abs(fold(angle(sk.near.elbow - sk.near.shoulder) - 90.0))

    /** The near forearm's angle above level, in degrees (forward or back, whichever way it points). */
    private fun forearmAboveLevel(sk: Skeleton, side: Side = Side.NEAR) = sk.side(side).let {
        val v = it.wrist - it.elbow
        atan2(-v.y, abs(v.x)) * 180 / PI
    }

    private fun knees(name: String, range: ClosedFloatingPointRange<Double>) {
        for (w in Which.entries) {
            inRange(name, "near knee ($w)", pose(name, w).nearLeg.knee, range)
            inRange(name, "far knee ($w)", pose(name, w).farLeg.knee, range)
        }
    }

    /** The distance from [p] to the segment [a]–[b]. */
    private fun dist(p: Point, a: Point, b: Point): Double {
        val ab = b - a
        val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / (ab.x * ab.x + ab.y * ab.y)).coerceIn(0.0, 1.0)
        return (p - (a + ab * t)).length()
    }

    @Test
    fun arms_everyExerciseHasAFigure_thatPassesItsChecks() {
        val area = Library.all().filter { it.area == BodyArea.ARMS }.map { it.name }
        assertEquals(31, area.size)
        val problems = area.flatMap { name -> Figures.all[name]?.let { FigureChecks.check(name, it) } ?: listOf(FigureProblem(name, null, "no figure")) }
        assertEquals(emptyList(), problems.map { "${it.name} ${it.which}: ${it.message}" })
        for (name in area) assertEquals(View.SIDE, figure(name).view, name)
    }

    // ---- standing curls ----

    /** Elbows at the sides in both panels, straight arms to a near-upright forearm, standing tall. */
    private fun assertStandingCurl(n: String, side: Side = Side.NEAR) {
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        for (w in Which.entries) {
            inRange(n, "upper arm from hanging ($w)", upperFromDown(s(n, w), side), 0.0..10.0)
            assertEquals(90.0, pose(n, w).torso, 1e-9, "$n: torso upright")
            inRange(n, "wrist ($w)", pose(n, w).nearArm.wrist, 0.0..0.0)
        }
        inRange(n, "arm straight", bend(n, Which.START, side), 0.0..1.0)
        inRange(n, "forearm near upright, not past it", forearmAboveLevel(s(n, Which.END), side), 55.0..72.0)
        inRange(n, "curled forward", s(n, Which.END).side(side).wrist.x - s(n, Which.END).side(side).elbow.x, 5.0..20.0)
    }

    @Test
    fun curl_barInFrontOfTheThighs_elbowsAtSides_toForearmsUpright() {
        for (n in listOf("Curl", "EZ-bar curl", "Reverse curl")) {
            assertStandingCurl(n)
            knees(n, 0.0..1.0)
            val st = s(n, Which.START)
            inRange(n, "bar in front of the thighs", st.near.grip.x - (st.near.hip.x + Body.THIGH_WIDTH / 2) - Barbell.SHAFT_RADIUS, 0.0..2.0)
        }
        assertTrue(Barbell() in props("Curl"), "Curl: a barbell")
        assertTrue(EZBar in props("EZ-bar curl"), "EZ-bar curl: an EZ bar, its smaller plates telling it apart")
        assertTrue(EZBar.PLATE_DIAMETER < Barbell.PLATE_DIAMETER - 10.0)
    }

    @Test
    fun dbCurl_palmsInToPalmUp_oneArmCurls_theOtherHangs() {
        for (n in listOf("DB curl", "Seated DB curl")) {
            assertEquals(listOf(Dumbbell(Side.FAR, neutral = true, squareToForearm = true), Dumbbell(Side.NEAR, neutral = true, squareToForearm = true)), props(n).filterIsInstance<Dumbbell>(), "$n: palms in")
            assertEquals(listOf(Dumbbell(Side.FAR, neutral = true, squareToForearm = true), Dumbbell(Side.NEAR)), props(n, Which.END).filterIsInstance<Dumbbell>(), "$n: palm turned up")
            for (w in Which.entries) {
                inRange(n, "upper arm at the side ($w)", upperFromDown(s(n, w)), 0.0..10.0)
                inRange(n, "free arm hangs ($w)", bend(n, w, Side.FAR), 0.0..0.0)
                inRange(n, "free arm at the side ($w)", upperFromDown(s(n, w), Side.FAR), 0.0..1.0)
            }
            inRange(n, "forearm near upright", forearmAboveLevel(s(n, Which.END)), 55.0..72.0)
        }
        assertStandingCurl("DB curl")
        knees("DB curl", 0.0..1.0)
        val n = "Seated DB curl"
        assertContact(n, BodyPoint.HIPS, Surface.BENCH)
        inRange(n, "arm straight", bend(n, Which.START), 0.0..1.0)
        inRange(n, "curled", bend(n, Which.END), 140.0..148.0)
        for (w in Which.entries) assertEquals(90.0, pose(n, w).torso, 1e-9, "$n: sitting tall ($w)")
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        knees(n, 70.0..110.0)
        // The hanging dumbbells clear the floor.
        inRange(n, "dumbbell off the floor", -s(n, Which.START).near.grip.y - Dumbbell.HEAD_RADIUS, 5.0..100.0)
    }

    @Test
    fun hammerCurl_palmsInBothPanels_bothArms_staggeredStance() {
        val n = "Hammer curl"
        assertStandingCurl(n)
        assertStandingCurl(n, Side.FAR)
        for (w in Which.entries) {
            assertTrue(props(n, w).filterIsInstance<Dumbbell>().all { it.neutral && it.squareToForearm }, "$n: palms in ($w)")
        }
        val st = s(n, Which.START)
        inRange(n, "front foot ahead of the back foot", st.far.ankle.x - st.near.ankle.x, 20.0..40.0)
        knees(n, 0.0..25.0)
        // Versus the DB curl: its END dumbbell is end-on, the hammer's in profile.
        assertTrue(props("DB curl", Which.END).filterIsInstance<Dumbbell>().any { it.hand == Side.NEAR && !it.neutral })
    }

    @Test
    fun cableCurls_faceTheLowPulley_cableToTheHands_elbowsAtSides() {
        for ((n, held) in listOf("Cable curl" to CableAttachment(Attachment.STRAIGHT_BAR), "Cable hammer curl" to CableAttachment(Attachment.ROPE), "One-arm cable curl" to CableHandle(Side.NEAR))) {
            assertStandingCurl(n)
            knees(n, 0.0..1.0)
            assertTrue(held in props(n), "$n: $held")
            val stack = props(n).filterIsInstance<CableStack>().single()
            val st = s(n, Which.START)
            assertTrue(stack.x > st.near.toe.x + 10.0, "$n: the column in front")
            inRange(n, "low pulley", stack.pulleyHeight, 5.0..30.0)
            assertCableToTheHand(n)
        }
        for (w in Which.entries) inRange("One-arm cable curl", "free arm hangs", bend("One-arm cable curl", w, Side.FAR), 0.0..0.0)
    }

    /** The cable is drawn from the pulley to the hand's grip in both panels. */
    private fun assertCableToTheHand(n: String) {
        val stack = props(n).filterIsInstance<CableStack>().single()
        for (w in Which.entries) {
            val cable = FigureRenderer.world(figure(n).scene(w), View.SIDE).map { it.shape }
                .filterIsInstance<Capsule>().filter { it.role == Role.EQUIPMENT && abs(it.width - 0.8) < 1e-9 }
            val g = s(n, w).near.grip
            val p = Point(stack.x + stack.reach, -stack.pulleyHeight)
            assertTrue(
                cable.any { abs(it.x1 - p.x) < 1e-6 && abs(it.y1 - p.y) < 1e-6 && abs(it.x2 - g.x) < 1e-6 && abs(it.y2 - g.y) < 1e-6 },
                "$n: cable from the pulley to the hand ($w)",
            )
        }
    }

    @Test
    fun dragCurl_barUpTheBody_elbowsTravelBack() {
        val n = "Drag curl"
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        knees(n, 0.0..1.0)
        assertTrue(BarTouches(BodyPoint.UPPER_ABDOMEN, Which.END) in figure(n).checks)
        val en = s(n, Which.END)
        inRange(n, "elbow behind the back", en.shoulder.x - en.near.elbow.x - Body.TORSO_RADIUS, 5.0..20.0)
        inRange(n, "forearm about level", forearmAboveLevel(en), -15.0..15.0)
        inRange(n, "arms straight", bend(n, Which.START), 0.0..1.0)
    }

    // ---- seated, incline, prone ----

    @Test
    fun inclineDbCurl_armsHangBehindTheBody_upperArmStaysVertical() {
        val n = "Incline DB curl"
        assertContact(n, BodyPoint.UPPER_BACK, Surface.BACK_PAD)
        assertContact(n, BodyPoint.HIPS, Surface.SEAT)
        val bench = props(n).filterIsInstance<AdjustableBench>().single()
        inRange(n, "bench 45-60", bench.backAngle, 45.0..60.0)
        for (w in Which.entries) {
            val sk = s(n, w)
            inRange(n, "upper arm vertical ($w)", upperFromDown(sk), 0.0..1.0)
            inRange(n, "behind the torso's line ($w)", -pose(n, w).nearArm.shoulder, 25.0..40.0)
            inRange(n, "dumbbells clear of the floor ($w)", -sk.far.grip.y - Dumbbell.HEAD_RADIUS, 5.0..100.0)
        }
        inRange(n, "curled", bend(n, Which.END), 140.0..148.0)
        assertEquals(Dumbbell(Side.NEAR), props(n, Which.END).filterIsInstance<Dumbbell>().last(), "$n: palm up at the top")
        knees(n, 60.0..110.0)
    }

    @Test
    fun spiderCurl_chestOnTheIncline_armsHangStraightDown_upperArmStill() {
        val n = "Spider curl"
        assertContact(n, BodyPoint.CHEST, Surface.CHEST_PAD)
        assertTrue(EZBar in props(n))
        for (w in Which.entries) inRange(n, "upper arm vertical ($w)", upperFromDown(s(n, w)), 0.0..1.0)
        val st = s(n, Which.START)
        inRange(n, "plates clear of the floor", -st.near.grip.y - EZBar.PLATE_DIAMETER / 2, 3.0..60.0)
        val bench = props(n).filterIsInstance<ProneBench>().single()
        // Shoulders near the top: the pad ends within 10 cm (along it) of the chest.
        val top = bench.low + Point(cos(bench.angle) * bench.length, -sin(bench.angle) * bench.length)
        inRange(n, "pad's top near the shoulders", (top - st.shoulder).length(), 0.0..25.0)
        inRange(n, "as far as the elbows bend", bend(n, Which.END), 140.0..148.0)
        knees(n, 10.0..20.0)
    }

    @Test
    fun concentrationCurl_upperArmOnTheThigh_curlsToTheShoulder() {
        val n = "Concentration curl"
        assertContact(n, BodyPoint.HIPS, Surface.BENCH)
        for (w in Which.entries) {
            val sk = s(n, w)
            inRange(n, "elbow on the thigh ($w)", dist(sk.near.elbow, sk.near.hip, sk.near.knee), 4.0..14.0)
            inRange(n, "leaning forward ($w)", pose(n, w).torso, 45.0..72.0)
        }
        inRange(n, "arm straight", bend(n, Which.START), 0.0..1.0)
        inRange(n, "dumbbell clear of the floor", -s(n, Which.START).near.grip.y - Dumbbell.HEAD_RADIUS, 3.0..60.0)
        val bench = props(n).filterIsInstance<FlatBench>().single()
        for (w in Which.entries) {
            val f = s(n, w).far
            // The free arm hangs straight down in front of the bench: never along the torso, through the hip or the bench.
            inRange(n, "free arm hangs straight down ($w)", upperFromDown(s(n, w), Side.FAR), 0.0..1.0)
            inRange(n, "free arm straight ($w)", bend(n, w, Side.FAR), 0.0..0.0)
            assertTrue(f.wrist.x > f.hip.x + 20.0 && f.wrist.x > bench.x + bench.length + Body.FOREARM_WIDTH, "$n: free wrist in front of the hip and the bench ($w)")
            inRange(n, "free hand off the floor ($w)", -f.hand.y - Body.HAND_WIDTH / 2, 2.0..60.0)
        }
        val en = s(n, Which.END)
        inRange(n, "toward the front of the shoulder", (en.near.grip - en.shoulder).length(), 10.0..30.0)
        assertTrue(en.near.grip.x > en.shoulder.x, "$n: in front of the shoulder")
        knees(n, 70.0..110.0)
    }

    private fun assertWristCurl(n: String) {
        assertContact(n, BodyPoint.HIPS, Surface.BENCH)
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        // Only the hand moves: the forearms stay on the thighs, level, the wrists just past the knees.
        assertEquals(st.near.elbow, en.near.elbow)
        assertEquals(st.near.wrist, en.near.wrist)
        for (sk in listOf(st, en)) {
            for (side in Side.entries) {
                val l = sk.side(side)
                inRange(n, "forearm on the thigh ($side)", dist(l.wrist, l.hip, l.knee), Body.THIGH_WIDTH / 2 + Body.FOREARM_WIDTH / 2 - 1.5..Body.THIGH_WIDTH / 2 + Body.FOREARM_WIDTH / 2 + 1.5)
                inRange(n, "elbow on the thigh ($side)", dist(l.elbow, l.hip, l.knee), Body.THIGH_WIDTH / 2 + Body.FOREARM_WIDTH / 2 - 1.5..Body.THIGH_WIDTH / 2 + Body.FOREARM_WIDTH / 2 + 1.5)
            }
            inRange(n, "forearm about level", forearmAboveLevel(sk), -10.0..10.0)
            inRange(n, "wrist just past the knee", sk.near.wrist.x - sk.near.knee.x, 0.0..8.0)
        }
        inRange(n, "hand down", st.near.hand.y - st.near.wrist.y, 10.0..20.0)
        inRange(n, "hand up", en.near.wrist.y - en.near.hand.y, 10.0..20.0)
        assertEquals(0.0, pose(n, Which.END).farArm.wrist, "$n: the free hand stays straight")
        knees(n, 70.0..110.0)
    }

    @Test
    fun wristCurls_forearmsOnTheThighs_onlyTheWristMoves_downThenUp() {
        assertWristCurl("Wrist curl")
        assertWristCurl("Reverse wrist curl")
    }

    // ---- on an arm pad ----

    private fun assertOnThePad(n: String) {
        assertContact(n, BodyPoint.ELBOWS, Surface.ARM_PAD)
        assertContact(n, BodyPoint.HIPS, Surface.SEAT)
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        // The upper arm does not move between the panels.
        assertEquals(s(n, Which.START).near.elbow, s(n, Which.END).near.elbow, "$n: elbow still")
        // Seated with the feet forward: the knees bent 40-60°, not kneeling.
        knees(n, 40.0..60.0)
    }

    @Test
    fun preacherCurls_upperArmsOnThePad_straightToForearmUpright() {
        for (n in listOf("Preacher curl", "DB preacher curl")) {
            assertOnThePad(n)
            inRange(n, "arm straight", bend(n, Which.START), 0.0..1.0)
            inRange(n, "forearm upright", forearmAboveLevel(s(n, Which.END)), 88.0..90.0)
            val bench = props(n).filterIsInstance<PreacherBench>().single()
            inRange(n, "pad slope", bench.padAngle, 45.0..55.0)
            // The armpit near the top of the pad.
            inRange(n, "pad top by the armpit", (bench.padTop - s(n, Which.START).shoulder).length(), 0.0..10.0)
        }
        assertTrue(EZBar in props("Preacher curl"))
        assertEquals(listOf(Dumbbell(Side.NEAR)), props("DB preacher curl").filterIsInstance<Dumbbell>())
    }

    @Test
    fun armMachines_elbowsOnThePivot_leverKeepsItsLength() {
        for (n in listOf("Machine curl", "Machine triceps extension")) {
            assertOnThePad(n)
            val m = props(n).filterIsInstance<ArmPadMachine>().single()
            for (w in Which.entries) {
                val sk = s(n, w)
                inRange(n, "pivot at the elbow ($w)", (sk.near.elbow - m.pivot).length(), 0.0..0.5)
                inRange(n, "lever length ($w)", (sk.near.grip - m.pivot).length(), FORE_LINK_TEST - 0.5..FORE_LINK_TEST + 0.5)
            }
            assertTrue(m.stackX > m.padTop.x + 20.0, "$n: stack in front of the pad")
        }
        inRange("Machine curl", "nearly straight", bend("Machine curl", Which.START), 10.0..20.0)
        inRange("Machine curl", "forearm upright", forearmAboveLevel(s("Machine curl", Which.END)), 80.0..90.0)
        val n = "Machine triceps extension"
        inRange(n, "folded", bend(n, Which.START), 140.0..148.0)
        inRange(n, "straight", bend(n, Which.END), 0.0..1.0)
        val st = s(n, Which.START)
        inRange(n, "hand clear in front of the face", st.near.grip.x - st.head.x - Body.HEAD_RADIUS - Body.HAND_WIDTH / 2, 1.0..15.0)
    }

    // ---- pushdowns ----

    @Test
    fun pushdowns_elbowsAtSides_forearmsAboutLevelToNearlyStraight_faceTheHighPulley() {
        for ((n, held) in listOf("Triceps pushdown" to CableAttachment(Attachment.STRAIGHT_BAR), "Rope pushdown" to CableAttachment(Attachment.ROPE), "One-arm pushdown" to CableHandle(Side.NEAR))) {
            assertContact(n, BodyPoint.FEET, Surface.FLOOR)
            for (w in Which.entries) assertTrue(held in props(n, w), "$n: $held ($w)")
            assertCableToTheHand(n)
            knees(n, 0.0..1.0)
            for (w in Which.entries) {
                inRange(n, "upper arm at the side ($w)", upperFromDown(s(n, w)), 0.0..6.0)
                assertEquals(90.0, pose(n, w).torso, 1e-9)
            }
            inRange(n, "forearm about level", forearmAboveLevel(s(n, Which.START)), 0.0..20.0)
            inRange(n, "nearly straight", bend(n, Which.END), 5.0..20.0)
            val stack = props(n).filterIsInstance<CableStack>().single()
            val st = s(n, Which.START)
            assertTrue(stack.x > st.near.toe.x, "$n: facing the pulley")
            assertTrue(stack.pulleyHeight > -st.head.y + Body.HEAD_RADIUS, "$n: above head height")
        }
        for (w in Which.entries) inRange("One-arm pushdown", "free arm hangs", bend("One-arm pushdown", w, Side.FAR), 0.0..0.0)
    }

    // ---- overhead and lying extensions ----

    @Test
    fun cableOverheadExtension_elbowsUp_ropeBehindTheHead_toStraight() {
        val n = "Cable overhead triceps extension"
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        for (w in Which.entries) {
            val sk = s(n, w)
            inRange(n, "upper arm beside the head ($w)", upperFromUp(sk), 10.0..20.0)
            inRange(n, "elbow up ($w)", sk.shoulder.y - sk.near.elbow.y, 28.0..33.0)
        }
        val st = s(n, Which.START)
        assertTrue(st.near.grip.x < st.head.x, "$n: rope behind the head")
        inRange(n, "hand clear of the head", (st.near.grip - st.head).length() - Body.HEAD_RADIUS - Body.HAND_WIDTH / 2, 3.0..20.0)
        inRange(n, "forearms to upper arms", bend(n, Which.START), 140.0..148.0)
        inRange(n, "straight", bend(n, Which.END), 0.0..1.0)
        val stack = props(n).filterIsInstance<CableStack>().single()
        assertTrue(stack.x + stack.reach < st.near.heel.x - 10.0 && stack.pulleyHeight < 30.0, "$n: low pulley behind")
        for (w in Which.entries) assertTrue(CableAttachment(Attachment.ROPE) in props(n, w), "$n: rope ($w)")
        assertCableToTheHand(n)
        knees(n, 0.0..25.0)
    }

    @Test
    fun dbOverheadExtension_upperArmsUpright_forearmsLevel_clearOfTheHead() {
        val n = "DB overhead triceps extension"
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        for (w in Which.entries) inRange(n, "upper arm upright ($w)", upperFromUp(s(n, w)), 0.0..10.0)
        inRange(n, "straight", bend(n, Which.START), 0.0..1.0)
        val en = s(n, Which.END)
        inRange(n, "forearm about level", forearmAboveLevel(en), -5.0..5.0)
        assertTrue(en.near.wrist.x < en.near.elbow.x, "$n: lowered behind the head")
        // The dumbbell (a 14 cm handle, 20 cm heads) is clear of the head.
        val c = OverheadDumbbell.center(en)
        val h = Dumbbell.HANDLE / 2 + 5.0
        val nx = en.head.x.coerceIn(c.x - Dumbbell.HEAD_RADIUS, c.x + Dumbbell.HEAD_RADIUS)
        val ny = en.head.y.coerceIn(c.y - h, c.y + h)
        inRange(n, "dumbbell clear of the head", (Point(nx, ny) - en.head).length() - Body.HEAD_RADIUS, 3.0..30.0)
        knees(n, 0.0..25.0)
    }

    private fun assertLyingWithSpotter(n: String, upperFromUpright: ClosedFloatingPointRange<Double> = 0.0..1.0) {
        assertContact(n, BodyPoint.HEAD, Surface.BENCH)
        assertContact(n, BodyPoint.UPPER_BACK, Surface.BENCH)
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        for (w in Which.entries) {
            assertTrue(props(n, w).any { it is Spotter }, "$n: spotter ($w)")
        }
        knees(n, 90.0..105.0)
        inRange(n, "upper arm upright", upperFromUp(s(n, Which.START)), 0.0..1.0)
        inRange(n, "upper arm at the end", upperFromUp(s(n, Which.END)), upperFromUpright)
    }

    @Test
    fun skullCrusher_upperArmsUpright_barJustAboveTheForehead_spotter() {
        val n = "Skull crusher"
        assertLyingWithSpotter(n)
        assertTrue(EZBar in props(n))
        inRange(n, "straight, not locked", bend(n, Which.START), 3.0..8.0)
        val en = s(n, Which.END)
        inRange(n, "bar just above the head", (en.near.grip - en.head).length() - Body.HEAD_RADIUS - Barbell.SHAFT_RADIUS, 1.5..4.0)
        inRange(n, "over the forehead", abs(en.near.grip.x - en.head.x), 0.0..Body.HEAD_RADIUS)
        assertTrue(en.near.grip.y < en.head.y, "$n: above the face")
    }

    @Test
    fun dbLyingExtension_overTheShoulders_toBesideTheHead_spotter() {
        val n = "DB lying triceps extension"
        // The upper arms tip back towards the head to reach beside it (never towards the feet: elbows up and back).
        assertLyingWithSpotter(n, 5.0..20.0)
        val e = s(n, Which.END)
        assertTrue(e.near.elbow.x < e.shoulder.x, "$n: elbows tip back towards the head")
        assertTrue(LockoutOverShoulders(Which.START) in figure(n).checks)
        val en = s(n, Which.END)
        inRange(n, "beside the head, front to back", abs(en.near.grip.x - en.head.x), 0.0..1.0)
        inRange(n, "level with the head", abs(en.near.grip.y - en.head.y), 0.0..Body.HEAD_RADIUS)
        inRange(n, "bent", bend(n, Which.END), 90.0..148.0)
    }

    @Test
    fun jmPress_rackWithPins_lockoutThenBarToTheUpperChest_elbowsHigh() {
        val n = "JM press"
        assertContact(n, BodyPoint.HEAD, Surface.BENCH)
        knees(n, 90.0..105.0)
        assertTrue(LockoutOverShoulders(Which.START) in figure(n).checks)
        val rack = props(n).filterIsInstance<PowerRack>().single()
        val en = s(n, Which.END)
        assertTrue(rack.safetyHeight < -en.near.grip.y - Barbell.SHAFT_RADIUS - 2.0, "$n: pins under the bar")
        inRange(n, "elbows high", en.shoulder.y - en.near.elbow.y, 12.0..33.0)
        assertTrue(en.near.elbow.x > en.shoulder.x, "$n: elbows towards the feet")
        inRange(n, "forearms against the upper arms", bend(n, Which.END), 140.0..148.0)
        // Between the chin and the upper chest, over the throat, and lowered well below the lockout.
        val chin = en.chin()
        val upperChest = en.point(BodyPoint.UPPER_CHEST)
        assertTrue(en.near.grip.x in chin.x..upperChest.x, "$n: bar between the chin and the upper chest")
        inRange(n, "bar above the body's axis", en.shoulder.y - en.near.grip.y, 10.0..22.0)
    }

    // ---- kickback, dip, push-ups ----

    @Test
    fun kickback_upperArmAlongTheTorso_forearmHangsThenStraight_freeHandOnTheThigh() {
        val n = "Triceps kickback"
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        for (w in Which.entries) {
            val sk = s(n, w)
            val off = abs(fold(angle(sk.near.elbow - sk.near.shoulder) - (pose(n, w).torso + 180.0)))
            inRange(n, "upper arm close to the torso's line ($w)", off, 0.0..20.0)
            val k = sk.far.knee
            val h = sk.far.hip
            inRange(n, "free hand on the front thigh ($w)", dist(sk.far.grip, h, k), Body.THIGH_WIDTH / 2..Body.THIGH_WIDTH / 2 + Body.HAND_WIDTH)
            inRange(n, "leaning forward ($w)", pose(n, w).torso, 20.0..40.0)
        }
        assertEquals(pose(n, Which.START).nearArm.shoulder, pose(n, Which.END).nearArm.shoulder, "$n: upper arm still")
        val st = s(n, Which.START)
        inRange(n, "forearm hangs straight down", abs(fold(angle(st.near.wrist - st.near.elbow) + 90.0)), 0.0..3.0)
        inRange(n, "elbow straight", bend(n, Which.END), 0.0..1.0)
        val en = s(n, Which.END)
        assertTrue(en.near.grip.x < en.pelvis.x, "$n: the dumbbell moved back")
        inRange(n, "back knee", pose(n, Which.START).nearLeg.knee, 0.0..25.0)
        inRange(n, "front knee", pose(n, Which.START).farLeg.knee, 10.0..50.0)
    }

    @Test
    fun tricepsDip_upright_unlikeTheChestDip_elbowsBack() {
        val n = "Triceps dip"
        assertContact(n, BodyPoint.HANDS, Surface.DIP_BAR)
        assertTrue(ShouldersOverHands(5.0, Which.START) in figure(n).checks)
        for (w in Which.entries) inRange(n, "torso upright ($w)", pose(n, w).torso, 78.0..90.0)
        assertTrue(pose("Chest dip", Which.END).torso < 65.0, "the chest dip leans clearly more")
        val en = s(n, Which.END)
        assertEquals(0.0, pose(n, Which.END).nearArm.out, "$n: elbows back, not flared")
        assertTrue(en.near.elbow.x < en.near.grip.x, "$n: elbows back behind the hands")
        val below = angle(en.near.shoulder - en.near.elbow)
        inRange(n, "upper arm a little short of level", below, 15.0..30.0)
        for (w in Which.entries) inRange(n, "feet off the floor ($w)", -maxOf(s(n, w).near.toe.y, s(n, w).near.heel.y), 10.0..120.0)
        knees(n, 35.0..60.0)
        for (w in Which.entries) inRange(n, "hips nearly straight ($w)", pose(n, w).nearLeg.hip, -5.0..20.0)
    }

    @Test
    fun closePushUps_handsUnderTheShouldersOrUnderTheChest() {
        val c = "Close-grip push-up"
        val t = "Triangle push-up"
        for (n in listOf(c, t)) {
            assertContact(n, BodyPoint.HANDS, Surface.FLOOR)
            assertContact(n, BodyPoint.FEET, Surface.FLOOR)
            knees(n, 0.0..0.0)
            inRange(n, "straight arms", bend(n, Which.START), 0.0..2.0)
        }
        val cs = s(c, Which.START)
        inRange(c, "hands under the shoulders", abs(cs.near.wrist.x - cs.shoulder.x), 0.0..3.0)
        assertEquals(0.0, pose(c, Which.START).nearArm.out, "$c: hands at shoulder width")
        assertTrue(ChestOffFloor(4.0, 9.0, Which.END) in figure(c).checks)
        val ce = s(c, Which.END)
        assertTrue(ce.near.elbow.x < ce.shoulder.x, "$c: elbows back along the body")
        val ts = s(t, Which.START)
        inRange(t, "hands under the chest", abs(ts.near.wrist.x - ts.chest().x), 0.0..6.0)
        inRange(t, "hands in towards the middle", pose(t, Which.START).nearArm.out, -25.0..-10.0)
        assertTrue(ChestOffFloor(8.0, 13.0, Which.END) in figure(t).checks)
        // END: the elbow bent out and back, behind the shoulder and above the hand; the hand still under the chest; the
        // forearm leaning in towards the body's middle (a wrong-way elbow, forward or down, fails).
        val te = s(t, Which.END)
        assertTrue(te.near.elbow.x < te.shoulder.x - 15.0, "$t: elbow behind the shoulder")
        assertTrue(te.near.elbow.y < te.near.wrist.y - 10.0, "$t: elbow above the hand")
        inRange(t, "hands still under the chest", abs(te.near.wrist.x - te.chest().x), 0.0..6.0)
        inRange(t, "forearm out of the plane", pose(t, Which.END).nearArm.forearmOut, -55.0..-25.0)
        inRange(t, "bent", bend(t, Which.END), 90.0..148.0)
        // The two read differently from the side: the triangle's hands are further back towards the feet.
        assertTrue((cs.near.wrist.x - cs.shoulder.x) - (ts.near.wrist.x - ts.shoulder.x) > 5.0)
    }

    private companion object {
        val FORE_LINK_TEST = Segment.FOREARM.length + Segment.HAND.length * Body.GRIP

        fun cos(deg: Double) = kotlin.math.cos(deg * PI / 180)

        fun sin(deg: Double) = kotlin.math.sin(deg * PI / 180)
    }
}
