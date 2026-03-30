import Foundation
import Observation
import AVFoundation
import Combine

// MARK: - Audio Cue Types

enum AudioCueType {
    case distance(km: Double)
    case pace(secondsPerKm: Double)
    case split(km: Int, paceSecondsPerKm: Double)
    case heartRate(bpm: Int, zone: String)
    case elapsed(minutes: Int)
    case halfwayPoint
    case workoutComplete
    case intervalStart(name: String)
    case intervalRest
    case countdown(seconds: Int)
    case custom(text: String)
}

// MARK: - Audio Service

@Observable
final class AudioService: NSObject, AVSpeechSynthesizerDelegate {

    var isMuted: Bool = false
    var isSpeaking: Bool = false
    var volume: Float = 1.0

    private let synthesizer = AVSpeechSynthesizer()
    private var audioSession: AVAudioSession { AVAudioSession.sharedInstance() }
    private var speechQueue: [String] = []
    private var isProcessingQueue = false

    // HIIT audio cue support
    private var tonePlayer: AVAudioPlayer?

    override init() {
        super.init()
        synthesizer.delegate = self
        configureAudioSession()
    }

    // MARK: - Audio Session

    private func configureAudioSession() {
        do {
            try audioSession.setCategory(
                .playback,
                mode: .voicePrompt,
                options: [.duckOthers, .interruptSpokenAudioAndMixWithOthers]
            )
            try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            print("AudioService: Failed to configure audio session: \(error.localizedDescription)")
        }
    }

    func activateForBackground() {
        do {
            try audioSession.setCategory(
                .playback,
                mode: .voicePrompt,
                options: [.duckOthers, .interruptSpokenAudioAndMixWithOthers]
            )
            try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            print("AudioService: Failed to activate background audio: \(error.localizedDescription)")
        }
    }

    func deactivateAudioSession() {
        synthesizer.stopSpeaking(at: .immediate)
        speechQueue.removeAll()
        do {
            try audioSession.setActive(false, options: .notifyOthersOnDeactivation)
        } catch {
            print("AudioService: Failed to deactivate audio session: \(error.localizedDescription)")
        }
    }

    // MARK: - Voice Cues

    func speak(_ cue: AudioCueType) {
        guard !isMuted else { return }

        let text = textForCue(cue)
        enqueueSpeech(text)
    }

    func speakText(_ text: String) {
        guard !isMuted else { return }
        enqueueSpeech(text)
    }

    private func enqueueSpeech(_ text: String) {
        speechQueue.append(text)
        processQueue()
    }

    private func processQueue() {
        guard !isProcessingQueue, !speechQueue.isEmpty else { return }
        isProcessingQueue = true

        let text = speechQueue.removeFirst()

        let utterance = AVSpeechUtterance(string: text)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 1.1
        utterance.pitchMultiplier = 1.0
        utterance.volume = volume
        utterance.preUtteranceDelay = 0.1
        utterance.postUtteranceDelay = 0.2

        // Prefer a natural-sounding voice
        if let voice = AVSpeechSynthesisVoice(identifier: "com.apple.voice.premium.en-AU.Lee") {
            utterance.voice = voice
        } else if let voice = AVSpeechSynthesisVoice(language: "en-AU") {
            utterance.voice = voice
        } else {
            utterance.voice = AVSpeechSynthesisVoice(language: "en-US")
        }

        isSpeaking = true
        synthesizer.speak(utterance)
    }

    func stopSpeaking() {
        speechQueue.removeAll()
        synthesizer.stopSpeaking(at: .immediate)
        isSpeaking = false
        isProcessingQueue = false
    }

    // MARK: - Text Generation for Cues

    private func textForCue(_ cue: AudioCueType) -> String {
        switch cue {
        case .distance(let km):
            return formatDistanceCue(km: km)

        case .pace(let secondsPerKm):
            let paceStr = formatPace(secondsPerKm)
            return "Current pace: \(paceStr) per kilometre"

        case .split(let km, let paceSecondsPerKm):
            let paceStr = formatPace(paceSecondsPerKm)
            return "Kilometre \(km) complete. Split pace: \(paceStr)"

        case .heartRate(let bpm, let zone):
            return "Heart rate: \(bpm) beats per minute. \(zone)"

        case .elapsed(let minutes):
            if minutes >= 60 {
                let hours = minutes / 60
                let mins = minutes % 60
                if mins == 0 {
                    return "\(hours) hour\(hours > 1 ? "s" : "") elapsed"
                }
                return "\(hours) hour\(hours > 1 ? "s" : "") and \(mins) minute\(mins > 1 ? "s" : "") elapsed"
            }
            return "\(minutes) minute\(minutes > 1 ? "s" : "") elapsed"

        case .halfwayPoint:
            return "You're halfway there! Keep it up!"

        case .workoutComplete:
            return "Workout complete! Great job!"

        case .intervalStart(let name):
            return "Go! \(name)"

        case .intervalRest:
            return "Rest"

        case .countdown(let seconds):
            return "\(seconds)"

        case .custom(let text):
            return text
        }
    }

    private func formatDistanceCue(km: Double) -> String {
        if km == km.rounded() {
            return "\(Int(km)) kilometre\(Int(km) > 1 ? "s" : "")"
        }
        return String(format: "%.1f kilometres", km)
    }

    private func formatPace(_ secondsPerKm: Double) -> String {
        guard secondsPerKm > 0, secondsPerKm.isFinite else { return "unknown pace" }
        let minutes = Int(secondsPerKm) / 60
        let seconds = Int(secondsPerKm) % 60
        return "\(minutes) minutes \(seconds) seconds"
    }

    // MARK: - HIIT Audio Cues

    func onHIITTick(remainingSeconds: Int, isWorkPhase: Bool) {
        guard !isMuted else { return }

        switch remainingSeconds {
        case 3:
            playSystemSound(id: 1057) // low beep
        case 2:
            playSystemSound(id: 1057) // medium beep
        case 1:
            playSystemSound(id: 1058) // high beep
        case 0:
            if isWorkPhase {
                speak(.intervalRest)
            } else {
                speak(.intervalStart(name: "Go!"))
            }
        default:
            break
        }
    }

    func playGoTone() {
        guard !isMuted else { return }
        playSystemSound(id: 1025)
    }

    func playRestTone() {
        guard !isMuted else { return }
        playSystemSound(id: 1016)
    }

    func playCompleteTone() {
        guard !isMuted else { return }
        playSystemSound(id: 1025)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
            self?.playSystemSound(id: 1025)
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) { [weak self] in
            self?.playSystemSound(id: 1026)
        }
    }

    private func playSystemSound(id: SystemSoundID) {
        AudioServicesPlaySystemSound(id)
    }

    // MARK: - Auto-Cue Configuration

    struct AutoCueConfig {
        var distanceIntervalKm: Double = 1.0
        var timeIntervalMinutes: Int = 5
        var announcePace: Bool = true
        var announceDistance: Bool = true
        var announceHeartRate: Bool = false
        var announceSplits: Bool = true
        var announceElapsedTime: Bool = true
    }

    private var autoCueConfig = AutoCueConfig()
    private var lastDistanceCue: Double = 0
    private var lastTimeCue: TimeInterval = 0

    func configureAutoCues(_ config: AutoCueConfig) {
        autoCueConfig = config
    }

    func resetAutoCueTracking() {
        lastDistanceCue = 0
        lastTimeCue = 0
    }

    func checkAndAnnounce(
        distanceMeters: Double,
        durationSeconds: TimeInterval,
        paceSecondsPerKm: Double,
        heartRate: Int?,
        splits: [Double]
    ) {
        guard !isMuted else { return }

        let distanceKm = distanceMeters / 1000.0

        // Distance cues
        if autoCueConfig.announceDistance {
            let nextDistanceCue = lastDistanceCue + autoCueConfig.distanceIntervalKm
            if distanceKm >= nextDistanceCue {
                speak(.distance(km: nextDistanceCue))
                lastDistanceCue = nextDistanceCue

                if autoCueConfig.announcePace {
                    speak(.pace(secondsPerKm: paceSecondsPerKm))
                }
            }
        }

        // Split announcements
        if autoCueConfig.announceSplits && !splits.isEmpty {
            let km = splits.count
            if km > Int(lastDistanceCue) {
                speak(.split(km: km, paceSecondsPerKm: splits.last!))
            }
        }

        // Time cues
        if autoCueConfig.announceElapsedTime {
            let minuteInterval = Double(autoCueConfig.timeIntervalMinutes) * 60
            let nextTimeCue = lastTimeCue + minuteInterval
            if durationSeconds >= nextTimeCue && nextTimeCue > 0 {
                let minutes = Int(nextTimeCue) / 60
                speak(.elapsed(minutes: minutes))
                lastTimeCue = nextTimeCue
            }
        }

        // Heart rate cues
        if autoCueConfig.announceHeartRate, let hr = heartRate {
            let zone = heartRateZoneName(bpm: hr)
            speak(.heartRate(bpm: hr, zone: zone))
        }
    }

    private func heartRateZoneName(bpm: Int) -> String {
        // Simplified zone calculation (user should set max HR for accuracy)
        switch bpm {
        case ..<120: return "Recovery zone"
        case 120..<140: return "Aerobic zone"
        case 140..<160: return "Tempo zone"
        case 160..<175: return "Threshold zone"
        default: return "Maximum zone"
        }
    }

    // MARK: - AVSpeechSynthesizerDelegate

    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        isProcessingQueue = false

        if speechQueue.isEmpty {
            isSpeaking = false
        } else {
            processQueue()
        }
    }

    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
        isProcessingQueue = false
        isSpeaking = false
    }
}
