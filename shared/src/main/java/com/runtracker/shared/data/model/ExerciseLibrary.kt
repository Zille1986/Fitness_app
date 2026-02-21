package com.runtracker.shared.data.model

object ExerciseLibrary {
    
    fun getDefaultExercises(): List<Exercise> = listOf(
        // CHEST
        Exercise(
            name = "Barbell Bench Press",
            description = "Classic compound chest exercise",
            muscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            instructions = listOf(
                "Lie on bench with eyes under the bar",
                "Grip bar slightly wider than shoulder width",
                "Unrack and lower bar to mid-chest",
                "Press up until arms are extended"
            )
        ),
        Exercise(
            name = "Incline Dumbbell Press",
            description = "Upper chest focused press",
            muscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Dumbbell Flyes",
            description = "Chest isolation exercise",
            muscleGroup = MuscleGroup.CHEST,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Cable Crossover",
            description = "Cable chest isolation",
            muscleGroup = MuscleGroup.CHEST,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Push-ups",
            description = "Bodyweight chest exercise",
            muscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.BEGINNER
        ),
        Exercise(
            name = "Chest Dips",
            description = "Lower chest focused dip",
            muscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS),
            equipment = Equipment.DIP_BARS,
            exerciseType = ExerciseType.COMPOUND
        ),

        // BACK
        Exercise(
            name = "Barbell Deadlift",
            description = "King of compound exercises",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.ADVANCED
        ),
        Exercise(
            name = "Barbell Row",
            description = "Horizontal pulling movement",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS, MuscleGroup.LATS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Pull-ups",
            description = "Vertical pulling bodyweight exercise",
            muscleGroup = MuscleGroup.LATS,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS, MuscleGroup.BACK),
            equipment = Equipment.PULL_UP_BAR,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Lat Pulldown",
            description = "Machine lat exercise",
            muscleGroup = MuscleGroup.LATS,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS),
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Seated Cable Row",
            description = "Horizontal cable pull",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS, MuscleGroup.LATS),
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Dumbbell Row",
            description = "Single arm rowing movement",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS, MuscleGroup.LATS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Face Pulls",
            description = "Rear delt and upper back exercise",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.SHOULDERS),
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION
        ),

        // SHOULDERS
        Exercise(
            name = "Overhead Press",
            description = "Standing barbell shoulder press",
            muscleGroup = MuscleGroup.SHOULDERS,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Dumbbell Shoulder Press",
            description = "Seated or standing dumbbell press",
            muscleGroup = MuscleGroup.SHOULDERS,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Lateral Raises",
            description = "Side delt isolation",
            muscleGroup = MuscleGroup.SHOULDERS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Front Raises",
            description = "Front delt isolation",
            muscleGroup = MuscleGroup.SHOULDERS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Reverse Flyes",
            description = "Rear delt isolation",
            muscleGroup = MuscleGroup.SHOULDERS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Arnold Press",
            description = "Rotational shoulder press",
            muscleGroup = MuscleGroup.SHOULDERS,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND
        ),

        // BICEPS
        Exercise(
            name = "Barbell Curl",
            description = "Classic bicep exercise",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Dumbbell Curl",
            description = "Alternating or simultaneous curls",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Hammer Curls",
            description = "Neutral grip curls",
            muscleGroup = MuscleGroup.BICEPS,
            secondaryMuscleGroups = listOf(MuscleGroup.FOREARMS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Preacher Curls",
            description = "Isolated bicep curl on preacher bench",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.EZ_BAR,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Cable Curls",
            description = "Constant tension bicep curl",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Incline Dumbbell Curl",
            description = "Stretched position bicep curl",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION
        ),

        // TRICEPS
        Exercise(
            name = "Close Grip Bench Press",
            description = "Tricep focused bench press",
            muscleGroup = MuscleGroup.TRICEPS,
            secondaryMuscleGroups = listOf(MuscleGroup.CHEST),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Tricep Pushdown",
            description = "Cable tricep isolation",
            muscleGroup = MuscleGroup.TRICEPS,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Overhead Tricep Extension",
            description = "Long head tricep focus",
            muscleGroup = MuscleGroup.TRICEPS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Skull Crushers",
            description = "Lying tricep extension",
            muscleGroup = MuscleGroup.TRICEPS,
            equipment = Equipment.EZ_BAR,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Tricep Dips",
            description = "Bodyweight tricep exercise",
            muscleGroup = MuscleGroup.TRICEPS,
            secondaryMuscleGroups = listOf(MuscleGroup.CHEST),
            equipment = Equipment.DIP_BARS,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Diamond Push-ups",
            description = "Tricep focused push-up",
            muscleGroup = MuscleGroup.TRICEPS,
            secondaryMuscleGroups = listOf(MuscleGroup.CHEST),
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.COMPOUND
        ),

        // LEGS - QUADS
        Exercise(
            name = "Barbell Squat",
            description = "King of leg exercises",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Front Squat",
            description = "Quad dominant squat variation",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.ADVANCED
        ),
        Exercise(
            name = "Leg Press",
            description = "Machine compound leg exercise",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Leg Extension",
            description = "Quad isolation",
            muscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Lunges",
            description = "Unilateral leg exercise",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Bulgarian Split Squat",
            description = "Single leg squat variation",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Hack Squat",
            description = "Machine squat variation",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.COMPOUND
        ),

        // LEGS - HAMSTRINGS
        Exercise(
            name = "Romanian Deadlift",
            description = "Hip hinge hamstring exercise",
            muscleGroup = MuscleGroup.HAMSTRINGS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Leg Curl",
            description = "Hamstring isolation",
            muscleGroup = MuscleGroup.HAMSTRINGS,
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Stiff Leg Deadlift",
            description = "Straight leg deadlift variation",
            muscleGroup = MuscleGroup.HAMSTRINGS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Good Mornings",
            description = "Hip hinge with bar on back",
            muscleGroup = MuscleGroup.HAMSTRINGS,
            secondaryMuscleGroups = listOf(MuscleGroup.LOWER_BACK, MuscleGroup.GLUTES),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.ADVANCED
        ),

        // LEGS - GLUTES
        Exercise(
            name = "Hip Thrust",
            description = "Primary glute exercise",
            muscleGroup = MuscleGroup.GLUTES,
            secondaryMuscleGroups = listOf(MuscleGroup.HAMSTRINGS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Glute Bridge",
            description = "Bodyweight glute activation",
            muscleGroup = MuscleGroup.GLUTES,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER
        ),
        Exercise(
            name = "Cable Kickbacks",
            description = "Glute isolation with cable",
            muscleGroup = MuscleGroup.GLUTES,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION
        ),

        // LEGS - CALVES
        Exercise(
            name = "Standing Calf Raise",
            description = "Gastrocnemius focused",
            muscleGroup = MuscleGroup.CALVES,
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Seated Calf Raise",
            description = "Soleus focused",
            muscleGroup = MuscleGroup.CALVES,
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION
        ),

        // ABS
        Exercise(
            name = "Plank",
            description = "Isometric core exercise",
            muscleGroup = MuscleGroup.ABS,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER
        ),
        Exercise(
            name = "Crunches",
            description = "Basic ab exercise",
            muscleGroup = MuscleGroup.ABS,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER
        ),
        Exercise(
            name = "Hanging Leg Raise",
            description = "Lower ab focused",
            muscleGroup = MuscleGroup.ABS,
            secondaryMuscleGroups = listOf(MuscleGroup.HIP_FLEXORS),
            equipment = Equipment.PULL_UP_BAR,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Cable Crunch",
            description = "Weighted ab exercise",
            muscleGroup = MuscleGroup.ABS,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Ab Wheel Rollout",
            description = "Advanced core exercise",
            muscleGroup = MuscleGroup.ABS,
            equipment = Equipment.OTHER,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.ADVANCED
        ),
        Exercise(
            name = "Russian Twist",
            description = "Oblique focused rotation",
            muscleGroup = MuscleGroup.OBLIQUES,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Side Plank",
            description = "Oblique isometric hold",
            muscleGroup = MuscleGroup.OBLIQUES,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION
        ),
        
        // ADDITIONAL CHEST EXERCISES
        Exercise(
            name = "Decline Bench Press",
            description = "Lower chest focused press",
            muscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Machine Chest Press",
            description = "Guided chest pressing motion",
            muscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.BEGINNER
        ),
        Exercise(
            name = "Pec Deck",
            description = "Machine chest fly",
            muscleGroup = MuscleGroup.CHEST,
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Incline Barbell Press",
            description = "Upper chest barbell press",
            muscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Decline Dumbbell Press",
            description = "Lower chest dumbbell press",
            muscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Low Cable Crossover",
            description = "Upper chest cable fly",
            muscleGroup = MuscleGroup.CHEST,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION
        ),
        
        // ADDITIONAL BACK EXERCISES
        Exercise(
            name = "Chin-ups",
            description = "Underhand grip pull-up",
            muscleGroup = MuscleGroup.LATS,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS, MuscleGroup.BACK),
            equipment = Equipment.PULL_UP_BAR,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "T-Bar Row",
            description = "Landmine rowing movement",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS, MuscleGroup.LATS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Pendlay Row",
            description = "Explosive barbell row from floor",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS, MuscleGroup.LATS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.ADVANCED
        ),
        Exercise(
            name = "Meadows Row",
            description = "Single arm landmine row",
            muscleGroup = MuscleGroup.LATS,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Straight Arm Pulldown",
            description = "Lat isolation with straight arms",
            muscleGroup = MuscleGroup.LATS,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Machine Row",
            description = "Seated machine rowing",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS),
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.BEGINNER
        ),
        Exercise(
            name = "Inverted Row",
            description = "Bodyweight horizontal pull",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS),
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.BEGINNER
        ),
        Exercise(
            name = "Rack Pulls",
            description = "Partial deadlift from rack",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.TRAPS, MuscleGroup.GLUTES),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Shrugs",
            description = "Trap isolation exercise",
            muscleGroup = MuscleGroup.TRAPS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Barbell Shrugs",
            description = "Heavy trap exercise",
            muscleGroup = MuscleGroup.TRAPS,
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        
        // ADDITIONAL SHOULDER EXERCISES
        Exercise(
            name = "Machine Shoulder Press",
            description = "Guided shoulder press",
            muscleGroup = MuscleGroup.SHOULDERS,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS),
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.BEGINNER
        ),
        Exercise(
            name = "Cable Lateral Raise",
            description = "Constant tension lateral raise",
            muscleGroup = MuscleGroup.SHOULDERS,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Upright Row",
            description = "Shoulder and trap exercise",
            muscleGroup = MuscleGroup.SHOULDERS,
            secondaryMuscleGroups = listOf(MuscleGroup.TRAPS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Rear Delt Cable Fly",
            description = "Cable rear delt isolation",
            muscleGroup = MuscleGroup.SHOULDERS,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Lu Raises",
            description = "Front to lateral raise combo",
            muscleGroup = MuscleGroup.SHOULDERS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Landmine Press",
            description = "Angled pressing movement",
            muscleGroup = MuscleGroup.SHOULDERS,
            secondaryMuscleGroups = listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        
        // ADDITIONAL ARM EXERCISES
        Exercise(
            name = "Concentration Curl",
            description = "Isolated single arm curl",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Spider Curl",
            description = "Incline bench bicep curl",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "EZ Bar Curl",
            description = "Angled grip bicep curl",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.EZ_BAR,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Reverse Curl",
            description = "Forearm and bicep exercise",
            muscleGroup = MuscleGroup.FOREARMS,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Wrist Curl",
            description = "Forearm flexor exercise",
            muscleGroup = MuscleGroup.FOREARMS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Reverse Wrist Curl",
            description = "Forearm extensor exercise",
            muscleGroup = MuscleGroup.FOREARMS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Rope Pushdown",
            description = "Tricep rope extension",
            muscleGroup = MuscleGroup.TRICEPS,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Kickbacks",
            description = "Tricep kickback exercise",
            muscleGroup = MuscleGroup.TRICEPS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "JM Press",
            description = "Hybrid bench/skull crusher",
            muscleGroup = MuscleGroup.TRICEPS,
            secondaryMuscleGroups = listOf(MuscleGroup.CHEST),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.ADVANCED
        ),
        
        // ADDITIONAL LEG EXERCISES
        Exercise(
            name = "Goblet Squat",
            description = "Front loaded squat variation",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.BEGINNER
        ),
        Exercise(
            name = "Sumo Deadlift",
            description = "Wide stance deadlift",
            muscleGroup = MuscleGroup.GLUTES,
            secondaryMuscleGroups = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.QUADS, MuscleGroup.BACK),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Trap Bar Deadlift",
            description = "Hex bar deadlift",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.BACK),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Walking Lunges",
            description = "Dynamic lunge movement",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Step Ups",
            description = "Single leg step exercise",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Sissy Squat",
            description = "Quad isolation bodyweight",
            muscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.ADVANCED
        ),
        Exercise(
            name = "Nordic Curl",
            description = "Eccentric hamstring exercise",
            muscleGroup = MuscleGroup.HAMSTRINGS,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.ADVANCED
        ),
        Exercise(
            name = "Glute Ham Raise",
            description = "Posterior chain exercise",
            muscleGroup = MuscleGroup.HAMSTRINGS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Single Leg Deadlift",
            description = "Unilateral hip hinge",
            muscleGroup = MuscleGroup.HAMSTRINGS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Reverse Lunge",
            description = "Backward stepping lunge",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Hip Abduction",
            description = "Outer thigh exercise",
            muscleGroup = MuscleGroup.GLUTES,
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Hip Adduction",
            description = "Inner thigh exercise",
            muscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Donkey Calf Raise",
            description = "Bent over calf raise",
            muscleGroup = MuscleGroup.CALVES,
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Single Leg Calf Raise",
            description = "Unilateral calf exercise",
            muscleGroup = MuscleGroup.CALVES,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION
        ),
        
        // ADDITIONAL CORE EXERCISES
        Exercise(
            name = "Dead Bug",
            description = "Core stability exercise",
            muscleGroup = MuscleGroup.ABS,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER
        ),
        Exercise(
            name = "Bird Dog",
            description = "Core and back stability",
            muscleGroup = MuscleGroup.LOWER_BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.ABS, MuscleGroup.GLUTES),
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER
        ),
        Exercise(
            name = "Mountain Climbers",
            description = "Dynamic core exercise",
            muscleGroup = MuscleGroup.ABS,
            secondaryMuscleGroups = listOf(MuscleGroup.HIP_FLEXORS),
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.CARDIO,
            difficulty = Difficulty.BEGINNER
        ),
        Exercise(
            name = "Bicycle Crunches",
            description = "Rotational ab exercise",
            muscleGroup = MuscleGroup.ABS,
            secondaryMuscleGroups = listOf(MuscleGroup.OBLIQUES),
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "V-Ups",
            description = "Full body crunch",
            muscleGroup = MuscleGroup.ABS,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Toe Touches",
            description = "Upper ab exercise",
            muscleGroup = MuscleGroup.ABS,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER
        ),
        Exercise(
            name = "Reverse Crunch",
            description = "Lower ab focused",
            muscleGroup = MuscleGroup.ABS,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Pallof Press",
            description = "Anti-rotation core exercise",
            muscleGroup = MuscleGroup.ABS,
            secondaryMuscleGroups = listOf(MuscleGroup.OBLIQUES),
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Woodchoppers",
            description = "Rotational cable exercise",
            muscleGroup = MuscleGroup.OBLIQUES,
            secondaryMuscleGroups = listOf(MuscleGroup.ABS),
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Back Extension",
            description = "Lower back strengthening",
            muscleGroup = MuscleGroup.LOWER_BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION
        ),
        Exercise(
            name = "Superman",
            description = "Prone back extension",
            muscleGroup = MuscleGroup.LOWER_BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER
        ),
        
        // FULL BODY / COMPOUND
        Exercise(
            name = "Clean and Press",
            description = "Olympic lift variation",
            muscleGroup = MuscleGroup.FULL_BODY,
            secondaryMuscleGroups = listOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK, MuscleGroup.QUADS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.ADVANCED
        ),
        Exercise(
            name = "Thrusters",
            description = "Squat to press combo",
            muscleGroup = MuscleGroup.FULL_BODY,
            secondaryMuscleGroups = listOf(MuscleGroup.QUADS, MuscleGroup.SHOULDERS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Burpees",
            description = "Full body cardio exercise",
            muscleGroup = MuscleGroup.FULL_BODY,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.CARDIO
        ),
        Exercise(
            name = "Kettlebell Swing",
            description = "Hip hinge power exercise",
            muscleGroup = MuscleGroup.GLUTES,
            secondaryMuscleGroups = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.BACK),
            equipment = Equipment.KETTLEBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Turkish Get Up",
            description = "Full body stability exercise",
            muscleGroup = MuscleGroup.FULL_BODY,
            equipment = Equipment.KETTLEBELL,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.ADVANCED
        ),
        Exercise(
            name = "Farmers Walk",
            description = "Loaded carry exercise",
            muscleGroup = MuscleGroup.FULL_BODY,
            secondaryMuscleGroups = listOf(MuscleGroup.FOREARMS, MuscleGroup.TRAPS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND
        ),
        Exercise(
            name = "Sled Push",
            description = "Lower body power exercise",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.CALVES),
            equipment = Equipment.OTHER,
            exerciseType = ExerciseType.CARDIO
        ),
        Exercise(
            name = "Battle Ropes",
            description = "Upper body cardio",
            muscleGroup = MuscleGroup.SHOULDERS,
            secondaryMuscleGroups = listOf(MuscleGroup.ABS),
            equipment = Equipment.OTHER,
            exerciseType = ExerciseType.CARDIO
        ),
        Exercise(
            name = "Box Jumps",
            description = "Plyometric leg exercise",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.CALVES),
            equipment = Equipment.OTHER,
            exerciseType = ExerciseType.PLYOMETRIC
        ),
        Exercise(
            name = "Jump Squats",
            description = "Explosive squat variation",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.PLYOMETRIC
        )
    )
    
    fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): List<Exercise> {
        return getDefaultExercises().filter { 
            it.muscleGroup == muscleGroup || muscleGroup in it.secondaryMuscleGroups 
        }
    }
    
    fun getExercisesByEquipment(equipment: Equipment): List<Exercise> {
        return getDefaultExercises().filter { it.equipment == equipment }
    }
    
    fun searchExercises(query: String): List<Exercise> {
        val lowerQuery = query.lowercase()
        return getDefaultExercises().filter {
            it.name.lowercase().contains(lowerQuery) ||
            it.muscleGroup.name.lowercase().contains(lowerQuery) ||
            it.equipment.name.lowercase().contains(lowerQuery)
        }
    }
}
