package com.runtracker.app.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Colors
val Primary = Color(0xFF1E88E5)
val PrimaryDark = Color(0xFF1565C0)
val PrimaryLight = Color(0xFF64B5F6)
val Secondary = Color(0xFF26A69A)
val SecondaryDark = Color(0xFF00897B)
val Accent = Color(0xFFFF6D00)
val AccentLight = Color(0xFFFF9E40)

val Background = Color(0xFFF5F5F5)
val Surface = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFF0F0F0)

val OnPrimary = Color(0xFFFFFFFF)
val OnSecondary = Color(0xFFFFFFFF)
val OnBackground = Color(0xFF1C1B1F)
val OnSurface = Color(0xFF1C1B1F)
val OnSurfaceVariant = Color(0xFF49454F)

val Error = Color(0xFFB00020)
val Success = Color(0xFF4CAF50)
val Warning = Color(0xFFFFC107)

val HeartRateZone1 = Color(0xFF90CAF9)
val HeartRateZone2 = Color(0xFF81C784)
val HeartRateZone3 = Color(0xFFFFD54F)
val HeartRateZone4 = Color(0xFFFFB74D)
val HeartRateZone5 = Color(0xFFE57373)

val ChartLine = Primary
val ChartFill = PrimaryLight.copy(alpha = 0.3f)
val ChartGrid = Color(0xFFE0E0E0)

// ── Premium Dark Theme ──────────────────────────────────────────────

// Primary accent
val DarkPrimary = Color(0xFF00E5CC)
val DarkPrimaryDark = Color(0xFF00B8A3)

// Backgrounds — deep navy-black with subtle blue undertones
val DarkBackground = Color(0xFF080B14)
val DarkSurface = Color(0xFF111827)
val DarkSurfaceVariant = Color(0xFF1A2332)

// Text
val DarkOnPrimary = Color(0xFF000000)
val DarkOnBackground = Color(0xFFE8EAED)
val DarkOnSurface = Color(0xFFE8EAED)
val DarkOnSurfaceVariant = Color(0xFFB0B8C1)

// Additional accents
val DarkSecondary = Color(0xFF7EE787)   // Green — success/progress
val DarkTertiary = Color(0xFFFFA657)    // Orange
val DarkPurple = Color(0xFF7C4DFF)      // Purple accent
val DarkPurpleLight = Color(0xFF9E7BFF)
val DarkGold = Color(0xFFFFD700)        // Gold accent
val DarkGoldMuted = Color(0xFFD4A843)

// Status
val DarkError = Color(0xFFF85149)
val DarkSuccess = Color(0xFF3FB950)
val DarkWarning = Color(0xFFD29922)

// Named activity colors (used in Home, dashboards, charts)
val ActivityRunning = Color(0xFF00E5CC)
val ActivitySwimming = Color(0xFF00B8D4)
val ActivityCycling = Color(0xFF7EE787)
val ActivityGym = Color(0xFFFFA657)

// ── Glow variants (low-alpha for radial blur / luminous effects) ────

object GlowColors {
    val Teal = Color(0xFF00E5CC).copy(alpha = 0.15f)
    val Purple = Color(0xFF7C4DFF).copy(alpha = 0.15f)
    val Orange = Color(0xFFFFA657).copy(alpha = 0.12f)
    val Green = Color(0xFF7EE787).copy(alpha = 0.12f)
    val Gold = Color(0xFFFFD700).copy(alpha = 0.10f)
}

// ── Gradient palettes ───────────────────────────────────────────────

object GradientColors {
    // Existing
    val CyanTeal = listOf(Color(0xFF00E5CC), Color(0xFF00B8A3))
    val CyanBlue = listOf(Color(0xFF00D4E5), Color(0xFF0088CC))
    val GreenTeal = listOf(Color(0xFF00E5A0), Color(0xFF00B880))
    val OrangeRed = listOf(Color(0xFFFF6B35), Color(0xFFE53935))
    val PurpleBlue = listOf(Color(0xFF7C4DFF), Color(0xFF536DFE))
    val GlassOverlay = listOf(Color(0x20FFFFFF), Color(0x08FFFFFF))

    // Premium screen background (3-stop navy gradient)
    val ScreenBackground = listOf(
        Color(0xFF080B14),
        Color(0xFF0D1120),
        Color(0xFF121828)
    )

    // New accent combos
    val PurpleTeal = listOf(Color(0xFF7C4DFF), Color(0xFF00E5CC))
    val GoldOrange = listOf(Color(0xFFFFD700), Color(0xFFFFA657))

    // Card glass overlays (multi-stop transparency)
    val CardGlass = listOf(
        Color(0x18FFFFFF),
        Color(0x08FFFFFF),
        Color(0x03FFFFFF)
    )

    // Nav bar glass
    val NavBarGlass = listOf(
        Color(0xFF111827).copy(alpha = 0.95f),
        Color(0xFF111827).copy(alpha = 0.85f)
    )
}
