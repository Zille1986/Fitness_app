import SwiftUI
import WatchKit

struct WorkoutSummaryScreen: View {

    @Environment(WatchWorkoutViewModel.self) private var vm

    var body: some View {
        ScrollView {
            if let data = vm.summaryData {
                VStack(spacing: 10) {
                    // Header
                    Image(systemName: "checkmark.circle.fill")
                        .font(.largeTitle)
                        .foregroundStyle(WatchTheme.primary)

                    Text("Workout Complete")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(WatchTheme.primary)

                    Text(data.activityType.rawValue)
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    Divider().padding(.horizontal)

                    // Duration
                    WatchMetricCard(
                        label: "Duration",
                        value: formatDurationLong(data.duration),
                        unit: "",
                        color: WatchTheme.activityColor(data.activityType)
                    )

                    // Calories
                    WatchMetricCard(
                        label: "Calories",
                        value: "\(Int(data.calories))",
                        unit: "kcal",
                        color: .orange
                    )

                    // Distance (if applicable)
                    if data.distanceMeters > 0 {
                        WatchMetricCard(
                            label: "Distance",
                            value: data.activityType.isSwimming
                                ? "\(Int(data.distanceMeters))"
                                : String(format: "%.2f", data.distanceMeters / 1000.0),
                            unit: data.activityType.isSwimming ? "m" : "km",
                            color: WatchTheme.activityColor(data.activityType)
                        )
                    }

                    // Heart Rate
                    if data.avgHeartRate > 0 {
                        HStack(spacing: 16) {
                            VStack(spacing: 2) {
                                Text("\(data.avgHeartRate)")
                                    .font(.system(size: 20, weight: .bold))
                                Text("avg bpm")
                                    .font(.system(size: 9))
                                    .foregroundStyle(.secondary)
                            }
                            VStack(spacing: 2) {
                                Text("\(data.maxHeartRate)")
                                    .font(.system(size: 20, weight: .bold))
                                    .foregroundStyle(.red)
                                Text("max bpm")
                                    .font(.system(size: 9))
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }

                    // HR Chart (simple sparkline)
                    if data.heartRateSamples.count > 2 {
                        hrChart(samples: data.heartRateSamples)
                            .frame(height: 40)
                            .padding(.horizontal, 8)
                    }

                    // Swimming stats
                    if data.activityType.isSwimming && data.strokeCount > 0 {
                        WatchMetricCard(
                            label: "Strokes",
                            value: "\(data.strokeCount)",
                            unit: "",
                            color: WatchTheme.swimming
                        )
                    }

                    // Elevation
                    if data.elevationGain > 0 {
                        WatchMetricCard(
                            label: "Elevation",
                            value: String(format: "%.0f", data.elevationGain),
                            unit: "m",
                            color: .green
                        )
                    }

                    // Laps
                    if !data.laps.isEmpty {
                        Text("\(data.laps.count) laps")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }

                    Divider().padding(.horizontal)

                    // Done button
                    Button {
                        vm.dismissSummary()
                    } label: {
                        Text("Done")
                            .fontWeight(.bold)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                    }
                    .tint(WatchTheme.primary)
                    .padding(.horizontal, 16)
                    .padding(.bottom, 16)
                }
                .padding(.horizontal, 8)
                .padding(.top, 8)
            }
        }
        .navigationBarBackButtonHidden(true)
    }

    // MARK: - HR Chart

    @ViewBuilder
    private func hrChart(samples: [(time: TimeInterval, bpm: Int)]) -> some View {
        let values = samples.map { Double($0.bpm) }
        let minVal = values.min() ?? 60
        let maxVal = values.max() ?? 200
        let range = max(maxVal - minVal, 1)

        GeometryReader { geo in
            Path { path in
                let stepX = geo.size.width / CGFloat(max(values.count - 1, 1))
                for (index, value) in values.enumerated() {
                    let x = CGFloat(index) * stepX
                    let y = geo.size.height * (1.0 - CGFloat((value - minVal) / range))
                    if index == 0 {
                        path.move(to: CGPoint(x: x, y: y))
                    } else {
                        path.addLine(to: CGPoint(x: x, y: y))
                    }
                }
            }
            .stroke(Color.red, lineWidth: 2)
        }
    }
}
