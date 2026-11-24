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
    moodTagFa = "وقار مالی فیروزه تیره ایرانی با طلای ملایم شامپاینی",
    auroraPrimary = Color(0x1835B8AB),
    auroraSecondary = Color(0x14DAB978),
    auroraTertiary = Color(0x104DA3B8),
    cardBorderTop = Color(0x3362C7BC),
    cardBorderBottom = Color(0x14DAB978),
    cardGlassTop = Color(0xFF132A34),
    cardGlassBottom = Color(0xFF0C1D24),
    glassSurface = Color(0xF50F232B),
    dockSurface = Color(0xF70B1A20),
    elevatedSurface = ObsidianSurfaceHigh,
    shimmerHighlight = Color(0x26DAB978),
    positive = StatusPaidGreen,
    onPositive = Color(0xFF04221F),
    warning = StatusUpcomingAmber,
    danger = StatusOverdueRed,
    info = StatusPartialBlue,
    heroStart = Color(0xFF0E3B3E),
    heroMid = Color(0xFF0B2B32),
    heroEnd = Color(0xFF081B22),
    heroAccent = GoldAccent,
    heroOnSurface = Color(0xFFF3F8F9),
    heroOnSurfaceMuted = Color(0xFFB5D2D8),
    miniCardStart = Color(0xFF16303B),
    miniCardEnd = Color(0xFF0E2129),
    miniCardOnSurface = Color(0xFFF3F8F9),
    miniCardOnSurfaceMuted = Color(0xFFA8C5CC),
    fabStart = Color(0xFF4CC9BC),
    fabMid = Color(0xFF35B8AB),
    fabEnd = Color(0xFF1F8C81),
    fabOnContent = Color(0xFF04221F),
    lockBackdropStart = Color(0xFF0B2027),
    lockBackdropEnd = Color(0xFF071317),
    tourCardSurface = Color(0xFF122831)
)

private val AlabasterExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.ALABASTER_LIGHT,
    isDark = false,
    moodTagFa = "شفافیت بانکداری روز با فیروزه عمیق و رگه‌های طلای گرم",
    auroraPrimary = Color(0x120F6B66),
    auroraSecondary = Color(0x10B58538),
    auroraTertiary = Color(0x0C227385),
    cardBorderTop = Color(0xFFD2E3E1),
    cardBorderBottom = Color(0xFFC2D6D4),
    cardGlassTop = Color(0xFFFFFFFF),
    cardGlassBottom = Color(0xFFF9FCFB),
    glassSurface = Color(0xFCFFFFFF),
    dockSurface = Color(0xFAFFFFFF),
    elevatedSurface = AlabasterSurfaceHigh,
    shimmerHighlight = Color(0x20B58538),
    positive = BankEmeraldLightPrimary,
    onPositive = Color.White,
    warning = WarmAmberLight,
    danger = Color(0xFFC44D4D),
    info = Color(0xFF1F6E80),
    heroStart = Color(0xFF0D4F4C),
    heroMid = Color(0xFF0B3B3D),
    heroEnd = Color(0xFF09262B),
    heroAccent = Color(0xFFE3C485),
    heroOnSurface = Color(0xFFF7FBFA),
    heroOnSurfaceMuted = Color(0xFFC7E0DD),
    miniCardStart = Color(0xFFFFFFFF),
    miniCardEnd = Color(0xFFEEF5F4),
    miniCardOnSurface = Color(0xFF0A1D22),
    miniCardOnSurfaceMuted = Color(0xFF3F5C63),
    fabStart = Color(0xFF17857E),
    fabMid = Color(0xFF0F6B66),
    fabEnd = Color(0xFF0A524E),
    fabOnContent = Color.White,
    lockBackdropStart = Color(0xFFE6F0EF),
    lockBackdropEnd = Color(0xFFF4F7F6),
    tourCardSurface = Color(0xFFFFFFFF)
)

private val TurquoiseLightExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.PERSIAN_TURQUOISE_LIGHT,
    isDark = false,
    moodTagFa = "اصالت فیروزه نیشابور و طلای کهربایی روی بوم روشن",
    auroraPrimary = Color(0x120D6E7A),
    auroraSecondary = Color(0x10B58538),
    auroraTertiary = Color(0x0C2B8379),
    cardBorderTop = Color(0xFFCCE0E5),
    cardBorderBottom = Color(0xFFBAD2D8),
    cardGlassTop = Color(0xFFFFFFFF),
    cardGlassBottom = Color(0xFFF8FCFD),
    glassSurface = Color(0xFCFFFFFF),
    dockSurface = Color(0xFAFFFFFF),
    elevatedSurface = TurquoiseSurfaceHigh,
    shimmerHighlight = Color(0x200D6E7A),
    positive = Color(0xFF0F6B66),
    onPositive = Color.White,
    warning = Color(0xFFB58538),
    danger = Color(0xFFC44D4D),
    info = PersianTurquoisePrimary,
    heroStart = Color(0xFF0D525E),
    heroMid = Color(0xFF0A3D4A),
    heroEnd = Color(0xFF082630),
    heroAccent = Color(0xFFE3C485),
    heroOnSurface = Color(0xFFF6FBFC),
    heroOnSurfaceMuted = Color(0xFFC4DFE5),
    miniCardStart = Color(0xFFFFFFFF),
    miniCardEnd = Color(0xFFECF4F6),
    miniCardOnSurface = Color(0xFF081B22),
    miniCardOnSurfaceMuted = Color(0xFF3A5A64),
    fabStart = Color(0xFF158594),
    fabMid = Color(0xFF0D6E7A),
    fabEnd = Color(0xFF09535C),
    fabOnContent = Color.White,
    lockBackdropStart = Color(0xFFE4F0F2),
    lockBackdropEnd = Color(0xFFF0F6F7),
    tourCardSurface = Color(0xFFFFFFFF)
)

private val SapphireExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.DEEP_SAPPHIRE,
    isDark = true,
    moodTagFa = "ساختار رسمی سرمه‌ای-فیروزه‌ای خزر با طلای ملایم",
    auroraPrimary = Color(0x1847B0C4),
    auroraSecondary = Color(0x14DAB978),
    auroraTertiary = Color(0x1035B8AB),
    cardBorderTop = Color(0x3366C3D4),
    cardBorderBottom = Color(0x14DAB978),
    cardGlassTop = Color(0xFF142A3D),
    cardGlassBottom = Color(0xFF0D1D2B),
    glassSurface = Color(0xF5102333),
    dockSurface = Color(0xF70B1925),
    elevatedSurface = SapphireSurfaceHigh,
    shimmerHighlight = Color(0x2647B0C4),
    positive = Color(0xFF35B8AB),
    onPositive = Color(0xFF04221F),
    warning = Color(0xFFDAB978),
    danger = StatusOverdueRed,
    info = CyanPrimary,
    heroStart = Color(0xFF10384F),
    heroMid = Color(0xFF0C283B),
    heroEnd = Color(0xFF081926),
    heroAccent = Color(0xFFDAB978),
    heroOnSurface = Color(0xFFF3F8FC),
    heroOnSurfaceMuted = Color(0xFFB5CEE0),
    miniCardStart = Color(0xFF173045),
    miniCardEnd = Color(0xFF0F2130),
    miniCardOnSurface = Color(0xFFF3F8FC),
    miniCardOnSurfaceMuted = Color(0xFFA9C3D4),
    fabStart = Color(0xFF66C3D4),
    fabMid = Color(0xFF47B0C4),
    fabEnd = Color(0xFF28899C),
    fabOnContent = Color(0xFF041D24),
    lockBackdropStart = Color(0xFF0B1E2E),
    lockBackdropEnd = Color(0xFF07111A),
    tourCardSurface = Color(0xFF132738)
)

private val MidnightGoldExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.MIDNIGHT_GOLD,
    isDark = true,
    moodTagFa = "خزانه اختصاصی طلای شامپاینی ملایم و فیروزه تیره",
    auroraPrimary = Color(0x18DAB978),
    auroraSecondary = Color(0x1435B8AB),
    auroraTertiary = Color(0x0EC8A05B),
    cardBorderTop = Color(0x33DAB978),
    cardBorderBottom = Color(0x1435B8AB),
    cardGlassTop = Color(0xFF1C272C),
    cardGlassBottom = Color(0xFF131B1F),
    glassSurface = Color(0xF5172024),
    dockSurface = Color(0xF711181B),
    elevatedSurface = MidnightGoldSurfaceHigh,
    shimmerHighlight = Color(0x26DAB978),
    positive = SovereignEmeraldAccent,
    onPositive = Color(0xFF04221F),
    warning = SovereignGoldPrimary,
    danger = StatusOverdueRed,
    info = Color(0xFF4DA3B8),
    heroStart = Color(0xFF2B2719),
    heroMid = Color(0xFF1A2426),
    heroEnd = Color(0xFF10171A),
    heroAccent = SovereignGoldPrimary,
    heroOnSurface = Color(0xFFF9F7F2),
    heroOnSurfaceMuted = Color(0xFFD4CEBF),
    miniCardStart = Color(0xFF202B30),
    miniCardEnd = Color(0xFF151D21),
    miniCardOnSurface = Color(0xFFF9F7F2),
    miniCardOnSurfaceMuted = Color(0xFFC4BFB2),
    fabStart = Color(0xFFE8CB90),
    fabMid = Color(0xFFDAB978),
    fabEnd = Color(0xFFB38E48),
    fabOnContent = Color(0xFF211805),
    lockBackdropStart = Color(0xFF141D20),
    lockBackdropEnd = Color(0xFF0B1012),
    tourCardSurface = Color(0xFF192328)
)

private val VelvetAmethystExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.VELVET_AMETHYST,
    isDark = true,
    moodTagFa = "شفق آرام فیروزه‌ای-سرمه‌ای با طلای عتیقه ملایم",
    auroraPrimary = Color(0x185AC2B8),
    auroraSecondary = Color(0x14DAB978),
    auroraTertiary = Color(0x107CAEC4),
    cardBorderTop = Color(0x336AC9BF),
    cardBorderBottom = Color(0x14DAB978),
    cardGlassTop = Color(0xFF1B2536),
    cardGlassBottom = Color(0xFF121A26),
    glassSurface = Color(0xF517202E),
    dockSurface = Color(0xF7111823),
    elevatedSurface = AmethystSurfaceHigh,
    shimmerHighlight = Color(0x26DAB978),
    positive = Color(0xFF35B8AB),
    onPositive = Color(0xFF04221F),
    warning = Color(0xFFDAB978),
    danger = StatusOverdueRed,
    info = Color(0xFF4DA3B8),
    heroStart = Color(0xFF173642),
    heroMid = Color(0xFF142738),
    heroEnd = Color(0xFF0E1724),
    heroAccent = Color(0xFFDAB978),
    heroOnSurface = Color(0xFFF5F8FA),
    heroOnSurfaceMuted = Color(0xFFC0D0DE),
    miniCardStart = Color(0xFF1F2A3D),
    miniCardEnd = Color(0xFF141C29),
    miniCardOnSurface = Color(0xFFF5F8FA),
    miniCardOnSurfaceMuted = Color(0xFFB4C5D4),
    fabStart = Color(0xFF72D1C8),
    fabMid = Color(0xFF5AC2B8),
    fabEnd = Color(0xFF35968D),
    fabOnContent = Color(0xFF04221F),
    lockBackdropStart = Color(0xFF121C29),
    lockBackdropEnd = Color(0xFF0B1017),
    tourCardSurface = Color(0xFF182230)
)

private val HighContrastNightExtendedColors = TarazinoExtendedColors(
    themeMode = TarazinoThemeMode.HIGH_CONTRAST_NIGHT_DARK,
    isDark = true,
    moodTagFa = "تاریک پُرکنتراست شبانه — خوانایی حداکثری اعداد مالی در تاریکی شب",
    auroraPrimary = Color(0x1445E6D5),
    auroraSecondary = Color(0x10F5D28A),
    auroraTertiary = Color(0x0A70D3EE),
    cardBorderTop = Color(0x6645E6D5),
    cardBorderBottom = Color(0x3DF5D28A),
    cardGlassTop = Color(0xFF0D161A),
    cardGlassBottom = Color(0xFF070C0E),
    glassSurface = Color(0xFA0A1114),
    dockSurface = Color(0xFA060B0D),
    elevatedSurface = HighContrastNightSurfaceHigh,
    shimmerHighlight = Color(0x3345E6D5),
    positive = HighContrastNightPrimary,
    onPositive = Color(0xFF021412),
    warning = HighContrastNightSecondary,
    danger = HighContrastNightDanger,
    info = HighContrastNightTertiary,
    heroStart = Color(0xFF082728),
    heroMid = Color(0xFF05181B),
    heroEnd = Color(0xFF02090B),
    heroAccent = HighContrastNightSecondary,
    heroOnSurface = HighContrastNightOnBg,
    heroOnSurfaceMuted = HighContrastNightOnSurfaceVariant,
    miniCardStart = Color(0xFF0E1A1F),
    miniCardEnd = Color(0xFF081014),
    miniCardOnSurface = HighContrastNightOnBg,
    miniCardOnSurfaceMuted = HighContrastNightOnSurfaceVariant,
    fabStart = Color(0xFF6CF2E4),
    fabMid = HighContrastNightPrimary,
    fabEnd = Color(0xFF1FA89A),
    fabOnContent = Color(0xFF021412),
    lockBackdropStart = Color(0xFF050D10),
    lockBackdropEnd = HighContrastNightBg,
    tourCardSurface = Color(0xFF0C1519)
)

// compositionLocalOf enables silky 60fps liquid color morphing across theme switches
val LocalTarazinoColors = compositionLocalOf { ObsidianExtendedColors }

private val ObsidianEmeraldScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF04221F),
    primaryContainer = Color(0xFF0F3D39),
    onPrimaryContainer = Color(0xFFC8F2ED),
    secondary = GoldAccent,
    onSecondary = Color(0xFF241A05),
    secondaryContainer = Color(0xFF3D3014),
    onSecondaryContainer = Color(0xFFF7E7C4),
    tertiary = StatusPartialBlue,
    onTertiary = Color(0xFF052129),
    tertiaryContainer = Color(0xFF133A47),
    onTertiaryContainer = Color(0xFFD3EEF5),
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
    inverseSurface = Color(0xFFEAF3F4),
    inverseOnSurface = Color(0xFF0C1D24),
    inversePrimary = Color(0xFF0F6B66),
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineVariant,
    error = StatusOverdueRed,
    onError = Color(0xFF2B0808),
    errorContainer = Color(0xFF4D1818),
    onErrorContainer = Color(0xFFFADCDC),
    scrim = Color(0xB8030A0D)
)

private val HighContrastNightDarkScheme = darkColorScheme(
    primary = HighContrastNightPrimary,
    onPrimary = Color(0xFF021412),
    primaryContainer = Color(0xFF0B3D39),
    onPrimaryContainer = Color(0xFFE0FFF9),
    secondary = HighContrastNightSecondary,
    onSecondary = Color(0xFF1A1202),
    secondaryContainer = Color(0xFF3E300F),
    onSecondaryContainer = Color(0xFFFFF4D6),
    tertiary = HighContrastNightTertiary,
    onTertiary = Color(0xFF021820),
    tertiaryContainer = Color(0xFF0C3847),
    onTertiaryContainer = Color(0xFFE2F8FF),
    background = HighContrastNightBg,
    onBackground = HighContrastNightOnBg,
    surface = HighContrastNightSurface,
    onSurface = HighContrastNightOnBg,
    surfaceVariant = HighContrastNightSurfaceVariant,
    onSurfaceVariant = HighContrastNightOnSurfaceVariant,
    surfaceContainerLowest = HighContrastNightSurfaceLowest,
    surfaceContainerLow = HighContrastNightSurface,
    surfaceContainer = HighContrastNightSurface,
    surfaceContainerHigh = HighContrastNightSurfaceHigh,
    surfaceContainerHighest = HighContrastNightSurfaceHighest,
    inverseSurface = Color(0xFFF4F8F9),
    inverseOnSurface = Color(0xFF040B0D),
    inversePrimary = Color(0xFF0B5C57),
    outline = HighContrastNightOutline,
    outlineVariant = HighContrastNightOutlineVariant,
    error = HighContrastNightDanger,
    onError = Color(0xFF210404),
    errorContainer = Color(0xFF5C1515),
    onErrorContainer = Color(0xFFFFE5E5),
    scrim = Color(0xD9000000)
)

private val AlabasterLightScheme = lightColorScheme(
    primary = BankEmeraldLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2F0ED),
    onPrimaryContainer = Color(0xFF053330),
    secondary = WarmAmberLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7EBCD),
    onSecondaryContainer = Color(0xFF4A330B),
    tertiary = Color(0xFF1F6E80),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD7EEF2),
    onTertiaryContainer = Color(0xFF07323B),
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
    inverseSurface = Color(0xFF10242B),
    inverseOnSurface = Color(0xFFF2F7F7),
    inversePrimary = Color(0xFF53C9BD),
    outline = AlabasterOutline,
    outlineVariant = AlabasterOutlineVariant,
    error = Color(0xFFC44D4D),
    onError = Color.White,
    errorContainer = Color(0xFFFCE3E3),
    onErrorContainer = Color(0xFF6E1D1D),
    scrim = Color(0x7A07151A)
)

private val PersianTurquoiseLightScheme = lightColorScheme(
    primary = PersianTurquoisePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4EEF2),
    onPrimaryContainer = Color(0xFF05333B),
    secondary = PersianTurquoiseSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7EBCD),
    onSecondaryContainer = Color(0xFF4A330B),
    tertiary = PersianTurquoiseTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD4F0EC),
    onTertiaryContainer = Color(0xFF063631),
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
    inverseSurface = Color(0xFF0E232B),
    inverseOnSurface = Color(0xFFF1F7F8),
    inversePrimary = Color(0xFF59C3D1),
    outline = TurquoiseOutline,
    outlineVariant = TurquoiseOutlineVariant,
    error = Color(0xFFC44D4D),
    onError = Color.White,
    errorContainer = Color(0xFFFCE3E3),
    onErrorContainer = Color(0xFF6E1D1D),
    scrim = Color(0x7A06141A)
)

private val DeepSapphireScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color(0xFF041F26),
    primaryContainer = Color(0xFF123B47),
    onPrimaryContainer = Color(0xFFD2EFF5),
    secondary = SapphireViolet,
    onSecondary = Color(0xFF241A05),
    secondaryContainer = Color(0xFF3D3014),
    onSecondaryContainer = Color(0xFFF7E7C4),
    tertiary = SapphireGold,
    onTertiary = Color(0xFF04221F),
    tertiaryContainer = Color(0xFF0F3D39),
    onTertiaryContainer = Color(0xFFC8F2ED),
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
    inverseSurface = Color(0xFFEAF2F7),
    inverseOnSurface = Color(0xFF102333),
    inversePrimary = Color(0xFF157587),
    outline = SapphireOutline,
    outlineVariant = SapphireOutlineVariant,
    error = StatusOverdueRed,
    onError = Color(0xFF2B0808),
    errorContainer = Color(0xFF4D1818),
    onErrorContainer = Color(0xFFFADCDC),
    scrim = Color(0xB8040B12)
)

private val MidnightGoldScheme = darkColorScheme(
    primary = SovereignGoldPrimary,
    onPrimary = Color(0xFF241A05),
    primaryContainer = Color(0xFF403214),
    onPrimaryContainer = Color(0xFFF7E7C4),
    secondary = SovereignEmeraldAccent,
    onSecondary = Color(0xFF04221F),
    secondaryContainer = Color(0xFF0F3D39),
    onSecondaryContainer = Color(0xFFC8F2ED),
    tertiary = SovereignGoldSecondary,
    onTertiary = Color(0xFF241805),
    tertiaryContainer = Color(0xFF422F13),
    onTertiaryContainer = Color(0xFFF7E4C4),
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
    inverseSurface = Color(0xFFF2EFE9),
    inverseOnSurface = Color(0xFF172024),
    inversePrimary = Color(0xFF8C6B28),
    outline = MidnightGoldOutline,
    outlineVariant = MidnightGoldOutlineVariant,
    error = StatusOverdueRed,
    onError = Color(0xFF2B0808),
    errorContainer = Color(0xFF4D1818),
    onErrorContainer = Color(0xFFFADCDC),
    scrim = Color(0xB805080A)
)

private val VelvetAmethystScheme = darkColorScheme(
    primary = AmethystPrimary,
    onPrimary = Color(0xFF04221F),
    primaryContainer = Color(0xFF123D39),
    onPrimaryContainer = Color(0xFFCCF2EE),
    secondary = AmethystSecondary,
    onSecondary = Color(0xFF241A05),
    secondaryContainer = Color(0xFF3D3014),
    onSecondaryContainer = Color(0xFFF7E7C4),
    tertiary = AmethystGold,
    onTertiary = Color(0xFF07202B),
    tertiaryContainer = Color(0xFF173A4A),
    onTertiaryContainer = Color(0xFFD7ECF5),
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
    inverseSurface = Color(0xFFECF2F7),
    inverseOnSurface = Color(0xFF17202E),
    inversePrimary = Color(0xFF16756D),
    outline = AmethystOutline,
    outlineVariant = AmethystOutlineVariant,
    error = StatusOverdueRed,
    onError = Color(0xFF2B0808),
    errorContainer = Color(0xFF4D1818),
    onErrorContainer = Color(0xFFFADCDC),
    scrim = Color(0xB805080E)
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
        TarazinoThemeMode.HIGH_CONTRAST_NIGHT_DARK -> HighContrastNightDarkScheme
        TarazinoThemeMode.ALABASTER_LIGHT -> AlabasterLightScheme
        TarazinoThemeMode.PERSIAN_TURQUOISE_LIGHT -> PersianTurquoiseLightScheme
        TarazinoThemeMode.DEEP_SAPPHIRE -> DeepSapphireScheme
        TarazinoThemeMode.MIDNIGHT_GOLD -> MidnightGoldScheme
        TarazinoThemeMode.VELVET_AMETHYST -> VelvetAmethystScheme
    }

    val targetExtendedColors = when (themeMode) {
        TarazinoThemeMode.OBSIDIAN_EMERALD_DARK -> ObsidianExtendedColors
        TarazinoThemeMode.HIGH_CONTRAST_NIGHT_DARK -> HighContrastNightExtendedColors
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
