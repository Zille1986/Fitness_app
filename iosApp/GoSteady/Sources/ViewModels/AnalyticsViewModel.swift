import Foundation
import Observation

// MARK: - ViewModel-specific Models

enum DateRangeOption: String, CaseIterable, Identifiable {
    case week, month, threeMonths, sixMonths, year
    var id: String { rawValue }
    var displayName: String {
        switch self {
        case .week: return "7D"
        case .month: return "1M"
        case .threeMonths: return "3M"
        case .sixMonths: return "6M"
        case .year: return "1Y"
        }
    }
    var days: Int {
        switch self {
        case .week: return 7
        case .month: return 30
        case .threeMonths: return 90
        case .sixMonths: return 180
        case .year: return 365
        }
    }
}

struct OverviewStats {
    var totalWorkouts: Int = 0
    var totalDistanceKm: Double = 0
    var totalTimeMinutes: Int = 0
    var totalCalories: Int = 0
}

struct SportBreakdown: Identifiable {
    let id = UUID()
    let sport: SportType
    var count: Int
    var percentage: Double
}

struct WeeklyActivity: Identifiable {
    let id = UUID()
    let day: String
    let date: Date
    var workouts: Int
    var minutes: Int
}

struct TrendDataPoint: Identifiable {
    let id = UUID()
    let date: Date
    let value: Double
}

struct AnalyticsPersonalBest: Identifiable {
    let id = UUID()
    let sport: SportType
    let metric: String
    let value: String
    let date: Date
}

struct SportDetailStats {
    var totalSessions: Int = 0
    var totalDistance: Double = 0
    var totalTime: Int = 0
    var avgDuration: Int = 0
    var avgDistance: Double = 0
    var paceOrSpeedTrend: [TrendDataPoint] = []
    var distanceTrend: [TrendDataPoint] = []
    var volumeTrend: [TrendDataPoint] = []
    var frequencyByWeek: [WeeklyActivity] = []
}

// MARK: - ViewModel

@Observable
final class AnalyticsViewModel {
    var selectedDateRange: DateRangeOption = .month
    var selectedSport: SportType?

    var overviewStats = OverviewStats()
    var sportBreakdown: [SportBreakdown] = []
    var weeklyActivity: [WeeklyActivity] = []
    var distanceTrend: [TrendDataPoint] = []
    var durationTrend: [TrendDataPoint] = []
    var personalBests: [AnalyticsPersonalBest] = []
    var sportDetailStats = SportDetailStats()
    var isLoading = false

    init() {
        Task { await loadData() }
    }

    @MainActor
    func loadData() async {
        isLoading = true
        await loadOverview()
        await loadSportBreakdown()
        await loadWeeklyActivity()
        await loadTrends()
        await loadAnalyticsPersonalBests()
        if let sport = selectedSport {
            await loadSportDetail(sport)
        }
        isLoading = false
    }

    @MainActor
    func selectDateRange(_ range: DateRangeOption) {
        selectedDateRange = range
        Task { await loadData() }
    }

    @MainActor
    func selectSport(_ sport: SportType) {
        selectedSport = sport
        Task { await loadSportDetail(sport) }
    }

    // MARK: - Data Loading

    @MainActor
    private func loadOverview() async {
        overviewStats = OverviewStats(
            totalWorkouts: 86,
            totalDistanceKm: 234.5,
            totalTimeMinutes: 3420,
            totalCalories: 48200
        )
    }

    @MainActor
    private func loadSportBreakdown() async {
        sportBreakdown = [
            SportBreakdown(sport: .run, count: 38, percentage: 0.44),
            SportBreakdown(sport: .gym, count: 28, percentage: 0.33),
            SportBreakdown(sport: .swim, count: 12, percentage: 0.14),
            SportBreakdown(sport: .bike, count: 8, percentage: 0.09),
        ]
    }

    @MainActor
    private func loadWeeklyActivity() async {
        let calendar = Calendar.current
        let dayFormatter = DateFormatter()
        dayFormatter.dateFormat = "EEE"
        weeklyActivity = (0..<7).reversed().map { offset in
            let date = calendar.date(byAdding: .day, value: -offset, to: .now)!
            return WeeklyActivity(
                day: dayFormatter.string(from: date),
                date: date,
                workouts: Int.random(in: 0...2),
                minutes: Int.random(in: 0...90)
            )
        }
    }

    @MainActor
    private func loadTrends() async {
        let calendar = Calendar.current
        let days = selectedDateRange.days
        distanceTrend = (0..<days).compactMap { offset -> TrendDataPoint? in
            guard offset % max(days / 20, 1) == 0 else { return nil }
            let date = calendar.date(byAdding: .day, value: -offset, to: .now)!
            return TrendDataPoint(date: date, value: Double.random(in: 2...12))
        }.reversed()

        durationTrend = (0..<days).compactMap { offset -> TrendDataPoint? in
            guard offset % max(days / 20, 1) == 0 else { return nil }
            let date = calendar.date(byAdding: .day, value: -offset, to: .now)!
            return TrendDataPoint(date: date, value: Double.random(in: 20...90))
        }.reversed()
    }

    @MainActor
    private func loadAnalyticsPersonalBests() async {
        personalBests = [
            AnalyticsPersonalBest(sport: .run, metric: "Fastest 5K", value: "23:42", date: Calendar.current.date(byAdding: .day, value: -5, to: .now)!),
            AnalyticsPersonalBest(sport: .run, metric: "Longest Run", value: "15.3 km", date: Calendar.current.date(byAdding: .day, value: -12, to: .now)!),
            AnalyticsPersonalBest(sport: .run, metric: "Best Pace", value: "4:35 /km", date: Calendar.current.date(byAdding: .day, value: -18, to: .now)!),
            AnalyticsPersonalBest(sport: .gym, metric: "Max Bench Press", value: "85 kg", date: Calendar.current.date(byAdding: .day, value: -3, to: .now)!),
            AnalyticsPersonalBest(sport: .gym, metric: "Max Squat", value: "120 kg", date: Calendar.current.date(byAdding: .day, value: -8, to: .now)!),
            AnalyticsPersonalBest(sport: .swim, metric: "Fastest 100m", value: "1:48", date: Calendar.current.date(byAdding: .day, value: -10, to: .now)!),
            AnalyticsPersonalBest(sport: .bike, metric: "Longest Ride", value: "42.1 km", date: Calendar.current.date(byAdding: .day, value: -15, to: .now)!),
        ]
    }

    @MainActor
    func loadSportDetail(_ sport: SportType) async {
        let calendar = Calendar.current
        let days = selectedDateRange.days

        let baseSessions: Int
        let baseDistance: Double
        switch sport {
        case .run:
            baseSessions = 38; baseDistance = 185
        case .gym:
            baseSessions = 28; baseDistance = 0
        case .swim:
            baseSessions = 12; baseDistance = 24
        case .bike:
            baseSessions = 8; baseDistance = 180
        case .hiit:
            baseSessions = 15; baseDistance = 0
        }

        let paceTrend: [TrendDataPoint] = (0..<days).compactMap { offset -> TrendDataPoint? in
            guard offset % max(days / 15, 1) == 0 else { return nil }
            let date = calendar.date(byAdding: .day, value: -offset, to: .now)!
            return TrendDataPoint(date: date, value: Double.random(in: 4.2...6.5))
        }.reversed()

        let distTrend: [TrendDataPoint] = (0..<days).compactMap { offset -> TrendDataPoint? in
            guard offset % max(days / 15, 1) == 0 else { return nil }
            let date = calendar.date(byAdding: .day, value: -offset, to: .now)!
            return TrendDataPoint(date: date, value: Double.random(in: 3...15))
        }.reversed()

        let volTrend: [TrendDataPoint] = (0..<days).compactMap { offset -> TrendDataPoint? in
            guard offset % max(days / 15, 1) == 0 else { return nil }
            let date = calendar.date(byAdding: .day, value: -offset, to: .now)!
            return TrendDataPoint(date: date, value: Double.random(in: 1000...8000))
        }.reversed()

        sportDetailStats = SportDetailStats(
            totalSessions: baseSessions,
            totalDistance: baseDistance,
            totalTime: baseSessions * Int.random(in: 30...60),
            avgDuration: Int.random(in: 30...55),
            avgDistance: baseDistance / max(Double(baseSessions), 1),
            paceOrSpeedTrend: paceTrend,
            distanceTrend: distTrend,
            volumeTrend: volTrend,
            frequencyByWeek: weeklyActivity
        )
    }
}
