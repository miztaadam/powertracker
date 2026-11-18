package com.powertracker.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.MainLift
import com.powertracker.ui.components.ChoiceChip
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.QuietButton
import com.powertracker.ui.theme.Space

/**
 * What "Create your own" holds: whether it is open, and everything typed into it. The picker keeps it in the screen's
 * own composition, outside the picker's window, so that it survives a rotation the way the screen's dialogs do.
 */
@Stable
class CustomExerciseForm {
    var open by mutableStateOf(false)
    var name by mutableStateOf("")
    var category by mutableStateOf(Category.ACCESSORY)
    var parent by mutableStateOf<MainLift?>(null)
    var muscles by mutableStateOf("")

    /** Opens the form as new: [initialName] (what was searched for), an accessory, no parent lift and no muscles. */
    fun start(initialName: String) {
        name = initialName
        category = Category.ACCESSORY
        parent = null
        muscles = ""
        open = true
    }

    companion object {
        val Saver: Saver<CustomExerciseForm, Any> = listSaver(
            save = { listOf(it.open, it.name, it.category.name, it.parent?.name.orEmpty(), it.muscles) },
            restore = { v ->
                CustomExerciseForm().apply {
                    open = v[0] as Boolean
                    name = v[1] as String
                    category = Category.valueOf(v[2] as String)
                    parent = (v[3] as String).takeIf { it.isNotEmpty() }?.let(MainLift::valueOf)
                    muscles = v[4] as String
                }
            },
        )
    }
}

@Composable
fun rememberCustomExerciseForm(): CustomExerciseForm = rememberSaveable(saver = CustomExerciseForm.Saver) { CustomExerciseForm() }

/**
 * Name, kind and muscles for an exercise the catalogue does not have, typed into [form] (opened with what was searched
 * for). [failures] counts the creates that failed: each one while the form is open shows "Couldn't save — try again."
 * over it, and the form keeps what was typed.
 */
@Composable
fun CustomExerciseDialog(
    form: CustomExerciseForm,
    onCreate: (name: String, Category, parentLift: MainLift?, muscles: List<String>) -> Unit,
    onDismiss: () -> Unit,
    failures: Int = 0,
) {
    val name = form.name
    val category = form.category
    val parent = form.parent
    val muscles = form.muscles
    val valid = name.isNotBlank() && (category != Category.VARIATION || parent != null)
    // Failures from before this form opened (a form closed and opened again) are not said again.
    val failuresBefore = remember { failures }

    AlertDialog(
        onDismissRequest = onDismiss,
        // Full-width properties: with the platform-default width a text field never settles under
        // Robolectric (the Compose test never goes idle).
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = 16.dp).testTag("custom-exercise-form"),
        title = { Text("Add custom exercise") },
        text = {
            FormMessage(SAVE_FAILED, showId = failures - failuresBefore)
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { form.name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("Variation", category == Category.VARIATION, { form.category = Category.VARIATION })
                    ChoiceChip("Accessory", category == Category.ACCESSORY, { form.category = Category.ACCESSORY })
                }
                if (category == Category.VARIATION) {
                    Text("Parent lift", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Squat, bench and deadlift only: this project adds no strict press variations.
                        for (lift in listOf(MainLift.SQUAT, MainLift.BENCH, MainLift.DEADLIFT)) {
                            ChoiceChip(lift.name.lowercase().replaceFirstChar { it.uppercase() }, parent == lift, { form.parent = lift })
                        }
                    }
                }
                OutlinedTextField(
                    muscles, { form.muscles = it }, label = { Text("Muscles (comma-separated)") }, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                "Create",
                onClick = {
                    val list = muscles.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onCreate(name.trim(), category, if (category == Category.VARIATION) parent else null, list)
                },
                enabled = valid,
                compact = true,
            )
        },
        dismissButton = { QuietButton("Cancel", onDismiss) },
    )
}

/**
 * The screen's snackbar, drawn from inside the form's window so that it sits over the form's scrim at the foot of the
 * screen, where the screen's own would be hidden behind it. Shown afresh each time [showId] goes up from 0.
 */
@Composable
private fun FormMessage(message: String, showId: Int) {
    val host = remember { SnackbarHostState() }
    LaunchedEffect(showId) {
        if (showId > 0) {
            host.currentSnackbarData?.dismiss()
            host.showSnackbar(message)
        }
    }
    if (host.currentSnackbarData == null) return
    val width = LocalConfiguration.current.screenWidthDp.dp
    Popup(popupPositionProvider = FootOfTheScreen, properties = PopupProperties(focusable = false)) {
        SnackbarHost(host, Modifier.width(width).padding(horizontal = Space.gutter, vertical = Space.l))
    }
}

/** Centred at the foot of the window it is shown in, whatever opened it. */
private object FootOfTheScreen : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset =
        IntOffset((windowSize.width - popupContentSize.width) / 2, windowSize.height - popupContentSize.height)
}
