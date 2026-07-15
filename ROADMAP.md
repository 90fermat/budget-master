# BudgetMaster — Production Roadmap & Deliverable State

> Status baseline: 2026-07-14. Derived from a full audit of the codebase against
> `ARCHITECTURE.md` and `DESIGN_SYSTEM.md`.

---

## 1. Where the app stands today

### Feature maturity matrix

| Feature | Domain layer | Data layer | MVI ViewModel | UI | Tests | Verdict |
|---|---|---|---|---|---|---|
| **Auth** | ✅ 10 use cases | ✅ Firebase (Android/iOS), ❌ Web stub | ✅ 6 ViewModels | ✅ 6 screens | ✅ 3 test files | **~85% done** |
| **Dashboard** | ✅ 5 use cases, 6 models | ✅ SQLDelight + Gemini AI | ✅ Full MVI | ✅ Components + skeleton | ✅ Unit + screenshot | **~90% done — reference implementation** |
| **Transactions** | ❌ none | ❌ none | ❌ none | ⚠️ Static mockup (hardcoded data) | ❌ | **~15% — facade only** |
| **Budgets** | ⚠️ Repo interface only | ⚠️ Repo impl bound in DI but **never consumed** | ❌ none | ⚠️ Static mockup | ❌ | **~25%** |
| **Goals** | ❌ | ❌ | ❌ | ⚠️ Static mockup | ❌ | **~10%** |
| **Reports** | ❌ | ❌ | ❌ | ⚠️ Static mockup | ❌ | **~15%** |
| **Settings** | ❌ | ❌ | ❌ | ⚠️ Static mockup ("John Doe"), theme toggle not persisted | ❌ | **~20%** |

The SQLDelight schema (10 entities incl. recurring transactions, exchange rates,
notifications, insights) is **well ahead** of the features — most tables have zero consumers.

### Architecture conformance (vs ARCHITECTURE.md)

**Respected ✅**
- Module graph matches the doc exactly; no feature→feature dependencies.
- Koin module-per-feature wiring, `expect/actual` platform splits, async SQLDelight for Wasm.
- Dashboard and Auth follow the MVI contract (Intent/State/Effect) faithfully.

**Violations / drift ❌**
1. ARCHITECTURE.md documents `TransactionsViewModel`, `BudgetsViewModel`, `ReportsViewModel`,
   `SettingsViewModel` — **none exist**. Doc describes the target, not the reality.
2. `budgetsModule` binds `SqlDelightBudgetRepository` that nothing injects (dead DI wiring).
3. "No hardcoded strings" rule: violated across Transactions/Budgets/Goals/Settings screens
   and the nav items in `App.kt` ("Home", "History", …).
4. "All colors from `MaterialTheme.colorScheme`" rule: **47 hardcoded `Color(0x…)`**
   occurrences across 8 feature files.
5. Theme state (`isDarkMode`) is a `remember {}` in `App()` — lost on process death; DataStore
   is available but unused for preferences.
6. Dashboard→Settings navigation uses stringly-typed `onQuickAction("Settings")` magic strings
   instead of a typed Effect.
7. `strings.xml` is duplicated verbatim in `:core` and `:shared` — guaranteed drift.
8. `feature/dashboard/.../BuildConfig.kt` ("empty file to avoid compilation conflicts") +
   `config/BuildConfig.kt` expect/actual with mock-key string comparisons in
   `GeminiInsightsService` — fragile config plumbing.
9. **Security**: Gemini is called directly from the client with an embedded API key —
   extractable from any APK/wasm bundle. Must move behind a proxy before release.
10. README/doc version drift (Kotlin 2.1.0 vs 2.1.21, etc.).

### Design-system conformance (vs DESIGN_SYSTEM.md)

**Implemented ✅** — color palette hex-for-hex in both themes; 600/1240dp breakpoints with
NavigationBar/Rail/Drawer; 1200dp max-width container; budget gauge thresholds (85%/100%).

**Missing ❌**
1. **Typography system entirely absent** — `MaterialTheme(colorScheme = …)` passes no
   `typography`; no Outfit/Inter fonts bundled; `tnum` applied ad hoc via `.copy()`.
2. **Incomplete colorScheme**: only ~12 roles overridden. Screens use `surfaceVariant`,
   `primaryContainer`, `outlineVariant` — these fall back to **M3 baseline purple** defaults
   that clash with the custom palette. Real visual bug.
3. **No token layer in code**: `Spacing.*`, `Duration.*`, easing, shapes exist only in the doc.
4. **No shared component library**: `TransactionRow`, budget progress cards, etc. are
   re-implemented per feature with diverging styles (36dp vs 44dp icons, 16 vs 20dp radii).
5. Dynamic Color (Material You) on Android 12+ — promised, not implemented.
6. Motion system (count-up, press-to-scale, shimmer, screen transitions) — only the dashboard
   skeleton exists.
7. A11y: no semantics on gauges/charts, touch-target & font-scale (200%) audits not done.
8. Placeholder nav icons (Info/Favorite/Star for Budgets/Reports/Goals).

### Localization & formatting
- English only, no locale variants; `$` and number formats hardcoded in UI despite
  `currency` columns in the schema and an `ExchangeRateEntity` table.

### Production infrastructure
- No CI, no detekt/ktlint/Konsist enforcement of the "non-negotiable" rules.
- Android release build: `isMinifyEnabled = false`, no signing config.
- No crash reporting, analytics, or performance monitoring.
- Web target: auth is a stub that throws; not shippable.

---

## 2. Roadmap

Phases are ordered so each ships a coherent increment. Estimates assume one developer.

### Phase 0 — Foundation & Design System (1–1.5 weeks) — **largely done**
> Everything later builds on this; do it first.

- [x] `core.designsystem` package: `AppTheme` with **4 user-selectable palettes**
  (Indigo default / Emerald / Ocean / Sunset — full light+dark M3 schemes),
  `AppTypography` (tabular figures), `FinancialColors`, `Spacing`, `Motion` tokens.
  - [ ] Bundle **Outfit + Inter** fonts via compose-resources (still platform sans-serif).
  - [ ] Shared components: `BalanceCard`, `TransactionRow`, `BudgetProgressGauge`,
    `SectionHeader`, `EmptyState`, `ShimmerBox`, `AmountText`.
- [x] Theme moved out of `App.kt` into `AppTheme`; palette/dark-mode/language persisted
  via `AppSettingsRepository` (DataStore on Android/iOS, localStorage on Wasm) and
  editable in Settings (real MVI `SettingsViewModel`).
  - [ ] Dynamic Color on Android 12+ as a "System" palette option.
- [x] String resources consolidated in `:core` (`:shared` copy deleted); **French locale
  added** (`values-fr`) with in-app language switching (`LocalAppLocale` expect/actual,
  verified live on Wasm).
  - [ ] Extract remaining hardcoded strings (auth/onboarding screen bodies, mockup screens).
- [x] Duplicate `BuildConfig` hack removed — Gemini key now generated at build time into
  commonMain from `GEMINI_API_KEY` env/property (`generateDashboardConfig` task).
- [x] **Toolchain modernized (2026-07)**: Gradle 9.6.1, AGP 9.3.0 (KMP modules on
  `com.android.kotlin.multiplatform.library`, composeApp on built-in Kotlin),
  Kotlin 2.4.10, CMP 1.11.1, Koin 4.2.2, Ktor 3.5.1, SQLDelight 2.3.2, Vico 2.5.2,
  Firebase SDK 2.4.0 (+BoM 34.16), kotest 6.2.2. **Paparazzi → Roborazzi 1.68**
  (16 goldens recorded in `:composeApp`). Web `ComposeViewport` migrated to the
  CMP 1.10+ container API; SQLDelight web worker set up via
  `@cashapp/sqldelight-sqljs-worker` + kotlinx-browser.
- [ ] Tooling: detekt + ktlint + Konsist tests enforcing module-dependency and
  no-hardcoded-color rules; GitHub Actions CI (build all targets + tests on PR).
- [x] Doc drift fixed in ARCHITECTURE.md / README / DESIGN_SYSTEM.md.

**Known issues / follow-ups**
- Language change re-keys the composition, so navigation resets to Splash — acceptable
  for now; later: persist/restore nav state across locale changes.
- Wasm default font lacks emoji glyphs (tofu boxes) — fixed by bundling fonts (above).
- Web database is **in-memory** (sql.js) — data resets on page reload. Follow-up:
  persist via IndexedDB (e.g. absolute-fs/OPFS worker) or sync (Phase 6).
- iOS target not yet compiled/verified (requires a macOS host).

**Deliverable:** every existing screen renders through `AppTheme` with zero hardcoded
colors/strings; CI green.

### Phase 1 — Transactions feature, end-to-end (1.5–2 weeks) — **mostly done**
> The core loop of a finance app. Model it on the dashboard feature.

- [x] Domain: `TransactionItem`, `TransactionCategory`, `TransactionFilter`, `TransactionDraft`
  models; use cases (observe+filter by search/category/type, save, delete, restore, observe categories).
- [x] Data: `SqlDelightTransactionRepository` (Flow-based); seeds a default account +
  categories on first launch (shared `default_user`/`default_account` with the dashboard).
- [x] Presentation: full MVI (`TransactionsIntent/State/Effect/ViewModel`); real list with
  day grouping (Today/Yesterday/date), working search + type/category filter chips,
  swipe-to-delete with undo snackbar.
- [x] Add/Edit transaction as bottom sheet on phone → centered dialog on ≥600dp:
  type toggle, amount, category picker, notes. (Date picker + recurring toggle deferred.)
- [x] Currency formatting utility in `:core` (`MoneyFormatter` expect/actual over
  NumberFormat / NSNumberFormatter / Intl) + `DateUtils` relative-day helper.
- [x] Dashboard shares the same DB (single source of truth); total balance =
  opening account balance + signed transaction sum.
- [x] Tests: use-case filter tests, ViewModel grouping/delete-undo tests, repository
  seeding/upsert/delete-restore tests against in-memory driver. Android assembles,
  all host tests green, Wasm compiles.
- [ ] **Deferred to later phases:** date picker + recurring toggle in the editor;
  typed dashboard quick-add navigation effect; paged query for very large histories.

### Phase 1.5 — Polish & premium identity (1–1.5 weeks)
> Small, high-visibility fixes that make the app feel finished and branded. Requested
> after the Phase 1 build.

**P0 — Web render regression (blocker for web verification)**
- [ ] After the Phase 1 additions, the Kotlin/Wasm app composes to a 0×0 surface (no
  Skia canvas; `main()` completes with no thrown error). Android is unaffected (assembles,
  tests + screenshots green). Root-cause (suspect: first-frame measure of
  `ComposeViewport(document.body)` or an early composable in the Splash path) and restore
  web rendering; re-verify palette + FR switching + the transactions flow on web.

**Product/UX**
- [x] **Onboarding shown once**: root cause was that completion was never persisted on
  any platform. Added `core.prefs.OnboardingPreferences` (backed by `KeyValueStore` —
  DataStore on Android/iOS, `localStorage` on Web); `CheckFirstLaunchUseCase` now reads it
  and onboarding marks itself complete on finish/skip. Settings has a **"Replay intro"**
  action that resets the flag and navigates back to onboarding.
- [x] **Onboarding icons**: replaced the tofu/emoji glyphs with themed vector illustration
  badges (ReceiptLong / PieChart / Savings on soft accent gradients); pages now use the
  localized string resources (EN/FR) instead of hardcoded English.
- [x] **No biometric on web**: added `core.util.isBiometricAuthSupported` (expect/actual);
  onboarding skips the biometric step straight to sign-in on Wasm. (Settings has no
  biometric toggle after the earlier rewrite, so nothing to hide there.)

**Brand & identity**
- [x] **App logo**: chosen concept **4 + 3** — a coin-and-growth mark + "Budget Master"
  wordmark, built as scalable Compose vector components in `core.designsystem`
  (`AppLogoMark`, `AppWordmark`, `AppLogo`). The ring is **palette-adaptive** (follows the
  active palette's primary→tertiary gradient); the growth spark stays income-green. Used in
  Splash and the desktop drawer header.
- [x] **Splash redesign**: animated mark reveal (spring overshoot + fade) over a pulsing
  radial glow, staggered wordmark + accent-line draw-in, and a **"by FoyangTech"** credit;
  minimum 2s on-screen before navigating.
- [x] **Palette overhaul**: replaced the flat 4 with a curated premium set — Midnight
  Indigo, Emerald, Amethyst, Sunset Gold — on a richer obsidian/slate foundation with
  layered surface elevation and tuned container/on-colors. Added a **5th "Dynamic" palette**
  (Material You via `dynamicColorSchemeOrNull` expect/actual: real extraction on Android 12+,
  Indigo fallback on iOS/Web), shown with a rainbow swatch in Settings.
- [x] **Rename to "Budget Master"** (two words) across user-facing strings; package names,
  module ids, and `budgetmaster` namespaces unchanged.

**Follow-ups**
- [ ] Android adaptive launcher icon (mipmap) using the fixed brand-indigo mark.
- [ ] Bundle Outfit/Inter fonts so the wordmark + emoji render identically on Web.
- [ ] Honor system reduced-motion for the splash animation where detectable.

**Deliverable:** first launch shows a polished animated splash (logo + "by FoyangTech")
then onboarding once; returning users skip straight to auth/dashboard; 5 premium palettes
(incl. Dynamic) selectable in Settings; "Budget Master" branding throughout.
**Status:** all items done — Android assembles, Wasm compiles, all host tests green
(verified in the user's browser; the in-app preview pane can't screenshot the animated
wasm canvas).

### Phase 2 — Budgets, Goals, Settings on real data (1.5 weeks)
- [ ] Budgets: consume the already-bound repository; `BudgetsViewModel` (MVI); create/edit
  budget per category & period; live `spent` computed from transactions (SQL join, not the
  denormalized column — or keep `spent` updated by a trigger/use case).
- [ ] Goals: repository over `SavingsGoalEntity`; contribute/withdraw flows; projected
  completion date.
- [ ] Settings: `SettingsViewModel` + DataStore-backed preferences (theme mode, dynamic color,
  currency, biometric toggle reusing auth use cases); real profile from `GetCurrentUserUseCase`;
  working sign-out via `SignOutUseCase`.
- [ ] Notifications groundwork: budget-threshold events written to `NotificationEntity`;
  platform notification `expect/actual` (Android first).

### Phase 3 — Reports & recurring engine (1.5 weeks)
- [ ] Reports: `ReportsViewModel`; monthly income/expense trends, category ring chart,
  period comparison — Vico on Android/iOS, existing Canvas fallback on Wasm; tabular
  breakdown side-by-side on desktop widths.
- [ ] CSV export (commonMain) + platform share/save (`expect/actual`); PDF optional later.
- [ ] Recurring transactions engine: use case that materializes due `RecurringTransactionEntity`
  rows on app start + WorkManager job on Android.
- [ ] Chart a11y: dynamic `contentDescription` summaries per DESIGN_SYSTEM.md §7.

### Phase 4 — Premium polish: motion, adaptive, delight (1–1.5 weeks)
- [ ] Motion tokens applied: balance count-up, press-to-scale on primary buttons, shimmer
  loading everywhere lists load, animated progress fills, `AnimatedContent` screen transitions.
- [ ] List-detail split layout for Transactions on ≥600dp (material3-adaptive is already in
  the version catalog — use it).
- [ ] Proper iconography (custom or extended Material set) for nav + categories;
  app icon + splash polish.
- [ ] Empty states with illustration + CTA for every list; error states with retry.
- [ ] Haptics on key actions (Android/iOS `expect/actual`).
- [ ] Font-scale 200% + small-screen audit; 48dp touch-target audit; contrast check
  (the 0.05f-alpha borders and 0.1f surface tints will need revisiting).

### Phase 5 — Localization (0.5–1 week)
- [ ] Extract remaining strings; add **French** (`values-fr/strings.xml`) as second locale —
  CMP resources handle runtime locale switching.
- [ ] Locale-aware date/number/currency formatting everywhere (no `$` literals).
- [ ] Pseudo-locale pass for truncation/wrapping; RTL smoke test.

### Phase 6 — Production hardening & release (1.5–2 weeks)
- [ ] **Gemini key removal**: migrate `GeminiInsightsService` from direct REST + embedded
  API key to **Firebase AI Logic** (see Phase 7) — the SDK proxies Gemini through Firebase
  with App Check attestation, so no secret ships in the client. Non-negotiable before any
  public build.
- [ ] Web auth: implement Firebase JS interop or hide auth-gated features on Wasm; remove
  `UnsupportedOperationException` paths.
- [ ] Firebase per-platform config hygiene (google-services.json per build type, iOS plist).
- [ ] Crashlytics + analytics events; performance monitoring; Android Baseline Profiles.
- [ ] Release engineering: signing configs, `isMinifyEnabled = true` + R8 rules, versioning
  from git tags, CI release lanes (AAB, TestFlight, static web deploy).
- [ ] Offline-first sync (Firestore libs are already declared): last-write-wins sync for
  transactions/budgets/goals, or explicitly de-scope and update README.
- [ ] Store readiness: privacy policy, data-safety forms, screenshots, listing copy.

### Phase 7 — AI intelligence layer (free-tier Gemini via Firebase) (2–2.5 weeks)

> Strategy: **on-device first, free cloud tier second, never a raw API key in the client.**
> All cloud calls go through **Firebase AI Logic** (Gemini Developer API free tier, App
> Check protected). On-device models (Gemini Nano via ML Kit GenAI / AICore) handle quick,
> private tasks on supported hardware. Privacy guardrails apply to every feature: prompts
> carry **aggregates only** (never the raw ledger, names, or emails), AI is an **opt-in
> toggle in Settings**, and every insight carries a "not financial advice" disclaimer.

**7.0 — Foundation**
- [ ] `GenAiClient` expect/actual in `:core` (no business logic): Android/iOS actuals use
  the Firebase AI Logic SDKs, Wasm actual uses the Firebase JS SDK via interop (feature
  degrades gracefully where unavailable).
- [ ] App Check on all platforms (Play Integrity / App Attest / reCAPTCHA Enterprise).
- [ ] Migrate `GeminiInsightsService` to `GenAiClient` with **structured JSON output**
  (response schema) instead of free-text parsing; keep the 24 h SQLDelight cache and add
  exponential backoff for free-tier rate limits (429s).
- [ ] Feature flags via Firebase Remote Config (free) so each AI feature can be toggled
  server-side; "AI features" master switch in Settings (opt-in, off by default).

**7.1 — Smart capture** (needs Phase 1 transactions)
- [ ] **Auto-categorization**: merchant/description → category. Gemini Nano on-device when
  available, Flash-Lite free tier otherwise, rule-based fallback offline. Learned
  merchant→category pairs cached locally so each merchant is asked once.
- [ ] **Natural-language quick add**: "coffee 4.50 yesterday" → parsed amount/category/date
  pre-filling the add-transaction sheet.
- [ ] **Receipt scan**: ML Kit on-device text recognition (free) + Gemini multimodal parse
  into a draft transaction — powers the existing Transactions FAB.

**7.2 — Coaching & analysis** (needs Phases 2–3 budgets/reports)
- [ ] **Monthly narrative summary**: locally computed aggregates → Gemini writes the
  human story ("Dining up 18%, mostly weekends…") in the user's language (EN/FR).
- [ ] **Budget suggestions**: propose per-category limits from 3-month averages, with
  one-tap apply.
- [ ] **Subscription & anomaly detection**: recurring-charge detection runs locally (SQL);
  Gemini labels and explains flagged items.
- [ ] **Finance Q&A**: "How much did I spend on food in June?" — the question is parsed to
  a local SQL aggregate; Gemini only phrases the answer. Raw transactions never leave the
  device.

**Cost & quota posture**: free tier only — aggressive local caching (existing
`InsightEntity` pattern), per-feature daily request budgets, batch prompts, and Remote
Config kill-switches if quotas tighten.

---

## 3. Deliverable state (Definition of Done)

The app is "production-ready premium" when all of the following hold:

**Functional**
- Every tab (Dashboard, Transactions, Budgets, Goals, Reports, Settings) operates on live
  SQLDelight data — zero hardcoded mock content anywhere.
- Full transaction lifecycle: add/edit/delete/search/filter/recurring, reflected instantly
  on Dashboard, Budgets, and Reports via shared Flows.
- Auth works on Android and iOS incl. biometrics; Web either works or degrades gracefully.
- Preferences (theme, currency, biometric, locale) persist across restarts.

**Architecture**
- Every feature mirrors the dashboard structure: `domain/ data/ presentation/ di/` with full
  MVI contract; Konsist/CI enforces module rules automatically.
- ARCHITECTURE.md matches reality (docs updated in the same PR as code).

**Design**
- Single `AppTheme` (colors incl. all M3 roles, Outfit/Inter typography, shapes, spacing,
  motion tokens); shared component library; no `Color(0x…)` outside the theme (CI-enforced).
- Motion system implemented; adaptive layouts incl. list-detail on tablet/desktop.
- WCAG AA contrast, 48dp targets, semantics on gauges/charts, 200% font-scale safe.

**Localization**
- 100% of UI text via string resources; ≥2 locales (en, fr); locale-aware currency/date/number
  formatting driven by the user's currency setting.

**Quality & security**
- Test pyramid: use-case + ViewModel unit tests for every feature, repository tests on
  in-memory drivers, Roborazzi screenshots for key screens; CI green on all targets.
- No secrets in client binaries (Gemini via Firebase AI Logic + App Check); R8 enabled;
  crash-free rate measurable via Crashlytics.
- One-command release pipeline per platform.

**AI**
- All AI features run on the free tier (Firebase AI Logic / on-device Gemini Nano),
  opt-in from Settings, degrade gracefully offline, and send only aggregated,
  PII-free prompts. AI answers are localized (EN/FR) and carry a
  "not financial advice" disclaimer.

---

## 4. Suggested sequencing summary

| Phase | Theme | Est. |
|---|---|---|
| 0 | Design system + tooling foundation | 1–1.5 wk |
| 1 | Transactions end-to-end | 1.5–2 wk |
| 1.5 | Polish & premium identity (onboarding, logo, splash, palettes, rebrand) | 1–1.5 wk |
| 2 | Budgets / Goals / Settings on real data | 1.5 wk |
| 3 | Reports + recurring engine | 1.5 wk |
| 4 | Motion & premium polish | 1–1.5 wk |
| 5 | Localization | 0.5–1 wk |
| 6 | Hardening & release | 1.5–2 wk |
| 7 | AI intelligence layer (free-tier Gemini via Firebase) | 2–2.5 wk |
| | **Total** | **~11–13.5 weeks** |

> Phase 7.0 (Firebase AI Logic migration) can be pulled forward into Phase 6 — it is the
> security fix for the embedded Gemini key. Phases 7.1/7.2 depend on Phases 1–3 data.
