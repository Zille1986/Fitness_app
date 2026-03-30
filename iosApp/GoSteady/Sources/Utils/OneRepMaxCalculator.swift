import Foundation

struct OneRepMaxCalculator {

    /// Brzycki formula: 1RM = weight * (36 / (37 - reps))
    static func calculate(weight: Double, reps: Int) -> Double {
        guard reps > 0, weight > 0 else { return 0 }
        if reps == 1 { return weight }
        return weight * (36.0 / (37.0 - Double(reps)))
    }

    /// Inverse Brzycki: estimate weight for a target rep count given a 1RM.
    static func estimateWeight(oneRepMax: Double, targetReps: Int) -> Double {
        guard targetReps > 0, oneRepMax > 0 else { return 0 }
        if targetReps == 1 { return oneRepMax }
        return oneRepMax * (37.0 - Double(targetReps)) / 36.0
    }

    /// Approximate percentage of 1RM for a given rep count.
    static func percentageOfMax(forReps reps: Int) -> Double {
        switch reps {
        case 1:  return 1.00
        case 2:  return 0.97
        case 3:  return 0.94
        case 4:  return 0.92
        case 5:  return 0.89
        case 6:  return 0.86
        case 7:  return 0.83
        case 8:  return 0.81
        case 9:  return 0.78
        case 10: return 0.75
        case 11: return 0.73
        case 12: return 0.71
        default: return reps > 12 ? 0.65 : 1.0
        }
    }

    /// Epley formula as an alternative: 1RM = weight * (1 + reps / 30).
    static func calculateEpley(weight: Double, reps: Int) -> Double {
        guard reps > 0, weight > 0 else { return 0 }
        if reps == 1 { return weight }
        return weight * (1.0 + Double(reps) / 30.0)
    }

    /// Return the average of Brzycki and Epley for a combined estimate.
    static func calculateCombined(weight: Double, reps: Int) -> Double {
        let brzycki = calculate(weight: weight, reps: reps)
        let epley = calculateEpley(weight: weight, reps: reps)
        return (brzycki + epley) / 2.0
    }

    /// Format 1RM value as a display string.
    static func format(_ oneRepMax: Double, useMetric: Bool = true) -> String {
        let unit = useMetric ? "kg" : "lb"
        if oneRepMax >= 100 {
            return String(format: "%.0f %@", oneRepMax, unit)
        }
        return String(format: "%.1f %@", oneRepMax, unit)
    }
}
