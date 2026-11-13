package com.powertracker.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

enum class AdviceType { TARGET, STALL, DELOAD, WEAK_POINT, RE_ENTRY }

enum class AdviceStatus { OPEN, APPLIED, DISMISSED }

@Serializable
data class AccessoryPrescription(val exerciseName: String, val sets: Int, val repsLow: Int, val repsHigh: Int)

@Serializable
sealed interface AdvicePayload {
    /**
     * [sets] overrides the planned set count (e.g. during a deload); null means use the plan's sets. [repsHigh] is the
     * top of a rep range with [reps] its low end; null means fixed reps, and is left out of the stored JSON.
     */
    @Serializable
    data class Target(
        val exerciseId: Long,
        val weightKg: Double,
        val reps: Int,
        val rpe: Double,
        val sets: Int? = null,
        val repsHigh: Int? = null,
    ) : AdvicePayload

    @Serializable
    data class Accessories(val items: List<AccessoryPrescription>) : AdvicePayload

    @Serializable
    data class Deload(val volumeFactor: Double = 0.6, val loadFactor: Double = 0.9) : AdvicePayload

    @Serializable
    data class WeakPoint(val lift: MainLift, val missPoint: MissPoint?) : AdvicePayload

    @Serializable
    data class ReEntry(val loadFactor: Double = 0.85) : AdvicePayload
}

data class Advice(
    val type: AdviceType,
    val lift: MainLift?,
    val createdAt: LocalDate,
    val reason: String,
    val severity: Int,
    val payload: AdvicePayload,
    val status: AdviceStatus = AdviceStatus.OPEN,
    val id: Long = 0,
    /** The side the advice is for. A deload or an easing-back-in belongs to the lifter and counts on both sides. */
    val mode: TrainingMode = TrainingMode.POWERLIFTING,
)
