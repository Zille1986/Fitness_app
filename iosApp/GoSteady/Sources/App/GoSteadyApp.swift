import SwiftUI
import SwiftData

@main
struct GoSteadyApp: App {
    @AppStorage("isDarkMode") private var isDarkMode = true
    @State private var router = AppRouter()

    var sharedModelContainer: ModelContainer = {
        let schema = Schema([
            UserProfile.self,
            Run.self,
            GymWorkout.self,
            TrainingPlan.self,
            DailyNutrition.self,
            NutritionGoals.self
        ])
        let modelConfiguration = ModelConfiguration(
            schema: schema,
            isStoredInMemoryOnly: false,
            allowsSave: true
        )
        do {
            return try ModelContainer(for: schema, configurations: [modelConfiguration])
        } catch {
            fatalError("Could not create ModelContainer: \(error)")
        }
    }()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(router)
                .preferredColorScheme(isDarkMode ? .dark : .light)
                .onOpenURL { url in
                    handleDeepLink(url)
                }
        }
        .modelContainer(sharedModelContainer)
    }

    private func handleDeepLink(_ url: URL) {
        if let deepLink = DeepLink.parse(url: url) {
            router.handleDeepLink(deepLink)
        }
    }
}
