import SwiftUI

// MARK: - Models

struct LeaderboardEntry: Identifiable {
    let id = UUID()
    let rank: Int
    let name: String
    let xp: Int
    let level: Int
    let avatarEmoji: String
    var isCurrentUser: Bool = false
}

enum LeaderboardPeriod: String, CaseIterable, Identifiable {
    case weekly = "This Week"
    case monthly = "This Month"
    case allTime = "All Time"
    var id: String { rawValue }
}

// MARK: - Screen

struct LeaderboardScreen: View {
    @State private var selectedPeriod: LeaderboardPeriod = .weekly
    @State private var entries: [LeaderboardEntry] = []
    @State private var friendsList: [LeaderboardEntry] = []
    @State private var showFriends = false

    var body: some View {
        VStack(spacing: 0) {
            // Period picker
            Picker("Period", selection: $selectedPeriod) {
                ForEach(LeaderboardPeriod.allCases) { period in
                    Text(period.rawValue).tag(period)
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 16)
            .padding(.top, 8)

            // Toggle
            HStack {
                Button(action: { showFriends = false }) {
                    Text("Global")
                        .font(.subheadline.bold())
                        .foregroundStyle(showFriends ? .secondary : AppTheme.primary)
                }
                Button(action: { showFriends = true }) {
                    Text("Friends")
                        .font(.subheadline.bold())
                        .foregroundStyle(showFriends ? AppTheme.primary : .secondary)
                }
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            ScrollView {
                LazyVStack(spacing: 0) {
                    // Top 3 podium
                    if !currentEntries.isEmpty {
                        podiumView
                    }

                    // Rest of leaderboard
                    ForEach(currentEntries.dropFirst(3)) { entry in
                        LeaderboardRowView(entry: entry)
                        Divider().padding(.leading, 60)
                    }

                    // Personal ranking card
                    if let myEntry = currentEntries.first(where: \.isCurrentUser) {
                        personalRankingCard(entry: myEntry)
                            .padding(16)
                    }

                    // Friends invite placeholder
                    if showFriends && friendsList.count < 5 {
                        inviteFriendsCard
                            .padding(16)
                    }
                }
            }
        }
        .navigationTitle("Leaderboard")
        .onAppear { generateData() }
        .onChange(of: selectedPeriod) { _, _ in generateData() }
    }

    private var currentEntries: [LeaderboardEntry] {
        showFriends ? friendsList : entries
    }

    // MARK: - Podium

    private var podiumView: some View {
        HStack(alignment: .bottom, spacing: 12) {
            if currentEntries.count > 1 {
                podiumColumn(entry: currentEntries[1], height: 80, medal: "2")
            }
            if !currentEntries.isEmpty {
                podiumColumn(entry: currentEntries[0], height: 110, medal: "1")
            }
            if currentEntries.count > 2 {
                podiumColumn(entry: currentEntries[2], height: 65, medal: "3")
            }
        }
        .padding(.horizontal, 32)
        .padding(.vertical, 20)
    }

    private func podiumColumn(entry: LeaderboardEntry, height: CGFloat, medal: String) -> some View {
        VStack(spacing: 8) {
            ZStack {
                Circle()
                    .fill(entry.isCurrentUser ? AppTheme.primary.opacity(0.2) : Color.gray.opacity(0.1))
                    .frame(width: 56, height: 56)
                Text(entry.avatarEmoji)
                    .font(.title)
            }

            Text(entry.name)
                .font(.caption.bold())
                .lineLimit(1)

            Text("\(entry.xp) XP")
                .font(.caption2)
                .foregroundStyle(.secondary)

            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(medalColor(medal))
                    .frame(height: height)
                    .frame(maxWidth: .infinity)

                VStack {
                    Text(medalEmoji(medal))
                        .font(.title2)
                    Text("#\(medal)")
                        .font(.caption.bold())
                        .foregroundStyle(.white)
                }
            }
        }
    }

    private func medalColor(_ medal: String) -> Color {
        switch medal {
        case "1": return Color(hex: "FFD700")
        case "2": return Color(hex: "C0C0C0")
        case "3": return Color(hex: "CD7F32")
        default: return .gray
        }
    }

    private func medalEmoji(_ medal: String) -> String {
        switch medal {
        case "1": return "\u{1F947}"
        case "2": return "\u{1F948}"
        case "3": return "\u{1F949}"
        default: return ""
        }
    }

    // MARK: - Personal Ranking

    private func personalRankingCard(entry: LeaderboardEntry) -> some View {
        HStack(spacing: 12) {
            Text("#\(entry.rank)")
                .font(.title3.bold())
                .foregroundStyle(AppTheme.primary)
                .frame(width: 40)

            Text(entry.avatarEmoji)
                .font(.title2)

            VStack(alignment: .leading, spacing: 2) {
                Text("Your Ranking")
                    .font(.subheadline.bold())
                Text("Level \(entry.level) - \(entry.xp) XP")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Image(systemName: "chevron.up")
                .foregroundStyle(AppTheme.primary)
                .font(.caption.bold())
            Text("Top \(min(entry.rank * 100 / max(entries.count, 1), 100))%")
                .font(.caption.bold())
                .foregroundStyle(AppTheme.primary)
        }
        .padding(16)
        .background(AppTheme.primaryContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Invite Friends

    private var inviteFriendsCard: some View {
        VStack(spacing: 12) {
            Image(systemName: "person.badge.plus")
                .font(.largeTitle)
                .foregroundStyle(AppTheme.primary)
            Text("Invite Friends")
                .font(.headline)
            Text("Compete with friends to stay motivated")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("Invite") {}
                .buttonStyle(.borderedProminent)
                .tint(AppTheme.primary)
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Data

    private func generateData() {
        let names = ["Alex M.", "Jordan K.", "Sam W.", "Taylor R.", "Casey L.",
                     "Morgan P.", "Riley D.", "Drew T.", "Quinn B.", "Avery S.",
                     "Blake H.", "Charlie N.", "Dakota F.", "Emery G.", "Finley J."]
        let emojis = ["\u{1F9D1}\u{200D}\u{1F3CB}", "\u{1F3C3}", "\u{1F6B4}", "\u{1F3CA}",
                      "\u{1F9D8}", "\u{1F9D7}", "\u{1F3CB}", "\u{26F7}", "\u{1F3C4}", "\u{1F938}",
                      "\u{1F93A}", "\u{1F93E}", "\u{1F3C7}", "\u{26BD}", "\u{1F3BE}"]

        let multiplier: Int = switch selectedPeriod {
        case .weekly: 1
        case .monthly: 4
        case .allTime: 20
        }

        entries = names.enumerated().map { i, name in
            let xp = max(3000 - i * Int.random(in: 100...300), 50) * multiplier
            return LeaderboardEntry(
                rank: i + 1, name: name, xp: xp,
                level: { var l = 1; while l * l * 50 <= xp { l += 1 }; return l - 1 }(),
                avatarEmoji: emojis[i % emojis.count],
                isCurrentUser: i == 6
            )
        }

        friendsList = Array(entries.filter { $0.isCurrentUser || Int.random(in: 0...2) == 0 }
            .prefix(8))
            .enumerated()
            .map { i, entry in
                var e = entry
                e.isCurrentUser = entry.isCurrentUser
                return LeaderboardEntry(rank: i + 1, name: entry.name, xp: entry.xp,
                                       level: entry.level, avatarEmoji: entry.avatarEmoji,
                                       isCurrentUser: entry.isCurrentUser)
            }
    }
}

// MARK: - Row View

private struct LeaderboardRowView: View {
    let entry: LeaderboardEntry

    var body: some View {
        HStack(spacing: 12) {
            Text("#\(entry.rank)")
                .font(.subheadline.bold())
                .foregroundStyle(.secondary)
                .frame(width: 32)

            Text(entry.avatarEmoji)
                .font(.title3)

            VStack(alignment: .leading, spacing: 2) {
                Text(entry.name)
                    .font(.subheadline.weight(entry.isCurrentUser ? .bold : .regular))
                Text("Level \(entry.level)")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Text("\(entry.xp) XP")
                .font(.subheadline.bold())
                .foregroundStyle(entry.isCurrentUser ? AppTheme.primary : .primary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(entry.isCurrentUser ? AppTheme.primaryContainer.opacity(0.3) : .clear)
    }
}

#Preview {
    NavigationStack {
        LeaderboardScreen()
    }
}
