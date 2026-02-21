package com.runtracker.app.health

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessActivities
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.SessionInsertRequest
import com.google.android.gms.fitness.request.SessionReadRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class GoogleFitManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1001
    }

    private val fitnessOptions: FitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_WRITE)
        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_WRITE)
        .addDataType(DataType.TYPE_WORKOUT_EXERCISE, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_WORKOUT_EXERCISE, FitnessOptions.ACCESS_WRITE)
        .addDataType(DataType.TYPE_WEIGHT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_WEIGHT, FitnessOptions.ACCESS_WRITE)
        .build()

    private val _connectionState = MutableStateFlow(GoogleFitConnectionState.NOT_CONNECTED)
    val connectionState: StateFlow<GoogleFitConnectionState> = _connectionState.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun isConnected(): Boolean {
        val account = getGoogleAccount()
        return account != null && GoogleSignIn.hasPermissions(account, fitnessOptions)
    }

    private fun getGoogleAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getAccountForExtension(context, fitnessOptions)
    }

    fun getSignInIntent(): Intent {
        return GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signInIntent
    }

    fun requestPermissions(activity: Activity) {
        val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                activity,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                account,
                fitnessOptions
            )
        } else {
            _connectionState.value = GoogleFitConnectionState.CONNECTED
        }
    }

    fun handlePermissionResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            _connectionState.value = GoogleFitConnectionState.CONNECTED
        } else {
            _connectionState.value = GoogleFitConnectionState.PERMISSION_DENIED
        }
    }

    fun disconnect() {
        val account = getGoogleAccount() ?: return
        Fitness.getConfigClient(context, account)
            .disableFit()
            .addOnSuccessListener {
                _connectionState.value = GoogleFitConnectionState.NOT_CONNECTED
            }
    }

    suspend fun syncRunToGoogleFit(
        startTimeMillis: Long,
        endTimeMillis: Long,
        distanceMeters: Float,
        calories: Float,
        steps: Int
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val account = getGoogleAccount()
        if (account == null || !isConnected()) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        _syncState.value = SyncState.SYNCING

        val dataSource = DataSource.Builder()
            .setAppPackageName(context)
            .setDataType(DataType.TYPE_DISTANCE_DELTA)
            .setType(DataSource.TYPE_RAW)
            .build()

        val distanceDataPoint = DataPoint.builder(dataSource)
            .setTimeInterval(startTimeMillis, endTimeMillis, TimeUnit.MILLISECONDS)
            .setField(Field.FIELD_DISTANCE, distanceMeters)
            .build()

        val distanceDataSet = DataSet.builder(dataSource)
            .add(distanceDataPoint)
            .build()

        // Create session
        val session = Session.Builder()
            .setName("Run")
            .setDescription("Running workout from RunTracker")
            .setIdentifier("run_${startTimeMillis}")
            .setActivity(FitnessActivities.RUNNING)
            .setStartTime(startTimeMillis, TimeUnit.MILLISECONDS)
            .setEndTime(endTimeMillis, TimeUnit.MILLISECONDS)
            .build()

        val insertRequest = SessionInsertRequest.Builder()
            .setSession(session)
            .addDataSet(distanceDataSet)
            .build()

        Fitness.getSessionsClient(context, account)
            .insertSession(insertRequest)
            .addOnSuccessListener {
                _syncState.value = SyncState.SUCCESS
                continuation.resume(true)
            }
            .addOnFailureListener { e ->
                _syncState.value = SyncState.ERROR
                continuation.resume(false)
            }
    }

    suspend fun syncGymWorkoutToGoogleFit(
        startTimeMillis: Long,
        endTimeMillis: Long,
        calories: Float,
        workoutName: String
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val account = getGoogleAccount()
        if (account == null || !isConnected()) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        _syncState.value = SyncState.SYNCING

        val session = Session.Builder()
            .setName(workoutName)
            .setDescription("Gym workout from RunTracker")
            .setIdentifier("gym_${startTimeMillis}")
            .setActivity(FitnessActivities.STRENGTH_TRAINING)
            .setStartTime(startTimeMillis, TimeUnit.MILLISECONDS)
            .setEndTime(endTimeMillis, TimeUnit.MILLISECONDS)
            .build()

        val insertRequest = SessionInsertRequest.Builder()
            .setSession(session)
            .build()

        Fitness.getSessionsClient(context, account)
            .insertSession(insertRequest)
            .addOnSuccessListener {
                _syncState.value = SyncState.SUCCESS
                continuation.resume(true)
            }
            .addOnFailureListener { e ->
                _syncState.value = SyncState.ERROR
                continuation.resume(false)
            }
    }

    suspend fun getTodaySteps(): Int = suspendCancellableCoroutine { continuation ->
        val account = getGoogleAccount()
        if (account == null || !isConnected()) {
            continuation.resume(0)
            return@suspendCancellableCoroutine
        }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(1)

        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(context, account)
            .readData(readRequest)
            .addOnSuccessListener { response ->
                var totalSteps = 0
                for (bucket in response.buckets) {
                    for (dataSet in bucket.dataSets) {
                        for (dataPoint in dataSet.dataPoints) {
                            totalSteps += dataPoint.getValue(Field.FIELD_STEPS).asInt()
                        }
                    }
                }
                continuation.resume(totalSteps)
            }
            .addOnFailureListener {
                continuation.resume(0)
            }
    }

    suspend fun syncWeight(weightKg: Float, timestampMillis: Long): Boolean = 
        suspendCancellableCoroutine { continuation ->
            val account = getGoogleAccount()
            if (account == null || !isConnected()) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            val dataSource = DataSource.Builder()
                .setAppPackageName(context)
                .setDataType(DataType.TYPE_WEIGHT)
                .setType(DataSource.TYPE_RAW)
                .build()

            val weightDataPoint = DataPoint.builder(dataSource)
                .setTimestamp(timestampMillis, TimeUnit.MILLISECONDS)
                .setField(Field.FIELD_WEIGHT, weightKg)
                .build()

            val dataSet = DataSet.builder(dataSource)
                .add(weightDataPoint)
                .build()

            Fitness.getHistoryClient(context, account)
                .insertData(dataSet)
                .addOnSuccessListener {
                    continuation.resume(true)
                }
                .addOnFailureListener {
                    continuation.resume(false)
                }
        }
}

enum class GoogleFitConnectionState {
    NOT_CONNECTED,
    CONNECTING,
    CONNECTED,
    PERMISSION_DENIED,
    ERROR
}

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}
