import SwiftUI
import SwiftData

struct TrainingPlanScreen: View {
    @Environment(\.modelContext) private var modelContext
    @State private var viewModel = TrainingPlanViewModel()

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                // Active plan section
                if let activePlan = viewModel.activePlan {
                    activePlanSection(plan: activePlan)
                }

                // Pre-built plans
                preBuiltPlansSection

                // Your plans
                yourPlansSection
            }
            .padding(.vertical)
        }
        .background(AppTheme.surface)
        .navigationTitle("Training Plans")
        .refreshable {
            viewModel.refresh()
        }
        .sheet(isPresented: $viewModel.showCreatePlanSheet) {
            createPlanSheet
        }
        .overlay {
            if viewModel.isGenerating {
                generatingOverlay
            }
        }
        .onAppear {
            viewModel.configure(modelContext: modelContext)
        }
    }

    // MARK: - Active Plan

    private func activePlanSection(plan: TrainingPlan) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        Circle()
                            .fill(AppTheme.running)
                            .frame(width: 8, height: 8)
                        Text("ACTIVE PLAN")
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundStyle(AppTheme.running)
                    }
                    Text(plan.name)
                        .font(.title3)
                        .fontWeight(.bold)
                    Text(plan.planDescription)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }

            // Progress
            let completedCount = plan.weeklySchedule.filter(\.isCompleted).count
            let totalCount = plan.weeklySchedule.count
            let progress = totalCount > 0 ? Double(completedCount) / Double(totalCount) : 0

            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("\(completedCount)/\(totalCount) workouts completed")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text("\(Int(progress * 100))%")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundStyle(AppTheme.running)
                }
                ProgressView(value: progress)
                    .tint(AppTheme.running)
            }

            // Week selector
            weekSelector

            // Weekly schedule
            weeklyScheduleView
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(AppTheme.running.opacity(0.2), lineWidth: 1)
        )
        .padding(.horizontal)
    }

    // MARK: - Week Selector

    private var weekSelector: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(1...max(viewModel.totalWeeksInActivePlan, 1), id: \.self) { week in
                    Button {
                        viewModel.selectWeek(week)
                    } label: {
                        Text("W\(week)")
                            .font(.caption)
                            .fontWeight(viewModel.selectedWeekNumber == week ? .bold : .medium)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(
                                viewModel.selectedWeekNumber == week
                                    ? AppTheme.running
                                    : AppTheme.running.opacity(0.1)
                            )
                            .foregroundStyle(
                                viewModel.selectedWeekNumber == week ? .white : AppTheme.running
                            )
                            .clipShape(Capsule())
                    }
                }
            }
        }
    }

    // MARK: - Weekly Schedule

    private var weeklyScheduleView: some View {
        VStack(spacing: 8) {
            ForEach(viewModel.weeklyScheduleForActivePlan) { day in
                HStack(spacing: 12) {
                    // Day label
                    Text(day.dayName)
                        .font(.caption)
                        .fontWeight(.bold)
                        .frame(width: 32)
                        .foregroundStyle(.secondary)

                    if day.isRestDay {
                        Text("Rest Day")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.vertical, 8)
                    } else if let workout = day.workout {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(workout.workoutType.rawValue.replacingOccurrences(of: "_", with: " ").capitalized)
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                                Text(workout.workoutDescription)
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(1)
                            }

                            Spacer()

                            if let duration = workout.targetDurationMinutes {
                                Text("\(duration)m")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }

                            // Completion indicator
                            if workout.isCompleted {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundStyle(AppTheme.running)
                            } else {
                                Button {
                                    viewModel.markWorkoutComplete(workoutId: workout.id)
                                } label: {
                                    Image(systemName: "circle")
                                        .foregroundStyle(.secondary)
                                }
                            }
                        }
                        .padding(.vertical, 8)
                        .padding(.horizontal, 12)
                        .background(
                            workout.isCompleted
                                ? AppTheme.running.opacity(0.05)
                                : .clear
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                }
            }
        }
    }

    // MARK: - Pre-Built Plans

    private var preBuiltPlansSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("PRE-BUILT PLANS")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)
                .padding(.horizontal)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(viewModel.preBuiltPlans) { plan in
                        Button {
                            viewModel.createFromPreBuilt(plan)
                        } label: {
                            VStack(alignment: .leading, spacing: 8) {
                                Image(systemName: plan.icon)
                                    .font(.title2)
                                    .foregroundStyle(plan.color)

                                Text(plan.name)
                                    .font(.subheadline)
                                    .fontWeight(.bold)
                                    .foregroundStyle(.primary)
                                    .lineLimit(1)

                                Text(plan.subtitle)
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(2)
                                    .multilineTextAlignment(.leading)

                                HStack(spacing: 8) {
                                    HStack(spacing: 2) {
                                        Image(systemName: "calendar")
                                            .font(.system(size: 8))
                                        Text("\(plan.weeks)w")
                                            .font(.caption2)
                                    }
                                    HStack(spacing: 2) {
                                        Image(systemName: "figure.run")
                                            .font(.system(size: 8))
                                        Text("\(plan.daysPerWeek)d/w")
                                            .font(.caption2)
                                    }
                                }
                                .foregroundStyle(.secondary)
                            }
                            .frame(width: 140, alignment: .leading)
                            .padding(14)
                            .background(plan.color.opacity(0.08))
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                            .overlay(
                                RoundedRectangle(cornerRadius: 14)
                                    .stroke(plan.color.opacity(0.15), lineWidth: 1)
                            )
                        }
                    }
                }
                .padding(.horizontal)
            }
        }
    }

    // MARK: - Your Plans

    private var yourPlansSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("YOUR PLANS")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundStyle(.secondary)
                Spacer()
                Button {
                    viewModel.showCreatePlanSheet = true
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "plus")
                        Text("New Plan")
                    }
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundStyle(AppTheme.running)
                }
            }
            .padding(.horizontal)

            if viewModel.plans.isEmpty {
                EmptyStateView(
                    icon: "calendar",
                    title: "No training plans",
                    message: "Create a plan to reach your running goals"
                )
                .padding(.horizontal)
            } else {
                ForEach(viewModel.plans) { plan in
                    planCard(plan)
                        .padding(.horizontal)
                }
            }
        }
    }

    private func planCard(_ plan: TrainingPlanInfo) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    if plan.isActive {
                        Text("ACTIVE")
                            .font(.caption2)
                            .fontWeight(.bold)
                            .foregroundStyle(AppTheme.running)
                    }
                    Text(plan.name)
                        .fontWeight(.bold)
                    Text(plan.description)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                Spacer()

                Menu {
                    if !plan.isActive {
                        Button {
                            viewModel.setActivePlan(plan.id)
                        } label: {
                            Label("Set as Active", systemImage: "checkmark")
                        }
                    }
                    Button(role: .destructive) {
                        viewModel.deletePlan(plan.id)
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .foregroundStyle(.secondary)
                }
            }

            // Info chips
            HStack(spacing: 12) {
                infoChip(icon: "calendar", text: "\(plan.weeks) weeks")
                infoChip(icon: "flag.fill", text: goalTypeName(plan.goalType))
                if let distance = plan.targetDistanceFormatted {
                    infoChip(icon: "figure.run", text: distance)
                }
            }

            // Progress bar
            ProgressView(value: plan.progress)
                .tint(plan.isActive ? AppTheme.running : .secondary)

            Text("\(plan.completedWorkouts)/\(plan.totalWorkouts) workouts")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .background(plan.isActive ? AppTheme.running.opacity(0.05) : AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(plan.isActive ? AppTheme.running.opacity(0.2) : .clear, lineWidth: 1)
        )
    }

    private func infoChip(icon: String, text: String) -> some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption2)
            Text(text)
                .font(.caption)
        }
        .foregroundStyle(.secondary)
    }

    // MARK: - Create Plan Sheet

    private var createPlanSheet: some View {
        NavigationStack {
            Form {
                Section("Goal") {
                    ForEach([GoalType.first5K, .improve5K, .first10K, .improve10K, .halfMarathon, .marathon], id: \.self) { goal in
                        Button {
                            viewModel.newPlanGoalType = goal
                        } label: {
                            HStack {
                                Text(goalTypeName(goal))
                                    .foregroundStyle(.primary)
                                Spacer()
                                if viewModel.newPlanGoalType == goal {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(AppTheme.running)
                                }
                            }
                        }
                    }
                }

                Section("Training Days") {
                    let dayLabels = [(1, "Sun"), (2, "Mon"), (3, "Tue"), (4, "Wed"), (5, "Thu"), (6, "Fri"), (7, "Sat")]
                    HStack {
                        ForEach(dayLabels, id: \.0) { dayValue, dayName in
                            Button {
                                if viewModel.newPlanSelectedDays.contains(dayValue) {
                                    viewModel.newPlanSelectedDays.remove(dayValue)
                                } else {
                                    viewModel.newPlanSelectedDays.insert(dayValue)
                                }
                            } label: {
                                Text(dayName)
                                    .font(.caption)
                                    .fontWeight(.semibold)
                                    .frame(width: 36, height: 36)
                                    .background(
                                        viewModel.newPlanSelectedDays.contains(dayValue)
                                            ? AppTheme.running : Color.gray.opacity(0.2)
                                    )
                                    .foregroundStyle(
                                        viewModel.newPlanSelectedDays.contains(dayValue)
                                            ? .white : .primary
                                    )
                                    .clipShape(Circle())
                            }
                        }
                    }
                    .frame(maxWidth: .infinity)
                }

                Section("Current Fitness") {
                    HStack {
                        VStack(alignment: .leading) {
                            Text("Weekly km")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            TextField("30", text: $viewModel.newPlanWeeklyKm)
                                .keyboardType(.numberPad)
                        }
                        VStack(alignment: .leading) {
                            Text("Long run km")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            TextField("12", text: $viewModel.newPlanLongRunKm)
                                .keyboardType(.numberPad)
                        }
                    }
                }
            }
            .navigationTitle("Create Plan")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        viewModel.showCreatePlanSheet = false
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create") {
                        viewModel.createPlan()
                    }
                    .disabled(viewModel.newPlanSelectedDays.count < 3)
                    .fontWeight(.bold)
                }
            }
        }
    }

    // MARK: - Generating Overlay

    private var generatingOverlay: some View {
        ZStack {
            Color.black.opacity(0.3)
                .ignoresSafeArea()

            VStack(spacing: 16) {
                ProgressView()
                    .scaleEffect(1.2)
                Text("Generating your plan...")
                    .font(.subheadline)
                    .fontWeight(.medium)
            }
            .padding(32)
            .background(.regularMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }

    // MARK: - Helpers

    private func goalTypeName(_ goalType: GoalType) -> String {
        switch goalType {
        case .first5K: return "First 5K"
        case .improve5K: return "Improve 5K"
        case .first10K: return "First 10K"
        case .improve10K: return "Improve 10K"
        case .halfMarathon: return "Half Marathon"
        case .marathon: return "Marathon"
        case .generalFitness: return "General Fitness"
        case .weightLoss: return "Weight Loss"
        case .custom: return "Custom"
        }
    }
}
