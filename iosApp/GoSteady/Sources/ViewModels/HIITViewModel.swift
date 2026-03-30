import Foundation
import Observation

// MARK: - ViewModel-specific Types

enum HIITTemplateType: String, CaseIterable, Identifiable {
    case tabata = "Tabata"
    case emom = "EMOM"
    case amrap = "AMRAP"
    case custom = "Custom"

    var id: String { rawValue }
    var icon: String {
        switch self {
        case .tabata: return "timer"
        case .emom: return "clock.arrow.circlepath"
        case .amrap: return "repeat"
        case .custom: return "slider.horizontal.3"
        }
    }
    var description: String {
        switch self {
        case .tabata: return "20s work / 10s rest x 8 rounds"
        case .emom: return "Every Minute On the Minute"
        case .amrap: return "As Many Rounds As Possible"
        case .custom: return "Build your own workout"
        }
    }
}

// MARK: - ViewModel

@Observable
final class HIITViewModel {
    var templates: [HIITWorkoutTemplate] = HIITExerciseLibrary.allTemplates
    var customTemplates: [HIITWorkoutTemplate] = []
    var recentSessions: [HIITSession] = []
    var weeklySessionCount: Int = 0
    var weeklyCalories: Int = 0
    var isLoading = true
    var errorMessage: String?
    var selectedCategory: HIITTemplateType? = nil

    private let hiitRepository: HIITRepository

    init(hiitRepository: HIITRepository) {
        self.hiitRepository = hiitRepository
        loadData()
    }

    var filteredTemplates: [HIITWorkoutTemplate] {
        guard let category = selectedCategory else { return templates + customTemplates }
        switch category {
        case .custom:
            return customTemplates
        case .tabata:
            return templates.filter { $0.workDurationSec == 20 && $0.restDurationSec == 10 }
        case .emom:
            return templates.filter { ($0.workDurationSec + $0.restDurationSec) == 60 }
        case .amrap:
            return templates
        }
    }

    func loadData() {
        isLoading = true
        errorMessage = nil

        Task { @MainActor in
            self.recentSessions = hiitRepository.fetchRecentCompleted(limit: 10)
            let stats = hiitRepository.weeklyStats()
            self.weeklySessionCount = stats.sessionCount
            self.weeklyCalories = stats.totalCalories
            self.isLoading = false
        }
    }

    func deleteSession(_ session: HIITSession) {
        hiitRepository.delete(session)
        recentSessions.removeAll { $0.id == session.id }
    }

    func saveCustomTemplate(_ template: HIITWorkoutTemplate) {
        customTemplates.append(template)
    }

    func deleteCustomTemplate(_ template: HIITWorkoutTemplate) {
        customTemplates.removeAll { $0.id == template.id }
    }

    static func difficultyColor(_ difficulty: String) -> String {
        if difficulty.contains("Hard") { return "E53935" }
        if difficulty.contains("Medium") { return "FF9800" }
        return "4CAF50"
    }
}
