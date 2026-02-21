package com.runtracker.app.media

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.view.KeyEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicControlManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _playbackState = MutableStateFlow(MusicPlaybackState())
    val playbackState: StateFlow<MusicPlaybackState> = _playbackState.asStateFlow()

    private val _currentTrack = MutableStateFlow<TrackInfo?>(null)
    val currentTrack: StateFlow<TrackInfo?> = _currentTrack.asStateFlow()

    private var mediaController: MediaController? = null

    fun playPause() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        updatePlaybackState()
    }

    fun next() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun previous() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    fun play() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)
        _playbackState.value = _playbackState.value.copy(isPlaying = true)
    }

    fun pause() {
        sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE)
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    private fun sendMediaKeyEvent(keyCode: Int) {
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        
        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }

    fun updatePlaybackState() {
        val isPlaying = audioManager.isMusicActive
        _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
    }

    fun isMusicPlaying(): Boolean {
        return audioManager.isMusicActive
    }

    fun setVolume(level: Int) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = (level * maxVolume / 100).coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        _playbackState.value = _playbackState.value.copy(volume = level)
    }

    fun getVolume(): Int {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return if (maxVolume > 0) (currentVolume * 100 / maxVolume) else 0
    }

    fun volumeUp() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            0
        )
        _playbackState.value = _playbackState.value.copy(volume = getVolume())
    }

    fun volumeDown() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            0
        )
        _playbackState.value = _playbackState.value.copy(volume = getVolume())
    }
}

data class MusicPlaybackState(
    val isPlaying: Boolean = false,
    val volume: Int = 50,
    val hasActiveSession: Boolean = false
)

data class TrackInfo(
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long = 0,
    val positionMs: Long = 0
)
