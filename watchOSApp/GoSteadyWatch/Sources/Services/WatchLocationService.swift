import Foundation
import CoreLocation
import Observation

@Observable
final class WatchLocationService: NSObject, CLLocationManagerDelegate {

    var currentLocation: CLLocation?
    var totalDistance: Double = 0
    var currentSpeed: Double = 0 // m/s
    var currentAltitude: Double = 0
    var totalElevationGain: Double = 0
    var routePoints: [CLLocation] = []

    private let locationManager = CLLocationManager()
    private var previousLocation: CLLocation?
    private var previousAltitude: Double?
    private var isTracking = false

    // Callback for workout service to receive location updates
    var onLocationsUpdated: (([CLLocation]) -> Void)?

    override init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.activityType = .fitness
        locationManager.allowsBackgroundLocationUpdates = true
    }

    // MARK: - Control

    func startTracking() {
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
        isTracking = true
        totalDistance = 0
        totalElevationGain = 0
        routePoints = []
        previousLocation = nil
        previousAltitude = nil
    }

    func stopTracking() {
        locationManager.stopUpdatingLocation()
        isTracking = false
    }

    // MARK: - Distance Calculation

    func calculateDistance(from: CLLocation, to: CLLocation) -> Double {
        from.distance(from: to)
    }

    // MARK: - CLLocationManagerDelegate

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let filtered = locations.filter { $0.horizontalAccuracy < 20 && $0.horizontalAccuracy > 0 }
        guard !filtered.isEmpty else { return }

        for location in filtered {
            currentLocation = location
            currentSpeed = max(0, location.speed)
            currentAltitude = location.altitude

            if let prev = previousLocation {
                let delta = location.distance(from: prev)
                if delta > 1.0 { // Ignore GPS jitter under 1m
                    totalDistance += delta
                }
            }

            // Elevation gain
            if let prevAlt = previousAltitude {
                let altDelta = location.altitude - prevAlt
                if altDelta > 0 {
                    totalElevationGain += altDelta
                }
            }

            previousLocation = location
            previousAltitude = location.altitude
            routePoints.append(location)
        }

        onLocationsUpdated?(filtered)
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("Location error: \(error.localizedDescription)")
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        switch manager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            if isTracking {
                manager.startUpdatingLocation()
            }
        default:
            break
        }
    }
}
