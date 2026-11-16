package com.powertracker.ui.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.powertracker.domain.model.AccessoryPrescription
import com.powertracker.domain.model.AdviceType
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.liftLabel
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.components.AdviceCard
import com.powertracker.ui.components.Eyebrow
import com.powertracker.ui.components.NumberDisplay
import com.powertracker.ui.components.NumberSize
import com.powertracker.ui.components.RatioBar
import com.powertracker.ui.components.Sparkline
import com.powertracker.ui.components.StatTile
import com.powertracker.ui.components.formatKg
import com.powertracker.ui.components.SoftDivider
import com.powertracker.ui.library.InfoButton
import com.powertracker.ui.theme.LocalTrainingMode
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone
import kotlin.math.roundToLong

/** One card of the Coach feed: what kind of advice, what to do, the coach's reason, the figures behind it, Apply and Dismiss. */
@Composable
fun CoachAdviceCard(card: CoachCard, onApply: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val advice = card.advice
    val detail = card.detail
    val lift = advice.lift?.label()
    // A deload or easing back in on the bodybuilding side speaks of exercises and weights, not lifts and top sets.
    val bodybuilding = LocalTrainingMode.current == TrainingMode.BODYBUILDING
    val allExercises = bodybuilding && (advice.type == AdviceType.DELOAD || advice.type == AdviceType.RE_ENTRY)
    val look = when (advice.type) {
        AdviceType.STALL -> Look(Tone.WARNING, Icons.AutoMirrored.Filled.TrendingFlat, "Stall", "${lift ?: "Progress"} has stalled", "Add to plan")
        AdviceType.DELOAD -> Look(Tone.DANGER, Icons.Filled.BatteryAlert, "Deload", "Take a deload week", "Apply deload")
        AdviceType.RE_ENTRY -> Look(Tone.INFO, Icons.Filled.Replay, "Re-entry", "Welcome back", "Ease back in")
        else -> {
            val other = (detail as? CardDetail.WeakPoint)?.ratio?.let { if (it.numerator == advice.lift) it.denominator else it.numerator }
            val title = "${lift ?: "A lift"} is lagging" + if (other != null) " your ${other.label().lowercase()}" else ""
            Look(Tone.INFO, Icons.Filled.Balance, "Weak point", title, "Prioritise ${lift?.lowercase() ?: "it"}")
        }
    }
    AdviceCard(
        tone = look.tone, icon = look.icon, tag = "${look.kind} · ${lift ?: if (allExercises) "All exercises" else "All lifts"}",
        title = look.title, body = advice.reason,
        modifier = modifier,
        primaryAction = look.action, onPrimary = onApply,
        secondaryAction = "Dismiss", onSecondary = onDismiss,
    ) {
        when (detail) {
            is CardDetail.Stall -> {
                if (detail.e1rmKg != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                        NumberDisplay(formatKg(detail.e1rmKg.toHalfKg()), unit = "kg", label = "${lift.orEmpty()} e1RM".trim(), size = NumberSize.MEDIUM)
                        if (detail.trend.size >= 2) {
                            // Scaled to at least 5% of the lift, so a stall reads as the flat line it is.
                            Sparkline(detail.trend, advice.lift.chartColor(), Modifier.weight(1f).height(48.dp), minSpan = (detail.e1rmKg * STALL_TREND_SPAN).toFloat())
                        }
                    }
                }
                PrescriptionList(detail.prescriptions)
            }
            is CardDetail.Deload -> Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                StatTile("Volume", detail.volumePct.toString(), Modifier.weight(1f), unit = "%", note = "of your usual sets")
                StatTile("Load", detail.loadPct.toString(), Modifier.weight(1f), unit = "%", note = if (bodybuilding) "of your last weights" else "of your last top sets")
            }
            is CardDetail.ReEntry ->
                StatTile("Load", detail.loadPct.toString(), Modifier.fillMaxWidth(), unit = "%", note = if (bodybuilding) "of your last weights, for 7 days" else "of your last top sets, for 7 days")
            is CardDetail.WeakPoint -> {
                val ratio = detail.ratio
                val miss = detail.missPoint
                if (ratio != null) {
                    RatioBar("${ratio.numerator.label()} ÷ ${ratio.denominator.label()}", ratio.valuePct, ratio.lowPct, ratio.highPct)
                }
                val weakLift = advice.lift
                if (miss != null && weakLift != null) {
                    StatTile(
                        "Sticking point", miss.label(weakLift), Modifier.fillMaxWidth(),
                        note = detail.muscles.takeIf { it.isNotEmpty() }?.let { "Works " + it.joinToString(", ") { m -> m.lowercase() } },
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    if (detail.additions.isEmpty()) {
                        Text("The work for this is already in your plan.", style = MaterialTheme.typography.bodySmall, color = PowertrackerTheme.colors.muted)
                    } else {
                        Eyebrow("Adds to your plan")
                        PrescriptionList(detail.additions)
                    }
                }
            }
            // Drawn by their own cards (PhysiqueCards.kt).
            is CardDetail.MoreWork, is CardDetail.Swap -> Unit
        }
    }
}

private const val STALL_TREND_SPAN = 0.05

private class Look(val tone: Tone, val icon: ImageVector, val kind: String, val title: String, val action: String)

/** The accessories on offer, one per line: name on the left, sets × reps on the right. */
@Composable
private fun PrescriptionList(items: List<AccessoryPrescription>) {
    if (items.isEmpty()) return
    Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium).padding(horizontal = Space.m, vertical = Space.xs)) {
        items.forEachIndexed { i, item ->
            if (i > 0) SoftDivider()
            Row(
                Modifier.fillMaxWidth().heightIn(min = 40.dp).semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                Text(item.exerciseName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text(item.dose(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                InfoButton(item.exerciseName)
            }
        }
    }
}

private fun AccessoryPrescription.dose(): String = if (repsLow == repsHigh) "$sets × $repsHigh" else "$sets × $repsLow–$repsHigh"

private fun MainLift.label(): String = liftLabel(this)

private fun MissPoint.label(lift: MainLift): String = when (this) {
    MissPoint.LOCKOUT -> "Lockout"
    MissPoint.MID -> "Mid-range"
    MissPoint.FLOOR -> when (lift) {
        MainLift.SQUAT -> "Out of the hole"
        MainLift.BENCH -> "Off the chest"
        MainLift.DEADLIFT -> "Off the floor"
        MainLift.STRICT_PRESS -> "Off the shoulders"
    }
}

/** An estimate is not worth more than half a kilo of precision. */
private fun Double.toHalfKg(): Double = (this * 2).roundToLong() / 2.0

@Composable
private fun MainLift?.chartColor(): Color {
    val pt = PowertrackerTheme.colors
    return when (this) {
        MainLift.SQUAT -> pt.squat
        MainLift.BENCH -> pt.bench
        MainLift.DEADLIFT -> pt.deadlift
        MainLift.STRICT_PRESS -> pt.press
        null -> pt.muted
    }
}
