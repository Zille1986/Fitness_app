import SwiftUI
import SwiftData

struct OnboardingScreen: View {
    @Environment(\.modelContext) private var modelContext
    @State private var viewModel = OnboardingViewModel()
    let onComplete: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            // Progress bar
            ProgressView(value: viewModel.progressValue)
                .tint(AppTheme.primary)
                .padding(.horizontal, AppSpacing.xxl)
                .padding(.top, AppSpacing.lg)

            // Step indicator
            Text("Step \(viewModel.currentStep + 1) of \(viewModel.totalSteps)")
                .font(AppTypography.captionLarge)
                .foregroundStyle(AppTheme.onSurfaceVariant)
                .padding(.top, AppSpacing.sm)

            // Content
            TabView(selection: $viewModel.currentStep) {
                WelcomeStep()
                    .tag(0)
                PersonalInfoStep(viewModel: viewModel)
                    .tag(1)
                OnboardingGoalsStep(viewModel: viewModel)
                    .tag(2)
                PermissionsStep(viewModel: viewModel)
                    .tag(3)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .animation(.easeInOut(duration: 0.3), value: viewModel.currentStep)

            // Navigation buttons
            HStack {
                if viewModel.currentStep > 0 {
                    Button {
                        viewModel.previousStep()
                    } label: {
                        HStack(spacing: AppSpacing.sm) {
                            Image(systemName: "chevron.left")
                            Text("Back")
                        }
                        .font(AppTypography.labelLarge)
                        .foregroundStyle(AppTheme.onSurfaceVariant)
                        .padding(.horizontal, AppSpacing.xl)
                        .padding(.vertical, AppSpacing.md)
                    }
                } else {
                    Spacer()
                }

                Spacer()

                Button {
                    if viewModel.currentStep == viewModel.totalSteps - 1 {
                        viewModel.completeOnboarding(modelContext: modelContext)
                        onComplete()
                    } else {
                        viewModel.nextStep()
                    }
                } label: {
                    HStack(spacing: AppSpacing.sm) {
                        Text(viewModel.currentStep == viewModel.totalSteps - 1 ? "Get Started" : "Next")
                        Image(systemName: viewModel.currentStep == viewModel.totalSteps - 1 ? "checkmark" : "chevron.right")
                    }
                    .font(AppTypography.labelLarge)
                    .foregroundStyle(.white)
                    .padding(.horizontal, AppSpacing.xxl)
                    .padding(.vertical, AppSpacing.md)
                    .background(viewModel.canProceed ? AppTheme.primary : AppTheme.primary.opacity(0.4))
                    .clipShape(Capsule())
                }
                .disabled(!viewModel.canProceed)
            }
            .padding(.horizontal, AppSpacing.xxl)
            .padding(.bottom, AppSpacing.xxl)
        }
        .background(AppTheme.background)
    }
}

// MARK: - Step 1: Welcome

private struct WelcomeStep: View {
    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            // Icon
            ZStack {
                Circle()
                    .fill(AppTheme.primary.opacity(0.15))
                    .frame(width: 120, height: 120)
                Image(systemName: "figure.run")
                    .font(.system(size: 52))
                    .foregroundStyle(AppTheme.primary)
            }
            .padding(.bottom, 24)

            // Text
            Text("Welcome to GoSteady")
                .font(.system(size: 30, weight: .bold))
                .foregroundStyle(AppTheme.onSurface)
                .multilineTextAlignment(.center)
                .minimumScaleFactor(0.7)
                .padding(.horizontal, 24)
                .padding(.bottom, 12)

            Text("Your all-in-one fitness companion for running, gym, swimming, cycling, and more.")
                .font(.body)
                .foregroundStyle(AppTheme.onSurfaceVariant)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
                .padding(.bottom, 32)

            // Feature icons
            HStack(spacing: 20) {
                OnboardingFeatureIcon(icon: AppTheme.SportIcon.running, label: "Run", color: AppTheme.running)
                OnboardingFeatureIcon(icon: AppTheme.SportIcon.gym, label: "Gym", color: AppTheme.gym)
                OnboardingFeatureIcon(icon: AppTheme.SportIcon.swimming, label: "Swim", color: AppTheme.swimming)
                OnboardingFeatureIcon(icon: AppTheme.SportIcon.cycling, label: "Cycle", color: AppTheme.cycling)
            }
            .padding(.horizontal, 24)

            Spacer()
            Spacer()
        }
        .padding(.horizontal, 16)
    }
}

// MARK: - Step 2: Personal Info

private struct PersonalInfoStep: View {
    @Bindable var viewModel: OnboardingViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: AppSpacing.xxl) {
                VStack(alignment: .leading, spacing: AppSpacing.sm) {
                    Text("About You")
                        .font(AppTypography.headlineMedium)
                        .foregroundStyle(AppTheme.onSurface)
                    Text("Let's personalize your experience")
                        .font(AppTypography.bodyMedium)
                        .foregroundStyle(AppTheme.onSurfaceVariant)
                }

                VStack(spacing: AppSpacing.lg) {
                    OnboardingTextField(
                        icon: "person.fill",
                        label: "Your Name",
                        text: $viewModel.name,
                        placeholder: "Enter your name"
                    )

                    OnboardingTextField(
                        icon: "calendar",
                        label: "Age",
                        text: $viewModel.age,
                        placeholder: "e.g. 28",
                        keyboardType: .numberPad
                    )

                    HStack(spacing: AppSpacing.md) {
                        OnboardingTextField(
                            icon: "scalemass.fill",
                            label: "Weight (kg)",
                            text: $viewModel.weight,
                            placeholder: "e.g. 75",
                            keyboardType: .decimalPad
                        )
                        OnboardingTextField(
                            icon: "ruler",
                            label: "Height (cm)",
                            text: $viewModel.height,
                            placeholder: "e.g. 175",
                            keyboardType: .decimalPad
                        )
                    }

                    // Gender
                    VStack(alignment: .leading, spacing: AppSpacing.sm) {
                        Text("Gender")
                            .font(AppTypography.labelMedium)
                            .foregroundStyle(AppTheme.onSurfaceVariant)
                        HStack(spacing: AppSpacing.md) {
                            ForEach(Gender.allCases, id: \.self) { g in
                                Button {
                                    viewModel.gender = g
                                } label: {
                                    Text(g.rawValue.capitalized)
                                        .font(AppTypography.labelMedium)
                                        .foregroundStyle(viewModel.gender == g ? .white : AppTheme.onSurface)
                                        .frame(maxWidth: .infinity)
                                        .padding(.vertical, AppSpacing.md)
                                        .background(viewModel.gender == g ? AppTheme.primary : AppTheme.surfaceVariant)
                                        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.medium))
                                }
                            }
                        }
                    }
                }

                // Privacy note
                HStack(spacing: AppSpacing.md) {
                    Image(systemName: "lock.fill")
                        .foregroundStyle(AppTheme.tertiary)
                    Text("Your data stays on your device and is never shared")
                        .font(AppTypography.captionLarge)
                        .foregroundStyle(AppTheme.onSurfaceVariant)
                }
                .padding(AppSpacing.lg)
                .background(AppTheme.tertiary.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.medium))
            }
            .padding(AppSpacing.xxl)
        }
    }
}

// MARK: - Step 3: Fitness Goals

private struct OnboardingGoalsStep: View {
    @Bindable var viewModel: OnboardingViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: AppSpacing.xxl) {
                VStack(alignment: .leading, spacing: AppSpacing.sm) {
                    Text("Your Goals")
                        .font(AppTypography.headlineMedium)
                        .foregroundStyle(AppTheme.onSurface)
                    Text("Select what you want to focus on")
                        .font(AppTypography.bodyMedium)
                        .foregroundStyle(AppTheme.onSurfaceVariant)
                }

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: AppSpacing.md) {
                    ForEach(OnboardingGoal.allCases) { goal in
                        GoalSelectionCard(
                            goal: goal,
                            isSelected: viewModel.fitnessGoals.contains(goal)
                        ) {
                            viewModel.toggleGoal(goal)
                        }
                    }
                }

                // Weekly distance goal
                VStack(alignment: .leading, spacing: AppSpacing.md) {
                    Text("Weekly Running Goal")
                        .font(AppTypography.labelLarge)
                        .foregroundStyle(AppTheme.onSurface)

                    HStack(spacing: AppSpacing.md) {
                        ForEach(["10", "20", "30", "50"], id: \.self) { preset in
                            Button {
                                viewModel.weeklyRunGoalKm = preset
                            } label: {
                                Text("\(preset)km")
                                    .font(AppTypography.labelMedium)
                                    .foregroundStyle(viewModel.weeklyRunGoalKm == preset ? .white : AppTheme.onSurface)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, AppSpacing.sm)
                                    .background(viewModel.weeklyRunGoalKm == preset ? AppTheme.primary : AppTheme.surfaceVariant)
                                    .clipShape(Capsule())
                            }
                        }
                    }
                }
            }
            .padding(AppSpacing.xxl)
        }
    }
}

// MARK: - Step 4: Permissions

private struct PermissionsStep: View {
    @Bindable var viewModel: OnboardingViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: AppSpacing.xxl) {
                VStack(alignment: .leading, spacing: AppSpacing.sm) {
                    Text("Permissions")
                        .font(AppTypography.headlineMedium)
                        .foregroundStyle(AppTheme.onSurface)
                    Text("These help GoSteady work best for you")
                        .font(AppTypography.bodyMedium)
                        .foregroundStyle(AppTheme.onSurfaceVariant)
                }

                VStack(spacing: AppSpacing.md) {
                    PermissionRow(
                        icon: "location.fill",
                        iconColor: AppTheme.accent,
                        title: "Location",
                        description: "Track your running routes with GPS",
                        isGranted: viewModel.locationPermissionGranted
                    ) {
                        viewModel.requestLocationPermission()
                    }

                    PermissionRow(
                        icon: "heart.fill",
                        iconColor: .red,
                        title: "Apple Health",
                        description: "Sync workouts, heart rate, and steps",
                        isGranted: viewModel.healthPermissionGranted
                    ) {
                        Task { await viewModel.requestHealthPermission() }
                    }

                    PermissionRow(
                        icon: "bell.fill",
                        iconColor: AppTheme.secondary,
                        title: "Notifications",
                        description: "Workout reminders and achievements",
                        isGranted: viewModel.notificationPermissionGranted
                    ) {
                        Task { await viewModel.requestNotificationPermission() }
                    }
                }

                // All set card
                VStack(spacing: AppSpacing.md) {
                    Image(systemName: "checkmark.seal.fill")
                        .font(.system(size: 48))
                        .foregroundStyle(AppTheme.primary)

                    Text("You're all set!")
                        .font(AppTypography.titleLarge)
                        .foregroundStyle(AppTheme.onSurface)

                    Text("Tap 'Get Started' to begin your fitness journey. You can always adjust these settings later.")
                        .font(AppTypography.bodyMedium)
                        .foregroundStyle(AppTheme.onSurfaceVariant)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(AppSpacing.xxl)
                .background(AppTheme.primary.opacity(0.08))
                .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.large))
            }
            .padding(AppSpacing.xxl)
        }
    }
}

// MARK: - Supporting Components

private struct OnboardingFeatureIcon: View {
    let icon: String
    let label: String
    let color: Color

    var body: some View {
        VStack(spacing: AppSpacing.sm) {
            ZStack {
                Circle()
                    .fill(color.opacity(0.15))
                    .frame(width: 56, height: 56)
                Image(systemName: icon)
                    .font(.title3)
                    .foregroundStyle(color)
            }
            Text(label)
                .font(AppTypography.captionLarge)
                .foregroundStyle(AppTheme.onSurfaceVariant)
        }
    }
}

private struct OnboardingTextField: View {
    let icon: String
    let label: String
    @Binding var text: String
    var placeholder: String = ""
    var keyboardType: UIKeyboardType = .default

    var body: some View {
        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            Text(label)
                .font(AppTypography.labelMedium)
                .foregroundStyle(AppTheme.onSurfaceVariant)

            HStack(spacing: AppSpacing.md) {
                Image(systemName: icon)
                    .foregroundStyle(AppTheme.primary)
                    .frame(width: 24)
                TextField(placeholder, text: $text)
                    .keyboardType(keyboardType)
                    .font(AppTypography.bodyMedium)
            }
            .padding(AppSpacing.md)
            .background(AppTheme.surfaceVariant)
            .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.medium))
        }
    }
}

private struct GoalSelectionCard: View {
    let goal: OnboardingGoal
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: AppSpacing.sm) {
                ZStack {
                    Circle()
                        .fill(isSelected ? goal.color : goal.color.opacity(0.15))
                        .frame(width: 48, height: 48)
                    Image(systemName: goal.icon)
                        .font(.title3)
                        .foregroundStyle(isSelected ? .white : goal.color)
                }

                Text(goal.rawValue)
                    .font(AppTypography.labelSmall)
                    .foregroundStyle(isSelected ? AppTheme.onSurface : AppTheme.onSurfaceVariant)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, AppSpacing.lg)
            .background(isSelected ? goal.color.opacity(0.15) : AppTheme.surfaceVariant)
            .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.medium))
            .overlay(
                RoundedRectangle(cornerRadius: AppCornerRadius.medium)
                    .stroke(isSelected ? goal.color : .clear, lineWidth: 2)
            )
        }
    }
}

private struct PermissionRow: View {
    let icon: String
    let iconColor: Color
    let title: String
    let description: String
    let isGranted: Bool
    let onRequest: () -> Void

    var body: some View {
        HStack(spacing: AppSpacing.md) {
            ZStack {
                Circle()
                    .fill(iconColor.opacity(0.15))
                    .frame(width: 44, height: 44)
                Image(systemName: icon)
                    .foregroundStyle(iconColor)
            }

            VStack(alignment: .leading, spacing: AppSpacing.xxs) {
                Text(title)
                    .font(AppTypography.titleSmall)
                    .foregroundStyle(AppTheme.onSurface)
                Text(description)
                    .font(AppTypography.captionLarge)
                    .foregroundStyle(AppTheme.onSurfaceVariant)
            }

            Spacer()

            if isGranted {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(AppTheme.success)
            } else {
                Button("Allow") {
                    onRequest()
                }
                .font(AppTypography.labelMedium)
                .foregroundStyle(.white)
                .padding(.horizontal, AppSpacing.lg)
                .padding(.vertical, AppSpacing.sm)
                .background(AppTheme.primary)
                .clipShape(Capsule())
            }
        }
        .padding(AppSpacing.lg)
        .background(AppTheme.surfaceVariant.opacity(0.5))
        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.medium))
    }
}
