package com.budgetmaster.transactions.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.transactions_review_discard
import budgetmaster.core.generated.resources.transactions_review_fee
import budgetmaster.core.generated.resources.transactions_review_hint
import budgetmaster.core.generated.resources.transactions_review_keep
import budgetmaster.core.generated.resources.transactions_review_subtitle
import budgetmaster.core.generated.resources.transactions_review_title
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.SurfaceLevel
import com.budgetmaster.core.designsystem.components.AmountEmphasis
import com.budgetmaster.core.designsystem.components.AmountText
import com.budgetmaster.core.designsystem.components.AppCard
import com.budgetmaster.core.util.DateUtils
import com.budgetmaster.core.util.MoneyFormatter
import com.budgetmaster.transactions.presentation.PendingImportItem
import org.jetbrains.compose.resources.stringResource

/**
 * The review queue.
 *
 * The importer defers rather than guesses when a captured message matches something already in
 * the ledger, because guessing wrong is silent either way: merge a genuine second payment and the
 * balance is quietly short, keep a true duplicate and it is quietly doubled. Before this existed
 * the deferral was invisible - nothing was imported and nothing was said.
 *
 * Both answers are phrased from the user's point of view rather than the importer's ("Already
 * have it", not "Discard"), because they are being asked about their money, not about our record.
 */
@Composable
fun PendingImportsCard(
    items: List<PendingImportItem>,
    currencyCode: String,
    onResolve: (hash: String, keep: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    AppCard(modifier = modifier, level = SurfaceLevel.Raised) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(Spacing.small))
            Text(
                stringResource(Res.string.transactions_review_title),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Spacer(Modifier.height(Spacing.small))
        Text(
            stringResource(Res.string.transactions_review_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        items.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(
                    Modifier.padding(vertical = Spacing.small),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            } else {
                Spacer(Modifier.height(Spacing.medium))
            }
            PendingImportRow(item, currencyCode, onResolve)
        }

        Spacer(Modifier.height(Spacing.small))
        Text(
            stringResource(Res.string.transactions_review_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PendingImportRow(
    item: PendingImportItem,
    currencyCode: String,
    onResolve: (hash: String, keep: Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.description, style = MaterialTheme.typography.bodyMedium)
                Text(
                    item.provider + " - " + DateUtils.toLocalDate(item.occurredAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(Spacing.small))
            AmountText(
                amount = item.amount,
                currencyCode = currencyCode,
                emphasis = AmountEmphasis.Standard,
            )
        }
        if (item.fee > 0.0) {
            // Named, because the fee is why the captured figure can differ from what the user
            // typed - and a mismatch they cannot explain is a reason to distrust every import.
            Text(
                stringResource(
                    Res.string.transactions_review_fee,
                    MoneyFormatter.format(item.fee, currencyCode),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { onResolve(item.hash, false) }) {
                Text(stringResource(Res.string.transactions_review_discard))
            }
            Spacer(Modifier.size(Spacing.small))
            OutlinedButton(onClick = { onResolve(item.hash, true) }) {
                Text(stringResource(Res.string.transactions_review_keep))
            }
        }
    }
}
