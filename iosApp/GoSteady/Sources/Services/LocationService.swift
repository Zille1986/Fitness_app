import Foundation
import Observation
import CoreLocation
import Combine

// MARK: - Tracking State

struct TrackingState: Equatable {
    var isTracking: Bool = false
    var isPaused: Bool = false
    var distanceMeters: Double = 0.0
    var durationSeconds: TimeInterval = 0
    var currentPaceSecondsPerKm: Double = 0.0
    var currentHeartRate: Int? = nil
    var currentLocation: RoutePoint? = nil
    var routePoints: [RoutePoint] = []
    var elevationGain: Double = 0.0
    var elevationLoss: Double = 0.0
    var currentAltitude: Double? = nil
    var splits: [Double] = [] // pace per km for each completed km

    var distanceKm: Double { distanceMeters / 1000.0 }

    var durationFormatted: String {
        let total = Int(durationSeconds)
        let hours = total / 3600
        let minutes = (total % 3600) / 60
        let seconds = total % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        }
        return String(format: "%d:%02d", minutes, seconds)
    }

    var paceFormatted: String {
        guard currentPaceSecondsPerKm > 0, currentPaceSecondsPerKm.isFinite else { return "--:--" }
        let minutes = Int(currentPaceSecondsPerKm) / 60
        let seconds = Int(currentPaceSecondsPerKm) % 60
        return String(format: "%d:%02d", minutes, seconds)
    }
}

// MARK: - Location Service

@Observable
final class LocationService: NSObject, CLLocationManagerDelegate {

    private let locationManager = CLLocationManager()
    private var timerCancellable: AnyCancellable?

    // Published state
    var trackingState = TrackingState()
    var authorizationStatus: CLAuthorizationStatus = .notDetermined
    var lastError: String?

    // Location stream for external consumers
    let locationSubject = PassthroughSubject<CLLocation, Never>()

    // Internal tracking state
    private var routePoints: [RoutePoint] = []
    private var startTime: Date?
    private var pausedDuration: TimeInterval = 0
    private var lastPauseTime: Date?
    private var lastSplitDistance: Double = 0
    private var lastSplitTime: TimeInterval = 0

    override init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = 5.0
        locationManager.activityType = .fitness
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.pausesLocationUpdatesAutomatically = false
        locationManager.showsBackgroundLocationIndicator = true
        authorizationStatus = locationManager.authorizationStatus
    }

    // MARK: - Authorization

    func requestAuthorization() {
        locationManager.requestWhenInUseAuthorization()
    }

    func requestAlwaysAuthorization() {
        locationManager.requestAlwaysAuthorization()
    }

    var hasLocationPermission: Bool {
        authorizationStatus == .authorizedAlways || authorizationStatus == .authorizedWhenInUse
    }

    // MARK: - Tracking Controls

    func startTracking() {
        guard !trackingState.isTracking else { return }
        guard hasLocationPermission else {
            lastError = "Location permission not granted"
            return
        }

        startTime = Date()
        routePoints.removeAll()
        pausedDuration = 0
        lastPauseTime = nil
        lastSplitDistance = 0
        lastSplitTime = 0

        trackingState = TrackingState(isTracking: true, isPaused: false)

        locationManager.startUpdatingLocation()
        startTimer()
    }

    func pauseTracking() {
        guard trackingState.isTracking, !trackingState.isPaused else { return }

        lastPauseTime = Date()
        locationManager.stopUpdatingLocation()
        timerCancellable?.cancel()

        trackingState.isPaused = true
    }

    func resumeTracking() {
        guard trackingState.isTracking, trackingState.isPaused else { return }

        if let pauseTime = lastPauseTime {
            pausedDuration += Date().timeIntervalSince(pauseTime)
        }
        lastPauseTime = nil

        locationManager.startUpdatingLocation()
        startTimer()

        trackingState.isPaused = false
    }

    func stopTracking() -> TrackingState {
        guard trackingState.isTracking else { return trackingState }

        locationManager.stopUpdatingLocation()
        timerCancellable?.cancel()

        let finalState = trackingState

        trackingState = TrackingState()
        startTime = nil
        routePoints.removeAll()

        return finalState
    }

    // MARK: - Distance & Pace Calculation

    static func calculateDistance(points: [RoutePoint]) -> Double {
        guard points.count >= 2 else { return 0 }

        var totalDistance: Double = 0
        for i in 1..<points.count {
            let prev = CLLocation(latitude: points[i - 1].latitude, longitude: points[i - 1].longitude)
            let curr = CLLocation(latitude: points[i].latitude, longitude: points[i].longitude)
            totalDistance += curr.distance(from: prev)
        }
        return totalDistance
    }

    static func calculateElevationGain(points: [RoutePoint]) -> Double {
        guard points.count >= 2 else { return 0 }

        var gain: Double = 0
        for i in 1..<points.count {
            guard let prevAlt = points[i - 1].altitude, let currAlt = points[i].altitude else { continue }
            if currAlt > prevAlt {
                gain += (currAlt - prevAlt)
            }
        }
        return gain
    }

    static func calculateElevationLoss(points: [RoutePoint]) -> Double {
        guard points.count >= 2 else { return 0 }

        var loss: Double = 0
        for i in 1..<points.count {
            guard let prevAlt = points[i - 1].altitude, let currAlt = points[i].altitude else { continue }
            if currAlt < prevAlt {
                loss += (prevAlt - currAlt)
            }
        }
        return loss
    }

    static func calculatePace(distanceMeters: Double, durationSeconds: TimeInterval) -> Double {
        guard distanceMeters > 0, durationSeconds > 0 else { return 0 }
        let distanceKm = distanceMeters / 1000.0
        return durationSeconds / distanceKm
    }

    // MARK: - Timer

    private func startTimer() {
        timerCancellable = Timer.publish(every: 1.0, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                self?.updateDuration()
            }
    }

    private func updateDuration() {
        guard let startTime, !trackingState.isPaused else { return }
        let elapsed = Date().timeIntervalSince(startTime) - pausedDuration
        trackingState.durationSeconds = elapsed
    }

    // MARK: - Splits

    private func checkForNewSplit() {
        let currentDistance = trackingState.distanceMeters
        let currentDuration = trackingState.durationSeconds

        let nextSplitDistance = lastSplitDistance + 1000.0
        if currentDistance >= nextSplitDistance {
            let splitDuration = currentDuration - lastSplitTime
            let splitDistance = currentDistance - lastSplitDistance
            let splitPace = splitDuration / (splitDistance / 1000.0)

            trackingState.splits.append(splitPace)
            lastSplitDistance = nextSplitDistance
            lastSplitTime = currentDuration
        }
    }

    // MARK: - CLLocationManagerDelegate

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard trackingState.isTracking, !trackingState.isPaused else { return }

        for location in locations {
            // Filter out inaccurate locations
            guard location.horizontalAccuracy >= 0, location.horizontalAccuracy < 30 else { continue }

            let routePoint = RoutePoint(
                latitude: location.coordinate.latitude,
                longitude: location.coordinate.longitude,
                altitude: location.verticalAccuracy >= 0 ? location.altitude : nil,
                timestamp: location.timestamp,
                accuracy: location.horizontalAccuracy,
                speed: location.speed >= 0 ? location.speed : nil
            )

            routePoints.append(routePoint)

            let distance = Self.calculateDistance(points: routePoints)
            let duration = trackingState.durationSeconds
            let pace = Self.calculatePace(distanceMeters: distance, durationSeconds: duration)
            let elevGain = Self.calculateElevationGain(points: routePoints)
            let elevLoss = Self.calculateElevationLoss(points: routePoints)

            trackingState.distanceMeters = distance
            trackingState.currentPaceSecondsPerKm = pace
            trackingState.currentLocation = routePoint
            trackingState.routePoints = routePoints
            trackingState.elevationGain = elevGain
            trackingState.elevationLoss = elevLoss
            trackingState.currentAltitude = routePoint.altitude

            checkForNewSplit()
            locationSubject.send(location)
        }
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        lastError = error.localizedDescription
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
    }
}
