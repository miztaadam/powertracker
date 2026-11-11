package com.powertracker.ui.lock

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.powertracker.R
import com.powertracker.platform.LockGate
import com.powertracker.ui.components.PrimaryButton
import com.powertracker.ui.theme.PowertrackerTheme
import com.powertracker.ui.theme.Space

/**
 * Shows [content] only while the app is open. While it is locked the content is not composed at
 * all, so nothing of the log (dialogs included) can show behind the lock; where the user was is
 * kept and comes back after unlocking.
 */
@Composable
fun LockGateHost(gate: LockGate, onUnlock: () -> Unit, onContinue: () -> Unit, content: @Composable () -> Unit) {
    val keep = rememberSaveableStateHolder()
    when (gate) {
        LockGate.LOADING -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        LockGate.OPEN -> keep.SaveableStateProvider("app") { content() }
        LockGate.LOCKED -> LockScreen(noDeviceLock = false, onUnlock = onUnlock, onContinue = onContinue)
        LockGate.NO_DEVICE_LOCK -> LockScreen(noDeviceLock = true, onUnlock = onUnlock, onContinue = onContinue)
    }
}

/**
 * What stands in for the app while it is locked: the Grit icon, one line on why, and the one action.
 * [noDeviceLock] is the case where app lock is on but the phone has nothing to unlock with.
 */
@Composable
fun LockScreen(noDeviceLock: Boolean, onUnlock: () -> Unit, onContinue: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Space.xl, vertical = Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s, Alignment.CenterVertically),
        ) {
            GritIcon()
            Text(
                if (noDeviceLock) "This phone has no screen lock" else "Grit is locked",
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Space.l),
            )
            Text(
                if (noDeviceLock) {
                    "App lock can't protect your log without one. Set a fingerprint, face or screen lock in your phone's settings, or turn App lock off in Plan › Settings."
                } else {
                    "Use your fingerprint, face or screen lock to open your training log."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (noDeviceLock) {
            PrimaryButton("Continue", onContinue, Modifier.fillMaxWidth())
        } else {
            PrimaryButton("Unlock", onUnlock, Modifier.fillMaxWidth(), icon = Icons.Filled.LockOpen)
        }
    }
}

/** The launcher icon as a launcher draws it: the middle 72 dp of the two 108 dp layers, with rounded corners. */
@Composable
private fun GritIcon() {
    Box(Modifier.size(ICON).clip(MaterialTheme.shapes.large), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ic_launcher_background), contentDescription = null, modifier = Modifier.requiredSize(ICON * 1.5f))
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = "Grit", modifier = Modifier.requiredSize(ICON * 1.5f))
    }
}

private val ICON = 96.dp

@Preview
@Composable
private fun LockScreenPreview() = PowertrackerTheme(darkTheme = true) {
    LockScreen(noDeviceLock = false, onUnlock = {}, onContinue = {})
}
