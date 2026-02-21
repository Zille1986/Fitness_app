package com.runtracker.wear.presentation

import androidx.compose.runtime.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.runtracker.wear.presentation.screens.*
import com.runtracker.wear.service.WearTrackingState

// Re-export for backwards compatibility with MainActivity imports
typealias ActivityType = com.runtracker.wear.presentation.screens.ActivityType
typealias SwimWorkoutTypeWatch = com.runtracker.wear.presentation.screens.SwimWorkoutTypeWatch
typealias CycleWorkoutTypeWatch = com.runtracker.wear.presentation.screens.CycleWorkoutTypeWatch

@Composable
fun WearRunTrackerApp(
    trackingState: WearTrackingState,
    isAmbient: Boolean = false,
    pendingWorkout: com.runtracker.shared.data.model.ScheduledWorkout? = null,
    onStartRun: () -> Unit,
    onStartWorkoutRun: (com.runtracker.shared.data.model.WorkoutType) -> Unit,
    onStartSwim: (String, Int) -> Unit = { _, _ -> },
    onStartSwimWorkout: (SwimWorkoutTypeWatch, String, Int) -> Unit = { _, _, _ -> },
    onStartCycling: (String) -> Unit = {},
    onStartCyclingWorkout: (CycleWorkoutTypeWatch, String) -> Unit = { _, _ -> },
    onPauseRun: () -> Unit,
    onResumeRun: () -> Unit,
    onStopRun: () -> Unit,
    onClearPendingWorkout: () -> Unit
) {
    val navController = rememberSwipeDismissableNavController()

    LaunchedEffect(trackingState.isTracking, pendingWorkout) {
        when {
            trackingState.isTracking -> {
                if (navController.currentDestination?.route != "tracking") {
                    navController.navigate("tracking") {
                        popUpTo("activity_select") { inclusive = false }
                    }
                }
            }
            pendingWorkout != null -> {
                if (navController.currentDestination?.route != "workout_preview") {
                    navController.navigate("workout_preview") {
                        popUpTo("activity_select") { inclusive = false }
                    }
                }
            }
            navController.currentDestination?.route == "tracking" -> {
                navController.navigate("activity_select") {
                    popUpTo("activity_select") { inclusive = true }
                }
            }
        }
    }

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "activity_select"
    ) {
        composable("activity_select") {
            ActivitySelectScreen(
                onSelectRunning = { navController.navigate("home") },
                onSelectSwimming = { navController.navigate("swimming_home") },
                onSelectCycling = { navController.navigate("cycling_home") },
                hasPendingWorkout = pendingWorkout != null,
                onViewPendingWorkout = { navController.navigate("workout_preview") },
                onMusicControl = { navController.navigate("music_control") }
            )
        }

        composable("home") {
            HomeScreen(
                onStartRun = onStartRun,
                onSelectWorkout = { navController.navigate("workout_select") },
                hasPendingWorkout = pendingWorkout != null,
                onViewPendingWorkout = { navController.navigate("workout_preview") },
                onBack = { navController.popBackStack() }
            )
        }

        composable("swimming_home") {
            SwimmingHomeScreen(
                onStartSwim = { swimType, poolLength -> onStartSwim(swimType, poolLength) },
                onStartSwimWorkout = { workoutType, swimType, poolLength ->
                    onStartSwimWorkout(workoutType, swimType, poolLength)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("cycling_home") {
            CyclingHomeScreen(
                onStartCycling = { cyclingType -> onStartCycling(cyclingType) },
                onStartCyclingWorkout = { workoutType, cyclingType ->
                    onStartCyclingWorkout(workoutType, cyclingType)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("workout_select") {
            WorkoutSelectScreen(
                onWorkoutSelected = { workoutType ->
                    navController.navigate("workout_preview_type/$workoutType")
                },
                onCustomWorkouts = { navController.navigate("custom_workouts") },
                onBack = { navController.popBackStack() }
            )
        }

        composable("custom_workouts") {
            CustomWorkoutsScreen(
                onWorkoutSelected = { navController.navigate("custom_workout_preview") },
                onBack = { navController.popBackStack() }
            )
        }

        composable("custom_workout_preview") {
            navController.popBackStack()
        }

        composable("workout_preview_type/{workoutType}") { backStackEntry ->
            val workoutTypeName = backStackEntry.arguments?.getString("workoutType") ?: "EASY_RUN"
            val workoutType = try {
                com.runtracker.shared.data.model.WorkoutType.valueOf(workoutTypeName)
            } catch (e: Exception) {
                com.runtracker.shared.data.model.WorkoutType.EASY_RUN
            }
            WorkoutTypePreviewScreen(
                workoutType = workoutType,
                onStartRun = { onStartWorkoutRun(workoutType) },
                onBack = { navController.popBackStack() }
            )
        }

        composable("workout_preview") {
            pendingWorkout?.let { workout ->
                WorkoutPreviewScreen(
                    workout = workout,
                    onStartRun = {
                        onClearPendingWorkout()
                        onStartRun()
                    },
                    onBack = {
                        onClearPendingWorkout()
                        navController.popBackStack()
                    }
                )
            }
        }

        composable("tracking") {
            TrackingPagerScreen(
                trackingState = trackingState,
                isAmbient = isAmbient,
                onPause = onPauseRun,
                onResume = onResumeRun,
                onStop = onStopRun
            )
        }

        composable("music_control") {
            MusicControlScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
