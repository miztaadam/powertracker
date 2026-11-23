package com.powertracker.domain.validation

import com.powertracker.domain.model.LoadUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InputValidatorPlatesTest {
    private fun jumps(weight: Double, last: Double?, step: Double = 1.0) =
        InputValidator.check(weight, 10, last, LoadUnit.PLATES, step).filterIsInstance<InputWarning.BigJump>()

    @Test
    fun oneStepUp_neverWarns() {
        assertTrue(jumps(4.0, 3.0).isEmpty())
        assertTrue(jumps(1.5, 1.0, 0.5).isEmpty())
    }

    @Test
    fun aBiggerJump_usesTheSameFactor() {
        assertEquals(listOf(InputWarning.BigJump(3.0, LoadUnit.PLATES)), jumps(5.0, 3.0))
        assertTrue(jumps(12.0, 10.0).isEmpty())
        assertTrue(jumps(13.0, 10.0).isEmpty())
        assertEquals(listOf(InputWarning.BigJump(10.0, LoadUnit.PLATES)), jumps(14.0, 10.0))
        assertEquals(listOf(InputWarning.BigJump(1.0, LoadUnit.PLATES)), jumps(2.0, 1.0, 0.5))
    }

    @Test
    fun noLastSetInThisUnit_neverWarns() {
        assertTrue(InputValidator.check(7.0, 10, null, LoadUnit.PLATES, 1.0).none { it is InputWarning.BigJump })
        assertTrue(InputValidator.check(60.0, 10, null, LoadUnit.KG, 1.0).none { it is InputWarning.BigJump })
    }

    @Test
    fun highReps_isUnchanged() {
        assertEquals(listOf(InputWarning.HighReps), InputValidator.check(7.0, 13, 7.0, LoadUnit.PLATES, 1.0))
    }

    @Test
    fun kg_throughTheNewForm_equalsTheOldForm() {
        for ((weight, last) in listOf(130.0 to 100.0, 131.0 to 100.0, 20.0 to 0.0)) {
            for (reps in listOf(5, 13)) {
                assertEquals(
                    InputValidator.check(weight, reps, last),
                    InputValidator.check(weight, reps, last, LoadUnit.KG, 1.0),
                    "$weight from $last, $reps reps",
                )
            }
        }
        assertEquals(listOf(InputWarning.BigJump(100.0)), InputValidator.check(131.0, 5, 100.0, LoadUnit.KG, 1.0))
    }
}
