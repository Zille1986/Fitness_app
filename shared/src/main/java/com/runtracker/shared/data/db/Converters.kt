package com.runtracker.shared.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.runtracker.shared.data.model.*

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromRoutePointList(value: List<RoutePoint>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toRoutePointList(value: String): List<RoutePoint> {
        val type = object : TypeToken<List<RoutePoint>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromSplitList(value: List<Split>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toSplitList(value: String): List<Split> {
        val type = object : TypeToken<List<Split>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromRunSource(value: RunSource): String {
        return value.name
    }

    @TypeConverter
    fun toRunSource(value: String): RunSource {
        return RunSource.valueOf(value)
    }

    @TypeConverter
    fun fromGoalType(value: GoalType): String {
        return value.name
    }

    @TypeConverter
    fun toGoalType(value: String): GoalType {
        return GoalType.valueOf(value)
    }

    @TypeConverter
    fun fromWeeklyWorkoutDayList(value: List<WeeklyWorkoutDay>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toWeeklyWorkoutDayList(value: String): List<WeeklyWorkoutDay> {
        val type = object : TypeToken<List<WeeklyWorkoutDay>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromScheduledWorkoutList(value: List<ScheduledWorkout>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toScheduledWorkoutList(value: String): List<ScheduledWorkout> {
        val type = object : TypeToken<List<ScheduledWorkout>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromGender(value: Gender?): String? {
        return value?.name
    }

    @TypeConverter
    fun toGender(value: String?): Gender? {
        return value?.let { Gender.valueOf(it) }
    }

    @TypeConverter
    fun fromUnits(value: Units): String {
        return value.name
    }

    @TypeConverter
    fun toUnits(value: String): Units {
        return Units.valueOf(value)
    }

    @TypeConverter
    fun fromPersonalRecords(value: PersonalRecords): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toPersonalRecords(value: String): PersonalRecords {
        return gson.fromJson(value, PersonalRecords::class.java) ?: PersonalRecords()
    }

    // Gym converters
    @TypeConverter
    fun fromMuscleGroup(value: MuscleGroup): String {
        return value.name
    }

    @TypeConverter
    fun toMuscleGroup(value: String): MuscleGroup {
        return MuscleGroup.valueOf(value)
    }

    @TypeConverter
    fun fromMuscleGroupList(value: List<MuscleGroup>): String {
        return gson.toJson(value.map { it.name })
    }

    @TypeConverter
    fun toMuscleGroupList(value: String): List<MuscleGroup> {
        val type = object : TypeToken<List<String>>() {}.type
        val names: List<String> = gson.fromJson(value, type) ?: emptyList()
        return names.map { MuscleGroup.valueOf(it) }
    }

    @TypeConverter
    fun fromEquipment(value: Equipment): String {
        return value.name
    }

    @TypeConverter
    fun toEquipment(value: String): Equipment {
        return Equipment.valueOf(value)
    }

    @TypeConverter
    fun fromExerciseType(value: ExerciseType): String {
        return value.name
    }

    @TypeConverter
    fun toExerciseType(value: String): ExerciseType {
        return ExerciseType.valueOf(value)
    }

    @TypeConverter
    fun fromDifficulty(value: Difficulty): String {
        return value.name
    }

    @TypeConverter
    fun toDifficulty(value: String): Difficulty {
        return Difficulty.valueOf(value)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromWorkoutExerciseList(value: List<WorkoutExercise>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toWorkoutExerciseList(value: String): List<WorkoutExercise> {
        val type = object : TypeToken<List<WorkoutExercise>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromTemplateExerciseList(value: List<TemplateExercise>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toTemplateExerciseList(value: String): List<TemplateExercise> {
        val type = object : TypeToken<List<TemplateExercise>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    // Nutrition converters
    @TypeConverter
    fun fromMealEntryList(value: List<MealEntry>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toMealEntryList(value: String): List<MealEntry> {
        val type = object : TypeToken<List<MealEntry>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromNutritionGoalType(value: NutritionGoalType): String {
        return value.name
    }

    @TypeConverter
    fun toNutritionGoalType(value: String): NutritionGoalType {
        return NutritionGoalType.valueOf(value)
    }

    @TypeConverter
    fun fromActivityLevel(value: ActivityLevel): String {
        return value.name
    }

    @TypeConverter
    fun toActivityLevel(value: String): ActivityLevel {
        return ActivityLevel.valueOf(value)
    }

    // Custom Run Workout converters
    @TypeConverter
    fun fromWorkoutPhaseList(value: List<WorkoutPhase>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toWorkoutPhaseList(value: String): List<WorkoutPhase> {
        val type = object : TypeToken<List<WorkoutPhase>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromRunDifficulty(value: RunDifficulty): String {
        return value.name
    }

    @TypeConverter
    fun toRunDifficulty(value: String): RunDifficulty {
        return RunDifficulty.valueOf(value)
    }

    @TypeConverter
    fun fromWorkoutCategory(value: WorkoutCategory): String {
        return value.name
    }

    @TypeConverter
    fun toWorkoutCategory(value: String): WorkoutCategory {
        return WorkoutCategory.valueOf(value)
    }

    // Custom Training Plan converters
    @TypeConverter
    fun fromPlanWeekList(value: List<PlanWeek>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toPlanWeekList(value: String): List<PlanWeek> {
        val type = object : TypeToken<List<PlanWeek>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromWorkoutType(value: WorkoutType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toWorkoutType(value: String?): WorkoutType? {
        return value?.let { WorkoutType.valueOf(it) }
    }

    // Gamification converters
    @TypeConverter
    fun fromAchievementCategory(value: AchievementCategory): String {
        return value.name
    }

    @TypeConverter
    fun toAchievementCategory(value: String): AchievementCategory {
        return AchievementCategory.valueOf(value)
    }

    @TypeConverter
    fun fromXpReason(value: XpReason): String {
        return value.name
    }

    @TypeConverter
    fun toXpReason(value: String): XpReason {
        return XpReason.valueOf(value)
    }

    // Mental Health converters
    @TypeConverter
    fun fromMoodLevel(value: MoodLevel): String = value.name

    @TypeConverter
    fun toMoodLevel(value: String): MoodLevel = MoodLevel.valueOf(value)

    @TypeConverter
    fun fromEnergyLevel(value: EnergyLevel): String = value.name

    @TypeConverter
    fun toEnergyLevel(value: String): EnergyLevel = EnergyLevel.valueOf(value)

    @TypeConverter
    fun fromStressLevel(value: StressLevel): String = value.name

    @TypeConverter
    fun toStressLevel(value: String): StressLevel = StressLevel.valueOf(value)

    @TypeConverter
    fun fromMindfulnessType(value: MindfulnessType): String = value.name

    @TypeConverter
    fun toMindfulnessType(value: String): MindfulnessType = MindfulnessType.valueOf(value)

    @TypeConverter
    fun fromMoodLevelNullable(value: MoodLevel?): String? = value?.name

    @TypeConverter
    fun toMoodLevelNullable(value: String?): MoodLevel? = value?.let { MoodLevel.valueOf(it) }

    @TypeConverter
    fun fromEnergyLevelNullable(value: EnergyLevel?): String? = value?.name

    @TypeConverter
    fun toEnergyLevelNullable(value: String?): EnergyLevel? = value?.let { EnergyLevel.valueOf(it) }

    @TypeConverter
    fun fromStressLevelNullable(value: StressLevel?): String? = value?.name

    @TypeConverter
    fun toStressLevelNullable(value: String?): StressLevel? = value?.let { StressLevel.valueOf(it) }

    // Personalized Plan converters
    @TypeConverter
    fun fromFitnessGoal(value: FitnessGoal): String = value.name

    @TypeConverter
    fun toFitnessGoal(value: String): FitnessGoal = FitnessGoal.valueOf(value)

    @TypeConverter
    fun fromPlanPreferences(value: PlanPreferences): String = gson.toJson(value)

    @TypeConverter
    fun toPlanPreferences(value: String): PlanPreferences = 
        gson.fromJson(value, PlanPreferences::class.java) ?: PlanPreferences(
            workoutDaysPerWeek = 4,
            preferredDays = listOf(2, 3, 5, 6),
            availableTimePerDay = mapOf(2 to 45, 3 to 45, 5 to 45, 6 to 60)
        )

    @TypeConverter
    fun fromPlannedWorkoutList(value: List<PlannedWorkout>): String = gson.toJson(value)

    @TypeConverter
    fun toPlannedWorkoutList(value: String): List<PlannedWorkout> {
        val type = object : TypeToken<List<PlannedWorkout>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromIntList(value: List<Int>): String = gson.toJson(value)

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromIntIntMap(value: Map<Int, Int>): String = gson.toJson(value)

    @TypeConverter
    fun toIntIntMap(value: String): Map<Int, Int> {
        val type = object : TypeToken<Map<Int, Int>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }

    // PlannedWorkout converters
    @TypeConverter
    fun fromPlannedWorkoutType(value: PlannedWorkoutType): String = value.name

    @TypeConverter
    fun toPlannedWorkoutType(value: String): PlannedWorkoutType = 
        try { PlannedWorkoutType.valueOf(value) } catch (e: Exception) { PlannedWorkoutType.REST }

    @TypeConverter
    fun fromPlannedExerciseType(value: PlannedExerciseType): String = value.name

    @TypeConverter
    fun toPlannedExerciseType(value: String): PlannedExerciseType = 
        try { PlannedExerciseType.valueOf(value) } catch (e: Exception) { PlannedExerciseType.COMPOUND_STRENGTH }

    // BodyScan converters
    @TypeConverter
    fun fromBodyType(value: BodyType): String = value.name

    @TypeConverter
    fun toBodyType(value: String): BodyType = BodyType.valueOf(value)

    @TypeConverter
    fun fromBodyZoneList(value: List<BodyZone>): String = gson.toJson(value.map { it.name })

    @TypeConverter
    fun toBodyZoneList(value: String): List<BodyZone> {
        val type = object : TypeToken<List<String>>() {}.type
        val names: List<String> = gson.fromJson(value, type) ?: emptyList()
        return names.mapNotNull { 
            try { BodyZone.valueOf(it) } catch (e: Exception) { null }
        }
    }

    @TypeConverter
    fun fromMuscleBalanceAssessment(value: MuscleBalanceAssessment): String = gson.toJson(value)

    @TypeConverter
    fun toMuscleBalanceAssessment(value: String): MuscleBalanceAssessment {
        return try {
            gson.fromJson(value, MuscleBalanceAssessment::class.java)
        } catch (e: Exception) {
            MuscleBalanceAssessment(
                overallBalance = BalanceLevel.BALANCED,
                leftRightSymmetry = BalanceLevel.BALANCED,
                frontBackBalance = BalanceLevel.BALANCED,
                upperLowerBalance = BalanceLevel.BALANCED,
                imbalances = emptyList()
            )
        }
    }

    @TypeConverter
    fun fromPostureAssessment(value: PostureAssessment): String = gson.toJson(value)

    @TypeConverter
    fun toPostureAssessment(value: String): PostureAssessment {
        return try {
            gson.fromJson(value, PostureAssessment::class.java)
        } catch (e: Exception) {
            PostureAssessment(
                overallPosture = PostureLevel.GOOD,
                issues = emptyList()
            )
        }
    }

    // WorkoutPlan converters
    @TypeConverter
    fun fromWeeklyWorkoutType(value: WeeklyWorkoutType): String = value.name

    @TypeConverter
    fun toWeeklyWorkoutType(value: String): WeeklyWorkoutType = 
        try { WeeklyWorkoutType.valueOf(value) } catch (e: Exception) { WeeklyWorkoutType.REST }

    // PersonalizedPlan converters
    @TypeConverter
    fun fromPlannedDayList(value: List<PlannedDay>): String = gson.toJson(value)

    @TypeConverter
    fun toPlannedDayList(value: String): List<PlannedDay> {
        val type = object : TypeToken<List<PlannedDay>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    // Swimming converters
    @TypeConverter
    fun fromSwimType(value: SwimType): String = value.name

    @TypeConverter
    fun toSwimType(value: String): SwimType = SwimType.valueOf(value)

    @TypeConverter
    fun fromPoolLength(value: PoolLength?): String? = value?.name

    @TypeConverter
    fun toPoolLength(value: String?): PoolLength? = value?.let { PoolLength.valueOf(it) }

    @TypeConverter
    fun fromStrokeType(value: StrokeType): String = value.name

    @TypeConverter
    fun toStrokeType(value: String): StrokeType = StrokeType.valueOf(value)

    @TypeConverter
    fun fromSwimSource(value: SwimSource): String = value.name

    @TypeConverter
    fun toSwimSource(value: String): SwimSource = SwimSource.valueOf(value)

    @TypeConverter
    fun fromSwimSplitList(value: List<SwimSplit>): String = gson.toJson(value)

    @TypeConverter
    fun toSwimSplitList(value: String): List<SwimSplit> {
        val type = object : TypeToken<List<SwimSplit>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromSwimGoalType(value: SwimGoalType): String = value.name

    @TypeConverter
    fun toSwimGoalType(value: String): SwimGoalType = SwimGoalType.valueOf(value)

    @TypeConverter
    fun fromScheduledSwimWorkoutList(value: List<ScheduledSwimWorkout>): String = gson.toJson(value)

    @TypeConverter
    fun toScheduledSwimWorkoutList(value: String): List<ScheduledSwimWorkout> {
        val type = object : TypeToken<List<ScheduledSwimWorkout>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    // Cycling converters
    @TypeConverter
    fun fromCyclingType(value: CyclingType): String = value.name

    @TypeConverter
    fun toCyclingType(value: String): CyclingType = CyclingType.valueOf(value)

    @TypeConverter
    fun fromCyclingSource(value: CyclingSource): String = value.name

    @TypeConverter
    fun toCyclingSource(value: String): CyclingSource = CyclingSource.valueOf(value)

    @TypeConverter
    fun fromCyclingSplitList(value: List<CyclingSplit>): String = gson.toJson(value)

    @TypeConverter
    fun toCyclingSplitList(value: String): List<CyclingSplit> {
        val type = object : TypeToken<List<CyclingSplit>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromPowerDataPointList(value: List<PowerDataPoint>): String = gson.toJson(value)

    @TypeConverter
    fun toPowerDataPointList(value: String): List<PowerDataPoint> {
        val type = object : TypeToken<List<PowerDataPoint>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromCyclingGoalType(value: CyclingGoalType): String = value.name

    @TypeConverter
    fun toCyclingGoalType(value: String): CyclingGoalType = CyclingGoalType.valueOf(value)

    @TypeConverter
    fun fromScheduledCyclingWorkoutList(value: List<ScheduledCyclingWorkout>): String = gson.toJson(value)

    @TypeConverter
    fun toScheduledCyclingWorkoutList(value: String): List<ScheduledCyclingWorkout> {
        val type = object : TypeToken<List<ScheduledCyclingWorkout>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromHeartRateZoneTimeList(value: List<HeartRateZoneTime>): String = gson.toJson(value)

    @TypeConverter
    fun toHeartRateZoneTimeList(value: String): List<HeartRateZoneTime> {
        val type = object : TypeToken<List<HeartRateZoneTime>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromEmergencyContactList(value: List<EmergencyContact>): String = gson.toJson(value)

    @TypeConverter
    fun toEmergencyContactList(value: String): List<EmergencyContact> {
        val type = object : TypeToken<List<EmergencyContact>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
