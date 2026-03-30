import Foundation
import Observation
import CoreLocation
import CoreMotion

@Observable
final class ActiveSwimViewModel {
    // MARK: - State
    var swimType: SwimType = .pool
    var poolLength: PoolLength? = .shortCourseMeters
    var isActive = false
    var isPaused = false
    var elapsedMillis: Int64 = 0
    var laps: Int = 0
    var totalDistance: Double = 0.0
    var strokeCount: Int = 0
    var currentHeartRate: Int? = nil
    var isInRestInterval = false
    var restIntervalElapsed: Int64 = 0
    var autoDetectEnabled = false
    var autoDetectedLaps: Int = 0
    var splits: [LiveSwimSplit] = []

    // Derived
    var avgPaceSecondsPer100m: Double {
        guard totalDistance > 0 else { return 0 }
        return (Double(elapsedMillis) / 1000.0) / (totalDistance / 100.0)
    }

    var avgPaceFormatted: String {
        SwimmingViewModel.formatPace(avgPaceSecondsPer100m)
    }

    var elapsedFormatted: String {
        formatDuration(elapsedMillis)
    }

    var restIntervalFormatted: String {
        formatDuration(restIntervalElapsed)
    }

    var distanceFormatted: String {
        String(format: "%.0f", totalDistance)
    }

    var lastLapPace: String? {
        guard let lastSplit = splits.last else { return nil }
        return SwimmingViewModel.formatPace(lastSplit.paceSecondsPer100m)
    }

    // MARK: - Private
    private var startTime: Date?
    private var pausedDuration: TimeInterval = 0
    private var pauseStartTime: Date?
    private var restStartTime: Date?
    private var timer: Timer?
    private var restTimer: Timer?
    private var lapStartTime: Date?
    private var lapStartDistance: Double = 0

    private var swimmingRepository: SwimmingRepository?
    private let locationService: LocationService?
    private var locationManager: CLLocationManager?
    private var lastLocation: CLLocation?
    private var routePoints: [(lat: Double, lon: Double, timestamp: Date)] = []

    init(locationService: LocationService? = nil) {
        self.locationService = locationService
    }

    func configure(swimmingRepository: SwimmingRepository) {
        self.swimmingRepository = swimmingRepository
    }

    // MARK: - Workout Lifecycle

    func startWorkout(swimType: SwimType, poolLength: PoolLength?) {
        self.swimType = swimType
        self.poolLength = swimType == .pool ? poolLength : nil
        self.isActive = true
        self.isPaused = false
        self.startTime = Date()
        self.lapStartTime = Date()
        self.lapStartDistance = 0
        self.autoDetectEnabled = swimType == .pool

        startTimer()

        if swimType != .pool {
            startGPSTracking()
        }
    }

    func pauseWorkout() {
        isPaused = true
        pauseStartTime = Date()
    }

    func resumeWorkout() {
        if let pauseStart = pauseStartTime {
            pausedDuration += Date().timeIntervalSince(pauseStart)
        }
        isPaused = false
        pauseStartTime = nil
    }

    func startRestInterval() {
        guard !isInRestInterval else { return }
        isInRestInterval = true
        restIntervalElapsed = 0
        restStartTime = Date()
        restTimer?.invalidate()
        restTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self, self.isInRestInterval else { return }
            if let restStart = self.restStartTime {
                self.restIntervalElapsed = Int64(Date().timeIntervalSince(restStart) * 1000)
            }
        }
    }

    func endRestInterval() {
        isInRestInterval = false
        restTimer?.invalidate()
        restTimer = nil
        restIntervalElapsed = 0
    }

    func addLap() {
        let lapDistance = Double(poolLength?.meters ?? 25)
        laps += 1
        totalDistance += lapDistance

        // Record split
        let now = Date()
        let lapDuration: Int64
        if let lapStart = lapStartTime {
            lapDuration = Int64(now.timeIntervalSince(lapStart) * 1000)
        } else {
            lapDuration = 0
        }
        let pace = lapDistance > 0 ? (Double(lapDuration) / 1000.0) / (lapDistance / 100.0) : 0
        let split = LiveSwimSplit(
            lapNumber: laps,
            distanceMeters: lapDistance,
            durationMillis: lapDuration,
            paceSecondsPer100m: pace,
            strokeType: .freestyle
        )
        splits.append(split)
        lapStartTime = now
        lapStartDistance = totalDistance

        if isInRestInterval {
            endRestInterval()
        }
    }

    func undoLap() {
        guard laps > 0 else { return }
        let lapDistance = Double(poolLength?.meters ?? 25)
        laps -= 1
        totalDistance = max(0, totalDistance - lapDistance)
        if !splits.isEmpty {
            splits.removeLast()
        }
    }

    func updateStrokeCount(_ count: Int) {
        strokeCount = count
    }

    func finishWorkout(completion: @escaping (String?) -> Void) {
        timer?.invalidate()
        restTimer?.invalidate()
        isActive = false

        let workoutSplits = splits.map { split in
            SwimSplit(
                lapNumber: split.lapNumber,
                distanceMeters: split.distanceMeters,
                durationMillis: split.durationMillis,
                paceSecondsPer100m: split.paceSecondsPer100m,
                strokeCount: nil,
                strokeType: split.strokeType,
                avgHeartRate: nil
            )
        }

        let bestPace = splits.map(\.paceSecondsPer100m).filter { $0 > 0 }.min() ?? avgPaceSecondsPer100m

        let workoutId = UUID()
        let workout = SwimmingWorkout(
            id: workoutId,
            startTime: startTime ?? Date(),
            endTime: Date(),
            swimType: swimType,
            poolLength: poolLength,
            distanceMeters: totalDistance,
            durationMillis: elapsedMillis,
            laps: laps,
            avgPaceSecondsPer100m: avgPaceSecondsPer100m,
            bestPaceSecondsPer100m: bestPace,
            caloriesBurned: estimateCalories(),
            strokeType: .freestyle,
            splits: workoutSplits,
            isCompleted: true
        )

        swimmingRepository?.insert(workout)
        completion(workoutId.uuidString)
    }

    func discardWorkout() {
        timer?.invalidate()
        restTimer?.invalidate()
        resetState()
    }

    // MARK: - Private Methods

    private func startTimer() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self, !self.isPaused, let start = self.startTime else { return }
            let elapsed = Date().timeIntervalSince(start) - self.pausedDuration
            self.elapsedMillis = Int64(elapsed * 1000)
        }
    }

    private func startGPSTracking() {
        // GPS tracking handled by LocationService for open water swims
        // Distance accumulates via CLLocationManager delegate updates
        routePoints.removeAll()
        lastLocation = nil
    }

    func handleLocationUpdate(_ location: CLLocation) {
        guard swimType != .pool, isActive, !isPaused else { return }
        if let last = lastLocation {
            let dist = location.distance(from: last)
            if dist > 1.0 { // filter noise
                totalDistance += dist
            }
        }
        lastLocation = location
        routePoints.append((lat: location.coordinate.latitude, lon: location.coordinate.longitude, timestamp: Date()))
    }

    private func estimateCalories() -> Int {
        let minutes = Double(elapsedMillis) / 60000.0
        return Int(minutes * 7.0) // ~7 cal/min swimming
    }

    private func resetState() {
        isActive = false
        isPaused = false
        elapsedMillis = 0
        laps = 0
        totalDistance = 0
        strokeCount = 0
        currentHeartRate = nil
        isInRestInterval = false
        restIntervalElapsed = 0
        autoDetectedLaps = 0
        splits = []
        startTime = nil
        pausedDuration = 0
        pauseStartTime = nil
        restStartTime = nil
        lapStartTime = nil
        lapStartDistance = 0
        routePoints = []
        lastLocation = nil
    }

    private func formatDuration(_ millis: Int64) -> String {
        let totalSeconds = millis / 1000
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        }
        return String(format: "%02d:%02d", minutes, seconds)
    }
}

// MARK: - Supporting Types

struct LiveSwimSplit: Identifiable {
    let id = UUID()
    let lapNumber: Int
    let distanceMeters: Double
    let durationMillis: Int64
    let paceSecondsPer100m: Double
    let strokeType: StrokeType
}
