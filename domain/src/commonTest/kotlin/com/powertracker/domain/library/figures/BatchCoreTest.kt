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

/** The core batch (Task 12): every exercise of the area, each with the key positions its guide gives. */
class BatchCoreTest {
    private fun figure(name: String): ExerciseFigure = assertNotNull(Figures.all[name], "no figure for $name")

    private fun s(name: String, which: Which) = figure(name).let { it.scene(which).pose.skeleton(it.view) }

    private fun pose(name: String, which: Which) = figure(name).scene(which).pose

    private fun props(name: String, which: Which = Which.START) = figure(name).scene(which).props

    private fun inRange(name: String, label: String, value: Double, range: ClosedFloatingPointRange<Double>) =
        assertTrue(value in range, "$name: $label ${round1(value)} not in $range")

    @Test
    fun core_everyExerciseHasAFigure_thatPassesItsChecks() {
        val area = Library.all().filter { it.area == BodyArea.CORE }.map { it.name }
        assertEquals(17, area.size)
        val problems = area.flatMap { name -> Figures.all[name]?.let { FigureChecks.check(name, it) } ?: listOf(FigureProblem(name, null, "no figure")) }
        assertEquals(emptyList(), problems.map { "${it.name} ${it.which}: ${it.message}" })
        val front = setOf("Side plank", "Cable woodchop", "Suitcase carry", "Farmer's walk")
        for (name in area) assertEquals(if (name in front) View.FRONT else View.SIDE, figure(name).view, name)
    }

    private fun angle(v: Point) = atan2(-v.y, v.x) * 180 / PI

    /** The bend between the lines [a]→[b] and [b]→[c], degrees (0: one straight line). */
    private fun bend(a: Point, b: Point, c: Point) = abs(fold(angle(b - a) - angle(c - b)))

    /** The lowest point of a limb's hand or foot above the floor, its round included. */
    private fun handUp(l: LimbPoints) = -maxOf(l.wrist.y, l.hand.y) - Body.HAND_WIDTH / 2
    private fun footUp(l: LimbPoints) = -maxOf(l.heel.y, l.toe.y) - Body.FOOT_WIDTH / 2
    private fun heelUp(l: LimbPoints) = -l.heel.y - Body.FOOT_WIDTH / 2

    /** The thigh's screen angle, hip to knee. */
    private fun thigh(l: LimbPoints) = angle(l.knee - l.hip)

    private fun hipsStill(n: String) {
        val a = s(n, Which.START)
        val b = s(n, Which.END)
        inRange(n, "pelvis still", (a.pelvis - b.pelvis).length(), 0.0..0.01)
        inRange(n, "thighs still", abs(fold(thigh(a.near) - thigh(b.near))), 0.0..0.5)
    }

    private fun distance(p: Point, a: Point, b: Point): Double {
        val ab = b - a
        val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / (ab.x * ab.x + ab.y * ab.y)).coerceIn(0.0, 1.0)
        return (p - (a + ab * t)).length()
    }

    // ---- lying on the back ----

    @Test
    fun floorCrunch_upperBackOff_hipsAndFeetDown_handsBehindTheHead() {
        val n = "Floor crunch"
        hipsStill(n)
        inRange(n, "lying (START)", pose(n, Which.START).torso, 179.9..180.1)
        inRange(n, "torso curled 30 degrees up (END)", 180.0 - pose(n, Which.END).torso, 29.0..31.0)
        inRange(n, "chin tucked (END)", pose(n, Which.END).neck, 10.0..20.0)
        for (w in Which.entries) {
            val sk = s(n, w)
            inRange(n, "hips on the floor ($w)", sk.pelvis.y + Body.TORSO_RADIUS, -0.5..0.5)
            for (side in Side.entries) {
                inRange(n, "${side.label} foot flat ($w)", abs(angle(sk.side(side).toe - sk.side(side).heel)), 0.0..1.0)
                inRange(n, "${side.label} hand at the head ($w)", (sk.side(side).grip - sk.head).length(), 0.0..Body.HEAD_RADIUS)
            }
            inRange(n, "knees bent ($w)", pose(n, w).nearLeg.knee, 70.0..110.0)
            inRange(n, "elbows back, out to the sides ($w)", pose(n, w).nearArm.out, 30.0..90.0)
        }
    }

    @Test
    fun bicycleCrunch_elbowTowardTheOppositeKnee_otherLegStraight_thenSwitch() {
        val n = "Bicycle crunch"
        for (w in Which.entries) {
            val p = pose(n, w)
            val sk = s(n, w)
            inRange(n, "upper body curled ($w)", 180.0 - p.torso, 29.0..31.0)
            inRange(n, "hips on the floor ($w)", sk.pelvis.y + Body.TORSO_RADIUS, -0.5..0.5)
            val (bent, straight) = if (w == Which.START) Side.FAR to Side.NEAR else Side.NEAR to Side.FAR
            val bl = if (bent == Side.NEAR) p.nearLeg else p.farLeg
            val sl = if (straight == Side.NEAR) p.nearLeg else p.farLeg
            inRange(n, "bent knee 90 ($w)", bl.knee, 85.0..95.0)
            inRange(n, "bent thigh just past upright ($w)", thigh(sk.side(bent)), 95.0..110.0)
            inRange(n, "other leg straight ($w)", sl.knee, 0.0..0.5)
            inRange(n, "straight leg off the floor ($w)", thigh(sk.side(straight)), 20.0..40.0)
            inRange(n, "straight foot off the floor ($w)", footUp(sk.side(straight)), 10.0..60.0)
            // The elbow opposite the bent knee swings towards it (forward, towards the feet).
            inRange(n, "opposite elbow forward ($w)", sk.side(straight).elbow.x - sk.side(bent).elbow.x, 5.0..60.0)
        }
    }

    @Test
    fun reverseCrunch_kneesOverHips_thenHipsRolledUp_sameKneeBend() {
        val n = "Reverse crunch"
        val a = s(n, Which.START)
        val b = s(n, Which.END)
        inRange(n, "knees over the hips (START)", abs(a.near.knee.x - a.pelvis.x), 0.0..1.0)
        inRange(n, "hips on the floor (START)", a.pelvis.y + Body.TORSO_RADIUS, -0.5..0.5)
        inRange(n, "hips rolled up off the floor (END)", -(b.pelvis.y + Body.TORSO_RADIUS), 12.0..16.0)
        inRange(n, "thighs towards the ribs (END)", thigh(b.near), 115.0..135.0)
        for (w in Which.entries) inRange(n, "knee bend the same ($w)", pose(n, w).nearLeg.knee, 89.9..90.1)
        inRange(n, "shoulders stay down", (a.shoulder - b.shoulder).length(), 0.0..0.01)
        for (w in Which.entries) inRange(n, "feet off the floor ($w)", footUp(s(n, w).near), 30.0..120.0)
    }

    @Test
    fun deadBug_oneArmAndTheOppositeLegLoweredToALightTouch() {
        val n = "Dead bug"
        val a = s(n, Which.START)
        val b = s(n, Which.END)
        for ((w, sk) in listOf(Which.START to a, Which.END to b)) {
            inRange(n, "lower back flat ($w)", pose(n, w).torso, 179.9..180.1)
            inRange(n, "hips on the floor ($w)", sk.pelvis.y + Body.TORSO_RADIUS, -0.5..0.5)
            inRange(n, "far arm up over the shoulder ($w)", abs(sk.far.wrist.x - sk.far.shoulder.x), 0.0..1.0)
            inRange(n, "near knee over the hip ($w)", abs(sk.near.knee.x - sk.pelvis.x), 0.0..1.0)
            inRange(n, "near knee 90 ($w)", pose(n, w).nearLeg.knee, 89.9..90.1)
        }
        inRange(n, "near arm up (START)", abs(a.near.wrist.x - a.near.shoulder.x), 0.0..1.0)
        inRange(n, "far knee over the hip (START)", abs(a.far.knee.x - a.pelvis.x), 0.0..1.0)
        inRange(n, "near hand overhead (END)", b.head.x - b.near.hand.x, 10.0..80.0)
        inRange(n, "near fingertips just off the floor (END)", handUp(b.near), 1.0..3.0)
        inRange(n, "near arm level overhead (END)", pose(n, Which.END).nearArm.shoulder, 179.9..180.1)
        inRange(n, "far leg straight (END)", pose(n, Which.END).farLeg.knee, 0.0..0.5)
        inRange(n, "far heel just off the floor (END)", footUp(b.far), 1.0..3.0)
    }

    // ---- planks ----

    @Test
    fun frontPlank_straightLineHeelsToHead_elbowsUnderShoulders() {
        val n = "Front plank"
        val e = s(n, Which.END)
        val p = pose(n, Which.END)
        inRange(n, "shoulders, hips and ankles in line", bend(e.shoulder, e.pelvis, e.near.ankle), 0.0..1.0)
        inRange(n, "hips straight (no sag, no pike)", abs(p.nearLeg.hip), 0.0..0.1)
        inRange(n, "knees straight", p.nearLeg.knee, 0.0..0.1)
        inRange(n, "head in line", abs(p.neck), 0.0..0.1)
        inRange(n, "elbows under the shoulders", abs(e.near.elbow.x - e.shoulder.x), 0.0..0.5)
        inRange(n, "forearms along the floor", abs(e.near.wrist.y - e.near.elbow.y), 0.0..0.5)
        inRange(n, "toes on the floor", footUp(e.near), -0.5..0.5)
        inRange(n, "body low and long", p.torso, 5.0..25.0)
        val st = s(n, Which.START)
        inRange(n, "lying: chest and hips down (START)", st.pelvis.y + Body.TORSO_RADIUS, -0.5..0.5)
        inRange(n, "lying: torso level (START)", abs(pose(n, Which.START).torso), 0.0..0.1)
        inRange(n, "elbow below the shoulder (START)", abs(st.near.elbow.x - st.shoulder.x), 0.0..0.5)
        inRange(n, "toes tucked on the floor (START)", footUp(st.near), -0.5..0.5)
    }

    @Test
    fun sidePlank_frontView_hipsDownThenUp_oneLineHeadToHeels_elbowUnderShoulder() {
        val n = "Side plank"
        val a = s(n, Which.START)
        val b = s(n, Which.END)
        val p = pose(n, Which.END)
        inRange(n, "body in one line (END)", abs(p.farLeg.hip), 0.0..0.1)
        inRange(n, "head in line (END)", abs(p.neck), 0.0..0.1)
        inRange(n, "bottom leg straight on from the body (END)", bend(b.shoulder, b.pelvis, b.pelvis + (b.far.ankle - b.far.hip)), 0.0..1.0)
        for ((w, sk) in listOf(Which.START to a, Which.END to b)) {
            inRange(n, "bottom elbow on the floor ($w)", sk.far.elbow.y + Body.UPPER_ARM_WIDTH / 2, -0.5..0.5)
            inRange(n, "bottom elbow under the bottom shoulder ($w)", abs(sk.far.elbow.x - sk.far.shoulder.x), 0.0..0.5)
            inRange(n, "feet stacked ($w)", (sk.far.ankle.y - sk.near.ankle.y) - Body.SHIN_WIDTH, -0.5..0.5)
            inRange(n, "legs straight ($w)", pose(n, w).nearLeg.knee + pose(n, w).farLeg.knee, 0.0..0.1)
        }
        assertEquals(0.0, p.farArm.out, "END: the upper arm upright in the picture, under the shoulder")
        // START: the bottom upper arm points out of the picture towards the camera (the shoulder is lower than the arm's
        // length), the forearm on the floor towards the camera; a wrong-way arm (away from it) fails.
        val p0 = pose(n, Which.START)
        inRange(n, "bottom upper arm towards the camera (START)", p0.farArm.out, 20.0..80.0)
        inRange(n, "bottom forearm towards the camera (START)", p0.farArm.forearmOut, 79.9..80.1)
        inRange(n, "bottom forearm towards the camera (END)", p.farArm.forearmOut, 79.9..80.1)
        inRange(n, "hips lifted", a.pelvis.y - b.pelvis.y, 10.0..40.0)
        inRange(n, "hips off the floor (END)", -b.pelvis.y - 16.0, 8.0..40.0)
    }

    // ---- kneeling ----

    @Test
    fun birdDog_oppositeArmAndLegInLineWithTheBack() {
        val n = "Bird dog"
        val a = s(n, Which.START)
        val b = s(n, Which.END)
        val p = pose(n, Which.END)
        inRange(n, "back slope", p.torso, 3.0..15.5)
        inRange(n, "knees under the hips", abs(a.near.knee.x - a.pelvis.x), 0.0..1.0)
        inRange(n, "hands under the shoulders", abs(a.near.wrist.x - a.shoulder.x), 0.0..1.5)
        inRange(n, "far hip extended no more than the back's slope", -p.farLeg.hip, 0.0..p.torso + 0.1)
        inRange(n, "far knee straight", p.farLeg.knee, 0.0..0.1)
        inRange(n, "far foot raised", footUp(b.far), 20.0..80.0)
        inRange(n, "far leg level", abs(fold(thigh(b.far) - 180.0)), 0.0..0.5)
        inRange(n, "near arm level", abs(angle(b.near.wrist - b.near.shoulder)), 0.0..0.5)
        inRange(n, "near hand raised", handUp(b.near), 25.0..80.0)
        inRange(n, "far hand still down", handUp(b.far), -0.5..1.5)
        inRange(n, "hips stay put", (a.pelvis - b.pelvis).length(), 0.0..0.01)
    }

    @Test
    fun abWheelRollout_fromTheKnees_armsNearlyStraight_longLineAtTheReach() {
        val n = "Ab wheel rollout"
        val a = s(n, Which.START)
        val b = s(n, Which.END)
        assertTrue(props(n).any { it == AbWheel })
        inRange(n, "wheel near the front of the knees (START)", a.near.grip.x - a.near.knee.x, 20.0..35.0)
        inRange(n, "shoulders over the wheel (START)", abs(a.shoulder.x - a.near.grip.x), 0.0..8.0)
        inRange(n, "arms straight (START)", pose(n, Which.START).nearArm.elbow, 0.0..12.0)
        inRange(n, "hips sat back behind the knees (START)", a.near.knee.x - a.pelvis.x, 5.0..30.0)
        inRange(n, "long line from the knees (END)", abs(pose(n, Which.END).nearLeg.hip), 0.0..0.1)
        inRange(n, "body 20 degrees up (END)", pose(n, Which.END).torso, 19.9..20.1)
        inRange(n, "arms nearly straight (END)", pose(n, Which.END).nearArm.elbow, 0.0..10.0)
        inRange(n, "wheel rolled out", b.near.grip.x - a.near.grip.x, 60.0..150.0)
        for ((w, sk) in listOf(Which.START to a, Which.END to b)) {
            inRange(n, "wheel on the floor ($w)", sk.near.grip.y + AbWheel.RADIUS, -0.1..0.1)
            inRange(n, "hand clear of the floor ($w)", handUp(sk.near), 0.0..30.0)
        }
    }

    @Test
    fun stabilityBallRollout_forearmsOnTheBall_hipsInLine_ballPushedOut() {
        val n = "Stability ball rollout"
        val a = s(n, Which.START)
        val b = s(n, Which.END)
        val r = StabilityBall.DIAMETER / 2
        val balls = Which.entries.map { props(n, it).filterIsInstance<StabilityBall>().single() }
        for ((i, w) in Which.entries.withIndex()) {
            val sk = s(n, w)
            inRange(n, "hips in line ($w)", abs(pose(n, w).nearLeg.hip), 0.0..0.1)
            val c = Point(balls[i].x, -r)
            inRange(n, "forearm on the ball ($w)", distance(c, sk.near.elbow, sk.near.wrist) - r - Body.FOREARM_WIDTH / 2, -0.3..0.3)
        }
        inRange(n, "shoulders right above the elbows (START)", abs(a.near.elbow.x - a.shoulder.x), 0.0..0.5)
        inRange(n, "arms straight (END)", pose(n, Which.END).nearArm.elbow, 0.0..0.1)
        inRange(n, "ball pushed forward", balls[1].x - balls[0].x, 15.0..80.0)
        inRange(n, "body lower at the reach", pose(n, Which.START).torso - pose(n, Which.END).torso, 10.0..30.0)
    }

    @Test
    fun cableCrunch_kneeling_hipsStill_trunkCurlsDown_elbowsToTheThighs() {
        val n = "Cable crunch"
        hipsStill(n)
        inRange(n, "thighs upright", thigh(s(n, Which.START).near), -91.0..-89.0)
        inRange(n, "trunk upright (START)", pose(n, Which.START).torso, 70.0..80.0)
        inRange(n, "trunk curled (END)", pose(n, Which.END).torso, 20.0..30.0)
        val e = s(n, Which.END)
        val st = s(n, Which.START)
        // The rigid torso cannot curl, so the elbows come towards the thighs, not onto them: plainly lower and nearer.
        inRange(n, "elbows come down (END)", e.near.elbow.y - st.near.elbow.y, 25.0..80.0)
        inRange(n, "elbows nearer the thighs (END)", (st.near.elbow - st.near.knee).length() - (e.near.elbow - e.near.knee).length(), 15.0..80.0)
        for (w in Which.entries) inRange(n, "rope by the head ($w)", (s(n, w).near.grip - s(n, w).head).length(), 0.0..14.0)
        assertTrue(props(n).any { it is CableAttachment && it.kind == Attachment.ROPE })
        val stack = props(n).filterIsInstance<CableStack>().single()
        // The head and the rope clear the column (its left edge 9 cm before its middle) in both panels.
        for (w in Which.entries) {
            val sk = s(n, w)
            inRange(n, "head clear of the column ($w)", stack.x - 9.0 - (sk.head.x + Body.HEAD_RADIUS), 5.0..60.0)
            inRange(n, "rope clear of the column ($w)", stack.x - 9.0 - maxOf(sk.near.hand.x, sk.near.grip.x) - Body.HAND_WIDTH / 2, 5.0..60.0)
        }
        for (w in Which.entries) inRange(n, "pulley above the head ($w)", -stack.pulleyHeight - s(n, w).head.y, -200.0..-30.0)
    }

    @Test
    fun machineCrunch_hipsStill_padOnTheChest_handsOnThePad() {
        val n = "Machine crunch"
        hipsStill(n)
        inRange(n, "upright (START)", pose(n, Which.START).torso, 89.9..90.1)
        inRange(n, "curled forward and down (END)", pose(n, Which.END).torso, 40.0..50.0)
        val ms = Which.entries.map { props(n, it).filterIsInstance<CrunchMachine>().single() }
        for ((i, w) in Which.entries.withIndex()) {
            val sk = s(n, w)
            inRange(n, "pad on the upper chest ($w)", (ms[i].pad - sk.upperChest()).length(), CrunchMachine.PAD_RADIUS - 0.1..CrunchMachine.PAD_RADIUS + 0.1)
            inRange(n, "hands on the pad ($w)", (sk.near.grip - ms[i].pad).length(), 0.0..CrunchMachine.PAD_RADIUS + 4.0)
            inRange(n, "back of the hips on the hip pad ($w)", sk.pelvis.x - Body.TORSO_RADIUS - ms[i].hipPadX, -0.5..0.5)
        }
        inRange(n, "lever the same length", abs((ms[0].pad - ms[0].pivot).length() - (ms[1].pad - ms[1].pivot).length()), 0.0..0.01)
        // The lever turns about the hips' axis and is drawn in front of the body, so the pad is plainly attached.
        inRange(n, "pivot at the hips", (ms[0].pivot - s(n, Which.START).pelvis).length(), 0.0..0.01)
        assertTrue(ms[0].drawFront(View.SIDE).any { val c = it.shape; c is Capsule && c.role == Role.EQUIPMENT && Point(c.x2, c.y2) == ms[0].pad })
    }

    // ---- standing ----

    @Test
    fun pallofPress_fromTheChestToStraightOut_torsoSquare_cableLevelWithTheChest() {
        val n = "Pallof press"
        for (w in Which.entries) {
            inRange(n, "torso upright, square ($w)", pose(n, w).torso, 89.9..90.1)
            inRange(n, "knees slightly bent ($w)", pose(n, w).nearLeg.knee, 8.0..12.0)
        }
        val a = s(n, Which.START)
        val b = s(n, Which.END)
        inRange(n, "handle at the chest (START)", a.near.grip.x - a.chest().x, 0.0..10.0)
        inRange(n, "handle level with the chest (START)", abs(a.near.grip.y - a.chest().y), 0.0..3.0)
        inRange(n, "elbows tucked by the sides (START)", abs(fold(angle(a.near.elbow - a.near.shoulder) - 270.0)), 0.0..30.0)
        inRange(n, "arms straight (END)", pose(n, Which.END).nearArm.elbow, 0.0..0.1)
        inRange(n, "pressed out level with the chest (END)", abs(b.near.grip.y - b.chest().y), 0.0..0.5)
        inRange(n, "pressed out in front (END)", b.near.grip.x - b.chest().x, 40.0..70.0)
        val stack = props(n).filterIsInstance<CableStack>().single()
        assertTrue(stack.beyond, "the column beside the lifter, beyond the body")
        inRange(n, "pulley level with the chest", abs(-stack.pulleyHeight - a.chest().y), 0.0..0.5)
        inRange(n, "pulley beside the chest", abs(stack.x + stack.reach - a.near.grip.x), 0.0..0.5)
    }

    @Test
    fun cableWoodchop_frontView_trunkTurns_aboveTheNearShoulderToTheFarHip_armsNearlyStraight() {
        val n = "Cable woodchop"
        val a = s(n, Which.START)
        val b = s(n, Which.END)
        // The trunk turns towards the machine at the top and away at the bottom; the shoulders narrow with the turn.
        inRange(n, "turned towards the machine (START)", pose(n, Which.START).twist, 25.0..35.0)
        inRange(n, "turned away (END)", pose(n, Which.END).twist, -35.0..-25.0)
        inRange(n, "shoulders narrowed by the turn", abs(a.near.shoulder.x - a.far.shoulder.x), 2 * Body.SHOULDER_HALF_WIDTH * 0.8..2 * Body.SHOULDER_HALF_WIDTH * 0.9)
        inRange(n, "hips square", abs(a.near.hip.x - a.far.hip.x), 2 * Body.HIP_HALF_WIDTH - 0.01..2 * Body.HIP_HALF_WIDTH + 0.01)
        // START above and outside the near shoulder, clear of the head; END at the far hip's height, outside it.
        inRange(n, "hands above the shoulders (START)", a.near.shoulder.y - a.near.grip.y, 30.0..40.0)
        inRange(n, "hands outside the near shoulder (START)", a.near.grip.x - a.near.shoulder.x, 3.0..25.0)
        inRange(n, "hands clear of the head (START)", a.near.grip.x - a.head.x - Body.HEAD_RADIUS, 5.0..40.0)
        inRange(n, "hands at the far hip's height (END)", abs(b.near.grip.y - b.far.hip.y), 0.0..1.0)
        inRange(n, "hands outside the far hip (END)", b.far.hip.x - b.near.grip.x, 8.0..30.0)
        inRange(n, "sideways sweep", a.near.grip.x - b.near.grip.x, 35.0..70.0)
        for (w in Which.entries) {
            val p = pose(n, w)
            val sk = s(n, w)
            for ((label, arm) in listOf("near" to p.nearArm, "far" to p.farArm)) {
                inRange(n, "$label elbow nearly straight ($w)", JointLimits.elbowBend(arm), 5.0..12.0)
                // The arms reach out in front, and the slight bend points the elbow back towards the body: the upper
                // arm points less out of the picture than the forearm. A wrong-way elbow (bent out towards the
                // camera) reverses this.
                inRange(n, "$label arm out in front ($w)", arm.out, 10.0..80.0)
                inRange(n, "$label elbow towards the body ($w)", arm.forearmOut - arm.out, 1.0..20.0)
            }
            inRange(n, "hands together ($w)", abs(sk.near.grip.x - sk.far.grip.x) + abs(sk.near.grip.y - sk.far.grip.y), 0.0..0.5)
            inRange(n, "feet wider than the shoulders ($w)", abs(sk.near.ankle.x - sk.far.ankle.x) - 2 * Body.SHOULDER_HALF_WIDTH, 5.0..40.0)
            inRange(n, "knees straight ($w)", p.nearLeg.knee + p.farLeg.knee, 0.0..0.1)
            inRange(n, "torso upright ($w)", p.torso, 89.9..90.1)
        }
        val stack = props(n).filterIsInstance<CableStack>().single()
        inRange(n, "column on the near side, clear of the hands", stack.x - 9.0 - a.near.hand.x, 10.0..120.0)
        inRange(n, "pulley at shoulder height", abs(-stack.pulleyHeight - s(n, Which.END).near.shoulder.y), 0.0..0.5)
    }

    // ---- carries ----

    private fun upright(n: String) {
        for (w in Which.entries) {
            val p = pose(n, w)
            inRange(n, "torso upright ($w)", p.torso, 89.9..90.1)
            inRange(n, "no lean ($w)", abs(p.lean), 0.0..0.1)
        }
    }

    /** END mid-stride (side view): the front foot landing on its heel, the toes up; the back foot on its ball, the heel up. */
    private fun midStride(n: String) {
        val e = s(n, Which.END)
        inRange(n, "stride length", e.near.ankle.x - e.far.ankle.x, 40.0..70.0)
        inRange(n, "front heel on the floor", heelUp(e.near), -0.5..0.5)
        inRange(n, "front toes up", e.near.heel.y - e.near.toe.y, 2.0..10.0)
        inRange(n, "back heel up", heelUp(e.far), 3.0..20.0)
        inRange(n, "back toes on the floor", footUp(e.far), -0.5..0.5)
        inRange(n, "front knee nearly straight", pose(n, Which.END).nearLeg.knee, 0.0..10.0)
        inRange(n, "back knee nearly straight", pose(n, Which.END).farLeg.knee, 0.0..20.0)
        val st = s(n, Which.START)
        for (side in Side.entries) inRange(n, "${side.label} foot flat (START)", abs(angle(st.side(side).toe - st.side(side).heel)), 0.0..1.0)
    }

    @Test
    fun farmersWalk_frontView_aWeightInEachHand_shouldersLevel_noLean() {
        val n = "Farmer's walk"
        upright(n)
        val dbs = props(n).filterIsInstance<Dumbbell>()
        assertEquals(setOf(Side.NEAR, Side.FAR), dbs.map { it.hand }.toSet())
        assertTrue(dbs.all { it.endOnFront }, "palms in: end on from the front")
        for (w in Which.entries) {
            val sk = s(n, w)
            inRange(n, "shoulders level ($w)", abs(sk.near.shoulder.y - sk.far.shoulder.y), 0.0..0.01)
            for (side in Side.entries) {
                val l = sk.side(side)
                val arm = pose(n, w).let { if (side == Side.NEAR) it.nearArm else it.farArm }
                inRange(n, "${side.label} arm straight ($w)", arm.elbow, 0.0..0.1)
                inRange(n, "${side.label} arm close to the side ($w)", arm.shoulder, 0.0..15.0)
                // Both weights clear of the thighs, the same on each side.
                inRange(n, "${side.label} weight clear of the thigh ($w)", abs(l.grip.x - sk.pelvis.x) - Dumbbell.HEAD_RADIUS - (abs(l.hip.x - sk.pelvis.x) + Body.THIGH_WIDTH / 2), 1.0..3.0)
            }
            inRange(n, "loads level ($w)", abs(sk.near.grip.y - sk.far.grip.y), 0.0..0.01)
        }
        inRange(n, "far foot swinging through, off the floor (END)", footUp(s(n, Which.END).far), 3.0..20.0)
        inRange(n, "standing leg straight", pose(n, Which.END).nearLeg.knee, 0.0..0.1)
        // Lessons 9 and 10: it differs from the suitcase carry by its second load, and from the side-view carries by its camera.
        assertEquals(1, figure("Suitcase carry").scene(Which.START).props.filterIsInstance<Dumbbell>().size)
        assertEquals(View.SIDE, figure("Plate pinch carry").view)
    }

    @Test
    fun suitcaseCarry_frontView_oneSideLoaded_shouldersLevel_noLean() {
        val n = "Suitcase carry"
        upright(n)
        val dbs = props(n).filterIsInstance<Dumbbell>()
        assertEquals(listOf(Side.NEAR), dbs.map { it.hand })
        for (w in Which.entries) {
            val sk = s(n, w)
            inRange(n, "shoulders level ($w)", abs(sk.near.shoulder.y - sk.far.shoulder.y), 0.0..0.01)
            inRange(n, "loaded arm straight ($w)", pose(n, w).nearArm.elbow, 0.0..0.1)
            inRange(n, "weight clear of the thigh ($w)", (sk.near.grip.x - Dumbbell.HEAD_RADIUS) - (sk.near.hip.x + Body.THIGH_WIDTH / 2), 1.0..3.0)
            inRange(n, "arm close to the side ($w)", pose(n, w).nearArm.shoulder, 0.0..15.0)
            inRange(n, "pelvis level ($w)", abs(sk.near.hip.y - sk.far.hip.y), 0.0..0.01)
        }
        inRange(n, "far foot swinging through, off the floor (END)", footUp(s(n, Which.END).far), 3.0..20.0)
        inRange(n, "standing leg straight", pose(n, Which.END).nearLeg.knee, 0.0..0.1)
    }

    @Test
    fun waitersCarry_rackThenOverheadOnAStraightArm_inLine_midStride() {
        val n = "Waiter's carry"
        upright(n)
        midStride(n)
        assertTrue(props(n, Which.START).any { it is Kettlebell && it.hold == KettlebellHold.RACK })
        assertTrue(props(n, Which.END).any { it is Kettlebell && it.hold == KettlebellHold.OVERHEAD })
        val e = s(n, Which.END)
        inRange(n, "arm straight overhead", pose(n, Which.END).nearArm.elbow, 0.0..0.1)
        inRange(n, "weight over the shoulder", abs(e.near.wrist.x - e.near.shoulder.x), 0.0..0.5)
        assertTrue(LockoutOverShoulders(Which.END) in figure(n).checks)
        for (w in Which.entries) inRange(n, "free arm hangs ($w)", abs(s(n, w).far.wrist.x - s(n, w).far.shoulder.x), 0.0..0.5)
        val st = s(n, Which.START)
        inRange(n, "kettlebell at the front of the shoulder (START)", (st.near.grip - st.near.shoulder).length(), 0.0..25.0)
    }

    @Test
    fun platePinchCarry_aPlateInEachHand_standingTall_thenMidStride() {
        val n = "Plate pinch carry"
        upright(n)
        midStride(n)
        assertEquals(setOf(Side.NEAR, Side.FAR), props(n).filterIsInstance<PinchPlate>().map { it.hand }.toSet())
        for (w in Which.entries) {
            val sk = s(n, w)
            for (side in Side.entries) inRange(n, "${side.label} arm straight down ($w)", abs(sk.side(side).wrist.x - sk.side(side).shoulder.x), 0.0..0.5)
            val c = PinchPlate(Side.NEAR).center(sk)
            inRange(n, "plate off the floor ($w)", -c.y - PinchPlate.RADIUS, 10.0..80.0)
            inRange(n, "plate's rim in the hand ($w)", (c - sk.near.grip).length(), PinchPlate.RADIUS - PinchPlate.PINCH - 0.01..PinchPlate.RADIUS - PinchPlate.PINCH + 0.01)
        }
    }
}
