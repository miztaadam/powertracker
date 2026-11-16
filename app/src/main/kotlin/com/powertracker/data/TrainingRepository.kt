package com.powertracker.data

import androidx.room.withTransaction
import com.powertracker.data.db.AppDatabase
import com.powertracker.data.db.BaselineEntity
import com.powertracker.data.db.CheckInEntity
import com.powertracker.data.db.ExerciseEntity
import com.powertracker.data.db.PlanItemEntity
import com.powertracker.data.db.SessionEntity
import com.powertracker.data.db.SetEntity
import com.powertracker.domain.coach.PlanChange
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.CheckIn
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.flow.first

/** Each side's plan. */
data class Plans(val powerlifting: List<PlanItem>, val bodybuilding: List<PlanItem>) {
    fun of(mode: TrainingMode): List<PlanItem> = if (mode == TrainingMode.BODYBUILDING) bodybuilding else powerlifting
}

/** A logged set together with its row ids, for editing and deleting. */
data class StoredSet(val id: Long, val sessionId: Long, val set: LoggedSet)

class ExerciseInUseException(val exerciseId: Long) :
    IllegalStateException("Exercise $exerciseId has logged sets; archive it instead")

open class TrainingRepository(private val db: AppDatabase) {
    private val catalogue = CatalogueResolver(db)

    fun history(): Flow<TrainingHistory> = combine(
        db.sessions().observeAll(),
        db.sets().observeAll(),
        db.exercises().observeAll(),
        db.baselines().observeAll(),
        ::historyOf,
    )

    /** The history as it was before the set [setId] was logged: everything stored, read once, but that set. */
    open suspend fun historyWithout(setId: Long): TrainingHistory =
        historyOf(
            db.sessions().observeAll().first(),
            db.sets().observeAll().first().filter { it.id != setId },
            db.exercises().observeAll().first(),
            db.baselines().observeAll().first(),
        )

    private fun historyOf(sessions: List<SessionEntity>, sets: List<SetEntity>, exercises: List<ExerciseEntity>, baselines: List<BaselineEntity>): TrainingHistory {
        val setsBySession = sets.groupBy { it.sessionId }
        return TrainingHistory(
            sessions = sessions.map { s -> s.toDomain(setsBySession[s.id].orEmpty().map { it.toDomain() }) },
            exercises = exercises.map { it.toDomain() }.associateBy { it.id },
            baselines = baselines.map { Baseline(it.exerciseId, it.e1rmKg, it.date) },
        )
    }

    fun exercises(): Flow<List<Exercise>> = db.exercises().observeAll().map { list -> list.map { it.toDomain() } }

    /** [mode]'s plan only: each side keeps its own (bodybuilding mode spec §6). */
    fun plan(mode: TrainingMode = TrainingMode.POWERLIFTING): Flow<List<PlanItem>> =
        db.plan().observeSide(mode).map { list -> list.map { it.toDomain() } }

    /**
     * Both sides' plans, watched together: a screen that follows the side picks one from each emission, so a switch
     * never opens a new query and the side and its plan always arrive together.
     */
    fun plans(): Flow<Plans> = combine(plan(TrainingMode.POWERLIFTING), plan(TrainingMode.BODYBUILDING), ::Plans)

    fun setsForSession(sessionId: Long): Flow<List<StoredSet>> =
        db.sets().observeForSession(sessionId).map { list -> list.map { StoredSet(it.id, it.sessionId, it.toDomain()) } }

    fun sessionOn(date: LocalDate): Flow<LoggedSession?> =
        db.sessions().observeOn(date).map { it?.toDomain(emptyList()) }

    /** [mode] is the side active when the session is started. */
    suspend fun startSession(date: LocalDate, bodyweightKg: Double?, readiness: Int?, mode: TrainingMode = TrainingMode.POWERLIFTING): Long =
        db.sessions().insert(SessionEntity(date = date, bodyweightKg = bodyweightKg, readiness = readiness, mode = mode))

    /**
     * Saves [set]. With a [catalogueName] the set is for that catalogue exercise, whatever its
     * `exerciseId` says: the exercise row is found, restored or inserted in the same transaction.
     */
    suspend fun logSet(sessionId: Long, set: LoggedSet, catalogueName: String? = null): Long {
        if (catalogueName == null) return db.sets().insert(set.toEntity(0, sessionId))
        return db.withTransaction {
            db.sets().insert(set.copy(exerciseId = catalogue.resolve(catalogueName)).toEntity(0, sessionId))
        }
    }

    suspend fun updateSet(setId: Long, set: LoggedSet) {
        val existing = db.sets().byId(setId) ?: return
        db.sets().update(set.toEntity(setId, existing.sessionId))
    }

    suspend fun deleteSet(setId: Long) = db.sets().delete(setId)

    /**
     * Inserts a new item (id 0) or replaces the item with the same id. With a [catalogueName] the item
     * is for that catalogue exercise, whatever its `exerciseId` says: the exercise row is found,
     * restored or inserted in the same transaction. The item is stored on its own side ([PlanItem.mode]).
     */
    open suspend fun savePlanItem(item: PlanItem, catalogueName: String? = null): Long {
        fun entity(exerciseId: Long) =
            PlanItemEntity(item.id, item.weekday, exerciseId, item.sets, item.reps, item.targetRpe, item.order, item.mode, item.repsHigh)
        if (catalogueName == null) return db.plan().upsert(entity(item.exerciseId))
        return db.withTransaction { db.plan().upsert(entity(catalogue.resolve(catalogueName))) }
    }

    suspend fun deletePlanItem(id: Long) = db.plan().delete(id)

    /**
     * Replaces [mode]'s whole plan with [items], in one transaction: that side's items are deleted, each item's catalogue
     * name (the second of the pair; null for the lifter's own exercise, which keeps its `exerciseId`) is resolved as
     * [savePlanItem] does, and the items are inserted in the order given, each on [mode]. If anything fails, the old plan
     * and every exercise row are left as they were. The other side's plan is never touched.
     */
    suspend fun replacePlan(mode: TrainingMode, items: List<Pair<PlanItem, String?>>) = db.withTransaction {
        db.plan().clearSide(mode)
        for ((item, catalogueName) in items) {
            val exerciseId = if (catalogueName == null) item.exerciseId else catalogue.resolve(catalogueName)
            db.plan().upsert(PlanItemEntity(0, item.weekday, exerciseId, item.sets, item.reps, item.targetRpe, item.order, mode, item.repsHigh))
        }
    }

    /** Runs [block] in one transaction: the plan items it saves and the catalogue rows it resolves land together or not at all. */
    suspend fun <T> inTransaction(block: suspend () -> T): T = db.withTransaction(block)

    /** The row for the catalogue exercise [name], found, restored or inserted (see [CatalogueResolver.resolve]). Call it inside [inTransaction]. */
    suspend fun resolveCatalogue(name: String): Long = catalogue.resolve(name)

    suspend fun addExercise(e: Exercise): Long = db.exercises().insert(
        ExerciseEntity(0, e.name, e.category, e.parentLift, e.muscles, e.isBuiltIn, e.archived),
    )

    /** Throws [ExerciseInUseException] if sets reference it; use [archiveExercise] instead. */
    suspend fun deleteExercise(id: Long) = db.withTransaction {
        if (db.exercises().setCount(id) > 0) throw ExerciseInUseException(id)
        db.exercises().delete(id)
    }

    suspend fun archiveExercise(id: Long) = db.exercises().archive(id)

    /** The bodybuilding check-ins, oldest first. */
    fun checkIns(): Flow<List<CheckIn>> = db.checkIns().observeAll().map { list -> list.map { it.toDomain() } }

    /** Saves [checkIn]; one already stored on the same date is replaced. */
    suspend fun saveCheckIn(checkIn: CheckIn): Long = db.checkIns().upsert(checkIn.toEntity())

    /**
     * Carries out a coach's change to the bodybuilding plan in one transaction: [PlanChange.updated] saved,
     * [PlanChange.removed] deleted, and each of [PlanChange.added] inserted on its catalogue name's row (found, restored or
     * inserted as [savePlanItem] does). If anything fails, the plan and the exercise rows are left as they were.
     */
    suspend fun applyPlanChange(change: PlanChange) = db.withTransaction {
        for (item in change.updated) savePlanItem(item)
        for (item in change.removed) db.plan().delete(item.id)
        for ((item, name) in change.added) savePlanItem(item.copy(id = 0), name)
    }

    suspend fun setBaseline(b: Baseline) = db.baselines().upsert(BaselineEntity(b.exerciseId, b.e1rmKg, b.date))
}

private fun ExerciseEntity.toDomain() = Exercise(id, name, category, parentLift, muscles, isBuiltIn, archived)

private fun SessionEntity.toDomain(sets: List<LoggedSet>) = LoggedSession(id, date, bodyweightKg, readiness, sets, mode)

private fun SetEntity.toDomain() = LoggedSet(exerciseId, weightKg, reps, rpe, failed, missPoint, isTopSet)

private fun LoggedSet.toEntity(id: Long, sessionId: Long) =
    SetEntity(id, sessionId, exerciseId, weightKg, reps, rpe, failed, missPoint, isTopSet)

private fun PlanItemEntity.toDomain() = PlanItem(id, weekday, exerciseId, sets, reps, targetRpe, sortOrder, mode, repsHigh)

/** The stored form of a check-in's groups: Kotlin names, comma-separated, in [MuscleGroup] order. */
internal fun groupsToDb(groups: Set<MuscleGroup>): String = MuscleGroup.entries.filter { it in groups }.joinToString(",") { it.name }

internal fun groupsFromDb(text: String): Set<MuscleGroup> =
    text.split(",").filter { it.isNotEmpty() }.mapTo(LinkedHashSet()) { MuscleGroup.valueOf(it) }

private fun CheckIn.toEntity() = CheckInEntity(id, date, groupsToDb(lagging), groupsToDb(growingWell))

private fun CheckInEntity.toDomain() = CheckIn(date, groupsFromDb(lagging), groupsFromDb(growingWell), id)
