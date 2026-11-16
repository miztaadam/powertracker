package com.powertracker.ui.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.repsText
import com.powertracker.ui.components.AdviceCard
import com.powertracker.ui.components.Eyebrow
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.PtCard
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.components.formatRpe
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

// The lagging-muscle coach's words on Coach (stage 3 plan, Task 2). The web app's Coach uses the same.
const val CHECK_IN_DUE_TITLE = "Time for your check-in"
const val CHECK_IN_DUE_LINE = "Four weeks on this plan. Mark the body parts that stand out; everything else stays on track."
const val CHECK_IN = "Check in"

/** "Monday". */
internal fun weekdayName(day: DayOfWeek): String = day.name.lowercase().replaceFirstChar { it.uppercase() }

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** "21 Oct", as the coach's swap reason writes it. */
internal fun shortDate(date: LocalDate): String = "${date.day} ${MONTHS[date.month.ordinal]}"

/** What Add to plan would do, one line per change: "One more set of Incline DB press on Monday (3 → 4)" or "Add Pec deck on Thursday, 3 × 8–12 @ 8". */
fun moreWorkLines(payload: AdvicePayload.MoreWork): List<String> {
    val new = payload.newExercise
    return if (new != null) {
        listOf("Add ${new.name} on ${weekdayName(new.weekday)}, ${new.sets} × ${repsText(new.repsLow, new.repsHigh)} @ ${formatRpe(new.rpe)}")
    } else {
        payload.addSets.map { "One more set of ${it.name} on ${weekdayName(it.weekday)} (${it.fromSets} → ${it.toSets})" }
    }
}

/** "Chest: 12 → 14 sets a week". */
fun weeklyLine(payload: AdvicePayload.MoreWork): String = "${payload.group.label}: ${payload.weeklyBefore} → ${payload.weeklyAfter} sets a week"

/** "Leg extension hasn't moved in 3 weeks": whole weeks from the session it has not beaten to the day the coach saw it. */
fun swapTitle(advice: Advice, payload: AdvicePayload.Swap): String {
    val weeks = payload.since.daysUntil(advice.createdAt) / 7
    return "${payload.name} hasn't moved in $weeks ${if (weeks == 1) "week" else "weeks"}"
}

fun swapLine(payload: AdvicePayload.Swap): String =
    "Same weight and no more reps since ${shortDate(payload.since)}. Another exercise for ${payload.group.label.lowercase()} can get it moving."

/** The check-in falling due, at the top of Coach on the bodybuilding side. It has no dismiss: a check-in with nothing marked is an answer. */
@Composable
fun CheckInDueCard(onCheckIn: () -> Unit, modifier: Modifier = Modifier) {
    PtCard(modifier.testTag("check-in-due")) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Eyebrow("Coach")
            Text(CHECK_IN_DUE_TITLE, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text(CHECK_IN_DUE_LINE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PrimaryButton(CHECK_IN, onCheckIn, Modifier.fillMaxWidth())
    }
}

/** A lagging body part's suggestion: the sets or the exercise it adds, and the body part's weekly sets before and after. */
@Composable
fun MoreWorkCard(advice: Advice, payload: AdvicePayload.MoreWork, onApply: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AdviceCard(
        tone = Tone.INFO, icon = Icons.Filled.AddCircle, tag = "More work",
        title = "${payload.group.label} is lagging", body = moreWorkLines(payload).joinToString("\n"),
        modifier = modifier.testTag("more-work-${advice.id}"),
        primaryAction = "Add to plan", onPrimary = onApply,
        secondaryAction = "Dismiss", onSecondary = onDismiss,
    ) {
        Text(weeklyLine(payload), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** A stuck exercise: one button per alternative, best first, then Dismiss. */
@Composable
fun SwapCard(advice: Advice, payload: AdvicePayload.Swap, onSwap: (String) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AdviceCard(
        tone = Tone.WARNING, icon = Icons.AutoMirrored.Filled.TrendingFlat, tag = "Stuck",
        title = swapTitle(advice, payload), body = swapLine(payload),
        modifier = modifier.testTag("swap-${advice.id}"),
        secondaryAction = "Dismiss", onSecondary = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            for (name in payload.alternatives) {
                SecondaryButton("Swap for $name", { onSwap(name) }, Modifier.fillMaxWidth(), compact = true)
            }
        }
    }
}
