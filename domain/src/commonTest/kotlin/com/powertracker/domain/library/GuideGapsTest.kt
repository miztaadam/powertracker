package com.powertracker.domain.library

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The two guide gaps noted in 1.5, filled in 1.13.1, each with an allowed source. */
class GuideGapsTest {
    @Test
    fun seatedCableFly_warnsAgainstGoingPastTheStart() {
        val e = assertNotNull(Library.find("Seated cable fly"))
        assertTrue("Letting your hands go back past the level of your armpits." in e.guide.mistakes, e.guide.mistakes.toString())
        assertTrue(Sources.ACE_STANDING_INCLINE_CABLE_FLYES in e.sources)
    }

    @Test
    fun hackSquat_stopsSoonerIfTheHipsLeaveThePad() {
        val e = assertNotNull(Library.find("Hack squat"))
        assertTrue(e.guide.steps.any { "Stop sooner if your hips start to lift off the pad." in it }, e.guide.steps.toString())
        assertTrue(Sources.EXRX_SL_HACK_SQUAT in e.sources)
    }
}
