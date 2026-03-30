import SwiftUI
import WatchKit

struct ActiveSwimScreen: View {

    @Environment(WatchWorkoutService.self) private var ws
    @Environment(WatchWorkoutViewModel.self) private var vm
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            // Page 1: Main Swim Metrics
            swimMetricsPage
                .tag(0)

            // Page 2: Lap Details
            lapDetailsPage
                .tag(1)

            // Page 3: Controls
            swimControlsPage
                .tag(2)
        }
        .tabViewStyle(.verticalPage)
        .navigationBarBackButtonHidden(true)
    }

    // MARK: - Swim Metrics Page

    private var swimMetricsPage: some View {
        VStack(spacing: 6) {
            // Water lock indicator
            HStack(spacing: 4) {
                Image(systemName: "drop.fill")
                    .font(.caption2)
                    .foregroundStyle(WatchTheme.swimming)
                Text("WATER LOCK ON")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundStyle(WatchTheme.swimming)
            }

            // Timer
            WatchTimerView(seconds: ws.elapsedSeconds, color: WatchTheme.swimming)

            // Lap count
            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text("\(ws.laps.count)")
                    .font(.system(size: 32, weight: .bold))
                Text("laps")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            // Distance
            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text("\(Int(ws.distanceMeters))")
                    .font(.system(size: 22, weight: .bold))
                Text("m")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            // Pace per 100m
            let pacePer100m: String = {
                guard ws.distanceMeters > 0 else { return "--:--" }
                let secsPer100 = ws.elapsedSeconds / (ws.distanceMeters / 100.0)
                let m = Int(secsPer100) / 60
                let s = Int(secsPer100) % 60
                return String(format: "%d:%02d", m, s)
            }()

            HStack(spacing: 4) {
                Text(pacePer100m)
                    .font(.system(size: 16, weight: .semibold))
                Text("/100m")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            // Heart rate
            HStack(spacing: 4) {
                Image(systemName: "heart.fill")
                    .font(.caption2)
                    .foregroundStyle(.red)
                Text("\(ws.heartRate)")
                    .font(.system(size: 14, weight: .medium))
                Text("bpm")
                    .font(.system(size: 9))
                    .foregroundStyle(.secondary)
            }

            // Stroke count
            if ws.strokeCount > 0 {
                HStack(spacing: 4) {
                    Text("\(ws.strokeCount)")
                        .font(.system(size: 14, weight: .medium))
                    Text("strokes")
                        .font(.system(size: 9))
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(.horizontal, 8)
    }

    // MARK: - Lap Details Page

    private var lapDetailsPage: some View {
        ScrollView {
            VStack(spacing: 6) {
                Text("LAPS")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundStyle(.secondary)
                    .padding(.top, 8)

                if ws.laps.isEmpty {
                    Text("No laps yet")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .padding(.top, 20)
                } else {
                    ForEach(ws.laps.reversed()) { lap in
                        HStack {
                            Text("Lap \(lap.id)")
                                .font(.caption)
                                .fontWeight(.medium)
                            Spacer()
                            Text(String(format: "%.0fm", lap.distanceMeters))
                                .font(.caption)
                            Text(formatPace(lap.paceSecondsPerKm / 10.0)) // /100m
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        .padding(.vertical, 3)
                        .padding(.horizontal, 8)
                    }
                }
            }
        }
    }

    // MARK: - Swim Controls Page

    private var swimControlsPage: some View {
        VStack(spacing: 10) {
            Text("CONTROLS")
                .font(.caption2)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            // Manual Lap Button
            if ws.currentActivity == .poolSwim {
                Button {
                    ws.addSwimLap()
                } label: {
                    Label("Add Lap", systemImage: "plus.circle.fill")
                        .frame(maxWidth: .infinity)
                }
                .tint(WatchTheme.swimming)
            }

            // Water Lock
            Button {
                WKInterfaceDevice.current().enableWaterLock()
            } label: {
                Label("Water Lock", systemImage: "drop.fill")
                    .frame(maxWidth: .infinity)
            }
            .tint(.blue)

            // Pause / Resume
            Button {
                if ws.isPaused { ws.resume() } else { ws.pause() }
            } label: {
                Label(
                    ws.isPaused ? "Resume" : "Pause",
                    systemImage: ws.isPaused ? "play.fill" : "pause.fill"
                )
                .frame(maxWidth: .infinity)
            }
            .tint(ws.isPaused ? .green : .yellow)

            // End
            Button(role: .destructive) {
                ws.stop()
            } label: {
                Label("End Swim", systemImage: "stop.fill")
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, 8)
    }
}
