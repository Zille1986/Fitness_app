import SwiftUI
import WatchKit

struct WorkoutSelectionScreen: View {

    @Environment(WatchWorkoutService.self) private var workoutService
    @Environment(WatchSettingsViewModel.self) private var settings

    var body: some View {
        List {
            Section("Running") {
                workoutButton(
                    icon: "figure.run",
                    title: "Outdoor Run",
                    subtitle: "GPS + Pace + HR",
                    color: WatchTheme.primary,
                    activity: .outdoorRun
                )
                workoutButton(
                    icon: "figure.run",
                    title: "Indoor Run",
                    subtitle: "Treadmill",
                    color: WatchTheme.primary,
                    activity: .indoorRun
                )
            }

            Section("Gym") {
                workoutButton(
                    icon: "dumbbell.fill",
                    title: "Gym",
                    subtitle: "Exercises + Sets + Rest timer",
                    color: WatchTheme.gym,
                    activity: .gym
                )
            }

            Section("Swimming") {
                NavigationLink {
                    PoolSwimSetupScreen()
                } label: {
                    WorkoutRow(icon: "figure.pool.swim", title: "Pool Swim",
                               subtitle: "Auto lap detection", color: WatchTheme.swimming)
                }
                workoutButton(
                    icon: "figure.open.water.swim",
                    title: "Open Water",
                    subtitle: "GPS tracking",
                    color: WatchTheme.swimming,
                    activity: .openWaterSwim
                )
            }

            Section("Cycling") {
                workoutButton(
                    icon: "bicycle",
                    title: "Outdoor Cycle",
                    subtitle: "GPS + Speed + HR",
                    color: WatchTheme.cycling,
                    activity: .outdoorCycle
                )
                workoutButton(
                    icon: "bicycle",
                    title: "Indoor Cycle",
                    subtitle: "Stationary + HR",
                    color: WatchTheme.cycling,
                    activity: .indoorCycle
                )
            }

            Section("Intensity") {
                NavigationLink {
                    HIITSelectionScreen()
                } label: {
                    WorkoutRow(icon: "flame.fill", title: "HIIT",
                               subtitle: "Interval training", color: WatchTheme.hiit)
                }
            }
        }
        .navigationTitle("Workouts")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func workoutButton(icon: String, title: String, subtitle: String, color: Color, activity: WatchActivityType) -> some View {
        Button {
            workoutService.startWorkout(activity: activity)
        } label: {
            WorkoutRow(icon: icon, title: title, subtitle: subtitle, color: color)
        }
    }
}

// MARK: - Workout Row

struct WorkoutRow: View {
    let icon: String
    let title: String
    let subtitle: String
    let color: Color

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .foregroundStyle(color)
                .font(.title3)
                .frame(width: 28)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .fontWeight(.semibold)
                    .font(.callout)
                Text(subtitle)
                    .font(.system(size: 10))
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Pool Swim Setup

struct PoolSwimSetupScreen: View {
    @Environment(WatchWorkoutService.self) private var workoutService
    @Environment(WatchSettingsViewModel.self) private var settings
    @Environment(\.dismiss) private var dismiss
    @State private var selectedPool: PoolLength = .m25

    var body: some View {
        List {
            Picker("Pool Length", selection: $selectedPool) {
                ForEach(PoolLength.allCases) { length in
                    Text(length.label).tag(length)
                }
            }

            Button {
                workoutService.startWorkout(activity: .poolSwim, pool: selectedPool)
                dismiss()
            } label: {
                HStack {
                    Spacer()
                    Label("Start Swim", systemImage: "play.fill")
                        .fontWeight(.bold)
                    Spacer()
                }
            }
            .listItemTint(WatchTheme.swimming)
        }
        .navigationTitle("Pool Swim")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { selectedPool = settings.poolLength }
    }
}

// MARK: - HIIT Selection

struct HIITSelectionScreen: View {
    @Environment(WatchWorkoutService.self) private var workoutService
    @Environment(WatchWorkoutViewModel.self) private var workoutVM

    var body: some View {
        List {
            ForEach(HIITTemplate.defaults) { template in
                Button {
                    workoutService.startWorkout(activity: .hiit)
                    workoutVM.startHIIT(template: template)
                } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(template.name)
                            .fontWeight(.bold)
                        Text("\(template.formattedDuration) - \(template.exercises.count) exercises - \(template.rounds)x")
                            .font(.system(size: 10))
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 4)
                }
                .listItemTint(WatchTheme.hiit.opacity(0.3))
            }
        }
        .navigationTitle("HIIT")
        .navigationBarTitleDisplayMode(.inline)
    }
}
