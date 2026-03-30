import SwiftUI
import SwiftData

struct ActiveWorkoutScreen: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @State private var viewModel = ActiveGymWorkoutViewModel()
    @State private var showFinishDialog = false
    @State private var showCancelDialog = false
    @State private var showExercisePicker = false
    @State private var expandedExerciseIndex: Int?

    let templateId: UUID?

    var body: some View {
        ZStack(alignment: .bottom) {
            VStack(spacing: 0) {
                if viewModel.isLoading {
                    Spacer()
                    ProgressView("Loading workout...")
                    Spacer()
                } else {
                    // Exercise list
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            if let workout = viewModel.workout, workout.exercises.isEmpty {
                                emptyExercisesView
                            } else if let workout = viewModel.workout {
                                ForEach(Array(workout.exercises.enumerated()), id: \.element.id) { index, exercise in
                                    ExerciseCardView(
                                        exercise: exercise,
                                        exerciseIndex: index,
                                        pb: viewModel.exercisePBs[exercise.exerciseId],
                                        lastWorkout: viewModel.exerciseLastWorkouts[exercise.exerciseId],
                                        isExpanded: expandedExerciseIndex == index,
                                        onToggleExpand: {
                                            withAnimation(.spring(response: 0.3)) {
                                                expandedExerciseIndex = expandedExerciseIndex == index ? nil : index
                                            }
                                        },
                                        onAddSet: { viewModel.addSet(exerciseIndex: index) },
                                        onRemoveSet: { setIndex in viewModel.removeSet(exerciseIndex: index, setIndex: setIndex) },
                                        onUpdateSet: { setIndex, weight, reps in viewModel.updateSet(exerciseIndex: index, setIndex: setIndex, weight: weight, reps: reps) },
                                        onCompleteSet: { setIndex in viewModel.completeSet(exerciseIndex: index, setIndex: setIndex) },
                                        onRemoveExercise: { viewModel.removeExercise(at: index) }
                                    )
                                }
                            }

                            // Add exercise button
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

                            // Spacer for rest timer bar
                            Color.clear.frame(height: 100)
                        }
                        .padding(16)
                    }
                }
            }

            // Rest timer overlay
            if viewModel.isRestTimerActive {
                restTimerBar
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .animation(.spring(response: 0.4, dampingFraction: 0.7), value: viewModel.isRestTimerActive)
            }
        }
        .navigationTitle(viewModel.workout?.name ?? "Workout")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button {
                    showCancelDialog = true
                } label: {
                    Image(systemName: "xmark")
                        .fontWeight(.semibold)
                }
            }

            ToolbarItem(placement: .principal) {
                VStack(spacing: 0) {
                    Text(viewModel.workout?.name ?? "Workout")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    Text(viewModel.elapsedFormatted)
                        .font(.caption)
                        .foregroundStyle(AppTheme.primary)
                        .monospacedDigit()
                }
            }

            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    showFinishDialog = true
                } label: {
                    Text("Finish")
                        .fontWeight(.bold)
                        .foregroundStyle(AppTheme.primary)
                }
            }
        }
        .alert("Finish Workout?", isPresented: $showFinishDialog) {
            Button("Finish") {
                viewModel.finishWorkout()
            }
            Button("Continue", role: .cancel) {}
        } message: {
            Text("You've completed \(viewModel.completedSetsCount) sets. Save this workout?")
        }
        .alert("Cancel Workout?", isPresented: $showCancelDialog) {
            Button("Discard", role: .destructive) {
                viewModel.cancelWorkout()
            }
            Button("Keep Going", role: .cancel) {}
        } message: {
            Text("This workout will be discarded and not saved.")
        }
        .sheet(isPresented: $showExercisePicker) {
            ExercisePickerScreen(mode: .pick) { exercise in
                viewModel.addExercise(exercise)
                showExercisePicker = false
            }
        }
        .onChange(of: viewModel.isFinished) { _, finished in
            if finished {
                dismiss()
            }
        }
        .onAppear {
            viewModel.configure(modelContext: modelContext)
            if let templateId {
                viewModel.createWorkoutFromTemplate(templateId: templateId)
            } else {
                viewModel.createBlankWorkout()
            }
        }
    }

    // MARK: - Empty State

    private var emptyExercisesView: some View {
        VStack(spacing: 16) {
            Image(systemName: "dumbbell.fill")
                .font(.system(size: 40))
                .foregroundStyle(.secondary.opacity(0.5))
            Text("No exercises added")
                .font(.headline)
            Text("Add exercises to start your workout")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Button {
                showExercisePicker = true
            } label: {
                Label("Add Exercise", systemImage: "plus")
                    .fontWeight(.semibold)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(AppTheme.primary)
                    .foregroundStyle(.white)
                    .clipShape(Capsule())
            }
        }
        .frame(maxWidth: .infinity)
        .padding(32)
    }

    // MARK: - Rest Timer Bar

    private var restTimerBar: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("Rest Timer")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(viewModel.restTimeFormatted)
                    .font(.title)
                    .fontWeight(.bold)
                    .monospacedDigit()
                    .foregroundStyle(AppTheme.primary)
            }
            Spacer()

            HStack(spacing: 12) {
                Button {
                    viewModel.addRestTime(30)
                } label: {
                    Text("+30s")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(.ultraThinMaterial)
                        .clipShape(Capsule())
                }

                Button {
                    viewModel.skipRestTimer()
                } label: {
                    Text("Skip")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 10)
                        .background(AppTheme.primary)
                        .foregroundStyle(.white)
                        .clipShape(Capsule())
                }
            }
        }
        .padding(16)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.1), radius: 10, y: -4)
        .padding(.horizontal, 16)
        .padding(.bottom, 8)
    }
}

// MARK: - Exercise Card

struct ExerciseCardView: View {
    let exercise: WorkoutExercise
    let exerciseIndex: Int
    let pb: ExercisePBInfo?
    let lastWorkout: LastWorkoutInfo?
    let isExpanded: Bool
    let onToggleExpand: () -> Void
    let onAddSet: () -> Void
    let onRemoveSet: (Int) -> Void
    let onUpdateSet: (Int, Double?, Int?) -> Void
    let onCompleteSet: (Int) -> Void
    let onRemoveExercise: () -> Void

    @State private var showDeleteConfirmation = false

    var body: some View {
        VStack(spacing: 0) {
            // Header
            Button(action: onToggleExpand) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(exercise.exerciseName)
                            .font(.headline)
                            .foregroundStyle(.primary)
                        Text("\(exercise.sets.filter(\.isCompleted).count)/\(exercise.sets.count) sets")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(16)

            // PB and last workout badges
            if pb != nil || lastWorkout != nil {
                HStack(spacing: 8) {
                    if let pb {
                        HStack(spacing: 4) {
                            Image(systemName: "trophy.fill")
                                .font(.caption2)
                                .foregroundStyle(Color(hex: "FFD700"))
                            Text("PB: \(pb.bestWeightFormatted)")
                                .font(.caption2)
                                .fontWeight(.medium)
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color(hex: "FFD700").opacity(0.1))
                        .clipShape(Capsule())
                    }

                    if let last = lastWorkout {
                        HStack(spacing: 4) {
                            Image(systemName: "clock")
                                .font(.caption2)
                            Text("Last: \(last.bestWeight, specifier: "%.0f")kg x \(last.bestReps)")
                                .font(.caption2)
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(.quaternary)
                        .clipShape(Capsule())
                    }

                    if let pb, pb.estimatedOneRepMax > 0 {
                        HStack(spacing: 4) {
                            Image(systemName: "dumbbell.fill")
                                .font(.caption2)
                            Text("1RM: \(pb.oneRepMaxFormatted)")
                                .font(.caption2)
                                .fontWeight(.medium)
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(AppTheme.primary.opacity(0.1))
                        .clipShape(Capsule())
                    }

                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
            }

            if isExpanded {
                Divider().padding(.horizontal, 16)

                // Set headers
                HStack {
                    Text("SET")
                        .frame(width: 36)
                    Text("PREV")
                        .frame(width: 64)
                    Text("KG")
                        .frame(width: 64)
                    Text("REPS")
                        .frame(width: 52)
                    Spacer()
                        .frame(width: 44)
                }
                .font(.caption2)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 16)
                .padding(.top, 12)

                // Sets
                ForEach(Array(exercise.sets.enumerated()), id: \.element.id) { setIndex, set in
                    SetRowView(
                        set: set,
                        setIndex: setIndex,
                        previousWeight: lastWorkout?.bestWeight,
                        previousReps: lastWorkout?.bestReps,
                        oneRepMax: pb?.estimatedOneRepMax,
                        onWeightChange: { weight in onUpdateSet(setIndex, weight, nil) },
                        onRepsChange: { reps in onUpdateSet(setIndex, nil, reps) },
                        onComplete: { onCompleteSet(setIndex) }
                    )
                }

                // Add/remove set buttons
                HStack {
                    Button(action: onAddSet) {
                        HStack(spacing: 4) {
                            Image(systemName: "plus")
                            Text("Add Set")
                        }
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundStyle(AppTheme.primary)
                    }

                    Spacer()

                    Button(role: .destructive) {
                        showDeleteConfirmation = true
                    } label: {
                        Image(systemName: "trash")
                            .font(.caption)
                            .foregroundStyle(.red.opacity(0.7))
                    }
                }
                .padding(16)
            }
        }
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .alert("Remove Exercise?", isPresented: $showDeleteConfirmation) {
            Button("Remove", role: .destructive, action: onRemoveExercise)
            Button("Cancel", role: .cancel) {}
        }
    }
}

// MARK: - Set Row

struct SetRowView: View {
    let set: WorkoutSet
    let setIndex: Int
    let previousWeight: Double?
    let previousReps: Int?
    let oneRepMax: Double?
    let onWeightChange: (Double) -> Void
    let onRepsChange: (Int) -> Void
    let onComplete: () -> Void

    @State private var weightText: String = ""
    @State private var repsText: String = ""

    var body: some View {
        let backgroundColor = set.isCompleted ? AppTheme.primary.opacity(0.08) : Color.clear
        let oneRmPercent = ActiveGymWorkoutViewModel.oneRepMaxPercentage(weight: set.weight, oneRepMax: oneRepMax ?? 0)

        HStack {
            // Set number
            Circle()
                .fill(set.isCompleted ? AppTheme.primary : Color.gray.opacity(0.2))
                .frame(width: 28, height: 28)
                .overlay {
                    Text("\(setIndex + 1)")
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundStyle(set.isCompleted ? .white : .secondary)
                }
                .frame(width: 36)

            // Previous
            Text(previousWeight != nil && previousReps != nil
                 ? "\(previousWeight!, specifier: "%.0f")x\(previousReps!)"
                 : "-")
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(width: 64)

            // Weight
            VStack(spacing: 0) {
                TextField("0", text: $weightText)
                    .keyboardType(.decimalPad)
                    .multilineTextAlignment(.center)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .frame(width: 64)
                    .disabled(set.isCompleted)
                    .onChange(of: weightText) { _, newValue in
                        if let w = Double(newValue) { onWeightChange(w) }
                    }
                if let pct = oneRmPercent {
                    Text("\(pct)% 1RM")
                        .font(.system(size: 8))
                        .foregroundStyle(AppTheme.primary.opacity(0.7))
                }
            }

            // Reps
            TextField("0", text: $repsText)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.center)
                .font(.subheadline)
                .fontWeight(.semibold)
                .frame(width: 52)
                .disabled(set.isCompleted)
                .onChange(of: repsText) { _, newValue in
                    if let r = Int(newValue) { onRepsChange(r) }
                }

            // Complete button
            Button(action: onComplete) {
                Image(systemName: set.isCompleted ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundStyle(set.isCompleted ? AppTheme.primary : .secondary)
            }
            .disabled(set.isCompleted || weightText.isEmpty || repsText.isEmpty)
            .frame(width: 44)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 6)
        .background(backgroundColor)
        .onAppear {
            weightText = set.weight > 0 ? "\(set.weight)" : ""
            repsText = set.reps > 0 ? "\(set.reps)" : ""
        }
    }
}
