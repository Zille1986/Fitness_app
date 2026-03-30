import SwiftUI
import AVFoundation

struct FormAnalysisScreen: View {
    @State private var viewModel = FormAnalysisViewModel()

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                // Mode selector
                modeSelector

                // AI auto-detect info
                if viewModel.mode == .autoDetect && viewModel.currentResult == nil && !viewModel.isCameraActive {
                    aiInfoCard
                }

                // Detected exercise badge
                if viewModel.mode == .autoDetect, let name = viewModel.detectedExerciseName {
                    detectedExerciseBadge(name: name)
                }

                // Exercise selector for gym mode
                if viewModel.mode == .gym && viewModel.analysisPhase == .idle {
                    exerciseSelector
                }

                // Form score card
                formScoreCard

                // Camera / analysis area
                cameraSection

                // Results
                if let result = viewModel.currentResult {
                    issuesList(result: result)
                    tipsList(result: result)
                }

                // History
                if !viewModel.analysisHistory.isEmpty {
                    historySection
                }
            }
            .padding(16)
        }
        .navigationTitle("Form Analysis")
    }

    // MARK: - Mode Selector

    private var modeSelector: some View {
        HStack(spacing: 8) {
            ForEach(FormAnalysisMode.allCases, id: \.rawValue) { mode in
                Button {
                    viewModel.setMode(mode)
                } label: {
                    Text(mode.rawValue)
                        .font(.caption.bold())
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(
                            viewModel.mode == mode ? AppTheme.primary : AppTheme.surfaceContainer,
                            in: Capsule()
                        )
                        .foregroundStyle(viewModel.mode == mode ? .white : .primary)
                }
            }
        }
    }

    // MARK: - AI Info Card

    private var aiInfoCard: some View {
        VStack(spacing: 8) {
            Image(systemName: "sparkles")
                .font(.largeTitle)
                .foregroundStyle(AppTheme.primary)
            Text("AI Form Coach")
                .font(.title3.bold())
            Text("Just start exercising - the AI will automatically recognize what you're doing and provide form feedback")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(20)
        .background(AppTheme.primaryContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Detected Exercise

    private func detectedExerciseBadge(name: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(AppTheme.primary)
            VStack(alignment: .leading, spacing: 2) {
                Text("Exercise Detected")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(name)
                    .font(.headline)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppTheme.primaryContainer.opacity(0.5), in: RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Exercise Selector

    private var exerciseSelector: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Select Exercise").font(.headline)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(GymExerciseType.allCases) { exercise in
                        Button {
                            viewModel.setExercise(exercise)
                        } label: {
                            Text(exercise.displayName)
                                .font(.caption.bold())
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(
                                    viewModel.selectedExercise == exercise
                                        ? AppTheme.gym : AppTheme.surfaceContainer,
                                    in: Capsule()
                                )
                                .foregroundStyle(viewModel.selectedExercise == exercise ? .white : .primary)
                        }
                    }
                }
            }
        }
    }

    // MARK: - Form Score

    private var formScoreCard: some View {
        let score = viewModel.currentResult?.overallScore ?? 0
        let hasResult = viewModel.currentResult != nil

        return VStack(spacing: 12) {
            ZStack {
                Circle()
                    .stroke(Color.gray.opacity(0.2), lineWidth: 10)
                    .frame(width: 100, height: 100)
                Circle()
                    .trim(from: 0, to: hasResult ? CGFloat(score) / 100 : 0)
                    .stroke(
                        scoreColor(score),
                        style: StrokeStyle(lineWidth: 10, lineCap: .round)
                    )
                    .frame(width: 100, height: 100)
                    .rotationEffect(.degrees(-90))
                    .animation(.easeOut(duration: 1), value: score)

                if hasResult {
                    VStack(spacing: 0) {
                        Text("\(score)")
                            .font(.title.bold())
                            .foregroundStyle(scoreColor(score))
                        Text("/ 100")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                } else {
                    Text("--")
                        .font(.title2)
                        .foregroundStyle(.secondary)
                }
            }

            if hasResult, let result = viewModel.currentResult {
                Text(result.summary)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Camera Section

    private var cameraSection: some View {
        VStack(spacing: 12) {
            switch viewModel.analysisPhase {
            case .idle:
                CameraPreviewPlaceholder()
                    .frame(height: 250)
                    .clipShape(RoundedRectangle(cornerRadius: 12))

                HStack(spacing: 12) {
                    Button {
                        viewModel.startAnalysis()
                    } label: {
                        Label("Start Analysis", systemImage: "camera.fill")
                            .font(.headline)
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(AppTheme.primary, in: RoundedRectangle(cornerRadius: 12))
                    }

                    Button {
                        viewModel.runDemoAnalysis()
                    } label: {
                        Label("Demo", systemImage: "play.fill")
                            .font(.headline)
                            .foregroundStyle(AppTheme.primary)
                            .padding(.vertical, 14)
                            .padding(.horizontal, 20)
                            .background(AppTheme.primaryContainer, in: RoundedRectangle(cornerRadius: 12))
                    }
                }

            case .countdown:
                ZStack {
                    CameraPreviewPlaceholder()
                        .frame(height: 250)
                        .clipShape(RoundedRectangle(cornerRadius: 12))

                    Text("\(viewModel.countdownSeconds)")
                        .font(.system(size: 72, weight: .bold))
                        .foregroundStyle(.white)
                        .shadow(radius: 8)
                }

            case .collecting:
                ZStack {
                    CameraPreviewPlaceholder()
                        .frame(height: 250)
                        .clipShape(RoundedRectangle(cornerRadius: 12))

                    VStack {
                        Spacer()
                        HStack(spacing: 16) {
                            VStack(spacing: 2) {
                                Text("\(viewModel.framesCollected)")
                                    .font(.headline.bold())
                                    .foregroundStyle(.white)
                                Text("Frames")
                                    .font(.caption2)
                                    .foregroundStyle(.white.opacity(0.8))
                            }
                            if viewModel.mode == .gym {
                                VStack(spacing: 2) {
                                    Text("\(viewModel.repsDetected)")
                                        .font(.headline.bold())
                                        .foregroundStyle(.white)
                                    Text("Reps")
                                        .font(.caption2)
                                        .foregroundStyle(.white.opacity(0.8))
                                }
                            }
                            VStack(spacing: 2) {
                                Text(viewModel.dataQuality.rawValue)
                                    .font(.caption.bold())
                                    .foregroundStyle(.white)
                                Text("Quality")
                                    .font(.caption2)
                                    .foregroundStyle(.white.opacity(0.8))
                            }
                        }
                        .padding(12)
                        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 10))
                        .padding(12)
                    }
                }

                Button {
                    viewModel.stopAnalysis()
                } label: {
                    Label("Stop", systemImage: "stop.fill")
                        .font(.headline)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.red, in: RoundedRectangle(cornerRadius: 12))
                }

            case .analyzing:
                VStack(spacing: 16) {
                    ProgressView(value: viewModel.analysisProgress)
                        .tint(AppTheme.primary)
                    Text("Analyzing form...")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .padding(20)
                .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))

            case .results:
                Button {
                    viewModel.resetAnalysis()
                } label: {
                    Label("New Analysis", systemImage: "arrow.counterclockwise")
                        .font(.headline)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(AppTheme.primary, in: RoundedRectangle(cornerRadius: 12))
                }
            }
        }
    }

    // MARK: - Issues List

    private func issuesList(result: FormAnalysisDisplay) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Issues Found").font(.headline)

            if result.issues.isEmpty {
                HStack(spacing: 8) {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(Color(hex: "4CAF50"))
                    Text("No form issues detected!")
                        .font(.subheadline)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(hex: "4CAF50").opacity(0.1), in: RoundedRectangle(cornerRadius: 12))
            } else {
                ForEach(result.issues) { issue in
                    FormIssueDisplayRow(issue: issue)
                }
            }
        }
    }

    // MARK: - Tips

    private func tipsList(result: FormAnalysisDisplay) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Improvement Tips").font(.headline)

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
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - History

    private var historySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Analysis History").font(.headline)

            ForEach(viewModel.analysisHistory) { entry in
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(scoreColor(entry.score).opacity(0.2))
                            .frame(width: 40, height: 40)
                        Text("\(entry.score)")
                            .font(.caption.bold())
                            .foregroundStyle(scoreColor(entry.score))
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text(entry.exerciseName)
                            .font(.subheadline.weight(.medium))
                        Text(entry.date.formatted(date: .abbreviated, time: .omitted))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }

                    Spacer()

                    Text("\(entry.issueCount) issue\(entry.issueCount == 1 ? "" : "s")")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding(.vertical, 4)
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
}

// MARK: - Form Issue Row

private struct FormIssueDisplayRow: View {
    let issue: FormIssueDisplay

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Circle()
                    .fill(severityColor)
                    .frame(width: 10, height: 10)
                Text(issue.bodyPart)
                    .font(.subheadline.bold())
                Spacer()
                Text(issue.severity.displayName)
                    .font(.caption2.bold())
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(severityColor.opacity(0.15), in: Capsule())
                    .foregroundStyle(severityColor)
            }
            Text(issue.description)
                .font(.caption)
                .foregroundStyle(.secondary)
            HStack(spacing: 4) {
                Image(systemName: "lightbulb.fill")
                    .font(.caption2)
                    .foregroundStyle(.yellow)
                Text(issue.suggestion)
                    .font(.caption)
                    .foregroundStyle(AppTheme.primary)
            }
        }
        .padding(12)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 12))
    }

    private var severityColor: Color {
        switch issue.severity {
        case .low: return Color(hex: "FFEB3B")
        case .medium: return Color(hex: "FF9800")
        case .high: return Color(hex: "F44336")
        }
    }
}

// MARK: - Camera Preview Placeholder

struct CameraPreviewPlaceholder: View {
    var body: some View {
        ZStack {
            Rectangle()
                .fill(Color.black.opacity(0.85))
            VStack(spacing: 12) {
                Image(systemName: "camera.fill")
                    .font(.largeTitle)
                    .foregroundStyle(.white.opacity(0.6))
                Text("Camera Preview")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.6))

                // Silhouette guide
                Image(systemName: "figure.stand")
                    .font(.system(size: 80))
                    .foregroundStyle(.white.opacity(0.15))
            }
        }
    }
}

#Preview {
    NavigationStack {
        FormAnalysisScreen()
    }
}
