package com.runtracker.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Serene Light Theme (forest-green palette) ────────────────────────
val LightPrimary = Color(0xFF3F6758)
val LightPrimaryDim = Color(0xFF335B4D)
val LightPrimaryContainer = Color(0xFFC0ECDA)
val LightOnPrimary = Color(0xFFE5FFF2)
val LightOnPrimaryContainer = Color(0xFF32594B)

val LightSecondary = Color(0xFF516170)
val LightSecondaryDim = Color(0xFF455564)
val LightSecondaryContainer = Color(0xFFD4E4F6)
val LightOnSecondary = Color(0xFFF6F9FF)
val LightOnSecondaryContainer = Color(0xFF445362)

val LightTertiary = Color(0xFF2E6771)
val LightTertiaryDim = Color(0xFF205A65)
val LightTertiaryContainer = Color(0xFFB7EFFB)
val LightOnTertiary = Color(0xFFEDFBFF)
val LightOnTertiaryContainer = Color(0xFF215B65)

val LightBackground = Color(0xFFF9F9F7)
val LightSurface = Color(0xFFF9F9F7)
val LightSurfaceDim = Color(0xFFD7DBD8)
val LightSurfaceBright = Color(0xFFF9F9F7)
val LightSurfaceVariant = Color(0xFFE0E3E0)
val LightSurfaceContainer = Color(0xFFECEEEC)
val LightSurfaceContainerLow = Color(0xFFF3F4F2)
val LightSurfaceContainerHigh = Color(0xFFE6E9E6)
val LightSurfaceContainerHighest = Color(0xFFE0E3E0)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)

val LightOnBackground = Color(0xFF2F3332)
val LightOnSurface = Color(0xFF2F3332)
val LightOnSurfaceVariant = Color(0xFF5C605E)

val LightOutline = Color(0xFF777C79)
val LightOutlineVariant = Color(0xFFAFB3B0)

val LightError = Color(0xFFA83836)
val LightOnError = Color(0xFFFFF7F6)
val LightErrorContainer = Color(0xFFFA746F)
val LightOnErrorContainer = Color(0xFF6E0A12)

val LightInverseSurface = Color(0xFF0C0F0E)
val LightInverseOnSurface = Color(0xFF9C9D9B)
val LightInversePrimary = Color(0xFFCCF8E5)

val LightSuccess = Color(0xFF4CAF50)
val LightWarning = Color(0xFFFFC107)

val HeartRateZone1 = Color(0xFF90CAF9)
val HeartRateZone2 = Color(0xFF81C784)
val HeartRateZone3 = Color(0xFFFFD54F)
val HeartRateZone4 = Color(0xFFFFB74D)
val HeartRateZone5 = Color(0xFFE57373)

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

    // Premium dark screen background (3-stop navy gradient)
    val ScreenBackground = listOf(
        Color(0xFF080B14),
        Color(0xFF0D1120),
        Color(0xFF121828)
    )

    // Light screen background (subtle warm gradient)
    val ScreenBackgroundLight = listOf(
        Color(0xFFF9F9F7),
        Color(0xFFF3F4F2),
        Color(0xFFECEEEC)
    )

    // New accent combos
    val PurpleTeal = listOf(Color(0xFF7C4DFF), Color(0xFF00E5CC))
    val GoldOrange = listOf(Color(0xFFFFD700), Color(0xFFFFA657))

    // Card glass overlays (multi-stop transparency) — dark
    val CardGlass = listOf(
        Color(0x18FFFFFF),
        Color(0x08FFFFFF),
        Color(0x03FFFFFF)
    )

    // Card glass overlays — light
    val CardGlassLight = listOf(
        Color(0x0A000000),
        Color(0x05000000),
        Color(0x02000000)
    )

    // Nav bar glass — dark
    val NavBarGlass = listOf(
        Color(0xFF111827).copy(alpha = 0.95f),
        Color(0xFF111827).copy(alpha = 0.85f)
    )

    // Nav bar glass — light
    val NavBarGlassLight = listOf(
        Color(0xFFFFFFFF).copy(alpha = 0.90f),
        Color(0xFFF9F9F7).copy(alpha = 0.85f)
    )
}
