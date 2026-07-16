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
| **Accounts** | ✅ models, repo, 9 use cases | ✅ SqlDelight, **live balance** = opening + own transactions; transfers, reconcile, FX conversion | ✅ Full MVI | ✅ Wallet list, net worth, global switcher, CRUD/archive, transfer, reconcile | ✅ repo (9) | **~90%** |
| **Reports** | ✅ models, repo, 2 use cases | ✅ SqlDelight, wallet-scoped, transfers excluded | ✅ Full MVI | ✅ Totals + period comparison, category donut, trend chart, CSV export | ✅ repo (4) | **~85% (was ~15%)** |
| **Recurring** | ✅ models, repo, 5 use cases | ✅ SqlDelight, calendar-correct, idempotent catch-up | ➖ (engine) | ⚠️ no management screen yet | ✅ repo (5) | **~70% (new)** |

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
6. **Hardcoded colors — partially open.** An earlier entry claimed these were gone from
   features; that was inaccurate. The dashboard's semantic colors now use
   `MaterialTheme.financialColors`, and the Konsist rule enforces the rest, but four files
   are explicitly allow-listed with reasons: the two `ColorHex` parsers (literal fallback for
   malformed stored hex), `ReportsScreen` (Phase 3 rewrite), `TopTransactionsList` +
   `AiInsightsWidget` (category/accent palettes pending the shared component library,
   Phase 4). Shrinking that allowlist is Phase 4 work.

7. **Security (open)**: Gemini is still called directly from the client with an embedded
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
- [ ] **Rate source**: rates are read from the local cache; nothing populates it yet. A
  fetcher (free FX endpoint, refreshed daily) is needed before multi-currency totals are
  meaningful in the wild — tracked with Phase 6 networking/hardening.

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
- [ ] **Verify on a real device** once the console setup above is done (not verifiable from
  the current Windows host / without the SHA-1 registered).

### Status: Phase 3 is the next phase

Every deferred item from Phases 1, 1.5, 2, 2.5, and 2.6 is closed, and the architecture
drift list is resolved or explicitly scoped. **Three items remain open and cannot be closed
from a Windows host — they are not Phase 3 blockers:**

1. **iOS Firebase init** — add the Firebase iOS SDK (SPM/Pods) and call
   `FirebaseApp.configure()` in `iOSApp.init()` before `initKoinIos()`. Needs macOS/Xcode.
2. **iOS Google sign-in** — add `GIDSignIn` + the reversed-client-id URL scheme, then flip
   `isGoogleSignInSupported` to true on iOS. Needs macOS/Xcode.
3. **On-device verification of Google sign-in** — the SHA-1 is registered and the code is in
   place, but the flow has not been exercised on real hardware.

Two smaller follow-ups are parked in their natural phases: an **FX rate fetcher** (Phase 6
networking) and **OS-level notification delivery** (rides with the Phase 3 recurring engine).

### Phase 3 — Reports & recurring engine (1.5 weeks)
- [x] **Reports on real data** (was a static mockup): `ReportsRepository` +
  `SqlDelightReportsRepository` scoped to the signed-in user and the **active wallet**, with
  transfers/adjustments excluded — matching the dashboard and budget rules. Period totals
  (30/90/365/all) with a **comparison against the preceding period of the same length**,
  spending **by category** with shares, and a daily **income-vs-expense trend**.
  `ReportsViewModel` (MVI); the range drives a `flatMapLatest` re-query, and the report also
  re-emits when the wallet or currency changes. Category ring + trend chart **side-by-side
  ≥600dp**, stacked on phones.
- [x] **Charts**: drawn with Compose `Canvas`. *Deviation from the original plan* (Vico on
  Android/iOS + a Wasm fallback): one implementation renders identically on all three targets
  with no per-platform divergence to maintain, and it honours reduced motion. Vico stays
  available if a future chart needs it.
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
- [ ] **Deferred — WorkManager job on Android**: today the engine catches up on app open,
  which is correct but means entries appear when you next launch rather than on the day. A
  periodic background job would close that gap (Android-only; iOS/Web keep open-time
  catch-up). Not a correctness issue — the dates on the created entries are always right.
- [ ] **Deferred — recurring management UI**: schedules can be created/paused/deleted through
  the repository, but there is no screen yet; the transaction editor's "repeats" toggle only
  flags an entry. Natural home is a section on Transactions (Phase 4 polish).
- [x] **Chart a11y**: a canvas is opaque to screen readers, so each chart carries a generated
  `contentDescription` summarising the data (top categories with shares; income/expense
  totals over N days), and the legend repeats the numbers as real text.

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

### Phase 4.5 — In-app guidance: explain every screen (1 week)

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

- [ ] **`:core` `guidance` package** (content is strings, and strings already live in `:core`,
  so no feature→feature edge): `GuidanceKey` per screen (dashboard, transactions, budgets,
  goals, accounts, reports, settings); `ScreenGuide` = title + intro + `List<FeatureNote>`
  (icon, title, body); a `GuidanceRegistry` mapping key → guide so Settings can enumerate all
  guides without touching feature modules.
- [ ] **`GuidancePreferences`** over `KeyValueStore`, mirroring `OnboardingPreferences`:
  per-screen "seen" flags + a global **"Show tips"** toggle. First visit auto-opens once;
  never again unless replayed.
- [ ] **Shared UI**: `GuidanceSheet` (bottom sheet on phone, dialog ≥600dp — the same
  `AdaptiveContainer` pattern the editors use) and a `HelpIconButton` for screen headers.
- [ ] **Wire all 7 screens**, each note written against what the screen actually does
  (including the non-obvious ones listed above).
- [ ] **Settings → "Help & tips"**: browse every guide on demand + **"Reset tips"**, next to
  the existing "Replay intro" row.
- [ ] **Localized EN/FR** as authored (not retrofitted), and included in Phase 5's
  pseudo-locale/truncation audit — guide bodies are the longest copy in the app.
- [ ] **A11y + motion**: notes are real text (screen-reader friendly, no image-only content);
  honours `isReducedMotionEnabled()`; every `?` has a content description.
- [ ] Tests: registry covers every `GuidanceKey`; auto-show fires once then stops; "Reset
  tips" restores it; ViewModel tests for the toggle.

**Deliverable:** every screen answers "what can I do here?" without leaving the app; tips
appear once, never nag, and can be replayed from Settings; fully EN/FR.
**Placement:** after Phase 4 so the UI is settled (explanations of a moving target rot), and
before Phase 5 so the localization audit covers this copy. The guides also make good
Phase 8 screenshots.

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
| 2.5 | Accounts foundation / multi-wallet (uid binding, switcher, net worth, transfers, reconcile, FX) | 1 wk | ✅ done |
| 2.6 | Google sign-in (Android) | 0.5 wk | ✅ done — SHA-1 registered; needs on-device verification |
| — | Deferred-item sweep (Phases 1 / 1.5 / 2 / 2.5 + architecture drift) | — | ✅ done |
| 3 | Reports + recurring engine | 1.5 wk | ✅ done (WorkManager job + recurring UI deferred to 4) |
| 4 | Motion & premium polish | 1–1.5 wk | ⬜ |
| 4.5 | In-app guidance — every screen explains its features | 1 wk | ⬜ |
| 5 | Localization | 0.5–1 wk | ⬜ |
| 6 | Hardening & release | 1.5–2 wk | ⬜ |
| 7 | AI intelligence layer (free-tier Gemini via Firebase) | 2–2.5 wk | ⬜ |
| 8 | Store polish & README screenshots | 0.5 wk | ⬜ |
| | **Total** | **~14–17 weeks** | |

> Phase 7.0 (Firebase AI Logic migration) can be pulled forward into Phase 6 — it is the
> security fix for the embedded Gemini key. Phases 7.1/7.2 depend on Phases 1–3 data.
