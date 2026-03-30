import SwiftUI
import SwiftData

struct ProfileScreen: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.colorScheme) private var colorScheme
    @State private var viewModel = ProfileViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: AppSpacing.xl) {
                    // Avatar & Name
                    profileHeader

                    // Body Stats
                    bodyStatsSection

                    // Fitness Info
                    fitnessInfoSection

                    // Heart Rate
                    heartRateSection

                    // Goals
                    goalsSection

                    // Quick Links
                    quickLinksSection

                    Spacer(minLength: 40)
                }
                .padding(.vertical)
            }
            .background(AppTheme.adaptiveBackground(colorScheme))
            .navigationTitle("Profile")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(viewModel.isEditing ? "Save" : "Edit") {
                        viewModel.toggleEditing()
                    }
                    .foregroundStyle(AppTheme.primary)
                }
            }
            .overlay {
                if viewModel.isSaved {
                    VStack {
                        Spacer()
                        HStack(spacing: AppSpacing.sm) {
                            Image(systemName: "checkmark.circle.fill")
                            Text("Profile saved")
                        }
                        .font(AppTypography.labelMedium)
                        .foregroundStyle(.white)
                        .padding(.horizontal, AppSpacing.xl)
                        .padding(.vertical, AppSpacing.md)
                        .background(AppTheme.success)
                        .clipShape(Capsule())
                        .padding(.bottom, 30)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                    }
                    .animation(.spring(duration: 0.4), value: viewModel.isSaved)
                }
            }
        }
        .onAppear {
            viewModel.load(modelContext: modelContext)
        }
    }

    // MARK: - Profile Header

    private var profileHeader: some View {
        VStack(spacing: AppSpacing.md) {
            // Avatar
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [AppTheme.primary, AppTheme.primary.opacity(0.6)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 100, height: 100)

                Text(viewModel.name.prefix(1).uppercased())
                    .font(.system(size: 42, weight: .bold, design: .rounded))
                    .foregroundStyle(.white)
            }

            if viewModel.isEditing {
                TextField("Your name", text: $viewModel.name)
                    .font(AppTypography.headlineSmall)
                    .multilineTextAlignment(.center)
                    .textFieldStyle(.roundedBorder)
                    .frame(maxWidth: 200)
            } else {
                Text(viewModel.name.isEmpty ? "Athlete" : viewModel.name)
                    .font(AppTypography.headlineSmall)
                    .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
            }

            // BMI badge
            if let bmi = viewModel.bmi {
                HStack(spacing: AppSpacing.xs) {
                    Circle()
                        .fill(viewModel.bmiColor)
                        .frame(width: 8, height: 8)
                    Text("BMI \(String(format: "%.1f", bmi)) - \(viewModel.bmiCategory)")
                        .font(AppTypography.captionLarge)
                        .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                }
            }
        }
        .padding(.horizontal)
    }

    // MARK: - Body Stats

    private var bodyStatsSection: some View {
        VStack(alignment: .leading, spacing: AppSpacing.md) {
            Text("BODY STATS")
                .sectionHeaderStyle()

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: AppSpacing.md) {
                ProfileStatField(
                    icon: "calendar",
                    label: "Age",
                    value: $viewModel.age,
                    unit: "years",
                    isEditing: viewModel.isEditing,
                    keyboardType: .numberPad
                )
                ProfileStatField(
                    icon: "scalemass.fill",
                    label: "Weight",
                    value: $viewModel.weight,
                    unit: viewModel.preferredUnits == .metric ? "kg" : "lbs",
                    isEditing: viewModel.isEditing,
                    keyboardType: .decimalPad
                )
                ProfileStatField(
                    icon: "ruler",
                    label: "Height",
                    value: $viewModel.height,
                    unit: viewModel.preferredUnits == .metric ? "cm" : "in",
                    isEditing: viewModel.isEditing,
                    keyboardType: .decimalPad
                )
                ProfileGenderField(
                    gender: $viewModel.gender,
                    isEditing: viewModel.isEditing
                )
            }
        }
        .padding(AppSpacing.lg)
        .background(AppTheme.adaptiveSurface(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.large))
        .padding(.horizontal)
    }

    // MARK: - Fitness Info

    private var fitnessInfoSection: some View {
        VStack(alignment: .leading, spacing: AppSpacing.md) {
            Text("FITNESS LEVEL")
                .sectionHeaderStyle()

            if viewModel.isEditing {
                FlowLayout(spacing: AppSpacing.sm) {
                    ForEach(FitnessLevel.allCases, id: \.self) { level in
                        Button {
                            viewModel.setFitnessLevel(level)
                        } label: {
                            Text(level.displayName)
                                .font(AppTypography.labelMedium)
                                .foregroundStyle(viewModel.fitnessLevel == level ? .white : AppTheme.adaptiveOnSurface(colorScheme))
                                .padding(.horizontal, AppSpacing.lg)
                                .padding(.vertical, AppSpacing.sm)
                                .background(viewModel.fitnessLevel == level ? AppTheme.primary : AppTheme.adaptiveSurfaceVariant(colorScheme))
                                .clipShape(Capsule())
                        }
                    }
                }
            } else {
                HStack {
                    Text(viewModel.fitnessLevel.displayName)
                        .font(AppTypography.titleMedium)
                        .foregroundStyle(AppTheme.primary)
                    Spacer()
                }
            }
        }
        .padding(AppSpacing.lg)
        .background(AppTheme.adaptiveSurface(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.large))
        .padding(.horizontal)
    }

    // MARK: - Heart Rate

    private var heartRateSection: some View {
        VStack(alignment: .leading, spacing: AppSpacing.md) {
            Text("HEART RATE")
                .sectionHeaderStyle()

            HStack(spacing: AppSpacing.md) {
                ProfileStatField(
                    icon: "heart.fill",
                    label: "Resting HR",
                    value: $viewModel.restingHeartRate,
                    unit: "bpm",
                    isEditing: viewModel.isEditing,
                    keyboardType: .numberPad
                )
                ProfileStatField(
                    icon: "heart.circle.fill",
                    label: "Max HR",
                    value: $viewModel.maxHeartRate,
                    unit: "bpm",
                    isEditing: viewModel.isEditing,
                    keyboardType: .numberPad
                )
            }

            if !viewModel.isEditing {
                Text("Estimated Max HR: \(viewModel.estimatedMaxHR) bpm")
                    .font(AppTypography.captionLarge)
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
            }
        }
        .padding(AppSpacing.lg)
        .background(AppTheme.adaptiveSurface(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.large))
        .padding(.horizontal)
    }

    // MARK: - Goals

    private var goalsSection: some View {
        VStack(alignment: .leading, spacing: AppSpacing.md) {
            Text("WEEKLY GOAL")
                .sectionHeaderStyle()

            if viewModel.isEditing {
                HStack {
                    TextField("20", text: $viewModel.weeklyGoalKm)
                        .keyboardType(.decimalPad)
                        .font(AppTypography.statSmall)
                        .frame(width: 80)
                        .multilineTextAlignment(.center)
                        .padding(AppSpacing.sm)
                        .background(AppTheme.adaptiveSurfaceVariant(colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.small))
                    Text("km / week")
                        .font(AppTypography.bodyMedium)
                        .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                }

                // Preset buttons
                HStack(spacing: AppSpacing.sm) {
                    ForEach(["10", "20", "30", "50"], id: \.self) { preset in
                        Button {
                            viewModel.weeklyGoalKm = preset
                        } label: {
                            Text("\(preset)km")
                                .font(AppTypography.labelSmall)
                                .foregroundStyle(viewModel.weeklyGoalKm == preset ? .white : AppTheme.adaptiveOnSurface(colorScheme))
                                .padding(.horizontal, AppSpacing.md)
                                .padding(.vertical, AppSpacing.xs)
                                .background(viewModel.weeklyGoalKm == preset ? AppTheme.primary : AppTheme.adaptiveSurfaceVariant(colorScheme))
                                .clipShape(Capsule())
                        }
                    }
                }
            } else {
                HStack {
                    Text("\(viewModel.weeklyGoalKm) km")
                        .font(AppTypography.statSmall)
                        .foregroundStyle(AppTheme.primary)
                    Text("per week")
                        .font(AppTypography.bodyMedium)
                        .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                }
            }
        }
        .padding(AppSpacing.lg)
        .background(AppTheme.adaptiveSurface(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.large))
        .padding(.horizontal)
    }

    // MARK: - Quick Links

    private var quickLinksSection: some View {
        VStack(spacing: AppSpacing.sm) {
            NavigationLink(value: AppRoute.achievements) {
                ProfileLinkRow(icon: "trophy.fill", title: "Achievements", color: AppTheme.warning)
            }
            NavigationLink(value: AppRoute.analytics) {
                ProfileLinkRow(icon: "chart.bar.xaxis", title: "Analytics", color: AppTheme.accent)
            }
            NavigationLink(value: AppRoute.settings) {
                ProfileLinkRow(icon: "gearshape.fill", title: "Settings", color: AppTheme.onSurfaceVariant)
            }
        }
        .padding(.horizontal)
    }
}

// MARK: - Supporting Views

struct ProfileStatField: View {
    let icon: String
    let label: String
    @Binding var value: String
    let unit: String
    let isEditing: Bool
    var keyboardType: UIKeyboardType = .default
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            HStack(spacing: AppSpacing.xs) {
                Image(systemName: icon)
                    .font(.caption)
                    .foregroundStyle(AppTheme.primary)
                Text(label)
                    .font(AppTypography.captionLarge)
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
            }

            if isEditing {
                HStack(spacing: AppSpacing.xs) {
                    TextField("--", text: $value)
                        .keyboardType(keyboardType)
                        .font(AppTypography.titleMedium)
                        .padding(AppSpacing.sm)
                        .background(AppTheme.adaptiveSurfaceVariant(colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.small))
                    Text(unit)
                        .font(AppTypography.captionLarge)
                        .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                }
            } else {
                HStack(alignment: .lastTextBaseline, spacing: AppSpacing.xxs) {
                    Text(value.isEmpty ? "--" : value)
                        .font(AppTypography.titleMedium)
                        .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
                    Text(unit)
                        .font(AppTypography.captionLarge)
                        .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct ProfileGenderField: View {
    @Binding var gender: Gender?
    let isEditing: Bool
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: AppSpacing.xs) {
            HStack(spacing: AppSpacing.xs) {
                Image(systemName: "person.fill")
                    .font(.caption)
                    .foregroundStyle(AppTheme.primary)
                Text("Gender")
                    .font(AppTypography.captionLarge)
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
            }

            if isEditing {
                HStack(spacing: AppSpacing.xs) {
                    ForEach(Gender.allCases, id: \.self) { g in
                        Button {
                            gender = g
                        } label: {
                            Text(g.rawValue.capitalized)
                                .font(AppTypography.captionSmall)
                                .foregroundStyle(gender == g ? .white : AppTheme.adaptiveOnSurface(colorScheme))
                                .padding(.horizontal, AppSpacing.sm)
                                .padding(.vertical, AppSpacing.xs)
                                .background(gender == g ? AppTheme.primary : AppTheme.adaptiveSurfaceVariant(colorScheme))
                                .clipShape(Capsule())
                        }
                    }
                }
            } else {
                Text(gender?.rawValue.capitalized ?? "--")
                    .font(AppTypography.titleMedium)
                    .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct ProfileLinkRow: View {
    let icon: String
    let title: String
    let color: Color
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(spacing: AppSpacing.md) {
            Image(systemName: icon)
                .foregroundStyle(color)
                .frame(width: 28)
            Text(title)
                .font(AppTypography.bodyMedium)
                .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))
            Spacer()
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
        }
        .padding(AppSpacing.lg)
        .background(AppTheme.adaptiveSurface(colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.medium))
    }
}

// MARK: - Flow Layout

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = layout(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = layout(proposal: proposal, subviews: subviews)
        for (index, position) in result.positions.enumerated() {
            subviews[index].place(at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y), proposal: .unspecified)
        }
    }

    private func layout(proposal: ProposedViewSize, subviews: Subviews) -> (size: CGSize, positions: [CGPoint]) {
        let maxWidth = proposal.width ?? .infinity
        var positions: [CGPoint] = []
        var currentX: CGFloat = 0
        var currentY: CGFloat = 0
        var lineHeight: CGFloat = 0
        var maxX: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if currentX + size.width > maxWidth && currentX > 0 {
                currentX = 0
                currentY += lineHeight + spacing
                lineHeight = 0
            }
            positions.append(CGPoint(x: currentX, y: currentY))
            lineHeight = max(lineHeight, size.height)
            currentX += size.width + spacing
            maxX = max(maxX, currentX)
        }

        return (CGSize(width: maxX, height: currentY + lineHeight), positions)
    }
}
