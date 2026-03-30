import SwiftUI

struct MeditationScreen: View {
    @Bindable var viewModel: MindfulnessViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var timer: Timer?
    @State private var showRatingDialog = false
    @State private var selectedRating = 0
    @State private var bellTimer: Timer?

    var body: some View {
        ZStack {
            sessionBackground
                .ignoresSafeArea()

            if let state = viewModel.guidedSessionState {
                guidedSessionView(state)
            } else if viewModel.meditationIsRunning {
                freeTimerView
            } else {
                meditationSetupView
            }
        }
        .onDisappear {
            stopTimers()
        }
        .alert("How was your session?", isPresented: $showRatingDialog) {
            Button("Skip") {
                viewModel.completeGuidedSession(rating: nil)
                dismiss()
            }
            Button("Submit") {
                viewModel.completeGuidedSession(rating: max(selectedRating, 1))
                dismiss()
            }
        } message: {
            Text("Rate from 1-5 stars by tapping stars on the session screen.")
        }
    }

    // MARK: - Background

    private var sessionBackground: some View {
        Group {
            if let state = viewModel.guidedSessionState {
                LinearGradient(
                    colors: [
                        sessionBgColor(state.content.type).opacity(0.9),
                        Color(hex: "121212")
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            } else {
                LinearGradient(
                    colors: [Color(hex: "1A237E").opacity(0.8), Color(hex: "121212")],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
        }
    }

    private func sessionBgColor(_ type: MindfulnessType) -> Color {
        switch type {
        case .preRunFocus: return Color(hex: "1A237E")
        case .postRunGratitude: return Color(hex: "1B5E20")
        case .bodyScan: return Color(hex: "4A148C")
        case .stressRelief: return Color(hex: "0D47A1")
        default: return Color(hex: "1A237E")
        }
    }

    // MARK: - Guided Session View

    private func guidedSessionView(_ state: GuidedSessionState) -> some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Button {
                    viewModel.cancelGuidedSession()
                    dismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.title3)
                        .foregroundStyle(.white.opacity(0.7))
                        .frame(width: 44, height: 44)
                }
                Spacer()
                Text(state.content.title)
                    .font(.headline)
                    .foregroundStyle(.white)
                Spacer()
                Button {
                    if state.isPaused {
                        viewModel.resumeGuidedSession()
                    } else {
                        viewModel.pauseGuidedSession()
                    }
                } label: {
                    Image(systemName: state.isPaused ? "play.fill" : "pause.fill")
                        .font(.title3)
                        .foregroundStyle(.white.opacity(0.7))
                        .frame(width: 44, height: 44)
                }
            }
            .padding(.horizontal)
            .padding(.top, 16)

            // Progress
            ProgressView(value: state.progress)
                .progressViewStyle(.linear)
                .tint(.white)
                .padding(.horizontal, 24)
                .padding(.top, 12)

            Text("Step \(state.currentStepIndex + 1) of \(state.content.instructions.count)")
                .font(.caption)
                .foregroundStyle(.white.opacity(0.6))
                .padding(.top, 8)

            Spacer()

            // Icon
            Image(systemName: state.content.type == .breathingExercise ? "wind" : "brain.head.profile")
                .font(.system(size: 64))
                .foregroundStyle(.white.opacity(0.5))
                .symbolEffect(.pulse, isActive: !state.isPaused)

            Spacer()

            // Current instruction
            VStack(spacing: 16) {
                Text(state.currentInstruction)
                    .font(.title3.weight(.medium))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
                    .lineSpacing(6)
                    .padding(.horizontal, 24)
                    .contentTransition(.opacity)
                    .animation(.easeInOut(duration: 0.5), value: state.currentStepIndex)

                Text("\(state.secondsRemaining)s")
                    .font(.system(size: 40, weight: .light, design: .rounded))
                    .foregroundStyle(.white.opacity(0.7))
                    .contentTransition(.numericText())

                if state.isPaused {
                    Text("Paused")
                        .font(.subheadline)
                        .foregroundStyle(.white.opacity(0.5))
                }
            }

            Spacer()

            // Navigation
            HStack(spacing: 20) {
                if state.currentStepIndex > 0 {
                    Button {
                        viewModel.goToPreviousStep()
                    } label: {
                        HStack {
                            Image(systemName: "chevron.left")
                            Text("Previous")
                        }
                        .font(.subheadline)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 12)
                        .background(Color.white.opacity(0.15))
                        .foregroundStyle(.white)
                        .clipShape(Capsule())
                    }
                }

                Button {
                    if state.isLastStep {
                        stopTimers()
                        showRatingDialog = true
                    } else {
                        viewModel.goToNextStep()
                    }
                } label: {
                    HStack {
                        Text(state.isLastStep ? "Complete" : "Next")
                        Image(systemName: state.isLastStep ? "checkmark" : "chevron.right")
                    }
                    .font(.subheadline.weight(.semibold))
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color.white)
                    .foregroundStyle(.black)
                    .clipShape(Capsule())
                }
            }
            .padding(.bottom, 40)
        }
        .onAppear { startGuidedTimer() }
    }

    // MARK: - Free Timer / Meditation Setup

    private var meditationSetupView: some View {
        VStack(spacing: 32) {
            Spacer()

            Image(systemName: "figure.mind.and.body")
                .font(.system(size: 80))
                .foregroundStyle(.white.opacity(0.5))

            Text("Meditation Timer")
                .font(.title.weight(.bold))
                .foregroundStyle(.white)

            // Duration picker
            VStack(spacing: 8) {
                Text("Duration")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.6))

                HStack(spacing: 12) {
                    ForEach([1, 3, 5, 10, 15, 20, 30], id: \.self) { minutes in
                        Button {
                            viewModel.meditationDuration = minutes
                        } label: {
                            Text("\(minutes)")
                                .font(.subheadline.weight(.medium))
                                .frame(width: 40, height: 40)
                                .background(
                                    viewModel.meditationDuration == minutes
                                        ? Color(hex: "4CAF50")
                                        : Color.white.opacity(0.15)
                                )
                                .foregroundStyle(.white)
                                .clipShape(Circle())
                        }
                    }
                }

                Text("minutes")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.5))
            }

            // Ambient sound
            VStack(spacing: 8) {
                Text("Ambient Sound")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.6))

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(AmbientSound.allCases, id: \.self) { sound in
                            Button {
                                viewModel.selectedAmbientSound = sound
                            } label: {
                                HStack(spacing: 6) {
                                    Image(systemName: sound.icon)
                                        .font(.caption)
                                    Text(sound.rawValue)
                                        .font(.caption)
                                }
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .background(
                                    viewModel.selectedAmbientSound == sound
                                        ? Color(hex: "2196F3").opacity(0.3)
                                        : Color.white.opacity(0.1)
                                )
                                .foregroundStyle(
                                    viewModel.selectedAmbientSound == sound
                                        ? Color(hex: "2196F3")
                                        : .white.opacity(0.6)
                                )
                                .clipShape(Capsule())
                            }
                        }
                    }
                    .padding(.horizontal, 24)
                }
            }

            // Bell interval
            VStack(spacing: 8) {
                Text("Bell Interval")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.6))
                HStack(spacing: 12) {
                    ForEach([0, 1, 2, 5, 10], id: \.self) { interval in
                        Button {
                            viewModel.meditationBellInterval = interval
                        } label: {
                            Text(interval == 0 ? "Off" : "\(interval)m")
                                .font(.caption.weight(.medium))
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(
                                    viewModel.meditationBellInterval == interval
                                        ? Color(hex: "FF9800").opacity(0.3)
                                        : Color.white.opacity(0.1)
                                )
                                .foregroundStyle(
                                    viewModel.meditationBellInterval == interval
                                        ? Color(hex: "FF9800")
                                        : .white.opacity(0.6)
                                )
                                .clipShape(Capsule())
                        }
                    }
                }
            }

            Spacer()

            Button {
                viewModel.startMeditation()
                startFreeTimer()
            } label: {
                HStack {
                    Image(systemName: "play.fill")
                    Text("Begin Meditation")
                }
                .font(.headline)
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color(hex: "4CAF50"))
                .foregroundStyle(.white)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .padding(.horizontal, 32)

            Button {
                dismiss()
            } label: {
                Text("Cancel")
                    .foregroundStyle(.white.opacity(0.5))
            }
            .padding(.bottom, 24)
        }
    }

    // MARK: - Free Timer Running

    private var freeTimerView: some View {
        VStack(spacing: 32) {
            Spacer()

            // Animated pulsing circle
            ZStack {
                Circle()
                    .fill(Color(hex: "4CAF50").opacity(0.1))
                    .frame(width: 240, height: 240)
                    .scaleEffect(pulseAnimation ? 1.1 : 0.9)
                    .animation(.easeInOut(duration: 3).repeatForever(autoreverses: true), value: pulseAnimation)

                Circle()
                    .stroke(Color(hex: "4CAF50").opacity(0.3), lineWidth: 2)
                    .frame(width: 200, height: 200)

                VStack(spacing: 8) {
                    Text(timeString(viewModel.meditationSecondsRemaining))
                        .font(.system(size: 48, weight: .light, design: .rounded))
                        .foregroundStyle(.white)
                        .contentTransition(.numericText())
                        .monospacedDigit()

                    Text("remaining")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.5))
                }
            }
            .onAppear { pulseAnimation = true }

            if viewModel.selectedAmbientSound != .none {
                Label(viewModel.selectedAmbientSound.rawValue, systemImage: viewModel.selectedAmbientSound.icon)
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.4))
            }

            Spacer()

            Button {
                viewModel.cancelMeditation()
                stopTimers()
                dismiss()
            } label: {
                Text("End Meditation")
                    .font(.subheadline.weight(.medium))
                    .padding(.horizontal, 32)
                    .padding(.vertical, 12)
                    .background(Color.white.opacity(0.15))
                    .foregroundStyle(.white)
                    .clipShape(Capsule())
            }
            .padding(.bottom, 40)
        }
    }

    @State private var pulseAnimation = false

    // MARK: - Timer Management

    private func startGuidedTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            viewModel.tickGuidedSession()
        }
    }

    private func startFreeTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            viewModel.tickMeditation()
            // Bell
            if viewModel.meditationBellInterval > 0 {
                let elapsed = (viewModel.meditationDuration * 60) - viewModel.meditationSecondsRemaining
                if elapsed > 0 && elapsed % (viewModel.meditationBellInterval * 60) == 0 {
                    triggerBellHaptic()
                }
            }
            // Complete check
            if !viewModel.meditationIsRunning {
                stopTimers()
                dismiss()
            }
        }
    }

    private func stopTimers() {
        timer?.invalidate()
        timer = nil
        bellTimer?.invalidate()
        bellTimer = nil
    }

    private func triggerBellHaptic() {
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)
    }

    private func timeString(_ totalSeconds: Int) -> String {
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return String(format: "%d:%02d", minutes, seconds)
    }
}

#Preview {
    MeditationScreen(viewModel: MindfulnessViewModel())
        .preferredColorScheme(.dark)
}
