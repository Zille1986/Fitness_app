import SwiftUI
import SwiftData

struct ExerciseDetailScreen: View {
    @Environment(\.modelContext) private var modelContext
    let exerciseId: UUID

    @State private var exercise: Exercise?
    @State private var history: [ExerciseHistory] = []
    @State private var personalBest: PersonalBest?
    @State private var selectedChartMetric: ChartMetric = .weight

    enum ChartMetric: String, CaseIterable {
        case weight = "Weight"
        case volume = "Volume"
        case oneRepMax = "1RM"
    }

    var body: some View {
        Group {
            if let exercise {
                ScrollView {
                    LazyVStack(spacing: 16) {
                        // Exercise info header
                        exerciseInfoHeader(exercise: exercise)

                        // Personal records
                        if let pb = personalBest {
                            personalRecordsSection(pb: pb)
                        }

                        // Progress chart
                        if !history.isEmpty {
                            progressChartSection
                        }

                        // Form tips
                        if !exercise.instructions.isEmpty || !exercise.tips.isEmpty {
                            formCuesSection(exercise: exercise)
                        }

                        // History
                        historySection
                    }
                    .padding(.vertical)
                }
                .background(AppTheme.surface)
                .navigationTitle(exercise.name)
                .navigationBarTitleDisplayMode(.inline)
            } else {
                VStack(spacing: 16) {
                    ProgressView()
                    Text("Loading...")
                        .foregroundStyle(.secondary)
                }
            }
        }
        .onAppear {
            loadData()
        }
    }

    // MARK: - Exercise Info

    private func exerciseInfoHeader(exercise: Exercise) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                // Muscle group icon
                VStack(spacing: 4) {
                    Image(systemName: muscleGroupIcon(exercise.muscleGroup))
                        .font(.title2)
                        .foregroundStyle(AppTheme.gym)
                }
                .frame(width: 56, height: 56)
                .background(AppTheme.gym.opacity(0.15))
                .clipShape(RoundedRectangle(cornerRadius: 12))

                VStack(alignment: .leading, spacing: 4) {
                    Text(exercise.name)
                        .font(.title3)
                        .fontWeight(.bold)

                    HStack(spacing: 8) {
                        infoTag(exercise.muscleGroup.displayName)
                        infoTag(exercise.equipment.displayName)
                        infoTag(exercise.exerciseType.rawValue.capitalized)
                    }
                }
            }

            if !exercise.exerciseDescription.isEmpty {
                Text(exercise.exerciseDescription)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            // Secondary muscle groups
            if !exercise.secondaryMuscleGroups.isEmpty {
                HStack(spacing: 4) {
                    Text("Also works:")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(exercise.secondaryMuscleGroups.map(\.displayName).joined(separator: ", "))
                        .font(.caption)
                        .fontWeight(.medium)
                }
            }

            // Difficulty
            HStack(spacing: 4) {
                Text("Difficulty:")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                ForEach(0..<4, id: \.self) { i in
                    Circle()
                        .fill(i < difficultyLevel(exercise.difficulty) ? AppTheme.gym : Color.gray.opacity(0.3))
                        .frame(width: 8, height: 8)
                }
                Text(exercise.difficulty.rawValue.capitalized)
                    .font(.caption)
                    .fontWeight(.medium)
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    private func infoTag(_ text: String) -> some View {
        Text(text)
            .font(.caption2)
            .fontWeight(.medium)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(Color.gray.opacity(0.3))
            .clipShape(Capsule())
    }

    // MARK: - Personal Records

    private func personalRecordsSection(pb: PersonalBest) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("PERSONAL RECORDS")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            HStack(spacing: 16) {
                recordTile(
                    icon: "trophy.fill",
                    iconColor: Color(hex: "FFD700"),
                    title: "Best Weight",
                    value: String(format: "%.1f kg", pb.bestWeight),
                    subtitle: "\(pb.bestReps) reps"
                )

                recordTile(
                    icon: "dumbbell.fill",
                    iconColor: AppTheme.gym,
                    title: "Est. 1RM",
                    value: String(format: "%.1f kg", pb.estimatedOneRepMax),
                    subtitle: "Epley formula"
                )
            }

            Text("Achieved: \(pb.achievedDate, format: .dateTime.month().day().year())")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    private func recordTile(icon: String, iconColor: Color, title: String, value: String, subtitle: String) -> some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(iconColor)

            Text(value)
                .font(.headline)
                .fontWeight(.bold)

            VStack(spacing: 2) {
                Text(title)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                Text(subtitle)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(iconColor.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Progress Chart

    private var progressChartSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("PROGRESS")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundStyle(.secondary)
                Spacer()
                Picker("Metric", selection: $selectedChartMetric) {
                    ForEach(ChartMetric.allCases, id: \.self) { metric in
                        Text(metric.rawValue).tag(metric)
                    }
                }
                .pickerStyle(.segmented)
                .frame(width: 200)
            }

            let values: [Double] = history.sorted(by: { $0.date < $1.date }).map { entry in
                switch selectedChartMetric {
                case .weight: return entry.bestWeight
                case .volume: return entry.totalVolume
                case .oneRepMax: return entry.estimatedOneRepMax
                }
            }

            if !values.isEmpty {
                let maxVal = max(values.max() ?? 1, 1)
                let minVal = max(values.min() ?? 0, 0)
                let range = max(maxVal - minVal, 1)

                // Simple line chart
                GeometryReader { geometry in
                    let width = geometry.size.width
                    let height = geometry.size.height
                    let stepX = width / CGFloat(max(values.count - 1, 1))

                    Path { path in
                        for (index, value) in values.enumerated() {
                            let x = CGFloat(index) * stepX
                            let normalizedY = (value - minVal) / range
                            let y = height - (CGFloat(normalizedY) * height * 0.8 + height * 0.1)

                            if index == 0 {
                                path.move(to: CGPoint(x: x, y: y))
                            } else {
                                path.addLine(to: CGPoint(x: x, y: y))
                            }
                        }
                    }
                    .stroke(AppTheme.gym, lineWidth: 2.5)

                    // Data points
                    ForEach(Array(values.enumerated()), id: \.offset) { index, value in
                        let x = CGFloat(index) * stepX
                        let normalizedY = (value - minVal) / range
                        let y = height - (CGFloat(normalizedY) * height * 0.8 + height * 0.1)

                        Circle()
                            .fill(AppTheme.gym)
                            .frame(width: 6, height: 6)
                            .position(x: x, y: y)
                    }
                }
                .frame(height: 120)

                // Min/max labels
                HStack {
                    Text(formatChartValue(values.first ?? 0))
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text(formatChartValue(values.last ?? 0))
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundStyle(AppTheme.gym)
                }
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - Form Cues

    private func formCuesSection(exercise: Exercise) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("FORM GUIDE")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            if !exercise.instructions.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Instructions")
                        .font(.subheadline)
                        .fontWeight(.semibold)

                    ForEach(Array(exercise.instructions.enumerated()), id: \.offset) { index, instruction in
                        HStack(alignment: .top, spacing: 8) {
                            Text("\(index + 1)")
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundStyle(.white)
                                .frame(width: 20, height: 20)
                                .background(AppTheme.gym)
                                .clipShape(Circle())

                            Text(instruction)
                                .font(.subheadline)
                        }
                    }
                }
            }

            if !exercise.tips.isEmpty {
                if !exercise.instructions.isEmpty {
                    Divider()
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("Tips")
                        .font(.subheadline)
                        .fontWeight(.semibold)

                    ForEach(exercise.tips, id: \.self) { tip in
                        HStack(alignment: .top, spacing: 8) {
                            Image(systemName: "lightbulb.fill")
                                .font(.caption)
                                .foregroundStyle(.yellow)
                            Text(tip)
                                .font(.subheadline)
                        }
                    }
                }
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - History

    private var historySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("HISTORY")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            if history.isEmpty {
                VStack(spacing: 8) {
                    Image(systemName: "clock")
                        .font(.title2)
                        .foregroundStyle(.secondary.opacity(0.5))
                    Text("No history yet")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text("Complete a workout with this exercise to track your progress")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(24)
            } else {
                ForEach(history.sorted(by: { $0.date > $1.date }), id: \.id) { entry in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(entry.date, format: .dateTime.month().day())
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text("\(String(format: "%.1f", entry.bestWeight)) kg x \(entry.bestReps) reps")
                                .fontWeight(.medium)
                        }

                        Spacer()

                        VStack(alignment: .trailing, spacing: 2) {
                            Text("\(entry.totalSets) sets")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text("\(String(format: "%.0f", entry.totalVolume)) kg vol")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .padding(.vertical, 8)

                    if entry.id != history.sorted(by: { $0.date > $1.date }).last?.id {
                        Divider()
                    }
                }
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - Helpers

    private func loadData() {
        let id = exerciseId
        let exDescriptor = FetchDescriptor<Exercise>(
            predicate: #Predicate<Exercise> { e in e.id == id }
        )
        exercise = try? modelContext.fetch(exDescriptor).first

        let histDescriptor = FetchDescriptor<ExerciseHistory>(
            predicate: #Predicate<ExerciseHistory> { h in h.exerciseId == id },
            sortBy: [SortDescriptor(\ExerciseHistory.date, order: .reverse)]
        )
        history = (try? modelContext.fetch(histDescriptor)) ?? []

        let pbDescriptor = FetchDescriptor<PersonalBest>(
            predicate: #Predicate<PersonalBest> { pb in pb.exerciseId == id }
        )
        personalBest = try? modelContext.fetch(pbDescriptor).first
    }

    private func muscleGroupIcon(_ group: MuscleGroup) -> String {
        switch group {
        case .chest: return "figure.strengthtraining.traditional"
        case .back, .lats, .traps: return "figure.rowing"
        case .shoulders: return "figure.highintensity.intervaltraining"
        case .biceps, .triceps, .forearms: return "figure.boxing"
        case .quads, .hamstrings, .glutes, .calves, .hipFlexors: return "figure.walk"
        case .abs, .obliques, .lowerBack: return "figure.core.training"
        case .fullBody: return "figure.cross.training"
        }
    }

    private func difficultyLevel(_ difficulty: Difficulty) -> Int {
        switch difficulty {
        case .beginner: return 1
        case .intermediate: return 2
        case .advanced: return 3
        case .expert: return 4
        }
    }

    private func formatChartValue(_ value: Double) -> String {
        switch selectedChartMetric {
        case .weight: return String(format: "%.0f kg", value)
        case .volume: return String(format: "%.0f kg", value)
        case .oneRepMax: return String(format: "%.0f kg", value)
        }
    }
}
