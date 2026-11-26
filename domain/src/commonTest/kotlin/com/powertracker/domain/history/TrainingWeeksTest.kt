package com.powertracker.domain.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class TrainingWeeksTest {
    // Wednesday 11 November 2026; its week is Monday 9 to Sunday 15.
    private val today = LocalDate(2026, 11, 11)
    private fun days(vararg iso: String) = iso.map { LocalDate.parse(it) }

    @Test
    fun noWorkouts_isZero() {
        assertEquals(0, weeksInARow(emptyList(), today))
    }

    @Test
    fun thisWeekAndTheTwoBefore_isThree_howeverManyWorkoutsAWeekHas() {
        assertEquals(3, weeksInARow(days("2026-10-26", "2026-10-28", "2026-11-01", "2026-11-03", "2026-11-09", "2026-11-11"), today))
        // Order does not matter, nor a day listed twice.
        assertEquals(3, weeksInARow(days("2026-11-11", "2026-10-28", "2026-11-03", "2026-11-03"), today))
    }

    @Test
    fun thisWeekEmptySoFar_doesNotBreakTheRun_itIsCountedBackFromLastWeek() {
        assertEquals(2, weeksInARow(days("2026-10-27", "2026-11-05"), today))
        // On the Monday of a new week the run of the weeks before still stands.
        assertEquals(2, weeksInARow(days("2026-10-27", "2026-11-05"), LocalDate(2026, 11, 9)))
    }

    @Test
    fun thisWeekAndLastBothEmpty_isZero_whateverCameBefore() {
        assertEquals(0, weeksInARow(days("2026-10-20", "2026-10-27"), today))
    }

    @Test
    fun aMissedWeek_endsTheRun() {
        // This week and last, then nothing in the week of 26 October, then three weeks before it.
        assertEquals(2, weeksInARow(days("2026-10-06", "2026-10-13", "2026-10-20", "2026-11-04", "2026-11-10"), today))
    }

    @Test
    fun aWeekIsMondayToSunday() {
        // Sunday 8 November is last week; Monday 9 is this week.
        assertEquals(1, weeksInARow(days("2026-11-08"), today))
        assertEquals(1, weeksInARow(days("2026-11-09"), today))
        assertEquals(2, weeksInARow(days("2026-11-08", "2026-11-09"), today))
        // Sunday of the week before last, and nothing since: no run.
        assertEquals(0, weeksInARow(days("2026-11-01"), today))
    }

    @Test
    fun aRunAcrossNewYear_isCounted() {
        // Thursday 7 January 2027: its week starts Monday 4 January; the week before starts 28 December 2026.
        val january = LocalDate(2027, 1, 7)
        assertEquals(4, weeksInARow(days("2026-12-15", "2026-12-23", "2026-12-31", "2027-01-05"), january))
        // Friday 1 January is in the week that starts in December.
        assertEquals(2, weeksInARow(days("2026-12-22", "2027-01-01"), LocalDate(2027, 1, 3)))
    }

    @Test
    fun aDayAfterToday_isNotCounted() {
        assertEquals(0, weeksInARow(days("2026-11-13"), today))
        assertEquals(1, weeksInARow(days("2026-11-04", "2026-11-20"), today))
    }
}
