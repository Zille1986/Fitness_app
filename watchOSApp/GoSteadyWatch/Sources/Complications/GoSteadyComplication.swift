import WidgetKit
import SwiftUI
import HealthKit

// MARK: - Timeline Provider
//
// Note: These complications should be placed in a separate WidgetKit extension target.
// The @main WidgetBundle entry point would live in that extension's main file.
// This file provides the complication implementations ready for that target.

struct GoSteadyTimelineProvider: TimelineProvider {

    func placeholder(in context: Context) -> GoSteadyEntry {
        GoSteadyEntry(date: Date(), heartRate: 72, steps: 5432, calories: 234, moveProgress: 0.6)
    }

    func getSnapshot(in context: Context, completion: @escaping (GoSteadyEntry) -> Void) {
        let entry = GoSteadyEntry(date: Date(), heartRate: 72, steps: 5432, calories: 234, moveProgress: 0.6)
        completion(entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<GoSteadyEntry>) -> Void) {
        fetchHealthData { entry in
            let nextUpdate = Calendar.current.date(byAdding: .minute, value: 15, to: Date())!
            let timeline = Timeline(entries: [entry], policy: .after(nextUpdate))
            completion(timeline)
        }
    }

    private func fetchHealthData(completion: @escaping (GoSteadyEntry) -> Void) {
        let healthStore = HKHealthStore()
        let calendar = Calendar.current
        let startOfDay = calendar.startOfDay(for: Date())
        let predicate = HKQuery.predicateForSamples(withStart: startOfDay, end: Date(), options: .strictStartDate)

        var heartRate = 0
        var steps = 0
        var calories = 0.0
        var moveProgress = 0.0

        let group = DispatchGroup()

        // Heart rate
        group.enter()
        if let hrType = HKQuantityType.quantityType(forIdentifier: .heartRate) {
            let sort = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)
            let query = HKSampleQuery(sampleType: hrType, predicate: nil, limit: 1, sortDescriptors: [sort]) { _, samples, _ in
                if let sample = samples?.first as? HKQuantitySample {
                    heartRate = Int(sample.quantity.doubleValue(for: .count().unitDivided(by: .minute())))
                }
                group.leave()
            }
            healthStore.execute(query)
        } else {
            group.leave()
        }

        // Steps
        group.enter()
        if let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount) {
            let query = HKStatisticsQuery(quantityType: stepType, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, stats, _ in
                steps = Int(stats?.sumQuantity()?.doubleValue(for: .count()) ?? 0)
                group.leave()
            }
            healthStore.execute(query)
        } else {
            group.leave()
        }

        // Calories
        group.enter()
        if let calType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned) {
            let query = HKStatisticsQuery(quantityType: calType, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, stats, _ in
                calories = stats?.sumQuantity()?.doubleValue(for: .kilocalorie()) ?? 0
                group.leave()
            }
            healthStore.execute(query)
        } else {
            group.leave()
        }

        // Activity rings
        group.enter()
        let components = calendar.dateComponents([.era, .year, .month, .day], from: Date())
        let endDate = calendar.date(byAdding: .day, value: 1, to: calendar.date(from: components)!)!
        let endComponents = calendar.dateComponents([.era, .year, .month, .day], from: endDate)
        let ringPredicate = HKQuery.predicate(forActivitySummariesBetweenStart: components, end: endComponents)

        let ringQuery = HKActivitySummaryQuery(predicate: ringPredicate) { _, summaries, _ in
            if let summary = summaries?.first {
                let goal = summary.activeEnergyBurnedGoal.doubleValue(for: .kilocalorie())
                let actual = summary.activeEnergyBurned.doubleValue(for: .kilocalorie())
                moveProgress = goal > 0 ? min(actual / goal, 1.0) : 0
            }
            group.leave()
        }
        healthStore.execute(ringQuery)

        group.notify(queue: .main) {
            let entry = GoSteadyEntry(
                date: Date(),
                heartRate: heartRate,
                steps: steps,
                calories: Int(calories),
                moveProgress: moveProgress
            )
            completion(entry)
        }
    }
}

// MARK: - Entry

struct GoSteadyEntry: TimelineEntry {
    let date: Date
    let heartRate: Int
    let steps: Int
    let calories: Int
    let moveProgress: Double
}

// MARK: - Complication Views

struct GoSteadyHeartRateView: View {
    let entry: GoSteadyEntry

    var body: some View {
        VStack(spacing: 2) {
            Image(systemName: "heart.fill")
                .foregroundStyle(.red)
                .font(.caption2)
            Text("\(entry.heartRate)")
                .font(.system(size: 20, weight: .bold))
            Text("bpm")
                .font(.system(size: 8))
                .foregroundStyle(.secondary)
        }
        .containerBackground(.black, for: .widget)
    }
}

struct GoSteadyRingsView: View {
    let entry: GoSteadyEntry

    var body: some View {
        ZStack {
            MiniActivityRing(progress: entry.moveProgress, color: .red, size: 36, lineWidth: 4)
            Text(String(format: "%.0f%%", entry.moveProgress * 100))
                .font(.system(size: 8, weight: .bold))
        }
        .containerBackground(.black, for: .widget)
    }
}

struct GoSteadyStepsView: View {
    let entry: GoSteadyEntry

    var body: some View {
        VStack(spacing: 2) {
            Image(systemName: "figure.walk")
                .foregroundStyle(Color(hex: "4CAF50"))
                .font(.caption2)
            Text("\(entry.steps)")
                .font(.system(size: 16, weight: .bold))
            Text("steps")
                .font(.system(size: 8))
                .foregroundStyle(.secondary)
        }
        .containerBackground(.black, for: .widget)
    }
}

struct GoSteadyCaloriesView: View {
    let entry: GoSteadyEntry

    var body: some View {
        VStack(spacing: 2) {
            Image(systemName: "flame.fill")
                .foregroundStyle(.orange)
                .font(.caption2)
            Text("\(entry.calories)")
                .font(.system(size: 16, weight: .bold))
            Text("kcal")
                .font(.system(size: 8))
                .foregroundStyle(.secondary)
        }
        .containerBackground(.black, for: .widget)
    }
}

// MARK: - Widget Configurations
// Move these to a separate WidgetKit extension target with its own @main entry point

struct GoSteadyHeartRateWidget: Widget {
    let kind = "GoSteadyHeartRate"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: GoSteadyTimelineProvider()) { entry in
            GoSteadyHeartRateView(entry: entry)
        }
        .configurationDisplayName("Heart Rate")
        .description("Current heart rate")
        .supportedFamilies([.accessoryCircular, .accessoryCorner, .accessoryInline])
    }
}

struct GoSteadyRingsWidget: Widget {
    let kind = "GoSteadyRings"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: GoSteadyTimelineProvider()) { entry in
            GoSteadyRingsView(entry: entry)
        }
        .configurationDisplayName("Activity Rings")
        .description("Daily move progress")
        .supportedFamilies([.accessoryCircular, .accessoryCorner])
    }
}

struct GoSteadyStepsWidget: Widget {
    let kind = "GoSteadySteps"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: GoSteadyTimelineProvider()) { entry in
            GoSteadyStepsView(entry: entry)
        }
        .configurationDisplayName("Steps")
        .description("Daily step count")
        .supportedFamilies([.accessoryCircular, .accessoryCorner, .accessoryInline])
    }
}

struct GoSteadyCaloriesWidget: Widget {
    let kind = "GoSteadyCalories"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: GoSteadyTimelineProvider()) { entry in
            GoSteadyCaloriesView(entry: entry)
        }
        .configurationDisplayName("Active Calories")
        .description("Daily active calories burned")
        .supportedFamilies([.accessoryCircular, .accessoryCorner, .accessoryInline])
    }
}

// In a separate WidgetKit extension target, use this as the @main entry point:
//
// @main
// struct GoSteadyWidgetBundle: WidgetBundle {
//     var body: some Widget {
//         GoSteadyHeartRateWidget()
//         GoSteadyRingsWidget()
//         GoSteadyStepsWidget()
//         GoSteadyCaloriesWidget()
//     }
// }
