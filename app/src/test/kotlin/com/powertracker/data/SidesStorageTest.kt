package com.powertracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Bodybuilding mode spec §5.1 and §6: each side keeps its own plan; one log, each session and advice knowing its side. */
@RunWith(RobolectricTestRunner::class)
class SidesStorageTest {
    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository
    private lateinit var advice: AdviceRepository

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
        training = TrainingRepository(db)
        advice = AdviceRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun id(name: String) = training.exercises().first().first { it.name == name }.id

    @Test
    fun bodybuildingPlan_besidePowerlifting_eachReadReturnsItsOwnSide() = runTest {
        val squat = id("Squat")
        val curl = id("Curl")
        training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, squat, sets = 5, reps = 5, targetRpe = 8.0, order = 0))
        training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, squat, sets = 3, reps = 3, targetRpe = 8.5, order = 0))
        val powerlifting = training.plan(TrainingMode.POWERLIFTING).first()

        val bb1 = PlanItem(0, DayOfWeek.MONDAY, curl, sets = 3, reps = 8, targetRpe = 8.0, order = 0, mode = TrainingMode.BODYBUILDING, repsHigh = 12)
        val bb2 = PlanItem(0, DayOfWeek.MONDAY, squat, sets = 4, reps = 10, targetRpe = 8.0, order = 1, mode = TrainingMode.BODYBUILDING, repsHigh = 15)
        val id1 = training.savePlanItem(bb1)
        training.savePlanItem(bb2)
        // Editing and deleting on the bodybuilding side.
        training.savePlanItem(bb1.copy(id = id1, sets = 4))
        val bodybuilding = training.plan(TrainingMode.BODYBUILDING).first()

        assertEquals(powerlifting, training.plan(TrainingMode.POWERLIFTING).first())
        assertEquals(powerlifting, training.plan().first())
        assertEquals(listOf(bb1.copy(sets = 4), bb2), bodybuilding.map { it.copy(id = 0) })
        assertEquals(listOf(12, 15), bodybuilding.map { it.repsHigh })

        training.deletePlanItem(id1)
        assertEquals(listOf(bb2), training.plan(TrainingMode.BODYBUILDING).first().map { it.copy(id = 0) })
        assertEquals(powerlifting, training.plan(TrainingMode.POWERLIFTING).first())
        assertEquals(powerlifting.map { null }, powerlifting.map { it.repsHigh })
    }

    @Test
    fun sessions_areStampedWithTheirSide_oneLog() = runTest {
        val curl = id("Curl")
        val pl = training.startSession(LocalDate(2026, 11, 2), 84.0, 4)
        val bb = training.startSession(LocalDate(2026, 11, 4), null, null, TrainingMode.BODYBUILDING)
        training.logSet(pl, LoggedSet(curl, 20.0, 10, 8.0, failed = false, missPoint = null, isTopSet = false))
        training.logSet(bb, LoggedSet(curl, 22.0, 11, 8.0, failed = false, missPoint = null, isTopSet = false))

        val history = training.history().first()
        assertEquals(listOf(TrainingMode.POWERLIFTING, TrainingMode.BODYBUILDING), history.sessions.map { it.mode })
        assertEquals(listOf(20.0, 22.0), history.sessions.flatMap { s -> s.sets.map { it.weightKg } })
        assertEquals(TrainingMode.BODYBUILDING, training.sessionOn(LocalDate(2026, 11, 4)).first()?.mode)
    }

    @Test
    fun advice_keepsItsSide() = runTest {
        val today = LocalDate(2026, 11, 4)
        advice.insert(
            listOf(
                Advice(AdviceType.TARGET, MainLift.SQUAT, today, "Coach suggests 150 × 5 @ 8.", 0, AdvicePayload.Target(1, 150.0, 5, 8.0)),
                Advice(AdviceType.TARGET, null, today, "Coach suggests 26 × 8–12 @ 8.", 0,
                    AdvicePayload.Target(9, 26.0, 8, 8.0, repsHigh = 12), mode = TrainingMode.BODYBUILDING),
            ),
        )
        val stored = advice.all()
        assertEquals(listOf(TrainingMode.POWERLIFTING, TrainingMode.BODYBUILDING), stored.map { it.mode })
        assertEquals(12, (stored[1].payload as AdvicePayload.Target).repsHigh)
        assertEquals(stored, advice.open().first().sortedBy { it.id })
    }
}
