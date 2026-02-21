package com.runtracker.app.ui.screens.progress

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class ProgressPhotosViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geminiBodyAnalyzer: com.runtracker.app.ai.GeminiBodyAnalyzer,
    private val bodyAnalysisRepository: com.runtracker.shared.data.repository.BodyAnalysisRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressPhotosUiState())
    val uiState: StateFlow<ProgressPhotosUiState> = _uiState.asStateFlow()

    private val photosDir = File(context.filesDir, "progress_photos")

    init {
        loadPhotos()
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            if (!photosDir.exists()) {
                photosDir.mkdirs()
            }

            val photos = photosDir.listFiles()
                ?.filter { it.extension in listOf("jpg", "jpeg", "png") }
                ?.sortedByDescending { it.lastModified() }
                ?.map { file ->
                    ProgressPhoto(
                        id = file.name,
                        uri = Uri.fromFile(file),
                        timestamp = file.lastModified(),
                        weight = extractWeight(file.name),
                        note = extractNote(file.name)
                    )
                } ?: emptyList()

            // Group by month
            val grouped = photos.groupBy { photo ->
                SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    .format(Date(photo.timestamp))
            }

            _uiState.value = ProgressPhotosUiState(
                photos = photos,
                groupedPhotos = grouped,
                isLoading = false
            )
        }
    }

    fun savePhoto(uri: Uri, weight: Double?, note: String?, runBodyAnalysis: Boolean = true) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isAnalyzing = runBodyAnalysis)
                
                val timestamp = System.currentTimeMillis()
                val weightStr = weight?.let { "_w${it}" } ?: ""
                val noteStr = note?.let { "_n${it.take(20).replace(" ", "_")}" } ?: ""
                val filename = "progress_${timestamp}${weightStr}${noteStr}.jpg"
                val destFile = File(photosDir, filename)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Run body analysis on the photo
                if (runBodyAnalysis) {
                    try {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(destFile.absolutePath)
                        if (bitmap != null) {
                            val result = geminiBodyAnalyzer.analyzeBodyImage(
                                bitmap, 
                                com.runtracker.shared.data.model.FitnessGoal.GENERAL_FITNESS
                            )
                            if (result is com.runtracker.app.ai.BodyAnalysisAIResult.Success) {
                                // Save analysis with photo reference
                                val scan = com.runtracker.shared.data.model.BodyScan(
                                    photoPath = destFile.absolutePath,
                                    userGoal = com.runtracker.shared.data.model.FitnessGoal.GENERAL_FITNESS,
                                    bodyType = result.bodyType,
                                    estimatedBodyFatPercentage = result.estimatedBodyFatPercentage,
                                    focusZones = result.focusZones,
                                    overallScore = result.overallScore,
                                    muscleBalance = result.muscleBalance,
                                    postureAssessment = result.postureAssessment
                                )
                                bodyAnalysisRepository.saveScan(scan)
                                _uiState.value = _uiState.value.copy(
                                    lastAnalysisResult = ProgressPhotoAnalysis(
                                        bodyFatPercentage = result.estimatedBodyFatPercentage,
                                        overallScore = result.overallScore,
                                        bodyType = result.bodyType.name
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Analysis failed, but photo is saved
                        android.util.Log.e("ProgressPhotos", "Body analysis failed: ${e.message}")
                    }
                }

                _uiState.value = _uiState.value.copy(isAnalyzing = false)
                loadPhotos()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isAnalyzing = false)
            }
        }
    }

    fun deletePhoto(photo: ProgressPhoto) {
        viewModelScope.launch {
            val file = File(photosDir, photo.id)
            if (file.exists()) {
                file.delete()
            }
            loadPhotos()
        }
    }

    private fun extractWeight(filename: String): Double? {
        val regex = "_w([0-9.]+)".toRegex()
        return regex.find(filename)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractNote(filename: String): String? {
        val regex = "_n([^.]+)".toRegex()
        return regex.find(filename)?.groupValues?.get(1)?.replace("_", " ")
    }

    fun createTempPhotoUri(): Uri {
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }
        val tempFile = File(photosDir, "temp_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ProgressPhotosUiState(
    val photos: List<ProgressPhoto> = emptyList(),
    val groupedPhotos: Map<String, List<ProgressPhoto>> = emptyMap(),
    val isLoading: Boolean = true,
    val isAnalyzing: Boolean = false,
    val error: String? = null,
    val lastAnalysisResult: ProgressPhotoAnalysis? = null
)

data class ProgressPhoto(
    val id: String,
    val uri: Uri,
    val timestamp: Long,
    val weight: Double? = null,
    val note: String? = null,
    val bodyFatPercentage: Float? = null,
    val overallScore: Int? = null
)

data class ProgressPhotoAnalysis(
    val bodyFatPercentage: Float,
    val overallScore: Int,
    val bodyType: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressPhotosScreen(
    onBack: () -> Unit,
    viewModel: ProgressPhotosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPhoto by remember { mutableStateOf<ProgressPhoto?>(null) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            pendingPhotoUri = tempPhotoUri
            showAddDialog = true
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingPhotoUri = it
            showAddDialog = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            tempPhotoUri = viewModel.createTempPhotoUri()
            tempPhotoUri?.let { cameraLauncher.launch(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress Photos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Show options: Camera or Gallery
                    showAddDialog = true
                    pendingPhotoUri = null
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Photo")
            }
        }
    ) { padding ->
        if (uiState.photos.isEmpty() && !uiState.isLoading) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Track Your Transformation",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Take progress photos to see how far you've come. Tap + to add your first photo.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Stats card
                if (uiState.photos.size >= 2) {
                    item {
                        TransformationStatsCard(photos = uiState.photos)
                    }
                }

                // Compare button
                if (uiState.photos.size >= 2) {
                    item {
                        CompareCard(
                            firstPhoto = uiState.photos.lastOrNull(),
                            latestPhoto = uiState.photos.firstOrNull()
                        )
                    }
                }

                // Photos by month
                uiState.groupedPhotos.forEach { (month, photos) ->
                    item {
                        Text(
                            text = month.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(photos) { photo ->
                                PhotoCard(
                                    photo = photo,
                                    onClick = { selectedPhoto = photo }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Dialog
    if (showAddDialog) {
        AddPhotoDialog(
            pendingUri = pendingPhotoUri,
            onDismiss = { 
                showAddDialog = false
                pendingPhotoUri = null
            },
            onTakePhoto = {
                showAddDialog = false
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onChooseFromGallery = {
                showAddDialog = false
                galleryLauncher.launch("image/*")
            },
            onSave = { weight, note ->
                pendingPhotoUri?.let { uri ->
                    viewModel.savePhoto(uri, weight, note)
                }
                showAddDialog = false
                pendingPhotoUri = null
            }
        )
    }

    // Photo detail dialog
    selectedPhoto?.let { photo ->
        PhotoDetailDialog(
            photo = photo,
            onDismiss = { selectedPhoto = null },
            onDelete = {
                viewModel.deletePhoto(photo)
                selectedPhoto = null
            }
        )
    }
}

@Composable
private fun TransformationStatsCard(photos: List<ProgressPhoto>) {
    val firstPhoto = photos.lastOrNull()
    val latestPhoto = photos.firstOrNull()
    val daysBetween = if (firstPhoto != null && latestPhoto != null) {
        ((latestPhoto.timestamp - firstPhoto.timestamp) / (1000 * 60 * 60 * 24)).toInt()
    } else 0

    val weightChange = if (firstPhoto?.weight != null && latestPhoto?.weight != null) {
        latestPhoto.weight - firstPhoto.weight
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your Journey",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "${photos.size}",
                    label = "Photos"
                )
                StatItem(
                    value = "$daysBetween",
                    label = "Days"
                )
                weightChange?.let { change ->
                    StatItem(
                        value = "${if (change >= 0) "+" else ""}${"%.1f".format(change)} kg",
                        label = "Weight",
                        color = if (change < 0) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun CompareCard(
    firstPhoto: ProgressPhoto?,
    latestPhoto: ProgressPhoto?
) {
    if (firstPhoto == null || latestPhoto == null) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Before & After",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = firstPhoto.uri,
                        contentDescription = "First photo",
                        modifier = Modifier
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("MMM d", Locale.getDefault())
                            .format(Date(firstPhoto.timestamp)),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = latestPhoto.uri,
                        contentDescription = "Latest photo",
                        modifier = Modifier
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("MMM d", Locale.getDefault())
                            .format(Date(latestPhoto.timestamp)),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoCard(
    photo: ProgressPhoto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            AsyncImage(
                model = photo.uri,
                contentDescription = "Progress photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = SimpleDateFormat("MMM d", Locale.getDefault())
                        .format(Date(photo.timestamp)),
                    style = MaterialTheme.typography.labelSmall
                )
                photo.weight?.let { weight ->
                    Text(
                        text = "${"%.1f".format(weight)} kg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPhotoDialog(
    pendingUri: Uri?,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
    onSave: (weight: Double?, note: String?) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (pendingUri == null) "Add Progress Photo" else "Save Photo")
        },
        text = {
            Column {
                if (pendingUri == null) {
                    // Source selection
                    OutlinedCard(
                        onClick = onTakePhoto,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Take Photo")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedCard(
                        onClick = onChooseFromGallery,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Choose from Gallery")
                        }
                    }
                } else {
                    // Photo details
                    AsyncImage(
                        model = pendingUri,
                        contentDescription = "Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight (kg) - optional") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note - optional") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            if (pendingUri != null) {
                TextButton(
                    onClick = {
                        onSave(weight.toDoubleOrNull(), note.ifEmpty { null })
                    }
                ) {
                    Text("Save")
                }
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
private fun PhotoDetailDialog(
    photo: ProgressPhoto,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                    .format(Date(photo.timestamp))
            )
        },
        text = {
            Column {
                AsyncImage(
                    model = photo.uri,
                    contentDescription = "Progress photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
                photo.weight?.let { weight ->
                    Text(
                        text = "Weight: ${"%.1f".format(weight)} kg",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                photo.note?.let { note ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Photo?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
