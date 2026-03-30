import SwiftUI
import Charts

struct AnalyticsDashboardScreen: View {
    @State private var viewModel = AnalyticsViewModel()

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                // Date range picker
                dateRangePicker

                // Overview cards
                overviewCards

                // Sport breakdown pie chart
                sportBreakdownChart

                // Weekly activity bar chart
                weeklyBarChart

                // Distance trend line
                distanceTrendChart

                // Duration trend line
                durationTrendChart

                // Personal bests
                personalBestsSection
            }
            .padding(16)
        }
        .navigationTitle("Analytics")
        .refreshable { await viewModel.loadData() }
    }

    // MARK: - Date Range Picker

    private var dateRangePicker: some View {
        HStack(spacing: 6) {
            ForEach(DateRangeOption.allCases) { option in
                Button {
                    viewModel.selectDateRange(option)
                } label: {
                    Text(option.displayName)
                        .font(.caption.bold())
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(
                            viewModel.selectedDateRange == option
                                ? AppTheme.primary : AppTheme.surfaceContainer,
                            in: Capsule()
                        )
                        .foregroundStyle(viewModel.selectedDateRange == option ? .white : .primary)
                }
            }
        }
    }

    // MARK: - Overview Cards

    private var overviewCards: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
            AnalyticsStatCard(
                title: "Workouts", value: "\(viewModel.overviewStats.totalWorkouts)",
                icon: "figure.run", color: AppTheme.primary
            )
            AnalyticsStatCard(
                title: "Distance", value: String(format: "%.1f km", viewModel.overviewStats.totalDistanceKm),
                icon: "map", color: Color(hex: "007AFF")
            )
            AnalyticsStatCard(
                title: "Time", value: formatMinutes(viewModel.overviewStats.totalTimeMinutes),
                icon: "clock", color: Color(hex: "FF9500")
            )
            AnalyticsStatCard(
                title: "Calories", value: formatCalories(viewModel.overviewStats.totalCalories),
                icon: "flame.fill", color: Color(hex: "FF2D55")
            )
        }
    }

    // MARK: - Sport Breakdown

    private var sportBreakdownChart: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Sport Breakdown").font(.headline)

            HStack(spacing: 20) {
                Chart(viewModel.sportBreakdown) { item in
                    SectorMark(
                        angle: .value("Count", item.count),
                        innerRadius: .ratio(0.6),
                        angularInset: 2
                    )
                    .foregroundStyle(colorForSport(item.sport))
                    .cornerRadius(4)
                }
                .frame(width: 120, height: 120)

                VStack(alignment: .leading, spacing: 8) {
                    ForEach(viewModel.sportBreakdown) { item in
                        HStack(spacing: 8) {
                            Circle()
                                .fill(colorForSport(item.sport))
                                .frame(width: 10, height: 10)
                            Text(item.sport.displayName)
                                .font(.caption)
                            Spacer()
                            Text("\(item.count)")
                                .font(.caption.bold())
                            Text("(\(Int(item.percentage * 100))%)")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Weekly Bar Chart

    private var weeklyBarChart: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Weekly Activity").font(.headline)

            Chart(viewModel.weeklyActivity) { item in
                BarMark(
                    x: .value("Day", item.day),
                    y: .value("Minutes", item.minutes)
                )
                .foregroundStyle(AppTheme.primary.gradient)
                .cornerRadius(4)
            }
            .frame(height: 180)
            .chartYAxisLabel("min")
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Distance Trend

    private var distanceTrendChart: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Distance Trend").font(.headline)

            Chart(viewModel.distanceTrend) { point in
                LineMark(
                    x: .value("Date", point.date),
                    y: .value("km", point.value)
                )
                .foregroundStyle(Color(hex: "007AFF"))
                .interpolationMethod(.catmullRom)

                AreaMark(
                    x: .value("Date", point.date),
                    y: .value("km", point.value)
                )
                .foregroundStyle(
                    LinearGradient(
                        colors: [Color(hex: "007AFF").opacity(0.3), .clear],
                        startPoint: .top, endPoint: .bottom
                    )
                )
                .interpolationMethod(.catmullRom)
            }
            .frame(height: 160)
            .chartYAxisLabel("km")
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Duration Trend

    private var durationTrendChart: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Duration Trend").font(.headline)

            Chart(viewModel.durationTrend) { point in
                LineMark(
                    x: .value("Date", point.date),
                    y: .value("min", point.value)
                )
                .foregroundStyle(Color(hex: "FF9500"))
                .interpolationMethod(.catmullRom)

                AreaMark(
                    x: .value("Date", point.date),
                    y: .value("min", point.value)
                )
                .foregroundStyle(
                    LinearGradient(
                        colors: [Color(hex: "FF9500").opacity(0.3), .clear],
                        startPoint: .top, endPoint: .bottom
                    )
                )
                .interpolationMethod(.catmullRom)
            }
            .frame(height: 160)
            .chartYAxisLabel("min")
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Personal Bests

    private var personalBestsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Personal Bests").font(.headline)

            ForEach(viewModel.personalBests) { pb in
                HStack(spacing: 12) {
                    Image(systemName: pb.sport.icon)
                        .font(.title3)
                        .foregroundStyle(colorForSport(pb.sport))
                        .frame(width: 32)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(pb.metric)
                            .font(.subheadline.weight(.medium))
                        Text(pb.date.formatted(date: .abbreviated, time: .omitted))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }

                    Spacer()

                    Text(pb.value)
                        .font(.subheadline.bold())
                        .foregroundStyle(AppTheme.primary)
                }
                .padding(.vertical, 4)
                if pb.id != viewModel.personalBests.last?.id {
                    Divider()
                }
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Helpers

    private func colorForSport(_ sport: SportType) -> Color {
        switch sport {
        case .run: return AppTheme.running
        case .gym: return AppTheme.gym
        case .swim: return AppTheme.swimming
        case .bike: return AppTheme.cycling
        case .hiit: return AppTheme.primary
        }
    }

    private func formatMinutes(_ minutes: Int) -> String {
        let hours = minutes / 60
        let mins = minutes % 60
        return hours > 0 ? "\(hours)h \(mins)m" : "\(mins)m"
    }

    private func formatCalories(_ cal: Int) -> String {
        cal >= 1000 ? String(format: "%.1fk", Double(cal) / 1000) : "\(cal)"
    }
}

// MARK: - Analytics Stat Card

private struct AnalyticsStatCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: icon)
                    .foregroundStyle(color)
                Spacer()
            }
            Text(value)
                .font(.title3.bold())
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(14)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 12))
    }
}

#Preview {
    NavigationStack {
        AnalyticsDashboardScreen()
    }
}
