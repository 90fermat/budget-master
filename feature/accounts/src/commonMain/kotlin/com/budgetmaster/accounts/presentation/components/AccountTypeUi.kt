package com.budgetmaster.accounts.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.account_type_cash
import budgetmaster.core.generated.resources.account_type_checking
import budgetmaster.core.generated.resources.account_type_credit_card
import budgetmaster.core.generated.resources.account_type_investment
import budgetmaster.core.generated.resources.account_type_savings
import com.budgetmaster.accounts.domain.model.AccountType
import org.jetbrains.compose.resources.stringResource

/** Localized display name for an [AccountType]. */
@Composable
fun AccountType.label(): String = stringResource(
    when (this) {
        AccountType.CASH -> Res.string.account_type_cash
        AccountType.CHECKING -> Res.string.account_type_checking
        AccountType.SAVINGS -> Res.string.account_type_savings
        AccountType.CREDIT_CARD -> Res.string.account_type_credit_card
        AccountType.INVESTMENT -> Res.string.account_type_investment
    },
)

/** Representative icon for an [AccountType]. */
val AccountType.icon: ImageVector
    get() = when (this) {
        AccountType.CASH -> Icons.Filled.Payments
        AccountType.CHECKING -> Icons.Filled.AccountBalance
        AccountType.SAVINGS -> Icons.Filled.Savings
        AccountType.CREDIT_CARD -> Icons.Filled.CreditCard
        AccountType.INVESTMENT -> Icons.Filled.TrendingUp
    }
