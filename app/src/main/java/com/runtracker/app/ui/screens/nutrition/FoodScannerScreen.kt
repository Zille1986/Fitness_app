package com.runtracker.app.ui.screens.nutrition

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.runtracker.app.ai.Confidence
import com.runtracker.app.ai.FoodAnalysisResult
import com.runtracker.app.ai.GeminiFoodAnalyzer
import com.runtracker.shared.data.model.MealEntry
import com.runtracker.shared.data.model.MealType
import com.runtracker.shared.data.repository.NutritionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.inject.Inject

@HiltViewModel
class FoodScannerViewModel @Inject constructor(
    private val foodAnalyzer: GeminiFoodAnalyzer,
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodScannerUiState())
    val uiState: StateFlow<FoodScannerUiState> = _uiState.asStateFlow()

    fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null) }
            
            when (val result = foodAnalyzer.analyzeFoodImage(bitmap)) {
                is FoodAnalysisResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            analysisResult = result,
                            capturedBitmap = bitmap
                        )
                    }
                }
                is FoodAnalysisResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun logMeal() {
        viewModelScope.launch {
            val result = _uiState.value.analysisResult ?: return@launch
            
            val meal = MealEntry(
                id = UUID.randomUUID().toString(),
                name = result.foodName,
                mealType = getCurrentMealType(),
                calories = result.calories,
                proteinGrams = result.proteinGrams,
                carbsGrams = result.carbsGrams,
                fatGrams = result.fatGrams,
                fiberGrams = result.fiberGrams,
                notes = "AI estimated (${result.confidence.name.lowercase()} confidence)"
            )
            
            nutritionRepository.addMeal(meal)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun updateCalories(calories: Int) {
        _uiState.update { state ->
            state.analysisResult?.let { result ->
                state.copy(analysisResult = result.copy(calories = calories))
            } ?: state
        }
    }

    fun updateProtein(protein: Double) {
        _uiState.update { state ->
            state.analysisResult?.let { result ->
                state.copy(analysisResult = result.copy(proteinGrams = protein))
            } ?: state
        }
    }

    fun updateCarbs(carbs: Double) {
        _uiState.update { state ->
            state.analysisResult?.let { result ->
                state.copy(analysisResult = result.copy(carbsGrams = carbs))
            } ?: state
        }
    }

    fun updateFat(fat: Double) {
        _uiState.update { state ->
            state.analysisResult?.let { result ->
                state.copy(analysisResult = result.copy(fatGrams = fat))
            } ?: state
        }
    }

    fun clearResult() {
        _uiState.update { 
            FoodScannerUiState()
        }
    }

    private fun getCurrentMealType(): MealType {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 10 -> MealType.BREAKFAST
            hour < 12 -> MealType.MORNING_SNACK
            hour < 14 -> MealType.LUNCH
            hour < 17 -> MealType.AFTERNOON_SNACK
            hour < 20 -> MealType.DINNER
            else -> MealType.EVENING_SNACK
        }
    }
}

data class FoodScannerUiState(
    val capturedBitmap: Bitmap? = null,
    val isAnalyzing: Boolean = false,
    val analysisResult: FoodAnalysisResult.Success? = null,
    val error: String? = null,
    val isSaved: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun FoodScannerScreen(
    onBack: () -> Unit,
    onMealLogged: () -> Unit,
    viewModel: FoodScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            val inputStream = context.contentResolver.openInputStream(photoUri!!)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            // Scale down for API
            val scaledBitmap = scaleBitmap(bitmap, 1024)
            viewModel.analyzeImage(scaledBitmap)
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            val scaledBitmap = scaleBitmap(bitmap, 1024)
            viewModel.analyzeImage(scaledBitmap)
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onMealLogged()
        }
    }

    fun takePhoto() {
        val photoFile = File.createTempFile(
            "food_${System.currentTimeMillis()}",
            ".jpg",
            context.cacheDir
        )
        photoUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
        cameraLauncher.launch(photoUri!!)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Food") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.analysisResult == null && !uiState.isAnalyzing) {
                // Initial state - show capture options
                CaptureOptionsCard(
                    onTakePhoto = {
                        if (cameraPermissionState.status.isGranted) {
                            takePhoto()
                        } else {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    onSelectFromGallery = {
                        galleryLauncher.launch("image/*")
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                HowItWorksCard()
            }
            
            // Analyzing state
            AnimatedVisibility(
                visible = uiState.isAnalyzing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AnalyzingCard()
            }
            
            // Error state
            uiState.error?.let { error ->
                ErrorCard(
                    error = error,
                    onRetry = { viewModel.clearResult() }
                )
            }
            
            // Result state
            uiState.analysisResult?.let { result ->
                uiState.capturedBitmap?.let { bitmap ->
                    FoodImagePreview(bitmap = bitmap)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                AnalysisResultCard(
                    result = result,
                    onCaloriesChange = { viewModel.updateCalories(it) },
                    onProteinChange = { viewModel.updateProtein(it) },
                    onCarbsChange = { viewModel.updateCarbs(it) },
                    onFatChange = { viewModel.updateFat(it) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clearResult() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retake")
                    }
                    
                    Button(
                        onClick = { viewModel.logMeal() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Meal")
                    }
                }
            }
        }
    }
}

@Composable
fun CaptureOptionsCard(
    onTakePhoto: () -> Unit,
    onSelectFromGallery: () -> Unit
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
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Scan Your Food",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Take a photo or select from gallery to estimate calories using AI",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onTakePhoto,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Camera")
                }
                
                OutlinedButton(
                    onClick = onSelectFromGallery,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gallery")
                }
            }
        }
    }
}

@Composable
fun HowItWorksCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "How it works",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            HowItWorksStep(
                number = "1",
                title = "Take a photo",
                description = "Capture your meal from above for best results"
            )
            
            HowItWorksStep(
                number = "2",
                title = "AI Analysis",
                description = "Gemini AI identifies foods and estimates nutrition"
            )
            
            HowItWorksStep(
                number = "3",
                title = "Review & Log",
                description = "Adjust estimates if needed and log your meal"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "⚠️ AI estimates may vary. For precise tracking, manually verify nutritional information.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HowItWorksStep(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AnalyzingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Analyzing your food...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Gemini AI is identifying foods and estimating nutritional content",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ErrorCard(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Analysis Failed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again")
            }
        }
    }
}

@Composable
fun FoodImagePreview(bitmap: Bitmap) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Food photo",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun AnalysisResultCard(
    result: FoodAnalysisResult.Success,
    onCaloriesChange: (Int) -> Unit,
    onProteinChange: (Double) -> Unit,
    onCarbsChange: (Double) -> Unit,
    onFatChange: (Double) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = result.foodName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = result.portionSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                ConfidenceBadge(confidence = result.confidence)
            }
            
            if (result.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Detected items
            if (result.itemsDetected.isNotEmpty()) {
                Text(
                    text = "Detected: ${result.itemsDetected.joinToString(", ")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Editable nutrition values
            Text(
                text = "Estimated Nutrition (tap to edit)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Calories - prominent
            EditableNutritionRow(
                label = "Calories",
                value = result.calories.toString(),
                unit = "cal",
                color = MaterialTheme.colorScheme.primary,
                isLarge = true,
                onValueChange = { onCaloriesChange(it.toIntOrNull() ?: result.calories) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Macros
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditableNutritionRow(
                    label = "Protein",
                    value = result.proteinGrams.toInt().toString(),
                    unit = "g",
                    color = Color(0xFFE91E63),
                    modifier = Modifier.weight(1f),
                    onValueChange = { onProteinChange(it.toDoubleOrNull() ?: result.proteinGrams) }
                )
                EditableNutritionRow(
                    label = "Carbs",
                    value = result.carbsGrams.toInt().toString(),
                    unit = "g",
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f),
                    onValueChange = { onCarbsChange(it.toDoubleOrNull() ?: result.carbsGrams) }
                )
                EditableNutritionRow(
                    label = "Fat",
                    value = result.fatGrams.toInt().toString(),
                    unit = "g",
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f),
                    onValueChange = { onFatChange(it.toDoubleOrNull() ?: result.fatGrams) }
                )
            }
            
            // Suggestions
            if (result.suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = result.suggestions,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfidenceBadge(confidence: Confidence) {
    val (color, text) = when (confidence) {
        Confidence.HIGH -> Pair(Color(0xFF4CAF50), "High confidence")
        Confidence.MEDIUM -> Pair(Color(0xFFFF9800), "Medium confidence")
        Confidence.LOW -> Pair(Color(0xFFF44336), "Low confidence")
    }
    
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
fun EditableNutritionRow(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false,
    onValueChange: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember(value) { mutableStateOf(value) }
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        if (isEditing) {
            OutlinedTextField(
                value = editValue,
                onValueChange = { editValue = it },
                modifier = Modifier.width(if (isLarge) 100.dp else 60.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = color,
                    cursorColor = color
                )
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row {
                TextButton(
                    onClick = { isEditing = false },
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(
                    onClick = {
                        onValueChange(editValue)
                        isEditing = false
                    },
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text("Save", style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.clickable { isEditing = true }
            ) {
                Text(
                    text = value,
                    style = if (isLarge) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
    val ratio = minOf(
        maxSize.toFloat() / bitmap.width,
        maxSize.toFloat() / bitmap.height
    )
    
    if (ratio >= 1f) return bitmap
    
    val newWidth = (bitmap.width * ratio).toInt()
    val newHeight = (bitmap.height * ratio).toInt()
    
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

@Composable
private fun Modifier.clickable(onClick: () -> Unit): Modifier {
    return this.then(
        Modifier.clickable(onClick = onClick)
    )
}
