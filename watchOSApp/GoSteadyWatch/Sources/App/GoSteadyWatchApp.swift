import SwiftUI
import HealthKit
import WatchKit

@main
struct GoSteadyWatchApp: App {

    @State private var workoutService = WatchWorkoutService()
    @State private var healthService = WatchHealthService()
    @State private var locationService = WatchLocationService()
    @State private var syncService = PhoneSyncService.shared
    @State private var homeVM = WatchHomeViewModel()
    @State private var workoutVM = WatchWorkoutViewModel()
    @State private var gymVM = WatchGymViewModel()
    @State private var settingsVM = WatchSettingsViewModel()

    var body: some Scene {
        WindowGroup {
            WatchContentView()
                .environment(workoutService)
                .environment(healthService)
                .environment(locationService)
                .environment(syncService)
                .environment(homeVM)
                .environment(workoutVM)
                .environment(gymVM)
                .environment(settingsVM)
                .onAppear {
                    healthService.requestAuthorization()
                    syncService.activate()
                    workoutService.configure(
                        healthService: healthService,
                        locationService: locationService,
                        syncService: syncService,
                        workoutVM: workoutVM
                    )
                    homeVM.configure(healthService: healthService)
                }
        }
    }
}
