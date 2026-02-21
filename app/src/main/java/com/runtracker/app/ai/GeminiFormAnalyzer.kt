package com.runtracker.app.ai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.runtracker.app.BuildConfig
import com.runtracker.shared.ai.FormIssue
import com.runtracker.shared.ai.FormIssueType
import com.runtracker.shared.ai.IssueSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiFormAnalyzer @Inject constructor() {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.3f
                topK = 32
                topP = 0.95f
                maxOutputTokens = 2048
            }
        )
    }

    /**
     * Analyze exercise form with auto-detection of exercise type
     */
    suspend fun analyzeExerciseFormAutoDetect(
        frames: List<Bitmap>
    ): FormAnalysisAIResult = withContext(Dispatchers.IO) {
        try {
            if (frames.isEmpty()) {
                return@withContext FormAnalysisAIResult.Error("No frames to analyze")
            }

            android.util.Log.d("GeminiFormAnalyzer", "Auto-detecting exercise and analyzing form with ${frames.size} frames")

            val prompt = """
                You are an expert fitness coach. First, identify what exercise is being performed in these images, then analyze the form and provide detailed feedback.
                
                STEP 1: Identify the exercise. Common exercises include:
                - Squat, Deadlift, Bench Press, Overhead Press, Barbell Row
                - Push-Up, Pull-Up, Lunge, Plank
                - Running, Walking, Jumping
                - Any other recognizable exercise
                
                STEP 2: Analyze the form for that specific exercise.
                
                Return ONLY a valid JSON object (no markdown, no explanation):
                {
                    "exercise_detected": "Name of the exercise identified",
                    "exercise_category": "STRENGTH" or "CARDIO" or "FLEXIBILITY" or "UNKNOWN",
                    "rep_count": estimated number of reps visible (integer, 0 if not applicable),
                    "overall_score": 0-100 score for form quality,
                    "form_quality": "EXCELLENT" or "GOOD" or "FAIR" or "POOR",
                    "issues": [
                        {
                            "type": "One of: DEPTH, KNEE_TRACKING, BACK_POSITION, HIP_POSITION, ELBOW_POSITION, SHOULDER_POSITION, FOOT_POSITION, BAR_PATH, LOCKOUT, HEAD_POSITION, WRIST_POSITION, POSTURE, BALANCE, TEMPO, RANGE_OF_MOTION",
                            "severity": "LOW" or "MEDIUM" or "HIGH",
                            "title": "Short issue title",
                            "description": "Detailed description of what's wrong",
                            "correction": "How to fix this issue"
                        }
                    ],
                    "positive_feedback": ["What they're doing well 1", "What they're doing well 2"],
                    "key_tips": ["Most important tip 1", "Most important tip 2", "Most important tip 3"],
                    "confidence": "HIGH" or "MEDIUM" or "LOW"
                }
                
                If you cannot identify the exercise or the image is unclear, set exercise_detected to "Unknown" and confidence to "LOW".
                Be specific and actionable with feedback.
            """.trimIndent()

            val frameToAnalyze = if (frames.size == 1) frames[0] else frames[frames.size / 2]

            val response = generativeModel.generateContent(
                content {
                    image(frameToAnalyze)
                    text(prompt)
                }
            )

            val responseText = response.text ?: throw Exception("Empty response from Gemini")
            android.util.Log.d("GeminiFormAnalyzer", "Auto-detect response, length: ${responseText.length}")
            
            parseFormAnalysisResponse(responseText)
        } catch (e: Exception) {
            android.util.Log.e("GeminiFormAnalyzer", "Auto-detect error: ${e.message}", e)
            FormAnalysisAIResult.Error(e.message ?: "Failed to analyze form")
        }
    }

    suspend fun analyzeExerciseForm(
        frames: List<Bitmap>,
        exerciseName: String
    ): FormAnalysisAIResult = withContext(Dispatchers.IO) {
        try {
            if (frames.isEmpty()) {
                return@withContext FormAnalysisAIResult.Error("No frames to analyze")
            }

            android.util.Log.d("GeminiFormAnalyzer", "Analyzing $exerciseName with ${frames.size} frames")

            val prompt = """
                You are an expert fitness coach analyzing exercise form from video frames.
                
                Exercise being performed: $exerciseName
                
                Analyze the form shown in these ${frames.size} frame(s) and provide detailed feedback.
                
                Return ONLY a valid JSON object (no markdown, no explanation):
                {
                    "exercise_detected": "$exerciseName or the actual exercise if different",
                    "exercise_category": "STRENGTH" or "CARDIO" or "FLEXIBILITY",
                    "rep_count": estimated number of reps visible (integer),
                    "overall_score": 0-100 score for form quality,
                    "form_quality": "EXCELLENT" or "GOOD" or "FAIR" or "POOR",
                    "issues": [
                        {
                            "type": "One of: DEPTH, KNEE_TRACKING, BACK_POSITION, HIP_POSITION, ELBOW_POSITION, SHOULDER_POSITION, FOOT_POSITION, BAR_PATH, LOCKOUT, HEAD_POSITION, WRIST_POSITION, POSTURE",
                            "severity": "LOW" or "MEDIUM" or "HIGH",
                            "title": "Short issue title",
                            "description": "Detailed description of what's wrong",
                            "correction": "How to fix this issue"
                        }
                    ],
                    "positive_feedback": ["What they're doing well 1", "What they're doing well 2"],
                    "key_tips": ["Most important tip 1", "Most important tip 2", "Most important tip 3"],
                    "confidence": "HIGH" or "MEDIUM" or "LOW"
                }
                
                Be specific and actionable with feedback. If the image is unclear or doesn't show exercise form, set confidence to "LOW".
                
                Common issues to look for based on exercise:
                - Squats: depth, knee cave, back rounding, heel rise, forward lean
                - Push-ups: hip sag/pike, elbow flare, incomplete range of motion, head position
                - Deadlifts: back rounding, bar path, hip hinge, lockout
                - Lunges: knee tracking, torso lean, depth, balance
                - Planks: hip position, shoulder alignment, head position
                - Bench Press: bar path, elbow angle, arch, leg drive
                - Overhead Press: back arch, bar path, lockout
                - Rows: back position, elbow path, full range of motion
            """.trimIndent()

            // Use the middle frame for single-frame analysis, or multiple frames if available
            val frameToAnalyze = if (frames.size == 1) {
                frames[0]
            } else {
                frames[frames.size / 2] // Use middle frame
            }

            val response = generativeModel.generateContent(
                content {
                    image(frameToAnalyze)
                    text(prompt)
                }
            )

            val responseText = response.text ?: throw Exception("Empty response from Gemini")
            android.util.Log.d("GeminiFormAnalyzer", "Got response, length: ${responseText.length}")
            
            parseFormAnalysisResponse(responseText)
        } catch (e: Exception) {
            android.util.Log.e("GeminiFormAnalyzer", "Error: ${e.message}", e)
            FormAnalysisAIResult.Error(e.message ?: "Failed to analyze form")
        }
    }

    private fun parseFormAnalysisResponse(responseText: String): FormAnalysisAIResult {
        return try {
            val jsonStart = responseText.indexOf('{')
            val jsonEnd = responseText.lastIndexOf('}') + 1
            
            if (jsonStart == -1 || jsonEnd == 0) {
                return FormAnalysisAIResult.Error("Could not parse response")
            }
            
            val jsonString = responseText.substring(jsonStart, jsonEnd)
            val json = JSONObject(jsonString)
            
            // Parse issues
            val issues = mutableListOf<FormIssue>()
            json.optJSONArray("issues")?.let { issuesArray ->
                for (i in 0 until issuesArray.length()) {
                    val issueJson = issuesArray.getJSONObject(i)
                    try {
                        val typeStr = issueJson.optString("type", "POSTURE")
                        val issueType = try {
                            FormIssueType.valueOf(typeStr)
                        } catch (e: Exception) {
                            FormIssueType.POSTURE
                        }
                        
                        issues.add(
                            FormIssue(
                                type = issueType,
                                severity = try {
                                    IssueSeverity.valueOf(issueJson.optString("severity", "MEDIUM"))
                                } catch (e: Exception) {
                                    IssueSeverity.MEDIUM
                                },
                                title = issueJson.optString("title", "Form Issue"),
                                description = issueJson.optString("description", ""),
                                correction = issueJson.optString("correction", "")
                            )
                        )
                    } catch (e: Exception) {
                        // Skip invalid issues
                    }
                }
            }
            
            // Parse positive feedback
            val positiveFeedback = mutableListOf<String>()
            json.optJSONArray("positive_feedback")?.let { arr ->
                for (i in 0 until arr.length()) {
                    positiveFeedback.add(arr.getString(i))
                }
            }
            
            // Parse key tips
            val keyTips = mutableListOf<String>()
            json.optJSONArray("key_tips")?.let { arr ->
                for (i in 0 until arr.length()) {
                    keyTips.add(arr.getString(i))
                }
            }
            
            FormAnalysisAIResult.Success(
                exerciseDetected = json.optString("exercise_detected", "Unknown"),
                exerciseCategory = json.optString("exercise_category", "STRENGTH"),
                repCount = json.optInt("rep_count", 0),
                overallScore = json.optInt("overall_score", 70),
                formQuality = json.optString("form_quality", "FAIR"),
                issues = issues,
                positiveFeedback = positiveFeedback,
                keyTips = keyTips,
                confidence = json.optString("confidence", "MEDIUM")
            )
        } catch (e: Exception) {
            android.util.Log.e("GeminiFormAnalyzer", "Parse error: ${e.message}", e)
            FormAnalysisAIResult.Error("Failed to parse form analysis: ${e.message}")
        }
    }
}

sealed class FormAnalysisAIResult {
    data class Success(
        val exerciseDetected: String,
        val exerciseCategory: String = "STRENGTH",
        val repCount: Int,
        val overallScore: Int,
        val formQuality: String,
        val issues: List<FormIssue>,
        val positiveFeedback: List<String>,
        val keyTips: List<String>,
        val confidence: String
    ) : FormAnalysisAIResult()
    
    data class Error(val message: String) : FormAnalysisAIResult()
}
