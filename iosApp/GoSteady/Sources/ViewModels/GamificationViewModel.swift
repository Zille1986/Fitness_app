import Foundation
import Observation
import SwiftData

// MARK: - ViewModel-specific Types

struct AchievementWithProgress: Identifiable {
    let achievement: Achievement
    var progress: Int
    var isUnlocked: Bool
    var unlockedAt: Date?

    var id: String { achievement.id }
    var progressPercent: Float {
        guard achievement.requirement > 0 else { return 0 }
        return min(max(Float(progress) / Float(achievement.requirement), 0), 1)
    }
}

// MARK: - ViewModel

@Observable
final class GamificationViewModel {
    private var gamificationRepository: GamificationRepository?

    var gamification: UserGamification?
    var todayRings: DailyRings?
    var weeklyRings: [DailyRings] = []
    var achievements: [AchievementWithProgress] = []
    var recentXp: [XpTransaction] = []
    var newAchievements: [Achievement] = []
    var isLoading = false

    var unlockedAchievements: [AchievementWithProgress] {
        achievements.filter(\.isUnlocked)
    }

    var lockedAchievements: [AchievementWithProgress] {
        achievements.filter { !$0.isUnlocked && !$0.achievement.isSecret }
    }

    var achievementsByCategory: [AchievementCategory: [AchievementWithProgress]] {
        Dictionary(grouping: achievements, by: { $0.achievement.category })
    }

    init() {}

    func configure(gamificationRepository: GamificationRepository) {
        self.gamificationRepository = gamificationRepository
        loadData()
    }

    func loadData() {
        guard let gamificationRepository else { return }
        isLoading = true
        gamification = gamificationRepository.fetchUserGamification()
        todayRings = gamificationRepository.getTodayRings()
        weeklyRings = gamificationRepository.fetchRecentDailyRings(days: 7)

        let allAchievements = DefaultAchievements.all
        let userAchievements = gamificationRepository.fetchUserAchievements()

        achievements = allAchievements.map { achievement in
            let userProgress = userAchievements.first(where: { $0.achievementId == achievement.id })
            return AchievementWithProgress(
                achievement: achievement,
                progress: userProgress?.progress ?? 0,
                isUnlocked: userProgress?.isUnlocked ?? false,
                unlockedAt: userProgress?.unlockedAt
            )
        }

        recentXp = gamificationRepository.fetchRecentXpTransactions(limit: 20)

        // Map unshown UserAchievements back to Achievement definitions
        let unshown = gamificationRepository.fetchUnshownNotifications()
        newAchievements = unshown.compactMap { ua in
            allAchievements.first(where: { $0.id == ua.achievementId })
        }
        isLoading = false
    }

    func dismissAchievementNotification(_ achievementId: String) {
        gamificationRepository?.markNotificationShown(achievementId: achievementId)
        newAchievements.removeAll { $0.id == achievementId }
    }

    func refreshData() {
        guard let gamificationRepository else { return }
        todayRings = gamificationRepository.getTodayRings()
    }
}

