package com.runtracker.shared.sync

object DataLayerPaths {
    const val RUN_DATA_PATH = "/run_data"
    const val RUN_CONTROL_PATH = "/run_control"
    const val SWIM_DATA_PATH = "/swim_data"
    const val CYCLING_DATA_PATH = "/cycling_data"
    const val HEART_RATE_PATH = "/heart_rate"
    const val TRAINING_PLAN_PATH = "/training_plan"
    const val SCHEDULED_WORKOUT_PATH = "/scheduled_workout"
    const val SYNC_REQUEST_PATH = "/sync_request"
    const val CUSTOM_WORKOUTS_PATH = "/custom_workouts"
    const val GAMIFICATION_PATH = "/gamification"
    const val DAILY_RINGS_PATH = "/daily_rings"
    
    const val KEY_RUN_ID = "run_id"
    const val KEY_RUN_JSON = "run_json"
    const val KEY_SWIM_JSON = "swim_json"
    const val KEY_CYCLING_JSON = "cycling_json"
    const val KEY_COMMAND = "command"
    const val KEY_HEART_RATE = "heart_rate"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_TRAINING_PLAN_JSON = "training_plan_json"
    const val KEY_PLAN_JSON = "plan_json"
    const val KEY_WORKOUT_JSON = "workout_json"
    const val KEY_AUTO_START = "auto_start"
    const val KEY_CUSTOM_WORKOUTS_JSON = "custom_workouts_json"
    const val KEY_GAMIFICATION_JSON = "gamification_json"
    const val KEY_DAILY_RINGS_JSON = "daily_rings_json"
    
    const val COMMAND_START = "start"
    const val COMMAND_PAUSE = "pause"
    const val COMMAND_RESUME = "resume"
    const val COMMAND_STOP = "stop"
    const val COMMAND_SYNC = "sync"
    
    // Safety paths
    const val SAFETY_SOS_PATH = "/safety/sos"
    const val SAFETY_PANIC_PATH = "/safety/panic"
    const val SAFETY_FAKE_CALL_PATH = "/safety/fake_call"
    const val SAFETY_CHECK_IN_PATH = "/safety/check_in"
    const val KEY_SAFETY_ACTION = "safety_action"
    const val KEY_LATITUDE = "latitude"
    const val KEY_LONGITUDE = "longitude"

    // HIIT paths
    const val HIIT_DATA_PATH = "/hiit_session"
    const val KEY_HIIT_JSON = "hiit_json"

    // Asset-based transfer for large runs (route points exceed DataLayer limits)
    const val RUN_ROUTE_ASSET_KEY = "run_route_asset"
    const val KEY_RUN_META_JSON = "run_meta_json"  // Run without routePoints
    const val KEY_ROUTE_POINT_COUNT = "route_point_count"
}
