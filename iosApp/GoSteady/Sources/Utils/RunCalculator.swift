import Foundation

struct RunCalculator {

    // MARK: - Pace Calculation

    /// Pace in seconds per kilometer from distance (meters) and duration (milliseconds).
    static func paceSecondsPerKm(distanceMeters: Double, durationMillis: Int64) -> Double {
        let distanceKm = distanceMeters / 1000.0
        guard distanceKm > 0 else { return 0 }
        return Double(durationMillis) / 1000.0 / distanceKm
    }

    /// Pace in seconds per mile.
    static func paceSecondsPerMile(distanceMeters: Double, durationMillis: Int64) -> Double {
        let distanceMiles = distanceMeters / 1609.344
        guard distanceMiles > 0 else { return 0 }
        return Double(durationMillis) / 1000.0 / distanceMiles
    }

    /// Format pace (seconds per km) into "M:SS" or "--:--".
    static func formatPace(_ secondsPerKm: Double) -> String {
        guard secondsPerKm > 0, !secondsPerKm.isInfinite, !secondsPerKm.isNaN else { return "--:--" }
        let minutes = Int(secondsPerKm / 60)
        let seconds = Int(secondsPerKm.truncatingRemainder(dividingBy: 60))
        return String(format: "%d:%02d", minutes, seconds)
    }

    /// Speed in km/h from pace (seconds per km).
    static func speedKmh(fromPaceSecondsPerKm pace: Double) -> Double {
        guard pace > 0 else { return 0 }
        return 3600.0 / pace
    }

    /// Pace (seconds per km) from speed in km/h.
    static func paceSecondsPerKm(fromSpeedKmh speed: Double) -> Double {
        guard speed > 0 else { return 0 }
        return 3600.0 / speed
    }

    // MARK: - Distance Formatting

    static func formatDistanceKm(_ meters: Double) -> String {
        let km = meters / 1000.0
        if km >= 100 {
            return String(format: "%.0f km", km)
        } else if km >= 10 {
            return String(format: "%.1f km", km)
        } else {
            return String(format: "%.2f km", km)
        }
    }

    static func formatDistanceMiles(_ meters: Double) -> String {
        let miles = meters / 1609.344
        if miles >= 100 {
            return String(format: "%.0f mi", miles)
        } else if miles >= 10 {
            return String(format: "%.1f mi", miles)
        } else {
            return String(format: "%.2f mi", miles)
        }
    }

    static func formatDistance(_ meters: Double, useMetric: Bool = true) -> String {
        useMetric ? formatDistanceKm(meters) : formatDistanceMiles(meters)
    }

    // MARK: - Split Times

    /// Calculate per-kilometer splits from an array of route points.
    static func calculateSplits(from routePoints: [RoutePoint]) -> [Split] {
        guard routePoints.count >= 2 else { return [] }

        var splits: [Split] = []
        var currentKm = 1
        var cumulativeDistance: Double = 0
        var splitStartIndex = 0
        var lastPoint = routePoints[0]

        for i in 1..<routePoints.count {
            let point = routePoints[i]
            let segmentDistance = haversineDistance(
                lat1: lastPoint.latitude, lon1: lastPoint.longitude,
                lat2: point.latitude, lon2: point.longitude
            )
            cumulativeDistance += segmentDistance
            lastPoint = point

            if cumulativeDistance >= Double(currentKm) * 1000.0 {
                let splitStartTime = routePoints[splitStartIndex].timestamp
                let splitEndTime = point.timestamp
                let durationMs = Int64(splitEndTime.timeIntervalSince(splitStartTime) * 1000)
                let paceSecPerKm = durationMs > 0 ? Double(durationMs) / 1000.0 : 0

                let elevationChange: Double = {
                    guard let startAlt = routePoints[splitStartIndex].altitude,
                          let endAlt = point.altitude else { return 0 }
                    return endAlt - startAlt
                }()

                let heartRates = routePoints[splitStartIndex...i].compactMap(\.heartRate)
                let avgHR: Int? = heartRates.isEmpty ? nil : heartRates.reduce(0, +) / heartRates.count

                splits.append(Split(
                    kilometer: currentKm,
                    durationMillis: durationMs,
                    paceSecondsPerKm: paceSecPerKm,
                    elevationChange: elevationChange,
                    avgHeartRate: avgHR
                ))

                currentKm += 1
                splitStartIndex = i
            }
        }

        return splits
    }

    // MARK: - Elevation

    /// Total elevation gain from route points (only positive changes).
    static func calculateElevationGain(from routePoints: [RoutePoint]) -> Double {
        guard routePoints.count >= 2 else { return 0 }
        var gain: Double = 0
        var previousAlt: Double?
        for point in routePoints {
            guard let alt = point.altitude else { continue }
            if let prev = previousAlt, alt > prev {
                gain += (alt - prev)
            }
            previousAlt = alt
        }
        return gain
    }

    /// Total elevation loss from route points (only negative changes).
    static func calculateElevationLoss(from routePoints: [RoutePoint]) -> Double {
        guard routePoints.count >= 2 else { return 0 }
        var loss: Double = 0
        var previousAlt: Double?
        for point in routePoints {
            guard let alt = point.altitude else { continue }
            if let prev = previousAlt, alt < prev {
                loss += (prev - alt)
            }
            previousAlt = alt
        }
        return loss
    }

    // MARK: - Calorie Estimation

    /// Estimate calories burned during a run using MET values based on speed.
    static func estimateCalories(distanceKm: Double, durationMinutes: Double, weightKg: Double) -> Int {
        guard durationMinutes > 0 else { return 0 }
        let speedKmH = (distanceKm / durationMinutes) * 60.0
        let met: Double
        switch speedKmH {
        case ..<6:   met = 6.0
        case 6..<8:  met = 8.3
        case 8..<10: met = 9.8
        case 10..<12: met = 11.0
        case 12..<14: met = 11.8
        default:      met = 12.8
        }
        return Int((met * 3.5 * weightKg / 200.0) * durationMinutes)
    }

    // MARK: - Haversine

    static func haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double) -> Double {
        let earthRadius = 6371000.0 // meters
        let dLat = (lat2 - lat1) * .pi / 180
        let dLon = (lon2 - lon1) * .pi / 180
        let a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1 * .pi / 180) * cos(lat2 * .pi / 180) *
                sin(dLon / 2) * sin(dLon / 2)
        let c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    // MARK: - Predicted Times

    /// Predict finish time for a target distance given a reference performance.
    /// Uses Riegel formula: T2 = T1 * (D2/D1)^1.06
    static func predictTime(
        referenceDistanceMeters: Double,
        referenceTimeMillis: Int64,
        targetDistanceMeters: Double
    ) -> Int64 {
        guard referenceDistanceMeters > 0, referenceTimeMillis > 0 else { return 0 }
        let ratio = targetDistanceMeters / referenceDistanceMeters
        let predictedMs = Double(referenceTimeMillis) * pow(ratio, 1.06)
        return Int64(predictedMs)
    }
}
