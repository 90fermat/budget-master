# 💰 BudgetMaster — Personal Finance Management

BudgetMaster is a production-ready, feature-rich personal finance management application built with **Compose Multiplatform**. It offers a seamless experience across **Android, iOS, and Web (Kotlin/Wasm)**, helping users track expenses, manage budgets, and gain AI-powered financial insights.

---

## 🚀 Key Features

- **Multi-Platform Support**: Unified code base for Android, iOS, and Web.
- **Biometric Security**: Secure access via Fingerprint or Face ID.
- **Intelligent Dashboard**: Real-time balance overview with interactive charts (powered by Vico).
- **Smart Transactions**: Categorization, tags, recurring payments, and receipt scanning.
- **Budget Tracking**: Set monthly limits and get alerts before overspending.
- **Advanced Analytics**: Heatmaps, period comparisons, and PDF/CSV exports.
- **Offline-First**: Fully functional without internet; syncs automatically when online.
- **Material You**: Dynamic theme support with mandatory Dark Mode.

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
