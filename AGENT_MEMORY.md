# AGENT_MEMORY.md — Tarazino (ترازینو) Stateful Agent Continuity & Engineering Ledger

## 1. Project Identity & Architectural Invariants
- **App Name:** ترازینو (Tarazino)
- **Application ID:** `com.aistudio.tarazino.fntk`
- **Namespace:** `com.example`
- **Schema Version:** `v1.0.0-offline-room`
- **Primary Language & Direction:** Persian (`fa-IR`), Strict Right-to-Left (`LayoutDirection.Rtl`)
- **Typography:** Local Bundled Google Font `Vazirmatn` (`R.font.vazirmatn`) across Compose UI and Native `PdfDocument` A4 Renderer
- **Currency Rule (INVARIANT-01):** All monetary values are stored internally in `Long` Toman integers (zero floating-point arithmetic). Display unit supports live switching between **تومان (Toman)** and **ریال (Rial = Toman × 10)** with live Persian Wordifier (`تبدیل زنده عدد به حروف فارسی`).
- **Jalali Calendar Rule (INVARIANT-02):** All dates are stored and processed as canonical Jalali strings `YYYY/MM/DD` (e.g., `1405/08/22`). Zero Gregorian `<input type="date">` usage. Month-stepping clamps end-of-month accurately (`1405/06/31` + 1 month → `1405/07/30`, and `29/30 Esfand` according to 33-year leap cycle).
- **Installment Exact Sum Rule (INVARIANT-03):** When splitting net payable obligations across `N` installments, `baseAmount = total / N` and `remainder = total % N` is added to the final installment so `sum(installments) == netPayable` with zero rounding drift.
- **Security Hash Rule (INVARIANT-04):** PIN codes (4 or 6 digits) are hashed with SHA-256 + static salt `TARAZINO_VAULT_SALT_2026` before persisting in preferences.
- **Interactive Guided Onboarding Tour (INVARIANT-05):** First launch triggers a multi-step graphical spotlight coach-mark tour that automatically navigates across the app's tabs to educate the user on each module, and can be re-triggered anytime from the top bar.

---

## 2. Atomic Module Checklist (`MOD-01` to `MOD-10`)

| Module ID | Title | Status | Primary Source Mapping | QA Test Suite Binding |
| :--- | :--- | :--- | :--- | :--- |
| **MOD-01** | Jalali Calendar Engine, Interactive Persian Date Picker & Currency Wordifier | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `core/JalaliCalendarEngine.kt`, `core/PersianCurrencyEngine.kt`, `ui/components/JalaliDatePickerModal.kt` | `SUITE-01`, `SUITE-02`, `SUITE-07` |
| **MOD-02** | Hierarchical Tree Portfolio Engine (Parent Categories -> Sub-Providers like SnappPay/DigiPay -> Purchases) | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `data/TarazinoEntities.kt`, `ui/screens/TreePortfolioScreen.kt` | `SUITE-04` |
| **MOD-03** | 3-Step Obligation Wizard with Editable Per-Installment Table, Running Ledger & Partial Payments | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `domain/InstallmentScheduleEngine.kt`, `ui/components/StepByStepObligationWizard.kt`, `ui/screens/TreePortfolioScreen.kt` | `SUITE-03`, `SUITE-04` |
| **MOD-04** | Smart Reminder, Multi-Stage Countdown Alerts, Criticality Weighting & Quick Pay/Snooze | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `domain/FinancialIntelligenceEngine.kt`, `ui/screens/DashboardScreen.kt` | `SUITE-05` |
| **MOD-05** | Dynamic Financial Intelligence, DTI Radar, Decade Crunch, Heatmap & Snowball/Avalanche Simulator | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `domain/FinancialIntelligenceEngine.kt`, `ui/screens/IntelligenceScreen.kt` | `SUITE-05`, `SUITE-06` |
| **MOD-06** | Standard A4 Colorful PDF (`PdfDocument`), Native Print & RTL Excel (`.xls` / `.csv` BOM) Studio | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `export/ReportExportEngine.kt`, `ui/components/A4ReportPreviewModal.kt` | `SUITE-09` |
| **MOD-07** | Security Vault: Glassmorphic Persian PIN Keypad, SHA-256 Hash, Biometric Sensor & Privacy Mask | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `core/SecurityVaultEngine.kt`, `ui/components/SecurityLockOverlay.kt` | `SUITE-08` |
| **MOD-08** | UI/UX Personalization (3 Themes: Obsidian Emerald, Alabaster Light, Deep Sapphire + Font Scale + Interactive Tour) | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `ui/theme/Theme.kt`, `ui/components/InteractiveOnboardingTour.kt` | `SUITE-10` |
| **MOD-09** | Offline Room SQLite Persistence, Rich Iranian Seed Data, 1-Click Reset & JSON Backup/Restore | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `data/TarazinoDatabase.kt`, `data/SeedDataEngine.kt` | `SUITE-03`, `SUITE-10` |
| **MOD-10** | Agent Short-Term Memory Hub, Handoff Context Snapshot Generator & 10 Automated QA Test Suites | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `memory/AgentMemoryLedger.kt`, `domain/QaDiagnosticsEngine.kt`, `ui/screens/ReportsAndSettingsScreen.kt` | `SUITE-01` .. `SUITE-10` |

---

## 3. Session Recovery & Handoff Protocol
- To resume or extend Tarazino in any future session, inspect `memory/AgentMemoryLedger.kt` and run the in-app **QA Diagnostics Runner** inside the **«گزارش، امنیت و حافظه ایجنت»** tab.
- All 10 automated test suites (`SUITE-01` to `SUITE-10`) verify Jalali leap years, month-end clamping, exact integer installment splits, partial payment recalculation, alert priority scoring, Snowball vs. Avalanche payoff simulation, Persian wordifier conversion, SHA-256 PIN verification, A4 PDF/Excel export serialization, and Agent Memory ledger integrity.
