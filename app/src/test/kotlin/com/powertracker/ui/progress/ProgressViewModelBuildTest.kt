package com.powertracker.ui.progress

import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.LoggedSession
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.TrainingHistory
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressViewModelBuildTest {
    private val squat = Exercise(1, "Squat", Category.SQUAT, null, emptyList(), isBuiltIn = true)
    private val bench = Exercise(2, "Bench", Category.BENCH, null, emptyList(), isBuiltIn = true)
    private val deadlift = Exercise(3, "Deadlift", Category.DEADLIFT, null, emptyList(), isBuiltIn = true)
    private val exercises = listOf(squat, bench, deadlift).associateBy { it.id }

    private val today: LocalDate = LocalDate(2026, 11, 11) // a Wednesday
    private val thisWeek: LocalDate = LocalDate(2026, 11, 9)

    /** A single at RPE 10: its e1RM is its weight. */
    private fun single(exercise: Exercise, kg: Double, failed: Boolean = false) =
        LoggedSet(exercise.id, kg, 1, 10.0, failed, null, isTopSet = false)

    private fun history(vararg sessions: Pair<LocalDate, List<LoggedSet>>) = TrainingHistory(
        sessions.mapIndexed { i, (date, sets) -> LoggedSession(i + 1L, date, null, null, sets) },
        exercises,
        emptyList(),
    )

    private fun build(history: TrainingHistory, range: ChartRange = ChartRange.MONTHS_6) = ProgressViewModel.build(history, range, today)

    /** Squat nine weeks ago and this week. */
    private val nineWeeks = history(
        thisWeek.minus(9, DateTimeUnit.WEEK) to listOf(single(squat, 100.0)),
        thisWeek to listOf(single(squat, 110.0)),
    )

    @Test
    fun range_eightWeeksDropsANineWeekOldPoint() {
        val state = build(nineWeeks, ChartRange.WEEKS_8)

        assertEquals(8, state.weeks.size)
        assertEquals(thisWeek.minus(7, DateTimeUnit.WEEK), state.weeks.first())
        assertEquals(thisWeek, state.weeks.last())
        assertEquals(listOf(110f), state.e1rm.getValue(MainLift.SQUAT).filterNotNull())
    }

    @Test
    fun range_allKeepsEveryPointAndStartsAtTheFirstOne() {
        val state = build(nineWeeks, ChartRange.ALL)

        assertEquals(10, state.weeks.size)
        assertEquals(thisWeek.minus(9, DateTimeUnit.WEEK), state.weeks.first())
        assertEquals(listOf(100f, 110f), state.e1rm.getValue(MainLift.SQUAT).filterNotNull())
        // Weeks without the lift are gaps, not zeros.
        assertNull(state.e1rm.getValue(MainLift.SQUAT)[1])
    }

    @Test
    fun range_longerThanHistoryStartsAtTheFirstPoint() {
        assertEquals(thisWeek.minus(9, DateTimeUnit.WEEK), build(nineWeeks, ChartRange.YEAR_1).weeks.first())
    }

    @Test
    fun change_positiveWhenALiftRoseBetweenTheTwoWindows() {
        // 100 five weeks ago (inside the window that ended 28 days ago), 107.5 this week.
        val state = build(history(today.minus(35, DateTimeUnit.DAY) to listOf(single(squat, 100.0)), today.minus(2, DateTimeUnit.DAY) to listOf(single(squat, 107.5))))

        val summary = state.lifts.first { it.lift == MainLift.SQUAT }
        assertEquals(107.5, summary.e1rmKg!!, 1e-9)
        assertEquals(7.5, summary.changeKg!!, 1e-9)
    }

    @Test
    fun change_nullWithNoPriorData() {
        val state = build(history(today.minus(2, DateTimeUnit.DAY) to listOf(single(squat, 107.5))))

        val summary = state.lifts.first { it.lift == MainLift.SQUAT }
        assertEquals(107.5, summary.e1rmKg!!, 1e-9)
        assertNull(summary.changeKg)
        assertNull(state.lifts.first { it.lift == MainLift.BENCH }.e1rmKg)
    }

    @Test
    fun totalChange_needsAllThreeLiftsInBothWindows() {
        val before = today.minus(35, DateTimeUnit.DAY)
        val now = today.minus(2, DateTimeUnit.DAY)
        val full = history(
            before to listOf(single(squat, 180.0), single(bench, 120.0), single(deadlift, 240.0)),
            now to listOf(single(squat, 185.0), single(bench, 120.0), single(deadlift, 247.5)),
        )
        val noPriorDeadlift = history(
            before to listOf(single(squat, 180.0), single(bench, 120.0)),
            now to listOf(single(squat, 185.0), single(bench, 120.0), single(deadlift, 247.5)),
        )

        assertEquals(552.5, build(full).totalKg!!, 1e-9)
        assertEquals(12.5, build(full).totalChangeKg!!, 1e-9)
        assertEquals(552.5, build(noPriorDeadlift).totalKg!!, 1e-9)
        assertNull(build(noPriorDeadlift).totalChangeKg)
    }

    @Test
    fun totalTrend_onlyWeeksWhereAllThreeLiftsHavePoints() {
        val state = build(
            history(
                thisWeek.minus(2, DateTimeUnit.WEEK) to listOf(single(squat, 180.0), single(bench, 120.0), single(deadlift, 240.0)),
                thisWeek.minus(1, DateTimeUnit.WEEK) to listOf(single(squat, 182.5), single(bench, 120.0)),
                thisWeek to listOf(single(squat, 185.0), single(bench, 122.5), single(deadlift, 245.0)),
            ),
        )

        assertEquals(listOf(thisWeek.minus(2, DateTimeUnit.WEEK) to 540.0, thisWeek to 552.5), state.totalTrend)
    }

    @Test
    fun hasData_needsACompletedMainLiftSet() {
        assertFalse(build(history()).hasData)
        assertFalse(build(history(today to listOf(single(squat, 100.0, failed = true)))).hasData)
        assertTrue(build(history(today to listOf(single(squat, 100.0, failed = true), single(squat, 90.0)))).hasData)
    }
}
