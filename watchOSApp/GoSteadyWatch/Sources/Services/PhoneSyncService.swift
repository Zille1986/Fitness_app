import Foundation
import WatchConnectivity
import HealthKit
import Observation

@Observable
final class PhoneSyncService: NSObject, WCSessionDelegate {

    static let shared = PhoneSyncService()

    var isPhoneReachable = false
    var isActivated = false
    var receivedWorkoutPlan: [String: Any]?
    var userPreferences: [String: Any] = [:]
    var lastSyncDate: Date?

    private var wcSession: WCSession?
    private var pendingTransfers: [[String: Any]] = []

    private override init() {
        super.init()
    }

    // MARK: - Activation

    func activate() {
        guard WCSession.isSupported() else { return }
        wcSession = WCSession.default
        wcSession?.delegate = self
        wcSession?.activate()
    }

    // MARK: - Send Completed Workout

    func sendCompletedWorkout(_ workout: HKWorkout, activity: WatchActivityType) {
        var data: [String: Any] = [
            "type": "workout_complete",
            "activityType": activity.rawValue,
            "startTime": workout.startDate.timeIntervalSince1970,
            "endTime": workout.endDate.timeIntervalSince1970,
            "durationSeconds": workout.duration,
            "timestamp": Date().timeIntervalSince1970
        ]

        if let distance = workout.totalDistance {
            data["distanceMeters"] = distance.doubleValue(for: .meter())
        }
        if let energy = workout.totalEnergyBurned {
            data["caloriesBurned"] = energy.doubleValue(for: .kilocalorie())
        }
        if activity.isSwimming {
            if let strokes = workout.totalSwimmingStrokeCount {
                data["strokeCount"] = strokes.doubleValue(for: .count())
            }
            data["swimLocationType"] = activity == .poolSwim ? 1 : 2
        }

        sendToPhone(data)
    }

    // MARK: - Send Heart Rate (Real-time relay)

    func sendHeartRate(_ bpm: Int) {
        guard isPhoneReachable else { return }
        let data: [String: Any] = [
            "type": "heart_rate",
            "heartRate": bpm,
            "timestamp": Date().timeIntervalSince1970
        ]
        wcSession?.sendMessage(data, replyHandler: nil, errorHandler: nil)
    }

    // MARK: - Send Workout Started

    func sendWorkoutStarted(activity: WatchActivityType) {
        let data: [String: Any] = [
            "type": "workout_started",
            "activityType": activity.rawValue,
            "timestamp": Date().timeIntervalSince1970
        ]
        sendToPhone(data)
    }

    // MARK: - Send HIIT Session

    func sendHIITSession(_ sessionData: [String: Any]) {
        var data = sessionData
        data["type"] = "hiit_session"
        data["timestamp"] = Date().timeIntervalSince1970
        data["source"] = "watch"
        sendToPhone(data)
    }

    // MARK: - Request Sync

    func requestSync() {
        let data: [String: Any] = [
            "type": "sync_request",
            "timestamp": Date().timeIntervalSince1970
        ]
        sendToPhone(data)
    }

    // MARK: - Send User Preferences

    func sendPreferences(_ prefs: [String: Any]) {
        var data = prefs
        data["type"] = "preferences_update"
        data["timestamp"] = Date().timeIntervalSince1970
        sendToPhone(data)
    }

    // MARK: - Private Send

    private func sendToPhone(_ data: [String: Any]) {
        guard let session = wcSession, session.activationState == .activated else {
            pendingTransfers.append(data)
            return
        }

        if session.isReachable {
            session.sendMessage(data, replyHandler: { [weak self] reply in
                self?.lastSyncDate = Date()
            }, errorHandler: { [weak self] error in
                print("WCSession send error: \(error)")
                // Fall back to guaranteed delivery
                self?.wcSession?.transferUserInfo(data)
            })
        } else {
            session.transferUserInfo(data)
        }
    }

    // MARK: - WCSessionDelegate

    func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        DispatchQueue.main.async {
            self.isActivated = activationState == .activated
            self.isPhoneReachable = session.isReachable

            // Send any pending transfers
            if activationState == .activated {
                for data in self.pendingTransfers {
                    self.sendToPhone(data)
                }
                self.pendingTransfers.removeAll()
            }
        }
    }

    func sessionReachabilityDidChange(_ session: WCSession) {
        DispatchQueue.main.async {
            self.isPhoneReachable = session.isReachable
        }
    }

    // Receive messages from phone
    func session(
        _ session: WCSession,
        didReceiveMessage message: [String: Any],
        replyHandler: @escaping ([String: Any]) -> Void
    ) {
        handleMessage(message)
        replyHandler(["status": "received"])
    }

    func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        handleMessage(message)
    }

    func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any]) {
        handleMessage(userInfo)
    }

    func session(
        _ session: WCSession,
        didReceiveApplicationContext applicationContext: [String: Any]
    ) {
        handleMessage(applicationContext)
    }

    // MARK: - Handle Incoming

    private func handleMessage(_ message: [String: Any]) {
        DispatchQueue.main.async {
            guard let type = message["type"] as? String else { return }

            switch type {
            case "workout_plan":
                self.receivedWorkoutPlan = message
                NotificationCenter.default.post(
                    name: .phoneRequestedWorkout,
                    object: message
                )

            case "start_workout":
                NotificationCenter.default.post(
                    name: .phoneRequestedWorkout,
                    object: message
                )

            case "preferences":
                self.userPreferences = message
                NotificationCenter.default.post(
                    name: .preferencesUpdated,
                    object: message
                )

            case "profile_data":
                NotificationCenter.default.post(
                    name: .profileDataReceived,
                    object: message
                )

            default:
                break
            }
        }
    }
}

// MARK: - Notification Names

extension Notification.Name {
    static let phoneRequestedWorkout = Notification.Name("phoneRequestedWorkout")
    static let preferencesUpdated = Notification.Name("preferencesUpdated")
    static let profileDataReceived = Notification.Name("profileDataReceived")
}
