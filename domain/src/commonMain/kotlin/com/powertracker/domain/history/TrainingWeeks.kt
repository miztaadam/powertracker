package com.powertracker.domain.history

import com.powertracker.domain.time.mondayOfWeek
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * How many weeks in a row (Monday to Sunday) had a workout, counting back from [today]'s week: History's "6 weeks in a
 * row" (1.22). [workoutDays] are the days of the workouts History lists (sessions with a set, either side); days after
 * [today] are not counted. This week counts once it has a workout; while it has none yet it does not break the run, which
 * is then counted back from last week. 0 when neither this week nor last has a workout.
 */
fun weeksInARow(workoutDays: Collection<LocalDate>, today: LocalDate): Int {
    val weeks = workoutDays.filter { it <= today }.mapTo(HashSet()) { it.mondayOfWeek() }
    var week = today.mondayOfWeek()
    if (week !in weeks) week = week.minus(7, DateTimeUnit.DAY)
    var count = 0
    while (week in weeks) {
        count++
        week = week.minus(7, DateTimeUnit.DAY)
    }
    return count
}
