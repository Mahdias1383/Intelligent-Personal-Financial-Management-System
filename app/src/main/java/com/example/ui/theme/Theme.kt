package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.FontScalePreset
import com.example.data.TarazinoThemeMode

@Immutable
data class TarazinoExtendedColors(
    val isDark: Boolean,
    val auroraPrimary: Color,
    val auroraSecondary: Color,
    val cardBorderTop: Color,
    val cardBorderBottom: Color,
    val glassSurface: Color,
    val elevatedSurface: Color,
    val positive: Color,
    val warning: Color,
    val danger: Color,
    val info: Color
)

private val ObsidianExtendedColors = TarazinoExtendedColors(
    isDark = true,
    auroraPrimary = Color(0x2800E5A0),
    auroraSecondary = Color(0x1EE8C67D),
    cardBorderTop = Color(0x34FFFFFF),
    cardBorderBottom = Color(0x10FFFFFF),
    glassSurface = Color(0xEB0B1017),
    elevatedSurface = ObsidianSurfaceHigh,
    positive = StatusPaidGreen,
    warning = StatusUpcomingAmber,
    danger = StatusOverdueRed,
    info = CyanSecondary
)

private val AlabasterExtendedColors = TarazinoExtendedColors(
    isDark = false,
    auroraPrimary = Color(0x18047857),
    auroraSecondary = Color(0x14D97706),
    cardBorderTop = Color(0xFFDCE7E2),
    cardBorderBottom = Color(0xFFC9D9D2),
    glassSurface = Color(0xF5FFFFFF),
    elevatedSurface = AlabasterSurfaceHigh,
    positive = BankEmeraldLightPrimary,
    warning = WarmAmberLight,
    danger = Color(0xFFDC2626),
    info = Color(0xFF0284C7)
)

private val SapphireExtendedColors = TarazinoExtendedColors(
    isDark = true,
    auroraPrimary = Color(0x2822D3EE),
    auroraSecondary = Color(0x1CF3CF7A),
    cardBorderTop = Color(0x34A5D8FF),
    cardBorderBottom = Color(0x12FFFFFF),
    glassSurface = Color(0xEB0A1226),
    elevatedSurface = SapphireSurfaceHigh,
    positive = StatusPaidGreen,
    warning = SapphireGold,
    danger = StatusOverdueRed,
    info = CyanSecondary
)

val LocalTarazinoColors = staticCompositionLocalOf { ObsidianExtendedColors }

private val ObsidianEmeraldScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF022019),
    primaryContainer = Color(0xFF053D2F),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = GoldAccent,
    onSecondary = Color(0xFF1F190D),
    secondaryContainer = Color(0xFF3E2B0D),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = CyanPrimary,
    onTertiary = Color(0xFF042632),
    background = ObsidianBg,
    onBackground = ObsidianOnBg,
    surface = ObsidianSurface,
    onSurface = ObsidianOnBg,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianOnSurfaceVariant,
    surfaceContainerLowest = ObsidianSurfaceLowest,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurface,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerHighest = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineVariant,
    error = StatusOverdueRed,
    onError = Color.White
)

private val AlabasterLightScheme = lightColorScheme(
    primary = BankEmeraldLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = WarmAmberLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = AlabasterBg,
    onBackground = AlabasterOnBg,
    surface = AlabasterSurface,
    onSurface = AlabasterOnBg,
    surfaceVariant = AlabasterSurfaceVariant,
    onSurfaceVariant = AlabasterOnSurfaceVariant,
    surfaceContainerLowest = AlabasterSurfaceLowest,
    surfaceContainerLow = AlabasterSurface,
    surfaceContainer = AlabasterSurfaceHigh,
    surfaceContainerHigh = AlabasterSurfaceHighest,
    surfaceContainerHighest = AlabasterSurfaceVariant,
    outline = AlabasterOutline,
    outlineVariant = AlabasterOutlineVariant,
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val DeepSapphireScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color(0xFF042233),
    primaryContainer = Color(0xFF13455B),
    onPrimaryContainer = Color(0xFFCFFAFE),
    secondary = SapphireGold,
    onSecondary = Color(0xFF1F190D),
    secondaryContainer = Color(0xFF3E2B0D),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = EmeraldPrimary,
    onTertiary = Color(0xFF022019),
    background = SapphireBg,
    onBackground = SapphireOnBg,
    surface = SapphireSurface,
    onSurface = SapphireOnBg,
    surfaceVariant = SapphireSurfaceVariant,
    onSurfaceVariant = SapphireOnSurfaceVariant,
    surfaceContainerLowest = SapphireSurfaceLowest,
    surfaceContainerLow = SapphireSurface,
    surfaceContainer = SapphireSurface,
    surfaceContainerHigh = SapphireSurfaceHigh,
    surfaceContainerHighest = SapphireSurfaceHighest,
    outline = SapphireOutline,
    outlineVariant = SapphireOutlineVariant,
    error = StatusOverdueRed,
    onError = Color.White
)

@Composable
fun TarazinoTheme(
    themeMode: TarazinoThemeMode = TarazinoThemeMode.OBSIDIAN_EMERALD_DARK,
    fontScale: FontScalePreset = FontScalePreset.STANDARD,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        TarazinoThemeMode.OBSIDIAN_EMERALD_DARK -> ObsidianEmeraldScheme
        TarazinoThemeMode.ALABASTER_LIGHT -> AlabasterLightScheme
        TarazinoThemeMode.DEEP_SAPPHIRE -> DeepSapphireScheme
    }

    val extendedColors = when (themeMode) {
        TarazinoThemeMode.OBSIDIAN_EMERALD_DARK -> ObsidianExtendedColors
        TarazinoThemeMode.ALABASTER_LIGHT -> AlabasterExtendedColors
        TarazinoThemeMode.DEEP_SAPPHIRE -> SapphireExtendedColors
    }

    val typography = buildTarazinoTypography(fontScale.scaleFactor)

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalTarazinoColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    TarazinoTheme(
        themeMode = if (darkTheme) TarazinoThemeMode.OBSIDIAN_EMERALD_DARK else TarazinoThemeMode.ALABASTER_LIGHT,
        content = content
    )
}
