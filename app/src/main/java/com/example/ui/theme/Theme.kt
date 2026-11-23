package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import com.example.data.FontScalePreset
import com.example.data.TarazinoThemeMode

@Immutable
data class TarazinoExtendedColors(
    val themeMode: TarazinoThemeMode,
    val isDark: Boolean,
    val moodTagFa: String,
    // 3-Layer Soft Ambient Background Wash Tokens
    val auroraPrimary: Color,
    val auroraSecondary: Color,
    val auroraTertiary: Color,
    // Sculpted Glass Surface & Specular Border Tokens
    val cardBorderTop: Color,
    val cardBorderBottom: Color,
    val cardGlassTop: Color,
    val cardGlassBottom: Color,
    val glassSurface: Color,
    val dockSurface: Color,
    val elevatedSurface: Color,
    val shimmerHighlight: Color,
    // High-Contrast Semantic Financial Status Tokens
    val positive: Color,
    val onPositive: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    // Hero Vault Card & Mini Brand Card Dedicated Palette Tokens
    val heroStart: Color,
    val heroMid: Color,
    val heroEnd: Color,
    val heroAccent: Color,
    val heroOnSurface: Color,
    val heroOnSurfaceMuted: Color,
    val miniCardStart: Color,
    val miniCardEnd: Color,
    val miniCardOnSurface: Color,
    val miniCardOnSurfaceMuted: Color,
    // Center Jewel FAB & Lock Vault & Tour Tokens
    val fabStart: Color,
    val fabMid: Color,
    val fabEnd: Color,
    val fabOnContent: Color,
    val lockBackdropStart: Color,
    val lockBackdropEnd: Color,
    val tourCardSurface: Color
)

private val ObsidianExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.OBSIDIAN_EMERALD_DARK,
    isDark = true,
    moodTagFa = "وقار مالی ابسیدین تیره با کنتراست عمیق زمردی",
    auroraPrimary = Color(0x1824C696),
    auroraSecondary = Color(0x124DA6F0),
    auroraTertiary = Color(0x0EE0BC6A),
    cardBorderTop = Color(0x289CDAC7),
    cardBorderBottom = Color(0x0AFFFFFF),
    cardGlassTop = Color(0xFF17232D),
    cardGlassBottom = Color(0xFF111A22),
    glassSurface = Color(0xF2131C24),
    dockSurface = Color(0xF5101820),
    elevatedSurface = ObsidianSurfaceHigh,
    shimmerHighlight = Color(0x2624C696),
    positive = StatusPaidGreen,
    onPositive = Color(0xFF031F17),
    warning = StatusUpcomingAmber,
    danger = StatusOverdueRed,
    info = CyanPrimary,
    heroStart = Color(0xFF0B2E28),
    heroMid = Color(0xFF0E212B),
    heroEnd = Color(0xFF0A1219),
    heroAccent = EmeraldPrimary,
    heroOnSurface = Color(0xFFF5FAF9),
    heroOnSurfaceMuted = Color(0xFFACC4C9),
    miniCardStart = Color(0xFF16232F),
    miniCardEnd = Color(0xFF101922),
    miniCardOnSurface = Color(0xFFF5FAF9),
    miniCardOnSurfaceMuted = Color(0xFFA2B6C5),
    fabStart = Color(0xFF39D9AA),
    fabMid = Color(0xFF1FB588),
    fabEnd = Color(0xFF0E7D5C),
    fabOnContent = Color(0xFF031E16),
    lockBackdropStart = Color(0xFF09161A),
    lockBackdropEnd = Color(0xFF0C1219),
    tourCardSurface = Color(0xFF131D26)
)

private val AlabasterExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.ALABASTER_LIGHT,
    isDark = false,
    moodTagFa = "شفافیت بانکداری روز با پس‌زمینه مرواریدی نرم و زمرد عمیق",
    auroraPrimary = Color(0x0E065F46),
    auroraSecondary = Color(0x0C9A4D06),
    auroraTertiary = Color(0x0A075975),
    cardBorderTop = Color(0xFFE5E2DA),
    cardBorderBottom = Color(0xFFD4CFC4),
    cardGlassTop = Color(0xFFFFFFFF),
    cardGlassBottom = Color(0xFFFCFBFA),
    glassSurface = Color(0xFCFFFFFF),
    dockSurface = Color(0xFAFFFFFF),
    elevatedSurface = AlabasterSurfaceHigh,
    shimmerHighlight = Color(0x20065F46),
    positive = BankEmeraldLightPrimary,
    onPositive = Color.White,
    warning = WarmAmberLight,
    danger = Color(0xFFB91C1C),
    info = Color(0xFF075985),
    // Deep Sovereign Forest-Emerald Vault Card for crisp daylight hierarchy
    heroStart = Color(0xFF064635),
    heroMid = Color(0xFF0A332E),
    heroEnd = Color(0xFF0D2129),
    heroAccent = Color(0xFF34D399),
    heroOnSurface = Color(0xFFF8FCFA),
    heroOnSurfaceMuted = Color(0xFFC7E4DA),
    // Crisp Satin Porcelain Mini Brand Card
    miniCardStart = Color(0xFFFFFFFF),
    miniCardEnd = Color(0xFFF4F2EC),
    miniCardOnSurface = Color(0xFF0F1720),
    miniCardOnSurfaceMuted = Color(0xFF475563),
    fabStart = Color(0xFF0D7A5C),
    fabMid = Color(0xFF065F46),
    fabEnd = Color(0xFF044936),
    fabOnContent = Color.White,
    lockBackdropStart = Color(0xFFEDF3F0),
    lockBackdropEnd = Color(0xFFF6F4EE),
    tourCardSurface = Color(0xFFFFFFFF)
)

private val TurquoiseLightExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.PERSIAN_TURQUOISE_LIGHT,
    isDark = false,
    moodTagFa = "آرامش مدرن فیروزه عمیق روی بوم روشن و خوانا",
    auroraPrimary = Color(0x0F075975),
    auroraSecondary = Color(0x0B9A5208),
    auroraTertiary = Color(0x0A3730A3),
    cardBorderTop = Color(0xFFD7E4EC),
    cardBorderBottom = Color(0xFFC4D6E2),
    cardGlassTop = Color(0xFFFFFFFF),
    cardGlassBottom = Color(0xFFF9FCFD),
    glassSurface = Color(0xFCFFFFFF),
    dockSurface = Color(0xFAFFFFFF),
    elevatedSurface = TurquoiseSurfaceHigh,
    shimmerHighlight = Color(0x22075975),
    positive = Color(0xFF065F46),
    onPositive = Color.White,
    warning = Color(0xFF9A5208),
    danger = Color(0xFFB91C1C),
    info = PersianTurquoisePrimary,
    // Deep Imperial Persian Teal-Navy Vault Card
    heroStart = Color(0xFF06445C),
    heroMid = Color(0xFF0A324D),
    heroEnd = Color(0xFF0B2036),
    heroAccent = Color(0xFF4CC9E8),
    heroOnSurface = Color(0xFFF4FBFF),
    heroOnSurfaceMuted = Color(0xFFC2E1EE),
    miniCardStart = Color(0xFFFFFFFF),
    miniCardEnd = Color(0xFFEEF4F8),
    miniCardOnSurface = Color(0xFF0B1B26),
    miniCardOnSurfaceMuted = Color(0xFF425B6B),
    fabStart = Color(0xFF0C7496),
    fabMid = Color(0xFF075975),
    fabEnd = Color(0xFF054359),
    fabOnContent = Color.White,
    lockBackdropStart = Color(0xFFE9F2F6),
    lockBackdropEnd = Color(0xFFF3F6F9),
    tourCardSurface = Color(0xFFFFFFFF)
)

private val SapphireExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.DEEP_SAPPHIRE,
    isDark = true,
    moodTagFa = "ساختار رسمی سرمه‌ای لاجوردی با کنتراست شفاف مالی",
    auroraPrimary = Color(0x184DA6F0),
    auroraSecondary = Color(0x14A794E8),
    auroraTertiary = Color(0x0CE2BE6B),
    cardBorderTop = Color(0x2A9EC5F2),
    cardBorderBottom = Color(0x0AFFFFFF),
    cardGlassTop = Color(0xFF172445),
    cardGlassBottom = Color(0xFF101A33),
    glassSurface = Color(0xF2121C36),
    dockSurface = Color(0xF50E162C),
    elevatedSurface = SapphireSurfaceHigh,
    shimmerHighlight = Color(0x264DA6F0),
    positive = Color(0xFF2CC295),
    onPositive = Color(0xFF042228),
    warning = SapphireGold,
    danger = StatusOverdueRed,
    info = CyanPrimary,
    heroStart = Color(0xFF142857),
    heroMid = Color(0xFF111E42),
    heroEnd = Color(0xFF0B1229),
    heroAccent = CyanPrimary,
    heroOnSurface = Color(0xFFF3F7FF),
    heroOnSurfaceMuted = Color(0xFFAFC3E6),
    miniCardStart = Color(0xFF18274E),
    miniCardEnd = Color(0xFF111B38),
    miniCardOnSurface = Color(0xFFF5F8FF),
    miniCardOnSurfaceMuted = Color(0xFFA8BCE0),
    fabStart = Color(0xFF6BBDF5),
    fabMid = Color(0xFF429CE8),
    fabEnd = Color(0xFF2364B8),
    fabOnContent = Color(0xFF041A30),
    lockBackdropStart = Color(0xFF091124),
    lockBackdropEnd = Color(0xFF0E1630),
    tourCardSurface = Color(0xFF131E3A)
)

private val MidnightGoldExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.MIDNIGHT_GOLD,
    isDark = true,
    moodTagFa = "وقار خزانه اختصاصی با زغال گرم و طلای مات شامپاینی",
    auroraPrimary = Color(0x18DEB14B),
    auroraSecondary = Color(0x12D9884B),
    auroraTertiary = Color(0x0C2CC295),
    cardBorderTop = Color(0x2CDEB14B),
    cardBorderBottom = Color(0x0AFFFFFF),
    cardGlassTop = Color(0xFF211D17),
    cardGlassBottom = Color(0xFF171410),
    glassSurface = Color(0xF21A1713),
    dockSurface = Color(0xF514120E),
    elevatedSurface = MidnightGoldSurfaceHigh,
    shimmerHighlight = Color(0x28DEB14B),
    positive = SovereignEmeraldAccent,
    onPositive = Color(0xFF04241A),
    warning = SovereignGoldPrimary,
    danger = StatusOverdueRed,
    info = Color(0xFF5FA6EA),
    heroStart = Color(0xFF2E2315),
    heroMid = Color(0xFF201912),
    heroEnd = Color(0xFF13100C),
    heroAccent = SovereignGoldPrimary,
    heroOnSurface = Color(0xFFFBF7F0),
    heroOnSurfaceMuted = Color(0xFFCEC1AF),
    miniCardStart = Color(0xFF252019),
    miniCardEnd = Color(0xFF181511),
    miniCardOnSurface = Color(0xFFF9F6F0),
    miniCardOnSurfaceMuted = Color(0xFFBDB09F),
    fabStart = Color(0xFFEDD082),
    fabMid = Color(0xFFDEB14B),
    fabEnd = Color(0xFFB07B1E),
    fabOnContent = Color(0xFF211706),
    lockBackdropStart = Color(0xFF15120D),
    lockBackdropEnd = Color(0xFF0C0B09),
    tourCardSurface = Color(0xFF1C1814)
)

private val VelvetAmethystExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.VELVET_AMETHYST,
    isDark = true,
    moodTagFa = "ارغوانی سلطنتی آرام و متعادل با هاله رز و شامپاین",
    auroraPrimary = Color(0x18C58DF0),
    auroraSecondary = Color(0x14D884A8),
    auroraTertiary = Color(0x0CE0BC6A),
    cardBorderTop = Color(0x2AD2B0F0),
    cardBorderBottom = Color(0x0AFFFFFF),
    cardGlassTop = Color(0xFF221C33),
    cardGlassBottom = Color(0xFF181324),
    glassSurface = Color(0xF21B1628),
    dockSurface = Color(0xF5151120),
    elevatedSurface = AmethystSurfaceHigh,
    shimmerHighlight = Color(0x26C58DF0),
    positive = Color(0xFF2CC295),
    onPositive = Color(0xFF04241A),
    warning = AmethystGold,
    danger = StatusOverdueRed,
    info = Color(0xFF5FA6EA),
    heroStart = Color(0xFF2F1D45),
    heroMid = Color(0xFF211634),
    heroEnd = Color(0xFF130E20),
    heroAccent = AmethystPrimary,
    heroOnSurface = Color(0xFFFAF7FD),
    heroOnSurfaceMuted = Color(0xFFCCBFE0),
    miniCardStart = Color(0xFF261F3A),
    miniCardEnd = Color(0xFF191428),
    miniCardOnSurface = Color(0xFFF8F5FC),
    miniCardOnSurfaceMuted = Color(0xFFBDB0D1),
    fabStart = Color(0xFFD7AEF7),
    fabMid = Color(0xFFC58DF0),
    fabEnd = Color(0xFF8C4EC4),
    fabOnContent = Color(0xFF1F0C30),
    lockBackdropStart = Color(0xFF140F20),
    lockBackdropEnd = Color(0xFF0C0A12),
    tourCardSurface = Color(0xFF1D172C)
)

// compositionLocalOf enables silky 60fps liquid color morphing across theme switches
val LocalTarazinoColors = compositionLocalOf { ObsidianExtendedColors }

private val ObsidianEmeraldScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF031F17),
    primaryContainer = Color(0xFF0B3A2C),
    onPrimaryContainer = Color(0xFFCFF7E8),
    secondary = GoldAccent,
    onSecondary = Color(0xFF221A08),
    secondaryContainer = Color(0xFF3C2F12),
    onSecondaryContainer = Color(0xFFFAF0D1),
    tertiary = CyanPrimary,
    onTertiary = Color(0xFF042034),
    tertiaryContainer = Color(0xFF123654),
    onTertiaryContainer = Color(0xFFD8EEFF),
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
    inverseSurface = Color(0xFFE7EFF2),
    inverseOnSurface = Color(0xFF121C24),
    inversePrimary = Color(0xFF065F46),
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineVariant,
    error = StatusOverdueRed,
    onError = Color(0xFF2E070D),
    errorContainer = Color(0xFF4D1520),
    onErrorContainer = Color(0xFFFFD9DF),
    scrim = Color(0xB304070A)
)

private val AlabasterLightScheme = lightColorScheme(
    primary = BankEmeraldLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F1E6),
    onPrimaryContainer = Color(0xFF043828),
    secondary = WarmAmberLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFAE9CE),
    onSecondaryContainer = Color(0xFF5C2B04),
    tertiary = Color(0xFF075975),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD4ECF5),
    onTertiaryContainer = Color(0xFF033142),
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
    inverseSurface = Color(0xFF18222B),
    inverseOnSurface = Color(0xFFF3F6F8),
    inversePrimary = Color(0xFF5EEAD4),
    outline = AlabasterOutline,
    outlineVariant = AlabasterOutlineVariant,
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFDE2E2),
    onErrorContainer = Color(0xFF6E0F0F),
    scrim = Color(0x7A0B1219)
)

private val PersianTurquoiseLightScheme = lightColorScheme(
    primary = PersianTurquoisePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4ECF5),
    onPrimaryContainer = Color(0xFF033142),
    secondary = PersianTurquoiseSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFAEBD2),
    onSecondaryContainer = Color(0xFF5A2E04),
    tertiary = PersianTurquoiseTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE1E4FA),
    onTertiaryContainer = Color(0xFF1E1B5E),
    background = TurquoiseBg,
    onBackground = TurquoiseOnBg,
    surface = TurquoiseSurface,
    onSurface = TurquoiseOnBg,
    surfaceVariant = TurquoiseSurfaceVariant,
    onSurfaceVariant = TurquoiseOnSurfaceVariant,
    surfaceContainerLowest = TurquoiseSurfaceLowest,
    surfaceContainerLow = TurquoiseSurface,
    surfaceContainer = TurquoiseSurfaceHigh,
    surfaceContainerHigh = TurquoiseSurfaceHighest,
    surfaceContainerHighest = TurquoiseSurfaceVariant,
    inverseSurface = Color(0xFF13222E),
    inverseOnSurface = Color(0xFFF1F6FA),
    inversePrimary = Color(0xFF6AD4F0),
    outline = TurquoiseOutline,
    outlineVariant = TurquoiseOutlineVariant,
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFDE2E2),
    onErrorContainer = Color(0xFF6E0F0F),
    scrim = Color(0x7A091620)
)

private val DeepSapphireScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color(0xFF041C30),
    primaryContainer = Color(0xFF153761),
    onPrimaryContainer = Color(0xFFDBEFFF),
    secondary = SapphireViolet,
    onSecondary = Color(0xFF1B123A),
    secondaryContainer = Color(0xFF2D2259),
    onSecondaryContainer = Color(0xFFECE6FF),
    tertiary = SapphireGold,
    onTertiary = Color(0xFF241B07),
    tertiaryContainer = Color(0xFF3E3012),
    onTertiaryContainer = Color(0xFFFAF0D1),
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
    inverseSurface = Color(0xFFE8EFFA),
    inverseOnSurface = Color(0xFF121C36),
    inversePrimary = Color(0xFF145294),
    outline = SapphireOutline,
    outlineVariant = SapphireOutlineVariant,
    error = StatusOverdueRed,
    onError = Color(0xFF2E070D),
    errorContainer = Color(0xFF4D1520),
    onErrorContainer = Color(0xFFFFD9DF),
    scrim = Color(0xB3040712)
)

private val MidnightGoldScheme = darkColorScheme(
    primary = SovereignGoldPrimary,
    onPrimary = Color(0xFF211706),
    primaryContainer = Color(0xFF403114),
    onPrimaryContainer = Color(0xFFFCF0CF),
    secondary = SovereignGoldSecondary,
    onSecondary = Color(0xFF261406),
    secondaryContainer = Color(0xFF422713),
    onSecondaryContainer = Color(0xFFFDE7D4),
    tertiary = SovereignEmeraldAccent,
    onTertiary = Color(0xFF042218),
    tertiaryContainer = Color(0xFF0E382B),
    onTertiaryContainer = Color(0xFFD1F6E8),
    background = MidnightGoldBg,
    onBackground = MidnightGoldOnBg,
    surface = MidnightGoldSurface,
    onSurface = MidnightGoldOnBg,
    surfaceVariant = MidnightGoldSurfaceVariant,
    onSurfaceVariant = MidnightGoldOnSurfaceVariant,
    surfaceContainerLowest = MidnightGoldSurfaceLowest,
    surfaceContainerLow = MidnightGoldSurface,
    surfaceContainer = MidnightGoldSurface,
    surfaceContainerHigh = MidnightGoldSurfaceHigh,
    surfaceContainerHighest = MidnightGoldSurfaceHighest,
    inverseSurface = Color(0xFFF2ECE2),
    inverseOnSurface = Color(0xFF1A1713),
    inversePrimary = Color(0xFF7D590F),
    outline = MidnightGoldOutline,
    outlineVariant = MidnightGoldOutlineVariant,
    error = StatusOverdueRed,
    onError = Color(0xFF2E070D),
    errorContainer = Color(0xFF4D1520),
    onErrorContainer = Color(0xFFFFD9DF),
    scrim = Color(0xB3070605)
)

private val VelvetAmethystScheme = darkColorScheme(
    primary = AmethystPrimary,
    onPrimary = Color(0xFF200C33),
    primaryContainer = Color(0xFF3D2459),
    onPrimaryContainer = Color(0xFFF3E6FF),
    secondary = AmethystSecondary,
    onSecondary = Color(0xFF2C0E1D),
    secondaryContainer = Color(0xFF4A2035),
    onSecondaryContainer = Color(0xFFFDE4EE),
    tertiary = AmethystGold,
    onTertiary = Color(0xFF241B07),
    tertiaryContainer = Color(0xFF3E3012),
    onTertiaryContainer = Color(0xFFFAF0D1),
    background = AmethystBg,
    onBackground = AmethystOnBg,
    surface = AmethystSurface,
    onSurface = AmethystOnBg,
    surfaceVariant = AmethystSurfaceVariant,
    onSurfaceVariant = AmethystOnSurfaceVariant,
    surfaceContainerLowest = AmethystSurfaceLowest,
    surfaceContainerLow = AmethystSurface,
    surfaceContainer = AmethystSurface,
    surfaceContainerHigh = AmethystSurfaceHigh,
    surfaceContainerHighest = AmethystSurfaceHighest,
    inverseSurface = Color(0xFFEFEAF7),
    inverseOnSurface = Color(0xFF1B1628),
    inversePrimary = Color(0xFF6B3AA1),
    outline = AmethystOutline,
    outlineVariant = AmethystOutlineVariant,
    error = StatusOverdueRed,
    onError = Color(0xFF2E070D),
    errorContainer = Color(0xFF4D1520),
    onErrorContainer = Color(0xFFFFD9DF),
    scrim = Color(0xB307050B)
)

private val ThemeMorphSpec = tween<Color>(durationMillis = 420, easing = FastOutSlowInEasing)

@Composable
private fun animColor(target: Color, label: String): Color {
    val color by animateColorAsState(targetValue = target, animationSpec = ThemeMorphSpec, label = label)
    return color
}

@Composable
private fun animateTarazinoColorScheme(target: ColorScheme): ColorScheme {
    return target.copy(
        primary = animColor(target.primary, "cs_primary"),
        onPrimary = animColor(target.onPrimary, "cs_onPrimary"),
        primaryContainer = animColor(target.primaryContainer, "cs_primaryContainer"),
        onPrimaryContainer = animColor(target.onPrimaryContainer, "cs_onPrimaryContainer"),
        secondary = animColor(target.secondary, "cs_secondary"),
        onSecondary = animColor(target.onSecondary, "cs_onSecondary"),
        secondaryContainer = animColor(target.secondaryContainer, "cs_secondaryContainer"),
        onSecondaryContainer = animColor(target.onSecondaryContainer, "cs_onSecondaryContainer"),
        tertiary = animColor(target.tertiary, "cs_tertiary"),
        onTertiary = animColor(target.onTertiary, "cs_onTertiary"),
        tertiaryContainer = animColor(target.tertiaryContainer, "cs_tertiaryContainer"),
        onTertiaryContainer = animColor(target.onTertiaryContainer, "cs_onTertiaryContainer"),
        background = animColor(target.background, "cs_background"),
        onBackground = animColor(target.onBackground, "cs_onBackground"),
        surface = animColor(target.surface, "cs_surface"),
        onSurface = animColor(target.onSurface, "cs_onSurface"),
        surfaceVariant = animColor(target.surfaceVariant, "cs_surfaceVariant"),
        onSurfaceVariant = animColor(target.onSurfaceVariant, "cs_onSurfaceVariant"),
        surfaceContainerLowest = animColor(target.surfaceContainerLowest, "cs_scLowest"),
        surfaceContainerLow = animColor(target.surfaceContainerLow, "cs_scLow"),
        surfaceContainer = animColor(target.surfaceContainer, "cs_sc"),
        surfaceContainerHigh = animColor(target.surfaceContainerHigh, "cs_scHigh"),
        surfaceContainerHighest = animColor(target.surfaceContainerHighest, "cs_scHighest"),
        inverseSurface = animColor(target.inverseSurface, "cs_invSurface"),
        inverseOnSurface = animColor(target.inverseOnSurface, "cs_invOnSurface"),
        inversePrimary = animColor(target.inversePrimary, "cs_invPrimary"),
        outline = animColor(target.outline, "cs_outline"),
        outlineVariant = animColor(target.outlineVariant, "cs_outlineVariant"),
        error = animColor(target.error, "cs_error"),
        onError = animColor(target.onError, "cs_onError"),
        errorContainer = animColor(target.errorContainer, "cs_errorContainer"),
        onErrorContainer = animColor(target.onErrorContainer, "cs_onErrorContainer"),
        scrim = animColor(target.scrim, "cs_scrim")
    )
}

@Composable
private fun animateTarazinoExtendedColors(target: TarazinoExtendedColors): TarazinoExtendedColors {
    return target.copy(
        auroraPrimary = animColor(target.auroraPrimary, "ext_aurora1"),
        auroraSecondary = animColor(target.auroraSecondary, "ext_aurora2"),
        auroraTertiary = animColor(target.auroraTertiary, "ext_aurora3"),
        cardBorderTop = animColor(target.cardBorderTop, "ext_borderTop"),
        cardBorderBottom = animColor(target.cardBorderBottom, "ext_borderBottom"),
        cardGlassTop = animColor(target.cardGlassTop, "ext_glassTop"),
        cardGlassBottom = animColor(target.cardGlassBottom, "ext_glassBottom"),
        glassSurface = animColor(target.glassSurface, "ext_glass"),
        dockSurface = animColor(target.dockSurface, "ext_dock"),
        elevatedSurface = animColor(target.elevatedSurface, "ext_elevated"),
        shimmerHighlight = animColor(target.shimmerHighlight, "ext_shimmer"),
        positive = animColor(target.positive, "ext_positive"),
        onPositive = animColor(target.onPositive, "ext_onPositive"),
        warning = animColor(target.warning, "ext_warning"),
        danger = animColor(target.danger, "ext_danger"),
        info = animColor(target.info, "ext_info"),
        heroStart = animColor(target.heroStart, "ext_heroStart"),
        heroMid = animColor(target.heroMid, "ext_heroMid"),
        heroEnd = animColor(target.heroEnd, "ext_heroEnd"),
        heroAccent = animColor(target.heroAccent, "ext_heroAccent"),
        heroOnSurface = animColor(target.heroOnSurface, "ext_heroOn"),
        heroOnSurfaceMuted = animColor(target.heroOnSurfaceMuted, "ext_heroOnMuted"),
        miniCardStart = animColor(target.miniCardStart, "ext_miniStart"),
        miniCardEnd = animColor(target.miniCardEnd, "ext_miniEnd"),
        miniCardOnSurface = animColor(target.miniCardOnSurface, "ext_miniOn"),
        miniCardOnSurfaceMuted = animColor(target.miniCardOnSurfaceMuted, "ext_miniMuted"),
        fabStart = animColor(target.fabStart, "ext_fabStart"),
        fabMid = animColor(target.fabMid, "ext_fabMid"),
        fabEnd = animColor(target.fabEnd, "ext_fabEnd"),
        fabOnContent = animColor(target.fabOnContent, "ext_fabOn"),
        lockBackdropStart = animColor(target.lockBackdropStart, "ext_lockStart"),
        lockBackdropEnd = animColor(target.lockBackdropEnd, "ext_lockEnd"),
        tourCardSurface = animColor(target.tourCardSurface, "ext_tourSurface")
    )
}

@Composable
fun TarazinoTheme(
    themeMode: TarazinoThemeMode = TarazinoThemeMode.OBSIDIAN_EMERALD_DARK,
    fontScale: FontScalePreset = FontScalePreset.STANDARD,
    content: @Composable () -> Unit
) {
    val targetColorScheme = when (themeMode) {
        TarazinoThemeMode.OBSIDIAN_EMERALD_DARK -> ObsidianEmeraldScheme
        TarazinoThemeMode.ALABASTER_LIGHT -> AlabasterLightScheme
        TarazinoThemeMode.PERSIAN_TURQUOISE_LIGHT -> PersianTurquoiseLightScheme
        TarazinoThemeMode.DEEP_SAPPHIRE -> DeepSapphireScheme
        TarazinoThemeMode.MIDNIGHT_GOLD -> MidnightGoldScheme
        TarazinoThemeMode.VELVET_AMETHYST -> VelvetAmethystScheme
    }

    val targetExtendedColors = when (themeMode) {
        TarazinoThemeMode.OBSIDIAN_EMERALD_DARK -> ObsidianExtendedColors
        TarazinoThemeMode.ALABASTER_LIGHT -> AlabasterExtendedColors
        TarazinoThemeMode.PERSIAN_TURQUOISE_LIGHT -> TurquoiseLightExtendedColors
        TarazinoThemeMode.DEEP_SAPPHIRE -> SapphireExtendedColors
        TarazinoThemeMode.MIDNIGHT_GOLD -> MidnightGoldExtendedColors
        TarazinoThemeMode.VELVET_AMETHYST -> VelvetAmethystExtendedColors
    }

    // Velvety 420ms cross-theme liquid color morphing across every Material 3 & extended token
    val animatedColorScheme = animateTarazinoColorScheme(targetColorScheme)
    val animatedExtendedColors = animateTarazinoExtendedColors(targetExtendedColors)

    val typography = remember(fontScale) { buildTarazinoTypography(fontScale.scaleFactor) }

    // Dynamically keep status bar & navigation bar icon contrast matched to active Light/Dark theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !targetExtendedColors.isDark
                controller.isAppearanceLightNavigationBars = !targetExtendedColors.isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalTarazinoColors provides animatedExtendedColors
    ) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            typography = typography
        ) {
            // CRITICAL: ProvideTextStyle ensures LocalTextStyle defaults to Estedad-FD across
            // every OutlinedTextField, Button, FilterChip, Dialog, and Text without explicit style!
            ProvideTextStyle(value = typography.bodyMedium) {
                content()
            }
        }
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
