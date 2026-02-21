package com.runtracker.wear.presentation

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*

@Composable
fun MusicControlScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var isPlaying by remember { mutableStateOf(audioManager.isMusicActive) }
    var volume by remember { 
        mutableStateOf(
            (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / 
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        ) 
    }

    fun sendMediaKey(keyCode: Int) {
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }

    fun updateVolume() {
        volume = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / 
                 audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🎵 Music",
                style = MaterialTheme.typography.title3,
                color = MaterialTheme.colors.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Volume indicator
            Text(
                text = "Vol: $volume%",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Main controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous
                Button(
                    onClick = { sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS) },
                    modifier = Modifier.size(40.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text("⏮", textAlign = TextAlign.Center)
                }

                // Play/Pause
                Button(
                    onClick = { 
                        sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                        isPlaying = !isPlaying
                    },
                    modifier = Modifier.size(52.dp),
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Text(
                        text = if (isPlaying) "⏸" else "▶",
                        style = MaterialTheme.typography.title2,
                        textAlign = TextAlign.Center
                    )
                }

                // Next
                Button(
                    onClick = { sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) },
                    modifier = Modifier.size(40.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text("⏭", textAlign = TextAlign.Center)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Volume controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Volume down
                Button(
                    onClick = { 
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            0
                        )
                        updateVolume()
                    },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text("🔉", textAlign = TextAlign.Center)
                }

                // Volume up
                Button(
                    onClick = { 
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            0
                        )
                        updateVolume()
                    },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text("🔊", textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun MusicControlChip(
    onClick: () -> Unit,
    isPlaying: Boolean
) {
    Chip(
        onClick = onClick,
        label = { Text("Music Controls") },
        icon = {
            Text(if (isPlaying) "🎵" else "🔇")
        },
        colors = ChipDefaults.secondaryChipColors(),
        modifier = Modifier.fillMaxWidth()
    )
}
