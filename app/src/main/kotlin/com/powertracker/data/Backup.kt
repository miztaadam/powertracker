package com.powertracker.data

import androidx.room.withTransaction
import com.powertracker.data.db.AdviceEntity
import com.powertracker.data.db.AppDatabase
import com.powertracker.data.db.BaselineEntity
import com.powertracker.data.db.ExerciseEntity
import com.powertracker.data.db.PlanItemEntity
import com.powertracker.data.db.SessionEntity
import com.powertracker.data.db.SetEntity
import com.powertracker.data.db.StrictPressUpgrade
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

@Serializable
data class BackupSettings(
    val changePlates: Boolean,
    val defaultRpe: Double,
    val trainingDays: List<Int>,
    val appLock: Boolean,
    val restMainSec: Int,
    val restAccessorySec: Int,
    /** Added after the first backups were written: a file without it means the default, vibrate. */
    val restAlert: String = RestAlert.VIBRATE.name,
    /** Written by the web app; an opaque string Android stores and returns untouched. Absent means unset. */
    val webRestAlert: String? = null,
    /** Version 2. A version-1 file has none, which means off. */
    val showStrictPress: Boolean = false,
) {
    fun toAppSettings() = AppSettings(
        changePlates, defaultRpe, trainingDays.map { DayOfWeek(it) }.toSet(), appLock, restMainSec, restAccessorySec,
        RestAlert.valueOf(restAlert), webRestAlert, showStrictPress,
    )

    companion object {
        fun of(s: AppSettings) = BackupSettings(
            s.changePlates, s.defaultRpe, s.trainingDays.sorted().map { it.isoDayNumber }, s.appLock, s.restMainSec, s.restAccessorySec,
            s.restAlert.name, s.webRestAlert, s.showStrictPress,
        )
    }
}

@Serializable
data class ExerciseDto(
    val id: Long, val name: String, val category: String, val parentLift: String?,
    val muscles: List<String>, val isBuiltIn: Boolean, val archived: Boolean,
) {
    fun toEntity() = ExerciseEntity(id, name, Category.valueOf(category), parentLift?.let(MainLift::valueOf), muscles, isBuiltIn, archived)
}

@Serializable
data class SessionDto(val id: Long, val date: Long, val bodyweightKg: Double?, val readiness: Int?) {
    fun toEntity() = SessionEntity(id, LocalDate.fromEpochDays(date), bodyweightKg, readiness)
}

@Serializable
data class SetDto(
    val id: Long, val sessionId: Long, val exerciseId: Long, val weightKg: Double, val reps: Int,
    val rpe: Double?, val failed: Boolean, val missPoint: String?, val isTopSet: Boolean,
) {
    fun toEntity() = SetEntity(id, sessionId, exerciseId, weightKg, reps, rpe, failed, missPoint?.let(MissPoint::valueOf), isTopSet)
}

@Serializable
data class PlanItemDto(
    val id: Long, val weekday: Int, val exerciseId: Long, val sets: Int, val reps: Int,
    val targetRpe: Double, val sortOrder: Int,
) {
    fun toEntity() = PlanItemEntity(id, DayOfWeek(weekday), exerciseId, sets, reps, targetRpe, sortOrder)
}

@Serializable
data class AdviceDto(
    val id: Long, val type: String, val lift: String?, val createdAt: Long, val reason: String,
    val severity: Int, val payload: String, val status: String,
) {
    fun toEntity() = AdviceEntity(
        id, AdviceType.valueOf(type), lift?.let(MainLift::valueOf), LocalDate.fromEpochDays(createdAt),
        reason, severity, payload, AdviceStatus.valueOf(status),
    )
}

@Serializable
data class BaselineDto(val exerciseId: Long, val e1rmKg: Double, val date: Long) {
    fun toEntity() = BaselineEntity(exerciseId, e1rmKg, LocalDate.fromEpochDays(date))
}

@Serializable
data class BackupFile(
    val version: Int,
    val exportedAt: Long,
    val settings: BackupSettings,
    val exercises: List<ExerciseDto>,
    val sessions: List<SessionDto>,
    val sets: List<SetDto>,
    val planItems: List<PlanItemDto>,
    val advice: List<AdviceDto>,
    val baselines: List<BaselineDto>,
)

sealed interface BackupResult {
    data class Ok(val file: BackupFile) : BackupResult
    data class TooNew(val version: Int) : BackupResult
    data class Corrupt(val message: String) : BackupResult
}

object Backup {
    /** 2: the strict press release (category STRICT_PRESS, `settings.showStrictPress`). Version 1 files still restore. */
    const val VERSION = 2

    private val strict = Json { ignoreUnknownKeys = false }
    private val writer = Json { encodeDefaults = true }

    fun encode(file: BackupFile): String = writer.encodeToString(BackupFile.serializer(), file)

    /** [supportedVersion] is the newest version this app reads; tests lower it to stand in for an older app. */
    fun decode(text: String, supportedVersion: Int = VERSION): BackupResult {
        try {
            val root = strict.parseToJsonElement(text) as? JsonObject
                ?: return BackupResult.Corrupt("Not a backup file")
            val v = (root["version"] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
            if (v == null || v <= 0) return BackupResult.Corrupt("Missing or invalid version")
            if (v > supportedVersion) return BackupResult.TooNew(v)
            val file = strict.decodeFromJsonElement(BackupFile.serializer(), root)
            // Reject bad enum names / out-of-range weekdays here rather than at restore time.
            file.exercises.forEach { it.toEntity() }
            file.sets.forEach { it.toEntity() }
            file.planItems.forEach { it.toEntity() }
            file.advice.forEach { it.toEntity() }
            file.settings.toAppSettings()
            return BackupResult.Ok(file)
        } catch (e: SerializationException) {
            return BackupResult.Corrupt(e.message ?: "Unreadable backup")
        } catch (e: IllegalArgumentException) {
            return BackupResult.Corrupt(e.message ?: "Unreadable backup")
        }
    }
}

class BackupService(private val db: AppDatabase, private val settings: SettingsRepository) {
    suspend fun export(nowMillis: Long): BackupFile {
        val s = settings.settings.first()
        return db.withTransaction {
            BackupFile(
                version = Backup.VERSION,
                exportedAt = nowMillis,
                settings = BackupSettings.of(s),
                exercises = db.exercises().all().map {
                    ExerciseDto(it.id, it.name, it.category.name, it.parentLift?.name, it.muscles, it.isBuiltIn, it.archived)
                },
                sessions = db.sessions().all().map { SessionDto(it.id, it.date.toEpochDays(), it.bodyweightKg, it.readiness) },
                sets = db.sets().all().map {
                    SetDto(it.id, it.sessionId, it.exerciseId, it.weightKg, it.reps, it.rpe, it.failed, it.missPoint?.name, it.isTopSet)
                },
                planItems = db.plan().all().map {
                    PlanItemDto(it.id, it.weekday.isoDayNumber, it.exerciseId, it.sets, it.reps, it.targetRpe, it.sortOrder)
                },
                advice = db.advice().all().map {
                    AdviceDto(it.id, it.type.name, it.lift?.name, it.createdAt.toEpochDays(), it.reason, it.severity, it.payload, it.status.name)
                },
                baselines = db.baselines().all().map { BaselineDto(it.exerciseId, it.e1rmKg, it.date.toEpochDays()) },
            )
        }
    }

    /**
     * Replaces all data in one transaction; any failure rolls back and leaves existing data untouched. A version-1 file
     * gets the strict press upgrade on the way in, as the database got [com.powertracker.data.db.MIGRATION_1_2].
     */
    suspend fun restore(file: BackupFile) {
        // Map everything first so any bad file content fails before a single row is deleted.
        val rows = file.exercises.map { it.toEntity() }
        val exercises = if (file.version < 2) StrictPressUpgrade.apply(rows) else rows
        val sessions = file.sessions.map { it.toEntity() }
        val sets = file.sets.map { it.toEntity() }
        val planItems = file.planItems.map { it.toEntity() }
        val advice = file.advice.map { it.toEntity() }
        val baselines = file.baselines.map { it.toEntity() }
        val newSettings = file.settings.toAppSettings()
        db.withTransaction {
            db.sets().clear()
            db.advice().clear()
            db.plan().clear()
            db.baselines().clear()
            db.sessions().clear()
            db.exercises().clear()
            db.exercises().insertAll(exercises)
            db.sessions().insertAll(sessions)
            db.sets().insertAll(sets)
            db.plan().insertAll(planItems)
            db.advice().insert(advice)
            db.baselines().insertAll(baselines)
        }
        settings.update { newSettings }
    }
}
