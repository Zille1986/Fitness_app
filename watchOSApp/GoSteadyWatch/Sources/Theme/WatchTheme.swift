import SwiftUI

enum WatchTheme {
    static let primary = Color(hex: "4CAF50")
    static let secondary = Color(hex: "FF9800")
    static let background = Color.black

    static let swimming = Color(hex: "0288D1")
    static let cycling = Color(hex: "FF5722")
    static let gym = Color(hex: "FFA657")
    static let hiit = Color(hex: "FF6D00")
    static let danger = Color(hex: "E53935")

    static let zoneBelow = Color(hex: "64B5F6")
    static let zoneIn = Color(hex: "81C784")
    static let zoneAbove = Color(hex: "E57373")

    static let intervalWarmup = Color(hex: "64B5F6")
    static let intervalWork = Color(hex: "E57373")
    static let intervalRecovery = Color(hex: "81C784")
    static let intervalCooldown = Color(hex: "64B5F6")

    static func activityColor(_ type: WatchActivityType) -> Color {
        switch type {
        case .outdoorRun, .indoorRun: return primary
        case .poolSwim, .openWaterSwim: return swimming
        case .outdoorCycle, .indoorCycle: return cycling
        case .gym: return gym
        case .hiit: return hiit
        }
    }

    static func zoneColor(_ zone: HRZone) -> Color {
        switch zone {
        case .zone1: return .gray
        case .zone2: return .blue
        case .zone3: return primary
        case .zone4: return secondary
        case .zone5: return danger
        }
    }
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 6: (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default: (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(.sRGB, red: Double(r) / 255, green: Double(g) / 255, blue: Double(b) / 255, opacity: Double(a) / 255)
    }
}
