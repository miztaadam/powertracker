package com.powertracker.data

import androidx.room.withTransaction
import com.powertracker.data.db.AdviceEntity
import com.powertracker.data.db.AppDatabase
import com.powertracker.data.db.BaselineEntity
import com.powertracker.data.db.CheckInEntity
import com.powertracker.data.db.ExerciseEntity
import com.powertracker.data.db.PlanItemEntity
import com.powertracker.data.db.SessionEntity
import com.powertracker.data.db.SetEntity
import com.powertracker.data.db.SkipEntity
import com.powertracker.data.db.StrictPressUpgrade
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.CheckIn
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.SkipReason
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.stack.PlateCheck
import com.powertracker.domain.stack.PlateStack
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
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
    // Version 3. An older file has none of these: the powerlifting side, bodybuilding days Monday, Wednesday and
    // Friday (the default), neither side ever left.
    val trainingMode: String = TrainingMode.POWERLIFTING.name,
    val bodybuildingTrainingDays: List<Int> = AppSettings().bodybuildingTrainingDays.sorted().map { it.isoDayNumber },
    /** epochDay, or null until the side is first left. */
    val powerliftingLeftOn: Long? = null,
    val bodybuildingLeftOn: Long? = null,
) {
    fun toAppSettings() = AppSettings(
        changePlates = changePlates,
        defaultRpe = defaultRpe,
        trainingDays = trainingDays.map { DayOfWeek(it) }.toSet(),
        appLock = appLock,
        restMainSec = restMainSec,
        restAccessorySec = restAccessorySec,
        restAlert = RestAlert.valueOf(restAlert),
        webRestAlert = webRestAlert,
        showStrictPress = showStrictPress,
        trainingMode = TrainingMode.valueOf(trainingMode),
        bodybuildingTrainingDays = bodybuildingTrainingDays.map { DayOfWeek(it) }.toSet(),
        powerliftingLeftOn = powerliftingLeftOn?.let(LocalDate::fromEpochDays),
        bodybuildingLeftOn = bodybuildingLeftOn?.let(LocalDate::fromEpochDays),
    )

    companion object {
        fun of(s: AppSettings) = BackupSettings(
            s.changePlates, s.defaultRpe, s.trainingDays.sorted().map { it.isoDayNumber }, s.appLock, s.restMainSec, s.restAccessorySec,
            s.restAlert.name, s.webRestAlert, s.showStrictPress,
            s.trainingMode.name, s.bodybuildingTrainingDays.sorted().map { it.isoDayNumber },
            s.powerliftingLeftOn?.toEpochDays(), s.bodybuildingLeftOn?.toEpochDays(),
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExerciseDto(
    val id: Long, val name: String, val category: String, val parentLift: String?,
    val muscles: List<String>, val isBuiltIn: Boolean, val archived: Boolean,
    /** Version 6: WHOLE or HALF. Left out when off, so a row without plates is written as version 5 wrote it. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val plateCount: String = PlateCount.OFF.name,
) {
    fun toEntity() = ExerciseEntity(
        id, name, Category.valueOf(category), parentLift?.let(MainLift::valueOf), muscles, isBuiltIn, archived, PlateCount.valueOf(plateCount),
    )
}

/** `mode` (version 3) is absent from older files, which means POWERLIFTING, as on plan items and advice. */
@Serializable
data class SessionDto(
    val id: Long, val date: Long, val bodyweightKg: Double?, val readiness: Int?,
    val mode: String = TrainingMode.POWERLIFTING.name,
) {
    fun toEntity() = SessionEntity(id, LocalDate.fromEpochDays(date), bodyweightKg, readiness, TrainingMode.valueOf(mode))
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SetDto(
    val id: Long, val sessionId: Long, val exerciseId: Long, val weightKg: Double, val reps: Int,
    val rpe: Double?, val failed: Boolean, val missPoint: String?, val isTopSet: Boolean,
    /** Version 6: PLATES for a plate set, whose [weightKg] is the plate number. Left out when kg. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val unit: String = LoadUnit.KG.name,
) {
    fun toEntity() = SetEntity(id, sessionId, exerciseId, weightKg, reps, rpe, failed, missPoint?.let(MissPoint::valueOf), isTopSet, LoadUnit.valueOf(unit))
}

@Serializable
data class PlanItemDto(
    val id: Long, val weekday: Int, val exerciseId: Long, val sets: Int, val reps: Int,
    val targetRpe: Double, val sortOrder: Int,
    val mode: String = TrainingMode.POWERLIFTING.name,
    /** Version 3: the top of a rep range; null for fixed reps. */
    val repsHigh: Int? = null,
) {
    fun toEntity() = PlanItemEntity(id, DayOfWeek(weekday), exerciseId, sets, reps, targetRpe, sortOrder, TrainingMode.valueOf(mode), repsHigh)
}

@Serializable
data class AdviceDto(
    val id: Long, val type: String, val lift: String?, val createdAt: Long, val reason: String,
    val severity: Int, val payload: String, val status: String,
    val mode: String = TrainingMode.POWERLIFTING.name,
) {
    fun toEntity() = AdviceEntity(
        id, AdviceType.valueOf(type), lift?.let(MainLift::valueOf), LocalDate.fromEpochDays(createdAt),
        reason, severity, payload, AdviceStatus.valueOf(status), TrainingMode.valueOf(mode),
    )
}

@Serializable
data class BaselineDto(val exerciseId: Long, val e1rmKg: Double, val date: Long) {
    fun toEntity() = BaselineEntity(exerciseId, e1rmKg, LocalDate.fromEpochDays(date))
}

/**
 * Version 4 (stage 3): a bodybuilding check-in. [date] is an epochDay; [lagging] and [growingWell] are muscle groups by
 * their Kotlin names, in [MuscleGroup] order.
 */
@Serializable
data class CheckInDto(val id: Long, val date: Long, val lagging: List<String>, val growingWell: List<String>) {
    /** Throws [IllegalArgumentException] for an unknown group or one marked both ways ([CheckIn]'s rule). */
    fun toEntity(): CheckInEntity {
        val checkIn = CheckIn(LocalDate.fromEpochDays(date), lagging.mapTo(LinkedHashSet(), MuscleGroup::valueOf), growingWell.mapTo(LinkedHashSet(), MuscleGroup::valueOf), id)
        return CheckInEntity(id, checkIn.date, groupsToDb(checkIn.lagging), groupsToDb(checkIn.growingWell))
    }

    companion object {
        fun of(e: CheckInEntity) = CheckInDto(e.id, e.date.toEpochDays(), groupsFromDb(e.lagging).map { it.name }, groupsFromDb(e.growingWell).map { it.name })
    }
}

/**
 * Version 5 (1.13): a skipped planned exercise. [date] is an epochDay; [mode], [reason] are names ([reason] null for
 * "Just skip"); [planItemId] is null once that plan item was deleted.
 */
@Serializable
data class SkipDto(val id: Long, val date: Long, val mode: String, val exerciseId: Long, val planItemId: Long?, val reason: String?) {
    fun toEntity() = SkipEntity(id, LocalDate.fromEpochDays(date), TrainingMode.valueOf(mode), exerciseId, planItemId, reason?.let(SkipReason::valueOf))

    companion object {
        fun of(e: SkipEntity) = SkipDto(e.id, e.date.toEpochDays(), e.mode.name, e.exerciseId, e.planItemId, e.reason?.name)
    }
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
    /** Version 4. An older file has none, which means no check-ins. */
    val checkIns: List<CheckInDto> = emptyList(),
    /** Version 5. An older file has none, which means no skips. */
    val skips: List<SkipDto> = emptyList(),
)

sealed interface BackupResult {
    data class Ok(val file: BackupFile) : BackupResult
    data class TooNew(val version: Int) : BackupResult
    data class Corrupt(val message: String) : BackupResult
}

object Backup {
    /**
     * 2: the strict press release (category STRICT_PRESS, `settings.showStrictPress`).
     * 3: bodybuilding mode (the training side: `mode` on sessions, plan items and advice, `repsHigh` on plan items, and
     * four settings). Version 1 and 2 files still restore, everything in them on the powerlifting side. An app that reads
     * only version 2 turns a version 3 file away as too new, so both apps must be updated together.
     * 4: stage 3's bodybuilding check-ins (`checkIns`) and the MORE_WORK and SWAP advice. A version 3 file restores
     * with no check-ins.
     * 5: 1.13's skipped planned exercises (`skips`) and the SKIP advice. A version 4 file restores with no skips; an app
     * that reads only version 4 turns a version 5 file away as too new.
     * 6: 1.17's plates on stack machines (`unit` on sets, PLATES only; `plateCount` on exercises, WHOLE or HALF only;
     * TARGET payloads may carry `"unit":"PLATES"`). Each key is written only when it is not the default, so a log with no
     * plates exports as version 5 did but for the version. A version 5 or older file restores with every set in kg and
     * every exercise off; an app that reads only version 5 turns a version 6 file away as too new. A plate set that is
     * not a whole or half plate from plate 1 to 50 is a corrupt file (1.17.1; 1.17 refused only one below plate 1).
     */
    const val VERSION = 6

    private val strict = Json { ignoreUnknownKeys = false }
    private val writer = Json { encodeDefaults = true }

    /** The setting names this app reads; any other key inside `settings` is dropped before the strict decode. */
    private val knownSettings: Set<String> = (0 until BackupSettings.serializer().descriptor.elementsCount)
        .map(BackupSettings.serializer().descriptor::getElementName).toSet()

    /**
     * Since 1.7 a key inside `settings` that this app does not know is ignored, so a later app can add a setting without
     * a version bump and without this app turning its backups away (1.6 rejected them). Only `settings` is lenient:
     * an unknown key anywhere else is still corrupt, and a known setting is still checked as strictly as before.
     */
    private fun withoutUnknownSettings(root: JsonObject): JsonObject {
        val settings = root["settings"] as? JsonObject ?: return root
        if (settings.keys.all { it in knownSettings }) return root
        return JsonObject(root + ("settings" to JsonObject(settings.filterKeys { it in knownSettings })))
    }

    /**
     * Why plate number [plate] cannot be stored, or null when it can: a whole or half plate from plate 1 to
     * [PlateStack.MAX_PLATE]. The grid is judged first, so 0.3 is "not a whole or half plate"; 0 and 0.5 are on it
     * and so read "below plate 1".
     */
    private fun plateFault(plate: Double): String? = when (PlateStack.check(plate, PlateCount.HALF)) {
        PlateCheck.OK -> null
        PlateCheck.STEP -> "A plate set that is not a whole or half plate"
        PlateCheck.RANGE -> if (plate < PlateStack.MIN_PLATE) "A plate set below plate 1" else "A plate set above plate 50"
    }

    fun encode(file: BackupFile): String = writer.encodeToString(BackupFile.serializer(), file)

    /** [supportedVersion] is the newest version this app reads; tests lower it to stand in for an older app. */
    fun decode(text: String, supportedVersion: Int = VERSION): BackupResult {
        try {
            val root = strict.parseToJsonElement(text) as? JsonObject
                ?: return BackupResult.Corrupt("Not a backup file")
            val v = (root["version"] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
            if (v == null || v <= 0) return BackupResult.Corrupt("Missing or invalid version")
            if (v > supportedVersion) return BackupResult.TooNew(v)
            val file = strict.decodeFromJsonElement(BackupFile.serializer(), withoutUnknownSettings(root))
            // Reject bad enum names / out-of-range weekdays here rather than at restore time.
            file.exercises.forEach { it.toEntity() }
            file.sessions.forEach { it.toEntity() }
            file.sets.forEach { it.toEntity() }
            // A plate number is a whole or half plate from plate 1 to 50, as the app stores them. It is judged on the
            // half-plate grid, never by the exercise's setting now: a logged 7½ stands with whole plates on, or in kg.
            // Under plate 1 (0 included, which means bodyweight in kg), over 50 or off the grid, the file is damaged.
            // The first such set in the file names the fault.
            file.sets.firstNotNullOfOrNull { s -> if (s.unit == LoadUnit.PLATES.name) plateFault(s.weightKg) else null }
                ?.let { return BackupResult.Corrupt(it) }
            file.planItems.forEach { it.toEntity() }
            file.advice.forEach { it.toEntity() }
            file.checkIns.forEach { it.toEntity() }
            // One check-in per date, as the database keeps them.
            if (file.checkIns.map { it.date }.toSet().size != file.checkIns.size) return BackupResult.Corrupt("Two check-ins on one date")
            file.skips.forEach { it.toEntity() }
            // One skip per exercise, date and side, as the database keeps them.
            if (file.skips.map { Triple(it.date, it.mode, it.exerciseId) }.toSet().size != file.skips.size) {
                return BackupResult.Corrupt("Two skips of one exercise on one date and side")
            }
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
                    ExerciseDto(it.id, it.name, it.category.name, it.parentLift?.name, it.muscles, it.isBuiltIn, it.archived, it.plateCount.name)
                },
                sessions = db.sessions().all().map { SessionDto(it.id, it.date.toEpochDays(), it.bodyweightKg, it.readiness, it.mode.name) },
                sets = db.sets().all().map {
                    SetDto(it.id, it.sessionId, it.exerciseId, it.weightKg, it.reps, it.rpe, it.failed, it.missPoint?.name, it.isTopSet, it.unit.name)
                },
                planItems = db.plan().all().map {
                    PlanItemDto(it.id, it.weekday.isoDayNumber, it.exerciseId, it.sets, it.reps, it.targetRpe, it.sortOrder, it.mode.name, it.repsHigh)
                },
                advice = db.advice().all().map {
                    AdviceDto(
                        it.id, it.type.name, it.lift?.name, it.createdAt.toEpochDays(), it.reason, it.severity, it.payload, it.status.name,
                        it.mode.name,
                    )
                },
                baselines = db.baselines().all().map { BaselineDto(it.exerciseId, it.e1rmKg, it.date.toEpochDays()) },
                checkIns = db.checkIns().all().map(CheckInDto::of),
                skips = db.skips().all().map(SkipDto::of),
            )
        }
    }

    /** The sides whose plan restoring [file] would remove ([sidesLosingPlanOf]), read from what is stored now. */
    suspend fun sidesLosingPlan(file: BackupFile): List<TrainingMode> = sidesLosingPlanOf(db.plan().all().map { it.mode }, file)

    /**
     * Replaces all data in one transaction; any failure rolls back and leaves existing data untouched. A version-1 file
     * gets the strict press upgrade on the way in, as the database got [com.powertracker.data.db.MIGRATION_1_2]. A
     * version 1 or 2 file has no sides: every row in it takes the powerlifting default, as the database got
     * [com.powertracker.data.db.MIGRATION_2_3]. A file before version 4 has no check-ins, and one before version 5 no
     * skips, so none are kept.
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
        val checkIns = file.checkIns.map { it.toEntity() }
        val skips = file.skips.map { it.toEntity() }
        val newSettings = file.settings.toAppSettings()
        db.withTransaction {
            db.skips().clear()
            db.sets().clear()
            db.checkIns().clear()
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
            db.checkIns().insertAll(checkIns)
            db.skips().insertAll(skips)
        }
        settings.update { newSettings }
    }
}

/**
 * The sides whose plan restoring [file] would remove (1.14.1): those with plan items now ([current], each stored item's
 * side) and none in the file, powerlifting first. A file's item without a side is a powerlifting item, as on the way in.
 */
fun sidesLosingPlanOf(current: Collection<TrainingMode>, file: BackupFile): List<TrainingMode> {
    val inFile = file.planItems.mapTo(HashSet()) { TrainingMode.valueOf(it.mode) }
    return TrainingMode.entries.filter { it in current && it !in inFile }
}
