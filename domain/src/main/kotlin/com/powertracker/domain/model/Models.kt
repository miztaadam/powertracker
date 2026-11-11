package com.powertracker.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

enum class MainLift { SQUAT, BENCH, DEADLIFT }

enum class Category { SQUAT, BENCH, DEADLIFT, VARIATION, ACCESSORY }

enum class MissPoint { FLOOR, MID, LOCKOUT }

data class Exercise(
    val id: Long,
    val name: String,
    val category: Category,
    val parentLift: MainLift?,
    val muscles: List<String>,
    val isBuiltIn: Boolean,
    val archived: Boolean = false,
) {
    /** SQUAT/BENCH/DEADLIFT category -> that lift; VARIATION -> [parentLift]; ACCESSORY -> null. */
    val mainLift: MainLift?
        get() = when (category) {
            Category.SQUAT -> MainLift.SQUAT
            Category.BENCH -> MainLift.BENCH
            Category.DEADLIFT -> MainLift.DEADLIFT
            Category.VARIATION -> parentLift
            Category.ACCESSORY -> null
        }

    /** True when the category is SQUAT, BENCH or DEADLIFT. */
    val isMainLift: Boolean
        get() = category == Category.SQUAT || category == Category.BENCH || category == Category.DEADLIFT
}

data class LoggedSet(
    val exerciseId: Long,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val failed: Boolean,
    val missPoint: MissPoint?,
    val isTopSet: Boolean,
)

data class LoggedSession(
    val id: Long,
    val date: LocalDate,
    val bodyweightKg: Double?,
    val readiness: Int?,
    val sets: List<LoggedSet>,
)

data class PlanItem(
    val id: Long,
    val weekday: DayOfWeek,
    val exerciseId: Long,
    val sets: Int,
    val reps: Int,
    val targetRpe: Double,
    val order: Int,
)

data class Baseline(val exerciseId: Long, val e1rmKg: Double, val date: LocalDate)

data class TrainingHistory(
    val sessions: List<LoggedSession>,
    val exercises: Map<Long, Exercise>,
    val baselines: List<Baseline>,
)
