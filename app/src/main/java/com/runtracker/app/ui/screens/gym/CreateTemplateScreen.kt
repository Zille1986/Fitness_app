package com.runtracker.app.ui.screens.gym

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.runtracker.app.BuildConfig
import com.runtracker.shared.data.model.*
import com.runtracker.shared.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateTemplateViewModel @Inject constructor(
    private val gymRepository: GymRepository,
    private val bodyAnalysisRepository: com.runtracker.shared.data.repository.BodyAnalysisRepository
) : ViewModel() {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.7f
            maxOutputTokens = 8192
        }
    )

    private val _uiState = MutableStateFlow(CreateTemplateUiState())
    val uiState: StateFlow<CreateTemplateUiState> = _uiState.asStateFlow()

    init {
        checkBodyScan()
    }

    private fun checkBodyScan() {
        viewModelScope.launch {
            val scan = bodyAnalysisRepository.getLatestScan()
            _uiState.update { it.copy(hasBodyScan = scan != null) }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun addExerciseById(exerciseId: Long) {
        viewModelScope.launch {
            val exercise = gymRepository.getExerciseById(exerciseId) ?: return@launch
            val templateExercise = TemplateExercise(
                id = UUID.randomUUID().toString(),
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                sets = 3,
                targetRepsMin = 8,
                targetRepsMax = 12,
                restSeconds = 90,
                orderIndex = _uiState.value.exercises.size
            )
            _uiState.update { it.copy(exercises = it.exercises + templateExercise) }
        }
    }

    fun removeExercise(index: Int) {
        _uiState.update { state ->
            val updatedExercises = state.exercises.toMutableList().apply {
                removeAt(index)
            }.mapIndexed { i, ex -> ex.copy(orderIndex = i) }
            state.copy(exercises = updatedExercises)
        }
    }

    fun updateExerciseSets(index: Int, sets: Int) {
        _uiState.update { state ->
            val updatedExercises = state.exercises.toMutableList().apply {
                set(index, get(index).copy(sets = sets.coerceIn(1, 10)))
            }
            state.copy(exercises = updatedExercises)
        }
    }

    fun updateExerciseReps(index: Int, minReps: Int, maxReps: Int) {
        _uiState.update { state ->
            val updatedExercises = state.exercises.toMutableList().apply {
                set(index, get(index).copy(
                    targetRepsMin = minReps.coerceIn(1, 50),
                    targetRepsMax = maxReps.coerceIn(minReps, 50)
                ))
            }
            state.copy(exercises = updatedExercises)
        }
    }

    fun updateExerciseRest(index: Int, restSeconds: Int) {
        _uiState.update { state ->
            val updatedExercises = state.exercises.toMutableList().apply {
                set(index, get(index).copy(restSeconds = restSeconds.coerceIn(0, 300)))
            }
            state.copy(exercises = updatedExercises)
        }
    }

    fun moveExerciseUp(index: Int) {
        if (index <= 0) return
        _uiState.update { state ->
            val exercises = state.exercises.toMutableList()
            val temp = exercises[index]
            exercises[index] = exercises[index - 1].copy(orderIndex = index)
            exercises[index - 1] = temp.copy(orderIndex = index - 1)
            state.copy(exercises = exercises)
        }
    }

    fun moveExerciseDown(index: Int) {
        val exercises = _uiState.value.exercises
        if (index >= exercises.size - 1) return
        _uiState.update { state ->
            val list = state.exercises.toMutableList()
            val temp = list[index]
            list[index] = list[index + 1].copy(orderIndex = index)
            list[index + 1] = temp.copy(orderIndex = index + 1)
            state.copy(exercises = list)
        }
    }

    fun saveTemplate() {
        val state = _uiState.value
        if (state.name.isBlank() || state.exercises.isEmpty()) return

        viewModelScope.launch {
            val template = WorkoutTemplate(
                name = state.name.trim(),
                description = state.description.trim(),
                exercises = state.exercises,
                estimatedDurationMinutes = estimateDuration(state.exercises),
                isDefault = false
            )
            gymRepository.insertTemplate(template)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    private fun estimateDuration(exercises: List<TemplateExercise>): Int {
        val totalSets = exercises.sumOf { it.sets }
        val avgRestSeconds = exercises.map { it.restSeconds }.average().toInt()
        val timePerSet = 45 // seconds for actual set
        return ((totalSets * (timePerSet + avgRestSeconds)) / 60).coerceAtLeast(15)
    }

    fun generateWithAI(duration: Int, focus: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, generationError = null) }
            
            try {
                // Get available exercises from database
                val availableExercises = gymRepository.getAllExercises().first()
                
                // Get body scan data for personalization
                val bodyScan = bodyAnalysisRepository.getLatestScan()
                
                val prompt = buildString {
                    appendLine("Create a gym workout template with the following requirements:")
                    appendLine("- Target duration: $duration minutes")
                    appendLine("- Focus area: $focus")
                    appendLine()
                    
                    // Add body scan personalization
                    if (bodyScan != null) {
                        appendLine("USER BODY ANALYSIS DATA (prioritize exercises for these areas):")
                        appendLine("- Fitness Goal: ${bodyScan.userGoal.displayName}")
                        appendLine("- Body Type: ${bodyScan.bodyType.displayName}")
                        appendLine("- Focus Zones to prioritize: ${bodyScan.focusZones.joinToString { it.displayName }}")
                        
                        if (bodyScan.postureAssessment.issues.isNotEmpty()) {
                            appendLine("- Posture Issues to address: ${bodyScan.postureAssessment.issues.joinToString { it.type.displayName }}")
                            appendLine("  (Include corrective exercises for these issues)")
                        }
                        
                        // Include muscle balance info if available
                        val muscleBalance = bodyScan.muscleBalance
                        if (muscleBalance.imbalances.isNotEmpty()) {
                            appendLine("- Muscle imbalances to address: ${muscleBalance.imbalances.joinToString { "${it.affectedZones.joinToString { z -> z.displayName }}: ${it.description}" }}")
                        }
                        appendLine()
                    }
                    
                    // Only include exercises relevant to the focus area to keep prompt small
                    val focusLower = focus.lowercase()
                    val relevantExercises = availableExercises.filter { ex ->
                        val group = ex.muscleGroup.lowercase()
                        when {
                            focusLower.contains("full body") -> true
                            focusLower.contains("push") -> group in listOf("chest", "shoulders", "triceps")
                            focusLower.contains("pull") -> group in listOf("back", "biceps", "forearms")
                            focusLower.contains("leg") || focusLower.contains("lower") -> group in listOf("quadriceps", "hamstrings", "glutes", "calves", "legs")
                            focusLower.contains("upper") -> group in listOf("chest", "back", "shoulders", "biceps", "triceps")
                            focusLower.contains("core") || focusLower.contains("abs") -> group in listOf("core", "abs", "abdominals")
                            focusLower.contains("arm") -> group in listOf("biceps", "triceps", "forearms")
                            focusLower.contains("chest") -> group in listOf("chest", "triceps")
                            focusLower.contains("back") -> group in listOf("back", "biceps")
                            focusLower.contains("shoulder") -> group in listOf("shoulders", "triceps")
                            else -> true
                        }
                    }
                    appendLine("Available exercises (use ONLY these exact names):")
                    relevantExercises.groupBy { it.muscleGroup }.forEach { (muscle, exercises) ->
                        appendLine("$muscle: ${exercises.joinToString { it.name }}")
                    }
                    appendLine()
                    appendLine("Respond in this exact JSON format:")
                    appendLine("""
{
  "name": "Template Name",
  "description": "Brief description of the workout",
  "exercises": [
    {
      "exerciseName": "Exact exercise name from list above",
      "sets": 3,
      "repsMin": 8,
      "repsMax": 12,
      "restSeconds": 90
    }
  ]
}
                    """.trimIndent())
                    appendLine()
                    appendLine("Guidelines:")
                    appendLine("- Select 4-8 exercises appropriate for the focus area")
                    appendLine("- Use compound movements first, then isolation")
                    appendLine("- Vary rep ranges based on exercise type (lower for compounds, higher for isolation)")
                    appendLine("- ONLY use exercise names that exactly match the available exercises list")
                    
                    if (bodyScan != null) {
                        appendLine("- PRIORITIZE exercises that target the user's focus zones and weak areas")
                        appendLine("- Include corrective exercises if posture issues were identified")
                        appendLine("- Adjust rep ranges based on user's goal (higher reps for fat loss, lower for strength)")
                    }
                }
                
                val response = generativeModel.generateContent(prompt)
                val responseText = response.text ?: ""
                
                parseAndApplyGeneratedTemplate(responseText, availableExercises)
                
            } catch (e: Exception) {
                android.util.Log.e("CreateTemplate", "AI generation failed", e)
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        showAIDialog = false,
                        generationError = "Generation failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun parseAndApplyGeneratedTemplate(responseText: String, availableExercises: List<Exercise>) {
        try {
            val jsonString = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            val json = JSONObject(jsonString)
            val name = json.getString("name")
            val description = json.optString("description", "")
            val exercisesArray = json.getJSONArray("exercises")
            
            val templateExercises = mutableListOf<TemplateExercise>()
            
            for (i in 0 until exercisesArray.length()) {
                val exerciseJson = exercisesArray.getJSONObject(i)
                val exerciseName = exerciseJson.getString("exerciseName")
                
                // Find matching exercise in database (case-insensitive, with fuzzy matching)
                var matchingExercise = availableExercises.find { 
                    it.name.equals(exerciseName, ignoreCase = true)
                }
                
                // Try fuzzy matching if exact match fails
                if (matchingExercise == null) {
                    matchingExercise = availableExercises.find {
                        it.name.contains(exerciseName, ignoreCase = true) ||
                        exerciseName.contains(it.name, ignoreCase = true)
                    }
                }
                
                if (matchingExercise != null) {
                    android.util.Log.d("CreateTemplate", "AI exercise '$exerciseName' matched to DB exercise '${matchingExercise.name}' (id=${matchingExercise.id})")
                    templateExercises.add(
                        TemplateExercise(
                            id = UUID.randomUUID().toString(),
                            exerciseId = matchingExercise.id,
                            exerciseName = matchingExercise.name,
                            sets = exerciseJson.optInt("sets", 3),
                            targetRepsMin = exerciseJson.optInt("repsMin", 8),
                            targetRepsMax = exerciseJson.optInt("repsMax", 12),
                            restSeconds = exerciseJson.optInt("restSeconds", 90),
                            orderIndex = templateExercises.size
                        )
                    )
                } else {
                    android.util.Log.w("CreateTemplate", "AI exercise '$exerciseName' NOT FOUND in database - skipping")
                }
            }
            
            if (templateExercises.isEmpty()) {
                _uiState.update { 
                    it.copy(
                        isGenerating = false,
                        generationError = "Could not match any exercises. Try again."
                    )
                }
                return
            }
            
            _uiState.update { 
                it.copy(
                    name = name,
                    description = description,
                    exercises = templateExercises,
                    isGenerating = false,
                    showAIDialog = false
                ) 
            }
            
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(
                    isGenerating = false,
                    generationError = "Failed to parse response: ${e.message}"
                )
            }
        }
    }
    
    fun showAIDialog() {
        _uiState.update { it.copy(showAIDialog = true) }
    }
    
    fun hideAIDialog() {
        _uiState.update { it.copy(showAIDialog = false, generationError = null) }
    }
    
    fun clearGenerationError() {
        _uiState.update { it.copy(generationError = null) }
    }
}

data class CreateTemplateUiState(
    val name: String = "",
    val description: String = "",
    val exercises: List<TemplateExercise> = emptyList(),
    val isSaved: Boolean = false,
    val isGenerating: Boolean = false,
    val showAIDialog: Boolean = false,
    val generationError: String? = null,
    val hasBodyScan: Boolean = false
) {
    val isValid: Boolean get() = name.isNotBlank() && exercises.isNotEmpty()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTemplateScreen(
    selectedExerciseId: Long? = null,
    onExerciseAdded: () -> Unit = {},
    onAddExercise: () -> Unit,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: CreateTemplateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(selectedExerciseId) {
        selectedExerciseId?.let { exerciseId ->
            viewModel.addExerciseById(exerciseId)
            onExerciseAdded()
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Template") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveTemplate() },
                        enabled = uiState.isValid
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExercise,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Exercise")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Template Name") },
                    placeholder = { Text("e.g., Push Day, Leg Day") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("e.g., Chest, shoulders, and triceps") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
            
            // AI Generation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Template Builder",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Let AI create a workout template for you based on duration and focus area.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (uiState.hasBodyScan) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Body scan data will personalize your workout",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = { viewModel.showAIDialog() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isGenerating
                        ) {
                            if (uiState.isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generating...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate with AI")
                            }
                        }
                        
                        uiState.generationError?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Exercises",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.exercises.isNotEmpty()) {
                        Text(
                            text = "${uiState.exercises.size} exercises",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiState.exercises.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No exercises yet",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Tap + to add exercises",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(uiState.exercises) { index, exercise ->
                    TemplateExerciseCard(
                        exercise = exercise,
                        index = index,
                        totalCount = uiState.exercises.size,
                        onSetsChange = { viewModel.updateExerciseSets(index, it) },
                        onRepsChange = { min, max -> viewModel.updateExerciseReps(index, min, max) },
                        onRestChange = { viewModel.updateExerciseRest(index, it) },
                        onMoveUp = { viewModel.moveExerciseUp(index) },
                        onMoveDown = { viewModel.moveExerciseDown(index) },
                        onRemove = { viewModel.removeExercise(index) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
    
    // AI Generation Dialog
    if (uiState.showAIDialog) {
        AIGenerationDialog(
            isGenerating = uiState.isGenerating,
            onDismiss = { viewModel.hideAIDialog() },
            onGenerate = { duration, focus ->
                viewModel.generateWithAI(duration, focus)
            }
        )
    }
}

@Composable
fun TemplateExerciseCard(
    exercise: TemplateExercise,
    index: Int,
    totalCount: Int,
    onSetsChange: (Int) -> Unit,
    onRepsChange: (Int, Int) -> Unit,
    onRestChange: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${exercise.sets} sets × ${exercise.targetRepsDisplay} reps • ${exercise.restSeconds}s rest",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move up",
                            tint = if (index > 0) MaterialTheme.colorScheme.onSurface 
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move down",
                            tint = if (index < totalCount - 1) MaterialTheme.colorScheme.onSurface 
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onRemove,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
        }
    }

    if (showEditDialog) {
        EditExerciseDialog(
            exercise = exercise,
            onDismiss = { showEditDialog = false },
            onSave = { sets, minReps, maxReps, rest ->
                onSetsChange(sets)
                onRepsChange(minReps, maxReps)
                onRestChange(rest)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun EditExerciseDialog(
    exercise: TemplateExercise,
    onDismiss: () -> Unit,
    onSave: (sets: Int, minReps: Int, maxReps: Int, rest: Int) -> Unit
) {
    var sets by remember { mutableStateOf(exercise.sets.toString()) }
    var minReps by remember { mutableStateOf(exercise.targetRepsMin.toString()) }
    var maxReps by remember { mutableStateOf(exercise.targetRepsMax.toString()) }
    var rest by remember { mutableStateOf(exercise.restSeconds.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(exercise.exerciseName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it.filter { c -> c.isDigit() } },
                    label = { Text("Sets") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minReps,
                        onValueChange = { minReps = it.filter { c -> c.isDigit() } },
                        label = { Text("Min Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxReps,
                        onValueChange = { maxReps = it.filter { c -> c.isDigit() } },
                        label = { Text("Max Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = rest,
                    onValueChange = { rest = it.filter { c -> c.isDigit() } },
                    label = { Text("Rest (seconds)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        sets.toIntOrNull() ?: exercise.sets,
                        minReps.toIntOrNull() ?: exercise.targetRepsMin,
                        maxReps.toIntOrNull() ?: exercise.targetRepsMax,
                        rest.toIntOrNull() ?: exercise.restSeconds
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AIGenerationDialog(
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (duration: Int, focus: String) -> Unit
) {
    var selectedDuration by remember { mutableStateOf(45) }
    var selectedFocus by remember { mutableStateOf("") }
    
    val focusOptions = listOf(
        "Push (Chest, Shoulders, Triceps)",
        "Pull (Back, Biceps)",
        "Legs (Quads, Hamstrings, Glutes)",
        "Upper Body",
        "Lower Body",
        "Full Body",
        "Core & Abs",
        "Arms (Biceps & Triceps)",
        "Chest Focus",
        "Back Focus",
        "Shoulder Focus"
    )
    
    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Template Builder")
            }
        },
        text = {
            Column {
                Text(
                    text = "Choose your workout preferences and AI will create a template for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Duration Selection
                Text(
                    text = "Workout Duration",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30, 45, 60, 90).forEach { duration ->
                        FilterChip(
                            selected = selectedDuration == duration,
                            onClick = { selectedDuration = duration },
                            label = { Text("${duration}min") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Focus Selection
                Text(
                    text = "Focus Area",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedFocus,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select focus area") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        focusOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedFocus = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                if (isGenerating) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Creating your workout...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(selectedDuration, selectedFocus) },
                enabled = selectedFocus.isNotBlank() && !isGenerating
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isGenerating
            ) {
                Text("Cancel")
            }
        }
    )
}
