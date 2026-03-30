import SwiftUI

struct GamificationScreen: View {
    @State private var viewModel = GamificationViewModel()
    @State private var showAchievementAlert = false
    @State private var alertAchievement: Achievement?
    @Environment(\.modelContext) private var modelContext

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                if let gamification = viewModel.gamification {
                    LevelCard(gamification: gamification)
                }

                if let rings = viewModel.todayRings {
                    DailyRingsCard(rings: rings)
                }

                if let gamification = viewModel.gamification {
                    StreakCard(gamification: gamification)
                }

                WeeklyProgressCard(weeklyRings: viewModel.weeklyRings)

                if !viewModel.recentXp.isEmpty {
                    RecentXpCard(transactions: viewModel.recentXp)
                }
            }
            .padding(16)
        }
        .navigationTitle("Progress")
        .onAppear {
            viewModel.configure(gamificationRepository: GamificationRepository(context: modelContext))
        }
        .refreshable { viewModel.loadData() }
        .onChange(of: viewModel.newAchievements.count) {
            if let first = viewModel.newAchievements.first {
                alertAchievement = first
                showAchievementAlert = true
            }
        }
        .alert("Achievement Unlocked!", isPresented: $showAchievementAlert) {
            Button("Awesome!") {
                if let a = alertAchievement {
                    viewModel.dismissAchievementNotification(a.id)
                }
            }
        } message: {
            if let a = alertAchievement {
                Text("\(a.name)\n\(a.achievementDescription)\n+\(a.xpReward) XP")
            }
        }
    }
}

// MARK: - Level Card

private struct LevelCard: View {
    let gamification: UserGamification

    var body: some View {
        VStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(AppTheme.primary)
                    .frame(width: 80, height: 80)
                Text("\(gamification.currentLevel)")
                    .font(.largeTitle.bold())
                    .foregroundStyle(.white)
            }

            Text("Level \(gamification.currentLevel)")
                .font(.title3.bold())

            Text("\(gamification.totalXp) XP")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            VStack(spacing: 4) {
                HStack {
                    Text("Next Level")
                        .font(.caption2)
                    Spacer()
                    Text("\(gamification.xpToNextLevel) XP to go")
                        .font(.caption2)
                }
                ProgressView(value: gamification.levelProgress)
                    .tint(AppTheme.primary)
            }
        }
        .padding(20)
        .background(AppTheme.primaryContainer, in: RoundedRectangle(cornerRadius: 16))
    }
}

// MARK: - Daily Rings Card (Apple Watch-style concentric rings)

private struct DailyRingsCard: View {
    let rings: DailyRings

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Today's Goals")
                .font(.headline)

            HStack(spacing: 0) {
                Spacer()
                ConcentricRingsView(
                    moveProgress: CGFloat(rings.moveProgress),
                    exerciseProgress: CGFloat(rings.exerciseProgress),
                    distanceProgress: CGFloat(rings.distanceProgress)
                )
                .frame(width: 140, height: 140)
                Spacer()
            }

            HStack {
                RingLegend(color: Color(hex: "FF2D55"), label: "Move",
                           value: "\(rings.moveMinutes)/\(rings.moveGoal) min")
                Spacer()
                RingLegend(color: Color(hex: "30D158"), label: "Exercise",
                           value: "\(rings.exerciseMinutes)/\(rings.exerciseGoal) min")
                Spacer()
                RingLegend(color: Color(hex: "007AFF"), label: "Distance",
                           value: String(format: "%.1f/%.1f km",
                                         rings.distanceMeters / 1000,
                                         rings.distanceGoal / 1000))
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }
}

private struct RingLegend: View {
    let color: Color
    let label: String
    let value: String

    var body: some View {
        VStack(spacing: 2) {
            Circle().fill(color).frame(width: 8, height: 8)
            Text(label).font(.caption2.bold())
            Text(value).font(.caption2).foregroundStyle(.secondary)
        }
    }
}

// MARK: - Concentric Activity Rings

struct ConcentricRingsView: View {
    let moveProgress: CGFloat
    let exerciseProgress: CGFloat
    let distanceProgress: CGFloat

    @State private var animatedMove: CGFloat = 0
    @State private var animatedExercise: CGFloat = 0
    @State private var animatedDistance: CGFloat = 0

    var body: some View {
        ZStack {
            // Outer ring - Move (red)
            ActivityRingShape(progress: animatedMove, lineWidth: 14)
                .foregroundStyle(Color(hex: "FF2D55"))
                .padding(0)

            ActivityRingBackground(lineWidth: 14)
                .foregroundStyle(Color(hex: "FF2D55").opacity(0.2))
                .padding(0)

            // Middle ring - Exercise (green)
            ActivityRingShape(progress: animatedExercise, lineWidth: 14)
                .foregroundStyle(Color(hex: "30D158"))
                .padding(18)

            ActivityRingBackground(lineWidth: 14)
                .foregroundStyle(Color(hex: "30D158").opacity(0.2))
                .padding(18)

            // Inner ring - Distance (blue)
            ActivityRingShape(progress: animatedDistance, lineWidth: 14)
                .foregroundStyle(Color(hex: "007AFF"))
                .padding(36)

            ActivityRingBackground(lineWidth: 14)
                .foregroundStyle(Color(hex: "007AFF").opacity(0.2))
                .padding(36)
        }
        .onAppear {
            withAnimation(.easeOut(duration: 1.0)) {
                animatedMove = min(moveProgress, 1)
                animatedExercise = min(exerciseProgress, 1)
                animatedDistance = min(distanceProgress, 1)
            }
        }
    }
}

private struct ActivityRingShape: Shape {
    var progress: CGFloat
    var lineWidth: CGFloat

    var animatableData: CGFloat {
        get { progress }
        set { progress = newValue }
    }

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let center = CGPoint(x: rect.midX, y: rect.midY)
        let radius = min(rect.width, rect.height) / 2 - lineWidth / 2
        path.addArc(center: center, radius: radius,
                    startAngle: .degrees(-90),
                    endAngle: .degrees(-90 + 360 * Double(progress)),
                    clockwise: false)
        return path.strokedPath(.init(lineWidth: lineWidth, lineCap: .round))
    }
}

private struct ActivityRingBackground: Shape {
    var lineWidth: CGFloat

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let center = CGPoint(x: rect.midX, y: rect.midY)
        let radius = min(rect.width, rect.height) / 2 - lineWidth / 2
        path.addArc(center: center, radius: radius,
                    startAngle: .degrees(0), endAngle: .degrees(360),
                    clockwise: false)
        return path.strokedPath(.init(lineWidth: lineWidth, lineCap: .round))
    }
}

// MARK: - Streak Card

private struct StreakCard: View {
    let gamification: UserGamification

    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(Color.orange.opacity(0.2))
                    .frame(width: 56, height: 56)
                Text("\u{1F525}")
                    .font(.system(size: 28))
            }

            VStack(alignment: .leading, spacing: 2) {
                Text("\(gamification.currentStreak) Day Streak")
                    .font(.headline)
                Text(gamification.currentStreak > 0 ? "Keep it going!" : "Start your streak today!")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 2) {
                Text("Best").font(.caption).foregroundStyle(.secondary)
                Text("\(gamification.longestStreak)")
                    .font(.title3.bold())
            }
        }
        .padding(16)
        .background(
            gamification.currentStreak > 0
                ? Color.orange.opacity(0.1)
                : AppTheme.surfaceContainer,
            in: RoundedRectangle(cornerRadius: 16)
        )
    }
}

// MARK: - Weekly Progress

private struct WeeklyProgressCard: View {
    let weeklyRings: [DailyRings]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("This Week").font(.headline)

            HStack {
                ForEach(sortedRings, id: \.date) { ring in
                    let isToday = Calendar.current.isDateInToday(ring.date)
                    VStack(spacing: 4) {
                        Text(dayLetter(for: ring.date))
                            .font(.caption2)
                            .foregroundStyle(isToday ? AppTheme.primary : .secondary)

                        ZStack {
                            Circle()
                                .fill(circleColor(ring: ring, isToday: isToday))
                                .frame(width: 32, height: 32)
                            if ring.allRingsClosed {
                                Image(systemName: "checkmark")
                                    .font(.caption2.bold())
                                    .foregroundStyle(.white)
                            }
                        }
                    }
                    if ring.date != sortedRings.last?.date {
                        Spacer()
                    }
                }
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }

    private var sortedRings: [DailyRings] {
        weeklyRings.sorted { $0.date < $1.date }
    }

    private func dayLetter(for date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "EEEEE"
        return formatter.string(from: date)
    }

    private func circleColor(ring: DailyRings, isToday: Bool) -> Color {
        if ring.allRingsClosed { return Color(hex: "30D158") }
        if ring.exerciseMinutes > 0 { return Color.orange.opacity(0.5) }
        if isToday { return AppTheme.primaryContainer }
        return AppTheme.surfaceContainer
    }
}

// MARK: - Recent XP

private struct RecentXpCard: View {
    let transactions: [XpTransaction]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Recent XP").font(.headline)

            ForEach(Array(transactions.prefix(5))) { tx in
                HStack {
                    Text(tx.reason.displayName)
                        .font(.subheadline)
                    Spacer()
                    Text("+\(tx.amount) XP")
                        .font(.subheadline.bold())
                        .foregroundStyle(AppTheme.primary)
                }
                .padding(.vertical, 2)
            }
        }
        .padding(16)
        .background(AppTheme.surfaceContainer, in: RoundedRectangle(cornerRadius: 16))
    }
}

#Preview {
    NavigationStack {
        GamificationScreen()
    }
}
