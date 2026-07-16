package com.budgetmaster.budgets.domain.usecase

import com.budgetmaster.budgets.domain.model.BudgetCategory
import com.budgetmaster.budgets.domain.model.BudgetDraft
import com.budgetmaster.budgets.domain.model.BudgetItem
import com.budgetmaster.budgets.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

/** Observes the current-period budgets with live spent amounts. */
class ObserveBudgetsUseCase(private val repository: BudgetRepository) {
    operator fun invoke(): Flow<List<BudgetItem>> = repository.observeBudgets()
}

/** Observes categories for the budget picker. */
class ObserveBudgetCategoriesUseCase(private val repository: BudgetRepository) {
    operator fun invoke(): Flow<List<BudgetCategory>> = repository.observeCategories()
}

/** Validates and saves a budget. */
class SaveBudgetUseCase(private val repository: BudgetRepository) {
    /** @throws IllegalArgumentException if the limit is not positive. */
    suspend operator fun invoke(draft: BudgetDraft) {
        require(draft.limit > 0.0) { "Budget limit must be greater than zero." }
        repository.upsertBudget(draft)
    }
}

/** Deletes a budget by id. */
class DeleteBudgetUseCase(private val repository: BudgetRepository) {
    suspend operator fun invoke(id: String) = repository.deleteBudget(id)
}
