package com.powertracker.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
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
