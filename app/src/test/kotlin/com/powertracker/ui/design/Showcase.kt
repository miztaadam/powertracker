package com.powertracker.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.powertracker.data.AppSettings
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.Exercise
import com.powertracker.domain.model.PlanItem
import com.powertracker.ui.PtBottomBar
import com.powertracker.ui.Tab
import com.powertracker.ui.components.AdviceCard
import com.powertracker.ui.components.ChartSeries
import com.powertracker.ui.components.CoachSuggestion
import com.powertracker.ui.components.Eyebrow
import com.powertracker.ui.components.LineChart
import com.powertracker.ui.components.NumberDisplay
import com.powertracker.ui.components.NumberSize
import com.powertracker.ui.components.Pill
import com.powertracker.ui.components.PlateStrip
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.PtCard
import com.powertracker.ui.components.RatioBar
import com.powertracker.ui.components.RestTimerBar
import com.powertracker.ui.components.ScreenHeader
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.components.SectionHeader
import com.powertracker.ui.components.SegmentedChoice
import com.powertracker.ui.components.SetRow
import com.powertracker.ui.components.SetState
import com.powertracker.ui.components.Sparkline
import com.powertracker.ui.components.StatTile
import com.powertracker.ui.components.StepperField
import com.powertracker.ui.plan.DaySection
import com.powertracker.ui.plan.PlanContent
import com.powertracker.ui.plan.PlanRow
import com.powertracker.ui.plan.PlanUiState
import com.powertracker.ui.plan.SettingsContent
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone
import java.time.DayOfWeek

// Mockups for owner approval: real components, hard-coded sample data. One lifter, one story:
// squat e1RM 190, bench 122.5 (stalled), deadlift 250, on Wednesday 11 November.

private val SQUAT = listOf(172.5f, 175f, 175f, 177.5f, 180f, 180f, 182.5f, 185f, 185f, 187.5f, 187.5f, 190f)
private val BENCH = listOf(115f, 117.5f, 119f, 120f, 121f, 122.5f, 122f, 122.5f, 122.5f, 122f, 122.5f, 122.5f)
private val DEADLIFT = listOf(232.5f, 235f, 237.5f, 237.5f, 240f, 242.5f, 242.5f, 245f, 247.5f, 247.5f, 250f, 250f)

/** Theme, background, a stand-in status bar, and the real bottom bar when [tab] is given. */
@Composable
fun ShowcaseFrame(dark: Boolean, tab: Tab?, content: @Composable () -> Unit) {
    PowertrackerTheme(darkTheme = dark) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("9:41", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
            }
            Box(Modifier.weight(1f)) { content() }
            if (tab != null) PtBottomBar(tab.route) {}
        }
    }
}

@Composable
fun TodayShowcase() {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Today", eyebrow = "Wednesday 11 November") {
            SecondaryButton("Finish", {}, compact = true, modifier = Modifier.padding(end = Space.s))
        }
        Column(Modifier.weight(1f).padding(horizontal = Space.gutter), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            PtCard(spacing = Space.s) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow("Exercise 1 of 4")
                        Text("Squat", style = MaterialTheme.typography.headlineMedium)
                    }
                    Pill("Set 4 of 5")
                }
                CoachSuggestion("155 × 5 @ 8", "Last time you hit 150 × 5 @ 7.", {}, {})
                Column {
                    SetRow(1, 155.0, 5, 8.0, SetState.DONE, isTopSet = true)
                    SetRow(2, 155.0, 5, 8.5, SetState.DONE)
                    SetRow(3, 155.0, 3, 10.0, SetState.FAILED)
                    Row(Modifier.padding(start = 32.dp, top = Space.xs, bottom = Space.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                        Text("Where did it stall?", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        SegmentedChoice(listOf("Floor", "Mid", "Lockout"), 0, {})
                    }
                }
                HorizontalDivider(color = PowertrackerTheme.colors.hairline)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    StepperField("Weight", "155", true, true, {}, {}, Modifier.weight(1.25f), unit = "kg")
                    StepperField("Reps", "5", true, true, {}, {}, Modifier.weight(1f))
                    StepperField("RPE", "8", true, true, {}, {}, Modifier.weight(1f))
                }
                PlateStrip(listOf(25.0, 25.0, 15.0, 2.5))
                PrimaryButton("Log set 4", {}, Modifier.fillMaxWidth(), icon = Icons.Filled.Check)
            }
        }
        RestTimerBar(108, 180, {}, {}, Modifier.padding(horizontal = Space.gutter).padding(bottom = Space.s))
    }
}

@Composable
private fun AccessoryLine(name: String, dose: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(dose, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun CoachShowcase() {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Coach", eyebrow = "3 open suggestions")
        Column(Modifier.padding(horizontal = Space.gutter).padding(bottom = Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            AdviceCard(
                tone = Tone.WARNING, icon = Icons.AutoMirrored.Filled.TrendingFlat, tag = "Stall · Bench",
                title = "Bench has stalled",
                body = "Your best weekly e1RM has been flat for 3 weeks, and your misses are at lockout. Add triceps work:",
                primaryAction = "Add to plan", secondaryAction = "Dismiss",
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    NumberDisplay("122.5", unit = "kg", label = "Bench e1RM", size = NumberSize.MEDIUM)
                    Sparkline(BENCH, pt.bench, Modifier.weight(1f).height(48.dp))
                }
                Column(Modifier.background(scheme.surfaceContainer, MaterialTheme.shapes.medium).padding(horizontal = Space.m, vertical = Space.xs)) {
                    AccessoryLine("Close-grip bench", "3 × 3–5")
                    HorizontalDivider(color = pt.hairline)
                    AccessoryLine("JM press", "3 × 8–10")
                    HorizontalDivider(color = pt.hairline)
                    AccessoryLine("Board press", "3 × 3–5")
                }
            }
            AdviceCard(
                tone = Tone.DANGER, icon = Icons.Filled.BatteryAlert, tag = "Deload · All lifts",
                title = "Take a deload week",
                body = "Two fatigue signs in the last 2 weeks: RPE is up a full point at the same load on squat and deadlift, and 3 reps failed on main lifts.",
                primaryAction = "Apply deload", secondaryAction = "Dismiss",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    StatTile("Volume", "60", Modifier.weight(1f), unit = "%", note = "of your usual sets")
                    StatTile("Load", "90", Modifier.weight(1f), unit = "%", note = "of your last top sets")
                }
            }
            AdviceCard(
                tone = Tone.INFO, icon = Icons.Filled.Balance, tag = "Weak point · Squat",
                title = "Squat is lagging your deadlift",
                body = "It has been below the typical range for 4 weeks. Give squat priority before it stalls.",
                primaryAction = "Prioritise squat", secondaryAction = "Dismiss",
            ) {
                RatioBar("Squat ÷ Deadlift", 76, 80, 90)
            }
        }
    }
}

@Composable
private fun PrLine(lift: String, detail: String, e1rm: String) {
    val pt = PowertrackerTheme.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        Box(Modifier.size(36.dp).background(pt.successContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = pt.success, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(lift, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = pt.muted)
        }
        NumberDisplay(e1rm, unit = "kg", size = NumberSize.MEDIUM)
    }
}

@Composable
fun ProgressShowcase() {
    val pt = PowertrackerTheme.colors
    val total = SQUAT.indices.map { SQUAT[it] + BENCH[it] + DEADLIFT[it] }
    val weeks = listOf("25 Aug", "", "", "", "", "29 Sep", "", "", "", "", "", "10 Nov")
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Progress", eyebrow = "Estimated 1RM")
        Column(Modifier.padding(horizontal = Space.gutter).padding(bottom = Space.l), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            PtCard {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        NumberDisplay("562.5", unit = "kg", label = "Total", size = NumberSize.HERO)
                        Pill("+42.5 kg in 12 weeks", tone = Tone.SUCCESS, icon = Icons.AutoMirrored.Filled.TrendingUp)
                    }
                    Sparkline(total, pt.accentText, Modifier.weight(1f).height(88.dp))
                }
            }
            PtCard {
                SegmentedChoice(listOf("4 wk", "12 wk", "1 yr", "All"), 1, {})
                LineChart(
                    listOf(ChartSeries("Squat", pt.squat, SQUAT), ChartSeries("Bench", pt.bench, BENCH), ChartSeries("Deadlift", pt.deadlift, DEADLIFT)),
                    weeks,
                )
            }
            Column {
                SectionHeader("Recent PRs")
                PtCard(contentPadding = PaddingValues(horizontal = Space.l, vertical = Space.xs), spacing = 0.dp) {
                    PrLine("Deadlift", "6 Nov · 225 × 3 @ 8.5", "250")
                    HorizontalDivider(color = pt.hairline)
                    PrLine("Squat", "4 Nov · 150 × 5 @ 7", "190")
                    HorizontalDivider(color = pt.hairline)
                    PrLine("Bench", "14 Oct · 105 × 5 @ 9", "122.5")
                }
            }
            Column {
                SectionHeader("Lift ratios")
                PtCard(spacing = Space.l) {
                    RatioBar("Squat ÷ Deadlift", 76, 80, 90)
                    RatioBar("Bench ÷ Squat", 64, 60, 70)
                }
            }
        }
    }
}

private fun exercise(id: Long, name: String, category: Category = Category.ACCESSORY) = Exercise(id, name, category, null, emptyList(), isBuiltIn = true)

private fun day(day: DayOfWeek, vararg rows: Triple<String, Pair<Int, Int>, Double>) = DaySection(
    day,
    rows.mapIndexed { i, (name, dose, rpe) -> PlanRow(PlanItem(day.value * 10L + i, day, i.toLong(), dose.first, dose.second, rpe, i), name) },
)

@Composable
fun PlanShowcase() {
    val state = PlanUiState(
        sections = listOf(
            day(DayOfWeek.MONDAY, Triple("Squat", 5 to 5, 8.0), Triple("Bench", 4 to 6, 7.5), Triple("Barbell row", 3 to 8, 8.0)),
            day(DayOfWeek.WEDNESDAY, Triple("Deadlift", 3 to 3, 8.0), Triple("Close-grip bench", 3 to 5, 8.0)),
            day(DayOfWeek.FRIDAY, Triple("Bench", 3 to 5, 8.0), Triple("Pause squat", 3 to 4, 7.5)),
        ),
        mainExercises = listOf(exercise(1, "Squat", Category.SQUAT), exercise(2, "Bench", Category.BENCH), exercise(3, "Deadlift", Category.DEADLIFT)),
    )
    PlanContent(state, onOpenSettings = {}, onAdd = {}, onEdit = {}, onMove = { _, _ -> }, onDelete = {}, onSetBaseline = {})
}

@Composable
fun SettingsShowcase() {
    SettingsContent(AppSettings(changePlates = true), onUpdate = {}, onExport = {}, onRestore = {}, onBack = {})
}
