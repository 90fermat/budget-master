# BudgetMaster Screen Layout & Wireframe Specifications

This document defines the wireframes, component layouts, and MVI specifications for the 9 core screens of **BudgetMaster**. All screens are designed using **Material 3 Expressive** components, support dark mode, and adapt responsively across Phone, Tablet, and Desktop/Web.

---

## Navigation & Architecture Layout Overview

BudgetMaster implements an **Adaptive Navigation Pattern**:
* **Phone (`< 600dp`)**: Bottom Navigation Bar.
* **Tablet (`600dp` to `1240dp`)**: Permanent Left Navigation Rail.
* **Desktop / Web (`>= 1240dp`)**: Left Navigation Drawer (expanded) + main content container maxed at `1200dp`.

---

## 1. Splash Screen

The entrance of the application. Handles brand expression, checking user credentials, and routing.

### Wireframe Layout
```text
+-----------------------------------+
|                                   |
|                                   |
|             [ LOGO ]              |
|           BudgetMaster            |
|       Precision meets Delight     |
|                                   |
|                                   |
|                (O)                |
+-----------------------------------+
```

### UI Components
* **Brand Logo**: Vector graphic representing a layered coin shield. Animates on entrance (scale & fade).
* **Application Title**: Text style `headlineLarge` with Outfit font.
* **Tagline**: Text style `bodyMedium` with subtle opacity.
* **Progress Indicator**: Circular M3 indeterminate indicator `(O)` centered at the bottom, appearing only if initialization exceeds 1.5 seconds.

### MVI Specifications
* **State**: `SplashState { isLoading: Boolean, destination: NavigationTarget? }`
* **Intents**: `InitializeApp` (Triggers user session verification, biometric status validation, and local DB loading).
* **Effects**: `NavigateTo(Onboarding | Login | Dashboard)`

---

## 2. Onboarding Screen

Introduces the user to key concepts: budgeting, automated tracking, and AI-powered insights.

### Wireframe Layout
```text
+-----------------------------------+
|  [ Skip ]                         |
|                                   |
|          +-------------+          |
|          |             |          |
|          | ILLUSTRATION|          |
|          |             |          |
|          +-------------+          |
|                                   |
|       Track Money Effortless      |
|    Sync bank accounts and categorize   |
|                                   |
|             o  *  o               |
|                                   |
|      [      GET STARTED      ]    |
+-----------------------------------+
```

### UI Components
* **Illustration Canvas**: Centered image area displaying modern, abstract fintech vectors.
* **Title Text**: Bold Outfit text (`headlineMedium`) describing the slide.
* **Body Text**: Descriptive Inter text (`bodyMedium`) describing benefits.
* **Page Indicator**: Row of 3 dots (`o * o`) using standard indicator dots. Active page transitions dynamically scale to double width.
* **Get Started / Next Button**: Filled M3 Button with `RoundedCornerShape(16.dp)`. Scales down slightly on press.
* **Skip Button**: Outlined text button in the top-right corner to bypass intro.

### MVI Specifications
* **State**: `OnboardingState { currentSlideIndex: Int }`
* **Intents**: `NextSlide`, `SkipOnboarding`
* **Effects**: `NavigateTo(Login)`

---

## 3. Login Screen

Entry portal containing credential input, biometric activation, and validation errors.

### Wireframe Layout
```text
+-----------------------------------+
|  [<-]                             |
|                                   |
|  Welcome back                      |
|  Enter credentials to sign in      |
|                                   |
|  +-----------------------------+  |
|  | Email Address               |  |
|  +-----------------------------+  |
|                                   |
|  +-----------------------------+  |
|  | Password                 [o] |  |
|  +-----------------------------+  |
|                                   |
|                     Forgot?       |
|                                   |
|  [          SIGN IN            ]  |
|                                   |
|             [ Face ID ]           |
|                                   |
|  Don't have an account? Sign Up   |
+-----------------------------------+
```

### UI Components
* **Back Button**: Navigation arrow icon in top-left (for users who navigated from onboarding manually).
* **Email Input Field**: M3 Outlined TextField with custom trailing clear icon.
* **Password Input Field**: M3 Outlined TextField with trailing eye toggle icon to show/hide text characters.
* **Forgot Password Link**: Plain text button aligned to the right.
* **Sign In Button**: Primary filled button. Triggers checking spinner when logging in.
* **Biometric Shortcut**: Large circular icon button placed below the sign-in button. Initiates system-native prompt (FaceID or Fingerprint).
* **Sign Up Toggle Link**: Small text action linking to the Register flow.

### MVI Specifications
* **State**: `LoginState { email: String, password: String, isLoggingIn: Boolean, errorMsg: String? }`
* **Intents**: `UpdateEmail(String)`, `UpdatePassword(String)`, `SubmitLogin`, `TriggerBiometricAuth`
* **Effects**: `NavigateTo(Dashboard)`, `ShowErrorSnackbar(String)`

---

## 4. Dashboard Screen

The central command of the application. Displays balances, active cash flow charts, and recommendations.

### Wireframe Layout
```text
+-----------------------------------+
|  [Profile]  BudgetMaster    [AI]  |
|                                   |
|  +-----------------------------+  |
|  | TOTAL BALANCE               |  |
|  | $12,450.80                  |  |
|  | +2.4% this month            |  |
|  +-----------------------------+  |
|                                   |
|  [ Add Expense ]   [ Scan Receipt ]|
|                                   |
|  Cash Flow                        |
|  +-----------------------------+  |
|  |  /\_                        |  |
|  | /    \_                     |  |
|  +-----------------------------+  |
|                                   |
|  Recent Transactions              |
|  - Starbucks            -$4.50    |
|  - Employer Paycheck +$2,500.00   |
+-----------------------------------+
```

### UI Components
* **App Bar Header**: Contains user avatar thumbnail on left and an "AI Insights" quick icon on right.
* **Balance Card**: Gradient glassmorphic card component (as defined in `DESIGN_SYSTEM.md`) displaying cash figures.
* **Quick Actions Row**: Horizontal linear layout containing prominent card action buttons: "Add Expense", "Scan Receipt", and "Transfer".
* **Cash Flow Sparkline**: Graph representation (Vico line graph) plotting cumulative income vs expense curves over the active month.
* **Recent Transactions List**: Stack of the top 3 transaction rows. Tap opens the transaction's detail.

### MVI & Responsive Rules
* **Phone layout**: Vertical scroll stack of all components.
* **Tablet layout**: Left Navigation Rail + 2 Column layout (Col 1: Balance card, Quick Actions, Sparkline. Col 2: Recent transactions, AI insights card).
* **Desktop layout**: Nav Drawer + 3 Column grid (Col 1: Accounts & Quick Actions, Col 2: Interactive Charts, Col 3: Real-time logs and AI feed).
* **State**: `DashboardState { balance: Double, flowPoints: List<Point>, recentTrans: List<Transaction>, isLoading: Boolean }`
* **Intents**: `RefreshDashboard`, `TriggerQuickAction(ActionType)`

---

## 5. Transactions Screen

A paginated transaction log with comprehensive filters, search, and action targets.

### Wireframe Layout
```text
+-----------------------------------+
|  +-----------------------------+  |
|  | Search transactions...   [o] |  |
|  +-----------------------------+  |
|                                   |
|  [All]  [Food]  [Bills]  [Travel] |
|                                   |
|  Today                            |
|  - Starbucks            -$4.50    |
|  - Chevron Gas Station -$45.00    |
|                                   |
|  Yesterday                        |
|  - Supermarket Group   -$128.40   |
|                                   |
|  +-----------------------------+  |
|  | [ + ] Quick Add Transaction |  |
+-----------------------------------+
```

### UI Components
* **Search Field**: Top-anchored Search Bar with inline text field and filters toggle action.
* **Category Filter Chips**: Scrollable row of filter chips.
* **Transaction Groups**: Chronological listing (LazyColumn) grouped by headers (e.g., "Today", "Yesterday", "Last Week").
* **Receipt Scanner FAB**: Dynamic bottom-right Floating Action Button containing a camera scan icon.
* **Quick Add Button**: Compact banner button at the bottom of the list for quick manual inputs.

### MVI & Responsive Rules
* **Phone layout**: Single pane list layout.
* **Tablet/Desktop layout**: List-Detail interface. Selecting a transaction from the list on the left displays the transaction details, notes, receipts, and edit forms on the right pane without opening a new screen.
* **State**: `TransactionsState { searchQuery: String, transactions: List<Transaction>, selectedCategory: Category?, isLoading: Boolean }`
* **Intents**: `Search(String)`, `FilterByCategory(Category?)`, `DeleteTransaction(String)`

---

## 6. Budgets Screen

Category-specific budget progress indicators tracking limits and overages.

### Wireframe Layout
```text
+-----------------------------------+
|  Active Budget: June 2026         |
|                                   |
|  +-----------------------------+  |
|  | Food & Dining               |  |
|  | [=========>      ] 45%      |  |
|  | Spent $450 of $1,000        |  |
|  +-----------------------------+  |
|                                   |
|  +-----------------------------+  |
|  | Housing & Rent              |  |
|  | [============] 100%         |  |
|  | Spent $1,500 of $1,500      |  |
|  +-----------------------------+  |
|                                   |
|  +-----------------------------+  |
|  | Entertainment               |  |
|  | [=============>!] OVERLIMIT |  |
|  | Spent $320 of $200          |  |
|  +-----------------------------+  |
+-----------------------------------+
```

### UI Components
* **Budget Header**: Displays month switcher and a progress ring of total global budget consumption.
* **Budget Progress Cards**: List of card structures. Each contains the category name, spend ratio text, and progress bar with dynamic colors (Emerald for safe, Amber for warning >85%, Coral Red for overlimit >=100%).
* **Rule indicator (50/30/20)**: Small dashboard indicator comparing actual spending against the 50/30/20 rule (Needs, Wants, Savings).

### MVI Specifications
* **State**: `BudgetsState { activeMonth: Date, budgets: List<Budget>, spentSummary: SpentSummary }`
* **Intents**: `ChangeActiveMonth(Date)`, `CreateBudget(Category, Double)`, `DeleteBudget(String)`
* **Effects**: `ShowOverbudgetAlert(CategoryName)`

---

## 7. Goals Screen

Savings objectives that visually track current status, targets, and dates.

### Wireframe Layout
```text
+-----------------------------------+
|  Savings Goals                    |
|                                   |
|  +-----------------------------+  |
|  | New Tesla Model 3           |  |
|  | Target: $45,000 (Dec 2027)  |  |
|  | [=====>         ] 30%       |  |
|  | Saved $13,500               |  |
|  +-----------------------------+  |
|                                   |
|  +-----------------------------+  |
|  | Emergency Fund              |  |
|  | Target: $10,000 (Dec 2026)  |  |
|  | [=============> ] 90%       |  |
|  | Saved $9,000                |  |
|  +-----------------------------+  |
+-----------------------------------+
```

### UI Components
* **Goals Grid**: List or Grid container containing active savings milestones.
* **Goal Progress card**: Card tracking description, target amount, current amount saved, target date, and projection timeline text (e.g., "On track to reach 2 months early").
* **New Goal Action Button**: Floating or header action button to launch the "Create Savings Goal" bottom sheet details.

### MVI & Responsive Rules
* **Phone layout**: 1-column list of goals.
* **Tablet layout**: 2-column grid of goals.
* **Desktop/Web layout**: 3-column or 4-column cards grid layout.
* **State**: `GoalsState { goals: List<SavingsGoal>, isSubmittingGoal: Boolean }`
* **Intents**: `CreateSavingsGoal(Name, Target, Date)`, `AddContribution(GoalId, Double)`

---

## 8. Reports Screen

Analytical screen displaying monthly charts, heatmaps, and data exports.

### Wireframe Layout
```text
+-----------------------------------+
|  Reports: June 2026        [Export]|
|  [ Weekly ]   [ Monthly ]  [ Annual]
|                                   |
|       Category Breakdown          |
|            +-----+                |
|          /         \              |
|         |    (o)    |             |
|          \         /              |
|            +-----+                |
|                                   |
|  - Rent (40%)           $1,500.00 |
|  - Food (20%)             $750.00 |
|  - Shopping (10%)         $375.00 |
+-----------------------------------+
```

### UI Components
* **Export Button**: Top-right app bar action button to generate PDF/CSV files.
* **Time Scale Tabs**: Segmented tab control representing: "Weekly", "Monthly", "Annual".
* **Category Donut Chart**: Main analytics view (Ring chart) with color-coded category segments. Selecting a slice details category transactions list below.
* **Metrics List**: Vertical listing mapping categories to percentages, total sums, and historical comparisons.

### MVI Specifications
* **State**: `ReportsState { selectedRange: TimeRange, breakdown: List<CategorySpend>, totalSpent: Double }`
* **Intents**: `ChangeTimeRange(TimeRange)`, `ExportData(FormatType)`
* **Effects**: `DownloadFinished(FileUri)`

---

## 9. Settings Screen

User profile details, configuration, connectivity, and customizations.

### Wireframe Layout
```text
+-----------------------------------+
|  Settings                         |
|                                   |
|  Account & Connections            |
|  [o] User Profile                 |
|  [o] Linked Bank Accounts         |
|                                   |
|  Customization                    |
|  [o] Primary Currency       (USD) |
|  [x] Dynamic Colors         (ON)  |
|  [x] Dark Mode              (ON)  |
|                                   |
|  Security                         |
|  [x] Biometric Login        (ON)  |
|                                   |
|  [         SIGN OUT            ]  |
+-----------------------------------+
```

### UI Components
* **Profile Summary Header**: Display avatar, name, and email. Tap opens edit profile details.
* **Linked Accounts Button**: Setting row linking to bank integrations (Plaid/Open Banking).
* **Currency Selection Row**: Displays target default currency. Tap opens bottom sheet selection.
* **Switch Controls**: Clean M3 switches for toggling "Dynamic Colors", "Dark Mode", and "Biometric Login".
* **Sign Out Button**: Outlined button centered at the bottom of settings.

### MVI Specifications
* **State**: `SettingsState { profile: UserProfile, defaultCurrency: String, isDynamicColorEnabled: Boolean, isDarkModeEnabled: Boolean, isBiometricEnabled: Boolean }`
* **Intents**: `ToggleDynamicColor(Boolean)`, `ToggleDarkMode(Boolean)`, `ToggleBiometric(Boolean)`, `ChangeCurrency(String)`, `SignOut`
* **Effects**: `TriggerRestart`
