import SwiftUI

@main
struct GoSteadyWatchApp: App {
    @StateObject private var workoutManager = WorkoutManager()
    @StateObject private var syncManager = PhoneSyncManager.shared

    var body: some Scene {
        WindowGroup {
            NavigationStack {
                if workoutManager.state.isTracking {
                    TrackingView()
                        .environmentObject(workoutManager)
                } else {
                    ActivitySelectView()
                        .environmentObject(workoutManager)
                }
            }
        }
        .onChange(of: workoutManager.isAuthorized) { _ in }
    }
}
