import SwiftUI
import Charts

struct FormResultsScreen: View {
    let result: FormAnalysisDisplay
    let history: [FormHistoryEntry]

    @State private var selectedBodyPart: String?

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                // Header with score
                scoreHeader

                // Exercise info
                exerciseInfoCard

                // Body part issue map
                bodyPartMap

                // Detailed issues
                detailedIssues

                // Tips section
                tipsSection

                // Score history chart
                if history.filter({ $0.exerciseName == result.exerciseName }).count > 1 {
                    scoreHistoryChart
                }

                // All past analyses for this exercise
                pastAnalyses
            }
            .padding(16)
        }
        .navigationTitle("Form Results")
    }

    // MARK: - Score Header

    private var scoreHeader: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .stroke(Color.gray.opacity(0.2), lineWidth: 12)
                    .frame(width: 120, height: 120)

                Circle()
                    .trim(from: 0, to: CGFloat(result.overallScore) / 100)
                    .stroke(
                        scoreColor,
                        style: StrokeStyle(lineWidth: 12, lineCap: .round)
                    )
                    .frame(width: 120, height: 120)
                    .rotationEffect(.degrees(-90))

                VStack(spacing: 0) {
                    Text("\(result.overallScore)")
                        .font(.system(size: 36, weight: .bold))
                        .foregroundStyle(scoreColor)
                    Text("Form Score")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }

            Text(result.summary)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 20)
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(
                colors: [scoreColor.opacity(0.1), .clear],
                startPoint: .top, endPoint: .bottom
            ),
            in: RoundedRectangle(cornerRadius: 16)
        )
    }

    // MARK: - Exercise Info

    private var exerciseInfoCard: some View {
        HStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Exercise").font(.caption).foregroundStyle(.secondary)
                Text(result.exerciseName).font(.headline)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text("Issues").font(.caption).foregroundStyle(.secondary)
                HStack(spacing: 4) {
                    Text("\(result.issues.count)")
                        .font(.headline)
                    issueCountBadge
                }
            }
            VStack(alignment: .trailing, spacing: 4) {
                Text("Date").font(.caption).foregroundStyle(.secondary)
                Text(result.timestamp.formatted(date: .abbreviated, time: .shortened))
                    .font(.caption)
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    private var issueCountBadge: some View {
        let highCount = result.issues.filter { $0.severity == .high }.count
        let medCount = result.issues.filter { $0.severity == .medium }.count
        let lowCount = result.issues.filter { $0.severity == .low }.count

        return HStack(spacing: 4) {
            if highCount > 0 {
                Circle().fill(Color(hex: "F44336")).frame(width: 8, height: 8)
                Text("\(highCount)").font(.caption2)
            }
            if medCount > 0 {
                Circle().fill(Color(hex: "FF9800")).frame(width: 8, height: 8)
                Text("\(medCount)").font(.caption2)
            }
            if lowCount > 0 {
                Circle().fill(Color(hex: "FFEB3B")).frame(width: 8, height: 8)
                Text("\(lowCount)").font(.caption2)
            }
        }
    }

    // MARK: - Body Part Map

    private var bodyPartMap: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Affected Body Parts").font(.headline)

            HStack(spacing: 16) {
                // Body silhouette with highlighted parts
                ZStack {
                    Image(systemName: "figure.stand")
                        .font(.system(size: 100))
                        .foregroundStyle(.gray.opacity(0.2))

                    // Overlay markers for affected parts
                    ForEach(result.issues) { issue in
                        bodyPartMarker(for: issue)
                    }
                }
                .frame(width: 100, height: 180)

                // Legend
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(result.issues) { issue in
                        Button {
                            selectedBodyPart = issue.bodyPart
                        } label: {
                            HStack(spacing: 8) {
                                Circle()
                                    .fill(severityColor(issue.severity))
                                    .frame(width: 10, height: 10)
                                Text(issue.bodyPart)
                                    .font(.subheadline)
                                    .foregroundStyle(
                                        selectedBodyPart == issue.bodyPart ? .primary : .secondary
                                    )
                            }
                        }
                    }
                }
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    private func bodyPartMarker(for issue: FormIssueDisplay) -> some View {
        let offset = bodyPartOffset(issue.bodyPart)
        return Circle()
            .fill(severityColor(issue.severity).opacity(0.6))
            .frame(width: 16, height: 16)
            .overlay(
                Circle().stroke(severityColor(issue.severity), lineWidth: 2)
            )
            .offset(x: offset.x, y: offset.y)
            .scaleEffect(selectedBodyPart == issue.bodyPart ? 1.4 : 1.0)
            .animation(.spring, value: selectedBodyPart)
    }

    private func bodyPartOffset(_ part: String) -> CGPoint {
        switch part.lowercased() {
        case "head", "head position": return CGPoint(x: 0, y: -70)
        case "shoulders", "shoulder position": return CGPoint(x: 0, y: -45)
        case "back", "lower back": return CGPoint(x: 8, y: -20)
        case "hips", "hip position": return CGPoint(x: 0, y: 0)
        case "knees", "knee", "knee tracking": return CGPoint(x: 0, y: 30)
        case "elbows", "elbow position": return CGPoint(x: -20, y: -30)
        case "wrists", "wrist position": return CGPoint(x: -25, y: -15)
        case "feet", "foot position", "ankles": return CGPoint(x: 0, y: 60)
        case "cadence": return CGPoint(x: 0, y: 50)
        case "balance": return CGPoint(x: 0, y: 10)
        case "depth": return CGPoint(x: 0, y: 20)
        case "bar path": return CGPoint(x: -18, y: -25)
        default: return CGPoint(x: 0, y: 0)
        }
    }

    // MARK: - Detailed Issues

    private var detailedIssues: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Detailed Feedback").font(.headline)

            ForEach(result.issues) { issue in
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Circle()
                            .fill(severityColor(issue.severity))
                            .frame(width: 10, height: 10)
                        Text(issue.bodyPart)
                            .font(.subheadline.bold())
                        Spacer()
                        Text(issue.severity.displayName)
                            .font(.caption2.bold())
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(severityColor(issue.severity).opacity(0.15), in: Capsule())
                            .foregroundStyle(severityColor(issue.severity))
                    }

                    Text(issue.description)
                        .font(.caption)
                        .foregroundStyle(.secondary)

                    HStack(spacing: 6) {
                        Image(systemName: "arrow.right.circle.fill")
                            .font(.caption)
                            .foregroundStyle(AppTheme.primary)
                        Text(issue.suggestion)
                            .font(.caption)
                            .foregroundStyle(AppTheme.primary)
                    }
                    .padding(10)
                    .background(AppTheme.primaryContainer.opacity(0.3), in: RoundedRectangle(cornerRadius: 8))
                }
                .padding(12)
                .background(
                    selectedBodyPart == issue.bodyPart
                        ? severityColor(issue.severity).opacity(0.08)
                        : Color.clear,
                    in: RoundedRectangle(cornerRadius: 12)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(
                            selectedBodyPart == issue.bodyPart
                                ? severityColor(issue.severity).opacity(0.3)
                                : .clear,
                            lineWidth: 1
                        )
                )
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Tips

    private var tipsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Improvement Tips", systemImage: "lightbulb.fill")
                .font(.headline)

            ForEach(Array(result.tips.enumerated()), id: \.offset) { index, tip in
                HStack(alignment: .top, spacing: 12) {
                    Text("\(index + 1)")
                        .font(.caption.bold())
                        .foregroundStyle(.white)
                        .frame(width: 24, height: 24)
                        .background(AppTheme.primary, in: Circle())
                    Text(tip)
                        .font(.subheadline)
                }
                .padding(.vertical, 2)
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Score History Chart

    private var scoreHistoryChart: some View {
        let exerciseHistory = history
            .filter { $0.exerciseName == result.exerciseName }
            .sorted { $0.date < $1.date }

        return VStack(alignment: .leading, spacing: 12) {
            Text("Score History - \(result.exerciseName)").font(.headline)

            Chart(exerciseHistory) { entry in
                LineMark(
                    x: .value("Date", entry.date),
                    y: .value("Score", entry.score)
                )
                .foregroundStyle(AppTheme.primary)
                .interpolationMethod(.catmullRom)
                .symbol(Circle())

                PointMark(
                    x: .value("Date", entry.date),
                    y: .value("Score", entry.score)
                )
                .foregroundStyle(AppTheme.primary)
            }
            .frame(height: 160)
            .chartYScale(domain: 0...100)
            .chartYAxisLabel("Score")
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Past Analyses

    private var pastAnalyses: some View {
        let exerciseHistory = history
            .filter { $0.exerciseName == result.exerciseName }
            .prefix(5)

        return Group {
            if exerciseHistory.count > 1 {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Past Analyses").font(.headline)

                    ForEach(Array(exerciseHistory)) { entry in
                        HStack(spacing: 12) {
                            ZStack {
                                Circle()
                                    .fill(entryScoreColor(entry.score).opacity(0.2))
                                    .frame(width: 40, height: 40)
                                Text("\(entry.score)")
                                    .font(.caption.bold())
                                    .foregroundStyle(entryScoreColor(entry.score))
                            }

                            VStack(alignment: .leading, spacing: 2) {
                                Text(entry.date.formatted(date: .abbreviated, time: .omitted))
                                    .font(.subheadline)
                                Text("\(entry.issueCount) issue\(entry.issueCount == 1 ? "" : "s")")
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }

                            Spacer()

                            if entry.date == result.timestamp {
                                Text("Current")
                                    .font(.caption2.bold())
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(AppTheme.primary.opacity(0.15), in: Capsule())
                                    .foregroundStyle(AppTheme.primary)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }
                .padding(16)
                .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
            }
        }
    }

    // MARK: - Helpers

    private var scoreColor: Color {
        entryScoreColor(result.overallScore)
    }

    private func entryScoreColor(_ score: Int) -> Color {
        switch score {
        case 80...100: return Color(hex: "4CAF50")
        case 60..<80: return Color(hex: "FF9800")
        default: return Color(hex: "F44336")
        }
    }

    private func severityColor(_ severity: FormIssueDisplaySeverity) -> Color {
        switch severity {
        case .low: return Color(hex: "FFEB3B")
        case .medium: return Color(hex: "FF9800")
        case .high: return Color(hex: "F44336")
        }
    }
}

#Preview {
    NavigationStack {
        FormResultsScreen(
            result: FormAnalysisDisplay(
                exerciseName: "Squat",
                overallScore: 72,
                issues: [
                    FormIssueDisplay(bodyPart: "Knees", description: "Knees caving inward during descent", severity: .high, suggestion: "Focus on pushing knees out over toes"),
                    FormIssueDisplay(bodyPart: "Back", description: "Slight rounding of lower back", severity: .medium, suggestion: "Maintain neutral spine"),
                    FormIssueDisplay(bodyPart: "Depth", description: "Not reaching parallel", severity: .low, suggestion: "Work on hip mobility"),
                ],
                tips: ["Warm up with bodyweight squats", "Film from a 45-degree angle", "Use a 3-1 tempo"],
                timestamp: .now,
                summary: "Good form with a few areas to address"
            ),
            history: [
                FormHistoryEntry(exerciseName: "Squat", score: 65, issueCount: 3, date: Calendar.current.date(byAdding: .day, value: -14, to: .now)!),
                FormHistoryEntry(exerciseName: "Squat", score: 72, issueCount: 2, date: .now),
            ]
        )
    }
}
