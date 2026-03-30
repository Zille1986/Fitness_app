import SwiftUI
import Charts

struct MoodTrackerScreen: View {
    @Bindable var viewModel: MindfulnessViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var selectedTab = 0

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("View", selection: $selectedTab) {
                    Text("Log").tag(0)
                    Text("History").tag(1)
                    Text("Trends").tag(2)
                }
                .pickerStyle(.segmented)
                .padding()

                ScrollView {
                    switch selectedTab {
                    case 0:
                        logMoodView
                    case 1:
                        moodHistoryView
                    default:
                        moodTrendsView
                    }
                }
            }
            .background(Color(hex: "121212"))
            .navigationTitle("Mood Tracker")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    if viewModel.showingMoodLog {
                        Button("Cancel") {
                            viewModel.resetMoodForm()
                            dismiss()
                        }
                    }
                }
            }
        }
    }

    // MARK: - Log Mood View

    private var logMoodView: some View {
        VStack(spacing: 24) {
            // Mood
            moodSelector(
                title: "How are you feeling?",
                items: MoodLevel.allCases,
                selected: viewModel.selectedMood,
                emoji: \.emoji,
                label: \.label
            ) { viewModel.selectedMood = $0 }

            // Energy
            moodSelector(
                title: "Energy Level",
                items: EnergyLevel.allCases,
                selected: viewModel.selectedEnergy,
                emoji: \.emoji,
                label: \.label
            ) { viewModel.selectedEnergy = $0 }

            // Stress
            moodSelector(
                title: "Stress Level",
                items: StressLevel.allCases,
                selected: viewModel.selectedStress,
                emoji: \.emoji,
                label: \.label
            ) { viewModel.selectedStress = $0 }

            // Notes
            VStack(alignment: .leading, spacing: 8) {
                Text("Notes (optional)")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(.white)

                TextEditor(text: $viewModel.moodNotes)
                    .frame(minHeight: 80)
                    .padding(8)
                    .background(Color(hex: "1E1E1E"))
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .foregroundStyle(.white)
                    .scrollContentBackground(.hidden)
            }

            // Save
            Button {
                viewModel.logMood()
                dismiss()
            } label: {
                HStack {
                    Image(systemName: "checkmark.circle.fill")
                    Text("Save Mood Entry")
                }
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color(hex: "4CAF50"))
                .foregroundStyle(.white)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
        .padding()
    }

    private func moodSelector<T: Hashable>(
        title: String,
        items: [T],
        selected: T,
        emoji: KeyPath<T, String>,
        label: KeyPath<T, String>,
        onSelect: @escaping (T) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(.white)

            HStack(spacing: 0) {
                ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                    Button {
                        withAnimation(.easeInOut(duration: 0.15)) { onSelect(item) }
                    } label: {
                        VStack(spacing: 4) {
                            Text(item[keyPath: emoji])
                                .font(.system(size: 32))
                            Text(item[keyPath: label])
                                .font(.caption2)
                                .foregroundStyle(selected == item ? .white : .secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(
                            selected == item
                                ? Color(hex: "4CAF50").opacity(0.2)
                                : Color.clear
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }
            }
            .padding(4)
            .background(Color(hex: "1E1E1E"))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    // MARK: - Mood History View

    private var moodHistoryView: some View {
        VStack(spacing: 16) {
            if viewModel.recentMoodEntries.isEmpty {
                emptyHistoryView
            } else {
                // Calendar-style mood grid
                moodCalendarGrid

                // Entry list
                ForEach(viewModel.recentMoodEntries.prefix(20)) { entry in
                    MoodEntryRow(entry: entry)
                }
            }
        }
        .padding()
    }

    private var emptyHistoryView: some View {
        VStack(spacing: 16) {
            Image(systemName: "calendar.badge.plus")
                .font(.system(size: 48))
                .foregroundStyle(Color(hex: "FF9800"))

            Text("No Mood Entries Yet")
                .font(.headline)
                .foregroundStyle(.white)

            Text("Start logging your mood daily to track patterns and correlations with your training.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            Button {
                selectedTab = 0
            } label: {
                Text("Log Your First Entry")
                    .font(.subheadline.weight(.semibold))
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color(hex: "4CAF50"))
                    .foregroundStyle(.white)
                    .clipShape(Capsule())
            }
        }
        .padding(.vertical, 40)
    }

    private var moodCalendarGrid: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Last 30 Days")
                .font(.subheadline.weight(.bold))
                .foregroundStyle(.white)

            let calendar = Calendar.current
            let today = calendar.startOfDay(for: Date())
            let days = (0..<30).compactMap { calendar.date(byAdding: .day, value: -$0, to: today) }

            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 4), count: 7), spacing: 4) {
                ForEach(days.reversed(), id: \.self) { day in
                    let entry = viewModel.recentMoodEntries.first { calendar.isDate($0.timestamp, inSameDayAs: day) }
                    MoodDayCell(date: day, mood: entry?.mood)
                }
            }

            // Legend
            HStack(spacing: 16) {
                ForEach(MoodLevel.allCases, id: \.self) { mood in
                    HStack(spacing: 4) {
                        Circle()
                            .fill(moodColor(mood))
                            .frame(width: 8, height: 8)
                        Text(mood.label)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .padding(.top, 4)
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Mood Trends View

    private var moodTrendsView: some View {
        VStack(spacing: 20) {
            // Average mood card
            averageMoodCard

            // Mood over time chart
            moodTimeChart

            // Energy over time chart
            energyTimeChart

            // Stress over time chart
            stressTimeChart
        }
        .padding()
    }

    private var averageMoodCard: some View {
        let avg = viewModel.averageMoodThisWeek
        let emoji: String
        let label: String
        switch avg {
        case 4.5...5: emoji = "😄"; label = "Great"
        case 3.5..<4.5: emoji = "🙂"; label = "Good"
        case 2.5..<3.5: emoji = "😐"; label = "Neutral"
        case 1.5..<2.5: emoji = "😔"; label = "Low"
        default: emoji = "😢"; label = "Very Low"
        }

        return VStack(spacing: 8) {
            Text("Weekly Average")
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(emoji)
                .font(.system(size: 48))
            Text(label)
                .font(.headline)
                .foregroundStyle(.white)
            Text(String(format: "%.1f / 5.0", avg))
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(24)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private var moodTimeChart: some View {
        chartSection(title: "Mood", icon: "face.smiling") {
            if viewModel.recentMoodEntries.isEmpty {
                Text("No data yet")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(height: 150)
            } else {
                Chart {
                    ForEach(viewModel.recentMoodEntries.prefix(14)) { entry in
                        LineMark(
                            x: .value("Date", entry.timestamp, unit: .day),
                            y: .value("Mood", entry.mood.rawValue)
                        )
                        .foregroundStyle(Color(hex: "4CAF50"))
                        .lineStyle(StrokeStyle(lineWidth: 2))

                        PointMark(
                            x: .value("Date", entry.timestamp, unit: .day),
                            y: .value("Mood", entry.mood.rawValue)
                        )
                        .foregroundStyle(moodColor(entry.mood))
                        .symbolSize(40)
                    }
                }
                .chartYScale(domain: 1...5)
                .chartYAxis {
                    AxisMarks(values: [1, 2, 3, 4, 5]) { value in
                        AxisValueLabel {
                            if let intValue = value.as(Int.self) {
                                Text(MoodLevel(rawValue: intValue)?.emoji ?? "")
                            }
                        }
                    }
                }
                .chartXAxis {
                    AxisMarks(values: .stride(by: .day, count: 2)) { _ in
                        AxisValueLabel(format: .dateTime.day().month(.abbreviated))
                    }
                }
                .frame(height: 150)
            }
        }
    }

    private var energyTimeChart: some View {
        chartSection(title: "Energy", icon: "bolt.fill") {
            if viewModel.recentMoodEntries.isEmpty {
                Text("No data yet")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(height: 150)
            } else {
                Chart {
                    ForEach(viewModel.recentMoodEntries.prefix(14)) { entry in
                        BarMark(
                            x: .value("Date", entry.timestamp, unit: .day),
                            y: .value("Energy", entry.energy.rawValue)
                        )
                        .foregroundStyle(
                            entry.energy.rawValue >= 4 ? Color(hex: "4CAF50") :
                            entry.energy.rawValue >= 3 ? Color(hex: "FF9800") :
                            Color(hex: "F44336")
                        )
                        .cornerRadius(4)
                    }
                }
                .chartYScale(domain: 0...5)
                .frame(height: 150)
            }
        }
    }

    private var stressTimeChart: some View {
        chartSection(title: "Stress", icon: "brain.head.profile") {
            if viewModel.recentMoodEntries.isEmpty {
                Text("No data yet")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(height: 150)
            } else {
                Chart {
                    ForEach(viewModel.recentMoodEntries.prefix(14)) { entry in
                        AreaMark(
                            x: .value("Date", entry.timestamp, unit: .day),
                            y: .value("Stress", entry.stress.rawValue)
                        )
                        .foregroundStyle(
                            LinearGradient(
                                colors: [Color(hex: "F44336").opacity(0.3), Color.clear],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )

                        LineMark(
                            x: .value("Date", entry.timestamp, unit: .day),
                            y: .value("Stress", entry.stress.rawValue)
                        )
                        .foregroundStyle(Color(hex: "F44336"))
                        .lineStyle(StrokeStyle(lineWidth: 2))
                    }
                }
                .chartYScale(domain: 0...5)
                .frame(height: 150)
            }
        }
    }

    private func chartSection<Content: View>(title: String, icon: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 8) {
                Image(systemName: icon)
                    .foregroundStyle(Color(hex: "FF9800"))
                Text(title)
                    .font(.headline)
                    .foregroundStyle(.white)
            }
            content()
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Helpers

    private func moodColor(_ mood: MoodLevel) -> Color {
        switch mood {
        case .veryLow: return Color(hex: "F44336")
        case .low: return Color(hex: "FF9800")
        case .neutral: return Color(hex: "FFC107")
        case .good: return Color(hex: "8BC34A")
        case .great: return Color(hex: "4CAF50")
        }
    }
}

// MARK: - Mood Day Cell

private struct MoodDayCell: View {
    let date: Date
    let mood: MoodLevel?

    private var dayNumber: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "d"
        return formatter.string(from: date)
    }

    var body: some View {
        VStack(spacing: 2) {
            if let mood = mood {
                Text(mood.emoji)
                    .font(.caption)
            } else {
                Circle()
                    .fill(Color.white.opacity(0.1))
                    .frame(width: 14, height: 14)
            }
            Text(dayNumber)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(height: 40)
    }
}

// MARK: - Mood Entry Row

private struct MoodEntryRow: View {
    let entry: MoodEntry

    var body: some View {
        HStack(spacing: 12) {
            Text(entry.mood.emoji)
                .font(.title2)

            VStack(alignment: .leading, spacing: 2) {
                Text(entry.mood.label)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(.white)
                HStack(spacing: 8) {
                    Label(entry.energy.label, systemImage: "bolt")
                    Label(entry.stress.label, systemImage: "brain.head.profile")
                }
                .font(.caption)
                .foregroundStyle(.secondary)

                if !entry.notes.isEmpty {
                    Text(entry.notes)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
            }

            Spacer()

            Text(entry.timestamp, style: .time)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(12)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

#Preview {
    MoodTrackerScreen(viewModel: MindfulnessViewModel())
        .preferredColorScheme(.dark)
}
