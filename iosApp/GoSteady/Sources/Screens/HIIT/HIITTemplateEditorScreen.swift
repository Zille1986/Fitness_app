import SwiftUI

private let hiitOrange = Color(hex: "FF6D00")

struct HIITTemplateEditorScreen: View {
    let onSave: (HIITWorkoutTemplate) -> Void

    @Environment(\.dismiss) private var dismiss

    @State private var templateName = ""
    @State private var templateDescription = ""
    @State private var workDuration: Int = 30
    @State private var restDuration: Int = 15
    @State private var rounds: Int = 3
    @State private var warmupDuration: Int = 60
    @State private var cooldownDuration: Int = 60
    @State private var selectedExercises: [HIITExercise] = []
    @State private var showExercisePicker = false
    @State private var exerciseOverrides: [String: Int] = [:]

    private var isValid: Bool {
        !templateName.trimmingCharacters(in: .whitespaces).isEmpty && !selectedExercises.isEmpty
    }

    private var estimatedDuration: String {
        let totalSec = warmupDuration + cooldownDuration +
            (selectedExercises.count * (workDuration + restDuration) * rounds) - restDuration
        let mins = totalSec / 60
        if mins >= 60 {
            return "\(mins / 60)h \(mins % 60)m"
        }
        return "\(mins)m"
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Name & Description
                nameSection

                // Timing configuration
                timingSection

                // Exercises
                exercisesSection

                // Summary
                summarySection

                // Save button
                saveButton
            }
            .padding()
            .padding(.bottom, 32)
        }
        .background(AppTheme.surface)
        .navigationTitle("Create HIIT Workout")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showExercisePicker) {
            exercisePickerSheet
        }
    }

    // MARK: - Name Section

    private var nameSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionHeader("WORKOUT INFO")

            TextField("Workout Name", text: $templateName)
                .font(.headline)
                .padding(12)
                .background(AppTheme.surfaceContainerLow)
                .clipShape(RoundedRectangle(cornerRadius: 12))

            TextField("Description (optional)", text: $templateDescription, axis: .vertical)
                .lineLimit(2...4)
                .font(.subheadline)
                .padding(12)
                .background(AppTheme.surfaceContainerLow)
                .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    // MARK: - Timing Section

    private var timingSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionHeader("TIMING")

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                timingCard(label: "Work", value: $workDuration, unit: "sec", range: 5...120, step: 5, color: Color(hex: "4CAF50"))
                timingCard(label: "Rest", value: $restDuration, unit: "sec", range: 5...120, step: 5, color: Color(hex: "2196F3"))
                timingCard(label: "Rounds", value: $rounds, unit: "", range: 1...20, step: 1, color: hiitOrange)
                timingCard(label: "Warmup", value: $warmupDuration, unit: "sec", range: 0...300, step: 15, color: Color(hex: "FFC107"))
            }

            // Cooldown
            HStack {
                Text("Cooldown")
                    .font(.subheadline)
                    .fontWeight(.medium)
                Spacer()
                Stepper("\(cooldownDuration)s", value: $cooldownDuration, in: 0...300, step: 15)
                    .font(.subheadline)
            }
            .padding(12)
            .background(AppTheme.surfaceContainerLow)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    private func timingCard(label: String, value: Binding<Int>, unit: String, range: ClosedRange<Int>, step: Int, color: Color) -> some View {
        VStack(spacing: 8) {
            Text(label)
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(color)

            Text(unit.isEmpty ? "\(value.wrappedValue)" : "\(value.wrappedValue)\(unit)")
                .font(.title2)
                .fontWeight(.bold)

            HStack(spacing: 16) {
                Button {
                    let newVal = value.wrappedValue - step
                    if newVal >= range.lowerBound {
                        value.wrappedValue = newVal
                    }
                } label: {
                    Image(systemName: "minus.circle.fill")
                        .font(.title3)
                        .foregroundStyle(color.opacity(0.6))
                }
                .disabled(value.wrappedValue <= range.lowerBound)

                Button {
                    let newVal = value.wrappedValue + step
                    if newVal <= range.upperBound {
                        value.wrappedValue = newVal
                    }
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.title3)
                        .foregroundStyle(color)
                }
                .disabled(value.wrappedValue >= range.upperBound)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Exercises Section

    private var exercisesSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                sectionHeader("EXERCISES (\(selectedExercises.count))")
                Spacer()
                Button {
                    showExercisePicker = true
                } label: {
                    Label("Add", systemImage: "plus.circle.fill")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundStyle(hiitOrange)
                }
            }

            if selectedExercises.isEmpty {
                VStack(spacing: 8) {
                    Image(systemName: "dumbbell.fill")
                        .font(.title2)
                        .foregroundStyle(.tertiary)
                    Text("No exercises added yet")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Button("Add Exercises") {
                        showExercisePicker = true
                    }
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundStyle(hiitOrange)
                }
                .frame(maxWidth: .infinity)
                .padding(24)
                .background(AppTheme.surfaceContainerLow)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            } else {
                ForEach(Array(selectedExercises.enumerated()), id: \.element.id) { index, exercise in
                    exerciseRow(exercise: exercise, index: index)
                }
                .onMove { source, destination in
                    selectedExercises.move(fromOffsets: source, toOffset: destination)
                }
            }
        }
    }

    private func exerciseRow(exercise: HIITExercise, index: Int) -> some View {
        HStack(spacing: 12) {
            // Reorder handle
            Image(systemName: "line.3.horizontal")
                .font(.caption)
                .foregroundStyle(.tertiary)

            VStack(alignment: .leading, spacing: 2) {
                Text(exercise.name)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                HStack(spacing: 8) {
                    Text(exercise.muscleGroups.joined(separator: ", "))
                        .font(.caption2)
                        .foregroundStyle(.secondary)

                    if let override = exerciseOverrides[exercise.id] {
                        Text("\(override)s")
                            .font(.caption2)
                            .fontWeight(.bold)
                            .foregroundStyle(hiitOrange)
                    }
                }
            }

            Spacer()

            // Duration override
            Menu {
                Button("Use default (\(workDuration)s)") {
                    exerciseOverrides.removeValue(forKey: exercise.id)
                }
                ForEach([15, 20, 25, 30, 35, 40, 45, 60], id: \.self) { sec in
                    Button("\(sec) seconds") {
                        exerciseOverrides[exercise.id] = sec
                    }
                }
            } label: {
                Image(systemName: "clock")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            // Remove
            Button {
                selectedExercises.remove(at: index)
                exerciseOverrides.removeValue(forKey: exercise.id)
            } label: {
                Image(systemName: "xmark.circle.fill")
                    .font(.caption)
                    .foregroundStyle(.red.opacity(0.7))
            }
        }
        .padding(12)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Summary

    private var summarySection: some View {
        VStack(alignment: .leading, spacing: 8) {
            sectionHeader("SUMMARY")

            HStack(spacing: 16) {
                summaryItem(icon: "timer", value: estimatedDuration, label: "Total Time")
                summaryItem(icon: "dumbbell.fill", value: "\(selectedExercises.count)", label: "Exercises")
                summaryItem(icon: "repeat", value: "\(rounds)", label: "Rounds")
            }
            .padding(16)
            .background(hiitOrange.opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }

    private func summaryItem(icon: String, value: String, label: String) -> some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundStyle(hiitOrange)
            Text(value)
                .font(.headline)
                .fontWeight(.bold)
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Save Button

    private var saveButton: some View {
        Button {
            let template = buildTemplate()
            onSave(template)
            dismiss()
        } label: {
            Text("Save Workout")
                .fontWeight(.bold)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(isValid ? hiitOrange : Color.gray)
                .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .disabled(!isValid)
    }

    // MARK: - Exercise Picker Sheet

    private var exercisePickerSheet: some View {
        NavigationStack {
            List {
                ForEach(HIITExerciseLibrary.allExercises, id: \.id) { exercise in
                    let isSelected = selectedExercises.contains { $0.id == exercise.id }

                    Button {
                        if isSelected {
                            selectedExercises.removeAll { $0.id == exercise.id }
                        } else {
                            selectedExercises.append(exercise)
                        }
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(exercise.name)
                                    .font(.subheadline)
                                    .fontWeight(.semibold)
                                    .foregroundStyle(AppTheme.onSurface)
                                Text(exercise.muscleGroups.joined(separator: ", "))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                HStack(spacing: 8) {
                                    difficultyBadge(exercise.difficulty)
                                    Text("~\(exercise.caloriesPerMinute) cal/min")
                                        .font(.caption2)
                                        .foregroundStyle(.tertiary)
                                }
                            }
                            Spacer()
                            if isSelected {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundStyle(hiitOrange)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Select Exercises")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        showExercisePicker = false
                    }
                    .fontWeight(.bold)
                    .foregroundStyle(hiitOrange)
                }
            }
        }
        .presentationDetents([.large])
    }

    private func difficultyBadge(_ difficulty: HIITDifficulty) -> some View {
        let colorHex = difficulty.colorHex
        return Text(difficulty.displayName)
            .font(.system(size: 9))
            .fontWeight(.bold)
            .foregroundStyle(Color(hex: colorHex))
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(Color(hex: colorHex).opacity(0.15))
            .clipShape(RoundedRectangle(cornerRadius: 4))
    }

    // MARK: - Helpers

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.caption)
            .fontWeight(.bold)
            .foregroundStyle(AppTheme.onSurfaceVariant)
    }

    private func buildTemplate() -> HIITWorkoutTemplate {
        let exercises = selectedExercises.map { exercise in
            HIITWorkoutExercise(
                exercise: exercise,
                durationOverrideSec: exerciseOverrides[exercise.id]
            )
        }

        return HIITWorkoutTemplate(
            id: "custom_\(UUID().uuidString.prefix(8))",
            name: templateName.trimmingCharacters(in: .whitespaces),
            templateDescription: templateDescription.trimmingCharacters(in: .whitespaces),
            difficulty: estimateDifficulty(),
            exercises: exercises,
            workDurationSec: workDuration,
            restDurationSec: restDuration,
            rounds: rounds,
            warmupSec: warmupDuration,
            cooldownSec: cooldownDuration,
            warmupSteps: [],
            cooldownSteps: []
        )
    }

    private func estimateDifficulty() -> String {
        let avgDifficulty = selectedExercises.reduce(0.0) { sum, ex in
            switch ex.difficulty {
            case .easy: return sum + 1.0
            case .medium: return sum + 2.0
            case .hard: return sum + 3.0
            }
        } / max(1.0, Double(selectedExercises.count))

        let workRestRatio = Double(workDuration) / max(1, Double(restDuration))
        let intensityScore = avgDifficulty * workRestRatio * Double(rounds) / 3.0

        if intensityScore < 2.0 { return "Easy" }
        if intensityScore < 4.0 { return "Medium" }
        return "Hard"
    }
}
