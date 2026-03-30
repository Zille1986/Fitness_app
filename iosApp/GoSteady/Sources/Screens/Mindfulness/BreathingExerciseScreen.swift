import SwiftUI

struct BreathingExerciseScreen: View {
    @Bindable var viewModel: MindfulnessViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var circleScale: CGFloat = 0.3
    @State private var timer: Timer?

    private var state: BreathingSessionState? { viewModel.breathingState }

    var body: some View {
        ZStack {
            // Background
            Color(hex: "121212")
                .ignoresSafeArea()

            if let state = state {
                activeSessionView(state)
            }
        }
        .onAppear { startTimer() }
        .onDisappear { stopTimer() }
        .onChange(of: state?.phase) { _, newPhase in
            guard let phase = newPhase, let pattern = state?.pattern else { return }
            animateCircle(for: phase, pattern: pattern)
            triggerHaptic(for: phase)
        }
    }

    // MARK: - Active Session

    private func activeSessionView(_ state: BreathingSessionState) -> some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Button {
                    viewModel.cancelBreathingSession()
                    dismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.title3)
                        .foregroundStyle(.white.opacity(0.7))
                        .frame(width: 44, height: 44)
                }
                Spacer()
                VStack(spacing: 2) {
                    Text(state.pattern.name)
                        .font(.headline)
                        .foregroundStyle(.white)
                    Text("Cycle \(state.currentCycle) of \(state.pattern.cycles)")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.6))
                }
                Spacer()
                Color.clear.frame(width: 44, height: 44)
            }
            .padding(.horizontal)
            .padding(.top, 16)

            // Progress bar
            GeometryReader { geo in
                let totalCycles = state.pattern.cycles
                let progress = CGFloat(state.currentCycle - 1) / CGFloat(totalCycles)
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.white.opacity(0.15))
                        .frame(height: 4)
                    Capsule()
                        .fill(Color.white.opacity(0.6))
                        .frame(width: geo.size.width * progress, height: 4)
                        .animation(.easeInOut(duration: 0.3), value: progress)
                }
            }
            .frame(height: 4)
            .padding(.horizontal, 24)
            .padding(.top, 16)

            Spacer()

            // Breathing Circle
            ZStack {
                // Outer glow
                Circle()
                    .fill(phaseColor(state.phase).opacity(0.08))
                    .scaleEffect(circleScale * 1.3)

                // Middle ring
                Circle()
                    .fill(phaseColor(state.phase).opacity(0.15))
                    .scaleEffect(circleScale * 1.1)

                // Main circle
                Circle()
                    .fill(phaseColor(state.phase).opacity(0.25))
                    .scaleEffect(circleScale)

                // Inner circle with border
                Circle()
                    .stroke(phaseColor(state.phase), lineWidth: 3)
                    .scaleEffect(circleScale)

                // Content
                VStack(spacing: 12) {
                    Text(state.phase.displayText)
                        .font(.title.weight(.semibold))
                        .foregroundStyle(.white)
                        .contentTransition(.opacity)
                        .animation(.easeInOut(duration: 0.3), value: state.phase.displayText)

                    Text("\(state.secondsRemaining)")
                        .font(.system(size: 56, weight: .bold, design: .rounded))
                        .foregroundStyle(.white)
                        .contentTransition(.numericText())
                }
            }
            .frame(width: 250, height: 250)

            Spacer()

            // Phase indicators
            phaseIndicators(state)

            Spacer()

            // End Session
            Button {
                viewModel.cancelBreathingSession()
                dismiss()
            } label: {
                Text("End Session")
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

    // MARK: - Phase Indicators

    private func phaseIndicators(_ state: BreathingSessionState) -> some View {
        HStack(spacing: 24) {
            phaseIndicator(
                label: "In",
                seconds: state.pattern.inhaleSeconds,
                isActive: state.phase == .inhale,
                colorHex: "4CAF50"
            )
            if state.pattern.holdAfterInhale > 0 {
                phaseIndicator(
                    label: "Hold",
                    seconds: state.pattern.holdAfterInhale,
                    isActive: state.phase == .holdIn,
                    colorHex: "2196F3"
                )
            }
            phaseIndicator(
                label: "Out",
                seconds: state.pattern.exhaleSeconds,
                isActive: state.phase == .exhale,
                colorHex: "9C27B0"
            )
            if state.pattern.holdAfterExhale > 0 {
                phaseIndicator(
                    label: "Hold",
                    seconds: state.pattern.holdAfterExhale,
                    isActive: state.phase == .holdOut,
                    colorHex: "FF9800"
                )
            }
        }
    }

    private func phaseIndicator(label: String, seconds: Int, isActive: Bool, colorHex: String) -> some View {
        VStack(spacing: 4) {
            Text("\(seconds)s")
                .font(.subheadline.weight(isActive ? .bold : .regular))
                .foregroundStyle(isActive ? Color(hex: colorHex) : .white.opacity(0.4))
            Text(label)
                .font(.caption2)
                .foregroundStyle(isActive ? Color(hex: colorHex) : .white.opacity(0.3))
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(isActive ? Color(hex: colorHex).opacity(0.15) : Color.clear)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    // MARK: - Animation & Timer

    private func animateCircle(for phase: BreathingPhase, pattern: BreathingPattern) {
        let duration: Double
        let target: CGFloat
        switch phase {
        case .inhale:
            duration = Double(pattern.inhaleSeconds)
            target = 1.0
        case .holdIn:
            duration = 0.1
            target = 1.0
        case .exhale:
            duration = Double(pattern.exhaleSeconds)
            target = 0.3
        case .holdOut:
            duration = 0.1
            target = 0.3
        }
        withAnimation(.easeInOut(duration: duration)) {
            circleScale = target
        }
    }

    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            viewModel.tickBreathingSession()
            // Check if complete
            if viewModel.breathingState == nil {
                stopTimer()
                dismiss()
            }
        }
        // Initial animation
        if let state = state {
            animateCircle(for: state.phase, pattern: state.pattern)
        }
    }

    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }

    private func phaseColor(_ phase: BreathingPhase) -> Color {
        Color(hex: phase.colorHex)
    }

    private func triggerHaptic(for phase: BreathingPhase) {
        switch phase {
        case .inhale:
            let generator = UIImpactFeedbackGenerator(style: .medium)
            generator.impactOccurred()
        case .holdIn, .holdOut:
            let generator = UIImpactFeedbackGenerator(style: .light)
            generator.impactOccurred()
        case .exhale:
            let generator = UIImpactFeedbackGenerator(style: .soft)
            generator.impactOccurred()
        }
    }
}

#Preview {
    BreathingExerciseScreen(viewModel: MindfulnessViewModel())
        .preferredColorScheme(.dark)
}
