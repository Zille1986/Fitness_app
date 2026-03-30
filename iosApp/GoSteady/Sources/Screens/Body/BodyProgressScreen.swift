import SwiftUI
import Charts

struct BodyProgressScreen: View {
    @State private var viewModel = BodyAnalysisViewModel()
    @State private var selectedMetric: ProgressMetric = .weight
    @State private var showBeforeAfter = false

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                // Summary card
                summaryCard

                // Metric selector
                metricSelector

                // Chart for selected metric
                metricChart

                // Measurement table
                measurementTable

                // Before/After comparison
                if viewModel.scanHistory.count >= 2 {
                    beforeAfterSection
                }

                // Body scan score history
                if viewModel.scoreHistory.count > 1 {
                    scanScoreChart
                }
            }
            .padding(16)
        }
        .navigationTitle("Body Progress")
    }

    // MARK: - Summary

    private var summaryCard: some View {
        VStack(spacing: 16) {
            Text("Progress Overview")
                .font(.headline)

            HStack(spacing: 20) {
                if let latest = viewModel.measurementHistory.last,
                   let first = viewModel.measurementHistory.first {
                    ProgressStat(
                        title: "Weight",
                        current: formatOpt(latest.weight, suffix: " kg"),
                        change: changeString(from: first.weight, to: latest.weight, suffix: " kg"),
                        isPositive: (latest.weight ?? 0) <= (first.weight ?? 0)
                    )
                    ProgressStat(
                        title: "Body Fat",
                        current: formatOpt(latest.bodyFatPercent.map(Double.init), suffix: "%"),
                        change: changeString(from: first.bodyFatPercent.map(Double.init),
                                             to: latest.bodyFatPercent.map(Double.init), suffix: "%"),
                        isPositive: (latest.bodyFatPercent ?? 0) <= (first.bodyFatPercent ?? 0)
                    )
                    ProgressStat(
                        title: "Waist",
                        current: formatOpt(latest.waist, suffix: " cm"),
                        change: changeString(from: first.waist, to: latest.waist, suffix: " cm"),
                        isPositive: (latest.waist ?? 0) <= (first.waist ?? 0)
                    )
                }
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Metric Selector

    private var metricSelector: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(ProgressMetric.allCases) { metric in
                    Button {
                        selectedMetric = metric
                    } label: {
                        Text(metric.displayName)
                            .font(.caption.bold())
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(
                                selectedMetric == metric ? AppTheme.primary : AppTheme.surfaceContainer,
                                in: Capsule()
                            )
                            .foregroundStyle(selectedMetric == metric ? .white : .primary)
                    }
                }
            }
        }
    }

    // MARK: - Metric Chart

    private var metricChart: some View {
        let dataPoints = viewModel.measurementHistory.compactMap { point -> (Date, Double)? in
            guard let value = selectedMetric.value(from: point) else { return nil }
            return (point.date, value)
        }

        return VStack(alignment: .leading, spacing: 12) {
            Text("\(selectedMetric.displayName) Trend").font(.headline)

            if dataPoints.count > 1 {
                Chart {
                    ForEach(Array(dataPoints.enumerated()), id: \.offset) { _, dp in
                        LineMark(
                            x: .value("Date", dp.0),
                            y: .value(selectedMetric.displayName, dp.1)
                        )
                        .foregroundStyle(AppTheme.primary)
                        .interpolationMethod(.catmullRom)

                        AreaMark(
                            x: .value("Date", dp.0),
                            y: .value(selectedMetric.displayName, dp.1)
                        )
                        .foregroundStyle(
                            LinearGradient(colors: [AppTheme.primary.opacity(0.3), .clear],
                                           startPoint: .top, endPoint: .bottom)
                        )
                        .interpolationMethod(.catmullRom)

                        PointMark(
                            x: .value("Date", dp.0),
                            y: .value(selectedMetric.displayName, dp.1)
                        )
                        .foregroundStyle(AppTheme.primary)
                    }
                }
                .frame(height: 200)
                .chartYAxisLabel(selectedMetric.unit)
            } else {
                Text("Not enough data points")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(height: 100)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Measurement Table

    private var measurementTable: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Measurements").font(.headline)

            // Header
            HStack {
                Text("Date").font(.caption2.bold()).frame(width: 60, alignment: .leading)
                Text("Weight").font(.caption2.bold()).frame(maxWidth: .infinity)
                Text("BF%").font(.caption2.bold()).frame(maxWidth: .infinity)
                Text("Waist").font(.caption2.bold()).frame(maxWidth: .infinity)
                Text("Chest").font(.caption2.bold()).frame(maxWidth: .infinity)
                Text("Bicep").font(.caption2.bold()).frame(maxWidth: .infinity)
            }
            .foregroundStyle(.secondary)

            ForEach(Array(viewModel.measurementHistory.suffix(8))) { point in
                HStack {
                    Text(point.date.formatted(.dateTime.month(.abbreviated).day()))
                        .font(.caption2)
                        .frame(width: 60, alignment: .leading)
                    Text(formatOpt(point.weight, suffix: ""))
                        .font(.caption2)
                        .frame(maxWidth: .infinity)
                    Text(point.bodyFatPercent.map { String(format: "%.1f", $0) } ?? "--")
                        .font(.caption2)
                        .frame(maxWidth: .infinity)
                    Text(formatOpt(point.waist, suffix: ""))
                        .font(.caption2)
                        .frame(maxWidth: .infinity)
                    Text(formatOpt(point.chest, suffix: ""))
                        .font(.caption2)
                        .frame(maxWidth: .infinity)
                    Text(formatOpt(point.bicep, suffix: ""))
                        .font(.caption2)
                        .frame(maxWidth: .infinity)
                }
                Divider()
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Before / After

    private var beforeAfterSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Before & After").font(.headline)

            HStack(spacing: 16) {
                // Before
                VStack(spacing: 8) {
                    Text("Before").font(.caption.bold()).foregroundStyle(.secondary)

                    Picker("Before", selection: $viewModel.selectedBeforeIndex) {
                        ForEach(Array(viewModel.scanHistory.enumerated()), id: \.offset) { index, scan in
                            Text(scan.timestamp.formatted(date: .abbreviated, time: .omitted))
                                .tag(index)
                        }
                    }
                    .pickerStyle(.menu)

                    let beforeScan = viewModel.scanHistory[safe: viewModel.selectedBeforeIndex]
                    if let scan = beforeScan {
                        scanComparisonCard(scan: scan, label: "Before")
                    }
                }
                .frame(maxWidth: .infinity)

                // VS
                Text("vs")
                    .font(.caption.bold())
                    .foregroundStyle(.secondary)

                // After
                VStack(spacing: 8) {
                    Text("After").font(.caption.bold()).foregroundStyle(.secondary)

                    Picker("After", selection: $viewModel.selectedAfterIndex) {
                        ForEach(Array(viewModel.scanHistory.enumerated()), id: \.offset) { index, scan in
                            Text(scan.timestamp.formatted(date: .abbreviated, time: .omitted))
                                .tag(index)
                        }
                    }
                    .pickerStyle(.menu)

                    let afterScan = viewModel.scanHistory[safe: viewModel.selectedAfterIndex]
                    if let scan = afterScan {
                        scanComparisonCard(scan: scan, label: "After")
                    }
                }
                .frame(maxWidth: .infinity)
            }

            // Comparison details
            if let before = viewModel.scanHistory[safe: viewModel.selectedBeforeIndex],
               let after = viewModel.scanHistory[safe: viewModel.selectedAfterIndex] {
                comparisonDetails(before: before, after: after)
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    private func scanComparisonCard(scan: BodyScanResult, label: String) -> some View {
        VStack(spacing: 6) {
            ZStack {
                Circle()
                    .fill(scanScoreColor(scan.overallScore).opacity(0.2))
                    .frame(width: 50, height: 50)
                Text("\(scan.overallScore)")
                    .font(.headline.bold())
                    .foregroundStyle(scanScoreColor(scan.overallScore))
            }
            if let bf = scan.estimatedBodyFatPercentage {
                Text(String(format: "%.1f%% BF", bf))
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Text(scan.bodyType.displayName)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }

    private func comparisonDetails(before: BodyScanResult, after: BodyScanResult) -> some View {
        let scoreDiff = after.overallScore - before.overallScore
        let bfDiff: Float? = {
            guard let a = after.estimatedBodyFatPercentage, let b = before.estimatedBodyFatPercentage else { return nil }
            return a - b
        }()

        return VStack(spacing: 8) {
            Divider()
            HStack {
                Text("Score Change")
                    .font(.subheadline)
                Spacer()
                HStack(spacing: 4) {
                    Image(systemName: scoreDiff >= 0 ? "arrow.up" : "arrow.down")
                        .font(.caption2)
                    Text("\(abs(scoreDiff)) pts")
                        .font(.subheadline.bold())
                }
                .foregroundStyle(scoreDiff >= 0 ? Color(hex: "4CAF50") : Color(hex: "F44336"))
            }

            if let bfDiff {
                HStack {
                    Text("Body Fat Change")
                        .font(.subheadline)
                    Spacer()
                    HStack(spacing: 4) {
                        Image(systemName: bfDiff <= 0 ? "arrow.down" : "arrow.up")
                            .font(.caption2)
                        Text(String(format: "%.1f%%", abs(bfDiff)))
                            .font(.subheadline.bold())
                    }
                    .foregroundStyle(bfDiff <= 0 ? Color(hex: "4CAF50") : Color(hex: "F44336"))
                }
            }
        }
    }

    // MARK: - Score History

    private var scanScoreChart: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Scan Score History").font(.headline)

            Chart {
                ForEach(Array(viewModel.scoreHistory.enumerated()), id: \.offset) { _, point in
                    LineMark(
                        x: .value("Date", point.date),
                        y: .value("Score", point.score)
                    )
                    .foregroundStyle(AppTheme.primary)
                    .interpolationMethod(.catmullRom)

                    PointMark(
                        x: .value("Date", point.date),
                        y: .value("Score", point.score)
                    )
                    .foregroundStyle(AppTheme.primary)
                }
            }
            .frame(height: 160)
            .chartYScale(domain: 0...100)
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Helpers

    private func formatOpt(_ value: Double?, suffix: String) -> String {
        guard let v = value else { return "--" }
        return String(format: "%.1f%@", v, suffix)
    }

    private func changeString(from: Double?, to: Double?, suffix: String) -> String {
        guard let f = from, let t = to else { return "--" }
        let diff = t - f
        let sign = diff >= 0 ? "+" : ""
        return String(format: "%@%.1f%@", sign, diff, suffix)
    }

    private func scanScoreColor(_ score: Int) -> Color {
        switch score {
        case 80...100: return Color(hex: "4CAF50")
        case 60..<80: return Color(hex: "FF9800")
        default: return Color(hex: "F44336")
        }
    }
}

// MARK: - Progress Metric

enum ProgressMetric: String, CaseIterable, Identifiable {
    case weight, bodyFat, chest, waist, hips, bicep, thigh

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .weight: return "Weight"
        case .bodyFat: return "Body Fat"
        case .chest: return "Chest"
        case .waist: return "Waist"
        case .hips: return "Hips"
        case .bicep: return "Bicep"
        case .thigh: return "Thigh"
        }
    }

    var unit: String {
        switch self {
        case .weight: return "kg"
        case .bodyFat: return "%"
        default: return "cm"
        }
    }

    func value(from point: BodyMeasurementPoint) -> Double? {
        switch self {
        case .weight: return point.weight
        case .bodyFat: return point.bodyFatPercent.map(Double.init)
        case .chest: return point.chest
        case .waist: return point.waist
        case .hips: return point.hips
        case .bicep: return point.bicep
        case .thigh: return point.thigh
        }
    }
}

// MARK: - Progress Stat

private struct ProgressStat: View {
    let title: String
    let current: String
    let change: String
    let isPositive: Bool

    var body: some View {
        VStack(spacing: 4) {
            Text(title)
                .font(.caption2)
                .foregroundStyle(.secondary)
            Text(current)
                .font(.subheadline.bold())
            Text(change)
                .font(.caption2.bold())
                .foregroundStyle(isPositive ? Color(hex: "4CAF50") : Color(hex: "F44336"))
        }
    }
}

// MARK: - Safe Array Access

extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

#Preview {
    NavigationStack {
        BodyProgressScreen()
    }
}
