import SwiftUI
import Charts

struct ReadinessDetailScreen: View {
    @Bindable var viewModel: WellnessViewModel

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 20) {
                // Readiness Score Ring
                readinessScoreSection

                // Breakdown
                breakdownSection

                // HRV Chart
                hrvChartSection

                // Resting HR Chart
                restingHRChartSection

                // Sleep Analysis
                sleepAnalysisSection

                // Training Load Detail
                trainingLoadSection

                // Mood Correlation
                moodCorrelationSection

                Spacer(minLength: 40)
            }
            .padding()
        }
        .background(Color(hex: "121212"))
        .navigationTitle("Readiness Detail")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Readiness Score

    private var readinessScoreSection: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .stroke(Color.gray.opacity(0.2), lineWidth: 14)
                Circle()
                    .trim(from: 0, to: CGFloat(viewModel.readinessScore) / 100.0)
                    .stroke(
                        scoreGradient,
                        style: StrokeStyle(lineWidth: 14, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                    .animation(.easeInOut(duration: 1.2), value: viewModel.readinessScore)

                VStack(spacing: 4) {
                    Text("\(viewModel.readinessScore)")
                        .font(.system(size: 56, weight: .bold, design: .rounded))
                        .foregroundStyle(.white)
                    Text("Readiness")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .frame(width: 180, height: 180)

            Text(readinessMessage)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
    }

    private var scoreGradient: AngularGradient {
        AngularGradient(
            gradient: Gradient(colors: [Color(hex: "F44336"), Color(hex: "FF9800"), Color(hex: "4CAF50")]),
            center: .center,
            startAngle: .degrees(-90),
            endAngle: .degrees(270)
        )
    }

    private var readinessMessage: String {
        switch viewModel.readinessScore {
        case 80...100: return "You're fully recovered and ready for a hard session!"
        case 60..<80: return "Good recovery. Moderate to hard training is fine."
        case 40..<60: return "Moderate readiness. Consider an easier day."
        default: return "Low readiness. Rest or very light activity recommended."
        }
    }

    // MARK: - Breakdown

    private var breakdownSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Score Breakdown")
                .font(.headline)
                .foregroundStyle(.white)

            VStack(spacing: 12) {
                BreakdownBar(label: "Sleep", value: viewModel.readinessBreakdown.sleepScore, maxValue: 30, colorHex: "2196F3", icon: "moon.zzz.fill")
                BreakdownBar(label: "Recovery", value: viewModel.readinessBreakdown.recoveryScore, maxValue: 30, colorHex: "4CAF50", icon: "heart.fill")
                BreakdownBar(label: "Stress", value: viewModel.readinessBreakdown.stressScore, maxValue: 20, colorHex: "FF9800", icon: "brain.head.profile")
                BreakdownBar(label: "Training Load", value: viewModel.readinessBreakdown.trainingLoadScore, maxValue: 20, colorHex: "E91E63", icon: "figure.run")
            }
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - HRV Chart

    private var hrvChartSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "waveform.path.ecg")
                    .foregroundStyle(Color(hex: "4CAF50"))
                Text("Heart Rate Variability")
                    .font(.headline)
                    .foregroundStyle(.white)
                Spacer()
                Text(viewModel.hrvTrendDirection)
                    .font(.caption.weight(.medium))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(trendColor(viewModel.hrvTrendDirection).opacity(0.2))
                    .foregroundStyle(trendColor(viewModel.hrvTrendDirection))
                    .clipShape(Capsule())
            }

            if viewModel.weeklyHRVData.isEmpty {
                emptyChartPlaceholder(message: "HRV data will appear after syncing with HealthKit")
            } else {
                Chart {
                    // Baseline reference
                    RuleMark(y: .value("Baseline", viewModel.hrvBaseline))
                        .foregroundStyle(Color.white.opacity(0.3))
                        .lineStyle(StrokeStyle(dash: [5, 5]))
                        .annotation(position: .trailing, alignment: .leading) {
                            Text("Baseline")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }

                    ForEach(viewModel.weeklyHRVData) { point in
                        LineMark(
                            x: .value("Date", point.date, unit: .day),
                            y: .value("HRV", point.value)
                        )
                        .foregroundStyle(Color(hex: "4CAF50"))
                        .lineStyle(StrokeStyle(lineWidth: 2))

                        PointMark(
                            x: .value("Date", point.date, unit: .day),
                            y: .value("HRV", point.value)
                        )
                        .foregroundStyle(Color(hex: "4CAF50"))
                        .symbolSize(30)

                        AreaMark(
                            x: .value("Date", point.date, unit: .day),
                            y: .value("HRV", point.value)
                        )
                        .foregroundStyle(
                            LinearGradient(
                                colors: [Color(hex: "4CAF50").opacity(0.3), Color.clear],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                        )
                    }
                }
                .chartXAxis {
                    AxisMarks(values: .stride(by: .day)) { _ in
                        AxisValueLabel(format: .dateTime.weekday(.abbreviated))
                    }
                }
                .frame(height: 180)
            }

            if let current = viewModel.currentHRV {
                HStack {
                    Label(String(format: "Current: %.0f ms", current), systemImage: "heart.fill")
                        .font(.caption)
                        .foregroundStyle(Color(hex: "4CAF50"))
                    Spacer()
                    Label(String(format: "Baseline: %.0f ms", viewModel.hrvBaseline), systemImage: "chart.line.flattrend.xyaxis")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Resting HR

    private var restingHRChartSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "heart.fill")
                    .foregroundStyle(Color(hex: "E91E63"))
                Text("Resting Heart Rate")
                    .font(.headline)
                    .foregroundStyle(.white)
            }

            if viewModel.weeklyRestingHR.isEmpty {
                emptyChartPlaceholder(message: "Resting HR data will appear after syncing with HealthKit")
            } else {
                Chart {
                    ForEach(viewModel.weeklyRestingHR) { point in
                        LineMark(
                            x: .value("Date", point.date, unit: .day),
                            y: .value("BPM", point.bpm)
                        )
                        .foregroundStyle(Color(hex: "E91E63"))
                        .lineStyle(StrokeStyle(lineWidth: 2))

                        PointMark(
                            x: .value("Date", point.date, unit: .day),
                            y: .value("BPM", point.bpm)
                        )
                        .foregroundStyle(Color(hex: "E91E63"))
                        .symbolSize(30)
                    }
                }
                .chartXAxis {
                    AxisMarks(values: .stride(by: .day)) { _ in
                        AxisValueLabel(format: .dateTime.weekday(.abbreviated))
                    }
                }
                .frame(height: 150)
            }

            if let rhr = viewModel.restingHR {
                Text("Current: \(rhr) bpm")
                    .font(.caption)
                    .foregroundStyle(Color(hex: "E91E63"))
            }
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Sleep Analysis

    private var sleepAnalysisSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "moon.zzz.fill")
                    .foregroundStyle(Color(hex: "2196F3"))
                Text("Sleep Analysis")
                    .font(.headline)
                    .foregroundStyle(.white)
            }

            if viewModel.weeklyWellnessSleepData.isEmpty {
                emptyChartPlaceholder(message: "Sleep data will appear after syncing with HealthKit")
            } else {
                Chart {
                    ForEach(viewModel.weeklyWellnessSleepData) { data in
                        BarMark(
                            x: .value("Date", data.date, unit: .day),
                            y: .value("Hours", data.hours)
                        )
                        .foregroundStyle(
                            data.hours >= 7 ? Color(hex: "4CAF50") :
                            data.hours >= 6 ? Color(hex: "FF9800") :
                            Color(hex: "F44336")
                        )
                        .cornerRadius(4)
                    }

                    RuleMark(y: .value("Target", 7))
                        .foregroundStyle(Color.white.opacity(0.3))
                        .lineStyle(StrokeStyle(dash: [5, 5]))
                }
                .chartYScale(domain: 0...12)
                .chartXAxis {
                    AxisMarks(values: .stride(by: .day)) { _ in
                        AxisValueLabel(format: .dateTime.weekday(.abbreviated))
                    }
                }
                .frame(height: 150)

                HStack {
                    if let lastNight = viewModel.lastNightSleepHours {
                        Label(String(format: "Last Night: %.1fh", lastNight), systemImage: "bed.double.fill")
                            .font(.caption)
                            .foregroundStyle(Color(hex: "2196F3"))
                    }
                    Spacer()
                    Label(String(format: "Avg: %.1fh", viewModel.sleepAverage), systemImage: "chart.bar")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Training Load Detail

    private var trainingLoadSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Image(systemName: "chart.bar.xaxis")
                    .foregroundStyle(Color(hex: "FF9800"))
                Text("Training Load Balance")
                    .font(.headline)
                    .foregroundStyle(.white)
            }

            // ACWR gauge visualization
            VStack(spacing: 8) {
                GeometryReader { geo in
                    let ratio = CGFloat(viewModel.trainingLoad.acuteChronicRatio)
                    let markerPosition = min(max(ratio / 2.0, 0), 1.0) * geo.size.width

                    ZStack(alignment: .leading) {
                        // Background gradient zones
                        HStack(spacing: 0) {
                            Rectangle().fill(Color(hex: "FF9800").opacity(0.3))
                                .frame(width: geo.size.width * 0.4)
                            Rectangle().fill(Color(hex: "4CAF50").opacity(0.3))
                                .frame(width: geo.size.width * 0.25)
                            Rectangle().fill(Color(hex: "FF9800").opacity(0.3))
                                .frame(width: geo.size.width * 0.1)
                            Rectangle().fill(Color(hex: "F44336").opacity(0.3))
                                .frame(width: geo.size.width * 0.25)
                        }
                        .clipShape(RoundedRectangle(cornerRadius: 6))
                        .frame(height: 20)

                        // Marker
                        Circle()
                            .fill(Color(hex: viewModel.trainingLoad.status.colorHex))
                            .frame(width: 16, height: 16)
                            .overlay(Circle().stroke(Color.white, lineWidth: 2))
                            .offset(x: markerPosition - 8)
                    }
                }
                .frame(height: 20)

                HStack {
                    Text("Under-training")
                        .font(.caption2)
                    Spacer()
                    Text("Optimal")
                        .font(.caption2)
                        .foregroundStyle(Color(hex: "4CAF50"))
                    Spacer()
                    Text("Over-training")
                        .font(.caption2)
                }
                .foregroundStyle(.secondary)
            }

            HStack(spacing: 24) {
                VStack(spacing: 4) {
                    Text("\(viewModel.trainingLoad.acuteLoad)")
                        .font(.title3.weight(.bold))
                        .foregroundStyle(.white)
                    Text("Acute (7d)")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity)

                VStack(spacing: 4) {
                    Text(viewModel.acuteChronicRatioFormatted)
                        .font(.title3.weight(.bold))
                        .foregroundStyle(Color(hex: viewModel.trainingLoad.status.colorHex))
                    Text("AC Ratio")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity)

                VStack(spacing: 4) {
                    Text("\(viewModel.trainingLoad.chronicLoad)")
                        .font(.title3.weight(.bold))
                        .foregroundStyle(.white)
                    Text("Chronic (28d)")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity)
            }

            Text("Ideal AC ratio is 0.8-1.3. Values above 1.5 significantly increase injury risk.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Mood Correlation

    private var moodCorrelationSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "face.smiling")
                    .foregroundStyle(Color(hex: "FF9800"))
                Text("Mood Correlation")
                    .font(.headline)
                    .foregroundStyle(.white)
            }

            Text("Track your mood daily to see how it correlates with your training readiness and recovery.")
                .font(.caption)
                .foregroundStyle(.secondary)

            if viewModel.weeklyReadinessScores.count >= 3 {
                Text("Based on your data, readiness scores tend to be higher on days when mood is good or better.")
                    .font(.caption)
                    .foregroundStyle(Color(hex: "4CAF50"))
            }
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Helpers

    private func emptyChartPlaceholder(message: String) -> some View {
        Text(message)
            .font(.caption)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 32)
    }

    private func trendColor(_ trend: String) -> Color {
        switch trend {
        case "Improving": return Color(hex: "4CAF50")
        case "Declining": return Color(hex: "F44336")
        default: return Color(hex: "FF9800")
        }
    }
}

// MARK: - Breakdown Bar

private struct BreakdownBar: View {
    let label: String
    let value: Int
    let maxValue: Int
    let colorHex: String
    let icon: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundStyle(Color(hex: colorHex))
                .frame(width: 20)

            Text(label)
                .font(.caption)
                .foregroundStyle(.white)
                .frame(width: 80, alignment: .leading)

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.white.opacity(0.1))
                        .frame(height: 8)
                    Capsule()
                        .fill(Color(hex: colorHex))
                        .frame(width: geo.size.width * CGFloat(value) / CGFloat(maxValue), height: 8)
                        .animation(.easeInOut(duration: 0.6), value: value)
                }
            }
            .frame(height: 8)

            Text("\(value)/\(maxValue)")
                .font(.caption2.monospacedDigit())
                .foregroundStyle(.secondary)
                .frame(width: 40, alignment: .trailing)
        }
    }
}

#Preview {
    NavigationStack {
        ReadinessDetailScreen(viewModel: WellnessViewModel())
    }
    .preferredColorScheme(.dark)
}
