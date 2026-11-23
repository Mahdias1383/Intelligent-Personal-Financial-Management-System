package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
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
    moodTagFa = "وقار مالی ابسیدین تیره با کنتراست شفاف زمردی و شامپاینی",
    auroraPrimary = Color(0x1810D9A0),
    auroraSecondary = Color(0x1438BDF8),
    auroraTertiary = Color(0x0EF2C97D),
    cardBorderTop = Color(0x2E8EEAD0),
    cardBorderBottom = Color(0x10FFFFFF),
    cardGlassTop = Color(0xFF152030),
    cardGlassBottom = Color(0xFF0F1724),
    glassSurface = Color(0xF5111926),
    dockSurface = Color(0xF70D1420),
    elevatedSurface = ObsidianSurfaceHigh,
    shimmerHighlight = Color(0x2610D9A0),
    positive = StatusPaidGreen,
    onPositive = Color(0xFF022117),
    warning = StatusUpcomingAmber,
    danger = StatusOverdueRed,
    info = CyanPrimary,
    heroStart = Color(0xFF073B32),
    heroMid = Color(0xFF0B2532),
    heroEnd = Color(0xFF09131F),
    heroAccent = EmeraldPrimary,
    heroOnSurface = Color(0xFFF8FAFC),
    heroOnSurfaceMuted = Color(0xFFB8D0DC),
    miniCardStart = Color(0xFF172334),
    miniCardEnd = Color(0xFF101826),
    miniCardOnSurface = Color(0xFFF8FAFC),
    miniCardOnSurfaceMuted = Color(0xFFADC0D4),
    fabStart = Color(0xFF2CEAA8),
    fabMid = Color(0xFF10D9A0),
    fabEnd = Color(0xFF089970),
    fabOnContent = Color(0xFF022117),
    lockBackdropStart = Color(0xFF07131A),
    lockBackdropEnd = Color(0xFF070B11),
    tourCardSurface = Color(0xFF121C2B)
)

private val AlabasterExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.ALABASTER_LIGHT,
    isDark = false,
    moodTagFa = "شفافیت بانکداری روز با بوم مرواریدی خالص و زمرد سلطنتی",
    auroraPrimary = Color(0x10046C4E),
    auroraSecondary = Color(0x0CB45309),
    auroraTertiary = Color(0x0A026AA2),
    cardBorderTop = Color(0xFFDCE5F0),
    cardBorderBottom = Color(0xFFCBD6E4),
    cardGlassTop = Color(0xFFFFFFFF),
    cardGlassBottom = Color(0xFFFBFDFF),
    glassSurface = Color(0xFCFFFFFF),
    dockSurface = Color(0xFAFFFFFF),
    elevatedSurface = AlabasterSurfaceHigh,
    shimmerHighlight = Color(0x1F046C4E),
    positive = BankEmeraldLightPrimary,
    onPositive = Color.White,
    warning = WarmAmberLight,
    danger = Color(0xFFDC2626),
    info = Color(0xFF026AA2),
    heroStart = Color(0xFF054A37),
    heroMid = Color(0xFF083732),
    heroEnd = Color(0xFF0A2229),
    heroAccent = Color(0xFF34D399),
    heroOnSurface = Color(0xFFF8FCFA),
    heroOnSurfaceMuted = Color(0xFFCDE8DF),
    miniCardStart = Color(0xFFFFFFFF),
    miniCardEnd = Color(0xFFF2F6FA),
    miniCardOnSurface = Color(0xFF0B1320),
    miniCardOnSurfaceMuted = Color(0xFF475569),
    fabStart = Color(0xFF0A8762),
    fabMid = Color(0xFF046C4E),
    fabEnd = Color(0xFF03523B),
    fabOnContent = Color.White,
    lockBackdropStart = Color(0xFFEAF2F0),
    lockBackdropEnd = Color(0xFFF4F6F9),
    tourCardSurface = Color(0xFFFFFFFF)
)

private val TurquoiseLightExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.PERSIAN_TURQUOISE_LIGHT,
    isDark = false,
    moodTagFa = "اصالت فیروزه ایرانی روی بوم روشن با کنتراست عالی",
    auroraPrimary = Color(0x10026AA2),
    auroraSecondary = Color(0x0CB45309),
    auroraTertiary = Color(0x0A4338CA),
    cardBorderTop = Color(0xFFD2E4F0),
    cardBorderBottom = Color(0xFFBFD6E6),
    cardGlassTop = Color(0xFFFFFFFF),
    cardGlassBottom = Color(0xFFF9FCFF),
    glassSurface = Color(0xFCFFFFFF),
    dockSurface = Color(0xFAFFFFFF),
    elevatedSurface = TurquoiseSurfaceHigh,
    shimmerHighlight = Color(0x20026AA2),
    positive = Color(0xFF046C4E),
    onPositive = Color.White,
    warning = Color(0xFFB45309),
    danger = Color(0xFFDC2626),
    info = PersianTurquoisePrimary,
    heroStart = Color(0xFF064E6E),
    heroMid = Color(0xFF093859),
    heroEnd = Color(0xFF0A213A),
    heroAccent = Color(0xFF38BDF8),
    heroOnSurface = Color(0xFFF5FBFF),
    heroOnSurfaceMuted = Color(0xFFC7E4F4),
    miniCardStart = Color(0xFFFFFFFF),
    miniCardEnd = Color(0xFFEDF5FA),
    miniCardOnSurface = Color(0xFF081826),
    miniCardOnSurfaceMuted = Color(0xFF3E586B),
    fabStart = Color(0xFF0884C4),
    fabMid = Color(0xFF026AA2),
    fabEnd = Color(0xFF03517D),
    fabOnContent = Color.White,
    lockBackdropStart = Color(0xFFE6F1F8),
    lockBackdropEnd = Color(0xFFF0F6FA),
    tourCardSurface = Color(0xFFFFFFFF)
)

private val SapphireExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.DEEP_SAPPHIRE,
    isDark = true,
    moodTagFa = "ساختار رسمی سرمه‌ای کبالت با درخشش لاجوردی و نقره‌ای",
    auroraPrimary = Color(0x1A38BDF8),
    auroraSecondary = Color(0x14818CF8),
    auroraTertiary = Color(0x0CFBBF24),
    cardBorderTop = Color(0x308CC8FA),
    cardBorderBottom = Color(0x10FFFFFF),
    cardGlassTop = Color(0xFF152347),
    cardGlassBottom = Color(0xFF0E1833),
    glassSurface = Color(0xF5101A36),
    dockSurface = Color(0xF70C142B),
    elevatedSurface = SapphireSurfaceHigh,
    shimmerHighlight = Color(0x2638BDF8),
    positive = Color(0xFF10D9A0),
    onPositive = Color(0xFF022117),
    warning = SapphireGold,
    danger = StatusOverdueRed,
    info = CyanPrimary,
    heroStart = Color(0xFF102A63),
    heroMid = Color(0xFF0E1F4A),
    heroEnd = Color(0xFF09132E),
    heroAccent = CyanPrimary,
    heroOnSurface = Color(0xFFF5F8FF),
    heroOnSurfaceMuted = Color(0xFFB8CBF0),
    miniCardStart = Color(0xFF17264F),
    miniCardEnd = Color(0xFF101B3B),
    miniCardOnSurface = Color(0xFFF5F8FF),
    miniCardOnSurfaceMuted = Color(0xFFADC0E2),
    fabStart = Color(0xFF60A5FA),
    fabMid = Color(0xFF38BDF8),
    fabEnd = Color(0xFF0284C7),
    fabOnContent = Color(0xFF03192E),
    lockBackdropStart = Color(0xFF081024),
    lockBackdropEnd = Color(0xFF060A17),
    tourCardSurface = Color(0xFF121D3D)
)

private val MidnightGoldExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.MIDNIGHT_GOLD,
    isDark = true,
    moodTagFa = "خزانه اختصاصی اونیکس کربنی با طلای ۲۴ عیار شامپاینی",
    auroraPrimary = Color(0x18F3C663),
    auroraSecondary = Color(0x12E59866),
    auroraTertiary = Color(0x0C10D9A0),
    cardBorderTop = Color(0x32F3C663),
    cardBorderBottom = Color(0x10FFFFFF),
    cardGlassTop = Color(0xFF1D1D25),
    cardGlassBottom = Color(0xFF14141A),
    glassSurface = Color(0xF516161C),
    dockSurface = Color(0xF7111116),
    elevatedSurface = MidnightGoldSurfaceHigh,
    shimmerHighlight = Color(0x28F3C663),
    positive = SovereignEmeraldAccent,
    onPositive = Color(0xFF022117),
    warning = SovereignGoldPrimary,
    danger = StatusOverdueRed,
    info = Color(0xFF38BDF8),
    heroStart = Color(0xFF2B2112),
    heroMid = Color(0xFF1C1714),
    heroEnd = Color(0xFF111115),
    heroAccent = SovereignGoldPrimary,
    heroOnSurface = Color(0xFFFAF8F5),
    heroOnSurfaceMuted = Color(0xFFD4CBBF),
    miniCardStart = Color(0xFF202028),
    miniCardEnd = Color(0xFF15151B),
    miniCardOnSurface = Color(0xFFFAF8F5),
    miniCardOnSurfaceMuted = Color(0xFFBEB8AE),
    fabStart = Color(0xFFF9DC8C),
    fabMid = Color(0xFFF3C663),
    fabEnd = Color(0xFFC49229),
    fabOnContent = Color(0xFF211604),
    lockBackdropStart = Color(0xFF131214),
    lockBackdropEnd = Color(0xFF09090B),
    tourCardSurface = Color(0xFF191920)
)

private val VelvetAmethystExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.VELVET_AMETHYST,
    isDark = true,
    moodTagFa = "ارغوانی سلطنتی عمیق با کنتراست یاسی، رز و شامپاین",
    auroraPrimary = Color(0x1AC084FC),
    auroraSecondary = Color(0x14F472B6),
    auroraTertiary = Color(0x0CFBBF24),
    cardBorderTop = Color(0x30D4A8FF),
    cardBorderBottom = Color(0x10FFFFFF),
    cardGlassTop = Color(0xFF201A3A),
    cardGlassBottom = Color(0xFF151128),
    glassSurface = Color(0xF517132B),
    dockSurface = Color(0xF7120E22),
    elevatedSurface = AmethystSurfaceHigh,
    shimmerHighlight = Color(0x26C084FC),
    positive = Color(0xFF10D9A0),
    onPositive = Color(0xFF022117),
    warning = AmethystGold,
    danger = StatusOverdueRed,
    info = Color(0xFF38BDF8),
    heroStart = Color(0xFF2E1A4D),
    heroMid = Color(0xFF1F1338),
    heroEnd = Color(0xFF110C22),
    heroAccent = AmethystPrimary,
    heroOnSurface = Color(0xFFFAF7FF),
    heroOnSurfaceMuted = Color(0xFFD2C7EC),
    miniCardStart = Color(0xFF231C3F),
    miniCardEnd = Color(0xFF161229),
    miniCardOnSurface = Color(0xFFFAF7FF),
    miniCardOnSurfaceMuted = Color(0xFFBDB2D9),
    fabStart = Color(0xFFD8B4FE),
    fabMid = Color(0xFFC084FC),
    fabEnd = Color(0xFF9333EA),
    fabOnContent = Color(0xFF1F0933),
    lockBackdropStart = Color(0xFF130E24),
    lockBackdropEnd = Color(0xFF090712),
    tourCardSurface = Color(0xFF1A1530)
)

// compositionLocalOf enables silky 60fps liquid color morphing across theme switches
val LocalTarazinoColors = compositionLocalOf { ObsidianExtendedColors }

private val ObsidianEmeraldScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF022117),
    primaryContainer = Color(0xFF093B2C),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = GoldAccent,
    onSecondary = Color(0xFF241904),
    secondaryContainer = Color(0xFF3D2E10),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = CyanPrimary,
    onTertiary = Color(0xFF032033),
    tertiaryContainer = Color(0xFF0F3654),
    onTertiaryContainer = Color(0xFFE0F2FE),
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
    inverseSurface = Color(0xFFEBF2F7),
    inverseOnSurface = Color(0xFF0F1724),
    inversePrimary = Color(0xFF046C4E),
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineVariant,
    error = StatusOverdueRed,
    onError = Color(0xFF2E050E),
    errorContainer = Color(0xFF521121),
    onErrorContainer = Color(0xFFFFE4E6),
    scrim = Color(0xB803060B)
)

private val AlabasterLightScheme = lightColorScheme(
    primary = BankEmeraldLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF023827),
    secondary = WarmAmberLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF5C2804),
    tertiary = Color(0xFF026AA2),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF06324D),
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
    inverseSurface = Color(0xFF131D2B),
    inverseOnSurface = Color(0xFFF4F7FA),
    inversePrimary = Color(0xFF34D399),
    outline = AlabasterOutline,
    outlineVariant = AlabasterOutlineVariant,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    scrim = Color(0x7A09111E)
)

private val PersianTurquoiseLightScheme = lightColorScheme(
    primary = PersianTurquoisePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF05334F),
    secondary = PersianTurquoiseSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF5C2804),
    tertiary = PersianTurquoiseTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E7FF),
    onTertiaryContainer = Color(0xFF1E1B4B),
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
    inverseSurface = Color(0xFF0F2030),
    inverseOnSurface = Color(0xFFF2F7FA),
    inversePrimary = Color(0xFF38BDF8),
    outline = TurquoiseOutline,
    outlineVariant = TurquoiseOutlineVariant,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    scrim = Color(0x7A071420)
)

private val DeepSapphireScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color(0xFF031B2E),
    primaryContainer = Color(0xFF113561),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = SapphireViolet,
    onSecondary = Color(0xFF151238),
    secondaryContainer = Color(0xFF2A245E),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = SapphireGold,
    onTertiary = Color(0xFF241904),
    tertiaryContainer = Color(0xFF3E2F10),
    onTertiaryContainer = Color(0xFFFEF3C7),
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
    inverseSurface = Color(0xFFEAF0FA),
    inverseOnSurface = Color(0xFF101A36),
    inversePrimary = Color(0xFF0284C7),
    outline = SapphireOutline,
    outlineVariant = SapphireOutlineVariant,
    error = StatusOverdueRed,
    onError = Color(0xFF2E050E),
    errorContainer = Color(0xFF521121),
    onErrorContainer = Color(0xFFFFE4E6),
    scrim = Color(0xB8030612)
)

private val MidnightGoldScheme = darkColorScheme(
    primary = SovereignGoldPrimary,
    onPrimary = Color(0xFF211604),
    primaryContainer = Color(0xFF423212),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = SovereignGoldSecondary,
    onSecondary = Color(0xFF261305),
    secondaryContainer = Color(0xFF452712),
    onSecondaryContainer = Color(0xFFFFEDD5),
    tertiary = SovereignEmeraldAccent,
    onTertiary = Color(0xFF022117),
    tertiaryContainer = Color(0xFF0B3B2C),
    onTertiaryContainer = Color(0xFFD1FAE5),
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
    inverseSurface = Color(0xFFF3EFEA),
    inverseOnSurface = Color(0xFF16161C),
    inversePrimary = Color(0xFF9A6B12),
    outline = MidnightGoldOutline,
    outlineVariant = MidnightGoldOutlineVariant,
    error = StatusOverdueRed,
    onError = Color(0xFF2E050E),
    errorContainer = Color(0xFF521121),
    onErrorContainer = Color(0xFFFFE4E6),
    scrim = Color(0xB8050507)
)

private val VelvetAmethystScheme = darkColorScheme(
    primary = AmethystPrimary,
    onPrimary = Color(0xFF1F0933),
    primaryContainer = Color(0xFF3E225E),
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = AmethystSecondary,
    onSecondary = Color(0xFF2E081C),
    secondaryContainer = Color(0xFF521837),
    onSecondaryContainer = Color(0xFFFCE7F3),
    tertiary = AmethystGold,
    onTertiary = Color(0xFF241904),
    tertiaryContainer = Color(0xFF3E2F10),
    onTertiaryContainer = Color(0xFFFEF3C7),
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
    inverseSurface = Color(0xFFF2ECFA),
    inverseOnSurface = Color(0xFF17132B),
    inversePrimary = Color(0xFF7E22CE),
    outline = AmethystOutline,
    outlineVariant = AmethystOutlineVariant,
    error = StatusOverdueRed,
    onError = Color(0xFF2E050E),
    errorContainer = Color(0xFF521121),
    onErrorContainer = Color(0xFFFFE4E6),
    scrim = Color(0xB805030A)
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
        LocalTarazinoColors provides animatedExtendedColors,
        LocalContentColor provides animatedColorScheme.onSurface
    ) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            typography = typography
        ) {
            CompositionLocalProvider(LocalContentColor provides animatedColorScheme.onSurface) {
                // CRITICAL: ProvideTextStyle ensures LocalTextStyle defaults to Estedad-FD across
                // every OutlinedTextField, Button, FilterChip, Dialog, and Text without explicit style!
                ProvideTextStyle(value = typography.bodyMedium) {
                    content()
                }
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
