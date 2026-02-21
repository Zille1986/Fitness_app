package com.runtracker.app.ui.screens

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.app.safety.SafetyService
import com.runtracker.shared.data.model.EmergencyContact
import com.runtracker.shared.data.model.SafetySettings
import com.runtracker.shared.data.repository.SafetyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SafetyViewModel @Inject constructor(
    private val safetyRepository: SafetyRepository,
    private val safetyService: SafetyService
) : ViewModel() {
    
    private val _settings = MutableStateFlow<SafetySettings?>(null)
    val settings: StateFlow<SafetySettings?> = _settings.asStateFlow()
    
    init {
        viewModelScope.launch {
            safetyRepository.getSettings().collect { settings ->
                _settings.value = settings ?: SafetySettings()
            }
        }
    }
    
    fun updateSettings(settings: SafetySettings) {
        viewModelScope.launch {
            safetyRepository.saveSettings(settings)
        }
    }
    
    fun addContact(contact: EmergencyContact) {
        viewModelScope.launch {
            safetyRepository.addEmergencyContact(contact)
        }
    }
    
    fun removeContact(contactId: String) {
        viewModelScope.launch {
            safetyRepository.removeEmergencyContact(contactId)
        }
    }
    
    fun testSos() {
        viewModelScope.launch {
            safetyService.triggerSos()
        }
    }
    
    fun testPanicAlarm() {
        safetyService.startPanicAlarm()
    }
    
    fun testFakeCall() {
        safetyService.triggerFakeCall()
    }
}

@Composable
fun SafetyScreenWrapper(
    onNavigateBack: () -> Unit,
    viewModel: SafetyViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    
    SafetyScreen(
        settings = settings,
        onUpdateSettings = viewModel::updateSettings,
        onAddContact = viewModel::addContact,
        onRemoveContact = viewModel::removeContact,
        onTestSos = viewModel::testSos,
        onTestPanicAlarm = viewModel::testPanicAlarm,
        onTestFakeCall = viewModel::testFakeCall,
        onNavigateBack = onNavigateBack
    )
}
