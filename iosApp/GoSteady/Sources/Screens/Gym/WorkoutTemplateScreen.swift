import SwiftUI
import SwiftData

struct WorkoutTemplateScreen: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var description = ""
    @State private var exercises: [TemplateExercise] = []
    @State private var showExercisePicker = false
    @State private var showEditDialog = false
    @State private var editingExerciseIndex: Int?
    @State private var isSaving = false

    // Edit dialog state
    @State private var editSets = "3"
    @State private var editMinReps = "8"
    @State private var editMaxReps = "12"
    @State private var editRest = "90"

    /// Optional: pass an existing template ID to edit
    var editTemplateId: UUID?

    private var isValid: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty && !exercises.isEmpty
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 16) {
                    // Name and description
                    templateInfoSection

                    // Pre-built templates
                    if exercises.isEmpty {
                        preBuiltTemplatesSection
                    }

                    // Exercise list
                    exerciseListSection

                    // Add exercise button
                    addExerciseButton

                    Color.clear.frame(height: 80)
                }
                .padding(16)
            }
            .background(AppTheme.surface)
            .navigationTitle(editTemplateId != nil ? "Edit Template" : "New Template")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        saveTemplate()
                    }
                    .disabled(!isValid || isSaving)
                    .fontWeight(.bold)
                }
            }
            .sheet(isPresented: $showExercisePicker) {
                ExercisePickerScreen(mode: .pick) { exercise in
                    addExercise(exercise)
                    showExercisePicker = false
                }
            }
            .alert("Edit Exercise", isPresented: $showEditDialog) {
                TextField("Sets", text: $editSets)
                    .keyboardType(.numberPad)
                TextField("Min Reps", text: $editMinReps)
                    .keyboardType(.numberPad)
                TextField("Max Reps", text: $editMaxReps)
                    .keyboardType(.numberPad)
                TextField("Rest (seconds)", text: $editRest)
                    .keyboardType(.numberPad)
                Button("Save") {
                    if let index = editingExerciseIndex {
                        exercises[index].sets = Int(editSets) ?? exercises[index].sets
                        exercises[index].targetRepsMin = Int(editMinReps) ?? exercises[index].targetRepsMin
                        exercises[index].targetRepsMax = Int(editMaxReps) ?? exercises[index].targetRepsMax
                        exercises[index].restSeconds = Int(editRest) ?? exercises[index].restSeconds
                    }
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                if let index = editingExerciseIndex, index < exercises.count {
                    Text(exercises[index].exerciseName)
                }
            }
            .onAppear {
                loadExistingTemplate()
            }
        }
    }

    // MARK: - Template Info

    private var templateInfoSection: some View {
        VStack(spacing: 12) {
            TextField("Template Name", text: $name)
                .font(.headline)
                .padding(12)
                .background(AppTheme.surfaceContainerLow)
                .clipShape(RoundedRectangle(cornerRadius: 12))

            TextField("Description (optional)", text: $description, axis: .vertical)
                .lineLimit(2...4)
                .font(.subheadline)
                .padding(12)
                .background(AppTheme.surfaceContainerLow)
                .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    // MARK: - Pre-Built Templates

    private var preBuiltTemplatesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("QUICK START")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    preBuiltCard(title: "Push Day", subtitle: "Chest, Shoulders, Triceps", icon: "arrow.up.circle.fill", color: .blue) {
                        applyPreBuilt(name: "Push Day", description: "Chest, shoulders, and triceps", exercises: [
                            ("Barbell Bench Press", .chest), ("Overhead Press", .shoulders),
                            ("Incline Dumbbell Press", .chest), ("Lateral Raise", .shoulders),
                            ("Tricep Pushdown", .triceps)
                        ])
                    }

                    preBuiltCard(title: "Pull Day", subtitle: "Back, Biceps", icon: "arrow.down.circle.fill", color: .green) {
                        applyPreBuilt(name: "Pull Day", description: "Back and biceps", exercises: [
                            ("Barbell Row", .back), ("Pull-Up", .lats),
                            ("Seated Cable Row", .back), ("Face Pull", .shoulders),
                            ("Barbell Curl", .biceps)
                        ])
                    }

                    preBuiltCard(title: "Leg Day", subtitle: "Quads, Hams, Glutes", icon: "figure.walk.circle.fill", color: .orange) {
                        applyPreBuilt(name: "Leg Day", description: "Quads, hamstrings, and glutes", exercises: [
                            ("Barbell Squat", .quads), ("Romanian Deadlift", .hamstrings),
                            ("Leg Press", .quads), ("Leg Curl", .hamstrings),
                            ("Calf Raise", .calves)
                        ])
                    }

                    preBuiltCard(title: "Upper Body", subtitle: "Chest, Back, Arms", icon: "figure.boxing", color: .purple) {
                        applyPreBuilt(name: "Upper Body", description: "Full upper body workout", exercises: [
                            ("Barbell Bench Press", .chest), ("Barbell Row", .back),
                            ("Overhead Press", .shoulders), ("Pull-Up", .lats),
                            ("Barbell Curl", .biceps), ("Tricep Pushdown", .triceps)
                        ])
                    }

                    preBuiltCard(title: "Lower Body", subtitle: "Legs & Core", icon: "figure.strengthtraining.functional", color: .red) {
                        applyPreBuilt(name: "Lower Body", description: "Legs and core", exercises: [
                            ("Barbell Squat", .quads), ("Deadlift", .hamstrings),
                            ("Leg Press", .quads), ("Hip Thrust", .glutes),
                            ("Plank", .abs)
                        ])
                    }
                }
            }
        }
    }

    private func preBuiltCard(title: String, subtitle: String, icon: String, color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundStyle(color)
                Text(title)
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundStyle(.primary)
                Text(subtitle)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            .frame(width: 120)
            .padding(.vertical, 16)
            .background(color.opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    // MARK: - Exercise List

    private var exerciseListSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            if !exercises.isEmpty {
                HStack {
                    Text("EXERCISES")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text("\(exercises.count) exercises")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            ForEach(Array(exercises.enumerated()), id: \.element.id) { index, exercise in
                HStack {
                    // Reorder controls
                    VStack(spacing: 2) {
                        Button {
                            moveExercise(from: index, direction: -1)
                        } label: {
                            Image(systemName: "chevron.up")
                                .font(.caption2)
                                .foregroundStyle(index > 0 ? .primary : .quaternary)
                        }
                        .disabled(index == 0)

                        Button {
                            moveExercise(from: index, direction: 1)
                        } label: {
                            Image(systemName: "chevron.down")
                                .font(.caption2)
                                .foregroundStyle(index < exercises.count - 1 ? .primary : .quaternary)
                        }
                        .disabled(index == exercises.count - 1)
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text(exercise.exerciseName)
                            .fontWeight(.medium)
                        Text("\(exercise.sets) sets x \(exercise.targetRepsDisplay) reps - \(exercise.restSeconds)s rest")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }

                    Spacer()

                    // Edit
                    Button {
                        editingExerciseIndex = index
                        editSets = "\(exercise.sets)"
                        editMinReps = "\(exercise.targetRepsMin)"
                        editMaxReps = "\(exercise.targetRepsMax)"
                        editRest = "\(exercise.restSeconds)"
                        showEditDialog = true
                    } label: {
                        Image(systemName: "pencil")
                            .font(.caption)
                            .foregroundStyle(AppTheme.primary)
                    }

                    // Delete
                    Button(role: .destructive) {
                        withAnimation {
                            exercises.remove(at: index)
                            reindexExercises()
                        }
                    } label: {
                        Image(systemName: "trash")
                            .font(.caption)
                            .foregroundStyle(.red.opacity(0.7))
                    }
                }
                .padding(12)
                .background(AppTheme.surfaceContainerLow)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    // MARK: - Add Exercise Button

    private var addExerciseButton: some View {
        Button {
            showExercisePicker = true
        } label: {
            HStack {
                Image(systemName: "plus.circle.fill")
                Text("Add Exercise")
            }
            .font(.subheadline)
            .fontWeight(.semibold)
            .foregroundStyle(AppTheme.primary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(AppTheme.primary.opacity(0.1))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    // MARK: - Helpers

    private func addExercise(_ exercise: Exercise) {
        let templateExercise = TemplateExercise(
            exerciseId: exercise.id,
            exerciseName: exercise.name,
            sets: 3,
            targetRepsMin: 8,
            targetRepsMax: 12,
            restSeconds: 90,
            orderIndex: exercises.count
        )
        exercises.append(templateExercise)
    }

    private func moveExercise(from index: Int, direction: Int) {
        let newIndex = index + direction
        guard newIndex >= 0 && newIndex < exercises.count else { return }
        withAnimation {
            exercises.swapAt(index, newIndex)
            reindexExercises()
        }
    }

    private func reindexExercises() {
        for i in exercises.indices {
            exercises[i].orderIndex = i
        }
    }

    private func applyPreBuilt(name: String, description: String, exercises: [(String, MuscleGroup)]) {
        self.name = name
        self.description = description
        self.exercises = exercises.enumerated().map { index, pair in
            TemplateExercise(
                exerciseId: UUID(), // Will be resolved when exercise is selected from library
                exerciseName: pair.0,
                sets: 3,
                targetRepsMin: pair.1 == .quads || pair.1 == .hamstrings || pair.1 == .glutes ? 6 : 8,
                targetRepsMax: pair.1 == .quads || pair.1 == .hamstrings || pair.1 == .glutes ? 10 : 12,
                restSeconds: pair.1 == .quads || pair.1 == .hamstrings || pair.1 == .glutes ? 120 : 90,
                orderIndex: index
            )
        }
    }

    private func loadExistingTemplate() {
        guard let editTemplateId else { return }

        let descriptor = FetchDescriptor<WorkoutTemplate>(
            predicate: #Predicate<WorkoutTemplate> { t in t.id == editTemplateId }
        )
        if let template = try? modelContext.fetch(descriptor).first {
            name = template.name
            description = template.templateDescription
            exercises = template.exercises.sorted(by: { $0.orderIndex < $1.orderIndex })
        }
    }

    private func saveTemplate() {
        guard isValid else { return }
        isSaving = true

        let trimmedName = name.trimmingCharacters(in: .whitespaces)
        let trimmedDesc = description.trimmingCharacters(in: .whitespaces)

        // Calculate estimated duration
        let totalSets = exercises.reduce(0) { $0 + $1.sets }
        let avgRest = exercises.isEmpty ? 90 : exercises.reduce(0) { $0 + $1.restSeconds } / exercises.count
        let estimatedMinutes = max(15, (totalSets * (45 + avgRest)) / 60)

        if let editTemplateId {
            // Update existing template
            let descriptor = FetchDescriptor<WorkoutTemplate>(
                predicate: #Predicate<WorkoutTemplate> { t in t.id == editTemplateId }
            )
            if let existing = try? modelContext.fetch(descriptor).first {
                existing.name = trimmedName
                existing.templateDescription = trimmedDesc
                existing.exercises = exercises
                existing.estimatedDurationMinutes = estimatedMinutes
            }
        } else {
            // Create new template
            let template = WorkoutTemplate(
                name: trimmedName,
                templateDescription: trimmedDesc,
                exercises: exercises,
                estimatedDurationMinutes: estimatedMinutes
            )
            modelContext.insert(template)
        }

        do {
            try modelContext.save()
            dismiss()
        } catch {
            isSaving = false
        }
    }
}
