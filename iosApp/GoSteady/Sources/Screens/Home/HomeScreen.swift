import SwiftUI
import SwiftData

struct HomeScreen: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.colorScheme) private var colorScheme
    @State private var viewModel = HomeViewModel()
    @State private var showManualEntry = false
    @State private var selectedSportForManual: SportType = .run

    var body: some View {
        NavigationStack {
            ScrollView {
                if viewModel.isLoading {
                    SkeletonList(count: 4)
                        .padding(.horizontal)
                        .padding(.top)
                } else {
                    VStack(spacing: AppSpacing.xl) {
                        // Greeting
                        greetingSection

                        // Readiness Score
                        readinessSection

                        // Active Streak
                        if viewModel.activeStreak > 0 {
                            streakBanner
                        }

                        // Today's Workout Plan
                        if let upcoming = viewModel.upcomingWorkout {
                            upcomingWorkoutCard(upcoming)
                        }

                        // Weekly Activity Grid
                        weeklyActivitySection

                        // Quick Start Buttons
                        quickStartSection

                        // Recent Workouts
                        recentWorkoutsSection

                        Spacer(minLength: 80)
                    }
                    .padding(.vertical)
                }
            }
            .background(AppTheme.adaptiveBackground(colorScheme))
            .navigationTitle("")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Text("GoSteady")
                        .font(AppTypography.headlineSmall)
                        .foregroundStyle(AppTheme.primary)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink(value: AppRoute.profile) {
                        Image(systemName: AppTheme.SportIcon.profile)
                            .foregroundStyle(AppTheme.primary)
                    }
                }
            }
            .sheet(isPresented: $showManualEntry) {
                ManualWorkoutSheet(sportType: selectedSportForManual)
            }
            .refreshable {
                await viewModel.load(modelContext: modelContext)
            }
            .navigationDestination(for: AppRoute.self) { route in
                switch route {
                case .profile:
                    ProfileScreen()
                case .settings:
                    SettingsScreen()
                case .achievements:
                    AchievementsScreen()
                case .analytics:
                    AnalyticsDashboardScreen()
                case .runningDashboard:
                    RunningDashboardScreen()
                case .gymDashboard:
                    GymDashboardScreen()
                case .swimmingDashboard:
                    SwimmingDashboardScreen()
                case .cyclingDashboard:
                    CyclingDashboardScreen()
                default:
                    Text("Coming soon")
                        .navigationTitle("GoSteady")
                }
            }
        }
        .task {
            await viewModel.load(modelContext: modelContext)
        }
    }

    // MARK: - Greeting

    private var greetingSection: some View {
        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            Text(viewModel.greeting)
                .font(AppTypography.bodyMedium)
                .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
            Text(viewModel.userName)
                .font(AppTypography.headlineLarge)
                .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal)
    }

    // MARK: - Readiness Score

    private var readinessSection: some View {
        VStack(spacing: AppSpacing.lg) {
            HStack(spacing: AppSpacing.xl) {
                // Gauge
                ZStack {
                    Circle()
                        .stroke(AppTheme.adaptiveSurfaceVariant(colorScheme), lineWidth: 10)
                        .frame(width: 100, height: 100)
                    Circle()
                        .trim(from: 0, to: Double(viewModel.readinessScore) / 100.0)
                        .stroke(readinessColor, style: StrokeStyle(lineWidth: 10, lineCap: .round))
                        .frame(width: 100, height: 100)
                        .rotationEffect(.degrees(-90))
                        .animation(.easeOut(duration: 1), value: viewModel.readinessScore)
                    VStack(spacing: 2) {
                        Text("\(viewModel.readinessScore)")
                            .font(AppTypography.statMedium)
                            .foregroundStyle(readinessColor)
                        Text(viewModel.readinessLabel)
                            .font(AppTypography.captionSmall)
                            .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                    }
                }

                // Stats
                VStack(alignment: .leading, spacing: AppSpacing.md) {
                    ReadinessRow(icon: AppTheme.SportIcon.sleep, label: "Sleep",
                                 value: viewModel.sleepHours.map { String(format: "%.1fh", $0) } ?? "No data",
                                 valueColor: sleepColor)
                    ReadinessRow(icon: "heart.fill", label: "RHR",
                                 value: viewModel.restingHeartRate.map { "\($0) bpm" } ?? "No data",
                                 valueColor: AppTheme.adaptiveOnSurface(colorScheme))
                    ReadinessRow(icon: "waveform.path.ecg", label: "HRV",
                                 value: viewModel.hrv.map { String(format: "%.0f ms", $0) } ?? "No data",
                                 valueColor: AppTheme.adaptiveOnSurface(colorScheme))
                }
            }
        }
        .padding(AppSpacing.xl)
        .background(AppTheme.adaptiveSurface(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.large))
        .padding(.horizontal)
    }

    private var readinessColor: Color {
        switch viewModel.readinessScore {
        case 80...: return AppTheme.success
        case 60..<80: return AppTheme.primary
        case 40..<60: return AppTheme.warning
        default: return AppTheme.error
        }
    }

    private var sleepColor: Color {
        guard let sleep = viewModel.sleepHours else { return AppTheme.onSurfaceVariant }
        switch sleep {
        case 7...: return AppTheme.success
        case 5.5..<7: return AppTheme.warning
        default: return AppTheme.error
        }
    }

    // MARK: - Streak Banner

    private var streakBanner: some View {
        HStack(spacing: AppSpacing.md) {
            Image(systemName: AppTheme.SportIcon.fire)
                .font(.title2)
                .foregroundStyle(AppTheme.secondary)
            VStack(alignment: .leading, spacing: 2) {
                Text("\(viewModel.activeStreak) Day Streak")
                    .font(AppTypography.titleMedium)
                    .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
                Text("Keep it going!")
                    .font(AppTypography.captionLarge)
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
            }
            Spacer()
            Text("\(viewModel.activeStreak)")
                .font(AppTypography.statMedium)
                .foregroundStyle(AppTheme.secondary)
        }
        .padding(AppSpacing.lg)
        .background(
            LinearGradient(
                colors: [AppTheme.secondary.opacity(0.15), AppTheme.secondary.opacity(0.05)],
                startPoint: .leading,
                endPoint: .trailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.medium))
        .padding(.horizontal)
    }

    // MARK: - Upcoming Workout

    private func upcomingWorkoutCard(_ workout: UpcomingWorkoutData) -> some View {
        VStack(alignment: .leading, spacing: AppSpacing.md) {
            HStack {
                Text("TODAY'S PLAN")
                    .sectionHeaderStyle()
                Spacer()
            }

            HStack(spacing: AppSpacing.md) {
                ZStack {
                    Circle()
                        .fill(AppTheme.accent.opacity(0.15))
                        .frame(width: 48, height: 48)
                    Image(systemName: AppTheme.SportIcon.running)
                        .foregroundStyle(AppTheme.accent)
                }

                VStack(alignment: .leading, spacing: AppSpacing.xxs) {
                    Text(workout.workoutType)
                        .font(AppTypography.titleMedium)
                        .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
                    Text(workout.description)
                        .font(AppTypography.bodySmall)
                        .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                        .lineLimit(2)
                }

                Spacer()

                if let distance = workout.targetDistance {
                    Text(distance)
                        .font(AppTypography.labelMedium)
                        .foregroundStyle(AppTheme.accent)
                        .padding(.horizontal, AppSpacing.md)
                        .padding(.vertical, AppSpacing.xs)
                        .background(AppTheme.accent.opacity(0.1))
                        .clipShape(Capsule())
                }
            }

            Button {
                // Start workout
            } label: {
                Text("Start Workout")
                    .font(AppTypography.labelLarge)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, AppSpacing.md)
                    .background(AppTheme.primary)
                    .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.medium))
            }
        }
        .padding(AppSpacing.lg)
        .background(AppTheme.adaptiveSurface(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.large))
        .padding(.horizontal)
    }

    // MARK: - Weekly Activity Grid

    private var weeklyActivitySection: some View {
        VStack(alignment: .leading, spacing: AppSpacing.md) {
            HStack {
                Text("THIS WEEK")
                    .sectionHeaderStyle()
                Spacer()
                Text("\(viewModel.weeklyWorkoutCount) workouts")
                    .font(AppTypography.captionLarge)
                    .foregroundStyle(AppTheme.primary)
            }
            .padding(.horizontal)

            // 7-day grid
            HStack(spacing: AppSpacing.sm) {
                ForEach(viewModel.weeklyActivity) { day in
                    VStack(spacing: AppSpacing.xs) {
                        Text(day.day)
                            .font(AppTypography.captionSmall)
                            .foregroundStyle(day.isToday ? AppTheme.primary : AppTheme.adaptiveOnSurfaceVariant(colorScheme))

                        ZStack {
                            Circle()
                                .fill(day.workoutCount > 0 ? AppTheme.primary : AppTheme.adaptiveSurfaceVariant(colorScheme))
                                .frame(width: 36, height: 36)
                            if day.workoutCount > 0 {
                                Image(systemName: "checkmark")
                                    .font(.caption2.bold())
                                    .foregroundStyle(.white)
                            }
                        }

                        if day.workoutCount > 1 {
                            Text("\(day.workoutCount)")
                                .font(AppTypography.captionSmall)
                                .foregroundStyle(AppTheme.primary)
                        } else {
                            Text(" ")
                                .font(AppTypography.captionSmall)
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .padding(.horizontal)

            // Weekly stats row
            HStack(spacing: AppSpacing.sm) {
                StatCard(
                    icon: AppTheme.SportIcon.distance,
                    value: String(format: "%.1f km", viewModel.weeklyDistanceKm),
                    label: "Distance",
                    color: AppTheme.running,
                    compact: true
                )
                StatCard(
                    icon: AppTheme.SportIcon.timer,
                    value: viewModel.weeklyDurationFormatted,
                    label: "Time",
                    color: AppTheme.accent,
                    compact: true
                )
                StatCard(
                    icon: AppTheme.SportIcon.fire,
                    value: "\(Int(viewModel.todayCalories))",
                    label: "Calories",
                    color: AppTheme.secondary,
                    compact: true
                )
            }
            .padding(.horizontal)
        }
    }

    // MARK: - Quick Start

    private var quickStartSection: some View {
        VStack(alignment: .leading, spacing: AppSpacing.md) {
            Text("QUICK START")
                .sectionHeaderStyle()
                .padding(.horizontal)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: AppSpacing.md) {
                    QuickStartPill(icon: AppTheme.SportIcon.running, title: "Run", color: AppTheme.running) {}
                    QuickStartPill(icon: AppTheme.SportIcon.gym, title: "Gym", color: AppTheme.gym) {}
                    QuickStartPill(icon: AppTheme.SportIcon.swimming, title: "Swim", color: AppTheme.swimming) {}
                    QuickStartPill(icon: AppTheme.SportIcon.cycling, title: "Cycle", color: AppTheme.cycling) {}
                    QuickStartPill(icon: AppTheme.SportIcon.hiit, title: "HIIT", color: AppTheme.hiit) {}

                    Button {
                        showManualEntry = true
                    } label: {
                        HStack(spacing: AppSpacing.sm) {
                            Image(systemName: "plus.circle.fill")
                            Text("Log")
                        }
                        .font(AppTypography.labelMedium)
                        .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
                        .padding(.horizontal, AppSpacing.lg)
                        .padding(.vertical, AppSpacing.sm + 2)
                        .background(AppTheme.adaptiveSurfaceVariant(colorScheme))
                        .clipShape(Capsule())
                    }
                }
                .padding(.horizontal)
            }
        }
    }

    // MARK: - Recent Workouts

    private var recentWorkoutsSection: some View {
        VStack(alignment: .leading, spacing: AppSpacing.md) {
            HStack {
                Text("RECENT")
                    .sectionHeaderStyle()
                Spacer()
                Button("See All") {}
                    .font(AppTypography.labelMedium)
                    .foregroundStyle(AppTheme.primary)
            }
            .padding(.horizontal)

            if viewModel.recentWorkouts.isEmpty {
                EmptyStateView(
                    icon: "figure.mixed.cardio",
                    title: "No workouts yet",
                    message: "Start your first workout to track your progress"
                )
                .padding(.horizontal)
            } else {
                VStack(spacing: AppSpacing.sm) {
                    ForEach(viewModel.recentWorkouts) { workout in
                        WorkoutSummaryCard(
                            sportIcon: workout.sportIcon,
                            sportColor: workout.sportColor,
                            title: workout.title,
                            duration: workout.duration,
                            keyMetric: workout.keyMetric,
                            date: workout.date
                        )
                    }
                }
                .padding(.horizontal)
            }
        }
    }
}

// MARK: - Supporting Views

private struct ReadinessRow: View {
    let icon: String
    let label: String
    let value: String
    let valueColor: Color
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(spacing: AppSpacing.sm) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                .frame(width: 20)
            Text(label)
                .font(AppTypography.captionLarge)
                .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
            Spacer()
            Text(value)
                .font(AppTypography.labelMedium)
                .foregroundStyle(valueColor)
        }
    }
}

private struct QuickStartPill: View {
    let icon: String
    let title: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: AppSpacing.sm) {
                Image(systemName: icon)
                    .font(.subheadline)
                    .foregroundStyle(color)
                Text(title)
                    .font(AppTypography.labelMedium)
                    .foregroundStyle(color)
            }
            .padding(.horizontal, AppSpacing.lg)
            .padding(.vertical, AppSpacing.sm + 2)
            .background(color.opacity(0.12))
            .clipShape(Capsule())
        }
    }
}
