package com.powertracker.ui.plan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.powertracker.data.AppSettings
import com.powertracker.data.Backup
import com.powertracker.data.BackupFile
import com.powertracker.data.BackupResult
import com.powertracker.data.BackupService
import com.powertracker.data.SettingsRepository
import com.powertracker.ui.common.Stepper
import com.powertracker.ui.common.formatRpe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Outcome of reading a backup file chosen by the user. */
sealed interface RestoreCheck {
    data class Ready(val file: BackupFile) : RestoreCheck
    data class Rejected(val message: String) : RestoreCheck
}

fun checkBackupText(text: String): RestoreCheck = when (val r = Backup.decode(text)) {
    is BackupResult.Ok -> RestoreCheck.Ready(r.file)
    is BackupResult.TooNew -> RestoreCheck.Rejected("This backup is from a newer version of Powertracker.")
    is BackupResult.Corrupt -> RestoreCheck.Rejected("This file isn't a valid Powertracker backup.")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settingsRepo: SettingsRepository,
    backup: BackupService,
    onBack: () -> Unit,
    today: () -> LocalDate = LocalDate::now,
    now: () -> Long = System::currentTimeMillis,
) {
    val settings by settingsRepo.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var pendingRestore by remember { mutableStateOf<BackupFile?>(null) }

    fun update(f: (AppSettings) -> AppSettings) {
        scope.launch { settingsRepo.update(f) }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                val text = Backup.encode(backup.export(now()))
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } ?: error("Cannot open file")
                }
            }.isSuccess
            snackbar.showSnackbar(if (ok) "Backup saved." else "Couldn't save the backup.")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = runCatching {
                withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } }
            }.getOrNull()
            val check = if (text == null) RestoreCheck.Rejected("This file isn't a valid Powertracker backup.") else checkBackupText(text)
            when (check) {
                is RestoreCheck.Ready -> pendingRestore = check.file
                is RestoreCheck.Rejected -> snackbar.showSnackbar(check.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SwitchRow("Change plates", settings.changePlates) { v -> update { it.copy(changePlates = v) } }
            StepperRow(
                "Default RPE", formatRpe(settings.defaultRpe), settings.defaultRpe > 6.0, settings.defaultRpe < 10.0,
                { update { it.copy(defaultRpe = (it.defaultRpe - 0.5).coerceAtLeast(6.0)) } },
                { update { it.copy(defaultRpe = (it.defaultRpe + 0.5).coerceAtMost(10.0)) } },
            )

            Text("Training days", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (day in DayOfWeek.entries) {
                    FilterChip(
                        selected = day in settings.trainingDays,
                        onClick = { update { s -> s.copy(trainingDays = if (day in s.trainingDays) s.trainingDays - day else s.trainingDays + day) } },
                        label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3)) },
                    )
                }
            }

            StepperRow(
                "Main lift rest", "${settings.restMainSec} s", settings.restMainSec > 60, settings.restMainSec < 600,
                { update { it.copy(restMainSec = (it.restMainSec - 30).coerceAtLeast(60)) } },
                { update { it.copy(restMainSec = (it.restMainSec + 30).coerceAtMost(600)) } },
            )
            StepperRow(
                "Accessory rest", "${settings.restAccessorySec} s", settings.restAccessorySec > 30, settings.restAccessorySec < 300,
                { update { it.copy(restAccessorySec = (it.restAccessorySec - 15).coerceAtLeast(30)) } },
                { update { it.copy(restAccessorySec = (it.restAccessorySec + 15).coerceAtMost(300)) } },
            )

            SwitchRow("App lock", settings.appLock) { v -> update { it.copy(appLock = v) } }

            OutlinedButton(onClick = { exportLauncher.launch("powertracker-backup-${today()}.json") }, modifier = Modifier.fillMaxWidth()) {
                Text("Export backup")
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Restore backup")
            }
        }
    }

    pendingRestore?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Replace all data?") },
            text = { Text("Restoring replaces everything in Powertracker with the contents of this backup.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    scope.launch {
                        val ok = runCatching { backup.restore(file) }.isSuccess
                        snackbar.showSnackbar(if (ok) "Backup restored." else "Couldn't restore the backup.")
                    }
                }) { Text("Replace") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun StepperRow(label: String, value: String, canDec: Boolean, canInc: Boolean, onDec: () -> Unit, onInc: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Stepper(label, value, canDec, canInc, onDec, onInc)
    }
}
