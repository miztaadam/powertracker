package com.powertracker.domain.validation

import kotlin.test.Test
import kotlin.test.assertEquals

class InputValidatorTest {
    @Test fun highReps() =
        assertEquals(listOf(InputWarning.HighReps), InputValidator.check(100.0, 13, 100.0))
    @Test fun twelveRepsIsFine() = assertEquals(emptyList(), InputValidator.check(100.0, 12, 100.0))
    @Test fun bigJump() =
        assertEquals(listOf(InputWarning.BigJump(100.0)), InputValidator.check(131.0, 5, 100.0))
    @Test fun exactlyThirtyPercentAllowed() = assertEquals(emptyList(), InputValidator.check(130.0, 5, 100.0))
    @Test fun noLastWeight() = assertEquals(emptyList(), InputValidator.check(100.0, 5, null))
}
