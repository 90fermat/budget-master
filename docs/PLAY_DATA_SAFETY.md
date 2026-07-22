# Play Console — Data safety answers

**Draft for review. Derived from the code, current as of 22 July 2026.**

Google requires this form to match what the app actually does; a wrong answer here is how apps
get pulled. Every answer below has the reason next to it, so you can re-check it rather than
trust it. **Re-check this whenever a Firebase SDK is added or the AI prompt changes** — those are
the two things that would make it wrong.

## Does your app collect or share any of the required user data types?

**Yes** — email address, crash logs, **the user's financial records once they sign in**, and
(opt-in only) aggregated financial summaries sent to an AI model.

> **This answer changed on 22 July 2026 and the change is material.** The app previously held every
> transaction on the device and nothing else. It now syncs the user's records to Cloud Firestore
> when they are signed in. Financial info must therefore be declared as **collected**, which it
> previously was not. Submitting the old answer against this build would be a false declaration.

---

## Data types

### Personal info → Email address
- **Collected:** Yes
- **Shared:** No
- **Processed ephemerally:** No
- **Required or optional:** Required (only if the user creates an account)
- **Purpose:** Account management
- **Why:** Firebase Authentication stores the email to sign the user in.

### Financial info → Purchase history / other financial info
- **Collected:** **Yes**
- **Shared:** No — stored in Google Firestore as the app's own backend processor, not disclosed to
  any other party.
- **Processed ephemerally:** No — it is stored so a second device can read it.
- **Required or optional:** Optional. Signed out, nothing financial leaves the device; signing in
  is what turns sync on.
- **Purpose:** App functionality (keeping the user's devices consistent).
- **Why:** Once signed in, accounts, transactions, budgets, savings goals, user-created categories
  and recurring entries are written to `users/{uid}/…` in Cloud Firestore and read back on the
  user's other devices. Security rules restrict every document to the owning account.
- **Not uploaded:** the record of which SMS messages were read, in-app notifications, cached
  exchange rates, AI insight caches.
- **Deletion:** account deletion erases the cloud copy along with the local database.

### Financial info → the AI nuance (separate from the above)
- **Nuance:** if the user turns on AI insights (**off by default**), *aggregated totals* —
  income/expense sums and per-category totals for 30 days — are sent to Google's Gemini via
  Firebase AI Logic to generate text. No individual transactions, descriptions, dates, or
  amounts tied to a merchant are sent.
  - Google's guidance treats this as **data collection for the "App functionality" purpose**, so
    if the form offers "Other financial info", declare it as:
    - **Collected:** Yes
    - **Shared:** Yes (with Google as the model provider)
    - **Optional:** Yes — user must opt in
    - **Purpose:** App functionality
  - Do **not** claim "processed ephemerally" unless you have confirmed Gemini's retention for
    the Firebase AI Logic tier you are on. Leave it unchecked if unsure.

### App activity → Crash logs
- **Collected:** Yes
- **Shared:** No
- **Required or optional:** Required
- **Purpose:** Crash reporting / diagnostics
- **Why:** Firebase Crashlytics is enabled in release builds. Stack traces and device metadata
  only; disabled in debug builds.

### Device or other IDs
- **Collected:** Yes
- **Shared:** No
- **Purpose:** Fraud prevention / app functionality
- **Why:** Firebase App Check issues a device attestation token so the AI endpoint cannot be
  abused. Firebase Analytics also generates an app instance id.

### Location, Contacts, Photos, Messages, Health, Calendar
- **Collected:** No. The app requests none of these permissions.

---

## Security practices

- **Is data encrypted in transit?** Yes — everything that leaves the device goes over HTTPS.
- **Can users request data deletion?** Yes — uninstalling deletes all local data, and Settings →
  Delete account erases the Firestore copy along with it. Uninstalling alone no longer removes
  everything once the user has signed in, which is a change from the previous answer.
- **Committed to the Play Families policy?** Not applicable — not directed at children.
- **Independent security review?** No.

---

## Answers that need a human

1. **Contact email** for the privacy policy and deletion requests — not decided yet.
2. **Gemini retention** for your Firebase AI Logic tier decides the "processed ephemerally" box.
   Check the terms for the tier before ticking it.
3. **Account deletion route.** Play requires an app-accessible or web route to request deletion.
   The app has no "delete my account" button today — either add one, or publish a web form and
   link it here.
