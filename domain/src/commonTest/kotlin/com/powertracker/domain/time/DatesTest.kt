package com.powertracker.domain.time

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals

class DatesTest {
    @Test
    fun mondayOfWeek_isTheMondayOnOrBeforeEachWeekday() {
        val monday = LocalDate(2026, 1, 5)
        for (offset in 0..6) {
            val date = monday.plus(offset, DateTimeUnit.DAY)
            assertEquals(DayOfWeek.entries[offset], date.dayOfWeek)
            assertEquals(monday, date.mondayOfWeek(), "$date")
        }
    }

    @Test
    fun mondayOfWeek_crossesAYearBoundary() {
        // Friday 1 January 2027 belongs to the week starting Monday 28 December 2026.
        assertEquals(LocalDate(2026, 12, 28), LocalDate(2027, 1, 1).mondayOfWeek())
        assertEquals(LocalDate(2026, 12, 28), LocalDate(2027, 1, 3).mondayOfWeek())
        assertEquals(LocalDate(2027, 1, 4), LocalDate(2027, 1, 4).mondayOfWeek())
    }

    @Test
    fun mondayOfWeek_onALeapDay() {
        // Tuesday 29 February 2028.
        assertEquals(LocalDate(2028, 2, 28), LocalDate(2028, 2, 29).mondayOfWeek())
        assertEquals(LocalDate(2028, 2, 28), LocalDate(2028, 3, 5).mondayOfWeek())
    }
}
