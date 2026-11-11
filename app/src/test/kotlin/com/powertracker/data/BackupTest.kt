package com.powertracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.db.AppDatabase
import com.powertracker.data.db.SessionEntity
import com.powertracker.data.db.SetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class BackupTest {
    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var service: BackupService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
        settings = SettingsRepository(context)
        service = BackupService(db, settings)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedData() {
        settings.update { AppSettings(changePlates = true, defaultRpe = 7.5, trainingDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY)) }
        val ex = db.exercises().all().first().id
        val s = db.sessions().insert(SessionEntity(date = LocalDate.of(2026, 5, 4), bodyweightKg = 90.5, readiness = 4))
        db.sets().insert(SetEntity(sessionId = s, exerciseId = ex, weightKg = 100.0, reps = 5, rpe = 8.0, failed = false, missPoint = null, isTopSet = true))
    }

    @Test
    fun encodeDecode_roundTrips() = runTest {
        seedData()
        val file = service.export(1234L)
        assertTrue(file.sets.isNotEmpty())
        assertEquals(BackupResult.Ok(file), Backup.decode(Backup.encode(file)))
    }

    @Test
    fun newerVersion_isTooNew() = runTest {
        val file = service.export(1L).copy(version = 2)
        assertEquals(BackupResult.TooNew(2), Backup.decode(Backup.encode(file)))
    }

    @Test
    fun garbage_isCorrupt() {
        assertTrue(Backup.decode("not json") is BackupResult.Corrupt)
        assertTrue(Backup.decode("{}") is BackupResult.Corrupt)
        assertTrue(Backup.decode("[1]") is BackupResult.Corrupt)
        assertTrue(Backup.decode("""{"version":"x"}""") is BackupResult.Corrupt)
        assertTrue(Backup.decode("""{"version":1}""") is BackupResult.Corrupt)
    }

    @Test
    fun restore_replacesAllData() = runTest {
        seedData()
        val exported = service.export(1L)
        db.sets().clear()
        db.sessions().clear()
        db.sessions().insert(SessionEntity(date = LocalDate.of(2026, 6, 1), bodyweightKg = null, readiness = null))
        settings.update { AppSettings() }

        service.restore(exported)

        assertEquals(exported, service.export(1L))
        assertEquals(setOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY), settings.settings.first().trainingDays)
    }

    @Test
    fun restore_failureLeavesDataUntouched() = runTest {
        seedData()
        val before = service.export(1L)
        val bad = before.copy(sets = before.sets.map { it.copy(exerciseId = 999_999) })

        assertThrows(Exception::class.java) { kotlinx.coroutines.runBlocking { service.restore(bad) } }

        assertEquals(before, service.export(1L))
    }

    @Test
    fun restore_invalidSettings_leavesDataUntouched() = runTest {
        seedData()
        val before = service.export(1L)
        val bad = before.copy(settings = before.settings.copy(trainingDays = listOf(9)))

        assertThrows(Exception::class.java) { kotlinx.coroutines.runBlocking { service.restore(bad) } }

        assertEquals(before, service.export(1L))
        assertTrue(Backup.decode(Backup.encode(bad)) is BackupResult.Corrupt)
    }
}
