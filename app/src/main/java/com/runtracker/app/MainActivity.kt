package com.runtracker.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.runtracker.app.assistant.AssistantChatDialog
import com.runtracker.app.assistant.AssistantOverlay
import com.runtracker.app.assistant.AssistantViewModel
import com.runtracker.app.assistant.MinimizedAssistantButton
import com.runtracker.app.notifications.AppNotificationManager
import com.runtracker.app.strava.StravaService
import com.runtracker.app.ui.navigation.RunTrackerNavGraph
import com.runtracker.app.ui.navigation.Screen
import com.runtracker.app.ui.theme.RunTrackerTheme
import com.runtracker.shared.data.repository.UserRepository
import com.runtracker.app.widget.WidgetDataUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var notificationManager: AppNotificationManager
    
    @Inject
    lateinit var stravaService: StravaService
    
    @Inject
    lateinit var widgetDataUpdater: WidgetDataUpdater
    
    private var stravaAuthCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channels
        notificationManager.createNotificationChannels()
        
        // Update widget data (including steps from Health Connect)
        lifecycleScope.launch {
            widgetDataUpdater.updateWidgetData()
        }
        
        // Handle Strava OAuth callback
        handleStravaCallback(intent)

        setContent {
            var startDestination by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                val profile = userRepository.getProfile().first()
                startDestination = if (profile?.isOnboardingComplete == true) {
                    Screen.Main.route
                } else {
                    Screen.Onboarding.route
                }
                
                // Process Strava auth code if present
                stravaAuthCode?.let { code ->
                    stravaService.handleAuthCallback(code)
                    stravaAuthCode = null
                }
            }

            RunTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    startDestination?.let { destination ->
                        val navController = rememberNavController()
                        val assistantViewModel: AssistantViewModel = hiltViewModel()
                        val assistantState by assistantViewModel.uiState.collectAsState()
                        
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Main app content
                            RunTrackerNavGraph(
                                navController = navController,
                                startDestination = destination
                            )
                            
                            // Assistant overlay (floating character with speech bubble)
                            AssistantOverlay(
                                uiState = assistantState,
                                onDismiss = { assistantViewModel.minimizeAssistant() },
                                onMinimize = { assistantViewModel.minimizeAssistant() },
                                onExpand = { assistantViewModel.expandChat() },
                                onQuickReply = { reply -> assistantViewModel.askQuestion(reply) }
                            )
                            
                            // Minimized assistant button (bottom-right, above nav bar)
                            if (assistantState.isMinimized || !assistantState.isVisible) {
                                MinimizedAssistantButton(
                                    onClick = { assistantViewModel.showAssistant() },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 16.dp, bottom = 80.dp)
                                )
                            }
                            
                            // Full chat dialog
                            if (assistantState.isExpanded) {
                                AssistantChatDialog(
                                    uiState = assistantState,
                                    onDismiss = { assistantViewModel.collapseChat() },
                                    onSendMessage = { message -> assistantViewModel.askQuestion(message) },
                                    onQuickReply = { reply -> assistantViewModel.askQuestion(reply) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleStravaCallback(intent)
    }
    
    private fun handleStravaCallback(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "http" && uri.host == "localhost" && uri.path?.startsWith("/callback") == true) {
                uri.getQueryParameter("code")?.let { code ->
                    stravaAuthCode = code
                    lifecycleScope.launch {
                        stravaService.handleAuthCallback(code)
                    }
                }
            }
        }
    }
}
