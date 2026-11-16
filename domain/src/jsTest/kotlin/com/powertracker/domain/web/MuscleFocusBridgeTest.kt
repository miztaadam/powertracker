package com.powertracker.domain.web

import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleFocus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MuscleFocusBridgeTest {
    @Test
    fun muscleFocus_lines_setThePairOnly() {
        val squat = assertNotNull(Library.focus("Squat") as? MuscleFocus.Lines)
        val js = assertNotNull(GritDomain.muscleFocus("squat"))
        assertEquals(squat.stretch, js.stretch)
        assertEquals(squat.squeeze, js.squeeze)
        assertNull(js.none)
    }

    @Test
    fun muscleFocus_noFocus_setsTheFixedLineOnly() {
        val js = assertNotNull(GritDomain.muscleFocus("Pin squat"))
        assertEquals(Library.NO_FOCUS_LINE, js.none)
        assertNull(js.stretch)
        assertNull(js.squeeze)
    }

    @Test
    fun muscleFocus_nullForAnOwnExercise() {
        assertNull(GritDomain.muscleFocus("My own lift"))
    }
}
