@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.runtracker.app.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.NutritionRepository
import com.runtracker.shared.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateAge(age: String) {
        _uiState.update { it.copy(age = age) }
    }

    fun updateWeight(weight: String) {
        _uiState.update { it.copy(weight = weight) }
    }

    fun updateHeight(height: String) {
        _uiState.update { it.copy(height = height) }
    }

    fun updateGender(gender: Gender) {
        _uiState.update { it.copy(gender = gender) }
    }

    fun updateGoal(goal: NutritionGoalType) {
        _uiState.update { it.copy(nutritionGoal = goal) }
    }

    fun updateActivityLevel(level: ActivityLevel) {
        _uiState.update { it.copy(activityLevel = level) }
    }

    fun updateWeeklyRunGoal(goal: String) {
        _uiState.update { it.copy(weeklyRunGoalKm = goal) }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val state = _uiState.value
            
            // Save user profile
            val profile = UserProfile(
                id = 1,
                name = state.name.ifEmpty { "Runner" },
                age = state.age.toIntOrNull(),
                weight = state.weight.toDoubleOrNull(),
                height = state.height.toDoubleOrNull(),
                gender = state.gender,
                weeklyGoalKm = state.weeklyRunGoalKm.toDoubleOrNull() ?: 20.0,
                isOnboardingComplete = true
            )
            userRepository.saveProfile(profile)
            
            // Save nutrition goals
            val nutritionGoals = NutritionGoals(
                goal = state.nutritionGoal,
                activityLevel = state.activityLevel
            )
            nutritionRepository.saveGoals(nutritionGoals)
            
            _uiState.update { it.copy(isComplete = true) }
        }
    }
}

data class OnboardingUiState(
    val name: String = "",
    val age: String = "",
    val weight: String = "",
    val height: String = "",
    val gender: Gender? = null,
    val nutritionGoal: NutritionGoalType = NutritionGoalType.MAINTAIN,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATELY_ACTIVE,
    val weeklyRunGoalKm: String = "20",
    val isComplete: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = (pagerState.currentPage + 1) / 5f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> ProfilePage(
                    name = uiState.name,
                    age = uiState.age,
                    gender = uiState.gender,
                    onNameChange = viewModel::updateName,
                    onAgeChange = viewModel::updateAge,
                    onGenderChange = viewModel::updateGender
                )
                2 -> BodyMetricsPage(
                    weight = uiState.weight,
                    height = uiState.height,
                    onWeightChange = viewModel::updateWeight,
                    onHeightChange = viewModel::updateHeight
                )
                3 -> GoalsPage(
                    nutritionGoal = uiState.nutritionGoal,
                    activityLevel = uiState.activityLevel,
                    onGoalChange = viewModel::updateGoal,
                    onActivityChange = viewModel::updateActivityLevel
                )
                4 -> FitnessGoalsPage(
                    weeklyGoal = uiState.weeklyRunGoalKm,
                    onGoalChange = viewModel::updateWeeklyRunGoal
                )
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (pagerState.currentPage > 0) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < 4) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        viewModel.completeOnboarding()
                    }
                }
            ) {
                Text(if (pagerState.currentPage < 4) "Next" else "Get Started")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (pagerState.currentPage < 4) Icons.Default.ArrowForward else Icons.Default.Check,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsRun,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome to RunTracker",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your all-in-one fitness companion for running, gym workouts, and nutrition tracking",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            FeatureIcon(Icons.Default.DirectionsRun, "Running")
            FeatureIcon(Icons.Default.FitnessCenter, "Gym")
            FeatureIcon(Icons.Default.Restaurant, "Nutrition")
        }
    }
}

@Composable
fun FeatureIcon(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePage(
    name: String,
    age: String,
    gender: Gender?,
    onNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onGenderChange: (Gender) -> Unit
) {
    OnboardingPageLayout(
        title = "About You",
        subtitle = "Let's personalize your experience"
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = age,
            onValueChange = { if (it.all { c -> c.isDigit() }) onAgeChange(it) },
            label = { Text("Age") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Gender",
            style = MaterialTheme.typography.labelLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GenderOption.entries.forEach { option ->
                FilterChip(
                    selected = gender == option.gender,
                    onClick = { onGenderChange(option.gender) },
                    label = { Text(option.label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

enum class GenderOption(val gender: Gender, val label: String) {
    MALE(Gender.MALE, "Male"),
    FEMALE(Gender.FEMALE, "Female"),
    OTHER(Gender.OTHER, "Other")
}

@Composable
fun BodyMetricsPage(
    weight: String,
    height: String,
    onWeightChange: (String) -> Unit,
    onHeightChange: (String) -> Unit
) {
    OnboardingPageLayout(
        title = "Body Metrics",
        subtitle = "Used for accurate calorie and pace calculations"
    ) {
        OutlinedTextField(
            value = weight,
            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) onWeightChange(it) },
            label = { Text("Weight (kg)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            leadingIcon = { Icon(Icons.Default.MonitorWeight, contentDescription = null) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = height,
            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) onHeightChange(it) },
            label = { Text("Height (cm)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            leadingIcon = { Icon(Icons.Default.Height, contentDescription = null) }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Your data stays on your device and is never shared",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsPage(
    nutritionGoal: NutritionGoalType,
    activityLevel: ActivityLevel,
    onGoalChange: (NutritionGoalType) -> Unit,
    onActivityChange: (ActivityLevel) -> Unit
) {
    OnboardingPageLayout(
        title = "Your Goals",
        subtitle = "We'll customize your nutrition targets"
    ) {
        Text(
            text = "What's your main goal?",
            style = MaterialTheme.typography.labelLarge
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        NutritionGoalType.entries.forEach { goal ->
            GoalOptionCard(
                title = goal.displayName,
                description = when (goal) {
                    NutritionGoalType.LOSE_WEIGHT -> "500 calorie deficit per day"
                    NutritionGoalType.LOSE_WEIGHT_SLOW -> "250 calorie deficit per day"
                    NutritionGoalType.MAINTAIN -> "Maintain current weight"
                    NutritionGoalType.GAIN_MUSCLE -> "250 calorie surplus per day"
                    NutritionGoalType.BULK -> "500 calorie surplus per day"
                },
                isSelected = nutritionGoal == goal,
                onClick = { onGoalChange(goal) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Activity Level",
            style = MaterialTheme.typography.labelLarge
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActivityLevel.entries.take(3).forEach { level ->
                FilterChip(
                    selected = activityLevel == level,
                    onClick = { onActivityChange(level) },
                    label = { 
                        Text(
                            level.displayName.split(" ").first(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActivityLevel.entries.drop(3).forEach { level ->
                FilterChip(
                    selected = activityLevel == level,
                    onClick = { onActivityChange(level) },
                    label = { 
                        Text(
                            level.displayName.split(" ").first(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) null else CardDefaults.outlinedCardBorder()
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
                    text = title,
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
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessGoalsPage(
    weeklyGoal: String,
    onGoalChange: (String) -> Unit
) {
    OnboardingPageLayout(
        title = "Running Goals",
        subtitle = "Set your weekly running target"
    ) {
        Text(
            text = "Weekly Distance Goal",
            style = MaterialTheme.typography.labelLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("10", "20", "30", "50").forEach { preset ->
                FilterChip(
                    selected = weeklyGoal == preset,
                    onClick = { onGoalChange(preset) },
                    label = { Text("${preset}km") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = weeklyGoal,
            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) onGoalChange(it) },
            label = { Text("Custom (km)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "You're all set! 🎉",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap 'Get Started' to begin your fitness journey. You can always adjust these settings later in your profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun OnboardingPageLayout(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        content()
    }
}
