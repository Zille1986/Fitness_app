package com.runtracker.app.ui.screens.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Plate math ──────────────────────────────────────────────

data class PlateConfig(
    val barbellWeight: Double = 20.0,
    val availablePlates: List<Double> = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
)

data class PlateBreakdown(
    val barbellWeight: Double,
    val platesPerSide: List<Pair<Double, Int>>,
    val totalWeight: Double,
    val remainder: Double
) {
    val isExact: Boolean get() = remainder == 0.0
    val weightPerSide: Double get() = platesPerSide.sumOf { it.first * it.second }
}

fun calculatePlates(
    targetWeight: Double,
    config: PlateConfig = PlateConfig()
): PlateBreakdown {
    if (targetWeight <= config.barbellWeight) {
        return PlateBreakdown(
            barbellWeight = config.barbellWeight,
            platesPerSide = emptyList(),
            totalWeight = config.barbellWeight,
            remainder = 0.0
        )
    }

    val weightPerSide = (targetWeight - config.barbellWeight) / 2.0
    var remaining = weightPerSide
    val platesPerSide = mutableListOf<Pair<Double, Int>>()

    for (plateWeight in config.availablePlates) {
        if (remaining >= plateWeight) {
            val count = (remaining / plateWeight).toInt()
            platesPerSide.add(plateWeight to count)
            remaining -= count * plateWeight
        }
    }

    // Round remainder to avoid floating point issues
    val roundedRemainder = (remaining * 100).toLong() / 100.0

    return PlateBreakdown(
        barbellWeight = config.barbellWeight,
        platesPerSide = platesPerSide,
        totalWeight = targetWeight - roundedRemainder * 2,
        remainder = roundedRemainder * 2
    )
}

// ── Plate colors ────────────────────────────────────────────

private fun plateColor(weight: Double): Color = when (weight) {
    25.0 -> Color(0xFFE53935)  // Red
    20.0 -> Color(0xFF1E88E5)  // Blue
    15.0 -> Color(0xFFFFB300)  // Yellow
    10.0 -> Color(0xFF43A047)  // Green
    5.0 -> Color(0xFF8E24AA)   // Purple
    2.5 -> Color(0xFF546E7A)   // Dark gray
    1.25 -> Color(0xFFBDBDBD)  // Light gray
    else -> Color(0xFF78909C)
}

private fun plateHeight(weight: Double): Dp = when {
    weight >= 25.0 -> 60.dp
    weight >= 20.0 -> 56.dp
    weight >= 15.0 -> 52.dp
    weight >= 10.0 -> 46.dp
    weight >= 5.0 -> 40.dp
    weight >= 2.5 -> 34.dp
    else -> 28.dp
}

private fun plateWidth(weight: Double): Dp = when {
    weight >= 20.0 -> 16.dp
    weight >= 10.0 -> 13.dp
    weight >= 5.0 -> 10.dp
    else -> 8.dp
}

// ── Composables ─────────────────────────────────────────────

@Composable
fun PlateCalculatorDialog(
    initialWeight: Double,
    onDismiss: () -> Unit,
    config: PlateConfig = PlateConfig()
) {
    var weightText by remember { mutableStateOf(
        if (initialWeight > 0) {
            if (initialWeight == initialWeight.toLong().toDouble()) initialWeight.toLong().toString()
            else initialWeight.toString()
        } else ""
    ) }
    val targetWeight = weightText.toDoubleOrNull() ?: 0.0
    val breakdown = remember(targetWeight) { calculatePlates(targetWeight, config) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Plate Calculator")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Weight input
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Target Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (targetWeight > 0) {
                    // Barbell info
                    Text(
                        text = "Barbell: ${config.barbellWeight.toLong()}kg",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (targetWeight <= config.barbellWeight) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Just the bar!",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Visual plate representation
                        PlateVisual(breakdown = breakdown)

                        Spacer(modifier = Modifier.height(12.dp))

                        // Plate list per side
                        Text(
                            text = "Per side:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        breakdown.platesPerSide.forEach { (weight, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(plateColor(weight))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatPlateWeight(weight),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    text = "× $count",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!breakdown.isExact) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFFFF9800).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Closest: ${formatPlateWeight(breakdown.totalWeight)} " +
                                            "(${formatPlateWeight(breakdown.remainder)} off)",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun PlateVisual(
    breakdown: PlateBreakdown,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visual bar + plates
            Row(
                modifier = Modifier.height(70.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Left plates (reversed order - biggest closest to bar)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    breakdown.platesPerSide.reversed().forEach { (weight, count) ->
                        repeat(count) {
                            Box(
                                modifier = Modifier
                                    .height(plateHeight(weight))
                                    .width(plateWidth(weight))
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(plateColor(weight))
                            )
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                    }
                }

                // Bar
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .width(60.dp)
                        .background(
                            Color(0xFF9E9E9E),
                            RoundedCornerShape(2.dp)
                        )
                )

                // Right plates
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    breakdown.platesPerSide.forEach { (weight, count) ->
                        repeat(count) {
                            Spacer(modifier = Modifier.width(1.dp))
                            Box(
                                modifier = Modifier
                                    .height(plateHeight(weight))
                                    .width(plateWidth(weight))
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(plateColor(weight))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${formatPlateWeight(breakdown.totalWeight)} total",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatPlateWeight(weight: Double): String {
    return if (weight == weight.toLong().toDouble()) {
        "${weight.toLong()}kg"
    } else {
        "${"%.1f".format(weight)}kg"
    }
}
