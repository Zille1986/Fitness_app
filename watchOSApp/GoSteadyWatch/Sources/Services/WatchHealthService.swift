import Foundation
import HealthKit
import Observation

@Observable
final class WatchHealthService {

    var isAuthorized = false
    var todaySteps: Int = 0
    var todayCalories: Double = 0
    var todayActiveMinutes: Int = 0
    var todayDistance: Double = 0
    var restingHeartRate: Int = 0
    var currentHeartRate: Int = 0
    var moveGoalProgress: Double = 0
    var exerciseGoalProgress: Double = 0
    var standGoalProgress: Double = 0

    private let healthStore = HKHealthStore()
    private var heartRateQuery: HKObserverQuery?

    // MARK: - Authorization

    func requestAuthorization() {
        let typesToShare: Set<HKSampleType> = [
            HKObjectType.workoutType(),
            HKSeriesType.workoutRoute()
        ]

        let typesToRead: Set<HKObjectType> = [
            HKObjectType.workoutType(),
            HKObjectType.quantityType(forIdentifier: .heartRate)!,
            HKObjectType.quantityType(forIdentifier: .activeEnergyBurned)!,
            HKObjectType.quantityType(forIdentifier: .basalEnergyBurned)!,
            HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning)!,
            HKObjectType.quantityType(forIdentifier: .distanceSwimming)!,
            HKObjectType.quantityType(forIdentifier: .distanceCycling)!,
            HKObjectType.quantityType(forIdentifier: .swimmingStrokeCount)!,
            HKObjectType.quantityType(forIdentifier: .stepCount)!,
            HKObjectType.quantityType(forIdentifier: .appleExerciseTime)!,
            HKObjectType.quantityType(forIdentifier: .restingHeartRate)!,
            HKObjectType.activitySummaryType()
        ]

        healthStore.requestAuthorization(toShare: typesToShare, read: typesToRead) { [weak self] success, _ in
            DispatchQueue.main.async {
                self?.isAuthorized = success
                if success {
                    self?.fetchTodayStats()
                    self?.startHeartRateObserver()
                    self?.fetchActivityRings()
                }
            }
        }
    }

    // MARK: - Today Stats

    func fetchTodayStats() {
        let calendar = Calendar.current
        let startOfDay = calendar.startOfDay(for: Date())
        let predicate = HKQuery.predicateForSamples(withStart: startOfDay, end: Date(), options: .strictStartDate)

        // Steps
        fetchSum(type: .stepCount, predicate: predicate, unit: .count()) { [weak self] value in
            self?.todaySteps = Int(value)
        }

        // Calories
        fetchSum(type: .activeEnergyBurned, predicate: predicate, unit: .kilocalorie()) { [weak self] value in
            self?.todayCalories = value
        }

        // Exercise minutes
        fetchSum(type: .appleExerciseTime, predicate: predicate, unit: .minute()) { [weak self] value in
            self?.todayActiveMinutes = Int(value)
        }

        // Distance
        fetchSum(type: .distanceWalkingRunning, predicate: predicate, unit: .meter()) { [weak self] value in
            self?.todayDistance = value
        }

        // Resting HR
        fetchMostRecent(type: .restingHeartRate, unit: .count().unitDivided(by: .minute())) { [weak self] value in
            self?.restingHeartRate = Int(value)
        }
    }

    // MARK: - Activity Rings

    func fetchActivityRings() {
        let calendar = Calendar.current
        let components = calendar.dateComponents([.era, .year, .month, .day], from: Date())
        let startDate = calendar.date(from: components)!
        let endDate = calendar.date(byAdding: .day, value: 1, to: startDate)!

        let predicate = HKQuery.predicate(forActivitySummariesBetweenStart: components,
                                           end: calendar.dateComponents([.era, .year, .month, .day], from: endDate))

        let query = HKActivitySummaryQuery(predicate: predicate) { [weak self] _, summaries, _ in
            guard let summary = summaries?.first else { return }
            DispatchQueue.main.async {
                let moveGoal = summary.activeEnergyBurnedGoal.doubleValue(for: .kilocalorie())
                let moveActual = summary.activeEnergyBurned.doubleValue(for: .kilocalorie())
                self?.moveGoalProgress = moveGoal > 0 ? min(moveActual / moveGoal, 1.0) : 0

                let exerciseGoal = summary.appleExerciseTimeGoal.doubleValue(for: .minute())
                let exerciseActual = summary.appleExerciseTime.doubleValue(for: .minute())
                self?.exerciseGoalProgress = exerciseGoal > 0 ? min(exerciseActual / exerciseGoal, 1.0) : 0

                let standGoal = summary.appleStandHoursGoal.doubleValue(for: .count())
                let standActual = summary.appleStandHours.doubleValue(for: .count())
                self?.standGoalProgress = standGoal > 0 ? min(standActual / standGoal, 1.0) : 0
            }
        }
        healthStore.execute(query)
    }

    // MARK: - Heart Rate Observer

    func startHeartRateObserver() {
        guard let hrType = HKObjectType.quantityType(forIdentifier: .heartRate) else { return }

        let query = HKObserverQuery(sampleType: hrType, predicate: nil) { [weak self] _, _, _ in
            self?.fetchLatestHeartRate()
        }
        healthStore.execute(query)
        heartRateQuery = query
    }

    private func fetchLatestHeartRate() {
        guard let hrType = HKQuantityType.quantityType(forIdentifier: .heartRate) else { return }
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)
        let query = HKSampleQuery(sampleType: hrType, predicate: nil, limit: 1, sortDescriptors: [sortDescriptor]) { [weak self] _, samples, _ in
            guard let sample = samples?.first as? HKQuantitySample else { return }
            let bpm = Int(sample.quantity.doubleValue(for: .count().unitDivided(by: .minute())))
            DispatchQueue.main.async {
                self?.currentHeartRate = bpm
            }
        }
        healthStore.execute(query)
    }

    // MARK: - Queries

    func queryHeartRateHistory(last hours: Int = 1, completion: @escaping ([(Date, Double)]) -> Void) {
        guard let hrType = HKQuantityType.quantityType(forIdentifier: .heartRate) else {
            completion([])
            return
        }

        let start = Calendar.current.date(byAdding: .hour, value: -hours, to: Date())!
        let predicate = HKQuery.predicateForSamples(withStart: start, end: Date(), options: .strictStartDate)
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: true)

        let query = HKSampleQuery(sampleType: hrType, predicate: predicate, limit: HKObjectQueryNoLimit, sortDescriptors: [sortDescriptor]) { _, samples, _ in
            let results = (samples as? [HKQuantitySample])?.map { sample in
                (sample.startDate, sample.quantity.doubleValue(for: .count().unitDivided(by: .minute())))
            } ?? []
            DispatchQueue.main.async {
                completion(results)
            }
        }
        healthStore.execute(query)
    }

    // MARK: - Private Helpers

    private func fetchSum(type: HKQuantityTypeIdentifier, predicate: NSPredicate, unit: HKUnit, completion: @escaping (Double) -> Void) {
        guard let quantityType = HKQuantityType.quantityType(forIdentifier: type) else { return }
        let query = HKStatisticsQuery(quantityType: quantityType, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, stats, _ in
            let value = stats?.sumQuantity()?.doubleValue(for: unit) ?? 0
            DispatchQueue.main.async { completion(value) }
        }
        healthStore.execute(query)
    }

    private func fetchMostRecent(type: HKQuantityTypeIdentifier, unit: HKUnit, completion: @escaping (Double) -> Void) {
        guard let quantityType = HKQuantityType.quantityType(forIdentifier: type) else { return }
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)
        let query = HKSampleQuery(sampleType: quantityType, predicate: nil, limit: 1, sortDescriptors: [sortDescriptor]) { _, samples, _ in
            let value = (samples?.first as? HKQuantitySample)?.quantity.doubleValue(for: unit) ?? 0
            DispatchQueue.main.async { completion(value) }
        }
        healthStore.execute(query)
    }
}
