import Foundation
import SwiftData

final class BodyAnalysisRepository {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    // MARK: - Scans

    func fetchAllScans() -> [BodyScan] {
        let descriptor = FetchDescriptor<BodyScan>(sortBy: [SortDescriptor(\.timestamp, order: .reverse)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchLatestScan() -> BodyScan? {
        var descriptor = FetchDescriptor<BodyScan>(sortBy: [SortDescriptor(\.timestamp, order: .reverse)])
        descriptor.fetchLimit = 1
        return try? context.fetch(descriptor).first
    }

    func fetchPreviousScan() -> BodyScan? {
        var descriptor = FetchDescriptor<BodyScan>(sortBy: [SortDescriptor(\.timestamp, order: .reverse)])
        descriptor.fetchLimit = 2
        let scans = (try? context.fetch(descriptor)) ?? []
        return scans.count >= 2 ? scans[1] : nil
    }

    func fetchScanById(_ id: UUID) -> BodyScan? {
        let descriptor = FetchDescriptor<BodyScan>(predicate: #Predicate { $0.id == id })
        return try? context.fetch(descriptor).first
    }

    func saveScan(_ scan: BodyScan) {
        context.insert(scan)
        try? context.save()
    }

    func deleteScan(_ scan: BodyScan) {
        context.delete(scan)
        try? context.save()
    }

    func scanCount() -> Int {
        let descriptor = FetchDescriptor<BodyScan>()
        return (try? context.fetchCount(descriptor)) ?? 0
    }

    // MARK: - Comparison

    func compareToPrevious(currentScan: BodyScan) -> RepoBodyScanComparison {
        let allScans = fetchAllScans()
        let previousScan = allScans.first { $0.timestamp < currentScan.timestamp }

        let daysBetween: Int
        if let prev = previousScan {
            daysBetween = Calendar.current.dateComponents([.day], from: prev.timestamp, to: currentScan.timestamp).day ?? 0
        } else {
            daysBetween = 0
        }

        let scoreChange = previousScan != nil ? currentScan.overallScore - (previousScan?.overallScore ?? 0) : 0

        let bodyFatChange: Float?
        if let currentBF = currentScan.estimatedBodyFatPercentage, let prevBF = previousScan?.estimatedBodyFatPercentage {
            bodyFatChange = currentBF - prevBF
        } else {
            bodyFatChange = nil
        }

        let previousFocusZones = Set(previousScan?.focusZones ?? [])
        let currentFocusZones = Set(currentScan.focusZones)
        let improvedZones = Array(previousFocusZones.subtracting(currentFocusZones))
        let needsMoreWork = Array(currentFocusZones)

        let postureImprovement: Bool
        if let prevPosture = previousScan?.postureAssessment, currentScan.postureAssessment.issues.count < prevPosture.issues.count {
            postureImprovement = true
        } else {
            postureImprovement = false
        }

        let progress: ProgressAssessment
        if previousScan == nil {
            progress = .firstScan
        } else if scoreChange > 5 {
            progress = .excellent
        } else if scoreChange > 2 {
            progress = .good
        } else if scoreChange > 0 {
            progress = .steady
        } else if scoreChange == 0 {
            progress = .plateau
        } else {
            progress = .slow
        }

        return RepoBodyScanComparison(
            currentScan: currentScan,
            previousScan: previousScan,
            daysBetween: daysBetween,
            scoreChange: scoreChange,
            bodyFatChange: bodyFatChange,
            improvedZones: improvedZones,
            needsMoreWorkZones: needsMoreWork,
            postureImprovement: postureImprovement,
            overallProgress: progress
        )
    }

    // MARK: - Progress Summary

    func progressSummary() -> RepoBodyProgressSummary {
        let scans = fetchAllScans()
        let scores = scans.map(\.overallScore)
        let avgScore = scores.isEmpty ? 0 : Float(scores.reduce(0, +)) / Float(scores.count)
        let bestScore = scores.max() ?? 0
        let scoreImprovement = scans.count >= 2 ? scans.first!.overallScore - scans.last!.overallScore : 0

        return RepoBodyProgressSummary(
            totalScans: scans.count,
            firstScanDate: scans.last?.timestamp,
            latestScanDate: scans.first?.timestamp,
            averageScore: avgScore,
            bestScore: bestScore,
            scoreImprovement: scoreImprovement
        )
    }
}

// MARK: - Supporting Types (local to repository, uses SwiftData models directly)

struct RepoBodyScanComparison {
    let currentScan: BodyScan
    let previousScan: BodyScan?
    let daysBetween: Int
    let scoreChange: Int
    let bodyFatChange: Float?
    let improvedZones: [BodyZone]
    let needsMoreWorkZones: [BodyZone]
    let postureImprovement: Bool
    let overallProgress: ProgressAssessment
}

struct RepoBodyProgressSummary {
    let totalScans: Int
    let firstScanDate: Date?
    let latestScanDate: Date?
    let averageScore: Float
    let bestScore: Int
    let scoreImprovement: Int
}
