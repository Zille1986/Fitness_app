import Foundation
import Observation
import WatchKit

@Observable
final class WatchSettingsViewModel {

    var useMetric: Bool {
        didSet { save() }
    }
    var hapticFeedback: Bool {
        didSet { save() }
    }
    var autoPauseEnabled: Bool {
        didSet { save() }
    }
    var maxHeartRate: Int {
        didSet { save() }
    }
    var poolLength: PoolLength {
        didSet { save() }
    }

    // Heart rate zone boundaries (derived from maxHR)
    var zone1Range: ClosedRange<Int> { HRZone.zone1.range(maxHR: maxHeartRate) }
    var zone2Range: ClosedRange<Int> { HRZone.zone2.range(maxHR: maxHeartRate) }
    var zone3Range: ClosedRange<Int> { HRZone.zone3.range(maxHR: maxHeartRate) }
    var zone4Range: ClosedRange<Int> { HRZone.zone4.range(maxHR: maxHeartRate) }
    var zone5Range: ClosedRange<Int> { HRZone.zone5.range(maxHR: maxHeartRate) }

    var distanceUnit: String { useMetric ? "km" : "mi" }
    var weightUnit: String { useMetric ? "kg" : "lb" }

    private let defaults = UserDefaults.standard

    init() {
        useMetric = defaults.object(forKey: "useMetric") as? Bool ?? true
        hapticFeedback = defaults.object(forKey: "hapticFeedback") as? Bool ?? true
        autoPauseEnabled = defaults.object(forKey: "autoPauseEnabled") as? Bool ?? true
        maxHeartRate = defaults.object(forKey: "maxHeartRate") as? Int ?? 190
        let poolRaw = defaults.object(forKey: "poolLength") as? Int ?? 25
        poolLength = PoolLength(rawValue: poolRaw) ?? .m25
    }

    private func save() {
        defaults.set(useMetric, forKey: "useMetric")
        defaults.set(hapticFeedback, forKey: "hapticFeedback")
        defaults.set(autoPauseEnabled, forKey: "autoPauseEnabled")
        defaults.set(maxHeartRate, forKey: "maxHeartRate")
        defaults.set(poolLength.rawValue, forKey: "poolLength")
    }

    func convertDistance(_ meters: Double) -> Double {
        useMetric ? meters / 1000.0 : meters / 1609.344
    }

    func formattedDistance(_ meters: Double) -> String {
        let value = convertDistance(meters)
        return String(format: "%.2f %@", value, distanceUnit)
    }

    func convertWeight(_ kg: Double) -> Double {
        useMetric ? kg : kg * 2.20462
    }

    func resetToDefaults() {
        useMetric = true
        hapticFeedback = true
        autoPauseEnabled = true
        maxHeartRate = 190
        poolLength = .m25
    }
}
