package com.runtracker.shared.ai

/**
 * Form Analysis Engine
 * Analyzes pose data to provide form feedback and injury prevention tips
 * Supports both running and gym exercises
 */
class FormAnalysisEngine {

    /**
     * Analyze running form from pose landmarks
     */
    fun analyzeForm(poseData: PoseData): FormAnalysisResult {
        val issues = mutableListOf<FormIssue>()
        val metrics = mutableMapOf<String, Float>()
        
        // Analyze posture (forward lean) - more sensitive
        val forwardLean = calculateForwardLean(poseData)
        metrics["forwardLean"] = forwardLean
        if (forwardLean < 3f) {
            issues.add(FormIssue(
                type = FormIssueType.POSTURE,
                severity = IssueSeverity.MEDIUM,
                title = "Too Upright",
                description = "Lean is only ${forwardLean.toInt()}° - aim for 5-10°",
                correction = "Lean slightly forward from your ankles"
            ))
        } else if (forwardLean > 12f) {
            issues.add(FormIssue(
                type = FormIssueType.POSTURE,
                severity = if (forwardLean > 18f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Excessive Forward Lean",
                description = "Leaning ${forwardLean.toInt()}° forward - straining lower back",
                correction = "Stand taller and engage your core"
            ))
        }
        
        // Analyze arm swing symmetry - more sensitive
        val armSwingSymmetry = calculateArmSwingSymmetry(poseData)
        metrics["armSwingSymmetry"] = armSwingSymmetry
        if (armSwingSymmetry < 0.85f) {
            issues.add(FormIssue(
                type = FormIssueType.ARM_SWING,
                severity = if (armSwingSymmetry < 0.7f) IssueSeverity.MEDIUM else IssueSeverity.LOW,
                title = "Asymmetric Arm Swing",
                description = "Arms ${((1 - armSwingSymmetry) * 100).toInt()}% uneven",
                correction = "Focus on relaxed, equal arm movements"
            ))
        }
        
        // Analyze arm angle - more sensitive
        val armAngle = calculateArmAngle(poseData)
        metrics["armAngle"] = armAngle
        if (armAngle < 75f) {
            issues.add(FormIssue(
                type = FormIssueType.ARM_SWING,
                severity = if (armAngle < 60f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Arms Too Straight",
                description = "Elbow angle is ${armAngle.toInt()}° - should be ~90°",
                correction = "Bend elbows to approximately 90 degrees"
            ))
        } else if (armAngle > 105f) {
            issues.add(FormIssue(
                type = FormIssueType.ARM_SWING,
                severity = if (armAngle > 120f) IssueSeverity.MEDIUM else IssueSeverity.LOW,
                title = "Arms Too Bent",
                description = "Elbow angle is ${armAngle.toInt()}° - too tight",
                correction = "Relax your arms to about 90 degrees"
            ))
        }
        
        // Analyze vertical oscillation - more sensitive
        val verticalOscillation = calculateVerticalOscillation(poseData)
        metrics["verticalOscillation"] = verticalOscillation
        if (verticalOscillation > 6f) {
            issues.add(FormIssue(
                type = FormIssueType.VERTICAL_OSCILLATION,
                severity = if (verticalOscillation > 12f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Bouncing Too Much",
                description = "Vertical movement of ${verticalOscillation.toInt()}cm wastes energy",
                correction = "Focus on moving forward, not up and down"
            ))
        }
        
        // Analyze hip drop - more sensitive
        val hipDrop = calculateHipDrop(poseData)
        metrics["hipDrop"] = hipDrop
        if (hipDrop > 5f) {
            issues.add(FormIssue(
                type = FormIssueType.HIP_STABILITY,
                severity = if (hipDrop > 10f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Excessive Hip Drop",
                description = "Hips dropping ${hipDrop.toInt()}° - risk of IT band issues",
                correction = "Strengthen glutes and focus on level hips"
            ))
        }
        
        // Analyze knee drive - more sensitive
        val kneeDrive = calculateKneeDrive(poseData)
        metrics["kneeDrive"] = kneeDrive
        if (kneeDrive < 40f) {
            issues.add(FormIssue(
                type = FormIssueType.KNEE_DRIVE,
                severity = if (kneeDrive < 25f) IssueSeverity.MEDIUM else IssueSeverity.LOW,
                title = "Low Knee Drive",
                description = "Knee lift only ${kneeDrive.toInt()}° - losing power",
                correction = "Drive knees forward and up"
            ))
        }
        
        // Analyze foot strike (if detectable)
        val footStrike = analyzeFootStrike(poseData)
        metrics["footStrikeAngle"] = footStrike.angle
        if (footStrike.type == FootStrikeType.HEEL) {
            issues.add(FormIssue(
                type = FormIssueType.FOOT_STRIKE,
                severity = if (footStrike.severity == IssueSeverity.HIGH) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Heel Striking",
                description = "Landing on heel increases impact forces",
                correction = "Land with foot under your body, not in front"
            ))
        }
        
        // Check shoulder alignment
        val shoulderAlign = calculateShoulderAlignment(poseData)
        metrics["shoulderAlignment"] = shoulderAlign
        if (shoulderAlign > 6f) {
            issues.add(FormIssue(
                type = FormIssueType.POSTURE,
                severity = if (shoulderAlign > 12f) IssueSeverity.MEDIUM else IssueSeverity.LOW,
                title = "Uneven Shoulders",
                description = "Shoulders tilted ${shoulderAlign.toInt()}%",
                correction = "Keep shoulders level and relaxed"
            ))
        }
        
        // Check crossover gait
        val crossover = calculateCrossoverGait(poseData)
        metrics["crossover"] = crossover
        if (crossover > 8f) {
            issues.add(FormIssue(
                type = FormIssueType.HIP_STABILITY,
                severity = if (crossover > 15f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Crossover Gait",
                description = "Feet crossing midline by ${crossover.toInt()}%",
                correction = "Run on imaginary train tracks, not a tightrope"
            ))
        }
        
        // Calculate overall form score
        val formScore = calculateFormScore(issues, metrics)
        
        return FormAnalysisResult(
            overallScore = formScore,
            issues = issues.sortedByDescending { it.severity.ordinal },
            metrics = metrics,
            cadenceEstimate = estimateCadence(poseData),
            strideAnalysis = analyzeStride(poseData)
        )
    }

    private fun calculateForwardLean(poseData: PoseData): Float {
        // Calculate angle between vertical and line from hip to shoulder
        val shoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 8f
        val hip = poseData.landmarks[PoseLandmark.LEFT_HIP] ?: return 8f
        
        val dx = shoulder.x - hip.x
        val dy = shoulder.y - hip.y
        
        return Math.toDegrees(Math.atan2(dx.toDouble(), -dy.toDouble())).toFloat().coerceIn(-30f, 30f)
    }

    private fun calculateArmSwingSymmetry(poseData: PoseData): Float {
        val leftWrist = poseData.landmarks[PoseLandmark.LEFT_WRIST] ?: return 1f
        val rightWrist = poseData.landmarks[PoseLandmark.RIGHT_WRIST] ?: return 1f
        val leftShoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 1f
        val rightShoulder = poseData.landmarks[PoseLandmark.RIGHT_SHOULDER] ?: return 1f
        
        val leftSwing = Math.abs(leftWrist.y - leftShoulder.y)
        val rightSwing = Math.abs(rightWrist.y - rightShoulder.y)
        
        return if (leftSwing > rightSwing) rightSwing / leftSwing else leftSwing / rightSwing
    }

    private fun calculateArmAngle(poseData: PoseData): Float {
        val shoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 90f
        val elbow = poseData.landmarks[PoseLandmark.LEFT_ELBOW] ?: return 90f
        val wrist = poseData.landmarks[PoseLandmark.LEFT_WRIST] ?: return 90f
        
        return calculateAngle(shoulder, elbow, wrist)
    }

    private fun calculateVerticalOscillation(poseData: PoseData): Float {
        // This would normally track hip position over time
        // For single frame, estimate based on knee bend and hip height
        return poseData.estimatedVerticalOscillation ?: 6f
    }

    private fun calculateHipDrop(poseData: PoseData): Float {
        val leftHip = poseData.landmarks[PoseLandmark.LEFT_HIP] ?: return 0f
        val rightHip = poseData.landmarks[PoseLandmark.RIGHT_HIP] ?: return 0f
        
        return Math.abs(leftHip.y - rightHip.y) * 100 // Convert to approximate degrees
    }

    private fun calculateKneeDrive(poseData: PoseData): Float {
        val hip = poseData.landmarks[PoseLandmark.LEFT_HIP] ?: return 45f
        val knee = poseData.landmarks[PoseLandmark.LEFT_KNEE] ?: return 45f
        
        val dy = hip.y - knee.y
        val dx = knee.x - hip.x
        
        return Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat().coerceIn(0f, 90f)
    }

    private fun analyzeFootStrike(poseData: PoseData): FootStrikeAnalysis {
        val ankle = poseData.landmarks[PoseLandmark.LEFT_ANKLE] ?: return FootStrikeAnalysis(FootStrikeType.MIDFOOT, 0f, IssueSeverity.LOW)
        val toe = poseData.landmarks[PoseLandmark.LEFT_FOOT_INDEX] ?: return FootStrikeAnalysis(FootStrikeType.MIDFOOT, 0f, IssueSeverity.LOW)
        val heel = poseData.landmarks[PoseLandmark.LEFT_HEEL] ?: return FootStrikeAnalysis(FootStrikeType.MIDFOOT, 0f, IssueSeverity.LOW)
        
        val heelToToeAngle = calculateAngle(
            Point3D(heel.x, heel.y, 0f),
            Point3D(ankle.x, ankle.y, 0f),
            Point3D(toe.x, toe.y, 0f)
        )
        
        return when {
            heelToToeAngle > 20f -> FootStrikeAnalysis(FootStrikeType.HEEL, heelToToeAngle, IssueSeverity.HIGH)
            heelToToeAngle > 10f -> FootStrikeAnalysis(FootStrikeType.HEEL, heelToToeAngle, IssueSeverity.MEDIUM)
            heelToToeAngle < -10f -> FootStrikeAnalysis(FootStrikeType.FOREFOOT, heelToToeAngle, IssueSeverity.LOW)
            else -> FootStrikeAnalysis(FootStrikeType.MIDFOOT, heelToToeAngle, IssueSeverity.LOW)
        }
    }

    private fun calculateAngle(a: Point3D, b: Point3D, c: Point3D): Float {
        val ba = Point3D(a.x - b.x, a.y - b.y, a.z - b.z)
        val bc = Point3D(c.x - b.x, c.y - b.y, c.z - b.z)
        
        val dot = ba.x * bc.x + ba.y * bc.y + ba.z * bc.z
        val magBA = Math.sqrt((ba.x * ba.x + ba.y * ba.y + ba.z * ba.z).toDouble())
        val magBC = Math.sqrt((bc.x * bc.x + bc.y * bc.y + bc.z * bc.z).toDouble())
        
        val cosAngle = dot / (magBA * magBC)
        return Math.toDegrees(Math.acos(cosAngle.coerceIn(-1.0, 1.0))).toFloat()
    }

    private fun estimateCadence(poseData: PoseData): Int {
        // Would normally track steps over time
        return poseData.estimatedCadence ?: 170
    }

    private fun analyzeStride(poseData: PoseData): StrideAnalysis {
        return StrideAnalysis(
            strideLength = poseData.estimatedStrideLength ?: 1.2f,
            groundContactTime = poseData.estimatedGroundContactTime ?: 250,
            flightTime = poseData.estimatedFlightTime ?: 100
        )
    }

    private fun calculateFormScore(issues: List<FormIssue>, metrics: Map<String, Float>): Int {
        var score = 100
        
        issues.forEach { issue ->
            score -= when (issue.severity) {
                IssueSeverity.HIGH -> 15
                IssueSeverity.MEDIUM -> 8
                IssueSeverity.LOW -> 3
            }
        }
        
        return score.coerceIn(0, 100)
    }

    // ==================== GYM EXERCISE ANALYSIS ====================

    /**
     * Analyze gym exercise form from pose landmarks
     */
    fun analyzeGymForm(poseData: PoseData, exerciseType: GymExerciseType): GymFormAnalysisResult {
        return when (exerciseType) {
            GymExerciseType.SQUAT -> analyzeSquatForm(poseData)
            GymExerciseType.DEADLIFT -> analyzeDeadliftForm(poseData)
            GymExerciseType.BENCH_PRESS -> analyzeBenchPressForm(poseData)
            GymExerciseType.OVERHEAD_PRESS -> analyzeOverheadPressForm(poseData)
            GymExerciseType.BARBELL_ROW -> analyzeBarbellRowForm(poseData)
            GymExerciseType.LUNGE -> analyzeLungeForm(poseData)
            GymExerciseType.PLANK -> analyzePlankForm(poseData)
            GymExerciseType.PUSH_UP -> analyzePushUpForm(poseData)
        }
    }

    private fun analyzeSquatForm(poseData: PoseData): GymFormAnalysisResult {
        val issues = mutableListOf<FormIssue>()
        val metrics = mutableMapOf<String, Float>()

        // Analyze knee angle for depth
        val kneeAngle = calculateKneeAngle(poseData)
        metrics["kneeAngle"] = kneeAngle
        
        // Check depth - more sensitive thresholds
        if (kneeAngle > 110f) {
            issues.add(FormIssue(
                type = FormIssueType.DEPTH,
                severity = IssueSeverity.HIGH,
                title = "Quarter Squat",
                description = "Knee angle is ${kneeAngle.toInt()}° - you're barely bending",
                correction = "Squat until thighs are at least parallel to the ground"
            ))
        } else if (kneeAngle > 90f) {
            issues.add(FormIssue(
                type = FormIssueType.DEPTH,
                severity = IssueSeverity.MEDIUM,
                title = "Insufficient Depth",
                description = "Knee angle is ${kneeAngle.toInt()}° - aim for 90° or less",
                correction = "Work on hip and ankle mobility to achieve proper depth"
            ))
        }

        // Check knee cave - more sensitive
        val kneeCaveAngle = calculateKneeCave(poseData)
        metrics["kneeCave"] = kneeCaveAngle
        if (kneeCaveAngle > 5f) {
            issues.add(FormIssue(
                type = FormIssueType.KNEE_TRACKING,
                severity = if (kneeCaveAngle > 15f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Knee Cave (Valgus)",
                description = "Knees collapsing inward by ${kneeCaveAngle.toInt()}%",
                correction = "Push your knees out over your toes, engage your glutes"
            ))
        }

        // Check back angle - more sensitive
        val backAngle = calculateBackAngle(poseData)
        metrics["backAngle"] = backAngle
        if (backAngle > 30f) {
            issues.add(FormIssue(
                type = FormIssueType.BACK_POSITION,
                severity = if (backAngle > 50f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Excessive Forward Lean",
                description = "Torso leaning ${backAngle.toInt()}° forward",
                correction = "Keep chest up, engage core, and sit back into your heels"
            ))
        }

        // Check heel rise
        val heelRise = detectHeelRise(poseData)
        metrics["heelRise"] = if (heelRise) 1f else 0f
        if (heelRise) {
            issues.add(FormIssue(
                type = FormIssueType.FOOT_POSITION,
                severity = IssueSeverity.MEDIUM,
                title = "Heels Rising",
                description = "Your heels are coming off the ground",
                correction = "Work on ankle mobility or try elevating heels slightly"
            ))
        }

        // Check shoulder alignment
        val shoulderAlign = calculateShoulderAlignment(poseData)
        metrics["shoulderAlignment"] = shoulderAlign
        if (shoulderAlign > 8f) {
            issues.add(FormIssue(
                type = FormIssueType.SHOULDER_POSITION,
                severity = IssueSeverity.MEDIUM,
                title = "Uneven Shoulders",
                description = "Shoulders are tilted - possible weight shift",
                correction = "Keep the bar centered and shoulders level"
            ))
        }

        val score = calculateFormScore(issues, metrics)
        return GymFormAnalysisResult(
            exerciseType = GymExerciseType.SQUAT,
            overallScore = score,
            issues = issues,
            metrics = metrics,
            repQuality = if (score >= 80) RepQuality.GOOD else if (score >= 60) RepQuality.FAIR else RepQuality.POOR,
            tips = getSquatTips(issues)
        )
    }

    private fun analyzeDeadliftForm(poseData: PoseData): GymFormAnalysisResult {
        val issues = mutableListOf<FormIssue>()
        val metrics = mutableMapOf<String, Float>()

        // Check back rounding - more sensitive
        val spineAngle = calculateSpineNeutrality(poseData)
        metrics["spineNeutrality"] = spineAngle
        if (spineAngle > 8f) {
            issues.add(FormIssue(
                type = FormIssueType.BACK_POSITION,
                severity = if (spineAngle > 20f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Rounded Lower Back",
                description = "Spine deviation of ${spineAngle.toInt()}° from neutral",
                correction = "Brace your core, chest up, and maintain neutral spine"
            ))
        }

        // Check bar path - more sensitive
        val barPath = calculateBarPath(poseData)
        metrics["barPath"] = barPath
        if (barPath > 3f) {
            issues.add(FormIssue(
                type = FormIssueType.BAR_PATH,
                severity = if (barPath > 8f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Bar Drifting Forward",
                description = "Bar is ${barPath.toInt()}cm away from your body",
                correction = "Keep the bar close to your shins and thighs"
            ))
        }

        // Check hip hinge - adjusted thresholds
        val hipHingeAngle = calculateHipHinge(poseData)
        metrics["hipHinge"] = hipHingeAngle
        if (hipHingeAngle < 40f) {
            issues.add(FormIssue(
                type = FormIssueType.HIP_POSITION,
                severity = if (hipHingeAngle < 25f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Insufficient Hip Hinge",
                description = "Hip angle is ${hipHingeAngle.toInt()}° - you're squatting it",
                correction = "Push hips back more, keep shins more vertical"
            ))
        }

        // Check lockout
        val lockoutComplete = checkLockout(poseData)
        metrics["lockout"] = if (lockoutComplete) 1f else 0f
        if (!lockoutComplete) {
            issues.add(FormIssue(
                type = FormIssueType.LOCKOUT,
                severity = IssueSeverity.MEDIUM,
                title = "Incomplete Lockout",
                description = "Not fully extending at the top",
                correction = "Squeeze glutes and stand tall at the top"
            ))
        }

        // Check shoulder position
        val shoulderAlign = calculateShoulderAlignment(poseData)
        metrics["shoulderAlignment"] = shoulderAlign
        if (shoulderAlign > 8f) {
            issues.add(FormIssue(
                type = FormIssueType.SHOULDER_POSITION,
                severity = IssueSeverity.MEDIUM,
                title = "Uneven Shoulders",
                description = "One shoulder is higher than the other",
                correction = "Keep shoulders level throughout the lift"
            ))
        }

        val score = calculateFormScore(issues, metrics)
        return GymFormAnalysisResult(
            exerciseType = GymExerciseType.DEADLIFT,
            overallScore = score,
            issues = issues,
            metrics = metrics,
            repQuality = if (score >= 80) RepQuality.GOOD else if (score >= 60) RepQuality.FAIR else RepQuality.POOR,
            tips = getDeadliftTips(issues)
        )
    }

    private fun analyzeBenchPressForm(poseData: PoseData): GymFormAnalysisResult {
        val issues = mutableListOf<FormIssue>()
        val metrics = mutableMapOf<String, Float>()

        // Check elbow flare - more sensitive
        val elbowAngle = calculateElbowFlare(poseData)
        metrics["elbowFlare"] = elbowAngle
        if (elbowAngle > 60f) {
            issues.add(FormIssue(
                type = FormIssueType.ELBOW_POSITION,
                severity = if (elbowAngle > 80f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Excessive Elbow Flare",
                description = "Elbows at ${elbowAngle.toInt()}° - should be 45-60°",
                correction = "Tuck elbows to about 45 degrees from your torso"
            ))
        } else if (elbowAngle < 30f) {
            issues.add(FormIssue(
                type = FormIssueType.ELBOW_POSITION,
                severity = IssueSeverity.MEDIUM,
                title = "Elbows Too Tucked",
                description = "Elbows at ${elbowAngle.toInt()}° - too close to body",
                correction = "Flare elbows out slightly to about 45 degrees"
            ))
        }

        // Check bar touch point - more sensitive
        val touchPoint = calculateBarTouchPoint(poseData)
        metrics["touchPoint"] = touchPoint
        if (touchPoint > 0.6f) {
            issues.add(FormIssue(
                type = FormIssueType.BAR_PATH,
                severity = if (touchPoint > 0.8f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Bar Too High on Chest",
                description = "Touching too high - stresses shoulders",
                correction = "Touch the bar to your lower chest/sternum"
            ))
        } else if (touchPoint < 0.3f) {
            issues.add(FormIssue(
                type = FormIssueType.BAR_PATH,
                severity = IssueSeverity.MEDIUM,
                title = "Bar Too Low",
                description = "Touching too low on torso",
                correction = "Touch the bar to your lower chest, not belly"
            ))
        }

        // Check arch - adjusted
        val archAngle = calculateBackArch(poseData)
        metrics["backArch"] = archAngle
        if (archAngle > 30f) {
            issues.add(FormIssue(
                type = FormIssueType.BACK_POSITION,
                severity = if (archAngle > 50f) IssueSeverity.MEDIUM else IssueSeverity.LOW,
                title = "Excessive Arch",
                description = "Back arch of ${archAngle.toInt()}° is very pronounced",
                correction = "Maintain a moderate arch that feels stable"
            ))
        }

        // Check wrist position - more sensitive
        val wristAngle = calculateWristAngle(poseData)
        metrics["wristAngle"] = wristAngle
        if (wristAngle > 15f) {
            issues.add(FormIssue(
                type = FormIssueType.WRIST_POSITION,
                severity = if (wristAngle > 35f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Bent Wrists",
                description = "Wrists bent ${wristAngle.toInt()}° - risking injury",
                correction = "Keep wrists straight, bar over heel of palm"
            ))
        }

        // Check shoulder alignment
        val shoulderAlign = calculateShoulderAlignment(poseData)
        metrics["shoulderAlignment"] = shoulderAlign
        if (shoulderAlign > 8f) {
            issues.add(FormIssue(
                type = FormIssueType.SHOULDER_POSITION,
                severity = IssueSeverity.MEDIUM,
                title = "Uneven Press",
                description = "One side pressing faster than the other",
                correction = "Focus on pressing evenly with both arms"
            ))
        }

        val score = calculateFormScore(issues, metrics)
        return GymFormAnalysisResult(
            exerciseType = GymExerciseType.BENCH_PRESS,
            overallScore = score,
            issues = issues,
            metrics = metrics,
            repQuality = if (score >= 80) RepQuality.GOOD else if (score >= 60) RepQuality.FAIR else RepQuality.POOR,
            tips = getBenchPressTips(issues)
        )
    }

    private fun analyzeOverheadPressForm(poseData: PoseData): GymFormAnalysisResult {
        val issues = mutableListOf<FormIssue>()
        val metrics = mutableMapOf<String, Float>()

        // Check bar path - more sensitive
        val barPathDeviation = calculateVerticalBarPath(poseData)
        metrics["barPath"] = barPathDeviation
        if (barPathDeviation > 5f) {
            issues.add(FormIssue(
                type = FormIssueType.BAR_PATH,
                severity = if (barPathDeviation > 12f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Bar Path Not Vertical",
                description = "Bar deviating ${barPathDeviation.toInt()}cm from vertical",
                correction = "Move your head back and press straight up"
            ))
        }

        // Check back lean - more sensitive
        val backLean = calculateBackLean(poseData)
        metrics["backLean"] = backLean
        if (backLean > 10f) {
            issues.add(FormIssue(
                type = FormIssueType.BACK_POSITION,
                severity = if (backLean > 25f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Excessive Back Lean",
                description = "Leaning back ${backLean.toInt()}° - straining lower back",
                correction = "Brace core tight and keep torso upright"
            ))
        }

        // Check lockout
        val lockoutComplete = checkOverheadLockout(poseData)
        metrics["lockout"] = if (lockoutComplete) 1f else 0f
        if (!lockoutComplete) {
            issues.add(FormIssue(
                type = FormIssueType.LOCKOUT,
                severity = IssueSeverity.MEDIUM,
                title = "Incomplete Lockout",
                description = "Arms not fully extended at the top",
                correction = "Push through to full extension, shrug at top"
            ))
        }

        // Check shoulder alignment
        val shoulderAlign = calculateShoulderAlignment(poseData)
        metrics["shoulderAlignment"] = shoulderAlign
        if (shoulderAlign > 8f) {
            issues.add(FormIssue(
                type = FormIssueType.SHOULDER_POSITION,
                severity = IssueSeverity.MEDIUM,
                title = "Uneven Press",
                description = "One arm higher than the other",
                correction = "Press evenly with both arms"
            ))
        }

        val score = calculateFormScore(issues, metrics)
        return GymFormAnalysisResult(
            exerciseType = GymExerciseType.OVERHEAD_PRESS,
            overallScore = score,
            issues = issues,
            metrics = metrics,
            repQuality = if (score >= 80) RepQuality.GOOD else if (score >= 60) RepQuality.FAIR else RepQuality.POOR,
            tips = getOverheadPressTips(issues)
        )
    }

    private fun analyzeBarbellRowForm(poseData: PoseData): GymFormAnalysisResult {
        val issues = mutableListOf<FormIssue>()
        val metrics = mutableMapOf<String, Float>()

        // Check torso angle - more sensitive
        val torsoAngle = calculateTorsoAngle(poseData)
        metrics["torsoAngle"] = torsoAngle
        if (torsoAngle > 50f) {
            issues.add(FormIssue(
                type = FormIssueType.BACK_POSITION,
                severity = if (torsoAngle > 70f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Too Upright",
                description = "Torso at ${torsoAngle.toInt()}° - should be 30-50°",
                correction = "Hinge forward more for better back engagement"
            ))
        } else if (torsoAngle < 20f) {
            issues.add(FormIssue(
                type = FormIssueType.BACK_POSITION,
                severity = IssueSeverity.MEDIUM,
                title = "Too Bent Over",
                description = "Torso at ${torsoAngle.toInt()}° - too horizontal",
                correction = "Raise torso slightly to reduce lower back strain"
            ))
        }

        // Check spine neutrality - more sensitive
        val spineNeutral = calculateSpineNeutrality(poseData)
        metrics["spineNeutrality"] = spineNeutral
        if (spineNeutral > 8f) {
            issues.add(FormIssue(
                type = FormIssueType.BACK_POSITION,
                severity = if (spineNeutral > 18f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Rounded Back",
                description = "Spine rounding ${spineNeutral.toInt()}° from neutral",
                correction = "Keep chest up and maintain neutral spine"
            ))
        }

        // Check shoulder alignment
        val shoulderAlign = calculateShoulderAlignment(poseData)
        metrics["shoulderAlignment"] = shoulderAlign
        if (shoulderAlign > 8f) {
            issues.add(FormIssue(
                type = FormIssueType.SHOULDER_POSITION,
                severity = IssueSeverity.MEDIUM,
                title = "Uneven Pull",
                description = "One side pulling more than the other",
                correction = "Pull evenly with both arms"
            ))
        }

        // Check elbow position
        val elbowFlare = calculateElbowFlare(poseData)
        metrics["elbowFlare"] = elbowFlare
        if (elbowFlare > 70f) {
            issues.add(FormIssue(
                type = FormIssueType.ELBOW_POSITION,
                severity = IssueSeverity.MEDIUM,
                title = "Elbows Flaring",
                description = "Elbows at ${elbowFlare.toInt()}° - too wide",
                correction = "Keep elbows closer to body, pull to lower chest"
            ))
        }

        val score = calculateFormScore(issues, metrics)
        return GymFormAnalysisResult(
            exerciseType = GymExerciseType.BARBELL_ROW,
            overallScore = score,
            issues = issues,
            metrics = metrics,
            repQuality = if (score >= 80) RepQuality.GOOD else if (score >= 60) RepQuality.FAIR else RepQuality.POOR,
            tips = getBarbellRowTips(issues)
        )
    }

    private fun analyzeLungeForm(poseData: PoseData): GymFormAnalysisResult {
        val issues = mutableListOf<FormIssue>()
        val metrics = mutableMapOf<String, Float>()

        // Check front knee tracking - more sensitive
        val kneeOverToe = calculateKneeOverToe(poseData)
        metrics["kneeOverToe"] = kneeOverToe
        if (kneeOverToe > 8f) {
            issues.add(FormIssue(
                type = FormIssueType.KNEE_TRACKING,
                severity = if (kneeOverToe > 20f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Knee Too Far Forward",
                description = "Knee ${kneeOverToe.toInt()}cm past toes",
                correction = "Take a longer stride and keep shin more vertical"
            ))
        }

        // Check torso position - more sensitive
        val torsoLean = calculateTorsoLean(poseData)
        metrics["torsoLean"] = torsoLean
        if (torsoLean > 12f) {
            issues.add(FormIssue(
                type = FormIssueType.BACK_POSITION,
                severity = if (torsoLean > 25f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Forward Lean",
                description = "Leaning forward ${torsoLean.toInt()}°",
                correction = "Keep torso upright, engage core"
            ))
        }

        // Check knee angle for depth
        val kneeAngle = calculateKneeAngle(poseData)
        metrics["kneeAngle"] = kneeAngle
        if (kneeAngle > 100f) {
            issues.add(FormIssue(
                type = FormIssueType.DEPTH,
                severity = if (kneeAngle > 120f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Insufficient Depth",
                description = "Front knee at ${kneeAngle.toInt()}° - go deeper",
                correction = "Lower until front thigh is parallel to ground"
            ))
        }

        // Check shoulder alignment
        val shoulderAlign = calculateShoulderAlignment(poseData)
        metrics["shoulderAlignment"] = shoulderAlign
        if (shoulderAlign > 8f) {
            issues.add(FormIssue(
                type = FormIssueType.SHOULDER_POSITION,
                severity = IssueSeverity.MEDIUM,
                title = "Uneven Shoulders",
                description = "Shoulders tilting to one side",
                correction = "Keep shoulders level throughout"
            ))
        }

        val score = calculateFormScore(issues, metrics)
        return GymFormAnalysisResult(
            exerciseType = GymExerciseType.LUNGE,
            overallScore = score,
            issues = issues,
            metrics = metrics,
            repQuality = if (score >= 80) RepQuality.GOOD else if (score >= 60) RepQuality.FAIR else RepQuality.POOR,
            tips = getLungeTips(issues)
        )
    }

    private fun analyzePlankForm(poseData: PoseData): GymFormAnalysisResult {
        val issues = mutableListOf<FormIssue>()
        val metrics = mutableMapOf<String, Float>()

        // Check hip sag - more sensitive
        val hipSag = calculateHipSag(poseData)
        metrics["hipSag"] = hipSag
        if (hipSag > 5f) {
            issues.add(FormIssue(
                type = FormIssueType.HIP_POSITION,
                severity = if (hipSag > 15f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Hips Sagging",
                description = "Hips dropping ${hipSag.toInt()}% below ideal line",
                correction = "Engage core and glutes, straight line head to heels"
            ))
        }

        // Check hip pike - more sensitive
        val hipPike = calculateHipPike(poseData)
        metrics["hipPike"] = hipPike
        if (hipPike > 8f) {
            issues.add(FormIssue(
                type = FormIssueType.HIP_POSITION,
                severity = if (hipPike > 18f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Hips Too High",
                description = "Hips piked ${hipPike.toInt()}% above ideal line",
                correction = "Lower hips to create a straight line"
            ))
        }

        // Check head position - more sensitive
        val headPosition = calculateHeadPosition(poseData)
        metrics["headPosition"] = headPosition
        if (headPosition > 10f) {
            issues.add(FormIssue(
                type = FormIssueType.HEAD_POSITION,
                severity = if (headPosition > 25f) IssueSeverity.MEDIUM else IssueSeverity.LOW,
                title = "Head Position",
                description = "Keep your head in a neutral position",
                correction = "Look at the floor about a foot in front of your hands"
            ))
        }

        val score = calculateFormScore(issues, metrics)
        return GymFormAnalysisResult(
            exerciseType = GymExerciseType.PLANK,
            overallScore = score,
            issues = issues,
            metrics = metrics,
            repQuality = if (score >= 80) RepQuality.GOOD else if (score >= 60) RepQuality.FAIR else RepQuality.POOR,
            tips = getPlankTips(issues)
        )
    }

    private fun analyzePushUpForm(poseData: PoseData): GymFormAnalysisResult {
        val issues = mutableListOf<FormIssue>()
        val metrics = mutableMapOf<String, Float>()

        // Check elbow flare - lower threshold for sensitivity
        val elbowFlare = calculateElbowFlare(poseData)
        metrics["elbowFlare"] = elbowFlare
        if (elbowFlare > 60f) {
            issues.add(FormIssue(
                type = FormIssueType.ELBOW_POSITION,
                severity = if (elbowFlare > 80f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Elbows Flaring Out",
                description = "Your elbows are at ${elbowFlare.toInt()}° - should be around 45°",
                correction = "Keep elbows tucked at 45 degrees from your body"
            ))
        } else if (elbowFlare < 20f) {
            issues.add(FormIssue(
                type = FormIssueType.ELBOW_POSITION,
                severity = IssueSeverity.MEDIUM,
                title = "Elbows Too Tucked",
                description = "Your elbows are too close to your body",
                correction = "Flare elbows out slightly to about 45 degrees"
            ))
        }

        // Check hip sag - more sensitive threshold
        val hipSag = calculateHipSag(poseData)
        metrics["hipSag"] = hipSag
        if (hipSag > 5f) {
            issues.add(FormIssue(
                type = FormIssueType.HIP_POSITION,
                severity = if (hipSag > 15f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Hips Sagging",
                description = "Your hips are dropping ${hipSag.toInt()}% below the ideal line",
                correction = "Engage core and glutes, maintain plank position"
            ))
        }

        // Check hip pike
        val hipPike = calculateHipPike(poseData)
        metrics["hipPike"] = hipPike
        if (hipPike > 5f) {
            issues.add(FormIssue(
                type = FormIssueType.HIP_POSITION,
                severity = if (hipPike > 15f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Hips Too High",
                description = "Your hips are piked up ${hipPike.toInt()}% above the ideal line",
                correction = "Lower your hips to form a straight line from head to heels"
            ))
        }

        // Check depth - more sensitive
        val depth = calculatePushUpDepth(poseData)
        metrics["depth"] = depth
        if (depth > 10f) {
            issues.add(FormIssue(
                type = FormIssueType.DEPTH,
                severity = if (depth > 30f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                title = "Insufficient Depth",
                description = "Only going ${(100 - depth).toInt()}% of full range",
                correction = "Lower until chest nearly touches the ground"
            ))
        }

        // Check head position
        val headPos = calculateHeadPosition(poseData)
        metrics["headPosition"] = headPos
        if (headPos > 8f) {
            issues.add(FormIssue(
                type = FormIssueType.HEAD_POSITION,
                severity = IssueSeverity.LOW,
                title = "Head Position",
                description = "Keep your head in a neutral position",
                correction = "Look at a spot on the floor about 1 foot ahead of your hands"
            ))
        }

        // Check shoulder alignment
        val shoulderAlignment = calculateShoulderAlignment(poseData)
        metrics["shoulderAlignment"] = shoulderAlignment
        if (shoulderAlignment > 10f) {
            issues.add(FormIssue(
                type = FormIssueType.SHOULDER_POSITION,
                severity = IssueSeverity.MEDIUM,
                title = "Uneven Shoulders",
                description = "Your shoulders are not level",
                correction = "Keep both shoulders at the same height throughout"
            ))
        }

        // Check hand position relative to shoulders
        val handPosition = calculateHandPosition(poseData)
        metrics["handPosition"] = handPosition
        if (handPosition > 15f) {
            issues.add(FormIssue(
                type = FormIssueType.HAND_POSITION,
                severity = IssueSeverity.MEDIUM,
                title = "Hands Too Wide",
                description = "Your hands are placed too wide",
                correction = "Place hands slightly wider than shoulder-width"
            ))
        } else if (handPosition < -10f) {
            issues.add(FormIssue(
                type = FormIssueType.HAND_POSITION,
                severity = IssueSeverity.MEDIUM,
                title = "Hands Too Narrow",
                description = "Your hands are placed too close together",
                correction = "Place hands slightly wider than shoulder-width"
            ))
        }

        val score = calculateFormScore(issues, metrics)
        return GymFormAnalysisResult(
            exerciseType = GymExerciseType.PUSH_UP,
            overallScore = score,
            issues = issues,
            metrics = metrics,
            repQuality = if (score >= 80) RepQuality.GOOD else if (score >= 60) RepQuality.FAIR else RepQuality.POOR,
            tips = getPushUpTips(issues)
        )
    }

    // Gym-specific calculation helpers
    private fun calculateKneeAngle(poseData: PoseData): Float {
        val hip = poseData.landmarks[PoseLandmark.LEFT_HIP] ?: return 90f
        val knee = poseData.landmarks[PoseLandmark.LEFT_KNEE] ?: return 90f
        val ankle = poseData.landmarks[PoseLandmark.LEFT_ANKLE] ?: return 90f
        return calculateAngle(hip, knee, ankle)
    }

    private fun calculateKneeCave(poseData: PoseData): Float {
        val leftKnee = poseData.landmarks[PoseLandmark.LEFT_KNEE] ?: return 0f
        val rightKnee = poseData.landmarks[PoseLandmark.RIGHT_KNEE] ?: return 0f
        val leftAnkle = poseData.landmarks[PoseLandmark.LEFT_ANKLE] ?: return 0f
        val rightAnkle = poseData.landmarks[PoseLandmark.RIGHT_ANKLE] ?: return 0f
        
        val kneeWidth = Math.abs(leftKnee.x - rightKnee.x)
        val ankleWidth = Math.abs(leftAnkle.x - rightAnkle.x)
        
        return if (ankleWidth > 0) ((ankleWidth - kneeWidth) / ankleWidth * 100).coerceAtLeast(0f) else 0f
    }

    private fun calculateBackAngle(poseData: PoseData): Float {
        val shoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 0f
        val hip = poseData.landmarks[PoseLandmark.LEFT_HIP] ?: return 0f
        return Math.toDegrees(Math.atan2((shoulder.y - hip.y).toDouble(), (shoulder.x - hip.x).toDouble())).toFloat().let { Math.abs(it - 90) }
    }

    private fun detectHeelRise(poseData: PoseData): Boolean {
        val heel = poseData.landmarks[PoseLandmark.LEFT_HEEL] ?: return false
        val toe = poseData.landmarks[PoseLandmark.LEFT_FOOT_INDEX] ?: return false
        return heel.y < toe.y - 0.02f
    }

    private fun calculateSpineNeutrality(poseData: PoseData): Float {
        // Simplified - would analyze curvature of spine
        return poseData.landmarks[PoseLandmark.LEFT_SHOULDER]?.let { 10f } ?: 0f
    }

    private fun calculateBarPath(poseData: PoseData): Float {
        val leftWrist = poseData.landmarks[PoseLandmark.LEFT_WRIST] ?: return 3f
        val leftShoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 3f
        // Check horizontal deviation of wrist from shoulder
        return Math.abs(leftWrist.x - leftShoulder.x) * 100
    }
    
    private fun calculateHipHinge(poseData: PoseData): Float {
        val shoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 45f
        val hip = poseData.landmarks[PoseLandmark.LEFT_HIP] ?: return 45f
        val knee = poseData.landmarks[PoseLandmark.LEFT_KNEE] ?: return 45f
        return calculateAngle(shoulder, hip, knee)
    }
    
    private fun checkLockout(poseData: PoseData): Boolean {
        val shoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return true
        val hip = poseData.landmarks[PoseLandmark.LEFT_HIP] ?: return true
        val knee = poseData.landmarks[PoseLandmark.LEFT_KNEE] ?: return true
        val hipAngle = calculateAngle(shoulder, hip, knee)
        return hipAngle > 160f // Nearly straight = locked out
    }
    
    private fun calculateElbowFlare(poseData: PoseData): Float {
        val shoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 45f
        val elbow = poseData.landmarks[PoseLandmark.LEFT_ELBOW] ?: return 45f
        val hip = poseData.landmarks[PoseLandmark.LEFT_HIP] ?: return 45f
        
        // Calculate angle between upper arm and torso
        val armVectorX = elbow.x - shoulder.x
        val armVectorY = elbow.y - shoulder.y
        val torsoVectorX = hip.x - shoulder.x
        val torsoVectorY = hip.y - shoulder.y
        
        val dotProduct = armVectorX * torsoVectorX + armVectorY * torsoVectorY
        val armMag = Math.sqrt((armVectorX * armVectorX + armVectorY * armVectorY).toDouble())
        val torsoMag = Math.sqrt((torsoVectorX * torsoVectorX + torsoVectorY * torsoVectorY).toDouble())
        
        if (armMag == 0.0 || torsoMag == 0.0) return 45f
        
        val cosAngle = (dotProduct / (armMag * torsoMag)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(Math.acos(cosAngle)).toFloat()
    }
    
    private fun calculateBarTouchPoint(poseData: PoseData): Float {
        val wrist = poseData.landmarks[PoseLandmark.LEFT_WRIST] ?: return 0.5f
        val shoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 0.5f
        val hip = poseData.landmarks[PoseLandmark.LEFT_HIP] ?: return 0.5f
        // Where on the torso is the bar touching (0 = hip level, 1 = shoulder level)
        val torsoLength = shoulder.y - hip.y
        if (torsoLength == 0f) return 0.5f
        return ((wrist.y - hip.y) / torsoLength).coerceIn(0f, 1f)
    }
    
    private fun calculateBackArch(poseData: PoseData): Float {
        val shoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 15f
        val hip = poseData.landmarks[PoseLandmark.LEFT_HIP] ?: return 15f
        // Simplified arch detection based on shoulder-hip vertical difference
        return Math.abs(shoulder.y - hip.y) * 100
    }
    
    private fun calculateWristAngle(poseData: PoseData): Float {
        val elbow = poseData.landmarks[PoseLandmark.LEFT_ELBOW] ?: return 10f
        val wrist = poseData.landmarks[PoseLandmark.LEFT_WRIST] ?: return 10f
        // Check if wrist is bent back (simplified)
        val deviation = Math.abs(wrist.x - elbow.x) * 100
        return deviation
    }
    
    private fun calculateVerticalBarPath(poseData: PoseData): Float {
        val wrist = poseData.landmarks[PoseLandmark.LEFT_WRIST] ?: return 5f
        val shoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 5f
        return Math.abs(wrist.x - shoulder.x) * 100
    }
    
    private fun calculateBackLean(poseData: PoseData): Float {
        val shoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 10f
        val hip = poseData.landmarks[PoseLandmark.LEFT_HIP] ?: return 10f
        // Angle from vertical
        val dx = shoulder.x - hip.x
        val dy = shoulder.y - hip.y
        if (dy == 0f) return 0f
        return Math.toDegrees(Math.atan2(dx.toDouble(), (-dy).toDouble())).toFloat().let { Math.abs(it) }
    }
    
    private fun checkOverheadLockout(poseData: PoseData): Boolean {
        val shoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return true
        val elbow = poseData.landmarks[PoseLandmark.LEFT_ELBOW] ?: return true
        val wrist = poseData.landmarks[PoseLandmark.LEFT_WRIST] ?: return true
        val armAngle = calculateAngle(shoulder, elbow, wrist)
        return armAngle > 160f // Nearly straight arm
    }
    
    private fun calculateTorsoAngle(poseData: PoseData): Float {
        val shoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 45f
        val hip = poseData.landmarks[PoseLandmark.LEFT_HIP] ?: return 45f
        // Angle from horizontal
        val dx = shoulder.x - hip.x
        val dy = shoulder.y - hip.y
        return Math.toDegrees(Math.atan2((-dy).toDouble(), dx.toDouble())).toFloat().let { Math.abs(it) }
    }
    
    private fun calculateKneeOverToe(poseData: PoseData): Float {
        val knee = poseData.landmarks[PoseLandmark.LEFT_KNEE] ?: return 5f
        val ankle = poseData.landmarks[PoseLandmark.LEFT_ANKLE] ?: return 5f
        val toe = poseData.landmarks[PoseLandmark.LEFT_FOOT_INDEX] ?: return 5f
        // How far knee is past the toe (negative = behind, positive = past)
        val kneeForward = (knee.x - toe.x) * 100
        return kneeForward.coerceAtLeast(0f)
    }
    
    private fun calculateTorsoLean(poseData: PoseData): Float {
        return calculateBackLean(poseData)
    }
    
    private fun calculateHipSag(poseData: PoseData): Float {
        val shoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 5f
        val hip = poseData.landmarks[PoseLandmark.LEFT_HIP] ?: return 5f
        val ankle = poseData.landmarks[PoseLandmark.LEFT_ANKLE] ?: return 5f
        
        // In a proper plank/push-up, shoulder-hip-ankle should be roughly linear
        // Calculate how much the hip deviates from the line between shoulder and ankle
        val expectedHipY = shoulder.y + (ankle.y - shoulder.y) * 0.5f
        val hipDeviation = (hip.y - expectedHipY) * 100
        
        // Positive = hips sagging below the line
        return hipDeviation.coerceAtLeast(0f)
    }
    
    private fun calculateHipPike(poseData: PoseData): Float {
        val shoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 5f
        val hip = poseData.landmarks[PoseLandmark.LEFT_HIP] ?: return 5f
        val ankle = poseData.landmarks[PoseLandmark.LEFT_ANKLE] ?: return 5f
        
        val expectedHipY = shoulder.y + (ankle.y - shoulder.y) * 0.5f
        val hipDeviation = (expectedHipY - hip.y) * 100
        
        // Positive = hips piked above the line
        return hipDeviation.coerceAtLeast(0f)
    }
    
    private fun calculateHeadPosition(poseData: PoseData): Float {
        val nose = poseData.landmarks[PoseLandmark.NOSE] ?: return 10f
        val shoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 10f
        // Check if head is too far forward or back
        return Math.abs(nose.x - shoulder.x) * 100
    }
    
    private fun calculatePushUpDepth(poseData: PoseData): Float {
        val shoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 10f
        val elbow = poseData.landmarks[PoseLandmark.LEFT_ELBOW] ?: return 10f
        val wrist = poseData.landmarks[PoseLandmark.LEFT_WRIST] ?: return 10f
        
        // Calculate elbow angle - smaller angle = deeper push-up
        val elbowAngle = calculateAngle(shoulder, elbow, wrist)
        
        // If elbow angle is > 120, not deep enough (return high value to trigger issue)
        // If elbow angle is < 90, good depth (return low value)
        return if (elbowAngle > 90f) (elbowAngle - 90f) else 0f
    }
    
    private fun calculateCrossoverGait(poseData: PoseData): Float {
        val leftAnkle = poseData.landmarks[PoseLandmark.LEFT_ANKLE] ?: return 0f
        val rightAnkle = poseData.landmarks[PoseLandmark.RIGHT_ANKLE] ?: return 0f
        val leftHip = poseData.landmarks[PoseLandmark.LEFT_HIP] ?: return 0f
        val rightHip = poseData.landmarks[PoseLandmark.RIGHT_HIP] ?: return 0f
        
        // Calculate hip center (midline)
        val hipCenterX = (leftHip.x + rightHip.x) / 2
        val hipWidth = Math.abs(leftHip.x - rightHip.x)
        
        if (hipWidth == 0f) return 0f
        
        // Check if ankles are crossing the midline
        val leftAnkleCrossover = if (leftAnkle.x > hipCenterX) (leftAnkle.x - hipCenterX) / hipWidth * 100 else 0f
        val rightAnkleCrossover = if (rightAnkle.x < hipCenterX) (hipCenterX - rightAnkle.x) / hipWidth * 100 else 0f
        
        return Math.max(leftAnkleCrossover, rightAnkleCrossover)
    }

    // Tips generators - comprehensive issue-specific tips
    private fun getSquatTips(issues: List<FormIssue>): List<String> {
        val tips = mutableListOf<String>()
        if (issues.any { it.type == FormIssueType.KNEE_TRACKING }) {
            tips.add("Place a mini band above knees to cue knee-out position")
            tips.add("Focus on 'spreading the floor' with your feet")
        }
        if (issues.any { it.type == FormIssueType.DEPTH }) {
            tips.add("Work on ankle mobility with wall stretches")
            tips.add("Practice goblet squats to groove the pattern")
            tips.add("Try box squats to build confidence at depth")
        }
        if (issues.any { it.type == FormIssueType.BACK_POSITION }) {
            tips.add("Keep chest up - imagine showing off a logo on your shirt")
            tips.add("Brace your core like you're about to be punched")
            tips.add("Try front squats to reinforce upright posture")
        }
        if (issues.any { it.type == FormIssueType.FOOT_POSITION }) {
            tips.add("Elevate heels with small plates or squat shoes")
            tips.add("Stretch calves and ankles daily")
        }
        if (tips.isEmpty()) tips.add("Great form! Focus on controlled tempo")
        return tips
    }

    private fun getDeadliftTips(issues: List<FormIssue>): List<String> {
        val tips = mutableListOf<String>()
        if (issues.any { it.type == FormIssueType.BACK_POSITION }) {
            tips.add("Practice hip hinges with a dowel on your back")
            tips.add("Engage lats by 'protecting your armpits'")
            tips.add("Take a big breath and brace before each rep")
        }
        if (issues.any { it.type == FormIssueType.BAR_PATH }) {
            tips.add("Drag the bar up your shins and thighs")
            tips.add("Think 'push the floor away' rather than pulling")
        }
        if (issues.any { it.type == FormIssueType.HIP_POSITION }) {
            tips.add("Start with hips higher - this isn't a squat")
            tips.add("Practice Romanian deadlifts to feel the hip hinge")
        }
        if (issues.any { it.type == FormIssueType.LOCKOUT }) {
            tips.add("Squeeze glutes hard at the top")
            tips.add("Stand tall - don't hyperextend your back")
        }
        if (tips.isEmpty()) tips.add("Solid technique! Focus on progressive overload")
        return tips
    }

    private fun getBenchPressTips(issues: List<FormIssue>): List<String> {
        val tips = mutableListOf<String>()
        if (issues.any { it.type == FormIssueType.ELBOW_POSITION }) {
            tips.add("Think about 'bending the bar' to engage lats")
            tips.add("Tuck elbows to 45° - not flared, not tucked")
            tips.add("Grip width affects elbow angle - experiment")
        }
        if (issues.any { it.type == FormIssueType.BAR_PATH }) {
            tips.add("Touch bar to lower chest/sternum area")
            tips.add("Press in a slight arc back toward your face")
        }
        if (issues.any { it.type == FormIssueType.WRIST_POSITION }) {
            tips.add("Bar should sit on heel of palm, not fingers")
            tips.add("Consider wrist wraps for heavy sets")
        }
        if (issues.any { it.type == FormIssueType.BACK_POSITION }) {
            tips.add("Squeeze shoulder blades together and down")
            tips.add("Maintain arch but keep glutes on bench")
        }
        if (tips.isEmpty()) tips.add("Excellent form! Work on pause reps for strength")
        return tips
    }

    private fun getOverheadPressTips(issues: List<FormIssue>): List<String> {
        val tips = mutableListOf<String>()
        if (issues.any { it.type == FormIssueType.BAR_PATH }) {
            tips.add("Move head back to clear the bar path")
            tips.add("Press straight up, then move head through")
        }
        if (issues.any { it.type == FormIssueType.BACK_POSITION }) {
            tips.add("Squeeze glutes hard to prevent back lean")
            tips.add("Brace core like you're about to be punched")
            tips.add("Consider a staggered stance for stability")
        }
        if (issues.any { it.type == FormIssueType.LOCKOUT }) {
            tips.add("Shrug shoulders up at the top")
            tips.add("Push head through at lockout")
        }
        if (tips.isEmpty()) tips.add("Strong pressing! Add push press for power")
        return tips
    }

    private fun getBarbellRowTips(issues: List<FormIssue>): List<String> {
        val tips = mutableListOf<String>()
        if (issues.any { it.type == FormIssueType.BACK_POSITION }) {
            tips.add("Hinge at hips, not waist")
            tips.add("Keep chest up and spine neutral")
            tips.add("If back rounds, reduce weight")
        }
        if (issues.any { it.type == FormIssueType.ELBOW_POSITION }) {
            tips.add("Pull elbows back, not out")
            tips.add("Aim to touch bar to lower chest/upper abs")
        }
        if (issues.any { it.type == FormIssueType.SHOULDER_POSITION }) {
            tips.add("Squeeze shoulder blades at the top")
            tips.add("Control the negative - don't just drop it")
        }
        if (tips.isEmpty()) tips.add("Great rowing! Try pause reps for more back activation")
        return tips
    }

    private fun getLungeTips(issues: List<FormIssue>): List<String> {
        val tips = mutableListOf<String>()
        if (issues.any { it.type == FormIssueType.KNEE_TRACKING }) {
            tips.add("Take a longer stride")
            tips.add("Keep front shin vertical or slightly angled back")
        }
        if (issues.any { it.type == FormIssueType.BACK_POSITION }) {
            tips.add("Stay upright - don't lean forward")
            tips.add("Engage core throughout the movement")
        }
        if (issues.any { it.type == FormIssueType.DEPTH }) {
            tips.add("Lower until back knee nearly touches floor")
            tips.add("Both knees should be at 90 degrees at bottom")
        }
        if (issues.any { it.type == FormIssueType.SHOULDER_POSITION }) {
            tips.add("Keep shoulders square and level")
            tips.add("Hands on hips can help with balance")
        }
        if (tips.isEmpty()) tips.add("Excellent lunges! Try walking lunges for a challenge")
        return tips
    }

    private fun getPlankTips(issues: List<FormIssue>): List<String> {
        val tips = mutableListOf<String>()
        if (issues.any { it.type == FormIssueType.HIP_POSITION }) {
            tips.add("Squeeze glutes to prevent hip sag")
            tips.add("Imagine a straight line from head to heels")
            tips.add("Posterior pelvic tilt - tuck tailbone under")
        }
        if (issues.any { it.type == FormIssueType.HEAD_POSITION }) {
            tips.add("Look at floor about 1 foot ahead of hands")
            tips.add("Keep neck neutral - don't look up or tuck chin")
        }
        if (tips.isEmpty()) tips.add("Solid plank! Try side planks or plank variations")
        return tips
    }

    private fun getPushUpTips(issues: List<FormIssue>): List<String> {
        val tips = mutableListOf<String>()
        if (issues.any { it.type == FormIssueType.HIP_POSITION }) {
            tips.add("Squeeze glutes and brace core throughout")
            tips.add("Maintain plank position - no sagging or piking")
        }
        if (issues.any { it.type == FormIssueType.ELBOW_POSITION }) {
            tips.add("Screw hands into floor for external rotation")
            tips.add("Elbows at 45° from body, not flared out")
        }
        if (issues.any { it.type == FormIssueType.DEPTH }) {
            tips.add("Touch chest to floor or a tennis ball")
            tips.add("Full range of motion builds more strength")
        }
        if (issues.any { it.type == FormIssueType.HEAD_POSITION }) {
            tips.add("Keep neck neutral - look at floor ahead")
        }
        if (issues.any { it.type == FormIssueType.HAND_POSITION }) {
            tips.add("Hands slightly wider than shoulder-width")
            tips.add("Fingers spread, middle fingers pointing forward")
        }
        if (tips.isEmpty()) tips.add("Perfect push-ups! Try archer or diamond variations")
        return tips
    }
    
    private fun calculateShoulderAlignment(poseData: PoseData): Float {
        val leftShoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 0f
        val rightShoulder = poseData.landmarks[PoseLandmark.RIGHT_SHOULDER] ?: return 0f
        // Check if shoulders are level (y difference)
        return Math.abs(leftShoulder.y - rightShoulder.y) * 100
    }
    
    private fun calculateHandPosition(poseData: PoseData): Float {
        val leftWrist = poseData.landmarks[PoseLandmark.LEFT_WRIST] ?: return 0f
        val rightWrist = poseData.landmarks[PoseLandmark.RIGHT_WRIST] ?: return 0f
        val leftShoulder = poseData.landmarks[PoseLandmark.LEFT_SHOULDER] ?: return 0f
        val rightShoulder = poseData.landmarks[PoseLandmark.RIGHT_SHOULDER] ?: return 0f
        
        val shoulderWidth = Math.abs(leftShoulder.x - rightShoulder.x)
        val handWidth = Math.abs(leftWrist.x - rightWrist.x)
        
        if (shoulderWidth == 0f) return 0f
        
        // Positive = hands wider than shoulders, negative = narrower
        return ((handWidth - shoulderWidth) / shoulderWidth) * 100
    }
}

// Data classes for form analysis
data class PoseData(
    val landmarks: Map<PoseLandmark, Point3D>,
    val timestamp: Long = System.currentTimeMillis(),
    val estimatedCadence: Int? = null,
    val estimatedVerticalOscillation: Float? = null,
    val estimatedStrideLength: Float? = null,
    val estimatedGroundContactTime: Int? = null,
    val estimatedFlightTime: Int? = null
)

data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float = 0f
)

enum class PoseLandmark {
    NOSE,
    LEFT_EYE, RIGHT_EYE,
    LEFT_EAR, RIGHT_EAR,
    LEFT_SHOULDER, RIGHT_SHOULDER,
    LEFT_ELBOW, RIGHT_ELBOW,
    LEFT_WRIST, RIGHT_WRIST,
    LEFT_HIP, RIGHT_HIP,
    LEFT_KNEE, RIGHT_KNEE,
    LEFT_ANKLE, RIGHT_ANKLE,
    LEFT_HEEL, RIGHT_HEEL,
    LEFT_FOOT_INDEX, RIGHT_FOOT_INDEX
}

data class FormAnalysisResult(
    val overallScore: Int,
    val issues: List<FormIssue>,
    val metrics: Map<String, Float>,
    val cadenceEstimate: Int,
    val strideAnalysis: StrideAnalysis
)

data class FormIssue(
    val type: FormIssueType,
    val severity: IssueSeverity,
    val title: String,
    val description: String,
    val correction: String
)

enum class FormIssueType {
    // Running
    POSTURE,
    ARM_SWING,
    VERTICAL_OSCILLATION,
    HIP_STABILITY,
    KNEE_DRIVE,
    FOOT_STRIKE,
    CADENCE,
    STRIDE_LENGTH,
    // Gym
    DEPTH,
    KNEE_TRACKING,
    BACK_POSITION,
    FOOT_POSITION,
    BAR_PATH,
    HIP_POSITION,
    LOCKOUT,
    ELBOW_POSITION,
    WRIST_POSITION,
    HEAD_POSITION,
    SHOULDER_POSITION,
    HAND_POSITION
}

enum class IssueSeverity {
    LOW, MEDIUM, HIGH
}

data class FootStrikeAnalysis(
    val type: FootStrikeType,
    val angle: Float,
    val severity: IssueSeverity
)

enum class FootStrikeType {
    HEEL, MIDFOOT, FOREFOOT
}

data class StrideAnalysis(
    val strideLength: Float,
    val groundContactTime: Int,
    val flightTime: Int
) {
    val verticalRatio: Float
        get() = if (groundContactTime > 0) flightTime.toFloat() / groundContactTime else 0f
}

// Gym-specific data classes
enum class GymExerciseType(val displayName: String, val muscleGroups: List<String>) {
    SQUAT("Squat", listOf("Quadriceps", "Glutes", "Hamstrings", "Core")),
    DEADLIFT("Deadlift", listOf("Back", "Glutes", "Hamstrings", "Core")),
    BENCH_PRESS("Bench Press", listOf("Chest", "Shoulders", "Triceps")),
    OVERHEAD_PRESS("Overhead Press", listOf("Shoulders", "Triceps", "Core")),
    BARBELL_ROW("Barbell Row", listOf("Back", "Biceps", "Rear Delts")),
    LUNGE("Lunge", listOf("Quadriceps", "Glutes", "Hamstrings")),
    PLANK("Plank", listOf("Core", "Shoulders")),
    PUSH_UP("Push-Up", listOf("Chest", "Shoulders", "Triceps", "Core"))
}

data class GymFormAnalysisResult(
    val exerciseType: GymExerciseType,
    val overallScore: Int,
    val issues: List<FormIssue>,
    val metrics: Map<String, Float>,
    val repQuality: RepQuality,
    val tips: List<String>
)

enum class RepQuality {
    POOR, FAIR, GOOD
}

/**
 * Form tips and drills for common issues
 */
object FormDrills {
    
    fun getDrillsForIssue(issueType: FormIssueType): List<FormDrill> {
        return when (issueType) {
            FormIssueType.POSTURE -> listOf(
                FormDrill(
                    name = "Wall Lean Drill",
                    description = "Stand facing a wall, lean forward until your chest touches, then run in place",
                    duration = "30 seconds",
                    frequency = "Before each run"
                ),
                FormDrill(
                    name = "Falling Start",
                    description = "Stand tall, lean forward until you have to step, then start running",
                    duration = "5 repetitions",
                    frequency = "During warm-up"
                )
            )
            FormIssueType.ARM_SWING -> listOf(
                FormDrill(
                    name = "Seated Arm Swings",
                    description = "Sit on ground, practice arm swing motion without leg movement",
                    duration = "1 minute",
                    frequency = "Daily"
                ),
                FormDrill(
                    name = "Relaxed Hands Drill",
                    description = "Run with hands loosely cupped, thumbs on top of fingers",
                    duration = "200m",
                    frequency = "During strides"
                )
            )
            FormIssueType.VERTICAL_OSCILLATION -> listOf(
                FormDrill(
                    name = "Low Ceiling Visualization",
                    description = "Imagine running under a low ceiling, keeping head level",
                    duration = "400m",
                    frequency = "Weekly"
                ),
                FormDrill(
                    name = "Quick Feet Drill",
                    description = "Run with very short, quick steps focusing on minimal bounce",
                    duration = "100m",
                    frequency = "During warm-up"
                )
            )
            FormIssueType.HIP_STABILITY -> listOf(
                FormDrill(
                    name = "Single Leg Squats",
                    description = "Stand on one leg, lower into partial squat, keep hips level",
                    duration = "10 each leg",
                    frequency = "3x per week"
                ),
                FormDrill(
                    name = "Clamshells",
                    description = "Lie on side, knees bent, lift top knee while keeping feet together",
                    duration = "15 each side",
                    frequency = "Daily"
                )
            )
            FormIssueType.KNEE_DRIVE -> listOf(
                FormDrill(
                    name = "High Knees",
                    description = "Run in place lifting knees to hip height",
                    duration = "30 seconds",
                    frequency = "During warm-up"
                ),
                FormDrill(
                    name = "A-Skips",
                    description = "Skip while driving knee up, focus on quick ground contact",
                    duration = "50m",
                    frequency = "Before speed work"
                )
            )
            FormIssueType.FOOT_STRIKE -> listOf(
                FormDrill(
                    name = "Barefoot Strides",
                    description = "Run short strides barefoot on grass to feel natural foot strike",
                    duration = "4x50m",
                    frequency = "Weekly"
                ),
                FormDrill(
                    name = "Metronome Running",
                    description = "Use a metronome at 180bpm to increase cadence and reduce overstriding",
                    duration = "5 minutes",
                    frequency = "During easy runs"
                )
            )
            else -> emptyList()
        }
    }
}

data class FormDrill(
    val name: String,
    val description: String,
    val duration: String,
    val frequency: String
)
