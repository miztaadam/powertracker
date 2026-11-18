package com.powertracker.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.ui.components.ChoiceChip
import com.powertracker.ui.components.Eyebrow
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.PtCard
import com.powertracker.ui.components.QuietButton
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.components.StatTile
import com.powertracker.ui.components.formatKg
import com.powertracker.ui.components.SoftDivider
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

private const val MAX_BODYWEIGHT_KG = 400.0

/** Before the first set: what is planned today, two optional check-in numbers, and Start. */
@Composable
fun StartSessionCard(
    items: List<TodayItem>,
    dayName: String,
    onStart: (bodyweightKg: Double?, readiness: Int?) -> Unit,
    modifier: Modifier = Modifier,
    /** Opens the reason sheet for a planned row; null shows no row menus. */
    onSkipToday: ((TodayItem) -> Unit)? = null,
    /** How many of today's planned exercises are skipped. With none left on the list, the eyebrow counts them. */
    skipped: Int = 0,
) {
    // Everything planned today was skipped: it is not a day with nothing planned. Anything on the list, an exercise the
    // lifter added included, is left to do.
    val allSkipped = items.isEmpty() && skipped > 0
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    var bodyweight by rememberSaveable { mutableStateOf<Double?>(null) }
    var readiness by rememberSaveable { mutableStateOf<Int?>(null) }
    var typingBodyweight by rememberSaveable { mutableStateOf(false) }

    PtCard(modifier) {
        Column {
            Eyebrow(
                when {
                    allSkipped -> "$dayName · $skipped skipped"
                    items.isEmpty() -> "Nothing planned"
                    else -> "$dayName · ${items.size} ${if (items.size == 1) "exercise" else "exercises"}"
                },
            )
            Text("Ready to train?", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        }

        // With everything skipped there is no list and no "nothing in your plan": the skipped line follows the card.
        if (items.isEmpty() && !allSkipped) {
            Text(
                "There is nothing in your plan for $dayName. You can still start a session and add exercises as you go.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        } else if (items.isNotEmpty()) {
            Column {
                items.forEachIndexed { i, item ->
                    if (i > 0) SoftDivider()
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = Space.xs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                        // The name is never cut: where it and the plan do not fit on one line, the plan goes under it.
                        WrappingPair(Modifier.weight(1f), first = {
                            Column {
                                Text(item.exercise.name, style = MaterialTheme.typography.titleMedium)
                                (item.suggestion?.payload as? AdvicePayload.Target)?.let { target ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = pt.accentText, modifier = Modifier.size(14.dp))
                                        Text("Coach suggests ${targetHeadline(target)}", style = MaterialTheme.typography.bodySmall, color = pt.accentText, maxLines = 1)
                                    }
                                }
                            }
                        }) {
                            item.planned?.let { Text(planText(it), style = MaterialTheme.typography.headlineSmall, maxLines = 1) }
                        }
                        if (onSkipToday != null && item.canSkip) SkipMenuButton(item.exercise.name, { onSkipToday(item) })
                    }
                }
            }
        }

        SoftDivider()

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Bodyweight", style = MaterialTheme.typography.titleSmall)
                Text("Optional", style = MaterialTheme.typography.bodySmall, color = pt.muted)
            }
            SecondaryButton(bodyweight?.let { "${formatKg(it)} kg" } ?: "Add", { typingBodyweight = true }, compact = true)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text("Readiness", style = MaterialTheme.typography.titleSmall)
            Text("Optional · 1 is wrecked, 5 is great", style = MaterialTheme.typography.bodySmall, color = pt.muted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                for (level in 1..5) {
                    ChoiceChip(level.toString(), selected = readiness == level, onClick = { readiness = level.takeUnless { it == readiness } }, modifier = Modifier.weight(1f))
                }
            }
        }

        PrimaryButton("Start session", { onStart(bodyweight, readiness) }, Modifier.fillMaxWidth().padding(top = Space.xs), icon = Icons.Filled.PlayArrow)
    }

    if (typingBodyweight) {
        NumberEntryDialog("Bodyweight", "kg", bodyweight, min = 20.0, max = MAX_BODYWEIGHT_KG, whole = false, onDismiss = { typingBodyweight = false }) {
            bodyweight = it
            typingBodyweight = false
        }
    }
}

/** After Finish: what was logged and whether the coach has something new to say. */
@Composable
fun SummaryCard(summary: FinishSummary, onOpenCoach: () -> Unit, onKeepLogging: () -> Unit, modifier: Modifier = Modifier) {
    val pt = PowertrackerTheme.colors
    val notes = when (summary.newNotes) {
        0 -> "No new coach notes"
        1 -> "1 new coach note"
        else -> "${summary.newNotes} new coach notes"
    } + if (summary.skipped >= 1) ", ${summary.skipped} skipped" else ""
    PtCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            Box(Modifier.size(44.dp).background(pt.successContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = pt.success, modifier = Modifier.size(24.dp))
            }
            Column {
                Text("Session saved", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                Text(notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            StatTile("Sets", summary.setCount.toString(), Modifier.weight(1f))
            StatTile("Exercises", summary.exerciseCount.toString(), Modifier.weight(1f))
        }
        if (summary.newNotes > 0) {
            PrimaryButton("See coach notes", onOpenCoach, Modifier.fillMaxWidth(), icon = Icons.Filled.Bolt)
        } else {
            SecondaryButton("Open Coach", onOpenCoach, Modifier.fillMaxWidth(), icon = Icons.Filled.Bolt)
        }
        QuietButton("Keep logging", onKeepLogging, Modifier.align(Alignment.CenterHorizontally))
    }
}
