package com.runtracker.app.ui.screens.nutrition

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.hilt.navigation.compose.hiltViewModel
import com.runtracker.shared.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionDashboardScreen(
    onBack: (() -> Unit)? = null,
    onViewSettings: () -> Unit,
    onScanFood: () -> Unit,
    viewModel: NutritionDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showQuickLogDialog by remember { mutableStateOf(false) }
    var showWaterDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Health Connect permission launcher
    val healthPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(healthPermissions)) {
            viewModel.refreshSteps()
        }
    }
    
    // Check and request permissions on first load
    LaunchedEffect(Unit) {
        try {
            val status = HealthConnectClient.getSdkStatus(context)
            if (status == HealthConnectClient.SDK_AVAILABLE) {
                val client = HealthConnectClient.getOrCreate(context)
                val grantedPermissions = client.permissionController.getGrantedPermissions()
                if (!grantedPermissions.containsAll(healthPermissions)) {
                    permissionLauncher.launch(healthPermissions)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NutritionDashboard", "Health Connect error", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onViewSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // Expanded FAB options
                androidx.compose.animation.AnimatedVisibility(visible = showFabMenu) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Scan Food FAB
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                onScanFood()
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Food")
                        }
                        
                        // Quick Log FAB
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                showQuickLogDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quick Log")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // Main FAB
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Log Meal"
                    )
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Day Type Banner
                item {
                    DayTypeBanner(dayType = uiState.dayType)
                }

                // Calorie Overview Card
                item {
                    uiState.todayNutrition?.let { nutrition ->
                        CalorieOverviewCard(
                            nutrition = nutrition,
                            onAddWater = { showWaterDialog = true }
                        )
                    }
                }

                // Macro Progress
                item {
                    uiState.todayNutrition?.let { nutrition ->
                        MacroProgressCard(nutrition = nutrition)
                    }
                }

                // Tips Section
                if (uiState.tips.isNotEmpty()) {
                    item {
                        TipsCard(tips = uiState.tips)
                    }
                }

                // Meal Suggestions
                if (uiState.mealSuggestions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Suggested for ${uiState.currentMealType.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.mealSuggestions) { suggestion ->
                                MealSuggestionCard(
                                    suggestion = suggestion,
                                    onLog = { viewModel.logMealFromSuggestion(suggestion) }
                                )
                            }
                        }
                    }
                }

                // Today's Meals
                uiState.todayNutrition?.let { nutrition ->
                    if (nutrition.meals.isNotEmpty()) {
                        item {
                            Text(
                                text = "Today's Meals",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(nutrition.meals.sortedByDescending { it.timestamp }) { meal ->
                            MealEntryCard(
                                meal = meal,
                                onRemove = { viewModel.removeMeal(meal.id) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showQuickLogDialog) {
        QuickLogMealDialog(
            onDismiss = { showQuickLogDialog = false },
            onLog = { name, calories, protein, carbs, fat ->
                viewModel.logQuickMeal(name, calories, protein, carbs, fat)
                showQuickLogDialog = false
            }
        )
    }

    if (showWaterDialog) {
        WaterLogDialog(
            onDismiss = { showWaterDialog = false },
            onLog = { ml ->
                viewModel.addWater(ml)
                showWaterDialog = false
            }
        )
    }
}

@Composable
fun DayTypeBanner(dayType: DayType) {
    val (icon, text, color) = when (dayType) {
        DayType.STRENGTH_DAY -> Triple(Icons.Default.FitnessCenter, "Strength Training Day", Color(0xFF7C4DFF))
        DayType.CARDIO_DAY -> Triple(Icons.Default.DirectionsRun, "Cardio Day", Color(0xFF00BCD4))
        DayType.HIGH_ACTIVITY -> Triple(Icons.Default.LocalFireDepartment, "High Activity Day", Color(0xFFFF5722))
        DayType.LIGHT_ACTIVITY -> Triple(Icons.Default.DirectionsWalk, "Light Activity Day", Color(0xFF4CAF50))
        DayType.REST_DAY -> Triple(Icons.Default.SelfImprovement, "Rest Day", Color(0xFF9E9E9E))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
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
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "Nutrition adjusted for your activity level",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CalorieOverviewCard(
    nutrition: DailyNutrition,
    onAddWater: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Main calorie display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${nutrition.remainingCalories}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "calories",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                // Circular progress
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    CircularProgressIndicator(
                        progress = nutrition.calorieProgress.coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 8.dp,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                        color = if (nutrition.calorieProgress > 1f) 
                            Color(0xFFE57373) 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(nutrition.calorieProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calorie breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CalorieBreakdownItem(
                    label = "Eaten",
                    value = nutrition.consumedCalories,
                    icon = Icons.Default.Restaurant
                )
                CalorieBreakdownItem(
                    label = "Burned",
                    value = nutrition.burnedCalories,
                    icon = Icons.Default.LocalFireDepartment
                )
                CalorieBreakdownItem(
                    label = "Steps",
                    value = nutrition.stepCount,
                    icon = Icons.Default.DirectionsWalk,
                    isSteps = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Water tracker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                    .clickable(onClick = onAddWater)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${nutrition.waterMl} / ${nutrition.targetWaterMl} ml",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    LinearProgressIndicator(
                        progress = nutrition.waterProgress.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF2196F3),
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add water",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun CalorieBreakdownItem(
    label: String,
    value: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSteps: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isSteps) value.toString() else "$value cal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun MacroProgressCard(nutrition: DailyNutrition) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Macros",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroCircle(
                    label = "Protein",
                    consumed = nutrition.consumedProteinGrams,
                    target = nutrition.targetProteinGrams,
                    progress = nutrition.proteinProgress,
                    color = Color(0xFFE91E63),
                    unit = "g"
                )
                MacroCircle(
                    label = "Carbs",
                    consumed = nutrition.consumedCarbsGrams,
                    target = nutrition.targetCarbsGrams,
                    progress = nutrition.carbsProgress,
                    color = Color(0xFF2196F3),
                    unit = "g"
                )
                MacroCircle(
                    label = "Fat",
                    consumed = nutrition.consumedFatGrams,
                    target = nutrition.targetFatGrams,
                    progress = nutrition.fatProgress,
                    color = Color(0xFFFF9800),
                    unit = "g"
                )
            }
        }
    }
}

@Composable
fun MacroCircle(
    label: String,
    consumed: Int,
    target: Int,
    progress: Float,
    color: Color,
    unit: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(70.dp)
        ) {
            CircularProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 6.dp,
                trackColor = color.copy(alpha = 0.2f),
                color = color
            )
            Text(
                text = "$consumed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
        Text(
            text = "/ $target$unit",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TipsCard(tips: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tips for Today",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            tips.forEach { tip ->
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MealSuggestionCard(
    suggestion: MealSuggestion,
    onLog: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onLog)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = suggestion.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = suggestion.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${suggestion.calories} cal",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${suggestion.proteinGrams.toInt()}g protein",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE91E63)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (suggestion.tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(suggestion.tags.take(2)) { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onLog,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun MealEntryCard(
    meal: MealEntry,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meal.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = meal.mealType.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${meal.calories} cal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MacroChip("P: ${meal.proteinGrams.toInt()}g", Color(0xFFE91E63))
                    MacroChip("C: ${meal.carbsGrams.toInt()}g", Color(0xFF2196F3))
                    MacroChip("F: ${meal.fatGrams.toInt()}g", Color(0xFFFF9800))
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onRemove,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove")
                }
            }
        }
    }
}

@Composable
fun MacroChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun QuickLogMealDialog(
    onDismiss: () -> Unit,
    onLog: (String, Int, Int, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Meal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meal Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { c -> c.isDigit() } },
                    label = { Text("Calories") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it.filter { c -> c.isDigit() } },
                        label = { Text("Protein (g)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { c -> c.isDigit() } },
                        label = { Text("Carbs (g)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it.filter { c -> c.isDigit() } },
                    label = { Text("Fat (g)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onLog(
                        name.ifEmpty { "Quick Meal" },
                        calories.toIntOrNull() ?: 0,
                        protein.toIntOrNull() ?: 0,
                        carbs.toIntOrNull() ?: 0,
                        fat.toIntOrNull() ?: 0
                    )
                },
                enabled = calories.isNotEmpty()
            ) {
                Text("Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun WaterLogDialog(
    onDismiss: () -> Unit,
    onLog: (Int) -> Unit
) {
    val presets = listOf(250, 500, 750, 1000)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Water") },
        text = {
            Column {
                Text(
                    text = "Quick add:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    presets.forEach { ml ->
                        FilledTonalButton(
                            onClick = { onLog(ml) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("${ml}ml")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

val MealType.displayName: String
    get() = when (this) {
        MealType.BREAKFAST -> "Breakfast"
        MealType.MORNING_SNACK -> "Morning Snack"
        MealType.LUNCH -> "Lunch"
        MealType.AFTERNOON_SNACK -> "Afternoon Snack"
        MealType.DINNER -> "Dinner"
        MealType.EVENING_SNACK -> "Evening Snack"
    }
