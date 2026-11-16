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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powertracker.data.StoredSet
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.MuscleFocus
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.missPointLabel
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.repsText
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
import com.powertracker.ui.components.StepperEntry
import com.powertracker.ui.components.StepperField
import com.powertracker.ui.components.formatKg
import com.powertracker.ui.components.formatRpe
import com.powertracker.ui.library.InfoButton
import com.powertracker.ui.library.RpeInfoButton
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone
import kotlin.math.roundToLong

/** Where the primary button leads once an exercise's sets are done: the next exercise, or Finish. */
class NextStep(val label: String, val onClick: () -> Unit)


/** RPE from which a set counts as a grind worth asking "where?" about. */
private const val GRIND_RPE = 9.5

/** "Floor / Mid / Lockout", or "Off the shoulders / …" for a strict press. An accessory has no lift: it gets the plain names. */
private fun missLabels(lift: MainLift?): List<String> = MissPoint.entries.map { missPointLabel(lift ?: MainLift.SQUAT, it) }

/** "155 × 5 @ 8". */
fun doseText(weightKg: Double, reps: Int, rpe: Double?): String =
    "${formatKg(weightKg)} × $reps" + if (rpe != null) " @ ${formatRpe(rpe)}" else ""

/** "5 × 5 @ 8": sets × reps @ RPE as planned; "3 × 8–12 @ 8" for a rep range. */
fun planText(item: PlanItem): String = "${item.sets} × ${repsText(item.reps, item.repsHigh)} @ ${formatRpe(item.targetRpe)}"

/**
 * The chip headline for a coach target: "155 × 5 @ 8", or "26 kg × 8–12 @ 8" for a range (bodybuilding mode spec
 * §7.4), plus the set count when the coach changes it.
 */
fun targetHeadline(target: AdvicePayload.Target): String {
    val dose = if (target.repsHigh == null) {
        doseText(target.weightKg, target.reps, target.rpe)
    } else {
        "${formatKg(target.weightKg)} kg × ${repsText(target.reps, target.repsHigh)} @ ${formatRpe(target.rpe)}"
    }
    return dose + (target.sets?.let { " · $it ${if (it == 1) "set" else "sets"}" } ?: "")
}

// The stored reason repeats the suggestion ("… Coach suggests 155 × 5 @ 8.", or "… 26 × 8–12 @ 8." for a range); the
// chip already shows it as its headline. The lazy match takes the range in its stride.
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

/**
 * The exercise being logged: coach chip, sets so far, steppers, plates and the one big button. In a [bodybuilding]
 * session there is no top set and no "where did it stall?" (bodybuilding mode spec §7.4); Missed still works.
 */
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
    bodybuilding: Boolean = false,
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
            InfoButton(item.exercise.name)
        }

        item.suggestion?.let { advice ->
            val target = advice.payload as? AdvicePayload.Target ?: return@let
            CoachSuggestion(targetHeadline(target), targetReason(advice), onApply = { actions.onApply(advice.id) }, onDismiss = { actions.onDismiss(advice.id) })
        }

        if (item.sets.isNotEmpty()) {
            val top = if (bodybuilding) null else topSetIndex(item.logged)
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
                    if (!bodybuilding && (set.failed || (set.rpe ?: 0.0) >= GRIND_RPE)) {
                        MissRow(i + 1, set, item.exercise.mainLift) { actions.onMissPoint(stored.id, it) }
                    }
                }
            }
        }

        // Bodybuilding side only, and only for an exercise with two lines: a hold, a custom exercise or one not written
        // yet adds nothing here, not even a gap.
        if (bodybuilding) {
            val focus = remember(item.exercise.name) { Library.focus(item.exercise.name) as? MuscleFocus.Lines }
            if (focus != null) MuscleCues(focus)
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

/** Wide enough for "SQUEEZE" at any font size: it grows with the text, so the label is never cut. */
private val CUE_LABEL_WIDTH = 72.sp

/**
 * Where the muscle is stretched and where it is squeezed, each line behind its label, as the bodybuilding mockup has
 * them between the logged sets and the weight entry. The lines wrap; nothing is cut at a large font size.
 */
@Composable
private fun MuscleCues(focus: MuscleFocus.Lines) {
    val labelWidth = with(LocalDensity.current) { CUE_LABEL_WIDTH.toDp() }
    Column(Modifier.testTag("muscle-cues"), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        HorizontalDivider(color = PowertrackerTheme.colors.hairline)
        for ((label, line) in listOf("Stretch" to focus.stretch, "Squeeze" to focus.squeeze)) {
            Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                Eyebrow(label, Modifier.width(labelWidth).alignByBaseline(), color = PowertrackerTheme.colors.accentText)
                Text(line, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f).alignByBaseline())
            }
        }
    }
}

@Composable
private fun MissRow(number: Int, set: LoggedSet, lift: MainLift?, onSelect: (MissPoint?) -> Unit) {
    val modifier = Modifier.padding(start = 32.dp, top = Space.xs, bottom = Space.xs).testTag("miss-row-$number")
    val question = @Composable {
        Text(
            if (set.failed) "Where did it stall?" else "Where was it slow?",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    // Tapping the chosen point again clears it.
    val choice = @Composable {
        SegmentedChoice(missLabels(lift), set.missPoint?.ordinal, { onSelect(MissPoint.entries[it].takeUnless { point -> point == set.missPoint }) })
    }
    if (lift == MainLift.STRICT_PRESS) {
        // "Off the shoulders" needs the whole width: the question goes above the points, not beside them.
        Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            question()
            choice()
        }
    } else {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            question()
            choice()
        }
    }
}

@Composable
private fun Editor(item: TodayItem, weightStepKg: Double, changePlates: Boolean, actions: TodayActions) {
    val id = item.exercise.id
    val e = item.editor
    var drafts by rememberSaveable(id, stateSaver = DraftsSaver) { mutableStateOf<Drafts>(emptyMap()) }
    val focus = remember { EntryKind.entries.associateWith { FocusRequester() } }
    var focused by remember { mutableStateOf<EntryKind?>(null) }

    // What a field shows instead of the editor's value. Text typed over an older value is stale: it
    // stays only where the cursor is, so the typing is not cut short. Anywhere else the newer value
    // shows, which is also what Log set saves. A refused entry always gives way to a newer value.
    fun shown(kind: EntryKind): Draft? =
        drafts[kind]?.takeIf { it.base == e.valueOf(kind) || (it.error == null && kind == focused) }
    // What is not shown is dropped, so it cannot come back if the value returns to what it was.
    val live = drafts.filterKeys { shown(it) != null }
    if (live.size != drafts.size) SideEffect { drafts = live }
    // The editor as it will be once the typing is committed: the plates and the buttons follow the typing.
    val typed = settle(e, drafts).editor
    val weight = typed.weightKg

    /** Leaving a field commits what was typed into it. */
    fun commit(kind: EntryKind) {
        val settled = settle(e, drafts.filterKeys { it == kind })
        drafts = drafts - kind + settled.drafts
        if (settled.editor != e) actions.onEdit(id, settled.editor)
    }

    /** − or +: steps from what is typed, or from the last good value when that was refused, and clears the message. */
    fun step(kind: EntryKind, next: (SetEditor) -> SetEditor) {
        val settled = settle(e, drafts)
        drafts = settled.drafts - kind
        actions.onEdit(id, next(settled.editor))
    }

    /** Log set or Missed. A refused entry on the card stops it: the lifter meant some other number, so the cursor goes to it instead. */
    fun log(failed: Boolean) {
        val settled = settle(e, drafts)
        val refused = EntryKind.entries.firstOrNull { it in settled.drafts }
        // The cursor moves first: the field it leaves commits its own entry, and the whole card's is stored after it.
        if (refused != null) focus.getValue(refused).requestFocus()
        drafts = settled.drafts
        if (settled.editor != e) actions.onEdit(id, settled.editor)
        if (refused == null && settled.editor.weightKg != null) actions.onLogSet(item.copy(editor = settled.editor), failed)
    }

    fun entry(kind: EntryKind, label: String, keyboard: KeyboardType, placeholder: String? = null) = StepperEntry(
        keyboard = keyboard,
        placeholder = placeholder,
        error = shown(kind)?.error,
        onDraft = { drafts = drafts + (kind to Draft(it, e.valueOf(kind))) },
        onCommit = { commit(kind) },
        onFocusChange = { has -> if (has) focused = kind else if (focused == kind) focused = null },
        label = label,
        focus = focus.getValue(kind),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        StepperField(
            "Weight", shown(EntryKind.KG)?.text ?: e.weightKg?.let(::formatKg).orEmpty(),
            canDecrement = weight != null && weight > 0.0,
            canIncrement = weight == null || weight < MAX_KG,
            onDecrement = { step(EntryKind.KG) { it.copy(weightKg = stepWeight(it.weightKg, -weightStepKg)) } },
            onIncrement = { step(EntryKind.KG) { it.copy(weightKg = stepWeight(it.weightKg, weightStepKg)) } },
            modifier = Modifier.weight(1.25f), unit = "kg",
            entry = entry(EntryKind.KG, "Weight, kilograms", KeyboardType.Decimal, placeholder = "kg"),
        )
        StepperField(
            "Reps", shown(EntryKind.REPS)?.text ?: e.reps.toString(),
            canDecrement = typed.reps > 1,
            canIncrement = typed.reps < MAX_REPS,
            onDecrement = { step(EntryKind.REPS) { it.copy(reps = it.reps - 1) } },
            onIncrement = { step(EntryKind.REPS) { it.copy(reps = it.reps + 1) } },
            modifier = Modifier.weight(1f),
            entry = entry(EntryKind.REPS, "Reps", KeyboardType.Number),
        )
        StepperField(
            "RPE", shown(EntryKind.RPE)?.text ?: formatRpe(e.rpe),
            canDecrement = typed.rpe > MIN_RPE,
            canIncrement = typed.rpe < MAX_RPE,
            onDecrement = { step(EntryKind.RPE) { it.copy(rpe = (it.rpe - 0.5).coerceAtLeast(MIN_RPE)) } },
            onIncrement = { step(EntryKind.RPE) { it.copy(rpe = (it.rpe + 0.5).coerceAtMost(MAX_RPE)) } },
            modifier = Modifier.weight(1f),
            entry = entry(EntryKind.RPE, "RPE", KeyboardType.Decimal),
            labelAction = { RpeInfoButton(inline = true) },
        )
    }

    val perSide = when (weight) {
        null -> null
        e.weightKg -> item.perSide
        else -> PlateLoader.perSide(weight, changePlates)
    }
    val isBarbellLift = item.exercise.category != Category.ACCESSORY
    when {
        weight == null -> PlateStripNote("No weight yet", "Type the weight to see the plates.")
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
        SecondaryButton("Missed", { log(true) }, enabled = weight != null, icon = Icons.Filled.Close)
        PrimaryButton("Log set ${item.sets.size + 1}", { log(false) }, Modifier.weight(1f), enabled = weight != null, icon = Icons.Filled.Check)
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
            InfoButton(item.exercise.name)
        }
    }
}
