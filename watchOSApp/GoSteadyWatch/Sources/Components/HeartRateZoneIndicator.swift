import SwiftUI

struct HeartRateZoneIndicator: View {

    let heartRate: Int
    let zone: HRZone
    let maxHR: Int

    var body: some View {
        GeometryReader { geo in
            let totalWidth = geo.size.width
            let barHeight: CGFloat = 8

            ZStack(alignment: .leading) {
                // Background zones
                HStack(spacing: 1) {
                    ForEach(HRZone.allCases, id: \.rawValue) { z in
                        RoundedRectangle(cornerRadius: 2)
                            .fill(z == zone ? z.color : z.color.opacity(0.3))
                            .frame(height: barHeight)
                    }
                }

                // Indicator dot
                let position = indicatorPosition(width: totalWidth)
                Circle()
                    .fill(.white)
                    .frame(width: 10, height: 10)
                    .shadow(color: .black.opacity(0.5), radius: 2)
                    .offset(x: position - 5)
            }
            .frame(height: barHeight + 4)
        }
    }

    private func indicatorPosition(width: CGFloat) -> CGFloat {
        let minHR = Double(maxHR) * 0.5  // Zone 1 start
        let maxHRDouble = Double(maxHR)
        let range = maxHRDouble - minHR
        guard range > 0 else { return 0 }
        let normalized = (Double(heartRate) - minHR) / range
        return CGFloat(normalized.clamped(to: 0...1)) * width
    }
}

// MARK: - Vertical Zone Bar

struct HeartRateZoneBar: View {

    let zone: HRZone
    let isActive: Bool

    var body: some View {
        VStack(spacing: 2) {
            RoundedRectangle(cornerRadius: 2)
                .fill(isActive ? zone.color : zone.color.opacity(0.3))
                .frame(width: isActive ? 8 : 4, height: isActive ? 24 : 16)
                .animation(.spring(response: 0.3), value: isActive)

            Text("Z\(zone.rawValue)")
                .font(.system(size: 7))
                .foregroundStyle(isActive ? zone.color : .secondary)
        }
    }
}

// MARK: - Clamped Extension

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}
