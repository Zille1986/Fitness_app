import SwiftUI

struct GymDashboardScreen: View {
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    DashboardHeroBanner(
                        title: "Gym",
                        subtitle: "Build strength and track your lifts",
                        color: AppTheme.gym
                    )

                    EmptyStateView(
                        icon: "dumbbell.fill",
                        title: "No workouts yet",
                        subtitle: "Start a workout to track your sets and reps"
                    )
                    .padding(.horizontal)
                }
                .padding(.vertical)
            }
            .background(AppTheme.surface)
            .navigationTitle("Gym")
            .overlay(alignment: .bottomTrailing) {
                Button {
                    // TODO: Start workout
                } label: {
                    Label("Start Workout", systemImage: "dumbbell.fill")
                        .fontWeight(.bold)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 14)
                        .background(AppTheme.primary)
                        .foregroundStyle(.white)
                        .clipShape(Capsule())
                        .shadow(color: AppTheme.primary.opacity(0.3), radius: 12, y: 6)
                }
                .padding(24)
            }
        }
    }
}
