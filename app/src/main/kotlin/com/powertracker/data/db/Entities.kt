package com.powertracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
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

/** The column default for `mode`: rows from before version 3 are on the powerlifting side ([MIGRATION_2_3]). */
private const val POWERLIFTING = "POWERLIFTING"
