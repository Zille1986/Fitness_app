import SwiftUI
import WatchKit

struct ActiveCycleScreen: View {

    @Environment(WatchWorkoutService.self) private var ws
    @Environment(WatchWorkoutViewModel.self) private var vm
    @Environment(WatchLocationService.self) private var locationService
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            // Page 1: Main Cycling Metrics
            cycleMetricsPage
                .tag(0)

            // Page 2: Heart Rate
            heartRatePage
                .tag(1)

            // Page 3: Controls
            WorkoutControlsView()
                .tag(2)
        }
        .tabViewStyle(.verticalPage)
        .navigationBarBackButtonHidden(true)
        .onChange(of: ws.heartRate) { _, newValue in
            vm.updateZone(heartRate: newValue)
        }
    }

    // MARK: - Metrics Page

    private var cycleMetricsPage: some View {
        VStack(spacing: 6) {
            // Timer
            WatchTimerView(seconds: ws.elapsedSeconds, color: WatchTheme.cycling)

            // Speed (stored in currentPaceSecondsPerKm as km/h for cycling)
            let speedKmh = ws.currentPaceSecondsPerKm
            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text(speedKmh > 0 ? String(format: "%.1f", speedKmh) : "--")
                    .font(.system(size: 28, weight: .bold))
                Text("km/h")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            // Distance
            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text(String(format: "%.2f", ws.distanceMeters / 1000.0))
                    .font(.system(size: 22, weight: .bold))
                Text("km")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            // Stats row
            HStack(spacing: 16) {
                // Heart Rate
                VStack(spacing: 2) {
                    HStack(spacing: 2) {
                        Image(systemName: "heart.fill")
                            .font(.system(size: 8))
                            .foregroundStyle(.red)
                        Text("\(ws.heartRate)")
                            .font(.system(size: 16, weight: .semibold))
                    }
                    Text("bpm")
                        .font(.system(size: 9))
                        .foregroundStyle(.secondary)
                }

                // Calories
                VStack(spacing: 2) {
                    HStack(spacing: 2) {
                        Image(systemName: "flame.fill")
                            .font(.system(size: 8))
                            .foregroundStyle(.orange)
                        Text("\(Int(ws.calories))")
                            .font(.system(size: 16, weight: .semibold))
                    }
                    Text("kcal")
                        .font(.system(size: 9))
                        .foregroundStyle(.secondary)
                }
            }

            // Elevation
            if ws.currentActivity == .outdoorCycle {
                HStack(spacing: 4) {
                    Image(systemName: "arrow.up.right")
                        .font(.system(size: 9))
                        .foregroundStyle(.green)
                    Text(String(format: "%.0f m", locationService.totalElevationGain))
                        .font(.system(size: 12, weight: .medium))
                    Text("elev")
                        .font(.system(size: 9))
                        .foregroundStyle(.secondary)
                }
            }

            // Pause indicator
            if ws.isPaused {
                Text("PAUSED")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundStyle(.yellow)
            }
        }
        .padding(.horizontal, 8)
    }

    // MARK: - Heart Rate Page

    private var heartRatePage: some View {
        VStack(spacing: 8) {
            Text("HEART RATE")
                .font(.caption2)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            HeartRateZoneIndicator(
                heartRate: ws.heartRate,
                zone: vm.currentZone,
                maxHR: vm.maxHR
            )

            Text("\(ws.heartRate)")
                .font(.system(size: 42, weight: .bold))
                .foregroundStyle(vm.currentZone.color)

            Text(vm.currentZone.name)
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(vm.currentZone.color)

            HStack(spacing: 20) {
                VStack(spacing: 2) {
                    Text(String(format: "%.1f", ws.distanceMeters / 1000.0))
                        .font(.system(size: 14, weight: .bold))
                    Text("km")
                        .font(.system(size: 9))
                        .foregroundStyle(.secondary)
                }
                VStack(spacing: 2) {
                    Text(formatDuration(ws.elapsedSeconds))
                        .font(.system(size: 14, weight: .bold))
                    Text("time")
                        .font(.system(size: 9))
                        .foregroundStyle(.secondary)
                }
            }
        }
    }
}
