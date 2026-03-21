package com.runtracker.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class ManualWorkoutType(val label: String, val distanceUnit: String) {
    RUN("Run", "km"),
    SWIM("Swim", "m"),
    BIKE("Bike", "km")
}

data class ManualWorkoutData(
    val type: ManualWorkoutType,
    val distance: Double,       // km for run/bike, meters for swim
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val notes: String
) {
    val durationMillis: Long get() = ((hours * 3600L) + (minutes * 60L) + seconds) * 1000L

    /** Distance in meters regardless of sport */
    val distanceMeters: Double get() = when (type) {
        ManualWorkoutType.RUN, ManualWorkoutType.BIKE -> distance * 1000.0
        ManualWorkoutType.SWIM -> distance
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualWorkoutDialog(
    workoutType: ManualWorkoutType,
    onDismiss: () -> Unit,
    onSave: (ManualWorkoutData) -> Unit
) {
    var distance by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var seconds by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val isValid = distance.toDoubleOrNull() != null &&
        distance.toDouble() > 0 &&
        (hours.toIntOrNull() ?: 0) + (minutes.toIntOrNull() ?: 0) + (seconds.toIntOrNull() ?: 0) > 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Log ${workoutType.label}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Distance
                OutlinedTextField(
                    value = distance,
                    onValueChange = { distance = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Distance (${workoutType.distanceUnit})") },
                    placeholder = {
                        Text(
                            when (workoutType) {
                                ManualWorkoutType.RUN -> "e.g. 5.0"
                                ManualWorkoutType.SWIM -> "e.g. 1500"
                                ManualWorkoutType.BIKE -> "e.g. 20.0"
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Duration - 3 fields in a row
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { hours = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Hrs") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Min") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = seconds,
                        onValueChange = { seconds = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Sec") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Save button
                Button(
                    onClick = {
                        onSave(
                            ManualWorkoutData(
                                type = workoutType,
                                distance = distance.toDoubleOrNull() ?: 0.0,
                                hours = hours.toIntOrNull() ?: 0,
                                minutes = minutes.toIntOrNull() ?: 0,
                                seconds = seconds.toIntOrNull() ?: 0,
                                notes = notes
                            )
                        )
                    },
                    enabled = isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Save ${workoutType.label}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
