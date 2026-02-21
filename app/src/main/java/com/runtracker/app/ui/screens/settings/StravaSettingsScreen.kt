package com.runtracker.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
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
import com.runtracker.app.strava.StravaService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StravaSettingsViewModel @Inject constructor(
    private val stravaService: StravaService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StravaUiState())
    val uiState: StateFlow<StravaUiState> = _uiState.asStateFlow()
    
    init {
        checkConnection()
    }
    
    private fun checkConnection() {
        _uiState.value = StravaUiState(
            isConnected = stravaService.isConnected,
            athleteName = stravaService.athleteName
        )
    }
    
    fun getAuthUrl(): String = stravaService.getAuthUrl()
    
    fun handleAuthCode(code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val success = stravaService.handleAuthCallback(code)
            _uiState.value = StravaUiState(
                isConnected = success,
                athleteName = stravaService.athleteName,
                message = if (success) "Connected to Strava!" else "Connection failed"
            )
        }
    }
    
    fun disconnect() {
        stravaService.disconnect()
        _uiState.value = StravaUiState(
            isConnected = false,
            message = "Disconnected from Strava"
        )
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class StravaUiState(
    val isConnected: Boolean = false,
    val athleteName: String? = null,
    val isLoading: Boolean = false,
    val message: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StravaSettingsScreen(
    onBack: () -> Unit,
    viewModel: StravaSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Strava") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Strava logo placeholder
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFC4C02) // Strava orange
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "STRAVA",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    if (uiState.isConnected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Connected",
                                color = Color.White
                            )
                        }
                        uiState.athleteName?.let { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.isConnected) {
                // Connected state
                Text(
                    text = "Your runs will automatically sync to Strava when completed.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                OutlinedButton(
                    onClick = { viewModel.disconnect() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.LinkOff, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect from Strava")
                }
            } else {
                // Not connected state
                Text(
                    text = "Connect your Strava account to automatically upload your runs.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
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
    }
}
