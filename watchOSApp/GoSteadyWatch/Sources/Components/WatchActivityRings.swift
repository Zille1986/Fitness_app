import SwiftUI

struct WatchActivityRings: View {

    let move: Double    // 0.0 - 1.0+
    let exercise: Double
    let stand: Double

    private let lineWidth: CGFloat = 5
    private let gap: CGFloat = 3

    var body: some View {
        ZStack {
            // Stand (outer)
            ringPair(progress: stand, color: .cyan, radius: radiusFor(ring: 0))
            // Exercise (middle)
            ringPair(progress: exercise, color: .green, radius: radiusFor(ring: 1))
            // Move (inner)
            ringPair(progress: move, color: .red, radius: radiusFor(ring: 2))
        }
    }

    private func radiusFor(ring: Int) -> CGFloat {
        let outerRadius: CGFloat = 28
        return outerRadius - CGFloat(ring) * (lineWidth + gap)
    }

    @ViewBuilder
    private func ringPair(progress: Double, color: Color, radius: CGFloat) -> some View {
        Circle()
            .stroke(color.opacity(0.2), lineWidth: lineWidth)
            .frame(width: radius * 2, height: radius * 2)

        Circle()
            .trim(from: 0, to: min(progress, 1.0))
            .stroke(color, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
            .frame(width: radius * 2, height: radius * 2)
            .rotationEffect(.degrees(-90))
    }
}

// MARK: - Mini Activity Ring (for lists)

struct MiniActivityRing: View {

    let progress: Double
    let color: Color
    var size: CGFloat = 24
    var lineWidth: CGFloat = 3

    var body: some View {
        ZStack {
            Circle()
                .stroke(color.opacity(0.2), lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: min(progress, 1.0))
                .stroke(color, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))
        }
        .frame(width: size, height: size)
    }
}
