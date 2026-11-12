package com.powertracker.domain.library.figures

import com.powertracker.domain.library.Library
import com.powertracker.domain.library.figure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The first batch (Task 4): ten figures with the key checks and contacts the plan lists for each. */
class BatchOneTest {
    private fun figure(name: String): ExerciseFigure = assertNotNull(Figures.all[name], "no figure for $name")

    private fun assertContact(name: String, point: BodyPoint, surface: Surface, at: Set<Which> = Which.entries.toSet()) {
        val f = figure(name)
        assertTrue(f.contacts.any { it.point == point && it.surface == surface && it.at.containsAll(at) }, "$name: $point on $surface at $at")
    }

    private fun assertCheck(name: String, check: KeyCheck) {
        assertTrue(check in figure(name).checks, "$name: $check in ${figure(name).checks}")
    }

    @Test
    fun batchOne_everyExerciseHasAFigure_thatPassesItsChecks() {
        for (name in BATCH) {
            assertEquals(name, Library.find(name)?.name, "catalogue name")
            assertEquals(emptyList(), FigureChecks.check(name, figure(name)), name)
        }
    }

    @Test
    fun squat_barOnTheUpperBack_competitionDepth_feetOnTheFloor() {
        val f = figure("Squat")
        for (which in Which.entries) assertTrue(Barbell(BarHold.UPPER_BACK) in f.scene(which).props, "bar on the back at $which")
        assertTrue(f.checks.any { it is HipDepth && it.at == Which.END && it.belowKnee > 0.0 }, f.checks.toString())
        assertCheck("Squat", BarOverMidfoot(Which.START))
        assertCheck("Squat", BarOverMidfoot(Which.END))
        assertContact("Squat", BodyPoint.FEET, Surface.FLOOR)
    }

    @Test
    fun bench_barOnTheChest_thenLockout() {
        assertCheck("Bench", BarTouches(BodyPoint.CHEST, Which.START))
        assertCheck("Bench", LockoutOverShoulders(Which.END))
        assertContact("Bench", BodyPoint.FEET, Surface.FLOOR)
        // ExRx bench press analyses: forearms vertical at the bottom. A side view with full-length segments gets within
        // 20° of it (the real upper arm is out to the side and foreshortened), the elbow under the bar's foot side.
        val s = figure("Bench").start.pose.skeleton()
        val forearm = s.near.wrist - s.near.elbow
        val offVertical = kotlin.math.atan2(kotlin.math.abs(forearm.x), -forearm.y) * 180 / kotlin.math.PI
        assertTrue(offVertical < 20.0, "forearm $offVertical° off vertical")
        assertTrue(s.near.elbow.y > s.near.wrist.y, "elbow below the wrist")
    }

    @Test
    fun deadliftAndRdl_barOverMidfoot_closeToTheLegs() {
        for (name in listOf("Deadlift", "RDL")) {
            for (which in Which.entries) {
                assertCheck(name, BarOverMidfoot(which))
                assertTrue(figure(name).checks.any { it is BarNearLegs && it.at == which }, "$name: bar near the legs at $which")
            }
            assertContact(name, BodyPoint.FEET, Surface.FLOOR)
        }
        // A conventional pull, not a stiff-legged one: shins forward, the torso 30–35° above level, the hips well
        // below the shoulders.
        val start = figure("Deadlift").start.pose
        val s = start.skeleton()
        assertTrue(start.nearLeg.ankle in 8.0..12.0, "shin lean ${start.nearLeg.ankle}")
        assertTrue(start.torso in 30.0..35.0, "torso ${start.torso}")
        assertTrue(s.near.shoulder.y < s.near.hip.y - 20.0, "hips well below the shoulders")
        val bar = Barbell().center(s)
        assertTrue(s.near.shoulder.x - bar.x in 0.0..4.0, "shoulders just ahead of the bar")
    }

    @Test
    fun strictPress_lockoutOverTheShoulders_feetOnTheFloor() {
        assertCheck("Strict press", LockoutOverShoulders(Which.END))
        assertContact("Strict press", BodyPoint.FEET, Surface.FLOOR)
        // START: the bar at the front of the shoulders, below the chin, not floating in front of the face.
        val f = figure("Strict press")
        val s = f.start.pose.skeleton()
        val bar = Barbell().center(s)
        assertTrue(bar.x - s.near.shoulder.x in 6.0..11.0, "bar ${bar.x - s.near.shoulder.x} cm in front of the shoulder")
        assertTrue(bar.y > s.chin().y, "bar below the chin")
    }

    @Test
    fun dbShoulderPress_seatedAgainstTheBack_lockout() {
        assertCheck("DB shoulder press", LockoutOverShoulders(Which.END))
        assertContact("DB shoulder press", BodyPoint.HIPS, Surface.SEAT)
        assertContact("DB shoulder press", BodyPoint.UPPER_BACK, Surface.BACK_PAD)
        assertContact("DB shoulder press", BodyPoint.HEAD, Surface.BACK_PAD)
        assertContact("DB shoulder press", BodyPoint.FEET, Surface.FLOOR)
        // START is a press, not a curl: the forearm vertical, the dumbbell straight above the elbow and above the shoulder.
        val s = figure("DB shoulder press").start.pose.skeleton()
        assertEquals(s.near.elbow.x, s.near.grip.x, 0.5, "dumbbell over the elbow")
        assertTrue(s.near.grip.y < s.near.shoulder.y && s.near.elbow.y > s.near.shoulder.y, "elbow below, dumbbell above the shoulder")
    }

    @Test
    fun pullUp_handsOnTheBar_chinClearsItAtTheTop() {
        assertContact("Pull-up", BodyPoint.HANDS, Surface.PULL_UP_BAR)
        assertCheck("Pull-up", ChinOverBar(at = Which.END))
        val f = figure("Pull-up")
        val bar = f.end.props.filterIsInstance<PullUpBar>().single()
        val above = -bar.height - PullUpBar.RADIUS - f.end.pose.skeleton().chin().y
        assertTrue(above in 5.0..6.5, "chin $above cm over the bar")
    }

    @Test
    fun latPulldown_seated_thighsUnderThePad_barToTheUpperChest() {
        assertContact("Lat pulldown", BodyPoint.HIPS, Surface.SEAT)
        assertContact("Lat pulldown", BodyPoint.THIGHS, Surface.THIGH_PAD)
        assertContact("Lat pulldown", BodyPoint.FEET, Surface.FLOOR)
        assertCheck("Lat pulldown", BarTouches(BodyPoint.UPPER_CHEST, Which.END))
    }

    @Test
    fun legPress_backOnThePad_feetOnThePlate() {
        assertContact("Leg press", BodyPoint.UPPER_BACK, Surface.BACK_PAD)
        assertContact("Leg press", BodyPoint.HIPS, Surface.SEAT)
        assertContact("Leg press", BodyPoint.FEET, Surface.FOOT_PLATE)
        // The guide: knees bend to about 90 degrees at the bottom, nearly straight at the top.
        val f = figure("Leg press")
        assertTrue(f.end.pose.nearLeg.knee in 80.0..100.0, "knee at the bottom ${f.end.pose.nearLeg.knee}")
        assertTrue(f.start.pose.nearLeg.knee in 5.0..25.0, "knee at the top ${f.start.pose.nearLeg.knee}")
        // "Stop sooner if your hips start to lift... don't let your thighs press into your ribs": hips kept shallow.
        assertTrue(f.end.pose.nearLeg.hip < 120.0, "hip flexion at the bottom ${f.end.pose.nearLeg.hip}")
    }

    @Test
    fun oneArmDbRow_handAndKneeOnTheBench() {
        assertContact("One-arm DB row", BodyPoint.FAR_HAND, Surface.BENCH)
        assertContact("One-arm DB row", BodyPoint.FAR_KNEE, Surface.BENCH)
        assertContact("One-arm DB row", BodyPoint.NEAR_FOOT, Surface.FLOOR)
        assertTrue(Dumbbell(Side.NEAR) in figure("One-arm DB row").start.props)
        // END "Pull to your ribs": the elbow up past the back, the dumbbell close under the torso (its middle line plus
        // the torso's half-depth, about 10 cm, plus about 10 cm at most).
        val end = figure("One-arm DB row").end.pose
        assertTrue(end.nearArm.shoulder in -30.0..-25.0, "shoulder ${end.nearArm.shoulder}")
        val s = end.skeleton()
        val t = s.near.shoulder - s.near.hip
        val lineY = s.near.hip.y + t.y * ((s.near.grip.x - s.near.hip.x) / t.x)
        assertTrue(s.near.grip.y - lineY in 0.0..22.0, "dumbbell ${s.near.grip.y - lineY} cm under the torso line")
    }

    @Test
    fun batchOne_resolvesByAlias() {
        assertEquals(figure("RDL"), Library.figure("Romanian deadlift"))
        assertEquals(figure("Strict press"), Library.figure("Overhead press"))
    }

    private companion object {
        val BATCH = listOf(
            "Squat", "Bench", "Deadlift", "Strict press", "DB shoulder press", "Lat pulldown", "Leg press", "Pull-up",
            "One-arm DB row", "RDL",
        )
    }
}
