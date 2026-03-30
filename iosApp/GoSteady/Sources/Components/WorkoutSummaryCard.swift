import SwiftUI

struct WorkoutSummaryCard: View {
    let sportIcon: String
    let sportColor: Color
    let title: String
    let duration: String
    let keyMetric: String
    let date: Date
    var onTap: (() -> Void)? = nil
    @Environment(\.colorScheme) private var colorScheme

    private var formattedDate: String {
        let formatter = DateFormatter()
        let calendar = Calendar.current
        if calendar.isDateInToday(date) {
            formatter.dateFormat = "h:mm a"
            return "Today, \(formatter.string(from: date))"
        } else if calendar.isDateInYesterday(date) {
            formatter.dateFormat = "h:mm a"
            return "Yesterday, \(formatter.string(from: date))"
        } else {
            formatter.dateFormat = "EEE, MMM d"
            return formatter.string(from: date)
        }
    }

    var body: some View {
        Button {
            onTap?()
        } label: {
            HStack(spacing: AppSpacing.md) {
                // Sport icon
                ZStack {
                    Circle()
                        .fill(sportColor.opacity(0.15))
                        .frame(width: 44, height: 44)
                    Image(systemName: sportIcon)
                        .font(.body)
                        .foregroundStyle(sportColor)
                }

                // Info
                VStack(alignment: .leading, spacing: AppSpacing.xxs) {
                    Text(title)
                        .font(AppTypography.titleSmall)
                        .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
                        .lineLimit(1)

                    Text(formattedDate)
                        .font(AppTypography.captionLarge)
                        .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                }

                Spacer()

                // Stats
                VStack(alignment: .trailing, spacing: AppSpacing.xxs) {
                    Text(duration)
                        .font(AppTypography.labelMedium)
                        .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))

                    Text(keyMetric)
                        .font(AppTypography.captionLarge)
                        .foregroundStyle(sportColor)
                }

                Image(systemName: "chevron.right")
                    .font(.caption2)
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
            }
            .padding(AppSpacing.md)
            .background(AppTheme.adaptiveSurface(colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.medium))
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    VStack {
        WorkoutSummaryCard(
            sportIcon: AppTheme.SportIcon.running,
            sportColor: AppTheme.running,
            title: "5.23 km Run",
            duration: "32:15",
            keyMetric: "6:10/km",
            date: Date()
        )
        WorkoutSummaryCard(
            sportIcon: AppTheme.SportIcon.gym,
            sportColor: AppTheme.gym,
            title: "Push Day",
            duration: "1h 12m",
            keyMetric: "8 exercises",
            date: Date().addingTimeInterval(-86400)
        )
    }
    .padding()
    .background(AppTheme.background)
}
