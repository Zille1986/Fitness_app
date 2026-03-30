import SwiftUI
import SwiftData

struct RunningDashboardScreen: View {
    @Environment(\.modelContext) private var modelContext
    @State private var viewModel = RunningViewModel()
    @State private var showStartRunSheet = false
    @State private var showManualEntry = false
    @State private var navigateToActiveRun = false
    @State private var selectedRunId: UUID?

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 16) {
                    // Hero banner
                    DashboardHeroBanner(
                        icon: "figure.run",
                        title: "Running",
                        subtitle: "Track your runs and chase your goals"
                    )

                    // Weekly stats card
                    weeklyStatsCard

                    // Next scheduled run
                    nextRunCard

                    // Personal bests
                    if !viewModel.personalBests.isEmpty {
                        personalBestsSection
                    }

                    // Recent runs
                    recentRunsSection
                }
                .padding(.vertical)
            }
            .background(AppTheme.surface)
            .navigationTitle("Running")
            .refreshable {
                viewModel.refresh()
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showManualEntry = true } label: {
                        Image(systemName: "square.and.pencil")
                    }
                }
            }
            .overlay(alignment: .bottomTrailing) {
                Button {
                    showStartRunSheet = true
                } label: {
                    Label("Start Run", systemImage: "play.fill")
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
            .sheet(isPresented: $showStartRunSheet) {
                StartRunSheet(onStartRun: {
                    showStartRunSheet = false
                    navigateToActiveRun = true
                })
            }
            .sheet(isPresented: $showManualEntry) {
                ManualRunEntrySheet(viewModel: viewModel)
            }
            .navigationDestination(isPresented: $navigateToActiveRun) {
                ActiveRunScreen()
            }
            .navigationDestination(item: $selectedRunId) { runId in
                RunDetailScreen(runId: runId)
            }
            .onAppear {
                viewModel.configure(modelContext: modelContext)
            }
        }
    }

    // MARK: - Weekly Stats

    private var weeklyStatsCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("WEEKLY DISTANCE")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.running)

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(format: "%.1f km", viewModel.totalWeeklyDistance))
                        .font(.title)
                        .fontWeight(.bold)
                        .foregroundStyle(AppTheme.running)
                    Text("Total this week")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(viewModel.averagePace)
                        .font(.title)
                        .fontWeight(.bold)
                    Text("Avg pace /km")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            // Bar chart
            WeeklyBarChart(
                data: viewModel.weeklyDistances,
                labels: ["M", "T", "W", "T", "F", "S", "S"],
                accentColor: AppTheme.running
            )
            .frame(height: 80)
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - Next Run

    private var nextRunCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("NEXT RUN")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundStyle(AppTheme.running)
                Spacer()
                if let next = viewModel.nextScheduledRun {
                    Text(next.scheduledDate)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            if let next = viewModel.nextScheduledRun {
                VStack(alignment: .leading, spacing: 4) {
                    Text(next.name)
                        .font(.title3)
                        .fontWeight(.bold)
                    Text(next.description)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    if !next.estimatedDuration.isEmpty {
                        HStack(spacing: 4) {
                            Image(systemName: "timer")
                                .font(.caption)
                            Text(next.estimatedDuration)
                                .font(.caption)
                        }
                        .foregroundStyle(.secondary)
                    }
                }
            } else {
                Text("No runs scheduled. Create a training plan!")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                NavigationLink(destination: TrainingPlanScreen()) {
                    Text("Browse Plans")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundStyle(AppTheme.running)
                }
            }
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

    // MARK: - Personal Bests

    private var personalBestsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("PERSONAL BESTS")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)
                .padding(.horizontal)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(viewModel.personalBests) { pb in
                        VStack(spacing: 8) {
                            Image(systemName: pb.icon)
                                .font(.title3)
                                .foregroundStyle(Color(hex: "FFD700"))

                            Text(pb.value)
                                .font(.headline)
                                .fontWeight(.bold)

                            Text(pb.category)
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                        .frame(width: 100)
                        .padding(.vertical, 12)
                        .background(Color(hex: "FFD700").opacity(0.08))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
                .padding(.horizontal)
            }
        }
    }

    // MARK: - Recent Runs

    private var recentRunsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("RECENT RUNS")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)
                .padding(.horizontal)

            if viewModel.recentRuns.isEmpty {
                EmptyStateView(
                    icon: "figure.run",
                    title: "No runs yet",
                    message: "Start your first run to see it here"
                )
                .padding(.horizontal)
            } else {
                ForEach(viewModel.recentRuns) { run in
                    Button {
                        selectedRunId = run.id
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(run.date)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                Text(run.title)
                                    .font(.body)
                                    .fontWeight(.medium)
                                    .foregroundStyle(AppTheme.onSurface)
                            }
                            Spacer()
                            HStack(spacing: 16) {
                                VStack(alignment: .trailing, spacing: 2) {
                                    Text(run.distanceFormatted)
                                        .font(.body)
                                        .fontWeight(.bold)
                                        .foregroundStyle(AppTheme.onSurface)
                                    Text("km")
                                        .font(.caption2)
                                        .foregroundStyle(.secondary)
                                }
                                VStack(alignment: .trailing, spacing: 2) {
                                    Text(run.paceFormatted)
                                        .font(.body)
                                        .fontWeight(.bold)
                                        .foregroundStyle(AppTheme.onSurface)
                                    Text("/km")
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
                    }
                    .padding(.horizontal)
                }
            }
        }
    }
}

// MARK: - Start Run Sheet

struct StartRunSheet: View {
    @Environment(\.dismiss) private var dismiss
    let onStartRun: () -> Void

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Button {
                        dismiss()
                        onStartRun()
                    } label: {
                        HStack(spacing: 16) {
                            Image(systemName: "play.circle.fill")
                                .font(.title)
                                .foregroundStyle(AppTheme.primary)
                            VStack(alignment: .leading) {
                                Text("Free Run").fontWeight(.bold)
                                Text("Track distance, time and heart rate")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }

                Section("Workout Templates") {
                    ForEach(RunTemplate.allTemplates) { template in
                        Button {
                            dismiss()
                            onStartRun()
                        } label: {
                            HStack {
                                Image(systemName: template.icon)
                                    .foregroundStyle(template.difficultyColor)
                                    .frame(width: 32, height: 32)
                                    .background(template.difficultyColor.opacity(0.15))
                                    .clipShape(RoundedRectangle(cornerRadius: 8))

                                VStack(alignment: .leading) {
                                    Text(template.name).fontWeight(.semibold)
                                    Text(template.subtitle)
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
            .navigationTitle("Start a Run")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
        .presentationDetents([.large])
    }
}

// MARK: - Manual Run Entry Sheet

struct ManualRunEntrySheet: View {
    @Environment(\.dismiss) private var dismiss
    let viewModel: RunningViewModel

    @State private var distance = ""
    @State private var hours = ""
    @State private var minutes = ""
    @State private var seconds = ""
    @State private var notes = ""

    private var isValid: Bool {
        guard let d = Double(distance), d > 0 else { return false }
        let h = Int(hours) ?? 0
        let m = Int(minutes) ?? 0
        let s = Int(seconds) ?? 0
        return h + m + s > 0
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Distance (km)") {
                    TextField("e.g. 5.0", text: $distance)
                        .keyboardType(.decimalPad)
                }

                Section("Duration") {
                    HStack(spacing: 12) {
                        VStack {
                            Text("Hrs").font(.caption2).foregroundStyle(.secondary)
                            TextField("0", text: $hours)
                                .keyboardType(.numberPad)
                                .multilineTextAlignment(.center)
                        }
                        VStack {
                            Text("Min").font(.caption2).foregroundStyle(.secondary)
                            TextField("0", text: $minutes)
                                .keyboardType(.numberPad)
                                .multilineTextAlignment(.center)
                        }
                        VStack {
                            Text("Sec").font(.caption2).foregroundStyle(.secondary)
                            TextField("0", text: $seconds)
                                .keyboardType(.numberPad)
                                .multilineTextAlignment(.center)
                        }
                    }
                }

                Section("Notes (optional)") {
                    TextField("How did it feel?", text: $notes, axis: .vertical)
                        .lineLimit(3)
                }

                Section {
                    Button {
                        save()
                        dismiss()
                    } label: {
                        Text("Save Run")
                            .frame(maxWidth: .infinity)
                            .fontWeight(.bold)
                    }
                    .disabled(!isValid)
                }
            }
            .navigationTitle("Log Run")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    private func save() {
        guard let dist = Double(distance) else { return }
        let h = Int(hours) ?? 0
        let m = Int(minutes) ?? 0
        let s = Int(seconds) ?? 0
        let totalMillis = Int64((h * 3600 + m * 60 + s) * 1000)
        let distanceMeters = dist * 1000

        viewModel.saveManualRun(
            distanceMeters: distanceMeters,
            durationMillis: totalMillis,
            notes: notes.isEmpty ? nil : notes
        )
    }
}

// MARK: - Weekly Bar Chart

struct WeeklyBarChart: View {
    let data: [Float]
    let labels: [String]
    let accentColor: Color

    var body: some View {
        let maxValue = max(data.max() ?? 1, 0.1)

        HStack(alignment: .bottom, spacing: 8) {
            ForEach(Array(data.enumerated()), id: \.offset) { index, value in
                VStack(spacing: 4) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(value > 0 ? accentColor : accentColor.opacity(0.15))
                        .frame(height: max(4, CGFloat(value / maxValue) * 60))
                        .animation(.spring(response: 0.6, dampingFraction: 0.7), value: value)

                    Text(index < labels.count ? labels[index] : "")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity)
            }
        }
    }
}

// MARK: - Models

struct RunTemplate: Identifiable {
    let id = UUID()
    let name: String
    let subtitle: String
    let icon: String
    let difficultyColor: Color

    static let allTemplates: [RunTemplate] = [
        RunTemplate(name: "400m Repeats", subtitle: "Speed \u{00B7} 30 min", icon: "bolt.fill", difficultyColor: .orange),
        RunTemplate(name: "800m Repeats", subtitle: "Speed \u{00B7} 35 min", icon: "bolt.fill", difficultyColor: .red),
        RunTemplate(name: "Tempo Run", subtitle: "Endurance \u{00B7} 40 min", icon: "chart.line.uptrend.xyaxis", difficultyColor: .orange),
        RunTemplate(name: "Hill Repeats", subtitle: "Strength \u{00B7} 35 min", icon: "mountain.2.fill", difficultyColor: .red),
        RunTemplate(name: "Fartlek", subtitle: "Speed \u{00B7} 30 min", icon: "bolt.fill", difficultyColor: .orange),
        RunTemplate(name: "Recovery Run", subtitle: "Recovery \u{00B7} 25 min", icon: "leaf.fill", difficultyColor: .green),
        RunTemplate(name: "Long Slow Distance", subtitle: "Endurance \u{00B7} 60 min", icon: "chart.line.uptrend.xyaxis", difficultyColor: .orange),
    ]
}
