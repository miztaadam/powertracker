package com.powertracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.powertracker.domain.model.LoadUnit
import com.powertracker.domain.model.LoggedSet
import com.powertracker.domain.stack.PlateStack
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

/** "Set 1: 25 kg × 10 at RPE 8"; "Set 2: plate 7 and a half × 9, missed". The web app's sentence, word for word. */
fun pastSetSpoken(number: Int, set: LoggedSet): String = buildString {
    append("Set $number: ")
    append(if (set.unit == LoadUnit.PLATES) PlateStack.spoken(set.weightKg) else "${formatKg(set.weightKg)} kg")
    append(" × ${set.reps}")
    set.rpe?.let { append(" at RPE ${formatRpe(it)}") }
    if (set.failed) append(", missed")
}

/**
 * The sets of one exercise in an earlier session (1.20, as the web app's since 1.18), in the order given and numbered
 * from 1: weight × reps, "@ RPE" when there is one, "Missed" on a missed set, each in the unit it was logged in. On the
 * Today card under "Last time", and in each workout of Progress's History. Read-only and quieter than a set being
 * logged: smaller figures in the secondary colour, no tick, nothing to tap. Each row is one sentence to TalkBack.
 * The weights end on one line and the "@" signs start on one; at a large font size or on a narrow screen a row's parts
 * go to the next line whole, never cut. [next] (1.21, Today only) is the index, from 0, of the row that matches the set
 * about to be logged: it is drawn in the full text colour with its number in the accent and the word "Next" after it, and
 * reads ", your next set".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PastSets(sets: List<LoggedSet>, modifier: Modifier = Modifier, next: Int? = null) {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    val style = MaterialTheme.typography.titleMedium
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val loads = sets.map { formatLoad(it.weightKg, it.unit) }
    val reps = sets.map { "× ${it.reps}" }
    // Each column as wide as its longest entry, in this style at this font size.
    val (loadWidth, repsWidth) = remember(loads, reps, style, density) {
        fun widest(texts: List<String>) = with(density) { (texts.maxOfOrNull { measurer.measure(it, style, maxLines = 1).size.width } ?: 0).toDp() }
        widest(loads) to widest(reps)
    }
    Column(modifier.testTag("past-sets"), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
        sets.forEachIndexed { i, set ->
            val marked = i == next
            val figures = if (marked) scheme.onSurface else scheme.onSurfaceVariant
            Row(
                Modifier.fillMaxWidth().clearAndSetSemantics { text = AnnotatedString(pastSetSpoken(i + 1, set) + if (marked) ", your next set" else "") },
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                Text((i + 1).toString(), style = MaterialTheme.typography.titleSmall, color = if (marked) pt.accentText else pt.muted, textAlign = TextAlign.Center, modifier = Modifier.width(20.dp).alignByBaseline())
                // Its parts sit on one baseline, the smaller "MISSED" and "NEXT" too, so the row is no taller for them.
                FlowRow(Modifier.weight(1f).alignByBaseline(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    Text(loads[i], style = style, color = figures, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.widthIn(min = loadWidth).alignByBaseline())
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = pt.muted)) { append("× ") }
                            append(set.reps.toString())
                        },
                        style = style, color = figures, maxLines = 1, modifier = Modifier.widthIn(min = repsWidth).alignByBaseline(),
                    )
                    set.rpe?.let { rpe ->
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = pt.muted)) { append("@ ") }
                                append(formatRpe(rpe))
                            },
                            style = style, color = figures, maxLines = 1, modifier = Modifier.padding(start = Space.xs).alignByBaseline(),
                        )
                    }
                    if (set.failed) {
                        Text("MISSED", style = MaterialTheme.typography.labelMedium, color = pt.danger, maxLines = 1, modifier = Modifier.padding(start = Space.xs).alignByBaseline())
                    }
                    // The marked row says so in a word too: on the powerlifting side the accent is a grey close to the others.
                    if (marked) {
                        Text("NEXT", style = MaterialTheme.typography.labelMedium, color = scheme.onSurface, maxLines = 1, modifier = Modifier.padding(start = Space.xs).alignByBaseline())
                    }
                }
            }
        }
    }
}
