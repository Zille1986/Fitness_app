import Foundation
import HealthKit
import Observation
import WatchKit
import CoreLocation

@Observable
final class WatchWorkoutService: NSObject {

    // MARK: - State

    var isActive = false
    var isPaused = false
    var currentActivity: WatchActivityType = .outdoorRun
    var elapsedSeconds: TimeInterval = 0
    var distanceMeters: Double = 0
    var heartRate: Int = 0
    var calories: Double = 0
    var currentPaceSecondsPerKm: Double = 0
    var laps: [LapData] = []
    var strokeCount: Int = 0
    var elevationGain: Double = 0
    var autoPauseActive = false
    var poolLength: PoolLength = .m25

    // Heart rate tracking
    var heartRateSamples: [(time: TimeInterval, bpm: Int)] = []
    var maxHeartRate: Int = 0
    private var heartRateSum: Double = 0
    private var heartRateCount: Int = 0
    var averageHeartRate: Int {
        heartRateCount > 0 ? Int(heartRateSum / Double(heartRateCount)) : 0
    }

    // MARK: - Private

    private let healthStore = HKHealthStore()
    private var session: HKWorkoutSession?
    private var builder: HKLiveWorkoutBuilder?
    private var routeBuilder: HKWorkoutRouteBuilder?

    private var timer: Timer?
    private var startDate: Date?
    private var pausedDuration: TimeInterval = 0
    private var pauseStartDate: Date?
    private var lastLapDistance: Double = 0
    private var lastLapTime: TimeInterval = 0

    // Auto-pause
    private var autoPauseEnabled = true
    private var lastSpeedCheckTime: Date?
    private var lowSpeedDuration: TimeInterval = 0
    private let autoPauseSpeedThreshold: Double = 0.5 // m/s
    private let autoPauseTriggerDuration: TimeInterval = 5.0

    // Dependencies
    private weak var healthService: WatchHealthService?
    private weak var locationService: WatchLocationService?
    private weak var syncService: PhoneSyncService?
    private weak var workoutVM: WatchWorkoutViewModel?

    // MARK: - Configuration

    func configure(
        healthService: WatchHealthService,
        locationService: WatchLocationService,
        syncService: PhoneSyncService,
        workoutVM: WatchWorkoutViewModel
    ) {
        self.healthService = healthService
        self.locationService = locationService
        self.syncService = syncService
        self.workoutVM = workoutVM
    }

    // MARK: - Start Workout

    func startWorkout(activity: WatchActivityType, pool: PoolLength = .m25) {
        let config = HKWorkoutConfiguration()
        config.activityType = activity.hkActivityType
        config.locationType = activity.hkLocationType

        if activity == .poolSwim {
            config.swimmingLocationType = .pool
            config.lapLength = pool.quantity
            poolLength = pool
        } else if activity == .openWaterSwim {
            config.swimmingLocationType = .openWater
        }

        do {
            session = try HKWorkoutSession(healthStore: healthStore, configuration: config)
            builder = session?.associatedWorkoutBuilder()

            session?.delegate = self
            builder?.delegate = self
            builder?.dataSource = HKLiveWorkoutDataSource(
                healthStore: healthStore,
                workoutConfiguration: config
            )

            let start = Date()
            session?.startActivity(with: start)
            builder?.beginCollection(withStart: start) { [weak self] _, error in
                guard let self, error == nil else { return }
                DispatchQueue.main.async {
                    self.startDate = start
                    self.currentActivity = activity
                    self.isActive = true
                    self.isPaused = false
                    self.resetMetrics()
                    self.startTimer()

                    if activity.needsGPS {
                        self.startRouteTracking()
                        self.locationService?.startTracking()
                    }

                    // Enable water lock for swimming
                    if activity.isSwimming {
                        WKInterfaceDevice.current().enableWaterLock()
                    }

                    WKInterfaceDevice.current().play(.start)
                    self.syncService?.sendWorkoutStarted(activity: activity)
                }
            }
        } catch {
            print("Failed to start workout: \(error)")
        }
    }

    // MARK: - Pause / Resume / Stop

    func pause() {
        session?.pause()
        pauseStartDate = Date()
        isPaused = true
        autoPauseActive = false
        WKInterfaceDevice.current().play(.stop)
        syncService?.sendHeartRate(0) // Signal pause
    }

    func resume() {
        session?.resume()
        if let ps = pauseStartDate {
            pausedDuration += Date().timeIntervalSince(ps)
            pauseStartDate = nil
        }
        isPaused = false
        autoPauseActive = false
        WKInterfaceDevice.current().play(.start)
    }

    func stop() {
        timer?.invalidate()
        timer = nil
        locationService?.stopTracking()

        let summaryData = buildSummary()

        session?.end()
        builder?.endCollection(withEnd: Date()) { [weak self] _, _ in
            self?.builder?.finishWorkout { [weak self] workout, _ in
                guard let self else { return }

                // Build route
                if let workout {
                    self.routeBuilder?.finishRoute(with: workout, metadata: nil) { _, _ in }
                    self.syncService?.sendCompletedWorkout(workout, activity: self.currentActivity)
                }

                DispatchQueue.main.async {
                    self.workoutVM?.showSummaryWith(data: summaryData)
                    self.isActive = false
                    self.isPaused = false
                    WKInterfaceDevice.current().play(.success)
                }
            }
        }
    }

    // MARK: - Lap

    func addLap() {
        let lapNumber = laps.count + 1
        let lap = LapData(
            id: lapNumber,
            startTime: lastLapTime,
            endTime: elapsedSeconds,
            distanceMeters: distanceMeters - lastLapDistance
        )
        laps.append(lap)
        lastLapDistance = distanceMeters
        lastLapTime = elapsedSeconds

        builder?.addWorkoutEvents([
            HKWorkoutEvent(type: .lap, dateInterval: DateInterval(start: Date(), duration: 0), metadata: nil)
        ]) { _, _ in }

        WKInterfaceDevice.current().play(.click)
    }

    func addSwimLap() {
        addLap()
        if currentActivity == .poolSwim {
            distanceMeters += Double(poolLength.rawValue)
        }
    }

    // MARK: - Auto-Pause

    private func checkAutoPause(speed: Double) {
        guard autoPauseEnabled, currentActivity.isRunning, isActive, !isPaused else { return }

        let now = Date()
        if speed < autoPauseSpeedThreshold {
            if let lastCheck = lastSpeedCheckTime {
                lowSpeedDuration += now.timeIntervalSince(lastCheck)
            }
            if lowSpeedDuration >= autoPauseTriggerDuration {
                autoPauseActive = true
                pause()
                WKInterfaceDevice.current().play(.directionDown)
            }
        } else {
            lowSpeedDuration = 0
            if autoPauseActive {
                autoPauseActive = false
                resume()
                WKInterfaceDevice.current().play(.directionUp)
            }
        }
        lastSpeedCheckTime = now
    }

    // MARK: - Timer

    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self, let start = self.startDate, !self.isPaused else { return }
            let currentPause = self.pauseStartDate.map { Date().timeIntervalSince($0) } ?? 0
            self.elapsedSeconds = Date().timeIntervalSince(start) - self.pausedDuration - currentPause
        }
    }

    // MARK: - Route

    private func startRouteTracking() {
        routeBuilder = HKWorkoutRouteBuilder(healthStore: healthStore, device: nil)
    }

    func addRouteLocations(_ locations: [CLLocation]) {
        let filtered = locations.filter { $0.horizontalAccuracy < 20 }
        guard !filtered.isEmpty else { return }
        routeBuilder?.insertRouteData(filtered) { _, _ in }

        // Auto-pause check from GPS speed
        if let speed = filtered.last?.speed, speed >= 0 {
            checkAutoPause(speed: speed)
        }
    }

    // MARK: - Helpers

    private func resetMetrics() {
        elapsedSeconds = 0
        distanceMeters = 0
        heartRate = 0
        calories = 0
        currentPaceSecondsPerKm = 0
        laps = []
        strokeCount = 0
        elevationGain = 0
        heartRateSamples = []
        maxHeartRate = 0
        heartRateSum = 0
        heartRateCount = 0
        lastLapDistance = 0
        lastLapTime = 0
        pausedDuration = 0
        pauseStartDate = nil
        lowSpeedDuration = 0
        autoPauseActive = false
    }

    private func buildSummary() -> WorkoutSummaryData {
        WorkoutSummaryData(
            activityType: currentActivity,
            duration: elapsedSeconds,
            distanceMeters: distanceMeters,
            calories: calories,
            avgHeartRate: averageHeartRate,
            maxHeartRate: maxHeartRate,
            laps: laps,
            heartRateSamples: heartRateSamples,
            strokeCount: strokeCount,
            elevationGain: elevationGain
        )
    }
}

// MARK: - HKWorkoutSessionDelegate

extension WatchWorkoutService: HKWorkoutSessionDelegate {
    func workoutSession(
        _ workoutSession: HKWorkoutSession,
        didChangeTo toState: HKWorkoutSessionState,
        from fromState: HKWorkoutSessionState,
        date: Date
    ) {
        // Handled by pause/resume/stop methods
    }

    func workoutSession(_ workoutSession: HKWorkoutSession, didFailWithError error: Error) {
        print("Workout session error: \(error)")
    }
}

// MARK: - HKLiveWorkoutBuilderDelegate

extension WatchWorkoutService: HKLiveWorkoutBuilderDelegate {
    func workoutBuilderDidCollectEvent(_ workoutBuilder: HKLiveWorkoutBuilder) {
        // Lap events automatically collected
    }

    func workoutBuilder(
        _ workoutBuilder: HKLiveWorkoutBuilder,
        didCollectDataOf collectedTypes: Set<HKSampleType>
    ) {
        for type in collectedTypes {
            guard let quantityType = type as? HKQuantityType else { continue }
            let stats = workoutBuilder.statistics(for: quantityType)

            DispatchQueue.main.async { [weak self] in
                guard let self else { return }

                switch quantityType {
                case HKQuantityType.quantityType(forIdentifier: .heartRate):
                    let bpm = stats?.mostRecentQuantity()?.doubleValue(
                        for: .count().unitDivided(by: .minute())
                    ) ?? 0
                    let hr = Int(bpm)
                    self.heartRate = hr
                    if hr > 0 {
                        self.heartRateSum += Double(hr)
                        self.heartRateCount += 1
                        if hr > self.maxHeartRate { self.maxHeartRate = hr }
                        self.heartRateSamples.append((time: self.elapsedSeconds, bpm: hr))
                        self.syncService?.sendHeartRate(hr)
                    }

                case HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning),
                     HKQuantityType.quantityType(forIdentifier: .distanceCycling),
                     HKQuantityType.quantityType(forIdentifier: .distanceSwimming):
                    let dist = stats?.sumQuantity()?.doubleValue(for: .meter()) ?? 0
                    self.distanceMeters = dist

                    if self.currentActivity.isRunning, dist > 0 {
                        self.currentPaceSecondsPerKm = self.elapsedSeconds / (dist / 1000.0)
                    }
                    if self.currentActivity.isCycling, dist > 0, self.elapsedSeconds > 0 {
                        // Speed in m/s for cycling pace display
                        let speedMS = dist / self.elapsedSeconds
                        self.currentPaceSecondsPerKm = speedMS * 3.6 // km/h stored in pace field
                    }
                    if self.currentActivity == .poolSwim {
                        let autoLaps = Int(dist / Double(self.poolLength.rawValue))
                        if autoLaps > self.laps.count {
                            // HealthKit auto-detected a lap
                            let lap = LapData(
                                id: self.laps.count + 1,
                                startTime: self.lastLapTime,
                                endTime: self.elapsedSeconds,
                                distanceMeters: Double(self.poolLength.rawValue)
                            )
                            self.laps.append(lap)
                            self.lastLapTime = self.elapsedSeconds
                            WKInterfaceDevice.current().play(.click)
                        }
                    }

                case HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned):
                    self.calories = stats?.sumQuantity()?.doubleValue(for: .kilocalorie()) ?? 0

                case HKQuantityType.quantityType(forIdentifier: .swimmingStrokeCount):
                    self.strokeCount = Int(stats?.sumQuantity()?.doubleValue(for: .count()) ?? 0)

                default:
                    break
                }
            }
        }
    }
}
