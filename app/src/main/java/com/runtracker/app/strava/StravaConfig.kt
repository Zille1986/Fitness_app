package com.runtracker.app.strava

import com.runtracker.app.BuildConfig

object StravaConfig {
    val CLIENT_ID: String get() = BuildConfig.STRAVA_CLIENT_ID
    val CLIENT_SECRET: String get() = BuildConfig.STRAVA_CLIENT_SECRET
    const val REDIRECT_URI = "http://localhost/callback"
    const val AUTH_URL = "https://www.strava.com/oauth/authorize"
    const val TOKEN_URL = "https://www.strava.com/oauth/token"
    const val API_BASE_URL = "https://www.strava.com/api/v3/"

    const val SCOPE = "activity:write,activity:read_all"
}
