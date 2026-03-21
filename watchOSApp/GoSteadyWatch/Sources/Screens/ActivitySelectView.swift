import SwiftUI

struct ActivitySelectView: View {
    @EnvironmentObject var workoutManager: WorkoutManager

    var body: some View {
        List {
            Section {
                NavigationLink {
                    RunStartView()
                        .environmentObject(workoutManager)
                } label: {
                    ActivityRow(activity: .running)
                }

                NavigationLink {
                    SwimStartView()
                        .environmentObject(workoutManager)
                } label: {
                    ActivityRow(activity: .swimming)
                }

                NavigationLink {
                    CyclingStartView()
                        .environmentObject(workoutManager)
                } label: {
                    ActivityRow(activity: .cycling)
                }

                Button {
                    workoutManager.requestAuthorization()
                    workoutManager.startWorkout(activity: .gym)
                } label: {
                    ActivityRow(activity: .gym)
                }

                Button {
                    workoutManager.requestAuthorization()
                    workoutManager.startWorkout(activity: .hiit)
                } label: {
                    ActivityRow(activity: .hiit)
                }
            }
        }
        .navigationTitle("GoSteady")
        .onAppear {
            workoutManager.requestAuthorization()
        }
    }
}

struct ActivityRow: View {
    let activity: ActivityType

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: activity.icon)
                .font(.title3)
                .foregroundStyle(WatchColors.activityColor(activity))
                .frame(width: 30)
            Text(activity.rawValue)
                .fontWeight(.semibold)
        }
    }
}

// MARK: - Run Start

struct RunStartView: View {
    @EnvironmentObject var workoutManager: WorkoutManager
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        List {
            Button {
                workoutManager.startWorkout(activity: .running)
                dismiss()
            } label: {
                VStack(alignment: .leading) {
                    Text("Free Run").fontWeight(.bold)
                    Text("Track distance, pace & HR")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            .listItemTint(WatchColors.primary)

            Section("Quick Workouts") {
                ForEach(["Tempo Run", "Interval 400m", "Long Run", "Recovery"], id: \.self) { name in
                    Button {
                        workoutManager.startWorkout(activity: .running)
                        dismiss()
                    } label: {
                        Text(name).fontWeight(.medium)
                    }
                }
            }
        }
        .navigationTitle("Run")
    }
}

// MARK: - Swim Start

struct SwimStartView: View {
    @EnvironmentObject var workoutManager: WorkoutManager
    @Environment(\.dismiss) private var dismiss
    @State private var selectedPool: PoolLength = .m25

    var body: some View {
        List {
            Section {
                Picker("Pool Length", selection: $selectedPool) {
                    ForEach(PoolLength.allCases) { length in
                        Text(length.label).tag(length)
                    }
                }
            }

            Button {
                workoutManager.startWorkout(activity: .swimming, swimType: .pool, poolLength: selectedPool)
                dismiss()
            } label: {
                VStack(alignment: .leading) {
                    Text("Pool Swim").fontWeight(.bold)
                    Text("Auto lap detection \u{00B7} \(selectedPool.label)")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            .listItemTint(WatchColors.swimming)

            Button {
                workoutManager.startWorkout(activity: .swimming, swimType: .openWater)
                dismiss()
            } label: {
                VStack(alignment: .leading) {
                    Text("Open Water").fontWeight(.bold)
                    Text("GPS tracking")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .navigationTitle("Swim")
    }
}

// MARK: - Cycling Start

struct CyclingStartView: View {
    @EnvironmentObject var workoutManager: WorkoutManager
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        List {
            ForEach(CyclingType.allCases) { type in
                Button {
                    workoutManager.startWorkout(activity: .cycling, cyclingType: type)
                    dismiss()
                } label: {
                    VStack(alignment: .leading) {
                        Text(type.rawValue).fontWeight(.bold)
                        Text(type == .outdoor ? "GPS route tracking" : "Indoor distance")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
        .navigationTitle("Cycle")
    }
}
