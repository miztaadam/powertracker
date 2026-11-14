package com.powertracker.domain.model

import com.powertracker.domain.library.MuscleGroup
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** New values go last: the names are stored in databases and backups. */
enum class AdviceType { TARGET, STALL, DELOAD, WEAK_POINT, RE_ENTRY, MORE_WORK, SWAP }

enum class AdviceStatus { OPEN, APPLIED, DISMISSED }

@Serializable
data class AccessoryPrescription(val exerciseName: String, val sets: Int, val repsLow: Int, val repsHigh: Int)

/** One more set for the plan item on [weekday] holding [exerciseId] ([name] when the advice was made): [fromSets] to [toSets]. */
@Serializable
data class SetAddition(val weekday: DayOfWeek, val exerciseId: Long, val name: String, val fromSets: Int, val toSets: Int)

/** A catalogue exercise ([name]) to add at the end of [weekday], [sets] x [repsLow]-[repsHigh] at [rpe]. */
@Serializable
data class NewExercise(val weekday: DayOfWeek, val name: String, val sets: Int, val repsLow: Int, val repsHigh: Int, val rpe: Double)

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

    /**
     * More work for a lagging [group] (bodybuilding side): either [addSets] (one or two) or [newExercise], never both.
     * [weeklyBefore] and [weeklyAfter] are the group's weekly counted sets in the plan before and after applying it.
     */
    @Serializable
    data class MoreWork(
        val group: MuscleGroup,
        val addSets: List<SetAddition>,
        val newExercise: NewExercise?,
        val weeklyBefore: Int,
        val weeklyAfter: Int,
    ) : AdvicePayload

    /**
     * A stuck exercise ([exerciseId], [name]; its main [group]) that has not beaten its session of [since], and up to
     * three catalogue exercises to swap it for, best first.
     */
    @Serializable
    data class Swap(val exerciseId: Long, val name: String, val group: MuscleGroup, val since: LocalDate, val alternatives: List<String>) : AdvicePayload
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
