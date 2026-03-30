import Foundation
import SwiftUI
import UIKit
import SwiftData
import UniformTypeIdentifiers

@Observable
final class SettingsViewModel {
    var isDarkMode: Bool = true
    var useMetricUnits: Bool = true
    var notificationsEnabled: Bool = true
    var workoutReminders: Bool = true
    var isStravaConnected: Bool = false
    var stravaAthleteName: String? = nil
    var isHealthConnected: Bool = false
    var isExporting: Bool = false
    var exportFormat: ExportFormat = .csv
    var showDeleteConfirmation: Bool = false
    var showExportSheet: Bool = false
    var exportFileURL: URL? = nil
    var appVersion: String = "1.0.0"
    var buildNumber: String = "1"

    private var modelContext: ModelContext?

    init() {
        appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
        buildNumber = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
    }

    func load(modelContext: ModelContext) {
        self.modelContext = modelContext
        let descriptor = FetchDescriptor<UserProfile>()
        if let profile = try? modelContext.fetch(descriptor).first {
            useMetricUnits = profile.preferredUnits == .metric
            isStravaConnected = profile.stravaAccessToken != nil
        }
    }

    func toggleUnits() {
        useMetricUnits.toggle()
        guard let modelContext else { return }
        let descriptor = FetchDescriptor<UserProfile>()
        if let profile = try? modelContext.fetch(descriptor).first {
            profile.preferredUnits = useMetricUnits ? .metric : .imperial
            profile.updatedAt = Date()
        }
    }

    func connectStrava() {
        // Open Strava OAuth URL
        let clientId = "YOUR_STRAVA_CLIENT_ID"
        let redirectUri = "http://localhost/callback"
        let scope = "activity:read_all,activity:write"
        let urlString = "https://www.strava.com/oauth/mobile/authorize?client_id=\(clientId)&redirect_uri=\(redirectUri)&response_type=code&scope=\(scope)"
        if let url = URL(string: urlString) {
            UIApplication.shared.open(url)
        }
    }

    func disconnectStrava() {
        guard let modelContext else { return }
        let descriptor = FetchDescriptor<UserProfile>()
        if let profile = try? modelContext.fetch(descriptor).first {
            profile.stravaAccessToken = nil
            profile.stravaRefreshToken = nil
            profile.stravaTokenExpiry = nil
            profile.stravaAthleteId = nil
            profile.updatedAt = Date()
            isStravaConnected = false
            stravaAthleteName = nil
        }
    }

    func requestHealthPermissions() async {
        let service = HealthKitService()
        do {
            try await service.requestAuthorization()
            isHealthConnected = true
        } catch {
            isHealthConnected = false
        }
    }

    func exportData(format: ExportFormat) {
        guard let modelContext else { return }
        isExporting = true

        let runsDescriptor = FetchDescriptor<Run>(sortBy: [SortDescriptor(\.startTime, order: .reverse)])
        let runs = (try? modelContext.fetch(runsDescriptor)) ?? []

        let gymDescriptor = FetchDescriptor<GymWorkout>(sortBy: [SortDescriptor(\.startTime, order: .reverse)])
        let gymWorkouts = (try? modelContext.fetch(gymDescriptor)) ?? []

        let tempDir = FileManager.default.temporaryDirectory

        switch format {
        case .csv:
            let csvURL = tempDir.appendingPathComponent("gosteady_export_\(Int(Date().timeIntervalSince1970)).csv")
            var csvContent = "Type,Date,Distance (km),Duration,Pace,Calories,Heart Rate,Notes\n"

            for run in runs {
                let dateStr = ISO8601DateFormatter().string(from: run.startTime)
                csvContent += "Run,\(dateStr),\(String(format: "%.2f", run.distanceKm)),\(run.durationFormatted),\(run.avgPaceFormatted),\(run.caloriesBurned),\(run.avgHeartRate ?? 0),\"\(run.notes?.replacingOccurrences(of: "\"", with: "\"\"") ?? "")\"\n"
            }

            for workout in gymWorkouts {
                let dateStr = ISO8601DateFormatter().string(from: workout.startTime)
                csvContent += "Gym,\(dateStr),,\(workout.durationFormatted),,, \"\(workout.name.replacingOccurrences(of: "\"", with: "\"\""))\"\n"
            }

            try? csvContent.write(to: csvURL, atomically: true, encoding: .utf8)
            exportFileURL = csvURL

        case .json:
            let jsonURL = tempDir.appendingPathComponent("gosteady_export_\(Int(Date().timeIntervalSince1970)).json")

            var exportData: [[String: Any]] = []
            for run in runs {
                exportData.append([
                    "type": "run",
                    "date": ISO8601DateFormatter().string(from: run.startTime),
                    "distanceKm": run.distanceKm,
                    "durationMs": run.durationMillis,
                    "avgPaceSecPerKm": run.avgPaceSecondsPerKm,
                    "calories": run.caloriesBurned,
                    "avgHeartRate": run.avgHeartRate as Any,
                    "notes": run.notes as Any
                ])
            }
            for workout in gymWorkouts {
                exportData.append([
                    "type": "gym",
                    "date": ISO8601DateFormatter().string(from: workout.startTime),
                    "name": workout.name,
                    "exercises": workout.exercises.count,
                    "totalVolume": workout.totalVolume,
                    "totalSets": workout.totalSets
                ])
            }

            if let jsonData = try? JSONSerialization.data(withJSONObject: exportData, options: .prettyPrinted) {
                try? jsonData.write(to: jsonURL)
                exportFileURL = jsonURL
            }
        }

        isExporting = false
        showExportSheet = exportFileURL != nil
    }

    func deleteAllData() {
        guard let modelContext else { return }
        do {
            try modelContext.delete(model: Run.self)
            try modelContext.delete(model: GymWorkout.self)
            try modelContext.delete(model: TrainingPlan.self)
            try modelContext.delete(model: DailyNutrition.self)
            try modelContext.delete(model: NutritionGoals.self)
            try modelContext.delete(model: UserProfile.self)
        } catch {
            print("Delete failed: \(error)")
        }
    }
}

enum ExportFormat: String, CaseIterable {
    case csv = "CSV"
    case json = "JSON"

    var fileExtension: String {
        switch self {
        case .csv: return "csv"
        case .json: return "json"
        }
    }

    var utType: UTType {
        switch self {
        case .csv: return .commaSeparatedText
        case .json: return .json
        }
    }
}
