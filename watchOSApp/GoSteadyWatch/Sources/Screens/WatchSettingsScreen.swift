import SwiftUI

struct WatchSettingsScreen: View {

    @Environment(WatchSettingsViewModel.self) private var settings

    var body: some View {
        @Bindable var s = settings
        List {
            Section("Units") {
                Toggle("Metric (km/kg)", isOn: $s.useMetric)
            }

            Section("Workout") {
                Toggle("Haptic Feedback", isOn: $s.hapticFeedback)
                Toggle("Auto-Pause (Run)", isOn: $s.autoPauseEnabled)
            }

            Section("Heart Rate Zones") {
                HStack {
                    Text("Max HR")
                    Spacer()
                    Text("\(settings.maxHeartRate) bpm")
                        .foregroundStyle(.secondary)
                }

                Stepper(value: $s.maxHeartRate, in: 140...220, step: 1) {
                    Text("Adjust Max HR")
                        .font(.caption)
                }

                ForEach(HRZone.allCases, id: \.rawValue) { zone in
                    let range = zone.range(maxHR: settings.maxHeartRate)
                    HStack {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(zone.color)
                            .frame(width: 4, height: 16)
                        Text("Z\(zone.rawValue)")
                            .font(.caption)
                            .fontWeight(.bold)
                        Spacer()
                        Text("\(range.lowerBound)-\(range.upperBound)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Section("Swimming") {
                Picker("Pool Length", selection: $s.poolLength) {
                    ForEach(PoolLength.allCases) { length in
                        Text(length.label).tag(length)
                    }
                }
            }

            Section("About") {
                HStack {
                    Text("GoSteady Watch")
                    Spacer()
                    Text("1.0")
                        .foregroundStyle(.secondary)
                }

                Button(role: .destructive) {
                    settings.resetToDefaults()
                } label: {
                    Text("Reset to Defaults")
                }
            }
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
    }
}
