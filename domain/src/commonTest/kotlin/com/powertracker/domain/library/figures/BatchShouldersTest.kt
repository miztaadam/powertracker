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

/** The shoulder batch (Task 8): every shoulder exercise, each with the key positions its guide gives. */
class BatchShouldersTest {
    private fun figure(name: String): ExerciseFigure = assertNotNull(Figures.all[name], "no figure for $name")

    private fun s(name: String, which: Which) = figure(name).let { it.scene(which).pose.skeleton(it.view) }

    private fun pose(name: String, which: Which) = figure(name).scene(which).pose

    private fun props(name: String, which: Which = Which.START) = figure(name).scene(which).props

    private fun assertContact(name: String, point: BodyPoint, surface: Surface) =
        assertTrue(figure(name).contacts.any { it.point == point && it.surface == surface && it.at == Which.entries.toSet() }, "$name: $point on $surface")

    private fun assertCheck(name: String, check: KeyCheck) = assertTrue(check in figure(name).checks, "$name: $check in ${figure(name).checks}")

    private fun inRange(name: String, label: String, value: Double, range: ClosedFloatingPointRange<Double>) =
        assertTrue(value in range, "$name: $label ${round1(value)} not in $range")

    private fun bend(name: String, which: Which, side: Side = Side.NEAR) =
        pose(name, which).let { JointLimits.elbowBend(if (side == Side.NEAR) it.nearArm else it.farArm) }

    private fun angle(v: Point) = atan2(-v.y, v.x) * 180 / PI

    private fun lockout(name: String) = assertCheck(name, LockoutOverShoulders(Which.END))

    @Test
    fun shoulders_everyExerciseHasAFigure_thatPassesItsChecks() {
        val area = Library.all().filter { it.area == BodyArea.SHOULDERS }.map { it.name }
        assertEquals(24, area.size)
        val problems = area.flatMap { name -> Figures.all[name]?.let { FigureChecks.check(name, it) } ?: listOf(FigureProblem(name, null, "no figure")) }
        assertEquals(emptyList(), problems.map { "${it.name} ${it.which}: ${it.message}" })
        val front = setOf(
            "Arnold press", "Lateral raise", "Seated lateral raise", "Cable lateral raise", "Machine lateral raise", "Rear delt raise",
            "Seated rear delt raise", "Chest-supported rear delt raise", "Machine reverse fly", "Cable reverse fly",
            "Cable external rotation", "Side-lying external rotation", "Cable internal rotation",
        )
        for (name in area) assertEquals(if (name in front) View.FRONT else View.SIDE, figure(name).view, name)
    }

    // ---- presses ----

    private fun assertSeatedOnTheBench(n: String) {
        assertContact(n, BodyPoint.HIPS, Surface.SEAT)
        assertContact(n, BodyPoint.UPPER_BACK, Surface.BACK_PAD)
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
    }

    @Test
    fun seatedBarbellPress_chinHeightInFrontOfTheFace_elbowsForward_lockout_inTheRackWithPins() {
        val n = "Seated barbell press"
        assertSeatedOnTheBench(n)
        val st = s(n, Which.START)
        // About chin height (the chin is 9 cm above the shoulder joint), clear in front of the face.
        inRange(n, "bar above the shoulder joint", st.shoulder.y - st.near.grip.y, 4.0..12.0)
        inRange(n, "bar clear of the face", st.near.grip.x - st.head.x - Body.HEAD_RADIUS - Barbell.SHAFT_RADIUS, 0.0..4.0)
        // Elbows forward and down under the bar (never back or up: a wrong-way elbow).
        inRange(n, "elbow in front of the shoulder", st.near.elbow.x - st.shoulder.x, 5.0..20.0)
        inRange(n, "elbow below the shoulder", st.near.elbow.y - st.shoulder.y, 10.0..33.0)
        inRange(n, "upper arm out of the plane", pose(n, Which.START).nearArm.out, 0.0..45.0)
        lockout(n)
        val rack = props(n).filterIsInstance<PowerRack>().single()
        inRange(n, "pins under the bar's start", -st.near.grip.y - rack.safetyHeight, 5.0..15.0)
    }

    @Test
    fun smithShoulderPress_barOnTheRails_clearOfTheHead_stopsAtTheShoulders() {
        val n = "Smith shoulder press"
        assertSeatedOnTheBench(n)
        assertCheck(n, BarOnRails(Which.START))
        assertCheck(n, BarOnRails(Which.END))
        val smith = props(n).filterIsInstance<SmithMachine>().single()
        for (w in Which.entries) {
            val sk = s(n, w)
            // The rail passes just in front of the head (pulled back against the pad): the bar never touches it.
            inRange(n, "bar clear of the head ($w)", smith.x - sk.head.x - Body.HEAD_RADIUS - Barbell.SHAFT_RADIUS, 0.3..3.0)
        }
        val st = s(n, Which.START)
        inRange(n, "bar at the front of the shoulders", abs(st.near.grip.y - st.shoulder.y), 0.0..4.0)
        inRange(n, "stops just under the bar", (-st.near.grip.y - Barbell.SHAFT_RADIUS) - smith.stopHeight, 0.0..6.0)
        inRange(n, "elbow down under the bar", st.near.elbow.y - st.shoulder.y, 15.0..33.0)
        inRange(n, "upper arm out to the side", pose(n, Which.START).nearArm.out, 5.0..60.0)
        val en = s(n, Which.END)
        inRange(n, "arms straight", bend(n, Which.END), 0.0..1.0)
        assertTrue(en.near.grip.y < en.head.y - Body.HEAD_RADIUS - 10.0, "$n: bar overhead")
    }

    @Test
    fun machineShoulderPress_handlesAtTheShoulders_pressedNearlyStraight_oneLever() {
        val n = "Machine shoulder press"
        assertSeatedOnTheBench(n)
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        inRange(n, "handles level with the shoulders or a little above", st.shoulder.y - st.near.grip.y, 0.0..12.0)
        inRange(n, "elbow below the handle", st.near.elbow.y - st.near.grip.y, 15.0..40.0)
        inRange(n, "upper arm out to the side", pose(n, Which.START).nearArm.out, 10.0..60.0)
        inRange(n, "nearly straight", bend(n, Which.END), 5.0..15.0)
        assertTrue(en.near.grip.y < en.head.y - Body.HEAD_RADIUS, "$n: pressed overhead")
        val m = props(n).filterIsInstance<PressMachine>().single()
        assertTrue(m.handleEndOn, "$n: palms forward, the handles across")
        inRange(n, "lever length kept", abs((st.near.grip - m.pivot).length() - (en.near.grip - m.pivot).length()), 0.0..0.5)
    }

    @Test
    fun oneArmDbPress_atOneShoulderElbowUnderWrist_toAStraightArm_freeArmHangs() {
        val n = "One-arm DB press"
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        assertEquals(listOf(Dumbbell(Side.NEAR)), props(n).filterIsInstance<Dumbbell>())
        val st = s(n, Which.START)
        // At the shoulder: the dumbbell just above the joint (its disc's lower edge level with it), as the DB shoulder press.
        inRange(n, "dumbbell at the shoulder", st.near.grip.y - st.shoulder.y, -20.0..-5.0)
        inRange(n, "elbow under the wrist", abs(st.near.elbow.x - st.near.wrist.x), 0.0..1.0)
        inRange(n, "elbow below the shoulder", st.near.elbow.y - st.shoulder.y, 10.0..25.0)
        inRange(n, "upper arm out to the side", pose(n, Which.START).nearArm.out, 30.0..60.0)
        assertEquals(Arm(0.0, 0.0), pose(n, Which.START).farArm)
        lockout(n)
        assertEquals(90.0, st.torso, "$n: stand tall")
    }

    @Test
    fun arnoldPress_frontView_elbowsTogetherInFront_thenArmsStraightAndApart() {
        val n = "Arnold press"
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        val st = s(n, Which.START)
        // The hands close together in front of the chin, the elbows in front of the chest, inside the shoulders.
        inRange(n, "hands close together", st.near.grip.x - st.far.grip.x, 15.0..30.0)
        inRange(n, "about chin height", st.shoulder.y - st.near.grip.y, 3.0..15.0)
        assertTrue(st.near.elbow.x < st.near.shoulder.x - 3.0, "$n: elbow inside the shoulder")
        inRange(n, "elbow under the wrist", abs(st.near.elbow.x - st.near.wrist.x), 0.0..1.0)
        assertTrue(st.near.elbow.y > st.near.grip.y + 20.0, "$n: forearm upright, elbow below")
        // The upper arm points forward, towards the camera (a wrong-way elbow, back or out to the side, fails).
        inRange(n, "upper arm forward", pose(n, Which.START).nearArm.out, 25.0..60.0)
        val en = s(n, Which.END)
        inRange(n, "arms straight", bend(n, Which.END), 0.0..1.0)
        inRange(n, "hands about shoulder width apart", en.near.grip.x - en.far.grip.x, 45.0..70.0)
        assertTrue(en.near.grip.y < en.head.y - Body.HEAD_RADIUS - 20.0, "$n: overhead")
        // Palms facing you, then away: the handles run across the body, so the front view sees them in profile.
        for (w in Which.entries) assertTrue(props(n, w).filterIsInstance<Dumbbell>().let { d -> d.size == 2 && d.none { it.endOnFront } }, "$n: profile")
    }

    @Test
    fun kettlebellPress_rackedInFrontOfTheChest_elbowByTheRibs_thenStraightOverhead() {
        val n = "Kettlebell press"
        assertEquals(Kettlebell(Side.NEAR, KettlebellHold.RACK), props(n, Which.START).filterIsInstance<Kettlebell>().single())
        assertEquals(Kettlebell(Side.NEAR, KettlebellHold.OVERHEAD), props(n, Which.END).filterIsInstance<Kettlebell>().single())
        val st = s(n, Which.START)
        inRange(n, "the kettlebell side's foot in front", st.near.ankle.x - st.far.ankle.x, 5.0..20.0)
        inRange(n, "near the shoulder, in front of the chest", st.near.grip.x - st.shoulder.x, 10.0..25.0)
        inRange(n, "at the shoulder's height", abs(st.near.grip.y - st.shoulder.y), 0.0..10.0)
        // The elbow down by the ribs: the upper arm close along the torso, forward of it.
        inRange(n, "upper arm off the torso", fold(angle(st.near.elbow - st.near.shoulder) - 270.0), 5.0..25.0)
        lockout(n)
        for (w in Which.entries) inRange(n, "knees nearly straight ($w)", pose(n, w).nearLeg.knee, 0.0..20.0)
    }

    @Test
    fun landminePress_splitStance_endAtTheShoulder_upAndForwardOnItsArc() {
        val n = "Landmine press"
        val lever = props(n).filterIsInstance<TBarLever>().single()
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        assertTrue(lever.pivot.x > st.far.toe.x + 50.0 && lever.pivot.y > -6.0, "$n: the far end on the floor in front")
        inRange(n, "bar length kept", abs((st.near.grip - lever.pivot).length() - (en.near.grip - lever.pivot).length()), 0.0..0.5)
        inRange(n, "end near the shoulder", (st.near.grip - st.shoulder).length(), 0.0..25.0)
        inRange(n, "arm straight", bend(n, Which.END), 0.0..1.0)
        assertTrue(en.near.grip.x > en.shoulder.x + 30.0 && en.near.grip.y < en.shoulder.y - 20.0, "$n: up and forward")
        // The pressing side's (near) leg back, the other forward and bent.
        assertTrue(st.near.ankle.x < st.pelvis.x - 30.0 && st.far.ankle.x > st.pelvis.x + 25.0, "$n: feet far apart")
        inRange(n, "front knee bent", pose(n, Which.START).farLeg.knee, 15.0..60.0)
        inRange(n, "back knee", pose(n, Which.START).nearLeg.knee, 0.0..40.0)
        assertTrue(lever.plateAlong < (st.near.grip - lever.pivot).length(), "$n: the plates on the held end, before the hand")
    }

    // ---- front raises ----

    @Test
    fun frontRaise_againstTheThighs_toShoulderHeight_armsStraight() {
        val n = "Front raise"
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        val st = s(n, Which.START)
        inRange(n, "dumbbell against the thigh", st.near.grip.x - (st.pelvis.x + Body.THIGH_WIDTH / 2) - Dumbbell.HEAD_RADIUS, -1.0..2.0)
        inRange(n, "arms straight", bend(n, Which.START), 0.0..1.0)
        assertCheck(n, HandsLevelWith(BodyPoint.SHOULDERS, 2.0, Which.END))
        for (w in Which.entries) assertEquals(90.0, s(n, w).torso, "$n: upright")
        assertTrue(props(n).filterIsInstance<Dumbbell>().all { !it.neutral }, "$n: palms facing the thighs: end-on")
    }

    @Test
    fun barbellFrontRaise_inFrontOfTheThighs_toShoulderHeight_nearlyStraight() {
        val n = "Barbell front raise"
        val st = s(n, Which.START)
        inRange(n, "bar just in front of the thighs", FigureChecks.barToLegs(st.near.grip, st), 0.0..2.0)
        assertCheck(n, HandsLevelWith(BodyPoint.SHOULDERS, 2.0, Which.END))
        inRange(n, "elbows nearly straight", bend(n, Which.END), 3.0..10.0)
    }

    @Test
    fun cableFrontRaise_armSlightlyBack_toShoulderHeight_cableFromTheLowPulleyBehind() {
        val n = "Cable front raise"
        val stack = props(n).filterIsInstance<CableStack>().single()
        val st = s(n, Which.START)
        assertTrue(stack.cableInFront && stack.pulleyHeight <= 30.0, "$n: a low pulley, its cable drawn")
        assertTrue(stack.x + stack.reach < st.near.heel.x - 20.0, "$n: the pulley behind")
        inRange(n, "arm slightly back", st.shoulder.x - st.near.grip.x, 5.0..25.0)
        assertCheck(n, HandsLevelWith(BodyPoint.SHOULDERS, 2.0, Which.END))
        assertEquals(Arm(0.0, 0.0), pose(n, Which.END).farArm)
    }

    // ---- lateral raises (front view) ----

    /** Arms hanging at START; elbows at shoulder height at END, the hands not above them; the bend fixed, the elbows back. */
    private fun assertLateral(n: String) {
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        inRange(n, "arms hanging", st.near.elbow.y - st.near.shoulder.y, 25.0..33.0)
        inRange(n, "elbow at shoulder height", abs(en.near.elbow.y - en.near.shoulder.y), 0.0..1.0)
        inRange(n, "hand level with the elbow, not above it", en.near.grip.y - en.near.elbow.y, -0.5..4.0)
        for (w in Which.entries) {
            inRange(n, "slight bend ($w)", bend(n, w), 10.0..30.0)
            // The upper arm out to the side (not forward), the forearm angled forward: the elbow points back.
            assertEquals(0.0, pose(n, w).nearArm.out, 1e-9, "$n: upper arm in the plane ($w)")
            inRange(n, "forearm forward ($w)", pose(n, w).nearArm.forearmOut, 10.0..30.0)
        }
    }

    @Test
    fun lateralRaise_palmsFacingTheBody_toShoulderHeight() {
        val n = "Lateral raise"
        assertLateral(n)
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        assertTrue(props(n).filterIsInstance<Dumbbell>().let { d -> d.size == 2 && d.all { it.endOnFront } }, "$n: end-on")
        val st = s(n, Which.START)
        inRange(n, "dumbbell clear of the thigh", st.near.grip.x - (st.near.knee.x + Body.SHIN_WIDTH / 2) - Dumbbell.HEAD_RADIUS, 0.0..8.0)
    }

    @Test
    fun seatedLateralRaise_onTheBenchEnd_legsTogether_toShoulderHeight() {
        val n = "Seated lateral raise"
        assertLateral(n)
        assertContact(n, BodyPoint.HIPS, Surface.SEAT)
        inRange(n, "legs together", pose(n, Which.START).nearLeg.hip, 0.0..4.0)
        val st = s(n, Which.START)
        val seat = props(n).filterIsInstance<Seat>().single()
        inRange(n, "dumbbell beside the bench", st.near.grip.x - seat.width / 2 - Dumbbell.HEAD_RADIUS, 0.0..10.0)
        assertTrue(st.near.grip.y + Dumbbell.HEAD_RADIUS < -10.0, "$n: clear of the floor")
    }

    @Test
    fun cableLateralRaise_sideOnToALowPulley_freeArmNearest_cableAcrossInFront() {
        val n = "Cable lateral raise"
        assertLateral(n)
        val stack = props(n).filterIsInstance<CableStack>().single()
        val st = s(n, Which.START)
        assertTrue(stack.cableInFront && stack.halfSpan == 0.0 && stack.pulleyHeight <= 30.0, "$n: one low pulley, the cable in front")
        assertTrue(stack.x + stack.reach < st.far.shoulder.x - 10.0, "$n: on the free arm's side")
        assertEquals(Arm(0.0, 0.0), pose(n, Which.START).farArm)
        assertEquals(listOf(CableHandle(Side.NEAR)), props(n).filterIsInstance<CableHandle>())
    }

    @Test
    fun machineLateralRaise_padsOutsideTheArms_pivotsAtTheShoulders_upperArmsToLevel() {
        val n = "Machine lateral raise"
        assertContact(n, BodyPoint.HIPS, Surface.SEAT)
        val m = props(n).filterIsInstance<LateralRaiseMachine>().single()
        for (w in Which.entries) {
            val sk = s(n, w)
            inRange(n, "pivot at the shoulder joint ($w)", (m.pivot(Side.NEAR) - sk.near.shoulder).length(), 0.0..0.5)
            inRange(n, "arm bent ($w)", bend(n, w), 85.0..95.0)
            val (a, b) = m.armPad(Side.NEAR, sk)
            val mid = (a + b) * 0.5
            if (w == Which.START) {
                inRange(n, "upper arm down", sk.near.elbow.y - sk.near.shoulder.y, 28.0..33.0)
                assertTrue(mid.x > sk.near.elbow.x + 5.0, "$n: pad outside the hanging arm")
            } else {
                inRange(n, "upper arm level", abs(sk.near.elbow.y - sk.near.shoulder.y), 0.0..1.0)
                assertTrue(mid.y < sk.near.elbow.y - 5.0, "$n: pad on top of the raised arm")
            }
        }
    }

    // ---- rear delts ----

    private fun assertRearRaise(n: String) {
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        inRange(n, "arms hanging", st.near.elbow.y - st.near.shoulder.y, 28.0..33.0)
        inRange(n, "elbow at shoulder height", abs(en.near.elbow.y - en.near.shoulder.y), 0.0..1.0)
        inRange(n, "straight out from the shoulder", en.near.elbow.x - en.near.shoulder.x, 31.0..33.0)
        for (w in Which.entries) {
            inRange(n, "slight bend ($w)", bend(n, w), 10.0..30.0)
            assertEquals(0.0, pose(n, w).nearArm.out, 1e-9, "$n: upper arm square to the torso ($w)")
            inRange(n, "forearm angled forward, towards the head ($w)", pose(n, w).nearArm.forearmOut, 10.0..40.0)
        }
        assertTrue(props(n).filterIsInstance<Dumbbell>().let { d -> d.size == 2 && d.all { it.endOnFront } }, "$n: palms facing each other")
        assertTrue(st.near.grip.y + Dumbbell.HEAD_RADIUS < 0.0, "$n: dumbbells clear of the floor")
    }

    @Test
    fun rearDeltRaise_bentCloseToLevel_kneesBent_armsOutToShoulderHeight() {
        val n = "Rear delt raise"
        assertRearRaise(n)
        assertContact(n, BodyPoint.FEET, Surface.FLOOR)
        val p = pose(n, Which.START)
        inRange(n, "torso close to level (lean)", p.lean, 65.0..75.0)
        inRange(n, "knees bent", p.nearLeg.out, 15.0..35.0)
        // The legs stand under the hips, so they show below the head-on torso.
        assertTrue(!FigureRenderer.legHidden(p.nearLeg), "$n: legs drawn")
    }

    @Test
    fun seatedRearDeltRaise_onTheBenchEdge_bentOverTheThighs() {
        val n = "Seated rear delt raise"
        assertRearRaise(n)
        assertContact(n, BodyPoint.HIPS, Surface.SEAT)
        inRange(n, "bent over (lean)", pose(n, Which.START).lean, 60.0..75.0)
        inRange(n, "thighs forward", pose(n, Which.START).nearLeg.out, 65.0..85.0)
        // Bent over, so it does not read as sitting up: the shoulders low over the seat, the head sunk to them.
        val st = s(n, Which.START)
        val seat = props(n).filterIsInstance<Seat>().single()
        inRange(n, "shoulders above the seat", -st.near.shoulder.y - seat.height, 15.0..35.0)
        inRange(n, "head above the shoulders' line", st.near.shoulder.y - st.head.y, 0.0..10.0)
        // Against the upright seated lateral raise: the shoulders at least 20 cm lower above their seat.
        val lat = s("Seated lateral raise", Which.START)
        val latSeat = props("Seated lateral raise").filterIsInstance<Seat>().single()
        assertTrue((-lat.near.shoulder.y - latSeat.height) - (-st.near.shoulder.y - seat.height) > 20.0, "$n: lower than sitting up")
    }

    @Test
    fun chestSupportedRearDeltRaise_faceDownOnANearlyFlatBench_legsHiddenBehind() {
        val n = "Chest-supported rear delt raise"
        assertRearRaise(n)
        val p = pose(n, Which.START)
        inRange(n, "bench close to flat (lean)", p.lean, 75.0..85.0)
        assertTrue(FigureRenderer.legHidden(p.nearLeg) && FigureRenderer.legHidden(p.farLeg), "$n: legs behind the shoulders")
        val bench = props(n).filterIsInstance<Seat>().single()
        val st = s(n, Which.START)
        inRange(n, "bench under the chest", -bench.height - st.shoulder.y, 8.0..15.0)
        // The bench top wider than the torso (so the body reads as lying on it), the arms hanging outside it.
        assertTrue(bench.width / 2 > Body.SHOULDER_HALF_WIDTH + 3.0, "$n: bench top wider than the shoulders")
        assertTrue(st.near.elbow.x > bench.width / 2, "$n: arms outside the bench")
    }

    @Test
    fun machineReverseFly_chestOnThePad_forwardThenJustBehindTheTorso_elbowsAtShoulderHeight() {
        val n = "Machine reverse fly"
        assertTrue(props(n).filterIsInstance<PecDeck>().single().facingPad, "$n: facing the pad")
        inRange(n, "arms forward to the handles", pose(n, Which.START).nearArm.out, 50.0..90.0)
        inRange(n, "elbows just behind the torso", pose(n, Which.END).nearArm.out, -20.0..-5.0)
        for (w in Which.entries) {
            val sk = s(n, w)
            inRange(n, "elbow at shoulder height ($w)", abs(sk.near.elbow.y - sk.near.shoulder.y), 0.0..1.0)
        }
        assertCheck(n, HandsLevelWith(BodyPoint.SHOULDERS, 4.0, Which.END))
    }

    @Test
    fun cableReverseFly_pulleysCloseAtShoulderHeight_crossedCables_outToTheSides() {
        val n = "Cable reverse fly"
        val stack = props(n).filterIsInstance<CableStack>().single()
        val st = s(n, Which.START)
        assertTrue(stack.crossed && stack.halfSpan in 20.0..40.0, "$n: two pulleys close together, crossed cables")
        assertTrue(stack.beyond, "$n: the columns beyond the lifter")
        inRange(n, "pulleys at shoulder height", abs(-stack.pulleyHeight - st.near.shoulder.y), 0.0..3.0)
        // Seen from behind, the hands reaching forward stay just outside the shoulders: nothing crosses over the back.
        inRange(n, "hands just outside the shoulders", st.near.grip.x - st.near.shoulder.x, 0.0..12.0)
        // Every limb is drawn after (in front of) both columns.
        for (w in Which.entries) {
            val world = FigureRenderer.world(figure(n).scene(w), View.FRONT).map { it.shape }
            val lastColumn = world.indexOfLast { it is RoundRect && it.h > 150.0 }
            val sk = s(n, w)
            val farArm = world.indexOfFirst { it is Capsule && it.x1 == sk.far.shoulder.x && it.y1 == sk.far.shoulder.y }
            assertTrue(lastColumn in 0 until farArm, "$n: far arm in front of the columns ($w)")
        }
        inRange(n, "arms forward at the start", pose(n, Which.START).nearArm.out, 50.0..90.0)
        val en = s(n, Which.END)
        inRange(n, "out to the side", pose(n, Which.END).nearArm.out, 0.0..15.0)
        for (sk in listOf(st, en)) inRange(n, "elbow at shoulder height", abs(sk.near.elbow.y - sk.near.shoulder.y), 0.0..1.0)
    }

    @Test
    fun cableRearDeltRow_barToTheUpperChest_elbowsUpAndOut_justBehindTheBack() {
        val n = "Cable rear delt row"
        assertContact(n, BodyPoint.HIPS, Surface.SEAT)
        assertContact(n, BodyPoint.FEET, Surface.FOOT_PLATE)
        assertCheck(n, BarTouches(BodyPoint.UPPER_CHEST, Which.END))
        for (w in Which.entries) assertEquals(90.0, s(n, w).torso, "$n: torso upright")
        inRange(n, "arms straight", bend(n, Which.START), 0.0..1.0)
        for (w in Which.entries) inRange(n, "knees only slightly bent ($w)", pose(n, w).nearLeg.knee, 0.0..40.0)
        val en = s(n, Which.END)
        inRange(n, "upper arms level", abs(en.near.elbow.y - en.shoulder.y), 0.0..1.0)
        // Just behind the back (10 cm behind the joint): out to the side, never down by the ribs or forward.
        inRange(n, "elbow just behind the back", en.shoulder.x - en.near.elbow.x - Body.TORSO_RADIUS, 0.5..5.0)
        inRange(n, "elbow out to the side", pose(n, Which.END).nearArm.out, 55.0..85.0)
        inRange(n, "elbow bend", bend(n, Which.END), 100.0..150.0)
    }

    // ---- rotations ----

    /** The elbow against the side, bent 90°, in both panels; the forearm's swing between [startX] and [endX] (signs). */
    private fun assertRotation(n: String) {
        for (w in Which.entries) {
            val sk = s(n, w)
            val arm = pose(n, w).nearArm
            assertEquals(0.0, arm.out, "$n: upper arm in the plane ($w)")
            inRange(n, "elbow bent 90° ($w)", bend(n, w), 89.0..91.0)
            val down = sk.near.elbow - sk.near.shoulder
            inRange(n, "upper arm along the side ($w)", down.length(), Segment.UPPER_ARM.length - 0.1..Segment.UPPER_ARM.length + 0.1)
            inRange(n, "upper arm along the torso ($w)", abs(fold(angle(down) - (sk.torso + 180.0))), 0.0..1.0)
        }
    }

    @Test
    fun cableExternalRotation_acrossTheBelly_thenAwayFromTheBody_elbowAtTheSide() {
        val n = "Cable external rotation"
        assertRotation(n)
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        inRange(n, "forearm across the belly, in front of it", pose(n, Which.START).nearArm.forearmOut, 25.0..60.0)
        assertTrue(st.near.grip.x < 0.0, "$n: the hand past the middle")
        inRange(n, "hand away from the body", en.near.grip.x - en.near.elbow.x, 10.0..30.0)
        val stack = props(n).filterIsInstance<CableStack>().single()
        assertTrue(stack.cableInFront && stack.x < st.far.shoulder.x, "$n: the pulley on the other side, the cable across")
        inRange(n, "pulley at about waist height", abs(-stack.pulleyHeight - st.near.elbow.y), 0.0..10.0)
    }

    @Test
    fun cableInternalRotation_turnedOutTowardsThePulley_thenAcrossTheBelly() {
        val n = "Cable internal rotation"
        assertRotation(n)
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        inRange(n, "hand out towards the pulley", st.near.grip.x - st.near.elbow.x, 10.0..30.0)
        assertTrue(en.near.grip.x < 0.0, "$n: across the belly")
        inRange(n, "forearm in front of the belly", pose(n, Which.END).nearArm.forearmOut, 25.0..60.0)
        val stack = props(n).filterIsInstance<CableStack>().single()
        assertTrue(stack.x > st.near.shoulder.x, "$n: the pulley on the handle's side")
        inRange(n, "pulley at elbow height", abs(-stack.pulleyHeight - st.near.elbow.y), 0.0..1.0)
    }

    @Test
    fun sideLyingExternalRotation_onTheSide_forearmFromHangingToUp_elbowAtTheSide() {
        val n = "Side-lying external rotation"
        assertRotation(n)
        assertContact(n, BodyPoint.FAR_FOOT, Surface.FLOOR)
        val st = s(n, Which.START)
        val en = s(n, Which.END)
        inRange(n, "lying along the floor", st.torso, 165.0..185.0)
        assertTrue(st.near.shoulder.y < st.far.shoulder.y - 30.0, "$n: on the side, the top shoulder above the bottom one")
        inRange(n, "forearm hanging down", st.near.grip.y - st.near.elbow.y, 15.0..30.0)
        inRange(n, "in front of the belly", pose(n, Which.START).nearArm.forearmOut, 30.0..60.0)
        inRange(n, "raised: forearm up", en.near.elbow.y - en.near.grip.y, 30.0..35.0)
        assertTrue(st.near.grip.y + Dumbbell.HEAD_RADIUS < 0.0, "$n: dumbbell off the floor")
        assertTrue(props(n).filterIsInstance<Dumbbell>().single().let { !it.endOnFront }, "$n: the handle along the body, in profile")
    }
}
