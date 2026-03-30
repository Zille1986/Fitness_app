import SwiftUI

struct EmptyStateView: View {
    let icon: String
    let title: String
    let message: String
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(spacing: AppSpacing.lg) {
            Spacer()

            ZStack {
                Circle()
                    .fill(AppTheme.primary.opacity(0.1))
                    .frame(width: 80, height: 80)
                Image(systemName: icon)
                    .font(.system(size: 32))
                    .foregroundStyle(AppTheme.primary.opacity(0.6))
            }

            VStack(spacing: AppSpacing.sm) {
                Text(title)
                    .font(AppTypography.titleMedium)
                    .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))

                Text(message)
                    .font(AppTypography.bodyMedium)
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, AppSpacing.xxl)
            }

            if let actionTitle, let action {
                Button(action: action) {
                    Text(actionTitle)
                        .font(AppTypography.labelLarge)
                        .foregroundStyle(.white)
                        .padding(.horizontal, AppSpacing.xxl)
                        .padding(.vertical, AppSpacing.md)
                        .background(AppTheme.primary)
                        .clipShape(Capsule())
                }
                .padding(.top, AppSpacing.sm)
            }

            Spacer()
        }
        .frame(maxWidth: .infinity)
        .padding(AppSpacing.xxxl)
        .background(AppTheme.adaptiveSurface(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.large))
    }
}

#Preview {
    EmptyStateView(
        icon: "figure.run",
        title: "No Workouts Yet",
        message: "Start your first workout to see your progress here",
        actionTitle: "Start Workout"
    ) {
        // action
    }
    .padding()
    .background(AppTheme.background)
}
