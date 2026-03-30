import SwiftUI
import WatchKit

struct WatchHomeScreen: View {

    @Environment(WatchHomeViewModel.self) private var vm
    @Environment(WatchHealthService.self) private var healthService

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                // Activity Rings
                WatchActivityRings(
                    move: vm.moveProgress,
                    exercise: vm.exerciseProgress,
                    stand: vm.standProgress
                )
                .frame(height: 60)

                // Today's Stats
                HStack(spacing: 16) {
                    VStack(spacing: 2) {
                        Text("\(vm.todaySteps)")
                            .font(.system(size: 18, weight: .bold))
                        Text("steps")
                            .font(.system(size: 10))
                            .foregroundStyle(.secondary)
                    }
                    VStack(spacing: 2) {
                        Text("\(Int(vm.todayCalories))")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundStyle(.orange)
                        Text("kcal")
                            .font(.system(size: 10))
                            .foregroundStyle(.secondary)
                    }
                }

                // Heart Rate
                if vm.currentHeartRate > 0 {
                    HStack(spacing: 4) {
                        Image(systemName: "heart.fill")
                            .foregroundStyle(.red)
                            .font(.caption2)
                        Text("\(vm.currentHeartRate)")
                            .font(.system(size: 16, weight: .semibold))
                        Text("bpm")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }

                Divider().padding(.horizontal)

                // Quick Start Workout
                NavigationLink(destination: WorkoutSelectionScreen()) {
                    HStack {
                        Image(systemName: "play.fill")
                            .foregroundStyle(WatchTheme.primary)
                        Text("Start Workout")
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(WatchTheme.primary.opacity(0.2))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)

                // Recent Workouts
                if !vm.recentWorkouts.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Recent")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                            .padding(.leading, 4)

                        ForEach(vm.recentWorkouts.prefix(3)) { workout in
                            HStack {
                                Image(systemName: workout.type.icon)
                                    .foregroundStyle(WatchTheme.activityColor(workout.type))
                                    .font(.caption)
                                    .frame(width: 20)
                                VStack(alignment: .leading, spacing: 1) {
                                    Text(workout.type.rawValue)
                                        .font(.caption)
                                        .fontWeight(.medium)
                                    Text(formatDuration(workout.duration))
                                        .font(.system(size: 10))
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                if workout.distance > 0 {
                                    Text(String(format: "%.1f km", workout.distance / 1000))
                                        .font(.system(size: 10))
                                        .foregroundStyle(.secondary)
                                }
                            }
                            .padding(.vertical, 4)
                            .padding(.horizontal, 8)
                            .background(Color.white.opacity(0.05))
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                    }
                }

                // Navigation links
                NavigationLink(destination: HeartRateScreen()) {
                    HStack {
                        Image(systemName: "heart.fill")
                            .foregroundStyle(.red)
                        Text("Heart Rate")
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 6)
                }
                .buttonStyle(.plain)

                NavigationLink(destination: WatchSettingsScreen()) {
                    HStack {
                        Image(systemName: "gear")
                            .foregroundStyle(.gray)
                        Text("Settings")
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 6)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 8)
        }
        .navigationTitle("GoSteady")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { vm.refresh() }
    }
}
