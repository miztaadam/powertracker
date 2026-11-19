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
        assertEquals(DropTimeline(115.0, 250.0, 330.0, 520.0, 640.0, 1120.0, .42), m.DROP)
        assertEquals(listOf(90.0, 400.0, 750.0), m.PUMP.beats)
        assertEquals(listOf(.36, .62, 1.0), m.PUMP.amps)
        assertEquals(listOf(110.0, 120.0, 180.0), m.PUMP.rise)
        assertEquals(listOf(190.0, 210.0, 370.0), m.PUMP.fall)
        assertEquals(listOf(175.0, 390.0, 1000.0), m.PUMP.reach)
        assertEquals(listOf(200.0, 230.0, 420.0), m.PUMP.surge)
        assertEquals(380.0, m.PUMP.flood)
        assertEquals(1330.0, m.PUMP.end)
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

        val f120 = m.dropFrame(120.0, w, h)
        pose(f120.hammer, 259.00, -69.99, -32.00, .420, .420, "120")
        near(.115, f120.shadowOpacity, .001); near(67.68, f120.shadowRx)

        val f = m.dropFrame(249.0, w, h)
        pose(f.hammer, 196.60, 735.38, -.80, .404, .461, "249")
        near(.496, f.shadowOpacity, .001); near(101.736, f.shadowRx); near(30.912, f.shadowRy)
        assertEquals(listOf(.32, .2, .1), f.ghosts.map { it.opacity })
        pose(f.ghosts[0].pose, 214.89, 499.27, -9.95, .408, .449, "249 ghost")
        near(0.0, f.flashOpacity); near(0.0, f.revealRadius); near(0.0, f.runeOpacity); near(0.0, f.cracksOpacity)
        near(0.0, f.ring1.opacity); near(0.0, f.burstOpacity)
        assertTrue(f.dust.all { it.opacity == 0.0 } && f.sparks.all { it.opacity == 0.0 })
    }

    @Test
    fun dropAtImpact() {
        val f = m.dropFrame(250.0, w, h)
        assertEquals(195.0, f.hitX); assertEquals(756.0, f.hitY)
        pose(f.hammer, 195.0, 760.0, 0.0, .479, .336, "250")
        pose(f.ghosts[0].pose, 213.64, 515.42, -9.32, .408, .450, "250 ghost")
        near(0.0, f.shadowOpacity); near(1.0, f.flashOpacity); near(1.0, f.runeOpacity); near(1.0, f.burstOpacity)
        near(.5, f.burstScale); near(30.0, f.revealRadius)
        near(10.16, f.shakeX); near(36.0, f.shakeY); near(2.02, f.shakeRotation); near(1.07, f.shakeScale); near(12.0, f.deviceShakeY)
        near(1.7, f.stampScale); near(1.0, f.stampOpacity)
        near(.9, f.ring1.opacity); near(20.0, f.ring1.radius); near(26.5, f.ring1.strokeWidth)
        near(0.0, f.ring2.opacity); near(14.5, f.ring2.strokeWidth)
        near(1.0, f.cracksOpacity); near(0.0, f.crackDrawn)
        val d = f.dust[0]
        near(172.5, d.cx, .06); near(748.9, d.cy, .06); near(21.4, d.r, .06); near(.844, d.opacity, .001)
        val s = f.sparks[0]
        near(195.0, s.x1, .06); near(750.0, s.y1, .06); near(1.0, s.opacity)
    }

    @Test
    fun dropAfterImpact() {
        val f = m.dropFrame(300.0, w, h)
        pose(f.hammer, 195.0, 760.0, 0.0, .479, .336, "300")
        assertTrue(f.ghosts.all { it.opacity == 0.0 })
        near(.919, f.runeOpacity, .001); near(542.1, f.revealRadius, .06); near(.667, f.flashOpacity, .001)
        near(.706, f.burstOpacity, .001); near(1.472, f.burstScale, .001)
        near(12.72, f.shakeX); near(4.96, f.shakeY); near(1.165, f.shakeRotation, .001); near(1.0517, f.shakeScale, .0001)
        near(1.375, f.stampScale, .001)
        near(.768, f.ring1.opacity, .001); near(316.0, f.ring1.radius, .06); near(22.68, f.ring1.strokeWidth)
        near(.070, 1 - f.crackDrawn, .001)
        val d = f.dust[0]
        near(150.7, d.cx, .06); near(739.6, d.cy, .06); near(33.3, d.r, .06); near(.771, d.opacity, .001)
        val s = f.sparks[0]
        near(237.5, s.x1, .06); near(703.9, s.y1, .06); near(270.7, s.x2, .06); near(666.3, s.y2, .06); near(.881, s.opacity, .001)

        val f400 = m.dropFrame(400.0, w, h)
        pose(f400.hammer, 198.90, 737.26, 4.68, .420, .420, "400")
        near(818.7, f400.revealRadius, .06)
        near(394.1, f400.ring2.radius, .06); near(.724, f400.ring2.opacity, .001)

        val f520 = m.dropFrame(520.0, w, h)
        pose(f520.hammer, 195.0, 756.0, 0.0, .42, .42, "520")
        assertTrue(f520.revealFull); near(0.0, f520.flashOpacity)
        near(-1.77, f520.shakeX); near(1.69, f520.shakeY); near(.428, f520.shakeRotation, .001)
        near(.185, f520.ring1.opacity, .001); near(793.2, f520.ring1.radius, .06)

        val f640 = m.dropFrame(640.0, w, h)
        pose(f640.hammer, 195.0, 756.0, 0.0, .42, .42, "640")
        near(.371, f640.runeOpacity, .001); near(.976, f640.cracksOpacity, .001); near(1.005, f640.stampScale, .001)
        near(.254, f640.ring2.opacity, .001); near(782.4, f640.ring2.radius, .06)

        val f900 = m.dropFrame(900.0, w, h)
        pose(f900.hammer, 200.79, 570.84, -2.70, .42, .42, "900")
        near(.357, f900.cracksOpacity, .001); near(1.0, f900.shakeScale)
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
        near(1.0, f90.contentScale); near(0.0, f90.tintOpacity); near(1.0, f90.veinsOpacity); near(.16, f90.glowLayerOpacity)
        near(.540, f90.halos[0].opacity, .001); near(26.0, f90.halos[0].radius); near(0.0, f90.halos[1].opacity)
        near(.32, f90.rings[0].opacity); near(20.0, f90.rings[0].radius); near(13.0, f90.rings[0].strokeWidth)
        near(0.0, f90.floodRadius); assertEquals(false, f90.floodFull); near(0.0, f90.edgeOpacity); near(1.0, f90.pillScale)
        near(0.0, f90.veinFront)

        val f150 = m.pumpFrame(150.0, w, h, src)
        near(1.0062, f150.contentScale, .0001); near(1.0066, f150.deviceScale, .0001); near(.039, f150.tintOpacity, .001)
        near(.466, f150.halos[0].opacity, .001); near(50.9, f150.halos[0].radius, .06)
        near(82.9, f150.rings[0].radius, .06); near(11.43, f150.rings[0].strokeWidth)
        near(.270, f150.glowLayerOpacity, .001); near(.668, f150.nodeOpacity, .001); near(3.08, 2.6 * f150.nodeScale)
        near(.769, f150.pulseOpacity, .001)

        val f400 = m.pumpFrame(400.0, w, h, src)
        near(.011, f400.tintOpacity, .001); near(.160, f400.halos[0].opacity, .001); near(94.2, f400.halos[0].radius, .06)
        near(.709, f400.halos[1].opacity, .001)
        near(.104, f400.rings[0].opacity, .001); near(197.4, f400.rings[0].radius, .06); near(.32, f400.rings[1].opacity)
        near(.190, f400.glowLayerOpacity, .001); near(.582, f400.nodeOpacity, .001); near(0.0, f400.pulseOpacity)

        val f750 = m.pumpFrame(750.0, w, h, src)
        near(.015, f750.tintOpacity, .001); near(.9, f750.halos[2].opacity); near(.145, f750.halos[1].opacity, .001)
        near(.077, f750.rings[1].opacity, .001); near(423.9, f750.rings[1].radius, .06)
        near(12.0, f750.floodRadius, .06); assertEquals(false, f750.floodFull)
        near(.5, f750.edgeOpacity); near(8.0, f750.edgeWidth); near(.2, f750.edgeSoftOpacity); near(1.0, f750.pillScale)

        val f900 = m.pumpFrame(900.0, w, h, src)
        near(1.028, f900.contentScale, .0001); near(1.0299, f900.deviceScale, .0001); near(.068, f900.tintOpacity, .001)
        near(308.2, f900.floodRadius, .06); near(.422, f900.edgeOpacity, .001); near(5.63, f900.edgeWidth)
        near(.121, f900.edgeSoftOpacity, .001); near(.593, f900.halos[2].opacity, .001); near(175.9, f900.halos[2].radius, .06)
    }

    @Test
    fun pumpWhenTheFloodHasCovered() {
        val f = m.pumpFrame(m.PUMP.beats[2] + m.PUMP.flood, w, h, src)
        assertTrue(f.floodFull)
        near(.9998, f.contentScale, .0001); near(.020, f.tintOpacity, .001); near(.667, f.veinsOpacity, .001)
        near(.217, f.glowLayerOpacity, .001); near(.123, f.halos[2].opacity, .001); near(235.5, f.halos[2].radius, .06)
        near(0.0, f.edgeOpacity); near(1.258, f.pillScale, .001)

        val rest = m.pumpFrame(1330.0, w, h, src)
        assertTrue(rest.floodFull); near(1.0, rest.contentScale); near(1.0, rest.deviceScale); near(0.0, rest.veinsOpacity)
        near(0.0, rest.tintOpacity); near(1.0, rest.pillScale)
        assertTrue((rest.halos + rest.rings).all { it.opacity == 0.0 })
    }

    @Test
    fun pumpElementsSwellNearestTheSwitchFirst() {
        val centres = listOf(76.0, 198.5, 328.0, 394.0, 460.0, 526.0, 720.5)
        val at150 = listOf(1.0, 1.0, 1.0, 1.0, 1.0004, 1.0011, 1.0031)
        val at900 = listOf(1.005, 1.0096, 1.0152, 1.0181, 1.0208, 1.0233, 1.0271)
        val at1130 = listOf(1.0221, 1.0177, 1.0131, 1.0108, 1.0086, 1.0067, 1.0037)
        centres.forEachIndexed { i, c ->
            near(at150[i], m.pumpElementScale(150.0, w, c, src), .0001, "150 $i")
            near(at900[i], m.pumpElementScale(900.0, w, c, src), .0001, "900 $i")
            near(at1130[i], m.pumpElementScale(1130.0, w, c, src), .0001, "1130 $i")
        }
        assertEquals(1.0, m.pumpElementScale(1330.0, w, 720.5, src))
    }

    @Test
    fun veinsGrowWithTheFrontAsOnThePage() {
        // the page's first 12 glow veins, as drawn fractions (1 - stroke-dashoffset), at the second and third beats
        val veins = m.pumpShapes(w, h, src, ModeSwitchMotion.REF_FAN_CENTRE, 1.0).veins.take(12)
        val at400 = listOf(.7978, .8437, .1771, .1858, .4352, .5640, .8437, .2656, 0.0, 0.0, 0.0, 0.0)
        val at750 = listOf(1.0, 1.0, 1.0, 1.0, .9698, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, .6299)
        veins.forEachIndexed { i, v ->
            near(at400[i], m.veinProgress(m.pumpFrame(400.0, w, h, src).veinFront, v), .0001, "400 $i")
            near(at750[i], m.veinProgress(m.pumpFrame(750.0, w, h, src).veinFront, v), .0001, "750 $i")
        }
        assertEquals(listOf(9.0, 6.0, 4.0, 6.0, 9.0, 6.0, 6.0, 4.0, 6.0, 4.0, 6.0, 6.0), veins.map { it.width })
    }

    private fun numbers(d: String) = Regex("-?\\d+(\\.\\d+)?").findAll(d).map { it.value.toDouble() }.toList()

    private fun sameShape(expected: String, actual: String, dx: Double = 0.0, dy: Double = 0.0) {
        assertEquals(expected.filter { it.isLetter() }, actual.filter { it.isLetter() }, "commands of $actual")
        val e = numbers(expected)
        val a = numbers(actual)
        assertEquals(e.size, a.size, actual)
        e.indices.forEach { i -> near(e[i], a[i] - if (i % 2 == 0) dx else dy, .051, "number $i of $actual") }
    }

    @Test
    fun crackDustSparkAndBurstSeedsMatchThePage() {
        val hit = Point(195.0, 756.0)
        val cracks = m.cracks(w, h, hit)
        assertEquals(38, cracks.size)
        sameShape("M195,756 L218.7,754.1 L242.3,752.5 L265.9,749.4 L288.4,742.1 L311.3,735.6 L334.8,732.9 L357.6,726.2", cracks[0].d)
        near(4.643086400674656, cracks[0].strokeWidth, 1e-9); assertEquals(.55, cracks[0].opacity)
        sameShape("M265.9,749.4 L293.7,711.9 L346.4,702.0", cracks[2].d)
        // the last crack's branch: every draw of rng(7) before it lines up
        sameShape("M323.6,736.6 L340.5,705.7 L378.8,692.7", cracks.last().d)
        assertEquals(30, m.dust(hit).size)
        assertEquals(34, m.sparks(hit).size)
        // the burst is the last user of rng(7)
        val page = "203.1,0.0 36.8,2.4 156.3,20.9 30.3,6.4 149.8,46.1 15.6,6.8 62.8,39.9 9.3,9.3 25.8,53.6 0.0,11.3 -21.0,43.7 " +
            "-10.5,10.6 -71.9,45.7 -15.5,6.8 -80.9,24.9 -29.0,6.1 -140.7,18.8 -28.1,1.8 -109.5,0.0 -30.1,-1.9 -174.8,-23.3 -32.0,-6.8 " +
            "-124.0,-38.1 -22.4,-9.8 -104.3,-66.2 -13.1,-13.2 -24.3,-50.6 -0.0,-13.6 33.1,-68.9 12.3,-12.4 101.7,-64.6 22.1,-9.7 " +
            "137.2,-42.2 25.0,-5.3 128.6,-17.2 35.4,-2.3"
        val expected = numbers(page)
        val burst = m.burst(hit).flatMap { listOf(it.x, it.y) }
        assertEquals(expected.size, burst.size)
        expected.indices.forEach { near(expected[it], burst[it], .051, "burst $it") }
    }

    @Test
    fun prototypeFanMatchesThePage() {
        val shapes = m.pumpShapes(w, h, src, ModeSwitchMotion.REF_FAN_CENTRE, 1.0)
        assertEquals(mapOf("glow" to 82, "core" to 96, "hot" to 7, "pulse" to 7), shapes.veins.groupingBy { it.layer }.eachCount())
        assertEquals(75, shapes.nodes.size)
        // the page draws veins relative to the switch; these are absolute
        sameShape(
            "M0.0,0.0 L-14.6,8.7 Q-29.3,17.3 -45.1,23.6 Q-60.9,29.8 -76.1,37.5 Q-91.2,45.1 -106.0,53.6 Q-120.8,62.0 -134.5,72.0 Q-148.3,81.9 -163.1,90.3 L-177.9,98.8",
            shapes.veins[0].d, src.x, src.y,
        )
        sameShape("M-91.2,45.1 L-90.9,57.1 Q-90.5,69.1 -95.8,79.9 Q-101.1,90.7 -108.6,100.0 L-116.2,109.3", shapes.veins[1].d, src.x, src.y)
        sameShape(
            "M0.0,0.0 L6.9,-15.5 Q13.8,-31.1 23.0,-45.3 Q32.3,-59.6 35.3,-76.4 Q38.4,-93.1 46.1,-108.2 Q53.7,-123.4 64.2,-136.8 Q74.7,-150.1 81.9,-165.6 Q89.1,-181.0 97.7,-195.6 L106.3,-210.3",
            shapes.veins.last().d, src.x, src.y,
        )
        near(-91.2, shapes.nodes[0].x - src.x, .051); near(45.1, shapes.nodes[0].y - src.y, .051)
        near(81.2, shapes.nodes.last().x - src.x, .051); near(-173.3, shapes.nodes.last().y - src.y, .051)
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
        // Measured on 24 switch places: the main veins' ends average within 7.6 degrees of the aim, each lies within
        // 77 of it, and the fan spans at least 104 (the prototype's fan is 120 wide, its veins wander).
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

    /** The two-arc circle the port writes for an SVG `<circle>`, worked out here from the page's attributes. */
    private fun circle(cx: Double, cy: Double, r: Double): String {
        fun n(v: Double) = if (v == kotlin.math.floor(v)) v.toLong().toString() else v.toString()
        return "M${n(cx - r)},${n(cy)} A${n(r)},${n(r)} 0 1,0 ${n(cx + r)},${n(cy)} A${n(r)},${n(r)} 0 1,0 ${n(cx - r)},${n(cy)} Z"
    }

    @Test
    fun theHammerIsTheReferences() {
        // Copied from the page's #hammerShape, element by element (circles as cx, cy, r): d, fill, stroke, stroke width,
        // opacity, line cap, line join. Group attributes are written on each element they apply to.
        val ink = "#0B0C0E"
        val bevel = "M-30,-148 C-70,-148 -108,-138 -146,-113 L-144,-104 C-106,-128 -70,-138 -30,-139 L30,-139 C70,-138 106,-128 144,-104 L146,-113 C108,-138 70,-148 30,-148 Z"
        val border = "M-30,-128 C-66,-127 -98,-120 -130,-100 L-138,-62 Q-78,-46 -28,-22 Q-10,-14 0,-14 Q10,-14 28,-22 Q78,-46 138,-62 L130,-100 C98,-120 66,-127 30,-128 Z"
        val inner = "M-30,-126 C-66,-125 -98,-118 -129,-98.500 L-136.500,-61 Q-78,-44 -28,-20 Q-10,-12 0,-12 Q10,-12 28,-20 Q78,-44 136.500,-61 L129,-98.500 C98,-118 66,-125 30,-126 Z"
        val head = "M-30,-150 C-70,-150 -108,-140 -148,-114 L-160,-52 Q-86,-34 -30,-7 Q-12,0 0,0 Q12,0 30,-7 Q86,-34 160,-52 L148,-114 C108,-140 70,-150 30,-150 Z"
        val expected = listOf(
            MotionPath(circle(0.0, -306.0, 11.0), null, "url(#gBand)", 6.0, 1.0),
            MotionPath("M-22,-278 Q-24,-296 0,-296 Q24,-296 22,-278 Z", "url(#gBand)", null, 0.0, 1.0),
            MotionPath("M-15,-280 L15,-280 L21,-150 L-21,-150 Z", "url(#gWood)", null, 0.0, 1.0),
            MotionPath("M-6,-276 L-8,-154 M3,-276 L4,-154 M10,-276 L13,-154", null, "#0C0A08", 1.2, .7),
            MotionPath("M-19,-284 L19,-284 L19.6,-272 L-19.6,-272 Z", "url(#gBand)", null, 0.0, 1.0),
            MotionPath("M-20,-226 L20,-226 L20.6,-214 L-20.6,-214 Z", "url(#gBand)", null, 0.0, 1.0),
            MotionPath("M-25,-176 L25,-176 L34,-142 L-34,-142 Z", "url(#gBand)", null, 0.0, 1.0),
            MotionPath(head, "url(#gIron)", ink, 2.0, 1.0, lineJoin = "round"),
            MotionPath(bevel, "#C9CED6", null, 0.0, .55),
            MotionPath("M-148,-114 L-160,-52 L-146,-56 L-137,-108 Z", "#0F1113", null, 0.0, .7),
            MotionPath("M148,-114 L160,-52 L146,-56 L137,-108 Z", "#9AA1AA", null, 0.0, .35),
            MotionPath(border, null, ink, 2.4, .75),
            MotionPath(inner, null, "#B8BEC6", 1.0, .35),
            MotionPath(circle(-98.0, -84.0, 13.0), null, ink, 2.6, 1.0),
            MotionPath(circle(98.0, -84.0, 13.0), null, ink, 2.6, 1.0),
            MotionPath(circle(-58.0, -66.0, 6.0), null, ink, 2.6, 1.0),
            MotionPath(circle(58.0, -66.0, 6.0), null, ink, 2.6, 1.0),
            MotionPath(circle(-98.0, -84.0, 4.0), ink, null, 0.0, 1.0),
            MotionPath(circle(98.0, -84.0, 4.0), ink, null, 0.0, 1.0),
            MotionPath(circle(-58.0, -66.0, 1.8), ink, null, 0.0, 1.0),
            MotionPath(circle(58.0, -66.0, 1.8), ink, null, 0.0, 1.0),
            MotionPath("M-9,-116 L-9,-36 M-9,-100 L15,-78 L-9,-56", null, "#08090A", 8.0, 1.0, lineCap = "square", lineJoin = "miter"),
        )
        assertEquals(expected.size, m.HAMMER_PATHS.size)
        expected.forEachIndexed { i, e -> assertEquals(e, m.HAMMER_PATHS[i], "hammer shape $i") }
        assertEquals(MotionPath("M-9,-116 L-9,-36 M-9,-100 L15,-78 L-9,-56", null, "#FFC98A", 5.0, 1.0, "square", "miter"), m.HAMMER_RUNE)
        val ids = m.GRADIENTS.map { it.id }.toSet()
        m.HAMMER_PATHS.flatMap { listOfNotNull(it.fill, it.stroke) }.filter { it.startsWith("url(") }
            .forEach { assertTrue(it.removePrefix("url(#").removeSuffix(")") in ids, it) }
    }

    @Test
    fun theGradientsAreTheReferences() {
        // The page's <defs>: linear ones with their x1 y1 x2 y2, stops as offset, colour, opacity.
        fun stops(vararg s: Triple<Double, String, Double>) = s.map { GradientStop(it.first, it.second, it.third) }
        assertEquals(
            listOf(
                MotionGradient("gIron", false, 0.0, 0.0, 0.0, 1.0, stops(Triple(0.0, "#7B828B", 1.0), Triple(.38, "#4A5058", 1.0), Triple(1.0, "#1D2024", 1.0))),
                MotionGradient("gBand", false, 0.0, 0.0, 1.0, 0.0, stops(Triple(0.0, "#2A2E33", 1.0), Triple(.35, "#9AA1AA", 1.0), Triple(1.0, "#23262A", 1.0))),
                MotionGradient("gWood", false, 0.0, 0.0, 1.0, 0.0, stops(Triple(0.0, "#15110E", 1.0), Triple(.4, "#4A3A2C", 1.0), Triple(1.0, "#120F0C", 1.0))),
                MotionGradient("gHalo", true, 0.0, 0.0, 0.0, 0.0, stops(Triple(0.0, "#BFD7FF", .75), Triple(.4, "#6FA4FF", .32), Triple(1.0, "#4F8DFF", 0.0))),
                MotionGradient("gChalk", true, 0.0, 0.0, 0.0, 0.0, stops(Triple(0.0, "#E4E6E2", .9), Triple(.55, "#C9CED6", .45), Triple(1.0, "#C9CED6", 0.0))),
            ),
            m.GRADIENTS,
        )
    }

    @Test
    fun theEffectColoursAreTheReferences_andTheLightSetIsPinned() {
        // The page's effect elements (#fx..., #px...) and the crack builder's strokes.
        with(ModeSwitchMotion.Colors) {
            assertEquals(
                listOf("#000000", "#000000", "#F3F4EF", "#C9CED6", "#F3F4EF", "#F3F4EF", "#93979D", "#FFF6E8", "#FFB36B"),
                listOf(SHADOW, CRACK_SHADOW, CRACK, CRACK_BRANCH, FLASH, RING1, RING2, BURST, SPARK),
            )
            assertEquals(
                listOf("#4F8DFF", "#8DB8FF", "#DCE9FF", "#8DB8FF", "#4F8DFF", "#8DB8FF", "#EAF2FF", "#FFFFFF", "#DCE9FF"),
                listOf(TINT, PUMP_RING, EDGE, EDGE_SOFT, VEIN_GLOW, VEIN_CORE, VEIN_HOT, VEIN_PULSE, NODE),
            )
        }
        assertEquals(2.4, m.SPARK_WIDTH)
        // The light scheme's set (spec "Settled" 1), the one both apps draw.
        with(ModeSwitchMotion.LightColors) {
            assertEquals("#2A2E33", FLASH); assertEquals(.5, FLASH_OPACITY_SCALE); assertEquals(.9, CRACK_SHADOW_OPACITY)
            assertEquals(listOf(GradientStop(0.0, "#E4E6E2", .9), GradientStop(.55, "#8A9098", .5), GradientStop(1.0, "#8A9098", 0.0)), CHALK)
            assertEquals("#1E3F7A", VEIN_EDGE); assertEquals(.55, VEIN_EDGE_OPACITY)
            assertEquals("#2A2E33", RING1); assertEquals("#C2570F", SPARK); assertEquals("#2A2E33", BURST_EDGE); assertEquals(1.5, BURST_EDGE_WIDTH)
        }
        // The edge is the core's swollen width plus 1.6.
        near(3.4 * 1.2 + 1.6, m.veinEdgeWidth(3.4, 1.2), 1e-9)
        assertTrue(m.nodeShown(56.1, 50.0)); assertTrue(!m.nodeShown(56.0, 50.0))
    }

    @Test
    fun theStampFadesFromTheLiftToTheEnd() {
        near(1.0, m.dropFrame(m.DROP.lift, w, h).stampOpacity, 1e-9)
        near(.5, m.dropFrame(880.0, w, h).stampOpacity, 1e-9)
        near(.25, m.dropFrame(1000.0, w, h).stampOpacity, 1e-9)
        near(0.0, m.dropFrame(m.DROP.impact - 1, w, h).stampOpacity, 1e-9)
    }
}
