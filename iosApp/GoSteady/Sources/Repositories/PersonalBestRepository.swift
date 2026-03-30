import Foundation
import SwiftData

final class PersonalBestRepository {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    // MARK: - Fetch

    func fetchAll() -> [PersonalBest] {
        let descriptor = FetchDescriptor<PersonalBest>(sortBy: [SortDescriptor(\.distanceMeters)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchByDistance(_ distanceMeters: Int) -> PersonalBest? {
        let descriptor = FetchDescriptor<PersonalBest>(predicate: #Predicate { $0.distanceMeters == distanceMeters })
        return try? context.fetch(descriptor).first
    }

    func fetchById(_ id: UUID) -> PersonalBest? {
        let descriptor = FetchDescriptor<PersonalBest>(predicate: #Predicate { $0.id == id })
        return try? context.fetch(descriptor).first
    }

    // MARK: - Check and Update

    /// Check if a run sets any new personal bests and update accordingly.
    /// Returns a list of PB updates achieved.
    func checkAndUpdatePersonalBests(run: Run) -> [PersonalBestUpdate] {
        var updates: [PersonalBestUpdate] = []

        for distance in PersonalBest.standardDistances {
            guard run.distanceMeters >= Double(distance) else { continue }
            guard let timeForDistance = calculateTimeForDistance(run: run, distanceMeters: distance) else { continue }
            if let update = checkDistancePB(run: run, distanceMeters: distance, timeMillis: timeForDistance) {
                updates.append(update)
            }
        }
        return updates
    }

    // MARK: - Private Helpers

    private func checkDistancePB(run: Run, distanceMeters: Int, timeMillis: Int64) -> PersonalBestUpdate? {
        let existing = fetchByDistance(distanceMeters)
        let paceSecPerKm = (Double(timeMillis) / 1000.0) / (Double(distanceMeters) / 1000.0)

        var newFastest = false
        var newLowestHR = false

        if let pb = existing {
            // Check fastest time
            if pb.fastestTimeMillis == nil || timeMillis < (pb.fastestTimeMillis ?? Int64.max) {
                newFastest = true
                pb.fastestTimeMillis = timeMillis
                pb.fastestTimeRunId = run.id
                pb.fastestTimeDate = run.startTime
                pb.fastestTimePaceSecondsPerKm = paceSecPerKm
            }
            // Check lowest HR
            if let runHR = run.avgHeartRate {
                if pb.lowestAvgHeartRate == nil || runHR < (pb.lowestAvgHeartRate ?? Int.max) {
                    newLowestHR = true
                    pb.lowestAvgHeartRate = runHR
                    pb.lowestHrRunId = run.id
                    pb.lowestHrDate = run.startTime
                    pb.lowestHrTimeMillis = timeMillis
                }
            }
            if newFastest || newLowestHR {
                try? context.save()
            }
        } else {
            // First time
            newFastest = true
            if run.avgHeartRate != nil { newLowestHR = true }
            let pb = PersonalBest(
                distanceMeters: distanceMeters,
                fastestTimeMillis: timeMillis,
                fastestTimeRunId: run.id,
                fastestTimeDate: run.startTime,
                fastestTimePaceSecondsPerKm: paceSecPerKm,
                lowestAvgHeartRate: run.avgHeartRate,
                lowestHrRunId: run.avgHeartRate != nil ? run.id : nil,
                lowestHrDate: run.avgHeartRate != nil ? run.startTime : nil,
                lowestHrTimeMillis: run.avgHeartRate != nil ? timeMillis : nil
            )
            context.insert(pb)
            try? context.save()
        }

        guard newFastest || newLowestHR else { return nil }

        return PersonalBestUpdate(
            distanceMeters: distanceMeters,
            distanceName: PersonalBest.getDistanceName(distanceMeters),
            newFastestTime: newFastest,
            newLowestHr: newLowestHR,
            timeMillis: timeMillis,
            avgHeartRate: run.avgHeartRate,
            previousFastestTime: existing?.fastestTimeMillis,
            previousLowestHr: existing?.lowestAvgHeartRate
        )
    }

    private func calculateTimeForDistance(run: Run, distanceMeters: Int) -> Int64? {
        let distanceKm = distanceMeters / 1000

        // Use splits for accuracy if available
        if run.splits.count >= distanceKm {
            var totalTime: Int64 = 0
            for i in 0..<distanceKm {
                totalTime += run.splits[i].durationMillis
            }
            return totalTime
        }

        // Estimate from average pace
        if run.avgPaceSecondsPerKm > 0 {
            return Int64(run.avgPaceSecondsPerKm * Double(distanceKm) * 1000)
        }

        return nil
    }

    /// Get the split times for the PB run at a given distance.
    func personalBestSplits(distanceMeters: Int, runRepository: RunRepository) -> [Int64]? {
        guard let pb = fetchByDistance(distanceMeters),
              let runId = pb.fastestTimeRunId,
              let run = runRepository.fetchById(runId) else { return nil }
        return run.splits.map(\.durationMillis)
    }
}

// MARK: - Supporting Types

struct PersonalBestUpdate {
    let distanceMeters: Int
    let distanceName: String
    let newFastestTime: Bool
    let newLowestHr: Bool
    let timeMillis: Int64
    let avgHeartRate: Int?
    let previousFastestTime: Int64?
    let previousLowestHr: Int?

    var timeImprovement: Int64? {
        guard let prev = previousFastestTime else { return nil }
        return prev - timeMillis
    }

    var timeImprovementFormatted: String? {
        guard let improvement = timeImprovement else { return nil }
        let seconds = improvement / 1000
        let minutes = seconds / 60
        let secs = seconds % 60
        if minutes > 0 {
            return "-\(minutes)m \(secs)s"
        }
        return "-\(secs)s"
    }
}
