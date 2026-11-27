package com.powertracker.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.powertracker.domain.coach.WarmUpSet
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.stack.PlateStack
import com.powertracker.ui.components.formatKg
import com.powertracker.ui.components.formatLoad
import com.powertracker.ui.components.platesLabel
import com.powertracker.ui.components.pressToggleable
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

/** How to load a barbell warm-up set, as it is spoken: "15 per side", "25 + 2.5 per side", "empty bar". Null where there is no bar. */
internal fun warmUpHint(set: WarmUpSet): String? =
    set.perSide?.let { if (it.isEmpty()) "empty bar" else "${platesLabel(it)} per side" }

/** "50 kg × 12"; "plate 3½ × 12". The web app's words. */
internal fun warmUpText(set: WarmUpSet): String = "${formatLoad(set.weight, set.unit)} × ${set.reps}"

/** "Warm-up set 1: 50 kg × 12, 15 per side"; "Warm-up set 1: plate 3 and a half × 12". The web app's sentence. */
internal fun warmUpSpoken(number: Int, set: WarmUpSet): String = buildString {
    append("Warm-up set $number: ")
    append(if (set.unit == LoadUnit.PLATES) PlateStack.spoken(set.weight) else "${formatKg(set.weight)} kg")
    append(" × ${set.reps}")
    warmUpHint(set)?.let { append(", $it") }
}

/** ... and as it is drawn under the set: the plates alone, "25 + 2.5" (the heading says they are per side), or "empty bar". */
internal fun warmUpPlates(set: WarmUpSet): String? = set.perSide?.let { if (it.isEmpty()) "empty bar" else platesLabel(it) }

/** Two sets sit side by side when the card's inside is at least this wide at the font size in use; else they stack. */
private val SIDE_BY_SIDE_WIDTH = 290.dp

/**
 * The warm-up checklist (1.23, with the web app): above the weight entry until the exercise's first set is logged, one
 * or two lighter sets to do first, each a row to tick. A tick is a note to the lifter and nothing more: no set is
 * logged, so nothing reaches History, the charts or the coach, and the ticks are forgotten when the app is closed.
 * The sets follow the weight in the field, as the plates do. Two sets sit side by side where there is room, so the card
 * stays short enough to show its Log button; on a small phone at a large font they stack. A barbell set has its plates
 * under it, with room kept for two lines of them whatever their number: the block is then the same height at every
 * weight, so the − and + under it keep still while the weight is stepped. The caller leaves the block out when [sets]
 * is empty.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WarmUpBlock(sets: List<WarmUpSet>, done: Set<WarmUpSet>, onTick: (WarmUpSet, Boolean) -> Unit) {
    val pt = PowertrackerTheme.colors
    Column(Modifier.testTag("warm-up")) {
        // "Plates per side" is said once, here, not on every set: the lines under the sets are then short enough not to
        // wrap. It goes under the heading where the two do not fit one line.
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Warm-up · not logged".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = pt.accentText,
                modifier = Modifier.semantics { heading() },
            )
            if (sets.any { !it.perSide.isNullOrEmpty() }) {
                Text("Plates per side".uppercase(), style = MaterialTheme.typography.labelSmall, color = pt.muted)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val sideBySide = sets.size == 2 && maxWidth / LocalDensity.current.fontScale >= SIDE_BY_SIDE_WIDTH
            if (sideBySide) {
                // As tall as the taller of the two, so each is a target all the way down.
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    sets.forEachIndexed { i, set -> WarmUpRow(i + 1, set, set in done, onTick, Modifier.weight(1f).fillMaxHeight()) }
                }
            } else {
                Column {
                    sets.forEachIndexed { i, set -> WarmUpRow(i + 1, set, set in done, onTick, Modifier.fillMaxWidth()) }
                }
            }
        }
    }
}

/** One warm-up set: its box, then the set over how to load it. The whole row is the control; the box shows its state. */
@Composable
private fun WarmUpRow(number: Int, set: WarmUpSet, ticked: Boolean, onTick: (WarmUpSet, Boolean) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    Row(
        modifier
            .heightIn(min = Space.touch)
            .clip(MaterialTheme.shapes.small)
            .pressToggleable(value = ticked, role = Role.Checkbox, onValueChange = { onTick(set, it) })
            .semantics { contentDescription = warmUpSpoken(number, set) }
            .testTag("warm-up-set-$number")
            .padding(vertical = 2.dp),
        // From the top, so two sets side by side start on one line whatever the plates under them take.
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Checkbox(
            checked = ticked,
            onCheckedChange = null,
            modifier = Modifier.padding(top = 2.dp),
            colors = CheckboxDefaults.colors(checkedColor = pt.accent, checkmarkColor = pt.onAccent, uncheckedColor = scheme.outline),
        )
        // Done: quieter and struck through, so what is left to do stands out.
        Column(Modifier.weight(1f).clearAndSetSemantics { }) {
            Text(
                warmUpText(set),
                style = MaterialTheme.typography.bodyLarge,
                color = if (ticked) pt.muted else scheme.onSurface,
                textDecoration = if (ticked) TextDecoration.LineThrough else null,
            )
            // Room for two lines of plates whatever their number, so the block keeps one height.
            warmUpPlates(set)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = pt.muted, minLines = 2) }
        }
    }
}
