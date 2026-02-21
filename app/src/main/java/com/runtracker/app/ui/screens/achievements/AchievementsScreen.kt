package com.runtracker.app.ui.screens.achievements

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtracker.app.achievements.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementManager: AchievementManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        loadAchievements()
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            achievementManager.checkAndUnlockBadges()
            
            val unlocked = achievementManager.unlockedBadges.value
            val all = AllBadges.badges
            val locked = all.filter { badge -> unlocked.none { it.id == badge.id } }
            
            _uiState.value = AchievementsUiState(
                unlockedBadges = unlocked.map { badge ->
                    BadgeWithStatus(
                        badge = badge,
                        isUnlocked = true,
                        unlockedDate = achievementManager.getUnlockDate(badge.id)
                    )
                },
                lockedBadges = locked.map { badge ->
                    BadgeWithStatus(
                        badge = badge,
                        isUnlocked = false,
                        unlockedDate = null
                    )
                },
                totalBadges = all.size,
                unlockedCount = unlocked.size
            )
        }
    }

    fun refresh() {
        loadAchievements()
    }
}

data class AchievementsUiState(
    val unlockedBadges: List<BadgeWithStatus> = emptyList(),
    val lockedBadges: List<BadgeWithStatus> = emptyList(),
    val totalBadges: Int = 0,
    val unlockedCount: Int = 0
)

data class BadgeWithStatus(
    val badge: Badge,
    val isUnlocked: Boolean,
    val unlockedDate: Long?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onBack: () -> Unit,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress header
            item {
                ProgressCard(
                    unlockedCount = uiState.unlockedCount,
                    totalBadges = uiState.totalBadges
                )
            }

            // Unlocked badges
            if (uiState.unlockedBadges.isNotEmpty()) {
                item {
                    Text(
                        text = "UNLOCKED",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }

                items(uiState.unlockedBadges) { badgeStatus ->
                    BadgeCard(badgeStatus = badgeStatus)
                }
            }

            // Locked badges
            if (uiState.lockedBadges.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "LOCKED",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }

                items(uiState.lockedBadges) { badgeStatus ->
                    BadgeCard(badgeStatus = badgeStatus)
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(
    unlockedCount: Int,
    totalBadges: Int
) {
    val progress = if (totalBadges > 0) unlockedCount.toFloat() / totalBadges else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🏆",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$unlockedCount / $totalBadges",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Badges Unlocked",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun BadgeCard(badgeStatus: BadgeWithStatus) {
    val badge = badgeStatus.badge
    val isUnlocked = badgeStatus.isUnlocked

    val rarityColor = when (badge.rarity) {
        BadgeRarity.COMMON -> Color(0xFF9E9E9E)
        BadgeRarity.UNCOMMON -> Color(0xFF4CAF50)
        BadgeRarity.RARE -> Color(0xFF2196F3)
        BadgeRarity.EPIC -> Color(0xFF9C27B0)
        BadgeRarity.LEGENDARY -> Color(0xFFFF9800)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isUnlocked) 1f else 0.6f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (isUnlocked) 0.8f else 0.4f
            )
        ),
        border = if (isUnlocked) {
            androidx.compose.foundation.BorderStroke(2.dp, rarityColor.copy(alpha = 0.5f))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (isUnlocked) rarityColor.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    Text(
                        text = badge.icon,
                        style = MaterialTheme.typography.headlineMedium
                    )
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = badge.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    RarityChip(rarity = badge.rarity, color = rarityColor)
                }
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isUnlocked && badgeStatus.unlockedDate != null) {
                    Text(
                        text = "Unlocked ${formatDate(badgeStatus.unlockedDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = rarityColor
                    )
                }
            }
        }
    }
}

@Composable
private fun RarityChip(rarity: BadgeRarity, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = rarity.name,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
