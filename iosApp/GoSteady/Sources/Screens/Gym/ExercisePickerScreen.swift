import SwiftUI
import SwiftData

struct ExercisePickerScreen: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    enum Mode {
        case browse
        case pick
    }

    let mode: Mode
    var onSelect: ((Exercise) -> Void)?

    @State private var allExercises: [Exercise] = []
    @State private var searchQuery = ""
    @State private var selectedMuscleGroup: MuscleGroup?
    @State private var selectedEquipment: Equipment?
    @State private var showFilterSheet = false
    @State private var showCreateExercise = false
    @State private var isLoading = true

    init(mode: Mode, onSelect: ((Exercise) -> Void)? = nil) {
        self.mode = mode
        self.onSelect = onSelect
    }

    private var filteredExercises: [Exercise] {
        allExercises.filter { exercise in
            let matchesMuscle = selectedMuscleGroup == nil ||
                exercise.muscleGroup == selectedMuscleGroup ||
                exercise.secondaryMuscleGroups.contains(selectedMuscleGroup!)

            let matchesEquipment = selectedEquipment == nil ||
                exercise.equipment == selectedEquipment

            let matchesQuery = searchQuery.isEmpty ||
                exercise.name.localizedCaseInsensitiveContains(searchQuery) ||
                exercise.muscleGroup.displayName.localizedCaseInsensitiveContains(searchQuery)

            return matchesMuscle && matchesEquipment && matchesQuery
        }
    }

    private var groupedExercises: [(MuscleGroup, [Exercise])] {
        let dict = Dictionary(grouping: filteredExercises, by: \.muscleGroup)
        return dict.sorted { $0.key.displayName < $1.key.displayName }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Search bar
                HStack(spacing: 12) {
                    HStack(spacing: 8) {
                        Image(systemName: "magnifyingglass")
                            .foregroundStyle(.secondary)
                        TextField("Search exercises...", text: $searchQuery)
                            .textFieldStyle(.plain)
                        if !searchQuery.isEmpty {
                            Button {
                                searchQuery = ""
                            } label: {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                    .padding(10)
                    .background(AppTheme.surfaceContainerLow)
                    .clipShape(RoundedRectangle(cornerRadius: 12))

                    Button {
                        showFilterSheet = true
                    } label: {
                        Image(systemName: "line.3.horizontal.decrease.circle\(hasActiveFilters ? ".fill" : "")")
                            .font(.title3)
                            .foregroundStyle(hasActiveFilters ? AppTheme.primary : .secondary)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)

                // Active filters
                if hasActiveFilters {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            if let muscle = selectedMuscleGroup {
                                filterChip(label: muscle.displayName) {
                                    selectedMuscleGroup = nil
                                }
                            }
                            if let equip = selectedEquipment {
                                filterChip(label: equip.displayName) {
                                    selectedEquipment = nil
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                    .padding(.bottom, 8)
                }

                // Muscle group quick scroll
                if selectedMuscleGroup == nil && searchQuery.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(MuscleGroup.allCases, id: \.self) { group in
                                Button {
                                    selectedMuscleGroup = group
                                } label: {
                                    Text(group.displayName)
                                        .font(.caption)
                                        .fontWeight(.medium)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(AppTheme.surfaceContainerLow)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                    .padding(.bottom, 8)
                }

                // Exercise list
                if isLoading {
                    Spacer()
                    ProgressView()
                    Spacer()
                } else if filteredExercises.isEmpty {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 40))
                            .foregroundStyle(.secondary.opacity(0.4))
                        Text("No exercises found")
                            .fontWeight(.semibold)
                        Text("Try adjusting your search or filters")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                } else {
                    List {
                        ForEach(groupedExercises, id: \.0) { group, exercises in
                            Section {
                                ForEach(exercises, id: \.id) { exercise in
                                    exerciseRow(exercise)
                                }
                            } header: {
                                Text(group.displayName)
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundStyle(AppTheme.primary)
                            }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Exercises")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if mode == .pick {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { dismiss() }
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showCreateExercise = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showFilterSheet) {
                filterSheet
            }
            .sheet(isPresented: $showCreateExercise) {
                CreateExerciseSheet { newExercise in
                    modelContext.insert(newExercise)
                    try? modelContext.save()
                    loadExercises()
                }
            }
            .onAppear {
                loadExercises()
            }
        }
    }

    // MARK: - Exercise Row

    private func exerciseRow(_ exercise: Exercise) -> some View {
        Button {
            if mode == .pick {
                onSelect?(exercise)
            }
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(exercise.name)
                        .fontWeight(.medium)
                        .foregroundStyle(.primary)
                    HStack(spacing: 6) {
                        Text(exercise.equipment.displayName)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Text("*")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Text(exercise.exerciseType.rawValue.capitalized)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                Spacer()
                if mode == .pick {
                    Image(systemName: "plus.circle")
                        .foregroundStyle(AppTheme.primary)
                } else {
                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                }
            }
            .contentShape(Rectangle())
        }
    }

    // MARK: - Filter Chip

    private func filterChip(label: String, onRemove: @escaping () -> Void) -> some View {
        HStack(spacing: 4) {
            Text(label)
                .font(.caption)
                .fontWeight(.medium)
            Button(action: onRemove) {
                Image(systemName: "xmark")
                    .font(.system(size: 8, weight: .bold))
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(AppTheme.primary.opacity(0.15))
        .foregroundStyle(AppTheme.primary)
        .clipShape(Capsule())
    }

    // MARK: - Filter Sheet

    private var filterSheet: some View {
        NavigationStack {
            List {
                Section("Muscle Group") {
                    Button {
                        selectedMuscleGroup = nil
                        showFilterSheet = false
                    } label: {
                        HStack {
                            Text("All Muscles")
                                .foregroundStyle(.primary)
                            Spacer()
                            if selectedMuscleGroup == nil {
                                Image(systemName: "checkmark")
                                    .foregroundStyle(AppTheme.primary)
                            }
                        }
                    }

                    ForEach(MuscleGroup.allCases, id: \.self) { group in
                        Button {
                            selectedMuscleGroup = group
                            showFilterSheet = false
                        } label: {
                            HStack {
                                Text(group.displayName)
                                    .foregroundStyle(.primary)
                                Spacer()
                                if selectedMuscleGroup == group {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(AppTheme.primary)
                                }
                            }
                        }
                    }
                }

                Section("Equipment") {
                    Button {
                        selectedEquipment = nil
                        showFilterSheet = false
                    } label: {
                        HStack {
                            Text("All Equipment")
                                .foregroundStyle(.primary)
                            Spacer()
                            if selectedEquipment == nil {
                                Image(systemName: "checkmark")
                                    .foregroundStyle(AppTheme.primary)
                            }
                        }
                    }

                    ForEach(Equipment.allCases, id: \.self) { equip in
                        Button {
                            selectedEquipment = equip
                            showFilterSheet = false
                        } label: {
                            HStack {
                                Text(equip.displayName)
                                    .foregroundStyle(.primary)
                                Spacer()
                                if selectedEquipment == equip {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(AppTheme.primary)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Filters")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { showFilterSheet = false }
                }
                ToolbarItem(placement: .cancellationAction) {
                    Button("Clear All") {
                        selectedMuscleGroup = nil
                        selectedEquipment = nil
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    // MARK: - Helpers

    private var hasActiveFilters: Bool {
        selectedMuscleGroup != nil || selectedEquipment != nil
    }

    private func loadExercises() {
        isLoading = true
        let descriptor = FetchDescriptor<Exercise>(sortBy: [SortDescriptor(\Exercise.name)])
        allExercises = (try? modelContext.fetch(descriptor)) ?? []
        isLoading = false
    }
}

// MARK: - Create Exercise Sheet

struct CreateExerciseSheet: View {
    @Environment(\.dismiss) private var dismiss
    let onSave: (Exercise) -> Void

    @State private var name = ""
    @State private var description = ""
    @State private var muscleGroup: MuscleGroup = .chest
    @State private var equipment: Equipment = .barbell
    @State private var exerciseType: ExerciseType = .compound
    @State private var difficulty: Difficulty = .intermediate

    private var isValid: Bool { !name.trimmingCharacters(in: .whitespaces).isEmpty }

    var body: some View {
        NavigationStack {
            Form {
                Section("Exercise Name") {
                    TextField("e.g. Incline Dumbbell Press", text: $name)
                }

                Section("Description (optional)") {
                    TextField("Brief description or form cues", text: $description, axis: .vertical)
                        .lineLimit(3)
                }

                Section("Details") {
                    Picker("Muscle Group", selection: $muscleGroup) {
                        ForEach(MuscleGroup.allCases, id: \.self) { group in
                            Text(group.displayName).tag(group)
                        }
                    }

                    Picker("Equipment", selection: $equipment) {
                        ForEach(Equipment.allCases, id: \.self) { equip in
                            Text(equip.displayName).tag(equip)
                        }
                    }

                    Picker("Type", selection: $exerciseType) {
                        ForEach(ExerciseType.allCases, id: \.self) { type in
                            Text(type.rawValue.capitalized).tag(type)
                        }
                    }

                    Picker("Difficulty", selection: $difficulty) {
                        ForEach(Difficulty.allCases, id: \.self) { diff in
                            Text(diff.rawValue.capitalized).tag(diff)
                        }
                    }
                }
            }
            .navigationTitle("New Exercise")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        let exercise = Exercise(
                            name: name.trimmingCharacters(in: .whitespaces),
                            exerciseDescription: description.trimmingCharacters(in: .whitespaces),
                            muscleGroup: muscleGroup,
                            equipment: equipment,
                            exerciseType: exerciseType,
                            difficulty: difficulty,
                            isCustom: true
                        )
                        onSave(exercise)
                        dismiss()
                    }
                    .disabled(!isValid)
                    .fontWeight(.bold)
                }
            }
        }
    }
}
