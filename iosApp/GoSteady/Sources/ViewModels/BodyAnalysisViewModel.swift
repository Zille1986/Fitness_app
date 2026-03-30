import Foundation
import Observation
import UIKit

// MARK: - ViewModel-specific Models

struct BodyScanResult: Identifiable {
    let id: Int
    let timestamp: Date
    let photoData: Data?
    let goal: FitnessGoal
    let bodyType: BodyType
    let estimatedBodyFatPercentage: Float?
    let focusZones: [BodyZone]
    let overallScore: Int
    let muscleBalance: MuscleBalanceAssessment
    let postureAssessment: PostureAssessment
    let notes: String?
}

struct BodyMeasurementPoint: Identifiable {
    let id = UUID()
    let date: Date
    let weight: Double?
    let bodyFatPercent: Float?
    let chest: Double?
    let waist: Double?
    let hips: Double?
    let bicep: Double?
    let thigh: Double?
}

enum BodyAnalysisPhase {
    case goalSelection, photoCapture, analyzing, results
}

// MARK: - ViewModel

@Observable
final class BodyAnalysisViewModel {
    var analysisPhase: BodyAnalysisPhase = .goalSelection
    var selectedGoal: FitnessGoal = .generalFitness
    var capturedImage: UIImage?
    var analysisProgress: Double = 0
    var analysisStatus: String = ""
    var currentScan: BodyScanResult?
    var scanHistory: [BodyScanResult] = []
    var measurementHistory: [BodyMeasurementPoint] = []
    var scoreHistory: [(date: Date, score: Int)] = []
    var showHistorySheet: Bool = false
    var errorMessage: String?
    var selectedBeforeIndex: Int = 0
    var selectedAfterIndex: Int = 0

    init() {
        loadHistory()
    }

    func setGoal(_ goal: FitnessGoal) {
        selectedGoal = goal
    }

    func proceedToCapture() {
        analysisPhase = .photoCapture
    }

    func setCapturedImage(_ image: UIImage) {
        capturedImage = image
    }

    @MainActor
    func startAnalysis() async {
        guard capturedImage != nil else {
            errorMessage = "No photo captured"
            return
        }
        analysisPhase = .analyzing
        analysisProgress = 0

        let steps: [(Double, String)] = [
            (0.1, "Loading image..."),
            (0.3, "Analyzing body composition..."),
            (0.5, "Assessing muscle groups..."),
            (0.7, "Evaluating posture..."),
            (0.9, "Generating recommendations..."),
            (1.0, "Complete!"),
        ]

        for (progress, status) in steps {
            try? await Task.sleep(for: .milliseconds(500))
            analysisProgress = progress
            analysisStatus = status
        }

        try? await Task.sleep(for: .milliseconds(300))

        let scan = generateScan()
        currentScan = scan
        scanHistory.insert(scan, at: 0)
        scoreHistory.insert((date: scan.timestamp, score: scan.overallScore), at: 0)
        analysisPhase = .results
    }

    func resetToCapture() {
        analysisPhase = .goalSelection
        capturedImage = nil
        currentScan = nil
        analysisProgress = 0
        analysisStatus = ""
    }

    func viewScanDetails(_ scanId: Int) {
        guard let scan = scanHistory.first(where: { $0.id == scanId }) else { return }
        currentScan = scan
        analysisPhase = .results
    }

    func toggleHistorySheet() {
        showHistorySheet.toggle()
    }

    func deleteScan(_ scanId: Int) {
        scanHistory.removeAll { $0.id == scanId }
        scoreHistory = scanHistory.map { ($0.timestamp, $0.overallScore) }
    }

    // MARK: - Private

    private func loadHistory() {
        let calendar = Calendar.current
        scanHistory = (1...5).map { i in
            let date = calendar.date(byAdding: .day, value: -i * 14, to: .now)!
            let score = Int.random(in: 55...90)
            return BodyScanResult(
                id: i, timestamp: date, photoData: nil,
                goal: .generalFitness,
                bodyType: BodyType.allCases.randomElement()!,
                estimatedBodyFatPercentage: Float.random(in: 12...28),
                focusZones: Array(BodyZone.allCases.shuffled().prefix(3)),
                overallScore: score,
                muscleBalance: MuscleBalanceAssessment(
                    overallBalance: .balanced,
                    leftRightSymmetry: .slightImbalance,
                    frontBackBalance: .balanced,
                    upperLowerBalance: .slightImbalance,
                    imbalances: []
                ),
                postureAssessment: PostureAssessment(
                    overallPosture: .good,
                    issues: [
                        PostureIssue(type: .roundedShoulders, severity: .mild,
                                     issueDescription: "Slight rounding of shoulders",
                                     exercises: ["Wall Angels", "Band Pull-Aparts"])
                    ]
                ),
                notes: nil
            )
        }
        scoreHistory = scanHistory.map { ($0.timestamp, $0.overallScore) }

        measurementHistory = (0..<10).map { i in
            let date = calendar.date(byAdding: .weekOfYear, value: -i, to: .now)!
            return BodyMeasurementPoint(
                date: date,
                weight: 78.5 - Double(i) * 0.3,
                bodyFatPercent: 18.5 - Float(i) * 0.2,
                chest: 102 - Double(i) * 0.1,
                waist: 84 - Double(i) * 0.3,
                hips: 98 - Double(i) * 0.1,
                bicep: 35 + Double(i) * 0.1,
                thigh: 58 + Double(i) * 0.1
            )
        }.reversed()
    }

    private func generateScan() -> BodyScanResult {
        let bodyType = BodyType.allCases.randomElement()!
        let bf = Float.random(in: 12...28)
        let zones = Array(BodyZone.allCases.shuffled().prefix(Int.random(in: 2...5)))
        let score = Int.random(in: 55...95)

        let postureIssues: [PostureIssue] = Bool.random() ? [
            PostureIssue(
                type: PostureIssueType.allCases.randomElement() ?? .roundedShoulders,
                severity: IssueSeverity.allCases.randomElement() ?? .mild,
                issueDescription: "Detected slight misalignment",
                exercises: ["Wall Angels", "Chin Tucks", "Cat-Cow Stretch"]
            )
        ] : []

        let postureLevel: PostureLevel = postureIssues.isEmpty ? .good :
            postureIssues.contains(where: { $0.severity == .severe }) ? .poor : .fair

        return BodyScanResult(
            id: (scanHistory.map(\.id).max() ?? 0) + 1,
            timestamp: .now,
            photoData: capturedImage?.jpegData(compressionQuality: 0.8),
            goal: selectedGoal,
            bodyType: bodyType,
            estimatedBodyFatPercentage: bf,
            focusZones: zones,
            overallScore: score,
            muscleBalance: MuscleBalanceAssessment(
                overallBalance: BalanceLevel.allCases.randomElement()!,
                leftRightSymmetry: BalanceLevel.allCases.randomElement()!,
                frontBackBalance: BalanceLevel.allCases.randomElement()!,
                upperLowerBalance: BalanceLevel.allCases.randomElement()!,
                imbalances: Bool.random() ? [
                    MuscleImbalance(
                        imbalanceDescription: "Slight left-right asymmetry in shoulders",
                        affectedZones: [.shoulders],
                        correction: "Include unilateral exercises"
                    )
                ] : []
            ),
            postureAssessment: PostureAssessment(
                overallPosture: postureLevel,
                issues: postureIssues
            ),
            notes: nil
        )
    }
}

