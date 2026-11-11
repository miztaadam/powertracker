package com.powertracker.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.powertracker.domain.model.Category
import com.powertracker.domain.model.MainLift
import com.powertracker.ui.components.ChoiceChip
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.QuietButton

/** Name, kind and muscles for an exercise the catalogue does not have. [initialName] is what was searched for. */
@Composable
fun CustomExerciseDialog(
    initialName: String,
    onCreate: (name: String, Category, parentLift: MainLift?, muscles: List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var category by rememberSaveable { mutableStateOf(Category.ACCESSORY) }
    var parent by rememberSaveable { mutableStateOf<MainLift?>(null) }
    var muscles by rememberSaveable { mutableStateOf("") }
    val valid = name.isNotBlank() && (category != Category.VARIATION || parent != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        // Full-width properties: with the platform-default width a text field never settles under
        // Robolectric (the Compose test never goes idle).
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = 16.dp),
        title = { Text("Add custom exercise") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("Variation", category == Category.VARIATION, { category = Category.VARIATION })
                    ChoiceChip("Accessory", category == Category.ACCESSORY, { category = Category.ACCESSORY })
                }
                if (category == Category.VARIATION) {
                    Text("Parent lift", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (lift in MainLift.entries) {
                            ChoiceChip(lift.name.lowercase().replaceFirstChar { it.uppercase() }, parent == lift, { parent = lift })
                        }
                    }
                }
                OutlinedTextField(
                    muscles, { muscles = it }, label = { Text("Muscles (comma-separated)") }, modifier = Modifier.fillMaxWidth(),
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
