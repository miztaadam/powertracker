package com.powertracker.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.MainLift
import com.powertracker.ui.coach.RatioUi
import com.powertracker.ui.components.BarStrip
import com.powertracker.ui.components.ChartSeries
import com.powertracker.ui.components.EmptyState
import com.powertracker.ui.components.Eyebrow
import com.powertracker.ui.components.NumberDisplay
import com.powertracker.ui.components.NumberSize
import com.powertracker.ui.components.Pill
import com.powertracker.ui.components.PtCard
import com.powertracker.ui.components.RatioBar
import com.powertracker.ui.components.ScreenHeader
import com.powertracker.ui.components.SectionHeader
import com.powertracker.ui.components.SegmentedChoice
import com.powertracker.ui.components.Sparkline
import com.powertracker.ui.components.StatTile
import com.powertracker.ui.components.formatKg
import com.powertracker.ui.components.formatRpe
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong
import com.powertracker.ui.components.LineChart as SeriesChart

/** Where the lifter stands: total and lift e1RMs, their trend, records, weekly volume, ratios and bodyweight. */
@Composable
fun ProgressScreen(
    training: TrainingRepository,
    onOpenLift: (MainLift) -> Unit,
    today: () -> LocalDate = LocalDate::now,
) {
    val viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.factory(training, today))
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProgressContent(state, onRange = viewModel::setRange, onOpenLift = onOpenLift)
}

/** The Progress screen without its view model. */
@Composable
fun ProgressContent(state: ProgressState, onRange: (ChartRange) -> Unit, onOpenLift: (MainLift) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenHeader("Progress", eyebrow = "Estimated 1RM") },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = Space.gutter, end = Space.gutter, bottom = Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            when {
                state.loading -> Unit
                !state.hasData -> EmptyState(
                    Icons.Filled.Insights,
                    "No lifts logged yet",
                    "Log a squat, bench or deadlift session on Today and your charts will appear here.",
                )
                else -> {
                    SummaryCard(state, onOpenLift)
                    TrendCard(state, onRange)
                    if (state.prs.isNotEmpty()) PrSection(state.prs)
                    VolumeSection(state)
                    if (state.ratios.isNotEmpty()) RatioSection(state.ratios)
                    if (state.bodyweight.isNotEmpty()) BodyweightSection(state.bodyweight)
                }
            }
        }
    }
}

internal val MainLift.label: String
    get() = when (this) {
        MainLift.SQUAT -> "Squat"
        MainLift.BENCH -> "Bench"
        MainLift.DEADLIFT -> "Deadlift"
    }

@Composable
private fun MainLift.chartColor(): Color {
    val pt = PowertrackerTheme.colors
    return when (this) {
        MainLift.SQUAT -> pt.squat
        MainLift.BENCH -> pt.bench
        MainLift.DEADLIFT -> pt.deadlift
    }
}

/** "+5 kg", "−2.5 kg" or "No change", with the tone and icon that go with it. */
private class Change(kg: Double) {
    val text = when {
        kg > 0 -> "+${formatKg(kg)} kg"
        kg < 0 -> "−${formatKg(abs(kg))} kg"
        else -> "No change"
    }
    val spoken = when {
        kg > 0 -> "up ${formatKg(kg)} kg"
        kg < 0 -> "down ${formatKg(abs(kg))} kg"
        else -> "no change"
    }
    val tone = when {
        kg > 0 -> Tone.SUCCESS
        kg < 0 -> Tone.WARNING
        else -> Tone.NEUTRAL
    }
    val icon = when {
        kg > 0 -> Icons.AutoMirrored.Filled.TrendingUp
        kg < 0 -> Icons.AutoMirrored.Filled.TrendingDown
        else -> Icons.AutoMirrored.Filled.TrendingFlat
    }
}

/** The total as the hero figure with its trend, then each lift as a tile that opens its history. */
@Composable
private fun SummaryCard(state: ProgressState, onOpenLift: (MainLift) -> Unit) {
    val pt = PowertrackerTheme.colors
    val scheme = MaterialTheme.colorScheme
    PtCard(spacing = Space.l) {
        if (state.totalKg != null) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    NumberDisplay(formatKg(state.totalKg), unit = "kg", label = "Total", size = NumberSize.HERO)
                    val change = state.totalChangeKg?.let(::Change)
                    if (change != null) Pill("${change.text} in 4 weeks", tone = change.tone, icon = change.icon)
                }
                if (state.totalTrend.size >= 2) {
                    Sparkline(state.totalTrend.map { it.second.toFloat() }, pt.accentText, Modifier.weight(1f).height(88.dp))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Eyebrow("Total")
                Text(
                    "Log all three lifts to see your total.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                for (summary in state.lifts) {
                    val change = summary.changeKg?.let(::Change)
                    val name = summary.lift.label
                    val spoken = buildString {
                        append("$name history. ")
                        append(if (summary.e1rmKg != null) "Estimated 1RM ${formatKg(summary.e1rmKg)} kg" else "No estimate yet")
                        if (change != null) append(", ${change.spoken} in 4 weeks")
                    }
                    StatTile(
                        label = name,
                        value = summary.e1rmKg?.let(::formatKg) ?: "–",
                        unit = if (summary.e1rmKg != null) "kg" else null,
                        note = change?.text,
                        noteTone = change?.tone ?: Tone.NEUTRAL,
                        onClick = { onOpenLift(summary.lift) },
                        onClickLabel = "Open history",
                        modifier = Modifier.weight(1f).fillMaxHeight().semantics { contentDescription = spoken },
                    )
                }
            }
            Text(
                "Change over the last 4 weeks. Tap a lift for its history.",
                style = MaterialTheme.typography.bodySmall,
                color = pt.muted,
            )
        }
    }
}

/** The three lifts on one axis over the chosen range. */
@Composable
private fun TrendCard(state: ProgressState, onRange: (ChartRange) -> Unit) {
    PtCard {
        SegmentedChoice(ChartRange.entries.map { it.label }, state.range.ordinal, { onRange(ChartRange.entries[it]) })
        Eyebrow("Weekly best e1RM")
        SeriesChart(
            MainLift.entries
                .filter { lift -> state.e1rm[lift].orEmpty().any { it != null } }
                .map { lift -> ChartSeries(lift.label, lift.chartColor(), state.e1rm.getValue(lift)) },
            axisLabels(state.weeks),
        )
    }
}

private val PR_DATE = DateTimeFormatter.ofPattern("d MMM")
private val PR_DATE_WITH_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
private fun PrSection(prs: List<PrUi>) {
    val pt = PowertrackerTheme.colors
    val thisYear = prs.maxOf { it.pr.date }.year
    Column {
        SectionHeader("Recent PRs")
        PtCard(contentPadding = PaddingValues(horizontal = Space.l, vertical = Space.xs), spacing = 0.dp) {
            prs.forEachIndexed { i, (lift, pr) ->
                if (i > 0) HorizontalDivider(color = pt.hairline)
                val date = (if (pr.date.year == thisYear) PR_DATE else PR_DATE_WITH_YEAR).format(pr.date)
                val set = "${formatKg(pr.weightKg)} × ${pr.reps}" + (pr.rpe?.let { " @ ${formatRpe(it)}" } ?: "")
                val e1rm = formatKg(pr.e1rmKg.toHalfKg())
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .clearAndSetSemantics { text = AnnotatedString("${lift.label} record: estimated 1RM $e1rm kg, from $set on $date") },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    Box(Modifier.size(36.dp).background(pt.successContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = pt.success, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(lift.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("$date · $set", style = MaterialTheme.typography.bodySmall, color = pt.muted)
                    }
                    NumberDisplay(e1rm, unit = "kg", size = NumberSize.MEDIUM)
                }
            }
        }
    }
}

private val VOLUME_LABEL_WIDTH = 128.dp

/** "12,450": tonnage has no use for decimals. */
private fun formatVolume(kg: Double): String = String.format(Locale.US, "%,d", kg.roundToLong())

/** One strip of eight weekly bars per lift, each on its own scale, with this week's figure beside it. */
@Composable
private fun VolumeSection(state: ProgressState) {
    val pt = PowertrackerTheme.colors
    val scheme = MaterialTheme.colorScheme
    Column {
        SectionHeader("Weekly volume")
        PtCard(spacing = Space.l) {
            for (volume in state.volume) {
                val name = volume.lift.label
                Row(
                    Modifier.clearAndSetSemantics { text = AnnotatedString("$name volume this week: ${formatVolume(volume.thisWeekKg)} kg") },
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    Column(Modifier.width(VOLUME_LABEL_WIDTH)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(8.dp).background(volume.lift.chartColor(), CircleShape))
                            Text("$name · this week", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1)
                        }
                        NumberDisplay(formatVolume(volume.thisWeekKg), unit = "kg", size = NumberSize.MEDIUM)
                    }
                    BarStrip(volume.weeks, volume.lift.chartColor(), Modifier.weight(1f).height(48.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    Spacer(Modifier.width(VOLUME_LABEL_WIDTH))
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(state.volumeFrom?.let { "Week of ${PR_DATE.format(it)}" }.orEmpty(), style = MaterialTheme.typography.bodySmall, color = pt.muted)
                        Text("This week", style = MaterialTheme.typography.bodySmall, color = pt.muted)
                    }
                }
                Text("Weight × reps of completed sets, last 8 weeks.", style = MaterialTheme.typography.bodySmall, color = pt.muted)
            }
        }
    }
}

@Composable
private fun RatioSection(ratios: List<RatioUi>) {
    Column {
        SectionHeader("Lift ratios")
        PtCard(spacing = Space.l) {
            for (ratio in ratios) {
                RatioBar("${ratio.numerator.label} ÷ ${ratio.denominator.label}", ratio.valuePct, ratio.lowPct, ratio.highPct)
            }
        }
    }
}

@Composable
private fun BodyweightSection(points: List<Pair<LocalDate, Double>>) {
    Column {
        SectionHeader("Bodyweight")
        PtCard { LineChart(points, name = "Latest") }
    }
}
