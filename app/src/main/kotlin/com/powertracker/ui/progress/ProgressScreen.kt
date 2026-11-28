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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
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
import com.powertracker.data.SettingsRepository
import com.powertracker.data.TrainingRepository
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.TrainingMode
import com.powertracker.domain.progress.ExerciseTrend
import com.powertracker.domain.stack.PlateStack
import com.powertracker.domain.progress.MuscleRow
import com.powertracker.domain.model.MuscleRating
import com.powertracker.ui.components.SecondaryButton
import androidx.compose.ui.platform.testTag
import com.powertracker.platform.systemToday
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
import com.powertracker.ui.components.LINE_DRAW_MS
import com.powertracker.ui.components.MARKER_SLIDE_MS
import com.powertracker.ui.components.rememberBarsGrowIn
import com.powertracker.ui.components.rememberDrawIn
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong
import com.powertracker.ui.components.LineChart as SeriesChart
import com.powertracker.ui.components.SoftDivider
import com.powertracker.ui.components.pressClickable

/** Where the lifter stands: total and lift e1RMs, their trend, records, weekly volume, ratios and bodyweight. */
@Composable
fun ProgressScreen(
    training: TrainingRepository,
    settings: SettingsRepository,
    onOpenLift: (MainLift) -> Unit,
    today: () -> LocalDate = ::systemToday,
    onOpenExercise: (exerciseId: Long) -> Unit = {},
    onCheckIn: () -> Unit = {},
    /** Opens every past workout (1.20), from the History button at the top right on both sides. */
    onOpenHistory: (() -> Unit)? = null,
) {
    val viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.factory(training, settings, today))
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProgressContent(state, onRange = viewModel::setRange, onOpenLift = onOpenLift, onOpenExercise = onOpenExercise, onCheckIn = onCheckIn, onOpenHistory = onOpenHistory)
}

/** The Progress screen without its view model. */
@Composable
fun ProgressContent(
    state: ProgressState,
    onRange: (ChartRange) -> Unit,
    onOpenLift: (MainLift) -> Unit,
    onOpenExercise: (exerciseId: Long) -> Unit = {},
    onCheckIn: () -> Unit = {},
    /**
     * The History button at the top right (1.20), the same on both sides and there whether or not this side has
     * anything logged: the list it opens is every workout, from both sides. Null (the guide's picture of Progress)
     * leaves the button out.
     */
    onOpenHistory: (() -> Unit)? = null,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val eyebrow = if (state.mode == TrainingMode.BODYBUILDING) "Best set per session" else "Estimated 1RM"
            ScreenHeader("Progress", eyebrow = eyebrow, modeLabel = true) {
                // Lined up with the cards' right edge, as Today's Finish is.
                if (onOpenHistory != null) SecondaryButton("History", onOpenHistory, compact = true, modifier = Modifier.padding(end = Space.s))
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = Space.gutter, end = Space.gutter, bottom = Space.xl),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            when {
                state.loading -> Unit
                // The powerlifting charts, records and total belong to that side (bodybuilding mode spec §7.6).
                state.mode == TrainingMode.BODYBUILDING -> {
                    // Stage 3: a check-in any time, then the body parts above the stage 1 exercise rows. With no plan
                    // (no exercise rows) there is nothing a check-in could add to, so it is not offered.
                    if (state.trends.isNotEmpty()) CheckInStart(state.checkedIn, onCheckIn)
                    if (state.muscles.isNotEmpty()) BodyPartsSection(state.muscles)
                    if (state.trends.isEmpty()) {
                        EmptyState(
                            Icons.Filled.Insights,
                            "No exercises in your plan yet",
                            "Add exercises on the Plan tab and log them on Today. Each one's best sets will show here.",
                        )
                    } else {
                        if (state.sideBalances.isNotEmpty()) SideBalanceSection(state.sideBalances)
                        Column {
                            SectionHeader("Exercises")
                            BodybuildingSection(state, onOpenExercise)
                        }
                    }
                }
                !state.hasData -> EmptyState(
                    Icons.Filled.Insights,
                    "No lifts logged yet",
                    if (state.showStrictPress) {
                        "Log a squat, bench, deadlift or strict press session on Today and your charts will appear here."
                    } else {
                        "Log a squat, bench or deadlift session on Today and your charts will appear here."
                    },
                )
                else -> {
                    SummaryCard(state, onOpenLift)
                    TrendCard(state, onRange)
                    if (state.prs.isNotEmpty()) PrSection(state.prs)
                    VolumeSection(state)
                    if (state.ratios.isNotEmpty()) RatioSection(state.ratios)
                    if (state.sideBalances.isNotEmpty()) SideBalanceSection(state.sideBalances)
                    if (state.bodyweight.isNotEmpty()) BodyweightSection(state.bodyweight, state.range)
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
        MainLift.STRICT_PRESS -> "Strict press"
    }

@Composable
private fun MainLift.chartColor(): Color {
    val pt = PowertrackerTheme.colors
    return when (this) {
        MainLift.SQUAT -> pt.squat
        MainLift.BENCH -> pt.bench
        MainLift.DEADLIFT -> pt.deadlift
        MainLift.STRICT_PRESS -> pt.press
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

/** Four tiles do not fit side by side on a small phone: from the fourth lift on they go two to a row. */
private const val TILES_IN_ONE_ROW = 3

/** The total as the hero figure with its trend, then each lift as a tile that opens its history. */
@Composable
private fun SummaryCard(state: ProgressState, onOpenLift: (MainLift) -> Unit) {
    val pt = PowertrackerTheme.colors
    val scheme = MaterialTheme.colorScheme
    // The total always says what it adds up.
    val totalLabel = if (state.showStrictPress) "Total (S+B+D+P)" else "Total (S+B+D)"
    PtCard(spacing = Space.l) {
        if (state.totalKg != null) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    NumberDisplay(formatKg(state.totalKg), unit = "kg", label = totalLabel, size = NumberSize.HERO)
                    val change = state.totalChangeKg?.let(::Change)
                    if (change != null) Pill("${change.text} in 4 weeks", tone = change.tone, icon = change.icon)
                }
                if (state.totalTrend.size >= 2) {
                    // The trend covers the chosen range, so it draws in again when the range changes.
                    val drawn = rememberDrawIn(state.range, LINE_DRAW_MS)
                    Sparkline(state.totalTrend.map { it.second.toFloat() }, pt.accentText, Modifier.weight(1f).height(88.dp), drawn = drawn::value)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Eyebrow(totalLabel)
                Text(
                    if (state.showStrictPress) "Log all four lifts, strict press included, to see your total." else "Log all three lifts to see your total.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            for (row in state.lifts.chunked(if (state.lifts.size > TILES_IN_ONE_ROW) 2 else TILES_IN_ONE_ROW)) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    for (summary in row) {
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
            }
            Text(
                "Change over the last 4 weeks. Tap a lift for its history.",
                style = MaterialTheme.typography.bodySmall,
                color = pt.muted,
            )
        }
    }
}

/** The shown lifts on one axis over the chosen range. */
@Composable
private fun TrendCard(state: ProgressState, onRange: (ChartRange) -> Unit) {
    PtCard {
        val series = MainLift.entries
            .filter { lift -> state.e1rm[lift].orEmpty().any { it != null } }
            .map { lift -> ChartSeries(lift.label, lift.chartColor(), state.e1rm.getValue(lift)) }
        SegmentedChoice(ChartRange.entries.map { it.label }, state.range.ordinal, { onRange(ChartRange.entries[it]) })
        Eyebrow("Weekly best e1RM")
        if (series.isNotEmpty()) {
            val drawn = rememberDrawIn(state.range, LINE_DRAW_MS)
            SeriesChart(series, axisLabels(state.weeks), drawn = drawn::value)
        } else {
            // The range selector stays, so a longer range is one tap away.
            Text(
                "No sessions in this range. Try a longer one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val PR_DATE = DateTimeFormatter.ofPattern("d MMM")
private val PR_DATE_WITH_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
private fun PrSection(prs: List<PrUi>) {
    val pt = PowertrackerTheme.colors
    val thisYear = prs.maxOf { it.pr.date }.year
    Column {
        SectionHeader("Records")
        PtCard(contentPadding = PaddingValues(horizontal = Space.l, vertical = Space.xs), spacing = 0.dp) {
            prs.forEachIndexed { i, (lift, pr) ->
                if (i > 0) SoftDivider()
                val date = (if (pr.date.year == thisYear) PR_DATE else PR_DATE_WITH_YEAR).format(pr.date.toJavaLocalDate())
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

/** Wide enough for "Strict press · this week" on one line, and no wider: the bars keep room for the two captions under them. */
private val VOLUME_LABEL_WIDTH_WITH_PRESS = 136.dp

/** "12,450": tonnage has no use for decimals. */
private fun formatVolume(kg: Double): String = String.format(Locale.US, "%,d", kg.roundToLong())

/** One strip of eight weekly bars per lift, each on its own scale, with this week's figure beside it. */
@Composable
private fun VolumeSection(state: ProgressState) {
    val pt = PowertrackerTheme.colors
    val scheme = MaterialTheme.colorScheme
    val labelWidth = if (state.showStrictPress) VOLUME_LABEL_WIDTH_WITH_PRESS else VOLUME_LABEL_WIDTH
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
                    Column(Modifier.width(labelWidth)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(8.dp).background(volume.lift.chartColor(), CircleShape))
                            Text("$name · this week", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1)
                        }
                        NumberDisplay(formatVolume(volume.thisWeekKg), unit = "kg", size = NumberSize.MEDIUM)
                    }
                    // The last 8 weeks whatever the range: they grow in once, when Progress opens.
                    val grown = rememberBarsGrowIn(Unit)
                    BarStrip(volume.weeks, volume.lift.chartColor(), Modifier.weight(1f).height(48.dp), grown = grown::value)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    Spacer(Modifier.width(labelWidth))
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(state.volumeFrom?.let { "Week of ${PR_DATE.format(it.toJavaLocalDate())}" }.orEmpty(), style = MaterialTheme.typography.bodySmall, color = pt.muted)
                        Text("This week", style = MaterialTheme.typography.bodySmall, color = pt.muted)
                    }
                }
                Text("Weight × reps of completed sets, last 8 weeks.", style = MaterialTheme.typography.bodySmall, color = pt.muted)
            }
        }
    }
}

/**
 * "26 × 11 · 26 × 10": the best sets as the list shows them, or, with [spoken], as a screen reader says them ("26 × 11,
 * 26 × 10"). A plate-counted machine's read "plate 7½ × 10", spoken "plate 7 and a half × 10" (1.17).
 */
private fun bestSetText(trend: ExerciseTrend, spoken: Boolean = false): String =
    trend.bestSets.joinToString(if (spoken) ", " else " · ") { (_, set) ->
        val weight = when {
            set.unit != LoadUnit.PLATES -> formatKg(set.weightKg)
            spoken -> PlateStack.spoken(set.weightKg)
            else -> PlateStack.text(set.weightKg)
        }
        "$weight × ${set.reps}"
    }

/**
 * The bodybuilding side's Progress: each exercise in its plan with the best set of each of its last four sessions
 * (oldest first) and whether it is going up. Every row opens that exercise's history, which has its sets from both
 * sides; a main lift's own history holds only powerlifting sessions, so it is never opened from here.
 */
@Composable
private fun BodybuildingSection(state: ProgressState, onOpenExercise: (Long) -> Unit) {
    val pt = PowertrackerTheme.colors
    PtCard(contentPadding = PaddingValues(horizontal = Space.l, vertical = Space.xs), spacing = 0.dp) {
            state.trends.forEachIndexed { i, trend ->
                if (i > 0) SoftDivider()
                val sets = if (trend.bestSets.isEmpty()) "Not logged yet" else bestSetText(trend)
                // Read as the web reads it, "DB press. 26 × 10, 26 × 11. Going up" (a list, not "dot"), then the click
                // label "Open history". The trend icons are left silent, since their words are already in it.
                val trendWords = when (trend.goingUp) { true -> "Going up"; false -> "Not going up"; null -> null }
                val spoken = listOfNotNull(trend.name, if (trend.bestSets.isEmpty()) sets else bestSetText(trend, spoken = true), trendWords).joinToString(". ")
                Row(
                    Modifier.fillMaxWidth()
                        .pressClickable(onClickLabel = "Open history") { onOpenExercise(trend.exerciseId) }
                        .semantics { contentDescription = spoken }
                        .heightIn(min = 56.dp).padding(vertical = Space.s),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(trend.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(sets, style = MaterialTheme.typography.bodyMedium, color = pt.muted)
                    }
                    when (trend.goingUp) {
                        true -> Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = pt.success, modifier = Modifier.size(24.dp))
                        false -> Icon(Icons.AutoMirrored.Filled.TrendingFlat, contentDescription = null, tint = pt.muted, modifier = Modifier.size(24.dp))
                        null -> Unit
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = pt.muted, modifier = Modifier.size(20.dp))
                }
            }
    }
}

// Stage 3's words on Progress. The web app's Progress uses the same.
const val FIRST_CHECK_IN_LINE = "Your first check-in is four weeks after your first bodybuilding session. You can check in any time."

fun ratingText(rating: MuscleRating): String = when (rating) {
    MuscleRating.LAGGING -> "Lagging"
    MuscleRating.GROWING_WELL -> "Growing well"
    MuscleRating.ON_TRACK -> "On track"
}

/** "11 of 14 planned sets in the last 7 days"; with none planned, "3 sets in the last 7 days". */
fun setsText(row: MuscleRow): String =
    if (row.setsPlanned == 0) "${row.setsDone} ${if (row.setsDone == 1) "set" else "sets"} in the last 7 days"
    else "${row.setsDone} of ${row.setsPlanned} planned sets in the last 7 days"

/** "2 of 3 exercises going up"; null when none of the body part's exercises has enough sessions to tell. */
fun goingUpText(row: MuscleRow): String? =
    if (row.exercisesTracked == 0) null
    else "${row.exercisesUp} of ${row.exercisesTracked} ${if (row.exercisesTracked == 1) "exercise" else "exercises"} going up"

/** The Check in button, with a line before the first check-in saying when one falls due. */
@Composable
private fun CheckInStart(checkedIn: Boolean, onCheckIn: () -> Unit) {
    // Clear of the title, as the first card on the other screens sits below their headers.
    Column(Modifier.padding(top = Space.m), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        SecondaryButton("Check in", onCheckIn, Modifier.fillMaxWidth().testTag("progress-check-in"))
        if (!checkedIn) {
            Text(FIRST_CHECK_IN_LINE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Space.xs))
        }
    }
}

/** One row per body part: its latest rating, the sets done against the plan, how its exercises move, earlier ratings. */
@Composable
private fun BodyPartsSection(rows: List<MuscleRow>) {
    val pt = PowertrackerTheme.colors
    Column {
        SectionHeader("Body parts")
        PtCard(contentPadding = PaddingValues(horizontal = Space.l, vertical = Space.xs), spacing = 0.dp) {
            rows.forEachIndexed { i, row ->
                if (i > 0) SoftDivider()
                val latest = row.ratings.lastOrNull()?.second
                val earlier = row.ratings.dropLast(1).map { ratingText(it.second) }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = Space.m).semantics(mergeDescendants = true) {}.testTag("body-part-${row.group.name}"),
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
                        Text(row.group.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        val target = if (row.setsPlanned > 0) row.setsPlanned else 12
                        val ratio = if (target > 0) row.setsDone.toFloat() / target else 0f
                        val heatColor = when {
                            row.setsDone == 0 || ratio < 0.45f -> pt.muted.copy(alpha = 0.65f)
                            ratio < 0.9f -> pt.accentText
                            ratio <= 1.25f -> pt.success
                            else -> pt.warning
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .height(6.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest, androidx.compose.foundation.shape.CircleShape),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .background(heatColor, androidx.compose.foundation.shape.CircleShape),
                            )
                        }
                        Text(setsText(row), style = MaterialTheme.typography.bodyMedium, color = pt.muted)
                        goingUpText(row)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = pt.muted) }
                        if (earlier.isNotEmpty()) {
                            Text("Earlier: " + earlier.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = pt.muted)
                        }
                    }
                    if (latest != null) Pill(ratingText(latest), tone = latest.tone())
                }
            }
        }
    }
}

@Composable
private fun SideBalanceSection(rows: List<com.powertracker.domain.unilateral.SideBalanceRow>) {
    val pt = PowertrackerTheme.colors
    Column {
        SectionHeader("Side balance (L / R)")
        PtCard(contentPadding = PaddingValues(horizontal = Space.l, vertical = Space.xs), spacing = 0.dp) {
            rows.forEachIndexed { i, row ->
                if (i > 0) SoftDivider()
                Row(
                    Modifier.fillMaxWidth().padding(vertical = Space.m).semantics(mergeDescendants = true) {},
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
                        Text(row.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(row.summary, style = MaterialTheme.typography.bodyMedium, color = pt.muted)
                    }
                    val weaker = row.weakerSide
                    if (weaker == null) {
                        Pill("Balanced", tone = Tone.SUCCESS)
                    } else {
                        val sideName = if (weaker == com.powertracker.domain.model.Side.LEFT) "Left" else "Right"
                        Pill("$sideName -${kotlin.math.abs(row.avgRepGap)}", tone = Tone.WARNING)
                    }
                }
            }
        }
    }
}

private fun MuscleRating.tone(): Tone = when (this) {
    MuscleRating.LAGGING -> Tone.WARNING
    MuscleRating.GROWING_WELL -> Tone.SUCCESS
    MuscleRating.ON_TRACK -> Tone.NEUTRAL
}

@Composable
private fun RatioSection(ratios: List<RatioUi>) {
    Column {
        SectionHeader("Lift ratios")
        PtCard(spacing = Space.l) {
            for (ratio in ratios) {
                val slid = rememberDrawIn(Unit, MARKER_SLIDE_MS)
                RatioBar("${ratio.numerator.label} ÷ ${ratio.denominator.label}", ratio.valuePct, ratio.lowPct, ratio.highPct, slid = slid::value)
            }
        }
    }
}

@Composable
private fun BodyweightSection(points: List<Pair<LocalDate, Double>>, range: ChartRange) {
    Column {
        SectionHeader("Bodyweight")
        // Its points are those in the chosen range, so it draws in again when the range changes.
        val drawn = rememberDrawIn(range, LINE_DRAW_MS)
        PtCard { LineChart(points, name = "Latest", drawn = drawn::value) }
    }
}
