package com.powertracker.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.powertracker.domain.library.RpeGuide
import com.powertracker.ui.theme.Space

/** What the info button is called, for a screen reader. */
const val RPE_INFO_LABEL = "What RPE means"

/** The room an inline [RpeInfoButton] takes beside a label; its 48 dp target reaches past it. */
private val INLINE_WIDTH = 24.dp

/**
 * The info button beside an RPE label: opens [RpeSheet]. It is a full 48 dp target wherever it is.
 * With [inline] it is drawn small and takes only the room of its icon and no height, so it can sit
 * in a caption without moving anything; it is then centred on the line it is in, and its target
 * reaches over what is around it.
 */
@Composable
fun RpeInfoButton(modifier: Modifier = Modifier, inline: Boolean = false) {
    var open by rememberSaveable { mutableStateOf(false) }
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    if (inline) {
        Box(
            modifier.layout { measurable, _ ->
                val button = measurable.measure(Constraints())
                val width = INLINE_WIDTH.roundToPx()
                layout(width, 0) { button.place((width - button.width) / 2, -button.height / 2) }
            },
        ) {
            IconButton(onClick = { open = true }, modifier = Modifier.size(Space.touch)) {
                Icon(Icons.Outlined.Info, contentDescription = RPE_INFO_LABEL, tint = tint, modifier = Modifier.size(18.dp))
            }
        }
    } else {
        IconButton(onClick = { open = true }, modifier = modifier.size(Space.touch)) {
            Icon(Icons.Outlined.Info, contentDescription = RPE_INFO_LABEL, tint = tint)
        }
    }
    if (open) RpeSheet(onDismiss = { open = false })
}

/** What RPE means, as a bottom sheet in the style of the form guides. */
@Composable
fun RpeSheet(onDismiss: () -> Unit) {
    GuideSheet(onDismiss) { RpeContent(onClose = onDismiss, modifier = Modifier.navigationBarsPadding()) }
}

/**
 * The sheet's content without the sheet: the title, then [RpeGuide]'s lines in order, the scale
 * itself (the lines that start with an RPE value) as a list. There is no safety line: this explains
 * a scale, not an exercise.
 */
@Composable
fun RpeContent(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val lines = RpeGuide.lines
    val isValue = { line: String -> line.firstOrNull()?.isDigit() == true }
    val scaleStart = lines.indexOfFirst(isValue).takeIf { it >= 0 } ?: lines.size
    val scaleEnd = maxOf(scaleStart, lines.indexOfLast(isValue) + 1)
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = Space.xl, end = Space.s, bottom = Space.xl)
            .testTag("rpe-sheet"),
    ) {
        GuideHeader(RpeGuide.title, eyebrow = null, closeLabel = "Close", onClose = onClose)
        Column(Modifier.padding(end = Space.l)) {
            for (line in lines.take(scaleStart)) Paragraph(line, Modifier.padding(top = Space.m))
            if (scaleEnd > scaleStart) GuideLines(lines.subList(scaleStart, scaleEnd), numbered = false, Modifier.padding(top = Space.l))
            for (line in lines.drop(scaleEnd)) Paragraph(line, Modifier.padding(top = Space.l))
        }
    }
}

@Composable
private fun Paragraph(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}
