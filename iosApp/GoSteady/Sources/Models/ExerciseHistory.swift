import Foundation
import SwiftData

@Model
final class ExerciseHistory {
    @Attribute(.unique) var id: UUID
    var exerciseId: UUID
    var workoutId: UUID
    var date: Date
    var bestWeight: Double
    var bestReps: Int
    var totalVolume: Double
    var totalSets: Int
    var totalReps: Int
    var estimatedOneRepMax: Double
    var isPersonalRecord: Bool

    init(
        id: UUID = UUID(),
        exerciseId: UUID,
        workoutId: UUID,
        date: Date,
        bestWeight: Double,
        bestReps: Int,
        totalVolume: Double,
        totalSets: Int,
        totalReps: Int,
        estimatedOneRepMax: Double,
        isPersonalRecord: Bool = false
    ) {
        self.id = id
        self.exerciseId = exerciseId
        self.workoutId = workoutId
        self.date = date
        self.bestWeight = bestWeight
        self.bestReps = bestReps
        self.totalVolume = totalVolume
        self.totalSets = totalSets
        self.totalReps = totalReps
        self.estimatedOneRepMax = estimatedOneRepMax
        self.isPersonalRecord = isPersonalRecord
    }
}

struct ExerciseStats: Codable, Hashable {
    var exerciseId: UUID
    var exerciseName: String
    var currentOneRepMax: Double
    var previousOneRepMax: Double?
    var currentMaxWeight: Double
    var previousMaxWeight: Double?
    var totalVolumeLast30Days: Double
    var totalSetsLast30Days: Int
    var lastPerformed: Date?
    var timesPerformed: Int
    var progressTrend: ProgressTrend
}

enum ProgressTrend: String, Codable, CaseIterable {
    case improving = "IMPROVING"
    case maintaining = "MAINTAINING"
    case declining = "DECLINING"
    case new = "NEW"
}

struct PersonalRecord: Codable, Hashable {
    var exerciseId: UUID
    var exerciseName: String
    var recordType: RecordType
    var value: Double
    var reps: Int?
    var date: Date
    var previousValue: Double?
}

enum RecordType: String, Codable, CaseIterable {
    case oneRepMax = "ONE_REP_MAX"
    case maxWeight = "MAX_WEIGHT"
    case maxReps = "MAX_REPS"
    case maxVolume = "MAX_VOLUME"
}

struct ProgressionSuggestion: Codable, Hashable {
    var exerciseId: UUID
    var exerciseName: String
    var suggestionType: SuggestionType
    var currentWeight: Double
    var currentReps: Int
    var suggestedWeight: Double
    var suggestedReps: Int
    var confidence: Float
    var reasoning: String
}

enum SuggestionType: String, Codable, CaseIterable {
    case increaseWeight = "INCREASE_WEIGHT"
    case increaseReps = "INCREASE_REPS"
    case maintain = "MAINTAIN"
    case deload = "DELOAD"
    case tryNewVariation = "TRY_NEW_VARIATION"
}
