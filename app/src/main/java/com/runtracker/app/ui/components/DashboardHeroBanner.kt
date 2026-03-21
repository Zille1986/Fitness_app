package com.runtracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Sport-specific hero image URLs.
 * Replace with your own hosted images or drawable resources for production.
 */
object SportImages {
    const val RUNNING = "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?w=800&q=80"
    const val GYM = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800&q=80"
    const val SWIMMING = "https://images.unsplash.com/photo-1530549387789-4c1017266635?w=800&q=80"
    const val CYCLING = "https://images.unsplash.com/photo-1541625602330-2277a4c46182?w=800&q=80"
}

/**
 * Editorial hero banner for the top of each sport dashboard tab.
 * Shows a full-width photo with gradient overlay and sport title.
 */
@Composable
fun DashboardHeroBanner(
    title: String,
    subtitle: String,
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Background photo
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // Text content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
