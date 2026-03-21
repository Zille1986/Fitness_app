import SwiftUI
import HealthKit

struct RunningDashboardScreen: View {
    @State private var showStartRunSheet = false
    @State private var showManualEntry = false
    @State private var recentRuns: [RunSummary] = []

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // Hero banner
                    DashboardHeroBanner(
                        title: "Running",
                        subtitle: "Track your runs and chase your goals",
                        color: AppTheme.running
                    )

                    // Recent Runs
                    VStack(alignment: .leading, spacing: 12) {
                        Text("RECENT RUNS")
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundStyle(AppTheme.onSurfaceVariant)

                        if recentRuns.isEmpty {
                            EmptyStateView(
                                icon: "figure.run",
                                title: "No runs yet",
                                subtitle: "Start your first run to see it here"
                            )
                        }
                    }
                    .padding(.horizontal)
                }
                .padding(.vertical)
            }
            .background(AppTheme.surface)
            .navigationTitle("Running")
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
                StartRunSheet()
            }
            .sheet(isPresented: $showManualEntry) {
                ManualWorkoutSheet(sportType: .run)
            }
        }
    }
}

struct StartRunSheet: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Button {
                        dismiss()
                        // TODO: Start free run tracking
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
                            // TODO: Start template run
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

// MARK: - Models

struct RunSummary: Identifiable {
    let id: Int64
    let title: String
    let date: Date
    let distanceKm: Double
    let durationFormatted: String
    let paceFormatted: String
}

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
