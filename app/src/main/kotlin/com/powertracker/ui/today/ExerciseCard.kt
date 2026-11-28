package com.powertracker.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import com.powertracker.data.StoredSet
import com.powertracker.domain.coach.WarmUp
import com.powertracker.domain.history.LastTime
import com.powertracker.domain.model.Advice
import com.powertracker.domain.model.AdvicePayload
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.model.MainLift
import com.powertracker.domain.model.MissPoint
import com.powertracker.domain.model.missPointLabel
import com.powertracker.domain.model.PlanItem
import com.powertracker.domain.model.repsText
import com.powertracker.domain.plates.PlateLoader
import com.powertracker.domain.stack.PlateCheck
import com.powertracker.domain.stack.PlateStack
import com.powertracker.ui.components.formatLoad
import com.powertracker.ui.components.CoachSuggestion
import com.powertracker.ui.components.Eyebrow
import com.powertracker.ui.plan.supersetTag
import com.powertracker.ui.components.PastSets
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
import com.powertracker.ui.components.SoftDivider
import com.powertracker.platform.systemToday
import com.powertracker.ui.library.InfoButton
import com.powertracker.ui.library.RpeInfoButton
import com.powertracker.ui.theme.LocalMotion
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import com.powertracker.ui.theme.Tone
import kotlin.math.roundToLong
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.powertracker.ui.components.pressScale

/** The sets last shown, kept for drawing the last one as it slides out. */
private class LastSets {
    var sets: List<StoredSet> = emptyList()
}

/**
 * The sets an open card has shown, so a set's tick draws itself in only when it was logged now (1.16): one there when
 * the card was first drawn (the screen opening, a restore, a row scrolled back, the card opened) is simply there.
 * Kept by the card, not its exercise id, which a catalogue exercise changes as its first set saves it.
 */
private class SeenSets {
    var shown: Set<Long>? = null

    /** Sets logged while the card was up whose tick has not yet been drawn. */
    val fresh = HashSet<Long>()
}

/** Where the primary button leads once an exercise's sets are done: the next exercise, or Finish. */
class NextStep(val label: String, val onClick: () -> Unit)


/** RPE from which a set counts as a grind worth asking "where?" about. */

/** "Floor / Mid / Lockout", or "Off the shoulders / …" for a strict press. An accessory has no lift: it gets the plain names. */
private fun missLabels(lift: MainLift?): List<String> = MissPoint.entries.map { missPointLabel(lift ?: MainLift.SQUAT, it) }

/** "155 × 5 @ 8"; "plate 7½ × 12 @ 8" for a plate number (1.17). */
fun doseText(weightKg: Double, reps: Int, rpe: Double?, unit: LoadUnit = LoadUnit.KG): String =
    "${if (unit == LoadUnit.PLATES) PlateStack.text(weightKg) else formatKg(weightKg)} × $reps" + if (rpe != null) " @ ${formatRpe(rpe)}" else ""

/** "5 × 5 @ 8": sets × reps @ RPE as planned; "3 × 8–12 @ 8" for a rep range. */
fun planText(item: PlanItem): String = "${item.sets} × ${repsText(item.reps, item.repsHigh)} @ ${formatRpe(item.targetRpe)}"

/**
 * The chip headline for a coach target: "155 × 5 @ 8", or "26 kg × 8–12 @ 8" for a range (bodybuilding mode spec
 * §7.4), plus the set count when the coach changes it. A plate target reads "plate 7 × 8–12 @ 8" (1.17).
 */
fun targetHeadline(target: AdvicePayload.Target): String {
    val dose = if (target.repsHigh == null) {
        doseText(target.weightKg, target.reps, target.rpe, target.unit)
    } else {
        "${formatLoad(target.weightKg, target.unit)} × ${repsText(target.reps, target.repsHigh)} @ ${formatRpe(target.rpe)}"
    }
    return dose + (target.sets?.let { " · $it ${if (it == 1) "set" else "sets"}" } ?: "")
}

// The stored reason repeats the suggestion ("… Coach suggests 155 × 5 @ 8.", or "… 26 × 8–12 @ 8." for a range); the
// chip already shows it as its headline. The lazy match takes the range in its stride.
private val SUGGESTION_SENTENCE = Regex("""\s*Coach suggests .*? @ \d+(\.\d+)?\.""")

fun targetReason(advice: Advice): String = advice.reason.replace(SUGGESTION_SENTENCE, "").trim()

/**
 * The set a later look at this session would call the top set, among the sets in [unit] only (a plate number is never
 * weighed against kg); null while every such set has the same weight.
 */
internal fun topSetIndex(sets: List<LoggedSet>, unit: LoadUnit = LoadUnit.KG): Int? {
    val mine = sets.withIndex().filter { it.value.unit == unit }
    if (mine.map { it.value.weightKg }.distinct().size < 2) return null
    return mine.filter { !it.value.failed }.maxWithOrNull(compareBy({ it.value.weightKg }, { it.index }))?.index
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
    /** The day shown: "Last time" writes the year of a session from another one. */
    today: LocalDate = systemToday(),
    /** With motion on, Today's modifier for the card's content as it opens and closes ([SlidingSwap]); null otherwise. */
    slidingContent: Modifier? = null,
    /** Opens the reason sheet; null hides the row menu (an added exercise, or one with sets). */
    onSkipToday: (() -> Unit)? = null,
    /** Opens the plate sheet (1.17); null leaves Count plates out of the row menu. */
    onCountPlates: (() -> Unit)? = null,
    /** Toggles single-sided ("one side at a time") tracking (1.25); null leaves it out of the row menu. */
    onToggleSingleSided: (() -> Unit)? = null,
    /** Set ids logged during this view that achieved a new personal record. */
    prSetIds: Set<Long> = emptySet(),
) {
    val id = item.exercise.id
    var extraSet by rememberSaveable(id) { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Pair<Int, StoredSet>?>(null) }

    PtCard(modifier, spacing = Space.s) {
        if (slidingContent == null) {
            CardContent(item, position, count, weightStepKg, changePlates, next, actions, bodybuilding, today, extraSet, { extraSet = true }, onSkipToday, onCountPlates, onToggleSingleSided, prSetIds) { selected = it }
        } else {
            Column(slidingContent, verticalArrangement = Arrangement.spacedBy(Space.s)) {
                CardContent(item, position, count, weightStepKg, changePlates, next, actions, bodybuilding, today, extraSet, { extraSet = true }, onSkipToday, onCountPlates, onToggleSingleSided, prSetIds) { selected = it }
            }
        }
    }

    selected?.let { (number, stored) ->
        val canQuickEdit = actions.onUpdateSet != null && stored.set.splitSides == null
        var editWeight by remember(stored.id) { mutableStateOf(formatKg(stored.set.weightKg)) }
        var editReps by remember(stored.id) { mutableStateOf(stored.set.reps.toString()) }
        var editRpe by remember(stored.id) { mutableStateOf(stored.set.rpe?.let(::formatRpe).orEmpty()) }
        val parsedWeight = editWeight.replace(',', '.').toDoubleOrNull()
        val parsedReps = editReps.toIntOrNull()
        val parsedRpe = if (editRpe.isBlank()) null else editRpe.replace(',', '.').toDoubleOrNull()
        val weightValid = parsedWeight != null && (if (stored.set.unit == LoadUnit.PLATES) PlateStack.check(parsedWeight, com.powertracker.domain.model.PlateCount.HALF) == PlateCheck.OK else parsedWeight in 0.0..MAX_KG)
        val repsValid = parsedReps != null && parsedReps in 1..MAX_REPS
        val rpeValid = editRpe.isBlank() || (parsedRpe != null && parsedRpe in MIN_RPE..MAX_RPE)
        val changed = parsedWeight != stored.set.weightKg || parsedReps != stored.set.reps || parsedRpe != stored.set.rpe
        val canSave = canQuickEdit && weightValid && repsValid && rpeValid && changed

        AlertDialog(
            onDismissRequest = { selected = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.padding(horizontal = 16.dp),
            title = { Text("Set $number") },
            // Each set in its own unit, whatever the exercise's setting is now.
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    val split = stored.set.splitSides?.let { "\n" + com.powertracker.domain.unilateral.SingleSided.summaryText(it, stored.set.unit) }.orEmpty()
                    Text(doseText(stored.set.weightKg, stored.set.reps, stored.set.rpe, stored.set.unit) + (if (stored.set.failed) " · failed" else "") + split, style = MaterialTheme.typography.headlineSmall)
                    if (canQuickEdit) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                            androidx.compose.material3.OutlinedTextField(
                                value = editWeight,
                                onValueChange = { editWeight = it },
                                label = { Text(if (stored.set.unit == LoadUnit.PLATES) "Plate" else "kg") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f),
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = editReps,
                                onValueChange = { editReps = it },
                                label = { Text("Reps") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = editRpe,
                                onValueChange = { editRpe = it },
                                label = { Text("RPE") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    if (canSave) {
                        TextButton(
                            onClick = {
                                actions.onUpdateSet(stored.id, parsedWeight, parsedReps, parsedRpe)
                                selected = null
                            },
                        ) { Text("Save", style = MaterialTheme.typography.labelLarge) }
                    }
                    val deleteSource = remember { MutableInteractionSource() }
                    TextButton(
                        onClick = {
                            actions.onDeleteSet(stored.id)
                            selected = null
                        },
                        modifier = Modifier.pressScale(deleteSource),
                        colors = ButtonDefaults.textButtonColors(contentColor = PowertrackerTheme.colors.danger),
                        interactionSource = deleteSource,
                    ) { Text("Delete set", style = MaterialTheme.typography.labelLarge) }
                }
            },
            dismissButton = { QuietButton("Keep", { selected = null }) },
        )
    }
}

/** What the card holds, top to bottom. */
@Composable
private fun ColumnScope.CardContent(
    item: TodayItem,
    position: Int,
    count: Int,
    weightStepKg: Double,
    changePlates: Boolean,
    next: NextStep,
    actions: TodayActions,
    bodybuilding: Boolean,
    today: LocalDate,
    extraSet: Boolean,
    onAddSet: () -> Unit,
    onSkipToday: (() -> Unit)?,
    onCountPlates: (() -> Unit)?,
    onToggleSingleSided: (() -> Unit)?,
    prSetIds: Set<Long>,
    onSelect: (Pair<Int, StoredSet>) -> Unit,
) {
    val id = item.exercise.id
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints {
        // PtCard's own padding, Space.l on each side, is part of the card's width.
        val stacked = stacksActions(maxWidth + Space.l * 2, fontScale)
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The name is never cut: where it and the set count do not fit on one line, the count goes under it.
            WrappingPair(Modifier.weight(1f), first = {
                Column {
                    // In a superset (1.24) the card says which half it is: A is done first, B straight after it, then
                    // the rest. The tag takes the count's place, as on the web (the two together are too wide for the
                    // line beside the pill and the buttons); a screen reader hears both.
                    val pair = item.superset
                    if (pair != null) {
                        val tag = supersetTag(pair.half)
                        Eyebrow(tag, Modifier.semantics { contentDescription = "Exercise $position of $count · $tag" }, maxLines = Int.MAX_VALUE)
                    } else {
                        Eyebrow("Exercise $position of $count")
                    }
                    Text(item.exercise.name, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                }
            }) {
                when {
                    item.planned == null && item.sets.isEmpty() -> QuietButton("Remove", { actions.onRemoveExercise(id) })
                    item.isComplete -> Pill("Done", tone = Tone.SUCCESS, icon = Icons.Filled.Check)
                    item.targetSets != null -> Pill("Set ${item.sets.size + 1} of ${item.targetSets}")
                    else -> Pill("Set ${item.sets.size + 1}")
                }
            }
            GuideAndMenu(item.exercise.name, onSkipToday, onCountPlates, item.singleSided, onToggleSingleSided, stacked)
        }
    }

        item.suggestion?.let { advice ->
            val target = advice.payload as? AdvicePayload.Target ?: return@let
            CoachSuggestion(targetHeadline(target), targetReason(advice), onApply = { actions.onApply(advice.id) }, onDismiss = { actions.onDismiss(advice.id) })
        }

        // With motion on, the last set to be deleted is drawn as it was while it slides out.
        val lastSets = remember(id) { LastSets() }
        if (item.sets.isNotEmpty()) lastSets.sets = item.sets
        // With motion on, a set new since the card was drawn has its tick drawn in. What the card has shown is kept
        // with motion off too, so a set logged then is not taken for one logged now when motion comes back on. (No key
        // on the side: a card's sets are its session's, and the session fixes the side Today shows.)
        val motion = LocalMotion.current
        val seen = remember { SeenSets() }
        val ids = item.sets.map { it.id }
        val shown = seen.shown
        if (!motion) seen.fresh.clear() else if (shown != null) ids.filterTo(seen.fresh) { it !in shown }
        SideEffect { seen.shown = (shown ?: emptySet()) + ids }
        SlidingBlock(item.sets.isNotEmpty()) {
            val sets = item.sets.ifEmpty { lastSets.sets }
            val top = if (bodybuilding) null else topSetIndex(sets.map { it.set }, item.unit)
            Column {
                SlidingColumnItems(sets, key = { it.id }) { i, stored ->
                    val set = stored.set
                    val drawIn = if (motion && stored.id in seen.fresh) eventProgress(EVENT_MOVE_MS) { seen.fresh.remove(stored.id) } else null
                    SetRow(
                        index = i + 1,
                        weightKg = set.weightKg,
                        reps = set.reps,
                        rpe = set.rpe,
                        state = if (set.failed) SetState.FAILED else SetState.DONE,
                        isTopSet = i == top,
                        isPr = stored.id in prSetIds,
                        onClick = { onSelect(i + 1 to stored) },
                        drawIn = drawIn,
                        unit = set.unit,
                        splitSides = set.splitSides,
                    )
                    if (!bodybuilding && (set.failed || (set.rpe ?: 0.0) >= GRIND_RPE)) {
                        MissRow(i + 1, set, item.exercise.mainLift) { actions.onMissPoint(stored.id, it) }
                    }
                }
            }
        }

        // What was done on this exercise last time (1.20), on both sides, where the stretch and squeeze lines were on the
        // bodybuilding side (they are in the exercise's guide). An exercise never done on this side adds nothing here,
        // not even a gap.
        // While the card takes sets (1.21), the row of last time with the number of the set about to be logged is
        // marked, and "Use set N" puts its weight and reps in the fields: only from a set in the unit the exercise
        // counts in now, so a plate number never goes in a kg field.
        val entering = !item.isComplete || extraSet
        item.lastTime?.takeIf { it.sets.isNotEmpty() }?.let { last ->
            val next = item.sets.size.takeIf { entering && it < last.sets.size }
            val match = next?.let { last.sets[it] }
            LastTimeBlock(
                last, today, next,
                // ... and, for a plate number, on the grid the machine counts on now (plate 7½ is not offered once whole
                // plates are on), as the coach's targets are held to.
                onUse = if (match != null && match.unit == item.unit && (match.unit != LoadUnit.PLATES || PlateStack.check(match.weightKg, item.plateCount) == PlateCheck.OK)) {
                    { actions.onEdit(id, item.editor.copy(weightKg = match.weightKg, reps = match.reps)) }
                } else {
                    null
                },
            )
        }

        // The entry, or once the sets are done, the way on: one slides closed as the other slides open.
        SlidingBlock(entering, spacing = Space.s) {
            SoftDivider()
            Editor(item, weightStepKg, changePlates, actions)
        }
        SlidingBlock(!entering, spacing = Space.s) {
            PrimaryButton(next.label, next.onClick, Modifier.fillMaxWidth().padding(top = Space.xs), icon = Icons.AutoMirrored.Filled.ArrowForward)
            QuietButton("Add another set", onAddSet, Modifier.align(Alignment.CenterHorizontally))
        }
}

private val LAST_TIME_DAY = DateTimeFormatter.ofPattern("EEE d MMM")
private val LAST_TIME_DAY_AND_YEAR = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

/** "Wed 18 Nov"; "Wed 18 Nov 2025" when it is not [today]'s year. */
internal fun lastTimeDate(date: LocalDate, today: LocalDate): String =
    (if (date.year == today.year) LAST_TIME_DAY else LAST_TIME_DAY_AND_YEAR).format(date.toJavaLocalDate())

/**
 * "Last time · Wed 18 Nov" over every set of that session ([PastSets]), between the logged sets and the weight entry,
 * and above the way on once the day's sets are done. The heading wraps at a large font size; nothing is cut.
 * [next] (1.21) is the index, from 0, of the row to mark as the set about to be logged; [onUse] puts that row's weight
 * and reps in the fields, and without it there is no button.
 */
@Composable
private fun LastTimeBlock(last: LastTime, today: LocalDate, next: Int? = null, onUse: (() -> Unit)? = null) {
    Column(Modifier.testTag("last-time"), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        SoftDivider()
        Text(
            "Last time · ${lastTimeDate(last.date, today)}".uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = PowertrackerTheme.colors.accentText,
            modifier = Modifier.padding(top = Space.xs).semantics { heading() },
        )
        PastSets(last.sets, next = next)
        if (next != null && onUse != null) {
            // Two lines at most: on a small phone at a large font the longest label does not fit one.
            SecondaryButton(useSetText(next + 1, last.sets[next]), onUse, compact = true, modifier = Modifier.padding(top = Space.xs), textMaxLines = 2)
        }
    }
}

/** "Use set 2: 25 kg × 9"; "Use set 1: plate 7½ × 12". The web app's words. */
internal fun useSetText(number: Int, set: LoggedSet): String = "Use set $number: ${formatLoad(set.weightKg, set.unit)} × ${set.reps}"

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
    // The weight is typed as kg or as a plate number (1.17): a draft typed in one is never read as the other, so the
    // drafts are kept per kind of weight field and only this card's three kinds are ever settled.
    val weightKind = weightKind(item.unit, item.plateCount)
    val kinds = setOf(weightKind, EntryKind.REPS, EntryKind.RPE)
    var drafts by rememberSaveable(id, weightKind, stateSaver = DraftsSaver) { mutableStateOf<Drafts>(emptyMap()) }
    val focus = remember { EntryKind.entries.associateWith { FocusRequester() } }
    var focused by remember { mutableStateOf<EntryKind?>(null) }
    fun settle(editor: SetEditor, drafts: Drafts) = settle(editor, drafts, kinds)

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
        if (refused == null && settled.editor.weightKg != null) {
            val updated = item.copy(editor = settled.editor)
            if (item.singleSided) actions.onLogSide(updated, failed) else actions.onLogSet(updated, failed)
        }
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

    // The warm-up checklist (1.23), while the item says there is one: it follows the typing, as the plates below do. An
    // exercise with no plan item is judged a high-rep one by the reps in the field.
    val warmUp = when {
        !item.warmUpOpen -> emptyList()
        weight == e.weightKg && typed.reps == e.reps -> item.warmUp
        else -> WarmUp.sets(item.exercise, weight, item.planned?.reps ?: typed.reps, changePlates)
    }
    if (warmUp.isNotEmpty()) WarmUpBlock(warmUp, item.warmUpDone) { set, done -> actions.onTickWarmUp(id, set, done) }

    if (item.singleSided) {
        val right = item.pendingRight
        Row(
            Modifier.fillMaxWidth().testTag("single-sided-banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            if (right == null) {
                Pill("Right side", tone = Tone.ACCENT)
                Text(
                    "Set ${item.sets.size + 1} · Right first, then Left",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Pill("Left side", tone = Tone.ACCENT)
                Text(
                    "Right: ${doseText(right.weightKg, right.reps, right.rpe, item.unit)}${if (right.failed) " · missed" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                QuietButton("Clear", { actions.onClearPendingRight(id) })
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        if (weightKind == EntryKind.KG) {
            StepperField(
                "Weight", shown(EntryKind.KG)?.text ?: e.weightKg?.let(::formatKg).orEmpty(),
                canDecrement = weight != null && weight > 0.0,
                canIncrement = weight == null || weight < MAX_KG,
                onDecrement = { step(EntryKind.KG) { it.copy(weightKg = stepWeight(it.weightKg, -weightStepKg)) } },
                onIncrement = { step(EntryKind.KG) { it.copy(weightKg = stepWeight(it.weightKg, weightStepKg)) } },
                modifier = Modifier.weight(1.25f), unit = "kg",
                entry = entry(EntryKind.KG, "Weight, kilograms", KeyboardType.Decimal, placeholder = "kg"),
                tick = true,
            )
        } else {
            // A plate number: one step of the machine's grid; + on an empty field gives plate 1, − stops at plate 1.
            // The field shows 7.5 (what it reads back), with no unit after it.
            val plateStep = PlateStack.step(item.plateCount)
            StepperField(
                "Plate", shown(weightKind)?.text ?: e.weightKg?.let(::formatKg).orEmpty(),
                canDecrement = weight != null && weight > PlateStack.MIN_PLATE,
                canIncrement = weight == null || weight < PlateStack.MAX_PLATE,
                onDecrement = { step(weightKind) { it.copy(weightKg = PlateStack.down(it.weightKg, plateStep)) } },
                onIncrement = { step(weightKind) { it.copy(weightKg = PlateStack.up(it.weightKg, plateStep)) } },
                modifier = Modifier.weight(1.25f),
                entry = entry(
                    weightKind, "Weight, plates",
                    if (weightKind == EntryKind.PLATE_HALF) KeyboardType.Decimal else KeyboardType.Number,
                    placeholder = "plate",
                ),
                tick = true,
            )
        }
        StepperField(
            "Reps", shown(EntryKind.REPS)?.text ?: e.reps.toString(),
            canDecrement = typed.reps > 1,
            canIncrement = typed.reps < MAX_REPS,
            onDecrement = { step(EntryKind.REPS) { it.copy(reps = it.reps - 1) } },
            onIncrement = { step(EntryKind.REPS) { it.copy(reps = it.reps + 1) } },
            modifier = Modifier.weight(1f),
            entry = entry(EntryKind.REPS, "Reps", KeyboardType.Number),
            tick = true,
        )
        StepperField(
            "RPE", shown(EntryKind.RPE)?.text ?: formatRpe(e.rpe),
            canDecrement = typed.rpe > MIN_RPE,
            canIncrement = typed.rpe < MAX_RPE,
            onDecrement = { step(EntryKind.RPE) { it.copy(rpe = (it.rpe - 0.5).coerceAtLeast(MIN_RPE)) } },
            onIncrement = { step(EntryKind.RPE) { it.copy(rpe = (it.rpe + 0.5).coerceAtMost(MAX_RPE)) } },
            modifier = Modifier.weight(1f),
            entry = entry(EntryKind.RPE, "RPE", KeyboardType.Decimal),
            tick = true,
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
        // A plate number has no bar and no kg plates: nothing is drawn for it.
        weightKind != EntryKind.KG -> Unit
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

    val logLabel = when {
        !item.singleSided -> "Log set ${item.sets.size + 1}"
        item.pendingRight == null -> "Log right side"
        else -> "Log left side"
    }
    LogRow(
        missed = { SecondaryButton("Missed", { log(true) }, enabled = weight != null, icon = Icons.Filled.Close) },
        log = { PrimaryButton(logLabel, { log(false) }, Modifier.tapAnchor(TapAnchor.LOG_SET), enabled = weight != null, icon = Icons.Filled.Check) },
    )
}

/**
 * Missed and Log set: side by side, Missed as wide as its label and Log set taking the rest, wherever both labels fit on
 * one line. Where they do not (a small phone at a large text size), Log set goes above Missed, each the card's width:
 * a button's label is one line, so squeezed beside Missed it lost its end ("Log" for "Log set 1") without a mark (1.24).
 */
@Composable
private fun LogRow(missed: @Composable () -> Unit, log: @Composable () -> Unit) {
    Layout(contents = listOf(missed, log)) { (missedButton, logButton), constraints ->
        val max = constraints.maxWidth
        val gap = Space.s.roundToPx()
        val missedWidth = missedButton.maxOfOrNull { it.maxIntrinsicWidth(Constraints.Infinity) } ?: 0
        val logWidth = logButton.maxOfOrNull { it.maxIntrinsicWidth(Constraints.Infinity) } ?: 0
        if (missedWidth + gap + logWidth <= max) {
            val a = missedButton.map { it.measure(Constraints(maxWidth = missedWidth)) }
            val room = max - missedWidth - gap
            val b = logButton.map { it.measure(Constraints(minWidth = room, maxWidth = room)) }
            val height = (a + b).maxOfOrNull { it.height } ?: 0
            layout(max, height) {
                a.forEach { it.place(0, (height - it.height) / 2) }
                b.forEach { it.place(missedWidth + gap, (height - it.height) / 2) }
            }
        } else {
            val whole = Constraints(minWidth = max, maxWidth = max)
            val b = logButton.map { it.measure(whole) }
            val a = missedButton.map { it.measure(whole) }
            val logHeight = b.maxOfOrNull { it.height } ?: 0
            val missedHeight = a.maxOfOrNull { it.height } ?: 0
            layout(max, logHeight + gap + missedHeight) {
                b.forEach { it.place(0, 0) }
                a.forEach { it.place(0, logHeight + gap) }
            }
        }
    }
}

/**
 * A folded row or open card narrower than this, over the font scale (a small phone with large text), stacks its guide
 * button and its menu so the name keeps its room, as the web's row does below 260 px (1.13).
 */
internal val ROW_STACK_WIDTH = 260.dp

/** Whether a row or card [width] wide, the whole card, stacks its guide button and menu ([ROW_STACK_WIDTH]). */
internal fun stacksActions(width: Dp, fontScale: Float): Boolean = width / fontScale < ROW_STACK_WIDTH

/**
 * The guide button (ⓘ) and, with [onSkipToday], [onCountPlates] or [onToggleSingleSided], the row menu (⋮): side by
 * side, or [stacked] one above the other.
 */
@Composable
private fun GuideAndMenu(
    name: String,
    onSkipToday: (() -> Unit)?,
    onCountPlates: (() -> Unit)?,
    singleSided: Boolean,
    onToggleSingleSided: (() -> Unit)?,
    stacked: Boolean,
) {
    val menu = onSkipToday != null || onCountPlates != null || onToggleSingleSided != null
    if (stacked && menu) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            InfoButton(name)
            SkipMenuButton(name, onSkipToday, onCountPlates = onCountPlates, singleSided = singleSided, onToggleSingleSided = onToggleSingleSided)
        }
    } else {
        InfoButton(name)
        if (menu) SkipMenuButton(name, onSkipToday, onCountPlates = onCountPlates, singleSided = singleSided, onToggleSingleSided = onToggleSingleSided)
    }
}

/** Another exercise in today's session, folded to one line. Tap to log it. */
@Composable
fun ExerciseRow(
    item: TodayItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSkipToday: (() -> Unit)? = null,
    onCountPlates: (() -> Unit)? = null,
    onToggleSingleSided: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val fontScale = LocalDensity.current.fontScale
    val padding = Space.l
    PtCard(
        modifier,
        contentPadding = PaddingValues(horizontal = padding, vertical = Space.s),
        onClickLabel = "Log ${item.exercise.name}",
        onClick = onClick,
    ) { BoxWithConstraints {
        val stacked = stacksActions(maxWidth + padding * 2, fontScale)
        Row(Modifier.heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            // The name is never cut: where it and the plan do not fit on one line, the plan goes under it, and a long
            // name wraps between words.
            WrappingPair(Modifier.weight(1f), first = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    Text(item.exercise.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                    if (item.suggestion != null) {
                        Icon(Icons.Filled.Bolt, contentDescription = "Coach suggestion", tint = PowertrackerTheme.colors.accentText, modifier = Modifier.size(18.dp))
                    }
                }
            }) {
                // The plan, a superset's tag (1.24) and the count: on a small phone the three are wider than the row, and
                // the last then goes to a line of its own instead of being squeezed to nothing.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                    verticalArrangement = Arrangement.spacedBy(Space.xs),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    item.planned?.let { Text(planText(it), style = MaterialTheme.typography.headlineSmall, color = scheme.onSurfaceVariant, maxLines = 1) }
                    item.superset?.let { Pill(supersetTag(it.half)) }
                    when {
                        item.isComplete -> Pill("Done", tone = Tone.SUCCESS, icon = Icons.Filled.Check)
                        item.sets.isNotEmpty() && item.targetSets != null -> Pill("${item.sets.size} of ${item.targetSets}")
                        item.sets.isNotEmpty() -> Pill("${item.sets.size} ${if (item.sets.size == 1) "set" else "sets"}")
                    }
                }
            }
            GuideAndMenu(item.exercise.name, onSkipToday, onCountPlates, item.singleSided, onToggleSingleSided, stacked)
        }
    } }
}
