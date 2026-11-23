package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// iPhone-inspired Frosted Glassmorphism Palette Tokens
object GlassTokens {
    // Light Mode Canvas & Glass
    val LightCanvasTop = Color(0xFFEEF3FB)
    val LightCanvasMid = Color(0xFFF6F8FC)
    val LightCanvasBottom = Color(0xFFE7EFF9)
    val LightGlassSurface = Color(0xCCFFFFFF)
    val LightGlassSurfaceElevated = Color(0xE6FFFFFF)
    val LightGlassBorder = Color(0xD9FFFFFF)
    val LightInkPrimary = Color(0xFF0F172A)
    val LightInkSecondary = Color(0xFF475569)

    // Dark Mode Canvas & Glass
    val DarkCanvasTop = Color(0xFF0A0F1F)
    val DarkCanvasMid = Color(0xFF101830)
    val DarkCanvasBottom = Color(0xFF0C1527)
    val DarkGlassSurface = Color(0x99192440)
    val DarkGlassSurfaceElevated = Color(0xBF1E2C4D)
    val DarkGlassBorder = Color(0x47FFFFFF)
    val DarkInkPrimary = Color(0xFFF8FAFC)
    val DarkInkSecondary = Color(0xFFB6C2D9)

    // Semantic Revision State Colors (Color + Shape + Motion)
    val DoneGreen = Color(0xFF10B981)
    val DoneGreenContainerDark = Color(0x3310B981)
    val DoneGreenContainerLight = Color(0x2410B981)

    val DueAmber = Color(0xFFF59E0B)
    val DueAmberContainerDark = Color(0x38F59E0B)
    val DueAmberContainerLight = Color(0x28F59E0B)

    val MissedRose = Color(0xFFEF4444)
    val MissedRoseContainerDark = Color(0x38EF4444)
    val MissedRoseContainerLight = Color(0x24EF4444)

    // 4 Timeless Accent Presets
    val IndigoPrimary = Color(0xFF4F46E5)
    val TealAccent = Color(0xFF14B8A6)

    val SapphirePrimary = Color(0xFF2563EB)
    val CyanAccent = Color(0xFF06B6D4)

    val DawnPrimary = Color(0xFFD97706)
    val CoralAccent = Color(0xFFF43F5E)

    val EmeraldPrimary = Color(0xFF059669)
    val MintAccent = Color(0xFF2DD4BF)

    // Subject Note Glass Tints (Subtle frosted washes)
    val NoteTintColorsLight = listOf(
        Color(0xD9FFFFFF), // Clear Crystal
        Color(0xD6E0ECFF), // Sky Frost
        Color(0xD6D8F8F0), // Mint Frost
        Color(0xD6FEF0D7), // Warm Cream Frost
        Color(0xD6FCE4EC), // Rose Quartz Frost
        Color(0xD6EDE4FF), // Lavender Frost
    )

    val NoteTintColorsDark = listOf(
        Color(0xA61A2542), // Clear Obsidian Glass
        Color(0xA6162D4C), // Deep Sky Glass
        Color(0xA6123338), // Deep Teal Glass
        Color(0xA6362B1E), // Amber Smoke Glass
        Color(0xA63A1F31), // Rose Smoke Glass
        Color(0xA629214A), // Indigo Haze Glass
    )
}
