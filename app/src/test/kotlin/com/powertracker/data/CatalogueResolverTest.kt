package com.powertracker.data

import android.content.Context
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.db.AppDatabase
import com.powertracker.data.db.ExerciseEntity
import com.powertracker.domain.library.Library
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalogueResolverTest {
    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository
    private lateinit var resolver: CatalogueResolver

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
        training = TrainingRepository(db)
        resolver = CatalogueResolver(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun rows(name: String): List<ExerciseEntity> = db.exercises().all().filter { it.name.equals(name, ignoreCase = true) }

    /** Takes a seeded exercise out, so its catalogue entry has no row yet: the state of a library exercise never used. */
    private suspend fun unseed(name: String) {
        for (row in rows(name)) db.exercises().delete(row.id)
        assertTrue(rows(name).isEmpty())
    }

    private suspend fun resolve(name: String): Long = db.withTransaction { resolver.resolve(name) }

    /** Makes every insert into [table] fail, the way a full disk or a broken row would. */
    private fun failInsertsInto(table: String) {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_$table BEFORE INSERT ON $table BEGIN SELECT RAISE(ABORT, 'forced failure'); END")
    }

    @Test
    fun firstAdd_insertsOneBuiltInRow() = runBlocking {
        unseed("Curl")
        val before = db.exercises().all().size

        val id = resolve("Curl")

        val entry = Library.find("Curl")!!
        assertEquals(before + 1, db.exercises().all().size)
        assertEquals(
            listOf(ExerciseEntity(id, "Curl", entry.category, entry.parentLift, entry.muscles, isBuiltIn = true, archived = false)),
            rows("Curl"),
        )
    }

    @Test
    fun secondAdd_reusesIt() = runBlocking {
        unseed("Curl")

        val first = resolve("Curl")
        val second = resolve("Curl")

        assertEquals(first, second)
        assertEquals(1, rows("Curl").size)
    }

    @Test
    fun seededBuiltIn_isReused() = runBlocking {
        val seeded = db.exercises().all()
        val squat = seeded.single { it.name == "Squat" }

        assertEquals(squat.id, resolve("Squat"))
        // Ignoring case, and by alias: the catalogue's own spelling is not needed to find the row.
        assertEquals(squat.id, resolve("squat"))
        assertEquals(squat.id, resolve("Back squat"))
        assertEquals(seeded, db.exercises().all())
    }

    @Test
    fun archivedSameName_isRestored() = runBlocking {
        val curl = rows("Curl").single()
        training.archiveExercise(curl.id)
        assertTrue(rows("Curl").single().archived)

        assertEquals(curl.id, resolve("Curl"))

        assertEquals(listOf(curl.copy(archived = false)), rows("Curl"))
    }

    @Test
    fun addCatalogue_reusesSameNamedCustomRow() = runBlocking {
        // The lifter made their own "hip thrust" before the catalogue had one.
        unseed("Hip thrust")
        val own = training.addExercise(Exercise(0, "hip thrust", Category.ACCESSORY, null, listOf("Glutes"), isBuiltIn = false))

        val planItem = training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, exerciseId = 0, sets = 3, reps = 10, targetRpe = 8.0, order = 0), catalogueName = "Hip thrust")

        // No duplicate: the plan item points at the lifter's own row, which stays theirs.
        assertEquals(listOf(ExerciseEntity(own, "hip thrust", Category.ACCESSORY, null, listOf("Glutes"), isBuiltIn = false)), rows("Hip thrust"))
        assertEquals(own, training.plan().first().single { it.id == planItem }.exerciseId)
        // The form guide follows the name, so the info button shows the catalogue guide for it.
        assertNotNull(Library.find("hip thrust"))
    }

    @Test
    fun insertAndPlanItem_areOneTransaction() = runBlocking {
        unseed("Curl")
        failInsertsInto("plan_item")
        try {
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, exerciseId = 0, sets = 3, reps = 10, targetRpe = 8.0, order = 0), catalogueName = "Curl")
            fail("the plan item insert should have failed")
        } catch (expected: android.database.sqlite.SQLiteException) {
            // The forced failure.
        }
        assertTrue("no exercise row is left behind", rows("Curl").isEmpty())
        assertTrue(training.plan().first().isEmpty())

        // The same for a set logged on Today.
        val session = training.startSession(LocalDate(2026, 11, 11), null, null)
        failInsertsInto("logged_set")
        try {
            training.logSet(session, LoggedSet(0, 20.0, 10, 8.0, failed = false, missPoint = null, isTopSet = false), catalogueName = "Curl")
            fail("the set insert should have failed")
        } catch (expected: android.database.sqlite.SQLiteException) {
            // The forced failure.
        }
        assertTrue("no exercise row is left behind", rows("Curl").isEmpty())
    }

    @Test
    fun archivedRowIsRestored_onlyWhenThePlanItemIsSaved() = runBlocking {
        val curl = rows("Curl").single()
        training.archiveExercise(curl.id)
        failInsertsInto("plan_item")
        try {
            training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, exerciseId = 0, sets = 3, reps = 10, targetRpe = 8.0, order = 0), catalogueName = "Curl")
            fail("the plan item insert should have failed")
        } catch (expected: android.database.sqlite.SQLiteException) {
            // The forced failure.
        }
        assertTrue("the restore is rolled back with the plan item", rows("Curl").single().archived)
        assertFalse(rows("Curl").size > 1)
    }
}
