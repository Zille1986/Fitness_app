import SwiftUI

struct DashboardHeroBanner: View {
    let icon: String
    let title: String
    let subtitle: String
    var actionTitle: String? = nil
    var secondaryActionTitle: String? = nil
    var tag: String? = nil
    var tagDetail: String? = nil
    var gradientColors: [Color] = [AppTheme.primary.opacity(0.4), AppTheme.primary.opacity(0.9)]
    var onAction: (() -> Void)? = nil
    var onSecondaryAction: (() -> Void)? = nil

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            // Background gradient
            Rectangle()
                .fill(
                    LinearGradient(
                        colors: gradientColors,
                        startPoint: .topTrailing,
                        endPoint: .bottomLeading
                    )
                )
                .frame(height: 240)
                .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.extraLarge))
                .overlay(alignment: .topTrailing) {
                    Image(systemName: icon)
                        .font(.system(size: 80))
                        .foregroundStyle(.white.opacity(0.1))
                        .offset(x: -20, y: 20)
                }

            // Content
            VStack(alignment: .leading, spacing: AppSpacing.sm) {
                if let tag {
                    HStack(spacing: AppSpacing.sm) {
                        Text(tag.uppercased())
                            .font(AppTypography.captionSmall)
                            .fontWeight(.bold)
                            .padding(.horizontal, AppSpacing.md)
                            .padding(.vertical, AppSpacing.xs)
                            .background(.white.opacity(0.25))
                            .foregroundStyle(.white)
                            .clipShape(Capsule())

                        if let tagDetail {
                            Text(tagDetail)
                                .font(AppTypography.captionLarge)
                                .foregroundStyle(.white.opacity(0.7))
                        }
                    }
                }

                Text(title)
                    .font(AppTypography.headlineMedium)
                    .foregroundStyle(.white)

                Text(subtitle)
                    .font(AppTypography.bodyMedium)
                    .foregroundStyle(.white.opacity(0.7))
                    .lineLimit(2)

                HStack(spacing: AppSpacing.md) {
                    if let actionTitle, let onAction {
                        Button(action: onAction) {
                            Text(actionTitle)
                                .font(AppTypography.labelLarge)
                                .foregroundStyle(AppTheme.onPrimaryContainer)
                                .padding(.horizontal, AppSpacing.xl)
                                .padding(.vertical, AppSpacing.sm + 2)
                                .background(AppTheme.primaryContainer)
                                .clipShape(Capsule())
                        }
                    }

                    if let secondaryActionTitle, let onSecondaryAction {
                        Button(action: onSecondaryAction) {
                            Text(secondaryActionTitle)
                                .font(AppTypography.labelLarge)
                                .foregroundStyle(.white)
                                .padding(.horizontal, AppSpacing.xl)
                                .padding(.vertical, AppSpacing.sm + 2)
                                .background(.white.opacity(0.2))
                                .clipShape(Capsule())
                        }
                    }
                }
                .padding(.top, AppSpacing.sm)
            }
            .padding(AppSpacing.xl)
        }
        .padding(.horizontal)
    }
}

#Preview {
    VStack {
        DashboardHeroBanner(
            icon: "figure.run",
            title: "Ready to Train",
            subtitle: "Start a new session and push your limits.",
            actionTitle: "Start Session",
            secondaryActionTitle: "View Details",
            tag: "Free Run",
            tagDetail: "45 Minutes"
        )
    }
    .background(AppTheme.background)
}
