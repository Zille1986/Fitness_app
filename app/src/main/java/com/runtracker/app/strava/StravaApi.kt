package com.runtracker.app.strava

import retrofit2.Response
import retrofit2.http.*

interface StravaApi {
    
    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun exchangeToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String = "authorization_code"
    ): Response<StravaTokenResponse>
    
    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): Response<StravaTokenResponse>
    
    @POST("uploads")
    @Multipart
    suspend fun uploadActivity(
        @Header("Authorization") authorization: String,
        @Part file: okhttp3.MultipartBody.Part,
        @Part("data_type") dataType: okhttp3.RequestBody,
        @Part("name") name: okhttp3.RequestBody,
        @Part("description") description: okhttp3.RequestBody?,
        @Part("sport_type") sportType: okhttp3.RequestBody
    ): Response<StravaUploadResponse>
    
    @POST("activities")
    suspend fun createActivity(
        @Header("Authorization") authorization: String,
        @Body activity: StravaActivityCreate
    ): Response<StravaActivity>
    
    @GET("athlete")
    suspend fun getAthlete(
        @Header("Authorization") authorization: String
    ): Response<StravaAthlete>
    
    @GET("athlete/activities")
    suspend fun getActivities(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): Response<List<StravaActivity>>
}

data class StravaTokenResponse(
    val token_type: String,
    val expires_at: Long,
    val expires_in: Int,
    val refresh_token: String,
    val access_token: String,
    val athlete: StravaAthlete?
)

data class StravaAthlete(
    val id: Long,
    val username: String?,
    val firstname: String,
    val lastname: String,
    val profile: String?,
    val profile_medium: String?
)

data class StravaActivityCreate(
    val name: String,
    val sport_type: String,
    val start_date_local: String,
    val elapsed_time: Int,
    val distance: Float,
    val description: String? = null
)

data class StravaActivity(
    val id: Long,
    val name: String,
    val distance: Float,
    val moving_time: Int,
    val elapsed_time: Int,
    val sport_type: String,
    val type: String? = null,
    val start_date: String,
    val start_date_local: String,
    val average_speed: Float? = null,
    val max_speed: Float? = null,
    val average_heartrate: Float? = null,
    val max_heartrate: Float? = null,
    val total_elevation_gain: Float? = null,
    val elev_high: Float? = null,
    val elev_low: Float? = null,
    val calories: Float? = null,
    val description: String? = null,
    val external_id: String? = null
)

data class StravaUploadResponse(
    val id: Long,
    val status: String,
    val activity_id: Long?
)
