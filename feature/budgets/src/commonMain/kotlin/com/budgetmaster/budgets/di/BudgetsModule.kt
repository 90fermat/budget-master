package com.budgetmaster.budgets.di

import com.budgetmaster.budgets.data.repository.SqlDelightBudgetRepository
import com.budgetmaster.budgets.data.repository.SqlDelightGoalRepository
import com.budgetmaster.budgets.domain.repository.BudgetRepository
import com.budgetmaster.budgets.domain.repository.GoalRepository
import com.budgetmaster.budgets.domain.usecase.ContributeToGoalUseCase
import com.budgetmaster.budgets.domain.usecase.DeleteBudgetUseCase
import com.budgetmaster.budgets.domain.usecase.DeleteGoalUseCase
import com.budgetmaster.budgets.domain.usecase.NotifyBudgetThresholdsUseCase
import com.budgetmaster.budgets.domain.usecase.WithdrawFromGoalUseCase
import com.budgetmaster.budgets.domain.usecase.ObserveBudgetCategoriesUseCase
import com.budgetmaster.budgets.domain.usecase.ObserveBudgetsUseCase
import com.budgetmaster.budgets.domain.usecase.ObserveGoalsUseCase
import com.budgetmaster.budgets.domain.usecase.SaveBudgetUseCase
import com.budgetmaster.budgets.domain.usecase.SuggestBudgetsUseCase
import com.budgetmaster.budgets.domain.usecase.SaveGoalUseCase
import com.budgetmaster.budgets.presentation.BudgetsViewModel
import com.budgetmaster.budgets.presentation.GoalsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module for the Budgets and Savings Goals feature.
 */
val budgetsModule = module {
    single { SqlDelightBudgetRepository(get(), get(), get()) } bind BudgetRepository::class
    single { SqlDelightGoalRepository(get(), get(), get()) } bind GoalRepository::class

    // Budgets use cases
    factory { ObserveBudgetsUseCase(get()) }
    factory { ObserveBudgetCategoriesUseCase(get()) }
    factory { SaveBudgetUseCase(get()) }
    factory { DeleteBudgetUseCase(get()) }

    // Goals use cases
    factory { ObserveGoalsUseCase(get()) }
    factory { SaveGoalUseCase(get()) }
    factory { ContributeToGoalUseCase(get()) }
    factory { WithdrawFromGoalUseCase(get()) }
    factory { NotifyBudgetThresholdsUseCase(get(), get()) }
    factory { DeleteGoalUseCase(get()) }
    factory { SuggestBudgetsUseCase(get(), get()) }

    viewModel { BudgetsViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { GoalsViewModel(get(), get(), get(), get(), get(), get()) }
}
