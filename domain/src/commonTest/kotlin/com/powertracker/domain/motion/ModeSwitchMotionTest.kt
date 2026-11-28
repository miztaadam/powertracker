package com.powertracker.domain.motion

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Expected values were read from the approved prototype, docs/design/motion/mode-switch-sound.html, in Chromium:
 * its seekable clock (`__drop.hold(t)`, `__pump.hold(t)`) and the attributes it then wrote. The prototype's screen
 * is 390 x 760, its hit (195, 756) and its switch (341, 715).
 */
class ModeSwitchMotionTest {
    private val m = ModeSwitchMotion
    private val w = 390.0
    private val h = 760.0
    private val src = Point(341.0, 715.0)

    private fun near(expected: Double, actual: Double, tol: Double = .01, what: String = "") =
        assertTrue(abs(expected - actual) <= tol, "$what: expected $expected, got $actual")

    private fun pose(p: Pose?, x: Double, y: Double, r: Double, sx: Double, sy: Double, what: String) {
        assertNotNull(p, what)
        near(x, p.x, what = "$what x"); near(y, p.y, what = "$what y"); near(r, p.rotation, what = "$what r")
        near(sx, p.scaleX, .0006, "$what sx"); near(sy, p.scaleY, .0006, "$what sy")
    }

    @Test
    fun timelinesAreTheReferences() {
        assertEquals(DropTimeline(95.0, 250.0, 335.0, 525.0, 650.0, 1120.0, .44), m.DROP)
        assertEquals(listOf(90.0, 400.0, 750.0), m.PUMP.beats)
        assertEquals(listOf(.38, .65, 1.0), m.PUMP.amps)
        assertEquals(listOf(110.0, 120.0, 180.0), m.PUMP.rise)
        assertEquals(listOf(190.0, 210.0, 370.0), m.PUMP.fall)
        assertEquals(listOf(175.0, 390.0, 1000.0), m.PUMP.reach)
        assertEquals(listOf(200.0, 230.0, 420.0), m.PUMP.surge)
        assertEquals(380.0, m.PUMP.flood)
        assertEquals(1330.0, m.PUMP.end)
        // Original vibration timings and patterns stay intact to match the original sounds.
        assertEquals(listOf(420L, 35, 180, 45, 110, 60, 60, 80, 30), m.DROP_VIBE.toList())
        assertEquals(listOf(28L, 55, 120), m.PUMP_VIBE.toList())
    }

    @Test
    fun patternsAndAmplitudesHaveTheSameLength() {
        assertEquals(m.DROP_VIBE.size, m.DROP_AMPLITUDES.size)
        assertEquals(m.PUMP_VIBE.size, m.PUMP_AMPLITUDES.size)
        assertEquals(255, m.DROP_AMPLITUDES[0])
        assertEquals(255, m.PUMP_AMPLITUDES.last())
        m.DROP_AMPLITUDES.forEachIndexed { i, a -> if (i % 2 == 1) assertEquals(0, a, "off slot $i") else assertTrue(a in 1..255) }
        val on = m.DROP_AMPLITUDES.filterIndexed { i, _ -> i % 2 == 0 }
        assertEquals(on.sortedDescending(), on, "aftershocks step down")
        assertEquals(m.PUMP_AMPLITUDES.sorted(), m.PUMP_AMPLITUDES.toList(), "beats build")
    }

    @Test
    fun dropBeforeImpact() {
        val f0 = m.dropFrame(0.0, w, h)
        assertNull(f0.hammer); near(0.0, f0.shadowOpacity); near(36.0, f0.shadowRx); near(9.0, f0.shadowRy)
        near(0.0, f0.flashOpacity); near(0.0, f0.revealRadius); assertEquals(false, f0.revealFull)
        assertTrue(f0.ghosts.all { it.opacity == 0.0 }); near(1.0, f0.shakeScale); near(0.0, f0.stampOpacity)

        // Overhead wind-up anticipation lifts the hammer slightly before accelerating downward
        val fWind = m.dropFrame(107.0, w, h)
        val windPose = assertNotNull(fWind.hammer)
        assertTrue(windPose.y < -60.0, "wind-up lifts above -60: ${windPose.y}")
        assertTrue(windPose.rotation < -36.0, "wind-up tilts back: ${windPose.rotation}")

        val f = m.dropFrame(249.0, w, h)
        val preHit = assertNotNull(f.hammer)
        assertTrue(preHit.y > 700.0 && preHit.y < 756.0, "hammer approaches hit: ${preHit.y}")
        assertTrue(f.shadowOpacity > .5, "shadow near max before impact")
        assertEquals(listOf(.28, .16, .08), f.ghosts.map { it.opacity })
        near(0.0, f.flashOpacity); near(0.0, f.revealRadius); near(0.0, f.runeOpacity); near(0.0, f.cracksOpacity)
        near(0.0, f.ring1.opacity); near(0.0, f.burstOpacity)
        assertTrue(f.dust.all { it.opacity == 0.0 } && f.sparks.all { it.opacity == 0.0 })
    }

    @Test
    fun dropAtImpact() {
        val f = m.dropFrame(250.0, w, h)
        assertEquals(195.0, f.hitX); assertEquals(756.0, f.hitY)
        pose(f.hammer, 195.0, 760.0, 0.0, 1.15 * .44, .78 * .44, "250")
        near(0.0, f.shadowOpacity); near(1.0, f.flashOpacity); near(1.0, f.runeOpacity); near(1.0, f.burstOpacity)
        near(.5, f.burstScale); near(30.0, f.revealRadius)
        near(10.16, f.shakeX); near(36.0, f.shakeY); near(2.02, f.shakeRotation); near(1.07, f.shakeScale); near(12.0, f.deviceShakeY)
        near(1.7, f.stampScale); near(1.0, f.stampOpacity)
        near(.9, f.ring1.opacity); near(20.0, f.ring1.radius); near(26.5, f.ring1.strokeWidth)
        near(0.0, f.ring2.opacity); near(14.5, f.ring2.strokeWidth)
        near(1.0, f.cracksOpacity); near(0.0, f.crackDrawn)
        assertEquals(30, f.dust.size)
        assertEquals(36, f.sparks.size)
        assertTrue(f.dust.all { it.opacity > 0.0 })
        assertTrue(f.sparks.all { it.opacity == 1.0 })
    }

    @Test
    fun dropAfterImpact() {
        val f = m.dropFrame(300.0, w, h)
        assertNotNull(f.hammer)
        assertTrue(f.ghosts.all { it.opacity == 0.0 })
        assertTrue(f.runeOpacity > .9); near(542.1, f.revealRadius, .06); near(.667, f.flashOpacity, .001)
        near(.706, f.burstOpacity, .001); near(1.472, f.burstScale, .001)
        near(1.375, f.stampScale, .001)
        near(.768, f.ring1.opacity, .001); near(316.0, f.ring1.radius, .06)
        near(.070, 1 - f.crackDrawn, .001)

        val f400 = m.dropFrame(400.0, w, h)
        assertTrue(assertNotNull(f400.hammer).y < 756.0, "recoil lifts hammer")
        assertTrue(f400.ring2.opacity > .7)

        val f530 = m.dropFrame(530.0, w, h)
        pose(f530.hammer, 195.0, 756.0, 0.0, .44, .44, "530 settled")
        assertTrue(f530.revealFull); near(0.0, f530.flashOpacity)

        val f900 = m.dropFrame(900.0, w, h)
        assertTrue(assertNotNull(f900.hammer).y < 620.0, "lifting out of frame")
        near(1.0, f900.shakeScale)
    }

    @Test
    fun dropAtTheEndIsAtRest() {
        val f = m.dropFrame(1120.0, w, h)
        assertNull(f.hammer)
        assertTrue(f.revealFull)
        near(0.0, f.flashOpacity); near(0.0, f.cracksOpacity); near(0.0, f.runeOpacity); near(0.0, f.stampOpacity)
        near(0.0, f.shakeX); near(0.0, f.shakeY); near(1.0, f.shakeScale); near(1.0, f.stampScale)
        near(0.0, f.ring1.opacity); near(0.0, f.ring2.opacity)
        assertTrue(f.dust.all { it.opacity == 0.0 } && f.sparks.all { it.opacity == 0.0 })
    }

    @Test
    fun hopsAsOnThePage() {
        // centres of the page's hopping rows: offsetTop + offsetHeight / 2
        val centres = listOf(76.0, 198.5, 328.0, 394.0, 460.0, 526.0, 720.5)
        val at400 = listOf(-.29, -12.31, -2.6, 5.11, 9.22, 8.06, -6.43)
        centres.forEachIndexed { i, c -> near(at400[i], m.hop(400.0, c, 756.0), what = "hop $i") }
        near(-13.41, m.hop(300.0, 720.5, 756.0))
        near(0.0, m.hop(300.0, 76.0, 756.0))
        near(0.0, m.hop(1120.0, 720.5, 756.0))
    }

    @Test
    fun pumpAtEachBeat() {
        val f90 = m.pumpFrame(90.0, w, h, src)
        near(1.0, f90.contentScale); near(0.0, f90.tintOpacity); near(1.0, f90.veinsOpacity); near(.18, f90.glowLayerOpacity)
        assertTrue(f90.halos[0].opacity > .5); near(26.0, f90.halos[0].radius); near(0.0, f90.halos[1].opacity)
        near(.36, f90.rings[0].opacity); near(20.0, f90.rings[0].radius); near(13.0, f90.rings[0].strokeWidth)
        near(0.0, f90.floodRadius); assertEquals(false, f90.floodFull); near(0.0, f90.edgeOpacity); near(1.0, f90.pillScale)
        near(0.0, f90.veinFront)

        // Cardiac lub-dub: beat 1 peaks around t=225ms with swell, tint and vein pulse
        val f225 = m.pumpFrame(225.0, w, h, src)
        assertTrue(f225.contentScale > 1.01); assertTrue(f225.tintOpacity > .03)
        assertTrue(f225.pulseOpacity > .5)

        val f400 = m.pumpFrame(400.0, w, h, src)
        assertTrue(f400.halos[1].opacity > .7)
        near(.36, f400.rings[1].opacity)

        val f750 = m.pumpFrame(750.0, w, h, src)
        near(.9, f750.halos[2].opacity)
        near(12.0, f750.floodRadius, .06); assertEquals(false, f750.floodFull)
        near(.6, f750.edgeOpacity); near(8.0, f750.edgeWidth); near(.28, f750.edgeSoftOpacity); near(1.0, f750.pillScale)

        val f900 = m.pumpFrame(900.0, w, h, src)
        assertTrue(f900.contentScale > 1.02)
        near(308.2, f900.floodRadius, .06)
    }

    @Test
    fun pumpWhenTheFloodHasCovered() {
        val f = m.pumpFrame(m.PUMP.beats[2] + m.PUMP.flood, w, h, src)
        assertTrue(f.floodFull)
        near(.667, f.veinsOpacity, .001)
        near(0.0, f.edgeOpacity)
        assertTrue(f.pillScale > 1.2)

        val rest = m.pumpFrame(1330.0, w, h, src)
        assertTrue(rest.floodFull); near(1.0, rest.contentScale); near(1.0, rest.deviceScale); near(0.0, rest.veinsOpacity)
        near(0.0, rest.tintOpacity); near(1.0, rest.pillScale)
        assertTrue((rest.halos + rest.rings).all { it.opacity == 0.0 })
    }

    @Test
    fun pumpElementsSwellNearestTheSwitchFirst() {
        val nearSwitch = m.pumpElementScale(150.0, w, 720.5, src)
        val farFromSwitch = m.pumpElementScale(150.0, w, 76.0, src)
        assertTrue(nearSwitch > farFromSwitch, "near $nearSwitch should swell before far $farFromSwitch")
        assertEquals(1.0, m.pumpElementScale(1330.0, w, 720.5, src))
    }

    private fun numbers(d: String) = Regex("-?\\d+(\\.\\d+)?").findAll(d).map { it.value.toDouble() }.toList()

    @Test
    fun dropAndPumpShapeCountsAndDeterminism() {
        val hit = Point(195.0, 756.0)
        val cracks = m.cracks(w, h, hit)
        // 15 cracks * (shadow + main) + 5 hot (i % 3 == 0) + 8 branches (i % 2 == 0) = 43
        assertEquals(43, cracks.size)
        assertEquals(30, m.dust(hit).size)
        assertEquals(36, m.sparks(hit).size)
        assertEquals(36, m.burst(hit).size)
        val shapes = m.pumpShapes(w, h, src, ModeSwitchMotion.REF_FAN_CENTRE, 1.0)
        assertEquals(7, shapes.veins.count { it.layer == VeinPath.LAYER_HOT })
        assertEquals(7, shapes.veins.count { it.layer == VeinPath.LAYER_PULSE })
        assertTrue(shapes.nodes.isNotEmpty())
    }

    @Test
    fun theRevealAndTheFloodCoverTheFarCorner() {
        for ((sw, sh) in listOf(390.0 to 760.0, 320.0 to 640.0, 412.0 to 915.0, 800.0 to 1280.0)) {
            val hit = m.hitPoint(sw, sh)
            val corner = max(hypot(hit.x, hit.y), hypot(sw - hit.x, hit.y))
            val end = m.dropFrame(m.DROP.impact + 169.0, sw, sh)
            assertTrue(m.dropFrame(m.DROP.impact + 170.0, sw, sh).revealFull)
            assertTrue(end.revealRadius >= corner * .99, "drop $sw x $sh: ${end.revealRadius} < $corner")
            assertTrue(m.dropFrame(m.DROP.end, sw, sh).revealFull)

            for (s in listOf(Point(sw - 40, 90.0), Point(40.0, 100.0), Point(sw / 2, sh - 60))) {
                val far = listOf(Point(0.0, 0.0), Point(sw, 0.0), Point(0.0, sh), Point(sw, sh)).maxOf { hypot(it.x - s.x, it.y - s.y) }
                val f = m.pumpFrame(m.PUMP.beats[2] + m.PUMP.flood - 1, sw, sh, s)
                assertTrue(f.floodRadius >= far, "pump $sw x $sh from $s: ${f.floodRadius} < $far")
                assertTrue(m.pumpFrame(m.PUMP.beats[2] + m.PUMP.flood, sw, sh, s).floodFull)
                // every vein is fully drawn by the end of the third surge
                val front = m.pumpFrame(m.PUMP.beats[2] + m.PUMP.surge[2], sw, sh, s).veinFront
                m.veins(sw, sh, s).filter { it.layer != VeinPath.LAYER_PULSE }.forEach { v ->
                    assertEquals(1.0, m.veinProgress(front, v), "vein from $s on $sw x $sh")
                }
            }
        }
        // the hammer leaves the top before it disappears, whatever the screen's height
        for (sh in listOf(640.0, 760.0, 915.0, 1280.0, 2000.0)) {
            val last = m.hammerPose(m.DROP.end - 1, m.hitPoint(400.0, sh))
            assertNotNull(last)
            assertTrue(last.y < 0, "hammer boss at ${last.y} on a screen $sh tall")
        }
        // the prototype's screen is unchanged by the scale rule
        near(820.0, m.dropFrame(m.DROP.impact + 170.0, w, h).revealRadius, 1e-9)
        near(872.0, m.pumpFrame(m.PUMP.end - 1, w, h, src).floodRadius, 1e-9)
    }

    @Test
    fun theVeinFanPointsFromATopLeftSwitchToTheCentre() {
        val sw = 412.0
        val sh = 915.0
        val s = Point(40.0, 110.0)
        val mains = m.veins(sw, sh, s).filter { it.layer == VeinPath.LAYER_PULSE }
        assertEquals(7, mains.size)
        // every main vein starts at the switch and reaches the screen's edge
        mains.forEach { v -> val n = numbers(v.d); near(s.x, n[0], .051); near(s.y, n[1], .051) }
        mains.map { numbers(it.d).takeLast(2) }.forEach { e ->
            val inside = minOf(e[0], sw - e[0], e[1], sh - e[1])
            assertTrue(inside < 40, "a vein stops ${inside} short of the edge at $e")
        }
    }

    /** Degrees from [aim] to [a], both in radians, in -180..180. */
    private fun offDeg(a: Double, aim: Double): Double {
        var d = (a - aim) * 180 / PI
        while (d > 180) d -= 360
        while (d < -180) d += 360
        return d
    }

    @Test
    fun theVeinFanIsCentredOnTheScreensCentre_fromAnywhere() {
        for ((sw, sh) in listOf(412.0 to 915.0, 320.0 to 640.0, 390.0 to 760.0, 800.0 to 1280.0)) {
            val places = listOf(Point(40.0, 110.0), Point(sw - 40, 110.0), Point(sw / 2, 90.0), Point(40.0, sh - 60), Point(sw - 30, sh / 2), Point(sw / 2, sh - 60))
            for (s in places) {
                val aim = atan2(sh / 2 - s.y, sw / 2 - s.x)
                val ends = m.veins(sw, sh, s).filter { it.layer == VeinPath.LAYER_PULSE }.map { numbers(it.d).takeLast(2) }
                val dirs = ends.map { atan2(it[1] - s.y, it[0] - s.x) }
                val mean = atan2(dirs.sumOf { kotlin.math.sin(it) }, dirs.sumOf { kotlin.math.cos(it) })
                val what = "$sw x $sh from $s"
                assertTrue(abs(offDeg(mean, aim)) < 10, "$what: fan centre ${offDeg(mean, aim)} degrees off the aim")
                val offs = dirs.map { offDeg(it, aim) }
                assertTrue(offs.all { abs(it) < 85 }, "$what: a vein outside the fan: $offs")
                assertTrue(offs.max() - offs.min() > 100, "$what: the fan is too narrow: $offs")
            }
        }
    }

    @Test
    fun nothingFlashesMoreThanThreeTimesASecond() {
        fun onsets(value: (Double) -> Double, end: Double): List<Double> {
            val out = ArrayList<Double>()
            var prev = value(0.0)
            var rising = false
            var t = 1.0
            while (t <= end) {
                val v = value(t)
                if (v > prev + 1e-6 && !rising) { out += t; rising = true } else if (v < prev - 1e-6) rising = false
                prev = v; t += 1.0
            }
            return out
        }
        fun maxInASecond(times: List<Double>) = times.maxOfOrNull { a -> times.count { it >= a && it < a + 1000 } } ?: 0
        val flash = onsets({ m.dropFrame(it, w, h).flashOpacity }, m.DROP.end)
        assertTrue(maxInASecond(flash) <= 3, "drop flashes at $flash")
        val tint = onsets({ m.pumpFrame(it, w, h, src).tintOpacity }, m.PUMP.end)
        assertTrue(maxInASecond(tint) <= 3, "pump tint pulses at $tint")
        assertEquals(3, tint.size)
        val halos = onsets({ t -> m.pumpFrame(t, w, h, src).halos.maxOf { it.opacity } }, m.PUMP.end)
        assertTrue(maxInASecond(halos) <= 3, "pump halos at $halos")
    }

    @Test
    fun seededShapesAreTheSameOnEveryCall() {
        val hit = m.hitPoint(412.0, 915.0)
        assertEquals(m.cracks(412.0, 915.0, hit), m.cracks(412.0, 915.0, hit))
        assertEquals(m.dust(hit), m.dust(hit))
        assertEquals(m.sparks(hit), m.sparks(hit))
        assertEquals(m.burst(hit), m.burst(hit))
        val s = Point(370.0, 96.0)
        val a = m.veins(412.0, 915.0, s)
        m.cracks(390.0, 760.0, Point(195.0, 756.0)) // another hit in between
        m.veins(390.0, 760.0, src)
        assertEquals(a, m.veins(412.0, 915.0, s))
        assertEquals(buildDropShapes(hit).cracks, m.cracks(412.0, 915.0, hit))
    }

    @Test
    fun theHammerIsForgedAndReferencesValidGradients() {
        assertEquals(24, m.HAMMER_PATHS.size)
        assertEquals(6, m.HAMMER_RUNE_PATHS.size)
        assertEquals("#FFB84D", m.HAMMER_RUNE.stroke)
        assertEquals(6, m.GRADIENTS.size)
        val ids = m.GRADIENTS.map { it.id }.toSet()
        assertTrue("gHalo" in ids && "gChalk" in ids && "gEmber" in ids && "gSteelFace" in ids)
        (m.HAMMER_PATHS + m.HAMMER_RUNE_PATHS)
            .flatMap { listOfNotNull(it.fill, it.stroke) }
            .filter { it.startsWith("url(") }
            .forEach { assertTrue(it.removePrefix("url(#").removeSuffix(")") in ids, it) }
    }

    @Test
    fun theEffectColoursAndLightSetArePinned() {
        with(ModeSwitchMotion.Colors) {
            assertEquals(
                listOf("#000000", "#000000", "#F3F4EF", "#FF9E3D", "#C9CED6", "#F3F4EF", "#F3F4EF", "#FFB36B", "#FFF3D6", "#FFB36B"),
                listOf(SHADOW, CRACK_SHADOW, CRACK, CRACK_HOT, CRACK_BRANCH, FLASH, RING1, RING2, BURST, SPARK),
            )
            assertEquals(
                listOf("#3B82F6", "#8DB8FF", "#EFF6FF", "#60A5FA", "#3B82F6", "#7DD3FC", "#F0F9FF", "#FFFFFF", "#E0F2FE"),
                listOf(TINT, PUMP_RING, EDGE, EDGE_SOFT, VEIN_GLOW, VEIN_CORE, VEIN_HOT, VEIN_PULSE, NODE),
            )
        }
        assertEquals(2.6, m.SPARK_WIDTH)
        with(ModeSwitchMotion.LightColors) {
            assertEquals("#2A2E33", FLASH); assertEquals(.5, FLASH_OPACITY_SCALE); assertEquals(.9, CRACK_SHADOW_OPACITY)
            assertEquals(listOf(GradientStop(0.0, "#E4E6E2", .9), GradientStop(.55, "#8A9098", .5), GradientStop(1.0, "#8A9098", 0.0)), CHALK)
            assertEquals("#1E3F7A", VEIN_EDGE); assertEquals(.55, VEIN_EDGE_OPACITY)
            assertEquals("#2A2E33", RING1); assertEquals("#C2570F", SPARK); assertEquals("#2A2E33", BURST_EDGE); assertEquals(1.5, BURST_EDGE_WIDTH)
        }
        near(3.4 * 1.2 + 1.6, m.veinEdgeWidth(3.4, 1.2), 1e-9)
        assertTrue(m.nodeShown(56.1, 50.0)); assertTrue(!m.nodeShown(56.0, 50.0))
    }

    @Test
    fun theStampFadesFromTheLiftToTheEnd() {
        near(1.0, m.dropFrame(m.DROP.lift, w, h).stampOpacity, 1e-9)
        near(.5, m.dropFrame((m.DROP.lift + m.DROP.end) / 2, w, h).stampOpacity, 1e-9)
        near(.25, m.dropFrame(m.DROP.lift + (m.DROP.end - m.DROP.lift) * .75, w, h).stampOpacity, 1e-9)
        near(0.0, m.dropFrame(m.DROP.impact - 1, w, h).stampOpacity, 1e-9)
    }
}
