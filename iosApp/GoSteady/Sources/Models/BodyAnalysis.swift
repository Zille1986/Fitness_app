import Foundation
import SwiftData

@Model
final class BodyScan {
    @Attribute(.unique) var id: UUID
    var timestamp: Date
    var photoPath: String
    var userGoal: FitnessGoal
    var bodyType: BodyType
    var estimatedBodyFatPercentage: Float?
    var focusZones: [BodyZone]
    var overallScore: Int
    var muscleBalance: MuscleBalanceAssessment
    var postureAssessment: PostureAssessment
    var notes: String?

    init(
        id: UUID = UUID(),
        timestamp: Date = Date(),
        photoPath: String,
        userGoal: FitnessGoal,
        bodyType: BodyType,
        estimatedBodyFatPercentage: Float? = nil,
        focusZones: [BodyZone],
        overallScore: Int,
        muscleBalance: MuscleBalanceAssessment,
        postureAssessment: PostureAssessment,
        notes: String? = nil
    ) {
        self.id = id
        self.timestamp = timestamp
        self.photoPath = photoPath
        self.userGoal = userGoal
        self.bodyType = bodyType
        self.estimatedBodyFatPercentage = estimatedBodyFatPercentage
        self.focusZones = focusZones
        self.overallScore = overallScore
        self.muscleBalance = muscleBalance
        self.postureAssessment = postureAssessment
        self.notes = notes
    }
}

enum FitnessGoal: String, Codable, CaseIterable, Identifiable {
    var id: String { rawValue }
    var icon: String {
        switch self {
        case .loseWeight: return "flame.fill"
        case .buildMuscle: return "dumbbell.fill"
        case .toneUp: return "figure.strengthtraining.traditional"
        case .improveEndurance: return "heart.fill"
        case .increaseStrength: return "bolt.fill"
        case .generalFitness: return "figure.run"
        case .athleticPerformance: return "sportscourt.fill"
        case .bodyRecomposition: return "figure.flexibility"
        }
    }
    //
    case loseWeight = "LOSE_WEIGHT"
    case buildMuscle = "BUILD_MUSCLE"
    case toneUp = "TONE_UP"
    case improveEndurance = "IMPROVE_ENDURANCE"
    case increaseStrength = "INCREASE_STRENGTH"
    case generalFitness = "GENERAL_FITNESS"
    case athleticPerformance = "ATHLETIC_PERFORMANCE"
    case bodyRecomposition = "BODY_RECOMPOSITION"

    var displayName: String {
        switch self {
        case .loseWeight: return "Lose Weight"
        case .buildMuscle: return "Build Muscle"
        case .toneUp: return "Tone & Define"
        case .improveEndurance: return "Improve Endurance"
        case .increaseStrength: return "Increase Strength"
        case .generalFitness: return "General Fitness"
        case .athleticPerformance: return "Athletic Performance"
        case .bodyRecomposition: return "Body Recomposition"
        }
    }

    var goalDescription: String {
        switch self {
        case .loseWeight: return "Focus on fat loss and cardio"
        case .buildMuscle: return "Focus on strength training and muscle growth"
        case .toneUp: return "Reduce body fat while maintaining muscle"
        case .improveEndurance: return "Build cardiovascular fitness"
        case .increaseStrength: return "Maximize power and lifting capacity"
        case .generalFitness: return "Overall health and wellness"
        case .athleticPerformance: return "Sport-specific training"
        case .bodyRecomposition: return "Lose fat and gain muscle simultaneously"
        }
    }
}

enum BodyType: String, Codable, CaseIterable {
    case ectomorph = "ECTOMORPH"
    case mesomorph = "MESOMORPH"
    case endomorph = "ENDOMORPH"
    case ectoMesomorph = "ECTO_MESOMORPH"
    case endoMesomorph = "ENDO_MESOMORPH"

    var displayName: String {
        switch self {
        case .ectomorph: return "Ectomorph"
        case .mesomorph: return "Mesomorph"
        case .endomorph: return "Endomorph"
        case .ectoMesomorph: return "Ecto-Mesomorph"
        case .endoMesomorph: return "Endo-Mesomorph"
        }
    }

    var characteristics: String {
        switch self {
        case .ectomorph: return "Lean build, fast metabolism, difficulty gaining weight"
        case .mesomorph: return "Athletic build, gains muscle easily, moderate metabolism"
        case .endomorph: return "Wider build, slower metabolism, gains weight easily"
        case .ectoMesomorph: return "Lean but can build muscle with effort"
        case .endoMesomorph: return "Athletic but prone to weight gain"
        }
    }
}

enum BodyZone: String, Codable, CaseIterable, Identifiable {
    var id: String { rawValue }
    //
    case upperChest = "UPPER_CHEST"
    case lowerChest = "LOWER_CHEST"
    case shoulders = "SHOULDERS"
    case upperBack = "UPPER_BACK"
    case lats = "LATS"
    case lowerBack = "LOWER_BACK"
    case biceps = "BICEPS"
    case triceps = "TRICEPS"
    case forearms = "FOREARMS"
    case abs = "ABS"
    case obliques = "OBLIQUES"
    case quads = "QUADS"
    case hamstrings = "HAMSTRINGS"
    case glutes = "GLUTES"
    case calves = "CALVES"
    case core = "CORE"

    var displayName: String {
        switch self {
        case .upperChest: return "Upper Chest"
        case .lowerChest: return "Lower Chest"
        case .shoulders: return "Shoulders"
        case .upperBack: return "Upper Back"
        case .lats: return "Lats"
        case .lowerBack: return "Lower Back"
        case .biceps: return "Biceps"
        case .triceps: return "Triceps"
        case .forearms: return "Forearms"
        case .abs: return "Abs"
        case .obliques: return "Obliques"
        case .quads: return "Quadriceps"
        case .hamstrings: return "Hamstrings"
        case .glutes: return "Glutes"
        case .calves: return "Calves"
        case .core: return "Core"
        }
    }

    var muscleGroups: [String] {
        switch self {
        case .upperChest: return ["Pectoralis Major (Clavicular)"]
        case .lowerChest: return ["Pectoralis Major (Sternal)"]
        case .shoulders: return ["Anterior Deltoid", "Lateral Deltoid", "Posterior Deltoid"]
        case .upperBack: return ["Trapezius", "Rhomboids", "Rear Deltoids"]
        case .lats: return ["Latissimus Dorsi"]
        case .lowerBack: return ["Erector Spinae"]
        case .biceps: return ["Biceps Brachii", "Brachialis"]
        case .triceps: return ["Triceps Brachii"]
        case .forearms: return ["Forearm Flexors", "Forearm Extensors"]
        case .abs: return ["Rectus Abdominis", "Transverse Abdominis"]
        case .obliques: return ["Internal Obliques", "External Obliques"]
        case .quads: return ["Rectus Femoris", "Vastus Lateralis", "Vastus Medialis"]
        case .hamstrings: return ["Biceps Femoris", "Semitendinosus", "Semimembranosus"]
        case .glutes: return ["Gluteus Maximus", "Gluteus Medius"]
        case .calves: return ["Gastrocnemius", "Soleus"]
        case .core: return ["Deep Core Stabilizers", "Hip Flexors"]
        }
    }
}

enum ZonePriority: String, Codable, CaseIterable {
    case high = "HIGH"
    case medium = "MEDIUM"
    case low = "LOW"
}

struct ZoneRecommendation: Codable, Hashable {
    var zone: BodyZone
    var priority: ZonePriority
    var currentAssessment: String
    var recommendedExercises: [String]
    var weeklyFrequency: Int
    var notes: String?
}

struct MuscleBalanceAssessment: Codable, Hashable {
    var overallBalance: BalanceLevel
    var leftRightSymmetry: BalanceLevel
    var frontBackBalance: BalanceLevel
    var upperLowerBalance: BalanceLevel
    var imbalances: [MuscleImbalance]

    init(
        overallBalance: BalanceLevel = .balanced,
        leftRightSymmetry: BalanceLevel = .balanced,
        frontBackBalance: BalanceLevel = .balanced,
        upperLowerBalance: BalanceLevel = .balanced,
        imbalances: [MuscleImbalance] = []
    ) {
        self.overallBalance = overallBalance
        self.leftRightSymmetry = leftRightSymmetry
        self.frontBackBalance = frontBackBalance
        self.upperLowerBalance = upperLowerBalance
        self.imbalances = imbalances
    }
}

enum BalanceLevel: String, Codable, CaseIterable {
    case balanced = "BALANCED"
    case slightImbalance = "SLIGHT_IMBALANCE"
    case moderateImbalance = "MODERATE_IMBALANCE"
    case significantImbalance = "SIGNIFICANT_IMBALANCE"

    var displayName: String {
        switch self {
        case .balanced: return "Balanced"
        case .slightImbalance: return "Slight Imbalance"
        case .moderateImbalance: return "Moderate Imbalance"
        case .significantImbalance: return "Significant Imbalance"
        }
    }
}

struct MuscleImbalance: Codable, Hashable, Identifiable {
    var id: String { imbalanceDescription }
    var imbalanceDescription: String
    var affectedZones: [BodyZone]
    var correction: String
}

struct PostureAssessment: Codable, Hashable {
    var overallPosture: PostureLevel
    var issues: [PostureIssue]

    init(overallPosture: PostureLevel = .good, issues: [PostureIssue] = []) {
        self.overallPosture = overallPosture
        self.issues = issues
    }
}

enum PostureLevel: String, Codable, CaseIterable {
    case excellent = "EXCELLENT"
    case good = "GOOD"
    case fair = "FAIR"
    case poor = "POOR"

    var displayName: String {
        switch self {
        case .excellent: return "Excellent"
        case .good: return "Good"
        case .fair: return "Fair"
        case .poor: return "Poor"
        }
    }
}

struct PostureIssue: Codable, Hashable, Identifiable {
    var id: String { "\(type.rawValue)_\(severity.rawValue)" }
    var type: PostureIssueType
    var severity: IssueSeverity
    var issueDescription: String
    var exercises: [String]
}

enum PostureIssueType: String, Codable, CaseIterable {
    case forwardHead = "FORWARD_HEAD"
    case roundedShoulders = "ROUNDED_SHOULDERS"
    case kyphosis = "KYPHOSIS"
    case lordosis = "LORDOSIS"
    case anteriorPelvicTilt = "ANTERIOR_PELVIC_TILT"
    case posteriorPelvicTilt = "POSTERIOR_PELVIC_TILT"
    case scoliosis = "SCOLIOSIS"
    case unevenShoulders = "UNEVEN_SHOULDERS"
    case unevenHips = "UNEVEN_HIPS"

    var displayName: String {
        switch self {
        case .forwardHead: return "Forward Head Posture"
        case .roundedShoulders: return "Rounded Shoulders"
        case .kyphosis: return "Upper Back Rounding"
        case .lordosis: return "Excessive Lower Back Curve"
        case .anteriorPelvicTilt: return "Anterior Pelvic Tilt"
        case .posteriorPelvicTilt: return "Posterior Pelvic Tilt"
        case .scoliosis: return "Lateral Spine Curvature"
        case .unevenShoulders: return "Uneven Shoulders"
        case .unevenHips: return "Uneven Hips"
        }
    }
}

enum IssueSeverity: String, Codable, CaseIterable {
    case mild = "MILD"
    case moderate = "MODERATE"
    case severe = "SEVERE"

    var displayName: String {
        switch self {
        case .mild: return "Mild"
        case .moderate: return "Moderate"
        case .severe: return "Severe"
        }
    }
}

struct BodyAnalysisReport: Codable {
    var scan: BodyScanData
    var zoneRecommendations: [ZoneRecommendation]
    var workoutRecommendations: [WorkoutRecommendation]
    var nutritionRecommendations: [NutritionRecommendation]
    var weeklyPlanSuggestion: WeeklyPlanSuggestion
    var progressNotes: String?
}

struct BodyScanData: Codable {
    var id: UUID
    var timestamp: Date
    var photoPath: String
    var userGoal: FitnessGoal
    var bodyType: BodyType
    var estimatedBodyFatPercentage: Float?
    var focusZones: [BodyZone]
    var overallScore: Int
    var muscleBalance: MuscleBalanceAssessment
    var postureAssessment: PostureAssessment
    var notes: String?
}

struct WorkoutRecommendation: Codable, Hashable {
    var title: String
    var recommendationDescription: String
    var type: WorkoutRecommendationType
    var frequency: String
    var duration: String
    var exercises: [RecommendedExercise]
    var priority: Int
}

enum WorkoutRecommendationType: String, Codable, CaseIterable {
    case strength = "STRENGTH"
    case hypertrophy = "HYPERTROPHY"
    case cardio = "CARDIO"
    case hiit = "HIIT"
    case flexibility = "FLEXIBILITY"
    case recovery = "RECOVERY"
    case postureCorrection = "POSTURE_CORRECTION"
}

struct RecommendedExercise: Codable, Hashable {
    var name: String
    var sets: String
    var reps: String
    var notes: String?
}

struct NutritionRecommendation: Codable, Hashable {
    var category: NutritionCategory
    var title: String
    var recommendationDescription: String
    var tips: [String]
    var priority: Int
}

enum NutritionCategory: String, Codable, CaseIterable {
    case protein = "PROTEIN"
    case carbs = "CARBS"
    case fats = "FATS"
    case hydration = "HYDRATION"
    case timing = "TIMING"
    case supplements = "SUPPLEMENTS"
    case calories = "CALORIES"
    case micronutrients = "MICRONUTRIENTS"

    var displayName: String {
        switch self {
        case .protein: return "Protein"
        case .carbs: return "Carbohydrates"
        case .fats: return "Healthy Fats"
        case .hydration: return "Hydration"
        case .timing: return "Meal Timing"
        case .supplements: return "Supplements"
        case .calories: return "Calorie Balance"
        case .micronutrients: return "Vitamins & Minerals"
        }
    }
}

struct WeeklyPlanSuggestion: Codable, Hashable {
    var totalTrainingDays: Int
    var cardioSessions: Int
    var strengthSessions: Int
    var restDays: Int
    var splitType: TrainingSplit
    var dailyBreakdown: [DayPlan]
}

enum TrainingSplit: String, Codable, CaseIterable {
    case fullBody = "FULL_BODY"
    case upperLower = "UPPER_LOWER"
    case pushPullLegs = "PUSH_PULL_LEGS"
    case broSplit = "BRO_SPLIT"
    case cardioStrength = "CARDIO_STRENGTH"

    var displayName: String {
        switch self {
        case .fullBody: return "Full Body"
        case .upperLower: return "Upper/Lower"
        case .pushPullLegs: return "Push/Pull/Legs"
        case .broSplit: return "Body Part Split"
        case .cardioStrength: return "Cardio + Strength"
        }
    }
}

struct DayPlan: Codable, Hashable {
    var dayOfWeek: String
    var focus: String
    var duration: String
    var isRestDay: Bool

    init(dayOfWeek: String, focus: String, duration: String, isRestDay: Bool = false) {
        self.dayOfWeek = dayOfWeek
        self.focus = focus
        self.duration = duration
        self.isRestDay = isRestDay
    }
}

struct BodyScanComparison: Codable {
    var currentScan: BodyScanData
    var previousScan: BodyScanData?
    var daysBetween: Int
    var scoreChange: Int
    var bodyFatChange: Float?
    var improvedZones: [BodyZone]
    var needsMoreWorkZones: [BodyZone]
    var postureImprovement: Bool
    var overallProgress: ProgressAssessment
    var improvements: [ImprovementItem]
    var declines: [ImprovementItem]
    var unchanged: [ImprovementItem]
    var summaryMessage: String
    var recommendations: [String]
}

struct ImprovementItem: Codable, Hashable {
    var category: ImprovementCategory
    var title: String
    var itemDescription: String
    var previousValue: String?
    var currentValue: String
    var changePercent: Float?
    var isPositive: Bool

    init(
        category: ImprovementCategory,
        title: String,
        itemDescription: String,
        previousValue: String? = nil,
        currentValue: String,
        changePercent: Float? = nil,
        isPositive: Bool = true
    ) {
        self.category = category
        self.title = title
        self.itemDescription = itemDescription
        self.previousValue = previousValue
        self.currentValue = currentValue
        self.changePercent = changePercent
        self.isPositive = isPositive
    }
}

enum ImprovementCategory: String, Codable, CaseIterable {
    case overallScore = "OVERALL_SCORE"
    case bodyFat = "BODY_FAT"
    case posture = "POSTURE"
    case muscleBalance = "MUSCLE_BALANCE"
    case focusZones = "FOCUS_ZONES"
    case symmetry = "SYMMETRY"

    var displayName: String {
        switch self {
        case .overallScore: return "Overall Score"
        case .bodyFat: return "Body Composition"
        case .posture: return "Posture"
        case .muscleBalance: return "Muscle Balance"
        case .focusZones: return "Focus Areas"
        case .symmetry: return "Symmetry"
        }
    }
}

enum ProgressAssessment: String, Codable, CaseIterable {
    case excellent = "EXCELLENT"
    case good = "GOOD"
    case steady = "STEADY"
    case slow = "SLOW"
    case plateau = "PLATEAU"
    case firstScan = "FIRST_SCAN"

    var displayName: String {
        switch self {
        case .excellent: return "Excellent Progress"
        case .good: return "Good Progress"
        case .steady: return "Steady Progress"
        case .slow: return "Slow Progress"
        case .plateau: return "Plateau"
        case .firstScan: return "First Scan"
        }
    }

    var assessmentDescription: String {
        switch self {
        case .excellent: return "You're making great strides toward your goals!"
        case .good: return "You're on the right track, keep it up!"
        case .steady: return "Consistent effort is showing results."
        case .slow: return "Consider adjusting your routine for better results."
        case .plateau: return "Time to shake things up with new challenges."
        case .firstScan: return "Great start! We'll track your progress from here."
        }
    }
}

struct BodyScanWithDetails: Codable {
    var scan: BodyScanData
    var formattedDate: String
    var goalDisplayName: String
    var focusZoneCount: Int
    var hasPostureIssues: Bool
}

struct BodyProgressSummary: Codable {
    var totalScans: Int
    var firstScanDate: Date?
    var latestScanDate: Date?
    var averageScore: Float
    var bestScore: Int
    var scoreImprovement: Int
    var consistentlyImprovedZones: [BodyZone]
    var persistentFocusZones: [BodyZone]
    var goalAchievementProgress: Float
}
