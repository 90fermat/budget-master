# Privacy Policy — Budget Master

**Last updated: 22 July 2026**

> **Draft for review.** This describes what the app does today, derived from the code, not from
> intent. Two things before it is published: it needs a real contact address (marked
> `[CONTACT EMAIL]` below), and if you are subject to GDPR/CCPA you should have someone qualified
> read it. Everything factual in here is checkable against the source.

## The short version

Everything you enter lives on your device. **If you sign in, it is also copied to your own private
area of Google Firestore**, so your devices show the same figures — that is what signing in is for.
Nobody but you can read it. There is no analytics on how you spend money, and nothing is sold or
shared with advertisers.

Signed out, nothing financial leaves the device at all.

## What is stored on your device

Everything you enter — accounts, transactions, budgets, goals, categories, and your settings — is
stored **locally on your device** in an app-private database. The app works fully offline; the
local copy is the one it reads and writes, and syncing is something that happens afterwards.
Uninstalling the app deletes the local copy.

## What is stored in the cloud, if you sign in

Signing in turns on sync. From then on your **accounts, transactions, budgets, savings goals, your
own categories, recurring entries, and your profile row** are copied to Firestore under your user
id, and pulled back down on your other devices.

Two things do **not** go up, deliberately:

- **The record of which SMS messages were read.** Cross-device duplicate detection already works
  without it, so uploading a log of who texted you and when would buy nothing.
- **In-app notifications, cached exchange rates and AI insight caches** — device-local or
  regenerable.

**Who can read it.** Only you. Security rules restrict every document to the account that owns it,
checked on Google's servers rather than in the app, and they are tested rather than assumed. The
data is not encrypted with a key only you hold, however: Google encrypts it at rest and in transit,
and Google — like any cloud provider — could technically access it, as could anyone who obtains
your account credentials. **Use a strong password, and the app lock.**

**Deleting.** Removing something on one device removes it everywhere, and deleting your account
erases the cloud copy along with the local one.

**If you never sign in**, none of this applies: nothing financial is uploaded, and if you lose the
device that data is gone, because there is no copy on our side to restore from. The encrypted
backup file in Settings exists for exactly that case.

## What leaves your device

### 1. Account sign-in (Firebase Authentication)

If you create an account, your **email address** is handled by Google Firebase Authentication,
which stores it in order to sign you in. Your password is never seen or stored by the app — it
goes directly to Firebase. If you sign in with Google, Google tells us your email address and
display name.

Signing in scopes your data to your account and, as described above, syncs it to your private area
of Firestore.

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

### 3. A device identifier, used only to order edits

Each install generates a **random identifier** the first time it syncs, and attaches it to the rows
it uploads. It exists to settle one question: when two devices change the same thing in the same
instant, both must agree on which change wins, or they drift apart permanently.

It contains nothing about your phone — no hardware id, no advertising id, nothing that could
identify you or the device to anyone else. It is generated randomly, it never leaves your own data,
and it is not used for analytics or tracking. It also stays out of backup files, so restoring a
backup onto a second phone does not give both the same identity.

### 4. Crash reports (Firebase Crashlytics)

Release builds send **crash reports** — stack traces plus device model, OS version, and app
version — so that crashes can be found and fixed. Crash reports contain no transactions, amounts,
or email addresses. Crash reporting is disabled in development builds.

### 5. Exchange rates

To convert between currencies, the app requests published exchange rates from
[ExchangeRate-API](https://www.exchangerate-api.com). This request contains **only a currency
code** (for example, "USD"). None of your data is sent. Rates by Exchange Rate API.

## What we do not do

- We do not sell or share your data with advertisers or data brokers.
- We do not track your location.
- We do not read your contacts, photos, or bank accounts.
- We do not read your SMS **unless you switch on mobile-money import** and grant the permission.
  Messages are then read on your phone and parsed there; the message text is never uploaded.
- We do not look at your transactions. If you sync, they are stored in your own area of Google's
  Firestore, reachable only by your account — but we will not claim they "never leave your device",
  because once you sign in, they do.

## Third parties

| Service | What it receives | Why |
|---|---|---|
| Firebase Authentication | Email address (and display name, if you use Google sign-in) | To sign you in |
| Cloud Firestore | Your accounts, transactions, budgets, goals, own categories and recurring entries — only once you sign in | To keep your devices showing the same figures |
| Firebase AI Logic → Gemini | Aggregated spending totals — only if you enable AI insights | To generate insights |
| Firebase Crashlytics | Crash stack traces, device model, OS version | To fix crashes |
| Firebase App Check | Device attestation token | To stop abuse of the AI and sync endpoints |
| ExchangeRate-API | A currency code | To convert currencies |

Firebase is operated by Google. See the
[Google Privacy Policy](https://policies.google.com/privacy).

## Your choices and rights

- **Turn AI off:** Settings → AI insights. It is off unless you turn it on.
- **Delete your data:** uninstall the app. Everything local is removed with it.
- **Delete your account:** contact `[CONTACT EMAIL]` and your Firebase Authentication record
  (your email address) will be deleted. Your financial data is not held by us, so there is
  nothing else to delete on our side.
- **Access your data:** it is on your device. Reports → Export CSV produces a copy, and
  Settings → Backup produces a complete encrypted file.
- **Stop syncing:** sign out. The device keeps its local copy and stops sending or receiving.
- **Delete the cloud copy:** deleting your account removes it along with everything local.

## Children

Budget Master is not directed at children under 13, and we do not knowingly collect data from
them.

## Changes

If this policy changes, the date at the top changes with it.

## Contact

`[CONTACT EMAIL]`
