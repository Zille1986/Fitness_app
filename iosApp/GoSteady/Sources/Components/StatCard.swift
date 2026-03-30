import SwiftUI

struct StatCard: View {
    let icon: String
    let value: String
    let label: String
    var trend: TrendDirection? = nil
    var trendValue: String? = nil
    var color: Color = AppTheme.primary
    var compact: Bool = false
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: compact ? AppSpacing.xs : AppSpacing.sm) {
            HStack(spacing: AppSpacing.sm) {
                Image(systemName: icon)
                    .font(compact ? .caption : .subheadline)
                    .foregroundStyle(color)

                Spacer()

                if let trend, let trendValue {
                    HStack(spacing: 2) {
                        Image(systemName: trend.icon)
                            .font(.system(size: 10, weight: .bold))
                        Text(trendValue)
                            .font(AppTypography.captionSmall)
                    }
                    .foregroundStyle(trend.color)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(trend.color.opacity(0.12))
                    .clipShape(Capsule())
                }
            }

            Text(value)
                .font(compact ? AppTypography.titleMedium : AppTypography.headlineSmall)
                .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
                .lineLimit(1)
                .minimumScaleFactor(0.7)

            Text(label)
                .font(compact ? AppTypography.captionSmall : AppTypography.captionLarge)
                .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
        }
        .padding(compact ? AppSpacing.md : AppSpacing.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppTheme.adaptiveSurface(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.medium))
    }
}

enum TrendDirection {
    case up
    case down
    case flat

    var icon: String {
        switch self {
        case .up: return "arrow.up.right"
        case .down: return "arrow.down.right"
        case .flat: return "arrow.right"
        }
    }

    var color: Color {
        switch self {
        case .up: return AppTheme.success
        case .down: return AppTheme.error
        case .flat: return AppTheme.onSurfaceVariant
        }
    }
}

#Preview {
    HStack {
        StatCard(
            icon: "figure.run",
            value: "5.2 km",
            label: "Distance",
            trend: .up,
            trendValue: "+12%",
            color: AppTheme.running
        )
        StatCard(
            icon: "timer",
            value: "32:15",
            label: "Duration",
            trend: .down,
            trendValue: "-3%",
            color: AppTheme.accent
        )
    }
    .padding()
    .background(AppTheme.background)
}
