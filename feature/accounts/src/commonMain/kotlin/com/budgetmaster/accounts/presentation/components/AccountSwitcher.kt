package com.budgetmaster.accounts.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.accounts_all
import budgetmaster.core.generated.resources.accounts_manage
import com.budgetmaster.accounts.presentation.AccountsState
import com.budgetmaster.core.util.MoneyFormatter
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.material3.Checkbox

/**
 * Compact global account selector: shows the active wallet (or "All accounts") and opens a
 * menu to switch scope or jump to account management.
 */
@Composable
fun AccountSwitcher(
    state: AccountsState,
    onSelect: (String?) -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
    onSetIncludedInTotals: (id: String, included: Boolean) -> Unit = { _, _ -> },
) {
    var open by remember { mutableStateOf(false) }
    val active = state.accounts.firstOrNull { it.id == state.activeAccountId && !it.isArchived }
    val label = active?.name ?: stringResource(Res.string.accounts_all)

    Row(modifier = modifier) {
        AssistChip(
            onClick = { open = true },
            label = {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.accounts_all)) },
                onClick = { open = false; onSelect(null) },
            )
            // Every wallet still in use, including those left out of the combined total. They are
            // still wallets: you can switch to one and see its own figures. The checkbox is what
            // decides whether it is added to the others, and it lives here as well as on the
            // accounts screen because this is where the question naturally comes up.
            state.visibleAccounts.forEach { account ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(account.name, modifier = Modifier.weight(1f, fill = false))
                            Text(
                                MoneyFormatter.format(account.currentBalance, account.currency),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    leadingIcon = { Icon(account.type.icon, contentDescription = null) },
                    trailingIcon = {
                        Checkbox(
                            checked = account.includeInTotals,
                            // Toggling does not switch wallet or close the menu: the user is
                            // adjusting which wallets make up "All accounts", and that is usually
                            // several decisions in a row.
                            onCheckedChange = { onSetIncludedInTotals(account.id, it) },
                        )
                    },
                    onClick = { open = false; onSelect(account.id) },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.accounts_manage)) },
                leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                onClick = { open = false; onManage() },
            )
        }
    }
}
