package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.core.JalaliCalendarEngine
import com.example.core.JalaliDate
import com.example.core.PersianCurrencyEngine
import com.example.ui.theme.LocalTarazinoColors

@Composable
fun JalaliDatePickerModal(
    initialDateJalali: String,
    onDismiss: () -> Unit,
    onConfirmDate: (String) -> Unit
) {
    val ext = LocalTarazinoColors.current
    val parsedInit = remember(initialDateJalali) {
        JalaliCalendarEngine.parseJalali(initialDateJalali)
    }
    val today = remember {
        JalaliCalendarEngine.parseJalali(JalaliCalendarEngine.todayJalali())
    }

    var selectedYear by remember { mutableIntStateOf(parsedInit.year.coerceIn(1395, 1420)) }
    var selectedMonth by remember { mutableIntStateOf(parsedInit.month.coerceIn(1, 12)) }
    var selectedDay by remember { mutableIntStateOf(parsedInit.day) }

    val maxDaysInMonth = JalaliCalendarEngine.getJalaliMonthLength(selectedYear, selectedMonth)
    if (selectedDay > maxDaysInMonth) {
        selectedDay = maxDaysInMonth
    }

    val firstDayWeekdayOffset = remember(selectedYear, selectedMonth) {
        JalaliCalendarEngine.getPersianWeekdayIndex(JalaliDate(selectedYear, selectedMonth, 1))
    }

    val currentPreviewStr = JalaliDate(selectedYear, selectedMonth, selectedDay).formatCanonical()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 14.dp,
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(listOf(ext.cardBorderTop, ext.cardBorderBottom))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("jalali_date_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Specular Gradient Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TarazinoIconBadge(
                            icon = Icons.Default.CalendarMonth,
                            accentColor = MaterialTheme.colorScheme.primary,
                            size = 42.dp,
                            iconSize = 21.dp
                        )
                        Column {
                            Text(
                                text = "انتخاب تاریخ تقویم شمسی (جلالی)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = JalaliCalendarEngine.formatJalaliVerbose(currentPreviewStr),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Shortcuts Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    DateShortcutPill(
                        label = "امروز (${today.formatPersian()})",
                        onClick = {
                            selectedYear = today.year
                            selectedMonth = today.month
                            selectedDay = today.day
                        }
                    )
                    DateShortcutPill(
                        label = "اول ماه بعد",
                        onClick = {
                            val nextFirst = JalaliCalendarEngine.parseJalali(
                                JalaliCalendarEngine.firstOfNextJalaliMonth(today.formatCanonical())
                            )
                            selectedYear = nextFirst.year
                            selectedMonth = nextFirst.month
                            selectedDay = nextFirst.day
                        }
                    )
                    DateShortcutPill(
                        label = "۱ ماه دیگر",
                        onClick = {
                            val plus1M = JalaliCalendarEngine.parseJalali(
                                JalaliCalendarEngine.addJalaliMonths(today.formatCanonical(), 1)
                            )
                            selectedYear = plus1M.year
                            selectedMonth = plus1M.month
                            selectedDay = plus1M.day
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Combined Year & Month Switcher Container
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                        // Year Selector Row (۱۳۹۵ تا ۱۴۲۰)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { if (selectedYear > 1395) selectedYear-- }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "سال قبل"
                                )
                            }
                            Text(
                                text = "سال ${PersianCurrencyEngine.toPersianDigits(selectedYear.toString())}" +
                                    if (JalaliCalendarEngine.isJalaliLeapYear(selectedYear)) " (کبیسه)" else "",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { if (selectedYear < 1420) selectedYear++ }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "سال بعد"
                                )
                            }
                        }

                        // Month Selector Row (فروردین تا اسفند)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (selectedMonth > 1) {
                                        selectedMonth--
                                    } else if (selectedYear > 1395) {
                                        selectedYear--
                                        selectedMonth = 12
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "ماه قبل"
                                )
                            }
                            Text(
                                text = JalaliCalendarEngine.getMonthName(selectedMonth),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = {
                                    if (selectedMonth < 12) {
                                        selectedMonth++
                                    } else if (selectedYear < 1420) {
                                        selectedYear++
                                        selectedMonth = 1
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "ماه بعد"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 7-Column Weekday Headers (ش، ی، د، س، چ، پ، ج)
                Row(modifier = Modifier.fillMaxWidth()) {
                    JalaliCalendarEngine.WEEKDAY_SHORT.forEachIndexed { idx, w ->
                        Text(
                            text = w,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (idx == 6) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Calendar Day Grid (6 rows x 7 columns max)
                val totalCells = firstDayWeekdayOffset + maxDaysInMonth
                val totalRows = (totalCells + 6) / 7

                for (rowIdx in 0 until totalRows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        for (colIdx in 0 until 7) {
                            val cellIndex = rowIdx * 7 + colIdx
                            val dayNum = cellIndex - firstDayWeekdayOffset + 1
                            if (dayNum in 1..maxDaysInMonth) {
                                val isSelected = dayNum == selectedDay
                                val isTodayCell = (selectedYear == today.year &&
                                    selectedMonth == today.month &&
                                    dayNum == today.day)

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isTodayCell -> MaterialTheme.colorScheme.secondaryContainer
                                                else -> Color.Transparent
                                            }
                                        )
                                        .then(
                                            if (isTodayCell && !isSelected) {
                                                Modifier.border(
                                                    width = 1.5.dp,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    shape = CircleShape
                                                )
                                            } else Modifier
                                        )
                                        .clickable { selectedDay = dayNum }
                                ) {
                                    Text(
                                        text = PersianCurrencyEngine.toPersianDigits(dayNum.toString()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected || isTodayCell) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isTodayCell -> MaterialTheme.colorScheme.onSecondaryContainer
                                            colIdx == 6 -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            } else {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .size(36.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف", fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { onConfirmDate(currentPreviewStr) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("confirm_jalali_date_btn")
                    ) {
                        Text(
                            text = "تایید (${PersianCurrencyEngine.toPersianDigits(currentPreviewStr)})",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateShortcutPill(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}
