import SwiftUI

struct WatchTimerView: View {

    let seconds: TimeInterval
    var color: Color = WatchTheme.primary
    var style: TimerStyle = .large

    enum TimerStyle {
        case large
        case medium
        case compact
    }

    var body: some View {
        switch style {
        case .large:
            Text(formatDuration(seconds))
                .font(.system(size: 36, weight: .bold, design: .monospaced))
                .foregroundStyle(color)
                .monospacedDigit()

        case .medium:
            Text(formatDuration(seconds))
                .font(.system(size: 24, weight: .bold, design: .monospaced))
                .foregroundStyle(color)
                .monospacedDigit()

        case .compact:
            Text(formatDuration(seconds))
                .font(.system(size: 16, weight: .semibold, design: .monospaced))
                .foregroundStyle(color)
                .monospacedDigit()
        }
    }
}

// MARK: - Countdown Timer View

struct CountdownTimerView: View {

    let remainingSeconds: Int
    var color: Color = .white
    var showProgress: Bool = true
    var totalSeconds: Int = 0

    var body: some View {
        VStack(spacing: 2) {
            let mins = remainingSeconds / 60
            let secs = remainingSeconds % 60

            Text(String(format: "%d:%02d", mins, secs))
                .font(.system(size: 44, weight: .bold, design: .monospaced))
                .foregroundStyle(color)
                .monospacedDigit()

            if showProgress, totalSeconds > 0 {
                ProgressView(value: Double(totalSeconds - remainingSeconds), total: Double(totalSeconds))
                    .tint(color)
                    .padding(.horizontal, 20)
            }
        }
    }
}
