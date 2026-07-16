package com.budgetmaster.budgets.domain.repository

import com.budgetmaster.budgets.domain.model.BudgetCategory
import com.budgetmaster.budgets.domain.model.BudgetDraft
import com.budgetmaster.budgets.domain.model.BudgetItem
import kotlinx.coroutines.flow.Flow

/**
 * Data source for category budgets. `spent` is computed live from transactions.
 */
interface BudgetRepository {
    /** Observes the budgets active in the current period, with live spent amounts. */
    fun observeBudgets(): Flow<List<BudgetItem>>

    /** Observes all categories (for the create/edit picker). */
    fun observeCategories(): Flow<List<BudgetCategory>>

    /** Inserts or updates a budget from [draft]. */
    suspend fun upsertBudget(draft: BudgetDraft)

    /** Deletes the budget with [id]. */
    suspend fun deleteBudget(id: String)
}
