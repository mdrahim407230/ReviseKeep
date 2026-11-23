package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.prefs.AccentPreset
import com.example.data.prefs.ThemeMode

data class GlassPalette(
    val isDark: Boolean,
    val canvasTop: Color,
    val canvasMid: Color,
    val canvasBottom: Color,
    val glassSurface: Color,
    val glassElevated: Color,
    val glassBorder: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val noteTints: List<Color>,
    val doneColor: Color,
    val doneContainer: Color,
    val dueColor: Color,
    val dueContainer: Color,
    val missedColor: Color,
    val missedContainer: Color
)

val LocalGlassPalette = staticCompositionLocalOf {
    GlassPalette(
        isDark = false,
        canvasTop = GlassTokens.LightCanvasTop,
        canvasMid = GlassTokens.LightCanvasMid,
        canvasBottom = GlassTokens.LightCanvasBottom,
        glassSurface = GlassTokens.LightGlassSurface,
        glassElevated = GlassTokens.LightGlassSurfaceElevated,
        glassBorder = GlassTokens.LightGlassBorder,
        primaryAccent = GlassTokens.IndigoPrimary,
        secondaryAccent = GlassTokens.TealAccent,
        inkPrimary = GlassTokens.LightInkPrimary,
        inkSecondary = GlassTokens.LightInkSecondary,
        noteTints = GlassTokens.NoteTintColorsLight,
        doneColor = GlassTokens.DoneGreen,
        doneContainer = GlassTokens.DoneGreenContainerLight,
        dueColor = GlassTokens.DueAmber,
        dueContainer = GlassTokens.DueAmberContainerLight,
        missedColor = GlassTokens.MissedRose,
        missedContainer = GlassTokens.MissedRoseContainerLight
    )
}

private val ReviseKeepShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun ReviseKeepTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentPreset: AccentPreset = AccentPreset.MIDNIGHT_TEAL,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val (primary, secondary) = when (accentPreset) {
        AccentPreset.MIDNIGHT_TEAL -> GlassTokens.IndigoPrimary to GlassTokens.TealAccent
        AccentPreset.OCEAN_SAPPHIRE -> GlassTokens.SapphirePrimary to GlassTokens.CyanAccent
        AccentPreset.WARM_DAWN -> GlassTokens.DawnPrimary to GlassTokens.CoralAccent
        AccentPreset.SOFT_EMERALD -> GlassTokens.EmeraldPrimary to GlassTokens.MintAccent
    }

    val glassPalette = if (isDark) {
        GlassPalette(
            isDark = true,
            canvasTop = GlassTokens.DarkCanvasTop,
            canvasMid = GlassTokens.DarkCanvasMid,
            canvasBottom = GlassTokens.DarkCanvasBottom,
            glassSurface = GlassTokens.DarkGlassSurface,
            glassElevated = GlassTokens.DarkGlassSurfaceElevated,
            glassBorder = GlassTokens.DarkGlassBorder,
            primaryAccent = primary,
            secondaryAccent = secondary,
            inkPrimary = GlassTokens.DarkInkPrimary,
            inkSecondary = GlassTokens.DarkInkSecondary,
            noteTints = GlassTokens.NoteTintColorsDark,
            doneColor = GlassTokens.DoneGreen,
            doneContainer = GlassTokens.DoneGreenContainerDark,
            dueColor = GlassTokens.DueAmber,
            dueContainer = GlassTokens.DueAmberContainerDark,
            missedColor = GlassTokens.MissedRose,
            missedContainer = GlassTokens.MissedRoseContainerDark
        )
    } else {
        GlassPalette(
            isDark = false,
            canvasTop = GlassTokens.LightCanvasTop,
            canvasMid = GlassTokens.LightCanvasMid,
            canvasBottom = GlassTokens.LightCanvasBottom,
            glassSurface = GlassTokens.LightGlassSurface,
            glassElevated = GlassTokens.LightGlassSurfaceElevated,
            glassBorder = GlassTokens.LightGlassBorder,
            primaryAccent = primary,
            secondaryAccent = secondary,
            inkPrimary = GlassTokens.LightInkPrimary,
            inkSecondary = GlassTokens.LightInkSecondary,
            noteTints = GlassTokens.NoteTintColorsLight,
            doneColor = GlassTokens.DoneGreen,
            doneContainer = GlassTokens.DoneGreenContainerLight,
            dueColor = GlassTokens.DueAmber,
            dueContainer = GlassTokens.DueAmberContainerLight,
            missedColor = GlassTokens.MissedRose,
            missedContainer = GlassTokens.MissedRoseContainerLight
        )
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = secondary,
            onPrimary = Color(0xFF06131A),
            primaryContainer = primary.copy(alpha = 0.28f),
            onPrimaryContainer = GlassTokens.DarkInkPrimary,
            secondary = primary,
            onSecondary = Color.White,
            background = GlassTokens.DarkCanvasMid,
            onBackground = GlassTokens.DarkInkPrimary,
            surface = GlassTokens.DarkGlassSurfaceElevated,
            onSurface = GlassTokens.DarkInkPrimary,
            surfaceVariant = GlassTokens.DarkGlassSurface,
            onSurfaceVariant = GlassTokens.DarkInkSecondary,
            error = GlassTokens.MissedRose
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.14f),
            onPrimaryContainer = GlassTokens.LightInkPrimary,
            secondary = secondary,
            onSecondary = Color.White,
            background = GlassTokens.LightCanvasMid,
            onBackground = GlassTokens.LightInkPrimary,
            surface = GlassTokens.LightGlassSurfaceElevated,
            onSurface = GlassTokens.LightInkPrimary,
            surfaceVariant = GlassTokens.LightGlassSurface,
            onSurfaceVariant = GlassTokens.LightInkSecondary,
            error = GlassTokens.MissedRose
        )
    }

    CompositionLocalProvider(LocalGlassPalette provides glassPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildReviseKeepTypography(fontScale),
            shapes = ReviseKeepShapes,
            content = content
        )
    }
}
