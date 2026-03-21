import SwiftUI
import WatchKit

/// Multi-page tracking view matching the Wear OS 6-page pager:
/// 1. Main metrics  2. Heart rate zone  3. Swim/pace details  4-6. Controls
struct TrackingView: View {
    @EnvironmentObject var workoutManager: WorkoutManager
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            MainTrackingPage()
                .environmentObject(workoutManager)
                .tag(0)

            HeartRateZonePage()
                .environmentObject(workoutManager)
                .tag(1)

            if workoutManager.state.activityType == .swimming {
                SwimDetailsPage()
                    .environmentObject(workoutManager)
                    .tag(2)
            } else {
                PaceDetailsPage()
                    .environmentObject(workoutManager)
                    .tag(2)
            }

            ControlsPage()
                .environmentObject(workoutManager)
                .tag(3)
        }
        .tabViewStyle(.verticalPage)
    }
}

// MARK: - Page 1: Main Metrics

struct MainTrackingPage: View {
    @EnvironmentObject var wm: WorkoutManager

    var body: some View {
        VStack(spacing: 4) {
            // Duration
            Text(wm.state.durationFormatted)
                .font(.system(size: 36, weight: .bold, design: .monospaced))
                .foregroundStyle(WatchColors.activityColor(wm.state.activityType))

            // Distance
            if wm.state.activityType == .swimming {
                HStack(alignment: .lastTextBaseline, spacing: 2) {
                    Text("\(Int(wm.state.distanceMeters))")
                        .font(.system(size: 28, weight: .bold))
                    Text("m")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            } else {
                HStack(alignment: .lastTextBaseline, spacing: 2) {
                    Text(String(format: "%.2f", wm.state.distanceKm))
                        .font(.system(size: 28, weight: .bold))
                    Text("km")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }

            // Heart rate
            HStack(spacing: 4) {
                Image(systemName: "heart.fill")
                    .foregroundStyle(hrColor)
                    .font(.caption)
                Text("\(wm.state.heartRate)")
                    .font(.system(size: 20, weight: .semibold))
                Text("bpm")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            // Swim: Lap button
            if wm.state.activityType == .swimming, wm.state.swimType == .pool {
                Button {
                    wm.addSwimLap()
                } label: {
                    Text("+ Lap (\(wm.state.laps))")
                        .fontWeight(.bold)
                        .frame(maxWidth: .infinity)
                }
                .tint(WatchColors.swimming)
                .padding(.top, 4)
            }

            // Pause indicator
            if wm.state.isPaused {
                Text("PAUSED")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundStyle(.yellow)
                    .padding(.top, 2)
            }
        }
        .padding(.horizontal, 8)
    }

    private var hrColor: Color {
        switch wm.state.hrAlert {
        case .inZone: return WatchColors.zoneIn
        case .tooLow: return WatchColors.zoneBelow
        case .tooHigh: return WatchColors.zoneAbove
        }
    }
}

// MARK: - Page 2: Heart Rate Zone

struct HeartRateZonePage: View {
    @EnvironmentObject var wm: WorkoutManager

    var body: some View {
        VStack(spacing: 8) {
            Text("HEART RATE")
                .font(.caption2)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            Text("\(wm.state.heartRate)")
                .font(.system(size: 48, weight: .bold))
                .foregroundStyle(hrColor)

            Text("bpm")
                .font(.caption)
                .foregroundStyle(.secondary)

            // Zone indicator
            if let min = wm.state.targetHRMin, let max = wm.state.targetHRMax {
                Text("Zone: \(min)-\(max)")
                    .font(.caption2)

                Text(zoneLabel)
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundStyle(hrColor)
            }

            // Calories
            HStack {
                Image(systemName: "flame.fill")
                    .foregroundStyle(.orange)
                    .font(.caption2)
                Text("\(Int(wm.state.calories)) kcal")
                    .font(.caption)
            }
            .padding(.top, 4)
        }
    }

    private var hrColor: Color {
        switch wm.state.hrAlert {
        case .inZone: return WatchColors.zoneIn
        case .tooLow: return WatchColors.zoneBelow
        case .tooHigh: return WatchColors.zoneAbove
        }
    }

    private var zoneLabel: String {
        switch wm.state.hrAlert {
        case .inZone: return "IN ZONE"
        case .tooLow: return "TOO LOW"
        case .tooHigh: return "TOO HIGH"
        }
    }
}

// MARK: - Page 3a: Pace Details (Run/Cycle)

struct PaceDetailsPage: View {
    @EnvironmentObject var wm: WorkoutManager

    var body: some View {
        VStack(spacing: 8) {
            Text("PACE")
                .font(.caption2)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            Text(wm.state.paceFormatted)
                .font(.system(size: 40, weight: .bold))

            Text("/km")
                .font(.caption)
                .foregroundStyle(.secondary)

            Divider()

            HStack {
                VStack {
                    Text(String(format: "%.2f", wm.state.distanceKm))
                        .font(.headline)
                    Text("km")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                VStack {
                    Text(wm.state.durationFormatted)
                        .font(.headline)
                    Text("time")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
            .padding(.horizontal)
        }
    }
}

// MARK: - Page 3b: Swim Details

struct SwimDetailsPage: View {
    @EnvironmentObject var wm: WorkoutManager

    var body: some View {
        VStack(spacing: 8) {
            Text("SWIM")
                .font(.caption2)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            HStack(spacing: 20) {
                VStack {
                    Text("\(wm.state.laps)")
                        .font(.system(size: 32, weight: .bold))
                    Text("laps")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }

                VStack {
                    Text("\(Int(wm.state.distanceMeters))")
                        .font(.system(size: 32, weight: .bold))
                    Text("meters")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }

            if wm.state.strokeCount > 0 {
                HStack {
                    Text("\(wm.state.strokeCount)")
                        .fontWeight(.bold)
                    Text("strokes")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }

            // Pace per 100m
            let pace100m: String = {
                guard wm.state.distanceMeters > 0 else { return "--:--" }
                let secsPer100 = wm.state.elapsedSeconds / (wm.state.distanceMeters / 100.0)
                let m = Int(secsPer100) / 60
                let s = Int(secsPer100) % 60
                return String(format: "%d:%02d", m, s)
            }()

            HStack {
                Text(pace100m)
                    .font(.headline)
                Text("/100m")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

// MARK: - Page 4: Controls

struct ControlsPage: View {
    @EnvironmentObject var wm: WorkoutManager
    @State private var showStopConfirmation = false

    var body: some View {
        VStack(spacing: 12) {
            Text("CONTROLS")
                .font(.caption2)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            // Pause / Resume
            Button {
                if wm.state.isPaused { wm.resume() } else { wm.pause() }
            } label: {
                Label(
                    wm.state.isPaused ? "Resume" : "Pause",
                    systemImage: wm.state.isPaused ? "play.fill" : "pause.fill"
                )
                .frame(maxWidth: .infinity)
            }
            .tint(wm.state.isPaused ? .green : .yellow)

            // Stop
            Button(role: .destructive) {
                showStopConfirmation = true
            } label: {
                Label("End Workout", systemImage: "stop.fill")
                    .frame(maxWidth: .infinity)
            }
            .confirmationDialog("End workout?", isPresented: $showStopConfirmation) {
                Button("Save & End", role: .destructive) {
                    wm.stop()
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Save this \(wm.state.durationFormatted) \(wm.state.activityType.rawValue.lowercased())?")
            }

            // Lock screen for swimming
            if wm.state.activityType == .swimming {
                Button {
                    WKInterfaceDevice.current().enableWaterLock()
                } label: {
                    Label("Water Lock", systemImage: "drop.fill")
                        .frame(maxWidth: .infinity)
                }
                .tint(WatchColors.swimming)
            }
        }
        .padding(.horizontal, 8)
    }
}
