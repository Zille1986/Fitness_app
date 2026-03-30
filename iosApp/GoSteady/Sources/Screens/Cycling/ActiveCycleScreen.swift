import SwiftUI
import UIKit
import MapKit

struct ActiveCycleScreen: View {
    let cyclingType: CyclingType

    @State private var viewModel = ActiveCycleViewModel()
    @State private var showFinishAlert = false
    @State private var showDiscardAlert = false
    @State private var showMap = true
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext

    var body: some View {
        VStack(spacing: 0) {
            // Map area (outdoor only)
            if cyclingType == .outdoor && showMap {
                mapSection
            }

            // Stats area
            ScrollView {
                VStack(spacing: 20) {
                    // Timer
                    timerDisplay

                    // Primary stats grid
                    primaryStatsGrid

                    // Secondary stats
                    secondaryStatsRow

                    // Splits
                    if !viewModel.splits.isEmpty {
                        splitsSection
                    }

                    Spacer(minLength: 32)
                }
                .padding(.horizontal)
                .padding(.top, 16)
                .padding(.bottom, 120)
            }

            // Control bar
            controlBar
        }
        .background(Color(hex: "121212"))
        .preferredColorScheme(.dark)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button {
                    showDiscardAlert = true
                } label: {
                    Image(systemName: "xmark")
                        .fontWeight(.bold)
                        .foregroundStyle(.white)
                }
            }
            ToolbarItem(placement: .principal) {
                Text(cyclingType.displayName)
                    .fontWeight(.bold)
                    .foregroundStyle(.white)
            }
            if cyclingType == .outdoor {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        withAnimation { showMap.toggle() }
                    } label: {
                        Image(systemName: showMap ? "map.fill" : "map")
                            .foregroundStyle(.white)
                    }
                }
            }
        }
        .onAppear {
            viewModel.configure(cyclingRepository: CyclingRepository(context: modelContext))
            if !viewModel.isActive {
                viewModel.startWorkout(type: cyclingType)
            }
            UIApplication.shared.isIdleTimerDisabled = true
        }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = false
        }
        .alert("Finish Ride?", isPresented: $showFinishAlert) {
            Button("Save") {
                viewModel.finishWorkout { _ in
                    dismiss()
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Save this \(viewModel.elapsedFormatted) ride (\(viewModel.distanceFormatted) km)?")
        }
        .alert("Discard Ride?", isPresented: $showDiscardAlert) {
            Button("Discard", role: .destructive) {
                viewModel.discardWorkout()
                dismiss()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to discard this workout?")
        }
    }

    // MARK: - Map Section

    private var mapSection: some View {
        ZStack(alignment: .topTrailing) {
            Map {
                if viewModel.routeCoordinates.count > 1 {
                    MapPolyline(coordinates: viewModel.routeCoordinates.map {
                        CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lon)
                    })
                    .stroke(Color(hex: "7EE787"), lineWidth: 4)
                }

                if let last = viewModel.routeCoordinates.last {
                    Annotation("", coordinate: CLLocationCoordinate2D(latitude: last.lat, longitude: last.lon)) {
                        Circle()
                            .fill(AppTheme.cycling)
                            .frame(width: 12, height: 12)
                            .overlay(
                                Circle()
                                    .stroke(Color.white, lineWidth: 2)
                            )
                    }
                }
            }
            .mapStyle(.standard(elevation: .realistic))
            .frame(height: 200)
            .clipShape(RoundedRectangle(cornerRadius: 0))
        }
    }

    // MARK: - Timer

    private var timerDisplay: some View {
        VStack(spacing: 4) {
            Text(viewModel.elapsedFormatted)
                .font(.system(size: 56, weight: .bold, design: .monospaced))
                .foregroundStyle(.white)
                .contentTransition(.numericText())
                .animation(.linear(duration: 0.3), value: viewModel.elapsedMillis)

            HStack(spacing: 4) {
                Circle()
                    .fill(viewModel.isPaused ? Color.yellow : Color.green)
                    .frame(width: 6, height: 6)
                Text(viewModel.isPaused ? "PAUSED" : "ACTIVE")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundStyle(viewModel.isPaused ? Color.yellow : Color.green)
            }
        }
    }

    // MARK: - Primary Stats

    private var primaryStatsGrid: some View {
        LazyVGrid(columns: [
            GridItem(.flexible()),
            GridItem(.flexible()),
            GridItem(.flexible())
        ], spacing: 16) {
            liveStatCard(
                value: viewModel.speedFormatted,
                unit: "km/h",
                label: "Speed",
                color: AppTheme.cycling,
                isLarge: true
            )
            liveStatCard(
                value: viewModel.distanceFormatted,
                unit: "km",
                label: "Distance",
                color: .white,
                isLarge: true
            )
            liveStatCard(
                value: viewModel.elevationFormatted,
                unit: "m",
                label: "Elevation",
                color: .orange,
                isLarge: true
            )
        }
    }

    // MARK: - Secondary Stats

    private var secondaryStatsRow: some View {
        HStack(spacing: 12) {
            secondaryStatPill(
                icon: "gauge.with.needle",
                value: viewModel.avgSpeedFormatted,
                unit: "km/h",
                label: "Avg Speed"
            )
            secondaryStatPill(
                icon: "bolt.fill",
                value: String(format: "%.1f", viewModel.maxSpeedKmh),
                unit: "km/h",
                label: "Max Speed"
            )
            if let hr = viewModel.currentHeartRate {
                secondaryStatPill(
                    icon: "heart.fill",
                    value: "\(hr)",
                    unit: "bpm",
                    label: "Heart Rate"
                )
            }
            if viewModel.currentCadence > 0 {
                secondaryStatPill(
                    icon: "arrow.triangle.2.circlepath",
                    value: "\(viewModel.currentCadence)",
                    unit: "rpm",
                    label: "Cadence"
                )
            }
        }
    }

    private func liveStatCard(value: String, unit: String, label: String, color: Color, isLarge: Bool) -> some View {
        VStack(spacing: 4) {
            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text(value)
                    .font(isLarge ? .title : .title3)
                    .fontWeight(.bold)
                    .foregroundStyle(color)
                    .contentTransition(.numericText())
                Text(unit)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(Color.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func secondaryStatPill(icon: String, value: String, unit: String, label: String) -> some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption2)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(.white)
            Text(label)
                .font(.system(size: 8))
                .foregroundStyle(.tertiary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .background(Color.white.opacity(0.03))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    // MARK: - Splits

    private var splitsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("SPLITS")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(.secondary)

            ForEach(viewModel.splits.suffix(5).reversed()) { split in
                HStack {
                    Text("KM \(split.kilometer)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .frame(width: 50, alignment: .leading)
                    Spacer()
                    Text(split.durationFormatted)
                        .font(.caption)
                        .foregroundStyle(.white)
                    Spacer()
                    Text(String(format: "%.1f km/h", split.avgSpeedKmh))
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundStyle(AppTheme.cycling)
                }
                .padding(.vertical, 4)
            }
        }
        .padding(12)
        .background(Color.white.opacity(0.03))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Control Bar

    private var controlBar: some View {
        HStack(spacing: 32) {
            Button {
                let generator = UIImpactFeedbackGenerator(style: .medium)
                generator.impactOccurred()
                if viewModel.isPaused {
                    viewModel.resumeWorkout()
                } else {
                    viewModel.pauseWorkout()
                }
            } label: {
                Image(systemName: viewModel.isPaused ? "play.fill" : "pause.fill")
                    .font(.title2)
                    .foregroundStyle(.white)
                    .frame(width: 64, height: 64)
                    .background(Color.white.opacity(0.15))
                    .clipShape(Circle())
            }

            Button {
                let generator = UINotificationFeedbackGenerator()
                generator.notificationOccurred(.success)
                showFinishAlert = true
            } label: {
                Image(systemName: "checkmark")
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundStyle(.white)
                    .frame(width: 64, height: 64)
                    .background(Color(hex: "4CAF50"))
                    .clipShape(Circle())
            }
        }
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity)
        .background(Color(hex: "1E1E1E"))
    }
}
