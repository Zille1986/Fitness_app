package com.runtracker.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val key: String,
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val subtitle: String = "",
    val color: Color = Color.Unspecified
) {
    object Home : BottomNavItem(
        key = "Home",
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        subtitle = "Dashboard overview",
        color = Color(0xFF00E5CC)
    )
    object Running : BottomNavItem(
        key = "Running",
        route = "running_dashboard",
        title = "Run",
        selectedIcon = Icons.Filled.DirectionsRun,
        unselectedIcon = Icons.Outlined.DirectionsRun,
        subtitle = "Track runs and plans",
        color = Color(0xFF00E5CC)
    )
    object Swimming : BottomNavItem(
        key = "Swimming",
        route = "swimming_dashboard",
        title = "Swim",
        selectedIcon = Icons.Filled.Pool,
        unselectedIcon = Icons.Outlined.Pool,
        subtitle = "Pool and open water",
        color = Color(0xFF64B5F6)
    )
    object Cycling : BottomNavItem(
        key = "Cycling",
        route = "cycling_dashboard",
        title = "Bike",
        selectedIcon = Icons.Filled.DirectionsBike,
        unselectedIcon = Icons.Outlined.DirectionsBike,
        subtitle = "Cycling workouts",
        color = Color(0xFFFFA657)
    )
    object Gym : BottomNavItem(
        key = "Gym",
        route = "gym_dashboard_new",
        title = "Gym",
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter,
        subtitle = "Weight training",
        color = Color(0xFFF85149)
    )
    object Nutrition : BottomNavItem(
        key = "Nutrition",
        route = "nutrition",
        title = "Nutrition",
        selectedIcon = Icons.Filled.Restaurant,
        unselectedIcon = Icons.Outlined.Restaurant,
        subtitle = "Track meals and macros",
        color = Color(0xFF7EE787)
    )
    object Analysis : BottomNavItem(
        key = "Analysis",
        route = "analysis_dashboard",
        title = "Analysis",
        selectedIcon = Icons.Filled.Analytics,
        unselectedIcon = Icons.Outlined.Analytics,
        subtitle = "Body scan and insights",
        color = Color(0xFFFFA657)
    )
    object Mindfulness : BottomNavItem(
        key = "Mindfulness",
        route = "mindfulness_tab",
        title = "Mind",
        selectedIcon = Icons.Filled.SelfImprovement,
        unselectedIcon = Icons.Outlined.SelfImprovement,
        subtitle = "Meditation and wellness",
        color = Color(0xFFBA68C8)
    )
    object Calendar : BottomNavItem(
        key = "Calendar",
        route = "calendar_tab",
        title = "Calendar",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        subtitle = "Workout schedule",
        color = Color(0xFF00E5CC)
    )
    object More : BottomNavItem(
        key = "More",
        route = "more_menu",
        title = "More",
        selectedIcon = Icons.Filled.MoreHoriz,
        unselectedIcon = Icons.Outlined.MoreHoriz,
        subtitle = "Additional features",
        color = Color(0xFF9E9E9E)
    )
}

/** All nav items indexed by key for lookup */
val allNavItems: Map<String, BottomNavItem> = mapOf(
    "Home" to BottomNavItem.Home,
    "Running" to BottomNavItem.Running,
    "Swimming" to BottomNavItem.Swimming,
    "Cycling" to BottomNavItem.Cycling,
    "Gym" to BottomNavItem.Gym,
    "Nutrition" to BottomNavItem.Nutrition,
    "Analysis" to BottomNavItem.Analysis,
    "Mindfulness" to BottomNavItem.Mindfulness,
    "Calendar" to BottomNavItem.Calendar,
    "More" to BottomNavItem.More
)

/** Items that can be placed in the middle of the nav bar (everything except Home and More) */
val customizableNavItems: List<BottomNavItem> = listOf(
    BottomNavItem.Running,
    BottomNavItem.Swimming,
    BottomNavItem.Cycling,
    BottomNavItem.Gym,
    BottomNavItem.Nutrition,
    BottomNavItem.Analysis,
    BottomNavItem.Mindfulness,
    BottomNavItem.Calendar
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(
    mainNavController: NavHostController
) {
    val context = LocalContext.current
    val navBarPrefs = remember { NavBarPreferences(context) }
    var activeNavItems by remember {
        mutableStateOf(
            navBarPrefs.getSelectedItems().mapNotNull { allNavItems[it] }
        )
    }
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
                activeNavItems.forEach { item ->
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
                    },
                    onViewHIIT = {
                        mainNavController.navigate(Screen.HIITDashboard.route)
                    }
                )
            }

            composable(BottomNavItem.Calendar.route) {
                CalendarScreen()
            }

            composable(BottomNavItem.Swimming.route) {
                SwimmingDashboardScreen(
                    onStartWorkout = { swimType, poolLength ->
                        mainNavController.navigate(Screen.ActiveSwimWorkout.createRoute(swimType.name, poolLength?.name))
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
                    activeNavItems = activeNavItems,
                    onNavigateTo = { item ->
                        bottomNavController.navigate(item.route) {
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
                    },
                    onNavigateToCustomizeNavBar = {
                        bottomNavController.navigate("customize_nav_bar") {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable("customize_nav_bar") {
                CustomizeNavBarScreen(
                    navBarPrefs = navBarPrefs,
                    currentItems = activeNavItems,
                    onSave = { newItems ->
                        navBarPrefs.saveSelectedItems(newItems.map { it.key })
                        activeNavItems = newItems
                        bottomNavController.navigate(BottomNavItem.More.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onBack = {
                        bottomNavController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
private fun MoreMenuScreen(
    activeNavItems: List<BottomNavItem>,
    onNavigateTo: (BottomNavItem) -> Unit,
    onNavigateToSafety: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToProgressPhotos: () -> Unit,
    onNavigateToHealthSync: () -> Unit,
    onNavigateToCustomizeNavBar: () -> Unit
) {
    // Items not currently in the bottom bar (excluding Home and More)
    val activeKeys = activeNavItems.map { it.key }.toSet()
    val hiddenNavItems = customizableNavItems.filter { it.key !in activeKeys }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "More",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Customize Tab Bar — always shown at top
        item {
            MoreMenuItem(
                icon = Icons.Default.Dashboard,
                title = "Customize Tab Bar",
                subtitle = "Choose and reorder your tabs",
                color = Color(0xFF7C4DFF),
                onClick = onNavigateToCustomizeNavBar
            )
        }

        // Show nav items that aren't in the bottom bar
        if (hiddenNavItems.isNotEmpty()) {
            item {
                Text(
                    text = "Quick Access",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(hiddenNavItems) { item ->
                MoreMenuItem(
                    icon = item.selectedIcon,
                    title = item.title,
                    subtitle = item.subtitle,
                    color = item.color,
                    onClick = { onNavigateTo(item) }
                )
            }
        }

        // Fixed "More" features that aren't nav tabs
        item {
            Text(
                text = "Features",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            MoreMenuItem(
                icon = Icons.Default.EmojiEvents,
                title = "Achievements",
                subtitle = "Badges and milestones",
                color = Color(0xFFFFD700),
                onClick = onNavigateToAchievements
            )
        }

        item {
            MoreMenuItem(
                icon = Icons.Default.Shield,
                title = "Safety",
                subtitle = "Emergency contacts and SOS features",
                color = Color(0xFFF85149),
                onClick = onNavigateToSafety
            )
        }

        item {
            MoreMenuItem(
                icon = Icons.Default.PhotoCamera,
                title = "Progress Photos",
                subtitle = "Track your body transformation",
                color = Color(0xFF64B5F6),
                onClick = onNavigateToProgressPhotos
            )
        }

        item {
            MoreMenuItem(
                icon = Icons.Default.Sync,
                title = "Health Sync",
                subtitle = "Connect Google Fit & health apps",
                color = Color(0xFF4CAF50),
                onClick = onNavigateToHealthSync
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomizeNavBarScreen(
    navBarPrefs: NavBarPreferences,
    currentItems: List<BottomNavItem>,
    onSave: (List<BottomNavItem>) -> Unit,
    onBack: () -> Unit
) {
    // Middle items only (exclude Home and More)
    var middleItems by remember {
        mutableStateOf(currentItems.filter { it.key != "Home" && it.key != "More" })
    }
    val middleKeys = middleItems.map { it.key }.toSet()
    val availableItems = customizableNavItems.filter { it.key !in middleKeys }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize Tab Bar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val fullList = mutableListOf<BottomNavItem>(BottomNavItem.Home)
                            fullList.addAll(middleItems)
                            fullList.add(BottomNavItem.More)
                            onSave(fullList)
                        }
                    ) {
                        Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preview of the bar
            item {
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home (fixed)
                        NavPreviewChip(BottomNavItem.Home, isPinned = true)
                        // Middle items
                        middleItems.forEach { item ->
                            NavPreviewChip(item, isPinned = false)
                        }
                        // Placeholder slots
                        repeat(NavBarPreferences.MAX_MIDDLE_TABS - middleItems.size) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        // More (fixed)
                        NavPreviewChip(BottomNavItem.More, isPinned = true)
                    }
                }
            }

            // Your tabs section
            item {
                Text(
                    text = "Your Tabs",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            // Home (pinned, not removable)
            item {
                TabConfigItem(
                    item = BottomNavItem.Home,
                    isPinned = true,
                    showReorder = false,
                    canMoveUp = false,
                    canMoveDown = false,
                    onMoveUp = {},
                    onMoveDown = {},
                    onRemove = {}
                )
            }

            // Middle items (reorderable, removable)
            items(middleItems.size) { index ->
                val item = middleItems[index]
                TabConfigItem(
                    item = item,
                    isPinned = false,
                    showReorder = true,
                    canMoveUp = index > 0,
                    canMoveDown = index < middleItems.size - 1,
                    onMoveUp = {
                        val list = middleItems.toMutableList()
                        val temp = list[index]
                        list[index] = list[index - 1]
                        list[index - 1] = temp
                        middleItems = list
                    },
                    onMoveDown = {
                        val list = middleItems.toMutableList()
                        val temp = list[index]
                        list[index] = list[index + 1]
                        list[index + 1] = temp
                        middleItems = list
                    },
                    onRemove = {
                        middleItems = middleItems.toMutableList().apply { removeAt(index) }
                    }
                )
            }

            // More (pinned, not removable)
            item {
                TabConfigItem(
                    item = BottomNavItem.More,
                    isPinned = true,
                    showReorder = false,
                    canMoveUp = false,
                    canMoveDown = false,
                    onMoveUp = {},
                    onMoveDown = {},
                    onRemove = {}
                )
            }

            // Available tabs section
            if (availableItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Available Tabs",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                    if (middleItems.size >= NavBarPreferences.MAX_MIDDLE_TABS) {
                        Text(
                            text = "Remove a tab above to add one from here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                items(availableItems) { item ->
                    AvailableTabItem(
                        item = item,
                        enabled = middleItems.size < NavBarPreferences.MAX_MIDDLE_TABS,
                        onAdd = {
                            if (middleItems.size < NavBarPreferences.MAX_MIDDLE_TABS) {
                                middleItems = middleItems + item
                            }
                        }
                    )
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun NavPreviewChip(item: BottomNavItem, isPinned: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(48.dp)
    ) {
        Icon(
            imageVector = item.selectedIcon,
            contentDescription = item.title,
            tint = if (isPinned) MaterialTheme.colorScheme.onSurfaceVariant
                   else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = if (isPinned) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TabConfigItem(
    item: BottomNavItem,
    isPinned: Boolean,
    showReorder: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPinned)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else
                item.color.copy(alpha = 0.1f)
        ),
        border = if (!isPinned) androidx.compose.foundation.BorderStroke(
            1.dp, item.color.copy(alpha = 0.2f)
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.selectedIcon,
                contentDescription = null,
                tint = if (isPinned) MaterialTheme.colorScheme.onSurfaceVariant else item.color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isPinned) {
                    Text(
                        text = "Pinned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            if (showReorder) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move up",
                        tint = if (canMoveUp) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move down",
                        tint = if (canMoveDown) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AvailableTabItem(
    item: BottomNavItem,
    enabled: Boolean,
    onAdd: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (enabled) 0.3f else 0.15f
            )
        ),
        modifier = Modifier.clickable(enabled = enabled, onClick = onAdd)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.unselectedIcon,
                contentDescription = null,
                tint = item.color.copy(alpha = if (enabled) 1f else 0.4f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f)
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.7f else 0.3f)
                )
            }
            IconButton(
                onClick = onAdd,
                enabled = enabled,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.AddCircleOutline,
                    contentDescription = "Add",
                    tint = if (enabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
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
        shape = RoundedCornerShape(16.dp),
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
                    fontWeight = FontWeight.Bold,
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
