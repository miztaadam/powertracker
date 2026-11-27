package com.powertracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.powertracker.data.db.AppDatabase
import com.powertracker.domain.coach.PlanChange
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Supersets in the Android app's storage (1.24): the link on a plan item, and what keeps or clears it. As the web's `supersets.test.ts`. */
@RunWith(RobolectricTestRunner::class)
class SupersetStorageTest {
    private lateinit var db: AppDatabase
    private lateinit var training: TrainingRepository

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>())
        training = TrainingRepository(db)
    }

    @After
    fun tearDown() = db.close()

    /** Wednesday on the powerlifting side: four different exercises in order; the items' ids, top to bottom. */
    private suspend fun wednesday(): List<Long> {
        val exercises = training.exercises().first().take(4)
        return exercises.mapIndexed { order, e -> training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, e.id, 3, 8, 8.0, order)) }
    }

    private suspend fun plan() = training.plan(TrainingMode.POWERLIFTING).first().filter { it.weekday == DayOfWeek.WEDNESDAY }
    private suspend fun linked() = plan().filter { it.supersetWithAbove }.map { it.id }
    private suspend fun order() = plan().map { it.id }
    private suspend fun item(id: Long) = plan().first { it.id == id }

    /** The Plan screen's move: the two items swap their places, inside the link rule, in one transaction. */
    private suspend fun move(id: Long, direction: Int) {
        val rows = plan()
        val i = rows.indexOfFirst { it.id == id }
        val moved = rows[i]
        val other = rows.getOrNull(i + direction) ?: return
        training.inTransaction {
            training.keepingLinks(moved.mode) {
                training.savePlanItem(moved.copy(order = other.order))
                training.savePlanItem(other.copy(order = moved.order))
            }
        }
    }

    @Test
    fun linksToTheOneAbove_andUnlinks() = runTest {
        val (a, b) = wednesday()
        assertTrue(training.setSuperset(b, true))
        assertEquals(listOf(b), linked())
        assertFalse(item(a).supersetWithAbove)
        assertTrue(training.setSuperset(b, false))
        assertEquals(emptyList<Long>(), linked())
    }

    @Test
    fun refusesWhereTheRuleSaysNo_changingNothing() = runTest {
        val (a, b, c, d) = wednesday()
        assertFalse("first on its day", training.setSuperset(a, true))
        assertFalse("no such item", training.setSuperset(9999, true))
        assertTrue(training.setSuperset(b, true))
        assertFalse("the one above is a pair's second", training.setSuperset(c, true))
        assertTrue(training.setSuperset(d, true))
        assertEquals(listOf(b, d), linked())
        // The same exercise as the one above.
        training.setSuperset(d, false)
        training.savePlanItem(item(d).copy(exerciseId = item(c).exerciseId))
        assertFalse(training.setSuperset(d, true))
        assertEquals(listOf(b), linked())
    }

    @Test
    fun editingAnItemsOwnNumbersOrExercise_keepsIt() = runTest {
        val (a, b) = wednesday()
        training.setSuperset(b, true)
        val other = training.exercises().first().last().id
        // As the Plan screen saves an edit: the stored item with its numbers changed.
        training.savePlanItem(item(b).copy(exerciseId = other, sets = 5, reps = 3))
        training.savePlanItem(item(a).copy(sets = 4))
        assertEquals(listOf(b), linked())
        assertEquals(5, item(b).sets)
    }

    @Test
    fun deletingTheOneAbove_unlinksTheOneUnderIt_andDeletingBetweenOrTheSecondDoesWhatTheRuleSays() = runTest {
        val (a, b, c, d) = wednesday()
        training.setSuperset(b, true)
        training.setSuperset(d, true)
        training.deletePlanItem(a)
        assertEquals(listOf(d), linked())
        assertFalse(item(b).supersetWithAbove)
        // The item between b and d's pair... c is d's first: deleting it unlinks d.
        training.deletePlanItem(c)
        assertEquals(listOf(b, d), order())
        assertEquals(emptyList<Long>(), linked())
        // A second deleted leaves nothing behind.
        training.setSuperset(d, true)
        training.deletePlanItem(d)
        assertEquals(emptyList<Long>(), linked())
        // An id that is not stored changes nothing.
        training.deletePlanItem(9999)
        assertEquals(listOf(b), order())
    }

    @Test
    fun aMove_unlinksEveryItemWhoseItemAboveChanged_andKeepsAPairThatDidNotMove() = runTest {
        val (a, b, c, d) = wednesday()
        training.setSuperset(b, true)
        training.setSuperset(d, true)
        // The first pair's first moved down: its two change places, and its link is gone. The other pair stands.
        move(a, 1)
        assertEquals(listOf(b, a, c, d), order())
        assertEquals(listOf(d), linked())
        // c moved down: d goes above it, and is no longer under c.
        move(c, 1)
        assertEquals(listOf(b, a, d, c), order())
        assertEquals(emptyList<Long>(), linked())
        // A pair elsewhere on the day is not touched by a move below it.
        training.setSuperset(a, true)
        move(c, -1)
        assertEquals(listOf(b, a, c, d), order())
        assertEquals(listOf(a), linked())
    }

    @Test
    fun theCoachsSwap_unlinksThePairItTouches_andMoreWorkKeepsIt() = runTest {
        val (a, b, c, d) = wednesday()
        training.setSuperset(b, true)
        training.setSuperset(d, true)
        // More work: an item's sets go up, the item handed back whole by the domain. Its link comes back with it.
        training.applyPlanChange(PlanChange(updated = listOf(item(b).copy(sets = 4)), removed = emptyList(), added = emptyList(), message = "", changed = true))
        assertEquals(4, item(b).sets)
        assertEquals(listOf(b, d), linked())
        // A swap: the first of the second pair is deleted and another exercise added in its place, as a new item.
        val old = item(c)
        training.applyPlanChange(PlanChange(emptyList(), listOf(old), listOf(old.copy(id = 0, exerciseId = 0) to "Leg press"), "", true))
        assertEquals(4, order().size)
        assertEquals(listOf(b), linked())
        // The second of a pair swapped: the new item carries no link (it is a new item, whatever the old one's flag).
        val second = item(b)
        training.applyPlanChange(PlanChange(emptyList(), listOf(second), listOf(second.copy(id = 0, exerciseId = 0) to "Lat pulldown"), "", true))
        assertEquals(emptyList<Long>(), linked())
        assertEquals(a, order().first())
    }

    @Test
    fun aWholePlanReplaced_thenPutBackByUndo_hasItsLinksAgain() = runTest {
        val (_, b) = wednesday()
        training.setSuperset(b, true)
        val snapshot = training.inTransaction {
            val taken = training.planSnapshot(TrainingMode.POWERLIFTING, emptySet())
            val exercise = training.exercises().first().first().id
            training.replacePlan(TrainingMode.POWERLIFTING, listOf(PlanItem(0, DayOfWeek.MONDAY, exercise, 3, 5, 8.0, 0) to null))
            taken.copy(replacedBy = training.planIds(TrainingMode.POWERLIFTING))
        }
        training.saved(snapshot)
        assertEquals(emptyList<Long>(), linked())
        assertTrue(training.restorePlan(TrainingMode.POWERLIFTING, snapshot))
        assertEquals(listOf(b), linked())
    }

    @Test
    fun theOtherSidesPlan_isNeverTouched() = runTest {
        val (a, b) = wednesday()
        training.setSuperset(b, true)
        val exercises = training.exercises().first().take(2)
        val other = exercises.mapIndexed { order, e -> training.savePlanItem(PlanItem(0, DayOfWeek.WEDNESDAY, e.id, 3, 8, 8.0, order, TrainingMode.BODYBUILDING, 12)) }
        assertTrue(training.setSuperset(other[1], true))
        // The powerlifting pair's first deleted: the bodybuilding pair stands.
        training.deletePlanItem(a)
        assertEquals(emptyList<Long>(), linked())
        assertEquals(listOf(other[1]), training.plan(TrainingMode.BODYBUILDING).first().filter { it.supersetWithAbove }.map { it.id })
    }
}
