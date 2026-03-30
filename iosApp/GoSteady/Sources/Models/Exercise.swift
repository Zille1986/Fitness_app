import Foundation
import SwiftData

@Model
final class Exercise {
    @Attribute(.unique) var id: UUID
    var name: String
    var exerciseDescription: String
    var muscleGroup: MuscleGroup
    var secondaryMuscleGroups: [MuscleGroup]
    var equipment: Equipment
    var exerciseType: ExerciseType
    var difficulty: Difficulty
    var instructions: [String]
    var tips: [String]
    var isCustom: Bool
    var imageUrl: String?
    var videoFileName: String?

    init(
        id: UUID = UUID(),
        name: String,
        exerciseDescription: String = "",
        muscleGroup: MuscleGroup,
        secondaryMuscleGroups: [MuscleGroup] = [],
        equipment: Equipment,
        exerciseType: ExerciseType,
        difficulty: Difficulty = .intermediate,
        instructions: [String] = [],
        tips: [String] = [],
        isCustom: Bool = false,
        imageUrl: String? = nil,
        videoFileName: String? = nil
    ) {
        self.id = id
        self.name = name
        self.exerciseDescription = exerciseDescription
        self.muscleGroup = muscleGroup
        self.secondaryMuscleGroups = secondaryMuscleGroups
        self.equipment = equipment
        self.exerciseType = exerciseType
        self.difficulty = difficulty
        self.instructions = instructions
        self.tips = tips
        self.isCustom = isCustom
        self.imageUrl = imageUrl
        self.videoFileName = videoFileName
    }
}

enum MuscleGroup: String, Codable, CaseIterable {
    case chest = "CHEST"
    case back = "BACK"
    case shoulders = "SHOULDERS"
    case biceps = "BICEPS"
    case triceps = "TRICEPS"
    case forearms = "FOREARMS"
    case abs = "ABS"
    case obliques = "OBLIQUES"
    case lowerBack = "LOWER_BACK"
    case quads = "QUADS"
    case hamstrings = "HAMSTRINGS"
    case glutes = "GLUTES"
    case calves = "CALVES"
    case hipFlexors = "HIP_FLEXORS"
    case traps = "TRAPS"
    case lats = "LATS"
    case fullBody = "FULL_BODY"

    var displayName: String {
        rawValue.replacingOccurrences(of: "_", with: " ").capitalized
    }
}

enum Equipment: String, Codable, CaseIterable {
    case barbell = "BARBELL"
    case dumbbell = "DUMBBELL"
    case kettlebell = "KETTLEBELL"
    case cable = "CABLE"
    case machine = "MACHINE"
    case bodyweight = "BODYWEIGHT"
    case resistanceBand = "RESISTANCE_BAND"
    case ezBar = "EZ_BAR"
    case smithMachine = "SMITH_MACHINE"
    case pullUpBar = "PULL_UP_BAR"
    case dipBars = "DIP_BARS"
    case bench = "BENCH"
    case medicineBall = "MEDICINE_BALL"
    case foamRoller = "FOAM_ROLLER"
    case other = "OTHER"
    case none = "NONE"

    var displayName: String {
        rawValue.replacingOccurrences(of: "_", with: " ").capitalized
    }
}

enum ExerciseType: String, Codable, CaseIterable {
    case compound = "COMPOUND"
    case isolation = "ISOLATION"
    case cardio = "CARDIO"
    case stretching = "STRETCHING"
    case plyometric = "PLYOMETRIC"
}

enum Difficulty: String, Codable, CaseIterable {
    case beginner = "BEGINNER"
    case intermediate = "INTERMEDIATE"
    case advanced = "ADVANCED"
    case expert = "EXPERT"
}
