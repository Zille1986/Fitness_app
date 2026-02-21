package com.runtracker.shared.training

import com.runtracker.shared.data.model.*
import java.util.*

object TrainingPlanGenerator {

    fun generatePlan(
        goalType: GoalType,
        currentFitnessLevel: FitnessLevel,
        weeklyAvailableDays: Int = 4,
        targetDate: Long? = null,
        currentWeeklyDistanceKm: Double = 0.0
    ): TrainingPlan {
        val weeksNeeded = getWeeksNeeded(goalType, currentFitnessLevel)
        val startDate = System.currentTimeMillis()
        val endDate = targetDate ?: (startDate + weeksNeeded * 7 * 24 * 60 * 60 * 1000L)
        
        val runDays = selectRunDays(weeklyAvailableDays)
        val schedule = generateWeeklySchedule(
            goalType = goalType,
            fitnessLevel = currentFitnessLevel,
            totalWeeks = weeksNeeded,
            runDays = runDays,
            startingWeeklyKm = currentWeeklyDistanceKm,
            startingLongRunKm = currentWeeklyDistanceKm * 0.35
        )
        
        return TrainingPlan(
            name = getPlanName(goalType),
            description = getPlanDescription(goalType, currentFitnessLevel),
            goalType = goalType,
            targetDistance = getTargetDistance(goalType),
            startDate = startDate,
            endDate = endDate,
            weeklySchedule = schedule
        )
    }
    
    fun generateAdvancedPlan(
        goalType: GoalType,
        currentFitnessLevel: FitnessLevel,
        selectedDays: List<Int>,
        currentWeeklyDistanceKm: Double,
        currentLongRunKm: Double,
        previousRuns: List<Run> = emptyList(),
        userAge: Int? = null,
        restingHeartRate: Int? = null
    ): TrainingPlan {
        val weeksNeeded = getWeeksNeeded(goalType, currentFitnessLevel)
        val startDate = System.currentTimeMillis()
        val endDate = startDate + weeksNeeded * 7 * 24 * 60 * 60 * 1000L
        
        // Calculate personalized zones from user's running history
        val personalPaceZones = if (previousRuns.isNotEmpty()) {
            PersonalZonesCalculator.calculatePaceZones(previousRuns)
        } else null
        
        val personalHrZones = PersonalZonesCalculator.calculateHeartRateZones(
            previousRuns, userAge, restingHeartRate
        )
        
        val schedule = generateWeeklySchedule(
            goalType = goalType,
            fitnessLevel = currentFitnessLevel,
            totalWeeks = weeksNeeded,
            runDays = selectedDays,
            startingWeeklyKm = currentWeeklyDistanceKm,
            startingLongRunKm = currentLongRunKm,
            personalPaceZones = personalPaceZones,
            personalHrZones = personalHrZones
        )
        
        return TrainingPlan(
            name = getPlanName(goalType),
            description = getPlanDescription(goalType, currentFitnessLevel),
            goalType = goalType,
            targetDistance = getTargetDistance(goalType),
            startDate = startDate,
            endDate = endDate,
            weeklySchedule = schedule
        )
    }

    private fun getWeeksNeeded(goalType: GoalType, fitnessLevel: FitnessLevel): Int {
        val baseWeeks = when (goalType) {
            GoalType.FIRST_5K -> 8
            GoalType.IMPROVE_5K -> 6
            GoalType.FIRST_10K -> 10
            GoalType.IMPROVE_10K -> 8
            GoalType.HALF_MARATHON -> 12
            GoalType.MARATHON -> 16
            GoalType.GENERAL_FITNESS -> 8
            GoalType.WEIGHT_LOSS -> 12
            GoalType.CUSTOM -> 8
        }
        
        return when (fitnessLevel) {
            FitnessLevel.BEGINNER -> baseWeeks + 4
            FitnessLevel.NOVICE -> baseWeeks + 2
            FitnessLevel.INTERMEDIATE -> baseWeeks
            FitnessLevel.ADVANCED -> baseWeeks - 2
            FitnessLevel.ELITE -> baseWeeks - 4
        }.coerceAtLeast(4)
    }

    private fun getTargetDistance(goalType: GoalType): Double? {
        return when (goalType) {
            GoalType.FIRST_5K, GoalType.IMPROVE_5K -> 5000.0
            GoalType.FIRST_10K, GoalType.IMPROVE_10K -> 10000.0
            GoalType.HALF_MARATHON -> 21097.5
            GoalType.MARATHON -> 42195.0
            else -> null
        }
    }

    private fun getPlanName(goalType: GoalType): String {
        return when (goalType) {
            GoalType.FIRST_5K -> "Couch to 5K"
            GoalType.IMPROVE_5K -> "5K Speed Builder"
            GoalType.FIRST_10K -> "10K Foundation"
            GoalType.IMPROVE_10K -> "10K Performance"
            GoalType.HALF_MARATHON -> "Half Marathon Prep"
            GoalType.MARATHON -> "Marathon Training"
            GoalType.GENERAL_FITNESS -> "General Fitness"
            GoalType.WEIGHT_LOSS -> "Run to Lose"
            GoalType.CUSTOM -> "Custom Plan"
        }
    }

    private fun getPlanDescription(goalType: GoalType, fitnessLevel: FitnessLevel): String {
        val levelDesc = when (fitnessLevel) {
            FitnessLevel.BEGINNER -> "beginner"
            FitnessLevel.NOVICE -> "novice"
            FitnessLevel.INTERMEDIATE -> "intermediate"
            FitnessLevel.ADVANCED -> "advanced"
            FitnessLevel.ELITE -> "elite"
        }
        
        return when (goalType) {
            GoalType.FIRST_5K -> "A $levelDesc plan to help you run your first 5K without stopping."
            GoalType.IMPROVE_5K -> "Improve your 5K time with structured speed work and tempo runs."
            GoalType.FIRST_10K -> "Build endurance to complete your first 10K race."
            GoalType.IMPROVE_10K -> "Take your 10K to the next level with advanced training."
            GoalType.HALF_MARATHON -> "Comprehensive training for the half marathon distance."
            GoalType.MARATHON -> "Full marathon preparation with progressive long runs."
            GoalType.GENERAL_FITNESS -> "Maintain and improve your overall running fitness."
            GoalType.WEIGHT_LOSS -> "Optimize your running for healthy weight management."
            GoalType.CUSTOM -> "A customized training plan tailored to your goals."
        }
    }

    private fun generateWeeklySchedule(
        goalType: GoalType,
        fitnessLevel: FitnessLevel,
        totalWeeks: Int,
        runDays: List<Int>,
        startingWeeklyKm: Double,
        startingLongRunKm: Double,
        personalPaceZones: PersonalPaceZones? = null,
        personalHrZones: PersonalHeartRateZones? = null
    ): List<ScheduledWorkout> {
        val workouts = mutableListOf<ScheduledWorkout>()
        
        for (week in 1..totalWeeks) {
            val weeklyDistance = calculateWeeklyDistance(
                week = week,
                totalWeeks = totalWeeks,
                goalType = goalType,
                fitnessLevel = fitnessLevel,
                startingKm = startingWeeklyKm
            )
            
            val longRunDistance = calculateLongRunDistance(
                week = week,
                totalWeeks = totalWeeks,
                goalType = goalType,
                startingLongRunKm = startingLongRunKm
            )
            
            val weekWorkouts = distributeWorkouts(
                week = week,
                totalWeeks = totalWeeks,
                weeklyDistanceKm = weeklyDistance,
                longRunDistanceKm = longRunDistance,
                runDays = runDays,
                goalType = goalType,
                fitnessLevel = fitnessLevel,
                personalPaceZones = personalPaceZones,
                personalHrZones = personalHrZones
            )
            
            workouts.addAll(weekWorkouts)
        }
        
        return workouts
    }

    private fun selectRunDays(daysPerWeek: Int): List<Int> {
        return when (daysPerWeek) {
            3 -> listOf(Calendar.TUESDAY, Calendar.THURSDAY, Calendar.SUNDAY)
            4 -> listOf(Calendar.TUESDAY, Calendar.THURSDAY, Calendar.SATURDAY, Calendar.SUNDAY)
            5 -> listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.THURSDAY, Calendar.SATURDAY, Calendar.SUNDAY)
            6 -> listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
            else -> listOf(Calendar.TUESDAY, Calendar.THURSDAY, Calendar.SATURDAY)
        }
    }

    private fun calculateWeeklyDistance(
        week: Int,
        totalWeeks: Int,
        goalType: GoalType,
        fitnessLevel: FitnessLevel,
        startingKm: Double
    ): Double {
        // Use the user's starting km as the base
        val baseKm = startingKm.coerceAtLeast(10.0)
        
        // Target is ~50% more than starting, capped by goal
        val maxTargetKm = when (goalType) {
            GoalType.FIRST_5K -> 25.0
            GoalType.IMPROVE_5K -> 45.0
            GoalType.FIRST_10K -> 45.0
            GoalType.IMPROVE_10K -> 55.0
            GoalType.HALF_MARATHON -> 65.0
            GoalType.MARATHON -> 80.0
            GoalType.GENERAL_FITNESS -> 40.0
            GoalType.WEIGHT_LOSS -> 40.0
            GoalType.CUSTOM -> 40.0
        }
        
        // Target is starting + 50%, but not more than max for goal
        val targetWeeklyKm = minOf(baseKm * 1.5, maxTargetKm)
        
        val buildUpWeeks = totalWeeks - 2
        
        return when {
            week <= buildUpWeeks -> {
                // Progressive 10% weekly increase
                val weeklyIncrease = baseKm * 0.10
                val targetForWeek = baseKm + (weeklyIncrease * (week - 1))
                minOf(targetForWeek, targetWeeklyKm)
            }
            week == totalWeeks - 1 -> baseKm * 0.8 // Taper week 1
            week == totalWeeks -> baseKm * 0.6 // Taper week 2
            else -> targetWeeklyKm
        }
    }
    
    private fun calculateLongRunDistance(
        week: Int,
        totalWeeks: Int,
        goalType: GoalType,
        startingLongRunKm: Double
    ): Double {
        // Use user's starting long run as base
        val baseLongRun = startingLongRunKm.coerceAtLeast(5.0)
        
        // Target is ~50% more than starting, capped by goal
        val maxLongRunKm = when (goalType) {
            GoalType.FIRST_5K -> 8.0
            GoalType.IMPROVE_5K -> 14.0
            GoalType.FIRST_10K -> 14.0
            GoalType.IMPROVE_10K -> 18.0
            GoalType.HALF_MARATHON -> 22.0
            GoalType.MARATHON -> 35.0
            GoalType.GENERAL_FITNESS -> 15.0
            GoalType.WEIGHT_LOSS -> 15.0
            GoalType.CUSTOM -> 15.0
        }
        
        val targetLongRunKm = minOf(baseLongRun * 1.5, maxLongRunKm)
        val buildUpWeeks = totalWeeks - 2
        
        return when {
            week <= buildUpWeeks -> {
                // Increase by ~1km per week
                val targetForWeek = baseLongRun + ((week - 1) * 1.0)
                minOf(targetForWeek, targetLongRunKm)
            }
            week == totalWeeks - 1 -> baseLongRun * 0.7 // Taper
            week == totalWeeks -> baseLongRun * 0.5 // Race week
            else -> targetLongRunKm
        }
    }

    private fun distributeWorkouts(
        week: Int,
        totalWeeks: Int,
        weeklyDistanceKm: Double,
        longRunDistanceKm: Double,
        runDays: List<Int>,
        goalType: GoalType,
        fitnessLevel: FitnessLevel,
        personalPaceZones: PersonalPaceZones? = null,
        personalHrZones: PersonalHeartRateZones? = null
    ): List<ScheduledWorkout> {
        val workouts = mutableListOf<ScheduledWorkout>()
        val numRuns = runDays.size
        
        // Long run distance, remaining split among other runs
        val actualLongRun = longRunDistanceKm
        val remainingDistance = weeklyDistanceKm - actualLongRun
        val numOtherRuns = numRuns - 1
        val baseEasyDistance = remainingDistance / numOtherRuns.coerceAtLeast(1)
        
        runDays.forEachIndexed { index, dayOfWeek ->
            val workoutType: WorkoutType
            val distance: Double
            val description: String
            
            when {
                // Last day is always long run
                index == runDays.lastIndex -> {
                    workoutType = WorkoutType.LONG_RUN
                    distance = actualLongRun
                    description = "Long run at easy, conversational pace. Build endurance."
                }
                // First quality session - Tempo or Threshold (mid-week)
                index == 1 && numRuns >= 3 -> {
                    when {
                        week % 3 == 1 -> {
                            workoutType = WorkoutType.TEMPO_RUN
                            distance = baseEasyDistance
                            description = "Tempo run: 10 min warm-up, ${(baseEasyDistance * 0.6).toInt()}km at threshold pace, 10 min cool-down."
                        }
                        week % 3 == 2 -> {
                            workoutType = WorkoutType.INTERVAL_TRAINING
                            distance = baseEasyDistance
                            description = "Intervals: Warm-up, then 6x800m at 5K pace with 400m jog recovery. Cool-down."
                        }
                        else -> {
                            workoutType = WorkoutType.FARTLEK
                            distance = baseEasyDistance
                            description = "Fartlek: Warm-up, then alternate 2 min hard / 2 min easy for 20 min. Cool-down."
                        }
                    }
                }
                // Second quality session for 4+ days (hill repeats or strides)
                index == 2 && numRuns >= 4 -> {
                    when (week % 2) {
                        0 -> {
                            workoutType = WorkoutType.HILL_REPEATS
                            distance = baseEasyDistance * 0.8
                            description = "Hill repeats: Warm-up, 6-8 x 60-90 sec hill sprints, jog down recovery. Cool-down."
                        }
                        else -> {
                            workoutType = WorkoutType.EASY_RUN
                            distance = baseEasyDistance
                            description = "Easy run with 6x100m strides at the end. Focus on form."
                        }
                    }
                }
                // Recovery run after long run (if 5+ days)
                index == 0 && numRuns >= 5 -> {
                    workoutType = WorkoutType.RECOVERY_RUN
                    distance = baseEasyDistance * 0.7
                    description = "Recovery run: Very easy pace, focus on active recovery."
                }
                // Easy runs on other days
                else -> {
                    workoutType = WorkoutType.EASY_RUN
                    distance = baseEasyDistance
                    description = "Easy run at conversational pace. Build aerobic base."
                }
            }
            
            // Get pace and HR targets based on workout type (personalized if available)
            val (paceMin, paceMax, hrZone, hrMin, hrMax) = getWorkoutTargets(
                workoutType, fitnessLevel, personalPaceZones, personalHrZones
            )
            
            // Get warm-up/cool-down targets (easy pace)
            val (warmupPaceMin, warmupPaceMax, _, warmupHrMin, warmupHrMax) = getWorkoutTargets(
                WorkoutType.EASY_RUN, fitnessLevel, personalPaceZones, personalHrZones
            )
            
            // Generate intervals with warm-up and cool-down
            val intervals = generateWorkoutIntervals(
                workoutType = workoutType,
                totalDistanceKm = distance,
                mainPaceMin = paceMin,
                mainPaceMax = paceMax,
                mainHrMin = hrMin,
                mainHrMax = hrMax,
                warmupPaceMin = warmupPaceMin,
                warmupPaceMax = warmupPaceMax,
                warmupHrMin = warmupHrMin,
                warmupHrMax = warmupHrMax
            )
            
            workouts.add(
                ScheduledWorkout(
                    id = UUID.randomUUID().toString(),
                    dayOfWeek = dayOfWeek,
                    weekNumber = week,
                    workoutType = workoutType,
                    targetDistanceMeters = distance * 1000,
                    targetPaceMinSecondsPerKm = paceMin,
                    targetPaceMaxSecondsPerKm = paceMax,
                    targetHeartRateZone = hrZone,
                    targetHeartRateMin = hrMin,
                    targetHeartRateMax = hrMax,
                    intervals = intervals,
                    description = description
                )
            )
        }
        
        return workouts
    }
    
    private fun generateWorkoutIntervals(
        workoutType: WorkoutType,
        totalDistanceKm: Double,
        mainPaceMin: Double?,
        mainPaceMax: Double?,
        mainHrMin: Int?,
        mainHrMax: Int?,
        warmupPaceMin: Double?,
        warmupPaceMax: Double?,
        warmupHrMin: Int?,
        warmupHrMax: Int?
    ): List<Interval> {
        val intervals = mutableListOf<Interval>()
        
        // Standard warm-up: 10 minutes at easy pace
        val warmup = Interval(
            type = IntervalType.WARMUP,
            durationSeconds = 600, // 10 minutes
            targetPaceMinSecondsPerKm = warmupPaceMin,
            targetPaceMaxSecondsPerKm = warmupPaceMax,
            targetHeartRateZone = HeartRateZone.ZONE_1,
            targetHeartRateMin = warmupHrMin,
            targetHeartRateMax = warmupHrMax
        )
        
        // Standard cool-down: 5-10 minutes at easy pace
        val cooldown = Interval(
            type = IntervalType.COOLDOWN,
            durationSeconds = 300, // 5 minutes
            targetPaceMinSecondsPerKm = warmupPaceMin,
            targetPaceMaxSecondsPerKm = warmupPaceMax,
            targetHeartRateZone = HeartRateZone.ZONE_1,
            targetHeartRateMin = warmupHrMin,
            targetHeartRateMax = warmupHrMax
        )
        
        when (workoutType) {
            WorkoutType.EASY_RUN, WorkoutType.RECOVERY_RUN -> {
                // Simple structure: warm-up, main run, cool-down
                intervals.add(warmup.copy(durationSeconds = 300)) // 5 min warm-up
                intervals.add(Interval(
                    type = IntervalType.WORK,
                    distanceMeters = (totalDistanceKm - 1.5) * 1000, // Main portion
                    targetPaceMinSecondsPerKm = mainPaceMin,
                    targetPaceMaxSecondsPerKm = mainPaceMax,
                    targetHeartRateMin = mainHrMin,
                    targetHeartRateMax = mainHrMax
                ))
                intervals.add(cooldown)
            }
            
            WorkoutType.LONG_RUN -> {
                // Longer warm-up and cool-down for long runs
                intervals.add(warmup) // 10 min warm-up
                intervals.add(Interval(
                    type = IntervalType.WORK,
                    distanceMeters = (totalDistanceKm - 2.5) * 1000,
                    targetPaceMinSecondsPerKm = mainPaceMin,
                    targetPaceMaxSecondsPerKm = mainPaceMax,
                    targetHeartRateMin = mainHrMin,
                    targetHeartRateMax = mainHrMax
                ))
                intervals.add(cooldown.copy(durationSeconds = 600)) // 10 min cool-down
            }
            
            WorkoutType.TEMPO_RUN -> {
                // Warm-up, tempo portion, cool-down
                intervals.add(warmup)
                intervals.add(Interval(
                    type = IntervalType.WORK,
                    durationSeconds = ((totalDistanceKm - 2.0) * 1000 / 4.0).toInt(), // ~4 m/s tempo pace
                    targetPaceMinSecondsPerKm = mainPaceMin,
                    targetPaceMaxSecondsPerKm = mainPaceMax,
                    targetHeartRateZone = HeartRateZone.ZONE_4,
                    targetHeartRateMin = mainHrMin,
                    targetHeartRateMax = mainHrMax
                ))
                intervals.add(cooldown.copy(durationSeconds = 600))
            }
            
            WorkoutType.INTERVAL_TRAINING -> {
                // Warm-up, intervals with recovery, cool-down
                intervals.add(warmup)
                
                // 6x800m with 400m recovery
                repeat(6) {
                    intervals.add(Interval(
                        type = IntervalType.WORK,
                        distanceMeters = 800.0,
                        targetPaceMinSecondsPerKm = mainPaceMin,
                        targetPaceMaxSecondsPerKm = mainPaceMax,
                        targetHeartRateZone = HeartRateZone.ZONE_5,
                        targetHeartRateMin = mainHrMin,
                        targetHeartRateMax = mainHrMax
                    ))
                    if (it < 5) { // No recovery after last interval
                        intervals.add(Interval(
                            type = IntervalType.RECOVERY,
                            distanceMeters = 400.0,
                            targetPaceMinSecondsPerKm = warmupPaceMin,
                            targetPaceMaxSecondsPerKm = warmupPaceMax,
                            targetHeartRateZone = HeartRateZone.ZONE_2,
                            targetHeartRateMin = warmupHrMin,
                            targetHeartRateMax = warmupHrMax
                        ))
                    }
                }
                
                intervals.add(cooldown.copy(durationSeconds = 600))
            }
            
            WorkoutType.HILL_REPEATS -> {
                // Warm-up, hill repeats, cool-down
                intervals.add(warmup)
                
                // 8x60 sec hill sprints with jog down recovery
                repeat(8) {
                    intervals.add(Interval(
                        type = IntervalType.WORK,
                        durationSeconds = 60,
                        targetHeartRateZone = HeartRateZone.ZONE_4,
                        targetHeartRateMin = mainHrMin,
                        targetHeartRateMax = mainHrMax
                    ))
                    if (it < 7) {
                        intervals.add(Interval(
                            type = IntervalType.RECOVERY,
                            durationSeconds = 90, // Jog down
                            targetHeartRateZone = HeartRateZone.ZONE_2,
                            targetHeartRateMin = warmupHrMin,
                            targetHeartRateMax = warmupHrMax
                        ))
                    }
                }
                
                intervals.add(cooldown.copy(durationSeconds = 600))
            }
            
            WorkoutType.FARTLEK -> {
                // Warm-up, fartlek (alternating hard/easy), cool-down
                intervals.add(warmup)
                
                // 10x (2 min hard, 2 min easy)
                repeat(10) {
                    intervals.add(Interval(
                        type = IntervalType.WORK,
                        durationSeconds = 120, // 2 min hard
                        targetPaceMinSecondsPerKm = mainPaceMin,
                        targetPaceMaxSecondsPerKm = mainPaceMax,
                        targetHeartRateZone = HeartRateZone.ZONE_4,
                        targetHeartRateMin = mainHrMin,
                        targetHeartRateMax = mainHrMax
                    ))
                    if (it < 9) {
                        intervals.add(Interval(
                            type = IntervalType.RECOVERY,
                            durationSeconds = 120, // 2 min easy
                            targetPaceMinSecondsPerKm = warmupPaceMin,
                            targetPaceMaxSecondsPerKm = warmupPaceMax,
                            targetHeartRateZone = HeartRateZone.ZONE_2,
                            targetHeartRateMin = warmupHrMin,
                            targetHeartRateMax = warmupHrMax
                        ))
                    }
                }
                
                intervals.add(cooldown.copy(durationSeconds = 600))
            }
            
            WorkoutType.RACE_PACE -> {
                // Warm-up, race pace segments, cool-down
                intervals.add(warmup)
                intervals.add(Interval(
                    type = IntervalType.WORK,
                    distanceMeters = (totalDistanceKm - 2.0) * 1000,
                    targetPaceMinSecondsPerKm = mainPaceMin,
                    targetPaceMaxSecondsPerKm = mainPaceMax,
                    targetHeartRateZone = HeartRateZone.ZONE_4,
                    targetHeartRateMin = mainHrMin,
                    targetHeartRateMax = mainHrMax
                ))
                intervals.add(cooldown.copy(durationSeconds = 600))
            }
            
            else -> {
                // Default: just the main workout
                intervals.add(Interval(
                    type = IntervalType.WORK,
                    distanceMeters = totalDistanceKm * 1000,
                    targetPaceMinSecondsPerKm = mainPaceMin,
                    targetPaceMaxSecondsPerKm = mainPaceMax,
                    targetHeartRateMin = mainHrMin,
                    targetHeartRateMax = mainHrMax
                ))
            }
        }
        
        return intervals
    }

    private data class WorkoutTargets(
        val paceMin: Double?,
        val paceMax: Double?,
        val hrZone: HeartRateZone?,
        val hrMin: Int?,
        val hrMax: Int?
    )
    
    private fun getWorkoutTargets(
        workoutType: WorkoutType, 
        fitnessLevel: FitnessLevel,
        personalPaceZones: PersonalPaceZones? = null,
        personalHrZones: PersonalHeartRateZones? = null
    ): WorkoutTargets {
        // Use personalized zones if available, otherwise fall back to defaults
        if (personalPaceZones != null && personalHrZones != null) {
            return getPersonalizedWorkoutTargets(workoutType, personalPaceZones, personalHrZones)
        }
        
        // Fallback: Base paces adjusted by fitness level
        val paceAdjustment = when (fitnessLevel) {
            FitnessLevel.BEGINNER -> 60.0
            FitnessLevel.NOVICE -> 30.0
            FitnessLevel.INTERMEDIATE -> 0.0
            FitnessLevel.ADVANCED -> -20.0
            FitnessLevel.ELITE -> -40.0
        }
        
        return when (workoutType) {
            WorkoutType.EASY_RUN -> WorkoutTargets(
                paceMin = 330.0 + paceAdjustment,
                paceMax = 390.0 + paceAdjustment,
                hrZone = HeartRateZone.ZONE_2,
                hrMin = 120,
                hrMax = 150
            )
            WorkoutType.LONG_RUN -> WorkoutTargets(
                paceMin = 330.0 + paceAdjustment,
                paceMax = 400.0 + paceAdjustment,
                hrZone = HeartRateZone.ZONE_2,
                hrMin = 125,
                hrMax = 155
            )
            WorkoutType.TEMPO_RUN -> WorkoutTargets(
                paceMin = 270.0 + paceAdjustment,
                paceMax = 310.0 + paceAdjustment,
                hrZone = HeartRateZone.ZONE_4,
                hrMin = 160,
                hrMax = 175
            )
            WorkoutType.INTERVAL_TRAINING -> WorkoutTargets(
                paceMin = 230.0 + paceAdjustment,
                paceMax = 280.0 + paceAdjustment,
                hrZone = HeartRateZone.ZONE_5,
                hrMin = 170,
                hrMax = 190
            )
            WorkoutType.HILL_REPEATS -> WorkoutTargets(
                paceMin = null,
                paceMax = null,
                hrZone = HeartRateZone.ZONE_4,
                hrMin = 160,
                hrMax = 180
            )
            WorkoutType.RECOVERY_RUN -> WorkoutTargets(
                paceMin = 360.0 + paceAdjustment,
                paceMax = 450.0 + paceAdjustment,
                hrZone = HeartRateZone.ZONE_1,
                hrMin = 100,
                hrMax = 130
            )
            WorkoutType.RACE_PACE -> WorkoutTargets(
                paceMin = 280.0 + paceAdjustment,
                paceMax = 320.0 + paceAdjustment,
                hrZone = HeartRateZone.ZONE_4,
                hrMin = 165,
                hrMax = 180
            )
            WorkoutType.FARTLEK -> WorkoutTargets(
                paceMin = 270.0 + paceAdjustment,
                paceMax = 390.0 + paceAdjustment,
                hrZone = HeartRateZone.ZONE_3,
                hrMin = 140,
                hrMax = 170
            )
            else -> WorkoutTargets(null, null, null, null, null)
        }
    }
    
    private fun getPersonalizedWorkoutTargets(
        workoutType: WorkoutType,
        paceZones: PersonalPaceZones,
        hrZones: PersonalHeartRateZones
    ): WorkoutTargets {
        return when (workoutType) {
            WorkoutType.EASY_RUN -> WorkoutTargets(
                paceMin = paceZones.easyPaceMin,
                paceMax = paceZones.easyPaceMax,
                hrZone = HeartRateZone.ZONE_2,
                hrMin = hrZones.zone2Min,
                hrMax = hrZones.zone2Max
            )
            WorkoutType.LONG_RUN -> WorkoutTargets(
                paceMin = paceZones.easyPaceMin,
                paceMax = paceZones.easyPaceMax * 1.05,
                hrZone = HeartRateZone.ZONE_2,
                hrMin = hrZones.zone2Min,
                hrMax = hrZones.zone2Max
            )
            WorkoutType.TEMPO_RUN -> WorkoutTargets(
                paceMin = paceZones.tempoPaceMin,
                paceMax = paceZones.tempoPaceMax,
                hrZone = HeartRateZone.ZONE_4,
                hrMin = hrZones.zone4Min,
                hrMax = hrZones.zone4Max
            )
            WorkoutType.INTERVAL_TRAINING -> WorkoutTargets(
                paceMin = paceZones.intervalPaceMin,
                paceMax = paceZones.intervalPaceMax,
                hrZone = HeartRateZone.ZONE_5,
                hrMin = hrZones.zone5Min,
                hrMax = hrZones.zone5Max
            )
            WorkoutType.HILL_REPEATS -> WorkoutTargets(
                paceMin = null,
                paceMax = null,
                hrZone = HeartRateZone.ZONE_4,
                hrMin = hrZones.zone4Min,
                hrMax = hrZones.zone4Max
            )
            WorkoutType.RECOVERY_RUN -> WorkoutTargets(
                paceMin = paceZones.recoveryPaceMin,
                paceMax = paceZones.recoveryPaceMax,
                hrZone = HeartRateZone.ZONE_1,
                hrMin = hrZones.zone1Min,
                hrMax = hrZones.zone1Max
            )
            WorkoutType.RACE_PACE -> WorkoutTargets(
                paceMin = paceZones.thresholdPaceMin,
                paceMax = paceZones.thresholdPaceMax,
                hrZone = HeartRateZone.ZONE_4,
                hrMin = hrZones.zone4Min,
                hrMax = hrZones.zone4Max
            )
            WorkoutType.FARTLEK -> WorkoutTargets(
                paceMin = paceZones.tempoPaceMin,
                paceMax = paceZones.easyPaceMax,
                hrZone = HeartRateZone.ZONE_3,
                hrMin = hrZones.zone3Min,
                hrMax = hrZones.zone3Max
            )
            else -> WorkoutTargets(null, null, null, null, null)
        }
    }
    
    fun recalculateZonesForPlan(
        plan: TrainingPlan,
        previousRuns: List<Run>,
        userAge: Int?,
        restingHeartRate: Int? = null
    ): TrainingPlan {
        // Calculate personalized zones from user's running history
        val personalPaceZones = if (previousRuns.isNotEmpty()) {
            PersonalZonesCalculator.calculatePaceZones(previousRuns)
        } else return plan  // No runs, can't personalize
        
        val personalHrZones = PersonalZonesCalculator.calculateHeartRateZones(
            previousRuns, userAge, restingHeartRate
        )
        
        // Update each workout with new personalized targets
        val updatedWorkouts = plan.weeklySchedule.map { workout ->
            val targets = getPersonalizedWorkoutTargetsStatic(
                workout.workoutType, personalPaceZones, personalHrZones
            )
            workout.copy(
                targetPaceMinSecondsPerKm = targets.paceMin ?: workout.targetPaceMinSecondsPerKm,
                targetPaceMaxSecondsPerKm = targets.paceMax ?: workout.targetPaceMaxSecondsPerKm,
                targetHeartRateMin = targets.hrMin ?: workout.targetHeartRateMin,
                targetHeartRateMax = targets.hrMax ?: workout.targetHeartRateMax
            )
        }
        
        return plan.copy(weeklySchedule = updatedWorkouts)
    }
    
    private fun getPersonalizedWorkoutTargetsStatic(
        workoutType: WorkoutType,
        paceZones: PersonalPaceZones,
        hrZones: PersonalHeartRateZones
    ): WorkoutTargets {
        return when (workoutType) {
            WorkoutType.EASY_RUN -> WorkoutTargets(
                paceMin = paceZones.easyPaceMin,
                paceMax = paceZones.easyPaceMax,
                hrZone = HeartRateZone.ZONE_2,
                hrMin = hrZones.zone2Min,
                hrMax = hrZones.zone2Max
            )
            WorkoutType.LONG_RUN -> WorkoutTargets(
                paceMin = paceZones.easyPaceMin,
                paceMax = paceZones.easyPaceMax * 1.05,
                hrZone = HeartRateZone.ZONE_2,
                hrMin = hrZones.zone2Min,
                hrMax = hrZones.zone2Max
            )
            WorkoutType.TEMPO_RUN -> WorkoutTargets(
                paceMin = paceZones.tempoPaceMin,
                paceMax = paceZones.tempoPaceMax,
                hrZone = HeartRateZone.ZONE_4,
                hrMin = hrZones.zone4Min,
                hrMax = hrZones.zone4Max
            )
            WorkoutType.INTERVAL_TRAINING -> WorkoutTargets(
                paceMin = paceZones.intervalPaceMin,
                paceMax = paceZones.intervalPaceMax,
                hrZone = HeartRateZone.ZONE_5,
                hrMin = hrZones.zone5Min,
                hrMax = hrZones.zone5Max
            )
            WorkoutType.HILL_REPEATS -> WorkoutTargets(
                paceMin = null,
                paceMax = null,
                hrZone = HeartRateZone.ZONE_4,
                hrMin = hrZones.zone4Min,
                hrMax = hrZones.zone4Max
            )
            WorkoutType.RECOVERY_RUN -> WorkoutTargets(
                paceMin = paceZones.recoveryPaceMin,
                paceMax = paceZones.recoveryPaceMax,
                hrZone = HeartRateZone.ZONE_1,
                hrMin = hrZones.zone1Min,
                hrMax = hrZones.zone1Max
            )
            WorkoutType.RACE_PACE -> WorkoutTargets(
                paceMin = paceZones.thresholdPaceMin,
                paceMax = paceZones.thresholdPaceMax,
                hrZone = HeartRateZone.ZONE_4,
                hrMin = hrZones.zone4Min,
                hrMax = hrZones.zone4Max
            )
            WorkoutType.FARTLEK -> WorkoutTargets(
                paceMin = paceZones.tempoPaceMin,
                paceMax = paceZones.easyPaceMax,
                hrZone = HeartRateZone.ZONE_3,
                hrMin = hrZones.zone3Min,
                hrMax = hrZones.zone3Max
            )
            else -> WorkoutTargets(null, null, null, null, null)
        }
    }
    
    fun suggestNextWorkout(
        recentRuns: List<Run>,
        activePlan: TrainingPlan?,
        userProfile: UserProfile
    ): ScheduledWorkout? {
        if (activePlan != null) {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val currentWeek = calculateCurrentWeek(activePlan.startDate)
            
            return activePlan.weeklySchedule
                .filter { it.weekNumber == currentWeek && !it.isCompleted }
                .minByOrNull { 
                    val diff = it.dayOfWeek - today
                    if (diff < 0) diff + 7 else diff
                }
        }
        
        val avgWeeklyDistance = recentRuns
            .filter { it.startTime > System.currentTimeMillis() - 14 * 24 * 60 * 60 * 1000L }
            .sumOf { it.distanceMeters } / 2.0
        
        val suggestedDistance = (avgWeeklyDistance / 4).coerceIn(3000.0, 15000.0)
        
        return ScheduledWorkout(
            id = UUID.randomUUID().toString(),
            dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK),
            weekNumber = 1,
            workoutType = WorkoutType.EASY_RUN,
            targetDistanceMeters = suggestedDistance,
            description = "Suggested easy run based on your recent activity."
        )
    }

    private fun calculateCurrentWeek(planStartDate: Long): Int {
        val elapsed = System.currentTimeMillis() - planStartDate
        return ((elapsed / (7 * 24 * 60 * 60 * 1000L)) + 1).toInt()
    }
}
