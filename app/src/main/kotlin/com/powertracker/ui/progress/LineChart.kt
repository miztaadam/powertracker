package com.powertracker.ui.progress

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.powertracker.ui.components.ChartSeries
import com.powertracker.ui.components.DrawnComplete
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import com.powertracker.ui.components.LineChart as SeriesChart

private val DAY_LABEL = DateTimeFormatter.ofPattern("d MMM")
private val MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yy")

/**
 * Axis labels for evenly spaced [dates]: the first, the middle and the last, blank elsewhere.
 * Past a year the day stops mattering, so the label is the month and year.
 */
internal fun axisLabels(dates: List<LocalDate>): List<String> {
    if (dates.isEmpty()) return emptyList()
    val format = if (dates.first().daysUntil(dates.last()) > 366) MONTH_LABEL else DAY_LABEL
    val shown = when {
        dates.size >= 5 -> setOf(0, dates.lastIndex / 2, dates.lastIndex)
        else -> setOf(0, dates.lastIndex)
    }
    return dates.mapIndexed { i, date -> if (i in shown) format.format(date.toJavaLocalDate()) else "" }
}

/**
 * One measurement over time (bodyweight), drawn with the design system's chart. The points sit at
 * their dates, so an uneven gap between them reads as one. [name] labels the latest value in the legend.
 * [drawn] is how much of the line is drawn ([com.powertracker.ui.components.rememberDrawIn]).
 */
@Composable
fun LineChart(
    points: List<Pair<LocalDate, Double>>,
    modifier: Modifier = Modifier,
    name: String = "Latest",
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    drawn: () -> Float = DrawnComplete,
) {
    if (points.isEmpty()) return
    val byDate = points.toMap()
    val first = points.minOf { it.first }
    val last = points.maxOf { it.first }
    val days = generateSequence(first) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it <= last }.toList()
    SeriesChart(listOf(ChartSeries(name, color, days.map { byDate[it]?.toFloat() })), axisLabels(days), modifier, drawn = drawn)
}
