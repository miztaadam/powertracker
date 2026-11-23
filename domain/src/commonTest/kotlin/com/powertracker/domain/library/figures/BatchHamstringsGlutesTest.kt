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

/** The hamstring and glute batch (Task 11): every exercise of the area, each with the key positions its guide gives. */
class BatchHamstringsGlutesTest {
    private fun figure(name: String): ExerciseFigure = assertNotNull(Figures.all[name], "no figure for $name")

    private fun s(name: String, which: Which) = figure(name).let { it.scene(which).pose.skeleton(it.view) }

    private fun pose(name: String, which: Which) = figure(name).scene(which).pose

    private fun props(name: String, which: Which = Which.START) = figure(name).scene(which).props

    private fun hasContact(name: String, point: BodyPoint, surface: Surface, at: Set<Which> = Which.entries.toSet()) =
        assertTrue(figure(name).contacts.any { it.point == point && it.surface == surface && it.at == at }, "$name: $point on $surface at $at")

    private fun inRange(name: String, label: String, value: Double, range: ClosedFloatingPointRange<Double>) =
        assertTrue(value in range, "$name: $label ${round1(value)} not in $range")

    private fun angle(v: Point) = atan2(-v.y, v.x) * 180 / PI

    /** The shin's lean forward of vertical, degrees (+: the knee in front of the ankle). */
    private fun shinLean(l: LimbPoints) = 90.0 - angle(l.knee - l.ankle)

    private fun knees(name: String, which: Which, range: ClosedFloatingPointRange<Double>, side: Side? = null) {
        val p = pose(name, which)
        if (side != Side.FAR) inRange(name, "near knee ($which)", p.nearLeg.knee, range)
        if (side != Side.NEAR) inRange(name, "far knee ($which)", p.farLeg.knee, range)
    }

    private fun feetFlat(name: String, which: Which, sides: List<Side> = Side.entries) {
        val sk = s(name, which)
        for (side in sides) inRange(name, "${side.label} foot tilt ($which)", abs(angle(sk.side(side).toe - sk.side(side).heel)), 0.0..1.0)
    }

    /** A foot's lowest point above the floor, cm (its capsule's round included). */
    private fun footUp(l: LimbPoints) = -maxOf(l.heel.y, l.toe.y) - Body.FOOT_WIDTH / 2

    private fun distance(p: Point, a: Point, b: Point): Double {
        val ab = b - a
        val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / (ab.x * ab.x + ab.y * ab.y)).coerceIn(0.0, 1.0)
        return (p - (a + ab * t)).length()
    }

    /** The bend at the hip between the shoulder-to-hip line and the hip-to-knee line, degrees (0: one straight line). */
    private fun lineBend(sk: Skeleton) = abs(fold(angle(sk.pelvis - sk.shoulder) - angle(sk.near.knee - sk.pelvis)))

    @Test
    fun hamstringsGlutes_everyExerciseHasAFigure_thatPassesItsChecks() {
        val area = Library.all().filter { it.area == BodyArea.HAMSTRINGS_GLUTES }.map { it.name }
        assertEquals(28, area.size)
        val problems = area.flatMap { name -> Figures.all[name]?.let { FigureChecks.check(name, it) } ?: listOf(FigureProblem(name, null, "no figure")) }
        assertEquals(emptyList(), problems.map { "${it.name} ${it.which}: ${it.message}" })
        val front = setOf("Machine hip abduction", "Machine hip adduction", "Lateral band walk", "Cable hip abduction", "Side-lying hip abduction", "Clamshell")
        for (name in area) assertEquals(if (name in front) View.FRONT else View.SIDE, figure(name).view, name)
    }

    // ---- hinges ----

    @Test
    fun stiffLegDeadlift_straighterKneesThanTheRdl_barAtTheKnees_closeAndOverTheFoot() {
        val n = "Stiff-leg deadlift"
        knees(n, Which.START, 7.9..8.1)
        knees(n, Which.END, 7.9..8.1)
        knees("RDL", Which.END, 17.9..18.1)
        for (c in listOf(BarOverMidfoot(Which.START), BarOverMidfoot(Which.END), BarNearLegs(4.0, Which.START), BarNearLegs(4.0, Which.END))) {
            assertTrue(c in figure(n).checks, "$n: $c")
        }
        inRange(n, "standing torso", pose(n, Which.START).torso, 86.0..90.0)
        // The straighter knees put the torso lower than the RDL's at the same depth: plainly different at a glance.
        inRange(n, "torso below the RDL's", pose("RDL", Which.END).torso - pose(n, Which.END).torso, 4.0..30.0)
        val e = s(n, Which.END)
        inRange(n, "bar about at the knees", e.near.grip.y - e.near.knee.y, -6.0..2.0)
        inRange(n, "shins vertical", abs(shinLean(e.near)), 0.0..1.0)
        assertTrue(props(n).none { it is PowerRack }, "$n: taken from the floor, no rack")
        feetFlat(n, Which.START)
        feetFlat(n, Which.END)
    }

    @Test
    fun dbRdl_theRdlsKnees_dumbbellsEndOnJustClearOfTheLegs() {
        val n = "DB RDL"
        knees(n, Which.START, 17.9..18.1)
        knees(n, Which.END, 17.9..18.1)
        for (w in Which.entries) assertEquals(2, props(n, w).filterIsInstance<Dumbbell>().count { !it.neutral }, "$n: palms to the thighs, end on")
        val st = s(n, Which.START)
        inRange(n, "disc clear of the thighs (START)", distance(st.near.grip, st.near.hip, st.near.knee) - Body.THIGH_WIDTH / 2 - Dumbbell.HEAD_RADIUS, 0.0..2.0)
        val e = s(n, Which.END)
        inRange(n, "disc clear of the knee (END)", (e.near.grip - e.near.knee).length() - Body.THIGH_WIDTH / 2 - Dumbbell.HEAD_RADIUS, 0.0..2.0)
        inRange(n, "weights about at the knees", e.near.grip.y - e.near.knee.y, -6.0..2.0)
        inRange(n, "torso lowered", pose(n, Which.END).torso, 15.0..45.0)
        inRange(n, "hips back of the ankle", e.near.ankle.x - e.pelvis.x, 5.0..30.0)
    }

    @Test
    fun singleLegRdl_oneLegHinge_backLegInLine_armsHangStraightDown() {
        val n = "Single-leg RDL"
        knees(n, Which.START, 9.9..10.1)
        assertEquals("Knees slightly bent", figure(n).startCaption)
        feetFlat(n, Which.START)
        hasContact(n, BodyPoint.FAR_FOOT, Surface.FLOOR)
        hasContact(n, BodyPoint.NEAR_FOOT, Surface.FLOOR, setOf(Which.START))
        val p = pose(n, Which.END)
        // A soft knee, bent more than at the start to reach the floor (ExRx), not a squat.
        inRange(n, "standing knee soft", p.farLeg.knee, 25.0..35.0)
        inRange(n, "back knee straight", p.nearLeg.knee, 0.0..0.5)
        inRange(n, "back leg in line with the torso", abs(p.nearLeg.hip), 0.0..0.5)
        inRange(n, "torso about level, a little below at most", p.torso, -18.0..0.0)
        val e = s(n, Which.END)
        feetFlat(n, Which.END, listOf(Side.FAR))
        inRange(n, "back foot well off the floor", footUp(e.near), 60.0..120.0)
        // Both arms hang straight down from the shoulder (the free arm too, not along the torso: lesson 11).
        for (side in Side.entries) inRange(n, "${side.label} elbow below the shoulder", abs(e.side(side).elbow.x - e.side(side).shoulder.x), 0.0..0.5)
        inRange(n, "free hand down by the weight", abs(e.far.wrist.x - e.shoulder.x), 0.0..20.0)
        inRange(n, "free hand off the floor", -maxOf(e.far.wrist.y, e.far.hand.y) - Body.HAND_WIDTH / 2, 1.0..30.0)
        // The guide: lower until the weight reaches the floor.
        inRange(n, "dumbbell at the floor", -Dumbbell(Side.NEAR).center(e).y - Dumbbell.HEAD_RADIUS, 0.0..3.0)
        // The hips over the standing foot.
        inRange(n, "hips over the foot", e.pelvis.x - e.far.heel.x, -6.0..20.0)
        assertEquals(1, props(n).filterIsInstance<Dumbbell>().size)
    }

    @Test
    fun goodMorning_barOnTheBack_torsoToLevel_kneesSoft_rackClearOfTheHead() {
        val n = "Good morning"
        assertTrue(props(n).any { it is Barbell && it.on == BarHold.UPPER_BACK })
        knees(n, Which.START, 9.9..10.1)
        knees(n, Which.END, 21.9..22.1)
        inRange(n, "torso level, no further", pose(n, Which.END).torso, 0.0..5.0)
        inRange(n, "standing torso", pose(n, Which.START).torso, 78.0..90.0)
        assertTrue(BarOverMidfoot(Which.START) in figure(n).checks)
        val e = s(n, Which.END)
        val bar = Barbell(BarHold.UPPER_BACK).center(e)
        inRange(n, "bar over the foot at the bottom", bar.x, e.near.heel.x..e.near.toe.x)
        inRange(n, "shins tipped back at most", shinLean(e.near), -10.1..2.0)
        val rack = props(n).filterIsInstance<PowerRack>().single()
        val st = s(n, Which.START)
        inRange(n, "J-hooks under the bar", -Barbell(BarHold.UPPER_BACK).center(st).y - rack.jHookHeight, 4.0..12.0)
        inRange(n, "safety arms under the low bar", -bar.y - rack.safetyHeight, 4.0..10.0)
        for (w in Which.entries) inRange(n, "posts clear of the head ($w)", rack.x - (s(n, w).head.x + Body.HEAD_RADIUS), 25.0..60.0)
        // The head clear of the safety arm at the bottom (not resting on its tip).
        val arm = rack.surfaces(View.SIDE).getValue(Surface.SAFETY_ARM).single()
        inRange(n, "head clear of the safety arm (END)", arm.signedDistance(e.head) - Body.HEAD_RADIUS, 5.0..100.0)
        feetFlat(n, Which.START)
        feetFlat(n, Which.END)
    }

    // ---- swings ----

    private fun bell(name: String, which: Which) = props(name, which).filterIsInstance<Kettlebell>().single().let { kb ->
        assertEquals(KettlebellHold.SWING, kb.hold)
        kb.bellCentre(s(name, which))
    }

    /** The swing's bottom: a hinge, the forearm on the thigh, the bell under the hips behind the knees; the top standing tall. */
    private fun assertSwing(n: String) {
        val p = pose(n, Which.START)
        inRange(n, "hinged torso", p.torso, 15.0..45.0)
        knees(n, Which.START, 15.0..30.0)
        val st = s(n, Which.START)
        inRange(n, "shins upright", shinLean(st.near), 0.0..8.0)
        inRange(n, "wrist on the thigh's line", distance(st.near.wrist, st.near.hip, st.near.knee), 0.0..1.0)
        inRange(n, "arm straight", p.nearArm.elbow, 0.0..0.5)
        val b0 = bell(n, Which.START)
        inRange(n, "bell behind the knees", st.near.knee.x - b0.x, 3.0..40.0)
        inRange(n, "bell below the hips", b0.y - st.pelvis.y, 10.0..60.0)
        knees(n, Which.END, 0.0..0.5)
        inRange(n, "standing tall", pose(n, Which.END).torso, 89.9..90.1)
        val e = s(n, Which.END)
        inRange(n, "hands at chest height", e.near.grip.y - e.point(BodyPoint.CHEST).y, -2.0..2.0)
        inRange(n, "arm straight (END)", pose(n, Which.END).nearArm.elbow, 0.0..0.5)
        inRange(n, "bell past the hands, not overhead", e.shoulder.y - bell(n, Which.END).y, -30.0..0.0)
        feetFlat(n, Which.START)
        feetFlat(n, Which.END)
    }

    @Test
    fun kettlebellSwing_hingeBetweenTheLegs_thenChestHeight_armsStraight() {
        val n = "Kettlebell swing"
        assertSwing(n)
        assertEquals(pose(n, Which.START).nearArm, pose(n, Which.START).farArm, "$n: both hands on the handle")
        assertEquals(Which.END, figure(n).thumbnail)
    }

    @Test
    fun singleArmKettlebellSwing_freeArmBackThenForward_apartFromTheBellArm() {
        val n = "Single-arm kettlebell swing"
        assertSwing(n)
        val st = s(n, Which.START)
        // The free arm swung back behind the shoulders, clear of the torso and hips (lesson 11).
        inRange(n, "free hand behind the shoulder", st.shoulder.x - st.far.grip.x, 35.0..70.0)
        inRange(n, "free hand above the hips", st.pelvis.y - st.far.grip.y, 0.0..40.0)
        val e = s(n, Which.END)
        inRange(n, "free hand forward, below the bell hand", e.far.grip.y - e.near.grip.y, 8.0..40.0)
        inRange(n, "free hand in front", e.far.grip.x - e.shoulder.x, 30.0..70.0)
        // Its thumbnail is the other panel from the two-arm swing's (lesson 10).
        assertEquals(Which.START, figure(n).thumbnail)
    }

    // ---- leg curls ----

    /** A curl machine's lever: from the pivot on the knee to the roller on the calf, the same length in both panels. */
    private fun assertLever(n: String, pivot: (Prop) -> Point?, roller: (Prop) -> Point?) {
        val lengths = Which.entries.map { w ->
            val m = props(n, w).first { pivot(it) != null }
            inRange(n, "pivot on the knee ($w)", (pivot(m)!! - s(n, w).near.knee).length(), 0.0..0.01)
            (roller(m)!! - pivot(m)!!).length()
        }
        inRange(n, "lever length the same", abs(lengths[0] - lengths[1]), 0.0..0.01)
    }

    @Test
    fun lyingLegCurl_faceDown_kneesPastTheEdge_padOnTheLowerLegs_curlTo115() {
        val n = "Lying leg curl"
        knees(n, Which.START, 0.0..0.5)
        knees(n, Which.END, 114.9..115.1)
        for (w in Which.entries) inRange(n, "torso level ($w)", pose(n, w).torso, -0.1..0.1)
        assertEquals(pose(n, Which.START).copy(nearLeg = pose(n, Which.END).nearLeg, farLeg = pose(n, Which.END).farLeg), pose(n, Which.END), "$n: only the knees move")
        hasContact(n, BodyPoint.HIPS, Surface.BENCH)
        hasContact(n, BodyPoint.THIGHS, Surface.BENCH)
        hasContact(n, BodyPoint.SHINS, Surface.SHIN_PAD)
        hasContact(n, BodyPoint.HANDS, Surface.HANDLE)
        val m = props(n).filterIsInstance<LyingLegCurlMachine>().single()
        val st = s(n, Which.START)
        inRange(n, "knee just past the edge", m.kneeEnd - st.near.knee.x, 2.0..6.0)
        assertLever(n, { (it as? LyingLegCurlMachine)?.pivot }, { (it as? LyingLegCurlMachine)?.roller })
        // The roller above the heels: 75..85% down the shin.
        val d = st.near.ankle - st.near.knee
        val along = ((m.roller - st.near.knee).x * d.x + (m.roller - st.near.knee).y * d.y) / (d.length() * d.length())
        inRange(n, "roller above the heels", along, 0.75..0.85)
        // END the shin up towards the thigh's back.
        inRange(n, "lower leg raised", -(s(n, Which.END).near.ankle.y - s(n, Which.END).near.knee.y), 30.0..43.0)
    }

    @Test
    fun seatedLegCurl_backOnThePad_lapPadAboveTheKnees_legsStraightThenPadBackUnder() {
        val n = "Seated leg curl"
        knees(n, Which.START, 0.0..5.0)
        knees(n, Which.END, 95.0..105.0)
        hasContact(n, BodyPoint.HIPS, Surface.SEAT)
        hasContact(n, BodyPoint.UPPER_BACK, Surface.BACK_PAD)
        hasContact(n, BodyPoint.THIGHS, Surface.THIGH_PAD)
        hasContact(n, BodyPoint.SHINS, Surface.SHIN_PAD)
        assertLever(n, { (it as? SeatedLegCurlMachine)?.pivot }, { (it as? SeatedLegCurlMachine)?.roller })
        val st = s(n, Which.START)
        inRange(n, "legs out level", abs(st.near.ankle.y - st.near.knee.y), 0.0..5.0)
        val e = s(n, Which.END)
        inRange(n, "pad back under: the feet behind the knees", shinLean(e.near), 5.0..20.0)
        val m = props(n).filterIsInstance<SeatedLegCurlMachine>().single()
        inRange(n, "lap pad above the knees", m.lap.y - st.near.knee.y, -20.0..-8.0)
        inRange(n, "lap pad just behind the knees", st.near.knee.x - m.lap.x, 4.0..14.0)
    }

    @Test
    fun standingLegCurl_bentForward_oneLeg_kneeOnThePivot_curlTo115() {
        val n = "Standing leg curl"
        knees(n, Which.START, 0.0..0.5, Side.NEAR)
        knees(n, Which.END, 114.9..115.1, Side.NEAR)
        for (w in Which.entries) inRange(n, "standing knee ($w)", pose(n, w).farLeg.knee, 0.0..5.0)
        for (w in Which.entries) inRange(n, "bent forward at the hips ($w)", pose(n, w).torso, 55.0..65.0)
        feetFlat(n, Which.START, listOf(Side.FAR))
        val st = s(n, Which.START)
        inRange(n, "working foot off the floor", footUp(st.near), 1.0..10.0)
        inRange(n, "working thigh near vertical", abs(fold(angle(st.near.knee - st.near.hip) + 90.0)), 10.0..20.0)
        hasContact(n, BodyPoint.NEAR_THIGH, Surface.THIGH_PAD)
        hasContact(n, BodyPoint.NEAR_SHIN, Surface.SHIN_PAD)
        hasContact(n, BodyPoint.HANDS, Surface.HANDLE)
        assertLever(n, { (it as? StandingLegCurlMachine)?.pivot }, { (it as? StandingLegCurlMachine)?.roller })
    }

    @Test
    fun cableLegCurl_armsStraightToTheStack_footJustOffTheFloor_cableToTheCuff() {
        val n = "Cable leg curl"
        knees(n, Which.START, 0.0..0.5)
        knees(n, Which.END, 115.0..125.0, Side.NEAR)
        for (w in Which.entries) {
            val p = pose(n, w)
            inRange(n, "back leg straight ($w)", p.farLeg.knee, 0.0..0.5)
            inRange(n, "back leg in line with the torso ($w)", abs(p.farLeg.hip), 0.0..0.5)
            inRange(n, "arms straight ($w)", p.nearArm.elbow, 0.0..0.5)
            assertTrue(props(n, w).any { it is AnkleStrap && it.side == Side.NEAR }, "$n: cuff on the working ankle")
            val stack = props(n, w).filterIsInstance<CableStack>().single()
            assertTrue(stack.cableInFront && stack.pulleyHeight < 20.0, "$n: a low pulley, its cable drawn to the cuff")
            inRange(n, "hands on the stack's frame ($w)", stack.x - 9.0 - s(n, w).near.grip.x, 0.0..4.0)
        }
        feetFlat(n, Which.START, listOf(Side.FAR))
        inRange(n, "working foot just off the floor", footUp(s(n, Which.START).near), 1.0..8.0)
        val e = s(n, Which.END)
        inRange(n, "lower leg back", e.near.knee.x - e.near.ankle.x, 30.0..43.0)
    }

    @Test
    fun stabilityBallLegCurl_straightLineOnTheBall_thenHeelsInHipsUp() {
        val n = "Stability ball leg curl"
        val p0 = pose(n, Which.START)
        inRange(n, "START legs straight", p0.nearLeg.knee, 0.0..0.5)
        inRange(n, "START body straight", abs(p0.nearLeg.hip), 0.0..0.5)
        val p1 = pose(n, Which.END)
        inRange(n, "END knees bent", p1.nearLeg.knee, 95.0..105.0)
        inRange(n, "END torso in line with the thighs", abs(p1.nearLeg.hip), 0.0..0.5)
        for (w in Which.entries) inRange(n, "shoulders on the floor ($w)", -s(n, w).shoulder.y, 9.5..11.0)
        hasContact(n, BodyPoint.HEAD, Surface.FLOOR)
        hasContact(n, BodyPoint.SHINS, Surface.BALL, setOf(Which.START))
        hasContact(n, BodyPoint.FEET, Surface.BALL, setOf(Which.END))
        val b0 = props(n, Which.START).filterIsInstance<StabilityBall>().single()
        val b1 = props(n, Which.END).filterIsInstance<StabilityBall>().single()
        assertEquals(65.0, b0.diameter)
        inRange(n, "ball rolled in", b0.x - b1.x, 10.0..60.0)
        inRange(n, "hips higher at the end", s(n, Which.START).pelvis.y - s(n, Which.END).pelvis.y, 5.0..40.0)
    }

    // ---- Nordic curl and glute-ham raise ----

    @Test
    fun nordicCurl_kneeling_oneLineFromTheKnees_loweredToTheFingertips() {
        val n = "Nordic curl"
        knees(n, Which.START, 89.9..90.1)
        knees(n, Which.END, 29.9..30.1)
        for (w in Which.entries) {
            inRange(n, "hips straight ($w)", abs(pose(n, w).nearLeg.hip), 0.0..0.01)
            val sk = s(n, w)
            inRange(n, "shins along the pad ($w)", abs(sk.near.ankle.y - sk.near.knee.y), 0.0..0.01)
            inRange(n, "toes down to the floor ($w)", footUp(sk.near), -0.5..1.0)
        }
        hasContact(n, BodyPoint.KNEES, Surface.KNEE_PAD)
        hasContact(n, BodyPoint.SHINS, Surface.LEG_BRACE)
        val e = s(n, Which.END)
        inRange(n, "fingertips on the floor", -maxOf(e.near.wrist.y, e.near.hand.y) - Body.HAND_WIDTH / 2, -0.5..1.0)
        inRange(n, "elbows only slightly bent", pose(n, Which.END).nearArm.elbow, 0.0..30.0)
        inRange(n, "START arms out to the sides", pose(n, Which.START).nearArm.out, 40.0..70.0)
        inRange(n, "START elbows slightly bent", pose(n, Which.START).nearArm.elbow, 15.0..25.0)
    }

    @Test
    fun gluteHamRaise_kneelingUpright_thenLevel_noFurther() {
        val n = "Glute-ham raise"
        knees(n, Which.START, 89.9..90.1)
        knees(n, Which.END, 0.0..0.1)
        inRange(n, "upright", pose(n, Which.START).torso, 89.9..90.1)
        inRange(n, "level, no further", pose(n, Which.END).torso, -0.1..0.1)
        for (w in Which.entries) {
            inRange(n, "hips straight ($w)", abs(pose(n, w).nearLeg.hip), 0.0..0.01)
            val sk = s(n, w)
            inRange(n, "shins level ($w)", abs(sk.near.ankle.y - sk.near.knee.y), 0.0..0.01)
        }
        inRange(n, "the knees stay on the pad", (s(n, Which.START).near.knee - s(n, Which.END).near.knee).length(), 0.0..0.01)
        hasContact(n, BodyPoint.KNEES, Surface.THIGH_PAD)
        hasContact(n, BodyPoint.FEET, Surface.FOOT_PLATE)
        hasContact(n, BodyPoint.SHINS, Surface.LEG_BRACE)
        assertTrue(props(n).single { it is HyperBench }.let { (it as HyperBench).angle == 0.0 }, "$n: the bench laid level")
    }

    // ---- bridges ----

    /** A bridge's top: the shoulders, hips and knees in one line, the shins near vertical, the shoulders on the floor. */
    private fun assertBridgeTop(n: String, which: Which = Which.END) {
        val sk = s(n, which)
        inRange(n, "one line, shoulders to knees", lineBend(sk), 0.0..1.0)
        inRange(n, "shins near vertical", -shinLean(sk.far), 0.0..10.0)
    }

    private fun assertOnTheFloor(n: String) {
        for (w in Which.entries) inRange(n, "shoulders on the floor ($w)", -s(n, w).shoulder.y, 9.5..11.0)
        hasContact(n, BodyPoint.HEAD, Surface.FLOOR)
        feetFlat(n, Which.START, listOf(Side.FAR))
        feetFlat(n, Which.END, listOf(Side.FAR))
    }

    @Test
    fun gluteBridge_lyingKneesBent_thenHipsUpInOneLine() {
        val n = "Glute bridge"
        assertOnTheFloor(n)
        hasContact(n, BodyPoint.HIPS, Surface.FLOOR, setOf(Which.START))
        assertBridgeTop(n)
        knees(n, Which.START, 110.0..128.0)
        knees(n, Which.END, 95.0..115.0)
        inRange(n, "hips lifted", -s(n, Which.END).pelvis.y, 25.0..40.0)
    }

    @Test
    fun singleLegGluteBridge_straightLegInLineWithTheHips_inBothPanels() {
        val n = "Single-leg glute bridge"
        assertOnTheFloor(n)
        assertBridgeTop(n)
        for (w in Which.entries) {
            inRange(n, "straight leg ($w)", pose(n, w).nearLeg.knee, 0.0..0.01)
            inRange(n, "in line with the hips ($w)", abs(pose(n, w).nearLeg.hip), 0.0..0.01)
        }
        inRange(n, "straight leg raised at the top", footUp(s(n, Which.END).near), 40.0..90.0)
        // The two-leg bridge's planted leg, the same.
        assertEquals(pose("Glute bridge", Which.END).farLeg, pose(n, Which.END).farLeg)
    }

    @Test
    fun barbellGluteBridge_platesOnTheFloor_thenBridgeTop_handsOnTheBar() {
        val n = "Barbell glute bridge"
        assertOnTheFloor(n)
        assertBridgeTop(n)
        for (w in Which.entries) {
            val sk = s(n, w)
            val bar = Barbell().center(sk)
            inRange(n, "bar on the hips ($w)", (bar - sk.pelvis).length(), 9.0..12.0)
            inRange(n, "hands together on the bar ($w)", (sk.near.grip - sk.far.grip).length(), 0.0..0.01)
        }
        inRange(n, "plates on the floor at the start", -Barbell().center(s(n, Which.START)).y - Barbell.PLATE_DIAMETER / 2, -0.3..0.3)
        assertElbowsOut(n)
    }

    // ---- thrusts ----

    /**
     * Hands on a load on the hips (Hip thrust, DB and Machine hip thrust, Barbell glute bridge): the elbow points out to
     * the side, towards the camera (a bend the wrong way, or in the picture's plane, would put it through the floor or the
     * bench), and the drawn elbow stays above the floor and clear of the bench.
     */
    private fun assertElbowsOut(n: String) {
        for (w in Which.entries) {
            val a = pose(n, w).nearArm
            inRange(n, "upper arm out towards the camera ($w)", a.out, 20.0..80.0)
            inRange(n, "elbow bent ($w)", JointLimits.elbowBend(a), 20.0..140.0)
            val sk = s(n, w)
            inRange(n, "elbow above the floor ($w)", -sk.near.elbow.y - Body.UPPER_ARM_WIDTH / 2, 0.5..100.0)
            props(n, w).filterIsInstance<FlatBench>().singleOrNull()?.let { b ->
                val pad = b.surfaces(View.SIDE).getValue(Surface.BENCH).single()
                inRange(n, "elbow clear of the bench ($w)", pad.signedDistance(sk.near.elbow) - Body.UPPER_ARM_WIDTH / 2, 0.0..200.0)
            }
        }
    }

    private fun assertThrust(n: String) {
        assertElbowsOut(n)
        hasContact(n, BodyPoint.UPPER_BACK, if (n == "Machine hip thrust") Surface.BENCH else Surface.BENCH)
        val e = s(n, Which.END)
        inRange(n, "torso level at the top", abs(fold(pose(n, Which.END).torso - 180.0)), 0.0..0.5)
        inRange(n, "hips in line with shoulders and knees", lineBend(e), 0.0..6.0)
        inRange(n, "shins vertical at the top", abs(shinLean(e.near)), 0.0..1.0)
        inRange(n, "hips lowered at the start", -s(n, Which.START).pelvis.y, 8.0..25.0)
        knees(n, Which.END, 80.0..100.0)
        knees(n, Which.START, 90.0..130.0)
        feetFlat(n, Which.START)
        feetFlat(n, Which.END)
        val sh0 = s(n, Which.START)
        inRange(n, "upper back on the bench's edge (START)", abs(sh0.onTorso(Segment.TORSO.length - 8.0, -Body.TORSO_RADIUS).x - 0.0), 0.0..3.0)
    }

    @Test
    fun hipThrust_upperBackOnABenchSeenEndOn_barOverTheHips_bodyStraightAtTheTop() {
        val n = "Hip thrust"
        assertThrust(n)
        val bench = props(n).filterIsInstance<FlatBench>().single()
        inRange(n, "bench 40..45 cm", bench.height, 40.0..45.0)
        hasContact(n, BodyPoint.FEET, Surface.FLOOR)
        for (w in Which.entries) {
            val sk = s(n, w)
            inRange(n, "bar on the hips ($w)", (Barbell().center(sk) - sk.pelvis).length(), 9.0..12.0)
        }
        inRange(n, "plates on the floor at the start", -Barbell().center(s(n, Which.START)).y - Barbell.PLATE_DIAMETER / 2, -0.3..0.3)
    }

    @Test
    fun dbHipThrust_dumbbellsOnTheHips_thrustPoses() {
        val n = "DB hip thrust"
        assertThrust(n)
        assertEquals(pose("Hip thrust", Which.END).nearLeg, pose(n, Which.END).nearLeg, "$n: the hip thrust's legs")
        for (w in Which.entries) {
            val sk = s(n, w)
            val db = props(n, w).filterIsInstance<Dumbbell>()
            assertEquals(2, db.size)
            inRange(n, "dumbbells resting on the hips ($w)", (db.first().center(sk) - sk.pelvis).length(), 18.0..21.0)
        }
    }

    @Test
    fun machineHipThrust_padOverTheHips_feetOnThePlatform_leverTheSameLength() {
        val n = "Machine hip thrust"
        assertThrust(n)
        hasContact(n, BodyPoint.FEET, Surface.FOOT_PLATE)
        hasContact(n, BodyPoint.HIPS, Surface.HIP_PAD)
        val m = Which.entries.map { props(n, it).filterIsInstance<HipThrustMachine>().single() }
        inRange(n, "lever length the same", abs((m[0].pad - m[0].pivot).length() - (m[1].pad - m[1].pivot).length()), 0.0..0.01)
        inRange(n, "pivot at the feet's end", m[0].pivot.x - s(n, Which.START).near.ankle.x, 5.0..40.0)
        for (w in Which.entries) inRange(n, "hands on the pad ($w)", (s(n, w).near.grip - m[w.ordinal].pad).length(), 5.0..12.0)
    }

    // ---- hip extensions ----

    @Test
    fun reverseHyper_hipsAtTheEdge_legsHangThenNearlyInLine_kneesStraight() {
        val n = "Reverse hyper"
        knees(n, Which.START, 0.0..0.01)
        knees(n, Which.END, 0.0..0.01)
        inRange(n, "legs hang straight down", pose(n, Which.START).nearLeg.hip, 89.9..90.1)
        inRange(n, "nearly in line", pose(n, Which.END).nearLeg.hip, 2.0..10.0)
        for (w in Which.entries) inRange(n, "torso level ($w)", abs(pose(n, w).torso), 0.0..0.1)
        hasContact(n, BodyPoint.WAIST, Surface.BENCH)
        hasContact(n, BodyPoint.HANDS, Surface.HANDLE)
        val bench = props(n).filterIsInstance<ReverseHyperBench>().single()
        inRange(n, "hips just past the edge", bench.edge - s(n, Which.START).pelvis.x, 7.0..10.0)
        inRange(n, "feet off the floor", footUp(s(n, Which.START).near), 5.0..30.0)
        for (w in Which.entries) inRange(n, "hands on the handle ($w)", (s(n, w).near.grip - bench.handle).length(), 0.0..0.5)
    }

    @Test
    fun quadrupedHipExtension_handsUnderShoulders_kneesUnderHips_liftKeepingTheKneeBent() {
        val n = "Quadruped hip extension"
        val st = s(n, Which.START)
        inRange(n, "hands under the shoulders", abs(st.near.wrist.x - st.near.shoulder.x), 0.0..1.0)
        inRange(n, "knees under the hips", abs(st.near.knee.x - st.pelvis.x), 0.0..0.5)
        inRange(n, "back slope up to the head", pose(n, Which.START).torso, 5.0..16.0)
        // The elbows only softened, bent backwards (towards the knees), the same in both arms.
        inRange(n, "soft elbows", pose(n, Which.START).nearArm.elbow, 0.0..35.0)
        inRange(n, "elbow behind the wrist", st.near.wrist.x - st.near.elbow.x, 0.0..20.0)
        assertEquals(pose(n, Which.START).nearArm, pose(n, Which.START).farArm)
        val p = pose(n, Which.END)
        inRange(n, "knee kept bent", p.nearLeg.knee, 89.9..90.1)
        inRange(n, "thigh up to the back's line, not past it", p.nearLeg.hip, -0.01..0.01)
        assertEquals(pose(n, Which.START).farLeg, p.farLeg, "$n: the other leg stays down")
        assertEquals(pose(n, Which.START).copy(nearLeg = p.nearLeg), p, "$n: only the hip moves")
        hasContact(n, BodyPoint.HANDS, Surface.FLOOR)
        hasContact(n, BodyPoint.FAR_KNEE, Surface.FLOOR)
        inRange(n, "foot lifted", footUp(s(n, Which.END).near), 40.0..100.0)
    }

    // ---- abduction and adduction (front view) ----

    private fun abduction(n: String, which: Which, side: Side = Side.NEAR): Double {
        val p = pose(n, which)
        return JointLimits.trueAbduction(if (side == Side.NEAR) p.nearLeg else p.farLeg, View.FRONT)
    }

    @Test
    fun machineHipAbduction_seated_padsOutsideTheKnees_legsPushedApart() {
        val n = "Machine hip abduction"
        val m = props(n).filterIsInstance<HipMachine>().single()
        assertTrue(m.padsOutside)
        inRange(n, "START thighs straight ahead", abduction(n, Which.START), -0.1..1.0)
        inRange(n, "END thighs apart", abduction(n, Which.END), 28.0..31.0)
        for (w in Which.entries) {
            val sk = s(n, w)
            inRange(n, "seated, thighs forward ($w)", JointLimits.trueFrontHipFlexion(pose(n, w).nearLeg, 0.0), 80.0..90.0)
            val (a, _) = m.kneePad(Side.NEAR, sk)
            inRange(n, "pad on the outside of the knee ($w)", a.x - sk.near.knee.x, 9.0..10.0)
            inRange(n, "shins hang vertically ($w)", abs(sk.near.ankle.x - sk.near.knee.x), 0.0..0.01)
        }
        hasContact(n, BodyPoint.HIPS, Surface.SEAT)
        assertHandsOnHandles(n)
    }

    /** The hands on the machine's handles: each grip on its handle, in both panels. */
    private fun assertHandsOnHandles(n: String) {
        hasContact(n, BodyPoint.HANDS, Surface.HANDLE)
        for (w in Which.entries) {
            val m = props(n, w).filterIsInstance<HipMachine>().single()
            val h = m.handles()
            assertEquals(2, h.size)
            val sk = s(n, w)
            inRange(n, "near hand on its handle ($w)", (sk.near.grip - h[0]).length(), 0.0..0.5)
            inRange(n, "far hand on its handle ($w)", (sk.far.grip - h[1]).length(), 0.0..0.5)
        }
    }

    @Test
    fun machineHipAdduction_padsInsideTheKnees_squeezedUntilThePadsMeet() {
        val n = "Machine hip adduction"
        val m = props(n).filterIsInstance<HipMachine>().single()
        assertTrue(!m.padsOutside)
        inRange(n, "START set width", abduction(n, Which.START), 28.0..31.0)
        inRange(n, "END together", abduction(n, Which.END), 0.0..10.0)
        val e = s(n, Which.END)
        val near = m.kneePad(Side.NEAR, e).first
        val far = m.kneePad(Side.FAR, e).first
        inRange(n, "pads meet", near.x - far.x - 2 * HipMachine.KNEE_PAD_RADIUS, 0.0..0.6)
        inRange(n, "pad on the inside of the knee", e.near.knee.x - near.x, 9.0..10.0)
        assertHandsOnHandles(n)
    }

    @Test
    fun lateralBandWalk_slightlyBent_feetHipWidth_thenAStepOut_bandAboveTheKnees() {
        val n = "Lateral band walk"
        assertTrue(props(n).any { it is MiniBand && it.at == BandAt.KNEES })
        val st = s(n, Which.START)
        val e = s(n, Which.END)
        inRange(n, "feet about hip-width", st.near.ankle.x - st.far.ankle.x, 20.0..32.0)
        inRange(n, "a step out", (e.near.ankle.x - e.far.ankle.x) - (st.near.ankle.x - st.far.ankle.x), 20.0..40.0)
        for (w in Which.entries) {
            val p = pose(n, w)
            inRange(n, "knees slightly bent ($w)", p.nearLeg.out, 10.0..20.0)
            inRange(n, "hips slightly bent ($w)", p.lean, 10.0..20.0)
            for (side in Side.entries) inRange(n, "${side.label} foot on the floor ($w)", footUp(s(n, w).side(side)), -0.1..0.1)
        }
    }

    @Test
    fun cableHipAbduction_crossedInFront_thenOutToTheSide_awayFromThePulley() {
        val n = "Cable hip abduction"
        val st = s(n, Which.START)
        val e = s(n, Which.END)
        inRange(n, "START crossed in", abduction(n, Which.START), -15.0..-8.0)
        inRange(n, "START in front", pose(n, Which.START).nearLeg.out, 5.0..15.0)
        inRange(n, "crossed over the standing foot", st.near.ankle.x - st.far.ankle.x, -5.0..5.0)
        inRange(n, "cuffed foot just off the floor", footUp(st.near), 1.0..6.0)
        inRange(n, "END out to the side", abduction(n, Which.END), 29.0..31.0)
        for (w in Which.entries) {
            val p = pose(n, w)
            inRange(n, "knee straight ($w)", p.nearLeg.knee, 0.0..0.01)
            inRange(n, "standing leg straight down ($w)", abs(p.farLeg.hip) + p.farLeg.knee, 0.0..0.01)
            inRange(n, "pelvis level ($w)", p.torso, 89.99..90.01)
            assertTrue(props(n, w).any { it is AnkleStrap && it.side == Side.NEAR })
            val stack = props(n, w).filterIsInstance<CableStack>().single()
            assertTrue(stack.cableInFront && stack.pulleyHeight < 20.0)
            // The pulley on the standing leg's side, the cuffed leg lifted away from it.
            assertTrue(stack.x < s(n, w).far.ankle.x - 40.0)
            inRange(n, "standing-side hand on the frame ($w)", s(n, w).far.grip.x - (stack.x + 9.0), 0.0..4.0)
        }
        inRange(n, "foot out and up", e.near.ankle.x - st.near.ankle.x, 40.0..70.0)
    }

    @Test
    fun sideLyingHipAbduction_legsStacked_thenTopLegLifted30() {
        val n = "Side-lying hip abduction"
        val p0 = pose(n, Which.START)
        val p1 = pose(n, Which.END)
        inRange(n, "top leg resting on the bottom one", p0.nearLeg.hip, -6.0..0.0)
        inRange(n, "lift", p1.nearLeg.hip - p0.nearLeg.hip, 29.9..30.1)
        for (p in listOf(p0, p1)) {
            inRange(n, "knees straight", p.nearLeg.knee + p.farLeg.knee, 0.0..0.01)
            inRange(n, "knee pointing forward (not turned)", abs(p.nearLeg.out), 0.0..0.01)
            assertEquals(174.0, p.torso)
        }
        assertEquals(p0.farLeg, p1.farLeg)
        val st = s(n, Which.START)
        inRange(n, "ankles stacked", (st.far.ankle.y - st.near.ankle.y) - Body.SHIN_WIDTH, -0.2..0.2)
        hasContact(n, BodyPoint.FAR_FOOT, Surface.FLOOR)
    }

    @Test
    fun clamshell_hips45Knees90_topKneeOpens_feetStayTogether() {
        val n = "Clamshell"
        // The START caption names what the front view shows (the knee bend is hidden in depth).
        assertEquals("Feet together", figure(n).startCaption)
        assertTrue(props(n).any { it is MiniBand && it.at == BandAt.KNEES })
        for (w in Which.entries) {
            val p = pose(n, w)
            // Hips bent 45° (the thighs out of the picture towards the camera), both legs.
            // (The opening turns the thigh about the hip-to-foot line, which takes a little off the flexion.)
            inRange(n, "near hip flexion ($w)", JointLimits.trueFrontHipFlexion(p.nearLeg, 0.0), 40.0..47.0)
            inRange(n, "far hip flexion ($w)", JointLimits.trueFrontHipFlexion(p.farLeg, 0.0), 43.0..47.0)
        }
        val st = s(n, Which.START)
        val e = s(n, Which.END)
        inRange(n, "feet together", (st.near.ankle - st.far.ankle).length(), 10.0..13.0)
        inRange(n, "feet stay together", (e.near.ankle - st.near.ankle).length(), 0.0..0.5)
        inRange(n, "top knee lifted", st.near.knee.y - e.near.knee.y, 12.0..25.0)
        // The knee's true 3D bend stays 90°: the thigh and the shin keep their lengths, so the knee lies on the hip-foot circle.
        val p = pose(n, Which.END).nearLeg
        inRange(n, "opened, not lifted from the hip", JointLimits.trueAbduction(p, View.FRONT), 10.0..30.0)
    }
}
