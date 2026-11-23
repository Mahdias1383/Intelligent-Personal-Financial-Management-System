package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyUnit
import com.example.core.PersianCurrencyEngine

@Composable
fun MoneyInputField(
    labelFa: String,
    amountToman: Long,
    onAmountChange: (Long) -> Unit,
    currencyUnit: CurrencyUnit = CurrencyUnit.TOMAN,
    modifier: Modifier = Modifier,
    showQuickChips: Boolean = true,
    testTagName: String = "money_input_field"
) {
    var rawText by remember(amountToman) {
        mutableStateOf(
            if (amountToman == 0L) ""
            else PersianCurrencyEngine.formatNumberWithCommas(amountToman, usePersianDigits = true)
        )
    }

    val quickIncrements = listOf(
        "+۵۰۰ هزار" to 500_000L,
        "+۱ میلیون" to 1_000_000L,
        "+۵ میلیون" to 5_000_000L,
        "+۱۰ میلیون" to 10_000_000L
    )

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = rawText,
            onValueChange = { input ->
                rawText = PersianCurrencyEngine.toPersianDigits(input)
                val evaluated = PersianCurrencyEngine.evaluateInlineMath(input)
                if (evaluated != null) {
                    onAmountChange(evaluated)
                }
            },
            label = {
                Text(
                    text = "$labelFa (تومان)",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            placeholder = {
                Text(
                    text = "مثلاً ۱۵,۴۰۰,۰۰۰ یا ۱۵۰۰۰۰۰۰+۴۰۰۰۰۰",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "تومان",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                    if (amountToman > 0L) {
                        IconButton(
                            onClick = {
                                rawText = ""
                                onAmountChange(0L)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "پاک کردن مبلغ",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTagName)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Live Persian Currency Wordifier Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                val formattedNumeric = PersianCurrencyEngine.formatAmount(
                    amountToman = amountToman,
                    unit = currencyUnit,
                    includeUnit = true
                )
                val wordified = PersianCurrencyEngine.amountToPersianWords(
                    amountToman = amountToman,
                    unit = currencyUnit
                )
                TarazinoAutoSizeText(
                    text = "${ltrIsolateDigits(formattedNumeric)} — $wordified",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    minFontSize = 9.5.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showQuickChips) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                quickIncrements.forEach { (chipLabel, deltaToman) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val nextVal = (amountToman + deltaToman).coerceAtLeast(0L)
                                rawText = PersianCurrencyEngine.formatNumberWithCommas(nextVal, true)
                                onAmountChange(nextVal)
                            }
                    ) {
                        Text(
                            text = chipLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JalaliDateSelectorField(
    labelFa: String,
    dateJalali: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTagName: String = "jalali_date_selector"
) {
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { showPicker = true }
            .testTag(testTagName)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                TarazinoIconBadge(
                    icon = Icons.Default.CalendarMonth,
                    accentColor = MaterialTheme.colorScheme.primary,
                    size = 38.dp,
                    iconSize = 19.dp,
                    cornerRadius = 11.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    TarazinoAutoSizeText(
                        text = labelFa,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        minFontSize = 8.5.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    TarazinoAutoSizeText(
                        text = JalaliCalendarEngineSafeVerbose(dateJalali),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        minFontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            TarazinoStatusPill(
                text = PersianCurrencyEngine.toPersianDigits(dateJalali),
                accentColor = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showPicker) {
        JalaliDatePickerModal(
            initialDateJalali = dateJalali,
            onDismiss = { showPicker = false },
            onConfirmDate = { selected ->
                onDateSelected(selected)
                showPicker = false
            }
        )
    }
}

private fun JalaliCalendarEngineSafeVerbose(dateStr: String): String {
    return com.example.core.JalaliCalendarEngine.formatJalaliVerbose(dateStr)
}
