import SwiftUI
import SwiftData
import MapKit

struct ActiveRunScreen: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @State private var viewModel = ActiveRunViewModel()
    @State private var showStopConfirmation = false
    @State private var showSettings = false
    @State private var mapCameraPosition: MapCameraPosition = .automatic
    @State private var navigateToRunDetail = false

    var body: some View {
        ZStack {
            // Background
            Color.black.ignoresSafeArea()

            VStack(spacing: 0) {
                switch viewModel.runState {
                case .ready:
                    readyView
                case .countdown:
                    countdownView
                case .running, .paused:
                    activeRunView
                case .finished:
                    finishedView
                }
            }

            // Split notification overlay
            if viewModel.showSplitNotification {
                VStack {
                    splitNotificationBanner
                    Spacer()
                }
                .transition(.move(edge: .top).combined(with: .opacity))
                .animation(.spring(response: 0.4, dampingFraction: 0.7), value: viewModel.showSplitNotification)
            }
        }
        .navigationBarBackButtonHidden(viewModel.runState != .ready)
        .toolbar {
            if viewModel.runState == .running || viewModel.runState == .paused {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showSettings = true
                    } label: {
                        Image(systemName: "gearshape")
                            .foregroundStyle(.white.opacity(0.7))
                    }
                }
            }
        }
        .sheet(isPresented: $showSettings) {
            runSettingsSheet
        }
        .navigationDestination(isPresented: $navigateToRunDetail) {
            if let runId = viewModel.finishedRunId {
                RunDetailScreen(runId: runId)
            }
        }
        .onAppear {
            viewModel.configure(modelContext: modelContext)
        }
        .onDisappear {
            if viewModel.runState != .finished {
                viewModel.cleanup()
            }
        }
    }

    // MARK: - Ready View

    private var readyView: some View {
        VStack(spacing: 32) {
            Spacer()

            if !viewModel.isLocationPermissionGranted {
                VStack(spacing: 16) {
                    Image(systemName: "location.fill")
                        .font(.system(size: 60))
                        .foregroundStyle(AppTheme.running)

                    Text("Location Permission Required")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundStyle(.white)

                    Text("GoSteady needs location access to track your run accurately.")
                        .font(.subheadline)
                        .foregroundStyle(.white.opacity(0.7))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)

                    Button {
                        viewModel.requestLocationPermission()
                    } label: {
                        Text("Grant Permission")
                            .fontWeight(.bold)
                            .padding(.horizontal, 32)
                            .padding(.vertical, 14)
                            .background(AppTheme.running)
                            .foregroundStyle(.white)
                            .clipShape(Capsule())
                    }
                }
            } else {
                Image(systemName: "figure.run")
                    .font(.system(size: 60))
                    .foregroundStyle(AppTheme.running)

                Text("Ready to Run")
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundStyle(.white)

                Text("Tap start when you're ready")
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.6))

                Button {
                    viewModel.startRun()
                } label: {
                    Circle()
                        .fill(AppTheme.running)
                        .frame(width: 100, height: 100)
                        .overlay {
                            Image(systemName: "play.fill")
                                .font(.system(size: 40))
                                .foregroundStyle(.white)
                        }
                        .shadow(color: AppTheme.running.opacity(0.4), radius: 20, y: 8)
                }
                .padding(.top, 20)
            }

            Spacer()
        }
    }

    // MARK: - Countdown View

    private var countdownView: some View {
        VStack {
            Spacer()
            if let count = viewModel.countdownValue {
                Text("\(count)")
                    .font(.system(size: 120, weight: .bold, design: .rounded))
                    .foregroundStyle(AppTheme.running)
                    .contentTransition(.numericText())
                    .animation(.spring(response: 0.3), value: count)
            }
            Spacer()
        }
    }

    // MARK: - Active Run View

    private var activeRunView: some View {
        VStack(spacing: 0) {
            // Map (top third)
            mapSection
                .frame(height: UIScreen.main.bounds.height * 0.3)

            // Stats
            VStack(spacing: 20) {
                // Distance - large
                VStack(spacing: 2) {
                    Text(String(format: "%.2f", viewModel.trackingState.distanceKm))
                        .font(.system(size: 72, weight: .bold, design: .rounded))
                        .foregroundStyle(AppTheme.running)
                        .contentTransition(.numericText())

                    Text("kilometers")
                        .font(.subheadline)
                        .foregroundStyle(.white.opacity(0.5))
                }

                // Duration
                Text(viewModel.trackingState.durationFormatted)
                    .font(.system(size: 36, weight: .medium, design: .rounded))
                    .foregroundStyle(.white)
                    .contentTransition(.numericText())

                // Secondary stats row
                HStack(spacing: 32) {
                    statBox(label: "Pace", value: viewModel.trackingState.paceFormatted, unit: "/km")
                    statBox(label: "Heart Rate", value: viewModel.trackingState.currentHeartRate.map { "\($0)" } ?? "--", unit: "bpm")
                    statBox(label: "Elevation", value: String(format: "%.0f", viewModel.trackingState.elevationGain), unit: "m")
                }
                .padding(.horizontal)

                Spacer()

                // Controls
                controlButtons
                    .padding(.bottom, 40)
            }
            .padding(.top, 20)
        }
    }

    // MARK: - Map Section

    private var mapSection: some View {
        Map(position: $mapCameraPosition) {
            // Route polyline
            if viewModel.trackingState.routePoints.count >= 2 {
                MapPolyline(coordinates: viewModel.trackingState.routePoints.map {
                    CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude)
                })
                .stroke(AppTheme.running, lineWidth: 4)
            }

            // Current position
            if let current = viewModel.trackingState.currentLocation {
                Annotation("", coordinate: CLLocationCoordinate2D(latitude: current.latitude, longitude: current.longitude)) {
                    Circle()
                        .fill(AppTheme.running)
                        .frame(width: 14, height: 14)
                        .overlay {
                            Circle().stroke(.white, lineWidth: 3)
                        }
                        .shadow(radius: 4)
                }
            }
        }
        .mapStyle(.standard(pointsOfInterest: .excludingAll))
        .allowsHitTesting(false)
        .onChange(of: viewModel.trackingState.currentLocation?.latitude) {
            if let loc = viewModel.trackingState.currentLocation {
                withAnimation(.easeInOut(duration: 0.5)) {
                    mapCameraPosition = .region(MKCoordinateRegion(
                        center: CLLocationCoordinate2D(latitude: loc.latitude, longitude: loc.longitude),
                        latitudinalMeters: 500,
                        longitudinalMeters: 500
                    ))
                }
            }
        }
    }

    // MARK: - Stat Box

    private func statBox(label: String, value: String, unit: String) -> some View {
        VStack(spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundStyle(.white.opacity(0.5))
            HStack(alignment: .lastTextBaseline, spacing: 2) {
                Text(value)
                    .font(.title2)
                    .fontWeight(.bold)
                    .foregroundStyle(.white)
                Text(unit)
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.5))
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(.white.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Control Buttons

    private var controlButtons: some View {
        HStack(spacing: 32) {
            if viewModel.runState == .paused {
                // Stop button
                Button {
                    showStopConfirmation = true
                } label: {
                    Circle()
                        .fill(Color.red)
                        .frame(width: 64, height: 64)
                        .overlay {
                            Image(systemName: "stop.fill")
                                .font(.title2)
                                .foregroundStyle(.white)
                        }
                }
                .alert("Finish Run?", isPresented: $showStopConfirmation) {
                    Button("Finish") {
                        viewModel.stopRun()
                    }
                    Button("Continue", role: .cancel) {}
                } message: {
                    Text("Are you sure you want to finish this run?")
                }

                // Resume button
                Button {
                    viewModel.resumeRun()
                } label: {
                    Circle()
                        .fill(AppTheme.running)
                        .frame(width: 80, height: 80)
                        .overlay {
                            Image(systemName: "play.fill")
                                .font(.system(size: 32))
                                .foregroundStyle(.white)
                        }
                        .shadow(color: AppTheme.running.opacity(0.4), radius: 12, y: 4)
                }
            } else {
                // Lap button
                Button {
                    viewModel.addLap()
                } label: {
                    Circle()
                        .fill(.white.opacity(0.15))
                        .frame(width: 64, height: 64)
                        .overlay {
                            Image(systemName: "flag.fill")
                                .font(.title3)
                                .foregroundStyle(.white)
                        }
                }

                // Pause button
                Button {
                    viewModel.pauseRun()
                } label: {
                    Circle()
                        .fill(Color(hex: "FF9800"))
                        .frame(width: 80, height: 80)
                        .overlay {
                            Image(systemName: "pause.fill")
                                .font(.system(size: 32))
                                .foregroundStyle(.white)
                        }
                        .shadow(color: Color(hex: "FF9800").opacity(0.4), radius: 12, y: 4)
                }
            }
        }
    }

    // MARK: - Split Notification

    private var splitNotificationBanner: some View {
        HStack(spacing: 12) {
            Image(systemName: "flag.checkered")
                .foregroundStyle(AppTheme.running)
            Text(viewModel.splitNotificationText)
                .fontWeight(.semibold)
                .foregroundStyle(.white)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(.ultraThinMaterial)
        .clipShape(Capsule())
        .padding(.top, 60)
    }

    // MARK: - Finished View

    private var finishedView: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 60))
                .foregroundStyle(AppTheme.running)

            Text("Run Complete!")
                .font(.title)
                .fontWeight(.bold)
                .foregroundStyle(.white)

            // Summary stats
            VStack(spacing: 16) {
                HStack(spacing: 24) {
                    summaryStatItem(label: "Distance", value: String(format: "%.2f km", viewModel.trackingState.distanceKm))
                    summaryStatItem(label: "Duration", value: viewModel.trackingState.durationFormatted)
                }
                HStack(spacing: 24) {
                    summaryStatItem(label: "Avg Pace", value: viewModel.trackingState.paceFormatted + " /km")
                    summaryStatItem(label: "Splits", value: "\(viewModel.trackingState.splits.count)")
                }
            }
            .padding(20)
            .background(.white.opacity(0.08))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding(.horizontal, 32)

            Spacer()

            VStack(spacing: 12) {
                Button {
                    navigateToRunDetail = true
                } label: {
                    Text("View Details")
                        .fontWeight(.bold)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(AppTheme.running)
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }

                Button {
                    dismiss()
                } label: {
                    Text("Done")
                        .fontWeight(.medium)
                        .foregroundStyle(.white.opacity(0.7))
                }
            }
            .padding(.horizontal, 32)
            .padding(.bottom, 40)
        }
    }

    private func summaryStatItem(label: String, value: String) -> some View {
        VStack(spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundStyle(.white.opacity(0.5))
            Text(value)
                .font(.title3)
                .fontWeight(.bold)
                .foregroundStyle(.white)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Settings Sheet

    private var runSettingsSheet: some View {
        NavigationStack {
            Form {
                Section("Audio Cues") {
                    Toggle("Enable Audio Cues", isOn: $viewModel.audioCuesEnabled)

                    if viewModel.audioCuesEnabled {
                        Picker("Cue Interval", selection: $viewModel.audioCueDistanceInterval) {
                            Text("Every 0.5 km").tag(0.5)
                            Text("Every 1 km").tag(1.0)
                            Text("Every 2 km").tag(2.0)
                            Text("Every 5 km").tag(5.0)
                        }
                    }
                }
            }
            .navigationTitle("Run Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { showSettings = false }
                }
            }
        }
        .presentationDetents([.medium])
    }
}
