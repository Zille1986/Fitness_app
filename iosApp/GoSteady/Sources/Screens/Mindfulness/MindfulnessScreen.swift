import SwiftUI

struct MindfulnessScreen: View {
    @State private var viewModel = MindfulnessViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 16) {
                    // Weekly Stats
                    weeklyStatsCard

                    // Quick Actions
                    quickActionsRow

                    // Breathing Exercises
                    breathingSection

                    // Guided Sessions
                    guidedSessionsSection

                    // Recent Sessions
                    if !viewModel.recentSessions.isEmpty {
                        recentSessionsSection
                    }

                    Spacer(minLength: 40)
                }
                .padding(.top)
            }
            .background(Color(hex: "121212"))
            .navigationTitle("Mindfulness")
        }
        .sheet(isPresented: $viewModel.showingMoodLog) {
            MoodTrackerScreen(viewModel: viewModel)
        }
        .fullScreenCover(item: Binding(
            get: { viewModel.breathingState.map { BreathingStateWrapper(state: $0) } },
            set: { _ in }
        )) { _ in
            BreathingExerciseScreen(viewModel: viewModel)
        }
        .fullScreenCover(item: Binding(
            get: { viewModel.guidedSessionState.map { GuidedStateWrapper(state: $0) } },
            set: { _ in }
        )) { _ in
            MeditationScreen(viewModel: viewModel)
        }
        .sheet(item: $viewModel.selectedBreathingPattern) { pattern in
            BreathingPatternPreview(pattern: pattern) {
                viewModel.startBreathingSession(pattern)
            }
            .presentationDetents([.medium])
        }
        .sheet(item: $viewModel.selectedGuidedSession) { content in
            GuidedSessionPreview(content: content) {
                viewModel.startGuidedSession(content)
            }
            .presentationDetents([.large])
        }
    }

    // MARK: - Weekly Stats

    private var weeklyStatsCard: some View {
        HStack(spacing: 0) {
            VStack(spacing: 4) {
                Text("\(viewModel.mindfulnessMinutesThisWeek)")
                    .font(.system(size: 36, weight: .bold, design: .rounded))
                    .foregroundStyle(Color(hex: "4CAF50"))
                Text("Minutes")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)

            Divider()
                .frame(height: 40)

            VStack(spacing: 4) {
                Text("\(viewModel.sessionsThisWeek)")
                    .font(.system(size: 36, weight: .bold, design: .rounded))
                    .foregroundStyle(Color(hex: "2196F3"))
                Text("Sessions")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)

            Divider()
                .frame(height: 40)

            VStack(spacing: 4) {
                Text("\(viewModel.currentStreak)")
                    .font(.system(size: 36, weight: .bold, design: .rounded))
                    .foregroundStyle(Color(hex: "FF9800"))
                Text("Day Streak")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)
        }
        .padding(20)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - Quick Actions

    private var quickActionsRow: some View {
        HStack(spacing: 12) {
            Button {
                viewModel.showingMoodLog = true
            } label: {
                QuickActionTile(emoji: "face.smiling", title: "Log Mood", subtitle: "How are you feeling?")
            }
            .buttonStyle(.plain)

            NavigationLink(destination: MoodTrackerScreen(viewModel: viewModel)) {
                QuickActionTile(emoji: "calendar", title: "Mood History", subtitle: "View trends")
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal)
    }

    // MARK: - Breathing Exercises

    private var breathingSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Breathing Exercises")
                .font(.headline)
                .foregroundStyle(.white)
                .padding(.horizontal)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(BreathingPatterns.getAll()) { pattern in
                        Button {
                            viewModel.selectedBreathingPattern = pattern
                        } label: {
                            BreathingPatternCard(pattern: pattern)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal)
            }
        }
    }

    // MARK: - Guided Sessions

    private var guidedSessionsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Guided Sessions")
                .font(.headline)
                .foregroundStyle(.white)
                .padding(.horizontal)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ForEach(MindfulnessSessions.getAll()) { session in
                        Button {
                            viewModel.selectedGuidedSession = session
                        } label: {
                            GuidedSessionCard(session: session)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal)
            }
        }
    }

    // MARK: - Recent Sessions

    private var recentSessionsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Recent Sessions")
                .font(.headline)
                .foregroundStyle(.white)
                .padding(.horizontal)

            ForEach(viewModel.recentSessions.prefix(5)) { session in
                RecentSessionRow(session: session)
                    .padding(.horizontal)
            }
        }
    }
}

// MARK: - Quick Action Tile

private struct QuickActionTile: View {
    let emoji: String
    let title: String
    let subtitle: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: emoji)
                .font(.largeTitle)
                .foregroundStyle(Color(hex: "4CAF50"))
            Text(title)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(.white)
            Text(subtitle)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(16)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Breathing Pattern Card

private struct BreathingPatternCard: View {
    let pattern: BreathingPattern

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Image(systemName: "wind")
                .font(.title2)
                .foregroundStyle(Color(hex: "2196F3"))

            Text(pattern.name)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(.white)
                .lineLimit(2)

            Text("\(pattern.totalDuration / 60) min")
                .font(.caption)
                .foregroundStyle(.secondary)

            Text(pattern.benefits.first ?? "")
                .font(.caption2)
                .foregroundStyle(Color(hex: "4CAF50"))
                .lineLimit(1)
        }
        .padding(16)
        .frame(width: 160, alignment: .leading)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Guided Session Card

private struct GuidedSessionCard: View {
    let session: MindfulnessContent

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Image(systemName: session.type.icon)
                .font(.title2)
                .foregroundStyle(Color(hex: "FF9800"))

            Text(session.title)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(.white)
                .lineLimit(2)

            Text("\(session.durationSeconds / 60) min")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .frame(width: 180, alignment: .leading)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Recent Session Row

private struct RecentSessionRow: View {
    let session: MindfulnessSession

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: session.type.icon)
                .font(.title3)
                .foregroundStyle(Color(hex: "4CAF50"))
                .frame(width: 40, height: 40)
                .background(Color(hex: "4CAF50").opacity(0.15))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 2) {
                Text(session.type.title)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(.white)
                Text(session.timestamp, style: .date)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Text("\(session.durationSeconds / 60)m")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color(hex: "4CAF50"))

            if let rating = session.rating {
                HStack(spacing: 2) {
                    ForEach(1...5, id: \.self) { star in
                        Image(systemName: star <= rating ? "star.fill" : "star")
                            .font(.caption2)
                            .foregroundStyle(star <= rating ? Color(hex: "FFD700") : .secondary)
                    }
                }
            }
        }
        .padding(12)
        .background(Color(hex: "1E1E1E"))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Preview Sheets

private struct BreathingPatternPreview: View {
    let pattern: BreathingPattern
    let onStart: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(pattern.name)
                        .font(.title2.weight(.bold))
                        .foregroundStyle(.white)

                    Text(pattern.patternDescription)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Pattern")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.white)
                        Label("Inhale: \(pattern.inhaleSeconds)s", systemImage: "arrow.up.circle")
                        if pattern.holdAfterInhale > 0 {
                            Label("Hold: \(pattern.holdAfterInhale)s", systemImage: "pause.circle")
                        }
                        Label("Exhale: \(pattern.exhaleSeconds)s", systemImage: "arrow.down.circle")
                        if pattern.holdAfterExhale > 0 {
                            Label("Hold: \(pattern.holdAfterExhale)s", systemImage: "pause.circle")
                        }
                        Label("\(pattern.cycles) cycles", systemImage: "repeat")
                    }
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Benefits")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.white)
                        ForEach(pattern.benefits, id: \.self) { benefit in
                            Label(benefit, systemImage: "checkmark.circle.fill")
                                .font(.subheadline)
                                .foregroundStyle(Color(hex: "4CAF50"))
                        }
                    }

                    Button {
                        dismiss()
                        onStart()
                    } label: {
                        HStack {
                            Image(systemName: "play.fill")
                            Text("Start Exercise")
                        }
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(hex: "4CAF50"))
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .padding(.top, 8)
                }
                .padding()
            }
            .background(Color(hex: "121212"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

private struct GuidedSessionPreview: View {
    let content: MindfulnessContent
    let onStart: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(content.title)
                        .font(.title2.weight(.bold))
                        .foregroundStyle(.white)

                    Text(content.contentDescription)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)

                    Text("Duration: \(content.durationSeconds / 60) minutes")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(.white)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("What you'll do:")
                            .font(.subheadline.weight(.bold))
                            .foregroundStyle(.white)

                        ForEach(Array(content.instructions.enumerated()), id: \.offset) { index, instruction in
                            HStack(alignment: .top, spacing: 8) {
                                Text("\(index + 1).")
                                    .font(.caption.weight(.bold))
                                    .foregroundStyle(Color(hex: "4CAF50"))
                                    .frame(width: 20)
                                Text(instruction)
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }

                    Button {
                        dismiss()
                        onStart()
                    } label: {
                        HStack {
                            Image(systemName: "play.fill")
                            Text("Begin Session")
                        }
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(hex: "4CAF50"))
                        .foregroundStyle(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .padding(.top, 8)
                }
                .padding()
            }
            .background(Color(hex: "121212"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

// MARK: - Wrapper types for fullScreenCover

private struct BreathingStateWrapper: Identifiable {
    let id = UUID()
    let state: BreathingSessionState
}

private struct GuidedStateWrapper: Identifiable {
    let id = UUID()
    let state: GuidedSessionState
}

#Preview {
    MindfulnessScreen()
        .preferredColorScheme(.dark)
}
