package com.powertracker.domain.web

import com.powertracker.domain.motion.ModeSwitchMotion
import com.powertracker.domain.motion.Point
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModeSwitchBridgeTest {
    @Test
    fun timelinesAndVibrationCrossAsNumbers() {
        val drop = GritDomain.modeSwitchDropTimeline()
        assertEquals(250.0, drop.impact); assertEquals(1120.0, drop.end); assertEquals(.42, drop.hammerScale)
        val pump = GritDomain.modeSwitchPumpTimeline()
        assertContentEquals(arrayOf(90.0, 400.0, 750.0), pump.beats); assertEquals(380.0, pump.flood); assertEquals(1330.0, pump.end)
        val v = GritDomain.modeSwitchVibration()
        assertContentEquals(arrayOf(420, 35, 180, 45, 110, 60, 60, 80, 30), v.dropPattern)
        assertContentEquals(arrayOf(28, 55, 120), v.pumpPulses)
        assertEquals(v.dropPattern.size, v.dropAmplitudes.size); assertEquals(v.pumpPulses.size, v.pumpAmplitudes.size)
    }

    @Test
    fun framesMatchTheDomain() {
        val f = GritDomain.modeSwitchDropFrame(250.0, 390.0, 760.0)
        val d = ModeSwitchMotion.dropFrame(250.0, 390.0, 760.0)
        assertEquals(d.flashOpacity, f.flashOpacity); assertEquals(d.revealRadius, f.revealRadius)
        assertEquals(d.hammer!!.scaleX, assertNotNull(f.hammer).scaleX)
        assertEquals(30, f.dust.size); assertEquals(34, f.sparks.size); assertEquals(3, f.ghosts.size)
        assertNull(GritDomain.modeSwitchDropFrame(1120.0, 390.0, 760.0).hammer)

        val p = GritDomain.modeSwitchPumpFrame(900.0, 390.0, 760.0, 341.0, 715.0)
        assertEquals(ModeSwitchMotion.pumpFrame(900.0, 390.0, 760.0, Point(341.0, 715.0)).floodRadius, p.floodRadius)
        assertEquals(3, p.halos.size); assertEquals(2, p.rings.size)
        assertEquals(ModeSwitchMotion.hop(400.0, 460.0, 756.0), GritDomain.modeSwitchHop(400.0, 460.0, 756.0))
        assertEquals(
            ModeSwitchMotion.pumpElementScale(900.0, 390.0, 460.0, Point(341.0, 715.0)),
            GritDomain.modeSwitchPumpElementScale(900.0, 390.0, 460.0, 341.0, 715.0),
        )
    }

    @Test
    fun shapesMatchTheDomain() {
        assertEquals(22, GritDomain.modeSwitchHammerPaths().size)
        assertEquals("#FFC98A", GritDomain.modeSwitchHammerRune().stroke)
        assertEquals(5, GritDomain.modeSwitchGradients().size)
        val hit = GritDomain.modeSwitchHitPoint(390.0, 760.0)
        assertEquals(JsMotionPoint(195.0, 756.0), hit)
        assertEquals(ModeSwitchMotion.cracks(390.0, 760.0, Point(195.0, 756.0)).map { it.d }, GritDomain.modeSwitchCracks(390.0, 760.0, 195.0, 756.0).map { it.d })
        assertEquals(30, GritDomain.modeSwitchDust(195.0, 756.0).size)
        assertEquals(34, GritDomain.modeSwitchSparks(195.0, 756.0).size)
        assertEquals(36, GritDomain.modeSwitchBurst(195.0, 756.0).size)
        val veins = GritDomain.modeSwitchVeins(412.0, 915.0, 370.0, 96.0)
        assertEquals(ModeSwitchMotion.veins(412.0, 915.0, Point(370.0, 96.0)).map { it.d }, veins.map { it.d })
        assertTrue(GritDomain.modeSwitchVeinNodes(412.0, 915.0, 370.0, 96.0).isNotEmpty())
        assertEquals(.5, GritDomain.modeSwitchVeinProgress(150.0, 50.0, 200.0))
        assertEquals(0.0, GritDomain.modeSwitchPulseOpacity(.8, 1100.0, 1000.0))
        assertEquals("#2A2E33", GritDomain.modeSwitchColors().flashLight)
    }
}
