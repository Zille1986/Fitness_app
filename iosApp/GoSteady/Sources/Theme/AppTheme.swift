import SwiftUI

// MARK: - Color Theme

enum AppTheme {
    // MARK: - Primary (Green)
    static let primary = Color(hex: "4CAF50")
    static let primaryDim = Color(hex: "388E3C")
    static let primaryContainer = Color(hex: "C8E6C9")
    static let onPrimary = Color.white
    static let onPrimaryContainer = Color(hex: "1B5E20")

    // MARK: - Secondary (Orange)
    static let secondary = Color(hex: "FF9800")
    static let secondaryContainer = Color(hex: "FFE0B2")
    static let onSecondary = Color.white
    static let onSecondaryContainer = Color(hex: "E65100")

    // MARK: - Accent (Blue)
    static let accent = Color(hex: "2196F3")
    static let accentContainer = Color(hex: "BBDEFB")

    // MARK: - Tertiary (Teal)
    static let tertiary = Color(hex: "009688")
    static let tertiaryContainer = Color(hex: "B2DFDB")

    // MARK: - Dark Surfaces
    static let background = Color(hex: "121212")
    static let surface = Color(hex: "1E1E1E")
    static let surfaceVariant = Color(hex: "2A2A2A")
    static let surfaceContainer = Color(hex: "252525")
    static let surfaceContainerLow = Color(hex: "1C1C1C")
    static let surfaceContainerHigh = Color(hex: "333333")

    // MARK: - Light Surfaces
    static let lightBackground = Color(hex: "FAFAFA")
    static let lightSurface = Color(hex: "FFFFFF")
    static let lightSurfaceVariant = Color(hex: "F5F5F5")

    // MARK: - Text
    static let onSurface = Color(hex: "E0E0E0")
    static let onSurfaceVariant = Color(hex: "9E9E9E")
    static let onBackground = Color(hex: "E0E0E0")

    // MARK: - Light Text
    static let lightOnSurface = Color(hex: "212121")
    static let lightOnSurfaceVariant = Color(hex: "757575")

    // MARK: - Error
    static let error = Color(hex: "CF6679")
    static let errorContainer = Color(hex: "FDECEA")
    static let onError = Color.white

    // MARK: - Success
    static let success = Color(hex: "4CAF50")

    // MARK: - Warning
    static let warning = Color(hex: "FFC107")

    // MARK: - Activity Colors
    static let running = Color(hex: "4CAF50")
    static let swimming = Color(hex: "00BCD4")
    static let cycling = Color(hex: "8BC34A")
    static let gym = Color(hex: "FF9800")
    static let hiit = Color(hex: "F44336")
    static let nutrition = Color(hex: "FF5722")
    static let training = Color(hex: "2196F3")
    static let mindfulness = Color(hex: "9C27B0")

    // MARK: - Sport SF Symbols
    enum SportIcon {
        static let running = "figure.run"
        static let gym = "dumbbell.fill"
        static let swimming = "figure.pool.swim"
        static let cycling = "bicycle"
        static let hiit = "bolt.heart.fill"
        static let walking = "figure.walk"
        static let yoga = "figure.mind.and.body"
        static let nutrition = "fork.knife"
        static let training = "calendar.badge.clock"
        static let mindfulness = "brain.head.profile"
        static let analytics = "chart.bar.xaxis"
        static let achievements = "trophy.fill"
        static let profile = "person.circle.fill"
        static let settings = "gearshape.fill"
        static let heart = "heart.fill"
        static let fire = "flame.fill"
        static let timer = "timer"
        static let distance = "location.fill"
        static let speed = "gauge.with.dots.needle.67percent"
        static let elevation = "mountain.2.fill"
        static let steps = "shoeprints.fill"
        static let sleep = "moon.zzz.fill"
        static let water = "drop.fill"
        static let weight = "scalemass.fill"
        static let coach = "bubble.left.and.text.bubble.right.fill"
    }

    // MARK: - Strava
    static let strava = Color(hex: "FC4C02")
}

// MARK: - Typography

enum AppTypography {
    static let displayLarge = Font.system(size: 57, weight: .regular)
    static let displayMedium = Font.system(size: 45, weight: .regular)
    static let displaySmall = Font.system(size: 36, weight: .regular)

    static let headlineLarge = Font.system(size: 32, weight: .bold)
    static let headlineMedium = Font.system(size: 28, weight: .bold)
    static let headlineSmall = Font.system(size: 24, weight: .semibold)

    static let titleLarge = Font.system(size: 22, weight: .semibold)
    static let titleMedium = Font.system(size: 16, weight: .semibold)
    static let titleSmall = Font.system(size: 14, weight: .semibold)

    static let bodyLarge = Font.system(size: 16, weight: .regular)
    static let bodyMedium = Font.system(size: 14, weight: .regular)
    static let bodySmall = Font.system(size: 12, weight: .regular)

    static let labelLarge = Font.system(size: 14, weight: .medium)
    static let labelMedium = Font.system(size: 12, weight: .medium)
    static let labelSmall = Font.system(size: 11, weight: .medium)

    static let captionLarge = Font.system(size: 12, weight: .regular)
    static let captionSmall = Font.system(size: 10, weight: .regular)

    // Rounded variants for numbers/stats
    static let statLarge = Font.system(size: 48, weight: .bold, design: .rounded)
    static let statMedium = Font.system(size: 32, weight: .bold, design: .rounded)
    static let statSmall = Font.system(size: 20, weight: .bold, design: .rounded)
}

// MARK: - Spacing

enum AppSpacing {
    static let xxs: CGFloat = 2
    static let xs: CGFloat = 4
    static let sm: CGFloat = 8
    static let md: CGFloat = 12
    static let lg: CGFloat = 16
    static let xl: CGFloat = 20
    static let xxl: CGFloat = 24
    static let xxxl: CGFloat = 32
    static let xxxxl: CGFloat = 48
}

// MARK: - Corner Radius

enum AppCornerRadius {
    static let small: CGFloat = 8
    static let medium: CGFloat = 12
    static let large: CGFloat = 16
    static let extraLarge: CGFloat = 20
    static let pill: CGFloat = 28
    static let circle: CGFloat = 999
}

// MARK: - Shadows

struct AppShadow {
    static func small(_ color: Color = .black.opacity(0.1)) -> some View {
        Color.clear.shadow(color: color, radius: 4, x: 0, y: 2)
    }

    static func medium(_ color: Color = .black.opacity(0.15)) -> some View {
        Color.clear.shadow(color: color, radius: 8, x: 0, y: 4)
    }

    static func large(_ color: Color = .black.opacity(0.2)) -> some View {
        Color.clear.shadow(color: color, radius: 16, x: 0, y: 8)
    }
}

// MARK: - View Modifiers

struct CardStyle: ViewModifier {
    @Environment(\.colorScheme) var colorScheme

    func body(content: Content) -> some View {
        content
            .background(colorScheme == .dark ? AppTheme.surface : AppTheme.lightSurface)
            .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.large))
            .shadow(color: .black.opacity(colorScheme == .dark ? 0.3 : 0.08), radius: 8, x: 0, y: 2)
    }
}

struct SectionHeaderStyle: ViewModifier {
    func body(content: Content) -> some View {
        content
            .font(AppTypography.labelLarge)
            .foregroundStyle(AppTheme.onSurfaceVariant)
            .textCase(.uppercase)
            .tracking(0.5)
    }
}

extension View {
    func cardStyle() -> some View {
        modifier(CardStyle())
    }

    func sectionHeaderStyle() -> some View {
        modifier(SectionHeaderStyle())
    }
}

// MARK: - Color(hex:)

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

// MARK: - Adaptive Colors

extension AppTheme {
    static func adaptiveBackground(_ colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? background : lightBackground
    }

    static func adaptiveSurface(_ colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? surface : lightSurface
    }

    static func adaptiveSurfaceVariant(_ colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? surfaceVariant : lightSurfaceVariant
    }

    static func adaptiveOnSurface(_ colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? onSurface : lightOnSurface
    }

    static func adaptiveOnSurfaceVariant(_ colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? onSurfaceVariant : lightOnSurfaceVariant
    }
}
