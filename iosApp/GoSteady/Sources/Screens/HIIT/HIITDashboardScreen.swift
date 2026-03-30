import SwiftUI

private let hiitOrange = Color(hex: "FF6D00")

struct HIITDashboardScreen: View {
    @State private var viewModel: HIITViewModel?
    @State private var navigateToActiveHIIT = false
    @State private var selectedTemplateId: String = ""
    @State private var navigateToEditor = false
    @State private var selectedCategory: HIITTemplateType? = nil
    @Environment(\.modelContext) private var modelContext

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // Hero banner
                    DashboardHeroBanner(
                        icon: "flame.fill",
                        title: "HIIT",
                        subtitle: "High intensity interval training"
                    )

                    if let vm = viewModel {
                        contentView(vm)
                    } else {
                        loadingView
                    }
                }
                .padding(.vertical)
                .padding(.bottom, 32)
            }
            .background(AppTheme.surface)
            .refreshable {
                viewModel?.loadData()
            }
            .navigationTitle("HIIT Workouts")
            .navigationDestination(isPresented: $navigateToActiveHIIT) {
                ActiveHIITScreen(templateId: selectedTemplateId)
            }
            .navigationDestination(isPresented: $navigateToEditor) {
                HIITTemplateEditorScreen { template in
                    viewModel?.saveCustomTemplate(template)
                }
            }
            .onAppear {
                if viewModel == nil {
                    let repo = HIITRepository(context: modelContext)
                    viewModel = HIITViewModel(hiitRepository: repo)
                }
            }
        }
    }

    @ViewBuilder
    private func contentView(_ vm: HIITViewModel) -> some View {
        if vm.isLoading {
            loadingView
        } else if let error = vm.errorMessage {
            errorView(error)
        } else {
            // Weekly stats
            if vm.weeklySessionCount > 0 {
                weeklyStatsCard(vm)
            }

            // Category filter
            categoryFilter(vm)

            // Template gallery
            templateGallery(vm)

            // Custom templates
            if !vm.customTemplates.isEmpty {
                customTemplatesSection(vm)
            }

            // Create custom button
            createCustomButton

            // Recent sessions
            if !vm.recentSessions.isEmpty {
                recentSessionsSection(vm)
            }
        }
    }

    // MARK: - Weekly Stats

    private func weeklyStatsCard(_ vm: HIITViewModel) -> some View {
        HStack(spacing: 0) {
            VStack(spacing: 4) {
                Text("\(vm.weeklySessionCount)")
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundStyle(hiitOrange)
                Text("This Week")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)

            VStack(spacing: 4) {
                Text("\(vm.weeklyCalories)")
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundStyle(hiitOrange)
                Text("Calories")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)
        }
        .padding(16)
        .background(hiitOrange.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - Category Filter

    private func categoryFilter(_ vm: HIITViewModel) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                categoryChip(label: "All", type: nil, vm: vm)
                ForEach(HIITTemplateType.allCases) { type in
                    categoryChip(label: type.rawValue, type: type, vm: vm)
                }
            }
            .padding(.horizontal)
        }
    }

    private func categoryChip(label: String, type: HIITTemplateType?, vm: HIITViewModel) -> some View {
        Button {
            withAnimation(.easeInOut(duration: 0.2)) {
                selectedCategory = type
                vm.selectedCategory = type
            }
        } label: {
            HStack(spacing: 4) {
                if let t = type {
                    Image(systemName: t.icon)
                        .font(.caption2)
                }
                Text(label)
                    .font(.caption)
                    .fontWeight(.semibold)
            }
            .foregroundStyle(selectedCategory == type ? .white : AppTheme.onSurface)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(selectedCategory == type ? hiitOrange : AppTheme.surfaceContainerLow)
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    // MARK: - Template Gallery

    private func templateGallery(_ vm: HIITViewModel) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("CHOOSE A WORKOUT")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.onSurfaceVariant)
                .padding(.horizontal)

            ForEach(vm.filteredTemplates, id: \.id) { template in
                Button {
                    selectedTemplateId = template.id
                    navigateToActiveHIIT = true
                } label: {
                    templateCard(template)
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func templateCard(_ template: HIITWorkoutTemplate) -> some View {
        let exerciseNames = template.exercises.map(\.exercise.name).joined(separator: ", ")
        return VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(template.name)
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundStyle(AppTheme.onSurface)
                Spacer()
                difficultyBadge(template.difficulty)
            }

            Text(template.templateDescription)
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(2)

            HStack(spacing: 16) {
                infoChip(icon: "timer", text: template.formattedDuration)
                infoChip(icon: "dumbbell.fill", text: "\(template.exercises.count) exercises")
                infoChip(icon: "repeat", text: "\(template.rounds) rounds")
            }

            Text(exerciseNames)
                .font(.caption2)
                .foregroundStyle(.tertiary)
                .lineLimit(2)
        }
        .padding(16)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    private func difficultyBadge(_ difficulty: String) -> some View {
        let colorHex = HIITViewModel.difficultyColor(difficulty)
        return Text(difficulty)
            .font(.caption2)
            .fontWeight(.bold)
            .foregroundStyle(Color(hex: colorHex))
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(Color(hex: colorHex).opacity(0.15))
            .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private func infoChip(icon: String, text: String) -> some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: 10))
            Text(text)
                .font(.caption2)
        }
        .foregroundStyle(.secondary)
    }

    // MARK: - Custom Templates

    private func customTemplatesSection(_ vm: HIITViewModel) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("YOUR CUSTOM WORKOUTS")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.onSurfaceVariant)
                .padding(.horizontal)

            ForEach(vm.customTemplates, id: \.id) { template in
                Button {
                    selectedTemplateId = template.id
                    navigateToActiveHIIT = true
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(template.name)
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .foregroundStyle(AppTheme.onSurface)
                            HStack(spacing: 12) {
                                infoChip(icon: "timer", text: template.formattedDuration)
                                infoChip(icon: "dumbbell.fill", text: "\(template.exercises.count) exercises")
                            }
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                    }
                    .padding(12)
                    .background(AppTheme.surfaceContainerLow)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .padding(.horizontal)
                }
                .buttonStyle(.plain)
                .contextMenu {
                    Button(role: .destructive) {
                        vm.deleteCustomTemplate(template)
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
            }
        }
    }

    // MARK: - Create Custom Button

    private var createCustomButton: some View {
        Button {
            navigateToEditor = true
        } label: {
            HStack {
                Image(systemName: "plus.circle.fill")
                    .foregroundStyle(hiitOrange)
                Text("Create Custom HIIT")
                    .fontWeight(.semibold)
                    .foregroundStyle(AppTheme.onSurface)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(hiitOrange.opacity(0.3), style: StrokeStyle(lineWidth: 1, dash: [6]))
            )
            .padding(.horizontal)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Recent Sessions

    private func recentSessionsSection(_ vm: HIITViewModel) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("RECENT SESSIONS")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundStyle(AppTheme.onSurfaceVariant)
                .padding(.horizontal)

            ForEach(vm.recentSessions, id: \.id) { session in
                recentSessionCard(session, vm: vm)
            }
        }
    }

    private func recentSessionCard(_ session: HIITSession, vm: HIITViewModel) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(session.templateName)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                Text(session.date.formatted(date: .abbreviated, time: .shortened))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text(session.durationFormatted)
                    .font(.subheadline)
                    .fontWeight(.bold)
                if session.caloriesEstimate > 0 {
                    Text("\(session.caloriesEstimate) cal")
                        .font(.caption)
                        .foregroundStyle(hiitOrange)
                }
            }
        }
        .padding(12)
        .background(AppTheme.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
        .contextMenu {
            Button(role: .destructive) {
                vm.deleteSession(session)
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
    }

    // MARK: - Loading/Error

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .tint(hiitOrange)
            Text("Loading HIIT data...")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(48)
    }

    private func errorView(_ message: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundStyle(AppTheme.error)
            Text("Something went wrong")
                .fontWeight(.semibold)
            Text(message)
                .font(.caption)
                .foregroundStyle(.secondary)
            Button("Retry") { viewModel?.loadData() }
                .buttonStyle(.borderedProminent)
                .tint(hiitOrange)
        }
        .frame(maxWidth: .infinity)
        .padding(32)
    }
}
