package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val HindSiliguriFamily = FontFamily(
    Font(R.font.hind_siliguri, FontWeight.Normal)
)

val PlusJakartaFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal)
)

fun buildReviseKeepTypography(fontScale: Float = 1.0f): Typography {
    val s = fontScale.coerceIn(0.85f, 1.35f)
    return Typography(
        displayLarge = TextStyle(
            fontFamily = PlusJakartaFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (36 * s).sp,
            lineHeight = (44 * s).sp,
            letterSpacing = (-0.5).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = HindSiliguriFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (28 * s).sp,
            lineHeight = (36 * s).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = HindSiliguriFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (22 * s).sp,
            lineHeight = (30 * s).sp
        ),
        titleLarge = TextStyle(
            fontFamily = HindSiliguriFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (20 * s).sp,
            lineHeight = (27 * s).sp
        ),
        titleMedium = TextStyle(
            fontFamily = HindSiliguriFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (17 * s).sp,
            lineHeight = (24 * s).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = HindSiliguriFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * s).sp,
            lineHeight = (24 * s).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = HindSiliguriFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * s).sp,
            lineHeight = (20 * s).sp
        ),
        labelLarge = TextStyle(
            fontFamily = PlusJakartaFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14 * s).sp,
            lineHeight = (20 * s).sp
        ),
        labelSmall = TextStyle(
            fontFamily = HindSiliguriFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11 * s).sp,
            lineHeight = (15 * s).sp
        )
    )
}
