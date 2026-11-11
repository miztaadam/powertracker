package com.powertracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.powertracker.platform.AppLock
import com.powertracker.platform.LockGate
import com.powertracker.ui.AppShell
import com.powertracker.ui.lock.LockGateHost
import com.powertracker.ui.theme.PowertrackerTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** A FragmentActivity because the system unlock prompt (BiometricPrompt) needs one. */
class MainActivity : FragmentActivity() {
    private var lockEnabled: Boolean? = AppLock.lastEnabled
    private var gate by mutableStateOf(LockGate.LOADING)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge to edge is enforced on current Android; this also keeps status-bar icons readable in both themes.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as PowertrackerApp).container
        lifecycleScope.launch {
            container.settings.settings.map { it.appLock }.distinctUntilChanged().collect { enabled ->
                AppLock.lastEnabled = enabled
                lockEnabled = enabled
                refreshGate()
            }
        }
        setContent {
            PowertrackerTheme {
                LockGateHost(gate, onUnlock = ::askToUnlock, onContinue = ::open) { AppShell(container) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refreshGate(onScreen = true)
    }

    override fun onStop() {
        AppLock.markStopped()
        super.onStop()
    }

    /** Decides what to show. Becoming locked while on screen brings up the system prompt right away. */
    private fun refreshGate(onScreen: Boolean = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        gate = AppLock.gate(lockEnabled, AppLock.isAvailable(this))
        when (gate) {
            // Also with app lock off, so that switching it on in Settings does not lock the session it was switched on in.
            LockGate.OPEN -> AppLock.markUnlocked()
            LockGate.LOCKED -> if (onScreen) askToUnlock()
            LockGate.LOADING, LockGate.NO_DEVICE_LOCK -> Unit
        }
    }

    private fun askToUnlock() = AppLock.prompt(this, ::open)

    private fun open() {
        AppLock.markUnlocked()
        refreshGate()
    }
}
