package com.runtracker.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.runtracker.app.ui.theme.GradientColors
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.runtracker.app.ui.screens.home.HomeScreen
import com.runtracker.app.ui.screens.running.RunningDashboardScreen
import com.runtracker.app.ui.screens.gym.GymDashboardScreenNew
import com.runtracker.app.ui.screens.nutrition.NutritionDashboardScreen
import com.runtracker.app.ui.screens.analysis.AnalysisDashboardScreen
import com.runtracker.app.ui.screens.mindfulness.MindfulnessScreen
import com.runtracker.app.ui.screens.calendar.CalendarScreen
import com.runtracker.app.ui.screens.swimming.SwimmingDashboardScreen
import com.runtracker.app.ui.screens.cycling.CyclingDashboardScreen

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    object Running : BottomNavItem(
        route = "running_dashboard",
        title = "Run",
        selectedIcon = Icons.Filled.DirectionsRun,
        unselectedIcon = Icons.Outlined.DirectionsRun
    )
    object Swimming : BottomNavItem(
        route = "swimming_dashboard",
        title = "Swim",
        selectedIcon = Icons.Filled.Pool,
        unselectedIcon = Icons.Outlined.Pool
    )
    object Cycling : BottomNavItem(
        route = "cycling_dashboard",
        title = "Bike",
        selectedIcon = Icons.Filled.DirectionsBike,
        unselectedIcon = Icons.Outlined.DirectionsBike
    )
    object Gym : BottomNavItem(
        route = "gym_dashboard_new",
        title = "Gym",
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter
    )
    object Nutrition : BottomNavItem(
        route = "nutrition",
        title = "Nutrition",
        selectedIcon = Icons.Filled.Restaurant,
        unselectedIcon = Icons.Outlined.Restaurant
    )
    object Analysis : BottomNavItem(
        route = "analysis_dashboard",
        title = "Analysis",
        selectedIcon = Icons.Filled.Analytics,
        unselectedIcon = Icons.Outlined.Analytics
    )
    object Mindfulness : BottomNavItem(
        route = "mindfulness_tab",
        title = "Mind",
        selectedIcon = Icons.Filled.SelfImprovement,
        unselectedIcon = Icons.Outlined.SelfImprovement
    )
    object Calendar : BottomNavItem(
        route = "calendar_tab",
        title = "Calendar",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    )
    object More : BottomNavItem(
        route = "more_menu",
        title = "More",
        selectedIcon = Icons.Filled.MoreHoriz,
        unselectedIcon = Icons.Outlined.MoreHoriz
    )
}

// Main bottom nav items: Home, Run, Swim, Bike, Gym, More
val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Running,
    BottomNavItem.Swimming,
    BottomNavItem.Cycling,
    BottomNavItem.Gym,
    BottomNavItem.More
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(
    mainNavController: NavHostController
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .height(64.dp)
                    .drawBehind {
                        // Gradient top border: teal → purple → orange
                        val borderHeight = 0.5.dp.toPx()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF00E5CC).copy(alpha = 0.3f),
                                    Color(0xFF7C4DFF).copy(alpha = 0.3f),
                                    Color(0xFFFFA657).copy(alpha = 0.3f)
                                )
                            ),
                            size = androidx.compose.ui.geometry.Size(size.width, borderHeight)
                        )
                    }
            ) {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { 
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            ) 
                        },
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent
                        ),
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onNavigateToRunning = {
                        bottomNavController.navigate(BottomNavItem.Running.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSwimming = {
                        bottomNavController.navigate(BottomNavItem.Swimming.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCycling = {
                        bottomNavController.navigate(BottomNavItem.Cycling.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToGym = {
                        bottomNavController.navigate(BottomNavItem.Gym.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToNutrition = {
                        bottomNavController.navigate(BottomNavItem.Nutrition.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAnalysis = {
                        bottomNavController.navigate(BottomNavItem.Analysis.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToMindfulness = {
                        bottomNavController.navigate(BottomNavItem.Mindfulness.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToProfile = {
                        mainNavController.navigate(Screen.Profile.route)
                    }
                )
            }
            
            composable(BottomNavItem.Running.route) {
                RunningDashboardScreen(
                    onStartRun = {
                        mainNavController.navigate(Screen.Running.route)
                    },
                    onViewPlans = {
                        mainNavController.navigate(Screen.TrainingPlans.route)
                    },
                    onViewFormAnalysis = {
                        mainNavController.navigate(Screen.FormAnalysis.route)
                    },
                    onViewRunDetail = { runId ->
                        mainNavController.navigate(Screen.RunDetail.createRoute(runId))
                    }
                )
            }
            
            composable(BottomNavItem.Gym.route) {
                GymDashboardScreenNew(
                    onStartWorkout = { templateId ->
                        mainNavController.navigate(Screen.ActiveWorkout.createRoute(templateId))
                    },
                    onViewWorkouts = {
                        mainNavController.navigate(Screen.GymHistory.route)
                    },
                    onViewFormAnalysis = {
                        mainNavController.navigate(Screen.FormAnalysis.route)
                    },
                    onViewWorkoutDetail = { workoutId ->
                        mainNavController.navigate(Screen.WorkoutDetail.createRoute(workoutId))
                    },
                    onCreateTemplate = {
                        mainNavController.navigate(Screen.CreateTemplate.route)
                    },
                    onEditTemplate = { templateId ->
                        mainNavController.navigate(Screen.EditTemplate.createRoute(templateId))
                    },
                    onScheduleWorkouts = {
                        mainNavController.navigate(Screen.GymScheduler.route)
                    }
                )
            }
            
            composable(BottomNavItem.Calendar.route) {
                CalendarScreen()
            }
            
            composable(BottomNavItem.Swimming.route) {
                SwimmingDashboardScreen(
                    onStartWorkout = { swimType ->
                        mainNavController.navigate(Screen.ActiveSwimWorkout.createRoute(swimType.name))
                    },
                    onViewHistory = {
                        // TODO: Navigate to swimming history
                    },
                    onViewPlans = {
                        mainNavController.navigate(Screen.CreateSwimmingPlan.route)
                    }
                )
            }
            
            composable(BottomNavItem.Cycling.route) {
                CyclingDashboardScreen(
                    onStartWorkout = { cyclingType ->
                        mainNavController.navigate(Screen.ActiveCyclingWorkout.createRoute(cyclingType.name))
                    },
                    onViewHistory = {
                        // TODO: Navigate to cycling history
                    },
                    onViewPlans = {
                        mainNavController.navigate(Screen.CreateCyclingPlan.route)
                    }
                )
            }
            
            composable(BottomNavItem.Nutrition.route) {
                NutritionDashboardScreen(
                    onBack = null,
                    onViewSettings = {
                        mainNavController.navigate(Screen.NutritionSettings.route)
                    },
                    onScanFood = {
                        mainNavController.navigate(Screen.FoodScanner.route)
                    }
                )
            }
            
            composable(BottomNavItem.Analysis.route) {
                AnalysisDashboardScreen(
                    onStartBodyScan = {
                        mainNavController.navigate(Screen.BodyAnalysis.route)
                    }
                )
            }
            
            composable(BottomNavItem.Mindfulness.route) {
                MindfulnessScreen(
                    onBack = null
                )
            }
            
            composable(BottomNavItem.More.route) {
                MoreMenuScreen(
                    onNavigateToNutrition = {
                        bottomNavController.navigate(BottomNavItem.Nutrition.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCalendar = {
                        bottomNavController.navigate(BottomNavItem.Calendar.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToMindfulness = {
                        bottomNavController.navigate(BottomNavItem.Mindfulness.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAnalysis = {
                        bottomNavController.navigate(BottomNavItem.Analysis.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSafety = {
                        mainNavController.navigate(Screen.Safety.route)
                    },
                    onNavigateToAchievements = {
                        mainNavController.navigate(Screen.Achievements.route)
                    },
                    onNavigateToProgressPhotos = {
                        mainNavController.navigate(Screen.ProgressPhotos.route)
                    },
                    onNavigateToHealthSync = {
                        // TODO: Navigate to health sync settings
                    }
                )
            }
        }
    }
}

@Composable
private fun MoreMenuScreen(
    onNavigateToNutrition: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToMindfulness: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToSafety: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToProgressPhotos: () -> Unit,
    onNavigateToHealthSync: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "More",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        MoreMenuItem(
            icon = Icons.Default.EmojiEvents,
            title = "Achievements",
            subtitle = "Badges and milestones",
            color = Color(0xFFFFD700),
            onClick = onNavigateToAchievements
        )
        
        MoreMenuItem(
            icon = Icons.Default.Restaurant,
            title = "Nutrition",
            subtitle = "Track meals and macros",
            color = Color(0xFF7EE787),
            onClick = onNavigateToNutrition
        )
        
        MoreMenuItem(
            icon = Icons.Default.CalendarMonth,
            title = "Calendar",
            subtitle = "View your workout schedule",
            color = Color(0xFF00E5CC),
            onClick = onNavigateToCalendar
        )
        
        MoreMenuItem(
            icon = Icons.Default.SelfImprovement,
            title = "Mindfulness",
            subtitle = "Meditation and mental wellness",
            color = Color(0xFFBA68C8),
            onClick = onNavigateToMindfulness
        )
        
        MoreMenuItem(
            icon = Icons.Default.Analytics,
            title = "Analysis",
            subtitle = "Body scan and insights",
            color = Color(0xFFFFA657),
            onClick = onNavigateToAnalysis
        )
        
        MoreMenuItem(
            icon = Icons.Default.Shield,
            title = "Safety",
            subtitle = "Emergency contacts and SOS features",
            color = Color(0xFFF85149),
            onClick = onNavigateToSafety
        )
        
        MoreMenuItem(
            icon = Icons.Default.PhotoCamera,
            title = "Progress Photos",
            subtitle = "Track your body transformation",
            color = Color(0xFF64B5F6),
            onClick = onNavigateToProgressPhotos
        )
        
        MoreMenuItem(
            icon = Icons.Default.Sync,
            title = "Health Sync",
            subtitle = "Connect Google Fit & health apps",
            color = Color(0xFF4CAF50),
            onClick = onNavigateToHealthSync
        )
    }
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
