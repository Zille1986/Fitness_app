import SwiftUI
import SwiftData

struct ContentView: View {
    @Environment(\.modelContext) private var modelContext
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding = false
    @State private var showOnboarding = false
    @State private var selectedTab: AppTab = .home
    @State private var showAICoach = false

    var body: some View {
        ZStack {
            if showOnboarding {
                OnboardingScreen {
                    hasCompletedOnboarding = true
                    withAnimation(.easeInOut(duration: 0.5)) {
                        showOnboarding = false
                    }
                }
                .transition(.opacity)
            } else {
                mainTabView
            }
        }
        .onAppear {
            checkOnboardingState()
        }
        .sheet(isPresented: $showAICoach) {
            AICoachScreen()
        }
    }

    // MARK: - Main Tab View

    private var mainTabView: some View {
        ZStack(alignment: .bottomTrailing) {
            TabView(selection: $selectedTab) {
                HomeScreen()
                    .tabItem {
                        Label(AppTab.home.rawValue, systemImage: AppTab.home.icon)
                    }
                    .tag(AppTab.home)

                ActivityHubScreen()
                    .tabItem {
                        Label(AppTab.activity.rawValue, systemImage: AppTab.activity.icon)
                    }
                    .tag(AppTab.activity)

                TrainingTabScreen()
                    .tabItem {
                        Label(AppTab.training.rawValue, systemImage: AppTab.training.icon)
                    }
                    .tag(AppTab.training)

                NutritionTabScreen()
                    .tabItem {
                        Label(AppTab.nutrition.rawValue, systemImage: AppTab.nutrition.icon)
                    }
                    .tag(AppTab.nutrition)

                ProfileTabScreen()
                    .tabItem {
                        Label(AppTab.profile.rawValue, systemImage: AppTab.profile.icon)
                    }
                    .tag(AppTab.profile)
            }
            .tint(AppTheme.primary)

            // Floating AI Coach button
            aiCoachButton
        }
    }

    // MARK: - AI Coach Button

    private var aiCoachButton: some View {
        Button {
            showAICoach = true
        } label: {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [AppTheme.primary, AppTheme.primary.opacity(0.8)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 52, height: 52)
                    .shadow(color: AppTheme.primary.opacity(0.4), radius: 8, x: 0, y: 4)

                Image(systemName: AppTheme.SportIcon.coach)
                    .font(.title3)
                    .foregroundStyle(.white)
            }
        }
        .padding(.trailing, AppSpacing.lg)
        .padding(.bottom, 80) // Above tab bar
    }

    // MARK: - Onboarding Check

    private func checkOnboardingState() {
        // Check AppStorage first
        if hasCompletedOnboarding {
            showOnboarding = false
            return
        }

        // Check SwiftData for existing profile
        let descriptor = FetchDescriptor<UserProfile>()
        if let profile = try? modelContext.fetch(descriptor).first,
           profile.isOnboardingComplete {
            hasCompletedOnboarding = true
            showOnboarding = false
        } else {
            showOnboarding = true
        }
    }
}

// MARK: - Training Tab

struct TrainingTabScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @Query(
        filter: #Predicate<TrainingPlan> { $0.isActive },
        sort: \TrainingPlan.createdAt,
        order: .reverse
    ) private var activePlans: [TrainingPlan]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: AppSpacing.xl) {
                    if activePlans.isEmpty {
                        EmptyStateView(
                            icon: "calendar.badge.plus",
                            title: "No Active Plans",
                            message: "Create a training plan to stay on track with your goals",
                            actionTitle: "Create Plan"
                        ) {
                            // Navigate to plan builder
                        }
                        .padding(.horizontal)
                        .padding(.top, AppSpacing.xxxxl)
                    } else {
                        ForEach(activePlans) { plan in
                            TrainingPlanCard(plan: plan)
                        }
                    }
                }
                .padding(.vertical)
            }
            .background(AppTheme.adaptiveBackground(colorScheme))
            .navigationTitle("Training")
        }
    }
}

private struct TrainingPlanCard: View {
    let plan: TrainingPlan
    @Environment(\.colorScheme) private var colorScheme

    private var completedCount: Int {
        plan.weeklySchedule.filter(\.isCompleted).count
    }

    private var totalCount: Int {
        plan.weeklySchedule.count
    }

    private var progress: Float {
        totalCount > 0 ? Float(completedCount) / Float(totalCount) : 0
    }

    var body: some View {
        VStack(alignment: .leading, spacing: AppSpacing.md) {
            HStack {
                VStack(alignment: .leading, spacing: AppSpacing.xs) {
                    Text(plan.name)
                        .font(AppTypography.titleMedium)
                        .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
                    Text(plan.planDescription)
                        .font(AppTypography.captionLarge)
                        .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                        .lineLimit(2)
                }
                Spacer()
                Text("\(Int(progress * 100))%")
                    .font(AppTypography.statSmall)
                    .foregroundStyle(AppTheme.primary)
            }

            ProgressView(value: progress)
                .tint(AppTheme.primary)

            HStack {
                Label("\(completedCount)/\(totalCount) workouts", systemImage: "checkmark.circle")
                    .font(AppTypography.captionLarge)
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                Spacer()
                Text(plan.endDate, style: .date)
                    .font(AppTypography.captionLarge)
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
            }
        }
        .padding(AppSpacing.lg)
        .background(AppTheme.adaptiveSurface(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.large))
        .padding(.horizontal)
    }
}

// MARK: - Nutrition Tab

struct NutritionTabScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @Query(sort: \DailyNutrition.date, order: .reverse) private var nutritionDays: [DailyNutrition]

    private var today: DailyNutrition? {
        let calendar = Calendar.current
        return nutritionDays.first { calendar.isDateInToday($0.date) }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: AppSpacing.xl) {
                    // Today's macros
                    if let today {
                        todayNutritionCard(today)
                    } else {
                        EmptyStateView(
                            icon: "fork.knife",
                            title: "No meals logged today",
                            message: "Log your meals to track your nutrition",
                            actionTitle: "Log Meal"
                        ) {
                            // Navigate to food logger
                        }
                        .padding(.horizontal)
                        .padding(.top, AppSpacing.xxxxl)
                    }
                }
                .padding(.vertical)
            }
            .background(AppTheme.adaptiveBackground(colorScheme))
            .navigationTitle("Nutrition")
        }
    }

    private func todayNutritionCard(_ nutrition: DailyNutrition) -> some View {
        VStack(spacing: AppSpacing.lg) {
            // Calorie ring
            GoSteadyDonutChart(
                segments: [
                    DonutSegment(label: "Consumed", value: Double(nutrition.consumedCalories), color: AppTheme.primary),
                    DonutSegment(label: "Remaining", value: Double(max(0, nutrition.remainingCalories)), color: AppTheme.surfaceVariant)
                ],
                centerText: "\(nutrition.consumedCalories)",
                centerSubtext: "of \(nutrition.targetCalories) cal"
            )

            // Macro breakdown
            HStack(spacing: AppSpacing.lg) {
                MacroBar(label: "Protein", current: nutrition.consumedProteinGrams, target: nutrition.targetProteinGrams, color: AppTheme.accent)
                MacroBar(label: "Carbs", current: nutrition.consumedCarbsGrams, target: nutrition.targetCarbsGrams, color: AppTheme.secondary)
                MacroBar(label: "Fat", current: nutrition.consumedFatGrams, target: nutrition.targetFatGrams, color: AppTheme.tertiary)
            }

            // Water
            HStack {
                Image(systemName: AppTheme.SportIcon.water)
                    .foregroundStyle(AppTheme.accent)
                Text("\(nutrition.waterMl) / \(nutrition.targetWaterMl) ml")
                    .font(AppTypography.labelMedium)
                Spacer()
                ProgressView(value: Float(nutrition.waterMl), total: Float(nutrition.targetWaterMl))
                    .tint(AppTheme.accent)
                    .frame(width: 100)
            }
        }
        .padding(AppSpacing.xl)
        .background(AppTheme.adaptiveSurface(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.large))
        .padding(.horizontal)
    }
}

private struct MacroBar: View {
    let label: String
    let current: Int
    let target: Int
    let color: Color
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(spacing: AppSpacing.xs) {
            Text("\(current)g")
                .font(AppTypography.titleSmall)
                .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
            ProgressView(value: Float(current), total: Float(max(target, 1)))
                .tint(color)
            Text(label)
                .font(AppTypography.captionSmall)
                .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
        }
    }
}

// MARK: - Profile Tab

struct ProfileTabScreen: View {
    var body: some View {
        ProfileScreen()
    }
}
