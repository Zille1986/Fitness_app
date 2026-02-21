package com.runtracker.app.service

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.runtracker.shared.data.model.CustomRunWorkout
import com.runtracker.shared.data.model.ScheduledWorkout
import com.runtracker.shared.data.model.TrainingPlan
import com.runtracker.shared.sync.DataLayerPaths
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchSyncService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataClient by lazy { Wearable.getDataClient(context) }
    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient by lazy { Wearable.getNodeClient(context) }
    private val gson = Gson()

    suspend fun sendWorkoutToWatch(workout: ScheduledWorkout, autoStart: Boolean = false): Boolean {
        return try {
            // Check if watch is connected first
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                android.util.Log.d("WatchSyncService", "No watch connected")
                return false
            }
            
            android.util.Log.d("WatchSyncService", "Watch connected: ${nodes.first().displayName}")
            
            val workoutJson = gson.toJson(workout)
            android.util.Log.d("WatchSyncService", "Sending workout: $workoutJson, autoStart: $autoStart")
            
            val putDataRequest = PutDataMapRequest
                .create(DataLayerPaths.SCHEDULED_WORKOUT_PATH)
                .apply {
                    dataMap.putString(DataLayerPaths.KEY_WORKOUT_JSON, workoutJson)
                    dataMap.putBoolean(DataLayerPaths.KEY_AUTO_START, autoStart)
                    dataMap.putLong(DataLayerPaths.KEY_TIMESTAMP, System.currentTimeMillis())
                }
                .asPutDataRequest()
                .setUrgent()

            dataClient.putDataItem(putDataRequest).await()
            android.util.Log.d("WatchSyncService", "Workout sent successfully")
            true
        } catch (e: Exception) {
            android.util.Log.e("WatchSyncService", "Error sending workout", e)
            e.printStackTrace()
            false
        }
    }

    suspend fun sendTrainingPlanToWatch(plan: TrainingPlan): Boolean {
        return try {
            val planJson = gson.toJson(plan)
            
            val putDataRequest = PutDataMapRequest
                .create(DataLayerPaths.TRAINING_PLAN_PATH)
                .apply {
                    dataMap.putString(DataLayerPaths.KEY_PLAN_JSON, planJson)
                    dataMap.putLong(DataLayerPaths.KEY_TIMESTAMP, System.currentTimeMillis())
                }
                .asPutDataRequest()
                .setUrgent()

            dataClient.putDataItem(putDataRequest).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun sendStartRunCommand(): Boolean {
        return sendCommand(DataLayerPaths.COMMAND_START)
    }

    suspend fun sendPauseRunCommand(): Boolean {
        return sendCommand(DataLayerPaths.COMMAND_PAUSE)
    }

    suspend fun sendResumeRunCommand(): Boolean {
        return sendCommand(DataLayerPaths.COMMAND_RESUME)
    }

    suspend fun sendStopRunCommand(): Boolean {
        return sendCommand(DataLayerPaths.COMMAND_STOP)
    }

    private suspend fun sendCommand(command: String): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.forEach { node ->
                messageClient.sendMessage(
                    node.id,
                    DataLayerPaths.RUN_CONTROL_PATH,
                    command.toByteArray()
                ).await()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun isWatchConnected(): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getConnectedWatchName(): String? {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            nodes.firstOrNull()?.displayName
        } catch (e: Exception) {
            null
        }
    }

    suspend fun syncGamificationToWatch(
        currentStreak: Int,
        longestStreak: Int,
        totalXp: Long,
        currentLevel: Int,
        moveProgress: Float,
        exerciseProgress: Float,
        distanceProgress: Float
    ): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                return false
            }
            
            val gamificationData = mapOf(
                "currentStreak" to currentStreak,
                "longestStreak" to longestStreak,
                "totalXp" to totalXp,
                "currentLevel" to currentLevel,
                "moveProgress" to moveProgress,
                "exerciseProgress" to exerciseProgress,
                "distanceProgress" to distanceProgress
            )
            
            val gamificationJson = gson.toJson(gamificationData)
            
            val putDataRequest = PutDataMapRequest
                .create(DataLayerPaths.GAMIFICATION_PATH)
                .apply {
                    dataMap.putString(DataLayerPaths.KEY_GAMIFICATION_JSON, gamificationJson)
                    dataMap.putLong(DataLayerPaths.KEY_TIMESTAMP, System.currentTimeMillis())
                }
                .asPutDataRequest()
                .setUrgent()

            dataClient.putDataItem(putDataRequest).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("WatchSyncService", "Error syncing gamification", e)
            false
        }
    }

    suspend fun syncCustomWorkoutsToWatch(workouts: List<CustomRunWorkout>): Boolean {
        return try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                android.util.Log.d("WatchSyncService", "No watch connected for custom workouts sync")
                return false
            }
            
            // Convert to simplified format for watch
            val syncedWorkouts = workouts.map { workout ->
                mapOf(
                    "id" to workout.id,
                    "name" to workout.name,
                    "description" to workout.description,
                    "phasesCount" to workout.phases.size,
                    "totalDurationSeconds" to workout.totalDurationSeconds,
                    "difficulty" to workout.difficulty.name,
                    "phasesJson" to gson.toJson(workout.phases)
                )
            }
            
            val workoutsJson = gson.toJson(syncedWorkouts)
            android.util.Log.d("WatchSyncService", "Syncing ${workouts.size} custom workouts to watch")
            
            val putDataRequest = PutDataMapRequest
                .create(DataLayerPaths.CUSTOM_WORKOUTS_PATH)
                .apply {
                    dataMap.putString(DataLayerPaths.KEY_CUSTOM_WORKOUTS_JSON, workoutsJson)
                    dataMap.putLong(DataLayerPaths.KEY_TIMESTAMP, System.currentTimeMillis())
                }
                .asPutDataRequest()
                .setUrgent()

            dataClient.putDataItem(putDataRequest).await()
            android.util.Log.d("WatchSyncService", "Custom workouts synced successfully")
            true
        } catch (e: Exception) {
            android.util.Log.e("WatchSyncService", "Error syncing custom workouts", e)
            e.printStackTrace()
            false
        }
    }
}
