package com.powertracker.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.powertracker.domain.model.SkipReason
import com.powertracker.ui.components.PtDropdownMenuItem
import com.powertracker.ui.components.PtIconButton
import com.powertracker.ui.components.QuietButton
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import kotlinx.coroutines.delay

// Skipping a planned exercise for today (skip-a-planned-exercise spec, "What you see on Today"). The web app's Today
// uses the same words.
const val SKIP_TODAY = "Skip today"
const val ONE_SIDE_AT_A_TIME = "One side at a time"
const val UNDO = "Undo"
const val SKIPPED_TODAY = "Skipped today"
const val BRING_BACK = "Bring back"

/** The reason sheet's title. */
fun skipSheetTitle(name: String): String = "Why skip $name?"

/** The snackbar after a skip, with [UNDO]. */
fun skippedMessage(name: String): String = "$name skipped."

/** Bring back's label for a screen reader, as on the web: it names what it brings back. */
fun bringBackLabel(name: String): String = "$BRING_BACK $name"

/** The row menu's label, for a screen reader. */
fun exerciseOptions(name: String): String = "Options for $name"

/** The four answers, in the sheet's order; "Just skip" stores no reason. */
val SKIP_REASONS: List<Pair<String, SkipReason?>> = listOf(
    "Busy or no equipment" to SkipReason.BUSY,
    "Pain" to SkipReason.PAIN,
    "Out of time" to SkipReason.TIME,
    "Just skip" to null,
)

/**
 * How long, in ms, the reason sheet ignores taps on its answers once it has opened: it opens under the finger that
 * chose Skip today, where a double tap's second tap would land (as Plan's add form, ADD_FORM_TAP_WINDOW_MS). While it
 * slides up the answers ignore taps too. The plate sheet's choices (1.17) keep the same window.
 */
internal const val SKIP_SHEET_TAP_WINDOW_MS = 300L

/** Whether Today offers "Skip today" for this row: a planned exercise with nothing logged. */
val TodayItem.canSkip: Boolean get() = planned != null && sets.isEmpty()

/**
 * The row menu (⋮): Skip today with [onSkipToday], Count plates on this machine with [onCountPlates] (1.17),
 * and One side at a time with [onToggleSingleSided] (1.25). Draws nothing when none are given.
 */
@Composable
fun SkipMenuButton(
    name: String,
    onSkipToday: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onCountPlates: (() -> Unit)? = null,
    singleSided: Boolean = false,
    onToggleSingleSided: (() -> Unit)? = null,
) {
    if (onSkipToday == null && onCountPlates == null && onToggleSingleSided == null) return
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        PtIconButton(onClick = { open = true }, modifier = Modifier.size(Space.touch)) {
            Icon(Icons.Filled.MoreVert, contentDescription = exerciseOptions(name), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (onSkipToday != null) {
                SkipMenuItem {
                    open = false
                    onSkipToday()
                }
            }
            if (onCountPlates != null) {
                PlateMenuItem {
                    open = false
                    onCountPlates()
                }
            }
            if (onToggleSingleSided != null) {
                PtDropdownMenuItem(
                    text = { Text(if (singleSided) "$ONE_SIDE_AT_A_TIME ✓" else ONE_SIDE_AT_A_TIME) },
                    onClick = {
                        open = false
                        onToggleSingleSided()
                    },
                )
            }
        }
    }
}

/** The menu's item; internal so the mockups can draw it outside its popup. */
@Composable
internal fun SkipMenuItem(onClick: () -> Unit) {
    PtDropdownMenuItem(text = { Text(SKIP_TODAY) }, leadingIcon = { Icon(Icons.Filled.SkipNext, null) }, onClick = onClick)
}

/**
 * Asks why [name] is skipped. Choosing an answer calls [onReason]; back, a tap outside or dragging it away calls
 * [onDismiss] and nothing is stored. For [SKIP_SHEET_TAP_WINDOW_MS] after it opens, taps on the answers do nothing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkipReasonSheet(name: String, onReason: (SkipReason?) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Not kept across recreation: a restored sheet holds taps for a moment again, which does no harm.
    val tapsLive = remember { mutableStateOf(false) }
    val opened = sheetState.currentValue == SheetValue.Expanded
    LaunchedEffect(opened) {
        if (opened && !tapsLive.value) {
            delay(SKIP_SHEET_TAP_WINDOW_MS)
            tapsLive.value = true
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = scheme.outline) },
    ) {
        SkipReasonContent(name, onReason, Modifier.navigationBarsPadding(), Modifier.dropTapsUntil(tapsLive))
    }
}

/** The sheet's content without the sheet: the question and the four answers. Internal for the mockups. */
@Composable
internal fun SkipReasonContent(name: String, onReason: (SkipReason?) -> Unit, modifier: Modifier = Modifier, answerModifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(start = Space.xl, end = Space.xl, bottom = Space.xl).testTag("skip-sheet"),
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Text(
            skipSheetTitle(name),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = Space.xs).semantics { heading() },
        )
        for ((label, reason) in SKIP_REASONS) {
            SecondaryButton(label, { onReason(reason) }, answerModifier.fillMaxWidth())
        }
    }
}

/** While [live] is false, every tap on the element is eaten before it reaches it. The plate sheet's choices use it too. */
internal fun Modifier.dropTapsUntil(live: State<Boolean>): Modifier = pointerInput(live) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (!live.value) event.changes.forEach { it.consume() }
        }
    }
}

/** The quiet line at the end of Today's list: what was skipped today, each with Bring back. */
@Composable
fun SkippedTodayLine(skipped: List<SkippedToday>, onBringBack: (exerciseId: Long) -> Unit, modifier: Modifier = Modifier) {
    val pt = PowertrackerTheme.colors
    Column(modifier.fillMaxWidth().padding(horizontal = Space.xs).testTag("skipped-today")) {
        Text(SKIPPED_TODAY, style = MaterialTheme.typography.labelLarge, color = pt.muted)
        for (s in skipped) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Never cut: a long name wraps between words beside its Bring back.
                    modifier = Modifier.weight(1f).padding(vertical = Space.xs),
                )
                QuietButton(
                    BRING_BACK,
                    { onBringBack(s.exerciseId) },
                    Modifier.testTag("bring-back-${s.exerciseId}").semantics { contentDescription = bringBackLabel(s.name) },
                )
            }
        }
    }
}
