import SwiftUI
import SwiftData

struct AICoachScreen: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @State private var viewModel = AICoachViewModel()
    @FocusState private var isInputFocused: Bool

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Chat messages
                chatMessages

                // Suggested prompts
                if let lastAssistant = viewModel.chatHistory.last(where: { !$0.isUser }),
                   !viewModel.isThinking {
                    suggestedPromptsRow
                }

                Divider()
                    .foregroundStyle(AppTheme.adaptiveSurfaceVariant(colorScheme))

                // Input bar
                inputBar
            }
            .background(AppTheme.adaptiveBackground(colorScheme))
            .navigationTitle("AI Coach")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button("Clear Chat", systemImage: "trash") {
                            viewModel.clearChat()
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                            .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                    }
                }
            }
        }
        .onAppear {
            viewModel.load(modelContext: modelContext)
        }
    }

    // MARK: - Chat Messages

    private var chatMessages: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: AppSpacing.md) {
                    // Welcome header
                    if viewModel.chatHistory.count <= 1 {
                        welcomeHeader
                    }

                    ForEach(viewModel.chatHistory) { message in
                        ChatBubble(message: message)
                            .id(message.id)
                    }

                    if viewModel.isThinking {
                        thinkingIndicator
                            .id("thinking")
                    }
                }
                .padding(AppSpacing.lg)
            }
            .onChange(of: viewModel.chatHistory.count) { _, _ in
                withAnimation {
                    if let last = viewModel.chatHistory.last {
                        proxy.scrollTo(last.id, anchor: .bottom)
                    }
                }
            }
            .onChange(of: viewModel.isThinking) { _, isThinking in
                if isThinking {
                    withAnimation {
                        proxy.scrollTo("thinking", anchor: .bottom)
                    }
                }
            }
        }
    }

    // MARK: - Welcome Header

    private var welcomeHeader: some View {
        VStack(spacing: AppSpacing.lg) {
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [AppTheme.primary, AppTheme.primary.opacity(0.6)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 72, height: 72)
                Image(systemName: AppTheme.SportIcon.coach)
                    .font(.system(size: 30))
                    .foregroundStyle(.white)
            }

            VStack(spacing: AppSpacing.sm) {
                Text("Buddy - AI Coach")
                    .font(AppTypography.titleLarge)
                    .foregroundStyle(AppTheme.adaptiveOnSurface(colorScheme))

                Text("Ask me about workouts, nutrition, recovery, or training plans!")
                    .font(AppTypography.bodyMedium)
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, AppSpacing.xxl)
            }
        }
        .padding(.top, AppSpacing.xxl)
        .padding(.bottom, AppSpacing.lg)
    }

    // MARK: - Thinking Indicator

    private var thinkingIndicator: some View {
        HStack(alignment: .top, spacing: AppSpacing.sm) {
            // Avatar
            ZStack {
                Circle()
                    .fill(AppTheme.primary.opacity(0.15))
                    .frame(width: 32, height: 32)
                Image(systemName: AppTheme.SportIcon.coach)
                    .font(.caption)
                    .foregroundStyle(AppTheme.primary)
            }

            HStack(spacing: 6) {
                ForEach(0..<3, id: \.self) { i in
                    Circle()
                        .fill(AppTheme.adaptiveOnSurfaceVariant(colorScheme))
                        .frame(width: 8, height: 8)
                        .scaleEffect(1.0)
                        .animation(
                            .easeInOut(duration: 0.6)
                            .repeatForever()
                            .delay(Double(i) * 0.2),
                            value: viewModel.isThinking
                        )
                }
            }
            .padding(AppSpacing.md)
            .background(AppTheme.adaptiveSurfaceVariant(colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.medium))

            Spacer()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: - Suggested Prompts

    private var suggestedPromptsRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: AppSpacing.sm) {
                ForEach(viewModel.suggestedPrompts, id: \.self) { prompt in
                    Button {
                        viewModel.sendMessage(prompt)
                    } label: {
                        Text(prompt)
                            .font(AppTypography.labelSmall)
                            .foregroundStyle(AppTheme.primary)
                            .padding(.horizontal, AppSpacing.md)
                            .padding(.vertical, AppSpacing.sm)
                            .background(AppTheme.primary.opacity(0.1))
                            .clipShape(Capsule())
                    }
                }
            }
            .padding(.horizontal, AppSpacing.lg)
            .padding(.vertical, AppSpacing.sm)
        }
    }

    // MARK: - Input Bar

    private var inputBar: some View {
        HStack(spacing: AppSpacing.sm) {
            TextField("Ask me anything...", text: $viewModel.inputText, axis: .vertical)
                .font(AppTypography.bodyMedium)
                .lineLimit(1...4)
                .focused($isInputFocused)
                .onSubmit {
                    viewModel.sendMessage()
                }
                .padding(.horizontal, AppSpacing.lg)
                .padding(.vertical, AppSpacing.sm + 2)
                .background(AppTheme.adaptiveSurfaceVariant(colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: AppCornerRadius.pill))

            Button {
                viewModel.sendMessage()
                isInputFocused = false
            } label: {
                Image(systemName: "arrow.up.circle.fill")
                    .font(.system(size: 36))
                    .foregroundStyle(
                        viewModel.inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || viewModel.isThinking
                        ? AppTheme.adaptiveOnSurfaceVariant(colorScheme).opacity(0.3)
                        : AppTheme.primary
                    )
            }
            .disabled(viewModel.inputText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || viewModel.isThinking)
        }
        .padding(.horizontal, AppSpacing.lg)
        .padding(.vertical, AppSpacing.md)
    }
}

// MARK: - Chat Bubble

private struct ChatBubble: View {
    let message: CoachChatMessage
    @Environment(\.colorScheme) private var colorScheme

    private var timeString: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "h:mm a"
        return formatter.string(from: message.timestamp)
    }

    var body: some View {
        HStack(alignment: .top, spacing: AppSpacing.sm) {
            if message.isUser {
                Spacer(minLength: 60)
            } else {
                // Bot avatar
                ZStack {
                    Circle()
                        .fill(AppTheme.primary.opacity(0.15))
                        .frame(width: 32, height: 32)
                    Image(systemName: AppTheme.SportIcon.coach)
                        .font(.caption)
                        .foregroundStyle(AppTheme.primary)
                }
            }

            VStack(alignment: message.isUser ? .trailing : .leading, spacing: AppSpacing.xxs) {
                Text(message.text)
                    .font(AppTypography.bodyMedium)
                    .foregroundStyle(message.isUser ? .white : AppTheme.adaptiveOnSurface(colorScheme))
                    .padding(AppSpacing.md)
                    .background(
                        message.isUser
                        ? AppTheme.primary
                        : AppTheme.adaptiveSurfaceVariant(colorScheme)
                    )
                    .clipShape(
                        RoundedRectangle(cornerRadius: AppCornerRadius.medium)
                    )

                Text(timeString)
                    .font(AppTypography.captionSmall)
                    .foregroundStyle(AppTheme.adaptiveOnSurfaceVariant(colorScheme).opacity(0.6))
            }

            if !message.isUser {
                Spacer(minLength: 60)
            } else {
                // User avatar
                ZStack {
                    Circle()
                        .fill(AppTheme.primary)
                        .frame(width: 32, height: 32)
                    Image(systemName: "person.fill")
                        .font(.caption)
                        .foregroundStyle(.white)
                }
            }
        }
    }
}
