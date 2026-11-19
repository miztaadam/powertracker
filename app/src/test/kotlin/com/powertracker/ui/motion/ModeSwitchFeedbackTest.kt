package com.powertracker.ui.motion

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import com.powertracker.R
import com.powertracker.domain.motion.ModeSwitchMotion
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowVibrator

/** The mode switch's vibration and sound (1.15): always on, strongest where the phone can, never throwing. */
@RunWith(RobolectricTestRunner::class)
class ModeSwitchFeedbackTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun reset() = ShadowVibrator.reset()

    /** A vibrator that notes what it was asked. */
    private class FakeVibrator(override val hasVibrator: Boolean = true, override val hasAmplitudeControl: Boolean = true) : ModeSwitchVibrator {
        val played = mutableListOf<ModeSwitchWaveform>()
        var cancels = 0
        override fun vibrate(waveform: ModeSwitchWaveform) {
            played += waveform
        }
        override fun cancel() {
            cancels++
        }
    }

    // ---- the waveforms ----

    @Test
    fun drop_withAmplitudes_waitsForTheImpact_thenSlamsAtFullStrength() {
        val w = modeSwitchWaveform(ModeSwitchKind.DROP, amplitudeControl = true)
        assertArrayEquals(longArrayOf(250, 420, 35, 180, 45, 110, 60, 60, 80, 30), w.timings)
        assertArrayEquals(intArrayOf(0, 255, 0, 200, 0, 150, 0, 100, 0, 60), w.amplitudes)
        assertEquals(w.timings.size, w.amplitudes!!.size)
        assertEquals(255, w.amplitudes!!.max())
    }

    @Test
    fun drop_withoutAmplitudeControl_isThePlainPattern_offFirst() {
        val w = modeSwitchWaveform(ModeSwitchKind.DROP, amplitudeControl = false)
        assertNull(w.amplitudes)
        // Off until the impact, then the reference's VIBE: on, off, on ...
        assertArrayEquals(longArrayOf(ModeSwitchMotion.DROP.impact.toLong()) + ModeSwitchMotion.DROP_VIBE, w.timings)
    }

    @Test
    fun pump_isThreeHeartbeats_onTheBeats_buildingToFullStrength() {
        val w = modeSwitchWaveform(ModeSwitchKind.PUMP, amplitudeControl = true)
        // Off 90, beat 28; off to 400, beat 55; off to 750, beat 120.
        assertArrayEquals(longArrayOf(90, 28, 282, 55, 295, 120), w.timings)
        assertArrayEquals(intArrayOf(0, 150, 0, 205, 0, 255), w.amplitudes)
        val plain = modeSwitchWaveform(ModeSwitchKind.PUMP, amplitudeControl = false)
        assertNull(plain.amplitudes)
        assertArrayEquals(w.timings, plain.timings)
    }

    @Test
    fun eachBeatStartsOnItsBeat() {
        val w = modeSwitchWaveform(ModeSwitchKind.PUMP, amplitudeControl = true)
        var at = 0L
        val onsets = mutableListOf<Long>()
        w.timings.forEachIndexed { i, ms ->
            if (i % 2 == 1) onsets += at
            at += ms
        }
        assertEquals(ModeSwitchMotion.PUMP.beats.map { it.toLong() }, onsets)
    }

    // ---- the vibration ----

    @Test
    fun amplitudeControl_getsTheAmplitudes_otherwiseThePlainPattern() {
        val strong = FakeVibrator(hasAmplitudeControl = true)
        feedback(strong).start(ModeSwitchKind.DROP)
        assertTrue(strong.played.single().amplitudes != null)

        val plain = FakeVibrator(hasAmplitudeControl = false)
        feedback(plain).start(ModeSwitchKind.DROP)
        assertNull(plain.played.single().amplitudes)
    }

    @Test
    fun noVibrator_vibratesNothing_andThrowsNothing() {
        val none = FakeVibrator(hasVibrator = false)
        val fb = feedback(none)
        fb.start(ModeSwitchKind.DROP)
        fb.stop()
        assertTrue(none.played.isEmpty())

        // The real adapter, on a phone with no vibrator at all, and on one whose service is missing.
        shadowOf(context.getSystemService(Vibrator::class.java)).setHasVibrator(false)
        AndroidModeSwitchFeedback(context).apply { start(ModeSwitchKind.PUMP); stop(); release() }
        AndroidModeSwitchFeedback(context, vibrator = { null }).apply { start(ModeSwitchKind.DROP); stop(); release() }
    }

    @Test
    fun aThrowingVibrator_throwsNothing() {
        val angry = object : ModeSwitchVibrator {
            override val hasVibrator get() = true
            override val hasAmplitudeControl: Boolean get() = error("no service")
            override fun vibrate(waveform: ModeSwitchWaveform) = error("no service")
            override fun cancel() = error("no service")
        }
        feedback(angry).apply { start(ModeSwitchKind.DROP); stop(); release() }
    }

    @Test
    fun theRealVibrator_isAskedForThePattern() {
        shadowOf(context.getSystemService(Vibrator::class.java)).apply {
            setHasVibrator(true)
            setHasAmplitudeControl(false)
        }
        val fb = AndroidModeSwitchFeedback(context)
        fb.start(ModeSwitchKind.DROP)
        val shadow = shadowOf(context.getSystemService(Vibrator::class.java))
        assertTrue(shadow.isVibrating)
        fb.stop()
        assertTrue("stopping cancels the vibration", shadow.isCancelled)
        fb.release()
    }

    @Test
    fun stop_cancelsTheVibration() {
        val v = FakeVibrator()
        val fb = feedback(v)
        fb.start(ModeSwitchKind.PUMP)
        val before = v.cancels
        fb.stop()
        assertEquals(before + 1, v.cancels)
    }

    // ---- the sound ----

    @Test
    fun soundPool_isMadeOnTheFirstSwitch_notBefore() {
        var made = 0
        val fb = AndroidModeSwitchFeedback(context, vibrator = { null }, soundPool = { made++; defaultSoundPool() })
        assertEquals("nothing at start", 0, made)
        fb.start(ModeSwitchKind.DROP)
        assertEquals(1, made)
        fb.start(ModeSwitchKind.PUMP)
        assertEquals("made once", 1, made)
        fb.release()
    }

    @Test
    fun theSound_playsOnceLoaded_andTheRightOne() {
        lateinit var pool: SoundPool
        val fb = AndroidModeSwitchFeedback(context, vibrator = { null }, soundPool = { defaultSoundPool().also { pool = it } })
        fb.start(ModeSwitchKind.DROP)
        val shadow = shadowOf(pool)
        assertFalse("not before it has loaded", shadow.wasResourcePlayed(R.raw.grit_hammer_drop))
        shadow.notifyResourceLoaded(R.raw.grit_hammer_drop, true)
        shadow.notifyResourceLoaded(R.raw.grit_pump, true)
        ShadowLooper.idleMainLooper()
        assertTrue(shadow.wasResourcePlayed(R.raw.grit_hammer_drop))
        assertFalse(shadow.wasResourcePlayed(R.raw.grit_pump))

        // Loaded now: the pump plays at once.
        fb.start(ModeSwitchKind.PUMP)
        assertTrue(shadow.wasResourcePlayed(R.raw.grit_pump))
        fb.release()
    }

    @Test
    fun aSoundStoppedBeforeItLoaded_neverPlays() {
        lateinit var pool: SoundPool
        val fb = AndroidModeSwitchFeedback(context, vibrator = { null }, soundPool = { defaultSoundPool().also { pool = it } })
        fb.start(ModeSwitchKind.DROP)
        fb.stop()
        shadowOf(pool).notifyResourceLoaded(R.raw.grit_hammer_drop, true)
        ShadowLooper.idleMainLooper()
        assertFalse(shadowOf(pool).wasResourcePlayed(R.raw.grit_hammer_drop))
        fb.release()
    }

    @Test
    fun silentMode_playsNoSound_butStillVibrates() {
        val audio = context.getSystemService(AudioManager::class.java)
        audio.ringerMode = AudioManager.RINGER_MODE_SILENT
        try {
            lateinit var pool: SoundPool
            val v = FakeVibrator()
            val fb = AndroidModeSwitchFeedback(context, vibrator = { v }, soundPool = { defaultSoundPool().also { pool = it } })
            fb.start(ModeSwitchKind.DROP)
            runCatching { shadowOf(pool).notifyResourceLoaded(R.raw.grit_hammer_drop, true) }
            ShadowLooper.idleMainLooper()
            assertTrue(runCatching { !shadowOf(pool).wasResourcePlayed(R.raw.grit_hammer_drop) }.getOrDefault(true))
            assertEquals(1, v.played.size)
            fb.release()
        } finally {
            audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
    }

    private fun feedback(v: ModeSwitchVibrator) = AndroidModeSwitchFeedback(context, vibrator = { v }, soundPool = { defaultSoundPool() })
}
