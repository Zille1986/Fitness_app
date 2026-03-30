import Foundation
import SwiftData

@Model
final class GymWorkout {
    @Attribute(.unique) var id: UUID
    var name: String
    var startTime: Date
    var endTime: Date?
    var exercises: [WorkoutExercise]
    var notes: String?
    var templateId: UUID?
    var isCompleted: Bool
    var totalVolume: Double
    var totalSets: Int
    var totalReps: Int

    var durationMillis: Int64 {
        let end = endTime ?? Date()
        return Int64(end.timeIntervalSince(startTime) * 1000)
    }

    var durationFormatted: String {
        let totalMinutes = durationMillis / 60000
        let hours = totalMinutes / 60
        let minutes = totalMinutes % 60
        return hours > 0 ? "\(hours)h \(minutes)m" : "\(minutes)m"
    }

    init(
        id: UUID = UUID(),
        name: String = "",
        startTime: Date,
        endTime: Date? = nil,
        exercises: [WorkoutExercise] = [],
        notes: String? = nil,
        templateId: UUID? = nil,
        isCompleted: Bool = false,
        totalVolume: Double = 0.0,
        totalSets: Int = 0,
        totalReps: Int = 0
    ) {
        self.id = id
        self.name = name
        self.startTime = startTime
        self.endTime = endTime
        self.exercises = exercises
        self.notes = notes
        self.templateId = templateId
        self.isCompleted = isCompleted
        self.totalVolume = totalVolume
        self.totalSets = totalSets
        self.totalReps = totalReps
    }
}

struct WorkoutExercise: Codable, Hashable, Identifiable {
    var id: String
    var exerciseId: UUID
    var exerciseName: String
    var sets: [WorkoutSet]
    var notes: String?
    var restSeconds: Int
    var orderIndex: Int
    var videoFileName: String?

    var completedSets: Int { sets.filter(\.isCompleted).count }
    var totalVolume: Double { sets.filter(\.isCompleted).reduce(0) { $0 + $1.weight * Double($1.reps) } }
    var totalReps: Int { sets.filter(\.isCompleted).reduce(0) { $0 + $1.reps } }

    init(
        id: String = UUID().uuidString,
        exerciseId: UUID,
        exerciseName: String,
        sets: [WorkoutSet] = [],
        notes: String? = nil,
        restSeconds: Int = 90,
        orderIndex: Int = 0,
        videoFileName: String? = nil
    ) {
        self.id = id
        self.exerciseId = exerciseId
        self.exerciseName = exerciseName
        self.sets = sets
        self.notes = notes
        self.restSeconds = restSeconds
        self.orderIndex = orderIndex
        self.videoFileName = videoFileName
    }
}

struct WorkoutSet: Codable, Hashable, Identifiable {
    var id: String
    var setNumber: Int
    var setType: SetType
    var weight: Double
    var reps: Int
    var targetWeight: Double?
    var targetReps: Int?
    var rpe: Int?
    var isCompleted: Bool
    var completedAt: Date?
    var notes: String?

    var volume: Double { weight * Double(reps) }

    init(
        id: String = UUID().uuidString,
        setNumber: Int,
        setType: SetType = .working,
        weight: Double = 0.0,
        reps: Int = 0,
        targetWeight: Double? = nil,
        targetReps: Int? = nil,
        rpe: Int? = nil,
        isCompleted: Bool = false,
        completedAt: Date? = nil,
        notes: String? = nil
    ) {
        self.id = id
        self.setNumber = setNumber
        self.setType = setType
        self.weight = weight
        self.reps = reps
        self.targetWeight = targetWeight
        self.targetReps = targetReps
        self.rpe = rpe
        self.isCompleted = isCompleted
        self.completedAt = completedAt
        self.notes = notes
    }
}

enum SetType: String, Codable, CaseIterable {
    case warmup = "WARMUP"
    case working = "WORKING"
    case dropSet = "DROP_SET"
    case failure = "FAILURE"
    case amrap = "AMRAP"

    var displayName: String {
        switch self {
        case .warmup: return "Warm-up"
        case .working: return "Working"
        case .dropSet: return "Drop Set"
        case .failure: return "To Failure"
        case .amrap: return "AMRAP"
        }
    }
}
