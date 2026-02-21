package com.runtracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.runtracker.shared.data.model.EmergencyContact
import com.runtracker.shared.data.model.SafetySettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyScreen(
    settings: SafetySettings?,
    onUpdateSettings: (SafetySettings) -> Unit,
    onAddContact: (EmergencyContact) -> Unit,
    onRemoveContact: (String) -> Unit,
    onTestSos: () -> Unit,
    onTestPanicAlarm: () -> Unit,
    onTestFakeCall: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showEditSosMessageDialog by remember { mutableStateOf(false) }
    var showEditFakeCallerDialog by remember { mutableStateOf(false) }
    
    val currentSettings = settings ?: SafetySettings()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Safety Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Safety Features",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Protect yourself while exercising",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // Emergency Contacts Section
            item {
                Text(
                    "Emergency Contacts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (currentSettings.emergencyContacts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddContactDialog = true },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "No emergency contacts",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Add at least one contact to enable safety features",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add contact"
                            )
                        }
                    }
                }
            } else {
                items(currentSettings.emergencyContacts) { contact ->
                    EmergencyContactCard(
                        contact = contact,
                        onRemove = { onRemoveContact(contact.id) }
                    )
                }
            }
            
            item {
                OutlinedButton(
                    onClick = { showAddContactDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Emergency Contact")
                }
            }
            
            // SOS Settings
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "SOS Alert",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SOS Message", fontWeight = FontWeight.Medium)
                                Text(
                                    currentSettings.sosMessage.take(50) + if (currentSettings.sosMessage.length > 50) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showEditSosMessageDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Countdown before sending", fontWeight = FontWeight.Medium)
                                Text(
                                    "${currentSettings.sosCountdownSeconds} seconds to cancel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Include GPS location", fontWeight = FontWeight.Medium)
                            Switch(
                                checked = currentSettings.autoShareLocationOnSos,
                                onCheckedChange = { 
                                    onUpdateSettings(currentSettings.copy(autoShareLocationOnSos = it))
                                }
                            )
                        }
                    }
                }
            }
            
            // Panic Alarm Settings
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Panic Alarm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Enable Panic Alarm", fontWeight = FontWeight.Medium)
                                Text(
                                    "Trigger loud alarm on phone from watch",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = currentSettings.panicAlarmEnabled,
                                onCheckedChange = { 
                                    onUpdateSettings(currentSettings.copy(panicAlarmEnabled = it))
                                }
                            )
                        }
                    }
                }
            }
            
            // Fake Call Settings
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Fake Call",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Enable Fake Call", fontWeight = FontWeight.Medium)
                                Text(
                                    "Simulate incoming call to exit uncomfortable situations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = currentSettings.fakeCallEnabled,
                                onCheckedChange = { 
                                    onUpdateSettings(currentSettings.copy(fakeCallEnabled = it))
                                }
                            )
                        }
                        
                        if (currentSettings.fakeCallEnabled) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showEditFakeCallerDialog = true },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Caller Name", fontWeight = FontWeight.Medium)
                                    Text(
                                        currentSettings.fakeCallerName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Delay before call", fontWeight = FontWeight.Medium)
                                    Text(
                                        "${currentSettings.fakeCallDelay} seconds",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Check-in Timer Settings
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Check-in Timer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Enable Check-in Timer", fontWeight = FontWeight.Medium)
                                Text(
                                    "Alert contacts if you don't check in after a run",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = currentSettings.checkInEnabled,
                                onCheckedChange = { 
                                    onUpdateSettings(currentSettings.copy(checkInEnabled = it))
                                }
                            )
                        }
                        
                        if (currentSettings.checkInEnabled) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Default duration", fontWeight = FontWeight.Medium)
                                    Text(
                                        "${currentSettings.defaultCheckInMinutes} minutes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Test Features Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Test Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onTestPanicAlarm,
                        modifier = Modifier.weight(1f),
                        enabled = currentSettings.panicAlarmEnabled
                    ) {
                        Text("🚨 Alarm")
                    }
                    OutlinedButton(
                        onClick = onTestFakeCall,
                        modifier = Modifier.weight(1f),
                        enabled = currentSettings.fakeCallEnabled
                    ) {
                        Text("📞 Fake Call")
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Add Contact Dialog
    if (showAddContactDialog) {
        AddContactDialog(
            onDismiss = { showAddContactDialog = false },
            onAdd = { contact ->
                onAddContact(contact)
                showAddContactDialog = false
            }
        )
    }
    
    // Edit SOS Message Dialog
    if (showEditSosMessageDialog) {
        EditTextDialog(
            title = "SOS Message",
            currentValue = currentSettings.sosMessage,
            onDismiss = { showEditSosMessageDialog = false },
            onSave = { newMessage ->
                onUpdateSettings(currentSettings.copy(sosMessage = newMessage))
                showEditSosMessageDialog = false
            }
        )
    }
    
    // Edit Fake Caller Dialog
    if (showEditFakeCallerDialog) {
        EditTextDialog(
            title = "Fake Caller Name",
            currentValue = currentSettings.fakeCallerName,
            onDismiss = { showEditFakeCallerDialog = false },
            onSave = { newName ->
                onUpdateSettings(currentSettings.copy(fakeCallerName = newName))
                showEditFakeCallerDialog = false
            }
        )
    }
}

@Composable
fun EmergencyContactCard(
    contact: EmergencyContact,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    contact.name.first().uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, fontWeight = FontWeight.Medium)
                Text(
                    contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (contact.relationship.isNotEmpty()) {
                    Text(
                        contact.relationship,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (contact.isPrimary) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Primary contact",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onAdd: (EmergencyContact) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var isPrimary by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Emergency Contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g., Partner, Parent, Friend") }
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Primary Contact")
                    Switch(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phoneNumber.isNotBlank()) {
                        onAdd(EmergencyContact(
                            name = name.trim(),
                            phoneNumber = phoneNumber.trim(),
                            relationship = relationship.trim(),
                            isPrimary = isPrimary
                        ))
                    }
                },
                enabled = name.isNotBlank() && phoneNumber.isNotBlank()
            ) {
                Text("Add")
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
fun EditTextDialog(
    title: String,
    currentValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentValue) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(text.trim()) },
                enabled = text.isNotBlank()
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
