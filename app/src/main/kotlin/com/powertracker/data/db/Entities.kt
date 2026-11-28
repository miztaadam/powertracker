package com.powertracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.SplitSides
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

@Entity(tableName = "exercise", indices = [Index("name")])
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: Category,
    val parentLift: MainLift?,
    val muscles: List<String>,
    val isBuiltIn: Boolean,
    val archived: Boolean = false,
    /** Version 6 (1.17): whether the lifter counts plates on this machine. Rows from before version 6 are off. */
    @ColumnInfo(defaultValue = OFF) val plateCount: PlateCount = PlateCount.OFF,
    /** Version 8 (1.25): whether this exercise is logged one side at a time (Right + Left = 1 set). */
    @ColumnInfo(defaultValue = "0") val singleSided: Boolean = false,
)

@Entity(tableName = "session", indices = [Index("date")])
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val bodyweightKg: Double?,
    val readiness: Int?,
    /** Version 3: the side that was active when the session was started. */
    @ColumnInfo(defaultValue = POWERLIFTING) val mode: TrainingMode = TrainingMode.POWERLIFTING,
)

@Entity(
    tableName = "logged_set",
    foreignKeys = [
        ForeignKey(SessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val failed: Boolean,
    val missPoint: MissPoint?,
    val isTopSet: Boolean,
    /** Version 6 (1.17): what [weightKg] counts, kg or a plate number. Sets from before version 6 are kg. */
    @ColumnInfo(defaultValue = KG) val unit: LoadUnit = LoadUnit.KG,
    /** Version 8 (1.25): both halves when logged one side at a time; null for a normal set. */
    val splitSides: SplitSides? = null,
)

@Entity(
    tableName = "plan_item",
    foreignKeys = [ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("exerciseId")],
)
data class PlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekday: DayOfWeek,
    val exerciseId: Long,
    val sets: Int,
    val reps: Int,
    val targetRpe: Double,
    val sortOrder: Int,
    /** Version 3: each side keeps its own plan. */
    @ColumnInfo(defaultValue = POWERLIFTING) val mode: TrainingMode = TrainingMode.POWERLIFTING,
    /** Version 3: the top of a rep range, with [reps] its low end; null for fixed reps. */
    val repsHigh: Int? = null,
    /** Version 7 (1.24): done as a superset with the item directly above it on its day and side (`Supersets`). */
    @ColumnInfo(defaultValue = "0") val supersetWithAbove: Boolean = false,
)

@Entity(tableName = "advice")
data class AdviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: AdviceType,
    val lift: MainLift?,
    val createdAt: LocalDate,
    val reason: String,
    val severity: Int,
    /** Json.encodeToString<AdvicePayload>. */
    val payload: String,
    val status: AdviceStatus,
    /** Version 3: the side the advice is for (deload and easing-back-in count on both). */
    @ColumnInfo(defaultValue = POWERLIFTING) val mode: TrainingMode = TrainingMode.POWERLIFTING,
)

@Entity(
    tableName = "baseline",
    foreignKeys = [ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE)],
)
data class BaselineEntity(
    @PrimaryKey val exerciseId: Long,
    val e1rmKg: Double,
    val date: LocalDate,
)

/**
 * Version 4 (stage 3): one bodybuilding check-in per date. [lagging] and [growingWell] are muscle groups by their Kotlin
 * names, comma-separated ("CHEST,SIDE_DELTS"; empty for none); every other group was on track.
 */
@Entity(tableName = "check_ins", indices = [Index(value = ["date"], unique = true)])
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val lagging: String,
    val growingWell: String,
)

/**
 * Version 5 (1.13): a planned exercise not done on [date], on the [mode] side; one per exercise, date and side. Deleting
 * the exercise deletes its skips; deleting the plan item keeps them, with [planItemId] null, so the coach's history
 * stays true. [reason] is null for "Just skip".
 */
@Entity(
    tableName = "skips",
    foreignKeys = [
        ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PlanItemEntity::class, ["id"], ["planItemId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index(value = ["date", "mode", "exerciseId"], unique = true), Index("exerciseId"), Index("planItemId")],
)
data class SkipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val mode: TrainingMode,
    val exerciseId: Long,
    val planItemId: Long?,
    val reason: SkipReason?,
)

/** The column default for `mode`: rows from before version 3 are on the powerlifting side ([MIGRATION_2_3]). */
private const val POWERLIFTING = "POWERLIFTING"

/** The column defaults for `unit` and `plateCount`: rows from before version 6 are kg and off ([MIGRATION_5_6]). */
private const val KG = "KG"
private const val OFF = "OFF"
