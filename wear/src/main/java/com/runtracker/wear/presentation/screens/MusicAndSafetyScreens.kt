package com.runtracker.wear.presentation.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MusicControlPage(isAmbient: Boolean) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
    var isPlaying by remember { mutableStateOf(audioManager.isMusicActive) }

    fun sendMediaKey(keyCode: Int) {
        val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }

    if (isAmbient) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎵 MUSIC",
                style = MaterialTheme.typography.title2,
                color = Color.White
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🎵 Music",
                style = MaterialTheme.typography.title3,
                color = WearColors.Music
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS) },
                    modifier = Modifier.size(40.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) { Text("⏮") }

                Button(
                    onClick = {
                        sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                        isPlaying = !isPlaying
                    },
                    modifier = Modifier.size(52.dp),
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Text(if (isPlaying) "⏸" else "▶", style = MaterialTheme.typography.title2)
                }

                Button(
                    onClick = { sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_NEXT) },
                    modifier = Modifier.size(40.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) { Text("⏭") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Button(
                    onClick = {
                        audioManager.adjustStreamVolume(
                            android.media.AudioManager.STREAM_MUSIC,
                            android.media.AudioManager.ADJUST_LOWER, 0
                        )
                    },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) { Text("🔉") }

                Button(
                    onClick = {
                        audioManager.adjustStreamVolume(
                            android.media.AudioManager.STREAM_MUSIC,
                            android.media.AudioManager.ADJUST_RAISE, 0
                        )
                    },
                    modifier = Modifier.size(36.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) { Text("🔊") }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Swipe ↕ for more",
                style = MaterialTheme.typography.caption3,
                color = MaterialTheme.colors.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SafetyPage(isAmbient: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSosConfirmation by remember { mutableStateOf(false) }
    var sosSent by remember { mutableStateOf(false) }
    var panicSent by remember { mutableStateOf(false) }
    var fakeCallSent by remember { mutableStateOf(false) }

    if (isAmbient) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("🛡️ SAFETY", style = MaterialTheme.typography.title2, color = Color.White)
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("🛡️ Safety", style = MaterialTheme.typography.title2,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary)

            Button(
                onClick = { showSosConfirmation = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = WearColors.Danger),
                enabled = !sosSent
            ) {
                Text(
                    text = if (sosSent) "✓ SOS SENT" else "🆘 SOS",
                    style = MaterialTheme.typography.button,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Button(
                onClick = {
                    scope.launch(Dispatchers.Default) {
                        com.runtracker.wear.safety.WatchSafetyService.triggerPanicAlarm(context)
                    }
                    panicSent = true
                },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = WearColors.Warning),
                enabled = !panicSent
            ) {
                Text(
                    text = if (panicSent) "✓ ALARM ON" else "🚨 Alarm",
                    style = MaterialTheme.typography.button, color = Color.White
                )
            }

            Button(
                onClick = {
                    scope.launch(Dispatchers.Default) {
                        com.runtracker.wear.safety.WatchSafetyService.triggerFakeCall(context)
                    }
                    fakeCallSent = true
                },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = WearColors.Success),
                enabled = !fakeCallSent
            ) {
                Text(
                    text = if (fakeCallSent) "✓ CALLING" else "📞 Fake Call",
                    style = MaterialTheme.typography.button, color = Color.White
                )
            }
        }
    }

    if (showSosConfirmation) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showSosConfirmation = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(MaterialTheme.colors.surface,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🆘 Send SOS?", style = MaterialTheme.typography.title3,
                        fontWeight = FontWeight.Bold, color = WearColors.Danger)
                    Text("This will send an emergency SMS with your location to your contacts",
                        style = MaterialTheme.typography.caption1,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showSosConfirmation = false },
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)
                        ) { Text("Cancel") }
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.Default) {
                                    com.runtracker.wear.safety.WatchSafetyService.triggerSos(context)
                                }
                                sosSent = true
                                showSosConfirmation = false
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = WearColors.Danger)
                        ) { Text("SEND SOS", color = Color.White) }
                    }
                }
            }
        }
    }
}

@Composable
fun HardwareButtonSosDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .background(WearColors.Danger,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🆘 SOS ACTIVATED", style = MaterialTheme.typography.title2,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text("Send emergency alert to your contacts?",
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.9f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.White.copy(alpha = 0.2f))
                    ) { Text("Cancel", color = Color.White) }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White)
                    ) { Text("SEND SOS", color = WearColors.Danger, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
