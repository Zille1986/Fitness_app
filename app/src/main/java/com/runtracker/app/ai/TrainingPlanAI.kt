package com.runtracker.app.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.runtracker.app.BuildConfig
import com.runtracker.shared.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.runtracker.shared.util.TimeUtils
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainingPlanAI @Inject constructor() {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    suspend fun generateTrainingPlan(
        goalType: GoalType,
        selectedDays: List<Int>,
        currentWeeklyKm: Double,
        currentLongRunKm: Double,
        targetRaceDate: Long? = null
    ): TrainingPlan? = withContext(Dispatchers.IO) {
        try {
            val dayNames = selectedDays.map { dayToName(it) }.joinToString(", ")
            val goalName = goalTypeToName(goalType)
            val targetDistance = getTargetDistance(goalType)
            
            val prompt = buildPrompt(
                goalName = goalName,
                targetDistanceKm = targetDistance,
                dayNames = dayNames,
                numDays = selectedDays.size,
                currentWeeklyKm = currentWeeklyKm,
                currentLongRunKm = currentLongRunKm
            )

            val response = generativeModel.generateContent(content { text(prompt) })
            val responseText = response.text ?: return@withContext null

            parseTrainingPlan(
                responseText = responseText,
                goalType = goalType,
                selectedDays = selectedDays,
                targetDistance = targetDistance
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun buildPrompt(
        goalName: String,
        targetDistanceKm: Double,
        dayNames: String,
        numDays: Int,
        currentWeeklyKm: Double,
        currentLongRunKm: Double
    ): String {
        return """
You are an expert running coach. Create a personalized ${goalName} training plan.

Runner Profile:
- Current weekly mileage: ${currentWeeklyKm}km
- Current longest run: ${currentLongRunKm}km
- Available training days: $dayNames ($numDays days/week)
- Goal: ${goalName} (${targetDistanceKm}km race)

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
          "dayOfWeek": "Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday",
          "type": "EASY_RUN|LONG_RUN|TEMPO_RUN|INTERVAL_TRAINING|RECOVERY_RUN",
          "distanceKm": number,
          "description": "string with specific workout details"
        }
      ]
    }
  ]
}

Important:
- Only include workouts on the specified days: $dayNames
- Start week 1 with approximately ${currentWeeklyKm}km total
- Long runs should start around ${currentLongRunKm}km and build up
- Be specific in descriptions (e.g., "6x800m at 5K pace with 400m jog recovery")
- Return ONLY valid JSON, no other text
""".trimIndent()
    }

    private fun parseTrainingPlan(
        responseText: String,
        goalType: GoalType,
        selectedDays: List<Int>,
        targetDistance: Double
    ): TrainingPlan? {
        try {
            // Extract JSON from response (handle markdown code blocks)
            val jsonText = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val json = JSONObject(jsonText)
            val planName = json.optString("planName", goalTypeToName(goalType))
            val description = json.optString("description", "AI-generated training plan")
            val totalWeeks = json.optInt("totalWeeks", 8)
            val weeksArray = json.getJSONArray("weeks")

            val workouts = mutableListOf<ScheduledWorkout>()

            for (w in 0 until weeksArray.length()) {
                val weekJson = weeksArray.getJSONObject(w)
                val weekNumber = weekJson.getInt("weekNumber")
                val workoutsArray = weekJson.getJSONArray("workouts")

                for (d in 0 until workoutsArray.length()) {
                    val workoutJson = workoutsArray.getJSONObject(d)
                    val dayName = workoutJson.getString("dayOfWeek")
                    val dayOfWeek = nameToDayOfWeek(dayName)
                    val type = workoutJson.getString("type")
                    val distanceKm = workoutJson.getDouble("distanceKm")
                    val workoutDescription = workoutJson.getString("description")

                    workouts.add(
                        ScheduledWorkout(
                            id = UUID.randomUUID().toString(),
                            dayOfWeek = dayOfWeek,
                            weekNumber = weekNumber,
                            workoutType = stringToWorkoutType(type),
                            targetDistanceMeters = distanceKm * 1000,
                            description = workoutDescription
                        )
                    )
                }
            }

            val startDate = System.currentTimeMillis()
            val endDate = startDate + (totalWeeks * TimeUtils.ONE_WEEK_MS)

            return TrainingPlan(
                name = planName,
                description = "$description (AI-generated)",
                goalType = goalType,
                targetDistance = targetDistance * 1000,
                startDate = startDate,
                endDate = endDate,
                weeklySchedule = workouts
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun dayToName(day: Int): String {
        return when (day) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Unknown"
        }
    }

    private fun nameToDayOfWeek(name: String): Int {
        return when (name.lowercase()) {
            "monday" -> Calendar.MONDAY
            "tuesday" -> Calendar.TUESDAY
            "wednesday" -> Calendar.WEDNESDAY
            "thursday" -> Calendar.THURSDAY
            "friday" -> Calendar.FRIDAY
            "saturday" -> Calendar.SATURDAY
            "sunday" -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }
    }

    private fun goalTypeToName(goalType: GoalType): String {
        return when (goalType) {
            GoalType.FIRST_5K -> "First 5K"
            GoalType.IMPROVE_5K -> "5K Improvement"
            GoalType.FIRST_10K -> "First 10K"
            GoalType.IMPROVE_10K -> "10K Improvement"
            GoalType.HALF_MARATHON -> "Half Marathon"
            GoalType.MARATHON -> "Marathon"
            GoalType.GENERAL_FITNESS -> "General Fitness"
            GoalType.WEIGHT_LOSS -> "Weight Loss Running"
            GoalType.CUSTOM -> "Custom Training"
        }
    }

    private fun getTargetDistance(goalType: GoalType): Double {
        return when (goalType) {
            GoalType.FIRST_5K, GoalType.IMPROVE_5K -> 5.0
            GoalType.FIRST_10K, GoalType.IMPROVE_10K -> 10.0
            GoalType.HALF_MARATHON -> 21.1
            GoalType.MARATHON -> 42.2
            else -> 10.0
        }
    }

    private fun stringToWorkoutType(type: String): WorkoutType {
        return when (type.uppercase()) {
            "EASY_RUN" -> WorkoutType.EASY_RUN
            "LONG_RUN" -> WorkoutType.LONG_RUN
            "TEMPO_RUN" -> WorkoutType.TEMPO_RUN
            "INTERVAL_TRAINING" -> WorkoutType.INTERVAL_TRAINING
            "RECOVERY_RUN" -> WorkoutType.RECOVERY_RUN
            "HILL_REPEATS" -> WorkoutType.HILL_REPEATS
            "RACE_PACE" -> WorkoutType.RACE_PACE
            "FARTLEK" -> WorkoutType.FARTLEK
            else -> WorkoutType.EASY_RUN
        }
    }
}
