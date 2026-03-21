import SwiftUI

struct SettingsScreen: View {
    @AppStorage("isDarkMode") private var isDarkMode = true

    var body: some View {
        NavigationStack {
            List {
                Section("Appearance") {
                    Toggle(isOn: $isDarkMode) {
                        HStack(spacing: 12) {
                            Image(systemName: isDarkMode ? "moon.fill" : "sun.max.fill")
                                .foregroundStyle(isDarkMode ? .purple : AppTheme.primary)
                            VStack(alignment: .leading) {
                                Text("Dark Mode")
                                Text(isDarkMode ? "Dark theme active" : "Light theme active")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }

                Section("Integrations") {
                    NavigationLink {
                        Text("Strava Settings")
                    } label: {
                        Label("Strava", systemImage: "link")
                    }

                    NavigationLink {
                        Text("Apple Health")
                    } label: {
                        Label("Health", systemImage: "heart.fill")
                    }
                }

                Section("About") {
                    NavigationLink {
                        PrivacyPolicyView()
                    } label: {
                        Label("Privacy Policy", systemImage: "hand.raised.fill")
                    }

                    HStack {
                        Text("Version")
                        Spacer()
                        Text("1.0")
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .navigationTitle("Settings")
        }
    }
}

struct PrivacyPolicyView: View {
    var body: some View {
        ScrollView {
            Text("Privacy policy content loads here from shared assets.")
                .padding()
        }
        .navigationTitle("Privacy Policy")
        .navigationBarTitleDisplayMode(.inline)
    }
}
