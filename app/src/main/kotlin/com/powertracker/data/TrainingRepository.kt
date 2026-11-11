package com.powertracker.data

import androidx.room.withTransaction
import com.powertracker.data.db.AppDatabase
import com.powertracker.data.db.BaselineEntity
import com.powertracker.data.db.ExerciseEntity
import com.powertracker.data.db.PlanItemEntity
import com.powertracker.data.db.SessionEntity
import com.powertracker.data.db.SetEntity
import com.powertracker.domain.model.Baseline
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/** A logged set together with its row ids, for editing and deleting. */
data class StoredSet(val id: Long, val sessionId: Long, val set: LoggedSet)

class ExerciseInUseException(val exerciseId: Long) :
    IllegalStateException("Exercise $exerciseId has logged sets; archive it instead")

class TrainingRepository(private val db: AppDatabase) {
    private val catalogue = CatalogueResolver(db)

    fun history(): Flow<TrainingHistory> = combine(
        db.sessions().observeAll(),
        db.sets().observeAll(),
        db.exercises().observeAll(),
        db.baselines().observeAll(),
    ) { sessions, sets, exercises, baselines ->
        val setsBySession = sets.groupBy { it.sessionId }
        TrainingHistory(
            sessions = sessions.map { s -> s.toDomain(setsBySession[s.id].orEmpty().map { it.toDomain() }) },
            exercises = exercises.map { it.toDomain() }.associateBy { it.id },
            baselines = baselines.map { Baseline(it.exerciseId, it.e1rmKg, it.date) },
        )
    }

    fun exercises(): Flow<List<Exercise>> = db.exercises().observeAll().map { list -> list.map { it.toDomain() } }

    fun plan(): Flow<List<PlanItem>> = db.plan().observeAll().map { list -> list.map { it.toDomain() } }

    fun setsForSession(sessionId: Long): Flow<List<StoredSet>> =
        db.sets().observeForSession(sessionId).map { list -> list.map { StoredSet(it.id, it.sessionId, it.toDomain()) } }

    fun sessionOn(date: LocalDate): Flow<LoggedSession?> =
        db.sessions().observeOn(date).map { it?.toDomain(emptyList()) }

    suspend fun startSession(date: LocalDate, bodyweightKg: Double?, readiness: Int?): Long =
        db.sessions().insert(SessionEntity(date = date, bodyweightKg = bodyweightKg, readiness = readiness))

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
     * restored or inserted in the same transaction.
     */
    suspend fun savePlanItem(item: PlanItem, catalogueName: String? = null): Long {
        fun entity(exerciseId: Long) = PlanItemEntity(item.id, item.weekday, exerciseId, item.sets, item.reps, item.targetRpe, item.order)
        if (catalogueName == null) return db.plan().upsert(entity(item.exerciseId))
        return db.withTransaction { db.plan().upsert(entity(catalogue.resolve(catalogueName))) }
    }

    suspend fun deletePlanItem(id: Long) = db.plan().delete(id)

    suspend fun addExercise(e: Exercise): Long = db.exercises().insert(
        ExerciseEntity(0, e.name, e.category, e.parentLift, e.muscles, e.isBuiltIn, e.archived),
    )

    /** Throws [ExerciseInUseException] if sets reference it; use [archiveExercise] instead. */
    suspend fun deleteExercise(id: Long) = db.withTransaction {
        if (db.exercises().setCount(id) > 0) throw ExerciseInUseException(id)
        db.exercises().delete(id)
    }

    suspend fun archiveExercise(id: Long) = db.exercises().archive(id)

    suspend fun setBaseline(b: Baseline) = db.baselines().upsert(BaselineEntity(b.exerciseId, b.e1rmKg, b.date))
}

private fun ExerciseEntity.toDomain() = Exercise(id, name, category, parentLift, muscles, isBuiltIn, archived)

private fun SessionEntity.toDomain(sets: List<LoggedSet>) = LoggedSession(id, date, bodyweightKg, readiness, sets)

private fun SetEntity.toDomain() = LoggedSet(exerciseId, weightKg, reps, rpe, failed, missPoint, isTopSet)

private fun LoggedSet.toEntity(id: Long, sessionId: Long) =
    SetEntity(id, sessionId, exerciseId, weightKg, reps, rpe, failed, missPoint, isTopSet)

private fun PlanItemEntity.toDomain() = PlanItem(id, weekday, exerciseId, sets, reps, targetRpe, sortOrder)
