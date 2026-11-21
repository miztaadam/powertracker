package com.powertracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.db.AdviceEntity
import com.powertracker.data.db.AppDatabase
import com.powertracker.data.db.ExerciseEntity
import com.powertracker.domain.coach.PlanChange
import com.powertracker.domain.model.AdviceStatus
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.PlateCount
import com.powertracker.domain.model.TrainingMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 1.17: each exercise stores its plate setting and each set its unit; changing the setting rewrites no history. */
@RunWith(RobolectricTestRunner::class)
class PlateCountStorageTest {
    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
        training = TrainingRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun rows(name: String): List<ExerciseEntity> = db.exercises().all().filter { it.name.equals(name, ignoreCase = true) }

    /** Takes a seeded exercise out, so its catalogue entry has no row yet. */
    private suspend fun unseed(name: String) {
        for (row in rows(name)) db.exercises().delete(row.id)
        assertTrue(rows(name).isEmpty())
    }

    private fun set(exerciseId: Long, weight: Double, unit: LoadUnit = LoadUnit.KG, top: Boolean = false) =
        LoggedSet(exerciseId, weight, 10, 8.0, failed = false, missPoint = null, isTopSet = top, unit = unit)

    @Test
    fun aFreshInstall_seedsEveryExerciseOff() = runBlocking {
        val all = db.exercises().all()
        assertTrue(all.isNotEmpty())
        assertTrue(all.all { it.plateCount == PlateCount.OFF })
        assertTrue(training.exercises().first().all { it.plateCount == PlateCount.OFF })
    }

    @Test
    fun setPlateCount_changesOnlyThatRow_andNoSetOrAdvice() = runBlocking {
        val pulldown = training.setPlateCount(null, "Lat pulldown", PlateCount.OFF)
        val squat = db.exercises().all().first { it.name == "Squat" }.id
        val session = training.startSession(LocalDate(2026, 11, 18), null, null)
        training.logSet(session, set(squat, 140.0, top = true))
        training.logSet(session, set(pulldown, 55.0))
        db.advice().insert(
            listOf(
                AdviceEntity(type = AdviceType.TARGET, lift = null, createdAt = LocalDate(2026, 11, 18), reason = "Coach suggests 57.5 × 8 @ 8.",
                    severity = 0, payload = """{"type":"target","exerciseId":$pulldown,"weightKg":57.5,"reps":8,"rpe":8.0}""", status = AdviceStatus.OPEN),
            ),
        )
        val setsBefore = db.sets().all()
        val adviceBefore = db.advice().all()
        val othersBefore = db.exercises().all().filter { it.id != pulldown }
        val rowBefore = db.exercises().all().single { it.id == pulldown }

        assertEquals(pulldown, training.setPlateCount(pulldown, null, PlateCount.WHOLE))

        assertEquals(rowBefore.copy(plateCount = PlateCount.WHOLE), db.exercises().all().single { it.id == pulldown })
        assertEquals(othersBefore, db.exercises().all().filter { it.id != pulldown })
        assertEquals(setsBefore, db.sets().all())
        assertEquals(adviceBefore, db.advice().all())

        training.setPlateCount(pulldown, null, PlateCount.OFF)
        assertEquals(rowBefore, db.exercises().all().single { it.id == pulldown })
        assertEquals(setsBefore, db.sets().all())
    }

    @Test
    fun setPlateCount_onACatalogueNameWithNoRow_insertsItsRowOnce() = runBlocking {
        unseed("Lat pulldown")

        val first = training.setPlateCount(null, "Lat pulldown", PlateCount.HALF)
        val second = training.setPlateCount(null, "Lat pulldown", PlateCount.HALF)

        assertEquals(first, second)
        val row = rows("Lat pulldown").single()
        assertEquals(first, row.id)
        assertEquals(PlateCount.HALF, row.plateCount)
        assertTrue(row.isBuiltIn)
        assertEquals(Category.ACCESSORY, row.category)
    }

    @Test
    fun aSetLoggedInPlates_keepsItsUnit_throughHistory() = runBlocking {
        val pulldown = training.setPlateCount(null, "Lat pulldown", PlateCount.HALF)
        val session = training.startSession(LocalDate(2026, 11, 18), null, null)
        training.logSet(session, set(pulldown, 50.0))
        val plateId = training.logSet(session, set(pulldown, 7.5, LoadUnit.PLATES, top = true))

        val history = training.history().first()
        assertEquals(listOf(LoadUnit.KG, LoadUnit.PLATES), history.sessions.single().sets.map { it.unit })
        assertEquals(listOf(50.0, 7.5), history.sessions.single().sets.map { it.weightKg })
        assertEquals(PlateCount.HALF, history.exercises.getValue(pulldown).plateCount)
        assertEquals(listOf(LoadUnit.KG, LoadUnit.PLATES), training.setsForSession(session).first().map { it.set.unit })

        // An edit keeps the unit it is given; turning the setting off rewrites no set.
        training.updateSet(plateId, set(pulldown, 8.0, LoadUnit.PLATES, top = true))
        training.setPlateCount(pulldown, null, PlateCount.OFF)
        assertEquals(listOf(LoadUnit.KG, LoadUnit.PLATES), training.history().first().sessions.single().sets.map { it.unit })
        assertEquals(8.0, db.sets().byId(plateId)!!.weightKg, 0.0)
        // Logged through a catalogue name, the set keeps its unit too.
        training.logSet(session, set(0, 9.0, LoadUnit.PLATES), "Lat pulldown")
        assertEquals(LoadUnit.PLATES, db.sets().all().last().unit)
    }

    @Test
    fun archiveThenRestore_keepsTheSetting() = runBlocking {
        val pulldown = training.setPlateCount(null, "Lat pulldown", PlateCount.WHOLE)
        training.archiveExercise(pulldown)
        assertEquals(PlateCount.WHOLE, db.exercises().all().single { it.id == pulldown }.plateCount)

        // Used again from the catalogue, the archived row is restored with its setting.
        val restored = training.inTransaction { training.resolveCatalogue("Lat pulldown") }

        assertEquals(pulldown, restored)
        val row = db.exercises().all().single { it.id == pulldown }
        assertFalse(row.archived)
        assertEquals(PlateCount.WHOLE, row.plateCount)
    }

    @Test
    fun aSwappedInExercise_startsOff() = runBlocking {
        val pulldown = training.setPlateCount(null, "Lat pulldown", PlateCount.WHOLE)
        unseed("Seated cable row")
        val item = PlanItem(0, DayOfWeek.MONDAY, pulldown, 3, 8, 8.0, 0, TrainingMode.BODYBUILDING, 12)
        val id = training.savePlanItem(item)

        training.applyPlanChange(
            PlanChange(updated = emptyList(), removed = listOf(item.copy(id = id)), added = listOf(item to "Seated cable row"), message = "", changed = true),
        )

        val swapped = rows("Seated cable row").single()
        assertEquals(PlateCount.OFF, swapped.plateCount)
        // The setting stays with the old exercise, ready if the lifter swaps back.
        assertEquals(PlateCount.WHOLE, db.exercises().all().single { it.id == pulldown }.plateCount)
    }

    @Test
    fun addExercise_writesItsSetting() = runBlocking {
        val id = training.addExercise(Exercise(0, "My row machine", Category.ACCESSORY, null, listOf("Back"), isBuiltIn = false, plateCount = PlateCount.HALF))
        assertEquals(PlateCount.HALF, db.exercises().all().single { it.id == id }.plateCount)
        val plain = training.addExercise(Exercise(0, "My other machine", Category.ACCESSORY, null, listOf("Back"), isBuiltIn = false))
        assertEquals(PlateCount.OFF, db.exercises().all().single { it.id == plain }.plateCount)
    }
}
