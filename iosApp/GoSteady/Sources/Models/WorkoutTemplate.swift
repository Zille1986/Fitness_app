import Foundation
import SwiftData

@Model
final class WorkoutTemplate {
    @Attribute(.unique) var id: UUID
    var name: String
    var templateDescription: String
    var targetMuscleGroups: [MuscleGroup]
    var exercises: [TemplateExercise]
    var estimatedDurationMinutes: Int
    var difficulty: Difficulty
    var isDefault: Bool
    var timesUsed: Int
    var lastUsed: Date?
    var createdAt: Date

    init(
        id: UUID = UUID(),
        name: String,
        templateDescription: String = "",
        targetMuscleGroups: [MuscleGroup] = [],
        exercises: [TemplateExercise] = [],
        estimatedDurationMinutes: Int = 60,
        difficulty: Difficulty = .intermediate,
        isDefault: Bool = false,
        timesUsed: Int = 0,
        lastUsed: Date? = nil,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.name = name
        self.templateDescription = templateDescription
        self.targetMuscleGroups = targetMuscleGroups
        self.exercises = exercises
        self.estimatedDurationMinutes = estimatedDurationMinutes
        self.difficulty = difficulty
        self.isDefault = isDefault
        self.timesUsed = timesUsed
        self.lastUsed = lastUsed
        self.createdAt = createdAt
    }
}

struct TemplateExercise: Codable, Hashable, Identifiable {
    var id: String
    var exerciseId: UUID
    var exerciseName: String
    var sets: Int
    var targetRepsMin: Int
    var targetRepsMax: Int
    var restSeconds: Int
    var notes: String?
    var orderIndex: Int
    var supersetGroup: Int?

    var targetRepsDisplay: String {
        targetRepsMin == targetRepsMax ? "\(targetRepsMin)" : "\(targetRepsMin)-\(targetRepsMax)"
    }

    var targetReps: ClosedRange<Int> {
        targetRepsMin...targetRepsMax
    }

    init(
        id: String = UUID().uuidString,
        exerciseId: UUID,
        exerciseName: String,
        sets: Int = 3,
        targetRepsMin: Int = 8,
        targetRepsMax: Int = 12,
        restSeconds: Int = 90,
        notes: String? = nil,
        orderIndex: Int = 0,
        supersetGroup: Int? = nil
    ) {
        self.id = id
        self.exerciseId = exerciseId
        self.exerciseName = exerciseName
        self.sets = sets
        self.targetRepsMin = targetRepsMin
        self.targetRepsMax = targetRepsMax
        self.restSeconds = restSeconds
        self.notes = notes
        self.orderIndex = orderIndex
        self.supersetGroup = supersetGroup
    }
}
