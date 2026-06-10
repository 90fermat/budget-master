package com.budgetmaster.budgets.di

import com.budgetmaster.budgets.data.repository.SqlDelightBudgetRepository
import com.budgetmaster.budgets.domain.repository.BudgetRepository
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module for the Budgets and Savings Goals feature dependencies.
 */
val budgetsModule = module {
    single { SqlDelightBudgetRepository(get()) } bind BudgetRepository::class
}
