package com.budgetmaster.transactions.di

import com.budgetmaster.transactions.data.SqlDelightTransactionRepository
import com.budgetmaster.transactions.domain.repository.TransactionRepository
import com.budgetmaster.transactions.domain.usecase.DeleteTransactionUseCase
import com.budgetmaster.transactions.domain.usecase.ObserveCategoriesUseCase
import com.budgetmaster.transactions.domain.usecase.ObserveTransactionsUseCase
import com.budgetmaster.transactions.domain.usecase.RestoreTransactionUseCase
import com.budgetmaster.transactions.domain.usecase.SaveTransactionUseCase
import com.budgetmaster.transactions.presentation.TransactionsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module for the Transactions feature.
 */
val transactionsModule = module {
    single { SqlDelightTransactionRepository(get()) } bind TransactionRepository::class

    factory { ObserveTransactionsUseCase(get()) }
    factory { ObserveCategoriesUseCase(get()) }
    factory { SaveTransactionUseCase(get()) }
    factory { DeleteTransactionUseCase(get()) }
    factory { RestoreTransactionUseCase(get()) }

    viewModel {
        TransactionsViewModel(
            observeTransactions = get(),
            observeCategories = get(),
            saveTransaction = get(),
            deleteTransaction = get(),
            restoreTransaction = get(),
        )
    }
}
