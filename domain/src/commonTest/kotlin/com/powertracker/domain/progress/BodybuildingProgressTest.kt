package com.powertracker.domain.progress

import com.powertracker.domain.Ids
import com.powertracker.domain.SessionBuilder
import com.powertracker.domain.history
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.TrainingHistory
import com.powertracker.domain.model.TrainingMode
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.plus

/** The bodybuilding side's Progress rows (spec 7.6): best set per session and whether it is going up. */
class BodybuildingProgressTest {
    private val start = LocalDate(2026, 1, 5)
    private val today = start.plus(60, DateTimeUnit.DAY)
    private val bb = TrainingMode.BODYBUILDING

    private fun item(id: Long, exerciseId: Long, weekday: DayOfWeek = DayOfWeek.MONDAY, order: Int = 0) =
        PlanItem(id, weekday, exerciseId, 3, 8, 8.0, order, bb, 12)

    private val plan = listOf(item(1, Ids.DB_PRESS))

    private fun weeks(vararg sessions: SessionBuilder.() -> Unit): TrainingHistory = history(start) {
        sessions.forEachIndexed { i, block -> session(i * 7, mode = bb) { block() } }
    }

    private fun SessionBuilder.sets(kg: Double, vararg reps: Int) = reps.forEach { set(Ids.DB_PRESS, kg, it) }

    private fun set(kg: Double, reps: Int) = LoggedSet(Ids.DB_PRESS, kg, reps, 8.0, false, null, false)

    @Test
    fun aHeavierWeekIsGoingUp() {
        val h = weeks({ sets(26.0, 12, 12, 12) }, { sets(26.0, 10); sets(28.0, 9, 8, 8) })
        val row = BodybuildingProgress.rows(h, plan, today).single()
        assertEquals(Ids.DB_PRESS, row.exerciseId)
        assertEquals("DB press", row.name)
        assertEquals(listOf(start to set(26.0, 12), start.plus(7, DateTimeUnit.DAY) to set(28.0, 9)), row.bestSets)
        assertEquals(true, row.goingUp)
    }

    @Test
    fun theSameWeightWithMoreRepsInTotalIsGoingUp() {
        val h = weeks({ sets(26.0, 11, 10, 9) }, { sets(26.0, 11, 11, 10) })
        val row = BodybuildingProgress.rows(h, plan, today).single()
        assertEquals(listOf(11, 11), row.bestSets.map { it.second.reps })
        assertEquals(true, row.goingUp)
    }

    @Test
    fun aWorseWeekIsNotGoingUp() {
        val h = weeks({ sets(26.0, 11, 10, 9) }, { sets(26.0, 10, 9, 9) })
        assertEquals(false, BodybuildingProgress.rows(h, plan, today).single().goingUp)
        val lighter = weeks({ sets(26.0, 11, 10, 9) }, { sets(24.0, 12, 12, 12) })
        assertEquals(false, BodybuildingProgress.rows(lighter, plan, today).single().goingUp)
    }

    @Test
    fun aSingleSessionHasNoTrendYet() {
        val row = BodybuildingProgress.rows(weeks({ sets(26.0, 11, 10, 9) }), plan, today).single()
        assertEquals(listOf(start to set(26.0, 11)), row.bestSets)
        assertNull(row.goingUp)
    }

    @Test
    fun noSessionAtAllIsARowWithNothingInIt() {
        val row = BodybuildingProgress.rows(history(start) { }, plan, today).single()
        assertEquals(emptyList(), row.bestSets)
        assertNull(row.goingUp)
    }

    @Test
    fun theLastFourBodybuildingSessionsOldestFirst() {
        val h = history(start) {
            for (w in 0..5) session(w * 7, mode = bb) { sets(20.0 + w, 10) }
            session(45) { set(Ids.DB_PRESS, 40.0, 5) } // powerlifting side: not counted
        }
        val row = BodybuildingProgress.rows(h, plan, today).single()
        assertEquals(listOf(22.0, 23.0, 24.0, 25.0), row.bestSets.map { it.second.weightKg })
        assertEquals(true, row.goingUp)
    }

    @Test
    fun aMissedSetIsNeverTheBestSet() {
        val h = weeks({
            sets(26.0, 10)
            set(Ids.DB_PRESS, 30.0, 4, failed = true)
        })
        assertEquals(26.0, BodybuildingProgress.rows(h, plan, today).single().bestSets.single().second.weightKg)
    }

    @Test
    fun oneRowPerExerciseInPlanOrderOnTheBodybuildingSideOnly() {
        val plan = listOf(
            item(1, Ids.CURL, DayOfWeek.WEDNESDAY, 0),
            item(2, Ids.DB_PRESS, DayOfWeek.MONDAY, 1),
            item(3, Ids.PULL_UP, DayOfWeek.MONDAY, 0),
            item(4, Ids.DB_PRESS, DayOfWeek.WEDNESDAY, 1),
            PlanItem(5, DayOfWeek.MONDAY, Ids.BENCH, 5, 5, 8.0, 0),
        )
        val rows = BodybuildingProgress.rows(history(start) { }, plan, today)
        assertEquals(listOf(Ids.PULL_UP, Ids.DB_PRESS, Ids.CURL), rows.map { it.exerciseId })
    }

    @Test
    fun sessionsAfterTodayAreNotCounted() {
        val h = weeks({ sets(26.0, 10) }, { sets(28.0, 10) })
        val row = BodybuildingProgress.rows(h, plan, start.plus(3, DateTimeUnit.DAY)).single()
        assertEquals(listOf(26.0), row.bestSets.map { it.second.weightKg })
    }
}
