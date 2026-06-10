\# GEMINI.md — Budget Master App Context



\## PROJECT OVERVIEW

Budget Master is a production-ready personal finance management app

built with Compose Multiplatform 1.8.2 targeting Android, iOS, and Web.



\## ARCHITECTURE RULES (NON-NEGOTIABLE)

\- Clean Architecture: data / domain / presentation layers STRICTLY separated

\- MVI pattern for all ViewModels (Intent → State → Effect)

\- No Android imports in :shared or :domain modules

\- Dependency injection via Koin 4.x only

\- All async via Kotlin Coroutines + StateFlow / SharedFlow



\## MODULE STRUCTURE

:composeApp        → Android entry point
:iosApp            → iOS entry point  
:webApp            → Kotlin/Wasm web entry point
:shared            → Orchestrator / UI Shell
:core              → Foundation (DB, Network, Navigation)
:feature:*         → Feature modules (auth, dashboard, transactions, etc.)
  ├── commonMain   → Feature logic & UI
  ├── androidMain  → Android-specific implementations
  └── iosMain      → iOS-specific implementations




\## CODING STANDARDS

\- Every public function must have KDoc

\- Every ViewModel must be tested (unit tests)

\- Every screen must have Paparazzi screenshot tests

\- No hardcoded strings — use StringResources (CMP)

\- All colors from MaterialTheme.colorScheme only

\- No magic numbers — use named constants

\- Use offline-first approach

\## DEPENDENCIES (use EXACT versions)

compose\_multiplatform = "1.8.2"

kotlin = "2.1.0"

koin = "4.0.1"

ktor = "3.1.2"

sqldelight = "2.1.0"

navigation\_compose = "2.9.0"

vico = "2.0.1"

firebase\_kotlin\_sdk = "2.1.0"

kotest = "5.9.1"

material3\_adaptive = "1.2.0-alpha03"



\## PLATFORM TARGETS

\- minSdk: 26 (Android)

\- iOS: 16.0+

\- Web: Kotlin/Wasm (Chrome, Firefox, Safari)



\## UI GUIDELINES

\- Material Design 3 with dynamic color support

\- Dark mode MANDATORY on all screens

\- Adaptive layouts: phone / tablet / foldable

\- Animations: use Compose animate\* APIs, never View animations

\- Accessibility: all elements must have contentDescription



\## NEVER DO

\- Never use GlobalScope

\- Never use runBlocking in production code

\- Never put business logic in Composables

\- Never use findViewById or View system

\- Never hardcode API keys (use BuildConfig or .env)

\# BUDGET MASTER — 6 MODULES PRINCIPAUX

┌─────────────────────────────────────────────┐
│  MODULE 1 : ONBOARDING & AUTH               │
│  ├── Splash Screen animé                    │
│  ├── Onboarding (3 slides)                  │
│  ├── Login / Register                       │
│  ├── Biométrie (Face ID / Fingerprint)      │
│  └── Profil utilisateur                     │
│                                             │
│  MODULE 2 : DASHBOARD                       │
│  ├── Balance globale animée                 │
│  ├── Graphique revenus/dépenses (Vico)      │
│  ├── Top dépenses du mois                   │
│  ├── Objectifs en cours (progress bars)     │
│  └── Insights IA (Gemini)                   │
│                                             │
│  MODULE 3 : TRANSACTIONS                    │
│  ├── Liste paginée (LazyColumn)             │
│  ├── Ajout rapide (Bottom Sheet)            │
│  ├── Catégorisation intelligente            │
│  ├── Tags & notes                           │
│  ├── Récurrence (abonnements)               │
│  ├── Scanner reçu (MLKit / CameraX)         │
│  └── Filtres & recherche avancée            │
│                                             │
│  MODULE 4 : BUDGETS & OBJECTIFS             │
│  ├── Budgets par catégorie                  │
│  ├── Alertes dépassement                    │
│  ├── Objectifs d'épargne (Goal Tracker)     │
│  ├── Projections temporelles                │
│  └── Règle 50/30/20 automatique             │
│                                             │
│  MODULE 5 : RAPPORTS & ANALYTICS           │
│  ├── Graphiques mensuels/annuels            │
│  ├── Heatmap dépenses                       │
│  ├── Export PDF/CSV                         │
│  ├── Comparaison périodes                   │
│  └── Tendances & prédictions               │
│                                             │
│  MODULE 6 : PARAMÈTRES                      │
│  ├── Multi-devises                          │
│  ├── Thème dynamique (Material You)         │
│  ├── Notifications & rappels                │
│  ├── Sauvegarde/restauration                │
│  ├── Comptes bancaires liés                 │
│  └── Import CSV/OFX                         │
└─────────────────────────────────────────────┘