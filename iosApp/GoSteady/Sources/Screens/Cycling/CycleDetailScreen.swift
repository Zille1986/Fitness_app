import SwiftUI
import MapKit

struct CycleDetailScreen: View {
    let workout: CyclingWorkoutSummary

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Header
                headerCard

                // Route map
                if !workout.routeCoordinates.isEmpty {
                    routeMapSection
                }

                // Primary stats
                primaryStatsGrid

                // Elevation profile
                if !workout.splits.isEmpty {
                    elevationProfileSection
                }

                // Speed chart
                if !workout.splits.isEmpty {
                    speedChartSection
                }

                // Splits breakdown
                if !workout.splits.isEmpty {
                    splitsBreakdownSection
                }

                // Notes
                if let notes = workout.notes, !notes.isEmpty {
                    notesSection(notes)
                }
            }
            .padding()
        }
        .background(AppTheme.surface)
        .navigationTitle("Ride Details")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Header Card

    private var headerCard: some View {
        VStack(spacing: 12) {
            HStack {
                Image(systemName: workout.cyclingType.icon)
                    .font(.title2)
                    .foregroundStyle(AppTheme.cycling)
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(workout.cyclingType.displayName)")
                        .font(.title3)
                        .fontWeight(.bold)
                    Text(workout.fullDateFormatted)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }

            // Big distance
            Text(workout.distanceFormatted)
                .font(.system(size: 48, weight: .bold, design: .rounded))
                .foregroundStyle(AppTheme.cycling)

            Text(workout.durationFormatted)
                .font(.title3)
                .foregroundStyle(.secondary)
        }
        .padding(20)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    // MARK: - Route Map

    private var routeMapSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("ROUTE")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.onSurfaceVariant)

            Map {
                MapPolyline(coordinates: workout.routeCoordinates.map {
                    CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lon)
                })
                .stroke(AppTheme.cycling, lineWidth: 3)

                if let first = workout.routeCoordinates.first {
                    Annotation("Start", coordinate: CLLocationCoordinate2D(latitude: first.lat, longitude: first.lon)) {
                        Image(systemName: "flag.fill")
                            .foregroundStyle(.green)
                            .font(.caption)
                    }
                }
                if let last = workout.routeCoordinates.last {
                    Annotation("End", coordinate: CLLocationCoordinate2D(latitude: last.lat, longitude: last.lon)) {
                        Image(systemName: "flag.checkered")
                            .foregroundStyle(.red)
                            .font(.caption)
                    }
                }
            }
            .mapStyle(.standard(elevation: .realistic))
            .frame(height: 220)
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }

    // MARK: - Primary Stats

    private var primaryStatsGrid: some View {
        LazyVGrid(columns: [
            GridItem(.flexible()),
            GridItem(.flexible()),
            GridItem(.flexible())
        ], spacing: 16) {
            detailStatCard(icon: "gauge.with.needle", value: workout.avgSpeedFormatted, label: "Avg Speed", color: AppTheme.cycling)
            detailStatCard(icon: "bolt.fill", value: String(format: "%.1f km/h", workout.maxSpeedKmh), label: "Max Speed", color: .orange)
            detailStatCard(icon: "mountain.2.fill", value: workout.elevationFormatted, label: "Elevation Gain", color: .brown)
            detailStatCard(icon: "flame.fill", value: "\(workout.caloriesBurned) cal", label: "Calories", color: .red)

            if let power = workout.avgPowerWatts {
                detailStatCard(icon: "bolt.circle.fill", value: "\(power) W", label: "Avg Power", color: .yellow)
            }
            if let cadence = workout.avgCadenceRpm {
                detailStatCard(icon: "arrow.triangle.2.circlepath", value: "\(cadence) rpm", label: "Cadence", color: .purple)
            }
            if let hr = workout.avgHeartRate {
                detailStatCard(icon: "heart.fill", value: "\(hr) bpm", label: "Avg HR", color: .red)
            }
            if let maxHr = workout.maxHeartRate {
                detailStatCard(icon: "heart.fill", value: "\(maxHr) bpm", label: "Max HR", color: .pink)
            }
        }
    }

    private func detailStatCard(icon: String, value: String, label: String, color: Color) -> some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundStyle(color)
            Text(value)
                .font(.subheadline)
                .fontWeight(.bold)
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Elevation Profile

    private var elevationProfileSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("ELEVATION PROFILE")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.onSurfaceVariant)

            let elevations = workout.splits.map(\.elevationChange)
            let maxElev = elevations.max() ?? 1
            let minElev = elevations.min() ?? 0
            let range = max(maxElev - minElev, 1)

            HStack(alignment: .bottom, spacing: 2) {
                ForEach(Array(workout.splits.enumerated()), id: \.element.id) { index, split in
                    let normalized = (split.elevationChange - minElev) / range
                    let barHeight = max(4, normalized * 80)

                    VStack(spacing: 0) {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(
                                LinearGradient(
                                    colors: [.brown.opacity(0.4), .brown.opacity(0.8)],
                                    startPoint: .bottom,
                                    endPoint: .top
                                )
                            )
                            .frame(height: barHeight)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .frame(height: 80)

            HStack {
                Text("Start")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
                Spacer()
                Text("Finish")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Speed Chart

    private var speedChartSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("SPEED PER KM")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.onSurfaceVariant)

            let speeds = workout.splits.map(\.avgSpeedKmh)
            let maxSpeed = speeds.max() ?? 1
            let minSpeed = speeds.min() ?? 0

            HStack(alignment: .bottom, spacing: 3) {
                ForEach(workout.splits) { split in
                    let normalized = maxSpeed > minSpeed
                        ? (split.avgSpeedKmh - minSpeed) / (maxSpeed - minSpeed)
                        : 0.5
                    let barHeight = 20.0 + normalized * 80.0

                    VStack(spacing: 2) {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(speedBarColor(speed: split.avgSpeedKmh, max: maxSpeed, min: minSpeed))
                            .frame(height: barHeight)
                        Text("\(split.kilometer)")
                            .font(.system(size: 7))
                            .foregroundStyle(.tertiary)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .frame(height: 100)

            HStack(spacing: 16) {
                HStack(spacing: 4) {
                    Circle().fill(AppTheme.cycling).frame(width: 6, height: 6)
                    Text("Fast")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                HStack(spacing: 4) {
                    Circle().fill(Color.blue).frame(width: 6, height: 6)
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

    private func speedBarColor(speed: Double, max: Double, min: Double) -> Color {
        guard max > min else { return .blue }
        let ratio = (speed - min) / (max - min)
        if ratio > 0.66 { return AppTheme.cycling }
        if ratio > 0.33 { return .blue }
        return .orange
    }

    // MARK: - Splits Breakdown

    private var splitsBreakdownSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("SPLITS")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.onSurfaceVariant)

            HStack {
                Text("KM")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .frame(width: 30, alignment: .leading)
                Spacer()
                Text("Time")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .frame(width: 50)
                Spacer()
                Text("Speed")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .frame(width: 65, alignment: .trailing)
            }
            .foregroundStyle(.secondary)

            Divider()

            ForEach(workout.splits) { split in
                HStack {
                    Text("\(split.kilometer)")
                        .font(.caption)
                        .frame(width: 30, alignment: .leading)
                    Spacer()
                    Text(split.durationFormatted)
                        .font(.caption)
                        .frame(width: 50)
                    Spacer()
                    Text(String(format: "%.1f km/h", split.avgSpeedKmh))
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundStyle(AppTheme.cycling)
                        .frame(width: 65, alignment: .trailing)
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
