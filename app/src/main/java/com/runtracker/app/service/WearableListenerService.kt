package com.runtracker.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import com.runtracker.app.R
import com.runtracker.shared.data.model.HIITSession
import com.runtracker.shared.data.model.RoutePoint
import com.runtracker.shared.data.model.Run
import com.runtracker.shared.data.model.SwimmingWorkout
import com.runtracker.shared.data.model.CyclingWorkout
import com.runtracker.shared.util.CompressionUtils
import kotlinx.coroutines.tasks.await
import com.runtracker.shared.data.repository.PersonalBestRepository
import com.runtracker.shared.data.repository.HIITRepository
import com.runtracker.shared.data.repository.RunRepository
import com.runtracker.shared.data.repository.SwimmingRepository
import com.runtracker.shared.data.repository.CyclingRepository
import com.runtracker.shared.sync.DataLayerPaths
import com.runtracker.app.strava.StravaService
import com.runtracker.app.safety.SafetyService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WearableListenerService : WearableListenerService() {

    @Inject
    lateinit var runRepository: RunRepository
    
    @Inject
    lateinit var swimmingRepository: SwimmingRepository
    
    @Inject
    lateinit var cyclingRepository: CyclingRepository

    @Inject
    lateinit var hiitRepository: HIITRepository
    
    @Inject
    lateinit var personalBestRepository: PersonalBestRepository
    
    @Inject
    lateinit var stravaService: StravaService
    
    @Inject
    lateinit var safetyService: SafetyService

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    
    companion object {
        private const val CHANNEL_ID = "strava_sync_channel"
        private const val NOTIFICATION_ID = 9999
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Strava Sync",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about Strava sync status"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun showStravaFailureNotification(reason: String) {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Strava Upload Failed")
            .setContentText(reason)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        
        Log.d("WearableListener", "onDataChanged called with ${dataEvents.count} events")
        
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            
            val path = event.dataItem.uri.path ?: return@forEach
            Log.d("WearableListener", "Received data on path: $path")
            
            when {
                path.startsWith(DataLayerPaths.RUN_DATA_PATH) -> {
                    handleRunData(event)
                }
                path.startsWith(DataLayerPaths.SWIM_DATA_PATH) -> {
                    handleSwimData(event)
                }
                path.startsWith(DataLayerPaths.CYCLING_DATA_PATH) -> {
                    handleCyclingData(event)
                }
                path.startsWith(DataLayerPaths.HIIT_DATA_PATH) -> {
                    handleHIITData(event)
                }
                path.startsWith(DataLayerPaths.HEART_RATE_PATH) -> {
                    handleHeartRateData(event)
                }
                path.startsWith("/safety") -> {
                    handleSafetyCommand(event)
                }
            }
        }
    }
    
    private fun handleSafetyCommand(event: DataEvent) {
        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
        val path = event.dataItem.uri.path ?: return
        
        Log.d("WearableListener", "Received safety command on path: $path")
        
        serviceScope.launch(Dispatchers.Main) {
            when {
                path.startsWith(DataLayerPaths.SAFETY_SOS_PATH) -> {
                    Log.d("WearableListener", "Triggering SOS from watch")
                    safetyService.triggerSos()
                }
                path.startsWith(DataLayerPaths.SAFETY_PANIC_PATH) -> {
                    Log.d("WearableListener", "Triggering panic alarm from watch")
                    safetyService.startPanicAlarm()
                }
                path.startsWith(DataLayerPaths.SAFETY_FAKE_CALL_PATH) -> {
                    Log.d("WearableListener", "Triggering fake call from watch")
                    safetyService.triggerFakeCall()
                }
                path.startsWith(DataLayerPaths.SAFETY_CHECK_IN_PATH) -> {
                    Log.d("WearableListener", "Check-in from watch")
                    safetyService.checkIn()
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        
        Log.d("WearableListener", "onMessageReceived: path=${messageEvent.path}, dataSize=${messageEvent.data?.size ?: 0}")
        
        when {
            messageEvent.path == DataLayerPaths.SYNC_REQUEST_PATH -> {
                handleSyncRequest()
            }
            messageEvent.path == DataLayerPaths.RUN_DATA_PATH -> {
                // Handle run data sent via MessageClient
                handleRunMessage(messageEvent)
            }
        }
    }
    
    private fun handleRunMessage(messageEvent: MessageEvent) {
        Log.d("WearableListener", "handleRunMessage called - processing run from watch via MessageClient")
        serviceScope.launch {
            try {
                val runJson = String(messageEvent.data ?: return@launch)

                Log.d("WearableListener", "Run JSON received via message, length: ${runJson.length}")

                if (runJson.isEmpty()) {
                    Log.e("WearableListener", "Run JSON is empty")
                    return@launch
                }

                Log.d("WearableListener", "Received run JSON: ${runJson.take(200)}...")

                val run = gson.fromJson(runJson, Run::class.java)
                Log.d("WearableListener", "Parsed run: distance=${run.distanceMeters}m, duration=${run.durationMillis}ms, routePoints=${run.routePoints.size}")

                // Check for duplicate run (same startTime)
                val existingRun = runRepository.getRunByStartTime(run.startTime)
                if (existingRun != null) {
                    Log.d("WearableListener", "Run with startTime ${run.startTime} already exists (id=${existingRun.id}), skipping duplicate")
                    return@launch
                }

                saveAndUploadRun(run)
                Log.d("WearableListener", "Run saved successfully from watch (via MessageClient)")

            } catch (e: Exception) {
                Log.e("WearableListener", "Error handling run message", e)
                e.printStackTrace()
            }
        }
    }

    private fun handleRunData(event: DataEvent) {
        Log.d("WearableListener", "handleRunData called - processing run from watch")
        serviceScope.launch {
            try {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val dataMap = dataMapItem.dataMap

                // Check for new Asset-based format (for large runs with many route points)
                if (dataMap.containsKey(DataLayerPaths.KEY_RUN_META_JSON)) {
                    handleRunWithAsset(event, dataMap)
                } else {
                    // Legacy format (for small runs)
                    handleLegacyRunData(dataMap)
                }
            } catch (e: Exception) {
                Log.e("WearableListener", "Error handling run data", e)
                e.printStackTrace()
            }
        }
    }

    private suspend fun handleRunWithAsset(event: DataEvent, dataMap: DataMap) {
        try {
            // 1. Parse run metadata
            val metaJson = dataMap.getString(DataLayerPaths.KEY_RUN_META_JSON)
            if (metaJson.isNullOrEmpty()) {
                Log.e("WearableListener", "Run meta JSON is null or empty")
                return
            }

            val routePointCount = dataMap.getInt(DataLayerPaths.KEY_ROUTE_POINT_COUNT)
            Log.d("WearableListener", "Received Asset-based run with $routePointCount route points")

            val runMeta = gson.fromJson(metaJson, Run::class.java)
            Log.d("WearableListener", "Parsed run metadata: distance=${runMeta.distanceMeters}m, duration=${runMeta.durationMillis}ms")

            // 2. Extract and decompress route points from Asset
            val routeAsset = dataMap.getAsset(DataLayerPaths.RUN_ROUTE_ASSET_KEY)
            var routePoints: List<RoutePoint> = emptyList()

            if (routeAsset != null) {
                try {
                    val dataClient = Wearable.getDataClient(this)
                    val assetInputStream = dataClient.getFdForAsset(routeAsset).await()
                    val compressedData = assetInputStream.inputStream.readBytes()
                    assetInputStream.inputStream.close()

                    Log.d("WearableListener", "Received compressed route data: ${compressedData.size} bytes")

                    val decompressed = CompressionUtils.gzipDecompress(compressedData)
                    Log.d("WearableListener", "Decompressed route data: ${decompressed.size} bytes")

                    val routePointsType = object : TypeToken<List<RoutePoint>>() {}.type
                    routePoints = gson.fromJson(String(decompressed), routePointsType)
                    Log.d("WearableListener", "Parsed ${routePoints.size} route points")
                } catch (e: Exception) {
                    Log.e("WearableListener", "Error extracting route points from Asset, saving run without routes", e)
                    // Continue with empty route points as fallback
                }
            } else {
                Log.w("WearableListener", "Route asset is null, saving run without route points")
            }

            // 3. Reconstruct full run
            val fullRun = runMeta.copy(routePoints = routePoints)
            Log.d("WearableListener", "Reconstructed full run with ${fullRun.routePoints.size} route points")

            // 4. Save and upload
            saveAndUploadRun(fullRun)

        } catch (e: Exception) {
            Log.e("WearableListener", "Error handling Asset-based run", e)
            e.printStackTrace()
        }
    }

    private suspend fun handleLegacyRunData(dataMap: DataMap) {
        val runJson = dataMap.getString(DataLayerPaths.KEY_RUN_JSON)

        Log.d("WearableListener", "Run JSON received (legacy format), length: ${runJson?.length ?: 0}")

        if (runJson.isNullOrEmpty()) {
            Log.e("WearableListener", "Run JSON is null or empty")
            return
        }

        Log.d("WearableListener", "Received run JSON: ${runJson.take(200)}...")

        val run = gson.fromJson(runJson, Run::class.java)
        Log.d("WearableListener", "Parsed run: distance=${run.distanceMeters}m, duration=${run.durationMillis}ms, routePoints=${run.routePoints.size}")

        saveAndUploadRun(run)
    }

    private suspend fun saveAndUploadRun(run: Run) {
        // Check for duplicate run (same startTime)
        val existingRun = runRepository.getRunByStartTime(run.startTime)
        if (existingRun != null) {
            Log.d("WearableListener", "Run with startTime ${run.startTime} already exists (id=${existingRun.id}), skipping duplicate")
            return
        }

        // Insert as new run from watch (watch runs have id=0)
        Log.d("WearableListener", "Inserting run into database...")
        val insertedId = runRepository.insertRun(run)
        Log.d("WearableListener", "Inserted run with id: $insertedId")

        val savedRun = run.copy(id = insertedId)

        // Check for personal bests
        try {
            val pbUpdates = personalBestRepository.checkAndUpdatePersonalBests(savedRun)
            if (pbUpdates.isNotEmpty()) {
                Log.d("WearableListener", "New PBs from watch run: ${pbUpdates.map { it.distanceName }}")
            }
        } catch (e: Exception) {
            Log.e("WearableListener", "Error checking PBs", e)
        }

        // Upload to Strava — let uploadRun() handle token refresh internally
        // Don't gate on isConnected because SharedPreferences can be stale
        // when WearableListenerService is cold-started by the system
        try {
            Log.d("WearableListener", "Attempting Strava upload for watch run...")
            val result = stravaService.uploadRun(savedRun)
            result.fold(
                onSuccess = { stravaId ->
                    Log.d("WearableListener", "Successfully uploaded to Strava, id: $stravaId")
                    // Update run with Strava ID so it shows as uploaded
                    try {
                        runRepository.updateRun(savedRun.copy(stravaId = stravaId.toString()))
                    } catch (e: Exception) {
                        Log.e("WearableListener", "Failed to update run with Strava ID", e)
                    }
                },
                onFailure = { error ->
                    Log.e("WearableListener", "Strava upload failed: ${error.message}", error)
                    if (error.message?.contains("Not authenticated") == true) {
                        Log.d("WearableListener", "Strava not connected, skipping notification")
                    } else {
                        showStravaFailureNotification("Run saved locally but Strava upload failed: ${error.message}")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("WearableListener", "Unexpected error during Strava upload", e)
        }

        Log.d("WearableListener", "Run saved successfully from watch")
    }

    private fun handleSwimData(event: DataEvent) {
        serviceScope.launch {
            try {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val swimJson = dataMapItem.dataMap.getString(DataLayerPaths.KEY_SWIM_JSON)
                
                if (swimJson.isNullOrEmpty()) {
                    Log.e("WearableListener", "Swim JSON is null or empty")
                    return@launch
                }
                
                Log.d("WearableListener", "Received swim JSON: ${swimJson.take(200)}...")
                
                val swim = gson.fromJson(swimJson, SwimmingWorkout::class.java)
                Log.d("WearableListener", "Parsed swim: distance=${swim.distanceMeters}m, duration=${swim.durationMillis}ms")
                
                // Insert swim workout
                val insertedId = swimmingRepository.insertWorkout(swim)
                Log.d("WearableListener", "Inserted swim with id: $insertedId")
                
                val savedSwim = swim.copy(id = insertedId)
                
                // Upload to Strava — let upload handle token refresh internally
                try {
                    Log.d("WearableListener", "Attempting Strava upload for watch swim...")
                    val result = stravaService.uploadSwim(savedSwim)
                    result.fold(
                        onSuccess = { stravaId ->
                            Log.d("WearableListener", "Successfully uploaded swim to Strava, id: $stravaId")
                        },
                        onFailure = { error ->
                            Log.e("WearableListener", "Strava swim upload failed: ${error.message}", error)
                            if (error.message?.contains("Not authenticated") != true) {
                                showStravaFailureNotification("Swim saved locally but Strava upload failed: ${error.message}")
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e("WearableListener", "Unexpected error during Strava swim upload", e)
                }
                
                Log.d("WearableListener", "Swim saved successfully from watch")
                
            } catch (e: Exception) {
                Log.e("WearableListener", "Error handling swim data", e)
                e.printStackTrace()
            }
        }
    }
    
    private fun handleCyclingData(event: DataEvent) {
        serviceScope.launch {
            try {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val cyclingJson = dataMapItem.dataMap.getString(DataLayerPaths.KEY_CYCLING_JSON)
                
                if (cyclingJson.isNullOrEmpty()) {
                    Log.e("WearableListener", "Cycling JSON is null or empty")
                    return@launch
                }
                
                Log.d("WearableListener", "Received cycling JSON: ${cyclingJson.take(200)}...")
                
                val ride = gson.fromJson(cyclingJson, CyclingWorkout::class.java)
                Log.d("WearableListener", "Parsed ride: distance=${ride.distanceMeters}m, duration=${ride.durationMillis}ms")
                
                // Insert cycling workout
                val insertedId = cyclingRepository.insertWorkout(ride)
                Log.d("WearableListener", "Inserted ride with id: $insertedId")
                
                val savedRide = ride.copy(id = insertedId)
                
                // Upload to Strava — let upload handle token refresh internally
                try {
                    Log.d("WearableListener", "Attempting Strava upload for watch ride...")
                    val result = stravaService.uploadCycling(savedRide)
                    result.fold(
                        onSuccess = { stravaId ->
                            Log.d("WearableListener", "Successfully uploaded ride to Strava, id: $stravaId")
                        },
                        onFailure = { error ->
                            Log.e("WearableListener", "Strava cycling upload failed: ${error.message}", error)
                            if (error.message?.contains("Not authenticated") != true) {
                                showStravaFailureNotification("Ride saved locally but Strava upload failed: ${error.message}")
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e("WearableListener", "Unexpected error during Strava cycling upload", e)
                }
                
                Log.d("WearableListener", "Ride saved successfully from watch")
                
            } catch (e: Exception) {
                Log.e("WearableListener", "Error handling cycling data", e)
                e.printStackTrace()
            }
        }
    }

    private fun handleHIITData(event: DataEvent) {
        serviceScope.launch {
            try {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val hiitJson = dataMapItem.dataMap.getString(DataLayerPaths.KEY_HIIT_JSON)

                if (hiitJson.isNullOrEmpty()) {
                    Log.e("WearableListener", "HIIT JSON is null or empty")
                    return@launch
                }

                Log.d("WearableListener", "Received HIIT JSON: ${hiitJson.take(200)}...")

                // Parse the session data from watch
                val sessionData = gson.fromJson(hiitJson, Map::class.java)
                val templateId = sessionData["templateId"] as? String ?: ""
                val templateName = sessionData["templateName"] as? String ?: "HIIT Workout"
                val totalDurationMs = ((sessionData["totalDurationMs"] as? Number)?.toLong()) ?: 0L
                val exerciseCount = ((sessionData["exerciseCount"] as? Number)?.toInt()) ?: 0
                val roundsCompleted = ((sessionData["roundsCompleted"] as? Number)?.toInt()) ?: 0
                val totalRounds = ((sessionData["totalRounds"] as? Number)?.toInt()) ?: 0
                val caloriesEstimate = ((sessionData["caloriesEstimate"] as? Number)?.toInt()) ?: 0
                val isCompleted = sessionData["isCompleted"] as? Boolean ?: true

                val hiitSession = HIITSession(
                    date = System.currentTimeMillis(),
                    templateId = templateId,
                    templateName = templateName,
                    totalDurationMs = totalDurationMs,
                    exerciseCount = exerciseCount,
                    roundsCompleted = roundsCompleted,
                    totalRounds = totalRounds,
                    caloriesEstimate = caloriesEstimate,
                    isCompleted = isCompleted,
                    source = "watch"
                )

                val insertedId = hiitRepository.insertSession(hiitSession)
                Log.d("WearableListener", "HIIT session saved from watch with id: $insertedId")

            } catch (e: Exception) {
                Log.e("WearableListener", "Error handling HIIT data", e)
                e.printStackTrace()
            }
        }
    }

    private fun handleHeartRateData(event: DataEvent) {
        // Heart rate data from watch during phone-initiated run
        try {
            val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
            val heartRate = dataMapItem.dataMap.getInt(DataLayerPaths.KEY_HEART_RATE)
            Log.d("WearableListener", "Received heart rate: $heartRate")
        } catch (e: Exception) {
            Log.e("WearableListener", "Error handling heart rate data", e)
        }
    }

    private fun handleSyncRequest() {
        // Watch is requesting sync of training plans or recent runs
        serviceScope.launch {
            // Send training plans and recent runs to watch
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
