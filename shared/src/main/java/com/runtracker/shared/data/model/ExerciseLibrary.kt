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
            ),
            videoFileName = "barbell-bench-press"
        ),
        Exercise(
            name = "Incline Dumbbell Press",
            description = "Upper chest focused press",
            muscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "incline-dumbbell-press"
        ),
        Exercise(
            name = "Dumbbell Flyes",
            description = "Chest isolation exercise",
            muscleGroup = MuscleGroup.CHEST,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "dumbbell-flyes"
        ),
        Exercise(
            name = "Cable Crossover",
            description = "Cable chest isolation",
            muscleGroup = MuscleGroup.CHEST,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "cable-crossover"
        ),
        Exercise(
            name = "Push-ups",
            description = "Bodyweight chest exercise",
            muscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.BEGINNER,
            videoFileName = "push-ups"
        ),
        Exercise(
            name = "Chest Dips",
            description = "Lower chest focused dip",
            muscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS),
            equipment = Equipment.DIP_BARS,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "chest-dips"
        ),

        // BACK
        Exercise(
            name = "Barbell Deadlift",
            description = "King of compound exercises",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.ADVANCED,
            videoFileName = "barbell-deadlift"
        ),
        Exercise(
            name = "Barbell Row",
            description = "Horizontal pulling movement",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS, MuscleGroup.LATS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "barbell-row"
        ),
        Exercise(
            name = "Pull-ups",
            description = "Vertical pulling bodyweight exercise",
            muscleGroup = MuscleGroup.LATS,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS, MuscleGroup.BACK),
            equipment = Equipment.PULL_UP_BAR,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "pull-ups"
        ),
        Exercise(
            name = "Lat Pulldown",
            description = "Machine lat exercise",
            muscleGroup = MuscleGroup.LATS,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS),
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "lat-pulldown"
        ),
        Exercise(
            name = "Seated Cable Row",
            description = "Horizontal cable pull",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS, MuscleGroup.LATS),
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "seated-cable-row"
        ),
        Exercise(
            name = "Dumbbell Row",
            description = "Single arm rowing movement",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS, MuscleGroup.LATS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "dumbbell-row"
        ),
        Exercise(
            name = "Face Pulls",
            description = "Rear delt and upper back exercise",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.SHOULDERS),
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "face-pulls"
        ),

        // SHOULDERS
        Exercise(
            name = "Overhead Press",
            description = "Standing barbell shoulder press",
            muscleGroup = MuscleGroup.SHOULDERS,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "overhead-press"
        ),
        Exercise(
            name = "Dumbbell Shoulder Press",
            description = "Seated or standing dumbbell press",
            muscleGroup = MuscleGroup.SHOULDERS,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "dumbbell-shoulder-press"
        ),
        Exercise(
            name = "Lateral Raises",
            description = "Side delt isolation",
            muscleGroup = MuscleGroup.SHOULDERS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "lateral-raises"
        ),
        Exercise(
            name = "Front Raises",
            description = "Front delt isolation",
            muscleGroup = MuscleGroup.SHOULDERS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "front-raises"
        ),
        Exercise(
            name = "Reverse Flyes",
            description = "Rear delt isolation",
            muscleGroup = MuscleGroup.SHOULDERS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "reverse-flyes"
        ),
        Exercise(
            name = "Arnold Press",
            description = "Rotational shoulder press",
            muscleGroup = MuscleGroup.SHOULDERS,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "arnold-press"
        ),

        // BICEPS
        Exercise(
            name = "Barbell Curl",
            description = "Classic bicep exercise",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "barbell-curl"
        ),
        Exercise(
            name = "Dumbbell Curl",
            description = "Alternating or simultaneous curls",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "dumbbell-curl"
        ),
        Exercise(
            name = "Hammer Curls",
            description = "Neutral grip curls",
            muscleGroup = MuscleGroup.BICEPS,
            secondaryMuscleGroups = listOf(MuscleGroup.FOREARMS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "hammer-curls"
        ),
        Exercise(
            name = "Preacher Curls",
            description = "Isolated bicep curl on preacher bench",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.EZ_BAR,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "preacher-curls"
        ),
        Exercise(
            name = "Cable Curls",
            description = "Constant tension bicep curl",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "cable-curls"
        ),
        Exercise(
            name = "Incline Dumbbell Curl",
            description = "Stretched position bicep curl",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "incline-dumbbell-curl"
        ),

        // TRICEPS
        Exercise(
            name = "Close Grip Bench Press",
            description = "Tricep focused bench press",
            muscleGroup = MuscleGroup.TRICEPS,
            secondaryMuscleGroups = listOf(MuscleGroup.CHEST),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "close-grip-bench-press"
        ),
        Exercise(
            name = "Tricep Pushdown",
            description = "Cable tricep isolation",
            muscleGroup = MuscleGroup.TRICEPS,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "tricep-pushdown"
        ),
        Exercise(
            name = "Overhead Tricep Extension",
            description = "Long head tricep focus",
            muscleGroup = MuscleGroup.TRICEPS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "overhead-tricep-extension"
        ),
        Exercise(
            name = "Skull Crushers",
            description = "Lying tricep extension",
            muscleGroup = MuscleGroup.TRICEPS,
            equipment = Equipment.EZ_BAR,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "skull-crushers"
        ),
        Exercise(
            name = "Tricep Dips",
            description = "Bodyweight tricep exercise",
            muscleGroup = MuscleGroup.TRICEPS,
            secondaryMuscleGroups = listOf(MuscleGroup.CHEST),
            equipment = Equipment.DIP_BARS,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "tricep-dips"
        ),
        Exercise(
            name = "Diamond Push-ups",
            description = "Tricep focused push-up",
            muscleGroup = MuscleGroup.TRICEPS,
            secondaryMuscleGroups = listOf(MuscleGroup.CHEST),
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "diamond-push-ups"
        ),

        // LEGS - QUADS
        Exercise(
            name = "Barbell Squat",
            description = "King of leg exercises",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "barbell-squat"
        ),
        Exercise(
            name = "Front Squat",
            description = "Quad dominant squat variation",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.ADVANCED,
            videoFileName = "front-squat"
        ),
        Exercise(
            name = "Leg Press",
            description = "Machine compound leg exercise",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "leg-press"
        ),
        Exercise(
            name = "Leg Extension",
            description = "Quad isolation",
            muscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "leg-extension"
        ),
        Exercise(
            name = "Lunges",
            description = "Unilateral leg exercise",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "lunges"
        ),
        Exercise(
            name = "Bulgarian Split Squat",
            description = "Single leg squat variation",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "bulgarian-split-squat"
        ),
        Exercise(
            name = "Hack Squat",
            description = "Machine squat variation",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "hack-squat"
        ),

        // LEGS - HAMSTRINGS
        Exercise(
            name = "Romanian Deadlift",
            description = "Hip hinge hamstring exercise",
            muscleGroup = MuscleGroup.HAMSTRINGS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "romanian-deadlift"
        ),
        Exercise(
            name = "Leg Curl",
            description = "Hamstring isolation",
            muscleGroup = MuscleGroup.HAMSTRINGS,
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "leg-curl"
        ),
        Exercise(
            name = "Stiff Leg Deadlift",
            description = "Straight leg deadlift variation",
            muscleGroup = MuscleGroup.HAMSTRINGS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "stiff-leg-deadlift"
        ),
        Exercise(
            name = "Good Mornings",
            description = "Hip hinge with bar on back",
            muscleGroup = MuscleGroup.HAMSTRINGS,
            secondaryMuscleGroups = listOf(MuscleGroup.LOWER_BACK, MuscleGroup.GLUTES),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.ADVANCED,
            videoFileName = "good-mornings"
        ),

        // LEGS - GLUTES
        Exercise(
            name = "Hip Thrust",
            description = "Primary glute exercise",
            muscleGroup = MuscleGroup.GLUTES,
            secondaryMuscleGroups = listOf(MuscleGroup.HAMSTRINGS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "hip-thrust"
        ),
        Exercise(
            name = "Glute Bridge",
            description = "Bodyweight glute activation",
            muscleGroup = MuscleGroup.GLUTES,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER,
            videoFileName = "glute-bridge"
        ),
        Exercise(
            name = "Cable Kickbacks",
            description = "Glute isolation with cable",
            muscleGroup = MuscleGroup.GLUTES,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "cable-kickbacks"
        ),

        // LEGS - CALVES
        Exercise(
            name = "Standing Calf Raise",
            description = "Gastrocnemius focused",
            muscleGroup = MuscleGroup.CALVES,
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "standing-calf-raise"
        ),
        Exercise(
            name = "Seated Calf Raise",
            description = "Soleus focused",
            muscleGroup = MuscleGroup.CALVES,
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "seated-calf-raise"
        ),

        // ABS
        Exercise(
            name = "Plank",
            description = "Isometric core exercise",
            muscleGroup = MuscleGroup.ABS,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER,
            videoFileName = "plank"
        ),
        Exercise(
            name = "Crunches",
            description = "Basic ab exercise",
            muscleGroup = MuscleGroup.ABS,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER,
            videoFileName = "crunches"
        ),
        Exercise(
            name = "Hanging Leg Raise",
            description = "Lower ab focused",
            muscleGroup = MuscleGroup.ABS,
            secondaryMuscleGroups = listOf(MuscleGroup.HIP_FLEXORS),
            equipment = Equipment.PULL_UP_BAR,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "hanging-leg-raise"
        ),
        Exercise(
            name = "Cable Crunch",
            description = "Weighted ab exercise",
            muscleGroup = MuscleGroup.ABS,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "cable-crunch"
        ),
        Exercise(
            name = "Ab Wheel Rollout",
            description = "Advanced core exercise",
            muscleGroup = MuscleGroup.ABS,
            equipment = Equipment.OTHER,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.ADVANCED,
            videoFileName = "ab-wheel-rollout"
        ),
        Exercise(
            name = "Russian Twist",
            description = "Oblique focused rotation",
            muscleGroup = MuscleGroup.OBLIQUES,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "russian-twist"
        ),
        Exercise(
            name = "Side Plank",
            description = "Oblique isometric hold",
            muscleGroup = MuscleGroup.OBLIQUES,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "side-plank"
        ),
        
        // ADDITIONAL CHEST EXERCISES
        Exercise(
            name = "Decline Bench Press",
            description = "Lower chest focused press",
            muscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "decline-bench-press"
        ),
        Exercise(
            name = "Machine Chest Press",
            description = "Guided chest pressing motion",
            muscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.BEGINNER,
            videoFileName = "machine-chest-press"
        ),
        Exercise(
            name = "Pec Deck",
            description = "Machine chest fly",
            muscleGroup = MuscleGroup.CHEST,
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "pec-deck"
        ),
        Exercise(
            name = "Incline Barbell Press",
            description = "Upper chest barbell press",
            muscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "incline-barbell-press"
        ),
        Exercise(
            name = "Decline Dumbbell Press",
            description = "Lower chest dumbbell press",
            muscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "decline-dumbbell-press"
        ),
        Exercise(
            name = "Low Cable Crossover",
            description = "Upper chest cable fly",
            muscleGroup = MuscleGroup.CHEST,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "low-cable-crossover"
        ),
        
        // ADDITIONAL BACK EXERCISES
        Exercise(
            name = "Chin-ups",
            description = "Underhand grip pull-up",
            muscleGroup = MuscleGroup.LATS,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS, MuscleGroup.BACK),
            equipment = Equipment.PULL_UP_BAR,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "chin-ups"
        ),
        Exercise(
            name = "T-Bar Row",
            description = "Landmine rowing movement",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS, MuscleGroup.LATS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "t-bar-row"
        ),
        Exercise(
            name = "Pendlay Row",
            description = "Explosive barbell row from floor",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS, MuscleGroup.LATS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.ADVANCED,
            videoFileName = "pendlay-row"
        ),
        Exercise(
            name = "Meadows Row",
            description = "Single arm landmine row",
            muscleGroup = MuscleGroup.LATS,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "meadows-row"
        ),
        Exercise(
            name = "Straight Arm Pulldown",
            description = "Lat isolation with straight arms",
            muscleGroup = MuscleGroup.LATS,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "straight-arm-pulldown"
        ),
        Exercise(
            name = "Machine Row",
            description = "Seated machine rowing",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS),
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.BEGINNER,
            videoFileName = "machine-row"
        ),
        Exercise(
            name = "Inverted Row",
            description = "Bodyweight horizontal pull",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS),
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.BEGINNER,
            videoFileName = "inverted-row"
        ),
        Exercise(
            name = "Rack Pulls",
            description = "Partial deadlift from rack",
            muscleGroup = MuscleGroup.BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.TRAPS, MuscleGroup.GLUTES),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "rack-pulls"
        ),
        Exercise(
            name = "Shrugs",
            description = "Trap isolation exercise",
            muscleGroup = MuscleGroup.TRAPS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "shrugs"
        ),
        Exercise(
            name = "Barbell Shrugs",
            description = "Heavy trap exercise",
            muscleGroup = MuscleGroup.TRAPS,
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "barbell-shrugs"
        ),
        
        // ADDITIONAL SHOULDER EXERCISES
        Exercise(
            name = "Machine Shoulder Press",
            description = "Guided shoulder press",
            muscleGroup = MuscleGroup.SHOULDERS,
            secondaryMuscleGroups = listOf(MuscleGroup.TRICEPS),
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.BEGINNER,
            videoFileName = "machine-shoulder-press"
        ),
        Exercise(
            name = "Cable Lateral Raise",
            description = "Constant tension lateral raise",
            muscleGroup = MuscleGroup.SHOULDERS,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "cable-lateral-raise"
        ),
        Exercise(
            name = "Upright Row",
            description = "Shoulder and trap exercise",
            muscleGroup = MuscleGroup.SHOULDERS,
            secondaryMuscleGroups = listOf(MuscleGroup.TRAPS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "upright-row"
        ),
        Exercise(
            name = "Rear Delt Cable Fly",
            description = "Cable rear delt isolation",
            muscleGroup = MuscleGroup.SHOULDERS,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "rear-delt-cable-fly"
        ),
        Exercise(
            name = "Lu Raises",
            description = "Front to lateral raise combo",
            muscleGroup = MuscleGroup.SHOULDERS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "lu-raises"
        ),
        Exercise(
            name = "Landmine Press",
            description = "Angled pressing movement",
            muscleGroup = MuscleGroup.SHOULDERS,
            secondaryMuscleGroups = listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "landmine-press"
        ),
        
        // ADDITIONAL ARM EXERCISES
        Exercise(
            name = "Concentration Curl",
            description = "Isolated single arm curl",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "concentration-curl"
        ),
        Exercise(
            name = "Spider Curl",
            description = "Incline bench bicep curl",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "spider-curl"
        ),
        Exercise(
            name = "EZ Bar Curl",
            description = "Angled grip bicep curl",
            muscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.EZ_BAR,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "ez-bar-curl"
        ),
        Exercise(
            name = "Reverse Curl",
            description = "Forearm and bicep exercise",
            muscleGroup = MuscleGroup.FOREARMS,
            secondaryMuscleGroups = listOf(MuscleGroup.BICEPS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "reverse-curl"
        ),
        Exercise(
            name = "Wrist Curl",
            description = "Forearm flexor exercise",
            muscleGroup = MuscleGroup.FOREARMS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "wrist-curl"
        ),
        Exercise(
            name = "Reverse Wrist Curl",
            description = "Forearm extensor exercise",
            muscleGroup = MuscleGroup.FOREARMS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "reverse-wrist-curl"
        ),
        Exercise(
            name = "Rope Pushdown",
            description = "Tricep rope extension",
            muscleGroup = MuscleGroup.TRICEPS,
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "rope-pushdown"
        ),
        Exercise(
            name = "Kickbacks",
            description = "Tricep kickback exercise",
            muscleGroup = MuscleGroup.TRICEPS,
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "kickbacks"
        ),
        Exercise(
            name = "JM Press",
            description = "Hybrid bench/skull crusher",
            muscleGroup = MuscleGroup.TRICEPS,
            secondaryMuscleGroups = listOf(MuscleGroup.CHEST),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.ADVANCED,
            videoFileName = "jm-press"
        ),
        
        // ADDITIONAL LEG EXERCISES
        Exercise(
            name = "Goblet Squat",
            description = "Front loaded squat variation",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.BEGINNER,
            videoFileName = "goblet-squat"
        ),
        Exercise(
            name = "Sumo Deadlift",
            description = "Wide stance deadlift",
            muscleGroup = MuscleGroup.GLUTES,
            secondaryMuscleGroups = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.QUADS, MuscleGroup.BACK),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "sumo-deadlift"
        ),
        Exercise(
            name = "Trap Bar Deadlift",
            description = "Hex bar deadlift",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.BACK),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "trap-bar-deadlift"
        ),
        Exercise(
            name = "Walking Lunges",
            description = "Dynamic lunge movement",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "walking-lunges"
        ),
        Exercise(
            name = "Step Ups",
            description = "Single leg step exercise",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "step-ups"
        ),
        Exercise(
            name = "Sissy Squat",
            description = "Quad isolation bodyweight",
            muscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.ADVANCED,
            videoFileName = "sissy-squat"
        ),
        Exercise(
            name = "Nordic Curl",
            description = "Eccentric hamstring exercise",
            muscleGroup = MuscleGroup.HAMSTRINGS,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.ADVANCED,
            videoFileName = "nordic-curl"
        ),
        Exercise(
            name = "Glute Ham Raise",
            description = "Posterior chain exercise",
            muscleGroup = MuscleGroup.HAMSTRINGS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "glute-ham-raise"
        ),
        Exercise(
            name = "Single Leg Deadlift",
            description = "Unilateral hip hinge",
            muscleGroup = MuscleGroup.HAMSTRINGS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "single-leg-deadlift"
        ),
        Exercise(
            name = "Reverse Lunge",
            description = "Backward stepping lunge",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "reverse-lunge"
        ),
        Exercise(
            name = "Hip Abduction",
            description = "Outer thigh exercise",
            muscleGroup = MuscleGroup.GLUTES,
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "hip-abduction"
        ),
        Exercise(
            name = "Hip Adduction",
            description = "Inner thigh exercise",
            muscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "hip-adduction"
        ),
        Exercise(
            name = "Donkey Calf Raise",
            description = "Bent over calf raise",
            muscleGroup = MuscleGroup.CALVES,
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "donkey-calf-raise"
        ),
        Exercise(
            name = "Single Leg Calf Raise",
            description = "Unilateral calf exercise",
            muscleGroup = MuscleGroup.CALVES,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "single-leg-calf-raise"
        ),
        
        // ADDITIONAL CORE EXERCISES
        Exercise(
            name = "Dead Bug",
            description = "Core stability exercise",
            muscleGroup = MuscleGroup.ABS,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER,
            videoFileName = "dead-bug"
        ),
        Exercise(
            name = "Bird Dog",
            description = "Core and back stability",
            muscleGroup = MuscleGroup.LOWER_BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.ABS, MuscleGroup.GLUTES),
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER,
            videoFileName = "bird-dog"
        ),
        Exercise(
            name = "Mountain Climbers",
            description = "Dynamic core exercise",
            muscleGroup = MuscleGroup.ABS,
            secondaryMuscleGroups = listOf(MuscleGroup.HIP_FLEXORS),
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.CARDIO,
            difficulty = Difficulty.BEGINNER,
            videoFileName = "mountain-climbers"
        ),
        Exercise(
            name = "Bicycle Crunches",
            description = "Rotational ab exercise",
            muscleGroup = MuscleGroup.ABS,
            secondaryMuscleGroups = listOf(MuscleGroup.OBLIQUES),
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "bicycle-crunches"
        ),
        Exercise(
            name = "V-Ups",
            description = "Full body crunch",
            muscleGroup = MuscleGroup.ABS,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "v-ups"
        ),
        Exercise(
            name = "Toe Touches",
            description = "Upper ab exercise",
            muscleGroup = MuscleGroup.ABS,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER,
            videoFileName = "toe-touches"
        ),
        Exercise(
            name = "Reverse Crunch",
            description = "Lower ab focused",
            muscleGroup = MuscleGroup.ABS,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "reverse-crunch"
        ),
        Exercise(
            name = "Pallof Press",
            description = "Anti-rotation core exercise",
            muscleGroup = MuscleGroup.ABS,
            secondaryMuscleGroups = listOf(MuscleGroup.OBLIQUES),
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "pallof-press"
        ),
        Exercise(
            name = "Woodchoppers",
            description = "Rotational cable exercise",
            muscleGroup = MuscleGroup.OBLIQUES,
            secondaryMuscleGroups = listOf(MuscleGroup.ABS),
            equipment = Equipment.CABLE,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "woodchoppers"
        ),
        Exercise(
            name = "Back Extension",
            description = "Lower back strengthening",
            muscleGroup = MuscleGroup.LOWER_BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.MACHINE,
            exerciseType = ExerciseType.ISOLATION,
            videoFileName = "back-extension"
        ),
        Exercise(
            name = "Superman",
            description = "Prone back extension",
            muscleGroup = MuscleGroup.LOWER_BACK,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.ISOLATION,
            difficulty = Difficulty.BEGINNER,
            videoFileName = "superman"
        ),
        
        // FULL BODY / COMPOUND
        Exercise(
            name = "Clean and Press",
            description = "Olympic lift variation",
            muscleGroup = MuscleGroup.FULL_BODY,
            secondaryMuscleGroups = listOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK, MuscleGroup.QUADS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.ADVANCED,
            videoFileName = "clean-and-press"
        ),
        Exercise(
            name = "Thrusters",
            description = "Squat to press combo",
            muscleGroup = MuscleGroup.FULL_BODY,
            secondaryMuscleGroups = listOf(MuscleGroup.QUADS, MuscleGroup.SHOULDERS),
            equipment = Equipment.BARBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "thrusters"
        ),
        Exercise(
            name = "Burpees",
            description = "Full body cardio exercise",
            muscleGroup = MuscleGroup.FULL_BODY,
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.CARDIO,
            videoFileName = "burpees"
        ),
        Exercise(
            name = "Kettlebell Swing",
            description = "Hip hinge power exercise",
            muscleGroup = MuscleGroup.GLUTES,
            secondaryMuscleGroups = listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.BACK),
            equipment = Equipment.KETTLEBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "kettlebell-swing"
        ),
        Exercise(
            name = "Turkish Get Up",
            description = "Full body stability exercise",
            muscleGroup = MuscleGroup.FULL_BODY,
            equipment = Equipment.KETTLEBELL,
            exerciseType = ExerciseType.COMPOUND,
            difficulty = Difficulty.ADVANCED,
            videoFileName = "turkish-get-up"
        ),
        Exercise(
            name = "Farmers Walk",
            description = "Loaded carry exercise",
            muscleGroup = MuscleGroup.FULL_BODY,
            secondaryMuscleGroups = listOf(MuscleGroup.FOREARMS, MuscleGroup.TRAPS),
            equipment = Equipment.DUMBBELL,
            exerciseType = ExerciseType.COMPOUND,
            videoFileName = "farmers-walk"
        ),
        Exercise(
            name = "Sled Push",
            description = "Lower body power exercise",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.CALVES),
            equipment = Equipment.OTHER,
            exerciseType = ExerciseType.CARDIO,
            videoFileName = "sled-push"
        ),
        Exercise(
            name = "Battle Ropes",
            description = "Upper body cardio",
            muscleGroup = MuscleGroup.SHOULDERS,
            secondaryMuscleGroups = listOf(MuscleGroup.ABS),
            equipment = Equipment.OTHER,
            exerciseType = ExerciseType.CARDIO,
            videoFileName = "battle-ropes"
        ),
        Exercise(
            name = "Box Jumps",
            description = "Plyometric leg exercise",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES, MuscleGroup.CALVES),
            equipment = Equipment.OTHER,
            exerciseType = ExerciseType.PLYOMETRIC,
            videoFileName = "box-jumps"
        ),
        Exercise(
            name = "Jump Squats",
            description = "Explosive squat variation",
            muscleGroup = MuscleGroup.QUADS,
            secondaryMuscleGroups = listOf(MuscleGroup.GLUTES),
            equipment = Equipment.BODYWEIGHT,
            exerciseType = ExerciseType.PLYOMETRIC,
            videoFileName = "jump-squats"
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
