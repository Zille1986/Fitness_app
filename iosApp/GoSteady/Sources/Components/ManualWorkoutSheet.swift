import SwiftUI
import HealthKit

enum SportType: String, CaseIterable {
    case run = "Run"
    case swim = "Swim"
    case bike = "Bike"

    var distanceUnit: String {
        switch self {
        case .run, .bike: return "km"
        case .swim: return "m"
        }
    }

    var distancePlaceholder: String {
        switch self {
        case .run: return "e.g. 5.0"
        case .swim: return "e.g. 1500"
        case .bike: return "e.g. 20.0"
        }
    }

    var hkWorkoutType: HKWorkoutActivityType {
        switch self {
        case .run: return .running
        case .swim: return .swimming
        case .bike: return .cycling
        }
    }
}

struct ManualWorkoutSheet: View {
    @Environment(\.dismiss) private var dismiss
    let sportType: SportType

    @State private var distance = ""
    @State private var hours = ""
    @State private var minutes = ""
    @State private var seconds = ""
    @State private var notes = ""

    private var isValid: Bool {
        guard let d = Double(distance), d > 0 else { return false }
        let h = Int(hours) ?? 0
        let m = Int(minutes) ?? 0
        let s = Int(seconds) ?? 0
        return h + m + s > 0
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Distance (\(sportType.distanceUnit))") {
                    TextField(sportType.distancePlaceholder, text: $distance)
                        .keyboardType(.decimalPad)
                }

                Section("Duration") {
                    HStack(spacing: 12) {
                        VStack {
                            Text("Hrs").font(.caption2).foregroundStyle(.secondary)
                            TextField("0", text: $hours).keyboardType(.numberPad).multilineTextAlignment(.center)
                        }
                        VStack {
                            Text("Min").font(.caption2).foregroundStyle(.secondary)
                            TextField("0", text: $minutes).keyboardType(.numberPad).multilineTextAlignment(.center)
                        }
                        VStack {
                            Text("Sec").font(.caption2).foregroundStyle(.secondary)
                            TextField("0", text: $seconds).keyboardType(.numberPad).multilineTextAlignment(.center)
                        }
                    }
                }

                Section("Notes (optional)") {
                    TextField("How did it feel?", text: $notes, axis: .vertical)
                        .lineLimit(3)
                }

                Section {
                    Button {
                        saveWorkout()
                        dismiss()
                    } label: {
                        Text("Save \(sportType.rawValue)")
                            .frame(maxWidth: .infinity)
                            .fontWeight(.bold)
                    }
                    .disabled(!isValid)
                }
            }
            .navigationTitle("Log \(sportType.rawValue)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    private func saveWorkout() {
        guard let dist = Double(distance) else { return }
        let h = Int(hours) ?? 0
        let m = Int(minutes) ?? 0
        let s = Int(seconds) ?? 0
        let totalSeconds = TimeInterval(h * 3600 + m * 60 + s)

        let distanceMeters: Double
        switch sportType {
        case .run, .bike: distanceMeters = dist * 1000
        case .swim: distanceMeters = dist
        }

        // Save to HealthKit
        let healthStore = HKHealthStore()
        let workout = HKWorkout(
            activityType: sportType.hkWorkoutType,
            start: Date().addingTimeInterval(-totalSeconds),
            end: Date(),
            duration: totalSeconds,
            totalEnergyBurned: nil,
            totalDistance: HKQuantity(unit: .meter(), doubleValue: distanceMeters),
            metadata: notes.isEmpty ? nil : [HKMetadataKeyWorkoutBrandName: "GoSteady"]
        )

        healthStore.save(workout) { success, error in
            if let error = error {
                print("Failed to save workout: \(error)")
            }
        }
    }
}
