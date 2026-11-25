package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.CurrencyUnit
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.export.ReportExportEngine
import com.example.ui.TarazinoNavTab
import com.example.ui.TarazinoUiState
import com.example.ui.TarazinoViewModel
import androidx.compose.animation.core.FastOutSlowInEasing
import com.example.data.InstallmentStatus
import com.example.ui.components.A4ReportPreviewModal
import com.example.ui.components.InteractiveOnboardingTourOverlay
import com.example.ui.components.LoanContractScannerModal
import com.example.ui.components.LocalSpotlightRegistry
import com.example.ui.components.SecurityLockOverlay
import com.example.ui.components.SpotlightTargetRegistry
import com.example.ui.components.StepByStepObligationWizardModal
import com.example.ui.components.TarazinoAutoSizeText
import com.example.ui.components.TarazinoCelebrationConfettiBurst
import com.example.ui.components.tarazinoAmbientBackdrop
import com.example.ui.components.tarazinoPressScale
import com.example.ui.components.tourSpotlightTarget
import com.example.ui.screens.ConnectivityPrivacyScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.IntelligenceScreen
import com.example.ui.screens.ReportsAndSettingsScreen
import com.example.ui.theme.ThemeSwitcherEngine
import com.example.ui.screens.TreePortfolioScreen
import com.example.ui.theme.LocalTarazinoColors
import com.example.ui.theme.TarazinoTheme

import android.Manifest
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.example.ai.configureAppCheck
import com.example.notification.ShamsiInstallmentNotificationManager

private data class NavBarDestination(
    val tab: TarazinoNavTab,
    val labelFa: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // STEP 1: Configure Firebase App Check with launch intent before any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        ShamsiInstallmentNotificationManager.ensureChannelAndSchedulePeriodicWork(this)
        setContent {
            val viewModel: TarazinoViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val preventScreenshots = uiState.preferences.connectivityPrivacy.preventScreenshotsAndRecents
            LaunchedEffect(preventScreenshots) {
                if (preventScreenshots) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            TarazinoTheme(
                themeMode = uiState.preferences.themeMode,
                fontScale = uiState.preferences.fontScale
            ) {
                TarazinoAppRoot(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TarazinoAppRoot(
    uiState: TarazinoUiState,
    viewModel: TarazinoViewModel
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            ShamsiInstallmentNotificationManager.ensureChannelAndSchedulePeriodicWork(context)
        }
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ShamsiInstallmentNotificationManager.isBackgroundReminderEnabled(context) &&
            !ShamsiInstallmentNotificationManager.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val spotlightRegistry = remember { SpotlightTargetRegistry() }
    val extColors = LocalTarazinoColors.current
    val todayShortHeader = remember {
        val parsed = JalaliCalendarEngine.parseJalali(JalaliCalendarEngine.todayJalali())
        "${PersianCurrencyEngine.toPersianDigits(parsed.day.toString())} ${JalaliCalendarEngine.getMonthName(parsed.month)}"
    }

    val activeTourStep = if (uiState.isTourActive) {
        viewModel.guidedTourSteps.getOrNull(uiState.currentTourStepIndex)
    } else {
        null
    }
    val activeTabScrollIndex = activeTourStep
        ?.takeIf { it.targetTab == uiState.activeTab }
        ?.scrollItemIndex ?: 0

    // Handle hierarchical tree step-back and non-dashboard tab back navigation
    if (uiState.activeTab != TarazinoNavTab.DASHBOARD) {
        BackHandler {
            if (uiState.activeTab == TarazinoNavTab.PORTFOLIO_TREE) {
                val steppedBack = viewModel.stepBackInTree()
                if (!steppedBack) {
                    viewModel.selectTab(TarazinoNavTab.DASHBOARD)
                }
            } else {
                viewModel.selectTab(TarazinoNavTab.DASHBOARD)
            }
        }
    }

    val navDestinations = remember {
        listOf(
            NavBarDestination(
                tab = TarazinoNavTab.DASHBOARD,
                labelFa = "داشبورد",
                selectedIcon = Icons.Filled.Dashboard,
                unselectedIcon = Icons.Outlined.Dashboard
            ),
            NavBarDestination(
                tab = TarazinoNavTab.PORTFOLIO_TREE,
                labelFa = "پوشه‌ها",
                selectedIcon = Icons.Filled.AccountTree,
                unselectedIcon = Icons.Outlined.AccountTree
            ),
            NavBarDestination(
                tab = TarazinoNavTab.INTELLIGENCE,
                labelFa = "هوش مالی",
                selectedIcon = Icons.Filled.Insights,
                unselectedIcon = Icons.Outlined.Insights
            ),
            NavBarDestination(
                tab = TarazinoNavTab.REPORTS_SETTINGS,
                labelFa = "گزارش‌ها",
                selectedIcon = Icons.Filled.PictureAsPdf,
                unselectedIcon = Icons.Outlined.PictureAsPdf
            )
        )
    }

    CompositionLocalProvider(LocalSpotlightRegistry provides spotlightRegistry) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .tarazinoAmbientBackdrop(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    auroraPrimary = extColors.auroraPrimary,
                    auroraSecondary = extColors.auroraSecondary,
                    auroraTertiary = extColors.auroraTertiary
                )
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            actionIconContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_app_logo_1794581824694),
                                    contentDescription = "لوگوی ترازینو",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            RoundedCornerShape(11.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    TarazinoAutoSizeText(
                                        text = "ترازینو",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        minFontSize = 15.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        com.example.ui.components.TarazinoLivePulseDot(
                                            color = MaterialTheme.colorScheme.primary,
                                            size = 6.dp
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        TarazinoAutoSizeText(
                                            text = "خزانه • $todayShortHeader",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            minFontSize = 8.5.sp
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = extColors.dockSurface,
                                border = BorderStroke(
                                    0.85.dp,
                                    Brush.verticalGradient(listOf(extColors.cardBorderTop, extColors.cardBorderBottom))
                                ),
                                shadowElevation = if (extColors.isDark) 0.dp else 3.dp,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .tourSpotlightTarget("top_bar_controls")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                                ) {
                                    // Currency Unit Quick Toggle Chip (تومان / ریال)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        border = BorderStroke(
                                            0.75.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
                                        ),
                                        modifier = Modifier
                                            .padding(start = 2.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                val nextUnit = if (uiState.preferences.currencyUnit == CurrencyUnit.TOMAN) {
                                                    CurrencyUnit.RIAL
                                                } else {
                                                    CurrencyUnit.TOMAN
                                                }
                                                viewModel.setCurrencyUnit(nextUnit)
                                            }
                                            .testTag("currency_unit_toggle_chip")
                                    ) {
                                        Text(
                                            text = uiState.preferences.currencyUnit.labelFa,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }

                                    // Quick 1-Tap Theme Switcher (Deep Teal Light <-> High-Contrast Late-Night Dark)
                                    val isDarkNow = ThemeSwitcherEngine.isDarkThemeMode(uiState.preferences.themeMode)
                                    val themeActionLabel = ThemeSwitcherEngine.nextToggleActionLabelFa(uiState.preferences.themeMode)
                                    IconButton(
                                        onClick = {
                                            viewModel.setThemeMode(
                                                ThemeSwitcherEngine.toggleDeepTealLightAndHighContrastDark(
                                                    uiState.preferences.themeMode
                                                )
                                            )
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("top_bar_theme_switcher_btn")
                                    ) {
                                        com.example.ui.components.TarazinoSpringScreenTransition(
                                            targetState = isDarkNow,
                                            slideOffsetPx = 10f
                                        ) { darkActive ->
                                            Icon(
                                                imageVector = if (darkActive) {
                                                    Icons.Default.LightMode
                                                } else {
                                                    Icons.Default.DarkMode
                                                },
                                                contentDescription = themeActionLabel,
                                                tint = if (darkActive) {
                                                    MaterialTheme.colorScheme.secondary
                                                } else {
                                                    MaterialTheme.colorScheme.primary
                                                },
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }

                                    // Privacy Mask Toggle Button
                                    IconButton(
                                        onClick = { viewModel.togglePrivacyMask() },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("privacy_mask_toggle_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.preferences.privacyMaskEnabled) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = "مخفی‌سازی ارقام مالی",
                                            tint = if (uiState.preferences.privacyMaskEnabled) {
                                                extColors.warning
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Interactive Guided Tour Launch Button
                                    IconButton(
                                        onClick = { viewModel.startGuidedTour() },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("top_bar_tour_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "اجرای تور آموزشی تعاملی",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Immediate Lock Button
                                    IconButton(
                                        onClick = { viewModel.lockAppImmediately() },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("top_bar_lock_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "قفل آنی برنامه",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    // Sleek Floating Glass Pill Dock + Properly Height-Reserved Center Emerald FAB (Zero Overlap!)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .height(74.dp)
                            .testTag("floating_quick_action_bar")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = extColors.dockSurface,
                            tonalElevation = 0.dp,
                            shadowElevation = if (extColors.isDark) 10.dp else 8.dp,
                            border = BorderStroke(
                                0.85.dp,
                                Brush.verticalGradient(
                                    listOf(extColors.cardBorderTop, extColors.cardBorderBottom)
                                )
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(58.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // First 2 Tabs (Dashboard, Portfolio Tree)
                                DockTabItem(
                                    dest = navDestinations[0],
                                    selected = uiState.activeTab == navDestinations[0].tab,
                                    onClick = { viewModel.selectTab(navDestinations[0].tab) },
                                    modifier = Modifier.weight(1f)
                                )
                                DockTabItem(
                                    dest = navDestinations[1],
                                    selected = uiState.activeTab == navDestinations[1].tab,
                                    onClick = { viewModel.selectTab(navDestinations[1].tab) },
                                    modifier = Modifier.weight(1f)
                                )

                                // Center Spacer Reserved for Elevated Emerald FAB
                                Spacer(modifier = Modifier.width(66.dp))

                                // Last 2 Tabs (Intelligence, Reports & Settings)
                                DockTabItem(
                                    dest = navDestinations[2],
                                    selected = uiState.activeTab == navDestinations[2].tab,
                                    onClick = { viewModel.selectTab(navDestinations[2].tab) },
                                    modifier = Modifier.weight(1f)
                                )
                                DockTabItem(
                                    dest = navDestinations[3],
                                    selected = uiState.activeTab == navDestinations[3].tab,
                                    onClick = { viewModel.selectTab(navDestinations[3].tab) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Elevated Center Emerald Jewel FAB seated cleanly inside its own 62dp mask ring (Draw-phase infiniteTransition!)
                        val primaryHaloColor = MaterialTheme.colorScheme.primary
                        val secondaryHaloColor = MaterialTheme.colorScheme.secondary
                        val fabHaloTransition = rememberInfiniteTransition(label = "FabHaloInfinite")
                        val fabHaloAngleState = fabHaloTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 12000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "FabHaloAngle"
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f))
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(62.dp)) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            primaryHaloColor.copy(alpha = 0.22f),
                                            Color.Transparent
                                        )
                                    ),
                                    radius = size.minDimension * 0.48f
                                )
                                rotate(degrees = fabHaloAngleState.value) {
                                    drawCircle(
                                        brush = Brush.sweepGradient(
                                            listOf(
                                                primaryHaloColor.copy(alpha = 0.88f),
                                                secondaryHaloColor.copy(alpha = 0.52f),
                                                primaryHaloColor.copy(alpha = 0.18f),
                                                secondaryHaloColor.copy(alpha = 0.45f),
                                                primaryHaloColor.copy(alpha = 0.88f)
                                            )
                                        ),
                                        radius = size.minDimension / 2f - 2.dp.toPx(),
                                        style = Stroke(width = 1.4.dp.toPx())
                                    )
                                }
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(50.dp)
                                    .shadow(
                                        elevation = 10.dp,
                                        shape = CircleShape,
                                        ambientColor = MaterialTheme.colorScheme.primary,
                                        spotColor = MaterialTheme.colorScheme.primary
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                extColors.fabStart,
                                                extColors.fabMid,
                                                extColors.fabEnd
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.1.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.65f),
                                                extColors.fabEnd
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .tarazinoPressScale(scaleDown = 0.88f) {
                                        viewModel.openStepByStepWizard(
                                            preselectedCategory = uiState.treeDrill.selectedCategory,
                                            preselectedCounterpartyId = uiState.treeDrill.selectedCounterpartyId
                                        )
                                    }
                                    .testTag("quick_bar_add_obligation_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "ثبت قسط، خرید اقساطی یا وام جدید",
                                    tint = extColors.fabOnContent,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Celebratory Banner on Installment Settlement
                    AnimatedVisibility(
                        visible = uiState.celebrationMessageFa != null,
                        enter = expandVertically(
                            animationSpec = com.example.ui.components.TarazinoMotionSpecs.fluidBouncyIntSizeSpring()
                        ) + fadeIn(
                            animationSpec = com.example.ui.components.TarazinoMotionSpecs.fluidBouncySpring()
                        ),
                        exit = shrinkVertically(
                            animationSpec = com.example.ui.components.TarazinoMotionSpecs.fluidBouncyIntSizeSpring()
                        ) + fadeOut(
                            animationSpec = com.example.ui.components.TarazinoMotionSpecs.snappyControlSpring()
                        )
                    ) {
                        uiState.celebrationMessageFa?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { viewModel.clearCelebration() }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
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
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "بستن",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Toast Feedback Banner
                    AnimatedVisibility(
                        visible = uiState.toastMessageFa != null,
                        enter = expandVertically(
                            animationSpec = com.example.ui.components.TarazinoMotionSpecs.fluidBouncyIntSizeSpring()
                        ) + fadeIn(
                            animationSpec = com.example.ui.components.TarazinoMotionSpecs.fluidBouncySpring()
                        ),
                        exit = shrinkVertically(
                            animationSpec = com.example.ui.components.TarazinoMotionSpecs.fluidBouncyIntSizeSpring()
                        ) + fadeOut(
                            animationSpec = com.example.ui.components.TarazinoMotionSpecs.snappyControlSpring()
                        )
                    ) {
                        uiState.toastMessageFa?.let { toast ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { viewModel.clearToast() }
                            ) {
                                Text(
                                    text = toast,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    // Active Tab Screen Content powered by Animatable + spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    Box(modifier = Modifier.weight(1f)) {
                        com.example.ui.components.TarazinoSpringScreenTransition(
                            targetState = uiState.activeTab,
                            slideOffsetPx = 38f,
                            directionResolver = { prev, curr -> if (curr.ordinal >= prev.ordinal) -1 else 1 },
                            modifier = Modifier.fillMaxSize()
                        ) { targetTab ->
                            when (targetTab) {
                                TarazinoNavTab.DASHBOARD -> DashboardScreen(
                                    uiState = uiState,
                                    tourScrollIndex = activeTabScrollIndex,
                                    onQuickPay = { viewModel.quickPayInstallment(it) },
                                    onSnoozeTomorrow = { viewModel.snoozeInstallmentTomorrow(it) },
                                    onOpenNewObligationWizard = { viewModel.openStepByStepWizard() },
                                    onOpenCategoryInTree = { cat -> viewModel.openCategoryFolderInTree(cat) },
                                    onOpenCounterpartyInTree = { cp -> viewModel.selectTreeCounterparty(cp) },
                                    onOpenObligationInTree = { ob, cat -> viewModel.selectTreeObligation(ob, cat) },
                                    onOpenIntelligenceTab = { viewModel.selectTab(TarazinoNavTab.INTELLIGENCE) },
                                    onOpenFullA4Report = { viewModel.openA4ReportPreview() },
                                    onQuickSavingsDepositDelta = { delta -> viewModel.recordMonthlySavingsDeposit(delta) },
                                    onUpdateMonthlySavingsGoal = { title, target, saved ->
                                        viewModel.updateMonthlySavingsGoal(title, target, saved)
                                    },
                                    onUpdateExtraPayoffBudget = { viewModel.updateExtraPayoffBudget(it) },
                                    onOpenContractScanner = { viewModel.openContractScanner() },
                                    onSaveAutomatedSchedule = { enabled, primaryDay, secondaryDay ->
                                        viewModel.saveAutomatedPaymentSchedule(enabled, primaryDay, secondaryDay)
                                    },
                                    onRefreshDailyAiTip = { viewModel.refreshDailyAiFinancialTip(forceRefresh = true) },
                                    onOpenConnectivityPrivacySettings = { viewModel.openConnectivityPrivacyScreen() },
                                    onMoveDashboardModule = { from, to -> viewModel.moveDashboardModule(from, to) },
                                    onMoveDashboardModuleByDelta = { id, delta ->
                                        viewModel.moveDashboardModuleByDelta(id, delta)
                                    },
                                    onPinDashboardModuleToTop = { id -> viewModel.pinDashboardModuleToTop(id) },
                                    onToggleDashboardModuleVisibility = { id ->
                                        viewModel.toggleDashboardModuleVisibility(id)
                                    },
                                    onApplyDashboardFocusPreset = { preset ->
                                        viewModel.applyDashboardFocusPreset(preset)
                                    },
                                    onResetDashboardLayout = { viewModel.resetDashboardLayoutToDefault() }
                                )

                                TarazinoNavTab.PORTFOLIO_TREE -> TreePortfolioScreen(
                                    uiState = uiState,
                                    tourScrollIndex = activeTabScrollIndex,
                                    onSelectCategory = { cat -> viewModel.openCategoryFolderInTree(cat) },
                                    onSelectCounterparty = { cp -> viewModel.selectTreeCounterparty(cp) },
                                    onSelectObligation = { ob, cat -> viewModel.selectTreeObligation(ob, cat) },
                                    onStepBackInTree = { viewModel.stepBackInTree() },
                                    onResetTreeToRoot = { viewModel.resetTreeToRoot() },
                                    onOpenWizard = { cat, cpId -> viewModel.openStepByStepWizard(cat, cpId) },
                                    onSaveCounterparty = { viewModel.saveCounterparty(it) },
                                    onDeleteCounterparty = { id, name -> viewModel.deleteCounterparty(id, name) },
                                    onDeleteObligation = { id, title -> viewModel.deleteObligation(id, title) },
                                    onUpdateObligationDetails = { ob -> viewModel.updateObligationDetails(ob) },
                                    onQuickPayInstallment = { viewModel.quickPayInstallment(it) },
                                    onBatchPayInstallments = { ids -> viewModel.batchPayInstallments(ids) },
                                    onDetailedInstallmentUpdate = { inst, title, baseAmt, dueDate, paid, fee, disc, paidDate, code, note ->
                                        viewModel.applyDetailedInstallmentPayment(
                                            installment = inst,
                                            customTitle = title,
                                            customBaseAmountToman = baseAmt,
                                            customDueDateJalali = dueDate,
                                            paidAmountToman = paid,
                                            lateFeeToman = fee,
                                            discountToman = disc,
                                            paidDateJalali = paidDate,
                                            trackingCode = code,
                                            note = note
                                        )
                                    },
                                    onAddSingleCustomInstallment = { obId, nextNum, title, dueDate, amt, isPaid, note ->
                                        viewModel.addSingleCustomInstallmentToObligation(
                                            obligationId = obId,
                                            nextNumber = nextNum,
                                            title = title,
                                            dueDateJalali = dueDate,
                                            amountToman = amt,
                                            isAlreadyPaid = isPaid,
                                            note = note
                                        )
                                    },
                                    onDeleteSingleInstallment = { id, title ->
                                        viewModel.deleteSingleInstallment(id, title)
                                    },
                                    onAddSubRecord = { ob, sub -> viewModel.addSubRecordToObligation(ob, sub) },
                                    onOpenCounterpartyA4Report = { cp ->
                                        viewModel.openA4ReportPreview(
                                            reportTitleFa = "صورتحساب اختصاصی زیرمجموعه: ${cp.name}",
                                            subtitleFa = "${cp.categoryEnum.labelFa} • ${cp.priorityEnum.labelFa}",
                                            filterCounterpartyId = cp.id
                                        )
                                    },
                                    onOpenObligationA4Report = { ob ->
                                        viewModel.openA4ReportPreview(
                                            reportTitleFa = "جدول رسمی اقساط: ${ob.title}",
                                            subtitleFa = "${ob.categoryEnum.labelFa} • ${ob.scheduleModeEnum.labelFa}",
                                            filterObligationId = ob.id
                                        )
                                    },
                                    onShowToast = { viewModel.showToast(it) }
                                )

                                TarazinoNavTab.INTELLIGENCE -> IntelligenceScreen(
                                    uiState = uiState,
                                    tourScrollIndex = activeTabScrollIndex,
                                    onUpdateExtraPayoffBudget = { viewModel.updateExtraPayoffBudget(it) },
                                    onChangeHeatmapMonth = { viewModel.changeHeatmapMonth(it) },
                                    onUpdateBudgetPlannerSettings = { inc, livingPct, days ->
                                        viewModel.updateBudgetPlannerSettings(inc, livingPct, days)
                                    },
                                    onQuickPayInstallment = { viewModel.quickPayInstallment(it) }
                                )

                                TarazinoNavTab.REPORTS_SETTINGS -> ReportsAndSettingsScreen(
                                    uiState = uiState,
                                    tourScrollIndex = activeTabScrollIndex,
                                    onOpenFullA4Report = { viewModel.openA4ReportPreview() },
                                    onOpenCounterpartyA4Report = { cp ->
                                        viewModel.openA4ReportPreview(
                                            reportTitleFa = "صورتحساب اختصاصی طرف‌حساب: ${cp.name}",
                                            subtitleFa = cp.categoryEnum.labelFa,
                                            filterCounterpartyId = cp.id
                                        )
                                    },
                                    onDirectExportPdf = {
                                        val payload = ReportExportEngine.buildReportPayload(
                                            reportTitleFa = "گزارش جامع وضعیت اقساط و تعهدات مالی",
                                            subtitleFa = "خروجی رسمی استاندارد A4 ترازینو",
                                            obligations = uiState.obligations,
                                            installments = uiState.installments,
                                            counterparties = uiState.counterparties,
                                            currencyUnit = uiState.preferences.currencyUnit
                                        )
                                        viewModel.exportA4Pdf(context, payload)
                                    },
                                    onDirectExportExcel = {
                                        val payload = ReportExportEngine.buildReportPayload(
                                            reportTitleFa = "گزارش جامع وضعیت اقساط و تعهدات مالی",
                                            subtitleFa = "خروجی اکسل راست‌به‌چپ ترازینو",
                                            obligations = uiState.obligations,
                                            installments = uiState.installments,
                                            counterparties = uiState.counterparties,
                                            currencyUnit = uiState.preferences.currencyUnit
                                        )
                                        viewModel.exportRtlExcel(context, payload)
                                    },
                                    onDirectExportCsv = {
                                        val payload = ReportExportEngine.buildReportPayload(
                                            reportTitleFa = "گزارش جامع وضعیت اقساط و تعهدات مالی",
                                            subtitleFa = "خروجی CSV جهانی ترازینو",
                                            obligations = uiState.obligations,
                                            installments = uiState.installments,
                                            counterparties = uiState.counterparties,
                                            currencyUnit = uiState.preferences.currencyUnit
                                        )
                                        viewModel.exportUtf8Csv(context, payload)
                                    },
                                    onSetThemeMode = { viewModel.setThemeMode(it) },
                                    onSetCurrencyUnit = { viewModel.setCurrencyUnit(it) },
                                    onSetFontScale = { viewModel.setFontScale(it) },
                                    onConfigureSecurityPin = { pin, len, bio, q, ans ->
                                        viewModel.configureSecurityPin(pin, len, bio, q, ans)
                                    },
                                    onRemoveSecurityPin = { viewModel.removeSecurityPin() },
                                    onLockAppNow = { viewModel.lockAppImmediately() },
                                    onStartGuidedTour = { viewModel.startGuidedTour() },
                                    onClearAllDataToZero = { viewModel.clearAllDataToZero() },
                                    onRestoreDemoSeedData = { viewModel.restoreDemoSeedData() },
                                    onExportBackupJson = { cb -> viewModel.exportFullBackupJson(cb) },
                                    onImportBackupJson = { viewModel.importFullBackupJson(it) },
                                    onShowToast = { viewModel.showToast(it) },
                                    onOpenCustomA4ReportPayload = { payload ->
                                        viewModel.openCustomA4ReportPayload(payload)
                                    },
                                    onOpenConnectivityPrivacySettings = {
                                        viewModel.openConnectivityPrivacyScreen()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 3-Step Full-Screen Obligation & Editable Per-Installment Wizard Modal
            if (uiState.wizardPreset.isOpen) {
                StepByStepObligationWizardModal(
                    counterparties = uiState.counterparties,
                    preselectedCategory = uiState.wizardPreset.preselectedCategory,
                    preselectedCounterpartyId = uiState.wizardPreset.preselectedCounterpartyId,
                    currencyUnit = uiState.preferences.currencyUnit,
                    onDismiss = { viewModel.closeStepByStepWizard() },
                    onCompleteWizard = { ob, customInst, newCp ->
                        viewModel.saveWizardObligationAndCustomInstallments(ob, customInst, newCp)
                    },
                    scannedPrefill = uiState.wizardPreset.scannedPrefill,
                    onOpenContractScanner = { viewModel.openContractScanner() }
                )
            }

            // CameraX + ML Kit OCR Loan Contract & Receipt Scanner Modal
            if (uiState.isContractScannerOpen) {
                val unpaidInstallments = remember(uiState.installments) {
                    uiState.installments.filter { it.statusEnum != InstallmentStatus.PAID && it.remainingToman > 0L }
                }
                LoanContractScannerModal(
                    currencyUnit = uiState.preferences.currencyUnit,
                    unpaidInstallments = unpaidInstallments,
                    onDismiss = { viewModel.closeContractScanner() },
                    onPopulateNewObligationWizard = { extraction ->
                        viewModel.openWizardFromScannedContract(extraction)
                    },
                    onApplyAsInstallmentPayment = { installment, extraction ->
                        viewModel.applyScannedReceiptToInstallment(installment, extraction)
                    }
                )
            }

            // Standard A4 Colorful Report Preview Modal
            uiState.activeA4ReportPayload?.let { payload ->
                A4ReportPreviewModal(
                    payload = payload,
                    onDismiss = { viewModel.closeA4ReportPreview() },
                    onExportPdf = { viewModel.exportA4Pdf(context, payload) },
                    onExportRtlExcel = { viewModel.exportRtlExcel(context, payload) },
                    onExportCsv = { viewModel.exportUtf8Csv(context, payload) }
                )
            }

            // Interactive Graphical Spotlight Onboarding Tour Overlay (First Launch & On-Demand)
            if (uiState.isTourActive && !uiState.isVaultLocked) {
                InteractiveOnboardingTourOverlay(
                    steps = viewModel.guidedTourSteps,
                    currentStepIndex = uiState.currentTourStepIndex,
                    onNextStep = { viewModel.nextTourStep() },
                    onPreviousStep = { viewModel.previousTourStep() },
                    onSkipOrFinish = { viewModel.finishGuidedTour() }
                )
            }

            // Procedural 60fps Emerald & Gold Confetti Burst on Installment Settlement
            uiState.celebrationMessageFa?.let { msg ->
                TarazinoCelebrationConfettiBurst(triggerKey = msg)
            }

            // Centralized Connectivity & Privacy Control Center Full-Screen Overlay
            if (uiState.isConnectivityPrivacyOpen && !uiState.isVaultLocked) {
                ConnectivityPrivacyScreen(
                    policy = uiState.preferences.connectivityPrivacy,
                    onlineInputs = uiState.preferences.onlineInputs,
                    hasPinLock = uiState.preferences.pinHash.isNotBlank(),
                    privacyMaskEnabled = uiState.preferences.privacyMaskEnabled,
                    isCloudSyncRunning = uiState.isCloudSyncRunning,
                    onUpdatePolicy = { transform -> viewModel.updateConnectivityPrivacyPolicy(transform) },
                    onSaveOnlineAiInputs = { mode, endpoint, model, apiKey ->
                        viewModel.saveOnlineAiInputs(mode, endpoint, model, apiKey)
                    },
                    onSaveCloudSyncInputs = { endpoint, token, workspace ->
                        viewModel.saveCloudSyncInputs(endpoint, token, workspace)
                    },
                    onTriggerCloudBackupSyncNow = { viewModel.triggerCloudBackupSyncNow() },
                    onClearStoredOnlineSecrets = { viewModel.clearStoredOnlineSecrets() },
                    onTogglePrivacyMask = { viewModel.togglePrivacyMask() },
                    onClearOnlineAiCache = { viewModel.clearOnlineAiCacheOnly() },
                    onResetStrictOffline = { viewModel.resetConnectivityPrivacyToStrictOffline() },
                    onBack = { viewModel.closeConnectivityPrivacyScreen() }
                )
            }

            // Security Vault Lock Gate Overlay
            if (uiState.isVaultLocked) {
                SecurityLockOverlay(
                    pinLength = uiState.preferences.pinLength,
                    biometricEnabled = uiState.preferences.biometricEnabled,
                    recoveryQuestion = uiState.preferences.recoveryQuestion,
                    hasRecoveryConfigured = uiState.preferences.recoveryAnswerHash.isNotBlank(),
                    onVerifyPin = { viewModel.attemptUnlockWithPin(it) },
                    onBiometricUnlock = { viewModel.unlockViaBiometricSensor() },
                    onVerifyRecoveryAnswer = { viewModel.attemptRecoveryUnlock(it) }
                )
            }
        }
    }
}

@Composable
private fun DockTabItem(
    dest: NavBarDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = com.example.ui.components.rememberSpringColorAnimatable(
        targetColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        springSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    ).value
    val pillBg = com.example.ui.components.rememberSpringColorAnimatable(
        targetColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
        springSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    ).value
    val selectionAnim = com.example.ui.components.rememberSpringFloatAnimatable(
        targetValue = if (selected) 1f else 0f,
        initialValue = if (selected) 1f else 0f,
        springSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
            visibilityThreshold = 0.005f
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(pillBg)
            .tarazinoPressScale(scaleDown = 0.92f, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .testTag("bottom_nav_${dest.tab.name.lowercase()}")
    ) {
        Icon(
            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
            contentDescription = dest.labelFa,
            tint = contentColor,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    val p = selectionAnim.value
                    val s = 1f + (0.12f * p)
                    scaleX = s
                    scaleY = s
                    translationY = (-1.5).dp.toPx() * p
                }
        )
        Spacer(modifier = Modifier.height(2.dp))
        TarazinoAutoSizeText(
            text = dest.labelFa,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            minFontSize = 8.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(2.8.dp)
                .graphicsLayer {
                    val p = selectionAnim.value.coerceIn(0f, 1f)
                    scaleX = p
                    alpha = p
                }
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                        )
                    )
                )
        )
    }
}
