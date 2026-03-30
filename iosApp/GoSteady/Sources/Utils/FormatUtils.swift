import Foundation

struct FormatUtils {

    // MARK: - Distance

    static func formatDistance(_ meters: Double, useMetric: Bool = true) -> String {
        if useMetric {
            let km = meters / 1000.0
            if km >= 100 { return String(format: "%.0f km", km) }
            if km >= 10  { return String(format: "%.1f km", km) }
            return String(format: "%.2f km", km)
        } else {
            let miles = meters / 1609.344
            if miles >= 100 { return String(format: "%.0f mi", miles) }
            if miles >= 10  { return String(format: "%.1f mi", miles) }
            return String(format: "%.2f mi", miles)
        }
    }

    static func formatDistanceShort(_ meters: Double, useMetric: Bool = true) -> String {
        if useMetric {
            let km = meters / 1000.0
            return km >= 10 ? String(format: "%.0f", km) : String(format: "%.1f", km)
        } else {
            let miles = meters / 1609.344
            return miles >= 10 ? String(format: "%.0f", miles) : String(format: "%.1f", miles)
        }
    }

    // MARK: - Pace

    static func formatPace(_ secondsPerKm: Double) -> String {
        guard secondsPerKm > 0, !secondsPerKm.isInfinite, !secondsPerKm.isNaN else { return "--:--" }
        let minutes = Int(secondsPerKm / 60)
        let seconds = Int(secondsPerKm.truncatingRemainder(dividingBy: 60))
        return String(format: "%d:%02d", minutes, seconds)
    }

    static func formatPaceWithUnit(_ secondsPerKm: Double, useMetric: Bool = true) -> String {
        let value = useMetric ? secondsPerKm : secondsPerKm * 1.60934
        let unit = useMetric ? "/km" : "/mi"
        return "\(formatPace(value)) \(unit)"
    }

    // MARK: - Duration

    static func formatDuration(milliseconds: Int64) -> String {
        let totalSeconds = milliseconds / 1000
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        }
        return String(format: "%d:%02d", minutes, seconds)
    }

    static func formatDurationLong(milliseconds: Int64) -> String {
        let totalMinutes = milliseconds / 60000
        let hours = totalMinutes / 60
        let minutes = totalMinutes % 60
        if hours > 0 {
            return "\(hours)h \(minutes)m"
        }
        return "\(minutes)m"
    }

    static func formatDurationSeconds(_ totalSeconds: Int) -> String {
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        }
        return String(format: "%d:%02d", minutes, seconds)
    }

    // MARK: - Weight

    static func formatWeight(_ kg: Double, useMetric: Bool = true) -> String {
        if useMetric {
            return kg >= 100 ? String(format: "%.0f kg", kg) : String(format: "%.1f kg", kg)
        } else {
            let lb = kg * 2.20462
            return lb >= 100 ? String(format: "%.0f lb", lb) : String(format: "%.1f lb", lb)
        }
    }

    static func formatWeightShort(_ kg: Double, useMetric: Bool = true) -> String {
        if useMetric {
            return kg >= 100 ? String(format: "%.0f", kg) : String(format: "%.1f", kg)
        } else {
            let lb = kg * 2.20462
            return lb >= 100 ? String(format: "%.0f", lb) : String(format: "%.1f", lb)
        }
    }

    // MARK: - Volume

    static func formatVolume(_ volume: Double) -> String {
        if volume >= 1_000_000 {
            return String(format: "%.1fM kg", volume / 1_000_000)
        } else if volume >= 1000 {
            return String(format: "%.1fk kg", volume / 1000)
        }
        return String(format: "%.0f kg", volume)
    }

    // MARK: - Calories

    static func formatCalories(_ calories: Int) -> String {
        if calories >= 10000 {
            return String(format: "%.1fk", Double(calories) / 1000.0)
        }
        return "\(calories)"
    }

    static func formatCaloriesWithUnit(_ calories: Int) -> String {
        "\(formatCalories(calories)) kcal"
    }

    // MARK: - Heart Rate

    static func formatHeartRate(_ bpm: Int) -> String {
        "\(bpm) bpm"
    }

    // MARK: - Speed

    static func formatSpeed(_ kmh: Double) -> String {
        String(format: "%.1f km/h", kmh)
    }

    static func formatSpeedWithUnit(_ kmh: Double, useMetric: Bool = true) -> String {
        if useMetric {
            return String(format: "%.1f km/h", kmh)
        } else {
            return String(format: "%.1f mph", kmh * 0.621371)
        }
    }

    // MARK: - Elevation

    static func formatElevation(_ meters: Double, useMetric: Bool = true) -> String {
        if useMetric {
            return String(format: "%.0f m", meters)
        } else {
            return String(format: "%.0f ft", meters * 3.28084)
        }
    }

    // MARK: - Percentage

    static func formatPercentage(_ value: Double) -> String {
        String(format: "%.0f%%", value * 100)
    }

    static func formatPercentage(_ value: Float) -> String {
        String(format: "%.0f%%", value * 100)
    }

    // MARK: - Power

    static func formatPower(_ watts: Int) -> String {
        "\(watts) W"
    }

    // MARK: - Water

    static func formatWater(_ ml: Int) -> String {
        if ml >= 1000 {
            return String(format: "%.1f L", Double(ml) / 1000.0)
        }
        return "\(ml) ml"
    }
}
