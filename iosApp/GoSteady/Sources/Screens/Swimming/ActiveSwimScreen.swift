import SwiftUI
import UIKit

struct ActiveSwimScreen: View {
    let swimType: SwimType
    let poolLength: PoolLength?

    @State private var viewModel = ActiveSwimViewModel()
    @State private var showFinishAlert = false
    @State private var showDiscardAlert = false
    @State private var navigateToDetail = false
    @State private var savedWorkoutId: String?
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext

    var body: some View {
        VStack(spacing: 0) {
            // Phase header
            phaseHeader

            ScrollView {
                VStack(spacing: 24) {
                    // Timer
                    timerDisplay

                    // Stats row
                    statsRow

                    Divider().padding(.horizontal)

                    // Pool: Lap controls
                    if swimType == .pool {
                        poolControls
                    } else {
                        openWaterInfo
                    }

                    // Rest interval
                    restIntervalSection

                    // Splits list
                    if !viewModel.splits.isEmpty {
                        splitsSection
                    }

                    Spacer(minLength: 32)

                    // Auto-save indicator
                    Text("Auto-saving every 30s")
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                }
                .padding(.horizontal)
                .padding(.top, 24)
                .padding(.bottom, 120)
            }

            // Bottom controls
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
                Text("\(swimType.displayName) Swim")
                    .fontWeight(.bold)
                    .foregroundStyle(.white)
            }
        }
        .onAppear {
            viewModel.configure(swimmingRepository: SwimmingRepository(context: modelContext))
            if !viewModel.isActive {
                viewModel.startWorkout(swimType: swimType, poolLength: poolLength)
            }
            UIApplication.shared.isIdleTimerDisabled = true
        }
        .onDisappear {
            UIApplication.shared.isIdleTimerDisabled = false
        }
        .alert("Finish Swim?", isPresented: $showFinishAlert) {
            Button("Save") {
                viewModel.finishWorkout { id in
                    savedWorkoutId = id
                    dismiss()
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Save this \(viewModel.elapsedFormatted) swim with \(viewModel.laps) laps (\(viewModel.distanceFormatted)m)?")
        }
        .alert("Discard Swim?", isPresented: $showDiscardAlert) {
            Button("Discard", role: .destructive) {
                viewModel.discardWorkout()
                dismiss()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to discard this workout?")
        }
    }

    // MARK: - Phase Header

    private var phaseHeader: some View {
        HStack {
            Circle()
                .fill(viewModel.isPaused ? Color.yellow : (viewModel.isInRestInterval ? Color.blue : Color.green))
                .frame(width: 8, height: 8)
            Text(viewModel.isPaused ? "PAUSED" : (viewModel.isInRestInterval ? "REST INTERVAL" : "ACTIVE"))
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(viewModel.isPaused ? Color.yellow : (viewModel.isInRestInterval ? Color.blue : Color.green))
            Spacer()
            if let poolLength {
                Text(poolLength.displayName)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.white.opacity(0.1))
                    .clipShape(Capsule())
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
        .background(Color(hex: "1E1E1E"))
    }

    // MARK: - Timer Display

    private var timerDisplay: some View {
        VStack(spacing: 4) {
            Text(viewModel.elapsedFormatted)
                .font(.system(size: 72, weight: .bold, design: .monospaced))
                .foregroundStyle(.white)
                .contentTransition(.numericText())
                .animation(.linear(duration: 0.3), value: viewModel.elapsedMillis)

            if viewModel.isInRestInterval {
                Text("Rest: \(viewModel.restIntervalFormatted)")
                    .font(.title3)
                    .fontWeight(.semibold)
                    .foregroundStyle(.blue)
            }
        }
    }

    // MARK: - Stats Row

    private var statsRow: some View {
        HStack(spacing: 0) {
            swimStatColumn(value: "\(viewModel.laps)", label: "Laps")
            Spacer()
            swimStatColumn(value: viewModel.distanceFormatted, label: "Meters")
            Spacer()
            swimStatColumn(value: viewModel.avgPaceFormatted, label: "Pace/100m")
        }
    }

    private func swimStatColumn(value: String, label: String) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.title)
                .fontWeight(.bold)
                .foregroundStyle(.white)
                .contentTransition(.numericText())
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    // MARK: - Pool Controls

    private var poolControls: some View {
        VStack(spacing: 16) {
            // Large lap button
            Button {
                let generator = UIImpactFeedbackGenerator(style: .heavy)
                generator.impactOccurred()
                viewModel.addLap()
            } label: {
                VStack(spacing: 4) {
                    Image(systemName: "plus")
                        .font(.title)
                        .fontWeight(.bold)
                    Text("LAP")
                        .font(.caption)
                        .fontWeight(.bold)
                }
                .foregroundStyle(.white)
                .frame(width: 120, height: 120)
                .background(AppTheme.swimming)
                .clipShape(Circle())
                .shadow(color: AppTheme.swimming.opacity(0.4), radius: 12, y: 4)
            }
            .sensoryFeedback(.impact(weight: .heavy), trigger: viewModel.laps)

            // Undo button
            Button {
                viewModel.undoLap()
            } label: {
                HStack(spacing: 4) {
                    Image(systemName: "arrow.uturn.backward")
                        .font(.caption)
                    Text("Undo last lap")
                        .font(.caption)
                }
                .foregroundStyle(viewModel.laps > 0 ? AppTheme.swimming : .secondary)
            }
            .disabled(viewModel.laps == 0)

            Text("Tap after each lap (\(poolLength?.displayName ?? "25m"))")
                .font(.caption2)
                .foregroundStyle(.secondary)

            // Last lap pace
            if let lastPace = viewModel.lastLapPace {
                HStack(spacing: 4) {
                    Image(systemName: "stopwatch")
                        .font(.caption2)
                    Text("Last lap: \(lastPace)/100m")
                        .font(.caption)
                }
                .foregroundStyle(AppTheme.swimming)
            }
        }
    }

    // MARK: - Open Water Info

    private var openWaterInfo: some View {
        VStack(spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: "location.fill")
                    .foregroundStyle(AppTheme.swimming)
                VStack(alignment: .leading, spacing: 2) {
                    Text("GPS tracking active")
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundStyle(.white)
                    Text("Distance updates automatically")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }
            .padding(12)
            .background(Color.white.opacity(0.05))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    // MARK: - Rest Interval

    private var restIntervalSection: some View {
        VStack(spacing: 8) {
            if viewModel.isInRestInterval {
                Button {
                    viewModel.endRestInterval()
                } label: {
                    HStack {
                        Image(systemName: "stop.fill")
                        Text("End Rest Interval")
                    }
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(Color.blue)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            } else {
                Button {
                    viewModel.startRestInterval()
                } label: {
                    HStack {
                        Image(systemName: "pause.circle")
                        Text("Start Rest Interval")
                    }
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(Color.white.opacity(0.05))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
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
                    Text("Lap \(split.lapNumber)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .frame(width: 50, alignment: .leading)
                    Spacer()
                    Text(String(format: "%.0fm", split.distanceMeters))
                        .font(.caption)
                        .foregroundStyle(.white)
                    Spacer()
                    Text(SwimmingViewModel.formatPace(split.paceSecondsPer100m))
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundStyle(AppTheme.swimming)
                }
                .padding(.vertical, 4)
            }

            if viewModel.splits.count > 5 {
                Text("+ \(viewModel.splits.count - 5) more laps")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
        }
        .padding(12)
        .background(Color.white.opacity(0.03))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Control Bar

    private var controlBar: some View {
        HStack(spacing: 32) {
            // Pause/Resume
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

            // Finish
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
