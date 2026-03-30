import Foundation
import SwiftData

final class RunRepository {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    // MARK: - Fetch

    func fetchAll(sortedBy sortOrder: SortOrder = .reverse) -> [Run] {
        let descriptor = FetchDescriptor<Run>(sortBy: [SortDescriptor(\.startTime, order: sortOrder)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchRecent(limit: Int = 10) -> [Run] {
        var descriptor = FetchDescriptor<Run>(sortBy: [SortDescriptor(\.startTime, order: .reverse)])
        descriptor.fetchLimit = limit
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchById(_ id: UUID) -> Run? {
        let descriptor = FetchDescriptor<Run>(predicate: #Predicate { $0.id == id })
        return try? context.fetch(descriptor).first
    }

    func fetchByStravaId(_ stravaId: String) -> Run? {
        let descriptor = FetchDescriptor<Run>(predicate: #Predicate { $0.stravaId == stravaId })
        return try? context.fetch(descriptor).first
    }

    func fetchInRange(start: Date, end: Date) -> [Run] {
        let descriptor = FetchDescriptor<Run>(
            predicate: #Predicate { $0.startTime >= start && $0.startTime <= end },
            sortBy: [SortDescriptor(\.startTime, order: .reverse)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchCompleted() -> [Run] {
        let descriptor = FetchDescriptor<Run>(
            predicate: #Predicate { $0.isCompleted == true },
            sortBy: [SortDescriptor(\.startTime, order: .reverse)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchActive() -> Run? {
        let descriptor = FetchDescriptor<Run>(predicate: #Predicate { $0.isCompleted == false })
        return try? context.fetch(descriptor).first
    }

    func fetchThisWeekRuns() -> [Run] {
        let range = Date.thisWeekRange
        return fetchInRange(start: range.start, end: range.end).filter(\.isCompleted)
    }

    func fetchSince(_ date: Date) -> [Run] {
        let now = Date()
        return fetchInRange(start: date, end: now)
    }

    // MARK: - Create / Update / Delete

    func insert(_ run: Run) {
        context.insert(run)
        try? context.save()
    }

    func update(_ run: Run) {
        try? context.save()
    }

    func delete(_ run: Run) {
        context.delete(run)
        try? context.save()
    }

    // MARK: - Personal Bests

    func fastestRun(forDistanceKm km: Double, toleranceKm: Double = 0.5) -> Run? {
        let minMeters = (km - toleranceKm) * 1000
        let maxMeters = (km + toleranceKm) * 1000
        let descriptor = FetchDescriptor<Run>(
            predicate: #Predicate {
                $0.isCompleted == true &&
                $0.distanceMeters >= minMeters &&
                $0.distanceMeters <= maxMeters
            },
            sortBy: [SortDescriptor(\.durationMillis, order: .forward)]
        )
        return try? context.fetch(descriptor).first
    }

    func longestRun() -> Run? {
        var descriptor = FetchDescriptor<Run>(
            predicate: #Predicate { $0.isCompleted == true },
            sortBy: [SortDescriptor(\.distanceMeters, order: .reverse)]
        )
        descriptor.fetchLimit = 1
        return try? context.fetch(descriptor).first
    }

    // MARK: - Stats

    func weeklyStats(weekStartDate: Date) -> WeeklyRunStats {
        let weekEnd = Calendar.current.date(byAdding: .day, value: 7, to: weekStartDate) ?? weekStartDate
        let runs = fetchInRange(start: weekStartDate, end: weekEnd).filter(\.isCompleted)

        let totalDistance = runs.reduce(0.0) { $0 + $1.distanceMeters }
        let totalDuration = runs.reduce(Int64(0)) { $0 + $1.durationMillis }
        let totalElevation = runs.reduce(0.0) { $0 + $1.elevationGainMeters }
        let totalCalories = runs.reduce(0) { $0 + $1.caloriesBurned }
        let avgPace = totalDistance > 0 ? (Double(totalDuration) / 1000.0) / (totalDistance / 1000.0) : 0
        let longestRun = runs.max(by: { $0.distanceMeters < $1.distanceMeters })?.distanceMeters ?? 0
        let fastestPace = runs.filter { $0.avgPaceSecondsPerKm > 0 }.min(by: { $0.avgPaceSecondsPerKm < $1.avgPaceSecondsPerKm })?.avgPaceSecondsPerKm ?? 0

        return WeeklyRunStats(
            weekStartDate: weekStartDate,
            totalDistanceMeters: totalDistance,
            totalDurationMillis: totalDuration,
            totalRuns: runs.count,
            avgPaceSecondsPerKm: avgPace,
            totalElevationGain: totalElevation,
            totalCalories: totalCalories,
            longestRunMeters: longestRun,
            fastestPaceSecondsPerKm: fastestPace
        )
    }

    func weeklyStatsForPastWeeks(_ weeks: Int) -> [WeeklyRunStats] {
        let now = Date()
        let mondayThisWeek = now.startOfWeek
        return (0..<weeks).map { weekOffset in
            let weekStart = Calendar.current.date(byAdding: .weekOfYear, value: -weekOffset, to: mondayThisWeek) ?? mondayThisWeek
            return weeklyStats(weekStartDate: weekStart)
        }
    }
}

struct WeeklyRunStats {
    let weekStartDate: Date
    let totalDistanceMeters: Double
    let totalDurationMillis: Int64
    let totalRuns: Int
    let avgPaceSecondsPerKm: Double
    let totalElevationGain: Double
    let totalCalories: Int
    let longestRunMeters: Double
    let fastestPaceSecondsPerKm: Double

    var totalDistanceKm: Double { totalDistanceMeters / 1000.0 }
    var avgPaceFormatted: String { FormatUtils.formatPace(avgPaceSecondsPerKm) }
    var totalDurationFormatted: String { FormatUtils.formatDurationLong(milliseconds: totalDurationMillis) }
}
