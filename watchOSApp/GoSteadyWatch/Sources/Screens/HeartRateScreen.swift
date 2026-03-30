import SwiftUI

struct HeartRateScreen: View {

    @Environment(WatchHealthService.self) private var healthService
    @Environment(WatchWorkoutViewModel.self) private var vm
    @State private var hrHistory: [(Date, Double)] = []

    var body: some View {
        ScrollView {
            VStack(spacing: 10) {
                // Current BPM
                Text("\(healthService.currentHeartRate)")
                    .font(.system(size: 52, weight: .bold))
                    .foregroundStyle(.red)

                Text("bpm")
                    .font(.caption)
                    .foregroundStyle(.secondary)

                // Zone indicator
                let zone = currentZone
                HeartRateZoneIndicator(
                    heartRate: healthService.currentHeartRate,
                    zone: zone,
                    maxHR: vm.maxHR
                )
                .frame(height: 12)
                .padding(.horizontal, 16)

                Text(zone.name)
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundStyle(zone.color)

                Divider().padding(.horizontal)

                // Resting HR
                if healthService.restingHeartRate > 0 {
                    HStack {
                        VStack(spacing: 2) {
                            Text("\(healthService.restingHeartRate)")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundStyle(.blue)
                            Text("resting")
                                .font(.system(size: 9))
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                // Session graph
                if hrHistory.count > 2 {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Last Hour")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(.secondary)

                        hrGraph
                            .frame(height: 50)
                    }
                    .padding(.horizontal, 8)
                }

                // Zone breakdown
                VStack(alignment: .leading, spacing: 4) {
                    Text("HR Zones")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(.secondary)
                        .padding(.leading, 8)

                    ForEach(HRZone.allCases, id: \.rawValue) { z in
                        let range = z.range(maxHR: vm.maxHR)
                        HStack {
                            RoundedRectangle(cornerRadius: 2)
                                .fill(z.color)
                                .frame(width: 4, height: 16)
                            Text("Z\(z.rawValue)")
                                .font(.system(size: 10, weight: .bold))
                            Text(z.name)
                                .font(.system(size: 10))
                                .foregroundStyle(.secondary)
                            Spacer()
                            Text("\(range.lowerBound)-\(range.upperBound)")
                                .font(.system(size: 10, design: .monospaced))
                                .foregroundStyle(.secondary)
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 1)
                    }
                }
            }
            .padding(.top, 8)
        }
        .navigationTitle("Heart Rate")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            healthService.queryHeartRateHistory(last: 1) { results in
                hrHistory = results
            }
        }
    }

    private var currentZone: HRZone {
        let hr = healthService.currentHeartRate
        for zone in HRZone.allCases.reversed() {
            let range = zone.range(maxHR: vm.maxHR)
            if hr >= range.lowerBound {
                return zone
            }
        }
        return .zone1
    }

    @ViewBuilder
    private var hrGraph: some View {
        let values = hrHistory.map { $0.1 }
        let minVal = values.min() ?? 40
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
            .stroke(
                LinearGradient(
                    colors: [.blue, .green, .yellow, .red],
                    startPoint: .bottom,
                    endPoint: .top
                ),
                lineWidth: 2
            )
        }
    }
}
