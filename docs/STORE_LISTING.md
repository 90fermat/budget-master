# Play Store listing copy — Budget Master

**Draft for review, 17 July 2026.**

Every claim here is one the app actually delivers today. That constraint is deliberate: the
README's feature list previously advertised receipt scanning, tags, heatmaps, PDF export and
automatic sync, none of which exist, and store listings are exactly where that kind of claim
becomes a refund request or a one-star review titled "where is the sync".

---

## App name (30 char max)

```
Budget Master
```

## Short description (80 char max)

```
Private budgeting. Your money data stays on your device — not on someone's server.
```
*79 characters. Privacy is the honest differentiator: the app genuinely has no backend for
financial data, which almost no competitor can say.*

## Full description (4000 char max)

```
Budget Master is a personal finance tracker built on a simple principle: your financial
data belongs on your device, not on someone else's server.

No account required to look around. No ads. No selling your data — there is nothing to
sell, because your transactions never leave your phone.


TRACK WHAT YOU ACTUALLY SPEND

• Log income and expenses in seconds
• Search, filter, and group by day
• Swipe to delete, with undo
• Set up recurring entries once — rent, salary, subscriptions — and they appear on the
  right day, every time


MULTIPLE WALLETS, ONE PICTURE

• Keep separate accounts: cash, bank, mobile money, credit
• Move money between them with proper transfers
• See your net worth across all of them, converted into your currency
• Reconcile an account when real life and the app disagree


BUDGETS THAT TELL THE TRUTH

• Set a monthly limit per category
• Progress is computed live from your actual transactions — never a stale stored number
• Clear warnings before you go over, not after


GOALS

• Save toward something specific
• Contribute whenever you like
• See a projected completion date based on what you actually put in


REPORTS

• Totals for 30, 90, 365 days or all time, compared against the period before
• Spending by category, with shares
• Daily income-vs-expense trend
• Export everything to CSV — it is your data


OPTIONAL AI INSIGHTS

Off by default. Turn it on and Budget Master sends a summary of your spending — category
totals only, never your transaction descriptions, names, or dates — to generate insights
about your habits. Turn it off and nothing is sent. AI-generated insights are not
financial advice.


BUILT PROPERLY

• Five colour themes, including Material You
• Full English and French
• Light and dark
• Works offline — because it was never online to begin with
• Respects your reduced-motion and font-size settings, and works with screen readers


HONEST ABOUT WHAT IT IS NOT

• No bank account linking
• No syncing between devices — your data is on this device only, so back up what matters
• No receipt scanning yet

Rates by Exchange Rate API.
```

## Category

Finance

## Tags

budget, expense tracker, personal finance, money manager, privacy, offline

## Screenshots needed (Phase 8)

Minimum 2, up to 8. Suggested order — lead with what is distinctive, not with a login screen:

1. Dashboard with balance, quick actions and chart
2. Transactions list, day-grouped
3. Budgets with gauges (one healthy, one over)
4. Accounts / net worth with the wallet switcher
5. Reports with the category ring
6. Settings showing the five palettes
7. Dark mode of the dashboard
8. A guide sheet open, showing the in-app explanations

Capture at Pixel 6 size in both light and dark. `LocalizationScreenshotTest` already renders the
dashboard in EN/FR if a localized listing is wanted later.

## Still needed from a human

- **Contact email** for the listing and the privacy policy.
- **Privacy policy URL** — Play requires a public URL; `docs/PRIVACY_POLICY.md` needs hosting
  (GitHub Pages is fine).
- **Feature graphic** (1024×500) and the final **512×512 icon**.
- **Account deletion route** — Play requires one for any app with accounts, and the app has no
  "delete my account" action today. This is a **blocker**, not a nicety.
