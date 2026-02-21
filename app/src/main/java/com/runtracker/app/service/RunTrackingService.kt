package com.runtracker.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.runtracker.app.MainActivity
import com.runtracker.app.R
import com.runtracker.app.RunTrackerApplication
import com.runtracker.shared.data.model.RoutePoint
import com.runtracker.shared.data.model.Run
import com.runtracker.shared.data.model.RunSource
import com.runtracker.shared.data.repository.GamificationRepository
import com.runtracker.shared.data.repository.PersonalBestRepository
import com.runtracker.shared.data.repository.RunRepository
import com.runtracker.app.strava.StravaService
import com.runtracker.shared.location.LocationTracker
import com.runtracker.shared.location.LocationTracker.Companion.calculateDistance
import com.runtracker.shared.location.LocationTracker.Companion.calculateElevationGain
import com.runtracker.shared.location.LocationTracker.Companion.calculateElevationLoss
import com.runtracker.shared.location.LocationTracker.Companion.toRoutePoint
import com.runtracker.shared.location.RunCalculator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class RunTrackingService : Service() {

    @Inject
    lateinit var locationTracker: LocationTracker

    @Inject
    lateinit var runRepository: RunRepository
    
    @Inject
    lateinit var stravaService: StravaService
    
    @Inject
    lateinit var personalBestRepository: PersonalBestRepository
    
    @Inject
    lateinit var gamificationRepository: GamificationRepository

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var locationJob: Job? = null
    private var timerJob: Job? = null
    private val stateMutex = Mutex()

    private val _trackingState = MutableStateFlow(TrackingState())
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private var currentRunId: Long? = null
    private val routePoints = mutableListOf<RoutePoint>()
    private var startTime: Long = 0
    private var pausedDuration: Long = 0
    private var lastPauseTime: Long = 0

    inner class LocalBinder : Binder() {
        fun getService(): RunTrackingService = this@RunTrackingService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    fun startTracking() {
        if (_trackingState.value.isTracking) return

        startTime = System.currentTimeMillis()
        routePoints.clear()
        pausedDuration = 0

        // Insert run and wait for ID before continuing
        serviceScope.launch {
            val run = Run(
                startTime = startTime,
                source = RunSource.PHONE
            )
            currentRunId = runRepository.insertRun(run)
            android.util.Log.d("RunTrackingService", "Run started with ID: $currentRunId")
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()
        startTimer()

        serviceScope.launch {
            stateMutex.withLock {
                _trackingState.value = TrackingState(
                    isTracking = true,
                    isPaused = false
                )
            }
        }
    }

    fun pauseTracking() {
        if (!_trackingState.value.isTracking || _trackingState.value.isPaused) return

        lastPauseTime = System.currentTimeMillis()
        locationJob?.cancel()
        timerJob?.cancel()

        serviceScope.launch {
            stateMutex.withLock {
                _trackingState.value = _trackingState.value.copy(isPaused = true)
            }
        }
        updateNotification()
    }

    fun resumeTracking() {
        if (!_trackingState.value.isTracking || !_trackingState.value.isPaused) return

        pausedDuration += System.currentTimeMillis() - lastPauseTime
        startLocationUpdates()
        startTimer()

        serviceScope.launch {
            stateMutex.withLock {
                _trackingState.value = _trackingState.value.copy(isPaused = false)
            }
        }
        updateNotification()
    }

    fun stopTracking() {
        if (!_trackingState.value.isTracking) return

        locationJob?.cancel()
        timerJob?.cancel()

        android.util.Log.d("RunTrackingService", "stopTracking called, currentRunId: $currentRunId")

        serviceScope.launch {
            // Wait briefly if currentRunId hasn't been set yet (race condition fix)
            var attempts = 0
            while (currentRunId == null && attempts < 10) {
                android.util.Log.d("RunTrackingService", "Waiting for currentRunId... attempt $attempts")
                kotlinx.coroutines.delay(100)
                attempts++
            }
            
            currentRunId?.let { runId ->
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime - pausedDuration
                val distance = calculateDistance(routePoints)
                val pace = RunCalculator.calculatePace(distance, duration)
                val splits = RunCalculator.calculateSplits(routePoints)
                val elevationGain = calculateElevationGain(routePoints)
                val elevationLoss = calculateElevationLoss(routePoints)
                val avgHeartRate = RunCalculator.calculateAverageHeartRate(routePoints)
                val maxHeartRate = RunCalculator.calculateMaxHeartRate(routePoints)
                val calories = RunCalculator.calculateCalories(distance, duration, null, avgHeartRate)

                val completedRun = Run(
                    id = runId,
                    startTime = startTime,
                    endTime = endTime,
                    distanceMeters = distance,
                    durationMillis = duration,
                    avgPaceSecondsPerKm = pace,
                    avgHeartRate = avgHeartRate,
                    maxHeartRate = maxHeartRate,
                    caloriesBurned = calories,
                    elevationGainMeters = elevationGain,
                    elevationLossMeters = elevationLoss,
                    routePoints = routePoints.toList(),
                    splits = splits,
                    source = RunSource.PHONE,
                    isCompleted = true
                )
                android.util.Log.d("RunTrackingService", "Saving completed run: id=$runId, distance=${distance}m, duration=${duration}ms, routePoints=${routePoints.size}")
                runRepository.updateRun(completedRun)
                android.util.Log.d("RunTrackingService", "Run saved successfully to database")
                
                // Check for personal bests
                try {
                    val pbUpdates = personalBestRepository.checkAndUpdatePersonalBests(completedRun)
                    if (pbUpdates.isNotEmpty()) {
                        android.util.Log.d("RunTrackingService", "New PBs: ${pbUpdates.map { it.distanceName }}")
                        // Award XP for each PB
                        pbUpdates.forEach { _ ->
                            gamificationRepository.recordPersonalBest(completedRun.id)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Record workout for gamification (XP, streaks, achievements)
                try {
                    val durationMinutes = (completedRun.durationMillis / 60000).toInt()
                    gamificationRepository.recordWorkoutCompleted(
                        distanceMeters = completedRun.distanceMeters,
                        durationMinutes = durationMinutes,
                        runId = completedRun.id
                    )
                    android.util.Log.d("RunTrackingService", "Gamification updated for run ${completedRun.id}")
                } catch (e: Exception) {
                    android.util.Log.e("RunTrackingService", "Error updating gamification", e)
                }
                
                // Auto-upload to Strava if connected
                android.util.Log.d("RunTrackingService", "Strava connected: ${stravaService.isConnected}")
                if (stravaService.isConnected) {
                    try {
                        android.util.Log.d("RunTrackingService", "Uploading to Strava...")
                        stravaService.uploadRun(completedRun)
                        android.util.Log.d("RunTrackingService", "Strava upload complete")
                    } catch (e: Exception) {
                        android.util.Log.e("RunTrackingService", "Strava upload failed", e)
                    }
                }
            } ?: run {
                android.util.Log.e("RunTrackingService", "ERROR: currentRunId is null, run was not saved!")
            }
        }

        _trackingState.value = TrackingState()
        currentRunId = null
        routePoints.clear()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startLocationUpdates() {
        locationJob = serviceScope.launch {
            locationTracker.getLocationUpdates().collect { location ->
                val routePoint = location.toRoutePoint()
                routePoints.add(routePoint)

                val distance = calculateDistance(routePoints)
                val duration = System.currentTimeMillis() - startTime - pausedDuration
                val pace = RunCalculator.calculatePace(distance, duration)

                stateMutex.withLock {
                    _trackingState.value = _trackingState.value.copy(
                        distanceMeters = distance,
                        durationMillis = duration,
                        currentPaceSecondsPerKm = pace,
                        currentLocation = routePoint,
                        routePoints = routePoints.toList()
                    )
                }

                updateNotification()
            }
        }
    }

    private fun startTimer() {
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                if (!_trackingState.value.isPaused) {
                    val duration = System.currentTimeMillis() - startTime - pausedDuration
                    stateMutex.withLock {
                        _trackingState.value = _trackingState.value.copy(durationMillis = duration)
                    }
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val state = _trackingState.value
        val distanceKm = state.distanceMeters / 1000.0
        val paceFormatted = Run.formatPace(state.currentPaceSecondsPerKm)

        return NotificationCompat.Builder(this, RunTrackerApplication.TRACKING_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_tracking_title))
            .setContentText("%.2f km • %s /km".format(distanceKm, paceFormatted))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_ID = 1
    }
}

data class TrackingState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val distanceMeters: Double = 0.0,
    val durationMillis: Long = 0,
    val currentPaceSecondsPerKm: Double = 0.0,
    val currentHeartRate: Int? = null,
    val currentLocation: RoutePoint? = null,
    val routePoints: List<RoutePoint> = emptyList()
) {
    val distanceKm: Double get() = distanceMeters / 1000.0
    
    val durationFormatted: String get() {
        val totalSeconds = durationMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    val paceFormatted: String get() = Run.formatPace(currentPaceSecondsPerKm)
}
