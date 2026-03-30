import Foundation
import Observation

// MARK: - Guided Session Content

enum MindfulnessSessions {
    static let preRunFocus = MindfulnessContent(
        id: "pre_run_focus",
        title: "Pre-Run Mental Preparation",
        contentDescription: "Get mentally ready for your run with focused breathing and intention setting.",
        type: .preRunFocus,
        durationSeconds: 180,
        instructions: [
            "Find a comfortable standing or seated position",
            "Close your eyes and take 3 deep breaths",
            "Set your intention for today's run",
            "Visualize yourself running strong and confident",
            "Feel gratitude for your body's ability to move",
            "Open your eyes, ready to run"
        ],
        tags: ["pre-run", "focus", "intention"]
    )

    static let postRunGratitude = MindfulnessContent(
        id: "post_run_gratitude",
        title: "Post-Run Reflection",
        contentDescription: "Celebrate your accomplishment and aid recovery with mindful reflection.",
        type: .postRunGratitude,
        durationSeconds: 180,
        instructions: [
            "Find a comfortable position to cool down",
            "Close your eyes and notice your breath returning to normal",
            "Acknowledge what you just accomplished",
            "Thank your body for carrying you through",
            "Notice how you feel - physically and emotionally",
            "Set an intention for recovery"
        ],
        tags: ["post-run", "gratitude", "recovery"]
    )

    static let bodyScan = MindfulnessContent(
        id: "body_scan",
        title: "Runner's Body Scan",
        contentDescription: "Check in with your body to identify tension and promote recovery.",
        type: .bodyScan,
        durationSeconds: 300,
        instructions: [
            "Lie down or sit comfortably",
            "Start at the top of your head, notice any tension",
            "Move attention to your face, jaw, neck",
            "Scan your shoulders, arms, hands",
            "Notice your chest, breathing naturally",
            "Feel your core, lower back",
            "Scan your hips, glutes",
            "Move to your thighs, knees",
            "Notice your calves, ankles, feet",
            "Take a moment to feel your whole body as one"
        ],
        tags: ["body-scan", "recovery", "awareness"]
    )

    static let stressRelief = MindfulnessContent(
        id: "stress_relief",
        title: "Quick Stress Relief",
        contentDescription: "Release tension and find calm in just 5 minutes.",
        type: .stressRelief,
        durationSeconds: 300,
        instructions: [
            "Sit or stand comfortably",
            "Take 3 deep breaths, exhaling fully",
            "Tense your shoulders up to your ears, hold, release",
            "Clench your fists tight, hold, release",
            "Scrunch your face, hold, release",
            "Feel the wave of relaxation through your body",
            "Continue breathing slowly and deeply",
            "Imagine stress leaving your body with each exhale"
        ],
        tags: ["stress", "relaxation", "quick"]
    )

    static func getAll() -> [MindfulnessContent] {
        [preRunFocus, postRunGratitude, bodyScan, stressRelief]
    }
}

// MARK: - Breathing State

enum BreathingPhase: String {
    case inhale = "Breathe In"
    case holdIn = "Hold In"
    case exhale = "Breathe Out"
    case holdOut = "Hold Out"

    var colorHex: String {
        switch self {
        case .inhale: return "4CAF50"
        case .holdIn: return "2196F3"
        case .exhale: return "9C27B0"
        case .holdOut: return "FF9800"
        }
    }

    var displayText: String {
        rawValue
    }
}

struct BreathingSessionState {
    var pattern: BreathingPattern
    var currentCycle: Int = 1
    var phase: BreathingPhase = .inhale
    var secondsRemaining: Int
    var isActive: Bool = true
}

struct GuidedSessionState {
    var content: MindfulnessContent
    var currentStepIndex: Int = 0
    var secondsRemaining: Int
    var secondsPerStep: Int
    var totalElapsedSeconds: Int = 0
    var isActive: Bool = true
    var isPaused: Bool = false

    var currentInstruction: String {
        guard currentStepIndex < content.instructions.count else { return "" }
        return content.instructions[currentStepIndex]
    }

    var progress: Float {
        Float(currentStepIndex + 1) / Float(max(content.instructions.count, 1))
    }

    var isLastStep: Bool {
        currentStepIndex >= content.instructions.count - 1
    }
}

// MARK: - MindfulnessViewModel

@Observable
final class MindfulnessViewModel {
    private let mentalHealthRepository: MentalHealthRepository

    // UI State
    var recentSessions: [MindfulnessSession] = []
    var recentMoodEntries: [MoodEntry] = []
    var mindfulnessMinutesThisWeek: Int = 0
    var sessionsThisWeek: Int = 0
    var currentStreak: Int = 0
    var isLoading = false
    var errorMessage: String?

    // Breathing
    var breathingState: BreathingSessionState?
    var selectedBreathingPattern: BreathingPattern?

    // Guided sessions
    var guidedSessionState: GuidedSessionState?
    var selectedGuidedSession: MindfulnessContent?

    // Mood logging
    var showingMoodLog = false
    var selectedMood: MoodLevel = .neutral
    var selectedEnergy: EnergyLevel = .moderate
    var selectedStress: StressLevel = .moderate
    var moodNotes: String = ""

    // Meditation
    var meditationDuration: Int = 5 // minutes
    var meditationIsRunning = false
    var meditationSecondsRemaining: Int = 0
    var meditationBellInterval: Int = 0 // 0 = no bell
    var selectedAmbientSound: AmbientSound = .none

    init(mentalHealthRepository: MentalHealthRepository = MentalHealthRepository()) {
        self.mentalHealthRepository = mentalHealthRepository
        loadData()
    }

    func loadData() {
        isLoading = true
        Task { @MainActor in
            do {
                let sessions = try await mentalHealthRepository.getRecentSessions(limit: 10)
                let moods = try await mentalHealthRepository.getRecentMoodEntries(limit: 30)
                let minutes = try await mentalHealthRepository.getMindfulnessMinutesThisWeek()
                let count = try await mentalHealthRepository.getSessionCountThisWeek()
                let streak = try await mentalHealthRepository.getMindfulnessStreak()

                self.recentSessions = sessions
                self.recentMoodEntries = moods
                self.mindfulnessMinutesThisWeek = minutes
                self.sessionsThisWeek = count
                self.currentStreak = streak
                self.isLoading = false
            } catch {
                self.errorMessage = error.localizedDescription
                self.isLoading = false
            }
        }
    }

    // MARK: - Mood Logging

    func logMood() {
        let entry = MoodEntry(
            mood: selectedMood,
            energy: selectedEnergy,
            stress: selectedStress,
            notes: moodNotes
        )
        Task { @MainActor in
            do {
                try await mentalHealthRepository.saveMoodEntry(entry)
                resetMoodForm()
                loadData()
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }

    func resetMoodForm() {
        selectedMood = .neutral
        selectedEnergy = .moderate
        selectedStress = .moderate
        moodNotes = ""
        showingMoodLog = false
    }

    // MARK: - Breathing Sessions

    func startBreathingSession(_ pattern: BreathingPattern) {
        breathingState = BreathingSessionState(
            pattern: pattern,
            secondsRemaining: pattern.inhaleSeconds
        )
        selectedBreathingPattern = nil
    }

    func tickBreathingSession() {
        guard var state = breathingState, state.isActive else { return }
        let newRemaining = state.secondsRemaining - 1

        if newRemaining <= 0 {
            let pattern = state.pattern
            switch state.phase {
            case .inhale:
                if pattern.holdAfterInhale > 0 {
                    state.phase = .holdIn
                    state.secondsRemaining = pattern.holdAfterInhale
                } else {
                    state.phase = .exhale
                    state.secondsRemaining = pattern.exhaleSeconds
                }
            case .holdIn:
                state.phase = .exhale
                state.secondsRemaining = pattern.exhaleSeconds
            case .exhale:
                if pattern.holdAfterExhale > 0 {
                    state.phase = .holdOut
                    state.secondsRemaining = pattern.holdAfterExhale
                } else {
                    if state.currentCycle >= pattern.cycles {
                        completeBreathingSession()
                        return
                    }
                    state.currentCycle += 1
                    state.phase = .inhale
                    state.secondsRemaining = pattern.inhaleSeconds
                }
            case .holdOut:
                if state.currentCycle >= pattern.cycles {
                    completeBreathingSession()
                    return
                }
                state.currentCycle += 1
                state.phase = .inhale
                state.secondsRemaining = pattern.inhaleSeconds
            }
        } else {
            state.secondsRemaining = newRemaining
        }
        breathingState = state
    }

    func completeBreathingSession() {
        guard let state = breathingState else { return }
        let session = MindfulnessSession(
            type: .breathingExercise,
            durationSeconds: state.pattern.totalDuration,
            completed: true
        )
        Task { @MainActor in
            try? await mentalHealthRepository.saveSession(session)
            self.breathingState = nil
            loadData()
        }
    }

    func cancelBreathingSession() {
        guard let state = breathingState else { return }
        let completedDuration = (state.currentCycle - 1) * state.pattern.cycleDuration
        if completedDuration > 30 {
            let session = MindfulnessSession(
                type: .breathingExercise,
                durationSeconds: completedDuration,
                completed: false
            )
            Task { @MainActor in
                try? await mentalHealthRepository.saveSession(session)
            }
        }
        breathingState = nil
    }

    // MARK: - Guided Sessions

    func startGuidedSession(_ content: MindfulnessContent) {
        let secondsPerStep = content.durationSeconds / max(content.instructions.count, 1)
        guidedSessionState = GuidedSessionState(
            content: content,
            secondsRemaining: secondsPerStep,
            secondsPerStep: secondsPerStep
        )
        selectedGuidedSession = nil
    }

    func tickGuidedSession() {
        guard var state = guidedSessionState, state.isActive, !state.isPaused else { return }
        let newRemaining = state.secondsRemaining - 1
        state.totalElapsedSeconds += 1

        if newRemaining <= 0 {
            if state.isLastStep {
                guidedSessionState = state
                return
            }
            state.currentStepIndex += 1
            state.secondsRemaining = state.secondsPerStep
        } else {
            state.secondsRemaining = newRemaining
        }
        guidedSessionState = state
    }

    func completeGuidedSession(rating: Int? = nil) {
        guard let state = guidedSessionState else { return }
        let session = MindfulnessSession(
            type: state.content.type,
            durationSeconds: max(state.totalElapsedSeconds, state.content.durationSeconds),
            completed: true,
            rating: rating
        )
        Task { @MainActor in
            try? await mentalHealthRepository.saveSession(session)
            self.guidedSessionState = nil
            loadData()
        }
    }

    func cancelGuidedSession() {
        guard let state = guidedSessionState else { return }
        if state.totalElapsedSeconds > 30 {
            let session = MindfulnessSession(
                type: state.content.type,
                durationSeconds: state.totalElapsedSeconds,
                completed: false
            )
            Task { @MainActor in
                try? await mentalHealthRepository.saveSession(session)
            }
        }
        guidedSessionState = nil
    }

    func pauseGuidedSession() {
        guidedSessionState?.isPaused = true
    }

    func resumeGuidedSession() {
        guidedSessionState?.isPaused = false
    }

    func goToPreviousStep() {
        guard var state = guidedSessionState, state.currentStepIndex > 0 else { return }
        state.currentStepIndex -= 1
        state.secondsRemaining = state.secondsPerStep
        guidedSessionState = state
    }

    func goToNextStep() {
        guard var state = guidedSessionState else { return }
        if state.isLastStep { return }
        state.currentStepIndex += 1
        state.secondsRemaining = state.secondsPerStep
        state.totalElapsedSeconds += (state.secondsPerStep - state.secondsRemaining)
        guidedSessionState = state
    }

    // MARK: - Meditation Timer

    func startMeditation() {
        meditationSecondsRemaining = meditationDuration * 60
        meditationIsRunning = true
    }

    func tickMeditation() {
        guard meditationIsRunning, meditationSecondsRemaining > 0 else { return }
        meditationSecondsRemaining -= 1
        if meditationSecondsRemaining <= 0 {
            completeMeditation()
        }
    }

    func completeMeditation() {
        meditationIsRunning = false
        let session = MindfulnessSession(
            type: .stressRelief,
            durationSeconds: meditationDuration * 60,
            completed: true
        )
        Task { @MainActor in
            try? await mentalHealthRepository.saveSession(session)
            loadData()
        }
    }

    func cancelMeditation() {
        let elapsed = (meditationDuration * 60) - meditationSecondsRemaining
        meditationIsRunning = false
        meditationSecondsRemaining = 0
        if elapsed > 30 {
            let session = MindfulnessSession(
                type: .stressRelief,
                durationSeconds: elapsed,
                completed: false
            )
            Task { @MainActor in
                try? await mentalHealthRepository.saveSession(session)
                loadData()
            }
        }
    }

    // Mood history helpers
    var moodHistoryByDay: [(date: Date, mood: MoodLevel)] {
        recentMoodEntries.map { ($0.timestamp, $0.mood) }
    }

    var averageMoodThisWeek: Double {
        let weekAgo = Calendar.current.date(byAdding: .day, value: -7, to: Date())!
        let weekMoods = recentMoodEntries.filter { $0.timestamp >= weekAgo }
        guard !weekMoods.isEmpty else { return 3.0 }
        return Double(weekMoods.map(\.mood.rawValue).reduce(0, +)) / Double(weekMoods.count)
    }
}

// MARK: - Ambient Sounds

enum AmbientSound: String, CaseIterable {
    case none = "None"
    case rain = "Rain"
    case ocean = "Ocean"
    case forest = "Forest"
    case whiteNoise = "White Noise"
    case singing = "Singing Bowls"

    var icon: String {
        switch self {
        case .none: return "speaker.slash"
        case .rain: return "cloud.rain"
        case .ocean: return "water.waves"
        case .forest: return "leaf"
        case .whiteNoise: return "waveform"
        case .singing: return "bell"
        }
    }
}

