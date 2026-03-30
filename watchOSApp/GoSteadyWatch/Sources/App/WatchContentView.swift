import SwiftUI
import WatchKit

struct WatchContentView: View {

    @Environment(WatchWorkoutService.self) private var workoutService
    @Environment(WatchWorkoutViewModel.self) private var workoutVM

    var body: some View {
        NavigationStack {
            if workoutService.isActive {
                activeWorkoutDestination
            } else if workoutVM.showSummary {
                WorkoutSummaryScreen()
            } else {
                WatchHomeScreen()
            }
        }
        .tint(WatchTheme.primary)
    }

    @ViewBuilder
    private var activeWorkoutDestination: some View {
        switch workoutService.currentActivity {
        case .outdoorRun, .indoorRun:
            ActiveRunScreen()
        case .gym:
            ActiveGymScreen()
        case .poolSwim, .openWaterSwim:
            ActiveSwimScreen()
        case .outdoorCycle, .indoorCycle:
            ActiveCycleScreen()
        case .hiit:
            ActiveHIITScreen()
        }
    }
}
