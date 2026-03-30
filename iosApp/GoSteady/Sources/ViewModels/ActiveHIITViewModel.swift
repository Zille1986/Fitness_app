import Foundation
import Observation
import AVFoundation
import UIKit

enum HIITPhase: String {
    case warmup = "WARM UP"
    case work = "WORK"
    case rest = "REST"
    case cooldown = "COOL DOWN"
    case complete = "COMPLETE"
    case paused = "PAUSED"
}

@Observable
final class ActiveHIITViewModel {
    // MARK: - State
    var template: HIITWorkoutTemplate?
    var phase: HIITPhase = .warmup
    var currentExerciseIndex: Int = 0
    var currentRound: Int = 1
    var remainingSeconds: Int = 0
    var phaseDurationSeconds: Int = 0
    var totalElapsedMs: Int64 = 0
    var isPaused = false
    var isComplete = false
    var currentExerciseName: String = "Get Ready!"
    var currentExerciseDescription: String = ""
    var nextExerciseName: String? = nil
    var caloriesEstimate: Int = 0
    var savedSessionId: String? = nil
    var phaseStepIndex: Int = 0
    var phaseStepCount: Int = 0

    // Derived
    var phaseProgress: Float {
        guard phaseDurationSeconds > 0 else { return 0 }
        return 1.0 - (Float(remainingSeconds) / Float(phaseDurationSeconds))
    }

    var roundProgress: String {
        "Round \(currentRound)/\(template?.rounds ?? 0)"
    }

    var exerciseProgress: String {
        "\(currentExerciseIndex + 1)/\(template?.exercises.count ?? 0)"
    }

    var remainingFormatted: String {
        let mins = remainingSeconds / 60
        let secs = remainingSeconds % 60
        return String(format: "%d:%02d", mins, secs)
    }

    var totalElapsedFormatted: String {
        let totalSeconds = totalElapsedMs / 1000
        let mins = (totalSeconds % 3600) / 60
        let secs = totalSeconds % 60
        return String(format: "%d:%02d", mins, secs)
    }

    // MARK: - Private
    private var timer: Timer?
    private var startTimeMs: Date?
    private var pausedAccumulatedMs: TimeInterval = 0
    private var pauseStartMs: Date?
    private var prePausePhase: HIITPhase = .warmup

    // Audio
    private var audioPlayer: AVAudioPlayer?
    private let synthesizer = AVSpeechSynthesizer()

    private var hiitRepository: HIITRepository?

    init() {
        configureAudioSession()
    }

    func configure(hiitRepository: HIITRepository) {
        self.hiitRepository = hiitRepository
    }

    // MARK: - Start

    func startWorkout(templateId: String) {
        guard let template = HIITExerciseLibrary.getTemplateById(templateId) else { return }
        self.template = template

        let firstStep = template.warmupSteps.first
        phase = .warmup
        remainingSeconds = template.warmupSec
        phaseDurationSeconds = template.warmupSec
        currentExerciseName = firstStep?.name ?? "Warm Up"
        currentExerciseDescription = firstStep?.stepDescription ?? "Get your body ready"
        nextExerciseName = template.exercises.first?.exercise.name
        phaseStepIndex = 0
        phaseStepCount = template.warmupSteps.count

        playPhaseTone()
        startTimer()
    }

    func startWorkoutWithTemplate(_ template: HIITWorkoutTemplate) {
        self.template = template

        let firstStep = template.warmupSteps.first
        phase = .warmup
        remainingSeconds = template.warmupSec
        phaseDurationSeconds = template.warmupSec
        currentExerciseName = firstStep?.name ?? "Warm Up"
        currentExerciseDescription = firstStep?.stepDescription ?? "Get your body ready"
        nextExerciseName = template.exercises.first?.exercise.name
        phaseStepIndex = 0
        phaseStepCount = template.warmupSteps.count

        playPhaseTone()
        startTimer()
    }

    // MARK: - Controls

    func togglePause() {
        if isComplete { return }

        if isPaused {
            // Resume
            if let pauseStart = pauseStartMs {
                pausedAccumulatedMs += Date().timeIntervalSince(pauseStart)
            }
            isPaused = false
            phase = prePausePhase
        } else {
            // Pause
            pauseStartMs = Date()
            prePausePhase = phase
            isPaused = true
            phase = .paused
        }
    }

    func stopWorkout() {
        timer?.invalidate()
        let elapsed = elapsedSinceStart()
        totalElapsedMs = Int64(elapsed * 1000)
        phase = .complete
        isComplete = true
        saveSession()
    }

    // MARK: - Timer

    private func startTimer() {
        startTimeMs = Date()
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.tick()
        }
    }

    private func tick() {
        guard !isPaused, !isComplete else { return }

        let newRemaining = remainingSeconds - 1
        let elapsed = elapsedSinceStart()
        totalElapsedMs = Int64(elapsed * 1000)

        // Audio cues at 3, 2, 1
        if newRemaining <= 3 && newRemaining > 0 {
            playCountdownTick()
        }

        // Calorie estimate
        let calPerSec: Double
        switch phase {
        case .work: calPerSec = 10.0 / 60.0
        case .rest: calPerSec = 4.0 / 60.0
        default: calPerSec = 3.0 / 60.0
        }
        let newCal = Int(elapsed * calPerSec)
        if newCal > caloriesEstimate {
            caloriesEstimate = newCal
        }

        if newRemaining <= 0 {
            advancePhase()
        } else {
            remainingSeconds = newRemaining
            advancePhaseStepIfNeeded(newRemaining)
        }
    }

    private func advancePhaseStepIfNeeded(_ remaining: Int) {
        guard let template else { return }
        let steps: [HIITPhaseStep]
        switch phase {
        case .warmup: steps = template.warmupSteps
        case .cooldown: steps = template.cooldownSteps
        default: return
        }
        guard !steps.isEmpty else { return }

        let totalSec = phaseDurationSeconds
        let elapsedSec = totalSec - remaining
        let secPerStep = totalSec / steps.count
        guard secPerStep > 0 else { return }
        let newIndex = min(elapsedSec / secPerStep, steps.count - 1)

        if newIndex != phaseStepIndex {
            let step = steps[newIndex]
            phaseStepIndex = newIndex
            currentExerciseName = step.name
            currentExerciseDescription = step.stepDescription
        }
    }

    private func advancePhase() {
        guard let template else { return }

        switch phase {
        case .warmup:
            // Start first exercise
            let firstExercise = template.exercises[0]
            phase = .work
            currentExerciseIndex = 0
            remainingSeconds = firstExercise.durationOverrideSec ?? template.workDurationSec
            phaseDurationSeconds = remainingSeconds
            currentExerciseName = firstExercise.exercise.name
            currentExerciseDescription = firstExercise.exercise.exerciseDescription
            nextExerciseName = template.exercises.count > 1 ? template.exercises[1].exercise.name : nil
            playPhaseTone()
            announceExercise(firstExercise.exercise.name)

        case .work:
            let nextExIndex = currentExerciseIndex + 1
            if nextExIndex < template.exercises.count {
                // Rest between exercises
                phase = .rest
                remainingSeconds = template.restDurationSec
                phaseDurationSeconds = template.restDurationSec
                currentExerciseName = "Rest"
                currentExerciseDescription = "Catch your breath"
                nextExerciseName = template.exercises[nextExIndex].exercise.name
                playPhaseTone()
            } else if currentRound < template.rounds {
                // Rest before next round
                phase = .rest
                remainingSeconds = template.restDurationSec
                phaseDurationSeconds = template.restDurationSec
                currentExerciseName = "Rest"
                currentExerciseDescription = "Round \(currentRound) complete!"
                nextExerciseName = template.exercises[0].exercise.name
                playPhaseTone()
            } else {
                // All rounds done -> cooldown
                let firstCooldown = template.cooldownSteps.first
                phase = .cooldown
                remainingSeconds = template.cooldownSec
                phaseDurationSeconds = template.cooldownSec
                currentExerciseName = firstCooldown?.name ?? "Cool Down"
                currentExerciseDescription = firstCooldown?.stepDescription ?? "Stretch and breathe"
                nextExerciseName = nil
                phaseStepIndex = 0
                phaseStepCount = template.cooldownSteps.count
                playPhaseTone()
            }

        case .rest:
            let nextExIndex = currentExerciseIndex + 1
            let newIndex: Int
            let newRound: Int
            if nextExIndex < template.exercises.count {
                newIndex = nextExIndex
                newRound = currentRound
            } else {
                newIndex = 0
                newRound = currentRound + 1
            }
            let exercise = template.exercises[newIndex]
            let afterNext = (newIndex + 1 < template.exercises.count) ? template.exercises[newIndex + 1].exercise.name : nil

            phase = .work
            currentExerciseIndex = newIndex
            currentRound = newRound
            remainingSeconds = exercise.durationOverrideSec ?? template.workDurationSec
            phaseDurationSeconds = remainingSeconds
            currentExerciseName = exercise.exercise.name
            currentExerciseDescription = exercise.exercise.exerciseDescription
            nextExerciseName = afterNext
            playPhaseTone()
            announceExercise(exercise.exercise.name)

        case .cooldown:
            // Complete
            timer?.invalidate()
            let elapsed = elapsedSinceStart()
            totalElapsedMs = Int64(elapsed * 1000)
            phase = .complete
            isComplete = true
            remainingSeconds = 0
            currentExerciseName = "Workout Complete!"
            currentExerciseDescription = ""
            playCompleteTone()
            saveSession()

        default:
            break
        }
    }

    // MARK: - Persistence

    private func saveSession() {
        guard let template else { return }

        let sessionId = UUID()
        let session = HIITSession(
            id: sessionId,
            date: Date(),
            templateId: template.id,
            templateName: template.name,
            totalDurationMs: totalElapsedMs,
            exerciseCount: template.exercises.count,
            roundsCompleted: currentRound,
            totalRounds: template.rounds,
            caloriesEstimate: caloriesEstimate,
            isCompleted: currentRound >= template.rounds
        )

        hiitRepository?.insert(session)
        self.savedSessionId = sessionId.uuidString
    }

    // MARK: - Audio

    private func configureAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            // Audio session setup failed, non-fatal
        }
    }

    private func playPhaseTone() {
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)
        playSystemSound(1007) // standard tone
    }

    private func playCountdownTick() {
        let generator = UIImpactFeedbackGenerator(style: .medium)
        generator.impactOccurred()
        playSystemSound(1057) // tick sound
    }

    private func playCompleteTone() {
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)
        playSystemSound(1025) // celebration-like tone
    }

    private func playSystemSound(_ soundID: UInt32) {
        AudioServicesPlaySystemSound(SystemSoundID(soundID))
    }

    private func announceExercise(_ name: String) {
        let utterance = AVSpeechUtterance(string: name)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate
        utterance.volume = 0.8
        synthesizer.speak(utterance)
    }

    // MARK: - Helpers

    private func elapsedSinceStart() -> TimeInterval {
        guard let start = startTimeMs else { return 0 }
        return Date().timeIntervalSince(start) - pausedAccumulatedMs
    }

    func cleanup() {
        timer?.invalidate()
        synthesizer.stopSpeaking(at: .immediate)
    }
}
