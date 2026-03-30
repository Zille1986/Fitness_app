import SwiftUI
import UIKit

private let hiitOrange = Color(hex: "FF6D00")

struct ActiveHIITScreen: View {
    let templateId: String

    @State private var viewModel = ActiveHIITViewModel()
    @State private var showStopAlert = false
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext

    private var phaseColor: Color {
        switch viewModel.phase {
        case .work: return Color(hex: "4CAF50")
        case .rest: return Color(hex: "2196F3")
        case .warmup, .cooldown: return Color(hex: "FFC107")
        case .complete: return hiitOrange
        case .paused: return Color(hex: "9E9E9E")
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Full-screen workout view
            ZStack {
                // Background gradient
                phaseColor.opacity(0.05)
                    .ignoresSafeArea()

                VStack(spacing: 0) {
                    // Top: Phase + Round
                    topSection

                    Spacer()

                    // Center: Timer ring
                    timerRing

                    Spacer()

                    // Bottom: Next exercise + controls
                    bottomSection
                }
                .padding(24)
            }
        }
        .preferredColorScheme(.dark)
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .onAppear {
            viewModel.configure(hiitRepository: HIITRepository(context: modelContext))
            if viewModel.template == nil {
                viewModel.startWorkout(templateId: templateId)
            }
            UIApplication.shared.isIdleTimerDisabled = true
        }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = false
            viewModel.cleanup()
        }
        .alert("Stop Workout?", isPresented: $showStopAlert) {
            Button("Stop", role: .destructive) {
                viewModel.stopWorkout()
            }
            Button("Continue", role: .cancel) {}
        } message: {
            Text("Your progress will be saved.")
        }
        .onChange(of: viewModel.savedSessionId) { _, newValue in
            if newValue != nil && viewModel.isComplete {
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                    dismiss()
                }
            }
        }
    }

    // MARK: - Top Section

    private var topSection: some View {
        VStack(spacing: 8) {
            // Phase badge
            Text(viewModel.phase.rawValue)
                .font(.subheadline)
                .fontWeight(.bold)
                .foregroundStyle(phaseColor)
                .padding(.horizontal, 20)
                .padding(.vertical, 8)
                .background(phaseColor.opacity(0.15))
                .clipShape(RoundedRectangle(cornerRadius: 20))
                .animation(.easeInOut(duration: 0.3), value: viewModel.phase)

            if !viewModel.isComplete {
                Text(viewModel.roundProgress)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }

    // MARK: - Timer Ring

    private var timerRing: some View {
        ZStack {
            // Background ring
            Circle()
                .stroke(phaseColor.opacity(0.15), lineWidth: 12)
                .frame(width: 260, height: 260)

            // Progress arc
            Circle()
                .trim(from: 0, to: CGFloat(viewModel.phaseProgress))
                .stroke(phaseColor, style: StrokeStyle(lineWidth: 12, lineCap: .round))
                .frame(width: 260, height: 260)
                .rotationEffect(.degrees(-90))
                .animation(.linear(duration: 0.9), value: viewModel.phaseProgress)

            // Center content
            VStack(spacing: 4) {
                // Countdown
                Text(viewModel.remainingFormatted)
                    .font(.system(size: 64, weight: .bold, design: .monospaced))
                    .foregroundStyle(phaseColor)
                    .contentTransition(.numericText())
                    .animation(.linear(duration: 0.3), value: viewModel.remainingSeconds)

                // Exercise name
                Text(viewModel.currentExerciseName)
                    .font(.headline)
                    .fontWeight(.semibold)
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 16)
                    .lineLimit(2)

                // Phase step counter for warmup/cooldown
                if (viewModel.phase == .warmup || viewModel.phase == .cooldown) && viewModel.phaseStepCount > 0 {
                    Text("\(viewModel.phaseStepIndex + 1) / \(viewModel.phaseStepCount)")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }

                // Exercise description
                if !viewModel.currentExerciseDescription.isEmpty &&
                   (viewModel.phase == .work || viewModel.phase == .warmup || viewModel.phase == .cooldown) {
                    Text(viewModel.currentExerciseDescription)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 16)
                        .lineLimit(2)
                }
            }
        }
    }

    // MARK: - Bottom Section

    private var bottomSection: some View {
        VStack(spacing: 16) {
            // Next exercise preview
            if let nextName = viewModel.nextExerciseName, !viewModel.isComplete {
                HStack(spacing: 4) {
                    Text("Next:")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(nextName)
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundStyle(.white)
                }
            }

            // Calories + elapsed
            if viewModel.caloriesEstimate > 0 {
                HStack(spacing: 16) {
                    HStack(spacing: 4) {
                        Image(systemName: "flame.fill")
                            .font(.caption2)
                        Text("\(viewModel.caloriesEstimate) cal")
                            .font(.caption)
                    }
                    .foregroundStyle(hiitOrange)

                    HStack(spacing: 4) {
                        Image(systemName: "clock")
                            .font(.caption2)
                        Text(viewModel.totalElapsedFormatted)
                            .font(.caption)
                    }
                    .foregroundStyle(.secondary)
                }
            }

            // Controls
            if viewModel.isComplete {
                completeView
            } else {
                controlButtons
            }
        }
    }

    // MARK: - Control Buttons

    private var controlButtons: some View {
        HStack(spacing: 24) {
            // Stop
            Button {
                showStopAlert = true
            } label: {
                Image(systemName: "stop.fill")
                    .font(.title3)
                    .foregroundStyle(Color(hex: "E53935"))
                    .frame(width: 56, height: 56)
                    .background(Color(hex: "E53935").opacity(0.15))
                    .clipShape(Circle())
            }

            // Pause/Resume
            Button {
                viewModel.togglePause()
            } label: {
                Image(systemName: viewModel.isPaused ? "play.fill" : "pause.fill")
                    .font(.title2)
                    .foregroundStyle(.white)
                    .frame(width: 72, height: 72)
                    .background(phaseColor)
                    .clipShape(Circle())
                    .shadow(color: phaseColor.opacity(0.3), radius: 12, y: 4)
            }
        }
    }

    // MARK: - Complete View

    private var completeView: some View {
        VStack(spacing: 16) {
            Image(systemName: "trophy.fill")
                .font(.system(size: 48))
                .foregroundStyle(hiitOrange)

            Text("Workout Complete!")
                .font(.title3)
                .fontWeight(.bold)
                .foregroundStyle(.white)

            // Stats summary
            HStack(spacing: 24) {
                completeStat(icon: "timer", value: viewModel.totalElapsedFormatted, label: "Duration")
                completeStat(icon: "flame.fill", value: "\(viewModel.caloriesEstimate)", label: "Calories")
                completeStat(icon: "repeat", value: "\(viewModel.currentRound)/\(viewModel.template?.rounds ?? 0)", label: "Rounds")
            }

            Button {
                dismiss()
            } label: {
                Text("Done")
                    .fontWeight(.bold)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(hiitOrange)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
        }
        .transition(.opacity.combined(with: .scale))
    }

    private func completeStat(icon: String, value: String, label: String) -> some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundStyle(hiitOrange)
            Text(value)
                .font(.headline)
                .fontWeight(.bold)
                .foregroundStyle(.white)
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }
}
