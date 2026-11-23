package com.powertracker.domain.library.figures

import com.powertracker.domain.library.Library
import com.powertracker.domain.library.figure
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The main-lift batch (Task 5): the 21 variations, each with the key positions its guide gives. */
class BatchMainLiftsTest {
    private fun figure(name: String): ExerciseFigure = assertNotNull(Figures.all[name], "no figure for $name")

    private fun assertContact(name: String, point: BodyPoint, surface: Surface, at: Set<Which> = Which.entries.toSet()) {
        val f = figure(name)
        assertTrue(f.contacts.any { it.point == point && it.surface == surface && it.at.containsAll(at) }, "$name: $point on $surface at $at")
    }

    private fun assertCheck(name: String, check: KeyCheck) {
        assertTrue(check in figure(name).checks, "$name: $check in ${figure(name).checks}")
    }

    private fun skeleton(name: String, which: Which) = figure(name).scene(which).pose.skeleton()

    private fun bar(name: String, which: Which): Point =
        figure(name).scene(which).props.filterIsInstance<HeldProp>().first().center(skeleton(name, which))

    /** How far the hip joint is below the knee joint, in cm (negative: above). */
    private fun hipBelowKnee(name: String, which: Which) = skeleton(name, which).let { it.near.hip.y - it.near.knee.y }

    /** The torso's angle above level, in degrees, for a lifter facing +x. */
    private fun torsoAboveLevel(name: String, which: Which) = figure(name).scene(which).pose.torso

    private fun shinLean(name: String, which: Which) = skeleton(name, which).let {
        val v = it.near.knee - it.near.ankle
        atan2(v.x, -v.y) * 180 / PI
    }

    @Test
    fun mainLifts_everyVariationHasAFigure_thatPassesItsChecks() {
        for (name in BATCH) {
            assertEquals(name, Library.find(name)?.name, "catalogue name")
            assertEquals(emptyList(), FigureChecks.check(name, figure(name)), name)
        }
        // With Task 4's four, the whole area is drawn.
        val area = Library.all().filter { it.area == com.powertracker.domain.library.BodyArea.MAIN_LIFTS }.map { it.name }
        assertEquals(emptyList(), area.filter { it !in Figures.all })
    }

    // ---- squats ----

    @Test
    fun backSquatVariations_barOnTheBack_feetOnTheFloor_squatDepth() {
        for (name in listOf("Pause squat", "High-bar squat", "Tempo squat", "Pin squat", "Box squat")) {
            for (which in Which.entries) assertTrue(Barbell(BarHold.UPPER_BACK) in figure(name).scene(which).props, "$name: bar on the back at $which")
            assertContact(name, BodyPoint.FEET, Surface.FLOOR)
            assertCheck(name, BarOverMidfoot(Which.START))
        }
        // Pause and tempo: the squat's own normal depth at the bottom.
        for (name in listOf("Pause squat", "Tempo squat")) {
            assertEquals(figure("Squat").end.pose, figure(name).end.pose, name)
            assertTrue(figure(name).checks.any { it is HipDepth && it.at == Which.END && it.belowKnee >= 2.0 }, name)
        }
        // High-bar: thighs just past level, the hip 2–5 cm under the knee.
        assertTrue(hipBelowKnee("High-bar squat", Which.END) in 2.0..5.0, "high-bar depth ${hipBelowKnee("High-bar squat", Which.END)}")
        // Tempo START is part-way down: between standing and the bottom.
        val mid = hipBelowKnee("Tempo squat", Which.START)
        assertTrue(mid < -15.0 && mid > hipBelowKnee("Squat", Which.START), "tempo halfway $mid")
        assertCheck("Tempo squat", BarOverMidfoot(Which.START))
    }

    @Test
    fun frontSquat_frontRack_upright_thighsLevel() {
        val f = figure("Front squat")
        for (which in Which.entries) assertTrue(Barbell(BarHold.FRONT_RACK) in f.scene(which).props, "front rack at $which")
        assertContact("Front squat", BodyPoint.FEET, Surface.FLOOR)
        assertCheck("Front squat", BarOverMidfoot(Which.END))
        // Thighs level: the hip within 0–2 cm under the knee.
        assertTrue(hipBelowKnee("Front squat", Which.END) in 0.0..2.0, "depth ${hipBelowKnee("Front squat", Which.END)}")
        // More upright than the back squat's bottom (44° above level), and clearly so.
        val torso = torsoAboveLevel("Front squat", Which.END)
        assertTrue(torso in 60.0..80.0 && torso > torsoAboveLevel("Squat", Which.END) + 15.0, "torso $torso")
        // Elbows straight ahead: the upper arms within 10° of level, the elbow in front of the bar.
        for (which in Which.entries) {
            val s = skeleton("Front squat", which)
            val upper = s.near.elbow - s.near.shoulder
            assertTrue(abs(atan2(-upper.y, upper.x) * 180 / PI) <= 10.0, "upper arm at $which")
            assertTrue(s.near.elbow.x > bar("Front squat", which).x, "elbow ahead of the bar at $which")
            // The bar rests in front of the throat: in front of the shoulder, below the chin.
            val b = bar("Front squat", which)
            assertTrue(b.x - s.shoulder.x in 5.0..14.0 && b.y > s.chin().y, "bar at $which: $b")
        }
    }

    @Test
    fun pinSquat_startsOnThePins_justBelowTheStickingRegion() {
        assertCheck("Pin squat", BarRestsOn(Surface.SAFETY_ARM, Which.START))
        assertCheck("Pin squat", BarOverMidfoot(Which.END))
        // The knees bent 80–90° (an inside angle of 90–100°, under the sticking region's 98–102°).
        val knee = figure("Pin squat").start.pose.nearLeg.knee
        assertTrue(knee in 80.0..90.0, "knee $knee")
        assertTrue(hipBelowKnee("Pin squat", Which.START) < -5.0, "above parallel")
        // A pin, not just a safety: its tip reaches past the bar.
        val rack = figure("Pin squat").start.props.filterIsInstance<PowerRack>().single()
        assertTrue(rack.x - rack.armLength < bar("Pin squat", Which.START).x - 5.0)
    }

    @Test
    fun safetyBarSquat_padsOnTheBack_handsOnTheHandles_justBelowLevel() {
        val f = figure("Safety-bar squat")
        for (which in Which.entries) {
            assertTrue(SafetySquatBar in f.scene(which).props, "safety bar at $which")
            val s = skeleton("Safety-bar squat", which)
            assertTrue((s.near.grip - safetyBarHandle(s)).length() < 1.0, "hands on the handles at $which")
            // The handles in front of the chest.
            assertTrue(safetyBarHandle(s).x > s.shoulder.x + 10.0, "handles in front at $which")
        }
        assertContact("Safety-bar squat", BodyPoint.FEET, Surface.FLOOR)
        assertTrue(hipBelowKnee("Safety-bar squat", Which.END) in 2.0..5.0, "depth")
        // More upright than the high-bar squat.
        assertTrue(torsoAboveLevel("Safety-bar squat", Which.END) > torsoAboveLevel("Squat", Which.END) + 3.0, "torso")
    }

    @Test
    fun boxSquat_sitsOnTheBox_shinsCloseToUpright_parallel() {
        assertContact("Box squat", BodyPoint.HIPS, Surface.BOX, setOf(Which.END))
        assertTrue(shinLean("Box squat", Which.END) in 0.0..8.0, "shin ${shinLean("Box squat", Which.END)}")
        assertTrue(abs(hipBelowKnee("Box squat", Which.END)) <= 1.0, "parallel")
        val box = figure("Box squat").end.props.filterIsInstance<SquatBox>().single()
        assertTrue(box.x < skeleton("Box squat", Which.START).near.heel.x, "the box behind the lifter")
    }

    // ---- bench ----

    @Test
    fun benchVariations_lyingOnTheBench_lockoutOverTheShoulders() {
        for (name in BENCH) {
            assertCheck(name, LockoutOverShoulders(Which.END))
            for (point in listOf(BodyPoint.HEAD, BodyPoint.UPPER_BACK, BodyPoint.HIPS)) assertContact(name, point, Surface.BENCH)
            if (name != "Larsen press") assertContact(name, BodyPoint.FEET, Surface.FLOOR)
        }
        for (name in listOf("Long-pause bench", "Larsen press", "Close-grip bench", "Wide-grip bench")) assertCheck(name, BarTouches(BodyPoint.CHEST, Which.START))
        assertEquals(figure("Bench").start.pose, figure("Long-pause bench").start.pose)
    }

    @Test
    fun spotoPress_stopsJustAboveTheChest() {
        assertTrue(figure("Spoto press").checks.any { it is BarAbove && it.point == BodyPoint.CHEST && it.min >= 3.5 && it.max <= 5.5 })
        val s = skeleton("Spoto press", Which.START)
        val gap = s.chest().y - bar("Spoto press", Which.START).y - Barbell.SHAFT_RADIUS
        assertTrue(gap in 3.5..5.5, "gap $gap")
    }

    @Test
    fun pinPress_startsOnThePins_aboutTenCmUp() {
        assertCheck("Pin press", BarRestsOn(Surface.SAFETY_ARM, Which.START))
        val s = skeleton("Pin press", Which.START)
        val up = s.chest().y - bar("Pin press", Which.START).y - Barbell.SHAFT_RADIUS
        assertTrue(up in 8.0..12.0, "bar $up cm over the chest")
    }

    @Test
    fun larsenPress_feetOffTheFloor_legsStraight() {
        val f = figure("Larsen press")
        assertTrue(f.contacts.none { it.point == BodyPoint.FEET }, "no feet contact")
        for (which in Which.entries) {
            val pose = f.scene(which).pose
            assertEquals(0.0, pose.nearLeg.knee, "legs straight at $which")
            val s = pose.skeleton()
            assertTrue(minOf(-s.near.heel.y, -s.near.toe.y) > 30.0, "feet well off the floor at $which")
        }
    }

    @Test
    fun boardPress_barTouchesTheBoardsOnTheChest() {
        assertCheck("Board press", BarRestsOn(Surface.BOARD, Which.START))
        val boards = figure("Board press").start.props.filterIsInstance<BenchBoards>().single()
        val chest = skeleton("Board press", Which.START).chest()
        assertEquals(chest.y, boards.bottom, 0.01)
        assertTrue(abs(boards.x - chest.x) < 1.0)
    }

    @Test
    fun wideGripBench_forearmsVertical_shorterLockout() {
        val s = skeleton("Wide-grip bench", Which.START)
        assertEquals(s.near.elbow.x, s.near.wrist.x, 0.5, "forearm vertical")
        val out = figure("Wide-grip bench").start.pose.nearArm.out
        assertTrue(out in 25.0..50.0, "upper arm out $out")
        // Lockout lower than the Bench's: the shorter range.
        val lower = bar("Wide-grip bench", Which.END).y - bar("Bench", Which.END).y
        assertTrue(lower in 2.0..8.0, "lockout $lower cm lower")
    }

    @Test
    fun floorPress_lyingOnTheFloor_upperArmsTouchIt() {
        for (point in listOf(BodyPoint.HEAD, BodyPoint.UPPER_BACK, BodyPoint.HIPS, BodyPoint.FEET)) {
            assertContact("Barbell floor press", point, Surface.FLOOR)
        }
        assertContact("Barbell floor press", BodyPoint.ELBOWS, Surface.FLOOR, setOf(Which.START))
        assertCheck("Barbell floor press", LockoutOverShoulders(Which.END))
        val s = skeleton("Barbell floor press", Which.START)
        // Forearms vertical, the bar over the chest and clear of it.
        assertEquals(s.near.elbow.x, s.near.wrist.x, 0.5, "forearm vertical")
        val b = bar("Barbell floor press", Which.START)
        assertTrue(abs(b.x - s.chest().x) < 8.0, "bar over the chest")
        assertTrue(s.chest().y - b.y > 8.0, "the bar stays off the chest")
        // Knees bent, up off the floor.
        assertTrue(s.near.knee.y < -25.0, "knees up")
    }

    // ---- pulls ----

    @Test
    fun pulls_barOverMidfoot_closeToTheLegs() {
        for (name in listOf("Deficit deadlift", "Paused deadlift", "Paused deadlift at the knee", "Block pull")) {
            for (which in Which.entries) {
                assertCheck(name, BarOverMidfoot(which))
                assertTrue(figure(name).checks.any { it is BarNearLegs && it.at == which }, "$name: bar near the legs at $which")
            }
        }
        for (name in listOf("Paused deadlift", "Paused deadlift at the knee", "Block pull", "Sumo deadlift")) assertContact(name, BodyPoint.FEET, Surface.FLOOR)
        // Block pull: the blocks under the plates only, no wider than the plates.
        assertTrue(figure("Block pull").start.props.filterIsInstance<PullingBlocks>().single().length <= Barbell.PLATE_DIAMETER)
    }

    @Test
    fun deficitDeadlift_onThePlatform_platesOnTheFloor_hipsLower() {
        assertContact("Deficit deadlift", BodyPoint.FEET, Surface.PLATFORM)
        val platform = figure("Deficit deadlift").start.props.filterIsInstance<DeficitPlatform>().single()
        assertTrue(platform.height in 2.5..10.0)
        val b = bar("Deficit deadlift", Which.START)
        assertEquals(0.0, b.y + Barbell.PLATE_DIAMETER / 2, 0.01, "plates on the floor")
        // Hips lower than the Deadlift's set-up, relative to the feet.
        val hipAboveFeet = -skeleton("Deficit deadlift", Which.START).pelvis.y - platform.height
        assertTrue(hipAboveFeet < -skeleton("Deadlift", Which.START).pelvis.y - 2.0, "hips $hipAboveFeet")
    }

    @Test
    fun pausedDeadlifts_holdJustOffTheFloor_andJustBelowTheKnee() {
        assertEquals(figure("Deadlift").start.pose, figure("Paused deadlift").start.pose)
        val off = -bar("Paused deadlift", Which.END).y - Barbell.PLATE_DIAMETER / 2
        assertTrue(off in 2.0..8.0, "plates $off cm off the floor")
        // The torso keeps its angle (hips and bar rise together), within 5°.
        assertTrue(abs(torsoAboveLevel("Paused deadlift", Which.END) - torsoAboveLevel("Deadlift", Which.START)) < 5.0)

        val s = skeleton("Paused deadlift at the knee", Which.END)
        val b = bar("Paused deadlift at the knee", Which.END)
        val under = b.y - s.near.knee.y
        assertTrue(under in 3.0..9.0, "bar $under cm below the knee joint")
        assertTrue(shinLean("Paused deadlift at the knee", Which.END) in 0.0..8.0, "shins back near vertical")
        assertTrue(s.shoulder.x - b.x in 0.0..5.0, "shoulders just ahead of the bar")
    }

    @Test
    fun blockPull_platesOnTheBlocks() {
        assertCheck("Block pull", BarRestsOn(Surface.BLOCK, Which.START))
        val blocks = figure("Block pull").start.props.filterIsInstance<PullingBlocks>().single()
        assertTrue(blocks.height in 5.0..15.0)
        assertTrue(abs(blocks.x - bar("Block pull", Which.START).x) < 1.0, "blocks under the plates")
    }

    @Test
    fun sumoDeadlift_frontView_wideStance_handsBetweenTheKnees() {
        val f = figure("Sumo deadlift")
        assertEquals(View.FRONT, f.view)
        assertCheck("Sumo deadlift", HandsInsideKnees(Which.START))
        val start = f.start.pose
        val s = start.skeleton(View.FRONT)
        // Very wide feet: the ankles at least twice the hip width apart, inside the plates.
        val feet = s.near.ankle.x - s.far.ankle.x
        assertTrue(feet in 60.0..Barbell.INNER_COLLARS - 20.0, "feet $feet cm apart")
        // The bar on the floor, the arms straight down, the hands clearly between the knees.
        val b = Barbell().center(s)
        assertEquals(-Barbell.PLATE_DIAMETER / 2, b.y, 0.01, "plates on the floor")
        assertEquals(0.0, start.nearArm.elbow)
        assertTrue(start.nearArm.shoulder in 0.0..10.0, "arms hang near straight down")
        assertTrue(s.near.knee.x - s.near.grip.x > 10.0, "hand ${s.near.knee.x - s.near.grip.x} cm inside the knee")
        // Chest high, hips low: the torso leans 35–45° (more upright than the Deadlift's 59° from vertical), the hips
        // lower than the Deadlift's set-up and near knee height.
        assertTrue(start.lean in 35.0..45.0 && 90.0 - start.lean > torsoAboveLevel("Deadlift", Which.START) + 10.0, "lean ${start.lean}")
        assertTrue(-s.pelvis.y < -skeleton("Deadlift", Which.START).pelvis.y - 8.0, "hips ${-s.pelvis.y}")
        assertTrue(s.pelvis.y - s.near.knee.y in -5.0..0.0, "hips just above the knees")
        // The true hip angles stay inside the limits (the drawn abduction is only a projection).
        assertTrue(JointLimits.trueAbduction(start.nearLeg, View.FRONT) in 30.0..40.0)
        assertTrue(JointLimits.trueFrontHipFlexion(start.nearLeg, start.lean) in 100.0..130.0)
        // END: standing tall on the same wide feet.
        val e = f.end.pose.skeleton(View.FRONT)
        assertEquals(s.near.ankle.x, e.near.ankle.x, 0.01, "same feet")
        assertEquals(0.0, f.end.pose.nearLeg.knee)
        assertEquals(0.0, f.end.pose.lean)
    }

    @Test
    fun trapBarDeadlift_handlesBesideTheLegs_hipsLowShouldersHigh() {
        val f = figure("Trap-bar deadlift")
        for (which in Which.entries) {
            assertTrue(TrapBar in f.scene(which).props)
            assertCheck("Trap-bar deadlift", BarOverMidfoot(which))
            assertCheck("Trap-bar deadlift", HandlesBesideLegs(within = Body.SHIN_WIDTH / 2, at = which))
        }
        assertContact("Trap-bar deadlift", BodyPoint.FEET, Surface.FLOOR)
        // More upright and more knee bend than the Deadlift: knees 85–95° (an inside angle of 85–95°).
        val start = f.start.pose
        assertTrue(start.nearLeg.knee in 85.0..95.0 && start.nearLeg.knee > figure("Deadlift").start.pose.nearLeg.knee + 5.0, "knee ${start.nearLeg.knee}")
        assertTrue(start.torso > torsoAboveLevel("Deadlift", Which.START) + 6.0, "torso ${start.torso}")
        // The arms hang straight down to the handles.
        val s = start.skeleton()
        assertEquals(s.shoulder.x, s.near.grip.x, 1.0, "arms vertical")
    }

    @Test
    fun mainLifts_resolveByAlias() {
        assertEquals(figure("Trap-bar deadlift"), Library.figure("Hex bar deadlift"))
        assertEquals(figure("Barbell floor press"), Library.figure("Floor press"))
        assertEquals(figure("Safety-bar squat"), Library.figure("SSB squat"))
    }

    private companion object {
        val BENCH = listOf(
            "Spoto press", "Long-pause bench", "Pin press", "Larsen press", "Close-grip bench", "Board press", "Wide-grip bench",
        )
        val BATCH = listOf(
            "Pause squat", "Front squat", "Pin squat", "Safety-bar squat", "Box squat", "High-bar squat", "Tempo squat",
        ) + BENCH + listOf(
            "Barbell floor press", "Deficit deadlift", "Paused deadlift", "Paused deadlift at the knee", "Block pull",
            "Sumo deadlift", "Trap-bar deadlift",
        )
    }
}
