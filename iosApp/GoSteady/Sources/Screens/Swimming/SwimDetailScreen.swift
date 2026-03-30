import SwiftUI

struct SwimDetailScreen: View {
    let workout: SwimmingWorkoutSummary

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header card
                headerCard

                // Primary stats
                primaryStatsGrid

                // Pace chart
                if !workout.splits.isEmpty {
                    paceChartSection
                }

                // Stroke distribution
                strokeDistributionSection

                // Laps breakdown
                if !workout.splits.isEmpty {
                    lapsBreakdownSection
                }

                // Notes
                if let notes = workout.notes, !notes.isEmpty {
                    notesSection(notes)
                }
            }
            .padding()
        }
        .background(AppTheme.surface)
        .navigationTitle("Swim Details")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Header Card

    private var headerCard: some View {
        VStack(spacing: 12) {
            HStack {
                Image(systemName: workout.swimType.icon)
                    .font(.title2)
                    .foregroundStyle(AppTheme.swimming)
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(workout.swimType.displayName) Swim")
                        .font(.title3)
                        .fontWeight(.bold)
                    Text(workout.fullDateFormatted)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                if let poolLength = workout.poolLength {
                    Text(poolLength.displayName)
                        .font(.caption)
                        .fontWeight(.medium)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(AppTheme.swimming.opacity(0.15))
                        .clipShape(Capsule())
                }
            }

            // Big distance
            Text(workout.distanceFormatted)
                .font(.system(size: 48, weight: .bold, design: .rounded))
                .foregroundStyle(AppTheme.swimming)

            Text(workout.durationFormatted)
                .font(.title3)
                .foregroundStyle(.secondary)
        }
        .padding(20)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    // MARK: - Primary Stats

    private var primaryStatsGrid: some View {
        LazyVGrid(columns: [
            GridItem(.flexible()),
            GridItem(.flexible()),
            GridItem(.flexible())
        ], spacing: 16) {
            detailStatCard(
                icon: "speedometer",
                value: workout.avgPaceFormatted,
                unit: "/100m",
                label: "Avg Pace",
                color: AppTheme.swimming
            )
            detailStatCard(
                icon: "bolt.fill",
                value: SwimmingViewModel.formatPace(workout.bestPaceSecondsPer100m),
                unit: "/100m",
                label: "Best Pace",
                color: .orange
            )
            detailStatCard(
                icon: "repeat",
                value: "\(workout.laps)",
                unit: "",
                label: "Laps",
                color: .blue
            )
            detailStatCard(
                icon: "flame.fill",
                value: "\(workout.caloriesBurned)",
                unit: "cal",
                label: "Calories",
                color: .red
            )
            detailStatCard(
                icon: "figure.pool.swim",
                value: workout.strokeType.displayName,
                unit: "",
                label: "Stroke",
                color: AppTheme.swimming
            )
            detailStatCard(
                icon: "chart.bar.fill",
                value: "\(workout.splits.count)",
                unit: "",
                label: "Splits",
                color: .purple
            )
        }
    }

    private func detailStatCard(icon: String, value: String, unit: String, label: String, color: Color) -> some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundStyle(color)
            HStack(alignment: .lastTextBaseline, spacing: 1) {
                Text(value)
                    .font(.subheadline)
                    .fontWeight(.bold)
                if !unit.isEmpty {
                    Text(unit)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Pace Chart

    private var paceChartSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("PACE PER LAP")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.onSurfaceVariant)

            let maxPace = workout.splits.map(\.paceSecondsPer100m).max() ?? 1
            let minPace = workout.splits.map(\.paceSecondsPer100m).filter { $0 > 0 }.min() ?? 0

            HStack(alignment: .bottom, spacing: 3) {
                ForEach(workout.splits) { split in
                    let normalized = maxPace > minPace
                        ? (split.paceSecondsPer100m - minPace) / (maxPace - minPace)
                        : 0.5
                    let barHeight = 20.0 + (1.0 - normalized) * 80.0

                    VStack(spacing: 2) {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(paceBarColor(pace: split.paceSecondsPer100m, best: minPace, worst: maxPace))
                            .frame(height: barHeight)

                        if workout.splits.count <= 20 {
                            Text("\(split.lapNumber)")
                                .font(.system(size: 7))
                                .foregroundStyle(.tertiary)
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .frame(height: 120)

            // Legend
            HStack(spacing: 16) {
                HStack(spacing: 4) {
                    Circle().fill(Color.green).frame(width: 6, height: 6)
                    Text("Fast")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                HStack(spacing: 4) {
                    Circle().fill(AppTheme.swimming).frame(width: 6, height: 6)
                    Text("Average")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                HStack(spacing: 4) {
                    Circle().fill(Color.orange).frame(width: 6, height: 6)
                    Text("Slow")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func paceBarColor(pace: Double, best: Double, worst: Double) -> Color {
        guard worst > best else { return AppTheme.swimming }
        let ratio = (pace - best) / (worst - best)
        if ratio < 0.33 { return .green }
        if ratio < 0.66 { return AppTheme.swimming }
        return .orange
    }

    // MARK: - Stroke Distribution

    private var strokeDistributionSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("STROKE DISTRIBUTION")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.onSurfaceVariant)

            if workout.splits.isEmpty {
                // Single stroke type for the whole workout
                HStack {
                    Text(workout.strokeType.displayName)
                        .font(.subheadline)
                        .fontWeight(.medium)
                    Spacer()
                    Text("100%")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundStyle(AppTheme.swimming)
                }
            } else {
                // Calculate from splits
                let strokeCounts = Dictionary(grouping: workout.splits, by: \.strokeType)
                    .mapValues { $0.count }
                let total = workout.splits.count

                ForEach(strokeCounts.sorted(by: { $0.value > $1.value }), id: \.key) { stroke, count in
                    HStack {
                        Text(stroke.displayName)
                            .font(.subheadline)
                        Spacer()
                        ProgressView(value: Double(count), total: Double(total))
                            .tint(AppTheme.swimming)
                            .frame(width: 80)
                        Text(String(format: "%.0f%%", Double(count) / Double(total) * 100))
                            .font(.caption)
                            .fontWeight(.semibold)
                            .frame(width: 40, alignment: .trailing)
                    }
                }
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Laps Breakdown

    private var lapsBreakdownSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("LAPS BREAKDOWN")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.onSurfaceVariant)

            // Header
            HStack {
                Text("Lap")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .frame(width: 40, alignment: .leading)
                Spacer()
                Text("Distance")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .frame(width: 60)
                Spacer()
                Text("Time")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .frame(width: 50)
                Spacer()
                Text("Pace")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .frame(width: 55, alignment: .trailing)
            }
            .foregroundStyle(.secondary)

            Divider()

            ForEach(workout.splits) { split in
                HStack {
                    Text("#\(split.lapNumber)")
                        .font(.caption)
                        .frame(width: 40, alignment: .leading)
                    Spacer()
                    Text(String(format: "%.0fm", split.distanceMeters))
                        .font(.caption)
                        .frame(width: 60)
                    Spacer()
                    Text(split.durationFormatted)
                        .font(.caption)
                        .frame(width: 50)
                    Spacer()
                    Text(split.paceFormatted)
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundStyle(AppTheme.swimming)
                        .frame(width: 55, alignment: .trailing)
                }
                .padding(.vertical, 2)
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Notes

    private func notesSection(_ notes: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("NOTES")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.onSurfaceVariant)
            Text(notes)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
