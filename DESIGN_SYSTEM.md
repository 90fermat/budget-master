# BudgetMaster Premium Fintech Design System

This design system is a comprehensive guide to building consistent, high-fidelity, and accessible user interfaces for **BudgetMaster** on Android, iOS, and Web (Kotlin/Wasm). 

It is designed to compete with premium personal finance apps such as **Revolut**, **YNAB**, and **Wallet**, aligning with **Material 3 Expressive** guidelines.

---

## 1. Visual Language & Brand Voice

Our brand voice is **Precision meets Delight**. A personal finance app must project high trust and reliability, but it should never feel dry. 
* **Expressive Shapes**: We use generous corner radii (up to `24.dp` for cards, `16.dp` for buttons) and dynamic elevation scales.
* **Glassmorphism**: Subtle translucent surfaces with fine border strokes are used for high-end dashboards, separating main balance accounts from list views.
* **Micro-interactions**: Every click, swipe, or balance change triggers immediate, fluid visual feedback to encourage financial logging habits.

---

## 2. Color Palette (Material 3 Expressive)

BudgetMaster uses a dual-palette architecture. It supports native **Dynamic Color** (Material You) extraction on Android 12+ while falling back to a custom, curated fintech palette on iOS and Web.

### Custom Palette Tokens

| Tone | Light Theme Hex | Dark Theme Hex | Purpose |
| :--- | :--- | :--- | :--- |
| **Primary** (Trust) | `#4F46E5` (Indigo) | `#6366F1` (Indigo Neon) | Core brand actions, selections, and primary active states. |
| **Secondary** (Inflow) | `#059669` (Emerald) | `#10B981` (Mint) | Cash inflow, positive balances, growth indicators. |
| **Tertiary** (Growth/Goals)| `#7C3AED` (Amethyst) | `#8B5CF6` (Bright Violet) | Saving goals, investments, future projections. |
| **Error** (Outflow/Alert)| `#DC2626` (Crimson) | `#F87171` (Coral Red) | Cash outflow, over-budget warnings, negative trends. |
| **Background** | `#F8FAFC` (Slate White) | `#0B0E14` (Deep Obsidian) | Full-screen backdrops. |
| **Surface** | `#FFFFFF` (Pure White) | `#131924` (Sleek Charcoal) | Cards, bottom sheets, list items. |
| **Outline** | `#E2E8F0` (Light Slate) | `#1F293D` (Steel Gray) | Borders, divider lines, and component boundaries. |

### Color Palettes for Financial Analytics & Charts
To maintain accessibility, chart palettes are distinct and optimized for color blindness:
* **Income/Inflow Chart Line**: `#10B981` (Emerald) / `#34D399` (Mint)
* **Expenses/Outflow Chart Line**: `#EF4444` (Coral) / `#F87171` (Light Red)
* **Category Distribution (Ring Chart)**:
  - Food & Dining: `#F59E0B` (Amber)
  - Housing & Bills: `#3B82F6` (Blue)
  - Shopping: `#EC4899` (Pink)
  - Travel: `#14B8A6` (Teal)
  - Entertainment: `#8B5CF6` (Purple)

---

## 3. Typography System

We pair **Outfit** (a geometric, friendly typeface for headlines) with **Inter** (highly readable for UI text) and force **Tabular Figures** (monospace numbers) for all financial balances.

> [!IMPORTANT]
> **Tabular Figures (`tnum`)** are mandatory for currency values to prevent UI layout shifts or text "wobble" during count-up transitions or numeric changes.

### Typography Scales (Compose Multiplatform)

```kotlin
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.fontFeatureSettings
import androidx.compose.ui.unit.sp

val Typography = Typography(
    // Balances & Large Amounts (Always Tabular Figures)
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, // Configured to "Outfit" in platform hosts
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        fontFeatureSettings = "tnum"
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontFeatureSettings = "tnum"
    ),
    // Core Screen Headers
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    // Section Headers & Card Titles
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    // List Content, Body Texts (Inter)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif, // Configured to "Inter" in platform hosts
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Transaction Amounts in Lists (Tabular)
    bodyMediumNumeric = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "tnum"
    ),
    // Badges, Tiny Captions
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)
```

---

## 4. Spacing & Keyline System

BudgetMaster utilizes a strict **8dp grid** for layout composition and a **4dp sub-grid** for component internal micro-spacing (e.g., padding inside pills and badges).

### Grid Tokens
* `Spacing.micro` = `4.dp` (Internal badge padding, label-to-icon spacing)
* `Spacing.small` = `8.dp` (List item elements, title-to-subtitle spacing)
* `Spacing.medium` = `16.dp` (Card margins, button padding, list item gaps)
* `Spacing.large` = `24.dp` (Screen padding on mobile, layout breaks)
* `Spacing.huge` = `32.dp` (Top balances vertical offset, desktop column grids)

### Adaptive Margin Rules
To ensure the layouts look gorgeous across all screens, we adjust outer screen boundaries dynamically:
* **Mobile (Width < 600dp)**: Outer margins are exactly `16.dp`.
* **Tablet (600dp <= Width < 1240dp)**: Outer margins scale to `24.dp`.
* **Desktop & Web (Width >= 1240dp)**: Outer margins scale to `32.dp` or restrict to a max-width container of `1200.dp` centered on screen.

---

## 5. Core Premium Fintech Components

Here are the specifications and code templates for the core components that give BudgetMaster its premium visual signature.

### A. The Glassmorphic Balance Card
A premium gradient card with a fine outline, mimicking a physical glass sheet. Displays the global balance, monthly flow indicators, and acts as the dashboard focal point.

```kotlin
@Composable
fun BalanceCard(
    balance: String,
    monthlyChange: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.90f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "TOTAL BALANCE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = balance,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (isPositive) Color(0xFF10B981) else Color(0xFFF87171),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = monthlyChange,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "this month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                )
            }
        }
    }
}
```

### B. Intelligent Transaction Row
A highly readable transaction listing row supporting category-themed icons, metadata tags, and color-coded transaction figures.

```kotlin
@Composable
fun TransactionRow(
    categoryName: String,
    description: String,
    amount: String,
    date: String,
    isExpense: Boolean,
    icon: ImageVector,
    categoryColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rounded category circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(categoryColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = categoryColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "$categoryName • $date",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        Text(
            text = (if (isExpense) "-" else "+") + amount,
            // Custom fontFeatureSettings mapping via typography helper
            style = MaterialTheme.typography.bodyMediumNumeric,
            color = if (isExpense) MaterialTheme.colorScheme.error else Color(0xFF10B981),
            fontWeight = FontWeight.SemiBold
        )
    }
}
```

### C. Budget Progress Gauge
A customizable indicator that transitions smoothly from safe greens to warning oranges, and turns solid high-contrast red when a budget is exceeded.

```kotlin
@Composable
fun BudgetProgressBar(
    categoryName: String,
    spentAmount: Float,
    limitAmount: Float,
    modifier: Modifier = Modifier
) {
    val progress = (spentAmount / limitAmount).coerceIn(0f, 1f)
    val progressColor = when {
        progress >= 1.0f -> MaterialTheme.colorScheme.error
        progress >= 0.85f -> Color(0xFFF59E0B) // Amber Warning
        else -> Color(0xFF10B981) // Emerald Safe
    }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "$${spentAmount.toInt()} of $${limitAmount.toInt()}",
                style = MaterialTheme.typography.bodyMediumNumeric,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}
```

---

## 6. Motion & Animation System

Animations in BudgetMaster make the app feel alive and interactive. We align with the **Material 3 Expressive motion curve**.

### Easing & Durations
* **Standard Easing (Expressive)**: `CubicBezierEasing(0.2, 0.8, 0.2, 1.0)`. Fast start, slow end, creating a snappy yet smooth transition.
* **Duration Tokens**:
  - `Duration.Snappy` = `150ms` (Button states, selection clicks, micro-toggles).
  - `Duration.Medium` = `300ms` (Progress bar fills, card expands, sheet slides).
  - `Duration.Slow` = `450ms` (Screen transition animations, full-screen transitions).

### Key Animation Behaviors
1. **Dynamic Value Count-up**: Balances count up from `$0.00` to target values on dashboard initialization using a simple coroutine loop or Compose value-interpolator.
2. **Press-to-Scale Interaction**: Main buttons shrink slightly (`scaleX/Y = 0.96f`) on press, providing a tactile, mechanical feel.
3. **Ghost Shimmer Effects**: While loading transactions or insights, items show a sweeping shimmer animation from left to right using a gradient offset.

---

## 7. Accessibility (A11y) Guidelines

A premium design system must accommodate all users. We enforce standard WCAG rules and add specific guidelines for financial text formatting.

* **Contrast Ratios**:
  - Normal Text (Body, Labels): Minimum 4.5:1 (WCAG AA).
  - Large Text (Display balances): Minimum 3.0:1.
  - Interactive icons: Minimum 3.0:1.
* **Minimum Target Area**: All interactive components (transaction rows, menu items, action widgets) must be at least `48.dp` x `48.dp` to prevent misclicks.
* **Screen Reader Descriptions**:
  - Standard transaction items must announce: *"Expense of 12 dollars for Starbucks, Category Food, June 6th"*.
  - Progress Gauges must describe: *"Food Budget is 85% spent. 170 dollars spent of 200 dollars limit."*
  - Charts must have dynamic content descriptions: *"Graph showing income rising from 2,000 dollars in January to 3,500 dollars in May."*
* **Dynamic Text Handling**: All screens must support layout wrapping to prevent clipping when the system font scale is zoomed up to 200%.

---

## 8. Responsive Layout & Adaptive Grid

We leverage Compose Multiplatform's adaptive layout features to resize content smoothly from a 6.1" Android/iOS screen to a 27" desktop browser.

### Grid Boundaries

| Device Category | Breakpoint (Width) | Layout Structure | Navigation Component |
| :--- | :--- | :--- | :--- |
| **Phone** | `< 600dp` | Single column list | Bottom Navigation Bar |
| **Tablet** | `600dp` to `1240dp` | Dual-pane split layout | Left Navigation Rail |
| **Desktop / Web** | `>= 1240dp` | Multi-column grid, max width 1200dp | Left Navigation Drawer |

### Adaptive Layout Rules
1. **List-Detail Splits**: On tablets and desktops, transactions list views split automatically to display list details on the right panel, preventing overly wide layout lines.
2. **Bottom Sheet Adaptation**: Bottom sheets on mobile (e.g., quick add transaction) adapt into centered Modal Dialogs on desktop and web views.
3. **Chart Aspect Ratio**: Charts retain a standard `16:9` ratio on mobile, scale to a maximum height of `320.dp` on tablet, and are placed side-by-side with tabular breakdowns on desktop layouts.
