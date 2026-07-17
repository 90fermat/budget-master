# 💰 BudgetMaster — Personal Finance Management

BudgetMaster is a personal finance management application built with **Compose Multiplatform**, running on **Android, iOS, and Web (Kotlin/Wasm)** from one code base. It helps you track expenses, manage budgets across multiple wallets, and see where your money goes.

---

## 🚀 Key Features

- **Multi-Platform Support**: Unified code base for Android, iOS, and Web.
- **Multi-Wallet**: Several accounts per user, per-wallet balances, transfers between them,
  reconciliation, and an approximate multi-currency net worth.
- **Intelligent Dashboard**: Real-time balance overview with interactive charts.
- **Smart Transactions**: Categorization, search and filters, swipe-to-delete with undo, and
  calendar-correct recurring schedules.
- **Budget Tracking**: Set monthly limits, see live spend, and get alerts before overspending.
- **Analytics**: Period totals with a comparison against the preceding period, spending by
  category, a daily income-vs-expense trend, and CSV export.
- **Biometric Security**: Fingerprint / Face ID on Android and iOS.
- **Localized**: Full EN/FR, including in-app guides that explain every screen.
- **Material You**: 5 palettes including Dynamic, with light and dark themes.

### Honest scope

A short list of things this app deliberately does **not** do yet, so the feature list above can
be trusted:

- **No cloud sync.** Data lives in a local database on each device. Signing in scopes your data
  to your account; it does not move it between devices. Multi-device sync is future work — see
  ROADMAP.md.
- **AI insights are Android-only, and off until you turn them on.** They run through Firebase AI
  Logic, which proxies Gemini and attests the caller with App Check, so the app ships no API key.
  iOS and Web have no AI provider wired (Firebase AI Logic has no Kotlin Multiplatform wrapper)
  and hide the feature rather than degrade. The toggle lives in Settings and is off by default:
  only category totals and monthly income/expense sums are ever sent — never transaction
  descriptions, names or dates.
  - Running a debug build locally? App Check prints a debug token to logcat on first launch;
    register it under **App Check → Apps → Manage debug tokens** in the Firebase console or the
    insights call will be rejected.
- **No receipt scanning, tags, heatmaps, or PDF export.** These were listed here before they
  existed; they are planned, not built. CSV export is real.
- **Web has no real authentication** — it keeps a local-only profile rather than signing in to
  Firebase.

---

## 🏗️ Architecture

BudgetMaster follows **Clean Architecture** principles with a **Feature-First** module structure to ensure scalability and maintainability.

### Tech Stack
- **UI**: Compose Multiplatform (1.11.1)
- **Logic**: Kotlin Multiplatform (2.4.10)
- **Build**: Gradle 9.6.1 + AGP 9.3.0 (KMP modules use `com.android.kotlin.multiplatform.library`)
- **DI**: Koin (4.2.2)
- **Networking**: Ktor (3.5.1)
- **Database**: SQLDelight (2.3.2)
- **Navigation**: Compose Navigation (2.9.2)
- **Charts**: Vico (2.5.2)
- **Screenshot tests**: Roborazzi (1.68.0) + Robolectric

### Project Structure
```bash
├── :composeApp     # Android Application Launcher
├── :iosApp         # iOS Swift Wrapper
├── :webApp         # Kotlin/Wasm Entry Point
├── :shared         # App Orchestrator & Navigation Shell
├── :core           # Foundation (Database, Network, Shared Models)
└── :feature        # Organised Feature Modules
    ├── :auth       # Onboarding & Authentication
    ├── :dashboard  # Main Overview & Insights
    ├── :transactions # Expense Tracking & History
    ├── :budgets     # Savings Goals & Limits
    ├── :reports    # Analytics & Exports
    └── :settings   # User Preferences & App Config
```

---

## 🛠️ Development Setup

### Prerequisites
- **Android Studio Ladybug** or newer.
- **JDK 17** or higher.
- **Xcode** (for iOS development).
- **Kotlin Multiplatform** plugin installed in IDE.

### Running the App
1.  **Android**: Select the `composeApp` configuration and run on an emulator or physical device.
2.  **Web**: Run `./gradlew :webApp:wasmJsBrowserRun` to launch in your browser.
3.  **iOS**: Open the `iosApp` folder in Xcode or run directly from Android Studio using the Kotlin Multiplatform Mobile plugin.

---

## 📜 Coding Standards
- **MVI Pattern**: Predictable state management (Intent → State → Effect).
- **Strict Separation**: No Android dependencies in common or domain modules.
- **Testing**: Unit tests for all ViewModels and Paparazzi screenshot tests for all screens.
- **Documentation**: KDoc for all public functions.

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
