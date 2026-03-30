import Foundation
import SwiftData

final class WellnessRepository {
    private let context: ModelContext

    init(context: ModelContext) {
        self.context = context
    }

    // MARK: - Mood Entries

    func fetchAllMoodEntries() -> [MoodEntry] {
        let descriptor = FetchDescriptor<MoodEntry>(sortBy: [SortDescriptor(\.timestamp, order: .reverse)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchMoodEntriesSince(_ date: Date) -> [MoodEntry] {
        let descriptor = FetchDescriptor<MoodEntry>(
            predicate: #Predicate { $0.timestamp >= date },
            sortBy: [SortDescriptor(\.timestamp, order: .reverse)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchMoodEntriesForWorkout(_ workoutId: UUID) -> [MoodEntry] {
        let descriptor = FetchDescriptor<MoodEntry>(
            predicate: #Predicate { $0.relatedRunId == workoutId },
            sortBy: [SortDescriptor(\.timestamp)]
        )
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchLatestMoodEntry() -> MoodEntry? {
        var descriptor = FetchDescriptor<MoodEntry>(sortBy: [SortDescriptor(\.timestamp, order: .reverse)])
        descriptor.fetchLimit = 1
        return try? context.fetch(descriptor).first
    }

    func logMood(
        mood: MoodLevel,
        energy: EnergyLevel,
        stress: StressLevel,
        notes: String = "",
        relatedWorkoutId: UUID? = nil,
        isPreWorkout: Bool = true,
        tags: [String] = []
    ) -> MoodEntry {
        let entry = MoodEntry(
            mood: mood,
            energy: energy,
            stress: stress,
            notes: notes,
            relatedRunId: relatedWorkoutId,
            isPreWorkout: isPreWorkout,
            tags: tags
        )
        context.insert(entry)
        try? context.save()
        return entry
    }

    func logPreWorkoutMood(mood: MoodLevel, energy: EnergyLevel, stress: StressLevel, workoutId: UUID? = nil) -> MoodEntry {
        logMood(mood: mood, energy: energy, stress: stress, relatedWorkoutId: workoutId, isPreWorkout: true, tags: ["pre-workout"])
    }

    func logPostWorkoutMood(mood: MoodLevel, energy: EnergyLevel, stress: StressLevel, workoutId: UUID) -> MoodEntry {
        logMood(mood: mood, energy: energy, stress: stress, relatedWorkoutId: workoutId, isPreWorkout: false, tags: ["post-workout"])
    }

    // MARK: - Mindfulness Sessions

    func fetchAllMindfulnessSessions() -> [MindfulnessSession] {
        let descriptor = FetchDescriptor<MindfulnessSession>(sortBy: [SortDescriptor(\.timestamp, order: .reverse)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchRecentMindfulnessSessions(limit: Int = 10) -> [MindfulnessSession] {
        var descriptor = FetchDescriptor<MindfulnessSession>(
            predicate: #Predicate { $0.completed == true },
            sortBy: [SortDescriptor(\.timestamp, order: .reverse)]
        )
        descriptor.fetchLimit = limit
        return (try? context.fetch(descriptor)) ?? []
    }

    func totalMindfulnessMinutesThisWeek() -> Int {
        let weekAgo = Calendar.current.date(byAdding: .day, value: -7, to: Date()) ?? Date()
        let sessions = fetchAllMindfulnessSessions().filter { $0.timestamp >= weekAgo && $0.completed }
        return sessions.reduce(0) { $0 + $1.durationSeconds } / 60
    }

    func mindfulnessSessionCountThisWeek() -> Int {
        let weekAgo = Calendar.current.date(byAdding: .day, value: -7, to: Date()) ?? Date()
        return fetchAllMindfulnessSessions().filter { $0.timestamp >= weekAgo && $0.completed }.count
    }

    func recordMindfulnessSession(
        type: MindfulnessType,
        durationSeconds: Int,
        completed: Bool = true,
        relatedWorkoutId: UUID? = nil,
        rating: Int? = nil
    ) -> MindfulnessSession {
        let session = MindfulnessSession(
            type: type,
            durationSeconds: durationSeconds,
            completed: completed,
            relatedRunId: relatedWorkoutId,
            rating: rating
        )
        context.insert(session)
        try? context.save()
        return session
    }

    // MARK: - Wellness Check-ins

    func fetchAllWellnessCheckins() -> [WellnessCheckin] {
        let descriptor = FetchDescriptor<WellnessCheckin>(sortBy: [SortDescriptor(\.date, order: .reverse)])
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchRecentCheckins(limit: Int = 7) -> [WellnessCheckin] {
        var descriptor = FetchDescriptor<WellnessCheckin>(sortBy: [SortDescriptor(\.date, order: .reverse)])
        descriptor.fetchLimit = limit
        return (try? context.fetch(descriptor)) ?? []
    }

    func fetchLatestCheckin() -> WellnessCheckin? {
        var descriptor = FetchDescriptor<WellnessCheckin>(sortBy: [SortDescriptor(\.date, order: .reverse)])
        descriptor.fetchLimit = 1
        return try? context.fetch(descriptor).first
    }

    func fetchTodayCheckin() -> WellnessCheckin? {
        let today = Date().startOfDay
        let descriptor = FetchDescriptor<WellnessCheckin>(predicate: #Predicate { $0.date == today })
        return try? context.fetch(descriptor).first
    }

    func saveWellnessCheckin(
        sleepHours: Float? = nil,
        sleepQuality: Int? = nil,
        restingHeartRate: Int? = nil,
        hrv: Int? = nil,
        mood: MoodLevel? = nil,
        energy: EnergyLevel? = nil,
        stress: StressLevel? = nil,
        soreness: Int? = nil,
        hydration: Int? = nil,
        notes: String = ""
    ) -> WellnessCheckin {
        let today = Date().startOfDay
        let existing = fetchTodayCheckin()

        let readiness = calculateReadinessScore(
            sleepHours: sleepHours ?? existing?.sleepHours,
            sleepQuality: sleepQuality ?? existing?.sleepQuality,
            mood: mood ?? existing?.mood,
            energy: energy ?? existing?.energy,
            stress: stress ?? existing?.stress,
            soreness: soreness ?? existing?.soreness,
            hrv: hrv ?? existing?.hrv
        )

        if let checkin = existing {
            checkin.sleepHours = sleepHours ?? checkin.sleepHours
            checkin.sleepQuality = sleepQuality ?? checkin.sleepQuality
            checkin.restingHeartRate = restingHeartRate ?? checkin.restingHeartRate
            checkin.hrv = hrv ?? checkin.hrv
            checkin.mood = mood ?? checkin.mood
            checkin.energy = energy ?? checkin.energy
            checkin.stress = stress ?? checkin.stress
            checkin.soreness = soreness ?? checkin.soreness
            checkin.hydration = hydration ?? checkin.hydration
            if !notes.isEmpty { checkin.notes = notes }
            checkin.readinessScore = readiness
            try? context.save()
            return checkin
        }

        let checkin = WellnessCheckin(
            date: today,
            sleepHours: sleepHours,
            sleepQuality: sleepQuality,
            restingHeartRate: restingHeartRate,
            hrv: hrv,
            mood: mood,
            energy: energy,
            stress: stress,
            soreness: soreness,
            hydration: hydration,
            notes: notes,
            readinessScore: readiness
        )
        context.insert(checkin)
        try? context.save()
        return checkin
    }

    func averageReadinessLastWeek() -> Float? {
        let weekAgo = Calendar.current.date(byAdding: .day, value: -7, to: Date())?.startOfDay ?? Date()
        let checkins = fetchAllWellnessCheckins().filter { $0.date >= weekAgo }
        let scores = checkins.compactMap(\.readinessScore)
        guard !scores.isEmpty else { return nil }
        return Float(scores.reduce(0, +)) / Float(scores.count)
    }

    // MARK: - Mood Improvement

    func moodImprovementForWorkout(_ workoutId: UUID) -> MoodImprovement? {
        let entries = fetchMoodEntriesForWorkout(workoutId)
        guard let pre = entries.first(where: \.isPreWorkout),
              let post = entries.first(where: { !$0.isPreWorkout }) else { return nil }

        return MoodImprovement(
            moodChange: post.mood.rawValue - pre.mood.rawValue,
            energyChange: post.energy.rawValue - pre.energy.rawValue,
            stressChange: pre.stress.rawValue - post.stress.rawValue
        )
    }

    // MARK: - Private

    private func calculateReadinessScore(
        sleepHours: Float?,
        sleepQuality: Int?,
        mood: MoodLevel?,
        energy: EnergyLevel?,
        stress: StressLevel?,
        soreness: Int?,
        hrv: Int?
    ) -> Int {
        var score = 50

        if let sh = sleepHours {
            if sh >= 7 && sh <= 9 { score += 10 }
            else if (sh >= 6 && sh < 7) || (sh > 9 && sh <= 10) { score += 5 }
            else { score -= 5 }
        }

        if let sq = sleepQuality { score += (sq - 3) * 5 }
        if let m = mood { score += (m.rawValue - 3) * 5 }
        if let e = energy { score += (e.rawValue - 3) * 5 }
        if let s = stress { score += (3 - s.rawValue) * 5 }
        if let so = soreness { score += (3 - so) * 3 }

        if let h = hrv {
            if h > 60 { score += 10 }
            else if h > 40 { score += 5 }
            else if h <= 20 { score -= 5 }
        }

        return max(0, min(100, score))
    }
}

struct MoodImprovement {
    let moodChange: Int
    let energyChange: Int
    let stressChange: Int

    var overallImprovement: Int { moodChange + energyChange + stressChange }
    var isPositive: Bool { overallImprovement > 0 }
}
