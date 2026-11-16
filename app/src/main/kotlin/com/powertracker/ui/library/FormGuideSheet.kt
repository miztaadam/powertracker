package com.powertracker.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.powertracker.domain.library.Library
import com.powertracker.domain.library.LibraryExercise
import com.powertracker.domain.library.MuscleFocus
import com.powertracker.domain.library.figure
import com.powertracker.domain.library.figures.ExerciseFigure
import com.powertracker.domain.model.TrainingMode
import com.powertracker.ui.components.Eyebrow
import com.powertracker.ui.theme.LocalTrainingMode
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

/** What the sheet says for an exercise the catalogue does not know. */
const val NO_GUIDE = "No form guide for your own exercises."

/**
 * The form guide for [exerciseName], as a bottom sheet to read between sets. The guide follows the
 * name: a catalogue name or alias shows the catalogue guide, whoever made the row; any other name
 * shows [NO_GUIDE].
 */
@Composable
fun FormGuideSheet(exerciseName: String, onDismiss: () -> Unit) {
    GuideSheet(onDismiss) { FormGuideContent(exerciseName, onClose = onDismiss, modifier = Modifier.navigationBarsPadding()) }
}

/** The bottom sheet every guide opens in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GuideSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Straight to its full height: a guide is read top to bottom, not peeked at.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = scheme.outline) },
    ) {
        content()
    }
}

/**
 * The sheet's content without the sheet: name, where and with what, the two-pose picture, purpose, the
 * four sections (with "Stretch and squeeze" after Cues on the bodybuilding side), the safety line. [actions] (the Exercises tab's "Add to plan" and "Add to today") stay in view under
 * the guide as it scrolls, below the safety line that ends it.
 */
@Composable
fun FormGuideContent(
    exerciseName: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** Finds the exercise's picture; the guide has no picture area when it finds none. */
    figureFor: (String) -> ExerciseFigure? = Library::figure,
    actions: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val entry = Library.find(exerciseName)
    Column(modifier.fillMaxWidth().testTag("form-guide")) {
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(start = Space.xl, end = Space.s, bottom = Space.xl),
        ) {
            GuideHeader(exerciseName, entry?.whereAndWith(), closeLabel = "Close guide", onClose = onClose)
            Column(Modifier.padding(end = Space.l)) {
                if (entry == null) {
                    Text(NO_GUIDE, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = Space.m))
                } else {
                    val guide = entry.guide
                    ExercisePicture(exerciseName, Modifier.padding(top = Space.m), figureFor)
                    Text(guide.purpose, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = Space.m))
                    GuideSection("Setup", guide.setup, numbered = false)
                    GuideSection("How to do it", guide.steps, numbered = true)
                    GuideSection("Cues", guide.cues, numbered = false)
                    StretchAndSqueeze(exerciseName)
                    GuideSection("Common mistakes", guide.mistakes, numbered = false)
                }
                SafetyLine(Modifier.padding(top = Space.xl))
            }
        }
        if (actions != null) {
            HorizontalDivider(color = PowertrackerTheme.colors.hairline)
            Box(Modifier.fillMaxWidth().padding(horizontal = Space.xl, vertical = Space.m).testTag("guide-actions")) { actions() }
        }
    }
}

/** The section's heading on the bodybuilding side. */
const val STRETCH_AND_SQUEEZE = "Stretch and squeeze"

/**
 * Where the muscle is stretched and where it is squeezed, or the one fixed line for a hold or a carry. Only while
 * bodybuilding mode is on (read from the theme, as the mode label is), so the powerlifting side's guide is unchanged
 * and switching the mode shows or hides it at once. An exercise with no lines yet has no section.
 */
@Composable
private fun StretchAndSqueeze(exerciseName: String) {
    if (LocalTrainingMode.current != TrainingMode.BODYBUILDING) return
    val lines = when (val focus = remember(exerciseName) { Library.focus(exerciseName) }) {
        is MuscleFocus.Lines -> listOf(focus.stretch, focus.squeeze)
        is MuscleFocus.NoFocus -> listOf(Library.NO_FOCUS_LINE)
        null -> return
    }
    GuideSection(STRETCH_AND_SQUEEZE, lines, numbered = false)
}

/** The top of a guide sheet: its title, an optional eyebrow under it, and the close button. */
@Composable
internal fun GuideHeader(title: String, eyebrow: String?, closeLabel: String, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f).padding(top = Space.xs)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = scheme.onSurface, modifier = Modifier.semantics { heading() })
            if (eyebrow != null) Eyebrow(eyebrow, Modifier.padding(top = Space.xxs))
        }
        IconButton(onClick = onClose, modifier = Modifier.size(Space.touch)) { Icon(Icons.Filled.Close, contentDescription = closeLabel, tint = scheme.onSurfaceVariant) }
    }
}

/** "Main lifts · Barbell", "Quads · Dumbbell or barbell". */
private fun LibraryExercise.whereAndWith(): String {
    val with = equipment.mapIndexed { i, e -> if (i == 0) e.label() else e.label().lowercase() }.joinToString(" or ")
    return "${area.label()} · $with"
}

private val MARKER_WIDTH = 28.dp

@Composable
private fun GuideSection(title: String, lines: List<String>, numbered: Boolean) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = Space.xl, bottom = Space.s).semantics { heading() },
    )
    GuideLines(lines, numbered)
}

/** A guide's lines, each behind a dot or its number. */
@Composable
internal fun GuideLines(lines: List<String>, numbered: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val body = MaterialTheme.typography.bodyLarge
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.s)) {
        lines.forEachIndexed { i, line ->
            // One spoken line each: "1, Take a big breath…".
            Row(Modifier.semantics(mergeDescendants = true) {}) {
                if (numbered) {
                    Text((i + 1).toString(), style = MaterialTheme.typography.titleMedium.copy(lineHeight = body.lineHeight), color = PowertrackerTheme.colors.muted, modifier = Modifier.width(MARKER_WIDTH))
                } else {
                    // A drawn dot, centred on the first line of text: the font has no bullet to rely on.
                    val lineHeight = with(LocalDensity.current) { body.lineHeight.toDp() }
                    Box(Modifier.width(MARKER_WIDTH).height(lineHeight), contentAlignment = Alignment.CenterStart) {
                        Box(Modifier.padding(start = Space.xxs).size(6.dp).background(PowertrackerTheme.colors.muted, CircleShape))
                    }
                }
                Text(line, style = body, color = scheme.onSurface, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** The fixed last line of every sheet, set apart from the guide in its own block. */
@Composable
private fun SafetyLine(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().background(scheme.surfaceContainerHigh, MaterialTheme.shapes.medium).padding(Space.m),
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Icon(Icons.Outlined.HealthAndSafety, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Text(Library.SAFETY_LINE, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface, modifier = Modifier.weight(1f).testTag("guide-safety"))
    }
}
