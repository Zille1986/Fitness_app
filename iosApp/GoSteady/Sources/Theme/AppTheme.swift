import SwiftUI

/// Design system matching the Android "Serene Balance" light theme
/// and the "Premium Dark" theme.
enum AppTheme {
    // MARK: - Primary (Forest Green)
    static let primary = Color(hex: "3F6758")
    static let primaryDim = Color(hex: "335B4D")
    static let primaryContainer = Color(hex: "C0ECDA")
    static let onPrimary = Color(hex: "E5FFF2")
    static let onPrimaryContainer = Color(hex: "32594B")

    // MARK: - Secondary (Slate Blue)
    static let secondary = Color(hex: "516170")
    static let secondaryContainer = Color(hex: "D4E4F6")

    // MARK: - Tertiary (Teal)
    static let tertiary = Color(hex: "2E6771")
    static let tertiaryContainer = Color(hex: "B7EFFB")

    // MARK: - Surfaces (Light)
    static let surface = Color(hex: "F9F9F7")
    static let surfaceContainer = Color(hex: "ECEEEC")
    static let surfaceContainerLow = Color(hex: "F3F4F2")

    // MARK: - Text
    static let onSurface = Color(hex: "2F3332")
    static let onSurfaceVariant = Color(hex: "5C605E")

    // MARK: - Error
    static let error = Color(hex: "A83836")

    // MARK: - Dark Theme
    static let darkPrimary = Color(hex: "00E5CC")
    static let darkBackground = Color(hex: "080B14")
    static let darkSurface = Color(hex: "111827")
    static let darkOnSurface = Color(hex: "E8EAED")

    // MARK: - Activity Colors
    static let running = Color(hex: "00E5CC")
    static let swimming = Color(hex: "00B8D4")
    static let cycling = Color(hex: "7EE787")
    static let gym = Color(hex: "FFA657")
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 6:
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8:
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}
