import Foundation
import SwiftData

final class MentalHealthRepository {
    private var context: ModelContext?

    init() {
        self.context = nil
    }

    init(context: ModelContext) {
        self.context = context
    }

    // MARK: - Sessions

    func getRecentSessions(limit: Int) async throws -> [MindfulnessSession] {
        guard let context else { return [] }
        var descriptor = FetchDescriptor<MindfulnessSession>(
            predicate: #Predicate { $0.completed == true },
            sortBy: [SortDescriptor(\.timestamp, order: .reverse)]
        )
        descriptor.fetchLimit = limit
        return (try? context.fetch(descriptor)) ?? []
    }

    func getSessionCountThisWeek() async throws -> Int {
        guard let context else { return 0 }
        let weekAgo = Calendar.current.date(byAdding: .day, value: -7, to: Date()) ?? Date()
        let descriptor = FetchDescriptor<MindfulnessSession>(sortBy: [SortDescriptor(\.timestamp, order: .reverse)])
        let all = (try? context.fetch(descriptor)) ?? []
        return all.filter { $0.timestamp >= weekAgo && $0.completed }.count
    }

    func getMindfulnessMinutesThisWeek() async throws -> Int {
        guard let context else { return 0 }
        let weekAgo = Calendar.current.date(byAdding: .day, value: -7, to: Date()) ?? Date()
        let descriptor = FetchDescriptor<MindfulnessSession>(sortBy: [SortDescriptor(\.timestamp, order: .reverse)])
        let all = (try? context.fetch(descriptor)) ?? []
        let sessions = all.filter { $0.timestamp >= weekAgo && $0.completed }
        return sessions.reduce(0) { $0 + $1.durationSeconds } / 60
    }

    func getMindfulnessStreak() async throws -> Int {
        guard let context else { return 0 }
        let descriptor = FetchDescriptor<MindfulnessSession>(
            predicate: #Predicate { $0.completed == true },
            sortBy: [SortDescriptor(\.timestamp, order: .reverse)]
        )
        let sessions = (try? context.fetch(descriptor)) ?? []
        guard !sessions.isEmpty else { return 0 }

        var streak = 0
        var currentDate = Calendar.current.startOfDay(for: Date())
        let calendar = Calendar.current

        while true {
            let nextDay = calendar.date(byAdding: .day, value: 1, to: currentDate)!
            let hasSession = sessions.contains { session in
                session.timestamp >= currentDate && session.timestamp < nextDay
            }
            if hasSession {
                streak += 1
                guard let prevDay = calendar.date(byAdding: .day, value: -1, to: currentDate) else { break }
                currentDate = prevDay
            } else {
                break
            }
        }
        return streak
    }

    func saveSession(_ session: MindfulnessSession) async throws {
        guard let context else { return }
        context.insert(session)
        try context.save()
    }

    // MARK: - Mood Entries

    func getRecentMoodEntries(limit: Int) async throws -> [MoodEntry] {
        guard let context else { return [] }
        var descriptor = FetchDescriptor<MoodEntry>(sortBy: [SortDescriptor(\.timestamp, order: .reverse)])
        descriptor.fetchLimit = limit
        return (try? context.fetch(descriptor)) ?? []
    }

    func saveMoodEntry(_ entry: MoodEntry) async throws {
        guard let context else { return }
        context.insert(entry)
        try context.save()
    }
}
