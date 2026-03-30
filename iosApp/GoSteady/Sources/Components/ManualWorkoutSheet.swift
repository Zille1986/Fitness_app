import SwiftUI
import SwiftData
import HealthKit

enum SportType: String, CaseIterable, Identifiable {
    case run = "Run"
    case swim = "Swim"
    case bike = "Bike"
    case gym = "Gym"
    case hiit = "HIIT"

    var id: String { rawValue }

    var icon: String {
        switch self {
        case .run: return AppTheme.SportIcon.running
        case .swim: return AppTheme.SportIcon.swimming
        case .bike: return AppTheme.SportIcon.cycling
        case .gym: return AppTheme.SportIcon.gym
        case .hiit: return AppTheme.SportIcon.hiit
        }
    }

    var color: Color {
        switch self {
        case .run: return AppTheme.running
        case .swim: return AppTheme.swimming
        case .bike: return AppTheme.cycling
        case .gym: return AppTheme.gym
        case .hiit: return AppTheme.hiit
        }
    }

    var distanceUnit: String {
        switch self {
        case .run, .bike: return "km"
        case .swim: return "m"
        case .gym, .hiit: return ""
        }
    }

    var distancePlaceholder: String {
        switch self {
        case .run: return "e.g. 5.0"
        case .swim: return "e.g. 1500"
        case .bike: return "e.g. 20.0"
        case .gym, .hiit: return ""
        }
    }

    var hasDistance: Bool {
        switch self {
        case .run, .swim, .bike: return true
        case .gym, .hiit: return false
        }
    }

    var displayName: String {
        switch self {
        case .run: return "Running"
        case .swim: return "Swimming"
        case .bike: return "Cycling"
        case .gym: return "Gym"
        case .hiit: return "HIIT"
        }
    }

    var hkWorkoutType: HKWorkoutActivityType {
        switch self {
        case .run: return .running
        case .swim: return .swimming
        case .bike: return .cycling
        case .gym: return .traditionalStrengthTraining
        case .hiit: return .highIntensityIntervalTraining
        }
    }
}

struct ManualWorkoutSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    let sportType: SportType

    @State private var distance = ""
    @State private var hours = ""
    @State private var minutes = ""
    @State private var seconds = ""
    @State private var calories = ""
    @State private var notes = ""
    @State private var workoutDate = Date()
    @State private var avgHeartRate = ""
    @State private var isSaving = false
    @State private var showError = false
    @State private var errorMessage = ""

    private var isValid: Bool {
        let h = Int(hours) ?? 0
        let m = Int(minutes) ?? 0
        let s = Int(seconds) ?? 0
        let hasDuration = h + m + s > 0

        if sportType.hasDistance {
            guard let d = Double(distance), d > 0 else { return false }
            return hasDuration
        }
        return hasDuration
    }

    private var totalSeconds: TimeInterval {
        let h = Int(hours) ?? 0
        let m = Int(minutes) ?? 0
        let s = Int(seconds) ?? 0
        return TimeInterval(h * 3600 + m * 60 + s)
    }

    private var distanceMeters: Double? {
        guard let d = Double(distance) else { return nil }
        switch sportType {
        case .run, .bike: return d * 1000
        case .swim: return d
        case .gym, .hiit: return nil
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                // Sport indicator
                Section {
                    HStack(spacing: AppSpacing.md) {
                        Image(systemName: sportType.icon)
                            .font(.title2)
                            .foregroundStyle(sportType.color)
                            .frame(width: 44, height: 44)
                            .background(sportType.color.opacity(0.15))
                            .clipShape(Circle())
                        VStack(alignment: .leading) {
                            Text("Log \(sportType.rawValue)")
                                .font(AppTypography.titleMedium)
                            Text("Manual entry")
                                .font(AppTypography.captionLarge)
                                .foregroundStyle(.secondary)
                        }
                    }
                }

                // Date
                Section("When") {
                    DatePicker("Date & Time", selection: $workoutDate, in: ...Date())
                }

                // Distance
                if sportType.hasDistance {
                    Section("Distance (\(sportType.distanceUnit))") {
                        TextField(sportType.distancePlaceholder, text: $distance)
                            .keyboardType(.decimalPad)
                    }
                }

                // Duration
                Section("Duration") {
                    HStack(spacing: AppSpacing.md) {
                        DurationField(label: "Hrs", value: $hours)
                        DurationField(label: "Min", value: $minutes)
                        DurationField(label: "Sec", value: $seconds)
                    }
                }

                // Optional fields
                Section("Additional Info (optional)") {
                    HStack {
                        Image(systemName: "flame.fill")
                            .foregroundStyle(AppTheme.secondary)
                        TextField("Calories burned", text: $calories)
                            .keyboardType(.numberPad)
                    }
                    HStack {
                        Image(systemName: "heart.fill")
                            .foregroundStyle(.red)
                        TextField("Avg heart rate (bpm)", text: $avgHeartRate)
                            .keyboardType(.numberPad)
                    }
                }

                // Notes
                Section("Notes (optional)") {
                    TextField("How did it feel?", text: $notes, axis: .vertical)
                        .lineLimit(3...6)
                }

                // Save button
                Section {
                    Button {
                        saveWorkout()
                    } label: {
                        HStack {
                            Spacer()
                            if isSaving {
                                ProgressView()
                                    .tint(.white)
                            } else {
                                Image(systemName: sportType.icon)
                                Text("Save \(sportType.rawValue)")
                                    .fontWeight(.bold)
                            }
                            Spacer()
                        }
                    }
                    .disabled(!isValid || isSaving)
                    .listRowBackground(isValid ? sportType.color : sportType.color.opacity(0.3))
                    .foregroundStyle(.white)
                }
            }
            .navigationTitle("Log \(sportType.rawValue)")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
            .alert("Error", isPresented: $showError) {
                Button("OK") {}
            } message: {
                Text(errorMessage)
            }
        }
    }

    private func saveWorkout() {
        isSaving = true

        let endDate = workoutDate
        let startDate = endDate.addingTimeInterval(-totalSeconds)

        // Save to SwiftData
        switch sportType {
        case .run:
            let distKm = (distanceMeters ?? 0) / 1000.0
            let paceSecondsPerKm = distKm > 0 ? totalSeconds / distKm : 0
            let run = Run(
                startTime: startDate,
                endTime: endDate,
                distanceMeters: distanceMeters ?? 0,
                durationMillis: Int64(totalSeconds * 1000),
                avgPaceSecondsPerKm: paceSecondsPerKm,
                maxPaceSecondsPerKm: paceSecondsPerKm,
                avgHeartRate: Int(avgHeartRate),
                caloriesBurned: Int(calories) ?? estimateCalories(),
                notes: notes.isEmpty ? nil : notes,
                source: .manual,
                isCompleted: true
            )
            modelContext.insert(run)

        case .gym:
            let workout = GymWorkout(
                name: "Manual Gym Session",
                startTime: startDate,
                endTime: endDate,
                notes: notes.isEmpty ? nil : notes,
                isCompleted: true
            )
            modelContext.insert(workout)

        default:
            break
        }

        // Save to HealthKit
        saveToHealthKit(startDate: startDate, endDate: endDate)

        isSaving = false
        dismiss()
    }

    private func saveToHealthKit(startDate: Date, endDate: Date) {
        guard HKHealthStore.isHealthDataAvailable() else { return }

        let healthStore = HKHealthStore()
        let distMeters = distanceMeters

        let configuration = HKWorkoutConfiguration()
        configuration.activityType = sportType.hkWorkoutType

        let builder = HKWorkoutBuilder(healthStore: healthStore, configuration: configuration, device: .local())

        Task {
            do {
                try await builder.beginCollection(at: startDate)

                if let dist = distMeters, dist > 0 {
                    let distTypeId: HKQuantityTypeIdentifier = sportType == .bike ? .distanceCycling :
                        sportType == .swim ? .distanceSwimming : .distanceWalkingRunning
                    if let distType = HKQuantityType.quantityType(forIdentifier: distTypeId) {
                        let sample = HKQuantitySample(
                            type: distType,
                            quantity: HKQuantity(unit: .meter(), doubleValue: dist),
                            start: startDate,
                            end: endDate
                        )
                        try await builder.addSamples([sample])
                    }
                }

                let cal = Double(calories) ?? Double(estimateCalories())
                if cal > 0, let energyType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) {
                    let sample = HKQuantitySample(
                        type: energyType,
                        quantity: HKQuantity(unit: .kilocalorie(), doubleValue: cal),
                        start: startDate,
                        end: endDate
                    )
                    try await builder.addSamples([sample])
                }

                try await builder.endCollection(at: endDate)
                try await builder.finishWorkout()
            } catch {
                // HealthKit save failure is non-blocking
                print("HealthKit save failed: \(error)")
            }
        }
    }

    private func estimateCalories() -> Int {
        let minutes = totalSeconds / 60
        let metValue: Double
        switch sportType {
        case .run: metValue = 9.8
        case .swim: metValue = 8.0
        case .bike: metValue = 7.5
        case .gym: metValue = 5.0
        case .hiit: metValue = 12.0
        }
        // Rough estimate: MET * weight(kg) * hours
        let weightKg = 75.0
        return Int(metValue * weightKg * (minutes / 60.0))
    }
}

// MARK: - Duration Field

private struct DurationField: View {
    let label: String
    @Binding var value: String

    var body: some View {
        VStack(spacing: 4) {
            Text(label)
                .font(AppTypography.captionSmall)
                .foregroundStyle(.secondary)
            TextField("0", text: $value)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.center)
                .font(AppTypography.titleMedium)
                .frame(width: 60)
                .padding(.vertical, AppSpacing.sm)
                .background(Color(.systemGray6))
                .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.small))
        }
    }
}

#Preview {
    ManualWorkoutSheet(sportType: .run)
}
