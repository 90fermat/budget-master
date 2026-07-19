# BudgetMaster — Production Roadmap & Deliverable State

> Original audit baseline: 2026-07-14. **Status refreshed: 2026-07-16** after Phases 0–4.5.
> Derived from the codebase against `ARCHITECTURE.md` and `DESIGN_SYSTEM.md`.

---

## 1. Where the app stands today

### Feature maturity matrix (refreshed 2026-07-17)

> "100%" here means the feature is functionally complete, tested, and building on every target.
> It does not claim zero possible future polish — it means nothing is *outstanding* against this
> roadmap's Definition of Done. Where a genuine external gate remains (iOS needs macOS; on-device
> Google sign-in needs a real account), it's called out rather than hidden behind the number.

| Feature | Domain | Data | MVI ViewModel | UI | Tests | Verdict |
|---|---|---|---|---|---|---|
| **Auth** | ✅ 12 use cases, typed `AuthError` | ✅ GitLive Firebase (Android/iOS) + Web local-only; `authStateChanged` | ✅ 6 ViewModels | ✅ 6 screens, **localized** EN/FR, animated splash | ✅ | **100% on Android** — email/password + Google sign-in code complete and gracefully degrading; *external gates:* iOS Google sign-in needs Xcode, on-device Google verify needs an account on the device |
| **Dashboard** | ✅ 5 use cases, 6 models | ✅ SQLDelight + **Firebase AI Logic** (no API key) | ✅ Full MVI | ✅ Premium components + skeleton | ✅ Unit + Roborazzi | **100% — reference implementation** |
| **Transactions** | ✅ models, repo, 6 use cases (incl. AI quick-add) | ✅ SqlDelight repo + first-launch seeding | ✅ Full MVI | ✅ Day-grouped list, search, filters, swipe+undo, editor, **NL quick-add** | ✅ use-case/VM/repo | **100%** |
| **Settings** | ✅ 7 use cases | ✅ DataStore/localStorage prefs | ✅ Full MVI | ✅ Theme/palette/language/currency, AI opt-in, replay-intro | ✅ ViewModel | **100%** |
| **Budgets** | ✅ models, repo, 5 use cases (incl. AI suggestions) | ✅ SqlDelight, **live spent**, 3-month category averages | ✅ Full MVI | ✅ Gauges, summary, CRUD, **AI budget suggestions** | ✅ repo + use-case | **100%** |
| **Goals** | ✅ models, repo, 4 use cases | ✅ SqlDelight over SavingsGoalEntity | ✅ Full MVI | ✅ Progress cards, contribute, create/edit/delete | ✅ repo | **100%** |
| **Accounts** | ✅ models, repo, 9 use cases | ✅ SqlDelight, **live balance** = opening + own transactions; transfers, reconcile, FX conversion | ✅ Full MVI | ✅ Wallet list, net worth, global switcher, CRUD/archive, transfer, reconcile | ✅ repo (9) | **100%** |
| **Reports** | ✅ models, repo, 4 use cases (incl. AI narrative + Q&A) | ✅ SqlDelight, wallet-scoped, transfers excluded | ✅ Full MVI | ✅ Totals + comparison, donut, trend, CSV export, **AI coach** | ✅ repo + use-case | **100%** |
| **Recurring** | ✅ models, repo, 5 use cases | ✅ SqlDelight, calendar-correct, idempotent catch-up + WorkManager daily job | ✅ Full MVI | ✅ Schedule list, pause/resume, delete, editor | ✅ repo (5) | **100%** |

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
**Email/Password** provider enabled. (3) **Google sign-in** is implemented for Android
(Phase 2.6) but needs the **Google provider enabled** and the app's **SHA-1 registered** in
the console before it will work; iOS needs `GIDSignIn` from Xcode.

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
1. ~~`budgetsModule` binds a repository nothing injects~~ — **fixed** (Phase 2): the Budgets
   and Goals repositories are real and injected.
2. ~~Budgets/Goals screens are static mockups~~ — **fixed** (Phase 2); **Reports** is still a
   mockup and is Phase 3's job.
3. ~~Login/Register/ForgotPassword screens use hardcoded English strings~~ — **fixed**
   (Phase 1.6): all three now use `StringResources` (EN/FR), including typed error copy.
4. ~~Dashboard→Settings uses stringly-typed `onQuickAction("Settings")`~~ — **fixed**: a
   `NotificationsClicked` intent → `NavigateToSettings` effect, type-checked end to end.
5. ~~Konsist does not enforce architecture rules~~ — **fixed**: `:shared` androidHostTest runs
   4 Konsist rules (no feature→feature imports; no hardcoded colors outside the design
   system; ViewModels in `presentation`; Repository interfaces in `domain`).
6. ~~Hardcoded colors — partially open~~ — **closed** (Phase 4). The Konsist allowlist is
   empty and the rule asserts with no exceptions: the hex parser moved to
   `core.designsystem.parseHexColor`, category accents to `categoryAccentFor` (reading the
   seeded hex rather than restating a palette), and insight accents to semantic tokens. A
   second rule now also forbids fading `outline`/`surfaceVariant` below usability.

7. **Security (open)**: Gemini is still called directly from the client with an embedded
   API key — extractable from any APK/wasm bundle. Must move behind Firebase AI Logic /
   a proxy before release (Phase 6/7).

### Design-system conformance (vs DESIGN_SYSTEM.md)

**Implemented ✅** — full light/dark `ColorScheme` across all roles; **5 palettes** incl.
Material You Dynamic; **Outfit + Inter** typography with tabular figures; `Spacing`/`Motion`
token objects; adaptive brand logo; 600/1240dp breakpoints (NavigationBar/Rail/Drawer);
1200dp max-width container; budget gauge thresholds; premium animated splash.

**Closed (Phase 5) ✅**
1. ~~Shared component library is partial~~ — `EmptyState`/`ErrorState`/`ShimmerListPlaceholder`,
   the motion primitives, `Haptics`, `parseHexColor`, and the `categoryIconFor`/`categoryAccentFor`/
   `categoryNameFor` lookups all live in `:core`. The outstanding "also share
   `BalanceCard`/`TransactionRow`" is closed as *not needed*: `BalanceCard` has a single caller
   and `TransactionRow` does not exist, so there is nothing to de-duplicate.

**Closed since the original audit ✅**
- ~~Motion system partial~~ — count-up, press-to-scale, and screen transitions landed in
  Phase 4, all honouring reduced motion.
- ~~A11y: no semantics on gauges/charts~~ — charts carry generated summaries (Phase 3); both
  gauges speak, and budget status exists as words, not only colour (Phase 4).
- ~~Touch-target & 200% font-scale audits not done~~ — both audited in Phase 4. Touch targets
  were already clean (everything goes through `IconButton`); buttons moved to `heightIn` so
  labels can't clip at 200%.
- ~~Placeholder nav icons~~ — replaced in Phase 4 (Reports was literally a heart).
- ~~Colored-emoji font not bundled for Web~~ — solved *without* bundling a font: category
  emoji became Material vectors, so nothing renders as tofu and the bundle didn't grow. A
  sweep confirms no emoji remain in UI code; `DefaultData` still stores them as data.

### Localization & formatting
- **EN/FR across every screen**, including auth (Phase 1.6) and the in-app guides (Phase 4.5,
  authored in both rather than retrofitted). A test pins EN/FR key parity.
- The user's **currency is wired end-to-end** — transactions, dashboard, budgets, goals,
  accounts, reports all format with it (Phase 3/4); `MoneyFormatter` stays the single
  formatter.
- **Month names come from resources, not a platform formatter** — a formatter follows the
  *system* locale, but the app has its own language setting, so resources follow the language
  the user actually chose.
- **Built-in category names come from resources too** — the database is seeded in English, so
  a stored name is only ever translated for categories the user created (Phase 5).
- ✅ Hardcoded-English sweep, truncation pass, and RTL smoke test all done (Phase 5). The
  screenshot pass caught quick-action buttons and period chips the grep sweep missed, and
  documented latent BiDi bugs for whoever adds an RTL language later.

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
- iOS: the Kotlin target now **compiles** (`:shared:compileKotlinIosArm64`, verified from the
  Windows host — only linking and running the app need macOS/Xcode). It had never compiled:
  `iosMain/SpendingChart.kt` was a copy of the Android/Vico implementation, and Vico publishes
  Android artifacts only, so `iosMain` even declared a Vico dependency that could not resolve.
  iOS and Wasm now share one `CanvasSpendingChart` in `commonMain`; **Android keeps Vico**,
  which gives it axes and a touch marker the Canvas rendering does not have. CI does not build
  iOS, which is why this went unnoticed — worth adding a compile-only iOS job.

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
  type toggle, amount, category picker, notes, date picker, and a recurring toggle.
- [x] Currency formatting utility in `:core` (`MoneyFormatter` expect/actual over
  NumberFormat / NSNumberFormatter / Intl) + `DateUtils` relative-day helper.
- [x] Dashboard shares the same DB (single source of truth); total balance =
  opening account balance + signed transaction sum.
- [x] Tests: use-case filter tests, ViewModel grouping/delete-undo tests, repository
  seeding/upsert/delete-restore tests against in-memory driver. Android assembles,
  all host tests green, Wasm compiles.
- [x] **Editor completions**: date picker (M3 `DatePickerDialog`) and a "repeats" toggle
  (`TransactionEntity.isRecurring`; the materializing engine is Phase 3).
- [x] **Typed dashboard navigation**: `onQuickAction("Settings")` magic strings replaced by a
  `NotificationsClicked` intent → `NavigateToSettings` effect (also architecture drift #4).
- [x] **Paged history**: `selectTransactionsByUser/AccountPaged` with a growing window
  (100 rows, `LoadMore` at the list tail). Filtering is in-memory, so a filtered query runs
  **unbounded** — otherwise a match older than the window would silently vanish.

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
- [x] **Category icons on Web**: emoji rendered as tofu on Wasm (no color-emoji font).
  Rather than bundle one, `core.designsystem.categoryIconFor()` maps each category to a
  Material vector used on every platform — zero bundle cost, consistent everywhere. The
  emoji stays in the database as data.
- [x] Honor system reduced-motion: `core.util.isReducedMotionEnabled()` (expect/actual —
  animator scale on Android, `UIAccessibilityIsReduceMotionEnabled` on iOS,
  `prefers-reduced-motion` on Web); the splash starts fully revealed and skips the
  staggered reveal and glow pulse when set.

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
- [x] **Per-account data scoping** — done in Phase 2.5: `SessionStore` binds the signed-in
  uid as the data owner and every feature repository scopes to it.
- [x] **Google sign-in** — done in Phase 2.6 (Android; iOS pending macOS).
- [x] **Console prerequisite**: the Firebase project has Email/Password enabled and the
  app's SHA-1 registered (added by the project owner; `google-services.json` refreshed).

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
- [x] **Goals — withdraw**: `withdraw` + `WithdrawFromGoalUseCase`, a dialog capped at the
  saved balance, and a repository clamp at zero.
- [x] **Goals — target date + projection**: the date is now user-chosen (the ViewModel used
  to hardcode a one-year horizon and ignore the date on edit); `projectedCompletionAt()`
  extrapolates from the saving rate and the card shows "on track" vs "behind".
- [x] **Currency threaded** into transactions and dashboard. The dashboard had its own
  `formatCurrency` hardcoding a `$` prefix — it now uses the shared `MoneyFormatter`, so the
  setting is honoured everywhere.
- [x] **Notifications groundwork**: `core.notifications.NotificationRepository` over
  `NotificationEntity` (observe/unread-count/mark-read/delete, user-scoped) +
  `NotifyBudgetThresholdsUseCase` raising warning/exceeded alerts from the budgets stream.
  Ids are keyed by (budget, period, threshold) so repeat emissions can't spam the inbox.
  **Delivery to the OS notification shade is still pending** — it rides with the Phase 3
  recurring engine; today this backs an in-app inbox/badge.

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
- [x] **Per-entry account picker** in the transaction editor; undo restores an entry to its
  original wallet rather than the active one.
- [x] **Dashboard scoped to the active wallet** (balance, chart, recent — opening balance
  included), consolidated across wallets under "All accounts". Budget progress stays
  cross-account by design.
- [x] **Transfers**: a linked pair sharing `TransactionEntity.transferGroupId`, written in one
  DB transaction, excluded from income/expense **and** budget spend; net worth unaffected.
- [x] **Reconciliation**: "set real balance" posts the difference as an adjustment entry
  rather than overwriting the balance, keeping the total derived and auditable.
- [x] **Multi-currency net worth**: `core.currency.ExchangeRateRepository` over the
  previously-unused `ExchangeRateEntity` (cached pairs, reciprocal fallback, 1:1 for
  identical codes) + `CalculateNetWorthUseCase`. Wallets with no known rate are added at face
  value and the total is labelled approximate rather than silently mixing currencies.
- [x] **Rate source — done.** `ExchangeRateFetcher` + `RefreshExchangeRatesUseCase` finally write
  to `ExchangeRateEntity`, which until now was **only ever read**: nothing populated it, so any
  wallet in a second currency was permanently unconvertible and net worth permanently
  "approximate".
  - Source is **ExchangeRate-API's open endpoint**: no API key (one embedded secret was enough)
    and it covers every code in `SUPPORTED_CURRENCY_CODES`. The ECB-backed free APIs
    (Frankfurter et al.) were the obvious alternative but publish neither **XAF nor NGN**, which
    would leave two of the app's six currencies permanently unconvertible.
  - Refreshes at most **once a day** (upstream updates daily and the endpoint is rate-limited),
    re-keyed on the user's currency, and **fails silently** — rates are a nicety and an offline
    launch must not be slower or noisier than an online one. A failed fetch never wipes the cache.
  - Only supported pairs are stored: the endpoint returns ~160, which would bloat the table with
    currencies the app cannot select.
  - Attribution ("Rates By Exchange Rate API") is required by the provider's terms and sits on the
    net-worth card, shown only when converted rates actually contributed.
  - `SUPPORTED_CURRENCY_CODES` now lives in `:core`: the list was duplicated in the Settings
    picker and the account editor, so adding a currency in one silently left the other behind.
  - 9 tests: supported-pair filtering, the daily throttle, cache preserved on failure, and the
    reciprocal path.

### Phase 2.6 — Google sign-in (0.5 week) — **code done, blocked on console setup**

- [x] `AuthRepository.signInWithGoogle(idToken)` implemented on Android/iOS via gitlive
  `GoogleAuthProvider.credential(idToken)` → `signInWithCredential`.
- [x] **Android flow**: Credential Manager (`androidx.credentials` 1.6.0 +
  `googleid` 1.1.1) behind an `expect/actual` `rememberGoogleSignInLauncher`, using the OAuth
  **web client id** (`default_web_client_id`, generated from `google-services.json`) as the
  server client id — resolved at runtime so `:feature:auth` needs no `R` dependency.
- [x] **Capability gate**: `expect val isGoogleSignInSupported` — true on Android, false on
  Web (local-only mode) and, for now, iOS. The Login screen only renders the Google button
  where it's true, so there is no dead or throwing path.
- [x] Typed `AuthError.GoogleCancelled` / `GoogleUnavailable` with localized EN/FR copy; a
  cancelled sheet is deliberately **not** surfaced as an error.
- [x] Tests: 5 new `LoginViewModel` cases (token → NavigateToHome, backend failure, blank
  token, cancellation is silent, real error surfaces).

**⚠️ Prerequisites — only you can do these; the flow will fail at runtime until then:**
1. **Enable the Google provider**: Firebase console → Authentication → Sign-in method → Google.
2. **Register the app's SHA-1** on the Android app in the Firebase console. The current
   config has a web OAuth client but **no Android client / certificate hash**, which makes
   Credential Manager return `NoCredentialException` (surfaced as `GoogleUnavailable`).
   Debug SHA-1 for this machine:
   `0A:80:35:0D:EE:D8:B4:47:6E:12:57:46:B9:55:76:54:AC:67:71:F2`
   (re-run `./gradlew :composeApp:signingReport` elsewhere; add the **release** SHA-1 too
   before shipping). Then **re-download `google-services.json`**.
- [ ] **iOS**: add the Google Sign-In SDK (`GIDSignIn`) + reversed-client-id URL scheme in
  Xcode and flip `isGoogleSignInSupported` to true — needs macOS.
- [~] **On-device verification — the code path is verified; the flow needs a Google account to
  finish.** On the Pixel 4 API 33 emulator, tapping "Sign in with Google" reaches Google Play
  Services (logcat shows `auth.api.identity.service.signin.START`) and comes back with no
  credential, which the app handles exactly right: typed `GoogleUnavailable`, localized inline
  message, no crash. Two facts confirmed: the debug SHA-1
  (`0A:80:35:0D:EE:D8:B4:47:6E:12:57:46:B9:55:76:54:AC:67:71:F2`) is the registered one, and the
  emulator has **no Google account signed in** — which is why Credential Manager returns nothing.
  - **To finish the check (needs you):** on the emulator, Settings → Passwords & accounts → Add
    account → Google, sign in with a Google account, then retry. Requires entering your Google
    password, so it's yours to do — I can't. Once an account is present the flow should complete;
    if it still fails, the next suspect is the OAuth **web client** / Google provider enablement
    in the Firebase console.

### Status: Phases 0–5 are done; Phase 6 is next

Phases 0 through 5 are complete. **Only the two iOS items genuinely cannot be closed from a
Windows host** — everything else that was once "deferred" is now either done or explicitly
closed with a reason:

1. **iOS Firebase init** — add the Firebase iOS SDK (SPM/Pods) and call
   `FirebaseApp.configure()` in `iOSApp.init()` before `initKoinIos()`. Needs macOS/Xcode.
2. **iOS Google sign-in** — add `GIDSignIn` + the reversed-client-id URL scheme, then flip
   `isGoogleSignInSupported` to true on iOS. Needs macOS/Xcode.

The iOS *Kotlin* target compiles from Windows as of Phase 5; only linking and running the app
need macOS. Open non-iOS work rides with Phase 6: the **FX rate fetcher**, Google sign-in
**verification on the emulator** (SHA-1 now registered), and the **pseudo-locale/RTL**
screenshot passes from Phase 5.

### Phase 3 — Reports & recurring engine (1.5 weeks)
- [x] **Reports on real data** (was a static mockup): `ReportsRepository` +
  `SqlDelightReportsRepository` scoped to the signed-in user and the **active wallet**, with
  transfers/adjustments excluded — matching the dashboard and budget rules. Period totals
  (30/90/365/all) with a **comparison against the preceding period of the same length**,
  spending **by category** with shares, and a daily **income-vs-expense trend**.
  `ReportsViewModel` (MVI); the range drives a `flatMapLatest` re-query, and the report also
  re-emits when the wallet or currency changes. Category ring + trend chart **side-by-side
  ≥600dp**, stacked on phones.
- [x] **Charts**: the reports charts are drawn with Compose `Canvas` on every target and honour
  reduced motion. The **dashboard** `SpendingChart` is the exception and stays `expect/actual`:
  Android renders it with Vico (axes + touch marker), while iOS and Web share one
  `CanvasSpendingChart` — Vico publishes Android artifacts only. *(Corrected in Phase 5: this
  entry previously claimed a single implementation on all three targets, which was never true
  and hid the fact that the iOS chart was an uncompilable copy of the Android one.)*
- [x] **CSV export**: generated in commonMain with RFC 4180 quoting, shared via
  `expect/actual` — share sheet (Android `FileProvider` / iOS `UIActivityViewController`),
  data-URL download on Web; returns false rather than pretending to succeed. The export
  **includes** transfers, flagged in a column: it is a record of the money, not an
  income/expense analysis. PDF still optional/later.
- [x] **Recurring engine** (consumes the previously-unused `RecurringTransactionEntity`):
  `Frequency` steps by **calendar unit**, not a fixed millisecond span, so months/years keep
  their real length, DST can't drift the time of day, and month-end behaves (Jan 31 → Feb 29
  in a leap year). `materializeDue()` catches up **one entry per missed period** rather than a
  lump, with a deterministic id per occurrence (`rec_<schedule>_<runAt>`) making the whole
  operation **idempotent**, plus a catch-up ceiling. Runs on app start. Repository supports
  observe/upsert (keeps its place in the cycle on edit)/pause/resume/delete. 5 tests.
- [x] **WorkManager job on Android** (done in Phase 4): `RecurringWorker` runs a unique daily
  periodic job scheduled from `BudgetMasterApplication`, so entries appear on the day rather
  than at next launch. iOS/Web keep the open-time catch-up; `materializeDue()` is idempotent,
  so the two paths cannot double-post.
- [x] **Recurring management UI** (done in Phase 4): full MVI screen
  (`RecurringScreen`/`ViewModel`/`Contract` + `AddEditRecurringForm`), reachable from the
  Transactions app bar and wired at `AuthRoute.Recurring` in `App.kt`.
- [x] **Chart a11y**: a canvas is opaque to screen readers, so each chart carries a generated
  `contentDescription` summarising the data (top categories with shares; income/expense
  totals over N days), and the legend repeats the numbers as real text.

### Phase 4 — Premium polish: motion, adaptive, delight (1–1.5 weeks) — **done**
- [x] **Motion tokens applied**: `core.designsystem.pressScale(interactionSource)` (takes the
  caller's source, so it tracks the same press the clickable sees) and `animateCounter()`
  (animates from the *previous* value, so an update reads as a change, not a re-count) — on the
  dashboard balance and accounts net worth. `NavHost` gained fade+scale transitions; a
  directional slide was rejected because the bottom bar jumps between unrelated tabs, where
  left/right implies an order that isn't there. Shimmer promoted out of `AiInsightsWidget`
  (which `DashboardSkeleton` was reaching across to borrow) into
  `core.designsystem.components`, plus a `ShimmerListPlaceholder` shaped like the real rows;
  the transactions list previously showed **nothing** while loading. **All of it honours
  `isReducedMotionEnabled()`** — press-scale no-ops, the counter snaps, transitions disable,
  and shimmer flattens to a static tint instead of looping.
- [x] **List-detail split for Transactions ≥600dp**: the editor docks beside the list instead
  of covering it. *Built on the app's existing `BoxWithConstraints` breakpoint, not
  material3-adaptive* — that catalog entry is declared but **wired into no module**, and is
  still `1.3.0-beta02`; adopting a beta pane scaffold for one screen would add a second
  adaptive idiom next to the one the shell and every editor already use. (Catalog entry left
  in place and still unused — delete or adopt it deliberately later.)
- [x] **Iconography**: nav icons were placeholders — Budgets used `Info`, Goals `Star`, and
  Reports a **heart** (`Favorite`); now PieChart / Flag / BarChart. Categories resolve through
  `categoryIconFor` + a new `categoryAccentFor`, which reads the same seeded hex the database
  holds rather than restating a palette in UI code.
- [x] **Empty + error states**: shared `EmptyState`/`ErrorState` in `core.designsystem`
  (illustration badge, title, explanation, optional CTA / retry), adopted by Transactions,
  Goals, Accounts, Budgets, and Recurring. The CTA is suppressed when a list is empty only
  because a filter matched nothing — offering "Add a transaction" there answers a question
  nobody asked.
- [x] **Haptics**: `core.util.Haptics`, intent-named (confirm / reject / toggle / longPress),
  on transaction save, swipe-to-delete, and the recurring pause switch. *No `expect/actual`,
  contrary to the original plan* — Compose Multiplatform 1.11 exposes `HapticFeedbackType` in
  common code with Android/iOS/skiko backends and already respects the system haptic setting,
  so the platform split would have been redundant.
- [x] **A11y — gauges**: both now speak. `BudgetCard` merges into one description stating the
  numbers and status in words; `GoalCard` scopes its description to the progress bar so the
  Add funds / Withdraw buttons stay separate targets. Budget status exists as localized words,
  not only as a colour. (Chart a11y landed in Phase 3.)
- [x] **Audit — contrast**: the roadmap's suspicion was right. The dashboard drew 1dp borders
  at `outline.copy(alpha = 0.05f)` — mathematically present, visually nothing — and tinted card
  containers at 15–20% `surfaceVariant`. The right tokens (`outlineVariant`, `surface`) already
  existed and every newer feature used them; the dashboard was just the oldest code. Fixed and
  pinned with a Konsist rule (skeletons exempt — a placeholder is meant to be low-contrast).
- [x] **Audit — font scale**: buttons pinned to `height(50/52.dp)` clip their label at 200%
  font scale. Now `heightIn(min = …)`: same visual floor, free to grow.
- [x] **Audit — touch targets**: already clean. Every interactive icon goes through
  `IconButton` (48dp minimum) and there are no small clickable boxes. Recorded so it isn't
  re-audited.
- [x] **Recurring management screen** (absorbed from Phase 3): list with next-run date,
  pause/resume, delete, and a full editor. Saving or resuming materializes immediately rather
  than waiting for the next launch — safe because `materializeDue()` is idempotent.
- [x] **WorkManager daily job** (absorbed from Phase 3): recurring entries land on their own
  day instead of at next launch. Android-only by design — iOS background execution is far more
  restrictive and Web has none; both keep open-time catch-up, which stays correct because
  entries carry their true occurrence dates regardless of when they were materialized.
- [x] **Konsist colour allowlist emptied**: carried since Phase 0, now zero exceptions. Closing
  it surfaced a real bug — `TopTransactionsList` matched category *names* ("food",
  "starbucks") while `Transaction.category` carries the *id* ("cat_food"), so every dashboard
  row silently drew a generic grey fallback.
- [x] **Schema migrations** (found by running the app, not by tests): three `.sq` edits had
  added columns with no version bump, so `Schema.create()` covered fresh installs while every
  *existing* one crashed with "no such column". Added `1.sqm` (v1 → v2) and `:core` tests that
  recreate the original v1 schema and migrate it — a fresh database proves nothing, since
  `create()` always emits the newest columns.
- [x] **Shared component library** — `EmptyState`/`ErrorState`/`ShimmerListPlaceholder`, the
  motion/haptics primitives, `parseHexColor` and the category icon/accent/name lookups all
  live in `:core`. Closed rather than carried: the remaining item was "also share
  `BalanceCard`/`TransactionRow`", but `BalanceCard` has exactly one caller (dashboard) and
  `TransactionRow` does not exist — there is no duplication to extract, and hoisting a
  single-use component into `:core` would add an edge for nothing.

### Phase 4.5 — In-app guidance: explain every screen (1 week) — **done**

**Approach — a per-screen guide sheet (as planned).** A `?` in each header opens a sheet
listing that screen's features: icon + name + one line each. It opens itself once on a
screen's first visit, then waits to be asked.

*Rejected: coach-mark/spotlight overlays.* They anchor to exact layout, and this app has three
adaptive layouts, five palettes, and a Wasm target — fragile, fights reduced-motion, and
interrupts rather than answers. *Also rejected: hints in empty states only* — they vanish
exactly when the screen has data and the questions start.

- [x] **`core.guidance`**: `GuidanceKey` per screen; `ScreenGuide` = title + intro +
  `List<FeatureNote>` (icon, title, body) holding **`StringResource`s rather than resolved
  text**, so the registry is a plain value and the copy follows the app's own language setting
  rather than the build locale. `GuidanceRegistry` keeps all seven guides in one file — Settings
  can list them without depending on seven feature modules, and a missing guide is a gap in one
  place instead of on the screen that lacks it. (Content is strings, and strings already live
  in `:core`, so there's no feature→feature edge.)
- [x] **`GuidancePreferences`** over `KeyValueStore`, mirroring `OnboardingPreferences`:
  per-screen "seen" flags + a global **"Show tips"** toggle. A guide is marked seen when it
  **opens**, not when dismissed — it *was* shown, and swiping it away is an answer. Reset
  deliberately leaves the toggle alone: someone who turned tips off and then reset shouldn't be
  nagged again.
- [x] **Shared UI**: `GuidanceSheet` (bottom sheet on phone, dialog ≥600dp — the same adaptive
  rule as every editor), `HelpIconButton`, and a `rememberGuidance` / `GuidanceHost` pair that
  costs each screen two lines.
- [x] **All 7 screens wired**, each note written against what the screen actually does,
  favouring the non-obvious: the switcher re-scopes the app, undo restores to the *original*
  account, budget spend is counted for you, transfers are excluded from income, goals project a
  finish date, the CSV includes transfers while the charts don't.
- [x] **Settings → "Help & tips"**: browse every guide on demand (enumerated from the registry,
  so a new screen's guide appears automatically), toggle auto-open, and **"Reset tips"** — next
  to the existing "Replay intro" row.
- [x] **Localized EN/FR as authored**, not retrofitted.
- [x] **A11y + motion**: notes are real text (screen-reader friendly, no image-only content);
  the sheet reuses the reduced-motion-aware primitives; every `?` has a content description.
- [x] Tests (6): every `GuidanceKey` has a guide and every guide explains something — so a new
  screen fails the build rather than shipping a `?` that opens nothing — plus seen/reset/toggle.
- [x] **Fixed `\'` rendering literally** (found by reading the running app). Compose resources
  are **not** Android XML: `aapt` unescapes `\'` and `\"`, compose-resources renders the
  backslash. The app had shipped "Don\'t have an account?" since Phase 0 — 69 escapes across 55
  strings, worst in French, which uses apostrophes constantly. Every phase compiled green
  because nothing ever read the screens. Fixed all, plus a test that scans the resource files.

**Deliverable:** every screen answers "what can I do here?" without leaving the app; tips
appear once, never nag, and can be replayed from Settings; fully EN/FR. Verified on device.

### Phase 4.5 — why the guide sheet, and what was rejected

> Kept for the design rationale only. **All of it shipped** — the section above records what
> was actually built; the plan's checklist lived here in duplicate and is removed rather than
> left permanently unticked.

> The app has grown features that aren't self-evident — the account switcher re-scopes the
> whole app, swipe-to-delete has undo, transfers are deliberately excluded from income, a
> budget's `spent` is computed live, goals project a completion date. Onboarding covers the
> pitch, not the mechanics. This phase makes every screen explain itself **in the app**.

**Approach — a per-screen guide sheet (chosen).** Each screen gets a `?` in its header that
opens a sheet listing that screen's features: icon + name + one line each. It shows itself
once automatically on a screen's first visit, then stays available on demand.

*Rejected: coach-mark/spotlight overlays.* They point at exact pixels, which means anchoring
to layout — and this app has three adaptive layouts (bottom bar / rail / drawer), five
palettes, and a Wasm target. That's a lot of fragility, it fights reduced-motion, and it
interrupts rather than answers. The sheet is layout-independent, replayable, screen-reader
friendly, and cheap to keep truthful as screens change. *Also rejected: hints in empty states
only* — they vanish exactly when the screen has data and the questions start.

**Deliverable:** every screen answers "what can I do here?" without leaving the app; tips
appear once, never nag, and can be replayed from Settings; fully EN/FR.
**Placement:** after Phase 4 so the UI is settled (explanations of a moving target rot), and
before Phase 5 so the localization audit covers this copy. The guides also make good
Phase 8 screenshots.

### Phase 5 — Localization ✅
- [x] Extract remaining strings; add **French** (`values-fr/strings.xml`) as second locale —
  CMP resources handle runtime locale switching.
- [x] Locale-aware date/number/currency formatting everywhere (no `$` literals).
- [x] **Built-in category names are localized.** `DefaultData` seeds English names into the
  database, so every category chip, budget card, and dashboard row read "Food & Dining" in
  French. `categoryNameFor` resolves seeded ids through `category_*` resources and leaves
  user-created names alone. A test pins every seeded id to a string in every locale.
- [x] **Day/month order and the 12- vs 24-hour clock live in the locale files**, not in code:
  `dateTimeLabel` supplies every component and each translation uses the placeholders its
  convention calls for ("January 20, 10:45 PM" / "20 janvier à 22:45").
- [x] Dropped English `contentDescription` overrides that replaced localized button labels
  with an English accessible name regardless of app language (16 in total across Phases 4.5–5).
- [x] **Truncation pass — done, and it found real gaps.** `LocalizationScreenshotTest` renders the
  dashboard in EN and FR via Roborazzi (no sign-in needed).
  - Android's `en-XA` pseudo-locale was **not** used: it inflates strings at the *framework*
    resource layer, but this app's strings come from compose-resources, which would fall back to
    English and prove nothing. French is the real second locale and ~20% longer — the honest test.
  - **Caught: the quick actions ("Add Expense"/"Add Income"/"Transfer") and the period chips
    ("Week/Month/Year/All") were hardcoded English** and rendered so in a French UI. The Phase 5
    grep sweep missed them because they are `label =` arguments and a `when` returning bare
    strings, not `Text("…")`. Now localized; French wraps to two lines rather than clipping.
  - The golden deliberately uses an **empty chart**: Vico populates through a coroutine, so
    Robolectric captured whichever state it happened to be in and the image flickered between
    runs with no code change. Empty is deterministic *and* covers the localized "no data" string.
- [x] **RTL smoke test — done, and it found latent BiDi bugs.** Key screens render with
  `LocalLayoutDirection = Rtl`. Layout mirroring is clean (no hardcoded left/right padding), but
  bidirectional text reordering mangles number+symbol strings:
  - `+2.4%` renders as `2.4%+` — the sign moves to the wrong end.
  - `450,00 $US sur 500,00 $US` scrambles to `US sur 500,00 $US$ 450,00`, and clips.
  - The balance flips from `12 450,80 $US` to `US$ 12 450,80`.
  - **Not fixed, on purpose:** EN and FR are both LTR, so none of this is user-visible today. The
    fix is Unicode BiDi isolation (FSI/PDI) around formatted amounts in `MoneyFormatter` and the
    trend/percentage strings. Doing it now would be speculative work against a language the app
    doesn't ship; the snapshots exist so whoever adds an RTL language starts from a known list
    rather than a surprise.
- [x] **The screenshot job was silently broken.** `DashboardScreenshotTest` stopped compiling when
  `BalanceCard` gained a `currencyCode` parameter, so `verifyRoborazziDebug` — which CI does run —
  could not have passed since the Phase 3 currency work. Fixed.

> Not verified on device: the dashboard sits behind sign-in, so the localized category names
> and date format are covered by compile + guard tests rather than a screenshot.

### Phase 6 — Production hardening & release (1.5–2 weeks)
- [x] **Gemini key removal — closed for release safety.** A release build now refuses to embed
  the key: `generateDashboardConfig` forces the generated constant empty for any Release/bundle
  task, and the service disables itself without one, so no secret can reach a public bundle
  (verified: the same `GEMINI_API_KEY` yields the key in a debug build and `""` in a release
  build). AI insights are therefore a **debug-only** feature until Phase 7 routes them through
  Firebase AI Logic, which remains the real fix and is planned there.
  - Two things found while doing this, both fixed:
    - **The prompt shipped the raw ledger** — every transaction's description (free text, where
      users write names), timestamp and id went to Google. It now carries **aggregates only**
      (per-category totals + income/expense sums), pinned by a test.
    - **With no key the app invented insights.** `getMockInsights()` returned hardcoded French
      claims like "your coffee spending rose 15%" — fabricated figures presented as analysis of
      the user's own money, in *every* build without a key. Deleted; the state is now
      `InsightsState.Unavailable` and the dashboard omits the section. The prompt language also
      followed a hardcoded "French" rather than the app's language.
- [x] **Web auth: degrades gracefully** (landed in Phase 1.6; verified in Phase 6). GitLive has
  no Wasm target, so `WasmAuthRepository` keeps a durable local-only profile in `localStorage`
  instead of a throwing stub. There is no `UnsupportedOperationException` left in the codebase;
  the only `throw` on the web path is a typed `AuthError.GoogleUnavailable` behind an
  unreachable guard (`isGoogleSignInSupported` is false on Wasm, so the Login screen never
  offers the button). Firebase JS interop stays unbuilt on purpose — a browser-only account
  with no sync is what the local profile already gives.
- [x] **Firebase config hygiene** — already correct, confirmed rather than changed. A **dummy**
  `google-services.json` (project `budgetmaster-dummy`) is committed so the google-services
  plugin can build any clone and CI; the real `budget-master-4c24f` config — API key, OAuth
  client ids, SHA-1 hash — stays on the developer's machine and is never committed. The iOS
  plist is gitignored. Use `git update-index --skip-worktree composeApp/google-services.json`
  so a local real config stops showing up as a pending change and cannot be committed by
  accident.
  - Per-**build-type** config is deliberately not added: it would need `applicationIdSuffix`,
    and the registered SHA-1 is bound to `com.budgetmaster` (see the release-engineering note
    above). Revisit only if a separate Firebase project for debug is ever wanted.
- [x] **Crashlytics + analytics** wired on Android (`composeApp`), with the Crashlytics Gradle
  plugin so the R8 mapping file is uploaded — without it every release stack trace is obfuscated
  and the reports are worthless. Collection is **off in debug builds**: a crash while someone is
  developing is not a signal about the shipped app, and letting those through buries the real
  reports. Crashlytics gets stack traces and device metadata only — nothing logs a transaction,
  an amount, or an email, and that must stay true.
  - The App Check provider moved into **variant source sets** (`src/debug` vs `src/release`).
    `firebase-appcheck-debug` is a `debugImplementation` dependency, so referencing
    `DebugAppCheckProviderFactory` from `src/main` compiled fine in debug and broke the release
    build — found by actually building release, not by reading.
  - Verified on the emulator: clean start, Crashlytics initializes, no crash; both `assembleDebug`
    and `assembleRelease` build.
- [ ] Performance monitoring; Android Baseline Profiles.
- [x] **Release engineering.** `isMinifyEnabled = true` + `isShrinkResources = true` with a
  written `proguard-rules.pro` (kotlinx.serialization's reflective `.serializer()` lookup, Ktor's
  ServiceLoader engines, Koin, SQLDelight, GitLive/Firebase, and the `@Serializable` nav routes —
  all things R8 cannot see from the code, and the classic source of "worked in debug, crashed in
  the store build"). Release APK builds clean through R8.
  - **Signing** reads `keystore.properties` (gitignored, with a committed `.example`) or the
    `ANDROID_*` env vars in CI. No keystore or password is committed; without them the release
    build stays *unsigned* rather than failing, so anyone can still verify the build.
  - **Version from git tags**: `v1.2.0` → versionName `1.2.0`, versionCode `10200`. `git
    describe` needs `isIgnoreExitValue` — it exits 128 when no tag matches, which is true of this
    repo today, and would otherwise fail every build. The release lane checks out with
    `fetch-depth: 0` or the fallback would silently mislabel the artifact.
  - **CI release lane** on `v*` tags: AAB + APK + Wasm distribution, keystore decoded from a
    secret and deleted afterwards. `GEMINI_API_KEY` is deliberately not passed.
  - Deliberately **not** added: `applicationIdSuffix = ".debug"`. `google-services.json` declares
    a client for `com.budgetmaster` only and the registered SHA-1 is bound to it, so suffixing
    the debug id would fail the google-services plugin and break Google sign-in.
- [x] **CI gaps closed.** Host tests ran for `:feature:auth` and `:feature:dashboard` only, so
  none of `:core`'s guard tests (string escaping, EN/FR parity, category-name localization,
  schema migration) or `:shared`'s Konsist architecture rules ever ran; it is now
  `./gradlew testAndroidHostTest` across every module. Added a **compile-only iOS job** —
  compiling the Kotlin iOS target needs no macOS, and nothing building it is what let `iosMain`
  drift into code that could never compile.
- [x] **Offline-first sync — explicitly de-scoped.** The app is local-only per device and the
  README now says so. Last-write-wins was the sketched design and is a poor fit for a ledger:
  two devices editing the same transaction would silently discard one edit, and "the money app
  quietly lost a record" is the worst bug this app could ship. It also can't be validated from
  this host (needs multiple devices + console). Real sync deserves its own phase with a
  conflict model chosen on purpose.
  - The Firestore dependency was declared in **4 source sets but imported nowhere** — removed,
    so it stops being pulled into every build. The catalog entry stays for the future work.
  - While updating the README, its feature list turned out to advertise **receipt scanning,
    tags, heatmaps, PDF export and automatic sync — none of which exist** ("receipt" was a
    `ReceiptLong` icon; `tags` is a column always written null). Corrected, with an explicit
    "Honest scope" section, since these are exactly the claims that end up in a store listing.
- [x] **Store readiness — drafted from the code** in `docs/`: `PRIVACY_POLICY.md`,
  `PLAY_DATA_SAFETY.md` (answers *with the reason beside each*, since a wrong answer here is how
  apps get pulled), and `STORE_LISTING.md`. Every claim is one the app actually delivers —
  the listing even carries an explicit "what it is not" section, because the README's old feature
  list advertised receipt scanning, tags and sync that never existed, and a store listing is
  where that becomes a refund request.
  - [x] **Account-deletion route — done.** Play requires one for any app with accounts.
    `DeleteAccountUseCase` deletes the auth credential first (so a re-auth failure leaves data
    intact rather than orphaning it) then `UserDataEraser` wipes every user-scoped table in one
    transaction — transactions/recurring included, resolved through the account rows, plus the
    derived insight cache. Settings has a destructive "Delete account" action behind a
    confirmation dialog; the shell routes to Login on success and Settings shows the re-auth
    hint inline on failure. Eraser test proves the wipe is complete and spares other users.
  - Also needs a human: a **contact email**, a **hosted URL** for the policy (Play requires a
    public link), the **feature graphic/icon**, and a decision on Gemini retention for the
    "processed ephemerally" checkbox.
- [ ] Screenshots for the listing (Phase 8).

### Phase 7 — AI intelligence layer (free-tier Gemini via Firebase) (2–2.5 weeks)

> Strategy: **on-device first, free cloud tier second, never a raw API key in the client.**
> All cloud calls go through **Firebase AI Logic** (Gemini Developer API free tier, App
> Check protected). On-device models (Gemini Nano via ML Kit GenAI / AICore) handle quick,
> private tasks on supported hardware. Privacy guardrails apply to every feature: prompts
> carry **aggregates only** (never the raw ledger, names, or emails), AI is an **opt-in
> toggle in Settings**, and every insight carries a "not financial advice" disclaimer.

**7.0 — Foundation**

> Split by what can actually be built here. The guardrails and robustness are done; the
> Firebase AI Logic migration itself is **console- and Xcode-gated** and is the remaining work.

- [x] **"AI features" master switch in Settings — opt-in, off by default.** `AppSettings.aiEnabled`
  defaults to false and an absent preference is never read as consent. The gate sits *before the
  call*, not around the UI: until the user opts in, nothing about their spending leaves the
  device, pinned by a test that fails if the service is reached without consent. The Settings
  copy names exactly what is and isn't sent rather than saying "enable AI insights".
- [x] **"Not financial advice" disclaimer** rendered with the insights themselves — a model's
  confident sentence about someone's money reads as advice unless it says otherwise.
- [x] **Structured JSON output** via `responseSchema`, enforced by the API instead of asked for
  in prose (free text that merely looks like JSON is the usual parse failure). `actionRoute` is
  an enum, so the model cannot invent a screen that doesn't exist.
- [x] **Exponential backoff for free-tier 429s.** A 429 means "wait", not "give up" — the service
  fell back to a stale cache on the first one. Only 429 retries; a 400/403 fails identically
  however long you wait, and a test pins that it is *not* retried. The backoff schedule is
  injectable so tests don't sleep. The 24 h SQLDelight cache is unchanged.
- [x] **`GenAiClient` + Firebase AI Logic migration — done on Android.** `core.ai.GenAiClient` is
  a provider-agnostic seam (no business logic): `generateJson(prompt, schema)` plus `isAvailable`
  and a typed `GenAiException`, with a small `GenAiSchema` so no vendor schema type leaks into
  `:core`'s API or the features. The Android actual uses `Firebase.ai(backend =
  GenerativeBackend.googleAI())` with `gemini-3.5-flash`.
  - **There is no API key in the app any more, on any build type.** The `GEMINI_API_KEY`
    BuildConfig generation, the release-build guard that forced it empty, and the whole
    GenerateContent REST envelope (`GeminiRequest`/`GeminiResponse`/`Content`/`Part`) are all
    deleted. The safest secret is the one that doesn't exist — and AI insights now work in
    *release* builds, which the key-stripping approach could never allow.
  - **iOS and Web return `isAvailable = false`** and the surface hides itself. Firebase AI Logic
    has no KMP wrapper: iOS needs the Swift SDK bridged from Xcode (macOS), Web needs Firebase JS
    interop. Calling Gemini's REST API directly from either would reintroduce the embedded key.
  - Tests now fake `GenAiClient` instead of mocking HTTP — the seam is the interface, so they say
    what they mean.
- [x] **App Check** wired on Android: Play Integrity for release, the debug provider for debug
  builds. Not optional hardening — Google began auto-enforcing App Check for AI Logic in early
  July 2026, so without it the calls are simply rejected.
  - **Per-machine setup:** each debug build prints a token to logcat
    (`DebugAppCheckProvider: Enter this debug secret…`) that must be registered once under
    **App Check → Apps → Manage debug tokens**. A new machine or emulator needs a new token.
  - Verified on the emulator: the app starts, `FirebaseApp initialization successful`, App Check
    installs and issues a debug token, no crash. The insights call itself is **not** verified
    end-to-end — that needs a registered debug token plus a signed-in user with the AI toggle on.
- [x] **Feature flags via Firebase Remote Config — done (Android).** `RemoteFeatureFlags` is a
  small expect/actual: the Android actual reads Firebase Remote Config (synchronous cached reads,
  in-app default `ai_features_enabled = true`, background `fetchAndActivate` on app start);
  iOS/Web return the defaults. Wired as a **server-side kill-switch with zero per-feature
  plumbing** — the Android `GenAiClient.isAvailable` consults it, and every AI surface already
  gates on `isAvailable`, so flipping the flag in the console takes them all dark without an app
  update. Per-*feature* flags (separate keys per AI feature) are a trivial extension on this.

**7.1 — Smart capture** (needs Phase 1 transactions)
- [x] **Natural-language quick add — done.** `ParseQuickEntryUseCase` turns "coffee 4.50
  yesterday" into draft fields (amount, expense/income, description, category, date) that prefill
  the add-transaction form for the user to confirm — AI drafts an entry, it never records money.
  Built on the shared `GenAiClient` seam with a structured schema, so `categoryId` can only be a
  real seeded id and the date is resolved **on device** from a relative `daysAgo` the model
  returns (never a wall-clock timestamp it would guess wrong). Same consent gate as every AI
  surface: the field shows only when a provider exists *and* the user opted in, and the prompt
  carries only the note the user just typed plus the category list — no ledger. 9 tests.
- [x] **Auto-categorization — done.** `SuggestCategoryUseCase` suggests a category from a typed
  description as a dismissible chip in the editor, with the learned `description → categoryId` pair
  cached in `KeyValueStore` so each merchant is asked once (a cache hit never calls the model).
  Enum schema so it can't invent a category; failures swallowed to null. 5 tests.
- [x] **Receipt scan — done (Android).** "Scan a receipt" in the transaction editor: pick a photo,
  ML Kit reads it **on-device**, and `ParseReceiptUseCase` turns the extracted text into the same
  draft fields quick-add produces, for review before saving.
  - **The image never leaves the phone.** OCR is local and free; only the recognised text (capped
    at 4k chars) is summarised to the model. A receipt photo shows the card's last digits, the
    address and the full basket — uploading it to a cloud OCR was rejected on exactly that basis.
  - Uses the **system photo picker**, so it needs *no camera or storage permission at all* — the
    user hands over one image. Also a better Play data-safety answer than requesting CAMERA. Full
    resolution too; a camera-preview thumbnail is far too low-res to OCR.
  - Always `isExpense = true`: a receipt is a purchase, and defaulting to income on a misread
    would silently inflate the balance. Hallucinated categories are dropped; a missing total is a
    typed `NoAmount` rather than a guess. 8 tests cover the parse half end to end.
  - iOS/Web return `isAvailable = false` and the action hides itself (ML Kit is a native SDK with
    no KMP wrapper).
  - **Needs your manual check** — see the test script in the session notes; the picker + real OCR
    can't be exercised without a device.

**7.2 — Coaching & analysis** (needs Phases 2–3 budgets/reports) — **done**
- [x] **Monthly narrative summary — done.** `GenerateNarrativeUseCase` sends the report
  aggregates (never raw transactions) and the app's language; the AI coach card on Reports shows
  a one-tap "summarize this period" in EN/FR with the not-financial-advice disclaimer.
- [x] **Budget suggestions — done.** `SuggestBudgetsUseCase` proposes per-category limits from a
  locally-computed 3-month average, with one-tap apply on the Budgets screen. The model proposes
  a round limit and reason; the number is clamped to a defensible band (never below the average,
  never more than 2× it) so a hallucination can't land in the user's budget.
- [x] **Subscription & anomaly detection — done.** `DetectRecurringChargesUseCase` finds likely
  subscriptions **entirely on device** — expenses that recur across ≥2 distinct months at a
  consistent amount — surfaced in a card on Transactions. The detection is local by design (which
  merchants you pay monthly shouldn't need to leave the device); the optional AI *labeling* of
  flagged items is the one remaining nicety, not the value.
- [x] **Finance Q&A — done.** `AnswerFinanceQuestionUseCase` answers a free-text question from the
  report aggregates only, in the user's language, and is told to say it can't rather than invent a
  number. *Deviation from the sketch:* rather than parse the question to a bespoke SQL query, it
  gives the model the same aggregated report the narrative uses — simpler, and still "raw
  transactions never leave the device".

**Cost & quota posture**: free tier only — aggressive local caching (existing
`InsightEntity` pattern), per-feature daily request budgets, batch prompts, and Remote
Config kill-switches if quotas tighten.

### Phase 8 — Mobile-money capture (the retention bet)

> The strategic wedge. Manual entry is the single biggest reason personal-finance apps get
> abandoned, and the incumbents solve it with bank APIs that barely exist in XAF/NGN markets.
> Mobile-money messages are the local equivalent of a bank feed, and they parse on-device.

- [x] **Orange Money parser** — golden corpus of real messages. The transaction id encodes the
  time (`MP250704.0013` → 2025-07-04 00:13) so entries carry the moment the transaction happened,
  not when the SMS landed, and the id doubles as the dedup key. Direction needs the user's own
  MSISDN: "Transfert de A vers B" reads identically both ways. Per-field extraction rather than
  whole-message patterns, because five real samples already spell one field three ways.
- [x] **Schema v3** — `externalId` + `source` on transactions with a partial unique index (many
  NULLs for manual rows, one row per provider transaction), and `ImportedMessageEntity` for
  idempotency, the review queue, and an audit trail. Message bodies are never stored.
- [x] **Ledger mapping** — three dedup layers (fingerprint → provider id → same-day/same-amount
  against hand-entered rows). Fees become their own `cat_fees` entry so the balance reconciles
  *and* "what did mobile money cost me" is answerable. Imported principals stay uncategorised: a
  wrong category silently skews budgets where nobody thinks to look.
- [x] **Automatic SMS capture** — broadcast receiver plus inbox backfill on opt-in, both through
  one importer. Sender allowlist applied before the body is read; multi-part SMS reassembled.
  `RECEIVE_SMS`/`READ_SMS` are Play-restricted, so a store release needs a Permissions
  Declaration and may be refused — a distribution question, not a code one.
- [ ] **MTN MoMo parser** — blocked on real samples. MTN messages are currently recorded as
  ignored rather than mis-parsed, which is the safe failure.
- [x] **Paste / share fallback — done.** An `ACTION_SEND` (`text/plain`) filter plus a paste field
  on Transactions, both routed into the existing `ImportMoneyMessageUseCase`. Needed three ways:
  if the Play permission is refused, on **iOS where SMS access does not exist at all**, and for
  messages the receiver missed. Dedup collapses a pasted message against a later automatic capture
  on the provider transaction id, even though the message fingerprints differ.
  - Parser selection falls back to matching on the **body** when there is no sender — but *only*
    then. An unrecognised sender is still refused: the allowlist is a privacy guarantee that
    ordinary SMS are never inspected, not an optimisation, and a test pins that boundary.
- [x] **Review queue UI — done.** A suspected duplicate used to import nothing and say nothing,
  which is the worst of both: the importer had a real doubt and the user never heard it. The
  deferred message now appears at the top of Transactions with the two answers phrased from the
  user's side — "Already have it" and "Add it" — because they are being asked about their money,
  not about our record.
  - Schema v4 (`3.sqm`) was needed to make the question answerable at all. Message bodies are
    deliberately never stored, so a pending row knew *that* something needed review but nothing
    about *what* — "add it" had nothing to add. The parsed fields are now kept for
    `PENDING_REVIEW` rows only, and cleared the moment the review is answered. They are the same
    aggregates the resulting entry holds, so the body is still never written to disk.
  - `ImportEntryFactory` is shared between first import and review resolution. Two copies would
    drift, and the failure is silent: a reviewed import missing its fee row leaves the balance
    quietly wrong. A test pins that the fee row still lands.
  - The share-sheet path now says "sent to review" instead of "skipped", which was simply untrue.
  - Four repository tests run against a real driver rather than a fake, because the whole claim is
    that the parsed fields survive a write and a read back — a fake handing its own objects back
    would prove nothing about the columns.

### Phase 9.3 — Warning and deprecation sweep

> 95 compiler warnings down to 2. The point was not the count: at 95 a real one has nowhere to
> show, and two of these turned out to be genuine bugs.

- [x] **iOS CSV export was broken.** `(content as NSString)` cannot succeed — a Kotlin String is
  not an instance of the Objective-C NSString class, and bridging only happens implicitly at an
  interop boundary, never for an explicit Kotlin-side cast. Every iOS CSV export would have
  thrown. Now written through `NSData`, which also pins the UTF-8 the CSV claims to be.
- [x] **Directional icons did not mirror in RTL.** Seven icons (`TrendingUp`, `ArrowForward`,
  `CompareArrows`, `ReceiptLong`, `Rule`, `HelpOutline`, `TrendingDown`) used the deprecated
  non-mirroring variants, so an RTL layout got LTR arrows. Moved to `Icons.AutoMirrored`. The two
  RTL snapshots changed and nothing else did, which is the proof it was RTL-only.
  - Worth a native RTL reader's eye: the trend arrow now rises leftward, which is correct for a
    right-to-left time axis but is the kind of call worth confirming with someone who reads that
    way daily.
- [x] `monthNumber` → `Month.number` (kotlinx-datetime deprecation, needs its own import as it is
  an extension property).
- [x] `confirmValueChange` on `rememberSwipeToDismissBoxState` is deprecated in favour of leaving
  disallowed anchors out of the set. The swipe direction was **already** restricted by
  `enableDismissFromStartToEnd = false`, so the veto was redundant; the delete now runs off the
  settled state instead of from inside a callback used for its side effects.
- [x] `-Xexpect-actual-classes` set once in the root `subprojects` block. That was 24 of the 95:
  the feature is Beta upstream (KT-61573), load-bearing here, and has no alternative spelling, so
  the flag acknowledges it rather than pretending the warning was actionable.
- [x] File-level opt-ins for `ExperimentalWasmJsInterop` (5 wasm files that are interop by nature)
  and `ExperimentalCoroutinesApi` (the settings test).
- [x] Dead `!!`, redundant casts, an unnecessary safe call, and a stray `Unit` removed.
- [x] **`feature/accounts` was missing from detekt's source list** and had never been linted.
- [ ] **Two warnings remain, deliberately.** `org.jetbrains.compose`'s `@Preview` is deprecated in
  favour of the androidx one, which does not resolve on wasmJs — taking the replacement trades a
  warning for a broken web build. Both sites are commented; revisit when the replacement covers
  every target.
- [ ] **Detekt still reports ~900 findings**, `ignoreFailures = true`. Overwhelmingly formatting
  (`ArgumentListWrapping` 371, `Indentation` 156, `ImportOrdering` 57). Auto-correctable, but that
  is a whole-repo reformat that would bury real changes in review, so it wants its own commit.
  - Note for whoever does it: **`NoUnusedImports` cannot be trusted here.** Detekt runs without
    type resolution, so it flags imports that are genuinely used — acting on its 33 findings broke
    the build in three modules. Verified this the hard way; use the compiler, not detekt, for it.

## Phase 11 — Real-device bug fixes

> Seven defects from the first real-device test session, plus three found while investigating them.
> All on branch `feature/device-fixes-and-sync`; nothing merges to `main` until the whole branch is
> verified on device.

### 11.1 Dashboard quick actions — done

- [x] **"Add expense" / "Add income" / "Transfer" did nothing at all.** The buttons were wired
  end to end at the MVI layer and had a *passing* ViewModel test asserting the effect was emitted.
  They died in the UI: the screen's effect collector ended in `else -> Unit`, and
  `NavigateToAddTransaction` fell into it. Testing the emission proved only half the wire existed.
  - The collector is now an **exhaustive `when` with no `else`**, so an unrouted effect is a
    compile error rather than a dead button. This is the actual fix; the rest is plumbing.
  - There was also nowhere to navigate *to*: `AuthRoute.Transactions` and `AuthRoute.Accounts` are
    now `data class`es carrying `openEditorFor` / `openTransfer`, and the editor pre-selects the
    kind the button named.
  - **Transfer routes to Accounts, not the transaction editor** — a transfer writes two linked legs
    between the user's own wallets, which the editor has no concept of.
  - Trap found while doing it: once a route carries arguments, `destination.route` stops equalling
    the bare qualified name, so the four `currentRoute == …qualifiedName` comparisons in the shell
    would have silently hidden the whole navigation bar on those tabs. Now matched with
    `hasRoute`, which is indifferent to arguments.
  - `onInsightNavigate` was never passed by the nav graph either, so AI insight taps were dead
    too. Now wired, with the mapping closed over the three values the AI schema permits.

### 11.2 Dashboard swipe-to-delete had no undo — done

- [x] Found by making the `when` exhaustive: `ShowUndoDelete` and `ShowError` **were** being
  emitted and silently dropped, so swiping a row on the dashboard deleted it permanently with no
  acknowledgement, and errors were invisible. In a finance app that is the worst kind of dead code.
  - The display model was too lossy to undo from — it carries no `accountId`, `externalId` or
    `source`, so restoring from it would have put the money in the wrong wallet and broken
    mobile-money dedup, letting the provider's next re-send import a second copy. The repository
    now captures the full row as a `DeletedTransaction` snapshot before deleting and restores from
    that, via the one insert that carries `externalId` and `source`.
  - Two effect cases (`NavigateToAnalytics`, `NavigateToBudgetDetail`) were **deleted**: nothing
    ever emitted them. They were speculative API that the `else ->` branch kept invisible.

### 11.3 Phone-number field cursor jumped backward — done

- [x] Every keystroke round-tripped through a DataStore write and a 7-way `combine` before coming
  back to the field, and a plain `String` value carries no selection for Compose to preserve, so
  the cursor snapped when the delayed value landed. The field now holds a local draft and never
  reads the echo back. Null means "nothing typed yet", so the persisted value still shows on first
  load — seeding eagerly would have blanked the field, because the state flow starts empty and
  fills in a moment later.
- [x] The write path was also unordered: one `viewModelScope.launch` per keystroke gave no
  guarantee, so fast typing could persist an *older* string. Now funnelled through a conflated
  flow and debounced, which also stops hammering the disk.
- [ ] Same shape exists in seven other fields (search, login/register/forgot-password email and
  password) that round-trip through an in-memory `MutableStateFlow` rather than disk, so they are
  far less visible. Worth converting together; not urgent.
- [ ] Separately: ten amount fields filter inside `onValueChange`, which moves the cursor when a
  rejected character is typed mid-string. Needs `TextFieldValue`.

### 11.4 First install reached the Dashboard without signing in — done

- [x] Splash was correct. The break was downstream: onboarding branched on
  `isBiometricAuthSupported`, true on Android, so a first install went **Splash → Onboarding →
  Biometric → Dashboard** and never asked anyone to sign in. Every button on the biometric screen,
  including "Skip", led to the Dashboard.
  - Onboarding now always routes to Login. Biometric setup is an *enrolment* step: it protects an
    account, so it can only sensibly run once there is an account to protect.
  - **"Bonjour, vous" was not a separate bug** and was deliberately left alone — it is the honest
    rendering of no session. Patching the greeting would have hidden the routing bug.
- [x] **Added an auth guard**, because the instance was a symptom of the class: routing was
  entirely imperative, so whoever called `navigate()` decided and nothing checked afterwards.
  Losing the session while the app was open also left the user sitting on their finances. Now, if
  the session is gone and the user is on a signed-in destination, they go to Login regardless of
  how they arrived.
- [x] Removed the dead onboarding→biometric edge. `AuthRoute.Biometric` is deliberately kept but
  currently unreachable; the app-lock phase re-enters it from after sign-in, where it will
  actually gate something.

### 11.5 Google sign-in failed the first time, worked the second — done

- [x] Two causes, both fixed.
  - The catch chain mapped **every** remaining `GetCredentialException` to `GoogleUnavailable`,
    so a cold-started provider told users with a perfectly good Google account that their device
    did not support Google sign-in. There is now a separate `GoogleTransient` error, because the
    two need opposite advice: "try again" versus "this will not work here".
  - The request used a single, non-retried `setFilterByAuthorizedAccounts(false)`, which forces
    the provider to cold-start and enumerate every account on the device — the call most likely to
    fail first time. It now asks for **authorized accounts first** and only falls back to the full
    enumeration on `NoCredentialException`, which is the documented order and the fast path for
    returning users. `GetCredentialUnknownException` and `GetCredentialInterruptedException` get
    one retry, so the "second tap" now happens without the user having to think of it.
- [x] The spinner starts at the tap rather than when a token returns. The sheet can take a moment
  to open on a cold provider, and the button looked dead for exactly that window.
- [ ] The exception `cause` is still discarded before it reaches anywhere observable, so the next
  failure of this kind is undiagnosable. Needs a logging abstraction `:feature:auth` can use from
  `commonMain` — deliberately not invented here; folded into the secure-logging work.

### 11.6 AI requests rejected as invalid App Check tokens — code side done

> The ~97% rejection rate is **environmental, not a code fault**. Init order is correct and the
> debug/release provider selection via variant source sets is the right pattern. What the code got
> wrong was making the failure invisible.

- [x] **`GenAiClient` had no generic catch.** Any `FirebaseAIException` subtype not named
  explicitly escaped `generateJson` un-wrapped, past callers that only catch `GenAiException`.
  There is now a catch-all for the SDK's hierarchy.
- [x] **App Check refusals get their own error**, `GenAiException.NotAuthorized`, and their own
  copy. Retrying cannot fix them and the fix is not the user's, but the old generic
  "could not reach the AI, try again in a moment" invited exactly the retry that kept failing —
  which is how a 97% rejection rate passed for a flaky network for so long.
- [x] `setTokenAutoRefreshEnabled(true)`, which was unset, so refresh followed the global
  data-collection default and an expiring token was renewed only after something already failed.
- [x] README now spells out the trap that actually caused this: the debug token is stored in
  SharedPreferences and is **regenerated on every fresh install or clear-data**, so a burst of
  reinstall-driven testing invalidates it repeatedly. Also documents where to see the rejection
  rate in the console, and that release builds must come from Play.
- [ ] **Operational, needs doing on your side:** register the current debug token in
  Firebase Console → App Check → Manage debug tokens. No code change will fix the rejections.

### 11.7 Category names shown in English in a French UI — partly done

> Root cause: default categories are seeded into the database as **English literals** at first
> run, once, guarded — so the stored name is English whatever the app language is, and re-seeding
> will not fix an existing install. `categoryNameFor(id, storedName)` exists to compensate by
> mapping the eleven seeded ids to string resources. The design was right; adoption was patchy.

- [x] **The reported one:** the editor's category picker localised, but the row it produced did
  not. A category chosen in French reappeared in English the moment it was saved.
- [x] The category icon's accessibility label was the raw English name too.
- [ ] **Search still matches the stored name**, so searching "Alimentation" in French finds
  nothing. Deliberately not half-fixed: the filter is a pure predicate in the domain layer, and
  `categoryNameFor` is `@Composable`. Doing this properly means a non-composable localized lookup
  (compose-resources' suspend `getString`) or passing a resolved id→name map into the filter.
- [ ] **Reports and budget suggestions bake the raw name in the data layer**, so they cannot be
  localized at render time at all — the id never reaches the UI. Needs those models to carry
  `categoryId`, which is a wider change than the render-site fixes above.
- [ ] `NotifyBudgetThresholdsUseCase` builds **hardcoded English sentences** in the domain layer.
  Folded into the notifications-screen work, which has to localize that producer anyway.
- [ ] `TopTransactionsList` calls `categoryNameFor(id, id)`, so a user-created category renders as
  a raw UUID.

## Phase 12 — Security hardening

### 12.1 Data-at-rest and deletion — done

- [x] **`allowBackup` was `true` with no extraction rules**, so the unencrypted SQLDelight database
  and the DataStore preferences were eligible for Google Drive auto-backup and device-to-device
  transfer: a complete financial ledger leaving the device in cleartext, outside anything the app
  controls. Now `false`, with `data_extraction_rules.xml` (API 31+) and `backup_rules.xml`
  (API 30-) as belt and braces should backup ever be re-enabled. Both files are needed — the newer
  one is ignored on older versions, which would otherwise keep the permissive behaviour.
  - The convenience this removes is replaced by the encrypted export in Settings, where the user
    holds the passphrase and chooses where the file goes.
- [x] **`UserDataEraser` never erased the imported-message ledger.** "Delete my account" left
  behind a record of every mobile-money message the app had read — sender, arrival time, and for
  anything awaiting review the parsed amount and description. Arguably the most invasive table in
  the database, and the query to clear it already existed for exactly this purpose. One line.
  - Covered by a test that was **verified to fail without the fix**, rather than assumed to.

### 12.2 Screen-capture protection — done

- [x] `FLAG_SECURE`, applied from a new `secureScreen` setting: no screenshots, no screen
  recording, and no live thumbnail of the user's balances in the recents switcher. It has to be a
  window flag rather than anything in Compose, because the recents snapshot is taken by the system
  outside the app's own drawing.
- [x] **On by default**, unlike the other privacy switches. Those govern what leaves the device,
  where an unset preference must never be read as consent; this governs what a passer-by can see,
  and defaulting it off would ship balances visible in the app switcher until someone went looking
  for a setting. A toggle rather than fixed because it also blocks the user's own screenshots.
- [x] Collected for the activity's lifetime, so toggling it applies immediately rather than on
  next launch.

### 12.3 Secure logging — done

- [x] Audited every logging call in the app. The result was better than expected and worth
  recording: **no `android.util.Log` anywhere, and no Crashlytics `setCustomKey`, `log` or
  `recordException`** — so crash reports carry stack traces only, with no attached user data.
- [x] One real leak found and removed: `GeminiInsightsService` printed the exception message on
  failure. That path handles a failure while generating insights *from the user's spending*, so
  the message can carry prompt or response fragments — on Android that means the user's finances
  in logcat, readable by anything with log access on a rooted device and captured verbatim in bug
  reports. The fallback handling below it was always the real behaviour; the `println` added
  nothing.
- [x] **Konsist rule so the next one fails the build**, covering the packages where a stray line
  would be worst: SMS parsers (raw message bodies), repositories (amounts, account ids), AI
  services (prompts built from spending), and auth. Verified by planting a `println` in the SMS
  parser and confirming the rule fails — a rule that passes on a real violation is worse than none.
  - Deliberately a build failure rather than a review convention: the cost of logging is invisible
    at the call site and only shows up in someone else's log capture.
- [ ] The Google sign-in exception `cause` is still dropped rather than reported. Now that
  Crashlytics is confirmed clean, `recordException` is the right home for it — but it needs a
  `commonMain`-visible abstraction, which belongs with the app-lock work rather than here.

### Phase 9 — Insight & polish

> Two gaps found by using the app rather than reading it.

**9.1 — Income analysis (the missing half of Reports)**
- [ ] **Income by source.** The category donut is expenses-only — `spendByCategory` filters
  `amount < 0`, so "where does my money come from" has no answer. Add `incomeCategories` to
  `ReportSummary` and an **Expenses / Income toggle** over the existing donut: same component,
  same tests, one flag. Cheapest high-value change on the list.
- [ ] **Top payees / payers.** SMS import now captures `counterpartyName`, which makes "who am I
  sending money to, and who pays me" answerable — the question people actually ask, and one no
  competitor in this market answers well.
- [ ] **Fees card.** `cat_fees` exists now; "mobile money cost you 3 400 XAF this month" is a line
  no other app in the market shows.

**9.2 — Premium design pass**
> The foundation is strong (5 palettes, Outfit/Inter, motion tokens, adaptive layouts). What keeps
> it feeling Material-default rather than premium is specific and fixable.
- [x] **One chart implementation — done.** Android now uses the same `CanvasSpendingChart` as
  every other target and **Vico is removed entirely** (catalog, `:shared`, `:feature:dashboard`).
  One chart in the app palette, so all five themes apply everywhere, and one dependency fewer.
- [x] **Surface hierarchy — done, and now actually applied.** `SurfaceLevel` (Hero / Raised /
  Flat) with radius, container colour, elevation and border per level, consumed through a shared
  `AppCard`. Radius scales with importance; elevation stays deliberately low because heavy shadows
  read as dated Material rather than premium.
  - This was first checked off when only Reports consumed it, which overstated it — the mechanism
    existed but the app a user opens still looked like a stack of equal boxes. Now rolled out
    across Dashboard, Accounts, Budgets, Goals, Settings, Reports and Transactions.
  - The rollout turned up that the hierarchy was **inverted**: the dashboard balance used
    Material's default ~12dp radius while the Analytics section under it used 24dp, so the most
    important element on the screen was the least rounded. It now reads Hero 28 → Raised 20 →
    Flat 14.
  - One Hero per screen, enforced by choosing it deliberately: the balance on Dashboard, net worth
    on Accounts, the net figure on Reports. AI output is Flat everywhere — on the Dashboard as
    well as in Reports — because it comments on the money rather than being it.
  - `AppCard` gained `onClick` and `verticalArrangement` along the way. `onClick` is not sugar:
    Material's clickable Card overload keeps the ripple inside the rounded corners, whereas a
    `Modifier.clickable` applied outside the shape spills a rectangular ripple past them, which is
    precisely the bug the component exists to stop callers writing.
  - Error surfaces were deliberately left out. `SurfaceLevel` models hierarchy, and an error
    surface is a different axis — its colour carries meaning that a level would flatten away. They
    share the radius and nothing else.
  - 17 screenshot baselines changed. Each diff was inspected before re-recording rather than
    blanket-accepted; the balance-card diff traces only the card edge with pixel-identical
    content, which is what "we changed the radius and nothing else" is supposed to look like.
- [x] **Amounts as the hero — done.** `AmountText` gives money its own type scale (Hero /
  Prominent / Standard) with tabular figures, direction colour from the palette-independent
  financial colours, and bidi isolation in one place. Adopted at the dashboard balance and the
  net-worth total; remaining call sites can migrate incrementally.
- [x] **BiDi bugs fixed** — and verified by re-recording the RTL screenshot that found them.
  `+2.4%` no longer renders `2.4%+`, and amounts no longer scramble.
  - Isolation lives **inside each `MoneyFormatter` actual**, not at the call sites. The first
    attempt used a common extension, which would have needed a new import in ~15 files and left
    any new call site free to reintroduce the bug. The CSV export is unaffected because it writes
    the raw `Double`, never the formatter.
  - `formatSigned` strips the inner isolate and re-wraps once: nesting would have left the sign
    *outside* the isolate, and the sign is exactly the character RTL reordering moves.
- [x] **Richer empty and loading states** — already in place from Phase 4 and left alone:
  `EmptyState` puts the icon in a filled circle with a press-scaled action, `ErrorState` offers a
  retry, and `ShimmerListPlaceholder` covers loading. Checked rather than assumed; changing them
  would have been busywork.
- [x] **Animate value changes** — already covered by `animateCounter`, which is applied at the
  dashboard balance and net-worth total and snaps instead of animating under reduced motion.

### Phase 10 — Store polish & README showcase (0.5 week)
> After the feature set is complete and premium (post Phase 9), capture the app at its best.

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
| 2.5 | Accounts foundation / multi-wallet (uid binding, switcher, net worth, transfers, reconcile, FX) | 1 wk | ✅ done |
| 2.6 | Google sign-in (Android) | 0.5 wk | ✅ done — SHA-1 registered; needs on-device verification |
| — | Deferred-item sweep (Phases 1 / 1.5 / 2 / 2.5 + architecture drift) | — | ✅ done |
| 3 | Reports + recurring engine | 1.5 wk | ✅ done (WorkManager job + recurring UI landed in 4) |
| 4 | Motion & premium polish | 1–1.5 wk | ✅ done |
| 4.5 | In-app guidance — every screen explains its features | 1 wk | ✅ done |
| 5 | Localization | 0.5–1 wk | ✅ done (pseudo-locale + RTL passes ride with 6) |
| 6 | Hardening & release | 1.5–2 wk | ⬜ |
| 7 | AI intelligence layer (free-tier Gemini via Firebase) | 2–2.5 wk | ⬜ |
| 8 | Store polish & README screenshots | 0.5 wk | ⬜ |
| | **Total** | **~14–17 weeks** | |

> Phase 7.0 (Firebase AI Logic migration) can be pulled forward into Phase 6 — it is the
> security fix for the embedded Gemini key. Phases 7.1/7.2 depend on Phases 1–3 data.
