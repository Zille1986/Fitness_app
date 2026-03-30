import SwiftUI
import UIKit
import SwiftData

struct SettingsScreen: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.colorScheme) private var colorScheme
    @AppStorage("isDarkMode") private var isDarkMode = true
    @State private var viewModel = SettingsViewModel()
    @State private var showExportOptions = false

    var body: some View {
        NavigationStack {
            List {
                // Appearance
                Section("Appearance") {
                    Toggle(isOn: $isDarkMode) {
                        HStack(spacing: AppSpacing.md) {
                            Image(systemName: isDarkMode ? "moon.fill" : "sun.max.fill")
                                .foregroundStyle(isDarkMode ? .purple : AppTheme.secondary)
                                .frame(width: 28)
                            VStack(alignment: .leading) {
                                Text("Dark Mode")
                                Text(isDarkMode ? "Dark theme active" : "Light theme active")
                                    .font(AppTypography.captionLarge)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }

                // Units
                Section("Measurement") {
                    HStack {
                        Image(systemName: "ruler")
                            .foregroundStyle(AppTheme.accent)
                            .frame(width: 28)
                        Text("Units")
                        Spacer()
                        Picker("", selection: Binding(
                            get: { viewModel.useMetricUnits },
                            set: { _ in viewModel.toggleUnits() }
                        )) {
                            Text("Metric").tag(true)
                            Text("Imperial").tag(false)
                        }
                        .pickerStyle(.segmented)
                        .frame(width: 180)
                    }
                }

                // Notifications
                Section("Notifications") {
                    Toggle(isOn: $viewModel.notificationsEnabled) {
                        HStack(spacing: AppSpacing.md) {
                            Image(systemName: "bell.fill")
                                .foregroundStyle(AppTheme.secondary)
                                .frame(width: 28)
                            VStack(alignment: .leading) {
                                Text("Push Notifications")
                                Text("Workout reminders and updates")
                                    .font(AppTypography.captionLarge)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }

                    Toggle(isOn: $viewModel.workoutReminders) {
                        HStack(spacing: AppSpacing.md) {
                            Image(systemName: "alarm.fill")
                                .foregroundStyle(AppTheme.primary)
                                .frame(width: 28)
                            VStack(alignment: .leading) {
                                Text("Workout Reminders")
                                Text("Daily training reminders")
                                    .font(AppTypography.captionLarge)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }

                // Integrations
                Section("Integrations") {
                    // Strava
                    Button {
                        if viewModel.isStravaConnected {
                            viewModel.disconnectStrava()
                        } else {
                            viewModel.connectStrava()
                        }
                    } label: {
                        HStack(spacing: AppSpacing.md) {
                            Image(systemName: "link")
                                .foregroundStyle(AppTheme.strava)
                                .frame(width: 28)
                            VStack(alignment: .leading) {
                                Text("Strava")
                                    .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
                                Text(viewModel.isStravaConnected ? "Connected -- Tap to disconnect" : "Tap to connect")
                                    .font(AppTypography.captionLarge)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            if viewModel.isStravaConnected {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundStyle(AppTheme.success)
                            } else {
                                Image(systemName: "chevron.right")
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }

                    // Apple Health
                    Button {
                        Task { await viewModel.requestHealthPermissions() }
                    } label: {
                        HStack(spacing: AppSpacing.md) {
                            Image(systemName: "heart.fill")
                                .foregroundStyle(.red)
                                .frame(width: 28)
                            VStack(alignment: .leading) {
                                Text("Apple Health")
                                    .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
                                Text(viewModel.isHealthConnected ? "Connected" : "Tap to grant permissions")
                                    .font(AppTypography.captionLarge)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            if viewModel.isHealthConnected {
                                Image(systemName: "checkmark.circle.fill")
                                    .foregroundStyle(AppTheme.success)
                            } else {
                                Image(systemName: "chevron.right")
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }

                // Data
                Section("Data") {
                    Button {
                        showExportOptions = true
                    } label: {
                        HStack(spacing: AppSpacing.md) {
                            Image(systemName: "square.and.arrow.up")
                                .foregroundStyle(AppTheme.accent)
                                .frame(width: 28)
                            VStack(alignment: .leading) {
                                Text("Export Data")
                                    .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
                                Text("Download your workouts as CSV or JSON")
                                    .font(AppTypography.captionLarge)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            if viewModel.isExporting {
                                ProgressView()
                            }
                        }
                    }
                    .confirmationDialog("Export Format", isPresented: $showExportOptions) {
                        Button("CSV") { viewModel.exportData(format: .csv) }
                        Button("JSON") { viewModel.exportData(format: .json) }
                        Button("Cancel", role: .cancel) {}
                    }
                }

                // About
                Section("About") {
                    NavigationLink {
                        PrivacyPolicyView()
                    } label: {
                        HStack(spacing: AppSpacing.md) {
                            Image(systemName: "hand.raised.fill")
                                .foregroundStyle(AppTheme.tertiary)
                                .frame(width: 28)
                            Text("Privacy Policy")
                        }
                    }

                    HStack(spacing: AppSpacing.md) {
                        Image(systemName: "info.circle.fill")
                            .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                            .frame(width: 28)
                        Text("Version")
                        Spacer()
                        Text("\(viewModel.appVersion) (\(viewModel.buildNumber))")
                            .foregroundStyle(.secondary)
                    }
                }

                // Danger Zone
                Section {
                    Button(role: .destructive) {
                        viewModel.showDeleteConfirmation = true
                    } label: {
                        HStack(spacing: AppSpacing.md) {
                            Image(systemName: "trash.fill")
                                .foregroundStyle(AppTheme.error)
                                .frame(width: 28)
                            Text("Delete All Data")
                                .foregroundStyle(AppTheme.error)
                        }
                    }
                    .alert("Delete All Data?", isPresented: $viewModel.showDeleteConfirmation) {
                        Button("Delete Everything", role: .destructive) {
                            viewModel.deleteAllData()
                        }
                        Button("Cancel", role: .cancel) {}
                    } message: {
                        Text("This will permanently delete all your workouts, plans, and profile data. This action cannot be undone.")
                    }
                }
            }
            .navigationTitle("Settings")
            .sheet(isPresented: $viewModel.showExportSheet) {
                if let url = viewModel.exportFileURL {
                    ShareSheet(items: [url])
                }
            }
        }
        .onAppear {
            viewModel.load(modelContext: modelContext)
        }
    }
}

// MARK: - Privacy Policy

struct PrivacyPolicyView: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: AppSpacing.lg) {
                Text("Privacy Policy")
                    .font(AppTypography.headlineMedium)

                Text("Last updated: March 2026")
                    .font(AppTypography.captionLarge)
                    .foregroundStyle(.secondary)

                Group {
                    Text("Data Collection")
                        .font(AppTypography.titleMedium)
                    Text("GoSteady collects fitness data that you provide, including workouts, body measurements, and nutrition logs. All data is stored locally on your device using SwiftData and is never transmitted to external servers without your explicit consent.")

                    Text("Apple Health Integration")
                        .font(AppTypography.titleMedium)
                    Text("When you grant permission, GoSteady reads and writes workout data to Apple Health. This data stays within the Health ecosystem and is governed by Apple's privacy policies.")

                    Text("Strava Integration")
                        .font(AppTypography.titleMedium)
                    Text("If you connect your Strava account, GoSteady can import and export workout data via the Strava API. OAuth tokens are stored securely on your device.")

                    Text("AI Coach")
                        .font(AppTypography.titleMedium)
                    Text("When you use the AI Coach feature, your fitness summary (not raw data) is sent to Google's Gemini API to generate personalized advice. No personally identifiable information is shared beyond what you type in chat.")

                    Text("Your Rights")
                        .font(AppTypography.titleMedium)
                    Text("You can export or delete all your data at any time from Settings. Deleting the app removes all locally stored data.")
                }
                .font(AppTypography.bodyMedium)
            }
            .padding()
        }
        .navigationTitle("Privacy Policy")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Share Sheet

struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
