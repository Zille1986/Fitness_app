import Foundation
import Observation
import AVFoundation
import UIKit

// MARK: - Models

enum FormAnalysisMode: String, CaseIterable {
    case autoDetect = "AI Auto-Detect"
    case running = "Running"
    case gym = "Gym"
}

enum FormAnalysisPhase {
    case idle, countdown, collecting, analyzing, results
}

enum FormDataQuality: String {
    case insufficient = "Need more data"
    case minimal = "Minimal"
    case good = "Good"
    case excellent = "Excellent"
}

enum FormIssueDisplaySeverity: String, CaseIterable {
    case low, medium, high

    var displayName: String { rawValue.capitalized }

    var color: String {
        switch self {
        case .low: return "FFEB3B"
        case .medium: return "FF9800"
        case .high: return "F44336"
        }
    }
}

struct FormIssueDisplay: Identifiable {
    let id = UUID()
    let bodyPart: String
    let description: String
    let severity: FormIssueDisplaySeverity
    let suggestion: String
}

struct FormAnalysisDisplay: Identifiable {
    let id = UUID()
    let exerciseName: String
    let overallScore: Int
    let issues: [FormIssueDisplay]
    let tips: [String]
    let timestamp: Date
    var summary: String

    var scoreColor: String {
        switch overallScore {
        case 80...100: return "4CAF50"
        case 60..<80: return "FF9800"
        default: return "F44336"
        }
    }
}

struct FormHistoryEntry: Identifiable {
    let id = UUID()
    let exerciseName: String
    let score: Int
    let issueCount: Int
    let date: Date
}

enum GymExerciseType: String, CaseIterable, Identifiable {
    case squat = "Squat"
    case deadlift = "Deadlift"
    case benchPress = "Bench Press"
    case overheadPress = "Overhead Press"
    case pushUp = "Push Up"
    case lunge = "Lunge"
    case plank = "Plank"
    case bicepCurl = "Bicep Curl"
    case other = "Other"

    var id: String { rawValue }
    var displayName: String { rawValue }
}

// MARK: - ViewModel

@Observable
final class FormAnalysisViewModel {
    var mode: FormAnalysisMode = .autoDetect
    var selectedExercise: GymExerciseType = .squat
    var analysisPhase: FormAnalysisPhase = .idle
    var countdownSeconds: Int = 5
    var framesCollected: Int = 0
    var repsDetected: Int = 0
    var dataQuality: FormDataQuality = .insufficient
    var isAnalyzing: Bool = false
    var isCameraActive: Bool = false
    var detectedExerciseName: String?
    var currentResult: FormAnalysisDisplay?
    var analysisHistory: [FormHistoryEntry] = []
    var errorMessage: String?
    var analysisProgress: Double = 0

    private var countdownTask: Task<Void, Never>?
    private var collectionTask: Task<Void, Never>?

    init() {
        loadHistory()
    }

    func setMode(_ mode: FormAnalysisMode) {
        self.mode = mode
        resetAnalysis()
    }

    func setExercise(_ exercise: GymExerciseType) {
        selectedExercise = exercise
    }

    @MainActor
    func startAnalysis() {
        resetAnalysis()
        isCameraActive = true
        analysisPhase = .countdown
        countdownSeconds = 5

        countdownTask = Task {
            for i in stride(from: 5, through: 1, by: -1) {
                guard !Task.isCancelled else { return }
                countdownSeconds = i
                try? await Task.sleep(for: .seconds(1))
            }
            guard !Task.isCancelled else { return }
            countdownSeconds = 0
            analysisPhase = .collecting
            isAnalyzing = true
            startCollecting()
        }
    }

    @MainActor
    func stopAnalysis() {
        countdownTask?.cancel()
        collectionTask?.cancel()
        isAnalyzing = false
        isCameraActive = false
        analysisPhase = .idle
    }

    @MainActor
    private func startCollecting() {
        collectionTask = Task {
            for frame in 0..<80 {
                guard !Task.isCancelled else { return }
                try? await Task.sleep(for: .milliseconds(100))
                framesCollected = frame + 1
                if mode == .gym {
                    repsDetected = frame / 16
                }
                dataQuality = switch framesCollected {
                case 0..<30: .insufficient
                case 30..<50: .minimal
                case 50..<70: .good
                default: .excellent
                }
            }
            guard !Task.isCancelled else { return }
            await captureAndAnalyze()
        }
    }

    @MainActor
    func captureAndAnalyze() async {
        isAnalyzing = false
        analysisPhase = .analyzing
        analysisProgress = 0

        let exerciseName: String
        switch mode {
        case .autoDetect:
            exerciseName = ["Squat", "Push Up", "Deadlift", "Lunge"].randomElement()!
            detectedExerciseName = exerciseName
        case .running:
            exerciseName = "Running"
        case .gym:
            exerciseName = selectedExercise.displayName
        }

        for step in 1...5 {
            try? await Task.sleep(for: .milliseconds(400))
            analysisProgress = Double(step) / 5.0
        }

        let issues = generateIssues(for: exerciseName)
        let score = max(100 - issues.reduce(0) { sum, issue in
            sum + (issue.severity == .high ? 20 : issue.severity == .medium ? 12 : 5)
        }, 0)

        currentResult = FormAnalysisDisplay(
            exerciseName: exerciseName,
            overallScore: score,
            issues: issues,
            tips: generateTips(for: exerciseName),
            timestamp: .now,
            summary: generateSummary(score: score, exerciseName: exerciseName)
        )

        let entry = FormHistoryEntry(
            exerciseName: exerciseName, score: score,
            issueCount: issues.count, date: .now
        )
        analysisHistory.insert(entry, at: 0)
        analysisPhase = .results
        isCameraActive = false
    }

    @MainActor
    func runDemoAnalysis() {
        Task { await captureAndAnalyze() }
    }

    func resetAnalysis() {
        countdownTask?.cancel()
        collectionTask?.cancel()
        analysisPhase = .idle
        framesCollected = 0
        repsDetected = 0
        dataQuality = .insufficient
        isAnalyzing = false
        isCameraActive = false
        currentResult = nil
        detectedExerciseName = nil
        analysisProgress = 0
    }

    private func loadHistory() {
        analysisHistory = [
            FormHistoryEntry(exerciseName: "Squat", score: 78, issueCount: 2, date: Calendar.current.date(byAdding: .day, value: -1, to: .now)!),
            FormHistoryEntry(exerciseName: "Push Up", score: 85, issueCount: 1, date: Calendar.current.date(byAdding: .day, value: -3, to: .now)!),
            FormHistoryEntry(exerciseName: "Deadlift", score: 62, issueCount: 3, date: Calendar.current.date(byAdding: .day, value: -5, to: .now)!),
            FormHistoryEntry(exerciseName: "Running", score: 91, issueCount: 1, date: Calendar.current.date(byAdding: .day, value: -7, to: .now)!),
        ]
    }

    private func generateIssues(for exercise: String) -> [FormIssueDisplay] {
        let issuePool: [(String, String, FormIssueDisplaySeverity, String)] = switch exercise {
        case "Squat":
            [
                ("Knees", "Knees caving inward during descent", .high, "Focus on pushing knees out over toes. Engage glutes."),
                ("Back", "Slight rounding of lower back at bottom position", .medium, "Maintain neutral spine. Brace core before descending."),
                ("Depth", "Not reaching parallel depth consistently", .low, "Work on hip mobility. Use box squats to train depth."),
            ]
        case "Deadlift":
            [
                ("Back", "Lower back rounding during lift-off", .high, "Set up with chest up, lats engaged. Pull slack out of bar first."),
                ("Hips", "Hips rising faster than shoulders", .medium, "Think of pushing the floor away. Keep hips and shoulders rising together."),
                ("Bar Path", "Bar drifting away from body", .medium, "Keep the bar in contact with your legs throughout the lift."),
            ]
        case "Push Up":
            [
                ("Hips", "Hips sagging below shoulder-ankle line", .medium, "Engage core and glutes. Maintain plank position throughout."),
                ("Elbows", "Elbows flaring out at 90 degrees", .low, "Keep elbows at 45-degree angle to torso."),
            ]
        case "Running":
            [
                ("Cadence", "Cadence below optimal range", .low, "Aim for 170-180 steps per minute. Use a metronome app."),
                ("Shoulders", "Tension in shoulders causing elevation", .medium, "Relax shoulders down and back. Shake arms periodically."),
            ]
        case "Lunge":
            [
                ("Knee", "Front knee extending past toes", .medium, "Take a longer step forward. Keep shin vertical."),
                ("Balance", "Lateral wobble during movement", .low, "Strengthen hip stabilizers. Practice single-leg balance drills."),
            ]
        default:
            [
                ("Posture", "General posture could be improved", .low, "Focus on maintaining neutral spine alignment."),
            ]
        }
        let count = min(issuePool.count, Int.random(in: 1...issuePool.count))
        return issuePool.prefix(count).map {
            FormIssueDisplay(bodyPart: $0.0, description: $0.1, severity: $0.2, suggestion: $0.3)
        }
    }

    private func generateTips(for exercise: String) -> [String] {
        switch exercise {
        case "Squat":
            return [
                "Warm up with bodyweight squats before loading the bar",
                "Film from a 45-degree angle for best form review",
                "Use a tempo of 3 seconds down, 1 second up for control",
                "Breathe in at the top, brace, then descend"
            ]
        case "Deadlift":
            return [
                "Start with the bar over mid-foot",
                "Take the slack out of the bar before pulling",
                "Drive through the floor, do not yank the bar",
                "Lock out with glutes, not by hyperextending the back"
            ]
        case "Push Up":
            return [
                "Hands should be slightly wider than shoulder width",
                "Lower your chest to just above the floor",
                "Fully extend arms at the top without locking elbows",
                "Keep your body in a straight line from head to heels"
            ]
        case "Running":
            return [
                "Land with your foot under your center of mass",
                "Keep a slight forward lean from the ankles",
                "Swing arms forward and back, not across your body",
                "Focus on quick, light ground contact"
            ]
        default:
            return [
                "Maintain proper breathing throughout the movement",
                "Control the eccentric (lowering) phase",
                "Focus on mind-muscle connection"
            ]
        }
    }

    private func generateSummary(score: Int, exerciseName: String) -> String {
        switch score {
        case 90...100:
            return "Excellent \(exerciseName) form! Minor refinements will take you to perfection."
        case 75..<90:
            return "Good \(exerciseName) form with a few areas to address for safer, more effective reps."
        case 60..<75:
            return "Decent \(exerciseName) technique, but several form issues should be corrected to prevent injury."
        default:
            return "Your \(exerciseName) form needs significant work. Consider reducing weight and focusing on technique."
        }
    }
}
