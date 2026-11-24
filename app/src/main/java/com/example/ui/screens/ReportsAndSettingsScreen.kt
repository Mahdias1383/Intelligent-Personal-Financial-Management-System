package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TableChart
import com.example.core.JalaliCalendarEngine
import com.example.notification.GoalThresholdNotificationEvaluator
import com.example.notification.GoalThresholdNotificationManager
import com.example.notification.ShamsiInstallmentNotificationManager
import com.example.notification.ShamsiInstallmentReminderEvaluator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.BiometricAuthResult
import com.example.core.BiometricPromptAuthenticator
import com.example.core.CurrencyUnit
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.FontScalePreset
import com.example.data.TarazinoThemeMode
import com.example.domain.DebtCategoryTag
import com.example.export.A4ReportPayload
import com.example.export.ReportExportEngine
import com.example.ui.TarazinoUiState
import com.example.ui.components.CounterpartyMonogramAvatar
import com.example.ui.components.DetailedCategorySpendingReportSection
import com.example.ui.components.PrintableFinancialSummaryPdfStudioCard
import com.example.ui.components.TarazinoAutoSizeText
import com.example.ui.components.TarazinoGlassCard
import com.example.ui.components.TarazinoSectionHeader
import com.example.ui.components.TarazinoSegmentedControl
import com.example.ui.theme.ThemeSwitcherEngine
import com.example.ui.components.TarazinoStatusPill
import com.example.ui.components.tarazinoPressScale
import com.example.ui.components.tourSpotlightTarget
import android.content.Intent
import com.example.ui.theme.LocalTarazinoColors
import com.example.widget.QuickLogWidgetActivity
import com.example.widget.TarazinoQuickLogWidgetProvider
import com.example.widget.WidgetQuickLogMode

@Composable
fun ReportsAndSettingsScreen(
    uiState: TarazinoUiState,
    onOpenFullA4Report: () -> Unit,
    onOpenCounterpartyA4Report: (CounterpartyEntity) -> Unit,
    onDirectExportPdf: () -> Unit,
    onDirectExportExcel: () -> Unit,
    onDirectExportCsv: () -> Unit,
    onSetThemeMode: (TarazinoThemeMode) -> Unit,
    onSetCurrencyUnit: (CurrencyUnit) -> Unit,
    onSetFontScale: (FontScalePreset) -> Unit,
    onConfigureSecurityPin: (String, Int, Boolean, String, String) -> Unit,
    onRemoveSecurityPin: () -> Unit,
    onLockAppNow: () -> Unit,
    onStartGuidedTour: () -> Unit,
    onClearAllDataToZero: () -> Unit,
    onRestoreDemoSeedData: () -> Unit,
    onExportBackupJson: ((String) -> Unit) -> Unit,
    onImportBackupJson: (String) -> Unit,
    onShowToast: (String) -> Unit,
    onOpenCustomA4ReportPayload: (A4ReportPayload) -> Unit = {},
    tourScrollIndex: Int? = null
) {
    val context = LocalContext.current
    val ext = LocalTarazinoColors.current
    val listState = rememberLazyListState()
    var activeSectionIndex by remember { mutableIntStateOf(0) } // 0=Reports, 1=Theme&Security, 2=Backup&Data
    var backupJsonDialogContent by remember { mutableStateOf<String?>(null) }
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }

    val sectionTabs = listOf(
        "چاپ A4 و اکسل",
        "پوسته و امنیت",
        "پشتیبان و داده"
    )

    LaunchedEffect(tourScrollIndex) {
        if (tourScrollIndex != null && tourScrollIndex >= 0) {
            activeSectionIndex = 0
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_and_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 3-Column Symmetrical Segmented Section Selector
        item {
            TarazinoSegmentedControl(
                options = sectionTabs,
                selectedIndex = activeSectionIndex,
                onSelectIndex = { activeSectionIndex = it },
                testTagPrefix = "settings_subtab"
            )
        }

        // =========================================================================
        // SECTION 0: A4 PDF & RTL EXCEL EXPORT STUDIO
        // =========================================================================
        if (activeSectionIndex == 0) {
            item {
                PrintableFinancialSummaryPdfStudioCard(
                    obligations = uiState.obligations,
                    installments = uiState.installments,
                    counterparties = uiState.counterparties,
                    preferences = uiState.preferences,
                    onOpenA4PreviewPayload = onOpenCustomA4ReportPayload,
                    onShowToast = onShowToast
                )
            }

            item {
                TarazinoGlassCard(
                    modifier = Modifier
                        .tourSpotlightTarget("reports_a4_export_studio")
                        .testTag("reports_a4_export_studio_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TarazinoSectionHeader(
                            title = "استودیو گزارش‌گیری و چاپ رسمی A4",
                            subtitle = "برگه رنگی A4 استاندارد، کادر مهر و امضا و اکسل راست‌به‌چپ",
                            icon = Icons.Default.PictureAsPdf,
                            accentColor = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Primary Live A4 Preview & Print Button
                        Button(
                            onClick = onOpenFullA4Report,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_full_a4_preview_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TarazinoAutoSizeText(
                                text = "پیش‌نمایش زنده و چاپ گزارش جامع A4 رنگی",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                minFontSize = 10.sp,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3 Symmetrical Rectangular Export Action Tiles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ExportFormatTileCard(
                                titleFa = "فایل PDF",
                                subtitleFa = "استاندارد A4",
                                icon = Icons.Default.PictureAsPdf,
                                accentColor = ext.positive,
                                onClick = onDirectExportPdf,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("direct_export_pdf_btn")
                            )
                            ExportFormatTileCard(
                                titleFa = "فایل اکسل",
                                subtitleFa = "راست‌به‌چپ .xls",
                                icon = Icons.Default.TableChart,
                                accentColor = ext.info,
                                onClick = onDirectExportExcel,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("direct_export_excel_btn")
                            )
                            ExportFormatTileCard(
                                titleFa = "فایل CSV",
                                subtitleFa = "UTF-8 BOM",
                                icon = Icons.Default.Description,
                                accentColor = ext.warning,
                                onClick = onDirectExportCsv,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("direct_export_csv_btn")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "خروجی اختصاصی صورتحساب A4 به تفکیک هر زیرمجموعه / طرف‌حساب:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        uiState.counterparties.forEach { cp ->
                            val accent = Color(cp.categoryEnum.defaultColorHex)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.68f else 0.88f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(0.85.dp, ext.cardBorderTop),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        CounterpartyMonogramAvatar(
                                            name = cp.name,
                                            color = accent,
                                            size = 38.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            TarazinoAutoSizeText(
                                                text = cp.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                minFontSize = 10.5.sp
                                            )
                                            TarazinoAutoSizeText(
                                                text = cp.categoryEnum.badgeFa,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                minFontSize = 8.5.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Surface(
                                        shape = RoundedCornerShape(11.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (ext.isDark) 0.15f else 0.10f),
                                        border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(11.dp))
                                            .tarazinoPressScale(scaleDown = 0.94f) {
                                                onOpenCounterpartyA4Report(cp)
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PictureAsPdf,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "صورتحساب A4",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                DetailedCategorySpendingReportSection(
                    obligations = uiState.obligations,
                    installments = uiState.installments,
                    counterparties = uiState.counterparties,
                    currencyUnit = uiState.preferences.currencyUnit,
                    privacyMasked = uiState.preferences.privacyMaskEnabled,
                    onOpenCategoryFilteredA4Report = { categoryTag, obIds ->
                        val filteredObs = if (categoryTag == DebtCategoryTag.ALL) {
                            uiState.obligations
                        } else {
                            uiState.obligations.filter { it.id in obIds.toSet() }
                        }
                        val payload = ReportExportEngine.buildReportPayload(
                            reportTitleFa = "گزارش تفصیلی دسته: ${categoryTag.labelFa}",
                            subtitleFa = "${categoryTag.descriptionFa} • تفکیک اقساط و مانده تعهدات",
                            obligations = filteredObs,
                            installments = uiState.installments,
                            counterparties = uiState.counterparties,
                            currencyUnit = uiState.preferences.currencyUnit
                        )
                        onOpenCustomA4ReportPayload(payload)
                    },
                    onExportCategoryFilteredPdf = { categoryTag, obIds ->
                        val filteredObs = if (categoryTag == DebtCategoryTag.ALL) {
                            uiState.obligations
                        } else {
                            uiState.obligations.filter { it.id in obIds.toSet() }
                        }
                        val payload = ReportExportEngine.buildReportPayload(
                            reportTitleFa = "گزارش تفصیلی دسته: ${categoryTag.labelFa}",
                            subtitleFa = categoryTag.descriptionFa,
                            obligations = filteredObs,
                            installments = uiState.installments,
                            counterparties = uiState.counterparties,
                            currencyUnit = uiState.preferences.currencyUnit
                        )
                        val file = ReportExportEngine.saveAndSharePdfFile(context, payload)
                        onShowToast(
                            PersianCurrencyEngine.formatRtlBidiSafe(
                                "فایل PDF دسته «${categoryTag.labelFa}» (${file.name}) آماده شد"
                            )
                        )
                    },
                    onExportCategoryFilteredExcel = { categoryTag, obIds ->
                        val filteredObs = if (categoryTag == DebtCategoryTag.ALL) {
                            uiState.obligations
                        } else {
                            uiState.obligations.filter { it.id in obIds.toSet() }
                        }
                        val payload = ReportExportEngine.buildReportPayload(
                            reportTitleFa = "گزارش تفصیلی دسته: ${categoryTag.labelFa}",
                            subtitleFa = categoryTag.descriptionFa,
                            obligations = filteredObs,
                            installments = uiState.installments,
                            counterparties = uiState.counterparties,
                            currencyUnit = uiState.preferences.currencyUnit
                        )
                        val file = ReportExportEngine.saveAndShareRtlExcelFile(context, payload)
                        onShowToast("فایل اکسل دسته «${categoryTag.labelFa}» (${file.name}) تولید شد")
                    }
                )
            }
        }

        // =========================================================================
        // SECTION 1: VISUAL THEME SWATCHES, DISPLAY & SECURITY VAULT
        // =========================================================================
        if (activeSectionIndex == 1) {
            item {
                TarazinoGlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TarazinoSectionHeader(
                            title = "شخصی‌سازی پوسته، واحد پول و نمایش",
                            subtitle = "تنظیم ظاهر مدرن، مقیاس فونت فارسی و بازپخش تور آموزشی",
                            icon = Icons.Default.Palette,
                            accentColor = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dedicated Quick Theme Switcher: Deep Teal Light <-> High-Contrast Late-Night Dark
                        val isDeepTealLightSelected =
                            uiState.preferences.themeMode == ThemeSwitcherEngine.DEEP_TEAL_LIGHT_MODE
                        val isHighContrastNightSelected =
                            ThemeSwitcherEngine.isHighContrastNightMode(uiState.preferences.themeMode)

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (ext.isDark) 0.55f else 0.72f),
                            border = BorderStroke(
                                1.dp,
                                if (isHighContrastNightSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Contrast,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "تغییر سریع حالت روز و شبانه پُرکنتراست",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isHighContrastNightSelected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                        } else {
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                                        },
                                        border = BorderStroke(
                                            0.75.dp,
                                            if (isHighContrastNightSelected) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                            } else {
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.40f)
                                            }
                                        )
                                    ) {
                                        Text(
                                            text = if (isHighContrastNightSelected) "WCAG AAA" else "روزانه",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isHighContrastNightSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.secondary
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (isHighContrastNightSelected) {
                                        "حالت تاریک پُرکنتراست شبانه فعال است؛ بهینه‌شده برای مدیریت مالی در شب با کنتراست حداکثری و محافظت از چشم."
                                    } else {
                                        "جابه‌جایی آنی بین پوسته روشن فیروزه تیره و حالت تاریک پُرکنتراست شبانه (همچنین از نوار بالای صفحه در دسترس است)."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val lightCardBg by animateColorAsState(
                                        targetValue = if (isDeepTealLightSelected) {
                                            Color(0xFF0F6B66)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        animationSpec = tween(260),
                                        label = "QuickSwitchLightBg"
                                    )
                                    val darkCardBg by animateColorAsState(
                                        targetValue = if (isHighContrastNightSelected) {
                                            Color(0xFF020506)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        animationSpec = tween(260),
                                        label = "QuickSwitchDarkBg"
                                    )

                                    // Deep Teal Light Mode Option
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = lightCardBg,
                                        border = BorderStroke(
                                            width = if (isDeepTealLightSelected) 1.6.dp else 1.dp,
                                            color = if (isDeepTealLightSelected) {
                                                Color(0xFF35B8AB)
                                            } else {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            }
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 56.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                onSetThemeMode(ThemeSwitcherEngine.DEEP_TEAL_LIGHT_MODE)
                                            }
                                            .testTag("settings_theme_switch_light")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isDeepTealLightSelected) {
                                                            Color.White.copy(alpha = 0.18f)
                                                        } else {
                                                            Color(0xFF0F6B66).copy(alpha = 0.14f)
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LightMode,
                                                    contentDescription = "حالت روشن فیروزه تیره",
                                                    tint = if (isDeepTealLightSelected) {
                                                        Color(0xFFF5D28A)
                                                    } else {
                                                        Color(0xFF0F6B66)
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                TarazinoAutoSizeText(
                                                    text = "فیروزه تیره (روشن)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isDeepTealLightSelected) {
                                                        Color.White
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    },
                                                    maxLines = 1,
                                                    minFontSize = 9.5.sp
                                                )
                                                Text(
                                                    text = "روشنایی متعادل روز",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isDeepTealLightSelected) {
                                                        Color(0xFFE0F2F1)
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    // High-Contrast Late-Night Dark Mode Option
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = darkCardBg,
                                        border = BorderStroke(
                                            width = if (isHighContrastNightSelected) 1.8.dp else 1.dp,
                                            color = if (isHighContrastNightSelected) {
                                                Color(0xFF45E6D5)
                                            } else {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            }
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 56.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                onSetThemeMode(ThemeSwitcherEngine.HIGH_CONTRAST_DARK_MODE)
                                            }
                                            .testTag("settings_theme_switch_high_contrast_dark")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isHighContrastNightSelected) {
                                                            Color(0xFF45E6D5).copy(alpha = 0.22f)
                                                        } else {
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DarkMode,
                                                    contentDescription = "حالت تاریک پُرکنتراست شبانه",
                                                    tint = if (isHighContrastNightSelected) {
                                                        Color(0xFF45E6D5)
                                                    } else {
                                                        MaterialTheme.colorScheme.primary
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                TarazinoAutoSizeText(
                                                    text = "تاریک پُرکنتراست",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isHighContrastNightSelected) {
                                                        Color.White
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    },
                                                    maxLines = 1,
                                                    minFontSize = 9.5.sp
                                                )
                                                Text(
                                                    text = "شبانه • کنتراست حداکثری",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isHighContrastNightSelected) {
                                                        Color(0xFFF5D28A)
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "استودیوی پوسته‌های ظاهری (۵ تم تیره + ۲ تم روشن):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Interactive Theme Mood Banner (Liquid-Morphs Smoothly on Every Theme Tap!)
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                Brush.linearGradient(
                                    listOf(
                                        ext.heroAccent.copy(alpha = 0.65f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f)
                                    )
                                )
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(ext.heroStart, ext.heroMid, ext.heroEnd)
                                        )
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            TarazinoAutoSizeText(
                                                text = uiState.preferences.themeMode.labelFa,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ext.heroOnSurface,
                                                maxLines = 1,
                                                minFontSize = 10.5.sp,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(999.dp),
                                                color = ext.heroAccent.copy(alpha = 0.22f),
                                                border = BorderStroke(0.75.dp, ext.heroAccent.copy(alpha = 0.55f))
                                            ) {
                                                Text(
                                                    text = if (ext.isDark) "تم تیره" else "تم روشن",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ext.heroAccent,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        AnimatedContent(
                                            targetState = ext.moodTagFa,
                                            transitionSpec = {
                                                fadeIn(tween(260)) togetherWith fadeOut(tween(180))
                                            },
                                            label = "ThemeMoodTagMorph"
                                        ) { moodText ->
                                            Text(
                                                text = moodText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ext.heroOnSurfaceMuted,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Live 3-Token Palette Harmony Swatch Pill
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color.Black.copy(alpha = 0.28f),
                                        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.16f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary,
                                                MaterialTheme.colorScheme.tertiary
                                            ).forEach { swatchColor ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(swatchColor)
                                                        .border(0.8.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Curated Emotional Fintech Theme Gallery (7 Distinct Atmospheres including High-Contrast Night)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ThemePreviewSwatchCard(
                                    mode = TarazinoThemeMode.ALABASTER_LIGHT,
                                    titleFa = "مروارید صبح (فیروزه تیره)",
                                    subtitleFa = "روشن • فیروزه و طلا",
                                    isLightTheme = true,
                                    bgPreview = Color(0xFFF4F7F6),
                                    surfacePreview = Color(0xFFFFFFFF),
                                    heroStartPreview = Color(0xFF0D4F4C),
                                    accentPreview = Color(0xFF0F6B66),
                                    secondaryPreview = Color(0xFFB58538),
                                    selected = uiState.preferences.themeMode == TarazinoThemeMode.ALABASTER_LIGHT,
                                    onClick = { onSetThemeMode(TarazinoThemeMode.ALABASTER_LIGHT) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_chip_${TarazinoThemeMode.ALABASTER_LIGHT.name}")
                                )
                                ThemePreviewSwatchCard(
                                    mode = TarazinoThemeMode.HIGH_CONTRAST_NIGHT_DARK,
                                    titleFa = "تاریک پُرکنتراست شبانه",
                                    subtitleFa = "شبانه • کنتراست حداکثری",
                                    isLightTheme = false,
                                    bgPreview = Color(0xFF020506),
                                    surfacePreview = Color(0xFF0A1114),
                                    heroStartPreview = Color(0xFF041C1E),
                                    accentPreview = Color(0xFF45E6D5),
                                    secondaryPreview = Color(0xFFF5D28A),
                                    selected = uiState.preferences.themeMode == TarazinoThemeMode.HIGH_CONTRAST_NIGHT_DARK,
                                    onClick = { onSetThemeMode(TarazinoThemeMode.HIGH_CONTRAST_NIGHT_DARK) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_chip_${TarazinoThemeMode.HIGH_CONTRAST_NIGHT_DARK.name}")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ThemePreviewSwatchCard(
                                    mode = TarazinoThemeMode.OBSIDIAN_EMERALD_DARK,
                                    titleFa = "زمرد شب",
                                    subtitleFa = "فیروزه تیره • طلای ملایم",
                                    isLightTheme = false,
                                    bgPreview = Color(0xFF071317),
                                    surfacePreview = Color(0xFF132932),
                                    heroStartPreview = Color(0xFF0E3B3E),
                                    accentPreview = Color(0xFF35B8AB),
                                    secondaryPreview = Color(0xFFDAB978),
                                    selected = uiState.preferences.themeMode == TarazinoThemeMode.OBSIDIAN_EMERALD_DARK,
                                    onClick = { onSetThemeMode(TarazinoThemeMode.OBSIDIAN_EMERALD_DARK) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_chip_${TarazinoThemeMode.OBSIDIAN_EMERALD_DARK.name}")
                                )
                                ThemePreviewSwatchCard(
                                    mode = TarazinoThemeMode.PERSIAN_TURQUOISE_LIGHT,
                                    titleFa = "فیروزه بهار",
                                    subtitleFa = "روشن • فیروزه نیشابور",
                                    isLightTheme = true,
                                    bgPreview = Color(0xFFF0F6F7),
                                    surfacePreview = Color(0xFFFFFFFF),
                                    heroStartPreview = Color(0xFF0D525E),
                                    accentPreview = Color(0xFF0D6E7A),
                                    secondaryPreview = Color(0xFFB58538),
                                    selected = uiState.preferences.themeMode == TarazinoThemeMode.PERSIAN_TURQUOISE_LIGHT,
                                    onClick = { onSetThemeMode(TarazinoThemeMode.PERSIAN_TURQUOISE_LIGHT) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_chip_${TarazinoThemeMode.PERSIAN_TURQUOISE_LIGHT.name}")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ThemePreviewSwatchCard(
                                    mode = TarazinoThemeMode.DEEP_SAPPHIRE,
                                    titleFa = "اقیانوس کبود",
                                    subtitleFa = "سرمه‌ای خزر • طلای ملایم",
                                    isLightTheme = false,
                                    bgPreview = Color(0xFF07111A),
                                    surfacePreview = Color(0xFF14293B),
                                    heroStartPreview = Color(0xFF10384F),
                                    accentPreview = Color(0xFF47B0C4),
                                    secondaryPreview = Color(0xFFDAB978),
                                    selected = uiState.preferences.themeMode == TarazinoThemeMode.DEEP_SAPPHIRE,
                                    onClick = { onSetThemeMode(TarazinoThemeMode.DEEP_SAPPHIRE) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_chip_${TarazinoThemeMode.DEEP_SAPPHIRE.name}")
                                )
                                ThemePreviewSwatchCard(
                                    mode = TarazinoThemeMode.MIDNIGHT_GOLD,
                                    titleFa = "شبِ طلایی",
                                    subtitleFa = "خزانه تیره • طلای شامپاینی",
                                    isLightTheme = false,
                                    bgPreview = Color(0xFF0B1012),
                                    surfacePreview = Color(0xFF1C262B),
                                    heroStartPreview = Color(0xFF2B2719),
                                    accentPreview = Color(0xFFDAB978),
                                    secondaryPreview = Color(0xFF35B8AB),
                                    selected = uiState.preferences.themeMode == TarazinoThemeMode.MIDNIGHT_GOLD,
                                    onClick = { onSetThemeMode(TarazinoThemeMode.MIDNIGHT_GOLD) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_chip_${TarazinoThemeMode.MIDNIGHT_GOLD.name}")
                                )
                            }
                            ThemePreviewSwatchCard(
                                mode = TarazinoThemeMode.VELVET_AMETHYST,
                                titleFa = "ارغوان مخملی",
                                subtitleFa = "شفق تیره • فیروزه و طلا",
                                isLightTheme = false,
                                bgPreview = Color(0xFF0B1017),
                                surfacePreview = Color(0xFF1C2636),
                                heroStartPreview = Color(0xFF173642),
                                accentPreview = Color(0xFF5AC2B8),
                                secondaryPreview = Color(0xFFDAB978),
                                selected = uiState.preferences.themeMode == TarazinoThemeMode.VELVET_AMETHYST,
                                onClick = { onSetThemeMode(TarazinoThemeMode.VELVET_AMETHYST) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("theme_chip_${TarazinoThemeMode.VELVET_AMETHYST.name}")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "واحد پول نمایشی در کل برنامه:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val currencyList = CurrencyUnit.entries
                        TarazinoSegmentedControl(
                            options = currencyList.map { it.labelFa },
                            selectedIndex = currencyList.indexOf(uiState.preferences.currencyUnit).coerceAtLeast(0),
                            onSelectIndex = { idx -> onSetCurrencyUnit(currencyList[idx]) },
                            testTagPrefix = "unit_seg"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "مقیاس اندازه فونت برنامه:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val fontScales = FontScalePreset.entries
                        TarazinoSegmentedControl(
                            options = fontScales.map { it.labelFa },
                            selectedIndex = fontScales.indexOf(uiState.preferences.fontScale).coerceAtLeast(0),
                            onSelectIndex = { idx -> onSetFontScale(fontScales[idx]) }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = onStartGuidedTour,
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("replay_guided_tour_btn")
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            TarazinoAutoSizeText(
                                text = "اجرای مجدد تور آموزشی المان‌به‌المان برنامه",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                minFontSize = 9.5.sp,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }
            }

            // Home Screen Quick-Log Widget Pinning & Instant Preview Card
            item {
                TarazinoGlassCard(
                    accentBorderColor = ext.positive.copy(alpha = 0.4f),
                    modifier = Modifier.testTag("settings_home_widget_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TarazinoSectionHeader(
                            title = "ویجت ثبت سریع صفحه اصلی",
                            subtitle = "ثبت هزینه جدید یا تسویه قسط بدهی مستقیم از صفحه اصلی گوشی",
                            icon = Icons.Default.AutoAwesome,
                            accentColor = ext.positive
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "با افزودن ویجت ترازینو به صفحه اصلی، می‌توانید قسط بعدی را با یک کلیک تسویه کنید یا هزینه و پرداخت بدهی جدید را در پنجره شناور بدون باز کردن برنامه کامل ثبت کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val requested = TarazinoQuickLogWidgetProvider.requestPinToHomeScreen(context)
                                    val msg = if (requested) {
                                        "درخواست افزودن ویجت به صفحه اصلی ارسال شد."
                                    } else {
                                        "برای افزودن ویجت، روی صفحه اصلی گوشی نگه دارید و «ترازینو • ثبت سریع» را انتخاب کنید."
                                    }
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                },
                                shape = RoundedCornerShape(13.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(48.dp)
                                    .testTag("pin_quick_log_widget_btn")
                            ) {
                                TarazinoAutoSizeText(
                                    text = "افزودن به صفحه اصلی",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    minFontSize = 9.5.sp
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(context, QuickLogWidgetActivity::class.java).apply {
                                        putExtra(
                                            TarazinoQuickLogWidgetProvider.EXTRA_QUICK_LOG_MODE,
                                            WidgetQuickLogMode.EXPENSE.name
                                        )
                                    }
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(13.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(48.dp)
                                    .testTag("preview_quick_log_overlay_btn")
                            ) {
                                TarazinoAutoSizeText(
                                    text = "تست ثبت سریع",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    minFontSize = 9.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // Security Vault Configuration Card
            item {
                var newPinInput by remember { mutableStateOf("") }
                var selectedPinLength by remember { mutableIntStateOf(uiState.preferences.pinLength) }
                var bioEnabled by remember { mutableStateOf(uiState.preferences.biometricEnabled) }
                var recQuestion by remember { mutableStateOf(uiState.preferences.recoveryQuestion) }
                var recAnswer by remember { mutableStateOf("") }
                val isPinActive = uiState.preferences.pinHash.isNotBlank()

                TarazinoGlassCard(
                    accentBorderColor = if (isPinActive) ext.positive else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TarazinoSectionHeader(
                            title = "خزانه امنیتی آفلاین (رمز پین و بیومتریک)",
                            subtitle = if (isPinActive) "قفل رمز عبور فعال با رمزنگاری امن" else "محافظت از اطلاعات مالی در دستگاه",
                            icon = Icons.Default.Shield,
                            accentColor = if (isPinActive) ext.positive else MaterialTheme.colorScheme.secondary,
                            trailing = if (isPinActive) {
                                {
                                    OutlinedButton(
                                        onClick = onLockAppNow,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("قفل آنی", maxLines = 1)
                                    }
                                }
                            } else {
                                {
                                    TarazinoStatusPill(
                                        text = "غیرفعال",
                                        accentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        TarazinoSegmentedControl(
                            options = listOf("پین ۴ رقمی", "پین ۶ رقمی"),
                            selectedIndex = if (selectedPinLength == 6) 1 else 0,
                            onSelectIndex = { selectedPinLength = if (it == 1) 6 else 4 }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = {
                                val normalized = PersianCurrencyEngine.toEnglishDigits(it).filter { c -> c.isDigit() }
                                if (normalized.length <= selectedPinLength) {
                                    newPinInput = normalized
                                }
                            },
                            label = {
                                Text(
                                    text = "رمز پین جدید (${PersianCurrencyEngine.toPersianDigits(selectedPinLength.toString())} رقم)",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("security_pin_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val biometricStatus = remember(context) {
                            BiometricPromptAuthenticator.queryHardwareStatus(context)
                        }

                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ورود بیومتریک با اثر انگشت یا تشخیص چهره",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${biometricStatus.statusBadgeFa} • ${biometricStatus.detailDescriptionFa}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (biometricStatus.isReadyToAuthenticate) {
                                                ext.positive
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                    Switch(
                                        checked = bioEnabled,
                                        onCheckedChange = { bioEnabled = it },
                                        modifier = Modifier.testTag("security_biometric_enable_switch")
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        BiometricPromptAuthenticator.authenticateWithBiometricPrompt(
                                            context = context,
                                            onResult = { res ->
                                                when (res) {
                                                    is BiometricAuthResult.Success -> {
                                                        bioEnabled = true
                                                        onShowToast(res.methodDescriptionFa)
                                                    }
                                                    is BiometricAuthResult.FallbackToTouchSheet -> {
                                                        bioEnabled = true
                                                        onShowToast("احراز هویت بیومتریک آماده شد (${res.reasonFa})")
                                                    }
                                                    is BiometricAuthResult.CancelledOrFailed -> {
                                                        onShowToast(res.errorMessageFa)
                                                    }
                                                }
                                            }
                                        )
                                    },
                                    shape = RoundedCornerShape(11.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("test_biometric_prompt_btn")
                                ) {
                                    TarazinoAutoSizeText(
                                        text = "آزمایش و فراخوانی مستقیم حسگر بیومتریک دستگاه",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        minFontSize = 9.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = recQuestion,
                            onValueChange = { recQuestion = it },
                            label = { Text("سوال امنیتی بازیابی رمز", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = recAnswer,
                            onValueChange = { recAnswer = it },
                            label = { Text("پاسخ سوال امنیتی", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("برای بازگشایی اضطراری رمز", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onConfigureSecurityPin(
                                        newPinInput,
                                        selectedPinLength,
                                        bioEnabled,
                                        recQuestion,
                                        recAnswer
                                    )
                                    newPinInput = ""
                                    recAnswer = ""
                                },
                                shape = RoundedCornerShape(13.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1.35f)
                                    .testTag("save_security_pin_btn")
                            ) {
                                TarazinoAutoSizeText(
                                    text = "ذخیره و فعال‌سازی قفل",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    minFontSize = 9.5.sp
                                )
                            }
                            if (isPinActive) {
                                OutlinedButton(
                                    onClick = onRemoveSecurityPin,
                                    shape = RoundedCornerShape(13.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    border = BorderStroke(1.dp, ext.danger.copy(alpha = 0.55f)),
                                    modifier = Modifier.weight(0.95f)
                                ) {
                                    TarazinoAutoSizeText(
                                        text = "حذف قفل",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ext.danger,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        minFontSize = 9.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Shamsi Installment Due-Date Background Worker & Local Notification Card
            item {
                var bgReminderEnabled by remember {
                    mutableStateOf(ShamsiInstallmentNotificationManager.isBackgroundReminderEnabled(context))
                }
                var hasNotifPermission by remember {
                    mutableStateOf(ShamsiInstallmentNotificationManager.hasNotificationPermission(context))
                }
                val liveSummary = remember(
                    uiState.installments,
                    uiState.obligations,
                    uiState.counterparties,
                    uiState.preferences.alertAdvanceDays,
                    uiState.preferences.currencyUnit,
                    uiState.preferences.privacyMaskEnabled
                ) {
                    ShamsiInstallmentReminderEvaluator.buildShamsiDueNotificationSummary(
                        installments = uiState.installments,
                        obligations = uiState.obligations,
                        counterparties = uiState.counterparties,
                        todayJalali = JalaliCalendarEngine.todayJalali(),
                        alertAdvanceDays = uiState.preferences.alertAdvanceDays,
                        currencyUnit = uiState.preferences.currencyUnit,
                        privacyMasked = uiState.preferences.privacyMaskEnabled
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasNotifPermission = granted
                    if (granted) {
                        ShamsiInstallmentNotificationManager.setBackgroundReminderEnabled(context, true)
                        bgReminderEnabled = true
                        ShamsiInstallmentNotificationManager.postSummaryNotification(
                            context = context,
                            summary = liveSummary,
                            forceEvenIfEmpty = true
                        )
                        onShowToast("مجوز اعلان فعال شد و یادآور سررسیدهای شمسی ارسال گردید")
                    } else {
                        onShowToast("مجوز نمایش اعلان در سیستم اندروید تایید نشد")
                    }
                }

                TarazinoGlassCard(
                    accentBorderColor = if (bgReminderEnabled && hasNotifPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.testTag("shamsi_notification_settings_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TarazinoSectionHeader(
                            title = "یادآور پس‌زمینه و اعلان سررسیدهای شمسی",
                            subtitle = "بررسی خودکار اقساط با تقویم جلالی (${liveSummary.todayVerboseFa})",
                            icon = Icons.Default.NotificationsActive,
                            accentColor = MaterialTheme.colorScheme.primary,
                            trailing = {
                                TarazinoStatusPill(
                                    text = if (bgReminderEnabled && hasNotifPermission) "فعال (۱۲ ساعته)" else "نیازمند مجوز",
                                    accentColor = if (bgReminderEnabled && hasNotifPermission) ext.positive else ext.warning
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "پایش خودکار سررسید اقساط در پس‌زمینه",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "هشدار تا ${PersianCurrencyEngine.toPersianDigits(uiState.preferences.alertAdvanceDays.toString())} روز قبل از سررسید شمسی",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = bgReminderEnabled,
                                    onCheckedChange = { enabled ->
                                        bgReminderEnabled = enabled
                                        ShamsiInstallmentNotificationManager.setBackgroundReminderEnabled(context, enabled)
                                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    modifier = Modifier.testTag("shamsi_bg_notification_switch")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (ext.isDark) 0.36f else 0.55f),
                            border = BorderStroke(0.85.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = liveSummary.notificationTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = liveSummary.notificationShortText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !ShamsiInstallmentNotificationManager.hasNotificationPermission(context)
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    ShamsiInstallmentNotificationManager.triggerImmediateWorkerCheck(context)
                                    val posted = ShamsiInstallmentNotificationManager.postSummaryNotification(
                                        context = context,
                                        summary = liveSummary,
                                        forceEvenIfEmpty = true
                                    )
                                    if (posted) {
                                        onShowToast("اعلان سررسیدهای شمسی با موفقیت ارسال شد")
                                    } else {
                                        onShowToast("امکان ارسال اعلان وجود ندارد؛ مجوز اعلان دستگاه را بررسی کنید")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("trigger_shamsi_due_notification_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            TarazinoAutoSizeText(
                                text = "بررسی فوری و ارسال اعلان آزمایشی سررسید شمسی",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                minFontSize = 9.5.sp
                            )
                        }
                    }
                }
            }

            item {
                var autoThresholdEnabled by remember {
                    mutableStateOf(
                        GoalThresholdNotificationManager.isAutoThresholdNotificationEnabled(context)
                    )
                }
                var savingsThresholdPct by remember {
                    mutableIntStateOf(
                        GoalThresholdNotificationManager.getSavingsThresholdPercent(context)
                    )
                }
                var expenseThresholdPct by remember {
                    mutableIntStateOf(
                        GoalThresholdNotificationManager.getExpenseThresholdPercent(context)
                    )
                }

                val thresholdSummary = remember(
                    uiState.preferences.monthlySavingsGoalTitle,
                    uiState.preferences.monthlySavingsSavedToman,
                    uiState.preferences.monthlySavingsTargetToman,
                    uiState.preferences.monthlyIncomeToman,
                    uiState.preferences.livingExpensePercent,
                    uiState.preferences.currencyUnit,
                    uiState.preferences.privacyMaskEnabled,
                    savingsThresholdPct,
                    expenseThresholdPct
                ) {
                    GoalThresholdNotificationManager.evaluateFromPreferences(context)
                }

                val thresholdPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        GoalThresholdNotificationManager.setAutoThresholdNotificationEnabled(context, true)
                        autoThresholdEnabled = true
                        val posted = GoalThresholdNotificationManager.postThresholdNotification(
                            context = context,
                            summary = thresholdSummary,
                            forceEvenIfBelowThreshold = true
                        )
                        if (posted) {
                            onShowToast("مجوز اعلان فعال شد و پایش آستانه پس‌انداز و هزینه ارسال گردید")
                        }
                    } else {
                        onShowToast("مجوز نمایش اعلان در سیستم اندروید تایید نشد")
                    }
                }

                val thresholdOptions = GoalThresholdNotificationEvaluator.SUPPORTED_THRESHOLD_PERCENTS

                TarazinoGlassCard(
                    modifier = Modifier.testTag("goal_threshold_notification_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TarazinoSectionHeader(
                            title = "اعلان خودکار آستانه پس‌انداز و سقف هزینه (۸۰٪ / ۹۰٪)",
                            subtitle = "هشدار هوشمند هنگام عبور از درصد تعیین‌شده هدف پس‌انداز یا سقف هزینه‌های ماهانه",
                            icon = Icons.Default.NotificationsActive,
                            accentColor = ext.heroAccent
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ارسال خودکار اعلان عبور از آستانه هدف",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (autoThresholdEnabled) {
                                        "فعال (بررسی لحظه‌ای و پایش خودکار پس‌زمینه)"
                                    } else {
                                        "غیرفعال"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (autoThresholdEnabled) ext.positive else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoThresholdEnabled,
                                onCheckedChange = { enabled ->
                                    autoThresholdEnabled = enabled
                                    GoalThresholdNotificationManager.setAutoThresholdNotificationEnabled(
                                        context,
                                        enabled
                                    )
                                    if (enabled) {
                                        GoalThresholdNotificationManager.evaluateAndNotifyIfThresholdCrossed(
                                            context = context,
                                            forceEvenIfBelowThreshold = false
                                        )
                                        onShowToast("اعلان خودکار آستانه هدف پس‌انداز و هزینه فعال شد")
                                    } else {
                                        onShowToast("اعلان خودکار آستانه هدف غیرفعال شد")
                                    }
                                },
                                modifier = Modifier.testTag("goal_threshold_auto_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "آستانه هشدار تحقق هدف پس‌انداز ماهانه:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            thresholdOptions.forEach { pct ->
                                val isSelected = savingsThresholdPct == pct
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else ext.cardBorderTop
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            savingsThresholdPct = pct
                                            GoalThresholdNotificationManager.setSavingsThresholdPercent(
                                                context,
                                                pct
                                            )
                                            GoalThresholdNotificationManager.evaluateAndNotifyIfThresholdCrossed(
                                                context = context,
                                                forceEvenIfBelowThreshold = false
                                            )
                                        }
                                        .testTag("savings_threshold_pct_$pct")
                                ) {
                                    Text(
                                        text = "${PersianCurrencyEngine.toPersianDigits(pct.toString())}٪",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "آستانه هشدار عبور از سقف هزینه‌های ماهانه:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            thresholdOptions.forEach { pct ->
                                val isSelected = expenseThresholdPct == pct
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) {
                                        ext.heroAccent.copy(alpha = 0.18f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) ext.heroAccent else ext.cardBorderTop
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            expenseThresholdPct = pct
                                            GoalThresholdNotificationManager.setExpenseThresholdPercent(
                                                context,
                                                pct
                                            )
                                            GoalThresholdNotificationManager.evaluateAndNotifyIfThresholdCrossed(
                                                context = context,
                                                forceEvenIfBelowThreshold = false
                                            )
                                        }
                                        .testTag("expense_threshold_pct_$pct")
                                ) {
                                    Text(
                                        text = "${PersianCurrencyEngine.toPersianDigits(pct.toString())}٪",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            ext.heroAccent
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                            border = BorderStroke(1.dp, ext.cardBorderTop)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = thresholdSummary.notificationTitleFa,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (thresholdSummary.isExpenseThresholdExceeded) {
                                        ext.danger
                                    } else if (thresholdSummary.isSavingsThresholdExceeded) {
                                        ext.positive
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                                Text(
                                    text = thresholdSummary.notificationShortTextFa,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !GoalThresholdNotificationManager.hasNotificationPermission(context)
                                ) {
                                    thresholdPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val posted = GoalThresholdNotificationManager.postThresholdNotification(
                                        context = context,
                                        summary = thresholdSummary,
                                        forceEvenIfBelowThreshold = true
                                    )
                                    if (posted) {
                                        onShowToast("اعلان پایش آستانه پس‌انداز و هزینه ارسال شد")
                                    } else {
                                        onShowToast("امکان ارسال اعلان وجود ندارد؛ مجوز اعلان دستگاه را بررسی کنید")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("trigger_goal_threshold_notification_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            TarazinoAutoSizeText(
                                text = "بررسی فوری و ارسال اعلان آستانه هدف (۸۰٪ / ۹۰٪)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                minFontSize = 9.5.sp
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 2: OFFLINE DATA MANAGEMENT, DEMO SEED RESET & JSON BACKUP
        // =========================================================================
        if (activeSectionIndex == 2) {
            item {
                TarazinoGlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TarazinoSectionHeader(
                            title = "مدیریت داده‌های آفلاین و پشتیبان‌گیری",
                            subtitle = "شروع از صفر، بازگردانی نمونه و انتقال کامل داده‌ها با JSON",
                            icon = Icons.Default.Backup,
                            accentColor = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "برای بررسی سریع امکانات برنامه، داده‌های نمونه واقع‌گرایانه (اسنپ‌پی، دیجی‌پی، بانک مهر و...) بارگذاری شده است. هر زمان بخواهید می‌توانید با یک کلیک تمام داده‌های نمونه را پاک کرده و از صفر شروع کنید:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onClearAllDataToZero,
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ext.danger,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_all_data_to_zero_btn")
                        ) {
                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            TarazinoAutoSizeText(
                                text = "پاکسازی داده‌های نمونه (شروع از صفر)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                minFontSize = 9.5.sp,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onRestoreDemoSeedData,
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("restore_demo_seed_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            TarazinoAutoSizeText(
                                text = "بازگردانی مجدد داده‌های نمونه آزمایشی",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                minFontSize = 9.5.sp,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onExportBackupJson { json ->
                                        backupJsonDialogContent = json
                                    }
                                },
                                shape = RoundedCornerShape(13.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_json_btn")
                            ) {
                                TarazinoAutoSizeText(
                                    text = "خروجی JSON",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    minFontSize = 9.5.sp
                                )
                            }
                            OutlinedButton(
                                onClick = { showImportJsonDialog = true },
                                shape = RoundedCornerShape(13.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_backup_json_btn")
                            ) {
                                TarazinoAutoSizeText(
                                    text = "بازیابی JSON",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    minFontSize = 9.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Backup JSON Copy Modal (Specular Glass Dialog)
    backupJsonDialogContent?.let { json ->
        com.example.ui.components.TarazinoRtlDialog(
            onDismissRequest = { backupJsonDialogContent = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            TarazinoGlassCard(
                cornerRadius = 26.dp,
                accentBorderColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 440.dp)
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    TarazinoSectionHeader(
                        title = "پشتیبان کامل آفلاین (JSON)",
                        subtitle = "کپی و ذخیره امن برای انتقال به دستگاه دیگر",
                        icon = Icons.Default.Backup,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = json,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 8,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { backupJsonDialogContent = null },
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("بستن", maxLines = 1)
                        }
                        Button(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("TarazinoBackupJson", json))
                                onShowToast(PersianCurrencyEngine.formatRtlBidiSafe("پشتیبان JSON در کلیپ‌بورد کپی شد."))
                                backupJsonDialogContent = null
                            },
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("کپی در حافظه", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    // Import JSON Modal (Specular Glass Dialog)
    if (showImportJsonDialog) {
        com.example.ui.components.TarazinoRtlDialog(
            onDismissRequest = { showImportJsonDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            TarazinoGlassCard(
                cornerRadius = 26.dp,
                accentBorderColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 440.dp)
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    TarazinoSectionHeader(
                        title = "بازیابی از پشتیبان JSON",
                        subtitle = "متن پشتیبان قبلی را جای‌گذاری کنید",
                        icon = Icons.Default.Restore,
                        accentColor = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        label = {
                            Text(
                                text = PersianCurrencyEngine.formatRtlBidiSafe("متن JSON پشتیبان"),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        maxLines = 8,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showImportJsonDialog = false },
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("انصراف", maxLines = 1)
                        }
                        Button(
                            onClick = {
                                onImportBackupJson(importJsonInput)
                                showImportJsonDialog = false
                            },
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text("بازیابی اطلاعات", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewSwatchCard(
    mode: TarazinoThemeMode,
    titleFa: String,
    subtitleFa: String,
    isLightTheme: Boolean,
    bgPreview: Color,
    surfacePreview: Color,
    heroStartPreview: Color,
    accentPreview: Color,
    secondaryPreview: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderCol by animateColorAsState(
        targetValue = if (selected) accentPreview else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "ThemeSwatchBorder_${mode.name}"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ThemeSwatchScale_${mode.name}"
    )
    val shimmerTransition = rememberInfiniteTransition(label = "ThemeCardSheen_${mode.name}")
    val shimmerX by shimmerTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ThemeCardSheenX_${mode.name}"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            accentPreview.copy(alpha = 0.11f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
        },
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderCol),
        shadowElevation = if (selected) 4.dp else 0.dp,
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .tarazinoPressScale(scaleDown = 0.95f, onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            // Miniature Living App Interface Preview with Radial Aurora + Active Shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 0.85.dp,
                        color = accentPreview.copy(alpha = if (selected) 0.55f else 0.28f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawRect(color = bgPreview)
                    // Ambient radial aurora top-right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentPreview.copy(alpha = if (isLightTheme) 0.22f else 0.34f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.15f),
                            radius = size.width * 0.62f
                        ),
                        radius = size.width * 0.62f,
                        center = Offset(size.width * 0.85f, size.height * 0.15f)
                    )
                    // Secondary ambient radial glow bottom-left
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                secondaryPreview.copy(alpha = if (isLightTheme) 0.16f else 0.24f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.16f, size.height * 0.88f),
                            radius = size.width * 0.52f
                        ),
                        radius = size.width * 0.52f,
                        center = Offset(size.width * 0.16f, size.height * 0.88f)
                    )
                    // Subtle live specular sweep on selected theme card
                    if (selected) {
                        val sweepX = size.width * shimmerX
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (isLightTheme) 0.28f else 0.12f),
                                    accentPreview.copy(alpha = 0.14f),
                                    Color.Transparent
                                ),
                                start = Offset(sweepX - size.width * 0.28f, 0f),
                                end = Offset(sweepX + size.width * 0.28f, size.height)
                            )
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(7.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Mini Top Bar + Light/Dark Mood Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(5.5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentPreview)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.5.dp)
                                    .clip(CircleShape)
                                    .background(accentPreview)
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.5.dp)
                                    .clip(CircleShape)
                                    .background(secondaryPreview)
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.5.dp)
                                    .clip(CircleShape)
                                    .background(heroStartPreview)
                                    .border(0.6.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                            )
                        }
                    }

                    // Mini Vault Hero Card Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        heroStartPreview,
                                        if (isLightTheme) heroStartPreview.copy(alpha = 0.85f) else surfacePreview
                                    )
                                )
                            )
                            .border(0.65.dp, accentPreview.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.5.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.SpaceBetween) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.58f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(accentPreview)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.36f)
                                    .height(3.5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(secondaryPreview.copy(alpha = 0.84f))
                            )
                        }
                    }

                    // Mini Surface Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(11.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(surfacePreview)
                                .border(
                                    0.5.dp,
                                    accentPreview.copy(alpha = if (isLightTheme) 0.22f else 0.16f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(11.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(surfacePreview)
                                .border(
                                    0.5.dp,
                                    secondaryPreview.copy(alpha = if (isLightTheme) 0.22f else 0.16f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    TarazinoAutoSizeText(
                        text = titleFa,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (selected) accentPreview else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        minFontSize = 9.5.sp
                    )
                    TarazinoAutoSizeText(
                        text = subtitleFa,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        minFontSize = 8.sp
                    )
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "پوسته فعال",
                        tint = accentPreview,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportFormatTileCard(
    titleFa: String,
    subtitleFa: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.78f else 0.92f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            0.9.dp,
            Brush.verticalGradient(
                listOf(
                    ext.cardBorderTop,
                    ext.cardBorderBottom
                )
            )
        ),
        modifier = modifier
            .height(94.dp)
            .tarazinoPressScale(scaleDown = 0.94f, onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accentColor.copy(alpha = if (ext.isDark) 0.18f else 0.14f))
                    .border(0.8.dp, accentColor.copy(alpha = 0.38f), RoundedCornerShape(11.dp))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            TarazinoAutoSizeText(
                text = titleFa,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                minFontSize = 9.5.sp,
                modifier = Modifier.fillMaxWidth()
            )
            TarazinoAutoSizeText(
                text = subtitleFa,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                minFontSize = 8.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
