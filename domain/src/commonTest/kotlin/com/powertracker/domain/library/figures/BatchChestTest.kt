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

/** The chest batch (Task 6): every chest exercise, each with the key positions its guide gives. */
class BatchChestTest {
    private fun figure(name: String): ExerciseFigure = assertNotNull(Figures.all[name], "no figure for $name")

    private fun s(name: String, which: Which) = figure(name).let { it.scene(which).pose.skeleton(it.view) }

    private fun held(name: String, which: Which): Point =
        figure(name).scene(which).props.filterIsInstance<HeldProp>().first().center(s(name, which))

    private fun assertContact(name: String, point: BodyPoint, surface: Surface, at: Set<Which> = Which.entries.toSet()) {
        assertTrue(figure(name).contacts.any { it.point == point && it.surface == surface && it.at.containsAll(at) }, "$name: $point on $surface at $at")
    }

    private fun assertCheck(name: String, check: KeyCheck) = assertTrue(check in figure(name).checks, "$name: $check in ${figure(name).checks}")

    private fun inRange(name: String, label: String, value: Double, range: ClosedFloatingPointRange<Double>) =
        assertTrue(value in range, "$name: $label ${round1(value)} not in $range")

    private fun bend(name: String, which: Which, side: Side = Side.NEAR) =
        figure(name).scene(which).pose.let { JointLimits.elbowBend(if (side == Side.NEAR) it.nearArm else it.farArm) }

    /** The world angle of [v], 0 = level towards +x, 90 = straight up. */
    private fun angle(v: Point) = atan2(-v.y, v.x) * 180 / PI

    private fun props(name: String, which: Which = Which.START) = figure(name).scene(which).props

    @Test
    fun chest_everyExerciseHasAFigure_thatPassesItsChecks() {
        val area = Library.all().filter { it.area == BodyArea.CHEST }.map { it.name }
        assertEquals(30, area.size)
        assertEquals(area.toSet(), BATCH.toSet())
        for (name in area) assertEquals(emptyList(), FigureChecks.check(name, figure(name)), name)
        // Views: the frontal-plane flys and the archer push-up from the front, the rest from the side.
        val front = setOf("Cable crossover", "Low-to-high cable fly", "Seated cable fly", "Pec deck", "Archer push-up")
        for (name in area) assertEquals(if (name in front) View.FRONT else View.SIDE, figure(name).view, name)
    }

    // ---- presses on a bench ----

    @Test
    fun dbPress_sidesOfTheChest_thenStraightOverTheShoulders() {
        val n = "DB press"
        assertTrue(props(n).count { it is Dumbbell } == 2 && FlatBench() in props(n))
        for (p in listOf(BodyPoint.FEET to Surface.FLOOR, BodyPoint.HEAD to Surface.BENCH, BodyPoint.UPPER_BACK to Surface.BENCH, BodyPoint.HIPS to Surface.BENCH)) {
            assertContact(n, p.first, p.second)
        }
        val chest = s(n, Which.START).point(BodyPoint.CHEST)
        val db = held(n, Which.START)
        inRange(n, "dumbbell beside the chest", (db - chest).length(), 0.0..8.0)
        // The forearm upright under the dumbbell: the elbow straight below it.
        val e = s(n, Which.START).near
        inRange(n, "forearm off upright", abs(e.wrist.x - e.elbow.x), 0.0..0.5)
        inRange(n, "elbow below the grip", e.elbow.y - e.grip.y, 10.0..30.0)
        assertCheck(n, LockoutOverShoulders(Which.END))
        inRange(n, "lockout over the shoulder", abs(held(n, Which.END).x - s(n, Which.END).shoulder.x), 0.0..1.0)
    }

    @Test
    fun inclineBenchPress_45degrees_inARack_barToTheChest_overTheUpperChest() {
        val n = "Incline bench press"
        val bench = props(n).filterIsInstance<AdjustableBench>().single()
        assertEquals(45.0, bench.backAngle)
        assertEquals(135.0, figure(n).start.pose.torso)
        val rack = props(n).filterIsInstance<PowerRack>().single()
        assertCheck(n, BarTouches(BodyPoint.CHEST, Which.START))
        // The pins under the bar's lowest point, 5 cm down; the J-hooks under its top.
        val low = held(n, Which.START)
        inRange(n, "pins under the bar", -low.y - Barbell.SHAFT_RADIUS - rack.safetyHeight, 4.0..6.0)
        val top = held(n, Which.END)
        inRange(n, "over the upper chest", abs(top.x - s(n, Which.END).point(BodyPoint.UPPER_CHEST).x), 0.0..2.0)
        inRange(n, "straight arms", bend(n, Which.END), 0.0..1.0)
        inRange(n, "bar above the head", s(n, Which.END).head.y - top.y, 20.0..80.0)
        assertContact(n, BodyPoint.UPPER_BACK, Surface.BACK_PAD)
        assertContact(n, BodyPoint.HIPS, Surface.SEAT)
    }

    @Test
    fun declineBenchPress_25degreesHeadDown_feetUnderTheBrace_inARack() {
        val n = "Decline bench press"
        val bench = props(n).filterIsInstance<DeclineBench>().single()
        assertEquals(25.0, bench.angle)
        val st = s(n, Which.START)
        inRange(n, "head below the hips", st.head.y - st.pelvis.y, 25.0..40.0)
        assertContact(n, BodyPoint.SHINS, Surface.LEG_BRACE)
        assertTrue(props(n).any { it is PowerRack })
        assertCheck(n, BarTouches(BodyPoint.CHEST, Which.START))
        inRange(n, "over the chest", abs(held(n, Which.END).x - s(n, Which.END).point(BodyPoint.CHEST).x), 0.0..2.0)
        inRange(n, "straight arms", bend(n, Which.END), 0.0..1.0)
        // The feet hang clear of the floor, hooked under the roller.
        inRange(n, "feet off the floor", -maxOf(st.near.toe.y, st.near.heel.y), 25.0..80.0)
    }

    @Test
    fun inclineDbPress_sidesOfTheUpperChest_thenOverTheShoulders() {
        val n = "Incline DB press"
        assertEquals(45.0, props(n).filterIsInstance<AdjustableBench>().single().backAngle)
        inRange(n, "beside the upper chest", (held(n, Which.START) - s(n, Which.START).point(BodyPoint.UPPER_CHEST)).length(), 0.0..8.0)
        val e = s(n, Which.START).near
        inRange(n, "forearm upright", abs(e.wrist.x - e.elbow.x), 0.0..0.5)
        assertCheck(n, LockoutOverShoulders(Which.END))
        inRange(n, "straight up", abs(held(n, Which.END).x - s(n, Which.END).shoulder.x), 0.0..1.0)
        assertContact(n, BodyPoint.HEAD, Surface.BACK_PAD)
    }

    @Test
    fun declineDbPress_sidesOfTheChest_forearmUnder_thenOverTheShoulders() {
        val n = "Decline DB press"
        assertTrue(props(n).any { it is DeclineBench })
        assertContact(n, BodyPoint.SHINS, Surface.LEG_BRACE)
        inRange(n, "beside the chest", (held(n, Which.START) - s(n, Which.START).point(BodyPoint.CHEST)).length(), 0.0..8.0)
        val e = s(n, Which.START).near
        inRange(n, "each forearm under its dumbbell", abs(e.wrist.x - e.elbow.x), 0.0..0.5)
        inRange(n, "over the shoulders", abs(held(n, Which.END).x - s(n, Which.END).shoulder.x), 0.0..1.0)
        inRange(n, "straight", bend(n, Which.END), 0.0..1.0)
    }

    // ---- flys on a bench ----

    /** A lying fly's two ends: over [over] with the arms nearly straight, then lowered to [level]'s height. */
    private fun assertLyingFly(n: String, top: Which, over: BodyPoint, level: BodyPoint) {
        val bottom = if (top == Which.START) Which.END else Which.START
        val t = s(n, top)
        inRange(n, "over the point", abs(held(n, top).x - t.point(over).x), 0.0..3.0)
        inRange(n, "high over the body", t.point(over).y - held(n, top).y, 40.0..75.0)
        val b = s(n, bottom)
        inRange(n, "level at the bottom", abs(held(n, bottom).y - b.point(level).y), 0.0..3.0)
        // Out to the side: the arm drawn short (it points at the camera).
        inRange(n, "arm out to the side", (b.near.grip - b.near.shoulder).length(), 0.0..25.0)
        for (w in Which.entries) inRange(n, "fixed slight bend", bend(n, w), 10.0..30.0)
        assertEquals(bend(n, Which.START), bend(n, Which.END), 0.5)
        assertCheck(n, ElbowBend(10.0, 30.0, Which.START))
        assertCheck(n, ElbowBend(10.0, 30.0, Which.END))
    }

    /** The flys' dumbbells are drawn in profile (neutral grip), both panels; the presses' end-on. */
    @Test
    fun lyingDbFlys_dumbbellsInProfile_pressesEndOn() {
        for (n in listOf("DB fly", "Incline DB fly", "Decline DB fly")) for (w in Which.entries) {
            val bells = props(n, w).filterIsInstance<Dumbbell>()
            assertTrue(bells.size == 2 && bells.all { it.neutral }, "$n at $w")
        }
        for (n in listOf("DB press", "Incline DB press", "Decline DB press")) {
            assertTrue(props(n).filterIsInstance<Dumbbell>().none { it.neutral }, n)
        }
    }

    /** The bench cable flys draw their cable in front, from the low pulley to the hand, in both panels. */
    @Test
    fun benchCableFlys_cableFromThePulleyToTheHand() {
        for (n in listOf("Lying cable fly", "Incline bench cable fly")) for (w in Which.entries) {
            val stack = props(n, w).filterIsInstance<CableStack>().single()
            assertTrue(stack.cableInFront, n)
            val grip = s(n, w).near.grip
            val cables = FigureRenderer.world(figure(n).scene(w), View.SIDE).map { it.shape }
                .filterIsInstance<Capsule>().filter { it.role == Role.EQUIPMENT && it.width == CableStack.FRONT_CABLE }
            assertEquals(1, cables.size, "$n at $w")
            assertEquals(grip, Point(cables.single().x2, cables.single().y2))
            assertEquals(-stack.pulleyHeight, cables.single().y1, 1e-9)
        }
    }

    @Test
    fun dbFly_overTheChest_downToChestLevel_bendFixed() {
        assertLyingFly("DB fly", Which.START, BodyPoint.CHEST, BodyPoint.CHEST)
        // Not below the shoulders: the dumbbell's centre at or above the shoulder joint's height.
        assertTrue(held("DB fly", Which.END).y <= s("DB fly", Which.END).shoulder.y)
    }

    @Test
    fun inclineDbFly_overTheUpperChest_downToShoulderLevel() {
        assertLyingFly("Incline DB fly", Which.START, BodyPoint.UPPER_CHEST, BodyPoint.SHOULDERS)
        assertEquals(45.0, props("Incline DB fly").filterIsInstance<AdjustableBench>().single().backAngle)
    }

    @Test
    fun declineDbFly_overTheUpperAbdomen_notTheFace_downToShoulderLevel() {
        val n = "Decline DB fly"
        assertLyingFly(n, Which.START, BodyPoint.UPPER_ABDOMEN, BodyPoint.SHOULDERS)
        // Over the abdomen means well clear of the face, towards the feet.
        inRange(n, "dumbbells past the face", held(n, Which.START).x - s(n, Which.START).head.x, 30.0..100.0)
        assertContact(n, BodyPoint.SHINS, Surface.LEG_BRACE)
    }

    @Test
    fun lyingCableFly_lowPulleys_outLevelWithShoulders_thenTogetherOverTheChest() {
        val n = "Lying cable fly"
        assertLyingFly(n, Which.END, BodyPoint.CHEST, BodyPoint.SHOULDERS)
        val stack = props(n).filterIsInstance<CableStack>().single()
        inRange(n, "low pulley", stack.pulleyHeight, 0.0..30.0)
        assertTrue(CableHandles in props(n))
    }

    @Test
    fun inclineBenchCableFly_armsStraightUp_thenLevelWithShoulders() {
        val n = "Incline bench cable fly"
        assertLyingFly(n, Which.START, BodyPoint.SHOULDERS, BodyPoint.SHOULDERS)
        // "Arms point straight up": the grip straight over the shoulder, an arm's length up.
        inRange(n, "straight up", angle(s(n, Which.START).near.grip - s(n, Which.START).shoulder), 85.0..95.0)
        inRange(n, "low pulley", props(n).filterIsInstance<CableStack>().single().pulleyHeight, 0.0..30.0)
    }

    // ---- Smith machine ----

    /** A Smith press: the bar on the rails in both panels, on the chest at START, pressed straight up, the stops under it. */
    private fun assertSmith(n: String) {
        run {
            val smith = props(n).filterIsInstance<SmithMachine>().single()
            assertCheck(n, BarTouches(BodyPoint.CHEST, Which.START))
            assertCheck(n, BarOnRails(Which.START))
            assertCheck(n, BarOnRails(Which.END))
            for (w in Which.entries) inRange(n, "bar on the rails", abs(held(n, w).x - smith.x), 0.0..0.5)
            inRange(n, "straight arms", bend(n, Which.END), 0.0..1.0)
            // The stops just under the bar on the chest: it can't come down below the chest.
            val low = held(n, Which.START)
            inRange(n, "stops under the bar", -low.y - Barbell.SHAFT_RADIUS - smith.stopHeight, 1.5..2.5)
            inRange(n, "pressed up", low.y - held(n, Which.END).y, 30.0..70.0)
        }
    }

    @Test
    fun smithBenchPress_flatBench_barOnTheRails_chestThenStraightUp() {
        assertSmith("Smith bench press")
        assertTrue(props("Smith bench press").any { it is FlatBench })
        assertContact("Smith bench press", BodyPoint.HIPS, Surface.BENCH)
    }

    @Test
    fun smithInclinePress_inclineBench_barOnTheRails_chestThenStraightUp() {
        assertSmith("Smith incline press")
        assertEquals(45.0, props("Smith incline press").filterIsInstance<AdjustableBench>().single().backAngle)
    }

    @Test
    fun smithDeclinePress_declineBench_feetUnderTheBrace_barOnTheRails() {
        assertSmith("Smith decline press")
        assertTrue(props("Smith decline press").any { it is DeclineBench })
        assertContact("Smith decline press", BodyPoint.SHINS, Surface.LEG_BRACE)
    }

    // ---- machines ----

    /** A seated machine press: seated, back on the pad, pressed straight but soft, on a lever that keeps its length. */
    private fun assertMachine(n: String) {
        run {
            assertContact(n, BodyPoint.HIPS, Surface.SEAT)
            assertContact(n, BodyPoint.UPPER_BACK, Surface.BACK_PAD)
            assertContact(n, BodyPoint.FEET, Surface.FLOOR)
            inRange(n, "straight, not locked", bend(n, Which.END), 5.0..15.0)
            // The lever keeps its length: the pivot is the same distance from both handle positions.
            val m = props(n).filterIsInstance<PressMachine>().single()
            val g0 = s(n, Which.START).near.grip
            val g1 = s(n, Which.END).near.grip
            assertEquals((g0 - m.pivot).length(), (g1 - m.pivot).length(), 0.01, n)
        }
    }

    @Test
    fun machineChestPress_handlesLevelWithMidChest_pressedLevel() {
        val n = "Machine chest press"
        assertMachine(n)
        val chest = s(n, Which.START).point(BodyPoint.CHEST)
        inRange(n, "level with mid chest", abs(s(n, Which.START).near.grip.y - chest.y), 0.0..2.0)
        inRange(n, "pressed level", abs(angle(s(n, Which.END).near.grip - s(n, Which.START).near.grip)), 0.0..1.0)
    }

    @Test
    fun inclineMachinePress_handlesJustBelowTheUpperChest_pressedUpAndForward() {
        val n = "Incline machine press"
        assertMachine(n)
        val upper = s(n, Which.START).point(BodyPoint.UPPER_CHEST)
        inRange(n, "upper chest just above the handles", s(n, Which.START).near.grip.y - upper.y, 1.0..6.0)
        inRange(n, "pressed up and forward", angle(s(n, Which.END).near.grip - s(n, Which.START).near.grip), 30.0..40.0)
    }

    @Test
    fun declineMachinePress_handlesAtTheLowerChest_elbowsJustBelowShoulders_pressedForwardAndDown() {
        val n = "Decline machine press"
        assertMachine(n)
        val lower = s(n, Which.START).point(BodyPoint.CHEST)
        inRange(n, "about level with the lower chest", s(n, Which.START).near.grip.y - lower.y, 0.0..5.0)
        inRange(n, "pressed forward and down", angle(s(n, Which.END).near.grip - s(n, Which.START).near.grip), -25.0..-15.0)
        val d = s(n, Which.START).near
        inRange(n, "elbow just below the shoulder", d.elbow.y - d.shoulder.y, 0.0..20.0)
    }

    // ---- front view: standing and seated flys ----

    /** Front-view fly: out wide at START, together in front at END, the bend fixed. */
    private fun assertFrontFly(n: String) {
        val st = s(n, Which.START)
        inRange(n, "arms out wide", st.near.grip.x - st.far.grip.x, 100.0..170.0)
        val e = s(n, Which.END)
        inRange(n, "hands together", abs(e.near.grip.x - e.far.grip.x), 0.0..14.0)
        for (w in Which.entries) inRange(n, "slight bend", bend(n, w), 10.0..30.0)
        assertEquals(bend(n, Which.START), bend(n, Which.END), 3.0, n)
        // The arms point at the camera when the hands meet in front: drawn shorter.
        inRange(n, "hands in front", (e.near.grip - e.near.shoulder).length(), 0.0..40.0)
        assertCheck(n, HandsTogether(if (n == "Pec deck") 14.0 else 12.0, Which.END))
    }

    @Test
    fun cableCrossover_highPulleys_armsOut_thenHandsTogetherLowInFront() {
        val n = "Cable crossover"
        assertFrontFly(n)
        val stack = props(n).filterIsInstance<CableStack>().single()
        inRange(n, "high pulleys", stack.pulleyHeight, 190.0..220.0)
        assertTrue(stack.halfSpan > 0.0)
        // High to low: the hands meet below the chest touch point's height, and start above where they meet.
        val e = s(n, Which.END)
        inRange(n, "hands low in front", e.near.grip.y - (e.near.shoulder.y + Body.CHEST_BELOW_SHOULDER), 0.0..15.0)
        assertTrue(s(n, Which.START).near.grip.y < e.near.grip.y - 20.0)
        // Hips and knees slightly bent.
        inRange(n, "slight forward lean", figure(n).start.pose.lean, 3.0..12.0)
        assertTrue(figure(n).start.pose.nearLeg.out in 5.0..20.0)
    }

    @Test
    fun lowToHighFly_lowPulleys_armsDownAndOut_thenMeetAtFaceHeight() {
        val n = "Low-to-high cable fly"
        assertFrontFly(n)
        val stack = props(n).filterIsInstance<CableStack>().single()
        // Between knee and hip height.
        val st = s(n, Which.START)
        assertTrue(-stack.pulleyHeight in st.near.hip.y..st.near.knee.y, "pulley between knee and hip")
        inRange(n, "arms down", st.near.grip.y - st.near.shoulder.y, 25.0..50.0)
        inRange(n, "meet at face height", abs(s(n, Which.END).near.grip.y - s(n, Which.END).head.y), 0.0..6.0)
    }

    @Test
    fun seatedCableFly_seated_elbowsHigh_handsTogether() {
        val n = "Seated cable fly"
        assertFrontFly(n)
        assertContact(n, BodyPoint.HIPS, Surface.SEAT)
        for (w in Which.entries) {
            val x = s(n, w)
            inRange(n, "elbows high", abs(x.near.elbow.y - x.near.shoulder.y), 0.0..8.0)
            inRange(n, "hands at shoulder height", abs(x.near.grip.y - x.near.shoulder.y), 0.0..6.0)
        }
        inRange(n, "pulleys at the shoulders", abs(props(n).filterIsInstance<CableStack>().single().pulleyHeight + s(n, Which.START).near.shoulder.y), 0.0..1.0)
    }

    @Test
    fun pecDeck_seated_handlesAtShoulderHeight_forwardAndTogether() {
        val n = "Pec deck"
        assertFrontFly(n)
        assertContact(n, BodyPoint.HIPS, Surface.SEAT)
        for (w in Which.entries) inRange(n, "handles at shoulder height", abs(s(n, w).near.grip.y - s(n, w).near.shoulder.y), 0.0..4.0)
        val m = props(n).filterIsInstance<PecDeck>().single()
        // The arms hang from pivots above the head; the back pad stands up behind the shoulders.
        assertTrue(-m.pivotHeight < s(n, Which.START).head.y - Body.HEAD_RADIUS)
        assertTrue(-m.padTop < s(n, Which.START).near.shoulder.y)
    }

    @Test
    fun cableChestPress_splitStance_besideTheChest_thenStraightAndLevel() {
        val n = "Cable chest press"
        val st = s(n, Which.START)
        // Split stance: one foot in front of the hips, one behind.
        assertTrue(st.near.ankle.x > st.pelvis.x + 20.0 && st.far.ankle.x < st.pelvis.x - 20.0)
        inRange(n, "beside the chest", (held(n, Which.START) - st.point(BodyPoint.CHEST)).length(), 0.0..8.0)
        val e = s(n, Which.END)
        inRange(n, "straight", bend(n, Which.END), 0.0..1.0)
        inRange(n, "level", abs(e.near.grip.y - e.shoulder.y), 0.0..2.0)
        val stack = props(n).filterIsInstance<CableStack>().single()
        inRange(n, "pulleys at shoulder height", abs(stack.pulleyHeight + st.shoulder.y), 0.0..1.0)
        assertTrue(stack.x < st.pelvis.x - 40.0, "the machine behind")
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
    }

    // ---- dips ----

    /** A dip: straight arms with the shoulders over the hands, down until the upper arms are level, leaning forward. */
    private fun assertDip(n: String) {
        run {
            assertContact(n, BodyPoint.HANDS, Surface.DIP_BAR)
            val st = s(n, Which.START)
            inRange(n, "shoulders over the hands", abs(st.shoulder.x - st.near.grip.x), 0.0..1.0)
            inRange(n, "straight arms", bend(n, Which.START), 0.0..1.0)
            val e = s(n, Which.END)
            val v = e.near.elbow - e.near.shoulder
            inRange(n, "upper arm level", abs(angle(v).let { if (it > 90) 180 - it else if (it < -90) -180 - it else it }), 0.0..2.0)
            inRange(n, "elbow at 90", bend(n, Which.END), 85.0..95.0)
            inRange(n, "forward lean", 90.0 - figure(n).end.pose.torso, 30.0..37.0)
            // The hands stay on the bars.
            assertEquals(st.near.grip.y, e.near.grip.y, 0.5)
            assertTrue(figure(n).end.pose.nearArm.out > 0.0, "$n: elbows flare out")
        }
    }

    @Test
    fun chestDip_parallelBars_straightArms_thenUpperArmsLevel() {
        assertDip("Chest dip")
        assertTrue(props("Chest dip").any { it is DipBars })
        val dip = s("Chest dip", Which.END)
        inRange("Chest dip", "feet off the floor", -maxOf(dip.near.toe.y, dip.near.heel.y), 10.0..100.0)
        // Knees and hips slightly bent.
        assertTrue(figure("Chest dip").start.pose.nearLeg.knee in 20.0..60.0 && figure("Chest dip").start.pose.nearLeg.hip in 5.0..40.0)
    }

    @Test
    fun assistedDip_onTheAssistStep_straightArms_thenUpperArmsLevel() {
        assertDip("Assisted dip")
        assertContact("Assisted dip", BodyPoint.FEET, Surface.ASSIST_STEP)
        // The step rides down with the body, on a lever that keeps its length.
        val m0 = props("Assisted dip", Which.START).filterIsInstance<AssistedDipMachine>().single()
        val m1 = props("Assisted dip", Which.END).filterIsInstance<AssistedDipMachine>().single()
        assertTrue(m1.stepHeight < m0.stepHeight - 10.0)
        assertEquals((Point(m0.stepX, -m0.stepHeight) - m0.pivot).length(), (Point(m1.stepX, -m1.stepHeight) - m1.pivot).length(), 0.01)
        // Hips and knees bent on the step.
        assertTrue(figure("Assisted dip").start.pose.nearLeg.knee > 60.0)
    }

    // ---- push-ups ----

    private fun handsOnFloor(n: String, which: Which) {
        val x = s(n, which)
        val low = minOf(-maxOf(x.near.wrist.y, x.near.hand.y), -maxOf(x.far.wrist.y, x.far.hand.y)) - Body.HAND_WIDTH / 2
        inRange(n, "hands on the floor at $which", low, -0.6..1.5)
    }

    /** The body in one straight line: legs in line with the torso. */
    private fun straight(n: String, which: Which) {
        val leg = figure(n).scene(which).pose.nearLeg
        assertEquals(0.0, leg.hip, 0.01, n)
        assertEquals(0.0, leg.knee, 0.01, n)
    }

    @Test
    fun pushUp_straightArmsBodyStraight_thenChestTowardTheFloor() {
        val n = "Push-up"
        for (w in Which.entries) { straight(n, w); handsOnFloor(n, w) }
        val st = s(n, Which.START)
        inRange(n, "hands under the shoulders", abs(st.near.wrist.x - st.shoulder.x), 0.0..1.0)
        inRange(n, "straight arms", bend(n, Which.START), 0.0..1.0)
        inRange(n, "chest near the floor", -s(n, Which.END).chest().y, 4.0..9.0)
        inRange(n, "elbows bent at the bottom", bend(n, Which.END), 90.0..145.0)
        // The hands and toes stay put.
        assertEquals(st.near.wrist.x, s(n, Which.END).near.wrist.x, 0.5)
        assertEquals(st.near.toe.x, s(n, Which.END).near.toe.x, 1.5)
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
    }

    @Test
    fun inclinePushUp_handsOnTheBench_chestToTheEdge() {
        val n = "Incline push-up"
        val bench = props(n).filterIsInstance<FlatBench>().single()
        assertContact(n, BodyPoint.HANDS, Surface.BENCH)
        assertContact(n, BodyPoint.CHEST, Surface.BENCH, setOf(Which.END))
        for (w in Which.entries) straight(n, w)
        inRange(n, "body inclined", figure(n).start.pose.torso, 20.0..45.0)
        // Arms square to the body at the top, the hands flat on the bench.
        val st = s(n, Which.START)
        inRange(n, "arms square to the body", abs(angle(st.near.wrist - st.shoulder) - (figure(n).start.pose.torso - 90.0)), 0.0..1.0)
        inRange(n, "hand flat", abs(angle(st.near.hand - st.near.wrist)), 0.0..0.5)
        // The chest at the bench's near top edge.
        val c = s(n, Which.END).chest()
        inRange(n, "chest at the edge", (c - Point(bench.x, -bench.height)).length(), 0.0..1.0)
    }

    @Test
    fun declinePushUp_feetOnTheBench_allTheWayDown_headTippedBack() {
        val n = "Decline push-up"
        assertContact(n, BodyPoint.FEET, Surface.BENCH)
        assertContact(n, BodyPoint.HANDS, Surface.FLOOR)
        // The feet up: the body slopes down towards the head as it lowers, the face below the feet.
        assertTrue(figure(n).end.pose.torso < figure(n).start.pose.torso - 10.0, "lowered head-first")
        val e = s(n, Which.END)
        assertTrue(e.head.y > e.near.toe.y + 15.0, "head below the feet")
        inRange(n, "all the way down", -e.chest().y, 4.0..9.0)
        // The head tipped back keeps the face clear of the floor.
        inRange(n, "face clear of the floor", -(e.head.y + Body.HEAD_RADIUS), 3.0..30.0)
        assertTrue(figure(n).end.pose.neck < -10.0, "head tipped back")
        assertEquals(0.0, figure(n).start.pose.neck)
    }

    @Test
    fun kneePushUp_onTheKnees_straightKneesToHead() {
        val n = "Knee push-up"
        assertContact(n, BodyPoint.KNEES, Surface.FLOOR)
        for (w in Which.entries) {
            assertEquals(0.0, figure(n).scene(w).pose.nearLeg.hip, 0.01) // straight from the knees to the head
            handsOnFloor(n, w)
        }
        inRange(n, "straight arms", bend(n, Which.START), 0.0..1.0)
        inRange(n, "chest toward the floor", -s(n, Which.END).chest().y, 4.0..9.0)
        // The body pivots at the knees: the lower legs lie back along the floor and stay put.
        for (w in Which.entries) {
            val l = s(n, w).near
            inRange(n, "shin along the floor", angle(l.ankle - l.knee), 170.0..175.0)
            inRange(n, "feet just clear of the floor", -maxOf(l.toe.y, l.heel.y) - Body.FOOT_WIDTH / 2, 0.0..10.0)
        }
    }

    @Test
    fun archerPushUp_headOn_legsHidden_oneArmOut_thenPressedStraight() {
        val n = "Archer push-up"
        val st = s(n, Which.START)
        // START: the far arm reaches far out along the floor, nearly straight; the near hand under the chest by the
        // ribs, its elbow bent; the shoulders low and level.
        inRange(n, "far hand out to the side", st.far.shoulder.x - st.far.wrist.x, 45.0..70.0)
        inRange(n, "far arm nearly straight", bend(n, Which.START, Side.FAR), 10.0..30.0)
        inRange(n, "near hand under the chest", st.near.shoulder.x - st.near.wrist.x, 0.0..15.0)
        inRange(n, "near elbow bent", bend(n, Which.START, Side.NEAR), 90.0..150.0)
        inRange(n, "chest low", -st.near.shoulder.y, 20.0..35.0)
        // END: the near arm straight and close to upright under its shoulder, the far arm straight and long, sloping.
        val e = s(n, Which.END)
        inRange(n, "near arm straight", bend(n, Which.END, Side.NEAR), 0.0..5.0)
        inRange(n, "far arm straight", bend(n, Which.END, Side.FAR), 0.0..5.0)
        val nearArm = e.near.wrist - e.near.shoulder
        inRange(n, "near arm off vertical", abs(atan2(nearArm.x, nearArm.y) * 180 / PI), 0.0..15.0)
        val farArm = e.far.wrist - e.far.shoulder
        inRange(n, "far arm sloping out", abs(atan2(farArm.x, farArm.y) * 180 / PI), 30.0..60.0)
        assertTrue(e.near.shoulder.y < st.near.shoulder.y - 20.0, "pressed up")
        // The hands stay put.
        assertEquals(st.near.wrist.x, e.near.wrist.x, 0.5)
        assertEquals(st.far.wrist.x, e.far.wrist.x, 0.5)
        for (w in Which.entries) handsOnFloor(n, w)
        // The legs are hidden behind the shoulders: nothing is drawn of them, and every body shape stays within the
        // torso's and head's outline or is an arm (no leg below or beside the torso).
        for (w in Which.entries) {
            val pose = figure(n).scene(w).pose
            assertTrue(FigureRenderer.legHidden(pose.nearLeg) && FigureRenderer.legHidden(pose.farLeg), "legs point away at $w")
            val shapes = FigureRenderer.shapes(figure(n), w).filter { it.role == Role.BODY }
            // Torso, neck, head and two arms of three segments: nothing else.
            assertEquals(1, shapes.count { it is Polygon })
            assertEquals(1, shapes.count { it is Circle })
            assertEquals(1 + 6, shapes.count { it is Capsule }, "neck and arms only at $w")
            // The torso is short (seen sloping away), under the head.
            val torso = shapes.filterIsInstance<Polygon>().single().bounds()
            val head = shapes.filterIsInstance<Circle>().single()
            assertTrue(pose.lean >= 70.0, "sloping away at $w")
            val sk = pose.skeleton(View.FRONT)
            inRange(n, "torso drawn short", (sk.shoulder - sk.pelvis).length(), 8.0..16.0)
            assertTrue(torso.minY < head.cy + head.r, "torso behind and below the head at $w")
        }
    }

    @Test
    fun clapPushUp_chestDown_thenHandsLeaveTheFloor() {
        val n = "Clap push-up"
        handsOnFloor(n, Which.START)
        inRange(n, "chest toward the floor", -s(n, Which.START).chest().y, 4.0..9.0)
        val e = s(n, Which.END)
        inRange(n, "hands in the air", -maxOf(e.near.wrist.y, e.near.hand.y) - Body.HAND_WIDTH / 2, 6.0..10.0)
        straight(n, Which.END)
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        assertTrue(figure(n).contacts.none { it.point == BodyPoint.HANDS && Which.END in it.at })
    }

    @Test
    fun weightedPushUp_platesOnTheBack_thePushUpsPoses() {
        val n = "Weighted push-up"
        for (w in Which.entries) {
            assertTrue(BackPlates() in props(n, w))
            assertEquals(figure("Push-up").scene(w).pose, figure(n).scene(w).pose)
            // The plates lie on the back: their centre behind the torso's back surface by half the stack.
            val x = s(n, w)
            val c = BackPlates().center(x)
            val back = x.onTorso(Segment.TORSO.length * BackPlates.ALONG, -Body.TORSO_RADIUS)
            assertEquals(BackPlates.THICKNESS, (c - back).length(), 0.01)
        }
    }

    private companion object {
        val BATCH = listOf(
            "DB press", "Incline bench press", "Decline bench press", "Incline DB press", "Decline DB press", "DB fly",
            "Incline DB fly", "Decline DB fly", "Machine chest press", "Incline machine press", "Decline machine press", "Pec deck",
            "Smith bench press", "Smith incline press", "Smith decline press", "Cable crossover", "Low-to-high cable fly",
            "Cable chest press", "Lying cable fly", "Incline bench cable fly", "Seated cable fly", "Chest dip", "Assisted dip",
            "Push-up", "Incline push-up", "Decline push-up", "Knee push-up", "Archer push-up", "Clap push-up", "Weighted push-up",
        )
    }
}
