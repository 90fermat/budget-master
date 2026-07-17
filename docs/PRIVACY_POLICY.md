# Privacy Policy — Budget Master

**Last updated: 17 July 2026**

> **Draft for review.** This describes what the app does today, derived from the code, not from
> intent. Two things before it is published: it needs a real contact address (marked
> `[CONTACT EMAIL]` below), and if you are subject to GDPR/CCPA you should have someone qualified
> read it. Everything factual in here is checkable against the source.

## The short version

Your financial data stays on your device. We do not have a server that stores your transactions,
budgets, goals, or account balances. There is no analytics on how you spend money, and nothing is
sold or shared with advertisers.

## What is stored on your device

Everything you enter — accounts, transactions, budgets, goals, categories, and your settings —
is stored **locally on your device** in an app-private database. It is not uploaded, backed up to
us, or synced between your devices. Uninstalling the app deletes it.

This also means: **if you lose your device, that data is gone.** There is no copy on our side to
restore from.

## What leaves your device

### 1. Account sign-in (Firebase Authentication)

If you create an account, your **email address** is handled by Google Firebase Authentication,
which stores it in order to sign you in. Your password is never seen or stored by the app — it
goes directly to Firebase. If you sign in with Google, Google tells us your email address and
display name.

Signing in scopes your data to your account **on that device**. It does not upload it.

### 2. AI insights — only if you turn them on

AI insights are **off by default**. Nothing is sent to any AI provider until you enable them in
**Settings → AI insights**.

When enabled, the app sends a **summary** of your spending to Google's Gemini, through Firebase AI
Logic, to generate insights. Specifically, it sends:

- your total income and total expenses for the last 30 days,
- the number of transactions,
- per-category totals (for example, "Groceries: 240.50"),
- the language you want the answer in.

It does **not** send transaction descriptions, merchant names, dates, transaction ids, account
names, balances, your email, or any identifier. Descriptions are excluded deliberately: they are
free text and people put names and private notes in them.

Insights are AI-generated and are **not financial advice**.

You can turn this off at any time in Settings; when off, nothing is sent.

### 3. Crash reports (Firebase Crashlytics)

Release builds send **crash reports** — stack traces plus device model, OS version, and app
version — so that crashes can be found and fixed. Crash reports contain no transactions, amounts,
or email addresses. Crash reporting is disabled in development builds.

### 4. Exchange rates

To convert between currencies, the app requests published exchange rates from
[ExchangeRate-API](https://www.exchangerate-api.com). This request contains **only a currency
code** (for example, "USD"). None of your data is sent. Rates by Exchange Rate API.

## What we do not do

- We do not sell or share your data with advertisers or data brokers.
- We do not track your location.
- We do not read your SMS, contacts, photos, or bank accounts.
- We do not have access to your transactions — they never leave your device.

## Third parties

| Service | What it receives | Why |
|---|---|---|
| Firebase Authentication | Email address (and display name, if you use Google sign-in) | To sign you in |
| Firebase AI Logic → Gemini | Aggregated spending totals — only if you enable AI insights | To generate insights |
| Firebase Crashlytics | Crash stack traces, device model, OS version | To fix crashes |
| Firebase App Check | Device attestation token | To stop abuse of the AI endpoint |
| ExchangeRate-API | A currency code | To convert currencies |

Firebase is operated by Google. See the
[Google Privacy Policy](https://policies.google.com/privacy).

## Your choices and rights

- **Turn AI off:** Settings → AI insights. It is off unless you turn it on.
- **Delete your data:** uninstall the app. Everything local is removed with it.
- **Delete your account:** contact `[CONTACT EMAIL]` and your Firebase Authentication record
  (your email address) will be deleted. Your financial data is not held by us, so there is
  nothing else to delete on our side.
- **Access your data:** it is on your device. Reports → Export CSV produces a copy.

## Children

Budget Master is not directed at children under 13, and we do not knowingly collect data from
them.

## Changes

If this policy changes, the date at the top changes with it.

## Contact

`[CONTACT EMAIL]`
