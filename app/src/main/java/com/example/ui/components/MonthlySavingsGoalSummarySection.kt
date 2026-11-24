package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyUnit
import com.example.core.PersianCurrencyEngine
import com.example.domain.MonthlySavingsGoalSummary
import com.example.ui.theme.LocalTarazinoColors

@Composable
fun MonthlySavingsGoalDashboardSummarySection(
    summary: MonthlySavingsGoalSummary,
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    onQuickDepositDelta: (Long) -> Unit,
    onUpdateSavingsGoal: (titleFa: String, targetAmountToman: Long, savedAmountToman: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTarazinoColors.current
    val statusColor = Color(summary.status.badgeColorHex)
    var isEditorExpanded by remember { mutableStateOf(false) }

    var titleInput by remember(summary.goalTitleFa) { mutableStateOf(summary.goalTitleFa) }
    var targetInput by remember(summary.targetAmountToman) {
        mutableStateOf(summary.targetAmountToman.toString())
    }
    var savedInput by remember(summary.savedAmountToman) {
        mutableStateOf(summary.savedAmountToman.toString())
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.68f else 0.90f),
        border = BorderStroke(
            width = 0.95.dp,
            color = if (summary.isTargetAchieved) {
                ext.positive.copy(alpha = 0.52f)
            } else {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_monthly_savings_goal_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Header Row: Savings Icon, Title + Shamsi Month, Status Badge & Edit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            imageVector = if (summary.isTargetAchieved) Icons.Default.CheckCircle else Icons.Default.Savings,
                            contentDescription = null,
                            tint = if (summary.isTargetAchieved) ext.positive else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        TarazinoAutoSizeText(
                            text = summary.goalTitleFa,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            minFontSize = 9.5.sp
                        )
                        TarazinoAutoSizeText(
                            text = "هدف پس‌انداز ${summary.shamsiMonthNameFa} ${PersianCurrencyEngine.toPersianDigits(summary.shamsiYear.toString())}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            minFontSize = 8.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TarazinoStatusPill(
                        text = summary.status.labelFa,
                        accentColor = statusColor
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { isEditorExpanded = !isEditorExpanded }
                            .testTag("toggle_savings_goal_editor_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تنظیم هدف پس‌انداز",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isEditorExpanded) "بستن" else "هدف‌گذاری",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 2. Saved vs. Target Amounts + Percentage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "پس‌انداز ثبت‌شده این ماه",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FormattedMoneyText(
                        amountToman = summary.savedAmountToman,
                        unit = currencyUnit,
                        privacyMasked = privacyMasked,
                        style = MaterialTheme.typography.titleSmall,
                        numberColor = MaterialTheme.colorScheme.primary,
                        unitColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        minFontSize = 9.5.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("savings_goal_saved_amount_text")
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusColor.copy(alpha = 0.16f),
                    border = BorderStroke(0.75.dp, statusColor.copy(alpha = 0.45f)),
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = "${PersianCurrencyEngine.toPersianDigits(summary.progressPercent.toString())}٪ تحقق",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                            .testTag("savings_goal_progress_percent_badge")
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "هدف ماهانه",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FormattedMoneyText(
                        amountToman = summary.targetAmountToman,
                        unit = currencyUnit,
                        privacyMasked = privacyMasked,
                        style = MaterialTheme.typography.titleSmall,
                        numberColor = MaterialTheme.colorScheme.secondary,
                        unitColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                        minFontSize = 9.5.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("savings_goal_target_amount_text")
                    )
                }
            }

            // 3. Luminous Progress Bar
            TarazinoLuminousProgressBar(
                progress = summary.progressRatio,
                color = if (summary.isTargetAchieved) ext.positive else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                height = 9.dp
            )

            // 4. Daily Rate / Remaining Hint Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val leftText = if (summary.isTargetAchieved) {
                    if (summary.surplusOverTargetToman > 0L) {
                        val surplusFmt = PersianCurrencyEngine.formatAmount(
                            amountToman = summary.surplusOverTargetToman,
                            unit = currencyUnit,
                            includeUnit = true,
                            privacyMasked = privacyMasked
                        )
                        "مازاد بر هدف: $surplusFmt"
                    } else {
                        "هدف پس‌انداز ${summary.shamsiMonthNameFa} کامل شد!"
                    }
                } else {
                    val remFmt = PersianCurrencyEngine.formatAmount(
                        amountToman = summary.remainingToTargetToman,
                        unit = currencyUnit,
                        includeUnit = true,
                        privacyMasked = privacyMasked
                    )
                    "مانده تا هدف: $remFmt"
                }

                val rightText = if (!summary.isTargetAchieved && summary.daysRemainingInMonth > 0) {
                    val dailyFmt = PersianCurrencyEngine.formatAmount(
                        amountToman = summary.requiredDailySavingsToman,
                        unit = currencyUnit,
                        includeUnit = true,
                        privacyMasked = privacyMasked
                    )
                    "روزی $dailyFmt تا پایان ماه"
                } else {
                    "ظرفیت آزاد ماه: ${
                        PersianCurrencyEngine.formatAmount(
                            amountToman = summary.estimatedMonthlyFreeCashflowToman,
                            unit = currencyUnit,
                            includeUnit = true,
                            privacyMasked = privacyMasked
                        )
                    }"
                }

                TarazinoAutoSizeText(
                    text = leftText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (summary.isTargetAchieved) ext.positive else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    minFontSize = 8.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TarazinoAutoSizeText(
                    text = rightText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    minFontSize = 8.sp,
                    modifier = Modifier.weight(1f)
                )
            }

            // 5. Quick 1-Tap Savings Deposit & Adjustment Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val quickPresets = listOf(
                    500_000L to "+۵۰۰ هزار",
                    1_000_000L to "+۱ میلیون",
                    2_000_000L to "+۲ میلیون"
                )
                quickPresets.forEach { (deltaToman, labelFa) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (ext.isDark) 0.16f else 0.11f),
                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .tarazinoPressScale(scaleDown = 0.93f) {
                                onQuickDepositDelta(deltaToman)
                            }
                            .testTag("savings_quick_deposit_${deltaToman}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = labelFa,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (summary.savedAmountToman >= 500_000L) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .tarazinoPressScale(scaleDown = 0.93f) {
                                onQuickDepositDelta(-500_000L)
                            }
                            .testTag("savings_quick_withdraw_500000")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "کسر ۵۰۰ هزار",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 6. Expandable Inline Goal Target & Saved Amount Editor
            AnimatedVisibility(
                visible = isEditorExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (ext.isDark) 0.78f else 0.95f),
                    border = BorderStroke(0.85.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .testTag("monthly_savings_goal_editor_panel")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تنظیم هدف و مبلغ پس‌انداز ماهانه (تومان)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.42f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable {
                                        targetInput = summary.recommendedTargetToman.toString()
                                    }
                                    .testTag("apply_recommended_savings_target_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "پیشنهاد هوشمند",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = {
                                Text("عنوان هدف پس‌انداز", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("savings_goal_title_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = PersianCurrencyEngine.toPersianDigits(targetInput),
                                onValueChange = { raw ->
                                    targetInput = PersianCurrencyEngine.toEnglishDigits(raw)
                                        .filter { it.isDigit() }
                                        .take(12)
                                },
                                label = {
                                    Text("هدف ماهانه (تومان)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("savings_goal_target_input")
                            )

                            OutlinedTextField(
                                value = PersianCurrencyEngine.toPersianDigits(savedInput),
                                onValueChange = { raw ->
                                    savedInput = PersianCurrencyEngine.toEnglishDigits(raw)
                                        .filter { it.isDigit() }
                                        .take(12)
                                },
                                label = {
                                    Text("ذخیره فعلی (تومان)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("savings_goal_saved_input")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val parsedTarget = targetInput.toLongOrNull() ?: summary.targetAmountToman
                                    val parsedSaved = savedInput.toLongOrNull() ?: summary.savedAmountToman
                                    onUpdateSavingsGoal(titleInput, parsedTarget, parsedSaved)
                                    isEditorExpanded = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .testTag("save_monthly_savings_goal_btn")
                            ) {
                                Text(
                                    text = "ذخیره هدف پس‌انداز",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = { isEditorExpanded = false },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text(
                                    text = "انصراف",
                                    style = MaterialTheme.typography.labelLarge,
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
