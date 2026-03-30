import SwiftUI

struct AchievementsScreen: View {
    @State private var viewModel = GamificationViewModel()
    @State private var selectedCategory: AchievementCategory?
    @State private var selectedAchievement: AchievementWithProgress?
    @Environment(\.modelContext) private var modelContext

    private let columns = [
        GridItem(.adaptive(minimum: 100, maximum: 120), spacing: 12)
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Summary header
                summaryHeader

                // Category filter
                categoryFilter

                // Achievement grid
                achievementGrid
            }
            .padding(16)
        }
        .navigationTitle("Achievements")
        .onAppear {
            viewModel.configure(gamificationRepository: GamificationRepository(context: modelContext))
        }
        .sheet(item: $selectedAchievement) { achievement in
            AchievementDetailSheet(achievement: achievement)
                .presentationDetents([.medium])
        }
    }

    // MARK: - Summary

    private var summaryHeader: some View {
        HStack(spacing: 24) {
            VStack(spacing: 4) {
                Text("\(viewModel.unlockedAchievements.count)")
                    .font(.title.bold())
                    .foregroundStyle(AppTheme.primary)
                Text("Unlocked")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            VStack(spacing: 4) {
                Text("\(viewModel.achievements.count)")
                    .font(.title.bold())
                Text("Total")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            VStack(spacing: 4) {
                let percent = viewModel.achievements.isEmpty ? 0 :
                    Int(Double(viewModel.unlockedAchievements.count) / Double(viewModel.achievements.count) * 100)
                Text("\(percent)%")
                    .font(.title.bold())
                    .foregroundStyle(.orange)
                Text("Complete")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Category Filter

    private var categoryFilter: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                CategoryChip(title: "All", isSelected: selectedCategory == nil) {
                    selectedCategory = nil
                }
                ForEach(AchievementCategory.allCases) { category in
                    if viewModel.achievementsByCategory[category] != nil {
                        CategoryChip(
                            title: category.displayName,
                            isSelected: selectedCategory == category
                        ) {
                            selectedCategory = category
                        }
                    }
                }
            }
        }
    }

    // MARK: - Grid

    private var achievementGrid: some View {
        let filtered: [AchievementWithProgress]
        if let cat = selectedCategory {
            filtered = viewModel.achievementsByCategory[cat] ?? []
        } else {
            filtered = viewModel.achievements
        }

        let sorted = filtered.sorted { a, b in
            if a.isUnlocked != b.isUnlocked { return a.isUnlocked }
            return a.achievement.name < b.achievement.name
        }

        return LazyVGrid(columns: columns, spacing: 12) {
            ForEach(sorted) { item in
                AchievementBadgeView(achievement: item)
                    .onTapGesture { selectedAchievement = item }
            }
        }
    }
}

// MARK: - Badge View

private struct AchievementBadgeView: View {
    let achievement: AchievementWithProgress

    var body: some View {
        VStack(spacing: 8) {
            ZStack {
                Circle()
                    .fill(achievement.isUnlocked ? AppTheme.primary : Color.gray.opacity(0.3))
                    .frame(width: 48, height: 48)
                Image(systemName: iconForCategory(achievement.achievement.category))
                    .font(.title3)
                    .foregroundStyle(achievement.isUnlocked ? .white : .gray)
            }

            Text(achievement.achievement.name)
                .font(.caption2.weight(.medium))
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .foregroundStyle(achievement.isUnlocked ? .primary : .secondary)

            if !achievement.isUnlocked {
                ProgressView(value: achievement.progressPercent)
                    .tint(AppTheme.primary)
                    .frame(height: 4)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .background(
            achievement.isUnlocked
                ? AppTheme.primaryContainer
                : Color.gray.opacity(0.08),
            in: RoundedRectangle(cornerRadius: 12)
        )
    }

    private func iconForCategory(_ category: AchievementCategory) -> String {
        switch category {
        case .distance: return "figure.run"
        case .workouts: return "dumbbell"
        case .streaks: return "flame"
        case .speed: return "hare"
        case .consistency: return "calendar"
        case .personalBest: return "trophy"
        case .social: return "person.2"
        case .exploration: return "map"
        case .special: return "star"
        }
    }
}

// MARK: - Category Chip

private struct CategoryChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.caption.bold())
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(isSelected ? AppTheme.primary : AppTheme.surfaceContainer,
                            in: Capsule())
                .foregroundStyle(isSelected ? .white : .primary)
        }
    }
}

// MARK: - Detail Sheet

private struct AchievementDetailSheet: View {
    let achievement: AchievementWithProgress

    var body: some View {
        VStack(spacing: 20) {
            ZStack {
                Circle()
                    .fill(achievement.isUnlocked ? AppTheme.primary : Color.gray.opacity(0.3))
                    .frame(width: 80, height: 80)
                Image(systemName: achievement.isUnlocked ? "trophy.fill" : "lock.fill")
                    .font(.title)
                    .foregroundStyle(achievement.isUnlocked ? .white : .gray)
            }
            .padding(.top, 20)

            Text(achievement.achievement.name)
                .font(.title2.bold())

            Text(achievement.achievement.achievementDescription)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            if achievement.isUnlocked {
                if let date = achievement.unlockedAt {
                    Text("Unlocked \(date.formatted(date: .abbreviated, time: .omitted))")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Text("+\(achievement.achievement.xpReward) XP")
                    .font(.headline)
                    .foregroundStyle(AppTheme.primary)
            } else {
                VStack(spacing: 8) {
                    Text("Progress")
                        .font(.subheadline.bold())
                    ProgressView(value: achievement.progressPercent)
                        .tint(AppTheme.primary)
                        .frame(width: 200)
                    Text("\(achievement.progress) / \(achievement.achievement.requirement)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            HStack(spacing: 8) {
                Label(achievement.achievement.category.displayName,
                      systemImage: "tag")
                    .font(.caption)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(AppTheme.surfaceContainer, in: Capsule())

                Label("+\(achievement.achievement.xpReward) XP",
                      systemImage: "star.fill")
                    .font(.caption)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color.orange.opacity(0.15), in: Capsule())
            }

            Spacer()
        }
    }
}

#Preview {
    NavigationStack {
        AchievementsScreen()
    }
}
