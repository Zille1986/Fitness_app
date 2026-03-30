import Foundation
import SwiftData

@Model
final class HIITSession {
    @Attribute(.unique) var id: UUID
    var date: Date
    var templateId: String
    var templateName: String
    var totalDurationMs: Int64
    var exerciseCount: Int
    var roundsCompleted: Int
    var totalRounds: Int
    var caloriesEstimate: Int
    var avgHeartRate: Int?
    var maxHeartRate: Int?
    var exerciseLog: String
    var isCompleted: Bool
    var source: String

    var durationFormatted: String {
        let totalSeconds = totalDurationMs / 1000
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            return String(format: "%d:%02d", minutes, seconds)
        }
    }

    init(
        id: UUID = UUID(),
        date: Date = Date(),
        templateId: String,
        templateName: String,
        totalDurationMs: Int64 = 0,
        exerciseCount: Int = 0,
        roundsCompleted: Int = 0,
        totalRounds: Int = 0,
        caloriesEstimate: Int = 0,
        avgHeartRate: Int? = nil,
        maxHeartRate: Int? = nil,
        exerciseLog: String = "[]",
        isCompleted: Bool = false,
        source: String = "phone"
    ) {
        self.id = id
        self.date = date
        self.templateId = templateId
        self.templateName = templateName
        self.totalDurationMs = totalDurationMs
        self.exerciseCount = exerciseCount
        self.roundsCompleted = roundsCompleted
        self.totalRounds = totalRounds
        self.caloriesEstimate = caloriesEstimate
        self.avgHeartRate = avgHeartRate
        self.maxHeartRate = maxHeartRate
        self.exerciseLog = exerciseLog
        self.isCompleted = isCompleted
        self.source = source
    }
}

enum HIITDifficulty: String, Codable, CaseIterable {
    case easy = "EASY"
    case medium = "MEDIUM"
    case hard = "HARD"

    var displayName: String {
        switch self {
        case .easy: return "Easy"
        case .medium: return "Medium"
        case .hard: return "Hard"
        }
    }

    var colorHex: String {
        switch self {
        case .easy: return "4CAF50"
        case .medium: return "FF9800"
        case .hard: return "E53935"
        }
    }
}

struct HIITExercise: Codable, Hashable, Identifiable {
    var id: String
    var name: String
    var exerciseDescription: String
    var muscleGroups: [String]
    var difficulty: HIITDifficulty
    var caloriesPerMinute: Int
    var videoFileName: String?

    init(
        id: String,
        name: String,
        exerciseDescription: String,
        muscleGroups: [String],
        difficulty: HIITDifficulty,
        caloriesPerMinute: Int,
        videoFileName: String? = nil
    ) {
        self.id = id
        self.name = name
        self.exerciseDescription = exerciseDescription
        self.muscleGroups = muscleGroups
        self.difficulty = difficulty
        self.caloriesPerMinute = caloriesPerMinute
        self.videoFileName = videoFileName
    }
}

struct HIITWorkoutExercise: Codable, Hashable {
    var exercise: HIITExercise
    var durationOverrideSec: Int?

    init(exercise: HIITExercise, durationOverrideSec: Int? = nil) {
        self.exercise = exercise
        self.durationOverrideSec = durationOverrideSec
    }
}

struct HIITPhaseStep: Codable, Hashable {
    var name: String
    var stepDescription: String
    var videoFileName: String?

    init(_ name: String, _ stepDescription: String, _ videoFileName: String? = nil) {
        self.name = name
        self.stepDescription = stepDescription
        self.videoFileName = videoFileName
    }
}

struct HIITWorkoutTemplate: Codable, Hashable, Identifiable {
    var id: String
    var name: String
    var templateDescription: String
    var difficulty: String
    var exercises: [HIITWorkoutExercise]
    var workDurationSec: Int
    var restDurationSec: Int
    var rounds: Int
    var warmupSec: Int
    var cooldownSec: Int
    var warmupSteps: [HIITPhaseStep]
    var cooldownSteps: [HIITPhaseStep]

    var estimatedDurationSec: Int {
        warmupSec + cooldownSec +
            (exercises.count * (workDurationSec + restDurationSec) * rounds) -
            restDurationSec
    }

    var formattedDuration: String {
        let mins = estimatedDurationSec / 60
        return mins >= 60 ? "\(mins / 60)h \(mins % 60)m" : "\(mins)m"
    }

    init(
        id: String,
        name: String,
        templateDescription: String,
        difficulty: String,
        exercises: [HIITWorkoutExercise],
        workDurationSec: Int,
        restDurationSec: Int,
        rounds: Int,
        warmupSec: Int,
        cooldownSec: Int,
        warmupSteps: [HIITPhaseStep] = [],
        cooldownSteps: [HIITPhaseStep] = []
    ) {
        self.id = id
        self.name = name
        self.templateDescription = templateDescription
        self.difficulty = difficulty
        self.exercises = exercises
        self.workDurationSec = workDurationSec
        self.restDurationSec = restDurationSec
        self.rounds = rounds
        self.warmupSec = warmupSec
        self.cooldownSec = cooldownSec
        self.warmupSteps = warmupSteps
        self.cooldownSteps = cooldownSteps
    }
}

enum HIITExerciseLibrary {

    // MARK: - Exercises

    static let burpees = HIITExercise(id: "burpees", name: "Burpees", exerciseDescription: "Drop into a push-up, jump feet to hands, then explosively jump up with arms overhead.", muscleGroups: ["Full body"], difficulty: .hard, caloriesPerMinute: 14, videoFileName: "hiit-burpees")
    static let jumpSquats = HIITExercise(id: "jump_squats", name: "Jump Squats", exerciseDescription: "Squat deep, then explode upward into a jump, landing softly back into the squat position.", muscleGroups: ["Quads", "Glutes"], difficulty: .medium, caloriesPerMinute: 12, videoFileName: "hiit-jump-squats")
    static let mountainClimbers = HIITExercise(id: "mountain_climbers", name: "Mountain Climbers", exerciseDescription: "In a push-up position, rapidly alternate driving each knee toward your chest.", muscleGroups: ["Core", "Shoulders"], difficulty: .medium, caloriesPerMinute: 11, videoFileName: "hiit-mountain-climbers")
    static let pushupShoulderTap = HIITExercise(id: "pushup_shoulder_tap", name: "Push-Up to Shoulder Tap", exerciseDescription: "Perform a push-up, then at the top, tap each shoulder with the opposite hand while keeping hips stable.", muscleGroups: ["Chest", "Core", "Shoulders"], difficulty: .hard, caloriesPerMinute: 10, videoFileName: "hiit-pushup-shoulder-tap")
    static let highKnees = HIITExercise(id: "high_knees", name: "High Knees", exerciseDescription: "Run in place, driving each knee up to hip height as fast as possible while pumping your arms.", muscleGroups: ["Hip flexors", "Quads"], difficulty: .easy, caloriesPerMinute: 10, videoFileName: "hiit-high-knees")
    static let plankJacks = HIITExercise(id: "plank_jacks", name: "Plank Jacks", exerciseDescription: "In a plank position, jump your feet wide apart and then back together, like a horizontal jumping jack.", muscleGroups: ["Core", "Shoulders"], difficulty: .medium, caloriesPerMinute: 9, videoFileName: "hiit-plank-jacks")
    static let skaterJumps = HIITExercise(id: "skater_jumps", name: "Skater Jumps", exerciseDescription: "Leap laterally from one foot to the other, landing on one leg and sweeping the other behind, like a speed skater.", muscleGroups: ["Glutes", "Quads"], difficulty: .medium, caloriesPerMinute: 11, videoFileName: "hiit-skater-jumps")
    static let bicycleCrunches = HIITExercise(id: "bicycle_crunches", name: "Bicycle Crunches", exerciseDescription: "Lie on your back, alternate bringing each elbow to the opposite knee in a pedaling motion.", muscleGroups: ["Core", "Obliques"], difficulty: .easy, caloriesPerMinute: 7, videoFileName: "hiit-bicycle-crunches")
    static let tuckJumps = HIITExercise(id: "tuck_jumps", name: "Tuck Jumps", exerciseDescription: "Jump explosively, pulling both knees up to your chest at the peak, then land softly.", muscleGroups: ["Full body"], difficulty: .hard, caloriesPerMinute: 14, videoFileName: "hiit-tuck-jumps")
    static let diamondPushups = HIITExercise(id: "diamond_pushups", name: "Diamond Push-Ups", exerciseDescription: "Place hands close together forming a diamond shape, then perform a push-up targeting the triceps.", muscleGroups: ["Triceps", "Chest"], difficulty: .hard, caloriesPerMinute: 9, videoFileName: "hiit-diamond-pushups")
    static let bearCrawl = HIITExercise(id: "bear_crawl", name: "Bear Crawl", exerciseDescription: "On all fours with knees hovering, crawl forward moving opposite hand and foot together, keeping back flat.", muscleGroups: ["Shoulders", "Core", "Quads"], difficulty: .medium, caloriesPerMinute: 12, videoFileName: "hiit-bear-crawl")
    static let boxJumps = HIITExercise(id: "box_jumps", name: "Box Jumps", exerciseDescription: "Explosively jump onto an elevated surface, landing softly with both feet, then step back down.", muscleGroups: ["Quads", "Glutes", "Calves"], difficulty: .hard, caloriesPerMinute: 13, videoFileName: "hiit-box-jumps")
    static let russianTwists = HIITExercise(id: "russian_twists", name: "Russian Twists", exerciseDescription: "Seated with feet elevated, lean back at 45 degrees and rotate your torso side to side, tapping the floor.", muscleGroups: ["Obliques", "Abs"], difficulty: .easy, caloriesPerMinute: 7, videoFileName: "hiit-russian-twists")
    static let commandoPlank = HIITExercise(id: "commando_plank", name: "Commando Plank", exerciseDescription: "From forearm plank, push up to high plank one arm at a time, then lower back down. Alternate leading arm.", muscleGroups: ["Triceps", "Shoulders", "Core"], difficulty: .medium, caloriesPerMinute: 10, videoFileName: "hiit-commando-plank")
    static let lateralLunges = HIITExercise(id: "lateral_lunges", name: "Lateral Lunges", exerciseDescription: "Step wide to one side, bending that knee and sitting hips back while keeping the other leg straight. Alternate.", muscleGroups: ["Glutes", "Adductors", "Quads"], difficulty: .easy, caloriesPerMinute: 8, videoFileName: "hiit-lateral-lunges")
    static let flutterKicks = HIITExercise(id: "flutter_kicks", name: "Flutter Kicks", exerciseDescription: "Lie on your back, lift both legs slightly off the ground, and alternate kicking up and down rapidly.", muscleGroups: ["Lower abs", "Hip flexors"], difficulty: .easy, caloriesPerMinute: 7, videoFileName: "hiit-flutter-kicks")
    static let pikePushups = HIITExercise(id: "pike_pushups", name: "Pike Push-Ups", exerciseDescription: "In a downward-dog position with hips high, bend elbows to lower your head toward the floor, then press up.", muscleGroups: ["Shoulders", "Triceps"], difficulty: .hard, caloriesPerMinute: 9, videoFileName: "hiit-pike-pushups")
    static let squatToPress = HIITExercise(id: "squat_to_press", name: "Squat to Press", exerciseDescription: "Perform a full squat, then as you stand explosively, press your arms overhead in one fluid motion.", muscleGroups: ["Quads", "Glutes", "Shoulders"], difficulty: .medium, caloriesPerMinute: 12, videoFileName: "hiit-squat-to-press")
    static let deadBug = HIITExercise(id: "dead_bug", name: "Dead Bug", exerciseDescription: "Lie on your back with arms up and knees at 90 degrees. Extend opposite arm and leg outward, then alternate.", muscleGroups: ["Deep core", "Abs"], difficulty: .easy, caloriesPerMinute: 6, videoFileName: "hiit-dead-bug")
    static let manMakers = HIITExercise(id: "man_makers", name: "Man Makers", exerciseDescription: "From standing, drop to a push-up, do a renegade row each side, jump feet to hands, and stand with press overhead.", muscleGroups: ["Full body"], difficulty: .hard, caloriesPerMinute: 14, videoFileName: "hiit-man-makers")

    static let allExercises: [HIITExercise] = [
        burpees, jumpSquats, mountainClimbers, pushupShoulderTap, highKnees,
        plankJacks, skaterJumps, bicycleCrunches, tuckJumps, diamondPushups,
        bearCrawl, boxJumps, russianTwists, commandoPlank, lateralLunges,
        flutterKicks, pikePushups, squatToPress, deadBug, manMakers
    ]

    // MARK: - Warmup & Cooldown Steps

    private static let jumpingJacks = HIITPhaseStep("Jumping Jacks", "Light pace to raise heart rate and loosen up.", "warmup-jumping-jacks")
    private static let armCircles = HIITPhaseStep("Arm Circles", "Extend arms wide, make small circles forward then backward.", "warmup-arm-circles")
    private static let legSwings = HIITPhaseStep("Leg Swings", "Hold a wall, swing each leg forward and back, then side to side.", "warmup-leg-swings")
    private static let hipCircles = HIITPhaseStep("Hip Circles", "Hands on hips, rotate hips in wide circles both directions.", "warmup-hip-circles")
    private static let bodyweightSquats = HIITPhaseStep("Bodyweight Squats", "Slow, controlled squats to warm up knees, hips, and glutes.", "warmup-bodyweight-squats")
    private static let inchworms = HIITPhaseStep("Inchworms", "Bend forward, walk hands out to plank, walk feet back to hands, stand.", "warmup-inchworms")
    private static let torsoTwists = HIITPhaseStep("Torso Twists", "Feet shoulder-width, rotate upper body side to side with arms relaxed.", "warmup-torso-twists")
    private static let highKneesGentle = HIITPhaseStep("Gentle High Knees", "March in place, lifting knees to hip height at an easy pace.", "warmup-gentle-high-knees")
    private static let shoulderRolls = HIITPhaseStep("Shoulder Rolls", "Roll shoulders forward in big circles, then reverse.", "warmup-shoulder-rolls")
    private static let walkingLunges = HIITPhaseStep("Walking Lunges", "Step forward into a lunge, alternate legs, slow and controlled.", "warmup-walking-lunges")
    private static let catCow = HIITPhaseStep("Cat-Cow", "On all fours, alternate arching and rounding your back with your breath.", "warmup-cat-cow")
    private static let wristAnkleCircles = HIITPhaseStep("Wrist & Ankle Circles", "Rotate wrists and ankles in both directions to mobilize joints.", "warmup-wrist-ankle-circles")

    private static let standingQuadStretch = HIITPhaseStep("Standing Quad Stretch", "Stand on one leg, pull foot to glute, hold 15 seconds each side.", "cooldown-standing-quad-stretch")
    private static let standingHamstringStretch = HIITPhaseStep("Standing Hamstring Stretch", "Extend one leg forward on heel, hinge at hips until you feel a stretch.", "cooldown-standing-hamstring-stretch")
    private static let chestOpener = HIITPhaseStep("Chest Opener Stretch", "Clasp hands behind back, squeeze shoulder blades together, lift chest.", "cooldown-chest-opener")
    private static let crossBodyShoulder = HIITPhaseStep("Cross-Body Shoulder Stretch", "Pull one arm across your chest with the other hand, hold each side.", "cooldown-cross-body-shoulder")
    private static let childsPose = HIITPhaseStep("Child's Pose", "Kneel, sit back on heels, reach arms forward on the floor, breathe deeply.", "cooldown-childs-pose")
    private static let seatedSpinalTwist = HIITPhaseStep("Seated Spinal Twist", "Sit with legs extended, cross one leg over, twist toward the bent knee.", "cooldown-seated-spinal-twist")
    private static let hipFlexorStretch = HIITPhaseStep("Hip Flexor Stretch", "Kneel on one knee, push hips forward until you feel a stretch in front hip.", "cooldown-hip-flexor-stretch")
    private static let standingCalfStretch = HIITPhaseStep("Standing Calf Stretch", "Step one foot back, press heel down, lean into front leg.", "cooldown-standing-calf-stretch")
    private static let tricepStretch = HIITPhaseStep("Overhead Tricep Stretch", "Raise one arm overhead, bend elbow, gently push with other hand.", "cooldown-tricep-stretch")
    private static let deepBreathing = HIITPhaseStep("Deep Breathing", "Stand tall, inhale slowly through nose for 4 counts, exhale for 6 counts.", "cooldown-deep-breathing")
    private static let forwardFold = HIITPhaseStep("Standing Forward Fold", "Feet hip-width, hinge at hips, let head and arms hang, relax.", "cooldown-forward-fold")
    private static let figureFourStretch = HIITPhaseStep("Figure Four Stretch", "Lie on back, cross ankle over opposite knee, pull thigh toward chest.", "cooldown-figure-four-stretch")

    // MARK: - Workout Templates

    static let quickBurnExpress = HIITWorkoutTemplate(
        id: "quick_burn_express", name: "Quick Burn Express",
        templateDescription: "Short and effective full-body workout for when you're tight on time.",
        difficulty: "Easy-Medium",
        exercises: [HIITWorkoutExercise(exercise: highKnees), HIITWorkoutExercise(exercise: jumpSquats), HIITWorkoutExercise(exercise: bicycleCrunches), HIITWorkoutExercise(exercise: lateralLunges), HIITWorkoutExercise(exercise: plankJacks)],
        workDurationSec: 30, restDurationSec: 15, rounds: 3, warmupSec: 60, cooldownSec: 60,
        warmupSteps: [jumpingJacks, hipCircles, bodyweightSquats, highKneesGentle],
        cooldownSteps: [standingQuadStretch, standingHamstringStretch, forwardFold, deepBreathing]
    )

    static let upperBodyBlitz = HIITWorkoutTemplate(
        id: "upper_body_blitz", name: "Upper Body Blitz",
        templateDescription: "Intense upper body and pushing-focused HIIT session.",
        difficulty: "Medium-Hard",
        exercises: [HIITWorkoutExercise(exercise: pushupShoulderTap), HIITWorkoutExercise(exercise: diamondPushups), HIITWorkoutExercise(exercise: commandoPlank), HIITWorkoutExercise(exercise: pikePushups), HIITWorkoutExercise(exercise: bearCrawl), HIITWorkoutExercise(exercise: mountainClimbers)],
        workDurationSec: 40, restDurationSec: 20, rounds: 4, warmupSec: 90, cooldownSec: 90,
        warmupSteps: [armCircles, shoulderRolls, inchworms, jumpingJacks, wristAnkleCircles],
        cooldownSteps: [chestOpener, crossBodyShoulder, tricepStretch, childsPose, deepBreathing]
    )

    static let legDayInferno = HIITWorkoutTemplate(
        id: "leg_day_inferno", name: "Leg Day Inferno",
        templateDescription: "Lower body focused -- glutes, quads, and explosive power.",
        difficulty: "Medium",
        exercises: [HIITWorkoutExercise(exercise: jumpSquats), HIITWorkoutExercise(exercise: skaterJumps), HIITWorkoutExercise(exercise: lateralLunges), HIITWorkoutExercise(exercise: boxJumps), HIITWorkoutExercise(exercise: tuckJumps), HIITWorkoutExercise(exercise: highKnees)],
        workDurationSec: 40, restDurationSec: 20, rounds: 3, warmupSec: 90, cooldownSec: 90,
        warmupSteps: [highKneesGentle, legSwings, hipCircles, bodyweightSquats, walkingLunges],
        cooldownSteps: [standingQuadStretch, standingHamstringStretch, hipFlexorStretch, standingCalfStretch, deepBreathing]
    )

    static let coreCrusher = HIITWorkoutTemplate(
        id: "core_crusher", name: "Core Crusher",
        templateDescription: "Target every angle of your core with this focused ab workout.",
        difficulty: "Easy-Medium",
        exercises: [HIITWorkoutExercise(exercise: deadBug), HIITWorkoutExercise(exercise: bicycleCrunches), HIITWorkoutExercise(exercise: russianTwists), HIITWorkoutExercise(exercise: flutterKicks), HIITWorkoutExercise(exercise: mountainClimbers)],
        workDurationSec: 35, restDurationSec: 15, rounds: 4, warmupSec: 60, cooldownSec: 90,
        warmupSteps: [torsoTwists, catCow, hipCircles, highKneesGentle],
        cooldownSteps: [childsPose, seatedSpinalTwist, hipFlexorStretch, forwardFold, deepBreathing]
    )

    static let totalBodyDestroyer = HIITWorkoutTemplate(
        id: "total_body_destroyer", name: "Total Body Destroyer",
        templateDescription: "Maximum intensity full-body session for advanced athletes.",
        difficulty: "Hard",
        exercises: [HIITWorkoutExercise(exercise: burpees), HIITWorkoutExercise(exercise: manMakers), HIITWorkoutExercise(exercise: boxJumps), HIITWorkoutExercise(exercise: pikePushups), HIITWorkoutExercise(exercise: squatToPress), HIITWorkoutExercise(exercise: tuckJumps), HIITWorkoutExercise(exercise: commandoPlank), HIITWorkoutExercise(exercise: skaterJumps)],
        workDurationSec: 45, restDurationSec: 15, rounds: 4, warmupSec: 120, cooldownSec: 120,
        warmupSteps: [jumpingJacks, armCircles, legSwings, hipCircles, inchworms, bodyweightSquats],
        cooldownSteps: [standingQuadStretch, standingHamstringStretch, chestOpener, hipFlexorStretch, figureFourStretch, deepBreathing]
    )

    static let steadyStateBuilder = HIITWorkoutTemplate(
        id: "steady_state_builder", name: "Steady State Builder",
        templateDescription: "Beginner-friendly with equal work and rest for active recovery days.",
        difficulty: "Easy",
        exercises: [HIITWorkoutExercise(exercise: highKnees), HIITWorkoutExercise(exercise: deadBug), HIITWorkoutExercise(exercise: lateralLunges), HIITWorkoutExercise(exercise: plankJacks), HIITWorkoutExercise(exercise: russianTwists), HIITWorkoutExercise(exercise: flutterKicks), HIITWorkoutExercise(exercise: bearCrawl)],
        workDurationSec: 30, restDurationSec: 30, rounds: 3, warmupSec: 120, cooldownSec: 120,
        warmupSteps: [highKneesGentle, armCircles, hipCircles, legSwings, walkingLunges, catCow],
        cooldownSteps: [standingQuadStretch, standingHamstringStretch, crossBodyShoulder, childsPose, forwardFold, deepBreathing]
    )

    static let allTemplates: [HIITWorkoutTemplate] = [
        quickBurnExpress, upperBodyBlitz, legDayInferno, coreCrusher, totalBodyDestroyer, steadyStateBuilder
    ]

    static func getTemplateById(_ id: String) -> HIITWorkoutTemplate? {
        allTemplates.first { $0.id == id }
    }

    static func getExerciseById(_ id: String) -> HIITExercise? {
        allExercises.first { $0.id == id }
    }
}
