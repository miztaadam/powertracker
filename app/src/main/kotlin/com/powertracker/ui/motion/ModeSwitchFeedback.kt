package com.powertracker.ui.motion

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.powertracker.R
import com.powertracker.domain.motion.ModeSwitchMotion

/**
 * The mode switch's vibration and sound (1.15). [start] is called at the tap and plays both from the start of
 * [kind]'s timeline, so they line up with the animation's clock; [stop] ends both (the sound on a quick fade). Neither
 * reads "Vibrate on taps": the owner wants this vibration always on. Nothing here throws.
 */
interface ModeSwitchFeedback {
    fun start(kind: ModeSwitchKind)

    fun stop()

    /** Lets go of the sound player. */
    fun release()
}

/** A waveform from the tap: [timings] in ms, off first; [amplitudes] (0..255) per slot, or null for the plain pattern. */
class ModeSwitchWaveform(val timings: LongArray, val amplitudes: IntArray?)

/**
 * [kind]'s vibration as one waveform from the tap.
 * - **The drop:** off until the impact, then the reference's `VIBE` (one long slam, then aftershocks), at
 *   [ModeSwitchMotion.DROP_AMPLITUDES] where the phone has [amplitudeControl].
 * - **The pump:** three heartbeats, each starting on its beat ([ModeSwitchMotion.PUMP]`.beats`), building to full strength.
 */
fun modeSwitchWaveform(kind: ModeSwitchKind, amplitudeControl: Boolean): ModeSwitchWaveform {
    val timings = ArrayList<Long>()
    val amps = ArrayList<Int>()
    when (kind) {
        ModeSwitchKind.DROP -> {
            timings += ModeSwitchMotion.DROP.impact.toLong()
            amps += 0
            ModeSwitchMotion.DROP_VIBE.forEachIndexed { i, ms ->
                timings += ms
                amps += ModeSwitchMotion.DROP_AMPLITUDES[i]
            }
        }
        ModeSwitchKind.PUMP -> {
            var at = 0L
            ModeSwitchMotion.PUMP.beats.forEachIndexed { i, beat ->
                val onset = beat.toLong()
                timings += onset - at
                amps += 0
                timings += ModeSwitchMotion.PUMP_VIBE[i]
                amps += ModeSwitchMotion.PUMP_AMPLITUDES[i]
                at = onset + ModeSwitchMotion.PUMP_VIBE[i]
            }
        }
    }
    return ModeSwitchWaveform(timings.toLongArray(), if (amplitudeControl) amps.toIntArray() else null)
}

/** The phone's vibrator, as far as the mode switch needs it. */
interface ModeSwitchVibrator {
    val hasVibrator: Boolean
    val hasAmplitudeControl: Boolean

    fun vibrate(waveform: ModeSwitchWaveform)

    fun cancel()
}

private class AndroidVibrator(private val vibrator: Vibrator) : ModeSwitchVibrator {
    override val hasVibrator get() = vibrator.hasVibrator()
    override val hasAmplitudeControl get() = vibrator.hasAmplitudeControl()

    override fun vibrate(waveform: ModeSwitchWaveform) {
        val amps = waveform.amplitudes
        vibrator.vibrate(if (amps != null) VibrationEffect.createWaveform(waveform.timings, amps, -1) else VibrationEffect.createWaveform(waveform.timings, -1))
    }

    override fun cancel() = vibrator.cancel()
}

private fun systemVibrator(context: Context): ModeSwitchVibrator? = runCatching {
    val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager?)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
    }
    v?.let(::AndroidVibrator)
}.getOrNull()

/** The sound player both sounds go through: media volume, a game's sound. */
fun defaultSoundPool(): SoundPool = SoundPool.Builder()
    .setMaxStreams(2)
    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
    .build()

/**
 * The real vibration and sound.
 * - **Vibration:** [modeSwitchWaveform] with amplitudes where the vibrator has amplitude control, the plain pattern
 *   otherwise, nothing without a vibrator.
 * - **Sound:** a [SoundPool], made and loaded on the first switch, never at app start. A sound that is not loaded yet
 *   plays once it is, if it is still wanted and not too late to line up with the picture. Silent and vibrate modes
 *   play no sound; otherwise it follows the media volume.
 */
class AndroidModeSwitchFeedback(
    context: Context,
    private val vibrator: () -> ModeSwitchVibrator? = { systemVibrator(context) },
    private val soundPool: () -> SoundPool = ::defaultSoundPool,
) : ModeSwitchFeedback {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var pool: SoundPool? = null
    private val ids = mutableMapOf<ModeSwitchKind, Int>()
    private val loaded = mutableSetOf<Int>()

    /** The sound wanted now: which, and since when (uptime ms). Null once stopped. */
    private var wanted: Pair<ModeSwitchKind, Long>? = null
    private var stream = 0
    private var fade: Runnable? = null

    override fun start(kind: ModeSwitchKind) {
        runCatching {
            val v = vibrator() ?: return@runCatching
            if (!v.hasVibrator) return@runCatching
            v.cancel()
            v.vibrate(modeSwitchWaveform(kind, runCatching { v.hasAmplitudeControl }.getOrDefault(false)))
        }
        runCatching { playSound(kind) }
    }

    override fun stop() {
        runCatching { vibrator()?.takeIf { it.hasVibrator }?.cancel() }
        runCatching { stopSound() }
    }

    override fun release() {
        runCatching {
            fade?.let(main::removeCallbacks)
            pool?.release()
        }
        pool = null
        ids.clear()
        loaded.clear()
        wanted = null
    }

    private fun silent(): Boolean {
        val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager? ?: return false
        return audio.ringerMode != AudioManager.RINGER_MODE_NORMAL
    }

    private fun playSound(kind: ModeSwitchKind) {
        stopSound(fadeMs = 30)
        if (silent()) return
        val p = pool ?: soundPool().also { made ->
            pool = made
            made.setOnLoadCompleteListener { _, id, status ->
                if (status != 0) return@setOnLoadCompleteListener
                loaded += id
                val (want, since) = wanted ?: return@setOnLoadCompleteListener
                // Late is fine by a beat; later than that it would no longer land with the picture.
                if (ids[want] == id && SystemClock.uptimeMillis() - since <= LATE_MS) {
                    wanted = null
                    stream = made.play(id, 1f, 1f, 1, 0, 1f)
                }
            }
            // The wanted one first: a fresh install is on powerlifting, so its first switch is the pump.
            val other = if (kind == ModeSwitchKind.DROP) ModeSwitchKind.PUMP else ModeSwitchKind.DROP
            ids[kind] = made.load(app, rawOf(kind), 1)
            ids[other] = made.load(app, rawOf(other), 1)
        }
        val id = ids[kind] ?: return
        if (id in loaded) {
            stream = p.play(id, 1f, 1f, 1, 0, 1f)
        } else {
            wanted = kind to SystemClock.uptimeMillis()
        }
    }

    /** Stops the sound playing on a quick fade, never a click, and forgets one still loading. */
    private fun stopSound(fadeMs: Long = 40) {
        wanted = null
        val p = pool ?: return
        val s = stream
        if (s == 0) return
        stream = 0
        fade?.let(main::removeCallbacks)
        val steps = 4
        var step = 0
        val r = object : Runnable {
            override fun run() {
                step++
                val v = 1f - step.toFloat() / steps
                runCatching { if (v <= 0f) p.stop(s) else p.setVolume(s, v, v) }
                if (v > 0f) main.postDelayed(this, fadeMs / steps)
            }
        }
        fade = r
        r.run()
    }

    private companion object {
        const val LATE_MS = 150L

        fun rawOf(kind: ModeSwitchKind) = if (kind == ModeSwitchKind.DROP) R.raw.grit_hammer_drop else R.raw.grit_pump
    }
}
