package com.powertracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode.BODYBUILDING
import com.powertracker.domain.model.TrainingMode.POWERLIFTING
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Saving a built plan: one side's plan replaced in one transaction (stage 2 plan, Task 2). */
@RunWith(RobolectricTestRunner::class)
class ReplacePlanTest {
    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
        training = TrainingRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun id(name: String) = training.exercises().first().first { it.name == name }.id

    private fun bb(day: DayOfWeek, exerciseId: Long, order: Int, sets: Int = 3) =
        PlanItem(0, day, exerciseId, sets, 8, 8.0, order, BODYBUILDING, 12)

    private fun names(items: List<PlanItem>, byId: Map<Long, String>) = items.map { "${it.weekday} ${it.order} ${byId[it.exerciseId]} ${it.sets}" }

    @Test
    fun replacesOnlyThatSide_inOrder_resolvingCatalogueNames() = runTest {
        val squat = id("Squat")
        val curl = id("Curl")
        training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, squat, 3, 5, 8.0, 0))
        training.savePlanItem(bb(DayOfWeek.TUESDAY, curl, 0))
        val own = training.addExercise(Exercise(0, "Tyre flip", Category.ACCESSORY, null, emptyList(), isBuiltIn = false))
        val powerliftingBefore = training.plan(POWERLIFTING).first()
        assertTrue("Lat pulldown" !in training.exercises().first().map { it.name })

        training.replacePlan(
            BODYBUILDING,
            listOf(
                bb(DayOfWeek.MONDAY, 0, 0, sets = 4) to "Leg press",
                bb(DayOfWeek.MONDAY, 0, 1) to "Lat pulldown",
                bb(DayOfWeek.MONDAY, own, 2) to null,
                bb(DayOfWeek.THURSDAY, 0, 0) to "Curl",
            ),
        )

        val byId = training.exercises().first().associate { it.id to it.name }
        assertEquals(
            listOf("MONDAY 0 Leg press 4", "MONDAY 1 Lat pulldown 3", "MONDAY 2 Tyre flip 3", "THURSDAY 0 Curl 3"),
            names(training.plan(BODYBUILDING).first(), byId),
        )
        // The catalogue's Curl is the stored Curl row; a name it had no row for got one.
        assertEquals(curl, training.plan(BODYBUILDING).first().last().exerciseId)
        assertEquals(1, byId.values.count { it == "Lat pulldown" })
        assertEquals(powerliftingBefore, training.plan(POWERLIFTING).first())
    }

    @Test
    fun aFailurePartWay_leavesTheOldPlan() = runTest {
        val curl = id("Curl")
        training.savePlanItem(bb(DayOfWeek.TUESDAY, curl, 0))
        val before = training.plan(BODYBUILDING).first()
        val exercisesBefore = training.exercises().first()
        try {
            training.replacePlan(
                BODYBUILDING,
                listOf(bb(DayOfWeek.MONDAY, 0, 0) to "Lat pulldown", bb(DayOfWeek.MONDAY, 0, 1) to "Not an exercise anyone knows"),
            )
            fail("expected the unknown name to fail the save")
        } catch (expected: IllegalArgumentException) {
        }
        assertEquals(before, training.plan(BODYBUILDING).first())
        // Nothing of the half-done save is left: not even the row the first name resolved to.
        assertEquals(exercisesBefore, training.exercises().first())
    }

    @Test
    fun anEmptyListClearsThatSideOnly() = runTest {
        val squat = id("Squat")
        training.savePlanItem(PlanItem(0, DayOfWeek.MONDAY, squat, 3, 5, 8.0, 0))
        training.savePlanItem(bb(DayOfWeek.TUESDAY, squat, 0))
        training.replacePlan(BODYBUILDING, emptyList())
        assertTrue(training.plan(BODYBUILDING).first().isEmpty())
        assertEquals(1, training.plan(POWERLIFTING).first().size)
    }
}
