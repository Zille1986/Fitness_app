import Foundation
import Observation
import UserNotifications

// MARK: - Notification Categories

enum NotificationCategory: String {
    case workoutReminder = "WORKOUT_REMINDER"
    case achievementUnlocked = "ACHIEVEMENT_UNLOCKED"
    case streakReminder = "STREAK_REMINDER"
    case restTimer = "REST_TIMER"
    case trainingPlan = "TRAINING_PLAN"
    case weeklyReport = "WEEKLY_REPORT"
}

enum NotificationAction: String {
    case startWorkout = "START_WORKOUT"
    case dismiss = "DISMISS"
    case snooze = "SNOOZE"
    case viewDetails = "VIEW_DETAILS"
}

// MARK: - Notification Service

@Observable
final class NotificationService: NSObject, UNUserNotificationCenterDelegate {

    var isAuthorized: Bool = false
    var pendingNotificationCount: Int = 0

    private let center = UNUserNotificationCenter.current()

    override init() {
        super.init()
        center.delegate = self
        registerCategories()
        checkAuthorizationStatus()
    }

    // MARK: - Authorization

    func requestAuthorization() async throws {
        let granted = try await center.requestAuthorization(options: [.alert, .sound, .badge, .provisional])
        await MainActor.run {
            self.isAuthorized = granted
        }
    }

    func checkAuthorizationStatus() {
        center.getNotificationSettings { [weak self] settings in
            Task { @MainActor in
                self?.isAuthorized = settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional
            }
        }
    }

    // MARK: - Register Categories & Actions

    private func registerCategories() {
        let startAction = UNNotificationAction(
            identifier: NotificationAction.startWorkout.rawValue,
            title: "Start Workout",
            options: [.foreground]
        )

        let dismissAction = UNNotificationAction(
            identifier: NotificationAction.dismiss.rawValue,
            title: "Dismiss",
            options: [.destructive]
        )

        let snoozeAction = UNNotificationAction(
            identifier: NotificationAction.snooze.rawValue,
            title: "Remind in 30 min",
            options: []
        )

        let viewAction = UNNotificationAction(
            identifier: NotificationAction.viewDetails.rawValue,
            title: "View Details",
            options: [.foreground]
        )

        let workoutCategory = UNNotificationCategory(
            identifier: NotificationCategory.workoutReminder.rawValue,
            actions: [startAction, snoozeAction, dismissAction],
            intentIdentifiers: [],
            options: []
        )

        let achievementCategory = UNNotificationCategory(
            identifier: NotificationCategory.achievementUnlocked.rawValue,
            actions: [viewAction, dismissAction],
            intentIdentifiers: [],
            options: []
        )

        let streakCategory = UNNotificationCategory(
            identifier: NotificationCategory.streakReminder.rawValue,
            actions: [startAction, dismissAction],
            intentIdentifiers: [],
            options: []
        )

        let restTimerCategory = UNNotificationCategory(
            identifier: NotificationCategory.restTimer.rawValue,
            actions: [dismissAction],
            intentIdentifiers: [],
            options: []
        )

        let trainingPlanCategory = UNNotificationCategory(
            identifier: NotificationCategory.trainingPlan.rawValue,
            actions: [startAction, viewAction, dismissAction],
            intentIdentifiers: [],
            options: []
        )

        let weeklyReportCategory = UNNotificationCategory(
            identifier: NotificationCategory.weeklyReport.rawValue,
            actions: [viewAction, dismissAction],
            intentIdentifiers: [],
            options: []
        )

        center.setNotificationCategories([
            workoutCategory,
            achievementCategory,
            streakCategory,
            restTimerCategory,
            trainingPlanCategory,
            weeklyReportCategory
        ])
    }

    // MARK: - Workout Reminders

    func scheduleWorkoutReminder(
        title: String,
        body: String,
        date: Date,
        identifier: String? = nil
    ) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        content.categoryIdentifier = NotificationCategory.workoutReminder.rawValue
        content.userInfo = ["type": NotificationCategory.workoutReminder.rawValue]

        let components = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: date)
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)

        let id = identifier ?? "workout_reminder_\(UUID().uuidString)"
        let request = UNNotificationRequest(identifier: id, content: content, trigger: trigger)

        center.add(request) { [weak self] error in
            if let error {
                Task { @MainActor in self?.pendingNotificationCount += 0 }
                print("Failed to schedule workout reminder: \(error.localizedDescription)")
            }
        }
    }

    func scheduleRecurringWorkoutReminder(
        title: String,
        body: String,
        hour: Int,
        minute: Int,
        weekdays: [Int], // 1=Sunday, 2=Monday, ..., 7=Saturday
        identifier: String
    ) {
        // Remove existing reminders with this base identifier
        let identifiers = weekdays.map { "\(identifier)_day\($0)" }
        center.removePendingNotificationRequests(withIdentifiers: identifiers)

        for weekday in weekdays {
            let content = UNMutableNotificationContent()
            content.title = title
            content.body = body
            content.sound = .default
            content.categoryIdentifier = NotificationCategory.workoutReminder.rawValue
            content.userInfo = ["type": NotificationCategory.workoutReminder.rawValue, "weekday": weekday]

            var dateComponents = DateComponents()
            dateComponents.hour = hour
            dateComponents.minute = minute
            dateComponents.weekday = weekday

            let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
            let request = UNNotificationRequest(
                identifier: "\(identifier)_day\(weekday)",
                content: content,
                trigger: trigger
            )

            center.add(request)
        }
    }

    // MARK: - Achievement Notifications

    func sendAchievementNotification(
        title: String,
        achievementName: String,
        description: String
    ) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = "\(achievementName): \(description)"
        content.sound = UNNotificationSound(named: UNNotificationSoundName("achievement.caf"))
        content.categoryIdentifier = NotificationCategory.achievementUnlocked.rawValue
        content.userInfo = [
            "type": NotificationCategory.achievementUnlocked.rawValue,
            "achievement": achievementName
        ]

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let request = UNNotificationRequest(
            identifier: "achievement_\(UUID().uuidString)",
            content: content,
            trigger: trigger
        )

        center.add(request)
    }

    // MARK: - Streak Reminders

    func scheduleStreakReminder(currentStreak: Int, hour: Int = 19, minute: Int = 0) {
        center.removePendingNotificationRequests(withIdentifiers: ["streak_reminder"])

        let content = UNMutableNotificationContent()
        content.title = "Keep Your Streak Alive!"

        if currentStreak > 0 {
            content.body = "You have a \(currentStreak)-day streak going. Don't break it -- get a workout in today!"
        } else {
            content.body = "Start a new streak today! Every journey begins with a single workout."
        }

        content.sound = .default
        content.categoryIdentifier = NotificationCategory.streakReminder.rawValue
        content.userInfo = [
            "type": NotificationCategory.streakReminder.rawValue,
            "streak": currentStreak
        ]

        var dateComponents = DateComponents()
        dateComponents.hour = hour
        dateComponents.minute = minute

        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        let request = UNNotificationRequest(
            identifier: "streak_reminder",
            content: content,
            trigger: trigger
        )

        center.add(request)
    }

    func cancelStreakReminder() {
        center.removePendingNotificationRequests(withIdentifiers: ["streak_reminder"])
    }

    // MARK: - Rest Timer Notifications

    func scheduleRestTimerNotification(seconds: Int, exerciseName: String? = nil) {
        center.removePendingNotificationRequests(withIdentifiers: ["rest_timer"])

        let content = UNMutableNotificationContent()
        content.title = "Rest Timer Complete"

        if let exercise = exerciseName {
            content.body = "Time to start your next set of \(exercise)!"
        } else {
            content.body = "Rest period is over. Time for your next set!"
        }

        content.sound = UNNotificationSound(named: UNNotificationSoundName("timer_complete.caf"))
        content.categoryIdentifier = NotificationCategory.restTimer.rawValue
        content.userInfo = ["type": NotificationCategory.restTimer.rawValue]

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: TimeInterval(seconds), repeats: false)
        let request = UNNotificationRequest(
            identifier: "rest_timer",
            content: content,
            trigger: trigger
        )

        center.add(request)
    }

    func cancelRestTimer() {
        center.removePendingNotificationRequests(withIdentifiers: ["rest_timer"])
    }

    // MARK: - Training Plan Notifications

    func scheduleTrainingPlanReminder(
        workoutDescription: String,
        date: Date,
        identifier: String
    ) {
        let content = UNMutableNotificationContent()
        content.title = "Today's Training"
        content.body = workoutDescription
        content.sound = .default
        content.categoryIdentifier = NotificationCategory.trainingPlan.rawValue
        content.userInfo = [
            "type": NotificationCategory.trainingPlan.rawValue,
            "workout": workoutDescription
        ]

        let components = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: date)
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)

        center.add(request)
    }

    // MARK: - Weekly Report

    func scheduleWeeklyReport(hour: Int = 9, minute: Int = 0, weekday: Int = 2) {
        center.removePendingNotificationRequests(withIdentifiers: ["weekly_report"])

        let content = UNMutableNotificationContent()
        content.title = "Weekly Summary Ready"
        content.body = "Check out your training stats from last week!"
        content.sound = .default
        content.categoryIdentifier = NotificationCategory.weeklyReport.rawValue
        content.userInfo = ["type": NotificationCategory.weeklyReport.rawValue]

        var dateComponents = DateComponents()
        dateComponents.hour = hour
        dateComponents.minute = minute
        dateComponents.weekday = weekday

        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        let request = UNNotificationRequest(identifier: "weekly_report", content: content, trigger: trigger)

        center.add(request)
    }

    // MARK: - Management

    func removeAllPendingNotifications() {
        center.removeAllPendingNotificationRequests()
    }

    func removePendingNotifications(withIdentifiers identifiers: [String]) {
        center.removePendingNotificationRequests(withIdentifiers: identifiers)
    }

    func removeAllDeliveredNotifications() {
        center.removeAllDeliveredNotifications()
    }

    func getPendingNotificationCount() async -> Int {
        let requests = await center.pendingNotificationRequests()
        let count = requests.count
        await MainActor.run {
            self.pendingNotificationCount = count
        }
        return count
    }

    // MARK: - UNUserNotificationCenterDelegate

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // Show notifications even when the app is in the foreground
        completionHandler([.banner, .sound, .badge])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        let actionIdentifier = response.actionIdentifier

        switch actionIdentifier {
        case NotificationAction.snooze.rawValue:
            // Re-schedule in 30 minutes
            let content = response.notification.request.content.mutableCopy() as! UNMutableNotificationContent
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1800, repeats: false)
            let request = UNNotificationRequest(
                identifier: "snoozed_\(UUID().uuidString)",
                content: content,
                trigger: trigger
            )
            center.add(request)

        case NotificationAction.startWorkout.rawValue:
            NotificationCenter.default.post(
                name: .init("GoSteadyStartWorkoutFromNotification"),
                object: nil,
                userInfo: userInfo
            )

        case NotificationAction.viewDetails.rawValue:
            NotificationCenter.default.post(
                name: .init("GoSteadyViewDetailsFromNotification"),
                object: nil,
                userInfo: userInfo
            )

        default:
            break
        }

        completionHandler()
    }
}
