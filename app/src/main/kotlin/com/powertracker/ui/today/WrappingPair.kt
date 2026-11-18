package com.powertracker.ui.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import com.powertracker.ui.theme.Space

/**
 * [first] (a name) and [second] (its plan or set count) on one line, [second] at the end, when [first] fits there whole;
 * otherwise [second] goes under [first], at the start, and [first] has the whole width to wrap in. So a name is never
 * cut to make room for what follows it.
 */
@Composable
fun WrappingPair(modifier: Modifier = Modifier, gap: Dp = Space.m, first: @Composable () -> Unit, second: @Composable () -> Unit) {
    Layout(contents = listOf(first, second), modifier = modifier) { (firsts, seconds), constraints ->
        val max = constraints.maxWidth
        val loose = Constraints(maxWidth = max)
        val gapPx = gap.roundToPx()
        val b = seconds.map { it.measure(loose) }
        val bWidth = b.maxOfOrNull { it.width } ?: 0
        val bHeight = b.maxOfOrNull { it.height } ?: 0
        val room = if (bWidth == 0) max else max - bWidth - gapPx
        val oneLine = room > 0 && firsts.all { it.maxIntrinsicWidth(Constraints.Infinity) <= room }
        if (oneLine) {
            val a = firsts.map { it.measure(Constraints(minWidth = room, maxWidth = room)) }
            val height = maxOf(a.maxOfOrNull { it.height } ?: 0, bHeight).coerceAtLeast(constraints.minHeight)
            layout(max, height) {
                a.forEach { it.place(0, ((height - it.height) / 2f).roundToInt()) }
                b.forEach { it.place(max - it.width, ((height - it.height) / 2f).roundToInt()) }
            }
        } else {
            val a = firsts.map { it.measure(Constraints(minWidth = max, maxWidth = max)) }
            val aHeight = a.maxOfOrNull { it.height } ?: 0
            val height = (aHeight + bHeight).coerceAtLeast(constraints.minHeight)
            layout(max, height) {
                a.forEach { it.place(0, 0) }
                b.forEach { it.place(0, aHeight) }
            }
        }
    }
}
