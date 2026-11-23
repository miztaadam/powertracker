package com.powertracker.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/** New values go last: the names are stored in databases and backups, and some lists keep this order. */
enum class MainLift { SQUAT, BENCH, DEADLIFT, STRICT_PRESS }

/** The main-lift categories map one-to-one to [MainLift]; new main lifts go before VARIATION. */
enum class Category { SQUAT, BENCH, DEADLIFT, STRICT_PRESS, VARIATION, ACCESSORY }

enum class MissPoint { FLOOR, MID, LOCKOUT }

/**
 * The side of the app a plan item, session or piece of advice belongs to. Off is POWERLIFTING, the app as it always
 * was. The names are stored in databases and backups.
 */
enum class TrainingMode { POWERLIFTING, BODYBUILDING }

/** What the apps call [lift]: "Squat", "Bench", "Deadlift" or "Strict press". */
fun liftLabel(lift: MainLift): String = when (lift) {
    MainLift.SQUAT -> "Squat"
    MainLift.BENCH -> "Bench"
    MainLift.DEADLIFT -> "Deadlift"
    MainLift.STRICT_PRESS -> "Strict press"
}

/** The label of a miss-point choice when logging [lift]: "Floor", "Mid", "Lockout"; strict press's floor is "Off the shoulders". */
fun missPointLabel(lift: MainLift, point: MissPoint): String = when (point) {
    MissPoint.FLOOR -> if (lift == MainLift.STRICT_PRESS) "Off the shoulders" else "Floor"
    MissPoint.MID -> "Mid"
    MissPoint.LOCKOUT -> "Lockout"
}

data class Exercise(
    val id: Long,
    val name: String,
    val category: Category,
    val parentLift: MainLift?,
    val muscles: List<String>,
    val isBuiltIn: Boolean,
    val archived: Boolean = false,
    /** Whether the lifter counts plates on this machine; it counts only where `PlateStack.offered` allows it. */
    val plateCount: PlateCount = PlateCount.OFF,
) {
    /** A main-lift category -> that lift; VARIATION -> [parentLift]; ACCESSORY -> null. */
    val mainLift: MainLift?
        get() = when (category) {
            Category.SQUAT -> MainLift.SQUAT
            Category.BENCH -> MainLift.BENCH
            Category.DEADLIFT -> MainLift.DEADLIFT
            Category.STRICT_PRESS -> MainLift.STRICT_PRESS
            Category.VARIATION -> parentLift
            Category.ACCESSORY -> null
        }

    /** True when the category is a main lift's own (SQUAT, BENCH, DEADLIFT or STRICT_PRESS). */
    val isMainLift: Boolean
        get() = when (category) {
            Category.SQUAT, Category.BENCH, Category.DEADLIFT, Category.STRICT_PRESS -> true
            Category.VARIATION, Category.ACCESSORY -> false
        }
}

data class LoggedSet(
    val exerciseId: Long,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val failed: Boolean,
    val missPoint: MissPoint?,
    val isTopSet: Boolean,
    /** What [weightKg] means: kg, or a plate number when this is [LoadUnit.PLATES]. */
    val unit: LoadUnit = LoadUnit.KG,
)

data class LoggedSession(
    val id: Long,
    val date: LocalDate,
    val bodyweightKg: Double?,
    val readiness: Int?,
    val sets: List<LoggedSet>,
    /** The side that was active when the session was started. */
    val mode: TrainingMode = TrainingMode.POWERLIFTING,
)

data class PlanItem(
    val id: Long,
    val weekday: DayOfWeek,
    val exerciseId: Long,
    val sets: Int,
    val reps: Int,
    val targetRpe: Double,
    val order: Int,
    val mode: TrainingMode = TrainingMode.POWERLIFTING,
    /** The top of a rep range, with [reps] its low end; null means fixed reps (every powerlifting item). */
    val repsHigh: Int? = null,
)

data class Baseline(val exerciseId: Long, val e1rmKg: Double, val date: LocalDate)

data class TrainingHistory(
    val sessions: List<LoggedSession>,
    val exercises: Map<Long, Exercise>,
    val baselines: List<Baseline>,
)

/** Why a planned exercise was skipped; a skip with no reason ("Just skip") stores null. New values go last: the names are stored. */
enum class SkipReason { BUSY, PAIN, TIME }

/**
 * A planned exercise the lifter did not do on [date], on the [mode] side. One per exercise, date and side. It is
 * never a set: nothing that counts sets reads it. [planItemId] is null once that plan item is deleted.
 */
data class Skip(
    val id: Long,
    val date: LocalDate,
    val mode: TrainingMode,
    val exerciseId: Long,
    val planItemId: Long?,
    val reason: SkipReason?,
)
