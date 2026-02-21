package com.runtracker.app.safety

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class FakeCallActivity : ComponentActivity() {
    
    private var ringtonePlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    
    companion object {
        const val EXTRA_CALLER_NAME = "caller_name"
        const val EXTRA_ALREADY_RINGING = "already_ringing"
        const val FAKE_CALL_NOTIFICATION_ID = 9003
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        
        val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Mom"
        val alreadyRinging = intent.getBooleanExtra(EXTRA_ALREADY_RINGING, false)
        
        // Cancel the notification that triggered this activity
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(FAKE_CALL_NOTIFICATION_ID)
        
        // Only start ringing if SafetyService hasn't already started it
        if (!alreadyRinging) {
            startRinging()
        }
        
        setContent {
            FakeCallScreen(
                callerName = callerName,
                onAnswer = {
                    stopRinging()
                    // Stay on screen for "call" - user can exit manually
                },
                onDecline = {
                    stopRinging()
                    finish()
                }
            )
        }
    }
    
    private fun startRinging() {
        // Use ALARM stream to bypass silent/vibrate mode
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        
        // Play aggressive alarm sound (bypasses silent mode)
        try {
            ringtonePlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                // Use alarm sound for more aggressive tone
                setDataSource(this@FakeCallActivity, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Fallback to ringtone if alarm fails
            try {
                ringtonePlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@FakeCallActivity, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
        
        // Vibrate aggressively
        vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        
        // More aggressive vibration pattern
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500, 500)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }
    
    private fun stopRinging() {
        // Stop local ringing
        ringtonePlayer?.apply {
            stop()
            release()
        }
        ringtonePlayer = null
        vibrator?.cancel()
        
        // Also stop SafetyService ringing via broadcast
        val stopIntent = Intent(this, SafetyBroadcastReceiver::class.java).apply {
            action = SafetyBroadcastReceiver.ACTION_STOP_FAKE_CALL
        }
        sendBroadcast(stopIntent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopRinging()
    }
}

@Composable
fun FakeCallScreen(
    callerName: String,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    var isAnswered by remember { mutableStateOf(false) }
    var callDuration by remember { mutableStateOf(0) }
    
    // Pulse animation for incoming call
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Call timer when answered
    LaunchedEffect(isAnswered) {
        if (isAnswered) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                callDuration++
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Spacer(modifier = Modifier.height(80.dp))
            
            // Caller info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(if (!isAnswered) scale else 1f)
                        .background(Color(0xFF4a4e69), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = callerName.first().uppercase(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = callerName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isAnswered) {
                        val minutes = callDuration / 60
                        val seconds = callDuration % 60
                        String.format("%02d:%02d", minutes, seconds)
                    } else {
                        "Incoming call..."
                    },
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Call buttons
            if (!isAnswered) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(80.dp),
                    modifier = Modifier.padding(bottom = 48.dp)
                ) {
                    // Decline button
                    IconButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFFe63946), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Decline",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    
                    // Answer button
                    IconButton(
                        onClick = {
                            isAnswered = true
                            onAnswer()
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF2a9d8f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Answer",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            } else {
                // End call button when answered
                IconButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFFe63946), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
