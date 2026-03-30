import Foundation
import Observation
import HealthKit

@Observable
final class WatchHomeViewModel {

    var todaySteps: Int = 0
    var todayCalories: Double = 0
    var todayActiveMinutes: Int = 0
    var todayDistance: Double = 0
    var currentHeartRate: Int = 0
    var restingHeartRate: Int = 0

    var moveProgress: Double = 0
    var exerciseProgress: Double = 0
    var standProgress: Double = 0

    var recentWorkouts: [RecentWorkoutItem] = []

    private weak var healthService: WatchHealthService?
    private var refreshTimer: Timer?

    struct RecentWorkoutItem: Identifiable {
        let id = UUID()
        let type: WatchActivityType
        let duration: TimeInterval
        let distance: Double
        let date: Date
    }

    func configure(healthService: WatchHealthService) {
        self.healthService = healthService
        refresh()
        startPeriodicRefresh()
    }

    func refresh() {
        healthService?.fetchTodayStats()
        healthService?.fetchActivityRings()

        // Sync from health service after short delay for queries to complete
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { [weak self] in
            guard let self, let hs = self.healthService else { return }
            self.todaySteps = hs.todaySteps
            self.todayCalories = hs.todayCalories
            self.todayActiveMinutes = hs.todayActiveMinutes
            self.todayDistance = hs.todayDistance
            self.currentHeartRate = hs.currentHeartRate
            self.restingHeartRate = hs.restingHeartRate
            self.moveProgress = hs.moveGoalProgress
            self.exerciseProgress = hs.exerciseGoalProgress
            self.standProgress = hs.standGoalProgress
        }

        fetchRecentWorkouts()
    }

    private func startPeriodicRefresh() {
        refreshTimer?.invalidate()
        refreshTimer = Timer.scheduledTimer(withTimeInterval: 60, repeats: true) { [weak self] _ in
            self?.refresh()
        }
    }

    private func fetchRecentWorkouts() {
        let healthStore = HKHealthStore()
        let workoutType = HKObjectType.workoutType()
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)
        let query = HKSampleQuery(
            sampleType: workoutType,
            predicate: nil,
            limit: 5,
            sortDescriptors: [sortDescriptor]
        ) { [weak self] _, samples, _ in
            guard let workouts = samples as? [HKWorkout] else { return }
            let items = workouts.compactMap { workout -> RecentWorkoutItem? in
                let actType: WatchActivityType
                switch workout.workoutActivityType {
                case .running: actType = .outdoorRun
                case .swimming: actType = .poolSwim
                case .cycling: actType = .outdoorCycle
                case .traditionalStrengthTraining: actType = .gym
                case .highIntensityIntervalTraining: actType = .hiit
                default: actType = .outdoorRun
                }
                return RecentWorkoutItem(
                    type: actType,
                    duration: workout.duration,
                    distance: workout.totalDistance?.doubleValue(for: .meter()) ?? 0,
                    date: workout.startDate
                )
            }
            DispatchQueue.main.async {
                self?.recentWorkouts = items
            }
        }
        healthStore.execute(query)
    }

    deinit {
        refreshTimer?.invalidate()
    }
}
