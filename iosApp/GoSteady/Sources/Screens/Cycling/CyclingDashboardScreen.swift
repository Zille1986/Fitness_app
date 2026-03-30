import SwiftUI
import SwiftData

struct CyclingDashboardScreen: View {
    @Environment(\.modelContext) private var modelContext
    @State private var viewModel = CyclingViewModel()
    @State private var showManualEntry = false
    @State private var showCyclingTypeSheet = false
    @State private var navigateToActiveCycle = false
    @State private var selectedCyclingType: CyclingType = .outdoor

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                ScrollView {
                    VStack(spacing: 16) {
                        // Hero banner
                        DashboardHeroBanner(
                            icon: "bicycle",
                            title: "Cycling",
                            subtitle: "Outdoor rides and indoor training"
                        )

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

                            // Quick actions
                            quickActionsRow

                            // Recent rides
                            recentRidesSection
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
                startRideButton
            }
            .navigationTitle("Cycling")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showManualEntry = true } label: {
                        Image(systemName: "square.and.pencil")
                    }
                }
            }
            .sheet(isPresented: $showManualEntry) {
                ManualWorkoutSheet(sportType: .bike)
            }
            .confirmationDialog("Ride Type", isPresented: $showCyclingTypeSheet) {
                ForEach(CyclingType.allCases) { type in
                    Button(type.displayName) {
                        selectedCyclingType = type
                        navigateToActiveCycle = true
                    }
                }
                Button("Cancel", role: .cancel) {}
            }
            .navigationDestination(isPresented: $navigateToActiveCycle) {
                ActiveCycleScreen(cyclingType: selectedCyclingType)
            }
            .onAppear {
                viewModel.setRepository(CyclingRepository(context: modelContext))
            }
        }
    }

    // MARK: - Weekly Stats

    private func weeklyStatsCard(_ stats: CyclingWeeklyStats) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("THIS WEEK")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.cycling)

            HStack(spacing: 0) {
                statItem(value: String(format: "%.1f", stats.totalDistanceKm), unit: "km", label: "Distance")
                Spacer()
                statItem(value: String(format: "%.0f", stats.averagePowerWatts), unit: "W", label: "Avg Power")
                Spacer()
                statItem(value: "\(stats.workoutCount)", unit: "", label: "Rides")
            }
        }
        .padding(16)
        .background(AppTheme.cycling.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(AppTheme.cycling.opacity(0.2), lineWidth: 1)
        )
        .padding(.horizontal)
    }

    // MARK: - Training Plan

    private func trainingPlanCard(_ plan: CyclingTrainingPlanSummary) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "calendar")
                    .foregroundStyle(AppTheme.cycling)
                Text("ACTIVE PLAN")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundStyle(AppTheme.cycling)
            }
            Text(plan.name)
                .font(.headline)
                .fontWeight(.bold)
            Text(plan.description)
                .font(.caption)
                .foregroundStyle(.secondary)
            ProgressView(value: Double(plan.weekProgress), total: Double(plan.totalWeeks))
                .tint(AppTheme.cycling)
            Text("Week \(plan.weekProgress) of \(plan.totalWeeks)")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - Quick Actions

    private var quickActionsRow: some View {
        HStack(spacing: 12) {
            quickActionCard(icon: "clock.arrow.circlepath", title: "History", color: AppTheme.cycling) {}
            quickActionCard(icon: "calendar", title: "Plans", color: AppTheme.cycling) {}
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

    // MARK: - Recent Rides

    private var recentRidesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("RECENT RIDES")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.onSurfaceVariant)
                .padding(.horizontal)

            if viewModel.recentWorkouts.isEmpty {
                EmptyStateView(
                    icon: "bicycle",
                    title: "No rides yet",
                    message: "Start your first ride to track your progress"
                )
                .padding(.horizontal)
            } else {
                ForEach(viewModel.recentWorkouts) { workout in
                    NavigationLink {
                        CycleDetailScreen(workout: workout)
                    } label: {
                        recentRideCard(workout)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func recentRideCard(_ workout: CyclingWorkoutSummary) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Image(systemName: workout.cyclingType.icon)
                        .font(.caption)
                        .foregroundStyle(AppTheme.cycling)
                    Text(workout.cyclingType.displayName)
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
                    Text(workout.avgSpeedFormatted)
                        .font(.caption)
                        .foregroundStyle(AppTheme.cycling)
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

    // MARK: - Start Ride FAB

    private var startRideButton: some View {
        Button {
            showCyclingTypeSheet = true
        } label: {
            Label("Start Ride", systemImage: "bicycle")
                .fontWeight(.bold)
                .padding(.horizontal, 20)
                .padding(.vertical, 14)
                .background(AppTheme.cycling)
                .foregroundStyle(.black)
                .clipShape(Capsule())
                .shadow(color: AppTheme.cycling.opacity(0.3), radius: 12, y: 6)
        }
        .padding(24)
    }

    // MARK: - Helper Views

    private func statItem(value: String, unit: String, label: String) -> some View {
        VStack(spacing: 2) {
            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text(value)
                    .font(.title3)
                    .fontWeight(.bold)
                if !unit.isEmpty {
                    Text(unit)
                        .font(.caption2)
                }
            }
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .tint(AppTheme.cycling)
            Text("Loading ride data...")
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
            .tint(AppTheme.cycling)
        }
        .frame(maxWidth: .infinity)
        .padding(32)
    }
}
