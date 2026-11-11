package com.powertracker.domain.validation

sealed interface InputWarning {
    data object HighReps : InputWarning
    data class BigJump(val fromKg: Double) : InputWarning
}

object InputValidator {
    private const val MAX_TYPICAL_REPS = 12
    private const val MAX_JUMP_FACTOR = 1.30

    const val MAX_BASELINE_KG = 500.0

    /** A known-best e1RM the coach can use: finite, above 0 and at most 500 kg. */
    fun isValidBaselineKg(kg: Double): Boolean = kg.isFinite() && kg > 0.0 && kg <= MAX_BASELINE_KG

    /** Parses typed baseline text ("182.5" or "182,5"); null unless [isValidBaselineKg]. */
    fun baselineKg(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf(::isValidBaselineKg)

    fun check(weightKg: Double, reps: Int, lastWeightKg: Double?): List<InputWarning> = buildList {
        if (reps > MAX_TYPICAL_REPS) add(InputWarning.HighReps)
        if (lastWeightKg != null && weightKg > lastWeightKg * MAX_JUMP_FACTOR) {
            add(InputWarning.BigJump(lastWeightKg))
        }
    }
}
