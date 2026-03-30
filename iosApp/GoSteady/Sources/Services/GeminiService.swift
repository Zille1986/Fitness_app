import Foundation
import Observation
import UIKit

// MARK: - Gemini Configuration

private enum GeminiConfig {
    static var apiKey: String {
        if let key = Bundle.main.object(forInfoDictionaryKey: "GEMINI_API_KEY") as? String, !key.isEmpty {
            return key
        }
        if let key = ProcessInfo.processInfo.environment["GEMINI_API_KEY"] {
            return key
        }
        return ""
    }
    static let model = "gemini-2.5-flash"
    static let baseURL = "https://generativelanguage.googleapis.com/v1beta/models"
}

// MARK: - Result Types

enum FormAnalysisResult {
    case success(FormAnalysis)
    case error(String)
}

struct FormAnalysis: Equatable {
    let exerciseDetected: String
    let exerciseCategory: String
    let repCount: Int
    let overallScore: Int
    let formQuality: String
    let issues: [FormIssue]
    let positiveFeedback: [String]
    let keyTips: [String]
    let confidence: String
}

struct FormIssue: Equatable, Identifiable {
    let id = UUID()
    let type: String
    let severity: String
    let title: String
    let description: String
    let correction: String
}

enum BodyAnalysisResult {
    case success(BodyAnalysis)
    case error(String)
}

struct BodyAnalysis: Equatable {
    let bodyType: String
    let estimatedBodyFatPercentage: Double
    let overallScore: Int
    let postureAssessment: GeminiPostureAssessment
    let focusZones: [FocusZone]
    let trainingRecommendations: [String]
    let nutritionRecommendations: [String]
    let confidence: String
    let notes: String
}

struct GeminiPostureAssessment: Equatable {
    let overallPosture: String
    let issues: [PostureIssueItem]
}

struct PostureIssueItem: Equatable, Identifiable {
    let id = UUID()
    let type: String
    let severity: String
    let description: String
}

struct FocusZone: Equatable, Identifiable {
    let id = UUID()
    let zone: String
    let priority: String
    let currentDevelopment: String
    let recommendation: String
}

enum FoodAnalysisResult {
    case success(FoodAnalysis)
    case error(String)
}

struct FoodAnalysis: Equatable {
    let foodName: String
    let description: String
    let portionSize: String
    let calories: Int
    let proteinGrams: Double
    let carbsGrams: Double
    let fatGrams: Double
    let fiberGrams: Double
    let confidence: String
    let itemsDetected: [String]
    let suggestions: String
}

struct TrainingPlanAI: Equatable, Identifiable {
    let id = UUID()
    let planName: String
    let description: String
    let totalWeeks: Int
    let weeks: [TrainingWeek]
}

struct TrainingWeek: Equatable, Identifiable {
    let id = UUID()
    let weekNumber: Int
    let focus: String
    let totalKm: Double
    let workouts: [GeminiPlannedWorkout]
}

struct GeminiPlannedWorkout: Equatable, Identifiable {
    let id = UUID()
    let dayOfWeek: String
    let type: String
    let distanceKm: Double
    let description: String
}

struct ChatMessage: Identifiable, Equatable {
    let id = UUID()
    let role: String // "user" or "model"
    let content: String
    let timestamp: Date = .now
}

// MARK: - Gemini Service

@Observable
final class GeminiService {

    var isProcessing: Bool = false
    var lastError: String?
    var chatHistory: [ChatMessage] = []

    private let session = URLSession.shared

    // MARK: - Form Analysis

    func analyzeExerciseForm(image: UIImage, exerciseName: String? = nil) async -> FormAnalysisResult {
        isProcessing = true
        defer { isProcessing = false }

        guard let base64Image = image.jpegData(compressionQuality: 0.8)?.base64EncodedString() else {
            return .error("Failed to encode image")
        }

        let exerciseContext: String
        if let name = exerciseName {
            exerciseContext = "Exercise being performed: \(name)\n\nAnalyze the form shown and provide detailed feedback."
        } else {
            exerciseContext = "First, identify what exercise is being performed, then analyze the form."
        }

        let prompt = """
        You are an expert fitness coach. \(exerciseContext)

        Return ONLY a valid JSON object (no markdown, no explanation):
        {
            "exercise_detected": "Name of the exercise",
            "exercise_category": "STRENGTH" or "CARDIO" or "FLEXIBILITY" or "UNKNOWN",
            "rep_count": estimated number of reps visible (integer, 0 if not applicable),
            "overall_score": 0-100 score for form quality,
            "form_quality": "EXCELLENT" or "GOOD" or "FAIR" or "POOR",
            "issues": [
                {
                    "type": "DEPTH|KNEE_TRACKING|BACK_POSITION|HIP_POSITION|ELBOW_POSITION|SHOULDER_POSITION|FOOT_POSITION|BAR_PATH|LOCKOUT|HEAD_POSITION|WRIST_POSITION|POSTURE|BALANCE|TEMPO|RANGE_OF_MOTION",
                    "severity": "LOW" or "MEDIUM" or "HIGH",
                    "title": "Short issue title",
                    "description": "Detailed description of what's wrong",
                    "correction": "How to fix this issue"
                }
            ],
            "positive_feedback": ["What they're doing well 1", "What they're doing well 2"],
            "key_tips": ["Tip 1", "Tip 2", "Tip 3"],
            "confidence": "HIGH" or "MEDIUM" or "LOW"
        }

        Be specific and actionable with feedback. If the image is unclear, set confidence to "LOW".
        """

        do {
            let responseText = try await sendImageRequest(base64Image: base64Image, mimeType: "image/jpeg", prompt: prompt)
            return parseFormAnalysis(responseText)
        } catch {
            let msg = error.localizedDescription
            lastError = msg
            return .error(msg)
        }
    }

    private func parseFormAnalysis(_ text: String) -> FormAnalysisResult {
        guard let json = extractJSON(from: text) else {
            return .error("Could not parse response")
        }

        let issues = (json["issues"] as? [[String: Any]] ?? []).compactMap { issueDict -> FormIssue? in
            guard let type = issueDict["type"] as? String,
                  let severity = issueDict["severity"] as? String,
                  let title = issueDict["title"] as? String,
                  let desc = issueDict["description"] as? String,
                  let correction = issueDict["correction"] as? String else { return nil }
            return FormIssue(type: type, severity: severity, title: title, description: desc, correction: correction)
        }

        return .success(FormAnalysis(
            exerciseDetected: json["exercise_detected"] as? String ?? "Unknown",
            exerciseCategory: json["exercise_category"] as? String ?? "UNKNOWN",
            repCount: json["rep_count"] as? Int ?? 0,
            overallScore: json["overall_score"] as? Int ?? 70,
            formQuality: json["form_quality"] as? String ?? "FAIR",
            issues: issues,
            positiveFeedback: json["positive_feedback"] as? [String] ?? [],
            keyTips: json["key_tips"] as? [String] ?? [],
            confidence: json["confidence"] as? String ?? "MEDIUM"
        ))
    }

    // MARK: - Body Composition Analysis

    func analyzeBodyComposition(image: UIImage, goal: String) async -> BodyAnalysisResult {
        isProcessing = true
        defer { isProcessing = false }

        guard let base64Image = image.jpegData(compressionQuality: 0.8)?.base64EncodedString() else {
            return .error("Failed to encode image")
        }

        let prompt = """
        Analyze this body/physique photo for fitness assessment purposes.
        The user's goal is: \(goal)

        Please respond in the following JSON format ONLY (no other text):
        {
            "body_type": "ECTOMORPH" or "MESOMORPH" or "ENDOMORPH" or "ECTO_MESOMORPH" or "ENDO_MESOMORPH",
            "estimated_body_fat_percentage": number between 5-45,
            "overall_score": number between 0-100,
            "posture_assessment": {
                "overall_posture": "EXCELLENT" or "GOOD" or "FAIR" or "POOR",
                "issues": [
                    {
                        "type": "FORWARD_HEAD|ROUNDED_SHOULDERS|ANTERIOR_PELVIC_TILT|POSTERIOR_PELVIC_TILT|KYPHOSIS|LORDOSIS|SCOLIOSIS",
                        "severity": "MILD" or "MODERATE" or "SEVERE",
                        "description": "Brief description"
                    }
                ]
            },
            "focus_zones": [
                {
                    "zone": "CHEST|BACK|SHOULDERS|ARMS|CORE|GLUTES|QUADS|HAMSTRINGS|CALVES",
                    "priority": "HIGH" or "MEDIUM" or "LOW",
                    "current_development": "UNDERDEVELOPED" or "AVERAGE" or "WELL_DEVELOPED",
                    "recommendation": "Specific advice"
                }
            ],
            "recommendations": {
                "training": ["Rec 1", "Rec 2"],
                "nutrition": ["Rec 1", "Rec 2"]
            },
            "confidence": "HIGH" or "MEDIUM" or "LOW",
            "notes": "Additional observations"
        }

        Be realistic and constructive. If the image is unclear, set confidence to "LOW".
        """

        do {
            let responseText = try await sendImageRequest(base64Image: base64Image, mimeType: "image/jpeg", prompt: prompt)
            return parseBodyAnalysis(responseText)
        } catch {
            let msg = error.localizedDescription
            lastError = msg
            return .error(msg)
        }
    }

    private func parseBodyAnalysis(_ text: String) -> BodyAnalysisResult {
        guard let json = extractJSON(from: text) else {
            return .error("Could not parse response")
        }

        let postureJSON = json["posture_assessment"] as? [String: Any] ?? [:]
        let postureIssues = (postureJSON["issues"] as? [[String: Any]] ?? []).compactMap { dict -> PostureIssueItem? in
            guard let type = dict["type"] as? String,
                  let severity = dict["severity"] as? String,
                  let desc = dict["description"] as? String else { return nil }
            return PostureIssueItem(type: type, severity: severity, description: desc)
        }

        let focusZones = (json["focus_zones"] as? [[String: Any]] ?? []).compactMap { dict -> FocusZone? in
            guard let zone = dict["zone"] as? String,
                  let priority = dict["priority"] as? String,
                  let dev = dict["current_development"] as? String,
                  let rec = dict["recommendation"] as? String else { return nil }
            return FocusZone(zone: zone, priority: priority, currentDevelopment: dev, recommendation: rec)
        }

        let recsJSON = json["recommendations"] as? [String: Any] ?? [:]

        return .success(BodyAnalysis(
            bodyType: json["body_type"] as? String ?? "MESOMORPH",
            estimatedBodyFatPercentage: json["estimated_body_fat_percentage"] as? Double ?? 20,
            overallScore: json["overall_score"] as? Int ?? 70,
            postureAssessment: GeminiPostureAssessment(
                overallPosture: postureJSON["overall_posture"] as? String ?? "GOOD",
                issues: postureIssues
            ),
            focusZones: focusZones,
            trainingRecommendations: recsJSON["training"] as? [String] ?? [],
            nutritionRecommendations: recsJSON["nutrition"] as? [String] ?? [],
            confidence: json["confidence"] as? String ?? "MEDIUM",
            notes: json["notes"] as? String ?? ""
        ))
    }

    // MARK: - Food Analysis

    func analyzeFoodImage(image: UIImage) async -> FoodAnalysisResult {
        isProcessing = true
        defer { isProcessing = false }

        guard let base64Image = image.jpegData(compressionQuality: 0.8)?.base64EncodedString() else {
            return .error("Failed to encode image")
        }

        let prompt = """
        Analyze this food image and estimate the nutritional content.

        Please respond in the following JSON format ONLY (no other text):
        {
            "food_name": "Name of the food/meal",
            "description": "Brief description of what you see",
            "portion_size": "Estimated portion size (e.g., '1 cup', '200g')",
            "calories": estimated_calories_as_number,
            "protein_grams": estimated_protein_as_number,
            "carbs_grams": estimated_carbs_as_number,
            "fat_grams": estimated_fat_as_number,
            "fiber_grams": estimated_fiber_as_number,
            "confidence": "HIGH" or "MEDIUM" or "LOW",
            "items_detected": ["item1", "item2"],
            "suggestions": "Any dietary suggestions or notes"
        }

        Be realistic with estimates. Round all numbers to whole integers.
        If you cannot identify the food clearly, set confidence to "LOW".
        """

        do {
            let responseText = try await sendImageRequest(base64Image: base64Image, mimeType: "image/jpeg", prompt: prompt)
            return parseFoodAnalysis(responseText)
        } catch {
            let msg = error.localizedDescription
            lastError = msg
            return .error(msg)
        }
    }

    private func parseFoodAnalysis(_ text: String) -> FoodAnalysisResult {
        guard let json = extractJSON(from: text) else {
            return .error("Could not parse response")
        }

        return .success(FoodAnalysis(
            foodName: json["food_name"] as? String ?? "Unknown Food",
            description: json["description"] as? String ?? "",
            portionSize: json["portion_size"] as? String ?? "1 serving",
            calories: (json["calories"] as? NSNumber)?.intValue ?? 0,
            proteinGrams: (json["protein_grams"] as? NSNumber)?.doubleValue ?? 0,
            carbsGrams: (json["carbs_grams"] as? NSNumber)?.doubleValue ?? 0,
            fatGrams: (json["fat_grams"] as? NSNumber)?.doubleValue ?? 0,
            fiberGrams: (json["fiber_grams"] as? NSNumber)?.doubleValue ?? 0,
            confidence: json["confidence"] as? String ?? "MEDIUM",
            itemsDetected: json["items_detected"] as? [String] ?? [],
            suggestions: json["suggestions"] as? String ?? ""
        ))
    }

    // MARK: - Training Plan Generation

    func generateTrainingPlan(
        goalType: String,
        selectedDays: [String],
        currentWeeklyKm: Double,
        currentLongRunKm: Double,
        targetDistanceKm: Double
    ) async -> TrainingPlanAI? {
        isProcessing = true
        defer { isProcessing = false }

        let dayNames = selectedDays.joined(separator: ", ")

        let prompt = """
        You are an expert running coach. Create a personalized \(goalType) training plan.

        Runner Profile:
        - Current weekly mileage: \(currentWeeklyKm)km
        - Current longest run: \(currentLongRunKm)km
        - Available training days: \(dayNames) (\(selectedDays.count) days/week)
        - Goal: \(goalType) (\(targetDistanceKm)km race)

        Generate an 8-week training plan following these principles:
        1. Progressive overload (max 10% weekly increase)
        2. Include variety: easy runs, long runs, tempo runs, intervals
        3. Long run should be on the last training day of the week
        4. Include recovery weeks (every 3-4 weeks, reduce volume by 20-30%)
        5. Taper in the final 1-2 weeks

        Return the plan as JSON in this exact format:
        {
          "planName": "string",
          "description": "string",
          "totalWeeks": 8,
          "weeks": [
            {
              "weekNumber": 1,
              "focus": "string describing week focus",
              "totalKm": number,
              "workouts": [
                {
                  "dayOfWeek": "Monday|Tuesday|...|Sunday",
                  "type": "EASY_RUN|LONG_RUN|TEMPO_RUN|INTERVAL_TRAINING|RECOVERY_RUN",
                  "distanceKm": number,
                  "description": "specific workout details"
                }
              ]
            }
          ]
        }

        Only include workouts on: \(dayNames). Return ONLY valid JSON.
        """

        do {
            let responseText = try await sendTextRequest(prompt: prompt)
            return parseTrainingPlan(responseText)
        } catch {
            lastError = error.localizedDescription
            return nil
        }
    }

    private func parseTrainingPlan(_ text: String) -> TrainingPlanAI? {
        let cleaned = text
            .replacingOccurrences(of: "```json", with: "")
            .replacingOccurrences(of: "```", with: "")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        guard let data = cleaned.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }

        guard let weeksArray = json["weeks"] as? [[String: Any]] else { return nil }

        let weeks = weeksArray.compactMap { weekDict -> TrainingWeek? in
            guard let weekNumber = weekDict["weekNumber"] as? Int,
                  let focus = weekDict["focus"] as? String,
                  let totalKm = (weekDict["totalKm"] as? NSNumber)?.doubleValue,
                  let workoutsArray = weekDict["workouts"] as? [[String: Any]] else { return nil }

            let workouts = workoutsArray.compactMap { wDict -> GeminiPlannedWorkout? in
                guard let day = wDict["dayOfWeek"] as? String,
                      let type = wDict["type"] as? String,
                      let dist = (wDict["distanceKm"] as? NSNumber)?.doubleValue,
                      let desc = wDict["description"] as? String else { return nil }
                return GeminiPlannedWorkout(dayOfWeek: day, type: type, distanceKm: dist, description: desc)
            }

            return TrainingWeek(weekNumber: weekNumber, focus: focus, totalKm: totalKm, workouts: workouts)
        }

        return TrainingPlanAI(
            planName: json["planName"] as? String ?? "Training Plan",
            description: (json["description"] as? String ?? "AI-generated training plan") + " (AI-generated)",
            totalWeeks: json["totalWeeks"] as? Int ?? weeks.count,
            weeks: weeks
        )
    }

    // MARK: - AI Coaching Chat

    func chat(message: String) async -> String {
        isProcessing = true
        defer { isProcessing = false }

        chatHistory.append(ChatMessage(role: "user", content: message))

        let systemContext = """
        You are an expert fitness coach in the GoSteady app. You provide personalized advice on:
        - Running technique and training
        - Strength training and form
        - Nutrition and diet
        - Recovery and injury prevention
        - Training plan adjustments
        - Motivation and mindset

        Keep responses concise and actionable. Use metric units.
        """

        var contents: [[String: Any]] = [
            [
                "role": "user",
                "parts": [["text": systemContext]]
            ],
            [
                "role": "model",
                "parts": [["text": "I'm your GoSteady fitness coach. How can I help you today?"]]
            ]
        ]

        for msg in chatHistory {
            contents.append([
                "role": msg.role == "user" ? "user" : "model",
                "parts": [["text": msg.content]]
            ])
        }

        do {
            let responseText = try await sendChatRequest(contents: contents)
            chatHistory.append(ChatMessage(role: "model", content: responseText))
            return responseText
        } catch {
            let errorMsg = "Sorry, I couldn't process that request. Please try again."
            lastError = error.localizedDescription
            return errorMsg
        }
    }

    func clearChatHistory() {
        chatHistory.removeAll()
    }

    // MARK: - API Communication

    private func sendImageRequest(base64Image: String, mimeType: String, prompt: String) async throws -> String {
        let url = URL(string: "\(GeminiConfig.baseURL)/\(GeminiConfig.model):generateContent?key=\(GeminiConfig.apiKey)")!

        let requestBody: [String: Any] = [
            "contents": [
                [
                    "parts": [
                        [
                            "inline_data": [
                                "mime_type": mimeType,
                                "data": base64Image
                            ]
                        ],
                        ["text": prompt]
                    ]
                ]
            ],
            "generationConfig": [
                "temperature": 0.3,
                "topK": 32,
                "topP": 0.95,
                "maxOutputTokens": 2048
            ]
        ]

        return try await executeRequest(url: url, body: requestBody)
    }

    private func sendTextRequest(prompt: String) async throws -> String {
        let url = URL(string: "\(GeminiConfig.baseURL)/\(GeminiConfig.model):generateContent?key=\(GeminiConfig.apiKey)")!

        let requestBody: [String: Any] = [
            "contents": [
                [
                    "parts": [
                        ["text": prompt]
                    ]
                ]
            ],
            "generationConfig": [
                "temperature": 0.4,
                "topK": 32,
                "topP": 0.95,
                "maxOutputTokens": 4096
            ]
        ]

        return try await executeRequest(url: url, body: requestBody)
    }

    private func sendChatRequest(contents: [[String: Any]]) async throws -> String {
        let url = URL(string: "\(GeminiConfig.baseURL)/\(GeminiConfig.model):generateContent?key=\(GeminiConfig.apiKey)")!

        let requestBody: [String: Any] = [
            "contents": contents,
            "generationConfig": [
                "temperature": 0.7,
                "topK": 40,
                "topP": 0.95,
                "maxOutputTokens": 1024
            ]
        ]

        return try await executeRequest(url: url, body: requestBody)
    }

    private func executeRequest(url: URL, body: [String: Any]) async throws -> String {
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        request.timeoutInterval = 30

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw GeminiError.invalidResponse
        }

        guard httpResponse.statusCode == 200 else {
            let errorBody = String(data: data, encoding: .utf8) ?? "Unknown error"
            throw GeminiError.apiError(statusCode: httpResponse.statusCode, message: errorBody)
        }

        guard let responseJSON = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let candidates = responseJSON["candidates"] as? [[String: Any]],
              let firstCandidate = candidates.first,
              let content = firstCandidate["content"] as? [String: Any],
              let parts = content["parts"] as? [[String: Any]],
              let text = parts.first?["text"] as? String else {
            throw GeminiError.emptyResponse
        }

        return text
    }

    // MARK: - JSON Parsing Helper

    private func extractJSON(from text: String) -> [String: Any]? {
        guard let jsonStart = text.firstIndex(of: "{"),
              let jsonEnd = text.lastIndex(of: "}") else { return nil }

        let jsonString = String(text[jsonStart...jsonEnd])
        guard let data = jsonString.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return nil }

        return json
    }
}

// MARK: - Errors

enum GeminiError: LocalizedError {
    case invalidResponse
    case emptyResponse
    case apiError(statusCode: Int, message: String)
    case encodingFailed

    var errorDescription: String? {
        switch self {
        case .invalidResponse: return "Invalid response from Gemini"
        case .emptyResponse: return "Empty response from Gemini"
        case .apiError(let code, let msg): return "Gemini API error (\(code)): \(msg)"
        case .encodingFailed: return "Failed to encode image data"
        }
    }
}
