package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyUnit
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.FontScalePreset
import com.example.data.TarazinoThemeMode
import com.example.ui.TarazinoUiState
import com.example.ui.components.CounterpartyMonogramAvatar
import com.example.ui.components.TarazinoGlassCard
import com.example.ui.components.TarazinoSectionHeader
import com.example.ui.components.TarazinoSegmentedControl
import com.example.ui.components.TarazinoStatusPill
import com.example.ui.components.tarazinoPressScale
import com.example.ui.components.tourSpotlightTarget

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
    tourScrollIndex: Int? = null
) {
    val context = LocalContext.current
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
                TarazinoGlassCard(
                    modifier = Modifier
                        .tourSpotlightTarget("reports_a4_export_studio")
                        .testTag("reports_a4_export_studio_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TarazinoSectionHeader(
                            title = "استودیو گزارش‌گیری و چاپ رسمی A4",
                            subtitle = "برگه رنگی A4 با فونت وزیرمتن، کادر مهر و امضا و اکسل راست‌به‌چپ",
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
                            Text(
                                text = "پیش‌نمایش زنده و چاپ گزارش جامع A4 رنگی",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                accentColor = Color(0xFF10B981),
                                onClick = onDirectExportPdf,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("direct_export_pdf_btn")
                            )
                            ExportFormatTileCard(
                                titleFa = "فایل اکسل",
                                subtitleFa = "راست‌به‌چپ .xls",
                                icon = Icons.Default.TableChart,
                                accentColor = Color(0xFF3B82F6),
                                onClick = onDirectExportExcel,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("direct_export_excel_btn")
                            )
                            ExportFormatTileCard(
                                titleFa = "فایل CSV",
                                subtitleFa = "UTF-8 BOM",
                                icon = Icons.Default.Description,
                                accentColor = Color(0xFFF59E0B),
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
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        uiState.counterparties.forEach { cp ->
                            val accent = Color(cp.avatarColorHex)
                            Surface(
                                shape = RoundedCornerShape(15.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(0.8.dp, accent.copy(alpha = 0.3f)),
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
                                            size = 36.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = cp.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = cp.categoryEnum.badgeFa,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedButton(
                                        onClick = { onOpenCounterpartyA4Report(cp) },
                                        shape = RoundedCornerShape(11.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "صورتحساب A4",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
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

        // =========================================================================
        // SECTION 1: VISUAL THEME SWATCHES, DISPLAY & SECURITY VAULT
        // =========================================================================
        if (activeSectionIndex == 1) {
            item {
                TarazinoGlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TarazinoSectionHeader(
                            title = "شخصی‌سازی پوسته، واحد پول و نمایش",
                            subtitle = "تنظیم ظاهر حرفه‌ای، مقیاس فونت وزیرمتن و بازپخش تور آموزشی",
                            icon = Icons.Default.Palette,
                            accentColor = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "انتخاب پوسته ظاهری (تیره و روشن):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // 3 Tactile Visual Theme Preview Swatch Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ThemePreviewSwatchCard(
                                mode = TarazinoThemeMode.OBSIDIAN_EMERALD_DARK,
                                titleFa = "زمرد شب",
                                bgPreview = Color(0xFF070D11),
                                surfacePreview = Color(0xFF14222A),
                                accentPreview = Color(0xFF10B981),
                                selected = uiState.preferences.themeMode == TarazinoThemeMode.OBSIDIAN_EMERALD_DARK,
                                onClick = { onSetThemeMode(TarazinoThemeMode.OBSIDIAN_EMERALD_DARK) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_chip_${TarazinoThemeMode.OBSIDIAN_EMERALD_DARK.name}")
                            )
                            ThemePreviewSwatchCard(
                                mode = TarazinoThemeMode.ALABASTER_LIGHT,
                                titleFa = "مرمر روشن",
                                bgPreview = Color(0xFFF4F7F6),
                                surfacePreview = Color(0xFFFFFFFF),
                                accentPreview = Color(0xFF047857),
                                selected = uiState.preferences.themeMode == TarazinoThemeMode.ALABASTER_LIGHT,
                                onClick = { onSetThemeMode(TarazinoThemeMode.ALABASTER_LIGHT) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_chip_${TarazinoThemeMode.ALABASTER_LIGHT.name}")
                            )
                            ThemePreviewSwatchCard(
                                mode = TarazinoThemeMode.DEEP_SAPPHIRE,
                                titleFa = "اقیانوس کبود",
                                bgPreview = Color(0xFF070C19),
                                surfacePreview = Color(0xFF13203E),
                                accentPreview = Color(0xFF06B6D4),
                                selected = uiState.preferences.themeMode == TarazinoThemeMode.DEEP_SAPPHIRE,
                                onClick = { onSetThemeMode(TarazinoThemeMode.DEEP_SAPPHIRE) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_chip_${TarazinoThemeMode.DEEP_SAPPHIRE.name}")
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
                            text = "مقیاس اندازه فونت وزیرمتن:",
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
                            Text(
                                text = "اجرای مجدد تور آموزشی المان‌به‌المان برنامه",
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                    accentBorderColor = if (isPinActive) Color(0xFF10B981) else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TarazinoSectionHeader(
                            title = "خزانه امنیتی آفلاین (PIN + بیومتریک)",
                            subtitle = if (isPinActive) "قفل رمز عبور فعال با هش SHA-256" else "محافظت از اطلاعات مالی در دستگاه",
                            icon = Icons.Default.Shield,
                            accentColor = if (isPinActive) Color(0xFF10B981) else MaterialTheme.colorScheme.secondary,
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
                                Text("رمز پین جدید (${PersianCurrencyEngine.toPersianDigits(selectedPinLength.toString())} رقم)")
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("security_pin_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ورود سریع با اثر انگشت / تشخیص چهره",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = bioEnabled,
                                    onCheckedChange = { bioEnabled = it }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = recQuestion,
                            onValueChange = { recQuestion = it },
                            label = { Text("سوال امنیتی بازیابی رمز") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = recAnswer,
                            onValueChange = { recAnswer = it },
                            label = { Text("پاسخ سوال امنیتی (برای بازگشایی اضطراری)") },
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
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("save_security_pin_btn")
                            ) {
                                Text("فعال‌سازی / ذخیره قفل", fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            if (isPinActive) {
                                OutlinedButton(
                                    onClick = onRemoveSecurityPin,
                                    shape = RoundedCornerShape(13.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("حذف قفل", color = Color(0xFFEF4444), maxLines = 1)
                                }
                            }
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_all_data_to_zero_btn")
                        ) {
                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "پاکسازی داده‌های نمونه (شروع از صفر)",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
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
                            Text("بازگردانی مجدد داده‌های نمونه آزمایشی", maxLines = 1)
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
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_json_btn")
                            ) {
                                Text("خروجی پشتیبان JSON", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { showImportJsonDialog = true },
                                shape = RoundedCornerShape(13.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_backup_json_btn")
                            ) {
                                Text("بازیابی از JSON", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }

    // Backup JSON Copy Modal (Specular Glass Dialog)
    backupJsonDialogContent?.let { json ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { backupJsonDialogContent = null }) {
            TarazinoGlassCard(
                cornerRadius = 26.dp,
                accentBorderColor = MaterialTheme.colorScheme.primary
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
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
                        maxLines = 9,
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
                            Text("بستن")
                        }
                        Button(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("TarazinoBackupJson", json))
                                onShowToast("پشتیبان JSON در کلیپ‌بورد کپی شد.")
                                backupJsonDialogContent = null
                            },
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("کپی در حافظه", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Import JSON Modal (Specular Glass Dialog)
    if (showImportJsonDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showImportJsonDialog = false }) {
            TarazinoGlassCard(
                cornerRadius = 26.dp,
                accentBorderColor = MaterialTheme.colorScheme.secondary
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
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
                        label = { Text("متن JSON پشتیبان را اینجا جای‌گذاری کنید") },
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
                            Text("انصراف")
                        }
                        Button(
                            onClick = {
                                onImportBackupJson(importJsonInput)
                                showImportJsonDialog = false
                            },
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text("بازیابی اطلاعات", fontWeight = FontWeight.Bold)
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
    bgPreview: Color,
    surfacePreview: Color,
    accentPreview: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderCol by animateColorAsState(
        targetValue = if (selected) accentPreview else MaterialTheme.colorScheme.outlineVariant,
        label = "ThemeSwatchBorder_${mode.name}"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderCol),
        modifier = modifier
            .tarazinoPressScale(scaleDown = 0.94f, onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            // Miniature Phone Theme Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(bgPreview)
                    .border(0.8.dp, accentPreview.copy(alpha = 0.3f), RoundedCornerShape(11.dp))
                    .padding(6.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentPreview)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(surfacePreview)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(accentPreview.copy(alpha = 0.65f))
                        )
                    }
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accentPreview,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = titleFa,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accentPreview else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = 0.09f),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    accentColor.copy(alpha = 0.5f),
                    accentColor.copy(alpha = 0.2f)
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
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = titleFa,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitleFa,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
