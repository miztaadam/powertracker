package com.powertracker.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

@Composable
private fun StepButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = scheme.surfaceContainerHighest,
            contentColor = scheme.onSurface,
            disabledContainerColor = scheme.surfaceContainer,
            disabledContentColor = scheme.onSurfaceVariant.copy(alpha = 0.4f),
        ),
    ) { Icon(icon, contentDescription = description) }
}

/** A numeric value with minus/plus buttons on one line; [label] names the buttons for accessibility. */
@Composable
fun Stepper(
    label: String,
    valueText: String,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
        StepButton(Icons.Filled.Remove, "Decrease $label", canDecrement, onDecrement, Modifier.size(Space.touch))
        Text(
            valueText,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 72.dp).padding(horizontal = Space.s),
        )
        StepButton(Icons.Filled.Add, "Increase $label", canIncrement, onIncrement, Modifier.size(Space.touch))
    }
}

/**
 * Makes the figure of a [StepperField] a number field the lifter types straight into.
 *
 * [onDraft] gets every keystroke; the owner hands the text back as the field's `valueText`.
 * [onCommit] fires when the cursor leaves the field, which Done on the keyboard also does.
 * [error] says what is wrong with the entry: it is shown under the field and read with it.
 * [label] is the field's spoken name ("Weight, kilograms"); [focus] lets the owner put the cursor in it.
 */
data class StepperEntry(
    val keyboard: KeyboardType,
    val placeholder: String?,
    val error: String?,
    val onDraft: (String) -> Unit,
    val onCommit: (String) -> Unit,
    val label: String? = null,
    val focus: FocusRequester? = null,
)

/**
 * The in-session stepper: label, a big number, and two wide buttons underneath. Place three in a row.
 * The figure reads as one phrase ("Weight 155 kg"); with [entry] the number is typed into instead,
 * and a tap anywhere on the caption or the figure puts the cursor in it.
 */
@Composable
fun StepperField(
    label: String,
    valueText: String,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
    entry: StepperEntry? = null,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (entry != null) {
            EntryFigure(label, valueText, unit, entry)
        } else {
            val spoken = listOfNotNull(label, valueText, unit).joinToString(" ")
            NumberDisplay(
                valueText, unit = unit, label = label, size = NumberSize.LARGE, centered = true,
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = Space.s), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            StepButton(Icons.Filled.Remove, "Decrease $label", canDecrement, onDecrement, Modifier.weight(1f).height(Space.touch))
            StepButton(Icons.Filled.Add, "Increase $label", canIncrement, onIncrement, Modifier.weight(1f).height(Space.touch))
        }
    }
}

/**
 * The big figure as a field: it looks like a [NumberDisplay] value over a thin rule, not a boxed form
 * field. Focus selects the value, so typing replaces it. An entry wider than the column scrolls
 * inside the field.
 */
@Composable
private fun EntryFigure(label: String, valueText: String, unit: String?, entry: StepperEntry) {
    val scheme = MaterialTheme.colorScheme
    val colors = PowertrackerTheme.colors
    val type = MaterialTheme.typography
    val requester = entry.focus ?: remember { FocusRequester() }
    // Where the cursor goes on Done. Clearing the focus instead would let the system hand it to
    // whatever is first on the screen, which may be another number field.
    val rest = remember { FocusRequester() }
    val current by rememberUpdatedState(entry)
    val text by rememberUpdatedState(valueText)

    // The cursor and selection live here; the text itself is the owner's [valueText].
    var held by remember { mutableStateOf(TextFieldValue(valueText)) }
    val field = if (held.text == valueText) held else TextFieldValue(valueText, TextRange(valueText.length))
    var focused by remember { mutableStateOf(false) }
    // After the tap that brought the focus has placed its cursor: select everything.
    LaunchedEffect(focused) { if (focused) held = TextFieldValue(text, TextRange(0, text.length)) }

    val rule = when {
        entry.error != null -> colors.danger
        focused -> colors.accent
        else -> colors.hairline
    }
    Column(
        // No semantics of its own: the field underneath is what a screen reader lands on.
        Modifier
            .fillMaxWidth()
            .focusRequester(rest)
            .focusTarget()
            .pointerInput(requester) { detectTapGestures { requester.requestFocus() } },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Eyebrow(label, Modifier.clearAndSetSemantics {})
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicTextField(
                value = field,
                onValueChange = {
                    held = it
                    if (it.text != text) current.onDraft(it.text)
                },
                modifier = Modifier
                    .alignByBaseline()
                    .weight(1f, fill = false)
                    .sizeIn(minWidth = Space.touch, minHeight = Space.touch)
                    .width(IntrinsicSize.Min)
                    .focusRequester(requester)
                    .onFocusChanged {
                        if (focused && !it.isFocused) current.onCommit(text)
                        focused = it.isFocused
                    }
                    .drawBehind {
                        val y = size.height - RULE_WIDTH.toPx() / 2
                        drawLine(rule, Offset(0f, y), Offset(size.width, y), RULE_WIDTH.toPx())
                    }
                    .semantics {
                        contentDescription = current.label ?: label
                        current.error?.let { error(it) }
                    },
                // Not centred text: a one-line field lays its text out unbounded once it fills the field,
                // and centred text is then drawn out of sight. The box below centres a short figure instead.
                textStyle = type.displayMedium.copy(color = scheme.onSurface),
                singleLine = true,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = entry.keyboard, imeAction = ImeAction.Done),
                // Leaving the field is what commits.
                keyboardActions = KeyboardActions(onDone = { rest.requestFocus() }),
                cursorBrush = SolidColor(colors.accentText),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (field.text.isEmpty() && entry.placeholder != null) {
                            Text(entry.placeholder, style = type.displayMedium, color = colors.muted.copy(alpha = 0.55f), maxLines = 1)
                        }
                        Box(Modifier.width(IntrinsicSize.Min)) { inner() }
                    }
                },
            )
            if (unit != null && field.text.isNotEmpty()) {
                Text(unit, style = type.titleSmall, color = colors.muted, maxLines = 1, modifier = Modifier.alignByBaseline().clearAndSetSemantics {})
            }
        }
        entry.error?.let {
            Text(
                it,
                style = type.bodySmall,
                color = colors.danger,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Space.xs).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

private val RULE_WIDTH = 2.dp

@Preview
@Composable
private fun StepperPreview() = PowertrackerTheme(darkTheme = true) {
    PtCard {
        Stepper("Sets", "3", true, true, {}, {})
        Row(horizontalArrangement = Arrangement.spacedBy(Space.m)) {
            StepperField(
                "Weight", "155", true, true, {}, {}, Modifier.weight(1.3f), unit = "kg",
                entry = StepperEntry(KeyboardType.Decimal, "kg", null, {}, {}, label = "Weight, kilograms"),
            )
            StepperField("Reps", "5", true, true, {}, {}, Modifier.weight(1f), entry = StepperEntry(KeyboardType.Number, null, "Whole, 1 to 99", {}, {}))
            StepperField("RPE", "8", true, true, {}, {}, Modifier.weight(1f))
        }
    }
}
