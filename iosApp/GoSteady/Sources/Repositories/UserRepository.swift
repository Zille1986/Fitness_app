import Foundation
import SwiftData

final class UserRepository {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    // MARK: - Profile

    func fetchProfile() -> UserProfile? {
        let descriptor = FetchDescriptor<UserProfile>()
        return try? context.fetch(descriptor).first
    }

    func saveProfile(_ profile: UserProfile) {
        profile.updatedAt = Date()
        if fetchProfile() == nil {
            context.insert(profile)
        }
        try? context.save()
    }

    func ensureProfileExists() {
        guard fetchProfile() == nil else { return }
        let profile = UserProfile()
        context.insert(profile)
        try? context.save()
    }

    // MARK: - Strava Connection

    func updateStravaTokens(
        accessToken: String?,
        refreshToken: String?,
        expiry: Date?,
        athleteId: String?
    ) {
        guard let profile = fetchProfile() else { return }
        profile.stravaAccessToken = accessToken
        profile.stravaRefreshToken = refreshToken
        profile.stravaTokenExpiry = expiry
        profile.stravaAthleteId = athleteId
        profile.updatedAt = Date()
        try? context.save()
    }

    func clearStravaConnection() {
        updateStravaTokens(accessToken: nil, refreshToken: nil, expiry: nil, athleteId: nil)
    }

    // MARK: - Settings

    func updatePreferredUnits(_ units: Units) {
        guard let profile = fetchProfile() else { return }
        profile.preferredUnits = units
        profile.updatedAt = Date()
        try? context.save()
    }

    func updateWeeklyGoal(_ goalKm: Double) {
        guard let profile = fetchProfile() else { return }
        profile.weeklyGoalKm = goalKm
        profile.updatedAt = Date()
        try? context.save()
    }

    func markOnboardingComplete() {
        guard let profile = fetchProfile() else { return }
        profile.isOnboardingComplete = true
        profile.updatedAt = Date()
        try? context.save()
    }

    // MARK: - Heart Rate Zones

    func heartRateZones() -> [HeartRateZoneRange] {
        guard let profile = fetchProfile() else { return [] }
        let maxHR = profile.estimatedMaxHeartRate
        return [
            HeartRateZoneRange(zone: .zone1, minBpm: Int(Double(maxHR) * 0.50), maxBpm: Int(Double(maxHR) * 0.60)),
            HeartRateZoneRange(zone: .zone2, minBpm: Int(Double(maxHR) * 0.60), maxBpm: Int(Double(maxHR) * 0.70)),
            HeartRateZoneRange(zone: .zone3, minBpm: Int(Double(maxHR) * 0.70), maxBpm: Int(Double(maxHR) * 0.80)),
            HeartRateZoneRange(zone: .zone4, minBpm: Int(Double(maxHR) * 0.80), maxBpm: Int(Double(maxHR) * 0.90)),
            HeartRateZoneRange(zone: .zone5, minBpm: Int(Double(maxHR) * 0.90), maxBpm: maxHR),
        ]
    }
}
