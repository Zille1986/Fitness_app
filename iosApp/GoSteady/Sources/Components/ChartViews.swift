import SwiftUI
import Charts

// MARK: - Line Chart

struct GoSteadyLineChart: View {
    let data: [ChartDataPoint]
    var lineColor: Color = AppTheme.primary
    var showArea: Bool = true
    var showDots: Bool = true
    var yAxisLabel: String? = nil
    var height: CGFloat = 200
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Chart(data) { point in
            LineMark(
                x: .value("Date", point.date),
                y: .value("Value", point.value)
            )
            .foregroundStyle(lineColor)
            .lineStyle(StrokeStyle(lineWidth: 2.5, lineCap: .round, lineJoin: .round))
            .interpolationMethod(.catmullRom)

            if showArea {
                AreaMark(
                    x: .value("Date", point.date),
                    y: .value("Value", point.value)
                )
                .foregroundStyle(
                    LinearGradient(
                        colors: [lineColor.opacity(0.3), lineColor.opacity(0.0)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .interpolationMethod(.catmullRom)
            }

            if showDots {
                PointMark(
                    x: .value("Date", point.date),
                    y: .value("Value", point.value)
                )
                .foregroundStyle(lineColor)
                .symbolSize(30)
            }
        }
        .chartYAxisLabel(yAxisLabel ?? "")
        .chartXAxis {
            AxisMarks(values: .stride(by: .day)) { _ in
                AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5, dash: [4]))
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme).opacity(0.3))
                AxisValueLabel(format: .dateTime.weekday(.abbreviated))
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
            }
        }
        .chartYAxis {
            AxisMarks { _ in
                AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5, dash: [4]))
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme).opacity(0.3))
                AxisValueLabel()
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
            }
        }
        .frame(height: height)
    }
}

// MARK: - Bar Chart

struct GoSteadyBarChart: View {
    let data: [ChartDataPoint]
    var barColor: Color = AppTheme.primary
    var showValues: Bool = false
    var height: CGFloat = 200
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Chart(data) { point in
            BarMark(
                x: .value("Date", point.label ?? ""),
                y: .value("Value", point.value)
            )
            .foregroundStyle(
                LinearGradient(
                    colors: [barColor, barColor.opacity(0.7)],
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .cornerRadius(4)

            if showValues {
                BarMark(
                    x: .value("Date", point.label ?? ""),
                    y: .value("Value", point.value)
                )
                .foregroundStyle(.clear)
                .annotation(position: .top, spacing: 4) {
                    Text(String(format: "%.0f", point.value))
                        .font(AppTypography.captionSmall)
                        .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                }
            }
        }
        .chartXAxis {
            AxisMarks { _ in
                AxisValueLabel()
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
            }
        }
        .chartYAxis {
            AxisMarks { _ in
                AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5, dash: [4]))
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme).opacity(0.3))
                AxisValueLabel()
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
            }
        }
        .frame(height: height)
    }
}

// MARK: - Pie / Donut Chart

struct GoSteadyDonutChart: View {
    let segments: [DonutSegment]
    var innerRadius: CGFloat = 0.6
    var size: CGFloat = 150
    var centerText: String? = nil
    var centerSubtext: String? = nil
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        ZStack {
            Chart(segments) { segment in
                SectorMark(
                    angle: .value("Value", segment.value),
                    innerRadius: .ratio(innerRadius),
                    outerRadius: .ratio(1.0)
                )
                .foregroundStyle(segment.color)
                .cornerRadius(3)
            }
            .frame(width: size, height: size)

            if let centerText {
                VStack(spacing: 2) {
                    Text(centerText)
                        .font(AppTypography.titleMedium)
                        .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
                    if let centerSubtext {
                        Text(centerSubtext)
                            .font(AppTypography.captionSmall)
                            .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                    }
                }
            }
        }
    }
}

// MARK: - Data Models

struct ChartDataPoint: Identifiable {
    let id = UUID()
    let date: Date
    let value: Double
    var label: String? = nil

    init(date: Date, value: Double, label: String? = nil) {
        self.date = date
        self.value = value
        self.label = label
    }

    init(label: String, value: Double) {
        self.date = Date()
        self.value = value
        self.label = label
    }
}

struct DonutSegment: Identifiable {
    let id = UUID()
    let label: String
    let value: Double
    let color: Color
}

// MARK: - Weekly Activity Bar Chart

struct WeeklyActivityBarChart: View {
    let dailyValues: [Double]
    let dayLabels: [String]
    var barColor: Color = AppTheme.primary
    var goalLine: Double? = nil
    var height: CGFloat = 120
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let data = zip(dayLabels, dailyValues).map { ChartDataPoint(label: $0.0, value: $0.1) }

        Chart {
            ForEach(data) { point in
                BarMark(
                    x: .value("Day", point.label ?? ""),
                    y: .value("Value", point.value)
                )
                .foregroundStyle(point.value > 0 ? barColor : barColor.opacity(0.2))
                .cornerRadius(4)
            }

            if let goal = goalLine {
                RuleMark(y: .value("Goal", goal))
                    .foregroundStyle(AppTheme.secondary.opacity(0.6))
                    .lineStyle(StrokeStyle(lineWidth: 1, dash: [5, 3]))
                    .annotation(position: .top, alignment: .trailing) {
                        Text("Goal")
                            .font(AppTypography.captionSmall)
                            .foregroundStyle(AppTheme.secondary)
                    }
            }
        }
        .chartXAxis {
            AxisMarks { _ in
                AxisValueLabel()
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
            }
        }
        .chartYAxis(.hidden)
        .frame(height: height)
    }
}
