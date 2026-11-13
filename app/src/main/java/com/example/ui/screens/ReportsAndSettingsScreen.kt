package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 3-Column Symmetrical Segmented Section Selector
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sectionTabs.forEachIndexed { idx, label ->
                        val selected = activeSectionIndex == idx
                        val bg by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            label = "SettingsTabBg"
                        )
                        val contentCol = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .clickable { activeSectionIndex = idx }
                                .testTag("settings_subtab_$idx")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = contentCol,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // SECTION 0: A4 PDF & RTL Excel Export Studio (Polished & Symmetrical!)
        if (activeSectionIndex == 0) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tourSpotlightTarget("reports_a4_export_studio")
                        .testTag("reports_a4_export_studio_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "استودیو گزارش‌گیری و چاپ رسمی A4",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "رندر گرافیکی برگه A4 رنگی (۲۱۰×۲۹۷ میلی‌متر) با فونت وزیرمتن، کادر مهر و امضا و اکسل راست‌به‌چپ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Primary Live A4 Preview & Print Button (Clean Rectangular Shape)
                        Button(
                            onClick = onOpenFullA4Report,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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

                        // FIXED: 3 Symmetrical Rectangular Export Action Tiles (No oval/circle distortion!)
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "خروجی اختصاصی صورتحساب A4 به تفکیک هر زیرمجموعه / طرف‌حساب:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        uiState.counterparties.forEach { cp ->
                            val accent = Color(cp.avatarColorHex)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .border(
                                        0.8.dp,
                                        accent.copy(alpha = 0.3f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(accent)
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
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
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

        // SECTION 1: Theme, Typography, Tour & Security Vault
        if (activeSectionIndex == 1) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "شخصی‌سازی پوسته، واحد پول، فونت و تور آموزشی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("پوسته ظاهری (تم روشن و تیره):", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            TarazinoThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = uiState.preferences.themeMode == mode,
                                    onClick = { onSetThemeMode(mode) },
                                    label = { Text(mode.labelFa, maxLines = 1) },
                                    modifier = Modifier.testTag("theme_chip_${mode.name}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("واحد پول نمایشی در کل برنامه:", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CurrencyUnit.entries.forEach { u ->
                                FilterChip(
                                    selected = uiState.preferences.currencyUnit == u,
                                    onClick = { onSetCurrencyUnit(u) },
                                    label = { Text(u.labelFa, maxLines = 1) },
                                    modifier = Modifier.testTag("unit_chip_${u.name}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("مقیاس اندازه فونت وزیرمتن:", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FontScalePreset.entries.forEach { scale ->
                                FilterChip(
                                    selected = uiState.preferences.fontScale == scale,
                                    onClick = { onSetFontScale(scale) },
                                    label = { Text(scale.labelFa, maxLines = 1) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onStartGuidedTour,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("replay_guided_tour_btn")
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "اجرای مجدد تور آموزشی المان‌به‌المان برنامه",
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

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "خزانه امنیتی آفلاین (PIN + بیومتریک)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (uiState.preferences.pinHash.isNotBlank()) {
                                            "وضعیت: قفل رمز عبور فعال است (SHA-256)"
                                        } else {
                                            "وضعیت: بدون رمز عبور"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (uiState.preferences.pinHash.isNotBlank()) {
                                            Color(0xFF10B981)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                            if (uiState.preferences.pinHash.isNotBlank()) {
                                OutlinedButton(
                                    onClick = onLockAppNow,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("قفل آنی", maxLines = 1)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedPinLength == 4,
                                onClick = { selectedPinLength = 4 },
                                label = { Text("پین ۴ رقمی") }
                            )
                            FilterChip(
                                selected = selectedPinLength == 6,
                                onClick = { selectedPinLength = 6 },
                                label = { Text("پین ۶ رقمی") }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("security_pin_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ورود سریع با اثر انگشت / تشخیص چهره (Biometric)",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = bioEnabled,
                                onCheckedChange = { bioEnabled = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = recQuestion,
                            onValueChange = { recQuestion = it },
                            label = { Text("سوال امنیتی بازیابی رمز") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = recAnswer,
                            onValueChange = { recAnswer = it },
                            label = { Text("پاسخ سوال امنیتی (برای بازگشایی اضطراری)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("save_security_pin_btn")
                            ) {
                                Text("فعال‌سازی / ذخیره قفل", fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            if (uiState.preferences.pinHash.isNotBlank()) {
                                OutlinedButton(
                                    onClick = onRemoveSecurityPin,
                                    shape = RoundedCornerShape(12.dp),
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

        // SECTION 2: Offline Data Management, Demo Seed Reset & JSON Backup
        if (activeSectionIndex == 2) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مدیریت داده‌های آفلاین، شروع از صفر و پشتیبان‌گیری",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "برای بررسی سریع امکانات برنامه، داده‌های نمونه واقع‌گرایانه (اسنپ‌پی، دیجی‌پی، بانک مهر و...) بارگذاری شده است. هر زمان بخواهید می‌توانید با یک کلیک تمام داده‌های نمونه را پاک کرده و از صفر شروع کنید:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onClearAllDataToZero,
                            shape = RoundedCornerShape(12.dp),
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("restore_demo_seed_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بازگردانی مجدد داده‌های نمونه آزمایشی", maxLines = 1)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

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
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_json_btn")
                            ) {
                                Text("خروجی پشتیبان JSON", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { showImportJsonDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_backup_json_btn")
                            ) {
                                Text("بازیابی از فایل JSON", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    // Backup JSON Copy Modal
    backupJsonDialogContent?.let { json ->
        AlertDialog(
            onDismissRequest = { backupJsonDialogContent = null },
            title = { Text("پشتیبان کامل پایگاه داده آفلاین (JSON)", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = json,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("TarazinoBackupJson", json))
                        onShowToast("پشتیبان JSON در کلیپ‌بورد کپی شد.")
                        backupJsonDialogContent = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("کپی در حافظه")
                }
            },
            dismissButton = {
                TextButton(onClick = { backupJsonDialogContent = null }) {
                    Text("بستن")
                }
            }
        )
    }

    // Import JSON Modal
    if (showImportJsonDialog) {
        AlertDialog(
            onDismissRequest = { showImportJsonDialog = false },
            title = { Text("بازیابی اطلاعات از پشتیبان JSON", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = importJsonInput,
                    onValueChange = { importJsonInput = it },
                    label = { Text("متن JSON پشتیبان را اینجا جای‌گذاری کنید") },
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportBackupJson(importJsonInput)
                        showImportJsonDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("بازیابی اطلاعات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportJsonDialog = false }) {
                    Text("انصراف")
                }
            }
        )
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
        color = accentColor.copy(alpha = 0.1f),
        modifier = modifier
            .height(92.dp)
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
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
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
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
