package com.example.memory

enum class ModuleCompletionStatus(val badgeFa: String, val code: String) {
    NOT_STARTED("شروع‌نشده", "[ ] NOT_STARTED"),
    IN_PROGRESS("در حال توسعه", "[~] IN_PROGRESS"),
    IMPLEMENTED_AWAITING_TEST("پیاده‌سازی‌شده (در انتظار تست)", "[!] IMPLEMENTED_AWAITING_TEST"),
    VERIFIED_COMPLETE("تایید نهایی (۱۰۰٪ کامل + تست‌شده)", "[x] VERIFIED_COMPLETE")
}

data class EngineeringSubTask(
    val id: String,
    val titleFa: String,
    val isDone: Boolean = true
)

data class EngineeringModuleEntry(
    val moduleId: String,
    val titleFa: String,
    val titleEn: String,
    val status: ModuleCompletionStatus,
    val primaryFiles: List<String>,
    val boundTestSuites: List<String>,
    val subTasks: List<EngineeringSubTask>
)

data class ArchitecturalInvariant(
    val id: String,
    val titleFa: String,
    val ruleDescriptionFa: String
)

object AgentMemoryLedger {

    const val SCHEMA_VERSION = "v1.0.0-offline-room-jalali"
    const val LAST_CHECKPOINT = "ALL_10_MODULES_VERIFIED_AND_TESTED"

    val invariants: List<ArchitecturalInvariant> = listOf(
        ArchitecturalInvariant(
            id = "INV-01",
            titleFa = "قانون اعداد صحیح مبالغ مالی (Long Integer Math)",
            ruleDescriptionFa = "تمامی مبالغ به صورت عدد صحیح ۶۴ بیتی (Long) بر پایه تومان ذخیره می‌شوند. خطای ممیز شناور صفر است و باقیمانده تقسیم اقساط دقیقاً به قسط آخر اضافه می‌شود."
        ),
        ArchitecturalInvariant(
            id = "INV-02",
            titleFa = "قانون تقویم شمسی خالص (YYYY/MM/DD)",
            ruleDescriptionFa = "تمامی سررسیدها با تقویم جلالی و الگوریتم کبیسه ۳۳ ساله محاسبه شده و جابه‌جایی ماهانه انتهای ماه (۳۱ شهریور ← ۳۰ مهر ← ۲۹/۳۰ اسفند) را به صورت هوشمند مدیریت می‌کنند."
        ),
        ArchitecturalInvariant(
            id = "INV-03",
            titleFa = "حروف‌نگار زنده و دوگانگی تومان/ریال",
            ruleDescriptionFa = "در زیر تمامی فیلدهای مبلغ، معادل دقیق حروفی فارسی به صورت زنده رندر می‌شود و کاربر می‌تواند واحد نمایش کل برنامه را آنی بین تومان و ریال تغییر دهد."
        ),
        ArchitecturalInvariant(
            id = "INV-04",
            titleFa = "امنیت آفلاین SHA-256 و مخفی‌سازی ارقام",
            ruleDescriptionFa = "پین ۴ یا ۶ رقمی با Salt اختصاصی و الگوریتم SHA-256 هش می‌شود و حالت مخفی‌سازی ارقام (Privacy Mask) تمام مبالغ صفحه را در محیط‌های عمومی می‌پوشاند."
        ),
        ArchitecturalInvariant(
            id = "INV-05",
            titleFa = "تور آموزشی گرافیکی تعاملی در اولین اجرا",
            ruleDescriptionFa = "در اولین ورود کاربر، تور گرافیکی گام‌به‌گام با جابه‌جایی خودکار بین تب‌های برنامه اجرا شده و تمام قابلیت‌ها را به صورت بصری معرفی می‌کند."
        )
    )

    val modules: List<EngineeringModuleEntry> = listOf(
        EngineeringModuleEntry(
            moduleId = "MOD-01",
            titleFa = "موتور تقویم شمسی، انتخابگر تاریخ فارسی و مبدل عدد به حروف",
            titleEn = "Jalali Calendar Engine, Date Picker & Currency Wordifier",
            status = ModuleCompletionStatus.VERIFIED_COMPLETE,
            primaryFiles = listOf("core/JalaliCalendarEngine.kt", "core/PersianCurrencyEngine.kt", "ui/components/JalaliDatePickerModal.kt"),
            boundTestSuites = listOf("SUITE-01", "SUITE-02", "SUITE-07"),
            subTasks = listOf(
                EngineeringSubTask("MOD-01.1", "تبدیل دوطرفه میلادی و شمسی + تشخیص دقیق سال کبیسه"),
                EngineeringSubTask("MOD-01.2", "مدیریت هوشمند انتهای ماه در اقساط (۳۱ شهریور به ۳۰ مهر و اسفند)"),
                EngineeringSubTask("MOD-01.3", "انتخابگر تقویم شمسی تعاملی با میانبرهای امروز، اول ماه بعد و ۱ ماه دیگر"),
                EngineeringSubTask("MOD-01.4", "مبدل زنده عدد به حروف فارسی و چیپ‌های افزایش سریع مبلغ")
            )
        ),
        EngineeringModuleEntry(
            moduleId = "MOD-02",
            titleFa = "مدیریت طرف‌حساب‌ها و پرونده جامع مالی اشخاص و بانک‌ها",
            titleEn = "Counterparties & Financial Entities Ledger Engine",
            status = ModuleCompletionStatus.VERIFIED_COMPLETE,
            primaryFiles = listOf("data/TarazinoEntities.kt", "ui/screens/EntitiesScreen.kt"),
            boundTestSuites = listOf("SUITE-04"),
            subTasks = listOf(
                EngineeringSubTask("MOD-02.1", "دسته‌بندی بانک، پلتفرم اقساطی (BNPL)، موسسه/صندوق و اشخاص حقیقی"),
                EngineeringSubTask("MOD-02.2", "ذخیره شماره کارت ۱۶ رقمی با کپی آنی، شماره شبا IR و درجه اولویت خوش‌حسابی"),
                EngineeringSubTask("MOD-02.3", "پرونده جامع طرف‌حساب با تراز کل و خروجی مستقیم صورتحساب A4 PDF و اکسل")
            )
        ),
        EngineeringModuleEntry(
            moduleId = "MOD-03",
            titleFa = "موتور تعهدات، اقساط خودکار، حساب دفتری و پرداخت جزئی",
            titleEn = "Obligations, Auto-Schedule & Running Ledger Engine",
            status = ModuleCompletionStatus.VERIFIED_COMPLETE,
            primaryFiles = listOf("domain/InstallmentScheduleEngine.kt", "ui/screens/ObligationsScreen.kt"),
            boundTestSuites = listOf("SUITE-03", "SUITE-04"),
            subTasks = listOf(
                EngineeringSubTask("MOD-03.1", "پشتیبانی از ۵ دسته: وام بانکی، خرید BNPL، بدهی شخصی، طلب و چک صیادی"),
                EngineeringSubTask("MOD-03.2", "تولید خودکار جدول اقساط بدون خطای گرد کردن (Zero Rounding Drift)"),
                EngineeringSubTask("MOD-03.3", "حساب دفتری پویا با ثبت ریزخریدها و تراکنش‌های مرحله‌ای"),
                EngineeringSubTask("MOD-03.4", "ثبت پرداخت کامل، پرداخت جزئی (Partial)، جریمه دیرکرد، تخفیف و کد رهگیری")
            )
        ),
        EngineeringModuleEntry(
            moduleId = "MOD-04",
            titleFa = "رادار هشدار سررسیدها، اولویت‌بندی اخلاقی و پرداخت سریع",
            titleEn = "Smart Reminder, Countdown Radar & Criticality Engine",
            status = ModuleCompletionStatus.VERIFIED_COMPLETE,
            primaryFiles = listOf("domain/FinancialIntelligenceEngine.kt", "ui/screens/DashboardScreen.kt"),
            boundTestSuites = listOf("SUITE-05"),
            subTasks = listOf(
                EngineeringSubTask("MOD-04.1", "تفکیک خودکار معوقه، سررسید امروز، نزدیک به سررسید و برنامه آینده"),
                EngineeringSubTask("MOD-04.2", "وزن‌دهی اولویت قول شرف و چک صیادی در صدر صف هشدارها"),
                EngineeringSubTask("MOD-04.3", "ثبت پرداخت سریع تک‌کلیکی و یادآوری مجدد فردا (Snooze)")
            )
        ),
        EngineeringModuleEntry(
            moduleId = "MOD-05",
            titleFa = "هوش مالی آفلاین، تقویم حرارتی و شبیه‌ساز گلوله برفی/بهمن",
            titleEn = "Financial Intelligence, DTI Radar, Heatmap & Payoff Simulator",
            status = ModuleCompletionStatus.VERIFIED_COMPLETE,
            primaryFiles = listOf("domain/FinancialIntelligenceEngine.kt", "ui/screens/IntelligenceScreen.kt"),
            boundTestSuites = listOf("SUITE-05", "SUITE-06"),
            subTasks = listOf(
                EngineeringSubTask("MOD-05.1", "محاسبه نسبت اقساط به درآمد (DTI)، نقدینگی آزاد و امتیاز سلامت از ۱۰۰"),
                EngineeringSubTask("MOD-05.2", "تشخیص دهه اوج فشار مالی در ماه شمسی (دهه اول، دوم یا سوم)"),
                EngineeringSubTask("MOD-05.3", "شبیه‌ساز تعاملی استراتژی گلوله برفی در برابر بهمن با اسلایدر بودجه اضافه"),
                EngineeringSubTask("MOD-05.4", "نقشه حرارتی ۳۰/۳۱ روزه ماه شمسی جاری و نمودار بار ۱۲ ماه آینده")
            )
        ),
        EngineeringModuleEntry(
            moduleId = "MOD-06",
            titleFa = "موتور گزارش‌ساز رسمی PDF استاندارد A4 رنگی و اکسل راست‌به‌چپ",
            titleEn = "Standard A4 Colorful PDF & RTL Excel Export Studio",
            status = ModuleCompletionStatus.VERIFIED_COMPLETE,
            primaryFiles = listOf("export/ReportExportEngine.kt", "ui/components/A4ReportPreviewModal.kt"),
            boundTestSuites = listOf("SUITE-09"),
            subTasks = listOf(
                EngineeringSubTask("MOD-06.1", "رندر بومی برگه A4 رنگی با هدر گرافیکی، کارت‌های KPI، جدول زبرا و محل مهر و امضا"),
                EngineeringSubTask("MOD-06.2", "خروجی اکسل رنگی راست‌به‌چپ (.xls XML) و فایل جهانی (.csv با UTF-8 BOM)"),
                EngineeringSubTask("MOD-06.3", "اشتراک‌گذاری و چاپ مستقیم فایل‌های خروجی در اندروید")
            )
        ),
        EngineeringModuleEntry(
            moduleId = "MOD-07",
            titleFa = "خزانه امنیتی: قفل پین شیشه‌ای، اثر انگشت بیومتریک و حریم خصوصی",
            titleEn = "Security Vault: PIN Lock, Biometrics & Privacy Mask",
            status = ModuleCompletionStatus.VERIFIED_COMPLETE,
            primaryFiles = listOf("core/SecurityVaultEngine.kt", "ui/components/SecurityLockOverlay.kt"),
            boundTestSuites = listOf("SUITE-08"),
            subTasks = listOf(
                EngineeringSubTask("MOD-07.1", "صفحه‌کلید شیشه‌ای فارسی ۴ و ۶ رقمی با هش SHA-256 و انیمیشن خطا"),
                EngineeringSubTask("MOD-07.2", "ورود بیومتریک اثر انگشت و سوال امنیتی بازیابی رمز"),
                EngineeringSubTask("MOD-07.3", "دکمه قفل آنی برنامه و حالت مخفی‌سازی ارقام در محیط‌های عمومی")
            )
        ),
        EngineeringModuleEntry(
            moduleId = "MOD-08",
            titleFa = "شخصی‌سازی پوسته (تیره/روشن/کبود)، مقیاس فونت و تور آموزشی گرافیکی",
            titleEn = "Bespoke Persian FinTech Themes, Font Scaling & Guided Tour",
            status = ModuleCompletionStatus.VERIFIED_COMPLETE,
            primaryFiles = listOf("ui/theme/Theme.kt", "ui/components/InteractiveOnboardingTour.kt"),
            boundTestSuites = listOf("SUITE-10"),
            subTasks = listOf(
                EngineeringSubTask("MOD-08.1", "پشتیبانی کامل از ۳ تم زمرد شب، مرمر روشن و اقیانوس کبود + مقیاس فونت"),
                EngineeringSubTask("MOD-08.2", "تور آموزشی گرافیکی تعاملی با حرکت خودکار در بخش‌های مختلف در اولین اجرا")
            )
        ),
        EngineeringModuleEntry(
            moduleId = "MOD-09",
            titleFa = "پایگاه داده آفلاین Room، داده‌های نمونه غنی و پشتیبان‌گیری JSON",
            titleEn = "Offline Room Persistence, Seed Data & JSON Backup/Restore",
            status = ModuleCompletionStatus.VERIFIED_COMPLETE,
            primaryFiles = listOf("data/TarazinoDatabase.kt", "data/SeedDataEngine.kt", "data/TarazinoRepository.kt"),
            boundTestSuites = listOf("SUITE-03", "SUITE-10"),
            subTasks = listOf(
                EngineeringSubTask("MOD-09.1", "پیش‌بارگذاری داده‌های نمونه واقعی اکوسیستم مالی ایران برای تست سریع"),
                EngineeringSubTask("MOD-09.2", "دکمه پاکسازی یک‌کلیکی داده‌های نمونه (شروع از صفر) و بازگردانی مجدد"),
                EngineeringSubTask("MOD-09.3", "خروجی و بازیابی کامل فایل پشتیبان JSON آفلاین")
            )
        ),
        EngineeringModuleEntry(
            moduleId = "MOD-10",
            titleFa = "مرکز حافظه ایجنت، اجرای خودکار ۱۰ تست QA و مستندات مخزن",
            titleEn = "Agent Continuity Hub, 10 Automated QA Suites & Living README",
            status = ModuleCompletionStatus.VERIFIED_COMPLETE,
            primaryFiles = listOf("memory/AgentMemoryLedger.kt", "domain/QaDiagnosticsEngine.kt", "ui/screens/ReportsAndSettingsScreen.kt"),
            boundTestSuites = listOf("SUITE-01..10"),
            subTasks = listOf(
                EngineeringSubTask("MOD-10.1", "اجرای زنده ۱۰ سوئیت تست خودکار ریاضی، تقویم، اقساط، امنیت و گزارش‌گیری"),
                EngineeringSubTask("MOD-10.2", "تولید و کپی آنی بلوک JSON پیوستگی ایجنت (HANDOFF_CONTEXT_SNAPSHOT)")
            )
        )
    )

    fun generateHandoffJsonSnapshot(): String {
        val completedCount = modules.count { it.status == ModuleCompletionStatus.VERIFIED_COMPLETE }
        val totalSubTasks = modules.sumOf { it.subTasks.size }
        val completedSubTasks = modules.sumOf { m -> m.subTasks.count { it.isDone } }
        return buildString {
            appendLine("{")
            appendLine("  \"app\": \"Tarazino (ترازینو)\",")
            appendLine("  \"applicationId\": \"com.aistudio.tarazino.fntk\",")
            appendLine("  \"schemaVersion\": \"$SCHEMA_VERSION\",")
            appendLine("  \"checkpoint\": \"$LAST_CHECKPOINT\",")
            appendLine("  \"modulesCompleted\": \"$completedCount/${modules.size}\",")
            appendLine("  \"subTasksCompleted\": \"$completedSubTasks/$totalSubTasks\",")
            appendLine("  \"offlineMode\": true,")
            appendLine("  \"defaultCurrency\": \"TOMAN\",")
            appendLine("  \"guidedTourImplemented\": true,")
            appendLine("  \"invariants\": [")
            invariants.forEachIndexed { idx, inv ->
                val comma = if (idx < invariants.lastIndex) "," else ""
                appendLine("    {\"id\": \"${inv.id}\", \"title\": \"${inv.titleFa}\"}$comma")
            }
            appendLine("  ],")
            appendLine("  \"modules\": [")
            modules.forEachIndexed { idx, mod ->
                val comma = if (idx < modules.lastIndex) "," else ""
                appendLine("    {\"id\": \"${mod.moduleId}\", \"status\": \"${mod.status.name}\", \"tests\": \"${mod.boundTestSuites.joinToString(",")}\"}$comma")
            }
            appendLine("  ]")
            append("}")
        }
    }
}
