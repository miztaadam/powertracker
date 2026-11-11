package com.powertracker.domain.validation

sealed interface InputWarning {
    data object HighReps : InputWarning
    data class BigJump(val fromKg: Double) : InputWarning
}

object InputValidator {
    private const val MAX_TYPICAL_REPS = 12
    private const val MAX_JUMP_FACTOR = 1.30

    fun check(weightKg: Double, reps: Int, lastWeightKg: Double?): List<InputWarning> = buildList {
        if (reps > MAX_TYPICAL_REPS) add(InputWarning.HighReps)
        if (lastWeightKg != null && weightKg > lastWeightKg * MAX_JUMP_FACTOR) {
            add(InputWarning.BigJump(lastWeightKg))
        }
    }
}
