import SwiftUI
import WatchKit

struct ActiveHIITScreen: View {

    @Environment(WatchWorkoutService.self) private var ws
    @Environment(WatchWorkoutViewModel.self) private var vm
    @Environment(PhoneSyncService.self) private var syncService
    @State private var timerActive = true

    var body: some View {
        ZStack {
            // Progress arc
            Circle()
                .trim(from: 0, to: vm.hiitPhaseProgress)
                .stroke(phaseColor, style: StrokeStyle(lineWidth: 6, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .padding(4)
                .animation(.linear(duration: 0.9), value: vm.hiitPhaseProgress)

            Circle()
                .trim(from: 0, to: 1)
                .stroke(phaseColor.opacity(0.2), lineWidth: 6)
                .padding(4)

            VStack(spacing: 4) {
                // Phase label
                Text(vm.hiitPhase.rawValue)
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(phaseColor)

                // Timer
                let mins = vm.hiitRemainingSeconds / 60
                let secs = vm.hiitRemainingSeconds % 60
                Text(String(format: "%d:%02d", mins, secs))
                    .font(.system(size: 40, weight: .bold, design: .monospaced))
                    .foregroundStyle(phaseColor)

                // Exercise name
                Text(vm.hiitCurrentExerciseName)
                    .font(.system(size: 13, weight: .semibold))
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 20)

                // Round counter
                if let template = vm.hiitTemplate {
                    Text("Round \(vm.hiitCurrentRound)/\(template.rounds)")
                        .font(.system(size: 10))
                        .foregroundStyle(.secondary)
                }

                // Next exercise
                if let next = vm.hiitNextExerciseName {
                    Text("Next: \(next)")
                        .font(.system(size: 10))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }

                // Heart rate
                HStack(spacing: 3) {
                    Image(systemName: "heart.fill")
                        .font(.system(size: 8))
                        .foregroundStyle(.red)
                    Text("\(ws.heartRate)")
                        .font(.system(size: 12, weight: .medium))
                }

                // Paused state
                if vm.hiitIsPaused {
                    Text("PAUSED - Tap to resume")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(.yellow)
                }
            }
        }
        .contentShape(Rectangle())
        .onTapGesture {
            if vm.hiitIsComplete {
                // Send session data and end
                let data = vm.buildHIITSessionData()
                syncService.sendHIITSession(data)
                ws.stop()
            } else {
                vm.toggleHIITPause()
            }
        }
        .navigationBarBackButtonHidden(true)
        .onAppear { startTimer() }
        .onDisappear { timerActive = false }
    }

    private var phaseColor: Color {
        switch vm.hiitPhase {
        case .work: return WatchTheme.primary
        case .rest: return .blue
        case .warmup, .cooldown: return .yellow
        case .complete: return WatchTheme.hiit
        }
    }

    private func startTimer() {
        timerActive = true
        Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { timer in
            guard timerActive else {
                timer.invalidate()
                return
            }
            vm.hiitTick()
            if vm.hiitIsComplete {
                timer.invalidate()
            }
        }
    }
}
