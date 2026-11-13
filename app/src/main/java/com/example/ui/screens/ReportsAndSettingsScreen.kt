package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyUnit
import com.example.core.PersianCurrencyEngine
import com.example.data.CounterpartyEntity
import com.example.data.FontScalePreset
import com.example.data.TarazinoThemeMode
import com.example.export.ReportExportEngine
import com.example.memory.AgentMemoryLedger
import com.example.ui.TarazinoUiState

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
    onReRunQaSuites: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    var activeSectionIndex by remember { mutableIntStateOf(0) } // 0=Reports, 1=Theme&Security, 2=Backup&Data, 3=AgentMemory&QA
    var backupJsonDialogContent by remember { mutableStateOf<String?>(null) }
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }

    val sectionTabs = listOf(
        "چاپ A4 و اکسل",
        "پوسته و امنیت",
        "پشتیبان و داده",
        "حافظه ایجنت و تست QA"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_and_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Selector Chips
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                sectionTabs.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = activeSectionIndex == idx,
                        onClick = { activeSectionIndex = idx },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (activeSectionIndex == idx) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("settings_subtab_$idx")
                    )
                }
            }
        }

        // SECTION 0: A4 PDF & RTL Excel Export Studio (MOD-06)
        if (activeSectionIndex == 0) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "استودیو گزارش‌گیری و چاپ رسمی استاندارد A4 و اکسل",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "رندر گرافیکی برگه A4 رنگی (۲۱۰×۲۹۷ میلی‌متر) با فونت وزیرمتن، کادر مهر و امضا و اکسل راست‌به‌چپ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onOpenFullA4Report,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_full_a4_preview_btn")
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("پیش‌نمایش زنده و چاپ گزارش جامع A4 رنگی", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDirectExportPdf,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فایل PDF A4")
                            }
                            OutlinedButton(
                                onClick = onDirectExportExcel,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اکسل (.xls)")
                            }
                            OutlinedButton(
                                onClick = onDirectExportCsv,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CSV BOM")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "خروجی اختصاصی صورتحساب A4 به تفکیک هر طرف‌حساب:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        uiState.counterparties.forEach { cp ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cp.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = cp.categoryEnum.badgeFa,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onOpenCounterpartyA4Report(cp) }
                                    ) {
                                        Text("صورتحساب A4", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECTION 1: Personalization, Themes, Guided Tour & Security Vault (MOD-07 & MOD-08)
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

                        Text("انتخاب پوسته رنگی (تم روشن و تیره):", style = MaterialTheme.typography.labelMedium)
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
                                    label = { Text(mode.labelFa) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("واحد پول نمایشی کل برنامه:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CurrencyUnit.entries.forEach { unit ->
                                FilterChip(
                                    selected = uiState.preferences.currencyUnit == unit,
                                    onClick = { onSetCurrencyUnit(unit) },
                                    label = { Text(unit.labelFa) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("مقیاس اندازه فونت وزیرمتن:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FontScalePreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = uiState.preferences.fontScale == preset,
                                    onClick = { onSetFontScale(preset) },
                                    label = { Text(preset.labelFa) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onStartGuidedTour,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("relaunch_guided_tour_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "اجرای مجدد تور آموزشی گرافیکی و تعاملی ترازینو",
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                SecurityVaultConfigurationCard(
                    uiState = uiState,
                    onConfigureSecurityPin = onConfigureSecurityPin,
                    onRemoveSecurityPin = onRemoveSecurityPin,
                    onLockAppNow = onLockAppNow
                )
            }
        }

        // SECTION 2: Seed Data Control & Encrypted/Offline JSON Backup (MOD-09)
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
                                text = "مدیریت داده‌های نمونه و پشتیبان‌گیری آفلاین JSON",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "برای بررسی سریع امکانات، داده‌های نمونه واقعی (بانک مهر، اسنپ‌پی، قرض رضا محمدی و چک صیادی) بارگذاری شده‌اند. هر زمان آماده ثبت اطلاعات شخصی خود بودید، با یک کلیک داده‌های نمونه را پاکسازی کنید:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onClearAllDataToZero,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("clear_seed_data_btn")
                            ) {
                                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("شروع از صفر (پاکسازی نمونه)")
                            }

                            OutlinedButton(
                                onClick = onRestoreDemoSeedData,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_seed_data_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("بازگردانی داده نمونه")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "پشتیبان‌گیری و بازیابی کامل فایل JSON:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onExportBackupJson { jsonStr ->
                                        backupJsonDialogContent = jsonStr
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("تولید و کپی پشتیبان JSON")
                            }

                            OutlinedButton(
                                onClick = { showImportJsonDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("بازیابی از JSON")
                            }
                        }
                    }
                }
            }
        }

        // SECTION 3: Agent Short-Term Memory Hub & 10 Automated QA Test Suites (MOD-10)
        if (activeSectionIndex == 3) {
            item {
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
                                    imageVector = Icons.Default.Science,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "سوئیت تست خودکار QA (۱۰ آزمون صحت‌سنجی)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val passedCount = uiState.qaSuiteResults.count { it.passed }
                                    Text(
                                        text = "${PersianCurrencyEngine.toPersianDigits(passedCount.toString())} از ${
                                            PersianCurrencyEngine.toPersianDigits(uiState.qaSuiteResults.size.toString())
                                        } سوئیت با موفقیت ۱۰۰٪ پاس شد",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }

                            Button(
                                onClick = onReRunQaSuites,
                                modifier = Modifier.testTag("rerun_qa_suites_btn")
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اجرای مجدد تست‌ها")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        uiState.qaSuiteResults.forEach { suite ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(
                                        width = 1.dp,
                                        color = if (suite.passed) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFEF4444),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (suite.passed) Color(0xFF10B981) else Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${suite.suiteId}: ${suite.titleFa}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = if (suite.passed) "PASSED" else "FAILED",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (suite.passed) Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = suite.detailFa,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Agent Short-Term Memory Ledger Card (MOD-01 .. MOD-10)
            item {
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
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "حافظه ایجنت و وضعیت توسعه (AGENT_MEMORY)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "نسخه اسکیما: ${AgentMemoryLedger.SCHEMA_VERSION}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val snap = AgentMemoryLedger.generateHandoffJsonSnapshot()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("HANDOFF_CONTEXT_SNAPSHOT", snap))
                                    onShowToast("HANDOFF_CONTEXT_SNAPSHOT.json در کلیپ‌بورد کپی شد.")
                                },
                                modifier = Modifier.testTag("copy_handoff_snapshot_btn")
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("کپی Handoff JSON")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "قوانین تغییرناپذیر معماری (Architectural Invariants):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        AgentMemoryLedger.invariants.forEach { inv ->
                            Text(
                                text = "• [${inv.id}] ${inv.titleFa}: ${inv.ruleDescriptionFa}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "وضعیت ۱۰ ماژول مهندسی (MOD-01 تا MOD-10):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        AgentMemoryLedger.modules.forEach { mod ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${mod.moduleId} — ${mod.titleFa}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = mod.status.badgeFa,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    mod.subTasks.forEach { st ->
                                        Text(
                                            text = "  ✓ [${st.id}] ${st.titleFa}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    backupJsonDialogContent?.let { jsonContent ->
        AlertDialog(
            onDismissRequest = { backupJsonDialogContent = null },
            title = { Text("پشتیبان کامل JSON ترازینو", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = jsonContent,
                    onValueChange = {},
                    readOnly = true,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Tarazino_Backup_JSON", jsonContent))
                        onShowToast("فایل پشتیبان JSON در حافظه کپی شد.")
                        backupJsonDialogContent = null
                    }
                ) {
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

    if (showImportJsonDialog) {
        AlertDialog(
            onDismissRequest = { showImportJsonDialog = false },
            title = { Text("بازیابی اطلاعات از فایل پشتیبان JSON", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = importJsonInput,
                    onValueChange = { importJsonInput = it },
                    label = { Text("متن JSON پشتیبان را اینجا جای‌گذاری (Paste) کنید") },
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportBackupJson(importJsonInput)
                        showImportJsonDialog = false
                    }
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
private fun SecurityVaultConfigurationCard(
    uiState: TarazinoUiState,
    onConfigureSecurityPin: (String, Int, Boolean, String, String) -> Unit,
    onRemoveSecurityPin: () -> Unit,
    onLockAppNow: () -> Unit
) {
    val prefs = uiState.preferences
    var pinLength by remember { mutableIntStateOf(prefs.pinLength) }
    var newPin by remember { mutableStateOf("") }
    var biometricEnabled by remember { mutableStateOf(prefs.biometricEnabled) }
    var recoveryQuestion by remember { mutableStateOf(prefs.recoveryQuestion) }
    var recoveryAnswer by remember { mutableStateOf("") }

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
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "خزانه امنیتی: قفل پین و سنسور بیومتریک",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (prefs.pinHash.isNotBlank()) "وضعیت: فعال (محافظت‌شده با SHA-256)" else "وضعیت: غیرفعال",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (prefs.pinHash.isNotBlank()) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (prefs.pinHash.isNotBlank()) {
                    OutlinedButton(onClick = onLockAppNow) {
                        Text("قفل آنی")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = pinLength == 4,
                    onClick = { pinLength = 4 },
                    label = { Text("پین ۴ رقمی") }
                )
                FilterChip(
                    selected = pinLength == 6,
                    onClick = { pinLength = 6 },
                    label = { Text("پین ۶ رقمی") }
                )
            }

            OutlinedTextField(
                value = newPin,
                onValueChange = {
                    newPin = PersianCurrencyEngine.toEnglishDigits(it).filter { c -> c.isDigit() }.take(pinLength)
                },
                label = { Text("رمز پین جدید (${PersianCurrencyEngine.toPersianDigits(pinLength.toString())} رقم)") },
                placeholder = { Text(if (pinLength == 4) "مثلاً ۲۵۸۰" else "مثلاً ۲۵۸۰۱۲") },
                singleLine = true,
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
                Text("ورود با اثر انگشت / سنسور بیومتریک:", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = { biometricEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = recoveryQuestion,
                onValueChange = { recoveryQuestion = it },
                label = { Text("سوال امنیتی بازیابی رمز") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = recoveryAnswer,
                onValueChange = { recoveryAnswer = it },
                label = { Text("پاسخ سوال امنیتی") },
                singleLine = true,
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
                            newPin,
                            pinLength,
                            biometricEnabled,
                            recoveryQuestion,
                            recoveryAnswer
                        )
                        newPin = ""
                        recoveryAnswer = ""
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_security_pin_btn")
                ) {
                    Text("ذخیره و فعال‌سازی قفل پین")
                }

                if (prefs.pinHash.isNotBlank()) {
                    OutlinedButton(
                        onClick = onRemoveSecurityPin,
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("حذف قفل")
                    }
                }
            }
        }
    }
}
