import Foundation
import WatchConnectivity
import HealthKit

/// Syncs completed workouts from Apple Watch to the iPhone app via WatchConnectivity.
class PhoneSyncManager: NSObject, ObservableObject, WCSessionDelegate {

    static let shared = PhoneSyncManager()

    @Published var isPhoneReachable = false

    private override init() {
        super.init()
        if WCSession.isSupported() {
            let session = WCSession.default
            session.delegate = self
            session.activate()
        }
    }

    // MARK: - Send Workout to Phone

    func sendWorkout(_ workout: HKWorkout, activityType: ActivityType) {
        guard WCSession.default.isReachable else {
            // Queue for later delivery
            sendAsUserInfo(workout, activityType: activityType)
            return
        }

        let data = workoutToDictionary(workout, activityType: activityType)

        WCSession.default.sendMessage(data, replyHandler: { reply in
            print("Phone acknowledged workout: \(reply)")
        }, errorHandler: { error in
            print("Failed to send workout via message: \(error)")
            // Fall back to transferUserInfo (guaranteed delivery)
            self.sendAsUserInfo(workout, activityType: activityType)
        })
    }

    private func sendAsUserInfo(_ workout: HKWorkout, activityType: ActivityType) {
        let data = workoutToDictionary(workout, activityType: activityType)
        WCSession.default.transferUserInfo(data)
    }

    private func workoutToDictionary(_ workout: HKWorkout, activityType: ActivityType) -> [String: Any] {
        var dict: [String: Any] = [
            "type": "workout_complete",
            "activityType": activityType.rawValue,
            "startTime": workout.startDate.timeIntervalSince1970,
            "endTime": workout.endDate.timeIntervalSince1970,
            "durationSeconds": workout.duration,
        ]

        // Distance
        if let distance = workout.totalDistance {
            dict["distanceMeters"] = distance.doubleValue(for: .meter())
        }

        // Calories
        if let energy = workout.totalEnergyBurned {
            dict["caloriesBurned"] = energy.doubleValue(for: .kilocalorie())
        }

        // Swimming specifics
        if activityType == .swimming {
            if let laps = workout.totalSwimmingStrokeCount {
                dict["strokeCount"] = laps.doubleValue(for: .count())
            }
            dict["swimLocationType"] = workout.workoutConfiguration.swimmingLocationType.rawValue
            if let lapLength = workout.workoutConfiguration.lapLength {
                dict["poolLengthMeters"] = lapLength.doubleValue(for: .meter())
            }
        }

        return dict
    }

    // MARK: - WCSessionDelegate

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {
        DispatchQueue.main.async {
            self.isPhoneReachable = session.isReachable
        }
    }

    func sessionReachabilityDidChange(_ session: WCSession) {
        DispatchQueue.main.async {
            self.isPhoneReachable = session.isReachable
        }
    }

    // Receive messages from phone (e.g., start workout command)
    func session(_ session: WCSession, didReceiveMessage message: [String: Any], replyHandler: @escaping ([String: Any]) -> Void) {
        if let command = message["command"] as? String {
            switch command {
            case "start_workout":
                // Phone requested workout start
                NotificationCenter.default.post(name: .phoneRequestedWorkout, object: message)
            default:
                break
            }
        }
        replyHandler(["status": "received"])
    }
}

extension Notification.Name {
    static let phoneRequestedWorkout = Notification.Name("phoneRequestedWorkout")
}
