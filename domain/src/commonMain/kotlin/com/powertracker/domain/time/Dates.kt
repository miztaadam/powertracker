package com.powertracker.domain.time

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

/** The Monday on or before this date: the start of its ISO week. */
fun LocalDate.mondayOfWeek(): LocalDate = minus(dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
