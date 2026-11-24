package com.example.ui.components

import android.content.Context
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.domain.ForeignCurrencyDebtPosition
import com.example.domain.ForeignDebtCurrency
import com.example.domain.OfflineCurrencyDebtConverterEngine
import com.example.ui.theme.LocalTarazinoColors
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private const val PREFS_NAME = "tarazino_prefs_v1"
private const val KEY_MANUAL_FX_RATES_JSON = "offline_manual_fx_rates_json_v1"
private const val KEY_FOREIGN_DEBT_POSITIONS_JSON = "offline_foreign_debt_positions_json_v1"

@Composable
fun OfflineCurrencyDebtConverterSection(
    currencyUnit: CurrencyUnit,
    privacyMasked: Boolean,
    domesticRemainingDebtToman: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ext = LocalTarazinoColors.current
    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var manualRates by remember {
        mutableStateOf(
            OfflineCurrencyDebtConverterEngine.deserializeRatesFromJson(
                prefs.getString(KEY_MANUAL_FX_RATES_JSON, null)
            )
        )
    }
    var positions by remember {
        mutableStateOf(
            OfflineCurrencyDebtConverterEngine.deserializePositionsFromJson(
                prefs.getString(KEY_FOREIGN_DEBT_POSITIONS_JSON, null)
            )
        )
    }

    fun persistRates(updated: Map<ForeignDebtCurrency, Long>) {
        manualRates = updated
        prefs.edit()
            .putString(
                KEY_MANUAL_FX_RATES_JSON,
                OfflineCurrencyDebtConverterEngine.serializeRatesToJson(updated)
            )
            .apply()
    }

    fun persistPositions(updated: List<ForeignCurrencyDebtPosition>) {
        positions = updated
        prefs.edit()
            .putString(
                KEY_FOREIGN_DEBT_POSITIONS_JSON,
                OfflineCurrencyDebtConverterEngine.serializePositionsToJson(updated)
            )
            .apply()
    }

    var selectedCurrency by remember { mutableStateOf(ForeignDebtCurrency.USD) }
    val activeRateToman = manualRates[selectedCurrency] ?: selectedCurrency.defaultManualRateToman

    // Manual exchange rate text field state (in Toman per 1 unit of selected currency)
    var rateInputText by remember(selectedCurrency, activeRateToman) {
        mutableStateOf(activeRateToman.toString())
    }

    // Conversion direction: true = Foreign -> Toman/Rial, false = Toman -> Foreign
    var isForeignToTomanMode by remember { mutableStateOf(true) }
    var foreignAmountInputText by remember(selectedCurrency) {
        mutableStateOf(if (selectedCurrency == ForeignDebtCurrency.GOLD_GRAM) "3.5" else "500")
    }
    var tomanAmountInputText by remember(selectedCurrency) {
        mutableStateOf("36000000")
    }

    // Add new multi-currency debt position panel state
    var showAddDebtForm by remember { mutableStateOf(false) }
    var newDebtTitle by remember { mutableStateOf("") }
    var newDebtCounterparty by remember { mutableStateOf("") }
    var showCrossMatrix by remember { mutableStateOf(false) }

    val parsedRateToman = rateInputText.toLongOrNull()?.coerceAtLeast(1L) ?: activeRateToman
    val effectiveForeignAmount = if (isForeignToTomanMode) {
        foreignAmountInputText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    } else {
        val tomanIn = tomanAmountInputText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        OfflineCurrencyDebtConverterEngine.convertTomanToForeign(tomanIn, parsedRateToman)
    }

    val liveRatesWithCurrentInput = remember(manualRates, selectedCurrency, parsedRateToman) {
        manualRates.toMutableMap().apply {
            put(selectedCurrency, parsedRateToman)
        }
    }

    val conversionResult = remember(selectedCurrency, effectiveForeignAmount, liveRatesWithCurrentInput) {
        OfflineCurrencyDebtConverterEngine.evaluateConversion(
            sourceCurrency = selectedCurrency,
            sourceForeignAmount = effectiveForeignAmount,
            manualRates = liveRatesWithCurrentInput
        )
    }

    val portfolioSummary = remember(positions, liveRatesWithCurrentInput, domesticRemainingDebtToman) {
        OfflineCurrencyDebtConverterEngine.evaluatePortfolioSummary(
            positions = positions,
            manualRates = liveRatesWithCurrentInput,
            domesticRemainingDebtToman = domesticRemainingDebtToman
        )
    }

    TarazinoGlassCard(
        cornerRadius = 24.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("offline_currency_debt_converter_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            // 1. Header with Offline Status Pill
            TarazinoSectionHeader(
                title = "مبدل آفلاین ارز و ردیاب بدهی چندارزی / طلا",
                subtitle = "تبدیل دوطرفه و پایش بدهی‌های ارزی با نرخ دستی کاربر (بدون نیاز به اینترنت)",
                icon = Icons.Default.CurrencyExchange,
                accentColor = MaterialTheme.colorScheme.primary,
                trailing = {
                    TarazinoStatusPill(
                        text = "${PersianCurrencyEngine.toPersianDigits(portfolioSummary.activePositionsCount.toString())} پرونده ارزی",
                        accentColor = MaterialTheme.colorScheme.secondary
                    )
                }
            )

            // 2. Currency Selector Chips (USD, EUR, AED, GOLD18, USDT, TRY)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                ForeignDebtCurrency.entries.forEach { curr ->
                    val isSelected = curr == selectedCurrency
                    val cAccent = Color(curr.accentColorHex)
                    val savedRate = liveRatesWithCurrentInput[curr] ?: curr.defaultManualRateToman

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            cAccent.copy(alpha = if (ext.isDark) 0.24f else 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.35.dp else 0.75.dp,
                            color = if (isSelected) cAccent else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                selectedCurrency = curr
                                val nextRate = manualRates[curr] ?: curr.defaultManualRateToman
                                rateInputText = nextRate.toString()
                            }
                            .testTag("fx_currency_chip_${curr.code}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = curr.shortLabelFa,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) cAccent else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "(${PersianCurrencyEngine.isolateLtr(curr.symbol)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = PersianCurrencyEngine.formatAmount(
                                    amountToman = savedRate,
                                    unit = currencyUnit,
                                    includeUnit = true,
                                    privacyMasked = privacyMasked
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) cAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 3. User-Definable Manual Exchange Rate Box + Quick Sensitivity Adjusters
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.68f else 0.90f),
                border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نرخ تبدیل دستی ۱ ${selectedCurrency.shortLabelFa} (به تومان):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = PersianCurrencyEngine.formatAmount(
                                amountToman = parsedRateToman,
                                unit = currencyUnit,
                                includeUnit = true,
                                privacyMasked = privacyMasked
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = PersianCurrencyEngine.toPersianDigits(rateInputText),
                        onValueChange = { raw ->
                            val clean = PersianCurrencyEngine.toEnglishDigits(raw)
                                .filter { it.isDigit() }
                                .take(11)
                            rateInputText = clean
                            val parsed = clean.toLongOrNull()
                            if (parsed != null && parsed > 0L) {
                                persistRates(
                                    manualRates.toMutableMap().apply {
                                        put(selectedCurrency, parsed)
                                    }
                                )
                            }
                        },
                        label = {
                            Text(
                                text = "نرخ دستی هر ۱ ${selectedCurrency.shortLabelFa} (تومان)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_exchange_rate_input")
                    )

                    // Quick Rate Sensitivity Adjusters (-5%, -1%, +1%, +5%, Reset)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            -0.05 to PersianCurrencyEngine.isolateLtr("−۵٪"),
                            -0.01 to PersianCurrencyEngine.isolateLtr("−۱٪"),
                            0.01 to PersianCurrencyEngine.isolateLtr("+۱٪"),
                            0.05 to PersianCurrencyEngine.isolateLtr("+۵٪")
                        ).forEach { (pctDelta, labelFa) ->
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable {
                                        val adjusted = (parsedRateToman.toDouble() * (1.0 + pctDelta))
                                            .roundToLong()
                                            .coerceAtLeast(1L)
                                        rateInputText = adjusted.toString()
                                        persistRates(
                                            manualRates.toMutableMap().apply {
                                                put(selectedCurrency, adjusted)
                                            }
                                        )
                                    }
                            ) {
                                Text(
                                    text = "نوسان $labelFa",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                            border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .clickable {
                                    val defRate = selectedCurrency.defaultManualRateToman
                                    rateInputText = defRate.toString()
                                    persistRates(
                                        manualRates.toMutableMap().apply {
                                            put(selectedCurrency, defRate)
                                        }
                                    )
                                }
                                .testTag("reset_manual_rate_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "نرخ پایه",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            // 4. Bidirectional Debt Amount Converter Card (Foreign <-> Toman / Rial)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (ext.isDark) 0.34f else 0.52f),
                border = BorderStroke(0.95.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isForeignToTomanMode) {
                                "تبدیل بدهی ${selectedCurrency.shortLabelFa} به تومان و ریال"
                            } else {
                                "تبدیل بدهی تومانی به ${selectedCurrency.shortLabelFa}"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (isForeignToTomanMode) {
                                        tomanAmountInputText = conversionResult.equivalentToman.toString()
                                    } else {
                                        val formattedNum = "%.2f".format(Locale.US, effectiveForeignAmount)
                                            .trimEnd('0')
                                            .trimEnd('.')
                                        foreignAmountInputText = formattedNum.ifBlank { "0" }
                                    }
                                    isForeignToTomanMode = !isForeignToTomanMode
                                }
                                .testTag("swap_conversion_direction_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "تغییر جهت تبدیل",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "تبدیل معکوس",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (isForeignToTomanMode) {
                        OutlinedTextField(
                            value = PersianCurrencyEngine.toPersianDigits(foreignAmountInputText),
                            onValueChange = { raw ->
                                val normalized = PersianCurrencyEngine.toEnglishDigits(raw)
                                    .replace('٫', '.')
                                    .filter { it.isDigit() || it == '.' }
                                if (normalized.count { it == '.' } <= 1 && normalized.length <= 12) {
                                    foreignAmountInputText = normalized
                                }
                            },
                            label = {
                                Text(
                                    text = "مبلغ بدهی به ${selectedCurrency.labelFa}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("converter_foreign_amount_input")
                        )
                    } else {
                        OutlinedTextField(
                            value = PersianCurrencyEngine.toPersianDigits(tomanAmountInputText),
                            onValueChange = { raw ->
                                tomanAmountInputText = PersianCurrencyEngine.toEnglishDigits(raw)
                                    .filter { it.isDigit() }
                                    .take(13)
                            },
                            label = {
                                Text(
                                    text = "مبلغ بدهی به تومان",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("converter_toman_amount_input")
                        )
                    }

                    // Live Converted Readout Box
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = if (ext.isDark) 0.72f else 0.94f),
                        border = BorderStroke(0.8.dp, ext.cardBorderTop),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("converter_output_summary_box")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "معادل دقیق تومانی:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FormattedMoneyText(
                                    amountToman = conversionResult.equivalentToman,
                                    unit = CurrencyUnit.TOMAN,
                                    privacyMasked = privacyMasked,
                                    style = MaterialTheme.typography.titleSmall,
                                    numberColor = MaterialTheme.colorScheme.primary,
                                    minFontSize = 10.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "معادل ریالی / ارزی:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${
                                        OfflineCurrencyDebtConverterEngine.formatForeignAmount(
                                            conversionResult.sourceForeignAmount,
                                            selectedCurrency
                                        )
                                    } • ${
                                        PersianCurrencyEngine.formatAmount(
                                            amountToman = conversionResult.equivalentToman,
                                            unit = CurrencyUnit.RIAL,
                                            includeUnit = true,
                                            privacyMasked = privacyMasked
                                        )
                                    }",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            if (!privacyMasked && conversionResult.equivalentToman > 0L) {
                                TarazinoAutoSizeText(
                                    text = PersianCurrencyEngine.amountToPersianWords(
                                        conversionResult.equivalentToman,
                                        currencyUnit
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    minFontSize = 8.sp
                                )
                            }
                        }
                    }

                    // Action Row: Toggle Cross-Currency Matrix & Add to Multi-Currency Debt Tracker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddDebtForm = !showAddDebtForm },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1.25f)
                                .testTag("open_add_foreign_debt_form_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            TarazinoAutoSizeText(
                                text = "ثبت در سبد بدهی ارزی",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                minFontSize = 8.5.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { showCrossMatrix = !showCrossMatrix },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("toggle_cross_currency_matrix_btn")
                        ) {
                            TarazinoAutoSizeText(
                                text = if (showCrossMatrix) "بستن جدول برابری" else "برابری ۶ ارز/طلا",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                minFontSize = 8.5.sp
                            )
                        }
                    }

                    // Expandable Cross-Currency Equivalence Matrix
                    AnimatedVisibility(
                        visible = showCrossMatrix,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cross_currency_matrix_panel")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "معادل این بدهی در سایر ارزها و طلای ۱۸ عیار (با نرخ‌های دستی شما):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                conversionResult.crossConversions.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${row.currency.labelFa} (نرخ: ${
                                                PersianCurrencyEngine.isolateLtr(
                                                    PersianCurrencyEngine.formatNumberWithCommas(row.manualRateToman)
                                                )
                                            })",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = row.formattedForeignAmountFa,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(row.currency.accentColorHex)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Expandable Form to Add Converted Amount as a Tracked Multi-Currency Debt Position
                    AnimatedVisibility(
                        visible = showAddDebtForm,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            border = BorderStroke(0.85.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_foreign_debt_form_panel")
                        ) {
                            Column(
                                modifier = Modifier.padding(11.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "افزودن بدهی ${selectedCurrency.shortLabelFa} به مقدار ${
                                        OfflineCurrencyDebtConverterEngine.formatForeignAmount(
                                            conversionResult.sourceForeignAmount,
                                            selectedCurrency
                                        )
                                    }",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                OutlinedTextField(
                                    value = newDebtTitle,
                                    onValueChange = { newDebtTitle = it },
                                    label = { Text("عنوان بدهی ارزی / طلا (مثلاً قرض دلاری)", maxLines = 1) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(11.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("new_foreign_debt_title_input")
                                )

                                OutlinedTextField(
                                    value = newDebtCounterparty,
                                    onValueChange = { newDebtCounterparty = it },
                                    label = { Text("نام طلبکار / طرف‌حساب", maxLines = 1) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(11.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("new_foreign_debt_counterparty_input")
                                )

                                Button(
                                    onClick = {
                                        val amountToSave = conversionResult.sourceForeignAmount.coerceAtLeast(0.01)
                                        val nextId = (positions.maxOfOrNull { it.id } ?: 0L) + 1L
                                        val newPos = ForeignCurrencyDebtPosition(
                                            id = nextId,
                                            titleFa = newDebtTitle.trim()
                                                .ifBlank { "بدهی ${selectedCurrency.shortLabelFa}" },
                                            counterpartyNameFa = newDebtCounterparty.trim()
                                                .ifBlank { "طرف‌حساب ارزی" },
                                            currency = selectedCurrency,
                                            totalForeignAmount = amountToSave,
                                            paidForeignAmount = 0.0,
                                            entryExchangeRateToman = parsedRateToman,
                                            dueDateJalali = JalaliCalendarEngine.addJalaliMonths(
                                                JalaliCalendarEngine.todayJalali(),
                                                1
                                            )
                                        )
                                        persistPositions(listOf(newPos) + positions)
                                        newDebtTitle = ""
                                        newDebtCounterparty = ""
                                        showAddDebtForm = false
                                    },
                                    shape = RoundedCornerShape(11.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("confirm_save_foreign_debt_btn")
                                ) {
                                    Text(
                                        text = "ذخیره در سبد بدهی‌های چندارزی",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Multi-Currency Debt Portfolio Summary & Tracked Positions
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                thickness = 0.75.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.65f else 0.88f),
                    border = BorderStroke(0.8.dp, ext.cardBorderTop),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
                        Text(
                            text = "جمع بدهی‌های ارزی/طلا (نرخ روز)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        FormattedMoneyText(
                            amountToman = portfolioSummary.totalForeignDebtEquivalentToman,
                            unit = currencyUnit,
                            privacyMasked = privacyMasked,
                            style = MaterialTheme.typography.titleSmall,
                            numberColor = MaterialTheme.colorScheme.secondary,
                            minFontSize = 9.5.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.65f else 0.88f),
                    border = BorderStroke(0.8.dp, ext.cardBorderTop),
                    modifier = Modifier.weight(1f)
                ) {
                    val fxDelta = portfolioSummary.totalFxDeltaToman
                    val deltaColor = when {
                        fxDelta > 0L -> ext.danger
                        fxDelta < 0L -> ext.positive
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
                        Text(
                            text = if (fxDelta >= 0L) "اختلاف نوسان نرخ ارز" else "صرفه‌جویی کاهش نرخ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        FormattedMoneyText(
                            amountToman = abs(fxDelta),
                            unit = currencyUnit,
                            privacyMasked = privacyMasked,
                            style = MaterialTheme.typography.titleSmall,
                            numberColor = deltaColor,
                            minFontSize = 9.5.sp
                        )
                    }
                }
            }

            // Tracked Multi-Currency Debt Items List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                portfolioSummary.evaluatedPositions.forEach { item ->
                    val pos = item.position
                    val currColor = Color(pos.currency.accentColorHex)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ext.elevatedSurface.copy(alpha = if (ext.isDark) 0.56f else 0.80f),
                        border = BorderStroke(
                            0.8.dp,
                            if (pos.isSettled) ext.positive.copy(alpha = 0.4f) else currColor.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("foreign_debt_position_row_${pos.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
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
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(currColor.copy(alpha = 0.18f))
                                    ) {
                                        Text(
                                            text = pos.currency.symbol,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = currColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        TarazinoAutoSizeText(
                                            text = pos.titleFa,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            minFontSize = 9.5.sp
                                        )
                                        Text(
                                            text = "${pos.counterpartyNameFa} • نرخ ثبت: ${
                                                PersianCurrencyEngine.isolateLtr(
                                                    PersianCurrencyEngine.formatNumberWithCommas(pos.entryExchangeRateToman)
                                                )
                                            } تومان",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = OfflineCurrencyDebtConverterEngine.formatForeignAmount(
                                            pos.remainingForeignAmount,
                                            pos.currency
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (pos.isSettled) ext.positive else currColor
                                    )
                                    FormattedMoneyText(
                                        amountToman = item.remainingTomanAtCurrentRate,
                                        unit = currencyUnit,
                                        privacyMasked = privacyMasked,
                                        style = MaterialTheme.typography.labelSmall,
                                        numberColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        minFontSize = 8.5.sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val pctText = when {
                                    pos.isSettled -> "تسویه‌شده ✨"
                                    item.fxChangePercent > 0 -> "${PersianCurrencyEngine.isolateLtr("+${PersianCurrencyEngine.toPersianDigits(item.fxChangePercent.toString())}٪")} افزایش نرخ نسبت به زمان ثبت"
                                    item.fxChangePercent < 0 -> "${PersianCurrencyEngine.isolateLtr("−${PersianCurrencyEngine.toPersianDigits(kotlin.math.abs(item.fxChangePercent).toString())}٪")} کاهش نرخ نسبت به زمان ثبت"
                                    else -> "بدون تغییر نرخ نسبت به زمان ثبت"
                                }
                                val pctColor = when {
                                    pos.isSettled -> ext.positive
                                    item.fxChangePercent > 0 -> ext.warning
                                    item.fxChangePercent < 0 -> ext.positive
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Text(
                                    text = pctText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = pctColor,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (!pos.isSettled) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = ext.positive.copy(alpha = 0.16f),
                                            border = BorderStroke(0.7.dp, ext.positive.copy(alpha = 0.42f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    persistPositions(
                                                        positions.map { existing ->
                                                            if (existing.id == pos.id) {
                                                                existing.copy(paidForeignAmount = existing.totalForeignAmount)
                                                            } else {
                                                                existing
                                                            }
                                                        }
                                                    )
                                                }
                                                .testTag("settle_foreign_debt_btn_${pos.id}")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = ext.positive,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "تسویه",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ext.positive
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            persistPositions(positions.filterNot { it.id == pos.id })
                                        },
                                        modifier = Modifier
                                            .size(26.dp)
                                            .testTag("delete_foreign_debt_btn_${pos.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "حذف بدهی ارزی",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
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
}
