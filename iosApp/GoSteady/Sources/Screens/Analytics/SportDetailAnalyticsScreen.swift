import SwiftUI
import Charts

struct SportDetailAnalyticsScreen: View {
    let sport: SportType
    @State private var viewModel = AnalyticsViewModel()

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                // Date range
                dateRangePicker

                // Summary cards
                summaryCards

                // Sport-specific charts
                switch sport {
                case .run:
                    runningCharts
                case .gym:
                    gymCharts
                case .swim:
                    swimmingCharts
                case .bike:
                    cyclingCharts
                case .hiit:
                    gymCharts
                }

                // Frequency chart
                frequencyChart

                // Personal bests for this sport
                sportPersonalBests
            }
            .padding(16)
        }
        .navigationTitle("\(sport.displayName) Analytics")
        .onAppear {
            viewModel.selectSport(sport)
        }
    }

    // MARK: - Date Range

    private var dateRangePicker: some View {
        HStack(spacing: 6) {
            ForEach(DateRangeOption.allCases) { option in
                Button {
                    viewModel.selectDateRange(option)
                    Task { await viewModel.loadSportDetail(sport) }
                } label: {
                    Text(option.displayName)
                        .font(.caption.bold())
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(
                            viewModel.selectedDateRange == option
                                ? sportColor : AppTheme.surfaceContainer,
                            in: Capsule()
                        )
                        .foregroundStyle(viewModel.selectedDateRange == option ? .white : .primary)
                }
            }
        }
    }

    // MARK: - Summary

    private var summaryCards: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
            SportStatCard(title: "Sessions", value: "\(viewModel.sportDetailStats.totalSessions)", color: sportColor)
            SportStatCard(title: "Avg Duration", value: "\(viewModel.sportDetailStats.avgDuration) min", color: sportColor)
            if sport != .gym {
                SportStatCard(title: "Total Distance", value: String(format: "%.1f km", viewModel.sportDetailStats.totalDistance), color: sportColor)
                SportStatCard(title: "Avg Distance", value: String(format: "%.1f km", viewModel.sportDetailStats.avgDistance), color: sportColor)
            }
            SportStatCard(title: "Total Time", value: formatMinutes(viewModel.sportDetailStats.totalTime), color: sportColor)
        }
    }

    // MARK: - Running Charts

    private var runningCharts: some View {
        VStack(spacing: 16) {
            chartCard(title: "Pace Trend (min/km)", data: viewModel.sportDetailStats.paceOrSpeedTrend, yLabel: "min/km", color: sportColor)
            chartCard(title: "Distance Per Run", data: viewModel.sportDetailStats.distanceTrend, yLabel: "km", color: Color(hex: "007AFF"))
        }
    }

    // MARK: - Gym Charts

    private var gymCharts: some View {
        VStack(spacing: 16) {
            chartCard(title: "Volume Trend (kg x reps)", data: viewModel.sportDetailStats.volumeTrend, yLabel: "volume", color: sportColor)
            chartCard(title: "Session Duration", data: viewModel.sportDetailStats.paceOrSpeedTrend.map {
                TrendDataPoint(date: $0.date, value: $0.value * 10)
            }, yLabel: "min", color: Color(hex: "FF2D55"))
        }
    }

    // MARK: - Swimming Charts

    private var swimmingCharts: some View {
        VStack(spacing: 16) {
            chartCard(title: "Pace Trend (min/100m)", data: viewModel.sportDetailStats.paceOrSpeedTrend, yLabel: "min/100m", color: sportColor)
            chartCard(title: "Distance Per Session", data: viewModel.sportDetailStats.distanceTrend.map {
                TrendDataPoint(date: $0.date, value: $0.value * 100)
            }, yLabel: "m", color: Color(hex: "007AFF"))
        }
    }

    // MARK: - Cycling Charts

    private var cyclingCharts: some View {
        VStack(spacing: 16) {
            chartCard(title: "Speed Trend (km/h)", data: viewModel.sportDetailStats.paceOrSpeedTrend.map {
                TrendDataPoint(date: $0.date, value: $0.value * 5)
            }, yLabel: "km/h", color: sportColor)
            chartCard(title: "Distance Per Ride", data: viewModel.sportDetailStats.distanceTrend, yLabel: "km", color: Color(hex: "007AFF"))
        }
    }

    // MARK: - Frequency

    private var frequencyChart: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Workout Frequency").font(.headline)

            Chart(viewModel.sportDetailStats.frequencyByWeek) { item in
                BarMark(
                    x: .value("Day", item.day),
                    y: .value("Workouts", item.workouts)
                )
                .foregroundStyle(sportColor.gradient)
                .cornerRadius(4)
            }
            .frame(height: 160)
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Personal Bests

    private var sportPersonalBests: some View {
        let bests = viewModel.personalBests.filter { $0.sport == sport }
        return Group {
            if !bests.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Personal Bests").font(.headline)

                    ForEach(bests) { pb in
                        HStack {
                            Image(systemName: "trophy.fill")
                                .foregroundStyle(.yellow)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(pb.metric)
                                    .font(.subheadline)
                                Text(pb.date.formatted(date: .abbreviated, time: .omitted))
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Text(pb.value)
                                .font(.subheadline.bold())
                                .foregroundStyle(sportColor)
                        }
                        .padding(.vertical, 4)
                    }
                }
                .padding(16)
                .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
            }
        }
    }

    // MARK: - Generic Chart Card

    private func chartCard(title: String, data: [TrendDataPoint], yLabel: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).font(.headline)

            Chart(data) { point in
                LineMark(
                    x: .value("Date", point.date),
                    y: .value(yLabel, point.value)
                )
                .foregroundStyle(color)
                .interpolationMethod(.catmullRom)

                AreaMark(
                    x: .value("Date", point.date),
                    y: .value(yLabel, point.value)
                )
                .foregroundStyle(
                    LinearGradient(colors: [color.opacity(0.3), .clear],
                                   startPoint: .top, endPoint: .bottom)
                )
                .interpolationMethod(.catmullRom)
            }
            .frame(height: 160)
            .chartYAxisLabel(yLabel)
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Helpers

    private var sportColor: Color {
        switch sport {
        case .run: return AppTheme.running
        case .gym: return AppTheme.gym
        case .swim: return AppTheme.swimming
        case .bike: return AppTheme.cycling
        case .hiit: return AppTheme.hiit
        }
    }

    private func formatMinutes(_ minutes: Int) -> String {
        let h = minutes / 60
        let m = minutes % 60
        return h > 0 ? "\(h)h \(m)m" : "\(m)m"
    }
}

// MARK: - Stat Card

private struct SportStatCard: View {
    let title: String
    let value: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(value).font(.headline.bold())
            Text(title).font(.caption).foregroundStyle(.secondary)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(color.opacity(0.1), in: RoundedRectangle(cornerRadius: 12))
    }
}

#Preview {
    NavigationStack {
        SportDetailAnalyticsScreen(sport: .run)
    }
}
