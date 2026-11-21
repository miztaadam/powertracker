package com.powertracker.domain.validation

import com.powertracker.domain.model.LoadUnit

sealed interface InputWarning {
    data object HighReps : InputWarning

    /** A jump from [fromKg], which is a plate number when [unit] is [LoadUnit.PLATES]. */
    data class BigJump(val fromKg: Double, val unit: LoadUnit = LoadUnit.KG) : InputWarning
}

object InputValidator {
    private const val MAX_TYPICAL_REPS = 12
    internal const val MAX_JUMP_FACTOR = 1.30

    const val MAX_BASELINE_KG = 500.0

    /** A known-best e1RM the coach can use: finite, above 0 and at most 500 kg. */
    fun isValidBaselineKg(kg: Double): Boolean = kg.isFinite() && kg > 0.0 && kg <= MAX_BASELINE_KG

    /** Parses typed baseline text ("182.5" or "182,5"); null unless [isValidBaselineKg]. */
    fun baselineKg(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf(::isValidBaselineKg)

    fun check(weightKg: Double, reps: Int, lastWeightKg: Double?): List<InputWarning> = buildList {
        if (reps > MAX_TYPICAL_REPS) add(InputWarning.HighReps)
        // From bodyweight (0 kg) any added load is "infinitely" more, so the jump check doesn't apply.
        if (lastWeightKg != null && lastWeightKg > 0.0 && weightKg > lastWeightKg * MAX_JUMP_FACTOR) {
            add(InputWarning.BigJump(lastWeightKg))
        }
    }

    /**
     * As the three-argument [check], in [unit]. [lastWeight] must be from the same unit, or null. In kg it answers
     * exactly as that check (and [plateStep] is ignored). In plates one step up never warns: a big jump is more than
     * one [plateStep] and more than [MAX_JUMP_FACTOR] times the last plate. A plate is never 0, so there is no
     * bodyweight case.
     */
    fun check(weight: Double, reps: Int, lastWeight: Double?, unit: LoadUnit, plateStep: Double): List<InputWarning> =
        when (unit) {
            LoadUnit.KG -> check(weight, reps, lastWeight)
            LoadUnit.PLATES -> buildList {
                if (reps > MAX_TYPICAL_REPS) add(InputWarning.HighReps)
                if (lastWeight != null && weight > lastWeight + plateStep && weight > lastWeight * MAX_JUMP_FACTOR) {
                    add(InputWarning.BigJump(lastWeight, LoadUnit.PLATES))
                }
            }
        }
}
