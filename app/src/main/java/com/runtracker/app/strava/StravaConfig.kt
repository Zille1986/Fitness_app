package com.runtracker.app.strava

object StravaConfig {
    const val CLIENT_ID = "193616"
    const val CLIENT_SECRET = "2adea0b7d31fe370b680fb25bec73da08f2a1ff1"
    const val REDIRECT_URI = "http://localhost/callback"
    const val AUTH_URL = "https://www.strava.com/oauth/authorize"
    const val TOKEN_URL = "https://www.strava.com/oauth/token"
    const val API_BASE_URL = "https://www.strava.com/api/v3/"
    
    const val SCOPE = "activity:write,activity:read_all"
}
