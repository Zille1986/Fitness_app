package com.runtracker.app.ui.screens.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runtracker.shared.data.model.MuscleGroup

// ── Simplified muscle groups for display ───────────────────

enum class DisplayMuscleGroup(val label: String, val sourceGroups: List<MuscleGroup>) {
    CHEST("Chest", listOf(MuscleGroup.CHEST)),
    BACK("Back", listOf(MuscleGroup.BACK, MuscleGroup.LATS)),
    SHOULDERS("Shoulders", listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRAPS)),
    BICEPS("Biceps", listOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS)),
    TRICEPS("Triceps", listOf(MuscleGroup.TRICEPS)),
    CORE("Core", listOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES, MuscleGroup.LOWER_BACK)),
    QUADS("Quads", listOf(MuscleGroup.QUADS, MuscleGroup.HIP_FLEXORS)),
    HAMSTRINGS("Hamstrings", listOf(MuscleGroup.HAMSTRINGS)),
    GLUTES("Glutes", listOf(MuscleGroup.GLUTES)),
    CALVES("Calves", listOf(MuscleGroup.CALVES));

    fun totalSets(volumeMap: Map<MuscleGroup, Int>): Int {
        return sourceGroups.sumOf { volumeMap[it] ?: 0 }
    }
}

// ── Heat intensity ──────────────────────────────────────────

enum class HeatLevel { NONE, LOW, MEDIUM, HIGH }

private fun heatLevel(sets: Int): HeatLevel = when {
    sets == 0 -> HeatLevel.NONE
    sets < 4 -> HeatLevel.LOW
    sets < 9 -> HeatLevel.MEDIUM
    else -> HeatLevel.HIGH
}

private fun heatColor(level: HeatLevel): Color = when (level) {
    HeatLevel.NONE -> Color(0xFF2D2D3A)
    HeatLevel.LOW -> Color(0xFF4A6741)
    HeatLevel.MEDIUM -> Color(0xFF66BB6A)
    HeatLevel.HIGH -> Color(0xFF00E676)
}

private fun heatTextColor(level: HeatLevel): Color = when (level) {
    HeatLevel.NONE -> Color(0xFF6B6B80)
    HeatLevel.LOW -> Color(0xFFB0BEC5)
    HeatLevel.MEDIUM -> Color.White
    HeatLevel.HIGH -> Color.White
}

// ── Composable ──────────────────────────────────────────────

@Composable
fun MuscleGroupHeatmap(
    muscleGroupVolume: Map<MuscleGroup, Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Muscle Heatmap",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Sets per muscle group this week",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Upper body row 1: Chest, Back, Shoulders
            HeatmapRow(
                groups = listOf(
                    DisplayMuscleGroup.CHEST,
                    DisplayMuscleGroup.BACK,
                    DisplayMuscleGroup.SHOULDERS
                ),
                volumeMap = muscleGroupVolume
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Upper body row 2: Biceps, Triceps, Core
            HeatmapRow(
                groups = listOf(
                    DisplayMuscleGroup.BICEPS,
                    DisplayMuscleGroup.TRICEPS,
                    DisplayMuscleGroup.CORE
                ),
                volumeMap = muscleGroupVolume
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Lower body row: Quads, Hamstrings, Glutes, Calves
            HeatmapRow(
                groups = listOf(
                    DisplayMuscleGroup.QUADS,
                    DisplayMuscleGroup.HAMSTRINGS,
                    DisplayMuscleGroup.GLUTES,
                    DisplayMuscleGroup.CALVES
                ),
                volumeMap = muscleGroupVolume
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeatLevel.entries.forEach { level ->
                    val label = when (level) {
                        HeatLevel.NONE -> "0"
                        HeatLevel.LOW -> "1-3"
                        HeatLevel.MEDIUM -> "4-8"
                        HeatLevel.HIGH -> "9+"
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(heatColor(level))
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (level != HeatLevel.HIGH) {
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "sets",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun HeatmapRow(
    groups: List<DisplayMuscleGroup>,
    volumeMap: Map<MuscleGroup, Int>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        groups.forEach { group ->
            val sets = group.totalSets(volumeMap)
            val level = heatLevel(sets)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(heatColor(level)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = group.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = heatTextColor(level),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                    if (sets > 0) {
                        Text(
                            text = "$sets",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = heatTextColor(level),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
