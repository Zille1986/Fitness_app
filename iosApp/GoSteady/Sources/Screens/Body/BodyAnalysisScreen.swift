import SwiftUI
import Charts
import UIKit
import AVFoundation

struct BodyAnalysisScreen: View {
    @State private var viewModel = BodyAnalysisViewModel()
    @State private var showCamera = false
    @State private var showPhotoPicker = false

    var body: some View {
        VStack(spacing: 0) {
            switch viewModel.analysisPhase {
            case .goalSelection:
                goalSelectionContent
            case .photoCapture:
                photoCaptureContent
            case .analyzing:
                analyzingContent
            case .results:
                resultsContent
            }
        }
        .navigationTitle("Body Analyzer")
        .toolbar {
            if !viewModel.scanHistory.isEmpty {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        viewModel.toggleHistorySheet()
                    } label: {
                        Image(systemName: "clock.arrow.circlepath")
                    }
                }
            }
        }
        .sheet(isPresented: $viewModel.showHistorySheet) {
            BodyHistorySheet(
                scanHistory: viewModel.scanHistory,
                scoreHistory: viewModel.scoreHistory,
                onSelectScan: { id in
                    viewModel.viewScanDetails(id)
                    viewModel.showHistorySheet = false
                }
            )
            .presentationDetents([.large])
        }
        .fullScreenCover(isPresented: $showCamera) {
            CameraCapture { image in
                viewModel.setCapturedImage(image)
                showCamera = false
            } onCancel: {
                showCamera = false
            }
        }
    }

    // MARK: - Goal Selection

    private var goalSelectionContent: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Header
                VStack(spacing: 8) {
                    Image(systemName: "figure.stand")
                        .font(.system(size: 48))
                        .foregroundStyle(AppTheme.primary)
                    Text("Body Analyzer")
                        .font(.title2.bold())
                    Text("Get AI-powered body composition analysis and personalized recommendations")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)
                }
                .padding(.vertical, 20)
                .frame(maxWidth: .infinity)
                .background(AppTheme.primaryContainer, in: RoundedRectangle(cornerRadius: 16))

                // Goal selection
                Text("Select Your Goal").font(.headline).frame(maxWidth: .infinity, alignment: .leading)

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    ForEach(FitnessGoal.allCases) { goal in
                        GoalCard(
                            goal: goal,
                            isSelected: viewModel.selectedGoal == goal,
                            onTap: { viewModel.setGoal(goal) }
                        )
                    }
                }

                // Recent scans summary
                if !viewModel.scanHistory.isEmpty {
                    recentScansSummary
                }

                // Continue button
                Button {
                    viewModel.proceedToCapture()
                } label: {
                    Text("Continue")
                        .font(.headline)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(AppTheme.primary, in: RoundedRectangle(cornerRadius: 14))
                }
                .padding(.top, 8)
            }
            .padding(16)
        }
    }

    private var recentScansSummary: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Recent Scans").font(.headline)
                Spacer()
                Button("View All") { viewModel.toggleHistorySheet() }
                    .font(.caption.bold())
                    .foregroundStyle(AppTheme.primary)
            }

            ForEach(viewModel.scanHistory.prefix(3)) { scan in
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(scoreColor(scan.overallScore).opacity(0.2))
                            .frame(width: 40, height: 40)
                        Text("\(scan.overallScore)")
                            .font(.caption.bold())
                            .foregroundStyle(scoreColor(scan.overallScore))
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text(scan.goal.displayName)
                            .font(.subheadline)
                        Text(scan.timestamp.formatted(date: .abbreviated, time: .omitted))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Text(scan.bodyType.displayName)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Photo Capture

    private var photoCaptureContent: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Instructions
                VStack(spacing: 8) {
                    Image(systemName: "camera.viewfinder")
                        .font(.system(size: 40))
                        .foregroundStyle(AppTheme.primary)
                    Text("Take Your Photo")
                        .font(.title3.bold())
                    Text("Stand in a well-lit area. Take front and side photos for the most accurate analysis.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.top, 20)

                // Pose guide
                HStack(spacing: 20) {
                    poseGuide(title: "Front", icon: "figure.stand")
                    poseGuide(title: "Side", icon: "figure.walk")
                }
                .padding(.horizontal, 32)

                // Preview
                if let image = viewModel.capturedImage {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                        .frame(maxHeight: 300)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }

                // Capture buttons
                VStack(spacing: 12) {
                    Button {
                        showCamera = true
                    } label: {
                        Label("Take Photo", systemImage: "camera.fill")
                            .font(.headline)
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(AppTheme.primary, in: RoundedRectangle(cornerRadius: 14))
                    }

                    if viewModel.capturedImage != nil {
                        Button {
                            Task { await viewModel.startAnalysis() }
                        } label: {
                            Label("Analyze", systemImage: "sparkles")
                                .font(.headline)
                                .foregroundStyle(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(Color(hex: "FF9800"), in: RoundedRectangle(cornerRadius: 14))
                        }
                    }

                    Button {
                        viewModel.resetToCapture()
                    } label: {
                        Text("Back")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .padding(16)
        }
    }

    private func poseGuide(title: String, icon: String) -> some View {
        VStack(spacing: 8) {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .stroke(style: StrokeStyle(lineWidth: 2, dash: [6]))
                    .foregroundStyle(AppTheme.primary.opacity(0.4))
                    .frame(width: 100, height: 140)
                Image(systemName: icon)
                    .font(.system(size: 50))
                    .foregroundStyle(AppTheme.primary.opacity(0.3))
            }
            Text(title).font(.caption.bold())
        }
    }

    // MARK: - Analyzing

    private var analyzingContent: some View {
        VStack(spacing: 24) {
            Spacer()
            ProgressView(value: viewModel.analysisProgress)
                .tint(AppTheme.primary)
                .frame(width: 200)
            Text(viewModel.analysisStatus)
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text("\(Int(viewModel.analysisProgress * 100))%")
                .font(.title.bold())
                .foregroundStyle(AppTheme.primary)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Results

    private var resultsContent: some View {
        ScrollView {
            if let scan = viewModel.currentScan {
                VStack(spacing: 16) {
                    // Score card
                    scoreCard(scan: scan)

                    // Body composition
                    bodyCompositionCard(scan: scan)

                    // Muscle balance
                    muscleBalanceCard(scan: scan)

                    // Posture assessment
                    postureCard(scan: scan)

                    // Focus zones
                    focusZonesCard(scan: scan)

                    // Actions
                    VStack(spacing: 12) {
                        Button {
                            viewModel.resetToCapture()
                        } label: {
                            Label("New Scan", systemImage: "camera")
                                .font(.headline)
                                .foregroundStyle(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(AppTheme.primary, in: RoundedRectangle(cornerRadius: 14))
                        }
                        Button {
                            viewModel.toggleHistorySheet()
                        } label: {
                            Label("View History", systemImage: "clock.arrow.circlepath")
                                .font(.subheadline)
                                .foregroundStyle(AppTheme.primary)
                        }
                    }
                }
                .padding(16)
            }
        }
    }

    private func scoreCard(scan: BodyScanResult) -> some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .stroke(Color.gray.opacity(0.2), lineWidth: 12)
                    .frame(width: 120, height: 120)
                Circle()
                    .trim(from: 0, to: CGFloat(scan.overallScore) / 100)
                    .stroke(scoreColor(scan.overallScore), style: StrokeStyle(lineWidth: 12, lineCap: .round))
                    .frame(width: 120, height: 120)
                    .rotationEffect(.degrees(-90))
                VStack(spacing: 0) {
                    Text("\(scan.overallScore)")
                        .font(.system(size: 36, weight: .bold))
                        .foregroundStyle(scoreColor(scan.overallScore))
                    Text("Score")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }

            HStack(spacing: 20) {
                VStack(spacing: 2) {
                    Text("Body Type").font(.caption).foregroundStyle(.secondary)
                    Text(scan.bodyType.displayName).font(.subheadline.bold())
                }
                VStack(spacing: 2) {
                    Text("Goal").font(.caption).foregroundStyle(.secondary)
                    Text(scan.goal.displayName).font(.subheadline.bold())
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(colors: [scoreColor(scan.overallScore).opacity(0.1), .clear],
                           startPoint: .top, endPoint: .bottom),
            in: RoundedRectangle(cornerRadius: 16)
        )
    }

    private func bodyCompositionCard(scan: BodyScanResult) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Body Composition").font(.headline)

            if let bf = scan.estimatedBodyFatPercentage {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Estimated Body Fat")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Text(String(format: "%.1f%%", bf))
                            .font(.title2.bold())
                    }
                    Spacer()
                    ZStack {
                        Circle()
                            .stroke(Color.gray.opacity(0.2), lineWidth: 8)
                            .frame(width: 60, height: 60)
                        Circle()
                            .trim(from: 0, to: CGFloat(bf / 50))
                            .stroke(bodyFatColor(bf), style: StrokeStyle(lineWidth: 8, lineCap: .round))
                            .frame(width: 60, height: 60)
                            .rotationEffect(.degrees(-90))
                    }
                }
            }

            Text("Focus Zones: \(scan.focusZones.count)")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    private func muscleBalanceCard(scan: BodyScanResult) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Muscle Balance", systemImage: "scalemass")
                .font(.headline)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                BalanceIndicator(title: "Overall", level: scan.muscleBalance.overallBalance)
                BalanceIndicator(title: "Left/Right", level: scan.muscleBalance.leftRightSymmetry)
                BalanceIndicator(title: "Front/Back", level: scan.muscleBalance.frontBackBalance)
                BalanceIndicator(title: "Upper/Lower", level: scan.muscleBalance.upperLowerBalance)
            }

            ForEach(scan.muscleBalance.imbalances) { imbalance in
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: "exclamationmark.triangle")
                        .foregroundStyle(.orange)
                        .font(.caption)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(imbalance.imbalanceDescription).font(.caption)
                        Text(imbalance.correction).font(.caption2).foregroundStyle(AppTheme.primary)
                    }
                }
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    private func postureCard(scan: BodyScanResult) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Label("Posture Assessment", systemImage: "figure.stand")
                    .font(.headline)
                Spacer()
                Text(scan.postureAssessment.overallPosture.displayName)
                    .font(.subheadline.bold())
                    .foregroundStyle(postureColor(scan.postureAssessment.overallPosture))
            }

            ForEach(scan.postureAssessment.issues) { issue in
                VStack(alignment: .leading, spacing: 6) {
                    HStack(spacing: 8) {
                        Circle()
                            .fill(issueSeverityColor(issue.severity))
                            .frame(width: 8, height: 8)
                        Text(issue.type.displayName)
                            .font(.subheadline.weight(.medium))
                        Spacer()
                        Text(issue.severity.displayName)
                            .font(.caption2)
                            .foregroundStyle(issueSeverityColor(issue.severity))
                    }
                    Text(issue.issueDescription)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    if !issue.exercises.isEmpty {
                        Text("Try: \(issue.exercises.prefix(2).joined(separator: ", "))")
                            .font(.caption)
                            .foregroundStyle(AppTheme.primary)
                    }
                }
                .padding(10)
                .background(issueSeverityColor(issue.severity).opacity(0.05), in: RoundedRectangle(cornerRadius: 8))
            }

            if scan.postureAssessment.issues.isEmpty {
                HStack(spacing: 8) {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(Color(hex: "4CAF50"))
                    Text("No significant posture issues detected")
                        .font(.subheadline)
                }
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    private func focusZonesCard(scan: BodyScanResult) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Focus Zones", systemImage: "target")
                .font(.headline)

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                ForEach(scan.focusZones) { zone in
                    Text(zone.displayName)
                        .font(.caption.bold())
                        .padding(.horizontal, 10)
                        .padding(.vertical, 8)
                        .frame(maxWidth: .infinity)
                        .background(Color(hex: "FF9800").opacity(0.15), in: RoundedRectangle(cornerRadius: 8))
                        .foregroundStyle(Color(hex: "FF9800"))
                }
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Helpers

    private func scoreColor(_ score: Int) -> Color {
        switch score {
        case 80...100: return Color(hex: "4CAF50")
        case 60..<80: return Color(hex: "FF9800")
        default: return Color(hex: "F44336")
        }
    }

    private func bodyFatColor(_ bf: Float) -> Color {
        switch bf {
        case ..<15: return Color(hex: "4CAF50")
        case 15..<25: return Color(hex: "FF9800")
        default: return Color(hex: "F44336")
        }
    }

    private func postureColor(_ level: PostureLevel) -> Color {
        switch level {
        case .excellent: return Color(hex: "4CAF50")
        case .good: return Color(hex: "8BC34A")
        case .fair: return Color(hex: "FF9800")
        case .poor: return Color(hex: "F44336")
        }
    }

    private func issueSeverityColor(_ severity: IssueSeverity) -> Color {
        switch severity {
        case .mild: return Color(hex: "FFEB3B")
        case .moderate: return Color(hex: "FF9800")
        case .severe: return Color(hex: "F44336")
        }
    }
}

// MARK: - Goal Card

private struct GoalCard: View {
    let goal: FitnessGoal
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 8) {
                Image(systemName: goal.icon)
                    .font(.title2)
                    .foregroundStyle(isSelected ? .white : AppTheme.primary)
                Text(goal.displayName)
                    .font(.caption.bold())
                    .foregroundStyle(isSelected ? .white : .primary)
                    .multilineTextAlignment(.center)
            }
            .padding(14)
            .frame(maxWidth: .infinity)
            .background(isSelected ? AppTheme.primary : AppTheme.surfaceContainer,
                        in: RoundedRectangle(cornerRadius: 12))
        }
    }
}

// MARK: - Balance Indicator

private struct BalanceIndicator: View {
    let title: String
    let level: BalanceLevel

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title).font(.caption2).foregroundStyle(.secondary)
            Text(level.displayName)
                .font(.caption.bold())
                .foregroundStyle(balanceColor)
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(balanceColor.opacity(0.08), in: RoundedRectangle(cornerRadius: 8))
    }

    private var balanceColor: Color {
        switch level {
        case .balanced: return Color(hex: "4CAF50")
        case .slightImbalance: return Color(hex: "FF9800")
        case .moderateImbalance: return Color(hex: "F44336")
        case .significantImbalance: return Color(hex: "B71C1C")
        }
    }
}

// MARK: - Camera Capture (UIImagePickerController wrapper)

struct CameraCapture: UIViewControllerRepresentable {
    let onCapture: (UIImage) -> Void
    let onCancel: () -> Void

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.cameraDevice = .front
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: CameraCapture
        init(_ parent: CameraCapture) { self.parent = parent }

        func imagePickerController(_ picker: UIImagePickerController,
                                   didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            if let image = info[.originalImage] as? UIImage {
                parent.onCapture(image)
            }
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.onCancel()
        }
    }
}

// MARK: - History Sheet

private struct BodyHistorySheet: View {
    let scanHistory: [BodyScanResult]
    let scoreHistory: [(date: Date, score: Int)]
    let onSelectScan: (Int) -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    if scoreHistory.count > 1 {
                        scoreChart
                    }

                    ForEach(scanHistory) { scan in
                        Button { onSelectScan(scan.id) } label: {
                            HStack(spacing: 12) {
                                ZStack {
                                    Circle()
                                        .fill(historyScoreColor(scan.overallScore).opacity(0.2))
                                        .frame(width: 44, height: 44)
                                    Text("\(scan.overallScore)")
                                        .font(.subheadline.bold())
                                        .foregroundStyle(historyScoreColor(scan.overallScore))
                                }
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(scan.goal.displayName)
                                        .font(.subheadline.weight(.medium))
                                    Text(scan.timestamp.formatted(date: .abbreviated, time: .omitted))
                                        .font(.caption2)
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            .padding(.vertical, 4)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(16)
            }
            .navigationTitle("Scan History")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private var scoreChart: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Score Over Time").font(.headline)

            Chart {
                ForEach(Array(scoreHistory.enumerated()), id: \.offset) { _, point in
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

    private func historyScoreColor(_ score: Int) -> Color {
        switch score {
        case 80...100: return Color(hex: "4CAF50")
        case 60..<80: return Color(hex: "FF9800")
        default: return Color(hex: "F44336")
        }
    }
}

import Charts

#Preview {
    NavigationStack {
        BodyAnalysisScreen()
    }
}
