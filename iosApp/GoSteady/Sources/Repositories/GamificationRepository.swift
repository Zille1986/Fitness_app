import Foundation
import SwiftData

final class GamificationRepository {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    // MARK: - User Gamification

    func fetchUserGamification() -> UserGamification? {
        let descriptor = FetchDescriptor<UserGamification>()
        return try? context.fetch(descriptor).first
    }

    func getOrCreateUserGamification() -> UserGamification {
        if let existing = fetchUserGamification() { return existing }
        let gamification = UserGamification()
        context.insert(gamification)
        try? context.save()
        return gamification
    }

    // MARK: - XP

    func addXp(amount: Int, reason: XpReason, relatedId: UUID? = nil) {
        let current = getOrCreateUserGamification()
        current.totalXp += Int64(amount)
        let newLevel = UserGamification.levelForXp(current.totalXp)

        let leveledUp = newLevel > current.currentLevel
        current.currentLevel = newLevel
        try? context.save()

        let transaction = XpTransaction(amount: amount, reason: reason, relatedId: relatedId)
        context.insert(transaction)
        try? context.save()

        if leveledUp {
            let bonus = newLevel * 25
            current.totalXp += Int64(bonus)
            current.currentLevel = UserGamification.levelForXp(current.totalXp)
            let bonusTx = XpTransaction(amount: bonus, reason: .levelUpBonus)
            context.insert(bonusTx)
            try? context.save()
        }
    }

    // MARK: - Workout Recording

    func recordWorkoutCompleted(distanceMeters: Double, durationMinutes: Int, workoutId: UUID) {
        let gamification = getOrCreateUserGamification()

        // Update streak
        let today = Date().startOfDay
        let yesterday = Calendar.current.date(byAdding: .day, value: -1, to: today)!.startOfDay
        let lastActivity = gamification.lastActivityDate?.startOfDay

        let newStreak: Int
        if lastActivity == today {
            newStreak = gamification.currentStreak
        } else if lastActivity == yesterday {
            newStreak = gamification.currentStreak + 1
        } else {
            newStreak = 1
        }

        gamification.currentStreak = newStreak
        gamification.longestStreak = max(newStreak, gamification.longestStreak)
        gamification.lastActivityDate = Date()
        gamification.totalWorkoutsCompleted += 1
        gamification.totalDistanceMeters += distanceMeters
        gamification.totalDurationMinutes += durationMinutes
        try? context.save()

        let xp = XpRewards.calculateWorkoutXp(
            distanceMeters: distanceMeters,
            durationMinutes: durationMinutes,
            currentStreak: newStreak
        )
        addXp(amount: xp, reason: .workoutCompleted, relatedId: workoutId)

        updateDailyRings(exerciseMinutes: durationMinutes, distanceMeters: distanceMeters)
        checkAchievements()
    }

    // MARK: - Daily Rings

    func getTodayRings() -> DailyRings {
        let today = Date().startOfDay
        let descriptor = FetchDescriptor<DailyRings>(predicate: #Predicate { $0.date == today })
        if let existing = try? context.fetch(descriptor).first {
            return existing
        }
        let rings = DailyRings(date: today)
        context.insert(rings)
        try? context.save()
        return rings
    }

    func fetchDailyRings(date: Date) -> DailyRings? {
        let dayStart = date.startOfDay
        let descriptor = FetchDescriptor<DailyRings>(predicate: #Predicate { $0.date == dayStart })
        return try? context.fetch(descriptor).first
    }

    func fetchRecentDailyRings(days: Int = 7) -> [DailyRings] {
        let cutoff = Calendar.current.date(byAdding: .day, value: -days, to: Date())?.startOfDay ?? Date()
        let descriptor = FetchDescriptor<DailyRings>(
            predicate: #Predicate { $0.date >= cutoff },
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func updateDailyRings(exerciseMinutes: Int, distanceMeters: Double) {
        let rings = getTodayRings()
        rings.moveMinutes += exerciseMinutes
        rings.exerciseMinutes += exerciseMinutes
        rings.distanceMeters += distanceMeters

        let allClosed = rings.moveProgress >= 1.0 && rings.exerciseProgress >= 1.0
        if allClosed && !rings.allRingsClosed {
            rings.allRingsClosed = true
            addXp(amount: XpRewards.dailyGoalBonus, reason: .dailyGoalCompleted)
        }
        try? context.save()
    }

    // MARK: - Achievements

    func fetchAllAchievements() -> [Achievement] {
        let descriptor = FetchDescriptor<Achievement>(sortBy: [SortDescriptor(\.name)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchUserAchievements() -> [UserAchievement] {
        let descriptor = FetchDescriptor<UserAchievement>(sortBy: [SortDescriptor(\.unlockedAt, order: .reverse)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchUnlockedAchievements() -> [UserAchievement] {
        let descriptor = FetchDescriptor<UserAchievement>(
            predicate: #Predicate { $0.isUnlocked == true },
            sortBy: [SortDescriptor(\.unlockedAt, order: .reverse)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchUnshownNotifications() -> [UserAchievement] {
        let descriptor = FetchDescriptor<UserAchievement>(
            predicate: #Predicate { $0.isUnlocked == true && $0.notificationShown == false }
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func markNotificationShown(achievementId: String) {
        let descriptor = FetchDescriptor<UserAchievement>(
            predicate: #Predicate { $0.achievementId == achievementId }
        )
        if let ua = try? context.fetch(descriptor).first {
            ua.notificationShown = true
            try? context.save()
        }
    }

    func checkAchievements() {
        let gamification = getOrCreateUserGamification()
        let achievements = fetchAllAchievements()

        for achievement in achievements {
            let achievementId = achievement.id
            let descriptor = FetchDescriptor<UserAchievement>(
                predicate: #Predicate { $0.achievementId == achievementId }
            )
            let userAchievement = try? context.fetch(descriptor).first

            if userAchievement?.isUnlocked == true { continue }

            let currentProgress: Int
            switch achievement.category {
            case .distance:    currentProgress = Int(gamification.totalDistanceMeters / 1000)
            case .workouts:    currentProgress = gamification.totalWorkoutsCompleted
            case .streaks:     currentProgress = gamification.longestStreak
            case .consistency: currentProgress = gamification.weeklyWorkouts
            default:           currentProgress = userAchievement?.progress ?? 0
            }

            let isUnlocked = currentProgress >= achievement.requirement

            if let ua = userAchievement {
                if currentProgress != ua.progress || isUnlocked != ua.isUnlocked {
                    ua.progress = currentProgress
                    if isUnlocked && !ua.isUnlocked {
                        ua.isUnlocked = true
                        ua.unlockedAt = Date()
                        addXp(amount: achievement.xpReward, reason: .achievementUnlocked)
                    }
                }
            } else {
                let newUA = UserAchievement(
                    achievementId: achievement.id,
                    unlockedAt: isUnlocked ? Date() : Date.distantPast,
                    progress: currentProgress,
                    isUnlocked: isUnlocked
                )
                context.insert(newUA)
                if isUnlocked {
                    addXp(amount: achievement.xpReward, reason: .achievementUnlocked)
                }
            }
        }
        try? context.save()
    }

    func recordPersonalBest(workoutId: UUID) {
        addXp(amount: XpRewards.personalBest, reason: .personalBest, relatedId: workoutId)
    }

    // MARK: - XP Transactions

    func fetchRecentXpTransactions(limit: Int = 20) -> [XpTransaction] {
        var descriptor = FetchDescriptor<XpTransaction>(sortBy: [SortDescriptor(\.timestamp, order: .reverse)])
        descriptor.fetchLimit = limit
        return (try? context.fetch(descriptor)) ?? []
    }

    // MARK: - Seed

    func seedDefaultAchievements() {
        let existing = fetchAllAchievements()
        guard existing.isEmpty else { return }
        for a in DefaultAchievements.all {
            context.insert(a)
        }
        try? context.save()
    }
}
