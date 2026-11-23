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

/** The calf batch (Task 12): every exercise of the area, each with the key positions its guide gives. */
class BatchCalvesTest {
    private fun figure(name: String): ExerciseFigure = assertNotNull(Figures.all[name], "no figure for $name")

    private fun s(name: String, which: Which) = figure(name).let { it.scene(which).pose.skeleton(it.view) }

    private fun pose(name: String, which: Which) = figure(name).scene(which).pose

    private fun props(name: String, which: Which = Which.START) = figure(name).scene(which).props

    private fun inRange(name: String, label: String, value: Double, range: ClosedFloatingPointRange<Double>) =
        assertTrue(value in range, "$name: $label ${round1(value)} not in $range")

    @Test
    fun calves_everyExerciseHasAFigure_thatPassesItsChecks() {
        val area = Library.all().filter { it.area == BodyArea.CALVES }.map { it.name }
        assertEquals(9, area.size)
        val problems = area.flatMap { name -> Figures.all[name]?.let { FigureChecks.check(name, it) } ?: listOf(FigureProblem(name, null, "no figure")) }
        assertEquals(emptyList(), problems.map { "${it.name} ${it.which}: ${it.message}" })
        for (name in area) assertEquals(View.SIDE, figure(name).view, name)
    }

    /** The block's top (y) and back edge (x) in [name]'s scene: the deficit platform used as the calf block. */
    private fun block(name: String) = props(name).filterIsInstance<DeficitPlatform>().single().let { Point(it.x - it.length / 2, -it.height) }

    /** How far the heel's underside is below (+) or above (-) the block's top, cm. */
    private fun heelBelowTop(l: LimbPoints, top: Double) = (l.heel.y + Body.FOOT_WIDTH / 2) - top

    /**
     * The calf raise's ankle, both feet unless [sides]: START the heels below the block's top behind its edge, END raised
     * well above it, the ankle's bend in the ranges given, the toes on the block in front of its edge.
     */
    private fun ankleRange(
        name: String,
        drop: ClosedFloatingPointRange<Double> = 14.0..18.0,
        rise: ClosedFloatingPointRange<Double> = -27.0..-23.0,
        sides: List<Side> = Side.entries,
    ) {
        val b = block(name)
        for (side in sides) {
            val st = s(name, Which.START).side(side)
            val en = s(name, Which.END).side(side)
            val p0 = pose(name, Which.START).let { if (side == Side.NEAR) it.nearLeg else it.farLeg }
            val p1 = pose(name, Which.END).let { if (side == Side.NEAR) it.nearLeg else it.farLeg }
            inRange(name, "${side.label} ankle dorsiflexed (START)", p0.ankle, drop)
            inRange(name, "${side.label} ankle pointed (END)", p1.ankle, rise)
            inRange(name, "${side.label} heel below the block's top (START)", heelBelowTop(st, b.y), 3.0..8.0)
            inRange(name, "${side.label} heel above the block's top (END)", -heelBelowTop(en, b.y), 5.0..14.0)
            for ((w, l) in listOf(Which.START to st, Which.END to en)) {
                inRange(name, "${side.label} heel behind the edge ($w)", b.x - l.heel.x, 5.0..25.0)
                inRange(name, "${side.label} toe on the block ($w)", l.toe.x - b.x, 1.0..10.0)
            }
        }
    }

    private fun kneesStraight(name: String, max: Double = 3.0, sides: List<Side> = Side.entries) {
        for (w in Which.entries) {
            val p = pose(name, w)
            if (Side.NEAR in sides) inRange(name, "near knee ($w)", p.nearLeg.knee, 0.0..max)
            if (Side.FAR in sides) inRange(name, "far knee ($w)", p.farLeg.knee, 0.0..max)
        }
    }

    private fun standingTall(name: String, torso: ClosedFloatingPointRange<Double> = 89.0..91.0) {
        for (w in Which.entries) {
            inRange(name, "torso upright ($w)", pose(name, w).torso, torso)
            // The straight legs lean no more than a couple of degrees as the ankles come forward.
            val sk = s(name, w)
            inRange(name, "legs near vertical ($w)", abs(atan2(sk.near.ankle.x - sk.pelvis.x, sk.near.ankle.y - sk.pelvis.y) * 180 / PI), 0.0..3.0)
        }
    }

    private fun rise(name: String) = s(name, Which.START).pelvis.y - s(name, Which.END).pelvis.y

    @Test
    fun standingCalfRaise_padsOnTheShoulders_handsOnTheHandles_kneesStraight_heelsDropThenRise() {
        val n = "Standing calf raise"
        ankleRange(n)
        kneesStraight(n)
        standingTall(n)
        inRange(n, "body rises", rise(n), 6.0..14.0)
        for (w in Which.entries) {
            val m = props(n, w).filterIsInstance<StandingCalfMachine>().single()
            val sk = s(n, w)
            // The pad on top of the shoulder: above the joint, by the drawn shoulder's top.
            inRange(n, "pad over the shoulder ($w)", sk.shoulder.y - m.pad.y, 9.0..14.0)
            inRange(n, "pad over the shoulder, front to back ($w)", abs(m.pad.x - sk.shoulder.x), 0.0..5.0)
            inRange(n, "hand on the handle ($w)", (sk.near.grip - m.handle).length(), 0.0..1.0)
            inRange(n, "handle in front of the shoulder ($w)", m.handle.x - sk.shoulder.x, 10.0..25.0)
        }
        // The lever is the same length in both panels.
        val (a, b) = Which.entries.map { props(n, it).filterIsInstance<StandingCalfMachine>().single() }
        inRange(n, "lever length", abs((a.pad - a.pivot).length() - (b.pad - b.pivot).length()), 0.0..0.01)
        assertTrue(figure(n).contacts.any { it.point == BodyPoint.HANDS && it.surface == Surface.HANDLE })
    }

    /**
     * Task 14 (the owner's finding): an upright lifter with the bar across the upper back reads as facing the way his
     * feet point (+x, the toes in front of the heels). The bar rests on the upper back (its centre within a few cm of
     * BodyPoint.UPPER_BACK's point, not outside the back's edge); each hand on the bar, above its elbow; the elbows at
     * or behind the shoulder, pointing down, so the forearms run up to the bar from below, never across the chest; the
     * head's centre in front of the bar's.
     */
    private fun facesTheToes(name: String) {
        val out = mutableListOf<String>()
        fun bound(label: String, value: Double, range: ClosedFloatingPointRange<Double>) {
            if (value !in range) out += "$label ${round1(value)} not in $range"
        }
        for (w in Which.entries) {
            val sk = s(name, w)
            val bar = props(name, w).filterIsInstance<Barbell>().single().center(sk)
            bound("toes in front of the heels ($w)", sk.near.toe.x - sk.near.heel.x, 10.0..30.0)
            val upperBack = sk.probes(BodyPoint.UPPER_BACK).single().a
            bound("bar centre from the upper back ($w)", (bar - upperBack).length(), 0.0..7.5)
            bound("head in front of the bar ($w)", sk.head.x - bar.x, 4.0..15.0)
            for (side in Side.entries) {
                val l = sk.side(side)
                bound("${side.label} elbow behind the shoulder ($w)", sk.shoulder.x - l.elbow.x, 0.0..15.0)
                bound("${side.label} grip above the elbow ($w)", l.elbow.y - l.grip.y, 15.0..40.0)
                bound("${side.label} hand on the bar ($w)", (l.grip - bar).length(), 0.0..1.0)
            }
        }
        assertEquals(emptyList(), out, "$name faces the way the feet point")
    }

    @Test
    fun barbellCalfRaise_barOnTheBack_inARack_hooksAndPinsBelowTheBar() {
        val n = "Barbell calf raise"
        ankleRange(n)
        kneesStraight(n)
        // Tall, with the small forward lean of a bar on the back (Task 14), the head kept level.
        standingTall(n, torso = 84.0..86.0)
        for (w in Which.entries) inRange(n, "head level ($w)", pose(n, w).torso - pose(n, w).neck, 89.0..91.0)
        facesTheToes(n)
        assertTrue(props(n).any { it is Barbell && it.on == BarHold.ON_TRAPS })
        val rack = props(n).filterIsInstance<PowerRack>().single()
        val bar = Barbell(BarHold.ON_TRAPS).center(s(n, Which.START))
        inRange(n, "J-hooks under the bar", -bar.y - rack.jHookHeight, 6.0..12.0)
        inRange(n, "safety arms under the bar", -bar.y - rack.safetyHeight, 12.0..25.0)
        // The posts clear of the body in front.
        for (w in Which.entries) inRange(n, "posts clear of the chest ($w)", rack.x - 11.0 - s(n, w).chest().x, 10.0..60.0)
        assertTrue(figure(n).endCaption.contains("pause"))
    }

    @Test
    fun smithCalfRaise_barOnTheRails_stopsJustBelow() {
        val n = "Smith calf raise"
        ankleRange(n)
        kneesStraight(n)
        // Tall, with the small forward lean of a bar on the back (Task 14), the head kept level.
        standingTall(n, torso = 84.0..86.0)
        for (w in Which.entries) inRange(n, "head level ($w)", pose(n, w).torso - pose(n, w).neck, 89.0..91.0)
        assertTrue(BarOnRails(Which.START) in figure(n).checks && BarOnRails(Which.END) in figure(n).checks)
        facesTheToes(n)
        assertTrue(props(n).any { it is Barbell && it.on == BarHold.ON_TRAPS })
        val smith = props(n).filterIsInstance<SmithMachine>().single()
        val bar = Barbell(BarHold.ON_TRAPS).center(s(n, Which.START))
        inRange(n, "stops just below the lowest bar", -bar.y - Barbell.SHAFT_RADIUS - smith.stopHeight, 2.0..6.0)
        // The stop shows past the back in both panels (the torso's back is 10 cm behind its axis).
        for (w in Which.entries) inRange(n, "stop shows behind the back ($w)", (s(n, w).pelvis.x - Body.TORSO_RADIUS) - (smith.x - smith.stopBehind), 5.0..30.0)
    }

    @Test
    fun dumbbellCalfRaise_dumbbellAtTheSide_otherHandOnASupport() {
        val n = "Dumbbell calf raise"
        ankleRange(n)
        kneesStraight(n)
        standingTall(n)
        val dbs = props(n).filterIsInstance<Dumbbell>()
        assertEquals(1, dbs.size)
        assertTrue(dbs.single().neutral, "palms in: seen in profile")
        for (w in Which.entries) {
            val sk = s(n, w)
            val post = props(n).filterIsInstance<SupportPost>().single()
            inRange(n, "far hand on the support ($w)", abs(sk.far.grip.y + post.gripHeight), 0.0..1.0)
            inRange(n, "near arm hangs straight ($w)", abs(sk.near.wrist.x - sk.near.shoulder.x), 0.0..1.0)
            inRange(n, "support in front of the toes ($w)", post.x - sk.near.toe.x, 5.0..40.0)
        }
    }

    @Test
    fun singleLegCalfRaise_oneFootOnTheStep_otherLegBentBehind() {
        val n = "Single-leg calf raise"
        ankleRange(n, sides = listOf(Side.NEAR))
        kneesStraight(n, sides = listOf(Side.NEAR))
        standingTall(n)
        val b = block(n)
        for (w in Which.entries) {
            val sk = s(n, w)
            inRange(n, "free knee bent ($w)", pose(n, w).farLeg.knee, 70.0..110.0)
            inRange(n, "free foot off the step ($w)", b.y - maxOf(sk.far.heel.y, sk.far.toe.y) - Body.FOOT_WIDTH / 2, 10.0..60.0)
            inRange(n, "free foot behind the edge ($w)", b.x - maxOf(sk.far.heel.x, sk.far.toe.x), 5.0..60.0)
        }
        assertTrue(props(n).none { it is Dumbbell }, "$n: the optional dumbbell is not drawn")
        assertTrue(props(n).any { it is SupportPost })
        assertEquals("Knee straight", figure(n).endCaption)
    }

    @Test
    fun seatedCalfRaise_kneesAbout90_padOnTheLowerThighs_handsOnThePad() {
        val n = "Seated calf raise"
        ankleRange(n, drop = 13.0..17.0, rise = -25.0..-21.0)
        for (w in Which.entries) {
            knees(n, w, 75.0..100.0)
            val sk = s(n, w)
            val m = props(n, w).filterIsInstance<SeatedCalfMachine>().single()
            inRange(n, "torso upright ($w)", pose(n, w).torso, 89.0..91.0)
            // The pad on top of the thigh, near the knee.
            val along = ((m.pad - sk.near.hip).let { it.x * (sk.near.knee - sk.near.hip).x + it.y * (sk.near.knee - sk.near.hip).y }) / (sk.near.knee - sk.near.hip).let { it.x * it.x + it.y * it.y }
            inRange(n, "pad on the lower thigh ($w)", along, 0.7..0.95)
            inRange(n, "pad on top of the thigh ($w)", distanceToThigh(m.pad, sk) - Body.THIGH_WIDTH / 2 - SeatedCalfMachine.PAD_RADIUS, -0.5..0.5)
        }
        inRange(n, "shins upright at the bottom", abs(s(n, Which.START).near.knee.x - s(n, Which.START).near.ankle.x), 0.0..1.0)
        inRange(n, "hips still", (s(n, Which.START).pelvis - s(n, Which.END).pelvis).length(), 0.0..0.01)
        inRange(n, "knees rise", s(n, Which.START).near.knee.y - s(n, Which.END).near.knee.y, 5.0..14.0)
    }

    @Test
    fun seatedDumbbellCalfRaise_onABench_dumbbellAcrossTheThighs() {
        val n = "Seated dumbbell calf raise"
        ankleRange(n, drop = 13.0..17.0, rise = -25.0..-21.0)
        val db = props(n).filterIsInstance<Dumbbell>().single()
        assertTrue(!db.neutral, "across the thighs: end on")
        for (w in Which.entries) {
            knees(n, w, 70.0..105.0)
            val sk = s(n, w)
            val c = db.center(sk)
            inRange(n, "dumbbell resting on the thigh ($w)", distanceToThigh(c, sk) - Body.THIGH_WIDTH / 2 - Dumbbell.HEAD_RADIUS, -0.5..0.5)
            inRange(n, "both hands on it ($w)", (sk.near.grip - sk.far.grip).length(), 0.0..0.5)
        }
        inRange(n, "hips still", (s(n, Which.START).pelvis - s(n, Which.END).pelvis).length(), 0.0..0.01)
    }

    @Test
    fun donkeyCalfRaise_bentOverAtTheHips_forearmsOnTheSupport_padOnTheHips() {
        val n = "Donkey calf raise"
        // The legs lean back a little as the hips rise under the fixed shoulders, so the ankle points 2° more.
        ankleRange(n, rise = -28.0..-23.0)
        kneesStraight(n)
        for (w in Which.entries) {
            val p = pose(n, w)
            val sk = s(n, w)
            inRange(n, "torso near level ($w)", p.torso, 0.0..15.0)
            inRange(n, "hips bent ($w)", p.nearLeg.hip, 70.0..95.0)
            val m = props(n, w).filterIsInstance<DonkeyCalfMachine>().single()
            // The forearm level on the support's top, the elbow on it.
            val top = -m.supportHeight
            inRange(n, "forearm on the support ($w)", top - (sk.near.elbow.y + Body.FOREARM_WIDTH / 2), -0.5..0.5)
            inRange(n, "forearm level ($w)", abs(sk.near.wrist.y - sk.near.elbow.y), 0.0..0.5)
            inRange(n, "pad on the hips ($w)", (m.pad - sk.pelvis).length() - Body.TORSO_RADIUS - DonkeyCalfMachine.PAD_RADIUS, -0.5..7.0)
        }
        inRange(n, "shoulders stay on the support", (s(n, Which.START).shoulder - s(n, Which.END).shoulder).length(), 0.0..0.01)
        inRange(n, "hips rise", rise(n), 5.0..14.0)
    }

    @Test
    fun legPressCalfRaise_legsStraight_toesOnThePlatformEdge_platformPushedAway() {
        val n = "Leg press calf raise"
        val pr = Which.entries.map { props(n, it).filterIsInstance<LegPress45>().single() }
        inRange(n, "platform pushed away", pr[1].sled - pr[0].sled, 4.0..20.0)
        for ((i, w) in Which.entries.withIndex()) {
            val p = pose(n, w)
            knees(n, w, 2.5..3.5)
            inRange(n, "ankle ($w)", p.nearLeg.ankle, if (w == Which.START) 15.5..16.5 else -25.5..-24.5)
            // The heel past the plate's lower edge, the toes on the face above it.
            val plate = pr[i].surfaces(View.SIDE).getValue(Surface.FOOT_PLATE).single()
            val up = Point(kotlin.math.cos((LegPress45.PLATE_ANGLE) * PI / 180), -kotlin.math.sin(LegPress45.PLATE_ANGLE * PI / 180))
            val lower = Point(plate.cx, plate.cy) - up * (plate.length / 2)
            val sk = s(n, w)
            fun along(q: Point) = (q - lower).let { it.x * up.x + it.y * up.y }
            inRange(n, "heel past the lower edge ($w)", -along(sk.near.heel), 5.0..20.0)
            inRange(n, "toes on the plate ($w)", along(sk.near.toe), 1.0..10.0)
        }
    }

    private fun knees(name: String, which: Which, range: ClosedFloatingPointRange<Double>) {
        val p = pose(name, which)
        inRange(name, "near knee ($which)", p.nearLeg.knee, range)
        inRange(name, "far knee ($which)", p.farLeg.knee, range)
    }

    private fun distanceToThigh(p: Point, sk: Skeleton): Double {
        val a = sk.near.hip
        val ab = sk.near.knee - a
        val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / (ab.x * ab.x + ab.y * ab.y)).coerceIn(0.0, 1.0)
        return (p - (a + ab * t)).length()
    }
}
