package com.budgetmaster.accounts.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.accounts_excluded_badge
import budgetmaster.core.generated.resources.accounts_exclude_from_totals
import budgetmaster.core.generated.resources.accounts_include_in_totals
import budgetmaster.core.generated.resources.accounts_archive
import budgetmaster.core.generated.resources.accounts_edit
import budgetmaster.core.generated.resources.accounts_reconcile
import budgetmaster.core.generated.resources.accounts_restore
import budgetmaster.core.generated.resources.action_delete
import com.budgetmaster.core.designsystem.SurfaceLevel
import com.budgetmaster.core.designsystem.components.AppCard
import com.budgetmaster.accounts.domain.model.Account
import com.budgetmaster.accounts.domain.model.AccountType
import com.budgetmaster.core.util.MoneyFormatter
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material3.Checkbox

/**
 * A single wallet row: type icon, name/type, current balance, and an overflow menu
 * (edit / archive-restore / delete).
 */
@Composable
fun AccountCard(
    account: Account,
    onEdit: () -> Unit,
    onArchiveToggle: () -> Unit,
    onIncludeInTotalsToggle: () -> Unit,
    onReconcile: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isLiability = account.type == AccountType.CREDIT_CARD
    val balanceColor = when {
        account.currentBalance < 0 || isLiability && account.currentBalance != 0.0 ->
            MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    AppCard(modifier = modifier, level = SurfaceLevel.Raised) {
        Row(
            modifier = Modifier.alpha(if (account.isArchived) 0.55f else 1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = account.type.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    account.type.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Said on the card itself: a wallet silently missing from the combined balance
                // would read as a bug rather than as the choice it is.
                if (!account.includeInTotals) {
                    Text(
                        text = stringResource(Res.string.accounts_excluded_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(
                text = MoneyFormatter.format(account.currentBalance, account.currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = balanceColor,
            )

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.accounts_edit)) },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    if (!account.isArchived) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.accounts_reconcile)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Rule, contentDescription = null) },
                            onClick = { menuOpen = false; onReconcile() },
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (account.isArchived) Res.string.accounts_restore else Res.string.accounts_archive,
                                ),
                            )
                        },
                        onClick = { menuOpen = false; onArchiveToggle() },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (account.includeInTotals) {
                                        Res.string.accounts_exclude_from_totals
                                    } else {
                                        Res.string.accounts_include_in_totals
                                    },
                                ),
                            )
                        },
                        // A checkbox rather than a bare label, so the row reads as a setting with
                        // a current state instead of an action whose effect you find out by trying
                        // it. Not interactive itself — the row's own click toggles it.
                        trailingIcon = {
                            Checkbox(checked = account.includeInTotals, onCheckedChange = null)
                        },
                        onClick = { menuOpen = false; onIncludeInTotalsToggle() },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(Res.string.action_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}
