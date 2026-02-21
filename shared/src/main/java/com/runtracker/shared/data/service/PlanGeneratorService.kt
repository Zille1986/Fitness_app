package com.runtracker.shared.data.service

import com.runtracker.shared.data.model.*
import java.util.*

/**
 * Service that generates personalized 3-month workout plans based on body scan results
 */
class PlanGeneratorService {

    /**
     * Generate a recommendation based on body scan results
     */
    fun generateRecommendation(bodyScan: BodyScan): PlanRecommendation {
        val focusZones = bodyScan.focusZones
        val goal = bodyScan.userGoal
        val bodyType = bodyScan.bodyType
        
        // Determine recommended days based on goal and body type
        val recommendedDays = when (goal) {
            FitnessGoal.LOSE_WEIGHT -> 5
            FitnessGoal.BUILD_MUSCLE -> 5
            FitnessGoal.TONE_UP -> 4
            FitnessGoal.IMPROVE_ENDURANCE -> 5
            FitnessGoal.INCREASE_STRENGTH -> 4
            FitnessGoal.GENERAL_FITNESS -> 4
            FitnessGoal.ATHLETIC_PERFORMANCE -> 5
            FitnessGoal.BODY_RECOMPOSITION -> 5
        }
        
        // Determine split type
        val split = when {
            goal == FitnessGoal.LOSE_WEIGHT -> TrainingSplit.CARDIO_STRENGTH
            goal == FitnessGoal.IMPROVE_ENDURANCE -> TrainingSplit.CARDIO_STRENGTH
            focusZones.size <= 3 -> TrainingSplit.FULL_BODY
            recommendedDays >= 5 -> TrainingSplit.PUSH_PULL_LEGS
            else -> TrainingSplit.UPPER_LOWER
        }
        
        // Determine running vs gym ratio
        val (runningDays, gymDays) = when (goal) {
            FitnessGoal.LOSE_WEIGHT -> Pair(3, 2)
            FitnessGoal.BUILD_MUSCLE -> Pair(1, 4)
            FitnessGoal.TONE_UP -> Pair(2, 2)
            FitnessGoal.IMPROVE_ENDURANCE -> Pair(4, 1)
            FitnessGoal.INCREASE_STRENGTH -> Pair(1, 3)
            FitnessGoal.GENERAL_FITNESS -> Pair(2, 2)
            FitnessGoal.ATHLETIC_PERFORMANCE -> Pair(2, 3)
            FitnessGoal.BODY_RECOMPOSITION -> Pair(2, 3)
        }
        
        // Estimate time per session
        val timePerSession = when (goal) {
            FitnessGoal.LOSE_WEIGHT -> 45
            FitnessGoal.BUILD_MUSCLE -> 60
            FitnessGoal.TONE_UP -> 45
            FitnessGoal.IMPROVE_ENDURANCE -> 50
            FitnessGoal.INCREASE_STRENGTH -> 60
            FitnessGoal.GENERAL_FITNESS -> 45
            FitnessGoal.ATHLETIC_PERFORMANCE -> 60
            FitnessGoal.BODY_RECOMPOSITION -> 50
        }
        
        val rationale = buildRationale(goal, bodyType, focusZones, split)
        
        return PlanRecommendation(
            recommendedDaysPerWeek = recommendedDays,
            recommendedSplit = split,
            runningDays = runningDays,
            gymDays = gymDays,
            focusAreas = focusZones,
            estimatedTimePerSession = timePerSession,
            rationale = rationale
        )
    }
    
    private fun buildRationale(
        goal: FitnessGoal,
        bodyType: BodyType,
        focusZones: List<BodyZone>,
        split: TrainingSplit
    ): String {
        val goalRationale = when (goal) {
            FitnessGoal.LOSE_WEIGHT -> "For weight loss, we'll combine cardio with strength training to maximize calorie burn while preserving muscle."
            FitnessGoal.BUILD_MUSCLE -> "To build muscle effectively, we'll focus on progressive overload with compound movements."
            FitnessGoal.TONE_UP -> "For toning, we'll balance moderate cardio with resistance training for definition."
            FitnessGoal.IMPROVE_ENDURANCE -> "To improve endurance, we'll prioritize running with supporting strength work."
            FitnessGoal.INCREASE_STRENGTH -> "For strength gains, we'll focus on heavy compound lifts with adequate recovery."
            FitnessGoal.GENERAL_FITNESS -> "For overall fitness, we'll create a balanced mix of cardio and strength."
            FitnessGoal.ATHLETIC_PERFORMANCE -> "For athletic performance, we'll combine sport-specific training with strength and conditioning."
            FitnessGoal.BODY_RECOMPOSITION -> "For body recomposition, we'll strategically combine strength training with cardio to lose fat while building muscle."
        }
        
        val zoneRationale = if (focusZones.isNotEmpty()) {
            " Your plan will emphasize ${focusZones.take(3).joinToString(", ") { it.displayName }}."
        } else ""
        
        return goalRationale + zoneRationale
    }

    /**
     * Generate a complete 3-month personalized plan
     */
    fun generatePlan(
        bodyScan: BodyScan,
        preferences: PlanPreferences
    ): PersonalizedPlan {
        val startDate = System.currentTimeMillis()
        val endDate = startDate + (90L * 24 * 60 * 60 * 1000) // 3 months
        
        // Create weekly schedule template
        val weeklySchedule = createWeeklySchedule(bodyScan, preferences)
        
        // Generate all workouts for 12 weeks
        val allWorkouts = generateAllWorkouts(bodyScan, preferences, weeklySchedule, startDate)
        
        val planName = "${bodyScan.userGoal.displayName} Plan"
        val description = "Personalized 3-month plan based on your body scan from ${
            java.text.SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(bodyScan.timestamp))
        }"
        
        return PersonalizedPlan(
            bodyScanId = bodyScan.id,
            name = planName,
            description = description,
            fitnessGoal = bodyScan.userGoal,
            startDate = startDate,
            endDate = endDate,
            userPreferences = preferences,
            weeklySchedule = weeklySchedule,
            allWorkouts = allWorkouts,
            totalWorkouts = allWorkouts.count { !it.workoutType.isRestDay() }
        )
    }
    
    private fun PlannedWorkoutType.isRestDay(): Boolean {
        return this == PlannedWorkoutType.REST || this == PlannedWorkoutType.ACTIVE_RECOVERY
    }
    
    private fun createWeeklySchedule(
        bodyScan: BodyScan,
        preferences: PlanPreferences
    ): List<PlannedDay> {
        val days = mutableListOf<PlannedDay>()
        val dayNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        
        // Use specific running/gym days if provided, otherwise fall back to legacy behavior
        val hasSpecificDays = preferences.runningDays.isNotEmpty() || preferences.gymDays.isNotEmpty()
        
        val workoutDays: List<Int>
        val runningDaysCount: Int
        val gymDaysCount: Int
        
        if (hasSpecificDays) {
            // Use the specific day assignments
            workoutDays = (preferences.runningDays + preferences.gymDays).distinct().sorted()
            runningDaysCount = preferences.runningDays.size
            gymDaysCount = preferences.gymDays.size
        } else {
            // Legacy behavior: distribute workouts across preferred days
            workoutDays = preferences.preferredDays.take(preferences.workoutDaysPerWeek)
            runningDaysCount = (preferences.workoutDaysPerWeek * preferences.runningToGymRatio).toInt()
                .coerceAtLeast(if (preferences.includeRunning) 1 else 0)
            gymDaysCount = preferences.workoutDaysPerWeek - runningDaysCount
        }
        
        val restDays = (1..7).filter { it !in workoutDays }
        
        // Assign workout types to days
        val workoutAssignments = assignWorkoutTypes(
            workoutDays = workoutDays,
            runningDaysCount = runningDaysCount,
            gymDaysCount = gymDaysCount,
            bodyScan = bodyScan,
            preferences = preferences
        )
        
        for (dayOfWeek in 1..7) {
            val isRest = dayOfWeek in restDays
            val workoutType = if (isRest) {
                if (preferences.includeRestDays) PlannedWorkoutType.REST else PlannedWorkoutType.ACTIVE_RECOVERY
            } else {
                workoutAssignments[dayOfWeek] ?: PlannedWorkoutType.REST
            }
            
            val focus = getFocusForWorkoutType(workoutType, bodyScan)
            val duration = preferences.availableTimePerDay[dayOfWeek] ?: 45
            
            days.add(PlannedDay(
                dayOfWeek = dayOfWeek,
                dayName = dayNames[dayOfWeek - 1],
                workoutType = workoutType,
                focus = focus,
                estimatedDuration = if (isRest) 0 else duration,
                isRestDay = isRest
            ))
        }
        
        return days
    }
    
    private fun assignWorkoutTypes(
        workoutDays: List<Int>,
        runningDaysCount: Int,
        gymDaysCount: Int,
        bodyScan: BodyScan,
        preferences: PlanPreferences
    ): Map<Int, PlannedWorkoutType> {
        val assignments = mutableMapOf<Int, PlannedWorkoutType>()
        val sortedDays = workoutDays.sorted()
        
        // Determine gym workout type based on goal
        val gymType = when (bodyScan.userGoal) {
            FitnessGoal.BUILD_MUSCLE -> PlannedWorkoutType.GYM_HYPERTROPHY
            FitnessGoal.INCREASE_STRENGTH -> PlannedWorkoutType.GYM_STRENGTH
            else -> PlannedWorkoutType.GYM_STRENGTH
        }
        
        // If specific days are provided, use them directly
        if (preferences.runningDays.isNotEmpty() || preferences.gymDays.isNotEmpty()) {
            for (day in preferences.runningDays) {
                assignments[day] = PlannedWorkoutType.RUNNING
            }
            for (day in preferences.gymDays) {
                assignments[day] = gymType
            }
            return assignments
        }
        
        // Legacy behavior: Alternate running and gym days for recovery
        var runningAssigned = 0
        var gymAssigned = 0
        
        for ((index, day) in sortedDays.withIndex()) {
            val shouldBeRunning = if (runningDaysCount == 0) false
            else if (gymDaysCount == 0) true
            else {
                // Alternate, but respect the ratio
                val runningRatio = runningDaysCount.toFloat() / (runningDaysCount + gymDaysCount)
                val currentRatio = if (runningAssigned + gymAssigned == 0) 0f 
                    else runningAssigned.toFloat() / (runningAssigned + gymAssigned)
                currentRatio < runningRatio
            }
            
            if (shouldBeRunning && runningAssigned < runningDaysCount) {
                assignments[day] = PlannedWorkoutType.RUNNING
                runningAssigned++
            } else if (gymAssigned < gymDaysCount) {
                assignments[day] = gymType
                gymAssigned++
            } else if (runningAssigned < runningDaysCount) {
                assignments[day] = PlannedWorkoutType.RUNNING
                runningAssigned++
            }
        }
        
        return assignments
    }
    
    private fun getFocusForWorkoutType(type: PlannedWorkoutType, bodyScan: BodyScan): String {
        return when (type) {
            PlannedWorkoutType.RUNNING -> "Cardio & Endurance"
            PlannedWorkoutType.GYM_STRENGTH -> {
                val zones = bodyScan.focusZones.take(2)
                if (zones.isNotEmpty()) zones.joinToString(" & ") { it.displayName }
                else "Full Body Strength"
            }
            PlannedWorkoutType.GYM_HYPERTROPHY -> {
                val zones = bodyScan.focusZones.take(2)
                if (zones.isNotEmpty()) zones.joinToString(" & ") { it.displayName }
                else "Muscle Building"
            }
            PlannedWorkoutType.HIIT -> "High Intensity Interval"
            PlannedWorkoutType.CARDIO -> "Cardiovascular"
            PlannedWorkoutType.FLEXIBILITY -> "Mobility & Flexibility"
            PlannedWorkoutType.ACTIVE_RECOVERY -> "Light Activity"
            PlannedWorkoutType.REST -> "Rest & Recovery"
        }
    }
    
    private fun generateAllWorkouts(
        bodyScan: BodyScan,
        preferences: PlanPreferences,
        weeklySchedule: List<PlannedDay>,
        startDate: Long
    ): List<PlannedWorkout> {
        val workouts = mutableListOf<PlannedWorkout>()
        val calendar = Calendar.getInstance().apply { timeInMillis = startDate }
        
        // Find the start of the week
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        
        for (week in 1..12) {
            for (day in weeklySchedule) {
                calendar.set(Calendar.DAY_OF_WEEK, day.dayOfWeek)
                val workoutDate = calendar.timeInMillis
                
                // Skip if date is before start date
                if (workoutDate < startDate) continue
                
                val workout = generateWorkoutForDay(
                    day = day,
                    weekNumber = week,
                    date = workoutDate,
                    bodyScan = bodyScan,
                    preferences = preferences
                )
                workouts.add(workout)
            }
            
            // Move to next week
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }
        
        return workouts.sortedBy { it.date }
    }
    
    private fun generateWorkoutForDay(
        day: PlannedDay,
        weekNumber: Int,
        date: Long,
        bodyScan: BodyScan,
        preferences: PlanPreferences
    ): PlannedWorkout {
        val exercises = when (day.workoutType) {
            PlannedWorkoutType.RUNNING -> generateRunningExercises(weekNumber, preferences, bodyScan.userGoal)
            PlannedWorkoutType.GYM_STRENGTH -> generateStrengthExercises(weekNumber, bodyScan, preferences)
            PlannedWorkoutType.GYM_HYPERTROPHY -> generateHypertrophyExercises(weekNumber, bodyScan, preferences)
            PlannedWorkoutType.HIIT -> generateHIITExercises(weekNumber, preferences)
            PlannedWorkoutType.CARDIO -> generateCardioExercises(weekNumber, preferences)
            PlannedWorkoutType.FLEXIBILITY -> generateFlexibilityExercises()
            PlannedWorkoutType.ACTIVE_RECOVERY -> generateActiveRecoveryExercises()
            PlannedWorkoutType.REST -> emptyList()
        }
        
        val title = generateWorkoutTitle(day.workoutType, weekNumber, day.dayName)
        val description = generateWorkoutDescription(day.workoutType, weekNumber, bodyScan.userGoal)
        
        return PlannedWorkout(
            id = UUID.randomUUID().toString(),
            date = date,
            dayOfWeek = day.dayOfWeek,
            weekNumber = weekNumber,
            workoutType = day.workoutType,
            title = title,
            description = description,
            estimatedDuration = day.estimatedDuration,
            exercises = exercises,
            targetZones = getTargetZonesForWorkout(day.workoutType, bodyScan)
        )
    }
    
    private fun generateWorkoutTitle(type: PlannedWorkoutType, week: Int, dayName: String): String {
        val phase = when {
            week <= 4 -> "Foundation"
            week <= 8 -> "Build"
            else -> "Peak"
        }
        
        return when (type) {
            PlannedWorkoutType.RUNNING -> "$phase Run - Week $week"
            PlannedWorkoutType.GYM_STRENGTH -> "$phase Strength - Week $week"
            PlannedWorkoutType.GYM_HYPERTROPHY -> "$phase Hypertrophy - Week $week"
            PlannedWorkoutType.HIIT -> "HIIT Session - Week $week"
            PlannedWorkoutType.CARDIO -> "Cardio - Week $week"
            PlannedWorkoutType.FLEXIBILITY -> "Mobility & Stretch"
            PlannedWorkoutType.ACTIVE_RECOVERY -> "Active Recovery"
            PlannedWorkoutType.REST -> "Rest Day"
        }
    }
    
    private fun generateWorkoutDescription(type: PlannedWorkoutType, week: Int, goal: FitnessGoal): String {
        val intensity = when {
            week <= 4 -> "moderate"
            week <= 8 -> "challenging"
            else -> "high"
        }
        
        return when (type) {
            PlannedWorkoutType.RUNNING -> "A $intensity run to build your cardiovascular base and support your ${goal.displayName.lowercase()} goal."
            PlannedWorkoutType.GYM_STRENGTH -> "Focus on compound movements with $intensity intensity to build functional strength."
            PlannedWorkoutType.GYM_HYPERTROPHY -> "Muscle-building session with $intensity volume to maximize growth."
            PlannedWorkoutType.HIIT -> "High-intensity intervals to boost metabolism and conditioning."
            PlannedWorkoutType.CARDIO -> "Steady-state cardio for heart health and calorie burn."
            PlannedWorkoutType.FLEXIBILITY -> "Improve mobility and prevent injury with stretching."
            PlannedWorkoutType.ACTIVE_RECOVERY -> "Light movement to promote recovery without stress."
            PlannedWorkoutType.REST -> "Complete rest to allow your body to recover and adapt."
        }
    }
    
    private fun generateRunningExercises(
        week: Int,
        preferences: PlanPreferences,
        goal: FitnessGoal
    ): List<PlannedExercise> {
        val exercises = mutableListOf<PlannedExercise>()
        val baseTime = preferences.availableTimePerDay.values.firstOrNull() ?: 30
        val fitnessMultiplier = preferences.fitnessLevel.multiplier
        
        // Progressive distance/time based on week
        val progressMultiplier = 1.0 + (week - 1) * 0.05 // 5% increase per week
        
        // Warmup
        exercises.add(PlannedExercise(
            id = UUID.randomUUID().toString(),
            name = "Warm-up Walk/Jog",
            exerciseType = PlannedExerciseType.WARMUP,
            duration = 5,
            notes = "Start easy to prepare your body"
        ))
        
        // Main run based on goal
        val mainRunDuration = ((baseTime - 10) * progressMultiplier * fitnessMultiplier).toInt()
        val runType = when {
            week % 4 == 0 -> PlannedExerciseType.LONG_RUN
            week % 3 == 0 -> PlannedExerciseType.TEMPO_RUN
            goal == FitnessGoal.IMPROVE_ENDURANCE -> PlannedExerciseType.EASY_RUN
            else -> PlannedExerciseType.EASY_RUN
        }
        
        exercises.add(PlannedExercise(
            id = UUID.randomUUID().toString(),
            name = when (runType) {
                PlannedExerciseType.LONG_RUN -> "Long Run"
                PlannedExerciseType.TEMPO_RUN -> "Tempo Run"
                else -> "Easy Run"
            },
            exerciseType = runType,
            duration = mainRunDuration,
            notes = when (runType) {
                PlannedExerciseType.LONG_RUN -> "Maintain a conversational pace"
                PlannedExerciseType.TEMPO_RUN -> "Comfortably hard - you can speak in short sentences"
                else -> "Keep it easy - you should be able to hold a conversation"
            }
        ))
        
        // Cooldown
        exercises.add(PlannedExercise(
            id = UUID.randomUUID().toString(),
            name = "Cool-down Walk",
            exerciseType = PlannedExerciseType.COOLDOWN,
            duration = 5,
            notes = "Gradually bring your heart rate down"
        ))
        
        return exercises
    }
    
    private fun generateStrengthExercises(
        week: Int,
        bodyScan: BodyScan,
        preferences: PlanPreferences
    ): List<PlannedExercise> {
        val exercises = mutableListOf<PlannedExercise>()
        
        // Use focus zones from scan, or default to a balanced full-body workout
        val focusZones = bodyScan.focusZones.ifEmpty { 
            listOf(BodyZone.QUADS, BodyZone.UPPER_BACK, BodyZone.SHOULDERS, BodyZone.CORE) 
        }
        
        // Progressive sets/reps based on week
        val sets = when {
            week <= 4 -> 3
            week <= 8 -> 4
            else -> 4
        }
        val reps = when {
            week <= 4 -> "8-10"
            week <= 8 -> "6-8"
            else -> "5-6"
        }
        
        // Warmup
        exercises.add(PlannedExercise(
            id = UUID.randomUUID().toString(),
            name = "Dynamic Warm-up",
            exerciseType = PlannedExerciseType.WARMUP,
            duration = 5,
            notes = "Arm circles, leg swings, bodyweight squats"
        ))
        
        // Add exercises based on focus zones
        val exerciseDatabase = getStrengthExerciseDatabase()
        val addedExercises = mutableSetOf<String>()
        
        for (zone in focusZones.take(4)) {
            val zoneExercises = exerciseDatabase[zone]
            if (zoneExercises != null && zoneExercises.isNotEmpty()) {
                val exercise = zoneExercises.firstOrNull { it !in addedExercises } ?: zoneExercises.first()
                addedExercises.add(exercise)
                exercises.add(PlannedExercise(
                    id = UUID.randomUUID().toString(),
                    name = exercise,
                    exerciseType = PlannedExerciseType.COMPOUND_STRENGTH,
                    sets = sets,
                    reps = reps,
                    restSeconds = 90,
                    targetZone = zone
                ))
            }
        }
        
        // If we didn't get enough exercises from focus zones, add some defaults
        if (exercises.size < 4) {
            val defaultExercises = listOf(
                Triple("Squats", BodyZone.QUADS, "Lower body compound"),
                Triple("Bench Press", BodyZone.UPPER_CHEST, "Upper body push"),
                Triple("Barbell Rows", BodyZone.UPPER_BACK, "Upper body pull"),
                Triple("Overhead Press", BodyZone.SHOULDERS, "Shoulder strength")
            )
            
            for ((name, zone, notes) in defaultExercises) {
                if (name !in addedExercises && exercises.size < 6) {
                    exercises.add(PlannedExercise(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        exerciseType = PlannedExerciseType.COMPOUND_STRENGTH,
                        sets = sets,
                        reps = reps,
                        restSeconds = 90,
                        targetZone = zone,
                        notes = notes
                    ))
                }
            }
        }
        
        // Core work
        exercises.add(PlannedExercise(
            id = UUID.randomUUID().toString(),
            name = "Plank",
            exerciseType = PlannedExerciseType.BODYWEIGHT,
            sets = 3,
            reps = "30-60 sec",
            restSeconds = 60,
            targetZone = BodyZone.CORE
        ))
        
        return exercises
    }
    
    private fun generateHypertrophyExercises(
        week: Int,
        bodyScan: BodyScan,
        preferences: PlanPreferences
    ): List<PlannedExercise> {
        val exercises = mutableListOf<PlannedExercise>()
        val focusZones = bodyScan.focusZones.ifEmpty {
            listOf(BodyZone.UPPER_CHEST, BodyZone.LATS, BodyZone.QUADS)
        }
        
        // Hypertrophy rep ranges
        val sets = when {
            week <= 4 -> 3
            week <= 8 -> 4
            else -> 4
        }
        val reps = "10-12"
        
        // Warmup
        exercises.add(PlannedExercise(
            id = UUID.randomUUID().toString(),
            name = "Light Cardio & Dynamic Stretching",
            exerciseType = PlannedExerciseType.WARMUP,
            duration = 8,
            notes = "5 min cardio + dynamic stretches"
        ))
        
        // Add exercises based on focus zones
        val exerciseDatabase = getHypertrophyExerciseDatabase()
        for (zone in focusZones.take(4)) {
            val zoneExercises = exerciseDatabase[zone] ?: continue
            // Add 2 exercises per zone for hypertrophy
            for (exercise in zoneExercises.take(2)) {
                exercises.add(PlannedExercise(
                    id = UUID.randomUUID().toString(),
                    name = exercise,
                    exerciseType = PlannedExerciseType.FREE_WEIGHT,
                    sets = sets,
                    reps = reps,
                    restSeconds = 60,
                    targetZone = zone
                ))
            }
        }
        
        return exercises
    }
    
    private fun generateHIITExercises(week: Int, preferences: PlanPreferences): List<PlannedExercise> {
        val exercises = mutableListOf<PlannedExercise>()
        
        exercises.add(PlannedExercise(
            id = UUID.randomUUID().toString(),
            name = "Warm-up",
            exerciseType = PlannedExerciseType.WARMUP,
            duration = 5
        ))
        
        val intervals = when {
            week <= 4 -> 6
            week <= 8 -> 8
            else -> 10
        }
        
        exercises.add(PlannedExercise(
            id = UUID.randomUUID().toString(),
            name = "HIIT Intervals",
            exerciseType = PlannedExerciseType.HIIT_CARDIO,
            sets = intervals,
            reps = "30 sec work / 30 sec rest",
            notes = "Burpees, mountain climbers, jump squats, high knees"
        ))
        
        exercises.add(PlannedExercise(
            id = UUID.randomUUID().toString(),
            name = "Cool-down",
            exerciseType = PlannedExerciseType.COOLDOWN,
            duration = 5
        ))
        
        return exercises
    }
    
    private fun generateCardioExercises(week: Int, preferences: PlanPreferences): List<PlannedExercise> {
        val duration = preferences.availableTimePerDay.values.firstOrNull() ?: 30
        
        return listOf(
            PlannedExercise(
                id = UUID.randomUUID().toString(),
                name = "Steady-State Cardio",
                exerciseType = PlannedExerciseType.RUNNING,
                duration = duration,
                notes = "Maintain moderate intensity - you should be able to talk"
            )
        )
    }
    
    private fun generateFlexibilityExercises(): List<PlannedExercise> {
        return listOf(
            PlannedExercise(
                id = UUID.randomUUID().toString(),
                name = "Full Body Stretch Routine",
                exerciseType = PlannedExerciseType.STRETCH,
                duration = 20,
                notes = "Hold each stretch for 30 seconds"
            ),
            PlannedExercise(
                id = UUID.randomUUID().toString(),
                name = "Foam Rolling",
                exerciseType = PlannedExerciseType.MOBILITY,
                duration = 10,
                notes = "Focus on tight areas"
            )
        )
    }
    
    private fun generateActiveRecoveryExercises(): List<PlannedExercise> {
        return listOf(
            PlannedExercise(
                id = UUID.randomUUID().toString(),
                name = "Light Walk or Yoga",
                exerciseType = PlannedExerciseType.YOGA,
                duration = 20,
                notes = "Keep it very easy - this is for recovery"
            )
        )
    }
    
    private fun getTargetZonesForWorkout(type: PlannedWorkoutType, bodyScan: BodyScan): List<BodyZone> {
        return when (type) {
            PlannedWorkoutType.RUNNING -> listOf(BodyZone.QUADS, BodyZone.HAMSTRINGS, BodyZone.CALVES, BodyZone.CORE)
            PlannedWorkoutType.GYM_STRENGTH, PlannedWorkoutType.GYM_HYPERTROPHY -> bodyScan.focusZones.take(4)
            PlannedWorkoutType.HIIT -> listOf(BodyZone.CORE, BodyZone.QUADS, BodyZone.GLUTES)
            else -> emptyList()
        }
    }
    
    private fun getStrengthExerciseDatabase(): Map<BodyZone, List<String>> {
        return mapOf(
            BodyZone.UPPER_CHEST to listOf("Incline Bench Press", "Incline Dumbbell Press"),
            BodyZone.LOWER_CHEST to listOf("Flat Bench Press", "Dips"),
            BodyZone.SHOULDERS to listOf("Overhead Press", "Lateral Raises"),
            BodyZone.UPPER_BACK to listOf("Barbell Rows", "Face Pulls"),
            BodyZone.LATS to listOf("Pull-ups", "Lat Pulldown"),
            BodyZone.LOWER_BACK to listOf("Deadlift", "Good Mornings"),
            BodyZone.BICEPS to listOf("Barbell Curls", "Hammer Curls"),
            BodyZone.TRICEPS to listOf("Close-Grip Bench", "Tricep Pushdowns"),
            BodyZone.QUADS to listOf("Squats", "Leg Press"),
            BodyZone.HAMSTRINGS to listOf("Romanian Deadlift", "Leg Curls"),
            BodyZone.GLUTES to listOf("Hip Thrusts", "Bulgarian Split Squats"),
            BodyZone.CALVES to listOf("Standing Calf Raises", "Seated Calf Raises"),
            BodyZone.ABS to listOf("Cable Crunches", "Hanging Leg Raises"),
            BodyZone.CORE to listOf("Plank", "Dead Bug")
        )
    }
    
    private fun getHypertrophyExerciseDatabase(): Map<BodyZone, List<String>> {
        return mapOf(
            BodyZone.UPPER_CHEST to listOf("Incline Dumbbell Press", "Incline Cable Flyes", "Low-to-High Cable Flyes"),
            BodyZone.LOWER_CHEST to listOf("Flat Dumbbell Press", "Cable Crossovers", "Dips"),
            BodyZone.SHOULDERS to listOf("Dumbbell Shoulder Press", "Lateral Raises", "Rear Delt Flyes"),
            BodyZone.UPPER_BACK to listOf("Cable Rows", "Face Pulls", "Reverse Flyes"),
            BodyZone.LATS to listOf("Lat Pulldown", "Straight Arm Pulldown", "Dumbbell Rows"),
            BodyZone.BICEPS to listOf("Incline Dumbbell Curls", "Preacher Curls", "Cable Curls"),
            BodyZone.TRICEPS to listOf("Overhead Tricep Extension", "Tricep Pushdowns", "Skull Crushers"),
            BodyZone.QUADS to listOf("Leg Press", "Leg Extensions", "Hack Squats"),
            BodyZone.HAMSTRINGS to listOf("Lying Leg Curls", "Seated Leg Curls", "Stiff-Leg Deadlift"),
            BodyZone.GLUTES to listOf("Hip Thrusts", "Cable Kickbacks", "Glute Bridges"),
            BodyZone.CALVES to listOf("Standing Calf Raises", "Seated Calf Raises"),
            BodyZone.ABS to listOf("Cable Crunches", "Ab Wheel Rollouts", "Decline Crunches")
        )
    }
}
