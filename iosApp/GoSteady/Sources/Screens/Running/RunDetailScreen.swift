import SwiftUI
import SwiftData
import MapKit

struct RunDetailScreen: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    let runId: UUID

    @State private var run: Run?
    @State private var showDeleteConfirmation = false
    @State private var showShareSheet = false

    var body: some View {
        Group {
            if let run {
                ScrollView {
                    LazyVStack(spacing: 16) {
                        // Route map
                        if !run.routePoints.isEmpty {
                            routeMapSection(run: run)
                        }

                        // Main stats
                        mainStatsSection(run: run)

                        // Pace chart
                        if !run.splits.isEmpty {
                            paceChartSection(run: run)
                        }

                        // Splits table
                        if !run.splits.isEmpty {
                            splitsTableSection(run: run)
                        }

                        // Heart rate section
                        if run.avgHeartRate != nil || run.maxHeartRate != nil {
                            heartRateSection(run: run)
                        }

                        // Elevation section
                        if run.elevationGainMeters > 0 || run.elevationLossMeters > 0 {
                            elevationSection(run: run)
                        }

                        // Notes
                        if let notes = run.notes, !notes.isEmpty {
                            notesSection(notes: notes)
                        }

                        // Metadata
                        metadataSection(run: run)
                    }
                    .padding(.vertical)
                }
                .background(AppTheme.surface)
            } else {
                VStack(spacing: 16) {
                    ProgressView()
                    Text("Loading run details...")
                        .foregroundStyle(.secondary)
                }
            }
        }
        .navigationTitle("Run Details")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button(role: .destructive) {
                        showDeleteConfirmation = true
                    } label: {
                        Label("Delete Run", systemImage: "trash")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
        .alert("Delete Run?", isPresented: $showDeleteConfirmation) {
            Button("Delete", role: .destructive) {
                deleteRun()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This action cannot be undone.")
        }
        .onAppear {
            loadRun()
        }
    }

    // MARK: - Route Map

    private func routeMapSection(run: Run) -> some View {
        let coordinates = run.routePoints.map {
            CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude)
        }

        return Map {
            MapPolyline(coordinates: coordinates)
                .stroke(AppTheme.running, lineWidth: 4)

            if let first = coordinates.first {
                Annotation("Start", coordinate: first) {
                    Circle()
                        .fill(.green)
                        .frame(width: 12, height: 12)
                        .overlay { Circle().stroke(.white, lineWidth: 2) }
                }
            }

            if let last = coordinates.last {
                Annotation("Finish", coordinate: last) {
                    Circle()
                        .fill(.red)
                        .frame(width: 12, height: 12)
                        .overlay { Circle().stroke(.white, lineWidth: 2) }
                }
            }
        }
        .mapStyle(.standard(pointsOfInterest: .excludingAll))
        .frame(height: 250)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
        .allowsHitTesting(false)
    }

    // MARK: - Main Stats

    private func mainStatsSection(run: Run) -> some View {
        VStack(spacing: 16) {
            // Title and date
            VStack(spacing: 4) {
                Text(run.notes ?? "Run")
                    .font(.title2)
                    .fontWeight(.bold)

                HStack(spacing: 8) {
                    Text(run.startTime, format: .dateTime.weekday(.wide).month().day())
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text(run.startTime, format: .dateTime.hour().minute())
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }

            // Key metrics
            HStack(spacing: 0) {
                metricTile(value: String(format: "%.2f", run.distanceKm), unit: "km", label: "Distance")
                metricDivider
                metricTile(value: run.durationFormatted, unit: "", label: "Duration")
                metricDivider
                metricTile(value: run.avgPaceFormatted, unit: "/km", label: "Avg Pace")
            }
            .padding(16)
            .background(AppTheme.surfaceContainerLow)
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .padding(.horizontal)
    }

    private func metricTile(value: String, unit: String, label: String) -> some View {
        VStack(spacing: 4) {
            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text(value)
                    .font(.title3)
                    .fontWeight(.bold)
                if !unit.isEmpty {
                    Text(unit)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }

    private var metricDivider: some View {
        Rectangle()
            .fill(.quaternary)
            .frame(width: 1, height: 40)
    }

    // MARK: - Pace Chart

    private func paceChartSection(run: Run) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("PACE CHART")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            let paces = run.splits.map { $0.paceSecondsPerKm }
            let minPace = (paces.min() ?? 300) - 30
            let maxPace = (paces.max() ?? 600) + 30
            let range = max(maxPace - minPace, 60)

            HStack(alignment: .bottom, spacing: 4) {
                ForEach(Array(run.splits.enumerated()), id: \.element.kilometer) { index, split in
                    let normalizedHeight = 1.0 - (split.paceSecondsPerKm - minPace) / range
                    let isFastest = split.paceSecondsPerKm == paces.min()

                    VStack(spacing: 2) {
                        Text(split.paceFormatted)
                            .font(.system(size: 8))
                            .foregroundStyle(.secondary)

                        RoundedRectangle(cornerRadius: 3)
                            .fill(isFastest ? AppTheme.running : AppTheme.running.opacity(0.6))
                            .frame(height: max(8, CGFloat(normalizedHeight) * 80))

                        Text("\(split.kilometer)")
                            .font(.system(size: 9))
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .frame(height: 120)
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - Splits Table

    private func splitsTableSection(run: Run) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("SPLITS")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            VStack(spacing: 0) {
                // Header
                HStack {
                    Text("KM")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundStyle(.secondary)
                        .frame(width: 40, alignment: .leading)
                    Text("PACE")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                    Text("TIME")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                    if run.splits.first?.avgHeartRate != nil {
                        Text("HR")
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundStyle(.secondary)
                            .frame(width: 50, alignment: .trailing)
                    }
                    Text("ELEV")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundStyle(.secondary)
                        .frame(width: 50, alignment: .trailing)
                }
                .padding(.vertical, 8)
                .padding(.horizontal, 12)

                Divider()

                let bestPace = run.splits.map(\.paceSecondsPerKm).min() ?? 0

                ForEach(run.splits, id: \.kilometer) { split in
                    HStack {
                        Text("\(split.kilometer)")
                            .fontWeight(.medium)
                            .frame(width: 40, alignment: .leading)

                        HStack(spacing: 4) {
                            if split.paceSecondsPerKm == bestPace {
                                Image(systemName: "bolt.fill")
                                    .font(.caption2)
                                    .foregroundStyle(AppTheme.running)
                            }
                            Text(split.paceFormatted)
                                .fontWeight(split.paceSecondsPerKm == bestPace ? .bold : .regular)
                                .foregroundStyle(split.paceSecondsPerKm == bestPace ? AppTheme.running : .primary)
                        }
                        .frame(maxWidth: .infinity, alignment: .trailing)

                        Text(split.durationFormatted)
                            .frame(maxWidth: .infinity, alignment: .trailing)

                        if split.avgHeartRate != nil {
                            Text(split.avgHeartRate.map { "\($0)" } ?? "--")
                                .frame(width: 50, alignment: .trailing)
                        }

                        Text(String(format: "%+.0fm", split.elevationChange))
                            .font(.caption)
                            .foregroundStyle(split.elevationChange >= 0 ? .red : .green)
                            .frame(width: 50, alignment: .trailing)
                    }
                    .font(.subheadline)
                    .padding(.vertical, 8)
                    .padding(.horizontal, 12)

                    if split.kilometer < (run.splits.last?.kilometer ?? 0) {
                        Divider().padding(.leading, 12)
                    }
                }
            }
            .background(AppTheme.surfaceContainerLow)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .padding(.horizontal)
    }

    // MARK: - Heart Rate

    private func heartRateSection(run: Run) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("HEART RATE")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            HStack(spacing: 24) {
                if let avg = run.avgHeartRate {
                    VStack(spacing: 4) {
                        HStack(alignment: .lastTextBaseline, spacing: 2) {
                            Text("\(avg)")
                                .font(.title2)
                                .fontWeight(.bold)
                                .foregroundStyle(.red)
                            Text("bpm")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Text("Average")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                if let max = run.maxHeartRate {
                    VStack(spacing: 4) {
                        HStack(alignment: .lastTextBaseline, spacing: 2) {
                            Text("\(max)")
                                .font(.title2)
                                .fontWeight(.bold)
                            Text("bpm")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Text("Maximum")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                Spacer()
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - Elevation

    private func elevationSection(run: Run) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("ELEVATION")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            HStack(spacing: 32) {
                VStack(spacing: 4) {
                    HStack(spacing: 4) {
                        Image(systemName: "arrow.up")
                            .font(.caption)
                            .foregroundStyle(.green)
                        Text(String(format: "%.0f m", run.elevationGainMeters))
                            .font(.title3)
                            .fontWeight(.bold)
                    }
                    Text("Gain")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                VStack(spacing: 4) {
                    HStack(spacing: 4) {
                        Image(systemName: "arrow.down")
                            .font(.caption)
                            .foregroundStyle(.red)
                        Text(String(format: "%.0f m", run.elevationLossMeters))
                            .font(.title3)
                            .fontWeight(.bold)
                    }
                    Text("Loss")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Spacer()
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - Notes

    private func notesSection(notes: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("NOTES")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            Text(notes)
                .font(.subheadline)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - Metadata

    private func metadataSection(run: Run) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("DETAILS")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            HStack {
                Text("Source")
                    .foregroundStyle(.secondary)
                Spacer()
                Text(run.source.rawValue.capitalized)
            }
            .font(.subheadline)

            if let cadence = run.avgCadence {
                HStack {
                    Text("Cadence")
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text("\(cadence) spm")
                }
                .font(.subheadline)
            }

            HStack {
                Text("Calories")
                    .foregroundStyle(.secondary)
                Spacer()
                Text("\(run.caloriesBurned) kcal")
            }
            .font(.subheadline)

            if let weather = run.weather {
                HStack {
                    Text("Weather")
                        .foregroundStyle(.secondary)
                    Spacer()
                    Text(weather)
                }
                .font(.subheadline)
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - Data

    private func loadRun() {
        let id = runId
        let descriptor = FetchDescriptor<Run>(
            predicate: #Predicate<Run> { r in r.id == id }
        )
        run = try? modelContext.fetch(descriptor).first
    }

    private func deleteRun() {
        if let run {
            modelContext.delete(run)
            try? modelContext.save()
            dismiss()
        }
    }
}
