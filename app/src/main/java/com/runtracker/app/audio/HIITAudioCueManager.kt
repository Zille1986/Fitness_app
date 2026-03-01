package com.runtracker.app.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages audio cues for HIIT workouts on the phone.
 *
 * Audio cue schedule:
 * - 3 seconds before transition: beep (low)
 * - 2 seconds before transition: beep (medium)
 * - 1 second before transition:  beep (high)
 * - On transition to WORK:       GO! tone
 * - On transition to REST:       REST tone
 * - Workout complete:            ascending 3-tone melody
 */
@Singleton
class HIITAudioCueManager @Inject constructor(
    private val context: Context
) {
    private var toneGenerator: ToneGenerator? = null
    private var isMuted: Boolean = false
    private val handler = Handler(Looper.getMainLooper())

    private fun ensureToneGenerator(): ToneGenerator? {
        if (toneGenerator == null) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            } catch (e: Exception) {
                android.util.Log.e("HIITAudio", "Failed to create ToneGenerator", e)
            }
        }
        return toneGenerator
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    /**
     * Called every second by the workout timer.
     * @param remainingSeconds seconds left in the current phase
     * @param isWorkPhase true if currently in WORK phase, false for REST/WARMUP/COOLDOWN
     */
    fun onTick(remainingSeconds: Int, isWorkPhase: Boolean) {
        if (isMuted) return

        when (remainingSeconds) {
            3 -> playCountdownBeep(low = true)
            2 -> playCountdownBeep(low = false)
            1 -> playCountdownBeepHigh()
            0 -> {
                // Transition tone — next phase is opposite of current
                if (isWorkPhase) playRestTone() else playGoTone()
            }
        }
    }

    /** Short low-pitched beep for 3s and 2s countdown */
    private fun playCountdownBeep(low: Boolean) {
        val tone = if (low) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_PROP_BEEP2
        ensureToneGenerator()?.startTone(tone, 150)
    }

    /** Higher-pitched beep for 1s countdown */
    private fun playCountdownBeepHigh() {
        ensureToneGenerator()?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
    }

    /** Distinct GO! tone when exercise starts */
    fun playGoTone() {
        if (isMuted) return
        ensureToneGenerator()?.startTone(ToneGenerator.TONE_DTMF_D, 400)
    }

    /** Lower REST tone when rest period starts */
    fun playRestTone() {
        if (isMuted) return
        ensureToneGenerator()?.startTone(ToneGenerator.TONE_DTMF_0, 400)
    }

    /** Ascending 3-tone melody when workout completes */
    fun playCompleteTone() {
        if (isMuted) return
        val gen = ensureToneGenerator() ?: return
        gen.startTone(ToneGenerator.TONE_DTMF_1, 200)
        handler.postDelayed({ gen.startTone(ToneGenerator.TONE_DTMF_5, 200) }, 250)
        handler.postDelayed({ gen.startTone(ToneGenerator.TONE_DTMF_9, 400) }, 500)
    }

    /** Warmup/cooldown start tone */
    fun playPhaseTone() {
        if (isMuted) return
        ensureToneGenerator()?.startTone(ToneGenerator.TONE_PROP_PROMPT, 300)
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
        handler.removeCallbacksAndMessages(null)
    }
}
