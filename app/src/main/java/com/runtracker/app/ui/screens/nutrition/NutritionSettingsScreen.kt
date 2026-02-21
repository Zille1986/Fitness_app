package com.runtracker.app.ui.screens.nutrition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.NutritionRepository
import com.runtracker.shared.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NutritionSettingsViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionSettingsUiState())
    val uiState: StateFlow<NutritionSettingsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            nutritionRepository.getGoals().collect { goals ->
                goals?.let { g ->
                    _uiState.update {
                        it.copy(
                            goal = g.goal,
                            activityLevel = g.activityLevel,
                            proteinPerKg = g.proteinPerKgBodyweight.toString(),
                            dailyStepGoal = g.dailyStepGoal.toString(),
                            mealFrequency = g.mealFrequency,
                            isLoading = false
                        )
                    }
                } ?: run {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }

        viewModelScope.launch {
            userRepository.getProfile().collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
        }
    }

    fun updateGoal(goal: NutritionGoalType) {
        _uiState.update { it.copy(goal = goal) }
    }

    fun updateActivityLevel(level: ActivityLevel) {
        _uiState.update { it.copy(activityLevel = level) }
    }

    fun updateProteinPerKg(value: String) {
        _uiState.update { it.copy(proteinPerKg = value) }
    }

    fun updateDailyStepGoal(value: String) {
        _uiState.update { it.copy(dailyStepGoal = value) }
    }

    fun updateMealFrequency(value: Int) {
        _uiState.update { it.copy(mealFrequency = value) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            val goals = NutritionGoals(
                goal = state.goal,
                activityLevel = state.activityLevel,
                proteinPerKgBodyweight = state.proteinPerKg.toDoubleOrNull() ?: 1.6,
                dailyStepGoal = state.dailyStepGoal.toIntOrNull() ?: 10000,
                mealFrequency = state.mealFrequency
            )
            nutritionRepository.saveGoals(goals)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}

data class NutritionSettingsUiState(
    val goal: NutritionGoalType = NutritionGoalType.MAINTAIN,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATELY_ACTIVE,
    val proteinPerKg: String = "1.6",
    val dailyStepGoal: String = "10000",
    val mealFrequency: Int = 4,
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionSettingsScreen(
    onBack: () -> Unit,
    viewModel: NutritionSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveSettings() }) {
                        Text("Save")
                    }
                }
            )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Calorie Preview
                uiState.userProfile?.let { profile ->
                    CaloriePreviewCard(
                        profile = profile,
                        goal = uiState.goal,
                        activityLevel = uiState.activityLevel
                    )
                }

                // Goal Selection
                SettingsSection(title = "Your Goal") {
                    NutritionGoalType.values().forEach { goal ->
                        GoalOption(
                            goal = goal,
                            isSelected = uiState.goal == goal,
                            onClick = { viewModel.updateGoal(goal) }
                        )
                    }
                }

                // Activity Level
                SettingsSection(title = "Activity Level") {
                    ActivityLevel.values().forEach { level ->
                        ActivityLevelOption(
                            level = level,
                            isSelected = uiState.activityLevel == level,
                            onClick = { viewModel.updateActivityLevel(level) }
                        )
                    }
                }

                // Protein Target
                SettingsSection(title = "Protein Target") {
                    OutlinedTextField(
                        value = uiState.proteinPerKg,
                        onValueChange = { viewModel.updateProteinPerKg(it) },
                        label = { Text("Grams per kg bodyweight") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = {
                            Text("Recommended: 1.6-2.2g for muscle building")
                        }
                    )
                }

                // Step Goal
                SettingsSection(title = "Daily Step Goal") {
                    OutlinedTextField(
                        value = uiState.dailyStepGoal,
                        onValueChange = { viewModel.updateDailyStepGoal(it) },
                        label = { Text("Steps per day") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = {
                            Text("Affects calorie adjustments on active days")
                        }
                    )
                }

                // Meal Frequency
                SettingsSection(title = "Meals Per Day") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (3..6).forEach { count ->
                            FilterChip(
                                selected = uiState.mealFrequency == count,
                                onClick = { viewModel.updateMealFrequency(count) },
                                label = { Text("$count") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun CaloriePreviewCard(
    profile: UserProfile,
    goal: NutritionGoalType,
    activityLevel: ActivityLevel
) {
    val calories = NutritionCalculator.calculateDailyCalories(
        weightKg = profile.weight ?: 70.0,
        heightCm = profile.height ?: 170.0,
        ageYears = profile.age ?: 30,
        gender = profile.gender,
        activityLevel = activityLevel,
        goal = goal
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your Daily Target",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$calories",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "calories per day",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
fun GoalOption(
    goal: NutritionGoalType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val description = when (goal) {
        NutritionGoalType.LOSE_WEIGHT -> "500 calorie deficit"
        NutritionGoalType.LOSE_WEIGHT_SLOW -> "250 calorie deficit"
        NutritionGoalType.MAINTAIN -> "Maintenance calories"
        NutritionGoalType.GAIN_MUSCLE -> "250 calorie surplus"
        NutritionGoalType.BULK -> "500 calorie surplus"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = goal.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ActivityLevelOption(
    level: ActivityLevel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val description = when (level) {
        ActivityLevel.SEDENTARY -> "Little to no exercise"
        ActivityLevel.LIGHTLY_ACTIVE -> "Light exercise 1-3 days/week"
        ActivityLevel.MODERATELY_ACTIVE -> "Moderate exercise 3-5 days/week"
        ActivityLevel.VERY_ACTIVE -> "Hard exercise 6-7 days/week"
        ActivityLevel.EXTREMELY_ACTIVE -> "Very hard exercise, physical job"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = level.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
