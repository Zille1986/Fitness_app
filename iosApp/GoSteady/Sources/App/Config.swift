import Foundation

// MARK: - App Configuration

enum AppConfig {

    // MARK: - Version Info

    static let appVersion: String = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    static let buildNumber: String = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
    static let bundleIdentifier: String = Bundle.main.bundleIdentifier ?? "com.gosteady.app"

    // MARK: - API Keys (loaded from environment or Secrets.plist)

    static let geminiAPIKey: String = loadSecret(key: "GEMINI_API_KEY")
    static let stravaClientID: String = loadSecret(key: "STRAVA_CLIENT_ID")
    static let stravaClientSecret: String = loadSecret(key: "STRAVA_CLIENT_SECRET")
    static let mapsAPIKey: String = loadSecret(key: "MAPS_API_KEY")

    // MARK: - Strava OAuth

    enum Strava {
        static let authorizeURL = "https://www.strava.com/oauth/mobile/authorize"
        static let tokenURL = "https://www.strava.com/oauth/token"
        static let deauthorizeURL = "https://www.strava.com/oauth/deauthorize"
        static let athleteURL = "https://www.strava.com/api/v3/athlete"
        static let activitiesURL = "https://www.strava.com/api/v3/athlete/activities"
        static let callbackScheme = "gosteady"
        static let callbackHost = "strava-callback"
        static let callbackURL = "\(callbackScheme)://\(callbackHost)"
        static let scope = "read,activity:read_all,activity:write"

        static func authorizationURL(clientID: String) -> URL? {
            var components = URLComponents(string: authorizeURL)
            components?.queryItems = [
                URLQueryItem(name: "client_id", value: clientID),
                URLQueryItem(name: "redirect_uri", value: callbackURL),
                URLQueryItem(name: "response_type", value: "code"),
                URLQueryItem(name: "approval_prompt", value: "auto"),
                URLQueryItem(name: "scope", value: scope),
            ]
            return components?.url
        }
    }

    // MARK: - Gemini AI

    enum Gemini {
        static let baseURL = "https://generativelanguage.googleapis.com/v1beta"
        static let model = "gemini-2.0-flash"
        static let generateContentPath = "/models/\(model):generateContent"

        static var endpointURL: URL? {
            URL(string: "\(baseURL)\(generateContentPath)?key=\(AppConfig.geminiAPIKey)")
        }

        static let maxTokens = 4096
        static let temperature: Double = 0.7
    }

    // MARK: - Health & Fitness Constants

    enum Health {
        static let defaultRestingHeartRate: Double = 60
        static let defaultMaxHeartRate: Int = 190
        static let heartRateZoneCount = 5
        static let stepsGoalDefault = 10_000
        static let calorieGoalDefault = 500
        static let sleepGoalHoursDefault: Double = 8.0
        static let hydrationGoalMLDefault: Double = 2500
    }

    // MARK: - Workout Defaults

    enum Workout {
        static let locationUpdateInterval: TimeInterval = 1.0
        static let locationDistanceFilter: Double = 5.0
        static let heartRateSampleInterval: TimeInterval = 5.0
        static let autoLapDistanceMeters: Double = 1000.0
        static let autoPauseSpeedThreshold: Double = 0.5
        static let gpsAccuracyThreshold: Double = 20.0
        static let minimumRunDistanceMeters: Double = 100.0
        static let minimumSwimDistanceMeters: Double = 25.0
        static let minimumCycleDistanceMeters: Double = 500.0
    }

    // MARK: - Watch Sync

    enum WatchSync {
        static let heartRatePath = "/heart_rate"
        static let workoutDataPath = "/workout_data"
        static let commandPath = "/command"
        static let statusPath = "/status"
        static let syncInterval: TimeInterval = 2.0
    }

    // MARK: - Feature Flags

    enum Features {
        static let stravaIntegration = true
        static let geminiCoaching = true
        static let formAnalysis = true
        static let swimTracking = true
        static let cyclingTracking = true
        static let gymWorkouts = true
        static let nutritionTracking = true
        static let mentalHealthTracking = true
        static let socialFeatures = false
        static let smartTrainerBluetooth = true
        static let safetyFeatures = true
        static let gamification = true
        static let widgets = true
        static let liveActivities = true
    }

    // MARK: - Networking

    enum Network {
        static let requestTimeout: TimeInterval = 30.0
        static let resourceTimeout: TimeInterval = 60.0
        static let maxRetryAttempts = 3
        static let retryDelay: TimeInterval = 1.0
    }

    // MARK: - Storage Keys

    enum StorageKeys {
        static let stravaAccessToken = "strava_access_token"
        static let stravaRefreshToken = "strava_refresh_token"
        static let stravaTokenExpiry = "strava_token_expiry"
        static let stravaAthleteID = "strava_athlete_id"
        static let onboardingComplete = "onboarding_complete"
        static let selectedUnitSystem = "selected_unit_system"
        static let darkModePreference = "dark_mode_preference"
        static let notificationsEnabled = "notifications_enabled"
        static let lastSyncTimestamp = "last_sync_timestamp"
        static let userProfileData = "user_profile_data"
    }

    // MARK: - Secret Loading

    private static func loadSecret(key: String) -> String {
        // Priority 1: Environment variable (for CI/CD and development)
        if let envValue = ProcessInfo.processInfo.environment[key], !envValue.isEmpty {
            return envValue
        }

        // Priority 2: Secrets.plist file (for local development)
        if let secretsPath = Bundle.main.path(forResource: "Secrets", ofType: "plist"),
           let secrets = NSDictionary(contentsOfFile: secretsPath),
           let value = secrets[key] as? String, !value.isEmpty {
            return value
        }

        // Priority 3: UserDefaults (for runtime configuration)
        if let value = UserDefaults.standard.string(forKey: key), !value.isEmpty {
            return value
        }

        #if DEBUG
        print("[AppConfig] Warning: No value found for secret key '\(key)'. Set it via environment variable, Secrets.plist, or UserDefaults.")
        #endif

        return ""
    }
}

// MARK: - Environment Detection

extension AppConfig {

    enum Environment: String {
        case debug
        case testing
        case release
    }

    static var currentEnvironment: Environment {
        #if DEBUG
        if ProcessInfo.processInfo.environment["GOSTEADY_ENV"] == "testing" {
            return .testing
        }
        return .debug
        #else
        return .release
        #endif
    }

    static var isDebug: Bool {
        currentEnvironment == .debug
    }

    static var isTesting: Bool {
        currentEnvironment == .testing
    }

    static var isRelease: Bool {
        currentEnvironment == .release
    }
}
