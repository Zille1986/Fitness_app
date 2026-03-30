import SwiftUI
import SwiftData

struct ActivityHubScreen: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.colorScheme) private var colorScheme
    @Query(sort: \Run.startTime, order: .reverse) private var recentRuns: [Run]
    @Query(sort: \GymWorkout.startTime, order: .reverse) private var recentGymWorkouts: [GymWorkout]
    @State private var showManualEntry = false
    @State private var selectedSportForManual: SportType = .run

    private let sports: [SportItem] = [
        SportItem(name: "Running", icon: AppTheme.SportIcon.running, color: AppTheme.running, route: .runningDashboard),
        SportItem(name: "Gym", icon: AppTheme.SportIcon.gym, color: AppTheme.gym, route: .gymDashboard),
        SportItem(name: "Swimming", icon: AppTheme.SportIcon.swimming, color: AppTheme.swimming, route: .swimmingDashboard),
        SportItem(name: "Cycling", icon: AppTheme.SportIcon.cycling, color: AppTheme.cycling, route: .cyclingDashboard),
        SportItem(name: "HIIT", icon: AppTheme.SportIcon.hiit, color: AppTheme.hiit, route: .hiitDashboard)
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: AppSpacing.xxl) {
                    // Sport Selection Grid
                    sportGrid

                    // Quick Actions
                    quickActionsSection

                    // Recent Activity Summary
                    recentActivitySection
                }
                .padding(.vertical)
            }
            .background(AppTheme.adaptiveBackground(colorScheme))
            .navigationTitle("Activity")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showManualEntry = true
                    } label: {
                        Image(systemName: "plus.circle.fill")
                            .foregroundStyle(AppTheme.primary)
                    }
                }
            }
            .sheet(isPresented: $showManualEntry) {
                ManualWorkoutSheet(sportType: selectedSportForManual)
            }
            .navigationDestination(for: AppRoute.self) { route in
                routeDestination(for: route)
            }
        }
    }

    // MARK: - Sport Grid

    private var sportGrid: some View {
        VStack(alignment: .leading, spacing: AppSpacing.md) {
            Text("CHOOSE YOUR SPORT")
                .sectionHeaderStyle()
                .padding(.horizontal)

            LazyVGrid(
                columns: [
                    GridItem(.flexible(), spacing: AppSpacing.md),
                    GridItem(.flexible(), spacing: AppSpacing.md)
                ],
                spacing: AppSpacing.md
            ) {
                ForEach(sports) { sport in
                    NavigationLink(value: sport.route) {
                        SportCard(sport: sport)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal)
        }
    }

    // MARK: - Quick Actions

    private var quickActionsSection: some View {
        VStack(alignment: .leading, spacing: AppSpacing.md) {
            Text("QUICK START")
                .sectionHeaderStyle()
                .padding(.horizontal)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: AppSpacing.md) {
                    QuickStartButton(title: "Free Run", icon: AppTheme.SportIcon.running, color: AppTheme.running) {
                        // Navigate to active run
                    }
                    QuickStartButton(title: "Quick Gym", icon: AppTheme.SportIcon.gym, color: AppTheme.gym) {
                        // Navigate to quick gym workout
                    }
                    QuickStartButton(title: "Pool Swim", icon: AppTheme.SportIcon.swimming, color: AppTheme.swimming) {
                        // Navigate to active swim
                    }
                    QuickStartButton(title: "Outdoor Ride", icon: AppTheme.SportIcon.cycling, color: AppTheme.cycling) {
                        // Navigate to active cycling
                    }
                    QuickStartButton(title: "Tabata", icon: AppTheme.SportIcon.hiit, color: AppTheme.hiit) {
                        // Navigate to HIIT
                    }
                }
                .padding(.horizontal)
            }
        }
    }

    // MARK: - Recent Activity

    private var recentActivitySection: some View {
        VStack(alignment: .leading, spacing: AppSpacing.md) {
            HStack {
                Text("RECENT ACTIVITY")
                    .sectionHeaderStyle()
                Spacer()
                Text("\(totalActivitiesThisWeek) this week")
                    .font(AppTypography.captionLarge)
                    .foregroundStyle(AppTheme.primary)
            }
            .padding(.horizontal)

            if recentRuns.isEmpty && recentGymWorkouts.isEmpty {
                EmptyStateView(
                    icon: "figure.mixed.cardio",
                    title: "No recent activity",
                    message: "Start a workout to see your activity here",
                    actionTitle: "Start Workout"
                ) {
                    // Action
                }
                .padding(.horizontal)
            } else {
                VStack(spacing: AppSpacing.sm) {
                    ForEach(recentRuns.prefix(3)) { run in
                        WorkoutSummaryCard(
                            sportIcon: AppTheme.SportIcon.running,
                            sportColor: AppTheme.running,
                            title: String(format: "%.2f km Run", run.distanceKm),
                            duration: run.durationFormatted,
                            keyMetric: "\(run.avgPaceFormatted)/km",
                            date: run.startTime
                        )
                    }
                    ForEach(recentGymWorkouts.prefix(2)) { workout in
                        WorkoutSummaryCard(
                            sportIcon: AppTheme.SportIcon.gym,
                            sportColor: AppTheme.gym,
                            title: workout.name,
                            duration: workout.durationFormatted,
                            keyMetric: "\(workout.exercises.count) exercises",
                            date: workout.startTime
                        )
                    }
                }
                .padding(.horizontal)
            }
        }
    }

    private var totalActivitiesThisWeek: Int {
        let startOfWeek = Calendar.current.dateInterval(of: .weekOfYear, for: Date())?.start ?? Date()
        let runCount = recentRuns.filter { $0.startTime >= startOfWeek }.count
        let gymCount = recentGymWorkouts.filter { $0.startTime >= startOfWeek }.count
        return runCount + gymCount
    }

    @ViewBuilder
    @ViewBuilder
    private func routeDestination(for route: AppRoute) -> some View {
        switch route {
        case .runningDashboard:
            RunningDashboardScreen()
        case .gymDashboard:
            GymDashboardScreen()
        case .swimmingDashboard:
            SwimmingDashboardScreen()
        case .cyclingDashboard:
            CyclingDashboardScreen()
        case .hiitDashboard:
            HIITDashboardScreen()
        case .profile:
            ProfileScreen()
        case .settings:
            SettingsScreen()
        case .achievements:
            AchievementsScreen()
        case .analytics:
            AnalyticsDashboardScreen()
        default:
            Text("Coming soon")
                .navigationTitle("GoSteady")
        }
    }
}

// MARK: - Supporting Types

struct SportItem: Identifiable {
    let id = UUID()
    let name: String
    let icon: String
    let color: Color
    let route: AppRoute
}

// MARK: - Sport Card

struct SportCard: View {
    let sport: SportItem
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(spacing: AppSpacing.md) {
            ZStack {
                Circle()
                    .fill(sport.color.opacity(0.15))
                    .frame(width: 56, height: 56)
                Image(systemName: sport.icon)
                    .font(.title2)
                    .foregroundStyle(sport.color)
            }

            Text(sport.name)
                .font(AppTypography.titleSmall)
                .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, AppSpacing.xl)
        .background(AppTheme.adaptiveSurface(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.large))
        .overlay(
            RoundedRectangle(cornerRadius: AppCornerRadius.large)
                .stroke(sport.color.opacity(0.2), lineWidth: 1)
        )
    }
}

// MARK: - Quick Start Button

struct QuickStartButton: View {
    let title: String
    let icon: String
    let color: Color
    let action: () -> Void
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Button(action: action) {
            HStack(spacing: AppSpacing.sm) {
                Image(systemName: icon)
                    .font(.subheadline)
                    .foregroundStyle(color)
                Text(title)
                    .font(AppTypography.labelMedium)
                    .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
            }
            .padding(.horizontal, AppSpacing.lg)
            .padding(.vertical, AppSpacing.md)
            .background(color.opacity(0.1))
            .clipShape(Capsule())
            .overlay(
                Capsule()
                    .stroke(color.opacity(0.3), lineWidth: 1)
            )
        }
    }
}
