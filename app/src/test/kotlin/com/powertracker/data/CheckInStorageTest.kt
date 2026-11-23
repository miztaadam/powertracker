package com.powertracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.coach.PlanChange
import com.powertracker.domain.library.MuscleGroup
import com.powertracker.domain.model.CheckIn
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Stage 3: check-ins are stored, one per date, and a coach's plan change lands in one go. */
@RunWith(RobolectricTestRunner::class)
class CheckInStorageTest {
    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
        training = TrainingRepository(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun savedCheckIns_readBackOldestFirst() = runTest {
        training.saveCheckIn(CheckIn(LocalDate(2026, 11, 10), setOf(MuscleGroup.CHEST, MuscleGroup.SIDE_DELTS), setOf(MuscleGroup.QUADS)))
        training.saveCheckIn(CheckIn(LocalDate(2026, 10, 13), emptySet(), emptySet()))

        val read = training.checkIns().first()

        assertEquals(listOf(LocalDate(2026, 10, 13), LocalDate(2026, 11, 10)), read.map { it.date })
        assertEquals(setOf(MuscleGroup.CHEST, MuscleGroup.SIDE_DELTS), read[1].lagging)
        assertEquals(setOf(MuscleGroup.QUADS), read[1].growingWell)
        assertEquals(emptySet<MuscleGroup>(), read[0].lagging)
        assertTrue(read.all { it.id > 0 })
    }

    @Test
    fun aSecondCheckInOnTheSameDate_replacesTheFirst() = runTest {
        val date = LocalDate(2026, 11, 10)
        training.saveCheckIn(CheckIn(date, setOf(MuscleGroup.CHEST), emptySet()))
        training.saveCheckIn(CheckIn(date, setOf(MuscleGroup.BICEPS), setOf(MuscleGroup.CHEST)))

        val read = training.checkIns().first()

        assertEquals(1, read.size)
        assertEquals(setOf(MuscleGroup.BICEPS), read.single().lagging)
        assertEquals(setOf(MuscleGroup.CHEST), read.single().growingWell)
    }

    @Test
    fun groupsAreStoredAsTheirNames_commaSeparated() = runTest {
        training.saveCheckIn(CheckIn(LocalDate(2026, 11, 10), setOf(MuscleGroup.UPPER_BACK, MuscleGroup.CHEST), emptySet()))
        db.openHelper.readableDatabase.query("SELECT lagging, growingWell FROM check_ins").use { c ->
            c.moveToFirst()
            assertEquals("CHEST,UPPER_BACK", c.getString(0))
            assertEquals("", c.getString(1))
        }
    }

    @Test
    fun aPlanChange_savesUpdatesDeletesRemovedAndInsertsAdded_resolvingCatalogueNames() = runTest {
        val curl = training.exercises().first().first { it.name == "Curl" }.id
        val squat = training.exercises().first().first { it.name == "Squat" }.id
        fun bb(day: DayOfWeek, id: Long, order: Int) = PlanItem(0, day, id, 3, 8, 8.0, order, TrainingMode.BODYBUILDING, 12)
        training.savePlanItem(bb(DayOfWeek.MONDAY, curl, 0))
        training.savePlanItem(bb(DayOfWeek.MONDAY, squat, 1))
        val powerlifting = PlanItem(0, DayOfWeek.TUESDAY, squat, 5, 5, 8.0, 0)
        training.savePlanItem(powerlifting)
        val plan = training.plan(TrainingMode.BODYBUILDING).first()
        val (curlItem, squatItem) = plan

        training.applyPlanChange(
            PlanChange(
                updated = listOf(curlItem.copy(sets = 4)),
                removed = listOf(squatItem),
                added = listOf(squatItem.copy(id = 0, exerciseId = 0) to "Hack squat"),
                message = "", changed = true,
            ),
        )

        val after = training.plan(TrainingMode.BODYBUILDING).first()
        val names = training.exercises().first().associate { it.id to it.name }
        assertEquals(listOf("Curl" to 4, "Hack squat" to 3), after.map { names.getValue(it.exerciseId) to it.sets })
        assertEquals(listOf(0, 1), after.map { it.order })
        assertEquals(1, training.plan(TrainingMode.POWERLIFTING).first().size)
    }
}
