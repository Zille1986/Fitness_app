import Foundation
import HealthKit
import CoreMotion
import CoreLocation
import WatchKit

/// Manages live workout sessions via HealthKit's HKWorkoutSession + HKLiveWorkoutBuilder.
/// Handles HR, distance, pace, calories, swim laps, and GPS tracking.
class WorkoutManager: NSObject, ObservableObject {

    // MARK: - Published State

    @Published var state = TrackingState()
    @Published var isAuthorized = false

    // MARK: - HealthKit

    private let healthStore = HKHealthStore()
    private var session: HKWorkoutSession?
    private var builder: HKLiveWorkoutBuilder?

    // MARK: - Timer

    private var timer: Timer?
    private var startDate: Date?
    private var pausedDuration: TimeInterval = 0
    private var pauseStart: Date?

    // MARK: - Location (open water swim, outdoor run/cycle)

    private let locationManager = CLLocationManager()
    private var routeBuilder: HKWorkoutRouteBuilder?
    private var lastLocation: CLLocation?

    // MARK: - Swim Lap Detection

    private let motionManager = CMMotionManager()

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
            HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning)!,
            HKObjectType.quantityType(forIdentifier: .distanceSwimming)!,
            HKObjectType.quantityType(forIdentifier: .distanceCycling)!,
            HKObjectType.quantityType(forIdentifier: .swimmingStrokeCount)!
        ]

        healthStore.requestAuthorization(toShare: typesToShare, read: typesToRead) { [weak self] success, _ in
            DispatchQueue.main.async {
                self?.isAuthorized = success
            }
        }
    }

    // MARK: - Start Workout

    func startWorkout(activity: ActivityType, swimType: SwimType? = nil, poolLength: PoolLength? = nil, cyclingType: CyclingType? = nil) {
        let config = HKWorkoutConfiguration()
        config.activityType = activity.hkType

        if activity == .swimming, let swim = swimType {
            config.swimmingLocationType = swim.hkSwimLocationType
            config.locationType = swim.hkLocationType
            if swim == .pool, let pool = poolLength {
                config.lapLength = pool.quantity
            }
        } else {
            config.locationType = activity.locationType
            if activity == .cycling, cyclingType == .indoor || cyclingType == .smartTrainer {
                config.locationType = .indoor
            }
        }

        do {
            session = try HKWorkoutSession(healthStore: healthStore, configuration: config)
            builder = session?.associatedWorkoutBuilder()

            session?.delegate = self
            builder?.delegate = self
            builder?.dataSource = HKLiveWorkoutDataSource(healthStore: healthStore, workoutConfiguration: config)

            let start = Date()
            session?.startActivity(with: start)
            builder?.beginCollection(withStart: start) { [weak self] _, _ in
                DispatchQueue.main.async {
                    self?.startDate = start
                    self?.state.isTracking = true
                    self?.state.isPaused = false
                    self?.state.activityType = activity
                    if let swim = swimType { self?.state.swimType = swim }
                    if let pool = poolLength { self?.state.poolLength = pool }
                    self?.startTimer()

                    // Start GPS for outdoor activities
                    let needsGPS = activity == .running ||
                        (activity == .cycling && cyclingType == .outdoor) ||
                        (activity == .swimming && swimType == .openWater)
                    if needsGPS {
                        self?.startLocationTracking()
                    }
                }
            }
        } catch {
            print("Failed to start workout: \(error)")
        }
    }

    // MARK: - Pause / Resume / Stop

    func pause() {
        session?.pause()
        pauseStart = Date()
        state.isPaused = true
        WKInterfaceDevice.current().play(.stop)
    }

    func resume() {
        session?.resume()
        if let ps = pauseStart {
            pausedDuration += Date().timeIntervalSince(ps)
            pauseStart = nil
        }
        state.isPaused = false
        WKInterfaceDevice.current().play(.start)
    }

    func stop() {
        timer?.invalidate()
        timer = nil
        locationManager.stopUpdatingLocation()

        session?.end()
        builder?.endCollection(withEnd: Date()) { [weak self] _, _ in
            self?.builder?.finishWorkout { [weak self] workout, _ in
                guard let self, let workout else { return }
                // Build route if we have location data
                self.routeBuilder?.finishRoute(with: workout, metadata: nil) { _, _ in }

                // Sync to phone
                PhoneSyncManager.shared.sendWorkout(workout, activityType: self.state.activityType)

                DispatchQueue.main.async {
                    self.state = TrackingState() // reset
                }
            }
        }
    }

    func addSwimLap() {
        // Manual lap — builder tracks it via HKWorkoutEvent
        builder?.addWorkoutEvents([
            HKWorkoutEvent(type: .lap, dateInterval: DateInterval(start: Date(), duration: 0), metadata: nil)
        ]) { _, _ in }

        state.laps += 1
        state.distanceMeters += Double(state.poolLength.rawValue)
        WKInterfaceDevice.current().play(.click)
    }

    // MARK: - Timer

    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self, let start = self.startDate, !self.state.isPaused else { return }
            let currentPause = self.pauseStart.map { Date().timeIntervalSince($0) } ?? 0
            self.state.elapsedSeconds = Date().timeIntervalSince(start) - self.pausedDuration - currentPause
        }
    }

    // MARK: - Location Tracking

    private func startLocationTracking() {
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
        routeBuilder = HKWorkoutRouteBuilder(healthStore: healthStore, device: nil)
    }

    // MARK: - Heart Rate Zone Check

    private func checkHRZone() {
        guard let min = state.targetHRMin, let max = state.targetHRMax else { return }
        let hr = state.heartRate

        let newAlert: ZoneAlert
        if hr < min {
            newAlert = .tooLow
        } else if hr > max {
            newAlert = .tooHigh
        } else {
            newAlert = .inZone
        }

        if newAlert != state.hrAlert {
            state.hrAlert = newAlert
            // Haptic feedback for zone changes
            switch newAlert {
            case .tooHigh:
                WKInterfaceDevice.current().play(.directionUp)
            case .tooLow:
                WKInterfaceDevice.current().play(.directionDown)
            case .inZone:
                WKInterfaceDevice.current().play(.success)
            }
        }
    }
}

// MARK: - HKWorkoutSessionDelegate

extension WorkoutManager: HKWorkoutSessionDelegate {
    func workoutSession(_ workoutSession: HKWorkoutSession, didChangeTo toState: HKWorkoutSessionState, from fromState: HKWorkoutSessionState, date: Date) {
        // State change handled by pause/resume/stop methods
    }

    func workoutSession(_ workoutSession: HKWorkoutSession, didFailWithError error: Error) {
        print("Workout session failed: \(error)")
    }
}

// MARK: - HKLiveWorkoutBuilderDelegate

extension WorkoutManager: HKLiveWorkoutBuilderDelegate {
    func workoutBuilderDidCollectEvent(_ workoutBuilder: HKLiveWorkoutBuilder) {
        // Lap events collected
    }

    func workoutBuilder(_ workoutBuilder: HKLiveWorkoutBuilder, didCollectDataOf collectedTypes: Set<HKSampleType>) {
        for type in collectedTypes {
            guard let quantityType = type as? HKQuantityType else { continue }

            let statistics = workoutBuilder.statistics(for: quantityType)

            DispatchQueue.main.async { [weak self] in
                guard let self else { return }

                switch quantityType {
                case HKQuantityType.quantityType(forIdentifier: .heartRate):
                    let hr = statistics?.mostRecentQuantity()?.doubleValue(for: .count().unitDivided(by: .minute())) ?? 0
                    self.state.heartRate = Int(hr)
                    self.checkHRZone()

                case HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning),
                     HKQuantityType.quantityType(forIdentifier: .distanceCycling),
                     HKQuantityType.quantityType(forIdentifier: .distanceSwimming):
                    let dist = statistics?.sumQuantity()?.doubleValue(for: .meter()) ?? 0
                    self.state.distanceMeters = dist
                    // Calculate pace for running
                    if self.state.activityType == .running, dist > 0 {
                        self.state.currentPaceSecondsPerKm = self.state.elapsedSeconds / (dist / 1000.0)
                    }
                    // Auto-calculate laps for pool swim
                    if self.state.activityType == .swimming, self.state.swimType == .pool {
                        self.state.laps = Int(dist / Double(self.state.poolLength.rawValue))
                    }

                case HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned):
                    self.state.calories = statistics?.sumQuantity()?.doubleValue(for: .kilocalorie()) ?? 0

                case HKQuantityType.quantityType(forIdentifier: .swimmingStrokeCount):
                    self.state.strokeCount = Int(statistics?.sumQuantity()?.doubleValue(for: .count()) ?? 0)

                default:
                    break
                }
            }
        }
    }
}

// MARK: - CLLocationManagerDelegate

extension WorkoutManager: CLLocationManagerDelegate {
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let filtered = locations.filter { $0.horizontalAccuracy < 20 }
        guard !filtered.isEmpty else { return }

        routeBuilder?.insertRouteData(filtered) { _, _ in }

        // For open water swim, calculate distance from GPS
        if state.activityType == .swimming, state.swimType == .openWater {
            for loc in filtered {
                if let last = lastLocation {
                    let delta = loc.distance(from: last)
                    if delta > 1.0 { // ignore jitter < 1m
                        DispatchQueue.main.async {
                            self.state.distanceMeters += delta
                        }
                    }
                }
                lastLocation = loc
            }
        }
    }
}
