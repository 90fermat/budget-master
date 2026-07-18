package com.budgetmaster.core.designsystem.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.budgetmaster.core.designsystem.financialColors
import com.budgetmaster.core.util.MoneyFormatter
import com.budgetmaster.core.util.formatSigned

/**
 * How much visual weight an amount carries.
 *
 * In a finance app the number *is* the content, so it gets its own scale rather than borrowing
 * body/title styles — that is most of the difference between reading as a spreadsheet and reading
 * as a product.
 */
enum class AmountEmphasis {
    /** The one number a screen is about — a balance, a net worth. */
    Hero,

    /** A section total: a budget limit, a period total. */
    Prominent,

    /** A row in a list. */
    Standard,
}

/**
 * Renders a money value.
 *
 * Three things every amount in the app needs, applied in one place so no caller has to remember:
 *
 * 1. **Tabular figures** (`tnum`), so digits occupy equal width and a column of amounts lines up
 *    instead of shimmering as values change.
 * 2. **Bidirectional isolation**, applied by [MoneyFormatter.format] itself so it cannot be
 *    forgotten — a formatted amount is a run of neutral characters, so in an RTL paragraph the
 *    surrounding text reorders it (`+2.4%` became `2.4%+`).
 * 3. **Direction colour** — income and expense read at a glance, from the palette-independent
 *    financial colours rather than a hardcoded green/red.
 *
 * @param signed shows an explicit `+`/`-` and colours by direction. Off for a balance, where the
 *   value's own sign is enough and a `+` would look like a change rather than a total.
 */
@Composable
fun AmountText(
    amount: Double,
    currencyCode: String,
    modifier: Modifier = Modifier,
    emphasis: AmountEmphasis = AmountEmphasis.Standard,
    signed: Boolean = false,
    color: Color? = null,
) {
    val text = if (signed) {
        MoneyFormatter.formatSigned(amount, currencyCode)
    } else {
        MoneyFormatter.format(amount, currencyCode)
    }

    val resolvedColor = color ?: when {
        !signed -> LocalTextStyle.current.color.takeIf { it != Color.Unspecified }
            ?: MaterialTheme.colorScheme.onSurface
        amount < 0 -> MaterialTheme.financialColors.expense
        else -> MaterialTheme.financialColors.income
    }

    Text(
        text = text,
        style = emphasis.style(),
        color = resolvedColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** The money type scale. Tabular figures throughout — a money column that jitters looks broken. */
@Composable
private fun AmountEmphasis.style(): TextStyle {
    val base = when (this) {
        AmountEmphasis.Hero -> MaterialTheme.typography.displaySmall
        AmountEmphasis.Prominent -> MaterialTheme.typography.headlineSmall
        AmountEmphasis.Standard -> MaterialTheme.typography.bodyLarge
    }
    return base.copy(
        fontFeatureSettings = "tnum",
        fontWeight = when (this) {
            AmountEmphasis.Hero -> FontWeight.Bold
            AmountEmphasis.Prominent -> FontWeight.Bold
            AmountEmphasis.Standard -> FontWeight.SemiBold
        },
    )
}
