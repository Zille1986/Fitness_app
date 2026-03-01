package com.runtracker.app.ai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.runtracker.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiFoodAnalyzer @Inject constructor() {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.4f
                topK = 32
                topP = 1f
                maxOutputTokens = 1024
            }
        )
    }

    suspend fun analyzeFoodImage(bitmap: Bitmap): FoodAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Analyze this food image and estimate the nutritional content.
                
                Please respond in the following JSON format ONLY (no other text):
                {
                    "food_name": "Name of the food/meal",
                    "description": "Brief description of what you see",
                    "portion_size": "Estimated portion size (e.g., '1 cup', '200g', '1 plate')",
                    "calories": estimated_calories_as_number,
                    "protein_grams": estimated_protein_as_number,
                    "carbs_grams": estimated_carbs_as_number,
                    "fat_grams": estimated_fat_as_number,
                    "fiber_grams": estimated_fiber_as_number,
                    "confidence": "high/medium/low",
                    "items_detected": ["item1", "item2"],
                    "suggestions": "Any dietary suggestions or notes"
                }
                
                Be realistic with estimates. If you cannot identify the food clearly, set confidence to "low".
                If multiple food items are present, estimate the total nutritional content.
                Round all numbers to whole integers.
            """.trimIndent()

            val response = generativeModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )

            val responseText = response.text ?: throw Exception("Empty response from Gemini")
            parseResponse(responseText)
        } catch (e: Exception) {
            FoodAnalysisResult.Error(e.message ?: "Failed to analyze image")
        }
    }

    private fun parseResponse(responseText: String): FoodAnalysisResult {
        return try {
            // Extract JSON from response (in case there's extra text)
            val jsonStart = responseText.indexOf('{')
            val jsonEnd = responseText.lastIndexOf('}') + 1
            
            if (jsonStart == -1 || jsonEnd == 0) {
                return FoodAnalysisResult.Error("Could not parse response")
            }
            
            val jsonString = responseText.substring(jsonStart, jsonEnd)
            val json = JSONObject(jsonString)
            
            FoodAnalysisResult.Success(
                foodName = json.optString("food_name", "Unknown Food"),
                description = json.optString("description", ""),
                portionSize = json.optString("portion_size", "1 serving"),
                calories = json.optInt("calories", 0),
                proteinGrams = json.optDouble("protein_grams", 0.0),
                carbsGrams = json.optDouble("carbs_grams", 0.0),
                fatGrams = json.optDouble("fat_grams", 0.0),
                fiberGrams = json.optDouble("fiber_grams", 0.0),
                confidence = Confidence.fromString(json.optString("confidence", "medium")),
                itemsDetected = parseStringArray(json.optJSONArray("items_detected")),
                suggestions = json.optString("suggestions", "")
            )
        } catch (e: Exception) {
            FoodAnalysisResult.Error("Failed to parse nutrition data: ${e.message}")
        }
    }

    private fun parseStringArray(jsonArray: org.json.JSONArray?): List<String> {
        if (jsonArray == null) return emptyList()
        return (0 until jsonArray.length()).map { jsonArray.getString(it) }
    }
}

sealed class FoodAnalysisResult {
    data class Success(
        val foodName: String,
        val description: String,
        val portionSize: String,
        val calories: Int,
        val proteinGrams: Double,
        val carbsGrams: Double,
        val fatGrams: Double,
        val fiberGrams: Double,
        val confidence: Confidence,
        val itemsDetected: List<String>,
        val suggestions: String
    ) : FoodAnalysisResult()
    
    data class Error(val message: String) : FoodAnalysisResult()
}

enum class Confidence {
    HIGH, MEDIUM, LOW;
    
    companion object {
        fun fromString(value: String): Confidence {
            return when (value.lowercase()) {
                "high" -> HIGH
                "low" -> LOW
                else -> MEDIUM
            }
        }
    }
}
