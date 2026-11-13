# AGENT_MEMORY.md — Tarazino (ترازینو) Stateful Agent Continuity & Engineering Ledger

> **IMPORTANT ARCHITECTURAL RULE:** This document (`AGENT_MEMORY.md`) is strictly an internal engineering & agent continuity artifact stored in the project root. It MUST NEVER be rendered or exposed inside the user-facing Android application UI.

## 1. Project Identity & Architectural Invariants
- **App Name:** ترازینو (Tarazino)
- **Application ID:** `com.aistudio.tarazino.fntk`
- **Namespace:** `com.example`
- **Schema Version:** `v2.2.0-offline-room`
- **Primary Language & Direction:** Persian (`fa-IR`), Strict Right-to-Left (`LayoutDirection.Rtl`)
- **Typography:** Local Bundled Google Font `Vazirmatn` (`R.font.vazirmatn`) across Compose UI and Native `PdfDocument` A4 Renderer
- **Currency Rule (INVARIANT-01):** All monetary values are stored internally in `Long` Toman integers (zero floating-point arithmetic). Display unit supports live switching between **تومان (Toman)** and **ریال (Rial = Toman × 10)** with live Persian Wordifier (`تبدیل زنده عدد به حروف فارسی`).
- **Jalali Calendar Rule (INVARIANT-02):** All dates are stored and processed as canonical Jalali strings `YYYY/MM/DD` (e.g., `1405/08/22`). Zero Gregorian `<input type="date">` usage. Month-stepping clamps end-of-month accurately (`1405/06/31` + 1 month → `1405/07/30`, and `29/30 Esfand` according to 33-year leap cycle).
- **Installment Exact Sum Rule (INVARIANT-03):** When splitting net payable obligations across `N` installments, `baseAmount = total / N` and `remainder = total % N` is added to the final installment so `sum(installments) == netPayable` with zero rounding drift.
- **Security Hash Rule (INVARIANT-04):** PIN codes (4 or 6 digits) are hashed with SHA-256 + static salt `TARAZINO_VAULT_SALT_2026` before persisting in preferences.
- **Coordinate-Aware Element Spotlight Tour (INVARIANT-05):** First launch (and the top-bar `✨` button) runs a 9-step coordinate-aware spotlight tour (`SpotlightTargetRegistry` + `Modifier.tourSpotlightTarget(id)`) that cuts out a glowing window around specific UI elements, renders an animated graphical pointer (`نشانگر گرافیکی متحرک`), auto-scrolls the active `LazyColumn` to the target index, and positions the explanation card above or below the target.
- **Button Geometry & No-Wrap Invariant (INVARIANT-06):** All buttons, segmented selectors, and export action cards use explicit `RoundedCornerShape(12.dp..16.dp)` with `maxLines = 1` or dedicated `ExportFormatTileCard` / `StrategySegmentButton` components so Material 3 `CircleShape` pills never wrap into vertical ovals.

---

## 2. Atomic Module Checklist (`MOD-01` to `MOD-10`)

| Module ID | Title | Status | Primary Source Mapping | Automated Verification |
| :--- | :--- | :--- | :--- | :--- |
| **MOD-01** | Jalali Calendar Engine, Interactive Persian Date Picker & Currency Wordifier | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `core/JalaliCalendarEngine.kt`, `core/PersianCurrencyEngine.kt`, `ui/components/JalaliDatePickerModal.kt` | `SUITE-01`, `SUITE-02`, `SUITE-07` |
| **MOD-02** | Hierarchical Tree Portfolio Engine (Parent Categories -> Sub-Providers like SnappPay/DigiPay -> Purchases) | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `data/TarazinoEntities.kt`, `ui/screens/TreePortfolioScreen.kt` | `SUITE-04` |
| **MOD-03** | 3-Step Obligation Wizard with Editable Per-Installment Table, Batch Settlement & Quick Edit Modal | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `domain/InstallmentScheduleEngine.kt`, `ui/components/StepByStepObligationWizard.kt`, `ui/screens/TreePortfolioScreen.kt` | `SUITE-03`, `SUITE-04` |
| **MOD-04** | Smart Reminder, Multi-Stage Countdown Alerts, Floating Quick-Action Bar & Quick Pay/Snooze | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `domain/FinancialIntelligenceEngine.kt`, `ui/screens/DashboardScreen.kt`, `MainActivity.kt` | `SUITE-05` |
| **MOD-05** | Financial Intelligence, Monthly Budget Planner, Safe Installment Capacity, Heatmap & Symmetrical Payoff Simulator | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `domain/FinancialIntelligenceEngine.kt`, `ui/screens/IntelligenceScreen.kt` | `SUITE-05`, `SUITE-06` |
| **MOD-06** | Standard A4 Colorful PDF (`PdfDocument`), Native Print & RTL Excel (`.xls` / `.csv` BOM) Tile Studio | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `export/ReportExportEngine.kt`, `ui/components/A4ReportPreviewModal.kt`, `ui/screens/ReportsAndSettingsScreen.kt` | `SUITE-09` |
| **MOD-07** | Security Vault: Glassmorphic Persian PIN Keypad, SHA-256 Hash, Biometric Sensor & Privacy Mask | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `core/SecurityVaultEngine.kt`, `ui/components/SecurityLockOverlay.kt` | `SUITE-08` |
| **MOD-08** | UI/UX Personalization (3 Themes, Font Scale & 9-Step Coordinate Spotlight Onboarding Tour) | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `ui/theme/Theme.kt`, `ui/components/InteractiveOnboardingTour.kt` | `SUITE-10` |
| **MOD-09** | Offline Room SQLite Persistence, Rich Iranian Seed Data, 1-Click Reset & JSON Backup/Restore | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `data/TarazinoDatabase.kt`, `data/SeedDataEngine.kt` | `SUITE-03`, `SUITE-10` |
| **MOD-10** | Standalone Agent Continuity Documentation (`AGENT_MEMORY.md`) & Automated JVM/Robolectric Test Suites | `[x] VERIFIED_COMPLETE (100% Detailed + Tested)` | `AGENT_MEMORY.md`, `domain/QaDiagnosticsEngine.kt`, `test/java/com/example/*` | `SUITE-01` .. `SUITE-10` |

---

## 3. Session Recovery & Handoff Protocol
- `AGENT_MEMORY.md` is maintained purely in the repository root for AI coding agents and developers.
- All 10 automated test suites (`SUITE-01` to `SUITE-10`) are verified via `gradle :app:testDebugUnitTest` (`ExampleUnitTest.kt` and `ExampleRobolectricTest.kt`).
