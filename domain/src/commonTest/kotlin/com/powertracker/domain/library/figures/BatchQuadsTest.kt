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

/** The quad batch (Task 10): every quad exercise, each with the key positions its guide gives. */
class BatchQuadsTest {
    private fun figure(name: String): ExerciseFigure = assertNotNull(Figures.all[name], "no figure for $name")

    private fun s(name: String, which: Which) = figure(name).let { it.scene(which).pose.skeleton(it.view) }

    private fun pose(name: String, which: Which) = figure(name).scene(which).pose

    private fun props(name: String, which: Which = Which.START) = figure(name).scene(which).props

    private fun hasContact(name: String, point: BodyPoint, surface: Surface, at: Set<Which> = Which.entries.toSet()) =
        assertTrue(figure(name).contacts.any { it.point == point && it.surface == surface && it.at == at }, "$name: $point on $surface at $at")

    private fun hasCheck(name: String, check: KeyCheck) = assertTrue(check in figure(name).checks, "$name: $check in ${figure(name).checks}")

    private fun inRange(name: String, label: String, value: Double, range: ClosedFloatingPointRange<Double>) =
        assertTrue(value in range, "$name: $label ${round1(value)} not in $range")

    private fun angle(v: Point) = atan2(-v.y, v.x) * 180 / PI

    /** The shin's lean forward of vertical, degrees (+: the knee in front of the ankle). */
    private fun shinLean(l: LimbPoints) = angle(l.knee - l.ankle).let { 90.0 - it }

    /** The thigh's angle below level from the hip to the knee, degrees (+: the knee lower than the hip). */
    private fun thighBelowLevel(l: LimbPoints) = (l.knee - l.hip).let { atan2(it.y, abs(it.x)) * 180 / PI }

    private fun knees(name: String, which: Which, range: ClosedFloatingPointRange<Double>, side: Side? = null) {
        val p = pose(name, which)
        if (side != Side.FAR) inRange(name, "near knee ($which)", p.nearLeg.knee, range)
        if (side != Side.NEAR) inRange(name, "far knee ($which)", p.farLeg.knee, range)
    }

    /** Feet flat: the foot's heel-to-toe line level (both sides), within 1°. */
    private fun feetFlat(name: String, which: Which, sides: List<Side> = Side.entries) {
        val sk = s(name, which)
        for (side in sides) inRange(name, "${side.label} foot tilt ($which)", abs(angle(sk.side(side).toe - sk.side(side).heel)), 0.0..1.0)
    }

    /** Standing tall: both knees straight, the torso upright (within [lean]°). */
    private fun standingTall(name: String, lean: Double = 4.0) {
        knees(name, Which.START, 0.0..1.0)
        inRange(name, "standing torso", abs(pose(name, Which.START).torso - 90.0), 0.0..lean)
        feetFlat(name, Which.START)
    }

    @Test
    fun quads_everyExerciseHasAFigure_thatPassesItsChecks() {
        val area = Library.all().filter { it.area == BodyArea.QUADS }.map { it.name }
        assertEquals(28, area.size)
        val problems = area.flatMap { name -> Figures.all[name]?.let { FigureChecks.check(name, it) } ?: listOf(FigureProblem(name, null, "no figure")) }
        assertEquals(emptyList(), problems.map { "${it.name} ${it.which}: ${it.message}" })
        val front = setOf("Lateral lunge", "Curtsy lunge", "Lateral step-up")
        for (name in area) assertEquals(if (name in front) View.FRONT else View.SIDE, figure(name).view, name)
    }

    // ---- free squats ----

    /** A squat's bottom: depth, both feet flat, the knees over the toes (shins forward), the torso leaning [torso]. */
    private fun assertSquatBottom(n: String, depth: Double, torso: ClosedFloatingPointRange<Double>) {
        hasCheck(n, HipDepth(depth, Which.END))
        hasContact(n, BodyPoint.FEET, Surface.FLOOR)
        feetFlat(n, Which.END)
        val e = s(n, Which.END)
        inRange(n, "shins forward (knees over the feet)", shinLean(e.near), 25.0..35.0)
        inRange(n, "knee in front of the toes by at most", e.near.knee.x - e.near.toe.x, -15.0..8.0)
        inRange(n, "hip below the knee", e.near.hip.y - e.near.knee.y, depth..depth + 3.0)
        inRange(n, "torso above level", pose(n, Which.END).torso, torso)
        knees(n, Which.END, 115.0..130.0)
    }

    @Test
    fun bodyweightSquat_thighsAboutLevel_armsForward() {
        val n = "Bodyweight squat"
        standingTall(n)
        assertSquatBottom(n, 0.0, 50.0..65.0)
        val e = s(n, Which.END)
        // The arms straight out in front, level (ACE: a counterweight).
        inRange(n, "arms level", abs(angle(e.near.wrist - e.near.shoulder)), 0.0..3.0)
        inRange(n, "arms straight", pose(n, Which.END).nearArm.elbow, 0.0..1.0)
    }

    @Test
    fun gobletSquat_dumbbellUprightAgainstTheChest_elbowsDown() {
        val n = "Goblet squat"
        standingTall(n)
        assertSquatBottom(n, 2.0, 50.0..65.0)
        for (w in Which.entries) {
            val db = props(n, w).filterIsInstance<Dumbbell>().single()
            assertTrue(db.neutral && !db.squareToForearm, "$n: the dumbbell upright, along the torso")
            val sk = s(n, w)
            // The dumbbell's heads (10 cm from its centre) against the chest: its centre 20..26 cm in front of the axis.
            val c = db.center(sk)
            val front = (c - sk.pelvis).let { it.x * dir(sk.torso - 90.0).x + it.y * dir(sk.torso - 90.0).y }
            inRange(n, "dumbbell in front of the torso's axis ($w)", front, 20.0..26.0)
            inRange(n, "dumbbell below the shoulder ($w)", c.y - sk.shoulder.y, 8.0..28.0)
            // Elbows down by the ribs: below the shoulder, close to the torso's front.
            inRange(n, "elbow below the shoulder ($w)", sk.near.elbow.y - sk.shoulder.y, 15.0..33.0)
        }
    }

    @Test
    fun dbSquat_dumbbellsHangAtTheSides_overTheFeet() {
        val n = "DB squat"
        standingTall(n)
        assertSquatBottom(n, 2.0, 50.0..65.0)
        for (w in Which.entries) {
            val sk = s(n, w)
            inRange(n, "arm hangs straight down ($w)", abs(fold(angle(sk.near.wrist - sk.near.shoulder) + 90.0)), 0.0..1.0)
            assertEquals(2, props(n, w).filterIsInstance<Dumbbell>().count { it.neutral && it.squareToForearm }, "$n: palms in")
            // The dumbbells hang over the feet, clear of the floor.
            inRange(n, "dumbbell over the foot ($w)", sk.near.grip.x - sk.near.heel.x, 0.0..25.0)
            inRange(n, "dumbbell off the floor ($w)", -sk.near.grip.y - Dumbbell.HEAD_RADIUS, 5.0..80.0)
        }
    }

    @Test
    fun kettlebellFrontSquat_bellsRackedAtTheShoulders() {
        val n = "Kettlebell front squat"
        standingTall(n)
        assertSquatBottom(n, 2.0, 50.0..65.0)
        for (w in Which.entries) {
            assertTrue(props(n, w).filterIsInstance<Kettlebell>().all { it.hold == KettlebellHold.RACK }, n)
            val sk = s(n, w)
            // The hand in front of the shoulder, about its height; the elbow down close to the body.
            inRange(n, "hand at shoulder height ($w)", sk.near.grip.y - sk.shoulder.y, -8.0..14.0)
            inRange(n, "elbow below the shoulder ($w)", sk.near.elbow.y - sk.shoulder.y, 20.0..33.0)
            assertEquals(0.0, pose(n, w).nearArm.wrist, "$n: wrists straight")
        }
    }

    @Test
    fun zercherSquat_barInTheElbowCrooks_overMidfoot_upright_inTheRack() {
        val n = "Zercher squat"
        hasCheck(n, BarOverMidfoot(Which.START))
        hasCheck(n, BarOverMidfoot(Which.END))
        assertSquatBottom(n, 0.0, 45.0..60.0)
        knees(n, Which.START, 0.0..1.0)
        for (w in Which.entries) {
            val bar = props(n, w).filterIsInstance<Barbell>().single()
            assertEquals(BarHold.CROOK, bar.on, n)
            val sk = s(n, w)
            val c = bar.center(sk)
            // In the crook: a forearm's half width and the shaft from the elbow, above it (the forearm rises).
            inRange(n, "bar from the elbow ($w)", (c - sk.near.elbow).length(), 5.0..8.0)
            inRange(n, "forearm up from the elbow ($w)", angle(sk.near.wrist - sk.near.elbow) - sk.torso, -30.0..0.0)
            // Close against the body: the shaft within 3 cm of the torso's front (12.5 cm out from its axis).
            val front = (c - sk.pelvis).let { it.x * dir(sk.torso - 90.0).x + it.y * dir(sk.torso - 90.0).y }
            inRange(n, "bar against the body ($w)", front - Body.CHEST_DEPTH - Barbell.SHAFT_RADIUS, -3.0..3.0)
        }
        val rack = props(n).filterIsInstance<PowerRack>().single()
        val low = props(n, Which.END).filterIsInstance<Barbell>().single().center(s(n, Which.END))
        inRange(n, "pins under the bar's lowest point", -low.y - rack.safetyHeight, 5.0..10.0)
    }

    @Test
    fun singleLegBoxSquat_freeLegOutInFront_offTheFloor_buttocksOnTheBox() {
        val n = "Single-leg box squat"
        hasContact(n, BodyPoint.NEAR_FOOT, Surface.FLOOR)
        hasContact(n, BodyPoint.HIPS, Surface.BOX, setOf(Which.END))
        knees(n, Which.START, 0.0..1.0, Side.NEAR)
        for (w in Which.entries) {
            val sk = s(n, w)
            // The free foot off the floor and in front of the working foot, plainly apart.
            inRange(n, "free foot off the floor ($w)", -maxOf(sk.far.heel.y, sk.far.toe.y) - Body.FOOT_WIDTH / 2, 3.0..40.0)
            inRange(n, "free ankle in front of the working one ($w)", sk.far.ankle.x - sk.near.ankle.x, 15.0..90.0)
            // The arms straight out in front, level.
            inRange(n, "arms level ($w)", abs(angle(sk.near.wrist - sk.near.shoulder)), 0.0..3.0)
        }
        inRange(n, "free leg straight at the bottom", pose(n, Which.END).farLeg.knee, 0.0..5.0)
        inRange(n, "free leg's start bend", pose(n, Which.START).farLeg.knee, 0.0..20.0)
        val e = s(n, Which.END)
        inRange(n, "working thigh about level", thighBelowLevel(e.near), -3.0..3.0)
        knees(n, Which.END, 100.0..125.0, Side.NEAR)
        val box = props(n).filterIsInstance<SquatBox>().single()
        inRange(n, "box no higher than the knee", box.height, 30.0..50.0)
        inRange(n, "box behind the working foot", e.near.heel.x - (box.x + box.depth / 2), 0.0..30.0)
    }

    @Test
    fun jumpSquat_startHipsBackKnees90_endInTheAirLegsStraight() {
        val n = "Jump squat"
        hasContact(n, BodyPoint.FEET, Surface.FLOOR, setOf(Which.START))
        assertTrue(figure(n).contacts.none { Which.END in it.at }, "$n: no contact in the air")
        hasCheck(n, FeetOffFloor(25.0, Which.END))
        knees(n, Which.START, 85.0..95.0)
        feetFlat(n, Which.START)
        inRange(n, "hips back of the heels", s(n, Which.START).let { it.near.heel.x - it.pelvis.x }, 5.0..30.0)
        knees(n, Which.END, 0.0..1.0)
        val air = s(n, Which.END)
        inRange(n, "feet off the floor", -maxOf(air.near.heel.y, air.near.toe.y) - Body.FOOT_WIDTH / 2, 30.0..50.0)
        inRange(n, "toes pointed", pose(n, Which.END).nearLeg.ankle, -45.0..-15.0)
        inRange(n, "upright in the air", abs(pose(n, Which.END).torso - 90.0), 0.0..1.0)
        // Arms at the sides at the start (hanging); swung forward and up in the air (ACE), straight.
        s(n, Which.START).let { inRange(n, "arm hangs (START)", it.near.wrist.y - it.shoulder.y, 50.0..70.0) }
        s(n, Which.END).let { inRange(n, "arms forward and up", angle(it.near.wrist - it.near.shoulder), 0.0..30.0) }
        assertEquals(0.0, pose(n, Which.END).nearArm.elbow)
        // The FeetOffFloor check itself: the START (on the floor) would fail it.
        val onFloor = figure(n).copy(checks = listOf(FeetOffFloor(10.0, Which.START)))
        assertTrue(FigureChecks.check(n, onFloor).any { it.message.startsWith("feet") }, "$n: FeetOffFloor fails a foot on the floor")
    }

    // ---- Smith machine ----

    private fun assertOnTheRails(n: String) {
        hasCheck(n, BarOnRails(Which.START))
        hasCheck(n, BarOnRails(Which.END))
        val smith = props(n).filterIsInstance<SmithMachine>().single()
        val low = props(n, Which.END).filterIsInstance<Barbell>().single().center(s(n, Which.END))
        inRange(n, "stops just under the lowest position", -low.y - Barbell.SHAFT_RADIUS - smith.stopHeight, 2.0..6.0)
    }

    @Test
    fun smithSquats_barOnTheRails_overTheFeet_depth_stopsBelow() {
        for (n in listOf("Smith squat", "Smith front squat")) {
            assertOnTheRails(n)
            hasCheck(n, BarOverMidfoot(Which.START))
            hasCheck(n, BarOverMidfoot(Which.END))
            knees(n, Which.START, 0.0..1.0)
        }
        assertSquatBottom("Smith squat", 2.0, 40.0..50.0)
        inRange("Smith squat", "standing torso", pose("Smith squat", Which.START).torso, 80.0..90.0)
        inRange("Smith squat", "standing legs' lean", pose("Smith squat", Which.START).nearLeg.ankle, 0.0..10.0)
        assertEquals(BarHold.UPPER_BACK, props("Smith squat").filterIsInstance<Barbell>().single().on)
        assertSquatBottom("Smith front squat", 0.0, 65.0..80.0)
        val f = s("Smith front squat", Which.START)
        assertEquals(BarHold.FRONT_RACK, props("Smith front squat").filterIsInstance<Barbell>().single().on)
        // Elbows forward: the upper arm level, in front of the shoulder.
        inRange("Smith front squat", "elbow in front of the shoulder", f.near.elbow.x - f.shoulder.x, 25.0..35.0)
    }

    // ---- machines ----

    @Test
    fun hackSquat_backAndShouldersOnThePads_feetOnThePlate_kneesJustShortOfFull() {
        val n = "Hack squat"
        hasContact(n, BodyPoint.UPPER_BACK, Surface.BACK_PAD)
        hasContact(n, BodyPoint.HIPS, Surface.BACK_PAD)
        hasContact(n, BodyPoint.FEET, Surface.FOOT_PLATE)
        knees(n, Which.START, 0.0..8.0)
        knees(n, Which.END, 120.0..132.0)
        for (w in Which.entries) {
            val m = props(n, w).filterIsInstance<HackSquatMachine>().single()
            val sk = s(n, w)
            // Lying back along the 45° rail; the shoulder pad over the top of the shoulder.
            inRange(n, "torso along the rail ($w)", sk.torso, 134.0..136.0)
            inRange(n, "shoulder pad over the shoulder ($w)", (m.shoulderPad() - sk.shoulder).length(), 7.0..14.0)
            assertEquals(sk.pelvis, m.pelvis, "$n: the pad holds the hips")
            // The hands on the handles by the hips, the same place on the body in both panels.
            val handle = sk.pelvis + dir(sk.torso) * 12.0 + dir(sk.torso - 90.0) * 12.0
            inRange(n, "hand on the handle by the hip ($w)", (sk.near.grip - handle).length(), 0.0..0.5)
            // The plate fixed, the feet flat on it (the foot's line along the plate's face).
            inRange(n, "foot along the plate ($w)", abs(fold(angle(sk.near.toe - sk.near.heel) - m.angle)), 0.0..1.0)
        }
        assertEquals(props(n).filterIsInstance<HackSquatMachine>().single().origin, props(n, Which.END).filterIsInstance<HackSquatMachine>().single().origin)
        // The hips slide down the rail: lower and further forward at the bottom.
        val a = s(n, Which.START).pelvis
        val b = s(n, Which.END).pelvis
        inRange(n, "hips move along the rail", abs(fold(angle(a - b) - 135.0)), 0.0..1.0)
        inRange(n, "rail travel", (a - b).length(), 30.0..70.0)
    }

    @Test
    fun leverSquat_yokeOnTheShoulders_overTheFeet_leverKeepsItsLength() {
        val n = "Lever squat"
        hasContact(n, BodyPoint.FEET, Surface.FLOOR)
        assertSquatBottom(n, 2.0, 40.0..55.0)
        knees(n, Which.START, 0.0..1.0)
        val ms = Which.entries.map { props(n, it).filterIsInstance<LeverSquatMachine>().single() }
        assertEquals(ms[0].pivot, ms[1].pivot)
        inRange(n, "lever the same length", abs((ms[0].pad - ms[0].pivot).length() - (ms[1].pad - ms[1].pivot).length()), 0.0..0.01)
        for ((i, w) in Which.entries.withIndex()) {
            val sk = s(n, w)
            // The yoke on the shoulders, behind the neck; directly over the middle of the foot (the feet under the shoulders).
            inRange(n, "yoke at the shoulder ($w)", (ms[i].pad - sk.shoulder).length(), 10.0..15.0)
            inRange(n, "yoke over the mid-foot ($w)", abs(ms[i].pad.x - (sk.near.heel.x + sk.near.toe.x) / 2), 0.0..1.0)
            inRange(n, "pivot behind the lifter ($w)", sk.near.heel.x - ms[i].pivot.x, 80.0..200.0)
        }
    }

    @Test
    fun legExtensions_backOnThePad_kneesAtThePivot_rollerOnTheLowerShin_hangingToStraight() {
        for (n in listOf("Leg extension", "Single-leg leg extension")) {
            hasContact(n, BodyPoint.HIPS, Surface.SEAT)
            hasContact(n, BodyPoint.UPPER_BACK, Surface.BACK_PAD)
            hasContact(n, BodyPoint.SHINS, Surface.SHIN_PAD)
            knees(n, Which.START, 88.0..92.0)
            knees(n, Which.END, 0.0..5.0, Side.NEAR)
            for (w in Which.entries) {
                val m = props(n, w).filterIsInstance<LegExtensionMachine>().single()
                val sk = s(n, w)
                assertEquals(sk.near.knee, m.pivot, "$n: knee at the pivot ($w)")
                // The thighs along the seat: level within 5°, the thigh's underside within 4 cm of the seat top.
                inRange(n, "thigh level ($w)", thighBelowLevel(sk.near), -1.0..5.0)
                inRange(n, "thigh on the seat ($w)", -m.seatHeight - (sk.near.hip.y + Body.THIGH_WIDTH / 2), 0.0..4.0)
                // The roller on the front of the lower shin: 70..90% of the way down, in front of it.
                val along = ((m.roller - sk.near.knee).length()) / Segment.SHIN.length
                inRange(n, "roller low on the shin ($w)", along, 0.8..1.0)
                inRange(n, "feet clear of the floor ($w)", -maxOf(sk.near.heel.y, sk.near.toe.y), 5.0..120.0)
                inRange(n, "back reclined ($w)", sk.torso - 90.0, 5.0..20.0)
            }
        }
        // Both legs together on the two-leg machine; apart at the end on the alternating one.
        assertEquals(pose("Leg extension", Which.END).nearLeg, pose("Leg extension", Which.END).farLeg)
        knees("Single-leg leg extension", Which.END, 88.0..92.0, Side.FAR)
        val m = props("Single-leg leg extension", Which.END).filterIsInstance<LegExtensionMachine>().single()
        assertNotNull(m.farRoller, "the far leg's own pad")
        assertEquals(null, props("Leg extension", Which.END).filterIsInstance<LegExtensionMachine>().single().farRoller)
        // At thumbnail size the two ankles are far apart (the free leg plainly down).
        val e = s("Single-leg leg extension", Which.END)
        inRange("Single-leg leg extension", "ankles apart", (e.near.ankle - e.far.ankle).length(), 50.0..80.0)
    }

    @Test
    fun singleLegLegPress_oneFootOnThePlate_theOtherOnTheFloor_kneesAsTheLegPress() {
        val n = "Single-leg leg press"
        hasContact(n, BodyPoint.NEAR_FOOT, Surface.FOOT_PLATE)
        hasContact(n, BodyPoint.FAR_FOOT, Surface.FLOOR)
        hasContact(n, BodyPoint.UPPER_BACK, Surface.BACK_PAD)
        hasContact(n, BodyPoint.HIPS, Surface.SEAT)
        // The working leg as the approved Leg press's, in both panels.
        for (w in Which.entries) {
            assertEquals(pose("Leg press", w).nearLeg, pose(n, w).nearLeg, "$n: the Leg press's working leg ($w)")
            assertEquals(figure("Leg press").scene(w).props, props(n, w), "$n: the Leg press's machine ($w)")
        }
        knees(n, Which.START, 10.0..20.0, Side.NEAR)
        knees(n, Which.END, 80.0..90.0, Side.NEAR)
        for (w in Which.entries) {
            knees(n, w, 65.0..95.0, Side.FAR)
            feetFlat(n, w, listOf(Side.FAR))
            val sk = s(n, w)
            // The free foot on the floor in front of the seat, plainly apart from the one on the plate.
            inRange(n, "feet apart ($w)", (sk.near.ankle - sk.far.ankle).length(), 70.0..160.0)
        }
    }

    // ---- split stances ----

    /** A lunge or split squat's bottom: front shin near upright, front thigh about level, back knee just off the floor. */
    private fun assertSplitBottom(n: String, which: Which = Which.END, front: Side = Side.NEAR, step: ClosedFloatingPointRange<Double> = 80.0..100.0, backHip: ClosedFloatingPointRange<Double> = -20.0..-15.0) {
        val back = if (front == Side.NEAR) Side.FAR else Side.NEAR
        val sk = s(n, which)
        val f = sk.side(front)
        val b = sk.side(back)
        inRange(n, "front shin near upright", shinLean(f), 0.0..15.0)
        inRange(n, "front thigh about level (the knee a little below the hip)", thighBelowLevel(f), -3.0..8.0)
        inRange(n, "back knee just above the floor (its round)", -b.knee.y - Body.SHIN_WIDTH / 2, 2.0..7.0)
        inRange(n, "back knee under the hip", sk.pelvis.x - b.knee.x, 0.0..20.0)
        inRange(n, "torso upright", abs(sk.torso - 90.0), 0.0..1.0)
        val p = pose(n, which)
        val fl = if (front == Side.NEAR) p.nearLeg else p.farLeg
        val bl = if (front == Side.NEAR) p.farLeg else p.nearLeg
        inRange(n, "front knee bend", fl.knee, 85.0..105.0)
        inRange(n, "back knee bend", bl.knee, 65.0..100.0)
        inRange(n, "back hip extension", bl.hip, backHip)
        // The back heel up, the toes on the floor; the front foot flat.
        inRange(n, "back heel up", -b.heel.y - Body.FOOT_WIDTH / 2, 14.0..25.0)
        // The shin slopes up from the knee to the raised heel (not lying along the floor: kneeling), and the toes point
        // forward under it, on the floor.
        inRange(n, "back shin slopes up to the heel", angle(b.ankle - b.knee).let { 180.0 - it }, 12.0..25.0)
        inRange(n, "back toes forward of the ankle", b.toe.x - b.ankle.x, 1.0..10.0)
        inRange(n, "back toes on the floor", -b.toe.y - Body.FOOT_WIDTH / 2, -0.5..1.5)
        inRange(n, "front foot flat", abs(angle(f.toe - f.heel)), 0.0..1.0)
        // A long step: the front ankle well ahead of the back toes.
        inRange(n, "step, ankle to ankle", f.ankle.x - b.ankle.x, step)
    }

    @Test
    fun lunges_frontThighLevel_backKneeJustUp_stepForwardOrBack() {
        for (n in listOf("Lunge", "Reverse lunge", "Barbell lunge", "Barbell reverse lunge")) {
            hasContact(n, BodyPoint.FEET, Surface.FLOOR)
            standingTall(n, lean = 0.5)
            assertSplitBottom(n)
        }
        // Lunge: the back foot stays where the feet were (its toes), the front foot steps ahead.
        for (n in listOf("Lunge", "Barbell lunge")) {
            val a = s(n, Which.START)
            val b = s(n, Which.END)
            inRange(n, "back toes where the feet were", abs(b.far.toe.x - a.near.toe.x), 0.0..1.0)
            inRange(n, "front foot steps forward", b.near.ankle.x - a.near.ankle.x, 70.0..140.0)
        }
        // Reverse: the front foot stays, the back foot steps back.
        for (n in listOf("Reverse lunge", "Barbell reverse lunge")) {
            val a = s(n, Which.START)
            val b = s(n, Which.END)
            inRange(n, "front foot stays", abs(b.near.ankle.x - a.near.ankle.x), 0.0..1.0)
            inRange(n, "back foot steps back", a.near.toe.x - b.far.toe.x, 80.0..140.0)
        }
        for (n in listOf("Lunge", "Reverse lunge")) {
            assertEquals(2, props(n).filterIsInstance<Dumbbell>().count { it.neutral && it.squareToForearm }, "$n: palms in at the sides")
        }
    }

    @Test
    fun barbellLunges_barOnTheBack_rackInFront_armsClearOfTheLifter() {
        for (n in listOf("Barbell lunge", "Barbell reverse lunge")) {
            for (w in Which.entries) {
                assertEquals(BarHold.UPPER_BACK, props(n, w).filterIsInstance<Barbell>().single().on)
                val rack = props(n, w).filterIsInstance<PowerRack>().single()
                val sk = s(n, w)
                val tip = rack.x - rack.armLength
                val chest = Which.entries.maxOf { s(n, it).chest().x }
                inRange(n, "safety arm tip just in front of the furthest-forward chest", tip - chest, 5.0..10.0)
                inRange(n, "posts in front of the furthest-forward toes", rack.x - Which.entries.maxOf { s(n, it).let { k -> maxOf(k.near.toe.x, k.far.toe.x) } }, 20.0..30.0)
                val low = props(n, Which.END).filterIsInstance<Barbell>().single().center(s(n, Which.END))
                inRange(n, "pins under the bar's lowest point", -low.y - rack.safetyHeight, 5.0..10.0)
            }
        }
    }

    @Test
    fun walkingLunge_twoLungesOneStepApart_legsSwapped() {
        val n = "Walking lunge"
        hasContact(n, BodyPoint.FEET, Surface.FLOOR)
        assertSplitBottom(n, Which.START, Side.NEAR)
        assertSplitBottom(n, Which.END, Side.FAR)
        val a = s(n, Which.START)
        val b = s(n, Which.END)
        // Travel: the new back toes where the old front toes were; the hips a full step on.
        inRange(n, "new back toes on the old front toes", abs(b.near.toe.x - a.near.toe.x), 0.0..1.0)
        inRange(n, "hips travel", b.pelvis.x - a.pelvis.x, 90.0..140.0)
    }

    @Test
    fun splitSquats_hipsStraightDown_backKneeNearTheFloor() {
        for (n in listOf("Split squat", "Smith split squat")) {
            hasContact(n, BodyPoint.FEET, Surface.FLOOR)
            assertSplitBottom(n, step = 80.0..90.0, backHip = -15.0..-5.0)
            val a = s(n, Which.START)
            val b = s(n, Which.END)
            // The feet stay put, the hips go straight up and down.
            inRange(n, "front foot stays", abs(a.near.ankle.x - b.near.ankle.x), 0.0..0.01)
            inRange(n, "back ankle stays over its spot", abs(a.far.ankle.x - b.far.ankle.x), 0.0..0.5)
            inRange(n, "hips straight up", abs(a.pelvis.x - b.pelvis.x), 0.0..0.01)
            inRange(n, "hips rise", b.pelvis.y - a.pelvis.y, 35.0..50.0)
            // At the top the back foot still on its ball, the back knee still bent, inside the hip's limit.
            val top = pose(n, Which.START)
            inRange(n, "back hip at the top", top.farLeg.hip, -20.0..0.0)
            inRange(n, "back knee at the top", top.farLeg.knee, 10.0..60.0)
            inRange(n, "front knee at the top", top.nearLeg.knee, 5.0..25.0)
        }
        assertOnTheRails("Smith split squat")
    }

    @Test
    fun bulgarianSplitSquat_rearFootOnTheBench_backKneeNearTheFloor() {
        val n = "Bulgarian split squat"
        hasContact(n, BodyPoint.NEAR_FOOT, Surface.FLOOR)
        hasContact(n, BodyPoint.FAR_FOOT, Surface.BENCH)
        val bench = props(n).filterIsInstance<FlatBench>().single()
        assertEquals(40.0, bench.height)
        val e = s(n, Which.END)
        inRange(n, "back knee nearly touching the floor (its round)", -e.far.knee.y - Body.SHIN_WIDTH / 2, 4.0..9.0)
        inRange(n, "front thigh about level", thighBelowLevel(e.near), -3.0..8.0)
        inRange(n, "front shin forward", shinLean(e.near), 5.0..15.0)
        inRange(n, "torso upright", abs(e.torso - 90.0), 0.0..0.01)
        knees(n, Which.END, 110.0..135.0, Side.FAR)
        knees(n, Which.END, 85.0..100.0, Side.NEAR)
        knees(n, Which.START, 5.0..15.0, Side.NEAR)
        for (w in Which.entries) {
            val sk = s(n, w)
            // The ankle just in front of the bench's edge, the foot pointed back over the pad (the limit).
            inRange(n, "ankle past the bench's edge ($w)", sk.far.ankle.x - (bench.x + bench.length), 5.0..9.0)
            inRange(n, "foot pointed ($w)", pose(n, w).farLeg.ankle, -50.0..-45.0)
            inRange(n, "foot points back over the pad ($w)", sk.far.toe.x - sk.far.ankle.x, -25.0..-10.0)
            feetFlat(n, w, listOf(Side.NEAR))
        }
        inRange(n, "front foot stays", abs(s(n, Which.START).near.ankle.x - e.near.ankle.x), 0.0..0.01)
    }

    @Test
    fun sledPush_armsStraight_hipsLow_thenLeaningInHeelsUp() {
        val n = "Sled push"
        hasContact(n, BodyPoint.FEET, Surface.FLOOR)
        hasContact(n, BodyPoint.HANDS, Surface.SLED_HANDLE)
        val a = pose(n, Which.START)
        val b = pose(n, Which.END)
        inRange(n, "torso nearly upright at the set-up", a.torso, 75.0..85.0)
        inRange(n, "leaning into the sled", b.torso, 45.0..65.0)
        for (w in Which.entries) {
            val sk = s(n, w)
            assertEquals(0.0, pose(n, w).nearArm.elbow, "$n: arms straight ($w)")
            inRange(n, "hips low ($w)", -sk.pelvis.y, 60.0..80.0)
            // The back foot back, on its ball.
            inRange(n, "back foot back ($w)", sk.pelvis.x - sk.near.ankle.x, 30.0..70.0)
            inRange(n, "back heel up ($w)", -sk.near.heel.y - Body.FOOT_WIDTH / 2, 3.0..20.0)
            knees(n, w, 10.0..100.0)
        }
        // The front foot flat at the set-up, its heel up when leaning in (both heels off the ground).
        val e = s(n, Which.END)
        inRange(n, "front heel up when leaning in", -e.far.heel.y - Body.FOOT_WIDTH / 2, 3.0..20.0)
        feetFlat(n, Which.START, listOf(Side.FAR))
        // The driving leg nearly straight behind when leaning in.
        inRange(n, "driving leg", b.nearLeg.knee, 10.0..60.0)
    }

    // ---- step-ups ----

    @Test
    fun stepUp_wholeFootOnTheBox_thenStandingTallOnIt() {
        val n = "Step-up"
        hasContact(n, BodyPoint.NEAR_FOOT, Surface.BOX)
        hasContact(n, BodyPoint.FAR_FOOT, Surface.FLOOR, setOf(Which.START))
        hasContact(n, BodyPoint.FAR_FOOT, Surface.BOX, setOf(Which.END))
        val box = props(n).filterIsInstance<SquatBox>().single()
        // No higher than the knee joint standing.
        inRange(n, "box height", box.height, 30.0..(Body.ANKLE_HEIGHT + Segment.SHIN.length))
        for (w in Which.entries) {
            val sk = s(n, w)
            inRange(n, "whole near foot on the box: heel ($w)", sk.near.heel.x - (box.x - box.depth / 2), 3.0..50.0)
            inRange(n, "whole near foot on the box: toe ($w)", (box.x + box.depth / 2) - sk.near.toe.x, 3.0..50.0)
            feetFlat(n, w)
            inRange(n, "torso upright ($w)", abs(sk.torso - 90.0), 0.0..0.01)
        }
        knees(n, Which.START, 95.0..120.0, Side.NEAR)
        knees(n, Which.START, 0.0..5.0, Side.FAR)
        knees(n, Which.END, 0.0..1.0)
        assertEquals(2, props(n).filterIsInstance<Dumbbell>().count { it.neutral && it.squareToForearm }, "$n: palms in")
    }

    // ---- front view ----

    @Test
    fun lateralLunge_stepOutToTheSide_nearShinUpright_farLegStraight() {
        val n = "Lateral lunge"
        hasContact(n, BodyPoint.FEET, Surface.FLOOR)
        val a = s(n, Which.START)
        val b = s(n, Which.END)
        // Hip-width at the start: the ankles under the hips.
        inRange(n, "feet hip-width", a.near.ankle.x - a.far.ankle.x, 18.0..30.0)
        knees(n, Which.START, 0.0..1.0)
        // The far foot stays; the near one steps out to the side.
        inRange(n, "far foot stays", abs(b.far.ankle.x - a.far.ankle.x), 0.0..0.01)
        inRange(n, "step out", b.near.ankle.x - a.near.ankle.x, 60.0..100.0)
        inRange(n, "near shin upright", abs(b.near.knee.x - b.near.ankle.x), 0.0..0.5)
        val e = pose(n, Which.END)
        inRange(n, "near leg bent (drawn)", e.nearLeg.knee, 40.0..80.0)
        inRange(n, "near thigh forward (hips back)", e.nearLeg.out, 30.0..60.0)
        inRange(n, "far leg straight", abs(e.farLeg.knee), 0.0..0.5)
        inRange(n, "far leg in line", abs(e.farLeg.out - e.farLeg.shinOut), 0.0..0.01)
        inRange(n, "hips lowered", b.pelvis.y - a.pelvis.y, 15.0..35.0)
        inRange(n, "hips over the bent leg", b.near.ankle.x - b.pelvis.x, 20.0..50.0)
        inRange(n, "lean (hips back)", e.lean, 25.0..45.0)
        // The weights come down towards the near foot: below the hips.
        inRange(n, "hands below the hips", b.near.grip.y - b.pelvis.y, 5.0..40.0)
        assertTrue(props(n).filterIsInstance<Dumbbell>().all { it.endOnFront }, "$n: palms facing each other, end-on")
    }

    @Test
    fun curtsyLunge_backLegCrossesBehind_frontKneeOverTheHeel() {
        val n = "Curtsy lunge"
        hasContact(n, BodyPoint.FEET, Surface.FLOOR)
        val a = s(n, Which.START)
        val b = s(n, Which.END)
        inRange(n, "feet wide apart", a.near.ankle.x - a.far.ankle.x, 50.0..80.0)
        knees(n, Which.START, 0.0..1.0)
        // The front (near) foot stays, its knee over the heel.
        inRange(n, "front foot stays", abs(b.near.ankle.x - a.near.ankle.x), 0.0..0.01)
        inRange(n, "front knee over the heel", abs(b.near.knee.x - b.near.ankle.x), 0.0..0.5)
        val e = pose(n, Which.END)
        inRange(n, "front thigh forward (knee bent)", e.nearLeg.out, 55.0..85.0)
        // The back (far) leg crosses: its foot past the front foot on the other side, its knee low, behind.
        inRange(n, "back foot past the front foot", b.far.toe.x - b.near.ankle.x, 5.0..30.0)
        inRange(n, "back knee low", -b.far.knee.y, 12.0..30.0)
        inRange(n, "back thigh crosses in (adduction)", e.farLeg.hip, -20.0..-5.0)
        inRange(n, "back thigh behind", e.farLeg.out, -30.0..-1.0)
        inRange(n, "back shin behind", e.farLeg.shinOut, -80.0..-20.0)
        inRange(n, "hips lowered", b.pelvis.y - a.pelvis.y, 20.0..40.0)
        inRange(n, "chest up", e.lean, 0.0..15.0)
    }

    @Test
    fun lateralStepUp_nearFootOnTheBoxBeside_thenStandingOnIt_otherFootOff() {
        val n = "Lateral step-up"
        hasContact(n, BodyPoint.NEAR_FOOT, Surface.BOX)
        hasContact(n, BodyPoint.FAR_FOOT, Surface.FLOOR, setOf(Which.START))
        val box = props(n).filterIsInstance<SquatBox>().single()
        inRange(n, "box height", box.height, 30.0..(Body.ANKLE_HEIGHT + Segment.SHIN.length))
        val a = s(n, Which.START)
        val b = s(n, Which.END)
        // The box beside the near side, the far foot on the floor beside it.
        inRange(n, "box beside the standing leg", (box.x - box.depth / 2) - a.far.ankle.x, 15.0..60.0)
        knees(n, Which.START, 0.0..1.0, Side.FAR)
        val sp = pose(n, Which.START)
        inRange(n, "near foot forward of the standing knee", sp.nearLeg.shinOut, -60.0..-5.0)
        inRange(n, "near knee bent forward", sp.nearLeg.out, 30.0..75.0)
        // END standing on the box on the straight near leg, the far foot off the floor, clear of the box.
        knees(n, Which.END, 0.0..0.5, Side.NEAR)
        inRange(n, "near leg upright", abs(b.near.hip.x - b.near.ankle.x), 0.0..0.5)
        inRange(n, "far foot off the floor", -maxOf(b.far.heel.y, b.far.toe.y) - Body.FOOT_WIDTH / 2, 10.0..60.0)
        inRange(n, "far leg beside the box", (box.x - box.depth / 2) - b.far.ankle.x, 5.0..30.0)
        assertTrue(props(n).filterIsInstance<Dumbbell>().all { it.endOnFront }, "$n: palms in at the sides, end-on")
    }
}
