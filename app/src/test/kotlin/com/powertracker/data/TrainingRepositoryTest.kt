package com.powertracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.coach.AccessoryMap
import com.powertracker.domain.seed.SeedLibrary
import com.powertracker.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrainingRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.inMemory(context)
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun byName(name: String) = training.exercises().first().first { it.name == name }

    @Test
    fun seedContainsEveryAccessoryMapName() = runTest {
        val names = training.exercises().first().map { it.name }.toSet()
        assertTrue(names.containsAll(AccessoryMap.allExerciseNames))
        assertTrue(names.containsAll(listOf("Squat", "Bench", "Deadlift", "Curl", "Row", "Pull-up")))
        val all = training.exercises().first()
        assertTrue(all.all { it.isBuiltIn })
        assertEquals(Category.SQUAT, byName("Squat").category)
        assertEquals(Category.VARIATION, byName("Pause squat").category)
        assertEquals(MainLift.SQUAT, byName("Pause squat").parentLift)
        assertEquals(Category.ACCESSORY, byName("Hip thrust").category)
        assertNull(byName("Hip thrust").parentLift)
        assertEquals(listOf("Glutes", "Hips", "Upper back"), byName("Hip thrust").muscles)
    }

    @Test
    fun seedIds_unchanged() = runTest {
        val all = training.exercises().first()
        assertEquals(SeedLibrary.exercises(), all)
        assertEquals(listOf("Squat" to 1L, "Bench" to 2L, "Deadlift" to 3L), all.take(3).map { it.name to it.id })
        assertEquals((1L..all.size).toList(), all.map { it.id })
    }

    @Test
    fun logAndReadHistory_roundTrips() = runTest {
        val squat = byName("Squat")
        val date = LocalDate(2026, 11, 2)
        val sessionId = training.startSession(date, 82.5, null)
        training.logSet(sessionId, LoggedSet(squat.id, 140.0, 5, 8.5, false, null, true))
        training.logSet(sessionId, LoggedSet(squat.id, 150.0, 3, null, true, MissPoint.MID, false))
        training.setBaseline(Baseline(squat.id, 170.0, date))

        val history = training.history().first()
        assertEquals(1, history.sessions.size)
        val s = history.sessions.single()
        assertEquals(sessionId, s.id)
        assertEquals(date, s.date)
        assertEquals(82.5, s.bodyweightKg!!, 0.0)
        assertNull(s.readiness)
        assertEquals(
            listOf(
                LoggedSet(squat.id, 140.0, 5, 8.5, false, null, true),
                LoggedSet(squat.id, 150.0, 3, null, true, MissPoint.MID, false),
            ),
            s.sets,
        )
        assertEquals(listOf(Baseline(squat.id, 170.0, date)), history.baselines)
        assertEquals(squat, history.exercises[squat.id])
    }

    @Test
    fun setsForSessionAndSessionOn() = runTest {
        val squat = byName("Squat")
        val date = LocalDate(2026, 11, 3)
        assertNull(training.sessionOn(date).first())
        val sid = training.startSession(date, null, 4)
        val set = LoggedSet(squat.id, 100.0, 5, 7.0, false, null, false)
        val setId = training.logSet(sid, set)
        assertEquals(sid, training.sessionOn(date).first()!!.id)
        assertEquals(listOf(StoredSet(setId, sid, set)), training.setsForSession(sid).first())
        training.updateSet(setId, set.copy(weightKg = 105.0))
        assertEquals(105.0, training.setsForSession(sid).first().single().set.weightKg, 0.0)
        training.deleteSet(setId)
        assertTrue(training.setsForSession(sid).first().isEmpty())
    }

    @Test
    fun deleteExerciseWithSets_throws_archiveWorks() = runTest {
        val bench = byName("Bench")
        val sid = training.startSession(LocalDate(2026, 11, 2), null, null)
        training.logSet(sid, LoggedSet(bench.id, 100.0, 5, 8.0, false, null, true))

        assertThrows(ExerciseInUseException::class.java) { kotlinx.coroutines.runBlocking { training.deleteExercise(bench.id) } }
        assertTrue(training.exercises().first().any { it.id == bench.id })

        training.archiveExercise(bench.id)
        val history = training.history().first()
        assertTrue(history.exercises.getValue(bench.id).archived)
        assertEquals(1, history.sessions.single().sets.size)

        val custom = training.addExercise(Exercise(0, "Zercher squat", Category.ACCESSORY, null, listOf("Quads"), false))
        training.deleteExercise(custom)
        assertNull(training.exercises().first().firstOrNull { it.id == custom })
    }

    @Test
    fun planItemsRoundTripAndDelete() = runTest {
        val squat = byName("Squat")
        val id = training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, squat.id, 3, 5, 8.0, 1))
        assertEquals(listOf(PlanItem(id, DayOfWeek.WEDNESDAY, squat.id, 3, 5, 8.0, 1)), training.plan().first())
        training.savePlanItem(PlanItem(id, DayOfWeek.WEDNESDAY, squat.id, 4, 5, 8.0, 1))
        assertEquals(4, training.plan().first().single().sets)
        training.deletePlanItem(id)
        assertTrue(training.plan().first().isEmpty())
    }

    @Test
    fun advicePayload_roundTrips() = runTest {
        val date = LocalDate(2026, 11, 9)
        val payloads = listOf(
            AdviceType.TARGET to AdvicePayload.Target(1, 142.5, 5, 8.0, 3),
            AdviceType.TARGET to AdvicePayload.Accessories(listOf(AccessoryPrescription("Pause squat", 3, 3, 5))),
            AdviceType.DELOAD to AdvicePayload.Deload(),
            AdviceType.WEAK_POINT to AdvicePayload.WeakPoint(MainLift.BENCH, MissPoint.LOCKOUT),
            AdviceType.RE_ENTRY to AdvicePayload.ReEntry(),
        )
        advice.insert(payloads.mapIndexed { i, (t, p) -> Advice(t, MainLift.SQUAT, date, "r$i", i, p) })
        val stored = advice.all()
        assertEquals(payloads.map { it.second }, stored.map { it.payload })
        assertTrue(stored.all { it.id > 0 && it.status == AdviceStatus.OPEN && it.createdAt == date })
        assertEquals(5, advice.open().first().size)

        advice.setStatus(stored[0].id, AdviceStatus.APPLIED)
        assertEquals(4, advice.open().first().size)
        assertEquals(AdviceStatus.APPLIED, advice.all().first { it.id == stored[0].id }.status)
    }
}
