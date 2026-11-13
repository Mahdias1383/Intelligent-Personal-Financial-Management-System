package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn, FontWeight.Normal),
    Font(R.font.vazirmatn, FontWeight.Medium),
    Font(R.font.vazirmatn, FontWeight.SemiBold),
    Font(R.font.vazirmatn, FontWeight.Bold)
)

fun buildTarazinoTypography(scale: Float = 1.0f): Typography {
    return Typography(
        displayLarge = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (30 * scale).sp,
            lineHeight = (40 * scale).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (24 * scale).sp,
            lineHeight = (34 * scale).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (20 * scale).sp,
            lineHeight = (28 * scale).sp
        ),
        titleLarge = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (18 * scale).sp,
            lineHeight = (26 * scale).sp
        ),
        titleMedium = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15 * scale).sp,
            lineHeight = (23 * scale).sp
        ),
        titleSmall = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (13.5f * scale).sp,
            lineHeight = (20 * scale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14.5f * scale).sp,
            lineHeight = (23 * scale).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13 * scale).sp,
            lineHeight = (20 * scale).sp
        ),
        bodySmall = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (11.5f * scale).sp,
            lineHeight = (18 * scale).sp
        ),
        labelLarge = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13 * scale).sp,
            lineHeight = (19 * scale).sp
        ),
        labelMedium = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11.5f * scale).sp,
            lineHeight = (16 * scale).sp
        ),
        labelSmall = TextStyle(
            fontFamily = VazirmatnFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (10.5f * scale).sp,
            lineHeight = (15 * scale).sp
        )
    )
}

val Typography = buildTarazinoTypography(1.0f)
