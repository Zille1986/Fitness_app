import SwiftUI

struct ActivityRingsView: View {
    let moveProgress: Double
    let exerciseProgress: Double
    let standProgress: Double
    var moveColor: Color = AppTheme.error
    var exerciseColor: Color = AppTheme.success
    var standColor: Color = AppTheme.accent
    var lineWidth: CGFloat = 14
    var size: CGFloat = 120
    var animated: Bool = true

    @State private var animatedMoveProgress: Double = 0
    @State private var animatedExerciseProgress: Double = 0
    @State private var animatedStandProgress: Double = 0

    var body: some View {
        ZStack {
            // Stand ring (outermost)
            RingView(
                progress: animated ? animatedStandProgress : standProgress,
                color: standColor,
                lineWidth: lineWidth * 0.75,
                size: size - lineWidth * 2.5 * 2
            )

            // Exercise ring (middle)
            RingView(
                progress: animated ? animatedExerciseProgress : exerciseProgress,
                color: exerciseColor,
                lineWidth: lineWidth * 0.85,
                size: size - lineWidth * 1.25 * 2
            )

            // Move ring (outermost)
            RingView(
                progress: animated ? animatedMoveProgress : moveProgress,
                color: moveColor,
                lineWidth: lineWidth,
                size: size
            )
        }
        .frame(width: size, height: size)
        .onAppear {
            guard animated else { return }
            withAnimation(.easeOut(duration: 1.2)) {
                animatedMoveProgress = moveProgress
            }
            withAnimation(.easeOut(duration: 1.0).delay(0.1)) {
                animatedExerciseProgress = exerciseProgress
            }
            withAnimation(.easeOut(duration: 0.8).delay(0.2)) {
                animatedStandProgress = standProgress
            }
        }
    }
}

struct RingView: View {
    let progress: Double
    let color: Color
    let lineWidth: CGFloat
    let size: CGFloat

    private var clampedProgress: Double {
        min(max(progress, 0), 2.0)
    }

    var body: some View {
        ZStack {
            // Background track
            Circle()
                .stroke(color.opacity(0.2), lineWidth: lineWidth)
                .frame(width: size, height: size)

            // Progress arc
            Circle()
                .trim(from: 0, to: clampedProgress)
                .stroke(
                    AngularGradient(
                        gradient: Gradient(colors: [color, color.opacity(0.8)]),
                        center: .center,
                        startAngle: .degrees(0),
                        endAngle: .degrees(360 * clampedProgress)
                    ),
                    style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
                )
                .frame(width: size, height: size)
                .rotationEffect(.degrees(-90))

            // End cap dot
            if clampedProgress > 0.02 {
                Circle()
                    .fill(color)
                    .frame(width: lineWidth, height: lineWidth)
                    .offset(y: -size / 2)
                    .rotationEffect(.degrees(360 * clampedProgress - 90))
                    .shadow(color: color.opacity(0.5), radius: 3, x: 0, y: 0)
            }
        }
    }
}

// MARK: - Activity Rings with Labels

struct ActivityRingsWithLabels: View {
    let moveCalories: Int
    let moveGoal: Int
    let exerciseMinutes: Int
    let exerciseGoal: Int
    let standHours: Int
    let standGoal: Int
    var size: CGFloat = 120
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(spacing: AppSpacing.xxl) {
            ActivityRingsView(
                moveProgress: Double(moveCalories) / Double(moveGoal),
                exerciseProgress: Double(exerciseMinutes) / Double(exerciseGoal),
                standProgress: Double(standHours) / Double(standGoal),
                size: size
            )

            VStack(alignment: .leading, spacing: AppSpacing.md) {
                RingLabel(
                    color: AppTheme.error,
                    label: "Move",
                    value: "\(moveCalories)/\(moveGoal)",
                    unit: "CAL"
                )
                RingLabel(
                    color: AppTheme.success,
                    label: "Exercise",
                    value: "\(exerciseMinutes)/\(exerciseGoal)",
                    unit: "MIN"
                )
                RingLabel(
                    color: AppTheme.accent,
                    label: "Stand",
                    value: "\(standHours)/\(standGoal)",
                    unit: "HRS"
                )
            }
        }
    }
}

struct RingLabel: View {
    let color: Color
    let label: String
    let value: String
    let unit: String
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(spacing: AppSpacing.sm) {
            Circle()
                .fill(color)
                .frame(width: 8, height: 8)

            VStack(alignment: .leading, spacing: 0) {
                Text(label)
                    .font(AppTypography.captionSmall)
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                HStack(spacing: 2) {
                    Text(value)
                        .font(AppTypography.labelMedium)
                        .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
                    Text(unit)
                        .font(AppTypography.captionSmall)
                        .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                }
            }
        }
    }
}

#Preview {
    VStack(spacing: 32) {
        ActivityRingsView(
            moveProgress: 0.75,
            exerciseProgress: 0.6,
            standProgress: 0.9
        )

        ActivityRingsWithLabels(
            moveCalories: 420,
            moveGoal: 600,
            exerciseMinutes: 22,
            exerciseGoal: 30,
            standHours: 9,
            standGoal: 12
        )
    }
    .padding()
    .background(AppTheme.background)
}
