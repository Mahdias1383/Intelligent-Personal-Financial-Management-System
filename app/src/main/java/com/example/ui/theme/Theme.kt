package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.FontScalePreset
import com.example.data.TarazinoThemeMode

private val ObsidianEmeraldScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = GoldAccent,
    onSecondary = Color(0xFF1E1B18),
    secondaryContainer = Color(0xFF451A03),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = CyanPrimary,
    background = ObsidianBg,
    onBackground = ObsidianOnBg,
    surface = ObsidianSurface,
    onSurface = ObsidianOnBg,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianOnSurfaceVariant,
    error = StatusOverdueRed
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
    background = AlabasterBg,
    onBackground = AlabasterOnBg,
    surface = AlabasterSurface,
    onSurface = AlabasterOnBg,
    surfaceVariant = AlabasterSurfaceVariant,
    onSurfaceVariant = AlabasterOnSurfaceVariant,
    error = StatusOverdueRed
)

private val DeepSapphireScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF164E63),
    onPrimaryContainer = Color(0xFFCFFAFE),
    secondary = SapphireGold,
    onSecondary = Color(0xFF1E1B18),
    secondaryContainer = Color(0xFF451A03),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = EmeraldPrimary,
    background = SapphireBg,
    onBackground = SapphireOnBg,
    surface = SapphireSurface,
    onSurface = SapphireOnBg,
    surfaceVariant = SapphireSurfaceVariant,
    onSurfaceVariant = SapphireOnSurfaceVariant,
    error = StatusOverdueRed
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

    val typography = buildTarazinoTypography(fontScale.scaleFactor)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
