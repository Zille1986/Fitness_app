import SwiftUI
import WatchKit

struct ActiveRunScreen: View {

    @Environment(WatchWorkoutService.self) private var ws
    @Environment(WatchWorkoutViewModel.self) private var vm
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            // Page 1: Main Metrics
            runMetricsPage
                .tag(0)

            // Page 2: Heart Rate Zone
            heartRateZonePage
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

    private var runMetricsPage: some View {
        VStack(spacing: 4) {
            // Timer
            WatchTimerView(seconds: ws.elapsedSeconds, color: WatchTheme.activityColor(ws.currentActivity))

            // Distance
            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text(String(format: "%.2f", ws.distanceMeters / 1000.0))
                    .font(.system(size: 28, weight: .bold))
                Text("km")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            // Pace
            HStack(spacing: 4) {
                Image(systemName: "speedometer")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                Text(formatPace(ws.currentPaceSecondsPerKm))
                    .font(.system(size: 18, weight: .semibold))
                Text("/km")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            // Heart Rate
            HStack(spacing: 4) {
                Image(systemName: "heart.fill")
                    .font(.caption2)
                    .foregroundStyle(zoneAlertColor)
                Text("\(ws.heartRate)")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(zoneAlertColor)
                Text("bpm")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            // Calories
            HStack(spacing: 4) {
                Image(systemName: "flame.fill")
                    .font(.caption2)
                    .foregroundStyle(.orange)
                Text("\(Int(ws.calories))")
                    .font(.system(size: 14, weight: .medium))
                Text("kcal")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            // Pause indicator
            if ws.isPaused {
                Text(ws.autoPauseActive ? "AUTO-PAUSED" : "PAUSED")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundStyle(.yellow)
            }
        }
        .padding(.horizontal, 8)
    }

    // MARK: - Heart Rate Zone Page

    private var heartRateZonePage: some View {
        VStack(spacing: 6) {
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
                .foregroundStyle(zoneAlertColor)

            Text("bpm")
                .font(.caption)
                .foregroundStyle(.secondary)

            Text(vm.currentZone.name)
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(vm.currentZone.color)

            // Stats row
            HStack(spacing: 20) {
                VStack(spacing: 2) {
                    Text(String(format: "%.2f", ws.distanceMeters / 1000.0))
                        .font(.system(size: 14, weight: .bold))
                    Text("km")
                        .font(.system(size: 9))
                        .foregroundStyle(.secondary)
                }
                VStack(spacing: 2) {
                    Text("\(Int(ws.calories))")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(.orange)
                    Text("kcal")
                        .font(.system(size: 9))
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    private var zoneAlertColor: Color {
        switch vm.hrAlert {
        case .inZone: return WatchTheme.zoneIn
        case .tooLow: return WatchTheme.zoneBelow
        case .tooHigh: return WatchTheme.zoneAbove
        }
    }
}
