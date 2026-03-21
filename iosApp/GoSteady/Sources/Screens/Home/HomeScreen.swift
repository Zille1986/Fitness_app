import SwiftUI
import HealthKit

struct HomeScreen: View {
    @State private var userName = "Athlete"
    @State private var sleepHours: Double?
    @State private var todaySteps: Int = 0

    private let healthStore = HKHealthStore()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // Greeting
                    VStack(alignment: .leading, spacing: 4) {
                        Text(greeting)
                            .font(.subheadline)
                            .foregroundStyle(AppTheme.onSurfaceVariant)
                        Text(userName)
                            .font(.largeTitle)
                            .fontWeight(.bold)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal)

                    // Main Session Card (editorial hero)
                    MainSessionCard()

                    // Weekly Activity
                    WeeklyActivityGrid()

                    // Readiness Score
                    ReadinessCard(sleepHours: sleepHours)

                    // Nutrition
                    NutritionCard()
                }
                .padding(.vertical)
            }
            .background(AppTheme.surface)
            .navigationTitle("")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    NavigationLink(destination: Text("Profile")) {
                        Image(systemName: "person.circle")
                            .foregroundStyle(AppTheme.primary)
                    }
                }
            }
        }
        .task {
            await requestHealthData()
        }
    }

    private var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 0..<12: return "Good morning"
        case 12..<17: return "Good afternoon"
        default: return "Good evening"
        }
    }

    private func requestHealthData() async {
        guard HKHealthStore.isHealthDataAvailable() else { return }

        let sleepType = HKObjectType.categoryType(forIdentifier: .sleepAnalysis)!
        let stepType = HKObjectType.quantityType(forIdentifier: .stepCount)!

        do {
            try await healthStore.requestAuthorization(toShare: [], read: [sleepType, stepType])
            // Read last night's sleep
            sleepHours = await readLastNightSleep()
            todaySteps = await readTodaySteps()
        } catch {
            print("HealthKit auth failed: \(error)")
        }
    }

    private func readLastNightSleep() async -> Double? {
        let sleepType = HKObjectType.categoryType(forIdentifier: .sleepAnalysis)!
        let yesterday6pm = Calendar.current.date(bySettingHour: 18, minute: 0, second: 0, of: Calendar.current.date(byAdding: .day, value: -1, to: Date())!)!
        let predicate = HKQuery.predicateForSamples(withStart: yesterday6pm, end: Date())

        return await withCheckedContinuation { continuation in
            let query = HKSampleQuery(sampleType: sleepType, predicate: predicate, limit: HKObjectQueryNoLimit, sortDescriptors: nil) { _, samples, _ in
                guard let samples = samples as? [HKCategorySample] else {
                    continuation.resume(returning: nil)
                    return
                }
                let asleepSamples = samples.filter { $0.value != HKCategoryValueSleepAnalysis.inBed.rawValue }
                let totalSeconds = asleepSamples.reduce(0.0) { $0 + $1.endDate.timeIntervalSince($1.startDate) }
                continuation.resume(returning: totalSeconds / 3600.0)
            }
            healthStore.execute(query)
        }
    }

    private func readTodaySteps() async -> Int {
        let stepType = HKObjectType.quantityType(forIdentifier: .stepCount)!
        let startOfDay = Calendar.current.startOfDay(for: Date())
        let predicate = HKQuery.predicateForSamples(withStart: startOfDay, end: Date())

        return await withCheckedContinuation { continuation in
            let query = HKStatisticsQuery(quantityType: stepType, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, result, _ in
                let steps = result?.sumQuantity()?.doubleValue(for: .count()) ?? 0
                continuation.resume(returning: Int(steps))
            }
            healthStore.execute(query)
        }
    }
}

// MARK: - Subviews

struct MainSessionCard: View {
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            // Background image placeholder
            Rectangle()
                .fill(
                    LinearGradient(
                        colors: [AppTheme.primary.opacity(0.3), AppTheme.primary.opacity(0.8)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .frame(height: 280)
                .clipShape(RoundedRectangle(cornerRadius: 16))

            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 8) {
                    Text("FREE RUN")
                        .font(.caption2)
                        .fontWeight(.bold)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 4)
                        .background(AppTheme.primary)
                        .foregroundStyle(.white)
                        .clipShape(Capsule())

                    Text("45 Minutes")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.8))
                }

                Text("Ready to Train")
                    .font(.title)
                    .fontWeight(.heavy)
                    .foregroundStyle(.white)

                Text("Start a new session and push your limits.")
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.7))

                HStack(spacing: 12) {
                    Button("Start Session") {}
                        .buttonStyle(.borderedProminent)
                        .tint(AppTheme.primaryContainer)
                        .foregroundStyle(AppTheme.onPrimaryContainer)

                    Button("View Details") {}
                        .buttonStyle(.bordered)
                        .tint(.white.opacity(0.3))
                        .foregroundStyle(.white)
                }
                .padding(.top, 8)
            }
            .padding(20)
        }
        .padding(.horizontal)
    }
}

struct WeeklyActivityGrid: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("THIS WEEK")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.onSurfaceVariant)
                .padding(.horizontal)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                ActivityCard(icon: "figure.run", label: "Run", value: "0", sub: "0.0 km", color: AppTheme.running)
                ActivityCard(icon: "figure.pool.swim", label: "Swim", value: "0", sub: "0 m", color: AppTheme.swimming)
                ActivityCard(icon: "bicycle", label: "Bike", value: "0", sub: "0.0 km", color: AppTheme.cycling)
                ActivityCard(icon: "dumbbell.fill", label: "Gym", value: "0", sub: "0 sets", color: AppTheme.gym)
            }
            .padding(.horizontal)
        }
    }
}

struct ActivityCard: View {
    let icon: String
    let label: String
    let value: String
    let sub: String
    let color: Color

    var body: some View {
        HStack {
            Image(systemName: icon)
                .foregroundStyle(color)
                .frame(width: 36, height: 36)
                .background(color.opacity(0.15))
                .clipShape(Circle())

            VStack(alignment: .leading) {
                Text(label)
                    .font(.caption2)
                    .foregroundStyle(AppTheme.onSurfaceVariant)
                HStack(alignment: .bottom, spacing: 4) {
                    Text(value).fontWeight(.bold)
                    Text(sub).font(.caption2).foregroundStyle(AppTheme.onSurfaceVariant)
                }
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(color.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

struct ReadinessCard: View {
    let sleepHours: Double?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("READINESS")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.primary)

            HStack {
                Text("Sleep")
                Spacer()
                if let hours = sleepHours {
                    let h = Int(hours)
                    let m = Int((hours - Double(h)) * 60)
                    Text("\(h)h \(m)m")
                        .fontWeight(.medium)
                        .foregroundStyle(hours >= 7 ? .green : hours >= 5.5 ? .orange : .red)
                } else {
                    Text("No data")
                        .foregroundStyle(AppTheme.onSurfaceVariant)
                }
            }
        }
        .padding()
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }
}

struct NutritionCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("NUTRITION")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.cycling)

            Text("No data \u{2014} tap to log a meal")
                .font(.subheadline)
                .foregroundStyle(AppTheme.onSurfaceVariant)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }
}
