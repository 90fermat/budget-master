package com.budgetmaster.accounts.di

import com.budgetmaster.accounts.data.repository.SqlDelightAccountRepository
import com.budgetmaster.accounts.domain.repository.AccountRepository
import com.budgetmaster.accounts.domain.usecase.ArchiveAccountUseCase
import com.budgetmaster.accounts.domain.usecase.DeleteAccountUseCase
import com.budgetmaster.accounts.domain.usecase.ObserveAccountsUseCase
import com.budgetmaster.accounts.domain.usecase.ObserveActiveAccountUseCase
import com.budgetmaster.accounts.domain.usecase.ReconcileAccountUseCase
import com.budgetmaster.accounts.domain.usecase.SaveAccountUseCase
import com.budgetmaster.accounts.domain.usecase.SelectActiveAccountUseCase
import com.budgetmaster.accounts.domain.usecase.TransferBetweenAccountsUseCase
import com.budgetmaster.accounts.presentation.AccountsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

/** Koin bindings for the Accounts feature. */
val accountsModule = module {
    single { SqlDelightAccountRepository(get(), get(), get()) } bind AccountRepository::class

    factory { ObserveAccountsUseCase(get()) }
    factory { SaveAccountUseCase(get()) }
    factory { ArchiveAccountUseCase(get()) }
    factory { DeleteAccountUseCase(get(), get()) }
    factory { ObserveActiveAccountUseCase(get()) }
    factory { SelectActiveAccountUseCase(get()) }
    factory { TransferBetweenAccountsUseCase(get()) }
    factory { ReconcileAccountUseCase(get()) }

    viewModel { AccountsViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
}
