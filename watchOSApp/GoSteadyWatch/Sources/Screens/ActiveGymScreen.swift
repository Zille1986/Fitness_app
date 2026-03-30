import SwiftUI
import WatchKit

struct ActiveGymScreen: View {

    @Environment(WatchWorkoutService.self) private var ws
    @Environment(WatchGymViewModel.self) private var gym
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            // Page 1: Current Exercise
            exercisePage
                .tag(0)

            // Page 2: Exercise List
            exerciseListPage
                .tag(1)

            // Page 3: Controls
            WorkoutControlsView()
                .tag(2)
        }
        .tabViewStyle(.verticalPage)
        .navigationBarBackButtonHidden(true)
        .onAppear {
            if !gym.isWorkoutStarted {
                gym.startWorkout()
            }
        }
    }

    // MARK: - Exercise Page

    private var exercisePage: some View {
        VStack(spacing: 6) {
            if let exercise = gym.currentExercise {
                // Exercise name
                Text(exercise.name)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(WatchTheme.gym)
                    .lineLimit(2)
                    .multilineTextAlignment(.center)

                // Set counter
                Text("Set \(exercise.completedSets + 1) of \(exercise.targetSets)")
                    .font(.system(size: 14, weight: .semibold))

                // Weight & Reps
                HStack(spacing: 16) {
                    VStack(spacing: 2) {
                        Text(exercise.weight > 0 ? String(format: "%.1f", exercise.weight) : "--")
                            .font(.system(size: 20, weight: .bold))
                        Text(exercise.unit)
                            .font(.system(size: 9))
                            .foregroundStyle(.secondary)
                    }
                    VStack(spacing: 2) {
                        Text("\(exercise.targetReps)")
                            .font(.system(size: 20, weight: .bold))
                        Text("reps")
                            .font(.system(size: 9))
                            .foregroundStyle(.secondary)
                    }
                }

                // Rest Timer
                if gym.restTimerRunning {
                    VStack(spacing: 2) {
                        Text("REST")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(.blue)
                        Text("\(gym.restTimerSeconds)")
                            .font(.system(size: 32, weight: .bold, design: .monospaced))
                            .foregroundStyle(.blue)
                        Button("Skip") {
                            gym.skipRest()
                        }
                        .font(.caption2)
                        .tint(.blue)
                    }
                } else {
                    // Log Set Button
                    Button {
                        gym.logSet()
                    } label: {
                        Text("Log Set")
                            .fontWeight(.bold)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                    }
                    .tint(WatchTheme.primary)
                    .padding(.horizontal, 16)
                }

                // Heart Rate
                HStack(spacing: 4) {
                    Image(systemName: "heart.fill")
                        .font(.caption2)
                        .foregroundStyle(.red)
                    Text("\(ws.heartRate)")
                        .font(.system(size: 14, weight: .medium))
                    Text("bpm")
                        .font(.system(size: 9))
                        .foregroundStyle(.secondary)
                }

                // Progress
                Text("\(gym.completedSetsTotal)/\(gym.totalSetsTotal) sets")
                    .font(.system(size: 10))
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.horizontal, 8)
    }

    // MARK: - Exercise List Page

    private var exerciseListPage: some View {
        ScrollView {
            VStack(spacing: 4) {
                Text("EXERCISES")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundStyle(.secondary)
                    .padding(.top, 8)

                ForEach(Array(gym.exercises.enumerated()), id: \.element.id) { index, exercise in
                    Button {
                        gym.currentExerciseIndex = index
                        selectedTab = 0
                    } label: {
                        HStack {
                            Circle()
                                .fill(index == gym.currentExerciseIndex ? WatchTheme.gym : Color.gray.opacity(0.3))
                                .frame(width: 8, height: 8)
                            VStack(alignment: .leading, spacing: 1) {
                                Text(exercise.name)
                                    .font(.caption)
                                    .fontWeight(index == gym.currentExerciseIndex ? .bold : .regular)
                                Text("\(exercise.completedSets)/\(exercise.targetSets) sets")
                                    .font(.system(size: 9))
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            if exercise.completedSets >= exercise.targetSets {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundStyle(WatchTheme.primary)
                                    .font(.caption)
                            }
                        }
                        .padding(.vertical, 4)
                        .padding(.horizontal, 8)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}
