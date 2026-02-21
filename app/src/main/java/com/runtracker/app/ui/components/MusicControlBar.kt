package com.runtracker.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runtracker.app.media.MusicControlManager
import com.runtracker.app.media.MusicPlaybackState

@Composable
fun MusicControlBar(
    musicControlManager: MusicControlManager,
    modifier: Modifier = Modifier,
    minimized: Boolean = false
) {
    val playbackState by musicControlManager.playbackState.collectAsState()
    val currentTrack by musicControlManager.currentTrack.collectAsState()

    LaunchedEffect(Unit) {
        musicControlManager.updatePlaybackState()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (minimized) {
            // Compact controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = { musicControlManager.previous() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = { musicControlManager.playPause() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                ) {
                    Icon(
                        if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                IconButton(
                    onClick = { musicControlManager.next() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            // Full controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Track info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack?.title ?: "Music",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        currentTrack?.artist?.let { artist ->
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Playback controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { musicControlManager.volumeDown() }) {
                        Icon(Icons.Default.VolumeDown, contentDescription = "Volume Down")
                    }
                    
                    IconButton(onClick = { musicControlManager.previous() }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                    }
                    
                    FloatingActionButton(
                        onClick = { musicControlManager.playPause() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    IconButton(onClick = { musicControlManager.next() }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next")
                    }
                    
                    IconButton(onClick = { musicControlManager.volumeUp() }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Volume Up")
                    }
                }
            }
        }
    }
}

@Composable
fun MusicControlFAB(
    onClick: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Icon(
            if (isPlaying) Icons.Default.MusicNote else Icons.Default.MusicOff,
            contentDescription = "Music Controls",
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
