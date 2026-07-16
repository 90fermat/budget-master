# BudgetMaster — Production Roadmap & Deliverable State

> Original audit baseline: 2026-07-14. **Status refreshed: 2026-07-15** after Phases 0, 1,
> and 1.5. Derived from the codebase against `ARCHITECTURE.md` and `DESIGN_SYSTEM.md`.

---

## 1. Where the app stands today

### Feature maturity matrix (refreshed 2026-07-15)

| Feature | Domain | Data | MVI ViewModel | UI | Tests | Verdict |
|---|---|---|---|---|---|---|
| **Auth** | ✅ 12 use cases, typed `AuthError` | ✅ GitLive Firebase (Android/iOS) + Web local-only; `authStateChanged` | ✅ 6 ViewModels | ✅ 6 screens, **localized** EN/FR, animated splash | ✅ | **~90% — functional, data scoped to the signed-in uid; Google sign-in pending** |
| **Dashboard** | ✅ 5 use cases, 6 models | ✅ SQLDelight + Gemini AI | ✅ Full MVI | ✅ Premium components + skeleton | ✅ Unit + Roborazzi | **~90% — reference implementation** |
| **Transactions** | ✅ models, repo, 5 use cases | ✅ SqlDelight repo + first-launch seeding | ✅ Full MVI | ✅ Day-grouped list, search, filters, swipe+undo, editor | ✅ use-case/VM/repo | **~90% (was ~15%)** |
| **Settings** | ✅ 6 use cases | ✅ DataStore/localStorage prefs | ✅ Full MVI | ✅ Theme/palette/language/currency, replay-intro | ➖ | **~85% (was ~20%)** |
| **Budgets** | ✅ models, repo, 4 use cases | ✅ SqlDelight, **live spent** from transactions | ✅ Full MVI | ✅ Gauges, summary, create/edit/delete | ✅ repo | **~85% (was ~25%)** |
| **Goals** | ✅ models, repo, 4 use cases | ✅ SqlDelight over SavingsGoalEntity | ✅ Full MVI | ✅ Progress cards, contribute, create/edit/delete | ✅ repo | **~85% (was ~10%)** |
| **Accounts** | ✅ models, repo, 6 use cases | ✅ SqlDelight, **live balance** = opening + own transactions | ✅ Full MVI | ✅ Wallet list, net worth, global switcher, create/edit/archive/delete | ✅ repo (5) | **~85% (new)** |
| **Reports** | ❌ | ❌ | ❌ | ⚠️ Static mockup | ❌ | **~15%** |

**Design system:** ✅ done — `AppTheme` with **5 palettes** (incl. Material You Dynamic),
bundled **Outfit + Inter** fonts, adaptive brand **logo**, `Spacing`/`Motion` tokens, and
palette-independent `FinancialColors`. **Localization:** EN/FR across the shell, transactions,
settings, and onboarding; the login/register/forgot screens are **now localized** too (EN/FR).
**Auth backend:** ✅ **implemented** (Phase 1.6) — see §Authentication below.

The SQLDelight schema (10 entities) now has unused tables only for **recurring, exchange
rates, and notifications** — the next features consume them. (`AccountEntity` gained an
`isArchived` column in Phase 2.5.)

### Authentication status ✅ (Phase 1.6)

Sign-in / sign-up / reset / sign-out are **functional**. Android & iOS use the GitLive
Firebase Kotlin SDK against the real project (`budget-master-4c24f`); `getAuthStatus()` is
backed by `authStateChanged`, so sessions persist across restarts and sign-out clears them.
Web runs a durable **local-only** profile in `localStorage` (no remote sync). Errors are a
typed `AuthError` mapped to localized EN/FR copy; the password-visibility toggle works.

**Remaining for auth:** (1) **iOS** needs, on macOS/Xcode, the Firebase iOS SDK added
(SPM/Pods) and `FirebaseApp.configure()` called in `iOSApp.init()` before `initKoinIos()`
— can't be built from the current Windows host. (2) The Firebase console must have the
**Email/Password** provider enabled. (3) **Google sign-in** — next up, after the accounts
foundation (Android/iOS only; Web stays local-only).

Per-account data scoping — previously deferred here — is **done** in Phase 2.5 below: the
signed-in uid is now the data owner.

### Architecture conformance (vs ARCHITECTURE.md)

**Respected ✅**
- Module graph matches the doc; no feature→feature dependencies.
- Koin module-per-feature wiring, `expect/actual` platform splits, async SQLDelight for Wasm.
- Dashboard, Transactions, and Settings follow the MVI contract faithfully.

**Fixed since the original audit ✅**
- Transactions/Settings ViewModels now exist; theme/palette/language persisted via DataStore.
- Single `strings.xml` source in `:core` (the `:shared` duplicate was deleted); EN/FR added.
- Hardcoded `Color(0x…)` removed from features — all color lives in `core.designsystem`.
- The `BuildConfig` "empty file" hack replaced by a generated config task.
- detekt + ktlint (via `detekt-formatting`) wired into the build and CI (reporting mode).

**Remaining drift ❌**
1. `budgetsModule` binds `SqlDelightBudgetRepository` that nothing injects (dead DI wiring).
2. Budgets/Goals/Reports screens are static mockups with hardcoded strings/data.
3. ~~Login/Register/ForgotPassword screens use hardcoded English strings~~ — **fixed**
   (Phase 1.6): all three now use `StringResources` (EN/FR), including typed error copy.
4. Dashboard→Settings navigation uses stringly-typed `onQuickAction("Settings")` magic strings
   instead of a typed Effect.
5. Konsist tests do not yet enforce the module-dependency / no-hardcoded-color rules
   automatically (detekt covers style/formatting, not architecture).

6. **Security (open)**: Gemini is still called directly from the client with an embedded
   API key — extractable from any APK/wasm bundle. Must move behind Firebase AI Logic /
   a proxy before release (Phase 6/7).

### Design-system conformance (vs DESIGN_SYSTEM.md)

**Implemented ✅** — full light/dark `ColorScheme` across all roles; **5 palettes** incl.
Material You Dynamic; **Outfit + Inter** typography with tabular figures; `Spacing`/`Motion`
token objects; adaptive brand logo; 600/1240dp breakpoints (NavigationBar/Rail/Drawer);
1200dp max-width container; budget gauge thresholds; premium animated splash.

**Still missing ❌**
1. Shared component library is only partial — `BalanceCard`/`TransactionRow` exist per
   feature; no single reusable set yet.
2. Motion system partial — splash + skeleton shimmer done; count-up, press-to-scale, and
   screen transitions pending.
3. A11y: no semantics on gauges/charts; touch-target & 200% font-scale audits not done.
4. Placeholder nav icons (Info/Favorite/Star for Budgets/Reports/Goals).
5. Colored-emoji font not bundled for Web → category emoji render as tofu on Wasm.

### Localization & formatting
- EN/FR for the shell, transactions, settings, and onboarding; **login/register/forgot
  still hardcoded English**. `MoneyFormatter` is locale/currency-aware, but the user's
  currency isn't wired end-to-end yet (screens default to USD).

### Production infrastructure
- ✅ GitHub Actions CI (build + host tests + Roborazzi screenshots); ✅ detekt + ktlint wired
  (reporting mode). Konsist architecture-rule tests still pending.
- Android release build: `isMinifyEnabled = false`, no signing config.
- No crash reporting, analytics, or performance monitoring.
- **Auth is stubbed on all platforms** (see §Authentication); Web DB is in-memory.

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
- [x] Tooling: **detekt + ktlint** (via `detekt-formatting`) wired at the root and into CI
  in reporting mode (`ignoreFailures = true`); GitHub Actions CI builds all targets, runs
  host tests, and verifies Roborazzi screenshots. `gradlew` executable bit fixed for Linux CI.
  - [ ] Clear the initial detekt findings, then flip detekt to blocking.
  - [ ] **Konsist** tests to enforce module-dependency and no-hardcoded-color rules
    (detekt covers style/formatting, not architecture).
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

**P0 — Web render (resolved)**
- [x] The suspected "0×0 surface" was a limitation of the in-app preview pane, not the
  app — the Wasm build renders correctly in a real browser (confirmed by the user). No
  code fix was required for rendering itself. Two real web issues found on device were
  fixed instead (below).

**Web fixes (found on device)**
- [x] **Invisible `onBackground` text on white**: the auth flow rendered with no painted
  background, so text sat on the raw page color. Added a root themed `Surface` in `App()`
  so every screen has a matching background + content color.
- [x] **Full-width text fields on desktop web**: constrained the auth forms
  (login/register/forgot/biometric) to a centered `widthIn(max = 420.dp)` column.

**Follow-ups (done)**
- [x] Android adaptive launcher icon from the brand mark (fixed indigo→violet background,
  white coin ring + emerald spark); app label set to "Budget Master".
- [x] Bundle **Outfit** (headings) and **Inter** (body) variable fonts via compose-resources
  and apply them in `appTypography()`; OFL licenses committed under `third_party/fonts/`.

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
- [x] Android adaptive launcher icon (mipmap) using the fixed brand-indigo mark.
- [x] Bundle Outfit/Inter fonts and apply them in the typography scale.
- [ ] Colored-emoji font for Web (category avatars render as tofu on Wasm); needs a
  bundled color-emoji font (~large) — deferred.
- [ ] Honor system reduced-motion for the splash animation where detectable.

**Deliverable:** first launch shows a polished animated splash (logo + "by FoyangTech")
then onboarding once; returning users skip straight to auth/dashboard; 5 premium palettes
(incl. Dynamic) selectable in Settings; "Budget Master" branding throughout.
**Status:** all items done — Android assembles, Wasm compiles, all host tests green
(verified in the user's browser; the in-app preview pane can't screenshot the animated
wasm canvas).

### Phase 1.6 — Real authentication (1–1.5 weeks) — **done (native config in place)**
> Sign-in/up was stubbed on every platform (threw "not configured"). It is now wired to the
> real GitLive Firebase SDK on Android/iOS and a durable local-only profile on Web. The real
> Firebase project (`budget-master-4c24f`) config is committed for Android + iOS.

- [x] **GitLive Firebase Kotlin SDK** wired in `FirebaseAuthRepository` (Android/iOS):
  `signInWithEmailAndPassword`, `createUserWithEmailAndPassword`, `sendPasswordResetEmail`,
  `signOut`; `FirebaseUser`→`User` mapping; `getAuthStatus()` now backed by the
  `authStateChanged` flow so sessions survive restarts. The dead `FirebaseAuthRepositoryImpl`
  stubs were deleted and a latent DI bug (`FirebaseAuthRepository(get(), get())` — no second
  dependency) was fixed.
- [x] **Firebase init**: Android auto-initializes via the `google-services` plugin +
  `google-services.json`. iOS `GoogleService-Info.plist` is committed. **iOS still needs, in
  Xcode (macOS):** add the Firebase iOS SDK (SPM/CocoaPods) and call `FirebaseApp.configure()`
  in `iOSApp.init()` *before* `initKoinIos()` — can't be built/verified from the Windows host.
- [x] **Web local-only mode**: `WasmAuthRepository` now persists a local profile in
  `localStorage` (via `KeyValueStore`) instead of throwing — sign-in/up create/reuse it,
  sign-out clears it, `getAuthStatus()` reflects it. Browser-scoped, no remote sync.
- [x] **Typed, localized errors**: new `AuthError`/`AuthException`; repos map Firebase
  failures (invalid credentials, user-not-found, email-in-use, weak password, network) onto
  them; screens resolve them to EN/FR strings. **Password-visibility toggle** wired (was a TODO).
- [x] **Localize** login/register/forgot-password screens (EN/FR) via string resources
  (previously hardcoded English).
- [x] **Sign-out** now calls the real `SignOutUseCase` from the composition root before
  routing to Login (previously navigation-only, which left the session active).
- [x] Tests: `AuthError` validation in `LoginUseCase`/`SignUpUseCase`; `LoginViewModel`
  success/typed-error/toggle flows.
- [ ] **Deferred — per-account data scoping**: still runs on the shared `default_user` seed;
  binding the signed-in uid as the data owner (and migrating local rows) is a larger change
  tracked separately to avoid destabilizing the local-first model.
- [ ] **Deferred — Google sign-in** button (optional) via gitlive.
- [ ] **Console prerequisite**: Email/Password provider must be enabled in the Firebase
  console for the project; otherwise Android/iOS sign-in returns an error.

### Phase 2 — Budgets, Goals, Settings on real data (1.5 weeks) — **mostly done**
- [x] **Shared seeding**: extracted default user/account/categories into a single
  `core.db.AppDataSeeder` (+ `DefaultData`) used by transactions, budgets, goals, and run
  at app startup — replaces the per-repo duplicated seed logic.
- [x] **Budgets**: real `BudgetRepository` + `SqlDelightBudgetRepository` with `spent`
  computed **live** from expense transactions in the category/period (denormalized column
  ignored); `BudgetsViewModel` (MVI); rewritten screen with a monthly summary header,
  status-colored gauges (OK/warning/exceeded), and a create/edit/delete editor
  (bottom sheet on phone, dialog on wide). Repository tests cover live-spent + status.
- [x] **Goals**: `GoalRepository` + `SqlDelightGoalRepository` over `SavingsGoalEntity`;
  `GoalsViewModel` (MVI); rewritten screen with progress cards, create/edit/delete, and an
  **Add funds** contribution dialog (edit preserves the saved amount). Repository tests
  cover contribute/complete/edit/delete.
- [x] **Settings currency**: added `currency` to `AppSettings` (persisted) with a picker in
  Settings; budgets/goals format amounts with it. Also wired the **"Replay intro"** row that
  had been left unhooked. (Settings MVI, theme/palette/language, sign-out already done.)
- [ ] **Deferred:** withdraw from goals; goal date picker + projected completion date;
  thread the currency into the transactions/dashboard screens (still default USD there);
  Notifications groundwork (budget-threshold → `NotificationEntity` + platform notify).

### Phase 2.5 — Accounts foundation / multi-wallet (1 week) — **done**
> One Firebase user owns **many** financial accounts (wallets), each with its own
> income/expense, plus a consolidated view. The schema already modelled this
> (`AccountEntity.userId` + `TransactionEntity.accountId`); this phase surfaces it and makes
> the signed-in uid the data owner.

- [x] **Identity binding** (closes the Phase 1.6 deferral): `AppDataSeeder.seedForUser(uid)`
  is idempotent per user — creates the `UserEntity` row and a first **"Cash"** wallet. The
  shared default categories stay global (`isDefault = 1`) under a system user. The
  composition root observes auth state and seeds/binds on sign-in.
- [x] **Session propagation**: new `core.session.SessionStore` exposes
  `currentUserId: StateFlow<String?>`, written by the app root from the auth state and read
  by every feature repository — so `:core` carries identity with no feature→feature edge.
  Transactions/budgets/goals/dashboard now scope to the signed-in uid (falling back to the
  local default user), re-querying reactively via `flatMapLatest` when the user changes.
- [x] **Active-account scope**: new `core.session.ActiveAccountStore` persists the selected
  wallet (`null` = **All accounts**) via `KeyValueStore`. Transactions observe the selected
  wallet or all of the user's wallets, and new transactions are written to the active wallet.
- [x] **`:feature:accounts` module**: `Account`/`AccountType`(CASH, CHECKING, SAVINGS,
  CREDIT_CARD, INVESTMENT)/`AccountDraft`, `AccountRepository` + `SqlDelightAccountRepository`,
  6 use cases, `AccountsViewModel` (MVI), and an Accounts screen with a **net-worth** header,
  wallet cards, and create/edit/**archive**/delete (delete is behind a confirm dialog since it
  cascades transactions). **Balance is derived** — `opening + Σ own transactions` — never
  stored, so it can't drift (same rule as budgets' live `spent`).
- [x] **Global switcher**: `AccountSwitcher` in a slim bar above the tab content on all three
  form factors (mobile/tablet/desktop) — pick "All accounts", a wallet, or "Manage accounts".
- [x] **Schema**: `AccountEntity.isArchived` column + `updateAccount` / `setAccountArchived` /
  `selectActiveAccountsByUserId` / `countAccountsByUserId` queries, and a
  `selectTransactionsByUser` join for per-user scoping.
- [x] **Tests**: 5 account repository tests (seeded first wallet, live balance incl. isolation
  between wallets, edit, archive/restore, delete). All existing suites still green.
- [ ] **Deferred follow-ups:** per-entry account picker in the transaction editor (entries
  currently land on the active wallet); **transfers** between wallets (linked pair, excluded
  from income/expense); **multi-currency conversion** for net worth via the unused
  `ExchangeRateEntity` (the overview flags mixed currencies as approximate today); scoping
  the **dashboard** to the active wallet (it is user-scoped but still consolidated);
  reconciliation ("set real balance" → adjustment entry).

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

### Phase 8 — Store polish & README showcase (0.5 week)
> After the feature set is complete and premium (post Phase 7), capture the app at its best.

- [ ] Capture polished **screenshots / a short GIF** across platforms and the 5 palettes
  (light + dark): splash, dashboard, transactions + add-editor, budgets, reports, settings.
  Prefer real-device/emulator captures over the Roborazzi goldens.
- [ ] Add a **Screenshots** section to `README.md` (a responsive table/grid), plus store
  listing assets (feature graphic, phone/tablet screenshots) for Play/App Store.
- [ ] Optional: a hosted web demo link once auth degrades gracefully on Wasm.

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

| Phase | Theme | Est. | Status |
|---|---|---|---|
| 0 | Design system + tooling foundation | 1–1.5 wk | ✅ done |
| 1 | Transactions end-to-end | 1.5–2 wk | ✅ done |
| 1.5 | Polish & premium identity (onboarding, logo, splash, palettes, rebrand) | 1–1.5 wk | ✅ done |
| 1.6 | Real authentication (Firebase wiring) | 1–1.5 wk | ✅ done (iOS init + Google sign-in pending) |
| 2 | Budgets / Goals / Settings on real data | 1.5 wk | ✅ done |
| 2.5 | Accounts foundation / multi-wallet (uid binding, switcher, net worth) | 1 wk | ✅ done |
| 2.6 | Google sign-in (Android/iOS) | 0.5 wk | ⬜ next |
| 3 | Reports + recurring engine | 1.5 wk | ⬜ |
| 4 | Motion & premium polish | 1–1.5 wk | ⬜ |
| 5 | Localization | 0.5–1 wk | ⬜ |
| 6 | Hardening & release | 1.5–2 wk | ⬜ |
| 7 | AI intelligence layer (free-tier Gemini via Firebase) | 2–2.5 wk | ⬜ |
| 8 | Store polish & README screenshots | 0.5 wk | ⬜ |
| | **Total** | **~12.5–15 weeks** | |

> Phase 7.0 (Firebase AI Logic migration) can be pulled forward into Phase 6 — it is the
> security fix for the embedded Gemini key. Phases 7.1/7.2 depend on Phases 1–3 data.
