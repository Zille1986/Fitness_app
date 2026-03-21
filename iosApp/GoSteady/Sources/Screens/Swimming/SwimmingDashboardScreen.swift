import SwiftUI

struct SwimmingDashboardScreen: View {
    @State private var showManualEntry = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    DashboardHeroBanner(
                        title: "Swimming",
                        subtitle: "Pool and open water sessions",
                        color: AppTheme.swimming
                    )

                    EmptyStateView(
                        icon: "figure.pool.swim",
                        title: "No swims yet",
                        subtitle: "Start a swim from your Apple Watch or log one manually"
                    )
                    .padding(.horizontal)
                }
                .padding(.vertical)
            }
            .background(AppTheme.surface)
            .navigationTitle("Swimming")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showManualEntry = true } label: {
                        Image(systemName: "square.and.pencil")
                    }
                }
            }
            .sheet(isPresented: $showManualEntry) {
                ManualWorkoutSheet(sportType: .swim)
            }
        }
    }
}
