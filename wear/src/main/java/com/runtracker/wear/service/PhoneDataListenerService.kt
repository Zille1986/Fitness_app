package com.runtracker.wear.service

import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import com.runtracker.shared.data.model.ScheduledWorkout
import com.runtracker.shared.sync.DataLayerPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PhoneDataListenerService : WearableListenerService() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val gson = Gson()

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        
        Log.d("PhoneDataListener", "onDataChanged called with ${dataEvents.count} events")
        
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path ?: return@forEach
                Log.d("PhoneDataListener", "Received data on path: $path")
                
                when {
                    path.startsWith(DataLayerPaths.SCHEDULED_WORKOUT_PATH) -> {
                        handleScheduledWorkout(event)
                    }
                    path.startsWith(DataLayerPaths.RUN_CONTROL_PATH) -> {
                        handleRunControl(event.dataItem.data)
                    }
                    path.startsWith(DataLayerPaths.TRAINING_PLAN_PATH) -> {
                        handleTrainingPlanSync(event.dataItem.data)
                    }
                    path.startsWith(DataLayerPaths.CUSTOM_WORKOUTS_PATH) -> {
                        handleCustomWorkoutsSync(event)
                    }
                    path.startsWith(DataLayerPaths.GAMIFICATION_PATH) -> {
                        handleGamificationSync(event)
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        
        when (messageEvent.path) {
            DataLayerPaths.RUN_CONTROL_PATH -> {
                val command = String(messageEvent.data)
                handleRunCommand(command)
            }
        }
    }

    private fun handleRunControl(data: ByteArray?) {
        data ?: return
        val command = String(data)
        handleRunCommand(command)
    }

    private fun handleRunCommand(command: String) {
        val serviceIntent = Intent(this, WearRunTrackingService::class.java)
        
        when (command) {
            DataLayerPaths.COMMAND_START -> {
                serviceIntent.action = WearRunTrackingService.ACTION_START
                startForegroundService(serviceIntent)
            }
            DataLayerPaths.COMMAND_PAUSE -> {
                serviceIntent.action = WearRunTrackingService.ACTION_PAUSE
                startService(serviceIntent)
            }
            DataLayerPaths.COMMAND_RESUME -> {
                serviceIntent.action = WearRunTrackingService.ACTION_RESUME
                startService(serviceIntent)
            }
            DataLayerPaths.COMMAND_STOP -> {
                serviceIntent.action = WearRunTrackingService.ACTION_STOP
                startService(serviceIntent)
            }
        }
    }

    private fun handleTrainingPlanSync(data: ByteArray?) {
        // Handle training plan sync from phone
        // Store locally for offline access
    }
    
    private fun handleCustomWorkoutsSync(event: DataEvent) {
        try {
            val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
            val workoutsJson = dataMapItem.dataMap.getString(DataLayerPaths.KEY_CUSTOM_WORKOUTS_JSON)
            
            Log.d("PhoneDataListener", "Received custom workouts: ${workoutsJson?.take(200)}...")
            
            workoutsJson?.let { json ->
                // Store in shared preferences for the watch app to access
                val sharedPrefs = getSharedPreferences("custom_workouts", MODE_PRIVATE)
                sharedPrefs.edit()
                    .putString("workouts_list", json)
                    .putLong("last_sync", System.currentTimeMillis())
                    .apply()
                
                Log.d("PhoneDataListener", "Custom workouts saved to shared preferences")
                
                serviceScope.launch {
                    Toast.makeText(
                        applicationContext,
                        "Custom workouts synced",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e("PhoneDataListener", "Error handling custom workouts sync", e)
        }
    }
    
    private fun handleGamificationSync(event: DataEvent) {
        try {
            val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
            val gamificationJson = dataMapItem.dataMap.getString(DataLayerPaths.KEY_GAMIFICATION_JSON)
            
            Log.d("PhoneDataListener", "Received gamification data")
            
            gamificationJson?.let { json ->
                val sharedPrefs = getSharedPreferences("gamification", MODE_PRIVATE)
                sharedPrefs.edit()
                    .putString("gamification_data", json)
                    .putLong("last_sync", System.currentTimeMillis())
                    .apply()
                
                Log.d("PhoneDataListener", "Gamification data saved")
            }
        } catch (e: Exception) {
            Log.e("PhoneDataListener", "Error handling gamification sync", e)
        }
    }
    
    private fun handleScheduledWorkout(event: DataEvent) {
        try {
            val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
            val workoutJson = dataMapItem.dataMap.getString(DataLayerPaths.KEY_WORKOUT_JSON)
            val autoStart = dataMapItem.dataMap.getBoolean(DataLayerPaths.KEY_AUTO_START, false)
            
            Log.d("PhoneDataListener", "Received workout: $workoutJson, autoStart: $autoStart")
            
            workoutJson?.let { json ->
                val workout = gson.fromJson(json, ScheduledWorkout::class.java)
                
                // Store the workout for when user starts a run
                WorkoutHolder.pendingWorkout = workout
                WorkoutHolder.pendingIntervals = workout.intervals
                
                // Always launch the watch app to show preview - never auto-start
                Log.d("PhoneDataListener", "Launching workout preview on watch")
                
                // Launch the watch app to foreground to show preview
                val launchIntent = Intent(this, com.runtracker.wear.presentation.MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(launchIntent)
                
                // Show toast that workout is ready for preview
                serviceScope.launch {
                    Toast.makeText(
                        applicationContext,
                        "Workout received - tap to preview",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e("PhoneDataListener", "Error handling workout", e)
        }
    }
    
    companion object {
        const val EXTRA_AUTO_START = "extra_auto_start"
    }
}

object WorkoutHolder {
    var pendingWorkout: ScheduledWorkout? = null
    var pendingIntervals: List<com.runtracker.shared.data.model.Interval>? = null
    var pendingActivityType: String? = null  // RUNNING, SWIMMING, CYCLING
    var pendingSwimType: String? = null      // POOL, OCEAN, LAKE, OPEN_WATER
    var pendingCyclingType: String? = null   // OUTDOOR, INDOOR, SMART_TRAINER
    var pendingSwimWorkoutType: String? = null
    var pendingCyclingWorkoutType: String? = null
    var pendingPoolLength: Int? = null       // Pool length in meters (25, 33, 50)
    var pendingTargetHrMin: Int? = null
    var pendingTargetHrMax: Int? = null
    var pendingTargetHrZone: Int? = null
    var pendingWorkoutDuration: Int? = null  // in seconds
}
