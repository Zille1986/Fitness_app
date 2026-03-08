package com.runtracker.shared.data.model

/**
 * HIIT (High Intensity Interval Training) models shared between phone and watch.
 */

enum class HIITDifficulty(val displayName: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}

data class HIITExercise(
    val id: String,
    val name: String,
    val description: String,
    val muscleGroups: List<String>,
    val difficulty: HIITDifficulty,
    val caloriesPerMinute: Int, // approximate for ~160lb person
    val videoFileName: String? = null
)

data class HIITWorkoutExercise(
    val exercise: HIITExercise,
    val durationOverrideSec: Int? = null // per-exercise override, null = use template default
)

data class HIITPhaseStep(
    val name: String,
    val description: String,
    val videoFileName: String? = null
)

data class HIITWorkoutTemplate(
    val id: String,
    val name: String,
    val description: String,
    val difficulty: String, // e.g. "Easy", "Medium-Hard", "Hard"
    val exercises: List<HIITWorkoutExercise>,
    val workDurationSec: Int,
    val restDurationSec: Int,
    val rounds: Int,
    val warmupSec: Int,
    val cooldownSec: Int,
    val warmupSteps: List<HIITPhaseStep> = emptyList(),
    val cooldownSteps: List<HIITPhaseStep> = emptyList()
) {
    /** Estimated total duration in seconds */
    val estimatedDurationSec: Int
        get() = warmupSec + cooldownSec +
                (exercises.size * (workDurationSec + restDurationSec) * rounds) -
                (restDurationSec) // no rest after last exercise in last round

    /** Formatted duration string */
    val formattedDuration: String
        get() {
            val mins = estimatedDurationSec / 60
            return if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
        }
}

/**
 * Library of all 20 HIIT exercises and 6 pre-built workout templates.
 */
object HIITExerciseLibrary {

    // ── Exercises ───────────────────────────────────────────────────────

    val BURPEES = HIITExercise(
        id = "burpees",
        name = "Burpees",
        description = "Drop into a push-up, jump feet to hands, then explosively jump up with arms overhead.",
        muscleGroups = listOf("Full body"),
        difficulty = HIITDifficulty.HARD,
        caloriesPerMinute = 14,
        videoFileName = "hiit-burpees"
    )

    val JUMP_SQUATS = HIITExercise(
        id = "jump_squats",
        name = "Jump Squats",
        description = "Squat deep, then explode upward into a jump, landing softly back into the squat position.",
        muscleGroups = listOf("Quads", "Glutes"),
        difficulty = HIITDifficulty.MEDIUM,
        caloriesPerMinute = 12,
        videoFileName = "hiit-jump-squats"
    )

    val MOUNTAIN_CLIMBERS = HIITExercise(
        id = "mountain_climbers",
        name = "Mountain Climbers",
        description = "In a push-up position, rapidly alternate driving each knee toward your chest.",
        muscleGroups = listOf("Core", "Shoulders"),
        difficulty = HIITDifficulty.MEDIUM,
        caloriesPerMinute = 11,
        videoFileName = "hiit-mountain-climbers"
    )

    val PUSHUP_SHOULDER_TAP = HIITExercise(
        id = "pushup_shoulder_tap",
        name = "Push-Up to Shoulder Tap",
        description = "Perform a push-up, then at the top, tap each shoulder with the opposite hand while keeping hips stable.",
        muscleGroups = listOf("Chest", "Core", "Shoulders"),
        difficulty = HIITDifficulty.HARD,
        caloriesPerMinute = 10,
        videoFileName = "hiit-pushup-shoulder-tap"
    )

    val HIGH_KNEES = HIITExercise(
        id = "high_knees",
        name = "High Knees",
        description = "Run in place, driving each knee up to hip height as fast as possible while pumping your arms.",
        muscleGroups = listOf("Hip flexors", "Quads"),
        difficulty = HIITDifficulty.EASY,
        caloriesPerMinute = 10,
        videoFileName = "hiit-high-knees"
    )

    val PLANK_JACKS = HIITExercise(
        id = "plank_jacks",
        name = "Plank Jacks",
        description = "In a plank position, jump your feet wide apart and then back together, like a horizontal jumping jack.",
        muscleGroups = listOf("Core", "Shoulders"),
        difficulty = HIITDifficulty.MEDIUM,
        caloriesPerMinute = 9,
        videoFileName = "hiit-plank-jacks"
    )

    val SKATER_JUMPS = HIITExercise(
        id = "skater_jumps",
        name = "Skater Jumps",
        description = "Leap laterally from one foot to the other, landing on one leg and sweeping the other behind, like a speed skater.",
        muscleGroups = listOf("Glutes", "Quads"),
        difficulty = HIITDifficulty.MEDIUM,
        caloriesPerMinute = 11,
        videoFileName = "hiit-skater-jumps"
    )

    val BICYCLE_CRUNCHES = HIITExercise(
        id = "bicycle_crunches",
        name = "Bicycle Crunches",
        description = "Lie on your back, alternate bringing each elbow to the opposite knee in a pedaling motion.",
        muscleGroups = listOf("Core", "Obliques"),
        difficulty = HIITDifficulty.EASY,
        caloriesPerMinute = 7,
        videoFileName = "hiit-bicycle-crunches"
    )

    val TUCK_JUMPS = HIITExercise(
        id = "tuck_jumps",
        name = "Tuck Jumps",
        description = "Jump explosively, pulling both knees up to your chest at the peak, then land softly.",
        muscleGroups = listOf("Full body"),
        difficulty = HIITDifficulty.HARD,
        caloriesPerMinute = 14,
        videoFileName = "hiit-tuck-jumps"
    )

    val DIAMOND_PUSHUPS = HIITExercise(
        id = "diamond_pushups",
        name = "Diamond Push-Ups",
        description = "Place hands close together forming a diamond shape, then perform a push-up targeting the triceps.",
        muscleGroups = listOf("Triceps", "Chest"),
        difficulty = HIITDifficulty.HARD,
        caloriesPerMinute = 9,
        videoFileName = "hiit-diamond-pushups"
    )

    val BEAR_CRAWL = HIITExercise(
        id = "bear_crawl",
        name = "Bear Crawl",
        description = "On all fours with knees hovering, crawl forward moving opposite hand and foot together, keeping back flat.",
        muscleGroups = listOf("Shoulders", "Core", "Quads"),
        difficulty = HIITDifficulty.MEDIUM,
        caloriesPerMinute = 12,
        videoFileName = "hiit-bear-crawl"
    )

    val BOX_JUMPS = HIITExercise(
        id = "box_jumps",
        name = "Box Jumps",
        description = "Explosively jump onto an elevated surface, landing softly with both feet, then step back down.",
        muscleGroups = listOf("Quads", "Glutes", "Calves"),
        difficulty = HIITDifficulty.HARD,
        caloriesPerMinute = 13,
        videoFileName = "hiit-box-jumps"
    )

    val RUSSIAN_TWISTS = HIITExercise(
        id = "russian_twists",
        name = "Russian Twists",
        description = "Seated with feet elevated, lean back at 45 degrees and rotate your torso side to side, tapping the floor.",
        muscleGroups = listOf("Obliques", "Abs"),
        difficulty = HIITDifficulty.EASY,
        caloriesPerMinute = 7,
        videoFileName = "hiit-russian-twists"
    )

    val COMMANDO_PLANK = HIITExercise(
        id = "commando_plank",
        name = "Commando Plank",
        description = "From forearm plank, push up to high plank one arm at a time, then lower back down. Alternate leading arm.",
        muscleGroups = listOf("Triceps", "Shoulders", "Core"),
        difficulty = HIITDifficulty.MEDIUM,
        caloriesPerMinute = 10,
        videoFileName = "hiit-commando-plank"
    )

    val LATERAL_LUNGES = HIITExercise(
        id = "lateral_lunges",
        name = "Lateral Lunges",
        description = "Step wide to one side, bending that knee and sitting hips back while keeping the other leg straight. Alternate.",
        muscleGroups = listOf("Glutes", "Adductors", "Quads"),
        difficulty = HIITDifficulty.EASY,
        caloriesPerMinute = 8,
        videoFileName = "hiit-lateral-lunges"
    )

    val FLUTTER_KICKS = HIITExercise(
        id = "flutter_kicks",
        name = "Flutter Kicks",
        description = "Lie on your back, lift both legs slightly off the ground, and alternate kicking up and down rapidly.",
        muscleGroups = listOf("Lower abs", "Hip flexors"),
        difficulty = HIITDifficulty.EASY,
        caloriesPerMinute = 7,
        videoFileName = "hiit-flutter-kicks"
    )

    val PIKE_PUSHUPS = HIITExercise(
        id = "pike_pushups",
        name = "Pike Push-Ups",
        description = "In a downward-dog position with hips high, bend elbows to lower your head toward the floor, then press up.",
        muscleGroups = listOf("Shoulders", "Triceps"),
        difficulty = HIITDifficulty.HARD,
        caloriesPerMinute = 9,
        videoFileName = "hiit-pike-pushups"
    )

    val SQUAT_TO_PRESS = HIITExercise(
        id = "squat_to_press",
        name = "Squat to Press",
        description = "Perform a full squat, then as you stand explosively, press your arms overhead in one fluid motion.",
        muscleGroups = listOf("Quads", "Glutes", "Shoulders"),
        difficulty = HIITDifficulty.MEDIUM,
        caloriesPerMinute = 12,
        videoFileName = "hiit-squat-to-press"
    )

    val DEAD_BUG = HIITExercise(
        id = "dead_bug",
        name = "Dead Bug",
        description = "Lie on your back with arms up and knees at 90 degrees. Extend opposite arm and leg outward, then alternate.",
        muscleGroups = listOf("Deep core", "Abs"),
        difficulty = HIITDifficulty.EASY,
        caloriesPerMinute = 6,
        videoFileName = "hiit-dead-bug"
    )

    val MAN_MAKERS = HIITExercise(
        id = "man_makers",
        name = "Man Makers",
        description = "From standing, drop to a push-up, do a renegade row each side, jump feet to hands, and stand with press overhead.",
        muscleGroups = listOf("Full body"),
        difficulty = HIITDifficulty.HARD,
        caloriesPerMinute = 14,
        videoFileName = "hiit-man-makers"
    )

    /** All 20 exercises */
    val allExercises: List<HIITExercise> = listOf(
        BURPEES, JUMP_SQUATS, MOUNTAIN_CLIMBERS, PUSHUP_SHOULDER_TAP, HIGH_KNEES,
        PLANK_JACKS, SKATER_JUMPS, BICYCLE_CRUNCHES, TUCK_JUMPS, DIAMOND_PUSHUPS,
        BEAR_CRAWL, BOX_JUMPS, RUSSIAN_TWISTS, COMMANDO_PLANK, LATERAL_LUNGES,
        FLUTTER_KICKS, PIKE_PUSHUPS, SQUAT_TO_PRESS, DEAD_BUG, MAN_MAKERS
    )

    // ── Warmup & Cooldown Steps ─────────────────────────────────────────

    // Warmup movements
    private val JUMPING_JACKS = HIITPhaseStep("Jumping Jacks", "Light pace to raise heart rate and loosen up.", "warmup-jumping-jacks")
    private val ARM_CIRCLES = HIITPhaseStep("Arm Circles", "Extend arms wide, make small circles forward then backward.", "warmup-arm-circles")
    private val LEG_SWINGS = HIITPhaseStep("Leg Swings", "Hold a wall, swing each leg forward and back, then side to side.", "warmup-leg-swings")
    private val HIP_CIRCLES = HIITPhaseStep("Hip Circles", "Hands on hips, rotate hips in wide circles both directions.", "warmup-hip-circles")
    private val BODYWEIGHT_SQUATS = HIITPhaseStep("Bodyweight Squats", "Slow, controlled squats to warm up knees, hips, and glutes.", "warmup-bodyweight-squats")
    private val INCHWORMS = HIITPhaseStep("Inchworms", "Bend forward, walk hands out to plank, walk feet back to hands, stand.", "warmup-inchworms")
    private val TORSO_TWISTS = HIITPhaseStep("Torso Twists", "Feet shoulder-width, rotate upper body side to side with arms relaxed.", "warmup-torso-twists")
    private val HIGH_KNEES_GENTLE = HIITPhaseStep("Gentle High Knees", "March in place, lifting knees to hip height at an easy pace.", "warmup-gentle-high-knees")
    private val SHOULDER_ROLLS = HIITPhaseStep("Shoulder Rolls", "Roll shoulders forward in big circles, then reverse.", "warmup-shoulder-rolls")
    private val WALKING_LUNGES = HIITPhaseStep("Walking Lunges", "Step forward into a lunge, alternate legs, slow and controlled.", "warmup-walking-lunges")
    private val CAT_COW = HIITPhaseStep("Cat-Cow", "On all fours, alternate arching and rounding your back with your breath.", "warmup-cat-cow")
    private val WRIST_ANKLE_CIRCLES = HIITPhaseStep("Wrist & Ankle Circles", "Rotate wrists and ankles in both directions to mobilize joints.", "warmup-wrist-ankle-circles")

    // Cooldown stretches
    private val STANDING_QUAD_STRETCH = HIITPhaseStep("Standing Quad Stretch", "Stand on one leg, pull foot to glute, hold 15 seconds each side.", "cooldown-standing-quad-stretch")
    private val STANDING_HAMSTRING_STRETCH = HIITPhaseStep("Standing Hamstring Stretch", "Extend one leg forward on heel, hinge at hips until you feel a stretch.", "cooldown-standing-hamstring-stretch")
    private val CHEST_OPENER = HIITPhaseStep("Chest Opener Stretch", "Clasp hands behind back, squeeze shoulder blades together, lift chest.", "cooldown-chest-opener")
    private val CROSS_BODY_SHOULDER = HIITPhaseStep("Cross-Body Shoulder Stretch", "Pull one arm across your chest with the other hand, hold each side.", "cooldown-cross-body-shoulder")
    private val CHILDS_POSE = HIITPhaseStep("Child's Pose", "Kneel, sit back on heels, reach arms forward on the floor, breathe deeply.", "cooldown-childs-pose")
    private val SEATED_SPINAL_TWIST = HIITPhaseStep("Seated Spinal Twist", "Sit with legs extended, cross one leg over, twist toward the bent knee.", "cooldown-seated-spinal-twist")
    private val HIP_FLEXOR_STRETCH = HIITPhaseStep("Hip Flexor Stretch", "Kneel on one knee, push hips forward until you feel a stretch in front hip.", "cooldown-hip-flexor-stretch")
    private val STANDING_CALF_STRETCH = HIITPhaseStep("Standing Calf Stretch", "Step one foot back, press heel down, lean into front leg.", "cooldown-standing-calf-stretch")
    private val TRICEP_STRETCH = HIITPhaseStep("Overhead Tricep Stretch", "Raise one arm overhead, bend elbow, gently push with other hand.", "cooldown-tricep-stretch")
    private val DEEP_BREATHING = HIITPhaseStep("Deep Breathing", "Stand tall, inhale slowly through nose for 4 counts, exhale for 6 counts.", "cooldown-deep-breathing")
    private val FORWARD_FOLD = HIITPhaseStep("Standing Forward Fold", "Feet hip-width, hinge at hips, let head and arms hang, relax.", "cooldown-forward-fold")
    private val FIGURE_FOUR_STRETCH = HIITPhaseStep("Figure Four Stretch", "Lie on back, cross ankle over opposite knee, pull thigh toward chest.", "cooldown-figure-four-stretch")

    // ── Workout Templates ───────────────────────────────────────────────

    val QUICK_BURN_EXPRESS = HIITWorkoutTemplate(
        id = "quick_burn_express",
        name = "Quick Burn Express",
        description = "Short and effective full-body workout for when you're tight on time.",
        difficulty = "Easy-Medium",
        exercises = listOf(
            HIITWorkoutExercise(HIGH_KNEES),
            HIITWorkoutExercise(JUMP_SQUATS),
            HIITWorkoutExercise(BICYCLE_CRUNCHES),
            HIITWorkoutExercise(LATERAL_LUNGES),
            HIITWorkoutExercise(PLANK_JACKS)
        ),
        workDurationSec = 30,
        restDurationSec = 15,
        rounds = 3,
        warmupSec = 60,
        cooldownSec = 60,
        warmupSteps = listOf(JUMPING_JACKS, HIP_CIRCLES, BODYWEIGHT_SQUATS, HIGH_KNEES_GENTLE),
        cooldownSteps = listOf(STANDING_QUAD_STRETCH, STANDING_HAMSTRING_STRETCH, FORWARD_FOLD, DEEP_BREATHING)
    )

    val UPPER_BODY_BLITZ = HIITWorkoutTemplate(
        id = "upper_body_blitz",
        name = "Upper Body Blitz",
        description = "Intense upper body and pushing-focused HIIT session.",
        difficulty = "Medium-Hard",
        exercises = listOf(
            HIITWorkoutExercise(PUSHUP_SHOULDER_TAP),
            HIITWorkoutExercise(DIAMOND_PUSHUPS),
            HIITWorkoutExercise(COMMANDO_PLANK),
            HIITWorkoutExercise(PIKE_PUSHUPS),
            HIITWorkoutExercise(BEAR_CRAWL),
            HIITWorkoutExercise(MOUNTAIN_CLIMBERS)
        ),
        workDurationSec = 40,
        restDurationSec = 20,
        rounds = 4,
        warmupSec = 90,
        cooldownSec = 90,
        warmupSteps = listOf(ARM_CIRCLES, SHOULDER_ROLLS, INCHWORMS, JUMPING_JACKS, WRIST_ANKLE_CIRCLES),
        cooldownSteps = listOf(CHEST_OPENER, CROSS_BODY_SHOULDER, TRICEP_STRETCH, CHILDS_POSE, DEEP_BREATHING)
    )

    val LEG_DAY_INFERNO = HIITWorkoutTemplate(
        id = "leg_day_inferno",
        name = "Leg Day Inferno",
        description = "Lower body focused — glutes, quads, and explosive power.",
        difficulty = "Medium",
        exercises = listOf(
            HIITWorkoutExercise(JUMP_SQUATS),
            HIITWorkoutExercise(SKATER_JUMPS),
            HIITWorkoutExercise(LATERAL_LUNGES),
            HIITWorkoutExercise(BOX_JUMPS),
            HIITWorkoutExercise(TUCK_JUMPS),
            HIITWorkoutExercise(HIGH_KNEES)
        ),
        workDurationSec = 40,
        restDurationSec = 20,
        rounds = 3,
        warmupSec = 90,
        cooldownSec = 90,
        warmupSteps = listOf(HIGH_KNEES_GENTLE, LEG_SWINGS, HIP_CIRCLES, BODYWEIGHT_SQUATS, WALKING_LUNGES),
        cooldownSteps = listOf(STANDING_QUAD_STRETCH, STANDING_HAMSTRING_STRETCH, HIP_FLEXOR_STRETCH, STANDING_CALF_STRETCH, DEEP_BREATHING)
    )

    val CORE_CRUSHER = HIITWorkoutTemplate(
        id = "core_crusher",
        name = "Core Crusher",
        description = "Target every angle of your core with this focused ab workout.",
        difficulty = "Easy-Medium",
        exercises = listOf(
            HIITWorkoutExercise(DEAD_BUG),
            HIITWorkoutExercise(BICYCLE_CRUNCHES),
            HIITWorkoutExercise(RUSSIAN_TWISTS),
            HIITWorkoutExercise(FLUTTER_KICKS),
            HIITWorkoutExercise(MOUNTAIN_CLIMBERS)
        ),
        workDurationSec = 35,
        restDurationSec = 15,
        rounds = 4,
        warmupSec = 60,
        cooldownSec = 90,
        warmupSteps = listOf(TORSO_TWISTS, CAT_COW, HIP_CIRCLES, HIGH_KNEES_GENTLE),
        cooldownSteps = listOf(CHILDS_POSE, SEATED_SPINAL_TWIST, HIP_FLEXOR_STRETCH, FORWARD_FOLD, DEEP_BREATHING)
    )

    val TOTAL_BODY_DESTROYER = HIITWorkoutTemplate(
        id = "total_body_destroyer",
        name = "Total Body Destroyer",
        description = "Maximum intensity full-body session for advanced athletes.",
        difficulty = "Hard",
        exercises = listOf(
            HIITWorkoutExercise(BURPEES),
            HIITWorkoutExercise(MAN_MAKERS),
            HIITWorkoutExercise(BOX_JUMPS),
            HIITWorkoutExercise(PIKE_PUSHUPS),
            HIITWorkoutExercise(SQUAT_TO_PRESS),
            HIITWorkoutExercise(TUCK_JUMPS),
            HIITWorkoutExercise(COMMANDO_PLANK),
            HIITWorkoutExercise(SKATER_JUMPS)
        ),
        workDurationSec = 45,
        restDurationSec = 15,
        rounds = 4,
        warmupSec = 120,
        cooldownSec = 120,
        warmupSteps = listOf(JUMPING_JACKS, ARM_CIRCLES, LEG_SWINGS, HIP_CIRCLES, INCHWORMS, BODYWEIGHT_SQUATS),
        cooldownSteps = listOf(STANDING_QUAD_STRETCH, STANDING_HAMSTRING_STRETCH, CHEST_OPENER, HIP_FLEXOR_STRETCH, FIGURE_FOUR_STRETCH, DEEP_BREATHING)
    )

    val STEADY_STATE_BUILDER = HIITWorkoutTemplate(
        id = "steady_state_builder",
        name = "Steady State Builder",
        description = "Beginner-friendly with equal work and rest for active recovery days.",
        difficulty = "Easy",
        exercises = listOf(
            HIITWorkoutExercise(HIGH_KNEES),
            HIITWorkoutExercise(DEAD_BUG),
            HIITWorkoutExercise(LATERAL_LUNGES),
            HIITWorkoutExercise(PLANK_JACKS),
            HIITWorkoutExercise(RUSSIAN_TWISTS),
            HIITWorkoutExercise(FLUTTER_KICKS),
            HIITWorkoutExercise(BEAR_CRAWL)
        ),
        workDurationSec = 30,
        restDurationSec = 30,
        rounds = 3,
        warmupSec = 120,
        cooldownSec = 120,
        warmupSteps = listOf(HIGH_KNEES_GENTLE, ARM_CIRCLES, HIP_CIRCLES, LEG_SWINGS, WALKING_LUNGES, CAT_COW),
        cooldownSteps = listOf(STANDING_QUAD_STRETCH, STANDING_HAMSTRING_STRETCH, CROSS_BODY_SHOULDER, CHILDS_POSE, FORWARD_FOLD, DEEP_BREATHING)
    )

    /** All 6 workout templates */
    val allTemplates: List<HIITWorkoutTemplate> = listOf(
        QUICK_BURN_EXPRESS,
        UPPER_BODY_BLITZ,
        LEG_DAY_INFERNO,
        CORE_CRUSHER,
        TOTAL_BODY_DESTROYER,
        STEADY_STATE_BUILDER
    )

    /** Find a template by ID */
    fun getTemplateById(id: String): HIITWorkoutTemplate? =
        allTemplates.find { it.id == id }

    /** Find an exercise by ID */
    fun getExerciseById(id: String): HIITExercise? =
        allExercises.find { it.id == id }
}
