package com.powertracker.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import com.powertracker.domain.coach.WarmUpSet
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.stack.PlateStack
import com.powertracker.ui.components.formatKg
import com.powertracker.ui.components.formatLoad
import com.powertracker.ui.components.platesLabel
import com.powertracker.ui.components.pressToggleable
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

/** How to load a barbell warm-up set: "15 per side", "25 + 2.5 per side", "empty bar". Null where there is no bar. */
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

/**
 * The warm-up checklist (1.23, with the web app): above the weight entry until the exercise's first set is logged, one
 * or two lighter sets to do first, each a row to tick. A tick is a note to the lifter and nothing more: no set is
 * logged, so nothing reaches History, the charts or the coach, and the ticks are forgotten when the app is closed.
 * The sets follow the weight in the field, as the plates do. The caller leaves the block out when [sets] is empty.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WarmUpBlock(sets: List<WarmUpSet>, done: Set<WarmUpSet>, onTick: (WarmUpSet, Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    Column(Modifier.testTag("warm-up")) {
        Text(
            "Warm-up · not logged".uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = pt.accentText,
            modifier = Modifier.semantics { heading() },
        )
        sets.forEachIndexed { i, set ->
            val ticked = set in done
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Space.touch)
                    .clip(MaterialTheme.shapes.small)
                    // The row is the control; the box only shows its state.
                    .pressToggleable(value = ticked, role = Role.Checkbox, onValueChange = { onTick(set, it) })
                    .semantics { contentDescription = warmUpSpoken(i + 1, set) }
                    .testTag("warm-up-set-${i + 1}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                Checkbox(
                    checked = ticked,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(checkedColor = pt.accent, checkmarkColor = pt.onAccent, uncheckedColor = scheme.outline),
                )
                // The set, then how to load it: each stays whole, and the second goes under the first where the row is
                // too narrow. Done: quieter and struck through, so what is left to do stands out.
                FlowRow(
                    Modifier.weight(1f).clearAndSetSemantics { },
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        warmUpText(set),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (ticked) pt.muted else scheme.onSurface,
                        textDecoration = if (ticked) TextDecoration.LineThrough else null,
                    )
                    warmUpHint(set)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = pt.muted) }
                }
            }
        }
    }
}
