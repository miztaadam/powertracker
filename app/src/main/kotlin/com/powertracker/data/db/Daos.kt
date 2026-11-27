package com.powertracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.TrainingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise ORDER BY id") fun observeAll(): Flow<List<ExerciseEntity>>
    @Insert suspend fun insert(e: ExerciseEntity): Long
    @Query("UPDATE exercise SET archived = 1 WHERE id = :id") suspend fun archive(id: Long)
    @Query("UPDATE exercise SET archived = 0 WHERE id = :id") suspend fun restore(id: Long)
    /** Version 6 (1.17): the exercise's plate setting; no set and no advice changes with it. */
    @Query("UPDATE exercise SET plateCount = :value WHERE id = :id") suspend fun setPlateCount(id: Long, value: PlateCount)
    @Query("DELETE FROM exercise WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT COUNT(*) FROM logged_set WHERE exerciseId = :id") suspend fun setCount(id: Long): Int
    @Query("SELECT * FROM exercise ORDER BY id") suspend fun all(): List<ExerciseEntity>
    @Insert suspend fun insertAll(e: List<ExerciseEntity>)
    @Query("DELETE FROM exercise") suspend fun clear()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM session ORDER BY date, id") fun observeAll(): Flow<List<SessionEntity>>
    @Query("SELECT * FROM session WHERE date = :date ORDER BY id LIMIT 1")
    fun observeOn(date: LocalDate): Flow<SessionEntity?>
    @Insert suspend fun insert(s: SessionEntity): Long
    @Query("SELECT * FROM session ORDER BY id") suspend fun all(): List<SessionEntity>
    @Insert suspend fun insertAll(s: List<SessionEntity>)
    @Query("DELETE FROM session") suspend fun clear()

    /** Whether any session was ever started (the guide's first-launch check). */
    @Query("SELECT EXISTS(SELECT 1 FROM session)") suspend fun any(): Boolean
}

@Dao
interface SetDao {
    @Query("SELECT * FROM logged_set ORDER BY id") fun observeAll(): Flow<List<SetEntity>>
    @Query("SELECT * FROM logged_set WHERE sessionId = :sessionId ORDER BY id")
    fun observeForSession(sessionId: Long): Flow<List<SetEntity>>
    @Insert suspend fun insert(s: SetEntity): Long
    @Update suspend fun update(s: SetEntity)
    @Query("DELETE FROM logged_set WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM logged_set WHERE id = :id") suspend fun byId(id: Long): SetEntity?
    @Query("SELECT * FROM logged_set ORDER BY id") suspend fun all(): List<SetEntity>
    @Insert suspend fun insertAll(s: List<SetEntity>)
    @Query("DELETE FROM logged_set") suspend fun clear()
}

@Dao
interface PlanDao {
    /** One side's plan: the other side's rows are never read here. */
    @Query("SELECT * FROM plan_item WHERE mode = :mode ORDER BY weekday, sortOrder, id")
    fun observeSide(mode: TrainingMode): Flow<List<PlanItemEntity>>
    /**
     * Inserts a new item (id 0, or an id not stored yet) or updates the stored one in place, and returns its id. Not
     * REPLACE: that deletes the row first, which would set its skips' `planItemId` to null on every edit (version 5).
     */
    @Transaction
    suspend fun upsert(p: PlanItemEntity): Long {
        val id = insertOrIgnore(p)
        if (id != -1L) return id
        update(p)
        return p.id
    }
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertOrIgnore(p: PlanItemEntity): Long
    @Update suspend fun update(p: PlanItemEntity)
    @Query("DELETE FROM plan_item WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM plan_item WHERE id = :id") suspend fun byId(id: Long): PlanItemEntity?
    /** Links the item to the one above it as a superset, or takes the link off (1.24). */
    @Query("UPDATE plan_item SET supersetWithAbove = :linked WHERE id IN (:ids)") suspend fun setSuperset(ids: List<Long>, linked: Boolean)
    @Query("SELECT * FROM plan_item ORDER BY id") suspend fun all(): List<PlanItemEntity>
    @Insert suspend fun insertAll(p: List<PlanItemEntity>)
    @Query("DELETE FROM plan_item") suspend fun clear()
    /** Deletes [mode]'s plan; the other side's rows are kept. */
    @Query("DELETE FROM plan_item WHERE mode = :mode") suspend fun clearSide(mode: TrainingMode)

    /** Whether either side has a plan item (the guide's first-launch check). */
    @Query("SELECT EXISTS(SELECT 1 FROM plan_item)") suspend fun any(): Boolean
}

@Dao
interface AdviceDao {
    @Query("SELECT * FROM advice WHERE status = 'OPEN' ORDER BY severity DESC, id")
    fun observeOpen(): Flow<List<AdviceEntity>>
    @Query("SELECT * FROM advice ORDER BY id") suspend fun all(): List<AdviceEntity>
    @Insert suspend fun insert(a: List<AdviceEntity>)
    @Query("DELETE FROM advice") suspend fun clear()
    @Query("UPDATE advice SET status = :status WHERE id = :id") suspend fun setStatus(id: Long, status: AdviceStatus)
    /** [type]'s OPEN advice, both sides', made on [createdAt]; the payloads are matched in Kotlin, never with LIKE. */
    @Query("SELECT * FROM advice WHERE type = :type AND status = 'OPEN' AND createdAt = :createdAt ORDER BY id")
    suspend fun openOf(type: AdviceType, createdAt: LocalDate): List<AdviceEntity>
    @Query("SELECT * FROM advice WHERE type = :type AND status = 'OPEN' ORDER BY id")
    suspend fun openOf(type: AdviceType): List<AdviceEntity>
    @Query("DELETE FROM advice WHERE id IN (:ids)") suspend fun delete(ids: List<Long>)
}

@Dao
interface SkipDao {
    @Query("SELECT * FROM skips ORDER BY date, id") fun observeAll(): Flow<List<SkipEntity>>
    @Query("SELECT * FROM skips WHERE date = :date AND mode = :mode AND exerciseId = :exerciseId")
    suspend fun find(date: LocalDate, mode: TrainingMode, exerciseId: Long): SkipEntity?
    @Query("SELECT * FROM skips WHERE id = :id") suspend fun byId(id: Long): SkipEntity?
    @Insert suspend fun insert(s: SkipEntity): Long
    @Update suspend fun update(s: SkipEntity)
    @Query("DELETE FROM skips WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM skips ORDER BY id") suspend fun all(): List<SkipEntity>
    @Insert suspend fun insertAll(s: List<SkipEntity>)
    @Query("DELETE FROM skips") suspend fun clear()
}

@Dao
interface BaselineDao {
    @Query("SELECT * FROM baseline ORDER BY exerciseId") fun observeAll(): Flow<List<BaselineEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(b: BaselineEntity)
    @Query("SELECT * FROM baseline ORDER BY exerciseId") suspend fun all(): List<BaselineEntity>
    @Insert suspend fun insertAll(b: List<BaselineEntity>)
    @Query("DELETE FROM baseline") suspend fun clear()
}

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins ORDER BY date") fun observeAll(): Flow<List<CheckInEntity>>
    /** A check-in on a date that already has one replaces it. */
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(c: CheckInEntity): Long
    @Query("SELECT * FROM check_ins ORDER BY date") suspend fun all(): List<CheckInEntity>
    @Insert suspend fun insertAll(c: List<CheckInEntity>)
    @Query("DELETE FROM check_ins") suspend fun clear()
}
