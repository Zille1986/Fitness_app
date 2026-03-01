package com.runtracker.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

private val WearColors = Colors(
    primary = Color(0xFF1E88E5),
    primaryVariant = Color(0xFF1565C0),
    secondary = Color(0xFF26A69A),
    secondaryVariant = Color(0xFF00897B),
    error = Color(0xFFE57373),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onError = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0B0)
)

@Composable
fun WearGoSteadyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = WearColors,
        content = content
    )
}
