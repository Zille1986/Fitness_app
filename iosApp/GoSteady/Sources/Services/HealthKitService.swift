import Foundation
import Observation
import HealthKit
import Combine

// MARK: - Health Data Models

struct SleepData: Equatable {
    let totalHours: Double
    let deepHours: Double
    let remHours: Double
    let lightHours: Double

    var quality: String {
        switch totalHours {
        case 7.0...: return "Good"
        case 5.5...: return "Moderate"
        default: return "Low"
        }
    }

    var formatted: String {
        let h = Int(totalHours)
        let m = Int((totalHours - Double(h)) * 60)
        return "\(h)h \(m)m"
    }
}

struct HeartRateSample: Identifiable, Equatable {
    let id = UUID()
    let timestamp: Date
    let bpm: Int
}

enum WorkoutActivityType: String, CaseIterable {
    case run = "Run"
    case gym = "Gym"
    case swim = "Swim"
    case cycle = "Cycle"
    case hiit = "HIIT"
    case walk = "Walk"
    case yoga = "Yoga"

    var hkType: HKWorkoutActivityType {
        switch self {
        case .run: return .running
        case .gym: return .traditionalStrengthTraining
        case .swim: return .swimming
        case .cycle: return .cycling
        case .hiit: return .highIntensityIntervalTraining
        case .walk: return .walking
        case .yoga: return .yoga
        }
    }
}

// MARK: - HealthKit Service

@Observable
final class HealthKitService {

    private let healthStore = HKHealthStore()

    var isAuthorized: Bool = false
    var todaySteps: Int = 0
    var todayCalories: Double = 0
    var restingHeartRate: Int? = nil
    var latestHRV: Double? = nil
    var lastError: String?

    // Heart rate streaming
    let heartRateSubject = PassthroughSubject<Int, Never>()
    private var heartRateQuery: HKAnchoredObjectQuery?

    static var isAvailable: Bool {
        HKHealthStore.isHealthDataAvailable()
    }

    // MARK: - Types to read/write

    private var readTypes: Set<HKObjectType> {
        var types: Set<HKObjectType> = []

        let quantityTypes: [HKQuantityTypeIdentifier] = [
            .stepCount,
            .heartRate,
            .restingHeartRate,
            .heartRateVariabilitySDNN,
            .activeEnergyBurned,
            .basalEnergyBurned,
            .distanceWalkingRunning,
            .distanceCycling,
            .distanceSwimming,
            .swimmingStrokeCount,
            .vo2Max,
            .oxygenSaturation,
            .bodyMass,
            .bodyFatPercentage,
            .height
        ]

        for id in quantityTypes {
            if let type = HKQuantityType.quantityType(forIdentifier: id) {
                types.insert(type)
            }
        }

        if let sleepType = HKCategoryType.categoryType(forIdentifier: .sleepAnalysis) {
            types.insert(sleepType)
        }

        types.insert(HKObjectType.workoutType())

        return types
    }

    private var writeTypes: Set<HKSampleType> {
        var types: Set<HKSampleType> = []

        let quantityTypes: [HKQuantityTypeIdentifier] = [
            .activeEnergyBurned,
            .distanceWalkingRunning,
            .distanceCycling,
            .distanceSwimming,
            .heartRate
        ]

        for id in quantityTypes {
            if let type = HKQuantityType.quantityType(forIdentifier: id) {
                types.insert(type)
            }
        }

        types.insert(HKObjectType.workoutType())

        return types
    }

    // MARK: - Authorization

    func requestAuthorization() async throws {
        guard Self.isAvailable else {
            throw HealthKitError.notAvailable
        }

        try await healthStore.requestAuthorization(toShare: writeTypes, read: readTypes)
        isAuthorized = true
    }

    // MARK: - Read: Steps

    func fetchTodaySteps() async -> Int {
        guard let stepsType = HKQuantityType.quantityType(forIdentifier: .stepCount) else { return 0 }

        let now = Date()
        let startOfDay = Calendar.current.startOfDay(for: now)
        let predicate = HKQuery.predicateForSamples(withStart: startOfDay, end: now, options: .strictStartDate)

        do {
            let result = try await querySum(quantityType: stepsType, predicate: predicate, unit: .count())
            let steps = Int(result)
            todaySteps = steps
            return steps
        } catch {
            lastError = error.localizedDescription
            return 0
        }
    }

    func fetchSteps(for date: Date) async -> Int {
        guard let stepsType = HKQuantityType.quantityType(forIdentifier: .stepCount) else { return 0 }

        let startOfDay = Calendar.current.startOfDay(for: date)
        let endOfDay = Calendar.current.date(byAdding: .day, value: 1, to: startOfDay) ?? date
        let predicate = HKQuery.predicateForSamples(withStart: startOfDay, end: endOfDay, options: .strictStartDate)

        do {
            let result = try await querySum(quantityType: stepsType, predicate: predicate, unit: .count())
            return Int(result)
        } catch {
            return 0
        }
    }

    func fetchWeeklySteps() async -> [Date: Int] {
        var result: [Date: Int] = [:]
        let today = Calendar.current.startOfDay(for: Date())

        for dayOffset in 0..<7 {
            guard let date = Calendar.current.date(byAdding: .day, value: -dayOffset, to: today) else { continue }
            let steps = await fetchSteps(for: date)
            result[date] = steps
        }

        return result
    }

    // MARK: - Read: Heart Rate

    func fetchLatestHeartRate() async -> Int? {
        guard let hrType = HKQuantityType.quantityType(forIdentifier: .heartRate) else { return nil }

        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)

        return await withCheckedContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: hrType,
                predicate: nil,
                limit: 1,
                sortDescriptors: [sortDescriptor]
            ) { _, samples, error in
                guard let sample = samples?.first as? HKQuantitySample, error == nil else {
                    continuation.resume(returning: nil)
                    return
                }
                let bpm = Int(sample.quantity.doubleValue(for: HKUnit.count().unitDivided(by: .minute())))
                continuation.resume(returning: bpm)
            }
            healthStore.execute(query)
        }
    }

    func fetchHeartRateHistory(from startDate: Date, to endDate: Date) async -> [HeartRateSample] {
        guard let hrType = HKQuantityType.quantityType(forIdentifier: .heartRate) else { return [] }

        let predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictStartDate)
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: true)

        return await withCheckedContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: hrType,
                predicate: predicate,
                limit: HKObjectQueryNoLimit,
                sortDescriptors: [sortDescriptor]
            ) { _, samples, error in
                guard let quantitySamples = samples as? [HKQuantitySample], error == nil else {
                    continuation.resume(returning: [])
                    return
                }

                let hrUnit = HKUnit.count().unitDivided(by: .minute())
                let heartRates = quantitySamples.map { sample in
                    HeartRateSample(
                        timestamp: sample.startDate,
                        bpm: Int(sample.quantity.doubleValue(for: hrUnit))
                    )
                }
                continuation.resume(returning: heartRates)
            }
            healthStore.execute(query)
        }
    }

    func fetchRestingHeartRate() async -> Int? {
        guard let type = HKQuantityType.quantityType(forIdentifier: .restingHeartRate) else { return nil }

        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)

        return await withCheckedContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: type,
                predicate: nil,
                limit: 1,
                sortDescriptors: [sortDescriptor]
            ) { [weak self] _, samples, error in
                guard let sample = samples?.first as? HKQuantitySample, error == nil else {
                    continuation.resume(returning: nil)
                    return
                }
                let bpm = Int(sample.quantity.doubleValue(for: HKUnit.count().unitDivided(by: .minute())))
                self?.restingHeartRate = bpm
                continuation.resume(returning: bpm)
            }
            healthStore.execute(query)
        }
    }

    // MARK: - Read: HRV

    func fetchLatestHRV() async -> Double? {
        guard let type = HKQuantityType.quantityType(forIdentifier: .heartRateVariabilitySDNN) else { return nil }

        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)

        return await withCheckedContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: type,
                predicate: nil,
                limit: 1,
                sortDescriptors: [sortDescriptor]
            ) { [weak self] _, samples, error in
                guard let sample = samples?.first as? HKQuantitySample, error == nil else {
                    continuation.resume(returning: nil)
                    return
                }
                let ms = sample.quantity.doubleValue(for: .secondUnit(with: .milli))
                self?.latestHRV = ms
                continuation.resume(returning: ms)
            }
            healthStore.execute(query)
        }
    }

    // MARK: - Read: Sleep

    func fetchLastNightSleep() async -> SleepData? {
        guard let sleepType = HKCategoryType.categoryType(forIdentifier: .sleepAnalysis) else { return nil }

        let now = Date()
        let yesterday6PM = Calendar.current.date(bySettingHour: 18, minute: 0, second: 0, of: Calendar.current.date(byAdding: .day, value: -1, to: now)!)!

        let predicate = HKQuery.predicateForSamples(withStart: yesterday6PM, end: now, options: .strictStartDate)
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: true)

        return await withCheckedContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: sleepType,
                predicate: predicate,
                limit: HKObjectQueryNoLimit,
                sortDescriptors: [sortDescriptor]
            ) { _, samples, error in
                guard let categorySamples = samples as? [HKCategorySample], error == nil, !categorySamples.isEmpty else {
                    continuation.resume(returning: nil)
                    return
                }

                var totalSeconds: TimeInterval = 0
                var deepSeconds: TimeInterval = 0
                var remSeconds: TimeInterval = 0
                var lightSeconds: TimeInterval = 0

                for sample in categorySamples {
                    let duration = sample.endDate.timeIntervalSince(sample.startDate)

                    switch HKCategoryValueSleepAnalysis(rawValue: sample.value) {
                    case .asleepDeep:
                        deepSeconds += duration
                        totalSeconds += duration
                    case .asleepREM:
                        remSeconds += duration
                        totalSeconds += duration
                    case .asleepCore:
                        lightSeconds += duration
                        totalSeconds += duration
                    case .asleepUnspecified:
                        totalSeconds += duration
                    default:
                        break // awake, inBed
                    }
                }

                guard totalSeconds > 0 else {
                    continuation.resume(returning: nil)
                    return
                }

                continuation.resume(returning: SleepData(
                    totalHours: totalSeconds / 3600,
                    deepHours: deepSeconds / 3600,
                    remHours: remSeconds / 3600,
                    lightHours: lightSeconds / 3600
                ))
            }
            healthStore.execute(query)
        }
    }

    // MARK: - Read: Calories

    func fetchTodayCalories() async -> Double {
        guard let activeType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) else { return 0 }

        let now = Date()
        let startOfDay = Calendar.current.startOfDay(for: now)
        let predicate = HKQuery.predicateForSamples(withStart: startOfDay, end: now, options: .strictStartDate)

        do {
            let active = try await querySum(quantityType: activeType, predicate: predicate, unit: .kilocalorie())
            todayCalories = active
            return active
        } catch {
            return 0
        }
    }

    // MARK: - Read: VO2 Max

    func fetchLatestVO2Max() async -> Double? {
        guard let type = HKQuantityType.quantityType(forIdentifier: .vo2Max) else { return nil }

        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)

        return await withCheckedContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: type,
                predicate: nil,
                limit: 1,
                sortDescriptors: [sortDescriptor]
            ) { _, samples, error in
                guard let sample = samples?.first as? HKQuantitySample, error == nil else {
                    continuation.resume(returning: nil)
                    return
                }
                let unit = HKUnit.literUnit(with: .milli).unitDivided(by: .gramUnit(with: .kilo).unitMultiplied(by: .minute()))
                let vo2 = sample.quantity.doubleValue(for: unit)
                continuation.resume(returning: vo2)
            }
            healthStore.execute(query)
        }
    }

    // MARK: - Read: Body metrics

    func fetchLatestWeight() async -> Double? {
        guard let type = HKQuantityType.quantityType(forIdentifier: .bodyMass) else { return nil }
        return await fetchLatestQuantity(type: type, unit: .gramUnit(with: .kilo))
    }

    func fetchLatestBodyFat() async -> Double? {
        guard let type = HKQuantityType.quantityType(forIdentifier: .bodyFatPercentage) else { return nil }
        return await fetchLatestQuantity(type: type, unit: .percent())
    }

    // MARK: - Stream: Heart Rate during workouts

    func startHeartRateStream() {
        guard let hrType = HKQuantityType.quantityType(forIdentifier: .heartRate) else { return }

        stopHeartRateStream()

        let predicate = HKQuery.predicateForSamples(withStart: Date(), end: nil, options: .strictStartDate)
        let hrUnit = HKUnit.count().unitDivided(by: .minute())

        let query = HKAnchoredObjectQuery(
            type: hrType,
            predicate: predicate,
            anchor: nil,
            limit: HKObjectQueryNoLimit
        ) { [weak self] _, samples, _, _, _ in
            self?.processHeartRateSamples(samples, unit: hrUnit)
        }

        query.updateHandler = { [weak self] _, samples, _, _, _ in
            self?.processHeartRateSamples(samples, unit: hrUnit)
        }

        heartRateQuery = query
        healthStore.execute(query)
    }

    func stopHeartRateStream() {
        if let query = heartRateQuery {
            healthStore.stop(query)
            heartRateQuery = nil
        }
    }

    private func processHeartRateSamples(_ samples: [HKSample]?, unit: HKUnit) {
        guard let quantitySamples = samples as? [HKQuantitySample] else { return }
        for sample in quantitySamples {
            let bpm = Int(sample.quantity.doubleValue(for: unit))
            heartRateSubject.send(bpm)
        }
    }

    // MARK: - Write: Workouts

    func saveWorkout(
        type: WorkoutActivityType,
        startDate: Date,
        endDate: Date,
        totalEnergyBurned: Double?,
        totalDistance: Double?,
        metadata: [String: Any]? = nil
    ) async throws {
        var hkMetadata: [String: Any] = metadata ?? [:]
        hkMetadata[HKMetadataKeyWasUserEntered] = false

        let configuration = HKWorkoutConfiguration()
        configuration.activityType = type.hkType
        if type == .swim {
            configuration.swimmingLocationType = .pool
            configuration.lapLength = HKQuantity(unit: .meter(), doubleValue: 25)
        }

        let builder = HKWorkoutBuilder(healthStore: healthStore, configuration: configuration, device: .local())

        try await builder.beginCollection(at: startDate)

        // Add energy burned
        if let calories = totalEnergyBurned, calories > 0,
           let energyType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) {
            let energyQuantity = HKQuantity(unit: .kilocalorie(), doubleValue: calories)
            let energySample = HKQuantitySample(
                type: energyType,
                quantity: energyQuantity,
                start: startDate,
                end: endDate
            )
            try await builder.addSamples([energySample])
        }

        // Add distance
        if let distance = totalDistance, distance > 0 {
            let distanceTypeId: HKQuantityTypeIdentifier
            switch type {
            case .run, .walk: distanceTypeId = .distanceWalkingRunning
            case .cycle: distanceTypeId = .distanceCycling
            case .swim: distanceTypeId = .distanceSwimming
            default: distanceTypeId = .distanceWalkingRunning
            }

            if let distanceType = HKQuantityType.quantityType(forIdentifier: distanceTypeId) {
                let distanceQuantity = HKQuantity(unit: .meter(), doubleValue: distance)
                let distanceSample = HKQuantitySample(
                    type: distanceType,
                    quantity: distanceQuantity,
                    start: startDate,
                    end: endDate
                )
                try await builder.addSamples([distanceSample])
            }
        }

        try await builder.addMetadata(hkMetadata)
        try await builder.endCollection(at: endDate)
        try await builder.finishWorkout()
    }

    func saveWorkoutWithHeartRate(
        type: WorkoutActivityType,
        startDate: Date,
        endDate: Date,
        totalEnergyBurned: Double?,
        totalDistance: Double?,
        heartRateSamples: [HeartRateSample],
        metadata: [String: Any]? = nil
    ) async throws {
        var hkMetadata: [String: Any] = metadata ?? [:]
        hkMetadata[HKMetadataKeyWasUserEntered] = false

        let configuration = HKWorkoutConfiguration()
        configuration.activityType = type.hkType

        let builder = HKWorkoutBuilder(healthStore: healthStore, configuration: configuration, device: .local())

        try await builder.beginCollection(at: startDate)

        // Add heart rate samples
        if let hrType = HKQuantityType.quantityType(forIdentifier: .heartRate), !heartRateSamples.isEmpty {
            let hrUnit = HKUnit.count().unitDivided(by: .minute())
            let hkSamples = heartRateSamples.map { sample in
                HKQuantitySample(
                    type: hrType,
                    quantity: HKQuantity(unit: hrUnit, doubleValue: Double(sample.bpm)),
                    start: sample.timestamp,
                    end: sample.timestamp
                )
            }
            try await builder.addSamples(hkSamples)
        }

        // Add energy burned
        if let calories = totalEnergyBurned, calories > 0,
           let energyType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) {
            let energySample = HKQuantitySample(
                type: energyType,
                quantity: HKQuantity(unit: .kilocalorie(), doubleValue: calories),
                start: startDate,
                end: endDate
            )
            try await builder.addSamples([energySample])
        }

        // Add distance
        if let distance = totalDistance, distance > 0 {
            let distanceTypeId: HKQuantityTypeIdentifier = type == .cycle ? .distanceCycling :
                                                            type == .swim ? .distanceSwimming : .distanceWalkingRunning
            if let distanceType = HKQuantityType.quantityType(forIdentifier: distanceTypeId) {
                let distanceSample = HKQuantitySample(
                    type: distanceType,
                    quantity: HKQuantity(unit: .meter(), doubleValue: distance),
                    start: startDate,
                    end: endDate
                )
                try await builder.addSamples([distanceSample])
            }
        }

        try await builder.addMetadata(hkMetadata)
        try await builder.endCollection(at: endDate)
        try await builder.finishWorkout()
    }

    // MARK: - Query: Workout History

    func fetchRecentWorkouts(limit: Int = 20) async -> [HKWorkout] {
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: false)

        return await withCheckedContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: .workoutType(),
                predicate: nil,
                limit: limit,
                sortDescriptors: [sortDescriptor]
            ) { _, samples, error in
                let workouts = (samples as? [HKWorkout]) ?? []
                continuation.resume(returning: workouts)
            }
            healthStore.execute(query)
        }
    }

    func fetchWorkouts(from startDate: Date, to endDate: Date) async -> [HKWorkout] {
        let predicate = HKQuery.predicateForSamples(withStart: startDate, end: endDate, options: .strictStartDate)
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: false)

        return await withCheckedContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: .workoutType(),
                predicate: predicate,
                limit: HKObjectQueryNoLimit,
                sortDescriptors: [sortDescriptor]
            ) { _, samples, _ in
                let workouts = (samples as? [HKWorkout]) ?? []
                continuation.resume(returning: workouts)
            }
            healthStore.execute(query)
        }
    }

    // MARK: - Helpers

    private func querySum(quantityType: HKQuantityType, predicate: NSPredicate, unit: HKUnit) async throws -> Double {
        try await withCheckedThrowingContinuation { continuation in
            let query = HKStatisticsQuery(
                quantityType: quantityType,
                quantitySamplePredicate: predicate,
                options: .cumulativeSum
            ) { _, statistics, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }
                let sum = statistics?.sumQuantity()?.doubleValue(for: unit) ?? 0
                continuation.resume(returning: sum)
            }
            healthStore.execute(query)
        }
    }

    private func fetchLatestQuantity(type: HKQuantityType, unit: HKUnit) async -> Double? {
        let sortDescriptor = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)

        return await withCheckedContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: type,
                predicate: nil,
                limit: 1,
                sortDescriptors: [sortDescriptor]
            ) { _, samples, error in
                guard let sample = samples?.first as? HKQuantitySample, error == nil else {
                    continuation.resume(returning: nil)
                    return
                }
                continuation.resume(returning: sample.quantity.doubleValue(for: unit))
            }
            healthStore.execute(query)
        }
    }
}

// MARK: - Errors

enum HealthKitError: LocalizedError {
    case notAvailable
    case notAuthorized
    case queryFailed(String)

    var errorDescription: String? {
        switch self {
        case .notAvailable: return "HealthKit is not available on this device"
        case .notAuthorized: return "HealthKit authorization not granted"
        case .queryFailed(let msg): return "HealthKit query failed: \(msg)"
        }
    }
}
