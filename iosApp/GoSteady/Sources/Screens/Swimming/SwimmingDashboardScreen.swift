import SwiftUI

struct SwimmingDashboardScreen: View {
    @State private var viewModel = SwimmingViewModel()
    @State private var showManualEntry = false
    @State private var showSwimTypeSheet = false
    @State private var showPoolLengthSheet = false
    @State private var selectedSwimType: SwimType = .pool
    @State private var navigateToActiveSwim = false
    @State private var selectedPoolLength: PoolLength = .shortCourseMeters

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                ScrollView {
                    VStack(spacing: 16) {
                        // Hero banner
                        DashboardHeroBanner(
                            icon: "figure.pool.swim",
                            title: "Swimming",
                            subtitle: "Pool and open water sessions"
                        )

                        // Filter toggle
                        filterToggle

                        if viewModel.isLoading {
                            loadingView
                        } else if let error = viewModel.errorMessage {
                            errorView(error)
                        } else {
                            // Weekly stats
                            if let stats = viewModel.weeklyStats {
                                weeklyStatsCard(stats)
                            }

                            // Active training plan
                            if let plan = viewModel.activePlan {
                                trainingPlanCard(plan)
                            }

                            // Stroke breakdown
                            if !viewModel.strokeBreakdown.isEmpty {
                                strokeBreakdownCard
                            }

                            // Quick actions
                            quickActionsRow

                            // Recent swims
                            recentSwimsSection
                        }
                    }
                    .padding(.vertical)
                    .padding(.bottom, 80)
                }
                .background(AppTheme.surface)
                .refreshable {
                    viewModel.loadData()
                }

                // FAB
                startSwimButton
            }
            .navigationTitle("Swimming")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showManualEntry = true } label: {
                        Image(systemName: "square.and.pencil")
                    }
                }
            }
            .sheet(isPresented: $showManualEntry) {
                ManualWorkoutSheet(sportType: .swim)
            }
            .confirmationDialog("Swim Type", isPresented: $showSwimTypeSheet) {
                ForEach(SwimType.allCases) { swimType in
                    Button(swimType.displayName) {
                        selectedSwimType = swimType
                        if swimType == .pool {
                            showPoolLengthSheet = true
                        } else {
                            navigateToActiveSwim = true
                        }
                    }
                }
                Button("Cancel", role: .cancel) {}
            }
            .confirmationDialog("Pool Length", isPresented: $showPoolLengthSheet) {
                ForEach(PoolLength.allCases) { length in
                    Button(length.displayName) {
                        selectedPoolLength = length
                        navigateToActiveSwim = true
                    }
                }
                Button("Cancel", role: .cancel) {}
            }
            .navigationDestination(isPresented: $navigateToActiveSwim) {
                ActiveSwimScreen(
                    swimType: selectedSwimType,
                    poolLength: selectedSwimType == .pool ? selectedPoolLength : nil
                )
            }
        }
    }

    // MARK: - Filter Toggle

    private var filterToggle: some View {
        Picker("Filter", selection: $viewModel.selectedFilter) {
            ForEach(SwimmingViewModel.SwimFilterType.allCases, id: \.self) { filter in
                Text(filter.rawValue).tag(filter)
            }
        }
        .pickerStyle(.segmented)
        .padding(.horizontal)
    }

    // MARK: - Weekly Stats

    private func weeklyStatsCard(_ stats: SwimmingWeeklyStats) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("THIS WEEK")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.swimming)

            HStack(spacing: 0) {
                statItem(value: String(format: "%.1f", stats.totalDistanceKm), unit: "km", label: "Distance")
                Spacer()
                statItem(value: "\(stats.workoutCount)", unit: "", label: "Swims")
                Spacer()
                statItem(value: SwimmingViewModel.formatPace(stats.averagePace), unit: "", label: "Avg Pace")
            }
        }
        .padding(16)
        .background(AppTheme.swimming.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(AppTheme.swimming.opacity(0.2), lineWidth: 1)
        )
        .padding(.horizontal)
    }

    // MARK: - Training Plan Card

    private func trainingPlanCard(_ plan: SwimmingTrainingPlanSummary) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "calendar")
                    .foregroundStyle(AppTheme.swimming)
                Text("ACTIVE PLAN")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundStyle(AppTheme.swimming)
            }
            Text(plan.name)
                .font(.headline)
                .fontWeight(.bold)
            Text(plan.description)
                .font(.caption)
                .foregroundStyle(.secondary)
            ProgressView(value: Double(plan.weekProgress), total: Double(plan.totalWeeks))
                .tint(AppTheme.swimming)
            Text("Week \(plan.weekProgress) of \(plan.totalWeeks)")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - Stroke Breakdown

    private var strokeBreakdownCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("STROKE BREAKDOWN")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.onSurfaceVariant)

            ForEach(viewModel.strokeBreakdown) { item in
                HStack {
                    Text(item.strokeType.displayName)
                        .font(.subheadline)
                    Spacer()
                    GeometryReader { geometry in
                        RoundedRectangle(cornerRadius: 4)
                            .fill(AppTheme.swimming.opacity(0.6))
                            .frame(width: geometry.size.width * item.percentage)
                    }
                    .frame(width: 100, height: 8)
                    Text(String(format: "%.0f%%", item.percentage * 100))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .frame(width: 40, alignment: .trailing)
                }
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - Quick Actions

    private var quickActionsRow: some View {
        HStack(spacing: 12) {
            quickActionCard(icon: "clock.arrow.circlepath", title: "History", color: AppTheme.swimming) {
                // Navigate to full history
            }
            quickActionCard(icon: "calendar", title: "Plans", color: AppTheme.swimming) {
                // Navigate to training plans
            }
        }
        .padding(.horizontal)
    }

    private func quickActionCard(icon: String, title: String, color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.title3)
                    .foregroundStyle(color)
                Text(title)
                    .font(.caption)
                    .foregroundStyle(AppTheme.onSurface)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(AppTheme.surfaceContainerLow)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }

    // MARK: - Recent Swims

    private var recentSwimsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("RECENT SWIMS")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.onSurfaceVariant)
                .padding(.horizontal)

            if viewModel.filteredWorkouts.isEmpty {
                EmptyStateView(
                    icon: "figure.pool.swim",
                    title: "No swims yet",
                    message: "Start your first swim to track your progress"
                )
                .padding(.horizontal)
            } else {
                ForEach(viewModel.filteredWorkouts) { workout in
                    NavigationLink {
                        SwimDetailScreen(workout: workout)
                    } label: {
                        recentSwimCard(workout)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func recentSwimCard(_ workout: SwimmingWorkoutSummary) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Image(systemName: workout.swimType.icon)
                        .font(.caption)
                        .foregroundStyle(AppTheme.swimming)
                    Text(workout.swimType.displayName)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                }
                Text(workout.dateFormatted)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text(workout.distanceFormatted)
                    .font(.subheadline)
                    .fontWeight(.bold)
                HStack(spacing: 8) {
                    Text(workout.durationFormatted)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(workout.avgPaceFormatted + "/100m")
                        .font(.caption)
                        .foregroundStyle(AppTheme.swimming)
                }
            }
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
        .padding(12)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
    }

    // MARK: - Start Swim FAB

    private var startSwimButton: some View {
        Button {
            showSwimTypeSheet = true
        } label: {
            Label("Start Swim", systemImage: "figure.pool.swim")
                .fontWeight(.bold)
                .padding(.horizontal, 20)
                .padding(.vertical, 14)
                .background(AppTheme.swimming)
                .foregroundStyle(.white)
                .clipShape(Capsule())
                .shadow(color: AppTheme.swimming.opacity(0.3), radius: 12, y: 6)
        }
        .padding(24)
    }

    // MARK: - Loading/Error Views

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .tint(AppTheme.swimming)
            Text("Loading swim data...")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(48)
    }

    private func errorView(_ message: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundStyle(AppTheme.error)
            Text("Something went wrong")
                .fontWeight(.semibold)
            Text(message)
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("Retry") {
                viewModel.loadData()
            }
            .buttonStyle(.borderedProminent)
            .tint(AppTheme.swimming)
        }
        .frame(maxWidth: .infinity)
        .padding(32)
    }

    // MARK: - Stat Item

    private func statItem(value: String, unit: String, label: String) -> some View {
        VStack(spacing: 2) {
            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text(value)
                    .font(.title2)
                    .fontWeight(.bold)
                if !unit.isEmpty {
                    Text(unit)
                        .font(.caption)
                }
            }
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }
}
