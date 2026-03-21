import SwiftUI

struct CyclingDashboardScreen: View {
    @State private var showManualEntry = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    DashboardHeroBanner(
                        title: "Cycling",
                        subtitle: "Outdoor rides and indoor training",
                        color: AppTheme.cycling
                    )

                    EmptyStateView(
                        icon: "bicycle",
                        title: "No rides yet",
                        subtitle: "Start a ride from your Apple Watch or log one manually"
                    )
                    .padding(.horizontal)
                }
                .padding(.vertical)
            }
            .background(AppTheme.surface)
            .navigationTitle("Cycling")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { showManualEntry = true } label: {
                        Image(systemName: "square.and.pencil")
                    }
                }
            }
            .sheet(isPresented: $showManualEntry) {
                ManualWorkoutSheet(sportType: .bike)
            }
        }
    }
}
