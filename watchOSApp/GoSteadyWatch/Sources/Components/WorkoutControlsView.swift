import SwiftUI
import WatchKit

struct WorkoutControlsView: View {

    @Environment(WatchWorkoutService.self) private var ws
    @State private var showStopConfirmation = false
    @State private var dragOffset: CGFloat = 0
    @State private var swipeToEndReached = false
    private let swipeThreshold: CGFloat = 100

    var body: some View {
        VStack(spacing: 10) {
            Text("CONTROLS")
                .font(.caption2)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            // Pause / Resume
            Button {
                if ws.isPaused { ws.resume() } else { ws.pause() }
            } label: {
                Label(
                    ws.isPaused ? "Resume" : "Pause",
                    systemImage: ws.isPaused ? "play.fill" : "pause.fill"
                )
                .fontWeight(.semibold)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
            }
            .tint(ws.isPaused ? .green : .yellow)

            // Lap (for running)
            if ws.currentActivity.isRunning {
                Button {
                    ws.addLap()
                } label: {
                    Label("Lap", systemImage: "stopwatch.fill")
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                }
                .tint(.blue)
            }

            // End Workout
            Button(role: .destructive) {
                showStopConfirmation = true
            } label: {
                Label("End Workout", systemImage: "stop.fill")
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
            }
            .confirmationDialog("End workout?", isPresented: $showStopConfirmation) {
                Button("Save & End", role: .destructive) {
                    ws.stop()
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Save this \(ws.currentActivity.rawValue.lowercased())?")
            }

            // Swipe to end indicator
            swipeToEndView
        }
        .padding(.horizontal, 8)
    }

    // MARK: - Swipe to End

    private var swipeToEndView: some View {
        ZStack(alignment: .leading) {
            RoundedRectangle(cornerRadius: 20)
                .fill(Color.red.opacity(0.15))
                .frame(height: 36)

            RoundedRectangle(cornerRadius: 20)
                .fill(Color.red.opacity(swipeToEndReached ? 0.8 : 0.4))
                .frame(width: max(36, 36 + dragOffset), height: 36)

            HStack {
                Image(systemName: swipeToEndReached ? "checkmark" : "chevron.right.2")
                    .font(.caption)
                    .foregroundStyle(.white)
                    .frame(width: 36, height: 36)
                    .offset(x: dragOffset)

                Spacer()

                Text("Slide to end")
                    .font(.system(size: 10))
                    .foregroundStyle(.secondary)
                    .opacity(1.0 - Double(dragOffset / swipeThreshold))

                Spacer()
            }
        }
        .gesture(
            DragGesture()
                .onChanged { value in
                    dragOffset = max(0, value.translation.width)
                    swipeToEndReached = dragOffset > swipeThreshold
                }
                .onEnded { _ in
                    if swipeToEndReached {
                        ws.stop()
                    }
                    withAnimation(.spring()) {
                        dragOffset = 0
                        swipeToEndReached = false
                    }
                }
        )
    }
}
