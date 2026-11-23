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

    @Test fun baseline_acceptsDecimalsWithCommaOrPoint() {
        assertEquals(182.5, InputValidator.baselineKg("182.5"))
        assertEquals(182.5, InputValidator.baselineKg(" 182,5 "))
        assertEquals(500.0, InputValidator.baselineKg("500"))
    }

    @Test fun baseline_rejectsNonFiniteZeroAndAboveFiveHundred() {
        for (text in listOf("Infinity", "-Infinity", "NaN", "1e400", "0", "-5", "500.5", "1e3", "", "abc")) {
            assertEquals(null, InputValidator.baselineKg(text), text)
        }
    }

    @Test
    fun addingWeightToABodyweightExercise_isNotABigJump() {
        // Pull-ups logged at bodyweight (0 kg), then +5 kg: there is no meaningful "30% jump" from zero.
        assertEquals(emptyList<InputWarning>(), InputValidator.check(5.0, 5, 0.0))
        assertEquals(emptyList<InputWarning>(), InputValidator.check(20.0, 5, 0.0))
    }
}
