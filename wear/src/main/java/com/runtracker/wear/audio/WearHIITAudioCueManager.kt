package com.runtracker.wear.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Manages audio cues AND haptic feedback for HIIT workouts on Wear OS.
 *
 * Audio cue schedule (same as phone):
 * - 3s before transition: beep + short vibration
 * - 2s before transition: beep + short vibration
 * - 1s before transition: high beep + short vibration
 * - WORK starts:          GO! tone + strong double-pulse
 * - REST starts:          REST tone + single medium pulse
 * - Complete:             melody + long buzz
 */
class WearHIITAudioCueManager(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    private var isMuted: Boolean = false
    private val handler = Handler(Looper.getMainLooper())

    init {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun ensureToneGenerator(): ToneGenerator? {
        if (toneGenerator == null) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            } catch (e: Exception) {
                android.util.Log.e("WearHIITAudio", "Failed to create ToneGenerator", e)
            }
        }
        return toneGenerator
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    /**
     * Called every second by the workout timer.
     */
    fun onTick(remainingSeconds: Int, isWorkPhase: Boolean) {
        when (remainingSeconds) {
            3 -> {
                if (!isMuted) playCountdownBeep(low = true)
                vibrateShort()
            }
            2 -> {
                if (!isMuted) playCountdownBeep(low = false)
                vibrateShort()
            }
            1 -> {
                if (!isMuted) playCountdownBeepHigh()
                vibrateShort()
            }
            0 -> {
                if (isWorkPhase) {
                    if (!isMuted) playRestTone()
                    vibrateMedium()
                } else {
                    if (!isMuted) playGoTone()
                    vibrateStrong()
                }
            }
        }
    }

    private fun playCountdownBeep(low: Boolean) {
        val tone = if (low) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_PROP_BEEP2
        ensureToneGenerator()?.startTone(tone, 150)
    }

    private fun playCountdownBeepHigh() {
        ensureToneGenerator()?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
    }

    fun playGoTone() {
        if (!isMuted) ensureToneGenerator()?.startTone(ToneGenerator.TONE_DTMF_D, 400)
        vibrateStrong()
    }

    fun playRestTone() {
        if (!isMuted) ensureToneGenerator()?.startTone(ToneGenerator.TONE_DTMF_0, 400)
        vibrateMedium()
    }

    fun playCompleteTone() {
        val gen = if (!isMuted) ensureToneGenerator() else null
        gen?.startTone(ToneGenerator.TONE_DTMF_1, 200)
        handler.postDelayed({ gen?.startTone(ToneGenerator.TONE_DTMF_5, 200) }, 250)
        handler.postDelayed({ gen?.startTone(ToneGenerator.TONE_DTMF_9, 400) }, 500)
        vibrateLong()
    }

    fun playPhaseTone() {
        if (!isMuted) ensureToneGenerator()?.startTone(ToneGenerator.TONE_PROP_PROMPT, 300)
        vibrateShort()
    }

    // ── Haptic patterns ─────────────────────────────────────────────

    /** Short pulse — countdown ticks */
    private fun vibrateShort() {
        vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /** Medium single pulse — REST transition */
    private fun vibrateMedium() {
        vibrator?.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /** Strong double-pulse — GO! transition */
    private fun vibrateStrong() {
        val timings = longArrayOf(0, 100, 50, 150) // pause, buzz, pause, buzz
        val amplitudes = intArrayOf(0, 255, 0, 255)
        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    /** Long buzz pattern — workout complete */
    private fun vibrateLong() {
        val timings = longArrayOf(0, 200, 100, 200, 100, 400)
        val amplitudes = intArrayOf(0, 200, 0, 200, 0, 255)
        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
        handler.removeCallbacksAndMessages(null)
    }
}
