package com.runtracker.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.runtracker.app.ui.screens.dashboard.DashboardScreen
import com.runtracker.app.ui.screens.history.HistoryScreen
import com.runtracker.app.ui.screens.profile.ProfileScreen
import com.runtracker.app.ui.screens.rundetail.RunDetailScreen
import com.runtracker.app.ui.screens.running.RunningScreen
import com.runtracker.app.ui.screens.training.TrainingPlansScreen
import com.runtracker.app.ui.screens.training.PlanDetailScreen
import com.runtracker.app.ui.screens.training.WorkoutPreviewScreen
import com.runtracker.app.ui.screens.analytics.AnalyticsScreen
import com.runtracker.app.ui.screens.strava.StravaImportScreen
import com.runtracker.app.ui.screens.settings.StravaSettingsScreen
import com.runtracker.app.ui.screens.gym.*
import com.runtracker.app.ui.screens.nutrition.*
import com.runtracker.app.ui.screens.onboarding.OnboardingScreen
import com.runtracker.app.ui.screens.workouts.*
import com.runtracker.app.ui.screens.body.BodyAnalysisScreen
import com.runtracker.app.ui.screens.plan.WorkoutPlanScreen
import com.runtracker.app.ui.navigation.MainNavigationScreen
import com.runtracker.app.ui.screens.swimming.ActiveSwimWorkoutScreen
import com.runtracker.app.ui.screens.swimming.CreateSwimmingPlanScreen
import com.runtracker.app.ui.screens.cycling.ActiveCyclingWorkoutScreen
import com.runtracker.app.ui.screens.cycling.CreateCyclingPlanScreen
import com.runtracker.app.ui.screens.hiit.HIITDashboardScreen
import com.runtracker.app.ui.screens.hiit.ActiveHIITWorkoutScreen
import com.runtracker.app.ui.screens.hiit.HIITSummaryScreen
import com.runtracker.app.ui.screens.SafetyScreenWrapper
import com.runtracker.shared.data.model.SwimType
import com.runtracker.shared.data.model.PoolLength
import com.runtracker.shared.data.model.CyclingType

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Main : Screen("main") // New main screen with bottom nav
    object Dashboard : Screen("dashboard")
    object Running : Screen("running")
    object History : Screen("history")
    object RunDetail : Screen("run_detail/{runId}") {
        fun createRoute(runId: Long) = "run_detail/$runId"
    }
    object TrainingPlans : Screen("training_plans")
    object PlanDetail : Screen("plan_detail/{planId}") {
        fun createRoute(planId: Long) = "plan_detail/$planId"
    }
    object Analytics : Screen("analytics")
    object Profile : Screen("profile")
    object StravaImport : Screen("strava_import")
    
    // Gym screens
    object GymDashboard : Screen("gym_dashboard")
    object GymScheduler : Screen("gym_scheduler")
    object ActiveWorkout : Screen("active_workout/{workoutId}") {
        fun createRoute(workoutId: Long) = "active_workout/$workoutId"
    }
    object ExerciseLibrary : Screen("exercise_library")
    object ExercisePicker : Screen("exercise_picker")
    object GymHistory : Screen("gym_history")
    object WorkoutDetail : Screen("workout_detail/{workoutId}") {
        fun createRoute(workoutId: Long) = "workout_detail/$workoutId"
    }
    object CreateTemplate : Screen("create_template")
    object TemplateExercisePicker : Screen("template_exercise_picker")
    object CreateExercise : Screen("create_exercise")
    object EditTemplate : Screen("edit_template/{templateId}") {
        fun createRoute(templateId: Long) = "edit_template/$templateId"
    }
    object EditTemplateExercisePicker : Screen("edit_template_exercise_picker/{templateId}") {
        fun createRoute(templateId: Long) = "edit_template_exercise_picker/$templateId"
    }
    object WorkoutPreview : Screen("workout_preview/{planId}/{workoutId}") {
        fun createRoute(planId: Long, workoutId: String) = "workout_preview/$planId/$workoutId"
    }
    object ExerciseAnalysis : Screen("exercise_analysis")
    
    // Nutrition screens
    object NutritionDashboard : Screen("nutrition_dashboard")
    object NutritionSettings : Screen("nutrition_settings")
    object FoodScanner : Screen("food_scanner")
    
    // Settings
    object StravaSettings : Screen("strava_settings")
    
    // Custom Workouts & Plans
    object CustomWorkouts : Screen("custom_workouts")
    object WorkoutBuilder : Screen("workout_builder")
    object CustomPlans : Screen("custom_plans")
    object PlanBuilder : Screen("plan_builder")
    
    // Gamification
    object Gamification : Screen("gamification")
    
    // Mental Health / Mindfulness
    object Mindfulness : Screen("mindfulness")
    
    // Wellness Dashboard
    object WellnessDashboard : Screen("wellness_dashboard")
    
    // AI Training Coach
    object AITrainingCoach : Screen("ai_training_coach")
    
    // Form Analysis
    object FormAnalysis : Screen("form_analysis")
    
    // Body Analysis
    object BodyAnalysis : Screen("body_analysis")
    
    // Workout Plan Setup
    object WorkoutPlan : Screen("workout_plan")
    
    // Swimming screens
    object ActiveSwimWorkout : Screen("active_swim_workout/{swimType}?poolLength={poolLength}") {
        fun createRoute(swimType: String, poolLength: String? = null): String {
            return if (poolLength != null) {
                "active_swim_workout/$swimType?poolLength=$poolLength"
            } else {
                "active_swim_workout/$swimType"
            }
        }
    }
    object SwimmingPlans : Screen("swimming_plans")
    object CreateSwimmingPlan : Screen("create_swimming_plan")
    
    // Cycling screens
    object ActiveCyclingWorkout : Screen("active_cycling_workout/{cyclingType}") {
        fun createRoute(cyclingType: String) = "active_cycling_workout/$cyclingType"
    }
    object CyclingPlans : Screen("cycling_plans")
    object CreateCyclingPlan : Screen("create_cycling_plan")
    
    // HIIT screens
    object HIITDashboard : Screen("hiit_dashboard")
    object ActiveHIITWorkout : Screen("hiit_active/{templateId}") {
        fun createRoute(templateId: String) = "hiit_active/$templateId"
    }
    object HIITSummary : Screen("hiit_summary/{sessionId}") {
        fun createRoute(sessionId: Long) = "hiit_summary/$sessionId"
    }

    // Safety
    object Safety : Screen("safety")
    
    // Achievements
    object Achievements : Screen("achievements")
    
    // Progress Photos
    object ProgressPhotos : Screen("progress_photos")
    
    // Health Sync Settings
    object HealthSync : Screen("health_sync")
}

@Composable
fun GoSteadyNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Main.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        // New main screen with bottom navigation
        composable(Screen.Main.route) {
            MainNavigationScreen(mainNavController = navController)
        }

        // Keep old dashboard for backwards compatibility
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onStartRun = { navController.navigate(Screen.Running.route) },
                onViewHistory = { navController.navigate(Screen.History.route) },
                onViewTrainingPlans = { navController.navigate(Screen.TrainingPlans.route) },
                onViewAnalytics = { navController.navigate(Screen.Analytics.route) },
                onViewProfile = { navController.navigate(Screen.Profile.route) },
                onViewGym = { navController.navigate(Screen.GymDashboard.route) },
                onViewNutrition = { navController.navigate(Screen.NutritionDashboard.route) },
                onViewGamification = { navController.navigate(Screen.Gamification.route) },
                onViewMindfulness = { navController.navigate(Screen.Mindfulness.route) },
                onViewFormAnalysis = { navController.navigate(Screen.FormAnalysis.route) },
                onViewAiCoach = { navController.navigate(Screen.AITrainingCoach.route) },
                onViewBodyAnalysis = { navController.navigate(Screen.BodyAnalysis.route) },
                onRunClick = { runId -> navController.navigate(Screen.RunDetail.createRoute(runId)) }
            )
        }

        composable(Screen.Running.route) {
            RunningScreen(
                onFinish = { runId ->
                    navController.popBackStack()
                    if (runId != null) {
                        navController.navigate(Screen.RunDetail.createRoute(runId))
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onRunClick = { runId -> navController.navigate(Screen.RunDetail.createRoute(runId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RunDetail.route,
            arguments = listOf(navArgument("runId") { type = NavType.LongType })
        ) { backStackEntry ->
            val runId = backStackEntry.arguments?.getLong("runId") ?: return@composable
            RunDetailScreen(
                runId = runId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TrainingPlans.route) {
            TrainingPlansScreen(
                onPlanClick = { planId -> navController.navigate(Screen.PlanDetail.createRoute(planId)) },
                onBack = { navController.popBackStack() },
                onCustomWorkouts = { navController.navigate(Screen.CustomWorkouts.route) },
                onCustomPlans = { navController.navigate(Screen.CustomPlans.route) }
            )
        }

        composable(
            route = Screen.PlanDetail.route,
            arguments = listOf(navArgument("planId") { type = NavType.LongType })
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getLong("planId") ?: return@composable
            PlanDetailScreen(
                planId = planId,
                onBack = { navController.popBackStack() },
                onStartWorkout = { navController.navigate(Screen.Running.route) },
                onPreviewWorkout = { workoutId -> 
                    navController.navigate(Screen.WorkoutPreview.createRoute(planId, workoutId))
                }
            )
        }

        composable(
            route = Screen.WorkoutPreview.route,
            arguments = listOf(
                navArgument("planId") { type = NavType.LongType },
                navArgument("workoutId") { type = NavType.StringType }
            )
        ) {
            WorkoutPreviewScreen(
                onStartWorkout = { navController.navigate(Screen.Running.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onStravaImport = { navController.navigate(Screen.StravaImport.route) },
                onStravaSettings = { navController.navigate(Screen.StravaSettings.route) }
            )
        }

        composable(Screen.StravaImport.route) {
            StravaImportScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Gym screens
        composable(Screen.GymDashboard.route) {
            GymDashboardScreen(
                onStartWorkout = { workoutId -> 
                    navController.navigate(Screen.ActiveWorkout.createRoute(workoutId))
                },
                onViewWorkout = { workoutId -> 
                    navController.navigate(Screen.WorkoutDetail.createRoute(workoutId))
                },
                onViewExercises = { navController.navigate(Screen.ExerciseLibrary.route) },
                onViewHistory = { navController.navigate(Screen.GymHistory.route) },
                onViewAnalysis = { navController.navigate(Screen.ExerciseAnalysis.route) },
                onCreateTemplate = { navController.navigate(Screen.CreateTemplate.route) },
                onEditTemplate = { templateId -> 
                    navController.navigate(Screen.EditTemplate.createRoute(templateId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ActiveWorkout.route,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: return@composable
            
            // Listen for selected exercise from ExercisePicker
            val selectedExerciseId = backStackEntry.savedStateHandle.get<Long>("selected_exercise")
            
            ActiveWorkoutScreen(
                workoutId = workoutId,
                selectedExerciseId = selectedExerciseId,
                onExerciseAdded = { backStackEntry.savedStateHandle.remove<Long>("selected_exercise") },
                onFinish = { navController.popBackStack() },
                onAddExercise = { navController.navigate(Screen.ExercisePicker.route) }
            )
        }

        composable(Screen.ExerciseLibrary.route) {
            ExerciseLibraryScreen(
                onExerciseSelected = null,
                onCreateExercise = { navController.navigate(Screen.CreateExercise.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ExercisePicker.route) {
            ExerciseLibraryScreen(
                onExerciseSelected = { exercise ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("selected_exercise", exercise.id)
                    navController.popBackStack()
                },
                onCreateExercise = { navController.navigate(Screen.CreateExercise.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.GymHistory.route) {
            GymHistoryScreen(
                onWorkoutClick = { workoutId -> 
                    navController.navigate(Screen.WorkoutDetail.createRoute(workoutId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.GymScheduler.route) {
            GymSchedulerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ExerciseAnalysis.route) {
            ExerciseAnalysisScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.WorkoutDetail.route,
            arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: return@composable
            WorkoutDetailScreen(
                workoutId = workoutId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateTemplate.route) { backStackEntry ->
            val selectedExerciseId = backStackEntry.savedStateHandle.get<Long>("selected_exercise")
            
            CreateTemplateScreen(
                selectedExerciseId = selectedExerciseId,
                onExerciseAdded = { backStackEntry.savedStateHandle.remove<Long>("selected_exercise") },
                onAddExercise = { navController.navigate(Screen.TemplateExercisePicker.route) },
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TemplateExercisePicker.route) {
            ExerciseLibraryScreen(
                onExerciseSelected = { exercise ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("selected_exercise", exercise.id)
                    navController.popBackStack()
                },
                onCreateExercise = { navController.navigate(Screen.CreateExercise.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateExercise.route) {
            CreateExerciseScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditTemplate.route,
            arguments = listOf(navArgument("templateId") { type = NavType.LongType })
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getLong("templateId") ?: return@composable
            val selectedExerciseId = backStackEntry.savedStateHandle.get<Long>("selected_exercise")
            
            EditTemplateScreen(
                templateId = templateId,
                selectedExerciseId = selectedExerciseId,
                onExerciseAdded = { backStackEntry.savedStateHandle.remove<Long>("selected_exercise") },
                onAddExercise = { navController.navigate(Screen.EditTemplateExercisePicker.createRoute(templateId)) },
                onSaved = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditTemplateExercisePicker.route,
            arguments = listOf(navArgument("templateId") { type = NavType.LongType })
        ) {
            ExerciseLibraryScreen(
                onExerciseSelected = { exercise ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("selected_exercise", exercise.id)
                    navController.popBackStack()
                },
                onCreateExercise = { navController.navigate(Screen.CreateExercise.route) },
                onBack = { navController.popBackStack() }
            )
        }

        // Nutrition screens
        composable(Screen.NutritionDashboard.route) {
            NutritionDashboardScreen(
                onBack = { navController.popBackStack() },
                onViewSettings = { navController.navigate(Screen.NutritionSettings.route) },
                onScanFood = { navController.navigate(Screen.FoodScanner.route) }
            )
        }

        composable(Screen.NutritionSettings.route) {
            NutritionSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FoodScanner.route) {
            FoodScannerScreen(
                onBack = { navController.popBackStack() },
                onMealLogged = { navController.popBackStack() }
            )
        }
        
        // Strava settings
        composable(Screen.StravaSettings.route) {
            StravaSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Custom Workouts
        composable(Screen.CustomWorkouts.route) {
            CustomWorkoutListScreen(
                onNavigateToBuilder = { navController.navigate(Screen.WorkoutBuilder.route) },
                onStartWorkout = { workoutId ->
                    // TODO: Start workout with custom intervals
                    navController.navigate(Screen.Running.route)
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.WorkoutBuilder.route) {
            WorkoutBuilderScreen(
                onSave = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Custom Training Plans
        composable(Screen.CustomPlans.route) {
            CustomPlanListScreen(
                onNavigateToBuilder = { navController.navigate(Screen.PlanBuilder.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.PlanBuilder.route) {
            PlanBuilderScreen(
                onSave = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Gamification
        composable(Screen.Gamification.route) {
            com.runtracker.app.ui.screens.gamification.GamificationScreen(
                onNavigateToWellness = { navController.navigate(Screen.WellnessDashboard.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Mindfulness
        composable(Screen.Mindfulness.route) {
            com.runtracker.app.ui.screens.mindfulness.MindfulnessScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Wellness Dashboard
        composable(Screen.WellnessDashboard.route) {
            com.runtracker.app.ui.screens.wellness.WellnessDashboardScreen(
                onNavigateToGamification = { navController.navigate(Screen.Gamification.route) },
                onNavigateToMindfulness = { navController.navigate(Screen.Mindfulness.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        // AI Training Coach
        composable(Screen.AITrainingCoach.route) {
            com.runtracker.app.ui.screens.ai.AdaptiveTrainingScreen(
                onStartWorkout = { navController.navigate(Screen.Running.route) },
                onStartModifiedWorkout = { navController.navigate(Screen.Running.route) },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Form Analysis
        composable(Screen.FormAnalysis.route) {
            com.runtracker.app.ui.screens.form.FormAnalysisScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Body Analysis
        composable(Screen.BodyAnalysis.route) {
            BodyAnalysisScreen(
                onBack = { navController.popBackStack() },
                onCreatePlan = { navController.navigate(Screen.WorkoutPlan.route) }
            )
        }
        
        // Workout Plan
        composable(Screen.WorkoutPlan.route) {
            WorkoutPlanScreen(
                onBack = { navController.popBackStack() },
                onPlanCreated = { 
                    navController.popBackStack(Screen.Main.route, inclusive = false)
                }
            )
        }
        
        // Swimming screens
        composable(
            route = Screen.ActiveSwimWorkout.route,
            arguments = listOf(
                navArgument("swimType") { type = NavType.StringType },
                navArgument("poolLength") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val swimTypeStr = backStackEntry.arguments?.getString("swimType") ?: "POOL"
            val swimType = try { SwimType.valueOf(swimTypeStr) } catch (e: Exception) { SwimType.POOL }
            val poolLengthStr = backStackEntry.arguments?.getString("poolLength")
            val poolLength = poolLengthStr?.let {
                try { PoolLength.valueOf(it) } catch (e: Exception) { PoolLength.SHORT_COURSE_METERS }
            }

            ActiveSwimWorkoutScreen(
                swimType = swimType,
                poolLength = poolLength,
                onFinish = { workoutId ->
                    navController.popBackStack()
                },
                onDiscard = { navController.popBackStack() }
            )
        }
        
        // Cycling screens
        composable(
            route = Screen.ActiveCyclingWorkout.route,
            arguments = listOf(navArgument("cyclingType") { type = NavType.StringType })
        ) { backStackEntry ->
            val cyclingTypeStr = backStackEntry.arguments?.getString("cyclingType") ?: "OUTDOOR"
            val cyclingType = try { CyclingType.valueOf(cyclingTypeStr) } catch (e: Exception) { CyclingType.OUTDOOR }
            
            ActiveCyclingWorkoutScreen(
                cyclingType = cyclingType,
                onFinish = { workoutId ->
                    navController.popBackStack()
                },
                onDiscard = { navController.popBackStack() }
            )
        }
        
        // Swimming Plan Creation
        composable(Screen.CreateSwimmingPlan.route) {
            CreateSwimmingPlanScreen(
                onBack = { navController.popBackStack() },
                onPlanCreated = { navController.popBackStack() }
            )
        }
        
        // Cycling Plan Creation
        composable(Screen.CreateCyclingPlan.route) {
            CreateCyclingPlanScreen(
                onBack = { navController.popBackStack() },
                onPlanCreated = { navController.popBackStack() }
            )
        }

        // HIIT screens
        composable(Screen.HIITDashboard.route) {
            HIITDashboardScreen(
                onStartWorkout = { templateId ->
                    navController.navigate(Screen.ActiveHIITWorkout.createRoute(templateId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ActiveHIITWorkout.route,
            arguments = listOf(navArgument("templateId") { type = NavType.StringType })
        ) {
            ActiveHIITWorkoutScreen(
                onComplete = { sessionId ->
                    navController.navigate(Screen.HIITSummary.createRoute(sessionId)) {
                        popUpTo(Screen.HIITDashboard.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.HIITSummary.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) {
            HIITSummaryScreen(
                onDone = {
                    navController.navigate(Screen.HIITDashboard.route) {
                        popUpTo(Screen.HIITDashboard.route) { inclusive = true }
                    }
                }
            )
        }

        // Safety
        composable(Screen.Safety.route) {
            SafetyScreenWrapper(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Achievements
        composable(Screen.Achievements.route) {
            com.runtracker.app.ui.screens.achievements.AchievementsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Progress Photos
        composable(Screen.ProgressPhotos.route) {
            com.runtracker.app.ui.screens.progress.ProgressPhotosScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
