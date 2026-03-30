import SwiftUI

// MARK: - Full Screen Loading

struct LoadingView: View {
    var message: String = "Loading..."
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(spacing: AppSpacing.lg) {
            ProgressView()
                .scaleEffect(1.2)
                .tint(AppTheme.primary)

            Text(message)
                .font(AppTypography.bodyMedium)
                .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppTheme.adaptiveBackground(colorScheme))
    }
}

// MARK: - Inline Loading Spinner

struct InlineLoadingView: View {
    var text: String? = nil

    var body: some View {
        HStack(spacing: AppSpacing.sm) {
            ProgressView()
                .tint(AppTheme.primary)
            if let text {
                Text(text)
                    .font(AppTypography.bodySmall)
                    .foregroundStyle(AppTheme.onSurfaceVariant)
            }
        }
    }
}

// MARK: - Skeleton / Shimmer Effect

struct SkeletonView: View {
    var width: CGFloat? = nil
    var height: CGFloat = 16
    var cornerRadius: CGFloat = AppCornerRadius.small
    @State private var isAnimating = false
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius)
            .fill(
                LinearGradient(
                    colors: [
                        AppTheme.adaptiveSurfaceVariant(colorScheme),
                        AppTheme.adaptiveSurfaceVariant(colorScheme).opacity(0.5),
                        AppTheme.adaptiveSurfaceVariant(colorScheme)
                    ],
                    startPoint: isAnimating ? .trailing : .leading,
                    endPoint: isAnimating ? .leading : .trailing
                )
            )
            .frame(width: width, height: height)
            .onAppear {
                withAnimation(.linear(duration: 1.5).repeatForever(autoreverses: false)) {
                    isAnimating = true
                }
            }
    }
}

// MARK: - Skeleton Card

struct SkeletonCard: View {
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: AppSpacing.md) {
            HStack(spacing: AppSpacing.md) {
                SkeletonView(width: 44, height: 44, cornerRadius: 22)
                VStack(alignment: .leading, spacing: AppSpacing.xs) {
                    SkeletonView(width: 120, height: 14)
                    SkeletonView(width: 80, height: 10)
                }
                Spacer()
                SkeletonView(width: 60, height: 14)
            }
        }
        .padding(AppSpacing.lg)
        .background(AppTheme.adaptiveSurface(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.medium))
    }
}

// MARK: - Skeleton List

struct SkeletonList: View {
    var count: Int = 5

    var body: some View {
        VStack(spacing: AppSpacing.sm) {
            ForEach(0..<count, id: \.self) { _ in
                SkeletonCard()
            }
        }
    }
}

#Preview {
    ScrollView {
        VStack(spacing: 20) {
            LoadingView()
                .frame(height: 200)

            SkeletonList(count: 3)
                .padding(.horizontal)

            InlineLoadingView(text: "Syncing workouts...")
                .padding()
        }
    }
    .background(AppTheme.background)
}
