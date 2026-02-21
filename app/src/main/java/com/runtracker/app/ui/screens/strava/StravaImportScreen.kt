package com.runtracker.app.ui.screens.strava

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.app.strava.StravaActivity
import com.runtracker.app.strava.StravaService
import com.runtracker.shared.data.model.Run
import com.runtracker.shared.data.repository.RunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StravaImportViewModel @Inject constructor(
    private val stravaService: StravaService,
    private val runRepository: RunRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StravaImportUiState())
    val uiState: StateFlow<StravaImportUiState> = _uiState.asStateFlow()

    init {
        checkConnection()
    }

    private fun checkConnection() {
        _uiState.update {
            it.copy(
                isConnected = stravaService.isConnected,
                athleteName = stravaService.athleteName,
                isLoading = false
            )
        }
        
        if (stravaService.isConnected) {
            loadStravaActivities()
        }
    }

    fun getAuthUrl(): String = stravaService.getAuthUrl()

    fun handleAuthCode(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = stravaService.handleAuthCallback(code)
            _uiState.update { 
                it.copy(
                    isConnected = success,
                    athleteName = stravaService.athleteName,
                    isLoading = false,
                    message = if (success) "Connected to Strava!" else "Failed to connect"
                )
            }
            if (success) {
                loadStravaActivities()
            }
        }
    }

    fun loadStravaActivities() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingActivities = true) }
            
            stravaService.getRunActivities(perPage = 50).fold(
                onSuccess = { activities ->
                    // Filter out already imported activities
                    val existingStravaIds = runRepository.getAllRunsOnce()
                        .mapNotNull { it.stravaId }
                        .toSet()
                    
                    val newActivities = activities.filter { 
                        it.id.toString() !in existingStravaIds 
                    }
                    
                    _uiState.update { 
                        it.copy(
                            stravaActivities = activities,
                            newActivities = newActivities,
                            isLoadingActivities = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoadingActivities = false,
                            message = "Failed to load activities: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun importSelectedActivities(activities: List<StravaActivity>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importProgress = 0f) }
            
            var imported = 0
            val total = activities.size
            
            activities.forEachIndexed { index, activity ->
                try {
                    val run = stravaService.convertStravaActivityToRun(activity)
                    runRepository.insertRun(run)
                    imported++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                _uiState.update { it.copy(importProgress = (index + 1).toFloat() / total) }
            }
            
            _uiState.update { 
                it.copy(
                    isImporting = false,
                    importProgress = 1f,
                    message = "Imported $imported of $total activities"
                )
            }
            
            // Refresh the list
            loadStravaActivities()
        }
    }

    fun importAllNewActivities() {
        importSelectedActivities(_uiState.value.newActivities)
    }

    fun disconnect() {
        stravaService.disconnect()
        _uiState.update { 
            it.copy(
                isConnected = false,
                athleteName = null,
                stravaActivities = emptyList(),
                newActivities = emptyList(),
                message = "Disconnected from Strava"
            )
        }
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

data class StravaImportUiState(
    val isConnected: Boolean = false,
    val athleteName: String? = null,
    val isLoading: Boolean = true,
    val isLoadingActivities: Boolean = false,
    val isImporting: Boolean = false,
    val importProgress: Float = 0f,
    val stravaActivities: List<StravaActivity> = emptyList(),
    val newActivities: List<StravaActivity> = emptyList(),
    val message: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StravaImportScreen(
    onBack: () -> Unit,
    viewModel: StravaImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Strava Import") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Strava Logo/Icon
            Icon(
                imageVector = Icons.Default.DirectionsRun,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFFFC4C02)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Strava",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (!uiState.isConnected) {
                // Not connected state
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Connect your Strava account to import your running activities.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.getAuthUrl()))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFC4C02)
                            )
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect with Strava")
                        }
                    }
                }
            } else {
                // Connected state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFC4C02).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Connected to Strava",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            uiState.athleteName?.let { name ->
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Activities summary
                if (uiState.isLoadingActivities) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Loading activities from Strava...")
                        }
                    }
                } else {
                    // Import section
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Import Activities",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { viewModel.loadStravaActivities() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${uiState.stravaActivities.size}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFC4C02)
                                    )
                                    Text(
                                        text = "Total Runs",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${uiState.newActivities.size}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "New to Import",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (uiState.isImporting) {
                                LinearProgressIndicator(
                                    progress = uiState.importProgress.coerceIn(0f, 1f),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Importing activities...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else if (uiState.newActivities.isNotEmpty()) {
                                Button(
                                    onClick = { viewModel.importAllNewActivities() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFC4C02)
                                    )
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Import ${uiState.newActivities.size} New Activities")
                                }
                            } else {
                                Text(
                                    text = "All activities already imported!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Disconnect button
                OutlinedButton(
                    onClick = { viewModel.disconnect() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.LinkOff, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect Strava")
                }
            }
            
            // Message snackbar
            uiState.message?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
