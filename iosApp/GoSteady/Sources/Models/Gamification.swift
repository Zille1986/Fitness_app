import Foundation
import SwiftData

@Model
final class UserGamification {
    @Attribute(.unique) var id: Int
    var totalXp: Int64
    var currentLevel: Int
    var currentStreak: Int
    var longestStreak: Int
    var lastActivityDate: Date?
    var streakProtectionAvailable: Bool
    var totalWorkoutsCompleted: Int
    var totalDistanceMeters: Double
    var totalDurationMinutes: Int
    var weeklyWorkouts: Int
    var weekStartDate: Date
    var dailyGoalMinutes: Int
    var dailyGoalCompleted: Bool
    var weeklyGoalDays: Int
    var weeklyGoalProgress: Int

    var levelProgress: Float {
        let xpForCurrent = UserGamification.xpRequiredForLevel(currentLevel)
        let xpForNext = UserGamification.xpRequiredForLevel(currentLevel + 1)
        let xpInCurrent = totalXp - xpForCurrent
        let xpNeeded = xpForNext - xpForCurrent
        guard xpNeeded > 0 else { return 0 }
        return min(max(Float(xpInCurrent) / Float(xpNeeded), 0), 1)
    }

    var xpToNextLevel: Int64 {
        UserGamification.xpRequiredForLevel(currentLevel + 1) - totalXp
    }

    static func xpRequiredForLevel(_ level: Int) -> Int64 {
        guard level > 1 else { return 0 }
        return Int64(100.0 * Double(level * level) * 0.5)
    }

    static func levelForXp(_ xp: Int64) -> Int {
        var level = 1
        while xpRequiredForLevel(level + 1) <= xp {
            level += 1
        }
        return level
    }

    init(
        id: Int = 1,
        totalXp: Int64 = 0,
        currentLevel: Int = 1,
        currentStreak: Int = 0,
        longestStreak: Int = 0,
        lastActivityDate: Date? = nil,
        streakProtectionAvailable: Bool = true,
        totalWorkoutsCompleted: Int = 0,
        totalDistanceMeters: Double = 0.0,
        totalDurationMinutes: Int = 0,
        weeklyWorkouts: Int = 0,
        weekStartDate: Date = Date(),
        dailyGoalMinutes: Int = 30,
        dailyGoalCompleted: Bool = false,
        weeklyGoalDays: Int = 4,
        weeklyGoalProgress: Int = 0
    ) {
        self.id = id
        self.totalXp = totalXp
        self.currentLevel = currentLevel
        self.currentStreak = currentStreak
        self.longestStreak = longestStreak
        self.lastActivityDate = lastActivityDate
        self.streakProtectionAvailable = streakProtectionAvailable
        self.totalWorkoutsCompleted = totalWorkoutsCompleted
        self.totalDistanceMeters = totalDistanceMeters
        self.totalDurationMinutes = totalDurationMinutes
        self.weeklyWorkouts = weeklyWorkouts
        self.weekStartDate = weekStartDate
        self.dailyGoalMinutes = dailyGoalMinutes
        self.dailyGoalCompleted = dailyGoalCompleted
        self.weeklyGoalDays = weeklyGoalDays
        self.weeklyGoalProgress = weeklyGoalProgress
    }
}

@Model
final class Achievement {
    @Attribute(.unique) var id: String
    var name: String
    var achievementDescription: String
    var iconName: String
    var category: AchievementCategory
    var requirement: Int
    var xpReward: Int
    var isSecret: Bool

    init(
        id: String,
        name: String,
        achievementDescription: String,
        iconName: String,
        category: AchievementCategory,
        requirement: Int,
        xpReward: Int = 50,
        isSecret: Bool = false
    ) {
        self.id = id
        self.name = name
        self.achievementDescription = achievementDescription
        self.iconName = iconName
        self.category = category
        self.requirement = requirement
        self.xpReward = xpReward
        self.isSecret = isSecret
    }
}

@Model
final class UserAchievement {
    @Attribute(.unique) var id: UUID
    var achievementId: String
    var unlockedAt: Date
    var progress: Int
    var isUnlocked: Bool
    var notificationShown: Bool

    init(
        id: UUID = UUID(),
        achievementId: String,
        unlockedAt: Date = Date(),
        progress: Int = 0,
        isUnlocked: Bool = false,
        notificationShown: Bool = false
    ) {
        self.id = id
        self.achievementId = achievementId
        self.unlockedAt = unlockedAt
        self.progress = progress
        self.isUnlocked = isUnlocked
        self.notificationShown = notificationShown
    }
}

enum AchievementCategory: String, Codable, CaseIterable, Identifiable {
    case distance = "DISTANCE"
    case workouts = "WORKOUTS"
    case streaks = "STREAKS"
    case speed = "SPEED"
    case consistency = "CONSISTENCY"
    case social = "SOCIAL"
    case exploration = "EXPLORATION"
    case personalBest = "PERSONAL_BEST"
    case special = "SPECIAL"

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .distance: return "Distance"
        case .workouts: return "Workouts"
        case .streaks: return "Streaks"
        case .speed: return "Speed"
        case .consistency: return "Consistency"
        case .social: return "Social"
        case .exploration: return "Exploration"
        case .personalBest: return "Personal Best"
        case .special: return "Special"
        }
    }
}

@Model
final class DailyRings {
    @Attribute(.unique) var date: Date
    var moveMinutes: Int
    var moveGoal: Int
    var exerciseMinutes: Int
    var exerciseGoal: Int
    var standHours: Int
    var standGoal: Int
    var caloriesBurned: Int
    var caloriesGoal: Int
    var distanceMeters: Double
    var distanceGoal: Double
    var allRingsClosed: Bool

    var moveProgress: Float { min(Float(moveMinutes) / Float(moveGoal), 1.0) }
    var exerciseProgress: Float { min(Float(exerciseMinutes) / Float(exerciseGoal), 1.0) }
    var standProgress: Float { min(Float(standHours) / Float(standGoal), 1.0) }
    var caloriesProgress: Float { min(Float(caloriesBurned) / Float(caloriesGoal), 1.0) }
    var distanceProgress: Float { min(Float(distanceMeters / distanceGoal), 1.0) }

    init(
        date: Date,
        moveMinutes: Int = 0,
        moveGoal: Int = 30,
        exerciseMinutes: Int = 0,
        exerciseGoal: Int = 30,
        standHours: Int = 0,
        standGoal: Int = 12,
        caloriesBurned: Int = 0,
        caloriesGoal: Int = 500,
        distanceMeters: Double = 0.0,
        distanceGoal: Double = 5000.0,
        allRingsClosed: Bool = false
    ) {
        self.date = date
        self.moveMinutes = moveMinutes
        self.moveGoal = moveGoal
        self.exerciseMinutes = exerciseMinutes
        self.exerciseGoal = exerciseGoal
        self.standHours = standHours
        self.standGoal = standGoal
        self.caloriesBurned = caloriesBurned
        self.caloriesGoal = caloriesGoal
        self.distanceMeters = distanceMeters
        self.distanceGoal = distanceGoal
        self.allRingsClosed = allRingsClosed
    }
}

@Model
final class XpTransaction {
    @Attribute(.unique) var id: UUID
    var amount: Int
    var reason: XpReason
    var relatedId: UUID?
    var timestamp: Date

    init(
        id: UUID = UUID(),
        amount: Int,
        reason: XpReason,
        relatedId: UUID? = nil,
        timestamp: Date = Date()
    ) {
        self.id = id
        self.amount = amount
        self.reason = reason
        self.relatedId = relatedId
        self.timestamp = timestamp
    }
}

enum XpReason: String, Codable, CaseIterable {
    case workoutCompleted = "WORKOUT_COMPLETED"
    case streakBonus = "STREAK_BONUS"
    case achievementUnlocked = "ACHIEVEMENT_UNLOCKED"
    case dailyGoalCompleted = "DAILY_GOAL_COMPLETED"
    case weeklyGoalCompleted = "WEEKLY_GOAL_COMPLETED"
    case personalBest = "PERSONAL_BEST"
    case challengeCompleted = "CHALLENGE_COMPLETED"
    case firstWorkout = "FIRST_WORKOUT"
    case levelUpBonus = "LEVEL_UP_BONUS"

    var displayName: String {
        switch self {
        case .workoutCompleted: return "Workout Completed"
        case .streakBonus: return "Streak Bonus"
        case .achievementUnlocked: return "Achievement Unlocked"
        case .dailyGoalCompleted: return "Daily Goal"
        case .weeklyGoalCompleted: return "Weekly Goal"
        case .personalBest: return "Personal Best"
        case .challengeCompleted: return "Challenge Completed"
        case .firstWorkout: return "First Workout"
        case .levelUpBonus: return "Level Up Bonus"
        }
    }
}

enum XpRewards {
    static let workoutBase = 50
    static let perKilometer = 10
    static let perMinute = 2
    static let streakBonusPerDay = 5
    static let dailyGoalBonus = 25
    static let weeklyGoalBonus = 100
    static let personalBest = 75
    static let firstWorkout = 100

    static func calculateWorkoutXp(distanceMeters: Double, durationMinutes: Int, currentStreak: Int) -> Int {
        let distanceXp = Int(distanceMeters / 1000 * Double(perKilometer))
        let durationXp = durationMinutes * perMinute
        let streakBonus = currentStreak * streakBonusPerDay
        return workoutBase + distanceXp + durationXp + streakBonus
    }
}

enum DefaultAchievements {
    static let all: [Achievement] = distance + workouts + streaks + speed + consistency + personalBests

    static let distance: [Achievement] = [
        Achievement(id: "dist_1k", name: "First Kilometer", achievementDescription: "Run your first kilometer", iconName: "figure.run", category: .distance, requirement: 1000, xpReward: 25),
        Achievement(id: "dist_5k", name: "5K Club", achievementDescription: "Run a total of 5 kilometers", iconName: "figure.run", category: .distance, requirement: 5000, xpReward: 50),
        Achievement(id: "dist_10k", name: "Double Digits", achievementDescription: "Run a total of 10 kilometers", iconName: "figure.run", category: .distance, requirement: 10000, xpReward: 75),
        Achievement(id: "dist_42k", name: "Marathon Distance", achievementDescription: "Run a total of 42.195 kilometers", iconName: "trophy", category: .distance, requirement: 42195, xpReward: 150),
        Achievement(id: "dist_100k", name: "Century Club", achievementDescription: "Run a total of 100 kilometers", iconName: "medal", category: .distance, requirement: 100000, xpReward: 200),
        Achievement(id: "dist_500k", name: "Road Warrior", achievementDescription: "Run a total of 500 kilometers", iconName: "medal", category: .distance, requirement: 500000, xpReward: 500),
        Achievement(id: "dist_1000k", name: "Thousand K Legend", achievementDescription: "Run a total of 1000 kilometers", iconName: "star.circle.fill", category: .distance, requirement: 1000000, xpReward: 1000),
    ]

    static let workouts: [Achievement] = [
        Achievement(id: "workout_1", name: "First Steps", achievementDescription: "Complete your first workout", iconName: "dumbbell", category: .workouts, requirement: 1, xpReward: 50),
        Achievement(id: "workout_10", name: "Getting Started", achievementDescription: "Complete 10 workouts", iconName: "dumbbell", category: .workouts, requirement: 10, xpReward: 100),
        Achievement(id: "workout_25", name: "Quarter Century", achievementDescription: "Complete 25 workouts", iconName: "dumbbell", category: .workouts, requirement: 25, xpReward: 150),
        Achievement(id: "workout_50", name: "Halfway Hero", achievementDescription: "Complete 50 workouts", iconName: "dumbbell", category: .workouts, requirement: 50, xpReward: 250),
        Achievement(id: "workout_100", name: "Century Runner", achievementDescription: "Complete 100 workouts", iconName: "trophy", category: .workouts, requirement: 100, xpReward: 500),
        Achievement(id: "workout_250", name: "Dedicated Athlete", achievementDescription: "Complete 250 workouts", iconName: "medal", category: .workouts, requirement: 250, xpReward: 750),
        Achievement(id: "workout_500", name: "Elite Runner", achievementDescription: "Complete 500 workouts", iconName: "star.circle.fill", category: .workouts, requirement: 500, xpReward: 1000),
    ]

    static let streaks: [Achievement] = [
        Achievement(id: "streak_3", name: "Hat Trick", achievementDescription: "Maintain a 3-day streak", iconName: "flame", category: .streaks, requirement: 3, xpReward: 30),
        Achievement(id: "streak_7", name: "Week Warrior", achievementDescription: "Maintain a 7-day streak", iconName: "flame", category: .streaks, requirement: 7, xpReward: 75),
        Achievement(id: "streak_14", name: "Fortnight Fighter", achievementDescription: "Maintain a 14-day streak", iconName: "flame", category: .streaks, requirement: 14, xpReward: 150),
        Achievement(id: "streak_30", name: "Monthly Master", achievementDescription: "Maintain a 30-day streak", iconName: "flame.fill", category: .streaks, requirement: 30, xpReward: 300),
        Achievement(id: "streak_100", name: "Century Streak", achievementDescription: "Maintain a 100-day streak", iconName: "star.circle.fill", category: .streaks, requirement: 100, xpReward: 1000),
        Achievement(id: "streak_365", name: "Year of Dedication", achievementDescription: "Maintain a 365-day streak", iconName: "star.circle.fill", category: .streaks, requirement: 365, xpReward: 5000, isSecret: true),
    ]

    static let speed: [Achievement] = [
        Achievement(id: "pace_6min", name: "Sub-6 Pace", achievementDescription: "Run a kilometer under 6:00 pace", iconName: "hare", category: .speed, requirement: 360, xpReward: 100),
        Achievement(id: "pace_5min", name: "Sub-5 Pace", achievementDescription: "Run a kilometer under 5:00 pace", iconName: "hare", category: .speed, requirement: 300, xpReward: 200),
        Achievement(id: "pace_4min", name: "Sub-4 Pace", achievementDescription: "Run a kilometer under 4:00 pace", iconName: "hare", category: .speed, requirement: 240, xpReward: 500, isSecret: true),
    ]

    static let consistency: [Achievement] = [
        Achievement(id: "weekly_3", name: "Tri-Weekly", achievementDescription: "Work out 3 times in a week", iconName: "calendar", category: .consistency, requirement: 3, xpReward: 50),
        Achievement(id: "weekly_5", name: "Five-a-Week", achievementDescription: "Work out 5 times in a week", iconName: "calendar", category: .consistency, requirement: 5, xpReward: 100),
        Achievement(id: "weekly_7", name: "Perfect Week", achievementDescription: "Work out every day for a week", iconName: "calendar", category: .consistency, requirement: 7, xpReward: 200),
        Achievement(id: "monthly_20", name: "Monthly Dedication", achievementDescription: "Complete 20 workouts in a month", iconName: "calendar.badge.checkmark", category: .consistency, requirement: 20, xpReward: 300),
    ]

    static let personalBests: [Achievement] = [
        Achievement(id: "pb_first", name: "Personal Best!", achievementDescription: "Set your first personal best", iconName: "trophy", category: .personalBest, requirement: 1, xpReward: 100),
        Achievement(id: "pb_5", name: "Record Breaker", achievementDescription: "Set 5 personal bests", iconName: "trophy", category: .personalBest, requirement: 5, xpReward: 200),
        Achievement(id: "pb_10", name: "PB Machine", achievementDescription: "Set 10 personal bests", iconName: "trophy", category: .personalBest, requirement: 10, xpReward: 400),
    ]
}
