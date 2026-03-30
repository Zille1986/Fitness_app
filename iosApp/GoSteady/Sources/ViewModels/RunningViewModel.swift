import SwiftUI
import SwiftData
import Combine

// MARK: - Running ViewModel

@Observable
final class RunningViewModel {

    // MARK: - Published State

    var recentRuns: [RecentRunInfo] = []
    var weeklyDistances: [Float] = Array(repeating: 0, count: 7)
    var totalWeeklyDistance: Float = 0
    var averagePace: String = "--:--"
    var nextScheduledRun: ScheduledRunInfo?
    var personalBests: [PersonalBestInfo] = []
    var isLoading = true
    var errorMessage: String?
    var totalRunCount: Int = 0
    var totalDistanceKm: Double = 0

    // MARK: - Dependencies

    private var modelContext: ModelContext?

    // MARK: - Init

    init() {}

    func configure(modelContext: ModelContext) {
        self.modelContext = modelContext
        loadData()
    }

    // MARK: - Data Loading

    func loadData() {
        isLoading = true
        errorMessage = nil

        loadRecentRuns()
        loadWeeklyStats()
        loadPersonalBests()
        loadNextScheduledRun()

        isLoading = false
    }

    func refresh() {
        loadData()
    }

    private func loadRecentRuns() {
        guard let modelContext else { return }

        do {
            var descriptor = FetchDescriptor<Run>(
                predicate: #Predicate<Run> { $0.isCompleted },
                sortBy: [SortDescriptor(\Run.startTime, order: .reverse)]
            )
            descriptor.fetchLimit = 10

            let runs = try modelContext.fetch(descriptor)

            totalRunCount = runs.count

            let allDescriptor = FetchDescriptor<Run>(
                predicate: #Predicate<Run> { $0.isCompleted }
            )
            let allRuns = try modelContext.fetch(allDescriptor)
            totalDistanceKm = allRuns.reduce(0) { $0 + $1.distanceKm }

            recentRuns = runs.map { run in
                let formatter = DateFormatter()
                formatter.dateFormat = "MMM d"

                return RecentRunInfo(
                    id: run.id,
                    title: run.notes ?? "Run",
                    date: formatter.string(from: run.startTime),
                    distanceKm: run.distanceKm,
                    distanceFormatted: String(format: "%.1f", run.distanceKm),
                    paceFormatted: run.avgPaceFormatted,
                    durationFormatted: run.durationFormatted,
                    heartRate: run.avgHeartRate,
                    source: run.source
                )
            }
        } catch {
            errorMessage = "Failed to load runs: \(error.localizedDescription)"
        }
    }

    private func loadWeeklyStats() {
        guard let modelContext else { return }

        do {
            let calendar = Calendar.current
            let now = Date()
            guard let weekStart = calendar.date(from: calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: now)) else { return }
            guard let weekEnd = calendar.date(byAdding: .day, value: 7, to: weekStart) else { return }

            let descriptor = FetchDescriptor<Run>(
                predicate: #Predicate<Run> { run in
                    run.isCompleted && run.startTime >= weekStart && run.startTime < weekEnd
                }
            )

            let weeklyRuns = try modelContext.fetch(descriptor)

            var dailyDistances = Array(repeating: Float(0), count: 7)
            for run in weeklyRuns {
                let dayOfWeek = calendar.component(.weekday, from: run.startTime)
                // Convert to Monday-based index (Monday=0, Sunday=6)
                let dayIndex = dayOfWeek == 1 ? 6 : dayOfWeek - 2
                dailyDistances[dayIndex] += Float(run.distanceKm)
            }

            weeklyDistances = dailyDistances
            totalWeeklyDistance = dailyDistances.reduce(0, +)

            // Calculate average pace
            let totalDurationMs = weeklyRuns.reduce(Int64(0)) { $0 + $1.durationMillis }
            let totalDistanceMeters = weeklyRuns.reduce(0.0) { $0 + $1.distanceMeters }

            if totalDistanceMeters > 0 {
                let paceMinutes = (Double(totalDurationMs) / 1000.0 / 60.0) / (totalDistanceMeters / 1000.0)
                let mins = Int(paceMinutes)
                let secs = Int((paceMinutes - Double(mins)) * 60)
                averagePace = String(format: "%d:%02d", mins, secs)
            } else {
                averagePace = "--:--"
            }
        } catch {
            errorMessage = "Failed to load weekly stats: \(error.localizedDescription)"
        }
    }

    private func loadPersonalBests() {
        guard let modelContext else { return }

        do {
            let descriptor = FetchDescriptor<Run>(
                predicate: #Predicate<Run> { $0.isCompleted },
                sortBy: [SortDescriptor(\Run.startTime, order: .reverse)]
            )
            let runs = try modelContext.fetch(descriptor)

            var bests: [PersonalBestInfo] = []

            // Fastest 1K
            let fastestPace = runs.filter { $0.distanceMeters >= 1000 }
                .min(by: { $0.avgPaceSecondsPerKm < $1.avgPaceSecondsPerKm })
            if let pb = fastestPace {
                bests.append(PersonalBestInfo(
                    category: "Fastest Pace",
                    value: pb.avgPaceFormatted + " /km",
                    date: pb.startTime,
                    icon: "bolt.fill"
                ))
            }

            // Longest run
            let longestRun = runs.max(by: { $0.distanceMeters < $1.distanceMeters })
            if let pb = longestRun {
                bests.append(PersonalBestInfo(
                    category: "Longest Run",
                    value: String(format: "%.1f km", pb.distanceKm),
                    date: pb.startTime,
                    icon: "ruler"
                ))
            }

            // Longest duration
            let longestDuration = runs.max(by: { $0.durationMillis < $1.durationMillis })
            if let pb = longestDuration {
                bests.append(PersonalBestInfo(
                    category: "Longest Duration",
                    value: pb.durationFormatted,
                    date: pb.startTime,
                    icon: "timer"
                ))
            }

            // Best 5K
            let fiveKRuns = runs.filter { $0.distanceMeters >= 5000 }
            if let best5K = fiveKRuns.min(by: {
                let pace0 = Double($0.durationMillis) / ($0.distanceMeters / 5000.0)
                let pace1 = Double($1.durationMillis) / ($1.distanceMeters / 5000.0)
                return pace0 < pace1
            }) {
                let estimatedTime = Double(best5K.durationMillis) / 1000.0 * (5000.0 / best5K.distanceMeters)
                let mins = Int(estimatedTime) / 60
                let secs = Int(estimatedTime) % 60
                bests.append(PersonalBestInfo(
                    category: "Best 5K",
                    value: String(format: "%d:%02d", mins, secs),
                    date: best5K.startTime,
                    icon: "star.fill"
                ))
            }

            // Best 10K
            let tenKRuns = runs.filter { $0.distanceMeters >= 10000 }
            if let best10K = tenKRuns.min(by: {
                let pace0 = Double($0.durationMillis) / ($0.distanceMeters / 10000.0)
                let pace1 = Double($1.durationMillis) / ($1.distanceMeters / 10000.0)
                return pace0 < pace1
            }) {
                let estimatedTime = Double(best10K.durationMillis) / 1000.0 * (10000.0 / best10K.distanceMeters)
                let mins = Int(estimatedTime) / 60
                let secs = Int(estimatedTime) % 60
                bests.append(PersonalBestInfo(
                    category: "Best 10K",
                    value: String(format: "%d:%02d", mins, secs),
                    date: best10K.startTime,
                    icon: "star.fill"
                ))
            }

            personalBests = bests
        } catch {
            // Silently ignore PB errors
        }
    }

    private func loadNextScheduledRun() {
        guard let modelContext else { return }

        do {
            let descriptor = FetchDescriptor<TrainingPlan>(
                predicate: #Predicate<TrainingPlan> { $0.isActive },
                sortBy: [SortDescriptor(\TrainingPlan.startDate)]
            )

            let plans = try modelContext.fetch(descriptor)
            guard let activePlan = plans.first else {
                nextScheduledRun = nil
                return
            }

            let calendar = Calendar.current
            let today = calendar.startOfDay(for: Date())
            let currentWeekOfYear = calendar.component(.weekOfYear, from: today)
            let planStartWeek = calendar.component(.weekOfYear, from: activePlan.startDate)
            let currentWeekNumber = currentWeekOfYear - planStartWeek + 1

            let upcoming = activePlan.weeklySchedule
                .filter { !$0.isCompleted && $0.weekNumber >= max(1, currentWeekNumber) }
                .sorted { workout0, workout1 in
                    if workout0.weekNumber != workout1.weekNumber {
                        return workout0.weekNumber < workout1.weekNumber
                    }
                    return workout0.dayOfWeek < workout1.dayOfWeek
                }

            if let next = upcoming.first {
                let daysAhead = (next.weekNumber - currentWeekNumber) * 7 + (next.dayOfWeek - calendar.component(.weekday, from: today))
                let scheduledDateText: String
                switch daysAhead {
                case 0: scheduledDateText = "Today"
                case 1: scheduledDateText = "Tomorrow"
                default: scheduledDateText = "In \(max(0, daysAhead)) days"
                }

                let workoutName = next.workoutType.rawValue
                    .replacingOccurrences(of: "_", with: " ")
                    .capitalized

                nextScheduledRun = ScheduledRunInfo(
                    name: workoutName,
                    description: next.workoutDescription,
                    scheduledDate: scheduledDateText,
                    estimatedDuration: next.targetDurationMinutes.map { "\($0) min" } ?? ""
                )
            } else {
                nextScheduledRun = nil
            }
        } catch {
            nextScheduledRun = nil
        }
    }

    // MARK: - Manual Entry

    func saveManualRun(distanceMeters: Double, durationMillis: Int64, notes: String?) {
        guard let modelContext else { return }

        let now = Date()
        let paceSecondsPerKm: Double = distanceMeters > 0
            ? (Double(durationMillis) / 1000.0) / (distanceMeters / 1000.0)
            : 0.0

        let run = Run(
            startTime: now.addingTimeInterval(-Double(durationMillis) / 1000.0),
            endTime: now,
            distanceMeters: distanceMeters,
            durationMillis: durationMillis,
            avgPaceSecondsPerKm: paceSecondsPerKm,
            maxPaceSecondsPerKm: paceSecondsPerKm,
            notes: notes,
            source: .manual,
            isCompleted: true
        )

        modelContext.insert(run)

        do {
            try modelContext.save()
            loadData()
        } catch {
            errorMessage = "Failed to save run: \(error.localizedDescription)"
        }
    }

    func deleteRun(_ runId: UUID) {
        guard let modelContext else { return }

        do {
            let descriptor = FetchDescriptor<Run>(
                predicate: #Predicate<Run> { run in run.id == runId }
            )
            if let run = try modelContext.fetch(descriptor).first {
                modelContext.delete(run)
                try modelContext.save()
                loadData()
            }
        } catch {
            errorMessage = "Failed to delete run: \(error.localizedDescription)"
        }
    }
}

// MARK: - Data Models

struct RecentRunInfo: Identifiable {
    let id: UUID
    let title: String
    let date: String
    let distanceKm: Double
    let distanceFormatted: String
    let paceFormatted: String
    let durationFormatted: String
    let heartRate: Int?
    let source: RunSource
}

struct ScheduledRunInfo {
    let name: String
    let description: String
    let scheduledDate: String
    let estimatedDuration: String
}

struct PersonalBestInfo: Identifiable {
    let id = UUID()
    let category: String
    let value: String
    let date: Date
    let icon: String
}
