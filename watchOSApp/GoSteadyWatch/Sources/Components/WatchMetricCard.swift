import SwiftUI

struct WatchMetricCard: View {

    let label: String
    let value: String
    let unit: String
    var color: Color = .white

    var body: some View {
        VStack(spacing: 2) {
            Text(label.uppercased())
                .font(.system(size: 9, weight: .bold))
                .foregroundStyle(.secondary)

            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text(value)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(color)
                if !unit.isEmpty {
                    Text(unit)
                        .font(.system(size: 10))
                        .foregroundStyle(.secondary)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 4)
    }
}

// MARK: - Compact Variant

struct WatchMetricCardCompact: View {

    let label: String
    let value: String
    let unit: String
    var color: Color = .white

    var body: some View {
        VStack(spacing: 1) {
            HStack(alignment: .lastTextBaseline, spacing: 1) {
                Text(value)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(color)
                Text(unit)
                    .font(.system(size: 8))
                    .foregroundStyle(.secondary)
            }
            Text(label)
                .font(.system(size: 8))
                .foregroundStyle(.secondary)
        }
    }
}
