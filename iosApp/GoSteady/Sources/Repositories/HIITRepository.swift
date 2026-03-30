import Foundation
import SwiftData

final class HIITRepository {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    // MARK: - Sessions

    func fetchAllSessions() -> [HIITSession] {
        let descriptor = FetchDescriptor<HIITSession>(sortBy: [SortDescriptor(\.date, order: .reverse)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchSessionById(_ id: UUID) -> HIITSession? {
        let descriptor = FetchDescriptor<HIITSession>(predicate: #Predicate { $0.id == id })
        return try? context.fetch(descriptor).first
    }

    func fetchRecentCompleted(limit: Int = 10) -> [HIITSession] {
        var descriptor = FetchDescriptor<HIITSession>(
            predicate: #Predicate { $0.isCompleted == true },
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        descriptor.fetchLimit = limit
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchSessionsSince(_ date: Date) -> [HIITSession] {
        let descriptor = FetchDescriptor<HIITSession>(
            predicate: #Predicate { $0.date >= date },
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func insert(_ session: HIITSession) {
        context.insert(session)
        try? context.save()
    }

    func update(_ session: HIITSession) {
        try? context.save()
    }

    func delete(_ session: HIITSession) {
        context.delete(session)
        try? context.save()
    }

    func sessionCountSince(_ date: Date) -> Int {
        fetchSessionsSince(date).filter(\.isCompleted).count
    }

    func totalCaloriesSince(_ date: Date) -> Int {
        fetchSessionsSince(date).filter(\.isCompleted).reduce(0) { $0 + $1.caloriesEstimate }
    }

    // MARK: - Weekly Stats

    func weeklyStats() -> HIITWeeklyStats {
        let weekStart = Date().startOfWeek
        return HIITWeeklyStats(
            sessionCount: sessionCountSince(weekStart),
            totalCalories: totalCaloriesSince(weekStart),
            weekStartDate: weekStart
        )
    }

    // MARK: - Templates (in-memory from library)

    func allTemplates() -> [HIITWorkoutTemplate] {
        HIITExerciseLibrary.allTemplates
    }

    func templateById(_ id: String) -> HIITWorkoutTemplate? {
        HIITExerciseLibrary.getTemplateById(id)
    }
}

struct HIITWeeklyStats {
    let sessionCount: Int
    let totalCalories: Int
    let weekStartDate: Date
}
