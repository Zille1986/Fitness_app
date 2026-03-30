import SwiftUI
import SwiftData
import Combine
import AVFoundation

// MARK: - Active Run ViewModel

@Observable
final class ActiveRunViewModel {

    // MARK: - State

    var trackingState = TrackingState()
    var runState: RunPhase = .ready
    var currentSplitIndex: Int = 0
    var lastSplitPace: String = "--:--"
    var showSplitNotification = false
    var splitNotificationText = ""
    var heartRateHistory: [(time: TimeInterval, bpm: Int)] = []
    var paceHistory: [(time: TimeInterval, pace: Double)] = []
    var countdownValue: Int? = nil
    var isLocationPermissionGranted = false
    var showFinishConfirmation = false
    var finishedRunId: UUID?

    // Audio cue settings
    var audioCuesEnabled = true
    var audioCueDistanceInterval: Double = 1.0 // km
    var lastAudioCueDistance: Double = 0

    // MARK: - Dependencies

    private let locationService = LocationService()
    private let synthesizer = AVSpeechSynthesizer()
    private var modelContext: ModelContext?
    private var timerCancellable: AnyCancellable?
    private var locationCancellable: AnyCancellable?
    private var splitCheckTimer: AnyCancellable?

    enum RunPhase {
        case ready
        case countdown
        case running
        case paused
        case finished
    }

    // MARK: - Init

    init() {
        isLocationPermissionGranted = locationService.hasLocationPermission
    }

    func configure(modelContext: ModelContext) {
        self.modelContext = modelContext
    }

    // MARK: - Permissions

    func requestLocationPermission() {
        locationService.requestAlwaysAuthorization()
        // Observe auth changes
        timerCancellable = Timer.publish(every: 0.5, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self else { return }
                self.isLocationPermissionGranted = self.locationService.hasLocationPermission
                if self.isLocationPermissionGranted {
                    self.timerCancellable?.cancel()
                }
            }
    }

    // MARK: - Run Controls

    func startRun() {
        runState = .countdown
        countdownValue = 3

        Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] timer in
            guard let self else { timer.invalidate(); return }

            if let count = self.countdownValue {
                if count > 1 {
                    self.countdownValue = count - 1
                } else {
                    self.countdownValue = nil
                    timer.invalidate()
                    self.beginTracking()
                }
            }
        }
    }

    private func beginTracking() {
        runState = .running
        locationService.startTracking()
        lastAudioCueDistance = 0
        currentSplitIndex = 0

        // Observe tracking state changes
        splitCheckTimer = Timer.publish(every: 1.0, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                self?.updateFromLocationService()
            }

        speakCue("Run started. Good luck.")
    }

    private func updateFromLocationService() {
        trackingState = locationService.trackingState

        // Check for new splits
        let completedKm = Int(trackingState.distanceKm)
        if completedKm > currentSplitIndex && !trackingState.splits.isEmpty {
            currentSplitIndex = completedKm
            let splitPace = trackingState.splits.last ?? 0
            let mins = Int(splitPace) / 60
            let secs = Int(splitPace) % 60
            lastSplitPace = String(format: "%d:%02d", mins, secs)

            showSplitAlert(km: completedKm, pace: lastSplitPace)
        }

        // Record heart rate history
        if let hr = trackingState.currentHeartRate {
            heartRateHistory.append((time: trackingState.durationSeconds, bpm: hr))
        }

        // Record pace history (every 10s)
        if trackingState.currentPaceSecondsPerKm > 0 && trackingState.currentPaceSecondsPerKm.isFinite {
            if paceHistory.isEmpty || (trackingState.durationSeconds - (paceHistory.last?.time ?? 0)) >= 10 {
                paceHistory.append((time: trackingState.durationSeconds, pace: trackingState.currentPaceSecondsPerKm))
            }
        }

        // Audio cues
        checkAudioCue()
    }

    func pauseRun() {
        locationService.pauseTracking()
        runState = .paused
        trackingState = locationService.trackingState
        speakCue("Run paused.")
    }

    func resumeRun() {
        locationService.resumeTracking()
        runState = .running
        trackingState = locationService.trackingState
        speakCue("Run resumed.")
    }

    func addLap() {
        // Manually add a split at the current distance
        let currentDist = trackingState.distanceMeters
        let currentTime = trackingState.durationSeconds

        if currentDist > 0 && currentTime > 0 {
            let pace = currentTime / (currentDist / 1000.0)
            trackingState.splits.append(pace)

            let mins = Int(pace) / 60
            let secs = Int(pace) % 60
            lastSplitPace = String(format: "%d:%02d", mins, secs)
            showSplitAlert(km: trackingState.splits.count, pace: lastSplitPace)
        }
    }

    func stopRun() {
        let finalState = locationService.stopTracking()
        splitCheckTimer?.cancel()
        runState = .finished

        saveRun(from: finalState)
        speakCue("Run completed. Great work!")
    }

    // MARK: - Save Run

    private func saveRun(from state: TrackingState) {
        guard let modelContext else { return }

        let now = Date()
        let startTime = now.addingTimeInterval(-state.durationSeconds)
        let avgPace = state.distanceMeters > 0
            ? state.durationSeconds / (state.distanceMeters / 1000.0)
            : 0.0

        let splits = state.splits.enumerated().map { index, pace in
            Split(
                kilometer: index + 1,
                durationMillis: Int64(pace * 1000.0 / 60.0 * 60000.0),
                paceSecondsPerKm: pace,
                elevationChange: 0.0,
                avgHeartRate: nil
            )
        }

        let run = Run(
            startTime: startTime,
            endTime: now,
            distanceMeters: state.distanceMeters,
            durationMillis: Int64(state.durationSeconds * 1000),
            avgPaceSecondsPerKm: avgPace,
            maxPaceSecondsPerKm: state.splits.min() ?? avgPace,
            avgHeartRate: heartRateHistory.isEmpty ? nil : heartRateHistory.reduce(0) { $0 + $1.bpm } / heartRateHistory.count,
            maxHeartRate: heartRateHistory.map(\.bpm).max(),
            elevationGainMeters: state.elevationGain,
            elevationLossMeters: state.elevationLoss,
            splits: splits,
            source: .phone,
            isCompleted: true
        )

        modelContext.insert(run)

        do {
            try modelContext.save()
            finishedRunId = run.id
        } catch {
            print("Failed to save run: \(error)")
        }
    }

    // MARK: - Audio Cues

    private func checkAudioCue() {
        guard audioCuesEnabled else { return }

        let currentKm = trackingState.distanceKm
        if currentKm - lastAudioCueDistance >= audioCueDistanceInterval {
            lastAudioCueDistance = Double(Int(currentKm / audioCueDistanceInterval)) * audioCueDistanceInterval
            let pace = trackingState.paceFormatted
            let distance = String(format: "%.1f", currentKm)
            let time = trackingState.durationFormatted
            speakCue("\(distance) kilometers. Pace: \(pace) per kilometer. Time: \(time).")
        }
    }

    private func speakCue(_ text: String) {
        guard audioCuesEnabled else { return }
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: "en-AU")
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 1.1
        utterance.volume = 0.8
        synthesizer.speak(utterance)
    }

    // MARK: - Split Notification

    private func showSplitAlert(km: Int, pace: String) {
        splitNotificationText = "Km \(km): \(pace) /km"
        showSplitNotification = true

        DispatchQueue.main.asyncAfter(deadline: .now() + 3) { [weak self] in
            self?.showSplitNotification = false
        }
    }

    // MARK: - Cleanup

    func cleanup() {
        splitCheckTimer?.cancel()
        timerCancellable?.cancel()
        locationCancellable?.cancel()
        if trackingState.isTracking {
            _ = locationService.stopTracking()
        }
    }
}
