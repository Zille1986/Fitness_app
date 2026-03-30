import SwiftUI
import Charts

struct WellnessDashboardScreen: View {
    @State private var viewModel = WellnessViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                if viewModel.isLoading {
                    loadingView
                } else {
                    contentView
                }
            }
            .background(Color(hex: "121212"))
            .navigationTitle("Wellness")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        viewModel.refresh()
                    } label: {
                        Image(systemName: "arrow.clockwise")
                            .foregroundStyle(Color(hex: "4CAF50"))
                    }
                }
            }
        }
    }

    private var loadingView: some View {
        VStack(spacing: 16) {
            Spacer(minLength: 200)
            ProgressView()
                .scaleEffect(1.5)
                .tint(Color(hex: "4CAF50"))
            Text("Loading wellness data...")
                .foregroundStyle(.secondary)
            Spacer()
        }
    }

    private var contentView: some View {
        LazyVStack(spacing: 16) {
            // Overall Wellness Score
            WellnessScoreGauge(score: viewModel.overallWellnessScore, status: viewModel.wellnessStatus)

            // Quick Stats
            quickStatsRow

            // Training Load
            trainingLoadCard

            // Readiness Detail Link
            NavigationLink(destination: ReadinessDetailScreen(viewModel: viewModel)) {
                readinessPreviewCard
            }
            .buttonStyle(.plain)

            // Weekly Trends Chart
            weeklyTrendsCard

            // Recommendations
            if !viewModel.recommendations.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Recommendations")
                        .font(.headline)
                        .foregroundStyle(.white)
                        .padding(.horizontal)

                    ForEach(viewModel.recommendations) { rec in
                        RecommendationRow(recommendation: rec)
                    }
                }
            }

            Spacer(minLength: 40)
        }
        .padding(.top)
    }

    // MARK: - Quick Stats

    private var quickStatsRow: some View {
        HStack(spacing: 12) {
            QuickStatBox(icon: "flame.fill", value: "\(viewModel.currentStreak)", label: "Day Streak", colorHex: "FF9800")
            QuickStatBox(icon: "figure.run", value: "\(viewModel.trainingLoad.weeklyRuns)", label: "Runs/Week", colorHex: "4CAF50")
            QuickStatBox(icon: "brain.head.profile", value: "\(viewModel.mindfulnessMinutesThisWeek)", label: "Mindful Min", colorHex: "2196F3")
        }
        .padding(.horizontal)
    }

    // MARK: - Training Load

    private var trainingLoadCard: some View {
        VStack(spacing: 16) {
            HStack {
                Text("Training Load")
                    .font(.headline)
                    .foregroundStyle(.white)
                Spacer()
                Text(viewModel.trainingLoad.status.label)
                    .font(.caption.weight(.medium))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 4)
                    .background(Color(hex: viewModel.trainingLoad.status.colorHex).opacity(0.2))
                    .foregroundStyle(Color(hex: viewModel.trainingLoad.status.colorHex))
                    .clipShape(Capsule())
            }

            HStack(spacing: 0) {
                trainingMetric(value: "\(viewModel.trainingLoad.acuteLoad)", label: "Acute Load")
                Spacer()
                trainingMetric(
                    value: viewModel.acuteChronicRatioFormatted,
                    label: "AC Ratio",
                    colorHex: viewModel.trainingLoad.status.colorHex
                )
                Spacer()
                trainingMetric(value: "\(viewModel.trainingLoad.chronicLoad)", label: "Chronic Load")
            }

            HStack(spacing: 24) {
                Label(String(format: "%.1f km", viewModel.trainingLoad.weeklyDistanceMeters / 1000), systemImage: "location")
                Label("\(viewModel.trainingLoad.weeklyDurationMinutes) min", systemImage: "clock")
                Label("\(viewModel.trainingLoad.weeklyRuns) runs", systemImage: "figure.run")
            }
            .font(.caption)
            .foregroundStyle(.secondary)
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    private func trainingMetric(value: String, label: String, colorHex: String? = nil) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.title2.weight(.bold))
                .foregroundStyle(colorHex != nil ? Color(hex: colorHex!) : .white)
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }

    // MARK: - Readiness Preview

    private var readinessPreviewCard: some View {
        VStack(spacing: 12) {
            HStack {
                Text("Daily Readiness")
                    .font(.headline)
                    .foregroundStyle(.white)
                Spacer()
                HStack(spacing: 4) {
                    Text("Details")
                        .font(.caption)
                    Image(systemName: "chevron.right")
                        .font(.caption2)
                }
                .foregroundStyle(Color(hex: "4CAF50"))
            }

            HStack(spacing: 24) {
                readinessItem(icon: "moon.zzz.fill", value: viewModel.lastNightSleepHours.map { String(format: "%.1fh", $0) } ?? "--", label: "Sleep")
                readinessItem(icon: "heart.fill", value: viewModel.restingHR.map { "\($0)" } ?? "--", label: "RHR")
                readinessItem(icon: "waveform.path.ecg", value: viewModel.currentHRV.map { String(format: "%.0f", $0) } ?? "--", label: "HRV")
                readinessItem(icon: "gauge.medium", value: "\(viewModel.readinessScore)", label: "Score")
            }
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    private func readinessItem(icon: String, value: String, label: String) -> some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(Color(hex: "4CAF50"))
            Text(value)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(.white)
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Weekly Trends

    private var weeklyTrendsCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Weekly Readiness Trend")
                .font(.headline)
                .foregroundStyle(.white)

            if viewModel.weeklyReadinessScores.isEmpty {
                Text("Complete daily check-ins to see trends")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 24)
            } else {
                Chart {
                    ForEach(Array(viewModel.weeklyReadinessScores.enumerated()), id: \.offset) { idx, point in
                        BarMark(
                            x: .value("Day", point.date, unit: .day),
                            y: .value("Score", point.score)
                        )
                        .foregroundStyle(
                            point.score >= 70 ? Color(hex: "4CAF50") :
                            point.score >= 50 ? Color(hex: "FF9800") :
                            Color(hex: "F44336")
                        )
                        .cornerRadius(4)
                    }
                }
                .chartYScale(domain: 0...100)
                .chartXAxis {
                    AxisMarks(values: .stride(by: .day)) { value in
                        AxisValueLabel(format: .dateTime.weekday(.narrow))
                    }
                }
                .chartYAxis {
                    AxisMarks(position: .leading) { value in
                        AxisValueLabel()
                    }
                }
                .frame(height: 120)
            }
        }
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }
}

// MARK: - Wellness Score Gauge

private struct WellnessScoreGauge: View {
    let score: Int
    let status: WellnessStatus
    @State private var animatedScore: Int = 0

    var body: some View {
        VStack(spacing: 16) {
            Text("Overall Wellness")
                .font(.headline)
                .foregroundStyle(.white)

            ZStack {
                Circle()
                    .stroke(Color.gray.opacity(0.2), lineWidth: 12)

                Circle()
                    .trim(from: 0, to: CGFloat(animatedScore) / 100.0)
                    .stroke(
                        Color(hex: status.colorHex),
                        style: StrokeStyle(lineWidth: 12, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                    .animation(.easeInOut(duration: 1.0), value: animatedScore)

                VStack(spacing: 4) {
                    Text("\(animatedScore)")
                        .font(.system(size: 48, weight: .bold, design: .rounded))
                        .foregroundStyle(.white)
                        .contentTransition(.numericText())
                    Image(systemName: status.emoji)
                        .font(.title2)
                        .foregroundStyle(Color(hex: status.colorHex))
                }
            }
            .frame(width: 150, height: 150)

            Text(status.label)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color(hex: status.colorHex))
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(Color(hex: status.colorHex).opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .padding(.horizontal)
        .onAppear {
            withAnimation(.easeInOut(duration: 1.0)) {
                animatedScore = score
            }
        }
        .onChange(of: score) { _, newValue in
            withAnimation(.easeInOut(duration: 0.5)) {
                animatedScore = newValue
            }
        }
    }
}

// MARK: - Quick Stat Box

private struct QuickStatBox: View {
    let icon: String
    let value: String
    let label: String
    let colorHex: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(Color(hex: colorHex))
            Text(value)
                .font(.title2.weight(.bold))
                .foregroundStyle(.white)
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Recommendation Row

private struct RecommendationRow: View {
    let recommendation: WellnessRecommendation

    private var bgColor: Color {
        switch recommendation.priority {
        case .high: return Color(hex: "F44336").opacity(0.1)
        case .medium: return Color(hex: "4CAF50").opacity(0.1)
        case .low: return Color(hex: "1E1E1E")
        }
    }

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: recommendation.icon)
                .font(.title3)
                .foregroundStyle(Color(hex: "FF9800"))
                .frame(width: 36)

            VStack(alignment: .leading, spacing: 2) {
                Text(recommendation.title)
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(.white)
                Text(recommendation.description)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(bgColor)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
    }
}

#Preview {
    WellnessDashboardScreen()
        .preferredColorScheme(.dark)
}
