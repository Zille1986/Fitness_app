import SwiftUI
import SwiftData

struct GymDashboardScreen: View {
    @Environment(\.modelContext) private var modelContext
    @State private var viewModel = GymViewModel()
    @State private var navigateToActiveWorkout = false
    @State private var selectedTemplateId: UUID?
    @State private var showQuickStartSheet = false
    @State private var showCreateTemplate = false
    @State private var selectedWorkoutId: UUID?

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 16) {
                    // Hero banner
                    DashboardHeroBanner(
                        icon: "dumbbell.fill",
                        title: "Gym",
                        subtitle: "Build strength and track your lifts"
                    )

                    // Weekly volume stats
                    weeklyVolumeCard

                    // Next workout
                    nextWorkoutCard

                    // Quick actions
                    quickActionsRow

                    // Templates section
                    templatesSection

                    // Recent workouts
                    recentWorkoutsSection
                }
                .padding(.vertical)
            }
            .background(AppTheme.surface)
            .navigationTitle("Gym")
            .refreshable {
                viewModel.refresh()
            }
            .overlay(alignment: .bottomTrailing) {
                Button {
                    showQuickStartSheet = true
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
            .sheet(isPresented: $showQuickStartSheet) {
                QuickStartWorkoutSheet(
                    templates: viewModel.templates,
                    onSelectTemplate: { templateId in
                        showQuickStartSheet = false
                        selectedTemplateId = templateId
                        navigateToActiveWorkout = true
                    },
                    onQuickStart: {
                        showQuickStartSheet = false
                        selectedTemplateId = nil
                        navigateToActiveWorkout = true
                    }
                )
            }
            .sheet(isPresented: $showCreateTemplate) {
                WorkoutTemplateScreen()
            }
            .navigationDestination(isPresented: $navigateToActiveWorkout) {
                ActiveWorkoutScreen(templateId: selectedTemplateId)
            }
            .navigationDestination(item: $selectedWorkoutId) { workoutId in
                ExerciseDetailScreen(exerciseId: workoutId)
            }
            .onAppear {
                viewModel.configure(modelContext: modelContext)
            }
        }
    }

    // MARK: - Weekly Volume

    private var weeklyVolumeCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("WEEKLY VOLUME")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.gym)

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(format: "%.0f kg", viewModel.totalWeeklyVolume))
                        .font(.title)
                        .fontWeight(.bold)
                        .foregroundStyle(AppTheme.gym)
                    Text("Total volume")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("\(viewModel.totalWeeklySets)")
                        .font(.title)
                        .fontWeight(.bold)
                    Text("Total sets")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            WeeklyBarChart(
                data: viewModel.weeklyVolume,
                labels: ["M", "T", "W", "T", "F", "S", "S"],
                accentColor: AppTheme.gym
            )
            .frame(height: 80)
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - Next Workout

    private var nextWorkoutCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("NEXT WORKOUT")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.gym)

            if let next = viewModel.nextWorkout {
                VStack(alignment: .leading, spacing: 4) {
                    Text(next.name)
                        .font(.title3)
                        .fontWeight(.bold)
                    Text("\(next.exerciseCount) exercises")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    if let lastPerformed = next.lastPerformed {
                        Text("Last: \(lastPerformed)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                Button {
                    selectedTemplateId = next.templateId
                    navigateToActiveWorkout = true
                } label: {
                    HStack {
                        Image(systemName: "play.fill")
                        Text("Start")
                    }
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                    .background(AppTheme.gym)
                    .foregroundStyle(.white)
                    .clipShape(Capsule())
                }
            } else {
                Text("No templates yet. Create one to get started!")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                Button {
                    showCreateTemplate = true
                } label: {
                    Text("Create Template")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundStyle(AppTheme.gym)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(AppTheme.gym.opacity(0.2), lineWidth: 1)
        )
        .padding(.horizontal)
    }

    // MARK: - Quick Actions

    private var quickActionsRow: some View {
        HStack(spacing: 12) {
            quickActionButton(icon: "list.bullet", title: "Exercises", subtitle: "Library") {
                // Navigate to exercise picker - handled by NavigationLink below
            }
            .overlay {
                NavigationLink(destination: ExercisePickerScreen(mode: .browse)) {
                    Color.clear
                }
            }

            quickActionButton(icon: "calendar", title: "Schedule", subtitle: "Plan workouts") {
                // Training plans
            }
            .overlay {
                NavigationLink(destination: TrainingPlanScreen()) {
                    Color.clear
                }
            }
        }
        .padding(.horizontal)
    }

    private func quickActionButton(icon: String, title: String, subtitle: String, action: @escaping () -> Void) -> some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundStyle(AppTheme.gym)
            Text(title)
                .font(.caption)
                .fontWeight(.bold)
            Text(subtitle)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Templates

    private var templatesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("WORKOUT TEMPLATES")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundStyle(.secondary)
                Spacer()
                Button {
                    showCreateTemplate = true
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "plus")
                        Text("New")
                    }
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundStyle(AppTheme.gym)
                }
            }
            .padding(.horizontal)

            if viewModel.templates.isEmpty {
                EmptyStateView(
                    icon: "dumbbell.fill",
                    title: "No templates yet",
                    message: "Create a workout template to get started"
                )
                .padding(.horizontal)
            } else {
                ForEach(viewModel.templates) { template in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(template.name)
                                .fontWeight(.medium)
                            Text("\(template.exerciseCount) exercises")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Button {
                            selectedTemplateId = template.id
                            navigateToActiveWorkout = true
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: "play.fill")
                                    .font(.caption)
                                Text("Start")
                                    .font(.caption)
                                    .fontWeight(.semibold)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(AppTheme.gym.opacity(0.15))
                            .foregroundStyle(AppTheme.gym)
                            .clipShape(Capsule())
                        }
                    }
                    .padding(16)
                    .background(AppTheme.surfaceContainerLow)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .padding(.horizontal)
                }
            }
        }
    }

    // MARK: - Recent Workouts

    private var recentWorkoutsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("RECENT WORKOUTS")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)
                .padding(.horizontal)

            if viewModel.recentWorkouts.isEmpty {
                EmptyStateView(
                    icon: "dumbbell.fill",
                    title: "No workouts yet",
                    message: "Start a workout to track your sets and reps"
                )
                .padding(.horizontal)
            } else {
                ForEach(viewModel.recentWorkouts.prefix(5)) { workout in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(workout.date)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text(workout.name)
                                .fontWeight(.medium)
                        }
                        Spacer()
                        HStack(spacing: 16) {
                            VStack(alignment: .trailing, spacing: 2) {
                                Text("\(workout.exerciseCount)")
                                    .fontWeight(.bold)
                                Text("exercises")
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                            VStack(alignment: .trailing, spacing: 2) {
                                Text("\(workout.totalSets)")
                                    .fontWeight(.bold)
                                Text("sets")
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                    }
                    .padding(16)
                    .background(AppTheme.surfaceContainerLow)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .padding(.horizontal)
                }
            }
        }
    }
}

// MARK: - Quick Start Sheet

struct QuickStartWorkoutSheet: View {
    @Environment(\.dismiss) private var dismiss
    let templates: [TemplateInfo]
    let onSelectTemplate: (UUID) -> Void
    let onQuickStart: () -> Void

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Button {
                        onQuickStart()
                    } label: {
                        HStack(spacing: 16) {
                            Image(systemName: "bolt.circle.fill")
                                .font(.title)
                                .foregroundStyle(AppTheme.gym)
                            VStack(alignment: .leading) {
                                Text("Quick Start").fontWeight(.bold)
                                Text("Empty workout - add exercises as you go")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }

                if !templates.isEmpty {
                    Section("From Template") {
                        ForEach(templates) { template in
                            Button {
                                onSelectTemplate(template.id)
                            } label: {
                                HStack {
                                    Image(systemName: "doc.text")
                                        .foregroundStyle(AppTheme.gym)
                                        .frame(width: 32, height: 32)
                                        .background(AppTheme.gym.opacity(0.15))
                                        .clipShape(RoundedRectangle(cornerRadius: 8))

                                    VStack(alignment: .leading) {
                                        Text(template.name).fontWeight(.semibold)
                                        Text("\(template.exerciseCount) exercises")
                                            .font(.caption)
                                            .foregroundStyle(.secondary)
                                    }

                                    Spacer()

                                    Image(systemName: "chevron.right")
                                        .font(.caption)
                                        .foregroundStyle(.tertiary)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Start Workout")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
