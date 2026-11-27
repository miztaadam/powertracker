package com.powertracker.ui.progress

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.powertracker.domain.history.weeksInARow
import com.powertracker.ui.components.PtIconButton
import com.powertracker.ui.components.pressClickable
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import java.time.DayOfWeek
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** A month as one number, so the next and the one before are +1 and -1 across a new year. */
internal fun monthIndex(date: LocalDate): Int = date.year * 12 + (date.monthNumber - 1)

/** The first day of the month with this [monthIndex]. */
internal fun firstOfMonth(index: Int): LocalDate = LocalDate(index.floorDiv(12), index.mod(12) + 1, 1)

/**
 * The days of the month starting on [first], in rows of seven from Monday to Sunday, with null before the 1st and
 * after the last day. The web app's `monthGrid`.
 */
internal fun monthGrid(first: LocalDate): List<List<LocalDate?>> {
    val days = first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
    val cells = MutableList<LocalDate?>(first.dayOfWeek.isoDayNumber - 1) { null }
    for (d in 1..days) cells += LocalDate(first.year, first.monthNumber, d)
    while (cells.size % 7 != 0) cells += null
    return cells.chunked(7)
}

/** "November 2026". */
internal fun monthTitle(first: LocalDate): String = "${Month.of(first.monthNumber).getDisplayName(TextStyle.FULL, Locale.getDefault())} ${first.year}"

/** "9 workouts in November", and " · 6 weeks in a row" while there is a run of weeks. The web app's words. */
internal fun calendarSummary(inMonth: Int, first: LocalDate, weeks: Int): String {
    val text = "${count(inMonth, "workout")} in ${Month.of(first.monthNumber).getDisplayName(TextStyle.FULL, Locale.getDefault())}"
    return if (weeks > 0) "$text · ${count(weeks, "week")} in a row" else text
}

private val SPOKEN_DAY = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

/**
 * History's calendar (1.22, as the web app's): one month at a time, Monday to Sunday, a dot on every day with a
 * workout and a ring on today. A day with a workout is a button that brings its workout up in the list below; the other
 * days are just numbers. The arrows go back as far as the month of the first workout and forward to this month. Under
 * the grid: how many workouts the month on screen has, and how many weeks in a row had one ([weeksInARow]).
 */
@Composable
fun WorkoutCalendar(workouts: List<Workout>, today: LocalDate, onOpen: (Workout) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    val last = monthIndex(today)
    // The oldest workout's month, and never after this month (a workout dated ahead does not open months to come).
    val firstMonth = remember(workouts, today) { minOf(workouts.minOfOrNull { monthIndex(it.date) } ?: last, last) }
    var month by rememberSaveable { mutableIntStateOf(last) }
    val byDay = remember(workouts) { workouts.associateBy { it.date } }
    val weeks = remember(workouts, today) { weeksInARow(workouts.map { it.date }, today) }
    val first = firstOfMonth(month)
    val inMonth = workouts.count { monthIndex(it.date) == month }

    Column(modifier.testTag("workout-calendar"), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PtIconButton(onClick = { month-- }, enabled = month > firstMonth) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month") }
            // Not a heading: the workouts below are the screen's headings. Said again when the arrows change it.
            Text(
                monthTitle(first),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            )
            PtIconButton(onClick = { month++ }, enabled = month < last) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month") }
        }
        // One letter over each column, for the eye only.
        Row(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
            for (day in DayOfWeek.entries) {
                Text(
                    day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = pt.muted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        for (week in monthGrid(first)) {
            Row(Modifier.fillMaxWidth()) {
                for (day in week) {
                    val workout = day?.let { byDay[it] }
                    val cell = Modifier.weight(1f).heightIn(min = Space.touch)
                    when {
                        day == null -> Box(cell)
                        workout != null -> Column(
                            cell
                                .clip(MaterialTheme.shapes.small)
                                .pressClickable(role = Role.Button) { onOpen(workout) }
                                .testTag("workout-day-$day")
                                .clearAndSetSemantics { contentDescription = "${SPOKEN_DAY.format(day.toJavaLocalDate())}: ${workout.summary}. Show it" },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            DayNumber(day, today, trained = true)
                            Box(Modifier.padding(top = 2.dp).size(6.dp).background(pt.accentText, CircleShape))
                        }
                        // A day with nothing to open is not a control: its number is for the eye, to place the others.
                        else -> Box(cell.testTag("day-$day").clearAndSetSemantics {}, contentAlignment = Alignment.Center) { DayNumber(day, today, trained = false) }
                    }
                }
            }
        }
        Text(
            calendarSummary(inMonth, first, weeks),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Space.m, vertical = Space.xs).testTag("workout-calendar-summary"),
        )
    }
}

/** A day's number: brighter and heavier on a workout day, in a round ring on [today] (at least 30 dp across; it grows with the font). */
@Composable
private fun DayNumber(day: LocalDate, today: LocalDate, trained: Boolean) {
    val ring = if (day == today) Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape).testTag("workout-day-today") else Modifier
    Box(ring.sizeIn(minWidth = 30.dp, minHeight = 30.dp), contentAlignment = Alignment.Center) {
        Text(
            day.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (trained) FontWeight.SemiBold else FontWeight.Normal,
            color = if (trained) MaterialTheme.colorScheme.onSurface else PowertrackerTheme.colors.muted,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
