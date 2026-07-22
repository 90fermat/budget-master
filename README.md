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
- **Multi-Device Sync**: Signed in, your wallets and records follow you between devices, with
  offline edits reconciled when the connection returns.
- **App Lock**: Biometric unlock with a PIN fallback, a configurable idle timeout, and a screenshot
  block on the financial screens.
- **Encrypted Backup**: Export everything to a passphrase-encrypted file and restore it, on any
  platform — the answer to a lost phone that does not require signing in.
- **Mobile-Money Import**: Orange Money SMS parsed on the device into transactions, with a review
  queue for anything ambiguous and a notification saying what landed where.
- **Wallets kept apart**: Mark a wallet as not counted in the combined total — savings or a tontine
  stay visible and usable without inflating everyday figures.
- **Localized**: Full EN/FR, including in-app guides that explain every screen.
- **Material You**: 5 palettes including Dynamic, with light and dark themes.

### Honest scope

A short list of things this app deliberately does **not** do yet, so the feature list above can
be trusted:

- **Sync is Android and iOS only, and only while signed in.** Signed in, your records are mirrored
  to your own area of Cloud Firestore and pulled back on your other devices. Signed out, nothing
  financial leaves the phone. The web build has no sync at all — its database is rebuilt on every
  page load, so there is no durable state to reconcile.
  - **Before any build with sync reaches a user, deploy the rules**: `cd firebase && npx firebase
    deploy --only firestore:rules --project <id>`, or paste `firebase/firestore.rules` into the
    console's Rules tab. Without that the project's default rules apply and every write is refused.
  - The rules are tested against the emulator rather than eyeballed: `npm --prefix firebase test`.
- **Data at rest is not separately encrypted.** The database is app-private and the platform
  encrypts it on a screen-locked device; auto-backup is off, so it cannot leak to Google Drive.
  SQLCipher was considered and deliberately skipped — see ROADMAP.md for the reasoning.
- **AI insights are Android-only, and off until you turn them on.** They run through Firebase AI
  Logic, which proxies Gemini and attests the caller with App Check, so the app ships no API key.
  iOS and Web have no AI provider wired (Firebase AI Logic has no Kotlin Multiplatform wrapper)
  and hide the feature rather than degrade. The toggle lives in Settings and is off by default:
  only category totals and monthly income/expense sums are ever sent — never transaction
  descriptions, names or dates.
  - **Running a debug build locally? Register the App Check debug token, and expect to redo it.**
    App Check prints a token to logcat on first launch (filter `DebugAppCheckProvider`); register
    it under **App Check → Apps → Manage debug tokens** in the Firebase console or every AI call
    is rejected.
    - The token lives in SharedPreferences, so it is **regenerated on every fresh install, clear
      data, or new emulator** — and the old one keeps occupying a slot in the console. A burst of
      reinstall-driven testing is enough to make almost every request fail attestation.
    - The symptom is not obvious from inside the app: a rejection reads as a normal failure. Check
      **App Check → Requests** in the console, where rejected calls are counted as
      "invalid App Check token". A high rejection rate there almost always means an unregistered
      debug token rather than anything wrong with the code.
    - Release builds attest with Play Integrity instead, which requires the app to have been
      **installed from Play**. A sideloaded release APK fails attestation by design.
- **No heatmaps or PDF export.** These were listed here before they existed; they are planned, not
  built. CSV export, receipt scanning and an encrypted backup file are real.
- **Mobile-money import reads Orange Money only.** The parser is built to take more providers and
  accepts several of your own numbers, but the MTN MoMo format is not implemented — it needs real
  sample messages to be written against rather than guessed at.
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
