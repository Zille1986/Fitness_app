package com.runtracker.app.ai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.runtracker.app.BuildConfig
import com.runtracker.shared.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiBodyAnalyzer @Inject constructor() {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.3f
                topK = 32
                topP = 0.95f
                maxOutputTokens = 2048
            }
        )
    }

    suspend fun analyzeBodyImage(bitmap: Bitmap, goal: FitnessGoal): BodyAnalysisAIResult = withContext(Dispatchers.IO) {
        try {
            val goalContext = when (goal) {
                FitnessGoal.LOSE_WEIGHT -> "The user's goal is weight loss. Focus on areas where fat reduction would be beneficial."
                FitnessGoal.BUILD_MUSCLE -> "The user's goal is muscle gain. Focus on muscle development and areas that could use more mass."
                FitnessGoal.BODY_RECOMPOSITION -> "The user's goal is body recomposition (losing fat while gaining muscle)."
                FitnessGoal.ATHLETIC_PERFORMANCE -> "The user's goal is athletic performance. Focus on functional fitness and balanced development."
                FitnessGoal.GENERAL_FITNESS -> "The user's goal is general fitness and health."
                FitnessGoal.TONE_UP -> "The user's goal is to tone up and define muscles while reducing body fat."
                FitnessGoal.IMPROVE_ENDURANCE -> "The user's goal is to improve cardiovascular endurance."
                FitnessGoal.INCREASE_STRENGTH -> "The user's goal is to increase overall strength and power."
            }

            val prompt = """
                Analyze this body/physique photo for fitness assessment purposes.
                $goalContext
                
                Please respond in the following JSON format ONLY (no other text):
                {
                    "body_type": "ECTOMORPH" or "MESOMORPH" or "ENDOMORPH" or "ECTO_MESOMORPH" or "ENDO_MESOMORPH",
                    "estimated_body_fat_percentage": number between 5-45,
                    "overall_score": number between 0-100 based on fitness level and goal alignment,
                    "posture_assessment": {
                        "overall_posture": "EXCELLENT" or "GOOD" or "FAIR" or "POOR",
                        "issues": [
                            {
                                "type": "FORWARD_HEAD" or "ROUNDED_SHOULDERS" or "ANTERIOR_PELVIC_TILT" or "POSTERIOR_PELVIC_TILT" or "UNEVEN_HIPS" or "KYPHOSIS" or "LORDOSIS" or "SCOLIOSIS" or "KNEE_VALGUS" or "FLAT_FEET",
                                "severity": "MILD" or "MODERATE" or "SEVERE",
                                "description": "Brief description of the issue"
                            }
                        ]
                    },
                    "muscle_balance": {
                        "overall_balance": "EXCELLENT" or "GOOD" or "FAIR" or "POOR",
                        "left_right_symmetry": "EXCELLENT" or "GOOD" or "FAIR" or "POOR",
                        "upper_lower_balance": "EXCELLENT" or "GOOD" or "FAIR" or "POOR",
                        "imbalances": [
                            {
                                "area": "Description of imbalanced area",
                                "recommendation": "What to do about it"
                            }
                        ]
                    },
                    "focus_zones": [
                        {
                            "zone": "CHEST" or "BACK" or "SHOULDERS" or "ARMS" or "CORE" or "GLUTES" or "QUADS" or "HAMSTRINGS" or "CALVES",
                            "priority": "HIGH" or "MEDIUM" or "LOW",
                            "current_development": "UNDERDEVELOPED" or "AVERAGE" or "WELL_DEVELOPED" or "OVERDEVELOPED",
                            "recommendation": "Specific advice for this zone"
                        }
                    ],
                    "recommendations": {
                        "training": ["Training recommendation 1", "Training recommendation 2"],
                        "nutrition": ["Nutrition recommendation 1", "Nutrition recommendation 2"],
                        "lifestyle": ["Lifestyle recommendation 1"]
                    },
                    "confidence": "HIGH" or "MEDIUM" or "LOW",
                    "notes": "Any additional observations or notes"
                }
                
                Be realistic and constructive with your assessment. If the image quality is poor or the body is not clearly visible, set confidence to "LOW".
                Focus on providing actionable, helpful feedback aligned with the user's fitness goal.
            """.trimIndent()

            android.util.Log.d("GeminiBodyAnalyzer", "Sending request to Gemini...")
            
            val response = generativeModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )

            android.util.Log.d("GeminiBodyAnalyzer", "Got response from Gemini")
            val responseText = response.text ?: throw Exception("Empty response from Gemini")
            android.util.Log.d("GeminiBodyAnalyzer", "Response text length: ${responseText.length}")
            parseResponse(responseText)
        } catch (e: Exception) {
            android.util.Log.e("GeminiBodyAnalyzer", "Error: ${e.message}", e)
            BodyAnalysisAIResult.Error(e.message ?: "Failed to analyze image")
        }
    }

    private fun parseResponse(responseText: String): BodyAnalysisAIResult {
        return try {
            // Extract JSON from response
            val jsonStart = responseText.indexOf('{')
            val jsonEnd = responseText.lastIndexOf('}') + 1
            
            if (jsonStart == -1 || jsonEnd == 0) {
                return BodyAnalysisAIResult.Error("Could not parse response")
            }
            
            val jsonString = responseText.substring(jsonStart, jsonEnd)
            val json = JSONObject(jsonString)
            
            // Parse body type
            val bodyType = try {
                BodyType.valueOf(json.optString("body_type", "MESOMORPH"))
            } catch (e: Exception) {
                BodyType.MESOMORPH
            }
            
            // Parse posture assessment
            val postureJson = json.optJSONObject("posture_assessment")
            val postureLevel = try {
                PostureLevel.valueOf(postureJson?.optString("overall_posture", "GOOD") ?: "GOOD")
            } catch (e: Exception) {
                PostureLevel.GOOD
            }
            
            val postureIssues = mutableListOf<PostureIssue>()
            postureJson?.optJSONArray("issues")?.let { issuesArray ->
                for (i in 0 until issuesArray.length()) {
                    val issueJson = issuesArray.getJSONObject(i)
                    try {
                        val issueType = PostureIssueType.valueOf(issueJson.optString("type", "FORWARD_HEAD"))
                        postureIssues.add(
                            PostureIssue(
                                type = issueType,
                                severity = IssueSeverity.valueOf(issueJson.optString("severity", "MILD")),
                                description = issueJson.optString("description", ""),
                                exercises = getCorrectiveExercises(issueType)
                            )
                        )
                    } catch (e: Exception) {
                        // Skip invalid issues
                    }
                }
            }
            
            // Parse muscle balance
            val muscleJson = json.optJSONObject("muscle_balance")
            val muscleBalance = MuscleBalanceAssessment(
                overallBalance = try {
                    BalanceLevel.valueOf(muscleJson?.optString("overall_balance", "BALANCED") ?: "BALANCED")
                } catch (e: Exception) { BalanceLevel.BALANCED },
                leftRightSymmetry = try {
                    BalanceLevel.valueOf(muscleJson?.optString("left_right_symmetry", "BALANCED") ?: "BALANCED")
                } catch (e: Exception) { BalanceLevel.BALANCED },
                frontBackBalance = BalanceLevel.BALANCED,
                upperLowerBalance = try {
                    BalanceLevel.valueOf(muscleJson?.optString("upper_lower_balance", "BALANCED") ?: "BALANCED")
                } catch (e: Exception) { BalanceLevel.BALANCED },
                imbalances = parseImbalances(muscleJson?.optJSONArray("imbalances"))
            )
            
            // Parse focus zones as BodyZone list
            val focusZones = mutableListOf<BodyZone>()
            json.optJSONArray("focus_zones")?.let { zonesArray ->
                for (i in 0 until zonesArray.length()) {
                    val zoneJson = zonesArray.getJSONObject(i)
                    try {
                        val zoneName = zoneJson.optString("zone", "CORE")
                        val bodyZone = mapZoneNameToBodyZone(zoneName)
                        if (bodyZone != null && !focusZones.contains(bodyZone)) {
                            focusZones.add(bodyZone)
                        }
                    } catch (e: Exception) {
                        // Skip invalid zones
                    }
                }
            }
            
            // Parse recommendations
            val recsJson = json.optJSONObject("recommendations")
            val trainingRecs = parseStringArray(recsJson?.optJSONArray("training"))
            val nutritionRecs = parseStringArray(recsJson?.optJSONArray("nutrition"))
            val lifestyleRecs = parseStringArray(recsJson?.optJSONArray("lifestyle"))
            
            BodyAnalysisAIResult.Success(
                bodyType = bodyType,
                estimatedBodyFatPercentage = json.optDouble("estimated_body_fat_percentage", 20.0).toFloat(),
                overallScore = json.optInt("overall_score", 70),
                postureAssessment = PostureAssessment(
                    overallPosture = postureLevel,
                    issues = postureIssues
                ),
                muscleBalance = muscleBalance,
                focusZones = focusZones,
                trainingRecommendations = trainingRecs,
                nutritionRecommendations = nutritionRecs,
                lifestyleRecommendations = lifestyleRecs,
                confidence = try {
                    Confidence.valueOf(json.optString("confidence", "MEDIUM"))
                } catch (e: Exception) { Confidence.MEDIUM },
                notes = json.optString("notes", "")
            )
        } catch (e: Exception) {
            BodyAnalysisAIResult.Error("Failed to parse body analysis: ${e.message}")
        }
    }

    private fun parseImbalances(jsonArray: JSONArray?): List<MuscleImbalance> {
        if (jsonArray == null) return emptyList()
        val imbalances = mutableListOf<MuscleImbalance>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            imbalances.add(
                MuscleImbalance(
                    description = obj.optString("area", ""),
                    affectedZones = emptyList(),
                    correction = obj.optString("recommendation", "")
                )
            )
        }
        return imbalances
    }

    private fun parseStringArray(jsonArray: JSONArray?): List<String> {
        if (jsonArray == null) return emptyList()
        return (0 until jsonArray.length()).map { jsonArray.getString(it) }
    }
    
    private fun mapZoneNameToBodyZone(zoneName: String): BodyZone? {
        return when (zoneName.uppercase()) {
            "CHEST" -> BodyZone.UPPER_CHEST
            "BACK" -> BodyZone.UPPER_BACK
            "SHOULDERS" -> BodyZone.SHOULDERS
            "ARMS" -> BodyZone.BICEPS
            "CORE" -> BodyZone.CORE
            "GLUTES" -> BodyZone.GLUTES
            "QUADS" -> BodyZone.QUADS
            "HAMSTRINGS" -> BodyZone.HAMSTRINGS
            "CALVES" -> BodyZone.CALVES
            "ABS" -> BodyZone.ABS
            "LATS" -> BodyZone.LATS
            "TRICEPS" -> BodyZone.TRICEPS
            "BICEPS" -> BodyZone.BICEPS
            "FOREARMS" -> BodyZone.FOREARMS
            "OBLIQUES" -> BodyZone.OBLIQUES
            "LOWER_BACK" -> BodyZone.LOWER_BACK
            else -> null
        }
    }
    
    private fun getCorrectiveExercises(issueType: PostureIssueType): List<String> {
        return when (issueType) {
            PostureIssueType.FORWARD_HEAD -> listOf(
                "Chin tucks (3 sets of 10)",
                "Neck stretches",
                "Upper trapezius stretch",
                "Thoracic spine extensions"
            )
            PostureIssueType.ROUNDED_SHOULDERS -> listOf(
                "Wall angels (3 sets of 10)",
                "Face pulls (3 sets of 15)",
                "Doorway chest stretch",
                "Band pull-aparts",
                "Reverse flyes"
            )
            PostureIssueType.KYPHOSIS -> listOf(
                "Thoracic extensions on foam roller",
                "Cat-cow stretches",
                "Prone Y raises",
                "Seated rows with squeeze"
            )
            PostureIssueType.LORDOSIS -> listOf(
                "Dead bugs (3 sets of 10 each side)",
                "Glute bridges",
                "Hip flexor stretches",
                "Planks (hold 30-60 seconds)"
            )
            PostureIssueType.ANTERIOR_PELVIC_TILT -> listOf(
                "Hip flexor stretches (90/90 stretch)",
                "Glute bridges (3 sets of 15)",
                "Dead bugs (3 sets of 10 each side)",
                "Posterior pelvic tilt exercises",
                "RKC planks"
            )
            PostureIssueType.POSTERIOR_PELVIC_TILT -> listOf(
                "Hip hinge practice",
                "Romanian deadlifts",
                "Hamstring stretches",
                "Lumbar extension exercises"
            )
            PostureIssueType.SCOLIOSIS -> listOf(
                "Side planks (focus on weaker side)",
                "Bird dogs",
                "Cat-cow stretches",
                "Consult a physical therapist"
            )
            PostureIssueType.UNEVEN_SHOULDERS -> listOf(
                "Single-arm carries",
                "Unilateral rows",
                "Shoulder shrugs",
                "Upper trap stretches"
            )
            PostureIssueType.UNEVEN_HIPS -> listOf(
                "Single-leg glute bridges",
                "Clamshells",
                "Hip abductor strengthening",
                "IT band stretches"
            )
        }
    }
}

sealed class BodyAnalysisAIResult {
    data class Success(
        val bodyType: BodyType,
        val estimatedBodyFatPercentage: Float,
        val overallScore: Int,
        val postureAssessment: PostureAssessment,
        val muscleBalance: MuscleBalanceAssessment,
        val focusZones: List<BodyZone>,
        val trainingRecommendations: List<String>,
        val nutritionRecommendations: List<String>,
        val lifestyleRecommendations: List<String>,
        val confidence: Confidence,
        val notes: String
    ) : BodyAnalysisAIResult()
    
    data class Error(val message: String) : BodyAnalysisAIResult()
}
