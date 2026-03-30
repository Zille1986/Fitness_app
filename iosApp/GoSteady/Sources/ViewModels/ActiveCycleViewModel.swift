import Foundation
import Observation
import CoreLocation

@Observable
final class ActiveCycleViewModel {
    // MARK: - State
    var cyclingType: CyclingType = .outdoor
    var isActive = false
    var isPaused = false
    var elapsedMillis: Int64 = 0
    var totalDistance: Double = 0.0
    var currentSpeedKmh: Double = 0.0
    var maxSpeedKmh: Double = 0.0
    var avgSpeedKmh: Double = 0.0
    var elevationGain: Double = 0.0
    var elevationLoss: Double = 0.0
    var currentAltitude: Double = 0.0
    var currentCadence: Int = 0
    var currentHeartRate: Int? = nil
    var currentPowerWatts: Int? = nil
    var routeCoordinates: [(lat: Double, lon: Double)] = []
    var splits: [LiveCyclingSplit] = []

    // Derived
    var elapsedFormatted: String {
        formatDuration(elapsedMillis)
    }

    var distanceKm: Double { totalDistance / 1000.0 }

    var distanceFormatted: String {
        String(format: "%.2f", distanceKm)
    }

    var speedFormatted: String {
        String(format: "%.1f", currentSpeedKmh)
    }

    var avgSpeedFormatted: String {
        String(format: "%.1f", avgSpeedKmh)
    }

    var elevationFormatted: String {
        String(format: "%.0f", elevationGain)
    }

    // MARK: - Private
    private var startTime: Date?
    private var pausedDuration: TimeInterval = 0
    private var pauseStartTime: Date?
    private var timer: Timer?
    private var lastLocation: CLLocation?
    private var lastAltitude: Double?
    private var speedReadings: [Double] = []
    private var splitStartTime: Date?
    private var splitStartDistance: Double = 0
    private var currentKm: Int = 0

    private var cyclingRepository: CyclingRepository?

    init() {}

    func configure(cyclingRepository: CyclingRepository) {
        self.cyclingRepository = cyclingRepository
    }

    // MARK: - Workout Lifecycle

    func startWorkout(type: CyclingType) {
        self.cyclingType = type
        self.isActive = true
        self.isPaused = false
        self.startTime = Date()
        self.splitStartTime = Date()
        self.splitStartDistance = 0
        self.currentKm = 0

        startTimer()
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

    func finishWorkout(completion: @escaping (String?) -> Void) {
        timer?.invalidate()
        isActive = false

        let workoutId = UUID()
        let workout = CyclingWorkout(
            id: workoutId,
            startTime: startTime ?? Date(),
            endTime: Date(),
            cyclingType: cyclingType,
            distanceMeters: totalDistance,
            durationMillis: elapsedMillis,
            avgSpeedKmh: avgSpeedKmh,
            maxSpeedKmh: maxSpeedKmh,
            avgPowerWatts: currentPowerWatts,
            avgCadenceRpm: currentCadence > 0 ? currentCadence : nil,
            avgHeartRate: currentHeartRate,
            caloriesBurned: estimateCalories(),
            elevationGainMeters: elevationGain,
            elevationLossMeters: elevationLoss,
            routePoints: routeCoordinates.map { coord in
                RoutePoint(latitude: coord.lat, longitude: coord.lon, timestamp: Date())
            },
            splits: splits.map { s in
                CyclingSplit(
                    kilometer: s.kilometer,
                    durationMillis: s.durationMillis,
                    avgSpeedKmh: s.avgSpeedKmh,
                    elevationChange: s.elevationChange
                )
            },
            source: cyclingType == .outdoor ? .phoneGps : .manual,
            isCompleted: true
        )

        cyclingRepository?.insert(workout)
        completion(workoutId.uuidString)
    }

    func discardWorkout() {
        timer?.invalidate()
        resetState()
    }

    // MARK: - Location Updates

    func handleLocationUpdate(_ location: CLLocation) {
        guard isActive, !isPaused else { return }

        // Speed
        let speed = max(0, location.speed * 3.6) // m/s to km/h
        if speed > 0 {
            currentSpeedKmh = speed
            speedReadings.append(speed)
            maxSpeedKmh = max(maxSpeedKmh, speed)
            avgSpeedKmh = speedReadings.reduce(0, +) / Double(speedReadings.count)
        }

        // Distance
        if let last = lastLocation {
            let dist = location.distance(from: last)
            if dist > 0.5 {
                totalDistance += dist
            }
        }

        // Elevation
        let altitude = location.altitude
        if let lastAlt = lastAltitude {
            let diff = altitude - lastAlt
            if diff > 0.5 {
                elevationGain += diff
            } else if diff < -0.5 {
                elevationLoss += abs(diff)
            }
        }
        currentAltitude = altitude
        lastAltitude = altitude

        // Route
        routeCoordinates.append((lat: location.coordinate.latitude, lon: location.coordinate.longitude))

        // Auto-splits per km
        let newKm = Int(totalDistance / 1000.0)
        if newKm > currentKm {
            recordSplit(km: newKm)
            currentKm = newKm
        }

        lastLocation = location
    }

    // MARK: - Private

    private func startTimer() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self, !self.isPaused, let start = self.startTime else { return }
            let elapsed = Date().timeIntervalSince(start) - self.pausedDuration
            self.elapsedMillis = Int64(elapsed * 1000)

            if self.totalDistance > 0 && self.elapsedMillis > 0 {
                self.avgSpeedKmh = (self.totalDistance / 1000.0) / (Double(self.elapsedMillis) / 3600000.0)
            }

            // Reset speed to 0 if no update for 3s in outdoor mode
            if self.cyclingType == .outdoor && self.speedReadings.count > 0 {
                // speed reading is handled in location updates
            }
        }
    }

    private func recordSplit(km: Int) {
        let now = Date()
        let splitDuration: Int64
        if let splitStart = splitStartTime {
            splitDuration = Int64(now.timeIntervalSince(splitStart) * 1000)
        } else {
            splitDuration = 0
        }
        let splitDistance = totalDistance - splitStartDistance
        let splitSpeed = splitDuration > 0 ? (splitDistance / 1000.0) / (Double(splitDuration) / 3600000.0) : 0

        let split = LiveCyclingSplit(
            kilometer: km,
            durationMillis: splitDuration,
            avgSpeedKmh: splitSpeed,
            elevationChange: 0
        )
        splits.append(split)
        splitStartTime = now
        splitStartDistance = totalDistance
    }

    private func estimateCalories() -> Int {
        let minutes = Double(elapsedMillis) / 60000.0
        let intensityFactor: Double
        switch avgSpeedKmh {
        case ..<15: intensityFactor = 5.0
        case 15..<20: intensityFactor = 7.0
        case 20..<25: intensityFactor = 9.0
        case 25..<30: intensityFactor = 11.0
        default: intensityFactor = 13.0
        }
        return Int(minutes * intensityFactor)
    }

    private func resetState() {
        isActive = false
        isPaused = false
        elapsedMillis = 0
        totalDistance = 0
        currentSpeedKmh = 0
        maxSpeedKmh = 0
        avgSpeedKmh = 0
        elevationGain = 0
        elevationLoss = 0
        currentAltitude = 0
        currentCadence = 0
        currentHeartRate = nil
        currentPowerWatts = nil
        routeCoordinates = []
        splits = []
        startTime = nil
        pausedDuration = 0
        pauseStartTime = nil
        lastLocation = nil
        lastAltitude = nil
        speedReadings = []
        splitStartTime = nil
        splitStartDistance = 0
        currentKm = 0
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

struct LiveCyclingSplit: Identifiable {
    let id = UUID()
    let kilometer: Int
    let durationMillis: Int64
    let avgSpeedKmh: Double
    let elevationChange: Double

    var durationFormatted: String {
        let totalSeconds = durationMillis / 1000
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%d:%02d", minutes, seconds)
    }
}
