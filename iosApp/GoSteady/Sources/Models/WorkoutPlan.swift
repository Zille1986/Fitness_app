import Foundation
import SwiftData

@Model
final class WorkoutPlan {
    @Attribute(.unique) var id: UUID
    var name: String
    var durationMinutes: Int
    var scheduledWorkouts: [WeeklyWorkoutDay]
    var createdAt: Date
    var isActive: Bool

    init(
        id: UUID = UUID(),
        name: String = "My Workout Plan",
        durationMinutes: Int = 45,
        scheduledWorkouts: [WeeklyWorkoutDay] = [],
        createdAt: Date = Date(),
        isActive: Bool = true
    ) {
        self.id = id
        self.name = name
        self.durationMinutes = durationMinutes
        self.scheduledWorkouts = scheduledWorkouts
        self.createdAt = createdAt
        self.isActive = isActive
    }
}

struct WeeklyWorkoutDay: Codable, Hashable {
    var dayOfWeek: Int
    var workoutType: WeeklyWorkoutType
    var templateId: UUID?
    var templateName: String?

    init(
        dayOfWeek: Int,
        workoutType: WeeklyWorkoutType,
        templateId: UUID? = nil,
        templateName: String? = nil
    ) {
        self.dayOfWeek = dayOfWeek
        self.workoutType = workoutType
        self.templateId = templateId
        self.templateName = templateName
    }
}

enum WeeklyWorkoutType: String, Codable, CaseIterable {
    case gym = "GYM"
    case running = "RUNNING"
    case rest = "REST"

    var displayName: String {
        switch self {
        case .gym: return "Gym"
        case .running: return "Run"
        case .rest: return "Rest"
        }
    }
}

@Model
final class PersonalizedPlan {
    @Attribute(.unique) var id: UUID
    var bodyScanId: UUID
    var name: String
    var planDescription: String
    var fitnessGoal: FitnessGoal
    var startDate: Date
    var endDate: Date
    var userPreferences: PlanPreferences
    var weeklySchedule: [PlannedDay]
    var allWorkouts: [PlannedWorkout]
    var isActive: Bool
    var createdAt: Date
    var completedWorkouts: Int
    var totalWorkouts: Int

    var progressPercent: Float {
        totalWorkouts > 0 ? (Float(completedWorkouts) / Float(totalWorkouts) * 100) : 0
    }

    var isExpired: Bool { Date() > endDate }

    var daysRemaining: Int {
        max(Int(endDate.timeIntervalSince(Date()) / 86400), 0)
    }

    init(
        id: UUID = UUID(),
        bodyScanId: UUID,
        name: String,
        planDescription: String,
        fitnessGoal: FitnessGoal,
        startDate: Date,
        endDate: Date,
        userPreferences: PlanPreferences,
        weeklySchedule: [PlannedDay] = [],
        allWorkouts: [PlannedWorkout] = [],
        isActive: Bool = true,
        createdAt: Date = Date(),
        completedWorkouts: Int = 0,
        totalWorkouts: Int = 0
    ) {
        self.id = id
        self.bodyScanId = bodyScanId
        self.name = name
        self.planDescription = planDescription
        self.fitnessGoal = fitnessGoal
        self.startDate = startDate
        self.endDate = endDate
        self.userPreferences = userPreferences
        self.weeklySchedule = weeklySchedule
        self.allWorkouts = allWorkouts
        self.isActive = isActive
        self.createdAt = createdAt
        self.completedWorkouts = completedWorkouts
        self.totalWorkouts = totalWorkouts
    }
}

struct PlanPreferences: Codable, Hashable {
    var workoutDaysPerWeek: Int
    var preferredDays: [Int]
    var availableTimePerDay: [Int: Int]
    var includeRunning: Bool
    var includeGym: Bool
    var runningToGymRatio: Float
    var preferMorningWorkouts: Bool
    var includeRestDays: Bool
    var fitnessLevel: FitnessLevel
    var runningDays: [Int]
    var gymDays: [Int]

    init(
        workoutDaysPerWeek: Int,
        preferredDays: [Int],
        availableTimePerDay: [Int: Int],
        includeRunning: Bool = true,
        includeGym: Bool = true,
        runningToGymRatio: Float = 0.5,
        preferMorningWorkouts: Bool = true,
        includeRestDays: Bool = true,
        fitnessLevel: FitnessLevel = .intermediate,
        runningDays: [Int] = [],
        gymDays: [Int] = []
    ) {
        self.workoutDaysPerWeek = workoutDaysPerWeek
        self.preferredDays = preferredDays
        self.availableTimePerDay = availableTimePerDay
        self.includeRunning = includeRunning
        self.includeGym = includeGym
        self.runningToGymRatio = runningToGymRatio
        self.preferMorningWorkouts = preferMorningWorkouts
        self.includeRestDays = includeRestDays
        self.fitnessLevel = fitnessLevel
        self.runningDays = runningDays
        self.gymDays = gymDays
    }
}

struct PlannedDay: Codable, Hashable {
    var dayOfWeek: Int
    var dayName: String
    var workoutType: PlannedWorkoutType
    var focus: String
    var estimatedDuration: Int
    var isRestDay: Bool

    init(
        dayOfWeek: Int,
        dayName: String,
        workoutType: PlannedWorkoutType,
        focus: String,
        estimatedDuration: Int,
        isRestDay: Bool = false
    ) {
        self.dayOfWeek = dayOfWeek
        self.dayName = dayName
        self.workoutType = workoutType
        self.focus = focus
        self.estimatedDuration = estimatedDuration
        self.isRestDay = isRestDay
    }
}

enum PlannedWorkoutType: String, Codable, CaseIterable {
    case running = "RUNNING"
    case gymStrength = "GYM_STRENGTH"
    case gymHypertrophy = "GYM_HYPERTROPHY"
    case hiit = "HIIT"
    case cardio = "CARDIO"
    case flexibility = "FLEXIBILITY"
    case activeRecovery = "ACTIVE_RECOVERY"
    case rest = "REST"
}

struct PlannedWorkout: Codable, Hashable, Identifiable {
    var id: String
    var date: Date
    var dayOfWeek: Int
    var weekNumber: Int
    var workoutType: PlannedWorkoutType
    var title: String
    var workoutDescription: String
    var estimatedDuration: Int
    var exercises: [PlannedExercise]
    var targetZones: [BodyZone]
    var isCompleted: Bool
    var completedAt: Date?
    var linkedRunId: UUID?
    var linkedGymWorkoutId: UUID?

    init(
        id: String = UUID().uuidString,
        date: Date,
        dayOfWeek: Int,
        weekNumber: Int,
        workoutType: PlannedWorkoutType,
        title: String,
        workoutDescription: String,
        estimatedDuration: Int,
        exercises: [PlannedExercise] = [],
        targetZones: [BodyZone] = [],
        isCompleted: Bool = false,
        completedAt: Date? = nil,
        linkedRunId: UUID? = nil,
        linkedGymWorkoutId: UUID? = nil
    ) {
        self.id = id
        self.date = date
        self.dayOfWeek = dayOfWeek
        self.weekNumber = weekNumber
        self.workoutType = workoutType
        self.title = title
        self.workoutDescription = workoutDescription
        self.estimatedDuration = estimatedDuration
        self.exercises = exercises
        self.targetZones = targetZones
        self.isCompleted = isCompleted
        self.completedAt = completedAt
        self.linkedRunId = linkedRunId
        self.linkedGymWorkoutId = linkedGymWorkoutId
    }
}

struct PlannedExercise: Codable, Hashable, Identifiable {
    var id: String
    var name: String
    var exerciseType: PlannedExerciseType
    var sets: Int?
    var reps: String?
    var duration: Int?
    var distance: Double?
    var targetPace: String?
    var restSeconds: Int?
    var notes: String?
    var targetZone: BodyZone?

    init(
        id: String = UUID().uuidString,
        name: String,
        exerciseType: PlannedExerciseType,
        sets: Int? = nil,
        reps: String? = nil,
        duration: Int? = nil,
        distance: Double? = nil,
        targetPace: String? = nil,
        restSeconds: Int? = nil,
        notes: String? = nil,
        targetZone: BodyZone? = nil
    ) {
        self.id = id
        self.name = name
        self.exerciseType = exerciseType
        self.sets = sets
        self.reps = reps
        self.duration = duration
        self.distance = distance
        self.targetPace = targetPace
        self.restSeconds = restSeconds
        self.notes = notes
        self.targetZone = targetZone
    }
}

enum PlannedExerciseType: String, Codable, CaseIterable {
    case compoundStrength = "COMPOUND_STRENGTH"
    case isolationStrength = "ISOLATION_STRENGTH"
    case bodyweight = "BODYWEIGHT"
    case machine = "MACHINE"
    case cable = "CABLE"
    case freeWeight = "FREE_WEIGHT"
    case running = "RUNNING"
    case intervalRun = "INTERVAL_RUN"
    case tempoRun = "TEMPO_RUN"
    case easyRun = "EASY_RUN"
    case longRun = "LONG_RUN"
    case hiitCardio = "HIIT_CARDIO"
    case stretch = "STRETCH"
    case mobility = "MOBILITY"
    case yoga = "YOGA"
    case warmup = "WARMUP"
    case cooldown = "COOLDOWN"
}

struct PlanRecommendation: Codable {
    var recommendedDaysPerWeek: Int
    var recommendedSplit: TrainingSplit
    var runningDays: Int
    var gymDays: Int
    var focusAreas: [BodyZone]
    var estimatedTimePerSession: Int
    var rationale: String
}

struct PlanProgress: Codable {
    var planId: UUID
    var currentWeek: Int
    var completedWorkouts: Int
    var totalWorkouts: Int
    var adherencePercent: Float
    var streakDays: Int
    var missedWorkouts: Int
    var upcomingWorkouts: [PlannedWorkout]
    var recentCompletedWorkouts: [PlannedWorkout]
}

@Model
final class ScheduledGymWorkout {
    @Attribute(.unique) var id: UUID
    var templateId: UUID
    var templateName: String
    var scheduledDate: Date
    var isCompleted: Bool
    var completedWorkoutId: UUID?
    var createdAt: Date

    init(
        id: UUID = UUID(),
        templateId: UUID,
        templateName: String,
        scheduledDate: Date,
        isCompleted: Bool = false,
        completedWorkoutId: UUID? = nil,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.templateId = templateId
        self.templateName = templateName
        self.scheduledDate = scheduledDate
        self.isCompleted = isCompleted
        self.completedWorkoutId = completedWorkoutId
        self.createdAt = createdAt
    }
}
