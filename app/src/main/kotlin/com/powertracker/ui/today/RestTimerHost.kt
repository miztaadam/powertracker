package com.powertracker.ui.today

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.powertracker.data.AppSettings
import com.powertracker.data.SettingsRepository
import com.powertracker.platform.RestTimer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the Today screen needs from the rest timer: its two hooks. */
class RestTimerHooks(val onSetLogged: (isMainOrVariation: Boolean) -> Unit, val restTimer: RestTimerUi?)

/** Main lifts and their variations rest longer than accessories. */
fun AppSettings.restSeconds(isMainOrVariation: Boolean): Int = if (isMainOrVariation) restMainSec else restAccessorySec

/**
 * Connects the Today screen to [RestTimer]: a logged set starts a rest of the length set in
 * Settings, and the bar shows it while it runs. The first rest also asks to show notifications
 * (Android 13+), once; if the answer is no, the bar in the app still counts down.
 */
@Composable
fun rememberRestTimerHooks(settings: SettingsRepository): RestTimerHooks {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    // Rests already running post their notification again once it is allowed to show.
    val askToNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) RestTimer.adjust(context, 0)
    }

    val timer by RestTimer.state.collectAsStateWithLifecycle()
    val now by produceState(RestTimer.now(), timer) {
        while (true) {
            value = RestTimer.now()
            if (!timer.isRunning(value)) break
            delay(TICK_MS)
        }
    }

    val onSetLogged: (Boolean) -> Unit = remember(settings, context) {
        { isMainOrVariation ->
            scope.launch {
                val current = settings.settings.first()
                RestTimer.start(context, current.restSeconds(isMainOrVariation), current.restAlert)
                if (shouldAskToNotify(context)) askToNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    val bar = if (timer.isRunning(now)) {
        RestTimerUi(
            remainingSec = timer.remainingSec(now),
            totalSec = timer.totalSec,
            onSubtractThirty = { RestTimer.adjust(context, -30) },
            onAddThirty = { RestTimer.adjust(context, 30) },
            onSkip = { RestTimer.stop(context) },
        )
    } else {
        null
    }
    return RestTimerHooks(onSetLogged, bar)
}

private const val TICK_MS = 200L
private const val PREFS = "rest_timer"
private const val KEY_ASKED = "asked_to_notify"

/** True the first time a rest starts without leave to notify; remembered so every set does not ask again. */
private fun shouldAskToNotify(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 33) return false
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return false
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    if (prefs.getBoolean(KEY_ASKED, false)) return false
    prefs.edit().putBoolean(KEY_ASKED, true).apply()
    return true
}
