package com.runtracker.wear.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.data.*
import com.google.android.gms.wearable.Wearable
import com.runtracker.shared.data.model.RoutePoint
import com.runtracker.shared.data.model.Run
import com.runtracker.shared.data.model.RunSource
import com.runtracker.shared.data.model.SwimmingWorkout
import com.runtracker.shared.data.model.CyclingWorkout
import com.runtracker.shared.data.model.SwimType
import com.runtracker.shared.data.model.SwimSource
import com.runtracker.shared.data.model.CyclingSource
import com.runtracker.shared.data.model.CyclingType
import com.runtracker.shared.data.model.PoolLength
import com.runtracker.shared.data.model.StrokeType
import com.runtracker.shared.location.LocationTracker.Companion.calculateDistance
import com.runtracker.shared.location.LocationTracker.Companion.calculateElevationGain
import com.runtracker.shared.location.LocationTracker.Companion.calculateElevationLoss
import com.runtracker.shared.location.RunCalculator
import com.runtracker.shared.sync.DataLayerPaths
import com.runtracker.shared.util.CompressionUtils
import com.runtracker.wear.WearGoSteadyApplication
import com.google.android.gms.wearable.Asset
import com.runtracker.wear.presentation.MainActivity
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.tasks.await

class WearRunTrackingService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var exerciseClient: ExerciseClient
    private val gson = Gson()
    private var vibrator: Vibrator? = null
    private var lastPaceAlert = PaceAlert.IN_ZONE
    private var lastHrAlert = HrAlert.IN_ZONE

    private val _trackingState = MutableStateFlow(WearTrackingState())
    val trackingState: StateFlow<WearTrackingState> = _trackingState.asStateFlow()

    private val routePoints = mutableListOf<RoutePoint>()
    private var startTime: Long = 0
    private var timerJob: Job? = null
    private var swimPoolLengthMeters: Int = 25
    private var totalPausedMillis: Long = 0
    private var pauseStartTime: Long = 0

    // Interval tracking
    private var intervals: List<com.runtracker.shared.data.model.Interval> = emptyList()
    private var currentIntervalIndex = 0
    private var intervalStartTime: Long = 0
    private var currentRepetition = 1

    inner class LocalBinder : Binder() {
        fun getService(): WearRunTrackingService = this@WearRunTrackingService
    }

    override fun onCreate() {
        super.onCreate()
        exerciseClient = HealthServices.getClient(this).exerciseClient
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    
    private fun vibrateAlert(isHighAlert: Boolean) {
        vibrator?.let { v ->
            if (v.hasVibrator()) {
                val pattern = if (isHighAlert) {
                    // Fast double vibration for "too high/fast"
                    longArrayOf(0, 100, 100, 100)
                } else {
                    // Slow single vibration for "too low/slow"
                    longArrayOf(0, 300)
                }
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
        }
    }
    
    private fun checkZoneAlerts(pace: Double, heartRate: Int?, state: WearTrackingState): Pair<PaceAlert, HrAlert> {
        var paceAlert = PaceAlert.IN_ZONE
        var hrAlert = HrAlert.IN_ZONE
        
        // Check pace (lower pace = faster, higher pace = slower)
        state.targetPaceMin?.let { minPace ->
            state.targetPaceMax?.let { maxPace ->
                paceAlert = when {
                    pace > 0 && pace < minPace -> PaceAlert.TOO_FAST
                    pace > maxPace -> PaceAlert.TOO_SLOW
                    else -> PaceAlert.IN_ZONE
                }
            }
        }
        
        // Check heart rate
        heartRate?.let { hr ->
            state.targetHrMin?.let { minHr ->
                state.targetHrMax?.let { maxHr ->
                    hrAlert = when {
                        hr < minHr -> HrAlert.TOO_LOW
                        hr > maxHr -> HrAlert.TOO_HIGH
                        else -> HrAlert.IN_ZONE
                    }
                }
            }
        }
        
        // Vibrate on zone change
        if (paceAlert != lastPaceAlert && paceAlert != PaceAlert.IN_ZONE) {
            vibrateAlert(paceAlert == PaceAlert.TOO_FAST)
        }
        if (hrAlert != lastHrAlert && hrAlert != HrAlert.IN_ZONE) {
            vibrateAlert(hrAlert == HrAlert.TOO_HIGH)
        }
        
        lastPaceAlert = paceAlert
        lastHrAlert = hrAlert
        
        return Pair(paceAlert, hrAlert)
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
        totalPausedMillis = 0
        pauseStartTime = 0
        routePoints.clear()

        serviceScope.launch {
            try {
                // Determine activity type and exercise type
                val activityType = WorkoutHolder.pendingActivityType ?: "RUNNING"
                val capabilities = exerciseClient.getCapabilitiesAsync().await()
                
                // Try to use the specific exercise type, fall back to RUNNING if not supported
                val preferredExerciseType = when (activityType) {
                    "SWIMMING" -> ExerciseType.SWIMMING_POOL
                    "CYCLING" -> ExerciseType.BIKING
                    else -> ExerciseType.RUNNING
                }
                
                val exerciseType = if (capabilities.supportedExerciseTypes.contains(preferredExerciseType)) {
                    preferredExerciseType
                } else {
                    // Fall back to RUNNING which is always supported
                    android.util.Log.w("WearTracking", "Exercise type $preferredExerciseType not supported, using RUNNING")
                    ExerciseType.RUNNING
                }
                
                val activityCapabilities = capabilities.getExerciseTypeCapabilities(exerciseType)

                val dataTypes = mutableSetOf<DataType<*, *>>()
                
                if (activityCapabilities.supportedDataTypes.contains(DataType.HEART_RATE_BPM)) {
                    dataTypes.add(DataType.HEART_RATE_BPM)
                }
                if (activityCapabilities.supportedDataTypes.contains(DataType.LOCATION)) {
                    dataTypes.add(DataType.LOCATION)
                }
                if (activityCapabilities.supportedDataTypes.contains(DataType.DISTANCE_TOTAL)) {
                    dataTypes.add(DataType.DISTANCE_TOTAL)
                }
                if (activityCapabilities.supportedDataTypes.contains(DataType.SPEED)) {
                    dataTypes.add(DataType.SPEED)
                }

                // Capture all pending data BEFORE clearing
                val pendingWorkout = WorkoutHolder.pendingWorkout
                val pendingIntervals = WorkoutHolder.pendingIntervals
                val pendingSwimType = WorkoutHolder.pendingSwimType
                val pendingCyclingType = WorkoutHolder.pendingCyclingType
                val pendingPoolLength = WorkoutHolder.pendingPoolLength ?: 25
                val targetHrMin = WorkoutHolder.pendingTargetHrMin
                val targetHrMax = WorkoutHolder.pendingTargetHrMax
                val targetHrZone = WorkoutHolder.pendingTargetHrZone
                val workoutDuration = WorkoutHolder.pendingWorkoutDuration
                
                // Store pool length for distance calculation
                swimPoolLengthMeters = pendingPoolLength

                // Clear pending data atomically after capture
                WorkoutHolder.clearAll()

                // Enable GPS for outdoor activities
                val enableGps = activityType == "RUNNING" || 
                    (activityType == "CYCLING" && pendingCyclingType == "OUTDOOR") ||
                    (activityType == "SWIMMING" && pendingSwimType in listOf("OCEAN", "LAKE", "OPEN_WATER"))

                val exerciseConfigBuilder = ExerciseConfig.builder(exerciseType)
                    .setDataTypes(dataTypes)
                    .setIsAutoPauseAndResumeEnabled(false)
                    .setIsGpsEnabled(enableGps)
                
                // Swimming pool requires pool length
                if (exerciseType == ExerciseType.SWIMMING_POOL) {
                    exerciseConfigBuilder.setSwimmingPoolLengthMeters(pendingPoolLength.toFloat())
                }
                
                val exerciseConfig = exerciseConfigBuilder.build()

                exerciseClient.setUpdateCallback(exerciseUpdateCallback)
                exerciseClient.startExerciseAsync(exerciseConfig).await()
                
                // Set up intervals if this is an interval workout (running only for now)
                intervals = pendingIntervals ?: emptyList()
                currentIntervalIndex = 0
                currentRepetition = 1
                intervalStartTime = System.currentTimeMillis()
                
                val isIntervalWorkout = intervals.isNotEmpty()
                val firstInterval = intervals.firstOrNull()
                
                // Determine if this is a structured workout with HR zones
                val hasHrTarget = targetHrMin != null && targetHrMax != null
                
                // Check if first interval is time-based or distance-based
                val firstDurationSeconds = firstInterval?.durationSeconds
                val firstDistanceMeters = firstInterval?.distanceMeters
                val firstIsTimeBased = firstDurationSeconds != null && firstDurationSeconds > 0
                val firstIntervalDuration = if (firstIsTimeBased) {
                    (firstDurationSeconds?.toLong() ?: 0) * 1000
                } else {
                    // For distance-based, use distance in meters as the "duration" for display
                    firstDistanceMeters?.toLong() ?: 0
                }
                
                _trackingState.value = WearTrackingState(
                    isTracking = true,
                    isPaused = false,
                    activityType = activityType,
                    targetPaceMin = firstInterval?.targetPaceMinSecondsPerKm 
                        ?: pendingWorkout?.targetPaceMinSecondsPerKm,
                    targetPaceMax = firstInterval?.targetPaceMaxSecondsPerKm 
                        ?: pendingWorkout?.targetPaceMaxSecondsPerKm,
                    targetHrMin = targetHrMin 
                        ?: firstInterval?.targetHeartRateMin 
                        ?: pendingWorkout?.targetHeartRateMin,
                    targetHrMax = targetHrMax 
                        ?: firstInterval?.targetHeartRateMax 
                        ?: pendingWorkout?.targetHeartRateMax,
                    targetHrZone = targetHrZone,
                    hasHrTarget = hasHrTarget || isIntervalWorkout,
                    workoutDurationSeconds = workoutDuration,
                    isIntervalWorkout = isIntervalWorkout,
                    currentIntervalIndex = 0,
                    currentIntervalType = firstInterval?.type?.name ?: "",
                    intervalTimeRemaining = firstIntervalDuration,
                    intervalTotalDuration = firstIntervalDuration,
                    totalIntervals = intervals.size,
                    currentRepetition = 1,
                    totalRepetitions = firstInterval?.repetitions ?: 1,
                    intervalStartDistance = 0.0,
                    isDistanceBasedInterval = !firstIsTimeBased
                )

                startForeground(NOTIFICATION_ID, createNotification())
                startTimer()

            } catch (e: Exception) {
                android.util.Log.e("WearTracking", "Error starting tracking: ${e.message}", e)
                e.printStackTrace()
                // Reset state on error
                _trackingState.value = WearTrackingState()
            }
        }
    }

    fun pauseTracking() {
        if (!_trackingState.value.isTracking || _trackingState.value.isPaused) return

        serviceScope.launch {
            try {
                exerciseClient.pauseExerciseAsync().await()
                timerJob?.cancel()
                pauseStartTime = System.currentTimeMillis()
                _trackingState.value = _trackingState.value.copy(isPaused = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resumeTracking() {
        if (!_trackingState.value.isTracking || !_trackingState.value.isPaused) return

        serviceScope.launch {
            try {
                exerciseClient.resumeExerciseAsync().await()
                totalPausedMillis += System.currentTimeMillis() - pauseStartTime
                startTimer()
                _trackingState.value = _trackingState.value.copy(isPaused = false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopTracking() {
        if (!_trackingState.value.isTracking) return

        // Cancel timer immediately
        timerJob?.cancel()

        // If stopped while paused, account for the current pause
        if (_trackingState.value.isPaused && pauseStartTime > 0) {
            totalPausedMillis += System.currentTimeMillis() - pauseStartTime
        }

        // Capture state before resetting
        val state = _trackingState.value
        val activityType = state.activityType
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime - totalPausedMillis
        val distance = state.distanceMeters
        val capturedRoutePoints = routePoints.toList()

        // Reset state immediately so UI updates
        _trackingState.value = WearTrackingState()
        routePoints.clear()

        serviceScope.launch {
            try {
                // Try to end exercise, but don't block on failure
                try {
                    exerciseClient.endExerciseAsync().await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                when (activityType) {
                    "SWIMMING" -> {
                        val pacePerHundred = if (distance > 0) (duration / 1000.0) / (distance / 100.0) else 0.0
                        val calories = RunCalculator.calculateCalories(distance, duration, null, state.heartRate)
                        val poolLen = swimPoolLengthMeters.coerceAtLeast(1)
                        val laps = (distance / poolLen).toInt()
                        val resolvedPoolLength = when (swimPoolLengthMeters) {
                            50 -> PoolLength.LONG_COURSE_METERS
                            23 -> PoolLength.SHORT_COURSE_YARDS
                            else -> PoolLength.SHORT_COURSE_METERS
                        }

                        val completedSwim = SwimmingWorkout(
                            startTime = startTime,
                            endTime = endTime,
                            swimType = SwimType.POOL,
                            poolLength = resolvedPoolLength,
                            distanceMeters = distance,
                            durationMillis = duration,
                            laps = laps,
                            avgPaceSecondsPer100m = pacePerHundred,
                            avgHeartRate = state.heartRate,
                            caloriesBurned = calories,
                            strokeType = StrokeType.FREESTYLE,
                            routePoints = capturedRoutePoints,
                            source = SwimSource.WATCH,
                            isCompleted = true
                        )
                        syncSwimToPhoneAndWait(completedSwim)
                    }
                    "CYCLING" -> {
                        val avgSpeedKmh = if (duration > 0) (distance / 1000.0) / (duration / 3600000.0) else 0.0
                        val elevationGain = calculateElevationGain(capturedRoutePoints)
                        val elevationLoss = calculateElevationLoss(capturedRoutePoints)
                        val calories = RunCalculator.calculateCalories(distance, duration, null, state.heartRate)
                        
                        val completedRide = CyclingWorkout(
                            startTime = startTime,
                            endTime = endTime,
                            cyclingType = CyclingType.OUTDOOR,
                            distanceMeters = distance,
                            durationMillis = duration,
                            avgSpeedKmh = avgSpeedKmh,
                            avgHeartRate = state.heartRate,
                            caloriesBurned = calories,
                            elevationGainMeters = elevationGain,
                            elevationLossMeters = elevationLoss,
                            routePoints = capturedRoutePoints,
                            source = CyclingSource.WATCH,
                            isCompleted = true
                        )
                        syncCyclingToPhoneAndWait(completedRide)
                    }
                    else -> {
                        // Running (default)
                        val pace = RunCalculator.calculatePace(distance, duration)
                        val splits = RunCalculator.calculateSplits(capturedRoutePoints)
                        val elevationGain = calculateElevationGain(capturedRoutePoints)
                        val elevationLoss = calculateElevationLoss(capturedRoutePoints)
                        val calories = RunCalculator.calculateCalories(distance, duration, null, state.heartRate)

                        val completedRun = Run(
                            startTime = startTime,
                            endTime = endTime,
                            distanceMeters = distance,
                            durationMillis = duration,
                            avgPaceSecondsPerKm = pace,
                            avgHeartRate = state.heartRate,
                            caloriesBurned = calories,
                            elevationGainMeters = elevationGain,
                            elevationLossMeters = elevationLoss,
                            routePoints = capturedRoutePoints,
                            splits = splits,
                            source = RunSource.WATCH,
                            isCompleted = true
                        )
                        android.util.Log.d("WearTracking", "About to sync run to phone...")
                        syncRunToPhoneAndWait(completedRun)
                        android.util.Log.d("WearTracking", "Run sync completed")
                    }
                }
                
                android.util.Log.d("WearTracking", "Workout sync finished, stopping service")

            } catch (e: Exception) {
                android.util.Log.e("WearTracking", "Error in stopTracking", e)
                e.printStackTrace()
            }
            
            // Small delay to ensure network operations complete
            delay(500)
            
            // Stop service AFTER sync completes
            android.util.Log.d("WearTracking", "Stopping foreground service")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            android.util.Log.d("WearTracking", "Service stopped")
        }
    }

    private val exerciseUpdateCallback = object : ExerciseUpdateCallback {
        // Battery optimization: Track last update time to throttle UI updates
        private var lastUiUpdateTime = 0L
        private var lastLocationTime = 0L
        private val UI_UPDATE_INTERVAL_MS = 1000L // Update UI every 1 second for smoother display
        private val LOCATION_SAMPLE_INTERVAL_MS = 5000L // Sample location every 5 seconds to save battery
        
        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            var heartRate: Int? = _trackingState.value.heartRate
            var distance = _trackingState.value.distanceMeters
            var speed: Double? = null
            val currentTime = System.currentTimeMillis()

            // Extract metrics from the ExerciseUpdate
            val latestMetrics = update.latestMetrics
            
            // Get heart rate
            val heartRateData = latestMetrics.getData(DataType.HEART_RATE_BPM)
            if (heartRateData.isNotEmpty()) {
                heartRate = heartRateData.last().value.toInt()
            }
            
            // Get distance (cumulative)
            val distanceData: CumulativeDataPoint<Double>? = latestMetrics.getData(DataType.DISTANCE_TOTAL)
            if (distanceData != null && distanceData.total > 0) {
                distance = distanceData.total
            }

            // For pool swimming, Health Services calculates DISTANCE_TOTAL using the
            // pool length set via setSwimmingPoolLengthMeters() and accelerometer-based
            // stroke/turn detection — no need for separate SWIM_LAP_COUNT handling.
            
            // Get speed for pace calculation
            val speedData = latestMetrics.getData(DataType.SPEED)
            if (speedData.isNotEmpty()) {
                speed = speedData.last().value
            }
            
            // Get location for route tracking - throttled to save battery
            if (currentTime - lastLocationTime >= LOCATION_SAMPLE_INTERVAL_MS) {
                val locationData = latestMetrics.getData(DataType.LOCATION)
                if (locationData.isNotEmpty()) {
                    val sample = locationData.last()
                    val latitude = sample.value.latitude
                    val longitude = sample.value.longitude
                    val altitude = sample.value.altitude
                    
                    if (latitude != 0.0 && longitude != 0.0) {
                        val routePoint = RoutePoint(
                            latitude = latitude,
                            longitude = longitude,
                            altitude = altitude,
                            timestamp = currentTime
                        )
                        routePoints.add(routePoint)
                        lastLocationTime = currentTime
                    }
                }
            }

            val pace = if (speed != null && speed!! > 0.1) {
                // Convert m/s to seconds per km
                1000.0 / speed!!
            } else {
                _trackingState.value.currentPaceSecondsPerKm
            }
            
            // Check zone alerts (always check for haptic feedback)
            val (paceAlert, hrAlert) = checkZoneAlerts(pace, heartRate, _trackingState.value)

            // Throttle UI state updates to save battery
            if (currentTime - lastUiUpdateTime >= UI_UPDATE_INTERVAL_MS) {
                _trackingState.value = _trackingState.value.copy(
                    distanceMeters = distance,
                    heartRate = heartRate,
                    currentPaceSecondsPerKm = pace,
                    paceAlert = paceAlert,
                    hrAlert = hrAlert
                )
                lastUiUpdateTime = currentTime
            }
            // Note: Notification updates moved to timer (every 5 seconds)
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}

        override fun onRegistered() {}

        override fun onRegistrationFailed(throwable: Throwable) {
            throwable.printStackTrace()
        }

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {}
    }

    private fun startTimer() {
        timerJob = serviceScope.launch {
            var updateCounter = 0
            while (isActive) {
                delay(1000)
                if (!_trackingState.value.isPaused) {
                    val duration = System.currentTimeMillis() - startTime - totalPausedMillis
                    updateCounter++
                    
                    // Update interval tracking if this is an interval workout
                    if (_trackingState.value.isIntervalWorkout && intervals.isNotEmpty()) {
                        updateIntervalProgress()
                    }
                    
                    _trackingState.value = _trackingState.value.copy(durationMillis = duration)
                    
                    // Only update notification every 5 seconds to save battery
                    if (updateCounter % 5 == 0) {
                        updateNotification()
                    }
                }
            }
        }
    }
    
    private fun updateIntervalProgress() {
        if (currentIntervalIndex >= intervals.size) return
        
        val currentInterval = intervals[currentIntervalIndex]
        val currentDistance = _trackingState.value.distanceMeters
        
        // Capture nullable properties to local variables for smart cast
        val intervalDurationSeconds = currentInterval.durationSeconds
        val intervalDistanceMeters = currentInterval.distanceMeters
        
        // Check if interval is time-based or distance-based
        val isTimeBased = intervalDurationSeconds != null && intervalDurationSeconds > 0
        val isDistanceBased = intervalDistanceMeters != null && intervalDistanceMeters > 0
        
        val intervalComplete: Boolean
        val remaining: Long
        
        if (isTimeBased && intervalDurationSeconds != null) {
            // Time-based interval
            val intervalDuration = intervalDurationSeconds.toLong() * 1000
            val elapsed = System.currentTimeMillis() - intervalStartTime
            remaining = (intervalDuration - elapsed).coerceAtLeast(0)
            intervalComplete = remaining <= 0
        } else if (isDistanceBased && intervalDistanceMeters != null) {
            // Distance-based interval - track distance covered since interval started
            val targetDistance = intervalDistanceMeters
            val distanceAtIntervalStart = _trackingState.value.intervalStartDistance
            val distanceCovered = currentDistance - distanceAtIntervalStart
            val distanceRemaining = (targetDistance - distanceCovered).coerceAtLeast(0.0)
            remaining = distanceRemaining.toLong() // Use as meters remaining for display
            intervalComplete = distanceRemaining <= 0
        } else {
            // No duration or distance specified - skip this interval
            android.util.Log.w("WearTracking", "Interval has no duration or distance, skipping")
            remaining = 0
            intervalComplete = true
        }
        
        if (intervalComplete) {
            // Interval complete - check if we need to repeat or move to next
            if (currentRepetition < (currentInterval.repetitions)) {
                // Repeat this interval
                currentRepetition++
                intervalStartTime = System.currentTimeMillis()
                _trackingState.value = _trackingState.value.copy(
                    intervalStartDistance = currentDistance
                )
                vibrateIntervalChange()
            } else {
                // Move to next interval
                currentIntervalIndex++
                currentRepetition = 1
                intervalStartTime = System.currentTimeMillis()
                
                if (currentIntervalIndex < intervals.size) {
                    val nextInterval = intervals[currentIntervalIndex]
                    val nextDurationSeconds = nextInterval.durationSeconds
                    val nextDistanceMeters = nextInterval.distanceMeters
                    val nextIsTimeBased = nextDurationSeconds != null && nextDurationSeconds > 0
                    val nextIntervalDuration = if (nextIsTimeBased && nextDurationSeconds != null) {
                        nextDurationSeconds.toLong() * 1000
                    } else {
                        // For distance-based, show distance in meters as "duration" for display
                        nextDistanceMeters?.toLong() ?: 0
                    }
                    
                    // Update targets for new interval
                    _trackingState.value = _trackingState.value.copy(
                        targetPaceMin = nextInterval.targetPaceMinSecondsPerKm ?: _trackingState.value.targetPaceMin,
                        targetPaceMax = nextInterval.targetPaceMaxSecondsPerKm ?: _trackingState.value.targetPaceMax,
                        targetHrMin = nextInterval.targetHeartRateMin ?: _trackingState.value.targetHrMin,
                        targetHrMax = nextInterval.targetHeartRateMax ?: _trackingState.value.targetHrMax,
                        currentIntervalIndex = currentIntervalIndex,
                        currentIntervalType = nextInterval.type.name,
                        intervalTimeRemaining = nextIntervalDuration,
                        intervalTotalDuration = nextIntervalDuration,
                        currentRepetition = 1,
                        totalRepetitions = nextInterval.repetitions,
                        intervalStartDistance = currentDistance,
                        isDistanceBasedInterval = !nextIsTimeBased
                    )
                    
                    vibrateIntervalChange()
                }
            }
        } else {
            // Update remaining time/distance
            _trackingState.value = _trackingState.value.copy(
                intervalTimeRemaining = remaining,
                currentRepetition = currentRepetition
            )
        }
    }
    
    private fun vibrateIntervalChange() {
        vibrator?.let { v ->
            if (v.hasVibrator()) {
                // Strong triple vibration for interval change
                val pattern = longArrayOf(0, 200, 100, 200, 100, 200)
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
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

        return NotificationCompat.Builder(this, WearGoSteadyApplication.TRACKING_CHANNEL_ID)
            .setContentTitle("Running")
            .setContentText("%.2f km".format(distanceKm))
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

    private suspend fun syncRunToPhoneAndWait(run: Run) {
        // Serialize JSON on Default dispatcher to avoid blocking IO/Main
        val (runJson, jsonSizeKb) = withContext(Dispatchers.Default) {
            val json = gson.toJson(run)
            json to (json.toByteArray().size / 1024)
        }

        android.util.Log.d("WearTracking", "Run JSON size: ${jsonSizeKb}KB, routePoints: ${run.routePoints.size}")

        if (jsonSizeKb < 80) {
            // Small run - use existing simple approach
            syncRunSimple(run, runJson)
        } else {
            // Large run - use Asset-based transfer
            android.util.Log.d("WearTracking", "Using Asset-based transfer for ${run.routePoints.size} route points")
            syncRunWithAsset(run)
        }
    }

    private suspend fun syncRunSimple(run: Run, runJson: String) {
        var retryCount = 0
        val maxRetries = 3

        while (retryCount < maxRetries) {
            try {
                android.util.Log.d("WearTracking", "Syncing run to phone (attempt ${retryCount + 1}): distance=${run.distanceMeters}m, duration=${run.durationMillis}ms")
                android.util.Log.d("WearTracking", "Run JSON length: ${runJson.length}")

                // Check if phone is connected
                val nodeClient = Wearable.getNodeClient(this@WearRunTrackingService)
                val connectedNodes = nodeClient.connectedNodes.await()
                android.util.Log.d("WearTracking", "Connected nodes: ${connectedNodes.size}")

                if (connectedNodes.isEmpty()) {
                    android.util.Log.w("WearTracking", "No connected nodes, phone may not be reachable")
                    // Still try DataClient as it can sync when phone reconnects
                }

                // Try MessageClient first for immediate delivery (more reliable)
                val messageClient = Wearable.getMessageClient(this@WearRunTrackingService)
                for (node in connectedNodes) {
                    try {
                        android.util.Log.d("WearTracking", "Sending run via MessageClient to node: ${node.displayName}")
                        messageClient.sendMessage(
                            node.id,
                            DataLayerPaths.RUN_DATA_PATH,
                            runJson.toByteArray()
                        ).await()
                        android.util.Log.d("WearTracking", "Run sent via MessageClient successfully")
                    } catch (e: Exception) {
                        android.util.Log.e("WearTracking", "MessageClient failed for node ${node.displayName}", e)
                    }
                }

                // Also use DataClient as backup (persists data)
                val dataClient = Wearable.getDataClient(this@WearRunTrackingService)

                val putDataRequest = com.google.android.gms.wearable.PutDataMapRequest
                    .create(DataLayerPaths.RUN_DATA_PATH)
                    .apply {
                        dataMap.putString(DataLayerPaths.KEY_RUN_JSON, runJson)
                        dataMap.putLong(DataLayerPaths.KEY_TIMESTAMP, System.currentTimeMillis())
                    }
                    .asPutDataRequest()
                    .setUrgent()

                val result = dataClient.putDataItem(putDataRequest).await()
                android.util.Log.d("WearTracking", "Run synced via DataClient: ${result.uri}")
                return // Success, exit the retry loop
            } catch (e: Exception) {
                retryCount++
                android.util.Log.e("WearTracking", "Error syncing run to phone (attempt $retryCount)", e)
                if (retryCount < maxRetries) {
                    android.util.Log.d("WearTracking", "Retrying in 1 second...")
                    delay(1000)
                } else {
                    android.util.Log.e("WearTracking", "Failed to sync run after $maxRetries attempts")
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun syncRunWithAsset(run: Run) {
        var retryCount = 0
        val maxRetries = 3

        while (retryCount < maxRetries) {
            try {
                // 1. Create run metadata (without route points)
                val runMeta = run.copy(routePoints = emptyList())
                val metaJson = gson.toJson(runMeta)
                android.util.Log.d("WearTracking", "Run metadata JSON length: ${metaJson.length}")

                // 2. Compress route points to byte array
                val routeJson = gson.toJson(run.routePoints)
                val routeBytes = routeJson.toByteArray()
                val compressedRoutes = CompressionUtils.gzipCompress(routeBytes)
                val compressionRatio = (1.0 - compressedRoutes.size.toDouble() / routeBytes.size) * 100
                android.util.Log.d("WearTracking", "Route points: ${routeBytes.size} bytes -> ${compressedRoutes.size} bytes (${compressionRatio.toInt()}% reduction)")

                // 3. Create Asset from compressed route data
                val routeAsset = Asset.createFromBytes(compressedRoutes)

                // 4. Send via DataClient with Asset
                val dataClient = Wearable.getDataClient(this@WearRunTrackingService)

                val putDataRequest = com.google.android.gms.wearable.PutDataMapRequest
                    .create(DataLayerPaths.RUN_DATA_PATH)
                    .apply {
                        dataMap.putString(DataLayerPaths.KEY_RUN_META_JSON, metaJson)
                        dataMap.putInt(DataLayerPaths.KEY_ROUTE_POINT_COUNT, run.routePoints.size)
                        dataMap.putAsset(DataLayerPaths.RUN_ROUTE_ASSET_KEY, routeAsset)
                        dataMap.putLong(DataLayerPaths.KEY_TIMESTAMP, System.currentTimeMillis())
                    }
                    .asPutDataRequest()
                    .setUrgent()

                val result = dataClient.putDataItem(putDataRequest).await()
                android.util.Log.d("WearTracking", "Run synced via Asset-based DataClient: ${result.uri}")
                return // Success, exit the retry loop
            } catch (e: Exception) {
                retryCount++
                android.util.Log.e("WearTracking", "Error syncing run with Asset (attempt $retryCount)", e)
                if (retryCount < maxRetries) {
                    android.util.Log.d("WearTracking", "Retrying in 1 second...")
                    delay(1000)
                } else {
                    android.util.Log.e("WearTracking", "Failed to sync run with Asset after $maxRetries attempts")
                    // Fallback: try simple sync without route points as last resort
                    try {
                        android.util.Log.w("WearTracking", "Fallback: syncing run without route points")
                        val runWithoutRoutes = run.copy(routePoints = emptyList())
                        val fallbackJson = gson.toJson(runWithoutRoutes)
                        syncRunSimple(runWithoutRoutes, fallbackJson)
                    } catch (fallbackError: Exception) {
                        android.util.Log.e("WearTracking", "Fallback sync also failed", fallbackError)
                    }
                }
            }
        }
    }
    
    private suspend fun syncSwimToPhoneAndWait(swim: SwimmingWorkout) {
        try {
            android.util.Log.d("WearTracking", "Syncing swim to phone: distance=${swim.distanceMeters}m, duration=${swim.durationMillis}ms")
            val swimJson = withContext(Dispatchers.Default) { gson.toJson(swim) }
            val dataClient = Wearable.getDataClient(this@WearRunTrackingService)
            
            val putDataRequest = com.google.android.gms.wearable.PutDataMapRequest
                .create(DataLayerPaths.SWIM_DATA_PATH)
                .apply {
                    dataMap.putString(DataLayerPaths.KEY_SWIM_JSON, swimJson)
                    dataMap.putLong(DataLayerPaths.KEY_TIMESTAMP, System.currentTimeMillis())
                }
                .asPutDataRequest()
                .setUrgent()

            val result = dataClient.putDataItem(putDataRequest).await()
            android.util.Log.d("WearTracking", "Swim synced to phone successfully: ${result.uri}")
        } catch (e: Exception) {
            android.util.Log.e("WearTracking", "Error syncing swim to phone", e)
            e.printStackTrace()
        }
    }
    
    private suspend fun syncCyclingToPhoneAndWait(ride: CyclingWorkout) {
        try {
            android.util.Log.d("WearTracking", "Syncing ride to phone: distance=${ride.distanceMeters}m, duration=${ride.durationMillis}ms")
            val rideJson = withContext(Dispatchers.Default) { gson.toJson(ride) }
            val dataClient = Wearable.getDataClient(this@WearRunTrackingService)
            
            val putDataRequest = com.google.android.gms.wearable.PutDataMapRequest
                .create(DataLayerPaths.CYCLING_DATA_PATH)
                .apply {
                    dataMap.putString(DataLayerPaths.KEY_CYCLING_JSON, rideJson)
                    dataMap.putLong(DataLayerPaths.KEY_TIMESTAMP, System.currentTimeMillis())
                }
                .asPutDataRequest()
                .setUrgent()

            val result = dataClient.putDataItem(putDataRequest).await()
            android.util.Log.d("WearTracking", "Ride synced to phone successfully: ${result.uri}")
        } catch (e: Exception) {
            android.util.Log.e("WearTracking", "Error syncing ride to phone", e)
            e.printStackTrace()
        }
    }

    private fun sendHeartRateToPhone(heartRate: Int) {
        serviceScope.launch {
            try {
                val dataClient = Wearable.getDataClient(this@WearRunTrackingService)
                
                val putDataRequest = com.google.android.gms.wearable.PutDataMapRequest
                    .create(DataLayerPaths.HEART_RATE_PATH)
                    .apply {
                        dataMap.putInt(DataLayerPaths.KEY_HEART_RATE, heartRate)
                        dataMap.putLong(DataLayerPaths.KEY_TIMESTAMP, System.currentTimeMillis())
                    }
                    .asPutDataRequest()
                    .setUrgent()

                dataClient.putDataItem(putDataRequest)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't cancel serviceScope here - let ongoing sync operations complete
        // The scope will be garbage collected when the service is destroyed
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_ID = 100
    }
}

data class WearTrackingState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val activityType: String = "RUNNING", // RUNNING, SWIMMING, CYCLING
    val distanceMeters: Double = 0.0,
    val durationMillis: Long = 0,
    val currentPaceSecondsPerKm: Double = 0.0,
    val heartRate: Int? = null,
    val targetPaceMin: Double? = null,
    val targetPaceMax: Double? = null,
    val targetHrMin: Int? = null,
    val targetHrMax: Int? = null,
    val targetHrZone: Int? = null, // 1-5 for HR zones
    val hasHrTarget: Boolean = false, // True if this workout has HR zone guidance
    val workoutDurationSeconds: Int? = null, // Target workout duration
    val paceAlert: PaceAlert = PaceAlert.IN_ZONE,
    val hrAlert: HrAlert = HrAlert.IN_ZONE,
    // Interval tracking
    val isIntervalWorkout: Boolean = false,
    val currentIntervalIndex: Int = 0,
    val currentIntervalType: String = "",
    val intervalTimeRemaining: Long = 0,
    val intervalTotalDuration: Long = 0,
    val totalIntervals: Int = 0,
    val currentRepetition: Int = 0,
    val totalRepetitions: Int = 0,
    val intervalStartDistance: Double = 0.0, // Distance at start of current interval (for distance-based intervals)
    val isDistanceBasedInterval: Boolean = false, // True if current interval is distance-based
    // Compete mode - personal best comparison
    val competeTargetDistance: Int = 5000, // Default 5K
    val competePbTimeMillis: Long? = null,
    val competePbPaceSecondsPerKm: Double? = null,
    val competeIsAhead: Boolean = false,
    val competeTimeDiffMillis: Long = 0, // Positive = behind, negative = ahead
    val competePbProgress: Float = 0f, // Where PB runner would be (0.0 to 1.0)
    val competeCurrentProgress: Float = 0f // Where current runner is (0.0 to 1.0)
) {
    // Progress through current phase (0.0 to 1.0)
    val currentPhaseProgress: Float get() {
        if (intervalTotalDuration <= 0) return 0f
        val elapsed = intervalTotalDuration - intervalTimeRemaining
        return (elapsed.toFloat() / intervalTotalDuration).coerceIn(0f, 1f)
    }
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

    val paceFormatted: String get() {
        if (currentPaceSecondsPerKm <= 0 || currentPaceSecondsPerKm.isInfinite() || currentPaceSecondsPerKm.isNaN()) {
            return "--:--"
        }
        val minutes = (currentPaceSecondsPerKm / 60).toInt()
        val seconds = (currentPaceSecondsPerKm % 60).toInt()
        return String.format("%d:%02d", minutes, seconds)
    }
    
    val targetPaceFormatted: String? get() {
        if (targetPaceMin == null || targetPaceMax == null) return null
        val minMins = (targetPaceMin / 60).toInt()
        val minSecs = (targetPaceMin % 60).toInt()
        val maxMins = (targetPaceMax / 60).toInt()
        val maxSecs = (targetPaceMax % 60).toInt()
        return "$minMins:${String.format("%02d", minSecs)} - $maxMins:${String.format("%02d", maxSecs)}"
    }
    
    val targetHrFormatted: String? get() {
        if (targetHrMin == null || targetHrMax == null) return null
        return "$targetHrMin - $targetHrMax bpm"
    }
}

enum class PaceAlert {
    TOO_SLOW,
    IN_ZONE,
    TOO_FAST
}

enum class HrAlert {
    TOO_LOW,
    IN_ZONE,
    TOO_HIGH
}
