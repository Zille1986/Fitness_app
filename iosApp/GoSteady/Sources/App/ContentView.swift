import SwiftUI

struct ContentView: View {
    @State private var selectedTab: Tab = .home

    enum Tab: String, CaseIterable {
        case home = "Home"
        case running = "Run"
        case gym = "Gym"
        case swimming = "Swim"
        case cycling = "Bike"
        case settings = "Settings"

        var icon: String {
            switch self {
            case .home: return "house.fill"
            case .running: return "figure.run"
            case .gym: return "dumbbell.fill"
            case .swimming: return "figure.pool.swim"
            case .cycling: return "bicycle"
            case .settings: return "gearshape"
            }
        }
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            HomeScreen()
                .tabItem {
                    Label(Tab.home.rawValue, systemImage: Tab.home.icon)
                }
                .tag(Tab.home)

            RunningDashboardScreen()
                .tabItem {
                    Label(Tab.running.rawValue, systemImage: Tab.running.icon)
                }
                .tag(Tab.running)

            GymDashboardScreen()
                .tabItem {
                    Label(Tab.gym.rawValue, systemImage: Tab.gym.icon)
                }
                .tag(Tab.gym)

            SwimmingDashboardScreen()
                .tabItem {
                    Label(Tab.swimming.rawValue, systemImage: Tab.swimming.icon)
                }
                .tag(Tab.swimming)

            CyclingDashboardScreen()
                .tabItem {
                    Label(Tab.cycling.rawValue, systemImage: Tab.cycling.icon)
                }
                .tag(Tab.cycling)

            SettingsScreen()
                .tabItem {
                    Label(Tab.settings.rawValue, systemImage: Tab.settings.icon)
                }
                .tag(Tab.settings)
        }
        .tint(AppTheme.primary)
    }
}
