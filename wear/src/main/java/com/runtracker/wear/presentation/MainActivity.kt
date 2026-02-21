package com.runtracker.wear.presentation

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.wear.ambient.AmbientModeSupport
import com.runtracker.shared.data.model.ScheduledWorkout
import com.runtracker.shared.data.model.WorkoutType
import com.runtracker.wear.presentation.screens.SwimWorkoutTypeWatch
import com.runtracker.wear.presentation.screens.CycleWorkoutTypeWatch
import com.runtracker.wear.presentation.screens.HardwareButtonSosDialog
import com.runtracker.wear.presentation.theme.WearRunTrackerTheme
import com.runtracker.wear.service.WearRunTrackingService
import com.runtracker.wear.service.WearTrackingState
import com.runtracker.wear.service.WorkoutHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {
    
    private var ambientController: AmbientModeSupport.AmbientController? = null
    private val isAmbient = MutableStateFlow(false)
    private val pendingWorkout = MutableStateFlow<ScheduledWorkout?>(null)

    private var trackingService: WearRunTrackingService? = null
    private var isBound = false
    
    private val trackingState = MutableStateFlow(WearTrackingState())
    
    // Hardware button SOS detection
    private var stemButtonPressed = false
    private var backButtonPressed = false
    private var bothButtonsStartTime: Long = 0
    private val sosHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var sosTriggered = false
    private val showSosDialog = MutableStateFlow(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WearRunTrackingService.LocalBinder
            trackingService = binder.getService()
            isBound = true
            
            lifecycleScope.launch {
                trackingService?.trackingState?.collect { state ->
                    trackingState.value = state
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable ambient mode support for always-on display during tracking
        try {
            ambientController = AmbientModeSupport.attach(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        requestPermissions()
        bindService()

        setContent {
            WearRunTrackerTheme {
                val state by trackingState.collectAsState()
                val ambient by isAmbient.collectAsState()
                
                val pending by pendingWorkout.collectAsState()
                
                // Check for pending workout from WorkoutHolder
                LaunchedEffect(Unit) {
                    WorkoutHolder.pendingWorkout?.let { workout ->
                        pendingWorkout.value = workout
                    }
                }
                
                // Manage screen and brightness based on tracking state
                LaunchedEffect(state.isTracking) {
                    if (state.isTracking) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        setLowBrightness()
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        restoreNormalBrightness()
                    }
                }
                
                val showSos by showSosDialog.collectAsState()
                
                WearRunTrackerApp(
                    trackingState = state,
                    isAmbient = ambient,
                    pendingWorkout = pending,
                    onStartRun = { startRun() },
                    onStartWorkoutRun = { workoutType -> startWorkoutRun(workoutType) },
                    onStartSwim = { swimType, poolLength -> startSwim(swimType, poolLength) },
                    onStartSwimWorkout = { workoutType, swimType, poolLength -> startSwimWorkout(workoutType, swimType, poolLength) },
                    onStartCycling = { cyclingType -> startCycling(cyclingType) },
                    onStartCyclingWorkout = { workoutType, cyclingType -> startCyclingWorkout(workoutType, cyclingType) },
                    onPauseRun = { pauseRun() },
                    onResumeRun = { resumeRun() },
                    onStopRun = { stopRun() },
                    onClearPendingWorkout = { 
                        pendingWorkout.value = null
                        WorkoutHolder.pendingWorkout = null
                    }
                )
                
                // Hardware button SOS confirmation dialog
                if (showSos) {
                    HardwareButtonSosDialog(
                        onConfirm = {
                            triggerSos()
                            showSosDialog.value = false
                            sosTriggered = false
                        },
                        onDismiss = {
                            showSosDialog.value = false
                            sosTriggered = false
                        }
                    )
                }
            }
        }
    }
    
    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return object : AmbientModeSupport.AmbientCallback() {
            override fun onEnterAmbient(ambientDetails: Bundle?) {
                super.onEnterAmbient(ambientDetails)
                isAmbient.value = true
            }
            
            override fun onExitAmbient() {
                super.onExitAmbient()
                isAmbient.value = false
            }
            
            override fun onUpdateAmbient() {
                super.onUpdateAmbient()
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
        
        // Add background permissions for API 29+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        
        // Add body sensors background for API 33+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add("android.permission.BODY_SENSORS_BACKGROUND")
        }
        
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
    }
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private const val SOS_HOLD_DURATION_MS = 3000L // 3 seconds

        // Pre-computed HR zone ranges (based on max HR of 190)
        // Avoids recalculating on every workout start
        private val HR_ZONE_CACHE = mapOf(
            1 to Pair(95, 114),    // Zone 1: 50-60%
            2 to Pair(114, 133),   // Zone 2: 60-70%
            3 to Pair(133, 152),   // Zone 3: 70-80%
            4 to Pair(152, 171),   // Zone 4: 80-90%
            5 to Pair(171, 190)    // Zone 5: 90-100%
        )
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Only detect during active tracking
        if (!trackingState.value.isTracking) {
            return super.onKeyDown(keyCode, event)
        }
        
        when (keyCode) {
            KeyEvent.KEYCODE_STEM_PRIMARY, KeyEvent.KEYCODE_STEM_1 -> {
                stemButtonPressed = true
                checkBothButtonsPressed()
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                backButtonPressed = true
                checkBothButtonsPressed()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_STEM_PRIMARY, KeyEvent.KEYCODE_STEM_1 -> {
                stemButtonPressed = false
                cancelSosCheck()
            }
            KeyEvent.KEYCODE_BACK -> {
                backButtonPressed = false
                cancelSosCheck()
            }
        }
        return super.onKeyUp(keyCode, event)
    }
    
    private fun checkBothButtonsPressed() {
        if (stemButtonPressed && backButtonPressed && !sosTriggered) {
            bothButtonsStartTime = System.currentTimeMillis()
            sosHandler.postDelayed({
                if (stemButtonPressed && backButtonPressed && !sosTriggered) {
                    // Both buttons held for 3 seconds - trigger SOS
                    sosTriggered = true
                    showSosDialog.value = true
                    // Vibrate to confirm
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                    vibrator?.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }, SOS_HOLD_DURATION_MS)
        }
    }
    
    private fun cancelSosCheck() {
        sosHandler.removeCallbacksAndMessages(null)
        if (!sosTriggered) {
            bothButtonsStartTime = 0
        }
    }
    
    private fun triggerSos() {
        lifecycleScope.launch {
            com.runtracker.wear.safety.WatchSafetyService.triggerSos(this@MainActivity)
        }
    }

    private fun bindService() {
        Intent(this, WearRunTrackingService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun startRun() {
        Intent(this, WearRunTrackingService::class.java).apply {
            action = WearRunTrackingService.ACTION_START
        }.also {
            startForegroundService(it)
        }
    }
    
    private fun startWorkoutRun(workoutType: WorkoutType) {
        // Generate intervals for the selected workout type and store in WorkoutHolder
        val intervals = generateIntervalsForWorkoutType(workoutType)
        WorkoutHolder.pendingIntervals = intervals
        WorkoutHolder.pendingWorkout = null
        
        Intent(this, WearRunTrackingService::class.java).apply {
            action = WearRunTrackingService.ACTION_START
        }.also {
            startForegroundService(it)
        }
    }
    
    private fun startSwim(swimType: String, poolLength: Int) {
        // Store swim type for the tracking service
        WorkoutHolder.pendingActivityType = "SWIMMING"
        WorkoutHolder.pendingSwimType = swimType
        WorkoutHolder.pendingPoolLength = poolLength
        
        Intent(this, WearRunTrackingService::class.java).apply {
            action = WearRunTrackingService.ACTION_START
            putExtra("activity_type", "SWIMMING")
            putExtra("swim_type", swimType)
            putExtra("pool_length", poolLength)
        }.also {
            startForegroundService(it)
        }
    }
    
    private fun startCycling(cyclingType: String) {
        // Store cycling type for the tracking service
        WorkoutHolder.pendingActivityType = "CYCLING"
        WorkoutHolder.pendingCyclingType = cyclingType
        
        Intent(this, WearRunTrackingService::class.java).apply {
            action = WearRunTrackingService.ACTION_START
            putExtra("activity_type", "CYCLING")
            putExtra("cycling_type", cyclingType)
        }.also {
            startForegroundService(it)
        }
    }
    
    private fun startSwimWorkout(workoutType: SwimWorkoutTypeWatch, swimType: String, poolLength: Int) {
        // Generate HR zone targets based on workout type
        val hrZone = workoutType.targetHrZone
        val (hrMin, hrMax) = getHrRangeForZone(hrZone)
        
        WorkoutHolder.pendingActivityType = "SWIMMING"
        WorkoutHolder.pendingSwimType = swimType
        WorkoutHolder.pendingPoolLength = poolLength
        WorkoutHolder.pendingSwimWorkoutType = workoutType.name
        WorkoutHolder.pendingTargetHrMin = hrMin
        WorkoutHolder.pendingTargetHrMax = hrMax
        WorkoutHolder.pendingTargetHrZone = hrZone
        WorkoutHolder.pendingWorkoutDuration = workoutType.durationMinutes * 60
        
        Intent(this, WearRunTrackingService::class.java).apply {
            action = WearRunTrackingService.ACTION_START
            putExtra("activity_type", "SWIMMING")
            putExtra("swim_type", swimType)
            putExtra("pool_length", poolLength)
            putExtra("workout_type", workoutType.name)
            putExtra("target_hr_min", hrMin)
            putExtra("target_hr_max", hrMax)
            putExtra("target_hr_zone", hrZone)
        }.also {
            startForegroundService(it)
        }
    }
    
    private fun startCyclingWorkout(workoutType: CycleWorkoutTypeWatch, cyclingType: String) {
        // Generate HR zone targets based on workout type
        val hrZone = workoutType.targetHrZone
        val (hrMin, hrMax) = getHrRangeForZone(hrZone)
        
        WorkoutHolder.pendingActivityType = "CYCLING"
        WorkoutHolder.pendingCyclingType = cyclingType
        WorkoutHolder.pendingCyclingWorkoutType = workoutType.name
        WorkoutHolder.pendingTargetHrMin = hrMin
        WorkoutHolder.pendingTargetHrMax = hrMax
        WorkoutHolder.pendingTargetHrZone = hrZone
        WorkoutHolder.pendingWorkoutDuration = workoutType.durationMinutes * 60
        
        Intent(this, WearRunTrackingService::class.java).apply {
            action = WearRunTrackingService.ACTION_START
            putExtra("activity_type", "CYCLING")
            putExtra("cycling_type", cyclingType)
            putExtra("workout_type", workoutType.name)
            putExtra("target_hr_min", hrMin)
            putExtra("target_hr_max", hrMax)
            putExtra("target_hr_zone", hrZone)
        }.also {
            startForegroundService(it)
        }
    }
    
    private fun getHrRangeForZone(zone: Int): Pair<Int, Int> {
        return HR_ZONE_CACHE[zone] ?: Pair(100, 150)
    }
    
    private fun generateIntervalsForWorkoutType(workoutType: WorkoutType): List<com.runtracker.shared.data.model.Interval> {
        val intervals = mutableListOf<com.runtracker.shared.data.model.Interval>()
        
        // Default paces (will be overridden by user's actual pace when running)
        val warmupPaceMin = 420.0 // 7:00/km
        val warmupPaceMax = 480.0 // 8:00/km
        val easyPaceMin = 360.0 // 6:00/km
        val easyPaceMax = 420.0 // 7:00/km
        val tempoPaceMin = 300.0 // 5:00/km
        val tempoPaceMax = 330.0 // 5:30/km
        val intervalPaceMin = 240.0 // 4:00/km
        val intervalPaceMax = 270.0 // 4:30/km
        
        val warmup = com.runtracker.shared.data.model.Interval(
            type = com.runtracker.shared.data.model.IntervalType.WARMUP,
            durationSeconds = 600,
            targetPaceMinSecondsPerKm = warmupPaceMin,
            targetPaceMaxSecondsPerKm = warmupPaceMax,
            targetHeartRateZone = com.runtracker.shared.data.model.HeartRateZone.ZONE_1,
            targetHeartRateMin = 100,
            targetHeartRateMax = 130
        )
        
        val cooldown = com.runtracker.shared.data.model.Interval(
            type = com.runtracker.shared.data.model.IntervalType.COOLDOWN,
            durationSeconds = 600,
            targetPaceMinSecondsPerKm = warmupPaceMin,
            targetPaceMaxSecondsPerKm = warmupPaceMax,
            targetHeartRateZone = com.runtracker.shared.data.model.HeartRateZone.ZONE_1,
            targetHeartRateMin = 100,
            targetHeartRateMax = 130
        )
        
        when (workoutType) {
            WorkoutType.EASY_RUN -> {
                intervals.add(warmup.copy(durationSeconds = 300))
                intervals.add(com.runtracker.shared.data.model.Interval(
                    type = com.runtracker.shared.data.model.IntervalType.WORK,
                    durationSeconds = 1500, // 25 min
                    targetPaceMinSecondsPerKm = easyPaceMin,
                    targetPaceMaxSecondsPerKm = easyPaceMax,
                    targetHeartRateZone = com.runtracker.shared.data.model.HeartRateZone.ZONE_2,
                    targetHeartRateMin = 130,
                    targetHeartRateMax = 150
                ))
                intervals.add(cooldown.copy(durationSeconds = 300))
            }
            
            WorkoutType.LONG_RUN -> {
                intervals.add(warmup)
                intervals.add(com.runtracker.shared.data.model.Interval(
                    type = com.runtracker.shared.data.model.IntervalType.WORK,
                    durationSeconds = 3600, // 60 min
                    targetPaceMinSecondsPerKm = easyPaceMin,
                    targetPaceMaxSecondsPerKm = easyPaceMax,
                    targetHeartRateZone = com.runtracker.shared.data.model.HeartRateZone.ZONE_2,
                    targetHeartRateMin = 130,
                    targetHeartRateMax = 155
                ))
                intervals.add(cooldown)
            }
            
            WorkoutType.TEMPO_RUN -> {
                intervals.add(warmup)
                intervals.add(com.runtracker.shared.data.model.Interval(
                    type = com.runtracker.shared.data.model.IntervalType.WORK,
                    durationSeconds = 1200, // 20 min tempo
                    targetPaceMinSecondsPerKm = tempoPaceMin,
                    targetPaceMaxSecondsPerKm = tempoPaceMax,
                    targetHeartRateZone = com.runtracker.shared.data.model.HeartRateZone.ZONE_4,
                    targetHeartRateMin = 160,
                    targetHeartRateMax = 175
                ))
                intervals.add(cooldown)
            }
            
            WorkoutType.INTERVAL_TRAINING -> {
                intervals.add(warmup)
                repeat(6) { index ->
                    intervals.add(com.runtracker.shared.data.model.Interval(
                        type = com.runtracker.shared.data.model.IntervalType.WORK,
                        distanceMeters = 800.0,
                        targetPaceMinSecondsPerKm = intervalPaceMin,
                        targetPaceMaxSecondsPerKm = intervalPaceMax,
                        targetHeartRateZone = com.runtracker.shared.data.model.HeartRateZone.ZONE_5,
                        targetHeartRateMin = 170,
                        targetHeartRateMax = 185
                    ))
                    if (index < 5) {
                        intervals.add(com.runtracker.shared.data.model.Interval(
                            type = com.runtracker.shared.data.model.IntervalType.RECOVERY,
                            distanceMeters = 400.0,
                            targetPaceMinSecondsPerKm = warmupPaceMin,
                            targetPaceMaxSecondsPerKm = warmupPaceMax,
                            targetHeartRateZone = com.runtracker.shared.data.model.HeartRateZone.ZONE_2,
                            targetHeartRateMin = 120,
                            targetHeartRateMax = 140
                        ))
                    }
                }
                intervals.add(cooldown)
            }
            
            WorkoutType.HILL_REPEATS -> {
                intervals.add(warmup)
                repeat(8) { index ->
                    intervals.add(com.runtracker.shared.data.model.Interval(
                        type = com.runtracker.shared.data.model.IntervalType.WORK,
                        durationSeconds = 60,
                        targetPaceMinSecondsPerKm = intervalPaceMin,
                        targetPaceMaxSecondsPerKm = intervalPaceMax,
                        targetHeartRateZone = com.runtracker.shared.data.model.HeartRateZone.ZONE_5,
                        targetHeartRateMin = 170,
                        targetHeartRateMax = 185
                    ))
                    if (index < 7) {
                        intervals.add(com.runtracker.shared.data.model.Interval(
                            type = com.runtracker.shared.data.model.IntervalType.RECOVERY,
                            durationSeconds = 90,
                            targetPaceMinSecondsPerKm = warmupPaceMin,
                            targetPaceMaxSecondsPerKm = warmupPaceMax,
                            targetHeartRateZone = com.runtracker.shared.data.model.HeartRateZone.ZONE_2,
                            targetHeartRateMin = 120,
                            targetHeartRateMax = 140
                        ))
                    }
                }
                intervals.add(cooldown)
            }
            
            WorkoutType.FARTLEK -> {
                intervals.add(warmup)
                intervals.add(com.runtracker.shared.data.model.Interval(
                    type = com.runtracker.shared.data.model.IntervalType.WORK,
                    durationSeconds = 1200, // 20 min varied pace
                    targetPaceMinSecondsPerKm = tempoPaceMin,
                    targetPaceMaxSecondsPerKm = easyPaceMax,
                    targetHeartRateZone = com.runtracker.shared.data.model.HeartRateZone.ZONE_3,
                    targetHeartRateMin = 145,
                    targetHeartRateMax = 165
                ))
                intervals.add(cooldown)
            }
            
            else -> {
                intervals.add(warmup.copy(durationSeconds = 300))
                intervals.add(com.runtracker.shared.data.model.Interval(
                    type = com.runtracker.shared.data.model.IntervalType.WORK,
                    durationSeconds = 1800,
                    targetPaceMinSecondsPerKm = easyPaceMin,
                    targetPaceMaxSecondsPerKm = easyPaceMax,
                    targetHeartRateZone = com.runtracker.shared.data.model.HeartRateZone.ZONE_2,
                    targetHeartRateMin = 130,
                    targetHeartRateMax = 150
                ))
                intervals.add(cooldown.copy(durationSeconds = 300))
            }
        }
        
        return intervals
    }

    private fun pauseRun() {
        trackingService?.pauseTracking()
    }

    private fun resumeRun() {
        trackingService?.resumeTracking()
    }

    private fun stopRun() {
        trackingService?.stopTracking()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel any pending SOS handler callbacks to prevent leaks
        sosHandler.removeCallbacksAndMessages(null)
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        restoreNormalBrightness()
    }
    
    // Battery saving: Set screen to low brightness during tracking
    private var originalBrightness: Float = -1f
    
    private fun setLowBrightness() {
        val layoutParams = window.attributes
        if (originalBrightness < 0) {
            originalBrightness = layoutParams.screenBrightness
        }
        // Set to 30% brightness (0.3f) - enough to see but saves significant battery
        layoutParams.screenBrightness = 0.3f
        window.attributes = layoutParams
    }
    
    private fun restoreNormalBrightness() {
        if (originalBrightness >= 0) {
            val layoutParams = window.attributes
            layoutParams.screenBrightness = originalBrightness
            window.attributes = layoutParams
        }
    }
}
