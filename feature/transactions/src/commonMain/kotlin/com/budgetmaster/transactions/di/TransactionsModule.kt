package com.budgetmaster.transactions.di

import com.budgetmaster.transactions.data.SqlDelightRecurringRepository
import com.budgetmaster.transactions.data.SqlDelightTransactionRepository
import com.budgetmaster.transactions.domain.repository.RecurringRepository
import com.budgetmaster.transactions.domain.usecase.DeleteRecurringUseCase
import com.budgetmaster.transactions.domain.usecase.MaterializeDueRecurringUseCase
import com.budgetmaster.transactions.domain.usecase.ObserveRecurringUseCase
import com.budgetmaster.transactions.domain.usecase.SaveRecurringUseCase
import com.budgetmaster.transactions.domain.usecase.SetRecurringActiveUseCase
import com.budgetmaster.transactions.domain.repository.TransactionRepository
import com.budgetmaster.transactions.domain.usecase.DeleteTransactionUseCase
import com.budgetmaster.transactions.domain.usecase.ObserveCategoriesUseCase
import com.budgetmaster.transactions.domain.usecase.ObserveTransactionAccountsUseCase
import com.budgetmaster.transactions.domain.usecase.ObserveTransactionsUseCase
import com.budgetmaster.transactions.domain.usecase.RestoreTransactionUseCase
import com.budgetmaster.transactions.domain.usecase.SaveTransactionUseCase
import com.budgetmaster.transactions.domain.usecase.ParseQuickEntryUseCase
import com.budgetmaster.transactions.presentation.TransactionsViewModel
import com.budgetmaster.transactions.presentation.recurring.RecurringViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module for the Transactions feature.
 */
val transactionsModule = module {
    single { SqlDelightTransactionRepository(get(), get(), get(), get()) } bind TransactionRepository::class
    single { SqlDelightRecurringRepository(get(), get(), get()) } bind RecurringRepository::class

    factory { ObserveRecurringUseCase(get()) }
    factory { SaveRecurringUseCase(get()) }
    factory { SetRecurringActiveUseCase(get()) }
    factory { DeleteRecurringUseCase(get()) }
    factory { MaterializeDueRecurringUseCase(get()) }

    viewModel {
        RecurringViewModel(
            observeRecurring = get(),
            observeCategories = get(),
            observeAccounts = get(),
            settingsRepository = get(),
            saveRecurring = get(),
            setRecurringActive = get(),
            deleteRecurring = get(),
            materializeDue = get(),
        )
    }

    factory { ObserveTransactionsUseCase(get()) }
    factory { ObserveCategoriesUseCase(get()) }
    factory { ObserveTransactionAccountsUseCase(get()) }
    factory { SaveTransactionUseCase(get()) }
    factory { DeleteTransactionUseCase(get()) }
    factory { RestoreTransactionUseCase(get()) }
    factory { ParseQuickEntryUseCase(get()) }

    viewModel {
        TransactionsViewModel(
            observeTransactions = get(),
            observeCategories = get(),
            observeAccounts = get(),
            settingsRepository = get(),
            activeAccountStore = get(),
            saveTransaction = get(),
            deleteTransaction = get(),
            restoreTransaction = get(),
            parseQuickEntry = get(),
        )
    }
}
