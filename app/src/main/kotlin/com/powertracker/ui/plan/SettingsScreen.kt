package com.powertracker.ui.plan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powertracker.BuildConfig
import com.powertracker.data.AppSettings
import com.powertracker.data.Backup
import com.powertracker.data.BackupFile
import com.powertracker.data.BackupResult
import com.powertracker.data.BackupService
import com.powertracker.data.RestAlert
import com.powertracker.data.SettingsRepository
import com.powertracker.domain.model.TrainingMode
import com.powertracker.platform.systemToday
import com.powertracker.ui.components.ChoiceChip
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.components.PtCard
import com.powertracker.ui.components.QuietButton
import com.powertracker.ui.components.ScreenHeader
import com.powertracker.ui.components.SecondaryButton
import com.powertracker.ui.components.SectionHeader
import com.powertracker.ui.components.SegmentedChoice
import com.powertracker.ui.components.Stepper
import com.powertracker.ui.components.formatRpe
import com.powertracker.ui.components.SoftDivider
import com.powertracker.ui.library.RpeInfoButton
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaDayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import com.powertracker.ui.components.PtIconButton
import com.powertracker.ui.components.pressToggleable

/** Outcome of reading a backup file chosen by the user. */
sealed interface RestoreCheck {
    data class Ready(val file: BackupFile) : RestoreCheck
    data class Rejected(val message: String) : RestoreCheck
}

fun checkBackupText(text: String): RestoreCheck = when (val r = Backup.decode(text)) {
    is BackupResult.Ok -> RestoreCheck.Ready(r.file)
    is BackupResult.TooNew -> RestoreCheck.Rejected("This backup is from a newer version of Grit.")
    is BackupResult.Corrupt -> RestoreCheck.Rejected("This file isn't a valid Grit backup.")
}

/** The quiet line at the very bottom of Settings, for example "Grit 1.1 · AppsbyAdam". */
fun appVersionLine(versionName: String): String = "Grit $versionName · AppsbyAdam"

/** The same line as a screen reader says it, without the middle dots. */
fun appVersionSpoken(versionName: String): String = "Grit version $versionName, by AppsbyAdam"

/** The "Vibrate on taps" row's second line; the web app says the same. */
const val VIBRATE_ON_TAPS_SUPPORTING = "A short buzz when you log a set and on other main taps"

/** The name offered when exporting a backup on [date]. */
fun backupFileName(date: LocalDate): String = "grit-backup-$date.json"

private val REST_ALERT_LABELS = listOf(
    RestAlert.VIBRATE to "Vibrate only",
    RestAlert.SOUND_AND_VIBRATE to "Sound + vibrate",
    RestAlert.OFF to "Off",
)

@Composable
fun SettingsScreen(
    settingsRepo: SettingsRepository,
    backup: BackupService,
    onBack: () -> Unit,
    today: () -> LocalDate = ::systemToday,
    now: () -> Long = System::currentTimeMillis,
) {
    val settings by settingsRepo.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var pendingRestore by remember { mutableStateOf<PendingRestore?>(null) }

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
            val check = if (text == null) RestoreCheck.Rejected("This file isn't a valid Grit backup.") else checkBackupText(text)
            when (check) {
                // Before asking: the sides whose plan this backup would remove get the warning instead (1.14.1).
                is RestoreCheck.Ready -> pendingRestore = PendingRestore(check.file, runCatching { backup.sidesLosingPlan(check.file) }.getOrDefault(emptyList()))
                is RestoreCheck.Rejected -> snackbar.showSnackbar(check.message)
            }
        }
    }

    SettingsContent(
        settings = settings,
        onUpdate = ::update,
        onExport = { exportLauncher.launch(backupFileName(today())) },
        onRestore = { importLauncher.launch(arrayOf("*/*")) },
        onBack = onBack,
        snackbar = snackbar,
        // Not a plain copy: switching sides dates the side left and, the first time, copies the training days.
        onTrainingMode = { mode -> scope.launch { settingsRepo.setTrainingMode(mode, today()) } },
    )

    pendingRestore?.let { pending ->
        RestoreConfirmDialog(
            losing = pending.losing,
            onConfirm = {
                pendingRestore = null
                scope.launch {
                    val ok = runCatching { backup.restore(pending.file) }.isSuccess
                    snackbar.showSnackbar(if (ok) "Backup restored." else "Couldn't restore the backup.")
                }
            },
            onCancel = { pendingRestore = null },
        )
    }
}

/** A backup file picked to restore, with the sides whose plan restoring it would remove ([BackupService.sidesLosingPlan]). */
data class PendingRestore(val file: BackupFile, val losing: List<TrainingMode>)

private fun sideNames(sides: List<TrainingMode>, joiner: String): String =
    sides.joinToString(" $joiner ") { if (it == TrainingMode.BODYBUILDING) "bodybuilding" else "powerlifting" }

/** The warning's title (1.14.1): "This backup has no powerlifting plan", or "no powerlifting or bodybuilding plan" for both. */
fun restoreWarningTitle(losing: List<TrainingMode>): String = "This backup has no ${sideNames(losing, "or")} plan"

/** The warning's body (1.14.1); with both sides, "your current powerlifting and bodybuilding plans". */
fun restoreWarningBody(losing: List<TrainingMode>): String {
    val plans = if (losing.size > 1) "${sideNames(losing, "and")} plans" else "${sideNames(losing, "and")} plan"
    return "Restoring it removes your current $plans. Your other data is replaced by the backup's too."
}

const val RESTORE_ANYWAY = "Restore anyway"

/**
 * The question before a restore. When the backup would remove a side's plan ([losing] is not empty) it is the warning
 * (1.14.1), with Restore anyway and Cancel; otherwise it is "Replace all data?" as before. [onConfirm] restores; Cancel,
 * or dismissing, restores nothing.
 */
@Composable
fun RestoreConfirmDialog(losing: List<TrainingMode>, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val warn = losing.isNotEmpty()
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (warn) restoreWarningTitle(losing) else "Replace all data?") },
        text = { Text(if (warn) restoreWarningBody(losing) else "Restoring replaces everything in Grit with the contents of this backup.") },
        confirmButton = { PrimaryButton(if (warn) RESTORE_ANYWAY else "Replace", onClick = onConfirm, compact = true) },
        dismissButton = { QuietButton("Cancel", onCancel) },
    )
}

/** The Settings screen without its repositories or file pickers: grouped cards of rows. */
@Composable
fun SettingsContent(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    /** The installed build's version name; only tests pass another. */
    versionName: String = BuildConfig.VERSION_NAME,
    onTrainingMode: (TrainingMode) -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = scheme.background,
        topBar = {
            ScreenHeader(
                "Settings",
                modeLabel = true,
                navigation = { PtIconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(start = Space.gutter, end = Space.gutter, bottom = Space.xl),
        ) {
            SectionHeader("Training style")
            Group {
                SwitchRow(
                    "Bodybuilding mode",
                    "Rep ranges, a physique coach and a blue look. Your powerlifting plan is kept.",
                    settings.trainingMode == TrainingMode.BODYBUILDING,
                    Modifier.testTag("bodybuilding-mode"),
                ) { on -> onTrainingMode(if (on) TrainingMode.BODYBUILDING else TrainingMode.POWERLIFTING) }
            }

            // The bodybuilding side hides the powerlifting-only rows (main-lift RPE, strict press, main-lift rest);
            // their stored values stay as they are and the rows come back on the powerlifting side.
            val powerlifting = settings.trainingMode == TrainingMode.POWERLIFTING
            SectionHeader("Loading and targets", Modifier.padding(top = Space.m))
            Group {
                SwitchRow("Change plates", "Adds 0.5 and 0.25 kg plates, so loads round to 0.5 kg", settings.changePlates) { v ->
                    onUpdate { it.copy(changePlates = v) }
                }
                if (powerlifting) {
                    RowDivider()
                    StepperRow(
                        "Default RPE", "Planned effort for main lifts", formatRpe(settings.defaultRpe), settings.defaultRpe > 6.0, settings.defaultRpe < 10.0,
                        { onUpdate { it.copy(defaultRpe = (it.defaultRpe - 0.5).coerceAtLeast(6.0)) } },
                        { onUpdate { it.copy(defaultRpe = (it.defaultRpe + 0.5).coerceAtMost(10.0)) } },
                        labelAction = { RpeInfoButton(inline = true) },
                    )
                }
            }

            if (powerlifting) {
                SectionHeader("Progress", Modifier.padding(top = Space.m))
                Group {
                    SwitchRow("Strict press", "Show it on Progress and add it to your total.", settings.showStrictPress) { v ->
                        onUpdate { it.copy(showStrictPress = v) }
                    }
                }
            }

            SectionHeader("Training days", Modifier.padding(top = Space.m))
            PtCard(contentPadding = PaddingValues(horizontal = Space.s, vertical = Space.s)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.xxs)) {
                    for (day in DayOfWeek.entries) {
                        ChoiceChip(
                            text = day.toJavaDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                            // The side the app is on keeps its own days (bodybuilding mode spec §6).
                            selected = day in settings.activeTrainingDays,
                            onClick = {
                                onUpdate { s ->
                                    val days = s.activeTrainingDays
                                    s.withActiveTrainingDays(if (day in days) days - day else days + day)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            SectionHeader("Rest timer", Modifier.padding(top = Space.m))
            Group {
                if (powerlifting) {
                    StepperRow(
                        "Main lift rest", "Squat, bench, deadlift", "${settings.restMainSec} s", settings.restMainSec > 60, settings.restMainSec < 600,
                        { onUpdate { it.copy(restMainSec = (it.restMainSec - 30).coerceAtLeast(60)) } },
                        { onUpdate { it.copy(restMainSec = (it.restMainSec + 30).coerceAtMost(600)) } },
                    )
                    RowDivider()
                }
                // Every bodybuilding exercise gets the accessory rest, so on that side it is the one rest row.
                StepperRow(
                    if (powerlifting) "Accessory rest" else "Rest",
                    if (powerlifting) "Everything else" else "Every exercise",
                    "${settings.restAccessorySec} s", settings.restAccessorySec > 30, settings.restAccessorySec < 300,
                    { onUpdate { it.copy(restAccessorySec = (it.restAccessorySec - 15).coerceAtLeast(30)) } },
                    { onUpdate { it.copy(restAccessorySec = (it.restAccessorySec + 15).coerceAtMost(300)) } },
                )
                RowDivider()
                RowLabel("Rest alert", "How you're told the rest is over")
                SegmentedChoice(
                    REST_ALERT_LABELS.map { it.second },
                    REST_ALERT_LABELS.indexOfFirst { it.first == settings.restAlert },
                    { i -> onUpdate { it.copy(restAlert = REST_ALERT_LABELS[i].first) } },
                    Modifier.padding(bottom = Space.m),
                    // At 320 dp and 130% text the labels would be cut short at their usual 15 sp.
                    minFontSize = 12.sp,
                )
                RowDivider()
                SwitchRow("Vibrate on taps", VIBRATE_ON_TAPS_SUPPORTING, settings.vibrateOnTaps) { v -> onUpdate { it.copy(vibrateOnTaps = v) } }
            }

            SectionHeader("Privacy", Modifier.padding(top = Space.m))
            Group {
                SwitchRow("App lock", "Ask for your fingerprint, face or screen lock when Grit opens, and after a minute away", settings.appLock) { v -> onUpdate { it.copy(appLock = v) } }
            }

            SectionHeader("Backup", Modifier.padding(top = Space.m))
            PtCard {
                Text(
                    "Your log lives only on this phone. Export a file now and then so you can restore it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                SecondaryButton("Export backup", onExport, Modifier.fillMaxWidth(), icon = Icons.Filled.Upload)
                SecondaryButton("Restore backup", onRestore, Modifier.fillMaxWidth(), icon = Icons.Filled.Download)
            }

            // Last of all: which version this is, and who made it. Plain text, read once without the dots.
            Text(
                appVersionLine(versionName),
                style = MaterialTheme.typography.bodySmall,
                color = PowertrackerTheme.colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.xl)
                    .testTag("app-version")
                    .semantics { contentDescription = appVersionSpoken(versionName) },
            )
        }
    }
}

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    PtCard(contentPadding = PaddingValues(horizontal = Space.l, vertical = Space.xs), spacing = 0.dp, content = content)
}

@Composable
private fun RowDivider() = SoftDivider()

@Composable
private fun RowLabel(label: String, supporting: String, modifier: Modifier = Modifier, labelAction: (@Composable () -> Unit)? = null) {
    Column(modifier.padding(vertical = Space.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f, fill = false))
            labelAction?.invoke()
        }
        Text(supporting, style = MaterialTheme.typography.bodySmall, color = PowertrackerTheme.colors.muted)
    }
}

@Composable
private fun SwitchRow(label: String, supporting: String, checked: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val pt = PowertrackerTheme.colors
    Row(
        modifier.fillMaxWidth().pressToggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        RowLabel(label, supporting, Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = pt.accent,
                checkedThumbColor = pt.onAccent,
                uncheckedTrackColor = scheme.surfaceContainerHighest,
                uncheckedThumbColor = scheme.onSurfaceVariant,
                uncheckedBorderColor = scheme.outline,
            ),
        )
    }
}

@Composable
private fun StepperRow(
    label: String,
    supporting: String,
    value: String,
    canDec: Boolean,
    canInc: Boolean,
    onDec: () -> Unit,
    onInc: () -> Unit,
    labelAction: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        RowLabel(label, supporting, Modifier.weight(1f), labelAction)
        Stepper(label, value, canDec, canInc, onDec, onInc)
    }
}
