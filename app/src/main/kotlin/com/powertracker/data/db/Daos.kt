package com.powertracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.powertracker.domain.model.AdviceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise ORDER BY id") fun observeAll(): Flow<List<ExerciseEntity>>
    @Insert suspend fun insert(e: ExerciseEntity): Long
    @Query("UPDATE exercise SET archived = 1 WHERE id = :id") suspend fun archive(id: Long)
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
    @Query("SELECT * FROM plan_item ORDER BY weekday, sortOrder, id") fun observeAll(): Flow<List<PlanItemEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(p: PlanItemEntity): Long
    @Query("DELETE FROM plan_item WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM plan_item ORDER BY id") suspend fun all(): List<PlanItemEntity>
    @Insert suspend fun insertAll(p: List<PlanItemEntity>)
    @Query("DELETE FROM plan_item") suspend fun clear()
}

@Dao
interface AdviceDao {
    @Query("SELECT * FROM advice WHERE status = 'OPEN' ORDER BY severity DESC, id")
    fun observeOpen(): Flow<List<AdviceEntity>>
    @Query("SELECT * FROM advice ORDER BY id") suspend fun all(): List<AdviceEntity>
    @Insert suspend fun insert(a: List<AdviceEntity>)
    @Query("DELETE FROM advice") suspend fun clear()
    @Query("UPDATE advice SET status = :status WHERE id = :id") suspend fun setStatus(id: Long, status: AdviceStatus)
}

@Dao
interface BaselineDao {
    @Query("SELECT * FROM baseline ORDER BY exerciseId") fun observeAll(): Flow<List<BaselineEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(b: BaselineEntity)
    @Query("SELECT * FROM baseline ORDER BY exerciseId") suspend fun all(): List<BaselineEntity>
    @Insert suspend fun insertAll(b: List<BaselineEntity>)
    @Query("DELETE FROM baseline") suspend fun clear()
}
