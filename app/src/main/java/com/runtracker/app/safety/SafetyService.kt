package com.runtracker.app.safety

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.runtracker.app.MainActivity
import com.runtracker.app.R
import com.runtracker.shared.data.model.EmergencyContact
import com.runtracker.shared.data.model.SafetySettings
import com.runtracker.shared.data.repository.SafetyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafetyService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safetyRepository: SafetyRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var panicAlarmPlayer: MediaPlayer? = null
    private var fakeCallPlayer: MediaPlayer? = null
    private var fakeCallVibrator: Vibrator? = null
    private var isPanicAlarmActive = false
    private var checkInJob: Job? = null
    private var fakeCallJob: Job? = null
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val notificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        private const val SAFETY_CHANNEL_ID = "safety_alerts"
        private const val SOS_NOTIFICATION_ID = 9001
        private const val CHECK_IN_NOTIFICATION_ID = 9002
        private const val FAKE_CALL_NOTIFICATION_ID = 9003
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            SAFETY_CHANNEL_ID,
            "Safety Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Emergency safety alerts and notifications"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
        }
        notificationManager.createNotificationChannel(channel)
    }
    
    // ============ EMERGENCY SOS ============
    
    suspend fun triggerSos(): Result<Unit> {
        return try {
            val settings = safetyRepository.getSettingsOnce()
            if (settings.emergencyContacts.isEmpty()) {
                return Result.failure(Exception("No emergency contacts configured"))
            }
            
            val location = getCurrentLocation()
            val message = buildSosMessage(settings, location)
            
            // Send SMS to all emergency contacts
            settings.emergencyContacts
                .filter { it.notifyOnSos }
                .forEach { contact ->
                    sendSms(contact.phoneNumber, message)
                }
            
            // Show notification
            showSosNotification()
            
            // Update any active check-in session
            safetyRepository.getActiveCheckInSession()?.let { session ->
                safetyRepository.triggerSos(session.id)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun getCurrentLocation(): Location? {
        return try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
                return null
            }
            
            val cancellationToken = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationToken.token
            ).await()
        } catch (e: Exception) {
            null
        }
    }
    
    private fun buildSosMessage(settings: SafetySettings, location: Location?): String {
        val sb = StringBuilder()
        sb.append("🆘 EMERGENCY ALERT\n\n")
        sb.append(settings.sosMessage)
        sb.append("\n\n")
        
        if (location != null) {
            sb.append("📍 Location:\n")
            sb.append("Lat: ${location.latitude}\n")
            sb.append("Lng: ${location.longitude}\n\n")
            sb.append("Google Maps:\n")
            sb.append("https://maps.google.com/?q=${location.latitude},${location.longitude}")
        } else {
            sb.append("⚠️ Location unavailable")
        }
        
        return sb.toString()
    }
    
    private fun sendSms(phoneNumber: String, message: String) {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.e("SafetyService", "SMS permission not granted")
                return
            }
            
            val smsManager = context.getSystemService(SmsManager::class.java)
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            android.util.Log.d("SafetyService", "SOS SMS sent to $phoneNumber")
        } catch (e: Exception) {
            android.util.Log.e("SafetyService", "Failed to send SMS: ${e.message}")
        }
    }
    
    private fun showSosNotification() {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, SAFETY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🆘 Emergency SOS Sent")
            .setContentText("Your emergency contacts have been notified with your location")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(SOS_NOTIFICATION_ID, notification)
    }
    
    // ============ PANIC ALARM ============
    
    fun startPanicAlarm() {
        if (isPanicAlarmActive) return
        
        isPanicAlarmActive = true
        
        // Set volume to max
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        
        // Play loud alarm sound
        try {
            panicAlarmPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Fallback to notification sound
            try {
                panicAlarmPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e2: Exception) {
                android.util.Log.e("SafetyService", "Failed to play alarm: ${e2.message}")
            }
        }
        
        // Vibrate continuously
        startContinuousVibration()
        
        // Show notification with stop button
        showPanicAlarmNotification()
    }
    
    fun stopPanicAlarm() {
        isPanicAlarmActive = false
        
        panicAlarmPlayer?.apply {
            stop()
            release()
        }
        panicAlarmPlayer = null
        
        stopVibration()
        notificationManager.cancel(SOS_NOTIFICATION_ID)
    }
    
    private fun startContinuousVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500, 500)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }
    
    private fun stopVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.cancel()
    }
    
    private fun showPanicAlarmNotification() {
        val stopIntent = Intent(context, SafetyBroadcastReceiver::class.java).apply {
            action = SafetyBroadcastReceiver.ACTION_STOP_PANIC_ALARM
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, SAFETY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🚨 PANIC ALARM ACTIVE")
            .setContentText("Tap to stop the alarm")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .addAction(R.drawable.ic_launcher_foreground, "STOP ALARM", stopPendingIntent)
            .build()
        
        notificationManager.notify(SOS_NOTIFICATION_ID, notification)
    }
    
    // ============ CHECK-IN TIMER ============
    
    fun startCheckInTimer(durationMinutes: Int, activityType: String) {
        scope.launch {
            val sessionId = safetyRepository.startCheckInSession(durationMinutes, activityType)
            
            checkInJob?.cancel()
            checkInJob = scope.launch {
                delay(durationMinutes * 60 * 1000L)
                
                // Check if still active (not checked in yet)
                val session = safetyRepository.getActiveCheckInSession()
                if (session != null && session.id == sessionId && !session.checkedIn) {
                    triggerCheckInMissed()
                }
            }
            
            showCheckInStartedNotification(durationMinutes)
        }
    }
    
    fun checkIn() {
        scope.launch {
            safetyRepository.getActiveCheckInSession()?.let { session ->
                safetyRepository.checkIn(session.id)
                checkInJob?.cancel()
                notificationManager.cancel(CHECK_IN_NOTIFICATION_ID)
                showCheckInConfirmedNotification()
            }
        }
    }
    
    fun cancelCheckIn() {
        scope.launch {
            safetyRepository.getActiveCheckInSession()?.let { session ->
                safetyRepository.cancelCheckIn(session.id)
                checkInJob?.cancel()
                notificationManager.cancel(CHECK_IN_NOTIFICATION_ID)
            }
        }
    }
    
    private suspend fun triggerCheckInMissed() {
        val settings = safetyRepository.getSettingsOnce()
        val location = getCurrentLocation()
        
        val message = buildCheckInMissedMessage(settings, location)
        
        // Send SMS to emergency contacts
        settings.emergencyContacts
            .filter { it.notifyOnCheckInMissed }
            .forEach { contact ->
                sendSms(contact.phoneNumber, message)
            }
        
        showCheckInMissedNotification()
    }
    
    private fun buildCheckInMissedMessage(settings: SafetySettings, location: Location?): String {
        val sb = StringBuilder()
        sb.append("⚠️ CHECK-IN MISSED\n\n")
        sb.append("I started an activity and haven't checked back in as expected.\n\n")
        
        if (location != null) {
            sb.append("📍 Last known location:\n")
            sb.append("https://maps.google.com/?q=${location.latitude},${location.longitude}")
        } else {
            sb.append("⚠️ Location unavailable")
        }
        
        return sb.toString()
    }
    
    private fun showCheckInStartedNotification(minutes: Int) {
        val checkInIntent = Intent(context, SafetyBroadcastReceiver::class.java).apply {
            action = SafetyBroadcastReceiver.ACTION_CHECK_IN
        }
        val checkInPendingIntent = PendingIntent.getBroadcast(
            context, 0, checkInIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, SAFETY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏱️ Check-in Timer Active")
            .setContentText("Tap 'I'm Safe' when you return (${minutes} min)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .addAction(R.drawable.ic_launcher_foreground, "I'M SAFE", checkInPendingIntent)
            .build()
        
        notificationManager.notify(CHECK_IN_NOTIFICATION_ID, notification)
    }
    
    private fun showCheckInConfirmedNotification() {
        val notification = NotificationCompat.Builder(context, SAFETY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("✅ Checked In")
            .setContentText("Glad you're safe!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(CHECK_IN_NOTIFICATION_ID, notification)
    }
    
    private fun showCheckInMissedNotification() {
        val notification = NotificationCompat.Builder(context, SAFETY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠️ Check-in Missed")
            .setContentText("Emergency contacts have been notified")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(CHECK_IN_NOTIFICATION_ID, notification)
    }
    
    // ============ FAKE CALL ============
    
    fun triggerFakeCall(delaySeconds: Int = 5) {
        fakeCallJob?.cancel()
        fakeCallJob = scope.launch {
            delay(delaySeconds * 1000L)
            showFakeCallScreen()
        }
    }
    
    fun cancelFakeCall() {
        fakeCallJob?.cancel()
    }
    
    private suspend fun showFakeCallScreen() {
        val settings = safetyRepository.getSettingsOnce()
        
        // Start ringing immediately (bypasses silent mode using ALARM stream)
        startFakeCallRinging()
        
        // Create intent for fake call activity
        val intent = Intent(context, FakeCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(FakeCallActivity.EXTRA_CALLER_NAME, settings.fakeCallerName)
            putExtra(FakeCallActivity.EXTRA_ALREADY_RINGING, true)
        }
        
        // Use full-screen intent notification to bypass background activity launch restrictions
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 
            FAKE_CALL_NOTIFICATION_ID, 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create a high-priority notification with full-screen intent
        val notification = NotificationCompat.Builder(context, SAFETY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Incoming Call")
            .setContentText(settings.fakeCallerName)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()
        
        notificationManager.notify(FAKE_CALL_NOTIFICATION_ID, notification)
        android.util.Log.d("SafetyService", "Fake call notification shown with full-screen intent")
    }
    
    private fun startFakeCallRinging() {
        // Use ALARM stream to bypass silent/vibrate mode
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        
        // Play aggressive alarm sound
        try {
            fakeCallPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Fallback to ringtone
            try {
                fakeCallPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e2: Exception) {
                android.util.Log.e("SafetyService", "Failed to play fake call sound", e2)
            }
        }
        
        // Vibrate aggressively
        fakeCallVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500, 500)
        fakeCallVibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }
    
    fun stopFakeCallRinging() {
        fakeCallPlayer?.apply {
            stop()
            release()
        }
        fakeCallPlayer = null
        fakeCallVibrator?.cancel()
        fakeCallVibrator = null
        notificationManager.cancel(FAKE_CALL_NOTIFICATION_ID)
    }
    
    // ============ UTILITY ============
    
    fun hasEmergencyContacts(): Boolean {
        return runBlocking {
            safetyRepository.getSettingsOnce().emergencyContacts.isNotEmpty()
        }
    }
    
    fun cleanup() {
        stopPanicAlarm()
        checkInJob?.cancel()
        fakeCallJob?.cancel()
        scope.cancel()
    }
}
