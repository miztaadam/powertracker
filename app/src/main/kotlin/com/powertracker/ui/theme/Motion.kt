package com.powertracker.ui.theme

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Whether motion is on: the press springs and, later, layout and chart motion. Off by default, so tests, screenshots
 * and anything drawn outside the app root run with no motion; [com.powertracker.ui.ModeTheme] turns it on from the
 * system's animation setting ([rememberSystemMotion]). Code that animates reads it and, when off, jumps straight to
 * the end state.
 */
val LocalMotion = staticCompositionLocalOf { false }

/** False when the phone's animator duration scale is 0 ("Remove animations" in the accessibility settings). */
fun systemMotionOn(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

/** [systemMotionOn], kept current while the app is open: the setting can change without the activity restarting. */
@Composable
fun rememberSystemMotion(): Boolean {
    val context = LocalContext.current
    var on by remember(context) { mutableStateOf(systemMotionOn(context)) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                on = systemMotionOn(context)
            }
        }
        val resolver = context.contentResolver
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        // A change between the first read and the registration would otherwise be missed.
        on = systemMotionOn(context)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return on
}
