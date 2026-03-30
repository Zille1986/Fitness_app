import Foundation
import Observation
import WatchConnectivity
import Combine

// MARK: - Sync Message Types

enum WatchMessageType: String {
    case workoutData = "workout_data"
    case heartRate = "heart_rate"
    case command = "command"
    case preferences = "preferences"
    case gamification = "gamification"
    case trainingPlan = "training_plan"
    case workoutHistory = "workout_history"
    case customWorkouts = "custom_workouts"
}

enum WatchCommand: String {
    case startRun = "start"
    case pauseRun = "pause"
    case resumeRun = "resume"
    case stopRun = "stop"
}

// MARK: - Watch Workout Data

struct WatchWorkoutData: Codable {
    let startTime: Date
    let endTime: Date?
    let distanceMeters: Double
    let durationSeconds: Int
    let avgHeartRate: Int?
    let maxHeartRate: Int?
    let calories: Int?
    let routePoints: [WatchRoutePoint]
    let workoutType: String

    struct WatchRoutePoint: Codable {
        let latitude: Double
        let longitude: Double
        let altitude: Double?
        let timestamp: Date
    }
}

// MARK: - Watch Sync Service

@Observable
final class WatchSyncService: NSObject, WCSessionDelegate {

    var isWatchConnected: Bool = false
    var isWatchAppInstalled: Bool = false
    var watchName: String?
    var lastSyncDate: Date?
    var lastError: String?

    // Incoming data subjects
    let receivedWorkoutSubject = PassthroughSubject<WatchWorkoutData, Never>()
    let receivedHeartRateSubject = PassthroughSubject<Int, Never>()
    let receivedCommandSubject = PassthroughSubject<WatchCommand, Never>()

    private var wcSession: WCSession?

    override init() {
        super.init()
        if WCSession.isSupported() {
            wcSession = WCSession.default
            wcSession?.delegate = self
            wcSession?.activate()
        }
    }

    // MARK: - Connection Status

    var isReachable: Bool {
        wcSession?.isReachable ?? false
    }

    var isPaired: Bool {
        wcSession?.isPaired ?? false
    }

    // MARK: - Send Workout to Watch

    func sendWorkoutToWatch(workout: [String: Any], autoStart: Bool = false) async -> Bool {
        guard let session = wcSession, session.isReachable else {
            lastError = "Watch is not reachable"
            return false
        }

        var message = workout
        message["type"] = WatchMessageType.workoutData.rawValue
        message["auto_start"] = autoStart
        message["timestamp"] = Date().timeIntervalSince1970

        return await withCheckedContinuation { continuation in
            session.sendMessage(message, replyHandler: { _ in
                continuation.resume(returning: true)
            }, errorHandler: { [weak self] error in
                self?.lastError = error.localizedDescription
                continuation.resume(returning: false)
            })
        }
    }

    // MARK: - Send Training Plan to Watch

    func sendTrainingPlanToWatch(plan: [String: Any]) -> Bool {
        guard let session = wcSession, session.activationState == .activated else {
            lastError = "Watch session not activated"
            return false
        }

        var context = session.applicationContext
        context["training_plan"] = plan
        context["type"] = WatchMessageType.trainingPlan.rawValue
        context["timestamp"] = Date().timeIntervalSince1970

        do {
            try session.updateApplicationContext(context)
            return true
        } catch {
            lastError = error.localizedDescription
            return false
        }
    }

    // MARK: - Send Commands

    func sendCommand(_ command: WatchCommand) async -> Bool {
        guard let session = wcSession, session.isReachable else {
            lastError = "Watch is not reachable"
            return false
        }

        let message: [String: Any] = [
            "type": WatchMessageType.command.rawValue,
            "command": command.rawValue,
            "timestamp": Date().timeIntervalSince1970
        ]

        return await withCheckedContinuation { continuation in
            session.sendMessage(message, replyHandler: { _ in
                continuation.resume(returning: true)
            }, errorHandler: { [weak self] error in
                self?.lastError = error.localizedDescription
                continuation.resume(returning: false)
            })
        }
    }

    func sendStartRunCommand() async -> Bool {
        await sendCommand(.startRun)
    }

    func sendPauseRunCommand() async -> Bool {
        await sendCommand(.pauseRun)
    }

    func sendResumeRunCommand() async -> Bool {
        await sendCommand(.resumeRun)
    }

    func sendStopRunCommand() async -> Bool {
        await sendCommand(.stopRun)
    }

    // MARK: - Sync Preferences

    func syncPreferences(_ preferences: [String: Any]) -> Bool {
        guard let session = wcSession, session.activationState == .activated else {
            lastError = "Watch session not activated"
            return false
        }

        var context = (try? session.applicationContext) ?? [:]
        context["preferences"] = preferences
        context["type"] = WatchMessageType.preferences.rawValue
        context["timestamp"] = Date().timeIntervalSince1970

        do {
            try session.updateApplicationContext(context)
            return true
        } catch {
            lastError = error.localizedDescription
            return false
        }
    }

    // MARK: - Sync Gamification Data

    func syncGamificationToWatch(
        currentStreak: Int,
        longestStreak: Int,
        totalXP: Int,
        currentLevel: Int,
        moveProgress: Float,
        exerciseProgress: Float,
        distanceProgress: Float
    ) -> Bool {
        guard let session = wcSession, session.activationState == .activated else {
            lastError = "Watch session not activated"
            return false
        }

        let gamificationData: [String: Any] = [
            "currentStreak": currentStreak,
            "longestStreak": longestStreak,
            "totalXp": totalXP,
            "currentLevel": currentLevel,
            "moveProgress": moveProgress,
            "exerciseProgress": exerciseProgress,
            "distanceProgress": distanceProgress
        ]

        var context = (try? session.applicationContext) ?? [:]
        context["gamification"] = gamificationData
        context["type"] = WatchMessageType.gamification.rawValue
        context["timestamp"] = Date().timeIntervalSince1970

        do {
            try session.updateApplicationContext(context)
            return true
        } catch {
            lastError = error.localizedDescription
            return false
        }
    }

    // MARK: - Sync Custom Workouts

    func syncCustomWorkoutsToWatch(workouts: [[String: Any]]) -> Bool {
        guard let session = wcSession, session.activationState == .activated else {
            lastError = "Watch session not activated"
            return false
        }

        var context = (try? session.applicationContext) ?? [:]
        context["custom_workouts"] = workouts
        context["type"] = WatchMessageType.customWorkouts.rawValue
        context["timestamp"] = Date().timeIntervalSince1970

        do {
            try session.updateApplicationContext(context)
            return true
        } catch {
            lastError = error.localizedDescription
            return false
        }
    }

    // MARK: - Transfer Workout History (Large data via file transfer)

    func transferWorkoutHistory(data: Data) {
        guard let session = wcSession, session.activationState == .activated else {
            lastError = "Watch session not activated"
            return
        }

        let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent("workout_history.json")
        do {
            try data.write(to: tempURL)
            session.transferFile(tempURL, metadata: [
                "type": WatchMessageType.workoutHistory.rawValue,
                "timestamp": Date().timeIntervalSince1970
            ])
        } catch {
            lastError = error.localizedDescription
        }
    }

    // MARK: - WCSessionDelegate

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        Task { @MainActor in
            if let error {
                self.lastError = error.localizedDescription
            }
            self.isWatchConnected = activationState == .activated
            self.isWatchAppInstalled = session.isWatchAppInstalled
            if session.isPaired {
                self.watchName = "Apple Watch"
            }
        }
    }

    func sessionDidBecomeInactive(_ session: WCSession) {
        Task { @MainActor in
            self.isWatchConnected = false
        }
    }

    func sessionDidDeactivate(_ session: WCSession) {
        Task { @MainActor in
            self.isWatchConnected = false
        }
        // Reactivate for watch switching
        session.activate()
    }

    func sessionWatchStateDidChange(_ session: WCSession) {
        Task { @MainActor in
            self.isWatchConnected = session.activationState == .activated
            self.isWatchAppInstalled = session.isWatchAppInstalled
        }
    }

    func sessionReachabilityDidChange(_ session: WCSession) {
        Task { @MainActor in
            self.isWatchConnected = session.isReachable
        }
    }

    // MARK: - Receive Messages

    func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        handleIncomingMessage(message)
    }

    func session(_ session: WCSession, didReceiveMessage message: [String: Any], replyHandler: @escaping ([String: Any]) -> Void) {
        handleIncomingMessage(message)
        replyHandler(["status": "received"])
    }

    private func handleIncomingMessage(_ message: [String: Any]) {
        guard let typeString = message["type"] as? String,
              let type = WatchMessageType(rawValue: typeString) else { return }

        switch type {
        case .heartRate:
            if let bpm = message["bpm"] as? Int {
                receivedHeartRateSubject.send(bpm)
            }

        case .command:
            if let cmdString = message["command"] as? String,
               let command = WatchCommand(rawValue: cmdString) {
                receivedCommandSubject.send(command)
            }

        case .workoutData:
            if let workoutData = decodeWorkoutData(from: message) {
                receivedWorkoutSubject.send(workoutData)
                Task { @MainActor in
                    self.lastSyncDate = Date()
                }
            }

        default:
            break
        }
    }

    // MARK: - Receive Application Context

    func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        handleIncomingMessage(applicationContext)
    }

    // MARK: - Receive User Info

    func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any]) {
        handleIncomingMessage(userInfo)
    }

    // MARK: - Receive File Transfers

    func session(_ session: WCSession, didReceive file: WCSessionFile) {
        guard let metadata = file.metadata,
              let typeString = metadata["type"] as? String else { return }

        if typeString == WatchMessageType.workoutHistory.rawValue {
            do {
                let data = try Data(contentsOf: file.fileURL)
                if let workouts = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
                    for workoutDict in workouts {
                        if let workoutData = decodeWorkoutData(from: workoutDict) {
                            receivedWorkoutSubject.send(workoutData)
                        }
                    }
                    Task { @MainActor in
                        self.lastSyncDate = Date()
                    }
                }
            } catch {
                Task { @MainActor in
                    self.lastError = error.localizedDescription
                }
            }
        }
    }

    // MARK: - Decode Helper

    private func decodeWorkoutData(from dict: [String: Any]) -> WatchWorkoutData? {
        guard let startTimeInterval = dict["start_time"] as? TimeInterval,
              let distanceMeters = dict["distance_meters"] as? Double,
              let durationSeconds = dict["duration_seconds"] as? Int,
              let workoutType = dict["workout_type"] as? String else { return nil }

        let endTimeInterval = dict["end_time"] as? TimeInterval
        let avgHeartRate = dict["avg_heart_rate"] as? Int
        let maxHeartRate = dict["max_heart_rate"] as? Int
        let calories = dict["calories"] as? Int

        var routePoints: [WatchWorkoutData.WatchRoutePoint] = []
        if let pointsArray = dict["route_points"] as? [[String: Any]] {
            routePoints = pointsArray.compactMap { pointDict in
                guard let lat = pointDict["latitude"] as? Double,
                      let lon = pointDict["longitude"] as? Double,
                      let ts = pointDict["timestamp"] as? TimeInterval else { return nil }
                return WatchWorkoutData.WatchRoutePoint(
                    latitude: lat,
                    longitude: lon,
                    altitude: pointDict["altitude"] as? Double,
                    timestamp: Date(timeIntervalSince1970: ts)
                )
            }
        }

        return WatchWorkoutData(
            startTime: Date(timeIntervalSince1970: startTimeInterval),
            endTime: endTimeInterval.map { Date(timeIntervalSince1970: $0) },
            distanceMeters: distanceMeters,
            durationSeconds: durationSeconds,
            avgHeartRate: avgHeartRate,
            maxHeartRate: maxHeartRate,
            calories: calories,
            routePoints: routePoints,
            workoutType: workoutType
        )
    }
}
