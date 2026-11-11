package com.powertracker.platform

import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** What the app shows instead of, or as, its content. */
enum class LockGate {
    /** The setting has not been read yet: show nothing. */
    LOADING,
    OPEN,
    LOCKED,

    /** App lock is on but the phone has nothing to unlock with: say so and let the user in. */
    NO_DEVICE_LOCK,
}

/**
 * Keeps the unlocked state in memory, so a new process always asks again. The grace period stops
 * the file picker, a rotation and the prompt itself from relocking the app.
 */
object AppLock {
    const val RELOCK_AFTER_MILLIS = 60_000L

    /**
     * A strong biometric or the phone's PIN, pattern or password. Before Android 11 the system
     * cannot combine "strong only" with the screen-lock fallback, so any enrolled biometric counts there.
     */
    val ALLOWED_AUTHENTICATORS: Int =
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Authenticators.BIOMETRIC_STRONG else Authenticators.BIOMETRIC_WEAK) or
            Authenticators.DEVICE_CREDENTIAL

    /** The clock (time since boot, so changing the phone's date does not unlock anything); tests replace it. */
    @Volatile
    internal var now: () -> Long = SystemClock::elapsedRealtime

    /** Whether the phone has a biometric or screen lock to ask for; tests replace it. */
    @Volatile
    internal var deviceLockAvailable: (Context) -> Boolean = ::canAuthenticate

    /** Shows the system prompt and calls back once the user is through; tests replace it. */
    @Volatile
    internal var prompt: (FragmentActivity, onUnlocked: () -> Unit) -> Unit = ::showPrompt

    /** The app-lock setting as last read, so a recreated activity (rotation) does not blank while it loads again. */
    @Volatile
    var lastEnabled: Boolean? = null

    private var unlocked = false
    private var stoppedAt: Long? = null

    fun isAvailable(context: Context): Boolean = deviceLockAvailable(context)

    /** True on a new process, and after [RELOCK_AFTER_MILLIS] or more in the background. */
    fun needsUnlock(): Boolean {
        val stopped = stoppedAt
        return !unlocked || (stopped != null && now() - stopped >= RELOCK_AFTER_MILLIS)
    }

    /** [enabled] is the app-lock setting, null while it is still being read. */
    fun gate(enabled: Boolean?, deviceLockAvailable: Boolean): LockGate = when {
        enabled == null -> LockGate.LOADING
        !enabled || !needsUnlock() -> LockGate.OPEN
        // If the phone's screen lock was removed the app opens, rather than shutting the user out for good.
        !deviceLockAvailable -> LockGate.NO_DEVICE_LOCK
        else -> LockGate.LOCKED
    }

    fun markUnlocked() {
        unlocked = true
        stoppedAt = null
    }

    /** The app left the screen. Leaving again while locked keeps the first time, so the lock cannot be waited out. */
    fun markStopped() {
        if (stoppedAt == null) stoppedAt = now()
    }

    /** Back to a new process. For tests. */
    internal fun reset() {
        unlocked = false
        stoppedAt = null
        lastEnabled = null
        now = SystemClock::elapsedRealtime
        deviceLockAvailable = ::canAuthenticate
        prompt = ::showPrompt
    }

    private fun canAuthenticate(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    private fun showPrompt(activity: FragmentActivity, onUnlocked: () -> Unit) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onUnlocked()
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Grit")
            .setSubtitle("Your training log is locked")
            .setConfirmationRequired(false)
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()
        // A cancelled or failed prompt leaves the lock screen up, where the Unlock button asks again.
        runCatching { BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(info) }
    }
}
