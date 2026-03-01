package com.runtracker.app.strava

import android.content.Context
import android.content.SharedPreferences
import com.runtracker.shared.data.model.Run
import com.runtracker.shared.data.model.SwimmingWorkout
import com.runtracker.shared.data.model.CyclingWorkout
import com.runtracker.shared.data.model.CyclingType
import com.runtracker.shared.data.model.SwimType
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.runtracker.shared.util.retryWithBackoff
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StravaService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Use getter to avoid stale SharedPreferences when service is cold-started
    private val prefs: SharedPreferences
        get() = context.getSharedPreferences("strava_prefs", Context.MODE_PRIVATE)
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
    
    // OAuth endpoints (token exchange, refresh)
    private val authRetrofit = Retrofit.Builder()
        .baseUrl("https://www.strava.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    // API endpoints (activities, athlete, uploads)
    private val apiRetrofit = Retrofit.Builder()
        .baseUrl("https://www.strava.com/api/v3/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val authApi = authRetrofit.create(StravaApi::class.java)
    private val api = apiRetrofit.create(StravaApi::class.java)
    
    val isConnected: Boolean
        get() = getAccessToken() != null
    
    val athleteName: String?
        get() = prefs.getString("athlete_name", null)
    
    fun getAuthUrl(): String {
        return "${StravaConfig.AUTH_URL}?" +
                "client_id=${StravaConfig.CLIENT_ID}&" +
                "redirect_uri=${StravaConfig.REDIRECT_URI}&" +
                "response_type=code&" +
                "scope=${StravaConfig.SCOPE}"
    }
    
    suspend fun handleAuthCallback(code: String): Boolean {
        return try {
            val response = authApi.exchangeToken(
                clientId = StravaConfig.CLIENT_ID,
                clientSecret = StravaConfig.CLIENT_SECRET,
                code = code
            )
            
            if (response.isSuccessful) {
                response.body()?.let { tokenResponse ->
                    saveTokens(tokenResponse)
                    true
                } ?: false
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun saveTokens(tokenResponse: StravaTokenResponse) {
        prefs.edit().apply {
            putString("access_token", tokenResponse.access_token)
            putString("refresh_token", tokenResponse.refresh_token)
            putLong("expires_at", tokenResponse.expires_at)
            tokenResponse.athlete?.let { athlete ->
                putString("athlete_name", "${athlete.firstname} ${athlete.lastname}")
                putLong("athlete_id", athlete.id)
            }
            apply()
        }
    }
    
    private fun getAccessToken(): String? = prefs.getString("access_token", null)
    private fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
    private fun getExpiresAt(): Long = prefs.getLong("expires_at", 0)
    
    private suspend fun ensureValidToken(): String? {
        val currentTime = System.currentTimeMillis() / 1000
        val expiresAt = getExpiresAt()
        
        android.util.Log.d("StravaService", "Token check: current=$currentTime, expires=$expiresAt, diff=${expiresAt - currentTime}s")
        
        if (currentTime >= expiresAt - 60) {
            // Token expired or about to expire, refresh it
            val refreshToken = getRefreshToken()
            if (refreshToken == null) {
                android.util.Log.e("StravaService", "No refresh token available")
                return null
            }
            
            android.util.Log.d("StravaService", "Refreshing expired token...")
            
            try {
                val response = authApi.refreshToken(
                    clientId = StravaConfig.CLIENT_ID,
                    clientSecret = StravaConfig.CLIENT_SECRET,
                    refreshToken = refreshToken
                )
                
                if (response.isSuccessful) {
                    response.body()?.let { 
                        saveTokens(it)
                        android.util.Log.d("StravaService", "Token refreshed successfully, new expiry: ${it.expires_at}")
                    }
                } else {
                    android.util.Log.e("StravaService", "Token refresh failed: ${response.code()} - ${response.errorBody()?.string()}")
                    return null
                }
            } catch (e: Exception) {
                android.util.Log.e("StravaService", "Token refresh exception: ${e.message}", e)
                return null
            }
        }
        
        return getAccessToken()
    }
    
    suspend fun uploadRun(run: Run): Result<Long> {
        android.util.Log.d("StravaService", "uploadRun called: distance=${run.distanceMeters}m, routePoints=${run.routePoints.size}, hasToken=${getAccessToken() != null}")
        val token = ensureValidToken() ?: return Result.failure(Exception("Not authenticated"))

        return try {
            // If we have GPS data, upload as GPX file for full route
            if (run.routePoints.isNotEmpty()) {
                android.util.Log.d("StravaService", "Uploading with GPX (${run.routePoints.size} points)")
                uploadRunWithGpx(run, token)
            } else {
                // Fallback to simple activity creation without GPS
                android.util.Log.d("StravaService", "Uploading simple (no GPS data)")
                uploadRunSimple(run, token)
            }
        } catch (e: Exception) {
            android.util.Log.e("StravaService", "uploadRun failed", e)
            Result.failure(e)
        }
    }
    
    private suspend fun uploadRunWithGpx(run: Run, token: String): Result<Long> {
        return retryWithBackoff(times = 3, initialDelayMs = 500) {
            val gpxContent = GpxGenerator.generateGpx(run)

            val gpxBody = gpxContent.toRequestBody("application/gpx+xml".toMediaType())
            val gpxPart = okhttp3.MultipartBody.Part.createFormData("file", "activity.gpx", gpxBody)

            val dataType = "gpx".toRequestBody("text/plain".toMediaType())
            val name = generateRunName(run).toRequestBody("text/plain".toMediaType())
            val description = generateDescription(run).toRequestBody("text/plain".toMediaType())
            val sportType = "run".toRequestBody("text/plain".toMediaType())

            val response = api.uploadActivity(
                authorization = "Bearer $token",
                file = gpxPart,
                dataType = dataType,
                name = name,
                description = description,
                sportType = sportType
            )

            if (response.isSuccessful) {
                response.body()?.id ?: throw Exception("Empty response")
            } else {
                // If GPX upload fails, try simple upload
                android.util.Log.w("StravaService", "GPX upload failed: ${response.code()}, trying simple upload")
                val simpleResult = uploadRunSimple(run, token)
                simpleResult.getOrThrow()
            }
        }
    }
    
    private suspend fun uploadRunSimple(run: Run, token: String): Result<Long> {
        return retryWithBackoff(times = 3, initialDelayMs = 500) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")

            val activity = StravaActivityCreate(
                name = generateRunName(run),
                sport_type = "Run",
                start_date_local = dateFormat.format(Date(run.startTime)),
                elapsed_time = (run.durationMillis / 1000).toInt(),
                distance = run.distanceMeters.toFloat(),
                description = generateDescription(run)
            )

            val response = api.createActivity(
                authorization = "Bearer $token",
                activity = activity
            )

            if (response.isSuccessful) {
                response.body()?.id ?: throw Exception("Empty response")
            } else {
                throw Exception("Upload failed: ${response.code()}")
            }
        }
    }
    
    private fun generateRunName(run: Run): String {
        val distanceKm = run.distanceMeters / 1000.0
        val calendar = Calendar.getInstance().apply { timeInMillis = run.startTime }
        val timeOfDay = when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..20 -> "Evening"
            else -> "Night"
        }
        return "$timeOfDay Run - %.1f km".format(distanceKm)
    }
    
    private fun generateDescription(run: Run): String {
        val paceMin = (run.avgPaceSecondsPerKm / 60).toInt()
        val paceSec = (run.avgPaceSecondsPerKm % 60).toInt()
        
        return buildString {
            append("Tracked with RunTracker\n")
            append("Pace: $paceMin:${String.format("%02d", paceSec)}/km\n")
            run.avgHeartRate?.let { append("Avg HR: $it bpm\n") }
            run.caloriesBurned?.let { append("Calories: $it\n") }
            if (run.elevationGainMeters > 0) {
                append("Elevation: +${run.elevationGainMeters.toInt()}m / -${run.elevationLossMeters.toInt()}m")
            }
        }
    }
    
    // Swimming upload
    suspend fun uploadSwim(swim: SwimmingWorkout): Result<Long> {
        val token = ensureValidToken() ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            
            val sportType = when (swim.swimType) {
                SwimType.POOL -> "Swim"
                SwimType.OCEAN, SwimType.LAKE, SwimType.RIVER -> "Open Water Swim"
            }
            
            val activity = StravaActivityCreate(
                name = generateSwimName(swim),
                sport_type = sportType,
                start_date_local = dateFormat.format(Date(swim.startTime)),
                elapsed_time = (swim.durationMillis / 1000).toInt(),
                distance = swim.distanceMeters.toFloat(),
                description = generateSwimDescription(swim)
            )
            
            val response = api.createActivity(
                authorization = "Bearer $token",
                activity = activity
            )
            
            if (response.isSuccessful) {
                response.body()?.let { 
                    Result.success(it.id)
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private fun generateSwimName(swim: SwimmingWorkout): String {
        val distanceM = swim.distanceMeters.toInt()
        val calendar = Calendar.getInstance().apply { timeInMillis = swim.startTime }
        val timeOfDay = when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..20 -> "Evening"
            else -> "Night"
        }
        val swimType = when (swim.swimType) {
            SwimType.POOL -> "Pool Swim"
            SwimType.OCEAN -> "Ocean Swim"
            SwimType.LAKE -> "Lake Swim"
            SwimType.RIVER -> "River Swim"
        }
        return "$timeOfDay $swimType - ${distanceM}m"
    }
    
    private fun generateSwimDescription(swim: SwimmingWorkout): String {
        return buildString {
            append("Tracked with RunTracker\n")
            if (swim.avgPaceSecondsPer100m > 0) {
                val paceMin = (swim.avgPaceSecondsPer100m / 60).toInt()
                val paceSec = (swim.avgPaceSecondsPer100m % 60).toInt()
                append("Pace: $paceMin:${String.format("%02d", paceSec)}/100m\n")
            }
            if (swim.laps > 0) append("Laps: ${swim.laps}\n")
            swim.avgHeartRate?.let { append("Avg HR: $it bpm\n") }
            if (swim.caloriesBurned > 0) append("Calories: ${swim.caloriesBurned}\n")
            swim.swolf?.let { append("SWOLF: $it\n") }
        }
    }
    
    // Cycling upload
    suspend fun uploadCycling(ride: CyclingWorkout): Result<Long> {
        val token = ensureValidToken() ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            if (ride.routePoints.isNotEmpty()) {
                uploadCyclingWithGpx(ride, token)
            } else {
                uploadCyclingSimple(ride, token)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private suspend fun uploadCyclingWithGpx(ride: CyclingWorkout, token: String): Result<Long> {
        val gpxContent = GpxGenerator.generateGpxForCycling(ride)
        
        val gpxBody = gpxContent.toRequestBody("application/gpx+xml".toMediaType())
        val gpxPart = okhttp3.MultipartBody.Part.createFormData("file", "activity.gpx", gpxBody)
        
        val dataType = "gpx".toRequestBody("text/plain".toMediaType())
        val name = generateCyclingName(ride).toRequestBody("text/plain".toMediaType())
        val description = generateCyclingDescription(ride).toRequestBody("text/plain".toMediaType())
        val sportType = if (ride.cyclingType != CyclingType.OUTDOOR) "VirtualRide" else "Ride"
        val sportTypeBody = sportType.toRequestBody("text/plain".toMediaType())
        
        val response = api.uploadActivity(
            authorization = "Bearer $token",
            file = gpxPart,
            dataType = dataType,
            name = name,
            description = description,
            sportType = sportTypeBody
        )
        
        return if (response.isSuccessful) {
            response.body()?.let { 
                Result.success(it.id)
            } ?: Result.failure(Exception("Empty response"))
        } else {
            uploadCyclingSimple(ride, token)
        }
    }
    
    private suspend fun uploadCyclingSimple(ride: CyclingWorkout, token: String): Result<Long> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        
        val sportType = if (ride.cyclingType != CyclingType.OUTDOOR) "VirtualRide" else "Ride"
        
        val activity = StravaActivityCreate(
            name = generateCyclingName(ride),
            sport_type = sportType,
            start_date_local = dateFormat.format(Date(ride.startTime)),
            elapsed_time = (ride.durationMillis / 1000).toInt(),
            distance = ride.distanceMeters.toFloat(),
            description = generateCyclingDescription(ride)
        )
        
        val response = api.createActivity(
            authorization = "Bearer $token",
            activity = activity
        )
        
        return if (response.isSuccessful) {
            response.body()?.let { 
                Result.success(it.id)
            } ?: Result.failure(Exception("Empty response"))
        } else {
            Result.failure(Exception("Upload failed: ${response.code()}"))
        }
    }
    
    private fun generateCyclingName(ride: CyclingWorkout): String {
        val distanceKm = ride.distanceMeters / 1000.0
        val calendar = Calendar.getInstance().apply { timeInMillis = ride.startTime }
        val timeOfDay = when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..20 -> "Evening"
            else -> "Night"
        }
        val rideType = if (ride.cyclingType != CyclingType.OUTDOOR) "Indoor Ride" else "Ride"
        return "$timeOfDay $rideType - %.1f km".format(distanceKm)
    }
    
    private fun generateCyclingDescription(ride: CyclingWorkout): String {
        return buildString {
            append("Tracked with RunTracker\n")
            if (ride.avgSpeedKmh > 0) append("Avg Speed: %.1f km/h\n".format(ride.avgSpeedKmh))
            ride.avgHeartRate?.let { append("Avg HR: $it bpm\n") }
            ride.avgPowerWatts?.let { append("Avg Power: $it W\n") }
            ride.avgCadenceRpm?.let { append("Avg Cadence: $it rpm\n") }
            if (ride.caloriesBurned > 0) append("Calories: ${ride.caloriesBurned}\n")
            if (ride.elevationGainMeters > 0) {
                append("Elevation: +${ride.elevationGainMeters.toInt()}m")
            }
        }
    }
    
    suspend fun getAthlete(): StravaAthlete? {
        val token = ensureValidToken() ?: return null
        
        return try {
            val response = api.getAthlete("Bearer $token")
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getActivities(page: Int = 1, perPage: Int = 30): Result<List<StravaActivity>> {
        val token = ensureValidToken() ?: return Result.failure(Exception("Not authenticated"))
        
        return try {
            val response = api.getActivities(
                authorization = "Bearer $token",
                page = page,
                perPage = perPage
            )
            
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch activities: ${response.code()}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun getRunActivities(page: Int = 1, perPage: Int = 30): Result<List<StravaActivity>> {
        return getActivities(page, perPage).map { activities ->
            activities.filter { activity ->
                activity.sport_type.equals("Run", ignoreCase = true) ||
                activity.type?.equals("Run", ignoreCase = true) == true
            }
        }
    }
    
    fun convertStravaActivityToRun(activity: StravaActivity): Run {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        
        val startTime = try {
            dateFormat.parse(activity.start_date)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
        
        val endTime = startTime + (activity.elapsed_time * 1000L)
        val durationMillis = activity.moving_time * 1000L
        
        // Calculate pace from average speed (m/s to s/km)
        val avgPaceSecondsPerKm = if (activity.average_speed != null && activity.average_speed > 0) {
            1000.0 / activity.average_speed
        } else if (activity.distance > 0 && durationMillis > 0) {
            (durationMillis / 1000.0) / (activity.distance / 1000.0)
        } else {
            0.0
        }
        
        return Run(
            startTime = startTime,
            endTime = endTime,
            distanceMeters = activity.distance.toDouble(),
            durationMillis = durationMillis,
            avgPaceSecondsPerKm = avgPaceSecondsPerKm,
            avgHeartRate = activity.average_heartrate?.toInt(),
            maxHeartRate = activity.max_heartrate?.toInt(),
            caloriesBurned = activity.calories?.toInt() ?: 0,
            elevationGainMeters = activity.total_elevation_gain?.toDouble() ?: 0.0,
            elevationLossMeters = 0.0, // Strava doesn't provide this directly
            routePoints = emptyList(), // Would need separate API call for route
            splits = emptyList(),
            source = com.runtracker.shared.data.model.RunSource.STRAVA,
            stravaId = activity.id.toString(),
            isCompleted = true
        )
    }
    
    fun disconnect() {
        prefs.edit().clear().apply()
    }
}
