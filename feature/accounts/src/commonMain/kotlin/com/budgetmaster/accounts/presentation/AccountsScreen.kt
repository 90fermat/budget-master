package com.budgetmaster.accounts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.accounts_add
import budgetmaster.core.generated.resources.accounts_archived_section
import budgetmaster.core.generated.resources.accounts_delete_confirm
import budgetmaster.core.generated.resources.accounts_empty
import budgetmaster.core.generated.resources.empty_accounts_cta
import budgetmaster.core.generated.resources.accounts_net_worth
import budgetmaster.core.generated.resources.accounts_rates_attribution
import budgetmaster.core.generated.resources.accounts_multi_currency_note
import budgetmaster.core.generated.resources.accounts_title
import budgetmaster.core.generated.resources.accounts_transfer
import budgetmaster.core.generated.resources.action_cancel
import budgetmaster.core.generated.resources.action_delete
import com.budgetmaster.core.designsystem.SurfaceLevel
import com.budgetmaster.core.designsystem.components.AppCard
import com.budgetmaster.accounts.presentation.components.AccountCard
import com.budgetmaster.accounts.presentation.components.AddEditAccountForm
import com.budgetmaster.accounts.presentation.components.ReconcileForm
import com.budgetmaster.accounts.presentation.components.TransferForm
import com.budgetmaster.core.designsystem.animateCounter
import com.budgetmaster.core.designsystem.components.EmptyState
import com.budgetmaster.core.designsystem.components.GuidanceHost
import com.budgetmaster.core.designsystem.components.HelpIconButton
import com.budgetmaster.core.designsystem.components.rememberGuidance
import com.budgetmaster.core.guidance.GuidanceKey
import com.budgetmaster.core.designsystem.components.AmountEmphasis
import com.budgetmaster.core.designsystem.components.AmountText
import com.budgetmaster.core.util.MoneyFormatter
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Accounts management screen: net-worth overview, wallet list, and create/edit/archive/delete. */
@Composable
fun AccountsScreen(viewModel: AccountsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    val guidance = rememberGuidance(GuidanceKey.ACCOUNTS)
    GuidanceHost(guidance)

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Transfers need at least two wallets to move money between.
                if (state.activeAccounts.size > 1) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.onIntent(AccountsIntent.OpenTransfer) },
                        icon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
                        text = { Text(stringResource(Res.string.accounts_transfer)) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    )
                }
                ExtendedFloatingActionButton(
                    onClick = { viewModel.onIntent(AccountsIntent.OpenAdd) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.accounts_add)) },
                )
            }
        },
    ) { padding ->
        val active = state.activeAccounts
        val archived = state.accounts.filter { it.isArchived }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(Res.string.accounts_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    HelpIconButton(onClick = guidance::show)
                }
            }
            item { NetWorthCard(state) }

            if (active.isEmpty() && archived.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.AccountBalanceWallet,
                        title = stringResource(Res.string.accounts_title),
                        subtitle = stringResource(Res.string.accounts_empty),
                        actionLabel = stringResource(Res.string.empty_accounts_cta),
                        onAction = { viewModel.onIntent(AccountsIntent.OpenAdd) },
                    )
                }
            }

            items(active, key = { it.id }) { account ->
                AccountCard(
                    account = account,
                    onEdit = { viewModel.onIntent(AccountsIntent.OpenEdit(account)) },
                    onArchiveToggle = { viewModel.onIntent(AccountsIntent.SetArchived(account.id, true)) },
                    onReconcile = { viewModel.onIntent(AccountsIntent.OpenReconcile(account)) },
                    onDelete = { pendingDelete = account.id },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (archived.isNotEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.accounts_archived_section),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(archived, key = { it.id }) { account ->
                    AccountCard(
                        account = account,
                        onEdit = { viewModel.onIntent(AccountsIntent.OpenEdit(account)) },
                        onArchiveToggle = { viewModel.onIntent(AccountsIntent.SetArchived(account.id, false)) },
                        onReconcile = { viewModel.onIntent(AccountsIntent.OpenReconcile(account)) },
                        onDelete = { pendingDelete = account.id },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (state.editorOpen) {
        AddEditAccountForm(
            existing = state.editingAccount,
            onSubmit = { viewModel.onIntent(AccountsIntent.Submit(it)) },
            onDismiss = { viewModel.onIntent(AccountsIntent.DismissEditor) },
        )
    }

    if (state.transferOpen) {
        TransferForm(
            accounts = state.activeAccounts,
            initialFromId = state.activeAccountId,
            onSubmit = { from, to, amount, timestamp ->
                viewModel.onIntent(AccountsIntent.SubmitTransfer(from, to, amount, timestamp))
            },
            onDismiss = { viewModel.onIntent(AccountsIntent.DismissTransfer) },
        )
    }

    state.reconcilingAccount?.let { account ->
        ReconcileForm(
            account = account,
            onSubmit = { viewModel.onIntent(AccountsIntent.SubmitReconcile(account.id, it)) },
            onDismiss = { viewModel.onIntent(AccountsIntent.DismissReconcile) },
        )
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            text = { Text(stringResource(Res.string.accounts_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onIntent(AccountsIntent.Delete(id))
                    pendingDelete = null
                }) {
                    Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun NetWorthCard(state: AccountsState) {
    AppCard(level = SurfaceLevel.Hero) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(Res.string.accounts_net_worth),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            val animatedNetWorth by animateCounter(state.netWorth)
            AmountText(
                amount = animatedNetWorth,
                currencyCode = state.primaryCurrency,
                emphasis = AmountEmphasis.Hero,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (state.isNetWorthApproximate) {
                Text(
                    stringResource(Res.string.accounts_multi_currency_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (state.isMultiCurrency) {
                // The rate provider's terms require a visible credit. It may be discreet, but it
                // has to be here — and only when converted rates actually contributed.
                Text(
                    stringResource(Res.string.accounts_rates_attribution),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}
