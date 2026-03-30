import Foundation
import SwiftUI
import SwiftData

@Observable
final class AICoachViewModel {
    var chatHistory: [CoachChatMessage] = []
    var inputText: String = ""
    var isThinking: Bool = false
    var suggestedPrompts: [String] = AICoachPrompts.greetings
    var errorMessage: String? = nil

    private var modelContext: ModelContext?
    private var chatInitialized = false
    private let apiKey: String = {
        Bundle.main.infoDictionary?["GEMINI_API_KEY"] as? String ?? ""
    }()

    func load(modelContext: ModelContext) {
        self.modelContext = modelContext
        if chatHistory.isEmpty {
            let greeting = getTimeBasedGreeting()
            chatHistory.append(CoachChatMessage(
                text: "\(greeting) I'm your AI fitness coach. Ask me anything about workouts, nutrition, recovery, or training plans!",
                isUser: false
            ))
        }
    }

    func sendMessage(_ text: String? = nil) {
        let message = text ?? inputText
        guard !message.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }

        // Add user message
        chatHistory.append(CoachChatMessage(text: message, isUser: true))
        inputText = ""
        isThinking = true
        errorMessage = nil

        Task {
            do {
                let response = try await generateResponse(for: message)
                await MainActor.run {
                    chatHistory.append(CoachChatMessage(text: response, isUser: false))
                    suggestedPrompts = getContextualPrompts(for: message)
                    isThinking = false
                }
            } catch {
                await MainActor.run {
                    chatHistory.append(CoachChatMessage(
                        text: "Sorry, I had trouble thinking about that. Please try again.",
                        isUser: false
                    ))
                    isThinking = false
                    errorMessage = error.localizedDescription
                }
            }
        }
    }

    func clearChat() {
        chatHistory.removeAll()
        chatInitialized = false
        suggestedPrompts = AICoachPrompts.greetings

        let greeting = getTimeBasedGreeting()
        chatHistory.append(CoachChatMessage(
            text: "\(greeting) Chat cleared! What would you like to know?",
            isUser: false
        ))
    }

    // MARK: - Private

    private func generateResponse(for question: String) async throws -> String {
        let context = await buildUserContext()
        let systemPrompt = """
        You are Buddy, an expert fitness coach built into the GoSteady fitness app. You have deep knowledge of exercise science, nutrition, and sports psychology.

        Guidelines:
        - Give specific, actionable advice based on the user's actual data
        - Be honest and constructive
        - Adapt response length to the question
        - Use emojis sparingly

        User's fitness data:
        \(context)

        Current date: \(DateFormatter.localizedString(from: Date(), dateStyle: .full, timeStyle: .none))
        """

        let requestBody: [String: Any] = [
            "contents": [
                ["role": "user", "parts": [["text": systemPrompt]]],
                ["role": "model", "parts": [["text": "Got it! I have the user's fitness profile loaded. I'm ready to help."]]],
                ["role": "user", "parts": [["text": question]]]
            ],
            "generationConfig": [
                "temperature": 0.7,
                "maxOutputTokens": 1024
            ]
        ]

        guard !apiKey.isEmpty else {
            return generateOfflineResponse(for: question)
        }

        let url = URL(string: "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=\(apiKey)")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: requestBody)

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            return generateOfflineResponse(for: question)
        }

        if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
           let candidates = json["candidates"] as? [[String: Any]],
           let content = candidates.first?["content"] as? [String: Any],
           let parts = content["parts"] as? [[String: Any]],
           let text = parts.first?["text"] as? String {
            return text
        }

        return generateOfflineResponse(for: question)
    }

    private func generateOfflineResponse(for question: String) -> String {
        let q = question.lowercased()
        if q.contains("today") || q.contains("workout") || q.contains("train") {
            return "Based on your recent activity, I'd suggest a moderate session today. Listen to your body -- if you're feeling fresh, push a bit harder. If you're tired, an easy recovery session works great too. What type of workout are you thinking?"
        } else if q.contains("nutrition") || q.contains("eat") || q.contains("food") || q.contains("diet") {
            return "For optimal performance, aim for about 1.6-2.0g of protein per kg of bodyweight, with complex carbs before workouts and a mix of protein + carbs within 30 minutes after. Stay hydrated throughout the day -- at least 2-3 liters of water."
        } else if q.contains("rest") || q.contains("recovery") || q.contains("sleep") {
            return "Recovery is where the gains happen! Aim for 7-9 hours of quality sleep, incorporate light mobility work on rest days, and consider foam rolling for 10-15 minutes. If your readiness score is low, prioritize an easy day."
        } else if q.contains("progress") || q.contains("improve") || q.contains("better") {
            return "Consistency is key. Track your workouts, progressively increase volume or intensity by about 10% per week, and make sure you're recovering properly. Small improvements compound into big results over time."
        } else {
            return "That's a great question! I'd recommend focusing on consistency with your training, eating well, and getting enough recovery. Would you like specific advice about workouts, nutrition, or recovery?"
        }
    }

    @MainActor
    private func buildUserContext() async -> String {
        guard let modelContext else { return "User is actively tracking workouts." }

        var parts: [String] = []

        let profileDescriptor = FetchDescriptor<UserProfile>()
        if let profile = try? modelContext.fetch(profileDescriptor).first {
            var info: [String] = []
            if !profile.name.isEmpty { info.append("Name: \(profile.name)") }
            if let age = profile.age { info.append("Age: \(age)") }
            if let weight = profile.weight { info.append("Weight: \(weight)kg") }
            if let height = profile.height { info.append("Height: \(height)cm") }
            if !info.isEmpty { parts.append("PROFILE: \(info.joined(separator: ", "))") }
        }

        var runDescriptor = FetchDescriptor<Run>(
            predicate: #Predicate<Run> { $0.isCompleted }
        )
        runDescriptor.sortBy = [SortDescriptor(\.startTime, order: .reverse)]
        runDescriptor.fetchLimit = 5
        let runs = (try? modelContext.fetch(runDescriptor)) ?? []
        if !runs.isEmpty {
            let lastRun = runs.first!
            parts.append("RECENT RUNS: \(runs.count) runs, last was \(String(format: "%.1f", lastRun.distanceKm))km")
        }

        var gymDescriptor = FetchDescriptor<GymWorkout>(
            predicate: #Predicate<GymWorkout> { $0.isCompleted }
        )
        gymDescriptor.sortBy = [SortDescriptor(\.startTime, order: .reverse)]
        gymDescriptor.fetchLimit = 5
        let gymWorkouts = (try? modelContext.fetch(gymDescriptor)) ?? []
        if !gymWorkouts.isEmpty {
            parts.append("RECENT GYM: \(gymWorkouts.count) sessions, last was \(gymWorkouts.first!.name)")
        }

        return parts.joined(separator: "\n")
    }

    private func getTimeBasedGreeting() -> String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 0..<12: return "Good morning!"
        case 12..<17: return "Good afternoon!"
        default: return "Good evening!"
        }
    }

    private func getContextualPrompts(for lastQuestion: String) -> [String] {
        let q = lastQuestion.lowercased()
        if q.contains("workout") || q.contains("train") {
            return ["What muscles should I hit?", "I'm tired today", "Quick 20 min session?"]
        } else if q.contains("nutrition") || q.contains("food") || q.contains("eat") {
            return ["Meal ideas", "How much protein?", "Pre-workout meal?"]
        } else if q.contains("injury") || q.contains("pain") {
            return ["What can I still do?", "Recovery tips", "Should I rest?"]
        } else {
            return ["What should I do today?", "Review my week", "Nutrition check"]
        }
    }
}

// MARK: - Chat Message

struct CoachChatMessage: Identifiable {
    let id = UUID()
    let text: String
    let isUser: Bool
    let timestamp: Date = Date()
}

// MARK: - Prompts

enum AICoachPrompts {
    static let greetings = [
        "What should I train today?",
        "How's my week looking?",
        "Help me plan my week"
    ]

    static let afterWorkout = [
        "How did that compare to last time?",
        "What should I eat now?",
        "When's my next session?"
    ]
}
