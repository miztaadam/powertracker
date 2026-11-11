package com.powertracker.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.powertracker.data.StoredSet
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.plates.PlateLoader
import com.powertracker.ui.components.CoachSuggestion
import com.powertracker.ui.components.Eyebrow
import com.powertracker.ui.components.Pill
import com.powertracker.ui.components.PlateStrip
import com.powertracker.ui.components.PlateStripNote
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.PtCard
import com.powertracker.ui.components.QuietButton
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.components.SegmentedChoice
import com.powertracker.ui.components.SetRow
import com.powertracker.ui.components.SetState
import com.powertracker.ui.components.StepperField
import com.powertracker.ui.components.formatKg
import com.powertracker.ui.components.formatRpe
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone
import kotlin.math.roundToLong

/** Where the primary button leads once an exercise's sets are done: the next exercise, or Finish. */
class NextStep(val label: String, val onClick: () -> Unit)

private const val MAX_KG = 1000.0
private const val MAX_REPS = 99
private const val MIN_RPE = 6.0
private const val MAX_RPE = 10.0

/** RPE from which a set counts as a grind worth asking "where?" about. */
private const val GRIND_RPE = 9.5

private val MISS_LABELS = listOf("Floor", "Mid", "Lockout")

/** "155 × 5 @ 8". */
fun doseText(weightKg: Double, reps: Int, rpe: Double?): String =
    "${formatKg(weightKg)} × $reps" + if (rpe != null) " @ ${formatRpe(rpe)}" else ""

/** "5 × 5 @ 8": sets × reps @ RPE as planned. */
fun planText(item: PlanItem): String = "${item.sets} × ${item.reps} @ ${formatRpe(item.targetRpe)}"

/** The chip headline for a coach target: "155 × 5 @ 8", plus the set count when the coach changes it. */
fun targetHeadline(target: AdvicePayload.Target): String =
    doseText(target.weightKg, target.reps, target.rpe) + (target.sets?.let { " · $it ${if (it == 1) "set" else "sets"}" } ?: "")

// The stored reason repeats the suggestion ("… Coach suggests 155 × 5 @ 8."); the chip already shows it as its headline.
private val SUGGESTION_SENTENCE = Regex("""\s*Coach suggests .*? @ \d+(\.\d+)?\.""")

fun targetReason(advice: Advice): String = advice.reason.replace(SUGGESTION_SENTENCE, "").trim()

/** The set a later look at this session would call the top set; null while every set has the same weight. */
private fun topSetIndex(sets: List<LoggedSet>): Int? {
    if (sets.map { it.weightKg }.distinct().size < 2) return null
    return sets.withIndex().filter { !it.value.failed }.maxWithOrNull(compareBy({ it.value.weightKg }, { it.index }))?.index
}

private fun stepWeight(current: Double?, deltaKg: Double): Double {
    if (current == null) return PlateLoader.BAR_KG
    val next = (current + deltaKg).coerceIn(0.0, MAX_KG)
    return (next * 100).roundToLong() / 100.0
}

/** The exercise being logged: coach chip, sets so far, steppers, plates and the one big button. */
@Composable
fun ExerciseCard(
    item: TodayItem,
    position: Int,
    count: Int,
    weightStepKg: Double,
    changePlates: Boolean,
    next: NextStep,
    actions: TodayActions,
    modifier: Modifier = Modifier,
) {
    val id = item.exercise.id
    var extraSet by rememberSaveable(id) { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Pair<Int, StoredSet>?>(null) }

    PtCard(modifier, spacing = Space.s) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Exercise $position of $count")
                Text(
                    item.exercise.name,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
            when {
                item.planned == null && item.sets.isEmpty() -> QuietButton("Remove", { actions.onRemoveExercise(id) })
                item.isComplete -> Pill("Done", tone = Tone.SUCCESS, icon = Icons.Filled.Check)
                item.targetSets != null -> Pill("Set ${item.sets.size + 1} of ${item.targetSets}")
                else -> Pill("Set ${item.sets.size + 1}")
            }
        }

        item.suggestion?.let { advice ->
            val target = advice.payload as? AdvicePayload.Target ?: return@let
            CoachSuggestion(targetHeadline(target), targetReason(advice), onApply = { actions.onApply(advice.id) }, onDismiss = { actions.onDismiss(advice.id) })
        }

        if (item.sets.isNotEmpty()) {
            val top = topSetIndex(item.logged)
            Column {
                item.sets.forEachIndexed { i, stored ->
                    val set = stored.set
                    SetRow(
                        index = i + 1,
                        weightKg = set.weightKg,
                        reps = set.reps,
                        rpe = set.rpe,
                        state = if (set.failed) SetState.FAILED else SetState.DONE,
                        isTopSet = i == top,
                        onClick = { selected = i + 1 to stored },
                    )
                    if (set.failed || (set.rpe ?: 0.0) >= GRIND_RPE) {
                        MissRow(i + 1, set) { actions.onMissPoint(stored.id, it) }
                    }
                }
            }
        }

        if (!item.isComplete || extraSet) {
            HorizontalDivider(color = PowertrackerTheme.colors.hairline)
            Editor(item, weightStepKg, changePlates, actions)
        } else {
            PrimaryButton(next.label, next.onClick, Modifier.fillMaxWidth().padding(top = Space.xs), icon = Icons.AutoMirrored.Filled.ArrowForward)
            QuietButton("Add another set", { extraSet = true }, Modifier.align(Alignment.CenterHorizontally))
        }
    }

    selected?.let { (number, stored) ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Set $number") },
            text = { Text(doseText(stored.set.weightKg, stored.set.reps, stored.set.rpe) + if (stored.set.failed) " · failed" else "", style = MaterialTheme.typography.headlineSmall) },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.onDeleteSet(stored.id)
                        selected = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = PowertrackerTheme.colors.danger),
                ) { Text("Delete set", style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = { QuietButton("Keep", { selected = null }) },
        )
    }
}

@Composable
private fun MissRow(number: Int, set: LoggedSet, onSelect: (MissPoint?) -> Unit) {
    Row(
        Modifier.padding(start = 32.dp, top = Space.xs, bottom = Space.xs).testTag("miss-row-$number"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Text(
            if (set.failed) "Where did it stall?" else "Where was it slow?",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Tapping the chosen point again clears it.
        SegmentedChoice(MISS_LABELS, set.missPoint?.ordinal, { onSelect(MissPoint.entries[it].takeUnless { point -> point == set.missPoint }) })
    }
}

private enum class Entry { WEIGHT, REPS }

@Composable
private fun Editor(item: TodayItem, weightStepKg: Double, changePlates: Boolean, actions: TodayActions) {
    val id = item.exercise.id
    val e = item.editor
    val weight = e.weightKg
    var entry by remember { mutableStateOf<Entry?>(null) }

    Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        StepperField(
            "Weight", weight?.let(::formatKg) ?: "–",
            canDecrement = weight != null && weight > 0.0,
            canIncrement = weight == null || weight < MAX_KG,
            onDecrement = { actions.onEdit(id, e.copy(weightKg = stepWeight(weight, -weightStepKg))) },
            onIncrement = { actions.onEdit(id, e.copy(weightKg = stepWeight(weight, weightStepKg))) },
            modifier = Modifier.weight(1.25f), unit = "kg", onValueClick = { entry = Entry.WEIGHT },
        )
        StepperField(
            "Reps", e.reps.toString(),
            canDecrement = e.reps > 1,
            canIncrement = e.reps < MAX_REPS,
            onDecrement = { actions.onEdit(id, e.copy(reps = e.reps - 1)) },
            onIncrement = { actions.onEdit(id, e.copy(reps = e.reps + 1)) },
            modifier = Modifier.weight(1f), onValueClick = { entry = Entry.REPS },
        )
        StepperField(
            "RPE", formatRpe(e.rpe),
            canDecrement = e.rpe > MIN_RPE,
            canIncrement = e.rpe < MAX_RPE,
            onDecrement = { actions.onEdit(id, e.copy(rpe = (e.rpe - 0.5).coerceAtLeast(MIN_RPE))) },
            onIncrement = { actions.onEdit(id, e.copy(rpe = (e.rpe + 0.5).coerceAtMost(MAX_RPE))) },
            modifier = Modifier.weight(1f),
        )
    }

    val perSide = item.perSide
    val isBarbellLift = item.exercise.category != Category.ACCESSORY
    when {
        weight == null -> PlateStripNote("No weight yet", "Tap the number to type it in.")
        // Accessories may be dumbbells or machines: only draw a bar when there are plates to put on it.
        perSide != null -> if (isBarbellLift || perSide.isNotEmpty()) PlateStrip(perSide)
        !isBarbellLift -> Unit
        weight < PlateLoader.BAR_KG -> PlateStripNote("Lighter than the bar", "The bar alone weighs ${formatKg(PlateLoader.BAR_KG)} kg.", tone = Tone.WARNING)
        else -> PlateStripNote(
            "Can't load ${formatKg(weight)} kg exactly",
            "Nearest is ${formatKg(PlateLoader.round(weight, changePlates))} kg.",
            tone = Tone.WARNING,
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        SecondaryButton("Missed", { actions.onLogSet(item, true) }, enabled = weight != null, icon = Icons.Filled.Close)
        PrimaryButton("Log set ${item.sets.size + 1}", { actions.onLogSet(item, false) }, Modifier.weight(1f), enabled = weight != null, icon = Icons.Filled.Check)
    }

    when (entry) {
        Entry.WEIGHT -> NumberEntryDialog("Weight", "kg", weight, max = MAX_KG, whole = false, onDismiss = { entry = null }) {
            actions.onEdit(id, e.copy(weightKg = it))
            entry = null
        }
        Entry.REPS -> NumberEntryDialog("Reps", null, e.reps.toDouble(), min = 1.0, max = MAX_REPS.toDouble(), whole = true, onDismiss = { entry = null }) {
            actions.onEdit(id, e.copy(reps = it.toInt()))
            entry = null
        }
        null -> Unit
    }
}

/** Another exercise in today's session, folded to one line. Tap to log it. */
@Composable
fun ExerciseRow(item: TodayItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    PtCard(
        modifier.clip(MaterialTheme.shapes.large).clickable(onClickLabel = "Log ${item.exercise.name}", onClick = onClick),
        contentPadding = PaddingValues(horizontal = Space.l, vertical = Space.s),
    ) {
        Row(Modifier.heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            Text(item.exercise.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (item.suggestion != null) {
                Icon(Icons.Filled.Bolt, contentDescription = "Coach suggestion", tint = PowertrackerTheme.colors.accentText, modifier = Modifier.size(18.dp))
            }
            item.planned?.let { Text(planText(it), style = MaterialTheme.typography.headlineSmall, color = scheme.onSurfaceVariant, maxLines = 1) }
            when {
                item.isComplete -> Pill("Done", tone = Tone.SUCCESS, icon = Icons.Filled.Check)
                item.sets.isNotEmpty() && item.targetSets != null -> Pill("${item.sets.size} of ${item.targetSets}")
                item.sets.isNotEmpty() -> Pill("${item.sets.size} ${if (item.sets.size == 1) "set" else "sets"}")
            }
        }
    }
}
