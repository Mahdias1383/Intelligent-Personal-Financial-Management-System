package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Authentic 7-weight Estedad-FD (Farsi Digits) — modern Iranian neo-grotesque fintech typeface.
 * All 9 Material/Compose FontWeights map cleanly to static local .ttf files with 100% native
 * Persian letters and digits coverage, eliminating every fallback to system Roboto/Noto.
 */
val EstedadFontFamily = FontFamily(
    Font(R.font.estedad_light, FontWeight.Thin),
    Font(R.font.estedad_light, FontWeight.ExtraLight),
    Font(R.font.estedad_light, FontWeight.Light),
    Font(R.font.estedad_regular, FontWeight.Normal),
    Font(R.font.estedad_medium, FontWeight.Medium),
    Font(R.font.estedad_semibold, FontWeight.SemiBold),
    Font(R.font.estedad_bold, FontWeight.Bold),
    Font(R.font.estedad_extrabold, FontWeight.ExtraBold),
    Font(R.font.estedad_black, FontWeight.Black)
)

/** Backward-compatible alias pointing to the unified Estedad-FD family. */
val VazirmatnFontFamily: FontFamily = EstedadFontFamily

fun buildTarazinoTypography(scale: Float = 1.0f): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = (31 * scale).sp,
            lineHeight = (40 * scale).sp,
            letterSpacing = (-0.35).sp,
            fontFeatureSettings = "tnum",
            textDirection = TextDirection.Rtl
        ),
        displayMedium = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (26 * scale).sp,
            lineHeight = (35 * scale).sp,
            letterSpacing = (-0.2).sp,
            fontFeatureSettings = "tnum",
            textDirection = TextDirection.Rtl
        ),
        displaySmall = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = (24 * scale).sp,
            lineHeight = (32 * scale).sp,
            letterSpacing = (-0.15).sp,
            fontFeatureSettings = "tnum",
            textDirection = TextDirection.Rtl
        ),
        headlineLarge = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (22.5f * scale).sp,
            lineHeight = (31 * scale).sp,
            fontFeatureSettings = "tnum",
            textDirection = TextDirection.Rtl
        ),
        headlineMedium = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (19.5f * scale).sp,
            lineHeight = (28 * scale).sp,
            fontFeatureSettings = "tnum",
            textDirection = TextDirection.Rtl
        ),
        headlineSmall = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (18 * scale).sp,
            lineHeight = (26 * scale).sp,
            fontFeatureSettings = "tnum",
            textDirection = TextDirection.Rtl
        ),
        titleLarge = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (17 * scale).sp,
            lineHeight = (25 * scale).sp,
            textDirection = TextDirection.Rtl
        ),
        titleMedium = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15 * scale).sp,
            lineHeight = (23 * scale).sp,
            textDirection = TextDirection.Rtl
        ),
        titleSmall = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13.5f * scale).sp,
            lineHeight = (20.5f * scale).sp,
            textDirection = TextDirection.Rtl
        ),
        bodyLarge = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14.5f * scale).sp,
            lineHeight = (23.5f * scale).sp,
            textDirection = TextDirection.Rtl
        ),
        bodyMedium = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13 * scale).sp,
            lineHeight = (21.5f * scale).sp,
            textDirection = TextDirection.Rtl
        ),
        bodySmall = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (11.5f * scale).sp,
            lineHeight = (19 * scale).sp,
            textDirection = TextDirection.Rtl
        ),
        labelLarge = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13 * scale).sp,
            lineHeight = (19 * scale).sp,
            textDirection = TextDirection.Rtl
        ),
        labelMedium = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11.5f * scale).sp,
            lineHeight = (17 * scale).sp,
            textDirection = TextDirection.Rtl
        ),
        labelSmall = TextStyle(
            fontFamily = EstedadFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (10.5f * scale).sp,
            lineHeight = (15.5f * scale).sp,
            textDirection = TextDirection.Rtl
        )
    )
}

val Typography = buildTarazinoTypography(1.0f)
