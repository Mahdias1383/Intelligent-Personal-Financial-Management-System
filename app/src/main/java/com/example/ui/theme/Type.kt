package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Authentic 7-weight Estedad-FD (Farsi Digits) — modern Iranian neo-grotesque fintech typeface.
 * Every weight maps to a real static .ttf with 100% native Persian letters & digits coverage.
 */
val EstedadFontFamily = FontFamily(
    Font(R.font.estedad_light, FontWeight.Light),
    Font(R.font.estedad_regular, FontWeight.Normal),
    Font(R.font.estedad_medium, FontWeight.Medium),
    Font(R.font.estedad_semibold, FontWeight.SemiBold),
    Font(R.font.estedad_bold, FontWeight.Bold),
    Font(R.font.estedad_extrabold, FontWeight.ExtraBold),
    Font(R.font.estedad_black, FontWeight.Black)
)

/** Backward-compatible alias pointing to the upgraded Estedad-FD family. */
val VazirmatnFontFamily: FontFamily = EstedadFontFamily

fun buildTarazinoTypography(scale: Float = 1.0f): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = (31 * scale).sp,
            lineHeight = (40 * scale).sp,
            letterSpacing = (-0.4).sp,
            fontFeatureSettings = "tnum"
        ),
        displayMedium = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (26 * scale).sp,
            lineHeight = (35 * scale).sp,
            letterSpacing = (-0.25).sp,
            fontFeatureSettings = "tnum"
        ),
        headlineLarge = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (23 * scale).sp,
            lineHeight = (32 * scale).sp,
            fontFeatureSettings = "tnum"
        ),
        headlineMedium = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (19.5f * scale).sp,
            lineHeight = (28 * scale).sp,
            fontFeatureSettings = "tnum"
        ),
        titleLarge = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (17.5f * scale).sp,
            lineHeight = (26 * scale).sp
        ),
        titleMedium = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15 * scale).sp,
            lineHeight = (23 * scale).sp
        ),
        titleSmall = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13.5f * scale).sp,
            lineHeight = (20 * scale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14.5f * scale).sp,
            lineHeight = (24 * scale).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13 * scale).sp,
            lineHeight = (21.5f * scale).sp
        ),
        bodySmall = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Light,
            fontSize = (11.5f * scale).sp,
            lineHeight = (19 * scale).sp
        ),
        labelLarge = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13 * scale).sp,
            lineHeight = (19 * scale).sp
        ),
        labelMedium = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11.5f * scale).sp,
            lineHeight = (17 * scale).sp
        ),
        labelSmall = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (10.5f * scale).sp,
            lineHeight = (15.5f * scale).sp
        )
    )
}

val Typography = buildTarazinoTypography(1.0f)
